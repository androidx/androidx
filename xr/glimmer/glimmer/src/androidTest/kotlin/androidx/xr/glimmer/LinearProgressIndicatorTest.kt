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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.testutils.captureToImage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class LinearProgressIndicatorTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun determinate_progress() {
        val progress = mutableFloatStateOf(0f)
        rule.setGlimmerThemeContent {
            LinearProgressIndicator(
                progress = { progress.floatValue },
                modifier = Modifier.testTag(IndicatorTag),
            )
        }

        rule.onNodeWithTag(IndicatorTag).assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnIdle { progress.floatValue = 0.5f }

        rule.onNodeWithTag(IndicatorTag).assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun determinate_progressIsCoercedInBounds() {
        val progress = mutableFloatStateOf(-1f)
        rule.setGlimmerThemeContent {
            LinearProgressIndicator(
                progress = { progress.floatValue },
                modifier = Modifier.testTag(IndicatorTag),
            )
        }

        rule.onNodeWithTag(IndicatorTag).assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnIdle { progress.floatValue = 1.5f }

        rule.onNodeWithTag(IndicatorTag).assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun determinate_nanProgress() {
        rule.setGlimmerThemeContent {
            LinearProgressIndicator(
                progress = { Float.NaN },
                modifier = Modifier.testTag(IndicatorTag),
            )
        }

        rule.onNodeWithTag(IndicatorTag).assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))
    }

    @Test
    fun determinate_defaultSize() {
        setContentInContainer { LinearProgressIndicator(progress = { 0f }) }
            .assertWidthIsEqualTo(ExpectedDefaultWidth)
            .assertHeightIsEqualTo(ExpectedDefaultHeight)
    }

    @Test
    fun determinate_modifierOverridesDefaultWidth() {
        rule.setGlimmerThemeContent {
            Box(Modifier.width(200.dp)) {
                LinearProgressIndicator(
                    progress = { 0f },
                    modifier = Modifier.testTag(IndicatorTag).fillMaxWidth(),
                )
            }
        }

        rule.onNodeWithTag(IndicatorTag).assertWidthIsEqualTo(200.dp)
    }

    @Test
    fun determinate_defaultTrackColor_isLowEmphasisContentColor() {
        setContentInContainer { LinearProgressIndicator(progress = { 0f }) }
            .assertCenterColor(calculateLowEmphasisContentColor(Color.White))
    }

    @Test
    fun determinate_defaultTrackColor_followsContentColor() {
        setContentInContainer {
                Box(Modifier.contentColorProvider(Color.Black)) {
                    LinearProgressIndicator(progress = { 0f })
                }
            }
            .assertCenterColor(calculateLowEmphasisContentColor(Color.Black))
    }

    @Test
    fun determinate_trackColor() {
        setContentInContainer { LinearProgressIndicator(progress = { 0f }, trackColor = Color.Red) }
            .assertCenterColor(Color.Red)
    }

    @Test
    fun indeterminate_progress() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            LinearProgressIndicator(modifier = Modifier.testTag(IndicatorTag))
        }

        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag(IndicatorTag).assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun indeterminate_defaultSize() {
        rule.mainClock.autoAdvance = false
        val indicator = setContentInContainer { LinearProgressIndicator() }

        rule.mainClock.advanceTimeByFrame()

        indicator
            .assertWidthIsEqualTo(ExpectedDefaultWidth)
            .assertHeightIsEqualTo(ExpectedDefaultHeight)
    }

    @Test
    fun indeterminate_defaultTrackColor_followsContentColor() {
        rule.mainClock.autoAdvance = false
        val container = setContentInContainer {
            Box(Modifier.contentColorProvider(Color.Black)) { LinearProgressIndicator() }
        }

        rule.mainClock.advanceTimeByFrame()

        container.assertCenterColor(calculateLowEmphasisContentColor(Color.Black))
    }

    @Test
    fun wavyDeterminate_progress() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            LinearWavyProgressIndicator(
                progress = { 0.3f },
                modifier = Modifier.testTag(IndicatorTag),
            )
        }

        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag(IndicatorTag).assertRangeInfoEquals(ProgressBarRangeInfo(0.3f, 0f..1f))
    }

    @Test
    fun wavyDeterminate_defaultSize() {
        rule.mainClock.autoAdvance = false
        val indicator = setContentInContainer {
            LinearWavyProgressIndicator(progress = { 0f })
        }

        rule.mainClock.advanceTimeByFrame()

        indicator
            .assertWidthIsEqualTo(ExpectedDefaultWidth)
            .assertHeightIsEqualTo(ExpectedDefaultWavyHeight)
    }

    @Test
    fun wavyDeterminate_strokeWidthDoesNotChangeHeight() {
        rule.mainClock.autoAdvance = false
        val indicator = setContentInContainer {
            LinearWavyProgressIndicator(progress = { 0f }, strokeWidth = 10.dp)
        }

        rule.mainClock.advanceTimeByFrame()

        indicator.assertHeightIsEqualTo(ExpectedDefaultWavyHeight)
    }

    @Test
    fun wavyDeterminate_defaultTrackColor_followsContentColor() {
        rule.mainClock.autoAdvance = false
        val container = setContentInContainer {
            Box(Modifier.contentColorProvider(Color.Black)) {
                LinearWavyProgressIndicator(progress = { 0f })
            }
        }

        rule.mainClock.advanceTimeByFrame()

        container.assertCenterColor(calculateLowEmphasisContentColor(Color.Black))
    }

    @Test
    fun wavyIndeterminate_progress() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            LinearWavyProgressIndicator(modifier = Modifier.testTag(IndicatorTag))
        }

        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag(IndicatorTag).assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun wavyIndeterminate_defaultSize() {
        rule.mainClock.autoAdvance = false
        val indicator = setContentInContainer { LinearWavyProgressIndicator() }

        rule.mainClock.advanceTimeByFrame()

        indicator
            .assertWidthIsEqualTo(ExpectedDefaultWidth)
            .assertHeightIsEqualTo(ExpectedDefaultWavyHeight)
    }

    @Test
    fun wavyIndeterminate_defaultTrackColor_followsContentColor() {
        rule.mainClock.autoAdvance = false
        val container = setContentInContainer {
            Box(Modifier.contentColorProvider(Color.Black)) { LinearWavyProgressIndicator() }
        }

        rule.mainClock.advanceTimeByFrame()

        container.assertCenterColor(calculateLowEmphasisContentColor(Color.Black))
    }

    /** Asserts that the pixel at the center of this node is [expected]. */
    private fun SemanticsNodeInteraction.assertCenterColor(expected: Color) {
        val centerColor = captureToImage().toPixelMap().run { get(width / 2, height / 2) }
        assertThat(centerColor).isEqualTo(expected)
    }

    /**
     * Sets [content] without a maximum size, so the screen size can't shrink the indicator, and
     * returns the box around it. Size and color assertions use this box, because the indicator's
     * own semantics bounds are taller than its layout to meet TalkBack's minimum focus size.
     */
    private fun setContentInContainer(content: @Composable () -> Unit): SemanticsNodeInteraction {
        rule.setGlimmerThemeContent {
            Box(Modifier.wrapContentSize(unbounded = true)) {
                Box(Modifier.testTag(ContainerTag)) { content() }
            }
        }
        return rule.onNodeWithTag(ContainerTag)
    }
}

private const val IndicatorTag = "indicator"

private const val ContainerTag = "container"

private val ExpectedDefaultWidth = 360.dp

private val ExpectedDefaultHeight = 6.dp

private val ExpectedDefaultWavyHeight = 12.dp
