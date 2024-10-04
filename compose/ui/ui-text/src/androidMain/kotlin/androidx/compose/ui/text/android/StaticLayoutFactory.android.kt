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

import android.graphics.text.LineBreakConfig
import android.os.Build
import android.text.Layout.Alignment
import android.text.StaticLayout
import android.text.StaticLayout.Builder
import android.text.TextDirectionHeuristic
import android.text.TextPaint
import android.text.TextUtils.TruncateAt
import android.util.Log
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.android.LayoutCompat.BreakStrategy
import androidx.compose.ui.text.android.LayoutCompat.HyphenationFrequency
import androidx.compose.ui.text.android.LayoutCompat.JustificationMode
import androidx.compose.ui.text.android.LayoutCompat.LineBreakStyle
import androidx.compose.ui.text.android.LayoutCompat.LineBreakWordStyle
import androidx.compose.ui.text.internal.requirePrecondition
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

private const val TAG = "StaticLayoutFactory"

@InternalPlatformTextApi
object StaticLayoutFactory {

    private val delegate: StaticLayoutFactoryImpl =
        if (Build.VERSION.SDK_INT >= 23) {
            StaticLayoutFactory23()
        } else {
            StaticLayoutFactoryDefault()
        }

    /** Builder class for StaticLayout. */
    fun create(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        start: Int = 0,
        end: Int = text.length,
        textDir: TextDirectionHeuristic = LayoutCompat.DEFAULT_TEXT_DIRECTION_HEURISTIC,
        alignment: Alignment = LayoutCompat.DEFAULT_LAYOUT_ALIGNMENT,
        @IntRange(from = 0) maxLines: Int = LayoutCompat.DEFAULT_MAX_LINES,
        ellipsize: TruncateAt? = null,
        @IntRange(from = 0) ellipsizedWidth: Int = width,
        @FloatRange(from = 0.0)
        lineSpacingMultiplier: Float = LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER,
        lineSpacingExtra: Float = LayoutCompat.DEFAULT_LINESPACING_EXTRA,
        @JustificationMode justificationMode: Int = LayoutCompat.DEFAULT_JUSTIFICATION_MODE,
        includePadding: Boolean = LayoutCompat.DEFAULT_INCLUDE_PADDING,
        useFallbackLineSpacing: Boolean = LayoutCompat.DEFAULT_FALLBACK_LINE_SPACING,
        @BreakStrategy breakStrategy: Int = LayoutCompat.DEFAULT_BREAK_STRATEGY,
        @LineBreakStyle lineBreakStyle: Int = LayoutCompat.DEFAULT_LINE_BREAK_STYLE,
        @LineBreakWordStyle lineBreakWordStyle: Int = LayoutCompat.DEFAULT_LINE_BREAK_WORD_STYLE,
        @HyphenationFrequency
        hyphenationFrequency: Int = LayoutCompat.DEFAULT_HYPHENATION_FREQUENCY,
        leftIndents: IntArray? = null,
        rightIndents: IntArray? = null
    ): StaticLayout {
        return delegate.create(
            StaticLayoutParams(
                text = text,
                start = start,
                end = end,
                paint = paint,
                width = width,
                textDir = textDir,
                alignment = alignment,
                maxLines = maxLines,
                ellipsize = ellipsize,
                ellipsizedWidth = ellipsizedWidth,
                lineSpacingMultiplier = lineSpacingMultiplier,
                lineSpacingExtra = lineSpacingExtra,
                justificationMode = justificationMode,
                includePadding = includePadding,
                useFallbackLineSpacing = useFallbackLineSpacing,
                breakStrategy = breakStrategy,
                lineBreakStyle = lineBreakStyle,
                lineBreakWordStyle = lineBreakWordStyle,
                hyphenationFrequency = hyphenationFrequency,
                leftIndents = leftIndents,
                rightIndents = rightIndents
            )
        )
    }

