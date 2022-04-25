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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.adapter

import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.core.UseCase
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.futures.Futures
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TIMEOUT_GET_SURFACE_IN_MS = 5_000L

/**
 * Aggregate the SessionConfig from a List of [UseCase]s, and provide a validated SessionConfig for
 * operation.
 */
class SessionConfigAdapter(
    private val useCases: Collection<UseCase>,
    private val threads: UseCaseThreads,
) {
    private val validatingBuilder: SessionConfig.ValidatingBuilder by lazy {
        val validatingBuilder = SessionConfig.ValidatingBuilder()
        useCases.forEach {
            validatingBuilder.add(it.sessionConfig)
        }
        validatingBuilder
    }

    private val deferrableSurfaces: List<DeferrableSurface> by lazy {
        sessionConfig.surfaces
    }

    private val sessionConfig: SessionConfig by lazy {
        check(validatingBuilder.isValid)

        validatingBuilder.build()
    }

    fun getValidSessionConfigOrNull(): SessionConfig? {
        return if (isSessionConfigValid()) sessionConfig else null
    }

    fun isSessionConfigValid(): Boolean {
        return validatingBuilder.isValid
    }

    fun setupSurfaceAsync(
        graph: CameraGraph,
        surfaceToStreamMap: Map<DeferrableSurface, StreamId>
    ): Deferred<Unit> =
        threads.scope.async {
            val surfaces = getSurfaces(deferrableSurfaces)

            if (!isActive) return@async

            if (surfaces.isEmpty()) {
                debug { "Surface list is empty" }
                return@async
            }

            if (areSurfacesValid(surfaces)) {
                surfaceToStreamMap.forEach {
                    val stream = it.value
                    val surface = surfaces[deferrableSurfaces.indexOf(it.key)]
                    debug { "Configured $surface for $stream" }
                    graph.setSurface(
                        stream = stream,
                        surface = surface
                    )
                }
            } else {
                debug { "Surface contains failed, notify SessionConfig invalid" }

                // Only handle the first failed Surface since subsequent calls to
                // CameraInternal#onUseCaseReset() will handle the other failed Surfaces if there
                // are any.
                val deferrableSurface = deferrableSurfaces[surfaces.indexOf(null)]
                val sessionConfig =
                    useCases.firstOrNull { useCase ->
                        useCase.sessionConfig.surfaces.contains(deferrableSurface)
                    }?.sessionConfig

                withContext(Dispatchers.Main) {
                    // The error listener is used to notify the UseCase to recreate the pipeline,
                    // and the create pipeline task would be executed on the main thread.
                    sessionConfig?.errorListeners?.forEach {
                        it.onError(
                            sessionConfig,
                            SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET
                        )
                    }
                }
            }
        }

    private suspend fun getSurfaces(deferrableSurfaces: List<DeferrableSurface>): List<Surface?> {
        return withTimeoutOrNull(timeMillis = TIMEOUT_GET_SURFACE_IN_MS) {
            Futures.successfulAsList(
                deferrableSurfaces.map {
                    it.surface
                }
            ).await()
        }.orEmpty()
    }

    private fun areSurfacesValid(surfaces: List<Surface?>): Boolean {
        // If a Surface in configuredSurfaces is null it means the
        // Surface was not retrieved from the ListenableFuture.
        return surfaces.isNotEmpty() && !surfaces.contains(null)
    }
}