/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.adapter

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.testing.FakeCameraExtensionMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 31)
class CameraExtensionCapabilitiesAdapterTest {
    private val characteristics: Map<CameraCharacteristics.Key<*>, Any?> =
        mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK)
    private val captureOutputSizes = mapOf(ImageFormat.JPEG to setOf(Size(1920, 1080)))
    private val previewOutputSizes: Map<Class<*>, Set<Size>> =
        mapOf(SurfaceTexture::class.java to setOf(Size(1280, 720)))
    private val postviewSizes =
        mapOf(ImageFormat.YUV_420_888 to mapOf(Size(1920, 1080) to setOf(Size(640, 480))))
    private val latencyRange =
        mapOf(ImageFormat.JPEG to mapOf(Size(1920, 1080) to Range(100L, 500L)))
    private val requestKeys =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setOf(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.EXTENSION_STRENGTH)
        } else {
            setOf(CaptureRequest.CONTROL_AF_MODE)
        }
    private val resultKeys =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setOf(CaptureResult.CONTROL_AF_STATE, CaptureResult.EXTENSION_CURRENT_TYPE)
        } else {
            setOf(CaptureResult.CONTROL_AF_STATE)
        }

    private val extensionMetadata =
        FakeCameraExtensionMetadata(
            camera = CameraId("0"),
            cameraExtension = 0,
            isPostviewSupported = true,
            isCaptureProgressSupported = true,
            characteristics = characteristics,
            captureOutputSizes = captureOutputSizes,
            previewOutputSizes = previewOutputSizes,
            postviewSizes = postviewSizes,
            estimatedCaptureLatencyRangeMillis = latencyRange,
            requestKeys = requestKeys,
            resultKeys = resultKeys,
        )
    private val adapter = CameraExtensionCapabilitiesAdapter(extensionMetadata)

    @Test
    fun get_returnsCorrectValue() {
        assertThat(adapter.get<Int>(CameraCharacteristics.LENS_FACING))
            .isEqualTo(CameraCharacteristics.LENS_FACING_BACK)
    }

    @Test
    @Config(minSdk = 34)
    fun isExtensionStrengthSupported_returnsCorrectValue() {
        assertThat(adapter.isExtensionStrengthSupported()).isTrue()
    }

    @Test
    @Config(minSdk = 34)
    fun isCurrentExtensionModeSupported_returnsCorrectValue() {
        assertThat(adapter.isCurrentExtensionModeSupported()).isTrue()
    }

    @Test
    fun isPostviewSupported_returnsCorrectValue() {
        assertThat(adapter.isPostviewSupported()).isTrue()
    }

    @Test
    fun isCaptureProcessProgressSupported_returnsCorrectValue() {
        assertThat(adapter.isCaptureProcessProgressSupported()).isTrue()
    }

    @Test
    fun getOutputSizes_byFormat_returnsCorrectValue() {
        assertThat(adapter.getOutputSizes(ImageFormat.JPEG))
            .isEqualTo(captureOutputSizes[ImageFormat.JPEG])
    }

    @Test
    fun getOutputSizes_byClass_returnsCorrectValue() {
        assertThat(adapter.getOutputSizes(SurfaceTexture::class.java))
            .isEqualTo(previewOutputSizes[SurfaceTexture::class.java])
    }

    @Test
    fun getPostviewSizes_returnsCorrectValue() {
        assertThat(adapter.getPostviewSizes(Size(1920, 1080), ImageFormat.YUV_420_888))
            .isEqualTo(postviewSizes[ImageFormat.YUV_420_888]!![Size(1920, 1080)])
    }

    @Test
    fun getEstimatedCaptureLatencyRangeMillis_returnsCorrectValue() {
        assertThat(
                adapter.getEstimatedCaptureLatencyRangeMillis(Size(1920, 1080), ImageFormat.JPEG)
            )
            .isEqualTo(latencyRange[ImageFormat.JPEG]?.get(Size(1920, 1080)))
    }

    @Test
    fun getAvailableCaptureRequestKeys_returnsCorrectValue() {
        assertThat(adapter.getAvailableCaptureRequestKeys()).isEqualTo(requestKeys)
    }

    @Test
    fun getAvailableCaptureResultKeys_returnsCorrectValue() {
        assertThat(adapter.getAvailableCaptureResultKeys()).isEqualTo(resultKeys)
    }

    @Test
    fun getAvailableCharacteristicsKeyValues_returnsCorrectValue() {
        val expected = characteristics.toList()
        assertThat(adapter.getAvailableCharacteristicsKeyValues())
            .containsExactlyElementsIn(expected)
    }
}
