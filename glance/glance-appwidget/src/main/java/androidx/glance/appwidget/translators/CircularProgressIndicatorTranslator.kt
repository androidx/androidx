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

import android.content.res.ColorStateList
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.widget.RemoteViewsCompat.setProgressBarIndeterminateTintList
import androidx.glance.appwidget.EmittableCircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidgetTag
import androidx.glance.appwidget.LayoutType
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.insertView
import androidx.glance.color.DayNightColorProvider
import androidx.glance.unit.FixedColorProvider
import androidx.glance.unit.ResourceColorProvider

internal fun RemoteViews.translateEmittableCircularProgressIndicator(
    translationContext: TranslationContext,
    element: EmittableCircularProgressIndicator
) {
    val viewDef = insertView(translationContext,
                             LayoutType.CircularProgressIndicator,
                             element.modifier)
    setProgressBar(viewDef.mainViewId, 0, 0, true)
    if (Build.VERSION.SDK_INT >= 31) {
      when (val indicatorColor = element.color) {
        is FixedColorProvider -> {
          setProgressBarIndeterminateTintList(
            viewId = viewDef.mainViewId,
            tint = ColorStateList.valueOf(indicatorColor.color.toArgb())
          )
        }
        is ResourceColorProvider -> {
          setProgressBarIndeterminateTintList(
            viewId = viewDef.mainViewId,
            resId = indicatorColor.resId
          )
        }
        is DayNightColorProvider -> {
          setProgressBarIndeterminateTintList(
              viewId = viewDef.mainViewId,
              notNightTint = ColorStateList.valueOf(indicatorColor.day.toArgb()),
              nightTint = ColorStateList.valueOf(indicatorColor.night.toArgb())
          )
        }
        else ->
            Log.w(GlanceAppWidgetTag, "Unexpected progress indicator color: $indicatorColor")
      }
    }
    applyModifiers(translationContext, this, element.modifier, viewDef)
}
