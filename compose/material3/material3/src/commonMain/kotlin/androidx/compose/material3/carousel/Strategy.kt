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

import androidx.collection.FloatList
import androidx.collection.mutableFloatListOf
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMapIndexed
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * An immutable class responsible for supplying carousel with a [KeylineList] that is corrected for
 * scroll offset, layout direction, and snapping behaviors.
 *
 * @param defaultKeylines the keylines that define how items should be arranged in their default
 *   state
 * @param startKeylineSteps a list of [KeylineList]s that move the focal range from its position in
 *   [defaultKeylines] to the start of the carousel container, one keyline at a time per step
 * @param endKeylineSteps a list of [KeylineList]s that move the focal range from its position in
 *   [defaultKeylines] to the end of the carousel container, one keyline at a time per step.
 *   [endKeylineSteps] and whose value is the percentage of [endShiftDistance] that should be
 *   scrolled when the end step is used.
 * @param availableSpace the available space in the main axis
 * @param itemSpacing the spacing between each item
 * @param beforeContentPadding the padding preceding the first item in the list
 * @param afterContentPadding the padding proceeding the last item in the list
 */
internal class Strategy
private constructor(
    val defaultKeylines: KeylineList,
    val startKeylineSteps: List<KeylineList>,
    val endKeylineSteps: List<KeylineList>,
    val availableSpace: Float,
    val itemSpacing: Float,
    val beforeContentPadding: Float,
    val afterContentPadding: Float,
) {

    /**
     * Creates a new [Strategy] for a keyline list and set of carousel container parameters.
     *
     * The [defaultKeylines] are a list of keylines that defines how items should be arranged, from
     * left-to-right (or top-to-bottom), to achieve the carousel's desired appearance. For example,
     * a start-aligned large item, followed by a medium and a small item for a multi-browse
     * carousel. Or a small item, a center-aligned large item, and a small item for a centered hero
     * carousel. This method will use the [defaultKeylines] to then derive new scroll and layout
     * direction-aware [KeylineList]s to be used by carousel. For example, when a device is running
     * in a right-to-left layout direction, Strategy will handle reversing the default
     * [KeylineList]. Or if the default keylines use a center-aligned large item, Strategy will
     * generate additional KeylineLists that handle shifting the large item to the start or end of
     * the screen when the carousel is scrolled to the start or end of the list, letting all items
     * become large without having them detach from the edges of the scroll container.
     *
     * @param defaultKeylines a default [KeylineList] that represents the arrangement of items in a
     *   left-to-right (or top-to-bottom) layout.
     * @param availableSpace the size of the carousel container in scrolling axis
     * @param beforeContentPadding the padding to add before the list content
     * @param afterContentPadding the padding to add after the list content
     */
    constructor(
        defaultKeylines: KeylineList,
        availableSpace: Float,
        itemSpacing: Float,
        beforeContentPadding: Float,
        afterContentPadding: Float
    ) : this(
        defaultKeylines = defaultKeylines,
        startKeylineSteps =
            getStartKeylineSteps(
                defaultKeylines,
                availableSpace,
                itemSpacing,
                beforeContentPadding
            ),
        endKeylineSteps =
            getEndKeylineSteps(defaultKeylines, availableSpace, itemSpacing, afterContentPadding),
        availableSpace = availableSpace,
        itemSpacing = itemSpacing,
        beforeContentPadding = beforeContentPadding,
        afterContentPadding = afterContentPadding,
    )

    /** The scroll distance needed to move through all steps in [startKeylineSteps]. */
    private val startShiftDistance = getStartShiftDistance(startKeylineSteps, beforeContentPadding)
    /** The scroll distance needed to move through all steps in [endKeylineSteps]. */
    private val endShiftDistance = getEndShiftDistance(endKeylineSteps, afterContentPadding)
    /**
     * A list of floats whose index aligns with a [KeylineList] from [startKeylineSteps] and whose
     * value is the percentage of [startShiftDistance] that should be scrolled when the start step
     * is used.
     */
    private val startShiftPoints =
        getStepInterpolationPoints(startShiftDistance, startKeylineSteps, true)
    /**
     * A list of floats whose index aligns with a [KeylineList] from [endKeylineSteps] and whose
     * value is the percentage of [endShiftDistance] that should be scrolled when the end step is
     * used.
     */
    private val endShiftPoints =
        getStepInterpolationPoints(endShiftDistance, endKeylineSteps, false)

    /** The size of items when in focus and fully unmasked. */
    val itemMainAxisSize: Float
        get() = defaultKeylines.firstFocal.size

    /** True if this strategy contains a valid arrangement of keylines for a valid container */
    val isValid: Boolean =
        defaultKeylines.isNotEmpty() && availableSpace != 0f && itemMainAxisSize != 0f

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
        // The scroll offset could sometimes be slightly negative due to rounding; it should always
        // be positive
        val positiveScrollOffset = max(0f, scrollOffset)
        val startShiftOffset = startShiftDistance
        val endShiftOffset = max(0f, maxScrollOffset - endShiftDistance)

        // If we're not within either shift range, return the default keylines
        if (positiveScrollOffset in startShiftOffset..endShiftOffset) {
            return defaultKeylines
        }

        var interpolation =
            lerp(
                outputMin = 1f,
                outputMax = 0f,
                inputMin = 0f,
                inputMax = startShiftOffset,
                value = positiveScrollOffset
            )
        var shiftPoints = startShiftPoints
        var steps = startKeylineSteps

        if (positiveScrollOffset > endShiftOffset) {
            interpolation =
                lerp(
                    outputMin = 0f,
                    outputMax = 1f,
                    inputMin = endShiftOffset,
                    inputMax = maxScrollOffset,
                    value = positiveScrollOffset
                )
            shiftPoints = endShiftPoints
            steps = endKeylineSteps
        }

        val shiftPointRange = getShiftPointRange(steps.size, shiftPoints, interpolation)

        if (roundToNearestStep) {
            val roundedStepIndex =
                if (shiftPointRange.steppedInterpolation.roundToInt() == 0) {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Strategy) return false

        // If neither strategy is valid, they should be considered equal
        if (!isValid && !other.isValid) return true

        if (isValid != other.isValid) return false
        if (availableSpace != other.availableSpace) return false
        if (itemSpacing != other.itemSpacing) return false
        if (beforeContentPadding != other.beforeContentPadding) return false
        if (afterContentPadding != other.afterContentPadding) return false
        if (itemMainAxisSize != other.itemMainAxisSize) return false
        if (startShiftDistance != other.startShiftDistance) return false
        if (endShiftDistance != other.endShiftDistance) return false
        if (startShiftPoints != other.startShiftPoints) return false
        if (endShiftPoints != other.endShiftPoints) return false
        // Only check default keyline equality since all other keylines are
        // derived from the defaults
        if (defaultKeylines != other.defaultKeylines) return false

        return true
    }

    override fun hashCode(): Int {
        if (!isValid) return isValid.hashCode()

        var result = isValid.hashCode()
        result = 31 * result + availableSpace.hashCode()
        result = 31 * result + itemSpacing.hashCode()
        result = 31 * result + beforeContentPadding.hashCode()
        result = 31 * result + afterContentPadding.hashCode()
        result = 31 * result + itemMainAxisSize.hashCode()
        result = 31 * result + startShiftDistance.hashCode()
        result = 31 * result + endShiftDistance.hashCode()
        result = 31 * result + startShiftPoints.hashCode()
        result = 31 * result + endShiftPoints.hashCode()
        result = 31 * result + defaultKeylines.hashCode()
        return result
    }

    companion object {
        val Empty =
            Strategy(
                defaultKeylines = emptyKeylineList(),
                startKeylineSteps = emptyList(),
                endKeylineSteps = emptyList(),
                availableSpace = 0f,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f,
            )
    }
}

