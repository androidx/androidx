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
import androidx.ui.core.Span
import androidx.ui.core.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shadow
import androidx.ui.graphics.lerp
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.text.LocaleList
import androidx.ui.text.SpanStyle
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
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
import androidx.ui.text.style.TextOverflow
import androidx.ui.unit.em
import androidx.ui.unit.px
import androidx.ui.unit.sp

val displayText = "Text Demo"
val displayTextChinese = "文本演示"
val displayTextArabic = "عرض النص"
val displayTextHindi = "पाठ डेमो"
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
            TagLine(tag = "TextOverFlow: Fade")
            TexDemoTextOverflowFade()
            TagLine(tag = "shadow")
            TextDemoShadowEffect()
            TagLine(tag = "composable textspan")
            TextDemoComposableTextSpan()
            TagLine(tag = "fontSizeScale")
            TextDemoFontSizeScale()
            TagLine(tag = "complex paragraph styling")
            TextDemoParagraphStyling()
        }
    }
}

@Composable
fun TagLine(tag: String) {
    Text {
        Span(text = "\n", style = SpanStyle(fontSize = fontSize8))
        Span(
            text = tag,
            style = SpanStyle(
                color = Color(0xFFAAAAAA),
                fontSize = fontSize6
            )
        )
    }
}

@Composable
fun SecondTagLine(tag: String) {
    Text {
        Span(
            text = tag,
            style = SpanStyle(
                color = Color(0xFFAAAAAA),
                fontSize = fontSize4
            )
        )
    }
}

@Composable
fun TextDemoBasic() {
    // This group of text composables show different color, fontSize, fontWeight and fontStyle in
    // English.
    Text {
        Span(
            text = "$displayText   ",
            style = SpanStyle(
                color = Color(0xFFFF0000),
                fontSize = fontSize6,
                fontWeight = FontWeight.W200,
                fontStyle = FontStyle.Italic
            )
        )

        Span(
            text = "$displayText   ",
            style = SpanStyle(
                color = Color(0xFF00FF00),
                fontSize = fontSize8,
                fontWeight = FontWeight.W500,
                fontStyle = FontStyle.Normal
            )
        )

        Span(
            text = displayText,
            style = SpanStyle(
                color = Color(0xFF0000FF),
                fontSize = fontSize10,
                fontWeight = FontWeight.W800,
                fontStyle = FontStyle.Normal
            )
        )
    }
}

@Composable
fun TextDemoComplexStyling() {
    TextStyleSample()
}

@Composable
fun TextDemoLanguage() {
    // This group of text composables show different color, fontSize, fontWeight and fontStyle in
    // Chinese, Arabic, and Hindi.
    Text {
        Span(
            text = "$displayTextChinese   ",
            style = SpanStyle(
                color = Color(0xFFFF0000),
                fontSize = fontSize6,
                fontWeight = FontWeight.W200,
                fontStyle = FontStyle.Italic
            )
        )

        Span(
            text = "$displayTextArabic   ",
            style = SpanStyle(
                color = Color(0xFF00FF00),
                fontSize = fontSize8,
                fontWeight = FontWeight.W500,
                fontStyle = FontStyle.Normal
            )
        )

        Span(
            text = displayTextHindi,
            style = SpanStyle(
                color = Color(0xFF0000FF),
                fontSize = fontSize10,
                fontWeight = FontWeight.W800,
                fontStyle = FontStyle.Normal
            )
        )
    }
}

@Composable
fun TextDemoFontFamily() {
    // This group of text composables show different fontFamilies in English.
    Text {
        Span(
            text = "$displayText sans-serif\n", style = SpanStyle(
                fontSize = fontSize8,
                fontFamily = FontFamily("sans-serif")
            )
        )

        Span(
            text = "$displayText serif\n", style = SpanStyle(
                fontSize = fontSize8,
                fontFamily = FontFamily("serif")
            )
        )

        Span(
            text = "$displayText monospace", style = SpanStyle(
                fontSize = fontSize8,
                fontFamily = FontFamily("monospace")
            )
        )
    }
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
    Text {
        Span(text = "$displayText   ", style = SpanStyle(fontSize = fontSize8))
        Span(
            text = displayText,
            style = SpanStyle(
                fontSize = fontSize8,
                letterSpacing = 0.5.em
            )
        )
    }
}

@Composable
fun TextDemoBaselineShift() {
    BaselineShiftSample()
}

@Composable
fun TextDemoHeight() {
    // This group of text composables show different height.
    Row(LayoutWidth.Fill) {
        Text {
            Span(
                text = "$displayText\n$displayText   ",
                style = SpanStyle(fontSize = fontSize8)
            )
        }
        Text(style = TextStyle(lineHeight = 50.sp)) {
            Span(
                text = "$displayText\n$displayText   ",
                style = SpanStyle(
                    fontSize = fontSize8
                )
            )
        }
    }
}

@Composable
fun TextDemoBackground() {
    // This group of text composables show different background.
    Text {
        Span(
            text = "$displayText   ",
            style = SpanStyle(
                fontSize = fontSize8,
                background = Color(0xFFFF0000)
            )
        )

        Span(
            text = "$displayText   ",
            style = SpanStyle(
                fontSize = fontSize8,
                background = Color(0xFF00FF00)
            )
        )

        Span(
            text = displayText,
            style = SpanStyle(
                fontSize = fontSize8,
                background = Color(0xFF0000FF)
            )
        )
    }
}

