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

package androidx.xr.glimmer.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.performIndirectSwipe
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalTestApi::class)
@MediumTest
@RunWith(Parameterized::class)
class ListInputInteroperabilityTest(orientation: Orientation) :
    BaseListTestWithOrientation(orientation) {

    @Test
    fun keyNavigation_withinList_movesFocusBetweenItems() {
        rule.setContent { FocusableTestList() }

        // Initial state: First item in the list should be focused.
        rule.onListItem(0).assertIsFocused()

        // Navigate to the next item using a key event.
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToNextItemWithKeyInput()
        rule.waitForIdle()
        rule.onListItem(1).assertIsFocused()

        // Navigate to the next item again.
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToNextItemWithKeyInput()
        rule.waitForIdle()
        rule.onListItem(2).assertIsFocused()

        // Navigate back to the previous item using a key event.
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToPreviousItemWithKeyInput()
        rule.waitForIdle()
        rule.onListItem(1).assertIsFocused()

        // Navigate back to the first item.
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToPreviousItemWithKeyInput()
        rule.waitForIdle()
        rule.onListItem(0).assertIsFocused()
    }

    @Test
    fun keyNavigation_movesFocusInAndOutOfList() {
        rule.setContent {
            val testContent: @Composable () -> Unit = {
                Box(modifier = Modifier.testTag("box-before-list").size(10.dp).focusable())
                FocusableTestList(itemsCount = 2, listSize = ItemSize * 2)
                Box(modifier = Modifier.testTag("box-outside-list").size(10.dp).focusable())
            }
            if (vertical) Column { testContent() } else Row { testContent() }
        }

        // Initial state: First item in testContent should be focused.
        rule.onNodeWithTag("box-before-list").assertIsFocused()

        // Navigate inside the list
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToNextItemWithKeyInput()
        rule.onListItem(0).assertIsFocused()

        // Navigate past the last item.
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToNextItemWithKeyInput() // To Item-1
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToNextItemWithKeyInput() // To Box
        rule.waitForIdle()

        // Focus should move to the element outside the list.
        rule.onNodeWithTag("box-outside-list").assertIsFocused()

        // Navigate back into the list.
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToPreviousItemWithKeyInput()
        rule.onListItem(1).assertIsFocused()
    }

    @Test
    fun touchScroll_updatesVisibleItemsWithoutAutofocusSlowdown() {
        rule.setContent { FocusableTestList() }

        // Initial state: First four items are visible.
        rule.onListItem(0).assertIsDisplayed()
        rule.onListItem(1).assertIsDisplayed()
        rule.onListItem(2).assertIsDisplayed()

        // Scroll down by more than two items' height.
        rule.onNodeWithTag(LIST_TEST_TAG).touchScrollMainAxisBy(distance = 102.dp)
        rule.waitForIdle()

        // Items 0 and 1 should be scrolled out of view.
        rule.onListItem(0).assertIsNotDisplayed()
        rule.onListItem(1).assertIsNotDisplayed()
        rule.onListItem(2).assertIsDisplayed()
        rule.onListItem(3).assertIsDisplayed()

        // Scroll back to the top of the list.
        rule.onNodeWithTag(LIST_TEST_TAG).touchScrollMainAxisBy(distance = (-102).dp)
        rule.waitForIdle()

        // Initial items should be visible again.
        rule.onListItem(0).assertIsDisplayed()
        rule.onListItem(1).assertIsDisplayed()
        rule.onListItem(2).assertIsDisplayed()
    }

    @Test
    fun touchScroll_afterKeyEvent_doesNotChangeFocusButScrollsTheContent() {
        rule.setContent { FocusableTestList() }

        rule.onListItem(0).assertIsFocused()

        // Navigate to Item-1 using a key event.
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToNextItemWithKeyInput()
        rule.waitForIdle()

        rule.onListItem(0).assertIsDisplayed()
        rule.onListItem(1).assertIsDisplayed()
        rule.onListItem(1).assertIsFocused()

        // Touch scroll to move Items 0, 1 and 2 out of viewport.
        rule.onNodeWithTag(LIST_TEST_TAG).touchScrollMainAxisBy(151.dp)
        rule.waitForIdle()

        // Focus should remain on Item-1 even though it's not visible.
        rule.onListItem(1).assertIsNotDisplayed()
        rule.onListItem(1).assertIsFocused()
        rule.onListItem(3).assertIsDisplayed()

        // Touch scroll to move back to top of the list
        rule.onNodeWithTag(LIST_TEST_TAG).touchScrollMainAxisBy(distance = (-151).dp)
        rule.waitForIdle()

        rule.onListItem(0).assertIsDisplayed()
        rule.onListItem(1).assertIsDisplayed()
        rule.onListItem(1).assertIsFocused()
    }

    @Test
    fun keyEvent_afterTouchScroll_movesFocusToFirstVisibleItem() {
        rule.setContent { FocusableTestList() }

        rule.onListItem(0).assertIsFocused()

        // Touch scroll to move Item-0 fully out of viewport
        rule.onNodeWithTag(LIST_TEST_TAG).touchScrollMainAxisBy(51.dp)
        rule.waitForIdle()

        // Focus is still on Item-0 but not being displayed.
        rule.onListItem(0).assertIsFocused()
        rule.onListItem(0).assertIsNotDisplayed()

        // Navigate to the next item using a key event.
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToNextItemWithKeyInput()
        rule.waitForIdle()

        // Focus should move to Item-1, which should be visible.
        rule.onListItem(1).assertIsFocused()
        rule.onListItem(1).assertIsDisplayed()

        // Navigate back to Item-0, which should be visible and focused
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToPreviousItemWithKeyInput()
        rule.waitForIdle()

        rule.onListItem(0).assertIsFocused()
        rule.onListItem(0).assertIsDisplayed()
        rule.onListItem(1).assertIsDisplayed()
    }

    @Test
    fun indirectSwipe_afterKeyEvent_updatesTheFocusForwardAndBackward() {
        rule.setContent { FocusableTestList() }

        rule.onListItem(0).assertIsFocused()

        // Navigate to Item-1 using a key event.
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToNextItemWithKeyInput()
        rule.waitForIdle()
        rule.onListItem(1).assertIsFocused()

        val swipeDistance = with(rule.density) { 153.dp.toPx() }

        // Perform an indirect swipe, shift the list by 3 position. Indirect swipe is expected to
        // start from top to bottom due to how autofocus works.
        rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(rule, swipeDistance)
        rule.waitForIdle()

        rule.onListItem(0).assertIsNotDisplayed()
        rule.onListItem(1).assertIsDisplayed()
        rule.onListItem(2).assertIsDisplayed()
        rule.onListItem(3).assertIsDisplayed()
        rule.onListItem(3).assertIsFocused()

        // Perform an indirect swipe, shift the list back to original position.
        rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(rule, -swipeDistance)
        rule.waitForIdle()

        rule.onListItem(0).assertIsFocused()
        rule.onListItem(0).assertIsDisplayed()
        rule.onListItem(1).assertIsDisplayed()
    }

    @Test
    fun keyEvent_afterIndirectSwipe_navigatesBetweenItemsForwardAndBackward() {
        rule.setContent { FocusableTestList() }

        rule.onListItem(0).assertIsFocused()

        // Perform an indirect scroll.
        val swipeDistance = with(rule.density) { 51.dp.toPx() }
        rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(rule, swipeDistance)
        rule.waitForIdle()

        // Indirect scroll focuses to the next item.
        rule.onListItem(0).assertIsDisplayed()
        rule.onListItem(1).assertIsFocused()
        rule.onListItem(1).assertIsDisplayed()

        // Navigate to the next item using a key event.
        rule.onNodeWithTag(LIST_TEST_TAG).navigateToNextItemWithKeyInput()
        rule.waitForIdle()

        // Focus should move from Item-1 to Item-2.
        rule.onListItem(2).assertIsFocused()
        rule.onListItem(1).assertIsDisplayed()
        rule.onListItem(2).assertIsDisplayed()

        rule.onNodeWithTag(LIST_TEST_TAG).navigateToPreviousItemWithKeyInput()
        rule.waitForIdle()

        // Focus should move from Item-2 to Item-1.
        rule.onListItem(1).assertIsFocused()
        rule.onListItem(1).assertIsDisplayed()
        rule.onListItem(2).assertIsDisplayed()
    }

    @Test
    fun indirectScroll_afterDirectScroll_updatesFocus() {
        rule.setContent { FocusableTestList() }

        rule.onListItem(0).assertIsFocused()

        // Direct scroll Item-0 out of view. Focus remains on Item-0.
        rule.onNodeWithTag(LIST_TEST_TAG).touchScrollMainAxisBy(51.dp)
        rule.waitForIdle()
        rule.onListItem(0).assertIsFocused()
        rule.onListItem(0).assertIsNotDisplayed()

        // Perform an indirect scroll.
        val swipeDistance = with(rule.density) { 51.dp.toPx() }
        rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(rule, swipeDistance)
        rule.waitForIdle()

        // Indirect scroll should set the new focus to the item in the middle which is Item-3 and
        // move by one item.
        rule.onListItem(1).assertIsNotDisplayed()
        rule.onListItem(2).assertIsDisplayed()
        rule.onListItem(4).assertIsFocused()
    }

    @Test
    fun touchScroll_afterIndirectSwipe_doesNotChangeFocus() {
        rule.setContent { FocusableTestList() }

        rule.onListItem(0).assertIsFocused()

        // Perform an indirect scroll, focusing Item-1.
        val swipeDistance = with(rule.density) { 51.dp.toPx() }
        rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(rule, swipeDistance)
        rule.waitForIdle()

        rule.onListItem(0).assertIsDisplayed()
        rule.onListItem(1).assertIsDisplayed()
        rule.onListItem(1).assertIsFocused()

        // Perform a touch scroll by one item.
        rule.onNodeWithTag(LIST_TEST_TAG).touchScrollMainAxisBy(distance = 51.dp)
        rule.waitForIdle()

        // Focus should remain on Item-1, despite the touch scroll by one item
        rule.onListItem(0).assertIsNotDisplayed()
        rule.onListItem(1).assertIsFocused()
        rule.onListItem(1).assertIsDisplayed()
        rule.onListItem(2).assertIsDisplayed()

        // Perform a touch scroll back to top of the list.
        rule.onNodeWithTag(LIST_TEST_TAG).touchScrollMainAxisBy(distance = -(102).dp)
        rule.waitForIdle()

        // Focus should remain on Item-1, despite the touch scroll.
        rule.onListItem(0).assertIsDisplayed()
        rule.onListItem(1).assertIsFocused()
        rule.onListItem(1).assertIsDisplayed()
    }

    @Composable
    fun FocusableTestList(
        itemsCount: Int = 100,
        listSize: Dp = ItemSize * 5,
        userScrollEnabled: Boolean = true,
        listOrientation: Orientation = orientation,
        state: ListState = rememberListState(),
        itemContent: @Composable (Int) -> Unit = { FocusableListItem(it) },
    ) {
        TestList(
            state = state,
            itemsCount = itemsCount,
            listOrientation = listOrientation,
            userScrollEnabled = userScrollEnabled,
            modifier = Modifier.requiredSize(listSize),
        ) { index ->
            itemContent(index)
        }
    }

    @Composable
    private fun FocusableListItem(index: Int, focusable: Boolean = true) {
        FocusableItem(
            text = index.toString(),
            modifier = Modifier.testTag("item-$index"),
            focusable = focusable,
        )
    }

    @Composable
    private fun FocusableItem(
        text: String,
        modifier: Modifier = Modifier,
        focusable: Boolean = true,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                modifier
                    .requiredSize(ItemSize)
                    .background(color = if (isFocused) Color.Red else Color.Green)
                    .border(1.dp, Color.Black)
                    .focusable(focusable, interactionSource),
        ) {
            Text(text = text, fontSize = 30.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }

    private fun SemanticsNodeInteractionsProvider.onListItem(index: Int): SemanticsNodeInteraction {
        return onNodeWithTag("item-$index")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}

private val ItemSize: Dp = 50.dp
