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

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.google.common.truth.Truth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalWearMaterialApi
class StepperTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            Stepper(
                value = 1f,
                onValueChange = {},
                steps = 5,
                modifier = Modifier.testTag(TEST_TAG)
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun coerces_value_top_limit() = rule.setNewValueAndCheck(
        range = 0f..10f,
        steps = 4,
        initialValue = 4f,
        newValue = 20f,
        expectedValue = 10f
    )

    @Test
    fun coerces_value_lower_limit() = rule.setNewValueAndCheck(
        range = 0f..10f,
        steps = 4,
        initialValue = 4f,
        newValue = -20f,
        expectedValue = 0f
    )

    @Test
    fun coerces_value_top_limit_and_doesnt_change_state() {
        val state = mutableStateOf(4f)
        val valueRange = 0f..10f

        rule.initDefaultStepper(state, valueRange, 4)

        rule.runOnIdle {
            state.value = 20f
        }
        assertEquals(20f, state.value)
    }

    @Test
    fun coerces_value_lower_limit_and_doesnt_change_state() {
        val state = mutableStateOf(4f)
        val valueRange = 0f..10f

        rule.initDefaultStepper(state, valueRange, 4)

        rule.runOnIdle {
            state.value = -20f
        }
        assertEquals(-20f, state.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_when_steps_negative() {
        rule.setContent {
            Stepper(value = 0f, onValueChange = {}, steps = -1) {}
        }
    }

    @Test
    fun snaps_value_exactly() = rule.setNewValueAndCheck(
        range = 0f..1f,
        steps = 4,
        initialValue = 0f,
        // Allowed values are only 0, 0.2, 0.4, 0.6, 0.8, 1
        newValue = 0.6f,
        expectedValue = 0.6f
    )

    @Test
    fun snaps_value_to_previous() = rule.setNewValueAndCheck(
        range = 0f..1f,
        steps = 4,
        initialValue = 0f,
        // Allowed values are only 0, 0.2, 0.4, 0.6, 0.8, 1
        newValue = 0.65f,
        expectedValue = 0.6f
    )

    @Test
    fun snaps_value_to_next() = rule.setNewValueAndCheck(
        range = 0f..1f,
        steps = 4,
        initialValue = 0f,
        // Allowed values are only 0, 0.2, 0.4, 0.6, 0.8, 1
        newValue = 0.55f,
        expectedValue = 0.6f
    )

    @Test
    fun snaps_value_to_next_and_does_not_change_state() {
        val state = mutableStateOf(0f)
        val valueRange = 0f..1f

        rule.initDefaultStepper(state, valueRange, 4)

        rule.runOnIdle {
            state.value = 0.55f
        }
        assertEquals(0.55f, state.value)
    }

    @Test
    fun decreases_value_by_clicking_bottom() {
        val state = mutableStateOf(2f)
        val range = 1f..4f

        rule.initDefaultStepper(state, range, 2)

        // The clickable area for a decrease button takes bottom 35% of the screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width / 2f, height - 15f)) }

        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(1f)
        }
    }

    @Test
    fun increases_value_by_clicking_top() {
        val state = mutableStateOf(2f)
        val range = 1f..4f

        rule.initDefaultStepper(state, range, 2)

        // The clickable area for an increase button takes top 35% of the screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width / 2f, 15f)) }

        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(3f)
        }
    }

    @Test
    fun reaches_min_clicking_bottom() {
        val state = mutableStateOf(1f)
        val range = 1f..4f

        rule.initDefaultStepper(state, range, 2)

        // The clickable area for a decrease button takes bottom 35% of the screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width / 2f, height - 15f)) }

        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(1f)
        }
    }

    @Test
    fun reaches_max_clicking_top() {
        val state = mutableStateOf(4f)
        val range = 1f..4f

        rule.initDefaultStepper(state, range, 2)

        // The clickable area for an increase button takes top 35% of the screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width / 2f, 15f)) }

        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(4f)
        }
    }

    @Test
    fun sets_custom_decrease_icon() {
        val iconTag = "iconTag_test"

        rule.setContentWithTheme {
            Stepper(
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
            ) {}
        }
        val unclippedBoundsInRoot = rule.onRoot().getUnclippedBoundsInRoot()

        rule.waitForIdle()
        rule.onNodeWithTag(iconTag, true)
            .assertExists()
            .assertTopPositionInRootIsEqualTo(
                unclippedBoundsInRoot.height -
                    BorderVerticalMargin - DefaultIconHeight
            )
    }

    @Test
    fun sets_custom_increase_icon() {
        val iconTag = "iconTag_test"

        rule.setContentWithTheme {
            Stepper(
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
            ) {}
        }
        rule.waitForIdle()
        rule.onNodeWithTag(iconTag, true)
            .assertExists()
            .assertTopPositionInRootIsEqualTo(BorderVerticalMargin)
    }

    @Test
    fun sets_content() {
        val contentTag = "contentTag_test"

        rule.setContentWithTheme {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                onValueChange = { },
            ) {
                Text("Testing", modifier = Modifier.testTag(contentTag).fillMaxHeight())
            }
        }

        val rootHeight = rule.onRoot().getUnclippedBoundsInRoot().height

        rule.waitForIdle()
        rule.onNodeWithTag(contentTag, true)
            .assertExists()
            .assertTopPositionInRootIsEqualTo(
                // Position of the content is a weight(35%) of (top button minus 2 spacers 8dp each)
                // plus 1 spacer
                (rootHeight - VerticalMargin * 2) * ButtonWeight + VerticalMargin
            )
    }

    private val BorderVerticalMargin = 8.dp
    private val VerticalMargin = 8.dp
    private val ButtonWeight = .35f
    private val DefaultIconHeight = 24.dp
}

private fun ComposeContentTestRule.setNewValueAndCheck(
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    initialValue: Float,
    newValue: Float,
    expectedValue: Float
) {
    val state = mutableStateOf(initialValue)

    initDefaultStepper(state, range, steps)

    runOnIdle { state.value = newValue }
    onNodeWithTag(TEST_TAG)
        .assertRangeInfoEquals(ProgressBarRangeInfo(expectedValue, range, steps))
}

private fun ComposeContentTestRule.initDefaultStepper(
    state: MutableState<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    setContentWithTheme {
        Stepper(
            value = state.value,
            onValueChange = { state.value = it },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.testTag(TEST_TAG)
        ) {}
    }
}
