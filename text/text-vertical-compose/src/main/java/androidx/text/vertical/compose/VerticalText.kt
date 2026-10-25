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

package androidx.text.vertical.compose

import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.text.vertical.TextOrientation
import androidx.text.vertical.VerticalTextLayout
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Displays [text] laid out vertically with [VerticalTextLayout].
 *
 * Accepts a plain [CharSequence], an [AnnotatedString], or an Android [android.text.Spanned] with
 * vertical spans such as [androidx.text.vertical.RubySpan], [androidx.text.vertical.EmphasisSpan],
 * [androidx.text.vertical.FontShearSpan], and [androidx.text.vertical.TextOrientationSpan].
 * [buildVerticalText] creates such a [android.text.Spanned]. Treat an [android.text.Spannable] as
 * immutable once passed: later span edits are not observed.
 *
 * For an [AnnotatedString], honors [androidx.compose.ui.text.SpanStyle.color],
 * [androidx.compose.ui.text.SpanStyle.fontSize], [androidx.compose.ui.text.SpanStyle.fontWeight],
 * [androidx.compose.ui.text.SpanStyle.fontStyle], [androidx.compose.ui.text.SpanStyle.background],
 * and [androidx.compose.ui.text.SpanStyle.letterSpacing]. Ignores other span styles and all
 * paragraph styles.
 *
 * Columns start at the right edge of the layout and advance to the left. The width wraps to the
 * columns that fit in the height constraint, capped at [maxColumns] and padded to [minColumns], and
 * the height wraps to the tallest drawn column.
 *
 * @param text text to display
 * @param modifier [Modifier] to apply to this layout node
 * @param style [VerticalTextStyle] for text measurement and rendering
 * @param overflow how to handle text that overflows the bounds. [TextOverflow.Visible] draws past
 *   the bounds; every other value clips. No value adds an ellipsis
 * @param maxColumns maximum number of columns to measure and draw. `0` measures and draws no
 *   column. Defaults to no limit
 * @param minColumns minimum number of columns to reserve in the measured width. `0` lets empty text
 *   measure zero width. Extra columns use the bounding column width across all laid-out columns, or
 *   the resolved font size when [text] is empty
 * @param orientation [TextOrientation] mode for each character
 * @throws IllegalArgumentException if [minColumns] or [maxColumns] is negative, or if [minColumns]
 *   is greater than [maxColumns]
 */
@Composable
public fun VerticalText(
    text: CharSequence,
    modifier: Modifier = Modifier,
    style: VerticalTextStyle = VerticalTextStyle.Default,
    overflow: TextOverflow = TextOverflow.Clip,
    maxColumns: Int = Int.MAX_VALUE,
    minColumns: Int = 1,
    orientation: TextOrientation = TextOrientation.Mixed,
) {
    require(minColumns >= 0 && maxColumns >= 0) {
        "minColumns ($minColumns) and maxColumns ($maxColumns) must be non-negative"
    }
    require(minColumns <= maxColumns) {
        "minColumns ($minColumns) must be less than or equal to maxColumns ($maxColumns)"
    }

    val cache = remember { VerticalTextLayoutCache() }
    var textLayout by remember { mutableStateOf<VerticalTextLayout?>(null) }
    val clipModifier = if (overflow != TextOverflow.Visible) Modifier.clipToBounds() else Modifier
    val semanticsText =
        remember(text) { text as? AnnotatedString ?: AnnotatedString(text.toString()) }
    val semanticsModifier = Modifier.semantics { this.text = semanticsText }
    val drawModifier = Modifier.drawBehind {
        val currentLayout = textLayout ?: return@drawBehind
        val drawnColumns = min(currentLayout.lineCount, maxColumns)
        drawIntoCanvas { canvas ->
            currentLayout.draw(canvas.nativeCanvas, size.width, 0f, 0, drawnColumns)
        }
    }
    val density = LocalDensity.current
    val resolver = LocalFontFamilyResolver.current
    val typeface by
        remember(
            style.fontFamily,
            style.fontWeight,
            style.fontStyle,
            style.fontSynthesis,
            resolver,
        ) {
            resolver.resolve(
                style.fontFamily,
                style.fontWeight ?: FontWeight.Normal,
                style.fontStyle ?: FontStyle.Normal,
                style.fontSynthesis ?: FontSynthesis.All,
            )
        }

    // TODO(b/571750156): The default MeasurePolicy answers intrinsic queries by running this
    // measure lambda, which can evict the cache with an intrinsic height.
    Layout(modifier = modifier.then(semanticsModifier).then(clipModifier).then(drawModifier)) {
        _,
        constraints ->
        val vtl =
            cache.getLayout(
                text,
                constraints.maxHeight,
                orientation,
                style,
                typeface as Typeface,
                density,
            )
        textLayout = vtl

        val columnWidth = if (vtl.lineCount > 0) vtl.columnWidth else cache.lastTextSize
        val effectiveColumns = min(vtl.lineCount, maxColumns)
        var desiredWidth = 0f
        var desiredHeight = 0f
        for (i in 0 until effectiveColumns) {
            desiredWidth += vtl.getLineWidth(i)
            desiredHeight = max(desiredHeight, vtl.getLineHeight(i))
        }
        val extraMinColumns = (minColumns - effectiveColumns).coerceAtLeast(0)

        val layoutWidth =
            ceil(desiredWidth + columnWidth * extraMinColumns)
                .toInt()
                .coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight =
            ceil(desiredHeight).toInt().coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {}
    }
}

