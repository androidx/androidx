/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text.demos

import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Text
import androidx.ui.core.px
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.Column
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.LayoutSize
import androidx.ui.text.AnnotatedString
import androidx.ui.text.ParagraphStyle
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextIndent

@Composable
fun MultiParagraphDemo() {
    VerticalScroller {
        Column(
            mainAxisSize = LayoutSize.Expand,
            crossAxisAlignment = CrossAxisAlignment.Start
        ) {
            TagLine(tag = "multiple paragraphs basic")
            TextDemoParagraph()
            TagLine(tag = "multiple paragraphs TextAlign")
            TextDemoParagraphTextAlign()
            TagLine(tag = "multiple paragraphs line height")
            TextDemoParagraphLineHeight()
            TagLine(tag = "multiple paragraphs TextIndent")
            TextDemoParagraphIndent()
            TagLine(tag = "multiple paragraphs TextDirection")
            TextDemoParagraphTextDirection()
        }
    }
}

@Composable
fun TextDemoParagraph() {
    val text1 = "paragraph1 paragraph1 paragraph1 paragraph1 paragraph1"
    val text2 = "paragraph2 paragraph2 paragraph2 paragraph2 paragraph2"
    Text(
        text = AnnotatedString(
            text = text1 + text2,
            textStyles = listOf(),
            paragraphStyles = listOf(
                AnnotatedString.Item(ParagraphStyle(), text1.length, text1.length)
            )
        ),
        style = TextStyle(fontSize = fontSize8)
    )
}

@Composable
fun TextDemoParagraphTextAlign() {
    var text = ""
    val paragraphStyles = mutableListOf<AnnotatedString.Item<ParagraphStyle>>()
    TextAlign.values().map { textAlign ->
        val str = List(4) { "TextAlign.$textAlign" }.joinToString(" ")
        val paragraphStyle = ParagraphStyle(textAlign = textAlign)
        Pair(str, paragraphStyle)
    }.forEach { (str, paragraphStyle) ->
        paragraphStyles.add(
            AnnotatedString.Item(
                paragraphStyle,
                text.length,
                text.length + str.length
            )
        )
        text += str
    }

    Text(
        text = AnnotatedString(
            text = text,
            textStyles = listOf(),
            paragraphStyles = paragraphStyles
        ),
        style = TextStyle(fontSize = fontSize8)
    )
}

@Composable
fun TextDemoParagraphLineHeight() {
    val text1 = "LineHeight=1.0f LineHeight=1.0f LineHeight=1.0f LineHeight=1.0f"
    val text2 = "LineHeight=1.5f LineHeight=1.5f LineHeight=1.5f LineHeight=1.5f"
    val text3 = "LineHeight=3.0f LineHeight=3.0f LineHeight=3.0f LineHeight=3.0f"

    Text(
        text = AnnotatedString(
            text = text1 + text2 + text3,
            textStyles = listOf(),
            paragraphStyles = listOf(
                AnnotatedString.Item(
                    ParagraphStyle(lineHeight = 1.0f),
                    0,
                    text1.length
                ),
                AnnotatedString.Item(
                    ParagraphStyle(lineHeight = 1.5f),
                    text1.length,
                    text1.length + text2.length
                ),
                AnnotatedString.Item(
                    ParagraphStyle(lineHeight = 2f),
                    text1.length + text2.length,
                    text1.length + text2.length + text3.length
                )
            )
        ),
        style = TextStyle(fontSize = fontSize8)
    )
}

@Composable
fun TextDemoParagraphIndent() {
    val text1 = "TextIndent firstLine TextIndent firstLine TextIndent firstLine"
    val text2 = "TextIndent restLine TextIndent restLine TextIndent restLine"

    Text(
        text = AnnotatedString(
            text = text1 + text2,
            textStyles = listOf(),
            paragraphStyles = listOf(
                AnnotatedString.Item(
                    ParagraphStyle(textIndent = TextIndent(firstLine = 100.px)),
                    0,
                    text1.length
                ),
                AnnotatedString.Item(
                    ParagraphStyle(textIndent = TextIndent(restLine = 100.px)),
                    text1.length,
                    text1.length + text2.length
                )
            )
        ),
        style = TextStyle(fontSize = fontSize8)
    )
}

@Composable
fun TextDemoParagraphTextDirection() {
    val ltrText = "Hello World! Hello World! Hello World! Hello World! Hello World!"
    val rtlText = "مرحبا بالعالم مرحبا بالعالم مرحبا بالعالم مرحبا بالعالم مرحبا بالعالم"
    Text(
        text = AnnotatedString(
            text = ltrText + rtlText,
            textStyles = listOf(),
            paragraphStyles = listOf(
                AnnotatedString.Item(
                    ParagraphStyle(),
                    0,
                    ltrText.length
                ),
                AnnotatedString.Item(
                    ParagraphStyle(),
                    ltrText.length,
                    ltrText.length + rtlText.length
                )
            )
        ),
        style = TextStyle(fontSize = fontSize8)
    )
}
