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

import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMapIndexed
import kotlin.math.abs

/**
 * A structure that is fixed at a specific [offset] along a scrolling axis and defines properties of
 * an item when its center is located at [offset].
 *
 * [Keyline] is the primary structure of any carousel. When multiple keylines are placed along a
 * carousel's axis and an item is scrolled, that item will always be between two keylines. The
 * item's distance between its two surrounding keylines can be used as a fraction to create an
 * interpolated keyline that the item uses to set its size and translation.
 *
 * @param size the size an item should be in pixels when its center is at [offset]
 * @param offset the location of the keyline along the scrolling axis and where the center of an
 *   item should be (usually translated to) when it is at [unadjustedOffset] in the end-to-end
 *   scrolling model
 * @param unadjustedOffset the location of:445 the keyline in the end-to-end scrolling model (when
 *   all items are laid out with their full size and placed end-to-end)
 * @param isFocal whether an item at this keyline is focal or fully "viewable"
 * @param isAnchor true if this keyline is able to be shifted within a list of keylines
 * @param isPivot true if this is the keyline that was used to calculate all other keyline offsets
 *   and unadjusted offsets in a list
 * @param cutoff the amount this item bleeds beyond the bounds of the container - 0 if the item is
 *   fully in-bounds or fully out-of-bounds
 */
internal data class Keyline(
    val size: Float,
    val offset: Float,
    val unadjustedOffset: Float,
    val isFocal: Boolean,
    val isAnchor: Boolean,
    val isPivot: Boolean,
    val cutoff: Float,
)

/**
 * A [List] of [Keyline]s with additional functionality specific to carousel.
 *
 * Note that [KeylineList]'s constructor should only be used when creating an interpolated
 * KeylineList. If creating a new KeylineList - for a strategy or shifted step - prefer using the
 * [keylineListOf] method which will handle setting all offsets and unadjusted offsets based on a
 * pivot keyline.
 */
