/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.jvm.JvmInline

/** Constructs a [IntOffset] from [x] and [y] position [Int] values. */
@Stable inline fun IntOffset(x: Int, y: Int): IntOffset = IntOffset(packInts(x, y))

/**
 * A two-dimensional position using [Int] pixels for units.
 *
 * To create an [IntOffset], call the top-level function that accepts an x/y pair of coordinates:
 * ```
 * val offset = IntOffset(x, y)
 * ```
 *
 * The primary constructor of [IntOffset] is intended to be used with the [packedValue] property to
 * allow storing offsets in arrays or collections of primitives without boxing.
 *
 * @param packedValue [Long] value encoding the [x] and [y] components of the [IntOffset]. Encoded
 *   values can be obtained by using the [packedValue] property of existing [IntOffset] instances.
 */
@Immutable
@JvmInline
value class IntOffset(val packedValue: Long) {
    /** The horizontal aspect of the position in [Int] pixels. */
    @Stable
    val x: Int
        get() = unpackInt1(packedValue)

    /** The vertical aspect of the position in [Int] pixels. */
    @Stable
    val y: Int
        get() = unpackInt2(packedValue)

    @Stable inline operator fun component1(): Int = x

    @Stable inline operator fun component2(): Int = y

    /** Returns a copy of this IntOffset instance optionally overriding the x or y parameter */
    fun copy(x: Int = unpackInt1(packedValue), y: Int = unpackInt2(packedValue)) =
        IntOffset(packInts(x, y))

    /** Subtract a [IntOffset] from another one. */
    @Stable
    operator fun minus(other: IntOffset) =
        IntOffset(
            packInts(
                unpackInt1(packedValue) - unpackInt1(other.packedValue),
                unpackInt2(packedValue) - unpackInt2(other.packedValue)
            )
        )

    /** Add a [IntOffset] to another one. */
    @Stable
    operator fun plus(other: IntOffset) =
        IntOffset(
            packInts(
                unpackInt1(packedValue) + unpackInt1(other.packedValue),
                unpackInt2(packedValue) + unpackInt2(other.packedValue)
            )
        )

    /** Returns a new [IntOffset] representing the negation of this point. */
    @Stable
    operator fun unaryMinus() =
        IntOffset(packInts(-unpackInt1(packedValue), -unpackInt2(packedValue)))

    /**
     * Multiplication operator.
     *
     * Returns an IntOffset whose coordinates are the coordinates of the left-hand-side operand (an
     * IntOffset) multiplied by the scalar right-hand-side operand (a Float). The result is rounded
     * to the nearest integer.
     */
    @Stable
    operator fun times(operand: Float): IntOffset =
        IntOffset(
            packInts(
                (unpackInt1(packedValue) * operand).fastRoundToInt(),
                (unpackInt2(packedValue) * operand).fastRoundToInt()
            )
        )

    /**
     * Division operator.
     *
     * Returns an IntOffset whose coordinates are the coordinates of the left-hand-side operand (an
     * IntOffset) divided by the scalar right-hand-side operand (a Float). The result is rounded to
     * the nearest integer.
     */
    @Stable
    operator fun div(operand: Float): IntOffset =
        IntOffset(
            packInts(
                (unpackInt1(packedValue) / operand).fastRoundToInt(),
                (unpackInt2(packedValue) / operand).fastRoundToInt()
            )
        )

    /**
     * Modulo (remainder) operator.
     *
     * Returns an IntOffset whose coordinates are the remainder of dividing the coordinates of the
     * left-hand-side operand (an IntOffset) by the scalar right-hand-side operand (an Int).
     */
    @Stable
    operator fun rem(operand: Int) =
        IntOffset(packInts(unpackInt1(packedValue) % operand, unpackInt2(packedValue) % operand))

    @Stable override fun toString(): String = "($x, $y)"

    companion object {
        val Zero = IntOffset(0x0L)
        val Max = IntOffset(0x7FFF_FFFF_7FFF_FFFF)
    }
}

/**
 * Linearly interpolate between two [IntOffset]s.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning that the
 * interpolation has not started, returning [start] (or something equivalent to [start]), 1.0
 * meaning that the interpolation has finished, returning [stop] (or something equivalent to
 * [stop]), and values in between meaning that the interpolation is at the relevant point on the
 * timeline between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and 1.0, so
 * negative values and values greater than 1.0 are valid.
 */
@Stable
fun lerp(start: IntOffset, stop: IntOffset, fraction: Float): IntOffset =
    IntOffset(packInts(lerp(start.x, stop.x, fraction), lerp(start.y, stop.y, fraction)))

/** Converts the [IntOffset] to an [Offset]. */
@Stable inline fun IntOffset.toOffset() = Offset(x.toFloat(), y.toFloat())

@Stable operator fun Offset.plus(offset: IntOffset): Offset = Offset(x + offset.x, y + offset.y)

@Stable operator fun Offset.minus(offset: IntOffset): Offset = Offset(x - offset.x, y - offset.y)

@Stable operator fun IntOffset.plus(offset: Offset): Offset = Offset(x + offset.x, y + offset.y)

@Stable operator fun IntOffset.minus(offset: Offset): Offset = Offset(x - offset.x, y - offset.y)

/** Round a [Offset] down to the nearest [Int] coordinates. */
@Stable fun Offset.round(): IntOffset = IntOffset(packInts(x.fastRoundToInt(), y.fastRoundToInt()))
