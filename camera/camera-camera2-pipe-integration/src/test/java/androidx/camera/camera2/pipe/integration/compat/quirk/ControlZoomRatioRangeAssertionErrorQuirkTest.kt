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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class ControlZoomRatioRangeAssertionErrorQuirkTest(
    private val brand: String,
    private val model: String,
    private val quirkEnablingExpected: Boolean
) {
    @Test
    fun canEnableControlZoomRatioRangeAssertionErrorQuirkCorrectly() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setModel(model)

        assertThat(DeviceQuirks[ControlZoomRatioRangeAssertionErrorQuirk::class.java] != null)
            .isEqualTo(quirkEnablingExpected)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Brand: {0}, Model: {1}")
        fun data() =
            listOf(
                arrayOf("jio", "LS1542QWN", true),
                arrayOf("samsung", "SM-A025M/DS", true),
                arrayOf("Samsung", "SM-S124DL", true),
                arrayOf("vivo", "vivo 2039", true),
                arrayOf("motorola", "MotoG100", false),
            )
    }
}
