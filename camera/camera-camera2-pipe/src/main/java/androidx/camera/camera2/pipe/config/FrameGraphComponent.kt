/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.framegraph.FrameGraphImpl
import androidx.camera.camera2.pipe.internal.FrameDistributor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Qualifier
import javax.inject.Scope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

@Scope internal annotation class FrameGraphScope

@Qualifier internal annotation class FrameGraphCoroutineScope

@FrameGraphScope
@Subcomponent(modules = [FrameGraphModule::class, FrameGraphConfigModule::class])
internal interface FrameGraphComponent {

    fun frameGraph(): FrameGraph

    @Subcomponent.Builder
    interface Builder {

        fun frameGraphConfigModule(config: FrameGraphConfigModule): Builder

        fun build(): FrameGraphComponent
    }
}

@Module
internal class FrameGraphConfigModule(
    private val cameraGraphComponent: CameraGraphComponent,
    private val config: FrameGraph.Config,
) {
    @Provides fun provideCameraGraphConfig(): FrameGraph.Config = config

    @Provides fun provideCameraGraph(): CameraGraph = cameraGraphComponent.cameraGraph()

    @Provides
    fun provideFrameDistributor(): FrameDistributor = cameraGraphComponent.frameDistributor()
}

@Module
internal abstract class FrameGraphModule {
    @Binds abstract fun bindFrameGraph(frameGraph: FrameGraphImpl): FrameGraph

    companion object {
        @FrameGraphScope
        @Provides
        @FrameGraphCoroutineScope
        fun provideFrameGraphCoroutineScope(
            threads: Threads,
            @CameraPipeJob cameraPipeJob: Job,
        ): CoroutineScope {
            return CoroutineScope(
                SupervisorJob(cameraPipeJob) +
                    threads.lightweightDispatcher.plus(CoroutineName("CXCP-FrameGraph"))
            )
        }
    }
}
