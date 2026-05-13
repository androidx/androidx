/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.annotation.IntRange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

@JvmDefaultWithCompatibility
actual sealed interface Paragraph {
    actual val width: Float
    actual val height: Float
    actual val minIntrinsicWidth: Float
    actual val maxIntrinsicWidth: Float
    actual val firstBaseline: Float
    actual val lastBaseline: Float
    actual val didExceedMaxLines: Boolean
    actual val lineCount: Int
    actual val placeholderRects: List<Rect?>

    actual fun getPathForRange(start: Int, end: Int): Path

    actual fun getCursorRect(offset: Int): Rect

    actual fun getLineLeft(lineIndex: Int): Float

    actual fun getLineRight(lineIndex: Int): Float

    actual fun getLineTop(lineIndex: Int): Float

    actual fun getLineBaseline(lineIndex: Int): Float

    actual fun getLineBottom(lineIndex: Int): Float

    actual fun getLineHeight(lineIndex: Int): Float

    actual fun getLineWidth(lineIndex: Int): Float

    actual fun getLineStart(lineIndex: Int): Int

    actual fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int

    actual fun isLineEllipsized(lineIndex: Int): Boolean

    actual fun getLineForOffset(offset: Int): Int

    actual fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float

    actual fun getParagraphDirection(offset: Int): ResolvedTextDirection

    actual fun getBidiRunDirection(offset: Int): ResolvedTextDirection

    actual fun getLineForVerticalPosition(vertical: Float): Int

    actual fun getOffsetForPosition(position: Offset): Int

    actual fun getRangeForRect(
        rect: Rect,
        granularity: TextGranularity,
        inclusionStrategy: TextInclusionStrategy,
    ): TextRange

    actual fun getBoundingBox(offset: Int): Rect

    actual fun fillBoundingBoxes(
        range: TextRange,
        array: FloatArray,
        @IntRange(from = 0) arrayStart: Int,
    )

    actual fun getWordBoundary(offset: Int): TextRange

    actual fun paint(canvas: Canvas, color: Color, shadow: Shadow?, textDecoration: TextDecoration?)

    actual fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode,
    )

    actual fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode,
    )
}

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
): Paragraph = implementedInJetBrainsFork()

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
): Paragraph = implementedInJetBrainsFork()

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
): Paragraph = implementedInJetBrainsFork()

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
): Paragraph = implementedInJetBrainsFork()

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
): Paragraph = implementedInJetBrainsFork()

@Deprecated(
    "Paragraph that takes ellipsis: Boolean is deprecated, pass TextOverflow instead.",
    level = DeprecationLevel.HIDDEN,
)
actual fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    constraints: Constraints,
    maxLines: Int,
    ellipsis: Boolean,
): Paragraph = implementedInJetBrainsFork()

actual fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    constraints: Constraints,
    maxLines: Int,
    overflow: TextOverflow,
): Paragraph = implementedInJetBrainsFork()
