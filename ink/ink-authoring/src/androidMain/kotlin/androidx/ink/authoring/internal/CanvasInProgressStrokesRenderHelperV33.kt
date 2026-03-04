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
import android.graphics.Rect
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AnyThread
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.graphics.withMatrix
import androidx.graphics.CanvasBufferedRenderer
import androidx.graphics.surface.SurfaceControlCompat
import androidx.hardware.SyncFenceCompat
import androidx.ink.authoring.ExperimentalCustomShapeWorkflowApi
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressShape
import androidx.ink.authoring.InProgressShapeRenderer
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.geometry.MutableBox
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil
import kotlin.math.floor

/**
 * An implementation of [InProgressStrokesRenderHelper] based on [CanvasBufferedRenderer] and
 * [SurfaceControlCompat], which allow for low-latency rendering in Android versions starting at
 * [android.os.Build.VERSION_CODES.TIRAMISU]. Compared to [CanvasInProgressStrokesRenderHelperV29],
 * this implementation has stronger guarantees about avoiding flickers, and implements handoff of
 * strokes to a HWUI-based client in a more efficient way that minimizes the time when input
 * handling and rendering are frozen.
 *
 * @param mainView The [View] within which the front buffer should be constructed.
 * @param callback How to render the desired content within the front buffer.
 * @param renderer Draws individual stroke objects using [Canvas].
 * @param uiThreadExecutor Replace the default for testing only.
 */
@Suppress("ObsoleteSdkInt") // TODO(b/262911421): Should not need to suppress.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalLatencyDataApi::class, ExperimentalCustomShapeWorkflowApi::class)
internal class CanvasInProgressStrokesRenderHelperV33<
    ShapeSpecT : Any,
    InProgressShapeT : InProgressShape<ShapeSpecT, CompletedShapeT>,
    CompletedShapeT : Any,
