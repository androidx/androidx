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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.util.equalsIncludingNaN

/**
 * Configures layout- and draw-affecting text attributes for `BasicText`.
 *
 * Use this scope in the `styleProvider` lambda parameter of `BasicText`. State reads performed
 * within this scope are observed during the layout phase, skipping recomposition when invalidated
 * and re-evaluating during measurement (recalculating text layout only when layout-affecting
 * attributes change).
 */
public sealed interface TextStyleScope {
    /**
     * Sets all text attributes from [style].
     *
     * Overwrites all attributes in this scope with the values from [style], resetting attributes
     * that are unspecified or `null` in [style]. Call individual setter methods after
     * [setTextStyle] to override specific attributes.
     *
     * @param style text style to copy attributes from
     */
    public fun setTextStyle(style: TextStyle)

    /**
     * Sets the color to apply to the text.
     *
     * If [color] is specified (`color.isSpecified`), this clears any brush and alpha previously set
     * via [setBrush], [setAlpha], or [setTextStyle]. If [color] is [Color.Unspecified], any
     * existing brush and alpha are preserved.
     */
    public fun setColor(color: Color)

    /**
     * Sets the [Brush] for painting the text, clearing any color previously set via [setColor] or
     * [setTextStyle] (resetting it to [Color.Unspecified]).
     */
    public fun setBrush(brush: Brush)

    /**
     * Sets the opacity of the text (0.0 to 1.0) applied when a brush is configured (via [setBrush]
     * or [setTextStyle]). Cleared when a specified color is set via [setColor] or when
     * [setTextStyle] sets a style without a brush.
     */
    public fun setAlpha(alpha: Float)

    /** Sets the size of glyphs to use when painting the text (e.g., `16.sp`). */
    public fun setFontSize(fontSize: TextUnit)

    /** Sets the typeface thickness to use when painting the text (e.g., [FontWeight.Bold]). */
    public fun setFontWeight(fontWeight: FontWeight)

    /** Sets the typeface variant to use when drawing letters (e.g., [FontStyle.Italic]). */
    public fun setFontStyle(fontStyle: FontStyle)

    /**
     * Sets the font synthesis rules determining if and how bold/italic styles are synthesized when
     * the font family does not natively support them.
     */
    public fun setFontSynthesis(fontSynthesis: FontSynthesis)

    /** Sets the font family used when rendering text. */
    public fun setFontFamily(fontFamily: FontFamily)

    /** Sets advanced typography settings in CSS format (e.g., `"smcp"` for small caps). */
    public fun setFontFeatureSettings(fontFeatureSettings: String)

    /** Sets the amount of space to add between letters (in SP or EM). */
    public fun setLetterSpacing(letterSpacing: TextUnit)

    /**
     * Sets the vertical shift amount from the baseline (e.g., [BaselineShift.Superscript]). If
     * [BaselineShift.Unspecified], inherits from parent or default style.
     */
    public fun setBaselineShift(baselineShift: BaselineShift)

    /** Sets the geometric transformation to apply to the text. */
    public fun setTextGeometricTransform(textGeometricTransform: TextGeometricTransform)

    /** Sets the locale list used to select region-specific glyphs. */
    public fun setLocaleList(localeList: LocaleList)

    /** Sets the background color covering the entire line height from start to end. */
    public fun setBackground(background: Color)

    /** Sets the decorations to paint on the text (e.g., [TextDecoration.Underline]). */
    public fun setTextDecoration(textDecoration: TextDecoration)

    /** Sets the shadow effect applied to the text. */
    public fun setShadow(shadow: Shadow)

    /** Sets the drawing style of the text (fill or stroke). */
    public fun setDrawStyle(drawStyle: DrawStyle)

    /** Sets the alignment of the text within lines of the paragraph. */
    public fun setTextAlign(textAlign: TextAlign)

    /** Sets the algorithm used to resolve text direction. */
    public fun setTextDirection(textDirection: TextDirection)

    /** Sets the line height for the paragraph (e.g., `24.sp`). */
    public fun setLineHeight(lineHeight: TextUnit)

    /** Sets the paragraph indentation. */
    public fun setTextIndent(textIndent: TextIndent)

