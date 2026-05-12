/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.brush

import androidx.ink.brush.StockBrushes.DashedLineVersion
import androidx.ink.brush.StockBrushes.EmojiHighlighterVersion
import androidx.ink.brush.StockBrushes.HighlighterVersion
import androidx.ink.brush.StockBrushes.MarkerVersion
import androidx.ink.brush.StockBrushes.PressurePenVersion
import androidx.kruth.assertThat
import kotlin.test.Test

/**
 * Trivial tests for stock brushes. These don't do much since they don't exercise rendering, but
 * they do ensure that the native code loading works as expected.
 */
class StockBrushesTest {

    @Test
    fun loadMarker() {
        val unused = StockBrushes.marker(MarkerVersion.LATEST)
    }

    @Test
    fun loadDashedLine() {
        val unused = StockBrushes.dashedLine(DashedLineVersion.LATEST)
    }

    @Test
    fun loadPressurePen() {
        val unused = StockBrushes.pressurePen(PressurePenVersion.LATEST)
    }

    @Test
    fun loadHighlighter() {
        val highlighter = StockBrushes.highlighter(SelfOverlap.DISCARD, HighlighterVersion.LATEST)
        assertThat(highlighter.coats).hasSize(1)
        assertThat(highlighter.coats[0].paintPreferences).hasSize(1)
        assertThat(highlighter.coats[0].paintPreferences[0].selfOverlap)
            .isEqualTo(SelfOverlap.DISCARD)
    }

    @Test
    fun loadEmojiHighlighter_noTrail() {
        val emojiHighlighter =
            StockBrushes.emojiHighlighter(
                "emoji",
                showMiniEmojiTrail = false,
                selfOverlap = SelfOverlap.DISCARD,
                version = EmojiHighlighterVersion.LATEST,
            )
        assertThat(emojiHighlighter.coats).hasSize(2)
        assertThat(emojiHighlighter.coats[0].paintPreferences[0].selfOverlap)
            .isEqualTo(SelfOverlap.DISCARD)
        for (coat in emojiHighlighter.coats) {
            val textureLayers = coat.paintPreferences[0].textureLayers
            if (!textureLayers.isEmpty()) {
                assertThat((textureLayers[0] as? BrushPaint.TilingTexture)?.clientTextureId)
                    .isEqualTo("emoji")
            }
        }
    }

    @Test
    fun loadEmojiHighlighter_withTrail() {
        val emojiHighlighter =
            StockBrushes.emojiHighlighter(
                "emoji",
                showMiniEmojiTrail = true,
                selfOverlap = SelfOverlap.DISCARD,
                version = EmojiHighlighterVersion.LATEST,
            )
        assertThat(emojiHighlighter.coats).hasSize(5)
        assertThat(emojiHighlighter.coats[0].paintPreferences[0].selfOverlap)
            .isEqualTo(SelfOverlap.DISCARD)
        for (coat in emojiHighlighter.coats) {
            val textureLayers = coat.paintPreferences[0].textureLayers
            if (!textureLayers.isEmpty()) {
                assertThat(
                        // The mini emoji trail uses a stamping texture, while the highlighter uses
                        // a tiling
                        // texture.
                        (textureLayers[0] as? BrushPaint.StampingTexture)?.clientTextureId
                            ?: (textureLayers[0] as? BrushPaint.TilingTexture)?.clientTextureId
                    )
                    .isEqualTo("emoji")
            }
        }
    }
}
