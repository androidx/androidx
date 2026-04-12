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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach

val textBorderModifier = Modifier.border(1.dp, Color.LightGray).padding(1.dp)
const val defaultText = "Line1\nLine2 text1 text2\nLine3"

@Preview
@Composable
fun TextSelectionDemo() {
    LazyColumn(modifier = Modifier.padding(32.dp, 0.dp)) {
        item {
            Text(
                modifier = textBorderModifier,
                text =
                    buildAnnotatedString {
                        appendWithColor(Color.Green, "Green")
                        append(" borders represent a ")
                        appendCode("SelectionContainer")
                        append(". ")
                        appendWithColor(Color.Red, "Red")
                        append(" borders represent a ")
                        appendCode("DisableSelection")
                        append(". ")
                        appendWithColor(Color.Gray, "Light gray")
                        append(" borders represent a single ")
                        appendCode("Text/BasicText")
                        append(".")
                    },
            )
        }
        item {
            TagLine(tag = "Single BasicText")
            TextDemoSingleTextSelection()
        }
        item {
            TagLine(tag = "One BasicText per line")
            TextDemoMultiTextSelection()
        }
        item {
            TagLine(tag = "selection")
            TextDemoSelection()
        }
        item {
            TagLine(tag = "selection with string input")
            TextDemoSelectionWithStringInput()
        }
        item {
            TagLine(tag = "selection in 2D Array Vertical")
            TextDemoSelection2DArrayVertical()
        }
        item {
            TagLine(tag = "enable and disable selection")
            TextDemoSelectionEnableAndDisable()
        }
        item {
            TagLine(tag = "fix crashing of longpress in the blank area")
            OutlinedSelectionContainer {
                Text(
                    text = "Hello World\nHello",
                    modifier =
                        Modifier.fillMaxWidth()
                            .border(BorderStroke(1.dp, color = Color.Black))
                            .height(80.dp),
                )
            }
        }
        item {
            TagLine(tag = "smart selection demo(Android only)")
            OutlinedSelectionContainer {
                Text(
                    text =
                        "This is a demo for smart selection. " +
                            "Try long press selection on following text. \n" +
                            "address: 1600 Amphitheatre Pkwy, Mountain View, CA 94043 \n" +
                            "phone number: 123-456-0000\n" +
                            "email: example_email@email.com\n",
                    modifier =
                        Modifier.fillMaxWidth().border(BorderStroke(1.dp, color = Color.Black)),
                )
            }
        }
        item {
            TagLine(tag = "Clickable children")
            TextDemoClickableChildrenSelection()
        }
    }
}

