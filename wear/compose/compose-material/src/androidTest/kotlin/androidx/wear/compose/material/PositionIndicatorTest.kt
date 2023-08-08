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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.lazy.AutoCenteringParams as AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn as ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults as ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListLayoutInfo as ScalingLazyListLayoutInfo
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope as ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState as ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState as rememberScalingLazyListState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
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
        scalingLazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(itemSpacingDp)
        ) {}
    }

    @Test
    fun scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize(itemSizeDp)
    }

    @Test
    fun scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSizeForZeroSizeItems() {
        scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize(0.dp)
    }

    private fun scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize(itemSize: Dp) {
        scalingLazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(itemSpacingDp),
            autoCentering = null
        ) {
            items(3) {
                Box(Modifier.requiredSize(itemSize))
            }
        }
    }

    @Test
    fun scalingLazyColumnNotLargeEnoughToScrollSwapVerticalAlignmentGivesCorrectPositionAndSize() {
        scalingLazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(
                space = itemSpacingDp,
                alignment = Alignment.Bottom
            )
        ) {
            items(3) {
                Box(Modifier.requiredSize(itemSizeDp))
            }
        }
    }

    private fun scalingLazyColumnNotLargeEnoughToScroll(
        verticalArrangement: Arrangement.Vertical,
        reverseLayout: Boolean = false,
        autoCentering: AutoCenteringParams? = AutoCenteringParams(),
        content: ScalingLazyListScope.() -> Unit
    ) {
        lateinit var state: ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyListState()
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = verticalArrangement,
                reverseLayout = reverseLayout,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f),
                autoCentering = autoCentering
            ) {
                content(this)
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingHorizontal = 5.dp,
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
    fun scrollableScalingLazyColumnGivesCorrectPositionAndSize() {
        scrollableScalingLazyColumnPositionAndSize(
            enableAutoCentering = true,
            contentPaddingPx = 0
        )
    }

    @Test
    fun scrollableScalingLazyColumnGivesCorrectPositionAndSizeWithContentPadding() {
        scrollableScalingLazyColumnPositionAndSize(
            enableAutoCentering = true,
            contentPaddingPx = itemSizePx + itemSpacingPx
        )
    }

    @Test
    fun scrollableScalingLazyColumnGivesCorrectPositionAndSizeWithContentPaddingNoAutoCenter() {
        scrollableScalingLazyColumnPositionAndSize(
            enableAutoCentering = false,
            contentPaddingPx = itemSizePx + itemSpacingPx
        )
    }

    private fun scrollableScalingLazyColumnPositionAndSize(
        enableAutoCentering: Boolean,
        contentPaddingPx: Int
    ) {
        lateinit var state: ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberScalingLazyListState(initialCenterItemIndex = 0)
            positionIndicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredHeight(
                        // Exactly the right size to hold 3 items with spacing
                        itemSizeDp * 3f + itemSpacingDp * 2f
                    )
                    .background(Color.Black),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeScale = 1.0f),
                autoCentering = if (enableAutoCentering)
                    AutoCenteringParams(itemIndex = 0) else null,
                contentPadding = with(LocalDensity.current) {
                    PaddingValues(contentPaddingPx.toDp())
                }
            ) {
                items(5) {
                    Box(
                        Modifier
                            .requiredSize(itemSizeDp)
                            .border(BorderStroke(1.dp, Color.Green))
                    )
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingHorizontal = 5.dp,
            )
        }

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
        scalingLazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(itemSpacingDp),
            reverseLayout = true
        ) {}
    }

    @Test
    fun reverseLayoutScalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        scalingLazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(
                space = itemSpacingDp,
                alignment = Alignment.Bottom
            ),
            autoCentering = null,
            reverseLayout = true
        ) {
            items(3) {
                Box(Modifier.requiredSize(itemSizeDp))
            }
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
                autoCentering = null
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingHorizontal = 5.dp,
            )
        }

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
        lazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(itemSpacingDp)
        ) {}
    }

    @Test
    fun lazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        lazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(itemSpacingDp)
        ) {
            items(3) {
                Box(Modifier.requiredSize(itemSizeDp))
            }
        }
    }

    @Test
    fun lazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSizeForZeroSizeItems() {
        lazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(itemSpacingDp)
        ) {
            items(3) {
                Box(Modifier.requiredSize(0.dp))
            }
        }
    }

    @Test
    fun lazyColumnNotLargeEnoughToScrollSwapVerticalAlignmentGivesCorrectPositionAndSize() {
        lazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(
                space = itemSpacingDp,
                alignment = Alignment.Bottom
            )
        ) {
            items(3) {
                Box(Modifier.requiredSize(itemSizeDp))
            }
        }
    }

    private fun lazyColumnNotLargeEnoughToScroll(
        verticalArrangement: Arrangement.Vertical,
        reverseLayout: Boolean = false,
        content: LazyListScope.() -> Unit
    ) {
        lateinit var state: LazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = rememberLazyListState()
            positionIndicatorState = LazyColumnStateAdapter(state)
            LazyColumn(
                state = state,
                verticalArrangement = verticalArrangement,
                reverseLayout = reverseLayout,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f)
            ) {
                content()
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingHorizontal = 5.dp,
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
                paddingHorizontal = 5.dp,
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
        lazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(itemSpacingDp),
            reverseLayout = true
        ) {}
    }

    @Test
    fun reverseLayoutLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        lazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(
                space = itemSpacingDp,
                alignment = Alignment.Bottom
            ),
            reverseLayout = true
        ) {
            items(3) {
                Box(Modifier.requiredSize(itemSizeDp))
            }
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
                paddingHorizontal = 5.dp,
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
        scrollableColumnNotLargeEnoughToScroll({})
    }

    @Test
    fun emptyReversedScrollableColumnGivesCorrectPositionAndSize() {
        scrollableColumnNotLargeEnoughToScroll({}, reverseScrolling = true)
    }

    @Test
    fun scrollableColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        scrollableColumnNotLargeEnoughToScroll(
            {
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
            },
            Arrangement.spacedBy(itemSpacingDp),
        )
    }

    @Test
    fun scrollableColumnNotLargeEnoughToScrollGivesCorrectPositionAndSizeForZeroSizeItems() {
        scrollableColumnNotLargeEnoughToScroll(
            {
                Box(Modifier.requiredSize(0.dp))
                Box(Modifier.requiredSize(0.dp))
                Box(Modifier.requiredSize(0.dp))
            },
            Arrangement.spacedBy(itemSpacingDp),
        )
    }

    @Test
    fun reversedScrollableColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        scrollableColumnNotLargeEnoughToScroll(
            {
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
                Box(Modifier.requiredSize(itemSizeDp))
            },
            Arrangement.spacedBy(itemSpacingDp),
            reverseScrolling = true
        )
    }

    private fun scrollableColumnNotLargeEnoughToScroll(
        columnContent: @Composable ColumnScope.() -> Unit,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        reverseScrolling: Boolean = false
    ) {
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
                    .verticalScroll(state = state, reverseScrolling = reverseScrolling),
                verticalArrangement = verticalArrangement
            ) {
                columnContent()
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingHorizontal = 5.dp,
                reverseDirection = reverseScrolling
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
                paddingHorizontal = 5.dp,
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
                paddingHorizontal = 5.dp,
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
                paddingHorizontal = 5.dp,
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
        assertThat(
            (viewPortHeight / 2f) >=
                (visibleItemsInfo.last().offset + (visibleItemsInfo.last().size / 2))
        )
    }
}

