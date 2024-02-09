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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Creates a list of keylines that arranges items into a multi-browse configuration.
 *
 * Note that this function may adjust the size of large items. In order to ensure large, medium,
 * and small items fit perfectly into the available space and are numbered/arranged in a
 * visually pleasing and opinionated way, this strategy finds the nearest number of large items that
 * will fit into an approved arrangement that requires the least amount of size adjustment
 * necessary.
 *
 * For more information, see <a href="https://material.io/components/carousel/overview">design
 * guidelines</a>.
 *
 * @param density The [Density] object that provides pixel density information of the device
 * @param carouselMainAxisSize The carousel container's pixel size in the main scrolling axis
 * @param preferredItemSize the desired size of large items, in pixels, in the main scrolling axis
 * @param itemSpacing the spacing between items in pixels
 * @param minSmallSize the minimum allowable size of small items in pixels
 * @param maxSmallSize the maximum allowable size of small items in pixels
 */
internal fun multiBrowseKeylineList(
    density: Density,
    carouselMainAxisSize: Float,
    preferredItemSize: Float,
    itemSpacing: Float,
    minSmallSize: Float = with(density) { StrategyDefaults.MinSmallSize.toPx() },
    maxSmallSize: Float = with(density) { StrategyDefaults.MaxSmallSize.toPx() },
): KeylineList? {
    if (carouselMainAxisSize == 0f || preferredItemSize == 0f) {
        return null
    }

    var smallCounts: IntArray = intArrayOf(1)
    val mediumCounts: IntArray = intArrayOf(1, 0)

    val targetLargeSize: Float = min(preferredItemSize + itemSpacing, carouselMainAxisSize)
    // Ideally we would like to create a balanced arrangement where a small item is 1/3 the size
    // of the large item and medium items are sized between large and small items. Clamp the
    // small target size within our min-max range and as close to 1/3 of the target large item
    // size as possible.
    val targetSmallSize: Float = (targetLargeSize / 3f + itemSpacing).coerceIn(
        minSmallSize + itemSpacing,
        maxSmallSize + itemSpacing
    )
    val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f

    if (carouselMainAxisSize < minSmallSize * 2) {
        // If the available space is too small to fit a large item and small item (where a large
        // item is bigger than a small item), allow arrangements with
        // no small items.
        smallCounts = intArrayOf(0)
    }

    // Find the minimum space left for large items after filling the carousel with the most
    // permissible medium and small items to determine a plausible minimum large count.
    val minAvailableLargeSpace = carouselMainAxisSize - targetMediumSize * mediumCounts.max() -
        maxSmallSize * smallCounts.max()
    val minLargeCount = max(
        1,
        floor(minAvailableLargeSpace / targetLargeSize).toInt())
    val maxLargeCount = ceil(carouselMainAxisSize / targetLargeSize).toInt()

    val largeCounts = IntArray(maxLargeCount - minLargeCount + 1) { maxLargeCount - it }
    val anchorSize = with(density) { StrategyDefaults.AnchorSize.toPx() }
    val arrangement = Arrangement.findLowestCostArrangement(
        availableSpace = carouselMainAxisSize,
        targetSmallSize = targetSmallSize,
        minSmallSize = minSmallSize,
        maxSmallSize = maxSmallSize,
        smallCounts = smallCounts,
        targetMediumSize = targetMediumSize,
        mediumCounts = mediumCounts,
        targetLargeSize = targetLargeSize,
        largeCounts = largeCounts,
    )

    return if (arrangement == null) {
        null
    } else {
        createStartAlignedKeylineList(
            carouselMainAxisSize = carouselMainAxisSize,
            anchorSize = anchorSize,
            arrangement = arrangement
        )
    }
}

