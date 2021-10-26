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

package androidx.glance.appwidget.translators

import android.os.Build
import android.view.Gravity
import android.widget.RemoteViews
import androidx.glance.appwidget.LayoutType
import androidx.glance.appwidget.R
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.inflateViewStub
import androidx.glance.appwidget.insertView
import androidx.glance.appwidget.layout.EmittableCheckBox
import androidx.glance.appwidget.setViewEnabled

internal fun RemoteViews.translateEmittableCheckBox(
    translationContext: TranslationContext,
    element: EmittableCheckBox
) {

    val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        LayoutType.CheckBox
    } else {
        LayoutType.CheckBoxBackport
    }

    val viewDef = insertView(translationContext, layoutType, element.modifier)
    val textViewId: Int

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        textViewId = viewDef.mainViewId
        CompoundButtonApi31Impl.setCompoundButtonChecked(
            this,
            viewDef.mainViewId,
            element.checked
        )
    } else {
        val iconId = inflateViewStub(translationContext, R.id.checkBoxIcon)
        textViewId = inflateViewStub(translationContext, R.id.checkBoxText)
        setViewEnabled(iconId, element.checked)
    }

    setText(
        translationContext,
        textViewId,
        element.text,
        element.style,
        verticalTextGravity = Gravity.CENTER_VERTICAL,
    )
    applyModifiers(translationContext, this, element.modifier, viewDef)
}
