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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(JUnit4::class)
class MultiBrowseTest {

    private val Density = Density(1f)

    @Test
    fun testMultiBrowse_doesNotResizeLargeWhenEnoughRoom() {
        val itemSize = 120f // minSmallItemSize = 40.dp * 3
        val keylineList =
            multiBrowseKeylineList(
                density = Density,
                carouselMainAxisSize = 500f,
                preferredItemSize = itemSize,
                itemSpacing = 0f,
                itemCount = 10,
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = 500f,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f,
            )

        assertThat(strategy.itemMainAxisSize).isEqualTo(itemSize)
    }

    @Test
    fun testMultiBrowse_resizesItemLargerThanContainerToFit1Small() {
        val itemSize = 200f
        val keylineList =
            multiBrowseKeylineList(
                density = Density,
                carouselMainAxisSize = 100f,
                preferredItemSize = itemSize,
                itemSpacing = 0f,
                itemCount = 10,
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = 100f,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f,
            )
        val minSmallItemSize: Float = with(Density) { CarouselDefaults.MinSmallItemSize.toPx() }
        val keylines = strategy.defaultKeylines

        // If the item size given is larger than the container, the adjusted keyline list from
        // the multi-browse keyline list should be [xSmall-Large-Small-xSmall]
        assertThat(strategy.itemMainAxisSize).isAtMost(100f)
        assertThat(keylines).hasSize(4)
        assertThat(keylines[0].unadjustedOffset).isLessThan(0f)
        assertThat(keylines[keylines.lastIndex].unadjustedOffset).isGreaterThan(100f)
        assertThat(keylines[1].isFocal).isTrue()
        assertThat(keylines[2].size).isEqualTo(minSmallItemSize)
    }

    @Test
    fun testMultiBrowse_hasNoSmallItemsIfNotEnoughRoom() {
        val minSmallItemSize: Float = with(Density) { CarouselDefaults.MinSmallItemSize.toPx() }
        val keylineList =
            multiBrowseKeylineList(
                density = Density,
                carouselMainAxisSize = minSmallItemSize,
                preferredItemSize = 200f,
                itemSpacing = 0f,
                itemCount = 10,
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = minSmallItemSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f,
            )
        val keylines = strategy.defaultKeylines

        assertThat(strategy.itemMainAxisSize).isEqualTo(minSmallItemSize)
        assertThat(keylines.firstFocal).isEqualTo(keylines.firstNonAnchor)
        assertThat(keylines.lastFocal).isEqualTo(keylines.lastNonAnchor)
    }

    @Test
    fun testMultiBrowse_isNullIfAvailableSpaceIsZero() {
        val keylineList =
            multiBrowseKeylineList(
                density = Density,
                carouselMainAxisSize = 0f,
                preferredItemSize = 200f,
                itemSpacing = 0f,
                itemCount = 10,
            )
        assertThat(keylineList).isEmpty()
    }

    @Test
    fun testMultiBrowse_adjustsMediumSizeToBeProportional() {
        val maxSmallItemSize: Float = with(Density) { CarouselDefaults.MaxSmallItemSize.toPx() }
        val preferredItemSize = 200f
        val carouselSize = preferredItemSize * 2 + maxSmallItemSize * 2
        val keylineList =
            multiBrowseKeylineList(
                density = Density,
                carouselMainAxisSize = carouselSize,
                preferredItemSize = preferredItemSize,
                itemSpacing = 0f,
                itemCount = 10,
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = carouselSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f,
            )
        val keylines = strategy.defaultKeylines

        // Assert that there's only one small item, and a medium item that has a size between
        // the large and small items
        // We expect a keyline list of [xSmall-Large-Large-Medium-Small-xSmall]
        assertThat(keylines).hasSize(6)
        assertThat(keylines[1].isFocal).isTrue()
        assertThat(keylines[2].isFocal).isTrue()
        assertThat(keylines[3].size).isLessThan(keylines[2].size)
        assertThat(keylines[4].size).isLessThan(keylines[3].size)
    }

    @Test
    fun testMultiBrowse_withLessItemsThanKeylines() {
        val maxSmallItemSize: Float = with(Density) { CarouselDefaults.MaxSmallItemSize.toPx() }
        val preferredItemSize = 200f
        val carouselSize = preferredItemSize * 2 + maxSmallItemSize * 2
        val keylineList =
            multiBrowseKeylineList(
                density = Density,
                carouselMainAxisSize = carouselSize,
                preferredItemSize = preferredItemSize,
                itemSpacing = 0f,
                itemCount = 3,
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = carouselSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f,
            )
        val keylines = strategy.defaultKeylines

        // We originally expect a keyline list of [xSmall-Large-Large-Medium-Small-xSmall], but with
        // a maximum keyline restriction of 3, we now expect [xSmall-Large-Large-Small-xSmall]
        assertThat(keylines).hasSize(5)
        assertThat(keylines[1].isFocal).isTrue()
        assertThat(keylines[2].isFocal).isTrue()
        assertThat(keylines[3].size).isLessThan(keylines[2].size)
    }

    @Test
    fun testMultiBrowse_adjustsForItemSpacing() {
        val keylineList =
            multiBrowseKeylineList(
                density = Density,
                carouselMainAxisSize = 380f,
                preferredItemSize = 186f,
                itemSpacing = 8f,
                itemCount = 10,
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = 380f,
                itemSpacing = 8f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f,
            )

        assertThat(keylineList.firstFocal.size).isEqualTo(186f)
        // Ensure the first visible item is large and aligned with the start of the container
        assertThat(keylineList.firstFocal.offset).isEqualTo(186f / 2)
        // Ensure the last  visible item is aligned with the end of the container
        assertThat(keylineList.lastNonAnchor.offset + (keylineList.lastNonAnchor.size / 2f))
            .isEqualTo(380f)

        assertThat(strategy.itemMainAxisSize).isEqualTo(186f)
        val lastVisible = strategy.defaultKeylines[3]
        assertThat(lastVisible.size).isEqualTo(56f)
        assertThat(lastVisible.offset).isEqualTo(380f - (56f / 2f))

        val maxScrollOffset = ((186f * 10) + (8f * 10)) - 380f
        val defaultActualUnadjustedOffsets =
            strategy
                .getKeylineListForScrollOffset(0f, maxScrollOffset)
                .map { it.unadjustedOffset }
                .toFloatArray()
        val defaultExpectedUnadjustedOffsets = floatArrayOf(-101f, 93f, 287f, 481f, 675f)
        assertThat(defaultActualUnadjustedOffsets).isEqualTo(defaultExpectedUnadjustedOffsets)
    }
}
