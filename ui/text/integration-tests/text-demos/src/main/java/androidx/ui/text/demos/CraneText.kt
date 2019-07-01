/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Span
import androidx.ui.core.Text
import androidx.ui.core.px
import androidx.ui.core.selection.Selection
import androidx.ui.core.selection.SelectionContainer
import androidx.ui.core.selection.SelectionMode
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.BaselineShift
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.window.Locale
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.layout.Column
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.Row
import androidx.ui.layout.VerticalScroller
import androidx.ui.painting.ParagraphStyle
import androidx.ui.painting.Shadow
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.TextOverflow
import androidx.compose.composer
import androidx.ui.core.Sp
import androidx.ui.core.sp

val displayText = "Text Demo"
val displayTextChinese = "文本演示"
val displayTextArabic = "عرض النص"
val displayTextHindi = "पाठ डेमो"
val fontSize4: Sp = 16.sp
val fontSize6: Sp = 20.sp
val fontSize8: Sp = 25.sp
val fontSize10: Sp = 30.sp

@Composable
fun TextDemo() {
    CraneWrapper {
        VerticalScroller {
            Column(crossAxisAlignment = CrossAxisAlignment.Start) {
                TagLine(tag = "color, fontSize, fontWeight and fontStyle")
                TextDemoBasic()
                TagLine(tag = "Chinese, Arabic, and Hindi")
                TextDemoLanguage()
                TagLine(tag = "FontFamily: sans-serif, serif, and monospace")
                TextDemoFontFamily()
                TagLine(tag = "decoration, decorationColor and decorationStyle")
                TextDemoTextDecoration()
                TagLine(tag = "letterSpacing")
                TextDemoLetterSpacing()
                TagLine(tag = "baselineShift")
                TextDemoBaselineShift()
                TagLine(tag = "lineHeight")
                TextDemoHeight()
                TagLine(tag = "background")
                TextDemoBackground()
                TagLine(tag = "Locale: Japanese, Simplified and Traditional Chinese")
                TextDemoLocale()
                TagLine(tag = "textAlign and textDirection")
                TextDemoTextAlign()
                TagLine(tag = "softWrap: on and off")
                TextDemoSoftWrap()
                TagLine(tag = "textScaleFactor: default and 2.0")
                TextDemoTextScaleFactor()
                TagLine(tag = "TextOverFlow: Fade")
                TexDemoTextOverflowFade()
                TagLine(tag = "shadow")
                TextDemoShadowEffect()
                TagLine(tag = "selection")
                TextDemoSelection()
                TagLine(tag = "selection with string input")
                TextDemoSelectionWithStringInput()
                TagLine(tag = "selection in 2D Array Vertical")
                TextDemoSelection2DArrayVertical()
                TagLine(tag = "selection in 2D Array Horizontal")
                TextDemoSelection2DArrayHorizontal()
                TagLine(tag = "composable textspan")
                TextDemoComposableTextSpan()
                TagLine(tag = "fontSizeScale")
                TextDemoFontSizeScale()
            }
        }
    }
}

@Composable
fun TagLine(tag: String) {
    Text {
        Span(text = "\n", style = TextStyle(fontSize = fontSize8))
        Span(
            text = tag,
            style = TextStyle(color = Color(0xFFAAAAAA.toInt()), fontSize = fontSize6)
        )
    }
}

@Composable
fun SecondTagLine(tag: String) {
    Text {
        Span(
            text = tag,
            style = TextStyle(color = Color(0xFFAAAAAA.toInt()), fontSize = fontSize4)
        )
    }
}

@Composable
fun TextDemoBasic() {
    // This group of text widgets show different color, fontSize, fontWeight and fontStyle in
    // English.
    Text {
        Span(
            text = "$displayText   ",
            style = TextStyle(
                color = Color(0xFFFF0000.toInt()),
                fontSize = fontSize6,
                fontWeight = FontWeight.w200,
                fontStyle = FontStyle.Italic
            )
        )

        Span(
            text = "$displayText   ",
            style = TextStyle(
                color = Color(0xFF00FF00.toInt()),
                fontSize = fontSize8,
                fontWeight = FontWeight.w500,
                fontStyle = FontStyle.Normal
            )
        )

        Span(
            text = displayText,
            style = TextStyle(
                color = Color(0xFF0000FF.toInt()),
                fontSize = fontSize10,
                fontWeight = FontWeight.w800,
                fontStyle = FontStyle.Normal
            )
        )
    }
}