/** Caches the most recent [VerticalTextLayout] across measure and draw passes. */
internal class VerticalTextLayoutCache {
    private var cachedLayout: VerticalTextLayout? = null
    private var lastText: CharSequence? = null
    private var lastConstraintsHeight: Int = -1
    private var lastOrientation: TextOrientation? = null
    private var lastStyle: VerticalTextStyle? = null
    private var lastTypeface: Typeface? = null
    private var lastDensity: Density? = null

    /** Text size in pixels that the last cache miss applied to its [TextPaint]. */
    var lastTextSize: Float = 0f
        private set

    /**
     * Returns the cached [VerticalTextLayout] when all inputs match, or builds a new one.
     *
     * Each cache miss creates its own [TextPaint], so a layout returned earlier keeps the paint it
     * was built with.
     *
     * @param text text to lay out, which may be an [AnnotatedString] or [android.text.Spanned]
     * @param height maximum height in pixels, or [Constraints.Infinity] for unbounded height
     * @param orientation [TextOrientation] mode for each character
     * @param style [VerticalTextStyle] for measurement and rendering
     * @param typeface resolved [Typeface] for [style]
     * @param density current [Density]
     * @return measured [VerticalTextLayout]
     */
    fun getLayout(
        text: CharSequence,
        height: Int,
        orientation: TextOrientation,
        style: VerticalTextStyle,
        typeface: Typeface,
        density: Density,
    ): VerticalTextLayout {
        val cached = cachedLayout
        if (
            cached != null &&
                lastText == text &&
                lastConstraintsHeight == height &&
                lastOrientation == orientation &&
                lastStyle == style &&
                lastTypeface == typeface &&
                lastDensity == density
        ) {
            return cached
        }
        val paint = TextPaint()
        setStyleToPaint(style, typeface, density, paint)
        val layoutText = if (text is AnnotatedString) text.toSpanned(density) else text
        val heightConstraint =
            if (height == Constraints.Infinity) Float.POSITIVE_INFINITY else height.toFloat()
        val newLayout =
            VerticalTextLayout(
                text = layoutText,
                start = 0,
                end = layoutText.length,
                paint = paint,
                height = heightConstraint,
                orientation = orientation,
            )
        cachedLayout = newLayout
        lastTextSize = paint.textSize
        lastText = text
        lastConstraintsHeight = height
        lastOrientation = orientation
        lastStyle = style
        lastTypeface = typeface
        lastDensity = density
        return newLayout
    }
}
