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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertContainsColor
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
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.samples.RadioButtonSample
import androidx.wear.compose.material3.samples.SplitRadioButtonSample
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class RadioButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun radio_button_supports_testtag() {
        rule.setContentWithTheme { RadioButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun split_radio_button_supports_testtag() {
        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun radio_button_has_role_radiobutton() {
        rule.setContentWithTheme { RadioButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG)) }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @Test
    fun radio_button_samples_build() {
        rule.setContentWithTheme { RadioButtonSample() }
    }

    @Test
    fun split_radio_button_samples_build() {
        rule.setContentWithTheme { SplitRadioButtonSample() }
    }

    @Test
    fun radio_button_has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            RadioButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_radio_button_has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun radio_button_has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            RadioButtonWithDefaults(enabled = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_radio_button_has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(enabled = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun radio_button_is_selectable() {
        rule.setContentWithTheme { RadioButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG)) }

        rule.onNode(isSelectable()).assertExists()
    }

    @Test
    fun split_radio_button_is_selectable() {
        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNode(isSelectable()).assertExists()
    }

    @Test
    fun split_radio_button_is_clickable() {
        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }
        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun radio_button_is_correctly_enabled() {
        rule.setContentWithTheme {
            RadioButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun split_radio_button_is_correctly_enabled() {
        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun radio_button_is_correctly_disabled() {
        rule.setContentWithTheme {
            RadioButtonWithDefaults(enabled = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun split_radio_button_is_correctly_disabled() {
        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(enabled = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertIsNotEnabled()
    }

    @Test
    fun radio_button_is_correctly_selected() {
        rule.setContentWithTheme {
            RadioButtonWithDefaults(selected = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsSelected()
    }

    @Test
    fun split_radio_button_is_correctly_selected() {
        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(selected = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsSelected()
    }

    @Test
    fun radio_button_is_correctly_unselected() {
        rule.setContentWithTheme {
            RadioButtonWithDefaults(selected = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected()
    }

    @Test
    fun split_radio_button_is_correctly_unselected() {
        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(selected = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsNotSelected()
    }

    @Test
    fun radio_button_responds_to_selection() {
        rule.setContentWithTheme {
            val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
            RadioButtonWithDefaults(
                selected = selectedIndex == 1,
                onSelected = { onIndexSelected(1) },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected().performClick().assertIsSelected()
    }

    @Test
    fun split_radio_button_responds_to_selection() {
        rule.setContentWithTheme {
            val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
            SplitRadioButtonWithDefaults(
                selected = selectedIndex == 1,
                onSelected = { onIndexSelected(1) },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .onChildAt(1)
            .assertIsNotSelected()
            .performClick()
            .assertIsSelected()
    }

    @Test
    fun radio_button_group_supports_single_selection() {
        val buttonA = "A"
        val buttonB = "B"
        rule.setContentWithTheme {
            Column(modifier = Modifier.selectableGroup()) {
                val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
                RadioButtonWithDefaults(
                    selected = selectedIndex == 0,
                    onSelected = { onIndexSelected(0) },
                    enabled = true,
                    modifier = Modifier.testTag(buttonA)
                )
                RadioButtonWithDefaults(
                    selected = selectedIndex == 1,
                    onSelected = { onIndexSelected(1) },
                    enabled = true,
                    modifier = Modifier.testTag(buttonB)
                )
            }
        }

        rule.onNodeWithTag(buttonA).assertIsSelected()
        rule.onNodeWithTag(buttonB).assertIsNotSelected()
        rule.onNodeWithTag(buttonB).performClick()
        rule.onNodeWithTag(buttonA).assertIsNotSelected()
        rule.onNodeWithTag(buttonB).assertIsSelected()
    }

    @Test
    fun split_button_supports_single_selection() {
        val buttonA = "A"
        val buttonB = "B"
        rule.setContentWithTheme {
            Column(modifier = Modifier.selectableGroup()) {
                val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
                SplitRadioButtonWithDefaults(
                    selected = selectedIndex == 0,
                    onSelected = { onIndexSelected(0) },
                    enabled = true,
                    modifier = Modifier.testTag(buttonA)
                )
                SplitRadioButtonWithDefaults(
                    selected = selectedIndex == 1,
                    onSelected = { onIndexSelected(1) },
                    enabled = true,
                    modifier = Modifier.testTag(buttonB)
                )
            }
        }

        rule.onNodeWithTag(buttonA).onChildAt(1).assertIsSelected()
        rule.onNodeWithTag(buttonB).onChildAt(1).assertIsNotSelected()
        rule.onNodeWithTag(buttonB).onChildAt(1).performClick()
        rule.onNodeWithTag(buttonA).onChildAt(1).assertIsNotSelected()
        rule.onNodeWithTag(buttonB).onChildAt(1).assertIsSelected()
    }

    @Test
    fun radio_button_is_not_selected_when_disabled() {
        rule.setContentWithTheme {
            val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
            RadioButtonWithDefaults(
                selected = selectedIndex == 1,
                onSelected = { onIndexSelected(1) },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected().performClick().assertIsNotSelected()
    }

    @Test
    fun split_radio_button_is_not_selected_when_disabled() {
        rule.setContentWithTheme {
            val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
            SplitRadioButtonWithDefaults(
                selected = selectedIndex == 1,
                onSelected = { onIndexSelected(1) },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .onChildAt(1)
            .assertIsNotSelected()
            .performClick()
            .assertIsNotSelected()
    }

    @Test
    fun can_override_role() {
        rule.setContentWithTheme {
            RadioButtonWithDefaults(
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
            SplitRadioButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        // NB The toggle control (Checkbox or Switch) provides its own role,
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
            RadioButtonWithDefaults(
                selected = true,
                onSelected = {},
                label = { Text(text = textContent) }
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @Test
    fun split_button_displays_label_content() {
        val textContent = "abc"

        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(
                selected = true,
                onSelected = {},
                label = { Text(text = textContent) }
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @Test
    fun radio_button_hasAdjustableHeight() {
        val minHeight: Dp = 53.dp

        rule
            .setContentWithThemeForSizeAssertions {
                RadioButtonWithDefaults(
                    label = {
                        Text(
                            text =
                                "RadioButton text spanning over multiple lines of text " +
                                    "to test height is adjustable. This should exceed the minimum height" +
                                    " for the RadioButton."
                        )
                    },
                    secondaryLabel = { Text(text = "Secondary label with text.") }
                )
            }
            .assertHeightIsAtLeast(minHeight)
    }

    @Test
    fun split_radio_button_hasAdjustableHeight() {
        val minHeight: Dp = 53.dp

        rule
            .setContentWithThemeForSizeAssertions {
                SplitRadioButtonWithDefaults(
                    label = { Text(text = "Primary label with 3 lines of text.") },
                    secondaryLabel = {
                        Text(
                            text =
                                "SplitRadioButton text spanning over multiple lines of text " +
                                    "to test height is adjustable. This should exceed the minimum height" +
                                    " for the SplitRadioButton."
                        )
                    }
                )
            }
            .assertHeightIsAtLeast(minHeight)
    }

    @Test
    fun radio_button_height_defaults_52dp() {
        rule
            .setContentWithThemeForSizeAssertions {
                RadioButtonWithDefaults(secondaryLabel = { Text("Secondary label") })
            }
            .assertHeightIsEqualTo(52.dp)
    }

    @Test
    fun split_radio_button_height_defaults_52dp() {
        rule
            .setContentWithThemeForSizeAssertions { SplitRadioButtonWithDefaults() }
            .assertHeightIsEqualTo(52.dp)
    }

    @Test
    fun radio_defines_default_textalign() {
        var labelTextAlign: TextAlign? = null
        var secondaryLabelTextAlign: TextAlign? = null

        rule.setContentWithTheme {
            RadioButtonWithDefaults(
                selected = true,
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
    fun splitradio_defines_default_textalign() {
        var labelTextAlign: TextAlign? = null
        var secondaryLabelTextAlign: TextAlign? = null

        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(
                selected = true,
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
    fun radio_defines_default_overflow() {
        var labelOverflow: TextOverflow? = null
        var secondaryLabelOverflow: TextOverflow? = null

        rule.setContentWithTheme {
            RadioButtonWithDefaults(
                selected = true,
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
    fun splitradio_defines_default_overflow() {
        var labelOverflow: TextOverflow? = null
        var secondaryLabelOverflow: TextOverflow? = null

        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(
                selected = true,
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
    fun radio_defines_default_maxlines() {
        var labelMaxLines: Int? = null
        var secondaryLabelMaxLines: Int? = null

        rule.setContentWithTheme {
            RadioButtonWithDefaults(
                selected = true,
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
    fun splitradio_defines_default_maxlines() {
        var labelMaxLines: Int? = null
        var secondaryLabelMaxLines: Int? = null

        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(
                selected = true,
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
    fun radio_button_allows_checked_background_color_override() =
        verifyRadioButtonBackgroundColor(
            selected = true,
            enabled = true,
            expectedColor = SELECTED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun radio_button_allows_unchecked_background_color_override() =
        verifyRadioButtonBackgroundColor(
            selected = false,
            enabled = true,
            expectedColor = UNSELECTED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_radio_button_allows_checked_background_color_override() =
        verifySplitRadioButtonBackgroundColor(
            selected = true,
            enabled = true,
            expectedColor = SELECTED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_radio_button_allows_unchecked_background_color_override() =
        verifySplitRadioButtonBackgroundColor(
            selected = false,
            enabled = true,
            expectedColor = UNSELECTED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyRadioButtonBackgroundColor(
        selected: Boolean,
        enabled: Boolean,
        expectedColor: Color
    ) {
        rule.setContentWithTheme {
            RadioButtonWithDefaults(
                selected = selected,
                colors =
                    RadioButtonDefaults.radioButtonColors(
                        selectedContainerColor = SELECTED_COLOR,
                        unselectedContainerColor = UNSELECTED_COLOR
                    ),
                onSelected = {},
                enabled = enabled,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifySplitRadioButtonBackgroundColor(
        selected: Boolean,
        enabled: Boolean,
        expectedColor: Color
    ) {
        rule.setContentWithTheme {
            SplitRadioButtonWithDefaults(
                selected = selected,
                colors =
                    RadioButtonDefaults.splitRadioButtonColors(
                        selectedContainerColor = SELECTED_COLOR,
                        unselectedContainerColor = UNSELECTED_COLOR
                    ),
                onSelected = {},
                enabled = enabled,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedColor)
    }
}

@Composable
private fun RadioButtonWithDefaults(
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.radioButtonColors(),
    onSelected: () -> Unit = {},
    label: @Composable RowScope.() -> Unit = { Text("Primary") },
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    icon: @Composable (BoxScope.() -> Unit)? = null,
) =
    RadioButton(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        colors = colors,
        onSelect = onSelected,
        label = label,
        secondaryLabel = secondaryLabel,
        icon = icon,
    )

@Composable
private fun SplitRadioButtonWithDefaults(
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    enabled: Boolean = true,
    colors: SplitRadioButtonColors = RadioButtonDefaults.splitRadioButtonColors(),
    onSelected: () -> Unit = {},
    onClick: () -> Unit = {},
    label: @Composable RowScope.() -> Unit = { Text("Primary") },
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
) =
    SplitRadioButton(
        modifier = modifier,
        colors = colors,
        selected = selected,
        enabled = enabled,
        onSelectionClick = onSelected,
        label = label,
        secondaryLabel = secondaryLabel,
        onContainerClick = onClick,
        selectionContentDescription = null,
    )

private val SELECTED_COLOR = Color(0xFFA020F0)
private val UNSELECTED_COLOR = Color(0xFFFFA500)
