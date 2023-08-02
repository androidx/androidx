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
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraBackendFactory
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraBackends
import androidx.camera.camera2.pipe.internal.CameraBackendsImpl
import androidx.camera.camera2.pipe.CameraContext
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraPipe.CameraMetadataConfig
import androidx.camera.camera2.pipe.compat.Camera2CameraDevices
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Threads
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Reusable
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
internal annotation class CameraPipeCameraBackend

@Singleton
@Component(
    modules = [
        CameraPipeConfigModule::class,
        CameraPipeModules::class,
        Camera2Module::class,
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
internal abstract class CameraPipeModules {
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

        @Singleton
        @Provides
        fun provideCameraContext(
            context: Context,
            threads: Threads,
            cameraBackends: CameraBackends
        ): CameraContext =
            object : CameraContext {
                override val appContext: Context = context
                override val threads: Threads = threads
                override val cameraBackends: CameraBackends = cameraBackends
            }

        @Singleton
        @Provides
        fun provideCameraBackends(
            config: CameraPipe.Config,
            @CameraPipeCameraBackend cameraPipeCameraBackend: Provider<CameraBackend>,
            appContext: Context,
            threads: Threads,
        ): CameraBackends {
            // This is intentionally lazy. If an internalBackend is defined as part of the
            // CameraPipe configuration, we will never create the default cameraPipeCameraBackend.
            val internalBackend = config.cameraBackendConfig.internalBackend
                ?: Debug.trace("Initialize cameraPipeCameraBackend") {
                    cameraPipeCameraBackend.get()
                }

            // Make sure that the list of additional backends does not contain the
            check(!config.cameraBackendConfig.cameraBackends.containsKey(internalBackend.id)) {
                "CameraBackendConfig#cameraBackends should not contain a backend with " +
                    "${internalBackend.id}. Use CameraBackendConfig#internalBackend field instead."
            }
            val allBackends: Map<CameraBackendId, CameraBackendFactory> =
                config.cameraBackendConfig.cameraBackends +
                    (internalBackend.id to CameraBackendFactory { internalBackend })

            val defaultBackendId = config.cameraBackendConfig.defaultBackend ?: internalBackend.id
            check(allBackends.containsKey(defaultBackendId)) {
                "Failed to find $defaultBackendId in the list of available CameraPipe backends! " +
                    "Available values are ${allBackends.keys}"
            }
            return CameraBackendsImpl(defaultBackendId, allBackends, appContext, threads)
        }
    }
}
