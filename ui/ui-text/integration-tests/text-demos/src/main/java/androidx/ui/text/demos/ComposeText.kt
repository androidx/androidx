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
import androidx.ui.core.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shadow
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.text.AnnotatedString
import androidx.ui.text.LocaleList
import androidx.ui.text.SpanStyle
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.fontFamily
import androidx.ui.text.samples.BaselineShiftSample
import androidx.ui.text.samples.FontFamilyCursiveSample
import androidx.ui.text.samples.FontFamilyMonospaceSample
import androidx.ui.text.samples.FontFamilySansSerifSample
import androidx.ui.text.samples.FontFamilySerifSample
import androidx.ui.text.samples.ParagraphStyleAnnotatedStringsSample
import androidx.ui.text.samples.ParagraphStyleSample
import androidx.ui.text.samples.TextDecorationCombinedSample
import androidx.ui.text.samples.TextDecorationLineThroughSample
import androidx.ui.text.samples.TextDecorationUnderlineSample
import androidx.ui.text.samples.TextStyleSample
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.em
import androidx.ui.unit.px
import androidx.ui.unit.sp

const val displayText = "Text Demo"
const val displayTextChinese = "文本演示"
const val displayTextArabic = "عرض النص"
const val displayTextHindi = "पाठ डेमो"
val fontSize4 = 16.sp
val fontSize6 = 20.sp
val fontSize8 = 25.sp
val fontSize10 = 30.sp

@Composable
fun TextDemo() {
    VerticalScroller {
        Column {
            TagLine(tag = "color, fontSize, fontWeight and fontStyle")
            TextDemoBasic()
            TagLine(tag = "color, fontSize, fontWeight, fontFamily, fontStyle, letterSpacing, " +
                    "background, decoration")
            TextDemoComplexStyling()
            TagLine(tag = "Chinese, Arabic, and Hindi")
            TextDemoLanguage()
            TagLine(tag = "FontFamily generic names")
            TextDemoFontFamily()
            TagLine(tag = "FontFamily default values")
            TextDemoFontFamilyDefaultValues()
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
            TagLine(tag = "shadow")
            TextDemoShadowEffect()
            TagLine(tag = "fontSizeScale")
            TextDemoFontSizeScale()
            TagLine(tag = "complex paragraph styling")
            TextDemoParagraphStyling()
        }
    }
}

@Composable
fun TagLine(tag: String) {
    Text(
        style = TextStyle(fontSize = fontSize8),
        text = AnnotatedString {
            append("\n")
            pushStyle(
                style = SpanStyle(
                    color = Color(0xFFAAAAAA),
                    fontSize = fontSize6
                )
            )
            append(tag)
        }
    )
}

@Composable
fun SecondTagLine(tag: String) {
    Text(
        text = AnnotatedString {
            pushStyle(
                style = SpanStyle(
                    color = Color(0xFFAAAAAA),
                    fontSize = fontSize4
                )
            )
            append(tag)
            popStyle()
        }
    )
}

@Composable
fun TextDemoBasic() {
    // This group of text composables show different color, fontSize, fontWeight and fontStyle in
    // English.
    Text(text = AnnotatedString {
        pushStyle(
            SpanStyle(
                color = Color(0xFFFF0000),
                fontSize = fontSize6,
                fontWeight = FontWeight.W200,
                fontStyle = FontStyle.Italic
            )
        )
        append("$displayText   ")
        popStyle()

        pushStyle(
            SpanStyle(
                color = Color(0xFF00FF00),
                fontSize = fontSize8,
                fontWeight = FontWeight.W500,
                fontStyle = FontStyle.Normal
            )
        )
        append("$displayText   ")
        popStyle()

        pushStyle(
            SpanStyle(
                color = Color(0xFF0000FF),
                fontSize = fontSize10,
                fontWeight = FontWeight.W800,
                fontStyle = FontStyle.Normal
            )
        )
        append(displayText)
        popStyle()
    })
}

@Composable
fun TextDemoComplexStyling() {
    TextStyleSample()
}

