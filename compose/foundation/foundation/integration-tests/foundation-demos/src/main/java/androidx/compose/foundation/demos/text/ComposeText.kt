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

import android.annotation.SuppressLint
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.samples.BaselineShiftSample
import androidx.compose.ui.text.samples.FontFamilyCursiveSample
import androidx.compose.ui.text.samples.FontFamilyMonospaceSample
import androidx.compose.ui.text.samples.FontFamilySansSerifSample
import androidx.compose.ui.text.samples.FontFamilySerifSample
import androidx.compose.ui.text.samples.ParagraphStyleAnnotatedStringsSample
import androidx.compose.ui.text.samples.ParagraphStyleSample
import androidx.compose.ui.text.samples.TextDecorationCombinedSample
import androidx.compose.ui.text.samples.TextDecorationLineThroughSample
import androidx.compose.ui.text.samples.TextDecorationUnderlineSample
import androidx.compose.ui.text.samples.TextOverflowClipSample
import androidx.compose.ui.text.samples.TextOverflowEllipsisSample
import androidx.compose.ui.text.samples.TextOverflowVisibleFixedSizeSample
import androidx.compose.ui.text.samples.TextOverflowVisibleMinHeightSample
import androidx.compose.ui.text.samples.TextStyleSample
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

private const val longText =
    "This is a very-very long string that wraps into a few lines " + "given the width restrictions."
const val displayText = "Text Demo"
const val displayTextChinese = "文本演示"
const val displayTextArabic = "\u0639\u0631\u0636\u0020\u0627\u0644\u0646\u0635"
const val displayTextHindi = "पाठ डेमो"
const val displayTextBidi = "Text \u0639\u0631\u0636"

val fontSize4 = 16.sp
val fontSize6 = 20.sp
val fontSize8 = 25.sp
val fontSize10 = 30.sp

@SuppressLint("PrimitiveInCollection")
private val overflowOptions =
    listOf(
        TextOverflow.Clip,
        TextOverflow.Visible,
        TextOverflow.StartEllipsis,
        TextOverflow.MiddleEllipsis,
        TextOverflow.Ellipsis
    )
private val boolOptions = listOf(true, false)
@SuppressLint("PrimitiveInCollection")
private val textAlignments =
    listOf(
        TextAlign.Left,
        TextAlign.Start,
        TextAlign.Center,
        TextAlign.Right,
        TextAlign.End,
        TextAlign.Justify
    )

@Preview
@Composable
fun TextDemo() {
    LazyColumn {
        item {
            TagLine(tag = "color, fontSize, fontWeight and fontStyle")
            TextDemoBasic()
        }
        item {
            TagLine(
                tag =
                    "color, fontSize, fontWeight, fontFamily, fontStyle, letterSpacing, " +
                        "background, decoration"
            )
            TextDemoComplexStyling()
        }
        item {
            TagLine(tag = "Chinese, Arabic, and Hindi")
            TextDemoLanguage()
        }
        item {
            TagLine(tag = "FontFamily generic names")
            TextDemoFontFamily()
        }
        item {
            TagLine(tag = "FontFamily default values")
            TextDemoFontFamilyDefaultValues()
        }
        item {
            TagLine(tag = "decoration, decorationColor and decorationStyle")
            TextDemoTextDecoration()
        }
        item {
            TagLine(tag = "letterSpacing")
            TextDemoLetterSpacing()
        }
        item {
            TagLine(tag = "baselineShift")
            TextDemoBaselineShift()
        }
        item {
            TagLine(tag = "lineHeight")
            TextDemoHeight()
        }
        item {
            TagLine(tag = "background")
            TextDemoBackground()
        }
        item {
            TagLine(tag = "Locale: Japanese, Simplified and Traditional Chinese")
            TextDemoLocale()
        }
        item {
            TagLine(tag = "textAlign and textDirection")
            TextDemoTextAlign()
        }
        item {
            TagLine(tag = "softWrap: on and off")
            TextDemoSoftWrap()
        }
        item {
            TagLine(tag = "shadow")
            TextDemoShadowEffect()
        }
        item {
            TagLine(tag = "fontSizeScale")
            TextDemoFontSizeScale()
        }
        item {
            TagLine(tag = "complex paragraph styling")
            TextDemoParagraphStyling()
        }

        item {
            TagLine(tag = "textOverflow: Clip, Ellipsis, Visible")
            TextDemoTextOverflow()
        }
        item {
            TagLine(tag = "inline content")
            TextDemoInlineContent()
        }
    }
}

