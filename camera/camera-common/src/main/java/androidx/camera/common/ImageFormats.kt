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

package androidx.camera.common

import androidx.annotation.RestrictTo
import kotlin.math.max

/** Utility functions for working with [ImageFormat] values. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object ImageFormats {

    /** Checks to see if the format is likely a compressed rgb format. */
    @JvmStatic
    public fun isCompressedRgb(@ImageFormat format: Int): Boolean {
        return when (format) {
            android.graphics.ImageFormat.JPEG_R,
            android.graphics.ImageFormat.DEPTH_JPEG,
            android.graphics.ImageFormat.HEIC,
            IMAGE_FORMAT_HEIC_ULTRAHDR,
            android.graphics.ImageFormat.JPEG,
            android.hardware.HardwareBuffer.BLOB -> true
            else -> false
        }
    }

    /**
     * Returns if image format is one of the raw type, RAW10, RAW12, RAW_PRIVATE, RAW_SENSOR,
     * RAW_DEPTH, or RAW_DEPTH10.
     */
    @JvmStatic
    public fun isRaw(@ImageFormat format: Int): Boolean {
        return when (format) {
            android.graphics.ImageFormat.RAW10,
            android.graphics.ImageFormat.RAW12,
            android.graphics.ImageFormat.RAW_PRIVATE,
            android.graphics.ImageFormat.RAW_SENSOR,
            IMAGE_FORMAT_RAW_DEPTH,
            IMAGE_FORMAT_RAW_DEPTH10 -> true
            else -> false
        }
    }

    /**
     * Estimate the number of bytes consumed per image based on the format and dimensions.
     *
     * @param format the ImageFormat.
     * @param width the width of the image.
     * @param height the height of the image.
     * @return estimated number of bytes consumed per image.
     */
    @JvmStatic
    public fun bytesPerImage(@ImageFormat format: Int, width: Int, height: Int): Long {
        var bpp = bitsPerPixel(format)

        if (bpp <= 0 && format == android.graphics.ImageFormat.DEPTH_POINT_CLOUD) {
            // TODO: This is a temporary fix for certain devices returning depth data in a
            // different format.
            bpp = 16
        }

        // Assume that unknown formats are compressed formats, and estimate the size.
        if (bpp <= 0 && isCompressedRgb(format)) {
            return estimateCompressedRgbBytesPerImage(width, height)
        }

        if (bpp <= 0 && format == android.graphics.ImageFormat.PRIVATE) {
            bpp = estimatePrivateBitsPerPixel()
        }

        bpp = max(bpp, 0)
        val bitsPerRow = width * bpp
        return (bitsPerRow.toLong() * height) / 8L
    }

    /**
     * This makes the assumption that compressible formats are *likely* 8 bit rgb based, and that
     * the compression is equivalent or better than jpeg compression at 100 quality.
     */
    private fun estimateCompressedRgbBytesPerImage(width: Int, height: Int): Long {
        val rgbBitsPerPixel = 24

        // A few different places estimated that jpeg compression at 100 is
        // worst-case: ~3.5
        // best-case:  ~12
        // median:     ~5

        // This value is set to be near the worst-case since it's usually better to
        // over-estimate the memory usage than to under-estimate it.
        val estimatedCompressionFactor = 4

        val rgbBitsPerRow = width * rgbBitsPerPixel
        val rgbBytes = (rgbBitsPerRow.toLong() * height) / 8L
        return rgbBytes / estimatedCompressionFactor
    }

    /**
     * This makes the assumption that PRIVATE formats is same as
     * [android.graphics.ImageFormat.YUV_420_888] in some specific platforms.
     */
    private fun estimatePrivateBitsPerPixel(): Int {
        return bitsPerPixel(android.graphics.ImageFormat.YUV_420_888)
    }

    /**
     * This function returns the number of bits per pixel for a given stream format.
     *
     * @return the number of bits per pixel or -1 if the format does not have a well defined number
     *   of bits per pixel.
     */
    @JvmStatic
    public fun bitsPerPixel(@ImageFormat format: Int): Int {
        when (format) {
            android.graphics.ImageFormat.DEPTH16 -> return 16
            android.graphics.ImageFormat.FLEX_RGB_888 -> return 24
            android.graphics.ImageFormat.FLEX_RGBA_8888 -> return 32
            android.graphics.ImageFormat.NV16 -> return 16
            android.graphics.ImageFormat.NV21 -> return 12
            android.graphics.ImageFormat.RAW10 -> return 10
            android.graphics.ImageFormat.RAW12 -> return 12
            IMAGE_FORMAT_RAW_DEPTH -> return 16
            android.graphics.ImageFormat.RAW_SENSOR -> return 16
            android.graphics.ImageFormat.RGB_565 -> return 16
            android.graphics.ImageFormat.YV12 -> return 12
            IMAGE_FORMAT_Y16 -> return 16
            android.graphics.ImageFormat.Y8 -> return 8
            android.graphics.ImageFormat.YCBCR_P010 -> return 24
            IMAGE_FORMAT_YCBCR_P210 -> return 32
            IMAGE_FORMAT_R_8 -> return 8
            IMAGE_FORMAT_HSV_888 -> return 24
            IMAGE_FORMAT_R_16 -> return 16
            IMAGE_FORMAT_RG_1616 -> return 32
            IMAGE_FORMAT_RGBA_10101010 -> return 64
            android.hardware.HardwareBuffer.D_16 -> return 16
            android.hardware.HardwareBuffer.D_24 -> return 32
            android.hardware.HardwareBuffer.DS_24UI8 -> return 32
            android.hardware.HardwareBuffer.D_FP32 -> return 32
            android.hardware.HardwareBuffer.DS_FP32UI8 -> return 64
            android.hardware.HardwareBuffer.S_UI8 -> return 8
            android.graphics.ImageFormat.YUV_420_888 -> return 12
            android.graphics.ImageFormat.YUV_422_888 -> return 16
            android.graphics.ImageFormat.YUV_444_888 -> return 24
            android.graphics.ImageFormat.YUY2 -> return 16
        }

        return -1
    }

    /**
     * This function returns a human-readable string for the associated format.
     *
     * @return a human-readable string representation of the ImageFormat.
     */
    @JvmStatic
    public fun name(@ImageFormat format: Int): String {
        return when (format) {
            android.graphics.ImageFormat.UNKNOWN -> "UNKNOWN"
            android.graphics.ImageFormat.PRIVATE -> "PRIVATE"
            android.graphics.ImageFormat.DEPTH16 -> "DEPTH16"
            android.graphics.ImageFormat.DEPTH_JPEG -> "DEPTH_JPEG"
            android.graphics.ImageFormat.DEPTH_POINT_CLOUD -> "DEPTH_POINT_CLOUD"
            android.graphics.ImageFormat.FLEX_RGB_888 -> "FLEX_RGB_888"
            android.graphics.ImageFormat.FLEX_RGBA_8888 -> "FLEX_RGBA_8888"
            android.graphics.ImageFormat.HEIC -> "HEIC"
            IMAGE_FORMAT_HEIC_ULTRAHDR -> "HEIC_ULTRAHDR"
            android.graphics.ImageFormat.JPEG -> "JPEG"
            android.graphics.ImageFormat.JPEG_R -> "JPEG_R"
            android.graphics.ImageFormat.NV16 -> "NV16"
            android.graphics.ImageFormat.NV21 -> "NV21"
            android.graphics.ImageFormat.RAW10 -> "RAW10"
            android.graphics.ImageFormat.RAW12 -> "RAW12"
            IMAGE_FORMAT_RAW_DEPTH -> "RAW_DEPTH"
            IMAGE_FORMAT_RAW_DEPTH10 -> "RAW_DEPTH10"
            android.graphics.ImageFormat.RAW_PRIVATE -> "RAW_PRIVATE"
            android.graphics.ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
            android.graphics.ImageFormat.RGB_565 -> "RGB_565"
            android.graphics.ImageFormat.YV12 -> "YV12"
            IMAGE_FORMAT_Y16 -> "Y16"
            android.graphics.ImageFormat.Y8 -> "Y8"
            android.graphics.ImageFormat.YCBCR_P010 -> "YCBCR_P010"
            IMAGE_FORMAT_YCBCR_P210 -> "YCBCR_P210"
            IMAGE_FORMAT_R_8 -> "R_8"
            IMAGE_FORMAT_HSV_888 -> "HSV_888"
            IMAGE_FORMAT_R_16 -> "R_16"
            IMAGE_FORMAT_RG_1616 -> "RG_1616"
            IMAGE_FORMAT_RGBA_10101010 -> "RGBA_10101010"
            android.hardware.HardwareBuffer.D_16 -> "D_16"
            android.hardware.HardwareBuffer.D_24 -> "D_24"
            android.hardware.HardwareBuffer.DS_24UI8 -> "DS_24UI8"
            android.hardware.HardwareBuffer.D_FP32 -> "D_FP32"
            android.hardware.HardwareBuffer.DS_FP32UI8 -> "DS_FP32UI8"
            android.hardware.HardwareBuffer.S_UI8 -> "S_UI8"
            android.graphics.ImageFormat.YUV_420_888 -> "YUV_420_888"
            android.graphics.ImageFormat.YUV_422_888 -> "YUV_422_888"
            android.graphics.ImageFormat.YUV_444_888 -> "YUV_444_888"
            android.graphics.ImageFormat.YUY2 -> "YUY2"
            android.hardware.HardwareBuffer.BLOB -> "BLOB"
            android.graphics.PixelFormat.TRANSLUCENT -> "TRANSLUCENT"
            android.graphics.PixelFormat.TRANSPARENT -> "TRANSPARENT"
            android.graphics.PixelFormat.OPAQUE -> "OPAQUE"
            else -> "UNKNOWN(${format.toString(16)})"
        }
    }
}
