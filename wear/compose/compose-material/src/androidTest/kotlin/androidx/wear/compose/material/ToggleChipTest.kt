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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ToggleChipBehaviourTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            ToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun selection_control_supports_testtag() {
        rule.setContentWithTheme {
            ToggleChip(
                selected = true,
                onSelect = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun split_chip_supports_testtag() {
        rule.setContentWithTheme {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun split_chip_selection_control_supports_testtag() {
        rule.setContentWithTheme {
            SplitToggleChip(
                selected = true,
                onSelect = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }
    @Test
    fun has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            ToggleChip(
                checked = true,
                onCheckedChange = {},
                enabled = true,
                label = { Text("Label") },
                toggleControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_chip_has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                enabled = true,
                label = { Text("Label") },
                toggleControl = { TestImage() },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            ToggleChip(
                checked = true,
                onCheckedChange = {},
                enabled = false,
                label = { Text("Label") },
                toggleControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_chip_has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                enabled = false,
                label = { Text("Label") },
                toggleControl = { TestImage() },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun is_toggleable() {
        rule.setContentWithTheme {
            ToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun split_chip_is_toggleable() {
        rule.setContentWithTheme {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun selection_control_is_selectable() {
        rule.setContentWithTheme {
            ToggleChip(
                selected = true,
                onSelect = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNode(isSelectable()).assertExists()
    }

    @Test
    fun split_chip_selection_control_is_selectable() {
        rule.setContentWithTheme {
            SplitToggleChip(
                selected = true,
                onSelect = {},
                label = { Text("Label") },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNode(isSelectable()).assertExists()
    }

    @Test
    fun split_chip_is_clickable() {
        rule.setContentWithTheme {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled_when_enabled_equals_true() {
        rule.setContentWithTheme {
            ToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun split_chip_is_correctly_enabled_when_enabled_equals_true() {
        rule.setContentWithTheme {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                enabled = true,
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled_when_enabled_equals_false() {
        rule.setContentWithTheme {
            ToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun split_chip_is_correctly_disabled_when_enabled_equals_false() {
        rule.setContentWithTheme {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                enabled = false,
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertIsNotEnabled()
    }

    @Test
    fun is_on_when_checked() {
        rule.setContentWithTheme {
            ToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun split_chip_is_on_when_checked() {
        rule.setContentWithTheme {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOn()
    }

    @Test
    fun is_selected_correctly() {
        rule.setContentWithTheme {
            ToggleChip(
                selected = true,
                onSelect = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsSelected()
    }

    @Test
    fun split_chip_is_selected_correctly() {
        rule.setContentWithTheme {
            SplitToggleChip(
                selected = true,
                onSelect = {},
                label = { Text("Label") },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsSelected()
    }

    @Test
    fun is_off_when_unchecked() {
        rule.setContentWithTheme {
            ToggleChip(
                checked = false,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun split_chip_is_off_when_unchecked() {
        rule.setContentWithTheme {
            SplitToggleChip(
                checked = false,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsOff()
    }

    @Test
    fun is_unselected_correctly() {
        rule.setContentWithTheme {
            ToggleChip(
                selected = false,
                onSelect = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected()
    }

    @Test
    fun split_chip_is_unselected_correctly() {
        rule.setContentWithTheme {
            SplitToggleChip(
                selected = false,
                onSelect = {},
                label = { Text("Label") },
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsNotSelected()
    }

    @Test
    fun responds_to_toggle_on() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            ToggleChip(
                checked = checked,
                onCheckedChange = onCheckedChange,
                label = { Text("Label") },
                toggleControl = { TestImage() },
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
    fun split_chip_responds_to_toggle_on() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            SplitToggleChip(
                checked = checked,
                onCheckedChange = onCheckedChange,
                label = { Text("Label") },
                toggleControl = { TestImage() },
                enabled = true,
                onClick = {},
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
    fun responds_to_selection() {
        rule.setContentWithTheme {
            val (selected, onSelected) = remember { mutableStateOf(false) }
            ToggleChip(
                selected = selected,
                onSelect = onSelected,
                label = { Text("Label") },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsNotSelected()
            .performClick()
            .assertIsSelected()
    }

    @Test
    fun split_chip_responds_to_selection() {
        rule.setContentWithTheme {
            val (selected, onSelected) = remember { mutableStateOf(false) }
            SplitToggleChip(
                selected = selected,
                onSelect = onSelected,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                enabled = true,
                onClick = {},
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
    fun responds_to_toggle_off() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            ToggleChip(
                checked = checked,
                onCheckedChange = onCheckedChange,
                label = { Text("Label") },
                toggleControl = { TestImage() },
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
    fun split_chip_responds_to_toggle_off() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            SplitToggleChip(
                checked = checked,
                onCheckedChange = onCheckedChange,
                label = { Text("Label") },
                toggleControl = { TestImage() },
                enabled = true,
                onClick = {},
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
            ToggleChip(
                checked = checked,
                onCheckedChange = onCheckedChange,
                label = { Text("Label") },
                toggleControl = { TestImage() },
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
    fun split_chip_does_not_toggle_when_disabled() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            SplitToggleChip(
                checked = checked,
                onCheckedChange = onCheckedChange,
                label = { Text("Label") },
                toggleControl = { TestImage() },
                enabled = false,
                onClick = {},
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
    fun does_not_select_when_disabled() {
        rule.setContentWithTheme {
            val (selected, onSelected) = remember { mutableStateOf(false) }
            ToggleChip(
                selected = selected,
                onSelect = onSelected,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsNotSelected()
            .performClick()
            .assertIsNotSelected()
    }

    @Test
    fun split_chip_does_not_select_when_disabled() {
        rule.setContentWithTheme {
            val (selected, onSelected) = remember { mutableStateOf(false) }
            SplitToggleChip(
                selected = selected,
                onSelect = onSelected,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                enabled = false,
                onClick = {},
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
    fun split_chip_clickable_has_role_button() {
        rule.setContentWithTheme {
            SplitToggleChip(
                checked = false,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
                onClick = {},
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
    }

    @Test
    fun displays_label_content() {
        val textContent = "abc"

        rule.setContentWithTheme {
            ToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text(textContent) },
                toggleControl = { TestImage() },
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @Test
    fun split_chip_displays_label_content() {
        val textContent = "abc"

        rule.setContentWithTheme {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text(textContent) },
                onClick = {},
                toggleControl = { TestImage() },
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }
}

class ToggleChipSizeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_base_chip_correct_height() =
        verifyHeight(ChipDefaults.Height)

    @Test
    fun gives_base_chip_has_adjustable_height() {
        val expectedMinHeight = ToggleChipDefaults.Height + 1.dp
        rule.setContentWithThemeForSizeAssertions {
            ToggleChip(
                checked = true,
                onCheckedChange = {},
                label = {
                    Text(
                        text = "ToggleChip text spanning over multiple lines of text " +
                            "to test height is adjustable. This should exceed the minimum height" +
                            " for the ToggleChip."
                    )
                },
                toggleControl = {
                    Checkbox(checked = true)
                }
            )
        }.assertHeightIsAtLeast(expectedMinHeight)
    }

    private fun verifyHeight(expectedHeight: Dp) {
        rule.verifyHeight(expectedHeight) {
            ToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                toggleControl = { TestImage() },
            )
        }
    }
}

class SplitToggleChipSizeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_base_chip_correct_height() =
        verifyHeight(ChipDefaults.Height)

    @Test
    fun gives_base_chip_has_adjustable_height() {
        val expectedMinHeight = ToggleChipDefaults.Height + 1.dp
        rule.setContentWithThemeForSizeAssertions {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                onClick = {},
                label = {
                    Text(
                        text = "SplitToggleChip text spanning over multiple lines of text " +
                            "to test height is adjustable. This should exceed the minimum height " +
                            "for the SplitToggleChip."
                    )
                },
                toggleControl = {
                    Checkbox(checked = true)
                }
            )
        }.assertHeightIsAtLeast(expectedMinHeight)
    }

    private fun verifyHeight(expectedHeight: Dp) {
        rule.verifyHeight(expectedHeight) {
            SplitToggleChip(
                checked = true,
                onCheckedChange = {},
                label = { Text("Label") },
                onClick = {},
                toggleControl = { TestImage() },
            )
        }
    }
}

class ToggleChipColorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_checked_colors() =
        verifyColors(
            ChipStatus.Enabled,
            checked = true,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.secondary }
        )

    @Test
    fun split_chip_gives_checked_colors() =
        verifyColors(
            ChipStatus.Enabled,
            checked = true,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.secondary },
            splitToggleChip = true,
        )

    @Test
    fun gives_unchecked_secondary_colors() =
        verifyColors(
            ChipStatus.Enabled,
            checked = false,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun split_check_gives_unchecked_secondary_colors() =
        verifyColors(
            ChipStatus.Enabled,
            checked = false,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface },
            splitToggleChip = true,
        )

    @Test
    fun gives_checked_disabled_alpha() =
        verifyColors(
            ChipStatus.Disabled,
            checked = true,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.secondary }
        )

    @Test
    fun split_chip_gives_checked_disabled_alpha() =
        verifyColors(
            ChipStatus.Disabled,
            checked = true,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.secondary },
            splitToggleChip = true
        )

    @Test
    fun gives_unchecked_disabled_alpha() =
        verifyColors(
            ChipStatus.Disabled,
            checked = false,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun split_chip_gives_unchecked_disabled_alpha() =
        verifyColors(
            ChipStatus.Disabled,
            checked = false,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface },
            splitToggleChip = true,
        )

    @Test
    fun allows_custom_checked_content_override() {
        val override = Color.Green
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ToggleChip(
                    checked = true,
                    onCheckedChange = {},
                    enabled = true,
                    colors = ToggleChipDefaults.toggleChipColors(checkedContentColor = override),
                    label = { actualContentColor = LocalContentColor.current },
                    toggleControl = {},
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        Assert.assertEquals(override, actualContentColor)
    }

    @Test
    fun split_chip_allows_custom_checked_content_override() {
        val override = Color.Green
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitToggleChip(
                    checked = true,
                    onCheckedChange = {},
                    enabled = true,
                    colors = ToggleChipDefaults.splitToggleChipColors(contentColor = override),
                    label = { actualContentColor = LocalContentColor.current },
                    toggleControl = {},
                    onClick = {},
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        Assert.assertEquals(override, actualContentColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_chip_background_color_correct() {
        var actualBackgrondColor = Color.Transparent

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitToggleChip(
                    checked = true,
                    onCheckedChange = {},
                    enabled = true,
                    colors = ToggleChipDefaults.splitToggleChipColors(),
                    label = {},
                    toggleControl = {},
                    onClick = {},
                    modifier = Modifier.testTag(TEST_TAG).fillMaxWidth()
                )
            }
            actualBackgrondColor = MaterialTheme.colors.surface
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(actualBackgrondColor, 50.0f)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_chip_overridden_background_color_correct() {
        val override = Color.Green

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitToggleChip(
                    checked = true,
                    onCheckedChange = {},
                    enabled = true,
                    colors = ToggleChipDefaults.splitToggleChipColors(backgroundColor = override),
                    label = {},
                    toggleControl = {},
                    onClick = {},
                    modifier = Modifier.testTag(TEST_TAG).fillMaxWidth()
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(override, 50.0f)
    }

    private fun verifyColors(
        status: ChipStatus,
        checked: Boolean,
        labelColor: @Composable () -> Color,
        toggleControlColor: @Composable () -> Color,
        splitToggleChip: Boolean = false,
    ) {
        var expectedLabel = Color.Transparent
        var expectedIcon = Color.Transparent
        var actualLabel = Color.Transparent
        var actualLabelDisabledAlpha = 0f
        var actualIcon = Color.Transparent
        var actualIconDisabledAlpha = 0f

        rule.setContentWithTheme {
            expectedLabel = labelColor()
            expectedIcon = toggleControlColor()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                if (splitToggleChip) {
                    SplitToggleChip(
                        checked = checked,
                        onCheckedChange = {},
                        enabled = status.enabled(),
                        toggleControl = {
                            actualIcon = LocalContentColor.current
                            actualIconDisabledAlpha = ContentAlpha.disabled
                        },
                        label = {
                            actualLabel = LocalContentColor.current
                            actualLabelDisabledAlpha = ContentAlpha.disabled
                        },
                        onClick = {},
                        modifier = Modifier.testTag(TEST_TAG)
                    )
                } else {
                    ToggleChip(
                        checked = checked,
                        onCheckedChange = {},
                        enabled = status.enabled(),
                        toggleControl = {
                            actualIcon = LocalContentColor.current
                            actualIconDisabledAlpha = ContentAlpha.disabled
                        },
                        label = {
                            actualLabel = LocalContentColor.current
                            actualLabelDisabledAlpha = ContentAlpha.disabled
                        },
                        modifier = Modifier.testTag(TEST_TAG)
                    )
                }
            }
        }

        if (status.enabled()) {
            Assert.assertEquals(expectedIcon, actualIcon)
            Assert.assertEquals(expectedLabel, actualLabel)
        } else {
            Assert.assertEquals(expectedIcon.copy(alpha = actualIconDisabledAlpha), actualIcon)
            Assert.assertEquals(expectedLabel.copy(alpha = actualLabelDisabledAlpha), actualLabel)
        }
    }
}

private fun ComposeContentTestRule.verifyHeight(expected: Dp, content: @Composable () -> Unit) {
    setContentWithThemeForSizeAssertions {
        content()
    }
        .assertHeightIsEqualTo(expected)
}
