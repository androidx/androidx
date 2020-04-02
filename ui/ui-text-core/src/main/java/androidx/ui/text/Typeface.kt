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

package androidx.ui.text

import android.content.Context
import androidx.ui.text.font.DefaultFontFamily
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontListFontFamily
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.GenericFontFamily
import androidx.ui.text.font.LoadedFontFamily
import androidx.ui.text.platform.AndroidDefaultTypeface
import androidx.ui.text.platform.AndroidFontListTypeface
import androidx.ui.text.platform.AndroidGenericFontFamilyTypeface

/**
 * A class that can be used for changing the font used in text.
 */
interface Typeface {
    /**
     * The font family used for creating this Typeface
     */
    val fontFamily: FontFamily
}

/**
 * Android specific Typeface builder function from FontFamily.
 *
 * You can pass necessaryStyles for loading only specific styles. The font style matching happens
 * only with the loaded Typeface.
 *
 * This function caches the internal native Typeface but always create the new Typeface object.
 * Caller should cache if necessary.
 *
 * @param context the context to be used for loading Typeface.
 * @param fontFamily the font family to be loaded
 * @param necessaryStyles optional style filter for loading subset of fontFamily. null means load
 *                        all fonts in fontFamily.
 * @return A loaded Typeface.
 */
fun typefaceFromFontFamily(
    context: Context,
    fontFamily: FontFamily,
    necessaryStyles: List<Pair<FontWeight, FontStyle>>? = null
): Typeface {
    return when (fontFamily) {
        is FontListFontFamily -> AndroidFontListTypeface(fontFamily, context, necessaryStyles)
        is GenericFontFamily -> AndroidGenericFontFamilyTypeface(fontFamily)
        is DefaultFontFamily -> AndroidDefaultTypeface()
        is LoadedFontFamily -> fontFamily.typeface
    }
}