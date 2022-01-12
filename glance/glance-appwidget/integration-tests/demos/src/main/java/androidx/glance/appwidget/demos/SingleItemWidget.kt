/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.demos

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.template.appwidget.GlanceTemplateAppWidget
import androidx.template.template.SingleItemTemplate

/** A [SingleItemTemplate] implementation that sets the title given widget state */
class MyWidgetTemplate : SingleItemTemplate() {
    override fun getData(state: Any?): Data {
        require(state is Preferences)
        return createData(getTitle(state[PressedKey] == true))
    }
}

private val PressedKey = booleanPreferencesKey("pressedKey")

class ButtonAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Toggle the "pressed" state
        val widget = SingleItemWidget()
        widget.updateAppWidgetState<Preferences>(context, glanceId) { prefs ->
            prefs.toMutablePreferences().apply { this[PressedKey] = this[PressedKey] != true }
        }
        widget.update(context, glanceId)
    }
}

class SingleItemWidget : GlanceTemplateAppWidget(MyWidgetTemplate())

class SingleItemWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleItemWidget()
}

private fun createData(title: String) = SingleItemTemplate.Data(
    title,
    "button",
    actionRunCallback<ButtonAction>(),
    ImageProvider(R.drawable.compose),
    R.color.default_widget_background
)

private fun getTitle(pressed: Boolean) = if (pressed) "title2" else "title1"
