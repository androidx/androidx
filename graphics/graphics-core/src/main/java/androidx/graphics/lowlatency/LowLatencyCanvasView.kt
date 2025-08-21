/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.lowlatency

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.hardware.DataSpace
import android.hardware.HardwareBuffer
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.CanvasBufferedRenderer
import androidx.graphics.lowlatency.ColorSpaceVerificationHelper.Companion.getColorSpaceFromDataSpace
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.utils.HandlerThreadExecutor
import androidx.hardware.SyncFenceCompat
import java.lang.IllegalStateException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * [View] implementation that leverages a "front buffered" rendering system. This allows for lower
 * latency graphics by leveraging a combination of front buffered alongside multi-buffered content
 * layers. This class provides similar functionality to [CanvasFrontBufferedRenderer], however,
 * leverages the traditional View system for implementing the multi buffered content instead of a
 * separate [SurfaceControlCompat] instance and entirely abstracts all [SurfaceView] usage for
 * simplicity.
 *
 * Drawing of this View's content is handled by a consumer specified [LowLatencyCanvasView.Callback]
 * implementation instead of [View.onDraw]. Rendering here is done with a [Canvas] into a single
 * buffer that is presented on screen above the rest of the View hierarchy content. This overlay is
 * transient and will only be visible after [LowLatencyCanvasView.renderFrontBufferedLayer] is
 * called and hidden after [LowLatencyCanvasView.commit] is invoked. After
 * [LowLatencyCanvasView.commit] is invoked, this same buffer is wrapped into a bitmap and drawn
 * within this View's [View.onDraw] implementation.
 *
 * Calls to [LowLatencyCanvasView.renderFrontBufferedLayer] will trigger
 * [LowLatencyCanvasView.Callback.onDrawFrontBufferedLayer] to be invoked to handle drawing of
 * content with the provided [Canvas].
 *
 * After [LowLatencyCanvasView.commit] is called, the overlay is hidden and the buffer is drawn
 * within the [View] hierarchy, similar to traditional [View] implementations.
 *
 * A common use case would be a drawing application that intends to minimize the amount of latency
 * when content is drawn with a stylus. In this case, touch events between [MotionEvent.ACTION_DOWN]
 * and [MotionEvent.ACTION_MOVE] can trigger calls to
 * [LowLatencyCanvasView.renderFrontBufferedLayer] which will minimize the delay between then the
 * content is visible on screen. Finally when the gesture is complete on [MotionEvent.ACTION_UP], a
 * call to [LowLatencyCanvasView.commit] would be invoked to hide the transient overlay and render
 * the scene within the View hierarchy like a traditional View. This helps provide a balance of low
 * latency guarantees while mitigating potential tearing artifacts.
 *
 * This helps support low latency rendering for simpler use cases at the expensive of configuration
 * customization of the multi buffered layer content.
 *
 * @sample androidx.graphics.core.samples.lowLatencyCanvasViewSample
 */
@RequiresApi(Build.VERSION_CODES.Q)
class LowLatencyCanvasView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    ViewGroup(context, attrs, defStyle) {

    /**
     * Internal SurfaceView used as the parent of the front buffered SurfaceControl. The handoff
     * between rendering from the front buffered layer to HWUI is done by translating this
     * SurfaceView instance offscreen in order to preserve the internal surface dependencies that
     * would otherwise get torn down due to visibility changes or other property changes that would
     * cause the contents to not be displayed.
     */
    private val mSurfaceView: SurfaceView

    /** Executor used to dispatch requests to render as well as SurfaceControl transactions */
    private val mHandlerThread = HandlerThreadExecutor("LowLatencyCanvasThread")

    /**
     * [SingleBufferedCanvasRenderer] instance used to render content to the front buffered layer
     * using [Canvas]
     */
    private var mFrontBufferedRenderer: SingleBufferedCanvasRenderer<Unit>? = null

    /** [SurfaceControlCompat] instance used to direct front buffer rendered output */
    private var mFrontBufferedSurfaceControl: SurfaceControlCompat? = null

    /**
     * [HardwareBuffer] that maintains the contents to be displayed by either the front buffered
     * SurfaceControl or by HWUI that is wrapped by a Bitmap.
     */
    private var mHardwareBuffer: HardwareBuffer? = null

    /**
     * [SyncFenceCompat] to be waited upon before consuming the rendered output in [mHardwareBuffer]
     */
    private var mBufferFence: SyncFenceCompat? = null

    /**
     * Bitmap that wraps [mHardwareBuffer] to be used to draw the content to HWUI as part of handing
     * off the front buffered content to a multi buffered layer
     */
    private var mSceneBitmap: Bitmap? = null

