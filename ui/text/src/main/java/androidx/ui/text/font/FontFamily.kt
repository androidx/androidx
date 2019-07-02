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

import android.content.Context

/**
 * Defines a font family. It can be constructed via a generic font family such as serif, sans-serif
 * (i.e. FontFamily("sans-serif"). It can also be constructed by a set of custom fonts.
 *
 * For example you can defined a [FontFamily] based on custom font files:
 * <code>
 * FontFamily(
 *      Font(name="my_font_400_regular.ttf", weight=400, style=FontStyle.Normal),
 *      Font(name="my_font_400_italic.ttf", weight=400, style=FontStyle.Italic)
 * )
 * </code>
 */
data class FontFamily private constructor(val genericFamily: String?, val fonts: List<Font>) :
// TODO(migration/siyamed): ADD COMMON GENERIC FONT FAMILY NAMES ENUMS
    List<Font> by fonts {

    /**
     * Constructs a generic font family.
     *
     * @param genericFamily the generic font family name to be used. i.e. "sans-serif".
     */
    constructor(genericFamily: String) : this(genericFamily, listOf())

    /**
     * Construct a font family that contains a single custom font file.
     *
     * @param font font file
     */
    constructor(font: Font) : this(null, listOf(font))

    /**
     * Construct a font family that contains list of custom font files.
     *
     * @param fonts list of font files
     */
    constructor(fonts: List<Font>) : this(null, fonts)

    /**
     * Construct a font family that contains list of custom font files.
     *
     * @param fonts list of font files
     */
    constructor(vararg fonts: Font) : this(null, fonts.asList())

    // TODO(migration/siyamed) this should be in an subclass or platform actual class
    lateinit var context: Context

    init {
        assert(genericFamily != null || fonts.size > 0) {
            "Either genericFamily or at least one font should be passed to FontFamily"
        }
        assert(fonts.distinctBy { Pair(it.weight, it.style) }.size == fonts.size) {
            "There cannot be two fonts with the same FontWeight and FontStyle in the same " +
                    "FontFamily"
        }
    }
}
