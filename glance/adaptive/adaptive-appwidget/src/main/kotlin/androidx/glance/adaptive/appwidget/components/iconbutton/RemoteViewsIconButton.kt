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

package androidx.glance.adaptive.appwidget.components.iconbutton

import android.widget.RemoteViews
import androidx.compose.ui.unit.Dp
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.components.RemoteViewsActions
import androidx.glance.adaptive.appwidget.components.density
import androidx.glance.adaptive.appwidget.components.setMaxSize
import androidx.glance.adaptive.appwidget.components.tappableStableId
import androidx.glance.adaptive.appwidget.components.toPx
import androidx.glance.adaptive.core.slots.IconButton

/**
 * Draws the icon button as a circle [diameter] across, or as a pill [width] wide and [diameter]
 * tall when [width] is larger, with its glyph [glyphSize] across at the center. It centers itself
 * in its slot.
 *
 * The glyph cannot be sized before API 31, so it wraps its drawable and is capped at [glyphSize],
 * and the shape is padded out around it: the glyph's drawable must be declared at least [glyphSize]
 * large for the shape to reach its size.
 *
 * A button that ignores taps is given no click, since any click makes a view clickable. The matrix
 * renderer adds a tappable button under a stable ID apart from an inert one (see
 * [tappableStableId]), so the host never recycles one into the other.
 *
 * @param taps Where the button's tap goes, and the context its views are built for.
 * @param diameter Height of the button, and its width when it is a circle.
 * @param glyphSize Edge length of the glyph.
 * @param width Width of the button; a pill when larger than [diameter].
 * @return The button's views.
 */
internal fun IconButton.buildRemoteViews(
    taps: RemoteViewsActions,
    diameter: Dp,
    glyphSize: Dp,
    width: Dp = diameter,
): RemoteViews {
    val context = taps.context
    val density = context.density
    val views =
        RemoteViews(context.packageName, R.layout.glance_adaptive_appwidget_remoteviews_icon_button)
    val buttonId = R.id.glance_adaptive_appwidget_remoteviews_icon_button
    val glyphId = R.id.glance_adaptive_appwidget_remoteviews_icon_button_glyph
    val pill = width > diameter
    // Set either way, as the host reapplies an update onto the previous hierarchy.
    views.setInt(
        buttonId,
        "setBackgroundResource",
        if (pill) {
            R.drawable.glance_adaptive_appwidget_remoteviews_button_pill
        } else {
            R.drawable.glance_adaptive_appwidget_remoteviews_button_circle
        },
    )

    views.setImageViewResource(glyphId, icon)
    val glyphPx = glyphSize.toPx(density)
    views.setMaxSize(glyphId, glyphPx)
    // The two halves can differ by a pixel, which leaves the glyph where centering it would.
    val horizontal = (width.toPx(density) - glyphPx).coerceAtLeast(0)
    val vertical = (diameter.toPx(density) - glyphPx).coerceAtLeast(0)
    views.setViewPadding(
        buttonId,
        horizontal / 2,
        vertical / 2,
        horizontal - horizontal / 2,
        vertical - vertical / 2,
    )

    views.setContentDescription(buttonId, contentDescription)
    taps.pendingIntentFor(action)?.let { views.setOnClickPendingIntent(buttonId, it) }
    return views
}
