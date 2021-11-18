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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckBoxColors
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.Switch
import androidx.glance.appwidget.SwitchColors
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.unit.ColorProvider
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class CompoundButtonAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact
    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    @Composable
    override fun Content() {
        val size = LocalSize.current
        val toggled = size.width >= 150.dp
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Color.LightGray)
                .padding(R.dimen.external_padding).cornerRadius(R.dimen.corner_radius)
                .appWidgetBackground(),
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
                    checkedColor = ColorProvider(day = Color.Red, night = Color.Cyan),
                    uncheckedColor = ColorProvider(day = Color.Green, night = Color.Magenta)
                )
            )
            CheckBox(
                checked = toggled,
                text = "Checkbox 3",
                colors = CheckBoxColors(R.color.my_checkbox_colors)
            )
            Switch(
                checked = toggled,
                text = "Switch 1",
                colors = SwitchColors(
                    checkedThumbColor = ColorProvider(day = Color.Red, night = Color.Cyan),
                    uncheckedThumbColor = ColorProvider(day = Color.Green, night = Color.Magenta),
                    checkedTrackColor = ColorProvider(day = Color.Blue, night = Color.Yellow),
                    uncheckedTrackColor = ColorProvider(day = Color.Magenta, night = Color.Green)
                )
            )
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
    val count = prefs[countClicksKey] ?: 0
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Button(
            text = "Count clicks",
            onClick = actionRunCallback<ClickAction>()
        )
        Text(text = "$count clicks")
    }
}

class ClickAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[countClicksKey] = (prefs[countClicksKey] ?: 0) + 1
            }
        }
        CompoundButtonAppWidget().update(context, glanceId)
    }
}

private val countClicksKey = intPreferencesKey("CountClicks")

class CompoundButtonAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CompoundButtonAppWidget()
}
