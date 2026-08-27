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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun TextLineHeightPerLineDemo() {
    val baseStyle =
        TextStyle(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false),
        )

    val pageIndex = remember { mutableIntStateOf(0) }
    val pageCount = 8

    Column(Modifier.padding(16.dp)) {
        Text(
            "Comparison of LineHeightStyle.Mode",
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(10.dp))

        Row(Modifier.padding(bottom = 16.dp)) {
            Text(
                " [ PREV ] ",
                modifier =
                    Modifier.background(Color.LightGray)
                        .clickable { if (pageIndex.intValue > 0) pageIndex.intValue-- }
                        .padding(8.dp),
                style = TextStyle(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "PAGE ${pageIndex.intValue + 1} OF $pageCount",
                modifier = Modifier.padding(top = 8.dp),
                style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Blue),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                " [ NEXT ] ",
                modifier =
                    Modifier.background(Color.LightGray)
                        .clickable { if (pageIndex.intValue < pageCount - 1) pageIndex.intValue++ }
                        .padding(8.dp),
                style = TextStyle(fontWeight = FontWeight.Bold),
            )
        }

        Spacer(Modifier.height(12.dp))

        when (pageIndex.intValue) {
            0 ->
                DemoSection(
                    title = "Case 1: Tall in Middle",
                    text =
                        buildAnnotatedString {
                            append("Small Line 1\n")
                            withStyle(SpanStyle(fontSize = 40.sp)) { append("မြန်မာ TALL LINE\n") }
                            append("Small Line 2")
                        },
                    baseStyle = baseStyle,
                )
            1 ->
                DemoSection(
                    title = "Case 2: Tall on First Line",
                    text =
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 40.sp)) { append("မြန်မာ TALL LINE\n") }
                            append("Small Line 2")
                        },
                    baseStyle = baseStyle,
                )
            2 ->
                DemoSection(
                    title = "Case 3: Tall on Last Line",
                    text =
                        buildAnnotatedString {
                            append("Small Line 1\n")
                            withStyle(SpanStyle(fontSize = 40.sp)) { append("မြန်မာ TALL LINE") }
                        },
                    baseStyle = baseStyle,
                )
            3 ->
                DemoSectionFourModes(
                    title = "Case 4: Mixed Script (No Size Spans)",
                    text =
                        buildAnnotatedString {
                            append("English Line 1\n")
                            append("မြန်မာစာ ၂\n")
                            append("English Line 3")
                        },
                    baseStyle = baseStyle,
                )
            4 ->
                DemoSectionFourModes(
                    title = "Case 7: Tibetan Extreme Overflow (U+0F00)",
                    text =
                        buildAnnotatedString {
                            append("Latin Top Line\n")
                            withStyle(SpanStyle(fontSize = 32.sp)) { append("Ȁༀའ ཆེན་པོ\n") }
                            append("Latin Bottom Line")
                        },
                    baseStyle = baseStyle,
                )
            5 ->
                DemoSectionFourModes(
                    title = "Case 8: Telugu Multi-Cluster (U+0C00)",
                    text =
                        buildAnnotatedString {
                            append("English Line 1\n")
                            withStyle(SpanStyle(fontSize = 32.sp)) { append("పెద్ద తెలుగు\n") }
                            append("English Line 3")
                        },
                    baseStyle = baseStyle,
                )
            6 ->
                DemoSectionFourModes(
                    title = "Case 9: Khmer Sub-consonants with ZWJ (U+1780)",
                    text =
                        buildAnnotatedString {
                            append("English Line 1\n")
                            withStyle(SpanStyle(fontSize = 32.sp)) { append("ធំ ភាសាខ្មែរ\n") }
                            append("English Line 3")
                        },
                    baseStyle = baseStyle,
                )
            7 ->
                DemoSectionFourModes(
                    title = "Case 10: Emoji Composite (U+1F3D7)",
                    text =
                        buildAnnotatedString {
                            append("English Line 1\n")
                            withStyle(SpanStyle(fontSize = 32.sp)) { append("🏗️ Large Ȁ\n") }
                            append("English Line 3")
                        },
                    baseStyle = baseStyle,
                )
        }
    }
}

@Composable
private fun DemoSectionNoLineHeight(title: String, text: AnnotatedString, baseStyle: TextStyle) {
    Column {
        Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Row {
            Column(Modifier.width(175.dp)) {
                Text("BasicText (Default)", style = TextStyle(fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                val textLayout = remember { mutableStateOf<TextLayoutResult?>(null) }
                BasicText(
                    text = text,
                    style = baseStyle,
                    modifier =
                        Modifier.drawTextMetrics(textLayout.value, null)
                            .background(Color.LightGray),
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                    onTextLayout = { textLayout.value = it },
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.width(175.dp)) {}
        }
    }
}

@Composable
private fun DemoSectionFourModes(title: String, text: AnnotatedString, baseStyle: TextStyle) {
    Column {
        Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Row {
            Column(Modifier.width(175.dp)) {
                Text("Mode.Fixed", style = TextStyle(fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                TextWithMetrics(
                    text = text,
                    style =
                        baseStyle.copy(
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Proportional,
                                    trim = LineHeightStyle.Trim.None,
                                    mode = LineHeightStyle.Mode.Fixed,
                                )
                        ),
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.width(175.dp)) {
                Text("Mode.Minimum", style = TextStyle(fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                TextWithMetrics(
                    text = text,
                    style =
                        baseStyle.copy(
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Proportional,
                                    trim = LineHeightStyle.Trim.None,
                                    mode = LineHeightStyle.Mode.Minimum,
                                )
                        ),
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Column(Modifier.width(175.dp)) {
                Text("Mode.Tight", style = TextStyle(fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                TextWithMetrics(
                    text = text,
                    style =
                        baseStyle.copy(
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Proportional,
                                    trim = LineHeightStyle.Trim.None,
                                    mode = LineHeightStyle.Mode.Tight,
                                )
                        ),
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.width(175.dp)) {
                Text("Mode.PerLine", style = TextStyle(fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                TextWithMetrics(
                    text = text,
                    style =
                        baseStyle.copy(
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Proportional,
                                    trim = LineHeightStyle.Trim.None,
                                    mode = LineHeightStyle.Mode.PerLine,
                                )
                        ),
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun DemoSection(title: String, text: AnnotatedString, baseStyle: TextStyle) {
    Column {
        Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Row {
            Column(Modifier.width(175.dp)) {
                Text("Mode.Minimum", style = TextStyle(fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                TextWithMetrics(
                    text = text,
                    style =
                        baseStyle.copy(
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Proportional,
                                    trim = LineHeightStyle.Trim.None,
                                    mode = LineHeightStyle.Mode.Minimum,
                                )
                        ),
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.width(175.dp)) {
                Text("Mode.PerLine", style = TextStyle(fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                TextWithMetrics(
                    text = text,
                    style =
                        baseStyle.copy(
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Proportional,
                                    trim = LineHeightStyle.Trim.None,
                                    mode = LineHeightStyle.Mode.PerLine,
                                )
                        ),
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
