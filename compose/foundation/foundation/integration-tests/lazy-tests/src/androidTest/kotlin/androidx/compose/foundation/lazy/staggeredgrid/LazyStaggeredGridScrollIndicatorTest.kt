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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.list.setContentWithTestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyStaggeredGridScrollIndicatorTest(private val orientation: Orientation) :
    BaseLazyStaggeredGridWithOrientation(orientation) {

    private val LazyStaggeredGridTag = "LazyStaggeredGridTag"

    @Test
    fun scrollIndicatorState_emptyGrid() {
        lateinit var state: LazyStaggeredGridState
        val lanesCount = 2
        val containerMainAxisSizePx = 100
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = lanesCount,
                modifier =
                    Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyStaggeredGridTag),
                state = state,
            ) {}
        }

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_whenContentFits() {
        lateinit var state: LazyStaggeredGridState
        val lanesCount = 2
        val itemMainSizePx = 50
        val itemMainSize = with(rule.density) { itemMainSizePx.toDp() }
        val totalItems = 4 // 1 line
        val containerMainAxisSizePx = itemMainSizePx * 3 // Viewport larger than content
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = lanesCount,
                modifier =
                    Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyStaggeredGridTag),
                state = state,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemMainSize).crossAxisSize(10.dp))
                }
            }
        }

        val expectedContentSize = itemMainSizePx * totalItems / lanesCount

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_uniformSizeItems() {
        lateinit var state: LazyStaggeredGridState
        val lanesCount = 2
        val itemMainSizePx = 50
        val itemMainSize = with(rule.density) { itemMainSizePx.toDp() }
        val totalItems = 10 // 5 lines
        val linesInViewport = 3
        val containerMainAxisSizePx = itemMainSizePx * linesInViewport
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = lanesCount,
                modifier =
                    Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyStaggeredGridTag),
                state = state,
            ) {
                items(totalItems) {
                    Spacer(Modifier.mainAxisSize(itemMainSize).crossAxisSize(10.dp))
                }
            }
        }

        val expectedContentSize = itemMainSizePx * totalItems / lanesCount

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_staggeredContent() {
        lateinit var state: LazyStaggeredGridState
        val lanesCount = 3
        val totalItems = 10
        val mainAxisSizesPx = listOf(20, 40, 30, 50)
        val containerMainAxisSizePx = 100
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = lanesCount,
                modifier =
                    Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyStaggeredGridTag),
                state = state,
            ) {
                items(totalItems) { index ->
                    with(rule.density) {
                        Spacer(
                            Modifier.mainAxisSize(
                                    mainAxisSizesPx[index % mainAxisSizesPx.size].toDp()
                                )
                                .crossAxisSize(30.toDp())
                        )
                    }
                }
            }
        }

        val expectedContentSize =
            (state.layoutInfo.visibleItemsAverageSize() * totalItems) / lanesCount

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_staggeredContent_withMainAxisSpacing() {
        lateinit var state: LazyStaggeredGridState
        val lanesCount = 3
        val totalItems = 10
        val mainAxisSizesPx = listOf(20, 40, 30, 50)
        val mainAxisSpacingPx = 10
        val mainAxisSpacing = with(rule.density) { mainAxisSpacingPx.toDp() }
        val containerMainAxisSizePx = 100
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = lanesCount,
                modifier =
                    Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyStaggeredGridTag),
                state = state,
                mainAxisSpacing = mainAxisSpacing,
            ) {
                items(totalItems) { index ->
                    with(rule.density) {
                        Spacer(
                            Modifier.mainAxisSize(
                                    mainAxisSizesPx[index % mainAxisSizesPx.size].toDp()
                                )
                                .crossAxisSize(30.toDp())
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        val expectedContentSize =
            ((state.layoutInfo.visibleItemsAverageSize() * totalItems) / lanesCount) -
                mainAxisSpacingPx

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_staggeredContent_withContentPadding() {
        lateinit var state: LazyStaggeredGridState
        val lanesCount = 3
        val totalItems = 10
        val mainAxisSizesPx = listOf(20, 40, 30, 50)
        val startPaddingPx = 20
        val startPadding = with(rule.density) { startPaddingPx.toDp() }
        val endPaddingPx = 30
        val endPadding = with(rule.density) { endPaddingPx.toDp() }
        val containerMainAxisSizePx = 100
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        val contentPadding =
            if (orientation == Orientation.Vertical) {
                PaddingValues(top = startPadding, bottom = endPadding)
            } else {
                PaddingValues(start = startPadding, end = endPadding)
            }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = lanesCount,
                modifier =
                    Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyStaggeredGridTag),
                state = state,
                contentPadding = contentPadding,
            ) {
                items(totalItems) { index ->
                    with(rule.density) {
                        Spacer(
                            Modifier.mainAxisSize(
                                    mainAxisSizesPx[index % mainAxisSizesPx.size].toDp()
                                )
                                .crossAxisSize(30.toDp())
                        )
                    }
                }
            }
        }

        val totalPadding = startPaddingPx + endPaddingPx
        val expectedContentSize =
            ((state.layoutInfo.visibleItemsAverageSize() * totalItems) / lanesCount) + totalPadding

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_staggeredContent_withReverseLayout() {
        lateinit var state: LazyStaggeredGridState
        val lanesCount = 3
        val totalItems = 10
        val mainAxisSizesPx = listOf(20, 40, 30, 50)
        val containerMainAxisSizePx = 100
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = lanesCount,
                modifier =
                    Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyStaggeredGridTag),
                state = state,
                reverseLayout = true,
            ) {
                items(totalItems) { index ->
                    with(rule.density) {
                        Spacer(
                            Modifier.mainAxisSize(
                                    mainAxisSizesPx[index % mainAxisSizesPx.size].toDp()
                                )
                                .crossAxisSize(30.toDp())
                        )
                    }
                }
            }
        }

        val expectedContentSize =
            (state.layoutInfo.visibleItemsAverageSize() * totalItems) / lanesCount

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_staggeredContent_onScrollByOffset() {
        lateinit var state: LazyStaggeredGridState
        val lanesCount = 3
        val totalItems = 12
        val mainAxisSizesPx = listOf(20, 40, 30, 50)
        val containerMainAxisSizePx = 100
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }
        val scrollAmountPx = 50
        val scrollAmount = with(rule.density) { scrollAmountPx.toDp() }

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = lanesCount,
                modifier =
                    Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyStaggeredGridTag),
                state = state,
            ) {
                items(totalItems) { index ->
                    with(rule.density) {
                        Spacer(
                            Modifier.mainAxisSize(
                                    mainAxisSizesPx[index % mainAxisSizesPx.size].toDp()
                                )
                                .crossAxisSize(30.toDp())
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyStaggeredGridTag).scrollMainAxisBy(scrollAmount)

        val visibleItemsAverageSize = state.layoutInfo.visibleItemsAverageSize()
        val expectedScrollAmount =
            (visibleItemsAverageSize * state.firstVisibleItemIndex) / lanesCount +
                state.firstVisibleItemScrollOffset
        val expectedContentSize = (visibleItemsAverageSize * totalItems) / lanesCount

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(expectedScrollAmount)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_staggeredContent_onScrollToItem() {
        lateinit var state: LazyStaggeredGridState
        val lanesCount = 3
        val totalItems = 15
        val mainAxisSizesPx = listOf(20, 40, 30, 50)
        val containerMainAxisSizePx = 100
        val containerMainAxisSize = with(rule.density) { containerMainAxisSizePx.toDp() }
        val scrollToItemIndex = 7 // Belongs to line 3 (0-indexed lines)

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = lanesCount,
                modifier =
                    Modifier.mainAxisSize(containerMainAxisSize).testTag(LazyStaggeredGridTag),
                state = state,
            ) {
                items(totalItems) { index ->
                    with(rule.density) {
                        Spacer(
                            Modifier.mainAxisSize(
                                    mainAxisSizesPx[index % mainAxisSizesPx.size].toDp()
                                )
                                .crossAxisSize(30.toDp())
                        )
                    }
                }
            }
        }

        state.scrollTo(scrollToItemIndex)

        val visibleItemsAverageSize = state.layoutInfo.visibleItemsAverageSize()
        val expectedScrollAmount =
            ((visibleItemsAverageSize * state.firstVisibleItemIndex) / lanesCount) +
                state.firstVisibleItemScrollOffset
        val expectedContentSize = (visibleItemsAverageSize * totalItems) / lanesCount

        rule.runOnIdle {
            assertNotNull(state.scrollIndicatorState)
            assertThat(state.scrollIndicatorState?.scrollOffset).isEqualTo(expectedScrollAmount)
            assertThat(state.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(state.scrollIndicatorState?.viewportSize).isEqualTo(containerMainAxisSizePx)
        }
    }

    private fun LazyStaggeredGridLayoutInfo.visibleItemsAverageSize(): Int {
        val visibleItems = visibleItemsInfo
        if (visibleItems.isEmpty()) return 0
        val itemSizeSum =
            visibleItems.fastSumBy {
                if (orientation == Orientation.Vertical) {
                    it.size.height
                } else {
                    it.size.width
                }
            }
        return itemSizeSum / visibleItems.size + mainAxisItemSpacing
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "orientation={0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