/**
 * Returns the total scroll offset needed to move through the entire list of [startKeylineSteps].
 */
private fun getStartShiftDistance(
    startKeylineSteps: List<KeylineList>,
    beforeContentPadding: Float
): Float {
    if (startKeylineSteps.isEmpty()) return 0f
    return max(
        startKeylineSteps.last().first().unadjustedOffset -
            startKeylineSteps.first().first().unadjustedOffset,
        beforeContentPadding
    )
}

/** Returns the total scroll offset needed to move through the entire list of [endKeylineSteps]. */
private fun getEndShiftDistance(
    endKeylineSteps: List<KeylineList>,
    afterContentPadding: Float
): Float {
    if (endKeylineSteps.isEmpty()) return 0f
    return max(
        endKeylineSteps.first().last().unadjustedOffset -
            endKeylineSteps.last().last().unadjustedOffset,
        afterContentPadding
    )
}

/**
 * Generates discreet steps which move the focal range from its original position until it reaches
 * the start of the carousel container.
 *
 * Each step can only move the focal range by one keyline at a time to ensure every item in the list
 * passes through the focal range. Each step removes the keyline at the start of the container and
 * re-inserts it after the focal range in an order that retains visual balance. This is repeated
 * until the first focal keyline is at the start of the container. Re-inserting keylines after the
 * focal range in a balanced way is done by looking at the size of they keyline next to the keyline
 * that is being re-positioned and finding a match on the other side of the focal range.
 *
 * The first state in the returned list is always the default [KeylineList] while the last state
 * will be the start state or the state that has the focal range at the beginning of the carousel.
 */
