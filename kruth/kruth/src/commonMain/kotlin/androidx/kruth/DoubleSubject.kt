/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.kruth

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.simpleFact

private val NEG_ZERO_BITS: Long = (-0.0).toRawBits()

/**
 * Ensures that the given tolerance is a non-negative finite value, i.e. not [Double.NaN],
 * [Double.POSITIVE_INFINITY], or negative, including -0.0.
 */
private fun checkTolerance(tolerance: Double) {
    require(!tolerance.isNaN()) { "tolerance cannot be NaN" }
    require(tolerance >= 0.0) { "tolerance $tolerance cannot be negative" }
    require(tolerance.toRawBits() != NEG_ZERO_BITS) { "tolerance $tolerance cannot be negative" }
    require(tolerance != Double.POSITIVE_INFINITY) { "tolerance cannot be POSITIVE_INFINITY" }
}

/** Propositions for double subjects. */
class DoubleSubject
internal constructor(
    actual: Double?,
    metadata: FailureMetadata = FailureMetadata(),
) : ComparableSubject<Double>(actual, metadata = metadata) {

    abstract class TolerantDoubleComparison internal constructor() {
        /**
         * Fails if the subject was expected to be within the tolerance of the given value but was
         * not _or_ if it was expected _not_ to be within the tolerance but was. The subject and
         * tolerance are specified earlier in the fluent call chain.
         */
        abstract fun of(expected: Double)

        /** @throws UnsupportedOperationException always */
        @Deprecated(
            """equals(Any?) is not supported on TolerantDoubleComparison. If
          you meant to compare doubles, use of(Double) instead.""",
            ReplaceWith("this.of(other)"),
        )
        override fun equals(other: Any?): Boolean {
            throw UnsupportedOperationException(
                "If you meant to compare doubles, use of(Double) instead."
            )
        }

        /** @throws UnsupportedOperationException always */
        @Deprecated("hashCode() is not supported on TolerantDoubleComparison")
        override fun hashCode(): Int {
            throw UnsupportedOperationException("Subject.hashCode() is not supported.")
        }
    }

    fun isWithin(tolerance: Double): TolerantDoubleComparison {
        return object : TolerantDoubleComparison() {
            override fun of(expected: Double) {
                requireNonNull(actual) {
                    "actual value cannot be null. tolerance=$tolerance expected=$expected"
                }
                checkTolerance(tolerance)

                if (!equalWithinTolerance(actual, expected, tolerance)) {
                    failWithoutActual(
                        fact("expected", expected),
                        fact("but was", actual),
                        fact("outside tolerance", tolerance),
                    )
                }
            }
        }
    }

    fun isNotWithin(tolerance: Double): TolerantDoubleComparison {
        return object : TolerantDoubleComparison() {
            override fun of(expected: Double) {
                requireNonNull(actual) {
                    "actual value cannot be null. tolerance=$tolerance expected=$expected"
                }
                checkTolerance(tolerance)

                if (!notEqualWithinTolerance(actual, expected, tolerance)) {
                    failWithoutActual(
                        fact("expected not to be", expected),
                        fact("but was", actual),
                        fact("within tolerance", tolerance),
                    )
                }
            }
        }
    }

    /** Asserts that the subject is zero (i.e. it is either `0.0` or `-0.0`). */
    fun isZero() {
        if (actual != 0.0) {
            failWithoutActual(simpleFact("Expected zero"))
        }
    }

    /**
     * Asserts that the subject is a non-null value other than zero (i.e. it is not `0.0`, `-0.0` or
     * `null`).
     */
    fun isNonZero() {
        if (actual == null) {
            failWithoutActual(simpleFact("Expected a double other than zero"))
        } else if (actual == 0.0) {
            failWithoutActual(simpleFact("Expected not to be zero"))
        }
    }

    /** Asserts that the subject is [Double.POSITIVE_INFINITY]. */
    fun isPositiveInfinity() {
        isEqualTo(Double.POSITIVE_INFINITY)
    }

    /** Asserts that the subject is [Double.NEGATIVE_INFINITY]. */
    fun isNegativeInfinity() {
        isEqualTo(Double.NEGATIVE_INFINITY)
    }

    /** Asserts that the subject is [Double.NaN]. */
    fun isNaN() {
        isEqualTo(Double.NaN)
    }

    /**
     * Asserts that the subject is finite, i.e. not [Double.POSITIVE_INFINITY],
     * [Double.NEGATIVE_INFINITY], or [Double.NaN].
     */
    fun isFinite() {
        if (actual?.isFinite() != true) {
            failWithoutActual(simpleFact("Expected to be finite"))
        }
    }

    /**
     * Asserts that the subject is a non-null value other than [Double.NaN] (but it may be
     * [Double.POSITIVE_INFINITY] or [Double.NEGATIVE_INFINITY]).
     */
    fun isNotNaN() {
        if (actual == null) {
            failWithoutActual(simpleFact("Expected a double other than NaN"))
        } else {
            isNotEqualTo(Double.NaN)
        }
    }

    /**
     * Checks that the subject is greater than [other].
     *
     * To check that the subject is greater than *or equal to* [other], use [isAtLeast].
     */
    fun isGreaterThan(other: Int) {
        isGreaterThan(other.toDouble())
    }

    /**
     * Checks that the subject is less than [other].
     *
     * To check that the subject is less than *or equal to* [other], use [isAtMost] .
     */
    fun isLessThan(other: Int) {
        isLessThan(other.toDouble())
    }

    /**
     * Checks that the subject is less than or equal to [other].
     *
     * To check that the subject is *strictly* less than [other], use [isLessThan].
     */
    fun isAtMost(other: Int) {
        isAtMost(other.toDouble())
    }

    /**
     * Checks that the subject is greater than or equal to [other].
     *
     * To check that the subject is *strictly* greater than [other], use [isGreaterThan].
     */
    fun isAtLeast(other: Int) {
        isAtLeast(other.toDouble())
    }
}
