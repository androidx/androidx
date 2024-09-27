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

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraBackendFactory
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraBackends
import androidx.camera.camera2.pipe.CameraContext
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraPipe.CameraMetadataConfig
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.compat.AndroidDevicePolicyManagerWrapper
import androidx.camera.camera2.pipe.compat.AudioRestrictionController
import androidx.camera.camera2.pipe.compat.AudioRestrictionControllerImpl
import androidx.camera.camera2.pipe.compat.DevicePolicyManagerWrapper
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.internal.CameraBackendsImpl
import androidx.camera.camera2.pipe.internal.CameraDevicesImpl
import androidx.camera.camera2.pipe.media.ImageReaderImageSources
import androidx.camera.camera2.pipe.media.ImageSources
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Reusable
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier internal annotation class DefaultCameraBackend

/** Qualifier for requesting the CameraPipe scoped Context object */
@Qualifier internal annotation class CameraPipeContext

@Qualifier internal annotation class ForGraphLifecycleManager

@Singleton
@Component(
    modules =
        [
            CameraPipeConfigModule::class,
            CameraPipeModules::class,
            Camera2Module::class,
        ]
)
internal interface CameraPipeComponent {
    fun cameraGraphComponentBuilder(): CameraGraphComponent.Builder

    fun cameras(): CameraDevices

    fun cameraBackends(): CameraBackends

    fun cameraSurfaceManager(): CameraSurfaceManager

    fun cameraAudioRestrictionController(): AudioRestrictionController
}

@Module(includes = [ThreadConfigModule::class], subcomponents = [CameraGraphComponent::class])
internal class CameraPipeConfigModule(private val config: CameraPipe.Config) {
    @Provides fun provideCameraPipeConfig(): CameraPipe.Config = config

    @Provides
    fun provideCameraInteropConfig(
        cameraPipeConfig: CameraPipe.Config
    ): CameraPipe.CameraInteropConfig {
        return cameraPipeConfig.cameraInteropConfig
    }
}

@Module
internal abstract class CameraPipeModules {
    @Binds abstract fun bindCameras(impl: CameraDevicesImpl): CameraDevices

    @Binds abstract fun bindTimeSource(timeSource: SystemTimeSource): TimeSource

    companion object {
        @Provides
        @CameraPipeContext
        fun provideContext(config: CameraPipe.Config): Context = config.appContext

        @Provides
        fun provideCameraMetadataConfig(config: CameraPipe.Config): CameraMetadataConfig =
            config.cameraMetadataConfig

        @Reusable
        @Provides
        fun provideCameraManager(@CameraPipeContext cameraPipeContext: Context): CameraManager =
            cameraPipeContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        @Reusable
        @Provides
        fun provideDevicePolicyManagerWrapper(
            @CameraPipeContext cameraPipeContext: Context
        ): DevicePolicyManagerWrapper {
            val devicePolicyService =
                cameraPipeContext.getSystemService(Context.DEVICE_POLICY_SERVICE)
            return AndroidDevicePolicyManagerWrapper(devicePolicyService as DevicePolicyManager)
        }

        @Singleton
        @Provides
        fun provideCameraContext(
            @CameraPipeContext cameraPipeContext: Context,
            threads: Threads,
            cameraBackends: CameraBackends
        ): CameraContext =
            object : CameraContext {
                override val appContext: Context = cameraPipeContext
                override val threads: Threads = threads
                override val cameraBackends: CameraBackends = cameraBackends
            }

        @Singleton
        @Provides
        fun provideCameraBackends(
            config: CameraPipe.Config,
            @DefaultCameraBackend defaultCameraBackend: Provider<CameraBackend>,
            @CameraPipeContext cameraPipeContext: Context,
            threads: Threads,
        ): CameraBackends {
            // This is intentionally lazy. If an internalBackend is defined as part of the
            // CameraPipe configuration, we will never create the default cameraPipeCameraBackend.
            val internalBackend =
                config.cameraBackendConfig.internalBackend
                    ?: Debug.trace("Initialize defaultCameraBackend") { defaultCameraBackend.get() }

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
            return CameraBackendsImpl(defaultBackendId, allBackends, cameraPipeContext, threads)
        }

        @Provides
        fun configureImageSources(
            imageReaderImageSources: ImageReaderImageSources,
            cameraPipeConfig: CameraPipe.Config
        ): ImageSources {
            if (cameraPipeConfig.imageSources != null) {
                return cameraPipeConfig.imageSources
            }
            return imageReaderImageSources
        }

        @Singleton @Provides fun provideCameraSurfaceManager() = CameraSurfaceManager()

        @Singleton
        @Provides
        fun provideAudioRestrictionController() = AudioRestrictionControllerImpl()
    }
}
