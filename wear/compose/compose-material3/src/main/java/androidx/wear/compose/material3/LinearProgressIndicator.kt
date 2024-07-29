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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
 * Small progress values that are larger than zero will be rounded up to at least the stroke width.
 *
 * [LinearProgressIndicator] sample:
 *
 * @sample androidx.wear.compose.material3.samples.LinearProgressIndicatorSample
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
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.colors(),
    strokeWidth: Dp = LinearProgressIndicatorDefaults.StrokeWidthLarge,
    enabled: Boolean = true,
) {
    require(strokeWidth >= LinearProgressIndicatorDefaults.StrokeWidthSmall) {
        "Stroke width cannot be less than ${LinearProgressIndicatorDefaults.StrokeWidthSmall}"
    }

    val coercedProgress = { progress().coerceIn(0f, 1f) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(strokeWidth)
                .padding(LinearProgressIndicatorDefaults.OuterHorizontalMargin)
                .scale(scaleX = if (isRtl) -1f else 1f, scaleY = 1f), // Flip X axis for RTL layouts
    ) {
        val progressPx = coercedProgress() * size.width

        // Draw the background
        drawLinearIndicator(
            start = 0f,
            end = size.width,
            brush = colors.trackBrush(enabled),
            strokeWidth = strokeWidth.toPx()
        )

        if (progressPx > 0) {
            // Draw the indicator
            drawLinearIndicator(
                start = 0f,
                end = progressPx,
                brush = colors.indicatorBrush(enabled),
                strokeWidth = strokeWidth.toPx(),
            )
        }

        // Draw the dot at the end of the line. The dot will be hidden when progress plus margin
        // would touch the dot
        val dotRadius = LinearProgressIndicatorDefaults.DotRadius.toPx()
        val dotMargin = LinearProgressIndicatorDefaults.DotMargin.toPx()

        if (progressPx + dotMargin * 2 + dotRadius * 2 < size.width) {
            drawLinearIndicatorDot(
                brush = colors.indicatorBrush(enabled),
                radius = dotRadius,
                offset = dotMargin
            )
        }
    }
}

/** Contains defaults for Linear Progress Indicator. */
object LinearProgressIndicatorDefaults {

    /**
     * Large stroke width for [LinearProgressIndicator].
     *
     * This is also the default stroke width for [LinearProgressIndicator].
     */
    val StrokeWidthLarge = 12.dp

    /**
     * Small stroke width for [LinearProgressIndicator].
     *
     * This is the minimum stroke value allowed for [LinearProgressIndicator] to ensure that the dot
     * shown at the end of the range can be distinguished.
     */
    val StrokeWidthSmall = 8.dp

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

    // need to adjust barStart and barEnd for the stroke caps
    val strokeCapOffset = strokeWidth / 2
    val adjustedBarStart = start + strokeCapOffset
    val adjustedBarEnd = end - strokeCapOffset

    if (adjustedBarEnd > adjustedBarStart) {
        // Draw progress line
        drawLine(
            brush = brush,
            start = Offset(adjustedBarStart, yOffset),
            end = Offset(adjustedBarEnd, yOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    } else {
        // For small values, draw a circle with diameter equal of stroke width
        drawCircle(
            brush = brush,
            radius = strokeCapOffset,
            center = Offset(strokeCapOffset, size.height / 2)
        )
    }
}

/** Draws a small dot at the end of the linear progress indicator. */
private fun DrawScope.drawLinearIndicatorDot(
    brush: Brush,
    radius: Float,
    offset: Float,
) {
    drawCircle(
        brush = brush,
        radius = radius,
        center = Offset(size.width - offset - radius, size.height / 2)
    )
}