>(
    private val mainView: ViewGroup,
    private val renderer: InProgressShapeRenderer<InProgressShapeT>,
    private val uiThreadExecutor: ScheduledExecutor =
        Looper.getMainLooper().let { looper ->
            ScheduledExecutorImpl(looper.thread, Handler(looper))
        },
    private val renderThreadExecutor: ScheduledExecutor =
        HandlerThread(CanvasInProgressStrokesRenderHelperV33::class.java.simpleName + "_Render")
            .let {
                it.start()
                ScheduledExecutorImpl(it, Handler(it.looper))
            },
) : InProgressStrokesRenderHelper<ShapeSpecT, InProgressShapeT, CompletedShapeT>() {

    override val contentsPreservedBetweenDraws = true

    override val supportsDebounce = true

    override val canSynchronouslyWaitForFlush = true

    @VisibleForTesting internal var countDownAfterHandoffsResumedTestLatch: CountDownLatch? = null

    private val surfaceView =
        SurfaceView(mainView.context).apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSLUCENT)
        }

    private var currentViewport: Viewport? = null

    /** Counts deferred calls to [requestDraw]. */
    private val deferredDrawCalls = AtomicInteger(0)

    private fun AtomicInteger.getAndDecrementIfPositive() = getAndUpdate {
        if (it > 0) it - 1 else 0
    }

    private val viewListener =
        object : View.OnAttachStateChangeListener {
            @UiThread
            override fun onViewAttachedToWindow(v: View) {
                addAndInitSurfaceView()
            }

            @UiThread
            override fun onViewDetachedFromWindow(v: View) {
                mainView.removeView(surfaceView)
            }
        }

    private val surfaceListener =
        object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) = Unit

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
                if (width == 0 || height == 0) {
                    onViewHiddenOrNoBounds()
                } else {
                    onViewVisibleWithBounds(width, height)
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                onViewHiddenOrNoBounds()
            }

            override fun surfaceRedrawNeeded(holder: SurfaceHolder) = Unit
        }

    /**
     * Defined as a lambda instead of a member function or companion object function to ensure that
     * no extra allocation takes place when passing this function object into the higher-level
     * callback.
     */
    private val finishesDrawCallsSetter = { data: LatencyData, timeNanos: Long ->
        data.canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = timeNanos
    }

    private val maskPaint =
        Paint().apply {
            color = Color.TRANSPARENT
            blendMode = BlendMode.CLEAR
        }

    /**
     * Used for a call to [RenderNode.setUseCompositingLayer] and [Canvas.drawRenderNode] to
     * overwrite the contents of the front buffer with the offscreen frame buffer (limited to the
     * clip region).
     */
    private val offScreenFrameBufferPaint = createPaintForUnscaledBlit()

    init {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "CanvasInProgressStrokesRenderHelperV33 requires Android T+."
        }
        if (mainView.isAttachedToWindow) {
            addAndInitSurfaceView()
        }
        mainView.addOnAttachStateChangeListener(viewListener)
    }

    @WorkerThread
    override fun assertOnRenderThread() {
        check(renderThreadExecutor.onThread()) {
            "Should be running on render thread, but actually running on ${Thread.currentThread()}."
        }
    }

    @UiThread
    override fun executeOnRenderThread(runnable: Runnable) {
        assertOnUiThread()
        renderThreadExecutor.execute(runnable)
    }

    @UiThread
    override fun requestDraw() {
        currentViewport?.requestDraw() ?: deferredDrawCalls.getAndIncrement()
    }

    @WorkerThread
    override fun prepareToDrawInModifiedRegion(modifiedRegionInMainView: MutableBox) {
        currentViewport?.prepareToDrawInModifiedRegion(modifiedRegionInMainView)
    }

    @WorkerThread
    override fun drawInModifiedRegion(
        inProgressShape: InProgressShapeT,
        strokeToMainViewTransform: Matrix,
    ) {
        currentViewport?.drawInModifiedRegion(inProgressShape, strokeToMainViewTransform)
    }

    @WorkerThread
    override fun afterDrawInModifiedRegion() {
        currentViewport?.afterDrawInModifiedRegion()
    }

    @UiThread
    override fun requestStrokeCohortHandoffToHwui(cohort: List<FinishedStroke<CompletedShapeT>>) {
        currentViewport?.requestHandoff(cohort)
    }

    @WorkerThread
    override fun startCohort() {
        // Nothing to be done here, we've swapped in the already-cleared inactive buffer already.
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
        surfaceView.holder.addCallback(surfaceListener)
    }

    @UiThread
    private fun onViewVisibleWithBounds(width: Int, height: Int) {
        assertOnUiThread()
        val oldViewport = currentViewport
        val newBounds = getNewBoundsIfChanged(oldViewport?.bounds, width, height) ?: return
        oldViewport?.discard()
        val newViewport = Viewport(newBounds)
        currentViewport = newViewport
        // If any calls to requestDraw came in before there was a viewport, process them now.
        if (deferredDrawCalls.getAndDecrementIfPositive() > 0) {
            // Just kick off one, it will chain another if there is more than one.
            newViewport.requestDraw()
        }
    }

    @UiThread
    private fun onViewHiddenOrNoBounds() {
        assertOnUiThread()
        currentViewport?.discard()
        currentViewport = null
    }

    private fun getNewBoundsIfChanged(oldBounds: Bounds?, newWidth: Int, newHeight: Int): Bounds? {
        assertOnUiThread()
        val transformHint = checkNotNull(mainView.rootSurfaceControl).bufferTransformHint
        if (
            oldBounds != null &&
                oldBounds.mainViewWidth == newWidth &&
                oldBounds.mainViewHeight == newHeight &&
                oldBounds.mainViewTransformHint == transformHint
        ) {
            return null
        }
        return Bounds(newWidth, newHeight, transformHint)
    }

    private inner class Viewport(val bounds: Bounds) {

        /**
         * When a [Viewport] is no longer valid (e.g. when the [Bounds] change), this will be set to
         * `true`, so that any in-flight callbacks don't execute on this now-invalid object.
         */
        private val discarded = AtomicBoolean(false)

        private val buffersState =
            object {
                private val stateInternal = AtomicReference<BuffersState?>()

                @AnyThread
                fun checkAndSet(expectedValue: BuffersState?, newValue: BuffersState?) {
                    check(stateInternal.compareAndSet(expectedValue, newValue)) {
                        "buffersState: expected $expectedValue, but current value is ${stateInternal.get()}"
                    }
                }

                @UiThread
                fun getAndSet(newValue: BuffersState?): BuffersState? {
                    assertOnUiThread()
                    return stateInternal.getAndSet(newValue)
                }

                @AnyThread fun get(): BuffersState? = stateInternal.get()
            }

        /**
         * The next value to pass to [SurfaceControlCompat.Transaction.setLayer]. This increases
         * with each handoff to ensure that the clear inactive buffer, ready to be activated and
         * drawn on, is always showing above the active layer with content, so that later strokes
         * are always drawn over earlier strokes. A positive number means drawing above the parent
         * layer ([SurfaceView]), which is why we start with a value of 1.
         *
         * While this could theoretically overflow, it is incremented once per handoff. A handoff
         * must contain at least one stroke, so it would take at least 2^31 strokes to cause this to
         * overflow - well above our target number for an entire document of 10k. Because handoffs
         * must have at least 500 milliseconds between them, 2^31 handoffs spaced 500 milliseconds
         * apart would take about 34 years. And if an overflow did occur, the outcome is that for
         * the brief moment when both front buffer layers are on screen simultaneously with content,
         * the more recent layer (with more recent strokes) will appear below the older layer (with
         * older strokes).
         */
        private val nextBufferLayer = AtomicInteger(1)

        private val renderThreadState =
            object {
                var drawingTo: BufferData? = null
                var frontBufferCanvas: Canvas? = null
                val scratchRect = Rect()
                /**
                 * Only valid from [prepareToDrawInModifiedRegion] to [afterDrawInModifiedRegion].
                 */
                var offScreenCanvas: Canvas? = null
            }

        private val frontBufferIncrementalRenderCallback =
            { renderResult: CanvasBufferedRenderer.RenderResult ->
                assertOnRenderThread()
                if (!discarded.get()) {
                    onFrontBufferIncrementalRenderResult(renderResult.hardwareBuffer)
                }
            }

        init {
            assertOnUiThread()
            val initialState =
                BuffersState(
                    active = createBufferData(bufferNumber = 1),
                    inactive = createBufferData(bufferNumber = 2),
                    inactiveIsReady = false,
                )
            val active = initialState.active
            callback.pauseStrokeCohortHandoffs()
            active.renderer
                .obtainRenderRequest()
                .preserveContents(true)
                .setTransformFromBounds(bounds)
                .drawAsync(renderThreadExecutor) { renderResult ->
                    if (discarded.get()) return@drawAsync
                    buffersState.checkAndSet(expectedValue = null, initialState)
                    SurfaceControlCompat.Transaction()
                        .setAndShow(active, renderResult.hardwareBuffer, renderResult.fence)
                        .addTransactionCommittedListener(
                            renderThreadExecutor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    if (discarded.get()) return
                                    // Some draws might have been interrupted by buffersState not
                                    // being set yet, so
                                    // give a chance to kick off if pending. That state is managed
                                    // by the render
                                    // thread.
                                    if (deferredDrawCalls.getAndDecrementIfPositive() > 0) {
                                        // Just kick off one, it will chain another if needed.
                                        handleDraw()
                                    }
                                    // This clears the inactive buffer to transparent and moves it
                                    // on screen on top of
                                    // the active buffer, ready for a seamless handoff. The same
                                    // logic is used here
                                    // for initial setup as is used when a handoff happens and the
                                    // new inactive buffer
                                    // (the previous active buffer) is made ready for use again.
                                    onInactiveBufferHidden()
                                }
                            },
                        )
                        .addTransactionCommittedListener(
                            uiThreadExecutor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    if (discarded.get()) return
                                    mainView.invalidate()
                                }
                            },
                        )
                        .commit()
                }
        }

        // This runnable is used per-frame (not just per-handoff), so caching to avoid allocation of
        // a
        // method reference each time.
        private val handleDrawRunnable = Runnable { handleDraw() }

        fun createBufferData(bufferNumber: Int): BufferData {
            val renderer = createRenderer()
            val debugName = createDebugName(bufferNumber)
            // The actual draw instructions go into the offscreen RenderNode, while the front buffer
            // RenderNode simply copies the contents of the content RenderNode but with the
            // appropriate
            // clip rectangle applied to ensure that the copy is only of the modified part of the
            // buffer.
            val offScreenRenderNode =
                createRenderNode("$debugName-OffScreen").apply {
                    setHasOverlappingRendering(true)
                    // The Paint ensures that the contents of the offscreen frame buffer will
                    // replace the
                    // contents of the front buffer (restricted to the clip region).
                    setUseCompositingLayer(
                        /* forceToLayer= */ true,
                        /* paint= */ offScreenFrameBufferPaint,
                    )
                }
            val frontBufferRenderNode = createRenderNode("$debugName-Front")
            renderer.setContentRoot(frontBufferRenderNode)
            return BufferData(
                SurfaceControlCompat.Builder().setName(debugName).setParent(surfaceView).build(),
                renderer,
                frontBufferRenderNode,
                offScreenRenderNode,
            )
        }

        private fun createDebugName(bufferNumber: Int) =
            "$bufferNumber-${CanvasInProgressStrokesRenderHelperV33::class.java.simpleName}"

        private fun createRenderNode(name: String): RenderNode =
            RenderNode(name).apply {
                // Make RenderNode coordinates the same as the View coordinates.
                setPosition(0, 0, bounds.mainViewWidth, bounds.mainViewHeight)
            }

        private fun createRenderer(): CanvasBufferedRenderer {
            val usageFlags =
                if (
                    HardwareBuffer.isSupported(
                        1,
                        1,
                        HardwareBuffer.RGBA_8888,
                        1,
                        DESIRED_USAGE_FLAGS,
                    )
                ) {
                    DESIRED_USAGE_FLAGS
                } else {
                    ALTERNATE_USAGE_FLAGS
                }
            return CanvasBufferedRenderer.Builder(bounds.bufferWidth, bounds.bufferHeight)
                .setMaxBuffers(1)
                .setBufferFormat(HardwareBuffer.RGBA_8888)
                .setUsageFlags(usageFlags)
                .build()
        }

        private fun SurfaceControlCompat.Transaction.setAndShow(
            bufferData: BufferData,
            hardwareBuffer: HardwareBuffer,
            initialRenderFence: SyncFenceCompat?,
        ): SurfaceControlCompat.Transaction {
            val sc = bufferData.surfaceControl
            setVisibility(sc, true)
            // Pass the initial render SyncFence here to ensure that the buffer doesn't appear on
            // screen
            // until it has been cleared, just in case it contains garbage data (apparently may
            // happen on
            // certain devices).
            setBuffer(sc, hardwareBuffer, initialRenderFence)
            setLayer(sc, nextBufferLayer.getAndIncrement())
            setFrameRate(
                sc,
                1000F,
                SurfaceControlCompat.FRAME_RATE_COMPATIBILITY_DEFAULT,
                SurfaceControlCompat.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS,
            )
            setPosition(sc, 0F, 0F)

            setTransformFromBounds(sc, bounds)

            return this
        }

        @Suppress("ScopeReceiverThis") // Extending Transaction which uses builder pattern
        private fun SurfaceControlCompat.Transaction.setTransformFromBounds(
            sc: SurfaceControlCompat,
            bounds: Bounds,
        ) = apply { bounds.surfaceTransform?.let { setBufferTransform(sc, it) } }

        @Suppress("ScopeReceiverThis") // Extending RenderRequest which uses builder pattern
        private fun CanvasBufferedRenderer.RenderRequest.setTransformFromBounds(bounds: Bounds) =
            apply {
                bounds.rendererTransform?.let { setBufferTransform(it) }
            }

        /* Dispatches a draw request to the render thread. */
        @UiThread
        fun requestDraw() {
            assertOnUiThread()
            // This instead of ::handleDraw because this instance is cached and method refs
            // allocate.
            renderThreadExecutor.execute(handleDrawRunnable)
        }

        /** Handles a draw request on the render thread. */
        @WorkerThread
        fun handleDraw() {
            assertOnRenderThread()
            val state = buffersState.get()
            if (discarded.get() || state == null || renderThreadState.drawingTo != null) {
                deferredDrawCalls.getAndIncrement()
                return
            }
            val activeBuffer = state.active
            renderThreadState.drawingTo = activeBuffer
            val frontBufferRenderNode = activeBuffer.frontBufferRenderNode
            val frontBufferCanvas = frontBufferRenderNode.beginRecording()

            // Just in case save/restores get imbalanced among callbacks
            val originalSaveCount = frontBufferCanvas.saveCount

            renderThreadState.frontBufferCanvas = frontBufferCanvas
            callback.onDraw()
            renderThreadState.frontBufferCanvas = null

            // Clear the client-defined masked area.
            maskPath?.let { frontBufferCanvas.drawPath(it, maskPaint) }

            callback.onDrawComplete()
            // Check that the save/restore count is balanced, if we haven't bailed out of the draw
            // early
            // due to the view being discarded.
            check(discarded.get() || frontBufferCanvas.saveCount == originalSaveCount) {
                "Unbalanced saves and restores. Expected save count of $originalSaveCount, " +
                    "got ${frontBufferCanvas.saveCount}."
            }

            frontBufferRenderNode.endRecording()

            activeBuffer.renderer
                .obtainRenderRequest()
                .preserveContents(true)
                .setTransformFromBounds(bounds)
                .drawAsync(renderThreadExecutor, frontBufferIncrementalRenderCallback)
        }

        @WorkerThread
        private fun onFrontBufferIncrementalRenderResult(hardwareBuffer: HardwareBuffer) {
            assertOnRenderThread()
            val state = buffersState.get() ?: return
            val activeBuffer = state.active
            // A handoff may have occurred while this front buffer render was still in progress.
            // Only
            // apply the new buffer to the active layer if it was the layer that this draw was
            // actually
            // initiated for. Applying the inactive layer's buffer to the active layer will cause
            // visual
            // inconsistencies. And we're better off not applying this transaction to the inactive
            // layer,
            // as there may be a transaction in progress to hide the inactive buffer in sync with
            // HWUI
            // rendering, and this transaction might interfere with that one which would cause a
            // flicker.
            // It's more likely that the inactive layer is already off screen anyway, but don't risk
            // the
            // flicker.
            if (renderThreadState.drawingTo == activeBuffer) {
                val sc = activeBuffer.surfaceControl
                SurfaceControlCompat.Transaction()
                    // For an incremental render like this, don't wait on a fence to show the buffer
                    // - get it
                    // in place for the display to read from it as soon as possible, so the render
                    // races the
                    // scanline.
                    .setBuffer(sc, hardwareBuffer, fence = null)
                    .setTransformFromBounds(sc, bounds)
                    .commit()
            }
            callback.setCustomLatencyDataField(finishesDrawCallsSetter)
            callback.handOffAllLatencyData()
            renderThreadState.drawingTo = null
            if (deferredDrawCalls.getAndDecrementIfPositive() > 0) {
                handleDraw()
            }
        }

        @WorkerThread
        fun prepareToDrawInModifiedRegion(modifiedRegionInMainView: MutableBox) {
            val frontBufferRenderNode =
                checkNotNull(renderThreadState.drawingTo).frontBufferRenderNode
            // Can only prepare to render during Callback.onDraw.
            val frontBufferCanvas = checkNotNull(renderThreadState.frontBufferCanvas)
            // Save the previous clip state. Restored in `afterDrawInModifiedRegion`.
            frontBufferCanvas.save()

            val offScreenRenderNode = checkNotNull(renderThreadState.drawingTo).offScreenRenderNode
            val offScreenCanvas = offScreenRenderNode.beginRecording()
            offScreenCanvas.save()
            renderThreadState.offScreenCanvas = offScreenCanvas

            // Set the clip to only apply changes to the modified region.
            // Clip uses integers, so round floats in a way that makes sure the entire updated
            // region
            // is captured. For the starting point (smallest values) round down, and for the ending
            // point
            // (largest values) round up. Pad the region a bit to avoid potential rounding errors
            // leading
            // to stray artifacts.
            renderThreadState.scratchRect.set(
                /* left = */ floor(modifiedRegionInMainView.xMin).toInt() - CLIP_REGION_OUTSET_PX,
                /* top = */ floor(modifiedRegionInMainView.yMin).toInt() - CLIP_REGION_OUTSET_PX,
                /* right = */ ceil(modifiedRegionInMainView.xMax).toInt() + CLIP_REGION_OUTSET_PX,
                /* bottom = */ ceil(modifiedRegionInMainView.yMax).toInt() + CLIP_REGION_OUTSET_PX,
            )
            // Make sure to set the clip region for both the off screen canvas and the front buffer
            // canvas. The off screen canvas is where the stroke draw operations are going first, so
            // clipping ensures that the minimum number of draw operations are being performed. And
            // when
            // the off screen canvas is being drawn over to the front buffer canvas, the off screen
            // canvas
            // only has content within the clip region, so setting the same clip region on the front
            // buffer canvas ensures that only that region is copied over - both for performance to
            // avoid
            // copying an entire screen-sized buffer, but also for correctness to ensure that the
            // retained
            // contents of the front buffer outside of the modified region aren't cleared.
            // Use RenderNode.setClipRect instead of Canvas.clipRect to avoid visual artifacts that
            // were
            // appearing with the latter.
            frontBufferRenderNode.setClipRect(renderThreadState.scratchRect)
            offScreenRenderNode.setClipRect(renderThreadState.scratchRect)

            // Clear the updated region of the offscreen frame buffer rather than the front buffer
            // because
            // the entire updated region will be copied from the former to the latter anyway. This
            // way,
            // the clear and draw operations will appear as one data-copying operation to the front
            // buffer, rather than as two separate operations. As two separate operations, the time
            // between the two can be visible due to scanline racing, which can cause parts of the
            // background to peek through the content being rendered.
            offScreenCanvas.drawColor(Color.TRANSPARENT, BlendMode.CLEAR)
        }

        @WorkerThread
        fun drawInModifiedRegion(
            inProgressShape: InProgressShapeT,
            strokeToMainViewTransform: Matrix,
        ) {
            val canvas = checkNotNull(renderThreadState.offScreenCanvas)
            canvas.withMatrix(strokeToMainViewTransform) {
                renderer.draw(canvas, inProgressShape, strokeToMainViewTransform)
            }
        }

        @WorkerThread
        fun afterDrawInModifiedRegion() {
            // Can only finalize rendering during Callback.onDraw.
            val frontBufferRenderNode =
                checkNotNull(renderThreadState.drawingTo).frontBufferRenderNode
            val frontBufferCanvas = checkNotNull(renderThreadState.frontBufferCanvas)
            val offScreenRenderNode = checkNotNull(renderThreadState.drawingTo).offScreenRenderNode

            // Previously saved in `prepareToDrawInModifiedRegion`.
            checkNotNull(renderThreadState.offScreenCanvas).restore()

            offScreenRenderNode.endRecording()
            check(offScreenRenderNode.hasDisplayList())

            // offScreenRenderNode is configured with BlendMode=SRC so that drawRenderNode replaces
            // the
            // contents of the front buffer with the contents of the off screen frame buffer, within
            // the
            // clip bounds set in `prepareToDrawInModifiedRegion` above.
            frontBufferCanvas.drawRenderNode(offScreenRenderNode)

            offScreenRenderNode.setClipRect(null)
            frontBufferRenderNode.setClipRect(null)
            renderThreadState.offScreenCanvas = null

            // Previously saved in `prepareToDrawInModifiedRegion`.
            frontBufferCanvas.restore()
        }

        @UiThread
        fun requestHandoff(cohort: List<FinishedStroke<CompletedShapeT>>) {
            assertOnUiThread()
            val state = buffersState.get() ?: return
            check(state.inactiveIsReady) {
                "Handoffs should be paused until the inactive buffer is ready again."
            }

            val rootSurfaceControl = checkNotNull(mainView.rootSurfaceControl)
            // Pause handoffs until the current buffer is fully off screen and cleared, because if
            // the
            // contents of the next buffer also need to be handed off then there will be no buffer
            // to
            // draw new inputs.
            callback.pauseStrokeCohortHandoffs()

            // The inactive buffer can't be cleared immediately in addTransactionCommittedListener
            // because
            // that's called when the transaction is committed, not when the result is actually
            // displayed.
            //
            // TODO: b/474652729 - Use
            // SurfaceControlCompat.Transaction.addTransactionCompletedListener
            // instead once that is implemented.
            //
            // The delay time is practically guaranteed to be long enough for the previously active
            // buffer to be hidden, so that it can be safely cleared and moved back on screen. This
            // should
            // only take about 1-5 vsync periods to get through the HWUI pipeline, which with a 60Hz
            // display refresh rate is about 16-80ms. But with the approach of this RenderHelper
            // using two
            // front buffers, there is limited benefit to making this time as tight as possible,
            // since it
            // doesn't delay the next inputs from being drawn, it just prevents another handoff from
            // occurring until this timer fires to consider the current handoff to be completed. If
            // Android offered a callback/fence to indicate when a particular transaction has been
            // presented to the display, then this delay could be based on that for tighter timing.
            val sc = state.active.surfaceControl
            SurfaceControlCompat.Transaction()
                .setVisibility(sc, false)
                .setBuffer(sc, null)
                .addTransactionCommittedListener(
                    renderThreadExecutor,
                    object : SurfaceControlCompat.TransactionCommittedListener {
                        override fun onTransactionCommitted() {
                            if (discarded.get()) return
                            renderThreadExecutor.executeDelayed(
                                { onInactiveBufferHidden() },
                                500,
                                TimeUnit.MILLISECONDS,
                            )
                        }
                    },
                )
                .commitTransactionOnDraw(rootSurfaceControl)
            // Since the inactive buffer is ready for drawing right away, we can start drawing on it
            // immediately. So swap the active and inactive buffers in the state, hand off the
            // cohort, and
            // allow drawing to resume right away.
            val newState =
                BuffersState(
                    active = state.inactive,
                    inactive = state.active,
                    inactiveIsReady = false,
                )
            buffersState.checkAndSet(state, newState)
            mainView.invalidate()
            callback.onStrokeCohortHandoffToHwui(cohort)
            callback.onStrokeCohortHandoffToHwuiComplete()
        }

        @WorkerThread
        fun onInactiveBufferHidden() {
            // This must be done on the render thread to allow a handoff to be completed and a new
            // handoff to be done synchronously during flush.
            assertOnRenderThread()
            val state = buffersState.get() ?: return
            check(!state.inactiveIsReady) {
                "This should only be called once, after the inactive buffer has been hidden."
            }
            // Clear the inactive layer now that it's safely off screen. Do this manually rather
            // than with
            // a simple render request with preserveContents(false), as the lack of a recorded
            // drawing
            // operation prevents the clear from completing fully.
            state.inactive.frontBufferRenderNode.apply {
                beginRecording().drawColor(Color.TRANSPARENT, BlendMode.CLEAR)
                endRecording()
            }
            state.inactive.renderer
                .obtainRenderRequest()
                .preserveContents(true) // Clearing manually above.
                .setTransformFromBounds(bounds)
                .drawAsync(renderThreadExecutor) { renderResult ->
                    if (discarded.get()) return@drawAsync
                    // Passing renderResult.fence to setBuffer in this transaction makes sure the
                    // inactive
                    // buffer can't show again until the clear operation has fully completed. If it
                    // shows too
                    // soon when it still has content, there would be a partial or complete
                    // double-draw
                    // flicker.
                    SurfaceControlCompat.Transaction()
                        .setAndShow(state.inactive, renderResult.hardwareBuffer, renderResult.fence)
                        .addTransactionCommittedListener(
                            renderThreadExecutor,
                            object : SurfaceControlCompat.TransactionCommittedListener {
                                override fun onTransactionCommitted() {
                                    if (discarded.get()) return
                                    val newState =
                                        BuffersState(
                                            active = state.active,
                                            inactive = state.inactive,
                                            inactiveIsReady = true,
                                        )
                                    buffersState.checkAndSet(state, newState)
                                    callback.resumeStrokeCohortHandoffs()
                                    countDownAfterHandoffsResumedTestLatch?.countDown()
                                }
                            },
                        )
                        .commit()
                }
        }

        @UiThread
        fun discard() {
            assertOnUiThread()
            if (discarded.getAndSet(true)) return
            val state = buffersState.getAndSet(null) ?: return
            SurfaceControlCompat.Transaction()
                .unsetAndHide(state.active)
                .unsetAndHide(state.inactive)
                .addTransactionCommittedListener(
                    renderThreadExecutor,
                    object : SurfaceControlCompat.TransactionCommittedListener {
                        override fun onTransactionCommitted() {
                            mainView.invalidate()
                            state.active.cleanup()
                            state.inactive.cleanup()
                        }
                    },
                )
                .commit()
        }

        private fun SurfaceControlCompat.Transaction.unsetAndHide(
            bufferData: BufferData
        ): SurfaceControlCompat.Transaction {
            val sc = bufferData.surfaceControl
            setVisibility(sc, false)
            setBuffer(sc, null)
            setLayer(sc, 0)
            clearFrameRate(sc)
            setPosition(sc, 0F, 0F)
            if (bounds.surfaceTransform != null) {
                setBufferTransform(sc, SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY)
            }
            return this
        }
    }

    /**
     * The dimensions of the rendering surface, along with a transform that must be used to render
     * in the display's native coordinates for maximum performance. This serves as a sort of cache
     * key for most of the state of the render helper, so the state can be preserved when the
     * [Bounds] continue to be the same, and discarded when the [Bounds] change.
     */
    @VisibleForTesting
    internal data class Bounds(
        /** The width of [mainView]. */
        val mainViewWidth: Int,
        /** The height of [mainView]. */
        val mainViewHeight: Int,
        /**
         * The system-provided suggestion on how to pre-transform rendered content into native
         * display coordinates so that the system doesn't need to perform the transformation later
         * in a way that could hinder performance. Not all values are supported - if the hint is not
         * supported, then both [rendererTransform] and [surfaceTransform] will be null, and the
         * system will use the hardware compositor for transformation later, which is not as
         * performant but still functions.
         */
        val mainViewTransformHint: Int,
    ) {
        /**
         * The width of the buffer used for rendering. This may be either [mainViewWidth] or
         * [mainViewHeight] depending on whether [mainViewTransformHint] suggests rotating the
         * buffer relative to the view or not.
         */
        val bufferWidth: Int
        /**
         * The height of the buffer used for rendering. This may be either [mainViewHeight] or
         * [mainViewHeight] depending on whether [mainViewTransformHint] suggests rotating the
         * buffer relative to the view or not.
         */
        val bufferHeight: Int
        /**
         * Derived from [android.view.AttachedSurfaceControl.getBufferTransformHint]. Same as this
         * if it is a type of transform that is handled by this class, or null otherwise. This is
         * how the producer of the buffer - in our case, [CanvasBufferedRenderer] - should be
         * transformed to align with the native hardware orientation. This is important to reduce
         * latency and power usage on certain devices, as not all devices can handle rotations
         * cheaply in their hardware composer (HWC).
         */
        val rendererTransform: Int?
        /**
         * The inverse of [rendererTransform], or null if and only if [rendererTransform] is also
         * null. This is applied to counteract [rendererTransform] so the content appears the right
         * direction visually to the user.
         */
        val surfaceTransform: Int?

        init {
            // transformInverse will be null if the transform hint is not one that we know how to
            // optimize, and therefore no transforms should be applied to the buffer and surface.
            when (mainViewTransformHint) {
                SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90,
                SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270 -> {
                    rendererTransform = mainViewTransformHint
                    surfaceTransform =
                        if (
                            mainViewTransformHint == SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90
                        ) {
                            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270
                        } else {
                            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90
                        }
                    // Flip the width and height of the buffer compared to the view.
                    bufferWidth = mainViewHeight
                    bufferHeight = mainViewWidth
                }
                else -> {
                    when (mainViewTransformHint) {
                        SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY,
                        SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180 -> {
                            rendererTransform = mainViewTransformHint
                            surfaceTransform = mainViewTransformHint
                        }
                        else -> {
                            rendererTransform = null
                            surfaceTransform = null
                        }
                    }
                    bufferWidth = mainViewWidth
                    bufferHeight = mainViewHeight
                }
            }
            require((rendererTransform == null) == (surfaceTransform == null))
        }
    }

    private class BufferData(
        val surfaceControl: SurfaceControlCompat,
        val renderer: CanvasBufferedRenderer,
        val frontBufferRenderNode: RenderNode,
        val offScreenRenderNode: RenderNode,
    ) {
        fun cleanup() {
            renderer.close()
            frontBufferRenderNode.discardDisplayList()
            offScreenRenderNode.discardDisplayList()
            surfaceControl.release()
        }
    }

    /** Current state of buffers for a [Viewport]. Replaced atomically during handoff. */
    private class BuffersState(
        /** The buffer that draw calls will go to. */
        val active: BufferData,
        /** The buffer that will take over as [active] when a handoff is requested. */
        val inactive: BufferData,
        /**
         * Whether [inactive] is ready to become [active]. If this is `false`, then handoff should
         * not currently be allowed, and must wait until [inactive] is cleared and is on its way to
         * being shown again. In the meantime, draws must continue going to [active].
         */
        val inactiveIsReady: Boolean,
    ) {
        init {
            check(active != inactive)
        }
    }

    /**
     * Like [java.util.concurrent.ScheduledExecutorService], but a reduced interface that is easier
     * to implement and fake.
     */
    interface ScheduledExecutor : Executor {
        fun onThread(): Boolean

        fun executeDelayed(command: Runnable, delayTime: Long, delayTimeUnit: TimeUnit)
    }

    private class ScheduledExecutorImpl(private val thread: Thread, private val handler: Handler) :
        ScheduledExecutor {

        override fun onThread() = Thread.currentThread() == thread

        override fun execute(command: Runnable) {
            check(thread.isAlive)
            if (!handler.post(command)) {
                throw RejectedExecutionException("$handler is shutting down")
            }
        }

        override fun executeDelayed(command: Runnable, delayTime: Long, delayTimeUnit: TimeUnit) {
            check(thread.isAlive)
            if (!handler.postDelayed(command, delayTimeUnit.toMillis(delayTime))) {
                throw RejectedExecutionException("$handler is shutting down")
            }
        }
    }

    private companion object {

        const val BASE_USAGE_FLAGS =
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or
                HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or
                HardwareBuffer.USAGE_COMPOSER_OVERLAY

        /**
         * The preferred flags to pass to [HardwareBuffer.create] for best performance. If not
         * supported, use [ALTERNATE_USAGE_FLAGS] instead.
         */
        const val DESIRED_USAGE_FLAGS = HardwareBuffer.USAGE_FRONT_BUFFER or BASE_USAGE_FLAGS

        /**
         * The flags passed to [HardwareBuffer.create] if [DESIRED_USAGE_FLAGS] are not supported.
         *
         * This fallback prevents ARM frame buffer compression from causing visual artifacts on
         * certain devices like Samsung Galaxy Tab S6 Lite. See b/365131024 for more information.
         */
        const val ALTERNATE_USAGE_FLAGS = HardwareBuffer.USAGE_CPU_WRITE_OFTEN or BASE_USAGE_FLAGS

        /**
         * Number of pixels to widen the transformed region by, in order to better guarantee that no
         * pixels are cut off during incremental draws that modify the smallest possible rectangle.
         */
        @Px const val CLIP_REGION_OUTSET_PX = 3
    }
}
