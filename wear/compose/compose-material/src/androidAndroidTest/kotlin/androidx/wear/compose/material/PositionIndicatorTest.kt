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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Ignore

@MediumTest
@RunWith(AndroidJUnit4::class)
public class PositionIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    private var itemSizePx: Int = 50
    private var itemSizeDp: Dp = Dp.Infinity
    private var defaultItemSpacingDp: Dp = 4.dp
    private var defaultItemSpacingPx = Int.MAX_VALUE

    @Before
    fun before() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
            defaultItemSpacingPx = defaultItemSpacingDp.roundToPx()
        }
    }

    @Test
    fun emptyScalingLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: ScalingLazyColumnState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyColumnState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
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
                positionIndicatorState.indicatorPosition()
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
            ).isEqualTo(1)
        }
    }

    @Test
    fun scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        lateinit var state: ScalingLazyColumnState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyColumnState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
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
                positionIndicatorState.indicatorPosition()
            ).isEqualTo(0f)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
            ).isEqualTo(1f)
        }
    }

    @Test
    @Ignore("1 pixel rounding error on some test AVDs b/197817612")
    fun scrollableScalingLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: ScalingLazyColumnState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyColumnState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            Box(modifier = Modifier.background(Color.Black)) {
                ScalingLazyColumn(
                    state = state,
                    modifier = Modifier
                        .onSizeChanged { viewPortHeight = it.height }
                        .requiredHeight(
                            // Exactly the right size to hold 3 items with spacing
                            itemSizeDp * 3f + defaultItemSpacingDp * 2f
                        )
                        .background(Color.Black),
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
        }

        rule.runOnIdle {
            state.layoutInfo.assertWhollyVisibleItems(
                firstItemIndex = 0, lastItemIndex = 2,
                viewPortHeight = viewPortHeight
            )

            // And that the indicator is at position 0 and of expected size
            assertThat(
                positionIndicatorState.indicatorPosition()
            ).isEqualTo(0f)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)

            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }

            state.layoutInfo.assertWhollyVisibleItems(
                firstItemIndex = 1, lastItemIndex = 3,
                viewPortHeight = viewPortHeight
            )

            // And that the indicator is at position 0.5 and of expected size
            assertThat(
                positionIndicatorState.indicatorPosition()
            ).isWithin(0.05f).of(0.5f)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)

            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }

            state.layoutInfo.assertWhollyVisibleItems(
                firstItemIndex = 2, lastItemIndex = 4,
                viewPortHeight = viewPortHeight
            )

            // And that the indicator is at position 0.5 and of expected size
            assertThat(
                positionIndicatorState.indicatorPosition()
            ).isWithin(0.05f).of(1.0f)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)
        }
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
                    .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
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
                positionIndicatorState.indicatorPosition()
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
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
                    .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
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
                positionIndicatorState.indicatorPosition()
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
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
                    .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
                    .verticalScroll(state = state),
                verticalArrangement = Arrangement.spacedBy(defaultItemSpacingDp)
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
                positionIndicatorState.indicatorPosition()
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
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
                    .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
                    .verticalScroll(state = state, reverseScrolling = true),
                verticalArrangement = Arrangement.spacedBy(defaultItemSpacingDp)
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
                positionIndicatorState.indicatorPosition()
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
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
                    .requiredHeight(itemSizeDp * 3f + defaultItemSpacingDp * 2f)
                    .fillMaxWidth()
                    .verticalScroll(state = state)
                    .background(Color.Black),
                verticalArrangement = Arrangement.spacedBy(defaultItemSpacingDp),

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
            // And that the indicator is at position 0 and of expected size
            assertThat(
                positionIndicatorState.indicatorPosition()
            ).isEqualTo(0)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)

            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }

            // And that the indicator is at position 0.5 and of expected size
            assertThat(
                positionIndicatorState.indicatorPosition()
            ).isWithin(0.05f).of(0.5f)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)

            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }

            // And that the indicator is at position 0.5 and of expected size
            assertThat(
                positionIndicatorState.indicatorPosition()
            ).isWithin(0.05f).of(1.0f)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
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
                    .requiredHeight(itemSizeDp * 3f + defaultItemSpacingDp * 2f)
                    .fillMaxWidth()
                    .verticalScroll(state = state, reverseScrolling = true)
                    .background(Color.Black),
                verticalArrangement = Arrangement.spacedBy(defaultItemSpacingDp),
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
            // And that the indicator is at position 0 and of expected size
            assertThat(
                positionIndicatorState.indicatorPosition()
            ).isWithin(0.05f).of(0.0f)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)

            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }

            // And that the indicator is at position 0 and of expected size
            assertThat(
                positionIndicatorState.indicatorPosition()
            ).isWithin(0.05f).of(0.5f)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)

            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }

            // And that the indicator is at position 0.5 and of expected size
            assertThat(
                positionIndicatorState.indicatorPosition()
            ).isWithin(0.05f).of(1.0f)
            assertThat(
                positionIndicatorState.indicatorSize(viewPortHeight.toFloat())
            ).isWithin(0.05f).of(0.6f)
        }
    }

    @Test
    fun rsbPositionIndicatorGivesCorrectPositionAndSize() {
        val state = mutableStateOf(0f)
        val positionIndicatorState = RsbPositionIndicatorState(state)

        rule.setContent {
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingRight = 5.dp,
            )
        }

        rule.runOnIdle {
            assertThat(positionIndicatorState.indicatorPosition()).isWithin(0.05f).of(1.0f)
            assertThat(positionIndicatorState.indicatorSize(0f)).isWithin(0.05f).of(0.0f)

            state.value = 0.5f

            assertThat(positionIndicatorState.indicatorPosition()).isWithin(0.05f).of(1.0f)
            assertThat(positionIndicatorState.indicatorSize(0f)).isWithin(0.05f).of(0.5f)

            state.value = 1.0f

            assertThat(positionIndicatorState.indicatorPosition()).isWithin(0.05f).of(1.0f)
            assertThat(positionIndicatorState.indicatorSize(0f)).isWithin(0.05f).of(1.0f)
        }
    }

    private fun ScalingLazyColumnLayoutInfo.assertWhollyVisibleItems(
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
}
