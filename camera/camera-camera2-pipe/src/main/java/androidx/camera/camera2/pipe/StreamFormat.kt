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

import androidx.annotation.RestrictTo
import androidx.camera.common.ImageFormat
import androidx.camera.common.ImageFormats

/**
 * Platform-independent Android ImageFormats and their associated values.
 *
 * Using this inline class prevents missing values on platforms where the format is not present or
 * not listed. // TODO: Consider adding data-space as a separate property, or finding a way to work
 * it in.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class StreamFormat(@ImageFormat public val value: Int) {
    public companion object {
        public val UNKNOWN: StreamFormat = StreamFormat(0)
        public val PRIVATE: StreamFormat = StreamFormat(0x22)

        public val DEPTH16: StreamFormat = StreamFormat(0x44363159)
        public val DEPTH_JPEG: StreamFormat = StreamFormat(0x69656963)
        public val DEPTH_POINT_CLOUD: StreamFormat = StreamFormat(0x101)
        public val FLEX_RGB_888: StreamFormat = StreamFormat(0x29)
        public val FLEX_RGBA_8888: StreamFormat = StreamFormat(0x2A)
        public val HEIC: StreamFormat = StreamFormat(0x48454946)
        public val JPEG: StreamFormat = StreamFormat(0x100)
        public val JPEG_R: StreamFormat = StreamFormat(0x1005)
        public val NV16: StreamFormat = StreamFormat(0x10)
        public val NV21: StreamFormat = StreamFormat(0x11)
        public val RAW10: StreamFormat = StreamFormat(0x25)
        public val RAW12: StreamFormat = StreamFormat(0x26)
        public val RAW_DEPTH: StreamFormat = StreamFormat(0x1002)
        public val RAW_DEPTH10: StreamFormat = StreamFormat(0x1003)
        public val RAW_PRIVATE: StreamFormat = StreamFormat(0x24)
        public val RAW_SENSOR: StreamFormat = StreamFormat(0x20)
        public val RGB_565: StreamFormat = StreamFormat(4)
        public val Y12: StreamFormat = StreamFormat(0x32315659)
        public val Y16: StreamFormat = StreamFormat(0x20363159)
        public val Y8: StreamFormat = StreamFormat(0x20203859)
        public val YCBCR_P010: StreamFormat = StreamFormat(0x36)
        public val YUV_420_888: StreamFormat = StreamFormat(0x23)
        public val YUV_422_888: StreamFormat = StreamFormat(0x27)
        public val YUV_444_888: StreamFormat = StreamFormat(0x28)
        public val YUY2: StreamFormat = StreamFormat(0x14)
        public val YV12: StreamFormat = StreamFormat(0x32315659)
        /**
         * BLOB is a specialized format defined in graphics.h for variable sized images, usually
         * JPEG.
         */
        public val BLOB: StreamFormat = StreamFormat(0x21)

        /** Checks to see if the format is likely a compressed rgb format. */
        @JvmStatic
        public fun isCompressedRgb(format: StreamFormat): Boolean {
            return ImageFormats.isCompressedRgb(format.value)
        }

        /**
         * Returns if image format is one of the raw type, RAW10, RAW12, RAW_PRIVATE, RAW_SENSOR,
         * RAW_DEPTH, or RAW_DEPTH10.
         */
        @JvmStatic
        public fun isRaw(format: StreamFormat): Boolean {
            return ImageFormats.isRaw(format.value)
        }

        /**
         * Estimate the number of bytes consumed per image based on the format and dimensions.
         *
         * @param format the StreamFormat.
         * @param width the width of the image.
         * @param height the height of the image.
         * @return estimated number of bytes consumed per image.
         */
        @JvmStatic
        public fun bytesPerImage(format: StreamFormat, width: Int, height: Int): Long {
            return ImageFormats.bytesPerImage(format.value, width, height)
        }
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
    public val bitsPerPixel: Int
        get() = ImageFormats.bitsPerPixel(value)

    /**
     * This function returns a human-readable string for the associated format.
     *
     * @return a human-readable string representation of the StreamFormat.
     */
    public val name: String
        get() = ImageFormats.name(value)
}
