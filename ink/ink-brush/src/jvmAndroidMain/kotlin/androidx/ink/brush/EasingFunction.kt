/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.brush

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.ink.geometry.ImmutableVec
import java.util.Collections.unmodifiableList
import kotlin.jvm.JvmField

/**
 * An easing function always passes through the (x, y) points (0, 0) and (1, 1). It typically acts
 * to map x values in the [0, 1] interval to y values in [0, 1] by either one of the predefined or
 * one of the parameterized curve types below. Depending on the type of curve, input and output
 * values outside [0, 1] are possible.
 */
@ExperimentalInkCustomBrushApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public abstract class EasingFunction private constructor() {

    public class Predefined private constructor(@JvmField internal val value: Int) :
        EasingFunction() {

        public fun toSimpleString(): String =
            when (value) {
                0 -> "LINEAR"
                1 -> "EASE"
                2 -> "EASE_IN"
                3 -> "EASE_OUT"
                4 -> "EASE_IN_OUT"
                5 -> "STEP_START"
                6 -> "STEP_END"
                else -> "INVALID"
            }

        override fun toString(): String = PREFIX + toSimpleString()

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is Predefined) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {
            /** The linear identity function: accepts and returns values outside [0, 1]. */
            @JvmField public val LINEAR: Predefined = Predefined(0)

            /**
             * Predefined cubic Bezier function. See
             * [ease](https://www.w3.org/TR/css-easing-1/#cubic-bezier-easing-functions)
             * and @see [CubicBezier] about input values outside [0, 1])
             */
            @JvmField public val EASE: Predefined = Predefined(1)

            /**
             * Predefined cubic Bezier function. See
             * [ease-in](https://www.w3.org/TR/css-easing-1/#cubic-bezier-easing-functions)
             * and @see [CubicBezier] about input values outside [0, 1])
             */
            @JvmField public val EASE_IN: Predefined = Predefined(2)

            /**
             * Predefined cubic Bezier function. See
             * [ease-out](https://www.w3.org/TR/css-easing-1/#cubic-bezier-easing-functions)
             * and @see [CubicBezier] about input values outside [0, 1])
             */
            @JvmField public val EASE_OUT: Predefined = Predefined(3)

            /**
             * Predefined cubic Bezier function. See
             * [ease-in-out](https://www.w3.org/TR/css-easing-1/#cubic-bezier-easing-functions)
             * and @see [CubicBezier] about input values outside [0, 1])
             */
            @JvmField public val EASE_IN_OUT: Predefined = Predefined(4)

            /**
             * Predefined step function with a jump-start at input progress value of 0. See
             * [step start](https://www.w3.org/TR/css-easing-1/#step-easing-functions)
             */
            @JvmField public val STEP_START: Predefined = Predefined(5)

            /**
             * Predefined step function with a jump-end at input progress value of 1. See
             * [step end](https://www.w3.org/TR/css-easing-1/#step-easing-functions)
             */
            @JvmField public val STEP_END: Predefined = Predefined(6)

            private const val PREFIX = "EasingFunction.Predefined."
        }
    }

    /**
     * Parameters for a custom cubic Bezier easing function.
     *
     * A cubic Bezier is generally defined by four points, P0 - P3. In the case of the easing
     * function, P0 is defined to be the point (0, 0), and P3 is defined to be the point (1, 1). The
     * values of [x1] and [x2] are required to be in the range [0, 1]. This guarantees that the
     * resulting curve is a function with respect to x and follows the CSS cubic Bezier
     * specification:
     * [https://developer.mozilla.org/en-US/docs/Web/CSS/easing-function#cubic_b%C3%A9zier_easing_function](https://developer.mozilla.org/en-US/docs/Web/CSS/easing-function#cubic_b%C3%A9zier_easing_function)
     *
     * Valid parameters must have all finite values, and [x1] and [x2] must be in the interval
     * [0, 1].
     *
     * Input x values that are outside the interval [0, 1] will be clamped, but output values will
     * not. This is somewhat different from the w3c defined cubic Bezier that allows extrapolated
     * values outside x in [0, 1] by following end-point tangents.
     */
    public class CubicBezier(
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true)
        public val x1: Float,
        public val y1: Float,
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true)
        public val x2: Float,
        public val y2: Float,
    ) : EasingFunction() {
        init {
            require(x1.isFinite() && x2.isFinite() && y1.isFinite() && y2.isFinite()) {
                "All parameters must be finite. x1 = $x1, x2 = $x2, y1 = $y1, y2 = $y2"
            }
            require(x1 in 0.0..1.0) { "x1 = $x1 is required to be in the range [0, 1]" }
            require(x2 in 0.0..1.0) { "x2 = $x2 is required to be in the range [0, 1]" }
        }

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is CubicBezier) {
                return false
            }
            return x1 == other.x1 && x2 == other.x2 && y1 == other.y1 && y2 == other.y2
        }

        override fun hashCode(): Int {
            var result = x1.hashCode()
            result = 31 * result + x2.hashCode()
            result = 31 * result + y1.hashCode()
            result = 31 * result + y2.hashCode()
            return result
        }

        override fun toString(): String =
            "EasingFunction.CubicBezier(x1=$x1, y1=$y1, x2=$x2, y2=$y2)"

        // Declared to make extension functions available.
        public companion object
    }

    /**
     * Parameters for a custom piecewise-linear easing function.
     *
     * A piecewise-linear function is defined by a sequence of points; the value of the function at
     * an x-position equal to one of those points is equal to the y-position of that point, and the
     * value of the function at an x-position between two points is equal to the linear
     * interpolation between those points' y-positions. This easing function implicitly includes the
     * points (0, 0) and (1, 1), so the `points` field below need only include any points between
     * those. If [points] is empty, then this function is equivalent to the [Predefined.LINEAR]
     * identity function.
     *
     * To be valid, all y-positions must be finite, and all x-positions must be in the range [0, 1]
     * and must be monotonically non-decreasing. It is valid for multiple points to have the same
     * x-position, in order to create a discontinuity in the function; in that case, the value of
     * the function at exactly that x-position is equal to the y-position of the last of these
     * points.
     *
     * If the input x-value is outside the interval [0, 1], the output will be extrapolated from the
     * first/last line segment.
     */
    public class Linear(
        // The [points] val below is a defensive copy of this parameter.
        points: List<ImmutableVec>
    ) : EasingFunction() {
        public val points: List<ImmutableVec> = unmodifiableList(points.toList())

        init {
            for (point: ImmutableVec in points) {
                require(point.x.isFinite() && point.y.isFinite()) {
                    "All points must be finite. Got $point"
                }
                require(point.x in 0.0..1.0) {
                    "point.x is required to be in the range [0, 1]. Got $point"
                }
            }
            for ((a, b) in points.zipWithNext()) {
                require(a.x <= b.x) { "Points must be sorted by x-value. Got $a before $b" }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is Linear) {
                return false
            }
            return points == other.points
        }

        override fun hashCode(): Int {
            return points.hashCode()
        }

        override fun toString(): String = "EasingFunction.Linear(${points})"

        // Declared to make extension functions available.
        public companion object
    }

    /**
     * Parameters for a custom step easing function.
     *
     * A step function is defined by the number of equal-sized steps into which the
     * [0, 1) interval of input-x is split and the behavior at the extremes. When x < 0, the output will always be 0. When x >= 1, the output will always be 1. The output of the first and last steps is governed by the [StepPosition].
     *
     * @param stepCount The number of steps. Must always be greater than 0, and must be greater than
     *   1 if [stepPosition] is [StepPosition.JUMP_NONE].
     *
     * The behavior and naming follows the CSS steps() specification at
     * [https://www.w3.org/TR/css-easing-1/#step-easing-functions](https://www.w3.org/TR/css-easing-1/#step-easing-functions)
     */
    public class Steps(public val stepCount: Int, public val stepPosition: StepPosition) :
        EasingFunction() {
        init {
            require(stepCount > 0) { "stepCount = $stepCount is required to be greater than 0." }
            require(stepPosition != StepPosition.JUMP_NONE || stepCount > 1) {
                "stepCount = $stepCount is required to be greater than 1 if stepPosition = JUMP_NONE."
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is Steps) {
                return false
            }
            return stepCount == other.stepCount && stepPosition == other.stepPosition
        }

        override fun hashCode(): Int {
            var result = stepCount.hashCode()
            result = 31 * result + stepPosition.hashCode()
            return result
        }

        override fun toString(): String =
            "EasingFunction.Steps(stepCount=$stepCount, stepPosition=$stepPosition)"

        // Declared to make extension functions available.
        public companion object
    }

    /**
     * Setting to determine the desired output value of the first and last step of
     * [0, 1) for [EasingFunction.Steps].
     */
    public class StepPosition private constructor(@JvmField internal val value: Int) :
        EasingFunction() {

        public fun toSimpleString(): String =
            when (value) {
                0 -> "JUMP_END"
                1 -> "JUMP_START"
                2 -> "JUMP_BOTH"
                3 -> "JUMP_NONE"
                else -> "INVALID"
            }

        override fun toString(): String = PREFIX + toSimpleString()

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is StepPosition) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {
            /**
             * The step function "jumps" at the end of [0, 1): For x in [0, 1/step_count) => y = 0.
             * For x in [1 - 1/step_count, 1) => y = 1 - 1/step_count.
             */
            @JvmField public val JUMP_END: StepPosition = StepPosition(0)
            /**
             * The step function "jumps" at the start of [0, 1): For x in [0, 1/step_count) => y =
             * 1/step_count. For x in [1 - 1/step_count, 1) => y = 1.
             */
            @JvmField public val JUMP_START: StepPosition = StepPosition(1)
            /**
             * The step function "jumps" at both the start and the end: For x in [0, 1/step_count)
             * => y = 1/(step_count + 1). For x in [1 - 1/step_count, 1) => y = 1 - 1/(step_count +
             * 1).
             */
            @JvmField public val JUMP_BOTH: StepPosition = StepPosition(2)

            /**
             * The step function does not "jump" at either boundary: For x in [0, 1/step_count) => y
             * = 0. For x in [1 - 1/step_count, 1) => y = 1.
             */
            @JvmField public val JUMP_NONE: StepPosition = StepPosition(3)
            private const val PREFIX = "EasingFunction.StepPosition."
        }
    }
}
