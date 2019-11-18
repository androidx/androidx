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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.ui.core

import androidx.compose.Immutable
import kotlin.math.max
import kotlin.math.min

/**
 * Dimension value representing scaled pixels (sp). Font related APIs specify their
 * dimensions such as font size in SP with Sp objects. Sp are normally
 * defined using [sp], which can be applied to [Int], [Double], and [Float].
 *     val leftMargin = 10.sp
 *     val rightMargin = 10f.sp
 *     val topMargin = 20.0.sp
 *     val bottomMargin = 10.sp
 * Drawing is done in pixels. To retrieve the pixel size of a Dp, use [toPx]:
 *     val lineThicknessPx = lineThickness.toPx(context)
 * [toPx] is normally needed only for painting operations.
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Immutable
data /*inline*/ class Sp(val value: Float) {
    /**
     * Add two [Sp]s together.
     */
    inline operator fun plus(other: Sp) = checkNotInherit(this, other) {
        Sp(value = this.value + other.value)
    }

    /**
     * Subtract a Sp from another one.
     */
    inline operator fun minus(other: Sp) = checkNotInherit(this, other) {
        Sp(value = this.value - other.value)
    }

    /**
     * This is the same as multiplying the Sp by -1.0.
     */
    inline operator fun unaryMinus() = checkNotInherit(this) { Sp(-value) }

    /**
     * Divide a Sp by a scalar.
     */
    inline operator fun div(other: Float): Sp = checkNotInherit(this) {
        Sp(value = value / other)
    }

    inline operator fun div(other: Int): Sp = checkNotInherit(this) {
        Sp(value = value / other)
    }

    /**
     * Divide by another Sp to get a scalar.
     */
    inline operator fun div(other: Sp): Float = checkNotInherit(this, other) {
        value / other.value
    }

    /**
     * Multiply a Sp by a scalar.
     */
    inline operator fun times(other: Float): Sp = checkNotInherit(this) {
        Sp(value = value * other)
    }

    inline operator fun times(other: Int): Sp = checkNotInherit(this) {
        Sp(value = value * other)
    }

    /**
     * Support comparing Dimensions with comparison operators.
     */
    inline operator fun compareTo(other: Sp) = value.compareTo(other.value)

    companion object {
        /**
         * Infinite Sp dimension.
         */
        val Infinity = Sp(value = Float.POSITIVE_INFINITY)

        /**
         * A special Sp instance for representing inheriting from the parent value.
         *
         * Do not set this value unless the API doc explicitly mentioned that [Sp.Inherit] is
         * allowed.
         */
        val Inherit = Sp(value = Float.NEGATIVE_INFINITY)
    }
}

/**
 * Create a [Sp] using an [Int]:
 *     val fontSize = 10
 *     val x = fontSize.sp
 *     // -- or --
 *     val y = 10.sp
 */
inline val Int.sp: Sp get() = Sp(value = this.toFloat())

/**
 * Create a [Sp] using a [Double]:
 *     val fontSize = 10.0
 *     val x = fontSize.sp
 *     // -- or --
 *     val y = 10.0.sp
 */
inline val Double.sp: Sp get() = Sp(value = this.toFloat())

/**
 * Create a [Sp] using a [Float]:
 *     val fontSize = 10f
 *     val x = fontSize.sp
 *     // -- or --
 *     val y = 10f.sp
 */
inline val Float.sp: Sp get() = Sp(value = this)

inline operator fun Float.times(other: Sp) = checkNotInherit(other) {
    Sp(this * other.value)
}

inline operator fun Double.times(other: Sp) = checkNotInherit(other) {
    Sp(this.toFloat() * other.value)
}

inline operator fun Int.times(other: Sp) = checkNotInherit(other) {
    Sp(this * other.value)
}

inline fun min(a: Sp, b: Sp): Sp = checkNotInherit(a, b) {
    Sp(value = min(a.value, b.value))
}

inline fun max(a: Sp, b: Sp): Sp = checkNotInherit(a, b) {
    Sp(value = max(a.value, b.value))
}

/**
 * Ensures that this value lies in the specified range [minimumValue]..[maximumValue].
 *
 * @return this value if it's in the range, or [minimumValue] if this value is less than
 * [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
 */
inline fun Sp.coerceIn(minimumValue: Sp, maximumValue: Sp): Sp =
    checkNotInherit(this, minimumValue, maximumValue) {
        Sp(value = value.coerceIn(minimumValue.value, maximumValue.value))
    }

/**
 * Ensures that this value is not less than the specified [minimumValue].
 *
 * @return this value if it's greater than or equal to the [minimumValue] or the
 * [minimumValue] otherwise.
 */
inline fun Sp.coerceAtLeast(minimumValue: Sp): Sp = checkNotInherit(this, minimumValue) {
    Sp(value = value.coerceAtLeast(minimumValue.value))
}

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 *
 * @return this value if it's less than or equal to the [maximumValue] or the
 * [maximumValue] otherwise.
 */
inline fun Sp.coerceAtMost(maximumValue: Sp): Sp = checkNotInherit(this, maximumValue) {
    Sp(value = value.coerceAtMost(maximumValue.value))
}

/**
 * Returns true if this value is [Sp.Inherit]
 */
inline fun Sp.isInherit(): Boolean = this.value == Float.NEGATIVE_INFINITY

@PublishedApi
internal inline fun <T> checkNotInherit(a: Sp, block: () -> T): T {
    if (a.isInherit()) throw IllegalArgumentException("Cannot perform operation for Sp.Inherit")
    return block()
}

@PublishedApi
internal inline fun <T> checkNotInherit(a: Sp, b: Sp, block: () -> T): T {
    if (a.isInherit() || b.isInherit())
        throw IllegalArgumentException("Cannot perform operation for Sp.Inherit")
    return block()
}

@PublishedApi
internal inline fun <T> checkNotInherit(a: Sp, b: Sp, c: Sp, block: () -> T): T {
    if (a.isInherit() || b.isInherit() || c.isInherit())
        throw IllegalArgumentException("Cannot perform operation for Sp.Inherit")
    return block()
}

/**
 * Linearly interpolate between two [Sp]s.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
fun lerp(start: Sp, stop: Sp, fraction: Float): Sp = checkNotInherit(start, stop) {
    Sp(androidx.ui.lerp(start.value, stop.value, fraction))
}