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

import androidx.ui.engine.text.BaselineShift
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontSynthesis
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextBaseline
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.TextGeometricTransform
import androidx.ui.engine.text.TextIndent
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.text.lerp
import androidx.ui.engine.window.Locale
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.lerp
import androidx.ui.painting.basictypes.RenderComparison
import androidx.ui.toStringAsFixed

/*import androidx.ui.toStringAsFixed*/

private const val _kDefaultDebugLabel: String = "unknown"

/** The default font size if none is specified. */
private const val _defaultFontSize: Float = 14.0f

/**
 * An opaque object that determines the size, position, and rendering of text.
 *
 * Creates a new TextStyle object.
 *
 * @param color The color to use when painting the text. If this is specified, `foreground` must be null.
 * @param fontSize The size of glyphs (in logical pixels) to use when painting the text.
 * @param fontWeight The typeface thickness to use when painting the text (e.g., bold).
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italics).
 * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight or
 *                      style cannot be found in the provided custom font family.
 * @param fontFeatureSettings The advanced typography settings provided by font. The format is the same as the CSS font-feature-settings attribute:
 *                            https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
 * @param letterSpacing The amount of space (in logical pixels) to add between each letter.
 * @param wordSpacing The amount of space (in logical pixels) to add at each sequence of white-space (i.e. between each word). Only works on Android Q and above.
 * @param textBaseline The common baseline that should be aligned between this text span and its parent text span, or, for the root text spans, with the line box.
 * @param baselineShift This parameter specifies how much the baseline is shifted from the current position.
 * @param textGeometricTransform The geometric transformation applied the text.
 * @param height The height of this text span, as a multiple of the font size.
 * @param locale The locale used to select region-specific glyphs.
 * @param background The background color for the text.
 * @param decoration The decorations to paint near the text (e.g., an underline).
 * @param fontFamily The name of the font to use when painting the text (e.g., Roboto).
 * @param textIndent Specify how much a paragraph is indented.
 * @param shadow The shadow effect applied on the text.
 * @param debugLabel A human-readable description of this text style.
 *
 * It is combined with the `fontFamily` argument to set the [fontFamily] property.
 */
