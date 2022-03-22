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

package androidx.graphics.opengl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLSurface
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.WorkerThread
import androidx.graphics.opengl.egl.EglConfigAttributes8888
import androidx.graphics.opengl.egl.EglManager
import androidx.graphics.opengl.egl.EglSpec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Class responsible for coordination of requests to render into surfaces using OpenGL.
 * This creates a backing thread to handle EGL dependencies and draw leveraging OpenGL across
 * multiple [android.view.Surface] instances that can be attached and detached throughout
 * the lifecycle of an application. Usage of this class is recommended to be done on the UI thread.
 *
 * @param eglSpecFactory Callback invoked to determine the EGL spec version to use
 * for EGL management. This is invoked on the GL Thread
 * @param eglConfigFactory Callback invoked to determine the appropriate EGLConfig used
 * to create the EGL context. This is invoked on the GL Thread
 */
// GL is the industry standard for referencing OpenGL vs Gl (lowercase l)
@Suppress("AcronymName")
class GLRenderer(
    eglSpecFactory: () -> EglSpec = { EglSpec.Egl14 },
    eglConfigFactory: EglManager.() -> EGLConfig = {
        // 8 bit channels should always be supported
        loadConfig(EglConfigAttributes8888)
            ?: throw IllegalStateException("Unable to obtain config for 8 bit EGL " +
                "configuration")
    }
) {

    /**
     * Factory method to determine which EglSpec the underlying EglManager implementation uses
     */
    private val mEglSpecFactory: () -> EglSpec = eglSpecFactory

    /**
     * Factory method used to create the corresponding EGLConfig used to create the EGLRenderer used
     * by EglManager
     */
    private val mEglConfigFactory: EglManager.() -> EGLConfig = eglConfigFactory

    /**
     * GLThread used to manage EGL dependencies, create EGLSurfaces and draw content
     */
    private var mGLThread: GLThread? = null

    /**
     * Collection of [RenderTarget] instances that are managed by the GLRenderer
     */
    private val mRenderTargets = ArrayList<RenderTarget>()

    /**
     * Collection of callbacks to be invoked when the EGL dependencies are initialized
     * or torn down
     */
    private val mEglContextCallback = HashSet<EglContextCallback>()

    /**
     * Removes the corresponding [RenderTarget] from management of the GLThread.
     * This destroys the EGLSurface associated with this surface and subsequent requests
     * to render into the surface with the provided token are ignored.
     *
     * If the [cancelPending] flag is set to true, any queued request
     * to render that has not started yet is cancelled. However, if this is invoked in the
     * middle of the frame being rendered, it will continue to process the current frame.
     *
     * Additionally if this flag is false, all pending requests to render will be processed
     * before the [RenderTarget] is detached.
     *
     * Note the detach operation will only occur if the GLRenderer is started, that is if
     * [isRunning] returns true. Otherwise this is a no-op. GLRenderer will automatically detach all
     * [RenderTarget] instances as part of its teardown process.
     */
    @JvmOverloads
    fun detach(
        target: RenderTarget,
        cancelPending: Boolean,
        @WorkerThread onDetachComplete: ((RenderTarget) -> Unit)? = null
    ) {
        if (mRenderTargets.contains(target)) {
            mGLThread?.detachSurface(target.token, cancelPending) {
                // WorkerThread
                target.release()
                target.onDetach.invoke()
                onDetachComplete?.invoke(target)
            }
            mRenderTargets.remove(target)
        }
    }

    /**
     * Determines if the GLThread has been started. That is [start] has been invoked
     * on this GLRenderer instance without a corresponding call to [stop].
     */
    fun isRunning(): Boolean = mGLThread != null

    /**
     * Starts the GLThread. After this method is called, consumers can attempt
     * to attach [android.view.Surface] instances through [attach] as well as
     * schedule content to be drawn through [requestRender]
     *
     * @param name Optional name to provide to the GLThread
     *
     * @throws IllegalStateException if EGLConfig with desired attributes cannot be created
     */
    @JvmOverloads
    fun start(
        name: String = "GLThread",
    ) {
        if (mGLThread == null) {
            GLThread.log("starting thread...")
            mGLThread = GLThread(
                name,
                mEglSpecFactory,
                mEglConfigFactory
            ).apply {
                start()
                if (!mEglContextCallback.isEmpty()) {
                    // Add a copy of the current collection as new entries to mEglContextCallback
                    // could be mistakenly added multiple times.
                    this.addEglCallbacks(ArrayList<EglContextCallback>(mEglContextCallback))
                }
            }
        }
    }

    /**
     * Mark the corresponding surface session with the given token as dirty
     * to schedule a call to [RenderCallback#onDrawFrame].
     * If there is already a queued request to render into the provided surface with
     * the specified token, this request is ignored.
     *
     * Note the render operation will only occur if the GLRenderer is started, that is if
     * [isRunning] returns true. Otherwise this is a no-op.
     *
     * @param target RenderTarget to be re-rendered
     * @param onRenderComplete Optional callback invoked on the backing thread after the frame has
     * been rendered.
     */
    @JvmOverloads
    fun requestRender(target: RenderTarget, onRenderComplete: ((RenderTarget) -> Unit)? = null) {
        val token = target.token
        val callbackRunnable = if (onRenderComplete != null) {
            Runnable {
                onRenderComplete.invoke(target)
            }
        } else {
            null
        }
        mGLThread?.requestRender(token, callbackRunnable)
    }

    /**
     * Resize the corresponding surface associated with the RenderTarget to the specified
     * width and height and re-render. This will destroy the EGLSurface created by
     * [RenderCallback.onSurfaceCreated] and invoke it again with the updated dimensions.
     * An optional callback is invoked on the backing thread after the resize operation
     * is complete.
     *
     * Note the resize operation will only occur if the GLRenderer is started, that is if
     * [isRunning] returns true. Otherwise this is a no-op.
     *
     * @param target RenderTarget to be resized
     * @param width Updated width of the corresponding surface
     * @param height Updated height of the corresponding surface
     * @param onResizeComplete Optional callback invoked on the backing thread when the resize
     * operation is complete
     */
    @JvmOverloads
    fun resize(
        target: RenderTarget,
        width: Int,
        height: Int,
        onResizeComplete: ((RenderTarget) -> Unit)? = null
    ) {
        val token = target.token
        val callbackRunnable = if (onResizeComplete != null) {
            Runnable {
                onResizeComplete.invoke(target)
            }
        } else {
            null
        }
        mGLThread?.resizeSurface(token, width, height, callbackRunnable)
    }

    /**
     * Stop the corresponding GL thread. This destroys all EGLSurfaces as well
     * as any other EGL dependencies. All queued requests that have not been processed
     * yet are cancelled.
     *
     * Note the stop operation will only occur if the GLRenderer was previously started, that is
     * [isRunning] returns true. Otherwise this is a no-op.
     *
     * @param cancelPending If true all pending requests and cancelled and the backing thread is
     * torn down immediately. If false, all pending requests are processed first before tearing
     * down the backing thread. Subsequent requests made after this call are ignored.
     * @param onStop Optional callback invoked on the backing thread after it is torn down.
     */
    @JvmOverloads
    fun stop(cancelPending: Boolean, onStop: ((GLRenderer) -> Unit)? = null) {
        GLThread.log("stopping thread...")
        // Make a copy of the render targets to call cleanup operations on to avoid potential
        // concurrency issues.
        // This method will clear the existing collection and we do not want to potentially tear
        // down a target that was attached after a subsequent call to start if the tear down
        // callback execution is delayed if previously pending requests have not been cancelled
        // (i.e. cancelPending is false)
        val renderTargets = ArrayList(mRenderTargets)
        mGLThread?.tearDown(cancelPending) {
            // No need to call target.detach as this callback is invoked after
            // the dependencies are cleaned up
            for (target in renderTargets) {
                target.release()
                target.onDetach.invoke()
            }
            onStop?.invoke(this@GLRenderer)
        }
        mGLThread = null
        mRenderTargets.clear()
    }

    /**
     * Add an [EglContextCallback] to receive callbacks for construction and
     * destruction of EGL dependencies.
     *
     * These callbacks are invoked on the backing thread.
     */
    fun registerEglContextCallback(callback: EglContextCallback) {
        mEglContextCallback.add(callback)
        mGLThread?.addEglCallback(callback)
    }

    /**
     * Remove [EglContextCallback] to no longer receive callbacks for construction and
     * destruction of EGL dependencies.
     *
     * These callbacks are invoked on the backing thread
     */
    fun unregisterEglContextCallback(callback: EglContextCallback) {
        mEglContextCallback.remove(callback)
        mGLThread?.removeEglCallback(callback)
    }

    /**
     * Callbacks invoked when the GL dependencies are created and destroyed.
     * These are logical places to setup and tear down any dependencies that are used
     * for drawing content within a frame (ex. compiling shaders)
     */
    interface EglContextCallback {

        /**
         * Callback invoked on the backing thread after EGL dependencies are initialized.
         * This is guaranteed to be invoked before any instance of
         * [RenderCallback.onSurfaceCreated] is called.
         * This will be invoked lazily before the first request to [GLRenderer.requestRender]
         */
        @WorkerThread
        fun onEglContextCreated(eglManager: EglManager)

        /**
         * Callback invoked on the backing thread before EGL dependencies are about to be torn down.
         * This is invoked after [GLRenderer.stop] is processed.
         */
        @WorkerThread
        fun onEglContextDestroyed(eglManager: EglManager)
    }

    /**
     * Interface used for creating an [EGLSurface] with a user defined configuration
     * from the provided surface as well as a callback used to render content into the surface
     * for a given frame
     */
    interface RenderCallback {
        /**
         * Used to create a corresponding [EGLSurface] from the provided
         * [android.view.Surface] instance. This enables consumers to configure
         * the corresponding [EGLSurface] they wish to render into.
         * The [EGLSurface] created here is guaranteed to be the current surface
         * before [onDrawFrame] is called. That is, implementations of onDrawFrame
         * do not need to call eglMakeCurrent on this [EGLSurface].
         *
         * This method is invoked on the GL thread.
         *
         * The default implementation will create a window surface with EGL_WIDTH and EGL_HEIGHT
         * set to [width] and [height] respectively.
         * Implementations can override this method to provide additional EglConfigAttributes
         * for this surface (ex. [EGL14.EGL_SINGLE_BUFFER]
         *
         * @param spec EGLSpec used to create the corresponding EGLSurface
         * @param config EGLConfig used to create the corresponding EGLSurface
         * @param surface [android.view.Surface] used to create an EGLSurface from
         * @param width Desired width of the surface to create
         * @param height Desired height of the surface to create
         */
        @WorkerThread
        fun onSurfaceCreated(
            spec: EglSpec,
            config: EGLConfig,
            surface: Surface,
            width: Int,
            height: Int
        ): EGLSurface =
            // Always default to creating an EGL window surface
            // Despite having access to the width and height here, do not explicitly
            // pass in EGLConfigAttributes specifying the EGL_WIDTH and EGL_HEIGHT parameters
            // as those are not accepted parameters for eglCreateWindowSurface but they are
            // for other EGL Surface factory methods such as eglCreatePBufferSurface
            // See accepted parameters here:
            // https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglCreateWindowSurface.xhtml
            // and here
            // https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglCreatePbufferSurface.xhtml
            spec.eglCreateWindowSurface(config, surface, null)

        /**
         * Callback used to issue OpenGL drawing commands into the [EGLSurface]
         * created in [onSurfaceCreated]. This [EGLSurface] is guaranteed to
         * be current before this callback is invoked and [EglManager.swapAndFlushBuffers]
         * will be invoked afterwards. If additional scratch [EGLSurface]s are used
         * here it is up to the implementation of this method to ensure that the proper
         * surfaces are made current and the appropriate swap buffers call is made
         *
         * This method is invoked on the backing thread
         *
         * @param eglManager Handle to EGL dependencies
         */
        @WorkerThread
        fun onDrawFrame(eglManager: EglManager)
    }

    /**
     * Adds the [android.view.Surface] to be managed by the GLThread.
     * A corresponding [EGLSurface] is created on the GLThread as well as a callback
     * for rendering into the surface through [RenderCallback].
     * Unlike the other [attach] methods that consume a [SurfaceView] or [TextureView],
     * this method does not handle any lifecycle callbacks associated with the target surface.
     * Therefore it is up to the consumer to properly setup/teardown resources associated with
     * this surface.
     *
     * @param surface Target surface to be managed by the backing thread
     * @param width Desired width of the [surface]
     * @param height Desired height of the [surface]
     * @param renderer Callbacks used to create a corresponding [EGLSurface] from the
     * given surface as well as render content into the created [EGLSurface]
     * @return [RenderTarget] used for subsequent requests to communicate
     * with the provided Surface (ex. [requestRender] or [detach]).
     *
     * @throws IllegalStateException If this method was called when the GLThread has not started
     * (i.e. start has not been called)
     */
    fun attach(surface: Surface, width: Int, height: Int, renderer: RenderCallback): RenderTarget {
        val thread = mGLThread
        if (thread != null) {
            val token = sToken.getAndIncrement()
            thread.attachSurface(token, surface, width, height, renderer)
            return RenderTarget(token, this).also { mRenderTargets.add(it) }
        } else {
            throw IllegalStateException("GLThread not started, did you forget to call start?")
        }
    }

    /**
     * Adds the [android.view.Surface] provided by the given [SurfaceView] to be managed by the
     * backing thread.
     *
     * A corresponding [EGLSurface] is created on the GLThread as well as a callback
     * for rendering into the surface through [RenderCallback].
     *
     * This method automatically configures a [SurfaceHolder.Callback] used to attach the
     * [android.view.Surface] when the underlying [SurfaceHolder] that contains the surface is
     * available. Similarly this surface will be detached from [GLRenderer] when the surface provided
     * by the [SurfaceView] is destroyed (i.e. [SurfaceHolder.Callback.surfaceDestroyed] is called.
     *
     * If the [android.view.Surface] is already available by the time this method is invoked,
     * it is attached synchronously.
     *
     * @param surfaceView SurfaceView that provides the surface to be rendered by the backing thread
     * @param renderer callbacks used to create a corresponding [EGLSurface] from the
     * given surface as well as render content into the created [EGLSurface]
     * @return [RenderTarget] used for subsequent requests to communicate
     * with the provided Surface (ex. [requestRender] or [detach]).
     *
     * @throws IllegalStateException If this method was called when the GLThread has not started
     * (i.e. start has not been called)
     */
    fun attach(surfaceView: SurfaceView, renderer: RenderCallback): RenderTarget {
        val thread = mGLThread
        if (thread != null) {
            val token = sToken.getAndIncrement()
            val holder = surfaceView.holder
            val callback = object : SurfaceHolder.Callback2 {

                var isAttached = false

                /**
                 * Optional condition that maybe used if we are issuing a blocking call to render
                 * in [SurfaceHolder.Callback2.surfaceRedrawNeeded]
                 * In this case we need to signal the condition of either the request to render
                 * has completed, or if the RenderTarget has been detached and the pending
                 * render request is cancelled.
                 */
                @Volatile var renderLatch: CountDownLatch? = null

                val renderTarget = RenderTarget(token, this@GLRenderer) @WorkerThread {
                    isAttached = false
                    // SurfaceHolder.add/remove callback is thread safe
                    holder.removeCallback(this)
                    // Countdown in case we have been detached while waiting for a render
                    // to be completed
                    renderLatch?.countDown()
                }

                override fun surfaceRedrawNeeded(p0: SurfaceHolder) {
                    val latch = CountDownLatch(1).also { renderLatch = it }
                    // Request a render and block until the rendering is complete
                    // surfaceRedrawNeeded is invoked on older API levels and is replaced with
                    // surfaceRedrawNeededAsync for newer API levels which is non-blocking
                    renderTarget.requestRender @WorkerThread {
                        latch.countDown()
                    }
                    latch.await()
                    renderLatch = null
                }

                override fun surfaceRedrawNeededAsync(
                    holder: SurfaceHolder,
                    drawingFinished: Runnable
                ) {
                    renderTarget.requestRender {
                        drawingFinished.run()
                    }
                }

                override fun surfaceCreated(holder: SurfaceHolder) {
                    // NO-OP wait until surfaceChanged which is guaranteed to be called and also
                    // provides the appropriate width height of the surface
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    if (!isAttached) {
                        thread.attachSurface(token, holder.surface, width, height, renderer)
                        isAttached = true
                    } else {
                        renderTarget.resize(width, height)
                    }
                    renderTarget.requestRender()
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    val detachLatch = CountDownLatch(1)
                    renderTarget.detach(true) {
                        detachLatch.countDown()
                    }
                    detachLatch.await()
                }
            }
            holder.addCallback(callback)
            if (holder.surface != null && holder.surface.isValid) {
                thread.attachSurface(
                    token,
                    holder.surface,
                    surfaceView.width,
                    surfaceView.height,
                    renderer
                )
            }
            mRenderTargets.add(callback.renderTarget)
            return callback.renderTarget
        } else {
            throw IllegalStateException("GLThread not started, did you forget to call start?")
        }
    }

    /**
     * Adds the [android.view.Surface] provided by the given [TextureView] to be managed by the
     * backing thread.
     *
     * A corresponding [EGLSurface] is created on the GLThread as well as a callback
     * for rendering into the surface through [RenderCallback].
     *
     * This method automatically configures a [TextureView.SurfaceTextureListener] used to create a
     * [android.view.Surface] when the underlying [SurfaceTexture] is available.
     * Similarly this surface will be detached from [GLRenderer] if the underlying [SurfaceTexture]
     * is destroyed (i.e. [TextureView.SurfaceTextureListener.onSurfaceTextureDestroyed] is called.
     *
     * If the [SurfaceTexture] is already available by the time this method is called, then it is
     * attached synchronously.
     *
     * @param textureView TextureView that provides the surface to be rendered into on the GLThread
     * @param renderer callbacks used to create a corresponding [EGLSurface] from the
     * given surface as well as render content into the created [EGLSurface]
     * @return [RenderTarget] used for subsequent requests to communicate
     * with the provided Surface (ex. [requestRender] or [detach]).
     *
     * @throws IllegalStateException If this method was called when the GLThread has not started
     * (i.e. start has not been called)
     */
    fun attach(textureView: TextureView, renderer: RenderCallback): RenderTarget {
        val thread = mGLThread
        if (thread != null) {
            val token = sToken.getAndIncrement()
            val renderTarget = RenderTarget(token, this) @WorkerThread {
                textureView.handler?.post {
                    textureView.surfaceTextureListener = null
                }
            }
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surfaceTexture: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    thread.attachSurface(token, Surface(surfaceTexture), width, height, renderer)
                }

                override fun onSurfaceTextureSizeChanged(
                    texture: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    renderTarget.resize(width, height)
                    renderTarget.requestRender()
                }

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    val detachLatch = CountDownLatch(1)
                    renderTarget.detach(true) {
                        detachLatch.countDown()
                    }
                    detachLatch.await()
                    return true
                }

                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                    // NO-OP
                }
            }
            if (textureView.isAvailable) {
                thread.attachSurface(
                    token,
                    Surface(textureView.surfaceTexture),
                    textureView.width,
                    textureView.height,
                    renderer
                )
            }
            mRenderTargets.add(renderTarget)
            return renderTarget
        } else {
            throw IllegalStateException("GLThread not started, did you forget to call start?")
        }
    }

    /**
     * Handle to a [android.view.Surface] that is given to [GLRenderer] to handle
     * rendering.
     */
    class RenderTarget internal constructor(
        internal val token: Int,
        glManager: GLRenderer,
        @WorkerThread internal val onDetach: () -> Unit = {}
    ) {

        @Volatile
        private var mManager: GLRenderer? = glManager

        internal fun release() {
            mManager = null
        }

        /**
         * Request that this [RenderTarget] should have its contents redrawn.
         * This consumes an optional callback that is invoked on the backing thread when
         * the rendering is completed.
         *
         * Note the render operation will only occur if the RenderTarget is attached, that is
         * [isAttached] returns true. If the [RenderTarget] is detached or the [GLRenderer] that
         * created this RenderTarget is stopped, this is a no-op.
         *
         * @param onRenderComplete Optional callback called on the backing thread when
         * rendering is finished
         */
        @JvmOverloads
        fun requestRender(@WorkerThread onRenderComplete: ((RenderTarget) -> Unit)? = null) {
            mManager?.requestRender(this@RenderTarget, onRenderComplete)
        }

        /**
         * Determines if the current RenderTarget is attached to GLRenderer.
         * This is true until [detach] has been called. If the RenderTarget is no longer
         * in an attached state (i.e. this returns false). Subsequent calls to [requestRender]
         * will be ignored.
         */
        fun isAttached(): Boolean = mManager != null

        /**
         * Resize the RenderTarget to the specified width and height.
         * This will destroy the EGLSurface created by [RenderCallback.onSurfaceCreated]
         * and invoke it again with the updated dimensions.
         * An optional callback is invoked on the backing thread after the resize operation
         * is complete
         *
         * Note the resize operation will only occur if the RenderTarget is attached, that is
         * [isAttached] returns true. If the [RenderTarget] is detached or the [GLRenderer] that
         * created this RenderTarget is stopped, this is a no-op.
         *
         * @param width New target width to resize the RenderTarget
         * @param height New target height to resize the RenderTarget
         * @param onResizeComplete Optional callback invoked after the resize is complete
         */
        @JvmOverloads
        fun resize(
            width: Int,
            height: Int,
            @WorkerThread onResizeComplete: ((RenderTarget) -> Unit)? = null
        ) {
            mManager?.resize(this, width, height, onResizeComplete)
        }

        /**
         * Removes the corresponding [RenderTarget] from management of the GLThread.
         * This destroys the EGLSurface associated with this surface and subsequent requests
         * to render into the surface with the provided token are ignored.
         *
         * If the [cancelPending] flag is set to true, any queued request
         * to render that has not started yet is cancelled. However, if this is invoked in the
         * middle of the frame being rendered, it will continue to process the current frame.
         *
         * Additionally if this flag is false, all pending requests to render will be processed
         * before the [RenderTarget] is detached.
         *
         * This is a convenience method around [GLRenderer.detach]
         *
         * Note the detach operation will only occur if the RenderTarget is attached, that is
         * [isAttached] returns true. If the [RenderTarget] is detached or the [GLRenderer] that
         * created this RenderTarget is stopped, this is a no-op.
         */
        @JvmOverloads
        fun detach(cancelPending: Boolean, onDetachComplete: ((RenderTarget) -> Unit)? = null) {
            mManager?.detach(this, cancelPending, onDetachComplete)
        }
    }

    companion object {
        /**
         * Counter used to issue unique identifiers for surfaces that are managed by GLRenderer
         */
        private val sToken = AtomicInteger()
    }
}
