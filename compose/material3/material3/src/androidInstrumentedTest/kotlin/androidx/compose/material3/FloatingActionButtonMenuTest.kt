/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FloatingActionButtonMenuTest {

    @get:Rule val rule = createComposeRule()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTestApi::class)
    @Test
    fun fabMenu_childrenCount_collapsed() {
        val items =
            listOf(
                Icons.Filled.Add to "Reply",
                Icons.Filled.Add to "Reply all",
                Icons.Filled.Add to "Forward",
                Icons.Filled.Add to "Snooze",
            )

        rule.setContent { FabMenuTest(initialExpanded = false, items = items) }

        rule.waitForIdle()
        rule.onAllNodes(isFocusable()).assertCountEquals(1)

        repeat(items.size) {
            rule.onNodeWithTag("button_$it").assertIsNotDisplayed().assertHasNoClickAction()
        }

        rule.onRoot().performKeyInput { repeat(items.size) { pressKey(Key.Tab) } }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTestApi::class)
    @Test
    fun fabMenu_childrenCount_expanded() {
        val items =
            listOf(
                Icons.Filled.Add to "Reply",
                Icons.Filled.Add to "Reply all",
                Icons.Filled.Add to "Forward",
                Icons.Filled.Add to "Snooze",
            )

        rule.setContent { FabMenuTest(initialExpanded = true, items = items) }

        rule.onAllNodes(isFocusable()).assertCountEquals(5)

        repeat(items.size) {
            rule.onNodeWithTag("button_$it").assertIsDisplayed().assertHasClickAction()
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTestApi::class)
    @Test
    fun fabMenu_keyboardNavigation_fromFabToFirstItem() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                FloatingActionButtonMenu(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    expanded = true,
                    button = {
                        ToggleFloatingActionButton(
                            checked = true,
                            onCheckedChange = {},
                            modifier = Modifier.testTag("fab").focusable(),
                        ) {}
                    },
                ) {
                    FloatingActionButtonMenuItem(
                        modifier = Modifier.testTag("item1").focusable(),
                        onClick = {},
                        icon = {},
                        text = { Text(text = "item1") },
                    )
                    FloatingActionButtonMenuItem(
                        modifier = Modifier.testTag("item2").focusable(),
                        onClick = {},
                        icon = {},
                        text = { Text(text = "item2") },
                    )
                }
            }
        }

        // Move focus to FAB.
        rule.onNodeWithTag("fab").requestFocus()
        rule.onNodeWithTag("fab").assertIsFocused()

        // Press Tab.
        rule.onNodeWithTag("fab").performKeyInput { pressKey(Key.Tab) }

        // Assert first item is focused.
        rule.onNodeWithTag("item1").assertIsFocused()

        // Move focus to FAB.
        rule.onNodeWithTag("fab").requestFocus()
        rule.onNodeWithTag("fab").assertIsFocused()

        // Press down.
        rule.onNodeWithTag("fab").performKeyInput { pressKey(Key.DirectionDown) }

        // Assert first item is focused.
        rule.onNodeWithTag("item1").assertIsFocused()
    }

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun FabMenuTest(initialExpanded: Boolean, items: List<Pair<ImageVector, String>>) {
        Box(Modifier.fillMaxSize()) {
            var fabMenuExpanded by rememberSaveable { mutableStateOf(initialExpanded) }
            FloatingActionButtonMenu(
                modifier = Modifier.align(Alignment.BottomEnd),
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                    ) {
                        val imageVector by remember {
                            derivedStateOf {
                                if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                            }
                        }
                        Icon(
                            painter = rememberVectorPainter(imageVector),
                            contentDescription = null,
                            modifier = Modifier.animateIcon({ checkedProgress }),
                        )
                    }
                },
            ) {
                items.forEachIndexed { index, item ->
                    FloatingActionButtonMenuItem(
                        modifier = Modifier.testTag("button_$index"),
                        onClick = { fabMenuExpanded = false },
                        icon = { Icon(item.first, contentDescription = null) },
                        text = { Text(text = item.second) },
                    )
                }
            }
        }
    }
}
