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

import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.ui.core.CraneWrapper
import androidx.ui.core.EditableText
import androidx.ui.core.EditorStyle
import androidx.ui.core.Selection
import androidx.ui.core.SelectionContainer
import androidx.ui.core.Text
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.BaselineShift
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.window.Locale
import androidx.ui.input.EditorState
import androidx.ui.graphics.Color
import androidx.ui.painting.Shadow
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.TextOverflow
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus

val displayText = "Text Demo"
val displayTextChinese = "文本演示"
val displayTextArabic = "عرض النص"
val displayTextHindi = "पाठ डेमो"
val fontSize4: Float = 40.0.toFloat()
val fontSize6: Float = 60.0.toFloat()
val fontSize7: Float = 70.0.toFloat()
val fontSize8: Float = 80.0.toFloat()
val fontSize10: Float = 100.0.toFloat()

@Composable
fun TextDemo() {
    LinearLayout(
        orientation = LinearLayout.VERTICAL,
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
    ) {
        ScrollView {
            LinearLayout(orientation = LinearLayout.VERTICAL) {
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
                TagLine(tag = "wordSpacing")
                TextDemoWordSpacing()
                TagLine(tag = "baselineShift")
                TextDemoBaselineShift()
                TagLine(tag = "height")
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
                TagLine(tag = "TextOverFlow: FADE")
                TexDemoTextOverflowFade()
                TagLine(tag = "shadow")
                TextDemoShadowEffect()
                TagLine(tag = "editing")
                EditLine()
                TagLine(tag = "selection")
                TextDemoSelection()
            }
        }
    }
}

@Composable
fun TagLine(tag: String) {
    CraneWrapper {
        Text(
            text = TextSpan(
                text = "      ",
                style = TextStyle(fontSize = fontSize8)
            )
        )
    }
    CraneWrapper {
        Text(
            text = TextSpan(
                text = tag,
                style = TextStyle(color = Color(0xFFAAAAAA.toInt()), fontSize = fontSize6)
            )
        )
    }
}

@Composable
fun SecondTagLine(tag: String) {
    CraneWrapper {
        Text(
            text = TextSpan(
                text = tag,
                style = TextStyle(color = Color(0xFFAAAAAA.toInt()), fontSize = fontSize4)
            )
        )
    }
}

@Composable
fun TextDemoBasic() {
    // This group of text widgets show different color, fontSize, fontWeight and fontStyle in
    // English.
    CraneWrapper {
        Text(
            text = TextSpan(
                children = listOf(
                    TextSpan(
                        text = displayText + "   ",
                        style = TextStyle(
                            color = Color(0xFFFF0000.toInt()),
                            fontSize = fontSize6,
                            fontWeight = FontWeight.w200,
                            fontStyle = FontStyle.italic
                        )
                    ),
                    TextSpan(
                        text = displayText + "   ",
                        style = TextStyle(
                            color = Color(0xFF00FF00.toInt()),
                            fontSize = fontSize8,
                            fontWeight = FontWeight.w500,
                            fontStyle = FontStyle.normal
                        )
                    ),
                    TextSpan(
                        text = displayText,
                        style = TextStyle(
                            color = Color(0xFF0000FF.toInt()),
                            fontSize = fontSize10,
                            fontWeight = FontWeight.w800,
                            fontStyle = FontStyle.normal
                        )
                    )
                )
            )
        )
    }
}

@Composable
fun TextDemoLanguage() {
    // This group of text widgets show different color, fontSize, fontWeight and fontStyle in
    // Chinese, Arabic, and Hindi.
    CraneWrapper {
        Text(
            text = TextSpan(
                children = listOf(
                    TextSpan(
                        text = displayTextChinese + "   ",
                        style = TextStyle(
                            color = Color(0xFFFF0000.toInt()),
                            fontSize = fontSize6,
                            fontWeight = FontWeight.w200,
                            fontStyle = FontStyle.italic
                        )
                    ),
                    TextSpan(
                        text = displayTextArabic + "   ",
                        style = TextStyle(
                            color = Color(0xFF00FF00.toInt()),
                            fontSize = fontSize8,
                            fontWeight = FontWeight.w500,
                            fontStyle = FontStyle.normal
                        )
                    ),
                    TextSpan(
                        text = displayTextHindi,
                        style = TextStyle(
                            color = Color(0xFF0000FF.toInt()),
                            fontSize = fontSize10,
                            fontWeight = FontWeight.w800,
                            fontStyle = FontStyle.normal
                        )
                    )
                )
            )
        )
    }
}

