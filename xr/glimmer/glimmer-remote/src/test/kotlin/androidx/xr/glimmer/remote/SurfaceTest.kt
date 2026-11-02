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

package androidx.xr.glimmer.remote

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SurfaceTest {

    @Test
    fun pressedWidthFraction_hasValuePerLayer() {
        assertWithMessage("Wrong amount of pressed widths.")
            .that(PressedWidthFraction)
            .hasLength(LayersCount)
    }

    @Test
    fun idleWidth_hasValuePerLayer() {
        assertWithMessage("Wrong amount of idle widths.").that(IdleWidth).hasLength(LayersCount)
    }

    @Test
    fun focusedWidth_hasValuePerLayer() {
        assertWithMessage("Wrong amount of focused widths.")
            .that(FocusedWidth)
            .hasLength(LayersCount)
    }

    /**
     * Each layer uses only [GradientStopsCount] unique colors. The gradient mirrors them (the
     * colors followed by the same colors in reverse order), so it has `2 * GradientStopsCount`
     * colors in total, and the sweep gradient needs one stop position per color.
     */
    @Test
    fun sweepGradientStops_coversMirroredGradient() {
        assertWithMessage("Wrong amount of gradient stops.")
            .that(SweepGradientStops)
            .hasSize(2 * GradientStopsCount)
    }

    @Test
    fun colorsWithoutAlpha_haveValuePerGradientStop() {
        assertWithMessage("Idle colors array has the wrong size.")
            .that(IdleColorsWithoutAlpha)
            .hasLength(GradientStopsCount)
        assertWithMessage("Non-idle colors array has the wrong size.")
            .that(NonIdleColorsWithoutAlpha)
            .hasLength(GradientStopsCount)
    }

    @Test
    fun idleLayerStopAlphas_haveValuePerLayerAndGradientStop() {
        assertHasValuePerLayerAndGradientStop("Idle", IdleLayerStopAlphas)
    }

    @Test
    fun focusedLayerStopAlphas_haveValuePerLayerAndGradientStop() {
        assertHasValuePerLayerAndGradientStop("Focused", FocusedLayerStopAlphas)
    }

    @Test
    fun pressedLayerStopAlphas_haveValuePerLayerAndGradientStop() {
        assertHasValuePerLayerAndGradientStop("Pressed", PressedLayerStopAlphas)
    }

    @Test
    fun colors_haveSameSize() {
        assertWithMessage("Focused and idle color arrays have different sizes.")
            .that(FocusedColors)
            .hasSize(IdleColors.size)
        assertWithMessage("Pressed and idle color arrays have different sizes.")
            .that(PressedColors)
            .hasSize(IdleColors.size)
    }

    /**
     * Alphas are indexed as `[layer][stop]`: they must have a row for every border layer, and every
     * row must have an alpha for every unique gradient color.
     */
    private fun assertHasValuePerLayerAndGradientStop(
        state: String,
        alphas: Array<Array<Float>>,
    ) {
        assertWithMessage("$state alphas array has the wrong size.")
            .that(alphas)
            .hasLength(LayersCount)
        alphas.forEachIndexed { index, layerAlphas ->
            assertWithMessage("$state alphas array $index has the wrong size.")
                .that(layerAlphas)
                .hasLength(GradientStopsCount)
        }
    }
}