    /** Sets platform-specific text styling parameters. */
    public fun setPlatformStyle(platformStyle: PlatformTextStyle)

    /** Sets the line height distribution configuration. */
    public fun setLineHeightStyle(lineHeightStyle: LineHeightStyle)

    /** Sets the line breaking configuration of the paragraph. */
    public fun setLineBreak(lineBreak: LineBreak)

    /** Sets the hyphenation configuration of the paragraph. */
    public fun setHyphens(hyphens: Hyphens)

    /** Sets the text character placement optimization for animated or static text. */
    public fun setTextMotion(textMotion: TextMotion)
}

private interface NonExhaustiveTextStyleScope : TextStyleScope

private val BoxedFontStyleNormal: FontStyle? = FontStyle.Normal
private val BoxedFontStyleItalic: FontStyle? = FontStyle.Italic
private val BoxedFontSynthesisNone: FontSynthesis? = FontSynthesis.None
private val BoxedFontSynthesisWeight: FontSynthesis? = FontSynthesis.Weight
private val BoxedFontSynthesisStyle: FontSynthesis? = FontSynthesis.Style
private val BoxedFontSynthesisAll: FontSynthesis? = FontSynthesis.All

/**
 * Mutable [TextStyleScope] implementation reused across layout and draw passes to avoid
 * allocations.
 */
public class ReusableTextStyleScope : TextStyleScope {
    /** Color to apply to the text, or [Color.Unspecified] if not set. */
    public var color: Color = Color.Unspecified
        private set

    /** [Brush] for painting the text, or `null` if not set. */
    public var brush: Brush? = null
        private set

    /**
     * Opacity of the text (0.0 to 1.0) applied when [brush] is provided, or `Float.NaN` if not set.
     */
    public var alpha: Float = Float.NaN
        private set

    /** Size of glyphs to use when painting the text, or [TextUnit.Unspecified] if not set. */
    public var fontSize: TextUnit = TextUnit.Unspecified
        private set

    /** Typeface thickness to use when painting the text, or `null` if not set. */
    public var fontWeight: FontWeight? = null
        private set

    private var _fontStyle: FontStyle? = null

    /** Typeface variant to use when drawing letters, or `null` if not set. */
    public val fontStyle: FontStyle?
        get() = _fontStyle

    private var _fontSynthesis: FontSynthesis? = null

    /**
     * Font synthesis rules determining if and how bold/italic styles are synthesized when the font
     * family does not natively support them, or `null` if not set.
     */
    public val fontSynthesis: FontSynthesis?
        get() = _fontSynthesis

    /** Font family used when rendering text, or `null` if not set. */
    public var fontFamily: FontFamily? = null
        private set

    /** Advanced typography settings in CSS format, or `null` if not set. */
    public var fontFeatureSettings: String? = null
        private set

    /** Amount of space to add between letters, or [TextUnit.Unspecified] if not set. */
    public var letterSpacing: TextUnit = TextUnit.Unspecified
        private set

    /** Vertical shift amount from the baseline, or [BaselineShift.Unspecified] if not set. */
    public var baselineShift: BaselineShift = BaselineShift.Unspecified
        private set

    /** Geometric transformation to apply to the text, or `null` if not set. */
    public var textGeometricTransform: TextGeometricTransform? = null
        private set

    /** Locale list used to select region-specific glyphs, or `null` if not set. */
    @get:Suppress("NullableCollection")
    public var localeList: LocaleList? = null
        private set

    /**
     * Background color covering the entire line height from start to end, or [Color.Unspecified] if
     * not set.
     */
    public var background: Color = Color.Unspecified
        private set

    /** Decorations to paint on the text, or `null` if not set. */
    public var textDecoration: TextDecoration? = null
        private set

    /** Shadow effect applied to the text, or `null` if not set. */
    public var shadow: Shadow? = null
        private set

    /** Drawing style of the text (fill or stroke), or `null` if not set. */
    public var drawStyle: DrawStyle? = null
        private set

    private var _textAlign: TextAlign = TextAlign.Unspecified

    /**
     * Alignment of the text within lines of the paragraph, or [TextAlign.Unspecified] if not set.
     */
    public val textAlign: TextAlign
        get() = _textAlign

