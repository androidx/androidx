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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
import androidx.template.template.TemplateImageWithDescription
import androidx.template.template.SingleEntityTemplate
import androidx.template.template.TemplateTextButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Main activity for the template data demo app. Hosts a form for template data to apply to a home
 * screen widget.
 */
class TemplateInputActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()

        setContent {
            val context: Context = this@TemplateInputActivity
            var expanded by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
        var titleText by remember { mutableStateOf(TextFieldValue(DEFAULT_TITLE)) }
        var subtitleText by remember { mutableStateOf(TextFieldValue()) }
        var bodyText by remember { mutableStateOf(TextFieldValue()) }
        var colorText by remember { mutableStateOf(TextFieldValue(DEFAULT_BACKGROUND)) }

        Column(modifier = Modifier.wrapContentHeight().fillMaxWidth()) {
            // Data input
            Text("Glanceable title:")
            TextField(
                value = titleText,
                singleLine = true,
                placeholder = { Text(DEFAULT_TITLE) },
                onValueChange = { titleText = it },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Text("Glanceable subtitle:", modifier = Modifier.padding(top = 8.dp))
            TextField(
                value = subtitleText,
                singleLine = true,
                onValueChange = { subtitleText = it },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Text("Glanceable body text:", modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = bodyText,
                    onValueChange = { bodyText = it },
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1F),
                    shape = MaterialTheme.shapes.medium
                )
                Button(
                    onClick = { bodyText = TextFieldValue(DEFAULT_BODY) }
                ) { Text("Use lorem ipsum") }
            }

            Text("Background color:", modifier = Modifier.padding(top = 8.dp))
            Text("Enter an 8-digit hex color code from 0 to FFFFFFFF (default: FFCCCCCC)")
            Text("Using ARGB format, so the first two digits are the alpha value. From 00 " +
                "(completely transparent) to FF (completely opaque)")
            Row {
                TextField(
                    value = colorText,
                    singleLine = true,
                    placeholder = { Text(DEFAULT_BACKGROUND) },
                    onValueChange = { colorText = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        colorText = TextFieldValue(correctBackground(context, colorText.text))

                        submitForm(
                            context,
                            TitleKey to titleText.text,
                            SubtitleKey to subtitleText.text,
                            BodyKey to bodyText.text,
                            BackgroundKey to colorText.text.toLong(16)
                        )
                    })
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { colorText = TextFieldValue(DEFAULT_BACKGROUND) }) {
                    Text("Use default")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { colorText = TextFieldValue("") }) {
                    Text("Clear")
                }
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    modifier = Modifier.width(100.dp),
                    onClick = {
                        titleText = TextFieldValue("")
                        subtitleText = TextFieldValue("")
                        bodyText = TextFieldValue("")
                        colorText = TextFieldValue("")
                    }
                ) { Text("Clear all") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    modifier = Modifier.width(100.dp),
                    onClick = {
                        colorText = TextFieldValue(correctBackground(context, colorText.text))

                        submitForm(
                            context,
                            TitleKey to titleText.text,
                            SubtitleKey to subtitleText.text,
                            BodyKey to bodyText.text,
                            BackgroundKey to colorText.text.toLong(16)
                        )
                    }
                ) { Text("Submit") }
            }
        }
    }

    private fun correctBackground(context: Context, color: String): String {
        if (color.isEmpty()) {
            return DEFAULT_BACKGROUND
        }

        var colorString = color
        if (colorString.length > 8) {
            colorString = colorString.subSequence(0, 8).toString()
        }
        while (colorString.length < 8) {
            colorString += "0"
        }

        try {
            val longValue = colorString.toLong(radix = 16)

            if (longValue > COLOR_LONG_MAX_HEX) {
                Toast.makeText(context, R.string.background_error_high, Toast.LENGTH_SHORT).show()
                return COLOR_STRING_MAX
            } else if (longValue < 0) {
                Toast.makeText(context, R.string.background_error_low, Toast.LENGTH_SHORT).show()
                return COLOR_STRING_MIN
            }
        } catch (e: NumberFormatException) {
            return DEFAULT_BACKGROUND
        }

        return colorString
    }

    private fun submitForm(context: Context, vararg pairs: Preferences.Pair<*>) {
        CoroutineScope(SupervisorJob()).launch {
            GlanceState.updateValue(context, state, appFileKey()) { prefs ->
                prefs.toMutablePreferences().apply {
                    this.putAll(*pairs)
                }
            }
        }
        Toast.makeText(context, R.string.template_data_saved_message, Toast.LENGTH_SHORT).show()
    }
}

