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

package androidx.ui.layout

import androidx.ui.core.IntPxPosition
import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.center
import androidx.ui.core.isFinite
import androidx.ui.core.looseMin
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Represents a positioning of a point inside a 2D box. [Alignment] is often used to define
 * the alignment of a box inside a parent container.
 * The coordinate space of the 2D box is the continuous [-1f, 1f] range in both dimensions,
 * so (verticalBias, horizontalBias) will be points in this space. (verticalBias=0f,
 * horizontalBias=0f) represents the center of the box, (verticalBias=-1f, horizontalBias=1f)
 * will be the top right, etc.
 */
enum class Alignment(private val verticalBias: Float, private val horizontalBias: Float) {
    TopLeft(-1f, -1f),
    TopCenter(-1f, 0f),
    TopRight(-1f, 1f),
    CenterLeft(0f, -1f),
    Center(0f, 0f),
    CenterRight(0f, 1f),
    BottomLeft(1f, -1f),
    BottomCenter(1f, 0f),
    BottomRight(1f, 1f);

    /**
     * Returns the position of a 2D point in a container of a given size,
     * according to this [Alignment].
     */
    fun align(size: IntPxSize): IntPxPosition {
        val center = size.center()
        return IntPxPosition(center.x * (1 + horizontalBias), center.y * (1 + verticalBias))
    }
}

/**
 * A layout that takes a child and aligns it within itself, according to the alignment parameter.
 * The layout will be as large as possible for finite incoming constraints,
 * or wrap content otherwise.
 *
 * For a widget that does alignment and tries to be the same size as its child, see [Wrap].
 */
@Composable
fun Align(alignment: Alignment, @Children children: @Composable() () -> Unit) {
    Layout(layoutBlock ={ measurables, constraints ->
        val measurable = measurables.firstOrNull()
        // The child cannot be larger than our max constraints, but we ignore min constraints.
        val placeable = measurable?.measure(constraints.looseMin())

        // The layout is as large as possible for bounded constraints,
        // or wrap content otherwise.
        val layoutWidth = if (constraints.maxWidth.isFinite()) {
            constraints.maxWidth
        } else {
            placeable?.width ?: constraints.minWidth
        }
        val layoutHeight = if (constraints.maxHeight.isFinite()) {
            constraints.maxHeight
        } else {
            placeable?.height ?: constraints.minHeight
        }

        layout(layoutWidth, layoutHeight) {
            if (placeable != null) {
                val position = alignment.align(
                    IntPxSize(layoutWidth - placeable.width, layoutHeight - placeable.height)
                )
                placeable.place(position.x, position.y)
            }
        }
    }, children = children)
}

/**
 * A layout that takes a child and centers it within itself.
 * The layout will be as large as possible for finite incoming
 * constraints, or wrap content otherwise.
 *
 * For a widget that does alignment and tries to be the same size as its child, see [Wrap].
 *
 * Example usage:
 * Center {
 *    SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 40.dp, height = 40.dp)
 * }
 */
@Composable
fun Center(@Children children: @Composable() () -> Unit) {
    Align(alignment = Alignment.Center, children = children)
}
