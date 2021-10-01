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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import dagger.Module
import dagger.Provides
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Deferred

internal val useCaseCameraIds = atomic(0)
internal val defaultOptionPriority = Config.OptionPriority.OPTIONAL
internal const val defaultTemplate = CameraDevice.TEMPLATE_PREVIEW

interface UseCaseCamera {
    // UseCases
    var activeUseCases: Set<UseCase>

    // RequestControl of the UseCaseCamera
    val requestControl: UseCaseCameraRequestControl

    // Parameters
    fun <T> setParameterAsync(
        key: CaptureRequest.Key<T>,
        value: T,
        priority: Config.OptionPriority = defaultOptionPriority,
    ): Deferred<Unit>

    fun setParametersAsync(
        values: Map<CaptureRequest.Key<*>, Any>,
        priority: Config.OptionPriority = defaultOptionPriority,
    ): Deferred<Unit>

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
    private val threads: UseCaseThreads,
    override val requestControl: UseCaseCameraRequestControl,
) : UseCaseCamera {
    private val debugId = useCaseCameraIds.incrementAndGet()

    override var activeUseCases = setOf<UseCase>()
        set(value) {
            field = value
            // Note: This may be called with the same set of values that was previously set. This
            // is used as a signal to indicate the properties of the UseCase may have changed.
            SessionConfigAdapter(value, threads).getValidSessionConfigOrNull()?.let {
                requestControl.setSessionConfigAsync(it)
            } ?: run {
                debug { "Unable to reset the session due to invalid config" }
                requestControl.setSessionConfigAsync(
                    SessionConfig.Builder().apply {
                        setTemplateType(defaultTemplate)
                    }.build()
                )
            }
        }

    init {
        debug { "Configured $this for $useCases" }
    }

    override fun close() {
        debug { "Closing $this" }
        cameraGraph.close()
    }

    override suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A> =
        requestControl.setTorchAsync(enabled)

    override suspend fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>,
        afRegions: List<MeteringRectangle>,
        awbRegions: List<MeteringRectangle>
    ): Deferred<Result3A> =
        requestControl.startFocusAndMeteringAsync(aeRegions, afRegions, awbRegions)

    override fun <T> setParameterAsync(
        key: CaptureRequest.Key<T>,
        value: T,
        priority: Config.OptionPriority,
    ): Deferred<Unit> = setParametersAsync(mapOf(key to (value as Any)), priority)

    override fun setParametersAsync(
        values: Map<CaptureRequest.Key<*>, Any>,
        priority: Config.OptionPriority,
    ): Deferred<Unit> = requestControl.appendParametersAsync(
        values = values,
        optionPriority = priority
    )

    override fun capture(captureSequence: List<CaptureConfig>) =
        requestControl.issueSingleCapture(captureSequence)

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
                requestListener: ComboRequestListener,
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
                    defaultListeners = listOf(callbackMap, requestListener),
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

                val requestControl = UseCaseCameraRequestControlImpl(
                    graph,
                    surfaceToStreamMap,
                    threads
                )

                graph.start()
                return UseCaseCameraImpl(
                    graph,
                    useCases,
                    threads,
                    requestControl,
                )
            }
        }
    }
}