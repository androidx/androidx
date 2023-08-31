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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class SelectionControlsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun checkbox_supports_testtag() {
        rule.setContentWithTheme {
            Checkbox(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun checkbox_is_expected_size() {
        rule.setContentWithThemeForSizeAssertions {
            Checkbox(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }.assertHeightIsEqualTo(24.dp).assertWidthIsEqualTo(32.dp)
    }

    @Test
    fun checkbox_has_role_checkbox_when_oncheckedchange_defined() {
        rule.setContentWithTheme {
            Checkbox(
                checked = true,
                onCheckedChange = {},
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
    fun checkbox_has_no_clickaction_by_default() {
        rule.setContentWithTheme {
            Checkbox(
                checked = true,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun checkbox_has_clickaction_when_oncheckedchange_defined() {
        rule.setContentWithTheme {
            Checkbox(
                checked = true,
                enabled = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun checkbox_is_toggleable_when_oncheckedchange_defined() {
        rule.setContentWithTheme {
            Checkbox(
                checked = true,
                enabled = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun checkbox_is_correctly_enabled() {
        rule.setContentWithTheme {
            Checkbox(
                checked = true,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun checkbox_is_correctly_disabled() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            Checkbox(
                checked = true,
                enabled = false,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun checkbox_is_on_when_checked() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            Checkbox(
                checked = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun checkbox_is_off_when_checked() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            Checkbox(
                checked = false,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun checkbox_responds_to_toggle_on() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
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
    fun checkbox_responds_to_toggle_off() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_checked_colors_are_customisable() {
        val boxColor = Color.Green
        val checkmarkColor = Color.Blue
        rule.setContentWithTheme {
            Checkbox(
                checked = true,
                colors = CheckboxDefaults.colors(
                    checkedBoxColor = boxColor,
                    checkedCheckmarkColor = checkmarkColor
                ),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColor)
        checkboxImage.assertContainsColor(checkmarkColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_unchecked_colors_are_customisable() {
        // NB checkmark is erased during animation, so we don't test uncheckedCheckmarkColor
        // as it is just used as a target color for the animation.
        val boxColor = Color.Green
        rule.setContentWithTheme {
            Checkbox(
                checked = false,
                colors = CheckboxDefaults.colors(
                    uncheckedBoxColor = boxColor,
                ),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColor)
    }

    @Test
    fun switch_supports_testtag() {
        rule.setContentWithTheme {
            Switch(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun switch_is_expected_size() {
        rule.setContentWithThemeForSizeAssertions {
            Switch(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }.assertHeightIsEqualTo(24.dp).assertWidthIsEqualTo(32.dp)
    }

    @Test
    fun switch_has_role_switch_when_oncheckedchange_defined() {
        rule.setContentWithTheme {
            Switch(
                checked = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Switch
                )
            )
    }

    @Test
    fun switch_has_no_clickaction_by_default() {
        rule.setContentWithTheme {
            Switch(
                checked = true,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun switch_has_clickaction_when_oncheckedchange_defined() {
        rule.setContentWithTheme {
            Switch(
                checked = true,
                enabled = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun switch_is_toggleable_when_oncheckedchange_defined() {
        rule.setContentWithTheme {
            Switch(
                checked = true,
                enabled = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun switch_is_correctly_enabled_when_enabled_equals_true() {
        rule.setContentWithTheme {
            Switch(
                checked = true,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun switch_is_correctly_disabled_when_enabled_equals_false() {
        rule.setContentWithTheme {
            Switch(
                checked = true,
                enabled = false,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun switch_is_on_when_checked() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            Switch(
                checked = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun switch_is_off_when_checked() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            Switch(
                checked = false,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun switch_responds_to_toggle_on() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
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
    fun switch_responds_to_toggle_off() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun switch_checked_colors_are_customisable() {
        val thumbColor = Color.Green
        val thumbIconColor = Color.Yellow
        val trackColor = Color.Blue
        val trackStrokeColor = Color.Red
        rule.setContentWithTheme {
            Switch(
                checked = true,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = thumbColor,
                    checkedThumbIconColor = thumbIconColor,
                    checkedTrackColor = trackColor,
                    checkedTrackBorderColor = trackStrokeColor
                ),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()
        image.assertContainsColor(thumbColor)
        image.assertContainsColor(thumbIconColor)
        image.assertContainsColor(trackColor)
        image.assertContainsColor(trackStrokeColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun switch_unchecked_colors_are_customisable() {
        val thumbColor = Color.Green
        val thumbIconColor = Color.Yellow
        val trackColor = Color.Blue
        val trackStrokeColor = Color.Red
        rule.setContentWithTheme {
            Switch(
                checked = false,
                colors = SwitchDefaults.colors(
                    uncheckedThumbColor = thumbColor,
                    uncheckedThumbIconColor = thumbIconColor,
                    uncheckedTrackColor = trackColor,
                    uncheckedTrackBorderColor = trackStrokeColor
                ),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()
        image.assertContainsColor(thumbColor)
        // NB opacity of icon is decreased to 0 in unchecked state, hence we are asserting that the thumbIconColor should not exist.
        image.assertDoesNotContainColor(thumbIconColor)
        image.assertContainsColor(trackColor)
    }

    @Test
    fun radiobutton_supports_testtag() {
        rule.setContentWithTheme {
            RadioButton(
                selected = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun radiobutton_is_expected_size() {
        rule.setContentWithThemeForSizeAssertions {
            Switch(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }.assertHeightIsEqualTo(24.dp).assertWidthIsEqualTo(32.dp)
    }

    @Test
    fun radiobutton_has_role_radiobutton_when_onclick_defined() {
        rule.setContentWithTheme {
            RadioButton(
                selected = true,
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.RadioButton
                )
            )
    }

    @Test
    fun radiobutton_has_no_clickaction_by_default() {
        rule.setContentWithTheme {
            RadioButton(
                selected = true,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun radiobutton_has_clickaction_when_onclick_defined() {
        rule.setContentWithTheme {
            RadioButton(
                selected = true,
                enabled = true,
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun radiobutton_is_selectable_when_onclick_defined() {
        rule.setContentWithTheme {
            RadioButton(
                selected = true,
                enabled = true,
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isSelectable()).assertExists()
    }

    @Test
    fun radiobutton_is_correctly_enabled() {
        rule.setContentWithTheme {
            RadioButton(
                selected = true,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun radiobutton_is_correctly_disabled() {
        // This test only applies when onClick is provided and the RadioButton itself is selectable.
        rule.setContentWithTheme {
            RadioButton(
                selected = true,
                enabled = false,
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun radiobutton_is_on_when_checked() {
        // This test only applies when onClick is provided and the RadioButton itself is selectable.
        rule.setContentWithTheme {
            RadioButton(
                selected = true,
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsSelected()
    }

    @Test
    fun radiobutton_is_off_when_checked() {
        // This test only applies when onClick is provided and the RadioButton itself is selectable.
        rule.setContentWithTheme {
            RadioButton(
                selected = false,
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected()
    }

    @Test
    fun radiobutton_responds_to_toggle_on() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            var selected by remember { mutableStateOf(false) }
            RadioButton(
                selected = selected,
                onClick = { selected = !selected },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsNotSelected()
            .performClick()
            .assertIsSelected()
    }

    @Test
    fun radiobutton_responds_to_toggle_off() {
        // This test only applies when onCheckedChange is defined.
        rule.setContentWithTheme {
            var selected by remember { mutableStateOf(true) }
            RadioButton(
                selected = selected,
                onClick = { selected = !selected },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsSelected()
            .performClick()
            .assertIsNotSelected()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun radiobutton_selected_colors_are_customisable() {
        val color = Color.Green
        rule.setContentWithTheme {
            RadioButton(
                selected = true,
                colors = RadioButtonDefaults.colors(
                    selectedColor = color
                ),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()
        image.assertContainsColor(color)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun radiobutton_unselected_colors_are_customisable() {
        // NB Dot is erased during animation, so we don't test uncheckedDotColor
        // as it is just used as a target color for the animation.
        val color = Color.Green
        rule.setContentWithTheme {
            RadioButton(
                selected = false,
                colors = RadioButtonDefaults.colors(
                    unselectedColor = color,
                ),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()
        image.assertContainsColor(color)
    }
}
