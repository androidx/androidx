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

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.LocalTextLinkStyle
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private const val WebLink = "https://developer.android.com"
private const val LongWebLink =
    "https://developer.android.com/design/ui/mobile/guides/foundations/system-bars"
private const val PhoneUri = "tel:+123456789"

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalFoundationApi::class)
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
                    withAnnotation(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
                }
                append(" link and a phone number ")
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.None
                    )
                ) {
                    withAnnotation(LinkAnnotation.Url(PhoneUri)) { append("+1 (234) 567890") }
                }
                append(" with a custom style.")
            }
            Text(text = stringWithLink)
        }
        Sample("Link styling via composition local") {
            CompositionLocalProvider(
                LocalTextLinkStyle provides LocalTextLinkStyle.current.copy(
                    color = Color(139, 195, 74, 255)
                )
            ) {
                Text(buildAnnotatedString {
                    append("Text with ")
                    withAnnotation(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
                    append(" link wrapped in green theming.")
                })
            }
        }
        Sample("BasicText styling") {
            BasicText(buildAnnotatedString {
                append("BasicText with ")
                withAnnotation(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
                append(" link.")
            })
        }

        Sample("Long links") {
            val text = buildAnnotatedString {
                append("Example that contains ")
                withAnnotation(LinkAnnotation.Url(LongWebLink)) {
                    append("a very very very very very very long long long long link")
                }
                append(" followed by another long link ")
                withAnnotation(LinkAnnotation.Url(LongWebLink)) { append(LongWebLink) }
            }
            Text(text)
        }
        Sample("Links with overlapped bounds") {
            val text = buildAnnotatedString {
                withAnnotation(LinkAnnotation.Url(LongWebLink)) { append("The first link") }
                append(" immediately followed by ")
                withAnnotation(LinkAnnotation.Url(LongWebLink)) {
                    append("the second quite long link")
                }
                append(" so their bounds are overlapped")
            }
            Text(text)
        }
        Sample("Link inside clickable text") {
            Text(buildAnnotatedString {
                append("Clickable text with a ")
                withAnnotation(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
                append(" link.")
            }, Modifier.clickable { })
        }
        Sample("Link inside selectable text") {
            SelectionContainer {
                Text(buildAnnotatedString {
                    append("Selectable text with a ")
                    withAnnotation(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
                    append(" link.")
                })
            }
        }
        Sample("Link and inline content in text") {
            val fontSize = 20.sp
            val inlineTextContent = InlineTextContent(
                placeholder = Placeholder(fontSize, fontSize, PlaceholderVerticalAlign.Center)
            ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Green))
            }
            Text(buildAnnotatedString {
                append("A ")
                appendInlineContent("box")
                append(" inline content and a ")
                withAnnotation(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
                append(" link.")
            }, inlineContent = mapOf("box" to inlineTextContent))
        }
        Sample("Invalid link not opened") {
            Text(
                buildAnnotatedString {
                    append("Attached ")
                    withAnnotation(LinkAnnotation.Url("asdf")) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append("link")
                        }
                    }
                    append(" is invalid and won't be opened.")
                }
            )
        }
        Sample("Clickable inside a text") {
            var color by remember { mutableStateOf(Color.LightGray) }
            var background by remember { mutableStateOf(Color.LightGray) }

            BasicText(
                buildAnnotatedString {
                    append("Text contains ")
                    withAnnotation(LinkAnnotation.Clickable("color")) {
                        withStyle(SpanStyle(color = color)) {
                            append("a variable color clickable")
                        }
                    }
                    append(" and ")
                    withAnnotation(LinkAnnotation.Clickable("background")) {
                        withStyle(SpanStyle(background = background)) {
                            append("a variable background clickable")
                        }
                    }
                    append(" parts.")
                },
                onLinkClicked = { link ->
                    (link as? LinkAnnotation.Clickable)?.let { clickable ->
                        when (clickable.tag) {
                            "color" -> {
                                color = Color(Random.nextInt())
                            }
                            "background" -> {
                                background = Color(Random.nextInt()).copy(alpha = 0.3f)
                            }
                        }
                    }
                }
            )
        }
        Sample("RTL text") {
            val text = buildAnnotatedString {
                withAnnotation(LinkAnnotation.Url(LongWebLink)) {
                    append(loremIpsum(Language.Arabic, 2))
                }
                append(loremIpsum(Language.Arabic, 5))
                withAnnotation(LinkAnnotation.Url(LongWebLink)) {
                    append(loremIpsum(Language.Arabic, 3))
                }
                append(loremIpsum(Language.Arabic, 5))
            }
            Text(text)
        }
        Sample("Bidi text") {
            val text = buildAnnotatedString {
                append(loremIpsum(Language.Arabic, 2))
                withAnnotation(LinkAnnotation.Url(LongWebLink)) {
                    append(" developer.android.com ")
                }
                append(loremIpsum(Language.Arabic, 5))
            }
            Text(text)
        }
    }
}

@Composable
private fun Sample(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black)
            .padding(8.dp)
    ) {
        Text(title, Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold)
        content()
    }
}
