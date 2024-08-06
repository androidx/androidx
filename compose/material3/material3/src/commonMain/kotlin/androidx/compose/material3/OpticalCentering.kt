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

package androidx.compose.material3

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset

/**
 * [Modifier] that centers the content depending on the shape provided. It will increase or decrease
 * the start padding to better center the content depending on the corner radii of the provided
 * shape. This is meant to be used with asymmetric shapes, the modifier will not do anything to the
 * content if the shape provided is symmetric.
 *
 * @param shape the [CornerBasedShape] that the content should be adjusted to so that the content is
 *   more centered within the shape.
 * @param basePadding the initial content padding that would have been applied to the content before
 *   correcting for the corner radii of the shape.
 */
@ExperimentalMaterial3ExpressiveApi
fun Modifier.opticalCentering(shape: CornerBasedShape, basePadding: PaddingValues) =
    this.layout { measurable, constraints ->
        val start = basePadding.calculateStartPadding(layoutDirection)
        val end = basePadding.calculateEndPadding(layoutDirection)
        val top = basePadding.calculateTopPadding()
        val bottom = basePadding.calculateBottomPadding()

        val horizontalPadding = start.roundToPx() + end.roundToPx()
        val verticalPadding = top.roundToPx() + bottom.roundToPx()

        val placeable = measurable.measure(constraints.offset(-horizontalPadding, -verticalPadding))

        val width = constraints.constrainWidth(placeable.width + horizontalPadding)
        val height = constraints.constrainHeight(placeable.height + verticalPadding)

        val size = Size(width = width.toFloat(), height = height.toFloat())
        val density = this@layout
        layout(width, height) {
            val topStart = shape.topStart.toPx(shapeSize = size, density = density)
            val topEnd = shape.topEnd.toPx(shapeSize = size, density = density)
            val bottomStart = shape.bottomStart.toPx(shapeSize = size, density = density)
            val bottomEnd = shape.bottomEnd.toPx(shapeSize = size, density = density)
            val avgStart = (topStart + bottomStart) / 2
            val avgEnd = (topEnd + bottomEnd) / 2
            val startPadding = start.roundToPx() + OpticalCenteringCoefficient * (avgStart - avgEnd)

            placeable.place(startPadding.toInt(), top.roundToPx())
        }
    }

@ExperimentalMaterial3ExpressiveApi
internal fun Modifier.opticalCentering(
    shape: ShapeWithOpticalCentering,
    basePadding: PaddingValues
) =
    this.layout { measurable, constraints ->
        val start = basePadding.calculateStartPadding(layoutDirection)
        val end = basePadding.calculateEndPadding(layoutDirection)
        val top = basePadding.calculateTopPadding()
        val bottom = basePadding.calculateBottomPadding()

        val horizontalPadding = start.roundToPx() + end.roundToPx()
        val verticalPadding = top.roundToPx() + bottom.roundToPx()

        val placeable = measurable.measure(constraints.offset(-horizontalPadding, -verticalPadding))

        val width = constraints.constrainWidth(placeable.width + horizontalPadding)
        val height = constraints.constrainHeight(placeable.height + verticalPadding)

        layout(width, height) {
            val startPadding = start.roundToPx() + shape.offset()
            placeable.place(startPadding.toInt(), top.roundToPx())
        }
    }

internal interface ShapeWithOpticalCentering : Shape {
    fun offset(): Float
}

internal const val OpticalCenteringCoefficient = 0.11f
