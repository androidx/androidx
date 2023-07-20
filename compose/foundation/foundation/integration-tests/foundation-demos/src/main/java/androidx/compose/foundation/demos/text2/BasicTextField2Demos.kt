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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.demos.text.fontSize8
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp

@Composable
fun SwapFieldSameStateDemo() {
    var swapped by remember { mutableStateOf(false) }
    val state = remember { TextFieldState() }

    Column {
        Button(onClick = { swapped = !swapped }) {
            Text("Swap")
        }
        if (swapped) {
            BasicTextField2(
                state,
                Modifier.border(1.dp, Color.Magenta)
            )
        } else {
            BasicTextField2(
                state,
                Modifier.border(1.dp, Color.Blue)
            )
        }
    }
}

@Composable
fun BasicTextField2Demos() {
    Column(
        Modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        TagLine(tag = "Plain BasicTextField2")
        PlainBasicTextField2()

        TagLine(tag = "Single Line BasicTextField2")
        SingleLineBasicTextField2()

        TagLine(tag = "Multi Line BasicTextField2")
        MultiLineBasicTextField2()

        TagLine(tag = "State toggling BasicTextField2")
        StateTogglingBasicTextField2()

        TagLine(tag = "BasicTextField2 Edit Controls")
        BasicTextField2EditControls()
    }
}

@Composable
fun BasicTextField2ValueCallbackDemo() {
    Column(
        Modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        TagLine("Simple string-only")
        SimpleValueCallbackDemo()

        TagLine("Simple TextFieldValue")
        SimpleTextFieldValueCallbackDemo()

        TagLine("Callback changes to caps")
        CapitalizeValueCallbackDemo()
    }
}

@Composable
private fun SimpleValueCallbackDemo() {
    var text by remember { mutableStateOf("") }
    BasicTextField2(
        value = text,
        onValueChange = { text = it },
        modifier = demoTextFieldModifiers
    )
}

@Composable
private fun SimpleTextFieldValueCallbackDemo() {
    var value by remember { mutableStateOf(TextFieldValue()) }
    BasicTextField2(
        value = value,
        onValueChange = { value = it },
        modifier = demoTextFieldModifiers
    )
}

@Composable
private fun CapitalizeValueCallbackDemo() {
    var text by remember { mutableStateOf("") }
    BasicTextField2(
        value = text,
        onValueChange = { text = it.toUpperCase(Locale.current) },
        modifier = demoTextFieldModifiers
    )
    Text(text = "Backing state: \"$text\"", style = MaterialTheme.typography.caption)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlainBasicTextField2() {
    val state = remember { TextFieldState() }
    BasicTextField2(state, demoTextFieldModifiers, textStyle = LocalTextStyle.current)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleLineBasicTextField2() {
    val state = remember { TextFieldState() }
    BasicTextField2(
        state = state,
        modifier = demoTextFieldModifiers,
        textStyle = TextStyle(fontSize = fontSize8),
        lineLimits = TextFieldLineLimits.SingleLine
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiLineBasicTextField2() {
    val state = remember { TextFieldState() }
    BasicTextField2(
        state = state,
        modifier = demoTextFieldModifiers,
        textStyle = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Center),
        lineLimits = TextFieldLineLimits.MultiLine(
            minHeightInLines = 3,
            maxHeightInLines = 3
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StateTogglingBasicTextField2() {
    var counter by remember { mutableIntStateOf(0) }
    val states = remember { listOf(TextFieldState(), TextFieldState()) }
    val state = states[counter]
    Text("Click to toggle state: $counter", modifier = Modifier.clickable {
        counter++
        counter %= 2
    })

    BasicTextField2(state, demoTextFieldModifiers, textStyle = LocalTextStyle.current)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicTextField2EditControls() {
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

        BasicTextField2(
            state,
            demoTextFieldModifiers,
            textStyle = LocalTextStyle.current,
            enabled = enabled,
            readOnly = readOnly
        )
    }
}
