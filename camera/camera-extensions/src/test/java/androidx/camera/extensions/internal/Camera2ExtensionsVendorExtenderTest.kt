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

package androidx.camera.extensions.internal

import android.graphics.ImageFormat
import android.hardware.camera2.CameraExtensionCharacteristics
import android.os.Build
import android.util.Size
import androidx.camera.extensions.ExtensionMode
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraExtensionCapabilities
import androidx.camera.testing.impl.fakes.FakeCameraExtensionCapabilities.CaptureConfig
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.TIRAMISU)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class Camera2ExtensionsVendorExtenderTest {

    private val jpegSizes = setOf(Size(1920, 1080))
    private val yuvSizes = setOf(Size(1280, 720))
    private val jpegRSizes = setOf(Size(3840, 2160))
    private val captureOutputSizes =
        mapOf(
            ImageFormat.JPEG to jpegSizes,
            ImageFormat.YUV_420_888 to yuvSizes,
            ImageFormat.JPEG_R to jpegRSizes,
        )
    private val postviewCaptureSize = Size(4000, 3000)
    private val postviewSizes =
        mapOf(
            CaptureConfig(postviewCaptureSize, ImageFormat.JPEG) to jpegSizes,
            CaptureConfig(postviewCaptureSize, ImageFormat.YUV_420_888) to yuvSizes,
            CaptureConfig(postviewCaptureSize, ImageFormat.JPEG_R) to jpegRSizes,
        )

    @Test
    @Config(minSdk = Build.VERSION_CODES.TIRAMISU, maxSdk = Build.VERSION_CODES.TIRAMISU)
    fun getSupportedCaptureOutputResolutions_api33_doesNotQueryJpegR() {
        val extender = createInitializedExtender(outputSizesFormat = captureOutputSizes)

        val formats = extender.supportedCaptureOutputResolutions.map { it.first }

        assertThat(formats).containsExactly(ImageFormat.JPEG, ImageFormat.YUV_420_888)
    }

    @Test
    @Config(
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    )
    fun getSupportedCaptureOutputResolutions_api34_doesNotQueryJpegR() {
        // The API 34 framework throws "Unsupported format: 4101" when JPEG_R is queried.
        val extender = createInitializedExtender(outputSizesFormat = captureOutputSizes)

        val formats = extender.supportedCaptureOutputResolutions.map { it.first }

        assertThat(formats).containsExactly(ImageFormat.JPEG, ImageFormat.YUV_420_888)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getSupportedCaptureOutputResolutions_api35_queriesJpegR() {
        val extender = createInitializedExtender(outputSizesFormat = captureOutputSizes)

        val formats = extender.supportedCaptureOutputResolutions.map { it.first }

        assertThat(formats)
            .containsExactly(ImageFormat.JPEG, ImageFormat.YUV_420_888, ImageFormat.JPEG_R)
    }

    @Test
    @Config(
        minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    )
    fun getSupportedPostviewResolutions_api34_doesNotQueryJpegR() {
        val extender = createInitializedExtender(postviewSizes = postviewSizes)

        val formats = extender.getSupportedPostviewResolutions(postviewCaptureSize).keys

        assertThat(formats).containsExactly(ImageFormat.JPEG, ImageFormat.YUV_420_888)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getSupportedPostviewResolutions_api35_queriesJpegR() {
        val extender = createInitializedExtender(postviewSizes = postviewSizes)

        val formats = extender.getSupportedPostviewResolutions(postviewCaptureSize).keys

        assertThat(formats)
            .containsExactly(ImageFormat.JPEG, ImageFormat.YUV_420_888, ImageFormat.JPEG_R)
    }

    private fun createInitializedExtender(
        outputSizesFormat: Map<Int, Set<Size>> = emptyMap(),
        postviewSizes: Map<CaptureConfig, Set<Size>> = emptyMap(),
    ): Camera2ExtensionsVendorExtender {
        val capabilities =
            FakeCameraExtensionCapabilities(
                outputSizesFormat = outputSizesFormat,
                postviewSizes = postviewSizes,
            )
        val cameraInfo =
            FakeCameraInfoInternal("0").apply {
                setSupportedExtensions(setOf(CameraExtensionCharacteristics.EXTENSION_NIGHT))
                setCameraExtensionCapabilities(
                    CameraExtensionCharacteristics.EXTENSION_NIGHT,
                    capabilities,
                )
            }
        return Camera2ExtensionsVendorExtender(ExtensionMode.NIGHT).apply { init(cameraInfo) }
    }
}
