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

package androidx.compose.ui.node

import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.node.DpTouchBoundsExpansion.Companion.Absolute
import androidx.compose.ui.node.TouchBoundsExpansion.Companion.Absolute
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmInline

/**
 * Describes the expansion of a [PointerInputModifierNode]'s touch bounds along each edges. See
 * [TouchBoundsExpansion] factories and [Absolute] for convenient ways to build
 * [TouchBoundsExpansion].
 *
 * @see PointerInputModifierNode.touchBoundsExpansion
 */
@JvmInline
value class TouchBoundsExpansion internal constructor(private val packedValue: Long) {
    companion object {
        /**
         * Creates a [TouchBoundsExpansion] that's unaware of [LayoutDirection]. The `left`, `top`,
         * `right` and `bottom` represent the amount of pixels that the touch bounds is expanded
         * along the corresponding edge. Each value must be in the range of 0 to 32767 (inclusive).
         */
        fun Absolute(
            left: Int = 0,
            top: Int = 0,
            right: Int = 0,
            bottom: Int = 0,
        ): TouchBoundsExpansion {
            requirePrecondition(left in 0..MAX_VALUE) {
                "Start must be in the range of 0 .. $MAX_VALUE"
            }
            requirePrecondition(top in 0..MAX_VALUE) {
                "Top must be in the range of 0 .. $MAX_VALUE"
            }
            requirePrecondition(right in 0..MAX_VALUE) {
                "End must be in the range of 0 .. $MAX_VALUE"
            }
            requirePrecondition(bottom in 0..MAX_VALUE) {
                "Bottom must be in the range of 0 .. $MAX_VALUE"
            }
            return TouchBoundsExpansion(pack(left, top, right, bottom, false))
        }

        /** Constant that represents no touch bounds expansion. */
        val None = TouchBoundsExpansion(0)

        internal fun pack(
            start: Int,
            top: Int,
            end: Int,
            bottom: Int,
            isLayoutDirectionAware: Boolean,
        ): Long {
            return trimAndShift(start, 0) or
                trimAndShift(top, 1) or
                trimAndShift(end, 2) or
                trimAndShift(bottom, 3) or
                if (isLayoutDirectionAware) IS_LAYOUT_DIRECTION_AWARE else 0L
        }

        private const val MASK = 0x7FFF

        private const val SHIFT = 15

        internal const val MAX_VALUE = MASK

        private const val IS_LAYOUT_DIRECTION_AWARE = 1L shl 63

        // We stored all
        private fun unpack(packedValue: Long, position: Int): Int =
            (packedValue shr (position * SHIFT)).toInt() and MASK

        private fun trimAndShift(int: Int, position: Int): Long =
            (int and MASK).toLong() shl (position * SHIFT)
    }

    /**
     * The amount of pixels the touch bounds should be expanded along the start edge. When
     * [isLayoutDirectionAware] is `true`, it's applied to the left edge when [LayoutDirection] is
     * [LayoutDirection.Ltr] and vice versa. When [isLayoutDirectionAware] is `false`, it's always
     * applied to the left edge.
     */
    val start: Int
        get() = unpack(packedValue, 0)

    /** The amount of pixels the touch bounds should be expanded along the top edge. */
    val top: Int
        get() = unpack(packedValue, 1)

    /**
     * The amount of pixels the touch bounds should be expanded along the end edge. When
     * [isLayoutDirectionAware] is `true`, it's applied to the left edge when [LayoutDirection] is
     * [LayoutDirection.Ltr] and vice versa. When [isLayoutDirectionAware] is `false`, it's always
     * applied to the left edge.
     */
    val end: Int
        get() = unpack(packedValue, 2)

    /** The amount of pixels the touch bounds should be expanded along the bottom edge. */
    val bottom: Int
        get() = unpack(packedValue, 3)

    /**
     * Whether this [TouchBoundsExpansion] is aware of [LayoutDirection] or not. See [start] and
     * [end] for more details.
     */
    val isLayoutDirectionAware: Boolean
        get() = (packedValue and IS_LAYOUT_DIRECTION_AWARE) != 0L

    /** Returns the amount of pixels the touch bounds is expanded towards left. */
    internal fun computeLeft(layoutDirection: LayoutDirection): Int {
        return if (!isLayoutDirectionAware || layoutDirection == LayoutDirection.Ltr) {
            start
        } else {
            end
        }
    }

    /** Returns the amount of pixels the touch bounds is expanded towards right. */
    internal fun computeRight(layoutDirection: LayoutDirection): Int {
        return if (!isLayoutDirectionAware || layoutDirection == LayoutDirection.Ltr) {
            end
        } else {
            start
        }
    }
}

