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

package androidx.ink.rendering.android.canvas

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.Stroke
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test

/** Emulator-based screenshot test of brushes defined by [StockBrushes] */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@OptIn(ExperimentalInkCustomBrushApi::class)
@MediumTest
class StockBrushesConsistencyTest() {

    val context = ApplicationProvider.getApplicationContext<Context>()
    val helper = StockBrushesTestHelper(context)

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    companion object {
        private val families =
            listOf(
                StockBrushes.marker(StockBrushes.MarkerVersion.V1),
                StockBrushes.pressurePen(StockBrushes.PressurePenVersion.V1),
                StockBrushes.highlighter(version = StockBrushes.HighlighterVersion.V1),
                StockBrushes.dashedLine(StockBrushes.DashedLineVersion.V1),
            )

        // Collections of test values to check consistency across.
        private val alphas = listOf(255, 153, 76)
        private val colors = listOf(TestColors.BLUE, TestColors.RED, TestColors.GREEN)
        private val sizes = listOf(40f, 5f, 1f)
    }

    fun assertStrokesMatchGolden(strokes: List<List<List<Stroke>>>, name: String) =
        ImageDiffer.diffBitmapWithGolden(
            screenshotRule,
            this::class.simpleName,
            helper.drawToBitmap(strokes),
            name,
        )

    @Test
    fun stockBrushes_withSameAlpha_appearSimilar() {
        assertStrokesMatchGolden(
            families.map { family ->
                alphas.map { alpha ->
                    listOf(
                        Stroke(
                            makeBrush(family = family, alpha = alpha),
                            helper.octogonStylusInputs,
                        )
                    )
                }
            },
            "stockBrushes_withSameAlpha_appearSimilar",
        )
    }

    @Test
    fun stockBrushes_withSameSize_appearSimilar() {
        assertStrokesMatchGolden(
            families.map { family ->
                sizes.map { size ->
                    listOf(
                        Stroke(makeBrush(family = family, size = size), helper.octogonStylusInputs)
                    )
                }
            },
            "stockBrushes_withSameSize_appearSimilar",
        )
    }

    @Test
    fun stockBrushes_withSameColor_appearSimilar() {
        assertStrokesMatchGolden(
            families.map { family ->
                colors.map { color ->
                    listOf(
                        Stroke(
                            makeBrush(family = family, color = color),
                            helper.octogonStylusInputs,
                        )
                    )
                }
            },
            "stockBrushes_withSameColor_appearSimilar",
        )
    }

    @Test
    fun stockBrushes_withSynthesizedInput_appear() {
        assertStrokesMatchGolden(
            families.map { family ->
                alphas.map { alpha ->
                    helper.synthesizedAsteriskDrawing.map { batch ->
                        Stroke(makeBrush(family = family, alpha = alpha, size = 5f), batch)
                    }
                }
            },
            "stockBrushes_withSynthesizedInput_appear",
        )
    }

    /**
     * Creates a brush with the given specifications.
     *
     * @param family required brush family.
     * @param color default blue.
     * @param alpha int value ranging [0,255], default 255.
     * @param size float value, default 20f.
     */
    private fun makeBrush(
        family: BrushFamily,
        @ColorInt color: Int = TestColors.BLUE,
        alpha: Int = 255,
        size: Float = 20f,
        epsilon: Float = 0.1f,
    ) =
        Brush.createWithColorIntArgb(
            family = family,
            colorIntArgb = ColorUtils.setAlphaComponent(color, alpha),
            size = size,
            epsilon = epsilon,
        )
}
