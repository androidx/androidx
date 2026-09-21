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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.material3.internal.LocalWristOrientation
import androidx.wear.compose.material3.internal.isLeftWrist
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal val POINTER_BACKGROUND_WIDTH = 54.dp
internal val POINTER_BACKGROUND_HEIGHT = 48.dp
internal val POINTER_BACKGROUND_SIZE = DpSize(POINTER_BACKGROUND_WIDTH, POINTER_BACKGROUND_HEIGHT)

/**
 * Draws the one-handed gesture pointer background natively in Compose using paths, rather than
 * using a vector drawable.
 */
@Composable
internal fun GestureIndicatorPointerBackground(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(POINTER_BACKGROUND_SIZE)) {
        val scaleX = size.width / 54f
        val scaleY = size.height / 48f
        withTransform(
            transformBlock = {
                scale(scaleX, scaleY, pivot = Offset.Zero)
            }
        ) {
            val path =
                Path().apply {
                    moveTo(24f, 0f)
                    cubicTo(35.572f, 0f, 45.23f, 8.19f, 47.496f, 19.09f)
                    lineTo(53f, 22.268f)
                    cubicTo(54.333f, 23.037f, 54.333f, 24.963f, 53f, 25.732f)
                    lineTo(47.496f, 28.909f)
                    cubicTo(45.23f, 39.809f, 35.572f, 48f, 24f, 48f)
                    cubicTo(10.745f, 48f, 0f, 37.255f, 0f, 24f)
                    cubicTo(0f, 10.745f, 10.745f, 0f, 24f, 0f)
                    close()
                }
            drawPath(path, color = color)
        }
    }
}

/**
 * Procedurally draws the one-handed gesture primary (double-pinch) indicator animation natively in
 * Compose using paths.
 */
@Composable
internal fun PrimaryIndicatorBox(
    active: Boolean,
    size: DpSize,
    tint: Color,
    modifier: Modifier = Modifier,
    scaleX: () -> Float = { 1.0f },
    scaleY: () -> Float = { 1.0f },
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(active) {
        if (active) {
            // Stage 0 -> 1: First pinch contact (0 -> 131ms)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 131, easing = PrimaryEasePinchIn1),
            )
            // Stage 1 -> 2: Partial release (131 -> 300ms, duration 169ms)
            progress.animateTo(
                targetValue = 2f,
                animationSpec = tween(durationMillis = 169, easing = PrimaryEasePinchOut1),
            )
            // Stage 2 -> 3: Second pinch contact (300 -> 433ms, duration 133ms)
            progress.animateTo(
                targetValue = 3f,
                animationSpec = tween(durationMillis = 133, easing = PrimaryEasePinchIn2),
            )
            // Stage 3 -> 4: Return to rest (433 -> 617ms, duration 184ms)
            progress.animateTo(
                targetValue = 4f,
                animationSpec = tween(durationMillis = 184, easing = PrimaryEaseReturn),
            )
        } else {
            progress.snapTo(0f)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val wristOrientation = LocalWristOrientation.current
        val stroke = remember {
            Stroke(
                width = 2.0f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
        }

        Canvas(
            modifier =
                Modifier.size(size).graphicsLayer {
                    // Mirror the indicator only when worn on the right wrist
                    this.scaleX = if (wristOrientation.isLeftWrist()) scaleX() else -scaleX()
                    this.scaleY = scaleY()
                }
        ) {
            val scale = this.size.minDimension / 36f
            val stage = progress.value.coerceIn(0f, 4f)

            withTransform(
                transformBlock = {
                    scale(scale, scale, pivot = Offset.Zero)
                }
            ) {
                drawPrimaryGesture(stage, tint, stroke)
            }
        }
    }
}

@Composable
internal fun PrimaryIndicatorBox(
    state: OneHandedGestureClickIndicatorState,
    gestureIndicatorSize: OneHandedGestureIndicatorSize,
    gestureIndicatorTint: Color,
    modifier: Modifier = Modifier,
) =
    PrimaryIndicatorBox(
        active = state.active,
        size = DpSize(gestureIndicatorSize.size, gestureIndicatorSize.size),
        tint = gestureIndicatorTint,
        modifier = modifier.layoutId("icon"),
        scaleX = { state.scaleAnimatable.value },
        scaleY = { state.scaleAnimatable.value },
    )

/**
 * Procedurally draws the one-handed gesture dismiss (wrist-turn) indicator animation natively in
 * Compose using paths.
 */
@Composable
internal fun DismissIndicatorBox(
    active: Boolean,
    size: DpSize,
    tint: Color,
    modifier: Modifier = Modifier,
    scaleX: () -> Float = { 1.0f },
    scaleY: () -> Float = { 1.0f },
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(active) {
        if (active) {
            // Forward wrist-turn rotation (0 -> 300ms)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = DismissEaseInOut),
            )
            // Return wrist-turn rotation (300 -> 600ms)
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = DismissEaseInOut),
            )
        } else {
            progress.snapTo(0f)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val wristOrientation = LocalWristOrientation.current
        val stroke = remember {
            Stroke(
                width = 2.0f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
        }

        Canvas(
            modifier =
                Modifier.size(size).graphicsLayer {
                    // Mirror the indicator only when worn on the right wrist
                    this.scaleX = if (wristOrientation.isLeftWrist()) scaleX() else -scaleX()
                    this.scaleY = scaleY()
                }
        ) {
            val scale = this.size.minDimension / 36f
            val currentProgress = progress.value.coerceIn(0f, 1f)

            withTransform(
                transformBlock = {
                    scale(scale, scale, pivot = Offset.Zero)
                    // Vertical bob: translateY moves from 18 -> 15.8dp (-2.2dp) at peak rotation
                    translate(left = 0f, top = -2.2f * currentProgress)
                }
            ) {
                drawDismissGesture(currentProgress, tint, stroke)
            }
        }
    }
}