/**
 * Describes the expansion of a [PointerInputModifierNode]'s touch bounds along each edges using
 * [Dp] for units. See [DpTouchBoundsExpansion] factories and [Absolute] for convenient ways to
 * build [DpTouchBoundsExpansion].
 *
 * @see PointerInputModifierNode.touchBoundsExpansion
 */
@Suppress("DataClassDefinition")
data class DpTouchBoundsExpansion(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp,
    val isLayoutDirectionAware: Boolean,
) {
    init {
        requirePrecondition(start.value >= 0) { "Left must be non-negative" }
        requirePrecondition(top.value >= 0) { "Top must be non-negative" }
        requirePrecondition(end.value >= 0) { "Right must be non-negative" }
        requirePrecondition(bottom.value >= 0) { "Bottom must be non-negative" }
    }

    fun roundToTouchBoundsExpansion(density: Density) =
        with(density) {
            TouchBoundsExpansion(
                packedValue =
                    TouchBoundsExpansion.pack(
                        start.roundToPx(),
                        top.roundToPx(),
                        end.roundToPx(),
                        bottom.roundToPx(),
                        isLayoutDirectionAware,
                    )
            )
        }

    companion object {
        /**
         * Creates a [DpTouchBoundsExpansion] that's unaware of [LayoutDirection]. The `left`,
         * `top`, `right` and `bottom` represent the distance that the touch bounds is expanded
         * along the corresponding edge.
         */
        fun Absolute(
            left: Dp = 0.dp,
            top: Dp = 0.dp,
            right: Dp = 0.dp,
            bottom: Dp = 0.dp,
        ): DpTouchBoundsExpansion {
            return DpTouchBoundsExpansion(left, top, right, bottom, false)
        }
    }
}

/**
 * Creates a [TouchBoundsExpansion] that's aware of [LayoutDirection]. See
 * [TouchBoundsExpansion.start] and [TouchBoundsExpansion.end] for more details about
 * [LayoutDirection].
 *
 * The `start`, `top`, `end` and `bottom` represent the amount of pixels that the touch bounds is
 * expanded along the corresponding edge. Each value must be in the range of 0 to 32767 (inclusive).
 */
fun TouchBoundsExpansion(
    start: Int = 0,
    top: Int = 0,
    end: Int = 0,
    bottom: Int = 0,
): TouchBoundsExpansion {
    requirePrecondition(start in 0..TouchBoundsExpansion.MAX_VALUE) {
        "Start must be in the range of 0 .. ${TouchBoundsExpansion.MAX_VALUE}"
    }
    requirePrecondition(top in 0..TouchBoundsExpansion.MAX_VALUE) {
        "Top must be in the range of 0 .. ${TouchBoundsExpansion.MAX_VALUE}"
    }
    requirePrecondition(end in 0..TouchBoundsExpansion.MAX_VALUE) {
        "End must be in the range of 0 .. ${TouchBoundsExpansion.MAX_VALUE}"
    }
    requirePrecondition(bottom in 0..TouchBoundsExpansion.MAX_VALUE) {
        "Bottom must be in the range of 0 .. ${TouchBoundsExpansion.MAX_VALUE}"
    }
    return TouchBoundsExpansion(
        packedValue = TouchBoundsExpansion.pack(start, top, end, bottom, true)
    )
}

/**
 * Creates a [DpTouchBoundsExpansion] that's aware of [LayoutDirection]. See
 * [DpTouchBoundsExpansion.start] and [DpTouchBoundsExpansion.end] for more details about
 * [LayoutDirection].
 *
 * The `start`, `top`, `end` and `bottom` represent the distance that the touch bounds is expanded
 * along the corresponding edge.
 */
fun DpTouchBoundsExpansion(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
): DpTouchBoundsExpansion {
    return DpTouchBoundsExpansion(start, top, end, bottom, true)
}
