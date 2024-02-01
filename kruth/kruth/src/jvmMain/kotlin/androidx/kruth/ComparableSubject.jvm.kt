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
 * Platform-specific propositions for [Comparable] typed subjects.
 *
 * @param T the type of the object being tested by this [ComparableSubject]
 */
internal actual interface PlatformComparableSubject<T : Comparable<T>> {

    /** Checks that the subject is in [range]. */
    fun isIn(range: Range<T>)

    /** Checks that the subject is *not* in [range]. */
    fun isNotIn(range: Range<T>)
}

internal actual class PlatformComparableSubjectImpl<T : Comparable<T>> actual constructor(
    actual: T?,
    metadata: FailureMetadata,
) : Subject<T>(actual, metadata), PlatformComparableSubject<T> {

    override fun isIn(range: Range<T>) {
        if (requireNonNull(actual) !in range) {
            failWithoutActualInternal(fact("Expected to be in range", range))
        }
    }

    override fun isNotIn(range: Range<T>) {
        if (requireNonNull(actual) in range) {
            failWithoutActualInternal(fact("Expected not to be in range", range))
        }
    }
}
