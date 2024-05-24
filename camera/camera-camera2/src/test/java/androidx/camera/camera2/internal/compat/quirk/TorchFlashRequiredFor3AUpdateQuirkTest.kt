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

package androidx.camera.camera2.internal.compat.quirk

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_EXTERNAL_FLASH
import android.os.Build
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
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

private const val CAMERA_ID_0 = "0"

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class TorchFlashRequiredFor3AUpdateQuirkTest(
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
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(CameraCharacteristics.LENS_FACING, lensFacing)
        shadowCharacteristics.set(
            CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES,
            if (externalFlashAeModeSupported) {
                intArrayOf(CONTROL_AE_MODE_ON_EXTERNAL_FLASH)
            } else intArrayOf(CONTROL_AE_MODE_ON)
        )
        val characteristicsCompat =
            CameraCharacteristicsCompat.toCameraCharacteristicsCompat(characteristics, CAMERA_ID_0)
        return CameraQuirks.get(CAMERA_ID_0, characteristicsCompat)
    }

    @Test
    fun canEnableQuirkCorrectly() {
        // Arrange
        ShadowBuild.setModel(model)

        // Act
        val cameraQuirks = getCameraQuirks(lensFacing, externalFlashAeModeSupported)

        // Verify
        Truth.assertThat(
                cameraQuirks
                    .get(TorchFlashRequiredFor3aUpdateQuirk::class.java)
                    ?.isFlashModeTorchRequired ?: false
            )
            .isEqualTo(enabled)
    }
}