    private var _textDirection: TextDirection = TextDirection.Unspecified

    /** Algorithm used to resolve text direction, or [TextDirection.Unspecified] if not set. */
    public val textDirection: TextDirection
        get() = _textDirection

    /** Line height for the paragraph, or [TextUnit.Unspecified] if not set. */
    public var lineHeight: TextUnit = TextUnit.Unspecified
        private set

    /** Paragraph indentation, or `null` if not set. */
    public var textIndent: TextIndent? = null
        private set

    /** Platform-specific text styling parameters, or `null` if not set. */
    public var platformStyle: PlatformTextStyle? = null
        private set

    /** Line height distribution configuration, or `null` if not set. */
    public var lineHeightStyle: LineHeightStyle? = null
        private set

    private var _lineBreak: LineBreak = LineBreak.Unspecified

    /** Line breaking configuration of the paragraph, or [LineBreak.Unspecified] if not set. */
    public val lineBreak: LineBreak
        get() = _lineBreak

    private var _hyphens: Hyphens = Hyphens.Unspecified

    /** Hyphenation configuration of the paragraph, or [Hyphens.Unspecified] if not set. */
    public val hyphens: Hyphens
        get() = _hyphens

    /** Text character placement optimization for animated or static text, or `null` if not set. */
    public var textMotion: TextMotion? = null
        private set

    override fun setTextStyle(style: TextStyle) {
        val spanStyle = style.spanStyle
        val paragraphStyle = style.paragraphStyle
        fontSize = spanStyle.fontSize
        fontWeight = spanStyle.fontWeight
        _fontStyle = spanStyle.fontStyle
        _fontSynthesis = spanStyle.fontSynthesis
        fontFamily = spanStyle.fontFamily
        fontFeatureSettings = spanStyle.fontFeatureSettings
        letterSpacing = spanStyle.letterSpacing
        baselineShift = spanStyle.baselineShift ?: BaselineShift.Unspecified
        textGeometricTransform = spanStyle.textGeometricTransform
        localeList = spanStyle.localeList
        background = spanStyle.background
        textDecoration = spanStyle.textDecoration
        shadow = spanStyle.shadow
        drawStyle = spanStyle.drawStyle
        val spanBrush = spanStyle.brush
        if (spanBrush != null) {
            brush = spanBrush
            alpha = spanStyle.alpha
            color = Color.Unspecified
        } else {
            brush = null
            alpha = Float.NaN
            color = spanStyle.color
        }
        _textAlign = paragraphStyle.textAlign
        _textDirection = paragraphStyle.textDirection
        lineHeight = paragraphStyle.lineHeight
        textIndent = paragraphStyle.textIndent
        platformStyle = style.platformStyle
        lineHeightStyle = paragraphStyle.lineHeightStyle
        _lineBreak = paragraphStyle.lineBreak
        _hyphens = paragraphStyle.hyphens
        textMotion = paragraphStyle.textMotion
    }

    override fun setColor(color: Color) {
        this.color = color
        if (color.isSpecified) {
            this.brush = null
            this.alpha = Float.NaN
        }
    }

    override fun setBrush(brush: Brush) {
        this.brush = brush
        this.color = Color.Unspecified
    }

    override fun setAlpha(alpha: Float) {
        this.alpha = alpha
    }

    override fun setFontSize(fontSize: TextUnit) {
        this.fontSize = fontSize
    }

    override fun setFontWeight(fontWeight: FontWeight) {
        this.fontWeight = fontWeight
    }

    override fun setFontStyle(fontStyle: FontStyle) {
        this._fontStyle =
            when (fontStyle) {
                FontStyle.Normal -> BoxedFontStyleNormal
                FontStyle.Italic -> BoxedFontStyleItalic
                else -> fontStyle
            }
    }

    override fun setFontSynthesis(fontSynthesis: FontSynthesis) {
        this._fontSynthesis =
            when (fontSynthesis) {
                FontSynthesis.None -> BoxedFontSynthesisNone
                FontSynthesis.Weight -> BoxedFontSynthesisWeight
                FontSynthesis.Style -> BoxedFontSynthesisStyle
                FontSynthesis.All -> BoxedFontSynthesisAll
                else -> fontSynthesis
            }
    }

