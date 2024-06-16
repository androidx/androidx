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
import android.hardware.camera2.CameraMetadata
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.Quirks
import com.google.common.truth.Truth
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
class TorchFlashRequiredFor3aUpdateQuirkTest(
    private val model: String,
    private val lensFacing: Int,
    private val externalFlashAeModeSupported: Boolean,
    private val enabled: Boolean
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "Model: {0}, lens facing: {1}, external ae mode: {2}, enabled: {3}"
        )
        fun data() =
            listOf(
                arrayOf("Pixel 3a", CameraCharacteristics.LENS_FACING_FRONT, false, false),
                arrayOf("Pixel 4", CameraCharacteristics.LENS_FACING_FRONT, true, false),
                arrayOf("Pixel 6", CameraCharacteristics.LENS_FACING_FRONT, false, false),
                arrayOf("Pixel 6A", CameraCharacteristics.LENS_FACING_BACK, false, false),
                arrayOf("Pixel 6A", CameraCharacteristics.LENS_FACING_FRONT, false, true),
                arrayOf("Pixel 7 pro", CameraCharacteristics.LENS_FACING_FRONT, false, true),
                arrayOf("Pixel 8", CameraCharacteristics.LENS_FACING_FRONT, false, true),
                arrayOf("SM-A320FL", CameraCharacteristics.LENS_FACING_FRONT, false, false),
            )
    }

    private fun getCameraQuirks(
        lensFacing: Int,
        externalFlashAeModeSupported: Boolean,
    ): Quirks {
        val characteristicsMap =
            mutableMapOf<CameraCharacteristics.Key<*>, Any?>()
                .apply {
                    this[CameraCharacteristics.LENS_FACING] = lensFacing

                    this[CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES] =
                        if (externalFlashAeModeSupported) {
                            intArrayOf(CameraMetadata.CONTROL_AE_MODE_ON_EXTERNAL_FLASH)
                        } else intArrayOf(CameraMetadata.CONTROL_AE_MODE_ON)
                }
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
        ShadowBuild.setModel(model)
        val cameraQuirks = getCameraQuirks(lensFacing, externalFlashAeModeSupported)

        // Act
        val isFlashModeTorchRequired =
            cameraQuirks
                .get(TorchFlashRequiredFor3aUpdateQuirk::class.java)
                ?.isFlashModeTorchRequired() ?: false

        // Verify
        Truth.assertThat(isFlashModeTorchRequired).isEqualTo(enabled)
    }
}
