/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.junit.Rule
import org.junit.Test

@MediumTest
// @RunWith(Parameterized::class)
class LazyScrollTest { // (private val orientation: Orientation)
    @get:Rule
    val rule = createComposeRule()

    private val vertical: Boolean
        get() = true // orientation == Orientation.Vertical

    private val itemsCount = 40
    private lateinit var state: LazyGridState

    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified
    private var containerSizeDp = Dp.Unspecified

    lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
            containerSizeDp = itemSizeDp * 3
        }
    }

    private fun testScroll(
        spacingPx: Int = 0,
        containerSizePx: Int = itemSizePx * 3,
        afterContentPaddingPx: Int = 0,
        assertBlock: suspend () -> Unit
    ) {
        rule.setContent {
            state = rememberLazyGridState()
            scope = rememberCoroutineScope()
            with(rule.density) {
                TestContent(spacingPx.toDp(), containerSizePx.toDp(), afterContentPaddingPx.toDp())
            }
        }
        runBlocking {
            assertBlock()
        }
    }

    @Test
    fun setupWorks() = testScroll {
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
    }

    @Test
    fun scrollToItem() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(2)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(2)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(0)
            state.scrollToItem(3)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(2)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun scrollToItemWithOffset() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(6, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(6)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun scrollToItemWithNegativeOffset() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(6, -10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(4)
        val item6Offset = state.layoutInfo.visibleItemsInfo.first { it.index == 6 }.offset.y
        assertThat(item6Offset).isEqualTo(10)
    }

    @Test
    fun scrollToItemWithPositiveOffsetLargerThanAvailableSize() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(itemsCount - 6, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 6)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0) // not 10
    }

    @Test
    fun scrollToItemWithNegativeOffsetLargerThanAvailableSize() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(1, -(itemSizePx + 10))
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0) // not -10
    }

    @Test
    fun scrollToItemWithIndexLargerThanItemsCount() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(itemsCount + 4)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 6)
    }

    @Test
    fun animateScrollBy() = testScroll {
        val scrollDistance = 320

        val expectedLine = scrollDistance / itemSizePx // resolves to 3
        val expectedItem = expectedLine * 2 // resolves to 6
        val expectedOffset = scrollDistance % itemSizePx // resolves to 20px

        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollBy(scrollDistance.toFloat())
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(expectedItem)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset)
    }

    @Test
    fun animateScrollToItem() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(10, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(10)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItemWithOffset() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(6, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(6)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItemWithNegativeOffset() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(6, -10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(4)
        val item6Offset = state.layoutInfo.visibleItemsInfo.first { it.index == 6 }.offset.y
        assertThat(item6Offset).isEqualTo(10)
    }

    @Test
    fun animateScrollToItemWithPositiveOffsetLargerThanAvailableSize() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(itemsCount - 6, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 6)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0) // not 10
    }

    @Test
    fun animateScrollToItemWithNegativeOffsetLargerThanAvailableSize() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(2, -(itemSizePx + 10))
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0) // not -10
    }

    @Test
    fun animateScrollToItemWithIndexLargerThanItemsCount() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(itemsCount + 2)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 6)
    }

    @Test
    fun animatePerFrameForwardToVisibleItem() = testScroll {
        assertSpringAnimation(toIndex = 4)
    }

    @Test
    fun animatePerFrameForwardToVisibleItemWithOffset() = testScroll {
        assertSpringAnimation(toIndex = 4, toOffset = 35)
    }

    @Test
    fun animatePerFrameForwardToNotVisibleItem() = testScroll {
        assertSpringAnimation(toIndex = 16)
    }

    @Test
    fun animatePerFrameForwardToNotVisibleItemWithOffset() = testScroll {
        assertSpringAnimation(toIndex = 20, toOffset = 35)
    }

    @Test
    fun animatePerFrameBackward() = testScroll {
        assertSpringAnimation(toIndex = 2, fromIndex = 12)
    }

    @Test
    fun animatePerFrameBackwardWithOffset() = testScroll {
        assertSpringAnimation(toIndex = 2, fromIndex = 10, fromOffset = 58)
    }

    @Test
    fun animatePerFrameBackwardWithInitialOffset() = testScroll {
        assertSpringAnimation(toIndex = 0, toOffset = 40, fromIndex = 8)
    }

    @Test
    fun animateScrollToItemWithOffsetLargerThanItemSize_forward() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(10, -itemSizePx * 3)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(4)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun animateScrollToItemWithOffsetLargerThanItemSize_backward() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(10)
            state.animateScrollToItem(0, itemSizePx * 3)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(6)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun canScrollForward() = testScroll {
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        assertThat(state.canScrollForward).isTrue()
        assertThat(state.canScrollBackward).isFalse()
    }

    @Test
    fun canScrollBackward() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(itemsCount)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(itemsCount - 6)
        assertThat(state.canScrollForward).isFalse()
        assertThat(state.canScrollBackward).isTrue()
    }

    @Test
    fun canScrollForwardAndBackward() = testScroll {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(10)
        assertThat(state.canScrollForward).isTrue()
        assertThat(state.canScrollBackward).isTrue()
    }

    @Test
    fun canScrollForwardAndBackward_afterSmallScrollFromStart() = testScroll(
        containerSizePx = (itemSizePx * 1.5f).roundToInt()
    ) {
        val delta = (itemSizePx / 3f).roundToInt()
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            // small enough scroll to not cause any new items to be composed or old ones disposed.
            state.scrollBy(delta.toFloat())
        }
        rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(delta)
            assertThat(state.canScrollForward).isTrue()
            assertThat(state.canScrollBackward).isTrue()
        }
        // and scroll back to start
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollBy(-delta.toFloat())
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isTrue()
            assertThat(state.canScrollBackward).isFalse()
        }
    }

    @Test
    fun canScrollForwardAndBackward_afterSmallScrollFromEnd() = testScroll(
        containerSizePx = (itemSizePx * 1.5f).roundToInt()
    ) {
        val delta = -(itemSizePx / 3f).roundToInt()
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            // scroll to the end of the list.
            state.scrollToItem(itemsCount)
            // small enough scroll to not cause any new items to be composed or old ones disposed.
            state.scrollBy(delta.toFloat())
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isTrue()
            assertThat(state.canScrollBackward).isTrue()
        }
        // and scroll back to the end
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollBy(-delta.toFloat())
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isFalse()
            assertThat(state.canScrollBackward).isTrue()
        }
    }

    @Test
    fun canScrollForwardAndBackward_afterSmallScrollFromEnd_withContentPadding() = testScroll(
        containerSizePx = (itemSizePx * 1.5f).roundToInt(),
        afterContentPaddingPx = 2,
    ) {
        val delta = -(itemSizePx / 3f).roundToInt()
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            // scroll to the end of the list.
            state.scrollToItem(itemsCount)

            assertThat(state.canScrollForward).isFalse()
            assertThat(state.canScrollBackward).isTrue()

            // small enough scroll to not cause any new items to be composed or old ones disposed.
            state.scrollBy(delta.toFloat())
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isTrue()
            assertThat(state.canScrollBackward).isTrue()
        }
        // and scroll back to the end
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollBy(-delta.toFloat())
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isFalse()
            assertThat(state.canScrollBackward).isTrue()
        }
    }

    @Test
    fun animatePerFrameForwardWithSpacing() = testScroll(spacingPx = 10) {
        assertSpringAnimation(toIndex = 16, spacingPx = 10)
    }

    @Test
    fun animatePerFrameForwardWithNegativeSpacing() = testScroll(spacingPx = -10) {
        assertSpringAnimation(toIndex = 16, spacingPx = -10)
    }

    @Test
    fun animatePerFrameBackwardWithSpacing() = testScroll(spacingPx = 10) {
        assertSpringAnimation(toIndex = 2, fromIndex = 12, spacingPx = 10)
    }

    @Test
    fun animatePerFrameBackwardWithNegativeSpacing() = testScroll(spacingPx = -10) {
        assertSpringAnimation(toIndex = 2, fromIndex = 12, spacingPx = -10)
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

        val itemWSpacing = itemSizePx + spacingPx
        val startOffset = (fromIndex / 2 * itemWSpacing + fromOffset).toFloat()
        val endOffset = (toIndex / 2 * itemWSpacing + toOffset).toFloat()
        val spec = FloatSpringSpec()

        val duration =
            TimeUnit.NANOSECONDS.toMillis(spec.getDurationNanos(startOffset, endOffset, 0f))
        rule.mainClock.advanceTimeByFrame()
        var expectedTime = rule.mainClock.currentTime
        for (i in 0..duration step FrameDuration) {
            val nanosTime = TimeUnit.MILLISECONDS.toNanos(i)
            val expectedValue =
                spec.getValueFromNanos(nanosTime, startOffset, endOffset, 0f)
            val actualValue =
                state.firstVisibleItemIndex / 2 * itemWSpacing + state.firstVisibleItemScrollOffset
            assertWithMessage(
                "On animation frame at $i index=${state.firstVisibleItemIndex} " +
                    "offset=${state.firstVisibleItemScrollOffset} expectedValue=$expectedValue"
            ).that(actualValue).isEqualTo(expectedValue.roundToInt(), tolerance = 1)

            rule.mainClock.advanceTimeBy(FrameDuration)
            expectedTime += FrameDuration
            assertThat(expectedTime).isEqualTo(rule.mainClock.currentTime)
            rule.waitForIdle()
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(toIndex)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(toOffset)
    }

    @Composable
    private fun TestContent(spacingDp: Dp, containerSizeDp: Dp, afterContentPaddingDp: Dp) {
        if (vertical) {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.height(containerSizeDp),
                state,
                contentPadding = PaddingValues(bottom = afterContentPaddingDp),
                verticalArrangement = Arrangement.spacedBy(spacingDp)
            ) {
                items(itemsCount) {
                    ItemContent()
                }
            }
        } else {
            // LazyRow(Modifier.width(300.dp), state) {
            //     items(items) {
            //         ItemContent()
            //     }
            // }
        }
    }

    @Composable
    private fun ItemContent() {
        val modifier = if (vertical) {
            Modifier.height(itemSizeDp)
        } else {
            Modifier.width(itemSizeDp)
        }
        Spacer(modifier)
    }

    // companion object {
    //     @JvmStatic
    //     @Parameterized.Parameters(name = "{0}")
    //     fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    // }
}

private val FrameDuration = 16L
