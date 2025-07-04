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

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * [Modifier] that centers the content horizontally depending on the [CornerBasedShape] provided. It
 * will increase or decrease the start padding to better center the content depending on the corner
 * radii of the provided shape. This is meant to be used with asymmetric shapes, the modifier will
 * not do anything to the content if the shape provided is symmetric.
 *
 * @param shape the [CornerBasedShape] that the content should be adjusted to so that the content is
 *   more centered within the shape.
 * @param maxStartOffset the maximum start offset that the content can be adjusted before it starts
 *   clipping the content
 * @param maxEndOffset the maximum end offset that the content can be adjusted before it starts
 *   clipping the content
 */
@ExperimentalMaterial3ExpressiveApi
internal fun Modifier.horizontalCenterOptically(
    shape: CornerBasedShape,
    maxStartOffset: Dp = 0.dp,
    maxEndOffset: Dp = 0.dp,
) =
    this.layout { measureable, constraints ->
        val placeable = measureable.measure(constraints)
        val width = placeable.width
        val height = placeable.height
        val size = Size(width = width.toFloat(), height = height.toFloat())
        val density = this@layout
        val maxStartOffsetPx = -maxStartOffset.toPx()
        val maxEndOffsetPx = maxEndOffset.toPx()

        val topStart = shape.topStart.toPx(shapeSize = size, density = density)
        val topEnd = shape.topEnd.toPx(shapeSize = size, density = density)
        val bottomStart = shape.bottomStart.toPx(shapeSize = size, density = density)
        val bottomEnd = shape.bottomEnd.toPx(shapeSize = size, density = density)
        val avgStart = (topStart + bottomStart) / 2
        val avgEnd = (topEnd + bottomEnd) / 2
        val paddingCorrection = CenterOpticallyCoefficient * (avgStart - avgEnd)
        layout(width, height) {
            val coercedCorrection = paddingCorrection.coerceIn(maxStartOffsetPx, maxEndOffsetPx)
            placeable.place(coercedCorrection.roundToInt(), 0)
        }
    }

@ExperimentalMaterial3ExpressiveApi
internal fun Modifier.horizontalCenterOptically(
    shape: ShapeWithHorizontalCenterOptically,
    maxStartOffset: Dp = 0.dp,
    maxEndOffset: Dp = 0.dp,
) =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val width = placeable.width
        val height = placeable.height
        val maxStartOffsetPx = -maxStartOffset.toPx()
        val maxEndOffsetPx = maxEndOffset.toPx()
        layout(width, height) {
            val coercedOffset = shape.offset().coerceIn(maxStartOffsetPx, maxEndOffsetPx)
            placeable.place(coercedOffset.roundToInt(), 0)
        }
    }

internal interface ShapeWithHorizontalCenterOptically : Shape {
    fun offset(): Float
}

internal const val CenterOpticallyCoefficient = 0.11f
