/*
 * Copyright 2022 The Android Open Source Project
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

/**
 * Propositions for [Comparable] typed subjects.
 *
 * @param T the type of the object being tested by this [ComparableSubject]
 */
open class ComparableSubject<T : Comparable<T>> internal constructor(
    actual: T?,
    metadata: FailureMetadata = FailureMetadata(),
) : Subject<T>(actual = actual, metadata = metadata) {

    /**
     * Checks that the subject is equivalent to [expected] according to [Comparable.compareTo],
     * (i.e., checks that `a.comparesTo(b) == 0`).
     *
     * **Note:** Do not use this method for checking object equality. Instead, use [isEqualTo].
     */
    fun isEquivalentAccordingToCompareTo(expected: T?) {
        requireNonNull(actual)
        requireNonNull(expected)

        asserter(withActual = true).assertEquals(
            expected = 0,
            actual = actual.compareTo(expected),
            message = "Expected value that sorts equal to: $expected",
        )
    }

    /**
     * Checks that the subject is greater than [other].
     *
     * To check that the subject is greater than *or equal to* [other], use [isAtLeast].
     */
    fun isGreaterThan(other: T?) {
        requireNonNull(actual)
        requireNonNull(other)

        asserter(withActual = true).assertTrue(
            actual > other,
            message = "Expected to be greater than: $other",
        )
    }

    /**
     * Checks that the subject is less than [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    fun isLessThan(other: T?) {
        requireNonNull(actual) { "Expected to be less than $other, but was $actual" }
        requireNonNull(other) { "Expected to be less than $other, but was $actual" }

        if (actual >= other) {
            asserter.fail("Expected to be less than $other, but was $actual")
        }
    }

    /**
     * Checks that the subject is less than or equal to [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    fun isAtMost(other: T?) {
        requireNonNull(actual) { "Expected to be at most $other, but was $actual" }
        requireNonNull(other) { "Expected to be at most $other, but was $actual" }
        if (actual > other) {
            asserter.fail("Expected to be at most $other, but was $actual")
        }
    }

    /**
     * Checks that the subject is greater than or equal to [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    fun isAtLeast(other: T?) {
        requireNonNull(actual) { "Expected to be at least $other, but was $actual" }
        requireNonNull(other) { "Expected to be at least $other, but was $actual" }
        if (actual < other) {
            asserter.fail("Expected to be at least $other, but was $actual")
        }
    }
}