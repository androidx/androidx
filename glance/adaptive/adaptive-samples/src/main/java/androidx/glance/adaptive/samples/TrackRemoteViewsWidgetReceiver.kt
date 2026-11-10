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
import androidx.glance.adaptive.core.templates.ChartBar
import androidx.glance.adaptive.core.templates.TemplateIcon
import androidx.glance.adaptive.core.templates.trackTemplate

/**
 * Receiver for the Track demo widget drawn by the direct RemoteViews renderer.
 *
 * A Glance Adaptive widget declares a [widgetName] and pushes a [trackTemplate] payload via
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

        private val WeeklySteps =
            listOf(
                ChartBar(0.65f, "M"),
                ChartBar(1.0f, "T", isEmphasized = true),
                ChartBar(0.80f, "W"),
                ChartBar(1.0f, "T", isEmphasized = true),
                ChartBar(0.45f, "F"),
                ChartBar(1.0f, "S", isEmphasized = true),
                ChartBar(0.55f, "S"),
            )

        val SampleData = trackTemplate {
            leading {
                progressRing(
                    progress = 0.78f,
                    icon = TemplateIcon.fromResource(R.drawable.ic_steps),
                )
            }

            primary { text("11,056") { sensitive("--") } }

            secondary { text("Steps") }

            hero {
                barChart(bars = WeeklySteps) {
                    startLabel("7 Day Avg")
                    endLabel("13,843", icon = TemplateIcon.fromResource(R.drawable.ic_arrow_up))
                }
            }

            supporting {
                text("78%") { sensitive("--") }
                text("3,118 to go") { sensitive("--") }
                badge("Z2")
            }
        }
    }
}
