/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.autofill

/**
 * Represents a data object that can be filled with different types of data for autofill.
 *
 * Implementations of this interface provide a way to access data as various primitive types. If a
 * specific data type is not available or supported by the implementation, the corresponding `get`
 * method will return `null`.
 */
internal interface FillableData {
    /**
     * Retrieves the `CharSequence` (text) representation of the data.
     *
     * @return The `CharSequence` data, or `null` if none is available.
     */
    fun getCharSequence(): CharSequence? {
        return null
    }

    /**
     * Retrieves the `Boolean` representation of the data.
     *
     * @return The `Boolean` data, or `null` if none is available.
     */
    fun getBool(): Boolean? {
        return null
    }

    /**
     * Retrieves the `Int` (integer) representation of the data.
     *
     * @return The `Int` data, or `null` if none is available.
     */
    fun getInt(): Int? {
        return null
    }
}

internal expect fun FillableData(booleanValue: Boolean): FillableData

internal expect fun FillableData(charSequenceValue: CharSequence): FillableData