@Preview
@Composable
fun TextDemoSingleTextSelection() {
    OutlinedSelectionContainer {
        BasicText(
            style = TextStyle(fontSize = fontSize8),
            text = defaultText,
            modifier = textBorderModifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
fun TextDemoMultiTextSelection() {
    val splitTexts = defaultText.split("\n")
    OutlinedSelectionContainer {
        Column {
            splitTexts.fastForEach {
                BasicText(
                    style = TextStyle(fontSize = fontSize8),
                    text = it,
                    modifier = textBorderModifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview
@Composable
fun TextDemoSelection() {
    val arabicSentence =
        "\nكلمة شين في قاموس المعاني الفوري مجال البحث مصطلحات المعجم الوسيط ،اللغة"
    OutlinedSelectionContainer {
        Text(
            modifier = textBorderModifier,
            style =
                TextStyle(
                    color = Color(0xFFFF0000),
                    fontSize = fontSize6,
                    fontWeight = FontWeight.W200,
                    fontStyle = FontStyle.Italic,
                ),
            text =
                buildAnnotatedString {
                    append(text = "$displayText   ")
                    append(text = "$displayTextArabic   ")
                    append(text = "$displayTextChinese   ")

                    withStyle(
                        SpanStyle(
                            color = Color(0xFF0000FF),
                            fontSize = fontSize10,
                            fontWeight = FontWeight.W800,
                            fontStyle = FontStyle.Normal,
                        )
                    ) {
                        append(displayTextHindi)
                    }

                    append(text = arabicSentence)

                    withStyle(SpanStyle(localeList = LocaleList("zh-CN"))) {
                        append("\n先帝创业未半而中道崩殂，今天下三分，益州疲弊，此诚危急存亡之秋也。")
                    }

                    withStyle(SpanStyle(localeList = LocaleList("ja-JP"))) {
                        append("\nまず、現在天下が魏・呉・蜀に分れており、そのうち蜀は疲弊していることを指摘する。")
                    }
                },
        )
    }
}

@Preview
@Composable
fun TextDemoSelectionWithStringInput() {
    OutlinedSelectionContainer {
        Text(
            text = "$displayText    $displayTextChinese    $displayTextHindi",
            color = Color(0xFFFF0000),
            fontSize = fontSize6,
            fontWeight = FontWeight.W200,
            fontStyle = FontStyle.Italic,
            modifier = textBorderModifier,
        )
    }
}

@Preview
@Composable
fun TextDemoSelection2DArrayVertical() {
    var text = ""
    for (i in 1..3) {
        text = "$text$displayText" + "\n"
    }

    val colorList =
        listOf(
            Color(0xFFFF0000),
            Color(0xFF00FF00),
            Color(0xFF0000FF),
            Color(0xFF00FFFF),
            Color(0xFFFF00FF),
            Color(0xFFFFFF00),
            Color(0xFF0000FF),
            Color(0xFF00FF00),
            Color(0xFFFF0000),
        )

    OutlinedSelectionContainer {
        Column(Modifier.fillMaxHeight()) {
            for (i in 0..2) {
                Row(Modifier.fillMaxWidth()) {
                    for (j in 0..2) {
                        Text(
                            text = text,
                            modifier = textBorderModifier,
                            style = TextStyle(color = colorList[i * 3 + j], fontSize = fontSize6),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TextDemoSelectionEnableAndDisable() {
    val textSelectable = "This text is selectable."
    val textNotSelectable = "This text is not selectable."
    var textEditable by remember { mutableStateOf("This text is editable.") }
    var clickCount by remember { mutableIntStateOf(0) }

    OutlinedSelectionContainer {
        Column(Modifier.fillMaxHeight()) {
            Text(
                text = textSelectable,
                modifier = textBorderModifier,
                style = TextStyle(fontSize = fontSize8),
            )
            OutlinedDisableSelection {
                Text(
                    text = textNotSelectable,
                    modifier = textBorderModifier,
                    style = TextStyle(fontSize = fontSize8),
                )
            }
            Text(
                text = textSelectable,
                modifier = textBorderModifier,
                style = TextStyle(fontSize = fontSize8),
            )
            TextField(
                value = textEditable,
                onValueChange = { textEditable = it },
                modifier = textBorderModifier,
                textStyle = TextStyle(fontSize = fontSize8),
            )
            Row {
                OutlinedDisableSelection {
                    OutlinedButton(onClick = { clickCount++ }) { Text("Clicks Count: $clickCount") }
                }
                OutlinedButton(onClick = { clickCount++ }) { Text("Clicks Count: $clickCount") }
            }
            Text(
                text = textSelectable + "\n" + textSelectable,
                modifier = textBorderModifier,
                style = TextStyle(fontSize = fontSize8),
            )
            OutlinedDisableSelection {
                Text(
                    text = textNotSelectable,
                    modifier = textBorderModifier,
                    style = TextStyle(fontSize = fontSize8),
                )
                Text(
                    text = textNotSelectable,
                    modifier = textBorderModifier,
                    style = TextStyle(fontSize = fontSize8),
                )
            }
            Text(
                text = textSelectable,
                modifier = textBorderModifier,
                style = TextStyle(fontSize = fontSize8),
            )
        }
    }
}

@Composable
fun OutlinedSelectionContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    SelectionContainer(
        modifier = modifier.border(1.dp, Color.Green).padding(1.dp),
        content = content,
    )
}

@Composable
fun OutlinedDisableSelection(content: @Composable () -> Unit) {
    Box(Modifier.border(1.dp, Color.Red).padding(1.dp)) { DisableSelection(content) }
}

@Preview
@Composable
fun TextDemoClickableChildrenSelection() {
    SelectionContainer {
        BasicText(
            "This text has a click handler. ".repeat(5),
            modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = {}) },
        )
    }
}

internal fun AnnotatedString.Builder.appendWithColor(color: Color, text: String) {
    withStyle(SpanStyle(color = color)) { append(text) }
}

internal fun AnnotatedString.Builder.appendCode(text: String) {
    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(text) }
}
