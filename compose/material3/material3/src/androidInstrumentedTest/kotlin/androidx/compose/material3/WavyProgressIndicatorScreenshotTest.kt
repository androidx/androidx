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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class WavyProgressIndicatorScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)
    private val wrapperTestTag = "progressIndicatorWrapper"

    @Test
    fun linearWavyProgressIndicator_indeterminate() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { LinearWavyProgressIndicator() }
        }
        rule.mainClock.advanceTimeBy(1200)
        assertIndicatorAgainstGolden("linearWavyProgressIndicator_indeterminate_${scheme.name}")
    }

    @Test
    fun linearWavyProgressIndicator_initialState_indeterminate() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { LinearWavyProgressIndicator() }
        }
        // Compare the screenshot without advancing the clock.
        assertIndicatorAgainstGolden(
            "linearWavyProgressIndicator_initialState_indeterminate_${scheme.name}"
        )
    }

    @Test
    fun linearWavyProgressIndicator_indeterminate_lowerAmplitude() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { LinearWavyProgressIndicator(amplitude = 0.5f) }
        }
        rule.mainClock.advanceTimeBy(1200)
        assertIndicatorAgainstGolden(
            "linearWavyProgressIndicator_indeterminate_lowerAmplitude_${scheme.name}"
        )
    }

    @Test
    fun linearWavyProgressIndicator_midProgress_determinate() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { LinearWavyProgressIndicator(progress = { 0.5f }) }
        }
        assertIndicatorAgainstGolden(
            "linearWavyProgressIndicator_midProgress_determinate_${scheme.name}"
        )
    }

    @Test
    fun linearWavyProgressIndicator_lowProgress_determinate() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { LinearWavyProgressIndicator(progress = { 0.09f }) }
        }
        assertIndicatorAgainstGolden(
            "linearWavyProgressIndicator_lowProgress_determinate_${scheme.name}"
        )
    }

    @Test
    fun linearWavyProgressIndicator_highProgress_determinate() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { LinearWavyProgressIndicator(progress = { 0.95f }) }
        }
        assertIndicatorAgainstGolden(
            "linearWavyProgressIndicator_highProgress_determinate_${scheme.name}"
        )
    }

    @Test
    fun linearWavyProgressIndicator_indeterminate_rtl() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(wrap.testTag(wrapperTestTag)) { LinearWavyProgressIndicator() }
            }
        }
        rule.mainClock.advanceTimeBy(1200)
        assertIndicatorAgainstGolden("linearWavyProgressIndicator_indeterminate_rtl_${scheme.name}")
    }

    @Test
    fun linearWavyProgressIndicator_determinate_rtl() {
        rule.setMaterialContent(scheme.colorScheme) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(wrap.testTag(wrapperTestTag)) {
                    LinearWavyProgressIndicator(progress = { 0.5f })
                }
            }
        }
        assertIndicatorAgainstGolden("linearWavyProgressIndicator_determinate_rtl_${scheme.name}")
    }

    @Test
    fun linearWavyProgressIndicator_determinate_customStroke() {
        rule.setMaterialContent(scheme.colorScheme) {
            val strokeWidth = with(LocalDensity.current) { 8.dp.toPx() }
            Box(wrap.testTag(wrapperTestTag)) {
                LinearWavyProgressIndicator(
                    progress = { 0.5f },
                    stroke = Stroke(width = strokeWidth, cap = StrokeCap.Square)
                )
            }
        }
        assertIndicatorAgainstGolden(
            "linearWavyProgressIndicator_determinate_customStroke_${scheme.name}"
        )
    }

    @Test
    fun linearWavyProgressIndicator_determinate_thick() {
        rule.setMaterialContent(scheme.colorScheme) {
            val thickStroke =
                Stroke(width = with(LocalDensity.current) { 8.dp.toPx() }, cap = StrokeCap.Round)
            Box(wrap.testTag(wrapperTestTag)) {
                LinearWavyProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier.height(14.dp),
                    stroke = thickStroke,
                    trackStroke = thickStroke
                )
            }
        }
        assertIndicatorAgainstGolden("linearWavyProgressIndicator_determinate_thick_${scheme.name}")
    }

    @Test
    fun circularWavyProgressIndicator_midProgress_determinate() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { CircularWavyProgressIndicator(progress = { 0.7f }) }
        }
        assertIndicatorAgainstGolden(
            "circularWavyProgressIndicator_midProgress_determinate_${scheme.name}"
        )
    }

    @Test
    fun circularWavyProgressIndicator_lowProgress_determinate() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularWavyProgressIndicator(progress = { 0.09f })
            }
        }
        assertIndicatorAgainstGolden(
            "circularWavyProgressIndicator_lowProgress_determinate_${scheme.name}"
        )
    }

    @Test
    fun circularWavyProgressIndicator_highProgress_determinate() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularWavyProgressIndicator(progress = { 0.95f })
            }
        }
        assertIndicatorAgainstGolden(
            "circularWavyProgressIndicator_highProgress_determinate_${scheme.name}"
        )
    }

    @Test
    fun circularWavyProgressIndicator_determinate_no_gap() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularWavyProgressIndicator(progress = { 0.7f }, gapSize = 0.dp)
            }
        }
        assertIndicatorAgainstGolden(
            "circularWavyProgressIndicator_determinate_no_gap_${scheme.name}"
        )
    }

    @Test
    fun circularWavyProgressIndicator_determinate_thick() {
        rule.setMaterialContent(scheme.colorScheme) {
            val thickStroke =
                Stroke(width = with(LocalDensity.current) { 8.dp.toPx() }, cap = StrokeCap.Round)
            Box(wrap.testTag(wrapperTestTag)) {
                CircularWavyProgressIndicator(
                    progress = { 0.7f },
                    modifier = Modifier.size(52.dp),
                    stroke = thickStroke,
                    trackStroke = thickStroke
                )
            }
        }
        assertIndicatorAgainstGolden(
            "circularWavyProgressIndicator_determinate_thick_${scheme.name}"
        )
    }

    @Test
    fun circularWavyProgressIndicator_indeterminate() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { CircularWavyProgressIndicator() }
        }
        rule.mainClock.advanceTimeBy(500)
        assertIndicatorAgainstGolden("circularWavyProgressIndicator_indeterminate_${scheme.name}")
    }

    @Test
    fun circularWavyProgressIndicator_indeterminate_start() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { CircularWavyProgressIndicator() }
        }
        rule.mainClock.advanceTimeBy(0)
        assertIndicatorAgainstGolden(
            "circularWavyProgressIndicator_lightTheme_indeterminate_start_${scheme.name}"
        )
    }

    @Test
    fun circularWavyProgressIndicator_indeterminate_lowerAmplitude() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { CircularWavyProgressIndicator(amplitude = 0.5f) }
        }
        rule.mainClock.advanceTimeBy(500)
        assertIndicatorAgainstGolden(
            "circularWavyProgressIndicator_indeterminate_lowerAmplitude_${scheme.name}"
        )
    }

    @Test
    fun circularWavyProgressIndicator_determinate_customCapAndTrack() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularWavyProgressIndicator(
                    progress = { 0.4f },
                    trackColor = Color.Gray,
                    stroke = Stroke(width = 6f, cap = StrokeCap.Butt)
                )
            }
        }
        assertIndicatorAgainstGolden(
            "circularWavyProgressIndicator_determinate_customCapAndTrack_${scheme.name}"
        )
    }

    private fun assertIndicatorAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated name is as
    // expected.
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("lightTheme", lightColorScheme()),
                ColorSchemeWrapper("darkTheme", darkColorScheme()),
            )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}
