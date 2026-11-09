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

package androidx.compose.animation

import androidx.compose.ui.unit.Density
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sign

/** Earth's gravity in SI units (m/s^2); used to compute deceleration based on friction. */
private const val GravityEarth = 9.80665f
private const val InchesPerMeter = 39.37f
private const val SecondsToMillis = 1_000L
private const val SecondsToNanos = 1_000_000_000L
private const val MillisToNanos = 1_000_000L

/**
 * The default rate of deceleration for a fling if not specified in the [FlingCalculator]
 * constructor.
 */
private val DecelerationRate = (ln(0.78) / ln(0.9)).toFloat()

/**
 * Compute the rate of deceleration based on pixel density, physical gravity and a
 * [coefficient of friction][friction].
 */
private fun computeDeceleration(friction: Float, density: Float): Float =
    GravityEarth * InchesPerMeter * density * 160f * friction

/**
 * Configuration for Android-feel flinging motion at the given density.
 *
 * @param friction scroll friction.
 * @param density density of the screen. Use LocalDensity to get current density in composition.
 */
internal class FlingCalculator(private val friction: Float, val density: Density) {

    /** A density-specific coefficient adjusted to physical values. */
    private val magicPhysicalCoefficient: Float = computeDeceleration(density)

    /** Computes the rate of deceleration in pixels based on the given [density]. */
    private fun computeDeceleration(density: Density) = computeDeceleration(0.84f, density.density)

    private fun getSplineDeceleration(velocity: Float): Double =
        AndroidFlingSpline.deceleration(velocity, friction * magicPhysicalCoefficient)

    /** Compute the duration in milliseconds of a fling with an initial velocity of [velocity] */
    fun flingDuration(velocity: Float): Long = flingDurationNanos(velocity) / MillisToNanos

    /** Compute the duration in nanoseconds of a fling with an initial velocity of [velocity] */
    fun flingDurationNanos(velocity: Float): Long {
        val l = getSplineDeceleration(velocity)
        val decelMinusOne = DecelerationRate - 1.0
        return (SecondsToNanos.toDouble() * exp(l / decelMinusOne)).toLong()
    }

    /** Compute the distance of a fling in units given an initial [velocity] of units/second */
    fun flingDistance(velocity: Float): Float {
        val l = getSplineDeceleration(velocity)
        val decelMinusOne = DecelerationRate - 1.0
        return (friction * magicPhysicalCoefficient * exp(DecelerationRate / decelMinusOne * l))
            .toFloat()
    }

    /** Compute all interesting information about a fling of initial velocity [velocity]. */
    fun flingInfo(velocity: Float): FlingInfo {
        val l = getSplineDeceleration(velocity)
        val decelMinusOne = DecelerationRate - 1.0
        return FlingInfo(
            initialVelocity = velocity,
            distance =
                (friction * magicPhysicalCoefficient * exp(DecelerationRate / decelMinusOne * l))
                    .toFloat(),
            duration = (SecondsToNanos.toDouble() * exp(l / decelMinusOne)).toLong(),
        )
    }

    /**
     * Info about a fling started with [initialVelocity]. The units of [initialVelocity] determine
     * the distance units of [distance]. [duration] is in nanoseconds.
     */
    data class FlingInfo(val initialVelocity: Float, val distance: Float, val duration: Long) {
        fun position(time: Long): Float = position(time, duration / MillisToNanos)

        fun velocity(time: Long): Float = velocity(time, duration / MillisToNanos, SecondsToMillis)

        fun positionFromNanos(timeNanos: Long): Float = position(timeNanos, duration)

        fun velocityFromNanos(timeNanos: Long): Float =
            velocity(timeNanos, duration, SecondsToNanos)

        private fun position(time: Long, duration: Long): Float {
            val splinePos = if (duration > 0) (time.toDouble() / duration).toFloat() else 1f
            return distance *
                sign(initialVelocity) *
                AndroidFlingSpline.flingPosition(splinePos).distanceCoefficient
        }

        private fun velocity(time: Long, duration: Long, secondsMultiplier: Long): Float {
            val splinePos = if (duration > 0) (time.toDouble() / duration).toFloat() else 1f
            return AndroidFlingSpline.flingPosition(splinePos).velocityCoefficient *
                sign(initialVelocity) *
                distance / duration * secondsMultiplier
        }
    }
}
