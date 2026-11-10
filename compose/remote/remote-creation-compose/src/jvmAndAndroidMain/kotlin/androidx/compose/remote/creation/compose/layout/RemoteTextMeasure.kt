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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.creation.compose.capture.LocalRemoteDensity
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

private val DefaultFontSize: RemoteTextUnit = 12.rsp

/**
 * Creates and remembers a [RemoteTextMeasurer] using [LocalRemoteDensity].
 *
 * Mirrors [androidx.compose.ui.text.rememberTextMeasurer] from Compose UI.
 *
 * @return a remembered [RemoteTextMeasurer] for the current [LocalRemoteDensity]
 */
@RemoteComposable
@Composable
public fun rememberRemoteTextMeasurer(): RemoteTextMeasurer {
    val density = LocalRemoteDensity.current
    return remember(density) { RemoteTextMeasurer(density = density) }
}

/**
 * Measures single-line remote text dimensions.
 *
 * Mirrors [androidx.compose.ui.text.TextMeasurer] from Compose UI.
 *
 * @param density density of the measurement environment, used to convert font sizes to pixels
 */
@Immutable
public class RemoteTextMeasurer(internal val density: RemoteDensity) {
    /**
     * Measures [text] as a single unconstrained line and returns a [RemoteTextLayoutResult].
     *
     * Mirrors [androidx.compose.ui.text.TextMeasurer.measure] for single-line text (`softWrap =
     * false`, `maxLines = 1`) with `includeFontPadding` disabled:
     * * [RemoteTextLayoutResult.size] spans the advance width and font-box height (`descent -
     *   ascent`), matching a single-line [RemoteText] layout box and
     *   [androidx.compose.ui.text.TextLayoutResult.size].
     * * If [fontSize] is null, uses [RemoteTextStyle.fontSize] from [style], or `12.rsp` if
     *   unspecified.
     *
     * @param text remote string to measure
     * @param style [RemoteTextStyle] to apply, defaulting to [RemoteTextStyle.Default]
     * @param fontSize optional font size override; if null, taken from [style] or defaults to
     *   `12.rsp`
     * @return [RemoteTextLayoutResult] containing lazy text layout measurement expressions
     */
    @Stable
    public fun measure(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteTextLayoutResult {
        val width = measureLayoutWidth(text, style, fontSize)
        val height = measureLayoutHeight(text, style, fontSize)
        val monospaceWidth = measureMonospaceWidth(text, style, fontSize)
        val glyphLeft = measureGlyphLeft(text, style, fontSize)
        val glyphTop = measureGlyphTop(text, style, fontSize)
        val glyphRight = measureGlyphRight(text, style, fontSize)
        val glyphBottom = measureGlyphBottom(text, style, fontSize)
        val ascent = measureAscent(text, style, fontSize)
        val descent = measureDescent(text, style, fontSize)
        return RemoteTextLayoutResult(
            size = RemoteSize(width, height),
            monospaceWidth = monospaceWidth,
            glyphLeft = glyphLeft,
            glyphTop = glyphTop,
            glyphRight = glyphRight,
            glyphBottom = glyphBottom,
            ascent = ascent,
            descent = descent,
        )
    }

    /** Measures the layout advance width of [text], including side-bearings and spaces. */
    internal fun measureLayoutWidth(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat =
        measureAttribute(
            text,
            style,
            fontSize,
            (TextAttribute.MEASURE_WIDTH.toInt() or (PaintContext.TEXT_MEASURE_SPACES shl 8))
                .toShort(),
        )

    /** Measures the font-box layout height (`descent - ascent`) of [text]. */
    internal fun measureLayoutHeight(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat =
        measureAttribute(
            text,
            style,
            fontSize,
            (TextAttribute.MEASURE_HEIGHT.toInt() or TextAttribute.MEASURE_MAX_HEIGHT_FLAG)
                .toShort(),
        )

    /** Measures the tabular width of [text] using fixed-width `'0'` character slots. */
    internal fun measureMonospaceWidth(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat =
        measureAttribute(
            text,
            style,
            fontSize,
            (TextAttribute.MEASURE_WIDTH.toInt() or TextAttribute.MEASURE_MONOSPACE_FLAG).toShort(),
        )

    /** Measures the tight left glyph ink bound of [text] relative to its origin. */
    internal fun measureGlyphLeft(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat = measureAttribute(text, style, fontSize, TextAttribute.MEASURE_LEFT)

    /** Measures the tight top glyph ink bound of [text] relative to its baseline. */
    internal fun measureGlyphTop(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat = measureAttribute(text, style, fontSize, TextAttribute.MEASURE_TOP)

    /** Measures the tight right glyph ink bound of [text] relative to its origin. */
    internal fun measureGlyphRight(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat = measureAttribute(text, style, fontSize, TextAttribute.MEASURE_RIGHT)

    /** Measures the tight bottom glyph ink bound of [text] relative to its baseline. */
    internal fun measureGlyphBottom(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat = measureAttribute(text, style, fontSize, TextAttribute.MEASURE_BOTTOM)

    /** Measures the font ascent of [text] relative to its baseline. */
    internal fun measureAscent(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat =
        measureAttribute(
            text,
            style,
            fontSize,
            (TextAttribute.MEASURE_TOP.toInt() or TextAttribute.MEASURE_MAX_HEIGHT_FLAG).toShort(),
        )

    /** Measures the font descent of [text] relative to its baseline. */
    internal fun measureDescent(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat =
        measureAttribute(
            text,
            style,
            fontSize,
            (TextAttribute.MEASURE_BOTTOM.toInt() or TextAttribute.MEASURE_MAX_HEIGHT_FLAG)
                .toShort(),
        )

    private fun measureAttribute(
        text: RemoteString,
        style: RemoteTextStyle,
        fontSize: RemoteTextUnit?,
        attributeType: Short,
    ): RemoteFloat {
        val resolvedFontSize = fontSize ?: style.fontSize ?: DefaultFontSize
        val textSize = resolvedFontSize.toPx(density)

        return RemoteFloat {
            val textSizePxId = textSize.getFloatIdForCreationState(this)
            document.rcPaint
                .setTextSize(textSizePxId)
                .setTypeface(
                    0,
                    (style.fontWeight ?: RemoteFontWeight.Normal)
                        .weight
                        .constantValueOrNull
                        ?.toInt() ?: 400,
                    style.fontStyle == RemoteFontStyle.Italic,
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
 * Holds the result of a remote text layout measurement.
 *
 * Mirrors [androidx.compose.ui.text.TextLayoutResult] for layout-box coordinates ([size], [width],
 * [height]).
 *
 * @property size single-line layout box dimensions in pixels, matching
 *   [androidx.compose.ui.text.TextLayoutResult.size]
 */
@Immutable
public class RemoteTextLayoutResult
/**
 * Creates a [RemoteTextLayoutResult] with measured layout dimensions, baselines, font metrics, and
 * tight glyph bounds.
 *
 * @param size single-line layout box dimensions in pixels, matching
 *   [androidx.compose.ui.text.TextLayoutResult.size]
 * @param monospaceWidth tabular width in pixels using fixed-width `'0'` character slots
 * @param glyphLeft tight left ink bound in pixels relative to the text origin
 * @param glyphTop tight top ink bound in pixels relative to the baseline (negative above baseline)
 * @param glyphRight tight right ink bound in pixels relative to the text origin
 * @param glyphBottom tight bottom ink bound in pixels relative to the baseline (positive below
 *   baseline)
 * @param ascent font ascent in pixels relative to the baseline (negative above baseline)
 * @param descent font descent in pixels relative to the baseline (positive below baseline)
 * @param firstBaseline distance in pixels from the top of the layout box to the first line's
 *   alphabetic baseline (`-ascent` for single-line measurements from [RemoteTextMeasurer.measure])
 * @param lastBaseline distance in pixels from the top of the layout box to the last line's
 *   alphabetic baseline (equals [firstBaseline] for single-line measurements from
 *   [RemoteTextMeasurer.measure])
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    /**
     * Contains the single-line layout box dimensions in pixels.
     *
     * * Matches [androidx.compose.ui.text.TextLayoutResult.size] when `includeFontPadding` is
     *   `false`: [width] includes glyph side-bearings and spaces, and [height] spans the font box
     *   (`descent - ascent`).
     */
    public val size: RemoteSize,
    /**
     * Contains the tabular width in pixels using fixed-width `'0'` character slots.
     *
     * * Approximates measuring tabular numerals (`fontFeatureSettings = "tnum"` on
     *   [androidx.compose.ui.text.TextStyle.fontFeatureSettings]) via
     *   [androidx.compose.ui.text.TextLayoutResult.size].
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val monospaceWidth: RemoteFloat,
    /**
     * Contains the tight left ink bound in pixels relative to the text origin.
     *
     * * Not exposed on [androidx.compose.ui.text.TextLayoutResult] (whose
     *   [androidx.compose.ui.text.TextLayoutResult.getLineLeft] is layout-box relative);
     *   corresponds to [android.graphics.Rect.left] from [android.graphics.Paint.getTextBounds].
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val glyphLeft: RemoteFloat,
    /**
     * Contains the tight top ink bound in pixels relative to the baseline (negative above
     * baseline).
     *
     * * Not exposed on [androidx.compose.ui.text.TextLayoutResult] (whose
     *   [androidx.compose.ui.text.TextLayoutResult.getLineTop] and
     *   [androidx.compose.ui.text.TextLayoutResult.getBoundingBox] span the full line box);
     *   corresponds to [android.graphics.Rect.top] from [android.graphics.Paint.getTextBounds].
     * * Add [firstBaseline] (`firstBaseline + glyphTop`) to convert to layout-box `y` coordinates.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val glyphTop: RemoteFloat,
    /**
     * Contains the tight right ink bound in pixels relative to the text origin.
     *
     * * Not exposed on [androidx.compose.ui.text.TextLayoutResult] (whose
     *   [androidx.compose.ui.text.TextLayoutResult.getLineRight] includes trailing advance);
     *   corresponds to [android.graphics.Rect.right] from [android.graphics.Paint.getTextBounds].
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val glyphRight: RemoteFloat,
    /**
     * Contains the tight bottom ink bound in pixels relative to the baseline (positive below
     * baseline).
     *
     * * Not exposed on [androidx.compose.ui.text.TextLayoutResult] (whose
     *   [androidx.compose.ui.text.TextLayoutResult.getLineBottom] and
     *   [androidx.compose.ui.text.TextLayoutResult.getBoundingBox] span the full line box);
     *   corresponds to [android.graphics.Rect.bottom] from [android.graphics.Paint.getTextBounds].
     * * Add [firstBaseline] (`firstBaseline + glyphBottom`) to convert to layout-box `y`
     *   coordinates.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val glyphBottom: RemoteFloat,
    /**
     * Contains the font ascent in pixels relative to the baseline (negative above baseline).
     *
     * * Corresponds to `getLineTop(0) - firstBaseline` on
     *   [androidx.compose.ui.text.TextLayoutResult] (see
     *   [androidx.compose.ui.text.TextLayoutResult.getLineTop] and
     *   [androidx.compose.ui.text.TextLayoutResult.firstBaseline]) when `includeFontPadding` is
     *   `false`, and to [android.graphics.Paint.FontMetrics.ascent].
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val ascent: RemoteFloat,
    /**
     * Contains the font descent in pixels relative to the baseline (positive below baseline).
     *
     * * Corresponds to `getLineBottom(0) - firstBaseline` on
     *   [androidx.compose.ui.text.TextLayoutResult] (see
     *   [androidx.compose.ui.text.TextLayoutResult.getLineBottom] and
     *   [androidx.compose.ui.text.TextLayoutResult.firstBaseline]) when `includeFontPadding` is
     *   `false`, and to [android.graphics.Paint.FontMetrics.descent].
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val descent: RemoteFloat,
    /**
     * Contains the distance in pixels from the top of the layout box to the first line's baseline.
     *
     * * Matches [androidx.compose.ui.text.TextLayoutResult.firstBaseline] (and `getLineBaseline(0)`
     *   from [androidx.compose.ui.text.TextLayoutResult.getLineBaseline]).
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val firstBaseline: RemoteFloat = 0f.rf - ascent,
    /**
     * Contains the distance in pixels from the top of the layout box to the last line's baseline.
     *
     * * Matches [androidx.compose.ui.text.TextLayoutResult.lastBaseline] (equals [firstBaseline]
     *   for single-line measurements from [RemoteTextMeasurer.measure]).
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val lastBaseline: RemoteFloat = firstBaseline,
) {
    /**
     * Creates a [RemoteTextLayoutResult] with the given [size].
     *
     * @param size measured dimensions as a [RemoteSize]
     */
    public constructor(
        size: RemoteSize
    ) : this(
        size = size,
        monospaceWidth = size.width,
        glyphLeft = 0f.rf,
        glyphTop = 0f.rf,
        glyphRight = size.width,
        glyphBottom = size.height,
        ascent = 0f.rf,
        descent = size.height,
    )

    /**
     * Creates a [RemoteTextLayoutResult] with the given [width] and [height].
     *
     * @param width measured width as a [RemoteFloat]
     * @param height measured height as a [RemoteFloat]
     */
    public constructor(width: RemoteFloat, height: RemoteFloat) : this(RemoteSize(width, height))

    /**
     * Returns the layout advance width in pixels, including glyph side-bearings and spaces.
     *
     * * Matches [androidx.compose.ui.text.TextLayoutResult.size] width (and single-line
     *   `getLineRight(0) - getLineLeft(0)` from
     *   [androidx.compose.ui.text.TextLayoutResult.getLineRight] and
     *   [androidx.compose.ui.text.TextLayoutResult.getLineLeft]).
     */
    public val width: RemoteFloat
        get() = size.width

    /**
     * Returns the font-box layout height in pixels (`descent - ascent`).
     *
     * * Matches [androidx.compose.ui.text.TextLayoutResult.size] height (and single-line
     *   `getLineBottom(0) - getLineTop(0)` from
     *   [androidx.compose.ui.text.TextLayoutResult.getLineBottom] and
     *   [androidx.compose.ui.text.TextLayoutResult.getLineTop]) when `includeFontPadding` is
     *   `false`.
     */
    public val height: RemoteFloat
        get() = size.height

    override fun toString(): String =
        "RemoteTextLayoutResult(" +
            "size=$size, " +
            "monospaceWidth=$monospaceWidth, " +
            "glyphLeft=$glyphLeft, " +
            "glyphTop=$glyphTop, " +
            "glyphRight=$glyphRight, " +
            "glyphBottom=$glyphBottom, " +
            "ascent=$ascent, " +
            "descent=$descent, " +
            "firstBaseline=$firstBaseline, " +
            "lastBaseline=$lastBaseline" +
            ")"
}
