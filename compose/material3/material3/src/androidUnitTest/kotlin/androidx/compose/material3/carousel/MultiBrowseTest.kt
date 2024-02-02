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

import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MultiBrowseTest {

    private val Density = Density(1f)

    @Test
    fun testMultiBrowse_doesNotResizeLargeWhenEnoughRoom() {
        val itemSize = 120f // minSmallItemSize = 40.dp * 3
        val keylineList = multiBrowseKeylineList(
            density = Density,
            carouselMainAxisSize = 500f,
            preferredItemSize = itemSize,
            itemSpacing = 0f
        )!!
        val strategy = Strategy { keylineList }.apply(500f)

        assertThat(strategy.itemMainAxisSize).isEqualTo(itemSize)
    }

    @Test
    fun testMultiBrowse_resizesItemLargerThanContainerToFit1Small() {
        val itemSize = 200f
        val keylineList = multiBrowseKeylineList(
            density = Density,
            carouselMainAxisSize = 100f,
            preferredItemSize = itemSize,
            itemSpacing = 0f
        )!!
        val strategy = Strategy { keylineList }.apply(100f)
        val minSmallItemSize: Float = with(Density) { StrategyDefaults.MinSmallSize.toPx() }
        val keylines = strategy.getDefaultKeylines()

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
        val minSmallItemSize: Float = with(Density) { StrategyDefaults.MinSmallSize.toPx() }
        val keylineList = multiBrowseKeylineList(
            density = Density,
            carouselMainAxisSize = minSmallItemSize,
            preferredItemSize = 200f,
            itemSpacing = 0f
        )!!
        val strategy = Strategy { keylineList }.apply(minSmallItemSize)
        val keylines = strategy.getDefaultKeylines()

        assertThat(strategy.itemMainAxisSize).isEqualTo(minSmallItemSize)
        assertThat(keylines.firstFocal == keylines.firstNonAnchor)
        assertThat(keylines.lastFocal == keylines.lastNonAnchor)
    }

    @Test
    fun testMultiBrowse_isNullIfAvailableSpaceIsZero() {
        val keylineList = multiBrowseKeylineList(
            density = Density,
            carouselMainAxisSize = 0f,
            preferredItemSize = 200f,
            itemSpacing = 0f
        )
        assertThat(keylineList).isNull()
    }

    @Test
    fun testMultiBrowse_adjustsMediumSizeToBeProportional() {
        val maxSmallItemSize: Float = with(Density) { StrategyDefaults.MaxSmallSize.toPx() }
        val preferredItemSize = 200f
        val carouselSize = preferredItemSize * 2 + maxSmallItemSize * 2
        val keylineList = multiBrowseKeylineList(
            density = Density,
            carouselMainAxisSize = carouselSize,
            preferredItemSize = preferredItemSize,
            itemSpacing = 0f
        )!!
        val strategy = Strategy { keylineList }.apply(carouselSize)
        val keylines = strategy.getDefaultKeylines()

        // Assert that there's only one small item, and a medium item that has a size between
        // the large and small items
        // We expect a keyline list of [xSmall-Large-Large-Medium-Small-xSmall]
        assertThat(keylines).hasSize(6)
        assertThat(keylines[1].isFocal).isTrue()
        assertThat(keylines[2].isFocal).isTrue()
        assertThat(keylines[3].size).isLessThan(keylines[2].size)
        assertThat(keylines[4].size).isLessThan(keylines[3].size)
    }
}