@Composable
fun TagLine(tag: String) {
    Text(
        style = TextStyle(fontSize = fontSize8),
        text =
            buildAnnotatedString {
                append("\n")
                withStyle(style = SpanStyle(color = Color(0xFFAAAAAA), fontSize = fontSize6)) {
                    append(tag)
                }
            }
    )
}

@Composable
fun SecondTagLine(tag: String) {
    Text(
        text =
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(0xFFAAAAAA), fontSize = fontSize4)) {
                    append(tag)
                }
            }
    )
}

@Composable
fun TextDemoBasic() {
    // This group of text composables show different color, fontSize, fontWeight and fontStyle in
    // English.
    Text(
        text =
            buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = Color(0xFFFF0000),
                        fontSize = fontSize6,
                        fontWeight = FontWeight.W200,
                        fontStyle = FontStyle.Italic
                    )
                ) {
                    append("$displayText   ")
                }

                withStyle(
                    SpanStyle(
                        color = Color(0xFF00FF00),
                        fontSize = fontSize8,
                        fontWeight = FontWeight.W500,
                        fontStyle = FontStyle.Normal
                    )
                ) {
                    append("$displayText   ")
                }

                withStyle(
                    SpanStyle(
                        color = Color(0xFF0000FF),
                        fontSize = fontSize10,
                        fontWeight = FontWeight.W800,
                        fontStyle = FontStyle.Normal
                    )
                ) {
                    append(displayText)
                }
            }
    )
}

@Composable
fun TextDemoComplexStyling() {
    TextStyleSample()
}

@Composable
fun TextDemoLanguage() {
    // This group of text composables show different color, fontSize, fontWeight and fontStyle in
    // Chinese, Arabic, and Hindi.
    Text(
        text =
            buildAnnotatedString {
                withStyle(
                    style =
                        SpanStyle(
                            color = Color(0xFFFF0000),
                            fontSize = fontSize6,
                            fontWeight = FontWeight.W200,
                            fontStyle = FontStyle.Italic
                        )
                ) {
                    append("$displayTextChinese   ")
                }

                withStyle(
                    style =
                        SpanStyle(
                            color = Color(0xFF00FF00),
                            fontSize = fontSize8,
                            fontWeight = FontWeight.W500,
                            fontStyle = FontStyle.Normal
                        )
                ) {
                    append("$displayTextArabic   ")
                }

                withStyle(
                    style =
                        SpanStyle(
                            color = Color(0xFF0000FF),
                            fontSize = fontSize10,
                            fontWeight = FontWeight.W800,
                            fontStyle = FontStyle.Normal
                        )
                ) {
                    append(displayTextHindi)
                }
            }
    )
}

@Composable
fun TextDemoFontFamily() {
    // This group of text composables show different fontFamilies in English.
    Text(
        buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = fontSize8, fontFamily = FontFamily.SansSerif)) {
                append("$displayText sans-serif\n")
            }

            withStyle(style = SpanStyle(fontSize = fontSize8, fontFamily = FontFamily.Serif)) {
                append("$displayText serif\n")
            }

            withStyle(style = SpanStyle(fontSize = fontSize8, fontFamily = FontFamily.Monospace)) {
                append("$displayText monospace")
            }
        }
    )
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
    // This group of text composables show different decoration, decorationColor and
    // decorationStyle.
    TextDecorationLineThroughSample()
    TextDecorationUnderlineSample()
    TextDecorationCombinedSample()
}

