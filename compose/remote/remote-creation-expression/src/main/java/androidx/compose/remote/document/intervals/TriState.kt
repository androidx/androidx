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
import androidx.compose.remote.document.ConditionType

/**
 * Result of statically evaluating a boolean predicate or relational comparison over intervals.
 *
 * [TRUE] and [FALSE] indicate that the predicate holds or fails across every reachable runtime
 * assignment in the analyzed intervals; [UNKNOWN] indicates that both outcomes are possible or an
 * input interval is empty.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class TriState {
    /** The predicate provably evaluates to `true` across all reachable runtime states. */
    TRUE,

    /** The predicate provably evaluates to `false` across all reachable runtime states. */
    FALSE,

    /** The predicate's runtime truth value cannot be statically proven constant. */
    UNKNOWN;

    /** Returns the boolean value if statically determined, or `null` for [UNKNOWN]. */
    public val booleanValueOrNull: Boolean?
        get() =
            when (this) {
                TRUE -> true
                FALSE -> false
                UNKNOWN -> null
            }

    /** Three-valued logical negation. */
    public operator fun not(): TriState =
        when (this) {
            TRUE -> FALSE
            FALSE -> TRUE
            UNKNOWN -> UNKNOWN
        }

    /**
     * Three-valued logical conjunction (Kleene strong logic).
     *
     * @param other The right-hand [TriState] operand.
     * @return [FALSE] if either operand is [FALSE], [TRUE] if both are [TRUE], else [UNKNOWN].
     */
    public infix fun and(other: TriState): TriState =
        when {
            this == FALSE || other == FALSE -> FALSE
            this == TRUE && other == TRUE -> TRUE
            else -> UNKNOWN
        }

    /**
     * Three-valued logical disjunction (Kleene strong logic).
     *
     * @param other The right-hand [TriState] operand.
     * @return [TRUE] if either operand is [TRUE], [FALSE] if both are [FALSE], else [UNKNOWN].
     */
    public infix fun or(other: TriState): TriState =
        when {
            this == TRUE || other == TRUE -> TRUE
            this == FALSE && other == FALSE -> FALSE
            else -> UNKNOWN
        }

    /** Converts this [TriState] to an integer interval (`[1, 1]`, `[0, 0]`, or `[0, 1]`). */
    public fun toIntInterval(): IntInterval =
        when (this) {
            TRUE -> IntInterval.point(1)
            FALSE -> IntInterval.ZERO
            UNKNOWN -> IntInterval(0, 1)
        }

    /** Converts this [TriState] to a float interval (`[1f, 1f]`, `[0f, 0f]`, or `[0f, 1f]`). */
    public fun toFloatInterval(): FloatInterval =
        when (this) {
            TRUE -> FloatInterval.point(1f)
            FALSE -> FloatInterval.ZERO
            UNKNOWN -> FloatInterval.UNIT
        }

    public companion object {
        /**
         * Converts a known [Boolean] into [TRUE] or [FALSE].
         *
         * @param value The concrete boolean value.
         * @return [TRUE] if [value] is `true`, otherwise [FALSE].
         */
        @JvmStatic public fun of(value: Boolean): TriState = if (value) TRUE else FALSE
    }
}

