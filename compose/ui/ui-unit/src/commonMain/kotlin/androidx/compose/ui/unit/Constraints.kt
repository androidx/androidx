/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Constraints.Companion.Infinity
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import kotlin.jvm.JvmInline
import kotlin.math.min

/**
 * Immutable constraints for measuring layouts, used by [layouts][androidx.compose.ui.layout.Layout]
 * or [layout modifiers][androidx.compose.ui.layout.LayoutModifier] to measure their layout
 * children. The parent chooses the [Constraints] defining a range, in pixels, within which the
 * measured layout should choose a size:
 * - `minWidth` <= `chosenWidth` <= `maxWidth`
 * - `minHeight` <= `chosenHeight` <= `maxHeight`
 *
 * For more details about how layout measurement works, see
 * [androidx.compose.ui.layout.MeasurePolicy] or
 * [androidx.compose.ui.layout.LayoutModifier.measure].
 *
 * A set of [Constraints] can have infinite maxWidth and/or maxHeight. This is a trick often used by
 * parents to ask their children for their preferred size: unbounded constraints force children
 * whose default behavior is to fill the available space (always size to maxWidth/maxHeight) to have
 * an opinion about their preferred size. Most commonly, when measured with unbounded [Constraints],
 * these children will fallback to size themselves to wrap their content, instead of expanding to
 * fill the available space (this is not always true as it depends on the child layout model, but is
 * a common behavior for core layout components).
 *
 * [Constraints] uses a [Long] to represent four values, [minWidth], [minHeight], [maxWidth], and
 * [maxHeight]. The range of the values varies to allow for at most 256K in one dimension. There are
 * four possible maximum ranges, 13 bits/18 bits, and 15 bits/16 bits for either width or height,
 * depending on the needs. For example, a width could range up to 18 bits and the height up to 13
 * bits. Alternatively, the width could range up to 16 bits and the height up to 15 bits. The height
 * and width requirements can be reversed, with a height of up to 18 bits and width of 13 bits or
 * height of 16 bits and width of 15 bits. Any constraints exceeding this range will fail.
 */
