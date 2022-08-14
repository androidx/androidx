/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.compose.ui.text.android

import android.text.BoringLayout
import android.text.BoringLayout.Metrics
import android.text.Layout.Alignment
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextPaint
import android.text.TextUtils.TruncateAt
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat

/**
 * Factory Class for BoringLayout
 */
@OptIn(InternalPlatformTextApi::class)
internal object BoringLayoutFactory {
    /**
     * Try to layout text by BoringLayout with provided paint and text direction.
     *
     * @param text the text to analyze.
     * @param paint TextPaint which carries text style parameters such as size, weight, font e.g.
     * @param textDir text direction heuristics.
     * @return null if not boring; the width, ascent, and descent in a BoringLayout.Metrics
     * object.
     */
    @androidx.annotation.OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
    fun measure(
        text: CharSequence,
        paint: TextPaint,
        textDir: TextDirectionHeuristic
    ): Metrics? {
        return if (BuildCompat.isAtLeastT()) {
            BoringLayoutFactory33.isBoring(text, paint, textDir)
        } else {
            BoringLayoutFactoryDefault.isBoring(text, paint, textDir)
        }
    }

    /**
     * create a BoringLayout with given parameter.
     *
     * @param text The text to be layout and displayed.
     * @param paint The paint used to specify render attributes such as text size, font, e.g.
     * @param width The width occupied by the this text layout, in pixel.
     * @param metrics The font [Metrics] returned from the measurement.
     * @param alignment To which edge the text is aligned.
     * @param includePadding Whether to add extra space beyond font ascent and descent (which is
     * needed to avoid clipping in some languages, such as Arabic and Kannada). Default is true.
     * @param useFallbackLineSpacing Sets Android TextView#setFallbackLineSpacing. This value should
     * be set to true in most cases and it is the default on platform; otherwise tall scripts such
     * as Burmese or Tibetan result in clippings on top and bottom sometimes making the text
     * not-readable.
     * @param ellipsize The ellipsize option specifying how the overflowed text is handled.
     * @param ellipsizedWidth The width where the exceeding text will be ellipsized, in pixel.
     *
     * @see BoringLayout.isFallbackLineSpacingEnabled
     * @see StaticLayout.Builder.setUseLineSpacingFromFallbacks
     **/
    @androidx.annotation.OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
    fun create(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        metrics: Metrics,
        alignment: Alignment = Alignment.ALIGN_NORMAL,
        includePadding: Boolean = LayoutCompat.DEFAULT_INCLUDE_PADDING,
        useFallbackLineSpacing: Boolean = LayoutCompat.DEFAULT_FALLBACK_LINE_SPACING,
        ellipsize: TruncateAt? = null,
        ellipsizedWidth: Int = width,
    ): BoringLayout {
        require(width >= 0)
        require(ellipsizedWidth >= 0)

        return if (BuildCompat.isAtLeastT()) {
            BoringLayoutFactory33.create(
                text,
                paint,
                width,
                alignment,
                LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER,
                LayoutCompat.DEFAULT_LINESPACING_EXTRA,
                metrics,
                includePadding,
                useFallbackLineSpacing,
                ellipsize,
                ellipsizedWidth
            )
        } else {
            BoringLayoutFactoryDefault.create(
                text,
                paint,
                width,
                alignment,
                LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER,
                LayoutCompat.DEFAULT_LINESPACING_EXTRA,
                metrics,
                includePadding,
                ellipsize,
                ellipsizedWidth
            )
        }
    }

    /**
     * Returns whether fallbackLineSpacing is enabled for the given layout.
     */
    @androidx.annotation.OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
    fun isFallbackLineSpacingEnabled(layout: BoringLayout): Boolean {
        return if (BuildCompat.isAtLeastT()) {
            BoringLayoutFactory33.isFallbackLineSpacingEnabled(layout)
        } else {
            return false
        }
    }
}

@RequiresApi(33)
@OptIn(InternalPlatformTextApi::class)
private object BoringLayoutFactory33 {

    @JvmStatic
    @DoNotInline
    fun isBoring(text: CharSequence, paint: TextPaint, textDir: TextDirectionHeuristic): Metrics? {
        return BoringLayout.isBoring(
            text,
            paint,
            textDir,
            LayoutCompat.DEFAULT_FALLBACK_LINE_SPACING,
            null /* metrics */
        )
    }

    @JvmStatic
    @DoNotInline
    fun create(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        alignment: Alignment,
        lineSpacingMultiplier: Float,
        lineSpacingExtra: Float,
        metrics: Metrics,
        includePadding: Boolean,
        useFallbackLineSpacing: Boolean,
        ellipsize: TruncateAt? = null,
        ellipsizedWidth: Int = width
    ): BoringLayout {
        return BoringLayoutConstructor33.create(
            text,
            paint,
            width,
            alignment,
            lineSpacingMultiplier,
            lineSpacingExtra,
            metrics,
            includePadding,
            ellipsize,
            ellipsizedWidth,
            useFallbackLineSpacing
        )
    }

    fun isFallbackLineSpacingEnabled(layout: BoringLayout): Boolean {
        return layout.isFallbackLineSpacingEnabled
    }
}

private object BoringLayoutFactoryDefault {
    @JvmStatic
    @DoNotInline
    fun isBoring(text: CharSequence, paint: TextPaint, textDir: TextDirectionHeuristic): Metrics? {
        return if (!textDir.isRtl(text, 0, text.length)) {
            return BoringLayout.isBoring(text, paint, null /* metrics */)
        } else {
            null
        }
    }

    @JvmStatic
    @DoNotInline
    fun create(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        alignment: Alignment,
        lineSpacingMultiplier: Float,
        lineSpacingExtra: Float,
        metrics: Metrics,
        includePadding: Boolean,
        ellipsize: TruncateAt? = null,
        ellipsizedWidth: Int = width
    ): BoringLayout {
        return BoringLayout(
            text,
            paint,
            width,
            alignment,
            lineSpacingMultiplier,
            lineSpacingExtra,
            metrics,
            includePadding,
            ellipsize,
            ellipsizedWidth
        )
    }
}