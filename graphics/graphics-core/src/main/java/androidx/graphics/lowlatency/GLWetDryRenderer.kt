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

import android.graphics.PixelFormat
import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Log
import android.view.SurfaceControl
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EglManager
import androidx.graphics.opengl.egl.EglSpec
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Class responsible for supporting a "wet"/"dry" rendering system. This allows for lower latency
 * graphics by leveraging a combination of front buffered and double buffered content layers.
 * Active content is rendered first into the wet layer which is simultaneously being presented
 * to the display. Periodically content is rendered into the "dry" or double buffered layer which
 * will have more traditional latency guarantees, however, minimize the impact of visual artifacts
 * due to graphical tearing.
 *
 * @param surfaceView Target SurfaceView to act as the parent rendering layer for dry
 *  content
 * @param callback Callbacks used to render into wet and dry layers as well as
 *  configuring [SurfaceControl.Transaction]s for controlling these layers in addition to
 *  other [SurfaceControl] instances that must be updated atomically within the user
 *  interface. These callbacks are invoked on the backing GL Thread.
 * @param glRenderer Optional [GLRenderer] instance that this [GLWetDryRenderer] should use
 *  for coordinating requests to render content. If this parameter is specified, the caller
 *  of this API is responsible for releasing resources on [GLRenderer] by calling
 *  [GLRenderer.stop]. Otherwise [GLWetDryRenderer] will create and manage its own
 *  [GLRenderer] internally and will automatically release its resources within
 *  [GLWetDryRenderer.release]
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Suppress("AcronymName")
class GLWetDryRenderer @JvmOverloads constructor(
    surfaceView: SurfaceView,
    callback: Callback,
    @Suppress("ListenerLast")
    glRenderer: GLRenderer? = null,
) {
    /**
     * [ParentRenderLayer] used to contain both the dry and wet layers
     */
    private val mParentRenderLayer: ParentRenderLayer = SurfaceViewRenderLayer(surfaceView)

    /**
     * Callbacks invoked to render into the wet and dry layers in addition to providing consumers
     * an opportunity to specify any potential additional interactions that must be synchronized
     * with the [SurfaceControl.Transaction] to show/hide visibility of the wet layer as well as
     * updating dry layers
     */
    private val mCallback = callback

    /**
     * [GLRenderer.EglContextCallback]s used to release the corresponding [RenderBufferPool]
     * if the [GLRenderer] is torn down.
     * This is especially helpful if a [GLRenderer] is being provided and shared across other
     * [GLRenderer.RenderTarget] instances in which case it can be released externally from
     * [GLWetDryRenderer]
     */
    private val mContextCallbacks = object : GLRenderer.EglContextCallback {
        override fun onEglContextCreated(eglManager: EglManager) {
            // no-op
        }

        override fun onEglContextDestroyed(eglManager: EglManager) {
            mBufferPool?.let { releaseBuffers(it) }
        }
    }

    /**
     * [ParentRenderLayer] callbacks used to be alerted of changes to the size of the parent
     * render layer which acts as a signal to teardown some internal resources and recreate them
     * with the updated dimensions
     */
    private val mParentLayerCallback = object :
        ParentRenderLayer.Callback {
        override fun onSizeChanged(width: Int, height: Int) {
            update(width, height)
        }

        override fun onLayerDestroyed() {
            release(true)
        }

        override fun pollWetLayerParams(): Any? =
            mWetBufferParamQueue.poll()

        override fun obtainDryLayerParams(): MutableCollection<Any?> =
            mParentBufferParamQueue

        override fun getWetLayerSurfaceControl(): SurfaceControl? =
            mWetLayerSurfaceControl

        override fun getRenderBufferPool(): RenderBufferPool? =
            mBufferPool
    }

    /**
     * Queue of parameters to be consumed in [Callback.onDrawWetLayer] with the parameter
     * provided in [renderWetLayer]
     */
    private val mWetBufferParamQueue = ConcurrentLinkedQueue<Any?>()

    /**
     * Collection of parameters to be consumed in [Callback.onDrawDryLayer] with the parameters
     * defined in consecutive calls to [renderWetLayer]. Once the corresponding
     * [Callback.onDrawDryLayer] callback is invoked, this collection is cleared and new parameters
     * are added to it with consecutive calls to [renderWetLayer]
     */
    private val mParentBufferParamQueue = Collections.synchronizedList(ArrayList<Any?>())

    /**
     * [RenderBuffer] used for rendering into the wet layer. This buffer is persisted across
     * frames as part of front buffered rendering and is not expected to be released again
     * after the corresponding [SurfaceControl.Transaction] that submits this buffer is applied
     * as per the implementation of "scan line racing" that is done for front buffered rendering
     */
    private var mWetLayerBuffer: RenderBuffer? = null

    /**
     * [RenderBufferPool] used to cycle through [RenderBuffer] instances that are released when
     * the [HardwareBuffer] within the [RenderBuffer] is already displayed by the hardware
     * compositor
     */
    private var mBufferPool: RenderBufferPool? = null

    /**
     * [GLRenderer.RenderCallback] used for drawing into the wet layer
     */
    private var mWetLayerRenderer: HardwareBufferRenderer? = null

    /**
     * [SurfaceControl] used to configure buffers and visibility of the wet layer
     */
    private var mWetLayerSurfaceControl: SurfaceControl? = null

    /**
     * Width of the layers to render. Only if the size changes to we re-initialize the internal
     * state of the [GLWetDryRenderer]
     */
    private var mWidth = -1

    /**
     * Height of the layers to render. Only if the size changes do we re-initialize the internal
     * state of the [GLWetDryRenderer]
     */
    private var mHeight = -1

    /**
     * [GLRenderer] used to issue requests to render into wet/dry layers
     */
    private val mGLRenderer: GLRenderer

    /**
     * Flag indicating if the [GLRenderer] being used was created internally within
     * [GLWetDryRenderer] as opposed to being provided by the consumer. If the former, then the
     * [GLWetDryRenderer] is responsible for stopping/releasing this [GLRenderer] in the [release]
     * method. If this is being provided, then we should not be releasing this [GLRenderer] as it
     * maybe used by other consumers. In this case, only the wet/dry [GLRenderer.RenderTarget]s
     * are detached.
     */
    private val mIsManagingGLRenderer: Boolean

    /**
     * [GLRenderer.RenderTarget] used to issue requests to render into the wet layer
     */
    private var mWetLayerRenderTarget: GLRenderer.RenderTarget? = null

    /**
     * [GLRenderer.RenderTarget] used to issue requests to render into the dry layer
     */
    private var mDryLayerRenderTarget: GLRenderer.RenderTarget? = null

    /**
     * Flag to determine if the [GLWetDryRenderer] has previously been released. If this flag is
     * true, then subsequent requests to [renderWetLayer], [dry], and [release] are ignored.
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
        renderer.registerEglContextCallback(mContextCallbacks)

        mDryLayerRenderTarget = mParentRenderLayer.createRenderTarget(renderer, mCallback)
        mGLRenderer = renderer
    }

    internal fun update(width: Int, height: Int) {
        if (mWidth != width || mHeight != height) {
            mWetLayerSurfaceControl?.release()

            val wetLayerSurfaceControl = SurfaceControl.Builder()
                .setBufferSize(width, height)
                .setOpaque(false)
                .setName("WetLayerSurfaceControl")
                .setFormat(PixelFormat.RGBA_8888)
                .build()

            val bufferPool = RenderBufferPool(
                width,
                height,
                format = HardwareBuffer.RGBA_8888,
                usage = obtainHardwareBufferUsageFlags(),
                maxPoolSize = 5
            )

            val previousBufferPool = mBufferPool
            mWetLayerRenderTarget?.detach(true) {
                if (previousBufferPool != null) {
                    releaseBuffers(previousBufferPool)
                }
            }

            val wetLayerRenderer = createWetLayerRenderer(wetLayerSurfaceControl)
            mWetLayerRenderTarget = mGLRenderer.createRenderTarget(
                width,
                height,
                wetLayerRenderer
            )

            mWetLayerRenderer = wetLayerRenderer
            mWetLayerSurfaceControl = wetLayerSurfaceControl
            mBufferPool = bufferPool
            mWidth = width
            mHeight = height
        }
    }

    /**
     * Determines whether or not the [GLWetDryRenderer] is in a valid state. That is the [release]
     * method has not been called.
     * If this returns false, then subsequent calls to [renderWetLayer], [dry], and [release]
     * are ignored
     *
     * @return `true` if this [GLWetDryRenderer] has been released, `false` otherwise
     */
    fun isValid(): Boolean = !mIsReleased

    /**
     * Render content to the wet layer providing optional parameters to be consumed in
     * [Callback.onDrawWetLayer].
     * Additionally the parameter provided here will also be consumed in [Callback.onDrawDryLayer]
     * when the corresponding [dry] method is invoked, which will include all [param]s in each call
     * made to this method up to the corresponding [dry] call.
     *
     * If this [GLWetDryRenderer] has been released, that is [isValid] returns `false`, this call
     * is ignored.
     *
     * @param param Optional parameter to be consumed when rendering content into the dry layer
     */
    fun renderWetLayer(param: Any?) {
        if (isValid()) {
            mWetBufferParamQueue.add(param)
            mParentBufferParamQueue.add(param)
            mWetLayerRenderTarget?.requestRender()
        } else {
            Log.w(TAG, "Attempt to render to wet layer when GLWetDryRenderer has " +
                "been released")
        }
    }

    /**
     * Clears the contents of both the wet and dry layers. This triggers a call to
     * [Callback.onDryLayerRenderComplete] and hides the wet layer.
     */
    fun clear() {
        clearParamQueues()
        mWetLayerRenderer?.clear()
        mParentRenderLayer.clear()
    }

    /**
     * Requests to render the entire scene to the dry layer and schedules a call to
     * [Callback.onDrawDryLayer]. The parameters provided to [Callback.onDrawDryLayer] will
     * include each argument provided to every [renderWetLayer] call since the last call to [dry]
     * has been made.
     *
     * If this [GLWetDryRenderer] has been released, that is [isValid] returns `false`, this call
     * is ignored.
     */
    fun dry() {
        if (isValid()) {
            mDryLayerRenderTarget?.requestRender()
            mWetLayerRenderer?.clear()
        } else {
            Log.w(TAG, "Attempt to render to dry layer when GLWetDryRenderer has " +
                "been released")
        }
    }

    /**
     * Releases the [GLWetDryRenderer] and provides an optional callback that is invoked when
     * the [GLWetDryRenderer] is fully torn down. If the [cancelPending] flag is true, all
     * pending requests to render into the wet or dry layers will be processed before the
     * [GLWetDryRenderer] is torn down. Otherwise all in process requests are ignored.
     * If the [GLWetDryRenderer] is already released, that is [isValid] returns `false`, this
     * method does nothing.
     *
     * @param cancelPending Flag indicating that requests to render should be processed before
     * the [GLWetDryRenderer] is released
     * @param onReleaseComplete Optional callback invoked when the [GLWetDryRenderer] has been
     * released. This callback is invoked on the backing GLThread
     */
    @JvmOverloads
    fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)? = null) {
        if (!isValid()) {
            Log.w(TAG, "Attempt to release GLWetDryRenderer that is already released")
            return
        }
        // Wrap the callback into a separate lambda to ensure it is invoked only after
        // both the wet layer target and dry layer target renderers are detached
        var callbackCount = 0
        var expectedCount = 0
        if (mWetLayerRenderTarget?.isAttached() == true) {
            expectedCount++
        }

        if (mDryLayerRenderTarget?.isAttached() == true) {
            expectedCount++
        }
        val wetLayerSurfaceControl = mWetLayerSurfaceControl
        val wrappedCallback: (GLRenderer.RenderTarget) -> Unit = {
            callbackCount++
            if (callbackCount >= expectedCount) {
                mBufferPool?.let { releaseBuffers(it) }
                clearParamQueues()

                wetLayerSurfaceControl?.let {
                    val transaction = SurfaceControl.Transaction()
                        .reparent(it, null)
                    mParentRenderLayer.release(transaction)
                    transaction.apply()
                    it.release()
                }

                onReleaseComplete?.invoke()
            }
        }
        mWetLayerRenderTarget?.detach(cancelPending, wrappedCallback)
        mDryLayerRenderTarget?.detach(cancelPending, wrappedCallback)
        mWetLayerRenderTarget = null
        mDryLayerRenderTarget = null

        mGLRenderer.unregisterEglContextCallback(mContextCallbacks)
        if (mIsManagingGLRenderer) {
            // If we are managing the GLRenderer that we created ourselves
            // do not cancel pending operations as we will miss callbacks that we are
            // expecting above to properly teardown resources
            // Instead rely on the cancel pending flags for detaching the wet and dry
            // render targets instead
            mGLRenderer.stop(false)
        }

        mWetLayerSurfaceControl = null
        mParentRenderLayer.setParentLayerCallbacks(null)
        mIsReleased = true
    }

    private fun createWetLayerRenderer(
        wetLayerSurfaceControl: SurfaceControl
    ) = HardwareBufferRenderer(
        object : HardwareBufferRenderer.RenderCallbacks {

            @WorkerThread
            override fun obtainRenderBuffer(egl: EglSpec): RenderBuffer {
                var buffer = mWetLayerBuffer
                if (buffer == null) {
                    // Allocate and persist a RenderBuffer instance across frames
                    buffer = mBufferPool?.obtain(egl).also { mWetLayerBuffer = it }
                       ?: throw IllegalArgumentException("Unable to obtain RenderBuffer")
                }
                return buffer
            }

            @WorkerThread
            override fun onDraw(eglManager: EglManager) {
                val params = mParentLayerCallback.pollWetLayerParams()
                mCallback.onDrawWetLayer(eglManager, params)
            }

            @WorkerThread
            override fun onDrawComplete(renderBuffer: RenderBuffer) {
                val transaction = SurfaceControl.Transaction()
                    // Make this layer the top most layer
                    .setLayer(wetLayerSurfaceControl, Integer.MAX_VALUE)
                    .setBuffer(wetLayerSurfaceControl, renderBuffer.hardwareBuffer)
                    .setVisibility(wetLayerSurfaceControl, true)
                mParentRenderLayer.buildReparentTransaction(wetLayerSurfaceControl, transaction)
                mCallback.onWetLayerRenderComplete(wetLayerSurfaceControl, transaction)
                transaction.apply()
            }
        }
    )

    private fun clearParamQueues() {
        mWetBufferParamQueue.clear()
        mParentBufferParamQueue.clear()
    }

    /**
     * Release the buffers associated with the wet layer as well as the [RenderBufferPool]
     */
    internal fun releaseBuffers(pool: RenderBufferPool) {
        mWetLayerBuffer?.let {
            pool.release(it)
            mWetLayerBuffer = null
        }
        pool.close()
    }

    companion object {

        internal const val TAG = "GLWetDryRenderer"

        /**
         * Flags that are expected to be supported on all [HardwareBuffer] instances
         */
        private const val BaseFlags = HardwareBuffer.USAGE_COMPOSER_OVERLAY or
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or
            HardwareBuffer.USAGE_GPU_COLOR_OUTPUT

        internal fun supportsFrontBufferUsage(): Boolean =
        // Query to determine if the USAGE_FRONT_BUFFER flag is supported
        // Even though it is introduced in Android T, not all devices may support
        // this flag so we query a sample HardwareBuffer of 1 x 1 pixels with
        // the RGBA_8888 format along with the USAGE_FRONT_BUFFER flag to see if it is
        // compatible
            HardwareBuffer.isSupported(
                1, // width
                1, // height
                HardwareBuffer.RGBA_8888, // format
                1, // layers,
                BaseFlags or
                    HardwareBuffer.USAGE_FRONT_BUFFER
            )

        internal fun obtainHardwareBufferUsageFlags(): Long =
            BaseFlags or
                // Conditionally introduce the front buffer usage flag if it is supported
                if (supportsFrontBufferUsage()) HardwareBuffer.USAGE_FRONT_BUFFER else 0
    }

    /**
     * Provides callbacks for consumers to draw into the wet and dry layers as well as provide
     * opportunities to synchronize [SurfaceControl.Transaction]s to submit the layers to the
     * hardware compositor
     */
    interface Callback {

        /**
         * Callback invoked to render content into the wet layer with the specified parameters.
         * @param eglManager [EglManager] useful in configuring EGL objects to be used when issuing
         * OpenGL commands to render into the wet layer
         * @param param optional parameter provided the corresponding
         * [GLWetDryRenderer.renderWetLayer] method that triggered this request to render into the
         * wet layer
         */
        @WorkerThread
        fun onDrawWetLayer(eglManager: EglManager, param: Any?)

        /**
         * Callback invoked to render content into the dry layer with the specified parameters.
         * @param eglManager [EglManager] useful in configuring EGL objects to be used when issuing
         * OpenGL commands to render into the dry layer
         * @param params optional parameter provided to render the entire scene into the dry layer.
         * This is a collection of all parameters provided in consecutive invocations to
         * [GLWetDryRenderer.renderWetLayer] since the last call to [GLWetDryRenderer.dry] has
         * been made. After [GLWetDryRenderer.dry] is invoked, this collection is cleared and new
         * parameters are added on each subsequent call to [GLWetDryRenderer.renderWetLayer].
         *
         * Consider the following example:
         *
         * myWetDryRenderer.renderWetLayer(1)
         * myWetDryRenderer.renderWetLayer(2)
         * myWetDryRenderer.renderWetLayer(3)
         * myWetDryRenderer.dry()
         *
         * This will generate a callback to this method with the params collection containing values
         * [1, 2, 3]
         *
         * myWetDryRenderer.renderWetLayer(4)
         * myWetDryRenderer.renderWetLayer(5)
         * myWetDryRenderer.dry()
         *
         * This will generate a callback to this method with the params collection containing values
         * [4, 5]
         */
        @WorkerThread
        fun onDrawDryLayer(eglManager: EglManager, params: Collection<Any?>)

        /**
         * Optional callback invoked when rendering to the wet layer is complete but before
         * the wet layer buffers are submitted to the hardware compositor.
         * This provides consumers a mechanism for synchronizing the transaction with other
         * [SurfaceControl] objects that maybe rendered within the scene.
         *
         * @param wetLayerSurfaceControl Handle to the [SurfaceControl] where the wet layer
         * content is drawn. This can be used to configure various properties of the
         * [SurfaceControl] like z-ordering or visibility with the corresponding
         * [SurfaceControl.Transaction].
         * @param transaction Current [SurfaceControl.Transaction] to apply updated buffered content
         * to the wet layer.
         */
        @WorkerThread
        fun onWetLayerRenderComplete(
            wetLayerSurfaceControl: SurfaceControl,
            transaction: SurfaceControl.Transaction
        ) {
            // Default implementation is a no-op
        }

        /**
         * Optional callback invoked when rendering to the dry layer is complete but before
         * the dry layer buffers are submitted to the hardware compositor.
         * This provides consumers a mechanism for synchronizing the transaction with other
         * [SurfaceControl] objects that maybe rendered within the scene.
         *
         * @param wetLayerSurfaceControl Handle to the [SurfaceControl] where the wet layer
         * content is drawn. This can be used to configure various properties of the
         * [SurfaceControl] like z-ordering or visibility with the corresponding
         * [SurfaceControl.Transaction].
         * @param transaction Current [SurfaceControl.Transaction] to apply updated buffered content
         * to the dry or double buffered layer.
         */
        @WorkerThread
        fun onDryLayerRenderComplete(
            wetLayerSurfaceControl: SurfaceControl,
            transaction: SurfaceControl.Transaction
        ) {
            // Default implementation is a no-op
        }
    }
}