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

package androidx.compose.remote.player.compose.embedded.resources

import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.compose.embedded.doc.SnapshotRemoteComposeState
import androidx.compose.ui.text.font.FontFamily as ComposeFontFamily
import androidx.compose.ui.text.font.Typeface as ComposeTypeface
import java.nio.ByteBuffer

/**
 * Returns the typeface for a font embedded in the document (`FontData`) with id [fontId], or null
 * if trusted content is disabled or [fontId] is not an embedded font.
 *
 * Like the View player's `DefaultTypefaceResolver`, the font is built with a system sans-serif
 * fallback and the variation axes in [variationSettings] (in the format accepted by
 * [Font.Builder.setFontVariationSettings]). Typefaces are cached per document font, weight, style
 * and axis values, so per-frame text drawing does not rebuild native font objects.
 */
internal fun documentFontTypeface(
    context: RemoteContext,
    fontId: Int,
    weight: Int,
    italic: Boolean,
    variationSettings: String?,
): Typeface? = documentFont(context, fontId, weight, italic, variationSettings)?.typeface

/**
 * Returns a Compose [ComposeFontFamily] for the document font with id [fontId], reusing the same
 * instance for identical inputs so that text styles built from it stay equal across recompositions.
 * See [documentFontTypeface].
 */
internal fun documentFontFamily(
    context: RemoteContext,
    fontId: Int,
    weight: Int,
    italic: Boolean,
    variationSettings: String?,
): ComposeFontFamily? = documentFont(context, fontId, weight, italic, variationSettings)?.fontFamily

private fun documentFont(
    context: RemoteContext,
    fontId: Int,
    weight: Int,
    italic: Boolean,
    variationSettings: String?,
): DocumentFont? {
    if ((context.mRemoteComposeState as? SnapshotRemoteComposeState)?.trustedContent != true) {
        return null
    }
    val info = context.getObject(fontId) as? RemoteContext.FontInfo ?: return null
    return context.typefaceCache.documentFontCache(info).get(weight, italic, variationSettings)
}

/** A typeface built from a document font, and the Compose font family wrapping it. */
internal class DocumentFont(val typeface: Typeface) {
    val fontFamily: ComposeFontFamily by
        lazy(LazyThreadSafetyMode.NONE) { ComposeFontFamily(ComposeTypeface(typeface)) }
}

private data class DocumentFontKey(
    val weight: Int,
    val italic: Boolean,
    val variationSettings: String?,
)

/** Bounded cache of the typefaces built from one document font. */
internal class DocumentFontCache(private val data: ByteArray) {
    private val buffer: ByteBuffer by
        lazy(LazyThreadSafetyMode.NONE) {
            ByteBuffer.allocateDirect(data.size).put(data).apply { rewind() }
        }

    // A null value records a font that failed to build, so it is not retried every frame.
    private val fonts = LruCache<DocumentFontKey, DocumentFont?>()

    fun get(weight: Int, italic: Boolean, variationSettings: String?): DocumentFont? {
        val key = DocumentFontKey(weight, italic, variationSettings)
        if (fonts.containsKey(key)) return fonts[key]
        val font = build(key)
        fonts[key] = font
        return font
    }

    private fun build(key: DocumentFontKey): DocumentFont? =
        try {
            val builder =
                Font.Builder(buffer)
                    .setWeight(key.weight)
                    .setSlant(
                        if (key.italic) FontStyle.FONT_SLANT_ITALIC
                        else FontStyle.FONT_SLANT_UPRIGHT
                    )
            if (!key.variationSettings.isNullOrEmpty()) {
                try {
                    builder.setFontVariationSettings(key.variationSettings)
                } catch (_: IllegalArgumentException) {
                    // Malformed settings from document input keep the default instance.
                }
            }
            val family = FontFamily.Builder(builder.build()).build()
            DocumentFont(
                Typeface.CustomFallbackBuilder(family).setSystemFallback("sans-serif").build()
            )
        } catch (_: Exception) {
            null
        }
}