private fun getStartKeylineSteps(
    defaultKeylines: KeylineList,
    carouselMainAxisSize: Float,
    itemSpacing: Float,
    beforeContentPadding: Float
): List<KeylineList> {
    if (defaultKeylines.isEmpty()) return emptyList()

    val steps: MutableList<KeylineList> = mutableListOf()
    steps.add(defaultKeylines)

    if (defaultKeylines.isFirstFocalItemAtStartOfContainer()) {
        if (beforeContentPadding != 0f) {
            steps.add(
                createShiftedKeylineListForContentPadding(
                    defaultKeylines,
                    carouselMainAxisSize,
                    itemSpacing,
                    beforeContentPadding,
                    defaultKeylines.firstFocal,
                    defaultKeylines.firstFocalIndex
                )
            )
        }
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
                carouselMainAxisSize = carouselMainAxisSize,
                itemSpacing = itemSpacing
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
            dstIndex = prevStep.firstIndexAfterFocalRangeWithSize(originalNeighborBeforeSize) - 1
        }

        steps.add(
            moveKeylineAndCreateShiftedKeylineList(
                from = prevStep,
                srcIndex = defaultKeylines.firstNonAnchorIndex,
                dstIndex = dstIndex,
                carouselMainAxisSize = carouselMainAxisSize,
                itemSpacing = itemSpacing
            )
        )
        i++
    }

    if (beforeContentPadding != 0f) {
        steps[steps.lastIndex] =
            createShiftedKeylineListForContentPadding(
                steps.last(),
                carouselMainAxisSize,
                itemSpacing,
                beforeContentPadding,
                steps.last().firstFocal,
                steps.last().firstFocalIndex
            )
    }

    return steps
}

/**
 * Generates discreet steps which move the focal range from its original position until it reaches
 * the end of the carousel container.
 *
 * Each step can only move the focal range by one keyline at a time to ensure every item in the list
 * passes through the focal range. Each step removes the keyline at the end of the container and
 * re-inserts it before the focal range in an order that retains visual balance. This is repeated
 * until the last focal keyline is at the start of the container. Re-inserting keylines before the
 * focal range in a balanced way is done by looking at the size of they keyline next to the keyline
 * that is being re-positioned and finding a match on the other side of the focal range.
 *
 * The first state in the returned list is always the default [KeylineList] while the last state
 * will be the end state or the state that has the focal range at the end of the carousel.
 */
private fun getEndKeylineSteps(
    defaultKeylines: KeylineList,
    carouselMainAxisSize: Float,
    itemSpacing: Float,
    afterContentPadding: Float
): List<KeylineList> {
    if (defaultKeylines.isEmpty()) return emptyList()
    val steps: MutableList<KeylineList> = mutableListOf()
    steps.add(defaultKeylines)

    if (defaultKeylines.isLastFocalItemAtEndOfContainer(carouselMainAxisSize)) {
        if (afterContentPadding != 0f) {
            steps.add(
                createShiftedKeylineListForContentPadding(
                    defaultKeylines,
                    carouselMainAxisSize,
                    itemSpacing,
                    -afterContentPadding,
                    defaultKeylines.lastFocal,
                    defaultKeylines.lastFocalIndex
                )
            )
        }
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
                carouselMainAxisSize = carouselMainAxisSize,
                itemSpacing = itemSpacing
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
            dstIndex = prevStep.lastIndexBeforeFocalRangeWithSize(originalNeighborAfterSize) + 1
        }

        val keylines =
            moveKeylineAndCreateShiftedKeylineList(
                from = prevStep,
                srcIndex = defaultKeylines.lastNonAnchorIndex,
                dstIndex = dstIndex,
                carouselMainAxisSize = carouselMainAxisSize,
                itemSpacing = itemSpacing
            )
        steps.add(keylines)
        i++
    }

    if (afterContentPadding != 0f) {
        steps[steps.lastIndex] =
            createShiftedKeylineListForContentPadding(
                steps.last(),
                carouselMainAxisSize,
                itemSpacing,
                -afterContentPadding,
                steps.last().lastFocal,
                steps.last().lastFocalIndex
            )
    }

    return steps
}

/**
 * Returns a new [KeylineList] identical to [from] but with each keyline's offset shifted by
 * [contentPadding].
 */
