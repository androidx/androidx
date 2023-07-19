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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
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
        rule.setContent {
            ToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun split_button_supports_testtag() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_clickaction_when_enabled() {
        rule.setContent {
            ToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_button_has_clickaction_when_enabled() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled() {
        rule.setContent {
            ToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_button_has_clickaction_when_disabled() {
        rule.setContent {
            SplitToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun is_toggleable() {
        rule.setContent {
            ToggleButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
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
    fun is_correctly_enabled() {
        rule.setContent {
            ToggleButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
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
    fun is_correctly_disabled() {
        rule.setContent {
            ToggleButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
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
    fun is_on_when_checked() {
        rule.setContent {
            ToggleButtonWithDefaults(
                checked = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
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
    fun is_off_when_unchecked() {
        rule.setContent {
            ToggleButtonWithDefaults(
                checked = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
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
    fun responds_to_toggle_on() {
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
    fun responds_to_toggle_off() {
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
    fun does_not_toggle_when_disabled() {
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
    fun has_role_checkbox() {
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
    fun displays_label_content() {
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
