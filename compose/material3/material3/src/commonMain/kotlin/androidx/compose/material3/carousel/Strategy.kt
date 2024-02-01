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

import androidx.annotation.VisibleForTesting
import androidx.collection.FloatList
import androidx.collection.mutableFloatListOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMapIndexed
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

/**
 * An interface which provides [Strategy] instances to a scrollable component.
 *
 * [StrategyProvider.createStrategy] will be called any time properties which affect a carousel's
 * arrangement change. It is the implementation's responsibility to create an arrangement for the
 * given parameters and return a [Strategy] by calling [Strategy.create].
 */
internal interface StrategyProvider {
    /**
     * Create and return a new [Strategy] for the given carousel size.
     *
     * TODO: Add additional parameters like alignment and item count.
     *
     * @param density The [Density] object that provides pixel density information of the device
     * @param carouselMainAxisSize the size of the carousel in the main axis in pixels
     */
    fun createStrategy(
        density: Density,
        carouselMainAxisSize: Float,
    ): Strategy
}

/**
 * A class which supplies carousel with the appropriate [KeylineList] for any given scroll offset.
 *
 * All items in a carousel need the opportunity to pass through the focal keyline range. Depending
 * on where the focal range is located within the scrolling container, some items, like those at
 * the beginning or end of the list, might not reach the focal range. To account for this,
 * [Strategy] manages shifting the focal keylines to the beginning of the list when scrolled an
 * offset of 0 and the end of the list when scrolled to the list's max offset. [StrategyProvider]
 * needs only to create a "default" [KeylineList] (where keylines should be placed when scrolling
 * in the middle of the list) and call [Strategy.create] to have [Strategy] generate the steps
 * needed to move the focal range to the start and end of the scroll container. When scrolling, the
 * scrollable component can access the correct [KeylineList] for any given offset using
 * [getKeylineListForScrollOffset].
 *
 * @param defaultKeylines the [KeylineList] used when anywhere in the center of the list
 * @param startKeylineSteps a list of [KeylineList]s that will be moved through when approaching
 * the beginning of the list
 * @param endKeylineSteps a list o [KeylineList]s that will be moved through when appraoching
 * the end of the list
 * @param startShiftDistance the scroll distance it should take to move through all the steps in
 * [startKeylineSteps]
 * @param endShiftDistance the scroll distance it should take to move through all the steps in the
 * [endKeylineSteps]
 * @param startShiftPoints a list of floats between 0-1 that define the percentage of shift distance
 * at which the start keyline step at the corresponding index should be used
 * @param endShiftPoints a list of floats between 0-1 that define the percentage of shift distance
 * at which the end keyline step at the corresponding index should be used
 */
