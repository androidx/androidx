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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class SelectionControlsTest {
    @get:Rule
    val rule = createComposeRule()

    // Checkbox colors
    private val boxColorChecked = Color.Green
    private val boxColorUnchecked = Color.Blue
    private val checkmarkColorChecked = Color.Red
    private val checkmarkColorUnchecked = Color.Yellow
    private val boxColorDisabledChecked = Color.Cyan
    private val boxColorDisabledUnchecked = Color.Magenta
    private val checkmarkColorDisabledChecked = Color.DarkGray
    private val checkmarkColorDisabledUnchecked = Color.White

    // Switch colors
    private val trackColorChecked = Color.Green
    private val trackColorUnchecked = Color.Blue
    private val trackColorDisabledChecked = Color.Cyan
    private val trackColorDisabledUnchecked = Color.Magenta
    private val trackStrokeColorChecked = Color.Red
    private val trackStrokeColorUnchecked = Color.Yellow
    private val trackStrokeColorDisabledChecked = Color.DarkGray
    private val trackStrokeColorDisabledUnchecked = Color.White
    private val thumbColorChecked = Color(0xFFA020F0)
    private val thumbColorUnchecked = Color(0xFFFFA500)
    private val thumbColorDisabledChecked = Color(0xFFA56D61)
    private val thumbColorDisabledUnchecked = Color(0xFF904332)
    private val thumbIconColorChecked = Color(0xFF0000FF)
    private val thumbIconColorUnchecked = Color(0xFF808000)
    private val thumbIconColorDisabledChecked = Color(0xFFCCCCFF)
    private val thumbIconColorDisabledUnchecked = Color(0xFFE3F48D)

    // Radio button colors
    private val radioRingChecked = Color.Green
    private val radioRingUnchecked = Color.Blue
    private val radioDotChecked = Color.Red
    private val radioDotUnchecked = Color.Yellow
    private val radioRingDisabledChecked = Color.Cyan
    private val radioRingDisabledUnchecked = Color.Magenta
    private val radioDotDisabledChecked = Color.DarkGray
    private val radioDotDisabledUnchecked = Color.White

    @Test
    fun checkbox_supports_testtag() {
        rule.setContent {
            CheckboxWithDefaults(
                checked = true, modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun checkbox_customize_canvas_size() {
        val width = 32.dp
        val height = 26.dp

        rule.setContentForSizeAssertions {
            CheckboxWithDefaults(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG),
                width = width,
                height = height
            )
        }.assertHeightIsEqualTo(height).assertWidthIsEqualTo(width)
    }

    @Test
    fun checkbox_has_role_checkbox_when_oncheckedchange_defined() {
        rule.setContent {
            CheckboxWithDefaults(
                checked = true, onCheckedChange = {}, modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role, Role.Checkbox
            )
        )
    }

    @Test
    fun checkbox_can_override_role() {
        rule.setContent {
            CheckboxWithDefaults(
                checked = true,
                onCheckedChange = {},
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .semantics {
                        role = Role.Image
                    }
            )
        }

        rule.onNodeWithTag(TEST_TAG).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role, Role.Image
            )
        )
    }

    @Test
    fun checkbox_has_no_clickaction_by_default() {
        rule.setContent {
            CheckboxWithDefaults(
                checked = true, enabled = true, modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun checkbox_has_clickaction_when_oncheckedchange_defined() {
        rule.setContent {
            CheckboxWithDefaults(
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
        rule.setContent {
            CheckboxWithDefaults(
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
        rule.setContent {
            CheckboxWithDefaults(
                checked = true, enabled = true, modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun checkbox_is_correctly_disabled() {
        // This test only applies when onCheckedChange is defined.
        rule.setContent {
            CheckboxWithDefaults(
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
        rule.setContent {
            CheckboxWithDefaults(
                checked = true, onCheckedChange = {}, modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun checkbox_is_off_when_unchecked() {
        // This test only applies when onCheckedChange is defined.
        rule.setContent {
            CheckboxWithDefaults(
                checked = false, onCheckedChange = {}, modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun checkbox_responds_to_toggle_on() {
        // This test only applies when onCheckedChange is defined.
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            CheckboxWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff().performClick()
            .assertIsOn()
    }

    @Test
    fun checkbox_responds_to_toggle_off() {
        // This test only applies when onCheckedChange is defined.
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            CheckboxWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn().performClick()
            .assertIsOff()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_enabled_checked_colors_are_customisable() {
        setupCheckBoxWithCustomColors(enabled = true, checked = true)

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColorChecked)
        checkboxImage.assertContainsColor(checkmarkColorChecked)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_enabled_unchecked_colors_are_customisable() {
        setupCheckBoxWithCustomColors(enabled = true, checked = false)

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColorUnchecked)
        checkboxImage.assertDoesNotContainColor(checkmarkColorChecked)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_disabled_checked_colors_are_customisable() {
        setupCheckBoxWithCustomColors(enabled = false, checked = true)

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColorDisabledChecked)
        checkboxImage.assertContainsColor(
            hardLightBlend(
                boxColorDisabledChecked,
                boxColorDisabledChecked
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_disabled_unchecked_colors_are_customisable() {
        setupCheckBoxWithCustomColors(enabled = false, checked = false)

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColorDisabledUnchecked)
        checkboxImage.assertDoesNotContainColor(checkmarkColorDisabledChecked)
    }

    @Test
    fun switch_supports_testtag() {
        rule.setContent {
            SwitchWithDefaults(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun switch_has_role_switch_when_oncheckedchange_defined() {
        rule.setContent {
            SwitchWithDefaults(
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
    fun switch_can_override_role() {
        rule.setContent {
            SwitchWithDefaults(
                checked = true,
                onCheckedChange = {},
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .semantics {
                        role = Role.Image
                    }
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Image
                )
            )
    }

    @Test
    fun switch_has_no_clickaction_by_default() {
        rule.setContent {
            SwitchWithDefaults(
                checked = true,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun switch_has_clickaction_when_oncheckedchange_defined() {
        rule.setContent {
            SwitchWithDefaults(
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
        rule.setContent {
            SwitchWithDefaults(
                checked = true,
                enabled = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun switch_is_correctly_enabled() {
        rule.setContent {
            SwitchWithDefaults(
                checked = true,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun switch_is_correctly_disabled() {
        rule.setContent {
            SwitchWithDefaults(
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
        rule.setContent {
            SwitchWithDefaults(
                checked = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun switch_is_off_when_unchecked() {
        // This test only applies when onCheckedChange is defined.
        rule.setContent {
            SwitchWithDefaults(
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
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            SwitchWithDefaults(
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
        rule.setContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            SwitchWithDefaults(
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

    @Test
    fun switch_customize_canvas_size() {
        val width = 34.dp
        val height = 26.dp

        rule.setContentForSizeAssertions {
            CheckboxWithDefaults(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG),
                width = width,
                height = height
            )
        }.assertHeightIsEqualTo(height).assertWidthIsEqualTo(width)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun switch_enabled_checked_colors_are_customisable() {
        setupSwitchWithCustomColors(enabled = true, checked = true)

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()

        image.assertContainsColor(trackColorChecked)
        image.assertContainsColor(trackStrokeColorChecked)
        image.assertContainsColor(thumbColorChecked)
        image.assertContainsColor(thumbIconColorChecked)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun switch_enabled_unchecked_colors_are_customisable() {
        setupSwitchWithCustomColors(enabled = true, checked = false)

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()

        image.assertContainsColor(trackColorUnchecked)
        image.assertContainsColor(trackStrokeColorUnchecked)
        image.assertContainsColor(thumbColorUnchecked)
        image.assertContainsColor(thumbIconColorUnchecked)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun switch_disabled_checked_colors_are_customisable() {
        setupSwitchWithCustomColors(enabled = false, checked = true)

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()

        image.assertContainsColor(trackColorDisabledChecked)
        image.assertContainsColor(trackStrokeColorDisabledChecked)
        image.assertContainsColor(thumbColorDisabledChecked)
        image.assertContainsColor(thumbIconColorDisabledChecked)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun switch_disabled_unchecked_colors_are_customisable() {
        setupSwitchWithCustomColors(enabled = false, checked = false)

        val image = rule.onNodeWithTag(TEST_TAG).captureToImage()

        image.assertContainsColor(trackColorDisabledUnchecked)
        image.assertContainsColor(trackStrokeColorDisabledUnchecked)
        image.assertContainsColor(thumbColorDisabledUnchecked)
        image.assertContainsColor(thumbIconColorDisabledUnchecked)
    }

    @Test
    fun radiobutton_supports_testtag() {
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = true
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun radiobutton_is_expected_size() {
        val width = 30.dp
        val height = 26.dp

        rule.setContentForSizeAssertions {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = true,
                width = width,
                height = height
            )
        }.assertHeightIsEqualTo(height).assertWidthIsEqualTo(width)
    }

    @Test
    fun radiobutton_has_role_radiobutton_when_onclick_defined() {
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = true,
                onClick = {}
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
    fun radiobutton_can_override_role() {
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .semantics {
                        role = Role.Image
                    },
                selected = true,
                onClick = {}
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Image
                )
            )
    }

    @Test
    fun radiobutton_has_no_clickaction_by_default() {
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = true,
                enabled = true
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun radiobutton_has_clickaction_when_onclick_defined() {
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = true,
                enabled = true,
                onClick = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun radiobutton_is_selectable_when_onclick_defined() {
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = true,
                enabled = true,
                onClick = {},
            )
        }

        rule.onNode(isSelectable()).assertExists()
    }

    @Test
    fun radiobutton_is_correctly_enabled() {
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = true,
                enabled = true,
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun radiobutton_is_correctly_disabled() {
        // This test only applies when onClick is provided and the RadioButton itself is selectable.
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = true,
                enabled = false,
                onClick = {}
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun radiobutton_is_on_when_checked() {
        // This test only applies when onClick is provided and the RadioButton itself is selectable.
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = true,
                onClick = {}
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsSelected()
    }

    @Test
    fun radiobutton_is_off_when_checked() {
        // This test only applies when onClick is provided and the RadioButton itself is selectable.
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = false,
                onClick = {}
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected()
    }

    @Test
    fun radiobutton_responds_to_toggle_on() {
        // This test only applies when onCheckedChange is defined.
        rule.setContent {
            var selected by remember { mutableStateOf(false) }

            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = selected,
                onClick = { selected = !selected }
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
        rule.setContent {
            var selected by remember { mutableStateOf(true) }

            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = selected,
                onClick = { selected = !selected }
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
    fun radiobutton_enabled_checked_colors_are_customisable() {
        setupRadioButtonWithCustomColors(enabled = true, selected = true)

        val radioImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        radioImage.assertContainsColor(radioRingChecked)
        radioImage.assertContainsColor(radioDotChecked)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun radiobutton_enabled_unchecked_colors_are_customisable() {
        setupRadioButtonWithCustomColors(enabled = true, selected = false)

        val radioImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        radioImage.assertContainsColor(radioRingUnchecked)
        radioImage.assertDoesNotContainColor(radioDotUnchecked)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun radiobutton_disabled_checked_colors_are_customisable() {
        setupRadioButtonWithCustomColors(enabled = false, selected = true)

        val radioImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        radioImage.assertContainsColor(radioRingDisabledChecked)
        radioImage.assertContainsColor(radioDotDisabledChecked)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun radiobutton_disabled_unchecked_colors_are_customisable() {
        setupRadioButtonWithCustomColors(enabled = false, selected = false)

        val radioImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        radioImage.assertContainsColor(radioRingDisabledUnchecked)
        radioImage.assertDoesNotContainColor(radioDotDisabledUnchecked)
    }

    @Composable
    private fun CheckboxWithDefaults(
        modifier: Modifier = Modifier,
        checked: Boolean = true,
        boxColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> =
            { isEnabled, isChecked ->
                selectionControlColor(
                    isEnabled, isChecked,
                    Color.Blue, Color.Red, Color.Green, Color.Gray
                )
            },
        checkmarkColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> =
            { isEnabled, isChecked ->
                selectionControlColor(
                    isEnabled, isChecked,
                    Color.Cyan, Color.Magenta, Color.White, Color.Yellow
                )
            },
        enabled: Boolean = true,
        onCheckedChange: ((Boolean) -> Unit)? = null,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        drawBox: FunctionDrawBox = FunctionDrawBox { _, _, _, _ -> },
        width: Dp = 24.dp,
        height: Dp = 24.dp
    ) = Checkbox(
        checked = checked,
        modifier = modifier,
        boxColor = boxColor,
        checkmarkColor = checkmarkColor,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        interactionSource = interactionSource,
        drawBox = drawBox,
        progressAnimationSpec =
        tween(200, 0, CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)),
        width = width,
        height = height
    )

    @Composable
    private fun SwitchWithDefaults(
        modifier: Modifier = Modifier,
        checked: Boolean = true,
        enabled: Boolean = true,
        onCheckedChange: ((Boolean) -> Unit)? = null,
        interactionSource: MutableInteractionSource = remember {
            MutableInteractionSource()
        },
        trackFillColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> =
            { isEnabled, isChecked ->
                selectionControlColor(
                    isEnabled, isChecked,
                    Color.Blue, Color.Red, Color.Green, Color.Gray
                )
            },
        trackStrokeColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> =
            { isEnabled, isChecked ->
                selectionControlColor(
                    isEnabled, isChecked,
                    Color.Blue, Color.Red, Color.Green, Color.Gray
                )
            },
        thumbColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> =
            { isEnabled, isChecked ->
                selectionControlColor(
                    isEnabled, isChecked,
                    Color.Cyan, Color.Magenta, Color.White, Color.Yellow
                )
            },
        thumbIconColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> =
            { isEnabled, isChecked ->
                selectionControlColor(
                    isEnabled, isChecked,
                    Color.Cyan, Color.Magenta, Color.White, Color.Yellow
                )
            },
        trackWidth: Dp = 32.dp,
        trackHeight: Dp = 24.dp,
        drawThumb: FunctionDrawThumb = FunctionDrawThumb { _, _, _, _, _ -> },
        width: Dp = 32.dp,
        height: Dp = 24.dp
    ) = Switch(
        checked = checked,
        modifier = modifier,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        interactionSource = interactionSource,
        trackFillColor = trackFillColor,
        trackStrokeColor = trackStrokeColor,
        thumbColor = thumbColor,
        thumbIconColor = thumbIconColor,
        trackWidth = trackWidth,
        trackHeight = trackHeight,
        drawThumb = drawThumb,
        progressAnimationSpec =
        tween(150, 0, CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)),
        width = width,
        height = height
    )

    @Composable
    private fun RadioButtonWithDefaults(
        modifier: Modifier = Modifier,
        selected: Boolean = true,
        enabled: Boolean = true,
        ringColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> =
            { isEnabled, isChecked ->
                selectionControlColor(
                    isEnabled, isChecked,
                    Color.Blue, Color.Red, Color.Green, Color.Gray
                )
            },
        dotColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color> =
            { isEnabled, isChecked ->
                selectionControlColor(
                    isEnabled, isChecked,
                    Color.Blue, Color.Red, Color.Green, Color.Gray
                )
            },
        onClick: (() -> Unit)? = null,
        interactionSource: MutableInteractionSource = remember {
            MutableInteractionSource()
        },
        dotRadiusProgressDuration: FunctionDotRadiusProgressDuration =
            FunctionDotRadiusProgressDuration { _ -> 200 },
        dotAlphaProgressDuration: Int = 200,
        dotAlphaProgressDelay: Int = 100,
        progressAnimationEasing: CubicBezierEasing =
            CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f),
        width: Dp = 32.dp,
        height: Dp = 24.dp
    ) = RadioButton(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        ringColor = ringColor,
        dotColor = dotColor,
        onClick = onClick,
        interactionSource = interactionSource,
        dotRadiusProgressDuration = dotRadiusProgressDuration,
        dotAlphaProgressDuration = dotAlphaProgressDuration,
        dotAlphaProgressDelay = dotAlphaProgressDelay,
        easing = progressAnimationEasing,
        width = width,
        height = height
    )

    private fun setupCheckBoxWithCustomColors(checked: Boolean, enabled: Boolean) {
        rule.setContent {
            CheckboxWithDefaults(checked = checked,
                enabled = enabled,
                modifier = Modifier.testTag(TEST_TAG),
                boxColor = { enabled, checked ->
                    selectionControlColor(
                        enabled = enabled,
                        checked = checked,
                        checkedColor = boxColorChecked,
                        uncheckedColor = boxColorUnchecked,
                        disabledCheckedColor = boxColorDisabledChecked,
                        disabledUncheckedColor = boxColorDisabledUnchecked
                    )
                },
                checkmarkColor = { enabled, checked ->
                    selectionControlColor(
                        enabled = enabled,
                        checked = checked,
                        checkedColor = checkmarkColorChecked,
                        uncheckedColor = checkmarkColorUnchecked,
                        disabledCheckedColor = checkmarkColorDisabledChecked,
                        disabledUncheckedColor = checkmarkColorDisabledUnchecked
                    )
                },
                drawBox = { drawScope, color, _, _ ->
                    drawScope.drawRoundRect(color)
                })
        }
    }

    private fun setupSwitchWithCustomColors(checked: Boolean, enabled: Boolean) {
        rule.setContent {
            SwitchWithDefaults(
                checked = checked,
                enabled = enabled,
                modifier = Modifier.testTag(TEST_TAG),
                trackFillColor = { enabled, checked ->
                    selectionControlColor(
                        enabled = enabled,
                        checked = checked,
                        checkedColor = trackColorChecked,
                        uncheckedColor = trackColorUnchecked,
                        disabledCheckedColor = trackColorDisabledChecked,
                        disabledUncheckedColor = trackColorDisabledUnchecked
                    )
                },
                trackStrokeColor = { enabled, checked ->
                    selectionControlColor(
                        enabled = enabled,
                        checked = checked,
                        checkedColor = trackStrokeColorChecked,
                        uncheckedColor = trackStrokeColorUnchecked,
                        disabledCheckedColor = trackStrokeColorDisabledChecked,
                        disabledUncheckedColor = trackStrokeColorDisabledUnchecked
                    )
                },
                thumbColor = { enabled, checked ->
                    selectionControlColor(
                        enabled = enabled,
                        checked = checked,
                        checkedColor = thumbColorChecked,
                        uncheckedColor = thumbColorUnchecked,
                        disabledCheckedColor = thumbColorDisabledChecked,
                        disabledUncheckedColor = thumbColorDisabledUnchecked
                    )
                },
                thumbIconColor = { enabled, checked ->
                    selectionControlColor(
                        enabled = enabled,
                        checked = checked,
                        checkedColor = thumbIconColorChecked,
                        uncheckedColor = thumbIconColorUnchecked,
                        disabledCheckedColor = thumbIconColorDisabledChecked,
                        disabledUncheckedColor = thumbIconColorDisabledUnchecked
                    )
                },
                drawThumb = { drawScope, thumbColor, _, thumbIconColor, _ ->
                    // drawing
                    drawScope.drawCircle(
                        color = thumbColor,
                        radius = with(drawScope) { 10.dp.toPx() }
                    )
                    // drawing thumb icon
                    drawScope.drawCircle(
                        color = thumbIconColor,
                        radius = with(drawScope) { 5.dp.toPx() }
                    )
                },
            )
        }
    }

    private fun setupRadioButtonWithCustomColors(selected: Boolean, enabled: Boolean) {
        rule.setContent {
            RadioButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                selected = selected,
                enabled = enabled,
                ringColor = { enabled, checked ->
                    selectionControlColor(
                        enabled = enabled,
                        checked = checked,
                        checkedColor = radioRingChecked,
                        uncheckedColor = radioRingUnchecked,
                        disabledCheckedColor = radioRingDisabledChecked,
                        disabledUncheckedColor = radioRingDisabledUnchecked
                    )
                },
                dotColor = { enabled, checked ->
                    selectionControlColor(
                        enabled = enabled,
                        checked = checked,
                        checkedColor = radioDotChecked,
                        uncheckedColor = radioDotUnchecked,
                        disabledCheckedColor = radioDotDisabledChecked,
                        disabledUncheckedColor = radioDotDisabledUnchecked
                    )
                }
            )
        }
    }

    // Formula taken from https://en.wikipedia.org/wiki/Blend_modes#Hard_Light
    private fun hardLightBlend(colorA: Color, colorB: Color): Color {
        fun blendChannel(a: Float, b: Float): Float {
            return if (b < 0.5f) {
                2 * a * b
            } else {
                1 - 2 * (1 - a) * (1 - b)
            }
        }

        val blendedRed = blendChannel(colorA.red, colorB.red)
        val blendedGreen = blendChannel(colorA.green, colorB.green)
        val blendedBlue = blendChannel(colorA.blue, colorB.blue)

        return Color(red = blendedRed, green = blendedGreen, blue = blendedBlue)
    }

    @Composable
    private fun selectionControlColor(
        enabled: Boolean,
        checked: Boolean,
        checkedColor: Color,
        uncheckedColor: Color,
        disabledCheckedColor: Color,
        disabledUncheckedColor: Color
    ) = animateColorAsState(
        if (enabled) {
            if (checked) checkedColor else uncheckedColor
        } else {
            if (checked) disabledCheckedColor else disabledUncheckedColor
        }
    )
}