@Immutable
@JvmInline
value class Constraints(@PublishedApi internal val value: Long) {
    /**
     * Indicates how the bits are assigned. One of:
     * - MinFocusWidth
     * - MaxFocusWidth
     * - MinFocusHeight
     * - MaxFocusHeight
     */
    private inline val focusIndex
        get() = (value and FocusMask).toInt()

    /** The minimum width that the measurement can take, in pixels. */
    val minWidth: Int
        get() {
            val mask = widthMask(indexToBitOffset(focusIndex))
            return ((value shr 2).toInt() and mask)
        }

    /**
     * The maximum width that the measurement can take, in pixels. This will either be a positive
     * value greater than or equal to [minWidth] or [Constraints.Infinity].
     */
    val maxWidth: Int
        get() {
            val mask = widthMask(indexToBitOffset(focusIndex))
            val width = ((value shr 33).toInt() and mask)
            return if (width == 0) Infinity else width - 1
        }

    /** The minimum height that the measurement can take, in pixels. */
    val minHeight: Int
        get() {
            val bitOffset = indexToBitOffset(focusIndex)
            val mask = heightMask(bitOffset)
            val offset = minHeightOffsets(bitOffset)
            return (value shr offset).toInt() and mask
        }

    /**
     * The maximum height that the measurement can take, in pixels. This will either be a positive
     * value greater than or equal to [minHeight] or [Constraints.Infinity].
     */
    val maxHeight: Int
        get() {
            val bitOffset = indexToBitOffset(focusIndex)
            val mask = heightMask(bitOffset)
            val offset = minHeightOffsets(bitOffset) + 31
            val height = (value shr offset).toInt() and mask
            return if (height == 0) Infinity else height - 1
        }

    /**
     * `false` when [maxWidth] is [Infinity] and `true` if [maxWidth] is a non-[Infinity] value.
     *
     * @see hasBoundedHeight
     */
    val hasBoundedWidth: Boolean
        get() {
            val mask = widthMask(indexToBitOffset(focusIndex))
            return ((value shr 33).toInt() and mask) != 0
        }

    /**
     * `false` when [maxHeight] is [Infinity] and `true` if [maxHeight] is a non-[Infinity] value.
     *
     * @see hasBoundedWidth
     */
    val hasBoundedHeight: Boolean
        get() {
            val bitOffset = indexToBitOffset(focusIndex)
            val mask = heightMask(bitOffset)
            val offset = minHeightOffsets(bitOffset) + 31
            return ((value shr offset).toInt() and mask) != 0
        }

    /** Whether there is exactly one width value that satisfies the constraints. */
    @Stable
    val hasFixedWidth: Boolean
        get() {
            val mask = widthMask(indexToBitOffset(focusIndex))
            val minWidth = ((value shr 2).toInt() and mask)
            val maxWidth =
                ((value shr 33).toInt() and mask).let { if (it == 0) Infinity else it - 1 }
            return minWidth == maxWidth
        }

    /** Whether there is exactly one height value that satisfies the constraints. */
    @Stable
    val hasFixedHeight: Boolean
        get() {
            val bitOffset = indexToBitOffset(focusIndex)
            val mask = heightMask(bitOffset)
            val offset = minHeightOffsets(bitOffset)
            val minHeight = (value shr offset).toInt() and mask
            val maxHeight =
                ((value shr (offset + 31)).toInt() and mask).let {
                    if (it == 0) Infinity else it - 1
                }
            return minHeight == maxHeight
        }

    /**
     * Whether the area of a component respecting these constraints will definitely be 0. This is
     * true when at least one of maxWidth and maxHeight are 0.
     */
    @Stable
    val isZero: Boolean
        get() {
            val bitOffset = indexToBitOffset(focusIndex)

            // No need to special case width == 0 -> Infinity, instead we let it go to -1
            // and fail the test that follows
            val maxWidth = ((value shr 33).toInt() and widthMask(bitOffset)) - 1
            if (maxWidth == 0) return true

            // Same here
            val offset = minHeightOffsets(bitOffset) + 31
            val maxHeight = ((value shr offset).toInt() and heightMask(bitOffset)) - 1
            return maxHeight == 0
        }

    /**
     * Copies the existing [Constraints], replacing some of [minWidth], [minHeight], [maxWidth], or
     * [maxHeight] as desired. [minWidth] and [minHeight] must be positive and [maxWidth] and
     * [maxHeight] must be greater than or equal to [minWidth] and [minHeight], respectively, or
     * [Infinity].
     */
    fun copy(
        minWidth: Int = this.minWidth,
        maxWidth: Int = this.maxWidth,
        minHeight: Int = this.minHeight,
        maxHeight: Int = this.maxHeight
    ): Constraints {
        requirePrecondition(minHeight >= 0 && minWidth >= 0) {
            "minHeight($minHeight) and minWidth($minWidth) must be >= 0"
        }
        // if maxWidth == Infinity, the test passes
        requirePrecondition(maxWidth >= minWidth) {
            "maxWidth($maxWidth) must be >= minWidth($minWidth)"
        }
        // if maxHeight == Infinity, the test passes
        requirePrecondition(maxHeight >= minHeight) {
            "maxHeight($maxHeight) must be >= minHeight($minHeight)"
        }
        return createConstraints(minWidth, maxWidth, minHeight, maxHeight)
    }

    override fun toString(): String {
        val maxWidth = maxWidth
        val maxWidthStr = if (maxWidth == Infinity) "Infinity" else maxWidth.toString()
        val maxHeight = maxHeight
        val maxHeightStr = if (maxHeight == Infinity) "Infinity" else maxHeight.toString()
        return "Constraints(minWidth = $minWidth, maxWidth = $maxWidthStr, " +
            "minHeight = $minHeight, maxHeight = $maxHeightStr)"
    }

    companion object {
        /**
         * A value that [maxWidth] or [maxHeight] will be set to when the constraint should be
         * considered infinite. [hasBoundedWidth] or [hasBoundedHeight] will be `false` when
         * [maxWidth] or [maxHeight] is [Infinity], respectively.
         */
        const val Infinity = Int.MAX_VALUE

        /** Creates constraints for fixed size in both dimensions. */
        @Stable
        fun fixed(width: Int, height: Int): Constraints {
            requirePrecondition(width >= 0 && height >= 0) {
                "width($width) and height($height) must be >= 0"
            }
            return createConstraints(width, width, height, height)
        }

        /** Creates constraints for fixed width and unspecified height. */
        @Stable
        fun fixedWidth(width: Int): Constraints {
            requirePrecondition(width >= 0) { "width($width) must be >= 0" }
            return createConstraints(
                minWidth = width,
                maxWidth = width,
                minHeight = 0,
                maxHeight = Infinity
            )
        }

        /** Creates constraints for fixed height and unspecified width. */
        @Stable
        fun fixedHeight(height: Int): Constraints {
            requirePrecondition(height >= 0) { "height($height) must be >= 0" }
            return createConstraints(
                minWidth = 0,
                maxWidth = Infinity,
                minHeight = height,
                maxHeight = height
            )
        }

        // This should be removed before the next release
        @Deprecated(
            "Replace with fitPrioritizingWidth",
            replaceWith =
                ReplaceWith(
                    "Constraints.fitPrioritizingWidth(minWidth, maxWidth, minHeight, maxHeight)"
                )
        )
        @Stable
        fun restrictConstraints(
            minWidth: Int,
            maxWidth: Int,
            minHeight: Int,
            maxHeight: Int,
            prioritizeWidth: Boolean = true
        ): Constraints {
            return if (prioritizeWidth) {
                fitPrioritizingWidth(minWidth, maxWidth, minHeight, maxHeight)
            } else {
                fitPrioritizingHeight(minWidth, maxWidth, minHeight, maxHeight)
            }
        }

        /**
         * Returns [Constraints] that match as close as possible to the values passed. If the
         * dimensions are outside of those that can be represented, the constraints are limited to
         * those that can be represented.
         *
         * [Constraints] is a `value class` based on a [Long] and 4 integers must be limited to fit
         * within its size. The larger dimension has up to 18 bits (262,143) and the smaller as few
         * as 13 bits (8191). The width is granted as much space as it needs or caps the size to 18
         * bits. The height is given the remaining space.
         *
         * This can be useful when layout constraints are possible to be extremely large, but not
         * everything is possible to display on the device. For example a text layout where an
         * entire chapter of a book is measured in one Layout and it isn't possible to break up the
         * content to show in a `LazyColumn`.
         */
        @Stable
        fun fitPrioritizingWidth(
            minWidth: Int,
            maxWidth: Int,
            minHeight: Int,
            maxHeight: Int,
        ): Constraints {
            val minW = min(minWidth, MaxFocusMask - 1)
            val maxW =
                if (maxWidth == Infinity) {
                    Infinity
                } else {
                    min(maxWidth, MaxFocusMask - 1)
                }
            val consumed = if (maxW == Infinity) minW else maxW
            val maxAllowed = maxAllowedForSize(consumed)
            val maxH = if (maxHeight == Infinity) Infinity else min(maxAllowed, maxHeight)
            val minH = min(maxAllowed, minHeight)
            return Constraints(minW, maxW, minH, maxH)
        }

        /**
         * Returns [Constraints] that match as close as possible to the values passed. If the
         * dimensions are outside of those that can be represented, the constraints are limited to
         * those that can be represented.
         *
         * [Constraints] is a `value class` based on a [Long] and 4 integers must be limited to fit
         * within its size. The larger dimension has up to 18 bits (262,143) and the smaller as few
         * as 13 bits (8191). The height is granted as much space as it needs or caps the size to 18
         * bits. The width is given the remaining space.
         *
         * This can be useful when layout constraints are possible to be extremely large, but not
         * everything is possible to display on the device. For example a text layout where an
         * entire chapter of a book is measured in one Layout and it isn't possible to break up the
         * content to show in a `LazyColumn`.
         */
        @Stable
        fun fitPrioritizingHeight(
            minWidth: Int,
            maxWidth: Int,
            minHeight: Int,
            maxHeight: Int,
        ): Constraints {
            val minH = min(minHeight, MaxFocusMask - 1)
            val maxH =
                if (maxHeight == Infinity) {
                    Infinity
                } else {
                    min(maxHeight, MaxFocusMask - 1)
                }
            val consumed = if (maxH == Infinity) minH else maxH
            val maxAllowed = maxAllowedForSize(consumed)
            val maxW = if (maxWidth == Infinity) Infinity else min(maxAllowed, maxWidth)
            val minW = min(maxAllowed, minWidth)
            return Constraints(minW, maxW, minH, maxH)
        }
    }
}

