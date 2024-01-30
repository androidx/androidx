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

import android.view.SurfaceHolder
import androidx.annotation.IntRange
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.wear.watchface.Renderer.SharedAssets
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * [ListenableFuture]-based compatibility wrapper around [Renderer.CanvasRenderer]'s suspending
 * methods.
 */
@Deprecated(message = "Use ListenableCanvasRenderer2 instead")
@Suppress("Deprecation")
public abstract class ListenableCanvasRenderer
@JvmOverloads
constructor(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    @CanvasTypeIntDef private val canvasType: Int,
    @IntRange(from = 0, to = 60000) interactiveDrawModeUpdateDelayMillis: Long,
    clearWithBackgroundTintBeforeRenderingHighlightLayer: Boolean = false
) :
    Renderer.CanvasRenderer(
        surfaceHolder,
        currentUserStyleRepository,
        watchState,
        canvasType,
        interactiveDrawModeUpdateDelayMillis,
        clearWithBackgroundTintBeforeRenderingHighlightLayer
    ) {
    /**
     * Perform UiThread specific initialization. Will be called once during initialization before
     * any subsequent calls to [render]. Note cancellation of the returned future is not supported.
     *
     * @return A ListenableFuture<Unit> which is resolved when UiThread has completed. Rendering
     *   will be blocked until this has resolved.
     */
    @UiThread
    @Suppress("AsyncSuffixFuture") // This is the guava wrapper for a suspend function
    public open fun initFuture(): ListenableFuture<Unit> {
        return SettableFuture.create<Unit>().apply { set(Unit) }
    }

    override suspend fun init(): Unit = suspendCancellableCoroutine {
        val future = initFuture()
        future.addListener({ it.resume(future.get()) }, { runnable -> runnable.run() })
    }
}

/**
 * [ListenableFuture]-based compatibility wrapper around [Renderer.CanvasRenderer2]'s suspending
 * methods.
 */
public abstract class ListenableCanvasRenderer2<SharedAssetsT>
@JvmOverloads
constructor(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    @CanvasTypeIntDef private val canvasType: Int,
    @IntRange(from = 0, to = 60000) interactiveDrawModeUpdateDelayMillis: Long,
    clearWithBackgroundTintBeforeRenderingHighlightLayer: Boolean = false
) :
    Renderer.CanvasRenderer2<SharedAssetsT>(
        surfaceHolder,
        currentUserStyleRepository,
        watchState,
        canvasType,
        interactiveDrawModeUpdateDelayMillis,
        clearWithBackgroundTintBeforeRenderingHighlightLayer
    ) where SharedAssetsT : SharedAssets {
    /**
     * Perform UiThread specific initialization. Will be called once during initialization before
     * any subsequent calls to [render]. Note cancellation of the returned future is not supported.
     *
     * @return A ListenableFuture<Unit> which is resolved when UiThread has completed. Rendering
     *   will be blocked until this has resolved.
     */
    @UiThread
    @Suppress("AsyncSuffixFuture") // This is the guava wrapper for a suspend function
    public open fun initFuture(): ListenableFuture<Unit> {
        return SettableFuture.create<Unit>().apply { set(Unit) }
    }

    final override suspend fun init(): Unit = suspendCancellableCoroutine {
        val future = initFuture()
        future.addListener({ it.resume(future.get()) }, { runnable -> runnable.run() })
    }

    /**
     * Implement to allow your Renderers to share data with SharedAssets. When editing multiple
     * [WatchFaceService], instances and hence Renderers can exist concurrently (e.g. a headless
     * instance and an interactive instance). Using [SharedAssets] allows memory to be saved by
     * sharing immutable data (e.g. Bitmaps, shaders, etc...) between them.
     *
     * To take advantage of SharedAssets, override this method. The constructed SharedAssets are
     * passed into the [render] as an argument (NB you'll have to cast this to your type).
     *
     * When all instances using SharedAssets have been closed, [SharedAssets.onDestroy] will be
     * called.
     *
     * Note that while SharedAssets are constructed on a background thread, they'll typically be
     * used on the main thread and subsequently destroyed there.
     *
     * @return A [ListenableFuture] for the [SharedAssetsT] that will be passed into [render] and
     *   [renderHighlightLayer]
     */
    @WorkerThread
    @Suppress("AsyncSuffixFuture") // This is the guava wrapper for a suspend function
    public abstract fun createSharedAssetsFuture(): ListenableFuture<SharedAssetsT>

    final override suspend fun createSharedAssets(): SharedAssetsT = suspendCancellableCoroutine {
        val future = createSharedAssetsFuture()
        future.addListener({ it.resume(future.get()) }, { runnable -> runnable.run() })
    }
}