@Composable
fun TextDemoLanguage() {
    // This group of text widgets show different color, fontSize, fontWeight and fontStyle in
    // Chinese, Arabic, and Hindi.
    Text {
        Span(
            text = "$displayTextChinese   ",
            style = TextStyle(
                color = Color(0xFFFF0000.toInt()),
                fontSize = fontSize6,
                fontWeight = FontWeight.w200,
                fontStyle = FontStyle.Italic
            )
        )

        Span(
            text = "$displayTextArabic   ",
            style = TextStyle(
                color = Color(0xFF00FF00.toInt()),
                fontSize = fontSize8,
                fontWeight = FontWeight.w500,
                fontStyle = FontStyle.Normal
            )
        )

        Span(
            text = displayTextHindi,
            style = TextStyle(
                color = Color(0xFF0000FF.toInt()),
                fontSize = fontSize10,
                fontWeight = FontWeight.w800,
                fontStyle = FontStyle.Normal
            )
        )
    }
}

@Composable
fun TextDemoFontFamily() {
    // This group of text widgets show different fontFamilies in English.
    Text {
        Span(
            text = "$displayText   ", style = TextStyle(
                fontSize = fontSize8,
                fontFamily = FontFamily("sans-serif")
            )
        )

        Span(
            text = "$displayText   ", style = TextStyle(
                fontSize = fontSize8,
                fontFamily = FontFamily("serif")
            )
        )

        Span(
            text = displayText, style = TextStyle(
                fontSize = fontSize8,
                fontFamily = FontFamily("monospace")
            )
        )
    }
}

@Composable
fun TextDemoTextDecoration() {
    // This group of text widgets show different decoration, decorationColor and decorationStyle.
    Text {
        Span(
            text = displayText, style = TextStyle(
                fontSize = fontSize8,
                decoration = TextDecoration.LineThrough
            )
        )

        Span(
            text = "$displayText\n", style = TextStyle(
                fontSize = fontSize8,
                decoration = TextDecoration.Underline
            )
        )

        Span(
            text = displayText, style = TextStyle(
                fontSize = fontSize8,
                decoration = TextDecoration.combine(
                    listOf(
                        TextDecoration.Underline,
                        TextDecoration.LineThrough
                    )
                )
            )
        )
    }
}

@Composable
fun TextDemoLetterSpacing() {
    // This group of text widgets show different letterSpacing.
    Text {
        Span(text = "$displayText   ", style = TextStyle(fontSize = fontSize8))
        Span(text = displayText, style = TextStyle(fontSize = fontSize8, letterSpacing = 0.5f))
    }
}

@Composable
fun TextDemoBaselineShift() {
    Text {
        Span(text = displayText, style = TextStyle(fontSize = fontSize8)) {

            Span(
                text = "superscript",
                style = TextStyle(
                    baselineShift = BaselineShift.Superscript,
                    fontSize = fontSize4
                )
            ) {
                Span(
                    text = "subscript",
                    style = TextStyle(
                        baselineShift = BaselineShift.Subscript,
                        fontSize = fontSize4
                    )
                )
            }
        }
    }
}

@Composable
fun TextDemoHeight() {
    // This group of text widgets show different height.
    Row {
        Text {
            Span(
                text = "$displayText\n$displayText   ",
                style = TextStyle(fontSize = fontSize8)
            )
        }
        Text(
            paragraphStyle = ParagraphStyle(
                lineHeight = 2.0f
            )
        ) {
            Span(
                text = "$displayText\n$displayText   ",
                style = TextStyle(
                    fontSize = fontSize8
                )
            )
        }
    }
}

@Composable
fun TextDemoBackground() {
    // This group of text widgets show different background.
    Text {
        Span(
            text = "$displayText   ",
            style = TextStyle(
                fontSize = fontSize8,
                background = Color(0xFFFF0000.toInt())
            )
        )

        Span(
            text = "$displayText   ",
            style = TextStyle(
                fontSize = fontSize8,
                background = Color(0xFF00FF00.toInt())
            )
        )

        Span(
            text = displayText,
            style = TextStyle(
                fontSize = fontSize8,
                background = Color(0xFF0000FF.toInt())
            )
        )
    }
}

@Composable
fun TextDemoLocale() {
    // This group of text widgets show different Locales of the same Unicode codepoint.
    val text = "\u82B1"
    Text {
        Span(
            text = "$text   ",
            style = TextStyle(
                fontSize = fontSize8,
                locale = Locale(_languageCode = "ja", _countryCode = "JP")
            )
        )

        Span(
            text = "$text   ",
            style = TextStyle(
                fontSize = fontSize8,
                locale = Locale(_languageCode = "zh", _countryCode = "CN")
            )
        )

        Span(
            text = text,
            style = TextStyle(
                fontSize = fontSize8,
                locale = Locale(_languageCode = "zh", _countryCode = "TW")
            )
        )
    }
}

