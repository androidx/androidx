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

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridScrollTest(private val orientation: Orientation) :
    BaseLazyStaggeredGridWithOrientation(orientation) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> =
            arrayOf(
                Orientation.Vertical,
                Orientation.Horizontal,
            )
    }

    internal lateinit var state: LazyStaggeredGridState

    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified
    private val itemCount = 100

    @Before
    fun initSizes() {
        itemSizeDp = with(rule.density) { itemSizePx.toDp() }
    }

    fun setContent(
        containerSizePx: Int = itemSizePx * 5,
        beforeContentPaddingPx: Int = 0,
        afterContentPaddingPx: Int = 0
    ) {
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            with(rule.density) {
                TestContent(
                    containerSizePx.toDp(),
                    beforeContentPaddingPx.toDp(),
                    afterContentPaddingPx.toDp()
                )
            }
        }
    }

    @Test
    fun setupWorks() {
        setContent()

        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

        rule.onNodeWithTag("0").assertIsDisplayed()
    }

    @Test
    fun scrollToItem_byIndexAndOffset_outsideBounds() {
        setContent()
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) { state.scrollToItem(10, 10) }
        assertThat(state.firstVisibleItemIndex).isEqualTo(10)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun scrollToItem_byIndexAndOffset_inBounds() {
        setContent()
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) { state.scrollToItem(2, 10) }
        assertThat(state.firstVisibleItemIndex).isEqualTo(1)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(110)
    }

    @Test
    fun scrollToItem_byIndexAndOffset_inBounds_secondLane() {
        setContent()
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) { state.scrollToItem(4, 10) }

        assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Test
    fun scrollToItem_byIndexAndNegativeOffset() {
        setContent()
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) { state.scrollToItem(4, -10) }

        assertThat(state.firstVisibleItemIndex).isEqualTo(1)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(itemSizePx * 2 - 10)
    }

    @Test
    fun scrollToItem_offsetLargerThanItem() {
        setContent()
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(10, itemSizePx * 2)
        }

        assertThat(state.firstVisibleItemIndex).isEqualTo(13)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun scrollToItem_beyondFirstItem() {
        setContent()
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(10)
            state.scrollToItem(0, -10)
        }

        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun scrollToItem_beyondLastItem() {
        setContent()
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) {
            state.scrollToItem(99, itemSizePx * 3)
        }

        val lastItem = state.layoutInfo.visibleItemsInfo.last()
        assertThat(lastItem.index).isEqualTo(99)
        val mainAxisOffset =
            if (orientation == Orientation.Vertical) {
                lastItem.offset.y
            } else {
                lastItem.offset.x
            }
        assertThat(mainAxisOffset).isEqualTo(itemSizePx * 3) // x5 (grid) - x2 (item)
    }

    @Test
    fun scrollToItem_beyondItemCount() {
        setContent()
        runBlocking(AutoTestFrameClock() + Dispatchers.Main) { state.scrollToItem(420) }

        val lastItem = state.layoutInfo.visibleItemsInfo.last()
        assertThat(lastItem.index).isEqualTo(99)
        val mainAxisOffset =
            if (orientation == Orientation.Vertical) {
                lastItem.offset.y
            } else {
                lastItem.offset.x
            }
        assertThat(mainAxisOffset).isEqualTo(itemSizePx * 3) // x5 (grid) - x2 (item)
    }

    @Test
    fun canScrollForward() {
        setContent()
        runBlocking {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.canScrollForward).isTrue()
            assertThat(state.canScrollBackward).isFalse()
        }
    }

    @Test
    fun canScrollBackward() {
        setContent()
        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) { state.scrollToItem(99) }
            val lastItem = state.layoutInfo.visibleItemsInfo.last()
            val mainAxisOffset =
                if (orientation == Orientation.Vertical) {
                    lastItem.offset.y
                } else {
                    lastItem.offset.x
                }
            assertThat(mainAxisOffset).isEqualTo(itemSizePx * 3) // x5 (grid) - x2 (item)
            assertThat(state.canScrollForward).isFalse()
            assertThat(state.canScrollBackward).isTrue()
        }
    }

    @Test
    fun canScrollForwardAndBackward() {
        setContent()
        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) { state.scrollToItem(10) }
            assertThat(state.firstVisibleItemIndex).isEqualTo(10)
            assertThat(state.canScrollForward).isTrue()
            assertThat(state.canScrollBackward).isTrue()
        }
    }

    @Test
    fun scrollToItem_fullSpan() {
        setContent()
        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) { state.scrollToItem(49, 10) }

            assertThat(state.firstVisibleItemIndex).isEqualTo(49)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
        }
    }

    @Test
    fun canScrollForwardAndBackward_afterSmallScrollFromStart() {
        setContent(containerSizePx = (itemSizePx * 1.5f).roundToInt())
        val delta = (itemSizePx / 3f).roundToInt()
        rule.runOnIdle {
            runBlocking {
                withContext(AutoTestFrameClock()) {
                    // small enough scroll to not cause any new items to be composed or old ones
                    // disposed.
                    state.scrollBy(delta.toFloat())
                }
            }
        }
        rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(delta)
            assertThat(state.canScrollForward).isTrue()
            assertThat(state.canScrollBackward).isTrue()
        }
        rule.runOnIdle {
            runBlocking {
                withContext(AutoTestFrameClock()) {
                    // and scroll back to start
                    state.scrollBy(-delta.toFloat())
                }
            }
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isTrue()
            assertThat(state.canScrollBackward).isFalse()
        }
    }

    @Test
    fun canScrollForwardAndBackward_afterSmallScrollFromEnd() {
        setContent(containerSizePx = (itemSizePx * 2.5f).roundToInt())

        val delta = -(itemSizePx / 3f).roundToInt()
        rule.runOnIdle {
            runBlocking {
                withContext(AutoTestFrameClock()) {
                    // scroll to the end of the list.
                    state.scrollToItem(itemCount)
                }
            }
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isFalse()
            assertThat(state.canScrollBackward).isTrue()
        }
        rule.runOnIdle {
            runBlocking {
                withContext(AutoTestFrameClock()) {
                    // small enough scroll to not cause any new items to be composed or old ones
                    // disposed.
                    state.scrollBy(delta.toFloat())
                }
            }
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isTrue()
            assertThat(state.canScrollBackward).isTrue()
        }
        rule.runOnIdle {
            runBlocking {
                // and scroll back to the end
                withContext(AutoTestFrameClock()) { state.scrollBy(-delta.toFloat()) }
            }
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isFalse()
            assertThat(state.canScrollBackward).isTrue()
        }
    }

    @Test
    fun canScrollForwardAndBackward_afterSmallScrollFromEnd_withContentPadding() {
        setContent(
            containerSizePx = (itemSizePx * 2.5f).roundToInt(),
            afterContentPaddingPx = 2,
        )
        val delta = -(itemSizePx / 3f).roundToInt()
        rule.runOnIdle {
            runBlocking {
                withContext(AutoTestFrameClock()) {
                    // scroll to the end of the list.
                    state.scrollToItem(itemCount)

                    assertThat(state.canScrollForward).isFalse()
                    assertThat(state.canScrollBackward).isTrue()

                    // small enough scroll to not cause any new items to be composed or old ones
                    // disposed.
                    state.scrollBy(delta.toFloat())
                }
            }
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isTrue()
            assertThat(state.canScrollBackward).isTrue()
        }
        rule.runOnIdle {
            runBlocking {
                withContext(AutoTestFrameClock()) {
                    // and scroll back to the end
                    state.scrollBy(-delta.toFloat())
                }
            }
        }
        rule.runOnIdle {
            assertThat(state.canScrollForward).isFalse()
            assertThat(state.canScrollBackward).isTrue()
        }
    }

    @Test
    fun scrollBy_emptyItemWithSpacing() {
        val state = LazyStaggeredGridState()
        val spacingDp = with(rule.density) { 10.toDp() }
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 1,
                state = state,
                mainAxisSpacing = spacingDp,
                modifier = Modifier.size(itemSizeDp),
            ) {
                item { Box(Modifier.size(itemSizeDp).testTag("0").debugBorder()) }
                item {} // empty item surrounded by spacings
                item { Box(Modifier.size(itemSizeDp).testTag("2").debugBorder()) }
            }
        }
        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // empty item introduces two spacings 10 pixels each
                // so after this operation item 2 is right at the edge of the viewport
                state.scrollBy(20f)
                // then we do some extra scrolling to make item 2 visible
                state.scrollBy(20f)
            }
        }
        rule
            .onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(
                itemSizeDp - with(rule.density) { 20.toDp() }
            )
    }

    @Test
    fun overScrollingBackShouldIgnoreBeforeContentPadding() {
        setContent(beforeContentPaddingPx = 5)

        val floatItemSize = itemSizePx.toFloat()
        var consumed: Float
        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                // scroll to next item
                state.scrollBy(floatItemSize)
                // scroll back with some overscroll, which should be ignored
                consumed = state.scrollBy(-(floatItemSize + 10f))
            }
        }
        assertThat(consumed).isEqualTo(-floatItemSize)
    }

    @Composable
    private fun TestContent(
        containerSizeDp: Dp,
        beforeContentPaddingDp: Dp,
        afterContentPaddingDp: Dp
    ) {
        // |-|-|
        // |0|1|
        // |-| |
        // |2| |
        // |-|-|
        // |3|4|
        // | |-|
        // | |5|
        // |-| |
        LazyStaggeredGrid(
            lanes = 2,
            state = state,
            modifier = Modifier.axisSize(itemSizeDp * 2, containerSizeDp),
            contentPadding =
                if (vertical) {
                    PaddingValues(top = beforeContentPaddingDp, bottom = afterContentPaddingDp)
                } else {
                    PaddingValues(start = beforeContentPaddingDp, end = afterContentPaddingDp)
                },
        ) {
            items(
                count = itemCount,
                span = {
                    if (it == 50) {
                        StaggeredGridItemSpan.FullLine
                    } else {
                        StaggeredGridItemSpan.SingleLane
                    }
                }
            ) {
                BasicText(
                    "$it",
                    Modifier.mainAxisSize(itemSizeDp * ((it % 2) + 1)).testTag("$it").debugBorder()
                )
            }
        }
    }
}
