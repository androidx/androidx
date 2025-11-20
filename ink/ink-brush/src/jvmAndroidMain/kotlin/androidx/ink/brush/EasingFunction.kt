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
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.util.Collections.unmodifiableList
import kotlin.jvm.JvmField

/**
 * An easing function always passes through the (x, y) points (0, 0) and (1, 1). It typically acts
 * to map x values in the [0, 1] interval to y values in [0, 1] by either one of the predefined or
 * one of the parameterized curve types below. Depending on the type of curve, input and output
 * values outside [0, 1] are possible.
 */
@ExperimentalInkCustomBrushApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi

// NotCloseable: Finalize is only used to free the native peer.
@Suppress("NotCloseable")
public abstract class EasingFunction private constructor(internal val nativePointer: Long) {

    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which
        // in Kotlin is always before any non-default field initialization has been done by a
        // derived
        // class constructor.
        if (nativePointer == 0L) return
        EasingFunctionNative.free(nativePointer)
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long): EasingFunction =
            when (EasingFunctionNative.getParametersType(unownedNativePointer)) {
                0 -> Predefined(unownedNativePointer)
                1 -> CubicBezier(unownedNativePointer)
                2 -> Linear(unownedNativePointer)
                3 -> Steps(unownedNativePointer)
                else -> throw IllegalArgumentException("Invalid easing function type")
            }
    }

    public class Predefined internal constructor(nativePointer: Long) :
        EasingFunction(nativePointer) {

        private constructor(value: Int) : this(EasingFunctionNative.createPredefined(value))

        internal val value: Int
            get() = EasingFunctionNative.getPredefinedValueInt(nativePointer)

        internal fun toSimpleString(): String =
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
     * resulting curve is a function with respect to x and follows the
     * [CSS specification](https://www.w3.org/TR/css-easing-1/#cubic-bezier-easing-functions)
     *
     * Valid parameters must have all finite values, and [x1] and [x2] must be in the interval
     * [0, 1].
     *
     * Input x values that are outside the interval [0, 1] will be clamped, but output values will
     * not. This is somewhat different from the w3c defined cubic Bezier that allows extrapolated
     * values outside x in [0, 1] by following end-point tangents.
     */
    public class CubicBezier internal constructor(nativePointer: Long) :
        EasingFunction(nativePointer) {

        /**
         * Creates a new [CubicBezier] easing function.
         *
         * @param x1 The x-coordinate of the first control point. Must be in the range [0, 1].
         * @param y1 The y-coordinate of the first control point.
         * @param x2 The x-coordinate of the second control point. Must be in the range [0, 1].
         * @param y2 The y-coordinate of the second control point.
         */
        public constructor(
            @FloatRange(from = 0.0, to = 1.0) x1: Float,
            y1: Float,
            @FloatRange(from = 0.0, to = 1.0) x2: Float,
            y2: Float,
        ) : this(EasingFunctionNative.createCubicBezier(x1, y1, x2, y2))

        /** The x-coordinate of the first control point. Must be in the range [0, 1]. */
        @get:FloatRange(from = 0.0, to = 1.0)
        public val x1: Float
            get() = EasingFunctionNative.getCubicBezierX1(nativePointer)

        /** The y-coordinate of the first control point. */
        public val y1: Float
            get() = EasingFunctionNative.getCubicBezierY1(nativePointer)

        /** The x-coordinate of the second control point. Must be in the range [0, 1]. */
        @get:FloatRange(from = 0.0, to = 1.0)
        public val x2: Float
            get() = EasingFunctionNative.getCubicBezierX2(nativePointer)

        /** The y-coordinate of the second control point. */
        public val y2: Float
            get() = EasingFunctionNative.getCubicBezierY2(nativePointer)

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
    public class Linear internal constructor(nativePointer: Long) : EasingFunction(nativePointer) {

        /**
         * Creates a new [Linear] easing function.
         *
         * @param points The points that define the piecewise-linear function.
         */
        public constructor(
            points: List<ImmutableVec>
        ) : this(
            EasingFunctionNative.createLinear(
                FloatArray(points.size * 2) { index ->
                    if (index % 2 == 0) {
                        points[index / 2].x
                    } else {
                        points[index / 2].y
                    }
                }
            )
        )

        /** The points that define the piecewise-linear function. */
        public val points: List<ImmutableVec> =
            unmodifiableList(
                List<ImmutableVec>(EasingFunctionNative.getLinearNumPoints(nativePointer)) { index
                    ->
                    ImmutableVec(
                        EasingFunctionNative.getLinearPointX(nativePointer, index),
                        EasingFunctionNative.getLinearPointY(nativePointer, index),
                    )
                }
            )

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
     * The behavior and naming follows the CSS steps() specification at
     * [CSS Easing Functions](https://www.w3.org/TR/css-easing-1/#step-easing-functions)
     */
    public class Steps internal constructor(nativePointer: Long) : EasingFunction(nativePointer) {

        /**
         * Creates a new [Steps] easing function.
         *
         * @param stepCount The number of steps. Must always be greater than 0, and must be greater
         *   than 1 if [stepPosition] is [StepPosition.JUMP_NONE].
         * @param stepPosition The behavior of the first and last steps.
         */
        public constructor(
            stepCount: Int,
            stepPosition: StepPosition,
        ) : this(EasingFunctionNative.createSteps(stepCount, stepPosition.value))

        /**
         * The number of steps. Must always be greater than 0, and must be greater than 1 if
         * [stepPosition] is [StepPosition.JUMP_NONE].
         */
        public val stepCount: Int
            get() = EasingFunctionNative.getStepsCount(nativePointer)

        /** The behavior of the first and last steps. */
        public val stepPosition: StepPosition
            get() = EasingFunctionNative.getStepsPosition(nativePointer)

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
    public class StepPosition internal constructor(@JvmField internal val value: Int) {

        internal fun toSimpleString(): String =
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

@OptIn(ExperimentalInkCustomBrushApi::class)
@UsedByNative
private object EasingFunctionNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun createPredefined(value: Int): Long

    @UsedByNative external fun createCubicBezier(x1: Float, y1: Float, x2: Float, y2: Float): Long

    @UsedByNative external fun createLinear(points: FloatArray): Long

    @UsedByNative external fun createSteps(stepCount: Int, stepPosition: Int): Long

    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative external fun getParametersType(nativePointer: Long): Int

    // Predefined easing function accessors:

    @UsedByNative external fun getPredefinedValueInt(nativePointer: Long): Int

    // Cubic Bezier easing function accessors:

    @UsedByNative external fun getCubicBezierX1(nativePointer: Long): Float

    @UsedByNative external fun getCubicBezierY1(nativePointer: Long): Float

    @UsedByNative external fun getCubicBezierX2(nativePointer: Long): Float

    @UsedByNative external fun getCubicBezierY2(nativePointer: Long): Float

    // Linear easing function accessors:

    @UsedByNative external fun getLinearNumPoints(nativePointer: Long): Int

    @UsedByNative external fun getLinearPointX(nativePointer: Long, index: Int): Float

    @UsedByNative external fun getLinearPointY(nativePointer: Long, index: Int): Float

    // Steps easing function accessors:

    @UsedByNative external fun getStepsCount(nativePointer: Long): Int

    fun getStepsPosition(nativePointer: Long): EasingFunction.StepPosition =
        EasingFunction.StepPosition(getStepsPositionInt(nativePointer))

    @UsedByNative private external fun getStepsPositionInt(nativePointer: Long): Int
}
