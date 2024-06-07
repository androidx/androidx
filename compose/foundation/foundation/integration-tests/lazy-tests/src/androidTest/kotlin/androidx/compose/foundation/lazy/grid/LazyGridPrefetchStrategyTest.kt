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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridPrefetchStrategyTest.RecordingLazyGridPrefetchStrategy.Callback
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.compose.foundation.lazy.layout.PrefetchScheduler
import androidx.compose.foundation.lazy.layout.TestPrefetchScheduler
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalFoundationApi::class)
class LazyGridPrefetchStrategyTest(val config: Config) :
    BaseLazyGridTestWithOrientation(config.orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> =
            arrayOf(
                Config(Orientation.Vertical),
                Config(Orientation.Horizontal),
            )

        class Config(
            val orientation: Orientation,
        ) {
            override fun toString() = "orientation=$orientation"
        }

        private val LazyGridLayoutInfo.visibleIndices: List<Int>
            get() = visibleItemsInfo.map { it.index }.sorted()

        private val LazyGridLayoutInfo.lastLineIndex: Int
            get() =
                visibleItemsInfo.last().let {
                    if (this.orientation == Orientation.Vertical) it.row else it.column
                }
    }

    private val itemsSizePx = 30
    private val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    lateinit var state: LazyGridState
    private val scheduler = TestPrefetchScheduler()

    @Test
    fun callbacksTriggered_whenScrollForwardsWithoutVisibleItemsChanged() {
        val strategy = RecordingLazyGridPrefetchStrategy(scheduler)

        composeGrid(prefetchStrategy = strategy)

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(0, 1, 2, 3)),
            )
            .inOrder()
        strategy.reset()

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnScroll(delta = -5f, visibleIndices = listOf(0, 1, 2, 3)),
            )
            .inOrder()
    }

    @Test
    fun callbacksTriggered_whenScrollBackwardsWithoutVisibleItemsChanged() {
        val strategy = RecordingLazyGridPrefetchStrategy(scheduler)

        composeGrid(firstItem = 10, itemOffset = 10, prefetchStrategy = strategy)

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(10, 11, 12, 13)),
            )
            .inOrder()
        strategy.reset()

        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnScroll(delta = 5f, visibleIndices = listOf(10, 11, 12, 13)),
            )
            .inOrder()
    }

    @Test
    fun callbacksTriggered_whenScrollWithVisibleItemsChanged() {
        val strategy = RecordingLazyGridPrefetchStrategy(scheduler)

        composeGrid(prefetchStrategy = strategy)

        assertThat(strategy.callbacks)
            .containsExactly(
                RecordingLazyGridPrefetchStrategy.Callback.OnVisibleItemsUpdated(
                    visibleIndices = listOf(0, 1, 2, 3)
                ),
            )
            .inOrder()
        strategy.reset()

        rule.runOnIdle { runBlocking { state.scrollBy(itemsSizePx + 5f) } }

        assertThat(strategy.callbacks)
            .containsExactly(
                RecordingLazyGridPrefetchStrategy.Callback.OnVisibleItemsUpdated(
                    visibleIndices = listOf(2, 3, 4, 5)
                ),
                RecordingLazyGridPrefetchStrategy.Callback.OnScroll(
                    delta = -(itemsSizePx + 5f),
                    visibleIndices = listOf(2, 3, 4, 5)
                ),
            )
            .inOrder()
    }

    @Test
    fun callbacksTriggered_whenItemsChangedWithoutScroll() {
        val strategy = RecordingLazyGridPrefetchStrategy(scheduler)
        val numItems = mutableStateOf(100)

        composeGrid(prefetchStrategy = strategy, numItems = numItems)

        assertThat(strategy.callbacks)
            .containsExactly(
                RecordingLazyGridPrefetchStrategy.Callback.OnVisibleItemsUpdated(
                    visibleIndices = listOf(0, 1, 2, 3)
                ),
            )
            .inOrder()
        strategy.reset()

        numItems.value = 1

        rule.waitForIdle()

        assertThat(strategy.callbacks)
            .containsExactly(
                RecordingLazyGridPrefetchStrategy.Callback.OnVisibleItemsUpdated(
                    visibleIndices = listOf(0)
                ),
            )
            .inOrder()
    }

    @Test
    fun itemComposed_whenPrefetchedFromCallback() {
        val strategy = PrefetchNextLargestLineIndexStrategy(scheduler)

        composeGrid(prefetchStrategy = strategy)

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch()
        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("5").assertExists()
    }

    private fun waitForPrefetch() {
        rule.runOnIdle { scheduler.executeActiveRequests() }
    }

    /**
     * Initialize a 2x100 LazyGrid where the first 2 items are fully visible and the next two are
     * peeking.
     *
     * Example in vertical orientation:
     * -------
     * | | | index 0, 1
     * -------
     * | | | index 2, 3
     */
    @OptIn(ExperimentalFoundationApi::class)
    private fun composeGrid(
        firstItem: Int = 0,
        itemOffset: Int = 0,
        numItems: MutableState<Int> = mutableStateOf(100),
        prefetchStrategy: LazyGridPrefetchStrategy = DefaultLazyGridPrefetchStrategy()
    ) {
        rule.setContent {
            state =
                rememberLazyGridState(
                    initialFirstVisibleItemIndex = firstItem,
                    initialFirstVisibleItemScrollOffset = itemOffset,
                    prefetchStrategy = prefetchStrategy
                )
            LazyGrid(
                cells = 2,
                Modifier.mainAxisSize(itemsSizeDp * 1.5f).crossAxisSize(itemsSizeDp * 2),
                state,
            ) {
                items(numItems.value) { Spacer(Modifier.size(itemsSizeDp).testTag("$it")) }
            }
        }
    }

    /** LazyGridPrefetchStrategy that just records callbacks without scheduling prefetches. */
    private class RecordingLazyGridPrefetchStrategy(
        override val prefetchScheduler: PrefetchScheduler?
    ) : LazyGridPrefetchStrategy {

        sealed interface Callback {
            data class OnScroll(val delta: Float, val visibleIndices: List<Int>) : Callback

            data class OnVisibleItemsUpdated(val visibleIndices: List<Int>) : Callback
        }

        private val _callbacks: MutableList<Callback> = mutableListOf()
        val callbacks: List<Callback> = _callbacks

        override fun LazyGridPrefetchScope.onScroll(delta: Float, layoutInfo: LazyGridLayoutInfo) {
            _callbacks.add(Callback.OnScroll(delta, layoutInfo.visibleIndices))
        }

        override fun LazyGridPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyGridLayoutInfo) {
            _callbacks.add(Callback.OnVisibleItemsUpdated(layoutInfo.visibleIndices))
        }

        override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) = Unit

        fun reset() {
            _callbacks.clear()
        }
    }

    /**
     * LazyGridPrefetchStrategy that always prefetches the largest index line off screen no matter
     * the scroll direction.
     */
    private class PrefetchNextLargestLineIndexStrategy(
        override val prefetchScheduler: PrefetchScheduler?
    ) : LazyGridPrefetchStrategy {

        private val handles = mutableVectorOf<LazyLayoutPrefetchState.PrefetchHandle>()
        private var prefetchIndex: Int = -1

        override fun LazyGridPrefetchScope.onScroll(delta: Float, layoutInfo: LazyGridLayoutInfo) {
            val index = layoutInfo.lastLineIndex + 1
            if (handles.isNotEmpty() && index != prefetchIndex) {
                cancelPrefetch()
            }
            handles.addAll(scheduleLinePrefetch(index))
            prefetchIndex = index
        }

        override fun LazyGridPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyGridLayoutInfo) =
            Unit

        override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) = Unit

        private fun cancelPrefetch() {
            handles.forEach { it.cancel() }
            prefetchIndex = -1
        }
    }
}
