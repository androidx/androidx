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

package androidx.camera.camera2.compat

import android.graphics.ImageFormat
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.compat.quirk.DeviceQuirks
import androidx.camera.camera2.compat.quirk.PixelJpegRSupportedQuirk

@RequiresApi(34)
internal class StreamConfigurationMapCompatApi34Impl(map: StreamConfigurationMap?) :
    StreamConfigurationMapCompatBaseImpl(map) {

    // Use `get()` to avoid storing a Boolean
    private val hasJpegRQuirk: Boolean
        get() = DeviceQuirks[PixelJpegRSupportedQuirk::class.java] != null

    override fun getOutputFormats(): Array<Int>? {
        val formats = super.getOutputFormats()

        if (hasJpegRQuirk) {
            return formats?.filter { it != ImageFormat.JPEG_R }?.toTypedArray()
        }

        return formats
    }

    override fun getOutputSizes(format: Int): Array<Size>? {
        // b/436119518: Return null if the device has the color tone issue and the format is JPEG_R.
        if (format == ImageFormat.JPEG_R && hasJpegRQuirk) {
            return null
        }
        return super.getOutputSizes(format)
    }

    override fun getHighResolutionOutputSizes(format: Int): Array<Size>? {
        // b/436119518: Return null if the device has the color tone issue and the format is JPEG_R.
        if (format == ImageFormat.JPEG_R && hasJpegRQuirk) {
            return null
        }
        return super.getHighResolutionOutputSizes(format)
    }

    override fun getOutputMinFrameDuration(format: Int, size: Size): Long {
        // b/436119518: Return 0 if the device has the color tone issue and the format is JPEG_R.
        if (format == ImageFormat.JPEG_R && hasJpegRQuirk) {
            return 0
        }

        return super.getOutputMinFrameDuration(format, size)
    }
}
