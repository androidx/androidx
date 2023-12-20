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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val WebLink = "https://developer.android.com"
private const val LongWebLink =
    "https://developer.android.com/design/ui/mobile/guides/foundations/system-bars"
private const val PhoneUri = "tel:+123456789"

@OptIn(ExperimentalTextApi::class)
@Composable
fun Hyperlinks() {
    Column(
        modifier = Modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 400.dp)
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Sample("Single link styling with SpanStyle") {
            val stringWithLink = buildAnnotatedString {
                append("Example with a custom style ")
                withStyle(SpanStyle(fontSize = 26.sp)) {
                    withAnnotation(UrlAnnotation(WebLink)) { append("developer.android.com") }
                }
                append(" link and a phone number ")
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.None
                    )
                ) {
                    withAnnotation(UrlAnnotation(PhoneUri)) { append("+1 (234) 567890") }
                }
                append(" with a custom style.\n")
            }
            Text(text = stringWithLink)
        }

        Sample("Long links") {
            val text = buildAnnotatedString {
                append("Example that contains ")
                withAnnotation(UrlAnnotation(LongWebLink)) {
                    append("a very very very very very very long long long long link")
                }
                append(" followed by another long link ")
                withAnnotation(UrlAnnotation(LongWebLink)) { append(LongWebLink) }
            }
            Text(text)
        }
        Sample("Links with overlapped bounds") {
            val text = buildAnnotatedString {
                withAnnotation(UrlAnnotation(LongWebLink)) { append("The first link") }
                append(" immediately followed by ")
                withAnnotation(UrlAnnotation(LongWebLink)) { append("the second quite long link") }
                append(" so their bounds are overlapped")
            }
            Text(text)
        }
        Sample("Link inside clickable text") {
            Text(buildAnnotatedString {
                append("Clickable text with a ")
                withAnnotation(UrlAnnotation(WebLink)) { append("developer.android.com") }
                append(" link.")
            }, Modifier.clickable { })
        }
        Sample("BasicText styling") {
            BasicText(buildAnnotatedString {
                append("BasicText with ")
                withAnnotation(UrlAnnotation(WebLink)) { append("developer.android.com") }
                append(" link.")
            })
        }
        Sample("Link inside selectable text") {
            SelectionContainer {
                Text(buildAnnotatedString {
                    append("Selectable text with a ")
                    withAnnotation(UrlAnnotation(WebLink)) { append("developer.android.com") }
                    append(" link.")
                })
            }
        }
        Sample("Link and inline content in text") {
            val fontSize = 20.sp
            val inlineTextContent = InlineTextContent(
                placeholder = Placeholder(fontSize, fontSize, PlaceholderVerticalAlign.Center)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Green))
            }
            BasicText(buildAnnotatedString {
                append("A ")
                appendInlineContent("box")
                append(" inline content and a ")
                withAnnotation(UrlAnnotation(WebLink)) { append("developer.android.com") }
                append(" link.")
            }, inlineContent = mapOf("box" to inlineTextContent))
        }
    }
}

@Composable
private fun ColumnScope.Sample(title: String, content: @Composable () -> Unit) {
    Text(title, Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold)
    content()
}
