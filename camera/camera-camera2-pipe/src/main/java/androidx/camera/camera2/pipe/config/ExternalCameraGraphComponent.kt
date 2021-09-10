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

import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.RequestProcessor
import androidx.camera.camera2.pipe.compat.CameraController
import androidx.camera.camera2.pipe.graph.GraphListener
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.atomicfu.atomic

@CameraGraphScope
@Subcomponent(
    modules = [
        CameraGraphModules::class,
        ExternalCameraGraphConfigModule::class
    ]
)
internal interface ExternalCameraGraphComponent {
    fun cameraGraph(): CameraGraph

    @Subcomponent.Builder
    interface Builder {
        fun externalCameraGraphConfigModule(config: ExternalCameraGraphConfigModule): Builder
        fun build(): ExternalCameraGraphComponent
    }
}

@Module
internal class ExternalCameraGraphConfigModule(
    private val config: CameraGraph.Config,
    private val cameraMetadata: CameraMetadata,
    private val requestProcessor: RequestProcessor
) {
    @Provides
    fun provideCameraGraphConfig(): CameraGraph.Config = config

    @Provides
    fun provideCameraMetadata(): CameraMetadata = cameraMetadata

    @Provides
    fun provideGraphController(graphListener: GraphListener): CameraController =
        object : CameraController {
            var started = atomic(false)
            override fun start() {
                if (started.compareAndSet(expect = false, update = true)) {
                    graphListener.onGraphStarted(requestProcessor)
                }
            }

            override fun stop() {
                if (started.compareAndSet(expect = true, update = false)) {
                    graphListener.onGraphStopped(requestProcessor)
                }
            }

            override fun restart() {
                stop()
                start()
            }
        }
}
