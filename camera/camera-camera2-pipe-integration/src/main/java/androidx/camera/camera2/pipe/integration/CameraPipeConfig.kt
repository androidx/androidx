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
package androidx.camera.camera2.pipe.integration

import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.ExtendableUseCaseConfigFactory
import androidx.camera.camera2.pipe.integration.impl.CameraPipeCameraFactory
import androidx.camera.camera2.pipe.integration.impl.CameraPipeDeviceSurfaceManager

/**
 * Convenience class for generating a pre-populated CameraPipe [CameraXConfig].
 */
object CameraPipeConfig {
    /**
     * Creates a [CameraXConfig] containing the default CameraPipe implementation for CameraX.
     */
    fun defaultConfig(): CameraXConfig {
        return CameraXConfig.Builder()
            .setCameraFactoryProvider(::CameraPipeCameraFactory)
            .setDeviceSurfaceManagerProvider(::CameraPipeDeviceSurfaceManager)
            .setUseCaseConfigFactoryProvider { ExtendableUseCaseConfigFactory() }
            .build()
    }

    // TODO: Add CameraPipeConfig.Builder for passing options to CameraPipe
}