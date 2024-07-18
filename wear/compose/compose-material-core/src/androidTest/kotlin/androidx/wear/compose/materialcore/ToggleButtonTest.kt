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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ToggleButtonTest {
    @get:Rule
    val rule = createComposeRule()

    /* Round Toggle buttons */
    @Test
    fun round_toggle_button_supports_testTag() {
        rule.setContent {
            RoundToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun round_toggle_button_is_toggleable() {
        rule.setContent {
            RoundToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun round_toggle_button_has_click_action_when_enabled() {
        rule.setContent {
            RoundToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun round_toggle_button_has_click_action_when_disabled() {
        rule.setContent {
            RoundToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun round_toggle_button_is_correctly_enabled() {
        rule.setContent {
            RoundToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun round_toggle_button_is_correctly_disabled() {
        rule.setContent {
            RoundToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun round_toggle_button_is_on_when_checked() {
        rule.setContent {
            RoundToggleButtonWithDefaults(
                enabled = true,
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun round_toggle_button_is_off_when_unchecked() {
        rule.setContent {
            RoundToggleButtonWithDefaults(
                enabled = true,
                checked = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun round_toggle_button_toggles_when_enabled() {
        var clicked = false

        rule.setContent {
            RoundToggleButtonWithDefaults(
                enabled = true,
                onCheckedChange = { clicked = true },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle {
            Assert.assertEquals(true, clicked)
        }
    }

    @Test
    fun round_toggle_button_responds_to_toggle_on() {
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            RoundToggleButtonWithDefaults(
                content = { TestImage() },
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun round_toggle_button_responds_to_toggle_off() {
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            RoundToggleButtonWithDefaults(
                content = { TestImage() },
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun round_toggle_button_does_not_respond_to_click_when_disabled() {
        var clicked = false

        rule.setContent {
            RoundToggleButtonWithDefaults(
                onCheckedChange = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle {
            Assert.assertEquals(false, clicked)
        }
    }

    @Test
    fun round_toggle_button_has_role_checkbox() {
        rule.setContent {
            RoundToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Checkbox
                )
            )
    }

    @Test
    fun round_toggle_button_supports_circle_shape_under_ltr() =
        rule.isShape(CircleShape, LayoutDirection.Ltr) {
            RoundToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
            ) { }
        }

    @Test
    fun round_toggle_button_supports_circle_shape_under_rtl() =
        rule.isShape(CircleShape, LayoutDirection.Rtl) {
            RoundToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
            ) { }
        }

    @Test
    fun extra_small_round_toggle_button_meets_accessibility_tapSize() {
        verifyTapSize(48.dp) {
            RoundToggleButtonWithDefaults(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .size(32.dp)
            )
        }
    }

    @Test
    fun extra_small_round_toggle_button_has_correct_visible_size() {
        verifyVisibleSize(32.dp) {
            RoundToggleButtonWithDefaults(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .requiredSize(32.dp)
            )
        }
    }

    @Test
    fun default_round_toggle_button_has_correct_tapSize() {
        // Tap size for Button should be 52.dp.
        verifyTapSize(52.dp) {
            RoundToggleButtonWithDefaults(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .size(52.dp)
            )
        }
    }

    @Test
    fun default_round_toggle_button_has_correct_visible_size() {
        // Tap size for Button should be 52.dp.
        verifyVisibleSize(52.dp) {
            RoundToggleButtonWithDefaults(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .size(52.dp)
            )
        }
    }

    @Test
    fun round_toggle_button_allows_custom_shape_override() {
        val shape = CutCornerShape(4.dp)

        rule.isShape(shape, LayoutDirection.Ltr) {
            RoundToggleButtonWithDefaults(
                shape = shape,
                modifier = Modifier.testTag(TEST_TAG)
            ) { }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun round_toggle_button_gives_correct_colors_when_enabled() =
        verifyToggleButtonColors(
            enabled = true,
            checked = false,
            { enabled, _ -> remember { mutableStateOf(if (enabled) Color.Green else Color.Red) } },
            { enabled, _ ->
                remember { mutableStateOf(if (enabled) Color.Blue else Color.Yellow) }
            },
            Color.Green,
            Color.Blue
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun round_toggle_button_gives_correct_colors_when_disabled() =
        verifyToggleButtonColors(
            enabled = false,
            checked = false,
            { enabled, _ -> remember { mutableStateOf(if (enabled) Color.Green else Color.Red) } },
            { enabled, _ ->
                remember { mutableStateOf(if (enabled) Color.Blue else Color.Yellow) }
            },
            Color.Red,
            Color.Yellow,
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun round_toggle_button_gives_correct_colors_when_checked() =
        verifyToggleButtonColors(
            enabled = true,
            checked = true,
            { _, checked -> remember { mutableStateOf(if (checked) Color.Green else Color.Red) } },
            { _, checked ->
                remember { mutableStateOf(if (checked) Color.Blue else Color.Yellow) }
            },
            Color.Green,
            Color.Blue,
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun round_toggle_button_gives_correct_colors_when_unchecked() =
        verifyToggleButtonColors(
            enabled = true,
            checked = false,
            { _, checked -> remember { mutableStateOf(if (checked) Color.Green else Color.Red) } },
            { _, checked ->
                remember { mutableStateOf(if (checked) Color.Blue else Color.Yellow) }
            },
            Color.Red,
            Color.Yellow,
        )

    @Test
    fun round_toggle_button_obeys_content_provider_values() {
        var data = -1

        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                RoundToggleButtonWithDefaults(
                    content = {
                        CompositionLocalProvider(
                            LocalContentTestData provides EXPECTED_LOCAL_TEST_DATA
                        ) {
                            data = LocalContentTestData.current
                        }
                    }
                )
            }
        }

        Assert.assertEquals(data, EXPECTED_LOCAL_TEST_DATA)
    }

    /* Toggle button */
    @Test
    fun toggle_button_supports_testTag() {
        rule.setContent {
            ToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun toggle_button_has_click_action_when_enabled() {
        rule.setContent {
            ToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun toggle_button_has_click_action_when_disabled() {
        rule.setContent {
            ToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun toggle_button_is_toggleable() {
        rule.setContent {
            ToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun toggle_button_is_correctly_disabled() {
        rule.setContent {
            ToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun toggle_button_is_correctly_enabled() {
        rule.setContent {
            ToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun toggle_button_is_on_when_checked() {
        rule.setContent {
            ToggleButtonWithDefaults(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun toggle_button_is_off_when_unchecked() {
        rule.setContent {
            ToggleButtonWithDefaults(
                checked = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun toggle_button_responds_to_toggle_on() {
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            ToggleButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun toggle_button_responds_to_toggle_off() {
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            ToggleButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun toggle_button_does_not_toggle_when_disabled() {
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            ToggleButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsOff()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun toggle_button_has_role_checkbox() {
        rule.setContent {
            ToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Checkbox
                )
            )
    }

    @Test
    fun toggle_button_displays_label_content() {
        val textContent = "abc"

        rule.setContent {
            ToggleButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = {
                    TestText(text = textContent)
                }
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun toggle_button_allows_checked_background_color_override() =
        verifyToggleButtonBackgroundColor(
            checked = true,
            enabled = true,
            expectedColor = CHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun toggle_button_allows_unchecked_background_color_override() =
        verifyToggleButtonBackgroundColor(
            checked = false,
            enabled = true,
            expectedColor = UNCHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun toggle_button_allows_disabled_checked_background_color_override() =
        verifyToggleButtonBackgroundColor(
            checked = true,
            enabled = false,
            expectedColor = DISABLED_CHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun toggle_button_allows_disabled_unchecked_background_color_override() =
        verifyToggleButtonBackgroundColor(
            checked = false,
            enabled = false,
            expectedColor = DISABLED_UNCHECKED_COLOR
        )

    /* Split toggle buttons */

    @Test
    fun split_button_supports_testTag() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun split_button_has_click_action_when_enabled() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun split_button_has_click_action_when_disabled() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun split_button_is_toggleable() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun split_button_is_clickable() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun split_button_is_correctly_enabled() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun split_button_is_correctly_disabled() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertIsNotEnabled()
    }

    @Test
    fun split_button_is_off_when_unchecked() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                checked = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOff()
    }

    @Test
    fun split_button_is_on_when_checked() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOn()
    }

    @Test
    fun split_button_responds_to_toggle_on() {
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            SplitToggleButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .onChildAt(1)
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun split_button_responds_to_toggle_off() {
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            SplitToggleButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .onChildAt(1)
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun split_button_does_not_toggle_when_disabled() {
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            SplitToggleButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .onChildAt(1)
            .assertIsOff()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun split_button_has_roles_button_and_checkbox() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )

        rule.onNodeWithTag(TEST_TAG).onChildAt(1)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Checkbox
                )
            )
    }

    @Test
    fun split_button_displays_label_content() {
        val textContent = "abc"

        rule.setContent {
            SplitToggleButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = {
                    TestText(text = textContent)
                }
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_toggle_button_allows_checked_background_color_override() =
        verifySplitToggleButtonBackgroundColor(
            checked = true,
            enabled = true,
            expectedColor = CHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_toggle_button_allows_unchecked_background_color_override() =
        verifySplitToggleButtonBackgroundColor(
            checked = false,
            enabled = true,
            expectedColor = UNCHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_toggle_button_allows_disabled_checked_background_color_override() =
        verifySplitToggleButtonBackgroundColor(
            checked = true,
            enabled = false,
            expectedColor = DISABLED_CHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_toggle_button_allows_disabled_unchecked_background_color_override() =
        verifySplitToggleButtonBackgroundColor(
            checked = false,
            enabled = false,
            expectedColor = DISABLED_UNCHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyToggleButtonBackgroundColor(
        checked: Boolean,
        enabled: Boolean,
        expectedColor: Color
    ) {
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                ToggleButtonWithDefaults(
                    checked = checked,
                    onCheckedChange = {},
                    enabled = enabled,
                    background = { isEnabled, isChecked ->
                        val color =
                            if (isEnabled) {
                                if (isChecked) CHECKED_COLOR else UNCHECKED_COLOR
                            } else {
                                if (isChecked) DISABLED_CHECKED_COLOR else DISABLED_UNCHECKED_COLOR
                            }

                        Modifier.background(color)
                    },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifySplitToggleButtonBackgroundColor(
        checked: Boolean,
        enabled: Boolean,
        expectedColor: Color
    ) {
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitToggleButtonWithDefaults(
                    checked = checked,
                    onCheckedChange = {},
                    enabled = enabled,
                    backgroundColor = { isEnabled, isChecked ->
                        rememberUpdatedState(
                            if (isEnabled) {
                                if (isChecked) CHECKED_COLOR else UNCHECKED_COLOR
                            } else {
                                if (isChecked) DISABLED_CHECKED_COLOR else DISABLED_UNCHECKED_COLOR
                            }
                        )
                    },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyToggleButtonColors(
        enabled: Boolean,
        checked: Boolean,
        backgroundColor: @Composable (Boolean, Boolean) -> State<Color>,
        borderColor: @Composable (Boolean, Boolean) -> State<Color>,
        expectedBackgroundColor: Color,
        expectedBorderColor: Color,
        backgroundThreshold: Float = 50.0f,
        borderThreshold: Float = 1.0f,
    ) {
        val testBackground = Color.White
        val expectedColor = { color: Color ->
            if (color != Color.Transparent)
                color.compositeOver(testBackground)
            else
                testBackground
        }

        rule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(testBackground)
            ) {
                val actualBorderColor = borderColor(enabled, checked).value
                val border = remember { mutableStateOf(BorderStroke(2.dp, actualBorderColor)) }
                RoundToggleButtonWithDefaults(
                    backgroundColor = backgroundColor,
                    border = { _, _ -> return@RoundToggleButtonWithDefaults border },
                    enabled = enabled,
                    checked = checked,
                    modifier = Modifier.testTag(TEST_TAG)
                ) {
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedColor(expectedBackgroundColor), backgroundThreshold)
        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedColor(expectedBorderColor), borderThreshold)
    }

    private fun verifyTapSize(
        expected: Dp,
        content: @Composable () -> Unit
    ) {
        rule.setContent {
            content()
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertTouchHeightIsEqualTo(expected)
            .assertTouchWidthIsEqualTo(expected)
    }

    private fun verifyVisibleSize(
        expected: Dp,
        content: @Composable () -> Unit
    ) {
        rule.setContent {
            content()
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertHeightIsEqualTo(expected)
            .assertWidthIsEqualTo(expected)
    }
}

@Composable
private fun RoundToggleButtonWithDefaults(
    modifier: Modifier = Modifier,
    checked: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    backgroundColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> =
        { _, _ -> rememberUpdatedState(DEFAULT_SHAPE_COLOR) },
    border: @Composable (enabled: Boolean, checked: Boolean) -> State<BorderStroke?>? =
        { _, _ -> null },
    toggleButtonSize: Dp = 52.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = CircleShape,
    content: @Composable BoxScope.() -> Unit = {
        TestText(text = "Label")
    }
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = backgroundColor,
        border = border,
        toggleButtonSize = toggleButtonSize,
        interactionSource = interactionSource,
        shape = shape,
        content = content
    )
}

@Composable
private fun ToggleButtonWithDefaults(
    modifier: Modifier = Modifier,
    checked: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
    label: @Composable RowScope.() -> Unit = {
        TestText(
            text = "Label"
        )
    },
    selectionControl: @Composable () -> Unit = { TestImage() },
    icon: @Composable (BoxScope.() -> Unit)? = null,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    background: @Composable (enabled: Boolean, checked: Boolean) -> Modifier = { _, _ ->
        Modifier.background(BACKGROUND_COLOR)
    },
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = PaddingValues(
        start = CHIP_HORIZONTAL_PADDING,
        top = CHIP_VERTICAL_PADDING,
        end = CHIP_HORIZONTAL_PADDING,
        bottom = CHIP_VERTICAL_PADDING
    ),
    shape: Shape = CHIP_SHAPE,
    selectionControlWidth: Dp = 24.dp,
    selectionControlHeight: Dp = 24.dp
) = ToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    label = label,
    selectionControl = selectionControl,
    modifier = modifier,
    icon = icon,
    secondaryLabel = secondaryLabel,
    background = background,
    enabled = enabled,
    interactionSource = interactionSource,
    contentPadding = contentPadding,
    shape = shape,
    selectionControlWidth = selectionControlWidth,
    selectionControlHeight = selectionControlHeight
)

@Composable
private fun SplitToggleButtonWithDefaults(
    modifier: Modifier = Modifier,
    checked: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
    label: @Composable RowScope.() -> Unit = {
        TestText(
            text = "Primary label"
        )
    },
    onClick: () -> Unit = {},
    selectionControl: @Composable BoxScope.() -> Unit = { TestImage() },
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    backgroundColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> = { _, _ ->
        remember { mutableStateOf(BACKGROUND_COLOR) }
    },
    splitBackgroundColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> =
        { _, _ ->
            remember { mutableStateOf(SPLIT_BACKGROUND_OVERLAY) }
        },
    enabled: Boolean = true,
    checkedInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    clickInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = PaddingValues(
        start = CHIP_HORIZONTAL_PADDING,
        top = CHIP_VERTICAL_PADDING,
        end = CHIP_HORIZONTAL_PADDING,
        bottom = CHIP_VERTICAL_PADDING
    ),
    shape: Shape = CHIP_SHAPE
) = SplitToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    label = label,
    onClick = onClick,
    selectionControl = selectionControl,
    modifier = modifier,
    secondaryLabel = secondaryLabel,
    backgroundColor = backgroundColor,
    splitBackgroundColor = splitBackgroundColor,
    enabled = enabled,
    checkedInteractionSource = checkedInteractionSource,
    clickInteractionSource = clickInteractionSource,
    contentPadding = contentPadding,
    shape = shape
)

private val CHIP_HORIZONTAL_PADDING = 14.dp
private val CHIP_VERTICAL_PADDING = 6.dp
private val CHIP_SHAPE = RoundedCornerShape(corner = CornerSize(50))

private val CHECKED_COLOR = Color(0xFFA020F0)
private val UNCHECKED_COLOR = Color(0xFFFFA500)
private val DISABLED_CHECKED_COLOR = Color(0xFFA56D61)
private val DISABLED_UNCHECKED_COLOR = Color(0xFF904332)
private val BACKGROUND_COLOR = Color.Blue
private val SPLIT_BACKGROUND_OVERLAY = Color.Red
