/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.MultiBufferedCanvasRenderer
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.utils.HandlerThreadExecutor
import androidx.hardware.SyncFenceCompat
import java.util.Collections
import java.util.concurrent.CountDownLatch

/**
 * Class responsible for supporting a "front buffered" rendering system. This allows for lower
 * latency graphics by leveraging a combination of front buffered and multi buffered content layers.
 * Active content is rendered first into the front buffered layer which is simultaneously being
 * presented to the display. Periodically content is rendered into the multi buffered layer which
 * will have more traditional latency guarantees, however, minimizes the impact of visual artifacts
 * due to graphical tearing.
 *
 * @param surfaceView Target SurfaceView to act as the parent rendering layer for multi buffered
 *  content
 * @param callback Callbacks used to render into front and multi buffered layers as well as
 *  configuring [SurfaceControlCompat.Transaction]s for controlling these layers in addition to
 *  other [SurfaceControlCompat] instances that must be updated atomically within the user
 *  interface. These callbacks are invoked on an internal rendering thread. The templated type
 *  here is consumer defined to represent the data structures to be consumed for rendering within
 *  [Callback.onDrawFrontBufferedLayer] and [Callback.onDrawMultiBufferedLayer] and are provided
 *  by the [CanvasFrontBufferedRenderer.renderFrontBufferedLayer] and
 *  [CanvasFrontBufferedRenderer.renderMultiBufferedLayer] methods.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class CanvasFrontBufferedRenderer<T>(
    private val surfaceView: SurfaceView,
    private val callback: Callback<T>,
) {

    /**
     * Executor used to deliver callbacks for rendering as well as issuing surface control
     * transactions
     */
    private val mHandlerThread = HandlerThreadExecutor("CanvasRenderThread")

    /**
     * Renderer used to draw [RenderNode] into a [HardwareBuffer] that is used to configure
     * the parent SurfaceControl that represents the multi-buffered scene
     */
    private var mMultiBufferedCanvasRenderer: MultiBufferedCanvasRenderer? = null

    /**
     * Renderer used to draw the front buffer content into a HardwareBuffer instance that is
     * preserved across frames
     */
    private var mPersistedCanvasRenderer: SingleBufferedCanvasRenderer<T>? = null

    /**
     * [SurfaceControlCompat] used to configure buffers and visibility of the front buffered layer
     */
    private var mFrontBufferSurfaceControl: SurfaceControlCompat? = null

    /**
     * [SurfaceControlCompat] used to configure buffers and visibility of the multi-buffered layer
     */
    private var mParentSurfaceControl: SurfaceControlCompat? = null

    /**
     * Queue of parameters to be consumed in [Callback.onDrawFrontBufferedLayer] with the parameter
     * provided in [renderFrontBufferedLayer]. When [commit] is invoked the collection is used
     * to render the multi-buffered scene and is subsequently cleared
     */
    private var mParams = ArrayList<T>()

    /**
     * Flag to determine if the [CanvasFrontBufferedRenderer] has previously been released. If this
     * flag is true, then subsequent requests to [renderFrontBufferedLayer],
     * [renderMultiBufferedLayer], [commit], and [release] are ignored.
     */
    private var mIsReleased = false

    /**
     * Flag to determine if a request to clear the front buffer content is pending. This should
     * only be accessed on the GLThread
     */
    private var mPendingClear = true

    /**
     * Runnable executed on the GLThread to update [FrontBufferSyncStrategy.isVisible] as well
     * as hide the SurfaceControl associated with the front buffered layer
     */
    private val mCancelRunnable = Runnable {
        mPersistedCanvasRenderer?.isVisible = false
        mFrontBufferSurfaceControl?.let { frontBufferSurfaceControl ->
            SurfaceControlCompat.Transaction()
                .setVisibility(frontBufferSurfaceControl, false)
                .commit()
        }
    }

    private var mInverse = BufferTransformHintResolver.UNKNOWN_TRANSFORM
    private val mParentLayerTransform = android.graphics.Matrix()
    private var mWidth = -1
    private var mHeight = -1
    private var mTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM
    private val mTransformResolver = BufferTransformHintResolver()
    private val mHolderCallback = object : SurfaceHolder.Callback2 {

        override fun surfaceCreated(p0: SurfaceHolder) {
            // NO-OP
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            update(surfaceView, width, height)
        }

        override fun surfaceDestroyed(p0: SurfaceHolder) {
            releaseInternal(true)
        }

        override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
            val latch = CountDownLatch(1)
            renderMultiBufferedLayerInternal {
                latch.countDown()
            }
            latch.await()
        }

        override fun surfaceRedrawNeededAsync(
            holder: SurfaceHolder,
            drawingFinished: Runnable
        ) {
            renderMultiBufferedLayerInternal(callback = drawingFinished)
        }
    }

    init {
        surfaceView.holder.addCallback(mHolderCallback)
        with(surfaceView.holder) {
            if (surface != null && surface.isValid) {
                update(surfaceView, surfaceView.width, surfaceView.height)
                renderMultiBufferedLayerInternal()
            }
        }
    }

    internal fun update(surfaceView: SurfaceView, width: Int, height: Int) {
        val transformHint = mTransformResolver.getBufferTransformHint(surfaceView)
        if ((mTransform != transformHint || mWidth != width || mHeight != height) && isValid()) {
            releaseInternal(true)

            val bufferTransform = BufferTransformer()
            val inverse = bufferTransform.invertBufferTransform(transformHint)
            bufferTransform.computeTransform(width, height, inverse)
            updateMatrixTransform(width.toFloat(), height.toFloat(), inverse)

            val parentSurfaceControl = SurfaceControlCompat.Builder()
                .setParent(surfaceView)
                .setName("MultiBufferedLayer")
                .build()
                .apply {
                    // SurfaceControl is not visible by default so make it visible right
                    // after creation
                    SurfaceControlCompat.Transaction()
                        .setVisibility(this, true)
                        .commit()
                }

            val frontBufferSurfaceControl = SurfaceControlCompat.Builder()
                .setParent(parentSurfaceControl)
                .setName("FrontBufferedLayer")
                .build()

            var singleBufferedCanvasRenderer: SingleBufferedCanvasRenderer<T>? = null
            singleBufferedCanvasRenderer = SingleBufferedCanvasRenderer.create<T>(
                width,
                height,
                bufferTransform,
                mHandlerThread,
                object : SingleBufferedCanvasRenderer.RenderCallbacks<T> {

                    override fun render(canvas: Canvas, width: Int, height: Int, param: T) {
                        if (mPendingClear) {
                            canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
                            mPendingClear = false
                        }
                        callback.onDrawFrontBufferedLayer(canvas, width, height, param)
                    }

                    @SuppressLint("WrongConstant")
                    override fun onBufferReady(
                        hardwareBuffer: HardwareBuffer,
                        syncFenceCompat: SyncFenceCompat?
                    ) {
                        singleBufferedCanvasRenderer?.isVisible = true
                        val transaction = SurfaceControlCompat.Transaction()
                            .setLayer(frontBufferSurfaceControl, Integer.MAX_VALUE)
                            .setBuffer(
                                frontBufferSurfaceControl,
                                hardwareBuffer,
                                syncFenceCompat
                            )
                            .setVisibility(frontBufferSurfaceControl, true)
                            .reparent(frontBufferSurfaceControl, parentSurfaceControl)
                        if (inverse != BufferTransformHintResolver.UNKNOWN_TRANSFORM) {
                            transaction.setBufferTransform(
                                frontBufferSurfaceControl,
                                inverse
                            )
                        }
                        callback.onFrontBufferedLayerRenderComplete(
                            frontBufferSurfaceControl, transaction)
                        transaction.commit()
                        syncFenceCompat?.close()
                    }
                })

            val multiBufferNode = RenderNode("MultiBufferNode").apply {
                setPosition(0, 0, bufferTransform.glWidth, bufferTransform.glHeight)
            }
            mMultiBufferedCanvasRenderer = MultiBufferedCanvasRenderer(
                multiBufferNode,
                bufferTransform.glWidth,
                bufferTransform.glHeight,
                usage = FrontBufferUtils.BaseFlags
            ).apply { preserveContents = false }

            mFrontBufferSurfaceControl = frontBufferSurfaceControl
            mPersistedCanvasRenderer = singleBufferedCanvasRenderer
            mParentSurfaceControl = parentSurfaceControl
            mTransform = transformHint
            mWidth = width
            mHeight = height
            mInverse = inverse
        }
    }

    /**
     * Render content to the front buffered layer providing optional parameters to be consumed in
     * [Callback.onDrawFrontBufferedLayer].
     * Additionally the parameter provided here will also be consumed in
     * [Callback.onDrawMultiBufferedLayer]
     * when the corresponding [commit] method is invoked, which will include all [param]s in each
     * call made to this method up to the corresponding [commit] call.
     *
     * If this [CanvasFrontBufferedRenderer] has been released, that is [isValid] returns `false`,
     * this call is ignored.
     *
     * @param param Optional parameter to be consumed when rendering content into the commit layer
     */
    fun renderFrontBufferedLayer(param: T) {
        if (isValid()) {
            mParams.add(param)
            mPersistedCanvasRenderer?.render(param)
        } else {
            Log.w(TAG, "Attempt to render to front buffered layer when " +
                    "CanvasFrontBufferedRenderer has been released"
            )
        }
    }

    /**
     * Requests to render to the multi buffered layer. This schedules a call to
     * [Callback.onDrawMultiBufferedLayer] with the parameters provided. If the front buffered
     * layer is visible, this will hide this layer after rendering to the multi buffered layer
     * is complete. This is equivalent to calling [CanvasFrontBufferedRenderer.renderFrontBufferedLayer]
     * for each parameter provided in the collection followed by a single call to
     * [CanvasFrontBufferedRenderer.commit]. This is useful for re-rendering the multi buffered
     * scene when the corresponding Activity is being resumed from the background in which the
     * contents should be re-drawn. Additionally this allows for applications to decide to
     * dynamically render to either front or multi buffered layers.
     *
     * If this [CanvasFrontBufferedRenderer] has been released, that is [isValid] returns 'false',
     * this call is ignored.
     *
     * @param params Parameters that to be consumed when rendering to the multi buffered layer.
     * These parameters will be provided in the corresponding call to
     * [Callback.onDrawMultiBufferedLayer]
     */
    fun renderMultiBufferedLayer(params: Collection<T>) {
        renderMultiBufferedLayerInternal(params)
    }

    /**
     * Helper method to commit contents to the multi buffered layer invoking an optional
     * callback when rendering is complete
     */
    internal fun renderMultiBufferedLayerInternal(
        params: Collection<T> = Collections.emptyList(),
        callback: Runnable? = null
    ) {
        if (isValid()) {
            mParams.addAll(params)
            commitInternal(callback)
        } else {
            Log.w(TAG, "Attempt to render to the multi buffered layer when " +
                "CanvasFrontBufferedRenderer has been released"
            )
        }
    }

    /**
     * Determines whether or not the [CanvasFrontBufferedRenderer] is in a valid state. That is the
     * [release] method has not been called.
     * If this returns false, then subsequent calls to [renderFrontBufferedLayer],
     * [renderMultiBufferedLayer], [commit], and [release] are ignored
     *
     * @return `true` if this [CanvasFrontBufferedRenderer] has been released, `false` otherwise
     */
    fun isValid() = !mIsReleased

    @SuppressLint("WrongConstant")
    internal fun setParentSurfaceControlBuffer(
        frontBufferSurfaceControl: SurfaceControlCompat?,
        parentSurfaceControl: SurfaceControlCompat?,
        persistedCanvasRenderer: SingleBufferedCanvasRenderer<T>?,
        multiBufferedCanvasRenderer: MultiBufferedCanvasRenderer,
        inverse: Int,
        buffer: HardwareBuffer,
        fence: SyncFenceCompat?
    ) {
        if (frontBufferSurfaceControl != null && parentSurfaceControl != null) {
            persistedCanvasRenderer?.isVisible = false
            val transaction = SurfaceControlCompat.Transaction()
                .setVisibility(frontBufferSurfaceControl, false)
                // Set a null buffer here so that the original front buffer's release callback
                // gets invoked and we can clear the content of the front buffer
                .setBuffer(frontBufferSurfaceControl, null)
                .setVisibility(parentSurfaceControl, true)
                .setBuffer(parentSurfaceControl, buffer, fence) { releaseFence ->
                    multiBufferedCanvasRenderer.releaseBuffer(buffer, releaseFence)
                }

            if (inverse != BufferTransformHintResolver.UNKNOWN_TRANSFORM) {
                transaction.setBufferTransform(parentSurfaceControl, inverse)
            }
            callback.onMultiBufferedLayerRenderComplete(
                frontBufferSurfaceControl, transaction)
            transaction.commit()
        }
    }

    /**
     * Clears the contents of both the front and multi buffered layers. This triggers a call to
     * [Callback.onMultiBufferedLayerRenderComplete] and hides the front buffered layer.
     */
    fun clear() {
        if (isValid()) {
            mParams.clear()
            val persistedCanvasRenderer = mPersistedCanvasRenderer?.apply {
                cancelPending()
                clear()
            }
            val inverse = mInverse
            val frontBufferSurfaceControl = mFrontBufferSurfaceControl
            val parentSurfaceControl = mParentSurfaceControl
            val multiBufferedCanvasRenderer = mMultiBufferedCanvasRenderer
            mHandlerThread.execute {
                multiBufferedCanvasRenderer?.let { multiBufferRenderer ->
                    with(multiBufferRenderer) {
                        record { canvas ->
                            canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
                        }
                        renderFrame(mHandlerThread) { buffer, fence ->
                            setParentSurfaceControlBuffer(
                                frontBufferSurfaceControl,
                                parentSurfaceControl,
                                persistedCanvasRenderer,
                                multiBufferRenderer,
                                inverse,
                                buffer,
                                fence
                            )
                        }
                    }
                }
            }
        } else {
            Log.w(TAG, "Attempt to clear front buffer after CanvasFrontBufferRenderer " +
                "has been released")
        }
    }

    /**
     * Requests to render the entire scene to the multi buffered layer and schedules a call to
     * [Callback.onDrawMultiBufferedLayer]. The parameters provided to
     * [Callback.onDrawMultiBufferedLayer] will include each argument provided to every
     * [renderFrontBufferedLayer] call since the last call to [commit] has been made. When rendering
     * to the multi-buffered layer is complete, this synchronously hides the front buffer and
     * updates the multi buffered layer.
     *
     * If this [CanvasFrontBufferedRenderer] has been released, that is [isValid] returns `false`,
     * this call is ignored.
     */
    fun commit() {
        commitInternal()
    }

    /**
     * Helper method to commit contents to the multi buffered layer, invoking an optional
     * callback on completion
     */
    private fun commitInternal(onComplete: Runnable? = null) {
        if (isValid()) {
            val persistedCanvasRenderer = mPersistedCanvasRenderer?.apply {
                cancelPending()
            }
            val params = mParams
            mParams = ArrayList<T>()
            val width = surfaceView.width
            val height = surfaceView.height
            val frontBufferSurfaceControl = mFrontBufferSurfaceControl
            val parentSurfaceControl = mParentSurfaceControl
            val multiBufferedCanvasRenderer = mMultiBufferedCanvasRenderer
            val inverse = mInverse
            val transform = mParentLayerTransform
            mHandlerThread.execute {
                mPendingClear = true
                multiBufferedCanvasRenderer?.let { multiBufferedRenderer ->
                    with(multiBufferedRenderer) {
                        record { canvas ->
                            canvas.save()
                            canvas.setMatrix(transform)
                            callback.onDrawMultiBufferedLayer(canvas, width, height, params)
                            canvas.restore()
                        }
                        params.clear()
                        renderFrame(mHandlerThread) { buffer, fence ->
                            setParentSurfaceControlBuffer(
                                frontBufferSurfaceControl,
                                parentSurfaceControl,
                                persistedCanvasRenderer,
                                multiBufferedCanvasRenderer,
                                inverse,
                                buffer,
                                fence
                            )
                            onComplete?.run()
                        }
                    }
                }
            }
        } else {
            Log.w(TAG, "Attempt to render to the multi buffered layer when " +
                "CanvasFrontBufferedRenderer has been released"
            )
        }
    }

    internal fun updateMatrixTransform(width: Float, height: Float, transform: Int) {
        mParentLayerTransform.apply {
            when (transform) {
                SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90 -> {
                    setRotate(270f)
                    postTranslate(0f, width)
                }
                SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180 -> {
                    setRotate(180f)
                    postTranslate(width, height)
                }
                SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270 -> {
                    setRotate(90f)
                    postTranslate(height, 0f)
                }
                else -> {
                    reset()
                }
            }
        }
    }

    /**
     * Requests to cancel rendering and hides the front buffered layer.
     * Unlike [commit], this does not schedule a call to render into the multi buffered layer. This
     * is useful in palm rejection use cases, where some initial touch events might be processed
     * before a corresponding cancel event is received indicating the touch gesture is coming
     * from a palm rather than intentional user input. In the case where MotionEvent#getAction
     * returns ACTION_CANCEL, this is to be invoked.
     *
     * If this [GLFrontBufferedRenderer] has been released, that is [isValid] returns `false`,
     * this call is ignored.
     */
    fun cancel() {
        if (isValid()) {
            mPersistedCanvasRenderer?.cancelPending()
            mHandlerThread.execute(mCancelRunnable)
            mPersistedCanvasRenderer?.clear()
        } else {
            Log.w(TAG, "Attempt to cancel rendering to front buffer after " +
                "CanvasFrontBufferRenderer has been released")
        }
    }

    internal fun releaseInternal(cancelPending: Boolean, releaseCallback: (() -> Unit)? = null) {
        val renderer = mPersistedCanvasRenderer
        if (renderer != null) {
            // Store a local copy of the corresponding SurfaceControls and renderers to make sure
            // the release callback is not invoked on potentially newly created dependencies
            // if we are in the middle of a render request and we get a surface changed event
            val frontBufferSurfaceControl = mFrontBufferSurfaceControl
            val parentSurfaceControl = mParentSurfaceControl
            val multiBufferRenderer = mMultiBufferedCanvasRenderer

            mFrontBufferSurfaceControl = null
            mParentSurfaceControl = null
            mPersistedCanvasRenderer = null
            mMultiBufferedCanvasRenderer = null
            mWidth = -1
            mHeight = -1
            mTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM

            renderer.release(cancelPending) {
                frontBufferSurfaceControl?.release()
                parentSurfaceControl?.release()
                multiBufferRenderer?.release()
                releaseCallback?.invoke()
            }
        } else if (releaseCallback != null) {
            mHandlerThread.execute(releaseCallback)
        }
    }

    /**
     * Releases the [CanvasFrontBufferedRenderer]. In process requests are ignored.
     * If the [CanvasFrontBufferedRenderer] is already released, that is [isValid] returns `false`,
     * this method does nothing.
     */
    @JvmOverloads
    fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)? = null) {
        if (!mIsReleased) {
            surfaceView.holder.removeCallback(mHolderCallback)
            releaseInternal(cancelPending) {
                onReleaseComplete?.invoke()
                mHandlerThread.quit()
            }
            mIsReleased = true
        }
    }

    /**
     * Provides callbacks for consumers to draw into the front and multi buffered layers as well as
     * provide opportunities to synchronize [SurfaceControlCompat.Transaction]s to submit the layers
     * to the hardware compositor.
     */
    @JvmDefaultWithCompatibility
    interface Callback<T> {

        /**
         * Callback invoked to render content into the front buffered layer with the specified
         * parameters.
         * @param canvas [Canvas] used to issue drawing instructions into the front buffered layer
         * @param bufferWidth Width of the buffer that is being rendered into.
         * @param bufferHeight Height of the buffer that is being rendered into.
         * @param param optional parameter provided the corresponding
         * [CanvasFrontBufferedRenderer.renderFrontBufferedLayer] method that triggered this request
         * to render into the front buffered layer
         */
        @WorkerThread
        fun onDrawFrontBufferedLayer(
            canvas: Canvas,
            bufferWidth: Int,
            bufferHeight: Int,
            param: T
        )

        /**
         * Callback invoked to render content into the front buffered layer with the specified
         * parameters.
         * @param canvas [Canvas] used to issue drawing instructions into the front buffered layer
         * @param bufferWidth Width of the buffer that is being rendered into.
         * @param bufferHeight Height of the buffer that is being rendered into.
         * @param params optional parameter provided to render the entire scene into the multi
         * buffered layer.
         * This is a collection of all parameters provided in consecutive invocations to
         * [CanvasFrontBufferedRenderer.renderFrontBufferedLayer] since the last call to
         * [CanvasFrontBufferedRenderer.commit] has been made. After
         * [CanvasFrontBufferedRenderer.commit] is invoked, this collection is cleared and new
         * parameters are added on each subsequent call to
         * [CanvasFrontBufferedRenderer.renderFrontBufferedLayer]
         */
        @WorkerThread
        fun onDrawMultiBufferedLayer(
            canvas: Canvas,
            bufferWidth: Int,
            bufferHeight: Int,
            params: Collection<T>
        )

        /**
         * Optional callback invoked when rendering to the front buffered layer is complete but
         * before the buffers are submitted to the hardware compositor.
         * This provides consumers a mechanism for synchronizing the transaction with other
         * [SurfaceControlCompat] objects that maybe rendered within the scene.
         *
         * @param frontBufferedLayerSurfaceControl Handle to the [SurfaceControlCompat] where the
         * front buffered layer content is drawn. This can be used to configure various properties
         * of the [SurfaceControlCompat] like z-ordering or visibility with the corresponding
         * [SurfaceControlCompat.Transaction].
         * @param transaction Current [SurfaceControlCompat.Transaction] to apply updated buffered
         * content to the front buffered layer.
         */
        @WorkerThread
        fun onFrontBufferedLayerRenderComplete(
            frontBufferedLayerSurfaceControl: SurfaceControlCompat,
            transaction: SurfaceControlCompat.Transaction
        ) {
            // Default implementation is a no-op
        }

        /**
         * Optional callback invoked when rendering to the multi buffered layer is complete but
         * before the buffers are submitted to the hardware compositor.
         * This provides consumers a mechanism for synchronizing the transaction with other
         * [SurfaceControlCompat] objects that maybe rendered within the scene.
         *
         * @param frontBufferedLayerSurfaceControl Handle to the [SurfaceControlCompat] where the
         * front buffered layer content is drawn. This can be used to configure various properties
         * of the [SurfaceControlCompat] like z-ordering or visibility with the corresponding
         * [SurfaceControlCompat.Transaction].
         * @param transaction Current [SurfaceControlCompat.Transaction] to apply updated buffered
         * content to the multi buffered layer.
         */
        @WorkerThread
        fun onMultiBufferedLayerRenderComplete(
            frontBufferedLayerSurfaceControl: SurfaceControlCompat,
            transaction: SurfaceControlCompat.Transaction
        ) {
            // Default implementation is a no-op
        }
    }

    internal companion object {

        internal const val TAG = "LowLatencyCanvas"
    }
}
