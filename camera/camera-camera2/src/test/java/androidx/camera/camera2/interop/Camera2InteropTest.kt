/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.interop

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.testing.FakeCameraInfoAdapterCreator.createCameraInfoAdapter
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.testing.impl.fakes.FakeConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics

private const val INVALID_TEMPLATE_TYPE = -1
private const val INVALID_COLOR_CORRECTION_MODE = -1
private const val PHYSICAL_CAMERA_ID = "0"
private val SESSION_CAPTURE_CALLBACK =
    object : CameraCaptureSession.CaptureCallback() {
        // unused
    }
private val SESSION_STATE_CALLBACK =
    object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            // unused
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            // unused
        }
    }
private val DEVICE_STATE_CALLBACK =
    object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            // unused
        }

        override fun onDisconnected(camera: CameraDevice) {
            // unused
        }

        override fun onError(camera: CameraDevice, error: Int) {
            // unused
        }
    }

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
@OptIn(ExperimentalCamera2Interop::class)
class Camera2InteropTest {
    @Test
    fun canExtendWithTemplateType() {
        // Arrange
        val builder = FakeConfig.Builder()
        Camera2Interop.Extender(builder).setCaptureRequestTemplate(CameraDevice.TEMPLATE_PREVIEW)

        // Act
        val config = Camera2ImplConfig(builder.build())

        // Assert
        assertThat(config.getCaptureRequestTemplate(INVALID_TEMPLATE_TYPE))
            .isEqualTo(CameraDevice.TEMPLATE_PREVIEW)
    }

    @Config(minSdk = 33)
    @Test
    fun canExtendWithStreamUseCase() {
        // Arrange
        val builder = FakeConfig.Builder()
        Camera2Interop.Extender(builder).setStreamUseCase(3)

        // Act
        val config = Camera2ImplConfig(builder.build())

        // Assert
        assertThat(config.getStreamUseCase(-1)).isEqualTo(3)
    }

    @Test
    fun defaultConfigDoesNotSetStreamUseCase() {
        // Arrange
        val builder = FakeConfig.Builder()

        // Act
        val config = Camera2ImplConfig(builder.build())

        // Assert
        assertThat(config.getStreamUseCase(-1)).isEqualTo(-1)
    }

    @Test
    fun canExtendWithSessionCaptureCallback() {
        // Arrange
        val builder = FakeConfig.Builder()
        Camera2Interop.Extender(builder).setSessionCaptureCallback(SESSION_CAPTURE_CALLBACK)

        // Act
        val config = Camera2ImplConfig(builder.build())

        // Assert
        assertThat(config.getSessionCaptureCallback(/* valueIfMissing= */ null))
            .isSameInstanceAs(SESSION_CAPTURE_CALLBACK)
    }

    @Test
    fun canExtendWithSessionStateCallback() {
        // Arrange
        val builder = FakeConfig.Builder()
        Camera2Interop.Extender(builder).setSessionStateCallback(SESSION_STATE_CALLBACK)

        // Act
        val config = Camera2ImplConfig(builder.build())

        // Assert
        assertThat(config.getSessionStateCallback(/* valueIfMissing= */ null))
            .isSameInstanceAs(SESSION_STATE_CALLBACK)
    }

    @Test
    fun canExtendWithDeviceStateCallback() {
        // Arrange
        val builder = FakeConfig.Builder()
        Camera2Interop.Extender(builder).setDeviceStateCallback(DEVICE_STATE_CALLBACK)

        // Act
        val config = Camera2ImplConfig(builder.build())

        // Assert
        assertThat(config.getDeviceStateCallback(/* valueIfMissing= */ null))
            .isSameInstanceAs(DEVICE_STATE_CALLBACK)
    }

    @Test
    fun canSetAndRetrieveCaptureRequestKeys() {
        // Arrange
        val builder = FakeConfig.Builder()
        val fakeRange = Range(0, 30)
        Camera2Interop.Extender(builder)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
            .setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                CameraMetadata.COLOR_CORRECTION_MODE_FAST,
            )

        // Act
        val config = Camera2ImplConfig(builder.build())

