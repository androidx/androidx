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
import androidx.compose.remote.creation.compose.state.RemoteFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteStateInstanceKey
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
 * Creates and remembers a [RemoteTextMeasurer].
 *
 * Parameters are read from CompositionLocals (such as [LocalRemoteDensity]).
 */
@Composable
public fun rememberRemoteTextMeasurer(): RemoteTextMeasurer {
    val density = LocalRemoteDensity.current
    return remember(density) { RemoteTextMeasurer(density = density) }
}

/**
 * Measures single-line remote text dimensions.
 *
 * Follows the [androidx.compose.ui.text.TextMeasurer] pattern from Compose UI.
 *
 * @param density density of the measurement environment, used for scaling fonts
 */
@Immutable
public class RemoteTextMeasurer(internal val density: RemoteDensity) {
    /**
     * Measures [text] as a single unconstrained line and returns a [RemoteTextLayoutResult].
     *
     * If [fontSize] is null, uses [RemoteTextStyle.fontSize] from [style], or `12.rsp` if
     * unspecified.
     *
     * @param text remote string to measure
     * @param style [RemoteTextStyle] to apply, defaulting to [RemoteTextStyle.Default]
     * @param fontSize optional font size override; if null, taken from [style] or defaults to
     *   `12.rsp`
     */
    @Stable
    public fun measure(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteTextLayoutResult {
        val width = measureWidth(text, style, fontSize)
        val height = measureHeight(text, style, fontSize)
        val advanceWidth = measureAdvanceWidth(text, style, fontSize)
        val monospaceWidth = measureMonospaceWidth(text, style, fontSize)
        val left = measureLeft(text, style, fontSize)
        val top = measureTop(text, style, fontSize)
        val right = measureRight(text, style, fontSize)
        val bottom = measureBottom(text, style, fontSize)
        val ascent = measureAscent(text, style, fontSize)
        val descent = measureDescent(text, style, fontSize)
        return RemoteTextLayoutResult(
            size = RemoteSize(width, height),
            advanceWidth = advanceWidth,
            monospaceWidth = monospaceWidth,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            ascent = ascent,
            descent = descent,
        )
    }

    /**
     * Measures the tight glyph bounds width of [text].
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
    internal fun measureWidth(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat = measureAttribute(text, style, fontSize, TextAttribute.MEASURE_WIDTH)

    /**
     * Measures the advance width of [text], including glyph side-bearings and leading/trailing
     * spaces.
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
    internal fun measureAdvanceWidth(
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

    /**
     * Measures the monospace-adjusted width of [text] based on fixed-width character slots (where
     * each character slot is calculated using the width of '0'), useful for vertical alignment and
     * tabular data.
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
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

    /**
     * Measures the height of [text].
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
    internal fun measureHeight(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat = measureAttribute(text, style, fontSize, TextAttribute.MEASURE_HEIGHT)

    /**
     * Measures the left bound of [text] relative to its origin.
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
    internal fun measureLeft(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat = measureAttribute(text, style, fontSize, TextAttribute.MEASURE_LEFT)

    /**
     * Measures the top bound of [text] relative to its baseline.
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
    internal fun measureTop(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat = measureAttribute(text, style, fontSize, TextAttribute.MEASURE_TOP)

    /**
     * Measures the right bound of [text] relative to its origin.
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
    internal fun measureRight(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat = measureAttribute(text, style, fontSize, TextAttribute.MEASURE_RIGHT)

    /**
     * Measures the bottom bound of [text] relative to its baseline.
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
    internal fun measureBottom(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat = measureAttribute(text, style, fontSize, TextAttribute.MEASURE_BOTTOM)

    /**
     * Measures the font ascent of [text] relative to its baseline.
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
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

    /**
     * Measures the font descent of [text] relative to its baseline.
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
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

        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteStateInstanceKey(),
        ) { creationState ->
            val doc = creationState.document
            val textSizePxId = textSize.getFloatIdForCreationState(creationState)
            doc.rcPaint
                .setTextSize(textSizePxId)
                .setTypeface(
                    0,
                    (style.remoteFontWeight ?: RemoteFontWeight.Normal)
                        .weight
                        .constantValueOrNull
                        ?.toInt() ?: 400,
                    style.remoteFontStyle == RemoteFontStyle.Italic,
                )
                .commit()

            floatArrayOf(
                doc.textAttribute(
                    text.getIdForCreationState(creationState),
                    attributeType,
                )
            )
        }
    }
}

/**
 * Holds the result of a remote text layout measurement.
 *
 * Follows the [androidx.compose.ui.text.TextLayoutResult] pattern from Compose UI.
 *
 * @property size Tight glyph dimensions ([RemoteSize]) of the measured text.
 */
@Immutable
public class RemoteTextLayoutResult
/**
 * Creates a [RemoteTextLayoutResult] with measured glyph bounds and font metrics.
 *
 * @param size tight glyph dimensions ([RemoteSize]) of the measured text
 * @param advanceWidth advance width of the measured text including glyph side-bearings and
 *   leading/trailing spaces
 * @param monospaceWidth monospace-adjusted width of the measured text
 * @param left tight left glyph bound of the measured text relative to its origin
 * @param top tight top glyph bound of the measured text relative to its baseline (negative above
 *   baseline)
 * @param right tight right glyph bound of the measured text relative to its origin
 * @param bottom tight bottom glyph bound of the measured text relative to its baseline (positive
 *   below baseline)
 * @param ascent font ascent of the measured text relative to its baseline (negative above baseline)
 * @param descent font descent of the measured text relative to its baseline (positive below
 *   baseline)
 * @param firstBaseline distance from the top of the font-height layout box to the first line's
 *   alphabetic baseline (`-ascent` for single-line measurements from [RemoteTextMeasurer.measure])
 * @param lastBaseline distance from the top of the font-height layout box to the last line's
 *   alphabetic baseline (equals [firstBaseline] for single-line measurements from
 *   [RemoteTextMeasurer.measure])
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    public val size: RemoteSize,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val advanceWidth: RemoteFloat,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val monospaceWidth: RemoteFloat,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val left: RemoteFloat,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val top: RemoteFloat,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val right: RemoteFloat,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val bottom: RemoteFloat,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val ascent: RemoteFloat,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val descent: RemoteFloat,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val firstBaseline: RemoteFloat = 0f.rf - ascent,
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
        advanceWidth = size.width,
        monospaceWidth = size.width,
        left = 0f.rf,
        top = 0f.rf,
        right = size.width,
        bottom = size.height,
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

    /** The measured width as a [RemoteFloat]. */
    public val width: RemoteFloat
        get() = size.width

    /** The measured height as a [RemoteFloat]. */
    public val height: RemoteFloat
        get() = size.height

    override fun toString(): String =
        "RemoteTextLayoutResult(" +
            "size=$size, " +
            "advanceWidth=$advanceWidth, " +
            "monospaceWidth=$monospaceWidth, " +
            "left=$left, " +
            "top=$top, " +
            "right=$right, " +
            "bottom=$bottom, " +
            "ascent=$ascent, " +
            "descent=$descent, " +
            "firstBaseline=$firstBaseline, " +
            "lastBaseline=$lastBaseline" +
            ")"
}
