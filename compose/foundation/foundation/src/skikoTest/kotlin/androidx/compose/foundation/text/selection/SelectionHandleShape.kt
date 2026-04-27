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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import kotlin.math.pow

internal data class SelectionHandleShape(
    val lineRect: Rect,
    val circleRect: Rect,
) {
    fun isInside(point: IntOffset): Boolean =
        lineRect.roundToIntRect().contains(point) ||
            circleRect.roundToIntRect().deflate(1).containsInOval(point)

    fun isOutside(point: IntOffset): Boolean =
        !lineRect.roundToIntRect().contains(point) &&
            !circleRect.roundToIntRect().inflate(1).containsInOval(point)

    private fun IntRect.containsInOval(point: IntOffset): Boolean {
        val normX = (point.x + 0.5 - center.x) / (width / 2)
        val normY = (point.y + 0.5 - center.y) / (height / 2)
        return normX.pow(2) + normY.pow(2) <= 1.0
    }
}

internal expect fun PlatformSelectionHandleShape(
    density: Density,
    cursor: Rect,
    isStartHandler: Boolean,
): SelectionHandleShape

internal fun DefaultSelectionHandleShape(
    density: Density,
    cursor: Rect,
    isStartHandler: Boolean,
    lineWidth: Dp = 2.dp,
    circleRadius: Dp = 6.dp,
): SelectionHandleShape = with(density) {
    val lineRect = cursor.copy(
        left = cursor.bottomCenter.x - lineWidth.toPx() / 2,
        right = cursor.bottomCenter.x + lineWidth.toPx() / 2,
    )
    val circleCenter = Offset(
        x = cursor.bottomCenter.x,
        y = if (isStartHandler) {
            cursor.top - circleRadius.toPx()
        } else {
            cursor.bottom + circleRadius.toPx()
        }
    )
    val circleRect = Rect(center = circleCenter, radius = circleRadius.toPx())

    SelectionHandleShape(lineRect, circleRect)
}
