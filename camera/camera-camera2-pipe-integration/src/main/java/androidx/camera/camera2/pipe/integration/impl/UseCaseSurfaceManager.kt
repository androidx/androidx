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

package androidx.camera.camera2.pipe.integration.impl

import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.compat.workaround.InactiveSurfaceCloser
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.DeferrableSurface.SurfaceClosedException
import androidx.camera.core.impl.DeferrableSurfaces
import androidx.camera.core.impl.utils.futures.Futures
import androidx.concurrent.futures.await
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TIMEOUT_GET_SURFACE_IN_MS = 5_000L

/** Configure the [DeferrableSurface]s to the [CameraGraph] and monitor the usage. */
@UseCaseCameraScope
public open class UseCaseSurfaceManager
@Inject
constructor(
    private val threads: UseCaseThreads,
    private val cameraPipe: CameraPipe,
    private val inactiveSurfaceCloser: InactiveSurfaceCloser,
    private val sessionConfigAdapter: SessionConfigAdapter
) : CameraSurfaceManager.SurfaceListener {

    private val lock = Any()

    @GuardedBy("lock") private var setupDeferred: Deferred<Unit>? = null

    @GuardedBy("lock") private val activeSurfaceMap = mutableMapOf<Surface, DeferrableSurface>()

    @GuardedBy("lock") private var configuredSurfaceMap: Map<Surface, DeferrableSurface>? = null

    @GuardedBy("lock") private var stopDeferred: CompletableDeferred<Unit>? = null

    /** Async set up the Surfaces to the [CameraGraph] */
    public fun setupAsync(
        graph: CameraGraph,
        sessionConfigAdapter: SessionConfigAdapter,
        surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
        timeoutMillis: Long = TIMEOUT_GET_SURFACE_IN_MS,
    ): Deferred<Unit> =
        synchronized(lock) {
            check(setupDeferred == null) { "Surfaces should only be set up once!" }
            check(stopDeferred == null) { "Surfaces being setup after stopped!" }
            check(configuredSurfaceMap == null)

            val deferrableSurfaces = sessionConfigAdapter.deferrableSurfaces
            try {
                DeferrableSurfaces.incrementAll(deferrableSurfaces)
            } catch (e: SurfaceClosedException) {
                Log.error { "Failed to increment DeferrableSurfaces: Surfaces closed" }
                // Report Surface invalid by launching a coroutine to avoid cyclic Dagger injection.
                threads.scope.launch {
                    sessionConfigAdapter.reportSurfaceInvalid(e.deferrableSurface)
                }
                return@synchronized CompletableDeferred(Unit)
            }

            val deferred =
                threads.scope
                    .async {
                        check(sessionConfigAdapter.isSessionConfigValid())

                        val surfaces =
                            try {
                                getSurfaces(deferrableSurfaces, timeoutMillis)
                            } catch (e: SurfaceClosedException) {
                                Log.error(e) { "Failed to get Surfaces: Surfaces closed" }
                                sessionConfigAdapter.reportSurfaceInvalid(e.deferrableSurface)
                                return@async
                            } catch (e: TimeoutCancellationException) {
                                Log.error(e) { "Failed to get Surfaces within $timeoutMillis ms" }
                                return@async
                            }
                        if (!isActive || surfaces.isEmpty()) {
                            Log.error {
                                "Failed to get Surfaces: isActive=$isActive, surfaces=$surfaces"
                            }
                            return@async
                        }
                        if (surfaces.areValid()) {
                            synchronized(lock) {
                                configuredSurfaceMap =
                                    deferrableSurfaces.associateBy { deferrableSurface ->
                                        checkNotNull(
                                            surfaces[deferrableSurfaces.indexOf(deferrableSurface)]
                                        )
                                    }
                                setSurfaceListener()
                            }

                            surfaceToStreamMap.forEach {
                                val stream = it.value
                                val surface = surfaces[deferrableSurfaces.indexOf(it.key)]
                                Log.debug { "Configured $surface for $stream" }
                                graph.setSurface(stream = stream, surface = surface)
                                inactiveSurfaceCloser.configure(stream, it.key, graph)
                            }
                            Log.info { "Surface setup complete" }
                        } else {
                            Log.error { "Surface setup failed: Some Surfaces are invalid" }
                            // Only handle the first failed Surface since subsequent calls to
                            // CameraInternal#onUseCaseReset() will handle the other failed Surfaces
                            // if there are any.
                            sessionConfigAdapter.reportSurfaceInvalid(
                                deferrableSurfaces[surfaces.indexOf(null)]
                            )
                        }
                    }
                    .apply {
                        // When setup is done or cancelled, decrement the DeferrableSurfaces.
                        invokeOnCompletion { DeferrableSurfaces.decrementAll(deferrableSurfaces) }
                    }
            setupDeferred = deferred
            return@synchronized deferred
        }

    /** Cancel the Surface set up and stop the monitoring of Surface usage. */
    public fun stopAsync(): Deferred<Unit> =
        synchronized(lock) {
            val currentStopDeferred = stopDeferred
            if (currentStopDeferred != null) {
                Log.warn { "UseCaseSurfaceManager is already stopping!" }
                return@synchronized currentStopDeferred
            }
            setupDeferred?.cancel()
            inactiveSurfaceCloser.closeAll()
            configuredSurfaceMap = null

            val deferred = CompletableDeferred<Unit>()
            this.stopDeferred = deferred
            // This may complete stopDeferred immediately
            tryClearSurfaceListener()

            return@synchronized deferred
        }

    /**
     * Waits for any ongoing [setupAsync] to be completed and returns a boolean value to indicate if
     * a successful setup exists.
     *
     * If [stopAsync] is called after a successful setup, this function returns false since the
     * setup was terminated.
     */
    public open suspend fun awaitSetupCompletion(): Boolean {
        if (sessionConfigAdapter.isSessionProcessorEnabled) {
            // The SessionProcessor flow does not use the setupAsync flow of this class and the
            // whole UseCaseCamera layer is created only after SessionProcessor setup is completed
            // successfully.
            return true
        }

        val setupDeferred =
            synchronized(lock) {
                val setupDeferredSnapshot = setupDeferred

                if (setupDeferredSnapshot == null || stopDeferred != null) {
                    return false
                }

                setupDeferredSnapshot
            }

        try {
            setupDeferred.await()
        } catch (e: CancellationException) {
            Log.warn(e) { "Surface setup was cancelled" }
            return false
        }

        return !setupDeferred.isCancelled
    }

    override fun onSurfaceActive(surface: Surface) {
        synchronized(lock) {
            configuredSurfaceMap?.get(surface)?.let {
                if (!activeSurfaceMap.containsKey(surface)) {
                    Log.debug { "SurfaceActive $it in ${this@UseCaseSurfaceManager}" }
                    activeSurfaceMap[surface] = it
                    try {
                        it.incrementUseCount()
                    } catch (e: SurfaceClosedException) {
                        Log.error(e) { "Error when $surface going to increase the use count." }
                        sessionConfigAdapter.reportSurfaceInvalid(e.deferrableSurface)
                    }
                }
            }
        }
    }

    override fun onSurfaceInactive(surface: Surface) {
        synchronized(lock) {
            activeSurfaceMap.remove(surface)?.let {
                Log.debug { "SurfaceInactive $it in ${this@UseCaseSurfaceManager}" }
                inactiveSurfaceCloser.onSurfaceInactive(it)
                try {
                    it.decrementUseCount()
                } catch (e: IllegalStateException) {
                    Log.error(e) { "Error when $surface going to decrease the use count." }
                }
                tryClearSurfaceListener()
            }
        }
    }

    @GuardedBy("lock")
    private fun setSurfaceListener() {
        cameraPipe.cameraSurfaceManager().addListener(this)
    }

    @GuardedBy("lock")
    private fun tryClearSurfaceListener() {
        synchronized(lock) {
            if (activeSurfaceMap.isEmpty() && configuredSurfaceMap == null) {
                Log.debug { "${this@UseCaseSurfaceManager} remove surface listener" }
                cameraPipe.cameraSurfaceManager().removeListener(this)
                stopDeferred?.complete(Unit)
            }
        }
    }

    private suspend fun getSurfaces(
        deferrableSurfaces: List<DeferrableSurface>,
        timeoutMillis: Long,
    ): List<Surface?> {
        return withTimeoutOrNull(timeMillis = timeoutMillis) {
                Futures.successfulAsList(
                        deferrableSurfaces.map { Futures.nonCancellationPropagating(it.surface) }
                    )
                    .await()
            }
            .orEmpty()
    }

    private fun List<Surface?>.areValid(): Boolean {
        // If a Surface in configuredSurfaces is null it means the
        // Surface was not retrieved from the ListenableFuture.
        return isNotEmpty() && !contains(null)
    }
}
