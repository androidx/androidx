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

import androidx.kruth.Fact.Companion.simpleFact

internal class HelperArraySubject<out T>(
    actual: T?,
    private val size: (T) -> Int,
    metadata: FailureMetadata = FailureMetadata(),
) : Subject<T>(actual, metadata = metadata, typeDescriptionOverride = null) {

    /** Fails if the array is not empty (i.e. `array.size > 0`). */
    fun isEmpty() {
        requireNonNull(actual)
        if (size(actual) > 0) {
            failWithActual(simpleFact("expected to be empty"))
        }
    }

    /** Fails if the array is empty (i.e. `array.size == 0`). */
    fun isNotEmpty() {
        requireNonNull(actual)
        if (size(actual) == 0) {
            failWithoutActual(simpleFact("expected not to be empty"))
        }
    }

    /**
     * Fails if the array does not have the given length.
     *
     * @throws IllegalArgumentException if [length] < 0
     */
    fun hasLength(length: Int) {
        require(length >= 0) { "length ($length) must be >= 0" }
        requireNonNull(actual)
        check("length").that(size(actual)).isEqualTo(length)
    }
}
