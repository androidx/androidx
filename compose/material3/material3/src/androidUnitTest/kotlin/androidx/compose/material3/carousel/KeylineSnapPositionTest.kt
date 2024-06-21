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

package androidx.compose.material3.carousel

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KeylineSnapPositionTest {

    @Test
    fun testSnapPosition_forCenterAlignedStrategy() {
        val itemCount = 6
        val strategy = testCenterAlignedStrategy()
        val expectedSnapPositions = arrayListOf(0, 200, 300, 300, 400, 600)
        repeat(itemCount) { i ->
            assertThat(getSnapPositionOffset(strategy, i, itemCount))
                .isEqualTo(expectedSnapPositions[i])
        }
    }

    @Test
    fun testSnapPosition_forStartAlignedStrategy() {
        val itemCount = 6
        val strategy = testStartAlignedStrategy()
        val expectedSnapPositions = arrayListOf(0, 0, 100, 200, 400, 600)
        repeat(itemCount) { i ->
            assertThat(getSnapPositionOffset(strategy, i, itemCount))
                .isEqualTo(expectedSnapPositions[i])
        }
    }

    @Test
    fun testSnapPosition_forStartAlignedStrategyWithMultipleFocal() {
        val itemCount = 5
        val strategy = testStartAlignedStrategyWithMultipleFocal()
        val expectedSnapPositions = arrayListOf(0, 0, 75, 200, 200)
        repeat(itemCount) { i ->
            assertThat(getSnapPositionOffset(strategy, i, itemCount))
                .isEqualTo(expectedSnapPositions[i])
        }
    }

    @Test
    fun testSnapPosition_forStartAlignedStrategyWithMultipleFocalAndLessItems() {
        val strategy = testStartAlignedStrategyWithMultipleFocal()
        // item count is the number of keylines minus anchor keylines
        val itemCount = strategy.defaultKeylines.size - 2
        val expectedSnapPositions = arrayListOf(0, 75, 200, 200)
        repeat(itemCount) { i ->
            assertThat(getSnapPositionOffset(strategy, i, itemCount))
                .isEqualTo(expectedSnapPositions[i])
        }
    }

    @Test
    fun testSnapPosition_forStartAlignedStrategyWithLessItemsThanFocal() {
        val strategy = testStartAlignedStrategyWithMultipleFocal()
        // item count is the number of focal keylines minus one
        val itemCount =
            strategy.defaultKeylines.lastFocalIndex - strategy.defaultKeylines.firstFocalIndex
        repeat(itemCount) { i ->
            assertThat(getSnapPositionOffset(strategy, i, itemCount)).isEqualTo(0)
        }
    }

    // Test strategy that is center aligned and has a complex keyline state, ie:
    // [xsmall - small - medium - large - medium - small - xsmall]
    // In this case, we expect this:
    // snap position at item 0: [xs-l-m-m-s-s-xs]
    // snap position at item 1: [xs-m-l-m-s-s-xs]
    // snap position at item 2: [xs-s-m-l-m-s-xs]
    // ...
    // snap position at third last item: [xs-s-m-l-m-s-xs]
    // snap position at second last item: [xs-s-m-m-l-m-xs]
    // snap position at last item: [xs-s-s-m-m-l-xs]
    private fun testCenterAlignedStrategy(): Strategy {
        val xSmallSize = 5f
        val smallSize = 100f
        val mediumSize = 200f
        val largeSize = 400f
        val keylineList =
            keylineListOf(
                carouselMainAxisSize = 1000f,
                itemSpacing = 0f,
                carouselAlignment = CarouselAlignment.Center
            ) {
                add(xSmallSize, isAnchor = true)
                add(smallSize)
                add(mediumSize)
                add(largeSize)
                add(mediumSize)
                add(smallSize)
                add(xSmallSize, isAnchor = true)
            }

        return Strategy(
            defaultKeylines = keylineList,
            availableSpace = 1000f,
            itemSpacing = 0f,
            beforeContentPadding = 0f,
            afterContentPadding = 0f
        )
    }

    // Test strategy that is start aligned:
    // [xs - large - medium - medium - small - small - xsmall]
    // In this case, we expect this:
    // snap position at item 0: [xs-l-m-m-s-s-xs]
    // snap position at item 1: [xs-l-m-m-s-s-xs]
    // snap position at item 2: [xs-s-l-m-m-s-xs]
    // snap position at item 3: [xs-s-s-l-m-m-xs]
    // snap position at item 4: [xs-s-s-m-l-m-xs]
    // snap position at item 5: [xs-s-s-m-m-l-xs]
    private fun testStartAlignedStrategy(): Strategy {
        val xSmallSize = 5f
        val smallSize = 100f
        val mediumSize = 200f
        val largeSize = 400f
        val keylineList =
            keylineListOf(
                carouselMainAxisSize = 1000f,
                itemSpacing = 0f,
                carouselAlignment = CarouselAlignment.Start
            ) {
                add(xSmallSize, isAnchor = true)
                add(largeSize)
                add(mediumSize)
                add(mediumSize)
                add(smallSize)
                add(smallSize)
                add(xSmallSize, isAnchor = true)
            }
        return Strategy(
            defaultKeylines = keylineList,
            availableSpace = 1000f,
            itemSpacing = 0f,
            beforeContentPadding = 0f,
            afterContentPadding = 0f
        )
    }

    // Test strategy that is start aligned:
    // [xs - large - large - medium - small - xsmall]
    // In this case, we expect this:
    // snap position at item 0: [xs-l-l-m-s-xs]
    // snap position at third last item: [xs-s-l-l-m-xs]
    // snap position at second last item: [xs-s-m-l-l-xs]
    // snap position at last item: [xs-s-m-l-l-xs]
    private fun testStartAlignedStrategyWithMultipleFocal(): Strategy {
        val xSmallSize = 5f
        val smallSize = 75f
        val mediumSize = 125f
        val largeSize = 400f
        val keylineList =
            keylineListOf(carouselMainAxisSize = 1000f, 0f, CarouselAlignment.Start) {
                add(xSmallSize, isAnchor = true)
                add(largeSize)
                add(largeSize)
                add(mediumSize)
                add(smallSize)
                add(xSmallSize, isAnchor = true)
            }
        return Strategy(
            defaultKeylines = keylineList,
            availableSpace = 1000f,
            itemSpacing = 0f,
            beforeContentPadding = 0f,
            afterContentPadding = 0f
        )
    }
}
