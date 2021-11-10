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

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.view.Gravity
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.widget.setCompoundButtonTintList
import androidx.core.widget.setImageViewColorFilter
import androidx.glance.appwidget.CheckBoxColors
import androidx.glance.appwidget.EmittableCheckBox
import androidx.glance.appwidget.LayoutType
import androidx.glance.appwidget.R
import androidx.glance.appwidget.ResolvedCheckBoxColors
import androidx.glance.appwidget.ResourceCheckBoxColors
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.inflateViewStub
import androidx.glance.appwidget.insertView
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
        when (val colors = element.colors) {
            is ResolvedCheckBoxColors -> {
                setCompoundButtonTintList(viewDef.mainViewId, colors.toColorStateList())
            }
            is ResourceCheckBoxColors -> setCompoundButtonTintList(viewDef.mainViewId, colors.resId)
        }.let {}
    } else {
        val iconId = inflateViewStub(translationContext, R.id.checkBoxIcon)
        textViewId = inflateViewStub(translationContext, R.id.checkBoxText)
        setViewEnabled(iconId, element.checked)
        setImageViewColorFilter(
            iconId,
            element.colors.resolve(translationContext.context, element.checked).toArgb()
        )
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

private fun ResolvedCheckBoxColors.toColorStateList(): ColorStateList {
    return ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(checked.toArgb(), unchecked.toArgb())
    )
}

private fun CheckBoxColors.resolve(context: Context, isChecked: Boolean): Color {
    return when (this) {
        is ResolvedCheckBoxColors -> if (isChecked) checked else unchecked
        is ResourceCheckBoxColors -> {
            val colorStateList = getColorStateList(context, resId)
            return Color(
                colorStateList.getColorForState(
                    if (isChecked) CheckedStateSet else UncheckedStateSet,
                    colorStateList.defaultColor
                )
            )
        }
    }
}

private val CheckedStateSet = intArrayOf(android.R.attr.state_checked)
private val UncheckedStateSet = intArrayOf(-android.R.attr.state_checked)

private fun getColorStateList(context: Context, @ColorRes resId: Int): ColorStateList {
    if (Build.VERSION.SDK_INT >= 23) {
        return CheckBoxTranslatorApi23Impl.getColorStateList(context, resId)
    } else {
        // Must create ColorStateList in this way before API 23.
        @Suppress("DEPRECATION", "ResourceType")
        return ColorStateList.createFromXml(
            context.resources,
            context.resources.getXml(resId)
        )
    }
}

@RequiresApi(23)
private object CheckBoxTranslatorApi23Impl {
    @DoNotInline
    fun getColorStateList(context: Context, @ColorRes resId: Int): ColorStateList {
        return context.getColorStateList(resId)
    }
}
