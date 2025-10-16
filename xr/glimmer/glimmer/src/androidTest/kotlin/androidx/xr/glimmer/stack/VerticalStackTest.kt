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

package androidx.xr.glimmer.stack

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.performIndirectSwipe
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VerticalStackTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val focusRequester = FocusRequester()

    @Test
    fun zeroItems_displaysNothing() {
        val state = StackState()
        rule.setContent {
            VerticalStack(modifier = Modifier.testTag("stack"), state = state) {
                // No items
            }
        }

        assertThat(rule.onNodeWithTag("stack").getBoundsInRoot().size).isEqualTo(DpSize.Zero)
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun singleItem_displaysItem() {
        val state = StackState()
        rule.setContent { VerticalStack(state = state) { item { Text("Single Item") } } }

        rule.onNodeWithText("Single Item").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_displaysFirstTwoItems() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsNotDisplayed()
        rule.onNodeWithText("Item 5").assertDoesNotExist()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_customInitialTopItem_displaysRequestedAndNextItems() {
        val state = StackState(initialTopItem = 1)
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_lastInitialTopItem_displaysLastItem() {
        val state = StackState(initialTopItem = 4)
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        rule.onNodeWithText("Item 4").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(4)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_outOfRangeInitialTopItem_displaysLastItem() {
        val state = StackState(initialTopItem = 10)
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        rule.onNodeWithText("Item 4").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(4)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_stateChanges_maintainsItemCount() {
        val state1 = StackState()
        val state2 = StackState()
        var state by mutableStateOf(state1)
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }
        rule.runOnIdle { assertThat(state1.itemCount).isEqualTo(5) }

        rule.runOnIdle { state = state2 }

        rule.runOnIdle { assertThat(state2.itemCount).isEqualTo(5) }
    }

    @Test
    fun swipeUp_pointerInput_displaysOnlyNextTwoItems() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsNotDisplayed()

        rule.onNodeWithText("Item 0").performTouchInput {
            swipe(
                start = Offset(x = centerX, y = itemHeight.toFloat()),
                end = Offset(x = centerX, y = 0f),
            )
        }

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun swipeForward_displaysOnlyNextTwoItems() {
        var itemHeight = 0
        val state = StackState()
        rule.setContentWithInitialFocus {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsNotDisplayed()

        requestFocusAndPerformIndirectSwipe(itemHeight)

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun swipeForward_almostItemHeight_snapsToNextItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContentWithInitialFocus {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsNotDisplayed()

        requestFocusAndPerformIndirectSwipe((itemHeight * 0.9f).toInt())

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun swipeBackward_almostItemHeight_snapsToPreviousItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContentWithInitialFocus {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()

        requestFocusAndPerformIndirectSwipe(-(itemHeight * 0.9f).toInt())

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(0)
    }

    @Test
    fun scrollToEndAndBack_displaysItemsInCorrectOrder() {
        var itemHeight = 0
        val state = StackState()
        rule.setContentWithInitialFocus {
            VerticalStack(state = state) {
                items(3) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }

        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(2)

        requestFocusAndPerformIndirectSwipe(-itemHeight)
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        requestFocusAndPerformIndirectSwipe(-itemHeight)
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        requestFocusAndPerformIndirectSwipe(-itemHeight)
        rule.onNodeWithText("Item 0").assertIsDisplayed() // Reached the beginning
        assertThat(state.topItem).isEqualTo(0)
    }

    @Test
    fun mixedDsl_displaysItemsInCorrectOrder() {
        var itemHeight = 0
        val state = StackState()
        rule.setContentWithInitialFocus {
            VerticalStack(state = state) {
                item { StackItem("First") { itemHeight = it } }
                items(3) { StackItem("Middle $it") }
                items(listOf(3, 4, 5)) { StackItem("Middle $it") }
                item { StackItem("Last") }
            }
        }
        rule.onNodeWithText("First").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        rule.onNodeWithText("Middle 0").assertIsDisplayed()
        rule.onNodeWithText("Middle 1").assertIsNotDisplayed()

        repeat(6) { index ->
            requestFocusAndPerformIndirectSwipe(itemHeight)

            rule.onNodeWithText("Middle $index").assertIsDisplayed()
            rule.onNodeWithText("Middle ${(index + 1).coerceAtMost(5)}").assertIsDisplayed()
            assertThat(state.topItem).isEqualTo(index + 1)
            rule.onNodeWithText("Middle ${index + 2}").assertIsNotDisplayed()
        }

        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Last").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(7)
    }

    @Test
    fun increaseItemCount_updatesItems() {
        var itemHeight = 0
        val state = StackState()
        var itemCount by mutableStateOf(3)
        rule.setContentWithInitialFocus {
            VerticalStack(state = state) {
                items(itemCount) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        requestFocusAndPerformIndirectSwipe(itemHeight * 2)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(2)

        rule.runOnIdle { itemCount++ }

        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 3").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(3)
    }

    @Test
    fun decreaseItemCount_updatesItems() {
        var itemHeight = 0
        val state = StackState()
        var itemCount by mutableStateOf(3)
        rule.setContentWithInitialFocus {
            VerticalStack(state = state) {
                items(itemCount) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        requestFocusAndPerformIndirectSwipe(itemHeight * 2)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(2)

        rule.runOnIdle { itemCount-- }

        rule.onNodeWithText("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 1").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun reorderItems_withKeys_preservesScrollPosition() {
        var itemHeight = 0
        val state = StackState()
        var items by mutableStateOf(listOf("A", "B", "C", "D"))
        rule.setContentWithInitialFocus {
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                item(key = "First", content = { StackItem("First") { itemHeight = it } })
                items(items, key = { it }) { StackItem(it) }
                items(1, key = { "Last" }) { StackItem("Last") }
            }
        }
        requestFocusAndPerformIndirectSwipe(itemHeight * 2)
        rule.onNodeWithText("B").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)

        rule.runOnIdle { items = listOf("A", "C", "B", "D") }

        rule.onNodeWithText("B").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("D").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(4)
        requestFocusAndPerformIndirectSwipe(itemHeight)
        rule.onNodeWithText("Last").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(5)

        requestFocusAndPerformIndirectSwipe(-itemHeight)
        rule.onNodeWithText("D").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(4)
        requestFocusAndPerformIndirectSwipe(-itemHeight)
        rule.onNodeWithText("B").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
        requestFocusAndPerformIndirectSwipe(-itemHeight)
        rule.onNodeWithText("C").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        requestFocusAndPerformIndirectSwipe(-itemHeight)
        rule.onNodeWithText("A").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        requestFocusAndPerformIndirectSwipe(-itemHeight)
        rule.onNodeWithText("First").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
    }

    @Test
    fun stateRestoration_restoresTopItem() {
        val restorationTester = StateRestorationTester(rule)
        var targetItem by mutableStateOf<Int?>(null)
        lateinit var state: StackState

        restorationTester.setContent {
            state = rememberStackState(initialTopItem = 0)

            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index") }
            }

            LaunchedEffect(targetItem) { targetItem?.let { state.scrollToItem(it) } }
        }

        // Verify the initial state
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(0)

        // Scroll to a new item
        rule.runOnIdle { targetItem = 2 }
        rule.waitForIdle()

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)

        // Simulate recreation
        restorationTester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        // Verify the restored state
        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
    }

    @Test
    fun interactionSource_emitsDragInteractions() {
        var itemHeight = 0
        val state = StackState()
        lateinit var scope: CoroutineScope
        rule.setContentWithInitialFocus {
            scope = rememberCoroutineScope()
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        val interactions = mutableListOf<Interaction>()
        scope.launch { state.interactionSource.interactions.collect { interactions.add(it) } }
        rule.runOnIdle { assertThat(interactions).isEmpty() }

        requestFocusAndPerformIndirectSwipe(itemHeight)

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions[0]).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
        }
    }

    @Test
    fun alwaysComposesNextTwoItems() = runTest {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }
        rule.onNodeWithText("Item 0").assertExists() // Top item
        rule.onNodeWithText("Item 1").assertExists() // Next item
        rule.onNodeWithText("Item 2").assertExists() // Next next item

        state.scrollToItem(2)

        rule.onNodeWithText("Item 1").assertExists() // Previous item
        rule.onNodeWithText("Item 2").assertExists() // Top item
        rule.onNodeWithText("Item 3").assertExists() // Next item
        rule.onNodeWithText("Item 4").assertExists() // Next next item
    }

    @Composable
    private fun StackItem(text: String, onHeightChanged: (Int) -> Unit = {}) {
        Box(Modifier.onSizeChanged { onHeightChanged(it.height) }.fillMaxSize().focusTarget()) {
            Text(text)
        }
    }

    private fun ComposeContentTestRule.setContentWithInitialFocus(content: @Composable () -> Unit) {
        setContent { Box(Modifier.focusRequester(focusRequester)) { content() } }
        requestFocus()
    }

    private fun requestFocusAndPerformIndirectSwipe(distancePx: Int) {
        require(distancePx != 0)
        // TODO(b/413429531): remove once VerticalStack supports moving focus automatically.
        requestFocus()
        rule.onRoot().performIndirectSwipe(rule, distancePx.toFloat())
    }

    private fun requestFocus() {
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.waitForIdle()
    }
}
