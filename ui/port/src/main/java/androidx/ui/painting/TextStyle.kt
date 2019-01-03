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

package androidx.ui.painting

import androidx.ui.assert
import androidx.ui.clamp
import androidx.ui.describeEnum
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontSynthesis
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextBaseline
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextDecorationStyle
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.window.Locale
import androidx.ui.foundation.diagnostics.DiagnosticLevel
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.Diagnosticable
import androidx.ui.foundation.diagnostics.DiagnosticsNode
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.MessageProperty
import androidx.ui.foundation.diagnostics.StringProperty
import androidx.ui.foundation.diagnostics.describeIdentity
import androidx.ui.lerpDouble
import androidx.ui.painting.basictypes.RenderComparison
import androidx.ui.toStringAsFixed

private const val _kDefaultDebugLabel: String = "unknown"

/** The default font size if none is specified. */
private const val _defaultFontSize: Double = 14.0

/**
 * An opaque object that determines the size, position, and rendering of text.
 *
 * Creates a new TextStyle object.
 *
 * * `color`: The color to use when painting the text. If this is specified, `foreground` must be null.
 * * `fontSize`: The size of glyphs (in logical pixels) to use when painting the text.
 * * `fontWeight`: The typeface thickness to use when painting the text (e.g., bold).
 * * `fontStyle`: The typeface variant to use when drawing the letters (e.g., italics).
 * * `letterSpacing`: The amount of space (in logical pixels) to add between each letter.
 * * `wordSpacing`: The amount of space (in logical pixels) to add at each sequence of white-space (i.e. between each word).
 * * `textBaseline`: The common baseline that should be aligned between this text span and its parent text span, or, for the root text spans, with the line box.
 * * `height`: The height of this text span, as a multiple of the font size.
 * * `locale`: The locale used to select region-specific glyphs.
 * * `background`: The paint drawn as a background for the text.
 * * `decoration`: The decorations to paint near the text (e.g., an underline).
 * * `decorationColor`: The color in which to paint the text decorations.
 * * `decorationStyle`: The style in which to paint the text decorations (e.g., dashed).
 * * `debugLabel`: A human-readable description of this text style.
 * * `fontFamily`: The name of the font to use when painting the text (e.g., Roboto).
 * * It is combined with the `fontFamily` argument to set the [fontFamily] property.
 */
