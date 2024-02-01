/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.util.fastForEach

/**
 * Composition local used to change the style used by text links in the hierarchy.
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@ExperimentalFoundationApi
@get:ExperimentalFoundationApi
val LocalTextLinkStyle = compositionLocalOf { DefaultTextLinkStyle }

private val DefaultTextLinkStyle = SpanStyle(textDecoration = TextDecoration.Underline)

internal fun AnnotatedString.withLinkStyle(linkStyle: SpanStyle): AnnotatedString {
    val links = getLinkAnnotations(0, text.length)
    val stringBuilder = AnnotatedString.Builder(text = this)
    links.fastForEach {
        stringBuilder.addStyle(linkStyle, it.start, it.end)
    }
    // re-apply developer provided span styles. Order is important because values provided by
    // developers directly through the annotations should always override the style coming from
    // the theme/LocalTextLinkStyle.
    spanStyles.fastForEach {
        stringBuilder.addStyle(it.item, it.start, it.end)
    }
    return stringBuilder.toAnnotatedString()
}