@Composable
fun TextDemoLanguage() {
    // This group of text composables show different color, fontSize, fontWeight and fontStyle in
    // Chinese, Arabic, and Hindi.
    Text(text = AnnotatedString {
        pushStyle(
            style = SpanStyle(
                color = Color(0xFFFF0000),
                fontSize = fontSize6,
                fontWeight = FontWeight.W200,
                fontStyle = FontStyle.Italic
            )
        )
        append("$displayTextChinese   ")
        popStyle()

        pushStyle(
            style = SpanStyle(
                color = Color(0xFF00FF00),
                fontSize = fontSize8,
                fontWeight = FontWeight.W500,
                fontStyle = FontStyle.Normal
            )
        )
        append("$displayTextArabic   ")
        popStyle()

        pushStyle(
            style = SpanStyle(
                color = Color(0xFF0000FF),
                fontSize = fontSize10,
                fontWeight = FontWeight.W800,
                fontStyle = FontStyle.Normal
            )
        )
        append(displayTextHindi)
        popStyle()
    })
}

@Composable
fun TextDemoFontFamily() {
    // This group of text composables show different fontFamilies in English.
    Text(AnnotatedString {
        pushStyle(
            style = SpanStyle(
                fontSize = fontSize8,
                fontFamily = FontFamily.SansSerif
            )
        )
        append("$displayText sans-serif\n")
        popStyle()

        pushStyle(
            style = SpanStyle(
                fontSize = fontSize8,
                fontFamily = FontFamily.Serif
            )
        )
        append("$displayText serif\n")
        popStyle()

        pushStyle(
            style = SpanStyle(
                fontSize = fontSize8,
                fontFamily = FontFamily.Monospace
            )
        )
        append("$displayText monospace")
        popStyle()
    })
}

@Composable
fun TextDemoFontFamilyDefaultValues() {
    // This group of text composables show the default font families in English.
    FontFamilySerifSample()
    FontFamilySansSerifSample()
    FontFamilyMonospaceSample()
    FontFamilyCursiveSample()
}

@Composable
fun TextDemoTextDecoration() {
    // This group of text composables show different decoration, decorationColor and decorationStyle.
    TextDecorationLineThroughSample()
    TextDecorationUnderlineSample()
    TextDecorationCombinedSample()
}

@Composable
fun TextDemoLetterSpacing() {
    // This group of text composables show different letterSpacing.
    Text(text = AnnotatedString {
        pushStyle(style = SpanStyle(fontSize = fontSize8))
        append("$displayText   ")
        popStyle()
        pushStyle(
            style = SpanStyle(
                fontSize = fontSize8,
                letterSpacing = 0.5.em
            )
        )
        append(displayText)
        popStyle()
    })
}

@Composable
fun TextDemoBaselineShift() {
    BaselineShiftSample()
}

@Composable
fun TextDemoHeight() {
    // This group of text composables show different height.
    Row(LayoutWidth.Fill) {
        Text(
            text = "$displayText\n$displayText   ",
            style = TextStyle(fontSize = fontSize8)
        )
        Text(
            text = "$displayText\n$displayText   ",
            style = TextStyle(fontSize = fontSize8, lineHeight = 50.sp)
        )
    }
}

@Composable
fun TextDemoBackground() {
    // This group of text composables show different background.
    Text(text = AnnotatedString {
        pushStyle(
            style = SpanStyle(
                fontSize = fontSize8,
                background = Color(0xFFFF0000)
            )
        )
        append("$displayText   ")
        popStyle()

        pushStyle(
            style = SpanStyle(
                fontSize = fontSize8,
                background = Color(0xFF00FF00)
            )
        )
        append("$displayText   ")
        popStyle()

        pushStyle(
            style = SpanStyle(
                fontSize = fontSize8,
                background = Color(0xFF0000FF)
            )
        )
        append(displayText)
        popStyle()
    })
}

