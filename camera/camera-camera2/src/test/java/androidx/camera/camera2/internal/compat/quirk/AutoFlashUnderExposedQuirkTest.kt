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

package androidx.camera.camera2.internal.compat.quirk

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
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

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class AutoFlashUnderExposedQuirkTest(
    private val model: String,
    private val lensFacing: Int,
    private val enabled: Boolean
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Model: {0}")
        fun data() = listOf(
            arrayOf("Pixel 3a", CameraCharacteristics.LENS_FACING_BACK, true),
            arrayOf("Pixel 3a", CameraCharacteristics.LENS_FACING_FRONT, false),
            arrayOf("Pixel 3a XL", CameraCharacteristics.LENS_FACING_BACK, true),
            arrayOf("Pixel 4", CameraCharacteristics.LENS_FACING_BACK, false),
            arrayOf("Samsung S7", CameraCharacteristics.LENS_FACING_BACK, false),
        )
    }

    private fun getCameraQuirks(
        lensFacing: Int
    ): Quirks {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(
            CameraCharacteristics.LENS_FACING,
            lensFacing
        )
        val characteristicsCompat =
            CameraCharacteristicsCompat.toCameraCharacteristicsCompat(characteristics)
        return CameraQuirks.get("0" /* don't care */, characteristicsCompat)
    }

    @Test
    fun canEnableQuirkCorrectly() {
        // Arrange
        ShadowBuild.setModel(model)
        ShadowBuild.setManufacturer("Google") // don't care
        ShadowBuild.setDevice("device") // don't care

        // Act
        val cameraQuirks = getCameraQuirks(lensFacing)

        // Verify
        assertThat(cameraQuirks.contains(AutoFlashUnderExposedQuirk::class.java)).isEqualTo(enabled)
    }
}