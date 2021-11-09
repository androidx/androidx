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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.config

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraPipe.CameraMetadataConfig
import androidx.camera.camera2.pipe.compat.Camera2CameraDevices
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Reusable
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CameraPipeConfigModule::class,
        Camera2CameraPipeModules::class,
    ]
)
internal interface CameraPipeComponent {
    fun cameraGraphComponentBuilder(): CameraGraphComponent.Builder
    fun cameras(): CameraDevices
}

@Module(
    includes = [ThreadConfigModule::class],
    subcomponents = [CameraGraphComponent::class]
)
internal class CameraPipeConfigModule(private val config: CameraPipe.Config) {
    @Provides
    fun provideCameraPipeConfig(): CameraPipe.Config = config
}

@Module
internal abstract class Camera2CameraPipeModules {
    @Binds
    abstract fun bindCameras(impl: Camera2CameraDevices): CameraDevices

    companion object {
        @Provides
        fun provideContext(config: CameraPipe.Config): Context = config.appContext

        @Provides
        fun provideCameraMetadataConfig(config: CameraPipe.Config): CameraMetadataConfig =
            config.cameraMetadataConfig

        @Reusable
        @Provides
        fun provideCameraManager(context: Context): CameraManager =
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
}
