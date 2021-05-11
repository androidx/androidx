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
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred

/**
 * [ListenableFuture]-based compatibility wrapper around [WatchFaceService]'s suspending
 * [WatchFaceService.createWatchFace].
 */
public abstract class ListenableWatchFaceService : WatchFaceService() {
    /** Override this factory method to create your WatchFaceImpl. */
    protected abstract fun createWatchFaceFuture(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationsManager: ComplicationsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ListenableFuture<WatchFace>

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationsManager: ComplicationsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace = CompletableDeferred<WatchFace>().apply {
        val future = createWatchFaceFuture(
            surfaceHolder,
            watchState,
            complicationsManager,
            currentUserStyleRepository
        )
        future.addListener(
            { complete(future.get()) },
            { runnable -> runnable.run() }
        )
    }.await()
}