/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace.layout

import androidx.annotation.IntRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/** Defines a feathering size based on the percent width and height of the layout. */
internal interface SpatialSmoothFeatheringSize {
    /**
     * Edge feathering based on percent width of the canvas.
     *
     * @param pixels Width of canvas in pixels.
     * @param density The current density of the screen.
     * @return A percent in the range of 0 - 0.5f.
     */
    public fun toWidthPercent(pixels: Float, density: Density): Float

    /**
     * Edge feathering based on percent height of the canvas.
     *
     * @param pixels Height of canvas in pixels.
     * @param density The current density of the screen.
     * @return A percent in the range of 0 - 0.5f.
     */
    public fun toHeightPercent(pixels: Float, density: Density): Float
}

/**
 * Defines a smooth feathering size based on the percent width and height of the layout.
 *
 * @param percentHorizontal Value to feather horizontal edges. A value of 5 represents 5% of the
 *   width of the visible canvas. Accepted value range is 0 - 50 percent.
 * @param percentVertical Value to feather vertical edges. A value of 5 represents 5% of the width
 *   the visible canvas. Accepted value range is 0 - 50 percent.
 */
internal fun spatialSmoothFeatheringSize(
    @IntRange(from = 0, to = 50) percentHorizontal: Int,
    @IntRange(from = 0, to = 50) percentVertical: Int,
): SpatialSmoothFeatheringSize = SpatialFeatheringPercentSize(percentHorizontal, percentVertical)

/**
 * Defines a smooth feathering size using [Dp].
 *
 * @param horizontal Non-negative [Dp] value to feather horizontal edges. Value will be capped at
 *   50% of canvas width if it is too large.
 * @param vertical Non-negative [Dp] value to feather vertical edges. Value will be capped at 50% of
 *   canvas height if it is too large.
 */
internal fun spatialSmoothFeatheringSize(
    horizontal: Dp,
    vertical: Dp,
): SpatialSmoothFeatheringSize = SpatialFeatheringDpSize(horizontal, vertical)

/**
 * Defines a smooth feathering size using on pixels.
 *
 * @param horizontal Non-negative pixels value to feather horizontal edges. Value will be capped at
 *   50% of canvas width if it is too large.
 * @param vertical Non-negative pixels value to feather vertical edges. Value will be capped at 50%
 *   of canvas height if it is too large.
 */
internal fun spatialSmoothFeatheringSize(
    horizontal: Float,
    vertical: Float,
): SpatialSmoothFeatheringSize = SpatialFeatheringPixelSize(horizontal, vertical)

internal class SpatialFeatheringPercentSize(
    private val widthPercent: Int,
    private val heightPercent: Int,
) : SpatialSmoothFeatheringSize {
    override fun toWidthPercent(pixels: Float, density: Density): Float {
        return (widthPercent.toFloat() * 0.01f).coerceIn(0f, 0.5f)
    }

    override fun toHeightPercent(pixels: Float, density: Density): Float {
        return (heightPercent.toFloat() * 0.01f).coerceIn(0f, 0.5f)
    }
}

internal class SpatialFeatheringDpSize(private val horizontalDp: Dp, private val verticalDp: Dp) :
    SpatialSmoothFeatheringSize {
    override fun toWidthPercent(pixels: Float, density: Density): Float {
        return ((horizontalDp.value * density.density) / (pixels * 0.5f)).coerceIn(0f, 0.5f)
    }

    override fun toHeightPercent(pixels: Float, density: Density): Float {
        return ((verticalDp.value * density.density) / (pixels * 0.5f)).coerceIn(0f, 0.5f)
    }
}

internal class SpatialFeatheringPixelSize(
    private val horizontal: Float,
    private val vertical: Float,
) : SpatialSmoothFeatheringSize {
    override fun toWidthPercent(pixels: Float, density: Density): Float {
        return (horizontal / (pixels * 0.5f)).coerceIn(0f, 0.5f)
    }

    override fun toHeightPercent(pixels: Float, density: Density): Float {
        return (vertical / (pixels * 0.5f)).coerceIn(0f, 0.5f)
    }
}