internal class KeylineList internal constructor(keylines: List<Keyline>) :
    List<Keyline> by keylines {

    /**
     * Returns the index of the pivot keyline used to calculate all other keyline offsets and
     * unadjusted offsets.
     */
    val pivotIndex: Int = indexOfFirst { it.isPivot }

    /** Returns the keyline used to calculate all other keyline offsets and unadjusted offsets. */
    val pivot: Keyline
        get() = get(pivotIndex)

    /**
     * Returns the index of the first non-anchor keyline or -1 if the list does not contain a
     * non-anchor keyline.
     */
    val firstNonAnchorIndex: Int = indexOfFirst { !it.isAnchor }

    /**
     * Returns the first non-anchor [Keyline].
     *
     * @throws [NoSuchElementException] if there are no non-anchor keylines.
     */
    val firstNonAnchor: Keyline
        get() = get(firstNonAnchorIndex)

    /**
     * Returns the index of the last non-anchor keyline or -1 if the list does not contain a
     * non-anchor keyline.
     */
    val lastNonAnchorIndex: Int = indexOfLast { !it.isAnchor }

    /**
     * Returns the last non-anchor [Keyline].
     *
     * @throws [NoSuchElementException] if there are no non-anchor keylines.
     */
    val lastNonAnchor: Keyline
        get() = get(lastNonAnchorIndex)

    /**
     * Returns the index of the first focal keyline or -1 if the list does not contain a focal
     * keyline.
     */
    val firstFocalIndex = indexOfFirst { it.isFocal }

    /**
     * Returns the first focal [Keyline].
     *
     * @throws [NoSuchElementException] if there are no focal keylines.
     */
    val firstFocal: Keyline
        get() =
            getOrNull(firstFocalIndex)
                ?: throw NoSuchElementException(
                    "All KeylineLists must have at least one focal keyline"
                )

    /**
     * Returns the index of the last focal keyline or -1 if the list does not contain a focal
     * keyline.
     */
    val lastFocalIndex: Int = indexOfLast { it.isFocal }

    /**
     * Returns the last focal [Keyline].
     *
     * @throws [NoSuchElementException] if there are no focal keylines.
     */
    val lastFocal: Keyline
        get() =
            getOrNull(lastFocalIndex)
                ?: throw NoSuchElementException(
                    "All KeylineLists must have at least one focal keyline"
                )

    /**
     * Returns true if the first focal item's left/top is within the visible bounds of the container
     * and is the first non-anchor keyline.
     *
     * When this is true, it means the focal range cannot be shifted left/top or is shifted as far
     * left/top as possible. When this is false, there are keylines that can be swapped to shift the
     * first focal item closer to the left/top of the container while still remaining visible.
     */
    fun isFirstFocalItemAtStartOfContainer(): Boolean {
        val firstFocalLeft = firstFocal.offset - (firstFocal.size / 2)
        return firstFocalLeft >= 0 && firstFocal == firstNonAnchor
    }

    /**
     * Returns true if the last focal item's right/bottom is within the visible bounds of the
     * container and is the last non-anchor keyline.
     *
     * When this is true, it means the focal range cannot be shifted right/bottom or is shifted as
     * far right/bottom as possible. When this is false, there are keylines that can be swapped to
     * shift the last focal item closer to the right/bottom of the container while still remaining
     * visible.
     */
    fun isLastFocalItemAtEndOfContainer(carouselMainAxisSize: Float): Boolean {
        val lastFocalRight = lastFocal.offset + (lastFocal.size / 2)
        return lastFocalRight <= carouselMainAxisSize && lastFocal == lastNonAnchor
    }

    /**
     * Returns the index of the first keyline after the focal range where the keyline's size is
     * equal to [size] or the last index if no keyline is found.
     *
     * This is useful when moving keylines from one side of the focal range to the other (shifting).
     * Find an index on the other side of the focal range where after moving the keyline, the
     * keyline list will retain its original visual balance.
     */
    fun firstIndexAfterFocalRangeWithSize(size: Float): Int {
        val from = lastFocalIndex
        val to = lastIndex
        return (from..to).firstOrNull { i -> this[i].size == size } ?: lastIndex
    }

    /**
     * Returns the index of the last keyline before the focal range where the keyline's size is
     * equal to [size] or 0 if no keyline is found.
     *
     * This is useful when moving keylines from one side of the focal range to the other (shifting).
     * Find an index on the other side of the focal range where after moving the keyline, the
     * keyline list will retain its original visual balance.
     */
    fun lastIndexBeforeFocalRangeWithSize(size: Float): Int {
        val from = firstFocalIndex - 1
        val to = 0
        return (from downTo to).firstOrNull { i -> this[i].size == size } ?: to
    }

    /**
     * Returns the last [Keyline] with an unadjustedOffset that is less than [unadjustedOffset] or
     * the first keyline if none is found.
     */
    fun getKeylineBefore(unadjustedOffset: Float): Keyline {
        for (index in indices.reversed()) {
            val k = get(index)
            if (k.unadjustedOffset < unadjustedOffset) {
                return k
            }
        }

        return first()
    }

    /**
     * Returns the first [Keyline] with an unadjustedOffset that is greater than [unadjustedOffset]
     * or the last keyline if none is found.
     */
    fun getKeylineAfter(unadjustedOffset: Float): Keyline {
        return fastFirstOrNull { it.unadjustedOffset >= unadjustedOffset } ?: last()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeylineList) return false
        if (size != other.size) return false

        fastForEachIndexed { i, keyline -> if (keyline != other[i]) return false }

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        fastForEach { keyline -> result += 31 * keyline.hashCode() }
        return result
    }

    companion object {
        val Empty = KeylineList(emptyList())
    }
}

internal fun emptyKeylineList() = KeylineList.Empty

/** Returns a [KeylineList] by aligning the focal range relative to the carousel container. */
internal fun keylineListOf(
    carouselMainAxisSize: Float,
    itemSpacing: Float,
    carouselAlignment: CarouselAlignment,
    keylines: KeylineListScope.() -> Unit
): KeylineList {
    val keylineListScope = KeylineListScopeImpl()
    keylines.invoke(keylineListScope)
    return keylineListScope.createWithAlignment(
        carouselMainAxisSize,
        itemSpacing,
        carouselAlignment
    )
}

