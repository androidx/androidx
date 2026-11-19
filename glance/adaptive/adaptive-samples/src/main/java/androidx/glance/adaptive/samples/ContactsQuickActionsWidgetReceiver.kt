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
 * Receiver for the Contacts Quick Actions demo widget: eight favorite contacts and no header, so
 * the tiles take the whole widget. Contacts are drawn with a person glyph; photos on tiles wait for
 * the design to settle.
 */
class ContactsQuickActionsWidgetReceiver : GlanceAdaptiveWidgetReceiver() {
    override val widgetName: String = WIDGET_NAME

    override suspend fun onUpdate(context: Context) {
        GlanceAdaptiveWidgetManager(context)
            .pushUpdate(widgetName = widgetName, currentData = SampleData)
    }

    internal companion object {
        const val WIDGET_NAME = "quick_actions_contacts_widget"

        /** Eight favorite contacts, redacted on the lock screen, with no header. */
        val SampleData: QuickActionsTemplate = quickActionsTemplate {
            actions {
                favoriteContacts().forEach { name ->
                    action(TemplateIcon.fromResource(R.drawable.quick_actions_glyph_person), name) {
                        sensitive("Contact")
                        onClick(QuickActionsSample.action(name))
                    }
                }
            }
        }

        private fun favoriteContacts(): List<String> =
            listOf(
                "Rachel",
                "Monica",
                "Joey",
                "Chandler",
                "Ross",
                "Phoebe",
                "Kiddo",
                "Grandma",
            )
    }
}
