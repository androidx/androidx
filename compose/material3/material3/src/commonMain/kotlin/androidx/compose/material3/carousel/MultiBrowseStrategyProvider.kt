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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * A [StrategyProvider] that provides the multi-browse strategy, which fits large, medium, and small
 * items into a layout for quick browsing of multiple items at once.
 *
 * Note that this strategy may adjust the size of large items. In order to ensure large, medium,
 * and small items fit perfectly into the available space and are numbered/arranged in a
 * visually pleasing and opinionated way, this strategy finds the nearest number of large items that
 * will fit into an approved arrangement that requires the least amount of size adjustment
 * necessary.
 *
 * For more information, see <a href="https://material.io/components/carousel/overview">design
 * guidelines</a>.
 */
internal class MultiBrowseStrategyProvider(
    private val targetLargeItemMainAxisSize: Dp,
    private val minSmallSize: Dp = StrategyDefaults.minSmallSize,
    private val maxSmallSize: Dp = StrategyDefaults.maxSmallSize
) :
    StrategyProvider() {

    override fun createStrategy(
        density: Density,
        carouselMainAxisSize: Float,
        itemSpacing: Int,
    ): Strategy? {
        if (carouselMainAxisSize == 0f || targetLargeItemMainAxisSize == 0.dp) {
            return null
        }

        val targetLargeItemSize = with(density) { targetLargeItemMainAxisSize.toPx() }
        val minSmallItemSize = with(density) { minSmallSize.toPx() }
        val maxSmallItemSize = with(density) { maxSmallSize.toPx() }
        var smallCounts: IntArray = intArrayOf(1)
        val mediumCounts: IntArray = intArrayOf(1, 0)

        val targetLargeSize: Float = min(targetLargeItemSize + itemSpacing, carouselMainAxisSize)
        // Ideally we would like to create a balanced arrangement where a small item is 1/3 the size
        // of the large item and medium items are sized between large and small items. Clamp the
        // small target size within our min-max range and as close to 1/3 of the target large item
        // size as possible.
        val targetSmallSize: Float = (targetLargeItemSize / 3f + itemSpacing).coerceIn(
            minSmallItemSize + itemSpacing,
            maxSmallItemSize + itemSpacing
        )
        val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f

        if (carouselMainAxisSize < minSmallItemSize * 2) {
            // If the available space is too small to fit a large item and small item (where a large
            // item is bigger than a small item), allow arrangements with
            // no small items.
            smallCounts = intArrayOf(0)
        }

        // Find the minimum space left for large items after filling the carousel with the most
        // permissible medium and small items to determine a plausible minimum large count.
        val minAvailableLargeSpace = carouselMainAxisSize - targetMediumSize * mediumCounts.max() -
            maxSmallItemSize * smallCounts.max()
        val minLargeCount = max(
            1,
            floor(minAvailableLargeSpace / targetLargeSize).toInt())
        val maxLargeCount = ceil(carouselMainAxisSize / targetLargeSize).toInt()

        val largeCounts = IntArray(maxLargeCount - minLargeCount + 1) { maxLargeCount - it }
        val arrangement = Arrangement.findLowestCostArrangement(
            availableSpace = carouselMainAxisSize,
            targetSmallSize = targetSmallSize,
            minSmallSize = minSmallItemSize,
            maxSmallSize = maxSmallItemSize,
            smallCounts = smallCounts,
            targetMediumSize = targetMediumSize,
            mediumCounts = mediumCounts,
            targetLargeSize = targetLargeSize,
            largeCounts = largeCounts,
        ) ?: return null

        return createStartAlignedStrategy(
            availableSpace = carouselMainAxisSize,
            arrangement = arrangement,
            anchorSize = with(density) { StrategyDefaults.anchorSize.toPx() }
        )
    }
}
