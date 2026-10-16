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
import android.text.Layout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.text.vertical.HorizontalTextLayout

private val AlignLeft: Layout.Alignment =
    findLayoutAlignment("ALIGN_LEFT", Layout.Alignment.ALIGN_NORMAL)
private val AlignRight: Layout.Alignment =
    findLayoutAlignment("ALIGN_RIGHT", Layout.Alignment.ALIGN_OPPOSITE)

private fun findLayoutAlignment(name: String, fallback: Layout.Alignment): Layout.Alignment {
    for (value in Layout.Alignment.values()) {
        if (value.name == name) return value
    }
    return fallback
}

/**
 * Resolves [TextAlign] to a [Layout.Alignment].
 *
 * Maps [TextAlign.Left] and [TextAlign.Right] to the framework `ALIGN_LEFT` and `ALIGN_RIGHT` enum
 * constants via [Layout.Alignment.values] so [android.text.StaticLayout] aligns each line to the
 * left or right edge regardless of paragraph direction or [android.text.style.ReplacementSpan]
 * runs.
 */
internal fun resolveHorizontalAlignment(textAlign: TextAlign): Layout.Alignment =
    when (textAlign) {
        TextAlign.Center -> Layout.Alignment.ALIGN_CENTER
        TextAlign.End -> Layout.Alignment.ALIGN_OPPOSITE
        TextAlign.Left -> AlignLeft
        TextAlign.Right -> AlignRight
        else -> Layout.Alignment.ALIGN_NORMAL
    }

/** Caches the most recent [HorizontalTextLayout] across measure and draw passes. */
internal class HorizontalTextLayoutCache {
    private var cachedLayout: HorizontalTextLayout? = null
    private var lastText: CharSequence? = null
    private var lastMinWidth: Int = -1
    private var lastMaxWidth: Int = -1
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastStyle: TextStyle? = null
    private var lastTypeface: Typeface? = null
    private var lastDensity: Density? = null

    /**
     * Returns the cached [HorizontalTextLayout] when all inputs match, or builds a new one.
     *
     * Applies [TextStyle.color], [TextStyle.fontSize], [TextStyle.fontWeight],
     * [TextStyle.fontStyle], [TextStyle.fontSynthesis], [TextStyle.fontFamily],
     * [TextStyle.fontFeatureSettings], [TextStyle.background], [TextStyle.localeList], and
     * [TextStyle.textAlign]. [TextAlign.Justify] falls back to start alignment. Ignores other
     * [TextStyle] properties.
     *
     * @param text text to lay out, which may be an [AnnotatedString] or [android.text.Spanned]
     * @param minWidth minimum width in pixels
     * @param maxWidth maximum width in pixels, or [Constraints.Infinity] for unbounded width
     * @param layoutDirection ambient [LayoutDirection]
     * @param style [TextStyle] for measurement and alignment
     * @param typeface resolved [Typeface] for [style]
     * @param density current [Density]
     * @return measured [HorizontalTextLayout]
     */
    fun getLayout(
        text: CharSequence,
        minWidth: Int,
        maxWidth: Int,
        layoutDirection: LayoutDirection,
        style: TextStyle,
        typeface: Typeface,
        density: Density,
    ): HorizontalTextLayout {
        val cached = cachedLayout
        if (
            cached != null &&
                lastText == text &&
                lastMinWidth == minWidth &&
                lastMaxWidth == maxWidth &&
                lastLayoutDirection == layoutDirection &&
                lastStyle == style &&
                lastTypeface == typeface &&
                lastDensity == density
        ) {
            return cached
        }
        val paint = TextPaint()
        setStyleToPaint(style.toVerticalTextStyle(), typeface, density, paint)
        val layoutText = if (text is AnnotatedString) text.toSpanned(density) else text
        val textDir =
            if (layoutDirection == LayoutDirection.Rtl) {
                TextDirectionHeuristics.FIRSTSTRONG_RTL
            } else {
                TextDirectionHeuristics.FIRSTSTRONG_LTR
            }
        val alignment = resolveHorizontalAlignment(style.textAlign)
        val effectiveMaxWidth = if (maxWidth == Constraints.Infinity) Int.MAX_VALUE else maxWidth
        val newLayout =
            HorizontalTextLayout(
                text = layoutText,
                start = 0,
                end = layoutText.length,
                paint = paint,
                minWidth = minWidth,
                maxWidth = effectiveMaxWidth,
                alignment = alignment,
                textDir = textDir,
            )
        cachedLayout = newLayout
        lastText = text
        lastMinWidth = minWidth
        lastMaxWidth = maxWidth
        lastLayoutDirection = layoutDirection
        lastStyle = style
        lastTypeface = typeface
        lastDensity = density
        return newLayout
    }
}
