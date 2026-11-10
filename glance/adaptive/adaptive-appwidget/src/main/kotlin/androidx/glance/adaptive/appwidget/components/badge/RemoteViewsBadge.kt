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

package androidx.glance.adaptive.appwidget.components.badge

import android.content.Context
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.components.addChild
import androidx.glance.adaptive.appwidget.components.density
import androidx.glance.adaptive.appwidget.components.setText
import androidx.glance.adaptive.appwidget.components.toPx
import androidx.glance.adaptive.core.slots.Badge

/**
 * Draws the badge for an action slot — the [RemoteViews] counterpart of the Glance
 * `SpecialBadgeBlock`. The outline and the dots are images colored at runtime with
 * `setColorFilter`, which works on every API level.
 *
 * @param context Context the views are built for.
 * @return The badge's views.
 */
internal fun Badge.buildRemoteViews(context: Context): RemoteViews {
    val density = context.density
    val views =
        RemoteViews(
            context.packageName,
            R.layout.glance_adaptive_appwidget_remoteviews_special_badge,
        )
    val labelId = R.id.glance_adaptive_appwidget_remoteviews_badge_label
    views.setText(labelId, label, BadgeDefaults.LabelTextSize.value)
    val horizontal = BadgeDefaults.HorizontalPadding.toPx(density)
    views.setViewPadding(labelId, horizontal, 0, horizontal, 0)
    views.setInt(
        R.id.glance_adaptive_appwidget_remoteviews_badge_outline,
        "setColorFilter",
        outlineColor,
    )

    val leadingId = R.id.glance_adaptive_appwidget_remoteviews_badge_leading_dots
    val trailingId = R.id.glance_adaptive_appwidget_remoteviews_badge_trailing_dots
    views.removeAllViews(leadingId)
    views.removeAllViews(trailingId)
    for (index in dotColors.indices) {
        val leading = index == 0
        val dot =
            RemoteViews(
                context.packageName,
                if (leading) {
                    R.layout.glance_adaptive_appwidget_remoteviews_badge_dot_leading
                } else {
                    R.layout.glance_adaptive_appwidget_remoteviews_badge_dot_trailing
                },
            )
        dot.setInt(
            R.id.glance_adaptive_appwidget_remoteviews_badge_dot,
            "setColorFilter",
            dotColors[index],
        )
        views.addChild(if (leading) leadingId else trailingId, dot, stableId = index)
    }
    return views
}
