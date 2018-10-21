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

package androidx.ui.animation

import androidx.ui.clamp
import androidx.ui.runtimeType
import androidx.ui.toStringAsFixed
import kotlin.math.absoluteValue
import kotlin.math.truncate

/**
 * A mapPIng of the unit interval to the unit interval.
 *
 * A curve must map t=0.0 to 0.0 and t=1.0 to 1.0.
 *
 * See [Curves] for a collection of common animation curves.
 */
abstract class Curve {

    /**
     * Returns the value of the curve at point `t`.
     *
     * The value of `t` must be between 0.0 and 1.0, inclusive. Subclasses should
     * assert that this is true.
     *
     * A curve must map t=0.0 to 0.0 and t=1.0 to 1.0.
     */
    abstract fun transform(t: Double): Double

    /**
     * Returns a new curve that is the reversed inversion of this one.
     * This is often useful as the reverseCurve of an [Animation].
     *
     * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_bounce_in.png)
     * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_flipped.png)
     *
     * See also:
     *
     *  * [FlippedCurve], the class that is used to implement this getter.
     */
    val flipped: Curve get() = FlippedCurve(this)

    override fun toString(): String {
        return runtimeType().toString()
    }
}

/**
 * The identity map over the unit interval.
 *
 * See [Curves.linear] for an instance of this class.
 */
private class Linear : Curve() {
    override fun transform(t: Double) = t
}

/**
 * A sawtooth curve that repeats a given number of times over the unit interval.
 *
 * The curve rises linearly from 0.0 to 1.0 and then falls discontinuously back
 * to 0.0 each iteration.
 *
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_sawtooth.png)
 */
class SawTooth(
    /** The number of repetitions of the sawtooth pattern in the unit interval. */
    private val count: Int
) : Curve() {

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        if (t == 1.0)
            return 1.0
        val newT = t * count
        return newT - truncate(newT)
    }

    override fun toString() = "${runtimeType()}($count)"
}

/**
 * A curve that is 0.0 until [begin], then curved (according to [curve] from
 * 0.0 to 1.0 at [end], then 1.0.
 *
 * An [Interval] can be used to delay an animation. For example, a six second
 * animation that uses an [Interval] with its [begin] set to 0.5 and its [end]
 * set to 1.0 will essentially become a three-second animation that starts
 * three seconds later.
 *
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_interval.png)
 */
class Interval(
    /**
     * The largest value for which this interval is 0.0.
     *
     * From t=0.0 to t=`begin`, the interval's value is 0.0.
     */
    private val begin: Double,
    /**
     * The smallest value for which this interval is 1.0.
     *
     * From t=`end` to t=1.0, the interval's value is 1.0.
     */
    private val end: Double,
    /** The curve to apply between [begin] and [end]. */
    private val curve: Curve = Curves.linear
) : Curve() {

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        assert(begin >= 0.0)
        assert(begin <= 1.0)
        assert(end >= 0.0)
        assert(end <= 1.0)
        assert(end >= begin)
        if (t == 0.0 || t == 1.0)
            return t
        val newT = ((t - begin) / (end - begin)).clamp(0.0, 1.0)
        if (newT == 0.0 || newT == 1.0)
            return newT
        return curve.transform(newT)
    }

    override fun toString(): String {
        return if (curve !is Linear) {
            "${runtimeType()}($begin\u22EF$end)\u27A9$curve"
        } else {
            "${runtimeType()}($begin\u22EF$end)"
        }
    }
}

/**
 * A curve that is 0.0 until it hits the threshold, then it jumps to 1.0.
 *
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_threshold.png)
 */
class Threshold(
    /**
     * The value before which the curve is 0.0 and after which the curve is 1.0.
     *
     * When t is exactly [threshold], the curve has the value 1.0.
     */
    private val threshold: Double
) : Curve() {

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        assert(threshold >= 0.0)
        assert(threshold <= 1.0)
        if (t == 0.0 || t == 1.0)
            return t
        return if (t < threshold) 0.0 else 1.0
    }
}

private const val CUBIC_ERROR_BOUND: Double = 0.001

/**
 * A cubic polynomial mapPIng of the unit interval.
 *
 * The [Curves] class contains some commonly used cubic curves:
 *
 *  * [Curves.ease]
 *  * [Curves.easeIn]
 *  * [Curves.easeOut]
 *  * [Curves.easeInOut]
 *
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease_in.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease_out.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease_in_out.png)
 *
 * The [Cubic] class implements third-order Bézier curves.
 *
 * Rather than creating a new instance, consider using one of the common
 * cubic curves in [Curves].
 */
