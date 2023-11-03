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

import android.content.Context
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.integration.impl.CameraInteropStateCallbackRepository
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/** Dependency bindings for adapting a [CameraFactory] instance to [CameraPipe] */
@Module(
    subcomponents = [CameraComponent::class]
)
abstract class CameraAppModule {
    companion object {
        @Provides
        fun provideCameraDevices(cameraPipe: CameraPipe): CameraDevices {
            return cameraPipe.cameras()
        }
    }
}

/** Configuration properties that are shared across this app process */
@Module
class CameraAppConfig(
    private val context: Context,
    private val cameraThreadConfig: CameraThreadConfig,
    private val cameraPipe: CameraPipe,
    private val camera2InteropCallbacks: CameraInteropStateCallbackRepository
) {
    @Provides
    fun provideContext(): Context = context

    @Provides
    fun provideCameraThreadConfig(): CameraThreadConfig = cameraThreadConfig

    @Provides
    fun provideCameraPipe(): CameraPipe = cameraPipe

    @Provides
    fun provideCamera2InteropCallbacks(): CameraInteropStateCallbackRepository =
        camera2InteropCallbacks
}

/** Dagger component for Application (Process) scoped dependencies. */
@Singleton
@Component(
    modules = [
        CameraAppModule::class,
        CameraAppConfig::class
    ]
)
interface CameraAppComponent {
    fun cameraBuilder(): CameraComponent.Builder
    fun getCameraPipe(): CameraPipe
    fun getCameraDevices(): CameraDevices

    @Component.Builder
    interface Builder {
        fun config(config: CameraAppConfig): Builder
        fun build(): CameraAppComponent
    }
}
