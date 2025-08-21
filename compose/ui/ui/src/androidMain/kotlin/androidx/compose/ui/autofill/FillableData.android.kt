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
internal class AndroidFillableData(private val autofillValue: AutofillValue) : FillableData {

    /**
     * Returns the CharSequence data if the backing [AutofillValue] contains text, otherwise returns
     * null.
     */
    override fun getCharSequence(): CharSequence? {
        return if (autofillValue.isText) autofillValue.textValue else null
    }

    /**
     * Returns the Boolean data if the backing [AutofillValue] contains a toggle value, otherwise
     * returns null.
     */
    @Suppress("AutoBoxing")
    override fun getBool(): Boolean? {
        return if (autofillValue.isToggle) autofillValue.toggleValue else null
    }

    /**
     * Returns the Int data if the backing [AutofillValue] contains a list selection, otherwise
     * returns null.
     */
    @Suppress("AutoBoxing")
    override fun getInt(): Int? {
        return if (autofillValue.isList) autofillValue.listValue else null
    }
}

/**
 * Creates a [FillableData] instance from a [CharSequence].
 *
 * This function is used to wrap a text value for autofill purposes. On Android, it creates an
 * [AutofillValue] that contains the provided text.
 *
 * @param charSequenceValue The text data to be used for autofill.
 * @return A [FillableData] object containing the text data.
 */
@RequiresApi(Build.VERSION_CODES.O)
actual fun FillableData(charSequenceValue: CharSequence): FillableData {
    return AndroidFillableData(AutofillValue.forText(charSequenceValue))
}

/**
 * Creates a [FillableData] instance from a [Boolean].
 *
 * This function is used to wrap a boolean value for autofill purposes, such as the state of a
 * checkbox or a switch. On Android, it creates an [AutofillValue] that represents a toggle state.
 *
 * @param booleanValue The boolean data to be used for autofill.
 * @return A [FillableData] object containing the boolean data.
 */
@RequiresApi(Build.VERSION_CODES.O)
actual fun FillableData(booleanValue: Boolean): FillableData {
    return AndroidFillableData(AutofillValue.forToggle(booleanValue))
}

/**
 * Creates a [FillableData] instance from an [Int].
 *
 * This function is used to wrap an integer value for autofill purposes, such as the selected index
 * in a dropdown menu or spinner. On Android, it creates an [AutofillValue] that represents a list
 * selection.
 *
 * @param intValue The integer data to be used for autofill, representing the index of the selected
 *   item in a list.
 * @return A [FillableData] object containing the integer data.
 */
@RequiresApi(Build.VERSION_CODES.O)
actual fun FillableData(intValue: Int): FillableData {
    return AndroidFillableData(AutofillValue.forList(intValue))
}
