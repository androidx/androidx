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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class AppBarRowTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun appbarRow_itemsDisplayed_noOverflow() {
        rule.setContent {
            AppBarRow(overflowIndicator = {}) {
                clickableItem(onClick = {}, icon = { Text("Item 1") }, label = "Item 1")
                clickableItem(onClick = {}, icon = { Text("Item 2") }, label = "Item 2")
            }
        }

        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
    }

    @Test
    fun appbarRow_maxCount_itemsDisplayed() {
        rule.setContent {
            AppBarRow(overflowIndicator = {}, maxItemCount = 2) {
                clickableItem(onClick = {}, icon = { Text("Item 1") }, label = "Item 1")
                clickableItem(onClick = {}, icon = { Text("Item 2") }, label = "Item 2")
                clickableItem(onClick = {}, icon = { Text("Item 3") }, label = "Item 3")
            }
        }

        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsNotDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
    }

    @Test
    fun appbarRow_maxCount_itemsDisplayed_lastItemShows() {
        rule.setContent {
            AppBarRow(overflowIndicator = {}, maxItemCount = 2) {
                clickableItem(onClick = {}, icon = { Text("Item 1") }, label = "Item 1")
                clickableItem(onClick = {}, icon = { Text("Item 2") }, label = "Item 2")
            }
        }

        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
    }

    @Test
    fun appbarRow_itemsOverflow_overflowIndicatorDisplayed() {
        rule.setContent {
            var menuState by remember { mutableStateOf(false) }

            AppBarRow(
                overflowIndicator = {
                    IconButton(
                        onClick = { menuState = true },
                        modifier = Modifier.testTag("overflowButton"),
                    ) {
                        // Icon(null, null) // Replace with your overflow icon
                    }
                },
                modifier = Modifier.testTag("appBarRow"),
            ) {
                // Add many items to force overflow
                repeat(10) {
                    clickableItem(onClick = {}, icon = { Text("Item $it") }, label = "Item $it")
                }
            }
        }

        // Verify that the overflow indicator is displayed
        rule.onNodeWithTag("overflowButton").assertIsDisplayed()
    }

    @Test
    fun appbarRow_overflowMenu_opensAndCloses() {
        rule.setContent {
            var menuState by remember { mutableStateOf(false) }

            AppBarRow(
                overflowIndicator = {
                    IconButton(
                        onClick = { it.show() },
                        modifier = Modifier.testTag("overflowButton"),
                    ) {
                        // Icon(null, null) // Replace with your overflow icon
                    }
                },
                modifier = Modifier.testTag("appBarRow"),
            ) {
                // Add many items to force overflow
                repeat(10) { clickableItem(onClick = {}, icon = {}, label = "Item $it") }
            }
        }

        // Open the overflow menu
        rule.onNodeWithTag("overflowButton").performClick()
        rule.onNodeWithText("Item 9").assertIsDisplayed() // Check an item in the menu
    }

    @Test
    fun appbarRow_clickableItem_onClickCalled() {
        var clicked = false
        rule.setContent {
            AppBarRow(overflowIndicator = {}) {
                clickableItem(
                    onClick = { clicked = true },
                    icon = { Text("Clickable") },
                    label = "Clickable Item",
                )
            }
        }

        rule.onNodeWithText("Clickable").performClick()
        assert(clicked)
    }

    @Test
    fun appbarRow_toggleableItem_onCheckedChangeCalled() {
        var checkedState by mutableStateOf(false)
        rule.setContent {
            AppBarRow(overflowIndicator = {}) {
                toggleableItem(
                    checked = checkedState,
                    onCheckedChange = { checkedState = it },
                    icon = { Text("Toggleable") },
                    label = "Toggleable Item",
                )
            }
        }

        rule.onNodeWithText("Toggleable").performClick()

        rule.runOnIdle { assertThat(checkedState).isTrue() }

        rule.onNodeWithText("Toggleable").performClick()

        rule.runOnIdle { assertThat(checkedState).isFalse() }
    }

    @Test
    fun appbarRow_overflowMenu_itemClickClosesMenu() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                AppBarRow(
                    modifier = Modifier.align(Alignment.Center).width(200.dp),
                    overflowIndicator = {
                        IconButton(
                            onClick = { it.show() },
                            modifier = Modifier.testTag("overflowButton"),
                        ) {
                            Text("O")
                        }
                    },
                ) {
                    repeat(5) {
                        clickableItem(
                            onClick = {},
                            icon = { Text(text = "Item $it", softWrap = false, maxLines = 1) },
                            label = "Item $it",
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("overflowButton").performClick()

        rule.waitForIdle()

        rule
            .onAllNodes(hasText("Item 4"))
            .filterToOne(SemanticsMatcher("visible") { it.boundsInRoot != Rect.Zero })
            .assertIsDisplayed()
            .performClick()

        rule
            .onAllNodes(hasText("Item 4"))
            .filter(SemanticsMatcher("visible") { it.boundsInRoot != Rect.Zero })
            .assertCountEquals(0)
    }
}
