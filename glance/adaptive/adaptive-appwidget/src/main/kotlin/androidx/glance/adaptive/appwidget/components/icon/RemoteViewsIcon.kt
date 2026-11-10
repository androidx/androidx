/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.appwidget.components.icon

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.ui.unit.Dp
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.components.density
import androidx.glance.adaptive.appwidget.components.setMaxSize
import androidx.glance.adaptive.appwidget.components.toPx
import androidx.glance.adaptive.core.slots.Icon

/**
 * Draws the icon for the leading slot, [size] across, tinted with the primary color.
 *
 * The glyph cannot be sized before API 31, so it wraps its drawable and is capped at [size] (see
 * [setMaxSize]): its drawable must be declared at least [size] large to be drawn at it.
 *
 * @param context Context the views are built for.
 * @param size Edge length the matrix gives the icon.
 * @return The icon's views.
 */
internal fun Icon.buildRemoteViews(context: Context, size: Dp): RemoteViews {
    val views =
        RemoteViews(context.packageName, R.layout.glance_adaptive_appwidget_remoteviews_icon)
    val iconId = R.id.glance_adaptive_appwidget_remoteviews_icon
    views.setImageViewResource(iconId, icon)
    views.setMaxSize(iconId, size.toPx(context.density))
    views.setContentDescription(iconId, contentDescription)
    return views
}
