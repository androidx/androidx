/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.annotation.RestrictTo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.materialcore.SelectionStage
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Forked from materialcore package in order to specialise for material3

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun DrawScope.animateTick(
    enabled: Boolean,
    checked: Boolean,
    tickColor: Color,
    tickProgress: Float,
    startXOffset: Dp,
) {
    val targetState = if (checked) SelectionStage.Checked else SelectionStage.Unchecked
    if (targetState == SelectionStage.Checked) {
        // Passing startXOffset as we want checkbox to be aligned to the end of the canvas.
        drawTick(tickColor, tickProgress, startXOffset, enabled)
    } else {
        // Passing startXOffset as we want checkbox to be aligned to the end of the canvas.
        eraseTick(tickColor, tickProgress, startXOffset, enabled)
    }
}

private fun DrawScope.drawTick(
    tickColor: Color,
    tickProgress: Float,
    startXOffset: Dp,
    enabled: Boolean,
) {
    // Using tickProgress animating from zero to TICK_TOTAL_LENGTH,
    // rotate the tick as we draw from 15 degrees to zero.
    val tickBaseLength = TICK_BASE_LENGTH.toPx()
    val tickStickLength = TICK_STICK_LENGTH.toPx()
    val tickTotalLength = tickBaseLength + tickStickLength
    val tickProgressPx = tickProgress * tickTotalLength
    val startXOffsetPx = startXOffset.toPx()
    val center = Offset(12.dp.toPx() + startXOffsetPx, 12.dp.toPx())
    val angle = TICK_ROTATION - TICK_ROTATION / tickTotalLength * tickProgressPx
    val angleRadians = angle.toRadians()

    // Animate the base of the tick.
    val baseStart = Offset(7.4f.dp.toPx() + startXOffsetPx, 13.0f.dp.toPx())
    val tickBaseProgress = min(tickProgressPx, tickBaseLength)

    val path = Path()
    path.moveTo(baseStart.rotate(angleRadians, center))
    path.lineTo(
        (baseStart + Offset(tickBaseProgress, tickBaseProgress)).rotate(angleRadians, center)
    )

    if (tickProgressPx > tickBaseLength) {
        val tickStickProgress = min(tickProgressPx - tickBaseLength, tickStickLength)
        val stickStart = Offset(10.5f.dp.toPx() + startXOffsetPx, 15.1f.dp.toPx())
        // Move back to the start of the stick (without drawing)
        path.moveTo(stickStart.rotate(angleRadians, center))
        path.lineTo(
            Offset(stickStart.x + tickStickProgress, stickStart.y - tickStickProgress)
                .rotate(angleRadians, center)
        )
    }
    drawPath(
        path,
        tickColor,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        blendMode = if (enabled) DefaultBlendMode else BlendMode.Hardlight,
    )
}

private fun DrawScope.eraseTick(
    tickColor: Color,
    tickProgress: Float,
    startXOffset: Dp,
    enabled: Boolean,
) {
    val tickBaseLength = TICK_BASE_LENGTH.toPx()
    val tickStickLength = TICK_STICK_LENGTH.toPx()
    val tickTotalLength = tickBaseLength + tickStickLength
    val tickProgressPx = tickProgress * tickTotalLength
    val startXOffsetPx = startXOffset.toPx()

    // Animate the stick of the tick, drawing down the stick from the top.
    val stickStartX = 16.1f.dp.toPx() + startXOffsetPx
    val stickStartY = 9.5f.dp.toPx()
    val tickStickProgress = min(tickProgressPx, tickStickLength)

    val path = Path()
    path.moveTo(stickStartX, stickStartY)
    path.lineTo(stickStartX - tickStickProgress, stickStartY + tickStickProgress)

    if (tickStickProgress > tickStickLength) {
        // Animate the base of the tick, drawing up the base from bottom of the stick.
        val tickBaseProgress = min(tickProgressPx - tickStickLength, tickBaseLength)
        val baseStartX = 10.0f.dp.toPx() + startXOffsetPx
        val baseStartY = 15.6f.dp.toPx()
        path.moveTo(baseStartX, baseStartY)
        path.lineTo(baseStartX - tickBaseProgress, baseStartY - tickBaseProgress)
    }

    drawPath(
        path,
        tickColor,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        blendMode = if (enabled) DefaultBlendMode else BlendMode.Hardlight,
    )
}

private fun Path.moveTo(offset: Offset) {
    moveTo(offset.x, offset.y)
}

private fun Path.lineTo(offset: Offset) {
    lineTo(offset.x, offset.y)
}

private fun Offset.rotate(angleRadians: Float): Offset {
    val angledDirection = directionVector(angleRadians)
    return angledDirection * x + angledDirection.rotate90() * y
}

private fun Offset.rotate(angleRadians: Float, center: Offset): Offset =
    (this - center).rotate(angleRadians) + center

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun directionVector(angleRadians: Float): Offset =
    Offset(cos(angleRadians), sin(angleRadians))

private fun Offset.rotate90() = Offset(-y, x)

private val TICK_BASE_LENGTH = 2.5.dp
private val TICK_STICK_LENGTH = 6.dp
private const val TICK_ROTATION = 15f