    /**
     * Render callbacks invoked by the consumer to render the entire scene or updates from the last
     * call to [renderFrontBufferedLayer]
     */
    private var mCallback: Callback? = null

    /** Logical width of the single buffered content */
    private var mWidth = -1

    /** Logical height of the single buffered content */
    private var mHeight = -1

    /** Transform to be used for pre-rotation of content */
    private var mProducerBufferTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM

    /** Flag determining if the front buffered layer is the current render destination */
    private val mFrontBufferTarget = AtomicBoolean(false)

    /** Flag determining if a clear operation is pending */
    private val mClearPending = AtomicBoolean(false)

    /** Flag determining if redrawing the entire scene is required */
    private val mRedrawScene = AtomicBoolean(false)

    /** Number of issued requests to render that have not been processed yet */
    private val mPendingRenderCount = AtomicInteger(0)

    /**
     * Optional [Runnable] to be executed when a render request is completed. This is used in
     * conjunction with [SurfaceHolder.Callback2.surfaceRedrawNeededAsync]
     */
    private val mDrawCompleteRunnable = AtomicReference<Runnable>()

    /**
     * Transform applied when drawing the scene to the View's Canvas to invert the pre-rotation
     * applied to the buffer when submitting to the front buffered SurfaceControl
     */
    private val mConsumerBufferTransform = Matrix()

    /**
     * Flag to determine if the buffer has been drawn by this View on the last call to
     * [View.onDraw].
     */
    private var mSceneBitmapDrawn = false

    /** Configured ColorSpace */
    private var mColorSpace = CanvasBufferedRenderer.DefaultColorSpace

    private val mSurfaceHolderCallbacks =
        object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // NO-OP wait for surfaceChanged
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
                update(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releaseInternal(true)
            }