/**
 * Returns a [KeylineList] by using a single pivot keyline to calculate the offset and unadjusted
 * offset of all keylines in the list.
 */
internal fun keylineListOf(
    carouselMainAxisSize: Float,
    itemSpacing: Float,
    pivotIndex: Int,
    pivotOffset: Float,
    keylines: KeylineListScope.() -> Unit
): KeylineList {
    val keylineListScope = KeylineListScopeImpl()
    keylines.invoke(keylineListScope)
    return keylineListScope.createWithPivot(
        carouselMainAxisSize,
        itemSpacing,
        pivotIndex,
        pivotOffset
    )
}

/** Receiver scope for creating a [KeylineList] using [keylineListOf] */
internal interface KeylineListScope {

    /**
     * Adds a keyline to the resulting [KeylineList].
     *
     * Note that keylines are added in the order they will appear.
     *
     * @param size the size of an item in pixels at this keyline
     * @param isAnchor true if this keyline should not be shifted - usually the first and last fully
     *   off-screen keylines
     */
    fun add(size: Float, isAnchor: Boolean = false)
}

private class KeylineListScopeImpl : KeylineListScope {

    private data class TmpKeyline(val size: Float, val isAnchor: Boolean)

    private var firstFocalIndex: Int = -1
    private var focalItemSize: Float = 0f
    private var pivotIndex: Int = -1
    private var pivotOffset: Float = 0f
    private val tmpKeylines = mutableListOf<TmpKeyline>()

    override fun add(size: Float, isAnchor: Boolean) {
        tmpKeylines.add(TmpKeyline(size, isAnchor))
        // Save the first "focal" item by looking for the first index of the largest item added
        // to the list. The last focal item index will be found when `create` is called by starting
        // from firstFocalIndex and incrementing the index until the next item's size does not
        // equal focalItemSize.
        if (size > focalItemSize) {
            firstFocalIndex = tmpKeylines.lastIndex
            focalItemSize = size
        }
    }

    fun createWithPivot(
        carouselMainAxisSize: Float,
        itemSpacing: Float,
        pivotIndex: Int,
        pivotOffset: Float
    ): KeylineList {
        val keylines =
            createKeylinesWithPivot(
                pivotIndex,
                pivotOffset,
                firstFocalIndex,
                findLastFocalIndex(),
                itemMainAxisSize = focalItemSize,
                carouselMainAxisSize = carouselMainAxisSize,
                itemSpacing,
                tmpKeylines
            )
        return KeylineList(keylines)
    }

    fun createWithAlignment(
        carouselMainAxisSize: Float,
        itemSpacing: Float,
        carouselAlignment: CarouselAlignment
    ): KeylineList {
        val lastFocalIndex = findLastFocalIndex()
        val focalItemCount = lastFocalIndex - firstFocalIndex

        pivotIndex = firstFocalIndex
        pivotOffset =
            when (carouselAlignment) {
                CarouselAlignment.Center -> {
                    // If there is an even number of keylines, the itemSpacing will be placed in the
                    // center of the container. Divide the item spacing by half before subtracting
                    // the pivot item's center.
                    val itemSpacingSplit =
                        if (itemSpacing == 0f || focalItemCount.mod(2) == 0) {
                            0f
                        } else {
                            itemSpacing / 2f
                        }
                    (carouselMainAxisSize / 2) -
                        ((focalItemSize / 2) * focalItemCount) -
                        itemSpacingSplit
                }
                CarouselAlignment.End -> carouselMainAxisSize - (focalItemSize / 2)
                // Else covers and defaults to CarouselAlignment.Start
                else -> focalItemSize / 2
            }

        val keylines =
            createKeylinesWithPivot(
                pivotIndex,
                pivotOffset,
                firstFocalIndex,
                lastFocalIndex,
                itemMainAxisSize = focalItemSize,
                carouselMainAxisSize = carouselMainAxisSize,
                itemSpacing,
                tmpKeylines
            )
        return KeylineList(keylines)
    }

