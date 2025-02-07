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

package androidx.wear.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/**
 * Material Design linear progress indicator.
 *
 * The [LinearProgressIndicator] displays progress as a horizontal bar, consisting of two visual
 * components:
 * - Track: The background line representing the total range of progress.
 * - Indicator: A colored line that fills the track, indicating the current progress value.
 *
 * The indicator also includes a small dot at the end of the progress line. This dot serves as an
 * accessibility feature to show the range of the indicator.
 *
 * Progress updates will be animated. Small progress values that are larger than zero will be
 * rounded up to at least the stroke width.
 *
 * [LinearProgressIndicator] sample:
 *
 * @sample androidx.wear.compose.material3.samples.LinearProgressIndicatorSample
 * @param progress The progress of this progress indicator where 0.0 represents no progress and 1.0
 *   represents completion. Values outside of this range are coerced into the range 0..1. Progress
 *   value changes will be animated.
 * @param modifier Modifier to be applied to the [LinearProgressIndicator].
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   colors for this progress indicator in different states.
 * @param strokeWidth The stroke width for the progress indicator. The minimum value is
 *   [LinearProgressIndicatorDefaults.StrokeWidthSmall] to ensure that the dot drawn at the end of
 *   the range can be distinguished.
 * @param enabled controls the enabled state. Although this component is not clickable, it can be
 *   contained within a clickable component. When enabled is `false`, this component will appear
 *   visually disabled.
 */
@Composable
public fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.colors(),
    strokeWidth: Dp = LinearProgressIndicatorDefaults.StrokeWidthLarge,
    enabled: Boolean = true,
) {
    require(strokeWidth >= LinearProgressIndicatorDefaults.StrokeWidthSmall) {
        "Stroke width cannot be less than ${LinearProgressIndicatorDefaults.StrokeWidthSmall}"
    }

    val progressAnimationSpec: AnimationSpec<Float> = linearProgressAnimationSpec
    val updatedProgress by rememberUpdatedState(progress)
    val animatedProgress = remember { Animatable(updatedProgress().coerceIn(0f, 1f)) }

    LaunchedEffect(Unit) {
        snapshotFlow(updatedProgress).collectLatest {
            val currentProgress = it.coerceIn(0f, 1f)
            animatedProgress.animateTo(currentProgress, progressAnimationSpec)
        }
    }

    LinearProgressIndicatorContent(
        progress = animatedProgress::value,
        modifier = modifier,
        colors = colors,
        strokeWidth = strokeWidth,
        enabled = enabled,
    )
}

/**
 * Linear progress indicator content with no progress animations.
 *
 * @param progress The progress of this progress indicator where 0.0 represents no progress and 1.0
 *   represents completion. Values outside of this range are coerced into the range 0..1.
 * @param modifier Modifier to be applied to the [LinearProgressIndicator].
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   colors for this progress indicator in different states.
 * @param strokeWidth The stroke width for the progress indicator. The minimum value is
 *   [LinearProgressIndicatorDefaults.StrokeWidthSmall] to ensure that the dot drawn at the end of
 *   the range can be distinguished.
 * @param enabled controls the enabled state. Although this component is not clickable, it can be
 *   contained within a clickable component. When enabled is `false`, this component will appear
 *   visually disabled.
 */
