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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.xr.compose.unit

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider
import com.android.extensions.xr.XrExtensions
import kotlin.math.roundToInt

/**
 * Represents a dimension value in meters within 3D space.
 *
 * This is the standard unit used by the system for representing size and distance in 3D
 * environments.
 */
@Immutable
@JvmInline
public value class Meter(public val value: Float) : Comparable<Meter> {

    /**
     * Adds another [Meter] value to this one.
     *
     * @param other the [Meter] value to add.
     * @return a new [Meter] representing the sum of the two values.
     */
    public inline operator fun plus(other: Meter): Meter = Meter(value + other.value)

    /**
     * Subtracts another [Meter] value from this one.
     *
     * @param other the [Meter] value to subtract.
     * @return a new [Meter] representing the difference between the two values.
     */
    public inline operator fun minus(other: Meter): Meter = Meter(value - other.value)

    /**
     * Multiplies this [Meter] value by an [Int] factor.
     *
     * @param other the integer factor to multiply by.
     * @return a new [Meter] representing the product.
     */
    public inline operator fun times(other: Int): Meter = Meter(value * other)

    /**
     * Multiplies this [Meter] value by a [Float] factor.
     *
     * @param other the float factor to multiply by.
     * @return a new [Meter] representing the product.
     */
    public inline operator fun times(other: Float): Meter = Meter(value * other)

    /**
     * Multiplies this [Meter] value by a [Double] factor.
     *
     * @param other the double factor to multiply by.
     * @return a new [Meter] representing the product.
     */
    public inline operator fun times(other: Double): Meter = Meter(value * other.toFloat())

    /**
     * Divides this [Meter] value by an [Int] factor.
     *
     * @param other the [Int] factor to divide by.
     * @return a new [Meter] representing the quotient.
     */
    public inline operator fun div(other: Int): Meter = Meter(value / other)

    /**
     * Divides this [Meter] value by a [Float] factor.
     *
     * @param other the [Float] factor to divide by.
     * @return a new [Meter] representing the quotient.
     */
    public inline operator fun div(other: Float): Meter = Meter(value / other)

    /**
     * Divides this [Meter] value by a [Double] factor.
     *
     * @param other the [Double] factor to divide by.
     * @return a new [Meter] representing the quotient.
     */
    public inline operator fun div(other: Double): Meter = Meter(value / other.toFloat())

    /**
     * Converts this [Meter] value to [Float] millimeters.
     *
     * @return the equivalent value in millimeters as a [Float].
     */
    public inline fun toMm(): Float = value * 1000f

    /**
     * Converts this [Meter] value to [Float] centimeters.
     *
     * @return the equivalent value in centimeters as a [Float].
     */
    public inline fun toCm(): Float = value * 100f

    /**
     * Converts this [Meter] value to [Float] meters.
     *
     * @return the equivalent value in meters as a [Float].
     */
    public inline fun toM(): Float = value

    /**
     * Converts this [Meter] value to an approximate number of pixels it contains.
     *
     * @return the approximate equivalent value in pixels as a [Float].
     */
    public inline fun toPx(density: Density): Float {
        with(density) {
            return toDp().toPx()
        }
    }

    /**
     * Converts this [Meter] value to the nearest [Int] number of pixels, taking into account
     * [density].
     *
     * @return the rounded equivalent value in pixels as an [Int].
     */
    public inline fun roundToPx(density: Density): Int {
        return toPx(density).roundToInt()
    }

    /**
     * Converts this [Meter] value to the [Dp] number of density-independent pixels it contains.
     *
     * @return the equivalent value in [Dp].
     */
    public inline fun toDp(): Dp {
        return (toM() * DP_PER_METER).dp
    }

    /**
     * Checks if this [Meter] value is specified (i.e., not NaN).
     *
     * @return `true` if the value is specified, `false` otherwise.
     */
    public inline val isSpecified: Boolean
        get() = !value.isNaN()

    /**
     * Checks if this [Meter] value is finite.
     *
     * @return `true` if the value is finite, `false` when [Meter.Infinity].
     */
    public inline val isFinite: Boolean
        get() = value != Float.POSITIVE_INFINITY

    /**
     * Compares this [Meter] value to [other].
     *
     * @param other The other [Meter] value to compare to.
     * @return a negative value if this [Meter] is less than [other], a positive value if it's
     *   greater, or 0 if they are equal.
     */
    override fun compareTo(other: Meter): Int {
        return value.compareTo(other.value)
    }

    public companion object {
        /**
         * If we can't look up the DPs per meter from the system, we will use this value. This value
         * was measured on the current Android XR device and will need to be updated if the device's
         * config changes.
         */
        private const val DP_PER_METER_FALLBACK: Float = 1151.856f

        /**
         * DPs per meter. The system's API is in pixels, but we can get the value we want be
         * specifying 1 dp == 1 pixel. This is a property with a getter, so it is re-evaluated on
         * each call, allowing unit test environments to override the value.
         */
        @PublishedApi
        internal val DP_PER_METER: Float
            get() = getXrExtensions()?.config?.defaultPixelsPerMeter(1.0f) ?: DP_PER_METER_FALLBACK

        /** Represents an infinite distance in meters. */
        public val Infinity: Meter = Meter(Float.POSITIVE_INFINITY)

        /** Represents an undefined or unrepresentable distance in meters. */
        public val NaN: Meter = Meter(Float.NaN)

        /**
         * Attempts to retrieve an instance of [XrExtensions].
         *
         * @return an instance of [XrExtensions] if available, or [null] otherwise.
         */
        private fun getXrExtensions(): XrExtensions? = XrExtensionsProvider.getXrExtensions()

        /**
         * Creates a [Meter] value from a given number of pixels.
         *
         * @param px the number of pixels.
         * @param density The pixel density of the display.
         * @return a [Meter] value representing the equivalent distance in meters.
         */
        public inline fun fromPixel(px: Float, density: Density): Meter {
            with(density) {
                // We do the conversion inline instead of calling Dp.toMeter(), which will check its
                // inputs.
                // We know if the input is an integer pixel, we won't have any exceptional Dp
                // values, e.g.,
                // Dp.Infinity.
                return Meter(px.toDp().value / DP_PER_METER)
            }
        }

        // Primitive conversion functions for creating [Meter] values from various units.

        /** Creates a [Meter] from the [Int] millimeter value. */
        public val Int.millimeters: Meter
            get() = Meter(this.toFloat() * 0.001f)

        /** Creates a [Meter] from the [Float] millimeter value. */
        public val Float.millimeters: Meter
            get() = Meter(this * 0.001f)

        /** Creates a [Meter] from the [Double] millimeter value. */
        public val Double.millimeters: Meter
            get() = Meter(this.toFloat() * 0.001f)

        /** Creates a [Meter] from the [Int] centimeter value. */
        public val Int.centimeters: Meter
            get() = Meter(this * 0.01f)

        /** Creates a [Meter] from the [Float] centimeter value. */
        public val Float.centimeters: Meter
            get() = Meter(this * 0.01f)

        /** Creates a [Meter] from the [Double] centimeter value. */
        public val Double.centimeters: Meter
            get() = Meter(this.toFloat() * 0.01f)

        /** Creates a [Meter] from the [Int] meter value. */
        public val Int.meters: Meter
            get() = Meter(this.toFloat())

        /** Creates a [Meter] from the [Float] meter value. */
        public val Float.meters: Meter
            get() = Meter(this)

        /** Creates a [Meter] from the [Double] meter value. */
        public val Double.meters: Meter
            get() = Meter(this.toFloat())
    }
}

/**
 * Converts a [Dp] value to [Meter].
 *
 * Handles unspecified and infinite [Dp] values gracefully.
 *
 * @return the equivalent value in meters, or [Meter.NaN] if the [Dp] is unspecified, or
 *   [Meter.Infinity] if the [Dp] is infinite.
 */
public inline fun Dp.toMeter(): Meter {
    if (!isSpecified) {
        return Meter.NaN
    }
    if (!isFinite) {
        return Meter.Infinity
    }
    return Meter(this.value / Meter.DP_PER_METER)
}

// Operator functions for performing arithmetic operations between numeric types and Meter

public inline operator fun Int.times(other: Meter): Meter = Meter(this * other.value)

public inline operator fun Float.times(other: Meter): Meter = Meter(this * other.value)

public inline operator fun Double.times(other: Meter): Meter = Meter(this.toFloat() * other.value)

public inline operator fun Int.div(other: Meter): Meter = Meter(this / other.value)

public inline operator fun Float.div(other: Meter): Meter = Meter(this / other.value)

public inline operator fun Double.div(other: Meter): Meter = Meter(this.toFloat() / other.value)
