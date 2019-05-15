/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.window.Locale
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.Shadow

/**
 * An opaque object that determines the size, position, and rendering of text.
 *
 * Creates a new TextStyle object.
 *
 * * `color`: The color to use when painting the text. If this is specified, `foreground` must be
 *             null.
 * * `decoration`: The decorations to paint near the text (e.g., an underline).
 * * `fontWeight`: The typeface thickness to use when painting the text (e.g., bold).
 * * `fontStyle`: The typeface variant to use when drawing the letters (e.g., italics).
 * * `fontFamily`: The name of the font to use when painting the text (e.g., Roboto).
 * * `fontSize`: The size of glyphs (in logical pixels) to use when painting the text.
 * * `fontFeatureSettings`: The advanced typography settings provided by font. The format is the same as the CSS font-feature-settings attribute:
 * *  https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
 * * `letterSpacing`: The amount of space (in EM) to add between each letter.
 * * `wordSpacing`: The amount of space (in logical pixels) to add at each sequence of white-space
 *                  (i.e. between each word). Only works on Android Q and above.
 * * `textBaseline`: The common baseline that should be aligned between this text span and its
 *                   parent text span, or, for the root text spans, with the line box.
 * * `baselineShift`: This parameter specifies how much the baseline is shifted from the current position.
 * * `height`: The height of this text span, as a multiple of the font size.
 * * `textGeometricTransform`: The geometric transformation applied the text.
 * * `locale`: The locale used to select region-specific glyphs.
 * * `background`: The background color for the text.
 * * `foreground`: The paint used to draw the text. If this is specified, `color` must be null.
 * * `fontSynthesis`: Whether to synthesize font weight and/or style when the requested weight or
 *                    style cannot be found in the provided custom font family.
 * * `textIndent`: The amount of indentation applied to the affected paragraph. A paragraph is affected
 *                 if any of its character is covered by the TextSpan.
 * * `shadow`: The shadow effect applied on the text.
 */
data class TextStyle constructor(
    val color: Color? = null,
    val decoration: TextDecoration? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = null,
    val fontSize: Float? = null,
    val fontFeatureSettings: String? = null,
    val letterSpacing: Float? = null,
    val wordSpacing: Float? = null,
    val textBaseline: TextBaseline? = null,
    val baselineShift: BaselineShift? = null,
    val textGeometricTransform: TextGeometricTransform? = null,
    val height: Float? = null,
    val locale: Locale? = null,
    // TODO(Migration/haoyuchang): background is changed to color from paint.
    val background: Color? = null,
    val foreground: Paint? = null,
    val fontSynthesis: FontSynthesis? = null,
    val textIndent: TextIndent? = null,
    val shadow: Shadow? = null
) {
    init {
        assert(color == null || foreground == null) {
            "Cannot provide both a color and a foreground\n" +
                "The color argument is just a shorthand for " +
                "'foreground: new Paint()..color = color'."
        }
    }
}

// TODO(Migration/siyamed) Remove, Native defaults
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
