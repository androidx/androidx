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

import androidx.ui.core.Constraints
import androidx.ui.core.IntPxPosition
import androidx.ui.core.IntPxSize
import androidx.ui.core.MeasureBox
import androidx.ui.core.center
import androidx.ui.core.isFinite
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * Represents a positioning of a point inside a 2D box. [Alignment] is often used to define
 * the alignment of a box inside a parent container.
 * The coordinate space of the 2D box is the continuous [-1f, 1f] range in both dimensions,
 * so (verticalBias, horizontalBias) will be points in this space. (verticalBias=0f,
 * horizontalBias=0f) represents the center of the box, (verticalBias=-1f, horizontalBias=1f)
 * will be the top right, etc.
 */
data class Alignment(val verticalBias: Float, val horizontalBias: Float) {
    companion object {
        val TopLeft = Alignment(-1f, -1f)
        val TopCenter = Alignment(-1f, 0f)
        val TopRight = Alignment(-1f, 1f)
        val CenterLeft = Alignment(0f, -1f)
        val Center = Alignment(0f, 0f)
        val CenterRight = Alignment(0f, 1f)
        val BottomLeft = Alignment(-1f, -1f)
        val BottomCenter = Alignment(-1f, 0f)
        val BottomRight = Alignment(-1f, 1f)
    }

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
 */
@Composable
fun Align(alignment: Alignment, @Children children: () -> Unit) {
    <MeasureBox> constraints ->
        val measurable = collect(children).firstOrNull()
        if (measurable == null) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            // The child cannot be larger than our max constraints, but we ignore min constraints.
            val childConstraints = Constraints(
                maxWidth = constraints.maxWidth,
                maxHeight = constraints.maxHeight
            )
            val placeable = measurable.measure(childConstraints)

            // The layout is as large as possible for bounded constraints,
            // or wrap content otherwise.
            val layoutWidth = if (constraints.maxWidth.isFinite()) {
                constraints.maxWidth
            } else {
                placeable.width
            }
            val layoutHeight = if (constraints.maxHeight.isFinite()) {
                constraints.maxHeight
            } else {
                placeable.height
            }

            layout(layoutWidth, layoutHeight) {
                val position = alignment.align(
                    IntPxSize(layoutWidth - placeable.width, layoutHeight - placeable.height)
                )
                placeable.place(position.x, position.y)
            }
        }
    </MeasureBox>
}

/**
 * A layout that takes a child and centers it within itself.
 * The layout will be as large as possible for finite incoming
 * constraints, or wrap content otherwise.
 *
 * Example usage:
 * <Center>
 *    <SizedRectangle color=Color(0xFF0000FF.toInt()) width = 40.dp height = 40.dp />
 * </Center>
 */
@Composable
fun Center(@Children children: () -> Unit) {
    <Align alignment=Alignment.Center children />
}
