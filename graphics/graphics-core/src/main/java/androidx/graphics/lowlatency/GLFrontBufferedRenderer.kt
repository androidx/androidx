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
import android.hardware.HardwareBuffer
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.util.Log
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.opengl.FrameBuffer
import androidx.graphics.opengl.FrameBufferPool
import androidx.graphics.opengl.FrameBufferRenderer
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.surface.SurfaceControlCompat
import androidx.hardware.SyncFenceCompat
import androidx.opengl.EGLExt.Companion.EGL_ANDROID_NATIVE_FENCE_SYNC
import androidx.opengl.EGLExt.Companion.EGL_KHR_FENCE_SYNC
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

/**
 * Class responsible for supporting a "front buffered" rendering system. This allows for lower
 * latency graphics by leveraging a combination of front buffered and multi buffered content
 * layers.
 * Active content is rendered first into the front buffered layer which is simultaneously being
 * presented to the display. Periodically content is rendered into the multi buffered layer which
 * will have more traditional latency guarantees, however, minimize the impact of visual artifacts
 * due to graphical tearing.
 *
 * @param surfaceView Target SurfaceView to act as the parent rendering layer for multi buffered
 *  content
 * @param callback Callbacks used to render into front and multi buffered layers as well as
 *  configuring [SurfaceControlCompat.Transaction]s for controlling these layers in addition to
 *  other [SurfaceControlCompat] instances that must be updated atomically within the user
 *  interface. These callbacks are invoked on the backing GL Thread.
 * @param glRenderer Optional [GLRenderer] instance that this [GLFrontBufferedRenderer] should use
 *  for coordinating requests to render content. If this parameter is specified, the caller
 *  of this API is responsible for releasing resources on [GLRenderer] by calling
 *  [GLRenderer.stop]. Otherwise [GLFrontBufferedRenderer] will create and manage its own
 *  [GLRenderer] internally and will automatically release its resources within
 *  [GLFrontBufferedRenderer.release]
 */
