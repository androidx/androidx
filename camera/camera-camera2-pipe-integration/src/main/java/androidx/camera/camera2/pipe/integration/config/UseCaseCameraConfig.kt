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
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraImpl
import androidx.camera.core.UseCase
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Scope

@Scope
annotation class UseCaseCameraScope

/** Dependency bindings for building a [UseCaseCamera] */
@Module(includes = [UseCaseCameraImpl.Bindings::class])
abstract class UseCaseCameraModule {
    // Used for dagger provider methods that are static.
    companion object
}

/** Dagger module for binding the [UseCase]'s to the [UseCaseCamera]. */
@Module
class UseCaseCameraConfig(
    private val useCases: List<UseCase>
) {
    @UseCaseCameraScope
    @Provides
    fun provideUseCaseList(): java.util.ArrayList<UseCase> {
        return java.util.ArrayList(useCases)
    }
}

/** Dagger subcomponent for a single [UseCaseCamera] instance. */
@UseCaseCameraScope
@Subcomponent(
    modules = [
        UseCaseCameraModule::class,
        UseCaseCameraConfig::class
    ]
)
interface UseCaseCameraComponent {
    fun getUseCaseCamera(): UseCaseCamera

    @Subcomponent.Builder
    interface Builder {
        fun config(config: UseCaseCameraConfig): Builder
        fun build(): UseCaseCameraComponent
    }
}