    override fun setFontFamily(fontFamily: FontFamily) {
        this.fontFamily = fontFamily
    }

    override fun setFontFeatureSettings(fontFeatureSettings: String) {
        this.fontFeatureSettings = fontFeatureSettings
    }

    override fun setLetterSpacing(letterSpacing: TextUnit) {
        this.letterSpacing = letterSpacing
    }

    override fun setBaselineShift(baselineShift: BaselineShift) {
        this.baselineShift = baselineShift
    }

    override fun setTextGeometricTransform(textGeometricTransform: TextGeometricTransform) {
        this.textGeometricTransform = textGeometricTransform
    }

    override fun setLocaleList(localeList: LocaleList) {
        this.localeList = localeList
    }

    override fun setBackground(background: Color) {
        this.background = background
    }

    override fun setTextDecoration(textDecoration: TextDecoration) {
        this.textDecoration = textDecoration
    }

    override fun setShadow(shadow: Shadow) {
        this.shadow = shadow
    }

    override fun setDrawStyle(drawStyle: DrawStyle) {
        this.drawStyle = drawStyle
    }

    override fun setTextAlign(textAlign: TextAlign) {
        this._textAlign = textAlign
    }

    override fun setTextDirection(textDirection: TextDirection) {
        this._textDirection = textDirection
    }

    override fun setLineHeight(lineHeight: TextUnit) {
        this.lineHeight = lineHeight
    }

    override fun setTextIndent(textIndent: TextIndent) {
        this.textIndent = textIndent
    }

    override fun setPlatformStyle(platformStyle: PlatformTextStyle) {
        this.platformStyle = platformStyle
    }

    override fun setLineHeightStyle(lineHeightStyle: LineHeightStyle) {
        this.lineHeightStyle = lineHeightStyle
    }

    override fun setLineBreak(lineBreak: LineBreak) {
        this._lineBreak = lineBreak
    }

    override fun setHyphens(hyphens: Hyphens) {
        this._hyphens = hyphens
    }

    override fun setTextMotion(textMotion: TextMotion) {
        this.textMotion = textMotion
    }

    /** Resets all text attributes to their initial unspecified values. */
    public fun reset() {
        color = Color.Unspecified
        brush = null
        alpha = Float.NaN
        fontSize = TextUnit.Unspecified
        fontWeight = null
        _fontStyle = null
        _fontSynthesis = null
        fontFamily = null
        fontFeatureSettings = null
        letterSpacing = TextUnit.Unspecified
        baselineShift = BaselineShift.Unspecified
        textGeometricTransform = null
        localeList = null
        background = Color.Unspecified
        textDecoration = null
        shadow = null
        drawStyle = null
        _textAlign = TextAlign.Unspecified
        _textDirection = TextDirection.Unspecified
        lineHeight = TextUnit.Unspecified
        textIndent = null
        platformStyle = null
        lineHeightStyle = null
        _lineBreak = LineBreak.Unspecified
        _hyphens = Hyphens.Unspecified
        textMotion = null
    }

    /**
     * Returns whether this scope and [other] have equivalent layout-affecting attributes.
     *
     * @param other style scope to compare against
     * @return `true` if all layout-affecting attributes match
     */
    public fun hasSameLayoutAffectingAttributes(other: ReusableTextStyleScope): Boolean {
        if (this === other) return true
        if (fontSize != other.fontSize) return false
        if (fontWeight != other.fontWeight) return false
        if (_fontStyle != other._fontStyle) return false
        if (_fontSynthesis != other._fontSynthesis) return false
        if (fontFamily != other.fontFamily) return false
        if (fontFeatureSettings != other.fontFeatureSettings) return false
        if (letterSpacing != other.letterSpacing) return false
        if (baselineShift != other.baselineShift) return false
        if (textGeometricTransform != other.textGeometricTransform) return false
        if (localeList != other.localeList) return false
        if (background != other.background) return false
        if (platformStyle != other.platformStyle) return false
        if (_textAlign != other._textAlign) return false
        if (_textDirection != other._textDirection) return false
        if (lineHeight != other.lineHeight) return false
        if (textIndent != other.textIndent) return false
        if (lineHeightStyle != other.lineHeightStyle) return false
        if (_lineBreak != other._lineBreak) return false
        if (_hyphens != other._hyphens) return false
        if (textMotion != other.textMotion) return false
        return true
    }

