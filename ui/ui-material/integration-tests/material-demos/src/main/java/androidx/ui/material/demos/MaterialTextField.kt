/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.material.demos

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.compose.foundation.Box
import androidx.compose.foundation.Icon
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.Text
import androidx.compose.foundation.selection.selectable
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.ColumnScope.gravity
import androidx.ui.layout.InnerPadding
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredWidth
import androidx.ui.material.Checkbox
import androidx.ui.material.EmphasisAmbient
import androidx.ui.material.MaterialTheme
import androidx.ui.material.OutlinedTextField
import androidx.ui.material.RadioButton
import androidx.ui.material.TextField
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.material.icons.filled.Info
import androidx.ui.material.samples.FilledTextFieldSample
import androidx.ui.material.samples.FilledTextFieldWithErrorState
import androidx.ui.material.samples.FilledTextFieldWithIcons
import androidx.ui.material.samples.FilledTextFieldWithPlaceholder
import androidx.ui.material.samples.PasswordFilledTextField
import androidx.ui.material.samples.SimpleOutlinedTextFieldSample
import androidx.ui.material.samples.TextFieldWithHelperMessage
import androidx.ui.material.samples.TextFieldWithHideKeyboardOnImeAction
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.ui.unit.dp

@Composable
fun TextFieldsDemo() {
    ScrollableColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = InnerPadding(10.dp)
    ) {
        Text("Password text field")
        PasswordFilledTextField()
        Text("Text field with leading and trailing icons")
        FilledTextFieldWithIcons()
        Text("Outlined text field")
        SimpleOutlinedTextFieldSample()
        Text("Text field with placeholder")
        FilledTextFieldWithPlaceholder()
        Text("Text field with error state handling")
        FilledTextFieldWithErrorState()
        Text("Text field with helper/error message")
        TextFieldWithHelperMessage()
        Text("Hide keyboard on IME action")
        TextFieldWithHideKeyboardOnImeAction()
        Text("TextFieldValue overload")
        FilledTextFieldSample()
    }
}

@Composable
fun MaterialTextFieldDemo() {
    ScrollableColumn(contentPadding = InnerPadding(10.dp)) {
        var text by savedInstanceState { "" }
        var leadingChecked by savedInstanceState { false }
        var trailingChecked by savedInstanceState { false }
        val characterCounterChecked by savedInstanceState { false }
        var selectedOption by savedInstanceState { Option.None }
        var selectedTextField by savedInstanceState { TextFieldType.Filled }

        val textField: @Composable () -> Unit = @Composable {
            when (selectedTextField) {
                TextFieldType.Filled ->
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        label = {
                            val label =
                                "Label" + if (selectedOption == Option.Error) "*" else ""
                            Text(text = label)
                        },
                        leadingIcon = { if (leadingChecked) Icon(Icons.Filled.Favorite) },
                        trailingIcon = { if (trailingChecked) Icon(Icons.Filled.Info) },
                        isErrorValue = selectedOption == Option.Error
                    )
                TextFieldType.Outlined ->
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = {
                            val label =
                                "Label" + if (selectedOption == Option.Error) "*" else ""
                            Text(text = label)
                        },
                        leadingIcon = { if (leadingChecked) Icon(Icons.Filled.Favorite) },
                        trailingIcon = { if (trailingChecked) Icon(Icons.Filled.Info) },
                        isErrorValue = selectedOption == Option.Error
                    )
            }
        }

        Box(Modifier.preferredHeight(150.dp).gravity(Alignment.CenterHorizontally)) {
            if (selectedOption == Option.None) {
                textField()
            } else {
                TextFieldWithMessage(textField, selectedOption)
            }
        }

        Column {
            Title("Text field type")
            Column {
                TextFieldType.values().map { it.name }.forEach { textType ->
                    Row(Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (textType == selectedTextField.name),
                            onClick = {
                                selectedTextField = TextFieldType.valueOf(textType)
                            }
                        )
                        .padding(horizontal = 16.dp)
                    ) {
                        RadioButton(
                            selected = (textType == selectedTextField.name),
                            onClick = { selectedTextField = TextFieldType.valueOf(textType) }
                        )
                        Text(
                            text = textType,
                            style = MaterialTheme.typography.body1.merge(),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
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
                title = "Character counter (TODO)",
                checked = characterCounterChecked,
                enabled = false,
                onCheckedChange = { /* TODO */ }
            )

            Spacer(Modifier.preferredHeight(20.dp))

            Title("Assistive text")
            Column {
                Option.values().map { it.name }.forEach { text ->
                    Row(Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (text == selectedOption.name),
                            onClick = { selectedOption = Option.valueOf(text) }
                        )
                        .padding(horizontal = 16.dp)
                    ) {
                        RadioButton(
                            selected = (text == selectedOption.name),
                            onClick = { selectedOption = Option.valueOf(text) }
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.body1.merge(),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Text field with helper or error message below.
 */
@Composable
private fun TextFieldWithMessage(
    textField: @Composable () -> Unit,
    helperMessageOption: Option
) {
    val typography = MaterialTheme.typography.caption
    val color = when (helperMessageOption) {
        Option.Helper -> {
            EmphasisAmbient.current.medium.applyEmphasis(MaterialTheme.colors.onSurface)
        }
        Option.Error -> MaterialTheme.colors.error
        else -> Color.Unset
    }

    Column {
        textField()
        Text(
            text = "Helper message",
            style = typography.copy(color = color),
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun Title(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.body1,
        modifier = Modifier.gravity(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.preferredHeight(10.dp))
}

@Composable
private fun OptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(Modifier.padding(start = 10.dp, top = 10.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Spacer(Modifier.preferredWidth(20.dp))
        Text(text = title, style = MaterialTheme.typography.body1)
    }
}

/**
 * Helper message option
 */
private enum class Option { None, Helper, Error }

private enum class TextFieldType { Filled, Outlined }
