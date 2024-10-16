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

package androidx.graphics.opengl

import android.hardware.HardwareBuffer
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.lowlatency.BufferInfo
import androidx.graphics.lowlatency.BufferTransformHintResolver
import androidx.graphics.lowlatency.BufferTransformer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.surface.SurfaceControlCompat
import androidx.hardware.DefaultFlags
import androidx.hardware.DefaultNumBuffers
import androidx.hardware.HardwareBufferFormat
import androidx.hardware.HardwareBufferUsage
import androidx.hardware.SyncFenceCompat
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.concurrent.CountDownLatch

/**
 * Class responsible for supporting rendering to frame buffer objects that are backed by
 * [HardwareBuffer] instances. This provides for more flexibility in OpenGL rendering as it supports
 * configuration of the number of buffers within the underlying swap chain, pixel format of the
 * buffers as well as fine grained control over synchronization of buffer content.
 */
@RequiresApi(Build.VERSION_CODES.Q)
@Suppress("AcronymName")
class GLFrameBufferRenderer
internal constructor(
    private val surfaceControlProvider: SurfaceControlProvider,
    callback: Callback,
    private val mFormat: Int,
    private val mUsage: Long,
    private val mMaxBuffers: Int,
    private val mSyncStrategy: SyncStrategy,
    glRenderer: GLRenderer?
) {

    /** Builder used to create a [GLFrameBufferRenderer] with various configurations */
    class Builder {
        private var mBufferFormat = HardwareBuffer.RGBA_8888
        private var mUsageFlags = DefaultFlags
        private var mMaxBuffers = DefaultNumBuffers
        private var mGLRenderer: GLRenderer? = null
        private var mSyncStrategy: SyncStrategy = SyncStrategy.ALWAYS
        private val mSurfaceControlProvider: SurfaceControlProvider
        private val mCallback: Callback

        /**
         * Create a new [GLFrameBufferRenderer.Builder] with the provided [SurfaceView] to be the
         * parent of the [SurfaceControlCompat] that is presented on screen.
         *
         * @param surfaceView SurfaceView to be the parent of the [SurfaceControlCompat] instance
         *   used for presenting rendered content on screen
         * @param callback Callback used to render content within the corresponding buffers as well
         *   as optionally configuring [SurfaceControlCompat.Transaction] to present contents to the
         *   display
         */
        constructor(surfaceView: SurfaceView, callback: Callback) {
            mSurfaceControlProvider = SurfaceViewProvider(surfaceView)
            mCallback = callback
        }

        /**
         * Creates a new [GLFrameBufferRenderer.Builder] with the provided [SurfaceControlCompat] as
         * the parent [SurfaceControlCompat] for presenting contents to the display.
         *
         * It is the responsibility of the caller to release the provided [SurfaceControlCompat]
         * instance as the [GLFrameBufferRenderer] will consume but not release it.
         *
         * @param parentSurfaceControl Parent [SurfaceControlCompat] instance.
         * @param width Logical width of the content to render. This dimension matches what is
         *   provided from [SurfaceHolder.Callback.surfaceChanged].
         * @param height Logical height of the content to render. This dimension matches what is
         *   provided from [SurfaceHolder.Callback.surfaceChanged].
         * @param transformHint Hint used to specify how to pre-rotate content to optimize
         *   consumption of content by the display without having to introduce an additional GPU
         *   pass to handle rotation.
         */
        internal constructor(
            parentSurfaceControl: SurfaceControlCompat,
            width: Int,
            height: Int,
            transformHint: Int,
            callback: Callback
        ) {
            mSurfaceControlProvider =
                DefaultSurfaceControlProvider(parentSurfaceControl, width, height, transformHint)
            mCallback = callback
        }

        /**
         * Specify the [SyncStrategy] used for determining when to create [SyncFenceCompat] objects
         * in order to handle synchronization. The [SyncFenceCompat] instance created according to
         * the algorithm specified in the provided [SyncStrategy] will be passed to the
         * corresponding [SurfaceControlCompat.Transaction.setBuffer] call in order to ensure the
         * underlying buffer is not presented by the display until the fence signals.
         *
         * @param syncStrategy [SyncStrategy] used to determine when to create synchronization
         *   boundaries for buffer consumption. The default is [SyncStrategy.ALWAYS], indicating a
         *   fence should always be created after a request to render has been made.
         * @return The builder instance
         */
        fun setSyncStrategy(syncStrategy: SyncStrategy): Builder {
            mSyncStrategy = syncStrategy
            return this
        }

        /**
         * Specify the buffer format of the underlying buffers being rendered into by the created
         * [GLFrameBufferRenderer]. The set of valid formats is implementation-specific and may
         * depend on additional EGL extensions. The particular valid combinations for a given
         * Android version and implementation should be documented by that version.
         *
         * [HardwareBuffer.RGBA_8888] and [HardwareBuffer.RGBX_8888] are guaranteed to be supported.
         * However, consumers are recommended to query the desired HardwareBuffer configuration
         * using [HardwareBuffer.isSupported].
         *
         * See: khronos.org/registry/EGL/extensions/ANDROID/EGL_ANDROID_get_native_client_buffer.txt
         *
         * @param format Pixel format of the buffers to be rendered into. The default is RGBA_8888.
         * @return The builder instance
         */
        fun setBufferFormat(@HardwareBufferFormat format: Int): Builder {
            mBufferFormat = format
            return this
        }

        /**
         * Specify the maximum number of buffers used within the swap chain of the
         * [GLFrameBufferRenderer]. If 1 is specified, then the created [GLFrameBufferRenderer] is
         * running in "single buffer mode". In this case consumption of the buffer content would
         * need to be coordinated with the [SyncFenceCompat] instance specified by the corresponding
         * [SyncStrategy] algorithm
         *
         * @param numBuffers The number of buffers within the swap chain to be consumed by the
         *   created [GLFrameBufferRenderer]. This must be greater than zero. The default number of
         *   buffers used is 3.
         * @return The builder instance
         * @see [setSyncStrategy].
         */
        fun setMaxBuffers(@IntRange(from = 1, to = 64) numBuffers: Int): Builder {
            require(numBuffers > 0) { "Must have at least 1 buffer" }
            mMaxBuffers = numBuffers
            return this
        }

        /**
         * Specify the usage flags to be configured on the underlying [HardwareBuffer] instances
         * created by the [GLFrameBufferRenderer].
         *
         * @param usageFlags Usage flags to be configured on the created [HardwareBuffer] instances
         *   that the [GLFrameBufferRenderer] will render into. Must be one of
         *   [HardwareBufferUsage]. Note that the provided flags here are combined with the
         *   following mandatory default flags, [HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE],
         *   [HardwareBuffer.USAGE_GPU_COLOR_OUTPUT] and [HardwareBuffer.USAGE_COMPOSER_OVERLAY]
         * @return The builder instance
         */
        fun setUsageFlags(@HardwareBufferUsage usageFlags: Long): Builder {
            mUsageFlags = usageFlags or DefaultFlags
            return this
        }

        /**
         * Configure the [GLRenderer] instance to be used by the [GLFrameBufferRenderer]. By default
         * this parameter is null indicating that the [GLFrameBufferRenderer] will create and manage
         * its own [GLRenderer]. This is useful to share the same OpenGL resources and thread across
         * multiple [GLFrameBufferRenderer] instances.
         *
         * @param glRenderer The [GLRenderer] used for leveraging OpenGL resources including the GL
         *   thread
         * @return The builder instance
         */
        @Suppress("AcronymName")
        fun setGLRenderer(glRenderer: GLRenderer?): Builder {
            mGLRenderer = glRenderer
            return this
        }

        /**
         * Create the [GLFrameBufferRenderer] with the specified parameters on this [Builder]
         * instance
         *
         * @return The newly created [GLFrameBufferRenderer]
         */
        fun build(): GLFrameBufferRenderer {
            return GLFrameBufferRenderer(
                mSurfaceControlProvider,
                mCallback,
                mBufferFormat,
                mUsageFlags,
                mMaxBuffers,
                mSyncStrategy,
                mGLRenderer
            )
        }
    }

    private var mSurfaceControl: SurfaceControlCompat? = null
    private var mBufferPool: FrameBufferPool? = null
    private var mRenderTarget: GLRenderer.RenderTarget? = null

    private val mIsManagingGLRenderer: Boolean
    private var mIsReleased = false
    private val mContextCallbacks =
        object : GLRenderer.EGLContextCallback {
            override fun onEGLContextCreated(eglManager: EGLManager) {
                // NO-OP
            }

            override fun onEGLContextDestroyed(eglManager: EGLManager) {
                mBufferPool?.close()
            }
        }

    private val mGLRenderer: GLRenderer

    init {
        if (mMaxBuffers < 1) {
            throw IllegalArgumentException("FrameBufferRenderer must have at least 1 buffer")
        }
        val renderer =
            if (glRenderer == null) {
                mIsManagingGLRenderer = true
                GLRenderer().apply { start() }
            } else {
                mIsManagingGLRenderer = false
                if (!glRenderer.isRunning()) {
                    throw IllegalStateException(
                        "The provided GLRenderer must be running prior to " +
                            "creation of GLFrameBufferRenderer, " +
                            "did you forget to call GLRenderer#start()?"
                    )
                }
                glRenderer
            }
        renderer.registerEGLContextCallback(mContextCallbacks)
        mGLRenderer = renderer
        surfaceControlProvider.createSurfaceControl(
            object : SurfaceControlProvider.Callback {
                override fun onSurfaceControlDestroyed() {
                    detachTargets(true)
                }

                override fun onSurfaceControlCreated(
                    surfaceControl: SurfaceControlCompat,
                    width: Int,
                    height: Int,
                    bufferTransformer: BufferTransformer,
                    inverseTransform: Int
                ) {
                    val frameBufferPool =
                        FrameBufferPool(
                            bufferTransformer.bufferWidth,
                            bufferTransformer.bufferHeight,
                            this@GLFrameBufferRenderer.mFormat,
                            mUsage,
                            mMaxBuffers
                        )
                    val renderCallback =
                        createFrameBufferRenderer(
                            surfaceControl,
                            inverseTransform,
                            bufferTransformer,
                            frameBufferPool,
                            callback
                        )
                    mBufferPool = frameBufferPool
                    mSurfaceControl = surfaceControl
                    mRenderTarget = renderer.createRenderTarget(width, height, renderCallback)
                }

                override fun requestRender(renderComplete: Runnable?) {
                    drawAsync(renderComplete)
                }
            }
        )
    }

    /**
     * Queue a [Runnable] to be executed on the GL rendering thread. Note it is important this
     * [Runnable] does not block otherwise it can stall the GL thread.
     *
     * @param runnable to be executed
     */
    fun execute(runnable: Runnable) {
        if (isValid()) {
            mGLRenderer.execute(runnable)
        } else {
            Log.w(
                TAG,
                "Attempt to execute runnable after " + "GLFrameBufferRenderer has been released"
            )
        }
    }

    /**
     * Returns the [HardwareBufferFormat] of the buffers that are being rendered into by this
     * [GLFrameBufferRenderer]
     */
    @HardwareBufferFormat
    val bufferFormat: Int
        get() = mFormat

    /**
     * Returns the current usage flag hints of the buffers that are being rendered into by this
     * [GLFrameBufferRenderer]
     */
    @HardwareBufferUsage
    val usageFlags: Long
        get() = mUsage

    /**
     * Returns the [GLRenderer] used for issuing requests to render into the underlying buffers with
     * OpenGL.
     */
    val glRenderer: GLRenderer
        @Suppress("AcronymName") @JvmName("getGLRenderer") get() = mGLRenderer

    /**
     * Returns the [SyncStrategy] used for determining when to create [SyncFenceCompat] objects in
     * order to handle synchronization. The [SyncFenceCompat] instance created according to the
     * algorithm specified in the provided [SyncStrategy] will be passed to the corresponding
     * [SurfaceControlCompat.Transaction.setBuffer] call in order to ensure the underlying buffer is
     * not presented by the display until the fence signals.
     */
    val syncStrategy: SyncStrategy
        get() = mSyncStrategy

    /**
     * Returns the number of buffers within the swap chain used for rendering with this
     * [GLFrameBufferRenderer]
     */
    val maxBuffers: Int
        get() = mMaxBuffers

    private var mCurrentFrameBuffer: FrameBuffer? = null

    internal fun createFrameBufferRenderer(
        surfaceControl: SurfaceControlCompat,
        inverseTransform: Int,
        bufferTransformer: BufferTransformer,
        frameBufferPool: FrameBufferPool,
        callback: Callback
    ): FrameBufferRenderer =
        FrameBufferRenderer(
            object : FrameBufferRenderer.RenderCallback {

                private val width = bufferTransformer.logicalWidth
                private val height = bufferTransformer.logicalHeight

                private val bufferInfo =
                    BufferInfo().apply {
                        this.width = bufferTransformer.bufferWidth
                        this.height = bufferTransformer.bufferHeight
                    }

                override fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer {
                    val currentFrameBuffer = mCurrentFrameBuffer
                    // Single buffer mode if we already allocated 1 buffer just return the previous
                    // one
                    return if (mMaxBuffers == 1 && currentFrameBuffer != null) {
                        currentFrameBuffer
                    } else {
                        frameBufferPool.obtain(egl).also {
                            bufferInfo.frameBufferId = it.frameBuffer
                            mCurrentFrameBuffer = it
                        }
                    }
                }

                override fun onDraw(eglManager: EGLManager) {
                    val buffer = mCurrentFrameBuffer
                    if (buffer != null && !buffer.isClosed) {
                        callback.onDrawFrame(
                            eglManager,
                            width,
                            height,
                            bufferInfo,
                            bufferTransformer.transform
                        )
                    }
                }

                override fun onDrawComplete(
                    frameBuffer: FrameBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    if (surfaceControl.isValid() && !frameBuffer.isClosed) {
                        val transaction =
                            SurfaceControlCompat.Transaction()
                                .setVisibility(surfaceControl, true)
                                .setBuffer(
                                    surfaceControl,
                                    frameBuffer.hardwareBuffer,
                                    syncFenceCompat
                                ) { releaseFence ->
                                    if (mGLRenderer.isRunning()) {
                                        mGLRenderer.execute {
                                            callback.onBufferReleased(frameBuffer, releaseFence)
                                        }
                                    }
                                    if (mMaxBuffers > 1 || frameBufferPool.isClosed) {
                                        // Release the previous buffer only if we are not in single
                                        // buffered
                                        // mode
                                        frameBufferPool.release(frameBuffer, releaseFence)
                                    }
                                }
                        if (inverseTransform != BufferTransformHintResolver.UNKNOWN_TRANSFORM) {
                            transaction.setBufferTransform(surfaceControl, inverseTransform)
                        }
                        callback.onDrawComplete(
                            surfaceControl,
                            transaction,
                            frameBuffer,
                            syncFenceCompat
                        )
                        transaction.commit()
                    }
                }
            },
            mSyncStrategy
        )

    internal fun drawAsync(onComplete: Runnable? = null) {
        val renderTarget = mRenderTarget
        val renderer = mGLRenderer
        if (renderTarget != null && renderer.isRunning()) {
            // Register a callback in case the GLRenderer is torn down while we are waiting
            // for rendering to complete. In this case invoke the drawFinished callback
            // either if the render is complete or if the GLRenderer is torn down, whatever
            // comes first
            val eglContextCallback =
                object : GLRenderer.EGLContextCallback {
                    override fun onEGLContextCreated(eglManager: EGLManager) {
                        // NO-OP
                    }

                    override fun onEGLContextDestroyed(eglManager: EGLManager) {
                        onComplete?.run()
                        renderer.unregisterEGLContextCallback(this)
                    }
                }
            renderer.registerEGLContextCallback(eglContextCallback)
            mRenderTarget?.requestRender {
                onComplete?.run()
                renderer.unregisterEGLContextCallback(eglContextCallback)
            }
        } else {
            onComplete?.run()
        }
    }

    /**
     * Release resources associated with the [GLFrameBufferRenderer]. After this method is invoked,
     * the [GLFrameBufferRenderer] is in an invalid state and can no longer handle rendering
     * content.
     *
     * @param cancelPending If true, pending render requests are cancelled immediately. If false,
     *   pending render requests are completed before releasing the renderer.
     * @param onReleaseCallback Optional callback to be invoked on the underlying OpenGL thread when
     *   releasing resources has been completed
     */
    @JvmOverloads
    fun release(cancelPending: Boolean, onReleaseCallback: (() -> Unit)? = null) {
        if (!mIsReleased) {
            detachTargets(cancelPending, onReleaseCallback)
            surfaceControlProvider.release()

            mGLRenderer.unregisterEGLContextCallback(mContextCallbacks)
            if (mIsManagingGLRenderer) {
                mGLRenderer.stop(false)
            }

            mIsReleased = true
        } else {
            Log.w(TAG, "Attempt to release already released GLFrameBufferRenderer")
        }
    }

    internal fun detachTargets(cancelPending: Boolean, onReleaseComplete: (() -> Unit)? = null) {
        val frameBufferPool = mBufferPool
        val renderTarget = mRenderTarget
        val surfaceControl = mSurfaceControl
        renderTarget?.detach(cancelPending)

        mGLRenderer.execute {
            mCurrentFrameBuffer?.let { buffer -> frameBufferPool?.release(buffer) }
            surfaceControl?.let { sc ->
                SurfaceControlCompat.Transaction().reparent(sc, null).apply {
                    commit()
                    close()
                }
                sc.release()
            }
            frameBufferPool?.close()
            onReleaseComplete?.invoke()
        }
        mBufferPool = null
        mSurfaceControl = null
        mRenderTarget = null
    }

    /**
     * Determines whether or not the [GLFrameBufferRenderer] is in a valid state. That is the
     * [release] method has not been called. If this returns false, then subsequent calls to
     * [render], and [release] are ignored
     *
     * @return `true` if this [GLFrameBufferRenderer] has been released, `false` otherwise
     */
    fun isValid(): Boolean = !mIsReleased

    /**
     * Render content to a buffer and present the result to the display.
     *
     * If this [GLFrameBufferRenderer] has been released, that is [isValid] returns `false`, this
     * call is ignored.
     */
    fun render() {
        if (!mIsReleased) {
            mRenderTarget?.requestRender()
        } else {
            Log.w(TAG, "renderer is released, ignoring request")
        }
    }

    /**
     * [GLFrameBufferRenderer] callbacks that are invoked to render OpenGL content within the
     * underlying buffers. This includes an optional callback to be used to configure the underlying
     * [SurfaceControlCompat.Transaction] used to present content to the display
     */
    interface Callback {

        /**
         * Callback invoked on the thread backed by the [GLRenderer] to render content into a buffer
         * with the specified parameters.
         *
         * @param eglManager [EGLManager] useful in configuring EGL objects to be used when issuing
         *   OpenGL commands to render into the front buffered layer
         * @param width Logical width of the content to render. This dimension matches what is
         *   provided from [SurfaceHolder.Callback.surfaceChanged]
         * @param height Logical height of the content to render. This dimension matches what is
         *   provided from [SurfaceHolder.Callback.surfaceChanged]
         * @param bufferInfo [BufferInfo] about the buffer that is being rendered into. This
         *   includes the width and height of the buffer which can be different than the
         *   corresponding dimensions of the [SurfaceView] provided to the [GLFrameBufferRenderer]
         *   as pre-rotation can occasionally swap width and height parameters in order to avoid GPU
         *   composition to rotate content. This should be used as input to [GLES20.glViewport].
         *   Additionally this also contains a frame buffer identifier that can be used to retarget
         *   rendering operations to the original destination after rendering into intermediate
         *   scratch buffers.
         * @param transform Matrix that should be applied to the rendering in this callback. This
         *   should be consumed as input to any vertex shader implementations. Buffers are
         *   pre-rotated in advance in order to avoid unnecessary overhead of GPU composition to
         *   rotate content in the same install orientation of the display. This is a 4 x 4 matrix
         *   is represented as a flattened array of 16 floating point values. Consumers are expected
         *   to leverage [Matrix.multiplyMM] with this parameter alongside any additional
         *   transformations that are to be applied. For example:
         * ```
         * val myMatrix = FloatArray(16)
         * Matrix.orthoM(
         *      myMatrix, // matrix
         *      0, // offset starting index into myMatrix
         *      0f, // left
         *      bufferInfo.width.toFloat(), // right
         *      0f, // bottom
         *      bufferInfo.height.toFloat(), // top
         *      -1f, // near
         *      1f, // far
         * )
         * val result = FloatArray(16)
         * Matrix.multiplyMM(result, 0, myMatrix, 0, transform, 0)
         * ```
         *
         * @sample androidx.graphics.core.samples.glFrameBufferSample
         */
        @WorkerThread
        fun onDrawFrame(
            eglManager: EGLManager,
            width: Int,
            height: Int,
            bufferInfo: BufferInfo,
            transform: FloatArray
        )

        /**
         * Optional callback invoked the thread backed by the [GLRenderer] when rendering to a
         * buffer is complete but before the buffer is submitted to the hardware compositor. This
         * provides consumers a mechanism for synchronizing the transaction with other
         * [SurfaceControlCompat] objects that maybe rendered within the scene.
         *
         * @param targetSurfaceControl Handle to the [SurfaceControlCompat] where the buffer is
         *   presented. This can be used to configure various properties of the
         *   [SurfaceControlCompat] like z-ordering or visibility with the corresponding
         *   [SurfaceControlCompat.Transaction].
         * @param transaction Current [SurfaceControlCompat.Transaction] to apply updated buffered
         *   content to the front buffered layer.
         * @param frameBuffer The buffer that has been rendered into and is ready to be displayed.
         *   The [HardwareBuffer] backing this [FrameBuffer] is already configured to be presented
         *   for the targetSurfaceControl. That is [SurfaceControlCompat.Transaction.setBuffer] is
         *   already invoked with the given [HardwareBuffer] and optional [SyncFenceCompat] instance
         *   before this method is invoked.
         * @param syncFence Optional [SyncFenceCompat] is used to determine when rendering is done
         *   and reflected within the given frameBuffer.
         */
        @WorkerThread
        fun onDrawComplete(
            targetSurfaceControl: SurfaceControlCompat,
            transaction: SurfaceControlCompat.Transaction,
            frameBuffer: FrameBuffer,
            syncFence: SyncFenceCompat?
        ) {
            // NO-OP
        }

        /**
         * Optional callback invoked the thread backed by the [GLRenderer] when the provided
         * framebuffer is released. That is the given [FrameBuffer] instance is no longer being
         * presented and is not visible.
         *
         * @param frameBuffer The buffer that is no longer being presented and has returned to the
         *   buffer allocation pool
         * @param releaseFence Optional fence that must be waited upon before the [FrameBuffer] can
         *   be reused. The framework will invoke this callback early to improve performance and
         *   signal the fence when it is ready to be re-used.
         */
        @WorkerThread
        fun onBufferReleased(frameBuffer: FrameBuffer, releaseFence: SyncFenceCompat?) {
            // NO-OP
        }
    }

    /**
     * Provider interface used to delegate creation and potential lifecycle callbacks associated
     * with the corresponding [SurfaceControlCompat] instance
     */
    internal interface SurfaceControlProvider {

        /**
         * Request a [SurfaceControlCompat] to be created and invokes the corresponding callback
         * when the created [SurfaceControlCompat] instance is ready for consumption
         */
        fun createSurfaceControl(callback: Callback)

        /** Release resources associated with the [SurfaceControlProvider] */
        fun release()

        /**
         * Callbacks invoked by the [SurfaceControlProvider] to consumers of the created
         * [SurfaceControlCompat] instance
         */
        interface Callback {

            /**
             * Callback invoked when resources associated with the created [SurfaceControlCompat]
             * instance should be destroyed
             */
            fun onSurfaceControlDestroyed()

            /**
             * Callback invoked when the [SurfaceControlCompat] is created. This includes the
             * logical width/height as well as a [BufferTransformer] instance that provides the
             * metadata necessary to pre-rotate content
             */
            fun onSurfaceControlCreated(
                surfaceControl: SurfaceControlCompat,
                width: Int,
                height: Int,
                bufferTransformer: BufferTransformer,
                inverseTransform: Int
            )

            /**
             * Requests the consumer of the [SurfaceControlCompat] instance to render content to be
             * presented by the [SurfaceControlCompat] instance and invoke a callback when rendering
             * is complete.
             */
            fun requestRender(renderComplete: Runnable? = null)
        }
    }

    /**
     * Default [SurfaceControlProvider] instance that returns the dependencies given to it to the
     * consumer of the [SurfaceControlCompat]
     */
    internal class DefaultSurfaceControlProvider(
        private val surfaceControl: SurfaceControlCompat,
        private val width: Int,
        private val height: Int,
        private val transformHint: Int,
    ) : SurfaceControlProvider {

        private val bufferTransformer = BufferTransformer()

        private var mSurfaceControlCallback: SurfaceControlProvider.Callback? = null

        override fun createSurfaceControl(callback: SurfaceControlProvider.Callback) {
            val inverse = bufferTransformer.invertBufferTransform(transformHint)
            bufferTransformer.computeTransform(width, height, inverse)
            callback.onSurfaceControlCreated(
                surfaceControl,
                width,
                height,
                bufferTransformer,
                inverse
            )
            mSurfaceControlCallback = callback
        }

        override fun release() {
            // NO-OP
        }
    }

    /**
     * [SurfaceControlProvider] instance that creates a [SurfaceControlCompat] instance with the
     * provided SurfaceView as the parent of the created [SurfaceControlCompat]. This implementation
     * handles all lifecycle callbacks associated with the underlying SurfaceHolder.Callback
     * attached to the holder on the given SurfaceView.
     */
    internal class SurfaceViewProvider(private var surfaceView: SurfaceView?) :
        SurfaceControlProvider {
        private val mTransformResolver = BufferTransformHintResolver()

        private var mSurfaceControl: SurfaceControlCompat? = null
        private var mSurfaceHolderCallback: SurfaceHolder.Callback2? = null
        private var mSurfaceControlCallback: SurfaceControlProvider.Callback? = null

        internal fun createSurfaceControl(
            surfaceView: SurfaceView,
            callback: SurfaceControlProvider.Callback
        ) {
            // Destroy previously created SurfaceControl as we are creating a new instance
            callback.onSurfaceControlDestroyed()

            val width = surfaceView.width
            val height = surfaceView.height
            val transformHint = mTransformResolver.getBufferTransformHint(surfaceView)
            val bufferTransformer = BufferTransformer()
            val inverse = bufferTransformer.invertBufferTransform(transformHint)
            bufferTransformer.computeTransform(surfaceView.width, surfaceView.height, inverse)
            val surfaceControl =
                SurfaceControlCompat.Builder()
                    .setName("GLFrameBufferRendererTarget")
                    .setParent(surfaceView)
                    .build()

            callback.onSurfaceControlCreated(
                surfaceControl,
                width,
                height,
                bufferTransformer,
                inverse
            )

            mSurfaceControl = surfaceControl
            mSurfaceControlCallback = callback
        }

        override fun createSurfaceControl(callback: SurfaceControlProvider.Callback) {
            surfaceView?.let { target ->
                val surfaceHolderCallback =
                    object : SurfaceHolder.Callback2 {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            // NO-OP wait for surfaceChanged callback
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            surfaceFormat: Int,
                            width: Int,
                            height: Int
                        ) {
                            if (width > 0 && height > 0) {
                                createSurfaceControl(target, callback)
                            } else {
                                Log.w(
                                    TAG,
                                    "Invalid dimensions provided, width and height must be > 0. " +
                                        "width: $width height: $height"
                                )
                            }
                        }

                        override fun surfaceDestroyed(p0: SurfaceHolder) {
                            callback.onSurfaceControlDestroyed()
                        }

                        override fun surfaceRedrawNeeded(p0: SurfaceHolder) {
                            val latch = CountDownLatch(1)
                            callback.requestRender { latch.countDown() }
                            latch.await()
                        }

                        override fun surfaceRedrawNeededAsync(
                            holder: SurfaceHolder,
                            drawingFinished: Runnable
                        ) {
                            callback.requestRender(drawingFinished)
                        }
                    }
                val holder = target.holder
                holder.addCallback(surfaceHolderCallback)
                if (holder.surface != null && holder.surface.isValid) {
                    if (target.width > 0 && target.height > 0) {
                        createSurfaceControl(target, callback)
                    }
                }
                mSurfaceHolderCallback = surfaceHolderCallback
            }
        }

        override fun release() {
            surfaceView?.holder?.removeCallback(mSurfaceHolderCallback)
            surfaceView = null
        }
    }

    internal companion object {
        internal val TAG = "GLFrameBufferRenderer"
    }
}