/**
 * Tests for PositionIndicator api which uses deprecated ScalingLazyColumn
 * from androidx.wear.compose.material package.
 */
@MediumTest
@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
public class PositionIndicatorWithMaterialSLCTest {
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
        scalingLazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(itemSpacingDp)
        ) {}
    }

    @Test
    fun scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize(itemSizeDp)
    }

    @Test
    fun scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSizeForZeroSizeItems() {
        scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize(0.dp)
    }

    private fun scalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize(itemSize: Dp) {
        scalingLazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(itemSpacingDp),
            autoCentering = null
        ) {
            items(3) {
                Box(Modifier.requiredSize(itemSize))
            }
        }
    }

    @Test
    fun scalingLazyColumnNotLargeEnoughToScrollSwapVerticalAlignmentGivesCorrectPositionAndSize() {
        scalingLazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(
                space = itemSpacingDp,
                alignment = Alignment.Bottom
            )
        ) {
            items(3) {
                Box(Modifier.requiredSize(itemSizeDp))
            }
        }
    }

    fun scrollableScalingLazyColumnGivesCorrectPositionAndSize() {
        scrollableScalingLazyColumnPositionAndSize(
            enableAutoCentering = true,
            contentPaddingPx = 0
        )
    }

    @Test
    fun scrollableScalingLazyColumnGivesCorrectPositionAndSizeWithContentPadding() {
        scrollableScalingLazyColumnPositionAndSize(
            enableAutoCentering = true,
            contentPaddingPx = itemSizePx + itemSpacingPx
        )
    }

    @Test
    fun scrollableScalingLazyColumnGivesCorrectPositionAndSizeWithContentPaddingNoAutoCenter() {
        scrollableScalingLazyColumnPositionAndSize(
            enableAutoCentering = false,
            contentPaddingPx = itemSizePx + itemSpacingPx
        )
    }

    private fun scrollableScalingLazyColumnPositionAndSize(
        enableAutoCentering: Boolean,
        contentPaddingPx: Int
    ) {
        lateinit var state: androidx.wear.compose.material.ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state =
                androidx.wear.compose.material.rememberScalingLazyListState(
                    initialCenterItemIndex = 0
                )
            positionIndicatorState = MaterialScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredHeight(
                        // Exactly the right size to hold 3 items with spacing
                        itemSizeDp * 3f + itemSpacingDp * 2f
                    )
                    .background(Color.Black),
                scalingParams =
                androidx.wear.compose.material.ScalingLazyColumnDefaults.scalingParams(
                    edgeScale = 1.0f
                ),
                autoCentering = if (enableAutoCentering)
                    androidx.wear.compose.material.AutoCenteringParams(itemIndex = 0) else null,
                contentPadding = with(LocalDensity.current) {
                    PaddingValues(contentPaddingPx.toDp())
                }
            ) {
                items(5) {
                    Box(
                        Modifier
                            .requiredSize(itemSizeDp)
                            .border(BorderStroke(1.dp, Color.Green))
                    )
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingHorizontal = 5.dp,
            )
        }

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
        scalingLazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(itemSpacingDp),
            reverseLayout = true
        ) {}
    }

    @Test
    fun reverseLayoutScalingLazyColumnNotLargeEnoughToScrollGivesCorrectPositionAndSize() {
        scalingLazyColumnNotLargeEnoughToScroll(
            Arrangement.spacedBy(
                space = itemSpacingDp,
                alignment = Alignment.Bottom
            ),
            autoCentering = null,
            reverseLayout = true
        ) {
            items(3) {
                Box(Modifier.requiredSize(itemSizeDp))
            }
        }
    }

    private fun scalingLazyColumnNotLargeEnoughToScroll(
        verticalArrangement: Arrangement.Vertical,
        reverseLayout: Boolean = false,
        autoCentering: androidx.wear.compose.material.AutoCenteringParams? =
            androidx.wear.compose.material.AutoCenteringParams(),
        slcContent: androidx.wear.compose.material.ScalingLazyListScope.() -> Unit
    ) {
        lateinit var state: androidx.wear.compose.material.ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = androidx.wear.compose.material.rememberScalingLazyListState()
            positionIndicatorState = MaterialScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = verticalArrangement,
                reverseLayout = reverseLayout,
                modifier = Modifier
                    .onSizeChanged { viewPortHeight = it.height }
                    .requiredSize(itemSizeDp * 3.5f + itemSpacingDp * 2.5f),
                autoCentering = autoCentering
            ) {
                slcContent(this)
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingHorizontal = 5.dp,
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
    fun reverseLayoutScrollableScalingLazyColumnGivesCorrectPositionAndSize() {
        lateinit var state: androidx.wear.compose.material.ScalingLazyListState
        lateinit var positionIndicatorState: PositionIndicatorState
        var viewPortHeight = 0
        rule.setContent {
            state = androidx.wear.compose.material.rememberScalingLazyListState()
            positionIndicatorState = MaterialScalingLazyColumnStateAdapter(state)
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
                scalingParams = androidx.wear.compose.material.ScalingLazyColumnDefaults
                    .scalingParams(edgeScale = 1.0f),
                autoCentering = null
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            PositionIndicator(
                state = positionIndicatorState,
                indicatorHeight = 50.dp,
                indicatorWidth = 4.dp,
                paddingHorizontal = 5.dp,
            )
        }

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

    private fun androidx.wear.compose.material.ScalingLazyListLayoutInfo.assertWhollyVisibleItems(
        firstItemIndex: Int,
        lastItemIndex: Int,
        viewPortHeight: Int
    ) {
        assertThat(visibleItemsInfo.first().index).isEqualTo(firstItemIndex)
        assertThat(visibleItemsInfo.last().index).isEqualTo(lastItemIndex)
        assertThat(
            (viewPortHeight / 2f) >=
                (visibleItemsInfo.last().offset + (visibleItemsInfo.last().size / 2))
        )
    }
}
