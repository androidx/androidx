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

package androidx.compose.ui.text.googlefonts

import android.content.res.Configuration
import android.graphics.fonts.FontVariationAxis
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontVariation.Settings
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
class FontVariationSettingsTest {

    /**
     * Helper class for better test case readability. See [fontVariationAdjustment] for actual test
     * cases.
     */
    @RequiresApi(26)
    class Matcher(val adjustedVariationSettingsString: String, val density: Density) {

        fun isEqualTo(vararg settings: FontVariation.Setting) {
            val expected = mutableMapOf<String, Float>()
            settings.forEach { expected[it.axisName] = it.toVariationValue(density) }

            if (adjustedVariationSettingsString.isEmpty()) {
                assertThat(expected).isEmpty()
                return
            }

            val adjustedFromString = mutableMapOf<String, Float>()
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
        return Matcher(base.toAndroidString(density, adjustment), density)
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

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun fontVariationAdjustment_zeroAdjustment() {
        with(FontVariation) {
            // If adjustment is 0, the settings should not be changed.
            assertAdjustment(Setting("wght", 400f), adjustment = 0)
                .isEqualTo(Setting("wght", 400.0f))
            assertAdjustment(Setting("slnt", -10f), adjustment = 0)
                .isEqualTo(Setting("slnt", -10.0f))
            assertAdjustment(Setting("slnt", -10f), Setting("wght", 400f), adjustment = 0)
                .isEqualTo(Setting("slnt", -10.0f), Setting("wght", 400.0f))
            assertAdjustment(adjustment = 0).isEqualTo()
        }
    }

    @Test
    fun getFontWeightAdjustment_returnsZero_whenContextIsNull() {
        assertThat(getFontWeightAdjustment(null)).isEqualTo(0)
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun getFontWeightAdjustment_returnsAdjustment_fromConfiguration() {
        val config = Configuration().apply { fontWeightAdjustment = 200 }
        val context =
            InstrumentationRegistry.getInstrumentation().context.createConfigurationContext(config)
        assertThat(getFontWeightAdjustment(context)).isEqualTo(200)
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun getFontWeightAdjustment_returnsZero_whenUndefined() {
        val config =
            Configuration().apply {
                fontWeightAdjustment = Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED
            }
        val context =
            InstrumentationRegistry.getInstrumentation().context.createConfigurationContext(config)
        assertThat(getFontWeightAdjustment(context)).isEqualTo(0)
    }

    @Test
    fun toAndroidString_zeroAdjustment() {
        val density = Density(1.0f)
        with(FontVariation) {
            assertThat(Settings(Setting("wght", 400f)).toAndroidString(density, 0))
                .isEqualTo("'wght' 400.0")

            assertThat(Settings(Setting("slnt", -10f)).toAndroidString(density, 0))
                .isEqualTo("'slnt' -10.0")

            assertThat(
                    Settings(Setting("slnt", -10f), Setting("wght", 400f))
                        .toAndroidString(density, 0)
                )
                .isEqualTo("'slnt' -10.0, 'wght' 400.0")

            assertThat(Settings().toAndroidString(density, 0)).isEqualTo("")
        }
    }

    @Test
    fun toAndroidString_withAdjustment() {
        val density = Density(1.0f)
        with(FontVariation) {
            assertThat(Settings(Setting("wght", 400f)).toAndroidString(density, 300))
                .isEqualTo("'wght' 700.0")

            assertThat(Settings(Setting("slnt", -10f)).toAndroidString(density, 300))
                .isEqualTo("'slnt' -10.0,'wght' 700.0")

            assertThat(
                    Settings(Setting("slnt", -10f), Setting("wght", 400f))
                        .toAndroidString(density, 300)
                )
                .isEqualTo("'slnt' -10.0,'wght' 700.0")

            assertThat(Settings().toAndroidString(density, 300)).isEqualTo("'wght' 700.0")
        }
    }
}
