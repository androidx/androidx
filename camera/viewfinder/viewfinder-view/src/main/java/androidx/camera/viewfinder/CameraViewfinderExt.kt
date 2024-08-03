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

package androidx.camera.viewfinder

import android.view.Surface
import androidx.concurrent.futures.await

/**
 * Provides a suspending function of [CameraViewfinder.requestSurfaceAsync] to request a [Surface]
 * by sending a [androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest].
 */
object CameraViewfinderExt {

    @Suppress("DEPRECATION")
    @Deprecated(
        message =
            "Use androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest " + "as argument",
        replaceWith =
            ReplaceWith(
                "requestSurface using " +
                    "androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest"
            )
    )
    suspend fun CameraViewfinder.requestSurface(
        viewfinderSurfaceRequest: ViewfinderSurfaceRequest
    ): Surface = requestSurfaceAsync(viewfinderSurfaceRequest).await()

    suspend fun CameraViewfinder.requestSurface(
        viewfinderSurfaceRequest: androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest
    ): Surface = requestSurfaceAsync(viewfinderSurfaceRequest).await()
}
