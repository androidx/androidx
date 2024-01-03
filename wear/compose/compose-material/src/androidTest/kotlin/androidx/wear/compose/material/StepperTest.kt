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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.google.common.truth.Truth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

public class StepperTest {
    @get:Rule
    public val rule = createComposeRule()

    @Test
    public fun supports_testtag() {
        rule.setContentWithTheme {
            Stepper(
                value = 1f,
                onValueChange = {},
                steps = 5,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                modifier = Modifier.testTag(TEST_TAG)
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun coerces_value_top_limit() = rule.setNewValueAndCheck(
        range = 0f..10f,
        steps = 4,
        initialValue = 4f,
        newValue = 20f,
        expectedFinalValue = 10f
    )

    @Test
    public fun coerces_value_lower_limit() = rule.setNewValueAndCheck(
        range = 0f..10f,
        steps = 4,
        initialValue = 4f,
        newValue = -20f,
        expectedFinalValue = 0f
    )

    @Test(expected = IllegalArgumentException::class)
    public fun throws_when_steps_negative() {
        rule.setContent {
            Stepper(
                value = 0f,
                onValueChange = {},
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                steps = -1
            ) {}
        }
    }

    @Test
    public fun coerce_value_exactly() = rule.setNewValueAndCheck(
        range = 0f..1f,
        steps = 4,
        initialValue = 0f,
        // Allowed values are only 0, 0.2, 0.4, 0.6, 0.8, 1
        newValue = 0.6f,
        expectedFinalValue = 0.6f
    )

    @Test
    public fun coerce_value_to_previous() = rule.setNewValueAndCheck(
        range = 0f..1f,
        steps = 4,
        initialValue = 0f,
        // Allowed values are only 0, 0.2, 0.4, 0.6, 0.8, 1
        newValue = 0.65f,
        expectedFinalValue = 0.6f
    )

    @Test
    public fun coerce_value_to_next() = rule.setNewValueAndCheck(
        range = 0f..1f,
        steps = 4,
        initialValue = 0f,
        // Allowed values are only 0, 0.2, 0.4, 0.6, 0.8, 1
        newValue = 0.55f,
        expectedFinalValue = 0.6f
    )

    @Test
    public fun decreases_value_by_clicking_bottom() {
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
    public fun increases_value_by_clicking_top() {
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
    public fun reaches_min_clicking_bottom() {
        // Start one step above the minimum.
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
    public fun reaches_max_clicking_top() {
        // Start one step below the maximum.
        val state = mutableStateOf(3f)
        val range = 1f..4f

        rule.initDefaultStepper(state, range, 2)

        // The clickable area for an increase button takes top 35% of the screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput { click(Offset(width / 2f, 15f)) }

        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(4f)
        }
    }

    @Test
    public fun disables_decrease_when_minimum_value_reached() {
        val state = mutableStateOf(1f)
        val range = 1f..4f

        rule.initDefaultStepper(state, range, 2)

        rule.onNodeWithContentDescription(DECREASE).onParent().assertHasNoClickAction()
    }

    @Test
    public fun disables_increase_when_maximum_value_reached() {
        val state = mutableStateOf(4f)
        val range = 1f..4f

        rule.initDefaultStepper(state, range, 2)

        rule.onNodeWithContentDescription(INCREASE).onParent().assertHasNoClickAction()
    }

    @Test
    public fun colors_decrease_icon_with_disabled_alpha() =
        verifyDisabledColors(increase = false, value = 1f)

    @Test
    public fun colors_increase_icon_with_disabled_alpha() =
        verifyDisabledColors(increase = true, value = 4f)

    @Test
    public fun sets_custom_decrease_icon() {
        val iconTag = "iconTag_test"

        rule.setContentWithTheme {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                onValueChange = { },
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = {
                    Icon(
                        modifier = Modifier.testTag(iconTag),
                        imageVector = Icons.Default.Star,
                        contentDescription = ""
                    )
                },
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
    public fun sets_custom_increase_icon() {
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
                },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
            ) {}
        }
        rule.waitForIdle()
        rule.onNodeWithTag(iconTag, true)
            .assertExists()
            .assertTopPositionInRootIsEqualTo(BorderVerticalMargin)
    }

    @Test
    public fun sets_content() {
        val contentTag = "contentTag_test"

        rule.setContentWithTheme {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
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

    @Test
    public fun sets_custom_description_for_increase_icon() {
        val testContentDescription = "testContentDescription"

        rule.setContentWithTheme {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                increaseIcon = { Icon(StepperDefaults.Increase, testContentDescription) },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                onValueChange = { },
            ) {}
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG, true)
            // 0 is the index of increase button, 1 - decrease button, content is empty
            .onChildAt(0)
            .onChild()
            .assertContentDescriptionContains(testContentDescription)
    }

    @Test
    public fun sets_custom_description_for_decrease_icon() {
        val testContentDescription = "testContentDescription"

        rule.setContentWithTheme {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, testContentDescription) },
                onValueChange = { },
            ) {}
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG, true)
            // 0 is the index of increase button, 1 - decrease button, content is empty
            .onChildAt(1)
            .onChild()
            .assertContentDescriptionContains(testContentDescription)
    }

    @Test
    fun supports_stepper_range_semantics_by_default() {
        val value = 1f
        val steps = 5
        val valueRange = 0f..(steps + 1).toFloat()

        val modifier = Modifier.testTag(TEST_TAG)

        rule.setContentWithTheme {
            Stepper(
                modifier = modifier,
                value = value,
                steps = steps,
                valueRange = valueRange,
                onValueChange = { },
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
            ) {}
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG, true)
            .assertExists()
            .assertRangeInfoEquals(ProgressBarRangeInfo(value, valueRange, steps))
    }

    @Test(expected = java.lang.AssertionError::class)
    fun disable_stepper_semantics_with_enableDefaultSemantics_false() {
        val value = 1f
        val steps = 5
        val valueRange = 0f..(steps + 1).toFloat()

        rule.setContentWithTheme {
            Stepper(
                modifier = Modifier.testTag(TEST_TAG),
                value = value,
                steps = steps,
                valueRange = valueRange,
                onValueChange = { },
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                enableRangeSemantics = false
            ) {}
        }
        rule.waitForIdle()
        // Should throw assertion error for assertRangeInfoEquals
        rule.onNodeWithTag(TEST_TAG, true)
            .assertExists()
            .assertRangeInfoEquals(ProgressBarRangeInfo(value, valueRange, steps))
    }

    private fun verifyDisabledColors(increase: Boolean, value: Float) {
        val state = mutableStateOf(value)
        var expectedIconColor = Color.Transparent
        var actualIconColor = Color.Transparent
        var expectedAlpha = 0f
        var actualAlpha = 0f

        rule.setContentWithTheme {
            expectedIconColor = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.disabled)
            expectedAlpha = expectedIconColor.alpha
            Stepper(
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 1f..4f,
                steps = 2,
                iconColor = MaterialTheme.colors.primary,
                increaseIcon = {
                    if (increase) {
                        actualIconColor = LocalContentColor.current
                        actualAlpha = LocalContentAlpha.current
                    }
                },
                decreaseIcon = {
                    if (!increase) {
                        actualIconColor = LocalContentColor.current
                        actualAlpha = LocalContentAlpha.current
                    }
                },
                modifier = Modifier.testTag(TEST_TAG)
            ) {}
        }

        assertEquals(expectedIconColor, actualIconColor)
        assertEquals(expectedAlpha, actualAlpha)
    }

    private val BorderVerticalMargin = 22.dp
    private val VerticalMargin = 8.dp
    private val ButtonWeight = .35f
    private val DefaultIconHeight = 24.dp
}

public class IntegerStepperTest {
    @get:Rule
    public val rule = createComposeRule()

