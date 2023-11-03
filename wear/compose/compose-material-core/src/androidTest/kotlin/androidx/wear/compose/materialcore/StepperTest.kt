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

package androidx.wear.compose.materialcore

import android.os.Build
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

public class StepperTest {
    @get:Rule
    public val rule = createComposeRule()

    @Test
    public fun supports_testtag() {
        rule.setContent {
            StepperWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test(expected = IllegalArgumentException::class)
    public fun throws_when_steps_negative() {
        rule.setContent {
            StepperWithDefaults(
                steps = -1
            ) {}
        }
    }

    @Test
    public fun decreases_value_by_clicking_bottom() {
        val state = mutableStateOf(2f)
        val range = 1f..4f

        rule.initDefaultStepper(state, range, 2)

        rule.onNodeWithContentDescription(DECREASE).performClick()

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
    public fun checks_decrease_icon_position() {
        rule.setContent {
            StepperWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            ) {}
        }
        val unclippedBoundsInRoot = rule.onRoot().getUnclippedBoundsInRoot()

        rule.waitForIdle()
        rule.onNodeWithTag(DECREASE, true)
            .assertExists()
            .assertTopPositionInRootIsEqualTo(
                unclippedBoundsInRoot.height -
                    BorderVerticalMargin - DefaultIconHeight
            )
    }

    @Test
    public fun sets_custom_decrease_icon() {
        val iconTag = "iconTag_test"

        rule.setContent {
            StepperWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                decreaseIcon = {
                    TestImage(Modifier.size(24.dp), iconTag)
                }
            ) {}
        }

        rule.waitForIdle()
        rule.onNodeWithTag(iconTag, true)
            .assertExists()
    }

    @Test
    public fun sets_custom_increase_icon() {
        val iconTag = "iconTag_test"

        rule.setContent {
            StepperWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                increaseIcon = {
                    TestImage(Modifier.size(24.dp), iconTag)
                }
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

        rule.setContent {
            StepperWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                TestText(
                    "Testing",
                    modifier = Modifier
                        .testTag(contentTag)
                        .fillMaxHeight()
                )
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public fun sets_background_color() {
        val testColor = Color.Blue
        val backgroundThreshold = 50.0f
        rule.setContent {
            StepperWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                backgroundColor = testColor
            ) {}
        }
        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(testColor, backgroundThreshold)
    }

    @Test
    public fun sets_custom_description_for_increase_icon() {
        val testContentDescription = "testContentDescription"

        rule.setContent {
            StepperWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                increaseIcon = {
                    TestImage(Modifier.size(24.dp), testContentDescription)
                }
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

        rule.setContent {
            StepperWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                value = 0f,
                steps = 5,
                decreaseIcon = {
                    TestImage(Modifier.size(24.dp), testContentDescription)
                }
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
    public fun sets_button_provider_values_correctly_for_minimum_value() {
        verifyButtonProviderValues(
            value = 1f,
            valueRange = 1f..4f,
            steps = 3,
            expectedIncreaseIconData = +1,
            expectedDecreaseIconData = -1,
            enabledButtonProviderValues = arrayOf(
                LocalContentTestData provides +1
            ),
            disabledButtonProviderValues = arrayOf(
                LocalContentTestData provides -1
            )
        )
    }

    @Test
    public fun sets_button_provider_values_correctly_for_maximum_value() {
        verifyButtonProviderValues(
            value = 4f,
            valueRange = 1f..4f,
            steps = 3,
            expectedIncreaseIconData = -1,
            expectedDecreaseIconData = +1,
            enabledButtonProviderValues = arrayOf(
                LocalContentTestData provides +1
            ),
            disabledButtonProviderValues = arrayOf(
                LocalContentTestData provides -1
            )
        )
    }

    @Test
    public fun sets_button_provider_values_correctly_for_value_between_min_and_max() {
        verifyButtonProviderValues(
            value = 2f,
            valueRange = 1f..4f,
            steps = 3,
            expectedIncreaseIconData = +1,
            expectedDecreaseIconData = +1,
            enabledButtonProviderValues = arrayOf(
                LocalContentTestData provides +1
            ),
            disabledButtonProviderValues = arrayOf(
                LocalContentTestData provides -1
            )
        )
    }

    private val BorderVerticalMargin = 22.dp
    private val VerticalMargin = 8.dp
    private val ButtonWeight = .35f
    private val DefaultIconHeight = 24.dp

    @Composable
    internal fun StepperWithDefaults(
        modifier: Modifier = Modifier,
        value: Float = 1f,
        onValueChange: (Float) -> Unit = {},
        steps: Int = 5,
        decreaseIcon: @Composable () -> Unit = {
            TestImage(
                modifier = Modifier.size(24.dp),
                iconLabel = DECREASE
            )
        },
        increaseIcon: @Composable () -> Unit = {
            TestImage(
                modifier = Modifier.size(24.dp),
                iconLabel = INCREASE
            )
        },
        valueRange: ClosedFloatingPointRange<Float> = 0f..5f,
        backgroundColor: Color = Color.Black,
        enabledButtonProviderValues: Array<ProvidedValue<*>> = arrayOf(),
        disabledButtonProviderValues: Array<ProvidedValue<*>> = arrayOf(),
        content: @Composable BoxScope.() -> Unit
    ) = Stepper(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        steps = steps,
        increaseIcon = increaseIcon,
        decreaseIcon = decreaseIcon,
        valueRange = valueRange,
        backgroundColor = backgroundColor,
        enabledButtonProviderValues = enabledButtonProviderValues,
        disabledButtonProviderValues = disabledButtonProviderValues,
        content = content,
    )

    private fun verifyButtonProviderValues(
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        expectedIncreaseIconData: Int,
        expectedDecreaseIconData: Int,
        enabledButtonProviderValues: Array<ProvidedValue<*>> = arrayOf(),
        disabledButtonProviderValues: Array<ProvidedValue<*>> = arrayOf()
    ) {
        var increaseIconData = 0
        var decreaseIconData = 0
        rule.setContent {
            StepperWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                value = value,
                steps = steps,
                valueRange = valueRange,
                backgroundColor = Color.Transparent,
                enabledButtonProviderValues = enabledButtonProviderValues,
                disabledButtonProviderValues = disabledButtonProviderValues,
                decreaseIcon = {
                    decreaseIconData = LocalContentTestData.current
                },
                increaseIcon = {
                    increaseIconData = LocalContentTestData.current
                }
            ) {}
        }

        Assert.assertEquals(increaseIconData, expectedIncreaseIconData)
        Assert.assertEquals(decreaseIconData, expectedDecreaseIconData)
    }

    private fun ComposeContentTestRule.initDefaultStepper(
        state: MutableState<Float>,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int
    ) {
        setContent {
            StepperWithDefaults(
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.testTag(TEST_TAG)
            ) {}
        }
    }

    private val INCREASE = "increase"
    private val DECREASE = "decrease"
}
