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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin

internal abstract class VelocityPathFinder {
    abstract fun calculateOffsetForTime(time: Long): Offset

    companion object {

        /**
         * Calculates a duration for a gesture such that a valid swipe can be generated for that
         * gesture that starts at [start] and ends at [end] with the given [endVelocity].
         *
         * In most cases the duration is going to be 200ms, except for a few edge cases where it
         * would not be possible to generate a valid swipe for the given requirements. If no
         * duration exist for which it would be possible to generate a valid swipe that meets the
         * requirements, and [IllegalArgumentException] is thrown.
         */
        fun calculateDefaultDuration(start: Offset, end: Offset, endVelocity: Float): Long {
            require(endVelocity >= 0f) { "Velocity cannot be $endVelocity, it must be positive" }
            require(start != end || endVelocity == 0f) {
                "When start == end; velocity cannot be $endVelocity, it must be 0f"
            }

            val distance = (end - start).getDistance()

            // Times 1000 because velocity is in px/s and our time unit is ms.
            val defaultDuration = 2 / endVelocity * distance * 1000

            // Referring to the graphs mentioned in the kdoc of [calculateOffsetForTime];
            // d = 0: start == end and velocity > 0           not possible (already checked for)
            // d = NaN: start == end and velocity == 0        T=200 (scenario 1)
            // d = Infinity: start != end and velocity == 0   T=200 (scenario 3)
            // d > 200: start != end and velocity > 0         T=200 (scenario 3)
            // d > HorizonMs: start != end and velocity > 0   T=200 (scenario 2)
            // d <= HorizonMs: start != end and velocity > 0  T=d   (scenario 3)

            if (defaultDuration.isNaN() || defaultDuration > HorizonMilliseconds) {
                return DefaultDurationMilliseconds
            }

            // d <= HorizonMilliseconds, so we have to pick `T = d`. But, when d is very small,
            // this leads to a duration too short to even get a velocity.
            // Check and throw if this is the case.
            val minimumDuration = ceil(2.5f * eventPeriodMillis).roundToLong()
            require(floor(defaultDuration).roundToLong() >= minimumDuration) {
                // Nope. This won't work.
                val suggestedVelocity = (2f / minimumDuration) * distance * 1000
                val suggestedDistance = .5f * minimumDuration * endVelocity / 1000
                "Unable to generate a swipe gesture between $start and $end that ends with " +
                    "velocity of $endVelocity px/s, without going outside of the range " +
                    "[start..end]. " +
                    "Suggested fixes: " +
                    "1. set velocity to $suggestedVelocity px/s or lower; or " +
                    "2. increase the distance between the start and end to " +
                    "$suggestedDistance or higher"
            }
            return floor(defaultDuration).roundToLong()
        }
    }
}

internal expect fun VelocityPathFinder(
    startPosition: Offset,
    endPosition: Offset,
    endVelocity: Float,
    durationMillis: Long,
): VelocityPathFinder

