/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.frontend.state

import android.graphics.Bitmap
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.BitmapTextMeasure
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState

/**
 * Represents a **bitmap font** within a Compose Remote hierarchy.
 *
 * A bitmap font defines its glyphs (character representations) using individual [Bitmap] images.
 * This class allows you to define custom fonts using rasterized images for each character or
 * character sequence.
 *
 * When bitmap fonts are rendered, a **greedy algorithm** is used to match parts of the text to
 * available glyphs. This means that the system prefers to match longer glyphs (e.g., a glyph for
 * "fi") over shorter ones (e.g., individual glyphs for 'f' and 'i') if both are available.
 *
 * @property glyphs A [List] of [Glyph] objects that define the character mappings and visual
 *   properties for this font. The maximum length of the glyphs list is 65535.
 * @property kerningTable The kerning table, where the key is pairs of glyphs (literally $1$2) and
 *   the value is the horizontal adjustment in pixels for that glyph pair. Can be empty. The maximum
 *   size of the kerning table is 65535 entries.
 */
class RemoteBitmapFont(val glyphs: List<Glyph>, val kerningTable: Map<String, Short> = emptyMap()) :
    BaseRemoteState {
    /** A Glyph from a [RemoteBitmapFont] which may represent one or more characters. */
    class Glyph(
        /** The character(s) this glyph represents. */
        val chars: String,

        /** The bitmap for this glyph, or null for a space. */
        val bitmap: Bitmap?,

        /** The margin in pixels to the left of the glyph bitmap. */
        val marginLeft: Short,

        /** The margin in pixels above of the glyph bitmap. */
        val marginTop: Short,

        /** The margin in pixels to the right of the glyph bitmap. */
        val marginRight: Short,

        /** The margin in pixels below the glyph bitmap. */
        val marginBottom: Short,
    )

    override val hasConstantValue: Boolean
        get() = true

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return creationState.document.addBitmapFont(
            Array<BitmapFontData.Glyph>(glyphs.size) { index ->
                val glyph = glyphs[index]
                BitmapFontData.Glyph(
                    glyph.chars,
                    glyph.bitmap?.let { creationState.document.addBitmap(it) } ?: -1,
                    glyph.marginLeft,
                    glyph.marginTop,
                    glyph.marginRight,
                    glyph.marginBottom,
                    glyph.bitmap?.width?.toShort() ?: 0,
                    glyph.bitmap?.height?.toShort() ?: 0,
                )
            },
            kerningTable,
        )
    }

    /**
     * Evaluates the width of the bounding box of the pixels that would be rendered if [text] was
     * drawn at position 0, 0.
     *
     * @param text The [RemoteString] whose width needs to be measured.
     * @return A [RemoteFloat] representing the calculated width in pixels.
     */
    fun measureWidth(text: RemoteString): RemoteFloat {
        return RemoteFloatExpression(text.hasConstantValue) { creationState ->
            floatArrayOf(
                creationState.document.bitmapTextMeasure(
                    text.getIdForCreationState(creationState),
                    getIdForCreationState(creationState),
                    BitmapTextMeasure.MEASURE_WIDTH,
                )
            )
        }
    }

    /**
     * Evaluates the height of the bounding box of the pixels that would be rendered if [text] was
     * drawn at position 0, 0.
     *
     * @param text The [RemoteString] whose height needs to be measured.
     * @return A [RemoteFloat] representing the calculated height in pixels.
     */
    fun measureHeight(text: RemoteString): RemoteFloat {
        return RemoteFloatExpression(text.hasConstantValue) { creationState ->
            floatArrayOf(
                creationState.document.bitmapTextMeasure(
                    text.getIdForCreationState(creationState),
                    getIdForCreationState(creationState),
                    BitmapTextMeasure.MEASURE_HEIGHT,
                )
            )
        }
    }
}
