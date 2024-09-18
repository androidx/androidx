/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ink.authoring.internal

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RenderNode
import android.hardware.DataSpace
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
import androidx.graphics.surface.SurfaceControlCompat
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.geometry.MutableBox
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.InProgressStroke
import kotlin.math.ceil
import kotlin.math.floor

/**
 * An implementation of [InProgressStrokesRenderHelper] based on [CanvasFrontBufferedRenderer],
 * which allows for low-latency rendering.
 *
 * @param mainView The [View] within which the front buffer should be constructed.
 * @param callback How to render the desired content within the front buffer.
 * @param renderer Draws individual stroke objects using [Canvas].
 * @param useOffScreenFrameBuffer A temporary flag to gate the offscreen frame buffer feature for
 *   reducing rendering artifacts until [CanvasInProgressStrokesRenderHelperV33] is fully rolled
 *   out.
 * @param canvasFrontBufferedRendererWrapper Override the default only for testing.
 * @param uiThreadHandler Override the default only for testing.
 */
@Suppress("ObsoleteSdkInt") // TODO(b/262911421): Should not need to suppress.
@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalLatencyDataApi::class)
internal class CanvasInProgressStrokesRenderHelperV29(
    private val mainView: ViewGroup,
    private val callback: InProgressStrokesRenderHelper.Callback,
    private val renderer: CanvasStrokeRenderer,
    private val useOffScreenFrameBuffer: Boolean,
    private val canvasFrontBufferedRendererWrapper: CanvasFrontBufferedRendererWrapper =
        CanvasFrontBufferedRendererWrapperImpl(),
    frontBufferToHwuiHandoffFactory: (SurfaceView) -> FrontBufferToHwuiHandoff = { surfaceView ->
        FrontBufferToHwuiHandoff.create(
            mainView,
            surfaceView,
            callback::onStrokeCohortHandoffToHwui,
            callback::onStrokeCohortHandoffToHwuiComplete,
        )
    },
    private val uiThreadHandler: Handler = Handler(Looper.getMainLooper()),
) : InProgressStrokesRenderHelper {

    // The front buffer is updated each time rather than cleared and completely redrawn every time
    // as
    // a performance optimization.
    override val contentsPreservedBetweenDraws = true

    override val supportsDebounce = true

    override val supportsFlush = true

    override var maskPath: Path? = null

    private val maskPaint =
        Paint().apply {
            color = Color.TRANSPARENT
            blendMode = BlendMode.CLEAR
        }

    private val surfaceView =
        SurfaceView(mainView.context).apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSLUCENT)
        }

    private val viewListener =
        object : View.OnAttachStateChangeListener {
            @UiThread
            override fun onViewAttachedToWindow(v: View) {
                addAndInitSurfaceView()
            }

            @UiThread
            override fun onViewDetachedFromWindow(v: View) {
                frontBufferToHwuiHandoff.cleanup()
                canvasFrontBufferedRendererWrapper.release(::recordRenderThreadIdentity)
                mainView.removeView(surfaceView)
            }
        }

    /** Valid only during active drawing (when `duringDraw` is `true`). */
    private val onDrawState =
        object {
            var duringDraw = false
            var frontBufferCanvas: Canvas? = null
            /** Only valid from [prepareToDrawInModifiedRegion] to [afterDrawInModifiedRegion]. */
            var offScreenCanvas: Canvas? = null
        }
        get() {
            assertOnRenderThread()
            return field
        }

    private val canvasFrontBufferedRendererCallback =
        object : CanvasFrontBufferedRendererWrapper.Callback {

            @WorkerThread
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int
            ) {
                recordRenderThreadIdentity()

                if (useOffScreenFrameBuffer) {
                    ensureOffScreenFrameBuffer(bufferWidth, bufferHeight)
                }

                // Just in case save/restores get imbalanced among callbacks
                val originalSaveCount = canvas.saveCount

                onDrawState.frontBufferCanvas = canvas

                onDrawState.duringDraw = true
                callback.onDraw()
                onDrawState.duringDraw = false

                // NOMUTANTS -- Defensive programming to avoid bad state being used later.
                run { onDrawState.frontBufferCanvas = null }

                // Clear the client-defined masked area.
                maskPath?.let { canvas.drawPath(it, maskPaint) }

                callback.onDrawComplete()
                check(canvas.saveCount == originalSaveCount) {
                    "Unbalanced saves and restores. Expected save count of $originalSaveCount, got ${canvas.saveCount}."
                }
            }

            @WorkerThread
            @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, lambda = 0)
            override fun onFrontBufferedLayerRenderComplete(
                transactionSetDataSpace: (SurfaceControlCompat, Int) -> Unit,
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    transactionSetDataSpace(
                        frontBufferedLayerSurfaceControl,
                        DataSpace.DATASPACE_DISPLAY_P3
                    )
                }
                callback.setCustomLatencyDataField(finishesDrawCallsSetter)
                callback.handOffAllLatencyData()
            }
        }

    /**
     * Defined as a lambda instead of a member function or companion object function to ensure that
     * no extra allocation takes place when passing this function object into the higher-level
     * callback.
     */
    private val finishesDrawCallsSetter = { data: LatencyData, timeNanos: Long ->
        data.canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = timeNanos
    }

    private val frontBufferToHwuiHandoff = frontBufferToHwuiHandoffFactory(surfaceView)

    /** Saved to later ensure that certain operations are running on the appropriate thread. */
    private lateinit var renderThread: Thread

    private var offScreenFrameBuffer: RenderNode? = null
    private val offScreenFrameBufferPaint =
        Paint().apply {
            // The SRC blend mode ensures that the modified region of the offscreen frame buffer
            // completely
            // replaces the matching region of the front buffer.
            blendMode = BlendMode.SRC
        }
    private val scratchRect = Rect()

    init {
        if (mainView.isAttachedToWindow) {
            addAndInitSurfaceView()
        }
        mainView.addOnAttachStateChangeListener(viewListener)
    }

    @UiThread
    override fun requestDraw() {
        canvasFrontBufferedRendererWrapper.renderFrontBufferedLayer()
    }

    @WorkerThread
    override fun prepareToDrawInModifiedRegion(modifiedRegionInMainView: MutableBox) {
        assertOnRenderThread()
        check(onDrawState.duringDraw) { "Can only prepare to render during Callback.onDraw." }
        val frontBufferCanvas = checkNotNull(onDrawState.frontBufferCanvas)
        // Save the previous clip state. Restored in `afterDrawInModifiedRegion`.
        frontBufferCanvas.save()

        if (useOffScreenFrameBuffer) {
            val offScreenCanvas = checkNotNull(offScreenFrameBuffer).beginRecording()
            offScreenCanvas.save()
            onDrawState.offScreenCanvas = offScreenCanvas
        }

        // Set the clip to only apply changes to the modified region.
        // Clip uses integers, so round floats in a way that makes sure the entire updated region
        // is captured. For the starting point (smallest values) round down, and for the ending
        // point
        // (largest values) round up. Pad the region a bit to avoid potential rounding errors
        // leading to
        // stray artifacts.
        val clipRegionOutset = renderer.strokeModifiedRegionOutsetPx()

        // Make sure to set the clip region for both the offscreen canvas and the front buffer
        // canvas.
        // The offscreen canvas is where the stroke draw operations are going first, so clipping
        // ensures that the minimum number of draw operations are being performed. And when the off
        // screen canvas is being drawn over to the front buffer canvas, the offscreen canvas only
        // has
        // content within the clip region, so setting the same clip region on the front buffer
        // canvas
        // ensures that only that region is copied over - both for performance to avoid copying an
        // entire screen-sized buffer, but also for correctness to ensure that the retained contents
        // of
        // the front buffer outside of the modified region aren't cleared.
        scratchRect.set(
            /* left = */ floor(modifiedRegionInMainView.xMin).toInt() - clipRegionOutset,
            /* top = */ floor(modifiedRegionInMainView.yMin).toInt() - clipRegionOutset,
            /* right = */ ceil(modifiedRegionInMainView.xMax).toInt() + clipRegionOutset,
            /* bottom = */ ceil(modifiedRegionInMainView.yMax).toInt() + clipRegionOutset,
        )
        frontBufferCanvas.clipRect(scratchRect)
        // Using RenderNode.setClipRect instead of Canvas.clipRect for the offscreen frame buffer
        // works
        // better. With the latter, the clipping region would sometimes appear a little behind where
        // it
        // should be. If the RenderNode of the front buffer were available with
        // CanvasFrontBufferedRenderer, then it would be preferred to set the clipping region on
        // that
        // instead of on frontBufferCanvas above. Note that setting the clipping region on the
        // RenderNode for both the front buffer and offscreen frame buffer is the strategy used by
        // the
        // v33 implementation of this class.
        if (useOffScreenFrameBuffer) {
            checkNotNull(offScreenFrameBuffer).setClipRect(scratchRect)
        }

        // Clear the updated region of the offscreen frame buffer rather than the front buffer
        // because
        // the entire updated region will be copied from the former to the latter anyway. This way,
        // the
        // clear and draw operations will appear as one data-copying operation to the front buffer,
        // rather than as two separate operations. As two separate operations, the time between the
        // two
        // can be visible due to scanline racing, which can cause parts of the background to peek
        // through the content being rendered.
        val canvasToClear =
            if (useOffScreenFrameBuffer) {
                checkNotNull(onDrawState.offScreenCanvas)
            } else {
                frontBufferCanvas
            }
        canvasToClear.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    @WorkerThread
    override fun drawInModifiedRegion(
        inProgressStroke: InProgressStroke,
        strokeToMainViewTransform: Matrix,
    ) {
        assertOnRenderThread()
        check(onDrawState.duringDraw) { "Can only render during Callback.onDraw." }

        renderer.draw(
            if (useOffScreenFrameBuffer) {
                checkNotNull(onDrawState.offScreenCanvas)
            } else {
                checkNotNull(onDrawState.frontBufferCanvas)
            },
            inProgressStroke,
            strokeToMainViewTransform,
        )
    }

    @WorkerThread
    override fun afterDrawInModifiedRegion() {
        assertOnRenderThread()
        check(onDrawState.duringDraw) { "Can only finalize rendering during Callback.onDraw." }
        val frontBufferCanvas = checkNotNull(onDrawState.frontBufferCanvas)

        if (useOffScreenFrameBuffer) {
            val offScreenRenderNode = checkNotNull(offScreenFrameBuffer)

            // Previously saved in `prepareToDrawInModifiedRegion`.
            checkNotNull(onDrawState.offScreenCanvas).restore()

            offScreenRenderNode.endRecording()
            check(offScreenRenderNode.hasDisplayList())

            // offScreenRenderNode is configured with BlendMode=SRC so that drawRenderNode replaces
            // the
            // contents of the front buffer with the contents of the offscreen frame buffer, within
            // the
            // clip bounds set in `prepareToDrawInModifiedRegion` above.
            frontBufferCanvas.drawRenderNode(offScreenRenderNode)

            offScreenRenderNode.setClipRect(null)
            onDrawState.offScreenCanvas = null
        }

        // Previously saved in `prepareToDrawInModifiedRegion`.
        frontBufferCanvas.restore()
    }

    @WorkerThread
    override fun clear() {
        assertOnRenderThread()
        check(onDrawState.duringDraw) { "Can only clear during Callback.onDraw." }

        checkNotNull(onDrawState.frontBufferCanvas)
            .drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    @UiThread
    override fun requestStrokeCohortHandoffToHwui(
        handingOff: Map<InProgressStrokeId, FinishedStroke>
    ) {
        frontBufferToHwuiHandoff.requestCohortHandoff(handingOff)
    }

    @WorkerThread
    override fun assertOnRenderThread() {
        check(::renderThread.isInitialized) { "Don't yet know how to identify the render thread." }
        check(Thread.currentThread() == renderThread) {
            "Should be running on the render thread, but instead running on ${Thread.currentThread()}."
        }
    }

    @WorkerThread
    private fun ensureOffScreenFrameBuffer(width: Int, height: Int) {
        assertOnRenderThread()
        check(useOffScreenFrameBuffer)
        val existingBuffer = offScreenFrameBuffer
        if (
            existingBuffer != null &&
                existingBuffer.width == width &&
                existingBuffer.height == height
        ) {
            // The existing buffer still works, use it.
            return
        }
        offScreenFrameBuffer =
            RenderNode(CanvasInProgressStrokesRenderHelperV29::class.java.simpleName + "-OffScreen")
                .apply {
                    setPosition(0, 0, width, height)
                    setHasOverlappingRendering(true)
                    // Use BlendMode=SRC so that the contents of the offscreen frame buffer replace
                    // the
                    // contents of the front buffer (restricted to the clip region).
                    setUseCompositingLayer(
                        /* forceToLayer= */ true,
                        /* paint= */ offScreenFrameBufferPaint
                    )
                }
    }

    @UiThread
    private fun addAndInitSurfaceView() {
        mainView.addView(
            surfaceView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        canvasFrontBufferedRendererWrapper.init(surfaceView, canvasFrontBufferedRendererCallback)
        frontBufferToHwuiHandoff.setup()

        // The Hardware Composer (HWC) does not render sRGB color space content correctly when
        // compositing the front buffer layer, so force both the front buffered renderer and HWUI to
        // work in the Display P3 color space in order to ensure that content looks the same when
        // handed
        // off from one to the other. This is also set on the front buffer layer itself from
        // onFrontBufferedLayerRenderComplete.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            canvasFrontBufferedRendererWrapper.setColorSpace(
                checkNotNull(ColorSpace.getFromDataSpace(DataSpace.DATASPACE_DISPLAY_P3))
            )
            if (mainView.display?.isWideColorGamut == true) {
                WindowFinder.findWindow(mainView)?.colorMode =
                    ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
            }
        }
    }

    @WorkerThread
    private fun recordRenderThreadIdentity() {
        if (!::renderThread.isInitialized) {
            renderThread = Thread.currentThread()
        }
        // Catch cases where the render thread changes since we recorded its identity.
        assertOnRenderThread()
    }

    /**
     * [CanvasFrontBufferedRenderer] is final, so use this for faking/mocking.
     *
     * @see CanvasFrontBufferedRenderer
     */
    internal interface CanvasFrontBufferedRendererWrapper {

        /** @see CanvasFrontBufferedRenderer */
        @UiThread fun init(surfaceView: SurfaceView, callback: Callback)

        @UiThread fun setColorSpace(colorSpace: ColorSpace)

        /** @see CanvasFrontBufferedRenderer.renderFrontBufferedLayer */
        @UiThread fun renderFrontBufferedLayer()

        /** @see CanvasFrontBufferedRenderer.release */
        @UiThread fun release(onReleaseComplete: (() -> Unit)? = null)

        /** @see CanvasFrontBufferedRenderer.Callback */
        interface Callback {

            /** @see CanvasFrontBufferedRenderer.Callback.onDrawFrontBufferedLayer */
            @WorkerThread
            fun onDrawFrontBufferedLayer(canvas: Canvas, bufferWidth: Int, bufferHeight: Int)

            /** @see CanvasFrontBufferedRenderer.Callback.onFrontBufferedLayerRenderComplete */
            @WorkerThread
            fun onFrontBufferedLayerRenderComplete(
                transactionSetDataSpace: (SurfaceControlCompat, Int) -> Unit,
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
            )
        }
    }

    /**
     * The real implementation based on [CanvasFrontBufferedRenderer], which is not intended to be
     * unit testable.
     */
    private class CanvasFrontBufferedRendererWrapperImpl : CanvasFrontBufferedRendererWrapper {
        private var delegate: CanvasFrontBufferedRenderer<Unit>? = null

        @UiThread
        override fun init(
            surfaceView: SurfaceView,
            callback: CanvasFrontBufferedRendererWrapper.Callback,
        ) {
            delegate =
                CanvasFrontBufferedRenderer(
                    surfaceView,
                    object : CanvasFrontBufferedRenderer.Callback<Unit> {
                        @WorkerThread
                        override fun onDrawFrontBufferedLayer(
                            canvas: Canvas,
                            bufferWidth: Int,
                            bufferHeight: Int,
                            param: Unit,
                        ) {
                            callback.onDrawFrontBufferedLayer(canvas, bufferWidth, bufferHeight)
                        }

                        // NewApi suppress: SurfaceControlCompat.Transaction already handles
                        // delegating to
                        // version-specific implementations for setDataSpace, so it doesn't need a
                        // compile-time RequiresApi check. We only execute setDataSpace on the
                        // high enough API versions where it does what we want.
                        @SuppressLint("NewApi")
                        @WorkerThread
                        override fun onFrontBufferedLayerRenderComplete(
                            frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                            transaction: SurfaceControlCompat.Transaction,
                        ) {
                            callback.onFrontBufferedLayerRenderComplete(
                                transaction::setDataSpace,
                                frontBufferedLayerSurfaceControl,
                            )
                        }

                        @WorkerThread
                        override fun onDrawMultiBufferedLayer(
                            canvas: Canvas,
                            bufferWidth: Int,
                            bufferHeight: Int,
                            params: Collection<Unit>,
                        ) {
                            // Do nothing - our code never calls commit().
                        }
                    },
                )
        }

        override fun setColorSpace(colorSpace: ColorSpace) {
            delegate?.colorSpace = colorSpace
        }

        @UiThread
        override fun renderFrontBufferedLayer() {
            delegate?.renderFrontBufferedLayer(Unit)
        }

        @UiThread
        override fun release(onReleaseComplete: (() -> Unit)?) {
            delegate?.release(cancelPending = true, onReleaseComplete)
            delegate = null
        }
    }
}