internal class ImpulseVelocityPathFinder(
    private val startPosition: Offset,
    private val endPosition: Offset,
    private val endVelocity: Float,
    private val durationMillis: Long,
) : VelocityPathFinder() {

    private val vx: Double
    private val vy: Double

    init {
        // Decompose v into its x and y components
        val delta = endPosition - startPosition
        val theta = atan2(delta.y.toDouble(), delta.x.toDouble())
        // Note: it would be more precise to do `theta = atan2(-y, x)`, because atan2 expects a
        // coordinate system where positive y goes up and in our coordinate system positive y goes
        // down. However, in that case we would also have to inverse `vy` to convert the velocity
        // back to our own coordinate system. But then it's just a double negation, so we can skip
        // both conversions entirely.

        // VelocityTracker internally calculates px/ms, not px/s
        vx = cos(theta) * endVelocity / 1000
        vy = sin(theta) * endVelocity / 1000
    }

    override fun calculateOffsetForTime(time: Long): Offset {
        val x = calculateOffsetForTime(vx, startPosition.x, endPosition.x, time)
        val y = calculateOffsetForTime(vy, startPosition.y, endPosition.y, time)
        return Offset(x, y)
    }

    private fun calculateOffsetForTime(
        velocity: Double,
        start: Float,
        end: Float,
        time: Long,
    ): Float {
        val T = durationMillis

        if (start == end) {
            require(abs(velocity) < 0.1) { "Can't have matching positions, but nonzero velocity" }
            return start
        }

        // Special handling for small velocity. We multiply by velocity to find the position rather
        // than divide by velocity to calculate time.
        if (abs(velocity) < 0.1) {
            // Same as the condition below. Must start the movement earlier than HorizonMilliseconds
            val suggestedDuration = HorizonMilliseconds
            require(T >= suggestedDuration) {
                "Unable to generate a swipe gesture between $start and $end with " +
                    "duration $durationMillis that ends with velocity of $velocity px/s, without " +
                    "going outside of the range [start..end]. " +
                    "Suggested fixes: " +
                    "1. set duration to $suggestedDuration or higher; "
            }
            val positionAtHorizonStart = (end - velocity * HorizonMilliseconds).toFloat()
            return when {
                // t == 0 condition is needed in case T <= HorizonMilliseconds
                time == 0L -> start
                time < (T - HorizonMilliseconds) ->
                    start + (positionAtHorizonStart - start) / (T - HorizonMilliseconds) * time
                else -> end - (T - time) * velocity.toFloat()
            }
        }

        if (T <= HorizonMilliseconds) {
            val result = searchPath(start, end, T, velocity.toFloat() * 1000)
            if (result != null) {
                val (d, x) = result
                return computePosition(start, end, T, d, x, time)
            }
        }

        if (T > HorizonMilliseconds) {
            // Best case: just need to gradually move up to the correct place
            val xHorizon = (end - HorizonMilliseconds * velocity).toFloat()
            if (min(start, end) < xHorizon && xHorizon < max(start, end)) {
                // Then it's within the start and end positions, so we are OK
                return when {
                    time < T - HorizonMilliseconds ->
                        start + (xHorizon - start) / (T - HorizonMilliseconds) * time
                    else ->
                        xHorizon +
                            (end - xHorizon) / (HorizonMilliseconds) *
                                (time - (T - HorizonMilliseconds))
                }
            }
            // Move the 'start' coordinate to a time of 'T-HorizonMilliseconds'. Therefore, we will
            // have 3 lines - flat line until 'T-HorizonMilliseconds', and then two lines as in
            // previous solutions.
            val result = searchPath(start, end, HorizonMilliseconds, velocity.toFloat() * 1000)
            if (result != null) {
                val (d, x) = result
                return when {
                    time < T - HorizonMilliseconds -> start
                    else ->
                        computePosition(
                            start,
                            end,
                            HorizonMilliseconds,
                            d,
                            x,
                            time - (T - HorizonMilliseconds),
                        )
                }
            }
        }

        throw IllegalArgumentException(
            "Could not find a path for start=$start end=$end velocity=$velocity T=$T." +
                "Try setting velocity=${(end - start) / T} or T=${(end - start) / velocity}." +
                "Typically, T should be $HorizonMilliseconds ms or longer."
        )
    }

    /**
     * Compute the position at time t when the path is piecewise defined as 2 lines: one from (0,
     * start) -> (d, x) and another from (d, x) -> (T, end)
     *
     * @param start Position where the curve starts.
     * @param end Position where the curve ends.
     * @param T Time at end position.
     * @param d Time at the end of the first piecewise line (and start of the second line).
     * @param x Position at the end of the first piecewise line (and start of the second line).
     * @param t The time in which we're interested to calculate this position at, given a time based
     *   curve, one of the point in this curve.
     */
    private fun computePosition(
        start: Float,
        end: Float,
        T: Long,
        d: Long,
        x: Float,
        t: Long,
    ): Float {
        require(t in 0L..T) { "You must provide 0 <= t <= $T, but received t=$t instead" }
        if (t < d) {
            return start + (x - start) / d * t
        }
        return end - (end - x) / (T - d) * (T - t)
    }

    /** Inject a 2-line path into VelocityTracker and find the resulting velocity. */
    private fun calculateVelocityFullPath(
        start: Float,
        end: Float,
        T: Long,
        d: Long,
        x: Float,
    ): Float {
        val vt = VelocityTracker()

        vt.addPosition(0, Offset(start, 0f))
        var t = eventPeriodMillis
        while (t < T) {
            val position = computePosition(start, end, T, d, x, t)
            vt.addPosition(t, Offset(position, 0f))
            t += eventPeriodMillis
        }
        vt.addPosition(T, Offset(end, 0f))

        return vt.calculateVelocity().x
    }

    private data class FittingResult(val d: Long, val x: Float)

    /**
     * Numerically find a path that best provides a motion that results in the velocity of
     * targetVelocity.
     */
    private fun searchPath(
        start: Float,
        end: Float,
        T: Long,
        targetVelocity: Float,
    ): FittingResult? {
        val TOLERANCE = 1f
        val step = (max(end, start) - min(end, start)) / 1000f
        for (d in 1 until T) {
            var x = min(start, end)
            while (x < max(start, end)) {
                val velocity = calculateVelocityFullPath(start, end, T, d, x)
                val diff = abs(targetVelocity - velocity)
                if (diff < TOLERANCE) {
                    return FittingResult(d, x)
                }
                x += step
            }
        }
        return null
    }
}