// TODO(Migration/qqd): Implement immutable.
// @immutable
data class TextStyle(
    val inherit: Boolean? = true,
    val color: Color? = null,
    val fontSize: Double? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontSynthesis: FontSynthesis? = null,
    val letterSpacing: Double? = null,
    val wordSpacing: Double? = null,
    val textBaseline: TextBaseline? = null,
    val height: Double? = null,
    val locale: Locale? = null,
    val background: Paint? = null,
    // TODO(Migration/qqd): The flutter version we are implementing does not have "foreground" in
    // painting/TextStyle, but has it in engine/TextStyle.
    val decoration: TextDecoration? = null,
    val decorationColor: Color? = null,
    val decorationStyle: TextDecorationStyle? = null,
    val debugLabel: String? = null,
    var fontFamily: FontFamily? = null
) : Diagnosticable {

    init {
        assert(inherit != null)
    }

    /**
     * Creates a copy of this text style replacing or altering the specified properties.
     *
     * The non-numeric properties [color], [fontFamily], [decoration], [decorationColor] and
     * [decorationStyle] are replaced with the new values.
     *
     * The numeric properties are multiplied by the given factors and then incremented by the given
     * deltas.
     *
     * For example, `style.apply(fontSizeFactor: 2.0, fontSizeDelta: 1.0)` would return a
     * [TextStyle] whose [fontSize] is `style.fontSize * 2.0 + 1.0`.
     *
     * For the [fontWeight], the delta is applied to the [FontWeight] enum index values, so that for
     * instance `style.apply(fontWeightDelta: -2)` when applied to a `style` whose [fontWeight] is
     * [FontWeight.w500] will return a [TextStyle] with a [FontWeight.w300].
     *
     * The numeric arguments must not be null.
     *
     * If the underlying values are null, then the corresponding factors and/or deltas must not be
     * specified.
     *
     * If [foreground] is specified on this object, then applying [color] here will have no effect.
     */
    fun apply(
        color: Color? = null,
        decoration: TextDecoration? = null,
        decorationColor: Color? = null,
        decorationStyle: TextDecorationStyle? = null,
        fontFamily: FontFamily? = null,
        fontSizeFactor: Double = 1.0,
        fontSizeDelta: Double = 0.0,
        fontWeightDelta: Int = 0,
        letterSpacingFactor: Double = 1.0,
        letterSpacingDelta: Double = 0.0,
        wordSpacingFactor: Double = 1.0,
        wordSpacingDelta: Double = 0.0,
        heightFactor: Double = 1.0,
        heightDelta: Double = 0.0
    ): TextStyle {
        assert(fontSize != null || (fontSizeFactor == 1.0 && fontSizeDelta == 0.0))
        assert(fontWeight != null || fontWeightDelta == 0)
        assert(letterSpacing != null || (letterSpacingFactor == 1.0 && letterSpacingDelta == 0.0))
        assert(wordSpacing != null || (wordSpacingFactor == 1.0 && wordSpacingDelta == 0.0))

        var modifiedDebugLabel = ""

        assert {
            if (debugLabel != null) {
                modifiedDebugLabel = "($debugLabel).apply"
            }
            true
        }

        return TextStyle(
            inherit = inherit,
            color = color ?: this.color,
            fontFamily = fontFamily ?: this.fontFamily,
            fontSize = if (fontSize == null) null else fontSize * fontSizeFactor + fontSizeDelta,
            fontWeight = if (fontWeight == null) null else {
                FontWeight.values[
                    (fontWeight.index + fontWeightDelta).clamp(0, FontWeight.values.size - 1)
                ]
            },
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            letterSpacing = if (letterSpacing == null) null else {
                letterSpacing * letterSpacingFactor + letterSpacingDelta
            },
            wordSpacing =
            if (wordSpacing == null) null else wordSpacing * wordSpacingFactor + wordSpacingDelta,
            textBaseline = textBaseline,
            height = if (height == null) null else height * heightFactor + heightDelta,
            locale = locale,
            background = background,
            decoration = decoration ?: this.decoration,
            decorationColor = decorationColor ?: this.decorationColor,
            decorationStyle = decorationStyle ?: this.decorationStyle,
            debugLabel = modifiedDebugLabel
        )
    }

    /**
     * Returns a new text style that is a combination of this style and the given [other] style.
     *
     * If the given [other] text style has its [TextStyle.inherit] set to true, its null properties
     * are replaced with the non-null properties of this text style. The [other] style
     * _inherits_ the properties of this style. Another way to think of it is that the "missing"
     * properties of the [other] style are _filled_ by the properties of this style.
     *
     * If the given [other] text style has its [TextStyle.inherit] set to false, returns the given
     * [other] style unchanged. The [other] style does not inherit properties of this style.
     *
     * If the given text style is null, returns this text style.
     */
    fun merge(other: TextStyle? = null): TextStyle {
        if (other == null) return this
        if (!other.inherit!!) return other

        var mergedDebugLabel = ""
        assert {
            if (other.debugLabel != null || debugLabel != null) {
                mergedDebugLabel = "(${debugLabel ?: _kDefaultDebugLabel}).merge(${other.debugLabel
                    ?: _kDefaultDebugLabel})"
            }
            true
        }

        return TextStyle(
            inherit = inherit,
            color = other.color ?: this.color,
            fontFamily = other.fontFamily ?: this.fontFamily,
            fontSize = other.fontSize ?: this.fontSize,
            fontWeight = other.fontWeight ?: this.fontWeight,
            fontStyle = other.fontStyle ?: this.fontStyle,
            fontSynthesis = other.fontSynthesis ?: this.fontSynthesis,
            letterSpacing = other.letterSpacing ?: this.letterSpacing,
            wordSpacing = other.wordSpacing ?: this.wordSpacing,
            textBaseline = other.textBaseline ?: this.textBaseline,
            height = other.height ?: this.height,
            locale = other.locale ?: this.locale,
            background = other.background ?: this.background,
            decoration = other.decoration ?: this.decoration,
            decorationColor = other.decorationColor ?: this.decorationColor,
            decorationStyle = other.decorationStyle ?: this.decorationStyle,
            debugLabel = mergedDebugLabel
        )
    }

    /**
     * Interpolate between two text styles.
     *
     * This will not work well if the styles don't set the same fields.
     *
     * The `t` argument represents position on the timeline, with 0.0 meaning that the interpolation
     * has not started, returning `a` (or something equivalent to `a`), 1.0 meaning that the
     * interpolation has finished, returning `b` (or something equivalent to `b`), and values in
     * between meaning that the interpolation is at the relevant point on the timeline between `a`
     * and `b`. The interpolation can be extrapolated beyond 0.0 and 1.0, so negative values and
     * values greater than 1.0 are valid (and can easily be generated by curves such as
     * [Curves.elasticInOut]).
     *
     * Values for `t` are usually obtained from an [Animation<double>], such as an
     * [AnimationController].
     */
    companion object {
        fun lerp(a: TextStyle? = null, b: TextStyle? = null, t: Double): TextStyle? {
            val aIsNull = a == null
            val bIsNull = b == null
            val inheritEqual = a?.inherit == b?.inherit
            assert(aIsNull || bIsNull || inheritEqual)
            if (aIsNull && bIsNull) return null

            var lerpDebugLabel = ""
            assert {
                lerpDebugLabel =
                        "lerp(${a?.debugLabel
                            ?: _kDefaultDebugLabel} ⎯${t.toStringAsFixed(1)}→ ${b?.debugLabel
                            ?: _kDefaultDebugLabel})"
                true
            }

            if (a == null) {
                val newB = TextStyle(
                    inherit = b?.inherit,
                    color = b?.color,
                    fontFamily = b?.fontFamily,
                    fontSize = b?.fontSize,
                    fontWeight = b?.fontWeight,
                    fontStyle = b?.fontStyle,
                    fontSynthesis = b?.fontSynthesis,
                    letterSpacing = b?.letterSpacing,
                    wordSpacing = b?.wordSpacing,
                    textBaseline = b?.textBaseline,
                    height = b?.height,
                    locale = b?.locale,
                    background = b?.background,
                    decoration = b?.decoration,
                    decorationColor = b?.decorationColor,
                    decorationStyle = b?.decorationStyle,
                    debugLabel = lerpDebugLabel
                )
                if (t < 0.5) {
                    return TextStyle(
                        inherit = newB.inherit,
                        color = Color.lerp(null, newB.color, t),
                        fontWeight = FontWeight.lerp(null, newB.fontWeight, t),
                        decorationColor = Color.lerp(null, newB.decorationColor, t),
                        debugLabel = lerpDebugLabel
                    )
                } else {
                    return TextStyle(
                        inherit = newB.inherit,
                        color = Color.lerp(null, newB.color, t),
                        fontFamily = newB.fontFamily,
                        fontSize = newB.fontSize,
                        fontWeight = FontWeight.lerp(null, newB.fontWeight, t),
                        fontStyle = newB.fontStyle,
                        fontSynthesis = newB.fontSynthesis,
                        letterSpacing = newB.letterSpacing,
                        wordSpacing = newB.wordSpacing,
                        textBaseline = newB.textBaseline,
                        height = newB.height,
                        locale = newB.locale,
                        background = newB.background,
                        decoration = newB.decoration,
                        decorationColor = Color.lerp(null, newB.decorationColor, t),
                        decorationStyle = newB.decorationStyle,
                        debugLabel = lerpDebugLabel
                    )
                }
            }

            if (b == null) {
                if (t < 0.5) {
                    return TextStyle(
                        inherit = a.inherit,
                        color = Color.lerp(a.color, null, t),
                        fontFamily = a.fontFamily,
                        fontSize = a.fontSize,
                        fontWeight = FontWeight.lerp(a.fontWeight, null, t),
                        fontStyle = a.fontStyle,
                        fontSynthesis = a.fontSynthesis,
                        letterSpacing = a.letterSpacing,
                        wordSpacing = a.wordSpacing,
                        textBaseline = a.textBaseline,
                        height = a.height,
                        locale = a.locale,
                        background = a.background,
                        decoration = a.decoration,
                        decorationColor = Color.lerp(a.decorationColor, null, t),
                        decorationStyle = a.decorationStyle,
                        debugLabel = lerpDebugLabel
                    )
                } else {
                    return TextStyle(
                        inherit = a.inherit,
                        color = Color.lerp(a.color, null, t),
                        fontWeight = FontWeight.lerp(a.fontWeight, null, t),
                        decorationColor = Color.lerp(a.decorationColor, null, t),
                        debugLabel = lerpDebugLabel
                    )
                }
            }

            // TODO(Migration/qqd): Currently [fontSize], [letterSpacing], [wordSpacing] and
            // [height] of textstyles a and b cannot be null if both a and b are not null, because
            // [lerpDouble(Double, Double, Double)] API cannot take null parameters. We could have a
            // workaround by using 0.0, but for now let's keep it this way.
            return TextStyle(
                inherit = b.inherit,
                color = Color.lerp(a.color, b.color, t),
                fontFamily = if (t < 0.5) a.fontFamily else b.fontFamily,
                fontSize = lerpDouble(a.fontSize ?: b.fontSize!!, b.fontSize ?: a.fontSize!!, t),
                fontWeight = FontWeight.lerp(a.fontWeight, b.fontWeight, t),
                fontStyle = if (t < 0.5) a.fontStyle else b.fontStyle,
                fontSynthesis = if (t < 0.5) a.fontSynthesis else b.fontSynthesis,
                letterSpacing = lerpDouble(
                    a.letterSpacing ?: b.letterSpacing!!,
                    b.letterSpacing ?: a.letterSpacing!!,
                    t
                ),
                wordSpacing = lerpDouble(
                    a.wordSpacing ?: b.wordSpacing!!,
                    b.wordSpacing ?: a.wordSpacing!!,
                    t
                ),
                textBaseline = if (t < 0.5) a.textBaseline else b.textBaseline,
                height = lerpDouble(a.height ?: b.height!!, b.height ?: a.height!!, t),
                locale = if (t < 0.5) a.locale else b.locale,
                background = if (t < 0.5) a.background else b.background,
                decoration = if (t < 0.5) a.decoration else b.decoration,
                decorationColor = Color.lerp(a.decorationColor, b.decorationColor, t),
                decorationStyle = if (t < 0.5) a.decorationStyle else b.decorationStyle,
                debugLabel = lerpDebugLabel
            )
        }
    }

    /** The style information for text runs, encoded for use by ui. */
    fun getTextStyle(textScaleFactor: Double = 1.0): androidx.ui.engine.text.TextStyle {
        return androidx.ui.engine.text.TextStyle(
            color = color,
            decoration = decoration,
            decorationColor = decorationColor,
            decorationStyle = decorationStyle,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            textBaseline = textBaseline,
            fontFamily = fontFamily,
            fontSize = if (fontSize == null) null else (fontSize * textScaleFactor),
            letterSpacing = letterSpacing,
            wordSpacing = wordSpacing,
            height = height,
            locale = locale,
            background = background
        )
    }

    /**
     * The style information for paragraphs, encoded for use by `ui`.
     * The `textScaleFactor` argument must not be null. If omitted, it defaults
     * to 1.0. The other arguments may be null. The `maxLines` argument, if
     * specified and non-null, must be greater than zero.
     *
     * If the font size on this style isn't set, it will default to 14 logical
     * pixels.
     */
    fun getParagraphStyle(
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        textScaleFactor: Double = 1.0,
        ellipsis: String? = null,
        maxLines: Int? = null,
        locale: Locale? = null
    ): ParagraphStyle {
        assert(maxLines == null || maxLines > 0)
        return ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            maxLines = maxLines,
            fontFamily = fontFamily,
            fontSize = (fontSize ?: _defaultFontSize) * textScaleFactor,
            lineHeight = height,
            ellipsis = ellipsis,
            locale = locale,
            fontSynthesis = fontSynthesis
        )
    }

    /**
     * Describe the difference between this style and another, in terms of how
     * much damage it will make to the rendering.
     *
     * See also:
     *
     *  * [TextSpan.compareTo], which does the same thing for entire [TextSpan]s.
     */
    fun compareTo(other: TextStyle): RenderComparison {
        if (this == other) {
            return RenderComparison.IDENTICAL
        }
        if (inherit != other.inherit ||
            fontFamily != other.fontFamily ||
            fontSize != other.fontSize ||
            fontWeight != other.fontWeight ||
            fontStyle != other.fontStyle ||
            fontSynthesis != other.fontSynthesis ||
            letterSpacing != other.letterSpacing ||
            wordSpacing != other.wordSpacing ||
            textBaseline != other.textBaseline ||
            height != other.height ||
            locale != other.locale ||
            background != other.background
        ) {
            return RenderComparison.LAYOUT
        }
        if (color != other.color ||
            decoration != other.decoration ||
            decorationColor != other.decorationColor ||
            decorationStyle != other.decorationStyle
        ) {
            return RenderComparison.PAINT
        }
        return RenderComparison.IDENTICAL
    }

    override fun toStringShort() = describeIdentity(this)

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        if (debugLabel != null) {
            properties.add(MessageProperty("debugLabel", debugLabel))
        }
        var styles: MutableList<DiagnosticsNode> = mutableListOf<DiagnosticsNode>()
        styles.add(DiagnosticsProperty.create("color", color, defaultValue = null))
        styles.add(
            StringProperty(
                "family",
                fontFamily.toString(),
                defaultValue = null,
                quoted = false
            )
        )
        styles.add(DoubleProperty.create("size", fontSize, defaultValue = null))
        var weightDescription = ""
        if (fontWeight != null) {
            when (fontWeight) {
                FontWeight.w100 -> weightDescription = "100"
                FontWeight.w200 -> weightDescription = "200"
                FontWeight.w300 -> weightDescription = "300"
                FontWeight.w400 -> weightDescription = "400"
                FontWeight.w500 -> weightDescription = "500"
                FontWeight.w600 -> weightDescription = "600"
                FontWeight.w700 -> weightDescription = "700"
                FontWeight.w800 -> weightDescription = "800"
                FontWeight.w900 -> weightDescription = "900"
            }
        }
        // TODO(jacobr): switch this to use enumProperty which will either cause the
        // weight description to change to w600 from 600 or require existing
        // enumProperty to handle this special case.
        styles.add(
            DiagnosticsProperty.create(
                "weight",
                fontWeight,
                description = weightDescription,
                defaultValue = null
            )
        )
        styles.add(EnumProperty<FontStyle>("style", fontStyle, defaultValue = null))
        styles.add(StringProperty("fontSynthesis", fontSynthesis?.toString(), defaultValue = null))
        styles.add(DoubleProperty.create("letterSpacing", letterSpacing, defaultValue = null))
        styles.add(DoubleProperty.create("wordSpacing", wordSpacing, defaultValue = null))
        styles.add(EnumProperty<TextBaseline>("baseline", textBaseline, defaultValue = null))
        styles.add(DoubleProperty.create("height", height, unit = "x", defaultValue = null))
        styles.add(
            StringProperty(
                "locale",
                locale?.toString(),
                defaultValue = null,
                quoted = false
            )
        )
        styles.add(
            StringProperty(
                "background",
                background?.toString(),
                defaultValue = null,
                quoted = false
            )
        )
        if (decoration != null || decorationColor != null || decorationStyle != null) {
            var decorationDescription: MutableList<String> = mutableListOf()
            if (decorationStyle != null) {
                decorationDescription.add(describeEnum(decorationStyle))
            }

            // Hide decorationColor from the default text view as it is shown in the
            // terse decoration summary as well.
            styles.add(
                DiagnosticsProperty.create(
                    "decorationColor",
                    decorationColor,
                    defaultValue = null,
                    level = DiagnosticLevel.fine
                )
            )

            if (decorationColor != null) {
                decorationDescription.add("$decorationColor")
            }

            // Intentionally collide with the property 'decoration' added below.
            // Tools that show hidden properties could choose the first property
            // matching the name to disambiguate.
            styles.add(
                DiagnosticsProperty.create(
                    "decoration",
                    decoration,
                    defaultValue = null,
                    level = DiagnosticLevel.hidden
                )
            )
            if (decoration != null) {
                decorationDescription.add("$decoration")
            }
            assert(decorationDescription.isNotEmpty())
            styles.add(
                MessageProperty(
                    "decoration",
                    decorationDescription.joinToString(separator = " ")
                )
            )
        }

        properties.add(
            DiagnosticsProperty.create(
                "inherit",
                inherit,
                level = DiagnosticLevel.info
            )
        )
        styles.iterator().forEach { properties.add(it) }
    }
}
