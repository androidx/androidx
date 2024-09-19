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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.compose.material3.internal.Strings
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test

class SliderTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            DefaultSlider(
                value = 1f,
                onValueChange = {},
                valueRange = 0f..10f,
                steps = 5,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun coerces_value_top_limit() {
        val state = mutableStateOf(4f)

        rule.setContentWithTheme {
            DefaultSlider(
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..10f,
                steps = 4,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.runOnIdle { state.value = 20f }
        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(10f, 0f..10f, 4))
    }

    @Test
    fun coerces_value_lower_limit() {
        val state = mutableStateOf(4f)

        rule.setContentWithTheme {
            DefaultSlider(
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..10f,
                steps = 4,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.runOnIdle { state.value = -20f }
        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..10f, 4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_when_steps_negative() {
        rule.setContent {
            DefaultSlider(value = 0f, valueRange = 0f..10f, onValueChange = {}, steps = -1)
        }
    }

    @Test
    fun coerces_value_exactly() {
        val state = mutableStateOf(0f)
        val range = 0f..1f

        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                steps = 4,
                valueRange = range
            )
        }

        rule.runOnUiThread { state.value = 0.6f }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, range, 4))
    }

    @Test
    fun coerces_value_to_previous() {
        val state = mutableStateOf(0f)
        val range = 0f..1f

        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                steps = 4,
                valueRange = range
            )
        }

        rule.runOnUiThread { state.value = 0.65f }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, range, 4))
    }

    @Test
    fun coerces_value_to_next() {
        val state = mutableStateOf(0f)
        val range = 0f..1f

        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                steps = 4,
                valueRange = range
            )
        }

        rule.runOnUiThread { state.value = 0.55f }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, range, 4))
    }

    @Test
    fun decreases_value_by_clicking_left() {
        val state = mutableStateOf(2f)
        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(0.001f).of(1f) }
    }

    @Test
    fun increases_value_by_clicking_right() {
        val state = mutableStateOf(2f)
        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width - 15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(0.001f).of(3f) }
    }

    @Test
    fun ignores_left_click_when_disabled() {
        val state = mutableStateOf(2f)
        rule.setContentWithTheme {
            Slider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                enabled = false,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(0.001f).of(2f) }
    }

    @Test
    fun ignores_right_click_when_disabled() {
        val state = mutableStateOf(2f)
        rule.setContentWithTheme {
            Slider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                enabled = false,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width - 15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(0.001f).of(2f) }
    }

    @Test
    fun reaches_min_clicking_left() {
        val state = mutableStateOf(1f)
        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(0.001f).of(1f) }
    }

    @Test
    fun reaches_max_clicking_right() {
        val state = mutableStateOf(4f)
        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width - 15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isWithin(0.001f).of(4f) }
    }

    @Test
    fun sets_custom_decrease_icon() {
        val iconTag = "iconTag_test"

        rule.setContentWithTheme {
            Slider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                onValueChange = {},
                decreaseIcon = {
                    Icon(
                        modifier = Modifier.testTag(iconTag).size(SliderDefaults.IconSize),
                        imageVector = Icons.Default.Star,
                        contentDescription = ""
                    )
                }
            )
        }

        rule.waitForIdle()
        rule
            .onNodeWithTag(iconTag, true)
            .assertExists()
            .assertLeftPositionInRootIsEqualTo(IconsOuterHorizontalMargin)
    }

    @Test
    fun sets_custom_increase_icon() {
        val iconTag = "iconTag_test"

        rule.setContentWithTheme {
            Slider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                onValueChange = {},
                increaseIcon = {
                    Icon(
                        modifier = Modifier.testTag(iconTag).size(SliderDefaults.IconSize),
                        imageVector = Icons.Default.Star,
                        contentDescription = ""
                    )
                },
            )
        }
        val unclippedBoundsInRoot = rule.onRoot().getUnclippedBoundsInRoot()

        rule.waitForIdle()
        rule
            .onNodeWithTag(iconTag, true)
            .assertExists()
            .assertLeftPositionInRootIsEqualTo(
                unclippedBoundsInRoot.width - IconsOuterHorizontalMargin - DefaultIconWidth
            )
    }

    @Test
    fun sets_custom_description_for_decrease_icon() {
        rule.setContentWithTheme {
            Slider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                onValueChange = {},
            )
        }

        rule.waitForIdle()
        rule
            .onNodeWithTag(TEST_TAG, true)
            // 0 is the index of decrease button, 1 - increase button
            .onChildAt(0)
            .onChild()
            .assertContentDescriptionContains(
                getString(Strings.SliderDecreaseButtonContentDescription)
            )
    }

    @Test
    fun sets_custom_description_for_increase_icon() {
        rule.setContentWithTheme {
            Slider(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                onValueChange = {},
            )
        }

        rule.waitForIdle()
        rule
            .onNodeWithTag(TEST_TAG, true)
            // 0 is the index of decrease button, 1 - increase button
            .onChildAt(1)
            .onChild()
            .assertContentDescriptionContains(
                getString(Strings.SliderIncreaseButtonContentDescription)
            )
    }

    @Test
    fun supports_testtag_in_integer_slider() {
        rule.setContentWithTheme {
            DefaultSlider(
                value = 1,
                onValueChange = {},
                valueProgression = 0..10,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun default_step_in_valueProgression_in_integer_slider() {
        rule.setContentWithTheme {
            DefaultSlider(
                value = 2,
                onValueChange = {},
                valueProgression = 0..10,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(2f, 0f..10f, 9))
    }

    @Test
    fun custom_valueProgression_in_integer_slider() {
        rule.setContentWithTheme {
            DefaultSlider(
                value = 2,
                onValueChange = {},
                valueProgression = IntProgression.fromClosedRange(0, 10, 2),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(2f, 0f..10f, 4))
    }

    @Test
    fun valueProgression_trimmed_in_integer_slider() {
        rule.setContentWithTheme {
            DefaultSlider(
                value = 6,
                onValueChange = {},
                valueProgression = IntProgression.fromClosedRange(0, 16, 6),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(6f, 0f..12f, 1))
    }

    @Test
    fun coerces_value_top_limit_in_integer_slider() {
        val state = mutableStateOf(4)

        rule.setContentWithTheme {
            DefaultSlider(
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 0..10,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.runOnIdle { state.value = 20 }
        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(10f, 0f..10f, 9))
    }

    @Test
    fun coerces_value_lower_limit_in_integer_slider() {
        val state = mutableStateOf(4)

        rule.setContentWithTheme {
            DefaultSlider(
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 0..10,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.runOnIdle { state.value = -20 }
        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..10f, 9))
    }

    @Test
    fun coerces_value_exactly_in_integer_slider() {
        val state = mutableStateOf(0)

        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = IntProgression.fromClosedRange(0, 12, 3)
            )
        }

        rule.runOnUiThread { state.value = 6 }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(6f, 0f..12f, 3))
    }

    @Test
    fun coerces_value_to_previous_in_integer_slider() {
        val state = mutableStateOf(0)

        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = IntProgression.fromClosedRange(0, 12, 3)
            )
        }

        rule.runOnUiThread { state.value = 7 }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(6f, 0f..12f, 3))
    }

    @Test
    fun coerces_value_to_next_in_integer_slider() {
        val state = mutableStateOf(0)

        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = IntProgression.fromClosedRange(0, 12, 3)
            )
        }

        rule.runOnUiThread { state.value = 8 }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(9f, 0f..12f, 3))
    }

    @Test
    fun decreases_value_by_clicking_left_in_integer_slider() {
        val state = mutableStateOf(2)
        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 1..4,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isEqualTo(1) }
    }

    @Test
    fun increases_value_by_clicking_right_in_integer_slider() {
        val state = mutableStateOf(2)
        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 1..4,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width - 15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isEqualTo(3) }
    }

    @Test
    fun ignores_left_click_when_disabled_in_integer_slider() {
        val state = mutableStateOf(2)
        rule.setContentWithTheme {
            Slider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                enabled = false,
                onValueChange = { state.value = it },
                valueProgression = 1..4,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isEqualTo(2) }
    }

    @Test
    fun reaches_min_clicking_left_in_integer_slider() {
        val state = mutableStateOf(1)
        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 1..4,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isEqualTo(1) }
    }

    @Test
    fun reaches_max_clicking_right_in_integer_slider() {
        val state = mutableStateOf(4)
        rule.setContentWithTheme {
            DefaultSlider(
                modifier = Modifier.testTag(TEST_TAG),
                value = state.value,
                onValueChange = { state.value = it },
                valueProgression = 1..4,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width - 15f, height / 2f)) }
        rule.runOnIdle { Truth.assertThat(state.value).isEqualTo(4) }
    }

    @Composable
    private fun DefaultSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        modifier: Modifier = Modifier,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int
    ) {
        Slider(
            modifier = modifier,
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }

    @Composable
    private fun DefaultSlider(
        value: Int,
        onValueChange: (Int) -> Unit,
        modifier: Modifier = Modifier,
        valueProgression: IntProgression
    ) {
        Slider(
            modifier = modifier,
            value = value,
            onValueChange = onValueChange,
            valueProgression = valueProgression,
        )
    }

    private fun getString(string: Strings) =
        InstrumentationRegistry.getInstrumentation().context.resources.getString(string.value)

    private val IconsOuterHorizontalMargin = 12.dp
    private val DefaultIconWidth = 24.dp
}
