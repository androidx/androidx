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

package androidx.camera.camera2.pipe.impl

import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.impl.GraphController.GraphListener
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import javax.inject.Qualifier
import javax.inject.Scope

@Scope
internal annotation class CameraGraphScope

@Qualifier
internal annotation class ForCameraGraph

@CameraGraphScope
@Subcomponent(
    modules = [
        CameraGraphModules::class,
        CameraGraphConfigModule::class,
        Camera2CameraGraphModules::class,
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
    @Provides
    fun provideCameraGraphConfig(): CameraGraph.Config = config
}

@Module
internal abstract class CameraGraphModules {
    @Binds
    abstract fun bindCameraGraph(cameraGraph: CameraGraphImpl): CameraGraph

    @Binds
    abstract fun bindGraphProcessor(graphProcessor: GraphProcessorImpl): GraphProcessor

    @Binds
    abstract fun bindGraphListener(graphProcessor: GraphProcessorImpl): GraphListener

    companion object {
        @CameraGraphScope
        @Provides
        @ForCameraGraph
        fun provideCameraGraphCoroutineScope(threads: Threads): CoroutineScope {
            return CoroutineScope(threads.defaultDispatcher.plus(CoroutineName("CXCP-Graph")))
        }

        @Provides
        fun provideCameraMetadata(
            graphConfig: CameraGraph.Config,
            cameraDevices: CameraDevices
        ): CameraMetadata {
            return cameraDevices.awaitMetadata(graphConfig.camera)
        }

        @CameraGraphScope
        @Provides
        @ForCameraGraph
        fun provideRequestListeners(
            graphConfig: CameraGraph.Config,
            listener3A: Listener3A
        ): java.util.ArrayList<Request.Listener> {
            // TODO: Dagger doesn't appear to like standard kotlin lists. Replace this with a standard
            //   Kotlin list interfaces when dagger compiles with them.
            val listeners = java.util.ArrayList<Request.Listener>()
            listeners.add(listener3A)
            // Listeners in CameraGraph.Config can de defined outside of the CameraPipe library,
            // and since we iterate thought the listeners in order and invoke them, it appears
            // beneficial to add the internal listeners first and then the graph config listeners.
            listeners.addAll(graphConfig.defaultListeners)
            return listeners
        }
    }
}

@Module(
    includes = [
        SessionFactoryModule::class
    ]
)
internal abstract class Camera2CameraGraphModules {
    @Binds
    abstract fun bindRequestProcessorFactory(
        factoryStandard: StandardCamera2RequestProcessorFactory
    ): Camera2RequestProcessorFactory

    @Binds
    abstract fun bindGraphState(camera2CameraState: Camera2CameraController): GraphController
}
