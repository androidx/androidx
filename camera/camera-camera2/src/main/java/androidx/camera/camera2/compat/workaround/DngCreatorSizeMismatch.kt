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

package androidx.camera.camera2.compat.workaround

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import androidx.camera.camera2.compat.quirk.DeviceQuirks
import androidx.camera.camera2.compat.quirk.DngCreatorSizeMismatchQuirk
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.core.Logger

/**
 * Workaround to filter out RAW output sizes that do not match sensor dimensions on devices with
 * [DngCreatorSizeMismatchQuirk].
 *
 * @see DngCreatorSizeMismatchQuirk
 */
public class DngCreatorSizeMismatch(private val cameraMetadata: CameraMetadata?) {
    private val tag = "DngCreatorSizeMismatch"
    private val quirk: DngCreatorSizeMismatchQuirk? =
        DeviceQuirks[DngCreatorSizeMismatchQuirk::class.java]

    /**
     * Filters out RAW output sizes that do not match the sensor pixel array size or pre-correction
     * active array size on affected devices.
     *
     * @param sizeList the original sizes list which must be a mutable list
     * @param format the image format to apply the workaround
     */
    public fun filterRawSizes(sizeList: MutableList<Size>, format: Int) {
        if (quirk == null || !isRawFormat(format) || cameraMetadata == null) {
            return
        }
        val pixelArraySize = cameraMetadata.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val preCorrectionArrayRect =
            cameraMetadata.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE)

        if (pixelArraySize == null && preCorrectionArrayRect == null) {
            return
        }

        // Keep only sizes that match sensor pixel array size or pre-correction active array size
        val iterator = sizeList.iterator()
        while (iterator.hasNext()) {
            val size = iterator.next()
            val matchesPixelArray =
                pixelArraySize != null &&
                    isSizeMatch(size, pixelArraySize.width, pixelArraySize.height)
            val matchesPreCorrectionArray =
                preCorrectionArrayRect != null &&
                    isSizeMatch(
                        size,
                        preCorrectionArrayRect.width(),
                        preCorrectionArrayRect.height(),
                    )
            if (!matchesPixelArray && !matchesPreCorrectionArray) {
                Logger.d(
                    tag,
                    "Excluding RAW size $size because it does not match sensor pixel array size " +
                        "$pixelArraySize or pre-correction active array size " +
                        "$preCorrectionArrayRect",
                )
                iterator.remove()
            }
        }
    }

    private fun isRawFormat(format: Int): Boolean {
        return format == ImageFormat.RAW_SENSOR ||
            format == ImageFormat.RAW10 ||
            format == ImageFormat.RAW12 ||
            format == ImageFormat.RAW_PRIVATE
    }

    private fun isSizeMatch(size: Size, targetWidth: Int, targetHeight: Int): Boolean {
        return (size.width == targetWidth && size.height == targetHeight) ||
            (size.width == targetHeight && size.height == targetWidth)
    }
}
