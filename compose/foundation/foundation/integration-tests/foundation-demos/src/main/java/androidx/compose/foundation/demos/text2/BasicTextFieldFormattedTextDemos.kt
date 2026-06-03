/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.samples.BasicTextFieldTrackedRangeSample
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.ExpandPolicy
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicTextFieldFormattedTextDemos() {
    if (ComposeFoundationFlags.isBasicTextFieldStyledTextEnabled) {
        Column(Modifier.imePadding().verticalScroll(rememberScrollState())) {
            TagLine(tag = "BasicTextField with SpanStyles")
            TextFieldWithSpanStyles()

            TagLine(tag = "BasicTextField with ParagraphStyles")
            TextFieldWithParagraphStyles()

            TagLine(
                tag = "BasicTextField that colors digits Cyan non-digits Red in InputTransformation"
            )
            TextFieldApplyStyleInInputTransformation()

            TagLine(tag = "BasicTextField that highlights whitespace in OutputTransformation")
            TextFieldApplyStyleInOutputTransformation()

            TagLine("TrackedRange Sample: BasicTextField with Markdown-like **bold** formatting")
            BasicTextFieldTrackedRangeSample()
        }
    } else {
        Text(
            "Please enable ComposeFoundationFlags.isBasicTextFieldStyledTextEnabled to view this Demo."
        )
    }
}

@Composable
fun TextFieldWithSpanStyles() {
    val state = remember {
        TextFieldState().apply {
            edit {
                append("formatted text")
                addStyle(SpanStyle(color = Color.Cyan), TextRange(0, 9), ExpandPolicy.AtEnd)
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    TextRange(10, 14),
                    ExpandPolicy.AtEnd,
                )
            }
        }
    }
    BasicTextField(state, demoTextFieldModifiers, textStyle = LocalTextStyle.current)
}

@Composable
fun TextFieldWithParagraphStyles() {
    val state = remember {
        TextFieldState().apply {
            edit {
                append("list: item1\nitem2\nitem3")
                addStyle(
                    ParagraphStyle(textIndent = TextIndent(firstLine = 16.sp, restLine = 16.sp)),
                    TextRange(6, 23),
                    ExpandPolicy.AtEnd,
                )
            }
        }
    }
    BasicTextField(state, demoTextFieldModifiers, textStyle = LocalTextStyle.current)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextFieldApplyStyleInInputTransformation() {
    val state = remember { TextFieldState() }
    BasicTextField(
        state = state,
        modifier = demoTextFieldModifiers,
        textStyle = LocalTextStyle.current,
        inputTransformation = {
            for (index in 0 until changes.changeCount) {
                val range = changes.getRange(index)
                val text = asCharSequence()
                var start = range.min
                while (start < range.max) {
                    var idx = start
                    val isDigit = text[idx].isDigit()
                    while (idx < range.max && text[idx].isDigit() == isDigit) {
                        ++idx
                    }
                    if (isDigit) {
                        addStyle(
                            SpanStyle(color = Color.Cyan),
                            TextRange(start, idx),
                            ExpandPolicy.AtEnd,
                        )
                    } else {
                        addStyle(
                            SpanStyle(color = Color.Red),
                            TextRange(start, idx),
                            ExpandPolicy.AtEnd,
                        )
                    }
                    start = idx + 1
                }
            }
        },
    )
}

@Composable
fun TextFieldApplyStyleInOutputTransformation() {
    val state = remember { TextFieldState() }
    BasicTextField(
        state = state,
        modifier = demoTextFieldModifiers,
        textStyle = LocalTextStyle.current,
        outputTransformation = {
            val text = asCharSequence()
            var start = 0
            while (start < text.length) {
                var idx = start
                while (idx < text.length && text[idx].isWhitespace()) {
                    ++idx
                }
                if (idx > start) {
                    addStyle(
                        SpanStyle(background = Color.Cyan),
                        TextRange(start, idx),
                        ExpandPolicy.AtEnd,
                    )
                }
                start = idx + 1
            }
        },
    )
}
