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

import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import androidx.annotation.RequiresApi

/**
 * This class is here to ensure that the classes that use this API will get verified and can be AOT
 * compiled. It is expected that this class will soft-fail verification, but the classes which use
 * this method will pass.
 */
@RequiresApi(28)
internal object AutofillApi28Helper {
    @RequiresApi(28)
    fun setMaxTextLength(structure: ViewStructure, length: Int) = structure.setMaxTextLength(length)
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be AOT
 * compiled. It is expected that this class will soft-fail verification, but the classes which use
 * this method will pass.
 */
@RequiresApi(27)
internal object AutofillApi27Helper {
    @RequiresApi(27)
    fun notifyViewVisibilityChanged(
        view: View,
        autofillManager: AutofillManager,
        semanticsId: Int,
        isVisible: Boolean,
    ) {
        autofillManager.notifyViewVisibilityChanged(view, semanticsId, isVisible)
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be AOT
 * compiled. It is expected that this class will soft-fail verification, but the classes which use
 * this method will pass.
 */
@RequiresApi(26)
internal object AutofillApi26Helper {
    @RequiresApi(26)
    fun newChild(structure: ViewStructure, index: Int): ViewStructure = structure.newChild(index)

    @RequiresApi(26)
    fun addChildCount(structure: ViewStructure, num: Int) = structure.addChildCount(num)

    @RequiresApi(26)
    fun setId(
        structure: ViewStructure,
        id: Int,
        packageName: String?,
        typeName: String?,
        entryName: String?,
    ) = structure.setId(id, packageName, typeName, entryName)

    @RequiresApi(26)
    fun setDimens(
        structure: ViewStructure,
        left: Int,
        top: Int,
        scrollX: Int,
        scrollY: Int,
        width: Int,
        height: Int,
    ) = structure.setDimens(left, top, scrollX, scrollY, width, height)

    @RequiresApi(26) fun getAutofillId(structure: ViewStructure) = structure.autofillId

    @RequiresApi(26) fun isDate(value: AutofillValue) = value.isDate

    @RequiresApi(26) fun isList(value: AutofillValue) = value.isList

    @RequiresApi(26) fun isText(value: AutofillValue) = value.isText

    @RequiresApi(26) fun isToggle(value: AutofillValue) = value.isToggle

    @RequiresApi(26)
    fun setContentDescription(structure: ViewStructure, contentDescription: CharSequence) =
        structure.setContentDescription(contentDescription)

    @RequiresApi(26)
    fun setAutofillHints(structure: ViewStructure, hints: Array<String>) =
        structure.setAutofillHints(hints)

    @RequiresApi(26)
    fun setAutofillId(structure: ViewStructure, parent: AutofillId, virtualId: Int) =
        structure.setAutofillId(parent, virtualId)

    @RequiresApi(26)
    fun setAutofillType(structure: ViewStructure, type: Int) = structure.setAutofillType(type)

    @RequiresApi(26)
    fun setAutofillValue(structure: ViewStructure, value: AutofillValue) =
        structure.setAutofillValue(value)

    @RequiresApi(26)
    fun setCheckable(structure: ViewStructure, checkable: Boolean) =
        structure.setCheckable(checkable)

    @RequiresApi(26)
    fun setChecked(structure: ViewStructure, checked: Boolean) = structure.setChecked(checked)

    @RequiresApi(26)
    fun setChildCount(structure: ViewStructure, numChildren: Int) {
        structure.childCount = numChildren
    }

    @RequiresApi(26)
    fun setClassName(structure: ViewStructure, classname: String) =
        structure.setClassName(classname)

    @RequiresApi(26)
    fun setClickable(structure: ViewStructure, clickable: Boolean) =
        structure.setClickable(clickable)

    @RequiresApi(26)
    fun setDataIsSensitive(structure: ViewStructure, isSensitive: Boolean) =
        structure.setDataIsSensitive(isSensitive)

    @RequiresApi(26)
    fun setEnabled(structure: ViewStructure, enabled: Boolean) = structure.setEnabled(enabled)

    @RequiresApi(26)
    fun setFocusable(structure: ViewStructure, focusable: Boolean) =
        structure.setFocusable(focusable)

    @RequiresApi(26)
    fun setFocused(structure: ViewStructure, focused: Boolean) = structure.setFocused(focused)

    @RequiresApi(26)
    fun setInputType(structure: ViewStructure, type: Int) = structure.setInputType(type)

    @RequiresApi(26)
    fun setLongClickable(structure: ViewStructure, longClickable: Boolean) =
        structure.setLongClickable(longClickable)

    @RequiresApi(26)
    fun setOpaque(structure: ViewStructure, isOpaque: Boolean) = structure.setOpaque(isOpaque)

    @RequiresApi(26)
    fun setSelected(structure: ViewStructure, isSelected: Boolean) =
        structure.setSelected(isSelected)

    @RequiresApi(26)
    fun setText(structure: ViewStructure, text: CharSequence) {
        structure.text = text
    }

    @RequiresApi(26)
    fun setVisibility(structure: ViewStructure, visibility: Int) =
        structure.setVisibility(visibility)

    @RequiresApi(26) fun textValue(value: AutofillValue): CharSequence = value.textValue

    @RequiresApi(26) fun booleanValue(value: AutofillValue): Boolean = value.toggleValue

    @RequiresApi(26) fun listValue(value: AutofillValue): Int = value.listValue

    @RequiresApi(26)
    fun getAutofillTextValue(value: String): AutofillValue {
        return AutofillValue.forText(value)
    }

    @RequiresApi(26)
    fun getAutofillToggleValue(value: Boolean): AutofillValue {
        return AutofillValue.forToggle(value)
    }
}
