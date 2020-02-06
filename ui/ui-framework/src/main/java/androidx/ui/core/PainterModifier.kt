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
import androidx.ui.graphics.painter.Painter
import androidx.ui.unit.Density
import androidx.ui.unit.PxSize

/**
 * Create a [DrawModifier] from this [Painter]. This modifier is memoized and re-used across
 * subsequent compositions
 *
 * @sample androidx.ui.foundation.samples.PainterModifierSample
 */
@Composable
fun Painter.toModifier(
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    rtl: Boolean = false
): DrawModifier {
    // TODO potentially create thread-safe PainterModifier pool to allow for re-use
    //  of PainterModifier instances and avoid gc overhead
    return remember(this, alpha, colorFilter, rtl) {
        PainterModifier(this, alpha, colorFilter, rtl)
    }
}

/**
 * [DrawModifier] used to draw the provided [Painter] followed by the contents
 * of the component itself
 */
private data class PainterModifier(
    val painter: Painter,
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
        painter.draw(
            canvas = canvas,
            bounds = size,
            alpha = alpha,
            colorFilter = colorFilter,
            rtl = rtl)
    }
}