@Composable
internal fun DismissIndicatorBox(
    state: OneHandedGestureClickIndicatorState,
    gestureIndicatorSize: OneHandedGestureIndicatorSize,
    gestureIndicatorTint: Color,
    modifier: Modifier = Modifier,
) =
    DismissIndicatorBox(
        active = state.active,
        size = DpSize(gestureIndicatorSize.size, gestureIndicatorSize.size),
        tint = gestureIndicatorTint,
        modifier = modifier.layoutId("icon"),
        scaleX = { state.scaleAnimatable.value },
        scaleY = { state.scaleAnimatable.value },
    )

private fun DrawScope.drawPrimaryGesture(
    stage: Float,
    tint: Color,
    stroke: Stroke,
) {
    // All coordinate offsets are centered at (18, 18)
    val cx = 18f
    val cy = 18f

    // 1. Index finger & thumb contour loop
    val handBodyPath =
        Path().apply {
            val startX = cx + lerpPrimary(-2.00f, -1.60f, -1.91f, stage)
            val startY = cy + lerpPrimary(-4.58f, -4.28f, -4.51f, stage)
            moveTo(startX, startY)

            cubicTo(
                cx + lerpPrimary(0.96f, 2.27f, 1.26f, stage),
                cy + lerpPrimary(-8.38f, -7.65f, -8.21f, stage),
                cx + lerpPrimary(5.49f, 6.02f, 5.61f, stage),
                cy + lerpPrimary(-10.17f, -7.65f, -9.59f, stage),
                cx + lerpPrimary(7.00f, 8.38f, 7.31f, stage),
                cy + lerpPrimary(-10.45f, -7.17f, -9.70f, stage),
            )
            cubicTo(
                cx + lerpPrimary(8.82f, 10.37f, 9.17f, stage),
                cy + lerpPrimary(-10.87f, -6.76f, -9.93f, stage),
                cx + lerpPrimary(10.72f, 12.38f, 11.10f, stage),
                cy + lerpPrimary(-11.07f, -5.24f, -9.74f, stage),
                cx + lerpPrimary(12.47f, 13.43f, 12.68f, stage),
                cy + lerpPrimary(-10.91f, -3.84f, -9.29f, stage),
            )
            cubicTo(
                cx + lerpPrimary(13.41f, 14.00f, 13.54f, stage),
                cy + lerpPrimary(-10.82f, -3.08f, -9.05f, stage),
                cx + lerpPrimary(14.65f, 14.66f, 14.65f, stage),
                cy + lerpPrimary(-10.74f, -1.93f, -8.73f, stage),
                cx + lerpPrimary(15.15f, 14.47f, 15.00f, stage),
                cy + lerpPrimary(-9.86f, -0.99f, -7.84f, stage),
            )
            cubicTo(
                cx + lerpPrimary(15.66f, 14.32f, 15.36f, stage),
                cy + lerpPrimary(-8.98f, -0.24f, -6.99f, stage),
                cx + lerpPrimary(15.67f, 13.69f, 15.21f, stage),
                cy + lerpPrimary(-8.28f, 0.32f, -6.32f, stage),
                cx + lerpPrimary(14.92f, 12.62f, 14.40f, stage),
                cy + lerpPrimary(-7.60f, 0.20f, -5.82f, stage),
            )
            cubicTo(
                cx + lerpPrimary(14.47f, 11.98f, 13.90f, stage),
                cy + lerpPrimary(-7.29f, 0.13f, -5.59f, stage),
                cx + lerpPrimary(13.54f, 11.13f, 12.99f, stage),
                cy + lerpPrimary(-7.06f, -0.51f, -5.56f, stage),
                cx + lerpPrimary(13.04f, 10.63f, 12.49f, stage),
                cy + lerpPrimary(-7.07f, -0.92f, -5.67f, stage),
            )
            cubicTo(
                cx + lerpPrimary(11.04f, 8.84f, 10.54f, stage),
                cy + lerpPrimary(-7.14f, -2.40f, -6.06f, stage),
                cx + lerpPrimary(7.57f, 7.97f, 7.66f, stage),
                cy + lerpPrimary(-6.65f, -3.25f, -5.87f, stage),
                cx + lerpPrimary(5.04f, 4.87f, 5.00f, stage),
                cy + lerpPrimary(-4.83f, -2.40f, -4.28f, stage),
            )
            cubicTo(
                cx + lerpPrimary(3.65f, 3.22f, 3.55f, stage),
                cy + lerpPrimary(-3.84f, -1.96f, -3.41f, stage),
                cx + lerpPrimary(2.02f, 2.15f, 2.05f, stage),
                cy + lerpPrimary(-2.15f, -0.88f, -1.86f, stage),
                cx + lerpPrimary(1.74f, 1.54f, 1.69f, stage),
                cy + lerpPrimary(-0.40f, 0.00f, -0.31f, stage),
            )
            cubicTo(
                cx + lerpPrimary(1.32f, 0.32f, 1.09f, stage),
                cy + lerpPrimary(2.16f, 1.75f, 2.07f, stage),
                cx + lerpPrimary(2.28f, 0.74f, 1.93f, stage),
                cy + lerpPrimary(3.98f, 3.45f, 3.86f, stage),
                cx + lerpPrimary(3.66f, 1.91f, 3.26f, stage),
                cy + lerpPrimary(4.57f, 4.27f, 4.50f, stage),
            )
            cubicTo(
                cx + lerpPrimary(5.90f, 5.12f, 5.72f, stage),
                cy + lerpPrimary(5.53f, 6.50f, 5.75f, stage),
                cx + lerpPrimary(10.10f, 8.68f, 9.77f, stage),
                cy + lerpPrimary(2.87f, 1.57f, 2.57f, stage),
                cx + lerpPrimary(12.29f, 11.11f, 12.02f, stage),
                cy + lerpPrimary(3.93f, 1.40f, 3.35f, stage),
            )
            cubicTo(
                cx + lerpPrimary(12.64f, 11.50f, 12.38f, stage),
                cy + lerpPrimary(4.10f, 1.37f, 3.48f, stage),
                cx + lerpPrimary(13.11f, 12.14f, 12.89f, stage),
                cy + lerpPrimary(4.57f, 1.54f, 3.88f, stage),
                cx + lerpPrimary(13.23f, 12.45f, 13.05f, stage),
                cy + lerpPrimary(4.99f, 1.85f, 4.27f, stage),
            )
            cubicTo(
                cx + lerpPrimary(13.84f, 14.09f, 13.90f, stage),
                cy + lerpPrimary(7.21f, 3.46f, 6.35f, stage),
                cx + lerpPrimary(11.15f, 11.90f, 11.32f, stage),
                cy + lerpPrimary(7.50f, 5.06f, 6.94f, stage),
                cx + lerpPrimary(9.93f, 11.04f, 10.18f, stage),
                cy + lerpPrimary(7.89f, 6.01f, 7.46f, stage),
            )
            cubicTo(
                cx + lerpPrimary(8.53f, 10.05f, 8.88f, stage),
                cy + lerpPrimary(8.35f, 7.10f, 8.06f, stage),
                cx + lerpPrimary(6.65f, 6.65f, 6.65f, stage),
                cy + lerpPrimary(9.67f, 9.67f, 9.67f, stage),
                cx + lerpPrimary(4.17f, 4.17f, 4.17f, stage),
                cy + lerpPrimary(10.85f, 10.85f, 10.85f, stage),
            )
            cubicTo(
                cx + lerpPrimary(2.19f, 2.19f, 2.19f, stage),
                cy + lerpPrimary(11.78f, 11.78f, 11.78f, stage),
                cx + lerpPrimary(-0.20f, 0.22f, -0.11f, stage),
                cy + lerpPrimary(12.60f, 12.39f, 12.55f, stage),
                cx + lerpPrimary(-2.69f, -2.27f, -2.59f, stage),
                cy + lerpPrimary(12.71f, 12.50f, 12.66f, stage),
            )
            cubicTo(
                cx + lerpPrimary(-4.31f, -3.89f, -4.22f, stage),
                cy + lerpPrimary(12.77f, 12.56f, 12.73f, stage),
                cx + lerpPrimary(-5.97f, -5.82f, -5.94f, stage),
                cy + lerpPrimary(12.54f, 12.38f, 12.50f, stage),
                cx + lerpPrimary(-7.58f, -7.37f, -7.53f, stage),
                cy + lerpPrimary(11.84f, 11.49f, 11.76f, stage),
            )
        }

    // 2. Inner knuckle/crease
    val knucklePath =
        Path().apply {
            val startX = cx + lerpPrimary(-5.12f, -4.47f, -4.97f, stage)
            val startY = cy + lerpPrimary(-7.51f, -7.01f, -7.40f, stage)
            moveTo(startX, startY)

            cubicTo(
                cx + lerpPrimary(-2.05f, 0.67f, -1.43f, stage),
                cy + lerpPrimary(-10.56f, -11.75f, -10.83f, stage),
                cx + lerpPrimary(3.77f, 7.62f, 4.65f, stage),
                cy + lerpPrimary(-15.11f, -12.90f, -14.60f, stage),
                cx + lerpPrimary(9.72f, 9.52f, 9.68f, stage),
                cy + lerpPrimary(-11.20f, -7.15f, -10.28f, stage),
            )
        }

    // 3. Lower wrist contour
    val lowerWristPath =
        Path().apply {
            val startX = cx + lerpPrimary(0.99f, 1.89f, 1.19f, stage)
            val startY = cy + lerpPrimary(-12.01f, -10.86f, -11.74f, stage)
            moveTo(startX, startY)

            cubicTo(
                cx + lerpPrimary(-1.14f, 0.32f, -0.80f, stage),
                cy + lerpPrimary(-12.35f, -12.00f, -12.27f, stage),
                cx + lerpPrimary(-3.70f, -3.00f, -3.54f, stage),
                cy + lerpPrimary(-13.14f, -12.44f, -12.98f, stage),
                cx + lerpPrimary(-7.61f, -6.91f, -7.45f, stage),
                cy + lerpPrimary(-10.01f, -9.31f, -9.85f, stage),
            )
            cubicTo(
                cx + lerpPrimary(-8.75f, -8.05f, -8.59f, stage),
                cy + lerpPrimary(-9.10f, -8.40f, -8.93f, stage),
                cx + lerpPrimary(-10.69f, -10.36f, -10.61f, stage),
                cy + lerpPrimary(-7.01f, -6.29f, -6.85f, stage),
                cx + lerpPrimary(-12.01f, -11.68f, -11.93f, stage),
                cy + lerpPrimary(-3.14f, -2.42f, -2.98f, stage),
            )
            cubicTo(
                cx + lerpPrimary(-13.33f, -13.00f, -13.25f, stage),
                cy + lerpPrimary(0.73f, 1.45f, 0.89f, stage),
                cx + lerpPrimary(-13.74f, -13.41f, -13.67f, stage),
                cy + lerpPrimary(2.77f, 2.79f, 2.77f, stage),
                cx + lerpPrimary(-15.15f, -14.82f, -15.07f, stage),
                cy + lerpPrimary(4.67f, 4.69f, 4.68f, stage),
            )
        }

    drawPath(handBodyPath, color = tint, style = stroke)
    drawPath(knucklePath, color = tint, style = stroke)
    drawPath(lowerWristPath, color = tint, style = stroke)
}

