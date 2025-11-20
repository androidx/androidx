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

package androidx.ink.rendering.android.canvas.internal

import android.graphics.Picture
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.brush.SelfOverlap
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Non-emulator logic test of [CanvasPathRenderer].
 *
 * Code in this test doesn't actually render, but it allows a limited subset of tests to run much
 * more quickly. It effectively covers the short-circuit cases that don't lead to any actual
 * rendering.
 *
 * Note that in AndroidX, this test runs on the emulator rather than Robolectric, so it doesn't have
 * a speed benefit.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
class CanvasPathRendererRobolectricTest {
    private val simplePaint = BrushPaint()
    private val simpleBrush =
        Brush(family = BrushFamily(paint = simplePaint), size = 10f, epsilon = 0.1f)

    private val simpleInputs =
        MutableStrokeInputBatch()
            .add(InputToolType.UNKNOWN, x = 10F, y = 10F, elapsedTimeMillis = 100)
            .toImmutable()

    private val renderer = CanvasPathRenderer()

    @Test
    fun canDraw_withEmptyStroke_returnsTrue() {
        val emptyStroke = Stroke(simpleBrush, ImmutableStrokeInputBatch.EMPTY)

        assertThat(
                renderer.canDraw(
                    canvas = createCanvas(),
                    stroke = emptyStroke,
                    coatIndex = 0,
                    paintPreferenceIndex = 0,
                )
            )
            .isTrue()
    }

    @Test
    fun canDraw_withSelfOverlapAccumulate_returnsFalse() {
        val selfOverlapDiscardPaint = BrushPaint(selfOverlap = SelfOverlap.ACCUMULATE)
        val selfOverlapDiscardBrush =
            Brush(
                family =
                    BrushFamily(
                        coats =
                            listOf(BrushCoat(paintPreferences = listOf(selfOverlapDiscardPaint)))
                    ),
                size = 10F,
                epsilon = 0.1F,
            )
        val stroke = Stroke(selfOverlapDiscardBrush, simpleInputs)

        assertThat(
                renderer.canDraw(
                    canvas = createCanvas(),
                    stroke = stroke,
                    coatIndex = 0,
                    paintPreferenceIndex = 0,
                )
            )
            .isFalse()
    }

    @Test
    fun canDraw_withUnsupportedTextureMapping_returnsFalse() {
        val stampingPaint =
            BrushPaint(
                textureLayers =
                    listOf(
                        BrushPaint.TextureLayer(
                            clientTextureId = "foo",
                            sizeX = 16F,
                            sizeY = 16F,
                            mapping = BrushPaint.TextureMapping.STAMPING,
                        )
                    )
            )
        val stampingBrush =
            Brush(
                family =
                    BrushFamily(
                        coats = listOf(BrushCoat(paintPreferences = listOf(stampingPaint)))
                    ),
                size = 10F,
                epsilon = 0.1F,
            )
        val stroke = Stroke(stampingBrush, simpleInputs)

        assertThat(
                renderer.canDraw(
                    canvas = createCanvas(),
                    stroke = stroke,
                    coatIndex = 0,
                    paintPreferenceIndex = 0,
                )
            )
            .isFalse()
    }

    private fun createCanvas() = Picture().beginRecording(100, 100)
}
