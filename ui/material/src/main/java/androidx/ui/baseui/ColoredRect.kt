/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.baseui

import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.toRect
import androidx.ui.core.vectorgraphics.Brush
import androidx.ui.core.vectorgraphics.SolidColor
import androidx.ui.layout.Container
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.trace

/**
 * Component that represents a rectangle painted with the specified [Brush].
 *
 * If width and/or height are not specified, this component will expand
 * to the corresponding max constraints received from the parent
 * if these are finite, or to the min constraints otherwise.
 * Note that even if width and height are specified, these will not be satisfied
 * if the component's incoming layout constraints do not allow that.
 *
 * @param brush brush to paint rect with
 * @param width width of this rect, by default it will match incoming layout constraints
 * @param height height of this rect, by default it will match incoming layout constraints
 */
@Composable
fun ColoredRect(brush: Brush, width: Dp? = null, height: Dp? = null) {
    trace("UI:ColoredRect") {
        Container(width = width, height = height, expanded = true) {
            DrawFillRect(brush = brush)
        }
    }
}

/**
 * Component that represents a rectangle painted with a solid color.
 *
 * @param color color to paint rect with
 * @param width width of this rect, by default it will match parent's constraints
 * @param height height of this rect, by default it will match parent's constraints
 */
@Composable
fun ColoredRect(color: Color, width: Dp? = null, height: Dp? = null) {
    ColoredRect(brush = SolidColor(color), width = width, height = height)
}

@Composable
private fun DrawFillRect(brush: Brush) {
    Draw { canvas, parentSize ->
        val paint = Paint()
        brush.applyBrush(paint)
        canvas.drawRect(parentSize.toRect(), paint)
    }
}