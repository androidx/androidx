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

package androidx.camera.camera2.pipe.integration.config

import android.content.Context
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.integration.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.pipe.integration.impl.DisplayInfoManager
import androidx.camera.core.CameraXConfig
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/** Dependency bindings for adapting a [CameraFactory] instance to [CameraPipe] */
@Module(subcomponents = [CameraComponent::class])
public abstract class CameraAppModule {
    public companion object {
        @Provides
        public fun provideCameraDevices(cameraPipe: CameraPipe): CameraDevices {
            return cameraPipe.cameras()
        }
    }
}

/** Configuration properties that are shared across this app process */
@Module
public class CameraAppConfig(
    private val context: Context,
    private val cameraThreadConfig: CameraThreadConfig,
    private val cameraPipe: CameraPipe,
    private val camera2InteropCallbacks: CameraInteropStateCallbackRepository,
    private val cameraCoordinator: CameraCoordinator,
    private val cameraXConfig: CameraXConfig,
) {
    @Provides public fun provideContext(): Context = context

    @Provides public fun provideCameraThreadConfig(): CameraThreadConfig = cameraThreadConfig

    @Provides public fun provideCameraPipe(): CameraPipe = cameraPipe

    @Provides
    public fun provideCamera2InteropCallbacks(): CameraInteropStateCallbackRepository =
        camera2InteropCallbacks

    @Provides public fun provideCameraCoordinator(): CameraCoordinator = cameraCoordinator

    @Provides public fun provideCameraXConfig(): CameraXConfig = cameraXConfig

    @Provides
    public fun provideDisplayInfoManager(context: Context): DisplayInfoManager {
        return DisplayInfoManager.getInstance(context)
    }
}

/** Dagger component for Application (Process) scoped dependencies. */
@Singleton
@Component(modules = [CameraAppModule::class, CameraAppConfig::class])
public interface CameraAppComponent {
    public fun cameraBuilder(): CameraComponent.Builder

    public fun getCameraPipe(): CameraPipe

    public fun getCameraDevices(): CameraDevices

    @Component.Builder
    public interface Builder {
        public fun config(config: CameraAppConfig): Builder

        public fun build(): CameraAppComponent
    }
}
