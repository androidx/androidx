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

import java.util.Locale

/**
 * An opaque object that determines the configuration used by
 * [ParagraphBuilder] to position lines within a [Paragraph] of text.
 *
 * Creates a new ParagraphStyle object.
 *
 * * `textAlign`: The alignment of the text within the lines of the
 *   paragraph. If the last line is ellipsized (see `ellipsis` below), the
 *   alignment is applied to that line after it has been truncated but before
 *   the ellipsis has been added.
 */
// TODO(Migration/siyamed): we should search for flutter in the code space and remove them
//   See: https://github.com/flutter/flutter/issues/9819
/**
 *
 * * `textDirection`: The directionality of the text, left-to-right (e.g.
 *   Norwegian) or right-to-left (e.g. Hebrew). This controls the overall
 *   directionality of the paragraph, as well as the meaning of
 *   [TextAlign.start] and [TextAlign.end] in the `textAlign` field.
 *
 * * `fontWeight`: The typeface thickness to use when painting the text
 *   (e.g., bold).
 *
 * * `fontStyle`: The typeface variant to use when drawing the letters (e.g.,
 *   italics).
 *
 * * `maxLines`: The maximum number of lines painted. Lines beyond this
 *   number are silently dropped. For example, if `maxLines` is 1, then only
 *   one line is rendered. If `maxLines` is null, but `ellipsis` is not null,
 *   then lines after the first one that overflows the width constraints are
 *   dropped. The width constraints are those set in the
 *   [ParagraphConstraints] object passed to the [Paragraph.layout] method.
 *
 * * `fontFamily`: The name of the font to use when painting the text (e.g.,
 *   Roboto).
 *
 * * `fontSize`: The size of glyphs (in logical pixels) to use when painting
 *   the text.
 *
 * * `lineHeight`: The minimum height of the line boxes, as a multiple of the
 *   font size.
 *
 * * `ellipsis`: String used to ellipsize overflowing text. If `maxLines` is
 *   not null, then the `ellipsis`, if any, is applied to the last rendered
 *   line, if that line overflows the width constraints. If `maxLines` is
 *   null, then the `ellipsis` is applied to the first line that overflows
 *   the width constraints, and subsequent lines are dropped. The width
 *   constraints are those set in the [ParagraphConstraints] object passed to
 *   the [Paragraph.layout] method. The empty string and the null value are
 *   considered equivalent and turn off this behavior.
 *
 * * `locale`: The locale used to select region-specific glyphs.
 */
data class ParagraphStyle constructor(
    val textAlign: TextAlign? = null,
    val textDirection: TextDirection? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val maxLines: Int? = null,
    // TODO(Migration/siyamed): fontFamily was String
    val fontFamily: FontFallback? = null,
    val fontSize: Double? = null,
    val lineHeight: Double? = null,
    val ellipsis: String? = null,
    val locale: Locale? = null
) {

    override fun toString(): String {
        return "ParagraphStyle(" +
            "textAlign: ${textAlign ?: "unspecified"}, " +
            "textDirection: ${textDirection ?: "unspecified"}, " +
            "fontWeight: ${fontWeight ?: "unspecified"}, " +
            "fontStyle: ${fontStyle ?: "unspecified"}, " +
            "maxLines: ${maxLines ?: "unspecified"}, " +
            "fontFamily: ${fontFamily ?: "unspecified"}, " +
            "fontSize: ${fontSize ?: "unspecified"}, " +
            "lineHeight: ${if (lineHeight != null) "${lineHeight}x" else "unspecified"}, " +
            "ellipsis: ${if (ellipsis != null) "\"$ellipsis\"" else "unspecified"}, " +
            "locale: ${locale ?: "unspecified"}" +
            ")"
    }

    fun getTextStyle(): TextStyle {
        return TextStyle(
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            fontSize = fontSize,
            locale = locale,
            height = lineHeight
        )
    }
    // TextStyle ParagraphStyle::GetTextStyle() const {
    //     TextStyle result;
    //     result.font_weight = font_weight;
    //     result.font_style = font_style;
    //     result.font_family = font_family;
    //     result.font_size = font_size;
    //     result.locale = locale;
    //     result.height = line_height;
    //     return result;
    // }
}

