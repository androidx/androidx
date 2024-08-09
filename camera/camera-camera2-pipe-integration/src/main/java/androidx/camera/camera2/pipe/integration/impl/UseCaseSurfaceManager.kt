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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

private const val TIMEOUT_GET_SURFACE_IN_MS = 5_000L

/** Configure the [DeferrableSurface]s to the [CameraGraph] and monitor the usage. */
@UseCaseCameraScope
public class UseCaseSurfaceManager
@Inject
constructor(
    private val threads: UseCaseThreads,
    private val cameraPipe: CameraPipe,
    private val inactiveSurfaceCloser: InactiveSurfaceCloser,
) : CameraSurfaceManager.SurfaceListener {

    private val lock = Any()

    @GuardedBy("lock") private val activeSurfaceMap = mutableMapOf<Surface, DeferrableSurface>()

    @GuardedBy("lock") private var configuredSurfaceMap: Map<Surface, DeferrableSurface>? = null

    private var setupSurfaceDeferred: Deferred<Unit>? = null

    @GuardedBy("lock") private var stopDeferred: CompletableDeferred<Unit>? = null

    @GuardedBy("lock") private var _sessionConfigAdapter: SessionConfigAdapter? = null

    /** Async set up the Surfaces to the [CameraGraph] */
    public fun setupAsync(
        graph: CameraGraph,
        sessionConfigAdapter: SessionConfigAdapter,
        surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
        timeoutMillis: Long = TIMEOUT_GET_SURFACE_IN_MS,
    ): Deferred<Unit> {
        check(setupSurfaceDeferred == null)
        check(synchronized(lock) { stopDeferred == null && configuredSurfaceMap == null })

        return threads.scope
            .async {
                check(sessionConfigAdapter.isSessionConfigValid())

                sessionConfigAdapter.useDeferrableSurfaces { deferrableSurfaces ->
                    val surfaces = getSurfaces(deferrableSurfaces, timeoutMillis)
                    if (!isActive) return@async
                    if (surfaces.isEmpty()) {
                        Log.error { "Surface list is empty" }
                        return@async
                    }
                    if (surfaces.areValid()) {
                        synchronized(lock) {
                            configuredSurfaceMap =
                                deferrableSurfaces.associateBy { deferrableSurface ->
                                    surfaces[deferrableSurfaces.indexOf(deferrableSurface)]!!
                                }
                            _sessionConfigAdapter = sessionConfigAdapter
                            setSurfaceListener()
                        }

                        surfaceToStreamMap.forEach {
                            val stream = it.value
                            val surface = surfaces[deferrableSurfaces.indexOf(it.key)]
                            Log.debug { "Configured $surface for $stream" }
                            graph.setSurface(stream = stream, surface = surface)
                            inactiveSurfaceCloser.configure(stream, it.key, graph)
                        }
                    } else {
                        // Only handle the first failed Surface since subsequent calls to
                        // CameraInternal#onUseCaseReset() will handle the other failed Surfaces if
                        // there are any.
                        sessionConfigAdapter.reportSurfaceInvalid(
                            deferrableSurfaces[surfaces.indexOf(null)]
                        )
                    }
                }
            }
            .also { completeDeferred ->
                setupSurfaceDeferred = completeDeferred
                completeDeferred.invokeOnCompletion { setupSurfaceDeferred = null }
            }
    }

    /** Cancel the Surface set up and stop the monitoring of Surface usage. */
    public fun stopAsync(): Deferred<Unit> {
        setupSurfaceDeferred?.cancel()

        return synchronized(lock) {
                inactiveSurfaceCloser.closeAll()
                configuredSurfaceMap = null
                stopDeferred =
                    stopDeferred
                        ?: CompletableDeferred<Unit>().apply {
                            invokeOnCompletion { synchronized(lock) { stopDeferred = null } }
                        }
                stopDeferred!!
            }
            .also { tryClearSurfaceListener() }
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
                        _sessionConfigAdapter?.reportSurfaceInvalid(e.deferrableSurface)
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

    private fun setSurfaceListener() {
        cameraPipe.cameraSurfaceManager().addListener(this)
    }

    private fun tryClearSurfaceListener() {
        synchronized(lock) {
            if (activeSurfaceMap.isEmpty() && configuredSurfaceMap == null) {
                Log.debug { "${this@UseCaseSurfaceManager} remove surface listener" }
                cameraPipe.cameraSurfaceManager().removeListener(this)
                _sessionConfigAdapter = null
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

    /**
     * Set use count at the [DeferrableSurface]s when the specified function [block] is using the
     * [DeferrableSurface].
     *
     * If it cannot set the use count to the [DeferrableSurface], the [block] will not be called.
     */
    private inline fun SessionConfigAdapter.useDeferrableSurfaces(
        block: (List<DeferrableSurface>) -> Unit
    ) =
        try {
            DeferrableSurfaces.incrementAll(deferrableSurfaces)
            try {
                block(deferrableSurfaces)
            } finally {
                DeferrableSurfaces.decrementAll(deferrableSurfaces)
            }
        } catch (e: SurfaceClosedException) {
            reportSurfaceInvalid(e.deferrableSurface)
        }

    private fun List<Surface?>.areValid(): Boolean {
        // If a Surface in configuredSurfaces is null it means the
        // Surface was not retrieved from the ListenableFuture.
        return isNotEmpty() && !contains(null)
    }
}