/**
 * Relational comparison operator aligned with [ConditionType] wire opcodes (`TYPE_EQ`..`TYPE_GTE`).
 *
 * Encapsulates logical negation, operand flipping, reflexive self-comparison, and sound interval
 * narrowing for both [FloatInterval] and [IntInterval].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class IntervalComparison(public val wireCode: Int) {
    /** Equality (`a == b`). */
    EQ(ConditionType.TYPE_EQ.toInt()),

    /** Inequality (`a != b`). */
    NEQ(ConditionType.TYPE_NEQ.toInt()),

    /** Strict less-than (`a < b`). */
    LT(ConditionType.TYPE_LT.toInt()),

    /** Less-than-or-equal (`a <= b`). */
    LTE(ConditionType.TYPE_LTE.toInt()),

    /** Strict greater-than (`a > b`). */
    GT(ConditionType.TYPE_GT.toInt()),

    /** Greater-than-or-equal (`a >= b`). */
    GTE(ConditionType.TYPE_GTE.toInt());

    /** Returns the logical negation of this comparison (`!(a op b)`). */
    public fun negate(): IntervalComparison =
        when (this) {
            EQ -> NEQ
            NEQ -> EQ
            LT -> GTE
            LTE -> GT
            GT -> LTE
            GTE -> LT
        }

    /** Returns the comparison obtained by swapping operands (`a op b` <=> `b flipped a`). */
    public fun flip(): IntervalComparison =
        when (this) {
            EQ -> EQ
            NEQ -> NEQ
            LT -> GT
            LTE -> GTE
            GT -> LT
            GTE -> LTE
        }

    /** Returns the result of `x op x` for any finite, non-NaN deterministic value `x`. */
    public fun selfComparisonResult(): TriState =
        when (this) {
            EQ,
            LTE,
            GTE -> TriState.TRUE
            NEQ,
            LT,
            GT -> TriState.FALSE
        }

    /** Narrows a pair of [FloatInterval] operands `(a, b)` assuming `a <this> b` holds. */
    public fun narrowFloat(a: FloatInterval, b: FloatInterval): Pair<FloatInterval, FloatInterval> {
        if (a.isEmpty || b.isEmpty) return Pair(FloatInterval.EMPTY, FloatInterval.EMPTY)
        return when (this) {
            EQ -> a.intersect(b).let { Pair(it, it) }
            NEQ -> Pair(a, b)
            LT ->
                Pair(
                    upperBound(a, b.max, inclusive = false),
                    lowerBound(b, a.min, inclusive = false),
                )
            LTE -> Pair(upperBound(a, b.max, b.maxInclusive), lowerBound(b, a.min, a.minInclusive))
            GT ->
                Pair(
                    lowerBound(a, b.min, inclusive = false),
                    upperBound(b, a.max, inclusive = false),
                )
            GTE -> Pair(lowerBound(a, b.min, b.minInclusive), upperBound(b, a.max, a.maxInclusive))
        }
    }

    /** Narrows a pair of [IntInterval] operands `(a, b)` assuming `a <this> b` holds. */
    public fun narrowInt(a: IntInterval, b: IntInterval): Pair<IntInterval, IntInterval> {
        if (a.isEmpty || b.isEmpty) return Pair(IntInterval.EMPTY, IntInterval.EMPTY)
        val minL = Int.MIN_VALUE.toLong()
        val maxL = Int.MAX_VALUE.toLong()
        return when (this) {
            EQ -> a.intersect(b).let { Pair(it, it) }
            NEQ -> Pair(a, b)
            LT ->
                Pair(
                    if (b.hasFiniteMax) {
                        a.intersect(IntInterval.fromLongBounds(minL, b.max.toLong() - 1L))
                    } else {
                        a
                    },
                    if (a.hasFiniteMin) {
                        b.intersect(IntInterval.fromLongBounds(a.min.toLong() + 1L, maxL))
                    } else {
                        b
                    },
                )
            LTE ->
                Pair(
                    if (b.hasFiniteMax) {
                        a.intersect(IntInterval.fromLongBounds(minL, b.max.toLong()))
                    } else {
                        a
                    },
                    if (a.hasFiniteMin) {
                        b.intersect(IntInterval.fromLongBounds(a.min.toLong(), maxL))
                    } else {
                        b
                    },
                )
            GT ->
                Pair(
                    if (b.hasFiniteMin) {
                        a.intersect(IntInterval.fromLongBounds(b.min.toLong() + 1L, maxL))
                    } else {
                        a
                    },
                    if (a.hasFiniteMax) {
                        b.intersect(IntInterval.fromLongBounds(minL, a.max.toLong() - 1L))
                    } else {
                        b
                    },
                )
            GTE ->
                Pair(
                    if (b.hasFiniteMin) {
                        a.intersect(IntInterval.fromLongBounds(b.min.toLong(), maxL))
                    } else {
                        a
                    },
                    if (a.hasFiniteMax) {
                        b.intersect(IntInterval.fromLongBounds(minL, a.max.toLong()))
                    } else {
                        b
                    },
                )
        }
    }

    /**
     * Evaluates `x <this> 0` across all `Long` values in `[lo, hi]`, returning [TriState.UNKNOWN]
     * if `lo > hi`.
     */
    public fun compareLongRangeAgainstZero(lo: Long, hi: Long): TriState {
        if (lo > hi) return TriState.UNKNOWN
        return when (this) {
            EQ ->
                when {
                    lo == 0L && hi == 0L -> TriState.TRUE
                    hi < 0L || lo > 0L -> TriState.FALSE
                    else -> TriState.UNKNOWN
                }
            NEQ ->
                when {
                    hi < 0L || lo > 0L -> TriState.TRUE
                    lo == 0L && hi == 0L -> TriState.FALSE
                    else -> TriState.UNKNOWN
                }
            LT ->
                when {
                    hi < 0L -> TriState.TRUE
                    lo >= 0L -> TriState.FALSE
                    else -> TriState.UNKNOWN
                }
            LTE ->
                when {
                    hi <= 0L -> TriState.TRUE
                    lo > 0L -> TriState.FALSE
                    else -> TriState.UNKNOWN
                }
            GT ->
                when {
                    lo > 0L -> TriState.TRUE
                    hi <= 0L -> TriState.FALSE
                    else -> TriState.UNKNOWN
                }
            GTE ->
                when {
                    lo >= 0L -> TriState.TRUE
                    hi < 0L -> TriState.FALSE
                    else -> TriState.UNKNOWN
                }
        }
    }

    public companion object {
        /**
         * Returns the [IntervalComparison] corresponding to [ConditionType] [wireCode], or `null`
         * for non-relational types (such as [ConditionType.TYPE_CHANGED]).
         */
        @JvmStatic
        public fun fromWireCode(wireCode: Int): IntervalComparison? =
            when (wireCode) {
                ConditionType.TYPE_EQ.toInt() -> EQ
                ConditionType.TYPE_NEQ.toInt() -> NEQ
                ConditionType.TYPE_LT.toInt() -> LT
                ConditionType.TYPE_LTE.toInt() -> LTE
                ConditionType.TYPE_GT.toInt() -> GT
                ConditionType.TYPE_GTE.toInt() -> GTE
                else -> null
            }

        private fun upperBound(x: FloatInterval, max: Float, inclusive: Boolean): FloatInterval =
            x.intersect(
                FloatInterval(
                    Float.NEGATIVE_INFINITY,
                    max,
                    minInclusive = true,
                    maxInclusive = inclusive,
                )
            )

        private fun lowerBound(x: FloatInterval, min: Float, inclusive: Boolean): FloatInterval =
            x.intersect(
                FloatInterval(
                    min,
                    Float.POSITIVE_INFINITY,
                    minInclusive = inclusive,
                    maxInclusive = true,
                )
            )
    }
}
