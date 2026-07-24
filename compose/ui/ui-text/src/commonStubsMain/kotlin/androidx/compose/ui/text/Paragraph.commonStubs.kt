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
public actual sealed interface Paragraph {
    public actual val width: Float
    public actual val height: Float
    public actual val minIntrinsicWidth: Float
    public actual val maxIntrinsicWidth: Float
    public actual val firstBaseline: Float
    public actual val lastBaseline: Float
    public actual val didExceedMaxLines: Boolean
    public actual val lineCount: Int
    public actual val placeholderRects: List<Rect?>

    public actual fun getPathForRange(start: Int, end: Int): Path

    public actual fun getCursorRect(offset: Int): Rect

    public actual fun getLineLeft(lineIndex: Int): Float

    public actual fun getLineRight(lineIndex: Int): Float

    public actual fun getLineTop(lineIndex: Int): Float

    public actual fun getLineBaseline(lineIndex: Int): Float

    public actual fun getLineBottom(lineIndex: Int): Float

    public actual fun getLineHeight(lineIndex: Int): Float

    public actual fun getLineWidth(lineIndex: Int): Float

    public actual fun getLineStart(lineIndex: Int): Int

    public actual fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int

    public actual fun isLineEllipsized(lineIndex: Int): Boolean

    public actual fun getLineForOffset(offset: Int): Int

    public actual fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float

    public actual fun getParagraphDirection(offset: Int): ResolvedTextDirection

    public actual fun getBidiRunDirection(offset: Int): ResolvedTextDirection

    public actual fun getLineForVerticalPosition(vertical: Float): Int

    public actual fun getOffsetForPosition(position: Offset): Int

    public actual fun getRangeForRect(
        rect: Rect,
        granularity: TextGranularity,
        inclusionStrategy: TextInclusionStrategy,
    ): TextRange

    public actual fun getBoundingBox(offset: Int): Rect

    public actual fun fillBoundingBoxes(
        range: TextRange,
        array: FloatArray,
        @IntRange(from = 0) arrayStart: Int,
    )

    public actual fun getWordBoundary(offset: Int): TextRange

    public actual fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
    )

    public actual fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode,
    )

    public actual fun paint(
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
            "Paragraph(text, style, Constraints(maxWidth = ceil(width).toInt()), density, " +
                "createFontFamilyResolver(resourceLoader), spanStyles, placeholders, maxLines, " +
                "if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip)",
            "kotlin.math.ceil",
            "androidx.compose.ui.unit.Constraints",
            "androidx.compose.ui.text.style.TextOverflow",
            "androidx.compose.ui.text.font.createFontFamilyResolver",
        ),
)
public actual fun Paragraph(
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
            "fontFamilyResolver, spanStyles, placeholders, maxLines, " +
            "if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip)",
        "kotlin.math.ceil",
        "androidx.compose.ui.unit.Constraints",
        "androidx.compose.ui.text.style.TextOverflow",
    ),
)
public actual fun Paragraph(
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
public actual fun Paragraph(
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

public actual fun Paragraph(
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
            "if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip)",
        "kotlin.math.ceil",
        "androidx.compose.ui.unit.Constraints",
        "androidx.compose.ui.text.style.TextOverflow",
    ),
)
public actual fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    maxLines: Int,
    ellipsis: Boolean,
    width: Float,
): Paragraph = implementedInJetBrainsFork()

@Deprecated(
    "Paragraph that takes ellipsis: Boolean is deprecated, pass TextOverflow instead.",
    level = DeprecationLevel.HIDDEN,
)
public actual fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    constraints: Constraints,
    maxLines: Int,
    ellipsis: Boolean,
): Paragraph = implementedInJetBrainsFork()

public actual fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    constraints: Constraints,
    maxLines: Int,
    overflow: TextOverflow,
): Paragraph = implementedInJetBrainsFork()
