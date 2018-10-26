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
package androidx.ui.engine.text

import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import java.util.Locale

/**
 * An opaque object that determines the size, position, and rendering of text.
 *
 * Creates a new TextStyle object.
 *
 * * `color`: The color to use when painting the text. If this is specified, `foreground` must be null.
 * * `decoration`: The decorations to paint near the text (e.g., an underline).
 * * `decorationColor`: The color in which to paint the text decorations.
 * * `decorationStyle`: The style in which to paint the text decorations (e.g., dashed).
 * * `fontWeight`: The typeface thickness to use when painting the text (e.g., bold).
 * * `fontStyle`: The typeface variant to use when drawing the letters (e.g., italics).
 * * `fontFamily`: The name of the font to use when painting the text (e.g., Roboto).
 * * `fontSize`: The size of glyphs (in logical pixels) to use when painting the text.
 * * `letterSpacing`: The amount of space (in logical pixels) to add between each letter.
 * * `wordSpacing`: The amount of space (in logical pixels) to add at each sequence of white-space (i.e. between each word).
 * * `textBaseline`: The common baseline that should be aligned between this text span and its parent text span, or, for the root text spans, with the line box.
 * * `height`: The height of this text span, as a multiple of the font size.
 * * `locale`: The locale used to select region-specific glyphs.
 * * `background`: The paint drawn as a background for the text.
 * * `foreground`: The paint used to draw the text. If this is specified, `color` must be null.
 */
data class TextStyle constructor(
    val color: Color? = null,
    val decoration: TextDecoration? = null,
    val decorationColor: Color? = null,
    val decorationStyle: TextDecorationStyle? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val textBaseline: TextBaseline? = null,
    // TODO(Migration/siyamed): fontFamily was String
    val fontFamily: FontFallback? = null,
    val fontSize: Double? = null,
    val letterSpacing: Double? = null,
    val wordSpacing: Double? = null,
    val height: Double? = null,
    val locale: Locale? = null,
    val background: Paint? = null,
    val foreground: Paint? = null
) {
    val _fontFamily: FontFallback

    init {
        assert(color == null || foreground == null) {
            "Cannot provide both a color and a foreground\n" +
                "The color argument is just a shorthand for " +
                "'foreground: new Paint()..color = color'."
        }

        _fontFamily = fontFamily ?: FontFallback()
    }

    override fun toString(): String {
        return "TextStyle(" +
        "color: ${color ?: "unspecified"}, " +
        "decoration: ${decoration ?: "unspecified"}, " +
        "decorationColor: ${decorationColor ?: "unspecified"}, " +
        "decorationStyle: ${decorationStyle ?: "unspecified"}, " +
        "fontWeight: ${fontWeight ?: "unspecified"}, " +
        "fontStyle: ${fontStyle ?: "unspecified"}, " +
        "textBaseline: ${textBaseline ?: "unspecified"}, " +
        "fontFamily: ${fontFamily ?: "unspecified"}, " +
        "fontSize: ${fontSize ?: "unspecified"}, " +
        "letterSpacing: ${if (letterSpacing != null) "${letterSpacing}x" else "unspecified"}, " +
        "wordSpacing: ${if (wordSpacing != null) "${wordSpacing}x" else "unspecified"}, " +
        "height: ${if (height != null) "${height}x" else "unspecified"}, " +
        "locale: ${locale ?: "unspecified"}, " +
        "background: ${background ?: "unspecified"}, " +
        "foreground: ${foreground ?: "unspecified"}" +
        ")"
    }
}

// TODO(Migration/siyamed) Native defaults
// class TextStyle {
//     public:
//     SkColor color = SK_ColorWHITE;
//     int decoration = TextDecoration::kNone;
//     // Does not make sense to draw a transparent object, so we use it as a default
//     // value to indicate no decoration color was set.
//     SkColor decoration_color = SK_ColorTRANSPARENT;
//     TextDecorationStyle decoration_style = TextDecorationStyle::kSolid;
//     // Thickness is applied as a multiplier to the default thickness of the font.
//     double decoration_thickness_multiplier = 1.0;
//     FontWeight font_weight = FontWeight::w400;
//     FontStyle font_style = FontStyle::normal;
//     TextBaseline text_baseline = TextBaseline::kAlphabetic;
//     std::string font_family;
//     double font_size = 14.0;
//     double letter_spacing = 0.0;
//     double word_spacing = 0.0;
//     double height = 1.0;
//     std::string locale;
//     bool has_background = false;
//     SkPaint background;
//     bool has_foreground = false;
//     SkPaint foreground;
//
//     TextStyle();
//
//     bool equals(const TextStyle& other) const;
// };