    /**
     * Returns whether this scope and [style] have equivalent layout-affecting attributes.
     *
     * @param style text style to compare against
     * @return `true` if all layout-affecting attributes match
     */
    public fun hasSameLayoutAffectingAttributes(style: TextStyle): Boolean {
        if (fontSize != style.fontSize) return false
        if (fontWeight != style.fontWeight) return false
        if (_fontStyle != style.fontStyle) return false
        if (_fontSynthesis != style.fontSynthesis) return false
        if (fontFamily != style.fontFamily) return false
        if (fontFeatureSettings != style.fontFeatureSettings) return false
        if (letterSpacing != style.letterSpacing) return false
        if (baselineShift != (style.baselineShift ?: BaselineShift.Unspecified)) return false
        if (textGeometricTransform != style.textGeometricTransform) return false
        if (localeList != style.localeList) return false
        if (background != style.background) return false
        if (platformStyle != style.platformStyle) return false
        if (_textAlign != style.textAlign) return false
        if (_textDirection != style.textDirection) return false
        if (lineHeight != style.lineHeight) return false
        if (textIndent != style.textIndent) return false
        if (lineHeightStyle != style.lineHeightStyle) return false
        if (_lineBreak != style.lineBreak) return false
        if (_hyphens != style.hyphens) return false
        if (textMotion != style.textMotion) return false
        return true
    }

    /**
     * Returns whether this scope and [other] have equivalent draw-affecting attributes.
     *
     * @param other style scope to compare against
     * @return `true` if all draw-affecting attributes match
     */
    public fun hasSameDrawAffectingAttributes(other: ReusableTextStyleScope): Boolean {
        if (this === other) return true
        if (color != other.color) return false
        if (brush != other.brush) return false
        if (!alpha.equalsIncludingNaN(other.alpha)) return false
        if (shadow != other.shadow) return false
        if (textDecoration != other.textDecoration) return false
        if (drawStyle != other.drawStyle) return false
        return true
    }

    /**
     * Returns whether this scope and [style] have equivalent draw-affecting attributes.
     *
     * @param style text style to compare against
     * @return `true` if all draw-affecting attributes match
     */
    public fun hasSameDrawAffectingAttributes(style: TextStyle): Boolean {
        val spanStyle = style.spanStyle
        val spanBrush = spanStyle.brush
        if (spanBrush != null) {
            if (color != Color.Unspecified) return false
            if (brush != spanBrush) return false
            if (!alpha.equalsIncludingNaN(spanStyle.alpha)) return false
        } else {
            if (color != spanStyle.color) return false
            if (brush != null) return false
            if (!alpha.isNaN()) return false
        }
        if (shadow != spanStyle.shadow) return false
        if (textDecoration != spanStyle.textDecoration) return false
        if (drawStyle != spanStyle.drawStyle) return false
        return true
    }

    /**
     * Copies all attributes from [other] into this [ReusableTextStyleScope].
     *
     * @param other source scope to copy attributes from
     */
    public fun copyFrom(other: ReusableTextStyleScope) {
        color = other.color
        brush = other.brush
        alpha = other.alpha
        fontSize = other.fontSize
        fontWeight = other.fontWeight
        _fontStyle = other._fontStyle
        _fontSynthesis = other._fontSynthesis
        fontFamily = other.fontFamily
        fontFeatureSettings = other.fontFeatureSettings
        letterSpacing = other.letterSpacing
        baselineShift = other.baselineShift
        textGeometricTransform = other.textGeometricTransform
        localeList = other.localeList
        background = other.background
        textDecoration = other.textDecoration
        shadow = other.shadow
        drawStyle = other.drawStyle
        _textAlign = other._textAlign
        _textDirection = other._textDirection
        lineHeight = other.lineHeight
        textIndent = other.textIndent
        platformStyle = other.platformStyle
        lineHeightStyle = other.lineHeightStyle
        _lineBreak = other._lineBreak
        _hyphens = other._hyphens
        textMotion = other.textMotion
    }
}
