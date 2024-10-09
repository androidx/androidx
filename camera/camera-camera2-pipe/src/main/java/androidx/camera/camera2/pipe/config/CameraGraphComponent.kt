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

package androidx.camera.camera2.pipe.config

import android.content.Context
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraBackends
import androidx.camera.camera2.pipe.CameraContext
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.graph.CameraGraphImpl
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.GraphProcessor
import androidx.camera.camera2.pipe.graph.GraphProcessorImpl
import androidx.camera.camera2.pipe.graph.Listener3A
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.graph.SurfaceGraph
import androidx.camera.camera2.pipe.internal.CameraGraphParametersImpl
import androidx.camera.camera2.pipe.internal.FrameCaptureQueue
import androidx.camera.camera2.pipe.internal.FrameDistributor
import androidx.camera.camera2.pipe.internal.ImageSourceMap
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Qualifier
import javax.inject.Scope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope

@Scope internal annotation class CameraGraphScope

@Qualifier internal annotation class ForCameraGraph

@Qualifier internal annotation class CameraGraphContext

@CameraGraphScope
@Subcomponent(
    modules =
        [
            SharedCameraGraphModules::class,
            InternalCameraGraphModules::class,
            CameraGraphConfigModule::class,
        ]
)
internal interface CameraGraphComponent {
    fun cameraGraph(): CameraGraph

    @Subcomponent.Builder
    interface Builder {
        fun cameraGraphConfigModule(config: CameraGraphConfigModule): Builder

        fun build(): CameraGraphComponent
    }
}

@Module
internal class CameraGraphConfigModule(private val config: CameraGraph.Config) {
    @Provides fun provideCameraGraphConfig(): CameraGraph.Config = config
}

@Module
internal abstract class SharedCameraGraphModules {
    @Binds abstract fun bindCameraGraph(cameraGraph: CameraGraphImpl): CameraGraph

    @Binds abstract fun bindGraphProcessor(graphProcessor: GraphProcessorImpl): GraphProcessor

    @Binds abstract fun bindGraphListener(graphProcessor: GraphProcessorImpl): GraphListener

    @Binds
    @CameraGraphContext
    abstract fun bindCameraGraphContext(@CameraPipeContext cameraPipeContext: Context): Context

    @Binds abstract fun bindStreamGraph(streamGraph: StreamGraphImpl): StreamGraph

    @Binds
    abstract fun bindCameraGraphParameters(
        parameters: CameraGraphParametersImpl
    ): CameraGraph.Parameters

    companion object {
        @CameraGraphScope
        @Provides
        fun provideCameraGraphId(): CameraGraphId {
            return CameraGraphId.nextId()
        }

        @CameraGraphScope
        @Provides
        @ForCameraGraph
        fun provideCameraGraphCoroutineScope(threads: Threads): CoroutineScope {
            return CoroutineScope(threads.lightweightDispatcher.plus(CoroutineName("CXCP-Graph")))
        }

        @CameraGraphScope
        @Provides
        @ForCameraGraph
        fun provideRequestListeners(
            graphConfig: CameraGraph.Config,
            listener3A: Listener3A,
            frameDistributor: FrameDistributor
        ): List<@JvmSuppressWildcards Request.Listener> {
            val listeners = mutableListOf<Request.Listener>(listener3A)

            // Order slightly matters, add internal listeners first, and external listeners second.
            listeners.add(listener3A)

            // FrameDistributor is responsible for all image grouping and distribution.
            listeners.add(frameDistributor)

            // Listeners in CameraGraph.Config can de defined outside of the CameraPipe library,
            // and since we iterate thought the listeners in order and invoke them, it appears
            // beneficial to add the internal listeners first and then the graph config listeners.
            listeners.addAll(graphConfig.defaultListeners)
            return listeners
        }

        @CameraGraphScope
        @Provides
        fun provideSurfaceGraph(
            streamGraphImpl: StreamGraphImpl,
            cameraController: CameraController,
            cameraSurfaceManager: CameraSurfaceManager,
            imageSourceMap: ImageSourceMap,
            graphConfig: CameraGraph.Config,
        ): SurfaceGraph {
            return SurfaceGraph(
                streamGraphImpl,
                cameraController,
                cameraSurfaceManager,
                imageSourceMap.imageSources,
                graphConfig.flags,
            )
        }

        @CameraGraphScope
        @Provides
        fun provideFrameDistributor(
            imageSourceMap: ImageSourceMap,
            frameCaptureQueue: FrameCaptureQueue
        ): FrameDistributor {
            return FrameDistributor(imageSourceMap.imageSources, frameCaptureQueue) {}
        }
    }
}

@Module
internal abstract class InternalCameraGraphModules {
    companion object {
        @CameraGraphScope
        @Provides
        fun provideCameraBackend(
            cameraBackends: CameraBackends,
            graphConfig: CameraGraph.Config,
            cameraContext: CameraContext
        ): CameraBackend {
            val customCameraBackend = graphConfig.customCameraBackend
            if (customCameraBackend != null) {
                return customCameraBackend.create(cameraContext)
            }

            val cameraBackendId = graphConfig.cameraBackendId
            if (cameraBackendId != null) {
                return checkNotNull(cameraBackends[cameraBackendId]) {
                    "Failed to initialize $cameraBackendId from $graphConfig"
                }
            }
            return cameraBackends.default
        }

        @CameraGraphScope
        @Provides
        fun provideCameraMetadata(
            graphConfig: CameraGraph.Config,
            cameraBackend: CameraBackend
        ): CameraMetadata {
            // TODO: It might be a good idea to cache and go through caches for some of these calls
            //   instead of reading it directly from the backend.
            return checkNotNull(cameraBackend.awaitCameraMetadata(graphConfig.camera)) {
                "Failed to load metadata for ${graphConfig.camera}!"
            }
        }

        @CameraGraphScope
        @Provides
        fun provideCameraController(
            graphId: CameraGraphId,
            graphConfig: CameraGraph.Config,
            cameraBackend: CameraBackend,
            cameraContext: CameraContext,
            graphProcessor: GraphProcessorImpl,
            streamGraph: StreamGraph,
        ): CameraController {
            return cameraBackend.createCameraController(
                cameraContext,
                graphId,
                graphConfig,
                graphProcessor,
                streamGraph
            )
        }
    }
}
