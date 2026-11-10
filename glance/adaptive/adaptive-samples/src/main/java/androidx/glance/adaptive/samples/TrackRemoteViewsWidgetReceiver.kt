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

package androidx.glance.adaptive.samples

import android.content.Context
import androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetReceiver
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.TrackTemplate

/**
 * Receiver for the Track demo widget drawn by the direct RemoteViews renderer.
 *
 * A Glance Adaptive widget declares a [widgetName] and pushes a template payload via
 * [GlanceAdaptiveWidgetManager].
 */
class TrackRemoteViewsWidgetReceiver : GlanceAdaptiveWidgetReceiver() {
    override val widgetName: String = WIDGET_NAME

    override suspend fun onUpdate(context: Context) {
        GlanceAdaptiveWidgetManager(context)
            .pushUpdate(widgetName = widgetName, currentData = SampleData)
    }

    internal companion object {
        const val WIDGET_NAME = "track_remoteviews_widget"

        val SampleData =
            TrackTemplate(
                leading = Ring(0.55f),
                primary = TemplateText("11,056"),
                secondary = TemplateText("Steps"),
                supporting = listOf(TemplateText("55%")),
            )
    }
}
