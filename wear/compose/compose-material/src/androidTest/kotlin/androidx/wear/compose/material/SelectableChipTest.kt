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
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isSelectable
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

class SelectableChipTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            SelectableChip(
                selected = true,
                onClick = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun split_selectable_chip_supports_testtag() {
        rule.setContentWithTheme {
            SplitSelectableChip(
                selected = true,
                onSelectionClick = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
                onContainerClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            SelectableChip(
                selected = true,
                onClick = {},
                enabled = true,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun split_chip_has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            SplitSelectableChip(
                selected = true,
                onSelectionClick = {},
                enabled = true,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                onContainerClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            SelectableChip(
                selected = true,
                onClick = {},
                enabled = false,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun selectable_chip_has_role_radiobutton() {
        rule.setContentWithTheme {
            SelectableChip(
                selected = true,
                onClick = {},
                enabled = false,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @Test
    fun split_chip_has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            SplitSelectableChip(
                selected = true,
                onSelectionClick = {},
                enabled = false,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                onContainerClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun selectable_chip_is_selectable() {
        rule.setContentWithTheme {
            SelectableChip(
                selected = true,
                onClick = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNode(isSelectable()).assertExists()
    }

    @Test
    fun split_chip_is_selectable() {
        rule.setContentWithTheme {
            SplitSelectableChip(
                selected = true,
                onSelectionClick = {},
                label = { Text("Label") },
                onContainerClick = {},
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNode(isSelectable()).assertExists()
    }

    @Test
    fun split_chip_is_clickable() {
        rule.setContentWithTheme {
            SplitSelectableChip(
                selected = true,
                onSelectionClick = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
                onContainerClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            SelectableChip(
                selected = true,
                onClick = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun split_chip_is_correctly_enabled() {
        rule.setContentWithTheme {
            SplitSelectableChip(
                selected = true,
                onSelectionClick = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
                enabled = true,
                onContainerClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            SelectableChip(
                selected = true,
                onClick = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun split_chip_is_correctly_disabled() {
        rule.setContentWithTheme {
            SplitSelectableChip(
                selected = true,
                onSelectionClick = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
                enabled = false,
                onContainerClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertIsNotEnabled()
    }

    @Test
    fun is_selected_correctly() {
        rule.setContentWithTheme {
            SelectableChip(
                selected = true,
                onClick = {},
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
            SplitSelectableChip(
                selected = true,
                onSelectionClick = {},
                label = { Text("Label") },
                onContainerClick = {},
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsSelected()
    }

    @Test
    fun is_unselected_correctly() {
        rule.setContentWithTheme {
            SelectableChip(
                selected = false,
                onClick = {},
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
            SplitSelectableChip(
                selected = false,
                onSelectionClick = {},
                label = { Text("Label") },
                onContainerClick = {},
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(1).assertIsNotSelected()
    }

    @Test
    fun responds_to_selection() {
        rule.setContentWithTheme {
            val (selected, onSelected) = remember { mutableStateOf(false) }
            SelectableChip(
                selected = selected,
                onClick = onSelected,
                label = { Text("Label") },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
                selectionControl = { TestImage() },
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected().performClick().assertIsSelected()
    }

    @Test
    fun split_chip_responds_to_selection() {
        rule.setContentWithTheme {
            val (selected, onSelected) = remember { mutableStateOf(false) }
            SplitSelectableChip(
                selected = selected,
                onSelectionClick = onSelected,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                enabled = true,
                onContainerClick = {},
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
    fun does_not_select_when_disabled() {
        rule.setContentWithTheme {
            val (selected, onSelected) = remember { mutableStateOf(false) }
            SelectableChip(
                selected = selected,
                onClick = onSelected,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotSelected().performClick().assertIsNotSelected()
    }

    @Test
    fun split_chip_does_not_select_when_disabled() {
        rule.setContentWithTheme {
            val (selected, onSelected) = remember { mutableStateOf(false) }
            SplitSelectableChip(
                selected = selected,
                onSelectionClick = onSelected,
                label = { Text("Label") },
                selectionControl = { TestImage() },
                enabled = false,
                onContainerClick = {},
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
            SplitSelectableChip(
                selected = false,
                onSelectionClick = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
                onContainerClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .onChildAt(0)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun displays_label_content() {
        val textContent = "abc"

        rule.setContentWithTheme {
            SelectableChip(
                selected = true,
                onClick = {},
                label = { Text(textContent) },
                selectionControl = { TestImage() },
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @Test
    fun split_chip_displays_label_content() {
        val textContent = "abc"

        rule.setContentWithTheme {
            SplitSelectableChip(
                selected = true,
                onSelectionClick = {},
                label = { Text(textContent) },
                onContainerClick = {},
                selectionControl = { TestImage() },
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }

    @Test fun gives_chip_correct_height() = verifyChipHeight(ChipDefaults.Height)

    @Test
    fun gives_chip_adjustable_height() {
        val expectedMinHeight = SelectableChipDefaults.Height + 1.dp
        rule
            .setContentWithThemeForSizeAssertions {
                SelectableChip(
                    selected = true,
                    onClick = {},
                    label = {
                        Text(
                            text =
                                "SelectableChip text spanning over multiple lines of text " +
                                    "to test height is adjustable. This should exceed the minimum height" +
                                    " for the SelectableChip."
                        )
                    },
                    selectionControl = { RadioButton(selected = true) }
                )
            }
            .assertHeightIsAtLeast(expectedMinHeight)
    }

    private fun verifyChipHeight(expectedHeight: Dp) {
        rule.verifyHeight(expectedHeight) {
            SelectableChip(
                selected = true,
                onClick = {},
                label = { Text("Label") },
                selectionControl = { TestImage() },
            )
        }
    }

    @Test fun gives_split_chip_correct_height() = verifySplitChipHeight(ChipDefaults.Height)

    @Test
    fun gives_split_chip_adjustable_height() {
        val expectedMinHeight = SelectableChipDefaults.Height + 1.dp
        rule
            .setContentWithThemeForSizeAssertions {
                SplitSelectableChip(
                    selected = true,
                    onSelectionClick = {},
                    onContainerClick = {},
                    label = {
                        Text(
                            text =
                                "SplitSelectableChip text spanning over multiple lines of text " +
                                    "to test height is adjustable. This should exceed the minimum height " +
                                    "for the SplitSelectableChip."
                        )
                    },
                    selectionControl = { RadioButton(selected = true) }
                )
            }
            .assertHeightIsAtLeast(expectedMinHeight)
    }

    private fun verifySplitChipHeight(expectedHeight: Dp) {
        rule.verifyHeight(expectedHeight) {
            SplitSelectableChip(
                selected = true,
                onSelectionClick = {},
                label = { Text("Label") },
                onContainerClick = {},
                selectionControl = { TestImage() },
            )
        }
    }

    @Test
    fun gives_selected_colors() =
        verifyColors(
            ChipStatus.Enabled,
            selected = true,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.secondary }
        )

    @Test
    fun split_chip_gives_selected_colors() =
        verifyColors(
            ChipStatus.Enabled,
            selected = true,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.secondary },
            splitSelectableChip = true,
        )

    @Test
    fun gives_unselected_secondary_colors() =
        verifyColors(
            ChipStatus.Enabled,
            selected = false,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun split_chip_gives_unselected_secondary_colors() =
        verifyColors(
            ChipStatus.Enabled,
            selected = false,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface },
            splitSelectableChip = true,
        )

    @Test
    fun gives_selected_disabled_alpha() =
        verifyColors(
            ChipStatus.Disabled,
            selected = true,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.secondary }
        )

    @Test
    fun split_chip_gives_selected_disabled_alpha() =
        verifyColors(
            ChipStatus.Disabled,
            selected = true,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.secondary },
            splitSelectableChip = true
        )

    @Test
    fun gives_unselected_disabled_alpha() =
        verifyColors(
            ChipStatus.Disabled,
            selected = false,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun split_chip_gives_unselected_disabled_alpha() =
        verifyColors(
            ChipStatus.Disabled,
            selected = false,
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface },
            splitSelectableChip = true,
        )

    @Test
    fun allows_custom_selected_content_override() {
        val override = Color.Green
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                SelectableChip(
                    selected = true,
                    onClick = {},
                    enabled = true,
                    colors =
                        SelectableChipDefaults.selectableChipColors(
                            selectedContentColor = override
                        ),
                    label = { actualContentColor = LocalContentColor.current },
                    selectionControl = {},
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        Assert.assertEquals(override, actualContentColor)
    }

    @Test
    fun split_chip_allows_custom_selected_content_override() {
        val override = Color.Green
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitSelectableChip(
                    selected = true,
                    onSelectionClick = {},
                    enabled = true,
                    colors =
                        SelectableChipDefaults.splitSelectableChipColors(contentColor = override),
                    label = { actualContentColor = LocalContentColor.current },
                    selectionControl = {},
                    onContainerClick = {},
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
                SplitSelectableChip(
                    selected = true,
                    onSelectionClick = {},
                    enabled = true,
                    colors = SelectableChipDefaults.splitSelectableChipColors(),
                    label = {},
                    selectionControl = {},
                    onContainerClick = {},
                    modifier = Modifier.testTag(TEST_TAG).fillMaxWidth()
                )
            }
            actualBackgrondColor = MaterialTheme.colors.surface
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(actualBackgrondColor, 50.0f)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun split_chip_overridden_background_color_correct() {
        val override = Color.Green

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitSelectableChip(
                    selected = true,
                    onSelectionClick = {},
                    enabled = true,
                    colors =
                        SelectableChipDefaults.splitSelectableChipColors(
                            backgroundColor = override
                        ),
                    label = {},
                    selectionControl = {},
                    onContainerClick = {},
                    modifier = Modifier.testTag(TEST_TAG).fillMaxWidth()
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(override, 50.0f)
    }

    private fun verifyColors(
        status: ChipStatus,
        selected: Boolean,
        labelColor: @Composable () -> Color,
        selectionControlColor: @Composable () -> Color,
        splitSelectableChip: Boolean = false,
    ) {
        var expectedLabel = Color.Transparent
        var expectedIcon = Color.Transparent
        var actualLabel = Color.Transparent
        var actualLabelDisabledAlpha = 0f
        var actualIcon = Color.Transparent
        var actualIconDisabledAlpha = 0f

        rule.setContentWithTheme {
            expectedLabel = labelColor()
            expectedIcon = selectionControlColor()
            Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                if (splitSelectableChip) {
                    SplitSelectableChip(
                        selected = selected,
                        onSelectionClick = {},
                        enabled = status.enabled(),
                        selectionControl = {
                            actualIcon = LocalContentColor.current
                            actualIconDisabledAlpha = ContentAlpha.disabled
                        },
                        label = {
                            actualLabel = LocalContentColor.current
                            actualLabelDisabledAlpha = ContentAlpha.disabled
                        },
                        onContainerClick = {},
                        modifier = Modifier.testTag(TEST_TAG)
                    )
                } else {
                    SelectableChip(
                        selected = selected,
                        onClick = {},
                        enabled = status.enabled(),
                        selectionControl = {
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
    setContentWithThemeForSizeAssertions { content() }.assertHeightIsEqualTo(expected)
}