internal class Strategy private constructor(
    private val defaultKeylines: KeylineList,
    private val startKeylineSteps: List<KeylineList>,
    private val endKeylineSteps: List<KeylineList>,
    private val startShiftDistance: Float,
    private val endShiftDistance: Float,
    private val startShiftPoints: FloatList,
    private val endShiftPoints: FloatList
) {

    /**
     * Returns the [KeylineList] that should be used for the current [scrollOffset].
     *
     * @param scrollOffset the current scroll offset of the scrollable component
     * @param maxScrollOffset the maximum scroll offset
     * @param roundToNearestStep true if the KeylineList returned should be a complete shift step
     */
    internal fun getKeylineListForScrollOffset(
        scrollOffset: Float,
        maxScrollOffset: Float,
        roundToNearestStep: Boolean = false
    ): KeylineList {
        val startShiftOffset = startShiftDistance
        val endShiftOffset = maxScrollOffset - endShiftDistance

        // If we're not within either shift range, return the default keylines
        if (scrollOffset in startShiftOffset..endShiftOffset) {
            return defaultKeylines
        }

        var interpolation = lerp(
            outputMin = 1f,
            outputMax = 0f,
            inputMin = 0f,
            inputMax = startShiftOffset,
            value = scrollOffset
        )
        var shiftPoints = startShiftPoints
        var steps = startKeylineSteps

        if (scrollOffset > endShiftOffset) {
            interpolation = lerp(
                outputMin = 0f,
                outputMax = 1f,
                inputMin = endShiftOffset,
                inputMax = maxScrollOffset,
                value = scrollOffset
            )
            shiftPoints = endShiftPoints
            steps = endKeylineSteps
        }

        val shiftPointRange = getShiftPointRange(
            steps.size,
            shiftPoints,
            interpolation
        )

        if (roundToNearestStep) {
            val roundedStepIndex = if (shiftPointRange.steppedInterpolation.roundToInt() == 0) {
                shiftPointRange.fromStepIndex
            } else {
                shiftPointRange.toStepIndex
            }
            return steps[roundedStepIndex]
        }

        return lerp(
            steps[shiftPointRange.fromStepIndex],
            steps[shiftPointRange.toStepIndex],
            shiftPointRange.steppedInterpolation
        )
    }

    companion object {

        /**
         * Creates a new [Strategy] based on a default [keylineList].
         *
         * The [keylineList] passed to this method will be the keylines used when the carousel is
         * scrolled anywhere in the middle of the list (not the beginning or end). From these
         * default keylines, additional [KeylineList]s will be created which move the focal range
         * to the beginning of the carousel container when scrolled to the beginning of the list and
         * the end of the container when scrolled to the end of the list.
         *
         * @param carouselMainAxisSize the size of the carousel container in scrolling axis
         * @param keylineList the default keylines that will be used to create the strategy
         */
        internal fun create(
            /** The size of the carousel in the main axis. */
            carouselMainAxisSize: Float,
            /** The keylines along the main axis */
            keylineList: KeylineList
        ): Strategy {

            val startKeylineSteps = getStartKeylineSteps(keylineList, carouselMainAxisSize)
            val endKeylineSteps =
                getEndKeylineSteps(keylineList, carouselMainAxisSize)

            // TODO: Update this to use the first/last focal keylines to calculate shift?
            val startShiftDistance = startKeylineSteps.last().first().unadjustedOffset -
                keylineList.first().unadjustedOffset
            val endShiftDistance = keylineList.last().unadjustedOffset -
                endKeylineSteps.last().last().unadjustedOffset

            return Strategy(
                defaultKeylines = keylineList,
                startKeylineSteps = startKeylineSteps,
                endKeylineSteps = endKeylineSteps,
                startShiftDistance = startShiftDistance,
                endShiftDistance = endShiftDistance,
                startShiftPoints = getStepInterpolationPoints(
                    startShiftDistance,
                    startKeylineSteps,
                    true
                ),
                endShiftPoints = getStepInterpolationPoints(
                    endShiftDistance,
                    endKeylineSteps,
                    false
                )
            )
        }

        /**
         * Generates discreet steps which move the focal range from its original position until
         * it reaches the start of the carousel container.
         *
         * Each step can only move the focal range by one keyline at a time to ensure every
         * item in the list passes through the focal range. Each step removes the keyline at the
         * start of the container and re-inserts it after the focal range in an order that retains
         * visual balance. This is repeated until the first focal keyline is at the start of the
         * container. Re-inserting keylines after the focal range in a balanced way is done by
         * looking at the size of they keyline next to the keyline that is being re-positioned
         * and finding a match on the other side of the focal range.
         *
         * The first state in the returned list is always the default [KeylineList] while
         * the last state will be the start state or the state that has the focal range at the
         * beginning of the carousel.
         */
        private fun getStartKeylineSteps(
            defaultKeylines: KeylineList,
            carouselMainAxisSize: Float
        ): List<KeylineList> {
            val steps: MutableList<KeylineList> = mutableListOf()
            steps.add(defaultKeylines)

            if (defaultKeylines.isFirstFocalItemAtStartOfContainer()) {
                return steps
            }

            val startIndex = defaultKeylines.firstNonAnchorIndex
            val endIndex = defaultKeylines.firstFocalIndex
            val numberOfSteps = endIndex - startIndex

            // If there are no steps but we need to account for a cutoff, create a
            // list of keylines shifted for the cutoff.
            if (numberOfSteps <= 0 && defaultKeylines.firstFocal.cutoff > 0) {
                steps.add(
                    moveKeylineAndCreateShiftedKeylineList(
                        from = defaultKeylines,
                        srcIndex = 0,
                        dstIndex = 0,
                        carouselMainAxisSize = carouselMainAxisSize
                    )
                )
                return steps
            }

            var i = 0
            while (i < numberOfSteps) {
                val prevStep = steps.last()
                val originalItemIndex = startIndex + i
                var dstIndex = defaultKeylines.lastIndex
                if (originalItemIndex > 0) {
                    val originalNeighborBeforeSize = defaultKeylines[originalItemIndex - 1].size
                    dstIndex = prevStep.firstIndexAfterFocalRangeWithSize(
                        originalNeighborBeforeSize
                    ) - 1
                }

                steps.add(
                    moveKeylineAndCreateShiftedKeylineList(
                        from = prevStep,
                        srcIndex = defaultKeylines.firstNonAnchorIndex,
                        dstIndex = dstIndex,
                        carouselMainAxisSize = carouselMainAxisSize
                    )
                )
                i++
            }

            return steps
        }

        /**
         * Generates discreet steps which move the focal range from its original position until
         * it reaches the end of the carousel container.
         *
         * Each step can only move the focal range by one keyline at a time to ensure every
         * item in the list passes through the focal range. Each step removes the keyline at the
         * end of the container and re-inserts it before the focal range in an order that retains
         * visual balance. This is repeated until the last focal keyline is at the start of the
         * container. Re-inserting keylines before the focal range in a balanced way is done by
         * looking at the size of they keyline next to the keyline that is being re-positioned
         * and finding a match on the other side of the focal range.
         *
         * The first state in the returned list is always the default [KeylineList] while
         * the last state will be the end state or the state that has the focal range at the
         * end of the carousel.
         */
        private fun getEndKeylineSteps(
            defaultKeylines: KeylineList,
            carouselMainAxisSize: Float
        ): List<KeylineList> {
            val steps: MutableList<KeylineList> = mutableListOf()
            steps.add(defaultKeylines)

            if (defaultKeylines.isLastFocalItemAtEndOfContainer(carouselMainAxisSize)) {
                return steps
            }

            val startIndex = defaultKeylines.lastFocalIndex
            val endIndex = defaultKeylines.lastNonAnchorIndex
            val numberOfSteps = endIndex - startIndex

            // If there are no steps but we need to account for a cutoff, create a
            // list of keylines shifted for the cutoff.
            if (numberOfSteps <= 0 && defaultKeylines.lastFocal.cutoff > 0) {
                steps.add(
                    moveKeylineAndCreateShiftedKeylineList(
                        from = defaultKeylines,
                        srcIndex = 0,
                        dstIndex = 0,
                        carouselMainAxisSize = carouselMainAxisSize
                    )
                )
                return steps
            }

            var i = 0
            while (i < numberOfSteps) {
                val prevStep = steps.last()
                val originalItemIndex = endIndex - i
                var dstIndex = 0

                if (originalItemIndex < defaultKeylines.lastIndex) {
                    val originalNeighborAfterSize = defaultKeylines[originalItemIndex + 1].size
                    dstIndex = prevStep.lastIndexBeforeFocalRangeWithSize(
                        originalNeighborAfterSize
                    ) + 1
                }

                val keylines = moveKeylineAndCreateShiftedKeylineList(
                    from = prevStep,
                    srcIndex = defaultKeylines.lastNonAnchorIndex,
                    dstIndex = dstIndex,
                    carouselMainAxisSize = carouselMainAxisSize
                )
                steps.add(keylines)
                i++
            }

            return steps
        }

        /**
         * Returns a new [KeylineList] where the keyline at [srcIndex] is moved to [dstIndex] and
         * with updated pivot and offsets that reflect any change in focal shift.
         */
        private fun moveKeylineAndCreateShiftedKeylineList(
            from: KeylineList,
            srcIndex: Int,
            dstIndex: Int,
            carouselMainAxisSize: Float
        ): KeylineList {
            // -1 if the pivot is shifting left/top, 1 if shifting right/bottom
            val pivotDir = if (srcIndex > dstIndex) 1 else -1
            val pivotDelta = from[srcIndex].size * pivotDir
            val newPivotIndex = from.pivotIndex + pivotDir
            val newPivotOffset = from.pivot.offset + pivotDelta
            return keylineListOf(carouselMainAxisSize, newPivotIndex, newPivotOffset) {
                from.toMutableList()
                    .move(srcIndex, dstIndex)
                    .fastForEach { k -> add(k.size, k.isAnchor) }
            }
        }

        /**
         * Creates and returns a list of float values containing points between 0 and 1 that
         * represent interpolation values for when the [KeylineList] at the corresponding index in
         * [steps] should be visible.
         *
         * For example, if [steps] has a size of 4, this method will return an array of 4 float
         * values that could look like [0, .33, .66, 1]. When interpolating through a list of
         * [KeylineList]s, an interpolation value will be between 0-1. This interpolation will be
         * used to find the range it falls within from this method's returned value. If
         * interpolation is .25, that would fall between the 0 and .33, the 0th and 1st indices
         * of the float array. Meaning the 0th and 1st items from [steps] should be the current
         * [KeylineList]s being interpolated. This is an example with equally distributed values
         * but these values will typically be unequally distributed since their size depends on
         * the distance keylines shift between each step.
         *
         * @see [lerp] for more details on how interpolation points are used
         * @see [getKeylineListForScrollOffset] for more details on how interpolation points
         * are used
         *
         * @param totalShiftDistance the total distance keylines will shift between the first and
         * last [KeylineList] of [steps]
         * @param steps the steps to find interpolation points for
         * @param isShiftingLeft true if this method should find interpolation points for shifting
         * keylines to the left/top of a carousel, false if this method should find interpolation
         * points for shifting keylines to the right/bottom of a carousel
         * @return a list of floats, equal in size to [steps] that contains points between 0-1
         * that align with when a [KeylineList] from [steps should be shown for a 0-1
         * interpolation value
         */
        private fun getStepInterpolationPoints(
            totalShiftDistance: Float,
            steps: List<KeylineList>,
            isShiftingLeft: Boolean
        ): FloatList {
            val points = mutableFloatListOf(0f)
            if (totalShiftDistance == 0f) {
                return points
            }

            (1 until steps.size).map { i ->
                val prevKeylines = steps[i - 1]
                val currKeylines = steps[i]
                val distanceShifted = if (isShiftingLeft) {
                    currKeylines.first().unadjustedOffset - prevKeylines.first().unadjustedOffset
                } else {
                    prevKeylines.last().unadjustedOffset - currKeylines.last().unadjustedOffset
                }
                val stepPercentage = distanceShifted / totalShiftDistance
                val point = if (i == steps.lastIndex) 1f else points[i - 1] + stepPercentage
                points.add(point)
            }
            return points
        }

        private data class ShiftPointRange(
            val fromStepIndex: Int,
            val toStepIndex: Int,
            val steppedInterpolation: Float
        )

        private fun getShiftPointRange(
            stepsCount: Int,
            shiftPoint: FloatList,
            interpolation: Float
        ): ShiftPointRange {
            var lowerBounds = shiftPoint[0]
            (1 until stepsCount).forEach { i ->
                val upperBounds = shiftPoint[i]
                if (interpolation <= upperBounds) {
                    return ShiftPointRange(
                        fromStepIndex = i - 1,
                        toStepIndex = i,
                        steppedInterpolation = lerp(0f, 1f, lowerBounds, upperBounds, interpolation)
                    )
                }
                lowerBounds = upperBounds
            }
            return ShiftPointRange(
                fromStepIndex = 0,
                toStepIndex = 0,
                steppedInterpolation = 0f)
        }

        private fun MutableList<Keyline>.move(srcIndex: Int, dstIndex: Int): MutableList<Keyline> {
            val keyline = get(srcIndex)
            removeAt(srcIndex)
            add(dstIndex, keyline)
            return this
        }
    }
}