@Composable
fun TextDemoLocale() {
    // This group of text composables show different Locales of the same Unicode codepoint.
    val text = "\u82B1"
    Text(AnnotatedString {
        pushStyle(
            style = SpanStyle(fontSize = fontSize8, localeList = LocaleList("ja-JP"))
        )
        append("$text   ")
        popStyle()

        pushStyle(
            style = SpanStyle(fontSize = fontSize8, localeList = LocaleList("zh-CN"))
        )
        append("$text   ")
        popStyle()

        pushStyle(
            style = SpanStyle(fontSize = fontSize8, localeList = LocaleList("zh-TW"))
        )
        append(text)
        popStyle()
    })
}

@Composable
fun TextDemoTextAlign() {
    // This group of text composables show different TextAligns: LEFT, RIGHT, CENTER, JUSTIFY, START for
    // LTR and RTL, END for LTR and RTL.
    var text = ""
    for (i in 1..10) {
        text = "$text$displayText "
    }
    Column(LayoutHeight.Fill) {
        SecondTagLine(tag = "textAlign = TextAlign.Left")
        Text(
            modifier = LayoutWidth.Fill,
            text = displayText,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Left)
        )

        SecondTagLine(tag = "textAlign = TextAlign.Right")
        Text(
            modifier = LayoutWidth.Fill,
            text = displayText,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Right)
        )

        SecondTagLine(tag = "textAlign = TextAlign.Center")
        Text(
            modifier = LayoutWidth.Fill,
            text = displayText,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Center)
        )

        SecondTagLine(tag = "textAlign = default and TextAlign.Justify")
        Text(
            modifier = LayoutWidth.Fill,
            text = text,
            style = TextStyle(
                fontSize = fontSize8,
                color = Color(0xFFFF0000)
            )
        )
        Text(
            modifier = LayoutWidth.Fill,
            text = text,
            style = TextStyle(
                fontSize = fontSize8,
                color = Color(0xFF0000FF),
                textAlign = TextAlign.Justify
            )
        )

        SecondTagLine(tag = "textAlign = TextAlign.Start for Ltr")
        Text(
            modifier = LayoutWidth.Fill,
            text = displayText,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Start)
        )
        SecondTagLine(tag = "textAlign = TextAlign.Start for Rtl")
        Text(
            modifier = LayoutWidth.Fill,
            text = displayTextArabic,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Start)
        )
        SecondTagLine(tag = "textAlign = TextAlign.End for Ltr")
        Text(
            modifier = LayoutWidth.Fill,
            text = displayText,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.End)
        )
        SecondTagLine(tag = "textAlign = TextAlign.End for Rtl")
        Text(
            modifier = LayoutWidth.Fill,
            text = displayTextArabic,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.End)
        )
    }
}

@Composable
fun TextDemoSoftWrap() {
    // This group of text composables show difference between softWrap is true and false.
    var text = ""
    for (i in 1..10) {
        text = "$text$displayText"
    }
    val textStyle = TextStyle(fontSize = fontSize8, color = Color(0xFFFF0000))

    Column(LayoutHeight.Fill) {
        Text(text = text, style = textStyle)
        Text(text = text, style = textStyle, softWrap = false)
    }
}

@Composable
fun TextDemoShadowEffect() {
    val shadow = Shadow(
        Color(0xFFE0A0A0),
        Offset(5f, 5f),
        blurRadius = 5.px
    )
    Text(
        style = TextStyle(fontSize = fontSize8),
        text = AnnotatedString {
            append("text with ")
            pushStyle(style = SpanStyle(shadow = shadow))
            append("shadow!")
            popStyle()
        }
    )
}

@Composable
fun TextDemoFontSizeScale() {
    Text(
        style = TextStyle(fontSize = fontSize8),
        text = AnnotatedString {
            for (i in 4..12 step 4) {
                val scale = i * 0.1f
                pushStyle(style = SpanStyle(fontSize = scale.em))
                append("fontSizeScale=$scale\n")
                popStyle()
            }
        }
    )
}

@Composable
fun TextDemoParagraphStyling() {
    ParagraphStyleSample()
    ParagraphStyleAnnotatedStringsSample()
}
