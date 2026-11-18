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

package androidx.xr.glimmer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class LinearProgressIndicatorScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun linearProgressIndicator_determinate_progress0() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox { LinearProgressIndicator(progress = { 0f }) }
        }
        rule.assertRootAgainstGolden(
            "linearProgressIndicator_determinate_progress0",
            screenshotRule,
        )
    }

    @Test
    fun linearProgressIndicator_determinate_progress25() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox { LinearProgressIndicator(progress = { 0.25f }) }
        }
        rule.assertRootAgainstGolden(
            "linearProgressIndicator_determinate_progress25",
            screenshotRule,
        )
    }

    @Test
    fun linearProgressIndicator_determinate_progress50() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox { LinearProgressIndicator(progress = { 0.5f }) }
        }
        rule.assertRootAgainstGolden(
            "linearProgressIndicator_determinate_progress50",
            screenshotRule,
        )
    }

    @Test
    fun linearProgressIndicator_determinate_progress100() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox { LinearProgressIndicator(progress = { 1f }) }
        }
        rule.assertRootAgainstGolden(
            "linearProgressIndicator_determinate_progress100",
            screenshotRule,
        )
    }

    @Test
    fun linearProgressIndicator_determinate_progress25_rtl() {
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ProgressIndicatorScreenshotBox { LinearProgressIndicator(progress = { 0.25f }) }
            }
        }
        rule.assertRootAgainstGolden(
            "linearProgressIndicator_determinate_progress25_rtl",
            screenshotRule,
        )
    }

    @Test
    fun linearProgressIndicator_determinate_progress50_noGap() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox {
                LinearProgressIndicator(progress = { 0.5f }, gapSize = 0.dp)
            }
        }
        rule.assertRootAgainstGolden(
            "linearProgressIndicator_determinate_progress50_noGap",
            screenshotRule,
        )
    }

    @Test
    fun linearProgressIndicator_indeterminate_300ms() {
        rule.setGlimmerThemeContent { ProgressIndicatorScreenshotBox { LinearProgressIndicator() } }
        rule.mainClock.advanceTimeBy(300)
        rule.assertRootAgainstGolden("linearProgressIndicator_indeterminate_300ms", screenshotRule)
    }

    @Test
    fun linearProgressIndicator_indeterminate_900ms() {
        rule.setGlimmerThemeContent { ProgressIndicatorScreenshotBox { LinearProgressIndicator() } }
        rule.mainClock.advanceTimeBy(900)
        rule.assertRootAgainstGolden("linearProgressIndicator_indeterminate_900ms", screenshotRule)
    }

    @Test
    fun linearWavyProgressIndicator_determinate_progress0() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox { LinearWavyProgressIndicator(progress = { 0f }) }
        }
        rule.mainClock.advanceTimeBy(WaveTimeMillis)
        rule.assertRootAgainstGolden(
            "linearWavyProgressIndicator_determinate_progress0",
            screenshotRule,
        )
    }

    @Test
    fun linearWavyProgressIndicator_determinate_progress25() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox { LinearWavyProgressIndicator(progress = { 0.25f }) }
        }
        rule.mainClock.advanceTimeBy(WaveTimeMillis)
        rule.assertRootAgainstGolden(
            "linearWavyProgressIndicator_determinate_progress25",
            screenshotRule,
        )
    }

    @Test
    fun linearWavyProgressIndicator_determinate_progress50() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox { LinearWavyProgressIndicator(progress = { 0.5f }) }
        }
        rule.mainClock.advanceTimeBy(WaveTimeMillis)
        rule.assertRootAgainstGolden(
            "linearWavyProgressIndicator_determinate_progress50",
            screenshotRule,
        )
    }

    @Test
    fun linearWavyProgressIndicator_determinate_progress100() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox { LinearWavyProgressIndicator(progress = { 1f }) }
        }
        rule.mainClock.advanceTimeBy(WaveTimeMillis)
        rule.assertRootAgainstGolden(
            "linearWavyProgressIndicator_determinate_progress100",
            screenshotRule,
        )
    }

    @Test
    fun linearWavyProgressIndicator_determinate_progress25_rtl() {
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ProgressIndicatorScreenshotBox { LinearWavyProgressIndicator(progress = { 0.25f }) }
            }
        }
        rule.mainClock.advanceTimeBy(WaveTimeMillis)
        rule.assertRootAgainstGolden(
            "linearWavyProgressIndicator_determinate_progress25_rtl",
            screenshotRule,
        )
    }

    @Test
    fun linearWavyProgressIndicator_determinate_progress50_noGap() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox {
                LinearWavyProgressIndicator(progress = { 0.5f }, gapSize = 0.dp)
            }
        }
        rule.mainClock.advanceTimeBy(WaveTimeMillis)
        rule.assertRootAgainstGolden(
            "linearWavyProgressIndicator_determinate_progress50_noGap",
            screenshotRule,
        )
    }

    @Test
    fun linearWavyProgressIndicator_indeterminate_300ms() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox { LinearWavyProgressIndicator() }
        }
        rule.mainClock.advanceTimeBy(300)
        rule.assertRootAgainstGolden(
            "linearWavyProgressIndicator_indeterminate_300ms",
            screenshotRule,
        )
    }

    @Test
    fun linearWavyProgressIndicator_indeterminate_900ms() {
        rule.setGlimmerThemeContent {
            ProgressIndicatorScreenshotBox { LinearWavyProgressIndicator() }
        }
        rule.mainClock.advanceTimeBy(900)
        rule.assertRootAgainstGolden(
            "linearWavyProgressIndicator_indeterminate_900ms",
            screenshotRule,
        )
    }

    /**
     * Adds space around the indicator, so screenshots are easier to see vs a tightly cropped
     * progress indicator line.
     */
    @Composable
    private fun ProgressIndicatorScreenshotBox(content: @Composable () -> Unit) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

// A quarter of the way through the wave's movement.
private const val WaveTimeMillis = 250L
