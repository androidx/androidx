/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.velocity_tracker

import androidx.ui.engine.geometry.Offset
import androidx.ui.toStringAsFixed

/** A velocity in two dimensions. */
data class Velocity(
    /** The number of pixels per second of velocity in the x and y directions. */
    val pixelsPerSecond: Offset
) {

    /** Return the negation of a velocity. */
    operator fun unaryMinus() = Velocity(pixelsPerSecond = -pixelsPerSecond)

    /** Return the difference of two velocities. */
    operator fun minus(other: Velocity) =
        Velocity(pixelsPerSecond = pixelsPerSecond - other.pixelsPerSecond)

    /** Return the sum of two velocities. */
    operator fun plus(other: Velocity) =
        Velocity(pixelsPerSecond = pixelsPerSecond + other.pixelsPerSecond)

    /**
     * Return a velocity whose magnitude has been clamped to [minValue]
     * and [maxValue].
     *
     * If the magnitude of this Velocity is less than minValue then return a new
     * Velocity with the same direction and with magnitude [minValue]. Similarly,
     * if the magnitude of this Velocity is greater than maxValue then return a
     * new Velocity with the same direction and magnitude [maxValue].
     *
     * If the magnitude of this Velocity is within the specified bounds then
     * just return this.
     */
    fun clampMagnitude(minValue: Double, maxValue: Double): Velocity {
        assert(minValue >= 0.0)
        assert((maxValue >= 0.0) && (maxValue >= minValue))
        val valueSquared: Double = pixelsPerSecond.getDistanceSquared()
        if (valueSquared > maxValue * maxValue) {
            return Velocity(
                pixelsPerSecond = (pixelsPerSecond / pixelsPerSecond.getDistance()) * maxValue
            )
        }
        if ((valueSquared < minValue * minValue)) {
            return Velocity(
                pixelsPerSecond = (pixelsPerSecond / pixelsPerSecond.getDistance()) * minValue
            )
        }
        return this
    }

    override fun toString() = "Velocity(${pixelsPerSecond.dx.toStringAsFixed(1)}, " +
            "${pixelsPerSecond.dy.toStringAsFixed(1)})"

    companion object {
        /** A velocity that isn't moving at all. */
        val zero: Velocity = Velocity(pixelsPerSecond = Offset.zero)
    }
}
