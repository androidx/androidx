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
import androidx.glance.adaptive.core.templates.TemplateIcon
import androidx.glance.adaptive.core.templates.quickactions.QuickActionsTemplate
import androidx.glance.adaptive.core.templates.quickactions.quickActionsTemplate

/**
 * Receiver for the Workouts Quick Actions demo widget: a quick launcher of eight workouts under a
 * "Start a workout" header, with "Log activity" as the header's trailing button.
 */
class WorkoutsQuickActionsWidgetReceiver : GlanceAdaptiveWidgetReceiver() {
    override val widgetName: String = WIDGET_NAME

    override suspend fun onUpdate(context: Context) {
        GlanceAdaptiveWidgetManager(context)
            .pushUpdate(widgetName = widgetName, currentData = SampleData)
    }

    internal companion object {
        const val WIDGET_NAME = "quick_actions_workouts_widget"

        /** Eight workouts with their last sessions, under a header with a "Log activity" button. */
        val SampleData: QuickActionsTemplate = quickActionsTemplate {
            leading { icon(TemplateIcon.fromResource(R.drawable.quick_actions_glyph_run)) }
            primary { text("Start a workout") }
            secondary { text("Last: Run, 5.2 km") }
            trailing {
                iconButton(
                    TemplateIcon.fromResource(R.drawable.quick_actions_glyph_add),
                    "Log activity",
                ) {
                    onClick(QuickActionsSample.action("Log activity"))
                }
            }
            actions {
                shortcut(R.drawable.quick_actions_glyph_run, "Run", "5.2 km last")
                shortcut(R.drawable.quick_actions_glyph_walk, "Walk", "Yesterday")
                shortcut(R.drawable.quick_actions_glyph_bike, "Bike", "12 km last")
                shortcut(R.drawable.quick_actions_glyph_weights, "Weights", "Monday")
                shortcut(R.drawable.quick_actions_glyph_run, "Treadmill", "30 min")
                shortcut(R.drawable.quick_actions_glyph_walk, "Hike", "Saturday")
                shortcut(R.drawable.quick_actions_glyph_bike, "Spinning", "45 min")
                shortcut(R.drawable.quick_actions_glyph_weights, "HIIT", "15 min")
            }
        }
    }
}