        // Assert
        assertThat(
                config.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    /*valueIfMissing=*/ null,
                )
            )
            .isEqualTo(fakeRange)
        assertThat(
                config.getCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    INVALID_COLOR_CORRECTION_MODE,
                )
            )
            .isEqualTo(CameraMetadata.COLOR_CORRECTION_MODE_FAST)
    }

    @Test
    fun canSetAndRetrieveCaptureRequestKeys_fromOptionIds() {
        // Arrange
        val builder = FakeConfig.Builder()
        val fakeRange = Range(0, 30)
        Camera2Interop.Extender(builder)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
            .setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                CameraMetadata.COLOR_CORRECTION_MODE_FAST,
            ) // Insert one non capture request option to ensure it gets filtered out
            .setCaptureRequestTemplate(CameraDevice.TEMPLATE_PREVIEW)

        // Act
        val config = Camera2ImplConfig(builder.build())

        // Assert
        config.findOptions(Camera2ImplConfig.CAPTURE_REQUEST_ID_STEM) { option ->
            // The token should be the capture request key
            assertThat(option.getToken())
                .isAnyOf(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    CaptureRequest.COLOR_CORRECTION_MODE,
                )
            true
        }
        assertThat(config.listOptions()).hasSize(3)
    }

    @Test
    fun captureRequestOptionPriorityIsAlwaysOverride() {
        // Arrange
        val builder = FakeConfig.Builder()
        val fakeRange = Range(0, 30)
        Camera2Interop.Extender(builder)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)

        // Act
        val config: androidx.camera.core.impl.Config = builder.build()

        // Assert
        config.findOptions(Camera2ImplConfig.CAPTURE_REQUEST_ID_STEM) {
            option: androidx.camera.core.impl.Config.Option<*>? ->
            assertThat(config.getOptionPriority(option!!))
                .isEqualTo(androidx.camera.core.impl.Config.OptionPriority.ALWAYS_OVERRIDE)
            true
        }
    }

    @Config(minSdk = 28)
    @Test
    fun canExtendWithPhysicalCameraId() {
        // Arrange
        val builder = FakeConfig.Builder()
        Camera2Interop.Extender<FakeConfig>(builder).setPhysicalCameraId(PHYSICAL_CAMERA_ID)

        // Act
        val config = Camera2ImplConfig(builder.build())

        // Assert
        assertThat(config.getPhysicalCameraId(null)).isEqualTo(PHYSICAL_CAMERA_ID)
    }

    @Test
    fun canGetId_fromCamera2InteropStatic() {
        val cameraId = "42"
        val cameraInfo = createCameraInfoAdapter(cameraId = CameraId(cameraId))
        val extractedId: String = Camera2Interop.getCameraId(cameraInfo)

        // Assert.
        assertThat(extractedId).isEqualTo(cameraId)
    }

    @Test
    fun canExtractCharacteristics_fromCamera2InteropStatic() {
        val cameraInfo =
            createCameraInfoWithCharacteristics(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
            )

        val hardwareLevel =
            Camera2Interop.getCameraCharacteristics(cameraInfo)
                .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

        // Assert.
        assertThat(hardwareLevel)
            .isEqualTo(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
    }

    @Test
    fun canCreateCameraSelector_fromCamera2InteropStatic() {
        val cameraId = "42"
        val cameraSelector = Camera2Interop.getCameraSelectorFromCameraId(cameraId)

        // Assert.
        assertThat(cameraSelector).isNotNull()
        assertCameraSelectorSelectsCameraId(cameraSelector, cameraId)
    }

    @Test
    fun canGetId_fromCameraInfoExtension() {
        val cameraId = "42"
        val cameraInfo: CameraInfo = createCameraInfoAdapter(cameraId = CameraId(cameraId))
        val extractedId: String = cameraInfo.cameraId

        // Assert.
        assertThat(extractedId).isEqualTo(cameraId)
    }

    @Test
    fun canExtractCharacteristics_fromCameraInfoExtension() {
        val cameraInfo =
            createCameraInfoWithCharacteristics(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
            )

        val hardwareLevel =
            cameraInfo.cameraCharacteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
            )

        // Assert.
        assertThat(hardwareLevel)
            .isEqualTo(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
    }

    @Test
    fun canCreateCameraSelector_fromCameraIdExtension() {
        val cameraId = "42"
        val cameraSelector = cameraId.toCameraSelector()

        // Assert.
        assertThat(cameraSelector).isNotNull()
        assertCameraSelectorSelectsCameraId(cameraSelector, cameraId)
    }

    private fun createCameraInfoWithCharacteristics(cameraHardwareLevel: Int): CameraInfo {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
            cameraHardwareLevel,
        )

        class FakeCamera2Metadata(
            private val delegate: FakeCameraMetadata,
            private val cameraCharacteristics: CameraCharacteristics,
        ) : androidx.camera.camera2.pipe.CameraMetadata by delegate {
            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> unwrapAs(type: Class<T>): T? {
                if (type == CameraCharacteristics::class.java) {
                    return cameraCharacteristics as T
                }
                return delegate.unwrapAs(type)
            }
        }

        val fakeMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                characteristicsOverrides =
                    mapOf(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to cameraHardwareLevel
                    ),
            )

        return createCameraInfoAdapter(
            cameraProperties =
                FakeCameraProperties(FakeCamera2Metadata(fakeMetadata, characteristics))
        )
    }

    private fun assertCameraSelectorSelectsCameraId(
        cameraSelector: CameraSelector,
        expectedCameraId: String,
    ) {
        val cameraInfo0 = createCameraInfoAdapter(cameraId = CameraId("0"))
        val cameraInfoExpected = createCameraInfoAdapter(cameraId = CameraId(expectedCameraId))
        val cameraInfo1 = createCameraInfoAdapter(cameraId = CameraId("1"))
        val filteredCameraInfos =
            cameraSelector.filter(listOf(cameraInfo0, cameraInfoExpected, cameraInfo1))
        assertThat(filteredCameraInfos).containsExactly(cameraInfoExpected)
    }
}
