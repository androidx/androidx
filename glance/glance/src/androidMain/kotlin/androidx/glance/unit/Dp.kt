/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.unit

import kotlin.math.max
import kotlin.math.min

/**
 * Dimension value representing device-independent pixels (dp). Component APIs specify their
 * dimensions such as line thickness in DP with Dp objects. Dp are normally defined using [dp],
 * which can be applied to [Int], [Double], and [Float].
 *     val leftMargin = 10.dp
 *     val rightMargin = 10f.dp
 *     val topMargin = 20.0.dp
 *     val bottomMargin = 10.dp
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
inline class Dp(val value: Float) : Comparable<Dp> {
    /**
     * Add two [Dp]s together.
     */
    operator fun plus(other: Dp) =
        Dp(value = this.value + other.value)

    /**
     * Subtract a Dp from another one.
     */
    operator fun minus(other: Dp) =
        Dp(value = this.value - other.value)

    /**
     * This is the same as multiplying the Dp by -1.0.
     */
    operator fun unaryMinus() = Dp(-value)

    /**
     * Divide a Dp by a scalar.
     */
    operator fun div(other: Float): Dp =
        Dp(value = value / other)

    operator fun div(other: Int): Dp =
        Dp(value = value / other)

    /**
     * Divide by another Dp to get a scalar.
     */
    operator fun div(other: Dp): Float = value / other.value

    /**
     * Multiply a Dp by a scalar.
     */
    operator fun times(other: Float): Dp =
        Dp(value = value * other)

    operator fun times(other: Int): Dp =
        Dp(value = value * other)

    /**
     * Support comparing Dimensions with comparison operators.
     */
    override /* TODO: inline */ operator fun compareTo(other: Dp) = value.compareTo(other.value)

    override fun toString() = if (isUnspecified) "Dp.Unspecified" else "$value.dp"

    companion object {
        /**
         * Constant that means unspecified Dp
         */
        val Unspecified = Dp(value = Float.NaN)
    }
}

/**
 * `false` when this is [Dp.Unspecified].
 */
inline val Dp.isSpecified: Boolean
    get() = !value.isNaN()

/**
 * `true` when this is [Dp.Unspecified].
 */
inline val Dp.isUnspecified: Boolean
    get() = value.isNaN()

/**
 * Create a [Dp] using an [Int]:
 *     val left = 10
 *     val x = left.dp
 *     // -- or --
 *     val y = 10.dp
 */
inline val Int.dp: Dp get() = Dp(value = this.toFloat())

/**
 * Create a [Dp] using a [Double]:
 *     val left = 10.0
 *     val x = left.dp
 *     // -- or --
 *     val y = 10.0.dp
 */
inline val Double.dp: Dp get() = Dp(value = this.toFloat())

/**
 * Create a [Dp] using a [Float]:
 *     val left = 10f
 *     val x = left.dp
 *     // -- or --
 *     val y = 10f.dp
 */
inline val Float.dp: Dp get() = Dp(value = this)

operator fun Float.times(other: Dp) =
    Dp(this * other.value)

operator fun Double.times(other: Dp) =
    Dp(this.toFloat() * other.value)

operator fun Int.times(other: Dp) =
    Dp(this * other.value)

fun min(a: Dp, b: Dp): Dp = Dp(value = min(a.value, b.value))

fun max(a: Dp, b: Dp): Dp = Dp(value = max(a.value, b.value))