    /**
     * Returns whether fallbackLineSpacing is enabled for the given layout.
     *
     * @param layout StaticLayout instance
     * @param useFallbackLineSpacing fallbackLineSpacing configuration passed while creating the
     *   StaticLayout.
     */
    fun isFallbackLineSpacingEnabled(
        layout: StaticLayout,
        useFallbackLineSpacing: Boolean
    ): Boolean {
        return delegate.isFallbackLineSpacingEnabled(layout, useFallbackLineSpacing)
    }
}

private class StaticLayoutParams(
    val text: CharSequence,
    val start: Int = 0,
    val end: Int,
    val paint: TextPaint,
    val width: Int,
    val textDir: TextDirectionHeuristic,
    val alignment: Alignment,
    val maxLines: Int,
    val ellipsize: TruncateAt?,
    val ellipsizedWidth: Int,
    val lineSpacingMultiplier: Float,
    val lineSpacingExtra: Float,
    val justificationMode: Int,
    val includePadding: Boolean,
    val useFallbackLineSpacing: Boolean,
    val breakStrategy: Int,
    val lineBreakStyle: Int,
    val lineBreakWordStyle: Int,
    val hyphenationFrequency: Int,
    val leftIndents: IntArray?,
    val rightIndents: IntArray?
) {
    init {
        requirePrecondition(start in 0..end) { "invalid start value" }
        requirePrecondition(end in 0..text.length) { "invalid end value" }
        requirePrecondition(maxLines >= 0) { "invalid maxLines value" }
        requirePrecondition(width >= 0) { "invalid width value" }
        requirePrecondition(ellipsizedWidth >= 0) { "invalid ellipsizedWidth value" }
        requirePrecondition(lineSpacingMultiplier >= 0f) { "invalid lineSpacingMultiplier value" }
    }
}

private interface StaticLayoutFactoryImpl {

    // API level specific, do not inline to prevent ART class verification breakages
    fun create(params: StaticLayoutParams): StaticLayout

    fun isFallbackLineSpacingEnabled(layout: StaticLayout, useFallbackLineSpacing: Boolean): Boolean
}

@RequiresApi(23)
private class StaticLayoutFactory23 : StaticLayoutFactoryImpl {

    override fun create(params: StaticLayoutParams): StaticLayout {
        return Builder.obtain(params.text, params.start, params.end, params.paint, params.width)
            .apply {
                setTextDirection(params.textDir)
                setAlignment(params.alignment)
                setMaxLines(params.maxLines)
                setEllipsize(params.ellipsize)
                setEllipsizedWidth(params.ellipsizedWidth)
                setLineSpacing(params.lineSpacingExtra, params.lineSpacingMultiplier)
                setIncludePad(params.includePadding)
                setBreakStrategy(params.breakStrategy)
                setHyphenationFrequency(params.hyphenationFrequency)
                setIndents(params.leftIndents, params.rightIndents)
                if (Build.VERSION.SDK_INT >= 26) {
                    StaticLayoutFactory26.setJustificationMode(this, params.justificationMode)
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    StaticLayoutFactory28.setUseLineSpacingFromFallbacks(
                        this,
                        params.useFallbackLineSpacing
                    )
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    StaticLayoutFactory33.setLineBreakConfig(
                        this,
                        params.lineBreakStyle,
                        params.lineBreakWordStyle
                    )
                }
            }
            .build()
    }

    override fun isFallbackLineSpacingEnabled(
        layout: StaticLayout,
        useFallbackLineSpacing: Boolean
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            StaticLayoutFactory33.isFallbackLineSpacingEnabled(layout)
        } else if (Build.VERSION.SDK_INT >= 28) {
            useFallbackLineSpacing
        } else {
            false
        }
    }
}

@RequiresApi(26)
private object StaticLayoutFactory26 {
    @JvmStatic
    fun setJustificationMode(builder: Builder, justificationMode: Int) {
        builder.setJustificationMode(justificationMode)
    }
}

