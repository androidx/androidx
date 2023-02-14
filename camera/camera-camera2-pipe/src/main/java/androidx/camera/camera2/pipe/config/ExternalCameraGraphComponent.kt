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

// TODO(b/200306659): Remove and replace with annotation on package-info.java
@file:Suppress("DEPRECATION")
@file:RequiresApi(21)

package androidx.camera.camera2.pipe.config

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.RequestProcessor
import androidx.camera.camera2.pipe.compat.ExternalCameraController
import androidx.camera.camera2.pipe.graph.GraphListener
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@CameraGraphScope
@Subcomponent(modules = [SharedCameraGraphModules::class, ExternalCameraGraphConfigModule::class])
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

    @CameraGraphScope
    @Provides
    fun provideGraphController(graphListener: GraphListener): CameraController =
        ExternalCameraController(config, graphListener, requestProcessor)
}
