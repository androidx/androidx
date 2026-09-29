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
import androidx.glance.adaptive.core.ui.templates.TrackTemplate

/**
 * Receiver for the Track demo widget.
 *
 * A Glance Adaptive widget declares a [widgetName] and pushes a template payload via
 * [GlanceAdaptiveWidgetManager].
 */
class TrackWidgetReceiver : GlanceAdaptiveWidgetReceiver() {
    override val widgetName: String = "track_widget"

    override suspend fun onUpdate(context: Context) {
        GlanceAdaptiveWidgetManager(context)
            .pushUpdate(
                widgetName = widgetName,
                currentData =
                    TrackTemplate(
                        title = "11,056",
                        subtitle = "Steps",
                        progress = 0.55f,
                        statusText = "55%",
                    ),
            )
    }
}
