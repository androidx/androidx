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

import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.compat.AudioRestrictionController
import androidx.camera.camera2.pipe.compat.AudioRestrictionControllerImpl
import androidx.camera.camera2.pipe.media.ImageReaderImageSources
import androidx.camera.camera2.pipe.media.ImageSources
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Singleton
@Component(modules = [ExternalCameraPipeModules::class, ThreadConfigModule::class])
internal interface ExternalCameraPipeComponent {
    fun cameraGraphBuilder(): ExternalCameraGraphComponent.Builder
}

@Module
internal abstract class ExternalCameraPipeModules {
    companion object {
        @Singleton @Provides fun provideCameraSurfaceManager() = CameraSurfaceManager()
    }

    @Binds
    abstract fun bindAudioRestrictionController(
        audioRestrictionController: AudioRestrictionControllerImpl
    ): AudioRestrictionController

    @Binds
    abstract fun bindImageSources(imageReaderImageSources: ImageReaderImageSources): ImageSources
}