internal class LsqVelocityPathFinder(
    private val startPosition: Offset,
    private val endPosition: Offset,
    private val endVelocity: Float,
    private val durationMillis: Long,
) : VelocityPathFinder() {
    private val vx: Double
    private val vy: Double

    init {
        // Decompose v into its x and y components
        val delta = endPosition - startPosition
        val theta = atan2(delta.y.toDouble(), delta.x.toDouble())
        // Note: it would be more precise to do `theta = atan2(-y, x)`, because atan2 expects a
        // coordinate system where positive y goes up and in our coordinate system positive y goes
        // down. However, in that case we would also have to inverse `vy` to convert the velocity
        // back to our own coordinate system. But then it's just a double negation, so we can skip
        // both conversions entirely.

        // VelocityTracker internally calculates px/ms, not px/s
        vx = cos(theta) * endVelocity / 1000
        vy = sin(theta) * endVelocity / 1000
    }

    override fun calculateOffsetForTime(time: Long): Offset {
        val x = calculateOffsetForTime(vx, startPosition.x, endPosition.x, time)
        val y = calculateOffsetForTime(vy, startPosition.y, endPosition.y, time)
        return Offset(x, y)
    }

    /**
     * Generates a function f(t) where `f(0) = start`, `f(T) = end`, and the polynomial fit over the
     * last 100ms is of the form `f(t) = a*(t-T)^2 + b*(t-T) + c`, with `start = [startPosition]`,
     * `end = [endPosition])`, `b = [velocity]` and `T = [durationMillis]`. Note that this implies
     * `f'(T) = [velocity]`.
     *
     * There are three different shapes that the function can take: a flat line, a flat line
     * followed by a parabola that starts with `f'(t) = 0`, or a parabola that starts with `f'(0) >
     * 0`.
     * 1. Flat line: This happens when start == end and requires that the requested velocity is 0.
     * 2. Flat line followed by a parabola: This happens when there is a parabola that satisfies
     *    `f(t_d) = start`, `f'(t_d) = 0`, `f'(T) = velocity` and `t_d >= 0`. The gesture will wait
     *    at the start location until t_d and then follow that parabola till `f(T) = end`.
     * 3. Parabola that starts with `f'(0) > 0`: If there is a parabola that satisfies `f(t_d) =
     *    start`, `f'(t_d) = 0`, `f'(T) = velocity`, but `t_d < 0`; or if `velocity = 0` (in which
     *    case the previously mentioned parabola doesn't exist); we can't follow that parabola
     *    because we'd have to start following it in the past (`t_d < 0`). Instead, it can be shown
     *    that in this case we can always create a parabola that satisfies `f(0) = start`, `f(T) =
     *    end` and `f'(T) = velocity`. This parabola will have `f'(0) > 0`.
     *
     * In the calculations below, instead of calculating t_d, we calculate `d = T - t_d`, and
     * immediately cap it to T.
     *
     * @param velocity The desired velocity in the x or y direction at the end position
     */
    private fun calculateOffsetForTime(
        velocity: Double,
        start: Float,
        end: Float,
        time: Long,
    ): Float {
        val T = durationMillis
        // `d = T - t_d` in scenario 2 (see documentation above)
        // `d = T` in scenario 1 and 3 (see documentation above)
        val d =
            if (start == end) {
                T.toDouble()
            } else {
                min(T.toDouble(), 2 / velocity * (end - start))
            }
        val a = (start + velocity * d - end) / (d * d)

        require(d >= min(T, HorizonMilliseconds)) {
            val requestedDistance = (endPosition - startPosition).getDistance()
            // 1) Decrease duration to d
            val suggestedDuration = d
            // 2) Decrease velocity to 2/100 * (end - start) -> should work for vectors too
            val suggestedVelocity = (2f / min(T, HorizonMilliseconds)) * requestedDistance * 1000
            // 3) Increase distance to 100/2 * velocity
            val suggestedDistance = (min(T, HorizonMilliseconds) / 2f) * endVelocity / 1000
            "Unable to generate a swipe gesture between $startPosition and $endPosition with " +
                "duration $durationMillis that ends with velocity of $endVelocity px/s, without " +
                "going outside of the range [start..end]. " +
                "Suggested fixes: " +
                "1. set duration to $suggestedDuration or lower; " +
                "2. set velocity to $suggestedVelocity px/s or lower; or " +
                "3. increase the distance between the start and end to $suggestedDistance or " +
                "higher"
        }

        val threshold = T - d
        return when {
            time < threshold -> start
            // `f(t) = a*(t-T)^2 + b*(t-T) + c`
            else -> a * (time - T) * (time - T) + velocity * (time - T) + end
        }.toFloat()
    }
}

// TODO(b/204895043): Taken from VelocityTrackerKt.HorizonMilliseconds. Must stay the same.
private const val HorizonMilliseconds: Long = 100
private const val DefaultDurationMilliseconds: Long = 200