private fun DrawScope.drawDismissGesture(
    progress: Float,
    tint: Color,
    stroke: Stroke,
) {
    // 1. Hand body / forearm / palm contour
    val handBodyPath =
        Path().apply {
            val startX = lerp(2.67f, 8.35f, progress)
            val startY = lerp(23.00f, 26.51f, progress)
            moveTo(startX, startY)

            // Lower wrist/palm curve
            cubicTo(
                lerp(3.41f, 9.42f, progress),
                lerp(24.74f, 27.37f, progress),
                lerp(4.68f, 10.62f, progress),
                lerp(26.11f, 27.93f, progress),
                lerp(6.17f, 11.86f, progress),
                lerp(27.17f, 28.29f, progress),
            )
            cubicTo(
                lerp(8.69f, 13.76f, progress),
                lerp(28.98f, 28.84f, progress),
                lerp(11.83f, 15.74f, progress),
                lerp(29.89f, 28.90f, progress),
                lerp(14.00f, 17.43f, progress),
                lerp(30.18f, 28.78f, progress),
            )
            // Pinky / Palm bottom turn
            cubicTo(
                lerp(15.87f, 19.55f, progress),
                lerp(30.43f, 28.64f, progress),
                lerp(17.98f, 22.15f, progress),
                lerp(30.89f, 28.22f, progress),
                lerp(19.80f, 23.30f, progress),
                lerp(31.12f, 28.27f, progress),
            )
            // Knuckle loop
            cubicTo(
                lerp(22.06f, 24.30f, progress),
                lerp(31.41f, 28.31f, progress),
                lerp(23.87f, 26.36f, progress),
                lerp(31.35f, 27.90f, progress),
                lerp(24.20f, 26.38f, progress),
                lerp(30.11f, 26.31f, progress),
            )
            // Palm interior fold
            cubicTo(
                lerp(24.68f, 26.38f, progress),
                lerp(28.33f, 26.01f, progress),
                lerp(21.60f, 26.12f, progress),
                lerp(27.14f, 25.59f, progress),
                lerp(20.60f, 25.89f, progress),
                lerp(26.95f, 25.39f, progress),
            )
            cubicTo(
                lerp(19.60f, 24.47f, progress),
                lerp(26.77f, 24.15f, progress),
                lerp(16.37f, 19.84f, progress),
                lerp(26.26f, 24.87f, progress),
                lerp(15.80f, 18.36f, progress),
                lerp(25.93f, 23.69f, progress),
            )
            // Thumb crease & extension
            cubicTo(
                lerp(14.50f, 16.11f, progress),
                lerp(25.19f, 22.11f, progress),
                lerp(15.20f, 17.02f, progress),
                lerp(23.71f, 19.79f, progress),
                lerp(16.40f, 17.88f, progress),
                lerp(23.78f, 18.23f, progress),
            )
            // Thumb tip & upper arc
            cubicTo(
                lerp(17.60f, 19.30f, progress),
                lerp(23.85f, 15.66f, progress),
                lerp(33.20f, 24.61f, progress),
                lerp(24.72f, 15.38f, progress),
                lerp(33.20f, 29.61f, progress),
                lerp(24.72f, 17.18f, progress),
            )
            cubicTo(
                lerp(35.00f, 31.80f, progress),
                lerp(24.72f, 17.97f, progress),
                lerp(35.00f, 33.37f, progress),
                lerp(24.72f, 17.51f, progress),
                lerp(35.00f, 34.06f, progress),
                lerp(23.54f, 16.64f, progress),
            )
            cubicTo(
                lerp(35.00f, 34.93f, progress),
                lerp(21.63f, 15.53f, progress),
                lerp(32.80f, 34.34f, progress),
                lerp(20.75f, 13.75f, progress),
                lerp(31.40f, 31.70f, progress),
                lerp(20.55f, 13.09f, progress),
            )
            // Upper forearm back
            cubicTo(
                lerp(26.60f, 25.40f, progress),
                lerp(19.96f, 11.53f, progress),
                lerp(20.00f, 22.29f, progress),
                lerp(19.36f, 10.25f, progress),
                lerp(17.60f, 17.40f, progress),
                lerp(19.95f, 9.93f, progress),
            )
        }

    // 2. Upper wrist contour
    val upperWristPath =
        Path().apply {
            moveTo(lerp(2.67f, 2.49f, progress), lerp(12.00f, 18.84f, progress))
            cubicTo(
                lerp(3.61f, 5.58f, progress),
                lerp(12.00f, 18.09f, progress),
                lerp(5.95f, 7.32f, progress),
                lerp(10.76f, 11.78f, progress),
                lerp(7.00f, 12.62f, progress),
                lerp(10.13f, 10.24f, progress),
            )
            cubicTo(
                lerp(9.84f, 14.57f, progress),
                lerp(8.55f, 9.68f, progress),
                lerp(13.41f, 15.75f, progress),
                lerp(8.00f, 9.70f, progress),
                lerp(16.40f, 17.00f, progress),
                lerp(8.00f, 9.90f, progress),
            )
        }

    // 3. Index finger loop
    val indexFingerPath =
        Path().apply {
            moveTo(lerp(16.40f, 15.98f, progress), lerp(8.00f, 9.81f, progress))
            cubicTo(
                lerp(20.60f, 17.18f, progress),
                lerp(8.00f, 9.86f, progress),
                lerp(30.80f, 17.49f, progress),
                lerp(7.80f, 9.90f, progress),
                lerp(30.80f, 18.21f, progress),
                lerp(10.99f, 9.96f, progress),
            )
            cubicTo(
                lerp(30.80f, 19.08f, progress),
                lerp(12.03f, 10.03f, progress),
                lerp(29.24f, 19.62f, progress),
                lerp(12.16f, 10.11f, progress),
                lerp(26.60f, 20.35f, progress),
                lerp(12.04f, 10.26f, progress),
            )
        }

    // 4. Middle finger loop
    val middleFingerAlpha = lerp(1f, 0f, ((progress - 0.3f) / 0.1f).coerceIn(0f, 1f))
    val middleFingerPath =
        Path().apply {
            moveTo(lerp(17.60f, 17.47f, progress), lerp(11.59f, 9.96f, progress))
            cubicTo(
                lerp(19.50f, 17.80f, progress),
                lerp(11.50f, 10.00f, progress),
                lerp(23.18f, 18.20f, progress),
                lerp(11.87f, 10.05f, progress),
                lerp(26.60f, 18.80f, progress),
                lerp(12.04f, 10.15f, progress),
            )
            cubicTo(
                lerp(29.20f, 19.40f, progress),
                lerp(12.09f, 10.25f, progress),
                lerp(33.80f, 19.80f, progress),
                lerp(12.19f, 10.35f, progress),
                lerp(33.80f, 19.80f, progress),
                lerp(14.58f, 11.20f, progress),
            )
            cubicTo(
                lerp(33.80f, 19.60f, progress),
                lerp(15.45f, 12.00f, progress),
                lerp(33.18f, 19.20f, progress),
                lerp(16.41f, 12.20f, progress),
                lerp(31.40f, 18.80f, progress),
                lerp(16.50f, 12.40f, progress),
            )
        }

    // 5. Ring finger loop
    val ringFingerPath =
        Path().apply {
            moveTo(lerp(18.20f, 17.40f, progress), lerp(15.77f, 9.95f, progress))
            cubicTo(
                lerp(19.50f, 20.57f, progress),
                lerp(15.50f, 10.27f, progress),
                lerp(27.20f, 24.98f, progress),
                lerp(15.95f, 11.21f, progress),
                lerp(31.40f, 29.05f, progress),
                lerp(16.50f, 12.38f, progress),
            )
            cubicTo(
                lerp(35.00f, 31.77f, progress),
                lerp(16.96f, 13.18f, progress),
                lerp(36.20f, 33.19f, progress),
                lerp(21.15f, 13.38f, progress),
                lerp(31.40f, 34.06f, progress),
                lerp(20.55f, 14.51f, progress),
            )
        }

    // Draw paths
    drawPath(handBodyPath, color = tint, style = stroke)
    drawPath(upperWristPath, color = tint, style = stroke)
    drawPath(indexFingerPath, color = tint, style = stroke)
    if (middleFingerAlpha > 0f) {
        drawPath(
            middleFingerPath,
            color = tint.copy(alpha = tint.alpha * middleFingerAlpha),
            style = stroke,
        )
    }
    drawPath(ringFingerPath, color = tint, style = stroke)
}

