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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ComposeFoundationFlags.isCacheWindowForPagerEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
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
class PagerCacheWindowTest(val config: ParamConfig) : BasePagerTest(config) {

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

    val pagesSizePx = 30
    val pagesSizeDp = with(rule.density) { pagesSizePx.toDp() }
    private val testPrefetchScheduler = TestPrefetchScheduler()

    lateinit var remeasure: Remeasurement

    @BeforeTest
    fun setUp() {
        Assume.assumeTrue(isCacheWindowForPagerEnabled)
    }

    @Test
    fun doNotPrefetchingForwardInitially() {
        createPager(
            modifier = Modifier.size(pagesSizeDp * 1.5f),
            pageSize = { PageSize.Fixed(pagesSizeDp) },
            prefetchScheduler = testPrefetchScheduler,
        )
        waitForPrefetch()
        if (config.beyondViewportPageCount == 0) {
            rule.onNodeWithTag("2").assertDoesNotExist()
        } else {
            rule.onNodeWithTag("3").assertDoesNotExist()
        }
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        createPager(
            modifier = Modifier.size(pagesSizeDp * 1.5f),
            initialPage = 2,
            pageSize = { PageSize.Fixed(pagesSizeDp) },
            prefetchScheduler = testPrefetchScheduler,
        )

        waitForPrefetch()

        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun scrollForward_shouldFillEntireWindow() {
        createPager(
            modifier = Modifier.size(pagesSizeDp * 1.5f),
            pageSize = { PageSize.Fixed(pagesSizeDp) },
            prefetchScheduler = testPrefetchScheduler,
        )

        rule.runOnIdle { runBlocking { pagerState.scrollBy(pagesSizePx.toFloat()) } }

        waitForPrefetch()

        if (config.beyondViewportPageCount == 0) {
            val preFetchIndex = 2
            // Moving 1  whole item will bring item 1 to the start, item 2 will peek and item 3 will
            // be
            // prefetched and fill the window.
            rule.onNodeWithTag("$preFetchIndex").assertIsDisplayed()
            rule.onNodeWithTag("${preFetchIndex + 1}").assertExists()
            rule.onNodeWithTag("${preFetchIndex + 2}").assertDoesNotExist()
        } else {
            val preFetchIndex = 2
            // Moving 1  whole item will bring item 1 to the start, item 2 will peek and item 3 will
            // be
            // prefetched and fill the window.
            rule.onNodeWithTag("$preFetchIndex").assertIsDisplayed()
            rule.onNodeWithTag("${preFetchIndex + 1}").assertExists()
            rule.onNodeWithTag("${preFetchIndex + 2}").assertExists()
            rule.onNodeWithTag("${preFetchIndex + 3}").assertDoesNotExist()
        }
    }

    @Test
    fun smallScrollBackwardShouldFillEntireWindow() {
        createPager(
            modifier = Modifier.size(pagesSizeDp * 1.5f),
            initialPage = 4,
            pageSize = { PageSize.Fixed(pagesSizeDp) },
            prefetchScheduler = testPrefetchScheduler,
        )

        rule.runOnIdle { runBlocking { pagerState.scrollBy(-5f) } }

        waitForPrefetch()

        val firsVisiblePage = pagerState.firstVisiblePage - config.beyondViewportPageCount
        rule.onNodeWithTag("$firsVisiblePage").assertExists()
        rule.onNodeWithTag("${firsVisiblePage - 1}").assertExists()
        rule.onNodeWithTag("${firsVisiblePage - 2}").assertDoesNotExist()
    }

    @Test
    fun scrollForward_shouldNotDisposeItemsInWindow() {
        createPager(
            modifier = Modifier.size(pagesSizeDp * 1.5f),
            initialPage = 3,
            pageSize = { PageSize.Fixed(pagesSizeDp) },
            prefetchScheduler = testPrefetchScheduler,
        )

        rule.runOnIdle { runBlocking { pagerState.scrollBy(pageSize * 2.5f) } }

        // Starting on item 3 and moving 2.5 items, we will end up on item 5.
        // This means item 6 will be visible, item 7 and 8 will be in the window.
        // On the other side, items 4 and 3 will be in the window.
        rule.runOnIdle { assertThat(pagerState.firstVisiblePage).isEqualTo(5) }

        waitForPrefetch()

        rule.onNodeWithTag("2").assertDoesNotExist()
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("7").assertExists()
        if (config.beyondViewportPageCount == 0) {
            rule.onNodeWithTag("9").assertDoesNotExist()
        } else {
            rule.onNodeWithTag("10").assertDoesNotExist()
        }
    }

    @Test
    fun scrollBackward_shouldDisposeItemsInWindow() {
        // at first, item 6 is fully visible and item 5 is partially visible
        createPager(
            modifier =
                Modifier.size(pagesSizeDp * 1.5f)
                    .then(
                        object : RemeasurementModifier {
                            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                                remeasure = remeasurement
                            }
                        }
                    ),
            initialPage = 6,
            initialPageOffsetFraction = -0.5f,
            pageSize = { PageSize.Fixed(pagesSizeDp) },
            prefetchScheduler = testPrefetchScheduler,
        )

        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()

        rule.runOnIdle { runBlocking { pagerState.scrollBy(-pageSize * 2.5f) } }
        // Moving 2.5 items back, the 0.5 will align item 5 with the start of the layout and the 2
        // will move two entire items, we will end up in item 3. Item 4 will be half visible. We
        // the keep around and prefetch window is 1.5 item, so part of this will be consumed by
        // item 4, and we will keep around item 5. On the other side, we will prefetch item 2 and
        // item 1 to fill up the 1.5 item size quota.
        rule.runOnIdle {
            assertThat(pagerState.firstVisiblePage).isEqualTo(3)
            remeasure.forceRemeasure()
        }

        waitForPrefetch()

        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertIsDisplayed()
        rule.onNodeWithTag("4").assertIsDisplayed()

        if (config.beyondViewportPageCount == 0) {
            rule.onNodeWithTag("5").assertDoesNotExist()
        } else {
            rule.onNodeWithTag("5").assertExists()
            rule.onNodeWithTag("6").assertDoesNotExist()
        }
    }

    private fun waitForPrefetch() {
        rule.runOnIdle { testPrefetchScheduler.executeActiveRequests() }
    }
}
