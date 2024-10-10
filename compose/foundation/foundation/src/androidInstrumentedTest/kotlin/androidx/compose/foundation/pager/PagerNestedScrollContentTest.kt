/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.DefaultFlingBehavior
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyList
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerNestedScrollContentTest(config: ParamConfig) : BasePagerTest(config = config) {

    @Test
    fun nestedScrollContent_shouldNotPropagateUnconsumedFlings() {
        // Arrange
        createPager(pageCount = { DefaultPageCount }) {
            LazyList(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                isVertical = vertical, // scrollable content on the same direction as pager
                reverseLayout = false,
                state = rememberLazyListState(),
                userScrollEnabled = true,
                overscrollEffect = ScrollableDefaults.overscrollEffect(),
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items(10) {
                    Box(modifier = Modifier.size(100.dp)) { BasicText(text = it.toString()) }
                }
            }
        }

        // Act: High velocity swipe should fling inner list to edge
        val forwardDelta = pagerSize / 2f * scrollForwardSign.toFloat()
        rule.onNodeWithTag(TestTag).performTouchInput {
            swipeWithVelocityAcrossMainAxis(10000f, forwardDelta)
        }
        rule.waitForIdle()

        // Assert: Fling was not propagated, so we didn't move pages
        assertThat(pagerState.currentPage).isEqualTo(0)
        assertEquals(pagerState.currentPageOffsetFraction, 0f, 0.01f)
    }

    @Test
    fun nestedScrollContent_shouldCancelFlingIfOnEdge() {
        // Arrange
        rule.mainClock.autoAdvance = false
        val defaultFlingBehavior = DefaultFlingBehavior(splineBasedDecay(rule.density))
        var flingTriggered = false
        val flingInspector =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    flingTriggered = true
                    return with(defaultFlingBehavior) { performFling(initialVelocity) }
                }
            }
        createPager(pageCount = { DefaultPageCount }) {
            LazyList(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                isVertical = vertical, // scrollable content on the same direction as pager
                reverseLayout = false,
                flingBehavior = flingInspector,
                state = rememberLazyListState(initialFirstVisibleItemIndex = 8),
                userScrollEnabled = true,
                overscrollEffect = ScrollableDefaults.overscrollEffect(),
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items(10) {
                    Box(modifier = Modifier.size(100.dp)) { BasicText(text = it.toString()) }
                }
            }
        }

        // Act: High velocity swipe should fling inner list to edge
        val forwardDelta = pagerSize * 0.5f * scrollForwardSign.toFloat()
        rule.onNodeWithTag(TestTag).performTouchInput {
            swipeWithVelocityAcrossMainAxis(10000f, forwardDelta)
        }

        rule.mainClock.advanceTimeUntil { flingTriggered } // wait for drag to finish

        val previousOffset = pagerState.currentPageOffsetFraction
        rule.mainClock.advanceTimeBy(1_000L) // advance time

        // should've moved by then.
        assertThat(pagerState.currentPageOffsetFraction).isNotEqualTo(previousOffset)
    }

    @Test
    fun nestedScrollContent_shouldPropagateCrossAxisUnconsumedFlings() {
        // Arrange
        var scrollAvailable = Offset.Zero
        var postFlingVelocity = Velocity.Zero
        val dataCapturingConnection =
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    scrollAvailable += available
                    return Offset.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    postFlingVelocity = available
                    return Velocity.Zero
                }
            }
        createPager(
            pageCount = { DefaultPageCount },
            nestedScrollConnection = dataCapturingConnection
        ) {
            LazyList(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                isVertical = !vertical, // scrollable content on the cross direction of pager
                reverseLayout = false,
                state = rememberLazyListState(),
                userScrollEnabled = true,
                overscrollEffect = ScrollableDefaults.overscrollEffect(),
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items(10) {
                    Box(modifier = Modifier.size(100.dp)) { BasicText(text = it.toString()) }
                }
            }
        }

        // Act
        val forwardDelta = pagerSize / 2f * scrollForwardSign.toFloat()
        rule.onNodeWithTag(TestTag).performTouchInput {
            swipeWithVelocityAcrossCrossAxis(10000f, forwardDelta)
        }
        rule.waitForIdle()

        // Assert
        val mainAxisVelocity = if (vertical) postFlingVelocity.y else postFlingVelocity.x
        val crossAxisVelocity = if (vertical) postFlingVelocity.x else postFlingVelocity.y
        assertThat(mainAxisVelocity.absoluteValue).isEqualTo(0f)
        assertThat(crossAxisVelocity.absoluteValue).isNotEqualTo(0f)

        val mainAxisScrollAvailable = if (vertical) scrollAvailable.y else scrollAvailable.x
        val crossAxisScrollAvailable = if (vertical) scrollAvailable.x else scrollAvailable.y
        assertThat(crossAxisScrollAvailable.absoluteValue).isNotEqualTo(0f)
        assertThat(mainAxisScrollAvailable.absoluteValue).isEqualTo(0f)
    }

    @Test
    fun nestedScrollContent_shouldPropagateScrollCorrectly() {
        // Arrange
        val lazyListState = LazyListState(9)
        createPager(pageCount = { DefaultPageCount }) {
            LazyList(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                isVertical = vertical, // scrollable content on the same direction as pager
                reverseLayout = false,
                state = lazyListState,
                userScrollEnabled = true,
                overscrollEffect = ScrollableDefaults.overscrollEffect(),
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items(10) {
                    Box(modifier = Modifier.size(100.dp)) { BasicText(text = it.toString()) }
                }
            }
        }

        // Act: Scroll More than Half an Item
        val forwardDelta = pagerSize * 0.6f * scrollForwardSign.toFloat()
        rule.onNodeWithTag(TestTag).performTouchInput {
            swipeWithVelocityAcrossMainAxis(10000f, forwardDelta)
        }
        rule.waitForIdle()

        // Assert: Inner list won't consume scroll and Pager can scroll to the next page
        assertThat(pagerState.currentPage).isEqualTo(1)
        assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0f)

        // reset inner list
        rule.runOnIdle { runBlocking { lazyListState.scrollToItem(0) } }

        // Act: Scroll More than Half an Item
        val backwardDelta = pagerSize * 0.6f * scrollForwardSign.toFloat() * -1f
        rule.onNodeWithTag(TestTag).performTouchInput {
            swipeWithVelocityAcrossMainAxis(10000f, backwardDelta)
        }
        rule.waitForIdle()

        // Assert: Inner list won't consume scroll and Pager can scroll to the previous page
        assertThat(pagerState.currentPage).isEqualTo(0)
        assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun nestedScrollContent_shouldEnsurePagerIsSettled_WhenDirectionChanges() {
        // Arrange
        val lazyListState = LazyListState(9)
        var touchSlop = 0f
        createPager(pageCount = { DefaultPageCount }) {
            touchSlop = LocalViewConfiguration.current.touchSlop
            LazyList(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                isVertical = vertical, // scrollable content on the same direction as pager
                reverseLayout = false,
                state = lazyListState,
                userScrollEnabled = true,
                overscrollEffect = ScrollableDefaults.overscrollEffect(),
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items(10) {
                    Box(modifier = Modifier.size(100.dp)) { BasicText(text = it.toString()) }
                }
            }
        }

        val forwardDelta = pagerSize * 0.4f * scrollForwardSign.toFloat()
        val firstLazyListItem = lazyListState.firstVisibleItemIndex
        val firstLazyListItemOffset = lazyListState.firstVisibleItemScrollOffset
        rule.onNodeWithTag(TestTag).performTouchInput {
            down(center)
            val toMove = forwardDelta + touchSlop * scrollForwardSign.toFloat()
            moveBy(if (vertical) Offset(x = 0f, y = toMove) else Offset(x = toMove, y = 0f))
        }

        // Assert: Inner list won't consume scroll and pager moved
        rule.runOnIdle {
            assertThat(abs(pagerState.currentPageOffsetFraction - 0.4f)).isLessThan(0.001f)
            assertThat(lazyListState.firstVisibleItemScrollOffset)
                .isEqualTo(firstLazyListItemOffset)
            assertThat(lazyListState.firstVisibleItemIndex).isEqualTo(firstLazyListItem)
        }
        rule.onNodeWithTag(TestTag).performTouchInput {
            moveBy(
                if (vertical) Offset(x = 0f, y = -forwardDelta / 2)
                else Offset(x = -forwardDelta / 2, y = 0f)
            )
        }

        // assert: pager moved, but list is still at 0 after direction change
        rule.runOnIdle {
            assertThat(abs(pagerState.currentPageOffsetFraction - 0.2f)).isLessThan(0.001f)
            assertThat(lazyListState.firstVisibleItemScrollOffset)
                .isEqualTo(firstLazyListItemOffset)
            assertThat(lazyListState.firstVisibleItemIndex).isEqualTo(firstLazyListItem)
        }

        rule.onNodeWithTag(TestTag).performTouchInput { up() }
    }

    @Test
    fun nestedScrollContent_shouldEnsurePagerIsSettled_WhenCrossDirectionScrolls() {
        // Arrange
        val lazyListState = LazyListState(9)
        var touchSlop = 0f
        createPager(pageCount = { DefaultPageCount }) { page ->
            touchSlop = LocalViewConfiguration.current.touchSlop
            LazyList(
                modifier = Modifier.fillMaxSize().testTag("InnerListPage$page"),
                contentPadding = PaddingValues(0.dp),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                isVertical = !vertical, // scrollable content on opposite direction of pager
                reverseLayout = false,
                state = lazyListState,
                userScrollEnabled = true,
                overscrollEffect = ScrollableDefaults.overscrollEffect(),
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items(10) {
                    Box(modifier = Modifier.size(100.dp)) { BasicText(text = it.toString()) }
                }
            }
        }

        val forwardDelta = pagerSize * 0.4f * scrollForwardSign.toFloat()
        val firstLazyListItem = lazyListState.firstVisibleItemIndex
        val firstLazyListItemOffset = lazyListState.firstVisibleItemScrollOffset
        rule.onNodeWithTag(TestTag).performTouchInput {
            down(center)
            val toMove = forwardDelta + touchSlop * scrollForwardSign.toFloat()
            moveBy(if (vertical) Offset(x = 0f, y = toMove) else Offset(x = toMove, y = 0f))
            up()
        }

        // Assert: Inner list won't consume scroll and pager moved
        rule.runOnIdle {
            assertThat(abs(pagerState.currentPageOffsetFraction)).isLessThan(0.001f)
            assertThat(lazyListState.firstVisibleItemScrollOffset)
                .isEqualTo(firstLazyListItemOffset)
            assertThat(lazyListState.firstVisibleItemIndex).isEqualTo(firstLazyListItem)
        }

        // try to move inner list
        rule.onNodeWithTag("InnerListPage0").performTouchInput {
            down(center)
            moveBy(
                if (vertical) Offset(x = -forwardDelta / 2, y = 0f)
                else Offset(x = 0f, y = -forwardDelta / 2)
            )
            up()
        }

        // assert: pager did not move
        rule.runOnIdle { assertThat(abs(pagerState.currentPageOffsetFraction)).isLessThan(0.001f) }
    }

    @Test
    fun nestedScrollContent_focusShouldMoveAndSnapPages() {
        // Arrange
        lateinit var innerListFocusRequester: FocusRequester
        lateinit var pagerFocusRequester: FocusRequester
        val focusItems = mutableSetOf<String>()
        val rowColumnContent: @Composable (Int) -> Unit = { page ->
            repeat(DefaultPageCount) { item ->
                val columnFocusRequester =
                    FocusRequester().apply {
                        if (item == 3 && page == 5) innerListFocusRequester = this
                    }
                Box(
                    modifier =
                        Modifier.focusRequester(columnFocusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    focusItems.add("page=$page-item=$item")
                                }
                            }
                            .size(150.dp)
                            .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = "page=$page-item=$item")
                }
            }
        }
        createPager(
            modifier = Modifier.fillMaxSize(),
            pageCount = { DefaultPageCount },
            initialPage = 3,
            pageSize = { PageSize.Fixed(100.dp) }
        ) { page ->
            val focusRequester =
                FocusRequester().apply { if (page == 5) pagerFocusRequester = this }
            val rowColumnModifier =
                Modifier.focusRequester(focusRequester).verticalScroll(rememberScrollState())

            if (vertical) {
                Row(modifier = rowColumnModifier) { rowColumnContent(page) }
            } else {
                Column(modifier = rowColumnModifier) { rowColumnContent(page) }
            }
        }

        // Act: Request first page to focus.
        rule.runOnIdle { pagerFocusRequester.requestFocus() }

        // Assert: Check we're settled.
        rule.runOnIdle { assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f) }

        // Act: Focus scroll inner scrollable
        rule.runOnIdle { innerListFocusRequester.requestFocus() }

        // Assert: Check we actually scrolled.
        rule.runOnIdle { assertThat(focusItems).contains("page=5-item=3") }

        // Act: Move focus in inner scrollable
        val previousPage = pagerState.currentPage
        rule.runOnIdle {
            assertTrue {
                if (vertical) {
                    focusManager.moveFocus(FocusDirection.Next)
                } else {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            }
        }

        // Assert: Check we actually scrolled, but didn't move pages.
        rule.runOnIdle {
            assertThat(focusItems).contains("page=5-item=4")
            assertThat(pagerState.currentPage).isEqualTo(previousPage)
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }

        // Act: Reset.
        rule.runOnIdle { pagerFocusRequester.requestFocus() }

        // Act: Move focus in pager.
        rule.runOnIdle {
            assertTrue {
                if (vertical) {
                    focusManager.moveFocus(FocusDirection.Down)
                } else {
                    focusManager.moveFocus(FocusDirection.Right)
                }
            }
        }

        // Assert: Check we moved pages.
        rule.runOnIdle {
            assertThat(focusItems).contains("page=6-item=0")
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        }
    }

    @Test
    fun focusableContentInPage_focusMoveShouldNotLeavePagesInIntermediateState() {
        lateinit var pagerFocusRequester: FocusRequester

        createPager(
            modifier = Modifier.fillMaxSize(),
            pageCount = { DefaultPageCount },
            initialPage = 3
        ) { page ->
            val focusRequester = remember {
                FocusRequester().apply { if (page == 5) pagerFocusRequester = this }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(64.dp).focusRequester(focusRequester).focusable())
            }
        }

        // Assert: Pager is settled
        assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        assertThat(pagerState.currentPage).isEqualTo(3)

        // Scroll to a page
        rule.runOnIdle { scope.launch { pagerState.scrollToPage(5) } }

        // Assert: Pager is settled
        assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
        assertThat(pagerState.currentPage).isEqualTo(5)

        // Act: Request focus.
        rule.runOnIdle { pagerFocusRequester.requestFocus() }

        // Assert: Pager is settled
        rule.runOnIdle {
            assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
            assertThat(pagerState.currentPage).isEqualTo(5)
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun params() = AllOrientationsParams
    }
}

private const val TestTag = "pager"
