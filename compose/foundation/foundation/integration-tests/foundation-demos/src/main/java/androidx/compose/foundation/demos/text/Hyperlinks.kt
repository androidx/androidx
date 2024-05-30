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
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.samples.AnnotatedStringWithHoveredLinkStylingSample
import androidx.compose.ui.text.samples.AnnotatedStringWithLinkSample
import androidx.compose.ui.text.samples.AnnotatedStringWithListenerSample
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach

private const val WebLink = "https://developer.android.com"
private const val LongWebLink =
    "https://developer.android.com/design/ui/mobile/guides/foundations/system-bars"
private const val PhoneUri = "tel:+123456789"

@SuppressLint("NullAnnotationGroup")
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
        Sample("State-based styling through builder") {
            BasicText(buildAnnotatedString {
                append("Text and a ")
                withLink(
                    LinkAnnotation.Url(
                        "https://developer.android.com",
                        TextLinkStyles(
                            style = SpanStyle(color = Color.Magenta),
                            focusedStyle = SpanStyle(background = Color.Yellow.copy(alpha = 0.3f)),
                            hoveredStyle = SpanStyle(textDecoration = TextDecoration.Underline),
                            pressedStyle = SpanStyle(color = Color.Red)
                        )
                    )
                ) {
                    append("DEVELOPER ANDROID COM LINK")
                }
                append(" immediately following.")
            }
            )
        }
        Sample("State-based styling from Html-tagged string") {
            val htmlString = """
                This is a <span style="color:red"><a href="https://developer.android.com">link</a></span> here.
                Another <a href="https://developer.android.com">link</a> follows.
            """.trimIndent()
            val annotatedString = AnnotatedString.fromHtml(htmlString)
            BasicText(annotatedString)
        }
        Sample("Single link styling with SpanStyle") {
            val stringWithLink = buildAnnotatedString {
                append("Example with a custom style ")
                withStyle(SpanStyle(fontSize = 26.sp)) {
                    withLink(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
                }
                append(" link and a phone number ")
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    withLink(LinkAnnotation.Url(PhoneUri)) { append("+1 (234) 567890") }
                }
                append(" with a custom style.")
            }
            BasicText(text = stringWithLink)
        }
        Sample("Material colors for links from builder") {
            Text(buildAnnotatedString {
                append("Text and ")
                withLink(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
                append(" link.")
            })
        }
        Sample("Material colors for links from html") {
            val htmlString =
                "Text and <a href=\"https://developer.android.com\">developer.android.com</a> link"
            Text(AnnotatedString.fromHtml(htmlString))
        }
        Sample("Long links") {
            val text = buildAnnotatedString {
                append("Example that contains ")
                withLink(LinkAnnotation.Url(LongWebLink)) {
                    append("a very very very very very very long long long long link")
                }
                append(" followed by another long link ")
                withLink(LinkAnnotation.Url(LongWebLink)) { append(LongWebLink) }
            }
            Text(text)
        }
        Sample("Links with overlapped bounds") {
            val text = buildAnnotatedString {
                withLink(LinkAnnotation.Url(LongWebLink)) { append("The first link") }
                append(" immediately followed by ")
                withLink(LinkAnnotation.Url(LongWebLink)) { append("the second quite long link") }
                append(" so their bounds are overlapped")
            }
            Text(text)
        }
        Sample("Link inside clickable text") {
            Text(buildAnnotatedString {
                append("Clickable text with a ")
                withLink(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
                append(" link.")
            }, Modifier.clickable { })
        }
        Sample("Link inside selectable text") {
            SelectionContainer {
                Text(buildAnnotatedString {
                    append("Selectable text with a ")
                    withLink(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
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
                withLink(LinkAnnotation.Url(WebLink)) { append("developer.android.com") }
                append(" link.")
            }, inlineContent = mapOf("box" to inlineTextContent))
        }
        Sample("Invalid link not opened") {
            Text(
                buildAnnotatedString {
                    append("Attached ")
                    withLink(LinkAnnotation.Url("asdf")) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append("link")
                        }
                    }
                    append(" is invalid and won't be opened.")
                },
                color = Color.Red
            )
        }
        Sample("RTL text") {
            val text = buildAnnotatedString {
                withLink(LinkAnnotation.Url(LongWebLink)) {
                    append(loremIpsum(Language.Arabic, 2))
                }
                append(loremIpsum(Language.Arabic, 5))
                withLink(LinkAnnotation.Url(LongWebLink)) {
                    append(loremIpsum(Language.Arabic, 3))
                }
                append(loremIpsum(Language.Arabic, 5))
            }
            Text(text)
        }
        Sample("Bidi text") {
            val text = buildAnnotatedString {
                append(loremIpsum(Language.Arabic, 2))
                withLink(LinkAnnotation.Url(LongWebLink)) { append(" developer.android.com ") }
                append(loremIpsum(Language.Arabic, 5))
            }
            Text(text)
        }
        Sample("Samples") {
            AnnotatedStringWithLinkSample()
            AnnotatedStringWithHoveredLinkStylingSample()
            AnnotatedStringWithListenerSample()
        }
        Sample("Custom Saver with link listener restoration") {
            val listener = LinkInteractionListener { /* do something */ }
            val text = buildAnnotatedString {
                withLink(LinkAnnotation.Clickable("tag", linkInteractionListener = listener)) {
                    append("Click me")
                }
            }
            val saveableText = rememberSaveable(stateSaver = AnnotatedStringSaver(listener)) {
                mutableStateOf(text)
            }
            Text(saveableText.value)
        }
        Sample("Link with listener - accessibility") {
            val localUriHandler = LocalUriHandler.current
            val listener = LinkInteractionListener { localUriHandler.openUri(WebLink) }
            val text = buildAnnotatedString {
                append("Click ")
                withLink(LinkAnnotation.Clickable("tag", linkInteractionListener = listener)) {
                    append("the first link")
                }
                append(" or ")
                withLink(LinkAnnotation.Url(WebLink, linkInteractionListener = listener)) {
                    append("the second link")
                }
            }
            Text(text)
        }
    }
}

