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

package androidx.wear.compose.material

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test

@ExperimentalWearMaterialApi
public class InlineSliderTest {
    @get:Rule
    public val rule = createComposeRule()

    @Test
    public fun supports_testtag() {
        rule.setContentWithTheme {
            InlineSlider(
                value = 1f,
                onValueChange = {},
                steps = 5,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun coerces_value_top_limit() {
        val state = mutableStateOf(4f)

        rule.setContentWithTheme {
            InlineSlider(
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..10f,
                steps = 4,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.runOnIdle {
            state.value = 20f
        }
        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(10f, 0f..10f, 4))
    }

    @Test
    public fun coerces_value_lower_limit() {
        val state = mutableStateOf(4f)

        rule.setContentWithTheme {
            InlineSlider(
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..10f,
                steps = 4,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.runOnIdle {
            state.value = -20f
        }
        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..10f, 4))
    }

    @Test(expected = IllegalArgumentException::class)
    public fun throws_when_steps_negative() {
        rule.setContent {
            InlineSlider(value = 0f, onValueChange = {}, steps = -1)
        }
    }

    @Test
    public fun snaps_value_exactly() {
        val state = mutableStateOf(0f)
        val range = 0f..1f

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                steps = 4,
                valueRange = range
            )
        }

        rule.runOnUiThread {
            state.value = 0.6f
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, range, 4))
    }

    @Test
    public fun snaps_value_to_previous() {
        val state = mutableStateOf(0f)
        val range = 0f..1f

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                steps = 4,
                valueRange = range
            )
        }

        rule.runOnUiThread {
            state.value = 0.65f
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, range, 4))
    }

    @Test
    public fun snaps_value_to_next() {
        val state = mutableStateOf(0f)
        val range = 0f..1f

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                steps = 4,
                valueRange = range
            )
        }

        rule.runOnUiThread {
            state.value = 0.55f
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, range, 4))
    }

    @Test
    public fun decreases_value_by_clicking_left() {
        val state = mutableStateOf(2f)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(1f)
        }
    }

    @Test
    public fun increases_value_by_clicking_right() {
        val state = mutableStateOf(2f)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width - 15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(3f)
        }
    }

    @Test
    public fun ignores_left_click_when_disabled() {
        val state = mutableStateOf(2f)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                enabled = false,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(2f)
        }
    }

    @Test
    public fun ignores_right_click_when_disabled() {
        val state = mutableStateOf(2f)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                enabled = false,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width - 15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(2f)
        }
    }

    @Test
    public fun reaches_min_clicking_left() {
        val state = mutableStateOf(1f)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(1f)
        }
    }

    @Test
    public fun reaches_max_clicking_right() {
        val state = mutableStateOf(4f)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width - 15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(4f)
        }
    }

    @Test
    public fun sets_custom_decrease_icon() {
        val iconTag = "iconTag_test"

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                onValueChange = { },
                decreaseIcon = {
                    Icon(
                        modifier = Modifier.testTag(iconTag),
                        imageVector = Icons.Default.Star,
                        contentDescription = ""
                    )
                }
            )
        }

        rule.waitForIdle()
        rule.onNodeWithTag(iconTag, true)
            .assertExists()
            .assertLeftPositionInRootIsEqualTo(IconsOuterHorizontalMargin)
    }

    @Test
    public fun sets_custom_increase_icon() {
        val iconTag = "iconTag_test"

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                onValueChange = { },
                increaseIcon = {
                    Icon(
                        modifier = Modifier.testTag(iconTag),
                        imageVector = Icons.Default.Star,
                        contentDescription = ""
                    )
                }
            )
        }
        val unclippedBoundsInRoot = rule.onRoot().getUnclippedBoundsInRoot()

        rule.waitForIdle()
        rule.onNodeWithTag(iconTag, true)
            .assertExists()
            .assertLeftPositionInRootIsEqualTo(
                unclippedBoundsInRoot.width -
                    IconsOuterHorizontalMargin - DefaultIconWidth
            )
    }

    private val IconsOuterHorizontalMargin = 8.dp
    private val DefaultIconWidth = 24.dp
}

@ExperimentalWearMaterialApi
public class IntegerInlineSliderTest {
    @get:Rule
    public val rule = createComposeRule()

    @Test
    public fun supports_testtag() {
        rule.setContentWithTheme {
            InlineSlider(
                value = 1,
                onValueChange = {},
                valueProgression = 0..10,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun default_step_in_valueProgression() {
        rule.setContentWithTheme {
            InlineSlider(
                value = 2,
                onValueChange = {},
                valueProgression = 0..10,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(2f, 0f..10f, 9))
    }

    @Test
    public fun custom_valueProgression() {
        rule.setContentWithTheme {
            InlineSlider(
                value = 2,
                onValueChange = {},
                valueProgression = IntProgression.fromClosedRange(0, 10, 2),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(2f, 0f..10f, 4))
    }

    @Test
    public fun valueProgression_trimmed() {
        rule.setContentWithTheme {
            InlineSlider(
                value = 6,
                onValueChange = {},
                valueProgression = IntProgression.fromClosedRange(0, 16, 6),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(6f, 0f..12f, 1))
    }

    @Test
    public fun coerces_value_top_limit() {
        val state = mutableStateOf(4)

        rule.setContentWithTheme {
            InlineSlider(
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 0..10,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.runOnIdle {
            state.value = 20
        }
        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(10f, 0f..10f, 9))
    }

    @Test
    public fun coerces_value_lower_limit() {
        val state = mutableStateOf(4)

        rule.setContentWithTheme {
            InlineSlider(
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 0..10,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.runOnIdle {
            state.value = -20
        }
        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..10f, 9))
    }

    @Test
    public fun snaps_value_exactly() {
        val state = mutableStateOf(0)

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = IntProgression.fromClosedRange(0, 12, 3)
            )
        }

        rule.runOnUiThread {
            state.value = 6
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(6f, 0f..12f, 3))
    }

    @Test
    public fun snaps_value_to_previous() {
        val state = mutableStateOf(0)

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = IntProgression.fromClosedRange(0, 12, 3)
            )
        }

        rule.runOnUiThread {
            state.value = 7
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(6f, 0f..12f, 3))
    }

    @Test
    public fun snaps_value_to_next() {
        val state = mutableStateOf(0)

        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = IntProgression.fromClosedRange(0, 12, 3)
            )
        }

        rule.runOnUiThread {
            state.value = 8
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(9f, 0f..12f, 3))
    }

    @Test
    public fun decreases_value_by_clicking_left() {
        val state = mutableStateOf(2)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 1..4,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(1)
        }
    }

    @Test
    public fun increases_value_by_clicking_right() {
        val state = mutableStateOf(2)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 1..4,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width - 15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(3)
        }
    }

    @Test
    public fun ignores_left_click_when_disabled() {
        val state = mutableStateOf(2)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                enabled = false,
                onValueChange = { state.value = it },
                valueProgression = 1..4,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(2)
        }
    }

    @Test
    public fun reaches_min_clicking_left() {
        val state = mutableStateOf(1)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 1..4,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(1)
        }
    }

    @Test
    public fun reaches_max_clicking_right() {
        val state = mutableStateOf(4)
        rule.setContentWithTheme {
            InlineSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 1..4,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width - 15f, height / 2f)) }
        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(4)
        }
    }
}
