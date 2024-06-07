/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.DefaultLazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListPrefetchScope
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.compose.foundation.lazy.layout.PrefetchScheduler
import androidx.compose.foundation.lazy.layout.TestPrefetchScheduler
import androidx.compose.foundation.lazy.list.LazyListPrefetchStrategyTest.RecordingLazyListPrefetchStrategy.Callback
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
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
class LazyListPrefetchStrategyTest(val config: Config) :
    BaseLazyListTestWithOrientation(config.orientation) {

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

        private val LazyListLayoutInfo.visibleIndices: List<Int>
            get() = visibleItemsInfo.map { it.index }.sorted()
    }

    private val itemsSizePx = 30
    private val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    lateinit var state: LazyListState
    private val scheduler = TestPrefetchScheduler()

    @Test
    fun callbacksTriggered_whenScrollForwardsWithoutVisibleItemsChanged() {
        val strategy = RecordingLazyListPrefetchStrategy(scheduler)

        composeList(prefetchStrategy = strategy)

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(0, 1)),
            )
            .inOrder()
        strategy.reset()

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnScroll(delta = -5f, visibleIndices = listOf(0, 1)),
            )
            .inOrder()
    }

    @Test
    fun callbacksTriggered_whenScrollBackwardsWithoutVisibleItemsChanged() {
        val strategy = RecordingLazyListPrefetchStrategy(scheduler)

        composeList(firstItem = 10, itemOffset = 10, prefetchStrategy = strategy)

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(10, 11)),
            )
            .inOrder()
        strategy.reset()

        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnScroll(delta = 5f, visibleIndices = listOf(10, 11)),
            )
            .inOrder()
    }

    @Test
    fun callbacksTriggered_whenScrollWithVisibleItemsChanged() {
        val strategy = RecordingLazyListPrefetchStrategy(scheduler)

        composeList(prefetchStrategy = strategy)

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(0, 1)),
            )
            .inOrder()
        strategy.reset()

        rule.runOnIdle { runBlocking { state.scrollBy(itemsSizePx + 5f) } }

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(1, 2)),
                Callback.OnScroll(delta = -(itemsSizePx + 5f), visibleIndices = listOf(1, 2)),
            )
            .inOrder()
    }

    @Test
    fun callbacksTriggered_whenItemsChangedWithoutScroll() {
        val strategy = RecordingLazyListPrefetchStrategy(scheduler)
        val numItems = mutableStateOf(100)

        composeList(prefetchStrategy = strategy, numItems = numItems)

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(0, 1)),
            )
            .inOrder()
        strategy.reset()

        numItems.value = 1

        rule.waitForIdle()

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(0)),
            )
            .inOrder()
    }

    @Test
    fun itemComposed_whenPrefetchedFromCallback() {
        val strategy = PrefetchNextLargestIndexStrategy(scheduler)

        composeList(prefetchStrategy = strategy)

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch()
        rule.onNodeWithTag("2").assertExists()
    }

    private fun waitForPrefetch() {
        rule.runOnIdle { scheduler.executeActiveRequests() }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun composeList(
        firstItem: Int = 0,
        itemOffset: Int = 0,
        numItems: MutableState<Int> = mutableStateOf(100),
        prefetchStrategy: LazyListPrefetchStrategy = DefaultLazyListPrefetchStrategy()
    ) {
        rule.setContent {
            state =
                rememberLazyListState(
                    initialFirstVisibleItemIndex = firstItem,
                    initialFirstVisibleItemScrollOffset = itemOffset,
                    prefetchStrategy = prefetchStrategy
                )
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                state,
            ) {
                items(numItems.value) {
                    Spacer(
                        Modifier.mainAxisSize(itemsSizeDp)
                            .fillMaxCrossAxis()
                            .testTag("$it")
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    )
                }
            }
        }
    }

    /** LazyListPrefetchStrategy that just records callbacks without scheduling prefetches. */
    private class RecordingLazyListPrefetchStrategy(
        override val prefetchScheduler: PrefetchScheduler?
    ) : LazyListPrefetchStrategy {

        sealed interface Callback {
            data class OnScroll(val delta: Float, val visibleIndices: List<Int>) : Callback

            data class OnVisibleItemsUpdated(val visibleIndices: List<Int>) : Callback
        }

        private val _callbacks: MutableList<Callback> = mutableListOf()
        val callbacks: List<Callback> = _callbacks

        override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
            _callbacks.add(Callback.OnScroll(delta, layoutInfo.visibleIndices))
        }

        override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) {
            _callbacks.add(Callback.OnVisibleItemsUpdated(layoutInfo.visibleIndices))
        }

        override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) = Unit

        fun reset() {
            _callbacks.clear()
        }
    }

    /**
     * LazyListPrefetchStrategy that always prefetches the next largest index off screen no matter
     * the scroll direction.
     */
    private class PrefetchNextLargestIndexStrategy(
        override val prefetchScheduler: PrefetchScheduler?
    ) : LazyListPrefetchStrategy {

        private var handle: LazyLayoutPrefetchState.PrefetchHandle? = null
        private var prefetchIndex: Int = -1

        override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
            val index = layoutInfo.visibleIndices.last() + 1
            if (handle != null && index != prefetchIndex) {
                cancelPrefetch()
            }
            handle = schedulePrefetch(index)
            prefetchIndex = index
        }

        override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) =
            Unit

        override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) = Unit

        private fun cancelPrefetch() {
            handle?.cancel()
            prefetchIndex = -1
        }
    }
}
