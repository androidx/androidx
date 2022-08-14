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
import android.os.Build
import android.util.Log
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.surface.SurfaceControlCompat
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Class responsible for supporting a "front buffered" rendering system. This allows for lower
 * latency graphics by leveraging a combination of front buffered and double buffered content
 * layers.
 * Active content is rendered first into the front buffered layer which is simultaneously being
 * presented to the display. Periodically content is rendered into the double buffered layer which
 * will have more traditional latency guarantees, however, minimize the impact of visual artifacts
 * due to graphical tearing.
 *
 * @param surfaceView Target SurfaceView to act as the parent rendering layer for double buffered
 *  content
 * @param callback Callbacks used to render into front and double buffered layers as well as
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
     * [ParentRenderLayer] used to contain both the front and double buffered layers
     */
    private val mParentRenderLayer: ParentRenderLayer<T> = SurfaceViewRenderLayer(surfaceView)

    /**
     * Callbacks invoked to render into the front and double buffered layers in addition to
     * providing consumers an opportunity to specify any potential additional interactions that must
     * be synchronized with the [SurfaceControlCompat.Transaction] to show/hide visibility of the
     * front buffered layer as well as updating double buffered layers
     */
    private val mCallback = callback

    /**
     * [GLRenderer.EGLContextCallback]s used to release the corresponding [RenderBufferPool]
     * if the [GLRenderer] is torn down.
     * This is especially helpful if a [GLRenderer] is being provided and shared across other
     * [GLRenderer.RenderTarget] instances in which case it can be released externally from
     * [GLFrontBufferedRenderer]
     */
    private val mContextCallbacks = object : GLRenderer.EGLContextCallback {
        override fun onEGLContextCreated(eglManager: EGLManager) {
            // no-op
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
            release(true)
        }

        override fun obtainDoubleBufferedLayerParams(): MutableCollection<T> =
            mParentBufferParamQueue

        override fun getFrontBufferedLayerSurfaceControl(): SurfaceControlCompat? =
            mFrontBufferedLayerSurfaceControl

        override fun getRenderBufferPool(): RenderBufferPool? =
            mBufferPool
    }

    /**
     * Queue of parameters to be consumed in [Callback.onDrawFrontBufferedLayer] with the parameter
     * provided in [renderFrontBufferedLayer]
     */
    private val mFrontBufferQueueParams = ConcurrentLinkedQueue<T>()

    /**
     * Collection of parameters to be consumed in [Callback.onDoubleBufferedLayerRenderComplete]
     * with the parameters defined in consecutive calls to [renderFrontBufferedLayer].
     * Once the corresponding [Callback.onDoubleBufferedLayerRenderComplete] callback is invoked,
     * this collection is cleared and new parameters are added to it with consecutive calls to
     * [renderFrontBufferedLayer].
     */
    private val mParentBufferParamQueue = Collections.synchronizedList(ArrayList<T>())

    /**
     * [RenderBuffer] used for rendering into the front buffered layer. This buffer is persisted
     * across frames as part of front buffered rendering and is not expected to be released again
     * after the corresponding [SurfaceControlCompat.Transaction] that submits this buffer is
     * applied as per the implementation of "scan line racing" that is done for front buffered
     * rendering
     */
    private var mFrontLayerBuffer: RenderBuffer? = null

    /**
     * [RenderBufferPool] used to cycle through [RenderBuffer] instances that are released when
     * the [HardwareBuffer] within the [RenderBuffer] is already displayed by the hardware
     * compositor
     */
    private var mBufferPool: RenderBufferPool? = null

    /**
     * [GLRenderer.RenderCallback] used for drawing into the front buffered layer
     */
    private var mFrontBufferedLayerRenderer: HardwareBufferRenderer? = null

    /**
     * [SurfaceControlCompat] used to configure buffers and visibility of the front buffered layer
     */
    private var mFrontBufferedLayerSurfaceControl: SurfaceControlCompat? = null

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
     * [GLRenderer] used to issue requests to render into front/double buffered layers
     */
    private val mGLRenderer: GLRenderer

    /**
     * Flag indicating if the [GLRenderer] being used was created internally within
     * [GLFrontBufferedRenderer] as opposed to being provided by the consumer.
     * If the former, then the [GLFrontBufferedRenderer] is responsible for stopping/releasing this
     * [GLRenderer] in the [release] method. If this is being provided, then we should not be
     * releasing this [GLRenderer] as it maybe used by other consumers.
     * In this case, only the front/double buffered [GLRenderer.RenderTarget]s are detached.
     */
    private val mIsManagingGLRenderer: Boolean

    /**
     * [GLRenderer.RenderTarget] used to issue requests to render into the front buffered layer
     */
    private var mFrontBufferedRenderTarget: GLRenderer.RenderTarget? = null

    /**
     * [GLRenderer.RenderTarget] used to issue requests to render into the double buffered layer
     */
    private var mDoubleBufferedLayerRenderTarget: GLRenderer.RenderTarget? = null

    /**
     * Flag to determine if the [GLFrontBufferedRenderer] has previously been released. If this flag
     * is true, then subsequent requests to [renderFrontBufferedLayer], [commit], and [release] are
     * ignored.
     */
    private var mIsReleased = false

    init {
        mParentRenderLayer.setParentLayerCallbacks(mParentLayerCallback)
        val renderer = if (glRenderer == null) {
            // If we have not been provided a [GLRenderer] then we should create/start one ourselves
            mIsManagingGLRenderer = true
            GLRenderer().apply { start() }
        } else {
            // ... otherwise use the [GLRenderer] that is being provided for us
            mIsManagingGLRenderer = false
            glRenderer
        }
        renderer.registerEGLContextCallback(mContextCallbacks)

        mDoubleBufferedLayerRenderTarget =
            mParentRenderLayer.createRenderTarget(renderer, mCallback)
        mGLRenderer = renderer
    }

    internal fun update(width: Int, height: Int) {
        if (mWidth != width || mHeight != height) {
            mFrontBufferedLayerSurfaceControl?.release()

            val frontBufferedSurfaceControl = SurfaceControlCompat.Builder()
                .setName("FrontBufferedSurfaceControl")
                .apply {
                    mParentRenderLayer.setParent(this)
                }
                .build()

            val bufferPool = RenderBufferPool(
                width,
                height,
                format = HardwareBuffer.RGBA_8888,
                usage = obtainHardwareBufferUsageFlags(),
                maxPoolSize = 5
            )

            val previousBufferPool = mBufferPool
            mFrontBufferedRenderTarget?.detach(true) {
                if (previousBufferPool != null) {
                    releaseBuffers(previousBufferPool)
                }
            }

            val frontBufferedLayerRenderer =
                createFrontBufferedLayerRenderer(frontBufferedSurfaceControl)
            mFrontBufferedRenderTarget = mGLRenderer.createRenderTarget(
                width,
                height,
                frontBufferedLayerRenderer
            )

            mFrontBufferedLayerRenderer = frontBufferedLayerRenderer
            mFrontBufferedLayerSurfaceControl = frontBufferedSurfaceControl
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
     * [Callback.onDrawDoubleBufferedLayer]
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
            mFrontBufferQueueParams.add(param)
            mParentBufferParamQueue.add(param)
            mFrontBufferedRenderTarget?.requestRender()
        } else {
            Log.w(TAG, "Attempt to render to front buffered layer when " +
                "GLFrontBufferedRenderer has been released")
        }
    }

    /**
     * Clears the contents of both the front and double buffered layers. This triggers a call to
     * [Callback.onDoubleBufferedLayerRenderComplete] and hides the front buffered layer.
     */
    fun clear() {
        clearParamQueues()
        mFrontBufferedLayerRenderer?.clear()
        mParentRenderLayer.clear()
    }

    /**
     * Requests to render the entire scene to the double buffered layer and schedules a call to
     * [Callback.onDoubleBufferedLayerRenderComplete]. The parameters provided to
     * [Callback.onDoubleBufferedLayerRenderComplete] will include each argument provided to every
     * [renderFrontBufferedLayer] call since the last call to [commit] has been made.
     *
     * If this [GLFrontBufferedRenderer] has been released, that is [isValid] returns `false`,
     * this call is ignored.
     */
    fun commit() {
        if (isValid()) {
            mDoubleBufferedLayerRenderTarget?.requestRender()
            mFrontBufferedLayerRenderer?.clear()
        } else {
            Log.w(TAG, "Attempt to render to the double buffered layer when " +
                "GLFrontBufferedRenderer has been released")
        }
    }

    /**
     * Releases the [GLFrontBufferedRenderer] and provides an optional callback that is invoked when
     * the [GLFrontBufferedRenderer] is fully torn down. If the [cancelPending] flag is true, all
     * pending requests to render into the front or double buffered layers will be processed before
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
        // Wrap the callback into a separate lambda to ensure it is invoked only after
        // both the front and double buffered layer target renderers are detached
        var callbackCount = 0
        var expectedCount = 0
        if (mFrontBufferedRenderTarget?.isAttached() == true) {
            expectedCount++
        }

        if (mDoubleBufferedLayerRenderTarget?.isAttached() == true) {
            expectedCount++
        }
        val frontBufferedLayerSurfaceControl = mFrontBufferedLayerSurfaceControl
        val wrappedCallback: (GLRenderer.RenderTarget) -> Unit = {
            callbackCount++
            if (callbackCount >= expectedCount) {
                mBufferPool?.let { releaseBuffers(it) }
                clearParamQueues()

                frontBufferedLayerSurfaceControl?.let {
                    val transaction = SurfaceControlCompat.Transaction()
                        .reparent(it, null)
                    mParentRenderLayer.release(transaction)
                    transaction.commit()
                    it.release()
                }

                onReleaseComplete?.invoke()
            }
        }
        mFrontBufferedRenderTarget?.detach(cancelPending, wrappedCallback)
        mDoubleBufferedLayerRenderTarget?.detach(cancelPending, wrappedCallback)
        mFrontBufferedRenderTarget = null
        mDoubleBufferedLayerRenderTarget = null

        mGLRenderer.unregisterEGLContextCallback(mContextCallbacks)
        if (mIsManagingGLRenderer) {
            // If we are managing the GLRenderer that we created ourselves
            // do not cancel pending operations as we will miss callbacks that we are
            // expecting above to properly teardown resources
            // Instead rely on the cancel pending flags for detaching the front/double buffered
            // render targets instead
            mGLRenderer.stop(false)
        }

        mFrontBufferedLayerSurfaceControl = null
        mParentRenderLayer.setParentLayerCallbacks(null)
        mIsReleased = true
    }

    private fun createFrontBufferedLayerRenderer(
        frontBufferedLayerSurfaceControl: SurfaceControlCompat
    ) = HardwareBufferRenderer(
        object : HardwareBufferRenderer.RenderCallbacks {

            @WorkerThread
            override fun obtainRenderBuffer(egl: EGLSpec): RenderBuffer {
                var buffer = mFrontLayerBuffer
                if (buffer == null) {
                    // Allocate and persist a RenderBuffer instance across frames
                    buffer = mBufferPool?.obtain(egl).also { mFrontLayerBuffer = it }
                       ?: throw IllegalArgumentException("Unable to obtain RenderBuffer")
                }
                return buffer
            }

            @WorkerThread
            override fun onDraw(eglManager: EGLManager) {
                try {
                    // Explicitly call remove in order to delineate between scenarios where
                    // no parameters are provided and the consumer explicitly supports nullable
                    // parameters.
                    // If poll was used instead, we would not be able to determine if the nullable
                    // parameter was because there were no items in the queue or the consumer
                    // explicitly provided null as a placeholder
                    mCallback.onDrawFrontBufferedLayer(eglManager, mFrontBufferQueueParams.remove())
                } catch (_: NoSuchElementException) {
                    // Skip rendering if we have been told to render but we do not have parameters
                    // Because the call to render to the front buffer takes in a parameter we should
                    // not run into this scenario.
                }
            }

            @WorkerThread
            override fun onDrawComplete(renderBuffer: RenderBuffer) {
                val transaction = SurfaceControlCompat.Transaction()
                    // Make this layer the top most layer
                    .setLayer(frontBufferedLayerSurfaceControl, Integer.MAX_VALUE)
                    .setBuffer(
                        frontBufferedLayerSurfaceControl,
                        renderBuffer.hardwareBuffer,
                        null
                    )
                    .setVisibility(frontBufferedLayerSurfaceControl, true)
                mParentRenderLayer.buildReparentTransaction(
                    frontBufferedLayerSurfaceControl, transaction)
                mCallback.onFrontBufferedLayerRenderComplete(
                    frontBufferedLayerSurfaceControl,
                    transaction
                )
                transaction.commit()
            }
        }
    )

    private fun clearParamQueues() {
        mFrontBufferQueueParams.clear()
        mParentBufferParamQueue.clear()
    }

    /**
     * Release the buffers associated with the front buffered layer as well as the
     * [RenderBufferPool]
     */
    internal fun releaseBuffers(pool: RenderBufferPool) {
        mFrontLayerBuffer?.let {
            pool.release(it)
            mFrontLayerBuffer = null
        }
        pool.close()
    }

    companion object {

        internal const val TAG = "GLFrontBufferedRenderer"

        // Leverage the same value as HardwareBuffer.USAGE_COMPOSER_OVERLAY.
        // While this constant was introduced in the SDK in the Android T release, it has
        // been available within the NDK as part of
        // AHardwareBuffer_UsageFlags#AHARDWAREBUFFER_USAGE_COMPOSER_OVERLAY for quite some time.
        // This flag is required for usage of ASurfaceTransaction#setBuffer
        // Use a separate constant with the same value to avoid SDK warnings of accessing the
        // newly added constant in the SDK.
        // See:
        // developer.android.com/ndk/reference/group/a-hardware-buffer#ahardwarebuffer_usageflags
        private const val USAGE_COMPOSER_OVERLAY: Long = 2048L

        /**
         * Flags that are expected to be supported on all [HardwareBuffer] instances
         */
        internal const val BaseFlags =
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or
                HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or
                USAGE_COMPOSER_OVERLAY

        internal fun obtainHardwareBufferUsageFlags(): Long =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                UsageFlagsVerificationHelper.obtainUsageFlagsV33()
            } else {
                BaseFlags
            }
    }

    /**
     * Provides callbacks for consumers to draw into the front and double buffered layers as well as
     * provide opportunities to synchronize [SurfaceControlCompat.Transaction]s to submit the layers
     * to the hardware compositor.
     */
    interface Callback<T> {

        /**
         * Callback invoked to render content into the front buffered layer with the specified
         * parameters.
         * @param eglManager [EGLManager] useful in configuring EGL objects to be used when issuing
         * OpenGL commands to render into the front buffered layer
         * @param param optional parameter provided the corresponding
         * [GLFrontBufferedRenderer.renderFrontBufferedLayer] method that triggered this request to render
         * into the front buffered layer
         */
        @WorkerThread
        fun onDrawFrontBufferedLayer(eglManager: EGLManager, param: T)

        /**
         * Callback invoked to render content into the doubled buffered layer with the specified
         * parameters.
         * @param eglManager [EGLManager] useful in configuring EGL objects to be used when issuing
         * OpenGL commands to render into the double buffered layer
         * @param params optional parameter provided to render the entire scene into the double
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
         */
        @WorkerThread
        fun onDrawDoubleBufferedLayer(eglManager: EGLManager, params: Collection<T>)

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
         * Optional callback invoked when rendering to the double buffered layer is complete but
         * before the buffers are submitted to the hardware compositor.
         * This provides consumers a mechanism for synchronizing the transaction with other
         * [SurfaceControlCompat] objects that maybe rendered within the scene.
         *
         * @param frontBufferedLayerSurfaceControl Handle to the [SurfaceControlCompat] where the
         * front buffered layer content is drawn. This can be used to configure various properties
         * of the [SurfaceControlCompat] like z-ordering or visibility with the corresponding
         * [SurfaceControlCompat.Transaction].
         * @param transaction Current [SurfaceControlCompat.Transaction] to apply updated buffered
         * content to the double buffered layer.
         */
        @WorkerThread
        fun onDoubleBufferedLayerRenderComplete(
            frontBufferedLayerSurfaceControl: SurfaceControlCompat,
            transaction: SurfaceControlCompat.Transaction
        ) {
            // Default implementation is a no-op
        }
    }
}

