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
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.view.Surface
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
        CameraUseCaseAdapter.DefaultCaptureOptionsUnpacker.unpack(useCaseConfig, builder)

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
            }
            .build()

        val useCaseConfig = ImageCapture.Builder()
            .setDefaultSessionConfig(defaultSessionCaptureConfig)
            .useCaseConfig
        val builder = SessionConfig.Builder()

        // Act
        CameraUseCaseAdapter.DefaultSessionOptionsUnpacker.unpack(useCaseConfig, builder)

        // Assert
        val config = builder.build()
        config.assertEquals(useCaseConfig.defaultSessionConfig)
    }

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
