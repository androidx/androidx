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
import androidx.ui.core.DensityAmbient
import androidx.ui.core.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.ColumnScope.gravity
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredWidth
import androidx.ui.material.Checkbox
import androidx.ui.material.EmphasisAmbient
import androidx.ui.material.FilledTextField
import androidx.ui.material.MaterialTheme
import androidx.ui.material.RadioGroup
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.material.icons.filled.Info
import androidx.ui.material.samples.FilledTextFieldSample
import androidx.ui.material.samples.FilledTextFieldWithErrorState
import androidx.ui.material.samples.FilledTextFieldWithIcons
import androidx.ui.material.samples.FilledTextFieldWithPlaceholder
import androidx.ui.material.samples.PasswordFilledTextField
import androidx.ui.material.samples.TextFieldWithHelperMessage
import androidx.ui.material.samples.TextFieldWithHideKeyboardOnImeAction
import androidx.ui.savedinstancestate.savedInstanceState
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

@Composable
fun TextFieldsDemo() {
    val space = with(DensityAmbient.current) { 5.dp.toIntPx() }
    Column(
        modifier = Modifier.fillMaxHeight().padding(10.dp),
        verticalArrangement = arrangeWithSpacer(space)
    ) {
        Text("Password text field")
        PasswordFilledTextField()
        Text("Text field with leading and trailing icons")
        FilledTextFieldWithIcons()
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
fun FilledTextFieldDemo() {
    Column(Modifier.padding(10.dp)) {
        var text by savedInstanceState { "" }
        var leadingChecked by savedInstanceState { false }
        var trailingChecked by savedInstanceState { false }
        val characterCounterChecked by savedInstanceState { false }
        var selectedOption by savedInstanceState { Option.None }

        val textField = @Composable {
            FilledTextField(
                value = text,
                onValueChange = { text = it },
                label = {
                    val label = "Label" + if (selectedOption == Option.Error) "*" else ""
                    Text(text = label)
                },
                leadingIcon = { if (leadingChecked) Icon(Icons.Filled.Favorite) },
                trailingIcon = { if (trailingChecked) Icon(Icons.Filled.Info) },
                isErrorValue = selectedOption == Option.Error
            )
        }

        Box(Modifier.preferredHeight(150.dp).gravity(Alignment.CenterHorizontally)) {
            if (selectedOption == Option.None) {
                textField()
            } else {
                TextFieldWithMessage(textField, selectedOption)
            }
        }

        Column {
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
                title = "Character counter",
                checked = characterCounterChecked,
                onCheckedChange = { /* TODO */ }
            )

            Spacer(Modifier.preferredHeight(20.dp))

            Title("Assistive text")
            RadioGroup(
                options = Option.values().map { it.name },
                selectedOption = selectedOption.name,
                onSelectedChange = { selectedOption = Option.valueOf(it) }
            )
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
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.padding(start = 10.dp, top = 10.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.preferredWidth(20.dp))
        Text(text = title, style = MaterialTheme.typography.body1)
    }
}

/**
 * Helper message option
 */
private enum class Option { None, Helper, Error }

private fun arrangeWithSpacer(space: IntPx) = object : Arrangement.Vertical {
    override fun arrange(
        totalSize: IntPx,
        size: List<IntPx>,
        layoutDirection: LayoutDirection
    ): List<IntPx> {
        val positions = mutableListOf<IntPx>()
        var current = 0.ipx
        size.forEach {
            positions.add(current)
            current += (it + space)
        }
        return positions
    }
}