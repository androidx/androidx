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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ToggleButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun split_button_supports_testtag() {
        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_button_has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_button_has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun is_toggleable() {
        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun split_button_is_toggleable() {
        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun split_button_is_clickable() {
        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun split_button_is_correctly_enabled() {
        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun split_button_is_correctly_disabled() {
        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertIsNotEnabled()
    }

    @Test
    fun is_on_when_checked() {
        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun split_button_is_on_when_checked() {
        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOn()
    }

    @Test
    fun is_off_when_unchecked() {
        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                checked = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun split_button_is_off_when_unchecked() {
        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                checked = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOff()
    }

    @Test
    fun responds_to_toggle_on() {
        rule.setContentWithTheme {
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
    fun split_button_responds_to_toggle_on() {
        rule.setContentWithTheme {
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
    fun responds_to_toggle_off() {
        rule.setContentWithTheme {
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
    fun split_button_responds_to_toggle_off() {
        rule.setContentWithTheme {
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
    fun does_not_toggle_when_disabled() {
        rule.setContentWithTheme {
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
    fun split_button_does_not_toggle_when_disabled() {
        rule.setContentWithTheme {
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
    fun has_role_checkbox() {
        rule.setContentWithTheme {
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
    fun can_override_role() {
        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .semantics {
                        role = Role.Button
                    }
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
    }

    @Test
    fun split_button_has_roles_button_and_checkbox() {
        rule.setContentWithTheme {
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
    fun displays_label_content() {
        val textContent = "abc"

        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = {
                    Text(text = textContent)
                }
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @Test
    fun split_button_displays_label_content() {
        val textContent = "abc"

        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                checked = true,
                onCheckedChange = {},
                label = {
                    Text(text = textContent)
                }
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @Test
    fun toggle_button_hasAdjustableHeight() {
        val minHeight: Dp = 53.dp

        rule.setContentWithThemeForSizeAssertions {
            ToggleButtonWithDefaults(
                label = {
                    Text(
                        text = "ToggleButton text spanning over multiple lines of text " +
                            "to test height is adjustable. This should exceed the minimum height" +
                            " for the ToggleButton."
                    )
                },
                secondaryLabel = {
                    Text(
                        text = "Secondary label with text."
                    )
                }
            )
        }.assertHeightIsAtLeast(minHeight)
    }

    @Test
    fun split_toggle_button_hasAdjustableHeight() {
        val minHeight: Dp = 53.dp

        rule.setContentWithThemeForSizeAssertions {
            ToggleButtonWithDefaults(
                label = {
                    Text(
                        text = "Primary label with 3 lines of text."
                    )
                },
                secondaryLabel = {
                    Text(
                        text = "SplitToggleButton text spanning over multiple lines of text " +
                            "to test height is adjustable. This should exceed the minimum height" +
                            " for the SplitToggleButton."
                    )
                }
            )
        }.assertHeightIsAtLeast(minHeight)
    }

    @Test
    fun toggle_button_height_defaults_52dp() {
        rule.setContentWithThemeForSizeAssertions {
            ToggleButtonWithDefaults()
        }.assertHeightIsEqualTo(52.dp)
    }

    @Test
    fun split_toggle_button_height_defaults_52dp() {
        rule.setContentWithThemeForSizeAssertions {
            SplitToggleButtonWithDefaults()
        }.assertHeightIsEqualTo(52.dp)
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
    private fun verifyToggleButtonBackgroundColor(
        checked: Boolean,
        enabled: Boolean,
        expectedColor: Color
    ) {
        rule.setContentWithTheme {
            ToggleButtonWithDefaults(
                checked = checked,
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedContainerColor = CHECKED_COLOR,
                    uncheckedContainerColor = UNCHECKED_COLOR
                ),
                onCheckedChange = {},
                enabled = enabled,
                modifier = Modifier.testTag(TEST_TAG),
            )
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
        rule.setContentWithTheme {
            SplitToggleButtonWithDefaults(
                checked = checked,
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedContainerColor = CHECKED_COLOR,
                    uncheckedContainerColor = UNCHECKED_COLOR
                ),
                onCheckedChange = {},
                enabled = enabled,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedColor)
    }
}

@Composable
private fun ToggleButtonWithDefaults(
    modifier: Modifier = Modifier,
    checked: Boolean = true,
    enabled: Boolean = true,
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    onCheckedChange: (Boolean) -> Unit = {},
    label: @Composable RowScope.() -> Unit = {
        Text("Primary")
    },
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    selectionControl: @Composable () -> Unit = {}
) =
    ToggleButton(
        modifier = modifier,
        checked = checked,
        enabled = enabled,
        colors = colors,
        onCheckedChange = onCheckedChange,
        label = label,
        secondaryLabel = secondaryLabel,
        icon = icon,
        selectionControl = selectionControl
    )

@Composable
private fun SplitToggleButtonWithDefaults(
    modifier: Modifier = Modifier,
    checked: Boolean = true,
    enabled: Boolean = true,
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    onCheckedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
    label: @Composable RowScope.() -> Unit = {
        Text("Primary")
    },
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    selectionControl: @Composable BoxScope.() -> Unit = {}
) = SplitToggleButton(
    modifier = modifier,
    colors = colors,
    checked = checked,
    enabled = enabled,
    onCheckedChange = onCheckedChange,
    label = label,
    secondaryLabel = secondaryLabel,
    onClick = onClick,
    selectionControl = selectionControl,
)

private val CHECKED_COLOR = Color(0xFFA020F0)
private val UNCHECKED_COLOR = Color(0xFFFFA500)
