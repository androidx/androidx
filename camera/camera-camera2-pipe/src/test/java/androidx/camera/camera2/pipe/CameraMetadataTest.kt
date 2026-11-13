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

package androidx.camera.camera2.pipe

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_HDR
import android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_NIGHT
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isSupported
import androidx.camera.camera2.pipe.testing.FakeCameraExtensionMetadata
import androidx.camera.camera2.pipe.testing.FakeCameraIds
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
internal class CameraMetadataTest {

    @Test
    fun isExtensionSupported_defaultImplementation_returnsTrueForSupportedExtension() {
        val cameraMetadata =
            FakeCameraMetadata(
                extensionMetadata =
                    mapOf(
                        EXTENSION_NIGHT to
                            FakeCameraExtensionMetadata(FakeCameraIds.default, EXTENSION_NIGHT)
                    )
            )

        assertThat(cameraMetadata.isExtensionSupported(EXTENSION_NIGHT)).isTrue()
    }

    @Test
    fun isExtensionSupported_defaultImplementation_returnsFalseForUnsupportedExtension() {
        val cameraMetadata =
            FakeCameraMetadata(
                extensionMetadata =
                    mapOf(
                        EXTENSION_NIGHT to
                            FakeCameraExtensionMetadata(FakeCameraIds.default, EXTENSION_NIGHT)
                    )
            )

        assertThat(cameraMetadata.isExtensionSupported(EXTENSION_HDR)).isFalse()
    }

    @Test
    fun isExtensionSupported_defaultImplementation_returnsFalseWhenNoExtensionsSupported() {
        val cameraMetadata = FakeCameraMetadata()

        assertThat(cameraMetadata.isExtensionSupported(EXTENSION_NIGHT)).isFalse()
    }

    @Test
    fun isSupported_aeMode_returnsTrueOnlyForAvailableModes() {
        val missingMetadata = FakeCameraMetadata()
        assertThat(missingMetadata.isSupported(AeMode.ON)).isFalse()

        val emptyMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to intArrayOf())
            )
        assertThat(emptyMetadata.isSupported(AeMode.ON)).isFalse()

        val populatedMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(
                        CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                            intArrayOf(AeMode.ON.value, AeMode.ON_AUTO_FLASH.value)
                    )
            )
        assertThat(populatedMetadata.isSupported(AeMode.ON)).isTrue()
        assertThat(populatedMetadata.isSupported(AeMode.ON_AUTO_FLASH)).isTrue()
        assertThat(populatedMetadata.isSupported(AeMode.OFF)).isFalse()
        assertThat(populatedMetadata.isSupported(AeMode.ON_EXTERNAL_FLASH)).isFalse()
    }

    @Test
    fun isSupported_afMode_returnsTrueOnlyForAvailableModes() {
        val missingMetadata = FakeCameraMetadata()
        assertThat(missingMetadata.isSupported(AfMode.AUTO)).isFalse()

        val emptyMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to intArrayOf())
            )
        assertThat(emptyMetadata.isSupported(AfMode.AUTO)).isFalse()

        val populatedMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(
                        CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                            intArrayOf(AfMode.AUTO.value, AfMode.CONTINUOUS_PICTURE.value)
                    )
            )
        assertThat(populatedMetadata.isSupported(AfMode.AUTO)).isTrue()
        assertThat(populatedMetadata.isSupported(AfMode.CONTINUOUS_PICTURE)).isTrue()
        assertThat(populatedMetadata.isSupported(AfMode.OFF)).isFalse()
        assertThat(populatedMetadata.isSupported(AfMode.MACRO)).isFalse()
    }

    @Test
    fun isSupported_awbMode_returnsTrueOnlyForAvailableModes() {
        val missingMetadata = FakeCameraMetadata()
        assertThat(missingMetadata.isSupported(AwbMode.AUTO)).isFalse()

        val emptyMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES to intArrayOf())
            )
        assertThat(emptyMetadata.isSupported(AwbMode.AUTO)).isFalse()

        val populatedMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(
                        CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES to
                            intArrayOf(AwbMode.AUTO.value, AwbMode.DAYLIGHT.value)
                    )
            )
        assertThat(populatedMetadata.isSupported(AwbMode.AUTO)).isTrue()
        assertThat(populatedMetadata.isSupported(AwbMode.DAYLIGHT)).isTrue()
        assertThat(populatedMetadata.isSupported(AwbMode.OFF)).isFalse()
        assertThat(populatedMetadata.isSupported(AwbMode.CLOUDY_DAYLIGHT)).isFalse()
    }

    @Test
    fun isSupported_controlMode_returnsTrueOnlyForAvailableModes() {
        val missingMetadata = FakeCameraMetadata()
        assertThat(missingMetadata.isSupported(ControlMode.AUTO)).isFalse()

        val emptyMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(CameraCharacteristics.CONTROL_AVAILABLE_MODES to intArrayOf())
            )
        assertThat(emptyMetadata.isSupported(ControlMode.AUTO)).isFalse()

        val populatedMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(
                        CameraCharacteristics.CONTROL_AVAILABLE_MODES to
                            intArrayOf(ControlMode.AUTO.value, ControlMode.OFF.value)
                    )
            )
        assertThat(populatedMetadata.isSupported(ControlMode.AUTO)).isTrue()
        assertThat(populatedMetadata.isSupported(ControlMode.OFF)).isTrue()
        assertThat(populatedMetadata.isSupported(ControlMode.USE_SCENE_MODE)).isFalse()
        assertThat(populatedMetadata.isSupported(ControlMode.USE_EXTENDED_SCENE_MODE)).isFalse()
    }

    @Test
    fun isSupported_flashMode_checksFlashInfoAvailableAndStandardModes() {
        val missingFlashMetadata = FakeCameraMetadata()
        assertThat(missingFlashMetadata.isSupported(FlashMode.OFF)).isTrue()
        assertThat(missingFlashMetadata.isSupported(FlashMode.SINGLE)).isFalse()
        assertThat(missingFlashMetadata.isSupported(FlashMode.TORCH)).isFalse()
        assertThat(missingFlashMetadata.isSupported(FlashMode(99))).isFalse()

        val noFlashMetadata =
            FakeCameraMetadata(
                characteristics = mapOf(CameraCharacteristics.FLASH_INFO_AVAILABLE to false)
            )
        assertThat(noFlashMetadata.isSupported(FlashMode.OFF)).isTrue()
        assertThat(noFlashMetadata.isSupported(FlashMode.SINGLE)).isFalse()
        assertThat(noFlashMetadata.isSupported(FlashMode.TORCH)).isFalse()
        assertThat(noFlashMetadata.isSupported(FlashMode(99))).isFalse()

        val flashAvailableMetadata =
            FakeCameraMetadata(
                characteristics = mapOf(CameraCharacteristics.FLASH_INFO_AVAILABLE to true)
            )
        assertThat(flashAvailableMetadata.isSupported(FlashMode.OFF)).isTrue()
        assertThat(flashAvailableMetadata.isSupported(FlashMode.SINGLE)).isTrue()
        assertThat(flashAvailableMetadata.isSupported(FlashMode.TORCH)).isTrue()
        assertThat(flashAvailableMetadata.isSupported(FlashMode(99))).isFalse()
    }
}
