/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.TestPrefetchScheduler
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@RunWith(Parameterized::class)
class LazyStaggeredGridCacheWindowTest(orientation: Orientation) :
    BaseLazyStaggeredGridWithOrientation(orientation) {

    private lateinit var remeasurement: Remeasurement
    private val prefetchScheduler = TestPrefetchScheduler()

    private lateinit var state: LazyStaggeredGridState

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }

    private fun scrollBy(offset: Dp) {
        rule.runOnIdle {
            runBlocking { state.scrollBy(with(rule.density) { offset.roundToPx().toFloat() }) }
            remeasurement.forceRemeasure()
        }
        rule.runOnIdle { prefetchScheduler.executeActiveRequests() }
    }

    private fun composeStaggeredGrid(
        itemCount: State<Int>,
        lanes: State<Int>,
        gridSize: Dp,
        firstItem: Int,
        firstItemOffset: Int,
        cacheWindow: LazyLayoutCacheWindow,
        itemSize: (Int) -> Dp,
    ) {
        state =
            LazyStaggeredGridState(
                initialFirstVisibleItems = intArrayOf(firstItem),
                initialFirstVisibleOffsets = intArrayOf(firstItemOffset),
                prefetchScheduler = prefetchScheduler,
            )
        rule.setContent {
            LazyStaggeredGrid(
                cacheWindow = cacheWindow,
                lanes = lanes.value,
                modifier =
                    Modifier.fillMaxCrossAxis()
                        .mainAxisSize(gridSize)
                        .then(
                            object : RemeasurementModifier {
                                override fun onRemeasurementAvailable(
                                    remeasurement: Remeasurement
                                ) {
                                    this@LazyStaggeredGridCacheWindowTest.remeasurement =
                                        remeasurement
                                }
                            }
                        ),
                state = state,
            ) {
                items(itemCount.value) { index ->
                    val itemColor =
                        Color.hsv(hue = (index * 137.508f) % 360f, saturation = 0.6f, value = 0.9f)
                    val itemSize = itemSize(index)
                    Box(
                        modifier =
                            Modifier.mainAxisSize(itemSize).background(itemColor).testTag("$index")
                    ) {
                        BasicText("$index $itemSize", Modifier.background(Color.White))
                    }
                }
            }
        }
        rule.runOnIdle { prefetchScheduler.executeActiveRequests() }
    }

    @Test
    fun initialPrefetchForward_noOverhang_oneItemPerLane() {
        val itemSize = 100.dp
        composeStaggeredGrid(
            itemCount = mutableStateOf(4),
            gridSize = itemSize,
            cacheWindow = LazyLayoutCacheWindow(ahead = 1.dp),
            itemSize = { itemSize },
            lanes = mutableStateOf(2),
            firstItem = 0,
            firstItemOffset = 0,
        )

        // Visible items:
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()

        // Prefetched items:
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertExists()

        // Out of range items
        rule.onNodeWithTag("4").assertDoesNotExist()
        rule.onNodeWithTag("5").assertDoesNotExist()
    }

    @Test
    fun initialPrefetchForward_noOverhang_multipleItemsPerLane() {
        val itemSize = 100.dp
        composeStaggeredGrid(
            itemCount = mutableStateOf(8),
            gridSize = itemSize,
            cacheWindow = LazyLayoutCacheWindow(ahead = itemSize * 2),
            itemSize = { itemSize },
            lanes = mutableStateOf(2),
            firstItem = 0,
            firstItemOffset = 0,
        )

        // Visible items:
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()

        // Prefetched items:
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("5").assertExists()

        // Out of range items
        rule.onNodeWithTag("6").assertDoesNotExist()
        rule.onNodeWithTag("7").assertDoesNotExist()
    }

    @Test
    fun initialPrefetchForward_overhang_noFetchForward() {
        val itemSize = 100.dp
        composeStaggeredGrid(
            itemCount = mutableStateOf(4),
            gridSize = itemSize / 2,
            cacheWindow = LazyLayoutCacheWindow(1.dp),
            itemSize = { itemSize },
            lanes = mutableStateOf(2),
            firstItem = 0,
            firstItemOffset = 0,
        )

        // Visible items:
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()

        // Out of range items
        rule.onNodeWithTag("2").assertDoesNotExist()
        rule.onNodeWithTag("3").assertDoesNotExist()
    }

    @Test
    fun initialPrefetchForward_overhang_fetchForward() {
        val itemSize = 100.dp
        val gridSize = itemSize / 2
        composeStaggeredGrid(
            itemCount = mutableStateOf(4),
            gridSize = gridSize,
            cacheWindow = LazyLayoutCacheWindow(gridSize + 1.dp),
            itemSize = { itemSize },
            lanes = mutableStateOf(2),
            firstItem = 0,
            firstItemOffset = 0,
        )

        // Visible items:
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()

        // Out of range items
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertExists()
    }

    @Test
    fun staggered_initialPrefetchForward_variesByLane() {
        val itemSizes = listOf(100.dp, 60.dp, 80.dp, 120.dp, 90.dp, 70.dp, 110.dp)
        composeStaggeredGrid(
            cacheWindow = LazyLayoutCacheWindow(ahead = 100.dp, behind = 0.dp),
            itemSize = { index -> itemSizes[index % itemSizes.size] },
            itemCount = mutableStateOf(100),
            lanes = mutableStateOf(2),
            gridSize = 150.dp,
            firstItem = 0,
            firstItemOffset = 0,
        )

        rule.waitForIdle()

        // Visible items
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("4").assertExists()

        // Prefetched items (exist in composition)
        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("6").assertExists()

        // Beyond budget
        rule.onNodeWithTag("7").assertDoesNotExist()
    }

    @Test
    fun staggered_scrollForward_prefetchesCorrectly() {
        val itemSizes =
            listOf(
                100.dp,
                60.dp,
                80.dp,
                120.dp,
                90.dp,
                70.dp,
                110.dp,
                50.dp,
                130.dp,
                80.dp,
                90.dp,
                70.dp,
            )
        composeStaggeredGrid(
            cacheWindow = LazyLayoutCacheWindow(ahead = 100.dp, behind = 80.dp),
            itemSize = { index -> itemSizes[index % itemSizes.size] },
            itemCount = mutableStateOf(100),
            lanes = mutableStateOf(2),
            gridSize = 150.dp,
            firstItem = 0,
            firstItemOffset = 0,
        )

        rule.waitForIdle()

        scrollBy(200.dp)
        rule.waitForIdle()

        // Visible
        rule.onNodeWithTag("3").assertIsDisplayed()
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("7").assertIsDisplayed()
        rule.onNodeWithTag("8").assertIsDisplayed()
        rule.onNodeWithTag("9").assertIsDisplayed()

        // Behind Cache (retained)
        rule.onNodeWithTag("2").assertExists()

        // Behind Cache (disposed)
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertDoesNotExist()

        // Ahead Prefetched
        rule.onNodeWithTag("10").assertExists()

        // Beyond budget
        rule.onNodeWithTag("11").assertDoesNotExist()
    }

    @Test
    fun staggered_scrollBackward_prefetchesCorrectly() {
        val itemSizes =
            listOf(
                100.dp,
                60.dp,
                80.dp,
                120.dp,
                90.dp,
                70.dp,
                110.dp,
                50.dp,
                130.dp,
                80.dp,
                90.dp,
                70.dp,
            )
        composeStaggeredGrid(
            cacheWindow = LazyLayoutCacheWindow(ahead = 100.dp, behind = 80.dp),
            itemSize = { index -> itemSizes[index % itemSizes.size] },
            itemCount = mutableStateOf(100),
            lanes = mutableStateOf(2),
            gridSize = 150.dp,
            firstItem = 0,
            firstItemOffset = 0,
        )

        rule.waitForIdle()

        scrollBy(200.dp)
        rule.waitForIdle()

        scrollBy((-100).dp)

        rule.waitForIdle()

        // Visible
        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("3").assertIsDisplayed()
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()

        // Ahead Prefetched (on start side)
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()

        // Behind Cache (retained on end side)
        rule.onNodeWithTag("7").assertExists()

        // Disposed/Beyond budget
        rule.onNodeWithTag("8").assertDoesNotExist()
        rule.onNodeWithTag("9").assertDoesNotExist()
        rule.onNodeWithTag("10").assertDoesNotExist()
    }

    @Test
    fun staggered_scrollAlternating_retainsAndDisposesCorrectly() {
        val itemSizes =
            listOf(
                100.dp,
                60.dp,
                80.dp,
                120.dp,
                90.dp,
                70.dp,
                110.dp,
                50.dp,
                130.dp,
                80.dp,
                90.dp,
                70.dp,
            )
        composeStaggeredGrid(
            cacheWindow = LazyLayoutCacheWindow(ahead = 100.dp, behind = 80.dp),
            itemSize = { index -> itemSizes[index % itemSizes.size] },
            itemCount = mutableStateOf(100),
            lanes = mutableStateOf(2),
            gridSize = 150.dp,
            firstItem = 0,
            firstItemOffset = 0,
        )

        rule.waitForIdle()

        // Scroll forward
        scrollBy(200.dp)
        rule.waitForIdle()
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()

        // Scroll backward
        scrollBy((-100).dp)
        rule.waitForIdle()
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("7").assertExists()
        rule.onNodeWithTag("8").assertDoesNotExist()

        // Scroll forward again
        scrollBy(100.dp)
        rule.waitForIdle()
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertDoesNotExist()
    }

    @Test
    fun asymmetricCacheWindow_scrollForwardAndBackward() {
        val itemSizes =
            listOf(
                100.dp,
                60.dp,
                80.dp,
                120.dp,
                90.dp,
                70.dp,
                110.dp,
                50.dp,
                130.dp,
                80.dp,
                90.dp,
                70.dp,
            )
        composeStaggeredGrid(
            cacheWindow = LazyLayoutCacheWindow(ahead = 150.dp, behind = 30.dp),
            itemSize = { index -> itemSizes[index % itemSizes.size] },
            itemCount = mutableStateOf(100),
            lanes = mutableStateOf(2),
            gridSize = 150.dp,
            firstItem = 0,
            firstItemOffset = 0,
        )

        rule.waitForIdle()

        scrollBy(200.dp)

        rule.waitForIdle()

        // Behind Cache (all disposed due to small behind window)
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("2").assertDoesNotExist()

        // Ahead Prefetched (deep prefetch due to large ahead window)
        rule.onNodeWithTag("10").assertExists()
        rule.onNodeWithTag("11").assertExists()

        scrollBy((-100).dp)

        rule.waitForIdle()

        // Ahead Prefetched (exists due to deep prefetch)
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()

        // Behind Cache (disposed due to small behind window)
        rule.onNodeWithTag("7").assertDoesNotExist()
        rule.onNodeWithTag("8").assertDoesNotExist()
    }

    @Test
    fun nonInitialEntryPoint_scrollForward_fromMiddle() {
        val itemSize: (Int) -> Dp = { index -> if (index % 2 == 0) 80.dp else 120.dp }

        composeStaggeredGrid(
            firstItem = 50,
            cacheWindow = LazyLayoutCacheWindow(ahead = 100.dp, behind = 100.dp),
            itemSize = itemSize,
            itemCount = mutableStateOf(100),
            lanes = mutableStateOf(2),
            gridSize = 150.dp,
            firstItemOffset = 0,
        )

        rule.waitForIdle()

        scrollBy(50.dp)

        rule.waitForIdle()

        // Visible
        rule.onNodeWithTag("50").assertIsDisplayed()
        rule.onNodeWithTag("51").assertIsDisplayed()
        rule.onNodeWithTag("52").assertIsDisplayed()
        rule.onNodeWithTag("53").assertIsDisplayed()
        rule.onNodeWithTag("54").assertIsDisplayed()

        // Prefetched ahead
        rule.onNodeWithTag("55").assertExists()
        rule.onNodeWithTag("56").assertExists()

        // Beyond budget
        rule.onNodeWithTag("57").assertDoesNotExist()
    }

    @Test
    fun nonInitialEntryPoint_scrollBackward_fromMiddle() {
        val itemSize: (Int) -> Dp = { index -> if (index % 2 == 0) 80.dp else 120.dp }

        composeStaggeredGrid(
            firstItem = 50,
            cacheWindow = LazyLayoutCacheWindow(ahead = 100.dp, behind = 100.dp),
            itemSize = itemSize,
            itemCount = mutableStateOf(100),
            lanes = mutableStateOf(2),
            gridSize = 150.dp,
            firstItemOffset = 0,
        )

        rule.waitForIdle()

        scrollBy((-50).dp)

        rule.waitForIdle()

        // Items scrolled into view and prefetched before the viewport should exist
        rule.onNodeWithTag("49").assertExists()
        rule.onNodeWithTag("48").assertExists()
        rule.onNodeWithTag("47").assertExists()
        rule.onNodeWithTag("46").assertExists()

        // Items far above should not exist
        rule.onNodeWithTag("44").assertDoesNotExist()
    }

    @Test
    fun staggered_datasetChanged_reschedulesCorrectly_andDoesNotCrash() {
        val itemCount = mutableStateOf(10)
        val itemSizes =
            listOf(
                100.dp,
                60.dp,
                80.dp,
                120.dp,
                90.dp,
                70.dp,
                110.dp,
                50.dp,
                130.dp,
                80.dp,
                90.dp,
                70.dp,
            )

        composeStaggeredGrid(
            itemCount = itemCount,
            cacheWindow = LazyLayoutCacheWindow(ahead = 100.dp, behind = 0.dp),
            itemSize = { index -> itemSizes[index] },
            lanes = mutableStateOf(2),
            gridSize = 150.dp,
            firstItem = 0,
            firstItemOffset = 0,
        )

        rule.waitForIdle()

        // Verify initial prefetch
        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("6").assertExists()
        rule.onNodeWithTag("7").assertDoesNotExist()

        // Scroll forward
        scrollBy(100.dp)
        rule.waitForIdle()

        // At 100.dp scroll:
        // Visible: 2, 3, 4, 5, 6
        // Prefetched ahead: 7, 8, 9
        rule.onNodeWithTag("9").assertExists()

        // Dynamically shrink the dataset to 8 (so indices are 0 to 7)
        rule.runOnIdle { itemCount.value = 8 }
        rule.runOnIdle { prefetchScheduler.executeActiveRequests() }
        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()

        // Items 8 and 9 should be disposed and no longer exist in composition
        rule.onNodeWithTag("7").assertExists()
        rule.onNodeWithTag("8").assertDoesNotExist()
        rule.onNodeWithTag("9").assertDoesNotExist()
    }

    @Test
    fun laneCountChanged_resizesArraysAndClearsCache() {
        val lanes = mutableStateOf(2)
        val itemSize = 100.dp

        composeStaggeredGrid(
            itemCount = mutableStateOf(10),
            lanes = lanes,
            gridSize = itemSize,
            cacheWindow = LazyLayoutCacheWindow(ahead = 100.dp, behind = 0.dp),
            itemSize = { itemSize },
            firstItem = 0,
            firstItemOffset = 0,
        )

        rule.waitForIdle()

        // 2 lanes initial check:
        // Visible: 0, 1.
        // Prefetched ahead: 2, 3.
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("4").assertDoesNotExist()

        // Change lane count to 3
        rule.runOnIdle { lanes.value = 3 }
        rule.runOnIdle { prefetchScheduler.executeActiveRequests() }
        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()

        // 3 lanes check (should resize arrays and clear/repopulate cache without crashing):
        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("6").assertDoesNotExist()
    }

    @Test
    fun cacheWindow_hitsBeginningOfLayout() {
        val itemSize = 50.dp
        // 2 lanes, 50.dp item size.
        // Grid size 90.dp, so items 4, 5 (at 100.dp) are completely out of the viewport (which ends
        // at 90.dp).
        composeStaggeredGrid(
            itemCount = mutableStateOf(10),
            lanes = mutableStateOf(2),
            gridSize = 90.dp,
            firstItem = 4, // Viewport starts at item 4 and 5 (100.dp scroll offset)
            firstItemOffset = 0,
            cacheWindow = LazyLayoutCacheWindow(ahead = 100.dp, behind = 0.dp),
            itemSize = { itemSize },
        )

        rule.waitForIdle()

        // Visible: 4, 5, 6, 7
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("7").assertIsDisplayed()

        // Behind: 0, 1, 2, 3 should not exist (behind = 0.dp)
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("2").assertDoesNotExist()
        rule.onNodeWithTag("3").assertDoesNotExist()

        // Scroll backward to the very beginning of the layout
        scrollBy((-100).dp)
        rule.waitForIdle()

        // Now visible: 0, 1, 2, 3
        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsDisplayed()
        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("3").assertIsDisplayed()

        // Items 4, 5 and beyond are outside the viewport and behind window (behind = 0.dp),
        // so they should be disposed.
        rule.onNodeWithTag("4").assertDoesNotExist()
        rule.onNodeWithTag("5").assertDoesNotExist()
    }

    @Test
    fun cacheWindow_hitsEndOfLayout() {
        val itemSize = 50.dp
        // 2 lanes, 50.dp item size.
        // Grid size 90.dp.
        // Total 8 items (indices 0 to 7), total height is 200.dp.
        composeStaggeredGrid(
            itemCount = mutableStateOf(8),
            lanes = mutableStateOf(2),
            gridSize = 90.dp,
            firstItem = 0,
            firstItemOffset = 0,
            cacheWindow = LazyLayoutCacheWindow(ahead = 150.dp, behind = 0.dp),
            itemSize = { itemSize },
        )

        rule.waitForIdle()

        // Visible: 0, 1, 2, 3
        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsDisplayed()
        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("3").assertIsDisplayed()

        // Ahead cache window (150.dp) covers up to 240.dp from start.
        // Items 4, 5 (100-150dp) and 6, 7 (150-200dp) should be prefetched and exist.
        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("6").assertExists()
        rule.onNodeWithTag("7").assertExists()

        // Scroll forward completely past items 0-3 (which end at 100.dp).
        // Scrolling by 110.dp ensures that the viewport starts at 110.dp, so items 0-3
        // are completely out of the viewport.
        scrollBy(110.dp)
        rule.waitForIdle()

        // Now visible: 4, 5, 6, 7
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("7").assertIsDisplayed()

        // Items 0, 1, 2, 3 are behind the viewport and behind window is 0.dp,
        // so they should be disposed.
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("2").assertDoesNotExist()
        rule.onNodeWithTag("3").assertDoesNotExist()
    }
}
