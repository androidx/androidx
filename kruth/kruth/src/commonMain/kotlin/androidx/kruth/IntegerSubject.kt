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

/**
 * Propositions for [Int] subjects.
 *
 * @constructor Constructor for use by subclasses. If you want to create an instance of this class
 * itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
 */
open class IntegerSubject protected constructor(
    metadata: FailureMetadata,
    actual: Int?,
) : ComparableSubject<Int>(metadata, actual) {

    internal constructor(actual: Int?, metadata: FailureMetadata) : this(metadata, actual)

    @Deprecated(
        "Use .isEqualTo instead. Long comparison is consistent with equality.",
        ReplaceWith("this.isEqualTo(other)")
    )
    override fun isEquivalentAccordingToCompareTo(other: Int?) {
        super.isEquivalentAccordingToCompareTo(other)
    }
}
