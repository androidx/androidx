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
import androidx.ui.painting.Shadow

/**
 * An opaque object that determines the size, position, and rendering of text.
 *
 * Creates a new TextStyle object.
 *
 * @param color The color to use when painting the text. If this is specified, `foreground` must be
 *              null.
 * @param decoration The decorations to paint near the text (e.g., an underline).
 * @param fontWeight The typeface thickness to use when painting the text (e.g., bold).
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italics).
 * @param fontFamily The name of the font to use when painting the text (e.g., Roboto).
 * @param fontSize The size of glyphs (in logical pixels) to use when painting the text.
 * @param fontFeatureSettings The advanced typography settings provided by font. The format is the same as the CSS font-feature-settings attribute:
 *                            https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
 * @param letterSpacing The amount of space (in EM) to add between each letter.
 * @param wordSpacing The amount of space (in logical pixels) to add at each sequence of white-space
 *                    (i.e. between each word). Only works on Android Q and above.
 * @param textBaseline The common baseline that should be aligned between this text span and its
 *                     parent text span, or, for the root text spans, with the line box.
 * @param baselineShift This parameter specifies how much the baseline is shifted from the current position.
 * @param height The height of this text span, as a multiple of the font size.
 * @param textGeometricTransform The geometric transformation applied the text.
 * @param locale The locale used to select region-specific glyphs.
 * @param background The background color for the text.
 * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight or
 *                      style cannot be found in the provided custom font family.
 * @param textIndent The amount of indentation applied to the affected paragraph. A paragraph is affected
 *                   if any of its character is covered by the TextSpan.
 * @param textAlign Specify how a paragraph is aligned.
 * @param shadow The shadow effect applied on the text.
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
    val baselineShift: BaselineShift? = null,
    val textGeometricTransform: TextGeometricTransform? = null,
    val height: Float? = null,
    val locale: Locale? = null,
    val background: Color? = null,
    val fontSynthesis: FontSynthesis? = null,
    val textIndent: TextIndent? = null,
    val textAlign: TextAlign? = null,
    val shadow: Shadow? = null
)