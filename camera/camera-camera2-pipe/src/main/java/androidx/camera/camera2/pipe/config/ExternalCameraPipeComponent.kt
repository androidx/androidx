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

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraSurfaceManager
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Singleton
@Component(modules = [ExternalCameraPipeModules::class, ThreadConfigModule::class])
internal interface ExternalCameraPipeComponent {
    fun cameraGraphBuilder(): ExternalCameraGraphComponent.Builder
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Module
internal abstract class ExternalCameraPipeModules {
    companion object {
        @Singleton
        @Provides
        fun provideCameraSurfaceManager() = CameraSurfaceManager()
    }
}
