/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.document.intervals

import androidx.annotation.RestrictTo
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * An immutable extended-real interval between [min] and [max] with configurable open or closed
 * endpoint inclusivity, representing a conservative bounding range guaranteed to contain every
 * non-`NaN` RemoteCompose float value reachable at runtime.
 *
 * Models the non-`NaN` extended real line `[-Infinity, +Infinity]` so IEEE-754 float overflow or
 * division by zero that yields infinities remains contained in [UNBOUNDED]; `NaN` is never a member
 * of any interval, and operations that may produce `NaN` widen to [UNBOUNDED]. Implements interval
 * arithmetic for expressions involving time, periodic waveforms, remainder, trigonometry, rounding,
 * and three-valued relational comparisons.
 *
 * @property min The infimum (lower bound) of this interval, or [Float.NEGATIVE_INFINITY] if
 *   unbounded below.
 * @property max The supremum (upper bound) of this interval, or [Float.POSITIVE_INFINITY] if
 *   unbounded above.
 * @property minInclusive Whether [min] itself is included (`true` for `[`, `false` for `(`).
 * @property maxInclusive Whether [max] itself is included (`true` for `]`, `false` for `)`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FloatInterval
@JvmOverloads
constructor(
    public val min: Float,
    public val max: Float,
    public val minInclusive: Boolean = true,
    public val maxInclusive: Boolean = true,
) {
    /**
     * Whether this non-empty interval has at least one infinite endpoint ([Float.NEGATIVE_INFINITY]
     * or [Float.POSITIVE_INFINITY]).
     */
    public val isUnbounded: Boolean
        get() = !isEmpty && (min.isInfinite() || max.isInfinite())

    /**
     * Whether this interval contains no real values (for example, `min > max`, either bound is
     * `NaN`, or `min == max` with at least one open endpoint).
     */
    public val isEmpty: Boolean
        get() =
            min.isNaN() ||
                max.isNaN() ||
                min > max ||
                (min == max && (!minInclusive || !maxInclusive))

    /** Whether this interval represents a single finite constant value `[v, v]`. */
    public val isPoint: Boolean
        get() = !isEmpty && !isUnbounded && min == max && minInclusive && maxInclusive

    /**
     * Returns an interval soundly bounding `a + b` for all `a in this` and `b in other`, or [EMPTY]
     * if either operand is empty. Computed bounds are closed because IEEE-754 rounding can map
     * interior values onto a rounded endpoint.
     */
    public operator fun plus(other: FloatInterval): FloatInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        val newMin = min + other.min
        val newMax = max + other.max
        if (newMin.isNaN() || newMax.isNaN()) return UNBOUNDED
        return FloatInterval(newMin, newMax)
    }

    /**
     * Returns an interval soundly bounding `a - b` for all `a in this` and `b in other`, or [EMPTY]
     * if either operand is empty. Computed bounds are closed because IEEE-754 rounding can map
     * interior values onto a rounded endpoint.
     */
    public operator fun minus(other: FloatInterval): FloatInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        val newMin = min - other.max
        val newMax = max - other.min
        if (newMin.isNaN() || newMax.isNaN()) return UNBOUNDED
        return FloatInterval(newMin, newMax)
    }

    /**
     * Returns an interval soundly bounding `a * b` for all `a in this` and `b in other`, or [EMPTY]
     * if either operand is empty.
     *
     * Singleton [ZERO] absorbs any non-empty interval to [ZERO] (preserving axis independence in
     * affine expressions when multiplying a zero matrix entry by an unbounded coordinate);
     * indeterminate corner products from non-point intervals containing `0f` and infinity widen
     * soundly to [UNBOUNDED].
     */
    public operator fun times(other: FloatInterval): FloatInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        if ((isPoint && min == 0f) || (other.isPoint && other.min == 0f)) return ZERO
        if (isPoint && other.isPoint) {
            val product = min * other.min
            return if (product.isNaN()) UNBOUNDED else FloatInterval(product, product)
        }
        return combineCornerBounds(
            min * other.min,
            min * other.max,
            max * other.min,
            max * other.max,
        )
    }

    /**
     * Returns an interval soundly bounding `a / b` for all `a in this` and `b in other`, or [EMPTY]
     * if either operand is empty. Widens soundly to [UNBOUNDED] if [other] contains `0f` or
     * produces indeterminate bounds.
     */
    public operator fun div(other: FloatInterval): FloatInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        if (other.min <= 0f && other.max >= 0f) return UNBOUNDED
        return combineCornerBounds(
            min / other.min,
            min / other.max,
            max / other.min,
            max / other.max,
        )
    }

    /**
     * Returns an interval soundly bounding `-a` for all `a in this`, or [EMPTY] if this interval is
     * empty.
     */
    public operator fun unaryMinus(): FloatInterval {
        if (isEmpty) return EMPTY
        return FloatInterval(-max, -min, minInclusive = maxInclusive, maxInclusive = minInclusive)
    }

    /**
     * Returns an interval soundly bounding `|a|` for all `a in this`, or [EMPTY] if this interval
     * is empty.
     */
    public fun abs(): FloatInterval {
        if (isEmpty) return EMPTY
        return when {
            min >= 0f -> this
            max <= 0f ->
                FloatInterval(-max, -min, minInclusive = maxInclusive, maxInclusive = minInclusive)
            else -> {
                val negMin = -min
                val outMax = max(negMin, max)
                val outMaxInc =
                    when {
                        negMin > max -> minInclusive
                        max > negMin -> maxInclusive
                        else -> minInclusive || maxInclusive
                    }
                FloatInterval(0f, outMax, minInclusive = true, maxInclusive = outMaxInc)
            }
        }
    }

    /**
     * Returns an interval soundly bounding `min(a, b)` for all `a in this` and `b in other`, or
     * [EMPTY] if either operand is empty.
     */
    public fun min(other: FloatInterval): FloatInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        return FloatInterval(
            min = min(min, other.min),
            max = min(max, other.max),
            minInclusive = lowerInclusiveForUnion(other),
            maxInclusive = upperInclusiveForIntersection(other),
        )
    }

    /**
     * Returns an interval soundly bounding `max(a, b)` for all `a in this` and `b in other`, or
     * [EMPTY] if either operand is empty.
     */
    public fun max(other: FloatInterval): FloatInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        return FloatInterval(
            min = max(min, other.min),
            max = max(max, other.max),
            minInclusive = lowerInclusiveForIntersection(other),
            maxInclusive = upperInclusiveForUnion(other),
        )
    }

    /**
     * Returns an interval soundly bounding `min(max(minimum, a), maximum)` for all `a in this`,
     * matching RemoteCompose `OP_CLAMP` semantics, or [EMPTY] if any operand is empty.
     *
     * @param minimum Interval bounding the lower clamp limit.
     * @param maximum Interval bounding the upper clamp limit.
     * @return The clamped interval, or [EMPTY] if any input interval is empty.
     */
    public fun clamp(minimum: FloatInterval, maximum: FloatInterval): FloatInterval {
        if (isEmpty || minimum.isEmpty || maximum.isEmpty) return EMPTY
        return max(minimum).min(maximum)
    }

    /**
     * Returns an interval guaranteed to contain `sqrt(a)` for all `a in this`, or [EMPTY] if this
     * interval is empty.
     *
     * Widens to [UNBOUNDED] if this interval may contain negative values (`min < 0f`), because
     * RemoteCompose `OP_SQRT` evaluates `(float) Math.sqrt(a)` and yields `NaN` for negative inputs
     * rather than clamping them. Computed bounds are closed because IEEE-754 rounding can map
     * interior values onto the endpoint square root.
     */
    public fun sqrt(): FloatInterval {
        if (isEmpty) return EMPTY
        if (min < 0f) return UNBOUNDED
        val minRoot = if (min == 0f) 0f else sqrt(min)
        return FloatInterval(minRoot, sqrt(max))
    }

    /** Returns an interval soundly bounding `floor(a)` for all `a in this`, or [EMPTY] if empty. */
    public fun floor(): FloatInterval = applyMonotonicRounding(::floor)

    /** Returns an interval soundly bounding `ceil(a)` for all `a in this`, or [EMPTY] if empty. */
    public fun ceil(): FloatInterval = applyMonotonicRounding(::ceil)

    /**
     * Returns an interval soundly bounding `(float) Math.round(a)` (matching RemoteCompose
     * `OP_ROUND`, which rounds ties toward `+Infinity` and saturates to 32-bit `Int` bounds) for
     * all `a in this`, or [EMPTY] if empty.
     */
    public fun round(): FloatInterval {
        if (isEmpty) return EMPTY
        val effectiveMin = if (minInclusive || min.isInfinite()) min else min.nextUp()
        val effectiveMax = if (maxInclusive || max.isInfinite()) max else max.nextDown()
        val outMin = Math.round(effectiveMin).toFloat()
        val outMax = Math.round(effectiveMax).toFloat()
        return if (outMin > outMax) EMPTY else FloatInterval(outMin, outMax)
    }

    /** Returns an interval soundly bounding `sin(a)` (radians) for all `a in this`, or [EMPTY]. */
    public fun sin(): FloatInterval =
        evaluatePeriodicTrig(
            fn = { sin(it).toFloat() },
            maxPeakPhase = PI / 2.0,
            minPeakPhase = 3.0 * PI / 2.0,
        )

    /** Returns an interval soundly bounding `cos(a)` (radians) for all `a in this`, or [EMPTY]. */
    public fun cos(): FloatInterval =
        evaluatePeriodicTrig(fn = { cos(it).toFloat() }, maxPeakPhase = 0.0, minPeakPhase = PI)

    /**
     * Returns an interval soundly bounding IEEE-754 truncated remainder `a % b` for all `a in this`
     * and `b in divisor`, or [EMPTY] if either operand is empty.
     *
     * @param divisor Interval bounding the right-hand divisor.
     * @return Sound bounding interval for `this % divisor`.
     */
    public operator fun rem(divisor: FloatInterval): FloatInterval {
        if (isEmpty || divisor.isEmpty) return EMPTY
        if (divisor.min <= 0f && divisor.max <= 0f) return UNBOUNDED
        if (divisor.isPoint && divisor.min > 0f) return remByPositivePoint(divisor.min)
        if (divisor.min > 0f) {
            val maxInf = divisor.max.isInfinite()
            return when {
                min >= 0f ->
                    FloatInterval(0f, divisor.max, minInclusive = true, maxInclusive = maxInf)
                max <= 0f ->
                    FloatInterval(-divisor.max, 0f, minInclusive = maxInf, maxInclusive = true)
                else ->
                    FloatInterval(
                        -divisor.max,
                        divisor.max,
                        minInclusive = maxInf,
                        maxInclusive = maxInf,
                    )
            }
        }
        return UNBOUNDED
    }

    /**
     * Returns the tightest interval hull containing both this interval and [other]. If either
     * operand is [EMPTY], returns the other operand.
     */
    public fun union(other: FloatInterval): FloatInterval {
        if (isEmpty) return other
        if (other.isEmpty) return this
        return FloatInterval(
            min = min(min, other.min),
            max = max(max, other.max),
            minInclusive = lowerInclusiveForUnion(other),
            maxInclusive = upperInclusiveForUnion(other),
        )
    }

    /**
     * Returns the exact set intersection of this interval and [other], or [EMPTY] if the intervals
     * are disjoint or either is empty.
     */
    public fun intersect(other: FloatInterval): FloatInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        val lower = max(min, other.min)
        val upper = min(max, other.max)
        val lowerInc = lowerInclusiveForIntersection(other)
        val upperInc = upperInclusiveForIntersection(other)
        if (lower > upper || (lower == upper && (!lowerInc || !upperInc))) return EMPTY
        return FloatInterval(lower, upper, lowerInc, upperInc)
    }

    /**
     * Returns `true` if [value] lies within this interval respecting endpoint inclusivity, or
     * `false` if [value] is `NaN` or outside the interval.
     */
    public operator fun contains(value: Float): Boolean {
        if (isEmpty || value.isNaN()) return false
        val geMin = if (minInclusive) value >= min else value > min
        val leMax = if (maxInclusive) value <= max else value < max
        return geMin && leMax
    }

    /**
     * Returns `true` if this interval and [other] share no common values (always `true` when either
     * interval is empty).
     */
    public fun isDisjoint(other: FloatInterval): Boolean {
        if (isEmpty || other.isEmpty) return true
        if (max < other.min || (max == other.min && (!maxInclusive || !other.minInclusive))) {
            return true
        }
        if (min > other.max || (min == other.max && (!minInclusive || !other.maxInclusive))) {
            return true
        }
        return false
    }

    /**
     * Evaluates the three-valued relational predicate `this < other` across all value pairs in
     * `this x other`.
     */
    public fun compareLessThan(other: FloatInterval): TriState {
        if (isEmpty || other.isEmpty) return TriState.UNKNOWN
        if (max < other.min || (max == other.min && (!maxInclusive || !other.minInclusive))) {
            return TriState.TRUE
        }
        if (min >= other.max) return TriState.FALSE
        return TriState.UNKNOWN
    }

    /**
     * Evaluates the three-valued relational predicate `this <= other` across all value pairs in
     * `this x other`.
     */
    public fun compareLessThanOrEqual(other: FloatInterval): TriState {
        if (isEmpty || other.isEmpty) return TriState.UNKNOWN
        if (max <= other.min) return TriState.TRUE
        if (min > other.max || (min == other.max && (!minInclusive || !other.maxInclusive))) {
            return TriState.FALSE
        }
        return TriState.UNKNOWN
    }

    /**
     * Evaluates the three-valued relational predicate `this > other` across all value pairs in
     * `this x other`.
     */
    public fun compareGreaterThan(other: FloatInterval): TriState = other.compareLessThan(this)

    /**
     * Evaluates the three-valued relational predicate `this >= other` across all value pairs in
     * `this x other`.
     */
    public fun compareGreaterThanOrEqual(other: FloatInterval): TriState =
        other.compareLessThanOrEqual(this)

    /**
     * Evaluates the three-valued relational predicate `this == other` across all value pairs in
     * `this x other`.
     */
    public fun compareEqualTo(other: FloatInterval): TriState {
        if (isEmpty || other.isEmpty) return TriState.UNKNOWN
        if (isPoint && other.isPoint && min == other.min) return TriState.TRUE
        if (isDisjoint(other)) return TriState.FALSE
        return TriState.UNKNOWN
    }

    /**
     * Evaluates the three-valued relational predicate `this != other` across all value pairs in
     * `this x other`.
     */
    public fun compareNotEqualTo(other: FloatInterval): TriState = !compareEqualTo(other)

    /** Evaluates [comparison] between `this` and [other]. */
    public fun compare(comparison: IntervalComparison, other: FloatInterval): TriState =
        when (comparison) {
            IntervalComparison.EQ -> compareEqualTo(other)
            IntervalComparison.NEQ -> compareNotEqualTo(other)
            IntervalComparison.LT -> compareLessThan(other)
            IntervalComparison.LTE -> compareLessThanOrEqual(other)
            IntervalComparison.GT -> compareGreaterThan(other)
            IntervalComparison.GTE -> compareGreaterThanOrEqual(other)
        }

    /**
     * Returns `true` if every value in this interval is strictly less than every value in [other].
     */
    public fun isAlwaysLessThan(other: FloatInterval): Boolean =
        compareLessThan(other) == TriState.TRUE

    /** Returns `true` if every value in this interval is `<=` every value in [other]. */
    public fun isAlwaysLessThanOrEqual(other: FloatInterval): Boolean =
        compareLessThanOrEqual(other) == TriState.TRUE

    /**
     * Returns `true` if every value in this interval is strictly greater than every value in
     * [other].
     */
    public fun isAlwaysGreaterThan(other: FloatInterval): Boolean =
        compareGreaterThan(other) == TriState.TRUE

    /** Returns `true` if every value in this interval is `>=` every value in [other]. */
    public fun isAlwaysGreaterThanOrEqual(other: FloatInterval): Boolean =
        compareGreaterThanOrEqual(other) == TriState.TRUE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FloatInterval) return false
        if (isEmpty && other.isEmpty) return true
        if (isEmpty || other.isEmpty) return false
        return min == other.min &&
            max == other.max &&
            minInclusive == other.minInclusive &&
            maxInclusive == other.maxInclusive
    }

    override fun hashCode(): Int {
        if (isEmpty) return 0
        var result = canonicalFloatBits(min)
        result = 31 * result + canonicalFloatBits(max)
        result = 31 * result + minInclusive.hashCode()
        result = 31 * result + maxInclusive.hashCode()
        return result
    }

    override fun toString(): String {
        val open = if (minInclusive) "[" else "("
        val close = if (maxInclusive) "]" else ")"
        return "FloatInterval($open$min, $max$close)"
    }

    private fun combineCornerBounds(c1: Float, c2: Float, c3: Float, c4: Float): FloatInterval {
        if (c1.isNaN() || c2.isNaN() || c3.isNaN() || c4.isNaN()) return UNBOUNDED
        return FloatInterval(minOf(c1, c2, c3, c4), maxOf(c1, c2, c3, c4))
    }

    private fun intervalPreservingNonEmpty(
        min: Float,
        max: Float,
        minInclusive: Boolean,
        maxInclusive: Boolean,
    ): FloatInterval {
        val collapsed = min == max
        return FloatInterval(
            min = min,
            max = max,
            minInclusive = collapsed || minInclusive,
            maxInclusive = collapsed || maxInclusive,
        )
    }

    private fun applyMonotonicRounding(op: (Float) -> Float): FloatInterval {
        if (isEmpty) return EMPTY
        val effectiveMin = if (minInclusive || min.isInfinite()) min else min.nextUp()
        val effectiveMax = if (maxInclusive || max.isInfinite()) max else max.nextDown()
        val outMin = if (effectiveMin.isInfinite()) effectiveMin else op(effectiveMin)
        val outMax = if (effectiveMax.isInfinite()) effectiveMax else op(effectiveMax)
        return if (outMin > outMax) EMPTY else FloatInterval(outMin, outMax)
    }

    private fun evaluatePeriodicTrig(
        fn: (Double) -> Float,
        maxPeakPhase: Double,
        minPeakPhase: Double,
    ): FloatInterval {
        if (isEmpty) return EMPTY
        val twoPi = 2.0 * PI
        if (isUnbounded || (max - min) >= twoPi.toFloat()) {
            return FloatInterval(-1f, 1f)
        }
        val vMin = fn(min.toDouble())
        val vMax = fn(max.toDouble())
        var low = min(vMin, vMax)
        var high = max(vMin, vMax)

        val kMax = ceil((min.toDouble() - maxPeakPhase) / twoPi)
        if (maxPeakPhase + kMax * twoPi <= max.toDouble()) {
            high = 1f
        }
        val kMin = ceil((min.toDouble() - minPeakPhase) / twoPi)
        if (minPeakPhase + kMin * twoPi <= max.toDouble()) {
            low = -1f
        }
        return FloatInterval(low.coerceIn(-1f, 1f), high.coerceIn(-1f, 1f))
    }

    private fun remByPositivePoint(m: Float): FloatInterval {
        if (min < 0f) {
            return if (max <= 0f) {
                FloatInterval(-m, 0f, minInclusive = false, maxInclusive = true)
            } else {
                FloatInterval(-m, m, minInclusive = false, maxInclusive = false)
            }
        }
        if (max.isInfinite() || (max - min) >= m) {
            return FloatInterval(0f, m, minInclusive = true, maxInclusive = false)
        }
        val effectiveMax = if (maxInclusive) max else max.nextDown()
        val k1 = floor(min.toDouble() / m.toDouble())
        val k2 = floor(effectiveMax.toDouble() / m.toDouble())
        if (k1 != k2) {
            return FloatInterval(0f, m, minInclusive = true, maxInclusive = false)
        }
        val remMin = min % m
        val rawRemMax = max % m
        return when {
            !maxInclusive && rawRemMax == 0f && max > min ->
                FloatInterval(remMin, m, minInclusive = minInclusive, maxInclusive = false)
            remMin <= rawRemMax ->
                intervalPreservingNonEmpty(remMin, rawRemMax, minInclusive, maxInclusive)
            else -> FloatInterval(0f, m, minInclusive = true, maxInclusive = false)
        }
    }

    private fun lowerInclusiveForUnion(other: FloatInterval): Boolean =
        when {
            min < other.min -> minInclusive
            other.min < min -> other.minInclusive
            else -> minInclusive || other.minInclusive
        }

    private fun upperInclusiveForUnion(other: FloatInterval): Boolean =
        when {
            max > other.max -> maxInclusive
            other.max > max -> other.maxInclusive
            else -> maxInclusive || other.maxInclusive
        }

    private fun lowerInclusiveForIntersection(other: FloatInterval): Boolean =
        when {
            min > other.min -> minInclusive
            other.min > min -> other.minInclusive
            else -> minInclusive && other.minInclusive
        }

    private fun upperInclusiveForIntersection(other: FloatInterval): Boolean =
        when {
            max < other.max -> maxInclusive
            other.max < max -> other.maxInclusive
            else -> maxInclusive && other.maxInclusive
        }

    public companion object {
        /** Largest integer magnitude representable exactly in IEEE-754 binary32 (`2^24`). */
        public const val MAX_EXACT_INT: Int = 1 shl 24

        /** The full extended-real interval `[-Infinity, +Infinity]`. */
        public val UNBOUNDED: FloatInterval =
            FloatInterval(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)

        /** The singleton interval `[0f, 0f]`. */
        public val ZERO: FloatInterval = FloatInterval(0f, 0f)

        /** The empty interval containing no values. */
        public val EMPTY: FloatInterval =
            FloatInterval(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

        /** The closed unit interval `[0f, 1f]`. */
        public val UNIT: FloatInterval = FloatInterval(0f, 1f)

        /**
         * Returns a singleton interval `[value, value]`, or [UNBOUNDED] if [value] is `NaN`.
         *
         * @param value The constant float value to wrap.
         * @return Point interval `[value, value]`, or [UNBOUNDED] for `NaN`.
         */
        @JvmStatic
        public fun point(value: Float): FloatInterval =
            if (value.isNaN()) UNBOUNDED else FloatInterval(value, value)

        private fun canonicalFloatBits(value: Float): Int =
            when {
                value == 0f -> 0
                value.isNaN() -> Float.NaN.toBits()
                else -> value.toBits()
            }
    }
}
