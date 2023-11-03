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

package androidx.tv.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalTvMaterial3Api::class
)
@RunWith(AndroidJUnit4::class)
@LargeTest
class IconButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun filledIconButton_DefaultSize() {
        rule.setContent {
            IconButton(
                modifier = Modifier
                    .testTag(FilledIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(FilledIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(FilledIconButtonTag)
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun filledIconButton_SmallSize() {
        rule.setContent {
            IconButton(
                modifier = Modifier
                    .size(IconButtonDefaults.SmallButtonSize)
                    .testTag(FilledIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(FilledIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(FilledIconButtonTag)
            .assertWidthIsEqualTo(28.dp)
            .assertHeightIsEqualTo(28.dp)
    }

    @Test
    fun filledIconButton_MediumSize() {
        rule.setContent {
            IconButton(
                modifier = Modifier
                    .size(IconButtonDefaults.MediumButtonSize)
                    .testTag(FilledIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(FilledIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(FilledIconButtonTag)
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun filledIconButton_LargeSize() {
        rule.setContent {
            IconButton(
                modifier = Modifier
                    .size(IconButtonDefaults.LargeButtonSize)
                    .testTag(FilledIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(FilledIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(FilledIconButtonTag)
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun filledIconButton_CustomSize() {
        rule.setContent {
            IconButton(
                modifier = Modifier
                    .size(64.dp)
                    .testTag(FilledIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(FilledIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(FilledIconButtonTag)
            .assertWidthIsEqualTo(64.dp)
            .assertHeightIsEqualTo(64.dp)
    }

    @Test
    fun filledIconButton_size_withoutMinimumTouchTarget() {
        val width = 24.dp
        val height = 24.dp
        rule.setContent {
            IconButton(
                modifier = Modifier
                    .testTag(FilledIconButtonTag)
                    .size(width, height),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(FilledIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(FilledIconButtonTag, useUnmergedTree = true)
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun filledIconButton_defaultSemantics() {
        rule.setContent {
            Box {
                IconButton(modifier = Modifier.testTag(FilledIconButtonTag), onClick = {}) {
                    Box(
                        modifier = Modifier
                            .size(IconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }

        rule.onNodeWithTag(FilledIconButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun filledIconButton_longClickSemantics() {
        rule.setContent {
            Box {
                IconButton(
                    modifier = Modifier.testTag(FilledIconButtonTag),
                    onClick = {},
                    onLongClick = {}) {
                    Box(
                        modifier = Modifier
                            .size(IconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }

        rule.onNodeWithTag(FilledIconButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .assertIsEnabled()
    }

    @Test
    fun filledIconButton_disabledSemantics() {
        rule.setContent {
            Box {
                IconButton(
                    modifier = Modifier.testTag(FilledIconButtonTag),
                    onClick = {},
                    enabled = false
                ) {
                    Box(
                        modifier = Modifier
                            .size(IconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }

        rule.onNodeWithTag(FilledIconButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun filledIconButton_findByTagAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                IconButton(
                    modifier = Modifier.testTag(FilledIconButtonTag),
                    onClick = onClick
                ) {
                    Box(
                        modifier = Modifier
                            .size(IconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }
        rule.onNodeWithTag(FilledIconButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun filledIconButton_findByTagAndLongClick() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                IconButton(
                    modifier = Modifier.testTag(FilledIconButtonTag),
                    onClick = {},
                    onLongClick = onLongClick
                ) {
                    Box(
                        modifier = Modifier
                            .size(IconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }
        rule.onNodeWithTag(FilledIconButtonTag)
            .requestFocus()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun filledIconButton_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            Box {
                IconButton(
                    modifier = Modifier.testTag(FilledIconButtonTag),
                    onClick = { enabled = false },
                    enabled = enabled
                ) {
                    Box(
                        modifier = Modifier
                            .size(IconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }
        rule.onNodeWithTag(FilledIconButtonTag)
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun filledIconButton_clickIsIndependentBetweenButtons() {
        var addButtonCounter = 0
        val addButtonOnClick: () -> Unit = { ++addButtonCounter }
        val addButtonTag = "AddButton"

        var phoneButtonCounter = 0
        val phoneButtonOnClick: () -> Unit = { ++phoneButtonCounter }
        val phoneButtonTag = "PhoneButton"

        rule.setContent {
            Column {
                IconButton(
                    modifier = Modifier.testTag(addButtonTag),
                    onClick = addButtonOnClick
                ) {
                    Box(
                        modifier = Modifier
                            .size(IconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
                IconButton(
                    modifier = Modifier.testTag(phoneButtonTag),
                    onClick = phoneButtonOnClick
                ) {
                    Box(
                        modifier = Modifier
                            .size(IconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }

        rule.onNodeWithTag(addButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(addButtonCounter).isEqualTo(1)
            Truth.assertThat(phoneButtonCounter).isEqualTo(0)
        }

        rule.onNodeWithTag(phoneButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(addButtonCounter).isEqualTo(1)
            Truth.assertThat(phoneButtonCounter).isEqualTo(1)
        }
    }

    @Test
    fun filledIconButton_buttonPositioningSmallSize() {
        rule.setContent {
            IconButton(
                modifier = Modifier
                    .size(IconButtonDefaults.SmallButtonSize)
                    .testTag(FilledIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(IconButtonDefaults.SmallIconSize)
                        .testTag(FilledIconButtonIconTag)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        val buttonBounds = rule.onNodeWithTag(FilledIconButtonTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(FilledIconButtonIconTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            6.dp,
            "padding between the start of the button and the start of the icon."
        )

        (iconBounds.top - buttonBounds.top).assertIsEqualTo(
            6.dp,
            "padding between the top of the button and the top of the icon."
        )

        (buttonBounds.right - iconBounds.right).assertIsEqualTo(
            6.dp,
            "padding between the end of the icon and the end of the button."
        )

        (buttonBounds.bottom - iconBounds.bottom).assertIsEqualTo(
            6.dp,
            "padding between the bottom of the button and the bottom of the icon."
        )
    }

    @Test
    fun filledIconButton_buttonPositioningDefaultOrMediumSize() {
        rule.setContent {
            IconButton(
                modifier = Modifier.testTag(FilledIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(IconButtonDefaults.MediumIconSize)
                        .testTag(FilledIconButtonIconTag)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        val buttonBounds = rule.onNodeWithTag(FilledIconButtonTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(FilledIconButtonIconTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            10.dp,
            "padding between the start of the button and the start of the icon."
        )

        (iconBounds.top - buttonBounds.top).assertIsEqualTo(
            10.dp,
            "padding between the top of the button and the top of the icon."
        )

        (buttonBounds.right - iconBounds.right).assertIsEqualTo(
            10.dp,
            "padding between the end of the icon and the end of the button."
        )

        (buttonBounds.bottom - iconBounds.bottom).assertIsEqualTo(
            10.dp,
            "padding between the bottom of the button and the bottom of the icon."
        )
    }

    @Test
    fun filledIconButton_buttonPositioningLargeSize() {
        rule.setContent {
            IconButton(
                modifier = Modifier
                    .size(IconButtonDefaults.LargeButtonSize)
                    .testTag(FilledIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(IconButtonDefaults.LargeIconSize)
                        .testTag(FilledIconButtonIconTag)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        val buttonBounds = rule.onNodeWithTag(FilledIconButtonTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(FilledIconButtonIconTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            14.dp,
            "padding between the start of the button and the start of the icon."
        )

        (iconBounds.top - buttonBounds.top).assertIsEqualTo(
            14.dp,
            "padding between the top of the button and the top of the icon."
        )

        (buttonBounds.right - iconBounds.right).assertIsEqualTo(
            14.dp,
            "padding between the end of the icon and the end of the button."
        )

        (buttonBounds.bottom - iconBounds.bottom).assertIsEqualTo(
            14.dp,
            "padding between the bottom of the button and the bottom of the icon."
        )
    }

    @Test
    fun filledIconButton_defaultSize_materialIconSize_iconPositioning() {
        val diameter = IconButtonDefaults.MediumIconSize
        rule.setContent {
            Box {
                IconButton(onClick = {}) {
                    Box(
                        Modifier
                            .size(diameter)
                            .testTag(FilledIconButtonIconTag)
                    )
                }
            }
        }

        // Icon should be centered inside the FilledIconButton
        rule.onNodeWithTag(FilledIconButtonIconTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(20.dp / 2)
            .assertTopPositionInRootIsEqualTo(20.dp / 2)
    }

    @Test
    fun filledIconButton_defaultSize_customIconSize_iconPositioning() {
        val diameter = 20.dp
        rule.setContent {
            Box {
                IconButton(onClick = {}) {
                    Box(
                        Modifier
                            .size(diameter)
                            .testTag(FilledIconButtonIconTag)
                    )
                }
            }
        }

        // Icon should be centered inside the FilledIconButton
        rule.onNodeWithTag(FilledIconButtonIconTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(10.dp)
            .assertTopPositionInRootIsEqualTo(10.dp)
    }

    @Test
    fun outlinedIconButton_DefaultSize() {
        rule.setContent {
            OutlinedIconButton(
                modifier = Modifier
                    .testTag(OutlinedIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OutlinedIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(OutlinedIconButtonTag)
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun outlinedIconButton_SmallSize() {
        rule.setContent {
            OutlinedIconButton(
                modifier = Modifier
                    .size(OutlinedIconButtonDefaults.SmallButtonSize)
                    .testTag(OutlinedIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OutlinedIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(OutlinedIconButtonTag)
            .assertWidthIsEqualTo(28.dp)
            .assertHeightIsEqualTo(28.dp)
    }

    @Test
    fun outlinedIconButton_MediumSize() {
        rule.setContent {
            OutlinedIconButton(
                modifier = Modifier
                    .size(OutlinedIconButtonDefaults.MediumButtonSize)
                    .testTag(OutlinedIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OutlinedIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(OutlinedIconButtonTag)
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun outlinedIconButton_LargeSize() {
        rule.setContent {
            OutlinedIconButton(
                modifier = Modifier
                    .size(OutlinedIconButtonDefaults.LargeButtonSize)
                    .testTag(OutlinedIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OutlinedIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(OutlinedIconButtonTag)
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun outlinedIconButton_CustomSize() {
        rule.setContent {
            OutlinedIconButton(
                modifier = Modifier
                    .size(64.dp)
                    .testTag(OutlinedIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OutlinedIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(OutlinedIconButtonTag)
            .assertWidthIsEqualTo(64.dp)
            .assertHeightIsEqualTo(64.dp)
    }

    @Test
    fun outlinedIconButton__size_withoutMinimumTouchTarget() {
        val width = 24.dp
        val height = 24.dp
        rule.setContent {
            OutlinedIconButton(
                modifier = Modifier
                    .testTag(OutlinedIconButtonTag)
                    .size(width, height),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OutlinedIconButtonIconSize)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        rule.onNodeWithTag(OutlinedIconButtonTag, useUnmergedTree = true)
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun outlinedIconButton_defaultSemantics() {
        rule.setContent {
            Box {
                OutlinedIconButton(
                    modifier = Modifier.testTag(OutlinedIconButtonTag),
                    onClick = {}
                ) {
                    Box(
                        modifier = Modifier
                            .size(OutlinedIconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }

        rule.onNodeWithTag(OutlinedIconButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun outlinedIconButton_longClickSemantics() {
        rule.setContent {
            Box {
                OutlinedIconButton(
                    modifier = Modifier.testTag(OutlinedIconButtonTag),
                    onClick = {},
                    onLongClick = {}
                ) {
                    Box(
                        modifier = Modifier
                            .size(OutlinedIconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }

        rule.onNodeWithTag(OutlinedIconButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .assertIsEnabled()
    }

    @Test
    fun outlinedIconButton_disabledSemantics() {
        rule.setContent {
            Box {
                OutlinedIconButton(
                    modifier = Modifier.testTag(OutlinedIconButtonTag),
                    onClick = {},
                    enabled = false
                ) {
                    Box(
                        modifier = Modifier
                            .size(OutlinedIconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }

        rule.onNodeWithTag(OutlinedIconButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun outlinedIconButton_findByTagAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                OutlinedIconButton(
                    modifier = Modifier.testTag(OutlinedIconButtonTag),
                    onClick = onClick
                ) {
                    Box(
                        modifier = Modifier
                            .size(OutlinedIconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }
        rule.onNodeWithTag(OutlinedIconButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun outlinedIconButton_findByTagAndLongClick() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                OutlinedIconButton(
                    modifier = Modifier.testTag(OutlinedIconButtonTag),
                    onClick = {},
                    onLongClick = onLongClick
                ) {
                    Box(
                        modifier = Modifier
                            .size(OutlinedIconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }
        rule.onNodeWithTag(OutlinedIconButtonTag)
            .requestFocus()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun outlinedIconButton_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            Box {
                OutlinedIconButton(
                    modifier = Modifier.testTag(OutlinedIconButtonTag),
                    onClick = { enabled = false },
                    enabled = enabled
                ) {
                    Box(
                        modifier = Modifier
                            .size(OutlinedIconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }
        rule.onNodeWithTag(OutlinedIconButtonTag)
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun outlinedIconButton_clickIsIndependentBetweenButtons() {
        var addButtonCounter = 0
        val addButtonOnClick: () -> Unit = { ++addButtonCounter }
        val addButtonTag = "AddButton"

        var phoneButtonCounter = 0
        val phoneButtonOnClick: () -> Unit = { ++phoneButtonCounter }
        val phoneButtonTag = "PhoneButton"

        rule.setContent {
            Column {
                OutlinedIconButton(
                    modifier = Modifier.testTag(addButtonTag),
                    onClick = addButtonOnClick
                ) {
                    Box(
                        modifier = Modifier
                            .size(OutlinedIconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
                OutlinedIconButton(
                    modifier = Modifier.testTag(phoneButtonTag),
                    onClick = phoneButtonOnClick
                ) {
                    Box(
                        modifier = Modifier
                            .size(OutlinedIconButtonDefaults.MediumIconSize)
                            .semantics(mergeDescendants = true) {}
                    )
                }
            }
        }

        rule.onNodeWithTag(addButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(addButtonCounter).isEqualTo(1)
            Truth.assertThat(phoneButtonCounter).isEqualTo(0)
        }

        rule.onNodeWithTag(phoneButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(addButtonCounter).isEqualTo(1)
            Truth.assertThat(phoneButtonCounter).isEqualTo(1)
        }
    }

    @Test
    fun outlinedIconButton_PositioningSmallSize() {
        rule.setContent {
            OutlinedIconButton(
                modifier = Modifier
                    .size(OutlinedIconButtonDefaults.SmallButtonSize)
                    .testTag(OutlinedIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OutlinedIconButtonDefaults.SmallIconSize)
                        .testTag(OutlinedIconButtonIconTag)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        val buttonBounds = rule.onNodeWithTag(OutlinedIconButtonTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(OutlinedIconButtonIconTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            6.dp,
            "padding between the start of the button and the start of the icon."
        )

        (iconBounds.top - buttonBounds.top).assertIsEqualTo(
            6.dp,
            "padding between the top of the button and the top of the icon."
        )

        (buttonBounds.right - iconBounds.right).assertIsEqualTo(
            6.dp,
            "padding between the end of the icon and the end of the button."
        )

        (buttonBounds.bottom - iconBounds.bottom).assertIsEqualTo(
            6.dp,
            "padding between the bottom of the button and the bottom of the icon."
        )
    }

    @Test
    fun outlinedIconButton_PositioningDefaultOrMediumSize() {
        rule.setContent {
            OutlinedIconButton(
                modifier = Modifier.testTag(OutlinedIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OutlinedIconButtonDefaults.MediumIconSize)
                        .testTag(OutlinedIconButtonIconTag)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        val buttonBounds = rule.onNodeWithTag(OutlinedIconButtonTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(OutlinedIconButtonIconTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            10.dp,
            "padding between the start of the button and the start of the icon."
        )

        (iconBounds.top - buttonBounds.top).assertIsEqualTo(
            10.dp,
            "padding between the top of the button and the top of the icon."
        )

        (buttonBounds.right - iconBounds.right).assertIsEqualTo(
            10.dp,
            "padding between the end of the icon and the end of the button."
        )

        (buttonBounds.bottom - iconBounds.bottom).assertIsEqualTo(
            10.dp,
            "padding between the bottom of the button and the bottom of the icon."
        )
    }

    @Test
    fun outlinedIconButton_PositioningLargeSize() {
        rule.setContent {
            OutlinedIconButton(
                modifier = Modifier
                    .size(OutlinedIconButtonDefaults.LargeButtonSize)
                    .testTag(OutlinedIconButtonTag),
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .size(OutlinedIconButtonDefaults.LargeIconSize)
                        .testTag(OutlinedIconButtonIconTag)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        val buttonBounds = rule.onNodeWithTag(OutlinedIconButtonTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(OutlinedIconButtonIconTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            14.dp,
            "padding between the start of the button and the start of the icon."
        )

        (iconBounds.top - buttonBounds.top).assertIsEqualTo(
            14.dp,
            "padding between the top of the button and the top of the icon."
        )

        (buttonBounds.right - iconBounds.right).assertIsEqualTo(
            14.dp,
            "padding between the end of the icon and the end of the button."
        )

        (buttonBounds.bottom - iconBounds.bottom).assertIsEqualTo(
            14.dp,
            "padding between the bottom of the button and the bottom of the icon."
        )
    }

    @Test
    fun outlinedIconButton_defaultSize_materialIconSize_iconPositioning() {
        val diameter = OutlinedIconButtonDefaults.MediumIconSize
        rule.setContent {
            Box {
                OutlinedIconButton(onClick = {}) {
                    Box(
                        Modifier
                            .size(diameter)
                            .testTag(OutlinedIconButtonIconTag)
                    )
                }
            }
        }

        // Icon should be centered inside the OutlinedIconButton
        rule.onNodeWithTag(OutlinedIconButtonIconTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(20.dp / 2)
            .assertTopPositionInRootIsEqualTo(20.dp / 2)
    }

    @Test
    fun outlinedIconButton_defaultSize_customIconSize_iconPositioning() {
        val diameter = 20.dp
        rule.setContent {
            Box {
                OutlinedIconButton(onClick = {}) {
                    Box(
                        Modifier
                            .size(diameter)
                            .testTag(OutlinedIconButtonIconTag)
                    )
                }
            }
        }

        // Icon should be centered inside the OutlinedIconButton
        rule.onNodeWithTag(OutlinedIconButtonIconTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(10.dp)
            .assertTopPositionInRootIsEqualTo(10.dp)
    }
}

private const val FilledIconButtonTag = "FilledIconButton"
private const val FilledIconButtonIconTag = "FilledIconButtonIcon"
private val FilledIconButtonIconSize = 18.0.dp

private const val OutlinedIconButtonTag = "OutlinedIconButton"
private const val OutlinedIconButtonIconTag = "OutlinedIconButtonIcon"
private val OutlinedIconButtonIconSize = 18.0.dp
