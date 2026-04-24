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

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.fastForEach
import androidx.text.vertical.AnnotationPosition
import androidx.text.vertical.EmphasisSpan
import androidx.text.vertical.EmphasisStyle
import androidx.text.vertical.FontShearSpan
import androidx.text.vertical.RubySpan
import androidx.text.vertical.TextOrientation
import androidx.text.vertical.TextOrientationSpan

private const val SPAN_FLAG = Spanned.SPAN_INCLUSIVE_EXCLUSIVE

/**
 * Marks the [VerticalTextScope] DSL. Prevents accidental calls against an outer receiver when
 * nesting blocks such as [VerticalTextScope.withRuby] or [VerticalTextScope.withStyle].
 */
@DslMarker public annotation class VerticalTextDsl

/**
 * Receiver for the [buildVerticalText] DSL. Provides operations for appending plain and styled
 * text, wrapping regions in ruby or orientation spans, and applying style modifications while
 * constructing a vertical-text [Spanned].
 *
 * Instances are not intended to be implemented by library consumers.
 */
@VerticalTextDsl
public interface VerticalTextScope {
    /**
     * Appends [text] and wraps it in a [TextOrientationSpan.Sideways] span: every glyph is rotated
     * 90° clockwise regardless of its script.
     */
    public fun sideways(text: CharSequence)

    /**
     * Appends [text] and wraps it in a [TextOrientationSpan.Upright] span: every glyph is drawn
     * upright regardless of its script.
     */
    public fun upright(text: CharSequence)

    /**
     * Appends [text] and wraps it in a [TextOrientationSpan.CombineUpright] span: all of the
     * appended characters are combined horizontally and laid out within a single vertical slot
     * (tate-chu-yoko).
     */
    public fun combineUpright(text: CharSequence)

    /**
     * Runs [block] and wraps the range it appends with a [RubySpan] annotating it with [withRuby],
     * with the given [position], [orientation] and [textScale].
     *
     * Ruby text is small annotation text (such as furigana) placed alongside the base text,
     * commonly used to indicate pronunciation or provide a brief description.
     *
     * @return the value returned by [block].
     */
    public fun <R : Any> withRuby(
        ruby: CharSequence,
        position: AnnotationPosition = RubySpan.DEFAULT_POSITION,
        orientation: TextOrientation = RubySpan.DEFAULT_ORIENTATION,
        textScale: Float = RubySpan.DEFAULT_TEXT_SCALE,
        block: VerticalTextScope.() -> R,
    ): R

    /**
     * Appends [text]. For each `(key, reading)` in [rubyMap], every occurrence of `key` inside the
     * just-appended [text] is annotated with a [RubySpan] carrying `reading`.
     */
    public fun text(text: CharSequence, rubyMap: Map<String, String> = emptyMap())

    /**
     * Runs [block] and wraps the range it appends with a [MetricAffectingSpan] applying the given
     * [fontSize], [textColor], and/or [backgroundColor] to the paint used for that range. If
     * [fontShear] is provided, it also wraps the range in a [FontShearSpan].
     *
     * Sp values in [fontSize] are resolved using [Density]; em values scale the inherited paint
     * size.
     *
     * @return the value returned by [block].
     */
    public fun <R : Any> withStyle(
        fontSize: TextUnit = TextUnit.Unspecified,
        textColor: Color = Color.Unspecified,
        backgroundColor: Color = Color.Unspecified,
        fontShear: Float = Float.NaN,
        block: VerticalTextScope.() -> R,
    ): R

    /**
     * Runs [block] and wraps the range it appends with an [EmphasisSpan] that renders emphasis
     * marks (傍点) alongside each character.
     *
     * @return the value returned by [block].
     */
    public fun <R : Any> withEmphasis(
        style: EmphasisStyle = EmphasisStyle.Dot,
        filled: Boolean = true,
        scale: Float = 0.5f,
        block: VerticalTextScope.() -> R,
    ): R
}

/**
 * A builder for constructing vertically-laid-out text with various typographic features.
 *
 * This builder allows applying specific vertical text spans and styles to different parts of the
 * text, such as text orientation (sideways, upright, tate-chu-yoko), ruby text (furigana), emphasis
 * marks, and font shear. It is designed to be used with [buildVerticalText].
 *
 * @property density The [Density] used to resolve sp-based [TextUnit] values inside [withStyle].
 *   Exposed so that nested [buildVerticalText] blocks can reuse the same density.
 */
