/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.creation.compose.capture.LocalRemoteDensity
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

private val DefaultFontSize: RemoteTextUnit = 12.rsp
private val EmptyRemoteString: RemoteString = "".rs

/**
 * Creates and remembers a [RemoteTextMeasurer] using [LocalRemoteDensity] and an optional default
 * [style].
 *
 * Mirrors [androidx.compose.ui.text.rememberTextMeasurer] from Compose UI.
 *
 * @param style default [RemoteTextStyle] used when `style` is omitted in measurement calls
 * @return remembered [RemoteTextMeasurer] for the current [LocalRemoteDensity]
 */
@RemoteComposable
@Composable
public fun rememberRemoteTextMeasurer(
    style: RemoteTextStyle = RemoteTextStyle.Default
): RemoteTextMeasurer {
    val density = LocalRemoteDensity.current
    return remember(density, style) { RemoteTextMeasurer(density = density, style = style) }
}

/**
 * Measures single-line remote text layout bounds, font metrics, tight paint bounds, and tabular
 * width.
 *
 * Each measurement method inspects a different aspect of text geometry:
 * * [measureSingleLine] returns [RemoteSingleLineTextBounds] with the single-line layout advance
 *   width (from [android.graphics.Paint.measureText], including leading/trailing spaces and glyph
 *   side-bearings), font-box height (`descent - ascent` from
 *   [android.graphics.Paint.getFontMetrics]), and alphabetic [RemoteSingleLineTextBounds.baseline]
 *   (`-ascent`). For example, `"ac"` and `"Ág"` share the same height and baseline, and `" Hello "`
 *   includes the advance width of the surrounding spaces.
 * * [measureFontMetrics] returns [RemoteFontMetrics] with the typeface's baseline-relative
 *   [RemoteFontMetrics.ascent] (negative above the baseline) and [RemoteFontMetrics.descent]
 *   (positive below the baseline), independent of any specific text string.
 * * [measurePaintBounds] returns [RemotePaintBounds] with the tight inked pixel bounding box of the
 *   rendered glyphs (from [android.graphics.Paint.getTextBounds]) relative to `(0, 0)` at the start
 *   of the text on its alphabetic baseline. Unlike [measureSingleLine], leading and trailing
 *   whitespace is excluded and vertical bounds wrap only the glyphs present in the string (for
 *   example, `"ac"` has a smaller height and higher bottom than `"Ág"`, and `" Hello "` has a
 *   positive [RemotePaintBounds.left] starting at the first inked pixel of `'H'`).
 * * [measureMonospaceWidth] returns the tabular width of the string using fixed-width `'0'`
 *   character slots.
 *
 * All measurements operate on a single unbroken run without paragraph line breaking; newline
 * characters (`'\n'`) are not split into multiple lines or replaced.
 *
 * @param density default [RemoteDensity] of the measurement environment, used to convert font sizes
 *   to pixels when not overridden in a measurement call
 * @param style default [RemoteTextStyle] applied when not overridden in a measurement call
 */
