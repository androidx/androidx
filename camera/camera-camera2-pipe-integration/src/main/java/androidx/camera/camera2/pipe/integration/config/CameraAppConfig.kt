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
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.CameraFactory
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/** Dependency bindings for adapting a [CameraFactory] instance to [CameraPipe] */
@Module(
    subcomponents = [CameraComponent::class]
)
abstract class CameraAppModule {
    companion object {
        @Singleton
        @Provides
        fun provideCameraPipe(context: Context): CameraPipe {
            return CameraPipe(CameraPipe.Config(appContext = context.applicationContext))
        }

        @Provides
        fun provideAvailableCameraIds(cameraPipe: CameraPipe): Set<String> {
            return runBlocking { cameraPipe.cameras().ids().map { it.value }.toSet() }
        }
    }
}

/** Configuration properties that are shared across this app process */
@Module
class CameraAppConfig(
    private val context: Context,
    private val cameraThreadConfig: CameraThreadConfig
) {
    @Provides
    fun provideContext(): Context = context

    @Provides
    fun provideCameraThreadConfig(): CameraThreadConfig = cameraThreadConfig
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
    fun getAvailableCameraIds(): Set<String>

    @Component.Builder
    interface Builder {
        fun config(config: CameraAppConfig): Builder
        fun build(): CameraAppComponent
    }
}
