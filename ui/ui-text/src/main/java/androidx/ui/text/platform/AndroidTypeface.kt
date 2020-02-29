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

package androidx.ui.text.platform

import android.content.Context
import androidx.ui.text.Typeface
import androidx.ui.text.font.DefaultFontFamily
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontListFontFamily
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontSynthesis
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.GenericFontFamily
import android.graphics.Typeface as NativeTypeface

/**
 * An interface of Android specific Typeface.
 */
internal interface AndroidTypeface : Typeface {
    /**
     * Returns the Android's native Typeface to be able use for given parameters.
     *
     * @param fontWeight A weight to be used for drawing text.
     * @param fontStyle A style to be used for drawing text.
     * @param synthesis An synthesis option for drawing text.
     *
     * @return the Android native Typeface which has closest style to the given parameter.
     */
    fun getNativeTypeface(
        fontWeight: FontWeight,
        fontStyle: FontStyle,
        synthesis: FontSynthesis
    ): NativeTypeface
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
internal fun androidTypefaceFromFontFamily(
    context: Context,
    fontFamily: FontFamily,
    necessaryStyles: List<Pair<FontWeight, FontStyle>>? = null
): AndroidTypeface {
    return when (fontFamily) {
        is FontListFontFamily -> AndroidFontListTypeface(fontFamily, context, necessaryStyles)
        is GenericFontFamily -> AndroidGenericFontFamilyTypeface(fontFamily)
        is DefaultFontFamily -> AndroidDefaultTypeface()
    }
}