class Cubic(
    /**
     * The x coordinate of the first control point.
     *
     * The line through the point (0, 0) and the first control point is tangent
     * to the curve at the point (0, 0).
     */
    private val a: Double,
    /**
     * The y coordinate of the first control point.
     *
     * The line through the point (0, 0) and the first control point is tangent
     * to the curve at the point (0, 0).
     */
    private val b: Double,
    /**
     * The x coordinate of the second control point.
     *
     * The line through the point (1, 1) and the second control point is tangent
     * to the curve at the point (1, 1).
     */
    private val c: Double,
    /**
     * The y coordinate of the second control point.
     *
     * The line through the point (1, 1) and the second control point is tangent
     * to the curve at the point (1, 1).
     */
    private val d: Double
) : Curve() {

    private fun evaluateCubic(a: Double, b: Double, m: Double): Double {
        return 3 * a * (1 - m) * (1 - m) * m +
                3 * b * (1 - m) * /*    */ m * m +
                /*                      */ m * m * m
    }

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        var start = 0.0
        var end = 1.0
        while (true) {
            val midpoint = (start + end) / 2
            val estimate = evaluateCubic(a, c, midpoint)
            if ((t - estimate).absoluteValue < CUBIC_ERROR_BOUND)
                return evaluateCubic(b, d, midpoint)
            if (estimate < t)
                start = midpoint
            else
                end = midpoint
        }
    }

    override fun toString() = "${runtimeType()}(" +
            "${a.toStringAsFixed(2)}, " +
            "${b.toStringAsFixed(2)}, " +
            "${c.toStringAsFixed(2)}, " +
            "${d.toStringAsFixed(2)})"
}

/**
 * A curve that is the reversed inversion of its given curve.
 *
 * This curve evaluates the given curve in reverse (i.e., from 1.0 to 0.0 as t
 * increases from 0.0 to 1.0) and returns the inverse of the given curve's value
 * (i.e., 1.0 minus the given curve's value).
 *
 * This is the class used to implement the [flipped] getter on curves.
 *
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_bounce_in.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_flipped_curve.png)
 */
class FlippedCurve(
    /** The curve that is being flipped. */
    private val curve: Curve
) : Curve() {

    override fun transform(t: Double) = 1.0 - curve.transform(1.0 - t)

    override fun toString() = "${runtimeType()}($curve)"
}

/**
 * A curve where the rate of change starts out quickly and then decelerates; an
 * upside-down `f(t) = t²` parabola.
 *
 * This is equivalent to the Android `DecelerateInterpolator` class with a unit
 * factor (the default factor).
 *
 * See [Curves.decelerate] for an instance of this class.
 */
private class DecelerateCurve : Curve() {

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        // Intended to match the behavior of:
        // https://android.googlesource.com/platform/frameworks/base/+/master/core/
        // java/android/view/animation/DecelerateInterpolator.java
        // ...as of December 2016.
        val newT = 1.0 - t
        return 1.0 - newT * newT
    }
}

// BOUNCE CURVES

internal fun bounce(origT: Double): Double {
    var t = origT
    if (t < 1.0 / 2.75) {
        return 7.5625 * t * t
    } else if (t < 2 / 2.75) {
        t -= 1.5 / 2.75
        return 7.5625 * t * t + 0.75
    } else if (t < 2.5 / 2.75) {
        t -= 2.25 / 2.75
        return 7.5625 * t * t + 0.9375
    }
    t -= 2.625 / 2.75
    return 7.5625 * t * t + 0.984375
}

/**
 * An oscillating curve that grows in magnitude.
 *
 * See [Curves.bounceIn] for an instance of this class.
 */
private class BounceInCurve : Curve() {

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        return 1.0 - bounce(1.0 - t)
    }
}

/**
 * An oscillating curve that shrink in magnitude.
 *
 * See [Curves.bounceOut] for an instance of this class.
 */
private class BounceOutCurve : Curve() {

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        return bounce(t)
    }
}

/**
 * An oscillating curve that first grows and then shrink in magnitude.
 *
 * See [Curves.bounceInOut] for an instance of this class.
 */
private class BounceInOutCurve : Curve() {

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        return if (t < 0.5)
            (1.0 - bounce(1.0 - t)) * 0.5
        else
            bounce(t * 2.0 - 1.0) * 0.5 + 0.5
    }
}

// ELASTIC CURVES

/**
 * An oscillating curve that grows in magnitude while overshooting its bounds.
 *
 * An instance of this class using the default period of 0.4 is available as
 * [Curves.elasticIn].
 *
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_elastic_in.png)
 *
 * Rather than creating a new instance, consider using [Curves.elasticIn].
 */
class ElasticInCurve(
    /** The duration of the oscillation. */
    private val period: Double = 0.4
) : Curve() {

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        val s = period / 4.0
        val newT = t - 1.0
        return -Math.pow(2.0, 10.0 * newT) * Math.sin((newT - s) * (Math.PI * 2.0) / period)
    }

    override fun toString() = "${runtimeType()}($period)"
}

