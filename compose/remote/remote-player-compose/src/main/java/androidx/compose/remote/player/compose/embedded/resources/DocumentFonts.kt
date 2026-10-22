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
import java.nio.ByteBuffer

/**
 * Returns the typeface for a font embedded in the document (`FontData`) with id [fontId], or null
 * if trusted content is disabled or [fontId] is not an embedded font.
 *
 * Mirrors the View player's `DefaultTypefaceResolver`, and shares its `Font.Builder` cache on the
 * document's font, so the font weight and slant are those of the first use.
 */
internal fun documentFontTypeface(
    context: RemoteContext,
    fontId: Int,
    weight: Int,
    italic: Boolean,
): Typeface? {
    if ((context.mRemoteComposeState as? SnapshotRemoteComposeState)?.trustedContent != true) {
        return null
    }
    val info = context.getObject(fontId) as? RemoteContext.FontInfo ?: return null
    return try {
        val builder =
            info.fontBuilder as? Font.Builder
                ?: createFontBuilder(info.mFontData, weight, italic).also { info.fontBuilder = it }
        val family = FontFamily.Builder(builder.build()).build()
        Typeface.CustomFallbackBuilder(family).build()
    } catch (_: Exception) {
        null
    }
}

private fun createFontBuilder(data: ByteArray, weight: Int, italic: Boolean): Font.Builder {
    val buffer = ByteBuffer.allocateDirect(data.size).put(data)
    buffer.rewind()
    return Font.Builder(buffer)
        .setWeight(weight)
        .setSlant(if (italic) FontStyle.FONT_SLANT_ITALIC else FontStyle.FONT_SLANT_UPRIGHT)
}
