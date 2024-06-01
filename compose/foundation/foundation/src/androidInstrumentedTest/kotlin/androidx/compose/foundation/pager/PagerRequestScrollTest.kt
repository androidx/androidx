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

package androidx.compose.foundation.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class PagerRequestScrollTest(private val orientation: Orientation) :
    BasePagerTest(ParamConfig(orientation)) {

    private val proposedPageSize = with(rule.density) { 100.toDp() }

    @Test
    fun requestScrollToPage_withIndex0_pagesPrepended_scrollsToNewCurrentPage() {
        // Given a list of pages numbered from 10 to 15 and a request to scroll to the start.
        var list by mutableStateOf((10..15).toList())
        lateinit var state: PagerState

        rule.setContent {
            state = rememberPagerState { list.size }

            HorizontalOrVerticalPager(
                modifier = Modifier.size(proposedPageSize * 2.5f).testTag(PagerTestTag),
                pageSize = PageSize.Fixed(proposedPageSize),
                state = state,
                key = { it }
            ) {
                Page(remember { "$it" })

                Snapshot.withoutReadObservation { state.requestScrollToPage(page = 0) }
            }
        }

        // When the list is updated by prepending from 0 to 9, such that the new list contains
        // from 0 to 15.
        rule.runOnIdle { list = (0..15).toList() }

        // Then we are scrolled to the start where the visible pages are 0, 1, and 2.
        rule.runOnIdle {
            Truth.assertThat(state.currentPage).isEqualTo(0)
            Truth.assertThat(state.visibleKeys).isEqualTo(listOf(0, 1, 2))
        }
    }

    // This test case is meant to cover a chat app use case.
    // Chat apps inside the conversation typically have the most-recent page be first in the list
    // (since they paginate the older pages as the user scrolls).
    // The most recent pages are also usually at the bottom.
    // If a long message comes in, we may want to scroll to the top of that message rather than
    // seeing the bottom of it.
    @Test
    fun requestScrollToPage_withStartOfNewCurrentPage_prependedPages_goToStartOfNewCurrentPage() {
        // Given list of ints from 3 to 30 laid out from bottom-to-top/right-to-left, currently at
        // the bottom/right, a request to scroll to the start of the first page, and a list that is
        // smaller than any individual page.
        var list by mutableStateOf((3..30).toList())
        val listSize = proposedPageSize * 0.6f
        lateinit var state: PagerState

        rule.setContent {
            // The content is inside of its own fun, so it gets recomposed when [list] changes and
            // triggers SideEffect.
            @Composable
            fun TestContent(list: List<Int>) {
                state = rememberPagerState { list.size }
                HorizontalOrVerticalPager(
                    modifier = Modifier.width(listSize).height(listSize).testTag(PagerTestTag),
                    pageSize = PageSize.Fixed(proposedPageSize),
                    state = state,
                    reverseLayout = true,
                    key = { it }
                ) {
                    Page(remember { "$it" })
                }

                SideEffect {
                    val firstMessageChanged = state.firstVisiblePageKey() != list.firstOrNull()
                    if (!state.canScrollBackward && firstMessageChanged) {
                        // This scrolls to the start of the first page.
                        state.requestScrollToPage(
                            page = 1,
                            pageOffsetFraction =
                                with(rule.density) {
                                    -listSize.roundToPx() / state.pageSizeWithSpacing.toFloat()
                                }
                        )
                    }
                }
            }

            TestContent(list)
        }
        onPager().performScrollToIndex(0)

        // When the list is updated, prepending pages 0 to 2 so the list contains from 0 to 30.
        rule.runOnIdle { list = (0..30).toList() }

        // Then the current page has index 0, its key is 0, and it's scrolled to the start
        // of that page (since we're laying out from bottom-to-top/right-to-left, we want the offset
        // to be the bottom/right part of that page that's not visible).
        rule.runOnIdle {
            Truth.assertThat(state.currentPage).isEqualTo(0)
            with(rule.density) {
                Truth.assertThat(state.currentPageOffsetFraction)
                    .isEqualTo(
                        (proposedPageSize.roundToPx() - listSize.roundToPx()) /
                            state.pageSizeWithSpacing
                    )
            }
            Truth.assertThat(state.visibleKeys).isEqualTo(listOf(0))
        }
    }

    @Test
    fun requestScrollToPage_withCurrentPage_currentPageMoved_staysScrolledAtSamePage() {
        // Given list of ints from 0 to 25 and request scrolling to the current page, and
        // currently scrolled to index 5.
        var list by mutableStateOf((0..25).toList())
        lateinit var state: PagerState

        rule.setContent {

            // The content is inside of its own fun, so it gets recomposed when [list] changes and
            // triggers SideEffect.
            @Composable
            fun TestContent(list: List<Int>) {
                state = rememberPagerState { list.size }
                HorizontalOrVerticalPager(
                    modifier = Modifier.size(proposedPageSize * 2.5f).testTag(PagerTestTag),
                    pageSize = PageSize.Fixed(proposedPageSize),
                    state = state,
                    key = { list[it] }
                ) {
                    Page(remember { "$it" })
                }

                SideEffect { state.requestScrollToPage(page = state.currentPage) }
            }

            TestContent(list = list)
        }
        onPager().performScrollToIndex(5)

        // When we update list of ints to move page 5 to the end of the list.
        rule.runOnIdle { list = (0..4).toList() + (6..25).toList() + listOf(5) }

        // Then current page is index is still 5, visible pages now (6, 7, 8) instead of (24, 25,
        // 5).
        rule.runOnIdle {
            Truth.assertThat(state.currentPage).isEqualTo(5)
            Truth.assertThat(state.visibleKeys).isEqualTo(listOf(6, 7, 8))
        }
    }

    @Test
    fun requestScrollToPage_withCurrentPage_index0PageRemoved_staysScrolledAtSamePage() {
        // Given list of ints from 0 to 35 and request scrolling to the current index, and
        // currently scrolled to index 15.
        var list by mutableStateOf((0..35).toList())
        lateinit var state: PagerState

        rule.setContent {

            // The content is inside of its own fun, so it gets recomposed when [list] changes and
            // triggers SideEffect.
            @Composable
            fun TestContent(list: List<Int>) {
                state = rememberPagerState { list.size }
                HorizontalOrVerticalPager(
                    modifier = Modifier.size(proposedPageSize * 2.5f).testTag(PagerTestTag),
                    pageSize = PageSize.Fixed(proposedPageSize),
                    state = state,
                    key = { list[it] }
                ) {
                    Page(remember { "$it" })
                }

                SideEffect {
                    if (!state.isScrollInProgress) {
                        state.requestScrollToPage(page = state.currentPage)
                    }
                }
            }

            TestContent(list = list)
        }
        onPager().performScrollToIndex(15)

        // When the list is updated with the current page removed (now from 1 to 35).
        rule.runOnIdle {
            Truth.assertThat(state.currentPage).isEqualTo(15)
            list = (1..35).toList()
        }

        // Then current page is index is still 15, the pages have shifted back one to (16, 17, 18).
        rule.runOnIdle {
            Truth.assertThat(state.currentPage).isEqualTo(15)
            Truth.assertThat(state.visibleKeys).isEqualTo(listOf(16, 17, 18))
        }
    }

    @Test
    fun requestScrollToPage_currentPageUpdatesInstantly() {
        // Given a list of pages numbered from 0 to 30.
        val list by mutableStateOf((0..30).toList())
        lateinit var state: PagerState

        rule.setContent {
            state = rememberPagerState { list.size }

            HorizontalOrVerticalPager(
                modifier = Modifier.size(proposedPageSize * 2.5f).testTag(PagerTestTag),
                pageSize = PageSize.Fixed(proposedPageSize),
                state = state,
                key = { list[it] }
            ) {
                Page(remember { "$it" })
            }
        }

        rule.runOnIdle {
            // When we scroll to index 30.
            state.requestScrollToPage(page = 30)

            // Then the current visible page index is instantly updated to 30.
            Truth.assertThat(state.currentPage).isEqualTo(30)
        }
    }

    // We want to make sure even without any other remeasure trigger, requestScrollToPage
    // itself triggers a remeasure.
    @Test
    fun requestScrollToPage_withoutChangingData_scrollsToRequestedPage() {
        // Given a list of pages numbered from 0 to 30.
        val list by mutableStateOf((0..30).toList())
        lateinit var state: PagerState

        rule.setContent {
            state = rememberPagerState { list.size }

            HorizontalOrVerticalPager(
                modifier = Modifier.size(proposedPageSize * 2.5f).testTag(PagerTestTag),
                pageSize = PageSize.Fixed(proposedPageSize),
                state = state,
                key = { list[it] }
            ) {
                Page(remember { "$it" })
            }
        }

        rule.runOnIdle {
            // When we scroll to an offset of 30 pages.
            with(rule.density) {
                // We don't change the index here because that will for sure trigger a remeasure.
                // Typically just changing the offset won't, but requestScrollToPage should always
                // trigger a remeasure.
                state.requestScrollToPage(
                    page = 0,
                    pageOffsetFraction =
                        proposedPageSize.roundToPx() * 30f / state.pageSizeWithSpacing
                )
            }
        }

        // Then we are scrolled to the end where the visible pages are 28, 29, and 30.
        rule.runOnIdle {
            Truth.assertThat(state.currentPage).isEqualTo(28)
            Truth.assertThat(state.visibleKeys).isEqualTo(listOf(28, 29, 30))
        }
    }

    @Test
    fun requestScrollToPage_whileScrolling_cancelsScroll() {
        // Given a list of pages numbered from 0 to 15.
        lateinit var state: PagerState
        lateinit var scope: CoroutineScope

        rule.setContent {
            state = rememberPagerState { 15 }
            scope = rememberCoroutineScope()

            HorizontalOrVerticalPager(
                modifier = Modifier.size(proposedPageSize).testTag(PagerTestTag),
                pageSize = PageSize.Fixed(proposedPageSize),
                state = state,
                key = { it }
            ) {
                Page(remember { "$it" })
            }
        }

        // When a scroll is launched, and requestScrollToPage is called.
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
        rule.runOnIdle { state.requestScrollToPage(page = 0) }

        // Then the scroll was canceled.
        rule.waitUntil { canceled }
    }

    @Test
    fun moveCurrentPage_afterScrollRequestIsProcessed_maintainsScrollByCurrentPageKey() {
        // Given a list of pages numbered from 10 to 20 and a request to scroll to the start if
        // the current page is at index 0. Pages 0 to 9 were prepended. Then we scrolled to
        // page 1.
        var list by mutableStateOf((10..20).toList())
        lateinit var state: PagerState

        rule.setContent {
            state = rememberPagerState { list.size }

            HorizontalOrVerticalPager(
                modifier = Modifier.size(proposedPageSize * 2.5f).testTag(PagerTestTag),
                pageSize = PageSize.Fixed(proposedPageSize),
                state = state,
                key = { list[it] }
            ) {
                Page(remember { "$it" })

                Snapshot.withoutReadObservation {
                    if (state.currentPage == 0) {
                        state.requestScrollToPage(page = 0)
                    }
                }
            }
        }
        rule.runOnIdle { list = (0..20).toList() }
        onPager().performScrollToIndex(1)

        // When page 1 moves to the end of the list.
        rule.runOnIdle { list = listOf(0) + (2..20).toList() + listOf(1) }

        // Then we are scrolled to the end where the visible pages are 19, 20, and 1.
        rule.runOnIdle {
            Truth.assertThat(state.currentPage).isEqualTo(list.size - 3)
            Truth.assertThat(state.visibleKeys).isEqualTo(listOf(19, 20, 1))
        }
    }

    private fun PagerState.firstVisiblePageKey() = layoutInfo.visiblePagesInfo.firstOrNull()?.key

    private val PagerState.visibleKeys
        get() = layoutInfo.visiblePagesInfo.map { it.key }

    @Composable
    private fun Page(tag: String) {
        Spacer(Modifier.testTag(tag).size(proposedPageSize))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
