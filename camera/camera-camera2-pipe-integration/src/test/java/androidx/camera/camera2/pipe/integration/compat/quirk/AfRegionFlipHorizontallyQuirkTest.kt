/*
 * Copyright 2022 The Android Open Source Project
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
class AfRegionFlipHorizontallyQuirkTest(
    private val brand: String,
    private val lensFacing: Int,
    private val quirkEnablingExpected: Boolean
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Brand: {0}, LensFacing = {1}")
        fun data() =
            listOf(
                arrayOf("Samsung", CameraCharacteristics.LENS_FACING_BACK, false),
                arrayOf("Samsung", CameraCharacteristics.LENS_FACING_FRONT, true),
                arrayOf("SAMSUNG", CameraCharacteristics.LENS_FACING_FRONT, true),
                arrayOf("Google", CameraCharacteristics.LENS_FACING_BACK, false),
                arrayOf("Google", CameraCharacteristics.LENS_FACING_FRONT, false),
                arrayOf("Moto", CameraCharacteristics.LENS_FACING_BACK, false),
            )
    }

    private fun getCameraQuirks(lensFacing: Int): Quirks {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(CameraCharacteristics.LENS_FACING, lensFacing)

        val cameraMetadata =
            FakeCameraMetadata(
                characteristics = mapOf(CameraCharacteristics.LENS_FACING to lensFacing)
            )

        return CameraQuirks(
                cameraMetadata,
                StreamConfigurationMapCompat(
                    StreamConfigurationMapBuilder.newBuilder().build(),
                    OutputSizesCorrector(
                        cameraMetadata,
                        StreamConfigurationMapBuilder.newBuilder().build()
                    )
                )
            )
            .quirks
    }

    @Test
    @Config(maxSdk = 32)
    fun canEnableQuirkCorrectly() {
        // Arrange
        ShadowBuild.setBrand(brand)
        ShadowBuild.setModel("DO NOT CARE")
        ShadowBuild.setDevice("DO NOT CARE")

        // Act
        val cameraQuirks = getCameraQuirks(lensFacing)

        // Verify
        Truth.assertThat(cameraQuirks.contains(AfRegionFlipHorizontallyQuirk::class.java))
            .isEqualTo(quirkEnablingExpected)
    }

    @Test
    @Config(minSdk = 33)
    fun canDisableQuirkOnSamsungAPI33() {
        // Arrange
        ShadowBuild.setBrand(brand)
        ShadowBuild.setModel("DO NOT CARE")
        ShadowBuild.setDevice("DO NOT CARE")

        // Act
        val cameraQuirks = getCameraQuirks(lensFacing)

        // Verify
        Truth.assertThat(cameraQuirks.contains(AfRegionFlipHorizontallyQuirk::class.java))
            .isEqualTo(false)
    }
}
