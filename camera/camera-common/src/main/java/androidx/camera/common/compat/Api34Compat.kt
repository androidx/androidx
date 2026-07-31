/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common.compat

import android.graphics.ColorSpace
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics as Camera2CameraExtensionCharacteristics
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.common.CameraCharacteristicsMetadata
import androidx.camera.common.ColorSpaceProfilesWrapper

@RequiresApi(34)
internal object Api34Compat {
    @JvmStatic fun getBt2020Hlg(): ColorSpace.Named = ColorSpace.Named.BT2020_HLG

    @JvmStatic fun getBt2020Pq(): ColorSpace.Named = ColorSpace.Named.BT2020_PQ

    @JvmStatic
    fun getColorSpaceProfiles(wrapper: CameraCharacteristicsMetadata): ColorSpaceProfilesWrapper? {
        return wrapper[CameraCharacteristics.REQUEST_AVAILABLE_COLOR_SPACE_PROFILES]?.let {
            AndroidColorSpaceProfiles(it)
        }
    }

    @JvmStatic
    fun getPostviewSupportedSizes(
        chars: Camera2CameraExtensionCharacteristics,
        extension: Int,
        captureSize: Size,
        format: Int,
    ): List<Size> = chars.getPostviewSupportedSizes(extension, captureSize, format)

    @JvmStatic
    fun isPostviewAvailable(chars: Camera2CameraExtensionCharacteristics, extension: Int): Boolean =
        chars.isPostviewAvailable(extension)

    @JvmStatic
    fun isCaptureProcessProgressAvailable(
        chars: Camera2CameraExtensionCharacteristics,
        extension: Int,
    ): Boolean = chars.isCaptureProcessProgressAvailable(extension)
}
