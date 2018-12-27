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
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import androidx.collection.LruCache
import androidx.core.content.res.ResourcesCompat
import androidx.ui.engine.text.FontStyle
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
        val fontStyle: FontStyle
    )

    companion object {
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
        fontStyle: FontStyle = FontStyle.normal
    ): Typeface {
        val cacheKey = CacheKey(fontFamily, fontWeight, fontStyle)
        val cachedTypeface = typefaceCache.get(cacheKey)
        if (cachedTypeface != null) return cachedTypeface

        val typeface = if (fontFamily != null && fontFamily.isNotEmpty()) {
            create(
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                context = fontFamily.context
            )
        } else {
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
     * [fontWeight] is used to define the tickness of the Typeface. Before Android 28 font weight
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
                fontStyle == FontStyle.italic
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
     * @param fontWeight the font weight to create the typeface in
     * @param fontStyle the font style to create the typeface in
     * @param fontFamily [FontFamily] that contains the list of [Font]s
     * @param context [Context] instance
     * @param resources [Resources] instance
     */
    private fun create(
        fontStyle: FontStyle = FontStyle.normal,
        fontWeight: FontWeight = FontWeight.normal,
        fontFamily: FontFamily,
        context: Context
    ): Typeface {
        // TODO(Migration/siyamed): add genericFontFamily : String? = null for fallback
        // TODO(Migration/siyamed): add support for multiple font families
        // TODO(Migration/siyamed): add font synthesis

        val font = fontMatcher.matchFont(fontFamily, fontWeight, fontStyle)

        // TODO(Migration/siyamed): This is an expensive operation and discouraged in the API Docs
        // remove when alternative resource loading system is defined.
        val resId = context.resources.getIdentifier(
            font.name.substringBefore("."),
            "font",
            context.packageName
        )

        val typeface = ResourcesCompat.getFont(context, resId)
            ?: throw IllegalStateException(
                "Cannot create Typeface from $font with resource id $resId"
            )

        // TODO(Migration/siyamed): This part includes synthesis, recheck when it is implemented
        val result = if (Build.VERSION.SDK_INT < 28) {
            val targetStyle = getTypefaceStyle(fontWeight, fontStyle)
            if (targetStyle != typeface.style) {
                Typeface.create(typeface, targetStyle)
            } else {
                typeface
            }
        } else {
            if (typeface.weight != fontWeight.weight ||
                typeface.isItalic != (fontStyle == FontStyle.italic)
            ) {
                Typeface.create(typeface, fontWeight.weight, fontStyle == FontStyle.italic)
            } else {
                typeface
            }
        }

        return result
    }

    /**
     * Convert given [FontWeight] and [FontStyle] to one of [Typeface.NORMAL], [Typeface.BOLD],
     * [Typeface.ITALIC], [Typeface.BOLD_ITALIC]. This function should be called for API < 28
     * since at those API levels system does not accept [FontWeight].
     */
    fun getTypefaceStyle(fontWeight: FontWeight, fontStyle: FontStyle): Int {
        // This code accepts anything at and above 600 to be bold.
        // 600 comes from FontFamily.cpp#computeFakery function in minikin
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
