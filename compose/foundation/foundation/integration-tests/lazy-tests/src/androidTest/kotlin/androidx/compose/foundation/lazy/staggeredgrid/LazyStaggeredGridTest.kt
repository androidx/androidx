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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") // b/407927787

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.list.assertIsNotPlaced
import androidx.compose.foundation.lazy.list.assertIsPlaced
import androidx.compose.foundation.lazy.list.setContentWithTestViewConfiguration
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridTest(
    private val orientation: Orientation,
    private val useLookahead: Boolean,
) : BaseLazyStaggeredGridWithOrientation(orientation) {
    private val LazyStaggeredGridTag = "LazyStaggeredGridTag"

    internal lateinit var state: LazyStaggeredGridState

    companion object {
        @Suppress("TYPE_INTERSECTION_AS_REIFIED_WARNING")
        @JvmStatic
        @Parameterized.Parameters(name = "orientation: {0}, useLookahead: {1}")
        fun initParameters(): Array<Any> =
            arrayOf(
                arrayOf(Orientation.Vertical, true),
                arrayOf(Orientation.Vertical, false),
                arrayOf(Orientation.Horizontal, false),
            )
    }

    private var itemSizeDp: Dp = Dp.Unspecified
    private val itemSizePx: Int = 50

    @Before
    fun setUp() {
        with(rule.density) { itemSizeDp = itemSizePx.toDp() }
    }

    @After
    fun tearDown() {
        if (::state.isInitialized) {
            var isSorted = true
            var previousIndex = Int.MIN_VALUE
            for (item in state.layoutInfo.visibleItemsInfo) {
                if (previousIndex > item.index) {
                    isSorted = false
                    break
                }
                previousIndex = item.index
            }
            assertTrue(
                "Visible items MUST BE sorted: ${state.layoutInfo.visibleItemsInfo}",
                isSorted,
            )

            assertThat(state.layoutInfo.orientation == orientation)
        }
    }

    private fun ComposeContentTestRule.setContentWithConfigurableLookahead(
        content: @Composable () -> Unit
    ) {
        setContent { ConfigurableLookaheadScope(useLookahead, content) }
    }

    @Composable
    private fun ConfigurableLookaheadScope(useLookahead: Boolean, content: @Composable () -> Unit) {
        if (useLookahead) {
            LookaheadScope { content() }
        } else {
            content()
        }
    }

    private fun ComposeContentTestRule.setContentWithTestViewConfiguration(
        useLookahead: Boolean,
        content: @Composable () -> Unit,
    ) {
        setContentWithTestViewConfiguration { ConfigurableLookaheadScope(useLookahead, content) }
    }

    @Test
    fun showsZeroItems() {
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()

            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.testTag(LazyStaggeredGridTag),
            ) {}
        }

        rule.onNodeWithTag(LazyStaggeredGridTag).onChildren().assertCountEquals(0)

        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun showsOneItem() {
        val itemTestTag = "itemTestTag"

        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()

            LazyStaggeredGrid(lanes = 3, state = state) {
                item { Spacer(Modifier.size(itemSizeDp).testTag(itemTestTag)) }
            }
        }

        rule.onNodeWithTag(itemTestTag).assertIsDisplayed()

        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun distributesSingleLine() {
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(lanes = 3, modifier = Modifier.crossAxisSize(itemSizeDp * 3)) {
                items(3) { Spacer(Modifier.size(itemSizeDp).testTag("$it")) }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        rule
            .onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
    }

    @Test
    fun distributesTwoLines() {
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(lanes = 3, modifier = Modifier.crossAxisSize(itemSizeDp * 3)) {
                items(6) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp * (it + 1))
                            .testTag("$it")
                    )
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        // [item, 0, 0]
        rule
            .onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        // [item, item x 2, 0]
        rule
            .onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)

        // [item, item x 2, item x 3]
        rule
            .onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        // [item x 4, item x 2, item x 3]
        rule
            .onNodeWithTag("4")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        // [item x 4, item x 7, item x 3]
        rule
            .onNodeWithTag("5")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 3)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)

        // [item x 4, item x 7, item x 9]
    }

    @Test
    fun moreItemsDisplayedOnScroll() {
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.axisSize(itemSizeDp * 3, itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp * (it + 1))
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("3").assertDoesNotExist()

        state.scrollBy(itemSizeDp * 3)

        // [item, item x 2, item x 3]
        rule
            .onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp * 2)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        // [item x 4, item x 2, item x 3]
        rule
            .onNodeWithTag("4")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        // [item x 4, item x 7, item x 3]
        rule
            .onNodeWithTag("5")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)

        // [item x 4, item x 7, item x 9]
    }

    @Test
    fun itemSizeInLayoutInfo() {
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.axisSize(itemSizeDp * 3, itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp * (it + 1))
                            .testTag("$it")
                            .debugBorder()
                    )
                }
            }
        }

        state.scrollBy(itemSizeDp * 3)

        val items = state.layoutInfo.visibleItemsInfo

        assertThat(items.size).isEqualTo(3)
        with(items[0]) {
            assertThat(index).isEqualTo(3)
            assertThat(size).isEqualTo(axisSize(itemSizePx, itemSizePx * 4))
            assertThat(offset).isEqualTo(axisOffset(0, -itemSizePx * 2))
        }

        with(items[1]) {
            assertThat(index).isEqualTo(4)
            assertThat(size).isEqualTo(axisSize(itemSizePx, itemSizePx * 5))
            assertThat(offset).isEqualTo(axisOffset(itemSizePx, -itemSizePx))
        }

        with(items[2]) {
            assertThat(index).isEqualTo(5)
            assertThat(size).isEqualTo(axisSize(itemSizePx, itemSizePx * 6))
            assertThat(offset).isEqualTo(axisOffset(itemSizePx * 2, 0))
        }
    }

    @Test
    fun itemCanEmitZeroNodes() {
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier =
                    Modifier.axisSize(itemSizeDp * 3, itemSizeDp).testTag(LazyStaggeredGridTag),
            ) {
                items(6) {}
            }
        }

        rule
            .onNodeWithTag(LazyStaggeredGridTag)
            .assertIsDisplayed()
            .onChildren()
            .assertCountEquals(0)
    }

    @Test
    fun itemsAreHiddenOnScroll() {
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.axisSize(itemSizeDp * 3, itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp * (it + 1))
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()

        state.scrollBy(itemSizeDp * 3)

        rule.onNodeWithTag("0").assertIsNotDisplayed()

        rule.onNodeWithTag("1").assertIsNotDisplayed()

        rule.onNodeWithTag("2").assertIsNotDisplayed()
    }

    @Test
    fun itemsArePresentedWhenScrollingBack() {
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.axisSize(itemSizeDp * 3, itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp * (it + 1))
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()

        state.scrollBy(itemSizeDp * 3)
        state.scrollBy(-itemSizeDp * 3)

        for (i in 0..2) {
            rule
                .onNodeWithTag("$i")
                .assertIsDisplayed()
                .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
        }
    }

    @Test
    fun itemsAreCorrectedWhenSizeIncreased() {
        var expanded by mutableStateOf(false)
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp * 2),
            ) {
                item {
                    Spacer(
                        Modifier.axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = if (expanded) itemSizeDp * 2 else itemSizeDp,
                            )
                            .testTag("0")
                    )
                }
                items(5) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp)
                            .testTag("${it + 1}")
                    )
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("1")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("2")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)

        state.scrollBy(itemSizeDp * 3)

        expanded = true

        state.scrollBy(-itemSizeDp * 3)

        rule
            .onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        rule
            .onNodeWithTag("2")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)
    }

    @Test
    fun itemsAreCorrectedWhenSizeDecreased() {
        var expanded by mutableStateOf(true)
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp * 2),
            ) {
                item {
                    Spacer(
                        Modifier.axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = if (expanded) itemSizeDp * 2 else itemSizeDp,
                            )
                            .testTag("0")
                    )
                }
                items(5) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp)
                            .testTag("${it + 1}")
                    )
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("1")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("2")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)

        state.scrollBy(itemSizeDp * 3)

        expanded = false

        state.scrollBy(-itemSizeDp * 3)

        rule
            .onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp)

        rule
            .onNodeWithTag("2")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)
    }

    @Test
    fun itemsAreCorrectedWhenItemCountIsIncreasedFromZero() {
        var itemCount by mutableStateOf(0)
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp * 2),
            ) {
                items(itemCount) { Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it")) }
            }
        }

        rule.onNodeWithTag("0").assertDoesNotExist()

        itemCount = 4

        rule.onNodeWithTag("0").assertIsDisplayed()

        rule.onNodeWithTag("1").assertIsDisplayed()
    }

    @Test
    fun itemsAreCorrectedWithWrongColumns() {
        rule.setContentWithConfigurableLookahead {
            // intentionally wrong values, normally items should be [0, 1][2, 3][4, 5]
            state =
                rememberLazyStaggeredGridState(
                    initialFirstVisibleItemIndex = 3,
                    initialFirstVisibleItemScrollOffset = itemSizePx / 2,
                )
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp)
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0").assertDoesNotExist()

        rule.onNodeWithTag("3").assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp / 2)

        rule
            .onNodeWithTag("4")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp / 2)

        state.scrollBy(-itemSizeDp * 3)

        rule
            .onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun itemsAreCorrectedWithAlignedOffsets() {
        var expanded by mutableStateOf(false)
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState(initialFirstVisibleItemIndex = 0)
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier.mainAxisSize(
                                if (it % 2 == 1 && expanded) itemSizeDp * 2 else itemSizeDp
                            )
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()

        state.scrollBy(itemSizeDp * 2)

        rule.runOnIdle { expanded = true }

        state.scrollBy(itemSizeDp * -2)
        state.scrollBy(-itemSizeDp)

        rule
            .onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun itemsAreCorrectedWhenItemIncreased() {
        var expanded by mutableStateOf(false)
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState(initialFirstVisibleItemIndex = 0)
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp),
            ) {
                items(6) {
                    Spacer(
                        Modifier.mainAxisSize(
                                if (it == 3 && expanded) itemSizeDp * 2 else itemSizeDp
                            )
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()

        state.scrollBy(itemSizeDp * 2)

        rule.runOnIdle { expanded = true }

        state.scrollBy(itemSizeDp * -2)
        state.scrollBy(-itemSizeDp)

        rule
            .onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun addItems() {
        val state = LazyStaggeredGridState()
        var itemsCount by mutableStateOf(1)
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp),
            ) {
                items(itemsCount) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp)
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()

        rule.onNodeWithTag("1").assertDoesNotExist()

        itemsCount = 10

        rule.waitForIdle()

        state.scrollBy(itemSizeDp * 10)

        rule.onNodeWithTag("8").assertIsDisplayed()

        rule.onNodeWithTag("9").assertIsDisplayed()

        itemsCount = 20

        rule.waitForIdle()

        state.scrollBy(itemSizeDp * 10)

        rule.onNodeWithTag("18").assertIsDisplayed()

        rule.onNodeWithTag("19").assertIsDisplayed()
    }

    @Test
    fun removeItems() {
        var itemsCount by mutableStateOf(20)
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp),
            ) {
                items(itemsCount) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp)
                            .testTag("$it")
                    )
                }
            }
        }

        state.scrollBy(itemSizeDp * 20)

        rule.onNodeWithTag("18").assertIsDisplayed()

        rule.onNodeWithTag("19").assertIsDisplayed()

        itemsCount = 10

        rule.onNodeWithTag("8").assertIsDisplayed()

        rule.onNodeWithTag("9").assertIsDisplayed()

        itemsCount = 1

        rule.onNodeWithTag("0").assertIsDisplayed()

        rule
            .onNodeWithTag("1")
            // seems like reuse keeps the node around?
            .assertIsNotDisplayed()
    }

    @Test
    fun resizingItems_maintainsScrollingRange() {
        val state = LazyStaggeredGridState()
        var itemSizes by mutableStateOf(List(10) { itemSizeDp * (it % 4 + 1) })
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier =
                    Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp * 5)
                        .testTag(LazyStaggeredGridTag)
                        .border(1.dp, Color.Red),
            ) {
                items(itemSizes.size) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizes[it])
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag(LazyStaggeredGridTag).scrollMainAxisBy(itemSizeDp * 10)

        rule.onNodeWithTag("8").assertMainAxisSizeIsEqualTo(itemSizes[8])

        rule.onNodeWithTag("9").assertMainAxisSizeIsEqualTo(itemSizes[9])

        itemSizes = itemSizes.reversed()

        rule.onNodeWithTag("8").assertIsDisplayed()

        rule.onNodeWithTag("9").assertIsDisplayed()

        rule.onNodeWithTag(LazyStaggeredGridTag).scrollMainAxisBy(-itemSizeDp * 10)

        rule.onNodeWithTag("0").assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1").assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun removingItems_maintainsCorrectOffsets() {
        var itemCount by mutableStateOf(20)
        rule.setContentWithConfigurableLookahead {
            state =
                rememberLazyStaggeredGridState(
                    initialFirstVisibleItemIndex = 10,
                    initialFirstVisibleItemScrollOffset = 0,
                )
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier =
                    Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp * 5)
                        .testTag(LazyStaggeredGridTag)
                        .border(1.dp, Color.Red),
            ) {
                items(itemCount) {
                    Box(
                        Modifier.axisSize(
                                crossAxis = itemSizeDp,
                                mainAxis = itemSizeDp * (it % 3 + 1),
                            )
                            .testTag("$it")
                            .border(1.dp, Color.Black)
                    ) {
                        BasicText("$it")
                    }
                }
            }
        }

        itemCount = 3

        rule.waitForIdle()

        rule.onNodeWithTag("0").assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1").assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun staggeredGrid_supportsLargeIndices() {
        rule.setContentWithConfigurableLookahead {
            state =
                rememberLazyStaggeredGridState(
                    initialFirstVisibleItemIndex = Int.MAX_VALUE / 2,
                    initialFirstVisibleItemScrollOffset = 0,
                )
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier =
                    Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp * 5)
                        .testTag(LazyStaggeredGridTag)
                        .border(1.dp, Color.Red),
            ) {
                items(Int.MAX_VALUE) {
                    Spacer(
                        Modifier.axisSize(crossAxis = itemSizeDp, mainAxis = itemSizeDp)
                            .testTag("$it")
                    )
                }
            }
        }

        rule.onNodeWithTag("${Int.MAX_VALUE / 2}").assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("${Int.MAX_VALUE / 2 + 1}")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        // check that scrolling back and forth doesn't crash
        val delta = itemSizeDp * 5
        state.scrollBy(-delta)

        state.scrollBy(delta * 2)

        state.scrollBy(-delta)

        rule.onNodeWithTag("${Int.MAX_VALUE / 2}").assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("${Int.MAX_VALUE / 2 + 1}")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun scrollPositionIsRestored() {
        val restorationTester = StateRestorationTester(rule)
        var state: LazyStaggeredGridState?

        restorationTester.setContent {
            ConfigurableLookaheadScope(useLookahead) {
                state = rememberLazyStaggeredGridState()
                LazyStaggeredGrid(
                    lanes = 3,
                    state = state!!,
                    modifier = Modifier.mainAxisSize(itemSizeDp * 10).testTag(LazyStaggeredGridTag),
                ) {
                    items(1000) { Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it")) }
                }
            }
        }

        rule.onNodeWithTag(LazyStaggeredGridTag).scrollMainAxisBy(itemSizeDp * 10f)

        rule.onNodeWithTag("30").assertIsDisplayed()

        state = null
        restorationTester.emulateSavedInstanceStateRestore()

        rule.onNodeWithTag("30").assertIsDisplayed()
    }

    @Test
    fun restoredScrollPositionIsCorrectWhenItemsAreLoadedAsynchronously() {
        val restorationTester = StateRestorationTester(rule)

        var itemsCount = 100
        val recomposeCounter = mutableStateOf(0)

        restorationTester.setContent {
            ConfigurableLookaheadScope(useLookahead) {
                state = rememberLazyStaggeredGridState()
                LazyStaggeredGrid(
                    lanes = 3,
                    state = state,
                    modifier = Modifier.mainAxisSize(itemSizeDp * 10).testTag(LazyStaggeredGridTag),
                ) {
                    recomposeCounter.value // read state to force recomposition

                    items(itemsCount) { Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it")) }
                }
            }
        }

        rule.runOnIdle {
            runBlocking { state.scrollToItem(9, 10) }
            itemsCount = 0
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            itemsCount = 100
            recomposeCounter.value = 1
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(9)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
        }
    }

    @Test
    fun screenRotate_oneItem_withAdaptiveCells_fillsContentCorrectly() {
        var rotated by mutableStateOf(false)

        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()

            val crossAxis = if (!rotated) itemSizeDp * 6 else itemSizeDp * 9
            val mainAxis = if (!rotated) itemSizeDp * 9 else itemSizeDp * 6

            LazyStaggeredGrid(
                cells = StaggeredGridCells.Adaptive(itemSizeDp * 3),
                state = state,
                modifier =
                    Modifier.mainAxisSize(mainAxis)
                        .crossAxisSize(crossAxis)
                        .testTag(LazyStaggeredGridTag),
            ) {
                item { Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("0")) }
            }
        }

        fun verifyState() {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        rule.runOnIdle { rotated = true }

        rule.runOnIdle { verifyState() }

        rule.runOnIdle { rotated = false }

        rule.runOnIdle { verifyState() }

        rule.runOnIdle { rotated = true }

        rule.runOnIdle { verifyState() }
    }

    @Test
    fun screenRotate_twoItems_withAdaptiveCells_fillsContentCorrectly() {
        var rotated by mutableStateOf(false)

        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()

            val crossAxis = if (!rotated) itemSizeDp * 6 else itemSizeDp * 9
            val mainAxis = if (!rotated) itemSizeDp * 9 else itemSizeDp * 6

            LazyStaggeredGrid(
                cells = StaggeredGridCells.Adaptive(itemSizeDp * 3),
                state = state,
                modifier =
                    Modifier.mainAxisSize(mainAxis)
                        .crossAxisSize(crossAxis)
                        .testTag(LazyStaggeredGridTag),
            ) {
                items(2) { Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it")) }
            }
        }

        fun verifyState() {
            rule.runOnIdle {
                assertThat(state.firstVisibleItemIndex).isEqualTo(0)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            }
            rule.onNodeWithTag("0").assertIsDisplayed()
            rule.onNodeWithTag("1").assertIsDisplayed()
        }

        rule.runOnIdle { rotated = true }

        verifyState()

        rule.runOnIdle { rotated = false }

        verifyState()

        rule.runOnIdle { rotated = true }

        verifyState()
    }

    @Test
    fun scrollingALot_layoutIsNotRecomposed() {
        var recomposed = 0
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier =
                    Modifier.mainAxisSize(itemSizeDp * 10).composed {
                        recomposed++
                        Modifier
                    },
            ) {
                items(1000) { Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it")) }
            }
        }

        rule.waitForIdle()
        assertThat(recomposed).isEqualTo(1)

        state.scrollBy(1000.dp)

        rule.waitForIdle()
        assertThat(recomposed).isEqualTo(1)
    }

    @Test
    fun onlyOneInitialMeasurePass() {
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.mainAxisSize(itemSizeDp * 10).composed { Modifier },
            ) {
                items(1000) { Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it")) }
            }
        }

        rule.waitForIdle()
        assertThat(state.measurePassCount).isEqualTo(1)
    }

    @Test
    fun fillingFullSize_nextItemIsNotComposed() {
        val state = LazyStaggeredGridState()
        state.prefetchingEnabled = false
        val itemSizePx = 5f
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(
                1,
                Modifier.testTag(LazyStaggeredGridTag).mainAxisSize(itemSize),
                state,
            ) {
                items(3) { index -> Box(Modifier.size(itemSize).testTag("$index")) }
            }
        }

        repeat(3) { index ->
            rule.onNodeWithTag("$index").assertIsDisplayed()
            rule.onNodeWithTag("${index + 1}").assertDoesNotExist()
            rule.runOnIdle { runBlocking { state.scrollBy(itemSizePx) } }
        }
    }

    @Test
    fun fullSpan_fillsAllCrossAxisSpace() {
        val state = LazyStaggeredGridState()
        state.prefetchingEnabled = false
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(
                3,
                Modifier.testTag(LazyStaggeredGridTag)
                    .crossAxisSize(itemSizeDp * 3)
                    .mainAxisSize(itemSizeDp * 10),
                state,
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.testTag("0").mainAxisSize(itemSizeDp))
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp * 3)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)
    }

    @Test
    fun fullSpan_leavesEmptyGapsWithOtherItems() {
        val state = LazyStaggeredGridState()
        state.prefetchingEnabled = false
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(
                3,
                Modifier.testTag(LazyStaggeredGridTag)
                    .crossAxisSize(itemSizeDp * 3)
                    .mainAxisSize(itemSizeDp * 10),
                state,
            ) {
                items(2) { Box(Modifier.testTag("$it").mainAxisSize(itemSizeDp)) }

                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.testTag("full").mainAxisSize(itemSizeDp))
                }
            }
        }

        // ┌─┬─┬─┐
        // │0│1│#│
        // ├─┴─┴─┤
        // │full │
        // └─────┘
        rule
            .onNodeWithTag("0")
            .assertAxisBounds(DpOffset(0.dp, 0.dp), DpSize(itemSizeDp, itemSizeDp))

        rule
            .onNodeWithTag("1")
            .assertAxisBounds(DpOffset(itemSizeDp, 0.dp), DpSize(itemSizeDp, itemSizeDp))

        rule
            .onNodeWithTag("full")
            .assertAxisBounds(DpOffset(0.dp, itemSizeDp), DpSize(itemSizeDp * 3, itemSizeDp))
    }

    @Test
    fun fullSpan_leavesGapsBetweenItems() {
        val state = LazyStaggeredGridState()
        state.prefetchingEnabled = false
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(
                3,
                Modifier.testTag(LazyStaggeredGridTag)
                    .crossAxisSize(itemSizeDp * 3)
                    .mainAxisSize(itemSizeDp * 10),
                state,
            ) {
                items(3) {
                    Box(Modifier.testTag("$it").mainAxisSize(itemSizeDp + itemSizeDp * it / 2))
                }

                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.testTag("full").mainAxisSize(itemSizeDp))
                }
            }
        }

        // ┌───┬───┬───┐
        // │ 0 │ 1 │ 2 │
        // ├───┤   │   │
        // │   └───┤   │
        // ├───────┴───┤
        // │   full    │
        // └───────────┘
        rule
            .onNodeWithTag("0")
            .assertAxisBounds(DpOffset(0.dp, 0.dp), DpSize(itemSizeDp, itemSizeDp))

        rule
            .onNodeWithTag("1")
            .assertAxisBounds(DpOffset(itemSizeDp, 0.dp), DpSize(itemSizeDp, itemSizeDp * 1.5f))

        rule
            .onNodeWithTag("2")
            .assertAxisBounds(DpOffset(itemSizeDp * 2, 0.dp), DpSize(itemSizeDp, itemSizeDp * 2f))

        rule
            .onNodeWithTag("full")
            .assertAxisBounds(DpOffset(0.dp, itemSizeDp * 2f), DpSize(itemSizeDp * 3, itemSizeDp))
    }

    @Test
    fun fullSpan_scrollsCorrectly() {
        val state = LazyStaggeredGridState()
        state.prefetchingEnabled = false
        rule.setContentWithTestViewConfiguration(useLookahead) {
            LazyStaggeredGrid(
                3,
                Modifier.testTag(LazyStaggeredGridTag)
                    .crossAxisSize(itemSizeDp * 3)
                    .mainAxisSize(itemSizeDp * 2),
                state,
            ) {
                items(3) {
                    Box(Modifier.testTag("$it").mainAxisSize(itemSizeDp + itemSizeDp * it / 2))
                }

                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.testTag("full").mainAxisSize(itemSizeDp))
                }

                items(3) {
                    Box(
                        Modifier.testTag("${it + 3}").mainAxisSize(itemSizeDp + itemSizeDp * it / 2)
                    )
                }

                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.testTag("full-2").mainAxisSize(itemSizeDp))
                }
            }
        }

        // ┌───┬───┬───┐
        // │ 0 │ 1 │ 2 │
        // ├───┤   │   │
        // │   └───┤   │
        // ├───────┴───┤ <-- scroll offset
        // │   full    │
        // ├───┬───┬───┤
        // │ 3 │ 4 │ 5 │
        // ├───┤   │   │ <-- end of screen
        // │   └───┤   │
        // ├───────┴───┤
        // │   full-2  │
        // └───────────┘
        rule.onNodeWithTag(LazyStaggeredGridTag).scrollMainAxisBy(itemSizeDp * 2f)

        rule
            .onNodeWithTag("full")
            .assertAxisBounds(DpOffset(0.dp, 0.dp), DpSize(itemSizeDp * 3, itemSizeDp))

        assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun fullSpan_scrollsCorrectly_pastFullSpan() {
        val state = LazyStaggeredGridState()
        state.prefetchingEnabled = false
        rule.setContentWithTestViewConfiguration(useLookahead) {
            LazyStaggeredGrid(
                3,
                Modifier.testTag(LazyStaggeredGridTag)
                    .crossAxisSize(itemSizeDp * 3)
                    .mainAxisSize(itemSizeDp * 2),
                state,
            ) {
                repeat(10) { repeatIndex ->
                    items(3) {
                        Box(
                            Modifier.testTag("${repeatIndex * 3 + it}")
                                .mainAxisSize(itemSizeDp + itemSizeDp * it / 2)
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(Modifier.testTag("full-$repeatIndex").mainAxisSize(itemSizeDp))
                    }
                }
            }
        }

        // ┌───┬───┬───┐
        // │ 0 │ 1 │ 2 │
        // ├───┤   │   │
        // │   └───┤   │
        // ├───────┴───┤
        // │   full-0  │
        // ├───┬───┬───┤  <-- scroll offset
        // │ 3 │ 4 │ 5 │
        // ├───┤   │   │
        // │   └───┤   │
        // ├───────┴───┤  <-- end of screen
        // │   full-1  │
        // └───────────┘
        rule.onNodeWithTag(LazyStaggeredGridTag).scrollMainAxisBy(itemSizeDp * 3f)

        rule
            .onNodeWithTag("3")
            .assertAxisBounds(DpOffset(0.dp, 0.dp), DpSize(itemSizeDp, itemSizeDp))

        rule
            .onNodeWithTag("4")
            .assertAxisBounds(DpOffset(itemSizeDp, 0.dp), DpSize(itemSizeDp, itemSizeDp * 1.5f))

        rule
            .onNodeWithTag("5")
            .assertAxisBounds(DpOffset(itemSizeDp * 2, 0.dp), DpSize(itemSizeDp, itemSizeDp * 2))

        assertThat(state.firstVisibleItemIndex).isEqualTo(4)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun fullSpan_scrollsCorrectly_pastFullSpan_andBack() {
        val state = LazyStaggeredGridState()
        state.prefetchingEnabled = false
        rule.setContentWithTestViewConfiguration(useLookahead) {
            LazyStaggeredGrid(
                3,
                Modifier.testTag(LazyStaggeredGridTag)
                    .crossAxisSize(itemSizeDp * 3)
                    .mainAxisSize(itemSizeDp * 2),
                state,
            ) {
                repeat(10) { repeatIndex ->
                    items(3) {
                        Box(
                            Modifier.testTag("${repeatIndex * 3 + it}")
                                .mainAxisSize(itemSizeDp + itemSizeDp * it / 2)
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(Modifier.testTag("full-$repeatIndex").mainAxisSize(itemSizeDp))
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyStaggeredGridTag).scrollMainAxisBy(itemSizeDp * 3f)

        rule.onNodeWithTag(LazyStaggeredGridTag).scrollMainAxisBy(-itemSizeDp * 3f)

        // ┌───┬───┬───┐  <-- scroll offset
        // │ 0 │ 1 │ 2 │
        // ├───┤   │   │
        // │   └───┤   │
        // ├───────┴───┤  <-- end of screen
        // │   full-0  │
        // ├───┬───┬───┤
        // │ 3 │ 4 │ 5 │
        // ├───┤   │   │
        // │   └───┤   │
        // ├───────┴───┤
        // │   full-1  │
        // └───────────┘

        rule
            .onNodeWithTag("0")
            .assertAxisBounds(DpOffset(0.dp, 0.dp), DpSize(itemSizeDp, itemSizeDp))

        rule
            .onNodeWithTag("1")
            .assertAxisBounds(DpOffset(itemSizeDp, 0.dp), DpSize(itemSizeDp, itemSizeDp * 1.5f))

        rule
            .onNodeWithTag("2")
            .assertAxisBounds(DpOffset(itemSizeDp * 2, 0.dp), DpSize(itemSizeDp, itemSizeDp * 2))

        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun fullSpan_scrollsCorrectly_multipleFullSpans() {
        val state = LazyStaggeredGridState()
        state.prefetchingEnabled = false
        rule.setContentWithTestViewConfiguration(useLookahead = useLookahead) {
            LazyStaggeredGrid(
                3,
                Modifier.testTag(LazyStaggeredGridTag)
                    .crossAxisSize(itemSizeDp * 3)
                    .mainAxisSize(itemSizeDp * 2),
                state,
            ) {
                items(10, span = { StaggeredGridItemSpan.FullLine }) {
                    Box(Modifier.testTag("$it").mainAxisSize(itemSizeDp))
                }
            }
        }

        rule.onNodeWithTag(LazyStaggeredGridTag).scrollMainAxisBy(itemSizeDp * 3f)

        rule
            .onNodeWithTag("3")
            .assertAxisBounds(DpOffset(0.dp, 0.dp), DpSize(itemSizeDp * 3, itemSizeDp))

        rule
            .onNodeWithTag("4")
            .assertAxisBounds(DpOffset(0.dp, itemSizeDp), DpSize(itemSizeDp * 3, itemSizeDp))

        assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

        rule.onNodeWithTag(LazyStaggeredGridTag).scrollMainAxisBy(itemSizeDp * 10f)

        rule
            .onNodeWithTag("8")
            .assertAxisBounds(DpOffset(0.dp, 0.dp), DpSize(itemSizeDp * 3, itemSizeDp))

        rule
            .onNodeWithTag("9")
            .assertAxisBounds(DpOffset(0.dp, itemSizeDp), DpSize(itemSizeDp * 3, itemSizeDp))

        assertThat(state.firstVisibleItemIndex).isEqualTo(8)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun initialIndex_largerThanItemCount_ordersItemsCorrectly_withFullSpan() {
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState(20)
            Box(Modifier.mainAxisSize(itemSizeDp * 4)) {
                LazyStaggeredGrid(
                    lanes = 3,
                    state = state,
                    modifier = Modifier.crossAxisSize(itemSizeDp * 3).testTag(LazyStaggeredGridTag),
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.testTag("full").mainAxisSize(itemSizeDp * 2))
                    }
                    items(6) {
                        val size =
                            when (it) {
                                0,
                                3 -> itemSizeDp * 2
                                1,
                                4 -> itemSizeDp * 1.5f
                                2,
                                5 -> itemSizeDp
                                else -> error("unexpected item $it")
                            }
                        Spacer(Modifier.testTag("$it").mainAxisSize(size))
                    }
                }
            }
        }

        // ┌───────────┐
        // │           │
        // │   full    │ <-- scroll offset
        // │           │
        // ├───┬───┬───┤
        // │ 0 │ 1 │ 2 │
        // │   │   ├───┤
        // │   │───┤ 3 │
        // ├───┤ 4 │   │
        // │ 5 │   │   │
        // └───┴───┴───┘ <-- end of grid

        rule
            .onNodeWithTag("full")
            .assertAxisBounds(DpOffset(0.dp, -itemSizeDp), DpSize(itemSizeDp * 3, itemSizeDp * 2))

        rule
            .onNodeWithTag("0")
            .assertAxisBounds(DpOffset(0.dp, itemSizeDp), DpSize(itemSizeDp, itemSizeDp * 2f))

        rule
            .onNodeWithTag("1")
            .assertAxisBounds(
                DpOffset(itemSizeDp, itemSizeDp),
                DpSize(itemSizeDp, itemSizeDp * 1.5f),
            )

        rule
            .onNodeWithTag("2")
            .assertAxisBounds(DpOffset(itemSizeDp * 2f, itemSizeDp), DpSize(itemSizeDp, itemSizeDp))

        rule
            .onNodeWithTag("3")
            .assertAxisBounds(
                DpOffset(itemSizeDp * 2f, itemSizeDp * 2f),
                DpSize(itemSizeDp, itemSizeDp * 2),
            )

        rule
            .onNodeWithTag("4")
            .assertAxisBounds(
                DpOffset(itemSizeDp, itemSizeDp * 2.5f),
                DpSize(itemSizeDp, itemSizeDp * 1.5f),
            )

        rule
            .onNodeWithTag("5")
            .assertAxisBounds(DpOffset(0.dp, itemSizeDp * 3), DpSize(itemSizeDp, itemSizeDp))
    }

    @Test
    fun initialIndex_largerThanItemCount_ordersItemsCorrectly() {
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState(20)
            Box(Modifier.mainAxisSize(itemSizeDp * 4)) {
                LazyStaggeredGrid(
                    lanes = 3,
                    state = state,
                    modifier = Modifier.crossAxisSize(itemSizeDp * 3).testTag(LazyStaggeredGridTag),
                ) {
                    items(6) {
                        val size =
                            when (it) {
                                0,
                                3 -> itemSizeDp * 2
                                1,
                                4 -> itemSizeDp * 1.5f
                                2,
                                5 -> itemSizeDp
                                else -> error("unexpected item $it")
                            }
                        Spacer(Modifier.testTag("$it").mainAxisSize(size))
                    }
                }
            }
        }

        // ┌───┬───┬───┐
        // │ 0 │ 1 │ 2 │
        // │   │   ├───┤
        // │   │───┤ 3 │
        // ├───┤ 4 │   │
        // │ 5 │   │   │
        // └───┴───┴───┘

        rule.onNodeWithTag(LazyStaggeredGridTag).assertMainAxisSizeIsEqualTo(itemSizeDp * 3)

        rule
            .onNodeWithTag("0")
            .assertAxisBounds(DpOffset(0.dp, 0.dp), DpSize(itemSizeDp, itemSizeDp * 2f))

        rule
            .onNodeWithTag("1")
            .assertAxisBounds(DpOffset(itemSizeDp, 0.dp), DpSize(itemSizeDp, itemSizeDp * 1.5f))

        rule
            .onNodeWithTag("2")
            .assertAxisBounds(DpOffset(itemSizeDp * 2f, 0.dp), DpSize(itemSizeDp, itemSizeDp))

        rule
            .onNodeWithTag("3")
            .assertAxisBounds(
                DpOffset(itemSizeDp * 2f, itemSizeDp),
                DpSize(itemSizeDp, itemSizeDp * 2),
            )

        rule
            .onNodeWithTag("4")
            .assertAxisBounds(
                DpOffset(itemSizeDp, itemSizeDp * 1.5f),
                DpSize(itemSizeDp, itemSizeDp * 1.5f),
            )

        rule
            .onNodeWithTag("5")
            .assertAxisBounds(DpOffset(0.dp, itemSizeDp * 2), DpSize(itemSizeDp, itemSizeDp))
    }

    @Test
    fun changeItemsAndScrollImmediately() {
        val keys = mutableStateListOf<Int>().also { list -> repeat(10) { list.add(it) } }
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(lanes = 2, Modifier.mainAxisSize(itemSizeDp), state) {
                items(keys, key = { it }) { Box(Modifier.size(itemSizeDp * 2)) }
            }
        }

        rule.waitForIdle()
        state.scrollTo(8)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(8)

            keys.add(0, -1)
            keys.add(0, -2)

            runBlocking(AutoTestFrameClock()) {
                state.scrollBy(10f)
                state.scrollBy(-10f)
            }

            assertThat(state.firstVisibleItemIndex).isEqualTo(10)
        }
    }

    @Test
    fun fixedSizeCell_forcesFixedSize() {
        val state = LazyStaggeredGridState()
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(
                cells = StaggeredGridCells.FixedSize(itemSizeDp * 2),
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 5, mainAxis = itemSizeDp * 5),
                state = state,
            ) {
                items(10) { index -> Box(Modifier.size(itemSizeDp).testTag(index.toString())) }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp * 2)
        rule
            .onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2f)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp * 2)
    }

    @Test
    fun manyPlaceablesInItem_itemSizeIsMaxOfPlaceables() {
        val state = LazyStaggeredGridState()
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp * 5),
                state = state,
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.size(itemSizeDp * 2))
                    Box(Modifier.size(itemSizeDp))
                }

                items(10) { index -> Box(Modifier.size(itemSizeDp).testTag(index.toString())) }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertAxisBounds(DpOffset(0.dp, itemSizeDp * 2), DpSize(itemSizeDp, itemSizeDp))
        rule
            .onNodeWithTag("1")
            .assertAxisBounds(DpOffset(itemSizeDp, itemSizeDp * 2), DpSize(itemSizeDp, itemSizeDp))
    }

    @Test
    fun scrollDuringMeasure() {
        rule.setContentWithConfigurableLookahead {
            BoxWithConstraints {
                val state = rememberLazyStaggeredGridState()
                LazyStaggeredGrid(
                    lanes = 1,
                    state = state,
                    modifier =
                        Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp * 5),
                ) {
                    items(20) {
                        Spacer(modifier = Modifier.mainAxisSize(itemSizeDp).testTag(it.toString()))
                    }
                }
                LaunchedEffect(state) { state.scrollToItem(10) }
            }
        }

        rule.onNodeWithTag("10").assertStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun scrollInLaunchedEffect() {
        rule.setContentWithConfigurableLookahead {
            val state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 1,
                state = state,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp * 5),
            ) {
                items(20) {
                    Spacer(modifier = Modifier.mainAxisSize(itemSizeDp).testTag(it.toString()))
                }
            }
            LaunchedEffect(state) { state.scrollToItem(10) }
        }

        rule.onNodeWithTag("10").assertStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun scrollToPreviouslyFullSpanItem() {
        var firstItemVisible by mutableStateOf(false)
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp * 2),
            ) {
                if (firstItemVisible) {
                    item { Spacer(modifier = Modifier.mainAxisSize(itemSizeDp).testTag("first")) }
                }

                items(
                    count = 20,
                    span = {
                        if (it == 10) StaggeredGridItemSpan.FullLine
                        else StaggeredGridItemSpan.SingleLane
                    },
                ) {
                    Spacer(modifier = Modifier.mainAxisSize(itemSizeDp).testTag(it.toString()))
                }
            }
        }

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) { state.scrollToItem(10) }

            firstItemVisible = true

            runBlocking(AutoTestFrameClock()) {
                state.scrollToItem(17)

                state.scrollToItem(10)
            }
        }

        rule.onNodeWithTag("9").assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun itemsRemovedAfterLargeThenSmallScrollForward() {
        lateinit var state: LazyStaggeredGridState
        val composedItems = mutableSetOf<Int>()
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.mainAxisSize(itemSizeDp * 1.5f).crossAxisSize(itemSizeDp * 2),
            ) {
                items(100) {
                    Spacer(Modifier.mainAxisSize(itemSizeDp))
                    DisposableEffect(it) {
                        composedItems += it
                        onDispose { composedItems -= it }
                    }
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.prefetchingEnabled = false
                state.scrollBy(itemSizePx * 3f)
                assertThat(state.firstVisibleItemIndex).isEqualTo(6)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
                state.scrollBy(10f)
                assertThat(state.firstVisibleItemIndex).isEqualTo(6)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
            }
        }
        rule.runOnIdle { assertThat(composedItems).isEqualTo(setOf(6, 7, 8, 9)) }
    }

    @Test
    fun itemsRemovedAfterLargeThenSmallScrollBackward() {
        lateinit var state: LazyStaggeredGridState
        val composedItems = mutableSetOf<Int>()
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState(initialFirstVisibleItemIndex = 6)
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.mainAxisSize(itemSizeDp * 1.5f).crossAxisSize(itemSizeDp * 2),
            ) {
                items(100) {
                    Spacer(Modifier.mainAxisSize(itemSizeDp))
                    DisposableEffect(it) {
                        composedItems += it
                        onDispose { composedItems -= it }
                    }
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.prefetchingEnabled = false
                state.scrollBy(-itemSizePx * 2.5f)
                assertThat(state.firstVisibleItemIndex).isEqualTo(0)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(itemSizePx / 2)
                state.scrollBy(-5f)
                assertThat(state.firstVisibleItemIndex).isEqualTo(0)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(itemSizePx / 2 - 5)
            }
        }
        rule.runOnIdle { assertThat(composedItems).isEqualTo(setOf(0, 1, 2, 3)) }
    }

    @Test
    fun zeroSizeItemIsPlacedWhenItIsAtTheTop() {
        lateinit var state: LazyStaggeredGridState

        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState(initialFirstVisibleItemIndex = 0)
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.mainAxisSize(itemSizeDp * 2).crossAxisSize(itemSizeDp * 2),
            ) {
                repeat(10) { index ->
                    items(2) { Spacer(Modifier.testTag("${index * 10 + it}")) }
                    items(8) { Spacer(Modifier.mainAxisSize(itemSizeDp)) }
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertIsPlaced()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(0.dp)

        rule
            .onNodeWithTag("1")
            .assertIsPlaced()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(0.dp)

        runBlocking(Dispatchers.Main + AutoTestFrameClock()) { state.scrollToItem(10, 0) }

        rule
            .onNodeWithTag("10")
            .assertIsPlaced()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(0.dp)

        rule
            .onNodeWithTag("11")
            .assertIsPlaced()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(0.dp)
    }

    @Test
    fun itemsAreDistributedCorrectlyOnOverscrollPassWithSameOffset() {
        val gridHeight = itemSizeDp * 11 // two big items + two small items
        state = LazyStaggeredGridState()
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(
                modifier = Modifier.mainAxisSize(gridHeight).crossAxisSize(itemSizeDp * 2),
                state = state,
                lanes = 2,
            ) {
                items(20) {
                    Spacer(
                        Modifier.mainAxisSize(
                                if (it % 2 == 0) itemSizeDp * 5 else itemSizeDp * 0.5f
                            )
                            .border(1.dp, Color.Red)
                            .testTag("$it")
                    )
                }
            }
        }

        // scroll to bottom
        state.scrollBy(gridHeight * 2)

        rule
            .onNodeWithTag("12")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("13")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        // scroll a back a bit
        state.scrollBy(-itemSizeDp * 5)

        // scroll by a grid height
        state.scrollBy(gridHeight)

        rule
            .onNodeWithTag("12")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)

        rule
            .onNodeWithTag("13")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun fixedCells_withSpacing_notEnoughSpace() {
        state = LazyStaggeredGridState()
        rule.setContentWithConfigurableLookahead {
            Box(Modifier.size(itemSizeDp)) {
                LazyStaggeredGrid(
                    modifier = Modifier.mainAxisSize(itemSizeDp * 5),
                    state = state,
                    lanes = 2,
                    crossAxisArrangement = Arrangement.spacedBy(itemSizeDp * 2),
                ) {
                    items(20) { Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it")) }
                }
            }
        }
    }

    @Test
    fun fullSpanItemShowsCorrectly_afterASmallScrollThroughAGap() {
        lateinit var state: LazyStaggeredGridState

        // ┌───┬───┐ <-- scroll offset
        // │ 0 │ 1 │
        // │   ├───┤ <-- end of screen
        // │   │   │
        // │   │   │
        // ├───┴───┤
        // │   2   │
        // └───────┘
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState().apply { prefetchingEnabled = false }
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.mainAxisSize(itemSizeDp).crossAxisSize(itemSizeDp * 2),
            ) {
                item { Spacer(Modifier.mainAxisSize(itemSizeDp * 3).testTag("0")) }

                item { Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("1")) }

                item(span = StaggeredGridItemSpan.FullLine) {
                    Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("2"))
                }
            }
        }

        // ┌───┬───┐
        // │ 0 │ 1 │
        // │   ├───┤ <-- scroll offset
        // │   │   │
        // │   │   │ <-- end of screen
        // ├───┴───┤
        // │   2   │
        // └───────┘
        state.scrollBy(itemSizeDp)

        rule.onNodeWithTag("0").assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp)
        rule.onNodeWithTag("1").assertIsNotPlaced()
        rule.onNodeWithTag("2").assertIsNotPlaced()

        assertThat(state.layoutInfo.visibleItemsInfo.map { it.index }).containsExactly(0)

        // ┌───┬───┐
        // │ 0 │ 1 │
        // │   ├───┤
        // │   │   │ <-- scroll offset
        // │   │   │
        // ├───┴───┤ <-- end of screen (1.dp higher to avoid entering item 2)
        // │   2   │
        // └───────┘
        state.scrollBy(itemSizeDp - 1.dp)
        rule.onNodeWithTag("0").assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp * 2 + 1.dp)
        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("2").assertIsNotPlaced()

        assertThat(state.layoutInfo.visibleItemsInfo.map { it.index }).containsExactly(0)

        // ┌───┬───┐
        // │ 0 │ 1 │
        // │   ├───┤
        // │   │   │ <-- scroll offset
        // │   │   │
        // ├───┴───┤ <-- end of screen
        // │   2   │
        // └───────┘
        state.scrollBy(1.dp)
        rule.onNodeWithTag("0").assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp * 2)
        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("2").assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)
        assertThat(state.layoutInfo.visibleItemsInfo.map { it.index }).containsExactly(0, 2)

        // ┌───┬───┐
        // │ 0 │ 1 │
        // │   ├───┤
        // │   │   │
        // │   │   │
        // ├───┴───┤ <-- scroll offset
        // │   2   │
        // └───────┘ <-- end of the screen
        //
        state.scrollBy(itemSizeDp * 3)
        rule.onNodeWithTag("0").assertIsNotPlaced()
        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("2").assertMainAxisStartPositionInRootIsEqualTo(0.dp)
        assertThat(state.layoutInfo.visibleItemsInfo.map { it.index }).containsExactly(2)
    }

    @Test
    fun customOverscroll() {
        val overscroll = TestOverscrollEffect()

        rule.setContentWithConfigurableLookahead {
            val state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier =
                    Modifier.mainAxisSize(itemSizeDp * 1.5f)
                        .crossAxisSize(itemSizeDp * 2)
                        .testTag("grid"),
                overscrollEffect = overscroll,
            ) {
                items(100) { Spacer(Modifier.mainAxisSize(itemSizeDp)) }
            }
        }

        // The overscroll modifier should be added / drawn
        rule.runOnIdle { assertThat(overscroll.drawCalled).isTrue() }

        // Swipe backwards to trigger overscroll
        rule.onNodeWithTag("grid").performTouchInput { if (vertical) swipeDown() else swipeRight() }

        rule.runOnIdle {
            // The swipe will result in multiple scroll deltas
            assertThat(overscroll.applyToScrollCalledCount).isGreaterThan(1)
            assertThat(overscroll.applyToFlingCalledCount).isEqualTo(1)
            if (vertical) {
                assertThat(overscroll.scrollOverscrollDelta.y).isGreaterThan(0)
                assertThat(overscroll.flingOverscrollVelocity.y).isGreaterThan(0)
            } else {
                assertThat(overscroll.scrollOverscrollDelta.x).isGreaterThan(0)
                assertThat(overscroll.flingOverscrollVelocity.x).isGreaterThan(0)
            }
        }
    }

    @Test
    fun mainSize_minusOne() {
        // Forces the main axis size to be calculated to exactly -1
        val mainAxisSize = with(rule.density) { (itemSizePx * 2f - 1f).toDp() }
        rule.setContentWithConfigurableLookahead {
            LazyStaggeredGrid(
                lanes = 2,
                modifier =
                    Modifier.mainAxisSize(mainAxisSize)
                        .crossAxisSize(itemSizeDp * 2)
                        .testTag(LazyStaggeredGridTag),
                contentPadding = PaddingValues(beforeContent = itemSizeDp),
            ) {
                items(2) { index -> Spacer(Modifier.mainAxisSize(itemSizeDp)) }
            }
        }

        rule.onNodeWithTag(LazyStaggeredGridTag).assertIsDisplayed()
    }

    @Test
    fun fullSpanItem_correctOrderWhenUpdated() {
        lateinit var state: LazyStaggeredGridState

        // ┌───┬───────┐
        // │ 0 │       │
        // ├───┴───────┤
        // │   full    │
        // └───────────┘

        var itemCount by mutableStateOf(1)
        rule.setContentWithConfigurableLookahead {
            state = rememberLazyStaggeredGridState().apply { prefetchingEnabled = false }
            LazyStaggeredGrid(
                lanes = 3,
                state = state,
                modifier = Modifier.mainAxisSize(itemSizeDp * 2).crossAxisSize(itemSizeDp * 3),
            ) {
                items(itemCount) { Spacer(Modifier.mainAxisSize(itemSizeDp).testTag("$it")) }

                item(span = StaggeredGridItemSpan.FullLine) {
                    Spacer(Modifier.mainAxisSize(itemSizeDp))
                }
            }
        }

        // ┌───┬───┬───┐
        // │ 0 │ 1 │   │
        // ├───┴───┴───┤
        // │   full    │
        // └───────────┘

        itemCount++
        rule.onNodeWithTag("0").assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1").assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)

        // ┌───┬───┬───┐
        // │ 0 │ 1 │ 2 │
        // ├───┴───┴───┤
        // │   full    │
        // └───────────┘
        itemCount++
        rule.onNodeWithTag("0").assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1").assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
        rule.onNodeWithTag("2").assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
    }
}
