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

package androidx.compose.ui.text

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Draws a filled background and optional [Border] behind an annotated text range.
 *
 * Use [BackgroundAnnotation] instead of [SpanStyle.background] to draw rounded multi-line
 * highlights, inline pills with [Border]s, or draw-phase color and brush animations without
 * triggering text measurement or relayout.
 *
 * How multi-line ranges are drawn depends on [mergeLines]:
 * - `mergeLines = true` (default): Overlapping boxes on adjacent lines merge into a single shape.
 *   Vertical [spread] applies only to the top of the first line and the bottom of the last line,
 *   keeping the inner seams between lines connected.
 * - `mergeLines = false`: Each line is drawn as a separate rounded rectangle. A negative vertical
 *   [spread] (for example, `SpreadValues(vertical = (-2).dp)`) shrinks each box's top and bottom
 *   edges to leave a gap between lines.
 *
 * ```
 * mergeLines = true (unified contour):    mergeLines = false (per-line pills):
 *        .----------.                            .----------.
 * Hello, | world    |                     Hello, | world    |
 * .------'   .------'                            '----------'
 * | again    | now.                       .----------.
 * '----------'                            | again    | now.
 *                                         '----------'
 * ```
 *
 * Note: [AnnotatedString.Saver] does not save or restore [BackgroundAnnotation] instances.
 *
 * @sample androidx.compose.ui.text.samples.BackgroundAnnotationSample
 * @sample androidx.compose.ui.text.samples.BackgroundAnnotationSpreadSample
 * @sample androidx.compose.ui.text.samples.BackgroundAnnotationColorProducerSample
 * @sample androidx.compose.ui.text.samples.BackgroundAnnotationBrushSample
 * @property color fill color drawn behind the annotated range, or [Color.Unspecified] when
 *   constructed with [colorProducer] or [brush]
 * @property colorProducer deferred color producer evaluated during the draw phase, or `null` when
 *   constructed with [color] or [brush]
 * @property brush fill brush drawn behind the annotated range, or `null` when constructed with
 *   [color] or [colorProducer]
 * @property cornerRadius radius applied to outer and inner corners of the contour; defaults to
 *   `0.dp`. Coerced to at most half the length of the adjacent edge
 * @property spread per-edge draw-phase spread applied to the range's bounding boxes (positive
 *   values expand outward, negative values shrink inward) without affecting text layout; defaults
 *   to [SpreadValues.Zero]. Any box or merged line stack whose resulting width or height is `<= 0`
 *   is not drawn. See [SpreadValues] for per-box and multi-line behavior
 * @property mergeLines whether vertically adjacent boxes on consecutive lines that overlap
 *   horizontally join into a single continuous contour (`true`) or draw as independent per-line
 *   rounded rectangles (`false`); defaults to `true`
 * @property border optional [Border] drawn as a centered stroke along the background contour (half
 *   of [Border.width] extends outside the contour and half overlaps inside the fill); defaults to
 *   `null` (no border)
 * @property alpha opacity applied to both fill and border, from `0.0f` to `1.0f`; defaults to
 *   `1.0f`
 * @see DrawAnnotation
 * @see BackgroundAnnotation.SpreadValues
 */
