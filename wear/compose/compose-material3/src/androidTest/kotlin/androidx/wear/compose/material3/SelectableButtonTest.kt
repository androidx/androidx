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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.samples.SelectableButtonSample
import androidx.wear.compose.material3.samples.SplitSelectableButtonSample
import org.junit.Rule
import org.junit.Test

class SelectableButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun selectable_button_supports_testtag() {
        rule.setContentWithTheme {
            SelectableButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun split_selectable_button_supports_testtag() {
        rule.setContentWithTheme {
            SplitSelectableButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun selectable_button_has_role_radiobutton() {
        rule.setContentWithTheme {
            SelectableButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @Test
    fun selectable_button_samples_build() {
        rule.setContentWithTheme { SelectableButtonSample() }
    }

    @Test
    fun split_selectable_button_samples_build() {
        rule.setContentWithTheme { SplitSelectableButtonSample() }
    }

    @Test
    fun selectable_button_has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            SelectableButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_selectable_button_has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            SplitSelectableButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun selectable_button_has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            SelectableButtonWithDefaults(enabled = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_selectable_button_has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            SplitSelectableButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun selectable_button_is_selectable() {
        rule.setContentWithTheme {
            SelectableButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNode(isSelectable()).assertExists()
    }

    @Test
    fun split_selectable_button_is_selectable() {
        rule.setContentWithTheme {
            SplitSelectableButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNode(isSelectable()).assertExists()
    }

    @Test
    fun split_selectable_button_is_clickable() {
        rule.setContentWithTheme {
            SplitSelectableButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }
        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun selectable_button_is_correctly_enabled() {
        rule.setContentWithTheme {
            SelectableButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun split_selectable_button_is_correctly_enabled() {
        rule.setContentWithTheme {
            SplitSelectableButtonWithDefaults(enabled = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun selectable_button_is_correctly_disabled() {
        rule.setContentWithTheme {
            SelectableButtonWithDefaults(enabled = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun split_selectable_button_is_correctly_disabled() {
        rule.setContentWithTheme {
            SplitSelectableButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertIsNotEnabled()
    }

    @Test
    fun selectable_button_is_correctly_selected() {
        rule.setContentWithTheme {
            SelectableButtonWithDefaults(selected = true, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsSelected()
    }

    @Test
    fun split_selectable_button_is_correctly_selected() {
        rule.setContentWithTheme {
            SplitSelectableButtonWithDefaults(
                selected = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsSelected()
    }

    @Test
    fun selectable_button_is_correctly_unselected() {
        rule.setContentWithTheme {
            SelectableButtonWithDefaults(selected = false, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected()
    }

    @Test
    fun split_selectable_button_is_correctly_unselected() {
        rule.setContentWithTheme {
            SplitSelectableButtonWithDefaults(
                selected = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsNotSelected()
    }

    @Test
    fun selectable_button_responds_to_selection() {
        rule.setContentWithTheme {
            val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
            SelectableButtonWithDefaults(
                selected = selectedIndex == 1,
                onSelected = { onIndexSelected(1) },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected().performClick().assertIsSelected()
    }

    @Test
    fun split_selectable_button_responds_to_selection() {
        rule.setContentWithTheme {
            val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
            SplitSelectableButtonWithDefaults(
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
    fun selectable_button_group_supports_single_selection() {
        val buttonA = "A"
        val buttonB = "B"
        rule.setContentWithTheme {
            Column(modifier = Modifier.selectableGroup()) {
                val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
                SelectableButtonWithDefaults(
                    selected = selectedIndex == 0,
                    onSelected = { onIndexSelected(0) },
                    enabled = true,
                    modifier = Modifier.testTag(buttonA)
                )
                SelectableButtonWithDefaults(
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
                SplitSelectableButtonWithDefaults(
                    selected = selectedIndex == 0,
                    onSelected = { onIndexSelected(0) },
                    enabled = true,
                    modifier = Modifier.testTag(buttonA)
                )
                SplitSelectableButtonWithDefaults(
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
    fun selectable_button_is_not_selected_when_disabled() {
        rule.setContentWithTheme {
            val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
            SelectableButtonWithDefaults(
                selected = selectedIndex == 1,
                onSelected = { onIndexSelected(1) },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected().performClick().assertIsNotSelected()
    }

    @Test
    fun split_selectable_button_is_not_selected_when_disabled() {
        rule.setContentWithTheme {
            val (selectedIndex, onIndexSelected) = remember { mutableStateOf(0) }
            SplitSelectableButtonWithDefaults(
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
            SelectableButtonWithDefaults(
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
            SplitSelectableButtonWithDefaults(modifier = Modifier.testTag(TEST_TAG))
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
            SelectableButtonWithDefaults(
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
            SplitSelectableButtonWithDefaults(
                selected = true,
                onSelected = {},
                label = { Text(text = textContent) }
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @Test
    fun selectable_button_hasAdjustableHeight() {
        val minHeight: Dp = 53.dp

        rule
            .setContentWithThemeForSizeAssertions {
                SelectableButtonWithDefaults(
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
    fun split_selectable_button_hasAdjustableHeight() {
        val minHeight: Dp = 53.dp

        rule
            .setContentWithThemeForSizeAssertions {
                SplitSelectableButtonWithDefaults(
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
    fun selectable_button_height_defaults_52dp() {
        rule
            .setContentWithThemeForSizeAssertions {
                SelectableButtonWithDefaults(secondaryLabel = { Text("Secondary label") })
            }
            .assertHeightIsEqualTo(52.dp)
    }

    @Test
    fun split_selectable_button_height_defaults_52dp() {
        rule
            .setContentWithThemeForSizeAssertions { SplitSelectableButtonWithDefaults() }
            .assertHeightIsEqualTo(52.dp)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun selectable_button_allows_checked_background_color_override() =
        verifyRadioButtonBackgroundColor(
            selected = true,
            enabled = true,
            expectedColor = SELECTED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun selectable_button_allows_unchecked_background_color_override() =
        verifyRadioButtonBackgroundColor(
            selected = false,
            enabled = true,
            expectedColor = UNSELECTED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_selectable_button_allows_checked_background_color_override() =
        verifySplitRadioButtonBackgroundColor(
            selected = true,
            enabled = true,
            expectedColor = SELECTED_COLOR
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_selectable_button_allows_unchecked_background_color_override() =
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
            SelectableButtonWithDefaults(
                selected = selected,
                colors =
                    SelectableButtonDefaults.selectableButtonColors(
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
            SplitSelectableButtonWithDefaults(
                selected = selected,
                colors =
                    SelectableButtonDefaults.splitSelectableButtonColors(
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
private fun SelectableButtonWithDefaults(
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    enabled: Boolean = true,
    colors: SelectableButtonColors = SelectableButtonDefaults.selectableButtonColors(),
    onSelected: () -> Unit = {},
    label: @Composable RowScope.() -> Unit = { Text("Primary") },
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    selectionControl: @Composable SelectionControlScope.() -> Unit = { RadioButton() }
) =
    SelectableButton(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        colors = colors,
        onSelect = onSelected,
        label = label,
        secondaryLabel = secondaryLabel,
        icon = icon,
        selectionControl = selectionControl
    )

@Composable
private fun SplitSelectableButtonWithDefaults(
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    enabled: Boolean = true,
    colors: SplitSelectableButtonColors = SelectableButtonDefaults.splitSelectableButtonColors(),
    onSelected: () -> Unit = {},
    onClick: () -> Unit = {},
    label: @Composable RowScope.() -> Unit = { Text("Primary") },
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    selectionControl: @Composable SelectionControlScope.() -> Unit = { RadioButton() }
) =
    SplitSelectableButton(
        modifier = modifier,
        colors = colors,
        selected = selected,
        enabled = enabled,
        onSelectionClick = onSelected,
        label = label,
        secondaryLabel = secondaryLabel,
        onContainerClick = onClick,
        selectionControl = selectionControl,
    )

private val SELECTED_COLOR = Color(0xFFA020F0)
private val UNSELECTED_COLOR = Color(0xFFFFA500)