@RequiresApi(28)
private object StaticLayoutFactory28 {
    @JvmStatic
    fun setUseLineSpacingFromFallbacks(builder: Builder, useFallbackLineSpacing: Boolean) {
        builder.setUseLineSpacingFromFallbacks(useFallbackLineSpacing)
    }
}

@RequiresApi(33)
private object StaticLayoutFactory33 {
    @JvmStatic
    fun isFallbackLineSpacingEnabled(layout: StaticLayout): Boolean {
        return layout.isFallbackLineSpacingEnabled
    }

    @JvmStatic
    fun setLineBreakConfig(builder: Builder, lineBreakStyle: Int, lineBreakWordStyle: Int) {
        val lineBreakConfig =
            LineBreakConfig.Builder()
                .setLineBreakStyle(lineBreakStyle)
                .setLineBreakWordStyle(lineBreakWordStyle)
                .build()
        builder.setLineBreakConfig(lineBreakConfig)
    }
}

private class StaticLayoutFactoryDefault : StaticLayoutFactoryImpl {

    companion object {
        private var isInitialized = false
        private var staticLayoutConstructor: Constructor<StaticLayout>? = null

        private fun getStaticLayoutConstructor(): Constructor<StaticLayout>? {
            if (isInitialized) return staticLayoutConstructor
            isInitialized = true
            try {
                staticLayoutConstructor =
                    StaticLayout::class
                        .java
                        .getConstructor(
                            CharSequence::class.java,
                            Int::class.javaPrimitiveType, /* start */
                            Int::class.javaPrimitiveType, /* end */
                            TextPaint::class.java,
                            Int::class.javaPrimitiveType, /* width */
                            Alignment::class.java,
                            TextDirectionHeuristic::class.java,
                            Float::class.javaPrimitiveType, /* lineSpacingMultiplier */
                            Float::class.javaPrimitiveType, /* lineSpacingExtra */
                            Boolean::class.javaPrimitiveType, /* includePadding */
                            TruncateAt::class.java,
                            Int::class.javaPrimitiveType, /* ellipsizeWidth */
                            Int::class.javaPrimitiveType /* maxLines */
                        )
            } catch (e: NoSuchMethodException) {
                staticLayoutConstructor = null
                Log.e(TAG, "unable to collect necessary constructor.")
            }

            return staticLayoutConstructor
        }
    }

    override fun create(params: StaticLayoutParams): StaticLayout {
        // On API 21 to 23, try to call the StaticLayoutConstructor which supports the
        // textDir and maxLines.
        val result =
            getStaticLayoutConstructor()?.let {
                try {
                    it.newInstance(
                        params.text,
                        params.start,
                        params.end,
                        params.paint,
                        params.width,
                        params.alignment,
                        params.textDir,
                        params.lineSpacingMultiplier,
                        params.lineSpacingExtra,
                        params.includePadding,
                        params.ellipsize,
                        params.ellipsizedWidth,
                        params.maxLines
                    )
                } catch (e: IllegalAccessException) {
                    staticLayoutConstructor = null
                    Log.e(TAG, "unable to call constructor")
                    null
                } catch (e: InstantiationException) {
                    staticLayoutConstructor = null
                    Log.e(TAG, "unable to call constructor")
                    null
                } catch (e: InvocationTargetException) {
                    staticLayoutConstructor = null
                    Log.e(TAG, "unable to call constructor")
                    null
                }
            }

        if (result != null) return result

        // On API 21 to 23 where it failed to find StaticLayout.Builder, create with
        // deprecated constructor, textDir and maxLines won't work in this case.
        @Suppress("DEPRECATION")
        return StaticLayout(
            params.text,
            params.start,
            params.end,
            params.paint,
            params.width,
            params.alignment,
            params.lineSpacingMultiplier,
            params.lineSpacingExtra,
            params.includePadding,
            params.ellipsize,
            params.ellipsizedWidth
        )
    }

    override fun isFallbackLineSpacingEnabled(
        layout: StaticLayout,
        useFallbackLineSpacing: Boolean
    ): Boolean {
        return false
    }
}
