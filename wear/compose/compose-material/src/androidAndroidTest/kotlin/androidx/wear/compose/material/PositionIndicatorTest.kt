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

package androidx.wear.compose.material

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
public class PositionIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    private var itemSizePx: Int = 50
    private var itemSizeDp: Dp = Dp.Infinity
    private var itemSpacingPx = 6
    private var itemSpacingDp: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
            itemSpacingDp = itemSpacingPx.toDp()
        }
    }

    @Test
    fun emptyScalingLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyListState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
            ) {
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1)
        }
    }

    @Test
    fun scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        lateinit var state: ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyListState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f),
                autoCentering = false
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1f)
        }
    }

    @Test
    @Ignore("Offsets not being handled correctly due to b/198751807")
    fun scalingLazyColumnNotLargeEnoughToScrollSwapVerticalAlignmentGivesCorrectPositionAndSize() {
        lateinit var state: ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyListState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(
                    space = itemSpacingDp,
                    alignment = Alignment.Bottom
                ),
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1f)
        }
    }

    @Test
    fun scrollableScalingLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyListState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredHeight(
                        // Exactly the right size to hold 3 items with spacing
                        itemSizeDp * 3f + itemSpacingDp * 2f
                    ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeScale = 1.0f),
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            // Scroll forwards so that item with index 2 is in the center of the viewport
            runBlocking {
                state.scrollBy((itemSizePx.toFloat() + itemSpacingPx.toFloat()) * 2f)
            }

            state.layoutInfo.assertWhollyVisibleItems(
                firstItemIndex = 1, lastItemIndex = 3,
                viewPortHeight = viewPortHeight
            )

            // And that the indicator is at position 0.5 and of expected size
            assertThat(
                positionIndicatorState.positionFraction
            ).isWithin(0.05f).of(0.5f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)
        }
    }

    @Test
    fun emptyReverseLayoutScalingLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyListState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                reverseLayout = true,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
            ) {
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1)
        }
    }

    @Test
    fun reverseLayoutScalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        lateinit var state: ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyListState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(
                    space = itemSpacingDp,
                    alignment = Alignment.Bottom
                ),
                reverseLayout = true,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .fillMaxWidth()
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
                    .background(Color.DarkGray),
                autoCentering = false
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1f)
        }
    }

    @Test
    fun reverseLayoutScrollableScalingLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyListState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                reverseLayout = true,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredHeight(
                        // Exactly the right size to hold 3 items with spacing
                        itemSizeDp * 3f + itemSpacingDp * 2f
                    )
                    .background(Color.DarkGray),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeScale = 1.0f),
                autoCentering = false
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + itemSpacingPx.toFloat())
            }

            state.layoutInfo.assertWhollyVisibleItems(
                firstItemIndex = 1, lastItemIndex = 3,
                viewPortHeight = viewPortHeight
            )

            // And that the indicator is at position 0.5 and of expected size
            assertThat(
                positionIndicatorState.positionFraction
            ).isWithin(0.05f).of(0.5f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)
        }
    }

    @Test
    fun emptyLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: LazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberLazyListState()
            positionIndicatorState = LazyColumnStateAdapter(state)
            LazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
            ) {
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1)
        }
    }

    @Test
    fun lazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        lateinit var state: LazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberLazyListState()
            positionIndicatorState = LazyColumnStateAdapter(state)
            LazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1f)
        }
    }

    @Test
    @Ignore("Offsets not being handled correctly due to b/198751807")
    fun lazyColumnNotLargeEnoughToScrollSwapVerticalAlignmentGivesCorrectPositionAndSize() {
        lateinit var state: LazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberLazyListState()
            positionIndicatorState = LazyColumnStateAdapter(state)
            LazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(
                    space = itemSpacingDp,
                    alignment = Alignment.Bottom
                ),
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1f)
        }
    }

    @Test
    fun scrollableLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: LazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberLazyListState()
            positionIndicatorState = LazyColumnStateAdapter(state)
            LazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredHeight(
                        // Exactly the right size to hold 3 items with spacing
                        itemSizeDp * 3f + itemSpacingDp * 2f
                    ),
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            // Scroll forwards
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + itemSpacingPx.toFloat())
            }

            // And that the indicator is at position 0.5 and of expected size
            assertThat(
                positionIndicatorState.positionFraction
            ).isWithin(0.05f).of(0.5f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)
        }
    }

    @Test
    fun emptyReverseLayoutLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: LazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberLazyListState()
            positionIndicatorState = LazyColumnStateAdapter(state)
            LazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                reverseLayout = true,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
            ) {
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1)
        }
    }

    @Test
    fun reverseLayoutLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        lateinit var state: LazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberLazyListState()
            positionIndicatorState = LazyColumnStateAdapter(state)
            LazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(
                    space = itemSpacingDp,
                    alignment = Alignment.Bottom
                ),
                reverseLayout = true,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .fillMaxWidth()
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
                    .background(Color.DarkGray),
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1f)
        }
    }

    @Test
    fun reverseLayoutScrollableLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: LazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberLazyListState()
            positionIndicatorState = LazyColumnStateAdapter(state)
            LazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                reverseLayout = false,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredHeight(
                        // Exactly the right size to hold 3 items with spacing
                        itemSizeDp * 3f + itemSpacingDp * 2f
                    )
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy((itemSizePx + itemSpacingPx).toFloat())
            }
        }
        rule.waitForIdle()

        // That the indicator is at position 0.5 and of expected size
        assertThat(
            positionIndicatorState.positionFraction
        ).isWithin(0.05f).of(0.5f)
        assertThat(
            positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
        ).isWithin(0.05f).of(0.6f)
    }

    @Test
    fun emptyScrollableColumnGivesCorrectPositionAndSize() {
        lateinit var state: ScrollState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScrollState()
            positionIndicatorState = ScrollStateAdapter(scrollState = state)
            Column(
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
                    .verticalScroll(state = state)
            ) {
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1)
        }
    }

    @Test
    fun emptyReversedScrollableColumnGivesCorrectPositionAndSize() {
        lateinit var state: ScrollState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScrollState()
            positionIndicatorState = ScrollStateAdapter(scrollState = state)
            Column(
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
                    .verticalScroll(state = state, reverseScrolling = true)
            ) {
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
                reverseDirection = true,
            )
        }

        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1)
        }
    }

    @Test
    fun scrollableColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        lateinit var state: ScrollState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScrollState()
            positionIndicatorState = ScrollStateAdapter(state)
            Column(
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
                    .verticalScroll(state = state),
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp)
            ) {
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1)
        }
    }

    @Test
    fun reversedScrollableColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        lateinit var state: ScrollState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScrollState()
            positionIndicatorState = ScrollStateAdapter(scrollState = state)
            Column(
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
                    .verticalScroll(state = state, reverseScrolling = true),
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp)
            ) {
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
                reverseDirection = true,
            )
        }

        rule.runOnIdle {
            assertThat(
                positionIndicatorState.positionFraction
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isEqualTo(1)
        }
    }

    @Test
    fun scrollableColumnGivesCorrectPositionAndSize() {
        lateinit var state: ScrollState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScrollState()
            positionIndicatorState = ScrollStateAdapter(state)
            Column(
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredHeight(itemSizeDp * 3f + itemSpacingDp * 2f)
                    .fillMaxWidth()
                    .verticalScroll(state = state)
                    .background(Color.Black),
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),

                ) {
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + itemSpacingPx.toFloat())
            }

            // And that the indicator is at position 0.5 and of expected size
            assertThat(
                positionIndicatorState.positionFraction
            ).isWithin(0.05f).of(0.5f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)
        }
    }

    @Test
    fun reversedScrollableColumnGivesCorrectPositionAndSize() {
        lateinit var state: ScrollState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScrollState()
            positionIndicatorState = ScrollStateAdapter(scrollState = state)
            Column(
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredHeight(itemSizeDp * 3f + itemSpacingDp * 2f)
                    .fillMaxWidth()
                    .verticalScroll(state = state, reverseScrolling = true)
                    .background(Color.Black),
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
            ) {
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
                reverseDirection = true,
            )
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + itemSpacingPx.toFloat())
            }

            // And that the indicator is at position 0 and of expected size
            assertThat(
                positionIndicatorState.positionFraction
            ).isWithin(0.05f).of(0.5f)
            assertThat(
                positionIndicatorState.sizeFraction(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)
        }
    }

    @Test
    fun rsbPositionIndicatorGivesCorrectPositionAndSize() {
        val state = mutableStateOf(0f)
        val positionIndicatorState = FractionPositionIndicatorState { state.value }

        rule.setContent {
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            assertThat(positionIndicatorState.positionFraction).isWithin(0.05f).of(1.0f)
            assertThat(positionIndicatorState.sizeFraction(0f)).isWithin(0.05f).of(0.0f)

            state.value = 0.5f

            assertThat(positionIndicatorState.positionFraction).isWithin(0.05f).of(1.0f)
            assertThat(positionIndicatorState.sizeFraction(0f)).isWithin(0.05f).of(0.5f)

            state.value = 1.0f

            assertThat(positionIndicatorState.positionFraction).isWithin(0.05f).of(1.0f)
            assertThat(positionIndicatorState.sizeFraction(0f)).isWithin(0.05f).of(1.0f)
        }
    }

    private fun LazyListLayoutInfo.assertWhollyVisibleItems(
        firstItemIndex: Int,
        firstItemNotVisible: Int = 0,
        lastItemIndex: Int,
        lastItemNotVisible: Int = 0,
        viewPortHeight: Int
    ) {
        assertThat(visibleItemsInfo.first().index).isEqualTo(firstItemIndex)
        assertThat(visibleItemsInfo.first().offset).isEqualTo(firstItemNotVisible)
        assertThat(visibleItemsInfo.last().index).isEqualTo(lastItemIndex)
        assertThat(
            viewPortHeight - (visibleItemsInfo.last().offset + visibleItemsInfo.last().size)
        ).isEqualTo(lastItemNotVisible)
    }

    private fun ScalingLazyListLayoutInfo.assertWhollyVisibleItems(
        firstItemIndex: Int,
        lastItemIndex: Int,
        viewPortHeight: Int
    ) {
        assertThat(visibleItemsInfo.first().index).isEqualTo(firstItemIndex)
        assertThat(visibleItemsInfo.last().index).isEqualTo(lastItemIndex)
        assertThat((viewPortHeight / 2f) >=
            (visibleItemsInfo.last().offset + (visibleItemsInfo.last().size / 2)))
    }
}
