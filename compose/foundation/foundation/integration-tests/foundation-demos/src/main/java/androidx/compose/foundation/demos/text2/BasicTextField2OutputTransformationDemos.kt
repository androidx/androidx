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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField2
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun BasicTextField2OutputTransformationDemos() {
    Column(
        modifier = Modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        TagLine("Insert, replace, delete")
        InsertReplaceDeleteDemo()

        TagLine("Phone number as-you-type")
        PhoneNumberAsYouTypeDemo()

        TagLine("Phone number full template")
        PhoneNumberFullTemplateDemo()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InsertReplaceDeleteDemo() {
    val text = remember { TextFieldState("abc def ghi") }
    var prefixEnabled by remember { mutableStateOf(true) }
    var suffixEnabled by remember { mutableStateOf(true) }
    var middleWedge by remember { mutableStateOf(true) }
    var replacementEnabled by remember { mutableStateOf(true) }
    var deletionEnabled by remember { mutableStateOf(true) }
    val prefix = remember { TextFieldState(">") }
    val suffix = remember { TextFieldState("<") }
    val middle = remember { TextFieldState("insertion") }
    val replacement = remember { TextFieldState("wedge") }

    Text(
        "To move the cursor around, use the GBoard menu to get at selection controls, plug in a " +
            "hardware keyboard, or use the Running Devices tool in Android Studio with a " +
            "physical device. On an emulator, your host's hardware arrow keys won't work.",
        style = MaterialTheme.typography.caption
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = prefixEnabled, onCheckedChange = { prefixEnabled = it })
        Text("Prefix insertion ")
        Text("\"", style = MaterialTheme.typography.caption)
        BasicTextField2(prefix, textStyle = MaterialTheme.typography.caption)
        Text("\"", style = MaterialTheme.typography.caption)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = middleWedge, onCheckedChange = { middleWedge = it })
        Text("Middle insertion ")
        Text("\"", style = MaterialTheme.typography.caption)
        BasicTextField2(middle, textStyle = MaterialTheme.typography.caption)
        Text("\"", style = MaterialTheme.typography.caption)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = suffixEnabled, onCheckedChange = { suffixEnabled = it })
        Text("Suffix insertion ")
        Text("\"", style = MaterialTheme.typography.caption)
        BasicTextField2(suffix, textStyle = MaterialTheme.typography.caption)
        Text("\"", style = MaterialTheme.typography.caption)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = replacementEnabled, onCheckedChange = { replacementEnabled = it })
        Text("Replacement ")
        Text("s/abc/", style = MaterialTheme.typography.caption)
        BasicTextField2(replacement, textStyle = MaterialTheme.typography.caption)
        Text("/", style = MaterialTheme.typography.caption)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = deletionEnabled, onCheckedChange = { deletionEnabled = it })
        Text("Deletion ")
        Text("s/def//", style = MaterialTheme.typography.caption)
    }
    Row(/*verticalAlignment = Alignment.CenterVertically*/) {
        var textLayoutResultProvider: () -> TextLayoutResult? by remember {
            mutableStateOf({ null })
        }
        var isFirstFieldFocused by remember { mutableStateOf(false) }
        BasicTextField2(
            state = text,
            onTextLayout = { textLayoutResultProvider = it },
            modifier = Modifier
                .alignByBaseline()
                .weight(0.5f)
                .then(demoTextFieldModifiers)
                .onFocusChanged { isFirstFieldFocused = it.isFocused }
                .drawWithContent {
                    drawContent()

                    // Only draw selection outline when not focused.
                    if (isFirstFieldFocused) return@drawWithContent
                    val textLayoutResult = textLayoutResultProvider() ?: return@drawWithContent
                    val selection = text.text.selectionInChars
                    if (selection.collapsed) {
                        val cursorRect = textLayoutResult.getCursorRect(selection.start)
                        drawLine(
                            Color.Blue,
                            start = cursorRect.topCenter,
                            end = cursorRect.bottomCenter
                        )
                    } else {
                        val selectionPath =
                            textLayoutResult.getPathForRange(selection.min, selection.max)
                        drawPath(
                            selectionPath,
                            Color.Blue,
                            alpha = 0.8f,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                },
        )
        Icon(
            Icons.AutoMirrored.Default.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.alignBy { (it.measuredHeight * 0.75f).toInt() }
        )
        BasicTextField2(
            state = text,
            modifier = Modifier
                .alignByBaseline()
                .weight(0.5f)
                .then(demoTextFieldModifiers),
            outputTransformation = {
                if (prefixEnabled) {
                    insert(0, prefix.text.toString())
                }
                if (replacementEnabled) {
                    "abc".toRegex().find(asCharSequence())?.let { match ->
                        replace(match.range.first, match.range.last + 1, replacement.text)
                    }
                }
                if (deletionEnabled) {
                    "def".toRegex().find(asCharSequence())?.let { match ->
                        delete(match.range.first, match.range.last + 1)
                    }
                }
                if (middleWedge) {
                    val index = asCharSequence().indexOf("ghi")
                    insert(index, middle.text.toString())
                }
                if (suffixEnabled) {
                    append(suffix.text)
                }
            },
            decorator = demoDecorationBox,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneNumberAsYouTypeDemo() {
    BasicTextField2(
        state = rememberTextFieldState(),
        modifier = demoTextFieldModifiers,
        outputTransformation = PhoneNumberOutputTransformation(pad = false),
        inputTransformation = OnlyDigitsFilter,
        decorator = demoDecorationBox,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneNumberFullTemplateDemo() {
    BasicTextField2(
        state = rememberTextFieldState(),
        modifier = demoTextFieldModifiers,
        // Monospace prevents the template from moving while characters are entered.
        textStyle = TextStyle(fontFamily = FontFamily.Monospace),
        outputTransformation = PhoneNumberOutputTransformation(pad = true),
        inputTransformation = OnlyDigitsFilter,
        decorator = demoDecorationBox,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Stable
private data class PhoneNumberOutputTransformation(
    private val pad: Boolean
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        if (pad) {
            // Pad the text with placeholder chars if too short.
            // (___) ___-____
            val padCount = 10 - length
            repeat(padCount) {
                append('_')
            }
        }

        // (123) 456-7890
        if (length > 0) insert(0, "(")
        if (length > 4) insert(4, ") ")
        if (length > 9) insert(9, "-")
    }
}

@OptIn(ExperimentalFoundationApi::class)
private object OnlyDigitsFilter : InputTransformation {
    override fun transformInput(
        originalValue: TextFieldCharSequence,
        valueWithChanges: TextFieldBuffer
    ) {
        if ("""\D""".toRegex().containsMatchIn(valueWithChanges.asCharSequence())) {
            valueWithChanges.revertAllChanges()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val demoDecorationBox = TextFieldDecorator { innerField ->
    Box(Modifier.padding(16.dp)) {
        innerField()
    }
}