@Composable
fun TextDemoLetterSpacing() {
    // This group of text composables show different letterSpacing.
    Text(
        text =
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = fontSize8)) { append("$displayText   ") }
                withStyle(style = SpanStyle(fontSize = fontSize8, letterSpacing = 0.5.em)) {
                    append(displayText)
                }
            }
    )
}

@Composable
fun TextDemoBaselineShift() {
    BaselineShiftSample()
}

@Composable
fun TextDemoHeight() {
    // This group of text composables show different height.
    Row(Modifier.fillMaxWidth()) {
        Text(text = "$displayText\n$displayText   ", style = TextStyle(fontSize = fontSize8))
        Text(
            text = "$displayText\n$displayText   ",
            style = TextStyle(fontSize = fontSize8, lineHeight = 50.sp)
        )
    }
}

@Composable
fun TextDemoBackground() {
    // This group of text composables show different background.
    Text(
        text =
            buildAnnotatedString {
                withStyle(style = SpanStyle(background = Color(0xFFFF0000))) {
                    append("$displayText   ")
                }

                withStyle(style = SpanStyle(background = Color(0xFF00FF00))) {
                    append("$displayText   ")
                }

                withStyle(style = SpanStyle(background = Color(0xFF0000FF))) { append(displayText) }
            },
        style = TextStyle(fontSize = fontSize8)
    )
}

@Composable
fun TextDemoLocale() {
    // This group of text composables show different Locales of the same Unicode codepoint.
    val text = "\u82B1"
    Text(
        text =
            buildAnnotatedString {
                withStyle(style = SpanStyle(localeList = LocaleList("ja-JP"))) {
                    append("$text   ")
                }

                withStyle(style = SpanStyle(localeList = LocaleList("zh-CN"))) {
                    append("$text   ")
                }

                withStyle(style = SpanStyle(localeList = LocaleList("zh-TW"))) { append(text) }
            },
        style = TextStyle(fontSize = fontSize8)
    )
}

@Composable
fun TextDemoTextAlign() {
    // This group of text composables show different TextAligns: LEFT, RIGHT, CENTER, JUSTIFY, START
    // for
    // LTR and RTL, END for LTR and RTL.
    var text = ""
    for (i in 1..10) {
        text = "$text$displayText "
    }
    Column(Modifier.fillMaxHeight()) {
        SecondTagLine(tag = "textAlign = TextAlign.Left")
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = displayText,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Left)
        )

        SecondTagLine(tag = "textAlign = TextAlign.Right")
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = displayText,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Right)
        )

        SecondTagLine(tag = "textAlign = TextAlign.Center")
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = displayText,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Center)
        )

        SecondTagLine(tag = "textAlign = default and TextAlign.Justify")
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            style = TextStyle(fontSize = fontSize8, color = Color(0xFFFF0000))
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            style =
                TextStyle(
                    fontSize = fontSize8,
                    color = Color(0xFF0000FF),
                    textAlign = TextAlign.Justify
                )
        )

        SecondTagLine(tag = "textAlign = TextAlign.Start for Ltr")
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = displayText,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Start)
        )
        SecondTagLine(tag = "textAlign = TextAlign.Start for Rtl")
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = displayTextArabic,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Start)
        )
        SecondTagLine(tag = "textAlign = TextAlign.End for Ltr")
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = displayText,
            style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.End)
        )
        SecondTagLine(tag = "textAlign = TextAlign.End for Rtl")
        Text(
            modifier = Modifier.fillMaxWidth(),
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

    Column(Modifier.fillMaxHeight()) {
        Text(text = text, style = textStyle)
        Text(text = text, style = textStyle, softWrap = false)
    }
}

