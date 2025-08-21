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
 * Represents a single piece of data for autofill purposes.
 *
 * An instance of `FillableData` is expected to hold a value of a single specific type. Consumers
 * can use the corresponding `get` method to retrieve the value. For any given instance, only the
 * method that matches the underlying data's type will return a non-null value. All other `get`
 * methods will return `null`.
 */
interface FillableData {
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
    @Suppress("AutoBoxing")
    fun getBool(): Boolean? {
        return null
    }

    /**
     * Retrieves the `Int` (integer) representation of the data.
     *
     * @return The `Int` data, or `null` if none is available.
     */
    @Suppress("AutoBoxing")
    fun getInt(): Int? {
        return null
    }
}

internal expect fun FillableData(booleanValue: Boolean): FillableData

internal expect fun FillableData(charSequenceValue: CharSequence): FillableData

internal expect fun FillableData(intValue: Int): FillableData
