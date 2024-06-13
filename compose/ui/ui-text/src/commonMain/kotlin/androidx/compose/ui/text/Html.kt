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

/**
 * Converts a string with HTML tags into [AnnotatedString].
 *
 * If you define your string in the resources, make sure to use HTML-escaped opening brackets
 * "&lt;" instead of "<".
 *
 * For a list of supported tags go check
 * [Styling with HTML markup](https://developer.android.com/guide/topics/resources/string-resource#StylingWithHTML)
 * guide. Note that bullet lists are not **yet** available.
 *
 * @param htmlString HTML-tagged string to be parsed to construct AnnotatedString
 * @param linkStyles style configuration to be applied to links present in the string in different
 * styles
 * @param linkInteractionListener a listener that will be attached to links that are present in
 * the string and triggered when user clicks on those links. When set to null, which is a default,
 * the system will try to open the corresponding links with the
 * [androidx.compose.ui.platform.UriHandler] composition local
 *
 * Note that any link style passed directly to this method will be merged with the styles set
 * directly on a HTML-tagged string. For example, if you set a color of the link via the span
 * annotation to "red" but also pass a green color via the [linkStyles], the link will be displayed
 * as green. If, however, you pass a green background via the [linkStyles] instead, the link will
 * be displayed as red on a green background.
 *
 * Example of displaying styled string from resources
 * @sample androidx.compose.ui.text.samples.AnnotatedStringFromHtml
 *
 * @see LinkAnnotation
 *
 */
expect fun AnnotatedString.Companion.fromHtml(
    htmlString: String,
    linkStyles: TextLinkStyles? = null,
    linkInteractionListener: LinkInteractionListener? = null
): AnnotatedString
