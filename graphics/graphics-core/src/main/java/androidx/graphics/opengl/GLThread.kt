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

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.graphics.opengl.GLRenderer.EGLContextCallback
import androidx.graphics.opengl.GLRenderer.RenderCallback
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.utils.post
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thread responsible for management of EGL dependencies, setup and teardown
 * of EGLSurface instances as well as delivering callbacks to draw a frame
 */
internal class GLThread(
    name: String = "GLThread",
    private val mEglSpecFactory: () -> EGLSpec,
    private val mEglConfigFactory: EGLManager.() -> EGLConfig,
) : HandlerThread(name) {

    // Accessed on internal HandlerThread
    private val mIsTearingDown = AtomicBoolean(false)
    private var mEglManager: EGLManager? = null
    private val mSurfaceSessions = HashMap<Int, SurfaceSession>()
    private var mHandler: Handler? = null
    private val mEglContextCallback = HashSet<EGLContextCallback>()

    override fun start() {
        super.start()
        mHandler = Handler(looper)
    }

    /**
     * Adds the given [android.view.Surface] to be managed by the GLThread.
     * A corresponding [EGLSurface] is created on the GLThread as well as a callback
     * for rendering into the surface through [RenderCallback].
     * @param surface intended to be be rendered into on the GLThread
     * @param width Desired width of the [surface]
     * @param height Desired height of the [surface]
     * @param renderer callbacks used to create a corresponding [EGLSurface] from the
     * given surface as well as render content into the created [EGLSurface]
     * @return Identifier used for subsequent requests to communicate
     * with the provided Surface (ex. [requestRender] or [detachSurface]
     */
    @AnyThread
    fun attachSurface(
        token: Int,
        surface: Surface?,
        width: Int,
        height: Int,
        renderer: RenderCallback
    ) {
        withHandler {
            post(token) {
                attachSurfaceSessionInternal(
                    SurfaceSession(token, surface, renderer).apply {
                        this.width = width
                        this.height = height
                    }
                )
            }
        }
    }

    @AnyThread
    fun resizeSurface(token: Int, width: Int, height: Int, callback: Runnable? = null) {
        withHandler {
            post(token) {
                resizeSurfaceSessionInternal(token, width, height)
                requestRenderInternal(token)
                callback?.run()
            }
        }
    }

    @AnyThread
    fun addEGLCallbacks(callbacks: ArrayList<EGLContextCallback>) {
        withHandler {
            post {
                mEglContextCallback.addAll(callbacks)
                mEglManager?.let {
                    for (callback in callbacks) {
                        callback.onEGLContextCreated(it)
                    }
                }
            }
        }
    }

    @AnyThread
    fun addEGLCallback(callbacks: EGLContextCallback) {
        withHandler {
            post {
                mEglContextCallback.add(callbacks)
                // If EGL dependencies are already initialized, immediately invoke
                // the added callback
                mEglManager?.let {
                    callbacks.onEGLContextCreated(it)
                }
            }
        }
    }

    @AnyThread
    fun removeEGLCallback(callbacks: EGLContextCallback) {
        withHandler {
            post {
                mEglContextCallback.remove(callbacks)
            }
        }
    }

    @AnyThread
    fun execute(runnable: Runnable) = withHandler { post(runnable) }

    /**
     * Removes the corresponding [android.view.Surface] from management of the GLThread.
     * This destroys the EGLSurface associated with this surface and subsequent requests
     * to render into the surface with the provided token are ignored. Any queued request
     * to render to the corresponding [SurfaceSession] that has not started yet is cancelled.
     * However, if this is invoked in the middle of the frame being rendered, it will continue to
     * process the current frame.
     */
    @AnyThread
    fun detachSurface(
        token: Int,
        cancelPending: Boolean,
        callback: Runnable?
    ) {
        log("dispatching request to detach surface w/ token: $token")
        withHandler {
            if (cancelPending) {
                removeCallbacksAndMessages(token)
            }
            post(token) {
                detachSurfaceSessionInternal(token, callback)
            }
        }
    }

    /**
     * Cancel all pending requests to all currently managed [SurfaceSession] instances,
     * destroy all EGLSurfaces, teardown EGLManager and quit this thread
     */
    @AnyThread
    fun tearDown(cancelPending: Boolean, callback: Runnable?) {
        withHandler {
            if (cancelPending) {
                removeCallbacksAndMessages(null)
            }
            post {
                releaseResourcesInternalAndQuit(callback)
            }
            mIsTearingDown.set(true)
        }
    }

    /**
     * Mark the corresponding surface session with the given token as dirty
     * to schedule a call to [RenderCallback#onDrawFrame].
     * If there is already a queued request to render into the provided surface with
     * the specified token, this request is ignored.
     */
    @AnyThread
    fun requestRender(token: Int, callback: Runnable? = null) {
        log("dispatching request to render for token: $token")
        withHandler {
            post(token) {
                requestRenderInternal(token)
                callback?.run()
            }
        }
    }

    /**
     * Lazily creates an [EGLManager] instance from the given [mEglSpecFactory]
     * used to determine the configuration. This result is cached across calls
     * unless [tearDown] has been called.
     */
    @WorkerThread
    fun obtainEGLManager(): EGLManager =
        mEglManager ?: EGLManager(mEglSpecFactory.invoke()).also {
            it.initialize()
            val config = mEglConfigFactory.invoke(it)
            it.createContext(config)
            for (callback in mEglContextCallback) {
                callback.onEGLContextCreated(it)
            }
            mEglManager = it
        }

    @WorkerThread
    private fun disposeSurfaceSession(session: SurfaceSession) {
        val eglSurface = session.eglSurface
        if (eglSurface != null && eglSurface != EGL14.EGL_NO_SURFACE) {
            obtainEGLManager().eglSpec.eglDestroySurface(eglSurface)
            session.eglSurface = null
        }
    }

    /**
     * Helper method to obtain the cached EGLSurface for the given [SurfaceSession],
     * creating one if it does not previously exist
     */
    @WorkerThread
    private fun obtainEGLSurfaceForSession(session: SurfaceSession): EGLSurface? {
        return if (session.eglSurface != null) {
            session.eglSurface
        } else {
            createEGLSurfaceForSession(
                session.surface,
                session.width,
                session.height,
                session.surfaceRenderer
            ).also {
                session.eglSurface = it
            }
        }
    }

    /**
     * Helper method to create the corresponding EGLSurface from the [SurfaceSession] instance
     * Consumers are expected to teardown the previously existing EGLSurface instance if it exists
     */
    @WorkerThread
    private fun createEGLSurfaceForSession(
        surface: Surface?,
        width: Int,
        height: Int,
        surfaceRenderer: RenderCallback
    ): EGLSurface? {
        with(obtainEGLManager()) {
            return if (surface != null) {
                surfaceRenderer.onSurfaceCreated(
                    eglSpec,
                    // Successful creation of EGLManager ensures non null EGLConfig
                    eglConfig!!,
                    surface,
                    width,
                    height
                )
            } else {
                null
            }
        }
    }

    @WorkerThread
    private fun releaseResourcesInternalAndQuit(callback: Runnable?) {
        val eglManager = obtainEGLManager()
        for (session in mSurfaceSessions) {
            disposeSurfaceSession(session.value)
        }
        callback?.run()
        mSurfaceSessions.clear()
        for (eglCallback in mEglContextCallback) {
            eglCallback.onEGLContextDestroyed(eglManager)
        }
        mEglContextCallback.clear()
        eglManager.release()
        mEglManager = null
        quit()
    }

    @WorkerThread
    private fun requestRenderInternal(token: Int) {
        log("requesting render for token: $token")
        mSurfaceSessions[token]?.let { surfaceSession ->
            val eglManager = obtainEGLManager()
            val eglSurface = obtainEGLSurfaceForSession(surfaceSession)
            if (eglSurface != null) {
                eglManager.makeCurrent(eglSurface)
            } else {
                eglManager.makeCurrent(eglManager.defaultSurface)
            }

            val width = surfaceSession.width
            val height = surfaceSession.height
            if (width > 0 && height > 0) {
                surfaceSession.surfaceRenderer.onDrawFrame(eglManager)
            }

            if (eglSurface != null) {
                eglManager.swapAndFlushBuffers()
            }
        }
    }

    @WorkerThread
    private fun attachSurfaceSessionInternal(surfaceSession: SurfaceSession) {
        mSurfaceSessions[surfaceSession.surfaceToken] = surfaceSession
    }

    @WorkerThread
    private fun resizeSurfaceSessionInternal(
        token: Int,
        width: Int,
        height: Int
    ) {
        mSurfaceSessions[token]?.let { surfaceSession ->
            surfaceSession.apply {
                this.width = width
                this.height = height
            }
            disposeSurfaceSession(surfaceSession)
            obtainEGLSurfaceForSession(surfaceSession)
        }
    }

    @WorkerThread
    private fun detachSurfaceSessionInternal(token: Int, callback: Runnable?) {
        val session = mSurfaceSessions.remove(token)
        if (session != null) {
            disposeSurfaceSession(session)
        }
        callback?.run()
    }

    /**
     * Helper method that issues a callback on the handler instance for this thread
     * ensuring proper nullability checks are handled.
     * This assumes that that [GLRenderer.start] has been called before attempts
     * to interact with the corresponding Handler are made with this method
     */
    private inline fun withHandler(block: Handler.() -> Unit) {
        val handler = mHandler
            ?: throw IllegalStateException("Did you forget to call GLThread.start()?")
        if (!mIsTearingDown.get()) {
            block(handler)
        }
    }

    companion object {

        private const val DEBUG = true
        private const val TAG = "GLThread"
        internal fun log(msg: String) {
            if (DEBUG) {
                Log.v(TAG, msg)
            }
        }
    }

    private class SurfaceSession(
        /**
         * Identifier used to lookup the mapping of this surface session.
         * Consumers are expected to provide this identifier to operate on the corresponding
         * surface to either request a frame be rendered or to remove this Surface
         */
        val surfaceToken: Int,

        /**
         * Target surface to render into. Can be null for situations where GL is used to render
         * into a frame buffer object provided from an AHardwareBuffer instance.
         * In these cases the actual surface is never used.
         */
        val surface: Surface?,

        /**
         * Callback used to create an EGLSurface from the provided surface as well as
         * render content to the surface
         */
        val surfaceRenderer: RenderCallback
    ) {
        /**
         * Lazily created + cached [EGLSurface] after [RenderCallback.onSurfaceCreated]
         * is invoked. This is  only modified on the backing thread
         */
        var eglSurface: EGLSurface? = null

        /**
         * Target width of the [surface]. This is only modified on the backing thread
         */
        var width: Int = 0

        /**
         * Target height of the [surface]. This is only modified on the backing thread
         */
        var height: Int = 0
    }
}
