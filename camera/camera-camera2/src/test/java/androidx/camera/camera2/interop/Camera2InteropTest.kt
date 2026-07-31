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

@file:Suppress("DEPRECATION")

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
import androidx.camera.camera2.impl.extractSessionParameters
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.testing.FakeCameraInfoAdapterCreator.createCameraInfoAdapter
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.InteropConfigurator
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.impl.MutableConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.testing.impl.fakes.FakeConfig
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoOutput
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
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
@Suppress("NewApi")
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
    fun canGetCameraFilter_fromCamera2InteropStatic() {
        val cameraId = "42"
        val cameraFilter = Camera2Interop.getCameraFilterFromCameraId(cameraId)

        // Assert.
        assertThat(cameraFilter).isNotNull()
        val cameraInfo0 = createCameraInfoAdapter(cameraId = CameraId("0"))
        val cameraInfoExpected = createCameraInfoAdapter(cameraId = CameraId(cameraId))
        val cameraInfo1 = createCameraInfoAdapter(cameraId = CameraId("1"))
        val filteredCameraInfos =
            cameraFilter.filter(listOf(cameraInfo0, cameraInfoExpected, cameraInfo1))
        assertThat(filteredCameraInfos).containsExactly(cameraInfoExpected)
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

    @Test
    fun canConfigureUseCase() {
        // Arrange
        val builder = Preview.Builder()
        val configurator =
            Camera2Interop.forUseCase { interop ->
                interop
                    .setPhysicalCameraId("0")
                    .setStreamUseCase(3)
                    .setTimestampBase(1)
                    .setDynamicRangeProfile(5L)
                    .setSurfaceGroupId(3)
                    .setMirrorMode(2)
            }

        // Act
        builder.setInterop(configurator)
        val config = Camera2ImplConfig(builder.interopMutableConfig)

        // Assert
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("0")
        assertThat(config.getStreamUseCase(-1)).isEqualTo(3)
        assertThat(config.getTimestampBase(null)).isEqualTo(1)
        assertThat(config.getDynamicRangeProfile(null)).isEqualTo(5L)
        assertThat(config.getSurfaceGroupId(-1)).isEqualTo(3)
        assertThat(config.getMirrorMode(-1)).isEqualTo(2)

        // Verify VideoCapture builder interop configuration
        val videoOutput = VideoOutput { _ -> }
        val videoBuilder = VideoCapture.Builder(videoOutput)
        val videoConfigurator =
            Camera2Interop.forUseCase { interop -> interop.setPhysicalCameraId("5") }
        videoBuilder.setInterop(videoConfigurator)
        val videoConfig = Camera2ImplConfig(videoBuilder.useCaseConfig)
        assertThat(videoConfig.getPhysicalCameraId(null)).isEqualTo("5")
    }

    @Test
    fun canConfigureSessionConfig() {
        // Arrange
        val builder = SessionConfig.Builder(Preview.Builder().build())
        val fakeRange = Range(15, 30)
        val configurator =
            Camera2Interop.forSessionConfig { interop ->
                interop
                    .setSessionType(4)
                    .setColorSpace(5)
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF,
                    )
                    .setRepeatingCaptureRequestTemplate(CameraDevice.TEMPLATE_RECORD)
                    .setSessionParameter(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
            }

        // Act
        builder.setInterop(configurator)
        val sessionConfig = builder.build()
        val camera2Config = Camera2ImplConfig(sessionConfig.interopConfig)
        val sessionParameters = sessionConfig.interopConfig.extractSessionParameters()

        // Assert
        assertThat(camera2Config.getSessionType(-1)).isEqualTo(4)
        assertThat(camera2Config.getColorSpace(-1)).isEqualTo(5)
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF)
        assertThat(camera2Config.getCaptureRequestTemplate(-1))
            .isEqualTo(CameraDevice.TEMPLATE_RECORD)
        assertThat(sessionParameters[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE])
            .isEqualTo(fakeRange)
    }

    @Test
    fun canConfigureImageCapture() {
        // Arrange & Act - Test still capture configuration and callback (with executor)
        val callback = object : CameraCaptureSession.CaptureCallback() {}
        val executor = Executor { it.run() }

        val builder = ImageCapture.Builder()
        val configurator =
            Camera2Interop.forImageCapture { interop ->
                interop
                    .setStillCaptureRequestTemplateType(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    .setStillCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX,
                    )
                    .setStillCaptureCallback(executor, callback)
            }

        builder.setInterop(configurator)
        val config = Camera2ImplConfig(builder.useCaseConfig)

        // Assert
        assertThat(config.getStillCaptureTemplateType(-1))
            .isEqualTo(CameraDevice.TEMPLATE_STILL_CAPTURE)
        assertThat(config.getStillCaptureOption(CaptureRequest.COLOR_CORRECTION_MODE, null))
            .isEqualTo(CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
        val retrievedCallback =
            config.retrieveOption(Camera2ImplConfig.STILL_CAPTURE_CALLBACK_OPTION, null)
        assertThat(retrievedCallback).isNotNull()
        assertThat(retrievedCallback).isInstanceOf(CaptureCallbackExecutorWrapper::class.java)

        // Act & Assert - Verify default overload (without executor)
        val builder2 = ImageCapture.Builder()
        val configurator2 =
            Camera2Interop.forImageCapture { interop -> interop.setStillCaptureCallback(callback) }
        builder2.setInterop(configurator2)
        val config2 = Camera2ImplConfig(builder2.useCaseConfig)
        val retrievedCallback2 =
            config2.retrieveOption(Camera2ImplConfig.STILL_CAPTURE_CALLBACK_OPTION, null)
        assertThat(retrievedCallback2).isNotNull()
        assertThat(retrievedCallback2).isInstanceOf(CaptureCallbackExecutorWrapper::class.java)
    }

    @Test
    fun canConfigureCaptureRequestOptionsAndClear() {
        // Arrange - Test setting and clearing a single capture request option
        val builder1 = SessionConfig.Builder(Preview.Builder().build())
        val configurator1 =
            Camera2Interop.forSessionConfig { interop ->
                interop
                    .setCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CameraMetadata.COLOR_CORRECTION_MODE_FAST,
                    )
                    .clearCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE)
            }
        builder1.setInterop(configurator1)
        val camera2Config1 = Camera2ImplConfig(builder1.build().interopConfig)
        assertThat(
                camera2Config1.getCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    /* valueIfMissing = */ null,
                )
            )
            .isNull()

        // Arrange & Act - Test setting options and clearing all options
        val builder2 = SessionConfig.Builder(Preview.Builder().build())
        val fakeRange = Range(0, 30)
        val configurator2 =
            Camera2Interop.forSessionConfig { interop ->
                interop
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                    .setCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CameraMetadata.COLOR_CORRECTION_MODE_FAST,
                    )
                    .setRepeatingCaptureRequestTemplate(CameraDevice.TEMPLATE_RECORD)
                    .clearAllCaptureRequestOptions()
            }
        builder2.setInterop(configurator2)
        val camera2Config2 = Camera2ImplConfig(builder2.build().interopConfig)

        // Assert
        assertThat(
                camera2Config2.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    /* valueIfMissing = */ null,
                )
            )
            .isNull()
        assertThat(
                camera2Config2.getCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    /* valueIfMissing = */ null,
                )
            )
            .isNull()
        // Verify non-capture-request options are preserved
        assertThat(camera2Config2.getCaptureRequestTemplate(-1))
            .isEqualTo(CameraDevice.TEMPLATE_RECORD)
    }

    @Test
    fun canConfigureSessionConfigCallbacks() {
        // Arrange
        val builder = SessionConfig.Builder(Preview.Builder().build())
        val deviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val sessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {}

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }
        val captureCallback = object : CameraCaptureSession.CaptureCallback() {}
        val executor = Executor { it.run() }

        val configurator =
            Camera2Interop.forSessionConfig { interop ->
                interop
                    .setDeviceStateCallback(executor, deviceCallback)
                    .setSessionStateCallback(executor, sessionCallback)
                    .setRepeatingCaptureCallback(executor, captureCallback)
            }

        // Act
        builder.setInterop(configurator)
        val sessionConfig = builder.build()
        val camera2Config = Camera2ImplConfig(sessionConfig.interopConfig)

        // Assert
        val retrievedDeviceCallback =
            camera2Config.retrieveOption(Camera2ImplConfig.DEVICE_STATE_CALLBACK_OPTION, null)
        assertThat(retrievedDeviceCallback).isNotNull()
        assertThat(retrievedDeviceCallback)
            .isInstanceOf(DeviceStateCallbackExecutorWrapper::class.java)

        val retrievedSessionCallback =
            camera2Config.retrieveOption(Camera2ImplConfig.SESSION_STATE_CALLBACK_OPTION, null)
        assertThat(retrievedSessionCallback).isNotNull()
        assertThat(retrievedSessionCallback)
            .isInstanceOf(SessionStateCallbackExecutorWrapper::class.java)

        val retrievedCaptureCallback =
            camera2Config.retrieveOption(
                Camera2ImplConfig.SESSION_REPEATING_CAPTURE_CALLBACK_OPTION,
                null,
            )
        assertThat(retrievedCaptureCallback).isNotNull()
        assertThat(retrievedCaptureCallback)
            .isInstanceOf(CaptureCallbackExecutorWrapper::class.java)

        // Verify default overloads (without executor)
        val builder2 = SessionConfig.Builder(Preview.Builder().build())
        val configurator2 =
            Camera2Interop.forSessionConfig { interop ->
                interop
                    .setDeviceStateCallback(deviceCallback)
                    .setSessionStateCallback(sessionCallback)
                    .setRepeatingCaptureCallback(captureCallback)
            }
        builder2.setInterop(configurator2)
        val sessionConfig2 = builder2.build()
        val camera2Config2 = Camera2ImplConfig(sessionConfig2.interopConfig)

        val retrievedDeviceCallback2 =
            camera2Config2.retrieveOption(Camera2ImplConfig.DEVICE_STATE_CALLBACK_OPTION, null)
        assertThat(retrievedDeviceCallback2).isNotNull()
        assertThat(retrievedDeviceCallback2)
            .isInstanceOf(DeviceStateCallbackExecutorWrapper::class.java)

        val retrievedSessionCallback2 =
            camera2Config2.retrieveOption(Camera2ImplConfig.SESSION_STATE_CALLBACK_OPTION, null)
        assertThat(retrievedSessionCallback2).isNotNull()
        assertThat(retrievedSessionCallback2)
            .isInstanceOf(SessionStateCallbackExecutorWrapper::class.java)

        val retrievedCaptureCallback2 =
            camera2Config2.retrieveOption(
                Camera2ImplConfig.SESSION_REPEATING_CAPTURE_CALLBACK_OPTION,
                null,
            )
        assertThat(retrievedCaptureCallback2).isNotNull()
        assertThat(retrievedCaptureCallback2)
            .isInstanceOf(CaptureCallbackExecutorWrapper::class.java)
    }

    @Test
    fun canConfigureCameraControlCallbacks() {
        // Arrange
        val callback = object : CameraCaptureSession.CaptureCallback() {}
        val executor = Executor { it.run() }

        // Test with executor
        val mutableConfig1 = MutableOptionsBundle.create()
        val cameraControl1 = createCameraControl(mutableConfig1)
        val configurator1 =
            Camera2Interop.forCameraControl { interop ->
                interop.setRepeatingCaptureCallback(executor, callback)
            }
        cameraControl1.applyInteropAsync(configurator1)
        val config1 = Camera2ImplConfig(mutableConfig1)
        val retrievedCallback1 =
            config1.retrieveOption(
                Camera2ImplConfig.SESSION_REPEATING_CAPTURE_CALLBACK_OPTION,
                null,
            )
        assertThat(retrievedCallback1).isNotNull()
        assertThat(retrievedCallback1).isInstanceOf(CaptureCallbackExecutorWrapper::class.java)

        // Test default overload (without executor)
        val mutableConfig2 = MutableOptionsBundle.create()
        val cameraControl2 = createCameraControl(mutableConfig2)
        val configurator2 =
            Camera2Interop.forCameraControl { interop ->
                interop.setRepeatingCaptureCallback(callback)
            }
        cameraControl2.applyInteropAsync(configurator2)
        val config2 = Camera2ImplConfig(mutableConfig2)
        val retrievedCallback2 =
            config2.retrieveOption(
                Camera2ImplConfig.SESSION_REPEATING_CAPTURE_CALLBACK_OPTION,
                null,
            )
        assertThat(retrievedCallback2).isNotNull()
        assertThat(retrievedCallback2).isInstanceOf(CaptureCallbackExecutorWrapper::class.java)
    }

    @Test
    fun canConfigureCameraControlCaptureRequestOptionsAndClear() {
        // Arrange & Act - Test setting capture request options
        val mutableConfig1 = MutableOptionsBundle.create()
        val cameraControl1 = createCameraControl(mutableConfig1)
        val fakeRange = Range(0, 30)
        val configurator1 =
            Camera2Interop.forCameraControl { interop ->
                interop
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                    .setCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CameraMetadata.COLOR_CORRECTION_MODE_FAST,
                    )
            }
        cameraControl1.applyInteropAsync(configurator1)
        val config1 = Camera2ImplConfig(mutableConfig1)

        // Assert
        assertThat(
                config1.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    /* valueIfMissing = */ null,
                )
            )
            .isEqualTo(fakeRange)
        assertThat(
                config1.getCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    INVALID_COLOR_CORRECTION_MODE,
                )
            )
            .isEqualTo(CameraMetadata.COLOR_CORRECTION_MODE_FAST)

        // Arrange & Act - Test clearing a single capture request option
        val mutableConfig2 = MutableOptionsBundle.create()
        val cameraControl2 = createCameraControl(mutableConfig2)
        val configurator2 =
            Camera2Interop.forCameraControl { interop ->
                interop
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                    .setCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CameraMetadata.COLOR_CORRECTION_MODE_FAST,
                    )
                    .clearCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE)
            }
        cameraControl2.applyInteropAsync(configurator2)
        val config2 = Camera2ImplConfig(mutableConfig2)

        // Assert
        assertThat(
                config2.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    /* valueIfMissing = */ null,
                )
            )
            .isEqualTo(fakeRange)
        assertThat(
                config2.getCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    /* valueIfMissing = */ null,
                )
            )
            .isNull()

        // Arrange & Act - Test clearing all capture request options
        val mutableConfig3 = MutableOptionsBundle.create()
        val cameraControl3 = createCameraControl(mutableConfig3)
        val configurator3 =
            Camera2Interop.forCameraControl { interop ->
                interop
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                    .setCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CameraMetadata.COLOR_CORRECTION_MODE_FAST,
                    )
                    .setRepeatingCaptureRequestTemplate(CameraDevice.TEMPLATE_RECORD)
                    .clearAllCaptureRequestOptions()
            }
        cameraControl3.applyInteropAsync(configurator3)
        val config3 = Camera2ImplConfig(mutableConfig3)

        // Assert
        assertThat(
                config3.getCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    /* valueIfMissing = */ null,
                )
            )
            .isNull()
        assertThat(
                config3.getCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    /* valueIfMissing = */ null,
                )
            )
            .isNull()
        // Verify non-capture-request options are preserved
        assertThat(config3.getCaptureRequestTemplate(-1)).isEqualTo(CameraDevice.TEMPLATE_RECORD)
    }

    private fun createCameraControl(
        mutableConfig: MutableConfig
    ): androidx.camera.core.CameraControl {
        return object : androidx.camera.core.CameraControl {
            override fun getInteropMutableConfig(): MutableConfig = mutableConfig

            override fun applyInteropAsync(
                configurator: InteropConfigurator<in androidx.camera.core.CameraControl>
            ): com.google.common.util.concurrent.ListenableFuture<Void> {
                configurator.configure(this)
                return androidx.camera.core.impl.utils.futures.Futures.immediateFuture(null)
            }

            override fun enableTorch(torch: Boolean) =
                androidx.camera.core.impl.utils.futures.Futures.immediateFuture<Void>(null)

            override fun startFocusAndMetering(action: androidx.camera.core.FocusMeteringAction) =
                TODO()

            override fun cancelFocusAndMetering() = TODO()

            override fun setZoomRatio(ratio: Float) = TODO()

            override fun setLinearZoom(linearZoom: Float) = TODO()

            override fun setExposureCompensationIndex(value: Int) = TODO()
        }
    }
}