// Redefinition of Constraints.Infinity to bypass the companion object
private const val Infinity = Int.MAX_VALUE

/**
 * The bit distribution when the focus of the bits should be on the width, but only a minimal
 * difference in focus.
 *
 * 16 bits assigned to width, 15 bits assigned to height.
 */
private const val MinFocusWidth = 0x02

/**
 * The bit distribution when the focus of the bits should be on the width, and a maximal number of
 * bits assigned to the width.
 *
 * 18 bits assigned to width, 13 bits assigned to height.
 */
private const val MaxFocusWidth = 0x03

/**
 * The bit distribution when the focus of the bits should be on the height, but only a minimal
 * difference in focus.
 *
 * 15 bits assigned to width, 16 bits assigned to height.
 */
private const val MinFocusHeight = 0x01

/**
 * The bit distribution when the focus of the bits should be on the height, and a a maximal number
 * of bits assigned to the height.
 *
 * 13 bits assigned to width, 18 bits assigned to height.
 */
private const val MaxFocusHeight = 0x00

/**
 * The mask to retrieve the focus ([MinFocusWidth], [MaxFocusWidth], [MinFocusHeight],
 * [MaxFocusHeight]).
 */
private const val FocusMask = 0x03L

/** The number of bits used for the focused dimension when there is minimal focus. */
private const val MinFocusBits = 16
private const val MaxAllowedForMinFocusBits = (1 shl (31 - MinFocusBits)) - 2

