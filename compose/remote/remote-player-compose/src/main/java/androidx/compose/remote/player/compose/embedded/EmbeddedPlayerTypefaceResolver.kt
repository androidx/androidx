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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Typeface
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver

/**
 * Default [TypefaceResolver] for the embedded player that handles built-in system fonts and custom
 * prefixes like "device:". For "google:" fonts without a GMS font resolver, it strips the prefix
 * and falls back to standard system font creation.
 */
internal object EmbeddedPlayerTypefaceResolver : TypefaceResolver {

    override fun resolve(
        fontType: Int,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        val baseTypeface =
            when (fontType) {
                1 -> Typeface.SANS_SERIF
                2 -> Typeface.SERIF
                3 -> Typeface.MONOSPACE
                else -> Typeface.DEFAULT
            }
        val typeface = createTypeface(baseTypeface, weight, italic)
        return SimpleFontInstance(typeface)
    }

    override fun resolve(
        fontName: String,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        val actualName =
            when {
                fontName.startsWith("device:") -> fontName.substring("device:".length)
                fontName.startsWith("google:") -> fontName.substring("google:".length)
                else -> fontName
            }

        val baseTypeface = Typeface.create(actualName, Typeface.NORMAL)
        val typeface = createTypeface(baseTypeface, weight, italic)
        return SimpleFontInstance(typeface)
    }

    private fun createTypeface(base: Typeface, weight: Int, italic: Boolean): Typeface {
        return Typeface.create(base, weight, italic)
    }

    private class SimpleFontInstance(private val typeface: Typeface) : FontInstance {
        override fun getTypeface(): Typeface = typeface

        override fun applyVariationSettings(tags: Array<String>, values: FloatArray): Typeface {
            return typeface
        }

        override fun setOnLoadedListener(listener: Runnable) {
            // Already loaded
        }
    }
}