            override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
                val latch = CountDownLatch(1)
                drawAsync { latch.countDown() }
                latch.await()
            }

            override fun surfaceRedrawNeededAsync(
                holder: SurfaceHolder,
                drawingFinished: Runnable,
            ) {
                drawAsync(drawingFinished)
            }

            private fun drawAsync(drawingFinished: Runnable?) {
                mRedrawScene.set(true)
                mFrontBufferTarget.set(false)
                mDrawCompleteRunnable.set(drawingFinished)
                mFrontBufferedRenderer?.render(Unit)
            }
        }

    init {
        setWillNotDraw(false)
        val surfaceView =
            SurfaceView(context).apply {
                setZOrderOnTop(true)
                holder.addCallback(mSurfaceHolderCallbacks)
            }
        mSurfaceView = surfaceView
        hideFrontBuffer()
        addView(surfaceView)
    }

    internal fun update(width: Int, height: Int) {
        val producerTransformHint = BufferTransformHintResolver().getBufferTransformHint(this)
        if (
            mWidth == width &&
                mHeight == height &&
                mProducerBufferTransform == producerTransformHint
        ) {
            // Updating with same config, ignoring
            return
        }
        releaseInternal()

        val bufferTransformer = BufferTransformer()
        val consumerBufferTransform = bufferTransformer.invertBufferTransform(producerTransformHint)
        bufferTransformer.computeTransform(width, height, producerTransformHint)
        BufferTransformHintResolver.configureTransformMatrix(
                mConsumerBufferTransform,
                bufferTransformer.bufferWidth.toFloat(),
                bufferTransformer.bufferHeight.toFloat(),
                producerTransformHint,
            )
            // TODO shouldn't invert?
            .apply { invert(this) }

        val frontBufferSurfaceControl =
            SurfaceControlCompat.Builder()
                .setParent(mSurfaceView)
                .setName("FrontBufferedLayer")
                .build()

        FrontBufferUtils.configureFrontBufferLayerFrameRate(frontBufferSurfaceControl)?.commit()

        val dataSpace: Int
        val colorSpace: ColorSpace
        if (isAndroidUPlus && supportsWideColorGamut()) {
            colorSpace = getColorSpaceFromDataSpace(DataSpace.DATASPACE_DISPLAY_P3)
            dataSpace =
                if (colorSpace === CanvasBufferedRenderer.DefaultColorSpace) {
                    DataSpace.DATASPACE_SRGB
                } else {
                    DataSpace.DATASPACE_DISPLAY_P3
                }
        } else {
            dataSpace = DataSpace.DATASPACE_SRGB
            colorSpace = CanvasBufferedRenderer.DefaultColorSpace
        }
        var frontBufferRenderer: SingleBufferedCanvasRenderer<Unit>? = null
        frontBufferRenderer =
            SingleBufferedCanvasRenderer(
                    width,
                    height,
                    bufferTransformer.bufferWidth,
                    bufferTransformer.bufferHeight,
                    HardwareBuffer.RGBA_8888,
                    producerTransformHint,
                    mHandlerThread,
                    object : SingleBufferedCanvasRenderer.RenderCallbacks<Unit> {

                        var hardwareBitmapConfigured = false

                        var mRenderCount = 0

                        override fun render(canvas: Canvas, width: Int, height: Int, param: Unit) {
                            if (mRedrawScene.getAndSet(false)) {
                                mCallback?.onRedrawRequested(canvas, width, height)
                            } else {
                                mRenderCount++
                                mCallback?.onDrawFrontBufferedLayer(canvas, width, height)
                            }
                        }

                        override fun onBufferReady(
                            hardwareBuffer: HardwareBuffer,
                            syncFenceCompat: SyncFenceCompat?,
                        ) {
                            mHardwareBuffer = hardwareBuffer
                            mBufferFence = syncFenceCompat
                            val pendingRenders: Boolean
                            if (
                                mPendingRenderCount.compareAndSet(mRenderCount, 0) ||
                                    mPendingRenderCount.get() == 0
                            ) {
                                mRenderCount = 0
                                pendingRenders = false
                            } else {
                                pendingRenders = true
                            }
                            if (mFrontBufferTarget.get() || pendingRenders) {
                                val transaction =
                                    SurfaceControlCompat.Transaction()
                                        .setLayer(frontBufferSurfaceControl, Integer.MAX_VALUE)
                                        .setBuffer(
                                            frontBufferSurfaceControl,
                                            hardwareBuffer,
                                            // Only block on SyncFnece if front buffer is previously
                                            // visible
                                            if (frontBufferRenderer?.isVisible == true) {
                                                null
                                            } else {
                                                syncFenceCompat
                                            },
                                        )
                                        .setVisibility(frontBufferSurfaceControl, true)
                                if (
                                    consumerBufferTransform !=
                                        BufferTransformHintResolver.UNKNOWN_TRANSFORM
                                ) {
                                    transaction.setBufferTransform(
                                        frontBufferSurfaceControl,
                                        consumerBufferTransform,
                                    )
                                }
                                if (isAndroidUPlus) {
                                    transaction.setDataSpace(frontBufferSurfaceControl, dataSpace)
                                }
                                mCallback?.onFrontBufferedLayerRenderComplete(
                                    frontBufferSurfaceControl,
                                    transaction,
                                )
                                transaction.commit()
                                syncFenceCompat?.close()
                                frontBufferRenderer?.isVisible = true
                            } else {
                                syncFenceCompat?.awaitForever()
                                // Contents of the rendered output do not update on emulators prior
                                // to
                                // Android U so always wrap the bitmap for older API levels but only
                                // do so
                                // once on Android U+ to avoid unnecessary allocations.
                                val bitmap =
                                    if (
                                        !hardwareBitmapConfigured ||
                                            updatedWrappedHardwareBufferRequired
                                    ) {
                                        hardwareBitmapConfigured = true
                                        Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                                    } else {
                                        null
                                    }

                                this@LowLatencyCanvasView.post {
                                    if (bitmap != null) {
                                        mSceneBitmap = bitmap
                                    }
                                    hideFrontBuffer()
                                    invalidate()
                                }
                            }
                            // Execute the pending runnable and mark as consumed
                            mDrawCompleteRunnable.getAndSet(null)?.run()
                        }
                    },
                )
                .apply {
                    this.colorSpace = colorSpace
                    this.isVisible = true
                }

        mColorSpace = colorSpace
        mFrontBufferedRenderer = frontBufferRenderer
        mFrontBufferedSurfaceControl = frontBufferSurfaceControl
        mWidth = width
        mHeight = height
        mProducerBufferTransform = producerTransformHint
    }

    /**
     * Dispatches a runnable to be executed on the background rendering thread. This is useful for
     * updating data structures used to issue drawing instructions on the same thread that
     * [Callback.onDrawFrontBufferedLayer] is invoked on.
     */
    fun execute(runnable: Runnable) {
        mHandlerThread.execute(runnable)
    }

    private fun showFrontBuffer() {
        mSurfaceView.translationX = 0f
        mSurfaceView.translationY = 0f
    }

    private fun hideFrontBuffer() {
        // Since Android N SurfaceView transformations are synchronous with View hierarchy rendering
        // To hide the front buffered layer, translate the SurfaceView so that the contents
        // are clipped out.
        mSurfaceView.translationX = this.width.toFloat()
        mSurfaceView.translationY = this.height.toFloat()
        mFrontBufferedRenderer?.isVisible = false
    }

    /**
     * Render content to the front buffered layer. This triggers a call to
     * [Callback.onDrawFrontBufferedLayer]. [Callback] implementations can also configure the
     * corresponding [SurfaceControlCompat.Transaction] that updates the contents on screen by
     * implementing the optional [Callback.onFrontBufferedLayerRenderComplete] callback
     */
    fun renderFrontBufferedLayer() {
        mFrontBufferTarget.set(true)
        mPendingRenderCount.incrementAndGet()
        mFrontBufferedRenderer?.render(Unit)
        showFrontBuffer()
        if (mSceneBitmapDrawn) {
            invalidate()
        }
    }

    /**
     * Clears the content of the buffer and hides the front buffered overlay. This will cancel all
     * pending requests to render. This is similar to [cancel], however in addition to cancelling
     * the pending render requests, this also clears the contents of the buffer. Similar to [commit]
     * this will also hide the front buffered overlay.
     */
    fun clear() {
        mClearPending.set(true)
        mFrontBufferedRenderer?.let { renderer ->
            renderer.cancelPending()
            renderer.clear { mClearPending.set(false) }
        }
        hideFrontBuffer()
        invalidate()
    }

    /**
     * Cancels any in progress request to render to the front buffer and hides the front buffered
     * overlay. Cancellation is a "best-effort" approach and any in progress rendering will still be
     * applied.
     */
    fun cancel() {
        if (mFrontBufferTarget.compareAndSet(true, false)) {
            mPendingRenderCount.set(0)
            mFrontBufferedRenderer?.cancelPending()
            hideFrontBuffer()
        }
    }

    /**
     * Invalidates this View and draws the buffer within [View#onDraw]. This will synchronously hide
     * the front buffered overlay when drawing the buffer to this View. Consumers are encouraged to
     * invoke this method when a user gesture that requires low latency rendering is complete. For
     * example in response to a [MotionEvent.ACTION_UP] event in an implementation of
     * [View.onTouchEvent].
     */
    fun commit() {
        mFrontBufferTarget.set(false)
        if (!isRenderingToFrontBuffer()) {
            if (mSceneBitmap == null) {
                mHandlerThread.execute {
                    val buffer = mHardwareBuffer
                    if (buffer != null) {
                        mBufferFence?.awaitForever()
                        val bitmap = Bitmap.wrapHardwareBuffer(buffer, mColorSpace)
                        post {
                            mSceneBitmap = bitmap
                            hideFrontBuffer()
                            invalidate()
                        }
                    }
                }
            } else {
                hideFrontBuffer()
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        // Always clip to the View bounds so we can translate the SurfaceView out of view without
        // it being visible in case View#clipToPadding is true
        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        val sceneBitmap = mSceneBitmap
        mSceneBitmapDrawn =
            if (!mClearPending.get() && !isRenderingToFrontBuffer() && sceneBitmap != null) {
                canvas.save()
                canvas.setMatrix(mConsumerBufferTransform)
                canvas.drawBitmap(sceneBitmap, 0f, 0f, null)
                canvas.restore()
                true
            } else {
                false
            }
        canvas.restore()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // HWC does not render contents of a SurfaceControl in the same way as HWUI on Android U+
        // To address this configure the window to be wide color gamut so that the content looks
        // identical after handing off from the front buffered layer to HWUI.
        val context = this.context
        if (supportsWideColorGamut() && context is Activity && isAndroidUPlus) {
            context.window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        }
    }

    private fun supportsWideColorGamut(): Boolean = this.display?.isWideColorGamut == true

    private fun isRenderingToFrontBuffer(): Boolean =
        mFrontBufferTarget.get() || mPendingRenderCount.get() != 0

    internal fun releaseInternal(
        cancelPending: Boolean = true,
        onReleaseCallback: (() -> Unit)? = null,
    ) {
        val renderer = mFrontBufferedRenderer
        if (renderer != null) {
            val frontBufferedLayerSurfaceControl = mFrontBufferedSurfaceControl
            val hardwareBuffer = mHardwareBuffer
            val bufferFence = mBufferFence
            val bitmap = mSceneBitmap
            mFrontBufferedSurfaceControl = null
            mFrontBufferedRenderer = null
            mSceneBitmap = null
            mWidth = -1
            mHeight = -1
            mProducerBufferTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM
            mHardwareBuffer = null
            mBufferFence = null
            mSceneBitmapDrawn = false

            renderer.release(cancelPending) {
                if (frontBufferedLayerSurfaceControl?.isValid() == true) {
                    SurfaceControlCompat.Transaction().apply {
                        reparent(frontBufferedLayerSurfaceControl, null)
                        commit()
                        close()
                    }
                    frontBufferedLayerSurfaceControl.release()
                }
                onReleaseCallback?.invoke()
                if (hardwareBuffer != null && !hardwareBuffer.isClosed) {
                    hardwareBuffer.close()
                }
                if (bufferFence != null && bufferFence.isValid()) {
                    bufferFence.close()
                }
                bitmap?.recycle()
            }
        }
    }

    /**
     * Configures the [Callback] used to render contents to the front buffered overlay as well as
     * optionally configuring the [SurfaceControlCompat.Transaction] used to update contents on
     * screen.
     */
    fun setRenderCallback(callback: Callback?) {
        mHandlerThread.execute { mCallback = callback }
    }

    override fun addView(child: View?) {
        addViewInternal(child) { super.addView(child) }
    }

    override fun addView(child: View?, index: Int) {
        addViewInternal(child) { super.addView(child, index) }
    }

    override fun addView(child: View?, width: Int, height: Int) {
        addViewInternal(child) { super.addView(child, width, height) }
    }

    override fun addView(child: View?, params: LayoutParams?) {
        addViewInternal(child) { super.addView(child, params) }
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        addViewInternal(child) { super.addView(child, index, params) }
    }

    /** Helper method to ensure that only the internal SurfaceView is added to this ViewGroup */
    private inline fun addViewInternal(child: View?, block: () -> Unit) {
        if (child === mSurfaceView) {
            block()
        } else {
            throw IllegalStateException(
                "LowLatencyCanvasView does not accept arbitrary child " + "Views"
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        mSurfaceView.layout(l, t, r, b)
    }

    /**
     * Provides callbacks for consumers to draw into the front buffered overlay as well as provide
     * opportunities to synchronize [SurfaceControlCompat.Transaction]s to submit the layers to the
     * hardware compositor
     */
    @JvmDefaultWithCompatibility
    interface Callback {

        /**
         * Callback invoked when the entire scene should be re-rendered. This is invoked during
         * initialization and when the corresponding Activity is resumed from a background state.
         *
         * @param canvas [Canvas] used to issue drawing instructions into the front buffered layer
         * @param width Logical width of the content that is being rendered.
         * @param height Logical height of the content that is being rendered.
         */
        @WorkerThread fun onRedrawRequested(canvas: Canvas, width: Int, height: Int)

        /**
         * Callback invoked to render content into the front buffered layer with the specified
         * parameters.
         *
         * @param canvas [Canvas] used to issue drawing instructions into the front buffered layer
         * @param width Logical width of the content that is being rendered.
         * @param height Logical height of the content that is being rendered.
         */
        @WorkerThread fun onDrawFrontBufferedLayer(canvas: Canvas, width: Int, height: Int)

        /**
         * Optional callback invoked when rendering to the front buffered layer is complete but
         * before the buffers are submitted to the hardware compositor. This provides consumers a
         * mechanism for synchronizing the transaction with other [SurfaceControlCompat] objects
         * that maybe rendered within the scene.
         *
         * @param frontBufferedLayerSurfaceControl Handle to the [SurfaceControlCompat] where the
         *   front buffered layer content is drawn. This can be used to configure various properties
         *   of the [SurfaceControlCompat] like z-ordering or visibility with the corresponding
         *   [SurfaceControlCompat.Transaction].
         * @param transaction Current [SurfaceControlCompat.Transaction] to apply updated buffered
         *   content to the front buffered layer.
         */
        @WorkerThread
        fun onFrontBufferedLayerRenderComplete(
            frontBufferedLayerSurfaceControl: SurfaceControlCompat,
            transaction: SurfaceControlCompat.Transaction,
        ) {
            // Default implementation is a no-op
        }
    }

    private companion object {

        val isEmulator: Boolean =
            Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.FINGERPRINT.contains("emulator") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("sdk_gphone64") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
                "google_sdk" == Build.PRODUCT

        val updatedWrappedHardwareBufferRequired: Boolean = !isAndroidUPlus && isEmulator

        val isAndroidUPlus: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class ColorSpaceVerificationHelper {
    companion object {

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun getColorSpaceFromDataSpace(dataSpace: Int) =
            ColorSpace.getFromDataSpace(dataSpace)
                // If wide color gamut is supported, then this should always return non-null
                // fallback to SRGB to maintain non-null ColorSpace kotlin type
                ?: CanvasBufferedRenderer.DefaultColorSpace
    }
}
