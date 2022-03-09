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
import androidx.core.widget.RemoteViewsCompat.setCompoundButtonTintList
import androidx.glance.appwidget.EmittableRadioButton
import androidx.glance.appwidget.LayoutType
import androidx.glance.appwidget.R
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.inflateViewStub
import androidx.glance.appwidget.insertView
import androidx.glance.appwidget.setViewEnabled
import androidx.glance.appwidget.unit.CheckedUncheckedColorProvider
import androidx.glance.appwidget.unit.ResourceCheckableColorProvider

// Translates a RadioButton composable to a RadioButton View.
internal fun RemoteViews.translateEmittableRadioButton(
    translationContext: TranslationContext,
    element: EmittableRadioButton
) {

    val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        LayoutType.RadioButton
    } else {
        LayoutType.RadioButtonBackport
    }

    val context = translationContext.context
    val viewDef = insertView(translationContext, layout, element.modifier)
    val textViewId: Int

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        textViewId = viewDef.mainViewId
        CompoundButtonApi31Impl.setCompoundButtonChecked(
            this,
            viewDef.mainViewId,
            element.checked
        )
        when (val colors = element.colors.radio) {
            is CheckedUncheckedColorProvider -> {
                val (day, night) = colors.toDayNightColorStateList(context)
                setCompoundButtonTintList(viewDef.mainViewId, notNight = day, night = night)
            }
            is ResourceCheckableColorProvider -> {
                setCompoundButtonTintList(viewDef.mainViewId, colors.resId)
            }
        }
    } else {
        textViewId = inflateViewStub(translationContext, R.id.radioText)
        val iconId = inflateViewStub(translationContext, R.id.radioIcon)
        setViewEnabled(iconId, element.checked)
        setImageViewColorFilter(iconId, element.colors.radio.resolve(context, element.checked))
    }

    setText(
        translationContext,
        textViewId,
        element.text,
        element.style,
        maxLines = element.maxLines,
        verticalTextGravity = Gravity.CENTER_VERTICAL,
    )
    setBoolean(viewDef.mainViewId, "setEnabled", element.enabled)
    applyModifiers(translationContext, this, element.modifier, viewDef)
}
