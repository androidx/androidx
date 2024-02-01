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
import androidx.wear.watchface.WatchFaceService.Companion.MAX_CREATE_WATCHFACE_TIME_MILLIS
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.google.common.util.concurrent.ListenableFuture
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * [ListenableFuture]-based compatibility wrapper around [WatchFaceService]'s suspending
 * [WatchFaceService.createWatchFace].
 *
 * ListenableWatchFaceServices are required to be stateless as multiple can be created in parallel.
 * If per instance state is required please use [ListenableStatefulWatchFaceService].
 */
public abstract class ListenableWatchFaceService : WatchFaceService() {
    /**
     * Override this factory method to create your WatchFaceImpl. This method will be called by the
     * library on a background thread, if possible any expensive initialization should be done
     * asynchronously. The [WatchFace] and its [Renderer] should be accessed exclusively from the
     * UiThread afterwards. There is a memory barrier between construction and rendering so no
     * special threading primitives are required.
     *
     * Warning the system will likely time out waiting for watch face initialization if it takes
     * longer than [MAX_CREATE_WATCHFACE_TIME_MILLIS] milliseconds.
     *
     * Note cancellation of the returned future is not supported.
     *
     * @param surfaceHolder The [SurfaceHolder] to pass to the [Renderer]'s constructor.
     * @param watchState The [WatchState] for the watch face.
     * @param complicationSlotsManager The [ComplicationSlotsManager] returned by
     *   [createComplicationSlotsManager].
     * @param currentUserStyleRepository The [CurrentUserStyleRepository] constructed using the
     *   [UserStyleSchema] returned by [createUserStyleSchema].
     * @return A [ListenableFuture] for a [WatchFace] whose [Renderer] uses the provided
     *   [surfaceHolder].
     */
    protected abstract fun createWatchFaceFuture(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ListenableFuture<WatchFace>

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace = suspendCancellableCoroutine {
        val future =
            createWatchFaceFuture(
                surfaceHolder,
                watchState,
                complicationSlotsManager,
                currentUserStyleRepository
            )
        future.addListener({ it.resume(future.get()) }, { runnable -> runnable.run() })
    }
}

/**
 * [ListenableFuture]-based compatibility wrapper around [StatefulWatchFaceService]'s suspending
 * [WatchFaceService.createWatchFace].
 *
 * [ListenableWatchFaceService] is required to be stateless as multiple can be created in
 * parallel. ListenableStatefulWatchFaceService allows for metadata to be associated with
 * watch faces on a per instance basis. This state is created by [createExtra] and is passed into
 * other methods.
 */
public abstract class
ListenableStatefulWatchFaceService<Extra> : StatefulWatchFaceService<Extra>() {
    /**
     * Override this factory method to create your WatchFaceImpl. This method will be called by the
     * library on a background thread, if possible any expensive initialization should be done
     * asynchronously. The [WatchFace] and its [Renderer] should be accessed exclusively from the
     * UiThread afterwards. There is a memory barrier between construction and rendering so no
     * special threading primitives are required.
     *
     * Warning the system will likely time out waiting for watch face initialization if it takes
     * longer than [MAX_CREATE_WATCHFACE_TIME_MILLIS] milliseconds.
     *
     * Note cancellation of the returned future is not supported.
     *
     * @param surfaceHolder The [SurfaceHolder] to pass to the [Renderer]'s constructor.
     * @param watchState The [WatchState] for the watch face.
     * @param complicationSlotsManager The [ComplicationSlotsManager] returned by
     *   [createComplicationSlotsManager].
     * @param currentUserStyleRepository The [CurrentUserStyleRepository] constructed using the
     *   [UserStyleSchema] returned by [createUserStyleSchema].
     * @param extra The object returned by [createExtra].
     * @return A [ListenableFuture] for a [WatchFace] whose [Renderer] uses the provided
     *   [surfaceHolder].
     */
    @Suppress("AsyncSuffixFuture") // This is java compat on top of the Kotlin version.
    protected abstract fun createWatchFaceFuture(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
        extra: Extra
    ): ListenableFuture<WatchFace>

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
        extra: Extra
    ): WatchFace = suspendCancellableCoroutine {
        val future =
            createWatchFaceFuture(
                surfaceHolder,
                watchState,
                complicationSlotsManager,
                currentUserStyleRepository,
                extra
            )
        future.addListener({ it.resume(future.get()) }, { runnable -> runnable.run() })
    }
}

/**
 * [ListenableFuture]-based compatibility wrapper around [WatchFaceRuntimeService]'s suspending
 * [WatchFaceService.createWatchFace].
 *
 * ListenableWatchFaceRuntimeService are required to be stateless as multiple can be created in
 * parallel. If per instance state is required please use
 * [ListenableStatefulWatchFaceRuntimeService].
 */
