/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.integration.adapter.CameraUseCaseAdapter
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeviceProperties
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 26)
class ImageCaptureOptionUnpackerTest {

    private val unpacker = CameraUseCaseAdapter.ImageCaptureOptionUnpacker.INSTANCE

    @Test
    fun unpackWithoutCaptureMode() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder().useCaseConfig
        setDeviceProperty(PROPERTIES_PIXEL_2_API26)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(CaptureRequest.CONTROL_ENABLE_ZSL, null)
        ).isNull()
    }

    @Test
    fun unpackWithValidPixel2AndMinLatency() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .useCaseConfig
        setDeviceProperty(PROPERTIES_PIXEL_2_API26)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(
                CaptureRequest.CONTROL_ENABLE_ZSL, null
            )
        ).isEqualTo(false)
    }

    @Test
    fun unpackWithValidPixel2AndMaxQuality() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .useCaseConfig
        setDeviceProperty(PROPERTIES_PIXEL_2_API26)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(
                CaptureRequest.CONTROL_ENABLE_ZSL, null
            )
        ).isEqualTo(true)
    }

    @Test
    fun unpackWithPixel2NotSupportApiLevelAndMinLatency() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .useCaseConfig
        setDeviceProperty(PROPERTIES_PIXEL_2_NOT_SUPPORT_API)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(
                CaptureRequest.CONTROL_ENABLE_ZSL, null
            )
        ).isNull()
    }

    @Test
    fun unpackWithPixel2NotSupportApiLevelAndMaxQuality() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .useCaseConfig
        setDeviceProperty(PROPERTIES_PIXEL_2_NOT_SUPPORT_API)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(
                CaptureRequest.CONTROL_ENABLE_ZSL, null
            )
        ).isNull()
    }

    @Test
    fun unpackWithValidPixel3AndMinLatency() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .useCaseConfig
        setDeviceProperty(PROPERTIES_PIXEL_3_API26)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(
                CaptureRequest.CONTROL_ENABLE_ZSL, null
            )
        ).isEqualTo(false)
    }

    @Test
    fun unpackWithValidPixel3AndMaxQuality() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .useCaseConfig
        setDeviceProperty(PROPERTIES_PIXEL_3_API26)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(
                CaptureRequest.CONTROL_ENABLE_ZSL, null
            )
        ).isEqualTo(true)
    }

    @Test
    fun unpackWithPixel3NotSupportApiLevelAndMinLatency() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .useCaseConfig
        setDeviceProperty(PROPERTIES_PIXEL_3_NOT_SUPPORT_API)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(
                CaptureRequest.CONTROL_ENABLE_ZSL, null
            )
        ).isNull()
    }

    @Test
    fun unpackWithPixel3NotSupportApiLevelAndMaxQuality() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .useCaseConfig
        setDeviceProperty(PROPERTIES_PIXEL_3_NOT_SUPPORT_API)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(
                CaptureRequest.CONTROL_ENABLE_ZSL, null
            )
        ).isNull()
    }

    @Test
    fun unpackWithNotSupportManufacture() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .useCaseConfig
        setDeviceProperty(PROPERTIES_NOT_GOOGLE)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(
                CaptureRequest.CONTROL_ENABLE_ZSL, null
            )
        ).isNull()
    }

    @Test
    fun unpackWithNotSupportModel() {
        val captureBuilder = CaptureConfig.Builder()
        val imageCaptureConfig = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .useCaseConfig
        setDeviceProperty(PROPERTIES_NOT_SUPPORT_MODEL)
        unpacker.unpack(imageCaptureConfig, captureBuilder)
        val captureConfig = captureBuilder.build()
        val camera2Config = Camera2ImplConfig(
            captureConfig.implementationOptions
        )
        assertThat(
            camera2Config.getCaptureRequestOption<Boolean>(
                CaptureRequest.CONTROL_ENABLE_ZSL, null
            )
        ).isNull()
    }

    private fun setDeviceProperty(properties: DeviceProperties) {
        ShadowBuild.setManufacturer(properties.manufacturer())
        ShadowBuild.setModel(properties.model())
        ReflectionHelpers.setStaticField(
            Build.VERSION::class.java,
            "SDK_INT",
            properties.sdkVersion()
        )
    }

    companion object {
        private const val MANUFACTURE_GOOGLE = "Google"
        private const val MANUFACTURE_NOT_GOOGLE = "ANY"
        private const val MODEL_PIXEL_2 = "Pixel 2"
        private const val MODEL_PIXEL_3 = "Pixel 3"
        private const val MODEL_NOT_SUPPORT_HDR = "ANY"
        private const val API_LEVEL_25 = Build.VERSION_CODES.N_MR1
        private const val API_LEVEL_26 = Build.VERSION_CODES.O
        private val PROPERTIES_PIXEL_2_API26 = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_PIXEL_2,
            API_LEVEL_26
        )
        private val PROPERTIES_PIXEL_3_API26 = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_PIXEL_3,
            API_LEVEL_26
        )
        private val PROPERTIES_PIXEL_2_NOT_SUPPORT_API = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_PIXEL_2,
            API_LEVEL_25
        )
        private val PROPERTIES_PIXEL_3_NOT_SUPPORT_API = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_PIXEL_3,
            API_LEVEL_25
        )
        private val PROPERTIES_NOT_GOOGLE = DeviceProperties.create(
            MANUFACTURE_NOT_GOOGLE,
            MODEL_PIXEL_2,
            API_LEVEL_26
        )
        private val PROPERTIES_NOT_SUPPORT_MODEL = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_NOT_SUPPORT_HDR,
            API_LEVEL_26
        )
    }
}
