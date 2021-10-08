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
 * Dimension value representing scale-independent pixels (sp). Component APIs specify their
 * text size in SP with Sp objects. Sp are normally defined using [sp], which can be applied to
 * [Int], [Double], and [Float].
 *     val titleSize = 10.sp
 *     val rightMargin = 10f.sp
 *     val topMargin = 20.0.sp
 *     val bottomMargin = 10.sp
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
public inline class Sp(public val value: Float) : Comparable<Sp> {
    /**
     * Add two [Sp]s together.
     */
    public operator fun plus(other: Sp): Sp =
        Sp(value = this.value + other.value)

    /**
     * Subtract a Sp from another one.
     */
    public operator fun minus(other: Sp): Sp =
        Sp(value = this.value - other.value)

    /**
     * This is the same as multiplying the Sp by -1.0.
     */
    public operator fun unaryMinus(): Sp = Sp(-value)

    /**
     * Divide a Sp by a scalar.
     */
    public operator fun div(other: Float): Sp =
        Sp(value = value / other)

    public operator fun div(other: Int): Sp =
        Sp(value = value / other)

    /**
     * Divide by another Sp to get a scalar.
     */
    public operator fun div(other: Sp): Float = value / other.value

    /**
     * Multiply a Sp by a scalar.
     */
    public operator fun times(other: Float): Sp =
        Sp(value = value * other)

    public operator fun times(other: Int): Sp =
        Sp(value = value * other)

    /**
     * Support comparing Dimensions with comparison operators.
     */
    override /* TODO: inline */ operator fun compareTo(other: Sp): Int =
        value.compareTo(other.value)

    override fun toString(): String = "$value.dp"
}

/**
 * Create a [Sp] using an [Int]:
 *     val left = 10
 *     val x = left.dp
 *     // -- or --
 *     val y = 10.dp
 */
public inline val Int.sp: Sp get() = Sp(value = this.toFloat())

/**
 * Create a [Sp] using a [Double]:
 *     val left = 10.0
 *     val x = left.dp
 *     // -- or --
 *     val y = 10.0.dp
 */
public inline val Double.sp: Sp get() = Sp(value = this.toFloat())

/**
 * Create a [Sp] using a [Float]:
 *     val left = 10f
 *     val x = left.dp
 *     // -- or --
 *     val y = 10f.dp
 */
public inline val Float.sp: Sp get() = Sp(value = this)

public operator fun Float.times(other: Sp): Sp =
    Sp(this * other.value)

public operator fun Double.times(other: Sp): Sp =
    Sp(this.toFloat() * other.value)

public operator fun Int.times(other: Sp): Sp =
    Sp(this * other.value)

public fun min(a: Sp, b: Sp): Sp = Sp(value = min(a.value, b.value))

public fun max(a: Sp, b: Sp): Sp = Sp(value = max(a.value, b.value))
