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

package androidx.ui.matchers

import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.FontTestData
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * Matcher to check if a given [Typeface] has a given [FontWeight] and [FontStyle]. Since
 * [Typeface] does not contain the required information before API 28, it uses the set of specific
 * fonts to infer which type of font was loaded. Check [DEFINED_CHARACTERS] and [FontTestData] for
 * the set of fonts designed for this class.
 *
 * Each font contains [a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r] characters and [wide,narrow] glyph
 * types. Each font file includes one character that is represented with wide glyph, and others
 * with narrow. This is used in the tests in order to differentiate the font that is loaded for
 * a specific weight/style in the FontFamily.
 *
 * The size difference between wide and narrow glyph is 3 (wide = 3 * narrow).
 *
 * - 200 italic has [a] with wide glyph
 * - 200 regular has [b] with wide glyph
 * - ...
 * - 900 italic has [q] with wide glyph
 * - 900 regular has [r] with wide glyph
 *
 * @param [fontWeight] expected [FontWeight]
 * @param [fontStyle] expected [FontStyle]
 */
class IsTypefaceOf(
    val fontWeight: FontWeight,
    val fontStyle: FontStyle
) : BaseMatcher<Typeface>() {

    private val DEFINED_CHARACTERS = arrayOf(
        CharacterInfo('a', FontWeight.w100, FontStyle.Italic),
        CharacterInfo('b', FontWeight.w100, FontStyle.Normal),
        CharacterInfo('c', FontWeight.w200, FontStyle.Italic),
        CharacterInfo('d', FontWeight.w200, FontStyle.Normal),
        CharacterInfo('e', FontWeight.w300, FontStyle.Italic),
        CharacterInfo('f', FontWeight.w300, FontStyle.Normal),
        CharacterInfo('g', FontWeight.w400, FontStyle.Italic),
        CharacterInfo('h', FontWeight.w400, FontStyle.Normal),
        CharacterInfo('i', FontWeight.w500, FontStyle.Italic),
        CharacterInfo('j', FontWeight.w500, FontStyle.Normal),
        CharacterInfo('k', FontWeight.w600, FontStyle.Italic),
        CharacterInfo('l', FontWeight.w600, FontStyle.Normal),
        CharacterInfo('m', FontWeight.w700, FontStyle.Italic),
        CharacterInfo('n', FontWeight.w700, FontStyle.Normal),
        CharacterInfo('o', FontWeight.w800, FontStyle.Italic),
        CharacterInfo('p', FontWeight.w800, FontStyle.Normal),
        CharacterInfo('q', FontWeight.w900, FontStyle.Italic),
        CharacterInfo('r', FontWeight.w900, FontStyle.Normal)
    )

    private val FONT_SIZE = 10f

    private fun getPaint(typeface: Typeface): TextPaint {
        val paint = TextPaint()
        paint.typeface = typeface
        paint.textSize = FONT_SIZE
        return paint
    }

    private fun isSelectedFont(typeface: Typeface, character: Char): Boolean {
        val string = Character.toString(character)
        val measuredWidth = getPaint(typeface).measureText(string)
        // wide glyphs are 3 times the width of narrow glyphs. Therefore for the selected character
        // if the right font is selected the width should be 3 times the font size.
        return java.lang.Float.compare(measuredWidth, FONT_SIZE * 3) == 0
    }

    override fun matches(typeface: Any?): Boolean {
        if (typeface == null || typeface !is Typeface) return false

        val charInfo = DEFINED_CHARACTERS.find {
            it.fontWeight == fontWeight && it.fontStyle == fontStyle
        }!!

        val isSelectedFont = isSelectedFont(typeface, charInfo.character)

        if (Build.VERSION.SDK_INT >= 28) {
            return isSelectedFont && typeface.weight == fontWeight.weight
            // cannot check typeface.isItalic == (fontStyle == FontStyle.Italic) since it is for
            // fake italic, and for cases where synthesis is disable this does not give correct
            // signal
        } else {
            return isSelectedFont
        }
    }

    override fun describeMismatch(typeface: Any?, description: Description) {
        if (typeface == null || typeface !is Typeface) {
            super.describeMismatch(typeface, description)
        } else {
            var selectedFont = DEFINED_CHARACTERS.find {
                isSelectedFont(typeface, it.character)
            }

            description.appendText("was ")
            if (selectedFont == null) {
                description.appendValue("unknown")
            } else {
                description.appendValue(selectedFont)
            }
        }
    }

    override fun describeTo(description: Description) {
        description.appendText(toString(fontWeight, fontStyle))
    }
}

private class CharacterInfo(
    val character: Char,
    val fontWeight: FontWeight,
    val fontStyle: FontStyle
) {
    override fun toString(): String {
        return toString(fontWeight, fontStyle)
    }
}

private fun toString(fontWeight: FontWeight, fontStyle: FontStyle): String {
    return "{fontWeight: $fontWeight, fontStyle: $fontStyle}"
}