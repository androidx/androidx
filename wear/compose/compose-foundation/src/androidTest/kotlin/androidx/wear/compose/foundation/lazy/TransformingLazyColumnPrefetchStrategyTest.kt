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

package androidx.wear.compose.foundation.lazy

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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnPrefetchStrategyTest.RecordingLazyListPrefetchStrategy.Callback
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.wear.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.wear.compose.foundation.lazy.layout.PrefetchRequest
import androidx.wear.compose.foundation.lazy.layout.PrefetchRequestScope
import androidx.wear.compose.foundation.lazy.layout.PrefetchScheduler
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@LargeTest
@OptIn(ExperimentalFoundationApi::class)
class TransformingLazyColumnPrefetchStrategyTest {

    @get:Rule val rule = createComposeRule()

    private val itemsSizePx = 30
    private val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    lateinit var state: TransformingLazyColumnState
    private val scheduler = TestPrefetchScheduler()

    @Test
    fun callbacksTriggered_whenScrollForwardsWithoutVisibleItemsChanged() {
        val strategy = RecordingLazyListPrefetchStrategy(scheduler)

        composeList(prefetchStrategy = strategy)

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(0, 1, 2)),
            )
            .inOrder()
        strategy.reset()

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        assertThat(strategy.callbacks)
            .contains(
                Callback.OnScroll(delta = -5f, visibleIndices = listOf(0, 1, 2)),
            )
    }

    @Test
    fun callbacksTriggered_whenScrollBackwardsWithoutVisibleItemsChanged() {
        val strategy = RecordingLazyListPrefetchStrategy(scheduler)

        composeList(anchorItemIndex = 10, anchorItemScrollOffset = 0, prefetchStrategy = strategy)

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(9, 10, 11)),
            )
            .inOrder()
        strategy.reset()

        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        assertThat(strategy.callbacks)
            .contains(
                Callback.OnScroll(delta = 5f, visibleIndices = listOf(9, 10, 11)),
            )
    }

    @Test
    fun callbacksTriggered_whenScrollWithVisibleItemsChanged() {
        val strategy = RecordingLazyListPrefetchStrategy(scheduler)

        composeList(prefetchStrategy = strategy)

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(0, 1, 2)),
            )
            .inOrder()
        strategy.reset()

        rule.runOnIdle { runBlocking { state.scrollBy(itemsSizePx + 5f) } }

        assertThat(strategy.callbacks)
            .contains(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(1, 2, 3)),
            )
        assertThat(strategy.callbacks)
            .contains(
                Callback.OnScroll(delta = -(itemsSizePx + 5f), visibleIndices = listOf(1, 2, 3)),
            )
    }

    @Test
    fun callbacksTriggered_whenItemsChangedWithoutScroll() {
        val strategy = RecordingLazyListPrefetchStrategy(scheduler)
        val numItems = mutableStateOf(100)

        composeList(prefetchStrategy = strategy, numItems = numItems)

        assertThat(strategy.callbacks)
            .containsExactly(
                Callback.OnVisibleItemsUpdated(visibleIndices = listOf(0, 1, 2)),
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
        rule.onNodeWithTag("1").assertExists()
    }

    private fun waitForPrefetch() {
        rule.runOnIdle { scheduler.executeActiveRequests() }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun composeList(
        anchorItemIndex: Int = 1,
        anchorItemScrollOffset: Int = 0,
        numItems: MutableState<Int> = mutableStateOf(100),
        prefetchStrategy: TransformingLazyColumnPrefetchStrategy =
            DefaultTransformingLazyColumnPrefetchStrategy()
    ) {
        rule.setContent {
            state =
                rememberTransformingLazyColumnState(
                    initialAnchorItemIndex = anchorItemIndex,
                    initialAnchorItemScrollOffset = anchorItemScrollOffset,
                    prefetchStrategy = prefetchStrategy
                )
            TransformingLazyColumn(
                modifier = Modifier.height(itemsSizeDp * 1.5f),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                state = state,
            ) {
                items(numItems.value) {
                    Spacer(
                        Modifier.height(itemsSizeDp)
                            .background(Color.Red)
                            .fillMaxWidth()
                            .semantics { testTag = "$it" }
                    )
                }
            }
        }
    }

    /** LazyListPrefetchStrategy that just records callbacks without scheduling prefetches. */
    private class RecordingLazyListPrefetchStrategy(
        override val prefetchScheduler: PrefetchScheduler?
    ) : TransformingLazyColumnPrefetchStrategy {

        sealed interface Callback {
            data class OnScroll(val delta: Float, val visibleIndices: List<Int>) : Callback

            data class OnVisibleItemsUpdated(val visibleIndices: List<Int>) : Callback
        }

        private val _callbacks: MutableList<Callback> = mutableListOf()
        val callbacks: List<Callback> = _callbacks

        override fun TransformingLazyColumnPrefetchScope.onScroll(
            delta: Float,
            measureResult: TransformingLazyColumnMeasureResult
        ) {
            _callbacks.add(Callback.OnScroll(delta, measureResult.visibleItems.map { it.index }))
        }

        override fun TransformingLazyColumnPrefetchScope.onVisibleItemsUpdated(
            measureResult: TransformingLazyColumnMeasureResult
        ) {
            _callbacks.add(
                Callback.OnVisibleItemsUpdated(measureResult.visibleItems.map { it.index })
            )
        }

        override fun NestedPrefetchScope.onNestedPrefetch(anchorItemIndex: Int) = Unit

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
    ) : TransformingLazyColumnPrefetchStrategy {

        private var handle: LazyLayoutPrefetchState.PrefetchHandle? = null
        private var prefetchIndex: Int = -1

        override fun TransformingLazyColumnPrefetchScope.onScroll(
            delta: Float,
            measureResult: TransformingLazyColumnMeasureResult
        ) {
            val index = measureResult.visibleItems.last().index + 1
            if (handle != null && index != prefetchIndex) {
                cancelPrefetch()
            }
            handle = schedulePrefetch(index)
            prefetchIndex = index
        }

        override fun TransformingLazyColumnPrefetchScope.onVisibleItemsUpdated(
            measureResult: TransformingLazyColumnMeasureResult
        ) = Unit

        override fun NestedPrefetchScope.onNestedPrefetch(anchorItemIndex: Int) = Unit

        private fun cancelPrefetch() {
            handle?.cancel()
            prefetchIndex = -1
        }
    }
}

@ExperimentalFoundationApi
internal class TestPrefetchScheduler : PrefetchScheduler {

    private var activeRequests = mutableListOf<PrefetchRequest>()

    override fun schedulePrefetch(prefetchRequest: PrefetchRequest) {
        activeRequests.add(prefetchRequest)
    }

    fun executeActiveRequests() {
        while (activeRequests.isNotEmpty()) {
            val request = activeRequests[0]
            val hasMoreWorkToDo = with(request) { scope.execute() }
            if (!hasMoreWorkToDo) activeRequests.removeAt(0)
        }
    }

    private val scope =
        object : PrefetchRequestScope {
            override fun availableTimeNanos(): Long = Long.MAX_VALUE
        }
}
