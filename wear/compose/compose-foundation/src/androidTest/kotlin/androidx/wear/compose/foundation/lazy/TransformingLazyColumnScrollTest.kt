/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnScrollTest {
    @get:Rule val rule = createComposeRule()

    private val lazyListTag = "LazyList"

    private val itemsCount = 20
    private lateinit var state: TransformingLazyColumnState

    private val itemSizePx = 100
    private var itemSizeDp = Dp.Unspecified

    private lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        with(rule.density) { itemSizeDp = itemSizePx.toDp() }
    }

    private fun testScroll(
        initialAnchorItemIndex: Int = -1,
        initialAnchorItemScrollOffset: Int = 0,
        spacingPx: Int = 0,
        containerSizePx: Int = itemSizePx * 3,
        frameClock: MonotonicFrameClock = AutoTestFrameClock(),
        scrollBlock: suspend () -> Unit,
        assertBlock: () -> Unit,
    ) {
        rule.setContent {
            state =
                rememberTransformingLazyColumnState(
                    initialAnchorItemIndex = initialAnchorItemIndex,
                    initialAnchorItemScrollOffset = initialAnchorItemScrollOffset,
                )
            scope = rememberCoroutineScope()
            with(rule.density) { TestContent(spacingPx.toDp(), containerSizePx.toDp()) }
        }
        runBlocking { withContext(Dispatchers.Main + frameClock) { scrollBlock() } }
        rule.runOnIdle { assertBlock() }
    }

    @Test
    fun setupWorks() =
        testScroll(scrollBlock = {}) {
            assertThat(state.anchorItemIndex).isEqualTo(1)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
        }

    @Test
    fun scrollToItem() =
        testScroll(scrollBlock = { state.scrollToItem(3) }) {
            assertThat(state.anchorItemIndex).isEqualTo(3)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
        }

    @Test
    fun scrollToItemWithOffset() =
        testScroll(scrollBlock = { state.scrollToItem(3, 10) }) {
            assertThat(state.layoutInfo.visibleItems.firstOrNull()?.index).isEqualTo(2)
            val item3Offset = state.layoutInfo.visibleItems.first { it.index == 3 }.offset
            assertThat(item3Offset).isEqualTo(itemSizePx - 10)
        }

    @Test
    fun scrollToItemWithNegativeOffset() =
        testScroll(scrollBlock = { state.scrollToItem(3, -10) }) {
            assertThat(state.anchorItemIndex).isEqualTo(3)
            assertThat(state.anchorItemScrollOffset).isEqualTo(-10)
        }

    @Test
    fun scrollToItemWithOffsetLargerThanAvailableSize() =
        testScroll(scrollBlock = { state.scrollToItem(itemsCount - 1, -10) }) {
            assertThat(state.anchorItemIndex).isEqualTo(itemsCount - 2) // last item feels the space
            assertThat(state.anchorItemScrollOffset).isEqualTo(0) // not 10
        }

    @Test
    fun scrollToItemWithIndexLargerThanItemsCount() =
        testScroll(scrollBlock = { state.scrollToItem(itemsCount + 2) }) {
            assertThat(state.anchorItemIndex).isEqualTo(itemsCount - 2) // last item feels the space
        }

    @Test
    fun animateScrollToItem_anchorItemWithOffset_scrollsInCorrectDirectionWithoutJump() {
        val recordedOffsets = mutableListOf<Int>()
        testScroll(
            initialAnchorItemIndex = 5,
            initialAnchorItemScrollOffset = 30,
            frameClock = AutoTestFrameClock { recordedOffsets.add(state.anchorItemScrollOffset) },
            scrollBlock = { state.animateScrollToItem(5, 0) },
        ) {
            assertThat(state.anchorItemIndex).isEqualTo(5)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
            // The offset should move monotonically from 30 down toward 0 (never above 30),
            // and the last animation frame before snapToItem should already be within 2px of 0.
            assertThat(recordedOffsets.maxOrNull() ?: 0).isAtMost(30)
            assertThat(abs(recordedOffsets.last())).isAtMost(2)
        }
    }

    @Test
    fun animateScrollToItem_withTransformedHeight_landsSmoothlyWithoutEndJump() {
        val containerSizePx = itemSizePx * 3
        val viewportCenterPx = containerSizePx / 2
        setTransformedContent(containerSizePx = containerSizePx)

        // Scroll forward to off-screen item 10 and record its visual center on each frame.
        val item10Centers = mutableListOf<Int>()
        runBlocking(
            Dispatchers.Main +
                AutoTestFrameClock {
                    state.layoutInfo.visibleItems
                        .firstOrNull { it.index == 10 }
                        ?.let { item10Centers.add(it.offset + it.transformedHeight / 2) }
                }
        ) {
            state.animateScrollToItem(10)
        }

        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(10)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
            assertThat(item10Centers).isInOrder(Comparator.reverseOrder<Int>())
            assertThat(abs(item10Centers.last() - viewportCenterPx)).isAtMost(2)
        }

        // Scroll backward to off-screen item 2 and record its visual center on each frame.
        val item2Centers = mutableListOf<Int>()
        runBlocking(
            Dispatchers.Main +
                AutoTestFrameClock {
                    state.layoutInfo.visibleItems
                        .firstOrNull { it.index == 2 }
                        ?.let { item2Centers.add(it.offset + it.transformedHeight / 2) }
                }
        ) {
            state.animateScrollToItem(2)
        }

        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(2)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
            assertThat(item2Centers).isInOrder()
            assertThat(abs(item2Centers.last() - viewportCenterPx)).isAtMost(2)
        }
    }

    @Test
    fun animateScrollToItem_alreadyVisibleTransformedItemWithSpacing_landsSmoothly() {
        val containerSizePx = itemSizePx * 3
        val viewportCenterPx = containerSizePx / 2
        setTransformedContent(
            initialAnchorItemIndex = 2,
            spacingPx = 12,
            containerSizePx = containerSizePx,
        )

        // Item 3 is already visible below anchor item 2; scroll forward to it.
        val item3Centers = mutableListOf<Int>()
        runBlocking(
            Dispatchers.Main +
                AutoTestFrameClock {
                    state.layoutInfo.visibleItems
                        .firstOrNull { it.index == 3 }
                        ?.let { item3Centers.add(it.offset + it.transformedHeight / 2) }
                }
        ) {
            state.animateScrollToItem(3)
        }

        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(3)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
            assertThat(item3Centers).isInOrder(Comparator.reverseOrder<Int>())
            assertThat(abs(item3Centers.last() - viewportCenterPx)).isAtMost(2)
        }

        // Item 2 is now visible above anchor item 3; scroll backward to it.
        val item2Centers = mutableListOf<Int>()
        runBlocking(
            Dispatchers.Main +
                AutoTestFrameClock {
                    state.layoutInfo.visibleItems
                        .firstOrNull { it.index == 2 }
                        ?.let { item2Centers.add(it.offset + it.transformedHeight / 2) }
                }
        ) {
            state.animateScrollToItem(2)
        }

        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(2)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
            assertThat(item2Centers).isInOrder()
            assertThat(abs(item2Centers.last() - viewportCenterPx)).isAtMost(2)
        }
    }

    @Test
    fun animateScrollToItem_withScrollOffset_landsSmoothlyAtRequestedOffset() {
        val containerSizePx = itemSizePx * 3
        val viewportCenterPx = containerSizePx / 2
        val targetIndex = 8
        // Use an offset (-68px) that places the target item outside the flat center region (±60px)
        // so that the target item itself is transformed (transformedHeight < itemSizePx) at its
        // final destination.
        val targetScrollOffset = -68
        val expectedCenterPx = viewportCenterPx - targetScrollOffset
        setTransformedContent(containerSizePx = containerSizePx)

        val targetCenters = mutableListOf<Int>()
        runBlocking(
            Dispatchers.Main +
                AutoTestFrameClock {
                    state.layoutInfo.visibleItems
                        .firstOrNull { it.index == targetIndex }
                        ?.let { targetCenters.add(it.offset + it.transformedHeight / 2) }
                }
        ) {
            state.animateScrollToItem(targetIndex, targetScrollOffset)
        }

        rule.runOnIdle {
            assertThat(targetCenters).isInOrder(Comparator.reverseOrder<Int>())
            assertThat(abs(targetCenters.last() - expectedCenterPx)).isAtMost(2)

            val targetItem = state.layoutInfo.visibleItems.first { it.index == targetIndex }
            assertThat(targetItem.transformedHeight).isLessThan(itemSizePx)
            assertThat(targetItem.offset)
                .isEqualTo(viewportCenterPx - itemSizePx / 2 - targetScrollOffset)
        }
    }

    @Test
    fun approximateDistanceTo_targetIsAnchorItem_returnsExpectedLinearDistance() {
        setTransformedContent(
            initialAnchorItemIndex = 3,
            initialAnchorItemScrollOffset = 10,
            spacingPx = 10,
        )

        rule.runOnIdle {
            runBlocking {
                state.scroll {
                    val scope = TransformingLazyColumnScrollScope(state, this)
                    assertThat(scope.approximateDistanceTo(targetIndex = 3, targetOffset = 0))
                        .isEqualTo(-10)
                    assertThat(scope.approximateDistanceTo(targetIndex = 3, targetOffset = 15))
                        .isEqualTo(-10 + 15)
                }
            }
        }
    }

    @Test
    fun approximateDistanceTo_targetIsVisibleItemAfterAnchor_returnsExpectedLinearDistance() {
        setTransformedContent(
            initialAnchorItemIndex = 3,
            initialAnchorItemScrollOffset = 10,
            spacingPx = 10,
        )

        rule.runOnIdle {
            runBlocking {
                state.scroll {
                    val scope = TransformingLazyColumnScrollScope(state, this)
                    // Uses full itemSizePx + spacing relative to anchor, not shrunk
                    // transformedHeight
                    assertThat(scope.approximateDistanceTo(targetIndex = 4, targetOffset = 0))
                        .isEqualTo((itemSizePx + 10) - 10)
                    assertThat(scope.approximateDistanceTo(targetIndex = 4, targetOffset = 15))
                        .isEqualTo((itemSizePx + 10) - 10 + 15)
                }
            }
        }
    }

    @Test
    fun approximateDistanceTo_targetIsVisibleItemBeforeAnchor_returnsExpectedLinearDistance() {
        setTransformedContent(
            initialAnchorItemIndex = 3,
            initialAnchorItemScrollOffset = 10,
            spacingPx = 10,
        )

        rule.runOnIdle {
            runBlocking {
                state.scroll {
                    val scope = TransformingLazyColumnScrollScope(state, this)
                    assertThat(scope.approximateDistanceTo(targetIndex = 2, targetOffset = 0))
                        .isEqualTo(-(itemSizePx + 10) - 10)
                    assertThat(scope.approximateDistanceTo(targetIndex = 2, targetOffset = 15))
                        .isEqualTo(-(itemSizePx + 10) - 10 + 15)
                }
            }
        }
    }

    @Test
    fun approximateDistanceTo_targetIsOffScreenItem_returnsExpectedLinearDistance() {
        setTransformedContent(
            initialAnchorItemIndex = 3,
            initialAnchorItemScrollOffset = 10,
            spacingPx = 10,
        )

        rule.runOnIdle {
            runBlocking {
                state.scroll {
                    val scope = TransformingLazyColumnScrollScope(state, this)
                    val expectedOffscreen = (itemSizePx + 10) * (10 - 3) - 10
                    assertThat(scope.approximateDistanceTo(targetIndex = 10, targetOffset = 0))
                        .isEqualTo(expectedOffscreen)
                    assertThat(scope.approximateDistanceTo(targetIndex = 10, targetOffset = 15))
                        .isEqualTo(expectedOffscreen + 15)
                }
            }
        }
    }

    private fun setTransformedContent(
        initialAnchorItemIndex: Int = -1,
        initialAnchorItemScrollOffset: Int = 0,
        spacingPx: Int = 0,
        containerSizePx: Int = itemSizePx * 3,
    ) {
        rule.setContent {
            state =
                rememberTransformingLazyColumnState(
                    initialAnchorItemIndex = initialAnchorItemIndex,
                    initialAnchorItemScrollOffset = initialAnchorItemScrollOffset,
                )
            scope = rememberCoroutineScope()
            with(rule.density) {
                TransformingLazyColumn(
                    Modifier.height(containerSizePx.toDp()).testTag(lazyListTag),
                    state,
                    verticalArrangement = Arrangement.spacedBy(spacingPx.toDp()),
                ) {
                    items(itemsCount) {
                        Spacer(
                            modifier =
                                Modifier.height(itemSizeDp).transformedHeight { height, progress ->
                                    // Keep a flat unscaled region around the viewport center
                                    // (within 20% of the center) and shrink items up to 50% near
                                    // the top/bottom edges, similar to ResponsiveTransformationSpec
                                    val centerFraction =
                                        (progress.topOffsetFraction +
                                            progress.bottomOffsetFraction) / 2f
                                    val edgeDist =
                                        (abs(centerFraction - 0.5f) - 0.2f).coerceAtLeast(0f)
                                    val scale = (1f - edgeDist * 1.5f).coerceIn(0.5f, 1f)
                                    (height * scale).roundToInt().coerceAtLeast(1)
                                }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TestContent(spacingDp: Dp, containerSizeDp: Dp) =
        TransformingLazyColumn(
            Modifier.height(containerSizeDp).testTag(lazyListTag),
            state,
            verticalArrangement = Arrangement.spacedBy(spacingDp),
        ) {
            items(itemsCount) { Spacer(modifier = Modifier.height(itemSizeDp)) }
        }
}

private class AutoTestFrameClock(private val onFrameComplete: () -> Unit = {}) :
    MonotonicFrameClock {
    private val time = AtomicLong(0)

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        val result = onFrame(time.getAndAdd(16_000_000))
        onFrameComplete()
        return result
    }
}
