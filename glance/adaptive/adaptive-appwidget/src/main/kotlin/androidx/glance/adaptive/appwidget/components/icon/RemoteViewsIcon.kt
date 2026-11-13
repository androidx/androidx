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
 * Draws the icon for the leading slot, [size] across.
 *
 * When [Icon.contained] is `true`, the glyph is drawn in a filled circle [size] across, tinted with
 * `onSecondaryContainer`, as the design draws the dashboard header's brand icon. When `false`, the
 * icon is the glyph alone, tinted with the primary color.
 *
 * The glyph cannot be sized before API 31, so it wraps its drawable and is capped (see
 * [setMaxSize]), and a circle is padded out around it: its drawable must be declared at least as
 * large as it is drawn.
 *
 * @param context Context the views are built for.
 * @param size Edge length the matrix gives the icon.
 * @return The icon's views.
 */
internal fun Icon.buildRemoteViews(context: Context, size: Dp): RemoteViews {
    val density = context.density
    val views =
        RemoteViews(
            context.packageName,
            if (contained) {
                R.layout.glance_adaptive_appwidget_remoteviews_icon_contained
            } else {
                R.layout.glance_adaptive_appwidget_remoteviews_icon
            },
        )
    val iconId = R.id.glance_adaptive_appwidget_remoteviews_icon
    views.setImageViewResource(iconId, icon)
    views.setContentDescription(iconId, contentDescription)
    if (!contained) {
        views.setMaxSize(iconId, size.toPx(density))
        return views
    }
    val glyphPx = (size * IconDefaults.ContainedGlyphRatio).toPx(density)
    views.setMaxSize(iconId, glyphPx)
    // The two halves can differ by a pixel, which leaves the glyph where centering it would.
    val inset = (size.toPx(density) - glyphPx).coerceAtLeast(0)
    val leading = inset / 2
    views.setViewPadding(
        R.id.glance_adaptive_appwidget_remoteviews_icon_container,
        leading,
        leading,
        inset - leading,
        inset - leading,
    )
    return views
}
