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
    override fun getBool(): Boolean? {
        return if (autofillValue.isToggle) autofillValue.toggleValue else null
    }

    /**
     * Returns the Int data if the backing [AutofillValue] contains a list selection, otherwise
     * returns null.
     */
    override fun getInt(): Int? {
        return if (autofillValue.isList) autofillValue.listValue else null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal actual fun FillableData(charSequenceValue: CharSequence): FillableData {
    return AndroidFillableData(AutofillValue.forText(charSequenceValue))
}

@RequiresApi(Build.VERSION_CODES.O)
internal actual fun FillableData(booleanValue: Boolean): FillableData {
    return AndroidFillableData(AutofillValue.forToggle(booleanValue))
}
