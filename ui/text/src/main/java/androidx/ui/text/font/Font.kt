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

package androidx.ui.text.font

// TODO(Migration/siyamed): might need nullable defaults for FontWeight and FontStyle which
// would mean read the weight and style from the font.
/**
 * Defines a Font file to be used while rendering text.
 */
data class Font(
    /**
     * Name of the font file in font resources. i.e. "myfont.ttf".
     */
    val name: String,

    /**
     * Weight of the font. Use while matching a font to a font request that is given in a
     * [TextStyle].
     */
    val weight: FontWeight = FontWeight.normal,

    /**
     * Style of the font. Use while matching a font to a font request that is given in a
     * [TextStyle].
     */
    val style: FontStyle = FontStyle.Normal,

    // TODO(Migration/siyamed): implement integration and add tests
    /**
     * Sets the index of the font collection. If the font is not a font collection the value should
     * not be changed.
     */
    val ttcIndex: Int = 0,

    // TODO(Migration/siyamed): implement integration and add tests
    // https://docs.microsoft.com/en-us/typography/opentype/spec/otvaroverview
    // not sure if this would be here or in the widget properties similar to TextStyle.fontWeight
    // CSS says: "These descriptors define initial settings that apply when the font defined by an
    // @font-face rule is rendered. They do not affect font selection"
    // https://www.w3.org/TR/css-fonts-4/#ref-for-descdef-font-face-font-variation-settings
    /**
     * Sets TrueType or OpenType font variation settings. The settings string is constructed from
     * multiple pairs of axis tag and style values. The axis tag must contain four ASCII characters
     * and must be wrapped with single quotes (U+0027) or double quotes (U+0022). Axis strings that
     * are longer or shorter than four characters, or contain characters outside of U+0020..U+007E
     * are invalid. If a specified axis name is not defined in the font, the settings will be
     * ignored.
     *
     * Examples:
     *
     * * Set font width to 150: "'wdth' 150"
     * * Set the font slant to 20 degrees and ask for italic style: "'slnt' 20, 'ital' 1"
     */
    val fontVariationSettings: String = ""
) {
    init {
        assert(name.isNotEmpty()) { "Font name cannot be empty" }
    }
}

/**
 * Create a [FontFamily] from this single font.
 */
fun Font.asFontFamily(): FontFamily {
    return FontFamily(this)
}
