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

@file:JvmMultifileClass
@file:JvmName("ParagraphKt")

package androidx.compose.ui.text

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.SkiaParagraphIntrinsics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

@Suppress("DEPRECATION")
@Deprecated(
    "Font.ResourceLoader is deprecated, instead pass FontFamily.Resolver",
    replaceWith =
        ReplaceWith(
            "ActualParagraph(text, style, spanStyles, placeholders, " +
                "maxLines, ellipsis, width, density, createFontFamilyResolver(resourceLoader))"
        ),
)
actual fun Paragraph(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    maxLines: Int,
    ellipsis: Boolean,
    width: Float,
    density: Density,
    resourceLoader: Font.ResourceLoader,
): Paragraph = SkiaParagraph(
    SkiaParagraphIntrinsics(
        text = text,
        style = style,
        placeholders = placeholders,
        annotations = spanStyles,
        fontFamilyResolver = createFontFamilyResolver(resourceLoader),
        density = density
    ),
    maxLines,
    if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
    Constraints(maxWidth = width.ceilToInt()),
)

@Deprecated(
    "Paragraph that takes maximum allowed width is deprecated, pass constraints instead.",
    ReplaceWith(
        "Paragraph(text, style, Constraints(maxWidth = ceil(width).toInt()), density, " +
            "fontFamilyResolver, spanStyles, placeholders, maxLines, ellipsis)",
        "kotlin.math.ceil",
        "androidx.compose.ui.unit.Constraints",
    ),
)
actual fun Paragraph(
    text: String,
    style: TextStyle,
    width: Float,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    maxLines: Int,
    ellipsis: Boolean,
): Paragraph = SkiaParagraph(
    SkiaParagraphIntrinsics(
        text = text,
        style = style,
        placeholders = placeholders,
        annotations = spanStyles,
        fontFamilyResolver = fontFamilyResolver,
        density = density
    ),
    maxLines,
    if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
    Constraints(maxWidth = width.ceilToInt()),
)

@Deprecated(
    "Paragraph that takes `ellipsis: Boolean` is deprecated, pass TextOverflow instead.",
    level = DeprecationLevel.HIDDEN,
)
actual fun Paragraph(
    text: String,
    style: TextStyle,
    constraints: Constraints,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    maxLines: Int,
    ellipsis: Boolean,
): Paragraph = SkiaParagraph(
    SkiaParagraphIntrinsics(
        text = text,
        style = style,
        placeholders = placeholders,
        annotations = spanStyles,
        fontFamilyResolver = fontFamilyResolver,
        density = density
    ),
    maxLines,
    if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
    constraints
)

actual fun Paragraph(
    text: String,
    style: TextStyle,
    constraints: Constraints,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    maxLines: Int,
    overflow: TextOverflow,
): Paragraph = SkiaParagraph(
    SkiaParagraphIntrinsics(
        text = text,
        style = style,
        placeholders = placeholders,
        annotations = spanStyles,
        fontFamilyResolver = fontFamilyResolver,
        density = density
    ),
    maxLines,
    overflow,
    constraints
)

@Deprecated(
    "Paragraph that takes maximum allowed width is deprecated, pass constraints instead.",
    ReplaceWith(
        "Paragraph(paragraphIntrinsics, Constraints(maxWidth = ceil(width).toInt()), maxLines, " +
            "ellipsis)",
        "kotlin.math.ceil",
        "androidx.compose.ui.unit.Constraints",
    ),
)
actual fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    maxLines: Int,
    ellipsis: Boolean,
    width: Float,
): Paragraph  =
    SkiaParagraph(
        paragraphIntrinsics as SkiaParagraphIntrinsics,
        maxLines,
        if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
        Constraints(maxWidth = width.ceilToInt()),
    )

@Deprecated(
    "Paragraph that takes ellipsis: Boolean is deprecated, pass TextOverflow instead.",
    level = DeprecationLevel.HIDDEN,
)
actual fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    constraints: Constraints,
    maxLines: Int,
    ellipsis: Boolean,
): Paragraph =
    SkiaParagraph(
        paragraphIntrinsics as SkiaParagraphIntrinsics,
        maxLines,
        if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
        constraints
    )

actual fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    constraints: Constraints,
    maxLines: Int,
    overflow: TextOverflow,
): Paragraph =
    SkiaParagraph(
        paragraphIntrinsics as SkiaParagraphIntrinsics,
        maxLines,
        overflow,
        constraints
    )
