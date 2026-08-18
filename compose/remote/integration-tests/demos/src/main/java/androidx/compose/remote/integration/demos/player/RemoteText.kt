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

package androidx.compose.remote.integration.demos.player

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteCustomComponent
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.util.fastFilter

/**
 * Remote composable that displays a [RemoteAnnotatedString] using [RemoteCustomComponent] and
 * [SupportSpannableString].
 */
@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
public fun RemoteText(text: RemoteAnnotatedString, modifier: RemoteModifier = RemoteModifier) {
    RemoteCustomComponent(name = "SupportSpannableString", modifier = modifier) {
        property(SupportSpannableString.PROP_TEXT.toInt(), text.text.rs)

        // 1. Links
        val links = text.linkAnnotations
        property(SupportSpannableString.PROP_LINK_COUNT.toInt(), links.size)
        for (index in links.indices) {
            val range = links[index]
            val url =
                when (val link = range.item) {
                    is LinkAnnotation.Url -> link.url
                    is LinkAnnotation.Clickable -> link.tag
                    else -> ""
                }
            property(SupportSpannableString.PROP_LINK_URL_BASE + index, url.rs)
            property(SupportSpannableString.PROP_LINK_START_BASE + index, range.start)
            property(SupportSpannableString.PROP_LINK_END_BASE + index, range.end)
        }

        // 2. SpanStyles
        val spans = text.spanStyles
        property(SupportSpannableString.PROP_SPAN_COUNT.toInt(), spans.size)
        for (index in spans.indices) {
            val range = spans[index]
            val style = range.item
            property(SupportSpannableString.PROP_SPAN_START_BASE + index, range.start)
            property(SupportSpannableString.PROP_SPAN_END_BASE + index, range.end)

            if (style.color.isSpecified) {
                property(SupportSpannableString.PROP_SPAN_COLOR_BASE + index, style.color.toArgb())
            }

            if (style.background.isSpecified) {
                property(
                    SupportSpannableString.PROP_SPAN_BG_COLOR_BASE + index,
                    style.background.toArgb(),
                )
            }

            val fontSize =
                if (style.fontSize.type == TextUnitType.Sp) style.fontSize.value.toInt() else 0
            property(SupportSpannableString.PROP_SPAN_FONT_SIZE_BASE + index, fontSize)

            var flags = 0
            if (style.fontWeight == FontWeight.Bold || (style.fontWeight?.weight ?: 0) >= 700) {
                flags = flags or 1
            }
            if (style.fontStyle == FontStyle.Italic) {
                flags = flags or 2
            }
            if (style.textDecoration?.contains(TextDecoration.Underline) == true) {
                flags = flags or 4
            }
            if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) {
                flags = flags or 8
            }
            property(SupportSpannableString.PROP_SPAN_FLAGS_BASE + index, flags)
        }

        // 3. Bullets
        val bulletAnnotations = text.annotations?.fastFilter { it.item is Bullet } ?: emptyList()
        property(SupportSpannableString.PROP_BULLET_COUNT.toInt(), bulletAnnotations.size)
        for (index in bulletAnnotations.indices) {
            val range = bulletAnnotations[index]
            property(SupportSpannableString.PROP_BULLET_START_BASE + index, range.start)
            property(SupportSpannableString.PROP_BULLET_END_BASE + index, range.end)
        }

        // 4. ParagraphStyles
        val paragraphs = text.paragraphStyles
        property(SupportSpannableString.PROP_PARAGRAPH_COUNT.toInt(), paragraphs.size)
        for (index in paragraphs.indices) {
            val range = paragraphs[index]
            val pStyle = range.item
            property(SupportSpannableString.PROP_PARAGRAPH_START_BASE + index, range.start)
            property(SupportSpannableString.PROP_PARAGRAPH_END_BASE + index, range.end)
            val align =
                when (pStyle.textAlign) {
                    TextAlign.Center -> 1
                    TextAlign.End,
                    TextAlign.Right -> 2
                    else -> 0
                }
            property(SupportSpannableString.PROP_PARAGRAPH_ALIGN_BASE + index, align)
        }
    }
}
