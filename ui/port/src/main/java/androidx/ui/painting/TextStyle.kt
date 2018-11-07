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

import androidx.ui.engine.text.FontFallback
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextBaseline
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextDecorationStyle
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.window.Locale
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.Diagnosticable
import androidx.ui.painting.basictypes.RenderComparison

// val _kDefaultDebugLabel: String = "unknown"

private const val _kColorForegroundWarning: String =
    """"Cannot provide both a color and a foreground
    The color argument is just a shorthand for 'val foreground = Paint()..color = color'."""

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
 * * `foreground`: The paint used to draw the text. If this is specified, `color` must be null.
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
    val letterSpacing: Double? = null,
    val wordSpacing: Double? = null,
    val textBaseline: TextBaseline? = null,
    val height: Double? = null,
    val locale: Locale? = null,
    val foreground: Paint? = null,
    val background: Paint? = null,
    val decoration: TextDecoration? = null,
    val decorationColor: Color? = null,
    val decorationStyle: TextDecorationStyle? = null,
    val debugLabel: String? = null,
    // TODO(Migration/qqd): fontFamily was String
    var fontFamily: FontFallback? = null
    // TODO(Migration/qqd): Delete packageName since we don't plan to implement it.
//    val packageName: String? = null
) : Diagnosticable {

    init {
        // TODO(Migration/qqd): Delete packageName since we don't plan to implement it.
//        if (packageName == null) {
//            fontFamily = fontFamily
//        } else {
//            // fontFamily = "packages//${packageName}//${fontFamily.toString()}"
//        }
        assert(inherit != null)
        assert((color == null) || (foreground == null)) { _kColorForegroundWarning }
    }

    // TODO(Migration/qqd): Implement apply.
    // / Creates a copy of this text style replacing or altering the specified
    // / properties.
    // /
    // / The non-numeric properties [color], [fontFamily], [decoration],
    // / [decorationColor] and [decorationStyle] are replaced with the new values.
    // /
    // / [foreground] will be given preference over [color] if it is not null.
    // /
    // / The numeric properties are multiplied by the given factors and then
    // / incremented by the given deltas.
    // /
    // / For example, `style.apply(fontSizeFactor: 2.0, fontSizeDelta: 1.0)` would
    // / return a [TextStyle] whose [fontSize] is `style.fontSize * 2.0 + 1.0`.
    // /
    // / For the [fontWeight], the delta is applied to the [FontWeight] enum index
    // / values, so that for instance `style.apply(fontWeightDelta: -2)` whenv
    // / applied to a `style` whose [fontWeight] is [FontWeight.w500] will return a
    // / [TextStyle] with a [FontWeight.w300].
    // /
    // / The numeric arguments must not be null.
    // /
    // / If the underlying values are null, then the corresponding factors and/or
    // / deltas must not be specified.
    // /
    // / If [foreground] is specified on this object, then applying [color] here
    // / will have no effect.
//    TextStyle apply({
//        Color color,
//        TextDecoration decoration,
//        Color decorationColor,
//        TextDecorationStyle decorationStyle,
//        String fontFamily,
//        double fontSizeFactor = 1.0,
//        double fontSizeDelta = 0.0,
//        int fontWeightDelta = 0,
//        double letterSpacingFactor = 1.0,
//        double letterSpacingDelta = 0.0,
//        double wordSpacingFactor = 1.0,
//        double wordSpacingDelta = 0.0,
//        double heightFactor = 1.0,
//        double heightDelta = 0.0,
//    }) {
//        assert(fontSizeFactor != null);
//        assert(fontSizeDelta != null);
//        assert(fontSize != null || (fontSizeFactor == 1.0 && fontSizeDelta == 0.0));
//        assert(fontWeightDelta != null);
//        assert(fontWeight != null || fontWeightDelta == 0.0);
//        assert(letterSpacingFactor != null);
//        assert(letterSpacingDelta != null);
//        assert(letterSpacing != null || (letterSpacingFactor == 1.0 && letterSpacingDelta == 0.0));
//        assert(wordSpacingFactor != null);
//        assert(wordSpacingDelta != null);
//        assert(wordSpacing != null || (wordSpacingFactor == 1.0 && wordSpacingDelta == 0.0));
//        assert(heightFactor != null);
//        assert(heightDelta != null);
//        assert(heightFactor != null || (heightFactor == 1.0 && heightDelta == 0.0));
//
//        String modifiedDebugLabel;
//        assert(() {
//            if (debugLabel != null)
//                modifiedDebugLabel = '($debugLabel).apply';
//            return true;
//        }());
//
//        return TextStyle(
//            inherit: inherit,
//            color: foreground == null ? color ?? this.color : null,
//        fontFamily: fontFamily ?? this.fontFamily,
//        fontSize: fontSize == null ? null : fontSize * fontSizeFactor + fontSizeDelta,
//        fontWeight: fontWeight == null ? null : FontWeight.values[(fontWeight.index + fontWeightDelta).clamp(0, FontWeight.values.length - 1)],
//        fontStyle: fontStyle,
//        letterSpacing: letterSpacing == null ? null : letterSpacing * letterSpacingFactor + letterSpacingDelta,
//        wordSpacing: wordSpacing == null ? null : wordSpacing * wordSpacingFactor + wordSpacingDelta,
//        textBaseline: textBaseline,
//        height: height == null ? null : height * heightFactor + heightDelta,
//        locale: locale,
//        foreground: foreground != null ? foreground : null,
//        background: background,
//        decoration: decoration ?? this.decoration,
//        decorationColor: decorationColor ?? this.decorationColor,
//        decorationStyle: decorationStyle ?? this.decorationStyle,
//        debugLabel: modifiedDebugLabel,
//        );
//    }

    // TODO(Migration/qqd): Implement merge.
    // / Returns a new text style that is a combination of this style and the given
    // / [other] style.
    // /
    // / If the given [other] text style has its [TextStyle.inherit] set to true,
    // / its null properties are replaced with the non-null properties of this text
    // / style. The [other] style _inherits_ the properties of this style. Another
    // / way to think of it is that the "missing" properties of the [other] style
    // / are _filled_ by the properties of this style.
    // /
    // / If the given [other] text style has its [TextStyle.inherit] set to false,
    // / returns the given [other] style unchanged. The [other] style does not
    // / inherit properties of this style.
    // /
    // / If the given text style is null, returns this text style.
    // /
    // / One of [color] or [foreground] must be null, and if this or `other` has
    // / [foreground] specified it will be given preference over any color parameter.
//    TextStyle merge(TextStyle other) {
//        if (other == null)
//            return this;
//        if (!other.inherit)
//            return other;
//
//        String mergedDebugLabel;
//        assert(() {
//            if (other.debugLabel != null || debugLabel != null)
//                mergedDebugLabel = '(${debugLabel ?? _kDefaultDebugLabel}).merge(${other.debugLabel ?? _kDefaultDebugLabel})';
//            return true;
//        }());
//
//        return copyWith(
//            color: other.color,
//        fontFamily: other.fontFamily,
//        fontSize: other.fontSize,
//        fontWeight: other.fontWeight,
//        fontStyle: other.fontStyle,
//        letterSpacing: other.letterSpacing,
//        wordSpacing: other.wordSpacing,
//        textBaseline: other.textBaseline,
//        height: other.height,
//        locale: other.locale,
//        foreground: other.foreground,
//        background: other.background,
//        decoration: other.decoration,
//        decorationColor: other.decorationColor,
//        decorationStyle: other.decorationStyle,
//        debugLabel: mergedDebugLabel,
//        );
//    }

    // TODO(Migration/qqd): Implement lerp.
    // / Interpolate between two text styles.
    // /
    // / This will not work well if the styles don't set the same fields.
    // /
    // / The `t` argument represents position on the timeline, with 0.0 meaning
    // / that the interpolation has not started, returning `a` (or something
    // / equivalent to `a`), 1.0 meaning that the interpolation has finished,
    // / returning `b` (or something equivalent to `b`), and values in between
    // / meaning that the interpolation is at the relevant point on the timeline
    // / between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
    // / 1.0, so negative values and values greater than 1.0 are valid (and can
    // / easily be generated by curves such as [Curves.elasticInOut]).
    // /
    // / Values for `t` are usually obtained from an [Animation<double>], such as
    // / an [AnimationController].
    // /
    // / If [foreground] is specified on either of `a` or `b`, both will be treated
    // / as if they have a [foreground] paint (creating a new [Paint] if necessary
    // / based on the [color] property).
//    static TextStyle lerp(TextStyle a, TextStyle b, double t) {
//        assert(t != null);
//        assert(a == null || b == null || a.inherit == b.inherit);
//        if (a == null && b == null) {
//            return null;
//        }
//
//        String lerpDebugLabel;
//        assert(() {
//            lerpDebugLabel = 'lerp(${a?.debugLabel ?? _kDefaultDebugLabel} ⎯${t.toStringAsFixed(1)}→ ${b?.debugLabel ?? _kDefaultDebugLabel})';
//            return true;
//        }());
//
//        if (a == null) {
//            return TextStyle(
//                inherit: b.inherit,
//            color: Color.lerp(null, b.color, t),
//            fontFamily: t < 0.5 ? null : b.fontFamily,
//            fontSize: t < 0.5 ? null : b.fontSize,
//            fontWeight: FontWeight.lerp(null, b.fontWeight, t),
//            fontStyle: t < 0.5 ? null : b.fontStyle,
//            letterSpacing: t < 0.5 ? null : b.letterSpacing,
//            wordSpacing: t < 0.5 ? null : b.wordSpacing,
//            textBaseline: t < 0.5 ? null : b.textBaseline,
//            height: t < 0.5 ? null : b.height,
//            locale: t < 0.5 ? null : b.locale,
//            foreground: t < 0.5 ? null : b.foreground,
//            background: t < 0.5 ? null : b.background,
//            decoration: t < 0.5 ? null : b.decoration,
//            decorationColor: Color.lerp(null, b.decorationColor, t),
//            decorationStyle: t < 0.5 ? null : b.decorationStyle,
//            debugLabel: lerpDebugLabel,
//            );
//        }
//
//        if (b == null) {
//            return TextStyle(
//                inherit: a.inherit,
//            color: Color.lerp(a.color, null, t),
//            fontFamily: t < 0.5 ? a.fontFamily : null,
//            fontSize: t < 0.5 ? a.fontSize : null,
//            fontWeight: FontWeight.lerp(a.fontWeight, null, t),
//            fontStyle: t < 0.5 ? a.fontStyle : null,
//            letterSpacing: t < 0.5 ? a.letterSpacing : null,
//            wordSpacing: t < 0.5 ? a.wordSpacing : null,
//            textBaseline: t < 0.5 ? a.textBaseline : null,
//            height: t < 0.5 ? a.height : null,
//            locale: t < 0.5 ? a.locale : null,
//            foreground: t < 0.5 ? a.foreground : null,
//            background: t < 0.5 ? a.background : null,
//            decoration: t < 0.5 ? a.decoration : null,
//            decorationColor: Color.lerp(a.decorationColor, null, t),
//            decorationStyle: t < 0.5 ? a.decorationStyle : null,
//            debugLabel: lerpDebugLabel,
//            );
//        }
//
//        return TextStyle(
//            inherit: b.inherit,
//        color: a.foreground == null && b.foreground == null ? Color.lerp(a.color, b.color, t) : null,
//        fontFamily: t < 0.5 ? a.fontFamily : b.fontFamily,
//        fontSize: ui.lerpDouble(a.fontSize ?? b.fontSize, b.fontSize ?? a.fontSize, t),
//        fontWeight: FontWeight.lerp(a.fontWeight, b.fontWeight, t),
//        fontStyle: t < 0.5 ? a.fontStyle : b.fontStyle,
//        letterSpacing: ui.lerpDouble(a.letterSpacing ?? b.letterSpacing, b.letterSpacing ?? a.letterSpacing, t),
//        wordSpacing: ui.lerpDouble(a.wordSpacing ?? b.wordSpacing, b.wordSpacing ?? a.wordSpacing, t),
//        textBaseline: t < 0.5 ? a.textBaseline : b.textBaseline,
//        height: ui.lerpDouble(a.height ?? b.height, b.height ?? a.height, t),
//        locale: t < 0.5 ? a.locale : b.locale,
//        foreground: (a.foreground != null || b.foreground != null)
//        ? t < 0.5
//        ? a.foreground ?? (Paint()..color = a.color)
//        : b.foreground ?? (Paint()..color = b.color)
//        : null,
//        background: t < 0.5 ? a.background : b.background,
//        decoration: t < 0.5 ? a.decoration : b.decoration,
//        decorationColor: Color.lerp(a.decorationColor, b.decorationColor, t),
//        decorationStyle: t < 0.5 ? a.decorationStyle : b.decorationStyle,
//        debugLabel: lerpDebugLabel,
//        );
//    }

    /** The style information for text runs, encoded for use by ui. */
    fun getTextStyle(textScaleFactor: Double = 1.0): androidx.ui.engine.text.TextStyle {
        return androidx.ui.engine.text.TextStyle(
            color,
            decoration,
            decorationColor,
            decorationStyle,
            fontWeight,
            fontStyle,
            textBaseline,
            fontFamily,
            if (fontSize == null) null else (fontSize * textScaleFactor),
            letterSpacing,
            wordSpacing,
            height,
            locale,
            foreground,
            background
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
        assert(textScaleFactor != null)
        assert(maxLines == null || maxLines > 0)
        return ParagraphStyle(
            textAlign,
            textDirection,
            fontWeight,
            fontStyle,
            maxLines,
            fontFamily,
            (fontSize ?: _defaultFontSize) * textScaleFactor,
            height,
            ellipsis,
            locale
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
            letterSpacing != other.letterSpacing ||
            wordSpacing != other.wordSpacing ||
            textBaseline != other.textBaseline ||
            height != other.height ||
            locale != other.locale ||
            foreground != other.foreground ||
            background != other.background) {
            return RenderComparison.LAYOUT
        }
        if (color != other.color ||
            decoration != other.decoration ||
            decorationColor != other.decorationColor ||
            decorationStyle != other.decorationStyle) {
            return RenderComparison.PAINT
        }
        return RenderComparison.IDENTICAL
    }

    // TODO(Migration/qqd): Implement toString.
    override fun toStringShort(): String {
        TODO()
    }
//    @override
//    String toStringShort() => '$runtimeType';

    // TODO(Migration/qqd): Implement debugFillProperties.
    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        TODO()
    }
// / Adds all properties prefixing property names with the optional `prefix`.
//    @override
//    void debugFillProperties(DiagnosticPropertiesBuilder properties, { String prefix = '' }) {
//        super.debugFillProperties(properties);
//        if (debugLabel != null)
//            properties.add(MessageProperty('${prefix}debugLabel', debugLabel));
//        final List<DiagnosticsNode> styles = <DiagnosticsNode>[];
//        styles.add(DiagnosticsProperty<Color>('${prefix}color', color, defaultValue: null));
//        styles.add(StringProperty('${prefix}family', fontFamily, defaultValue: null, quoted: false));
//        styles.add(DoubleProperty('${prefix}size', fontSize, defaultValue: null));
//        String weightDescription;
//        if (fontWeight != null) {
//            switch (fontWeight) {
//                case FontWeight.w100:
//                weightDescription = '100';
//                break;
//                case FontWeight.w200:
//                weightDescription = '200';
//                break;
//                case FontWeight.w300:
//                weightDescription = '300';
//                break;
//                case FontWeight.w400:
//                weightDescription = '400';
//                break;
//                case FontWeight.w500:
//                weightDescription = '500';
//                break;
//                case FontWeight.w600:
//                weightDescription = '600';
//                break;
//                case FontWeight.w700:
//                weightDescription = '700';
//                break;
//                case FontWeight.w800:
//                weightDescription = '800';
//                break;
//                case FontWeight.w900:
//                weightDescription = '900';
//                break;
//            }
//        }
//        // TODO(jacobr): switch this to use enumProperty which will either cause the
//        // weight description to change to w600 from 600 or require existing
//        // enumProperty to handle this special case.
//        styles.add(DiagnosticsProperty<FontWeight>(
//            '${prefix}weight',
//            fontWeight,
//            description: weightDescription,
//            defaultValue: null,
//        ));
//        styles.add(EnumProperty<FontStyle>('${prefix}style', fontStyle, defaultValue: null));
//        styles.add(DoubleProperty('${prefix}letterSpacing', letterSpacing, defaultValue: null));
//        styles.add(DoubleProperty('${prefix}wordSpacing', wordSpacing, defaultValue: null));
//        styles.add(EnumProperty<TextBaseline>('${prefix}baseline', textBaseline, defaultValue: null));
//        styles.add(DoubleProperty('${prefix}height', height, unit: 'x', defaultValue: null));
//        styles.add(DiagnosticsProperty<Locale>('${prefix}locale', locale, defaultValue: null));
//        styles.add(DiagnosticsProperty<Paint>('${prefix}foreground', foreground, defaultValue: null));
//        styles.add(DiagnosticsProperty<Paint>('${prefix}background', background, defaultValue: null));
//        if (decoration != null || decorationColor != null || decorationStyle != null) {
//            final List<String> decorationDescription = <String>[];
//            if (decorationStyle != null)
//                decorationDescription.add(describeEnum(decorationStyle));
//
//            // Hide decorationColor from the default text view as it is shown in the
//            // terse decoration summary as well.
//            styles.add(DiagnosticsProperty<Color>('${prefix}decorationColor', decorationColor, defaultValue: null, level: DiagnosticLevel.fine));
//
//            if (decorationColor != null)
//                decorationDescription.add('$decorationColor');
//
//            // Intentionally collide with the property 'decoration' added below.
//            // Tools that show hidden properties could choose the first property
//            // matching the name to disambiguate.
//            styles.add(DiagnosticsProperty<TextDecoration>('${prefix}decoration', decoration, defaultValue: null, level: DiagnosticLevel.hidden));
//            if (decoration != null)
//                decorationDescription.add('$decoration');
//            assert(decorationDescription.isNotEmpty);
//            styles.add(MessageProperty('${prefix}decoration', decorationDescription.join(' ')));
//        }
//
//        final bool styleSpecified = styles.any((DiagnosticsNode n) => !n.isFiltered(DiagnosticLevel.info));
//        properties.add(DiagnosticsProperty<bool>('${prefix}inherit', inherit, level: (!styleSpecified && inherit) ? DiagnosticLevel.fine : DiagnosticLevel.info));
//        styles.forEach(properties.add);
//
//        if (!styleSpecified)
//            properties.add(FlagProperty('inherit', value: inherit, ifTrue: '$prefix<all styles inherited>', ifFalse: '$prefix<no style specified>'));
//    }
}
