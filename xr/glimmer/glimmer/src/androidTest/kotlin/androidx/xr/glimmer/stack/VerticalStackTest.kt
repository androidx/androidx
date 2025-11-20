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

import android.os.Build
import android.os.SystemClock.uptimeMillis
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalIndirectPointerApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.size
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.createGlimmerRule
import androidx.xr.glimmer.performIndirectPointerEvent
import androidx.xr.glimmer.performIndirectSwipe
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class VerticalStackTest {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

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
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsNotDisplayed()

        performIndirectSwipe(itemHeight)

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
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsNotDisplayed()

        performIndirectSwipe((itemHeight * 0.9f).toInt())

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
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()

        performIndirectSwipe(-(itemHeight * 0.9f).toInt())

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(0)
    }

    @Test
    fun scrollToEndAndBack_displaysItemsInCorrectOrder() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(3) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }

        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(2)

        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("Item 0").assertIsDisplayed() // Reached the beginning
        assertThat(state.topItem).isEqualTo(0)
    }

    @Test
    fun mixedDsl_displaysItemsInCorrectOrder() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
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
            performIndirectSwipe(itemHeight)

            rule.onNodeWithText("Middle $index").assertIsDisplayed()
            rule.onNodeWithText("Middle ${(index + 1).coerceAtMost(5)}").assertIsDisplayed()
            assertThat(state.topItem).isEqualTo(index + 1)
            rule.onNodeWithText("Middle ${index + 2}").assertIsNotDisplayed()
        }

        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Last").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(7)
    }

    @Test
    fun increaseItemCount_updatesItems() {
        var itemHeight = 0
        val state = StackState()
        var itemCount by mutableStateOf(3)
        rule.setContent {
            VerticalStack(state = state) {
                items(itemCount) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        performIndirectSwipe(itemHeight * 2)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(2)

        rule.runOnIdle { itemCount++ }

        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 3").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(3)
    }

    @Test
    fun decreaseItemCount_updatesItems() {
        var itemHeight = 0
        val state = StackState()
        var itemCount by mutableStateOf(3)
        rule.setContent {
            VerticalStack(state = state) {
                items(itemCount) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        performIndirectSwipe(itemHeight * 2)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(2)

        rule.runOnIdle { itemCount-- }

        rule.onNodeWithText("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 1").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun reorderItems_withKeys_preservesScrollPosition() {
        var itemHeight = 0
        val state = StackState()
        var items by mutableStateOf(listOf("A", "B", "C", "D"))
        rule.setContent {
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                item(key = "First", content = { StackItem("First") { itemHeight = it } })
                items(items, key = { it }) { StackItem(it) }
                items(1, key = { "Last" }) { StackItem("Last") }
            }
        }
        performIndirectSwipe(itemHeight * 2)
        rule.onNodeWithText("B").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)

        rule.runOnIdle { items = listOf("A", "C", "B", "D") }

        rule.onNodeWithText("B").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("D").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(4)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Last").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(5)

        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("D").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(4)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("B").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("C").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("A").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        performIndirectSwipe(-itemHeight)
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
        rule.setContent {
            scope = rememberCoroutineScope()
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        val interactions = mutableListOf<Interaction>()
        scope.launch { state.interactionSource.interactions.collect { interactions.add(it) } }
        rule.runOnIdle { assertThat(interactions).isEmpty() }

        performIndirectSwipe(itemHeight)

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

        runOnUiThread { state.scrollToItem(2) }

        rule.onNodeWithText("Item 1").assertExists() // Previous item
        rule.onNodeWithText("Item 2").assertExists() // Top item
        rule.onNodeWithText("Item 3").assertExists() // Next item
        rule.onNodeWithText("Item 4").assertExists() // Next next item
    }

    @Test
    fun positioningAndScale_topItem_occupiesViewportExceptForRevealArea() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                items(5) { index -> StackItem("Item $index") }
            }
        }

        val stackBounds = rule.onNodeWithTag("stack").getBoundsInRoot()
        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()

        assertThat(topItemBounds.left).isEqualTo(stackBounds.left)
        assertThat(topItemBounds.top).isEqualTo(stackBounds.top)
        assertThat(topItemBounds.right).isEqualTo(stackBounds.right)
        assertThat(topItemBounds.bottom).isLessThan(stackBounds.bottom)
    }

    @Test
    fun positioningAndScale_nextItem_smallerAndBelowTopItem() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()

        assertThat(nextItemBounds.left).isGreaterThan(topItemBounds.left)
        assertThat(nextItemBounds.top).isGreaterThan(topItemBounds.top)
        assertThat(nextItemBounds.right).isLessThan(topItemBounds.right)
        assertThat(nextItemBounds.bottom).isGreaterThan(topItemBounds.bottom)
    }

    @Test
    fun positioningAndScale_nextNextItem_smallerAndBelowNextItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        // Scroll almost fully to the next item to reveal the next-next item.
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { state.dispatchRawDelta(itemHeight * 0.99f) }

        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()
        val nextNextItemBounds = rule.onNodeWithTag("Item 2").getBoundsInRoot()

        assertThat(nextNextItemBounds.left).isGreaterThan(nextItemBounds.left)
        assertThat(nextNextItemBounds.top).isGreaterThan(nextItemBounds.top)
        assertThat(nextNextItemBounds.right).isLessThan(nextItemBounds.right)
        assertThat(nextNextItemBounds.bottom).isGreaterThan(nextItemBounds.bottom)
    }

    @Test
    fun positioningAndScale_scrollBackwardSlightly_threeItemsVisible() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(1) }

        // Scroll backward slightly reveal the previous item.
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { state.dispatchRawDelta(-itemHeight * 0.01f) }

        val stackBounds = rule.onNodeWithTag("stack").getBoundsInRoot()
        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()
        val nextNextItemBounds = rule.onNodeWithTag("Item 2").getBoundsInRoot()

        assertThat(topItemBounds.left).isEqualTo(stackBounds.left)
        assertThat(topItemBounds.top).isEqualTo(stackBounds.top)
        assertThat(topItemBounds.right).isEqualTo(stackBounds.right)
        assertThat(topItemBounds.bottom).isGreaterThan(stackBounds.top)

        assertThat(nextItemBounds.left).isGreaterThan(topItemBounds.left)
        assertThat(nextItemBounds.top).isGreaterThan(topItemBounds.top)
        assertThat(nextItemBounds.top).isLessThan(topItemBounds.bottom)
        assertThat(nextItemBounds.right).isLessThan(topItemBounds.right)
        assertThat(nextItemBounds.bottom).isGreaterThan(topItemBounds.bottom)

        assertThat(nextNextItemBounds.left).isGreaterThan(nextItemBounds.left)
        assertThat(nextNextItemBounds.top).isGreaterThan(nextItemBounds.top)
        assertThat(nextNextItemBounds.top).isLessThan(nextItemBounds.bottom)
        assertThat(nextNextItemBounds.right).isLessThan(nextItemBounds.right)
        assertThat(nextNextItemBounds.bottom).isGreaterThan(nextItemBounds.bottom)
    }

    @Test
    fun positioningAndScale_largerNextItem_nextItemBoundsLargerThanTopItem() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.size(100.dp)) {
                item { StackItem("Item 0", Modifier.fillMaxWidth().height(10.dp)) }
                item { StackItem("Item 1", Modifier.fillMaxWidth().height(50.dp)) }
            }
        }

        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()

        assertThat(nextItemBounds.left).isGreaterThan(topItemBounds.left)
        assertThat(nextItemBounds.top).isLessThan(topItemBounds.top)
        assertThat(nextItemBounds.right).isLessThan(topItemBounds.right)
        assertThat(nextItemBounds.bottom).isGreaterThan(topItemBounds.bottom)
    }

    @Test
    fun positioningAndScale_smallItems_bottomAlignedInViewport() {
        val stackSize = 100.dp
        val itemHeight = 10.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.size(stackSize).testTag("stack")) {
                items(5) { index ->
                    StackItem("Item $index", Modifier.fillMaxWidth().height(itemHeight))
                }
            }
        }

        val stackBounds = rule.onNodeWithTag("stack").getBoundsInRoot()
        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()

        // Calculate the expected position based on the bottom alignment logic.
        val expectedTopOffset = stackBounds.height - topItemBounds.height - RevealAreaSize
        assertThat(topItemBounds.left).isEqualTo(stackBounds.left)
        assertThat(topItemBounds.top.value).isWithin(1f).of(expectedTopOffset.value)
        assertThat(topItemBounds.right).isEqualTo(stackBounds.right)
        assertThat(topItemBounds.bottom.value)
            .isWithin(1f)
            .of(stackBounds.height.value - RevealAreaSize.value)

        assertThat(nextItemBounds.left).isGreaterThan(topItemBounds.left)
        assertThat(nextItemBounds.top).isGreaterThan(topItemBounds.top)
        assertThat(nextItemBounds.right).isLessThan(topItemBounds.right)
        assertThat(nextItemBounds.bottom).isGreaterThan(topItemBounds.bottom)
    }

    @Test
    fun itemDecoration_setsSizeAndShapeOnItemScope() = runTest {
        val state = StackState()
        val triangleShape = GenericShape { size, _ ->
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            close()
        }
        var itemScope0: StackItemScopeImpl? = null
        var itemScope1: StackItemScopeImpl? = null
        var itemScope2: StackItemScopeImpl? = null
        rule.setContent {
            VerticalStack(state = state) {
                repeat(10) { index ->
                    item {
                        itemScope0 = this as StackItemScopeImpl
                        StackItem(
                            "Item ${index * 3}",
                            Modifier.height(10.dp).itemDecoration(RectangleShape),
                        )
                    }
                    item {
                        itemScope1 = this as StackItemScopeImpl
                        StackItem(
                            "Item ${index * 3 + 1}",
                            Modifier.height(20.dp).itemDecoration(RoundedCornerShape(2.dp)),
                        )
                    }
                    item {
                        itemScope2 = this as StackItemScopeImpl
                        StackItem(
                            "Item ${index * 3 + 2}",
                            Modifier.height(30.dp).itemDecoration(triangleShape),
                        )
                    }
                }
            }
        }
        val initialItemScope0 = itemScope0
        val initialItemScope1 = itemScope1
        val initialItemScope2 = itemScope2

        rule.runOnIdle {
            val itemDecoration0 = itemScope0?.firstDecoration()
            val itemDecoration1 = itemScope1?.firstDecoration()
            val itemDecoration2 = itemScope2?.firstDecoration()

            with(rule.density) {
                assertThat(itemDecoration0?.size?.height).isEqualTo(10.dp.roundToPx())
                assertThat(itemDecoration1?.size?.height).isEqualTo(20.dp.roundToPx())
                assertThat(itemDecoration2?.size?.height).isEqualTo(30.dp.roundToPx())
            }

            assertThat(itemDecoration0?.shape).isEqualTo(RectangleShape)
            assertThat(itemDecoration1?.shape).isEqualTo(RoundedCornerShape(2.dp))
            assertThat(itemDecoration2?.shape).isSameInstanceAs(triangleShape)
        }

        repeat(30) { index -> runOnUiThread { state.scrollToItem(index) } }

        rule.runOnIdle {
            assertThat(state.topItem).isEqualTo(29)
            assertThat(itemScope0).isNotSameInstanceAs(initialItemScope0)
            assertThat(itemScope1).isNotSameInstanceAs(initialItemScope1)
            assertThat(itemScope2).isNotSameInstanceAs(initialItemScope2)

            val itemDecoration0 = itemScope0?.firstDecoration()
            val itemDecoration1 = itemScope1?.firstDecoration()
            val itemDecoration2 = itemScope2?.firstDecoration()

            with(rule.density) {
                assertThat(itemDecoration0?.size?.height).isEqualTo(10.dp.roundToPx())
                assertThat(itemDecoration1?.size?.height).isEqualTo(20.dp.roundToPx())
                assertThat(itemDecoration2?.size?.height).isEqualTo(30.dp.roundToPx())
            }

            assertThat(itemDecoration0?.shape).isEqualTo(RectangleShape)
            assertThat(itemDecoration1?.shape).isEqualTo(RoundedCornerShape(2.dp))
            assertThat(itemDecoration2?.shape).isSameInstanceAs(triangleShape)
        }
    }

    @Test
    fun itemDecoration_shapeChange_updatesShapeOnItemScope() {
        val state = StackState()
        val triangleShape = GenericShape { size, _ ->
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            close()
        }
        var itemShape0: Shape by mutableStateOf(RectangleShape)
        var itemShape1: Shape by mutableStateOf(RoundedCornerShape(2.dp))
        var itemShape2: Shape by mutableStateOf(triangleShape)
        var itemScope0: StackItemScopeImpl? = null
        var itemScope1: StackItemScopeImpl? = null
        var itemScope2: StackItemScopeImpl? = null
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    itemScope0 = this as StackItemScopeImpl
                    StackItem("Item 0", Modifier.itemDecoration(itemShape0))
                }
                item {
                    itemScope1 = this as StackItemScopeImpl
                    StackItem("Item 1", Modifier.itemDecoration(itemShape1))
                }
                item {
                    itemScope2 = this as StackItemScopeImpl
                    StackItem("Item 2", Modifier.itemDecoration(itemShape2))
                }
            }
        }
        rule.runOnIdle {
            assertThat(itemScope0?.firstDecoration()?.shape).isEqualTo(RectangleShape)
            assertThat(itemScope1?.firstDecoration()?.shape).isEqualTo(RoundedCornerShape(2.dp))
            assertThat(itemScope2?.firstDecoration()?.shape).isEqualTo(triangleShape)
        }

        rule.runOnIdle {
            itemShape0 = RoundedCornerShape(2.dp)
            itemShape1 = triangleShape
            itemShape2 = RectangleShape
        }

        rule.runOnIdle {
            assertThat(itemScope0?.firstDecoration()?.shape).isEqualTo(RoundedCornerShape(2.dp))
            assertThat(itemScope1?.firstDecoration()?.shape).isEqualTo(triangleShape)
            assertThat(itemScope2?.firstDecoration()?.shape).isEqualTo(RectangleShape)
        }
    }

    @Test
    fun itemDecoration_sizeChange_updatesSizeOnItemScope() {
        val state = StackState()
        var itemScope0: StackItemScopeImpl? = null
        var itemScope1: StackItemScopeImpl? = null
        var itemScope2: StackItemScopeImpl? = null
        var itemHeight0 by mutableStateOf(10.dp)
        var itemHeight1 by mutableStateOf(20.dp)
        var itemHeight2 by mutableStateOf(30.dp)
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    itemScope0 = this as StackItemScopeImpl
                    StackItem("Item 0", Modifier.height(itemHeight0).itemDecoration(RectangleShape))
                }
                item {
                    itemScope1 = this as StackItemScopeImpl
                    StackItem("Item 1", Modifier.height(itemHeight1).itemDecoration(RectangleShape))
                }
                item {
                    itemScope2 = this as StackItemScopeImpl
                    StackItem("Item 2", Modifier.height(itemHeight2).itemDecoration(RectangleShape))
                }
            }
        }
        rule.runOnIdle {
            with(rule.density) {
                assertThat(itemScope0?.firstDecoration()?.size?.height).isEqualTo(10.dp.roundToPx())
                assertThat(itemScope1?.firstDecoration()?.size?.height).isEqualTo(20.dp.roundToPx())
                assertThat(itemScope2?.firstDecoration()?.size?.height).isEqualTo(30.dp.roundToPx())
            }
        }

        rule.runOnIdle {
            itemHeight0 = 20.dp
            itemHeight1 = 30.dp
            itemHeight2 = 40.dp
        }

        rule.runOnIdle {
            with(rule.density) {
                assertThat(itemScope0?.firstDecoration()?.size?.height).isEqualTo(20.dp.roundToPx())
                assertThat(itemScope1?.firstDecoration()?.size?.height).isEqualTo(30.dp.roundToPx())
                assertThat(itemScope2?.firstDecoration()?.size?.height).isEqualTo(40.dp.roundToPx())
            }
        }
    }

    @Test
    fun masking_largerNextItem_clipsNextItemToTopItemShape() {
        val stackSize = 100.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.size(stackSize)) {
                item {
                    StackItem(
                        "Item 0",
                        Modifier.fillMaxWidth()
                            .height(10.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Red),
                    )
                }
                item {
                    StackItem(
                        "Item 1",
                        Modifier.fillMaxWidth()
                            .height(50.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Green),
                    )
                }
            }
        }

        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            val topItemTop = with(rule.density) { topItemBounds.top.roundToPx() }
            // The top of the top item is between the top and bottom of the next item
            assertThat(topItemTop).isGreaterThan(0)
            assertThat(topItemTop).isLessThan(pixels.height - 1)

            // The part of the next item above the top item is clipped
            for (x in 0 until pixels.width) {
                for (y in 0 until topItemTop) {
                    assertWithMessage("Pixel at ($x, $y) should not have the next item's color")
                        .that(pixels[x, y].toOpaque())
                        .isNotEqualTo(Color.Green)
                }
            }

            // The bottom of the next item is visible below the top item
            val nextItemBottom = with(rule.density) { nextItemBounds.bottom.roundToPx() }
            val x = (pixels.width / 2)
            val y = nextItemBottom - 1
            val nextItemColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the next item's color")
                .that(nextItemColor.red)
                .isLessThan(0.2f)
            assertWithMessage("Pixel at ($x, $y) should have the next item's color")
                .that(nextItemColor.green)
                .isGreaterThan(0.3f)
            assertWithMessage("Pixel at ($x, $y) should have the next item's color")
                .that(nextItemColor.blue)
                .isLessThan(0.2f)
        }
    }

    @Test
    fun masking_largerNextNextItem_clipsNextNextItemToNextItemShape() = runTest {
        var topItemHeight = 0
        val stackSize = 100.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.size(stackSize)) {
                item {
                    StackItem(
                        "Item 0",
                        Modifier.fillMaxWidth()
                            .height(80.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Red),
                    ) {
                        topItemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        Modifier.fillMaxWidth()
                            .height(10.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Green),
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        Modifier.fillMaxWidth()
                            .height(80.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Blue),
                    )
                }
            }
        }

        // Scroll almost fully to the next item to reveal the next-next item.
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { state.dispatchRawDelta(topItemHeight * 0.99f) }
        rule.waitForIdle()

        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()
        val nextNextItemBounds = rule.onNodeWithTag("Item 2").getBoundsInRoot()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()

            val topItemBottom = with(rule.density) { topItemBounds.bottom.roundToPx() }
            // The bottom of the top item is visible at the top of the next-next item bounds
            assertThat(topItemBottom).isGreaterThan(0)

            val nextItemTop = with(rule.density) { nextItemBounds.top.roundToPx() }
            val nextItemBottom = with(rule.density) { nextItemBounds.bottom.roundToPx() }
            // The top of the next item is between the top and bottom of the next-next item
            assertThat(nextItemTop).isGreaterThan(0)
            assertThat(nextItemTop).isLessThan(nextItemBottom)
            // The top of the next item is below the bottom of the top item
            assertThat(nextItemTop).isGreaterThan(topItemBottom)

            // The part of the next-next item above the next item and below the top item is clipped
            for (x in 0 until pixels.width) {
                for (y in topItemBottom + 1 until nextItemTop) {
                    assertWithMessage(
                            "Pixel at ($x, $y) should not have the next-next item's color"
                        )
                        .that(pixels[x, y].toOpaque())
                        .isNotEqualTo(Color.Blue)
                }
            }

            // The bottom of the next-next item is visible below the next item
            val nextNextItemBottom = with(rule.density) { nextNextItemBounds.bottom.roundToPx() }
            assertThat(nextNextItemBottom).isGreaterThan(nextItemBottom)
            val x = (pixels.width / 2)
            val y = nextNextItemBottom - 1
            val nextNextItemColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the next-next item's color")
                .that(nextNextItemColor.red)
                .isLessThan(0.1f)
            assertWithMessage("Pixel at ($x, $y) should have the next-next item's color")
                .that(nextNextItemColor.green)
                .isLessThan(0.1f)
            assertWithMessage("Pixel at ($x, $y) should have the next-next item's color")
                .that(nextNextItemColor.blue)
                .isGreaterThan(0.3f)
        }
    }

    @Test
    fun swipeForward_movesFocusForward() {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(itemHeight)

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isTrue()

            assertThat(item2FocusEvents).isEmpty()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(itemHeight)

        rule.onNodeWithTag("Item 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isTrue()
        }
    }

    @Test
    fun swipeBackward_movesFocusBackward() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
        rule.onNodeWithTag("Item 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(3)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()
            assertThat(item0FocusEvents[2].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(2)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
            assertThat(item2FocusEvents[1].isFocused).isTrue()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(-itemHeight)

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isTrue()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(-itemHeight)

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).isEmpty()
        }
    }

    @Test
    fun swipeForwardShortDistance_doesNotMoveFocus() {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe((itemHeight * 0.1f).toInt())

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()
            assertThat(item1FocusEvents).isEmpty()
        }
    }

    @Test
    fun swipeBackwardShortDistance_doesNotMoveFocus() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        runOnUiThread { state.scrollToItem(1) }
        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(3)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()
            assertThat(item0FocusEvents[2].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(2)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
            assertThat(item1FocusEvents[1].isFocused).isTrue()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe(-(itemHeight * 0.1f).toInt())

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()
            assertThat(item1FocusEvents).isEmpty()
        }
    }

    @Test
    fun swipeForwardAlmostItemHeight_movesFocusToNextItem() {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe((itemHeight * 0.9f).toInt())

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isTrue()
        }
    }

    @Test
    fun swipeBackwardAlmostItemHeight_movesFocusToPreviousItem() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        runOnUiThread { state.scrollToItem(1) }
        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(3)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()
            assertThat(item0FocusEvents[2].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(2)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
            assertThat(item1FocusEvents[1].isFocused).isTrue()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe(-(itemHeight * 0.9f).toInt())

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
        }
    }

    @Test
    fun swipeForward_stackIsNotFocused_doesNotMoveFocus() {
        val nonStackFocusRequester = FocusRequester()
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
            Box(modifier = Modifier.focusRequester(nonStackFocusRequester).focusTarget())
        }
        rule.runOnIdle { nonStackFocusRequester.requestFocus() }
        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()

        performIndirectSwipe(itemHeight)

        rule.onNodeWithTag("Item 0").assertIsNotFocused()
        rule.onNodeWithTag("Item 1").assertIsNotFocused()
        rule.onNodeWithTag("Item 2").assertIsNotFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()
            assertThat(item1FocusEvents).isEmpty()
            assertThat(item2FocusEvents).isEmpty()
        }
    }

    @Test
    fun scrollToItem_movesFocus() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()

        runOnUiThread { state.scrollToItem(2) }

        rule.onNodeWithTag("Item 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isFalse()

            assertThat(item1FocusEvents).isEmpty()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isTrue()
        }
    }

    @Test
    fun edgeScrim_onIndirectPressAndRelease_drawsScrim() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.background(Color.Red)) {
                item { Box(Modifier.focusable().fillMaxSize().background(Color.Green)) }
                item { Box(Modifier.focusable().fillMaxSize().background(Color.Blue)) }
            }
        }
        val x = 0
        val y = 0

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the item's color before press")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }

        performIndirectPress()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            val scrimTopPixelColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.red)
                .isGreaterThan(0.9f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.green)
                .isLessThan(0.1f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.blue)
                .isZero()
        }

        performIndirectRelease()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the item's color after release")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }
    }

    @Test
    fun edgeScrim_onPointerPressAndRelease_drawsScrim() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.background(Color.Red)) {
                item { Box(Modifier.focusable().fillMaxSize().background(Color.Green)) }
                item { Box(Modifier.focusable().fillMaxSize().background(Color.Blue)) }
            }
        }
        val x = 0
        val y = 0

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the item's color before press")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }

        rule.onRoot().performTouchInput { down(center) }

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            val scrimTopPixelColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.red)
                .isGreaterThan(0.9f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.green)
                .isLessThan(0.1f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.blue)
                .isZero()
        }

        rule.onRoot().performTouchInput { up() }

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the item's color after release")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }
    }

    @Composable
    private fun StackItem(
        text: String,
        modifier: Modifier = Modifier,
        onHeightChanged: (Int) -> Unit = {},
    ) {
        Box(
            modifier
                .onSizeChanged { onHeightChanged(it.height) }
                .fillMaxSize()
                .focusable()
                .testTag(text)
        ) {
            Text(text)
        }
    }

    @OptIn(ExperimentalIndirectPointerApi::class)
    private fun performIndirectPress() {
        val currentTime = uptimeMillis()
        val down =
            MotionEvent.obtain(
                /* downTime = */ currentTime,
                /* eventTime = */ currentTime,
                /* action = */ MotionEvent.ACTION_DOWN,
                /* x = */ 0f,
                /* y = */ 0f,
                /* metaState = */ 0,
            )
        down.source = SOURCE_TOUCH_NAVIGATION
        rule.onRoot().performIndirectPointerEvent(rule, IndirectPointerEvent(motionEvent = down))
    }

    @OptIn(ExperimentalIndirectPointerApi::class)
    private fun performIndirectRelease() {
        val currentTime = uptimeMillis()
        val up =
            MotionEvent.obtain(
                /* downTime = */ currentTime,
                /* eventTime = */ currentTime,
                /* action = */ MotionEvent.ACTION_UP,
                /* x = */ 0f,
                /* y = */ 0f,
                /* metaState = */ 0,
            )
        up.source = SOURCE_TOUCH_NAVIGATION
        rule.onRoot().performIndirectPointerEvent(rule, IndirectPointerEvent(motionEvent = up))
    }

    private fun performIndirectSwipe(distancePx: Int) {
        require(distancePx != 0)
        rule.onRoot().performIndirectSwipe(rule, distancePx.toFloat())
    }

    suspend fun runOnUiThread(action: suspend () -> Unit) {
        rule.waitForIdle()
        withContext(Dispatchers.Main) { action() }
    }

    private fun Color.toOpaque(): Color = copy(alpha = 1.0f)
}