public abstract class ListenableWatchFaceRuntimeService : WatchFaceRuntimeService() {
    /**
     * Override this factory method to create your WatchFaceImpl. This method will be called by the
     * library on a background thread, if possible any expensive initialization should be done
     * asynchronously. The [WatchFace] and its [Renderer] should be accessed exclusively from the
     * UiThread afterwards. There is a memory barrier between construction and rendering so no
     * special threading primitives are required.
     *
     * Warning the system will likely time out waiting for watch face initialization if it takes
     * longer than [MAX_CREATE_WATCHFACE_TIME_MILLIS] milliseconds.
     *
     * Note cancellation of the returned future is not supported.
     *
     * @param surfaceHolder The [SurfaceHolder] to pass to the [Renderer]'s constructor.
     * @param watchState The [WatchState] for the watch face.
     * @param complicationSlotsManager The [ComplicationSlotsManager] returned by
     *   [createComplicationSlotsManager].
     * @param currentUserStyleRepository The [CurrentUserStyleRepository] constructed using the
     *   [UserStyleSchema] returned by [createUserStyleSchema].
     * @param resourceOnlyWatchFacePackageName The android package from which the watch face
     *   definition should be loaded.
     * @return A [ListenableFuture] for a [WatchFace] whose [Renderer] uses the provided
     *   [surfaceHolder].
     */
    @Suppress("AsyncSuffixFuture") // This is java compat on top of the Kotlin version.
    protected abstract fun createWatchFaceFutureAsync(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
        resourceOnlyWatchFacePackageName: String
    ): ListenableFuture<WatchFace>

    final override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
        resourceOnlyWatchFacePackageName: String
    ): WatchFace = suspendCancellableCoroutine {
        val future =
            createWatchFaceFutureAsync(
                surfaceHolder,
                watchState,
                complicationSlotsManager,
                currentUserStyleRepository,
                resourceOnlyWatchFacePackageName
            )
        future.addListener({ it.resume(future.get()) }, { runnable -> runnable.run() })
    }
}

/**
 * [ListenableFuture]-based compatibility wrapper around [WatchFaceRuntimeService]'s suspending
 * [WatchFaceService.createWatchFace].
 *
 * [ListenableWatchFaceRuntimeService] is required to be stateless as multiple can be created in
 * parallel. ListenableStatefulWatchFaceRuntimeService allows for metadata to be associated with
 * watch faces on a per instance basis. This state is created by [createExtra] and is passed into
 * other methods.
 */
public abstract class
ListenableStatefulWatchFaceRuntimeService<Extra> : StatefulWatchFaceRuntimeService<Extra>() {
    /**
     * Override this factory method to create your WatchFaceImpl. This method will be called by the
     * library on a background thread, if possible any expensive initialization should be done
     * asynchronously. The [WatchFace] and its [Renderer] should be accessed exclusively from the
     * UiThread afterwards. There is a memory barrier between construction and rendering so no
     * special threading primitives are required.
     *
     * Warning the system will likely time out waiting for watch face initialization if it takes
     * longer than [MAX_CREATE_WATCHFACE_TIME_MILLIS] milliseconds.
     *
     * Note cancellation of the returned future is not supported.
     *
     * @param surfaceHolder The [SurfaceHolder] to pass to the [Renderer]'s constructor.
     * @param watchState The [WatchState] for the watch face.
     * @param complicationSlotsManager The [ComplicationSlotsManager] returned by
     *   [createComplicationSlotsManager].
     * @param currentUserStyleRepository The [CurrentUserStyleRepository] constructed using the
     *   [UserStyleSchema] returned by [createUserStyleSchema].
     * @param resourceOnlyWatchFacePackageName The android package from which the watch face
     *   definition should be loaded.
     * @param extra The object returned by [createExtra].
     * @return A [ListenableFuture] for a [WatchFace] whose [Renderer] uses the provided
     *   [surfaceHolder].
     */
    protected abstract fun createWatchFaceFutureAsync(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
        resourceOnlyWatchFacePackageName: String,
        extra: Extra
    ): ListenableFuture<WatchFace>

    final override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
        resourceOnlyWatchFacePackageName: String,
        extra: Extra
    ): WatchFace = suspendCancellableCoroutine {
        val future =
            createWatchFaceFutureAsync(
                surfaceHolder,
                watchState,
                complicationSlotsManager,
                currentUserStyleRepository,
                resourceOnlyWatchFacePackageName,
                extra
            )
        future.addListener({ it.resume(future.get()) }, { runnable -> runnable.run() })
    }
}