internal fun createStartAlignedKeylineList(
    carouselMainAxisSize: Float,
    anchorSize: Float,
    arrangement: Arrangement
): KeylineList {
    return keylineListOf(carouselMainAxisSize, CarouselAlignment.Start) {
        add(anchorSize, isAnchor = true)

        repeat(arrangement.largeCount) { add(arrangement.largeSize) }
        repeat(arrangement.mediumCount) { add(arrangement.mediumSize) }
        repeat(arrangement.smallCount) { add(arrangement.smallSize) }

        add(anchorSize, isAnchor = true)
    }
}

/**
 * Creates a list of keylines that arranges items into the 'uncontained' configuration. This
 * configuration lays out as many items as it can in the given item size without getting cut off,
 * and with the remaining space adds a cut off item with size constraints to ensure enough motion
 * when scrolling off-screen.
 *
 * For more information, see <a href="https://material.io/components/carousel/overview">design
 * guidelines</a>.
 *
 * @param density The [Density] object that provides pixel density information of the device
 * @param carouselMainAxisSize The carousel container's pixel size in the main scrolling axis
 * @param itemSize the size of large items, in pixels, in the main scrolling axis
 * @param itemSpacing the spacing between items in pixels
 */
internal fun uncontainedKeylineList(
    density: Density,
    carouselMainAxisSize: Float,
    itemSize: Float,
    itemSpacing: Float,
): KeylineList? {
    if (carouselMainAxisSize == 0f || itemSize == 0f) {
        return null
    }

    val largeItemSize = min(itemSize + itemSpacing, carouselMainAxisSize)
    // Calculate how much space there is remaining after squeezing in as many large items as we can.
    val largeCount = max(1, floor(carouselMainAxisSize / largeItemSize).toInt())
    val remainingSpace: Float = carouselMainAxisSize - largeCount * largeItemSize

    val mediumCount = if (remainingSpace > 0) 1 else 0

    val defaultAnchorSize = with(density) { StrategyDefaults.AnchorSize.toPx() }
    val mediumItemSize = calculateMediumChildSize(
        minimumMediumSize = defaultAnchorSize,
        largeItemSize = largeItemSize,
        remainingSpace = remainingSpace)
    val arrangement = Arrangement(
        0,
        0F,
        0,
        mediumItemSize,
        mediumCount,
        largeItemSize,
        largeCount
    )

    val xSmallSize = min(defaultAnchorSize, itemSize)
    // Make the anchor size half the cut off item size to make the motion at the left closer
    // to the right where the cut off is.
    val anchorSize: Float = max(xSmallSize, mediumItemSize * 0.5f)
    return createStartAlignedKeylineList(
        carouselMainAxisSize = carouselMainAxisSize,
        anchorSize = anchorSize,
        arrangement = arrangement)
}

/**
 * Calculates a size of a medium item in the carousel that is not bigger than the large item
 * size, and arbitrarily chooses a size small enough such that there is a size disparity between
 * the medium and large sizes, but large enough to have a sufficient percentage cut off.
 */
private fun calculateMediumChildSize(
    minimumMediumSize: Float,
    largeItemSize: Float,
    remainingSpace: Float
): Float {
    // With the remaining space, we want to add a 'medium' size item that gets sufficiently
    // cut off. Ideally, it is large enough such that a third of the item is cut off, meaning
    // that it is 1.5x the remaining space.
    var mediumItemSize = minimumMediumSize
    val sizeWithThirdCutOff = remainingSpace * 1.5f
    mediumItemSize = max(sizeWithThirdCutOff, mediumItemSize)

    // If the medium child is larger than the threshold percentage of the large child size,
    // it's too similar and won't create sufficient motion when scrolling items between the large
    // items and the medium item.
    val largeItemThreshold: Float =
        largeItemSize * StrategyDefaults.MediumLargeItemDiffThreshold
    if (mediumItemSize > largeItemThreshold) {
        // Choose whichever is bigger between the maximum threshold of the medium child size, or
        // a size such that only 20% of the space is cut off.
        val sizeWithFifthCutOff = remainingSpace * 1.2f
        mediumItemSize = max(largeItemThreshold, sizeWithFifthCutOff)
    }
    return mediumItemSize
}
