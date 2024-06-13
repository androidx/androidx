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

package androidx.compose.ui.text

import androidx.compose.ui.text.style.TextDecoration

/**
 * Converts a string with HTML tags into [AnnotatedString].
 *
 * If you define your string in the resources, make sure to use HTML-escaped opening brackets
 * "&lt;" instead of "<".
 *
 * For a list of supported tags go check
 * [Styling with HTML markup](https://developer.android.com/guide/topics/resources/string-resource#StylingWithHTML)
 * guide. Note that bullet lists and custom annotations are not **yet** available.
 *
 * Example of displaying styled string from resources
 * @sample androidx.compose.ui.text.samples.AnnotatedStringFromHtml
 */
actual fun AnnotatedString.Companion.fromHtml(
    htmlString: String,
    linkStyles: TextLinkStyles?,
    linkInteractionListener: LinkInteractionListener?
): AnnotatedString {
    throw UnsupportedOperationException("Compose Multiplatform doesn't support fromHtml")
}