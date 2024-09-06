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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.Quirks
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class QuickSuccessiveImageCaptureFailsRepeatingRequestQuirkTest(
    private val brand: String,
    private val cameraHwLevel: Int,
    private val isEnabledExpected: Boolean
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "Model: {0}, lens facing: {1}, external ae mode: {2}, enabled: {3}"
        )
        fun data() =
            listOf(
                arrayOf("Samsung", INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, true),
                arrayOf("Samsung", INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, false),
                arrayOf("Samsung", INFO_SUPPORTED_HARDWARE_LEVEL_FULL, false),
                arrayOf("Samsung", INFO_SUPPORTED_HARDWARE_LEVEL_3, false),
                arrayOf("Samsung", INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL, false),
                arrayOf("Google", INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, false),
            )
    }

    private fun getCameraQuirks(
        cameraHwLevel: Int,
    ): Quirks {
        val characteristicsMap =
            mutableMapOf<CameraCharacteristics.Key<*>, Any?>()
                .apply { this[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] = cameraHwLevel }
                .toMap()

        val cameraCharacteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics =
            Shadow.extract<ShadowCameraCharacteristics>(cameraCharacteristics)
        characteristicsMap.forEach { entry -> shadowCharacteristics.set(entry.key, entry.value) }

        val cameraMetadata = FakeCameraMetadata(characteristicsMap)

        return CameraQuirks(
                cameraMetadata,
                StreamConfigurationMapCompat(
                    StreamConfigurationMapBuilder.newBuilder().build(),
                    OutputSizesCorrector(
                        cameraMetadata,
                        StreamConfigurationMapBuilder.newBuilder().build()
                    )
                ),
            )
            .quirks
    }

    @Test
    fun canEnableQuirkCorrectly() {
        // Arrange
        ShadowBuild.setBrand(brand)
        val cameraQuirks = getCameraQuirks(cameraHwLevel)

        // Act
        val isEnabled =
            cameraQuirks.contains(QuickSuccessiveImageCaptureFailsRepeatingRequestQuirk::class.java)

        // Verify
        assertThat(isEnabled).isEqualTo(isEnabledExpected)
    }
}