private fun createShiftedKeylineListForContentPadding(
    from: KeylineList,
    carouselMainAxisSize: Float,
    itemSpacing: Float,
    contentPadding: Float,
    pivot: Keyline,
    pivotIndex: Int
): KeylineList {
    val numberOfNonAnchorKeylines = from.fastFilter { !it.isAnchor }.count()
    val sizeReduction = contentPadding / numberOfNonAnchorKeylines
    // Let keylineListOf create a new keyline list with offsets adjusted for each item's
    // reduction in size
    val newKeylines =
        keylineListOf(
            carouselMainAxisSize = carouselMainAxisSize,
            itemSpacing = itemSpacing,
            pivotIndex = pivotIndex,
            pivotOffset = pivot.offset - (sizeReduction / 2f) + contentPadding
        ) {
            from.fastForEach { k -> add(k.size - abs(sizeReduction), k.isAnchor) }
        }

    // Then reset each item's unadjusted offset back to their original value from the
    // incoming keyline list. This is necessary because Pager will still be laying out items
    // end-to-end with the original page size and not the new reduced size.
    return KeylineList(
        newKeylines.fastMapIndexed { i, k -> k.copy(unadjustedOffset = from[i].unadjustedOffset) }
    )
}

/**
 * Returns a new [KeylineList] where the keyline at [srcIndex] is moved to [dstIndex] and with
 * updated pivot and offsets that reflect any change in focal shift.
 */
private fun moveKeylineAndCreateShiftedKeylineList(
    from: KeylineList,
    srcIndex: Int,
    dstIndex: Int,
    carouselMainAxisSize: Float,
    itemSpacing: Float
): KeylineList {
    // -1 if the pivot is shifting left/top, 1 if shifting right/bottom
    val pivotDir = if (srcIndex > dstIndex) 1 else -1
    val pivotDelta = (from[srcIndex].size - from[srcIndex].cutoff + itemSpacing) * pivotDir
    val newPivotIndex = from.pivotIndex + pivotDir
    val newPivotOffset = from.pivot.offset + pivotDelta
    return keylineListOf(carouselMainAxisSize, itemSpacing, newPivotIndex, newPivotOffset) {
        from.toMutableList().move(srcIndex, dstIndex).fastForEach { k -> add(k.size, k.isAnchor) }
    }
}

/**
 * Creates and returns a list of float values containing points between 0 and 1 that represent
 * interpolation values for when the [KeylineList] at the corresponding index in [steps] should be
 * visible.
 *
 * For example, if [steps] has a size of 4, this method will return an array of 4 float values that
 * could look like [0, .33, .66, 1]. When interpolating through a list of [KeylineList]s, an
 * interpolation value will be between 0-1. This interpolation will be used to find the range it
 * falls within from this method's returned value. If interpolation is .25, that would fall between
 * the 0 and .33, the 0th and 1st indices of the float array. Meaning the 0th and 1st items from
 * [steps] should be the current [KeylineList]s being interpolated. This is an example with equally
 * distributed values but these values will typically be unequally distributed since their size
 * depends on the distance keylines shift between each step.
 *
 * @param totalShiftDistance the total distance keylines will shift between the first and last
 *   [KeylineList] of [steps]
 * @param steps the steps to find interpolation points for
 * @param isShiftingLeft true if this method should find interpolation points for shifting keylines
 *   to the left/top of a carousel, false if this method should find interpolation points for
 *   shifting keylines to the right/bottom of a carousel
 * @return a list of floats, equal in size to [steps] that contains points between 0-1 that align
 *   with when a [KeylineList] from [steps should be shown for a 0-1 interpolation value
 * @see [lerp] for more details on how interpolation points are used
 * @see [Strategy.getKeylineListForScrollOffset] for more details on how interpolation points are
 *   used
 */
private fun getStepInterpolationPoints(
    totalShiftDistance: Float,
    steps: List<KeylineList>,
    isShiftingLeft: Boolean
): FloatList {
    val points = mutableFloatListOf(0f)
    if (totalShiftDistance == 0f || steps.isEmpty()) {
        return points
    }

    (1 until steps.size).map { i ->
        val prevKeylines = steps[i - 1]
        val currKeylines = steps[i]
        val distanceShifted =
            if (isShiftingLeft) {
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
    return ShiftPointRange(fromStepIndex = 0, toStepIndex = 0, steppedInterpolation = 0f)
}

private fun MutableList<Keyline>.move(srcIndex: Int, dstIndex: Int): MutableList<Keyline> {
    val keyline = get(srcIndex)
    removeAt(srcIndex)
    add(dstIndex, keyline)
    return this
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
