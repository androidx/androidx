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

package androidx.camera.camera2.pipe.integration.config

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter.Companion.toCamera2ImplConfig
import androidx.camera.camera2.pipe.integration.compat.workaround.CapturePipelineTorchCorrection
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.pipe.integration.impl.CapturePipeline
import androidx.camera.camera2.pipe.integration.impl.CapturePipelineImpl
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraImpl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControlImpl
import androidx.camera.camera2.pipe.integration.impl.UseCaseSurfaceManager
import androidx.camera.core.UseCase
import androidx.camera.core.impl.DeferrableSurface
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import java.util.concurrent.CancellationException
import javax.inject.Scope

@Scope
annotation class UseCaseCameraScope

/** Dependency bindings for building a [UseCaseCamera] */
@Module(
    includes = [
        UseCaseCameraImpl.Bindings::class,
        UseCaseCameraRequestControlImpl.Bindings::class,
    ]
)
abstract class UseCaseCameraModule {
    // Used for dagger provider methods that are static.
    companion object {

        @UseCaseCameraScope
        @Provides
        fun provideCapturePipeline(
            capturePipelineImpl: CapturePipelineImpl,
            capturePipelineTorchCorrection: CapturePipelineTorchCorrection
        ): CapturePipeline {
            if (CapturePipelineTorchCorrection.isEnabled) {
                return capturePipelineTorchCorrection
            }

            return capturePipelineImpl
        }
    }
}

/** Dagger module for binding the [UseCase]'s to the [UseCaseCamera]. */
@Module
class UseCaseCameraConfig(
    private val useCases: List<UseCase>,
    private val cameraStateAdapter: CameraStateAdapter,
    private val cameraGraphFlags: CameraGraph.Flags,
) {
    @UseCaseCameraScope
    @Provides
    fun provideUseCaseList(): java.util.ArrayList<UseCase> {
        return java.util.ArrayList(useCases)
    }

    /**
     * [UseCaseGraphConfig] would store the CameraGraph and related surface map that would
     * be used for [UseCaseCamera].
     */
    @UseCaseCameraScope
    @Provides
    fun provideUseCaseGraphConfig(
        callbackMap: CameraCallbackMap,
        cameraConfig: CameraConfig,
        cameraPipe: CameraPipe,
        requestListener: ComboRequestListener,
        useCaseSurfaceManager: UseCaseSurfaceManager,
        cameraInteropStateCallbackRepository: CameraInteropStateCallbackRepository
    ): UseCaseGraphConfig {
        val streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()

        // TODO: This may need to combine outputs that are (or will) share the same output
        //  imageReader or surface.
        val sessionConfigAdapter = SessionConfigAdapter(useCases)
        sessionConfigAdapter.getValidSessionConfigOrNull()?.let { sessionConfig ->
            cameraInteropStateCallbackRepository.updateCallbacks(sessionConfig)
            sessionConfig.surfaces.forEach { deferrableSurface ->
                val outputConfig = CameraStream.Config.create(
                    streamUseCase = getStreamUseCase(
                        deferrableSurface,
                        sessionConfigAdapter.surfaceToStreamUseCaseMap
                    ),
                    streamUseHint = getStreamUseHint(
                        deferrableSurface,
                        sessionConfigAdapter.surfaceToStreamUseHintMap
                    ),
                    size = deferrableSurface.prescribedSize,
                    format = StreamFormat(deferrableSurface.prescribedStreamFormat),
                    camera = CameraId(
                        sessionConfig.toCamera2ImplConfig().getPhysicalCameraId(
                            cameraConfig.cameraId.value
                        )!!
                    )
                )
                streamConfigMap[outputConfig] = deferrableSurface
                Log.debug {
                    "Prepare config for: $deferrableSurface (${deferrableSurface.prescribedSize}," +
                        " ${deferrableSurface.prescribedStreamFormat})"
                }
            }
        }

        // Build up a config (using TEMPLATE_PREVIEW by default)
        // TODO(b/277310425): Turn off CameraGraph.Flags.quirkFinalizeSessionOnCloseBehavior when
        //  it's not needed. This should be needed only when all use cases are detached (with
        //  VideoCapture) on devices where Surfaces cannot be released immediately.
        val graph = cameraPipe.create(
            CameraGraph.Config(
                camera = cameraConfig.cameraId,
                streams = streamConfigMap.keys.toList(),
                defaultListeners = listOf(callbackMap, requestListener),
                flags = cameraGraphFlags,
            )
        )

        val surfaceToStreamMap = mutableMapOf<DeferrableSurface, StreamId>()
        streamConfigMap.forEach { (streamConfig, deferrableSurface) ->
            graph.streams[streamConfig]?.let {
                surfaceToStreamMap[deferrableSurface] = it.id
            }
        }

        Log.debug {
            "Prepare UseCaseCameraGraphConfig: $graph "
        }

        if (sessionConfigAdapter.isSessionConfigValid()) {
            useCaseSurfaceManager.setupAsync(graph, sessionConfigAdapter, surfaceToStreamMap)
                .invokeOnCompletion { throwable ->
                    // Only show logs for error cases, ignore CancellationException since the task
                    // could be cancelled by UseCaseSurfaceManager#stopAsync().
                    if (throwable != null && throwable !is CancellationException) {
                        Log.error(throwable) { "Surface setup error!" }
                    }
                }
        } else {
            Log.error {
                "Unable to create capture session due to conflicting configurations"
            }
        }

        graph.start()

        return UseCaseGraphConfig(
            graph = graph,
            surfaceToStreamMap = surfaceToStreamMap,
            cameraStateAdapter = cameraStateAdapter,
        )
    }

    private fun getStreamUseCase(
        deferrableSurface: DeferrableSurface,
        mapping: Map<DeferrableSurface, Long>
    ): OutputStream.StreamUseCase? {
        return mapping[deferrableSurface]?.let { OutputStream.StreamUseCase(it) }
    }

    private fun getStreamUseHint(
        deferrableSurface: DeferrableSurface,
        mapping: Map<DeferrableSurface, Long>
    ): OutputStream.StreamUseHint? {
        return mapping[deferrableSurface]?.let { OutputStream.StreamUseHint(it) }
    }
}

data class UseCaseGraphConfig(
    val graph: CameraGraph,
    val surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
    val cameraStateAdapter: CameraStateAdapter,
) {
    fun getStreamIdsFromSurfaces(
        deferrableSurfaces: Collection<DeferrableSurface>
    ): Set<StreamId> {
        val streamIds = mutableSetOf<StreamId>()
        deferrableSurfaces.forEach {
            surfaceToStreamMap[it]?.let { streamId ->
                streamIds.add(streamId)
            }
        }
        return streamIds
    }
}

/** Dagger subcomponent for a single [UseCaseCamera] instance. */
@UseCaseCameraScope
@Subcomponent(
    modules = [
        UseCaseCameraModule::class,
        UseCaseCameraConfig::class
    ]
)
interface UseCaseCameraComponent {
    fun getUseCaseCamera(): UseCaseCamera

    @Subcomponent.Builder
    interface Builder {
        fun config(config: UseCaseCameraConfig): Builder
        fun build(): UseCaseCameraComponent
    }
}