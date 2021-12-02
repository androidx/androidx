/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface

import android.opengl.EGL14
import android.view.SurfaceHolder
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.resume

internal val EGL_CONFIG_ATTRIB_LIST = intArrayOf(
    EGL14.EGL_RENDERABLE_TYPE,
    EGL14.EGL_OPENGL_ES2_BIT,
    EGL14.EGL_RED_SIZE,
    8,
    EGL14.EGL_GREEN_SIZE,
    8,
    EGL14.EGL_BLUE_SIZE,
    8,
    EGL14.EGL_ALPHA_SIZE,
    8,
    EGL14.EGL_NONE
)

internal val EGL_SURFACE_ATTRIB_LIST = intArrayOf(EGL14.EGL_NONE)

/**
 * [ListenableFuture]-based compatibility wrapper around [Renderer.GlesRenderer]'s suspending
 * methods.
 */
public abstract class ListenableGlesRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    @IntRange(from = 0, to = 60000)
    interactiveDrawModeUpdateDelayMillis: Long,
    eglConfigAttribList: IntArray = EGL_CONFIG_ATTRIB_LIST,
    eglSurfaceAttribList: IntArray = EGL_SURFACE_ATTRIB_LIST
) : Renderer.GlesRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    interactiveDrawModeUpdateDelayMillis,
    eglConfigAttribList,
    eglSurfaceAttribList
) {
    /**
     * Inside of a [Mutex] this function sets the GL context associated with the
     * [WatchFaceService.getBackgroundThreadHandler]'s looper thread as the current one,
     * executes [runnable] and finally unsets the GL context.
     *
     * Access to the GL context this way is necessary because GL contexts are not shared
     * between renderers and there can be multiple watch face instances existing concurrently
     * (e.g. headless and interactive, potentially from different watch faces if an APK
     * contains more than one [WatchFaceService]).
     *
     * NB this function is called by the library before running
     * [runBackgroundThreadGlCommands] so there's no need to use this directly in client
     * code unless you need to make GL calls outside of those methods.
     *
     * @throws [IllegalStateException] if the calls to [EGL14.eglMakeCurrent] fails
     */
    @WorkerThread
    public fun runBackgroundThreadGlCommands(runnable: Runnable) {
        runBlocking {
            runBackgroundThreadGlCommands {
                runnable.run()
            }
        }
    }

    /**
     * Inside of a [Mutex] this function sets the UiThread GL context as the current
     * one, executes [runnable] and finally unsets the GL context.
     *
     * Access to the GL context this way is necessary because GL contexts are not shared
     * between renderers and there can be multiple watch face instances existing concurrently
     * (e.g. headless and interactive, potentially from different watch faces if an APK
     * contains more than one [WatchFaceService]).
     *
     * @throws [IllegalStateException] if the calls to [EGL14.eglMakeCurrent] fails
     */
    @UiThread
    public fun runUiThreadGlCommands(runnable: Runnable) {
        runBlocking {
            runUiThreadGlCommands {
                runnable.run()
            }
        }
    }

    /**
     * Called once a background thread when a new GL context is created on the background
     * thread, before any subsequent calls to [render]. Note this function is called inside a
     * lambda passed to [runBackgroundThreadGlCommands] which has synchronized access to the
     * GL context. Note cancellation of the returned future is not supported.
     *
     * @return A ListenableFuture<Unit> which is resolved when background thread work has
     * completed. Rendering will be blocked until this has resolved.
     */
    protected open fun onBackgroundThreadGlContextCreatedFuture(): ListenableFuture<Unit> {
        return SettableFuture.create<Unit>().apply {
            set(Unit)
        }
    }

    override suspend fun onBackgroundThreadGlContextCreated(): Unit = suspendCancellableCoroutine {
        val future = onBackgroundThreadGlContextCreatedFuture()
        future.addListener(
            { it.resume(future.get()) },
            { runnable -> runnable.run() }
        )
    }

    /**
     * Called when a new GL surface is created on the UiThread, before any subsequent calls
     * to [render] and in response to [SurfaceHolder.Callback.surfaceChanged]. Note this function
     * is called inside a lambda passed to [Renderer.GlesRenderer.runUiThreadGlCommands] which
     * has synchronized access to the GL context.  Note cancellation of the returned future is not
     * supported.
     *
     * @param width width of surface in pixels
     * @param height height of surface in pixels
     * @return A ListenableFuture<Unit> which is resolved when UI thread work has completed.
     * Rendering will be blocked until this has resolved.
     */
    @UiThread
    protected open fun
    onUiThreadGlSurfaceCreatedFuture(@Px width: Int, @Px height: Int): ListenableFuture<Unit> {
        return SettableFuture.create<Unit>().apply {
            set(Unit)
        }
    }

    override suspend fun onUiThreadGlSurfaceCreated(@Px width: Int, @Px height: Int): Unit =
        suspendCancellableCoroutine {
            val future = onUiThreadGlSurfaceCreatedFuture(width, height)
            future.addListener(
                { it.resume(future.get()) },
                { runnable -> runnable.run() }
            )
        }
}