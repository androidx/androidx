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

import androidx.kruth.Fact.Companion.fact
import com.google.common.collect.Range

/**
 * Propositions for [Comparable] typed subjects.
 *
 * @param T the type of the object being tested by this [ComparableSubject]
 * @constructor Constructor for use by subclasses. If you want to create an instance of this class
 *   itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
 */
actual open class ComparableSubject<T : Comparable<T>>
protected actual constructor(metadata: FailureMetadata, actual: T?) :
    Subject<T>(actual, metadata, typeDescriptionOverride = null) {
    internal actual constructor(actual: T?, metadata: FailureMetadata) : this(metadata, actual)

    /**
     * Checks that the subject is equivalent to [other] according to [Comparable.compareTo], (i.e.,
     * checks that `a.comparesTo(b) == 0`).
     *
     * **Note:** Do not use this method for checking object equality. Instead, use [isEqualTo].
     */
    actual open fun isEquivalentAccordingToCompareTo(other: T?) =
        commonIsEquivalentAccordingToCompareTo(other)

    /**
     * Checks that the subject is greater than [other].
     *
     * To check that the subject is greater than *or equal to* [other], use [isAtLeast].
     */
    actual fun isGreaterThan(other: T?) = commonIsGreaterThan(other)

    /**
     * Checks that the subject is less than [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    actual fun isLessThan(other: T?) = commonIsLessThan(other)

    /**
     * Checks that the subject is less than or equal to [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    actual fun isAtMost(other: T?) = commonIsAtMost(other)

    /**
     * Checks that the subject is greater than or equal to [other].
     *
     * @throws NullPointerException if [actual] or [other] is `null`.
     */
    actual fun isAtLeast(other: T?) = commonIsAtLeast(other)

    /** Checks that the subject is in [range]. */
    fun isIn(range: Range<T>) {
        if (requireNonNull(actual) !in range) {
            failWithoutActualInternal(fact("Expected to be in range", range))
        }
    }

    /** Checks that the subject is *not* in [range]. */
    fun isNotIn(range: Range<T>) {
        if (requireNonNull(actual) in range) {
            failWithoutActualInternal(fact("Expected not to be in range", range))
        }
    }
}
