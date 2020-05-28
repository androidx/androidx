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

package androidx.ui.graphics.drawscope

import androidx.ui.core.LayoutDirection
import androidx.ui.geometry.Size
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.painter.Painter

/**
 * TestDrawScope implementation with default values for density and layout direction
 */
class TestDrawScope() : DrawScope() {
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override var density: Float = 1.0f

    override var fontScale: Float = 1.0f
}

/**
 * Helper method that draws a Painter into the given canvas, automatically creating a DrawScope
 * to do so with
 */
fun drawPainter(
    painter: Painter,
    canvas: Canvas,
    size: Size,
    alpha: Float = DrawScope.DefaultAlpha,
    colorFilter: ColorFilter? = null,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr
) {
    TestDrawScope().apply { this.layoutDirection = layoutDirection }.draw(canvas, size) {
        with (painter) {
            draw(size, alpha = alpha, colorFilter = colorFilter)
        }
    }
}