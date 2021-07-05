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
import android.hardware.camera2.params.MeteringRectangle
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.TorchState
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.adapter.CaptureConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.getImplementationOptionParameters
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
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
    suspend fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>,
        afRegions: List<MeteringRectangle>,
        awbRegions: List<MeteringRectangle>
    ): Deferred<Result3A>

    // Capture
    fun capture(captureSequence: List<CaptureConfig>)

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
    private val state: UseCaseCameraState,
    private val configAdapter: CaptureConfigAdapter,
    private val threads: UseCaseThreads,
) : UseCaseCamera {
    private val debugId = useCaseCameraIds.incrementAndGet()
    private val currentParameters = mutableMapOf<CaptureRequest.Key<*>, Any>()
    private var activeSessionConfigAdapter: SessionConfigAdapter? = null

    private var _activeUseCases = setOf<UseCase>()
    override var activeUseCases: Set<UseCase>
        get() = _activeUseCases
        set(value) {
            // Note: This may be called with the same set of values that was previously set. This
            // is used as a signal to indicate the properties of the UseCase may have changed.
            _activeUseCases = value
            activeSessionConfigAdapter = SessionConfigAdapter(_activeUseCases.toList(), threads)
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

    override suspend fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>,
        afRegions: List<MeteringRectangle>,
        awbRegions: List<MeteringRectangle>
    ): Deferred<Result3A> {
        return cameraGraph.acquireSession().use {
            it.lock3A(
                aeRegions = aeRegions,
                afRegions = afRegions,
                awbRegions = awbRegions,
                afLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN
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

    override fun capture(captureSequence: List<CaptureConfig>) {
        val requests = captureSequence.map { configAdapter.mapToRequest(it) }
        state.capture(requests)
    }

    private fun updateUseCases() {
        val repeatingStreamIds = mutableSetOf<StreamId>()
        val repeatingListeners = CameraCallbackMap()

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

        activeSessionConfigAdapter?.getValidSessionConfigOrNull()?.let { sessionConfig ->
            sessionConfig.repeatingCameraCaptureCallbacks.forEach { callback ->
                repeatingListeners.addCaptureCallback(
                    callback,
                    threads.backgroundExecutor
                )
            }

            // Only update the state when the SessionConfig is valid
            state.update(
                parameters = sessionConfig.getImplementationOptionParameters(),
                streams = repeatingStreamIds,
                listeners = setOf(repeatingListeners)
            )
        } ?: run {
            debug { "Unable to reset the session due to invalid config" }
            // TODO: Consider to reset the session if there is no valid config.
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
                threads: UseCaseThreads,
            ): UseCaseCamera {
                val streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()

                // TODO: This may need to combine outputs that are (or will) share the same output
                //  imageReader or surface.
                val adapter = SessionConfigAdapter(useCases, threads)
                adapter.getValidSessionConfigOrNull()?.surfaces?.forEach {
                    val outputConfig = CameraStream.Config.create(
                        size = it.prescribedSize,
                        format = StreamFormat(it.prescribedStreamFormat),
                        camera = cameraConfig.cameraId
                    )
                    streamConfigMap[outputConfig] = it
                    debug {
                        "Prepare config for: $it " +
                            "(${it.prescribedSize}, ${it.prescribedStreamFormat})"
                    }
                }

                // Build up a config (using TEMPLATE_PREVIEW by default)
                val config = CameraGraph.Config(
                    camera = cameraConfig.cameraId,
                    streams = streamConfigMap.keys.toList(),
                    defaultListeners = listOf(callbackMap),
                )
                val graph = cameraPipe.create(config)

                val surfaceToStreamMap = mutableMapOf<DeferrableSurface, StreamId>()
                streamConfigMap.forEach { (streamConfig, deferrableSurface) ->
                    graph.streams[streamConfig]?.let {
                        surfaceToStreamMap[deferrableSurface] = it.id
                    }
                }

                if (adapter.isSessionConfigValid()) {
                    adapter.setupSurfaceAsync(graph, surfaceToStreamMap)
                } else {
                    debug { "Unable to create capture session due to conflicting configurations" }
                }

                val state = UseCaseCameraState(graph, threads)
                val configAdapter =
                    CaptureConfigAdapter(surfaceToStreamMap, threads.backgroundExecutor)

                graph.start()
                return UseCaseCameraImpl(
                    graph,
                    useCases,
                    surfaceToStreamMap,
                    state,
                    configAdapter,
                    threads
                )
            }
        }
    }
}