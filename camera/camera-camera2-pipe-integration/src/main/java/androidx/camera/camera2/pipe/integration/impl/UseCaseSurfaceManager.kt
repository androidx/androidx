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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.impl

import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.utils.futures.Futures
import androidx.concurrent.futures.await
import javax.inject.Inject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

private const val TIMEOUT_GET_SURFACE_IN_MS = 5_000L

/**
 * Configure the [DeferrableSurface]s to the [CameraGraph].
 */
@UseCaseCameraScope
class UseCaseSurfaceManager @Inject constructor(
    private val threads: UseCaseThreads,
) {

    private var setupSurfaceDeferred: Deferred<Unit>? = null

    /**
     * Async set up the Surfaces to the [CameraGraph]
     */
    fun setupAsync(
        graph: CameraGraph,
        sessionConfigAdapter: SessionConfigAdapter,
        surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
        timeoutMillis: Long = TIMEOUT_GET_SURFACE_IN_MS,
    ): Deferred<Unit> {
        setupSurfaceDeferred = threads.scope.async {
            check(sessionConfigAdapter.isSessionConfigValid())

            val deferrableSurfaces = sessionConfigAdapter.deferrableSurfaces

            val surfaces = getSurfaces(deferrableSurfaces, timeoutMillis)

            if (!isActive) return@async

            if (surfaces.isEmpty()) {
                Log.error { "Surface list is empty" }
                return@async
            }

            if (areSurfacesValid(surfaces)) {
                surfaceToStreamMap.forEach {
                    val stream = it.value
                    val surface = surfaces[deferrableSurfaces.indexOf(it.key)]
                    Log.debug { "Configured $surface for $stream" }
                    graph.setSurface(
                        stream = stream, surface = surface
                    )
                }
            } else {
                // Only handle the first failed Surface since subsequent calls to
                // CameraInternal#onUseCaseReset() will handle the other failed Surfaces if there
                // are any.
                sessionConfigAdapter.reportSurfaceInvalid(
                    deferrableSurfaces[surfaces.indexOf(null)]
                )
            }
        }

        return setupSurfaceDeferred!!
    }

    /**
     * Cancel the Surface set up.
     */
    fun stop() {
        setupSurfaceDeferred?.cancel()
    }

    private suspend fun getSurfaces(
        deferrableSurfaces: List<DeferrableSurface>,
        timeoutMillis: Long,
    ): List<Surface?> {
        return withTimeoutOrNull(timeMillis = timeoutMillis) {
            Futures.successfulAsList(deferrableSurfaces.map {
                Futures.nonCancellationPropagating(it.surface)
            }).await()
        }.orEmpty()
    }

    private fun areSurfacesValid(surfaces: List<Surface?>): Boolean {
        // If a Surface in configuredSurfaces is null it means the
        // Surface was not retrieved from the ListenableFuture.
        return surfaces.isNotEmpty() && !surfaces.contains(null)
    }
}