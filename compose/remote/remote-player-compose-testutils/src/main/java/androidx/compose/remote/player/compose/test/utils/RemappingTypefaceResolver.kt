/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.test.utils

import android.graphics.Typeface
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver

/** TypefaceResolver that remaps standard types (0-3) or names to other names. */
public class RemappingTypefaceResolver(private val next: TypefaceResolver) : TypefaceResolver {
    private val typeRemap: MutableIntObjectMap<String> = mutableIntObjectMapOf()
    private val nameRemap: MutableMap<String, String> = HashMap()

    /** Remaps a standard font type (0-3) to a string name. */
    public fun remapType(fontType: Int, mappedName: String) {
        typeRemap[fontType] = mappedName
    }

    /** Remaps a font name to another font name. */
    public fun remapName(fontName: String, mappedName: String) {
        nameRemap[fontName] = mappedName
    }

    override fun resolve(
        fontType: Int,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        val mappedName = typeRemap[fontType]
        if (mappedName != null) {
            return next.resolve(
                mappedName,
                weight,
                italic,
                fallbackTypeface,
                fallbackWeight,
                fallbackItalic,
            )
        }
        return next.resolve(
            fontType,
            weight,
            italic,
            fallbackTypeface,
            fallbackWeight,
            fallbackItalic,
        )
    }

    override fun resolve(
        fontName: String,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        val mappedName = nameRemap[fontName]
        if (mappedName != null) {
            return next.resolve(
                mappedName,
                weight,
                italic,
                fallbackTypeface,
                fallbackWeight,
                fallbackItalic,
            )
        }
        return next.resolve(
            fontName,
            weight,
            italic,
            fallbackTypeface,
            fallbackWeight,
            fallbackItalic,
        )
    }
}
