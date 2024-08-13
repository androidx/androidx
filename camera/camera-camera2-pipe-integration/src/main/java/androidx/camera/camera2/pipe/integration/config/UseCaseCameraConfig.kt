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

package androidx.camera.camera2.pipe.integration.config

import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.compat.workaround.CapturePipelineTorchCorrection
import androidx.camera.camera2.pipe.integration.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.pipe.integration.impl.CapturePipeline
import androidx.camera.camera2.pipe.integration.impl.CapturePipelineImpl
import androidx.camera.camera2.pipe.integration.impl.SessionProcessorManager
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

@Scope public annotation class UseCaseCameraScope

/** Dependency bindings for building a [UseCaseCamera] */
@Module(
    includes =
        [
            UseCaseCameraImpl.Bindings::class,
            UseCaseCameraRequestControlImpl.Bindings::class,
        ]
)
public abstract class UseCaseCameraModule {
    // Used for dagger provider methods that are static.
    public companion object {

        @UseCaseCameraScope
        @Provides
        public fun provideCapturePipeline(
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
public class UseCaseCameraConfig(
    private val useCases: List<UseCase>,
    private val sessionConfigAdapter: SessionConfigAdapter,
    private val cameraStateAdapter: CameraStateAdapter,
    private val cameraGraph: CameraGraph,
    private val streamConfigMap: Map<CameraStream.Config, DeferrableSurface>,
    private val sessionProcessorManager: SessionProcessorManager?,
) {
    @UseCaseCameraScope
    @Provides
    public fun provideUseCaseList(): java.util.ArrayList<UseCase> {
        return java.util.ArrayList(useCases)
    }

    @UseCaseCameraScope
    @Provides
    public fun provideSessionConfigAdapter(): SessionConfigAdapter {
        return sessionConfigAdapter
    }

    @UseCaseCameraScope
    @Provides
    public fun provideSessionProcessorManager(): SessionProcessorManager? {
        return sessionProcessorManager
    }

    /**
     * [UseCaseGraphConfig] would store the CameraGraph and related surface map that would be used
     * for [UseCaseCamera].
     */
    @UseCaseCameraScope
    @Provides
    public fun provideUseCaseGraphConfig(
        useCaseSurfaceManager: UseCaseSurfaceManager,
        cameraInteropStateCallbackRepository: CameraInteropStateCallbackRepository
    ): UseCaseGraphConfig {
        sessionConfigAdapter.getValidSessionConfigOrNull()?.let { sessionConfig ->
            cameraInteropStateCallbackRepository.updateCallbacks(sessionConfig)
        }

        val surfaceToStreamMap = mutableMapOf<DeferrableSurface, StreamId>()
        streamConfigMap.forEach { (streamConfig, deferrableSurface) ->
            cameraGraph.streams[streamConfig]?.let { surfaceToStreamMap[deferrableSurface] = it.id }
        }

        Log.debug { "Prepare UseCaseCameraGraphConfig: $cameraGraph " }

        if (!sessionConfigAdapter.isSessionProcessorEnabled) {
            Log.debug { "Setting up Surfaces with UseCaseSurfaceManager" }
            if (sessionConfigAdapter.isSessionConfigValid()) {
                useCaseSurfaceManager
                    .setupAsync(
                        cameraGraph,
                        sessionConfigAdapter,
                        surfaceToStreamMap,
                    )
                    .invokeOnCompletion { throwable ->
                        // Only show logs for error cases, ignore CancellationException since the
                        // task
                        // could be cancelled by UseCaseSurfaceManager#stopAsync().
                        if (throwable != null && throwable !is CancellationException) {
                            Log.error(throwable) { "Surface setup error!" }
                        }
                    }
            } else {
                Log.error { "Unable to create capture session due to conflicting configurations" }
            }
        }

        cameraGraph.start()

        return UseCaseGraphConfig(
            graph = cameraGraph,
            surfaceToStreamMap = surfaceToStreamMap,
            cameraStateAdapter = cameraStateAdapter,
        )
    }
}

public data class UseCaseGraphConfig(
    val graph: CameraGraph,
    val surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
    val cameraStateAdapter: CameraStateAdapter,
) {
    public fun getStreamIdsFromSurfaces(
        deferrableSurfaces: Collection<DeferrableSurface>
    ): Set<StreamId> {
        val streamIds = mutableSetOf<StreamId>()
        deferrableSurfaces.forEach {
            surfaceToStreamMap[it]?.let { streamId -> streamIds.add(streamId) }
        }
        return streamIds
    }
}

/** Dagger subcomponent for a single [UseCaseCamera] instance. */
@UseCaseCameraScope
@Subcomponent(modules = [UseCaseCameraModule::class, UseCaseCameraConfig::class])
public interface UseCaseCameraComponent {
    public fun getUseCaseCamera(): UseCaseCamera

    public fun getUseCaseGraphConfig(): UseCaseGraphConfig

    @Subcomponent.Builder
    public interface Builder {
        public fun config(config: UseCaseCameraConfig): Builder

        public fun build(): UseCaseCameraComponent
    }
}
