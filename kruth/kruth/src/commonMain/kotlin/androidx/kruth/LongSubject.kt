/*
 * Copyright 2024 The Android Open Source Project
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

/** Propositions for [Long] subjects. */
open class LongSubject
internal constructor(
    actual: Long?,
    metadata: FailureMetadata = FailureMetadata(),
) : ComparableSubject<Long>(actual, metadata) {

    /**
     * Prepares for a check that the subject is a number within the given tolerance of an expected
     * value that will be provided in the next call in the fluent chain.
     *
     * @param tolerance an inclusive upper bound on the difference between the subject and object
     *   allowed by the check, which must be a non-negative value.
     */
    fun isWithin(tolerance: Long): TolerantLongComparison {
        return object : TolerantLongComparison() {
            override fun of(expected: Long) {
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
    }

    /**
     * Prepares for a check that the subject is a number not within the given tolerance of an
     * expected value that will be provided in the next call in the fluent chain.
     *
     * @param tolerance an exclusive lower bound on the difference between the subject and object
     *   allowed by the check, which must be a non-negative value.
     */
    fun isNotWithin(tolerance: Long): TolerantLongComparison {
        return object : TolerantLongComparison() {
            override fun of(expected: Long) {
                requireNonNull(actual) {
                    "actual value cannot be null, tolerance=$tolerance, expected=$expected"
                }
                checkTolerance(tolerance)

                if (equalWithinTolerance(actual, expected, tolerance)) {
                    failWithoutActual(
                        fact("expected not to be", expected),
                        fact("but was", actual),
                        fact("within tolerance", tolerance),
                    )
                }
            }
        }
    }

    @Deprecated(
        "Use isEqualTo instead. Long comparison is consistent with equality.",
        ReplaceWith("this.isEqualTo(other)"),
    )
    override fun isEquivalentAccordingToCompareTo(other: Long?) {
        super.isEquivalentAccordingToCompareTo(other)
    }

    private fun checkTolerance(tolerance: Long) {
        require(tolerance >= 0) { "tolerance ($tolerance) cannot be negative" }
    }

    /**
     * A partially specified check about an approximate relationship to a `long` subject using a
     * tolerance.
     */
    abstract class TolerantLongComparison internal constructor() {
        /**
         * Fails if the subject was expected to be within the tolerance of the given value but was
         * not *or* if it was expected *not* to be within the tolerance but was. The subject and
         * tolerance are specified earlier in the fluent call chain.
         */
        abstract fun of(expected: Long)

        /** @throws UnsupportedOperationException always */
        @Deprecated(
            "Not supported on TolerantLongComparison. " +
                "If you meant to compare longs, use `of(Long)` instead."
        )
        override fun equals(other: Any?): Boolean {
            throw UnsupportedOperationException(
                "If you meant to compare longs, use of(Long) instead."
            )
        }

        /** @throws UnsupportedOperationException always */
        @Deprecated("Not supported on TolerantLongComparison")
        override fun hashCode(): Int {
            throw UnsupportedOperationException("Subject.hashCode() is not supported.")
        }
    }
}
