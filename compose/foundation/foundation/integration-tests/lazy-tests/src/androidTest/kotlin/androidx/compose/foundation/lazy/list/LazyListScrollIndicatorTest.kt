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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.util.fastSumBy
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyListScrollIndicatorTest(orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    private val LazyListTag = "LazyListTag"

    @Test
    fun scrollIndicatorState_emptyList() {
        lateinit var state: LazyListState
        val mainAxisSizePx = 100
        val mainAxisSize = with(rule.density) { mainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(mainAxisSize).testTag(LazyListTag),
                state = state,
            ) {}
        }

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(mainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_whenContentFits() {
        lateinit var state: LazyListState
        val itemSizePx = 50
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val totalItems = 3

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * 4).testTag(LazyListTag),
                state = state,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemSize).then(fillParentMaxCrossAxis()))
                }
            }
        }

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo((itemSizePx * totalItems))
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo((itemSizePx * 4))
        }
    }

    @Test
    fun scrollIndicatorState_uniformSizeItems() {
        lateinit var state: LazyListState
        val itemSizePx = 50
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val totalItems = 10
        val itemsInViewport = 5

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * itemsInViewport).testTag(LazyListTag),
                state = state,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemSize).then(fillParentMaxCrossAxis()))
                }
            }
        }

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo((itemSizePx * totalItems))
            assertThat(state.scrollIndicatorState?.viewportSize)
                .isEqualTo((itemSizePx * itemsInViewport))
        }
    }

    @Test
    fun scrollIndicatorState_uniformSizeItems_withItemSpacing() {
        lateinit var state: LazyListState
        val itemSizePx = 50
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val totalItems = 10
        val itemsInViewport = 5
        val itemSpacingPx = 20
        val itemSpacing = with(rule.density) { itemSpacingPx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * itemsInViewport).testTag(LazyListTag),
                state = state,
                spacedBy = itemSpacing,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemSize).then(fillParentMaxCrossAxis()))
                }
            }
        }

        val expectedContentSize = (itemSizePx * totalItems) + (itemSpacingPx * (totalItems - 1))

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize)
                .isEqualTo((itemSizePx * itemsInViewport))
        }
    }

    @Test
    fun scrollIndicatorState_variableSizeItems() {
        lateinit var state: LazyListState
        val itemSizesPx = listOf(50, 100, 80, 70, 120)
        val itemSizes = with(rule.density) { itemSizesPx.map { it.toDp() } }
        val totalItems = 10
        val containerSizePx = 300
        val containerSize = with(rule.density) { containerSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(containerSize).testTag(LazyListTag),
                state = state,
            ) {
                items(totalItems) {
                    Spacer(
                        Modifier.mainAxisSize(itemSizes[it % itemSizes.size])
                            .then(fillParentMaxCrossAxis())
                    )
                }
            }
        }

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize)
                .isEqualTo((state.layoutInfo.visibleItemsAverageSize() * totalItems))
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo((containerSizePx))
        }
    }

    @Test
    fun scrollIndicatorState_variableSizeItems_withItemSpacing() {
        lateinit var state: LazyListState
        val itemSizesPx = listOf(50, 100, 80, 70, 120)
        val itemSizes = with(rule.density) { itemSizesPx.map { it.toDp() } }
        val totalItems = 10
        val containerSizePx = 300
        val containerSize = with(rule.density) { containerSizePx.toDp() }
        val itemSpacingPx = 20
        val itemSpacing = with(rule.density) { itemSpacingPx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(containerSize).testTag(LazyListTag),
                state = state,
                spacedBy = itemSpacing,
            ) {
                items(totalItems) {
                    Spacer(
                        Modifier.mainAxisSize(itemSizes[it % itemSizes.size])
                            .then(fillParentMaxCrossAxis())
                    )
                }
            }
        }

        val expectedContentSize =
            (state.layoutInfo.visibleItemsAverageSize() * totalItems) - itemSpacingPx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo((containerSizePx))
        }
    }

    @Test
    fun scrollIndicatorState_withContentPadding() {
        lateinit var state: LazyListState
        val itemSizePx = 50
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val startPaddingPx = 20
        val startPadding = with(rule.density) { startPaddingPx.toDp() }
        val endPaddingPx = 30
        val endPadding = with(rule.density) { endPaddingPx.toDp() }
        val totalItems = 10
        val itemsInViewport = 5

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * itemsInViewport).testTag(LazyListTag),
                state = state,
                contentPadding =
                    if (vertical) {
                        PaddingValues(top = startPadding, bottom = endPadding)
                    } else {
                        PaddingValues(start = startPadding, end = endPadding)
                    },
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemSize).then(fillParentMaxCrossAxis()))
                }
            }
        }

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize)
                .isEqualTo(itemSizePx * totalItems + startPaddingPx + endPaddingPx)
            assertThat(state.scrollIndicatorState?.viewportSize)
                .isEqualTo((itemSizePx * itemsInViewport))
        }
    }

    @Test
    fun scrollIndicatorState_withReverseLayout() {
        lateinit var state: LazyListState
        val itemSizePx = 50
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val startPaddingPx = 20
        val startPadding = with(rule.density) { startPaddingPx.toDp() }
        val endPaddingPx = 30
        val endPadding = with(rule.density) { endPaddingPx.toDp() }
        val totalItems = 20
        val itemsInViewport = 5

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * itemsInViewport).testTag(LazyListTag),
                state = state,
                reverseLayout = true,
                contentPadding =
                    if (vertical) {
                        PaddingValues(top = startPadding, bottom = endPadding)
                    } else {
                        PaddingValues(start = startPadding, end = endPadding)
                    },
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemSize).then(fillParentMaxCrossAxis()))
                }
            }
        }

        val expectedContentSize = (itemSizePx * totalItems) + startPaddingPx + endPaddingPx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize)
                .isEqualTo(itemSizePx * itemsInViewport)
        }
    }

    @Test
    fun scrollIndicatorState_withInitialScroll() {
        lateinit var state: LazyListState
        val itemSizePx = 50
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val startPaddingPx = 20
        val startPadding = with(rule.density) { startPaddingPx.toDp() }
        val endPaddingPx = 30
        val endPadding = with(rule.density) { endPaddingPx.toDp() }
        val totalItems = 20
        val itemsInViewport = 5
        val initialFirstVisibleItemIndex = 2
        val initialFirstVisibleItemScrollOffsetPx = 25

        rule.setContentWithTestViewConfiguration {
            state =
                rememberLazyListState(
                    initialFirstVisibleItemIndex,
                    initialFirstVisibleItemScrollOffsetPx,
                )
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * itemsInViewport).testTag(LazyListTag),
                state = state,
                contentPadding =
                    if (vertical) {
                        PaddingValues(top = startPadding, bottom = endPadding)
                    } else {
                        PaddingValues(start = startPadding, end = endPadding)
                    },
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemSize).then(fillParentMaxCrossAxis()))
                }
            }
        }

        val expectedScrollOffset =
            (initialFirstVisibleItemIndex * itemSizePx) + initialFirstVisibleItemScrollOffsetPx
        val expectedContentSize = (itemSizePx * totalItems) + startPaddingPx + endPaddingPx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(expectedScrollOffset)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize)
                .isEqualTo(itemSizePx * itemsInViewport)
        }
    }

    @Test
    fun scrollIndicatorState_reverseLayout_withInitialScroll() {
        lateinit var state: LazyListState
        val itemSizePx = 50
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val startPaddingPx = 20
        val startPadding = with(rule.density) { startPaddingPx.toDp() }
        val endPaddingPx = 30
        val endPadding = with(rule.density) { endPaddingPx.toDp() }
        val totalItems = 20
        val itemsInViewport = 5
        val initialFirstVisibleItemIndex = 2
        val initialFirstVisibleItemScrollOffsetPx = 25

        rule.setContentWithTestViewConfiguration {
            state =
                rememberLazyListState(
                    initialFirstVisibleItemIndex,
                    initialFirstVisibleItemScrollOffsetPx,
                )
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * itemsInViewport).testTag(LazyListTag),
                state = state,
                reverseLayout = true,
                contentPadding =
                    if (vertical) {
                        PaddingValues(top = startPadding, bottom = endPadding)
                    } else {
                        PaddingValues(start = startPadding, end = endPadding)
                    },
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemSize).then(fillParentMaxCrossAxis()))
                }
            }
        }

        val expectedScrollOffset =
            (initialFirstVisibleItemIndex * itemSizePx) + initialFirstVisibleItemScrollOffsetPx
        val expectedContentSize = (itemSizePx * totalItems) + startPaddingPx + endPaddingPx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(expectedScrollOffset)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize)
                .isEqualTo(itemSizePx * itemsInViewport)
        }
    }

    @Test
    fun scrollIndicatorState_onScrollByOffset() {
        lateinit var state: LazyListState
        val itemSizePx = 50
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val totalItems = 20
        val itemsInViewport = 5
        val scrollAmountPx = 80
        val scrollAmount = with(rule.density) { scrollAmountPx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * itemsInViewport).testTag(LazyListTag),
                state = state,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemSize).then(fillParentMaxCrossAxis()))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag).scrollMainAxisBy(scrollAmount)

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset)
                .isEqualTo(
                    state.layoutInfo.visibleItemsAverageSize() * state.firstVisibleItemIndex +
                        state.firstVisibleItemScrollOffset
                )
            assertThat(state.scrollIndicatorState?.contentSize)
                .isEqualTo(state.layoutInfo.visibleItemsAverageSize() * totalItems)
            assertThat(state.scrollIndicatorState?.viewportSize)
                .isEqualTo(itemSizePx * itemsInViewport)
        }
    }

    @Test
    fun scrollIndicatorState_onScrollToIndexAndOffset() {
        lateinit var state: LazyListState
        val itemSizePx = 50
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val totalItems = 20
        val itemsInViewport = 5
        val scrollToIndex = 2
        val scrollAmountPx = 10
        val scrollAmount = with(rule.density) { scrollAmountPx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * itemsInViewport).testTag(LazyListTag),
                state = state,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemSize).then(fillParentMaxCrossAxis()))
                }
            }
        }

        state.scrollTo(scrollToIndex)

        rule.waitForIdle()

        state.scrollBy(scrollAmount)

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset)
                .isEqualTo(
                    state.layoutInfo.visibleItemsAverageSize() * scrollToIndex + scrollAmountPx
                )
            assertThat(state.scrollIndicatorState?.contentSize)
                .isEqualTo(state.layoutInfo.visibleItemsAverageSize() * totalItems)
            assertThat(state.scrollIndicatorState?.viewportSize)
                .isEqualTo(itemSizePx * itemsInViewport)
        }
    }

    @Test
    fun scrollIndicatorState_withContentPadding_onScroll() {
        lateinit var state: LazyListState
        val itemSizePx = 50
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val startPaddingPx = 70
        val startPadding = with(rule.density) { startPaddingPx.toDp() }
        val endPaddingPx = 30
        val endPadding = with(rule.density) { endPaddingPx.toDp() }
        val totalItems = 20
        val itemsInViewport = 5
        val scrollAmountPx = 40
        val scrollAmount = with(rule.density) { scrollAmountPx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * itemsInViewport).testTag(LazyListTag),
                state = state,
                contentPadding =
                    if (vertical) {
                        PaddingValues(top = startPadding, bottom = endPadding)
                    } else {
                        PaddingValues(start = startPadding, end = endPadding)
                    },
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemSize).then(fillParentMaxCrossAxis()))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag).scrollMainAxisBy(scrollAmount)

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset)
                .isEqualTo(
                    state.layoutInfo.visibleItemsAverageSize() * state.firstVisibleItemIndex +
                        state.firstVisibleItemScrollOffset
                )
            assertThat(state.scrollIndicatorState?.contentSize)
                .isEqualTo(
                    state.layoutInfo.visibleItemsAverageSize() * totalItems +
                        startPaddingPx +
                        endPaddingPx
                )
            assertThat(state.scrollIndicatorState?.viewportSize)
                .isEqualTo(itemSizePx * itemsInViewport)
        }
    }

    private fun LazyListLayoutInfo.visibleItemsAverageSize(): Int {
        val visibleItems = visibleItemsInfo
        if (visibleItems.isEmpty()) return 0
        val itemsSum = visibleItems.fastSumBy { it.size }
        return itemsSum / visibleItems.size + mainAxisItemSpacing
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
