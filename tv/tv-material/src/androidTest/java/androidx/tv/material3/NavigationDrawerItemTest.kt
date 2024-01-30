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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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
class NavigationDrawerItemTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun navigationDrawerItem_findByTagAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = onClick,
                    leadingContent = { },
                    modifier = Modifier.testTag(NavigationDrawerItemTag),
                ) {
                    Text(text = "Test Text")
                }
            }
        }

        rule.onNodeWithTag(NavigationDrawerItemTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun navigationDrawerItem_clickIsIndependentBetweenItems() {
        var openItemClickCounter = 0
        val openItemOnClick: () -> Unit = { ++openItemClickCounter }
        val openItemTag = "OpenItem"

        var closeItemClickCounter = 0
        val closeItemOnClick: () -> Unit = { ++closeItemClickCounter }
        val closeItemTag = "CloseItem"

        rule.setContent {
            DrawerScope {
                Column {
                    NavigationDrawerItem(
                        selected = false,
                        onClick = openItemOnClick,
                        leadingContent = { },
                        modifier = Modifier.testTag(openItemTag),
                    ) {
                        Text(text = "Test Text")
                    }
                    NavigationDrawerItem(
                        selected = false,
                        onClick = closeItemOnClick,
                        leadingContent = { },
                        modifier = Modifier.testTag(closeItemTag),
                    ) {
                        Text(text = "Test Text")
                    }
                }
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
    fun navigationDrawerItem_longClickAction() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }

        rule.setContent {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = { },
                    onLongClick = onLongClick,
                    leadingContent = { },
                    modifier = Modifier.testTag(NavigationDrawerItemTag),
                ) {
                    Text(text = "Test Text")
                }
            }
        }

        rule.onNodeWithTag(NavigationDrawerItemTag)
            .requestFocus()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }

        rule.onNodeWithTag(NavigationDrawerItemTag)
            .requestFocus()
            .performLongKeyPress(rule, Key.DirectionCenter, count = 2)
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(3)
        }
    }

    @Test
    fun navigationDrawerItem_findByTagAndStateChangeCheck() {
        var checkedState by mutableStateOf(true)
        val onClick: () -> Unit = { checkedState = !checkedState }

        rule.setContent {
            DrawerScope {
                NavigationDrawerItem(
                    selected = checkedState,
                    onClick = onClick,
                    leadingContent = { },
                    modifier = Modifier.testTag(NavigationDrawerItemTag),
                ) {
                    Text(text = "Test Text")
                }
            }
        }

        rule.onNodeWithTag(NavigationDrawerItemTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(!checkedState)
        }
    }

    @Test
    fun navigationDrawerItem_trailingContentPadding() {
        val testTrailingContentTag = "TrailingIconTag"

        rule.setContent {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(NavigationDrawerItemDefaults.IconSize)
                                .background(Color.Red)
                                .testTag(testTrailingContentTag)
                        )
                    },
                    leadingContent = { },
                    modifier = Modifier
                        .testTag(NavigationDrawerItemTag)
                        .border(1.dp, Color.Blue),
                ) {
                    Text(
                        text = "Test Text",
                        modifier = Modifier
                            .testTag(NavigationDrawerItemTextTag)
                            .fillMaxWidth()
                    )
                }
            }
        }

        rule.waitForIdle()

        val itemBounds = rule.onNodeWithTag(NavigationDrawerItemTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(
            NavigationDrawerItemTextTag,
            useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        val trailingContentBounds = rule
            .onNodeWithTag(testTrailingContentTag, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        (itemBounds.bottom - trailingContentBounds.bottom).assertIsEqualTo(
            16.dp,
            "padding between the bottom of the trailing content and the bottom of the nav " +
                "drawer item"
        )

        (itemBounds.right - trailingContentBounds.right).assertIsEqualTo(
            16.dp,
            "padding between the end of the trailing content and the end of the nav drawer item"
        )

        (trailingContentBounds.left - textBounds.right).assertIsEqualTo(
            8.dp,
            "padding between the start of the trailing content and the end of the text."
        )
    }

    @Test
    fun navigationDrawerItem_semantics() {
        var selected by mutableStateOf(false)
        rule.setContent {
            DrawerScope {
                NavigationDrawerItem(
                    selected = selected,
                    onClick = { selected = !selected },
                    leadingContent = { },
                    modifier = Modifier.testTag(NavigationDrawerItemTag),
                ) {
                    Text(text = "Test Text")
                }
            }
        }

        rule.onNodeWithTag(NavigationDrawerItemTag)
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
    fun navigationDrawerItem_longClickSemantics() {
        var selected by mutableStateOf(false)
        rule.setContent {
            DrawerScope {
                NavigationDrawerItem(
                    selected = selected,
                    onClick = {},
                    onLongClick = { selected = !selected },
                    leadingContent = { },
                    modifier = Modifier.testTag(NavigationDrawerItemTag),
                ) {
                    Text(text = "Test Text")
                }
            }
        }

        rule.onNodeWithTag(NavigationDrawerItemTag)
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
    fun navigationDrawerItem_disabledSemantics() {
        rule.setContent {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    enabled = false,
                    leadingContent = { },
                    modifier = Modifier.testTag(NavigationDrawerItemTag),
                ) {
                    Text(text = "Test Text")
                }
            }
        }

        rule.onNodeWithTag(NavigationDrawerItemTag)
            .assertIsNotEnabled()
    }

    @Test
    fun navigationDrawerItem_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = { enabled = false },
                    leadingContent = { },
                    enabled = enabled,
                    modifier = Modifier.testTag(NavigationDrawerItemTag),
                ) {
                    Text(text = "Test Text")
                }
            }
        }
        rule.onNodeWithTag(NavigationDrawerItemTag)
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
    fun navigationDrawerItem_oneLineHeight() {
        val expectedHeightNoIcon = 56.dp

        rule.setContent {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    leadingContent = { },
                    modifier = Modifier.testTag(NavigationDrawerItemTag),
                ) {
                    Text(text = "Test Text")
                }
            }
        }

        rule.onNodeWithTag(NavigationDrawerItemTag).assertHeightIsEqualTo(expectedHeightNoIcon)
    }

    @Test
    fun navigationDrawerItem_width() {
        rule.setContent {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = { },
                    leadingContent = { },
                    modifier = Modifier.testTag(NavigationDrawerItemTag),
                ) {
                    Text(text = "Test Text")
                }
            }
        }
        rule.onNodeWithTag(NavigationDrawerItemTag)
            .assertWidthIsEqualTo(rule.onRoot().getUnclippedBoundsInRoot().width)
    }

    @Test
    fun navigationDrawerItem_widthInInactiveState() {
        rule.setContent {
            DrawerScope(false) {
                NavigationDrawerItem(
                    selected = false,
                    onClick = { },
                    leadingContent = { },
                    modifier = Modifier.testTag(NavigationDrawerItemTag),
                ) {
                    Text(text = "Test Text")
                }
            }
        }
        rule.onNodeWithTag(NavigationDrawerItemTag)
            .assertWidthIsEqualTo(56.dp)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DrawerScope(
    isActivated: Boolean = true,
    content: @Composable NavigationDrawerScope.() -> Unit
) {
    Box {
        NavigationDrawerScopeImpl(isActivated).apply {
            content()
        }
    }
}

private const val NavigationDrawerItemTag = "NavigationDrawerItem"
private const val NavigationDrawerItemTextTag = "NavigationDrawerItemText"