/**
 * Returns an interpolated [Keyline] whose values are all interpolated based on [fraction]
 * between the [start] and [end] keylines.
 */
@VisibleForTesting
internal fun lerp(start: Keyline, end: Keyline, fraction: Float): Keyline {
    return Keyline(
        size = lerp(start.size, end.size, fraction),
        offset = lerp(start.offset, end.offset, fraction),
        unadjustedOffset = lerp(start.unadjustedOffset, end.unadjustedOffset, fraction),
        isFocal = if (fraction < .5f) start.isFocal else end.isFocal,
        isAnchor = if (fraction < .5f) start.isAnchor else end.isAnchor,
        isPivot = if (fraction < .5f) start.isPivot else end.isPivot,
        cutoff = lerp(start.cutoff, end.cutoff, fraction)
    )
}

/**
 * Returns an interpolated KeylineList between [from] and [to].
 *
 * Unlike creating a [KeylineList] using [keylineListOf], this method does not set unadjusted
 * offsets by calculating them from a pivot index. This method simply interpolates all values of
 * all keylines between the given pair.
 */
@VisibleForTesting
internal fun lerp(
    from: KeylineList,
    to: KeylineList,
    fraction: Float
): KeylineList {
    val interpolatedKeylines = from.fastMapIndexed { i, k ->
        lerp(k, to[i], fraction)
    }
    return KeylineList(interpolatedKeylines)
}

private fun lerp(
    outputMin: Float,
    outputMax: Float,
    inputMin: Float,
    inputMax: Float,
    value: Float
): Float {
    if (value <= inputMin) {
        return outputMin
    } else if (value >= inputMax) {
        return outputMax
    }

    return lerp(outputMin, outputMax, (value - inputMin) / (inputMax - inputMin))
}