@Composable
fun TextDemoHyphens() {
    val text = "Transformation"
    val textStyleHyphensOn =
        TextStyle(fontSize = fontSize8, color = Color.Red, hyphens = Hyphens.Auto)
    val textStyleHyphensOff =
        TextStyle(fontSize = fontSize8, color = Color.Blue, hyphens = Hyphens.None)
    Column {
        var width by remember { mutableFloatStateOf(30f) }
        Slider(value = width, onValueChange = { width = it }, valueRange = 20f..400f)
        Column(Modifier.width(width.dp)) {
            Text(text = text, style = textStyleHyphensOn)
            Text(text = text, style = textStyleHyphensOff)
        }
    }
}

@Composable
fun TextDemoShadowEffect() {
    val shadow = Shadow(Color(0xFFE0A0A0), Offset(5f, 5f), blurRadius = 5.0f)
    Text(
        style = TextStyle(fontSize = fontSize8),
        text =
            buildAnnotatedString {
                append("text with ")
                withStyle(style = SpanStyle(shadow = shadow)) { append("shadow!") }
            }
    )
}

@Composable
fun TextDemoFontSizeScale() {
    Text(
        style = TextStyle(fontSize = fontSize8),
        text =
            buildAnnotatedString {
                for (i in 4..12 step 4) {
                    val scale = i * 0.1f
                    withStyle(style = SpanStyle(fontSize = scale.em)) {
                        append("fontSizeScale=$scale\n")
                    }
                }
            }
    )
}

@Composable
fun TextDemoParagraphStyling() {
    ParagraphStyleSample()
    ParagraphStyleAnnotatedStringsSample()
}

@Composable
fun TextDemoTextOverflow() {
    SecondTagLine(tag = "overflow = TextOverflow.Clip")
    TextOverflowClipSample()
    SecondTagLine(tag = "overflow = TextOverflow.Ellipsis")
    TextOverflowEllipsisSample()
    SecondTagLine(tag = "overflow = TextOverflow.Visible with fixed size")
    TextOverflowVisibleFixedSizeSample()
    Spacer(modifier = Modifier.size(30.dp))
    SecondTagLine(tag = "overflow = TextOverflow.Visible with fixed width and min height")
    TextOverflowVisibleMinHeightSample()
}

@Composable
fun TextOverflowVisibleInPopupDemo() {
    Popup(alignment = Alignment.Center, properties = PopupProperties(clippingEnabled = false)) {
        val text = "Line\n".repeat(10)
        Box(Modifier.background(Color.Magenta).size(100.dp)) {
            Text(text, fontSize = fontSize6, overflow = TextOverflow.Visible)
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun TextOverflowVisibleInDrawText() {
    val textMeasurer = rememberTextMeasurer()
    val text = "Line\n".repeat(10)
    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier.graphicsLayer().align(Alignment.Center).background(Color.Green).size(100.dp)
        ) {
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = TextStyle(fontSize = fontSize6),
                overflow = TextOverflow.Visible
            )
        }
    }
}