    @Test
    public fun supports_testtag() {
        rule.setContentWithTheme {
            Stepper(
                value = 1,
                onValueChange = {},
                valueProgression = 0..5,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                modifier = Modifier.testTag(TEST_TAG)
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun default_step_in_valueProgression() {
        rule.setContentWithTheme {
            Stepper(
                value = 1,
                onValueChange = {},
                valueProgression = 0..10,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                modifier = Modifier.testTag(TEST_TAG)
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..10f, 9))
    }

    @Test
    public fun coerces_value_top_limit() = rule.setNewValueAndCheck(
        progression = 0..10,
        initialValue = 4,
        newValue = 20,
        expectedFinalValue = 10
    )

    @Test
    public fun coerces_value_lower_limit() = rule.setNewValueAndCheck(
        progression = 0..10,
        initialValue = 4,
        newValue = -20,
        expectedFinalValue = 0
    )

    @Test
    public fun coerce_value_exactly() = rule.setNewValueAndCheck(
        progression = IntProgression.fromClosedRange(0, 12, 3),
        initialValue = 0,
        newValue = 3,
        expectedFinalValue = 3
    )

    @Test
    public fun coerce_value_to_previous() = rule.setNewValueAndCheck(
        progression = IntProgression.fromClosedRange(0, 12, 3),
        initialValue = 0,
        newValue = 4,
        expectedFinalValue = 3
    )

    @Test
    public fun coerce_value_to_next() = rule.setNewValueAndCheck(
        progression = IntProgression.fromClosedRange(0, 12, 3),
        initialValue = 0,
        newValue = 5,
        expectedFinalValue = 6
    )

    @Test
    fun supports_stepper_range_semantics_by_default() {
        val value = 1
        val valueProgression = 0..10

        rule.setContentWithTheme {
            Stepper(
                value = value,
                onValueChange = {},
                valueProgression = valueProgression,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                modifier = Modifier.testTag(TEST_TAG)
            ) {}
        }
        rule.waitForIdle()
        // Should throw assertion error for assertRangeInfoEquals
        rule.onNodeWithTag(TEST_TAG, true)
            .assertExists()
            .assertRangeInfoEquals(
                ProgressBarRangeInfo(
                    value.toFloat(),
                    valueProgression.first.toFloat()..valueProgression.last.toFloat(),
                    valueProgression.stepsNumber()
                )
            )
    }

    @Test(expected = java.lang.AssertionError::class)
    fun disable_stepper_semantics_with_enableDefaultSemantics_false() {
        val value = 1
        val valueProgression = 0..10

        rule.setContentWithTheme {
            Stepper(
                value = value,
                onValueChange = {},
                valueProgression = valueProgression,
                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                modifier = Modifier.testTag(TEST_TAG),
                enableRangeSemantics = false
            ) {}
        }
        rule.waitForIdle()
        // Should throw assertion error for assertRangeInfoEquals
        rule.onNodeWithTag(TEST_TAG, true)
            .assertExists()
            .assertRangeInfoEquals(
                ProgressBarRangeInfo(
                    value.toFloat(),
                    valueProgression.first.toFloat()..valueProgression.last.toFloat(),
                    valueProgression.stepsNumber()
                )
            )
    }
}

private fun ComposeContentTestRule.setNewValueAndCheck(
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    initialValue: Float,
    newValue: Float,
    expectedFinalValue: Float
) {
    val state = mutableStateOf(initialValue)

    initDefaultStepper(state, range, steps)

    runOnIdle { state.value = newValue }
    onNodeWithTag(TEST_TAG)
        .assertRangeInfoEquals(ProgressBarRangeInfo(expectedFinalValue, range, steps))

    // State value is not coerced to expectedValue - thus we expect it to be equal to
    // the last set value, which is newValue
    waitForIdle()
    assertEquals(newValue, state.value)
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
            increaseIcon = { Icon(StepperDefaults.Increase, INCREASE) },
            decreaseIcon = { Icon(StepperDefaults.Decrease, DECREASE) },
            modifier = Modifier.testTag(TEST_TAG)
        ) {}
    }
}

private fun ComposeContentTestRule.setNewValueAndCheck(
    progression: IntProgression,
    initialValue: Int,
    newValue: Int,
    expectedFinalValue: Int
) {
    val state = mutableStateOf(initialValue)

    initDefaultStepper(state, progression)

    runOnIdle { state.value = newValue }
    onNodeWithTag(TEST_TAG)
        .assertRangeInfoEquals(
            ProgressBarRangeInfo(
                expectedFinalValue.toFloat(),
                progression.first.toFloat()..progression.last.toFloat(),
                progression.stepsNumber()
            )
        )

    // State value is not coerced to expectedValue - thus we expect it to be equal to
    // the last set value, which is newValue
    waitForIdle()
    assertEquals(newValue, state.value)
}

private fun ComposeContentTestRule.initDefaultStepper(
    state: MutableState<Int>,
    valueProgression: IntProgression,
) {
    setContentWithTheme {
        Stepper(
            value = state.value,
            onValueChange = { state.value = it },
            valueProgression = valueProgression,
            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
            modifier = Modifier.testTag(TEST_TAG)
        ) {}
    }
}

private val INCREASE = "increase"
private val DECREASE = "decrease"
