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
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.widget.RemoteViewsCompat.setImageViewColorFilter
import androidx.glance.appwidget.unit.CheckableColorProvider
import androidx.glance.appwidget.unit.CheckedStateSet
import androidx.glance.appwidget.unit.CheckedUncheckedColorProvider
import androidx.glance.appwidget.unit.ResourceCheckableColorProvider
import androidx.glance.appwidget.unit.resolveCheckedColor
import androidx.glance.color.isNightMode

internal val checkableColorProviderFallbackColor = Color.Black

private fun CheckedUncheckedColorProvider.toColorStateList(
    context: Context,
    isNightMode: Boolean
): ColorStateList {
    return createCheckedColorStateList(
        checked = getColor(context, isNightMode, isChecked = true),
        unchecked = getColor(context, isNightMode, isChecked = false)
    )
}

internal fun CheckedUncheckedColorProvider.toDayNightColorStateList(
    context: Context
): DayNightColorStateList {
    return DayNightColorStateList(
        day = toColorStateList(context, isNightMode = false),
        night = toColorStateList(context, isNightMode = true)
    )
}

/**
 * Creates a [ColorStateList] switching between [checked] and [unchecked] depending on the checked
 * state.
 */
private fun createCheckedColorStateList(checked: Color, unchecked: Color): ColorStateList {
    return ColorStateList(
        arrayOf(CheckedStateSet, intArrayOf()),
        intArrayOf(checked.toArgb(), unchecked.toArgb())
    )
}

internal fun CheckableColorProvider.getColor(context: Context, isChecked: Boolean): Color {
    return when (this) {
        is CheckedUncheckedColorProvider -> getColor(context, context.isNightMode, isChecked)
        is ResourceCheckableColorProvider -> {
            resolveCheckedColor(context, resId, isChecked)
        }
    } ?: checkableColorProviderFallbackColor
}

/**
 * Pair class holding two [ColorStateList]s corresponding to day and night alternatives for
 * [RemoteViews] APIs that accept two [ColorStateList]s for day/night.
 */
internal data class DayNightColorStateList(val day: ColorStateList, val night: ColorStateList)

internal fun RemoteViews.setImageViewColorFilter(viewId: Int, color: Color) {
    setImageViewColorFilter(viewId, color.toArgb())
}