@Immutable
public class BackgroundAnnotation
private constructor(
    public val color: Color = Color.Unspecified,
    public val colorProducer: ColorProducer? = null,
    public val brush: Brush? = null,
    public val cornerRadius: Dp = DefaultCornerRadius,
    public val spread: SpreadValues = DefaultSpread,
    @get:Suppress("GetterSetterNames") public val mergeLines: Boolean = DefaultMergeLines,
    public val border: Border? = null,
    public val alpha: Float = DefaultAlpha,
) : DrawAnnotation() {

    internal var path: Path? = null

    /**
     * Creates a [BackgroundAnnotation] filled with a static solid [color].
     *
     * For animated colors, use the [ColorProducer] constructor (`BackgroundAnnotation(colorProducer
     * = ...)`) instead so color changes are resolved during the draw phase without recomposing or
     * re-measuring text.
     *
     * @param color static fill color drawn behind the annotated range
     * @param cornerRadius radius applied to outer and inner corners of the contour; defaults to
     *   `0.dp`. Coerced to at most half the length of the adjacent edge
     * @param spread per-edge draw-phase spread applied to the range's bounding boxes (positive
     *   expands outward, negative shrinks inward); defaults to [SpreadValues.Zero]
     * @param mergeLines whether vertically adjacent overlapping boxes on consecutive lines join
     *   into a single contour (`true`) or draw as independent rounded rectangles (`false`);
     *   defaults to `true`
     * @param border optional [Border] drawn as a centered stroke along the background contour;
     *   defaults to `null` (no border)
     * @param alpha opacity applied to both fill and border, from `0.0f` to `1.0f`; defaults to
     *   `1.0f`
     */
    public constructor(
        color: Color,
        cornerRadius: Dp = DefaultCornerRadius,
        spread: SpreadValues = DefaultSpread,
        mergeLines: Boolean = DefaultMergeLines,
        border: Border? = null,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
    ) : this(
        color = color,
        colorProducer = null,
        brush = null,
        cornerRadius = cornerRadius,
        spread = spread,
        mergeLines = mergeLines,
        border = border,
        alpha = alpha,
    )

    /**
     * Creates a [BackgroundAnnotation] whose fill color is resolved lazily on each draw pass via
     * [colorProducer], enabling animations without relayout.
     *
     * @param colorProducer deferred color producer evaluated during the draw phase
     * @param cornerRadius radius applied to outer and inner corners of the contour; defaults to
     *   `0.dp`. Coerced to at most half the length of the adjacent edge
     * @param spread per-edge draw-phase spread applied to the range's bounding boxes (positive
     *   expands outward, negative shrinks inward); defaults to [SpreadValues.Zero]
     * @param mergeLines whether vertically adjacent overlapping boxes on consecutive lines join
     *   into a single contour (`true`) or draw as independent rounded rectangles (`false`);
     *   defaults to `true`
     * @param border optional [Border] drawn as a centered stroke along the background contour;
     *   defaults to `null` (no border)
     * @param alpha opacity applied to both fill and border, from `0.0f` to `1.0f`; defaults to
     *   `1.0f`
     */
    public constructor(
        colorProducer: ColorProducer,
        cornerRadius: Dp = DefaultCornerRadius,
        spread: SpreadValues = DefaultSpread,
        mergeLines: Boolean = DefaultMergeLines,
        border: Border? = null,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
    ) : this(
        color = Color.Unspecified,
        colorProducer = colorProducer,
        brush = null,
        cornerRadius = cornerRadius,
        spread = spread,
        mergeLines = mergeLines,
        border = border,
        alpha = alpha,
    )

    /**
     * Creates a [BackgroundAnnotation] filled with a [brush].
     *
     * @param brush fill brush drawn behind the annotated range
     * @param cornerRadius radius applied to outer and inner corners of the contour; defaults to
     *   `0.dp`. Coerced to at most half the length of the adjacent edge
     * @param spread per-edge draw-phase spread applied to the range's bounding boxes (positive
     *   expands outward, negative shrinks inward); defaults to [SpreadValues.Zero]
     * @param mergeLines whether vertically adjacent overlapping boxes on consecutive lines join
     *   into a single contour (`true`) or draw as independent rounded rectangles (`false`);
     *   defaults to `true`
     * @param border optional [Border] drawn as a centered stroke along the background contour;
     *   defaults to `null` (no border)
     * @param alpha opacity applied to both fill and border, from `0.0f` to `1.0f`; defaults to
     *   `1.0f`
     */
    public constructor(
        brush: Brush,
        cornerRadius: Dp = DefaultCornerRadius,
        spread: SpreadValues = DefaultSpread,
        mergeLines: Boolean = DefaultMergeLines,
        border: Border? = null,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = DefaultAlpha,
    ) : this(
        color = Color.Unspecified,
        colorProducer = null,
        brush = brush,
        cornerRadius = cornerRadius,
        spread = spread,
        mergeLines = mergeLines,
        border = border,
        alpha = alpha,
    )

    /**
     * Per-edge [Dp] spread applied to a text range's bounding boxes during the draw phase.
     *
     * Spread applies to the per-line boxes returned by [TextLayoutResult.getBoundingBoxesForRange],
     * rather than to each glyph individually:
     * - Neighboring characters on the same line whose horizontal bounds touch or overlap are merged
     *   into a single box spanning the line's `[top, bottom]`.
     * - Multi-line ranges and bidirectional text ranges that split into separate visual runs on the
     *   same line produce one box per run.
     *
     * By default ([SpreadValues.Zero]), each box spans the full line height (`[top, bottom]`) and
     * fits tightly around the glyphs horizontally. Configure [BackgroundAnnotation.spread] with
     * [SpreadValues] to adjust those bounds at draw time without shifting text layout:
     * - Positive horizontal spread (for example, `SpreadValues(horizontal = 4.dp)`) expands the
     *   left and right edges outward to add space between the first or last glyph and a pill's
     *   border or rounded corners. If horizontal expansion causes two separate boxes on the same
     *   line to touch or overlap, they are merged into a single box.
     * - Negative vertical spread (for example, `SpreadValues(vertical = (-2).dp)`): when
     *   [BackgroundAnnotation.mergeLines] is `false`, shrinks each per-line box's top and bottom
     *   edges inward from the line bounds so pills on consecutive lines have a vertical gap instead
     *   of touching; when [BackgroundAnnotation.mergeLines] is `true`, shrinks only the outer top
     *   of the first line and outer bottom of the last line of each merged stack while inner line
     *   seams remain joined as a continuous contour.
     * - Positive vertical spread expands the top and bottom edges outward past the line bounds (on
     *   each per-line box when `mergeLines = false`, or on the outer top and bottom of each merged
     *   stack when `mergeLines = true`).
     *
     * If negative spread values reduce a box's width or height (or a merged line stack's height) to
     * `<= 0`, that box or stack is omitted and not drawn.
     *
     * @sample androidx.compose.ui.text.samples.BackgroundAnnotationSpreadSample
     * @property left spread applied to the visual left edge of each per-line bounding box (positive
     *   expands outward, negative shrinks inward); defaults to `0.dp`
     * @property top spread applied to the visual top edge of each box, or the outer top edge of
     *   each merged stack when [BackgroundAnnotation.mergeLines] is `true` (positive expands
     *   outward, negative shrinks inward); defaults to `0.dp`
     * @property right spread applied to the visual right edge of each per-line bounding box
     *   (positive expands outward, negative shrinks inward); defaults to `0.dp`
     * @property bottom spread applied to the visual bottom edge of each box, or the outer bottom
     *   edge of each merged stack when [BackgroundAnnotation.mergeLines] is `true` (positive
     *   expands outward, negative shrinks inward); defaults to `0.dp`
     */
    @Immutable
    public class SpreadValues(
        public val left: Dp = 0.dp,
        public val top: Dp = 0.dp,
        public val right: Dp = 0.dp,
        public val bottom: Dp = 0.dp,
    ) {
        /**
         * Creates [SpreadValues] with symmetric [horizontal] (`left` and `right`) and [vertical]
         * (`top` and `bottom`) spread amounts.
         *
         * @param horizontal spread applied to `left` and `right` (positive expands outward,
         *   negative shrinks inward); defaults to `0.dp`
         * @param vertical spread applied to `top` and `bottom` (positive expands outward, negative
         *   shrinks inward); defaults to `0.dp`
         */
        public constructor(
            horizontal: Dp = 0.dp,
            vertical: Dp = 0.dp,
        ) : this(horizontal, vertical, horizontal, vertical)

        /** Creates [SpreadValues] with [all] four edges spread by the same amount. */
        public constructor(all: Dp) : this(all, all, all, all)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SpreadValues) return false
            return left == other.left &&
                top == other.top &&
                right == other.right &&
                bottom == other.bottom
        }

        override fun hashCode(): Int {
            var result = left.hashCode()
            result = 31 * result + top.hashCode()
            result = 31 * result + right.hashCode()
            result = 31 * result + bottom.hashCode()
            return result
        }

        public override fun toString(): String =
            "SpreadValues(left=$left, top=$top, right=$right, bottom=$bottom)"

        public companion object {
            /** A [SpreadValues] with `0.dp` spread on all four edges. */
            public val Zero: SpreadValues = SpreadValues(0.dp, 0.dp, 0.dp, 0.dp)
        }
    }

    /**
     * Border stroke specification for [BackgroundAnnotation].
     *
     * Equivalent to `BorderStroke` in `androidx.compose.foundation` (which `ui-text` does not
     * depend on), with an additional [ColorProducer] overload for draw-phase color animations.
     *
     * Drawn as a centered stroke along the background contour: half of [width] extends outside the
     * contour (past [spread]) and half overlaps inside the fill.
     *
     * @property width stroke width, or [Dp.Hairline] for a single-pixel hairline stroke
     * @property color stroke color applied to the contour border, or [Color.Unspecified] when
     *   constructed with [colorProducer] or [brush]
     * @property colorProducer deferred color producer evaluated during the draw phase, or `null`
     *   when constructed with [color] or [brush]
     * @property brush stroke brush applied to the contour border, or `null` when constructed with
     *   [color] or [colorProducer]
     */
    @Immutable
    public class Border
    private constructor(
        public val width: Dp,
        public val color: Color,
        public val colorProducer: ColorProducer?,
        public val brush: Brush?,
    ) {
        /**
         * Creates a [Border] with a stroke [width] and a solid [color].
         *
         * @param width stroke width, or [Dp.Hairline] for a single-pixel hairline stroke
         * @param color stroke color applied to the contour border
         */
        public constructor(
            width: Dp,
            color: Color,
        ) : this(width = width, color = color, colorProducer = null, brush = null)

        /**
         * Creates a [Border] with a stroke [width] whose color is resolved lazily on each draw pass
         * via [colorProducer], enabling border color animations without relayout.
         *
         * @param width stroke width, or [Dp.Hairline] for a single-pixel hairline stroke
         * @param colorProducer deferred color producer evaluated during the draw phase
         */
        public constructor(
            width: Dp,
            colorProducer: ColorProducer,
        ) : this(
            width = width,
            color = Color.Unspecified,
            colorProducer = colorProducer,
            brush = null,
        )

        /**
         * Creates a [Border] with a stroke [width] painted with a [brush].
         *
         * @param width stroke width, or [Dp.Hairline] for a single-pixel hairline stroke
         * @param brush stroke brush applied to the contour border
         */
        public constructor(
            width: Dp,
            brush: Brush,
        ) : this(width = width, color = Color.Unspecified, colorProducer = null, brush = brush)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Border) return false
            if (width != other.width) return false
            if (color != other.color) return false
            if (colorProducer != other.colorProducer) return false
            if (brush != other.brush) return false
            return true
        }

        override fun hashCode(): Int {
            var result = width.hashCode()
            result = 31 * result + color.hashCode()
            result = 31 * result + (colorProducer?.hashCode() ?: 0)
            result = 31 * result + (brush?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String =
            "Border(width=$width, color=$color, colorProducer=$colorProducer, brush=$brush)"
    }

    override fun DrawAnnotationScope.drawBehind() {
        val resolvedFillColor = colorProducer?.invoke() ?: color
        val hasFill = brush != null || resolvedFillColor.isSpecified
        val border = border
        val resolvedBorderColor =
            if (border != null) border.colorProducer?.invoke() ?: border.color
            else Color.Unspecified
        val hasBorder =
            border != null &&
                border.width.isSpecified &&
                border.width.value >= 0f &&
                (border.brush != null || resolvedBorderColor.isSpecified)
        if (!hasFill && !hasBorder) return

        val targetPath = path ?: Path().also { path = it }
        val cornerRadiusPx = cornerRadius.toPx()
        textLayoutResult.calculatePathFromBoundingBoxes(
            start = start,
            end = end,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            spreadLeft = spread.left.toPx(),
            spreadTop = spread.top.toPx(),
            spreadRight = spread.right.toPx(),
            spreadBottom = spread.bottom.toPx(),
            mergeLines = mergeLines,
            target = targetPath,
        )
        if (brush != null) {
            drawPath(
                path = targetPath,
                brush = brush,
                alpha = alpha,
                style = Fill,
            )
        } else if (resolvedFillColor.isSpecified) {
            drawPath(
                path = targetPath,
                color = resolvedFillColor,
                alpha = alpha,
                style = Fill,
            )
        }
        if (border != null && hasBorder) {
            val strokeWidthPx =
                if (border.width == Dp.Hairline) Stroke.HairlineWidth else border.width.toPx()
            val stroke = Stroke(width = strokeWidthPx)
            if (border.brush != null) {
                drawPath(
                    path = targetPath,
                    brush = border.brush,
                    alpha = alpha,
                    style = stroke,
                )
            } else {
                drawPath(
                    path = targetPath,
                    color = resolvedBorderColor,
                    alpha = alpha,
                    style = stroke,
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackgroundAnnotation) return false
        if (color != other.color) return false
        if (colorProducer != other.colorProducer) return false
        if (brush != other.brush) return false
        if (cornerRadius != other.cornerRadius) return false
        if (spread != other.spread) return false
        if (mergeLines != other.mergeLines) return false
        if (border != other.border) return false
        if (alpha != other.alpha) return false
        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + (colorProducer?.hashCode() ?: 0)
        result = 31 * result + (brush?.hashCode() ?: 0)
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + spread.hashCode()
        result = 31 * result + mergeLines.hashCode()
        result = 31 * result + (border?.hashCode() ?: 0)
        result = 31 * result + alpha.hashCode()
        return result
    }

    override fun toString(): String {
        return "BackgroundAnnotation(" +
            "color=$color, " +
            "colorProducer=$colorProducer, " +
            "brush=$brush, " +
            "cornerRadius=$cornerRadius, " +
            "spread=$spread, " +
            "mergeLines=$mergeLines, " +
            "border=$border, " +
            "alpha=$alpha)"
    }
}

private val DefaultCornerRadius: Dp = 0.dp
private val DefaultSpread: BackgroundAnnotation.SpreadValues =
    BackgroundAnnotation.SpreadValues.Zero
private const val DefaultMergeLines: Boolean = true
private const val DefaultAlpha: Float = 1.0f