/**
 * An oscillating curve that shrinks in magnitude while overshooting its bounds.
 *
 * An instance of this class using the default period of 0.4 is available as
 * [Curves.elasticOut].
 *
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_elastic_out.png)
 *
 * Rather than creating a new instance, consider using [Curves.elasticOut].
 */
class ElasticOutCurve(
    /** The duration of the oscillation. */
    private val period: Double = 0.4
) : Curve() {

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        val s = period / 4.0
        return Math.pow(2.0, -10 * t) * Math.sin((t - s) * (Math.PI * 2.0) / period) + 1.0
    }

    override fun toString() = "${runtimeType()}($period)"
}

/**
 * An oscillating curve that grows and then shrinks in magnitude while
 * overshooting its bounds.
 *
 * An instance of this class using the default period of 0.4 is available as
 * [Curves.elasticInOut].
 *
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_elastic_in_out.png)
 *
 * Rather than creating a new instance, consider using [Curves.elasticInOut].
 */
class ElasticInOutCurve(
    /** The duration of the oscillation. */
    private val period: Double = 0.4
) : Curve() {

    override fun transform(t: Double): Double {
        assert(t in 0.0..1.0)
        val s = period / 4.0
        val newT = 2.0 * t - 1.0
        return if (newT < 0.0)
            -0.5 * Math.pow(2.0, 10.0 * newT) * Math.sin((newT - s) * (Math.PI * 2.0) / period)
        else
            Math.pow(
                2.0,
                -10.0 * newT
            ) * Math.sin((newT - s) * (Math.PI * 2.0) / period) * 0.5 + 1.0
    }

    override fun toString() = "${runtimeType()}($period)"
}

// PREDEFINED CURVES

/**
 * A collection of common animation curves.
 *
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_bounce_in.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_bounce_in_out.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_bounce_out.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_decelerate.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease_in.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease_in_out.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease_out.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_elastic_in.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_elastic_in_out.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_elastic_out.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_fast_out_slow_in.png)
 * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_linear.png)
 *
 * See also:
 *
 *  * [Curve], the interface implemented by the constants available from the
 *    [Curves] class.
 */
class Curves {
    companion object {

        /**
         * A linear animation curve.
         *
         * This is the identity map over the unit interval: its [Curve.transform]
         * method returns its input unmodified. This is useful as a default curve for
         * cases where a [Curve] is required but no actual curve is desired.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_linear.png)
         */
        val linear: Curve = Linear()

        /**
         * A curve where the rate of change starts out quickly and then decelerates; an
         * upside-down `f(t) = t²` parabola.
         *
         * This is equivalent to the Android `DecelerateInterpolator` class with a unit
         * factor (the default factor).
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_decelerate.png)
         */
        val decelerate: Curve = DecelerateCurve()

        /**
         * A cubic animation curve that speeds up quickly and ends slowly.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease.png)
         */
        val ease = Cubic(0.25, 0.1, 0.25, 1.0)

        /**
         * A cubic animation curve that starts slowly and ends quickly.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease_in.png)
         */
        val easeIn = Cubic(0.42, 0.0, 1.0, 1.0)

        /**
         * A cubic animation curve that starts quickly and ends slowly.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease_out.png)
         */
        val easeOut = Cubic(0.0, 0.0, 0.58, 1.0)

        /**
         * A cubic animation curve that starts slowly, speeds up, and then and ends slowly.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_ease_in_out.png)
         */
        val easeInOut = Cubic(0.42, 0.0, 0.58, 1.0)

        /**
         * A curve that starts quickly and eases into its val position.
         *
         * Over the course of the animation, the object spends more time near its
         * val destination. As a result, the user isn’t left waiting for the
         * animation to finish, and the negative effects of motion are minimized.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_fast_out_slow_in.png)
         */
        val fastOutSlowIn = Cubic(0.4, 0.0, 0.2, 1.0)

        /**
         * An oscillating curve that grows in magnitude.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_bounce_in.png)
         */
        val bounceIn: Curve = BounceInCurve()

        /**
         * An oscillating curve that first grows and then shrink in magnitude.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_bounce_out.png)
         */
        val bounceOut: Curve = BounceOutCurve()

        /**
         * An oscillating curve that first grows and then shrink in magnitude.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_bounce_in_out.png)
         */
        val bounceInOut: Curve = BounceInOutCurve()

        /**
         * An oscillating curve that grows in magnitude while overshooting its bounds.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_elastic_in.png)
         */
        val elasticIn = ElasticInCurve()

        /**
         * An oscillating curve that shrinks in magnitude while overshooting its bounds.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_elastic_out.png)
         */
        val elasticOut = ElasticOutCurve()

        /**
         * An oscillating curve that grows and then shrinks in magnitude while overshooting its bounds.
         *
         * ![](https://flutter.github.io/assets-for-aPI-docs/assets/animation/curve_elastic_in_out.png)
         */
        val elasticInOut = ElasticInOutCurve()
    }
}