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

/**
 * Defines a font to be used while rendering text.
 *
 * @sample androidx.ui.text.samples.CustomFontFamilySample
 *
 * @param name The name of the font file in font resources. i.e. "myfont.ttf".
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.ui.text.SpanStyle].
 *
 * @see FontFamily
 */
data class Font(
    val name: String,
    val weight: FontWeight = FontWeight.Normal,
    val style: FontStyle = FontStyle.Normal
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
        fun load(font: Font): Any
    }
}

/**
 * Create a [FontFamily] from this single [Font].
 */
fun Font.asFontFamily(): FontFamily {
    return FontFamily(this)
}
