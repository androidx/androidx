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
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CapturePipelineImpl
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraImpl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControlImpl
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.core.UseCase
import androidx.camera.core.impl.DeferrableSurface
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Scope

@Scope
annotation class UseCaseCameraScope

/** Dependency bindings for building a [UseCaseCamera] */
@Module(includes = [
    CapturePipelineImpl.Bindings::class,
    UseCaseCameraImpl.Bindings::class,
    UseCaseCameraRequestControlImpl.Bindings::class,
])
abstract class UseCaseCameraModule {
    // Used for dagger provider methods that are static.
    companion object
}

/** Dagger module for binding the [UseCase]'s to the [UseCaseCamera]. */
@Module
class UseCaseCameraConfig(
    private val useCases: List<UseCase>
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
        threads: UseCaseThreads,
    ): UseCaseGraphConfig {
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
            Log.debug {
                "Prepare config for: $it " +
                    "(${it.prescribedSize}, ${it.prescribedStreamFormat})"
            }
        }

        // Build up a config (using TEMPLATE_PREVIEW by default)
        val graph = cameraPipe.create(CameraGraph.Config(
            camera = cameraConfig.cameraId,
            streams = streamConfigMap.keys.toList(),
            defaultListeners = listOf(callbackMap, requestListener),
        ))

        val surfaceToStreamMap = mutableMapOf<DeferrableSurface, StreamId>()
        streamConfigMap.forEach { (streamConfig, deferrableSurface) ->
            graph.streams[streamConfig]?.let {
                surfaceToStreamMap[deferrableSurface] = it.id
            }
        }

        Log.debug {
            "Prepare UseCaseCameraGraphConfig: $graph "
        }

        if (adapter.isSessionConfigValid()) {
            adapter.setupSurfaceAsync(graph, surfaceToStreamMap)
        } else {
            Log.debug {
                "Unable to create capture session due to conflicting configurations"
            }
        }

        graph.start()

        return UseCaseGraphConfig(
            graph = graph,
            surfaceToStreamMap = surfaceToStreamMap,
        )
    }
}

data class UseCaseGraphConfig(
    val graph: CameraGraph,
    val surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
)

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