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

/**
 * Propositions for [Float] subjects.
 *
 * @constructor Constructor for use by subclasses. If you want to create an instance of this class
 *   itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
 */
class FloatSubject
internal constructor(
    actual: Float?,
    metadata: FailureMetadata = FailureMetadata(),
) : ComparableSubject<Float>(metadata, actual) {

    private val asDouble = DoubleSubject(actual = actual?.toDouble(), metadata = metadata)

    /**
     * Prepares for a check that the subject is a finite number within the given tolerance of an
     * expected value that will be provided in the next call in the fluent chain.
     *
     * The check will fail if either the subject or the object is [Float.POSITIVE_INFINITY],
     * [Float.NEGATIVE_INFINITY], or [Float.NaN]. To check for those values, use
     * [isPositiveInfinity], [isNegativeInfinity], [isNaN], or (with more generality) [isEqualTo].
     *
     * The check will pass if both values are zero, even if one is `0.0f` and the other is `-0.0f.
     * Use [isEqualTo] to assert that a value is exactly `0.0f` or that it is exactly `-0.0f`.
     *
     * You can use a tolerance of `0.0f` to assert the exact equality of finite floats, but often
     * [isEqualTo] is preferable (note the different behaviours around non-finite values and
     * `-0.0f`). See the documentation on [isEqualTo] for advice on when exact equality assertions
     * are appropriate.
     *
     * @param tolerance an inclusive upper bound on the difference between the subject and object
     *   allowed by the check, which must be a non-negative finite value, i.e. not [Float.NaN],
     *   [Float.POSITIVE_INFINITY], or negative, including `-0.0f`.
     */
    fun isWithin(tolerance: Float): TolerantFloatComparison =
        object : TolerantFloatComparison() {
            override fun of(expected: Float) {
                requireNonNull(actual) {
                    "actual value cannot be null, tolerance=$tolerance, expected=$expected"
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

    /**
     * Prepares for a check that the subject is a finite number not within the given tolerance of an
     * expected value that will be provided in the next call in the fluent chain.
     *
     * The check will fail if either the subject or the object is [Float.POSITIVE_INFINITY],
     * [Float.NEGATIVE_INFINITY], or [Float.NaN]. See [isFinite], [isNotNaN], or [isNotEqualTo] for
     * checks with other behaviours.
     *
     * The check will fail if both values are zero, even if one is `0.0f` and the other is `-0.0f`.
     * Use [isNotEqualTo] for a test which fails for a value of exactly zero with one sign but
     * passes for zero with the opposite sign.
     *
     * You can use a tolerance of `0.0f` to assert the exact non-equality of finite floats, but
     * sometimes [isNotEqualTo] is preferable (note the different behaviours around non-finite
     * values and `-0.0f`).
     *
     * @param tolerance an exclusive lower bound on the difference between the subject and object
     *   allowed by the check, which must be a non-negative finite value, i.e. not [Float.NaN],
     *   [Float.POSITIVE_INFINITY], or negative, including `-0.0f`.
     */
    fun isNotWithin(tolerance: Float): TolerantFloatComparison =
        object : TolerantFloatComparison() {
            override fun of(expected: Float) {
                requireNonNull(actual) {
                    "actual value cannot be null, tolerance=$tolerance, expected=$expected"
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

    /**
     * Asserts that the subject is exactly equal to the given value, with equality defined as by
     * [Float.equals]. This method is *not* recommended when the code under test is doing any kind
     * of arithmetic: use [isWithin] with a suitable tolerance in that case. (Remember that the
     * exact result of floating point arithmetic is sensitive to apparently trivial changes such as
     * replacing `(a + b) + c` with `a + (b + c)`, and that unless `strictfp` is in force even the
     * result of `(a + b) + c` is sensitive to the JVM's choice of precision for the intermediate
     * result.) This method is recommended when the code under test is specified as either copying a
     * value without modification from its input or returning a well-defined literal or constant
     * value.
     *
     * **Note:** The assertion `isEqualTo(0.0f)` fails for an input of `-0.0f`, and vice versa. For
     * an assertion that passes for either `0.0f` or `-0.0f`, use [isZero].
     */
    override fun isEqualTo(expected: Any?) {
        super.isEqualTo(expected)
    }

    /**
     * Asserts that the subject is not exactly equal to the given value, with equality defined as by
     * [Float.equals]. See [isEqualTo] for advice on when exact equality is recommended. Use
     * [isNotWithin] for an assertion with a tolerance.
     *
     * **Note:** The assertion `isNotEqualTo(0.0f)` passes for `-0.0f`, and vice versa. For an
     * assertion that fails for either `0.0f` or `-0.0f`, use [isNonZero].
     */
    override fun isNotEqualTo(unexpected: Any?) {
        super.isNotEqualTo(unexpected)
    }

    @Deprecated(
        "Use isEqualTo instead. Long comparison is consistent with equality.",
        ReplaceWith("this.isEqualTo(other)"),
    )
    override fun isEquivalentAccordingToCompareTo(other: Float?) {
        super.isEquivalentAccordingToCompareTo(other)
    }

    /** Asserts that the subject is zero (i.e. it is either `0.0f` or `-0.0f`). */
    fun isZero() {
        if (actual != 0.0f) {
            failWithActual(simpleFact("expected zero"))
        }
    }

    /**
     * Asserts that the subject is a non-null value other than zero (i.e. it is not `0.0f`, `-0.0f`
     * or `null`).
     */
    fun isNonZero() {
        when (actual) {
            null -> failWithActual(simpleFact("expected a float other than zero"))
            0.0f -> failWithActual(simpleFact("expected not to be zero"))
        }
    }

    /** Asserts that the subject is [Float.POSITIVE_INFINITY]. */
    fun isPositiveInfinity() {
        isEqualTo(Float.POSITIVE_INFINITY)
    }

    /** Asserts that the subject is [Float.NEGATIVE_INFINITY]. */
    fun isNegativeInfinity() {
        isEqualTo(Float.NEGATIVE_INFINITY)
    }

    /** Asserts that the subject is [Float.NaN]. */
    fun isNaN() {
        isEqualTo(Float.NaN)
    }

    /**
     * Asserts that the subject is finite, i.e. not [Float.POSITIVE_INFINITY],
     * [Float.NEGATIVE_INFINITY], or [Float.NaN].
     */
    fun isFinite() {
        if ((actual == null) || actual.isNaN() || actual.isInfinite()) {
            failWithActual(simpleFact("expected to be finite"))
        }
    }

    /**
     * Asserts that the subject is a non-null value other than [Float.NaN] (but it may be
     * [Float.POSITIVE_INFINITY] or [Float.NEGATIVE_INFINITY]).
     */
    fun isNotNaN() {
        if (actual == null) {
            failWithActual(simpleFact("expected a float other than NaN"))
        } else {
            isNotEqualTo(Float.NaN)
        }
    }

    /**
     * Checks that the subject is greater than [other].
     *
     * To check that the subject is greater than *or equal to* [other], use [isAtLeast].
     */
    fun isGreaterThan(other: Int) {
        asDouble.isGreaterThan(other)
    }

    /**
     * Checks that the subject is less than [other].
     *
     * To check that the subject is less than *or equal to* [other], use [isAtMost].
     */
    fun isLessThan(other: Int) {
        asDouble.isLessThan(other)
    }

    /**
     * Checks that the subject is less than or equal to [other].
     *
     * To check that the subject is *strictly* less than `other`, use [isLessThan].
     */
    fun isAtMost(other: Int) {
        asDouble.isAtMost(other)
    }

    /**
     * Checks that the subject is greater than or equal to [other].
     *
     * To check that the subject is *strictly* greater than [other], use [isGreaterThan].
     */
    fun isAtLeast(other: Int) {
        asDouble.isAtLeast(other)
    }

    abstract class TolerantFloatComparison internal constructor() {
        /**
         * Fails if the subject was expected to be within the tolerance of the given value but was
         * not _or_ if it was expected _not_ to be within the tolerance but was. The subject and
         * tolerance are specified earlier in the fluent call chain.
         */
        abstract fun of(expected: Float)

        /** @throws UnsupportedOperationException always */
        @Deprecated(
            "Not supported on TolerantDoubleComparison. " +
                "If you meant to compare doubles, use of(Double) instead.",
        )
        override fun equals(other: Any?): Boolean {
            throw UnsupportedOperationException(
                "If you meant to compare doubles, use of(Double) instead."
            )
        }

        /** @throws UnsupportedOperationException always */
        @Deprecated("Not supported on TolerantFloatComparison")
        override fun hashCode(): Int {
            throw UnsupportedOperationException("Subject.hashCode() is not supported.")
        }
    }
}

private val NEG_ZERO_BITS = (-0F).toRawBits()

/**
 * Ensures that the given tolerance is a non-negative finite value, i.e. not `Float.NaN`,
 * `Float.POSITIVE_INFINITY`, or negative, including `-0.0f`.
 */
private fun checkTolerance(tolerance: Float) {
    require(!tolerance.isNaN()) { "Tolerance cannot be NaN" }
    require(tolerance >= 0.0f) { "Tolerance ($tolerance) cannot be negative" }
    require(tolerance.toBits() != NEG_ZERO_BITS) { "Tolerance ($tolerance) cannot be negative" }
    require(tolerance != Float.POSITIVE_INFINITY) { "Tolerance cannot be POSITIVE_INFINITY" }
}
