/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.integration.core.util

import android.util.Size
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.CompareSizesByArea
import java.util.Collections

object CameraInfoUtil {

    @JvmStatic
    fun getHighResolutionOutputSizes(cameraInfo: CameraInfo, imageFormat: Int): List<Size> =
        (cameraInfo as CameraInfoInternal).getSupportedHighResolutions(imageFormat)

    @JvmStatic
    fun getMaxHighResolutionOutputSize(cameraInfo: CameraInfo, imageFormat: Int): Size? {
        val highResolutionOutputSizes = getHighResolutionOutputSizes(cameraInfo, imageFormat)
        return if (highResolutionOutputSizes.isEmpty()) {
            null
        } else {
            Collections.max(highResolutionOutputSizes, CompareSizesByArea())
        }
    }
}
