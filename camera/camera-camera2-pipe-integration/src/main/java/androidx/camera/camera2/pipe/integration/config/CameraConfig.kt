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
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraInfoAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraInternalAdapter
import androidx.camera.camera2.pipe.integration.compat.Camera2CameraControlCompat
import androidx.camera.camera2.pipe.integration.compat.EvCompCompat
import androidx.camera.camera2.pipe.integration.compat.ZoomCompat
import androidx.camera.camera2.pipe.integration.impl.CameraPipeCameraProperties
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.EvCompControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraThreadConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import javax.inject.Scope

@Scope
annotation class CameraScope

/** Dependency bindings for adapting an individual [CameraInternal] instance to [CameraPipe] */
@OptIn(ExperimentalCamera2Interop::class)
@Module(
    includes = [
        ZoomCompat.Bindings::class,
        ZoomControl.Bindings::class,
        EvCompCompat.Bindings::class,
        EvCompControl.Bindings::class,
        Camera2CameraControl.Bindings::class,
        Camera2CameraControlCompat.Bindings::class,
    ],
    subcomponents = [UseCaseCameraComponent::class]
)
abstract class CameraModule {
    companion object {

        @CameraScope
        @Provides
        fun provideUseCaseThreads(
            cameraConfig: CameraConfig,
            cameraThreadConfig: CameraThreadConfig
        ): UseCaseThreads {

            val executor = cameraThreadConfig.cameraExecutor
            val dispatcher = cameraThreadConfig.cameraExecutor.asCoroutineDispatcher()

            val cameraScope = CoroutineScope(
                SupervisorJob() +
                    dispatcher +
                    CoroutineName("CXCP-UseCase-${cameraConfig.cameraId.value}")
            )

            return UseCaseThreads(
                cameraScope,
                executor,
                dispatcher
            )
        }

        @Provides
        fun provideCameraMetadata(cameraPipe: CameraPipe, config: CameraConfig): CameraMetadata =
            cameraPipe.cameras().awaitMetadata(config.cameraId)
    }

    @Binds
    abstract fun bindCameraProperties(impl: CameraPipeCameraProperties): CameraProperties

    @Binds
    abstract fun bindCameraInternal(adapter: CameraInternalAdapter): CameraInternal

    @Binds
    abstract fun bindCameraInfoInternal(adapter: CameraInfoAdapter): CameraInfoInternal

    @Binds
    abstract fun bindCameraControlInternal(adapter: CameraControlAdapter): CameraControlInternal
}

/** Configuration properties used when creating a [CameraInternal] instance. */
@Module
class CameraConfig(val cameraId: CameraId) {
    @Provides
    fun provideCameraConfig(): CameraConfig = this
}

/** Dagger subcomponent for a single [CameraInternal] instance. */
@CameraScope
@Subcomponent(
    modules = [
        CameraModule::class,
        CameraConfig::class
    ]
)
interface CameraComponent {
    @Subcomponent.Builder
    interface Builder {
        fun config(config: CameraConfig): Builder
        fun build(): CameraComponent
    }

    fun getCameraInternal(): CameraInternal
}
