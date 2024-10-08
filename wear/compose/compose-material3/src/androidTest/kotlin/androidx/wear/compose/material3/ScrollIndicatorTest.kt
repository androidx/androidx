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

package androidx.wear.compose.material3

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollIndicatorTest {
    @get:Rule val rule = createComposeRule()

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
    fun scalingLazyColumnStateAdapter_veryLongContent() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.2f,
            itemsCount = 40
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_longContent() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.2f,
            itemsCount = 15
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_mediumContent() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            itemsCount = 6
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_shortContent() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.8f,
            itemsCount = 3
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_veryShortContent() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.8f,
            itemsCount = 1
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_mediumContent_withContentPadding() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            itemsCount = 6,
            contentPaddingPx = itemSizePx + itemSpacingPx
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_veryLongContent_scrolled() {
        verifySlcScrollToCenter(expectedIndicatorSize = 0.2f, itemsCount = 40)
    }

    @Test
    fun scalingLazyColumnStateAdapter_longContent_scrolled() {
        verifySlcScrollToCenter(expectedIndicatorSize = 0.2f, itemsCount = 16)
    }

    @Test
    fun scalingLazyColumnStateAdapter_mediumContent_scrolled() {
        verifySlcScrollToCenter(expectedIndicatorSize = 0.5f, itemsCount = 6)
    }

    @Test
    fun scalingLazyColumnStateAdapter_shortContent_scrolled() {
        verifySlcScrollToCenter(expectedIndicatorSize = 0.75f, itemsCount = 4)
    }

    @Test
    fun scalingLazyColumnStateAdapter_mediumContent_reversed() {
        // ScrollIndicators state isn't affected by reverseLayout flag, only its
        // representation - that's why indicatorPosition remains 0.
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            reverseLayout = true,
            itemsCount = 6
        )
    }

    @Test
    fun lazyColumnStateAdapter_veryLongContent() {
        verifyLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.2f,
            itemsCount = 40
        )
    }

    @Test
    fun lazyColumnStateAdapter_longContent() {
        verifyLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.2f,
            itemsCount = 15
        )
    }

    @Test
    fun lazyColumnStateAdapter_mediumContent() {
        verifyLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            itemsCount = 6
        )
    }

    @Test
    fun lazyColumnStateAdapter_veryLongContent_scrolled() {
        verifyLazyColumnScrollToCenter(expectedIndicatorSize = 0.2f, itemsCount = 40)
    }

    @Test
    fun lazyColumnStateAdapter_longContent_scrolled() {
        verifyLazyColumnScrollToCenter(expectedIndicatorSize = 0.2f, itemsCount = 16)
    }

    @Test
    fun lazyColumnStateAdapter_mediumContent_scrolled() {
        verifyLazyColumnScrollToCenter(expectedIndicatorSize = 0.5f, itemsCount = 6)
    }

    @Test
    fun lazyColumnStateAdapter_shortContent_scrolled() {
        verifyLazyColumnScrollToCenter(expectedIndicatorSize = 0.75f, itemsCount = 4)
    }

    @Test
    fun lazyColumnStateAdapter_mediumContent_reversed() {
        // ScrollIndicators state isn't affected by reverseLayout flag, only its
        // representation - that's why indicatorPosition remains 0.
        verifyLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            reverseLayout = true,
            itemsCount = 6
        )
    }

    @Test
    fun transformingLazyColumnStateAdapter_veryLongContent() {
        verifyTransformingLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.2f,
            itemsCount = 40
        )
    }

    @Test
    fun transformingLazyColumnStateAdapter_longContent() {
        verifyTransformingLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.2f,
            itemsCount = 15
        )
    }

    @Test
    fun transformingLazyColumnStateAdapter_mediumContent() {
        verifyTransformingLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            itemsCount = 6
        )
    }

    @Test
    fun columnStateAdapter_veryLongContent() {
        verifyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.2f,
            itemsCount = 40
        )
    }

    @Test
    fun columnStateAdapter_longContent() {
        verifyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.2f,
            itemsCount = 15
        )
    }

    @Test
    fun columnStateAdapter_mediumContent() {
        verifyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            itemsCount = 6
        )
    }

    @Test
    fun columnStateAdapter_veryLongContent_scrolled() {
        verifyColumnScrollToCenter(expectedIndicatorSize = 0.2f, itemsCount = 40)
    }

    @Test
    fun columnStateAdapter_longContent_scrolled() {
        verifyColumnScrollToCenter(expectedIndicatorSize = 0.2f, itemsCount = 16)
    }

    @Test
    fun columnStateAdapter_mediumContent_scrolled() {
        verifyColumnScrollToCenter(expectedIndicatorSize = 0.5f, itemsCount = 6)
    }

    @Test
    fun columnStateAdapter_shortContent_scrolled() {
        verifyColumnScrollToCenter(expectedIndicatorSize = 0.75f, itemsCount = 4)
    }

    @Test
    fun columnStateAdapter_mediumContent_reversed() {
        // ScrollIndicators state isn't affected by reverseLayout flag, only its
        // representation - that's why indicatorPosition remains 0.
        verifyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            reverseLayout = true,
            itemsCount = 6
        )
    }

    private fun verifySlcScrollToCenter(expectedIndicatorSize: Float, itemsCount: Int) {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0.5f,
            expectedIndicatorSize = expectedIndicatorSize,
            // Scrolling by half of the list height, minus original central position of the list,
            // which is 1.5th item.
            scrollByItems = itemsCount / 2f - 1.5f,
            itemsCount = itemsCount
        )
    }

    private fun verifySlcPositionAndSize(
        expectedIndicatorPosition: Float,
        expectedIndicatorSize: Float,
        verticalArrangement: Arrangement.Vertical =
            Arrangement.spacedBy(space = itemSpacingDp, alignment = Alignment.Bottom),
        reverseLayout: Boolean = false,
        autoCentering: AutoCenteringParams? = AutoCenteringParams(),
        contentPaddingPx: Int = 0,
        scrollByItems: Float = 0f,
        itemsCount: Int = 0,
    ) {
        lateinit var state: ScalingLazyListState
        lateinit var indicatorState: IndicatorState
        rule.setContent {
            state = rememberScalingLazyListState()
            indicatorState = ScalingLazyColumnStateAdapter(state)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = verticalArrangement,
                reverseLayout = reverseLayout,
                modifier = Modifier.requiredSize(itemSizeDp * 3f + itemSpacingDp * 2f),
                autoCentering = autoCentering,
                contentPadding =
                    with(LocalDensity.current) { PaddingValues(contentPaddingPx.toDp()) }
            ) {
                items(itemsCount) { Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) }
            }
        }

        rule.runOnIdle {
            if (scrollByItems != 0f) {
                runBlocking {
                    state.scrollBy((itemSizePx.toFloat() + itemSpacingPx.toFloat()) * scrollByItems)
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(indicatorState.positionFraction)
                .isWithin(0.05f)
                .of(expectedIndicatorPosition)
            Truth.assertThat(indicatorState.sizeFraction).isWithin(0.05f).of(expectedIndicatorSize)
        }
    }

    private fun verifyLazyColumnScrollToCenter(expectedIndicatorSize: Float, itemsCount: Int) {
        verifyLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0.5f,
            expectedIndicatorSize = expectedIndicatorSize,
            // Scrolling by half of the list height, minus original central position of the list,
            // which is 1.5th item.
            scrollByItems = itemsCount / 2f - 1.5f,
            itemsCount = itemsCount
        )
    }

    private fun verifyLazyColumnPositionAndSize(
        expectedIndicatorPosition: Float,
        expectedIndicatorSize: Float,
        verticalArrangement: Arrangement.Vertical =
            Arrangement.spacedBy(space = itemSpacingDp, alignment = Alignment.Bottom),
        reverseLayout: Boolean = false,
        scrollByItems: Float = 0f,
        itemsCount: Int = 0,
    ) {
        lateinit var state: LazyListState
        lateinit var indicatorState: IndicatorState
        rule.setContent {
            state = rememberLazyListState()
            indicatorState = LazyColumnStateAdapter(state)
            LazyColumn(
                state = state,
                verticalArrangement = verticalArrangement,
                reverseLayout = reverseLayout,
                modifier = Modifier.requiredSize(itemSizeDp * 3f + itemSpacingDp * 2f),
            ) {
                items(itemsCount) {
                    Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) { Text("$it") }
                }
            }
        }

        rule.runOnIdle {
            if (scrollByItems != 0f) {
                runBlocking {
                    state.scrollBy((itemSizePx.toFloat() + itemSpacingPx.toFloat()) * scrollByItems)
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(indicatorState.positionFraction)
                .isWithin(0.05f)
                .of(expectedIndicatorPosition)
            Truth.assertThat(indicatorState.sizeFraction).isWithin(0.05f).of(expectedIndicatorSize)
        }
    }

    private fun verifyColumnScrollToCenter(expectedIndicatorSize: Float, itemsCount: Int) {
        verifyColumnPositionAndSize(
            expectedIndicatorPosition = 0.5f,
            expectedIndicatorSize = expectedIndicatorSize,
            // Scrolling by half of the list height, minus original central position of the list,
            // which is 1.5th item.
            scrollByItems = itemsCount / 2f - 1.5f,
            itemsCount = itemsCount
        )
    }

    private fun verifyColumnPositionAndSize(
        expectedIndicatorPosition: Float,
        expectedIndicatorSize: Float,
        verticalArrangement: Arrangement.Vertical =
            Arrangement.spacedBy(space = itemSpacingDp, alignment = Alignment.Bottom),
        reverseLayout: Boolean = false,
        scrollByItems: Float = 0f,
        itemsCount: Int = 0,
    ) {
        lateinit var state: ScrollState
        lateinit var indicatorState: IndicatorState
        var viewPortSize = IntSize.Zero
        rule.setContent {
            state = rememberScrollState()
            indicatorState = ScrollStateAdapter(state) { viewPortSize }
            Box(
                modifier =
                    Modifier.onSizeChanged { viewPortSize = it }
                        .requiredSize(itemSizeDp * 3f + itemSpacingDp * 2f)
            ) {
                Column(
                    verticalArrangement = verticalArrangement,
                    modifier =
                        Modifier.verticalScroll(state = state, reverseScrolling = reverseLayout)
                ) {
                    for (it in 0 until itemsCount) {
                        Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) { Text("$it") }
                    }
                }
            }
        }

        rule.runOnIdle {
            if (scrollByItems != 0f) {
                runBlocking {
                    state.scrollBy((itemSizePx.toFloat() + itemSpacingPx.toFloat()) * scrollByItems)
                }
            }
        }
        rule.runOnIdle {
            Truth.assertThat(indicatorState.positionFraction)
                .isWithin(0.05f)
                .of(expectedIndicatorPosition)
            Truth.assertThat(indicatorState.sizeFraction).isWithin(0.05f).of(expectedIndicatorSize)
        }
    }

    private fun verifyTransformingLazyColumnPositionAndSize(
        expectedIndicatorPosition: Float,
        expectedIndicatorSize: Float,
        verticalArrangement: Arrangement.Vertical =
            Arrangement.spacedBy(space = itemSpacingDp, alignment = Alignment.Bottom),
        scrollByItems: Float = 0f,
        itemsCount: Int = 0,
    ) {
        lateinit var state: LazyListState
        lateinit var indicatorState: IndicatorState
        rule.setContent {
            state = rememberLazyListState()
            indicatorState = LazyColumnStateAdapter(state)
            LazyColumn(
                state = state,
                verticalArrangement = verticalArrangement,
                modifier = Modifier.requiredSize(itemSizeDp * 3f + itemSpacingDp * 2f),
            ) {
                items(itemsCount) {
                    Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) { Text("$it") }
                }
            }
        }

        rule.runOnIdle {
            if (scrollByItems != 0f) {
                runBlocking {
                    state.scrollBy((itemSizePx.toFloat() + itemSpacingPx.toFloat()) * scrollByItems)
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(indicatorState.positionFraction)
                .isWithin(0.05f)
                .of(expectedIndicatorPosition)
            Truth.assertThat(indicatorState.sizeFraction).isWithin(0.05f).of(expectedIndicatorSize)
        }
    }
}
