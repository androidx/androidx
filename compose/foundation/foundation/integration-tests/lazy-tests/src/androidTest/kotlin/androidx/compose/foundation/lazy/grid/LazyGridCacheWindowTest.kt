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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
class LazyGridCacheWindowTest(orientation: Orientation) :
    BaseLazyGridTestWithOrientation(orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }

    val itemsSizePx = 30
    val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    lateinit var state: LazyGridState
    lateinit var remeasure: Remeasurement

    private val viewportWindow = LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 1f)

    @Test
    fun prefetchingForwardInitially() {
        composeGrid(cacheWindow = viewportWindow)
        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("6").assertDoesNotExist()
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        composeGrid(firstItem = 3, cacheWindow = viewportWindow)
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun smallScrollForward_shouldFillEntireWindow() {
        composeGrid(cacheWindow = viewportWindow)
        val preFetchIndex = 4
        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        // the viewport fits 1.5 lines, we start with indices 0, 1, 2 and 3 visible.
        // line 1 is halfway in the window, we will be able to fit 1 extra line.
        // Since we're moving 5pixels, we can prefetch another line to fill the window.
        rule.onNodeWithTag("$preFetchIndex").assertExists()
        rule.onNodeWithTag("${preFetchIndex + 1}").assertExists()
        rule.onNodeWithTag("${preFetchIndex + 2}").assertExists()
        rule.onNodeWithTag("${preFetchIndex + 3}").assertExists()
        rule.onNodeWithTag("${preFetchIndex + 4}").assertDoesNotExist()
        rule.onNodeWithTag("${preFetchIndex + 5}").assertDoesNotExist()
    }

    @Test
    fun smallScrollBackwardShouldFillEntireWindow() {
        composeGrid(firstItem = 6, itemOffset = 10, cacheWindow = viewportWindow)

        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun scrollForward_shouldNotDisposeItemsInWindow() {
        composeGrid(firstItem = 6, cacheWindow = viewportWindow)
        rule.runOnIdle { runBlocking { state.scrollBy(itemsSizePx * 2.5f) } }
        // Starting on line 3 and moving 2.5 lines, we will end up on line 5.
        // This means item 10-13 will be visible, item 14-17 will be in the window.
        // On the other side, items 6-9 will be in the window.
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(10) }
        rule.onNodeWithTag("4").assertDoesNotExist()
        rule.onNodeWithTag("5").assertDoesNotExist()
        rule.onNodeWithTag("6").assertExists()
        rule.onNodeWithTag("7").assertExists()
        rule.onNodeWithTag("8").assertExists()
        rule.onNodeWithTag("9").assertExists()
        rule.onNodeWithTag("10").assertIsDisplayed()
        rule.onNodeWithTag("11").assertIsDisplayed()
        rule.onNodeWithTag("12").assertIsDisplayed()
        rule.onNodeWithTag("13").assertIsDisplayed()
        rule.onNodeWithTag("14").assertExists()
        rule.onNodeWithTag("15").assertExists()
        rule.onNodeWithTag("16").assertExists()
        rule.onNodeWithTag("17").assertExists()
        rule.onNodeWithTag("18").assertDoesNotExist()
        rule.onNodeWithTag("19").assertDoesNotExist()
    }

    @Test
    fun scrollBackward_shouldNotDisposeItemsInWindow() {
        composeGrid(firstItem = 12, itemOffset = -itemsSizePx / 2, cacheWindow = viewportWindow)
        // We start at item 12 offset by half a line, we have 2 visible lines and first measure
        // prefetch will create  1 additional line of items
        rule.onNodeWithTag("10").assertIsDisplayed()
        rule.onNodeWithTag("11").assertIsDisplayed()
        rule.onNodeWithTag("12").assertIsDisplayed()
        rule.onNodeWithTag("13").assertIsDisplayed()
        rule.onNodeWithTag("14").assertExists()
        rule.onNodeWithTag("15").assertExists()

        rule.runOnIdle { runBlocking { state.scrollBy(-itemsSizePx * 2.5f) } }
        // We will scroll 2.5 line sizes back. This means that we will line up item 10 and then move
        // two lines to item 6 (4 items). Now item 6-9 will be visible. We will prefetch 2 lines
        // forward, items 2-5. We will keep around 1 line of items, 10 an 12.
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(6)
            remeasure.forceRemeasure()
        }

        rule.onNodeWithTag("0").assertDoesNotExist() // line 0
        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("2").assertExists() // line 1
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("4").assertExists() // line 2
        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("6").assertIsDisplayed() // line 3
        rule.onNodeWithTag("7").assertIsDisplayed()
        rule.onNodeWithTag("8").assertIsDisplayed() // line 4
        rule.onNodeWithTag("9").assertIsDisplayed()
        rule.onNodeWithTag("10").assertExists() // line 5
        rule.onNodeWithTag("11").assertExists()
        rule.onNodeWithTag("12").assertDoesNotExist() // line 6
        rule.onNodeWithTag("13").assertDoesNotExist()
    }

    @Test
    fun datasetChanged_shouldScheduleNewPrefetching_ifWindowIsNotFull() {
        val numItems = mutableStateOf(100)

        composeGrid(firstItem = 94, numItems = numItems, cacheWindow = viewportWindow)
        rule.onNodeWithTag("94").assertIsDisplayed()
        rule.onNodeWithTag("95").assertIsDisplayed()
        rule.onNodeWithTag("96").assertIsDisplayed()
        rule.onNodeWithTag("97").assertIsDisplayed()
        rule.onNodeWithTag("98").assertExists()
        rule.onNodeWithTag("99").assertExists()

        rule.runOnIdle { runBlocking { state.scrollBy(itemsSizePx.toFloat()) } }

        /**
         * At this point items 96-99 are on the visible window there is still space left on the
         * window.
         */
        rule.onNodeWithTag("98").assertIsDisplayed()
        rule.onNodeWithTag("99").assertIsDisplayed()
        rule.onNodeWithTag("100").assertDoesNotExist()

        rule.runOnIdle { numItems.value = 200 }
        rule.waitForIdle()

        rule.onNodeWithTag("100").assertExists() // window is not full
        rule.onNodeWithTag("101").assertExists() // window is not full
    }

    @Test
    fun datasetChanged_shouldNotScheduleNewPrefetching_ifWindowIsFull() {
        val numItems = mutableStateOf(100)

        composeGrid(firstItem = 92, numItems = numItems, cacheWindow = viewportWindow)
        rule.onNodeWithTag("92").assertIsDisplayed()
        rule.onNodeWithTag("93").assertIsDisplayed()
        rule.onNodeWithTag("94").assertIsDisplayed()
        rule.onNodeWithTag("95").assertIsDisplayed()

        rule.runOnIdle { runBlocking { state.scrollBy(itemsSizePx.toFloat()) } }

        /**
         * At this point items 94-97 are on the visible window and 98-99 are in the cache window.
         * The window is full because items 97-99 fill the window.
         */
        rule.onNodeWithTag("94").assertIsDisplayed()
        rule.onNodeWithTag("95").assertIsDisplayed()
        rule.onNodeWithTag("96").assertIsDisplayed()
        rule.onNodeWithTag("97").assertIsDisplayed()
        rule.onNodeWithTag("98").assertExists() // part of the window
        rule.onNodeWithTag("99").assertExists() // part of the window
        rule.onNodeWithTag("100").assertDoesNotExist()
        rule.onNodeWithTag("101").assertDoesNotExist()

        rule.runOnIdle { numItems.value = 200 }
        rule.waitForIdle()

        rule.onNodeWithTag("100").assertDoesNotExist() // window is full
        rule.onNodeWithTag("101").assertDoesNotExist() // window is full
    }

    private val activeNodes = mutableSetOf<Int>()

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

    private fun composeGrid(
        cacheWindow: LazyLayoutCacheWindow,
        firstItem: Int = 0,
        itemOffset: Int = 0,
        reverseLayout: Boolean = false,
        numItems: State<Int> = mutableStateOf(100),
        contentPadding: PaddingValues = PaddingValues(0.dp),
    ) {
        rule.setContent {
            state =
                rememberState(
                    cacheWindow = cacheWindow,
                    initialFirstVisibleItemIndex = firstItem,
                    initialFirstVisibleItemScrollOffset = itemOffset,
                )
            LazyGrid(
                2,
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
