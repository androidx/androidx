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
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MultiBrowseStrategyTest {

    private val Density = Density(1f)

    @Test
    fun testStrategy_doesntResizeLargeWhenEnoughRoom() {

        val itemSize = 120.dp // minSmallItemSize = 40.dp * 3
        val strategyProvider = MultiBrowseStrategyProvider(itemSize)
        val strategy = strategyProvider.createStrategy(
            density = Density,
            carouselMainAxisSize = 500f,
            itemSpacing = 0)

        assertThat(strategy?.itemMainAxisSize).isEqualTo(with(Density) { itemSize.toPx() })
    }

    @Test
    fun testStrategy_resizesItemLargerThanContainerToFit1Small() {
        val itemSize = 200f
        val strategyProvider = MultiBrowseStrategyProvider(with(Density) { itemSize.toDp() })
        val strategy = strategyProvider.createStrategy(
            density = Density,
            carouselMainAxisSize = 100f,
            itemSpacing = 0)

        val minSmallItemSize: Float = with(Density) { StrategyDefaults.minSmallSize.toPx() }
        val keylines = strategy?.getDefaultKeylines()

        // If the item size given is larger than the container, the adjusted keyline list from
        // the MultibrowseStrategy should be [xSmall-Large-Small-xSmall]
        assertThat(strategy?.itemMainAxisSize).isAtMost(100f)
        assertThat(keylines).hasSize(4)
        assertThat(keylines?.get(0)?.unadjustedOffset).isLessThan(0f)
        assertThat(keylines?.get(keylines.lastIndex)?.unadjustedOffset).isGreaterThan(100f)
        assertThat(keylines?.get(1)?.isFocal).isTrue()
        assertThat(keylines?.get(2)?.size).isEqualTo(minSmallItemSize)
    }

    @Test
    fun testStrategy_hasNoSmallItemsIfNotEnoughRoom() {
        val minSmallItemSize: Float = with(Density) { StrategyDefaults.minSmallSize.toPx() }
        val strategyProvider = MultiBrowseStrategyProvider(with(Density) { 200f.toDp() })
        val strategy = strategyProvider.createStrategy(
            density = Density,
            carouselMainAxisSize = minSmallItemSize,
            itemSpacing = 0)
        val keylines = strategy?.getDefaultKeylines()

        assertThat(strategy?.itemMainAxisSize).isEqualTo(minSmallItemSize)
        assertThat(keylines?.firstFocal == keylines?.firstNonAnchor)
        assertThat(keylines?.lastFocal == keylines?.lastNonAnchor)
    }

    @Test
    fun testStrategy_isNullIfAvailableSpaceIsZero() {
        val strategyProvider = MultiBrowseStrategyProvider(with(Density) { 200f.toDp() })
        val strategy = strategyProvider.createStrategy(
            density = Density,
            carouselMainAxisSize = 0F,
            itemSpacing = 0)

        assertThat(strategy).isNull()
    }

    @Test
    fun testStrategy_adjustsMediumSizeToBeProportional() {
        val maxSmallItemSize: Float = with(Density) { StrategyDefaults.maxSmallSize.toPx() }
        val targetItemSize = 200f
        val carouselSize = targetItemSize * 2 + maxSmallItemSize * 2
        val strategyProvider = MultiBrowseStrategyProvider(with(Density) { targetItemSize.toDp() })
        val strategy = strategyProvider.createStrategy(
            density = Density,
            carouselMainAxisSize = carouselSize,
            itemSpacing = 0)
        val keylines = strategy?.getDefaultKeylines()

        // Assert that there's only one small item, and a medium item that has a size between
        // the large and small items
        // We expect a keyline list of [xSmall-Large-Large-Medium-Small-xSmall]
        assertThat(keylines).hasSize(6)
        assertThat(keylines?.get(1)?.isFocal).isTrue()
        assertThat(keylines?.get(2)?.isFocal).isTrue()
        assertThat(keylines?.get(3)?.size).isLessThan(keylines?.get(2)?.size)
        assertThat(keylines?.get(4)?.size).isLessThan(keylines?.get(3)?.size)
    }
}
