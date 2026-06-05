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

package androidx.xr.glimmer.testutils

import android.graphics.Bitmap
import androidx.compose.testutils.assertShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Asserts that the [ImageBitmap] matches the specified [shape] for a Glimmer surface component.
 *
 * Converts the rendered bitmap into a 2-color binary mask (mapping non-background pixels to
 * [Color.Green] and background pixels to [Color.Red]) and delegates directly to Compose's standard
 * [assertShape] to test shape geometry without requiring exact interior surface color matches.
 *
 * @param density The [Density] of the test rule.
 * @param shape The expected [Shape] of the Glimmer surface.
 * @param backgroundColor The expected background [Color].
 * @param shapeSize The size of the shape bounding box.
 * @param shapeCenter The center offset of the shape bounding box.
 * @param antiAliasingGap The size of the margin around the shape outline to leave untested for
 *   sub-pixel anti-aliased edge smoothing.
 */
fun ImageBitmap.assertGlimmerSurfaceShape(
    density: Density,
    shape: Shape,
    backgroundColor: Color,
    shapeSize: Size = Size(width.toFloat(), height.toFloat()),
    shapeCenter: Offset = Offset(width / 2f, height / 2f),
    antiAliasingGap: Dp = 1.dp,
) {
    val actualPixels = toIntArray()
    val backgroundArgb = backgroundColor.toArgb()
    val insidePixelColorArgb = Color.Green.toArgb()
    val outsidePixelColorArgb = Color.Red.toArgb()

    val maskPixels =
        IntArray(actualPixels.size) { i ->
            if (actualPixels[i] == backgroundArgb) outsidePixelColorArgb else insidePixelColorArgb
        }

    Bitmap.createBitmap(maskPixels, width, height, Bitmap.Config.ARGB_8888)
        .asImageBitmap()
        .assertShape(
            density = density,
            shape = shape,
            shapeColor = Color.Green,
            shapeSize = shapeSize,
            shapeCenter = shapeCenter,
            backgroundColor = Color.Red,
            antiAliasingGap = with(density) { antiAliasingGap.toPx() },
        )
}
