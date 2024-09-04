/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.integration.testingtestapp

import android.content.Context
import androidx.camera.integration.testingtestapp.camerax.CameraModule
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.testing.TestInstallIn
import java.util.concurrent.Executor
import javax.inject.Named

@Module
@TestInstallIn(components = [ViewModelComponent::class], replaces = [CameraModule::class])
class TestCameraModule() {

    @Provides
    fun provideLifecycleCameraController(
        @ApplicationContext context: Context
    ): LifecycleCameraController {
        // TODO: Replace with fake
        return LifecycleCameraController(context)
    }

    @Provides
    @Named("MainExecutor")
    fun provideMainExecutor(@ApplicationContext context: Context): Executor {
        // TODO: Replace if necessary
        return ContextCompat.getMainExecutor(context)
    }
}
