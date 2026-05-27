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

@file:JvmName("ParagraphIntrinsicsKt")

package androidx.compose.ui.text
import androidx.compose.ui.text.platform.SkiaParagraphIntrinsics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import kotlin.jvm.JvmName

@Deprecated(
    "Font.ResourceLoader is deprecated, instead use FontFamily.Resolver",
    ReplaceWith(
        "ParagraphIntrinsics(text, style, spanStyles, placeholders, density, " +
            "fontFamilyResolver"
    ),
)
actual fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    resourceLoader: Font.ResourceLoader,
): ParagraphIntrinsics =
    SkiaParagraphIntrinsics(
        text = text,
        style = style,
        annotations = spanStyles,
        placeholders = placeholders,
        density = density,
        fontFamilyResolver = createFontFamilyResolver(resourceLoader),
    )

@Deprecated(
    "Use an overload that takes `annotations` instead",
    ReplaceWith(
        "ParagraphIntrinsics(text, style, spanStyles, density, fontFamilyResolver, placeholders)"
    ),
)
actual fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
): ParagraphIntrinsics =
    SkiaParagraphIntrinsics(
        text = text,
        style = style,
        annotations = spanStyles,
        placeholders = placeholders,
        density = density,
        fontFamilyResolver = fontFamilyResolver,
    )

actual fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
): ParagraphIntrinsics =
    SkiaParagraphIntrinsics(
        text = text,
        style = style,
        annotations = annotations,
        placeholders = placeholders,
        density = density,
        fontFamilyResolver = fontFamilyResolver,
    )

actual fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    softWrap: Boolean,
): ParagraphIntrinsics =
    SkiaParagraphIntrinsics(
        text = text,
        style = style,
        annotations = annotations,
        placeholders = placeholders,
        density = density,
        fontFamilyResolver = fontFamilyResolver,
    )
