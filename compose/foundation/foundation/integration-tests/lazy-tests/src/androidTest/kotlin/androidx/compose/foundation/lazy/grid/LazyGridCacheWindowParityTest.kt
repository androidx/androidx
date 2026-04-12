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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") // b/407927787

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.R
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.TestPrefetchScheduler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyGridCacheWindowParityTest(orientation: Orientation) :
    BaseLazyGridTestWithOrientation(orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }

    val itemsSizePx = 30
    val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    private val parityWindow = ParityWindow()
    private val scheduler = TestPrefetchScheduler()

    class ParityWindow() : LazyLayoutCacheWindow {
        var ahead = 0
        var behind = 0

        override fun Density.calculateAheadWindow(viewport: Int): Int = ahead

        override fun Density.calculateBehindWindow(viewport: Int): Int = behind
    }

    lateinit var state: LazyGridState

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun rememberState(
        cacheWindow: LazyLayoutCacheWindow,
        initialFirstVisibleItemIndex: Int = 0,
        initialFirstVisibleItemScrollOffset: Int = 0,
    ): LazyGridState = remember {
        LazyGridState(
            cacheWindow,
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
        )
    }

    @Test
    fun notPrefetchingForwardInitially() {
        composeGrid(cacheWindow = LazyLayoutCacheWindow(ahead = 0.dp))

        rule.onNodeWithTag("4").assertDoesNotExist()
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        composeGrid(cacheWindow = parityWindow, firstItem = 4)
        parityWindow.ahead = itemsSizePx

        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAfterSmallScroll() {
        composeGrid(cacheWindow = parityWindow)
        parityWindow.ahead = itemsSizePx

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch()

        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("6").assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardAfterSmallScroll() {
        composeGrid(cacheWindow = parityWindow, firstItem = 4, itemOffset = 10)
        parityWindow.ahead = itemsSizePx

        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        waitForPrefetch()

        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackward() {
        composeGrid(cacheWindow = parityWindow, firstItem = 2)
        parityWindow.ahead = itemsSizePx

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch()

        rule.onNodeWithTag("6").assertExists()
        rule.onNodeWithTag("7").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-2f)
                state.scrollBy(-1f)
            }
        }

        waitForPrefetch()

        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("6").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardTwice() {
        composeGrid(cacheWindow = parityWindow)
        parityWindow.ahead = itemsSizePx

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemsSizePx / 2f)
                state.scrollBy(itemsSizePx / 2f)
            }
        }

        waitForPrefetch()

        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("6").assertExists()
        rule.onNodeWithTag("8").assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardTwice() {
        composeGrid(cacheWindow = parityWindow, firstItem = 8)
        parityWindow.ahead = itemsSizePx

        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        waitForPrefetch()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-itemsSizePx / 2f)
                state.scrollBy(-itemsSizePx / 2f)
            }
        }

        waitForPrefetch()

        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackwardReverseLayout() {
        composeGrid(cacheWindow = parityWindow, firstItem = 2, reverseLayout = true)
        parityWindow.ahead = itemsSizePx

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch()

        rule.onNodeWithTag("6").assertExists()
        rule.onNodeWithTag("7").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-2f)
                state.scrollBy(-1f)
            }
        }

        waitForPrefetch()

        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("6").assertDoesNotExist()
        rule.onNodeWithTag("7").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackwardWithContentPadding() {
        val halfItemSize = itemsSizeDp / 2f
        composeGrid(
            cacheWindow = parityWindow,
            firstItem = 4,
            itemOffset = 5,
            contentPadding = PaddingValues(mainAxis = halfItemSize),
        )
        parityWindow.ahead = itemsSizePx

        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("8").assertDoesNotExist()

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch()

        rule.onNodeWithTag("8").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()

        rule.runOnIdle { runBlocking { state.scrollBy(-2f) } }

        waitForPrefetch()

        rule.onNodeWithTag("0").assertExists()
    }

    @Test
    fun disposingWhilePrefetchingScheduled() {
        var emit = true
        lateinit var remeasure: Remeasurement
        rule.setContent {
            SubcomposeLayout(
                modifier =
                    object : RemeasurementModifier {
                        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                            remeasure = remeasurement
                        }
                    }
            ) { constraints ->
                val placeable =
                    if (emit) {
                        subcompose(Unit) {
                                state = rememberState(cacheWindow = parityWindow)
                                LazyGrid(2, Modifier.mainAxisSize(itemsSizeDp * 1.5f), state) {
                                    items(1000) { Spacer(Modifier.mainAxisSize(itemsSizeDp)) }
                                }
                            }
                            .first()
                            .measure(constraints)
                    } else {
                        null
                    }
                layout(constraints.maxWidth, constraints.maxHeight) { placeable?.place(0, 0) }
            }
        }

        parityWindow.ahead = itemsSizePx

        rule.runOnIdle {
            // this will schedule the prefetching
            runBlocking(AutoTestFrameClock()) { state.scrollBy(itemsSizePx.toFloat()) }
            // then we synchronously dispose LazyColumn
            emit = false
            remeasure.forceRemeasure()
        }

        rule.runOnIdle {}
    }

    @Test
    fun snappingToOtherPositionWhilePrefetchIsScheduled() {
        val composedItems = mutableListOf<Int>()
        lateinit var remeasure: Remeasurement
        rule.setContent {
            state = rememberState(cacheWindow = parityWindow)
            LazyGrid(
                1,
                Modifier.mainAxisSize(itemsSizeDp * 1.5f)
                    .then(
                        object : RemeasurementModifier {
                            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                                remeasure = remeasurement
                            }
                        }
                    ),
                state,
            ) {
                items(1000) {
                    composedItems.add(it)
                    Spacer(Modifier.mainAxisSize(itemsSizeDp))
                }
            }
        }
        parityWindow.ahead = itemsSizePx

        rule.runOnIdle {
            // now we have items 0 and 1 visible
            runBlocking(AutoTestFrameClock()) {
                // this will move the viewport so items 1 and 2 are visible
                // and schedule a prefetching for 3
                state.scrollBy(itemsSizePx.toFloat())
                // then we move so that items 100 and 101 are visible.
                // this should cancel the prefetch for 3
                state.scrollToItem(100)
            }
        }

        // wait a few frames to make sure prefetch happens if was scheduled
        waitForPrefetch()

        // We updated the window bounds in the last scroll and that didn't generate a measure pass
        // to allow disposing of some items. Here we're forcing a remeasure so we can let go
        // of those items.
        rule.runOnIdle { remeasure.forceRemeasure() }

        rule.runOnIdle { Truth.assertThat(composedItems).doesNotContain(3) }
    }

    @Test
    fun scrollingByListSizeCancelsPreviousPrefetch() {
        composeGrid(cacheWindow = parityWindow)
        parityWindow.ahead = itemsSizePx

        // now we have items 0-3 visible
        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // this will move the viewport so items 2-5 are visible
                // and schedule a prefetching for 6-7
                state.scrollBy(itemsSizePx.toFloat())

                // move viewport by screen size to items 8-11, so item 6 is just behind
                // the first visible item
                state.scrollBy(itemsSizePx * 3f)

                // move scroll further to items 10-13, so item 6 is reused
                state.scrollBy(itemsSizePx.toFloat())
            }
        }

        waitForPrefetch()

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // scroll again to ensure item 6 was dropped
                state.scrollBy(itemsSizePx * 100f)
            }
        }

        rule.runOnIdle { assertThat(activeNodes).doesNotContain(6) }
    }

    private fun waitForPrefetch() {
        rule.runOnUiThread { scheduler.executeActiveRequests() }
    }

    private val activeNodes = mutableSetOf<Int>()

    private fun composeGrid(
        cacheWindow: LazyLayoutCacheWindow,
        firstItem: Int = 0,
        itemOffset: Int = 0,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
    ) {
        rule.setContent {
            LocalView.current.setTag(R.id.compose_prefetch_scheduler, scheduler)
            state =
                rememberState(
                    cacheWindow = cacheWindow,
                    initialFirstVisibleItemIndex = firstItem,
                    initialFirstVisibleItemScrollOffset = itemOffset,
                )
            LazyGrid(
                2,
                Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                state,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding,
            ) {
                items(100) {
                    DisposableEffect(it) {
                        activeNodes.add(it)
                        onDispose { activeNodes.remove(it) }
                    }
                    Spacer(
                        Modifier.mainAxisSize(itemsSizeDp).testTag("$it").layout {
                            measurable,
                            constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                    )
                }
            }
        }
    }
}