private class AnnotatedStringSaver(
    private val linkInteractionListener: LinkInteractionListener?
) : Saver<AnnotatedString, Any> {
    override fun SaverScope.save(value: AnnotatedString): Any? {
        // It will store LinkAnnotation ignoring the LinkInteractionListener that needs to be put
        // back manually after restoration
        return with(AnnotatedString.Saver) {
            save(value)
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Suppress("UNCHECKED_CAST")
    @SuppressLint("NullAnnotationGroup")
    override fun restore(value: Any): AnnotatedString? {
       with(AnnotatedString.Saver as Saver<AnnotatedString, Any>) {
           val result = this.restore(value)
           result?.let { text ->
               // create a builder and copy over all styles
               val builder = AnnotatedString.Builder(text.text)
               text.spanStyles.fastForEach { builder.addStyle(it.item, it.start, it.end) }
               text.paragraphStyles.fastForEach { builder.addStyle(it.item, it.start, it.end) }
               // put annotations back apart from links
               text.getStringAnnotations(0, text.length).fastForEach {
                   builder.addStringAnnotation(it.tag, it.item, it.start, it.end)
               }
               text.getTtsAnnotations(0, text.length).fastForEach {
                   builder.addTtsAnnotation(it.item, it.start, it.end)
               }
               // copy link annotations and apply the listener where needed
               text.getLinkAnnotations(0, text.length).fastForEach { linkRange ->
                   when (linkRange.item) {
                       is LinkAnnotation.Url -> {
                           // in our example we assume that we never provide listeners to the
                           // LinkAnnotation.Url annotations. Therefore we restore
                           // LinkAnnotation.Url annotations as is.
                           builder.addLink(
                               linkRange.item as LinkAnnotation.Url,
                               linkRange.start,
                               linkRange.end
                           )
                       }
                       is LinkAnnotation.Clickable -> {
                           val link = LinkAnnotation.Clickable(
                               (linkRange.item as LinkAnnotation.Clickable).tag,
                               linkRange.item.styles,
                               linkInteractionListener
                           )
                           builder.addLink(link, linkRange.start, linkRange.end)
                       }
                   }
               }

               return builder.toAnnotatedString()
           }
           return null
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
