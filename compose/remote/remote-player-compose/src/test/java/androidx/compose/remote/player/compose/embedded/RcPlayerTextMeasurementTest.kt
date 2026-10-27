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

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.unit.isUnspecified
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Text sizing in the embedded player ([RcPlayer]). Text is laid out and autosized by Compose's
 * BasicText; autosized text scales its line height with the size Compose picks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerTextMeasurementTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun autosizeSingleLineWithLineHeightAdjustments_shrinksToFitWidth() {
        // A single-line label that only fits 100px wide at a small size. With line height
        // adjustments it must still shrink to fit the width, not grow to the max size and clip.
        val text = "Autosized label text"
        rule.renderDocument(
            textDocument(
                text = text,
                fontSize = 14f,
                minFontSize = 10f,
                maxFontSize = 80f,
                lineHeightAdd = 2f,
                lineHeightMultiplier = 1.2f,
                maxLines = 1,
                autosize = true,
                modifier = RecordingModifier().size(100, 100),
            )
        )
        val layout = rule.textLayoutResultOf(text)

        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.hasVisualOverflow).isFalse()
        assertThat(layout.size.width).isAtMost(100)
        assertThat(layout.getLineRight(0)).isAtMost(100f)
    }

    @Test
    fun autosizeLineHeightMultiplier_scalesWithAutosizedFontSize() {
        val text = "AAAA\nBBBB"
        val bitmap =
            rule.renderDocument(
                textDocument(
                    text = text,
                    fontSize = 200f,
                    lineHeightAdd = 4f,
                    lineHeightMultiplier = 1.5f,
                    autosize = true,
                    modifier = RecordingModifier().size(100, 100),
                )
            )
        val layout = rule.textLayoutResultOf(text)
        val style = layout.layoutInput.style

        // The multiplier is relative to the font size Compose picked, not the 200px base size,
        // whose 1.5x line height alone would be 300px.
        assertThat(style.lineHeight.isEm).isTrue()
        assertThat(style.lineHeight.value).isEqualTo(1.5f)
        assertThat(layout.getLineBottom(0) - layout.getLineTop(0)).isLessThan(100f)
        assertThat(layout.lineCount).isEqualTo(2)
        assertThat(layout.multiParagraph.height).isAtMost(100f)
        assertThat(inkBands(bitmap)).isEqualTo(2)
    }

    @Test
    fun autosizeAdditiveLineHeightOnly_usesDefaultLineHeight() {
        val text = "AAAA\nBBBB"
        rule.renderDocument(
            textDocument(
                text = text,
                fontSize = 200f,
                lineHeightAdd = 4f,
                autosize = true,
                modifier = RecordingModifier().size(100, 100),
            )
        )
        val style = rule.textLayoutResultOf(text).layoutInput.style

        // The additive px can't follow the autosized size, so it is dropped rather than applied
        // to the 200px base size.
        assertThat(style.lineHeight.isUnspecified).isTrue()
    }

    @Test
    fun fixedSizeLineHeight_includesAdditivePx() {
        val text = "AAAA\nBBBB"
        rule.renderDocument(
            textDocument(
                text = text,
                fontSize = 20f,
                lineHeightAdd = 4f,
                lineHeightMultiplier = 1.5f,
            )
        )
        val style = rule.textLayoutResultOf(text).layoutInput.style

        // Density is 1, so sp == px: 20 * 1.5 + 4.
        assertThat(style.lineHeight.value).isWithin(0.01f).of(34f)
    }

    @Test
    fun wrappedLetterSpacedText_usesComposeParagraphWidth() {
        // Wrapped text keeps Compose's paragraph width (the max width) rather than core's tight
        // line span, so it can be up to a pixel wider than the View player with letter spacing.
        val bitmap =
            rule.renderDocument(
                textDocument(text = "WWW WWW WWW", fontSize = 20f, letterSpacing = 0.1f)
            )
        assertThat(rule.textLayoutResultOf("WWW WWW WWW").lineCount).isGreaterThan(1)

        assertThat(redExtent(bitmap, y = 1)).isEqualTo(100)
    }

    /** Returns one past the right-most red pixel in row [y]. */
    private fun redExtent(bitmap: Bitmap, y: Int): Int {
        for (x in bitmap.width - 1 downTo 0) {
            val pixel = bitmap.getPixel(x, y)
            if (
                AndroidColor.red(pixel) > 200 &&
                    AndroidColor.green(pixel) < 60 &&
                    AndroidColor.blue(pixel) < 60
            ) {
                return x + 1
            }
        }
        return 0
    }

    /** Counts vertical runs of rows that contain dark (text) pixels. */
    private fun inkBands(bitmap: Bitmap): Int {
        var bands = 0
        var inBand = false
        for (y in 0 until bitmap.height) {
            var hasInk = false
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (AndroidColor.alpha(pixel) > 128 && AndroidColor.red(pixel) < 100) {
                    hasInk = true
                    break
                }
            }
            if (hasInk && !inBand) bands++
            inBand = hasInk
        }
        return bands
    }

    private fun textDocument(
        text: String,
        fontSize: Float,
        minFontSize: Float = -1f,
        maxFontSize: Float = -1f,
        letterSpacing: Float = 0f,
        lineHeightAdd: Float = 0f,
        lineHeightMultiplier: Float = 1f,
        maxLines: Int = Int.MAX_VALUE,
        autosize: Boolean = false,
        modifier: RecordingModifier = RecordingModifier().background(0xFFFF0000.toInt()),
    ): CoreDocument = buildDocument {
        val textId = addText(text)
        root {
            startTextComponent(
                modifier,
                textId,
                -1,
                0xFF000000.toInt(),
                -1,
                fontSize,
                minFontSize,
                maxFontSize,
                0,
                400f,
                null,
                CoreText.TEXT_ALIGN_LEFT,
                CoreText.OVERFLOW_CLIP,
                maxLines,
                letterSpacing,
                lineHeightAdd,
                lineHeightMultiplier,
                0,
                0,
                0,
                false,
                false,
                null,
                null,
                autosize,
                0,
            )
            endTextComponent()
        }
    }
}
