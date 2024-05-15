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

package androidx.compose.ui.autofill

import android.view.ViewStructure
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(26)
internal object AutofillApi26Helper {
    @RequiresApi(26)
    @DoNotInline
    fun setAutofillId(structure: ViewStructure, parent: AutofillId, virtualId: Int) =
        structure.setAutofillId(parent, virtualId)

    @RequiresApi(26)
    @DoNotInline
    fun getAutofillId(structure: ViewStructure) = structure.autofillId

    @RequiresApi(26)
    @DoNotInline
    fun setAutofillType(structure: ViewStructure, type: Int) = structure.setAutofillType(type)

    @RequiresApi(26)
    @DoNotInline
    fun setAutofillHints(structure: ViewStructure, hints: Array<String>) =
        structure.setAutofillHints(hints)

    @RequiresApi(26)
    @DoNotInline
    fun isText(value: AutofillValue) = value.isText

    @RequiresApi(26)
    @DoNotInline
    fun isDate(value: AutofillValue) = value.isDate

    @RequiresApi(26)
    @DoNotInline
    fun isList(value: AutofillValue) = value.isList

    @RequiresApi(26)
    @DoNotInline
    fun isToggle(value: AutofillValue) = value.isToggle

    @RequiresApi(26)
    @DoNotInline
    fun textValue(value: AutofillValue): CharSequence = value.textValue
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(23)
internal object AutofillApi23Helper {
    @RequiresApi(23)
    @DoNotInline
    fun newChild(structure: ViewStructure, index: Int): ViewStructure? =
        structure.newChild(index)

    @RequiresApi(23)
    @DoNotInline
    fun addChildCount(structure: ViewStructure, num: Int) =
        structure.addChildCount(num)

    @RequiresApi(23)
    @DoNotInline
    fun setId(
        structure: ViewStructure,
        id: Int,
        packageName: String?,
        typeName: String?,
        entryName: String?
    ) = structure.setId(id, packageName, typeName, entryName)

    @RequiresApi(23)
    @DoNotInline
    fun setDimens(
        structure: ViewStructure,
        left: Int,
        top: Int,
        scrollX: Int,
        scrollY: Int,
        width: Int,
        height: Int
    ) = structure.setDimens(left, top, scrollX, scrollY, width, height)
}