// TODO(Migration/siyamed) Native equals
// bool TextStyle::equals(const TextStyle& other) const {
//    if (color != other.color)
//        return false;
//    if (decoration != other.decoration)
//        return false;
//    if (decoration_color != other.decoration_color)
//        return false;
//    if (decoration_style != other.decoration_style)
//        return false;
//    if (decoration_thickness_multiplier != other.decoration_thickness_multiplier)
//        return false;
//    if (font_weight != other.font_weight)
//        return false;
//    if (font_style != other.font_style)
//        return false;
//    if (font_family != other.font_family)
//        return false;
//    if (letter_spacing != other.letter_spacing)
//        return false;
//    if (word_spacing != other.word_spacing)
//        return false;
//    if (height != other.height)
//        return false;
//    if (locale != other.locale)
//        return false;
//    if (foreground != other.foreground)
//        return false;
//
//    return true;
// }

// TODO(Migration/siyamed): this is implemented to communicate with C++, we probably don't need it.
//    val _encoded: IntArray
//        _encoded = _encodeTextStyle(
//            color,
//            decoration,
//            decorationColor,
//            decorationStyle,
//            fontWeight,
//            fontStyle,
//            textBaseline,
//            fontFamily,
//            fontSize,
//            letterSpacing,
//            wordSpacing,
//            height,
//            locale,
//            background
//        )

// TODO(Migration/siyamed): toString version if we use encoded values, (alread converted to kotlin)
//        "color: ${if(_encoded[0] and 0x0002 == 0x0002) Color(_encoded[1]) else "unspecified"}, "
//        "decoration: ${if(_encoded[0] and 0x0004 == 0x0004) TextDecoration(_encoded[2]) else "unspecified"}, "
//        "decorationColor: ${if(_encoded[0] and 0x0008 == 0x0008) Color(_encoded[3]) else "unspecified"}, "
//        "decorationStyle: ${if(_encoded[0] and 0x0010 == 0x0010) TextDecorationStyle.values()[_encoded[4]] else "unspecified"}, "
//        "fontWeight: ${if(_encoded[0] and 0x0020 == 0x0020) FontWeight.values[_encoded[5]] else "unspecified"}, "
//        "fontStyle: ${if(_encoded[0] and 0x0040 == 0x0040) FontStyle.values()[_encoded[6]] else "unspecified"}, "
//        "textBaseline: ${if(_encoded[0] and 0x0080 == 0x0080) TextBaseline.values()[_encoded[7]] else "unspecified"}, "
//        "fontFamily: ${if(_encoded[0] and 0x0100 == 0x0100) fontFamily else "unspecified"}, "
//        "fontSize: ${if(_encoded[0] and 0x0200 == 0x0200) fontSize else "unspecified"}, "
//        "letterSpacing: ${if(_encoded[0] and 0x0400 == 0x0400) "${letterSpacing}x" else "unspecified"}, "
//        "wordSpacing: ${if(_encoded[0] and 0x0800 == 0x0800) "${wordSpacing}x" else "unspecified"}, "
//        "height: ${if(_encoded[0] and 0x1000 == 0x1000) "${height}x" else "unspecified"}, "
//        "locale: ${if(_encoded[0] and 0x2000 == 0x2000) locale else "unspecified"}, "
//        "background: ${if(_encoded[0] and 0x4000 == 0x4000) background else "unspecified"}, "
//        "foreground: ${if(_encoded[0] and 0x8000 == 0x8000) foreground else "unspecified"}"
//        ")";