// TODO(Migration/qqd): Implement immutable.
// @immutable
data class TextStyle(
    val color: Color? = null,
    val fontSize: Float? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontSynthesis: FontSynthesis? = null,
    val fontFeatureSettings: String? = null,
    val letterSpacing: Float? = null,
    val wordSpacing: Float? = null,
    val textBaseline: TextBaseline? = null,
    val baselineShift: BaselineShift? = null,
    val textGeometricTransform: TextGeometricTransform? = null,
    val height: Float? = null,
    val locale: Locale? = null,
    // TODO(Migration/haoyuchang): Changed from Paint to Color.
    val background: Color? = null,
    // TODO(Migration/qqd): The flutter version we are implementing does not have "foreground" in
    // painting/TextStyle, but has it in engine/TextStyle.
    val decoration: TextDecoration? = null,
    var fontFamily: FontFamily? = null,
    val textIndent: TextIndent? = null,
    val shadow: Shadow? = null,
    val debugLabel: String? = null
) /*: Diagnosticable*/ {

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

        // TODO(siyamed) remove debug labels
        var mergedDebugLabel = ""
        if (other.debugLabel != null || debugLabel != null) {
            mergedDebugLabel = "(${debugLabel ?: _kDefaultDebugLabel}).merge(" +
                    "${other.debugLabel ?: _kDefaultDebugLabel})"
        }

        return TextStyle(
            color = other.color ?: this.color,
            fontFamily = other.fontFamily ?: this.fontFamily,
            fontSize = other.fontSize ?: this.fontSize,
            fontWeight = other.fontWeight ?: this.fontWeight,
            fontStyle = other.fontStyle ?: this.fontStyle,
            fontSynthesis = other.fontSynthesis ?: this.fontSynthesis,
            fontFeatureSettings = other.fontFeatureSettings ?: this.fontFeatureSettings,
            letterSpacing = other.letterSpacing ?: this.letterSpacing,
            wordSpacing = other.wordSpacing ?: this.wordSpacing,
            textBaseline = other.textBaseline ?: this.textBaseline,
            baselineShift = other.baselineShift ?: this.baselineShift,
            textGeometricTransform = other.textGeometricTransform ?: this.textGeometricTransform,
            height = other.height ?: this.height,
            locale = other.locale ?: this.locale,
            background = other.background ?: this.background,
            decoration = other.decoration ?: this.decoration,
            textIndent = other.textIndent ?: this.textIndent,
            shadow = other.shadow ?: this.shadow,
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
     * Values for `t` are usually obtained from an [Animation<Float>], such as an
     * [AnimationController].
     */
    companion object {
        private fun lerpColor(a: Color?, b: Color?, t: Float): Color? {
            if (a == null) {
                if (b == null) {
                    return null
                } else {
                    return b.copy(alpha = lerp(0f, 1f, t))
                }
            } else if (b == null) {
                return a.copy(alpha = lerp(1f, 0f, t))
            }
            return lerp(a, b, t)
        }

        fun lerp(a: TextStyle? = null, b: TextStyle? = null, t: Float): TextStyle? {
            val aIsNull = a == null
            val bIsNull = b == null

            if (aIsNull && bIsNull) return null
            // TODO(siyamed) remove debug labels
            val lerpDebugLabel = "lerp(${a?.debugLabel
                ?: _kDefaultDebugLabel} ⎯${t.toStringAsFixed(1)}→ ${b?.debugLabel
                ?: _kDefaultDebugLabel})"

            if (a == null) {
                val newB =
                    b?.copy(debugLabel = lerpDebugLabel) ?: TextStyle(debugLabel = lerpDebugLabel)
                return if (t < 0.5) {
                    TextStyle(
                        color = lerpColor(null, newB.color, t),
                        fontWeight = FontWeight.lerp(null, newB.fontWeight, t),
                        debugLabel = lerpDebugLabel
                    )
                } else {
                    newB.copy(
                        color = lerpColor(null, newB.color, t),
                        fontWeight = FontWeight.lerp(null, newB.fontWeight, t)
                    )
                }
            }

            if (b == null) {
                return if (t < 0.5) {
                    a.copy(
                        color = lerpColor(a.color, null, t),
                        fontWeight = FontWeight.lerp(a.fontWeight, null, t),
                        debugLabel = lerpDebugLabel
                    )
                } else {
                    TextStyle(
                        color = lerpColor(a.color, null, t),
                        fontWeight = FontWeight.lerp(a.fontWeight, null, t),
                        debugLabel = lerpDebugLabel
                    )
                }
            }

            // TODO(Migration/qqd): Currently [fontSize], [letterSpacing], [wordSpacing] and
            // [height] of textstyles a and b cannot be null if both a and b are not null, because
            // [lerp(Float, Float, Float)] API cannot take null parameters. We could have a
            // workaround by using 0.0, but for now let's keep it this way.
            return TextStyle(
                color = lerpColor(a.color, b.color, t),
                fontFamily = if (t < 0.5) a.fontFamily else b.fontFamily,
                fontSize = lerp(a.fontSize ?: b.fontSize!!, b.fontSize ?: a.fontSize!!, t),
                fontWeight = FontWeight.lerp(a.fontWeight, b.fontWeight, t),
                fontStyle = if (t < 0.5) a.fontStyle else b.fontStyle,
                fontSynthesis = if (t < 0.5) a.fontSynthesis else b.fontSynthesis,
                fontFeatureSettings = if (t < 0.5) {
                    a.fontFeatureSettings
                } else {
                    b.fontFeatureSettings
                },
                letterSpacing = lerp(
                    a.letterSpacing ?: b.letterSpacing!!,
                    b.letterSpacing ?: a.letterSpacing!!,
                    t
                ),
                wordSpacing = lerp(
                    a.wordSpacing ?: b.wordSpacing!!,
                    b.wordSpacing ?: a.wordSpacing!!,
                    t
                ),
                textBaseline = if (t < 0.5) a.textBaseline else b.textBaseline,
                baselineShift = BaselineShift.lerp(a.baselineShift, b.baselineShift, t),
                textGeometricTransform = lerp(
                    a.textGeometricTransform ?: TextGeometricTransform.None,
                    b.textGeometricTransform ?: TextGeometricTransform.None,
                    t
                ),
                height = lerp(a.height ?: b.height!!, b.height ?: a.height!!, t),
                locale = if (t < 0.5) a.locale else b.locale,
                background = if (t < 0.5) a.background else b.background,
                decoration = if (t < 0.5) a.decoration else b.decoration,
                textIndent = lerp(
                    a.textIndent ?: TextIndent.NONE,
                    b.textIndent ?: TextIndent.NONE,
                    t
                ),
                shadow = lerp(
                    a.shadow ?: Shadow(),
                    b.shadow ?: Shadow(),
                    t
                ),
                debugLabel = lerpDebugLabel
            )
        }
    }

    /** The style information for text runs, encoded for use by ui. */
    fun getTextStyle(textScaleFactor: Float = 1.0f): androidx.ui.engine.text.TextStyle {
        return androidx.ui.engine.text.TextStyle(
            color = color,
            decoration = decoration,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFeatureSettings = fontFeatureSettings,
            fontFamily = fontFamily,
            fontSize = if (fontSize == null) null else (fontSize * textScaleFactor),
            letterSpacing = letterSpacing,
            wordSpacing = wordSpacing,
            textBaseline = textBaseline,
            baselineShift = baselineShift,
            textGeometricTransform = textGeometricTransform,
            height = height,
            locale = locale,
            background = background,
            shadow = shadow
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
        textScaleFactor: Float = 1.0f,
        ellipsis: Boolean? = null,
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
        if (fontFamily != other.fontFamily ||
            fontSize != other.fontSize ||
            fontWeight != other.fontWeight ||
            fontStyle != other.fontStyle ||
            fontSynthesis != other.fontSynthesis ||
            fontFeatureSettings != other.fontFeatureSettings ||
            letterSpacing != other.letterSpacing ||
            wordSpacing != other.wordSpacing ||
            textBaseline != other.textBaseline ||
            baselineShift != other.baselineShift ||
            textGeometricTransform != other.textGeometricTransform ||
            height != other.height ||
            locale != other.locale ||
            background != other.background
        ) {
            return RenderComparison.LAYOUT
        }
        if (color != other.color || decoration != other.decoration || shadow != other.shadow) {
            return RenderComparison.PAINT
        }
        return RenderComparison.IDENTICAL
    }

    /*override fun toStringShort() = describeIdentity(this)

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
        styles.add(FloatProperty.create("size", fontSize, defaultValue = null))
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
        styles.add(FloatProperty.create("letterSpacing", letterSpacing, defaultValue = null))
        styles.add(FloatProperty.create("wordSpacing", wordSpacing, defaultValue = null))
        styles.add(EnumProperty<TextBaseline>("baseline", textBaseline, defaultValue = null))
        styles.add(FloatProperty.create("baselineShift",
            baselineShift?.multiplier, defaultValue = null))
        styles.add(FloatProperty.create("height", height, unit = "x", defaultValue = null))
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
        if (decoration != null) {
            var decorationDescription: MutableList<String> = mutableListOf()

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
    } */
}
