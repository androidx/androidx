/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class CameraMetadataIntegrationTest {
    private lateinit var cameraMetadata: androidx.camera.camera2.pipe.CameraMetadata

    private fun initCameraMetadata(
        cameraCharacteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap()
    ) {
        cameraMetadata = FakeCameraMetadata(cameraCharacteristics)
    }

    @Before
    fun setUp() {
        initCameraMetadata()
    }

    @Test
    fun getSupportedAeMode_returnsPreferredMode_whenSupported() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                    intArrayOf(
                        CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                        CameraMetadata.CONTROL_AE_MODE_ON,
                        CameraMetadata.CONTROL_AE_MODE_OFF,
                    )
            )
        )

        assertThat(
                cameraMetadata.getSupportedAeMode(CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
            )
            .isEqualTo(CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
    }

    @Test
    fun getSupportedAeMode_returnsAeModeOnIfSupported_whenPreferredModeNotSupported() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                    intArrayOf(
                        CameraMetadata.CONTROL_AE_MODE_ON,
                        CameraMetadata.CONTROL_AE_MODE_OFF,
                    )
            )
        )

        assertThat(
                cameraMetadata.getSupportedAeMode(CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
            )
            .isEqualTo(CameraMetadata.CONTROL_AE_MODE_ON)
    }

    @Test
    fun getSupportedAeMode_returnsAeModeOff_whenPreferredModeAndAeModeOnNotSupported() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                    intArrayOf(
                        CameraMetadata.CONTROL_AE_MODE_OFF,
                    )
            )
        )

        assertThat(
                cameraMetadata.getSupportedAeMode(CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
            )
            .isEqualTo(CameraMetadata.CONTROL_AE_MODE_OFF)
    }

    @Test
    @Config(maxSdk = 27)
    fun isExternalFlashAeModeSupported_returnsFalseWhenBelowApiLevel28() {
        assertThat(cameraMetadata.isExternalFlashAeModeSupported()).isFalse()
    }

    @Test
    @Config(minSdk = 28)
    fun isExternalFlashAeModeSupported_returnsFalseWhenNotSupported() {
        assertThat(cameraMetadata.isExternalFlashAeModeSupported()).isFalse()
    }

    @Test
    @Config(minSdk = 28)
    fun isExternalFlashAeModeSupported_returnsTrueWhenSupported() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                    intArrayOf(
                        CameraMetadata.CONTROL_AE_MODE_ON_EXTERNAL_FLASH,
                    )
            )
        )

        assertThat(cameraMetadata.isExternalFlashAeModeSupported()).isTrue()
    }

    @Test
    fun getSupportedAfMode_returnsPreferredMode_whenSupported() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                    intArrayOf(
                        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO,
                    )
            )
        )

        assertThat(
                cameraMetadata.getSupportedAfMode(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            )
            .isEqualTo(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
    }

    @Test
    fun getSupportedAfMode_returnsContinuousPictureIfSupported_whenPreferredModeNotSupported() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                    intArrayOf(
                        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                        CameraMetadata.CONTROL_AF_MODE_AUTO,
                        CameraMetadata.CONTROL_AF_MODE_OFF
                    )
            )
        )

        assertThat(
                cameraMetadata.getSupportedAfMode(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            )
            .isEqualTo(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
    }

    @Test
    fun getSupportedAfMode_returnsAfModeAutoIfSupported_whenNoPreferredModeAndContinuousPicture() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                    intArrayOf(
                        CameraMetadata.CONTROL_AF_MODE_AUTO,
                        CameraMetadata.CONTROL_AF_MODE_OFF
                    )
            )
        )

        assertThat(
                cameraMetadata.getSupportedAfMode(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            )
            .isEqualTo(CameraMetadata.CONTROL_AF_MODE_AUTO)
    }

    @Test
    fun getSupportedAfMode_returnsAfModeOff_whenNoPreferredModeAndContinuousPictureAndAfModeAuto() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                    intArrayOf(CameraMetadata.CONTROL_AF_MODE_OFF)
            )
        )

        assertThat(
                cameraMetadata.getSupportedAfMode(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            )
            .isEqualTo(CameraMetadata.CONTROL_AF_MODE_OFF)
    }

    @Test
    fun getSupportedAwbMode_returnsPreferredMode_whenSupported() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES to
                    intArrayOf(
                        CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT,
                        CameraMetadata.CONTROL_AWB_MODE_AUTO,
                        CameraMetadata.CONTROL_AWB_MODE_OFF
                    )
            )
        )

        assertThat(cameraMetadata.getSupportedAwbMode(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT))
            .isEqualTo(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT)
    }

    @Test
    fun getSupportedAwbMode_returnsAwbModeAutoIfSupported_whenPreferredModeNotSupported() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES to
                    intArrayOf(
                        CameraMetadata.CONTROL_AWB_MODE_AUTO,
                        CameraMetadata.CONTROL_AWB_MODE_OFF
                    )
            )
        )

        assertThat(cameraMetadata.getSupportedAwbMode(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT))
            .isEqualTo(CameraMetadata.CONTROL_AWB_MODE_AUTO)
    }

    @Test
    fun getSupportedAwbMode_returnsAwbModeOff_whenPreferredModeAndAwbModeAutoNotSupported() {
        initCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES to
                    intArrayOf(CameraMetadata.CONTROL_AWB_MODE_OFF)
            )
        )

        assertThat(cameraMetadata.getSupportedAwbMode(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT))
            .isEqualTo(CameraMetadata.CONTROL_AWB_MODE_OFF)
    }
}
