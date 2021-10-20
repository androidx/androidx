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
import android.widget.RemoteViews
import androidx.glance.appwidget.LayoutSelector
import androidx.glance.appwidget.R
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.createRemoteViews
import androidx.glance.appwidget.layout.EmittableCheckBox
import androidx.glance.appwidget.setViewEnabled

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
        createRemoteViews(translationContext, layoutType, element.modifier)
    val rv = layoutDef.remoteViews
    val textViewId: Int

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        textViewId = layoutDef.mainViewId
        CompoundButtonApi31Impl.setCompoundButtonChecked(
            rv,
            layoutDef.mainViewId,
            element.checked
        )
    } else {
        textViewId = R.id.checkBoxText
        rv.setViewEnabled(R.id.checkBoxIcon, element.checked)
    }

    rv.setText(translationContext, textViewId, element.text, element.textStyle)
    applyModifiers(translationContext, rv, element.modifier, layoutDef)
    return rv
}
