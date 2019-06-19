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

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.collection.LruCache
import androidx.core.content.res.ResourcesCompat
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontSynthesis
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.font.Font
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.text.font.FontMatcher

/**
 * Creates a Typeface based on generic font family or a custom [FontFamily].
 *
 * @param fontMatcher [FontMatcher] class to be used to match given [FontWeight] and [FontStyle]
 *                    constraints to select a [Font] from a [FontFamily]
 */
// TODO(Migration/siyamed): font matcher should be at an upper layer such as Paragraph, whoever
// will call TypefaceAdapter can know about a single font
internal open class TypefaceAdapter constructor(
    val fontMatcher: FontMatcher = FontMatcher()
) {

    data class CacheKey(
        val fontFamily: FontFamily? = null,
        val fontWeight: FontWeight,
        val fontStyle: FontStyle,
        val fontSynthesis: FontSynthesis
    )

    companion object {
        // Accept FontWeights at and above 600 to be bold. 600 comes from FontFamily.cpp#computeFakery
        // function in minikin
        private val ANDROID_BOLD = FontWeight.w600

        // 16 is a random number and is not based on any strong logic
        val typefaceCache = LruCache<CacheKey, Typeface>(16)
    }

    /**
     * Creates a Typeface based on the [fontFamily] and the selection constraints [fontStyle] and
     * [fontWeight].
     *
     * @param fontFamily [FontFamily] that defines the system family or a set of custom fonts
     * @param fontWeight the font weight to create the typeface in
     * @param fontStyle the font style to create the typeface in
     */
    open fun create(
        fontFamily: FontFamily? = null,
        fontWeight: FontWeight = FontWeight.normal,
        fontStyle: FontStyle = FontStyle.Normal,
        fontSynthesis: FontSynthesis = FontSynthesis.All
    ): Typeface {
        val cacheKey = CacheKey(fontFamily, fontWeight, fontStyle, fontSynthesis)
        val cachedTypeface = typefaceCache.get(cacheKey)
        if (cachedTypeface != null) return cachedTypeface

        val typeface = if (fontFamily != null && fontFamily.isNotEmpty()) {
            create(
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis,
                context = fontFamily.context
            )
        } else {
            // there is no option to control fontSynthesis in framework for system fonts
            create(
                genericFontFamily = fontFamily?.genericFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle
            )
        }

        // For system Typeface, on different framework versions Typeface might not be cached,
        // therefore it is safer to cache this result on our code and the cost is minimal.
        typefaceCache.put(cacheKey, typeface)

        return typeface
    }

    /**
     * Creates a Typeface object based on the system installed fonts. [genericFontFamily] is used
     * to define the main family to create the Typeface such as serif, sans-serif.
     *
     * [fontWeight] is used to define the thickness of the Typeface. Before Android 28 font weight
     * cannot be defined therefore this function assumes anything at and above [FontWeight.w600]
     * is bold and any value less than [FontWeight.w600] is normal.
     *
     * @param genericFontFamily generic font family name such as serif, sans-serif
     * @param fontWeight the font weight to create the typeface in
     * @param fontStyle the font style to create the typeface in
     */
    private fun create(
        genericFontFamily: String? = null,
        fontWeight: FontWeight = FontWeight.normal,
        fontStyle: FontStyle = FontStyle.Normal
    ): Typeface {
        if (fontStyle == FontStyle.Normal &&
            fontWeight == FontWeight.normal &&
            genericFontFamily.isNullOrEmpty()
        ) {
            return Typeface.DEFAULT
        }

        // TODO(Migration/siyamed): ideally we should not have platform dependent if's here.
        // will think more and move to ui-text later.
        val result = if (Build.VERSION.SDK_INT < 28) {
            val targetStyle = getTypefaceStyle(fontWeight, fontStyle)
            if (genericFontFamily.isNullOrEmpty()) {
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

            Typeface.create(
                familyTypeface,
                fontWeight.weight,
                fontStyle == FontStyle.Italic
            )
        }

        return result
    }

    /**
     * Creates a [Typeface] based on the [fontFamily] the requested [FontWeight], [FontStyle]. If
     * the requested [FontWeight] and [FontStyle] exists in the [FontFamily], the exact match is
     * returned. If it does not, the matching is defined based on CSS Font Matching. See
     * [FontMatcher] for more information.
     *
     * @param fontStyle the font style to create the typeface in
     * @param fontWeight the font weight to create the typeface in
     * @param fontFamily [FontFamily] that contains the list of [Font]s
     * @param context [Context] instance
     * @param fontSynthesis [FontSynthesis] which attributes of the font family to synthesize
     *        custom fonts for if they are not already present in the font family
     */
    private fun create(
        fontStyle: FontStyle = FontStyle.Normal,
        fontWeight: FontWeight = FontWeight.normal,
        fontFamily: FontFamily,
        context: Context,
        fontSynthesis: FontSynthesis = FontSynthesis.All
    ): Typeface {
        // TODO(Migration/siyamed): add genericFontFamily : String? = null for fallback
        // TODO(Migration/siyamed): add support for multiple font families

        val font = fontMatcher.matchFont(fontFamily, fontWeight, fontStyle)

        // TODO(Migration/siyamed): This is an expensive operation and discouraged in the API Docs
        // remove when alternative resource loading system is defined.
        val resId = context.resources.getIdentifier(
            font.name.substringBefore("."),
            "font",
            context.packageName
        )

        val typeface = try {
            ResourcesCompat.getFont(context, resId)
        } catch (e: Throwable) {
            null
        }

        if (typeface == null) {
            throw IllegalStateException(
                "Cannot create Typeface from $font with resource id $resId"
            )
        }

        val loadedFontIsSameAsRequest = fontWeight == font.weight && fontStyle == font.style
        // if synthesis is not requested or there is an exact match we don't need synthesis
        if (fontSynthesis == FontSynthesis.None || loadedFontIsSameAsRequest) {
            return typeface
        }

        return synthesize(typeface, font, fontWeight, fontStyle, fontSynthesis)
    }

    fun synthesize(
        typeface: Typeface,
        font: Font,
        fontWeight: FontWeight,
        fontStyle: FontStyle,
        fontSynthesis: FontSynthesis
    ): Typeface {

        val synthesizeWeight = fontSynthesis.isWeightOn &&
                (fontWeight >= ANDROID_BOLD && font.weight < ANDROID_BOLD)

        val synthesizeStyle = fontSynthesis.isStyleOn && fontStyle != font.style

        if (!synthesizeStyle && !synthesizeWeight) return typeface

        return if (Build.VERSION.SDK_INT < 28) {
            val targetStyle = getTypefaceStyle(
                isBold = synthesizeWeight,
                isItalic = synthesizeStyle && fontStyle == FontStyle.Italic)
            Typeface.create(typeface, targetStyle)
        } else {
            val finalFontWeight = if (synthesizeWeight) {
                // if we want to synthesize weight, we send the requested fontWeight
                fontWeight.weight
            } else {
                // if we do not want to synthesize weight, we keep the loaded font weight
                font.weight.weight
            }

            val finalFontStyle = if (synthesizeStyle) {
                // if we want to synthesize style, we send the requested fontStyle
                fontStyle == FontStyle.Italic
            } else {
                // if we do not want to synthesize style, we keep the loaded font style
                font.style == FontStyle.Italic
            }

            Typeface.create(typeface, finalFontWeight, finalFontStyle)
        }
    }

    /**
     * Convert given [FontWeight] and [FontStyle] to one of [Typeface.NORMAL], [Typeface.BOLD],
     * [Typeface.ITALIC], [Typeface.BOLD_ITALIC]. This function should be called for API < 28
     * since at those API levels system does not accept [FontWeight].
     */
    fun getTypefaceStyle(fontWeight: FontWeight, fontStyle: FontStyle): Int {
        return getTypefaceStyle(fontWeight >= ANDROID_BOLD, fontStyle == FontStyle.Italic)
    }

    fun getTypefaceStyle(isBold: Boolean, isItalic: Boolean): Int {
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