@Composable
fun TextOverflowDemo() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        var singleParagraph by remember { mutableStateOf(boolOptions[0]) }
        var selectedOverflow by remember { mutableStateOf(overflowOptions[0]) }
        var singleLinePerPar by remember { mutableStateOf(boolOptions[1]) }
        var width by remember { mutableFloatStateOf(250f) }
        var height by remember { mutableFloatStateOf(50f) }
        var letterSpacing by remember { mutableFloatStateOf(0f) }
        var textAlign by remember { mutableStateOf(TextAlign.Left) }
        var softWrap by remember { mutableStateOf(true) }

        TextOverflowDemo(
            singleParagraph,
            selectedOverflow,
            singleLinePerPar,
            width.dp,
            height.dp,
            letterSpacing.sp,
            textAlign,
            softWrap
        )

        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.selectableGroup().weight(1f)) {
                Text("TextOverflow", fontWeight = FontWeight.Bold)
                overflowOptions.forEach {
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = (it == selectedOverflow),
                                onClick = { selectedOverflow = it },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (it == selectedOverflow),
                            onClick = null // null recommended for accessibility with screenreaders
                        )
                        Text(text = it.toString())
                    }
                }
            }
            Column(Modifier.selectableGroup().weight(1f)) {
                Text("Paragraph", fontWeight = FontWeight.Bold)
                boolOptions.forEach {
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = (it == singleParagraph),
                                onClick = { singleParagraph = it },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (it == singleParagraph),
                            onClick = null // null recommended for accessibility with screenreaders
                        )
                        Text(text = if (it) "Single" else "Multi")
                    }
                }
            }
            Column(Modifier.selectableGroup().weight(1f)) {
                Text("Single line", fontWeight = FontWeight.Bold)
                boolOptions.forEach {
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = (it == singleLinePerPar),
                                onClick = { singleLinePerPar = it },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (it == singleLinePerPar), onClick = null)
                        Text(text = it.toString())
                    }
                }
            }
        }
        Column {
            Text("Width " + "%.1f".format(width) + "dp")
            Slider(width, { width = it }, valueRange = 30f..300f)
        }
        Column {
            Text("Height " + "%.1f".format(height) + "dp")
            Slider(height, { height = it }, valueRange = 5f..300f)
        }
        Column {
            Text("Letter spacing " + "%.1f".format(letterSpacing) + "sp")
            Slider(letterSpacing, { letterSpacing = it }, valueRange = -4f..8f, steps = 11)
        }
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Text Align", fontWeight = FontWeight.Bold)
                textAlignments.forEach {
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = (it == textAlign),
                                onClick = { textAlign = it },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (it == textAlign), onClick = null)
                        Text(text = it.toString())
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Soft wrap", fontWeight = FontWeight.Bold)
                boolOptions.forEach {
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = (it == softWrap),
                                onClick = { softWrap = it },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (it == softWrap), onClick = null)
                        Text(text = it.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TextOverflowDemo(
    singleParagraph: Boolean,
    textOverflow: TextOverflow,
    singeLine: Boolean,
    width: Dp,
    height: Dp,
    letterSpacing: TextUnit,
    textAlign: TextAlign,
    softWrap: Boolean
) {
    Box(Modifier.weight(1f).fillMaxWidth()) {
        val text =
            if (singleParagraph) {
                AnnotatedString(longText)
            } else {
                buildAnnotatedString {
                    append(longText)
                    withStyle(ParagraphStyle(textAlign = TextAlign.End)) {
                        append("This is a second paragraph.")
                    }
                }
            }
        val textStyle =
            TextStyle(fontSize = fontSize6, letterSpacing = letterSpacing, textAlign = textAlign)
        BasicText(
            text = text,
            modifier =
                Modifier.align(Alignment.Center)
                    .background(Color.Magenta)
                    .widthIn(max = width)
                    .heightIn(max = height),
            style = textStyle,
            overflow = textOverflow,
            maxLines = if (singeLine) 1 else Int.MAX_VALUE,
            softWrap = softWrap
        )
    }
}

@Composable
fun TextDemoInlineContent() {
    val inlineContentId = "box"
    val inlineTextContent =
        InlineTextContent(
            placeholder =
                Placeholder(
                    width = 5.em,
                    height = 1.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                )
        ) {
            val colorAnimation = rememberInfiniteTransition()
            val color by
                colorAnimation.animateColor(
                    initialValue = Color.Red,
                    targetValue = Color.Blue,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
                )
            Box(modifier = Modifier.fillMaxSize().background(color))
        }

    Text(
        text =
            buildAnnotatedString {
                append("Here is a wide inline composable ")
                appendInlineContent(inlineContentId)
                append(" that is repeatedly changing its color.")
            },
        inlineContent = mapOf(inlineContentId to inlineTextContent),
        modifier = Modifier.fillMaxWidth()
    )

    SecondTagLine(tag = "RTL Layout")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text =
                buildAnnotatedString {
                    append("Here is a wide inline composable ")
                    appendInlineContent(inlineContentId)
                    append(" that is repeatedly changing its color.")
                },
            inlineContent = mapOf(inlineContentId to inlineTextContent),
            modifier = Modifier.fillMaxWidth()
        )
    }

    SecondTagLine(tag = "Bidi Text - LTR/RTL")
    Text(
        text =
            buildAnnotatedString {
                append("$displayText   ")
                appendInlineContent(inlineContentId)
                append("$displayTextArabic   ")
            },
        inlineContent = mapOf(inlineContentId to inlineTextContent),
        modifier = Modifier.fillMaxWidth()
    )

    SecondTagLine(tag = "Bidi Text - RTL/LTR")
    Text(
        text =
            buildAnnotatedString {
                append("$displayTextArabic   ")
                appendInlineContent(inlineContentId)
                append("$displayText   ")
            },
        inlineContent = mapOf(inlineContentId to inlineTextContent),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EllipsizeDemo() {
    var softWrap by remember { mutableStateOf(true) }
    var ellipsis by remember { mutableStateOf(true) }
    var withSpans by remember { mutableStateOf(false) }
    val lineHeight = remember { mutableStateOf(16.sp) }
    val heightRestriction = remember { mutableStateOf(45.dp) }

    Column {
        ListItem(
            Modifier.selectable(softWrap) { softWrap = !softWrap },
            trailing = { Switch(softWrap, null) }
        ) {
            Text("Soft wrap")
        }
        ListItem(
            Modifier.selectable(ellipsis) { ellipsis = !ellipsis },
            trailing = { Switch(ellipsis, null) }
        ) {
            Text("Ellipsis")
        }
        ListItem(
            Modifier.selectable(withSpans) { withSpans = !withSpans },
            trailing = { Switch(withSpans, null) },
            secondaryText = { Text("Text with spans") }
        ) {
            Text("Spans")
        }

        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton({
                    heightRestriction.value = (heightRestriction.value + 5.dp).coerceAtMost(300.dp)
                }) {
                    Icon(Icons.Default.KeyboardArrowUp, "Increase height")
                }
                Text("Max height ${heightRestriction.value}")
                IconButton({
                    heightRestriction.value = (heightRestriction.value - 5.dp).coerceAtLeast(0.dp)
                }) {
                    Icon(Icons.Default.KeyboardArrowDown, "Decrease height")
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton({
                    lineHeight.value = ((lineHeight.value.value + 2f)).coerceAtMost(100f).sp
                }) {
                    Icon(Icons.Default.KeyboardArrowUp, "Increase line height")
                }
                Text("Line height ${lineHeight.value.value.toInt().sp}")
                IconButton({
                    lineHeight.value = ((lineHeight.value.value - 2f)).coerceAtLeast(5f).sp
                }) {
                    Icon(Icons.Default.KeyboardArrowDown, "Decrease line height")
                }
            }
        }

        val fontSize = 16.sp
        val text =
            "This is a very-very " +
                "long text that has a limited height and width to test how it's ellipsized." +
                " This is a second sentence of the text."
        val textWithSpans = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = fontSize / 2)) {
                append("This is a very-very long text that has ")
            }
            withStyle(SpanStyle(fontSize = fontSize * 2)) { append("a limited height") }
            append(" and width to test how it's ellipsized. This is a second sentence of the text.")
        }
        Text(
            text = if (withSpans) textWithSpans else AnnotatedString(text),
            fontSize = fontSize,
            lineHeight = lineHeight.value,
            modifier =
                Modifier.background(Color.Magenta)
                    .width(200.dp)
                    .heightIn(max = heightRestriction.value),
            softWrap = softWrap,
            overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip
        )
    }
}
