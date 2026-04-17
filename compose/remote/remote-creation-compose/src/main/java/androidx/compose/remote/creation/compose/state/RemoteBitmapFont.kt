/*
 * Copyright 2025 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.BitmapTextMeasure
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a **bitmap font** within a Compose Remote hierarchy.
 *
 * A bitmap font defines its glyphs (character representations) using individual [ImageBitmap]
 * images. This class allows you to define custom fonts using rasterized images for each character
 * or character sequence.
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteBitmapFont(
    glyphs: List<Glyph>,
    @Suppress("PrimitiveInCollection") public val kerningTable: Map<String, Short> = emptyMap(),
) : BaseRemoteState<Any>() {
    public val glyphs: List<Glyph> =
        if (glyphs.size <= 1) {
            glyphs
        } else {
            val mutableGlyphs = ArrayList<Glyph>(glyphs.size)
            for (i in glyphs.indices) {
                mutableGlyphs.add(glyphs[i])
            }
            mutableGlyphs.sortWith { a, b -> b.chars.length.compareTo(a.chars.length) }
            mutableGlyphs
        }

    internal enum class OperationKey {
        MeasureWidth,
        MeasureHeight,
    }

    internal override val cacheKey: RemoteStateCacheKey = RemoteStateInstanceKey()

    /** A Glyph from a [RemoteBitmapFont] which may represent one or more characters. */
    public class Glyph(
        /** The character(s) this glyph represents. */
        public val chars: String,

        /** The bitmap for this glyph, or null for a space. */
        public val bitmap: ImageBitmap?,

        /** The margin in pixels to the left of the glyph bitmap. */
        public val marginLeft: Short,

        /** The margin in pixels above of the glyph bitmap. */
        public val marginTop: Short,

        /** The margin in pixels to the right of the glyph bitmap. */
        public val marginRight: Short,

        /** The margin in pixels below the glyph bitmap. */
        public val marginBottom: Short,

        /** The width of the glyph, defaults to the bitmap width. */
        public val width: Short = bitmap?.width?.toShort() ?: 0,

        /** The height of the glyph, defaults to the bitmap height. */
        public val height: Short = bitmap?.height?.toShort() ?: 0,
    )

    public override val constantValueOrNull: Any?
        get() = null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return creationState.document.addBitmapFont(
            Array<BitmapFontData.Glyph>(glyphs.size) { index ->
                val glyph = glyphs[index]
                BitmapFontData.Glyph(
                    glyph.chars,
                    glyph.bitmap?.let { creationState.document.addBitmap(it.asAndroidBitmap()) }
                        ?: -1,
                    glyph.marginLeft,
                    glyph.marginTop,
                    glyph.marginRight,
                    glyph.marginBottom,
                    glyph.width,
                    glyph.height,
                )
            },
            kerningTable,
        )
    }

    private fun lookupGlyph(string: String, offset: Int): Glyph? {
        // Since glyphs is sorted on decreasing size, it will match the longest items first.
        // It is expected that the glyphs array will be fairly small.
        return glyphs.fastFirstOrNull { string.startsWith(it.chars, offset) }
    }

    private class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        fun width() = right - left

        fun height() = bottom - top
    }

    private fun measure(text: String, glyphSpacing: Float): Bounds {
        val xMin = 0f
        var yMin = 1000f
        var xMax = 0f
        var yMax = -Float.MAX_VALUE
        var xPos = 0f
        var pos = 0
        val len = text.length
        var prevGlyph: String? = ""
        while (pos < len) {
            val glyph = lookupGlyph(text, pos)

            if (glyph == null) {
                pos++
                prevGlyph = ""
                continue
            }

            pos += glyph.chars.length
            xPos += (glyph.marginLeft + glyph.marginRight).toFloat()
            if (glyph.bitmap != null) {
                // Space is represented by a null bitmap.
                xPos += glyph.width.toFloat()
            }

            val kerningAdjustment = kerningTable.get(prevGlyph + glyph.chars)
            if (kerningAdjustment != null) {
                xPos += kerningAdjustment.toFloat()
            }

            xMax = xPos
            yMax = max(yMax, (glyph.height + glyph.marginTop + glyph.marginBottom).toFloat())
            yMin = min(yMin, glyph.marginTop.toFloat())
            prevGlyph = glyph.chars

            xPos += glyphSpacing
        }

        return Bounds(xMin, yMin, xMax, yMax)
    }

    /**
     * Evaluates the width of the bounding box of the pixels that would be rendered if [text] was
     * drawn at position 0, 0.
     *
     * @param text The [RemoteString] whose width needs to be measured.
     * @param glyphSpacing A [RemoteFloat] specifying a horizontal adjustment between glyphs in
     *   pixels.
     * @return A [RemoteFloat] representing the calculated width in pixels.
     */
    public fun measureWidth(text: RemoteString, glyphSpacing: RemoteFloat): RemoteFloat {
        if (text.hasConstantValue && glyphSpacing.hasConstantValue) {
            return RemoteFloat(measure(text.constantValue, glyphSpacing.constantValue).width())
        }
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.MeasureWidth, this, text),
        ) { creationState ->
            floatArrayOf(
                creationState.document.bitmapTextMeasure(
                    text.getIdForCreationState(creationState),
                    getIdForCreationState(creationState),
                    BitmapTextMeasure.MEASURE_WIDTH,
                    glyphSpacing.getFloatIdForCreationState(creationState),
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
    public fun measureHeight(text: RemoteString): RemoteFloat {
        if (text.hasConstantValue) {
            return RemoteFloat(measure(text = text.constantValue, glyphSpacing = 0f).height())
        }
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.MeasureHeight, this, text),
        ) { creationState ->
            floatArrayOf(
                creationState.document.bitmapTextMeasure(
                    text.getIdForCreationState(creationState),
                    getIdForCreationState(creationState),
                    BitmapTextMeasure.MEASURE_HEIGHT,
                    0f,
                )
            )
        }
    }
}
