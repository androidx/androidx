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

/**
 * Dimension value representing Em. 1 Em is defined to be the font size when doing the text layout.
 * For example:
 *     Text(style = TextStyle(fontSize = 20.sp, letterSpacing = 0.2.em)
 * The letter spacing rendered equals to 4.sp = 20.sp * 0.2em.
 *
 * Em is used only in typography related APIs. Since it's a relative length unit, the
 * change of font size will also influence the computed Em length.
 * For example:
 *     Text {
 *         Span(style = TextStyle(letterSpacing = 0.5.em) {
 *             // letter spacing for "Hello" equals to 7.sp
 *             Span(text = "Hello", style = TextStyle(fontSize = 14.sp))
 *             // letter spacing for "World" equals to 9.sp
 *             Span(text = "World", style = TextStyle(fontSize = 18.sp))
 *         }
 *     }
 */
data /*inline*/ class Em(val value: Float) {
    /**
     * Add two [Em]s together.
     */
    inline operator fun plus(other: Em) =
        Em(value = this.value + other.value)

    /**
     * Subtract a Em from another one.
     */
    inline operator fun minus(other: Em) =
        Em(value = this.value - other.value)

    /**
     * This is the same as multiplying the Em by -1.0.
     */
    inline operator fun unaryMinus() = Em(-value)

    /**
     * Divide a Em by a scalar.
     */
    inline operator fun div(other: Float): Em =
        Em(value = value / other)

    inline operator fun div(other: Int): Em =
        Em(value = value / other)

    /**
     * Divide by another Em to get a scalar.
     */
    inline operator fun div(other: Em): Float = value / other.value

    /**
     * Multiply a Em by a scalar.
     */
    inline operator fun times(other: Float): Em =
        Em(value = value * other)

    inline operator fun times(other: Int): Em =
        Em(value = value * other)

    /**
     * Support comparing Dimensions with comparison operators.
     */
    inline operator fun compareTo(other: Em) = value.compareTo(other.value)
}

/**
 * Create a [Em] using an [Int]:
 *     val letterSpacing = 1
 *     val x = letterSpacing.em
 *     // -- or --
 *     val y = 1.em
 */
inline val Int.em: Em get() = Em(value = this.toFloat())

/**
 * Create a [Em] using a [Double]:
 *     val letterSpacing = 1.0
 *     val x = letterSpacing.em
 *     // -- or --
 *     val y = 1.0.em
 */
inline val Double.em: Em get() = Em(value = this.toFloat())

/**
 * Create a [Em] using a [Float]:
 *     val letterSpacing = 1f
 *     val x = letterSpacing.em
 *     // -- or --
 *     val y = 1f.em
 */
inline val Float.em: Em get() = Em(value = this)

inline operator fun Float.times(other: Em) =
    Em(this * other.value)

inline operator fun Double.times(other: Em) =
    Em(this.toFloat() * other.value)

inline operator fun Int.times(other: Em) =
    Em(this * other.value)

inline fun min(a: Em, b: Em): Em = Em(value = kotlin.math.min(a.value, b.value))

inline fun max(a: Em, b: Em): Em = Em(value = kotlin.math.max(a.value, b.value))

/**
 * Ensures that this value lies in the specified range [minimumValue]..[maximumValue].
 *
 * @return this value if it's in the range, or [minimumValue] if this value is less than
 * [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
 */
inline fun Em.coerceIn(minimumValue: Em, maximumValue: Em): Em =
    Em(value = value.coerceIn(minimumValue.value, maximumValue.value))

/**
 * Ensures that this value is not less than the specified [minimumValue].
 *
 * @return this value if it's greater than or equal to the [minimumValue] or the
 * [minimumValue] otherwise.
 */
inline fun Em.coerceAtLeast(minimumValue: Em): Em =
    Em(value = value.coerceAtLeast(minimumValue.value))

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 *
 * @return this value if it's less than or equal to the [maximumValue] or the
 * [maximumValue] otherwise.
 */
inline fun Em.coerceAtMost(maximumValue: Em): Em =
    Em(value = value.coerceAtMost(maximumValue.value))

/**
 * Convert a value of [Sp] to [Em].
 *
 * @param fontSize the context font size where [Em] is used.
 */
inline fun Sp.toEm(fontSize: Sp): Em = Em(value = value / fontSize.value)

/**
 * Linearly interpolate between two [Em]s.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
fun lerp(start: Em, stop: Em, fraction: Float): Em {
    return Em(androidx.ui.lerp(start.value, stop.value, fraction))
}