/*
 * Copyright 2020 The Android Open Source Project
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

import android.hardware.camera2.CameraDevice
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamConfig
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.StreamType
import androidx.camera.camera2.pipe.TorchState
import androidx.camera.camera2.pipe.impl.Log.debug
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.core.UseCase
import androidx.camera.core.impl.DeferrableSurface
import dagger.Module
import dagger.Provides
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

internal val useCaseCameraIds = atomic(0)

/**
 * API for interacting with a [CameraGraph] that has been configured with a set of [UseCase]'s
 */
class UseCaseCamera(
    private val cameraGraph: CameraGraph,
    private val useCases: List<UseCase>,
    private val surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
    private val cameraScope: CoroutineScope
) {
    private val debugId = useCaseCameraIds.incrementAndGet()

    private var _activeUseCases = setOf<UseCase>()

    var activeUseCases: Set<UseCase>
        get() = _activeUseCases
        set(value) {
            // Note: This may be called with the same set of values that was previously set. This
            // is used as a signal to indicate the properties of the UseCase may have changed.
            _activeUseCases = value
            updateUseCases()
        }

    init {
        debug { "Configured $this for $useCases" }
    }

    fun close() {
        debug { "Closing $this" }
        cameraGraph.close()
    }

    suspend fun enableTorchAsync(enabled: Boolean): Deferred<FrameNumber> {
        return cameraGraph.acquireSession().use {
            it.setTorch(
                when (enabled) {
                    true -> TorchState.ON
                    false -> TorchState.OFF
                }
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun updateUseCases() {
        val repeatingStreamIds = mutableSetOf<StreamId>()
        for (useCase in activeUseCases) {
            val repeatingCapture = useCase.sessionConfig?.repeatingCaptureConfig
            if (repeatingCapture != null) {
                for (deferrableSurface in repeatingCapture.surfaces) {
                    val streamId = surfaceToStreamMap[deferrableSurface]
                    if (streamId != null) {
                        repeatingStreamIds.add(streamId)
                    }
                }
            }
        }

        // TODO: This needs to aggregate the current parameters and pass them to the request.

        // In order to preserve ordering, this starts acquiring the session on the current thread,
        // and will only switch to the cameraScope threads if it needs to suspend. This is important
        // because access to the cameraGraph is well ordered, and if the coroutine suspends, it will
        // resume in the order it accessed the cameraGraph.
        cameraScope.launch(start = CoroutineStart.UNDISPATCHED) {
            cameraGraph.acquireSession().use {
                it.setRepeating(
                    Request(
                        streams = repeatingStreamIds.toList()
                    )
                )
            }
        }
    }

    override fun toString(): String = "UseCaseCamera-$debugId"

    @Module
    class Bindings {
        companion object {
            @UseCaseCameraScope
            @Provides
            fun provideCameraGraphController(
                cameraPipe: CameraPipe,
                useCases: java.util.ArrayList<UseCase>,
                cameraConfig: CameraConfig,
                callbackMap: CameraCallbackMap,
                coroutineScope: CoroutineScope,
            ): UseCaseCamera {
                val streamConfigs = mutableListOf<StreamConfig>()
                val useCaseMap = mutableMapOf<StreamConfig, UseCase>()

                // TODO: This may need to combine outputs that are (or will) share the same output
                //  imageReader or surface. Right now, each UseCase gets its own [StreamConfig]
                // TODO: useCases only have a single `attachedSurfaceResolution`, yet they have a
                //  list of deferrableSurfaces.
                for (useCase in useCases) {
                    val config = StreamConfig(
                        size = useCase.attachedSurfaceResolution!!,
                        format = StreamFormat(useCase.imageFormat),
                        camera = cameraConfig.cameraId,
                        type = StreamType.SURFACE,
                        deferrable = false
                    )
                    streamConfigs.add(config)
                    useCaseMap[config] = useCase
                }

                // Build up a config (using TEMPLATE_PREVIEW by default)
                val config = CameraGraph.Config(
                    camera = cameraConfig.cameraId,
                    streams = streamConfigs,
                    listeners = listOf(callbackMap),
                    template = RequestTemplate(CameraDevice.TEMPLATE_PREVIEW)
                )
                val graph = cameraPipe.create(config)

                val surfaceToStreamMap = mutableMapOf<DeferrableSurface, StreamId>()
                for ((streamConfig, useCase) in useCaseMap) {
                    val stream = graph.streams[streamConfig]
                    val useCaseSessionConfig = useCase.sessionConfig

                    // TODO: UseCases have inconsistent opinions about how surfaces are handled,
                    //  this code assumes only a single surface per UseCase.
                    val deferredSurfaces = useCaseSessionConfig?.surfaces
                    if (stream != null && deferredSurfaces != null && deferredSurfaces.size == 1) {
                        val deferredSurface = deferredSurfaces[0]
                        graph.setSurface(stream.id, deferredSurface.surface.get())
                        surfaceToStreamMap[deferredSurface] = stream.id
                    }
                }

                graph.start()
                return UseCaseCamera(graph, useCases, surfaceToStreamMap, coroutineScope)
            }
        }
    }
}