/** The mask to use for the focused dimension when there is minimal focus. */
private const val MinFocusMask = 0xFFFF // 64K (16 bits)

/** The number of bits used for the non-focused dimension when there is minimal focus. */
private const val MinNonFocusBits = 15
private const val MaxAllowedForMinNonFocusBits = (1 shl (31 - MinNonFocusBits)) - 2

/** The mask to use for the non-focused dimension when there is minimal focus. */
private const val MinNonFocusMask = 0x7FFF // 32K (15 bits)

/** The number of bits to use for the focused dimension when there is maximal focus. */
private const val MaxFocusBits = 18
private const val MaxAllowedForMaxFocusBits = (1 shl (31 - MaxFocusBits)) - 2

/** The mask to use for the focused dimension when there is maximal focus. */
private const val MaxFocusMask = 0x3FFFF // 256K (18 bits)

/** The number of bits to use for the non-focused dimension when there is maximal focus. */
private const val MaxNonFocusBits = 13
private const val MaxAllowedForMaxNonFocusBits = (1 shl (31 - MaxNonFocusBits)) - 2

/** The mask to use for the non-focused dimension when there is maximal focus. */
private const val MaxNonFocusMask = 0x1FFF // 8K (13 bits)

// Wrap those throws in functions to avoid inlining the string building at the call sites
private fun throwInvalidConstraintException(widthVal: Int, heightVal: Int) {
    throw IllegalArgumentException(
        "Can't represent a width of $widthVal and height of $heightVal in Constraints"
    )
}

private fun throwInvalidSizeException(size: Int): Nothing {
    throw IllegalArgumentException("Can't represent a size of $size in Constraints")
}

/** Creates a [Constraints], only checking that the values fit in the packed Long. */
internal fun createConstraints(
    minWidth: Int,
    maxWidth: Int,
    minHeight: Int,
    maxHeight: Int
): Constraints {
    val heightVal = if (maxHeight == Infinity) minHeight else maxHeight
    val heightBits = bitsNeedForSizeUnchecked(heightVal)

    val widthVal = if (maxWidth == Infinity) minWidth else maxWidth
    val widthBits = bitsNeedForSizeUnchecked(widthVal)

    if (widthBits + heightBits > 31) {
        throwInvalidConstraintException(widthVal, heightVal)
    }

    // Same as if (maxWidth == Infinity) 0 else maxWidth + 1 but branchless
    // in DEX and saves 2 instructions on aarch64. Relies on integer overflow
    // since Infinity == Int.MAX_VALUE
    var maxWidthValue = maxWidth + 1
    maxWidthValue = maxWidthValue and (maxWidthValue shr 31).inv()

    var maxHeightValue = maxHeight + 1
    maxHeightValue = maxHeightValue and (maxHeightValue shr 31).inv()

    val focus =
        when (widthBits) {
            MinNonFocusBits -> MinFocusHeight
            MinFocusBits -> MinFocusWidth
            MaxNonFocusBits -> MaxFocusHeight
            MaxFocusBits -> MaxFocusWidth
            else -> 0x00 // can't happen, widthBits is computed from bitsNeedForSizeUnchecked()
        }

    val minHeightOffset = minHeightOffsets(indexToBitOffset(focus))
    val maxHeightOffset = minHeightOffset + 31

    val value =
        focus.toLong() or
            (minWidth.toLong() shl 2) or
            (maxWidthValue.toLong() shl 33) or
            (minHeight.toLong() shl minHeightOffset) or
            (maxHeightValue.toLong() shl maxHeightOffset)
    return Constraints(value)
}