@Composable
fun TextDemoFontFamily() {
    // This group of text widgets show different fontFamilies in English.
    CraneWrapper {
        Text(
            text = TextSpan(
                children = listOf(
                    TextSpan(
                        text = displayText + "   ",
                        style = TextStyle(
                            fontSize = fontSize8,
                            fontFamily = FontFamily("sans-serif")
                        )
                    ),
                    TextSpan(
                        text = displayText + "   ",
                        style = TextStyle(
                            fontSize = fontSize8,
                            fontFamily = FontFamily("serif")
                        )
                    ),
                    TextSpan(
                        text = displayText,
                        style = TextStyle(
                            fontSize = fontSize8,
                            fontFamily = FontFamily("monospace")
                        )
                    )
                )
            )
        )
    }
}

@Composable
fun TextDemoTextDecoration() {
    // This group of text widgets show different decoration, decorationColor and decorationStyle.
    CraneWrapper {
        Text(
            text = TextSpan(
                children = listOf(
                    TextSpan(
                        text = displayText,
                        style = TextStyle(
                            fontSize = fontSize8,
                            decoration = TextDecoration.lineThrough
                        )
                    ),
                    TextSpan(
                        text = displayText + '\n',
                        style = TextStyle(
                            fontSize = fontSize8,
                            decoration = TextDecoration.underline
                        )
                    ),
                    TextSpan(
                        text = displayText,
                        style = TextStyle(
                            fontSize = fontSize8,
                            decoration = TextDecoration.combine(
                                listOf(
                                    TextDecoration.underline,
                                    TextDecoration.lineThrough
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}

@Composable
fun TextDemoLetterSpacing() {
    // This group of text widgets show different letterSpacing.
    CraneWrapper {
        Text(
            text = TextSpan(
                children = listOf(
                    TextSpan(
                        text = displayText + "   ",
                        style = TextStyle(
                            fontSize = fontSize8
                        )
                    ),
                    TextSpan(
                        text = displayText,
                        style = TextStyle(
                            fontSize = fontSize8,
                            letterSpacing = 0.5.toFloat()
                        )
                    )
                )
            )
        )
    }
}

@Composable
fun TextDemoWordSpacing() {
    // This group of text widgets show different wordSpacing.
    CraneWrapper {
        Text(
            text = TextSpan(
                children = listOf(
                    TextSpan(
                        text = displayText + "   ",
                        style = TextStyle(
                            fontSize = fontSize8
                        )
                    ),
                    TextSpan(
                        text = displayText,
                        style = TextStyle(
                            fontSize = fontSize8,
                            wordSpacing = 100.0.toFloat()
                        )
                    )
                )
            )
        )
    }
}

@Composable
fun TextDemoBaselineShift() {
    CraneWrapper {
        Text(
            text = TextSpan(
                text = displayText,
                style = TextStyle(
                    fontSize = fontSize8
                ),
                children = listOf(
                    TextSpan(
                        text = "superscript",
                        style = TextStyle(
                            baselineShift = BaselineShift.SUPERSCRIPT,
                            fontSize = fontSize4
                        ),
                        children = listOf(
                            TextSpan(
                                text = "subscript",
                                style = TextStyle(
                                    baselineShift = BaselineShift.SUBSCRIPT,
                                    fontSize = fontSize4
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}

@Composable
fun TextDemoHeight() {
    // This group of text widgets show different height.
    LinearLayout(orientation = LinearLayout.HORIZONTAL) {
        CraneWrapper {
            Text(
                text = TextSpan(
                    text = displayText + "\n" + displayText + "   ",
                    style = TextStyle(
                        fontSize = fontSize8
                    )
                )
            )
        }
        CraneWrapper {
            Text(
                text = TextSpan(
                    text = displayText + "\n" + displayText,
                    style = TextStyle(
                        fontSize = fontSize8,
                        height = 2.0.toFloat()
                    )
                )
            )
        }
    }
}

@Composable
fun TextDemoBackground() {
    // This group of text widgets show different background.
    CraneWrapper {
        Text(
            text = TextSpan(
                children = listOf(
                    TextSpan(
                        text = displayText + "   ",
                        style = TextStyle(
                            fontSize = fontSize8,
                            background = Color(0xFFFF0000.toInt())
                        )
                    ),
                    TextSpan(
                        text = displayText + "   ",
                        style = TextStyle(
                            fontSize = fontSize8,
                            background = Color(0xFF00FF00.toInt())
                        )
                    ),
                    TextSpan(
                        text = displayText,
                        style = TextStyle(
                            fontSize = fontSize8,
                            background = Color(0xFF0000FF.toInt())
                        )
                    )
                )
            )
        )
    }
}

@Composable
fun TextDemoLocale() {
    // This group of text widgets show different Locales of the same Unicode codepoint.
    val text = "\u82B1"
    CraneWrapper {
        Text(
            text = TextSpan(
                children = listOf(
                    TextSpan(
                        text = text + "   ",
                        style = TextStyle(
                            fontSize = fontSize8,
                            locale = Locale(_languageCode = "ja", _countryCode = "JP")
                        )
                    ),
                    TextSpan(
                        text = text + "   ",
                        style = TextStyle(
                            fontSize = fontSize8,
                            locale = Locale(_languageCode = "zh", _countryCode = "CN")
                        )
                    ),
                    TextSpan(
                        text = text,
                        style = TextStyle(
                            fontSize = fontSize8,
                            locale = Locale(_languageCode = "zh", _countryCode = "TW")
                        )
                    )
                )
            )
        )
    }
}

@Composable
fun TextDemoTextAlign() {
    // This group of text widgets show different TextAligns: LEFT, RIGHT, CENTER, JUSTIFY, START for
    // LTR and RTL, END for LTR and RTL.
    val textSpan = TextSpan(
        text = displayText,
        style = TextStyle(fontSize = fontSize8)
    )
    var text: String = ""
    for (i in 1..10) {
        text = text + displayText + " "
    }
    LinearLayout(orientation = LinearLayout.VERTICAL) {
        SecondTagLine(tag = "textAlgin = TextAlign.LEFT")
        CraneWrapper {
            Text(
                text = textSpan,
                textAlign = TextAlign.LEFT
            )
        }
        SecondTagLine(tag = "textAlgin = TextAlign.RIGHT")
        CraneWrapper {
            Text(
                text = textSpan,
                textAlign = TextAlign.RIGHT
            )
        }
        SecondTagLine(tag = "textAlgin = TextAlign.CENTER")
        CraneWrapper {
            Text(
                text = textSpan,
                textAlign = TextAlign.CENTER
            )
        }
        SecondTagLine(tag = "textAlgin = default and TextAlign.JUSTIFY")
        CraneWrapper {
            Text(
                text = TextSpan(
                    text = text,
                    style = TextStyle(
                        fontSize = fontSize8,
                        color = Color(0xFFFF0000.toInt())
                    )
                )
            )
        }
        CraneWrapper {
            Text(
                text = TextSpan(
                    text = text,
                    style = TextStyle(
                        fontSize = fontSize8,
                        color = Color(0xFF0000FF.toInt())
                    )
                ),
                textAlign = TextAlign.JUSTIFY
            )
        }
        SecondTagLine(tag = "textAlgin = TextAlign.START for LTR")
        CraneWrapper {
            Text(
                text = textSpan,
                textAlign = TextAlign.START
            )
        }
        SecondTagLine(tag = "textAlgin = TextAlign.START for RTL")
        CraneWrapper {
            Text(
                text = textSpan,
                textDirection = TextDirection.RTL,
                textAlign = TextAlign.START
            )
        }
        SecondTagLine(tag = "textAlgin = TextAlign.END for LTR")
        CraneWrapper {
            Text(
                text = textSpan,
                textAlign = TextAlign.END
            )
        }
        SecondTagLine(tag = "textAlgin = TextAlign.END for RTL")
        CraneWrapper {
            Text(
                text = textSpan,
                textDirection = TextDirection.RTL,
                textAlign = TextAlign.END
            )
        }
    }
}

@Composable
fun TextDemoSoftWrap() {
    // This group of text widgets show difference between softWrap is true and false.
    var text: String = ""
    for (i in 1..10) {
        text = text + displayText
    }

    LinearLayout(orientation = LinearLayout.VERTICAL) {
        CraneWrapper {
            Text(
                text = TextSpan(
                    text = text,
                    style = TextStyle(
                        fontSize = fontSize8, color = Color(0xFFFF0000.toInt())
                    )
                )
            )
        }
        CraneWrapper {
            Text(
                text = TextSpan(
                    text = text,
                    style = TextStyle(fontSize = fontSize8, color = Color(0xFF00FF00.toInt()))
                ),
                softWrap = false
            )
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
    val textSpan = TextSpan(
        text = displayText,
        style = TextStyle(fontSize = fontSize8)
    )
    LinearLayout(orientation = LinearLayout.VERTICAL) {
        CraneWrapper {
            Text(text = textSpan)
        }
        CraneWrapper {
            Text(text = textSpan, textScaleFactor = 2.0f)
        }
    }
}

@Composable
fun TexDemoTextOverflowFade() {
    var text = ""
    for (i in 1..15) {
        text = text + displayText
    }
    val textSpan = TextSpan(
        text = text,
        style = TextStyle(fontSize = fontSize8, color = Color(0xFFFF0000.toInt()))
    )

    SecondTagLine(tag = "horizontally fading edge")
    CraneWrapper {
        Text(
            text = textSpan,
            maxLines = 1,
            overflow = TextOverflow.FADE,
            softWrap = false
        )
    }
    SecondTagLine(tag = "vertically fading edge")
    CraneWrapper {
        Text(
            text = textSpan,
            maxLines = 3,
            overflow = TextOverflow.FADE
        )
    }
}

@Composable
fun TextDemoShadowEffect() {
    val shadow = Shadow(
        Color(0xFFE0A0A0.toInt()),
        Offset(5f, 5f),
        blurRadius = 5.px
    )
    val textSpan = TextSpan(
        text = "text with ",
        style = TextStyle(fontSize = fontSize8),
        children = listOf(
            TextSpan(
                text = "shadow!",
                style = TextStyle(shadow = shadow)
            )
        )
    )
    CraneWrapper {
        Text(text = textSpan)
    }
}

@Composable
fun EditLine() {
    val state = +state { EditorState() }
    CraneWrapper {
        EditableText(
            value = state.value,
            onValueChange = { state.value = it },
            editorStyle = EditorStyle(textStyle = TextStyle(fontSize = fontSize8))
        )
    }
}

fun TextDemoSelection() {
    var textSpan = TextSpan(
        children = listOf(
            TextSpan(
                text = displayText + "   ",
                style = TextStyle(
                    color = Color(0xFFFF0000.toInt()),
                    fontSize = fontSize6,
                    fontWeight = FontWeight.w200,
                    fontStyle = FontStyle.italic
                )
            ),
            TextSpan(
                text = displayTextChinese + "   ",
                style = TextStyle(
                    color = Color(0xFFFF0000.toInt()),
                    fontSize = fontSize6,
                    fontWeight = FontWeight.w200,
                    fontStyle = FontStyle.italic
                )
            ),
            TextSpan(
                text = displayTextHindi,
                style = TextStyle(
                    color = Color(0xFF0000FF.toInt()),
                    fontSize = fontSize10,
                    fontWeight = FontWeight.w800,
                    fontStyle = FontStyle.normal
                )
            ),
            TextSpan(
                text = "\n" + "先帝创业未半而中道崩殂，今天下三分，益州疲弊，此诚危急存亡之秋也。",
                style = TextStyle(
                    color = Color(0xFFFF0000.toInt()),
                    fontSize = fontSize6,
                    fontWeight = FontWeight.w200,
                    fontStyle = FontStyle.italic,
                    locale = Locale("zh", "CN")
                )
            ),
            TextSpan(
                text = "\nまず、現在天下が魏・呉・蜀に分れており、そのうち蜀は疲弊していることを指摘する。",
                style = TextStyle(
                    color = Color(0xFFFF0000.toInt()),
                    fontSize = fontSize6,
                    fontWeight = FontWeight.w200,
                    fontStyle = FontStyle.italic,
                    locale = Locale("ja", "JP")
                )
            )
        )
    )

    val selection = +state<Selection?> { null }
    CraneWrapper {
        SelectionContainer(
            selection = selection.value,
            onSelectionChange = { selection.value = it }) {
            Text(text = textSpan)
        }
    }
}