    private fun findLastFocalIndex(): Int {
        // Find the last focal index. Start from the first focal index and walk up the indices
        // while items remain the same size as the first focal item size - finding a contiguous
        // range of indices where item size is equal to focalItemSize.
        var lastFocalIndex = firstFocalIndex
        while (
            lastFocalIndex < tmpKeylines.lastIndex &&
                tmpKeylines[lastFocalIndex + 1].size == focalItemSize
        ) {
            lastFocalIndex++
        }
        return lastFocalIndex
    }

    /**
     * Converts a list of [TmpKeyline] to a list of [Keyline]s whose offset, unadjusted offset, and
     * cutoff are calculated from a pivot.
     *
     * Pivoting is useful when aligning the entire arrangement relative to the scrolling container.
     * When creating a keyline list with the first focal keyline aligned to the start of the
     * container, use the first focal item as the pivot and set the pivot offset to where that first
     * focal item's center should be placed (carouselStart + (item size / 2)). All keylines before
     * and after the pivot will have their offset, unadjusted offset, and cutoff calculated based on
     * the pivot offset. When shifting keylines and moving the carousel's alignment from start to
     * end, use setPivot to align the last focal keyline to the end of the container.
     *
     * @param pivotIndex the index of the keyline from [tmpKeylines] that is used to align the
     *   entire arrangement
     * @param pivotOffset the offset along the scrolling axis where the pivot keyline should be
     *   placed and where keylines before and after will have their offset, unadjustedOffset, and
     *   cutoff calculated from
     * @param firstFocalIndex the index of the first focal item in the [tmpKeylines] list
     * @param lastFocalIndex the index of the last focal item in the [tmpKeylines] list
     * @param itemMainAxisSize the size of focal, or fully unmasked/clipped, items
     * @param carouselMainAxisSize the size of the carousel container in the scrolling axis
     */
    private fun createKeylinesWithPivot(
        pivotIndex: Int,
        pivotOffset: Float,
        firstFocalIndex: Int,
        lastFocalIndex: Int,
        itemMainAxisSize: Float,
        carouselMainAxisSize: Float,
        itemSpacing: Float,
        tmpKeylines: List<TmpKeyline>
    ): List<Keyline> {
        val pivot = tmpKeylines[pivotIndex]
        val keylines = mutableListOf<Keyline>()

        val pivotCutoff: Float =
            when {
                isCutoffLeft(pivot.size, pivotOffset) -> pivotOffset - (pivot.size / 2)
                isCutoffRight(pivot.size, pivotOffset, carouselMainAxisSize) ->
                    (pivotOffset + (pivot.size / 2)) - carouselMainAxisSize
                else -> 0f
            }
        keylines.add(
            // Add the pivot keyline first
            Keyline(
                size = pivot.size,
                offset = pivotOffset,
                unadjustedOffset = pivotOffset,
                isFocal = pivotIndex in firstFocalIndex..lastFocalIndex,
                isAnchor = pivot.isAnchor,
                isPivot = true,
                cutoff = pivotCutoff
            )
        )

        // Convert all TmpKeylines before the pivot to Keylines by calculating their offset,
        // unadjustedOffset, and cutoff and insert them at the beginning of the keyline list,
        // maintaining the tmpKeyline list's original order.
        var offset = pivotOffset - (itemMainAxisSize / 2) - itemSpacing
        var unadjustedOffset = pivotOffset - (itemMainAxisSize / 2) - itemSpacing
        (pivotIndex - 1 downTo 0).forEach { originalIndex ->
            val tmp = tmpKeylines[originalIndex]
            val tmpOffset = offset - (tmp.size / 2)
            val tmpUnadjustedOffset = unadjustedOffset - (itemMainAxisSize / 2)
            val cutoff =
                if (isCutoffLeft(tmp.size, tmpOffset)) abs(tmpOffset - (tmp.size / 2)) else 0f
            keylines.add(
                0,
                Keyline(
                    size = tmp.size,
                    offset = tmpOffset,
                    unadjustedOffset = tmpUnadjustedOffset,
                    isFocal = originalIndex in firstFocalIndex..lastFocalIndex,
                    isAnchor = tmp.isAnchor,
                    isPivot = false,
                    cutoff = cutoff
                )
            )

            offset -= tmp.size + itemSpacing
            unadjustedOffset -= itemMainAxisSize + itemSpacing
        }

        // Convert all TmpKeylines after the pivot to Keylines by calculating their offset,
        // unadjustedOffset, and cutoff and inserting them at the end of the keyline list,
        // maintaining the tmpKeyline list's original order.
        offset = pivotOffset + (itemMainAxisSize / 2) + itemSpacing
        unadjustedOffset = pivotOffset + (itemMainAxisSize / 2) + itemSpacing
        (pivotIndex + 1 until tmpKeylines.size).forEach { originalIndex ->
            val tmp = tmpKeylines[originalIndex]
            val tmpOffset = offset + (tmp.size / 2)
            val tmpUnadjustedOffset = unadjustedOffset + (itemMainAxisSize / 2)
            val cutoff =
                if (isCutoffRight(tmp.size, tmpOffset, carouselMainAxisSize)) {
                    (tmpOffset + (tmp.size / 2)) - carouselMainAxisSize
                } else {
                    0f
                }
            keylines.add(
                Keyline(
                    size = tmp.size,
                    offset = tmpOffset,
                    unadjustedOffset = tmpUnadjustedOffset,
                    isFocal = originalIndex in firstFocalIndex..lastFocalIndex,
                    isAnchor = tmp.isAnchor,
                    isPivot = false,
                    cutoff = cutoff
                )
            )

            offset += tmp.size + itemSpacing
            unadjustedOffset += itemMainAxisSize + itemSpacing
        }

        return keylines
    }

