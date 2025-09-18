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
 * can use the corresponding property to retrieve the value. For any given instance, only the
 * property that matches the underlying data's type will return a non-null value. All other
 * properties will return `null`.
 */
interface FillableData {
    /** The `CharSequence` (text) representation of the data, or `null` if none is available. */
    val textValue: CharSequence?
        get() = null

    /** The `Boolean` representation of the data, or `null` if none is available. */
    val booleanValue: Boolean?
        @Suppress("AutoBoxing") get() = null

    /** The `Int` (integer) representation of the data, or `null` if none is available. */
    val listIndexValue: Int?
        @Suppress("AutoBoxing") get() = null

    /** Returns the list index value if it is available, otherwise returns the [defaultValue]. */
    fun getListIndexOrDefault(defaultValue: Int): Int = listIndexValue ?: defaultValue

    /** The date in milliseconds since epoch, or `null` if none is available. */
    val dateMillisValue: Long?
        @Suppress("AutoBoxing") get() = null

    /**
     * Returns the date in milliseconds value if it is available, otherwise returns the
     * [defaultValue].
     */
    fun getDateMillisOrDefault(defaultValue: Long): Long = dateMillisValue ?: defaultValue

    companion object
}

/**
 * Creates a [FillableData] instance from a [Boolean].
 *
 * This function is used to wrap a boolean value for autofill purposes, such as the state of a
 * checkbox or a switch.
 *
 * @param booleanValue The boolean data to be used for autofill.
 * @return A [FillableData] object containing the boolean data, or `null` if the platform does not
 *   support autofill.
 */
expect fun FillableData.Companion.createFrom(booleanValue: Boolean): FillableData?

/**
 * Creates a [FillableData] instance from a [CharSequence].
 *
 * This function is used to wrap a text value for autofill purposes.
 *
 * @param textValue The text data to be used for autofill.
 * @return A [FillableData] object containing the text data, or `null` if the platform does not
 *   support autofill.
 */
expect fun FillableData.Companion.createFrom(textValue: CharSequence): FillableData?

/**
 * Creates a [FillableData] instance from an [Int].
 *
 * This function is used to wrap an integer value for autofill purposes, such as the selected index
 * in a dropdown menu or spinner.
 *
 * @param listIndexValue The integer data to be used for autofill, representing the index of the
 *   selected item in a list.
 * @return A [FillableData] object containing the integer data, or `null` if the platform does not
 *   support autofill.
 */
expect fun FillableData.Companion.createFrom(listIndexValue: Int): FillableData?

/**
 * Creates a [FillableData] instance from a [Long].
 *
 * This function is used to wrap a long value for autofill purposes, such as a date represented in
 * milliseconds since the epoch.
 *
 * @param dateMillisValue The long data to be used for autofill, representing a date in milliseconds
 *   since the epoch.
 * @return A [FillableData] object containing the long data, or `null` if the platform does not
 *   support autofill.
 */
expect fun FillableData.Companion.createFrom(dateMillisValue: Long): FillableData?
