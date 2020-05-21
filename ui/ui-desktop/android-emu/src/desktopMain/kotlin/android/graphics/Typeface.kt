/*
 * Copyright 2020 The Android Open Source Project
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

package android.graphics

private const val NORMAL_FONT_NAME = "NotoSans-Regular"
private const val BOLD_FONT_NAME = "NotoSans-Bold"
private const val ITALIC_FONT_NAME = "NotoSans-Italic"
private const val BOLD_ITALIC_FONT_NAME = "NotoSans-BoldItalic"

class Typeface(val skijaTypeface: org.jetbrains.skija.Typeface) {
    companion object {
        // TODO: Skija should make it possible to make fonts from non-file in-memory source
        val NORMAL_TYPEFACE = Typeface(org.jetbrains.skija.Typeface.makeFromFile(
            getFontPathAsString(NORMAL_FONT_NAME)))
        val BOLD_TYPEFACE = Typeface(org.jetbrains.skija.Typeface.makeFromFile(
            getFontPathAsString(BOLD_FONT_NAME)))
        val ITALIC_TYPEFACE = Typeface(org.jetbrains.skija.Typeface.makeFromFile(
            getFontPathAsString(ITALIC_FONT_NAME)))
        val BOLD_ITALIC_TYPEFACE = Typeface(org.jetbrains.skija.Typeface.makeFromFile(
            getFontPathAsString(BOLD_ITALIC_FONT_NAME)))

        @JvmField
        val DEFAULT = NORMAL_TYPEFACE

        @JvmField
        val NORMAL = 0x0

        @JvmField
        val BOLD = 0x1

        @JvmField
        val ITALIC = 0x2

        @JvmField
        val BOLD_ITALIC = 0x3

        @JvmStatic
        fun defaultFromStyle(style: Int): Typeface = when (style) {
            BOLD -> BOLD_TYPEFACE
            ITALIC -> ITALIC_TYPEFACE
            BOLD_ITALIC -> BOLD_ITALIC_TYPEFACE
            else -> DEFAULT
        }
    }
}

fun getFontPathAsString(font: String): String {
    return Typeface::class.java.getClassLoader().getResource("$font.ttf").getFile()
}
