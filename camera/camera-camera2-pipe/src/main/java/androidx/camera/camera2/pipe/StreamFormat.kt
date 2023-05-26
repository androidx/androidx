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

import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * Platform-independent Android ImageFormats and their associated values.
 *
 * Using this inline class prevents missing values on platforms where the format is not present or
 * not listed. // TODO: Consider adding data-space as a separate property, or finding a way to work
 * it in.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@JvmInline
value class StreamFormat(val value: Int) {
    companion object {
        val UNKNOWN: StreamFormat = StreamFormat(0)
        val PRIVATE: StreamFormat = StreamFormat(0x22)

        val DEPTH16: StreamFormat = StreamFormat(0x44363159)
        val DEPTH_JPEG: StreamFormat = StreamFormat(0x69656963)
        val DEPTH_POINT_CLOUD: StreamFormat = StreamFormat(0x101)
        val FLEX_RGB_888: StreamFormat = StreamFormat(0x29)
        val FLEX_RGBA_8888: StreamFormat = StreamFormat(0x2A)
        val HEIC: StreamFormat = StreamFormat(0x48454946)
        val JPEG: StreamFormat = StreamFormat(0x100)
        val NV16: StreamFormat = StreamFormat(0x10)
        val NV21: StreamFormat = StreamFormat(0x11)
        val RAW10: StreamFormat = StreamFormat(0x25)
        val RAW12: StreamFormat = StreamFormat(0x26)
        val RAW_DEPTH: StreamFormat = StreamFormat(0x1002)
        val RAW_PRIVATE: StreamFormat = StreamFormat(0x24)
        val RAW_SENSOR: StreamFormat = StreamFormat(0x20)
        val RGB_565: StreamFormat = StreamFormat(4)
        val Y12: StreamFormat = StreamFormat(0x32315659)
        val Y16: StreamFormat = StreamFormat(0x20363159)
        val Y8: StreamFormat = StreamFormat(0x20203859)
        val YUV_420_888: StreamFormat = StreamFormat(0x23)
        val YUV_422_888: StreamFormat = StreamFormat(0x27)
        val YUV_444_888: StreamFormat = StreamFormat(0x28)
        val YUY2: StreamFormat = StreamFormat(0x14)
        val YV12: StreamFormat = StreamFormat(0x32315659)
    }

    override fun toString(): String {
        return "StreamFormat($name)"
    }

    /**
     * This function returns the number of bits per pixel for a given stream format.
     *
     * @return the number of bits per pixel or -1 if the format does not have a well defined number
     *   of bits per pixel.
     */
    val bitsPerPixel: Int
        get() {
            when (this) {
                DEPTH16 -> return 16
                FLEX_RGB_888 -> return 24
                FLEX_RGBA_8888 -> return 32
                NV16 -> return 16
                NV21 -> return 12
                RAW10 -> return 10
                RAW12 -> return 12
                RAW_DEPTH -> return 16
                RAW_SENSOR -> return 16
                RGB_565 -> return 16
                Y12 -> return 12
                Y16 -> return 16
                Y8 -> return 8
                YUV_420_888 -> return 12
                YUV_422_888 -> return 16
                YUV_444_888 -> return 24
                YUY2 -> return 16
                YV12 -> return 12
            }

            return -1
        }

    /**
     * This function returns a human readable string for the associated format.
     *
     * @return a human readable string representation of the StreamFormat.
     */
    val name: String
        get() {
            when (this) {
                UNKNOWN -> return "UNKNOWN"
                PRIVATE -> return "PRIVATE"
                DEPTH16 -> return "DEPTH16"
                DEPTH_JPEG -> return "DEPTH_JPEG"
                DEPTH_POINT_CLOUD -> return "DEPTH_POINT_CLOUD"
                FLEX_RGB_888 -> return "FLEX_RGB_888"
                FLEX_RGBA_8888 -> return "FLEX_RGBA_8888"
                HEIC -> return "HEIC"
                JPEG -> return "JPEG"
                NV16 -> return "NV16"
                NV21 -> return "NV21"
                RAW10 -> return "RAW10"
                RAW12 -> return "RAW12"
                RAW_DEPTH -> return "RAW_DEPTH"
                RAW_PRIVATE -> return "RAW_PRIVATE"
                RAW_SENSOR -> return "RAW_SENSOR"
                RGB_565 -> return "RGB_565"
                Y12 -> return "Y12"
                Y16 -> return "Y16"
                Y8 -> return "Y8"
                YUV_420_888 -> return "YUV_420_888"
                YUV_422_888 -> return "YUV_422_888"
                YUV_444_888 -> return "YUV_444_888"
                YUY2 -> return "YUY2"
                YV12 -> return "YV12"
            }
            return "UNKNOWN-${this.value.toString(16)}"
        }
}
