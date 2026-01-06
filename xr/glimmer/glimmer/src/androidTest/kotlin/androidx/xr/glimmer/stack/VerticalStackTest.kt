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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.size
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.createGlimmerRule
import androidx.xr.glimmer.performIndirectMove
import androidx.xr.glimmer.performIndirectPress
import androidx.xr.glimmer.performIndirectRelease
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
    fun multipleItems_displaysFirstThreeItems() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        rule.onNodeWithText("Item 5").assertDoesNotExist()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_withKeys_displaysFirstThreeItems() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item(key = 0) { StackItem("Item 0") }
                item(key = 1) { StackItem("Item 1") }
                item(key = 2) { StackItem("Item 2") }
                item(key = 3) { StackItem("Item 3") }
            }
        }

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_withNullAndIntKey_displaysFirstThreeItems() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item(key = null) { StackItem("Item 0") }
                item(key = 0) { StackItem("Item 1") }
                item(key = 1) { StackItem("Item 2") }
                item(key = 3) { StackItem("Item 3") }
            }
        }

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
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
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()
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
    fun swipeUp_pointerInput_displaysOnlyNextThreeItems() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()

        rule.onNodeWithText("Item 0").performTouchInput {
            swipe(
                start = Offset(x = centerX, y = itemHeight.toFloat()),
                end = Offset(x = centerX, y = 0f),
            )
        }

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun swipeForward_displaysOnlyNextThreeItems() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()

        performIndirectSwipe(itemHeight)

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()
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
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()

        performIndirectSwipe((itemHeight * 0.9f).toInt())

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun swipeForwardAndBackward_singleGesture_pointerInput_returnsToOriginalPosition() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)

        rule.onRoot().performTouchInput {
            down(Offset(x = centerX, y = itemHeight / 2f))
            moveTo(Offset(x = centerX, y = 0f))
        }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isGreaterThan(0.3f)

        rule.onRoot().performTouchInput { moveTo(Offset(x = centerX, y = itemHeight / 2f)) }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)

        rule.onRoot().performTouchInput { up() }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun swipeForwardAndBackward_singleGesture_returnsToOriginalPosition() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)

        val press = performIndirectPress()
        val moveForward =
            performIndirectMove(distancePx = itemHeight / 2f, previousMotionEvent = press)
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isGreaterThan(0.3f)

        val moveBackward =
            performIndirectMove(distancePx = -itemHeight / 2f, previousMotionEvent = moveForward)
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isLessThan(0.1f)

        performIndirectRelease(previousMotionEvent = moveBackward)
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
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
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()

        performIndirectSwipe(-(itemHeight * 0.9f).toInt())

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
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
        rule.onNodeWithText("Middle 1").assertIsDisplayed()
        rule.onNodeWithText("Middle 2").assertIsNotDisplayed()

        repeat(6) { index ->
            performIndirectSwipe(itemHeight)

            rule.onNodeWithText("Middle $index").assertIsDisplayed()
            rule.onNodeWithText("Middle ${(index + 1).coerceAtMost(5)}").assertIsDisplayed()
            assertThat(state.topItem).isEqualTo(index + 1)
        }

        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Last").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(7)
    }

    @Test
    fun increaseItemCount_updatesItems() = runTest {
        var itemHeight = 0
        val state = StackState()
        var itemCount by mutableStateOf(3)
        rule.setContent {
            VerticalStack(state = state) {
                items(itemCount) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
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
    fun decreaseItemCount_updatesItems() = runTest {
        var itemHeight = 0
        val state = StackState()
        var itemCount by mutableStateOf(3)
        rule.setContent {
            VerticalStack(state = state) {
                items(itemCount) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
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
    fun reorderItems_withKeys_preservesScrollPosition() = runTest {
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
        runOnUiThread { state.scrollToItem(2) }
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
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(0)

        // Scroll to a new item
        rule.runOnIdle { targetItem = 3 }
        rule.waitForIdle()

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)

        // Simulate recreation
        restorationTester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        // Verify the restored state
        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
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

        val press = performIndirectPress()

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

        performIndirectRelease(previousMotionEvent = press)

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

    @Test
    fun swipeForward_largeDistance_stopsAtNextItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        assertThat(state.topItem).isEqualTo(0)

        performIndirectSwipe(itemHeight * 3)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(1)
        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()
    }

    @Test
    fun swipeBackward_largeDistance_stopsAtPreviousItem() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(3) }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(3)

        performIndirectSwipe(-itemHeight * 3)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(2)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsNotDisplayed()
    }

    @Test
    fun flingForward_highVelocity_stopsAtNextItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(10) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }

        performIndirectSwipe(itemHeight, durationMillis = FlingDuration)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(1)
        rule.onNodeWithText("Item 1").assertIsDisplayed()
    }

    @Test
    fun twoFlingsForward_stopsAtNextNextItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(10) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }

        performIndirectSwipe(itemHeight, durationMillis = FlingDuration)
        performIndirectSwipe(itemHeight, durationMillis = FlingDuration)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(2)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
    }

    @Test
    fun flingBackward_highVelocity_stopsAtPreviousItem() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(10) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(3) }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(3)

        performIndirectSwipe(-itemHeight, durationMillis = FlingDuration)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(2)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsNotDisplayed()
    }

    @Test
    fun dragForwardAndBackward_sameGesture_canReachPreviousItem() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(2)

        val press = performIndirectPress()
        val moveForward =
            performIndirectMove(distancePx = itemHeight.toFloat() * 3, previousMotionEvent = press)
        val moveBackward =
            performIndirectMove(
                distancePx = -itemHeight.toFloat() * 2,
                previousMotionEvent = moveForward,
            )
        performIndirectRelease(previousMotionEvent = moveBackward)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(1)
        rule.onNodeWithText("Item 1").assertIsDisplayed()
    }

    @Test
    fun recompositions_afterInitialComposition_doesNotRecompose() {
        var recompositionCount = 0
        rule.setContent {
            VerticalStack {
                item {
                    StackItem("Item 0")
                    SideEffect { recompositionCount++ }
                }
            }
        }

        rule.runOnIdle { assertThat(recompositionCount).isEqualTo(1) }
    }

    @Test
    fun recompositions_afterScroll_doesNotRecompose() {
        val stackHeight = 100.dp
        val stackHeightPx = with(rule.density) { stackHeight.roundToPx() }
        var item0RecompositionCount = 0
        var item1RecompositionCount = 0
        var item2RecompositionCount = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(modifier = Modifier.size(stackHeight), state = state) {
                item {
                    StackItem("Item 0")
                    SideEffect { item0RecompositionCount++ }
                }
                item {
                    StackItem("Item 1")
                    SideEffect { item1RecompositionCount++ }
                }
                item {
                    StackItem("Item 2")
                    SideEffect { item2RecompositionCount++ }
                }
            }
        }

        performIndirectSwipe(stackHeightPx)

        rule.runOnIdle {
            assertThat(state.topItem).isEqualTo(1)
            assertThat(item0RecompositionCount).isEqualTo(1)
            assertThat(item1RecompositionCount).isEqualTo(1)
            assertThat(item2RecompositionCount).isEqualTo(1)
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

    private fun performIndirectPress() = rule.onRoot().performIndirectPress(rule)

    private fun performIndirectMove(distancePx: Float, previousMotionEvent: MotionEvent) =
        rule
            .onRoot()
            .performIndirectMove(
                rule = rule,
                distancePx = distancePx,
                previousMotionEvent = previousMotionEvent,
            )

    private fun performIndirectRelease(previousMotionEvent: MotionEvent) =
        rule.onRoot().performIndirectRelease(rule, previousMotionEvent)

    private fun performIndirectSwipe(distancePx: Int, durationMillis: Long = 200L) {
        require(distancePx != 0)
        rule
            .onRoot()
            .performIndirectSwipe(rule, distancePx.toFloat(), moveDuration = durationMillis)
    }

    suspend fun runOnUiThread(action: suspend () -> Unit) {
        rule.waitForIdle()
        withContext(Dispatchers.Main) { action() }
    }
}

/** A short swipe duration to trigger a fling. */
private const val FlingDuration: Long = 50L
