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

import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RenderNode
import android.os.Build
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.graphics.withMatrix
import androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
import androidx.graphics.surface.SurfaceControlCompat
import androidx.ink.authoring.ExperimentalCustomShapeWorkflowApi
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressShape
import androidx.ink.authoring.InProgressShapeRenderer
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.geometry.MutableBox
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.ceil
import kotlin.math.floor

/**
 * An implementation of [InProgressStrokesRenderHelper] based on [CanvasFrontBufferedRenderer],
 * which allows for low-latency rendering that works on Android versions starting at
 * [android.os.Build.VERSION_CODES.Q] and before [android.os.Build.VERSION_CODES.TIRAMISU].
 *
 * @param mainView The [View] within which the front buffer should be constructed.
 * @param callback How to render the desired content within the front buffer.
 * @param renderer Draws individual stroke objects using [Canvas].
 * @param canvasFrontBufferedRendererAdapter Override the default only for testing.
 * @param frontBufferToHwuiHandoffFactory Override the default only for testing.
 */
@Suppress("ObsoleteSdkInt") // TODO(b/262911421): Should not need to suppress.
@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalLatencyDataApi::class, ExperimentalCustomShapeWorkflowApi::class)
internal class CanvasInProgressStrokesRenderHelperV29<
    ShapeSpecT : Any,
    InProgressShapeT : InProgressShape<ShapeSpecT, CompletedShapeT>,
    CompletedShapeT : Any,
