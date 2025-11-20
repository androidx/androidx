/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.compose

import androidx.camera.core.CameraInfo
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.ViewfinderDefaults

/**
 * Utility class for determining the appropriate [ImplementationMode] for [CameraXViewfinder].
 *
 * This class extends the compatibility logic from `viewfinder-core` by incorporating
 * camera-specific information, such as the hardware level.
 */
internal class CameraImplementationModeCompat {
    companion object {
        /**
         * Chooses a compatible [ImplementationMode] for the viewfinder based on the camera's
         * capabilities.
         *
         * In general, this method returns [ImplementationMode.EXTERNAL] as it typically offers
         * higher performance. However, it returns [ImplementationMode.EMBEDDED] under the following
         * conditions:
         * - If the camera device associated with the [cameraInfo] has a
         *   [LEGACY][android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY]
         *   hardware level.
         * - If the device is not LEGACY, it falls back to checking for other compatibility issues,
         *   including:
         *     - Being on API level 24 (Android N) or below.
         *     - Being on a device with known surface-related quirks.
         *
         * @param cameraInfo The [CameraInfo] used to determine the camera's hardware level.
         * @return The chosen [ImplementationMode].
         */
        fun chooseCompatibleMode(cameraInfo: CameraInfo): ImplementationMode {
            val isLegacyDevice =
                cameraInfo.getImplementationType() == CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY

            return if (isLegacyDevice) {
                ImplementationMode.EMBEDDED
            } else {
                // Fall back to the duplicated device-specific compatibility logic.
                return ViewfinderDefaults.implementationMode
            }
        }
    }
}
