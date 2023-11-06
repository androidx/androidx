/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.impl.createCaptureRequestOption
import androidx.camera.camera2.pipe.integration.interop.Camera2Interop
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraUseCaseAdapterTest {

    private val resolution: Size = Size(640, 480)

    @Test
    fun shouldApplyOptionsFromConfigToBuilder_whenDefaultConfigSet() {
        // Arrange
        val defaultCaptureConfig = CaptureConfig.Builder()
            .apply {
                templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
                implementationOptions = MutableOptionsBundle.create()
                    .apply {
                        insertOption(ImageOutputConfig.OPTION_TARGET_ROTATION, Surface.ROTATION_180)
                    }
                addCameraCaptureCallback(object : CameraCaptureCallback() {})
            }
            .build()
        val useCaseConfig = ImageCapture.Builder()
            .setDefaultCaptureConfig(defaultCaptureConfig)
            .useCaseConfig
        val builder = CaptureConfig.Builder()

        // Act
        CameraUseCaseAdapter.DefaultCaptureOptionsUnpacker.INSTANCE.unpack(useCaseConfig, builder)

        // Assert
        val config = builder.build()
        config.assertEquals(useCaseConfig.defaultCaptureConfig)
    }

    @Test
    fun shouldApplySessionConfig_whenDefaultConfigSet() {
        // Arrange
        val defaultSessionCaptureConfig = SessionConfig.Builder()
            .apply {
                setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                addImplementationOptions(
                    MutableOptionsBundle.create()
                        .apply {
                            insertOption(
                                ImageOutputConfig.OPTION_TARGET_ROTATION,
                                Surface.ROTATION_180
                            )
                        }
                )
                addDeviceStateCallback(object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        // unused
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        // unused
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        // unused
                    }
                })
                addSessionStateCallback(object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        // unused
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        // unused
                    }
                })
                addRepeatingCameraCaptureCallback(object : CameraCaptureCallback() {})
                addCameraCaptureCallback(object : CameraCaptureCallback() {})
            }
            .build()

        val useCaseConfig = ImageCapture.Builder()
            .setDefaultSessionConfig(defaultSessionCaptureConfig)
            .useCaseConfig
        val builder = SessionConfig.Builder()

        // Act
        CameraUseCaseAdapter.DefaultSessionOptionsUnpacker.unpack(resolution,
            useCaseConfig, builder)

        // Assert
        val config = builder.build()
        config.assertEquals(useCaseConfig.defaultSessionConfig)
    }

    @Test
    @OptIn(ExperimentalCamera2Interop::class)
    fun unpackerSessionConfig_ExtractsInteropCallbacks() {
        // Arrange
        val imageCaptureBuilder = ImageCapture.Builder()
        val captureCallback = object : CaptureCallback() {}
        val deviceCallback = object : CameraDevice.StateCallback() {
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
        val sessionStateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                // unused
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                // unused
            }
        }

        Camera2Interop.Extender<ImageCapture>(imageCaptureBuilder)
            .setSessionCaptureCallback(captureCallback)
            .setDeviceStateCallback(deviceCallback)
            .setSessionStateCallback(sessionStateCallback)

        // Act
        val sessionBuilder = SessionConfig.Builder()
        CameraUseCaseAdapter.DefaultSessionOptionsUnpacker.unpack(
            resolution,
            imageCaptureBuilder.useCaseConfig,
            sessionBuilder
        )
        val sessionConfig = sessionBuilder.build()

        // Assert
        val interopCallback = sessionConfig.singleCameraCaptureCallbacks[0]
        assertThat(
            (interopCallback as CameraUseCaseAdapter.CaptureCallbackContainer).captureCallback
        ).isEqualTo(captureCallback)
        assertThat(sessionConfig.singleCameraCaptureCallbacks).containsExactly(interopCallback)
        assertThat(sessionConfig.repeatingCameraCaptureCallbacks).containsExactly(interopCallback)
        assertThat(sessionConfig.deviceStateCallbacks).containsExactly(deviceCallback)
        assertThat(sessionConfig.sessionStateCallbacks).containsExactly(sessionStateCallback)
    }

    @Test
    @OptIn(ExperimentalCamera2Interop::class)
    fun unpackerSessionConfig_ExtractsInteropOptions() {
        // Arrange
        val physicalCameraId = "0"
        val imageCaptureConfigBuilder = ImageCapture.Builder()

        // Add 2 options to ensure that multiple options can be unpacked.
        Camera2Interop.Extender<ImageCapture>(
            imageCaptureConfigBuilder
        ).setCaptureRequestOption<Int>(
            CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO
        ).setCaptureRequestOption<Int>(
            CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH
        ).apply {
            if (Build.VERSION.SDK_INT >= 28) {
                setPhysicalCameraId(physicalCameraId)
            }
        }
        val useCaseConfig = imageCaptureConfigBuilder.useCaseConfig
        val priorityAfMode = useCaseConfig.getCaptureRequestOptionPriority(
            CaptureRequest.CONTROL_AF_MODE
        )
        val priorityFlashMode = useCaseConfig.getCaptureRequestOptionPriority(
            CaptureRequest.FLASH_MODE
        )
        val sessionBuilder = SessionConfig.Builder()

        // Act
        CameraUseCaseAdapter.DefaultSessionOptionsUnpacker.unpack(resolution,
            useCaseConfig, sessionBuilder)
        val sessionConfig = sessionBuilder.build()

        // Assert
        val config =
            Camera2ImplConfig(sessionConfig.implementationOptions)
        assertThat(
            config.getCaptureRequestOption<Int>(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF
            )
        ).isEqualTo(CaptureRequest.CONTROL_AF_MODE_AUTO)
        assertThat(
            config.getCaptureRequestOption<Int>(
                CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF
            )
        ).isEqualTo(CaptureRequest.FLASH_MODE_TORCH)
        if (Build.VERSION.SDK_INT >= 28) {
            assertThat(
                config.getPhysicalCameraId(null)
            ).isEqualTo(physicalCameraId)
        }
        // Make sures the priority of Camera2Interop is preserved after unpacking.
        assertThat(config.getCaptureRequestOptionPriority(CaptureRequest.CONTROL_AF_MODE))
            .isEqualTo(priorityAfMode)
        assertThat(config.getCaptureRequestOptionPriority(CaptureRequest.CONTROL_AF_MODE))
            .isEqualTo(priorityFlashMode)
    }

    @Test
    @OptIn(ExperimentalCamera2Interop::class)
    fun unpackerCaptureConfig_ExtractsCaptureCallbacks() {
        // Arrange
        val imageCaptureBuilder = ImageCapture.Builder()
        val captureCallback = object : CaptureCallback() {}

        Camera2Interop.Extender<ImageCapture>(imageCaptureBuilder)
            .setSessionCaptureCallback(captureCallback)

        // Act
        val captureBuilder = CaptureConfig.Builder()
        CameraUseCaseAdapter.DefaultCaptureOptionsUnpacker.INSTANCE.unpack(
            imageCaptureBuilder.useCaseConfig,
            captureBuilder
        )
        val captureConfig = captureBuilder.build()

        // Assert
        val cameraCaptureCallback = captureConfig.cameraCaptureCallbacks[0]
        assertThat(
            (cameraCaptureCallback as CameraUseCaseAdapter.CaptureCallbackContainer).captureCallback
        ).isEqualTo(captureCallback)
    }

    @Test
    @OptIn(ExperimentalCamera2Interop::class)
    fun unpackerCaptureConfig_ExtractsOptions() {
        // Arrange
        val imageCaptureConfigBuilder = ImageCapture.Builder()

        // Add 2 options to ensure that multiple options can be unpacked.
        Camera2Interop.Extender<ImageCapture>(
            imageCaptureConfigBuilder
        ).setCaptureRequestOption<Int>(
            CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO
        ).setCaptureRequestOption<Int>(
            CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH
        )
        val useCaseConfig = imageCaptureConfigBuilder.useCaseConfig
        val priorityAfMode = useCaseConfig.getCaptureRequestOptionPriority(
            CaptureRequest.CONTROL_AF_MODE
        )
        val priorityFlashMode = useCaseConfig.getCaptureRequestOptionPriority(
            CaptureRequest.FLASH_MODE
        )

        val captureBuilder = CaptureConfig.Builder()

        // Act
        CameraUseCaseAdapter.DefaultCaptureOptionsUnpacker.INSTANCE.unpack(
            useCaseConfig, captureBuilder
        )
        val captureConfig = captureBuilder.build()

        // Assert
        val config = Camera2ImplConfig(captureConfig.implementationOptions)
        assertThat(
            config.getCaptureRequestOption<Int>(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF
            )
        ).isEqualTo(CaptureRequest.CONTROL_AF_MODE_AUTO)
        assertThat(
            config.getCaptureRequestOption<Int>(
                CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF
            )
        ).isEqualTo(CaptureRequest.FLASH_MODE_TORCH)

        // Make sures the priority of Camera2Interop is preserved after unpacking.
        assertThat(config.getCaptureRequestOptionPriority(CaptureRequest.CONTROL_AF_MODE))
            .isEqualTo(priorityAfMode)
        assertThat(config.getCaptureRequestOptionPriority(CaptureRequest.CONTROL_AF_MODE))
            .isEqualTo(priorityFlashMode)
    }

    private fun androidx.camera.core.impl.Config.getCaptureRequestOptionPriority(
        key: CaptureRequest.Key<*>
    ) = getOptionPriority(key.createCaptureRequestOption())

    private fun CaptureConfig.assertEquals(other: CaptureConfig) {
        assertThat(templateType).isEqualTo(other.templateType)
        assertThat(isUseRepeatingSurface).isEqualTo(other.isUseRepeatingSurface)
        assertThat(cameraCaptureCallbacks).isEqualTo(other.cameraCaptureCallbacks)
        assertThat(surfaces).isEqualTo(other.surfaces)

        // Implementation options
        assertThat(implementationOptions.listOptions())
            .isEqualTo(other.implementationOptions.listOptions())
        implementationOptions.listOptions().forEach { option ->
            assertThat(implementationOptions.retrieveOption(option)).isEqualTo(
                other.implementationOptions.retrieveOption(option)
            )
        }

        // Tag bundle
        assertThat(tagBundle.listKeys()).isEqualTo(other.tagBundle.listKeys())
        tagBundle.listKeys().forEach { key ->
            assertThat(tagBundle.getTag(key)).isEqualTo(other.tagBundle.getTag(key))
        }
    }

    private fun SessionConfig.assertEquals(other: SessionConfig) {
        assertThat(templateType).isEqualTo(other.templateType)
        // Implementation options
        assertThat(implementationOptions.listOptions())
            .isEqualTo(other.implementationOptions.listOptions())
        implementationOptions.listOptions().forEach { option ->
            assertThat(implementationOptions.retrieveOption(option)).isEqualTo(
                other.implementationOptions.retrieveOption(option)
            )
        }

        // Verify callbacks
        assertThat(deviceStateCallbacks).isEqualTo(other.deviceStateCallbacks)
        assertThat(sessionStateCallbacks).isEqualTo(other.sessionStateCallbacks)
        assertThat(repeatingCameraCaptureCallbacks).isEqualTo(other.repeatingCameraCaptureCallbacks)
    }
}
