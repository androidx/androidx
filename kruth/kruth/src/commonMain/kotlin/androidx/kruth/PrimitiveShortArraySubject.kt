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
 * A Subject for [Char] arrays.
 */
class PrimitiveShortArraySubject internal constructor(
    actual: ShortArray?,
    metadata: FailureMetadata = FailureMetadata(),
) : Subject<ShortArray?>(actual, metadata = metadata, typeDescriptionOverride = "array") {

    private val helper =
        HelperArraySubject(
            actual = actual,
            size = ShortArray::size,
            metadata = metadata,
        )

    /** Fails if the array is not empty (i.e. `array.size > 0`). */
    fun isEmpty() {
        helper.isEmpty()
    }

    /** Fails if the array is empty (i.e. `array.size == 0`). */
    fun isNotEmpty() {
        helper.isNotEmpty()
    }

    /**
     * Fails if the array does not have the given length.
     *
     * @throws IllegalArgumentException if [length] < 0
     */
    fun hasLength(length: Int) {
        helper.hasLength(length)
    }

    /** Converts this [PrimitiveBooleanArraySubject] to [IterableSubject].*/
    fun asList(): IterableSubject<Short> {
        requireNonNull(actual)
        return IterableSubject(actual = actual.asList(), metadata = metadata)
    }
}
