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
import kotlin.math.max
import kotlin.math.min

/**
 * An immutable overflow-safe closed 32-bit signed integer interval `[min, max]`.
 *
 * Uses [MIN_BOUND] and [MAX_BOUND] as negative and positive infinity sentinels representing
 * `[Int.MIN_VALUE, MIN_BOUND]` and `[MAX_BOUND, Int.MAX_VALUE]` respectively. Arithmetic operators
 * (`+`, `-`, `*`, `/`, unary `-`, `abs`) model 32-bit two's-complement wrap-around soundly by
 * widening to [UNBOUNDED] whenever the exact 64-bit result exits `[Int.MIN_VALUE, Int.MAX_VALUE]`.
 *
 * Constructor arguments with `min > max` normalize to [EMPTY] (`[1, 0]`), and finite bounds outside
 * `[MIN_BOUND, MAX_BOUND]` normalize to the corresponding sentinel.
 *
 * @property min The lower endpoint of this interval (values `<= MIN_BOUND` represent `-Infinity`).
 * @property max The upper endpoint of this interval (values `>= MAX_BOUND` represent `+Infinity`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IntInterval(min: Int, max: Int) {
    /** The lower endpoint of this interval ([MIN_BOUND] represents unbounded below). */
    public val min: Int = if (min > max) 1 else min.coerceIn(MIN_BOUND, MAX_BOUND)

    /** The upper endpoint of this interval ([MAX_BOUND] represents unbounded above). */
    public val max: Int = if (min > max) 0 else max.coerceIn(MIN_BOUND, MAX_BOUND)

    /** Whether this interval contains no integers (`min > max`). */
    public val isEmpty: Boolean
        get() = min > max

    /**
     * Whether either endpoint is represented by an infinity sentinel ([MIN_BOUND] or [MAX_BOUND]).
     */
    public val isUnbounded: Boolean
        get() = !isEmpty && (min <= MIN_BOUND || max >= MAX_BOUND)

    /** Whether this interval represents a single finite integer `[v, v]`. */
    public val isPoint: Boolean
        get() = min == max && !isUnbounded && !isEmpty

    /** Whether this non-empty interval has a finite lower bound (`min > MIN_BOUND`). */
    public val hasFiniteMin: Boolean
        get() = !isEmpty && min > MIN_BOUND

    /** Whether this non-empty interval has a finite upper bound (`max < MAX_BOUND`). */
    public val hasFiniteMax: Boolean
        get() = !isEmpty && max < MAX_BOUND

    private val actualMinLong: Long
        get() = if (hasFiniteMin) min.toLong() else Int.MIN_VALUE.toLong()

    private val actualMaxLong: Long
        get() = if (hasFiniteMax) max.toLong() else Int.MAX_VALUE.toLong()

    /**
     * Widens this integer interval to a [FloatInterval], mapping infinity sentinels to
     * [Float.NEGATIVE_INFINITY] and [Float.POSITIVE_INFINITY].
     */
    public fun toFloatInterval(): FloatInterval =
        when {
            isEmpty -> FloatInterval.EMPTY
            isUnbounded ->
                FloatInterval(
                    if (min <= MIN_BOUND) Float.NEGATIVE_INFINITY else min.toFloat(),
                    if (max >= MAX_BOUND) Float.POSITIVE_INFINITY else max.toFloat(),
                )
            else -> FloatInterval(min.toFloat(), max.toFloat())
        }

    /**
     * Returns an interval soundly bounding 32-bit `a + b` for all `a in this` and `b in other`,
     * widening to [UNBOUNDED] if 32-bit signed overflow can occur, or [EMPTY] if either operand is
     * empty.
     */
    public operator fun plus(other: IntInterval): IntInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        return fromInt32ArithmeticBounds(
            actualMinLong + other.actualMinLong,
            actualMaxLong + other.actualMaxLong,
        )
    }

    /**
     * Returns an interval soundly bounding 32-bit `a - b` for all `a in this` and `b in other`,
     * widening to [UNBOUNDED] if 32-bit signed overflow can occur, or [EMPTY] if either operand is
     * empty.
     */
    public operator fun minus(other: IntInterval): IntInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        return fromInt32ArithmeticBounds(
            actualMinLong - other.actualMaxLong,
            actualMaxLong - other.actualMinLong,
        )
    }

    /**
     * Returns an interval soundly bounding 32-bit `a * b` for all `a in this` and `b in other`,
     * widening to [UNBOUNDED] if 32-bit signed overflow can occur, or [EMPTY] if either operand is
     * empty.
     */
    public operator fun times(other: IntInterval): IntInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        if ((isPoint && min == 0) || (other.isPoint && other.min == 0)) return ZERO
        val aMin = actualMinLong
        val aMax = actualMaxLong
        val bMin = other.actualMinLong
        val bMax = other.actualMaxLong
        val p1 = aMin * bMin
        val p2 = aMin * bMax
        val p3 = aMax * bMin
        val p4 = aMax * bMax
        return fromInt32ArithmeticBounds(minOf(p1, p2, p3, p4), maxOf(p1, p2, p3, p4))
    }

    /**
     * Returns an interval soundly bounding 32-bit truncating integer division `a / b` for all `a in
     * this` and `b in other`, or [EMPTY] if either operand is empty. Widens to [UNBOUNDED] if
     * [other] contains `0` or `Int.MIN_VALUE / -1` wrap-around is possible.
     */
    public operator fun div(other: IntInterval): IntInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        if (other.min <= 0 && other.max >= 0) return UNBOUNDED
        val aMin = actualMinLong
        val aMax = actualMaxLong
        val bMin = other.actualMinLong
        val bMax = other.actualMaxLong
        val d1 = aMin / bMin
        val d2 = aMin / bMax
        val d3 = aMax / bMin
        val d4 = aMax / bMax
        return fromInt32ArithmeticBounds(minOf(d1, d2, d3, d4), maxOf(d1, d2, d3, d4))
    }

    /**
     * Returns an interval soundly bounding 32-bit `-a` for all `a in this`, widening to [UNBOUNDED]
     * if [Int.MIN_VALUE] is reachable (`-Int.MIN_VALUE == Int.MIN_VALUE`), or [EMPTY] if empty.
     */
    public operator fun unaryMinus(): IntInterval {
        if (isEmpty) return EMPTY
        return fromInt32ArithmeticBounds(-actualMaxLong, -actualMinLong)
    }

    /**
     * Returns an interval soundly bounding `min(a, b)` for all `a in this` and `b in other`, or
     * [EMPTY] if either operand is empty.
     */
    public fun min(other: IntInterval): IntInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        return IntInterval(min(min, other.min), min(max, other.max))
    }

    /**
     * Returns an interval soundly bounding `max(a, b)` for all `a in this` and `b in other`, or
     * [EMPTY] if either operand is empty.
     */
    public fun max(other: IntInterval): IntInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        return IntInterval(max(min, other.min), max(max, other.max))
    }

    /**
     * Returns an interval soundly bounding 32-bit `Math.abs(a)` for all `a in this`, widening to
     * [UNBOUNDED] if [Int.MIN_VALUE] is reachable (`Math.abs(Int.MIN_VALUE) == Int.MIN_VALUE`), or
     * [EMPTY] if empty.
     */
    public fun abs(): IntInterval {
        if (isEmpty) return EMPTY
        return when {
            min >= 0 -> this
            !hasFiniteMin -> UNBOUNDED
            max <= 0 -> -this
            !hasFiniteMax -> IntInterval(0, MAX_BOUND)
            else -> fromLongBounds(0L, max(-min.toLong(), max.toLong()))
        }
    }

    /**
     * Returns an interval soundly bounding `min(max(minimum, a), maximum)` for all `a in this`,
     * matching RemoteCompose integer `I_CLAMP` semantics, or [EMPTY] if any operand is empty.
     *
     * @param minimum Interval bounding the lower clamp limit.
     * @param maximum Interval bounding the upper clamp limit.
     * @return The clamped interval, or [EMPTY] if any operand is empty.
     */
    public fun clamp(minimum: IntInterval, maximum: IntInterval): IntInterval {
        if (isEmpty || minimum.isEmpty || maximum.isEmpty) return EMPTY
        return max(minimum).min(maximum)
    }

    /**
     * Clamps this interval to the fixed closed range `[minimum, maximum]`, or returns [EMPTY] if
     * this interval is empty.
     *
     * @param minimum Fixed inclusive lower bound.
     * @param maximum Fixed inclusive upper bound.
     * @return The clamped interval, or [EMPTY] if this interval is empty.
     * @throws IllegalArgumentException if `minimum > maximum`.
     */
    public fun clampToRange(minimum: Int, maximum: Int): IntInterval {
        require(minimum <= maximum) { "minimum ($minimum) must be <= maximum ($maximum)" }
        if (isEmpty) return EMPTY
        return IntInterval(min.coerceIn(minimum, maximum), max.coerceIn(minimum, maximum))
    }

    /**
     * Returns an interval soundly bounding integer remainder `a % b` for all `a in this` and `b in
     * divisor`, or [EMPTY] if either operand is empty.
     */
    public operator fun rem(divisor: IntInterval): IntInterval {
        if (isEmpty || divisor.isEmpty) return EMPTY
        if (divisor.isUnbounded) return UNBOUNDED
        if (divisor.min > 0 && divisor.min == divisor.max) {
            return remByPositivePoint(divisor.min.toLong())
        }
        if (divisor.min > 0) {
            val maxRem = divisor.max.toLong() - 1L
            return when {
                min >= 0 -> fromLongBounds(0L, maxRem)
                max <= 0 -> fromLongBounds(-maxRem, 0L)
                else -> fromLongBounds(-maxRem, maxRem)
            }
        }
        return UNBOUNDED
    }

    /**
     * Returns the tightest interval hull containing both this interval and [other]. If either
     * operand is [EMPTY], returns the other operand.
     */
    public fun union(other: IntInterval): IntInterval {
        if (isEmpty) return other
        if (other.isEmpty) return this
        return IntInterval(min(min, other.min), max(max, other.max))
    }

    /**
     * Returns the exact set intersection of this interval and [other], or [EMPTY] if the intervals
     * are disjoint or either is empty.
     */
    public fun intersect(other: IntInterval): IntInterval {
        if (isEmpty || other.isEmpty) return EMPTY
        val l = max(min, other.min)
        val u = min(max, other.max)
        return if (l <= u) IntInterval(l, u) else EMPTY
    }

    /**
     * Returns `true` if [value] lies within this interval, treating [MIN_BOUND] and [MAX_BOUND]
     * endpoints as unbounded below and above respectively.
     */
    public operator fun contains(value: Int): Boolean =
        !isEmpty && (!hasFiniteMin || value >= min) && (!hasFiniteMax || value <= max)

    /**
     * Returns `true` if this interval and [other] share no common integers (always `true` when
     * either interval is empty).
     */
    public fun isDisjoint(other: IntInterval): Boolean {
        if (isEmpty || other.isEmpty) return true
        if (hasFiniteMax && other.hasFiniteMin && max < other.min) return true
        if (hasFiniteMin && other.hasFiniteMax && min > other.max) return true
        return false
    }

    /** Evaluates `this < other` across all integer pairs in `this x other`. */
    public fun compareLessThan(other: IntInterval): TriState {
        if (isEmpty || other.isEmpty) return TriState.UNKNOWN
        if (hasFiniteMax && other.hasFiniteMin && max < other.min) return TriState.TRUE
        if (hasFiniteMin && other.hasFiniteMax && min >= other.max) return TriState.FALSE
        return TriState.UNKNOWN
    }

    /** Evaluates `this <= other` across all integer pairs in `this x other`. */
    public fun compareLessThanOrEqual(other: IntInterval): TriState {
        if (isEmpty || other.isEmpty) return TriState.UNKNOWN
        if (hasFiniteMax && other.hasFiniteMin && max <= other.min) return TriState.TRUE
        if (hasFiniteMin && other.hasFiniteMax && min > other.max) return TriState.FALSE
        return TriState.UNKNOWN
    }

    /** Evaluates `this > other` across all integer pairs in `this x other`. */
    public fun compareGreaterThan(other: IntInterval): TriState = other.compareLessThan(this)

    /** Evaluates `this >= other` across all integer pairs in `this x other`. */
    public fun compareGreaterThanOrEqual(other: IntInterval): TriState =
        other.compareLessThanOrEqual(this)

    /** Evaluates `this == other` across all integer pairs in `this x other`. */
    public fun compareEqualTo(other: IntInterval): TriState {
        if (isEmpty || other.isEmpty) return TriState.UNKNOWN
        if (isPoint && other.isPoint && min == other.min) return TriState.TRUE
        if (isDisjoint(other)) return TriState.FALSE
        return TriState.UNKNOWN
    }

    /** Evaluates `this != other` across all integer pairs in `this x other`. */
    public fun compareNotEqualTo(other: IntInterval): TriState = !compareEqualTo(other)

    /** Evaluates [comparison] between `this` and [other]. */
    public fun compare(comparison: IntervalComparison, other: IntInterval): TriState =
        when (comparison) {
            IntervalComparison.EQ -> compareEqualTo(other)
            IntervalComparison.NEQ -> compareNotEqualTo(other)
            IntervalComparison.LT -> compareLessThan(other)
            IntervalComparison.LTE -> compareLessThanOrEqual(other)
            IntervalComparison.GT -> compareGreaterThan(other)
            IntervalComparison.GTE -> compareGreaterThanOrEqual(other)
        }

    /** Returns `true` if every integer in `this` is strictly less than every integer in [other]. */
    public fun isAlwaysLessThan(other: IntInterval): Boolean =
        compareLessThan(other) == TriState.TRUE

    /** Returns `true` if every integer in `this` is `<=` every integer in [other]. */
    public fun isAlwaysLessThanOrEqual(other: IntInterval): Boolean =
        compareLessThanOrEqual(other) == TriState.TRUE

    /**
     * Returns `true` if every integer in `this` is strictly greater than every integer in [other].
     */
    public fun isAlwaysGreaterThan(other: IntInterval): Boolean =
        compareGreaterThan(other) == TriState.TRUE

    /** Returns `true` if every integer in `this` is `>=` every integer in [other]. */
    public fun isAlwaysGreaterThanOrEqual(other: IntInterval): Boolean =
        compareGreaterThanOrEqual(other) == TriState.TRUE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntInterval) return false
        return min == other.min && max == other.max
    }

    override fun hashCode(): Int = 31 * min + max

    override fun toString(): String = "IntInterval([$min, $max])"

    private fun remByPositivePoint(m: Long): IntInterval {
        if (min < 0) {
            return if (max <= 0) fromLongBounds(-m + 1L, 0L) else fromLongBounds(-m + 1L, m - 1L)
        }
        if (isUnbounded || max.toLong() - min.toLong() >= m) {
            return fromLongBounds(0L, m - 1L)
        }
        val k1 = min.toLong() / m
        val k2 = max.toLong() / m
        return if (k1 == k2) {
            fromLongBounds(min.toLong() % m, max.toLong() % m)
        } else {
            fromLongBounds(0L, m - 1L)
        }
    }

    public companion object {
        /** Sentinel lower endpoint representing negative infinity in [IntInterval]. */
        public const val MIN_BOUND: Int = Int.MIN_VALUE / 2

        /** Sentinel upper endpoint representing positive infinity in [IntInterval]. */
        public const val MAX_BOUND: Int = Int.MAX_VALUE / 2

        /** The unbounded integer interval `[MIN_BOUND, MAX_BOUND]`. */
        public val UNBOUNDED: IntInterval = IntInterval(MIN_BOUND, MAX_BOUND)

        /** The singleton interval `[0, 0]`. */
        public val ZERO: IntInterval = IntInterval(0, 0)

        /** The non-negative integer interval `[0, MAX_BOUND]`. */
        public val NON_NEGATIVE: IntInterval = IntInterval(0, MAX_BOUND)

        /** The empty interval containing no integers. */
        public val EMPTY: IntInterval = IntInterval(1, 0)

        /**
         * Returns a singleton interval `[value, value]`, saturating values outside `(MIN_BOUND,
         * MAX_BOUND)` to a sound non-point sentinel range (`[MIN_BOUND, MIN_BOUND]` or `[MAX_BOUND,
         * MAX_BOUND]`).
         */
        @JvmStatic
        public fun point(value: Int): IntInterval = fromLongBounds(value.toLong(), value.toLong())

        /**
         * Constructs an [IntInterval] from 64-bit bounds `[lo, hi]`, clamping values outside
         * `(MIN_BOUND, MAX_BOUND)` to the infinity sentinels.
         */
        @JvmStatic
        public fun fromLongBounds(lo: Long, hi: Long): IntInterval {
            if (lo > hi) return EMPTY
            val minInt = lo.coerceIn(MIN_BOUND.toLong(), MAX_BOUND.toLong()).toInt()
            val maxInt = hi.coerceIn(MIN_BOUND.toLong(), MAX_BOUND.toLong()).toInt()
            return IntInterval(minInt, maxInt)
        }

        private fun fromInt32ArithmeticBounds(lo: Long, hi: Long): IntInterval {
            if (lo > hi) return EMPTY
            if (lo < Int.MIN_VALUE.toLong() || hi > Int.MAX_VALUE.toLong()) return UNBOUNDED
            return fromLongBounds(lo, hi)
        }
    }
}
