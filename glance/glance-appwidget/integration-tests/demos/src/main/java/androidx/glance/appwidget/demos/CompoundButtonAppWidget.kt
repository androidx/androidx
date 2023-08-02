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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckBoxColors
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.RadioButton
import androidx.glance.appwidget.RadioButtonColors
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.Switch
import androidx.glance.appwidget.SwitchColors
import androidx.glance.appwidget.selectableGroup
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ToggleableStateKey
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
import androidx.glance.layout.height
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle

class CompoundButtonAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    enum class Buttons {
        CHECK_1, CHECK_2, CHECK_3, SWITCH_1, SWITCH_2, RADIO_1, RADIO_2, RADIO_3;
        val prefsKey = booleanPreferencesKey(name)
    }

    enum class Radios {
        RADIO_1, RADIO_2, RADIO_3;
        val prefsKey = booleanPreferencesKey(name)
    }

    @Composable
    override fun Content() {
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

            val prefs = currentState<Preferences>()
            val checkbox1Checked = prefs[Buttons.CHECK_1.prefsKey] ?: false
            val checkbox2Checked = prefs[Buttons.CHECK_2.prefsKey] ?: false
            val checkbox3Checked = prefs[Buttons.CHECK_3.prefsKey] ?: false
            val switch1Checked = prefs[Buttons.SWITCH_1.prefsKey] ?: false
            val switch2Checked = prefs[Buttons.SWITCH_2.prefsKey] ?: false
            val radio1Checked = prefs[Buttons.RADIO_1.prefsKey] ?: false
            val radio2Checked = prefs[Buttons.RADIO_2.prefsKey] ?: false
            val radio3Checked = prefs[Buttons.RADIO_3.prefsKey] ?: false

            CheckBox(
                checked = checkbox1Checked,
                onCheckedChange = actionRunCallback<ToggleAction>(
                    actionParametersOf(EventTargetKey to Buttons.CHECK_1.name)
                ),
                text = "Checkbox 1",
                modifier = GlanceModifier.height(56.dp).padding(bottom = 24.dp),
            )
            CheckBox(
                checked = checkbox2Checked,
                onCheckedChange = actionRunCallback<ToggleAction>(
                    actionParametersOf(EventTargetKey to Buttons.CHECK_2.name)
                ),
                text = "Checkbox 2",
                style = textStyle,
                modifier = fillModifier,
                colors = CheckBoxColors(
                    checkedColor = ColorProvider(day = Color.Red, night = Color.Cyan),
                    uncheckedColor = ColorProvider(day = Color.Green, night = Color.Magenta)
                )
            )
            CheckBox(
                checked = checkbox3Checked,
                onCheckedChange = actionRunCallback<ToggleAction>(
                    actionParametersOf(EventTargetKey to Buttons.CHECK_3.name)
                ),
                text = "Checkbox 3",
                colors = CheckBoxColors(R.color.my_checkbox_colors)
            )
            Switch(
                checked = switch1Checked,
                onCheckedChange = actionRunCallback<ToggleAction>(
                    actionParametersOf(EventTargetKey to Buttons.SWITCH_1.name)
                ),
                text = "Switch 1",
                colors = SwitchColors(
                    checkedThumbColor = ColorProvider(day = Color.Red, night = Color.Cyan),
                    uncheckedThumbColor = ColorProvider(day = Color.Green, night = Color.Magenta),
                    checkedTrackColor = ColorProvider(day = Color.Blue, night = Color.Yellow),
                    uncheckedTrackColor = ColorProvider(day = Color.Magenta, night = Color.Green)
                ),
            )
            Switch(
                checked = switch2Checked,
                onCheckedChange = actionRunCallback<ToggleAction>(
                    actionParametersOf(EventTargetKey to Buttons.SWITCH_2.name)
                ),
                text = "Switch 2",
                style = textStyle,
                modifier = fillModifier
            )
            Column(modifier = fillModifier.selectableGroup()) {
                RadioButton(
                    checked = radio1Checked,
                    onClick = actionRunCallback<RadioAction>(
                        actionParametersOf(EventTargetKey to Radios.RADIO_1.name)
                    ),
                    text = "Radio 1",
                    colors = RadioButtonColors(
                        checkedColor = ColorProvider(day = Color.Red, night = Color.Cyan),
                        uncheckedColor = ColorProvider(day = Color.Green, night = Color.Magenta)
                    ),
                )
                RadioButton(
                    checked = radio2Checked,
                    onClick = actionRunCallback<RadioAction>(
                        actionParametersOf(EventTargetKey to Radios.RADIO_2.name)
                    ),
                    text = "Radio 2",
                    colors = RadioButtonColors(
                        checkedColor = ColorProvider(day = Color.Cyan, night = Color.Yellow),
                        uncheckedColor = ColorProvider(day = Color.Red, night = Color.Blue)
                    ),
                )
                RadioButton(
                    checked = radio3Checked,
                    onClick = actionRunCallback<RadioAction>(
                        actionParametersOf(EventTargetKey to Radios.RADIO_3.name)
                    ),
                    text = "Radio 3",
                )
            }
            Row(modifier = fillModifier.selectableGroup()) {
                RadioButton(
                    checked = radio1Checked,
                    onClick = null,
                    text = "Radio 1",
                )
                RadioButton(
                    checked = radio2Checked,
                    onClick = null,
                    text = "Radio 2",
                )
                RadioButton(
                    checked = radio3Checked,
                    onClick = null,
                    text = "Radio 3",
                )
            }
        }
    }
}

class ToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val target = requireNotNull(parameters[EventTargetKey]) {
            "Add event target to parameters in order to update the view state."
        }.let { CompoundButtonAppWidget.Buttons.valueOf(it) }
        val checked = requireNotNull(parameters[ToggleableStateKey]) {
            "This action should only be called in response to toggleable events"
        }

        updateAppWidgetState(context, glanceId) { state ->
            state[target.prefsKey] = checked
        }
        CompoundButtonAppWidget().update(context, glanceId)
    }
}

private val EventTargetKey = ActionParameters.Key<String>("EventTarget")

class RadioAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val target = requireNotNull(parameters[EventTargetKey]) {
            "Add event target to parameters in order to update the view state."
        }.let { CompoundButtonAppWidget.Radios.valueOf(it) }

        updateAppWidgetState(context, glanceId) { state ->
            CompoundButtonAppWidget.Radios.values().forEach { state[it.prefsKey] = it == target }
        }
        CompoundButtonAppWidget().update(context, glanceId)
    }
}

class CompoundButtonAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CompoundButtonAppWidget()
}
