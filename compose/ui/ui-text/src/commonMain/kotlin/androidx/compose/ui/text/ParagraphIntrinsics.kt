/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density

/** Calculates and presents the intrinsic width and height of text. */
interface ParagraphIntrinsics {
    /** The width for text if all soft wrap opportunities were taken. */
    val minIntrinsicWidth: Float

    /** Returns the smallest width beyond which increasing the width never decreases the height. */
    val maxIntrinsicWidth: Float

    /**
     * Any [Paragraph] rendered using this [ParagraphIntrinsics] will be measured and drawn using
     * stale resolved fonts.
     *
     * If this is false, this [ParagraphIntrinsics] is using the most current font resolution from
     * [FontFamily.Resolver].
     *
     * If this is true, recreating this [ParagraphIntrinsics] will use new fonts from
     * [FontFamily.Resolver] for both display and measurement. Recreating this [ParagraphIntrinsics]
     * and displaying the resulting [Paragraph] causes user-visible reflow of the displayed text.
     *
     * Once true, this will never become false without recreating this [ParagraphIntrinsics].
     *
     * It is discouraged, but safe, to continue to use this object after this becomes true. The only
     * impact of using this object after [hasStaleResolvedFonts] becomes true is stale resolutions
     * of async fonts for measurement and display.
     */
    val hasStaleResolvedFonts: Boolean
        get() = false
}

/**
 * Factory method to create a [ParagraphIntrinsics].
 *
 * If the [style] does not contain any [androidx.compose.ui.text.style.TextDirection],
 * [androidx.compose.ui.text.style.TextDirection.Content] is used as the default value.
 *
 * @see ParagraphIntrinsics
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Font.ResourceLoader is deprecated, instead use FontFamily.Resolver",
    ReplaceWith(
        "ParagraphIntrinsics(text, style, spanStyles, placeholders, density, " +
            "fontFamilyResolver)"
    ),
)
expect fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    density: Density,
    resourceLoader: Font.ResourceLoader,
): ParagraphIntrinsics

@Deprecated(
    "Use an overload that takes `annotations` instead",
    ReplaceWith(
        "ParagraphIntrinsics(text, style, spanStyles, density, fontFamilyResolver, placeholders)"
    ),
)
expect fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
): ParagraphIntrinsics

/**
 * Factory method to create a [ParagraphIntrinsics].
 *
 * If the [style] does not contain any [androidx.compose.ui.text.style.TextDirection],
 * [androidx.compose.ui.text.style.TextDirection.Content] is used as the default value.
 *
 * @see ParagraphIntrinsics
 */
@Deprecated(
    "Use an override with `softWrap`",
    ReplaceWith(
        "ParagraphIntrinsics(text, style, annotations, density, fontFamilyResolver, true, listOf())"
    ),
)
expect fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
): ParagraphIntrinsics

/**
 * Factory method to create a [ParagraphIntrinsics].
 *
 * @param text The text to be measured.
 * @param style The [TextStyle] to apply to the text.
 * @param annotations The annotations to apply to the text.
 * @param density The [Density] of the display environment.
 * @param fontFamilyResolver The [FontFamily.Resolver] to resolve fonts.
 * @param softWrap Whether the text should break at soft line breaks. When the intention is to lay
 *   out text as a single line, setting [softWrap] to false enables optimizations that avoid certain
 *   expensive calculations.
 * @param placeholders The list of [Placeholder] to be used in the text.
 */
expect fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    softWrap: Boolean,
): ParagraphIntrinsics