// TODO(Migration/siyamed): this is implemented to communicate with C++, we probably don't need it.
// except if we need to reduce memory foot print
//    private val _encoded : IntArray
//    init {
//        _encoded = _encodeParagraphStyle(textAlign, textDirection, fontWeight, fontStyle, maxLines, fontFamily, fontSize, lineHeight, ellipsis, locale)
//    }

// TODO(Migration/siyamed): toString version if we use encoded values, (alread converted to kotlin)
// toString (kotlin)
//        return "textAlign: ${if(_encoded[0] and 0x002 == 0x002) TextAlign.values()[_encoded[1]] else "unspecified"}, " +
//            "textDirection: ${if(_encoded[0] and 0x004 == 0x004) TextDirection.values()[_encoded[2]] else "unspecified"}, " +
//            "fontWeight: ${if(_encoded[0] and 0x008 == 0x008) FontWeight.values[_encoded[3]] else "unspecified"}, " +
//            "fontStyle: ${if(_encoded[0] and 0x010 == 0x010) FontStyle.values()[_encoded[4]] else "unspecified"}, " +
//            "maxLines: ${if(_encoded[0] and 0x020 == 0x020) _encoded[5] else "unspecified"}, " +
//            "fontFamily: ${if(_encoded[0] and 0x040 == 0x040) fontFamily else "unspecified"}, " +
//            "fontSize: ${if(_encoded[0] and 0x080 == 0x080) fontSize else "unspecified"}, " +
//            "lineHeight: ${if(_encoded[0] and 0x100 == 0x100) "${lineHeight}x" else "unspecified"}, " +
//            "ellipsis: ${if(_encoded[0] and 0x200 == 0x200) "\"$ellipsis\"" else "unspecified"}, " +
//            "locale: ${if(_encoded[0] and 0x400 == 0x400) locale else "unspecified"}"
//        ")"

//    @override
//    bool operator ==(dynamic other) {
//        if (identical(this, other))
//            return true;
//        if (other.runtimeType != runtimeType)
//            return false;
//        final ParagraphStyle typedOther = other;
//        if (_fontFamily != typedOther._fontFamily ||
//                _fontSize != typedOther._fontSize ||
//                _lineHeight != typedOther._lineHeight ||
//                _ellipsis != typedOther._ellipsis ||
//                _locale != typedOther._locale)
//            return false;
//        for (int index = 0; index < _encoded.length; index += 1) {
//            if (_encoded[index] != typedOther._encoded[index])
//                return false;
//        }
//        return true;
//    }
//
//    @override
//    int get hashCode => hashValues(hashList(_encoded), _fontFamily, _fontSize, _lineHeight, _ellipsis, _locale);

// This encoding must match the C++ version ParagraphBuilder::build.
//
// The encoded array buffer has 5 elements.
//
//  - Element 0: A bit mask indicating which fields are non-null.
//    Bit 0 is unused. Bits 1-n are set if the corresponding index in the
//    encoded array is non-null.  The remaining bits represent fields that
//    are passed separately from the array.
//
//  - Element 1: The enum index of the |textAlign|.
//
//  - Element 2: The index of the |fontWeight|.
//
//  - Element 3: The enum index of the |fontStyle|.
//
//  - Element 4: The value of |maxLines|.
//
// Int32List _encodeParagraphStyle(TextAlign textAlign,
// TextDirection textDirection,
// FontWeight fontWeight,
// FontStyle fontStyle,
// int maxLines,
// String fontFamily,
// double fontSize,
// double lineHeight,
// String ellipsis,
// Locale locale,) {
//    final Int32List result = new Int32List(6); // also update paragraph_builder.cc
//    if (textAlign != null) {
//        result[0] |= 1 << 1;
//        result[1] = textAlign.index;
//    }
//    if (textDirection != null) {
//        result[0] |= 1 << 2;
//        result[2] = textDirection.index;
//    }
//    if (fontWeight != null) {
//        result[0] |= 1 << 3;
//        result[3] = fontWeight.index;
//    }
//    if (fontStyle != null) {
//        result[0] |= 1 << 4;
//        result[4] = fontStyle.index;
//    }
//    if (maxLines != null) {
//        result[0] |= 1 << 5;
//        result[5] = maxLines;
//    }
//    if (fontFamily != null) {
//        result[0] |= 1 << 6;
//        // Passed separately to native.
//    }
//    if (fontSize != null) {
//        result[0] |= 1 << 7;
//        // Passed separately to native.
//    }
//    if (lineHeight != null) {
//        result[0] |= 1 << 8;
//        // Passed separately to native.
//    }
//    if (ellipsis != null) {
//        result[0] |= 1 << 9;
//        // Passed separately to native.
//    }
//    if (locale != null) {
//        result[0] |= 1 << 10;
//        // Passed separately to native.
//    }
//    return result;
// }

