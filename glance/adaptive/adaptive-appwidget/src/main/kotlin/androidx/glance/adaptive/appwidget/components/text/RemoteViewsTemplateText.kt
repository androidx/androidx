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

package androidx.glance.adaptive.appwidget.components.text

import android.content.Context
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.components.setText
import androidx.glance.adaptive.core.slots.TemplateText

/**
 * Draws this text as a supporting value, for an action slot — the [RemoteViews] counterpart of the
 * Glance `SupportingValueBlock`. Every supporting value has the same style: the primary color at
 * 70% opacity, truncated to a single line.
 *
 * @param context Context the views are built for.
 * @return The value's views.
 */
internal fun TemplateText.buildSupportingRemoteViews(context: Context): RemoteViews {
    val views =
        RemoteViews(
            context.packageName,
            R.layout.glance_adaptive_appwidget_remoteviews_supporting_value,
        )
    views.setText(
        R.id.glance_adaptive_appwidget_remoteviews_supporting_value,
        value,
        TemplateTextDefaults.SupportingTextSize.value,
    )
    return views
}
