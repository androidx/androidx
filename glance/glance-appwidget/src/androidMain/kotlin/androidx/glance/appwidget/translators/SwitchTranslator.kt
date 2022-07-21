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
import androidx.core.widget.RemoteViewsCompat.setSwitchThumbTintList
import androidx.core.widget.RemoteViewsCompat.setSwitchTrackTintList
import androidx.glance.appwidget.EmittableSwitch
import androidx.glance.appwidget.LayoutType
import androidx.glance.appwidget.R
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.inflateViewStub
import androidx.glance.appwidget.insertView
import androidx.glance.appwidget.setViewEnabled
import androidx.glance.appwidget.unit.CheckedUncheckedColorProvider
import androidx.glance.appwidget.unit.ResourceCheckableColorProvider

internal fun RemoteViews.translateEmittableSwitch(
    translationContext: TranslationContext,
    element: EmittableSwitch
) {

    val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        LayoutType.Swtch
    } else {
        LayoutType.SwtchBackport
    }

    val context = translationContext.context
    val viewDef = insertView(translationContext, layoutType, element.modifier)
    val textViewId: Int

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        textViewId = viewDef.mainViewId
        CompoundButtonApi31Impl.setCompoundButtonChecked(
            this,
            viewDef.mainViewId,
            element.checked
        )
        when (val thumbColors = element.colors.thumb) {
            is CheckedUncheckedColorProvider -> {
                val (day, night) = thumbColors.toDayNightColorStateList(context)
                setSwitchThumbTintList(viewDef.mainViewId, notNight = day, night = night)
            }
            is ResourceCheckableColorProvider -> {
                setSwitchThumbTintList(viewDef.mainViewId, thumbColors.resId)
            }
        }.let {}
        when (val trackColors = element.colors.track) {
            is CheckedUncheckedColorProvider -> {
                val (day, night) = trackColors.toDayNightColorStateList(context)
                setSwitchTrackTintList(viewDef.mainViewId, notNight = day, night = night)
            }
            is ResourceCheckableColorProvider -> {
                setSwitchTrackTintList(viewDef.mainViewId, trackColors.resId)
            }
        }.let {}
    } else {
        textViewId = inflateViewStub(translationContext, R.id.switchText)
        val thumbId = inflateViewStub(translationContext, R.id.switchThumb)
        val trackId = inflateViewStub(translationContext, R.id.switchTrack)
        setViewEnabled(thumbId, element.checked)
        setViewEnabled(trackId, element.checked)
        setImageViewColorFilter(thumbId, element.colors.thumb.getColor(context, element.checked))
        setImageViewColorFilter(trackId, element.colors.track.getColor(context, element.checked))
    }

    setText(
        translationContext,
        textViewId,
        element.text,
        element.style,
        maxLines = element.maxLines,
        verticalTextGravity = Gravity.CENTER_VERTICAL,
    )
    applyModifiers(translationContext.forCompoundButton(), this, element.modifier, viewDef)
}