@Composable
private fun LinearProgressIndicatorContent(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.colors(),
    strokeWidth: Dp = LinearProgressIndicatorDefaults.StrokeWidthLarge,
    enabled: Boolean = true,
) {
    require(strokeWidth >= LinearProgressIndicatorDefaults.StrokeWidthSmall) {
        "Stroke width cannot be less than ${LinearProgressIndicatorDefaults.StrokeWidthSmall}"
    }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(strokeWidth)
                .padding(LinearProgressIndicatorDefaults.OuterHorizontalMargin)
                .scale(scaleX = if (isRtl) -1f else 1f, scaleY = 1f), // Flip X axis for RTL layouts
    ) {
        val progressPx = progress() * (size.width - strokeWidth.toPx())
        val strokeCapOffset = strokeWidth.toPx() / 2f

        // Draw the background
        drawLinearIndicator(
            start = strokeCapOffset,
            end = size.width - strokeCapOffset,
            brush = colors.trackBrush(enabled),
            strokeWidth = strokeWidth.toPx()
        )

        if (progressPx > 0f) {
            // Draw the indicator
            drawLinearIndicator(
                start = strokeCapOffset,
                end = strokeCapOffset + progressPx,
                brush = colors.indicatorBrush(enabled),
                strokeWidth = strokeWidth.toPx(),
            )
        }

        // Draw a dot at the end of the line.
        val dotRadius = LinearProgressIndicatorDefaults.DotRadius.toPx()
        val dotMargin = LinearProgressIndicatorDefaults.DotMargin.toPx()
        val dotCenterX = size.width - dotRadius - dotMargin
        val dotCenterY = size.height / 2f
        val distanceFromProgressToDot = dotCenterX - dotRadius - progressPx - strokeCapOffset * 2f

        // The dot will be hidden when the progress line would touch it.
        if (distanceFromProgressToDot > 0f) {
            // The dot will be scaled down when distance from progress line  to dot is smaller than
            // the margin.
            val scaleFraction = (distanceFromProgressToDot / dotMargin).coerceAtMost(1f)

            drawLinearIndicatorDot(
                brush = colors.indicatorBrush(enabled),
                radius = dotRadius,
                center = Offset(dotCenterX, dotCenterY),
                scaleFraction = scaleFraction
            )
        }
    }
}

/** Contains defaults for Linear Progress Indicator. */
public object LinearProgressIndicatorDefaults {

    /**
     * Large stroke width for [LinearProgressIndicator].
     *
     * This is also the default stroke width for [LinearProgressIndicator].
     */
    public val StrokeWidthLarge: Dp = 12.dp

    /**
     * Small stroke width for [LinearProgressIndicator].
     *
     * This is the minimum stroke value allowed for [LinearProgressIndicator] to ensure that the dot
     * shown at the end of the range can be distinguished.
     */
    public val StrokeWidthSmall: Dp = 8.dp

    /** Radius for the dot shown at the end of the [LinearProgressIndicator]. */
    internal val DotRadius = 2.dp

    /** Margin for the dot shown at the end of the [LinearProgressIndicator]. */
    internal val DotMargin = 4.dp

    /** Horizontal padding for the [LinearProgressIndicator]. */
    internal val OuterHorizontalMargin = 2.dp
}

/** Draws a line for the linear indicator segment. */
private fun DrawScope.drawLinearIndicator(
    start: Float,
    end: Float,
    brush: Brush,
    strokeWidth: Float,
) {
    // Start drawing from the vertical center of the stroke
    val yOffset = size.height / 2
    if (end > start) {
        // Draw progress line
        drawLine(
            brush = brush,
            start = Offset(start, yOffset),
            end = Offset(end, yOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

/** Draws a small dot at the end of the linear progress indicator. */
private fun DrawScope.drawLinearIndicatorDot(
    brush: Brush,
    radius: Float,
    center: Offset,
    scaleFraction: Float = 1f
) {
    // Scale down the dot by the scale fraction.
    val scaledDotRadius = radius * scaleFraction
    // Apply the scale fraction alpha to the dot color.
    val alpha = scaleFraction.coerceAtMost(1f)
    // Draw the dot with scaled down radius and alpha color.
    drawCircle(brush = brushWithAlpha(brush, alpha), radius = scaledDotRadius, center = center)
}

/** Progress animation spec for [LinearProgressIndicator] */
internal val linearProgressAnimationSpec: AnimationSpec<Float>
    @Composable get() = MaterialTheme.motionScheme.defaultEffectsSpec()
