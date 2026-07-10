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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class ImageFormatsTest {

    @Test
    fun imageFormatsHaveNames() {
        assertThat(ImageFormats.name(android.graphics.ImageFormat.YUV_420_888))
            .isEqualTo("YUV_420_888")
        assertThat(ImageFormats.name(android.graphics.ImageFormat.RAW10)).isEqualTo("RAW10")
        assertThat(ImageFormats.name(android.hardware.HardwareBuffer.BLOB)).isEqualTo("BLOB")
        assertThat(ImageFormats.name(android.graphics.ImageFormat.JPEG_R)).isEqualTo("JPEG_R")
        assertThat(ImageFormats.name(IMAGE_FORMAT_RAW_DEPTH)).isEqualTo("RAW_DEPTH")
        assertThat(ImageFormats.name(IMAGE_FORMAT_RAW_DEPTH10)).isEqualTo("RAW_DEPTH10")
        assertThat(ImageFormats.name(IMAGE_FORMAT_Y16)).isEqualTo("Y16")
        assertThat(ImageFormats.name(IMAGE_FORMAT_YCBCR_P210)).isEqualTo("YCBCR_P210")
        assertThat(ImageFormats.name(IMAGE_FORMAT_HEIC_ULTRAHDR)).isEqualTo("HEIC_ULTRAHDR")
        assertThat(ImageFormats.name(IMAGE_FORMAT_R_8)).isEqualTo("R_8")
        assertThat(ImageFormats.name(IMAGE_FORMAT_HSV_888)).isEqualTo("HSV_888")
        assertThat(ImageFormats.name(IMAGE_FORMAT_R_16)).isEqualTo("R_16")
        assertThat(ImageFormats.name(IMAGE_FORMAT_RG_1616)).isEqualTo("RG_1616")
        assertThat(ImageFormats.name(IMAGE_FORMAT_RGBA_10101010)).isEqualTo("RGBA_10101010")
        assertThat(ImageFormats.name(android.hardware.HardwareBuffer.D_16)).isEqualTo("D_16")
        assertThat(ImageFormats.name(android.graphics.PixelFormat.TRANSLUCENT))
            .isEqualTo("TRANSLUCENT")
    }

    @Test
    fun imageFormatsHaveBitsPerPixel() {
        assertThat(ImageFormats.bitsPerPixel(android.graphics.ImageFormat.YUV_420_888))
            .isEqualTo(12)
        assertThat(ImageFormats.bitsPerPixel(android.graphics.ImageFormat.RAW10)).isEqualTo(10)
        assertThat(ImageFormats.bitsPerPixel(IMAGE_FORMAT_Y16)).isEqualTo(16)
        assertThat(ImageFormats.bitsPerPixel(android.graphics.ImageFormat.YCBCR_P010)).isEqualTo(24)
        assertThat(ImageFormats.bitsPerPixel(IMAGE_FORMAT_YCBCR_P210)).isEqualTo(32)
        assertThat(ImageFormats.bitsPerPixel(IMAGE_FORMAT_R_8)).isEqualTo(8)
        assertThat(ImageFormats.bitsPerPixel(IMAGE_FORMAT_HSV_888)).isEqualTo(24)
        assertThat(ImageFormats.bitsPerPixel(IMAGE_FORMAT_R_16)).isEqualTo(16)
        assertThat(ImageFormats.bitsPerPixel(IMAGE_FORMAT_RG_1616)).isEqualTo(32)
        assertThat(ImageFormats.bitsPerPixel(IMAGE_FORMAT_RGBA_10101010)).isEqualTo(64)
        assertThat(ImageFormats.bitsPerPixel(android.hardware.HardwareBuffer.D_16)).isEqualTo(16)
        assertThat(ImageFormats.bitsPerPixel(android.hardware.HardwareBuffer.D_24)).isEqualTo(32)
        assertThat(ImageFormats.bitsPerPixel(android.hardware.HardwareBuffer.DS_FP32UI8))
            .isEqualTo(64)
    }

    @Test
    fun isCompressedRgb_identifiesCompressedFormats() {
        assertThat(ImageFormats.isCompressedRgb(android.graphics.ImageFormat.JPEG)).isTrue()
        assertThat(ImageFormats.isCompressedRgb(android.graphics.ImageFormat.HEIC)).isTrue()
        assertThat(ImageFormats.isCompressedRgb(IMAGE_FORMAT_HEIC_ULTRAHDR)).isTrue()
        assertThat(ImageFormats.isCompressedRgb(android.hardware.HardwareBuffer.BLOB)).isTrue()
        assertThat(ImageFormats.isCompressedRgb(android.graphics.ImageFormat.JPEG_R)).isTrue()

        // Negative cases
        assertThat(ImageFormats.isCompressedRgb(android.graphics.ImageFormat.YUV_420_888)).isFalse()
        assertThat(ImageFormats.isCompressedRgb(android.graphics.ImageFormat.RAW10)).isFalse()
    }

    @Test
    fun isRaw_identifiesRawFormats() {
        assertThat(ImageFormats.isRaw(android.graphics.ImageFormat.RAW10)).isTrue()
        assertThat(ImageFormats.isRaw(android.graphics.ImageFormat.RAW_SENSOR)).isTrue()
        assertThat(ImageFormats.isRaw(IMAGE_FORMAT_RAW_DEPTH10)).isTrue()
        assertThat(ImageFormats.isRaw(IMAGE_FORMAT_RAW_DEPTH)).isTrue()

        // Negative cases
        assertThat(ImageFormats.isRaw(android.graphics.ImageFormat.JPEG)).isFalse()
        assertThat(ImageFormats.isRaw(android.graphics.ImageFormat.YUV_420_888)).isFalse()
    }

    @Test
    fun bytesPerImage_calculatesForStandardFormats() {
        val width = 100
        val height = 100

        // YUV_420_888 has 12 bpp.
        // Formula: (width * bpp * height) / 8 -> (100 * 12 * 100) / 8 = 15000
        assertThat(
                ImageFormats.bytesPerImage(android.graphics.ImageFormat.YUV_420_888, width, height)
            )
            .isEqualTo(15000L)

        // RAW10 has 10 bpp.
        // Formula: (100 * 10 * 100) / 8 = 12500
        assertThat(ImageFormats.bytesPerImage(android.graphics.ImageFormat.RAW10, width, height))
            .isEqualTo(12500L)

        // YCBCR_P010 has 24 bpp.
        // Formula: (100 * 24 * 100) / 8 = 30000
        assertThat(
                ImageFormats.bytesPerImage(android.graphics.ImageFormat.YCBCR_P010, width, height)
            )
            .isEqualTo(30000L)

        // YCBCR_P210 has 32 bpp.
        // Formula: (100 * 32 * 100) / 8 = 40000
        assertThat(ImageFormats.bytesPerImage(IMAGE_FORMAT_YCBCR_P210, width, height))
            .isEqualTo(40000L)
    }

    @Test
    fun bytesPerImage_estimatesForCompressedFormats() {
        val width = 100
        val height = 100

        // Compressed formats use the rgbBitsPerPixel (24) and compression factor (4)
        // RGB bytes: (100 * 24 * 100) / 8 = 30000
        // Estimated final: 30000 / 4 = 7500
        assertThat(ImageFormats.bytesPerImage(android.graphics.ImageFormat.JPEG, width, height))
            .isEqualTo(7500L)
        assertThat(ImageFormats.bytesPerImage(android.hardware.HardwareBuffer.BLOB, width, height))
            .isEqualTo(7500L)
        assertThat(ImageFormats.bytesPerImage(android.graphics.ImageFormat.JPEG_R, width, height))
            .isEqualTo(7500L)
    }

    @Test
    fun bytesPerImage_estimatesForPrivateFormat() {
        val width = 100
        val height = 100

        // PRIVATE falls back to YUV_420_888 bpp (12)
        // Formula: (100 * 12 * 100) / 8 = 15000
        assertThat(ImageFormats.bytesPerImage(android.graphics.ImageFormat.PRIVATE, width, height))
            .isEqualTo(15000L)
    }
}
