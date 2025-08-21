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

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyList
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerNestedPrefetchingTest(val config: ParamConfig) : BasePagerTest(config) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> =
            arrayOf(ParamConfig(Orientation.Vertical), ParamConfig(Orientation.Horizontal))
    }

    sealed interface Action {
        data class Compose(val index: Int, val nestedIndex: Int? = null) : Action

        data class Measure(val index: Int, val nestedIndex: Int? = null) : Action
    }

    private val pageSizePx = 30
    private val oageSizeDp = with(rule.density) { pageSizePx.toDp() }
    private val scheduler = TestPrefetchScheduler()

    @Test
    fun nestedPrefetchingForwardAfterSmallScroll() {
        val listState = LazyListState()
        composeListOfPagers(listState)
        val prefetchIndex = 2
        val actions = trackingActions {
            rule.runOnIdle { runBlocking { listState.scrollBy(5f) } }
            waitForPrefetch()
        }

        assertThat(actions)
            .containsExactly(
                Action.Compose(prefetchIndex),
                Action.Compose(prefetchIndex, 0),
                Action.Measure(prefetchIndex),
                Action.Measure(prefetchIndex, 0),
                Action.Compose(prefetchIndex, 1),
                Action.Measure(prefetchIndex, 1),
                Action.Compose(prefetchIndex, 2),
                Action.Measure(prefetchIndex, 2),
                Action.Compose(prefetchIndex, 3),
                Action.Measure(prefetchIndex, 3),
            )
            .inOrder()

        rule.onNodeWithTag(tagFor(prefetchIndex)).assertExists()
        rule.onNodeWithTag(tagFor(2, 0)).assertExists()
        rule.onNodeWithTag(tagFor(2, 1)).assertExists()
        rule.onNodeWithTag(tagFor(2, 2)).assertExists()
        rule.onNodeWithTag(tagFor(2, 3)).assertExists()
        rule.onNodeWithTag(tagFor(2, 4)).assertDoesNotExist()
    }

    @Test
    fun cancelingPrefetchCancelsItsNestedPrefetches() {
        val listState = LazyListState()
        composeListOfPagers(listState)

        rule.runOnIdle {
            runBlocking {
                // this will move the viewport so pages 1-2 are visible
                // and schedule a prefetching for 3
                listState.scrollBy(pageSizePx * 2f.toFloat())
            }
        }

        waitForPrefetch()

        rule.runOnIdle {
            assertThat(activeNodes).contains(tagFor(3))
            assertThat(activeNodes).contains(tagFor(3, 0))
            assertThat(activeNodes).contains(tagFor(3, 1))
        }

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // move viewport by screen size to pages 4-5, so page 3 is just behind
                // the first visible page
                listState.scrollBy(pageSizePx * 3f)

                // move scroll further to pages 5-6, so page 3 is reused
                listState.scrollBy(pageSizePx.toFloat())
            }
        }

        waitForPrefetch()

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // scroll again to ensure page 3 was dropped
                listState.scrollBy(pageSizePx * 100f)
            }
        }

        rule.runOnIdle {
            assertThat(activeNodes).doesNotContain(tagFor(3))
            assertThat(activeNodes).doesNotContain(tagFor(3, 0))
            assertThat(activeNodes).doesNotContain(tagFor(3, 1))
        }
    }

    @Test
    fun nestedPrefetchStartsFromFirstVisiblePageIndex() {
        val listState = LazyListState()
        composeListOfPagers(listState) { PagerState(currentPage = 4) { 10 } }
        val prefetchIndex = 2
        val actions = trackingActions {
            rule.runOnIdle { runBlocking { listState.scrollBy(5f) } }
            waitForPrefetch()
        }

        assertThat(actions)
            .containsExactly(
                Action.Compose(prefetchIndex),
                Action.Compose(prefetchIndex, 4),
                Action.Measure(prefetchIndex),
                Action.Measure(prefetchIndex, 4),
                Action.Compose(prefetchIndex, 5),
                Action.Measure(prefetchIndex, 5),
                Action.Compose(prefetchIndex, 6),
                Action.Measure(prefetchIndex, 6),
                Action.Compose(prefetchIndex, 7),
                Action.Measure(prefetchIndex, 7),
            )
            .inOrder()
    }

    private var actions: MutableList<Action>? = null

    /** Returns the list of Actions performed during block() */
    private fun trackingActions(block: () -> Unit): List<Action> {
        return mutableListOf<Action>().apply {
            actions = this
            block()
            actions = null
        }
    }

    private fun waitForPrefetch() {
        rule.runOnIdle { scheduler.executeActiveRequests() }
    }

    fun tagFor(index: Int, nestedIndex: Int? = null): String {
        return if (nestedIndex == null) {
            "$index"
        } else {
            "$index:$nestedIndex"
        }
    }

    private fun composeListOfPagers(
        lazyListState: LazyListState,
        createNestedPagerState: (index: Int) -> PagerState = { PagerState { 10 } },
    ) {
        rule.setContent {
            LazyList(
                modifier = Modifier.size(oageSizeDp * 2.5f),
                contentPadding = PaddingValues(0.dp),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                isVertical = !vertical,
                reverseLayout = false,
                state = lazyListState,
                userScrollEnabled = true,
                overscrollEffect = rememberOverscrollEffect(),
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                items(100) { index ->
                    TrackActiveNodesEffect(index)
                    val nestedState = remember(index) { createNestedPagerState(index) }
                    HorizontalOrVerticalPager(
                        modifier =
                            Modifier.size(oageSizeDp * 2)
                                .testTag(tagFor(index))
                                .trackWhenMeasured(index),
                        state = nestedState,
                        pageSize = PageSize.Fixed(oageSizeDp),
                    ) { page ->
                        TrackActiveNodesEffect(index, page)
                        Spacer(
                            Modifier.size(oageSizeDp)
                                .testTag(tagFor(index, page))
                                .trackWhenMeasured(index, page)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TrackActiveNodesEffect(index: Int, nestedIndex: Int? = null) {
        val tag = tagFor(index, nestedIndex)
        DisposableEffect(tag) {
            activeNodes.add(tag)
            actions?.add(Action.Compose(index, nestedIndex))
            onDispose { activeNodes.remove(tag) }
        }
    }

    private fun Modifier.trackWhenMeasured(index: Int, nestedIndex: Int? = null): Modifier {
        return this then
            Modifier.layout { measurable, constraints ->
                actions?.add(Action.Measure(index, nestedIndex))
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
    }
}
