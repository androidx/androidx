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

package androidx.camera.camera2.pipe.integration.compat.quirk

import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.internal.compat.quirk.OnePixelShiftQuirk
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class YuvImageOnePixelShiftQuirkTest(
    private val brand: String,
    private val model: String,
    private val quirkEnablingExpected: Boolean
) {
    @Test
    fun canEnableOnePixelShiftQuirkCorrectly() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setModel(model)

        val cameraQuirks =
            CameraQuirks(
                    FakeCameraMetadata(),
                    StreamConfigurationMapCompat(
                        StreamConfigurationMapBuilder.newBuilder().build(),
                        OutputSizesCorrector(
                            FakeCameraMetadata(),
                            StreamConfigurationMapBuilder.newBuilder().build()
                        )
                    )
                )
                .quirks

        assertThat(cameraQuirks.contains(OnePixelShiftQuirk::class.java))
            .isEqualTo(quirkEnablingExpected)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Brand: {0}, Model: {1}")
        fun data() =
            listOf(
                arrayOf("motorola", "MotoG3", true),
                arrayOf("samsung", "SM-G532F", true),
                arrayOf("samsung", "SM-J700F", true),
                arrayOf("motorola", "MotoG100", false),
            )
    }
}
