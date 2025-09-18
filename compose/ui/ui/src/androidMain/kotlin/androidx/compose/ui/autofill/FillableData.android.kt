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

import android.os.Build
import android.view.autofill.AutofillValue
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
internal class AndroidFillableData(internal val autofillValue: AutofillValue) : FillableData {

    /** The CharSequence data if the backing [AutofillValue] contains text, otherwise null. */
    override val textValue: CharSequence?
        get() = if (autofillValue.isText) autofillValue.textValue else null

    /** The Boolean data if the backing [AutofillValue] contains a toggle value, otherwise null. */
    override val booleanValue: Boolean?
        @Suppress("AutoBoxing")
        get() = if (autofillValue.isToggle) autofillValue.toggleValue else null

    /** The Int data if the backing [AutofillValue] contains a list selection, otherwise null. */
    override val listIndexValue: Int?
        @Suppress("AutoBoxing") get() = if (autofillValue.isList) autofillValue.listValue else null

    /**
     * Returns the list index value if it is available, otherwise returns the [defaultValue].
     *
     * @param defaultValue The value to return if the backing [AutofillValue] does not represent a
     *   list selection.
     * @return The list index if available, or [defaultValue] otherwise.
     */
    override fun getListIndexOrDefault(defaultValue: Int): Int {
        if (autofillValue.isList) {
            return autofillValue.listValue
        }
        return defaultValue
    }

    /** The Long data if the backing [AutofillValue] contains a date value, otherwise null. */
    override val dateMillisValue: Long?
        @Suppress("AutoBoxing") get() = if (autofillValue.isDate) autofillValue.dateValue else null
}

/**
 * Creates a [FillableData] instance from a [CharSequence].
 *
 * This function is used to wrap a text value for autofill purposes. On Android, it creates an
 * [AutofillValue] that contains the provided text.
 *
 * @param textValue The text data to be used for autofill.
 * @return A [FillableData] object containing the text data, or `null` if the platform version is
 *   lower than [Build.VERSION_CODES.O].
 */
actual fun FillableData.Companion.createFrom(textValue: CharSequence): FillableData? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AndroidFillableData(AutofillValue.forText(textValue))
    } else null
}

/**
 * Creates a [FillableData] instance from a [Boolean].
 *
 * This function is used to wrap a boolean value for autofill purposes, such as the state of a
 * checkbox or a switch. On Android, it creates an [AutofillValue] that represents a toggle state.
 *
 * @param booleanValue The boolean data to be used for autofill.
 * @return A [FillableData] object containing the boolean data, or `null` if the platform version is
 *   lower than [Build.VERSION_CODES.O].
 */
actual fun FillableData.Companion.createFrom(booleanValue: Boolean): FillableData? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AndroidFillableData(AutofillValue.forToggle(booleanValue))
    } else null
}

/**
 * Creates a [FillableData] instance from an [Int].
 *
 * This function is used to wrap an integer value for autofill purposes, such as the selected index
 * in a dropdown menu or spinner. On Android, it creates an [AutofillValue] that represents a list
 * selection.
 *
 * @param listIndexValue The integer data to be used for autofill, representing the index of the
 *   selected item in a list.
 * @return A [FillableData] object containing the integer data, or `null` if the platform version is
 *   lower than [Build.VERSION_CODES.O].
 */
actual fun FillableData.Companion.createFrom(listIndexValue: Int): FillableData? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AndroidFillableData(AutofillValue.forList(listIndexValue))
    } else null
}

/**
 * Creates a [FillableData] instance from a [Long].
 *
 * This function is used to wrap a long value for autofill purposes, such as a date represented in
 * milliseconds since the epoch. On Android, it creates an [AutofillValue] that represents a date.
 *
 * @param dateMillisValue The long data to be used for autofill, representing a date in
 *   milliseconds.
 * @return A [FillableData] object containing the long data, or `null` if the platform version is
 *   lower than [Build.VERSION_CODES.O].
 */
actual fun FillableData.Companion.createFrom(dateMillisValue: Long): FillableData? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AndroidFillableData(AutofillValue.forDate(dateMillisValue))
    } else null
}

/**
 * Creates a [FillableData] from the platform [AutofillValue] type.
 *
 * @param autofillValue The platform autofill value to create the [FillableData] from.
 * @return A [FillableData] object containing the platform autofill data, or `null` if the platform
 *   version is lower than [Build.VERSION_CODES.O].
 */
fun FillableData.Companion.createFrom(autofillValue: AutofillValue): FillableData? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AndroidFillableData(autofillValue)
    } else null
}

/**
 * Retrieves the underlying platform [AutofillValue] from the [FillableData].
 *
 * @return The platform [AutofillValue], or `null` if the [FillableData] is not an instance of
 *   [AndroidFillableData] or the platform version is lower than [Build.VERSION_CODES.O].
 */
fun FillableData.toAutofillValue(): AutofillValue? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        (this as? AndroidFillableData)?.autofillValue
    } else null
}