/**
 * Helper class to avoid class verification failures
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class UsageFlagsVerificationHelper private constructor() {
    companion object {

        /**
         * Helper method to determine if a particular HardwareBuffer usage flag is supported.
         * Even though the FRONT_BUFFER_USAGE and COMPOSER_OVERLAY flags are introduced in
         * Android T, not all devices may support this flag. So we conduct a capability query
         * with a sample 1x1 HardwareBuffer with the provided flag to see if it is compatible
         */
        // Suppressing WrongConstant warnings as we are leveraging a constant with the same value
        // as HardwareBuffer.USAGE_COMPOSER_OVERLAY to avoid SDK checks as the constant has been
        // supported in the NDK for several platform releases.
        // See:
        // developer.android.com/ndk/reference/group/a-hardware-buffer#ahardwarebuffer_usageflags
        @SuppressLint("WrongConstant")
        @RequiresApi(Build.VERSION_CODES.Q)
        @androidx.annotation.DoNotInline
        internal fun isSupported(flag: Long): Boolean =
            HardwareBuffer.isSupported(
                1, // width
                1, // height
                HardwareBuffer.RGBA_8888, // format
                1, // layers
                GLFrontBufferedRenderer.BaseFlags or flag
            )

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @androidx.annotation.DoNotInline
        fun obtainUsageFlagsV33(): Long {
            // First verify if the front buffer usage flag is supported along with the
            // "usage composer overlay" flag that was introduced in API level
            return if (isSupported(HardwareBuffer.USAGE_FRONT_BUFFER)) {
                GLFrontBufferedRenderer.BaseFlags or HardwareBuffer.USAGE_FRONT_BUFFER
            } else {
                GLFrontBufferedRenderer.BaseFlags
            }
        }
    }
}