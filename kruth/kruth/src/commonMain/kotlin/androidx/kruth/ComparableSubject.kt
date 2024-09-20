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
 * @constructor Constructor for use by subclasses. If you want to create an instance of this class
 *   itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
 */
expect open class ComparableSubject<T : Comparable<T>>
protected constructor(
    metadata: FailureMetadata,
    actual: T?,
) : Subject<T> {

    internal constructor(actual: T?, metadata: FailureMetadata)

    /**
     * Checks that the subject is equivalent to [other] according to [Comparable.compareTo], (i.e.,
     * checks that `a.comparesTo(b) == 0`).
     *
     * **Note:** Do not use this method for checking object equality. Instead, use [isEqualTo].
     */
    open fun isEquivalentAccordingToCompareTo(other: T?)

    /**
     * Checks that the subject is greater than [other].
     *
     * To check that the subject is greater than *or equal to* [other], use [isAtLeast].
     */
    fun isGreaterThan(other: T?)

    /**
     * Checks that the subject is less than [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    fun isLessThan(other: T?)

    /**
     * Checks that the subject is less than or equal to [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    fun isAtMost(other: T?)

    /**
     * Checks that the subject is greater than or equal to [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    fun isAtLeast(other: T?)
}

internal fun <T : Comparable<T>> ComparableSubject<T>.commonIsEquivalentAccordingToCompareTo(
    other: T?
) {
    requireNonNull(actual)
    requireNonNull(other)

    if (actual.compareTo(other) != 0) {
        failWithActualInternal("expected value that sorts equal to", other)
    }
}

internal fun <T : Comparable<T>> ComparableSubject<T>.commonIsGreaterThan(other: T?) {
    requireNonNull(actual)
    requireNonNull(other)

    if (actual <= other) {
        failWithActualInternal("expected to be greater than", other)
    }
}

internal fun <T : Comparable<T>> ComparableSubject<T>.commonIsLessThan(other: T?) {
    requireNonNull(actual)
    requireNonNull(other)

    if (actual >= other) {
        failWithActualInternal("expected to be less than", other)
    }
}

internal fun <T : Comparable<T>> ComparableSubject<T>.commonIsAtMost(other: T?) {
    requireNonNull(actual)
    requireNonNull(other)

    if (actual > other) {
        failWithActualInternal("expected to be at most", other)
    }
}

internal fun <T : Comparable<T>> ComparableSubject<T>.commonIsAtLeast(other: T?) {
    requireNonNull(actual)
    requireNonNull(other)

    if (actual < other) {
        failWithActualInternal("expected to be at least", other)
    }
}
