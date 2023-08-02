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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.template.GlanceTemplateAppWidget
import androidx.glance.appwidget.template.SingleEntityTemplate
import androidx.glance.currentState
import androidx.glance.template.SingleEntityTemplateData
import androidx.glance.template.TemplateImageWithDescription
import androidx.glance.template.TemplateText
import androidx.glance.template.TemplateTextButton
import androidx.glance.template.TextType

/**
 * Demo app widget using [SingleEntityTemplate] to define layout.
 */
class SingleEntityDemoWidget : GlanceTemplateAppWidget() {
    override val sizeMode = SizeMode.Exact

    @Composable
    override fun TemplateContent() {
        SingleEntityTemplate(
            SingleEntityTemplateData(
                header = TemplateText("Single Entity Demo", TextType.Title),
                headerIcon = TemplateImageWithDescription(
                    ImageProvider(R.drawable.compose),
                    "Header icon"
                ),
                text1 = TemplateText(
                    getTitle(currentState<Preferences>()[ToggleKey] == true), TextType.Title
                ),
                text2 = TemplateText("Subtitle", TextType.Label),
                text3 = TemplateText(
                    "Body Lorem ipsum dolor sit amet, consectetur adipiscing elit",
                    TextType.Body
                ),
                button = TemplateTextButton(
                    actionRunCallback<SEButtonAction>(),
                    "Toggle title"
                ),
                image = TemplateImageWithDescription(
                    ImageProvider(R.drawable.compose),
                    "Compose image"
                )
            )
        )
    }
}

class SingleEntityWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleEntityDemoWidget()
}

class SEButtonAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { it[ToggleKey] = it[ToggleKey] != true }
        SingleEntityDemoWidget().update(context, glanceId)
    }
}

private val ToggleKey = booleanPreferencesKey("title_toggled_key")

private fun getTitle(toggled: Boolean) = if (toggled) "Title2" else "Title1"
