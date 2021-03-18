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

import android.hardware.camera2.CaptureRequest
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.TorchState
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.core.UseCase
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import dagger.Module
import dagger.Provides
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Deferred

internal val useCaseCameraIds = atomic(0)

interface UseCaseCamera {
    // UseCases
    var activeUseCases: Set<UseCase>

    // Parameters
    fun <T> setParameter(key: CaptureRequest.Key<T>, value: T)
    fun <T> setParameterAsync(key: CaptureRequest.Key<T>, value: T): Deferred<Unit>
    fun <T> setParameters(values: Map<CaptureRequest.Key<*>, Any>)
    fun <T> setParametersAsync(values: Map<CaptureRequest.Key<*>, Any>): Deferred<Unit>

    // 3A
    suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A>

    // Capture

    // Lifecycle
    fun close()
}

/**
 * API for interacting with a [CameraGraph] that has been configured with a set of [UseCase]'s
 */
class UseCaseCameraImpl(
    private val cameraGraph: CameraGraph,
    private val useCases: List<UseCase>,
    private val surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
    private val state: UseCaseCameraState
) : UseCaseCamera {
    private val debugId = useCaseCameraIds.incrementAndGet()
    private val currentParameters = mutableMapOf<CaptureRequest.Key<*>, Any>()

    private var _activeUseCases = setOf<UseCase>()
    override var activeUseCases: Set<UseCase>
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

    override fun close() {
        debug { "Closing $this" }
        cameraGraph.close()
    }

    override suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A> {
        return cameraGraph.acquireSession().use {
            it.setTorch(
                when (enabled) {
                    true -> TorchState.ON
                    false -> TorchState.OFF
                }
            )
        }
    }

    override fun <T> setParameter(key: CaptureRequest.Key<T>, value: T) {
        currentParameters[key] = value as Any
        state.update(parameters = currentParameters)
    }

    override fun <T> setParameterAsync(key: CaptureRequest.Key<T>, value: T): Deferred<Unit> {
        currentParameters[key] = value as Any
        return state.updateAsync(parameters = currentParameters)
    }

    override fun <T> setParameters(values: Map<CaptureRequest.Key<*>, Any>) {
        currentParameters.putAll(values)
        state.update(parameters = currentParameters)
    }

    override fun <T> setParametersAsync(values: Map<CaptureRequest.Key<*>, Any>): Deferred<Unit> {
        currentParameters.putAll(values)
        return state.updateAsync(parameters = currentParameters)
    }

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

        state.update(streams = repeatingStreamIds)
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
                threads: UseCaseThreads,
            ): UseCaseCamera {
                val streamConfigs = mutableListOf<CameraStream.Config>()
                val useCaseMap = mutableMapOf<CameraStream.Config, UseCase>()

                // TODO: This may need to combine outputs that are (or will) share the same output
                //  imageReader or surface. Right now, each UseCase gets its own [StreamConfig]
                // TODO: useCases only have a single `attachedSurfaceResolution`, yet they have a
                //  list of deferrableSurfaces.
                for (useCase in useCases) {
                    val outputConfig = CameraStream.Config.create(
                        size = useCase.attachedSurfaceResolution!!,
                        format = StreamFormat(useCase.imageFormat),
                        camera = cameraConfig.cameraId
                    )
                    streamConfigs.add(outputConfig)
                    useCaseMap[outputConfig] = useCase
                }

                // Build up a config (using TEMPLATE_PREVIEW by default)
                val config = CameraGraph.Config(
                    camera = cameraConfig.cameraId,
                    streams = streamConfigs,
                    defaultListeners = listOf(callbackMap),
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
                        val deferredSurface = deferredSurfaces.first()
                        surfaceToStreamMap[deferredSurface] = stream.id

                        Futures.addCallback(
                            deferredSurface.surface,
                            object : FutureCallback<Surface?> {
                                override fun onSuccess(result: Surface?) {
                                    debug { "Configured $result for $stream" }
                                    graph.setSurface(stream.id, result)
                                }

                                override fun onFailure(t: Throwable) {
                                    debug(t) { "Surface for $deferredSurface failed to arrive!" }
                                    graph.setSurface(stream.id, null)
                                }
                            },
                            threads.backgroundExecutor
                        )
                    }
                }

                val state = UseCaseCameraState(graph, threads)

                graph.start()
                return UseCaseCameraImpl(graph, useCases, surfaceToStreamMap, state)
            }
        }
    }
}