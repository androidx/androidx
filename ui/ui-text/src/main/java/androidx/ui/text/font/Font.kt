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

// TODO(siyamed): might need nullable defaults for FontWeight and FontStyle which
//  would mean read the weight and style from the font.
/**
 * Defines a font to be used while rendering text.
 *
 * @sample androidx.ui.text.samples.CustomFontFamilySample
 *
 * @param name The name of the font file in font resources. i.e. "myfont.ttf".
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a `TextStyle`.
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a `TextStyle`.
 * @param ttcIndex The index of the font collection. If the font file is not a font collection the
 * value should not be changed
 * @param fontVariationSettings The TrueType or OpenType font variation settings. Valid only when
 * the font that is used is a variable font. The settings string is constructed from multiple
 * pairs of axis tag and style values. The axis tag must contain four ASCII characters and must
 * be wrapped with single quotes (U+0027) or double quotes (U+0022). Axis strings that are longer
 * or shorter than four characters, or contain characters outside of U+0020..U+007E are invalid.
 * If a specified axis name is not defined in the font, the settings will be ignored.
 * Examples: to set font width to 150: "'wdth' 150", to set the font slant to 20 degrees and ask
 * for italic style: "'slnt' 20, 'ital' 1".
 *
 * @see [FontFamily]
 */
data class Font(
    val name: String,
    val weight: FontWeight = FontWeight.Normal,
    val style: FontStyle = FontStyle.Normal,
    // TODO(siyamed): implement integration and add tests
    val ttcIndex: Int = 0,
    // TODO(siyamed): implement integration and add tests
    // https://docs.microsoft.com/en-us/typography/opentype/spec/otvaroverview
    // not sure if this would be here or in the composable properties similar to TextStyle.fontWeight
    // CSS says: "These descriptors define initial settings that apply when the font defined by an
    // @font-face rule is rendered. They do not affect font selection"
    // https://www.w3.org/TR/css-fonts-4/#ref-for-descdef-font-face-font-variation-settings
    val fontVariationSettings: String = ""
) {
    init {
        assert(name.isNotEmpty()) { "Font name cannot be empty" }
    }

    /**
     * Interface used to load a font resource.
     */
    interface ResourceLoader {
        /**
         * Loads resource represented by the [Font] object.
         *
         * @param font [Font] to be loaded
         * @return platform specific font
         */
        // TODO(siyamed) when expect/actual is enabled return a typealias
        fun load(font: Font): Any
    }
}

/**
 * Create a [FontFamily] from this single [Font].
 */
fun Font.asFontFamily(): FontFamily {
    return FontFamily(this)
}