@Composable
fun TextDemoTextAlign() {
    // This group of text widgets show different TextAligns: LEFT, RIGHT, CENTER, JUSTIFY, START for
    // LTR and RTL, END for LTR and RTL.
    var text: String = ""
    for (i in 1..10) {
        text = "$text$displayText "
    }
    Column(crossAxisAlignment = CrossAxisAlignment.Start) {
        SecondTagLine(tag = "textAlign = TextAlign.Left")
        Text(paragraphStyle = ParagraphStyle(textAlign = TextAlign.Left)) {
            Span(text = displayText, style = TextStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlign = TextAlign.Right")
        Text(paragraphStyle = ParagraphStyle(textAlign = TextAlign.Right)) {
            Span(text = displayText, style = TextStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlign = TextAlign.Center")
        Text(paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center)) {
            Span(text = displayText, style = TextStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlign = default and TextAlign.Justify")
        Text {
            Span(
                text = text,
                style = TextStyle(
                    fontSize = fontSize8,
                    color = Color(0xFFFF0000.toInt())
                )
            )
        }
        Text(paragraphStyle = ParagraphStyle(textAlign = TextAlign.Justify)) {
            Span(
                text = text,
                style = TextStyle(
                    fontSize = fontSize8,
                    color = Color(0xFF0000FF.toInt())
                )
            )
        }
        SecondTagLine(tag = "textAlgin = TextAlign.Start for Ltr")
        Text(paragraphStyle = ParagraphStyle(textAlign = TextAlign.Start)) {
            Span(text = displayText, style = TextStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlgin = TextAlign.Start for Rtl")
        Text(
            paragraphStyle = ParagraphStyle(
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Start
            )
        ) {
            Span(text = displayText, style = TextStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlgin = TextAlign.End for Ltr")
        Text(paragraphStyle = ParagraphStyle(textAlign = TextAlign.End)) {
            Span(text = displayText, style = TextStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlgin = TextAlign.End for Rtl")
        Text(
            paragraphStyle = ParagraphStyle(
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.End
            )
        ) {
            Span(text = displayText, style = TextStyle(fontSize = fontSize8))
        }
    }
}

@Composable
fun TextDemoSoftWrap() {
    // This group of text widgets show difference between softWrap is true and false.
    var text: String = ""
    for (i in 1..10) {
        text = "$text$displayText"
    }
    val textStyle = TextStyle(fontSize = fontSize8, color = Color(0xFFFF0000.toInt()))

    Column(crossAxisAlignment = CrossAxisAlignment.Start) {
        Text {
            Span(text = text, style = textStyle)
        }
        Text(softWrap = false) {
            Span(text = text, style = textStyle)
        }
    }
}

// TODO(Migration/qqd): Impelement text demo for overflow and maxLines.
@Composable
fun TextDemoOverflow() {
}

@Composable
fun TextDemoMaxLines() {
}

@Composable
fun TextDemoTextScaleFactor() {
    // This group of text widgets show the different textScaleFactor.
    Column(crossAxisAlignment = CrossAxisAlignment.Start) {
        Text {
            Span(text = displayText, style = TextStyle(fontSize = fontSize8))
        }

        Text(textScaleFactor = 2.0f) {
            Span(text = displayText, style = TextStyle(fontSize = fontSize8))
        }
    }
}

@Composable
fun TexDemoTextOverflowFade() {
    var text = ""
    for (i in 1..15) {
        text = text + displayText
    }
    val textSytle = TextStyle(fontSize = fontSize8, color = Color(0xFFFF0000.toInt()))
    SecondTagLine(tag = "horizontally fading edge")
    Text(
        maxLines = 1,
        overflow = TextOverflow.Fade,
        softWrap = false
    ) {
        Span(text = text, style = textSytle)
    }
    SecondTagLine(tag = "vertically fading edge")
    Text(
        maxLines = 3,
        overflow = TextOverflow.Fade
    ) {
        Span(text = text, style = textSytle)
    }
}

@Composable
fun TextDemoShadowEffect() {
    val shadow = Shadow(
        Color(0xFFE0A0A0.toInt()),
        Offset(5f, 5f),
        blurRadius = 5.px
    )
    Text {
        Span(text = "text with ", style = TextStyle(fontSize = fontSize8)) {
            Span(text = "shadow!", style = TextStyle(shadow = shadow))
        }
    }
}

@Composable
fun TextDemoSelection() {
    val selection = +state<Selection?> { null }
    SelectionContainer(
        selection = selection.value,
        onSelectionChange = { selection.value = it }) {
        Text {
            Span(
                style = TextStyle(
                    color = Color(0xFFFF0000.toInt()),
                    fontSize = fontSize6,
                    fontWeight = FontWeight.w200,
                    fontStyle = FontStyle.Italic
                )
            ) {
                Span(text = "$displayText   ")
                Span(text = "$displayTextChinese   ")
                Span(
                    text = displayTextHindi,
                    style = TextStyle(
                        color = Color(0xFF0000FF.toInt()),
                        fontSize = fontSize10,
                        fontWeight = FontWeight.w800,
                        fontStyle = FontStyle.Normal
                    )
                )
                Span(
                    text = "\n先帝创业未半而中道崩殂，今天下三分，益州疲弊，此诚危急存亡之秋也。",
                    style = TextStyle(locale = Locale("zh", "CN"))
                )
                Span(
                    text = "\nまず、現在天下が魏・呉・蜀に分れており、そのうち蜀は疲弊していることを指摘する。",
                    style = TextStyle(locale = Locale("ja", "JP"))
                )
            }
        }
    }
}

@Composable
fun TextDemoSelectionWithStringInput() {
    val selection = +state<Selection?> { null }
    SelectionContainer(
        selection = selection.value,
        onSelectionChange = { selection.value = it }) {
        Text(
            text = "$displayText    $displayTextChinese    $displayTextHindi",
            style = TextStyle(
                color = Color(0xFFFF0000.toInt()),
                fontSize = fontSize6,
                fontWeight = FontWeight.w200,
                fontStyle = FontStyle.Italic
            )
        )
    }
}

@Composable
fun TextDemoSelection2DArrayVertical() {
    var text = ""
    for (i in 1..3) {
        text = "$text$displayText" + "\n"
    }

    val colorList = listOf(
        Color(0xFFFF0000.toInt()),
        Color(0xFF00FF00.toInt()),
        Color(0xFF0000FF.toInt()),
        Color(0xFF00FFFF.toInt()),
        Color(0xFFFF00FF.toInt()),
        Color(0xFFFFFF00.toInt()),
        Color(0xFF0000FF.toInt()),
        Color(0xFF00FF00.toInt()),
        Color(0xFFFF0000.toInt())
        )

    val selection = +state<Selection?> { null }
    SelectionContainer(
        selection = selection.value,
        onSelectionChange = { selection.value = it }) {
        Column {
            for (i in 0..2) {
                Row {
                    for (j in 0..2) {
                        Text {
                            Span(
                                text = text,
                                style = TextStyle(
                                    color = colorList[i * 3 + j],
                                    fontSize = fontSize6
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextDemoSelection2DArrayHorizontal() {
    var text = ""
    for (i in 1..3) {
        text = "$text$displayText" + "\n"
    }

    val colorList = listOf(
        Color(0xFFFF0000.toInt()),
        Color(0xFF00FF00.toInt()),
        Color(0xFF0000FF.toInt()),
        Color(0xFF00FFFF.toInt()),
        Color(0xFFFF00FF.toInt()),
        Color(0xFFFFFF00.toInt()),
        Color(0xFF0000FF.toInt()),
        Color(0xFF00FF00.toInt()),
        Color(0xFFFF0000.toInt())
    )

    val selection = +state<Selection?> { null }
    SelectionContainer(
        selection = selection.value,
        onSelectionChange = { selection.value = it },
        mode = SelectionMode.Horizontal) {
        Column {
            for (i in 0..2) {
                Row {
                    for (j in 0..2) {
                        Text {
                            Span(
                                text = text,
                                style = TextStyle(
                                    color = colorList[i * 3 + j],
                                    fontSize = fontSize6
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextDemoComposableTextSpan() {
    Text {
        Span(text = "This is a ", style = TextStyle(fontSize = fontSize8)) {
            Span(text = "composable ", style = TextStyle(fontStyle = FontStyle.Italic))
            val color1 = Color(0xFFEF50AD.toInt())
            val color2 = Color(0xFF10AF52.toInt())
            val text = "TextSpan"
            text.forEachIndexed { index, ch ->
                val color = lerp(color1, color2, index.toFloat() / text.lastIndex)
                Span(text = "$ch", style = TextStyle(color = color))
            }
        }
    }
}

@Composable
fun TextDemoFontSizeScale() {
    Text {
        Span(style = TextStyle(fontSize = fontSize8)) {
            for (i in 4..12 step 4) {
                val scale = i * 0.1f
                Span("fontSizeScale=$scale\n", style = TextStyle(fontSizeScale = scale))
            }
        }
    }
}