    /**
     * Returns whether an item of [size] whose center is at [offset] is straddling the carousel
     * container's left/top.
     *
     * This method will return false if the item is either fully visible (its left/top edge comes
     * after the container's left/top) or fully invisible (its right/bottom edge comes before the
     * container's left/top).
     */
    private fun isCutoffLeft(size: Float, offset: Float): Boolean {
        return offset - (size / 2) < 0f && offset + (size / 2) > 0f
    }

    /**
     * Returns whether an item of [size] whose center is at [offset] is straddling the carousel
     * container's right/bottom edge.
     *
     * This method will return false if the item is either fully visible (its right/bottom edge
     * comes before the container's right/bottom) or fully invisible (its left/top edge comes after
     * the container's right/bottom).
     */
    private fun isCutoffRight(size: Float, offset: Float, carouselMainAxisSize: Float): Boolean {
        return offset - (size / 2) < carouselMainAxisSize &&
            offset + (size / 2) > carouselMainAxisSize
    }
}

/**
 * Returns an interpolated [Keyline] whose values are all interpolated based on [fraction] between
 * the [start] and [end] keylines.
 */
internal fun lerp(start: Keyline, end: Keyline, fraction: Float): Keyline {
    return Keyline(
        size = androidx.compose.ui.util.lerp(start.size, end.size, fraction),
        offset = androidx.compose.ui.util.lerp(start.offset, end.offset, fraction),
        unadjustedOffset =
            androidx.compose.ui.util.lerp(start.unadjustedOffset, end.unadjustedOffset, fraction),
        isFocal = if (fraction < .5f) start.isFocal else end.isFocal,
        isAnchor = if (fraction < .5f) start.isAnchor else end.isAnchor,
        isPivot = if (fraction < .5f) start.isPivot else end.isPivot,
        cutoff = androidx.compose.ui.util.lerp(start.cutoff, end.cutoff, fraction)
    )
}

/**
 * Returns an interpolated KeylineList between [from] and [to].
 *
 * Unlike creating a [KeylineList] using [keylineListOf], this method does not set unadjusted
 * offsets by calculating them from a pivot index. This method simply interpolates all values of all
 * keylines between the given pair.
 */
internal fun lerp(from: KeylineList, to: KeylineList, fraction: Float): KeylineList {
    val interpolatedKeylines = from.fastMapIndexed { i, k -> lerp(k, to[i], fraction) }
    return KeylineList(interpolatedKeylines)
}
