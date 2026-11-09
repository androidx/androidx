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
import androidx.glance.adaptive.appwidget.matrix.IconButton

/**
 * Draws the icon button as a circle [diameter] across, with its glyph [glyphSize] across at the
 * center. It centers itself in its slot.
 *
 * The glyph cannot be sized before API 31, so it wraps its drawable and is capped at [glyphSize],
 * and the circle is padded out around it: the glyph's drawable must be declared at least
 * [glyphSize] large for the circle to reach [diameter].
 *
 * A button that ignores taps is given no click, since any click makes a view clickable. The matrix
 * renderer adds a tappable button under a stable ID apart from an inert one (see
 * [tappableStableId]), so the host never recycles one into the other.
 *
 * @param taps Where the button's tap goes, and the context its views are built for.
 * @param diameter Diameter of the circle.
 * @param glyphSize Edge length of the glyph.
 * @return The button's views.
 */
internal fun IconButton.buildRemoteViews(
    taps: RemoteViewsActions,
    diameter: Dp,
    glyphSize: Dp,
): RemoteViews {
    val context = taps.context
    val density = context.density
    val views =
        RemoteViews(context.packageName, R.layout.glance_adaptive_appwidget_remoteviews_icon_button)
    val buttonId = R.id.glance_adaptive_appwidget_remoteviews_icon_button
    val glyphId = R.id.glance_adaptive_appwidget_remoteviews_icon_button_glyph

    views.setImageViewResource(glyphId, icon)
    val glyphPx = glyphSize.toPx(density)
    views.setMaxSize(glyphId, glyphPx)
    // The two halves can differ by a pixel, which leaves the glyph where centering it would.
    val inset = (diameter.toPx(density) - glyphPx).coerceAtLeast(0)
    val leading = inset / 2
    views.setViewPadding(buttonId, leading, leading, inset - leading, inset - leading)

    views.setContentDescription(buttonId, contentDescription)
    taps.pendingIntentFor(action)?.let { views.setOnClickPendingIntent(buttonId, it) }
    return views
}
