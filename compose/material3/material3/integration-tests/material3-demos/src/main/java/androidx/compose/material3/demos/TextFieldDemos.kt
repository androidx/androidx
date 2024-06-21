/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.demos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MaterialTextFieldDemo() {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(PaddingValues(10.dp))) {
        var text by rememberSaveable { mutableStateOf("") }
        var leadingChecked by rememberSaveable { mutableStateOf(false) }
        var trailingChecked by rememberSaveable { mutableStateOf(false) }
        val characterCounterChecked by rememberSaveable { mutableStateOf(false) }
        var singleLineChecked by rememberSaveable { mutableStateOf(true) }
        var selectedOption by rememberSaveable { mutableStateOf(Option.None) }
        var selectedTextField by rememberSaveable { mutableStateOf(TextFieldType.Filled) }
        var disabled by rememberSaveable { mutableStateOf(false) }
        var readOnly by rememberSaveable { mutableStateOf(false) }

        val textField: @Composable () -> Unit =
            @Composable {
                when (selectedTextField) {
                    TextFieldType.Filled ->
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            enabled = !disabled,
                            readOnly = readOnly,
                            singleLine = singleLineChecked,
                            label = {
                                val label =
                                    "Label" + if (selectedOption == Option.Error) "*" else ""
                                Text(text = label)
                            },
                            leadingIcon =
                                if (leadingChecked) {
                                    @Composable { Icon(Icons.Filled.Favorite, "Favorite") }
                                } else {
                                    null
                                },
                            trailingIcon =
                                if (trailingChecked) {
                                    @Composable { Icon(Icons.Filled.Info, "Info") }
                                } else {
                                    null
                                },
                            isError = selectedOption == Option.Error,
                            modifier = Modifier.requiredWidth(300.dp)
                        )
                    TextFieldType.Outlined ->
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            enabled = !disabled,
                            readOnly = readOnly,
                            singleLine = singleLineChecked,
                            label = {
                                val label =
                                    "Label" + if (selectedOption == Option.Error) "*" else ""
                                Text(text = label)
                            },
                            leadingIcon =
                                if (leadingChecked) {
                                    @Composable { Icon(Icons.Filled.Favorite, "Favorite") }
                                } else {
                                    null
                                },
                            trailingIcon =
                                if (trailingChecked) {
                                    @Composable { Icon(Icons.Filled.Info, "Info") }
                                } else {
                                    null
                                },
                            isError = selectedOption == Option.Error,
                            modifier = Modifier.requiredWidth(300.dp)
                        )
                }
            }

        Box(Modifier.height(150.dp).align(Alignment.CenterHorizontally)) {
            if (selectedOption == Option.None) {
                textField()
            } else {
                TextFieldWithMessage(selectedOption, textField)
            }
        }

        Column {
            Title("Text field type")
            TextFieldType.values()
                .map { it.name }
                .forEach { textType ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = (textType == selectedTextField.name),
                                onClick = { selectedTextField = TextFieldType.valueOf(textType) }
                            )
                            .padding(horizontal = 16.dp)
                    ) {
                        RadioButton(selected = (textType == selectedTextField.name), onClick = null)
                        Text(
                            text = textType,
                            style = MaterialTheme.typography.bodyLarge.merge(),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

            Title("Options")
            OptionRow(
                title = "Leading icon",
                checked = leadingChecked,
                onCheckedChange = { leadingChecked = it }
            )
            OptionRow(
                title = "Trailing icon",
                checked = trailingChecked,
                onCheckedChange = { trailingChecked = it }
            )
            OptionRow(
                title = "Single line",
                checked = singleLineChecked,
                onCheckedChange = { singleLineChecked = it }
            )
            OptionRow(
                title = "Character counter (TODO)",
                checked = characterCounterChecked,
                enabled = false,
                onCheckedChange = { /* TODO */ }
            )

            Spacer(Modifier.height(20.dp))

            Title("Assistive text")
            Option.values()
                .map { it.name }
                .forEach { text ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = (text == selectedOption.name),
                                onClick = { selectedOption = Option.valueOf(text) }
                            )
                            .padding(horizontal = 16.dp)
                    ) {
                        RadioButton(selected = (text == selectedOption.name), onClick = null)
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge.merge(),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

            Title("Other settings")
            OptionRow(title = "Read-only", checked = readOnly, onCheckedChange = { readOnly = it })
            OptionRow(title = "Disabled", checked = disabled, onCheckedChange = { disabled = it })
        }
    }
}

/** Text field with helper or error message below. */
@Composable
private fun TextFieldWithMessage(helperMessageOption: Option, content: @Composable () -> Unit) {
    val typography = MaterialTheme.typography.labelMedium
    val color =
        when (helperMessageOption) {
            Option.Helper -> {
                MaterialTheme.colorScheme.onSurface
            }
            Option.Error -> MaterialTheme.colorScheme.error
            else -> Color.Unspecified
        }

    Column {
        Box(modifier = Modifier.weight(1f, fill = false)) { content() }
        Text(
            text = "Helper message",
            color = color,
            style = typography,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun ColumnScope.Title(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun OptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        Modifier.padding(start = 10.dp, top = 10.dp)
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, enabled = enabled)
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Spacer(Modifier.width(20.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Helper message option */
private enum class Option {
    None,
    Helper,
    Error
}

private enum class TextFieldType {
    Filled,
    Outlined
}
