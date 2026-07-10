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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.materialcore.SelectionStage
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
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

/**
 * Draws the provided tick mark that scales in/out in size. The color's alpha component controls the
 * fade in/out. The scaleProgress controls the size, from 0.0 (no size) to 1.0 (full size), with a
 * Cubic Ease-Out curve applied to the scaling. This means the tick grows quickly at the start of
 * the animation and slows down as it reaches full size.
 */
internal fun DrawScope.drawScalingTick(
    tickPath: Path,
    tickColor: Color,
    scaleProgress: Float,
    enabled: Boolean,
) {
    // Optimization: Don't draw if completely transparent or scaled to zero
    if (tickColor.alpha == 0f || scaleProgress == 0f) {
        return
    }

    val strokeWidth = TICK_STROKE_WIDTH_DP.toPx()
    val tickDesignCenterX = TICK_DESIGN_CENTER_X_DP.toPx()
    val tickDesignCenterY = TICK_DESIGN_CENTER_Y_DP.toPx()

    val pivotOffset = Offset(tickDesignCenterX, tickDesignCenterY)

    val normalizedProgress = scaleProgress.coerceIn(0f, 1f)
    // Apply a Cubic Ease-Out function: increase quickly at the beginning and slow down towards the
    // end for better tick visibility
    val easedScaleFactor = 1f - (1f - normalizedProgress).pow(3)

    // Scale around the tick's design center.
    scale(scale = easedScaleFactor, pivot = pivotOffset) {
        drawPath(
            path = tickPath,
            color = tickColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            blendMode = if (enabled) DefaultBlendMode else BlendMode.Hardlight,
        )
    }
}

/**
 * Creates a [Path] object representing the complete geometry of the checkmark. The path coordinates
 * are defined based on fitting the icon within a 24.dp x 24.dp container, and are converted to
 * pixels using the receiver [Density].
 */
internal fun Density.createFullTickPath(): Path {
    val tickBaseComponent = TICK_BASE_COMPONENT_DP.toPx()
    val tickStickComponent = TICK_STICK_COMPONENT_DP.toPx()

    val baseStartX = BASE_START_X_DP.toPx()
    val baseStartY = BASE_START_Y_DP.toPx()
    val stickStartX = STICK_START_X_DP.toPx()
    val stickStartY = STICK_START_Y_DP.toPx()

    return Path().apply {
        // Base segment
        moveTo(baseStartX, baseStartY)
        lineTo(baseStartX + tickBaseComponent, baseStartY + tickBaseComponent)
        // Stick segment
        moveTo(stickStartX, stickStartY)
        lineTo(stickStartX + tickStickComponent, stickStartY - tickStickComponent)
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
    val tickBaseLength = TICK_BASE_COMPONENT_DP.toPx()
    val tickStickLength = TICK_STICK_COMPONENT_DP.toPx()
    val tickTotalLength = tickBaseLength + tickStickLength
    val tickProgressPx = tickProgress * tickTotalLength
    val startXOffsetPx = startXOffset.toPx()
    val center =
        Offset(TICK_DESIGN_CENTER_X_DP.toPx() + startXOffsetPx, TICK_DESIGN_CENTER_Y_DP.toPx())

    // Normalized progress for angle calculation (0 to 1)
    val normalizedProgress = tickProgress.coerceIn(0f, 1f)

    // Apply a Cubic Ease-In function (progress^3) to the normalized progress.
    // This creates an "eased" progress value that increases slowly at the beginning
    val rotationEasedProgress = normalizedProgress.pow(3)

    // Angle decays from TICK_ROTATION to 0.
    // We use (1f - rotationEasedProgress) such that the *change* in angle is slower at the start
    // Meaning the angle stays larger for longer.
    // Ensuring the tick is more visible while undergoing the most significant part of its rotation.
    val angle = TICK_ROTATION * (1f - rotationEasedProgress)
    val angleRadians = angle.toRadians()

    // Animate the base of the tick.
    val baseStart = Offset(BASE_START_X_DP.toPx() + startXOffsetPx, BASE_START_Y_DP.toPx())
    val tickBaseProgress = min(tickProgressPx, tickBaseLength)

    val path = Path()
    path.moveTo(baseStart.rotate(angleRadians, center))
    path.lineTo(
        (baseStart + Offset(tickBaseProgress, tickBaseProgress)).rotate(angleRadians, center)
    )

    if (tickProgressPx > tickBaseLength) {
        val tickStickProgress = min(tickProgressPx - tickBaseLength, tickStickLength)
        val stickStart = Offset(STICK_START_X_DP.toPx() + startXOffsetPx, STICK_START_Y_DP.toPx())
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
        style = Stroke(width = TICK_STROKE_WIDTH_DP.toPx(), cap = StrokeCap.Round),
        blendMode = if (enabled) DefaultBlendMode else BlendMode.Hardlight,
    )
}

private fun DrawScope.eraseTick(
    tickColor: Color,
    tickProgress: Float,
    startXOffset: Dp,
    enabled: Boolean,
) {
    val tickBaseLength = TICK_BASE_COMPONENT_DP.toPx()
    val tickStickLength = TICK_STICK_COMPONENT_DP.toPx()
    val tickTotalLength = tickBaseLength + tickStickLength
    val tickProgressPx = tickProgress * tickTotalLength
    val startXOffsetPx = startXOffset.toPx()

    // Animate the stick of the tick, drawing down the stick from the top.
    val stickStartX = 16.5f.dp.toPx() + startXOffsetPx
    val stickStartY = 9.0f.dp.toPx()
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
        style = Stroke(width = TICK_STROKE_WIDTH_DP.toPx(), cap = StrokeCap.Round),
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

// These COMPONENT constants represent the equal horizontal and vertical projections
// of the 45-degree line segments. The actual Euclidean length of the segments
// is sqrt(2) * component_value.
private val TICK_BASE_COMPONENT_DP = 2.5.dp // dX and dY for the base segment
private val TICK_STICK_COMPONENT_DP = 6.dp // dX and dY for the stick segment

private val BASE_START_X_DP = 7.4.dp
private val BASE_START_Y_DP = 13.0.dp
private val STICK_START_X_DP = 10.5.dp
private val STICK_START_Y_DP = 15.1f.dp

// Center of the tick's 24.dp design box
private val TICK_DESIGN_CENTER_X_DP = 12.dp
private val TICK_DESIGN_CENTER_Y_DP = 12.dp

private val TICK_STROKE_WIDTH_DP = 2.dp
private const val TICK_ROTATION = 15f
