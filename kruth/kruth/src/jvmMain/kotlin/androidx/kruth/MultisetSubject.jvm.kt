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

import com.google.common.collect.Multiset

/** Propositions for [Multiset] subjects. */
class MultisetSubject<T> internal constructor(
    actual: Multiset<T>?,
    metadata: FailureMetadata = FailureMetadata(),
) : IterableSubject<T>(actual, metadata) {
    private val _actual = actual

    /** Fails if the element does not have the given count.  */
    fun hasCount(element: Any?, expectedCount: Int) {
        require(expectedCount >= 0) { "expectedCount($expectedCount) must be >= 0" }
        val actualCount = requireNonNull(_actual).count(element)

        // TODO: Use check("count($element)") instead once the API is there
        check().that(actualCount).isEqualTo(expectedCount)
    }
}
