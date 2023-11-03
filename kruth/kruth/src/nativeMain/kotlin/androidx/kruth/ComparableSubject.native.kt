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
actual open class ComparableSubject<T : Comparable<T>> internal actual constructor(
    actual: T?,
    metadata: FailureMetadata,
) : Subject<T>(actual = actual, metadata = metadata) {

    /**
     * Checks that the subject is equivalent to [other] according to [Comparable.compareTo],
     * (i.e., checks that `a.comparesTo(b) == 0`).
     *
     * **Note:** Do not use this method for checking object equality. Instead, use [isEqualTo].
     */
    actual open fun isEquivalentAccordingToCompareTo(other: T?) {
        isEquivalentAccordingToCompareToImpl(other)
    }

    /**
     * Checks that the subject is greater than [other].
     *
     * To check that the subject is greater than *or equal to* [other], use [isAtLeast].
     */
    actual fun isGreaterThan(other: T?) {
        isGreaterThanImpl(other)
    }

    /**
     * Checks that the subject is less than [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    actual fun isLessThan(other: T?) {
        isLessThanImpl(other)
    }

    /**
     * Checks that the subject is less than or equal to [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    actual fun isAtMost(other: T?) {
        isAtMostImpl(other)
    }

    /**
     * Checks that the subject is greater than or equal to [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    actual fun isAtLeast(other: T?) {
        isAtLeastImpl(other)
    }
}
