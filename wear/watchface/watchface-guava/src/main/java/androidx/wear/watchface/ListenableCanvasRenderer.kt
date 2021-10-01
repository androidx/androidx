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
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * [ListenableFuture]-based compatibility wrapper around [Renderer.CanvasRenderer]'s suspending
 * methods.
 */
public abstract class ListenableCanvasRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    @CanvasType private val canvasType: Int,
    @IntRange(from = 0, to = 60000)
    interactiveDrawModeUpdateDelayMillis: Long
) : Renderer.CanvasRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    interactiveDrawModeUpdateDelayMillis
) {
    /**
     * Perform UiThread specific initialization.  Will be called once during initialization
     * before any subsequent calls to [render].
     *
     * @return A ListenableFuture<Unit> which is resolved when UiThread has completed. Rendering
     * will be blocked until this has resolved.
     */
    @UiThread
    public open fun initFuture(): ListenableFuture<Unit> {
        return SettableFuture.create<Unit>().apply {
            set(Unit)
        }
    }

    override suspend fun init(): Unit = suspendCancellableCoroutine {
        val future = initFuture()
        future.addListener(
            { it.resume(future.get()) },
            { runnable -> runnable.run() }
        )
    }
}