@Composable
fun TextDemoLocale() {
    // This group of text composables show different Locales of the same Unicode codepoint.
    val text = "\u82B1"
    Text {
        Span(
            text = "$text   ",
            style = SpanStyle(
                fontSize = fontSize8,
                localeList = LocaleList("ja-JP")
            )
        )

        Span(
            text = "$text   ",
            style = SpanStyle(
                fontSize = fontSize8,
                localeList = LocaleList("zh-CN")
            )
        )

        Span(
            text = text,
            style = SpanStyle(
                fontSize = fontSize8,
                localeList = LocaleList("zh-TW")
            )
        )
    }
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
        Text(style = TextStyle(textAlign = TextAlign.Left)) {
            Span(text = displayText, style = SpanStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlign = TextAlign.Right")
        Text(style = TextStyle(textAlign = TextAlign.Right)) {
            Span(text = displayText, style = SpanStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlign = TextAlign.Center")
        Text(style = TextStyle(textAlign = TextAlign.Center)) {
            Span(text = displayText, style = SpanStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlign = default and TextAlign.Justify")
        Text {
            Span(
                text = text,
                style = SpanStyle(
                    fontSize = fontSize8,
                    color = Color(0xFFFF0000)
                )
            )
        }
        Text(style = TextStyle(textAlign = TextAlign.Justify)) {
            Span(
                text = text,
                style = SpanStyle(
                    fontSize = fontSize8,
                    color = Color(0xFF0000FF)
                )
            )
        }
        SecondTagLine(tag = "textAlgin = TextAlign.Start for Ltr")
        Text(style = TextStyle(textAlign = TextAlign.Start)) {
            Span(text = displayText, style = SpanStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlgin = TextAlign.Start for Rtl")
        Text(style = TextStyle(textAlign = TextAlign.Start)) {
            Span(text = displayText, style = SpanStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlgin = TextAlign.End for Ltr")
        Text(style = TextStyle(textAlign = TextAlign.End)) {
            Span(text = displayText, style = SpanStyle(fontSize = fontSize8))
        }
        SecondTagLine(tag = "textAlgin = TextAlign.End for Rtl")
        Text(style = TextStyle(textAlign = TextAlign.End)) {
            Span(text = displayText, style = SpanStyle(fontSize = fontSize8))
        }
    }
}

@Composable
fun TextDemoSoftWrap() {
    // This group of text composables show difference between softWrap is true and false.
    var text = ""
    for (i in 1..10) {
        text = "$text$displayText"
    }
    val spanStyle = SpanStyle(fontSize = fontSize8, color = Color(0xFFFF0000))

    Column(LayoutHeight.Fill) {
        Text {
            Span(text = text, style = spanStyle)
        }
        Text(softWrap = false) {
            Span(text = text, style = spanStyle)
        }
    }
}

@Composable
fun TexDemoTextOverflowFade() {
    var text = ""
    for (i in 1..15) {
        text += displayText
    }
    val spanStyle = SpanStyle(fontSize = fontSize8, color = Color(0xFFFF0000))
    SecondTagLine(tag = "horizontally fading edge")
    Text(
        maxLines = 1,
        overflow = TextOverflow.Fade,
        softWrap = false
    ) {
        Span(text = text, style = spanStyle)
    }
    SecondTagLine(tag = "vertically fading edge")
    Text(
        maxLines = 3,
        overflow = TextOverflow.Fade
    ) {
        Span(text = text, style = spanStyle)
    }
}

@Composable
fun TextDemoShadowEffect() {
    val shadow = Shadow(
        Color(0xFFE0A0A0),
        Offset(5f, 5f),
        blurRadius = 5.px
    )
    Text {
        Span(text = "text with ", style = SpanStyle(fontSize = fontSize8)) {
            Span(text = "shadow!", style = SpanStyle(shadow = shadow))
        }
    }
}

@Composable
fun TextDemoComposableTextSpan() {
    Text(style = TextStyle(fontSize = fontSize8)) {
        Span(text = "This is a ")
        Span(text = "composable ", style = SpanStyle(fontStyle = FontStyle.Italic))
        val color1 = Color(0xFFEF50AD)
        val color2 = Color(0xFF10AF52)
        val text = "TextSpan"
        text.forEachIndexed { index, ch ->
            val color = lerp(color1, color2, index.toFloat() / text.lastIndex)
            Span(text = "$ch", style = SpanStyle(color = color))
        }
    }
}

@Composable
fun TextDemoFontSizeScale() {
    Text {
        Span(style = SpanStyle(fontSize = fontSize8)) {
            for (i in 4..12 step 4) {
                val scale = i * 0.1f
                Span("fontSizeScale=$scale\n", style = SpanStyle(fontSize = scale.em))
            }
        }
    }
}

@Composable
fun TextDemoParagraphStyling() {
    ParagraphStyleSample()
    ParagraphStyleAnnotatedStringsSample()
}
