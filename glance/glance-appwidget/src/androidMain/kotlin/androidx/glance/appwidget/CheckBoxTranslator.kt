/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.layout.EmittableCheckBox

internal fun translateEmittableCheckBox(
    translationContext: TranslationContext,
    element: EmittableCheckBox
): RemoteViews {

    val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        LayoutSelector.Type.CheckBox
    } else {
        LayoutSelector.Type.CheckBoxBackport
    }

    val layoutDef =
        selectLayout(layoutType, element.modifier, translationContext.sizeContext)
    val rv = remoteViews(translationContext, layoutDef.layoutId)
    val textViewId: Int

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        textViewId = layoutDef.mainViewId
        CheckBoxTranslatorApi31Impl.setCompoundButtonChecked(
            rv,
            layoutDef.mainViewId,
            element.checked
        )
    } else {
        textViewId = R.id.checkBoxText
        // Special case for needing the reflection method. View.setEnabled is only safe on
        // TextViews from API 24, but is safe for other views before that, which is why
        // RemoteViews.kt sets required api 24 on setViewEnabled.
        rv.setBoolean(R.id.checkBoxIcon, "setEnabled", element.checked)
    }

    rv.setText(translationContext.context, textViewId, element.text, element.textStyle)
    applyModifiers(translationContext, rv, element.modifier, layoutDef)
    return rv
}

@RequiresApi(Build.VERSION_CODES.S)
private object CheckBoxTranslatorApi31Impl {
    @DoNotInline
    fun setCompoundButtonChecked(rv: RemoteViews, viewId: Int, checked: Boolean) {
        rv.setCompoundButtonChecked(viewId, checked)
    }
}