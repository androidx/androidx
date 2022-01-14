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

package androidx.template.appwidget.demos

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.unit.ColorProvider
import androidx.template.appwidget.GlanceTemplateAppWidget
import androidx.template.template.GlanceTemplate
import androidx.template.template.SingleEntityTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TemplateInputActivity : ComponentActivity() {
    private val MAX_LONG_HEX = "FFFFFFFF".toLong(radix = 16)

    override fun onResume() {
        super.onResume()

        setContent {
            val context: Context = this@TemplateInputActivity
            var expanded by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxSize()) {
                // Template selection
                Row(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                    Button(onClick = { expanded = true }) { Text("Select template to edit") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(onClick = {
                            CoroutineScope(SupervisorJob()).launch {
                                // Save template selection to app datastore
                                GlanceState.updateValue(context, state, appFileKey()) { prefs ->
                                    prefs.toMutablePreferences().apply {
                                        this[TemplateKey] = Templates.SingleEntity.ordinal
                                    }
                                }
                                expanded = false
                            }
                        }) { Text("SingleEntityTemplate") }
                    }
                }
                // TODO: set activity content based on selected template
                SingleEntityContent()
            }
        }
    }

    @Composable
    fun SingleEntityContent() {
        val context: Context = this@TemplateInputActivity
        var mainText by remember { mutableStateOf(TextFieldValue()) }

        Column(modifier = Modifier.wrapContentHeight().fillMaxWidth()) {
            // Data input
            Text("Background color:")
            // TODO: clearer instructions and more input verification
            TextField(value = mainText, onValueChange = { mainText = it })

            Button(
                onClick = {
                    val longValue = mainText.text.toLong(radix = 16)
                    if (longValue > MAX_LONG_HEX) {
                        mainText = TextFieldValue("FFFFFFFF")
                        Toast.makeText(context, "Value out of range", Toast.LENGTH_SHORT).show()
                    } else if (longValue < 0) {
                        mainText = TextFieldValue("00000000")
                        Toast.makeText(context, "Value out of range", Toast.LENGTH_SHORT).show()
                    }

                    CoroutineScope(SupervisorJob()).launch {
                        // Save input to app datastore
                        GlanceState.updateValue(context, state, appFileKey()) { prefs ->
                            prefs.toMutablePreferences().apply {
                                this[BackgroundKey] = (mainText.text).toLong(radix = 16)
                            }
                        }
                    }
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }
            ) { Text("Submit") }
        }
    }
}

val state: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

class SingleEntityInputWidgetTemplate : SingleEntityTemplate() {
    override fun getData(state: Any?): Data {
        require(state is Preferences)
        val background = state[BackgroundKey]?.let { ColorProvider(Color(it)) }
            ?: ColorProvider(R.color.default_widget_background)
        return createData(background)
    }
}

class SingleEntityTemplateWidget : GlanceTemplateAppWidget(template)

class TemplateInputWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleEntityTemplateWidget()
}

class TemplateButtonAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Fetch the template and values from the app datastore and apply to the widget
        val background = GlanceState.getValue(context, state, appFileKey())[BackgroundKey]
        val templateIndex = GlanceState.getValue(context, state, appFileKey())[TemplateKey] ?: 0
        template = getTemplate(Templates.values()[templateIndex])
        val widget = SingleEntityTemplateWidget()
        if (background != null) {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[BackgroundKey] = background
                }
            }
        }
        widget.update(context, glanceId)
    }
}

// TODO: Add templates
private enum class Templates {
    SingleEntity
}

private val TemplateKey = intPreferencesKey("template_key")
private val BackgroundKey = longPreferencesKey("background_key")

// Template used by the widget, update this to change the widget type
private var template: GlanceTemplate<*> = SingleEntityInputWidgetTemplate()

private fun createData(background: ColorProvider) = SingleEntityTemplate.Data(
    "Template example",
    "Apply",
    actionRunCallback<TemplateButtonAction>(),
    ImageProvider(R.drawable.compose),
    background
)

private fun getTemplate(type: Templates): GlanceTemplate<*> =
    when (type) {
        Templates.SingleEntity -> SingleEntityInputWidgetTemplate()
    }

private fun appFileKey() = "appKey-" + TemplateInputActivity::class.java
