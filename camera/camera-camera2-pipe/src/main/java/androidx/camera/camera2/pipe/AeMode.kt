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

package androidx.camera.camera2.pipe

import android.hardware.camera2.CameraMetadata

/**
 * An enum to match the CameraMetadata.CONTROL_AE_MODE_* constants.
 */
enum class AeMode(val value: Int) {
    OFF(CameraMetadata.CONTROL_AE_MODE_OFF),
    ON(CameraMetadata.CONTROL_AE_MODE_ON),
    ON_AUTO_FLASH(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH),
    ON_ALWAYS_FLASH(CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH),
    ON_AUTO_FLASH_REDEYE(CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);

    companion object {
        @JvmStatic
        fun fromIntOrNull(value: Int) = values().firstOrNull { it.value == value }
    }
}