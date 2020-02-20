/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.DefaultAlpha
import androidx.ui.graphics.ScaleFit
import androidx.ui.graphics.painter.Painter
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxSize
import kotlin.math.ceil

/**
 * Create a [DrawModifier] from this [Painter]. This modifier is memoized and re-used across
 * subsequent compositions
 *
 * @sample androidx.ui.framework.samples.PainterModifierSample
 */
@Composable
fun Painter.toModifier(
    alignment: Alignment = Alignment.Center,
    scaleFit: ScaleFit = ScaleFit.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    rtl: Boolean = false
): DrawModifier {
    // TODO potentially create thread-safe PainterModifier pool to allow for re-use
    //  of PainterModifier instances and avoid gc overhead
    return remember(this, alignment, scaleFit, alpha, colorFilter, rtl) {
        PainterModifier(this, alignment, scaleFit, alpha, colorFilter, rtl)
    }
}

/**
 * [DrawModifier] used to draw the provided [Painter] followed by the contents
 * of the component itself
 */
private data class PainterModifier(
    val painter: Painter,
    var alignment: Alignment = Alignment.Center,
    var scaleFit: ScaleFit = ScaleFit.Fit,
    var alpha: Float = DefaultAlpha,
    var colorFilter: ColorFilter? = null,
    var rtl: Boolean = false
) : DrawModifier {

    override fun draw(
        density: Density,
        drawContent: () -> Unit,
        canvas: Canvas,
        size: PxSize
    ) {
        val intrinsicSize = painter.intrinsicSize
        val srcWidth = if (intrinsicSize.width.value != Float.POSITIVE_INFINITY) {
            intrinsicSize.width
        } else {
            size.width
        }

        val srcHeight = if (intrinsicSize.height.value != Float.POSITIVE_INFINITY) {
            intrinsicSize.height
        } else {
            size.height
        }

        val scale = scaleFit.scale(PxSize(srcWidth, srcHeight), size)

        val alignedPosition = alignment.align(
            IntPxSize(
                IntPx(ceil(size.width.value - (srcWidth.value * scale)).toInt()),
                IntPx(ceil(size.height.value - (srcHeight.value * scale)).toInt())
            )
        )

        val dx = alignedPosition.x.value.toFloat()
        val dy = alignedPosition.y.value.toFloat()

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)

        painter.draw(
            canvas = canvas,
            bounds = PxSize(srcWidth, srcHeight),
            alpha = alpha,
            colorFilter = colorFilter,
            rtl = rtl)

        canvas.restore()
    }
}