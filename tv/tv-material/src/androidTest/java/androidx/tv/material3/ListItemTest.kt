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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
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
@LargeTest
@RunWith(AndroidJUnit4::class)
class ListItemTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun listItem_findByTagAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                ListItem(
                    modifier = Modifier.testTag(ListItemTag),
                    headlineContent = { Text(text = "Test Text") },
                    onClick = onClick,
                    selected = false
                )
            }
        }

        rule.onNodeWithTag(ListItemTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun listItem_clickIsIndependentBetweenItems() {
        var openItemClickCounter = 0
        val openItemOnClick: () -> Unit = { ++openItemClickCounter }
        val openItemTag = "OpenItem"

        var closeItemClickCounter = 0
        val closeItemOnClick: () -> Unit = { ++closeItemClickCounter }
        val closeItemTag = "CloseItem"

        rule.setContent {
            Column {
                ListItem(
                    modifier = Modifier.testTag(openItemTag),
                    headlineContent = { Text(text = "Test Text") },
                    onClick = openItemOnClick,
                    selected = false
                )
                ListItem(
                    modifier = Modifier.testTag(closeItemTag),
                    headlineContent = { Text(text = "Test Text") },
                    onClick = closeItemOnClick,
                    selected = false
                )
            }
        }

        rule.onNodeWithTag(openItemTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(openItemClickCounter).isEqualTo(1)
            Truth.assertThat(closeItemClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag(closeItemTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(openItemClickCounter).isEqualTo(1)
            Truth.assertThat(closeItemClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun listItem_longClickAction() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                ListItem(
                    modifier = Modifier.testTag(ListItemTag),
                    headlineContent = { Text(text = "Test Text") },
                    onClick = { },
                    onLongClick = onLongClick,
                    selected = false
                )
            }
        }

        rule.onNodeWithTag(ListItemTag)
            .requestFocus()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }

        rule.onNodeWithTag(ListItemTag)
            .requestFocus()
            .performLongKeyPress(rule, Key.DirectionCenter, count = 2)
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(3)
        }
    }

    @Test
    fun listItem_findByTagAndStateChangeCheck() {
        var checkedState by mutableStateOf(true)
        val onClick: () -> Unit = { checkedState = !checkedState }

        rule.setContent {
            ListItem(
                modifier = Modifier.testTag(ListItemTag),
                headlineContent = { Text(text = "Test Text") },
                onClick = onClick,
                selected = checkedState
            )
        }

        rule.onNodeWithTag(ListItemTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(!checkedState)
        }
    }

    @Test
    fun listItem_contentPaddingHorizontal() {
        rule.setContent {
            ListItem(
                modifier = Modifier.testTag(ListItemTag),
                headlineContent = {
                    Text(
                        text = "ListItemText",
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {}
                    )
                },
                onClick = {},
                selected = false
            )
        }

        val itemBounds = rule.onNodeWithTag(ListItemTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithText("ListItemText").getUnclippedBoundsInRoot()

        (textBounds.left - itemBounds.left).assertIsEqualTo(
            16.dp,
            "padding between the start of the list item and the start of the text."
        )

        (itemBounds.right - textBounds.right).assertIsEqualTo(
            16.dp,
            "padding between the end of the text and the end of the list item."
        )
    }

    @Test
    fun listItem_contentPaddingVertical() {
        rule.setContent {
            ListItem(
                modifier = Modifier.testTag(ListItemTag),
                headlineContent = {
                    Text(
                        text = "Test Text",
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag(ListItemTextTag)
                            .semantics(mergeDescendants = true) {}
                    )
                },
                onClick = {},
                selected = false
            )
        }

        val itemBounds = rule.onNodeWithTag(ListItemTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(ListItemTextTag).getUnclippedBoundsInRoot()

        (textBounds.top - itemBounds.top).assertIsEqualTo(
            12.dp,
            "padding between the top of the list item and the top of the text."
        )

        (itemBounds.bottom - textBounds.bottom).assertIsEqualTo(
            12.dp,
            "padding between the bottom of the text and the bottom of the list item."
        )
    }

    @Test
    fun listItem_leadingContentPadding() {
        val testIconTag = "IconTag"

        rule.setContent {
            ListItem(
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(ListItemDefaults.IconSize)
                            .testTag(testIconTag)
                            .semantics(mergeDescendants = true) {}
                    )
                },
                modifier = Modifier.testTag(ListItemTag),
                headlineContent = {
                    Text(
                        text = "Test Text",
                        modifier = Modifier
                            .testTag(ListItemTextTag)
                            .semantics(mergeDescendants = true) {}
                    )
                },
                onClick = {},
                selected = false
            )
        }

        val itemBounds = rule.onNodeWithTag(ListItemTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(ListItemTextTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(testIconTag).getUnclippedBoundsInRoot()

        (iconBounds.top - itemBounds.top).assertIsEqualTo(
            12.dp,
            "padding between the top of the list item and the top of the icon."
        )

        (itemBounds.bottom - iconBounds.bottom).assertIsEqualTo(
            12.dp,
            "padding between the bottom of the icon and the bottom of the list item."
        )

        (iconBounds.left - itemBounds.left).assertIsEqualTo(
            16.dp,
            "padding between the start of the icon and the start of the list item."
        )

        (textBounds.left - iconBounds.right).assertIsEqualTo(
            8.dp,
            "padding between the end of the icon and the start of the text."
        )
    }

    @Test
    fun listItem_trailingContentPadding() {
        val testTrailingContentTag = "TrailingIconTag"

        rule.setContent {
            ListItem(
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(ListItemDefaults.IconSize)
                            .testTag(testTrailingContentTag)
                            .semantics(mergeDescendants = true) {}
                    )
                },
                modifier = Modifier.testTag(ListItemTag),
                headlineContent = {
                    Text(
                        text = "Test Text",
                        modifier = Modifier
                            .testTag(ListItemTextTag)
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {}
                    )
                },
                onClick = {},
                selected = false
            )
        }

        val itemBounds = rule.onNodeWithTag(ListItemTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(ListItemTextTag).getUnclippedBoundsInRoot()
        val trailingContentBounds =
            rule.onNodeWithTag(testTrailingContentTag).getUnclippedBoundsInRoot()

        (trailingContentBounds.top - itemBounds.top).assertIsEqualTo(
            12.dp,
            "padding between the top of the list item and the top of the trailing content."
        )

        (itemBounds.bottom - trailingContentBounds.bottom).assertIsEqualTo(
            12.dp,
            "padding between the bottom of the trailing content and the bottom of the list item."
        )

        (itemBounds.right - trailingContentBounds.right).assertIsEqualTo(
            16.dp,
            "padding between the end of the trailing content and the end of the list item."
        )

        (trailingContentBounds.left - textBounds.right).assertIsEqualTo(
            8.dp,
            "padding between the start of the trailing content and the end of the text."
        )
    }

    @Test
    fun listItem_semantics() {
        var selected by mutableStateOf(false)
        rule.setContent {
            Box {
                ListItem(
                    modifier = Modifier.testTag(ListItemTag),
                    onClick = { selected = !selected },
                    headlineContent = { Text(text = "List item") },
                    selected = selected
                )
            }
        }

        rule.onNodeWithTag(ListItemTag)
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
            .requestFocus()
            .assertIsEnabled()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        Truth.assertThat(selected).isEqualTo(true)
    }

    @Test
    fun listItem_longClickSemantics() {
        var selected by mutableStateOf(false)
        rule.setContent {
            Box {
                ListItem(
                    modifier = Modifier.testTag(ListItemTag),
                    onClick = { },
                    onLongClick = { selected = !selected },
                    headlineContent = { Text(text = "List item") },
                    selected = selected
                )
            }
        }

        rule.onNodeWithTag(ListItemTag)
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
            .requestFocus()
            .assertIsEnabled()
            .performLongKeyPress(rule, Key.DirectionCenter)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        Truth.assertThat(selected).isEqualTo(true)
    }

    @Test
    fun listItem_disabledSemantics() {
        rule.setContent {
            Box {
                ListItem(
                    modifier = Modifier.testTag(ListItemTag),
                    onClick = {},
                    enabled = false,
                    headlineContent = { Text(text = "List Item") },
                    selected = false
                )
            }
        }

        rule.onNodeWithTag(ListItemTag)
            .assertIsNotEnabled()
    }

    @Test
    fun listItem_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            Box {
                ListItem(
                    modifier = Modifier.testTag(ListItemTag),
                    onClick = { enabled = false },
                    enabled = enabled,
                    headlineContent = { Text(text = "List Item") },
                    selected = false
                )
            }
        }
        rule.onNodeWithTag(ListItemTag)
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
    fun listItem_oneLineHeight() {
        val expectedHeightNoIcon = 48.dp

        rule.setContent {
            ListItem(
                modifier = Modifier.testTag(ListItemTag),
                headlineContent = { Text(text = "text") },
                onClick = {},
                selected = false
            )
        }

        rule.onNodeWithTag(ListItemTag).assertHeightIsEqualTo(expectedHeightNoIcon)
    }

    @Test
    fun listItem_width() {
        rule.setContent {
            ListItem(
                modifier = Modifier.testTag(ListItemTag),
                headlineContent = { Text(text = "text") },
                onClick = {},
                selected = false
            )
        }
        rule.onNodeWithTag(ListItemTag)
            .assertWidthIsEqualTo(rule.onRoot().getUnclippedBoundsInRoot().width)
    }

    @Test
    fun denseListItem_contentPaddingHorizontal() {
        rule.setContent {
            DenseListItem(
                modifier = Modifier.testTag(DenseListItemTag),
                headlineContent = {
                    Text(
                        text = "Test Text",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(DenseListItemTextTag)
                            .semantics(mergeDescendants = true) {}
                    )
                },
                onClick = {},
                selected = false
            )
        }

        val itemBounds = rule.onNodeWithTag(DenseListItemTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(DenseListItemTextTag).getUnclippedBoundsInRoot()

        (textBounds.left - itemBounds.left).assertIsEqualTo(
            12.dp,
            "padding between the start of the list item and the start of the text."
        )

        (itemBounds.right - textBounds.right).assertIsEqualTo(
            12.dp,
            "padding between the end of the text and the end of the list item."
        )
    }

    @Test
    fun denseListItem_contentPaddingVertical() {
        rule.setContent {
            ListItem(
                modifier = Modifier.testTag(DenseListItemTag),
                headlineContent = {
                    Text(
                        text = "Test Text",
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag(DenseListItemTextTag)
                            .semantics(mergeDescendants = true) {}
                    )
                },
                onClick = {},
                selected = false
            )
        }

        val itemBounds = rule.onNodeWithTag(DenseListItemTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(DenseListItemTextTag).getUnclippedBoundsInRoot()

        (textBounds.top - itemBounds.top).assertIsEqualTo(
            12.dp,
            "padding between the top of the list item and the top of the text."
        )

        (itemBounds.bottom - textBounds.bottom).assertIsEqualTo(
            12.dp,
            "padding between the bottom of the text and the bottom of the list item."
        )
    }

    @Test
    fun denseListItem_leadingContentPadding() {
        val testIconTag = "IconTag"

        rule.setContent {
            DenseListItem(
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(ListItemDefaults.IconSizeDense)
                            .testTag(testIconTag)
                            .semantics(mergeDescendants = true) {}
                    )
                },
                modifier = Modifier.testTag(DenseListItemTag),
                headlineContent = {
                    Text(
                        text = "Test Text",
                        modifier = Modifier
                            .testTag(DenseListItemTextTag)
                            .semantics(mergeDescendants = true) {}
                    )
                },
                onClick = {},
                selected = false
            )
        }

        val itemBounds = rule.onNodeWithTag(DenseListItemTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(DenseListItemTextTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(testIconTag).getUnclippedBoundsInRoot()

        (iconBounds.top - itemBounds.top).assertIsEqualTo(
            10.dp,
            "padding between the top of the list item and the top of the icon."
        )

        (itemBounds.bottom - iconBounds.bottom).assertIsEqualTo(
            10.dp,
            "padding between the bottom of the icon and the bottom of the list item."
        )

        (iconBounds.left - itemBounds.left).assertIsEqualTo(
            12.dp,
            "padding between the start of the icon and the start of the list item."
        )

        (textBounds.left - iconBounds.right).assertIsEqualTo(
            8.dp,
            "padding between the end of the icon and the start of the text."
        )
    }

    @Test
    fun denseListItem_trailingContentPadding() {
        val testTrailingContentTag = "TrailingIconTag"

        rule.setContent {
            DenseListItem(
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(ListItemDefaults.IconSizeDense)
                            .testTag(testTrailingContentTag)
                            .semantics(mergeDescendants = true) {}
                    )
                },
                modifier = Modifier.testTag(DenseListItemTag),
                headlineContent = {
                    Text(
                        text = "Test Text",
                        modifier = Modifier
                            .testTag(DenseListItemTextTag)
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {}
                    )
                },
                onClick = {},
                selected = false
            )
        }

        val itemBounds = rule.onNodeWithTag(DenseListItemTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(DenseListItemTextTag).getUnclippedBoundsInRoot()
        val trailingContentBounds =
            rule.onNodeWithTag(testTrailingContentTag).getUnclippedBoundsInRoot()

        (trailingContentBounds.top - itemBounds.top).assertIsEqualTo(
            10.dp,
            "padding between the top of the list item and the top of the trailing content."
        )

        (itemBounds.bottom - trailingContentBounds.bottom).assertIsEqualTo(
            10.dp,
            "padding between the bottom of the trailing content and the bottom of the list item."
        )

        (itemBounds.right - trailingContentBounds.right).assertIsEqualTo(
            12.dp,
            "padding between the end of the trailing content and the end of the list item."
        )

        (trailingContentBounds.left - textBounds.right).assertIsEqualTo(
            8.dp,
            "padding between the start of the trailing content and the end of the text."
        )
    }

    @Test
    fun denseListItem_oneLineHeight() {
        val expectedHeightNoIcon = 40.dp

        rule.setContent {
            DenseListItem(
                modifier = Modifier.testTag(DenseListItemTag),
                headlineContent = { Text(text = "text") },
                onClick = {},
                selected = false
            )
        }

        rule.onNodeWithTag(DenseListItemTag).assertHeightIsEqualTo(expectedHeightNoIcon)
    }

    @Test
    fun denseListItem_width() {
        rule.setContent {
            DenseListItem(
                modifier = Modifier.testTag(DenseListItemTag),
                headlineContent = { Text(text = "text") },
                onClick = {},
                selected = false
            )
        }
        rule.onNodeWithTag(DenseListItemTag)
            .assertWidthIsEqualTo(rule.onRoot().getUnclippedBoundsInRoot().width)
    }
}

private const val ListItemTag = "ListItem"
private const val ListItemTextTag = "ListItemText"

private const val DenseListItemTag = "DenseListItem"
private const val DenseListItemTextTag = "DenseListItemText"