class SingleEntityInputWidgetTemplate : SingleEntityTemplate() {
    override fun getData(state: Any?): Data {
        require(state is Preferences)
        val title = state[TitleKey] ?: DEFAULT_TITLE
        val subtitle = state[SubtitleKey] ?: ""
        val body = state[BodyKey] ?: ""
        val background = state[BackgroundKey]?.let { ColorProvider(Color(it)) }
            ?: ColorProvider(R.color.default_widget_background)
        return createData(title, subtitle, body, background)
    }
}

class SingleEntityTemplateWidget : GlanceTemplateAppWidget(template)

class TemplateInputWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleEntityTemplateWidget()
}

class TemplateButtonAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Fetch the template and values from the app datastore and apply to the widget
        val appPrefs = GlanceState.getValue(context, state, appFileKey())
        val templateIndex = appPrefs[TemplateKey] ?: 0
        template = getTemplate(Templates.values()[templateIndex])

        updateAppWidgetState(context, glanceId) { widgetPrefs ->
            widgetPrefs.putAll(
                TitleKey to (appPrefs[TitleKey] ?: DEFAULT_TITLE),
                BackgroundKey to
                    (appPrefs[BackgroundKey] ?: DEFAULT_BACKGROUND.toLong(radix = 16))
            )
            val subtitleText = appPrefs[SubtitleKey]
            if (subtitleText == null) {
                widgetPrefs.minusAssign(SubtitleKey)
            } else {
                widgetPrefs[SubtitleKey] = subtitleText
            }
            val bodyText = appPrefs[BodyKey]
            if (bodyText == null) {
                widgetPrefs.minusAssign(BodyKey)
            } else {
                widgetPrefs[BodyKey] = bodyText
            }
        }
        SingleEntityTemplateWidget().update(context, glanceId)
    }
}

// TODO: Add templates
private enum class Templates {
    SingleEntity
}

private const val DEFAULT_TITLE = "Title"
private const val DEFAULT_BACKGROUND = "FFCCCCCC"
private const val DEFAULT_BODY = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed " +
    "do eiusmod tempor incididunt ut labore et dolore magna aliqua."
private const val COLOR_STRING_MIN = "FF000000"
private const val COLOR_STRING_MAX = "FFFFFFFF"

private val COLOR_LONG_MAX_HEX = COLOR_STRING_MAX.toLong(radix = 16)

private val state: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

private val TemplateKey = intPreferencesKey("template_key")
private val TitleKey = stringPreferencesKey("title_key")
private val SubtitleKey = stringPreferencesKey("subtitle_key")
private val BodyKey = stringPreferencesKey("body_key")
private val BackgroundKey = longPreferencesKey("background_key")

// Template used by the widget, update this to change the widget type
private var template: GlanceTemplate<*> = SingleEntityInputWidgetTemplate()

private fun createData(
    title: String,
    subtitle: String,
    body: String,
    background: ColorProvider
) = SingleEntityTemplate.Data(
    header = "Demo",
    headerIcon = TemplateImageWithDescription(
        ImageProvider(R.drawable.compose),
        "Header icon"
    ),
    title = title,
    subtitle = subtitle,
    bodyText = body,
    button = TemplateTextButton(
        actionRunCallback<TemplateButtonAction>(),
        "Apply"
    ),
    mainImage = TemplateImageWithDescription(
        ImageProvider(R.drawable.compose),
        "Compose image"
    ),
    backgroundColor = background
)

private fun getTemplate(type: Templates): GlanceTemplate<*> =
    when (type) {
        Templates.SingleEntity -> SingleEntityInputWidgetTemplate()
    }

private fun appFileKey() = "appKey-" + TemplateInputActivity::class.java
