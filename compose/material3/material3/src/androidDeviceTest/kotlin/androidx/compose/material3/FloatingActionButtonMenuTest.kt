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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FloatingActionButtonMenuTest {

    @get:Rule val rule = createComposeRule()

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
        // Items aren't composed until the menu is first expanded.
        repeat(items.size) { rule.onNodeWithTag("button_$it").assertDoesNotExist() }
        rule.onRoot().performKeyInput { repeat(items.size) { pressKey(Key.Tab) } }

        // Once expanded, items stay composed but hidden after collapsing.
        rule.onNode(isToggleable()).performClick()
        rule.waitForIdle()
        rule.onNode(isToggleable()).performClick()
        rule.waitForIdle()
        rule.onAllNodes(isFocusable()).assertCountEquals(1)
        repeat(items.size) {
            rule.onNodeWithTag("button_$it").assertIsNotDisplayed().assertHasNoClickAction()
        }
    }

    @Test
    fun fabMenu_itemsComposedOnFirstExpand_notRecomposedOnToggle() {
        var expanded by mutableStateOf(false)
        var contentCompositions = 0
        // Hoisted so that recomposing the caller doesn't re-invoke it; only the menu can.
        val content: @Composable FloatingActionButtonMenuScope.() -> Unit =
            @NonSkippableComposable {
                SideEffect { contentCompositions++ }
                FloatingActionButtonMenuItem(onClick = {}, icon = {}, text = {})
            }
        rule.setContent {
            FloatingActionButtonMenu(expanded = expanded, button = {}, content = content)
        }
        rule.runOnIdle { assertThat(contentCompositions).isEqualTo(0) }

        expanded = true
        rule.runOnIdle { assertThat(contentCompositions).isEqualTo(1) }

        repeat(6) {
            expanded = !expanded
            rule.waitForIdle()
        }
        rule.runOnIdle { assertThat(contentCompositions).isEqualTo(1) }
    }

    @Test
    fun fabMenu_childrenCount_expanded() {
        val items =
            listOf(
                Icons.Filled.Add to "Reply",
                Icons.Filled.Add to "Reply all",
                Icons.Filled.Add to "Forward",
                Icons.Filled.Add to "Snooze",
            )

        rule.mainClock.autoAdvance = false
        rule.setContent { FabMenuTest(initialExpanded = true, items = items) }

        rule.onAllNodes(isFocusable()).assertCountEquals(5)

        repeat(items.size) {
            rule.onNodeWithTag("button_$it").assertIsDisplayed().assertHasClickAction()
        }
    }

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

    @Test
    fun fabMenu_staggeredAnimationAndMidExpandCollapse() {
        val items =
            listOf(
                Icons.Filled.Add to "Reply",
                Icons.Filled.Add to "Reply all",
                Icons.Filled.Add to "Forward",
                Icons.Filled.Add to "Snooze",
                Icons.Filled.Add to "Archive",
                Icons.Filled.Add to "Label",
            )
        var expanded by mutableStateOf(false)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                FloatingActionButtonMenu(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    expanded = expanded,
                    button = {
                        ToggleFloatingActionButton(
                            checked = expanded,
                            onCheckedChange = { expanded = !expanded },
                        ) {
                            val imageVector by remember {
                                derivedStateOf {
                                    if (checkedProgress > 0.5f) Icons.Filled.Close
                                    else Icons.Filled.Add
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
                            onClick = { expanded = false },
                            icon = { Icon(item.first, contentDescription = null) },
                            text = { Text(text = item.second) },
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        fun itemWidth(index: Int): Float =
            rule.onNodeWithTag("button_$index").fetchSemanticsNode().boundsInRoot.width

        // Expand and verify bottom-to-top staggered fan-out.
        expanded = true
        repeat(8) { rule.mainClock.advanceTimeBy(16L) }
        rule.onNodeWithTag("button_5").assertIsDisplayed()
        rule.onNodeWithTag("button_4").assertIsDisplayed()
        rule.onNodeWithTag("button_3").assertIsDisplayed()
        rule.onNodeWithTag("button_0").assertIsNotDisplayed()
        assertThat(itemWidth(5)).isGreaterThan(itemWidth(4))
        assertThat(itemWidth(4)).isGreaterThan(itemWidth(3))

        rule.mainClock.advanceTimeBy(400L)
        repeat(items.size) { rule.onNodeWithTag("button_$it").assertIsDisplayed() }

        // Collapse and verify top-to-bottom staggered collapse.
        expanded = false
        repeat(12) { rule.mainClock.advanceTimeBy(16L) }
        rule.onNodeWithTag("button_0").assertIsNotDisplayed()
        rule.onNodeWithTag("button_5").assertIsDisplayed()

        rule.mainClock.advanceTimeBy(400L)
        repeat(items.size) { rule.onNodeWithTag("button_$it").assertIsNotDisplayed() }

        // Mid-expand reversal: expand partway, then collapse before top items appear.
        expanded = true
        repeat(7) { rule.mainClock.advanceTimeBy(16L) }
        rule.onNodeWithTag("button_5").assertIsDisplayed()
        rule.onNodeWithTag("button_0").assertIsNotDisplayed()

        expanded = false
        rule.mainClock.advanceTimeBy(500L)
        repeat(items.size) { rule.onNodeWithTag("button_$it").assertIsNotDisplayed() }
    }

    @Test
    fun fabMenu_verticalScroll_inConstrainedHeightAndLookahead() {
        var expanded by mutableStateOf(false)
        val itemCount = 8
        rule.setContent {
            LookaheadScope {
                Box(Modifier.size(300.dp, 220.dp)) {
                    FloatingActionButtonMenu(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        expanded = expanded,
                        button = {
                            ToggleFloatingActionButton(checked = expanded, onCheckedChange = {}) {}
                        },
                    ) {
                        repeat(itemCount) { index ->
                            FloatingActionButtonMenuItem(
                                modifier = Modifier.testTag("button_$index"),
                                onClick = {},
                                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                text = { Text(text = "Item $index") },
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        expanded = true
        rule.waitForIdle()

        // Scroll to the top-most item inside the height-constrained LookaheadScope.
        rule.onNodeWithTag("button_0").performScrollTo().assertIsDisplayed()

        expanded = false
        rule.waitForIdle()
        repeat(itemCount) { rule.onNodeWithTag("button_$it").assertIsNotDisplayed() }
    }

    @Test
    fun fabMenu_dynamicItemChangesDuringAnimation() {
        var expanded by mutableStateOf(false)
        var itemCount by mutableIntStateOf(4)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                FloatingActionButtonMenu(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    expanded = expanded,
                    button = {
                        ToggleFloatingActionButton(checked = expanded, onCheckedChange = {}) {}
                    },
                ) {
                    repeat(itemCount) { index ->
                        FloatingActionButtonMenuItem(
                            modifier = Modifier.testTag("button_$index"),
                            onClick = {},
                            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            text = { Text(text = "Item $index") },
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false

        // Start expanding, then remove items mid-animation and add items after expand.
        expanded = true
        repeat(4) { rule.mainClock.advanceTimeBy(16L) }
        itemCount = 2
        rule.mainClock.advanceTimeBy(400L)
        repeat(2) { rule.onNodeWithTag("button_$it").assertIsDisplayed() }

        itemCount = 5
        rule.mainClock.advanceTimeBy(400L)
        repeat(5) { rule.onNodeWithTag("button_$it").assertIsDisplayed() }
    }

    @Test
    fun fabMenu_collapsedHeightRestoredAfterExpandAndCollapse() {
        var expanded by mutableStateOf(false)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalAlignment = Alignment.End,
                ) {
                    Box(Modifier.size(40.dp).testTag("secondary_fab"))
                    FloatingActionButtonMenu(
                        modifier = Modifier.testTag("menu"),
                        expanded = expanded,
                        button = {
                            ToggleFloatingActionButton(
                                checked = expanded,
                                onCheckedChange = { expanded = !expanded },
                            ) {}
                        },
                    ) {
                        repeat(3) { index ->
                            FloatingActionButtonMenuItem(
                                modifier = Modifier.testTag("button_$index"),
                                onClick = { expanded = false },
                                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                text = { Text(text = "Item $index") },
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        val initialMenuBounds = rule.onNodeWithTag("menu").getUnclippedBoundsInRoot()
        val initialSecondaryBounds = rule.onNodeWithTag("secondary_fab").getUnclippedBoundsInRoot()

        expanded = true
        rule.waitForIdle()
        val expandedMenuBounds = rule.onNodeWithTag("menu").getUnclippedBoundsInRoot()
        val expandedSecondaryBounds = rule.onNodeWithTag("secondary_fab").getUnclippedBoundsInRoot()
        assertThat(expandedMenuBounds.top).isLessThan(initialMenuBounds.top)
        assertThat(expandedSecondaryBounds.top).isLessThan(initialSecondaryBounds.top)

        expanded = false
        rule.waitForIdle()
        assertThat(rule.onNodeWithTag("menu").getUnclippedBoundsInRoot())
            .isEqualTo(initialMenuBounds)
        assertThat(rule.onNodeWithTag("secondary_fab").getUnclippedBoundsInRoot())
            .isEqualTo(initialSecondaryBounds)
    }

    @Composable
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
