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

package androidx.ui.engine.text.platform

import android.graphics.Typeface
import android.os.Build
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.font.FontFamily

/**
 * Creates a Typeface based on generic font family or a custom [FontFamily].
 */
internal class TypefaceAdapter {

    /**
     * Creates a Typeface object based on the system installed fonts. [genericFontFamily] is used
     * to define the main family to create the Typeface such as serif, sans-serif.
     *
     * [fontWeight] is used to define the tickness of the Typeface. Before Android 28 font weight
     * cannot be defined therefore this function assumes anything at and above [FontWeight.w600]
     * is bold and any value less than [FontWeight.w600] is normal.
     *
     * @param genericFontFamily generic font family name such as serif, sans-serif
     * @param fontWeight the font weight to create the typeface in
     * @param fontStyle the font style to create the typeface in
     */
    fun create(
        genericFontFamily: String? = null,
        fontWeight: FontWeight = FontWeight.normal,
        fontStyle: FontStyle = FontStyle.normal
    ): Typeface {
        if (fontStyle == FontStyle.normal &&
            fontWeight == FontWeight.normal &&
            genericFontFamily.isNullOrEmpty()
        ) {
            return Typeface.DEFAULT
        }

        // TODO(Migration/siyamed): ideally we should not have platform dependent if's here.
        // will think more and move to ui-text later.
        if (Build.VERSION.SDK_INT < 28) {
            val targetStyle = getTypefaceStyle(fontWeight, fontStyle)
            return if (genericFontFamily.isNullOrEmpty()) {
                Typeface.defaultFromStyle(targetStyle)
            } else {
                Typeface.create(genericFontFamily, targetStyle)
            }
        } else {
            val familyTypeface: Typeface
            if (genericFontFamily == null) {
                familyTypeface = Typeface.DEFAULT
            } else {
                familyTypeface = Typeface.create(genericFontFamily, Typeface.NORMAL)
            }

            return Typeface.create(
                familyTypeface,
                fontWeight.weight,
                fontStyle == FontStyle.italic
            )
        }
    }

    /**
     * Convert given [FontWeight] and [FontStyle] to one of [Typeface.NORMAL], [Typeface.BOLD],
     * [Typeface.ITALIC], [Typeface.BOLD_ITALIC]. This function should be called for API < 28
     * since at those API levels system does not accept [FontWeight].
     */
    fun getTypefaceStyle(fontWeight: FontWeight, fontStyle: FontStyle): Int {
        // This code accepts anything at and above 600 to be bold.
        val isBold = fontWeight.weight >= 600
        val isItalic = fontStyle == FontStyle.italic
        return if (isItalic && isBold) {
            Typeface.BOLD_ITALIC
        } else if (isBold) {
            Typeface.BOLD
        } else if (isItalic) {
            Typeface.ITALIC
        } else {
            Typeface.NORMAL
        }
    }
}