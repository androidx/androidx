/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyListRequestScrollTest(private val orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    private val itemSize = with(rule.density) { 100.toDp() }

    @Test
    fun requestScrollToItem_withIndex0_itemsPrepended_scrollsToNewFirstItem() {
        // Given a list of items numbered from 10 to 15 and a request to scroll to the start.
        var list by mutableStateOf((10..15).toList())
        lateinit var state: LazyListState

        rule.setContent {
            state = rememberLazyListState()

            LazyColumnOrRow(Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) { Item(remember { "$it" }) }

                Snapshot.withoutReadObservation { state.requestScrollToItem(index = 0) }
            }
        }

        // When the list is updated by prepending from 0 to 9, such that the new list contains
        // from 0 to 15.
        rule.runOnIdle { list = (0..15).toList() }

        // Then we are scrolled to the start where the visible items are 0, 1, and 2.
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.visibleKeys).isEqualTo(listOf(0, 1, 2))
        }
    }

    @Test
    fun requestScrollToItem_withIndex0_scrollToEnd_isScrolledToEnd() {
        // Given a list of items numbered from 0 to 15 and a request to scroll to the start.
        val list by mutableStateOf((0..15).toList())
        lateinit var state: LazyListState

        rule.setContent {
            state = rememberLazyListState()

            LazyColumnOrRow(Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) { Item(remember { "$it" }) }

                Snapshot.withoutReadObservation { state.requestScrollToItem(index = 0) }
            }
        }

        // When we scroll towards the end very quickly.
        rule.onNode(hasScrollAction()).performTouchInput {
            swipeWithVelocity(
                start =
                    when (orientation) {
                        Orientation.Vertical -> bottomCenter
                        Orientation.Horizontal -> centerRight
                    },
                end =
                    when (orientation) {
                        Orientation.Vertical -> topCenter
                        Orientation.Horizontal -> centerLeft
                    },
                endVelocity = 5_000F
            )
        }

        // Then we are at the end of the list because the data didn't change so we never
        // scrolled back to the start.
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isNotEqualTo(0)
            assertThat(state.visibleKeys).isEqualTo(list.takeLast(3))
        }
    }

    // This test case is meant to cover a chat app use case.
    // Chat apps inside the conversation typically have the most-recent item be first in the list
    // (since they paginate the older items as the user scrolls).
    // The most recent items are also usually at the bottom.
    // If a long message comes in, we may want to scroll to the top of that message rather than
    // seeing the bottom of it.
    @Test
    fun requestScrollToItem_withStartOfNewFirstItem_prependedItems_scrollsToStartOfNewFirstItem() {
        // Given list of ints from 3 to 30 laid out from bottom-to-top/right-to-left, currently at
        // the bottom/right, a request to scroll to the start of the first item, and a list that is
        // smaller than any individual item.
        var list by mutableStateOf((3..30).toList())
        val listSize = itemSize * 0.6f
        lateinit var state: LazyListState

        rule.setContent {
            state = rememberLazyListState()

            // The content is inside of its own fun, so it gets recomposed when [list] changes and
            // triggers SideEffect.
            @Composable
            fun TestContent(list: List<Int>) {
                LazyColumnOrRow(
                    Modifier.width(listSize).height(listSize),
                    state,
                    reverseLayout = true
                ) {
                    items(list, key = { it }) { Item(remember { "$it" }) }
                }

                SideEffect {
                    val firstMessageChanged = state.firstVisibleItemKey() != list.firstOrNull()
                    if (!state.canScrollBackward && firstMessageChanged) {
                        // This scrolls to the start of the first item.
                        state.requestScrollToItem(
                            index = 1,
                            scrollOffset = with(rule.density) { -listSize.roundToPx() }
                        )
                    }
                }
            }

            TestContent(list)
        }
        rule.onNode(hasScrollAction()).performScrollToIndex(0)

        // When the list is updated, prepending items 0 to 2 so the list contains from 0 to 30.
        rule.runOnIdle { list = (0..30).toList() }

        // Then the first visible item has index 0, its key is 0, and it's scrolled to the start
        // of that item (since we're laying out from bottom-to-top/right-to-left, we want the offset
        // to be the bottom/right part of that item that's not visible).
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            with(rule.density) {
                assertThat(state.firstVisibleItemScrollOffset)
                    .isEqualTo(itemSize.roundToPx() - listSize.roundToPx())
            }
            assertThat(state.visibleKeys).isEqualTo(listOf(0))
        }
    }

    @Test
    fun requestScrollToItem_withFirstVisibleIndex_firstVisibleItemMoved_staysScrolledAtSameIndex() {
        // Given list of ints from 0 to 25 and request scrolling to the current index, and
        // currently scrolled to index 5.
        var list by mutableStateOf((0..25).toList())
        lateinit var state: LazyListState

        rule.setContent {
            state = rememberLazyListState()

            // The content is inside of its own fun, so it gets recomposed when [list] changes and
            // triggers SideEffect.
            @Composable
            fun TestContent(list: List<Int>) {
                LazyColumnOrRow(Modifier.size(itemSize * 2.5f), state) {
                    items(list, key = { it }) { Item(remember { "$it" }) }
                }

                SideEffect { state.requestScrollToItem(index = state.firstVisibleItemIndex) }
            }

            TestContent(list = list)
        }
        rule.onNode(hasScrollAction()).performScrollToIndex(5)

        // When we update list of ints to move item 5 to the end of the list.
        rule.runOnIdle { list = (0..4).toList() + (6..25).toList() + listOf(5) }

        // Then first item is index is still 5, visible items now (6, 7, 8) instead of (24, 25, 5).
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(5)
            assertThat(state.visibleKeys).isEqualTo(listOf(6, 7, 8))
        }
    }

    @Test
    fun requestScrollToItem_withFirstVisibleIndex_index0ItemRemoved_staysScrolledAtSameIndex() {
        // Given list of ints from 0 to 35 and request scrolling to the current index, and
        // currently scrolled to index 15.
        var list by mutableStateOf((0..35).toList())
        lateinit var state: LazyListState

        rule.setContent {
            state = rememberLazyListState()

            // The content is inside of its own fun, so it gets recomposed when [list] changes and
            // triggers SideEffect.
            @Composable
            fun TestContent(list: List<Int>) {
                LazyColumnOrRow(Modifier.size(itemSize * 2.5f), state) {
                    items(list, key = { it }) { Item(remember { "$it" }) }
                }

                SideEffect {
                    if (!state.isScrollInProgress) {
                        state.requestScrollToItem(index = state.firstVisibleItemIndex)
                    }
                }
            }

            TestContent(list = list)
        }
        rule.onNode(hasScrollAction()).performScrollToIndex(15)

        // When the list is updated with the first item removed (now from 1 to 35).
        rule.runOnIdle { list = (1..35).toList() }

        // Then first item is index is still 15, the items have shifted back one to (16, 17, 18).
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(15)
            assertThat(state.visibleKeys).isEqualTo(listOf(16, 17, 18))
        }
    }

    @Test
    fun requestScrollToItem_firstVisibleItemIndexUpdatesInstantly() {
        // Given a list of items numbered from 0 to 30.
        val list by mutableStateOf((0..30).toList())
        lateinit var state: LazyListState

        rule.setContent {
            state = rememberLazyListState()

            LazyColumnOrRow(Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) { Item(remember { "$it" }) }
            }
        }

        rule.runOnIdle {
            // When we scroll to index 30.
            state.requestScrollToItem(index = 30)

            // Then the first visible item index is instantly updated to 30.
            assertThat(state.firstVisibleItemIndex).isEqualTo(30)
        }
    }

    // We want to make sure even without any other remeasure trigger, requestScrollToItem
    // itself triggers a remeasure.
    @Test
    fun requestScrollToItem_withoutChangingData_scrollsToRequestedIndex() {
        // Given a list of items numbered from 0 to 30.
        val list by mutableStateOf((0..30).toList())
        lateinit var state: LazyListState

        rule.setContent {
            state = rememberLazyListState()

            LazyColumnOrRow(Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) { Item(remember { "$it" }) }
            }
        }

        rule.runOnIdle {
            // When we scroll to an offset of 30 items.
            with(rule.density) {
                // We don't change the index here because that will for sure trigger a remeasure.
                // Typically just changing the offset won't, but requestScrollToItem should always
                // trigger a remeasure.
                state.requestScrollToItem(index = 0, scrollOffset = itemSize.roundToPx() * 30)
            }
        }

        // Then we are scrolled to the end where the visible items are 28, 29, and 30.
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(28)
            assertThat(state.visibleKeys).isEqualTo(listOf(28, 29, 30))
        }
    }

    @Test
    fun requestScrollToItem_whileScrolling_cancelsScroll() {
        // Given a list of items numbered from 0 to 15.
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope

        rule.setContent {
            state = rememberLazyListState()
            scope = rememberCoroutineScope()

            LazyColumnOrRow(Modifier.size(itemSize), state) {
                items(15, key = { it }) { Item(remember { "$it" }) }
            }
        }

        // When a scroll is launched, and requestScrollToItem is called.
        var canceled = false
        rule.runOnIdle {
            scope.launch {
                state.scroll {
                    try {
                        awaitCancellation()
                    } finally {
                        canceled = true
                    }
                }
            }
        }
        rule.runOnIdle { state.requestScrollToItem(index = 0) }

        // Then the scroll was canceled.
        rule.waitUntil { canceled }
    }

    @Test
    fun moveFirstVisibleItem_afterScrollRequestIsProcessed_maintainsScrollByFirstVisibleItemKey() {
        // Given a list of items numbered from 10 to 20 and a request to scroll to the start if
        // the first visible item is at index 0. Items 0 to 9 were prepended. Then we scrolled to
        // item 1.
        var list by mutableStateOf((10..20).toList())
        lateinit var state: LazyListState

        rule.setContent {
            state = rememberLazyListState()

            LazyColumnOrRow(Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) { Item(remember { "$it" }) }

                Snapshot.withoutReadObservation {
                    if (state.firstVisibleItemIndex == 0) {
                        state.requestScrollToItem(index = 0)
                    }
                }
            }
        }
        rule.runOnIdle { list = (0..20).toList() }
        rule.onNode(hasScrollAction()).performScrollToIndex(1)

        // When item 1 moves to the end of the list.
        rule.runOnIdle { list = listOf(0) + (2..20).toList() + listOf(1) }

        // Then we are scrolled to the end where the visible items are 19, 20, and 1.
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(list.size - 3)
            assertThat(state.visibleKeys).isEqualTo(listOf(19, 20, 1))
        }
    }

    private fun LazyListState.firstVisibleItemKey() = layoutInfo.visibleItemsInfo.firstOrNull()?.key

    @Composable
    private fun Item(tag: String) {
        Spacer(Modifier.testTag(tag).size(itemSize))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}

private val LazyListState.visibleKeys: List<Any>
    get() = layoutInfo.visibleItemsInfo.map { it.key }
