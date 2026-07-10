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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class StreamFormatTest {
    @Test
    fun streamFormatsAreEqual() {
        assertThat(StreamFormat(0x23)).isEqualTo(StreamFormat.YUV_420_888)
    }

    @Test
    fun streamFormatsHaveNames() {
        assertThat(StreamFormat(0x23).name).isEqualTo("YUV_420_888")
        assertThat(StreamFormat.RAW10.name).isEqualTo("RAW10")
        assertThat(StreamFormat.BLOB.name).isEqualTo("BLOB")
    }

    @Test
    fun streamFormatsHaveToString() {
        assertThat(StreamFormat(0x23).toString()).contains("YUV_420_888")
        assertThat(StreamFormat(0x23).toString()).contains("StreamFormat")
        assertThat(StreamFormat.RAW10.toString()).contains("RAW10")
        assertThat(StreamFormat.RAW10.toString()).contains("StreamFormat")
    }

    @Test
    fun streamFormatsHaveBitsPerPixel() {
        assertThat(StreamFormat(0x23).bitsPerPixel).isEqualTo(12)
        assertThat(StreamFormat.RAW10.bitsPerPixel).isEqualTo(10)
    }

    @Test
    fun isCompressedRgb_identifiesCompressedFormats() {
        assertThat(StreamFormat.isCompressedRgb(StreamFormat.JPEG)).isTrue()
        assertThat(StreamFormat.isCompressedRgb(StreamFormat.HEIC)).isTrue()
        assertThat(StreamFormat.isCompressedRgb(StreamFormat.BLOB)).isTrue()

        // Negative cases
        assertThat(StreamFormat.isCompressedRgb(StreamFormat.YUV_420_888)).isFalse()
        assertThat(StreamFormat.isCompressedRgb(StreamFormat.RAW10)).isFalse()
    }

    @Test
    fun isRaw_identifiesRawFormats() {
        assertThat(StreamFormat.isRaw(StreamFormat.RAW10)).isTrue()
        assertThat(StreamFormat.isRaw(StreamFormat.RAW_SENSOR)).isTrue()
        assertThat(StreamFormat.isRaw(StreamFormat.RAW_DEPTH10)).isTrue()

        // Negative cases
        assertThat(StreamFormat.isRaw(StreamFormat.JPEG)).isFalse()
        assertThat(StreamFormat.isRaw(StreamFormat.YUV_420_888)).isFalse()
    }

    @Test
    fun bytesPerImage_calculatesForStandardFormats() {
        val width = 100
        val height = 100

        // YUV_420_888 has 12 bpp.
        // Formula: (width * bpp * height) / 8 -> (100 * 12 * 100) / 8 = 15000
        assertThat(StreamFormat.bytesPerImage(StreamFormat.YUV_420_888, width, height))
            .isEqualTo(15000L)

        // RAW10 has 10 bpp.
        // Formula: (100 * 10 * 100) / 8 = 12500
        assertThat(StreamFormat.bytesPerImage(StreamFormat.RAW10, width, height)).isEqualTo(12500L)
    }

    @Test
    fun bytesPerImage_estimatesForCompressedFormats() {
        val width = 100
        val height = 100

        // Compressed formats use the rgbBitsPerPixel (24) and compression factor (4)
        // RGB bytes: (100 * 24 * 100) / 8 = 30000
        // Estimated final: 30000 / 4 = 7500
        assertThat(StreamFormat.bytesPerImage(StreamFormat.JPEG, width, height)).isEqualTo(7500L)
        assertThat(StreamFormat.bytesPerImage(StreamFormat.BLOB, width, height)).isEqualTo(7500L)
    }

    @Test
    fun bytesPerImage_estimatesForPrivateFormat() {
        val width = 100
        val height = 100

        // PRIVATE falls back to YUV_420_888 bpp (12)
        // Formula: (100 * 12 * 100) / 8 = 15000
        assertThat(StreamFormat.bytesPerImage(StreamFormat.PRIVATE, width, height))
            .isEqualTo(15000L)
    }
}