private fun lerpPrimary(p0: Float, p1: Float, p2: Float, stage: Float): Float {
    return when {
        stage <= 1f -> lerp(p0, p1, stage)
        stage <= 2f -> lerp(p1, p2, stage - 1f)
        stage <= 3f -> lerp(p2, p1, stage - 2f)
        else -> lerp(p1, p0, stage - 3f)
    }
}

internal val EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT =
    spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 350f)

internal val EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)

internal val EXPRESSIVE_DEFAULT_EFFECTS_SPRING_COLOR =
    spring<Color>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)

internal const val INDICATOR_ANIMATION_START_DELAY_MILLIS = 450L
internal const val POST_INDICATOR_ANIMATION_DELAY_MILLIS = 200L

internal const val PRIMARY_INDICATOR_ANIMATION_DURATION_MILLIS = 617L
internal const val DISMISS_INDICATOR_ANIMATION_DURATION_MILLIS = 600L

internal val OneHandedGestureAction.indicatorDuration: Duration
    get() =
        if (this == OneHandedGestureAction.Dismiss) {
            DISMISS_INDICATOR_ANIMATION_DURATION_MILLIS.milliseconds
        } else {
            PRIMARY_INDICATOR_ANIMATION_DURATION_MILLIS.milliseconds
        }

private val PrimaryEasePinchIn1 = CubicBezierEasing(0.75f, 0.0f, 0.8f, 1.0f)
private val PrimaryEasePinchOut1 = CubicBezierEasing(0.31f, 0.0f, 0.833f, 1.0f)
private val PrimaryEasePinchIn2 = CubicBezierEasing(0.167f, 0.167f, 0.8f, 1.0f)
private val PrimaryEaseReturn = CubicBezierEasing(0.31f, 0.0f, 0.25f, 1.0f)
private val DismissEaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
