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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.ActionRunnable
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionUpdateContent
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckBoxColors
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LocalUiKey
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.Switch
import androidx.glance.appwidget.layout.cornerRadius
import androidx.glance.appwidget.unit.ColorProvider
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.io.File

class CompoundButtonAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact
    override var stateDefinition: GlanceStateDefinition<*> = MyStateDefinition

    @Composable
    override fun Content() {
        val size = LocalSize.current
        val toggled = size.width >= 150.dp
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Color.LightGray).padding(8.dp)
                .cornerRadius(16.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            val textStyle = TextStyle(
                color = ColorProvider(day = Color.Red, night = Color.Cyan),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            )
            val fillModifier = GlanceModifier.fillMaxWidth()

            CheckBox(checked = toggled, text = "Checkbox 1")
            CheckBox(
                checked = !toggled,
                text = "Checkbox 2",
                style = textStyle,
                modifier = fillModifier,
                colors = CheckBoxColors(
                    checked = ColorProvider(
                        day = Color.Red,
                        night = Color.Cyan
                    ),
                    unchecked = ColorProvider(
                        day = Color.Green,
                        night = Color.Magenta
                    )
                )
            )
            CheckBox(
                checked = toggled,
                text = "Checkbox 3",
                colors = CheckBoxColors(R.color.my_checkbox_colors)
            )
            Switch(checked = toggled, text = "Switch 1")
            Switch(
                checked = !toggled,
                text = "Switch 2",
                style = textStyle,
                modifier = fillModifier
            )
            CountClicks()
        }
    }
}

@Composable
fun CountClicks() {
    val prefs = currentState<Preferences>()
    val count = prefs[countClicksKey]

    val parameters = actionParametersOf(uiNameKey to LocalUiKey.current)
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Button(
            text = "Count clicks",
            onClick = actionUpdateContent<ClickAction>(parameters)
        )
        Text(text = "$count clicks")
    }
}

class ClickAction : ActionRunnable {
    override suspend fun run(context: Context, parameters: ActionParameters) {
        val uiKey = requireNotNull(parameters[uiNameKey]) {
            "Add UI name to parameters, to access the view state."
        }
        GlanceState.updateValue(context, MyStateDefinition, uiKey) { prefs ->
            prefs.toMutablePreferences().apply {
                this[countClicksKey] = prefs[countClicksKey]!! + 1
            }.toPreferences()
        }
    }
}

object MyStateDefinition : GlanceStateDefinition<Preferences> {
    override fun getLocation(context: Context, fileKey: String): File =
        context.preferencesDataStoreFile(fileKey)

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> getDataStore(context: Context, fileKey: String): DataStore<T> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(fileKey) }
            .apply {
                edit { prefs ->
                    if (prefs[countClicksKey] == null) {
                        prefs[countClicksKey] = 0
                    }
                }
            } as DataStore<T>
}

private val countClicksKey = intPreferencesKey("CountClicks")
private val uiNameKey = ActionParameters.Key<String>("UiKey")

class CompoundButtonAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CompoundButtonAppWidget()
}
