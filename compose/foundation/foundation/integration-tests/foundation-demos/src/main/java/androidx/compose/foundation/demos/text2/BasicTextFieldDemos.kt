/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.demos.text.fontSize8
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp

@Composable
fun SwapFieldSameStateDemo() {
    var swapped by remember { mutableStateOf(false) }
    val state = remember { TextFieldState() }

    Column {
        Button(onClick = { swapped = !swapped }) { Text("Swap") }
        if (swapped) {
            BasicTextField(state, Modifier.border(1.dp, Color.Magenta))
        } else {
            BasicTextField(state, Modifier.border(1.dp, Color.Blue))
        }
    }
}

@Composable
fun BasicTextFieldDemos() {
    Column(Modifier.imePadding().verticalScroll(rememberScrollState())) {
        TagLine(tag = "Plain BasicTextField")
        PlainBasicTextField()

        TagLine(tag = "Single Line BasicTextField")
        SingleLineBasicTextField()

        TagLine(tag = "Multi Line BasicTextField")
        MultiLineBasicTextField()

        TagLine(tag = "State toggling BasicTextField")
        StateTogglingBasicTextField()

        TagLine(tag = "BasicTextField Edit Controls")
        BasicTextFieldEditControls()

        TagLine(tag = "BasicTextField Programmatic Edit")
        BasicTextFieldProgrammaticEdit()
    }
}

@Composable
fun BasicTextFieldValueCallbackDemo() {
    Column(Modifier.imePadding().verticalScroll(rememberScrollState())) {
        TagLine("Simple string-only")
        SimpleValueCallbackDemo()

        TagLine("Callback changes to caps")
        CapitalizeValueCallbackDemo()
    }
}

@Composable
private fun SimpleValueCallbackDemo() {
    var text by remember { mutableStateOf("") }
    BasicTextField(value = text, onValueChange = { text = it }, modifier = demoTextFieldModifiers)
}

@Composable
private fun CapitalizeValueCallbackDemo() {
    var text by remember { mutableStateOf("") }
    BasicTextField(
        value = text,
        onValueChange = { text = it.toUpperCase(Locale.current) },
        modifier = demoTextFieldModifiers
    )
    Text(text = "Backing state: \"$text\"", style = MaterialTheme.typography.caption)
}

@Composable
fun PlainBasicTextField() {
    val state = remember { TextFieldState() }
    BasicTextField(state, demoTextFieldModifiers, textStyle = LocalTextStyle.current)
}

@Composable
fun SingleLineBasicTextField() {
    val state = remember { TextFieldState() }
    BasicTextField(
        state = state,
        modifier = demoTextFieldModifiers,
        textStyle = TextStyle(fontSize = fontSize8),
        lineLimits = TextFieldLineLimits.SingleLine
    )
}

@Composable
fun MultiLineBasicTextField() {
    val state = remember { TextFieldState() }
    BasicTextField(
        state = state,
        modifier = demoTextFieldModifiers,
        textStyle = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Center),
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 3)
    )
}

@Composable
fun StateTogglingBasicTextField() {
    var counter by remember { mutableIntStateOf(0) }
    val states = remember { listOf(TextFieldState(), TextFieldState()) }
    val state = states[counter]
    Text(
        "Click to toggle state: $counter",
        modifier =
            Modifier.clickable {
                counter++
                counter %= 2
            }
    )

    BasicTextField(state, demoTextFieldModifiers, textStyle = LocalTextStyle.current)
}

@Composable
fun BasicTextFieldEditControls() {
    var enabled by remember { mutableStateOf(true) }
    var readOnly by remember { mutableStateOf(false) }
    val state = remember { TextFieldState("Content goes here") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enabled")
            Checkbox(checked = enabled, onCheckedChange = { enabled = it })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Readonly")
            Checkbox(checked = readOnly, onCheckedChange = { readOnly = it })
        }

        BasicTextField(
            state,
            demoTextFieldModifiers,
            textStyle = LocalTextStyle.current,
            enabled = enabled,
            readOnly = readOnly
        )
    }
}

@Composable
fun BasicTextFieldProgrammaticEdit() {
    val state = remember { TextFieldState() }
    Column {
        Row {
            Button(onClick = { state.edit { replace(selection.start, selection.end, "A") } }) {
                Text("A")
            }
            Button(onClick = { state.edit { replace(selection.start, selection.end, "B") } }) {
                Text("B")
            }
            Button(
                onClick = {
                    state.edit {
                        if (selection.collapsed) {
                            delete((selection.min - 1).coerceAtLeast(0), selection.min)
                        } else {
                            delete(selection.start, selection.end)
                        }
                    }
                }
            ) {
                Text("Backspace")
            }
        }
        BasicTextField(
            state = state,
            modifier = demoTextFieldModifiers,
        )
    }
}
