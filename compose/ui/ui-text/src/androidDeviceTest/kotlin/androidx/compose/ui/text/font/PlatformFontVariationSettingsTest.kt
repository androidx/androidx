/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.text.font

import android.graphics.fonts.FontVariationAxis
import androidx.annotation.RequiresApi
import androidx.collection.mutableObjectFloatMapOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlatformFontVariationSettingsTest {
    val context = InstrumentationRegistry.getInstrumentation().context

    /**
     * Helper class for better test case readability. See [fontVariationAdjustment] for actual test
     * cases.
     */
    @RequiresApi(26)
    class Matcher(
        val adjustedVariationSettingsString: String,
        val adjustedVariationSettingsArray: Array<FontVariationAxis>,
        val density: Density,
    ) {

        fun isEqualTo(vararg settings: FontVariation.Setting) {
            val expected = mutableObjectFloatMapOf<String>()
            settings.forEach { expected[it.axisName] = it.toVariationValue(density) }

            val adjustedFromArray = mutableObjectFloatMapOf<String>()
            adjustedVariationSettingsArray.forEach { adjustedFromArray[it.tag] = it.styleValue }
            assertThat(adjustedFromArray).isEqualTo(expected)

            val adjustedFromString = mutableObjectFloatMapOf<String>()
            val convertedAdjusted =
                requireNotNull(
                    FontVariationAxis.fromFontVariationSettings(adjustedVariationSettingsString)
                )
            convertedAdjusted.forEach { adjustedFromString[it.tag] = it.styleValue }
            assertThat(adjustedFromString).isEqualTo(expected)
        }
    }

    /**
     * Execute tests that adjust the given variation settings for both string output and array
     * output.
     */
    @OptIn(ExperimentalTextApi::class)
    @RequiresApi(26)
    private fun assertAdjustment(vararg settings: FontVariation.Setting, adjustment: Int): Matcher {
        val density = Density(1.0f)
        val base = FontVariation.Settings(*settings)
        return Matcher(
            base.toAndroidString(density, adjustment),
            base.toAndroidArray(density, adjustment),
            density,
        )
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun fontVariationAdjustment() {
        with(FontVariation) {
            // If weight is specified in the font variation settings, it should be adjusted.
            assertAdjustment(Setting("wght", 400f), adjustment = 300)
                .isEqualTo(Setting("wght", 700.0f))

            // If weight is not specified, it is adjusted with assuming weight 400.
            assertAdjustment(adjustment = 300).isEqualTo(Setting("wght", 700.0f))

            // Unrelated axes should be preserved.
            assertAdjustment(Setting("slnt", -10f), Setting("wght", 400f), adjustment = 300)
                .isEqualTo(Setting("slnt", -10f), Setting("wght", 700.0f))
            assertAdjustment(Setting("slnt", -10f), adjustment = 300)
                .isEqualTo(Setting("slnt", -10f), Setting("wght", 700.0f))

            // If the weight exceeds the maximum allowed value, it should be clamped.
            assertAdjustment(Setting("wght", 800.0f), adjustment = 300)
                .isEqualTo(Setting("wght", 1000.0f))

            // If the weight exceeds the minimum allowed value, it should be clamped.
            assertAdjustment(Setting("wght", 400.0f), adjustment = -700)
                .isEqualTo(Setting("wght", 1.0f))
        }
    }
}
