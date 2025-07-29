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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
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
class LazyListCacheWindowTest(orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }

    val itemsSizePx = 30
    val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    lateinit var state: LazyListState
    lateinit var remeasure: Remeasurement

    private val viewportWindow = LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 1.0f)

    @Test
    fun prefetchingForwardInitially() {
        composeList(cacheWindow = viewportWindow)
        rule.waitForIdle()
        // window will fill automatically 1 extra item
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertDoesNotExist()
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        composeList(firstItem = 2, cacheWindow = viewportWindow)
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun smallScrollForward_shouldFillEntireWindow() {
        composeList(cacheWindow = viewportWindow)
        val preFetchIndex = 2
        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        // the viewport fits 1.5 items, we start with indices 0 and 1 visible.
        // index 1 is halfway in the window, we will be able to fit 1 extra item.
        // Since we're moving 5pixels, we can prefetch another item to fill the window.
        rule.onNodeWithTag("$preFetchIndex").assertExists()
        rule.onNodeWithTag("${preFetchIndex + 1}").assertExists()
        rule.onNodeWithTag("${preFetchIndex + 2}").assertDoesNotExist()
    }

    @Test
    fun smallScrollBackwardShouldFillEntireWindow() {
        composeList(firstItem = 3, itemOffset = 10, cacheWindow = viewportWindow)

        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun scrollForward_shouldNotDisposeItemsInWindow() {
        composeList(firstItem = 3, cacheWindow = viewportWindow)
        rule.runOnIdle { runBlocking { state.scrollBy(itemsSizePx * 2.5f) } }
        // Starting on item 3 and moving 2.5 items, we will end up on item 5.
        // This means item 6 will be visible, item 7 and 8 will be in the window.
        // On the other side, items 4 and 3 will be in the window.
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(5) }
        rule.onNodeWithTag("2").assertDoesNotExist()
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("7").assertExists()
        rule.onNodeWithTag("8").assertExists()
        rule.onNodeWithTag("9").assertDoesNotExist()
    }

    @Test
    fun scrollBackward_shouldNotDisposeItemsInWindow() {
        // at first, item 6 is fully visible and item 5 is partially visible
        composeList(firstItem = 6, itemOffset = -itemsSizePx / 2, cacheWindow = viewportWindow)
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()

        // we also filled the window in the forward direction
        rule.onNodeWithTag("7").assertExists()
        rule.onNodeWithTag("8").assertExists()

        rule.runOnIdle { runBlocking { state.scrollBy(-itemsSizePx * 2.5f) } }
        // Moving 2.5 items back, the 0.5 will align item 5 with the start of the layout and the 2
        // will move two entire items, we will end up in item 3. Item 4 will be half visible. We
        // the keep around and prefetch window is 1.5 item, so part of this will be consumed by
        // item 4, and we will keep around item 5. On the other side, we will prefetch item 2 and
        // item 1 to fill up the 1.5 item size quota.
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(3)
            remeasure.forceRemeasure()
        }

        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertIsDisplayed()
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("6").assertDoesNotExist() // at this point we have removed this
        rule.onNodeWithTag("7").assertDoesNotExist() // at this point we have removed this
        rule.onNodeWithTag("8").assertDoesNotExist() // at this point we have removed this
    }

    @Test
    fun datasetChanged_shouldScheduleNewPrefetching_ifWindowIsNotFull() {
        val numItems = mutableStateOf(100)

        composeList(firstItem = 97, numItems = numItems, cacheWindow = viewportWindow)
        rule.onNodeWithTag("97").assertIsDisplayed()
        rule.onNodeWithTag("98").assertIsDisplayed()
        rule.onNodeWithTag("99").assertExists()

        rule.runOnIdle { runBlocking { state.scrollBy(itemsSizePx.toFloat()) } }

        /**
         * At this point item 98 and 99 are visible and item 99 is peeking in the cache window, so
         * there's still space.
         */
        rule.onNodeWithTag("98").assertIsDisplayed()
        rule.onNodeWithTag("99").assertIsDisplayed()
        rule.onNodeWithTag("100").assertDoesNotExist()

        rule.runOnIdle { numItems.value = 200 }
        rule.waitForIdle()

        rule.onNodeWithTag("100").assertExists() // window is not full
    }

    @Test
    fun datasetChanged_shouldNotScheduleNewPrefetching_ifWindowIsFull() {
        val numItems = mutableStateOf(100)

        composeList(firstItem = 96, numItems = numItems, cacheWindow = viewportWindow)
        rule.onNodeWithTag("96").assertIsDisplayed()
        rule.onNodeWithTag("97").assertIsDisplayed()
        rule.onNodeWithTag("97").assertExists()

        rule.runOnIdle { runBlocking { state.scrollBy(itemsSizePx.toFloat()) } }

        /**
         * At this point, item 97 and 98 are visible and item 99 is in the cache window. Since the
         * window fit 1.5 item, there's no more space left because we have 0.5 of item 98 and item
         * 99 fully in the window.
         */
        rule.onNodeWithTag("97").assertIsDisplayed()
        rule.onNodeWithTag("98").assertIsDisplayed()
        rule.onNodeWithTag("99").assertExists() // part of the window
        rule.onNodeWithTag("100").assertDoesNotExist()

        rule.runOnIdle { numItems.value = 200 }
        rule.waitForIdle()

        rule.onNodeWithTag("100").assertDoesNotExist() // window is full
    }

    @Test
    fun datasetChanged_noScrollHappened_shouldKeepAroundWithinBounds_notCrash() {
        val numItems = mutableStateOf(100)

        composeList(firstItem = 96, numItems = numItems, cacheWindow = viewportWindow)
        rule.onNodeWithTag("96").assertIsDisplayed()
        rule.onNodeWithTag("97").assertIsDisplayed()
        rule.onNodeWithTag("97").assertExists()

        rule.runOnIdle { numItems.value = 50 }
        rule.onNodeWithTag("49").assertExists()
        rule.onNodeWithTag("96").assertDoesNotExist()
        rule.onNodeWithTag("97").assertDoesNotExist()
    }

    private val activeNodes = mutableSetOf<Int>()

    private fun composeList(
        firstItem: Int = 0,
        itemOffset: Int = 0,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        numItems: State<Int> = mutableStateOf(100),
        cacheWindow: LazyLayoutCacheWindow,
    ) {
        rule.setContent {
            @OptIn(ExperimentalFoundationApi::class)
            state =
                rememberLazyListState(
                    initialFirstVisibleItemIndex = firstItem,
                    initialFirstVisibleItemScrollOffset = itemOffset,
                    cacheWindow = cacheWindow,
                )
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemsSizeDp * 1.5f)
                    .then(
                        object : RemeasurementModifier {
                            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                                remeasure = remeasurement
                            }
                        }
                    ),
                state,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding,
            ) {
                items(numItems.value) {
                    DisposableEffect(it) {
                        activeNodes.add(it)
                        onDispose { activeNodes.remove(it) }
                    }
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
}
