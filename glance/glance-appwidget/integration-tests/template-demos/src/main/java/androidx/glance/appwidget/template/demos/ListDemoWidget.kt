/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget.template.demos

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.template.GlanceTemplateAppWidget
import androidx.glance.appwidget.template.ListTemplate
import androidx.glance.currentState
import androidx.glance.template.ListTemplateData
import androidx.glance.template.ListTemplateItem
import androidx.glance.template.TemplateImageButton
import androidx.glance.template.TemplateImageWithDescription
import androidx.glance.template.TemplateText
import androidx.glance.template.TemplateTextButton
import androidx.glance.unit.ColorProvider

class ListDemoWidget : GlanceTemplateAppWidget() {
    override val sizeMode = SizeMode.Exact

    @Composable
    override fun TemplateContent() {
        val state = currentState<Preferences>()
        val content = mutableListOf<ListTemplateItem>()
        for (i in 1..(state[CountKey] ?: 1)) {
            var label = "Item $i"
            if (state[ItemClickedKey] == i) {
                label = "$label (clicked)"
            }
            content.add(
                ListTemplateItem(
                    title = TemplateText(
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
                        TemplateText.Type.Title
                    ),
                    body = TemplateText(label, TemplateText.Type.Label),
                    image = TemplateImageWithDescription(
                        ImageProvider(R.drawable.ic_favorite), "List item $i image"
                    ),
                    button = TemplateImageButton(
                        actionRunCallback<ListTemplateItemAction>(
                            actionParametersOf(ClickedKey to i)
                        ),
                        TemplateImageWithDescription(ImageProvider(R.drawable.compose), "button")
                    ),
                    action = actionRunCallback<ListTemplateItemAction>(
                        actionParametersOf(ClickedKey to i)
                    ),
                )
            )
        }

        ListTemplate(
            ListTemplateData(
                header = TemplateText("List Demo", TemplateText.Type.Title),
                headerIcon = TemplateImageWithDescription(
                    ImageProvider(R.drawable.compose),
                    "Logo"
                ),
                title = TemplateText("Title", TemplateText.Type.Title),
                button = TemplateTextButton(actionRunCallback<ListButtonAction>(), "Add item"),
                listContent = content,
                backgroundColor = ColorProvider(R.color.default_widget_background)
            )
        )
    }
}

class ListDemoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ListDemoWidget()
}

class ListButtonAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            var count = prefs[CountKey] ?: 0
            if (count >= MAX_ITEMS) {
                count = 0
                if (prefs[ItemClickedKey] != 1) {
                    prefs.minusAssign(ItemClickedKey)
                }
            }
            prefs[CountKey] = ++count
        }
        ListDemoWidget().update(context, glanceId)
    }
}

class ListTemplateItemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) {
            it[ItemClickedKey] = parameters[ClickedKey] ?: -1
        }
        ListDemoWidget().update(context, glanceId)
    }
}

private val CountKey = intPreferencesKey("item_count_key")
private val ItemClickedKey = intPreferencesKey("item_clicked_key")
private val ClickedKey = ActionParameters.Key<Int>("item_clicked_key")
private const val MAX_ITEMS = 10