@RequiresApi(Build.VERSION_CODES.Q)
@Suppress("AcronymName")
class GLFrontBufferedRenderer<T> @JvmOverloads constructor(
    surfaceView: SurfaceView,
    callback: Callback<T>,
    @Suppress("ListenerLast")
    glRenderer: GLRenderer? = null,
) {
    /**
     * [ParentRenderLayer] used to contain both the front and multi buffered layers
     */
    private val mParentRenderLayer: ParentRenderLayer<T> = SurfaceViewRenderLayer(surfaceView)

    /**
     * Callbacks invoked to render into the front and multi buffered layers in addition to
     * providing consumers an opportunity to specify any potential additional interactions that must
     * be synchronized with the [SurfaceControlCompat.Transaction] to show/hide visibility of the
     * front buffered layer as well as updating multi buffered layers
     */
    private val mCallback = object : Callback<T> by callback {
        @WorkerThread
        override fun onMultiBufferedLayerRenderComplete(
            frontBufferedLayerSurfaceControl: SurfaceControlCompat,
            transaction: SurfaceControlCompat.Transaction
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                transaction.addTransactionCommittedListener(mExecutor, mCommittedListener)
            } else {
                clearFrontBuffer()
            }
            mFrontBufferSyncStrategy.isVisible = false
            callback.onMultiBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl,
                transaction
            )
        }

        @WorkerThread
        override fun onFrontBufferedLayerRenderComplete(
            frontBufferedLayerSurfaceControl: SurfaceControlCompat,
            transaction: SurfaceControlCompat.Transaction
        ) {
            mFrontBufferSyncStrategy.isVisible = true
            callback.onFrontBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl,
                transaction
            )
        }
    }

    private val mExecutor = Executors.newSingleThreadExecutor()

    private val mCommittedListener = object : SurfaceControlCompat.TransactionCommittedListener {
        override fun onTransactionCommitted() {
            clearFrontBuffer()
        }
    }

    internal fun clearFrontBuffer() {
        mFrontBufferedLayerRenderer?.clear()
        mFrontBufferedRenderTarget?.requestRender()
    }

    /**
     * [GLRenderer.EGLContextCallback]s used to release the corresponding [FrameBufferPool]
     * if the [GLRenderer] is torn down.
     * This is especially helpful if a [GLRenderer] is being provided and shared across other
     * [GLRenderer.RenderTarget] instances in which case it can be released externally from
     * [GLFrontBufferedRenderer]
     */
    private val mContextCallbacks = object : GLRenderer.EGLContextCallback {
        override fun onEGLContextCreated(eglManager: EGLManager) {
            with(eglManager) {
                val supportsEglFences = isExtensionSupported(EGL_KHR_FENCE_SYNC)
                val supportsAndroidFences = isExtensionSupported(EGL_ANDROID_NATIVE_FENCE_SYNC)
                Log.d(TAG, "Supports KHR_FENCE_SYNC: $supportsEglFences")
                Log.d(TAG, "Supports ANDROID_NATIVE_FENCE_SYNC: $supportsAndroidFences")
            }
        }

        override fun onEGLContextDestroyed(eglManager: EGLManager) {
            mBufferPool?.let { releaseBuffers(it) }
        }
    }

    /**
     * [ParentRenderLayer] callbacks used to be alerted of changes to the size of the parent
     * render layer which acts as a signal to teardown some internal resources and recreate them
     * with the updated dimensions
     */
    private val mParentLayerCallback = object :
        ParentRenderLayer.Callback<T> {
        override fun onSizeChanged(width: Int, height: Int) {
            update(width, height)
        }

        override fun onLayerDestroyed() {
            detachTargets(true)
        }

        override fun obtainMultiBufferedLayerParams(): MutableCollection<T>? =
            mSegments.poll()

        override fun getFrontBufferedLayerSurfaceControl(): SurfaceControlCompat? =
            mFrontBufferedLayerSurfaceControl

        override fun getFrameBufferPool(): FrameBufferPool? =
            mBufferPool
    }

    /**
     * Runnable executed on the GLThread to update [FrontBufferSyncStrategy.isVisible] as well
     * as hide the SurfaceControl associated with the front buffered layer
     */
    private val mCancelRunnable = Runnable {
        mFrontBufferSyncStrategy.isVisible = false
        mFrontBufferedLayerSurfaceControl?.let { frontBufferSurfaceControl ->
            SurfaceControlCompat.Transaction()
                .setVisibility(frontBufferSurfaceControl, false)
                .commit()
        }
    }

    /**
     * Queue of parameters to be consumed in [Callback.onDrawFrontBufferedLayer] with the parameter
     * provided in [renderFrontBufferedLayer]
     */
    private val mActiveSegment = ParamQueue<T>()

    /**
     * Collection of parameters to be consumed in [Callback.onMultiBufferedLayerRenderComplete]
     * with the parameters defined in consecutive calls to [renderFrontBufferedLayer].
     * Once the corresponding [Callback.onMultiBufferedLayerRenderComplete] callback is invoked,
     * this collection is cleared and new parameters are added to it with consecutive calls to
     * [renderFrontBufferedLayer].
     */
    private val mSegments = ConcurrentLinkedQueue<MutableCollection<T>>()

    /**
     * [FrameBuffer] used for rendering into the front buffered layer. This buffer is persisted
     * across frames as part of front buffered rendering and is not expected to be released again
     * after the corresponding [SurfaceControlCompat.Transaction] that submits this buffer is
     * applied as per the implementation of "scan line racing" that is done for front buffered
     * rendering
     */
    private var mFrontLayerBuffer: FrameBuffer? = null

    /**
     * [FrameBufferPool] used to cycle through [FrameBuffer] instances that are released when
     * the [HardwareBuffer] within the [FrameBuffer] is already displayed by the hardware
     * compositor
     */
    private var mBufferPool: FrameBufferPool? = null

    /**
     * [GLRenderer.RenderCallback] used for drawing into the front buffered layer
     */
    private var mFrontBufferedLayerRenderer: FrameBufferRenderer? = null

    /**
     * [SurfaceControlCompat] used to configure buffers and visibility of the front buffered layer
     */
    private var mFrontBufferedLayerSurfaceControl: SurfaceControlCompat? = null

    /**
     * [FrontBufferSyncStrategy] used for [FrameBufferRenderer] to conditionally decide
     * when to create a [SyncFenceCompat] for transaction calls.
     */
    private val mFrontBufferSyncStrategy: FrontBufferSyncStrategy

    /**
     * Width of the layers to render. Only if the size changes to we re-initialize the internal
     * state of the [GLFrontBufferedRenderer]
     */
    private var mWidth = -1

    /**
     * Height of the layers to render. Only if the size changes do we re-initialize the internal
     * state of the [GLFrontBufferedRenderer]
     */
    private var mHeight = -1

    /**
     * [GLRenderer] used to issue requests to render into front/multi buffered layers
     */
    private val mGLRenderer: GLRenderer

    /**
     * Flag indicating if the [GLRenderer] being used was created internally within
     * [GLFrontBufferedRenderer] as opposed to being provided by the consumer.
     * If the former, then the [GLFrontBufferedRenderer] is responsible for stopping/releasing this
     * [GLRenderer] in the [release] method. If this is being provided, then we should not be
     * releasing this [GLRenderer] as it maybe used by other consumers.
     * In this case, only the front/multi buffered [GLRenderer.RenderTarget]s are detached.
     */
    private val mIsManagingGLRenderer: Boolean

    /**
     * [GLRenderer.RenderTarget] used to issue requests to render into the front buffered layer
     */
    private var mFrontBufferedRenderTarget: GLRenderer.RenderTarget? = null

    /**
     * [GLRenderer.RenderTarget] used to issue requests to render into the multi buffered layer
     */
    private var mMultiBufferedLayerRenderTarget: GLRenderer.RenderTarget? = null

    /**
     * Flag to determine if the [GLFrontBufferedRenderer] has previously been released. If this flag
     * is true, then subsequent requests to [renderFrontBufferedLayer], [commit], and [release] are
     * ignored.
     */
    private var mIsReleased = false

    /**
     * Cached value to store what [HardwareBuffer] usage flags are supported on the device.
     */
    private val mHardwareBufferUsageFlags: Long

    init {
        mParentRenderLayer.setParentLayerCallbacks(mParentLayerCallback)
        val renderer = if (glRenderer == null) {
            // If we have not been provided a [GLRenderer] then we should create/start one ourselves
            mIsManagingGLRenderer = true
            GLRenderer().apply { start() }
        } else {
            // ... otherwise use the [GLRenderer] that is being provided for us
            mIsManagingGLRenderer = false
            if (!glRenderer.isRunning()) {
                throw IllegalStateException("The provided GLRenderer must be running prior to " +
                    "creation of GLFrontBufferedRenderer, " +
                    "did you forget to call GLRenderer#start()?")
            }
            glRenderer
        }
        renderer.registerEGLContextCallback(mContextCallbacks)

        mGLRenderer = renderer

        mHardwareBufferUsageFlags = FrontBufferUtils.obtainHardwareBufferUsageFlags()

        mFrontBufferSyncStrategy = FrontBufferSyncStrategy(mHardwareBufferUsageFlags)
    }

    internal fun update(width: Int, height: Int) {
        if (mWidth != width || mHeight != height && isValid()) {

            mMultiBufferedLayerRenderTarget?.detach(true)
            val multiBufferTarget = mParentRenderLayer.createRenderTarget(mGLRenderer, mCallback)

            mFrontBufferedLayerSurfaceControl?.release()

            val frontBufferedSurfaceControl = SurfaceControlCompat.Builder()
                .setName("FrontBufferedSurfaceControl")
                .apply {
                    mParentRenderLayer.setParent(this)
                }
                .build()

            val bufferWidth = mParentRenderLayer.getBufferWidth()
            val bufferHeight = mParentRenderLayer.getBufferHeight()

            // Create buffer pool for the multi-buffered layer
            // The flags here are identical to those used for buffers in the front buffered layer
            // except USAGE_FRONT_BUFFER is not specified
            val bufferPool = FrameBufferPool(
                bufferWidth,
                bufferHeight,
                format = HardwareBuffer.RGBA_8888,
                usage = FrontBufferUtils.BaseFlags,
                maxPoolSize = 4
            )

            val previousBufferPool = mBufferPool
            mFrontBufferedRenderTarget?.detach(true) {
                if (previousBufferPool != null) {
                    releaseBuffers(previousBufferPool)
                }
            }

            val frontBufferedLayerRenderer =
                createFrontBufferedLayerRenderer(
                    frontBufferedSurfaceControl,
                    bufferWidth,
                    bufferHeight,
                    mHardwareBufferUsageFlags
                )
            mFrontBufferedRenderTarget = mGLRenderer.createRenderTarget(
                width,
                height,
                frontBufferedLayerRenderer
            )

            mFrontBufferedLayerRenderer = frontBufferedLayerRenderer
            mFrontBufferedLayerSurfaceControl = frontBufferedSurfaceControl
            mMultiBufferedLayerRenderTarget = multiBufferTarget
            mBufferPool = bufferPool
            mWidth = width
            mHeight = height
        }
    }

    /**
     * Determines whether or not the [GLFrontBufferedRenderer] is in a valid state. That is the
     * [release] method has not been called.
     * If this returns false, then subsequent calls to [renderFrontBufferedLayer], [commit], and
     * [release] are ignored
     *
     * @return `true` if this [GLFrontBufferedRenderer] has been released, `false` otherwise
     */
    fun isValid(): Boolean = !mIsReleased

    /**
     * Render content to the front buffered layer providing optional parameters to be consumed in
     * [Callback.onDrawFrontBufferedLayer].
     * Additionally the parameter provided here will also be consumed in
     * [Callback.onDrawMultiBufferedLayer]
     * when the corresponding [commit] method is invoked, which will include all [param]s in each
     * call made to this method up to the corresponding [commit] call.
     *
     * If this [GLFrontBufferedRenderer] has been released, that is [isValid] returns `false`, this
     * call is ignored.
     *
     * @param param Optional parameter to be consumed when rendering content into the commit layer
     */
    fun renderFrontBufferedLayer(param: T) {
        if (isValid()) {
            mActiveSegment.add(param)
            mFrontBufferedRenderTarget?.requestRender()
        } else {
            Log.w(
                TAG, "Attempt to render to front buffered layer when " +
                    "GLFrontBufferedRenderer has been released"
            )
        }
    }

    /**
     * Requests to render to the multi buffered layer. This schedules a call to
     * [Callback.onDrawMultiBufferedLayer] with the parameters provided. If the front buffered
     * layer is visible, this will hide this layer after rendering to the multi buffered layer
     * is complete. This is equivalent to calling [GLFrontBufferedRenderer.renderFrontBufferedLayer]
     * for each parameter provided in the collection followed by a single call to
     * [GLFrontBufferedRenderer.commit]. This is useful for re-rendering the multi buffered
     * scene when the corresponding Activity is being resumed from the background in which the
     * contents should be re-drawn. Additionally this allows for applications to decide to
     * dynamically render to either front or multi buffered layers.
     *
     * If this [GLFrontBufferedRenderer] has been released, that is [isValid] returns 'false',
     * this call is ignored.
     *
     * @param params Parameters that to be consumed when rendering to the multi buffered layer.
     * These parameters will be provided in the corresponding call to
     * [Callback.onDrawMultiBufferedLayer]
     */
    fun renderMultiBufferedLayer(params: Collection<T>) {
        if (isValid()) {
            val segment = if (params is MutableCollection<T>) {
                params
            } else {
                ArrayList<T>().apply { addAll(params) }
            }
            mSegments.add(segment)
            mMultiBufferedLayerRenderTarget?.requestRender()
        } else {
            Log.w(
                TAG, "Attempt to render to the multi buffered layer when " +
                    "GLFrontBufferedRenderer has been released"
            )
        }
    }

    /**
     * Clears the contents of both the front and multi buffered layers. This triggers a call to
     * [Callback.onMultiBufferedLayerRenderComplete] and hides the front buffered layer.
     */
    fun clear() {
        clearParamQueues()
        mFrontBufferedLayerRenderer?.clear()
        mParentRenderLayer.clear()
    }

    /**
     * Requests to render the entire scene to the multi buffered layer and schedules a call to
     * [Callback.onDrawMultiBufferedLayer]. The parameters provided to
     * [Callback.onDrawMultiBufferedLayer] will include each argument provided to every
     * [renderFrontBufferedLayer] call since the last call to [commit] has been made.
     *
     * If this [GLFrontBufferedRenderer] has been released, that is [isValid] returns `false`,
     * this call is ignored.
     */
    fun commit() {
        if (isValid()) {
            mSegments.add(mActiveSegment.release())
            mMultiBufferedLayerRenderTarget?.requestRender()
        } else {
            Log.w(
                TAG, "Attempt to render to the multi buffered layer when " +
                    "GLFrontBufferedRenderer has been released"
            )
        }
    }

    /**
     * Requests to cancel rendering and hides the front buffered layer.
     * Unlike [commit], this does not schedule a call to render into the multi buffered layer.
     *
     * If this [GLFrontBufferedRenderer] has been released, that is [isValid] returns `false`,
     * this call is ignored.
     */
    fun cancel() {
        if (isValid()) {
            mActiveSegment.clear()
            mGLRenderer.execute(mCancelRunnable)
            mFrontBufferedLayerRenderer?.clear()
        } else {
            Log.w(TAG, "Attempt to cancel rendering to front buffer after " +
                "GLFrontBufferedRenderer has been released")
        }
    }

    /**
     * Queue a [Runnable] to be executed on the GL rendering thread. Note it is important
     * this [Runnable] does not block otherwise it can stall the GL thread.
     *
     * @param runnable to be executed
     */
    fun execute(runnable: Runnable) {
        if (isValid()) {
            mGLRenderer.execute(runnable)
        } else {
            Log.w(TAG, "Attempt to execute runnable after GLFrontBufferedRenderer has " +
                "been released")
        }
    }

    /**
     * Helper method used to detach the front and multi buffered render targets as well as
     * release SurfaceControl instances
     */
    internal fun detachTargets(cancelPending: Boolean, onReleaseComplete: (() -> Unit)? = null) {
        // GLRenderer processes requests in order on a single thread. So detach the corresponding
        // render targets then queue a request to teardown all resources
        mFrontBufferedRenderTarget?.detach(cancelPending)
        mMultiBufferedLayerRenderTarget?.detach(cancelPending)

        val frontBufferedLayerSurfaceControl = mFrontBufferedLayerSurfaceControl
        mFrontBufferedLayerSurfaceControl = null
        mGLRenderer.execute {
            mBufferPool?.let { releaseBuffers(it) }
            clearParamQueues()

            val transaction = SurfaceControlCompat.Transaction()
            if (frontBufferedLayerSurfaceControl != null) {
                transaction.reparent(frontBufferedLayerSurfaceControl, null)
            }
            mParentRenderLayer.release(transaction)
            transaction.commit()
            frontBufferedLayerSurfaceControl?.release()

            onReleaseComplete?.invoke()
        }
        mFrontBufferedRenderTarget = null
        mMultiBufferedLayerRenderTarget = null
        mWidth = -1
        mHeight = -1
    }

    /**
     * Releases the [GLFrontBufferedRenderer] and provides an optional callback that is invoked when
     * the [GLFrontBufferedRenderer] is fully torn down. If the [cancelPending] flag is true, all
     * pending requests to render into the front or multi buffered layers will be processed before
     * the [GLFrontBufferedRenderer] is torn down. Otherwise all in process requests are ignored.
     * If the [GLFrontBufferedRenderer] is already released, that is [isValid] returns `false`, this
     * method does nothing.
     *
     * @param cancelPending Flag indicating that requests to render should be processed before
     * the [GLFrontBufferedRenderer] is released
     * @param onReleaseComplete Optional callback invoked when the [GLFrontBufferedRenderer] has
     * been released. This callback is invoked on the backing GLThread
     */
    @JvmOverloads
    fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)? = null) {
        if (!isValid()) {
            Log.w(TAG, "Attempt to release GLFrontbufferedRenderer that is already released")
            return
        }
        detachTargets(cancelPending, onReleaseComplete)

        mGLRenderer.unregisterEGLContextCallback(mContextCallbacks)
        if (mIsManagingGLRenderer) {
            // If we are managing the GLRenderer that we created ourselves
            // do not cancel pending operations as we will miss callbacks that we are
            // expecting above to properly teardown resources
            // Instead rely on the cancel pending flags for detaching the front/multi buffered
            // render targets instead
            mGLRenderer.stop(false)
        }

        mExecutor.shutdown()
        mIsReleased = true
    }

    private fun createFrontBufferedLayerRenderer(
        frontBufferedLayerSurfaceControl: SurfaceControlCompat,
        bufferWidth: Int,
        bufferHeight: Int,
        usageFlags: Long
    ): FrameBufferRenderer {
        val bufferInfo = BufferInfo()
        return FrameBufferRenderer(
            object : FrameBufferRenderer.RenderCallback {
                private fun createFrontBufferLayer(usageFlags: Long): HardwareBuffer {
                    return HardwareBuffer.create(
                        bufferWidth,
                        bufferHeight,
                        HardwareBuffer.RGBA_8888,
                        1,
                        usageFlags
                    )
                }

                @WorkerThread
                override fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer {
                    var buffer = mFrontLayerBuffer
                    if (buffer == null) {
                        // Allocate and persist a FrameBuffer instance across frames
                        buffer = FrameBuffer(
                            egl,
                            createFrontBufferLayer(usageFlags)
                        ).also {
                            mFrontLayerBuffer = it
                            bufferInfo.frameBufferId = it.frameBuffer
                        }
                    }
                    return buffer
                }

                @WorkerThread
                override fun onDraw(eglManager: EGLManager) {
                    bufferInfo.apply {
                        this.width = mParentRenderLayer.getBufferWidth()
                        this.height = mParentRenderLayer.getBufferHeight()
                    }
                    mActiveSegment.next { param ->
                        mCallback.onDrawFrontBufferedLayer(
                            eglManager,
                            bufferInfo,
                            mParentRenderLayer.getTransform(),
                            param
                        )
                    }
                }

                @SuppressLint("WrongConstant")
                @WorkerThread
                override fun onDrawComplete(
                    frameBuffer: FrameBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    val transaction = SurfaceControlCompat.Transaction()
                        // Make this layer the top most layer
                        .setLayer(frontBufferedLayerSurfaceControl, Integer.MAX_VALUE)
                        .setBuffer(
                            frontBufferedLayerSurfaceControl,
                            frameBuffer.hardwareBuffer,
                            syncFenceCompat
                        )
                        .setVisibility(frontBufferedLayerSurfaceControl, true)
                    val inverseTransform = mParentRenderLayer.getInverseBufferTransform()
                    if (inverseTransform != BufferTransformHintResolver.UNKNOWN_TRANSFORM) {
                        transaction.setBufferTransform(
                            frontBufferedLayerSurfaceControl,
                            inverseTransform
                        )
                    }
                    mParentRenderLayer.buildReparentTransaction(
                        frontBufferedLayerSurfaceControl, transaction
                    )
                    mCallback.onFrontBufferedLayerRenderComplete(
                        frontBufferedLayerSurfaceControl,
                        transaction
                    )
                    transaction.commit()
                    syncFenceCompat?.close()
                }
            },
            mFrontBufferSyncStrategy
        )
    }

    private fun clearParamQueues() {
        mActiveSegment.clear()
        mSegments.clear()
    }

    /**
     * Release the buffers associated with the front buffered layer as well as the
     * [FrameBufferPool]
     */
    internal fun releaseBuffers(pool: FrameBufferPool) {
        mFrontLayerBuffer?.close()
        mFrontLayerBuffer = null
        pool.close()
    }

    internal companion object {

        internal const val TAG = "GLFrontBufferedRenderer"
    }

    @JvmDefaultWithCompatibility
    /**
     * Provides callbacks for consumers to draw into the front and multi buffered layers as well as
     * provide opportunities to synchronize [SurfaceControlCompat.Transaction]s to submit the layers
     * to the hardware compositor.
     */
    interface Callback<T> {

        /**
         * Callback invoked to render content into the front buffered layer with the specified
         * parameters.
         * @param eglManager [EGLManager] useful in configuring EGL objects to be used when issuing
         * OpenGL commands to render into the front buffered layer
         * @param bufferInfo [BufferInfo] about the buffer that is being rendered into. This
         * includes the width and height of the buffer which can be different than the corresponding
         * dimensions of the [SurfaceView] provided to the [GLFrontBufferedRenderer] as pre-rotation
         * can occasionally swap width and height parameters in order to avoid GPU composition to
         * rotate content. This should be used as input to [GLES20.glViewport].
         * Additionally this also contains a frame buffer identifier that can be used to retarget
         * rendering operations to the original destination after rendering into intermediate
         * scratch buffers.
         * @param transform Matrix that should be applied to the rendering in this callback.
         * This should be consumed as input to any vertex shader implementations. Buffers are
         * pre-rotated in advance in order to avoid unnecessary overhead of GPU composition to
         * rotate content in the same install orientation of the display.
         * This is a 4 x 4 matrix is represented as a flattened array of 16 floating point values.
         * Consumers are expected to leverage [Matrix.multiplyMM] with this parameter alongside
         * any additional transformations that are to be applied.
         * For example:
         * ```
         * val myMatrix = FloatArray(16)
         * Matrix.orthoM(
         *      myMatrix, // matrix
         *      0, // offset starting index into myMatrix
         *      0f, // left
         *      bufferInfo.bufferWidth.toFloat(), // right
         *      0f, // bottom
         *      bufferInfo.bufferHeight.toFloat(), // top
         *      -1f, // near
         *      1f, // far
         * )
         * val result = FloatArray(16)
         * Matrix.multiplyMM(result, 0, myMatrix, 0, transform, 0)
         * ```
         * @param param optional parameter provided the corresponding
         * [GLFrontBufferedRenderer.renderFrontBufferedLayer] method that triggered this request to render
         * into the front buffered layer
         */
        @WorkerThread
        fun onDrawFrontBufferedLayer(
            eglManager: EGLManager,
            bufferInfo: BufferInfo,
            transform: FloatArray,
            param: T
        )

        /**
         * Callback invoked to render content into the multid buffered layer with the specified
         * parameters.
         * @param eglManager [EGLManager] useful in configuring EGL objects to be used when issuing
         * OpenGL commands to render into the multi buffered layer
         * @param bufferInfo [BufferInfo] about the buffer that is being rendered into. This
         * includes the width and height of the buffer which can be different than the corresponding
         * dimensions of the [SurfaceView] provided to the [GLFrontBufferedRenderer] as pre-rotation
         * can occasionally swap width and height parameters in order to avoid GPU composition to
         * rotate content. This should be used as input to [GLES20.glViewport].
         * Additionally this also contains a frame buffer identifier that can be used to retarget
         * rendering operations to the original destination after rendering into intermediate
         * scratch buffers.
         * @param transform Matrix that should be applied to the rendering in this callback.
         * This should be consumed as input to any vertex shader implementations. Buffers are
         * pre-rotated in advance in order to avoid unnecessary overhead of GPU composition to
         * rotate content in the same install orientation of the display.
         * This is a 4 x 4 matrix is represented as a flattened array of 16 floating point values.
         * Consumers are expected to leverage [Matrix.multiplyMM] with this parameter alongside
         * any additional transformations that are to be applied.
         * For example:
         * ```
         * val myMatrix = FloatArray(16)
         * Matrix.orthoM(
         *      myMatrix, // matrix
         *      0, // offset starting index into myMatrix
         *      0f, // left
         *      bufferInfo.bufferWidth.toFloat(), // right
         *      0f, // bottom
         *      bufferInfo.bufferHeight.toFloat(), // top
         *      -1f, // near
         *      1f, // far
         * )
         * val result = FloatArray(16)
         * Matrix.multiplyMM(result, 0, myMatrix, 0, transform, 0)
         * ```
         * @param params optional parameter provided to render the entire scene into the multi
         * buffered layer.
         * This is a collection of all parameters provided in consecutive invocations to
         * [GLFrontBufferedRenderer.renderFrontBufferedLayer] since the last call to
         * [GLFrontBufferedRenderer.commit] has been made. After [GLFrontBufferedRenderer.commit]
         * is invoked, this collection is cleared and new
         * parameters are added on each subsequent call to
         * [GLFrontBufferedRenderer.renderFrontBufferedLayer].
         *
         * Consider the following example:
         *
         * myFrontBufferedRenderer.renderFrontBufferedLayer(1)
         * myFrontBufferedRenderer.renderFrontBufferedLayer(2)
         * myFrontBufferedRenderer.renderFrontBufferedLayer(3)
         * myFrontBufferedRenderer.commit()
         *
         * This will generate a callback to this method with the params collection containing values
         * [1, 2, 3]
         *
         * myFrontBufferedRenderer.renderFrontBufferedLayer(4)
         * myFrontBufferedRenderer.renderFrontBufferedLayer(5)
         * myFrontBufferedRenderer.commit()
         *
         * This will generate a callback to this method with the params collection containing values
         * [4, 5]
         *
         * By default [GLES20.glViewport] is invoked with the correct dimensions of the buffer that
         * is being rendered into taking into account pre-rotation transformations
         */
        @WorkerThread
        fun onDrawMultiBufferedLayer(
            eglManager: EGLManager,
            bufferInfo: BufferInfo,
            transform: FloatArray,
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
}