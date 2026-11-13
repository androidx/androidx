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

package androidx.camera.camera2.compat.workaround

import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build
import androidx.camera.camera2.compat.quirk.DolbyVisionStreamIncorrectColorQuirk
import androidx.camera.core.DynamicRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.TIRAMISU)
class DolbyVisionStreamProfileCorrectorTest {

    private val dvOemAndHlg10 =
        DynamicRangeProfiles(
            longArrayOf(
                DynamicRangeProfiles.HLG10,
                0,
                0,
                DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM,
                0,
                0,
            )
        )

    private val dvOemOnly =
        DynamicRangeProfiles(longArrayOf(DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM, 0, 0))

    @Test
    fun dolbyVision10Bit_quirkEnabled_returnsHlg10() {
        val corrector = DolbyVisionStreamProfileCorrector(isQuirkEnabled = true)

        assertThat(
                corrector.getStreamProfileOverride(DynamicRange.DOLBY_VISION_10_BIT, dvOemAndHlg10)
            )
            .isEqualTo(DynamicRangeProfiles.HLG10)
    }

    @Test
    fun dolbyVision10Bit_quirkEnabled_hlg10Unsupported_returnsNull() {
        val corrector = DolbyVisionStreamProfileCorrector(isQuirkEnabled = true)

        assertThat(corrector.getStreamProfileOverride(DynamicRange.DOLBY_VISION_10_BIT, dvOemOnly))
            .isNull()
    }

    @Test
    fun otherDynamicRanges_quirkEnabled_returnsNull() {
        val corrector = DolbyVisionStreamProfileCorrector(isQuirkEnabled = true)

        listOf(
                DynamicRange.SDR,
                DynamicRange.HLG_10_BIT,
                DynamicRange.HDR10_10_BIT,
                DynamicRange.HDR10_PLUS_10_BIT,
                DynamicRange.DOLBY_VISION_8_BIT,
            )
            .forEach {
                assertThat(corrector.getStreamProfileOverride(it, dvOemAndHlg10)).isNull()
            }
    }

    @Test
    fun dolbyVision10Bit_quirkDisabled_returnsNull() {
        val corrector = DolbyVisionStreamProfileCorrector(isQuirkEnabled = false)

        assertThat(
                corrector.getStreamProfileOverride(DynamicRange.DOLBY_VISION_10_BIT, dvOemAndHlg10)
            )
            .isNull()
    }

    @Test
    fun quirkLoadsOnlyOnAffectedDevice() {
        ShadowBuild.setManufacturer("Xiaomi")
        ShadowBuild.setModel("2509FPN0BC")
        assertThat(DolbyVisionStreamIncorrectColorQuirk.load()).isTrue()

        ShadowBuild.setModel("2407FPN8EG")
        assertThat(DolbyVisionStreamIncorrectColorQuirk.load()).isFalse()

        ShadowBuild.setManufacturer("Google")
        ShadowBuild.setBrand("google")
        ShadowBuild.setModel("2509FPN0BC")
        assertThat(DolbyVisionStreamIncorrectColorQuirk.load()).isFalse()
    }
}
