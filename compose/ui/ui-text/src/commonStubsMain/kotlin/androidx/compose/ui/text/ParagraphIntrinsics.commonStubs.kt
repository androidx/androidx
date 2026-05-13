/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density

@Suppress("DEPRECATION")
@Deprecated(
    "Font.ResourceLoader is deprecated, instead use FontFamily.Resolver",
    ReplaceWith(
        "ParagraphIntrinsics(text, style, spanStyles, placeholders, density, " +
            "fontFamilyResolver)"
    ),
)
actual fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    density: Density,
    resourceLoader: Font.ResourceLoader,
): ParagraphIntrinsics = implementedInJetBrainsFork()

@Deprecated(
    "Use an overload that takes `annotations` instead",
    ReplaceWith(
        "ParagraphIntrinsics(text, style, spanStyles, density, fontFamilyResolver, placeholders)"
    ),
)
actual fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
): ParagraphIntrinsics = implementedInJetBrainsFork()

@Deprecated(
    "Use an override with `softWrap`",
    ReplaceWith(
        "ParagraphIntrinsics(text, style, annotations, density, fontFamilyResolver, true, listOf())"
    ),
)
actual fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    annotations: List<Range<out AnnotatedString.Annotation>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<Range<Placeholder>>,
): ParagraphIntrinsics = implementedInJetBrainsFork()

actual fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    annotations: List<Range<out AnnotatedString.Annotation>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<Range<Placeholder>>,
    softWrap: Boolean,
): ParagraphIntrinsics = implementedInJetBrainsFork()
