@file:OptIn(GlanceInternalApi::class)
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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier
import androidx.glance.action.Action
import androidx.glance.action.ActionModifier
import androidx.glance.action.LaunchActivityAction
import androidx.glance.layout.PaddingModifier
import androidx.glance.unit.Dp

private fun applyAction(
    rv: RemoteViews,
    action: Action,
    context: Context,
    @IdRes viewId: Int
) {
    when (action) {
        is LaunchActivityAction -> {
            val intent = Intent(context, action.activityClass)
            val pendingIntent: PendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_MUTABLE
                )
            rv.setOnClickPendingIntent(viewId, pendingIntent)
        }
        else -> throw IllegalArgumentException("Unrecognized action type.")
    }
}

private fun applyPadding(
    rv: RemoteViews,
    modifier: PaddingModifier,
    resources: Resources,
    @IdRes viewId: Int
) {
    val displayMetrics = resources.displayMetrics
    val isRtl = modifier.rtlAware &&
        resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    val start = modifier.start.toPixels(displayMetrics)
    val end = modifier.end.toPixels(displayMetrics)
    rv.setViewPadding(
        viewId,
        if (isRtl) end else start,
        modifier.top.toPixels(displayMetrics),
        if (isRtl) start else end,
        modifier.bottom.toPixels(displayMetrics),
    )
}

internal fun applyModifiers(
    context: Context,
    rv: RemoteViews,
    modifiers: Modifier,
    @IdRes viewId: Int = R.id.glanceView
) {
    modifiers.foldOut(Unit) { modifier, _ ->
        when (modifier) {
            is ActionModifier -> applyAction(rv, modifier.action, context, viewId)
            is PaddingModifier -> applyPadding(rv, modifier, context.resources, viewId)
        }
    }
}

internal fun Dp.toPixels(displayMetrics: DisplayMetrics) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics).toInt()
