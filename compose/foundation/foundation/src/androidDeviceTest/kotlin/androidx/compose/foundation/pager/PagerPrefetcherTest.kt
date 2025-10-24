/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.foundation.ComposeFoundationFlags.isCacheWindowForPagerEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.BeforeTest
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerPrefetcherTest(private val paramConfig: ParamConfig) : BasePagerTest(paramConfig) {

    var pageSizePx = 300
    val pageSizeDp = with(rule.density) { pageSizePx.toDp() }
    var touchSlope: Float = 0.0f
    private val scheduler = TestPrefetchScheduler()

    @BeforeTest
    fun setUp() {
        Assume.assumeFalse(isCacheWindowForPagerEnabled)
    }

    @Test
    fun notPrefetchingForwardInitially() {
        composePager()

        rule.onNodeWithTag("${paramConfig.beyondViewportPageCount + 2}").assertDoesNotExist()
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        composePager(initialPage = 2)

        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAfterSmallScroll_programmatically() {
        composePager()
        val preFetchIndex = 2
        rule.runOnIdle { runBlocking { pagerState.scrollBy(5f) } }

        waitForPrefetch()

        rule.onNodeWithTag("$preFetchIndex").assertExists()
        rule
            .onNodeWithTag("${paramConfig.beyondViewportPageCount + preFetchIndex + 1}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardAfterSmallScroll_programmatically() {
        composePager(initialPage = 5, initialPageOffsetFraction = 10 / pageSizePx.toFloat())

        val preFetchIndex = 4
        rule.runOnIdle { runBlocking { pagerState.scrollBy(-5f) } }

        waitForPrefetch()

        rule.onNodeWithTag("$preFetchIndex").assertExists()
        rule
            .onNodeWithTag("${preFetchIndex - paramConfig.beyondViewportPageCount - 1}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAfterSmallScroll_withGesture() {
        composePager()
        val preFetchIndex = 2
        val delta = (touchSlope + 5) * scrollForwardSign

        onPager().performTouchInput {
            down(center)
            if (vertical) {
                moveBy(Offset(x = 0f, y = delta))
            } else {
                moveBy(Offset(x = delta, y = 0f))
            }
            up()
        }

        waitForPrefetch()

        rule.onNodeWithTag("$preFetchIndex").assertExists()
        rule
            .onNodeWithTag("${paramConfig.beyondViewportPageCount + preFetchIndex + 1}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardAfterSmallScroll_withGesture() {
        composePager(initialPage = 5, initialPageOffsetFraction = 10 / pageSizePx.toFloat())

        val preFetchIndex = 4
        val delta = (touchSlope + 5) * -1 * scrollForwardSign

        onPager().performTouchInput {
            down(center)
            if (vertical) {
                moveBy(Offset(x = 0f, y = delta))
            } else {
                moveBy(Offset(x = delta, y = 0f))
            }
            up()
        }

        waitForPrefetch()

        rule.onNodeWithTag("$preFetchIndex").assertExists()
        rule
            .onNodeWithTag("${preFetchIndex - paramConfig.beyondViewportPageCount - 1}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackward() {
        val initialIndex = 5
        composePager(initialPage = initialIndex)

        rule.runOnIdle { runBlocking { pagerState.scrollBy(5f) } }
        var prefetchIndex = initialIndex + 2
        waitForPrefetch()

        rule.onNodeWithTag("$prefetchIndex").assertExists()
        rule
            .onNodeWithTag("${prefetchIndex - paramConfig.beyondViewportPageCount - 3}")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                pagerState.scrollBy(-2f)
                pagerState.scrollBy(-1f)
            }
        }

        prefetchIndex -= 3
        waitForPrefetch()

        rule.onNodeWithTag("$prefetchIndex").assertExists()
        rule
            .onNodeWithTag("${prefetchIndex + paramConfig.beyondViewportPageCount + 3}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardTwice() {
        composePager()

        rule.runOnIdle { runBlocking { pagerState.scrollBy(5f) } }

        waitForPrefetch()

        rule.runOnIdle {
            runBlocking {
                pagerState.scrollBy(pageSizePx / 2f)
                pagerState.scrollBy(pageSizePx / 2f)
            }
        }

        val prefetchIndex = 3

        waitForPrefetch()

        rule.onNodeWithTag("${prefetchIndex - 1}").assertIsDisplayed()
        rule.onNodeWithTag("$prefetchIndex").assertExists()
        rule
            .onNodeWithTag("${prefetchIndex + paramConfig.beyondViewportPageCount + 1}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardTwice() {
        composePager(initialPage = 5)

        val preFetchIndex = 3

        rule.runOnIdle { runBlocking { pagerState.scrollBy(-5f) } }

        waitForPrefetch()

        rule.runOnIdle {
            runBlocking {
                pagerState.scrollBy(-pageSizePx / 2f)
                pagerState.scrollBy(-pageSizePx / 2f)
            }
        }

        waitForPrefetch()

        rule.onNodeWithTag("$preFetchIndex").assertIsDisplayed()
        rule.onNodeWithTag("${preFetchIndex - 1}").assertExists()
        rule
            .onNodeWithTag("${preFetchIndex - 1 - paramConfig.beyondViewportPageCount - 1}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackwardReverseLayout() {
        val initialIndex = 5
        composePager(initialPage = initialIndex, reverseLayout = true)

        rule.runOnIdle { runBlocking { pagerState.scrollBy(5f) } }

        var prefetchIndex = initialIndex + 2

        waitForPrefetch()

        rule.onNodeWithTag("$prefetchIndex").assertExists()
        rule
            .onNodeWithTag("${prefetchIndex - paramConfig.beyondViewportPageCount - 3}")
            .assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                pagerState.scrollBy(-2f)
                pagerState.scrollBy(-1f)
            }
        }

        prefetchIndex -= 3
        waitForPrefetch()

        rule.onNodeWithTag("$prefetchIndex").assertExists()
        rule
            .onNodeWithTag("${prefetchIndex + paramConfig.beyondViewportPageCount + 3}")
            .assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackwardWithContentPadding() {
        val halfItemSize = pageSizeDp / 2f
        val initialIndex = 5
        composePager(
            initialPage = initialIndex,
            initialPageOffsetFraction = 5 / pageSizePx.toFloat(),
            contentPadding = PaddingValues(mainAxis = halfItemSize),
        )

        rule.onNodeWithTag("${initialIndex - 1}").assertIsDisplayed()
        rule.onNodeWithTag("$initialIndex").assertIsDisplayed()
        rule.onNodeWithTag("${initialIndex + 1}").assertIsDisplayed()
        rule
            .onNodeWithTag("${initialIndex - paramConfig.beyondViewportPageCount - 2}")
            .assertDoesNotExist()
        rule
            .onNodeWithTag("${initialIndex + paramConfig.beyondViewportPageCount + 2}")
            .assertDoesNotExist()

        rule.runOnIdle { runBlocking { pagerState.scrollBy(5f) } }

        var prefetchIndex = initialIndex + 1
        waitForPrefetch()

        rule.onNodeWithTag("${prefetchIndex + 1}").assertExists()
        rule
            .onNodeWithTag("${prefetchIndex - paramConfig.beyondViewportPageCount - 3}")
            .assertDoesNotExist()

        rule.runOnIdle { runBlocking { pagerState.scrollBy(-2f) } }

        prefetchIndex -= 3
        waitForPrefetch()

        rule.onNodeWithTag("$prefetchIndex").assertExists()
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
                                pagerState = rememberPagerState { 1000 }
                                HorizontalOrVerticalPager(
                                    modifier = Modifier.mainAxisSize(pageSizeDp * 1.5f),
                                    state = pagerState,
                                ) {
                                    Spacer(
                                        Modifier.mainAxisSize(pageSizeDp)
                                            .then(
                                                if (vertical) Modifier.fillMaxWidth()
                                                else Modifier.fillMaxHeight()
                                            )
                                    )
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

        rule.runOnIdle {
            // this will schedule the prefetching
            runBlocking(AutoTestFrameClock()) { pagerState.scrollBy(pageSize.toFloat()) }
            // then we synchronously dispose LazyColumn
            emit = false
            remeasure.forceRemeasure()
        }

        rule.waitForIdle()
    }

    @Test
    fun snappingToOtherPositionWhilePrefetchIsScheduled() {
        val composedItems = mutableListOf<Int>()
        rule.setContent {
            pagerState = rememberPagerState { 1000 }
            HorizontalOrVerticalPager(
                modifier = Modifier.mainAxisSize(pageSizeDp * 1.5f),
                state = pagerState,
            ) {
                composedItems.add(it)
                Spacer(
                    Modifier.mainAxisSize(pageSizeDp)
                        .then(if (vertical) Modifier.fillMaxWidth() else Modifier.fillMaxHeight())
                )
            }
        }

        rule.runOnIdle {
            // now we have pages 0 and 1 visible
            runBlocking(AutoTestFrameClock()) {
                // this will move the viewport so pages 1 and 2 are visible
                // and schedule a prefetching for 3
                pagerState.scrollBy(pageSize.toFloat())
                // then we move so that pages 100 and 101 are visible.
                // this should cancel the prefetch for 3
                pagerState.scrollToPage(100)
            }
        }

        // wait a few frames to make sure prefetch happens if was scheduled
        rule.waitForIdle()
        rule.waitForIdle()
        rule.waitForIdle()

        rule.runOnIdle { assertThat(composedItems).doesNotContain(3) }
    }

    @Test
    fun scrollingByListSizeCancelsPreviousPrefetch() {
        composePager()

        // now we have pages 0-1 visible
        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // this will move the viewport so pages 1-2 are visible
                // and schedule a prefetching for 3
                pagerState.scrollBy(pageSizePx.toFloat())

                // move viewport by screen size to pages 4-5, so page 3 is just behind
                // the first visible page
                pagerState.scrollBy(pageSizePx * 3f)

                // move scroll further to pages 5-6, so page 3 is reused
                pagerState.scrollBy(pageSizePx.toFloat())
            }
        }

        waitForPrefetch()

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // scroll again to ensure page 3 was dropped
                pagerState.scrollBy(pageSizePx * 100f)
            }
        }

        rule.runOnIdle { assertThat(activeNodes).doesNotContain("3") }
    }

    @Test
    fun overflowFromLargePageCountDoesNotPrefetchStartPages() {
        composePager(pageCount = Int.MAX_VALUE, initialPage = Int.MAX_VALUE - 3)

        rule.runOnIdle { runBlocking { pagerState.scrollBy(5f) } }

        waitForPrefetch()

        rule.onNodeWithTag("${Int.MAX_VALUE - 1}").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertDoesNotExist()
    }

    private suspend fun PagerState.scrollBy(delta: Float): Float {
        val consumed = (this as ScrollableState).scrollBy(delta)
        scroll {} // cancel fling animation
        return consumed
    }

    private fun waitForPrefetch() {
        rule.runOnIdle { scheduler.executeActiveRequests() }
    }

    private fun composePager(
        pageCount: Int = 100,
        initialPage: Int = 0,
        initialPageOffsetFraction: Float = 0f,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
    ) {
        createPager(
            modifier = Modifier.mainAxisSize(pageSizeDp * 1.5f),
            reverseLayout = reverseLayout,
            contentPadding = contentPadding,
            beyondViewportPageCount = paramConfig.beyondViewportPageCount,
            initialPage = initialPage,
            initialPageOffsetFraction = initialPageOffsetFraction,
            prefetchScheduler = scheduler,
            pageCount = { pageCount },
            pageSize = {
                object : PageSize {
                    override fun Density.calculateMainAxisPageSize(
                        availableSpace: Int,
                        pageSpacing: Int,
                    ): Int {
                        return pageSizePx
                    }
                }
            },
        ) {
            touchSlope = LocalViewConfiguration.current.touchSlop
            DisposableEffect(it) {
                activeNodes.add(it.toString())
                onDispose { activeNodes.remove(it.toString()) }
            }

            Spacer(
                Modifier.mainAxisSize(pageSizeDp).fillMaxCrossAxis().testTag("$it").layout {
                    measurable,
                    constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Array<Any> =
            arrayOf(
                ParamConfig(Orientation.Vertical, beyondViewportPageCount = 0),
                ParamConfig(Orientation.Vertical, beyondViewportPageCount = 1),
                ParamConfig(Orientation.Horizontal, beyondViewportPageCount = 0),
                ParamConfig(Orientation.Horizontal, beyondViewportPageCount = 1),
            )
    }
}