internal class VerticalTextBuilder(private val density: Density) : VerticalTextScope {
    private val result: SpannableStringBuilder = SpannableStringBuilder()

    fun build(): Spanned = result

    override fun sideways(text: CharSequence): Unit =
        withSpan(TextOrientationSpan.Sideways()) { this.text(text) }

    override fun upright(text: CharSequence): Unit =
        withSpan(TextOrientationSpan.Upright()) { this.text(text) }

    override fun combineUpright(text: CharSequence): Unit =
        withSpan(TextOrientationSpan.CombineUpright()) { this.text(text) }

    override fun <R : Any> withRuby(
        ruby: CharSequence,
        position: AnnotationPosition,
        orientation: TextOrientation,
        textScale: Float,
        block: VerticalTextScope.() -> R,
    ): R = withSpan(RubySpan(ruby, position, orientation, textScale), block)

    override fun text(text: CharSequence, rubyMap: Map<String, String>) {
        val textStartOffset = result.length
        result.append(text)

        rubyMap.entries
            .sortedByDescending { it.key.length }
            .fastForEach { (key, ruby) ->
                if (key.isEmpty()) return@fastForEach
                var searchOffset = textStartOffset
                var found = result.indexOf(key, searchOffset)
                while (found != -1) {
                    result.setSpan(RubySpan(ruby), found, found + key.length, SPAN_FLAG)
                    searchOffset = found + key.length
                    found = result.indexOf(key, searchOffset)
                }
            }
    }

    private fun <R : Any> withSpan(span: Any, block: VerticalTextScope.() -> R): R {
        val index = result.length
        val r = block(this)
        result.setSpan(span, index, result.length, SPAN_FLAG)
        return r
    }

    private class TextStyleSpan(
        private val fontSize: TextUnit = TextUnit.Unspecified,
        private val textColor: Color = Color.Unspecified,
        private val backgroundColor: Color = Color.Unspecified,
        private val density: Density,
    ) : MetricAffectingSpan() {

        override fun updateMeasureState(textPaint: TextPaint) {
            if (fontSize.isSpecified) {
                if (fontSize.isSp) {
                    textPaint.textSize = with(density) { fontSize.toPx() }
                } else {
                    textPaint.textSize *= fontSize.value
                }
            }
            if (textColor.isSpecified) {
                textPaint.color = textColor.toArgb()
            }
            if (backgroundColor.isSpecified) {
                textPaint.bgColor = backgroundColor.toArgb()
            }
        }

        override fun updateDrawState(tp: TextPaint): Unit = updateMeasureState(tp)
    }

    override fun <R : Any> withStyle(
        fontSize: TextUnit,
        textColor: Color,
        backgroundColor: Color,
        fontShear: Float,
        block: VerticalTextScope.() -> R,
    ): R {
        return if (!fontShear.isNaN()) {
            // TODO(b/505944438): merge this with `TextStyleSpan`
            withSpan(FontShearSpan(fontShear)) {
                this@VerticalTextBuilder.withSpan(
                    TextStyleSpan(
                        fontSize,
                        textColor,
                        backgroundColor,
                        this@VerticalTextBuilder.density,
                    ),
                    block,
                )
            }
        } else {
            withSpan(TextStyleSpan(fontSize, textColor, backgroundColor, density), block)
        }
    }

    override fun <R : Any> withEmphasis(
        style: EmphasisStyle,
        filled: Boolean,
        scale: Float,
        block: VerticalTextScope.() -> R,
    ): R = withSpan(EmphasisSpan(style, filled, scale = scale), block)
}

/**
 * Builds a [Spanned] representing vertical text by running [builder] against a [VerticalTextScope].
 * The resulting [Spanned] is an immutable snapshot that can be passed to [VerticalText].
 *
 * @param density used to resolve sp-based [TextUnit] values in [VerticalTextScope.withStyle].
 * @param builder the DSL block describing the content.
 */
public fun buildVerticalText(density: Density, builder: VerticalTextScope.() -> Unit): Spanned =
    VerticalTextBuilder(density).apply(builder).build()

/**
 * Composable-context convenience for [buildVerticalText]. Uses [LocalDensity] to resolve sp-based
 * [TextUnit] values. For non-composable callers or nested builds, use the overload that takes an
 * explicit [Density].
 *
 * @param builder the DSL block describing the content.
 */
@Composable
public fun buildVerticalText(builder: VerticalTextScope.() -> Unit): Spanned =
    buildVerticalText(LocalDensity.current, builder)
