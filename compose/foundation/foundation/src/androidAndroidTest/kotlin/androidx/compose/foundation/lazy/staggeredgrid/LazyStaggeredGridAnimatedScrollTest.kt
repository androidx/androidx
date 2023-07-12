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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.grid.isEqualTo
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridAnimatedScrollTest(
    orientation: Orientation
) : BaseLazyStaggeredGridWithOrientation(orientation) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal,
        )
    }

    internal lateinit var state: LazyStaggeredGridState
    internal lateinit var scope: CoroutineScope

    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified

    @Before
    fun setUp() {
        itemSizeDp = with(rule.density) {
            itemSizePx.toDp()
        }
    }

    private fun testScroll(spacingPx: Int = 0, assertBlock: suspend () -> Unit) {
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            scope = rememberCoroutineScope()
            TestContent(with(rule.density) { spacingPx.toDp() })
        }
        rule.waitForIdle()
        runBlocking {
            assertBlock()
        }
    }

    @Test
    fun animateScrollBy() = testScroll {
        val scrollDistance = 320

        val expectedIndex = scrollDistance * 2 / itemSizePx // resolves to 6
        val expectedOffset = scrollDistance % itemSizePx // resolves to 20px

        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollBy(scrollDistance.toFloat())
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(expectedIndex)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset)
    }

    @Test
    fun animateScrollToItem_positiveOffset() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(10, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(10)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItem_positiveOffset_largerThanItem() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(10, 150)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(12)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(50)
    }

    @Test
    fun animateScrollToItem_negativeOffset() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(10, -10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(8)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(itemSizePx - 10)
    }

    @Test
    fun animateScrollToItem_beforeFirstItem() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(10)
            state.animateScrollToItem(0, -10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun animateScrollToItem_afterLastItem() = testScroll {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(100)
        }
        rule.waitForIdle()
        assertThat(state.firstVisibleItemIndex).isEqualTo(91)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun animateScrollToItem_toFullSpan() = testScroll {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(50, 10)
        }
        rule.waitForIdle()
        assertThat(state.firstVisibleItemIndex).isEqualTo(50)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItem_toFullSpan_andBack() = testScroll {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(50, 10)
        }
        rule.waitForIdle()

        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(45, 0)
        }

        assertThat(state.firstVisibleItemIndex).isEqualTo(44)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun animateScrollToItem_inBounds() = testScroll {
        assertSpringAnimation(2)
    }

    @Test
    fun animateScrollToItem_inBounds_withOffset() = testScroll {
        assertSpringAnimation(2, itemSizePx / 2)
    }

    @Test
    fun animateScrollToItem_outOfBounds() = testScroll {
        assertSpringAnimation(10)
    }

    @Test
    fun animateScrollToItem_firstItem() = testScroll {
        assertSpringAnimation(fromIndex = 10, fromOffset = 10, toIndex = 0)
    }

    @Test
    fun animateScrollToItem_firstItem_toOffset() = testScroll {
        assertSpringAnimation(fromIndex = 10, fromOffset = 10, toIndex = 0, toOffset = 10)
    }

    @Test
    fun animateScrollToItemWithOffsetLargerThanItemSize_forward() = testScroll {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(20, -itemSizePx * 3)
        }
        rule.waitForIdle()
        assertThat(state.firstVisibleItemIndex).isEqualTo(14)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun animateScrollToItemWithOffsetLargerThanItemSize_backward() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(20)
            state.animateScrollToItem(0, itemSizePx * 3)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(6)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun animateScrollToItem_outOfBounds_withSpacing() = testScroll(spacingPx = 10) {
        assertSpringAnimation(20, spacingPx = 10)
    }

    @Test
    fun animateScrollToItem_outOfBounds_withNegativeSpacing() = testScroll(spacingPx = -10) {
        assertSpringAnimation(20, spacingPx = -10)
    }

    @Test
    fun animateScrollToItem_backwards_withSpacing() = testScroll(spacingPx = 10) {
        assertSpringAnimation(toIndex = 0, fromIndex = 20, spacingPx = 10)
    }

    @Test
    fun animateScrollToItem_backwards_withNegativeSpacing() = testScroll(spacingPx = -10) {
        assertSpringAnimation(toIndex = 0, fromIndex = 20, spacingPx = -10)
    }

    private fun assertSpringAnimation(
        toIndex: Int,
        toOffset: Int = 0,
        fromIndex: Int = 0,
        fromOffset: Int = 0,
        spacingPx: Int = 0
    ) {
        if (fromIndex != 0 || fromOffset != 0) {
            rule.runOnIdle {
                runBlocking {
                    state.scrollToItem(fromIndex, fromOffset)
                }
            }
        }
        rule.waitForIdle()

        assertThat(state.firstVisibleItemIndex).isEqualTo(fromIndex)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(fromOffset)

        rule.mainClock.autoAdvance = false

        scope.launch {
            state.animateScrollToItem(toIndex, toOffset)
        }

        while (!state.isScrollInProgress) {
            Thread.sleep(5)
        }

        val itemSizeWSpacing = spacingPx + itemSizePx
        val startOffset = (fromIndex / 2 * itemSizeWSpacing + fromOffset).toFloat()
        val endOffset = (toIndex / 2 * itemSizeWSpacing + toOffset).toFloat()

        val spec = FloatSpringSpec()

        val duration =
            TimeUnit.NANOSECONDS.toMillis(spec.getDurationNanos(startOffset, endOffset, 0f))
        rule.mainClock.advanceTimeByFrame()
        var expectedTime = rule.mainClock.currentTime
        val frameDuration = 16L
        for (i in 0..duration step frameDuration) {
            val nanosTime = TimeUnit.MILLISECONDS.toNanos(i)
            val expectedValue =
                spec.getValueFromNanos(nanosTime, startOffset, endOffset, 0f)
            val actualValue =
                (state.firstVisibleItemIndex / 2 * itemSizeWSpacing +
                    state.firstVisibleItemScrollOffset)
            assertWithMessage(
                "On animation frame at ${i}ms index=${state.firstVisibleItemIndex} " +
                    "offset=${state.firstVisibleItemScrollOffset} expectedValue=$expectedValue"
            ).that(actualValue).isEqualTo(expectedValue.roundToInt(), tolerance = 1)

            rule.mainClock.advanceTimeBy(frameDuration)
            expectedTime += frameDuration
            assertThat(expectedTime).isEqualTo(rule.mainClock.currentTime)
            rule.waitForIdle()
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(toIndex)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(toOffset)
    }

    @Composable
    private fun TestContent(spacingDp: Dp) {
        LazyStaggeredGrid(
            lanes = 2,
            state = state,
            modifier = Modifier.axisSize(itemSizeDp * 2, itemSizeDp * 5),
            mainAxisSpacing = spacingDp
        ) {
            items(
                count = 100,
                span = {
                    // mark a span to check scroll through
                    if (it == 50)
                        StaggeredGridItemSpan.FullLine
                    else
                        StaggeredGridItemSpan.SingleLane
                }
            ) {
                BasicText(
                    "$it",
                    Modifier
                        .mainAxisSize(itemSizeDp)
                        .testTag("$it")
                        .debugBorder()
                )
            }
        }
    }
}