private fun bitsNeedForSizeUnchecked(size: Int): Int {
    return when {
        size < MaxNonFocusMask -> MaxNonFocusBits
        size < MinNonFocusMask -> MinNonFocusBits
        size < MinFocusMask -> MinFocusBits
        size < MaxFocusMask -> MaxFocusBits
        else -> 255
    }
}

private fun maxAllowedForSize(size: Int): Int {
    return when {
        size < MaxNonFocusMask -> MaxAllowedForMaxNonFocusBits
        size < MinNonFocusMask -> MaxAllowedForMinNonFocusBits
        size < MinFocusMask -> MaxAllowedForMinFocusBits
        size < MaxFocusMask -> MaxAllowedForMaxFocusBits
        else -> throwInvalidSizeException(size)
    }
}

/**
 * Create a [Constraints]. [minWidth] and [minHeight] must be positive and [maxWidth] and
 * [maxHeight] must be greater than or equal to [minWidth] and [minHeight], respectively, or
 * [Infinity][Constraints.Infinity].
 */
@Stable
fun Constraints(
    minWidth: Int = 0,
    maxWidth: Int = Infinity,
    minHeight: Int = 0,
    maxHeight: Int = Infinity
): Constraints {
    requirePrecondition(maxWidth >= minWidth) {
        "maxWidth($maxWidth) must be >= than minWidth($minWidth)"
    }
    requirePrecondition(maxHeight >= minHeight) {
        "maxHeight($maxHeight) must be >= than minHeight($minHeight)"
    }
    requirePrecondition(minWidth >= 0 && minHeight >= 0) {
        "minWidth($minWidth) and minHeight($minHeight) must be >= 0"
    }
    return createConstraints(minWidth, maxWidth, minHeight, maxHeight)
}

/**
 * Takes [otherConstraints] and returns the result of coercing them in the current constraints. Note
 * this means that any size satisfying the resulting constraints will satisfy the current
 * constraints, but they might not satisfy the [otherConstraints] when the two set of constraints
 * are disjoint. Examples (showing only width, height works the same): (minWidth=2,
 * maxWidth=10).constrain(minWidth=7, maxWidth=12) -> (minWidth = 7, maxWidth = 10) (minWidth=2,
 * maxWidth=10).constrain(minWidth=11, maxWidth=12) -> (minWidth=10, maxWidth=10) (minWidth=2,
 * maxWidth=10).constrain(minWidth=5, maxWidth=7) -> (minWidth=5, maxWidth=7)
 */
fun Constraints.constrain(otherConstraints: Constraints) =
    Constraints(
        minWidth = otherConstraints.minWidth.fastCoerceIn(minWidth, maxWidth),
        maxWidth = otherConstraints.maxWidth.fastCoerceIn(minWidth, maxWidth),
        minHeight = otherConstraints.minHeight.fastCoerceIn(minHeight, maxHeight),
        maxHeight = otherConstraints.maxHeight.fastCoerceIn(minHeight, maxHeight)
    )

/** Takes a size and returns the closest size to it that satisfies the constraints. */
@Stable
fun Constraints.constrain(size: IntSize) =
    IntSize(
        width = size.width.fastCoerceIn(minWidth, maxWidth),
        height = size.height.fastCoerceIn(minHeight, maxHeight)
    )

/** Takes a width and returns the closest size to it that satisfies the constraints. */
@Stable fun Constraints.constrainWidth(width: Int) = width.fastCoerceIn(minWidth, maxWidth)

/** Takes a height and returns the closest size to it that satisfies the constraints. */
@Stable fun Constraints.constrainHeight(height: Int) = height.fastCoerceIn(minHeight, maxHeight)

