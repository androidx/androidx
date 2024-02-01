/*
 * Copyright 2023 The Android Open Source Project
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

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A class that holds data about a combination of large, medium, and small items, knows how to alter
 * an arrangement to fit within an available space, and can assess the arrangement's
 * desirability according to a priority heuristic.
 */
internal class Arrangement(
    private val priority: Int,
    val smallSize: Float,
    val smallCount: Int,
    val mediumSize: Float,
    val mediumCount: Int,
    val largeSize: Float,
    val largeCount: Int
) {

    private fun isValid(): Boolean {
        if (largeCount > 0 && smallCount > 0 && mediumCount > 0) {
            return largeSize > mediumSize && mediumSize > smallSize
        } else if (largeCount > 0 && smallCount > 0) {
            return largeSize > smallSize
        }
        return true
    }

    /**
     * Calculates the cost of this arrangement to determine visual desirability and adherence to
     * inputs. Cost is a value calculated based on the arrangement's `priority` and how true it is
     * to the target large size.
     * Arrangements have a lower cost if they have a priority closer to 1 and their `largeSize` is
     * altered as little as possible from `targetLargeSize`.
     *
     * @param targetLargeSize the size large items would like to be
     * @return a float representing the cost of this arrangement where the lower the cost the better
     */
    private fun cost(targetLargeSize: Float): Float {
        if (!isValid()) {
            return Float.MAX_VALUE
        }
        return abs(targetLargeSize - largeSize) * priority
    }

    companion object {
        // Specifies a percentage of a medium item's size by which it can be increased or decreased
        // to help fit an arrangement into the carousel's available space.
        private const val MediumItemFlexPercentage = .1f

        /**
         * Create an arrangement for all possible permutations for `smallCounts` and `largeCounts`,
         * fit each into the available space, and return the arrangement with the lowest
         * cost.
         *
         * Keep in mind that the returned arrangements do not take into account the available space
         * from the carousel. They will all occupy varying degrees of more or less space. The caller
         * needs to handle sorting the returned list, picking the most desirable arrangement, and
         * fitting the arrangement to the size of the carousel.
         *
         * @param availableSpace the space the arrangement needs to fit
         * @param targetSmallSize the size small items would like to be
         * @param minSmallSize the minimum size of which small item sizes are allowed to be
         * @param maxSmallSize the maximum size of which small item sizes are allowed to be
         * @param smallCounts an array of small item counts for a valid arrangement ordered by
         * priority
         * @param targetMediumSize the size medium items would like to be
         * @param mediumCounts an array of medium item counts for a valid arrangement ordered by
         * priority
         * @param targetLargeSize the size large items would like to be
         * @param largeCounts an array of large item counts for a valid arrangement ordered by
         * priority
         * @return the arrangement that is considered the most desirable and has been adjusted to
         * fit within the available space
         */
        fun findLowestCostArrangement(
            availableSpace: Float,
            targetSmallSize: Float,
            minSmallSize: Float,
            maxSmallSize: Float,
            smallCounts: IntArray,
            targetMediumSize: Float,
            mediumCounts: IntArray,
            targetLargeSize: Float,
            largeCounts: IntArray
        ): Arrangement? {
            var lowestCostArrangement: Arrangement? = null
            var priority = 1
            for (largeCount in largeCounts) {
                for (mediumCount in mediumCounts) {
                    for (smallCount in smallCounts) {
                        val arrangement = fit(
                            priority = priority,
                            availableSpace = availableSpace,
                            smallCount = smallCount,
                            smallSize = targetSmallSize,
                            minSmallSize = minSmallSize,
                            maxSmallSize = maxSmallSize,
                            mediumCount = mediumCount,
                            mediumSize = targetMediumSize,
                            largeCount = largeCount,
                            largeSize = targetLargeSize,
                        )
                        if (lowestCostArrangement == null ||
                            arrangement.cost(targetLargeSize) <
                            lowestCostArrangement.cost(targetLargeSize)
                        ) {
                            lowestCostArrangement = arrangement
                            if (lowestCostArrangement.cost(targetLargeSize) == 0f) {
                                // If the new lowestCostArrangement has a cost of 0, we know it
                                // didn't have to alter the large item size at all. We also know
                                // that arrangement permutations will be generated in order of
                                // priority. We can exit early knowing there will not be an
                                // arrangement with a better cost or priority.
                                return lowestCostArrangement
                            }
                        }
                        priority++
                    }
                }
            }
            return lowestCostArrangement
        }

        /**
         * Creates an arrangement that fits item sizes until the space occupied with the given item
         * counts within the `availableSpace`.
         *
         * This method tries to adjust the size of large items as little as possible by first
         * adjusting small items as much as possible, then adjusting medium items as much as
         * possible, and finally adjusting large items if the arrangement is still unable to fit.
         *
         * @param priority The priority to place on this particular arrangement of item counts
         * @param availableSpace The space in which to fit the arrangement
         * @param smallCount the number of small items to fit
         * @param smallSize the size of each small item
         * @param minSmallSize the minimum size a small item is allowed to be
         * @param maxSmallSize the maximum size a small item is allowed to be
         * @param mediumCount the number of medium items to fit
         * @param mediumSize the size of each medium item
         * @param largeCount the number of large items to fit
         * @param largeSize the size of each large item
         * @return the corresponding arrangement of each item count with each size adjusted to fit
         */
        private fun fit(
            priority: Int,
            availableSpace: Float,
            smallCount: Int,
            smallSize: Float,
            minSmallSize: Float,
            maxSmallSize: Float,
            mediumCount: Int,
            mediumSize: Float,
            largeCount: Int,
            largeSize: Float
        ): Arrangement {
            var arrangedSmallSize = smallSize.coerceIn(
                minSmallSize,
                maxSmallSize
            )
            var arrangedMediumSize = mediumSize
            var arrangedLargeSize = largeSize

            val totalSpaceTakenByArrangement = arrangedLargeSize * largeCount +
                arrangedMediumSize * mediumCount + arrangedSmallSize * smallCount
            val delta = availableSpace - totalSpaceTakenByArrangement
            // First, resize small items within their allowable min-max range to try to fit the
            // arrangement into the available space.
            if (smallCount > 0 && delta > 0) {
                // grow the small items
                arrangedSmallSize += min(
                    delta / smallCount,
                    maxSmallSize - arrangedSmallSize
                )
            } else if (smallCount > 0 && delta < 0) {
                // shrink the small items
                arrangedSmallSize += max(
                    delta / smallCount,
                    minSmallSize - arrangedSmallSize
                )
            }

            // Zero out small size if there are no small items
            arrangedSmallSize = if (smallCount > 0) arrangedSmallSize else 0f
            arrangedLargeSize = calculateLargeSize(
                availableSpace, smallCount, arrangedSmallSize,
                mediumCount, largeCount
            )
            arrangedMediumSize = (arrangedLargeSize + arrangedSmallSize) / 2f

            // If the large size has been adjusted away from its target size to fit the arrangement,
            // counter this as much as possible by altering the medium item within its acceptable
            // flex range.
            if (mediumCount > 0 && arrangedLargeSize != largeSize) {
                val targetAdjustment = (largeSize - arrangedLargeSize) * largeCount
                val availableMediumFlex =
                    arrangedMediumSize * MediumItemFlexPercentage * mediumCount
                val distribute: Float = min(abs(targetAdjustment), availableMediumFlex)
                if (targetAdjustment > 0f) {
                    // Reduce the size of the medium item and give it back to the large items
                    arrangedMediumSize -= distribute / mediumCount
                    arrangedLargeSize += distribute / largeCount
                } else {
                    // Increase the size of the medium item and take from the large items
                    arrangedMediumSize += distribute / mediumCount
                    arrangedLargeSize -= distribute / largeCount
                }
            }

            return Arrangement(
                priority,
                arrangedSmallSize,
                smallCount,
                arrangedMediumSize,
                mediumCount,
                arrangedLargeSize,
                largeCount
            )
        }

        /**
         * Calculates the large size that is able to fit within the available space given item
         * counts, the small size, and that the medium size is `(largeSize + smallSize) / 2`.
         *
         * This method solves the following equation for largeSize:
         *
         * `availableSpace = (largeSize * largeCount) + (((largeSize + smallSize) / 2) *
         * mediumCount) + (smallSize * smallCount)`
         *
         * @param availableSpace The space in which to calculate the large size to fit
         * @param smallCount the number of small items in the calculation
         * @param smallSize the size of each small item in the calculation
         * @param mediumCount the number of medium items in the calculation
         * @param largeCount the number of large items in the calculation
         * @return the large item size which will fit for the available space and other item
         * constraints
         */
        private fun calculateLargeSize(
            availableSpace: Float,
            smallCount: Int,
            smallSize: Float,
            mediumCount: Int,
            largeCount: Int
        ): Float {
            return ((availableSpace -
                (smallCount.toFloat() + mediumCount.toFloat() / 2f) * smallSize) /
                (largeCount.toFloat() + mediumCount.toFloat() / 2f))
        }
    }
}