// TODO(Migration/siyamed): native paragraph_style.h
// #ifndef LIB_TXT_SRC_PARAGRAPH_STYLE_H_
// #define LIB_TXT_SRC_PARAGRAPH_STYLE_H_
//
// #include <climits>
// #include <string>
//
// #include "font_style.h"
// #include "font_weight.h"
// #include "minikin/LineBreaker.h"
// #include "text_style.h"
//
// namespace txt {
//
//     enum class TextAlign {
//         left,
//         right,
//         center,
//         justify,
//         start,
//         end,
//     };
//
//     enum class TextDirection {
//         rtl,
//         ltr,
//     };
//
//     class ParagraphStyle {
//         public:
//         FontWeight font_weight = FontWeight::w400;
//         FontStyle font_style = FontStyle::normal;
//         std::string font_family = "";
//         double font_size = 14;
//
//         TextAlign text_align = TextAlign::start;
//         TextDirection text_direction = TextDirection::ltr;
//         size_t max_lines = std::numeric_limits<size_t>::max();
//         double line_height = 1.0;
//         std::u16string ellipsis;
//         std::string locale;
//
//         // Default strategy is kBreakStrategy_Greedy. Sometimes,
//         // kBreakStrategy_HighQuality will produce more desireable layouts (eg, very
//         // long words are more likely to be reasonably placed).
//         // kBreakStrategy_Balanced will balance between the two.
//         minikin::BreakStrategy break_strategy =
//         minikin::BreakStrategy::kBreakStrategy_Greedy;
//
//         TextStyle GetTextStyle() const;
//
//         bool unlimited_lines() const;
//         bool ellipsized() const;
//
//         // Return a text alignment value that is not dependent on the text direction.
//         TextAlign effective_align() const;
//     };
//
// }  // namespace txt
//
// #endif  // LIB_TXT_SRC_PARAGRAPH_STYLE_H_

//  TODO(Migration/siyamed): native paragraph_style.cc
// namespace txt {
//
//     TextStyle ParagraphStyle::GetTextStyle() const {
//         TextStyle result;
//         result.font_weight = font_weight;
//         result.font_style = font_style;
//         result.font_family = font_family;
//         result.font_size = font_size;
//         result.locale = locale;
//         result.height = line_height;
//         return result;
//     }
//
//     bool ParagraphStyle::unlimited_lines() const {
//         return max_lines == std::numeric_limits<size_t>::max();
//     };
//
//     bool ParagraphStyle::ellipsized() const {
//         return !ellipsis.empty();
//     }
//
//     TextAlign ParagraphStyle::effective_align() const {
//         if (text_align == TextAlign::start) {
//             return (text_direction == TextDirection::ltr) ? TextAlign::left
//             : TextAlign::right;
//         } else if (text_align == TextAlign::end) {
//             return (text_direction == TextDirection::ltr) ? TextAlign::right
//             : TextAlign::left;
//         } else {
//             return text_align;
//         }
//     }
//
// }  // namespace txt