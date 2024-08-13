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

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.integration.adapter.CameraFactoryProvider
import androidx.camera.camera2.pipe.integration.adapter.CameraSurfaceAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraUseCaseAdapter
import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.CameraThreadConfig

/** Convenience class for generating a pre-populated CameraPipe based [CameraXConfig]. */
public class CameraPipeConfig private constructor() {
    public companion object {
        /** Creates a [CameraXConfig] containing a default CameraPipe implementation for CameraX. */
        @JvmStatic
        public fun defaultConfig(): CameraXConfig {
            return from()
        }

        /** Creates a [CameraXConfig] using a pre-existing [CameraPipe] instance. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun from(
            sharedCameraPipe: CameraPipe? = null,
            sharedAppContext: Context? = null,
            sharedThreadConfig: CameraThreadConfig? = null
        ): CameraXConfig {
            val cameraFactoryProvider =
                CameraFactoryProvider(sharedCameraPipe, sharedAppContext, sharedThreadConfig)
            return CameraXConfig.Builder()
                .setCameraFactoryProvider(cameraFactoryProvider)
                .setDeviceSurfaceManagerProvider(::CameraSurfaceAdapter)
                .setUseCaseConfigFactoryProvider(::CameraUseCaseAdapter)
                .build()
        }
    }
}