@Immutable
public class RemoteTextMeasurer(
    internal val density: RemoteDensity,
    internal val style: RemoteTextStyle = RemoteTextStyle.Default,
) {
    /**
     * Measures [text] as a single unconstrained line and returns [RemoteSingleLineTextBounds].
     *
     * Mirrors [androidx.compose.ui.text.TextMeasurer.measure] for single-line text (`softWrap =
     * false`, `maxLines = 1`) with `includeFontPadding` disabled:
     * * [RemoteSingleLineTextBounds.size] spans the advance width (from
     *   [android.graphics.Paint.measureText], including leading/trailing spaces and glyph
     *   side-bearings) and the font-box height (`descent - ascent` from
     *   [android.graphics.Paint.getFontMetrics]), matching a single-line [RemoteText] layout box
     *   and [androidx.compose.ui.text.TextLayoutResult.size]. Unlike [measurePaintBounds], vertical
     *   bounds span the full font ascent-to-descent box regardless of which characters appear in
     *   [text], and horizontal bounds include surrounding whitespace.
     * * [RemoteSingleLineTextBounds.baseline] gives the distance from the top of the layout box to
     *   the alphabetic baseline (`-ascent`).
     * * [text] is measured as a single run without paragraph line breaking; newline characters
     *   (`'\n'`) are not broken into multiple lines or replaced with whitespace.
     * * Font size is resolved in order from `style.fontSize`, the measurer's default
     *   `style.fontSize`, or `12.rsp`.
     *
     * @param text remote string to measure as a single line
     * @param style [RemoteTextStyle] to apply, defaulting to the measurer's default style
     * @param density [RemoteDensity] used to convert font sizes to pixels, defaulting to the
     *   measurer's default density
     * @return [RemoteSingleLineTextBounds] containing single-line layout [RemoteSize] and baseline
     */
    @Stable
    public fun measureSingleLine(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteSingleLineTextBounds {
        val width = measureLayoutWidth(text, style, density)
        val height = measureLayoutHeight(text, style, density)
        val ascent = measureAscent(text, style, density)
        return RemoteSingleLineTextBounds(
            size = RemoteSize(width, height),
            baseline = 0f.rf - ascent,
        )
    }

    /**
     * Measures the font ascent and descent for [style] and returns [RemoteFontMetrics].
     *
     * * Corresponds to [android.graphics.Paint.FontMetrics.ascent] and
     *   [android.graphics.Paint.FontMetrics.descent].
     * * Unlike [measurePaintBounds], [RemoteFontMetrics] reflects the typeface's vertical metrics
     *   across all glyphs rather than the tight ink bounds of a specific string; `descent - ascent`
     *   equals `measureSingleLine(...).size.height`.
     * * Font size is resolved in order from `style.fontSize`, the measurer's default
     *   `style.fontSize`, or `12.rsp`.
     *
     * @param style [RemoteTextStyle] to inspect, defaulting to the measurer's default style
     * @param density [RemoteDensity] used to convert font sizes to pixels, defaulting to the
     *   measurer's default density
     * @return [RemoteFontMetrics] containing baseline-relative [RemoteFontMetrics.ascent] and
     *   [RemoteFontMetrics.descent]
     */
    @Stable
    public fun measureFontMetrics(
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteFontMetrics =
        RemoteFontMetrics(
            ascent = measureAscent(EmptyRemoteString, style, density),
            descent = measureDescent(EmptyRemoteString, style, density),
        )

    /**
     * Measures the tight paint bounds of [text] and returns [RemotePaintBounds].
     *
     * * Corresponds to [android.graphics.Paint.getTextBounds] with `(0, 0)` at the start of [text]
     *   on its alphabetic baseline.
     * * Unlike [measureSingleLine] and [measureFontMetrics], which span the full advance width
     *   (including spaces) and font-wide ascent/descent box, [RemotePaintBounds] tightly wraps only
     *   the inked pixels of [text]: for example, `"ac"` has a smaller height and higher bottom than
     *   `"Ág"`, and `" Hello "` has a positive [RemotePaintBounds.left] starting at the first inked
     *   pixel of `'H'` and excludes trailing spaces from [RemotePaintBounds.right].
     * * [text] is measured as a single run without paragraph line breaking; newline characters
     *   (`'\n'`) are not broken into multiple lines.
     * * Font size is resolved in order from `style.fontSize`, the measurer's default
     *   `style.fontSize`, or `12.rsp`.
     *
     * @param text remote string whose paint bounds to measure
     * @param style [RemoteTextStyle] to apply, defaulting to the measurer's default style
     * @param density [RemoteDensity] used to convert font sizes to pixels, defaulting to the
     *   measurer's default density
     * @return [RemotePaintBounds] containing baseline-relative tight paint bounds
     */
    @Stable
    public fun measurePaintBounds(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemotePaintBounds =
        RemotePaintBounds(
            left = measureGlyphLeft(text, style, density),
            top = measureGlyphTop(text, style, density),
            right = measureGlyphRight(text, style, density),
            bottom = measureGlyphBottom(text, style, density),
        )

    /**
     * Measures the tabular width of [text] in pixels using fixed-width `'0'` character slots.
     *
     * * Approximates measuring tabular numerals (`fontFeatureSettings = "tnum"` on
     *   [androidx.compose.ui.text.TextStyle.fontFeatureSettings]).
     * * [text] is measured as a single run without paragraph line breaking; newline characters
     *   (`'\n'`) are not broken into multiple lines.
     * * Font size is resolved in order from `style.fontSize`, the measurer's default
     *   `style.fontSize`, or `12.rsp`.
     *
     * @param text remote string whose tabular width to measure
     * @param style [RemoteTextStyle] to apply, defaulting to the measurer's default style
     * @param density [RemoteDensity] used to convert font sizes to pixels, defaulting to the
     *   measurer's default density
     * @return tabular width in pixels as a [RemoteFloat]
     */
    @Stable
    public fun measureMonospaceWidth(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteFloat =
        measureAttribute(
            text,
            style,
            density,
            (TextAttribute.MEASURE_WIDTH.toInt() or TextAttribute.MEASURE_MONOSPACE_FLAG).toShort(),
        )

    /** Measures the layout advance width of [text], including side-bearings and spaces. */
    internal fun measureLayoutWidth(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteFloat =
        measureAttribute(
            text,
            style,
            density,
            (TextAttribute.MEASURE_WIDTH.toInt() or (PaintContext.TEXT_MEASURE_SPACES shl 8))
                .toShort(),
        )

    /** Measures the font-box layout height (`descent - ascent`) of [text]. */
    internal fun measureLayoutHeight(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteFloat =
        measureAttribute(
            text,
            style,
            density,
            (TextAttribute.MEASURE_HEIGHT.toInt() or TextAttribute.MEASURE_MAX_HEIGHT_FLAG)
                .toShort(),
        )

    /** Measures the tight left glyph ink bound of [text] relative to its origin. */
    internal fun measureGlyphLeft(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteFloat = measureAttribute(text, style, density, TextAttribute.MEASURE_LEFT)

    /** Measures the tight top glyph ink bound of [text] relative to its baseline. */
    internal fun measureGlyphTop(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteFloat = measureAttribute(text, style, density, TextAttribute.MEASURE_TOP)

    /** Measures the tight right glyph ink bound of [text] relative to its origin. */
    internal fun measureGlyphRight(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteFloat = measureAttribute(text, style, density, TextAttribute.MEASURE_RIGHT)

    /** Measures the tight bottom glyph ink bound of [text] relative to its baseline. */
    internal fun measureGlyphBottom(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteFloat = measureAttribute(text, style, density, TextAttribute.MEASURE_BOTTOM)

    /** Measures the font ascent of [text] relative to its baseline. */
    internal fun measureAscent(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteFloat =
        measureAttribute(
            text,
            style,
            density,
            (TextAttribute.MEASURE_TOP.toInt() or TextAttribute.MEASURE_MAX_HEIGHT_FLAG).toShort(),
        )

    /** Measures the font descent of [text] relative to its baseline. */
    internal fun measureDescent(
        text: RemoteString,
        style: RemoteTextStyle = this.style,
        density: RemoteDensity = this.density,
    ): RemoteFloat =
        measureAttribute(
            text,
            style,
            density,
            (TextAttribute.MEASURE_BOTTOM.toInt() or TextAttribute.MEASURE_MAX_HEIGHT_FLAG)
                .toShort(),
        )

    private fun measureAttribute(
        text: RemoteString,
        style: RemoteTextStyle,
        density: RemoteDensity,
        attributeType: Short,
    ): RemoteFloat {
        val resolvedFontSize = style.fontSize ?: this.style.fontSize ?: DefaultFontSize
        val textSize = resolvedFontSize.toPx(density)
        val resolvedWeight = style.fontWeight ?: this.style.fontWeight ?: RemoteFontWeight.Normal
        val resolvedStyle = style.fontStyle ?: this.style.fontStyle

        return RemoteFloat {
            val textSizePxId = textSize.getFloatIdForCreationState(this)
            document.rcPaint
                .setTextSize(textSizePxId)
                .setTypeface(
                    0,
                    resolvedWeight.weight.constantValueOrNull?.toInt() ?: 400,
                    resolvedStyle == RemoteFontStyle.Italic,
                )
                .commit()

            RemoteFloat(
                document.textAttribute(
                    text.getIdForCreationState(this),
                    attributeType,
                )
            )
        }
    }
}

/**
 * Contains the single-line layout dimensions and baseline from
 * [RemoteTextMeasurer.measureSingleLine].
 *
 * @property size single-line layout box dimensions in pixels
 * @property baseline distance in pixels from the top of the layout box to the alphabetic baseline
 */
@Immutable
public class RemoteSingleLineTextBounds(
    /**
     * Contains the single-line layout box dimensions in pixels.
     *
     * * Matches [androidx.compose.ui.text.TextLayoutResult.size] when `includeFontPadding` is
     *   `false`: `size.width` includes glyph side-bearings and spaces, and `size.height` spans the
     *   font box (`descent - ascent`).
     */
    public val size: RemoteSize,
    /**
     * Contains the distance in pixels from the top of the layout box to the alphabetic baseline.
     *
     * * Matches [androidx.compose.ui.text.TextLayoutResult.firstBaseline] for a single-line layout
     *   (`-ascent` when `includeFontPadding` is `false`).
     */
    public val baseline: RemoteFloat,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteSingleLineTextBounds) return false
        return size.width.isStructurallyEqual(other.size.width) &&
            size.height.isStructurallyEqual(other.size.height) &&
            baseline.isStructurallyEqual(other.baseline)
    }

    override fun hashCode(): Int {
        var result = size.width.cacheKey.hashCode()
        result = 31 * result + size.height.cacheKey.hashCode()
        result = 31 * result + baseline.cacheKey.hashCode()
        return result
    }

    override fun toString(): String = "RemoteSingleLineTextBounds(size=$size, baseline=$baseline)"
}

/**
 * Contains the font ascent and descent measured by [RemoteTextMeasurer.measureFontMetrics].
 *
 * @property ascent font ascent in pixels relative to the baseline (negative above baseline)
 * @property descent font descent in pixels relative to the baseline (positive below baseline)
 */
@Immutable
public class RemoteFontMetrics(
    /**
     * Contains the font ascent in pixels relative to the baseline (negative above baseline).
     *
     * * Corresponds to [android.graphics.Paint.FontMetrics.ascent] (and `getLineTop(0) -
     *   firstBaseline` on [androidx.compose.ui.text.TextLayoutResult] when `includeFontPadding` is
     *   `false`).
     */
    public val ascent: RemoteFloat,
    /**
     * Contains the font descent in pixels relative to the baseline (positive below baseline).
     *
     * * Corresponds to [android.graphics.Paint.FontMetrics.descent] (and `getLineBottom(0) -
     *   firstBaseline` on [androidx.compose.ui.text.TextLayoutResult] when `includeFontPadding` is
     *   `false`).
     */
    public val descent: RemoteFloat,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteFontMetrics) return false
        return ascent.isStructurallyEqual(other.ascent) &&
            descent.isStructurallyEqual(other.descent)
    }

    override fun hashCode(): Int {
        var result = ascent.cacheKey.hashCode()
        result = 31 * result + descent.cacheKey.hashCode()
        return result
    }

    override fun toString(): String = "RemoteFontMetrics(ascent=$ascent, descent=$descent)"
}

/**
 * Contains the tight paint bounds measured by [RemoteTextMeasurer.measurePaintBounds].
 *
 * Coordinates are in pixels relative to `(0, 0)` at the start of the text on its alphabetic
 * baseline, matching [android.graphics.Paint.getTextBounds]:
 * * Add [RemoteSingleLineTextBounds.baseline] (`baseline + top` and `baseline + bottom`) to convert
 *   [top] and [bottom] into single-line layout-box `y` coordinates.
 *
 * @property left tight left paint bound in pixels relative to the text origin
 * @property top tight top paint bound in pixels relative to the baseline (negative above baseline)
 * @property right tight right paint bound in pixels relative to the text origin
 * @property bottom tight bottom paint bound in pixels relative to the baseline (positive below
 *   baseline)
 */
@Immutable
public class RemotePaintBounds(
    /**
     * Contains the tight left paint bound in pixels relative to the text origin.
     *
     * * Corresponds to [android.graphics.Rect.left] from [android.graphics.Paint.getTextBounds].
     */
    public val left: RemoteFloat,
    /**
     * Contains the tight top paint bound in pixels relative to the baseline (negative above
     * baseline).
     *
     * * Corresponds to [android.graphics.Rect.top] from [android.graphics.Paint.getTextBounds].
     */
    public val top: RemoteFloat,
    /**
     * Contains the tight right paint bound in pixels relative to the text origin.
     *
     * * Corresponds to [android.graphics.Rect.right] from [android.graphics.Paint.getTextBounds].
     */
    public val right: RemoteFloat,
    /**
     * Contains the tight bottom paint bound in pixels relative to the baseline (positive below
     * baseline).
     *
     * * Corresponds to [android.graphics.Rect.bottom] from [android.graphics.Paint.getTextBounds].
     */
    public val bottom: RemoteFloat,
) {
    /** Returns the tight paint width (`right - left`) in pixels. */
    public val width: RemoteFloat
        get() = right - left

    /** Returns the tight paint height (`bottom - top`) in pixels. */
    public val height: RemoteFloat
        get() = bottom - top

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemotePaintBounds) return false
        return left.isStructurallyEqual(other.left) &&
            top.isStructurallyEqual(other.top) &&
            right.isStructurallyEqual(other.right) &&
            bottom.isStructurallyEqual(other.bottom)
    }

    override fun hashCode(): Int {
        var result = left.cacheKey.hashCode()
        result = 31 * result + top.cacheKey.hashCode()
        result = 31 * result + right.cacheKey.hashCode()
        result = 31 * result + bottom.cacheKey.hashCode()
        return result
    }

    override fun toString(): String =
        "RemotePaintBounds(left=$left, top=$top, right=$right, bottom=$bottom)"
}