/** Takes a size and returns whether it satisfies the current constraints. */
@Stable
fun Constraints.isSatisfiedBy(size: IntSize): Boolean {
    return size.width in minWidth..maxWidth && size.height in minHeight..maxHeight
}

/** Returns the Constraints obtained by offsetting the current instance with the given values. */
@Stable
fun Constraints.offset(horizontal: Int = 0, vertical: Int = 0) =
    Constraints(
        (minWidth + horizontal).fastCoerceAtLeast(0),
        addMaxWithMinimum(maxWidth, horizontal),
        (minHeight + vertical).fastCoerceAtLeast(0),
        addMaxWithMinimum(maxHeight, vertical)
    )

private fun addMaxWithMinimum(max: Int, value: Int): Int {
    return if (max == Infinity) {
        max
    } else {
        (max + value).fastCoerceAtLeast(0)
    }
}

// NOTE: The functions below are not on the companion object to avoid unnecessary
//       static resolutions. Even inlined, calling these functions requires a
//       static inline access in case initialization is required. Making them top
//       level functions addresses this issues.

// The following describes the computations performed by the 4 functions below.
// Each function is a mapping from an index to a value.
//
// index = 0 -> MaxFocusHeight = 15
// index = 1 -> MinFocusHeight = 17
// index = 2 -> MinFocusWidth  = 18
// index = 3 -> MaxFocusWidth  = 20
//
// MinHeightOffsets = 2 + 13 + (index and 0x1 shl 1) + ((index and 0x2 shr 1) * 3)
//
// The sub-expression `(index and 0x1 shl 1) + ((index and 0x2 shr 1) * 3)` applies
// the following mapping:
//
// 0 -> 0
// 1 -> 2
// 2 -> 3
// 3 -> 5
//
// From this mapping we can build all the other mappings:
//
// index = 0 -> MaxNonFocusMask = 0x1fff (13 bits)
// index = 1 -> MinNonFocusMask = 0x7fff (15 bits)
// index = 2 -> MinFocusMask    = 0xffff (16 bits)
// index = 3 -> MaxFocusMask    = 0x3ffff (18 bits)
//
// WidthMask = (1 shl (13 + (index and 0x1 shl 1) + ((index and 0x2 shr 1) * 3))) - 1
//
// Here we used the pattern `(1 << n) - 1` to set all first n bits to 1.
//
// index = 0 -> MaxFocusMask    = 0x3ffff
// index = 1 -> MinFocusMask    = 0xffff
// index = 2 -> MinNonFocusMask = 0x7fff
// index = 3 -> MaxNonFocusMask = 0x1fff
//
// HeightMask = (1 shl (18 - (index and 0x1 shl 1) - ((index and 0x2 shr 1) * 3))) - 1
//
// To optimize computations, the common part that follows is factored into the
// `indexToBitOffset` function:
//
// (index and 0x1 shl 1) + ((index and 0x2 shr 1) * 3)
//
// We are therefore left with:
//
// MinHeightOffsets = 15 + indexToBitOffset(index)
// WidthMask = (1 shl (13 + indexToBitOffset(index))) - 1
// HeightMask = (1 shl (18 - indexToBitOffset(index))) - 1

/**
 * Maps an index (MaxFocusHeight, MinFocusHeight, MinFocusWidth, MaxFocusWidth) to a "bit offset":
 * 0, 2, 3 or 5. That bit offset is used by [minHeightOffsets], [widthMask], and [heightMask] to
 * compute other values without the need of lookup tables. For instance, [minHeightOffsets] returns
 * `2 + 13 + bitOffset`.
 */
private inline fun indexToBitOffset(index: Int) =
    (index and 0x1 shl 1) + ((index and 0x2 shr 1) * 3)

/**
 * Minimum Height shift offsets into Long value, indexed by FocusMask Max offsets are these + 31
 * Width offsets are always either 2 (min) or 33 (max)
 */
private inline fun minHeightOffsets(bitOffset: Int) = 15 + bitOffset

/** The mask to use for both minimum and maximum width. */
private inline fun widthMask(bitOffset: Int) = (1 shl (13 + bitOffset)) - 1

/** The mask to use for both minimum and maximum height. */
private inline fun heightMask(bitOffset: Int) = (1 shl (18 - bitOffset)) - 1