//    @override
//    bool operator ==(dynamic other) {
//        if (identical(this, other))
//            return true;
//        if (other is! TextStyle)
//        return false;
//        final TextStyle typedOther = other;
//        if (_fontFamily != typedOther._fontFamily ||
//                _fontSize != typedOther._fontSize ||
//                _letterSpacing != typedOther._letterSpacing ||
//                _wordSpacing != typedOther._wordSpacing ||
//                _height != typedOther._height ||
//                _locale != typedOther._locale ||
//                _background != typedOther._background ||
//                _foreground != typedOther._foreground)
//            return false;
//        for (int index = 0; index < _encoded.length; index += 1) {
//            if (_encoded[index] != typedOther._encoded[index])
//                return false;
//        }
//        return true;
//    }
//
//    @override
//    int get hashCode => hashValues(hashList(_encoded), _fontFamily, _fontSize, _letterSpacing, _wordSpacing, _height, _locale, _background, _foreground);
//
// // This encoding must match the C++ version of ParagraphBuilder::pushStyle.
// //
// // The encoded array buffer has 8 elements.
// //
// //  - Element 0: A bit field where the ith bit indicates wheter the ith element
// //    has a non-null value. Bits 8 to 12 indicate whether |fontFamily|,
// //    |fontSize|, |letterSpacing|, |wordSpacing|, and |height| are non-null,
// //    respectively. Bit 0 is unused.
// //
// //  - Element 1: The |color| in ARGB with 8 bits per channel.
// //
// //  - Element 2: A bit field indicating which text decorations are present in
// //    the |textDecoration| list. The ith bit is set if there's a TextDecoration
// //    with enum index i in the list.
// //
// //  - Element 3: The |decorationColor| in ARGB with 8 bits per channel.
// //
// //  - Element 4: The bit field of the |decorationStyle|.
// //
// //  - Element 5: The index of the |fontWeight|.
// //
// //  - Element 6: The enum index of the |fontStyle|.
// //
// //  - Element 7: The enum index of the |textBaseline|.
// //
// Int32List _encodeTextStyle(Color color,
// TextDecoration decoration,
// Color decorationColor,
// TextDecorationStyle decorationStyle,
// FontWeight fontWeight,
// FontStyle fontStyle,
// TextBaseline textBaseline,
// String fontFamily,
// double fontSize,
// double letterSpacing,
// double wordSpacing,
// double height,
// Locale locale,
// Paint background,
// Paint foreground,) {
//    final Int32List result = new Int32List(8);
//    if (color != null) {
//        result[0] |= 1 << 1;
//        result[1] = color.value;
//    }
//    if (decoration != null) {
//        result[0] |= 1 << 2;
//        result[2] = decoration._mask;
//    }
//    if (decorationColor != null) {
//        result[0] |= 1 << 3;
//        result[3] = decorationColor.value;
//    }
//    if (decorationStyle != null) {
//        result[0] |= 1 << 4;
//        result[4] = decorationStyle.index;
//    }
//    if (fontWeight != null) {
//        result[0] |= 1 << 5;
//        result[5] = fontWeight.index;
//    }
//    if (fontStyle != null) {
//        result[0] |= 1 << 6;
//        result[6] = fontStyle.index;
//    }
//    if (textBaseline != null) {
//        result[0] |= 1 << 7;
//        result[7] = textBaseline.index;
//    }
//    if (fontFamily != null) {
//        result[0] |= 1 << 8;
//        // Passed separately to native.
//    }
//    if (fontSize != null) {
//        result[0] |= 1 << 9;
//        // Passed separately to native.
//    }
//    if (letterSpacing != null) {
//        result[0] |= 1 << 10;
//        // Passed separately to native.
//    }
//    if (wordSpacing != null) {
//        result[0] |= 1 << 11;
//        // Passed separately to native.
//    }
//    if (height != null) {
//        result[0] |= 1 << 12;
//        // Passed separately to native.
//    }
//    if (locale != null) {
//        result[0] |= 1 << 13;
//        // Passed separately to native.
//    }
//    if (background != null) {
//        result[0] |= 1 << 14;
//        // Passed separately to native.
//    }
//    if (foreground != null) {
//        result[0] |= 1 << 15;
//        // Passed separately to native.
//    }
//    return result;
// }