>(
    private val mainView: ViewGroup,
    private val renderer: InProgressShapeRenderer<InProgressShapeT>,
    private val canvasFrontBufferedRendererAdapter: CanvasFrontBufferedRendererAdapter =
        CanvasFrontBufferedRendererWrapper(),
    frontBufferToHwuiHandoffFactory: ((SurfaceView) -> FrontBufferToHwuiHandoff<CompletedShapeT>)? =
        null,
) : InProgressStrokesRenderHelper<ShapeSpecT, InProgressShapeT, CompletedShapeT>() {

    // The front buffer is updated each time rather than cleared and completely redrawn every time
    // as
    // a performance optimization.
    override val contentsPreservedBetweenDraws = true

    override val supportsDebounce = true

    override val canSynchronouslyWaitForFlush = true

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
                canvasFrontBufferedRendererAdapter.release()
                mainView.removeView(surfaceView)
            }
        }

    private val beforeDrawRunnables = ConcurrentLinkedQueue<Runnable>()

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

    private val callbackAdapter =
        object : CanvasFrontBufferedRendererAdapter.CallbackAdapter {

            @WorkerThread
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                bufferWidth: Int,
                bufferHeight: Int,
            ) {
                while (beforeDrawRunnables.isNotEmpty()) {
                    beforeDrawRunnables.poll()?.run()
                }

                ensureOffScreenFrameBuffer(bufferWidth, bufferHeight)

                // Just in case save/restores get imbalanced among callbacks
                val originalSaveCount = canvas.saveCount

                onDrawState.frontBufferCanvas = canvas

                onDrawState.duringDraw = true
                callback.onDraw()
                onDrawState.duringDraw = false

                // NOMUTANTS -- Defensive programming to avoid bad state being used later.
                onDrawState.frontBufferCanvas = null

                // Clear the client-defined masked area.
                maskPath?.let { canvas.drawPath(it, maskPaint) }

                callback.onDrawComplete()
                check(canvas.saveCount == originalSaveCount) {
                    "Unbalanced saves and restores. Expected save count of $originalSaveCount, got ${canvas.saveCount}."
                }
            }

            @WorkerThread
            override fun onFrontBufferedLayerRenderComplete() {
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

    private val frontBufferToHwuiHandoff =
        frontBufferToHwuiHandoffFactory?.invoke(surfaceView)
            ?: FrontBufferToHwuiHandoff.create(
                mainView,
                surfaceView,
                // Lambdas instead of function references because we can't actually get the late-set
                // callback during init.
                { callback.onStrokeCohortHandoffToHwui(it) },
                { callback.onStrokeCohortHandoffToHwuiComplete() },
            )

    private var offScreenFrameBuffer: RenderNode? = null

    /**
     * Used for a call to [RenderNode.setUseCompositingLayer] and [Canvas.drawRenderNode] to
     * overwrite the contents of the front buffer with the offscreen frame buffer (limited to the
     * clip region).
     */
    private val offScreenFrameBufferPaint = createPaintForUnscaledBlit()

    private val scratchRect = Rect()

    init {
        check(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            "CanvasInProgressStrokesRenderHelperV29 requires Android Q+. After Android T, use " +
                "CanvasInProgressStrokesRenderHelperV33 instead."
        }
        if (mainView.isAttachedToWindow) {
            addAndInitSurfaceView()
        }
        mainView.addOnAttachStateChangeListener(viewListener)
    }

    @UiThread
    override fun requestDraw() {
        canvasFrontBufferedRendererAdapter.renderFrontBufferedLayer()
    }

    @WorkerThread
    override fun prepareToDrawInModifiedRegion(modifiedRegionInMainView: MutableBox) {
        assertOnRenderThread()
        check(onDrawState.duringDraw) { "Can only prepare to render during Callback.onDraw." }
        val frontBufferCanvas = checkNotNull(onDrawState.frontBufferCanvas)
        // Save the previous clip state. Restored in `afterDrawInModifiedRegion`.
        frontBufferCanvas.save()

        val offScreenCanvas = checkNotNull(offScreenFrameBuffer).beginRecording()
        offScreenCanvas.save()
        onDrawState.offScreenCanvas = offScreenCanvas

        // Set the clip to only apply changes to the modified region.
        // Clip uses integers, so round floats in a way that makes sure the entire updated region
        // is captured. For the starting point (smallest values) round down, and for the ending
        // point
        // (largest values) round up. Pad the region a bit to avoid potential rounding errors
        // leading to
        // stray artifacts.
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
            /* left = */ floor(modifiedRegionInMainView.xMin).toInt() - CLIP_REGION_OUTSET_PX,
            /* top = */ floor(modifiedRegionInMainView.yMin).toInt() - CLIP_REGION_OUTSET_PX,
            /* right = */ ceil(modifiedRegionInMainView.xMax).toInt() + CLIP_REGION_OUTSET_PX,
            /* bottom = */ ceil(modifiedRegionInMainView.yMax).toInt() + CLIP_REGION_OUTSET_PX,
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
        checkNotNull(offScreenFrameBuffer).setClipRect(scratchRect)

        // Clear the updated region of the offscreen frame buffer rather than the front buffer
        // because
        // the entire updated region will be copied from the former to the latter anyway. This way,
        // the
        // clear and draw operations will appear as one data-copying operation to the front buffer,
        // rather than as two separate operations. As two separate operations, the time between the
        // two
        // can be visible due to scanline racing, which can cause parts of the background to peek
        // through the content being rendered.
        checkNotNull(onDrawState.offScreenCanvas)
            .drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    @WorkerThread
    override fun drawInModifiedRegion(
        inProgressShape: InProgressShapeT,
        strokeToMainViewTransform: Matrix,
    ) {
        assertOnRenderThread()
        check(onDrawState.duringDraw) { "Can only render during Callback.onDraw." }

        val canvas = checkNotNull(onDrawState.offScreenCanvas)
        canvas.withMatrix(strokeToMainViewTransform) {
            renderer.draw(canvas, inProgressShape, strokeToMainViewTransform)
        }
    }

    @WorkerThread
    override fun afterDrawInModifiedRegion() {
        assertOnRenderThread()
        check(onDrawState.duringDraw) { "Can only finalize rendering during Callback.onDraw." }
        val frontBufferCanvas = checkNotNull(onDrawState.frontBufferCanvas)

        val offScreenRenderNode = checkNotNull(offScreenFrameBuffer)

        // Previously saved in `prepareToDrawInModifiedRegion`.
        checkNotNull(onDrawState.offScreenCanvas).restore()

        offScreenRenderNode.endRecording()
        check(offScreenRenderNode.hasDisplayList())

        // offScreenRenderNode is configured with BlendMode=SRC so that drawRenderNode replaces the
        // contents of the front buffer with the contents of the offscreen frame buffer, within the
        // clip bounds set in `prepareToDrawInModifiedRegion` above.
        frontBufferCanvas.drawRenderNode(offScreenRenderNode)

        offScreenRenderNode.setClipRect(null)
        onDrawState.offScreenCanvas = null

        // Previously saved in `prepareToDrawInModifiedRegion`.
        frontBufferCanvas.restore()
    }

    @WorkerThread
    override fun startCohort() {
        assertOnRenderThread()
        check(onDrawState.duringDraw) { "Can only clear during Callback.onDraw." }
        checkNotNull(onDrawState.frontBufferCanvas)
            .drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    @UiThread
    override fun requestStrokeCohortHandoffToHwui(cohort: List<FinishedStroke<CompletedShapeT>>) {
        frontBufferToHwuiHandoff.requestCohortHandoff(cohort)
    }

    @WorkerThread
    override fun assertOnRenderThread() {
        // Actually just checks that this is not on the UI thread.
        //
        // This implementation doesn't have control over its own thread, which is initialized by
        // CanvasFrontBufferedRenderer and can't be read from there. While we could try to record it
        // in
        // the callback and check that here, the old thread is released asynchronously (canceling
        // pending tasks, but still possibly waiting on in-progress tasks), so there's no guarantee
        // that
        // there aren't still callbacks in flight on the old thread when this assertion is called by
        // something on the new thread. Instead, just assert that we're not on the main thread,
        // which
        // will catch most of the cases where one of these methods is called from the wrong thread.
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Should not be running on the UI thread."
        }
    }

    @UiThread
    override fun executeOnRenderThread(runnable: Runnable) {
        assertOnUiThread()
        // TODO: b/486935851 - Unfortunately, CanvasFrontBufferedRenderer doesn't provide access to
        // its
        // Handler, so there's no way to run arbitrary callbacks on the render thread interleaved
        // with
        // draws. However, we can queue a runnable to execute before the next draw and request a
        // draw.
        beforeDrawRunnables.offer(runnable)
        requestDraw()
    }

    @WorkerThread
    private fun ensureOffScreenFrameBuffer(width: Int, height: Int) {
        assertOnRenderThread()
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
                    // The Paint ensures that the contents of the offscreen frame buffer will
                    // replace the
                    // contents of the front buffer (restricted to the clip region).
                    setUseCompositingLayer(
                        /* forceToLayer= */ true,
                        /* paint= */ offScreenFrameBufferPaint,
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
        canvasFrontBufferedRendererAdapter.init(surfaceView, callbackAdapter)
        if (beforeDrawRunnables.isNotEmpty()) {
            requestDraw()
        }
        frontBufferToHwuiHandoff.setup()
    }

    /**
     * [CanvasFrontBufferedRenderer] is final, so use this for faking/mocking.
     *
     * @see CanvasFrontBufferedRenderer
     */
    internal interface CanvasFrontBufferedRendererAdapter {

        /** @see CanvasFrontBufferedRenderer */
        @UiThread fun init(surfaceView: SurfaceView, callbackAdapter: CallbackAdapter)

        /** @see CanvasFrontBufferedRenderer.renderFrontBufferedLayer */
        @UiThread fun renderFrontBufferedLayer()

        /** @see CanvasFrontBufferedRenderer.release */
        @UiThread fun release()

        /** @see CanvasFrontBufferedRenderer.Callback */
        interface CallbackAdapter {

            /** @see CanvasFrontBufferedRenderer.Callback.onDrawFrontBufferedLayer */
            @WorkerThread
            fun onDrawFrontBufferedLayer(canvas: Canvas, bufferWidth: Int, bufferHeight: Int)

            /** @see CanvasFrontBufferedRenderer.Callback.onFrontBufferedLayerRenderComplete */
            @WorkerThread fun onFrontBufferedLayerRenderComplete()
        }
    }

    /**
     * The real implementation based on [CanvasFrontBufferedRenderer], which is not intended to be
     * unit testable. Since this is faked out in tests, minimize the amount of logic put in this
     * wrapper.
     */
    private class CanvasFrontBufferedRendererWrapper : CanvasFrontBufferedRendererAdapter {
        private var delegate: CanvasFrontBufferedRenderer<Unit>? = null

        @UiThread
        override fun init(
            surfaceView: SurfaceView,
            callbackAdapter: CanvasFrontBufferedRendererAdapter.CallbackAdapter,
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
                            callbackAdapter.onDrawFrontBufferedLayer(
                                canvas,
                                bufferWidth,
                                bufferHeight,
                            )
                        }

                        @WorkerThread
                        override fun onFrontBufferedLayerRenderComplete(
                            frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                            transaction: SurfaceControlCompat.Transaction,
                        ) {
                            callbackAdapter.onFrontBufferedLayerRenderComplete()
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

        @UiThread
        override fun renderFrontBufferedLayer() {
            delegate?.renderFrontBufferedLayer(Unit)
        }

        @UiThread
        override fun release() {
            delegate?.release(cancelPending = true)
            delegate = null
        }
    }

    private companion object {
        /**
         * Number of pixels to widen the transformed region by, in order to better guarantee that no
         * pixels are cut off during incremental draws that modify the smallest possible rectangle.
         */
        @Px const val CLIP_REGION_OUTSET_PX = 3
    }
}
