/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.samples.CheckboxButtonSample
import androidx.wear.compose.material3.samples.SplitCheckboxButtonSample
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class CheckboxButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun split_button_supports_testtag() {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun checkbox_button_samples_build() {
        rule.setContentWithTheme { CheckboxButtonSample() }
    }

    @Test
    fun split_checkbox_button_samples_build() {
        rule.setContentWithTheme { SplitCheckboxButtonSample() }
    }

    @Test
    fun has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_button_has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(enabled = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_button_has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(enabled = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun is_toggleable() {
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun split_button_is_toggleable() {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun split_button_is_clickable() {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }
        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun split_button_is_correctly_enabled() {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(enabled = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun split_button_is_correctly_disabled() {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(enabled = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertIsNotEnabled()
    }

    @Test
    fun is_on_when_checked() {
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(checked = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun split_button_is_on_when_checked() {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(checked = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOn()
    }

    @Test
    fun is_off_when_unchecked() {
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(checked = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun split_button_is_off_when_unchecked() {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(checked = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOff()
    }

    @Test
    fun responds_to_toggle_on() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            CheckboxButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff().performClick().assertIsOn()
    }

    @Test
    fun split_button_responds_to_toggle_on() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            SplitCheckboxButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOff().performClick().assertIsOn()
    }

    @Test
    fun responds_to_toggle_off() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            CheckboxButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn().performClick().assertIsOff()
    }

    @Test
    fun split_button_responds_to_toggle_off() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            SplitCheckboxButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOn().performClick().assertIsOff()
    }

    @Test
    fun does_not_toggle_when_disabled() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            CheckboxButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff().performClick().assertIsOff()
    }

    @Test
    fun split_button_does_not_toggle_when_disabled() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            SplitCheckboxButtonWithDefaults(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOff().performClick().assertIsOff()
    }

    @Test
    fun can_override_role() {
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG).semantics { role = Role.Button }
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun split_button_clickable_has_role_button() {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        // NB The toggle control provides its own role,
        // but the main clickable section is a Button.
        rule
            .onNodeWithTag(TEST_TAG)
            .onChildAt(0)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun displays_label_content() {
        val textContent = "abc"

        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = { Text(text = textContent) }
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @Test
    fun split_button_displays_label_content() {
        val textContent = "abc"

        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = { Text(text = textContent) }
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @Test
    fun checkbox_button_hasAdjustableHeight() {
        val minHeight: Dp = 53.dp

        rule
            .setContentWithThemeForSizeAssertions {
                CheckboxButtonWithDefaults(
                    label = {
                        Text(
                            text =
                                "ToggleButton text spanning over multiple lines of text " +
                                    "to test height is adjustable. This should exceed the minimum height" +
                                    " for the ToggleButton."
                        )
                    },
                    secondaryLabel = { Text(text = "Secondary label with text.") }
                )
            }
            .assertHeightIsAtLeast(minHeight)
    }

    @Test
    fun split_checkbox_button_hasAdjustableHeight() {
        val minHeight: Dp = 53.dp

        rule
            .setContentWithThemeForSizeAssertions {
                SplitCheckboxButtonWithDefaults(
                    label = { Text(text = "Primary label with 3 lines of text.") },
                    secondaryLabel = {
                        Text(
                            text =
                                "SplitToggleButton text spanning over multiple lines of text " +
                                    "to test height is adjustable. This should exceed the minimum height" +
                                    " for the SplitToggleButton."
                        )
                    }
                )
            }
            .assertHeightIsAtLeast(minHeight)
    }

    @Test
    fun checkbox_button_height_defaults_52dp() {
        rule
            .setContentWithThemeForSizeAssertions {
                CheckboxButtonWithDefaults(secondaryLabel = { Text("Secondary label") })
            }
            .assertHeightIsEqualTo(52.dp)
    }

    @Test
    fun split_checkbox_button_height_defaults_52dp() {
        rule
            .setContentWithThemeForSizeAssertions { SplitCheckboxButtonWithDefaults() }
            .assertHeightIsEqualTo(52.dp)
    }

    @Test
    fun checkbox_defines_default_textalign() {
        var labelTextAlign: TextAlign? = null
        var secondaryLabelTextAlign: TextAlign? = null

        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = { labelTextAlign = LocalTextConfiguration.current.textAlign },
                secondaryLabel = {
                    secondaryLabelTextAlign = LocalTextConfiguration.current.textAlign
                },
            )
        }

        Assert.assertEquals(TextAlign.Start, labelTextAlign)
        Assert.assertEquals(TextAlign.Start, secondaryLabelTextAlign)
    }

    @Test
    fun splitcheckbox_defines_default_textalign() {
        var labelTextAlign: TextAlign? = null
        var secondaryLabelTextAlign: TextAlign? = null

        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = { labelTextAlign = LocalTextConfiguration.current.textAlign },
                secondaryLabel = {
                    secondaryLabelTextAlign = LocalTextConfiguration.current.textAlign
                },
            )
        }

        Assert.assertEquals(TextAlign.Start, labelTextAlign)
        Assert.assertEquals(TextAlign.Start, secondaryLabelTextAlign)
    }

    @Test
    fun checkbox_defines_default_overflow() {
        var labelOverflow: TextOverflow? = null
        var secondaryLabelOverflow: TextOverflow? = null

        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = { labelOverflow = LocalTextConfiguration.current.overflow },
                secondaryLabel = {
                    secondaryLabelOverflow = LocalTextConfiguration.current.overflow
                },
            )
        }

        Assert.assertEquals(TextOverflow.Ellipsis, labelOverflow)
        Assert.assertEquals(TextOverflow.Ellipsis, secondaryLabelOverflow)
    }

    @Test
    fun splitcheckbox_defines_default_overflow() {
        var labelOverflow: TextOverflow? = null
        var secondaryLabelOverflow: TextOverflow? = null

        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = { labelOverflow = LocalTextConfiguration.current.overflow },
                secondaryLabel = {
                    secondaryLabelOverflow = LocalTextConfiguration.current.overflow
                },
            )
        }

        Assert.assertEquals(TextOverflow.Ellipsis, labelOverflow)
        Assert.assertEquals(TextOverflow.Ellipsis, secondaryLabelOverflow)
    }

    @Test
    fun checkbox_defines_default_maxlines() {
        var labelMaxLines: Int? = null
        var secondaryLabelMaxLines: Int? = null

        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = { labelMaxLines = LocalTextConfiguration.current.maxLines },
                secondaryLabel = {
                    secondaryLabelMaxLines = LocalTextConfiguration.current.maxLines
                },
            )
        }

        Assert.assertEquals(3, labelMaxLines)
        Assert.assertEquals(2, secondaryLabelMaxLines)
    }

    @Test
    fun splitcheckbox_defines_default_maxlines() {
        var labelMaxLines: Int? = null
        var secondaryLabelMaxLines: Int? = null

        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = { labelMaxLines = LocalTextConfiguration.current.maxLines },
                secondaryLabel = {
                    secondaryLabelMaxLines = LocalTextConfiguration.current.maxLines
                },
            )
        }

        Assert.assertEquals(3, labelMaxLines)
        Assert.assertEquals(2, secondaryLabelMaxLines)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_button_allows_checked_background_color_override() =
        verifyToggleButtonBackgroundColor(
            checked = true,
            enabled = true,
            expectedColor = CHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_button_allows_unchecked_background_color_override() =
        verifyToggleButtonBackgroundColor(
            checked = false,
            enabled = true,
            expectedColor = UNCHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_checkbox_button_allows_checked_background_color_override() =
        verifySplitToggleButtonBackgroundColor(
            checked = true,
            enabled = true,
            expectedColor = CHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_checkbox_button_allows_unchecked_background_color_override() =
        verifySplitToggleButtonBackgroundColor(
            checked = false,
            enabled = true,
            expectedColor = UNCHECKED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun verify_checkbox_button_colors_enabled_and_checked() {
        rule.verifyCheckboxButtonColors(checked = true, enabled = true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun verify_checkbox_button_colors_enabled_and_unchecked() {
        rule.verifyCheckboxButtonColors(checked = false, enabled = true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun verify_checkbox_button_colors_disabled_and_checked() {
        rule.verifyCheckboxButtonColors(checked = true, enabled = false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun verify_checkbox_button_colors_disabled_and_unchecked() {
        rule.verifyCheckboxButtonColors(checked = false, enabled = false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun verify_split_checkbox_button_colors_enabled_and_checked() {
        rule.verifySplitCheckboxButtonColors(checked = true, enabled = true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun verify_split_checkbox_button_colors_enabled_and_unchecked() {
        rule.verifySplitCheckboxButtonColors(checked = false, enabled = true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun verify_split_checkbox_button_colors_disabled_and_checked() {
        rule.verifySplitCheckboxButtonColors(checked = true, enabled = false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun verify_split_checkbox_button_colors_disabled_and_unchecked() {
        rule.verifySplitCheckboxButtonColors(checked = false, enabled = false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun checkbox_checked_colors_are_customisable() {
        val boxColor = Color.Green
        val checkmarkColor = Color.Blue
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(
                enabled = true,
                checked = true,
                colors =
                    CheckboxButtonDefaults.checkboxButtonColors(
                        checkedBoxColor = boxColor,
                        checkedCheckmarkColor = checkmarkColor
                    ),
                modifier = Modifier.testTag(TEST_TAG),
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
            CheckboxButtonWithDefaults(
                enabled = true,
                checked = false,
                colors =
                    CheckboxButtonDefaults.checkboxButtonColors(
                        uncheckedBoxColor = boxColor,
                    ),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun disabled_checkbox_checked_colors_are_customisable() {
        val boxColor = Color.Green
        val checkmarkColor = Color.Blue
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(
                enabled = false,
                checked = true,
                colors =
                    CheckboxButtonDefaults.checkboxButtonColors(
                        disabledCheckedBoxColor = boxColor,
                        disabledCheckedCheckmarkColor = checkmarkColor
                    ),
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColor)
        checkboxImage.assertContainsColor(checkmarkColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun disabled_checkbox_unchecked_colors_are_customisable() {
        // NB checkmark is erased during animation, so we don't test uncheckedCheckmarkColor
        // as it is just used as a target color for the animation.
        val boxColor = Color.Green
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(
                enabled = false,
                checked = false,
                colors =
                    CheckboxButtonDefaults.checkboxButtonColors(
                        disabledUncheckedBoxColor = boxColor,
                    ),
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        val checkboxImage = rule.onNodeWithTag(TEST_TAG).captureToImage()
        checkboxImage.assertContainsColor(boxColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyToggleButtonBackgroundColor(
        checked: Boolean,
        enabled: Boolean,
        expectedColor: Color
    ) {
        rule.setContentWithTheme {
            CheckboxButtonWithDefaults(
                checked = checked,
                colors =
                    CheckboxButtonDefaults.checkboxButtonColors(
                        checkedContainerColor = CHECKED_COLOR,
                        uncheckedContainerColor = UNCHECKED_COLOR
                    ),
                onCheckedChange = {},
                enabled = enabled,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifySplitToggleButtonBackgroundColor(
        checked: Boolean,
        enabled: Boolean,
        expectedColor: Color
    ) {
        rule.setContentWithTheme {
            SplitCheckboxButtonWithDefaults(
                checked = checked,
                colors =
                    CheckboxButtonDefaults.splitCheckboxButtonColors(
                        checkedContainerColor = CHECKED_COLOR,
                        uncheckedContainerColor = UNCHECKED_COLOR
                    ),
                onCheckedChange = {},
                enabled = enabled,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedColor)
    }
}

@Composable
private fun CheckboxButtonWithDefaults(
    modifier: Modifier = Modifier,
    checked: Boolean = true,
    enabled: Boolean = true,
    colors: CheckboxButtonColors = CheckboxButtonDefaults.checkboxButtonColors(),
    onCheckedChange: (Boolean) -> Unit = {},
    label: @Composable RowScope.() -> Unit = { Text("Primary") },
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    icon: @Composable (BoxScope.() -> Unit)? = null,
) =
    CheckboxButton(
        modifier = modifier,
        checked = checked,
        enabled = enabled,
        colors = colors,
        onCheckedChange = onCheckedChange,
        label = label,
        secondaryLabel = secondaryLabel,
        icon = icon,
    )

@Composable
private fun SplitCheckboxButtonWithDefaults(
    modifier: Modifier = Modifier,
    checked: Boolean = true,
    enabled: Boolean = true,
    colors: SplitCheckboxButtonColors = CheckboxButtonDefaults.splitCheckboxButtonColors(),
    onCheckedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
    label: @Composable RowScope.() -> Unit = { Text("Primary") },
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
) =
    SplitCheckboxButton(
        modifier = modifier,
        colors = colors,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        label = label,
        secondaryLabel = secondaryLabel,
        onContainerClick = onClick,
        toggleContentDescription = "description",
    )

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeContentTestRule.verifyCheckboxButtonColors(enabled: Boolean, checked: Boolean) {
    val testBackgroundColor = Color.White
    var expectedContainerColor = Color.Transparent
    var expectedLabelColor = Color.Transparent
    var expectedIconColor = Color.Transparent
    var expectedSecondaryLabelColor = Color.Transparent
    var actualLabelColor = Color.Transparent
    var actualIconColor = Color.Transparent
    var actualSecondaryLabelColor = Color.Transparent
    setContentWithTheme {
        expectedContainerColor =
            checkbox_button_container_color(checked, enabled).compositeOver(testBackgroundColor)
        expectedLabelColor = checkbox_button_content_color(checked, enabled)
        expectedSecondaryLabelColor = checkbox_button_secondary_label_color(checked, enabled)
        expectedIconColor = checkbox_button_icon_color(enabled)
        Box(Modifier.fillMaxSize().background(testBackgroundColor)) {
            CheckboxButton(
                modifier = Modifier.testTag(TEST_TAG),
                checked = checked,
                onCheckedChange = {},
                enabled = enabled,
                label = { actualLabelColor = LocalContentColor.current },
                secondaryLabel = { actualSecondaryLabelColor = LocalContentColor.current },
                icon = { actualIconColor = LocalContentColor.current }
            )
        }
    }
    Assert.assertEquals(expectedLabelColor, actualLabelColor)
    Assert.assertEquals(expectedSecondaryLabelColor, actualSecondaryLabelColor)
    Assert.assertEquals(expectedIconColor, actualIconColor)

    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertContainsColor(
            if (expectedContainerColor != Color.Transparent) expectedContainerColor
            else testBackgroundColor,
        )
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeContentTestRule.verifySplitCheckboxButtonColors(
    enabled: Boolean,
    checked: Boolean
) {
    val testBackgroundColor = Color.White
    var expectedContainerColor = Color.Transparent
    var expectedLabelColor = Color.Transparent
    var expectedSecondaryLabelColor = Color.Transparent
    var actualLabelColor = Color.Transparent
    var actualSecondaryLabelColor = Color.Transparent
    setContentWithTheme {
        expectedContainerColor =
            split_checkbox_button_container_color(checked)
                .withDisabledAlphaApplied(enabled = enabled)
                .compositeOver(testBackgroundColor)
        expectedLabelColor =
            split_checkbox_button_content_color(checked).withDisabledAlphaApplied(enabled = enabled)
        expectedSecondaryLabelColor =
            split_checkbox_button_secondary_label_color(checked)
                .withDisabledAlphaApplied(enabled = enabled)
        Box(Modifier.fillMaxSize().background(testBackgroundColor)) {
            SplitCheckboxButton(
                modifier = Modifier.testTag(TEST_TAG),
                checked = checked,
                onCheckedChange = {},
                onContainerClick = {},
                toggleContentDescription = "description",
                enabled = enabled,
                label = { actualLabelColor = LocalContentColor.current },
                secondaryLabel = { actualSecondaryLabelColor = LocalContentColor.current },
            )
        }
    }
    Assert.assertEquals(expectedLabelColor, actualLabelColor)
    Assert.assertEquals(expectedSecondaryLabelColor, actualSecondaryLabelColor)

    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertContainsColor(
            if (expectedContainerColor != Color.Transparent) expectedContainerColor
            else testBackgroundColor,
        )
}

@Composable
private fun checkbox_button_container_color(
    checked: Boolean,
    enabled: Boolean,
): Color {
    return if (checked && enabled) MaterialTheme.colorScheme.primaryContainer
    else if (!checked && enabled) MaterialTheme.colorScheme.surfaceContainer
    else MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.12f)
}

@Composable
private fun checkbox_button_content_color(checked: Boolean, enabled: Boolean): Color {
    return if (checked && enabled) MaterialTheme.colorScheme.onPrimaryContainer
    else if (!checked && enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.38f)
}

@Composable
private fun checkbox_button_secondary_label_color(checked: Boolean, enabled: Boolean): Color {
    return if (checked && enabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    else if (!checked && enabled) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.38f)
}

@Composable
private fun checkbox_button_icon_color(enabled: Boolean): Color {
    return if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.38f)
}

@Composable
private fun split_checkbox_button_container_color(checked: Boolean): Color {
    return if (checked) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainer
}

@Composable
private fun split_checkbox_button_content_color(checked: Boolean): Color {
    return if (checked) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
}

@Composable
private fun split_checkbox_button_secondary_label_color(checked: Boolean): Color {
    return if (checked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun Color.withDisabledAlphaApplied(enabled: Boolean): Color {
    return if (!enabled) toDisabledColor(disabledAlpha = 0.38f) else this
}

private val CHECKED_COLOR = Color(0xFFA020F0)
private val UNCHECKED_COLOR = Color(0xFFFFA500)
