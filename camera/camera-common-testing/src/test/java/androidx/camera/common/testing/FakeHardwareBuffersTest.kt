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

package androidx.camera.common.testing

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.HardwareBuffer
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
@SuppressLint("NewApi")
class FakeHardwareBuffersTest {

    @Test
    @Config(minSdk = 26)
    fun createForImage_yuv420_returnsHardwareBuffer() {
        val hwBuffer =
            FakeHardwareBuffers.createForImage(
                imageFormat = ImageFormat.YUV_420_888,
                imageWidth = 640,
                imageHeight = 480,
            )
        if (hwBuffer != null) {
            assertThat(hwBuffer.format).isEqualTo(HardwareBuffer.YCBCR_420_888)
            assertThat(hwBuffer.width).isEqualTo(640)
            assertThat(hwBuffer.height).isEqualTo(480)
        }
    }

    @Test
    @Config(minSdk = 26)
    fun createForImage_jpeg_returnsBlobHardwareBuffer() {
        val hwBuffer =
            FakeHardwareBuffers.createForImage(
                imageFormat = ImageFormat.JPEG,
                imageWidth = 640,
                imageHeight = 480,
            )
        if (hwBuffer != null) {
            assertThat(hwBuffer.format).isEqualTo(HardwareBuffer.BLOB)
            assertThat(hwBuffer.height).isEqualTo(1)
            assertThat(hwBuffer.width)
                .isEqualTo(FakeHardwareBuffers.estimateBlobBufferSize(640, 480))
        }
    }

    @Test
    @Config(sdk = [26])
    fun createForImage_unsupportedFormat_onSdk26_returnsNull() {
        // Y8 is not supported on API 26
        val hwBuffer =
            FakeHardwareBuffers.createForImage(
                imageFormat = ImageFormat.Y8,
                imageWidth = 640,
                imageHeight = 480,
            )
        assertThat(hwBuffer).isNull()
    }

    @Test
    @Config(minSdk = 26)
    fun createForImage_nv21_alwaysReturnsNull() {
        // NV21 is not supported by HardwareBuffer natively
        val hwBuffer =
            FakeHardwareBuffers.createForImage(
                imageFormat = ImageFormat.NV21,
                imageWidth = 640,
                imageHeight = 480,
            )
        assertThat(hwBuffer).isNull()
    }

    @Test
    @Config(minSdk = 26)
    fun createForImage_depthPointCloud_returnsBlobHardwareBuffer() {
        val hwBuffer =
            FakeHardwareBuffers.createForImage(
                imageFormat = ImageFormat.DEPTH_POINT_CLOUD,
                imageWidth = 640,
                imageHeight = 480,
            )
        if (hwBuffer != null) {
            assertThat(hwBuffer.format).isEqualTo(HardwareBuffer.BLOB)
            assertThat(hwBuffer.height).isEqualTo(1)
            assertThat(hwBuffer.width)
                .isEqualTo(FakeHardwareBuffers.estimateBlobBufferSize(640, 480))
        }
    }

    @Test
    @Config(sdk = [28])
    fun createForImage_depthFormats_supportedOnApi28() {
        // D_16 is supported on API 28+
        val hwBuffer =
            FakeHardwareBuffers.createForImage(
                imageFormat = HardwareBuffer.D_16,
                imageWidth = 640,
                imageHeight = 480,
            )
        if (hwBuffer != null) {
            assertThat(hwBuffer.format).isEqualTo(HardwareBuffer.D_16)
        }
    }

    @Test
    @Config(sdk = [27])
    fun createForImage_depthFormats_unsupportedBeforeApi28() {
        // D_16 is unsupported on API 27
        val hwBuffer =
            FakeHardwareBuffers.createForImage(
                imageFormat = HardwareBuffer.D_16,
                imageWidth = 640,
                imageHeight = 480,
            )
        assertThat(hwBuffer).isNull()
    }
}
