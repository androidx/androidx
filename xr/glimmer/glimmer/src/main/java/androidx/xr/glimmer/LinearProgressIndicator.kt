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

package androidx.xr.glimmer

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceIn
import androidx.xr.glimmer.internal.progressindicator.IncreaseVerticalSemanticsBounds
import androidx.xr.glimmer.internal.progressindicator.LinearIndicatorWidth
import androidx.xr.glimmer.internal.progressindicator.LinearStrokeCap
import androidx.xr.glimmer.internal.progressindicator.ProgressIndicatorCanvas
import androidx.xr.glimmer.internal.progressindicator.ProgressIndicatorGapSize
import androidx.xr.glimmer.internal.progressindicator.ProgressIndicatorStrokeWidth
import androidx.xr.glimmer.internal.progressindicator.linearIndeterminateFirstLineHeadAnimationSpec
import androidx.xr.glimmer.internal.progressindicator.linearIndeterminateFirstLineTailAnimationSpec
import androidx.xr.glimmer.internal.progressindicator.linearIndeterminateSecondLineHeadAnimationSpec
import androidx.xr.glimmer.internal.progressindicator.linearIndeterminateSecondLineTailAnimationSpec
import kotlin.math.abs
import kotlin.math.min

/**
 * A determinate linear progress indicator that shows how far a process has advanced.
 *
 * The indicator's height sets the thickness of the progress line and the track. There is no
 * animation between [progress] values; animate the value before passing it in if needed.
 *
 * @sample androidx.xr.glimmer.samples.LinearProgressIndicatorSample
 * @param progress the progress of this indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range
 * @param modifier the [Modifier] to be applied to this indicator
 * @param color the [Color] of the progress line
 * @param trackColor the [Color] of the track that the progress line hasn't reached yet. If
 *   [Color.Unspecified], uses a less prominent shade of the content color provided by the nearest
 *   [surface].
 * @param gapSize the visible gap between the progress line and the track
 */
@Composable
public fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = LinearProgressIndicatorDefaults.color,
    trackColor: Color = Color.Unspecified,
    gapSize: Dp = LinearProgressIndicatorDefaults.GapSize,
) {
    val coercedProgress = { progress().fastCoerceIn(0f, 1f) }
    ProgressIndicatorCanvas(
        modifier
            .then(IncreaseVerticalSemanticsBounds)
            .semantics(mergeDescendants = true) {
                // Check for NaN, as the ProgressBarRangeInfo will throw an exception.
                progressBarRangeInfo =
                    ProgressBarRangeInfo(coercedProgress().takeUnless { it.isNaN() } ?: 0f, 0f..1f)
            }
            .size(LinearIndicatorWidth, LinearIndicatorHeight),
        trackColor,
    ) { resolvedTrackColor ->
        val strokeWidthPx = size.height
        val adjustedGapSize =
            if (strokeWidthPx > size.width) {
                gapSize
            } else {
                gapSize + strokeWidthPx.toDp()
            }
        val gapSizeFraction = adjustedGapSize / size.width.toDp()
        val currentCoercedProgress = coercedProgress()

        // track
        val trackStartFraction =
            currentCoercedProgress + min(currentCoercedProgress, gapSizeFraction)
        if (trackStartFraction <= 1f) {
            drawLinearIndicator(
                trackStartFraction,
                1f,
                resolvedTrackColor,
                strokeWidthPx,
                LinearStrokeCap,
            )
        }
        // indicator
        drawLinearIndicator(0f, currentCoercedProgress, color, strokeWidthPx, LinearStrokeCap)
    }
}

/**
 * An indeterminate linear progress indicator that shows that a process is ongoing, without
 * indicating how far it has advanced.
 *
 * The indicator's height sets the thickness of the progress lines and the track.
 *
 * @sample androidx.xr.glimmer.samples.IndeterminateLinearProgressIndicatorSample
 * @param modifier the [Modifier] to be applied to this indicator
 * @param color the [Color] of the moving progress lines
 * @param trackColor the [Color] of the track behind the progress lines. If [Color.Unspecified],
 *   uses a less prominent shade of the content color provided by the nearest [surface].
 * @param gapSize the visible gap between the progress lines and the track
 */
@Composable
public fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = LinearProgressIndicatorDefaults.color,
    trackColor: Color = Color.Unspecified,
    gapSize: Dp = LinearProgressIndicatorDefaults.GapSize,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val firstLineHead =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateFirstLineHeadAnimationSpec,
        )
    val firstLineTail =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateFirstLineTailAnimationSpec,
        )
    val secondLineHead =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateSecondLineHeadAnimationSpec,
        )
    val secondLineTail =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateSecondLineTailAnimationSpec,
        )
    ProgressIndicatorCanvas(
        modifier
            .then(IncreaseVerticalSemanticsBounds)
            .progressSemantics()
            .size(LinearIndicatorWidth, LinearIndicatorHeight),
        trackColor,
    ) { resolvedTrackColor ->
        val strokeWidthPx = size.height
        val adjustedGapSize =
            if (strokeWidthPx > size.width) {
                gapSize
            } else {
                gapSize + strokeWidthPx.toDp()
            }
        val gapSizeFraction = adjustedGapSize / size.width.toDp()

        // Track before line 1
        if (firstLineHead.value < 1f - gapSizeFraction) {
            val start = if (firstLineHead.value > 0) firstLineHead.value + gapSizeFraction else 0f
            drawLinearIndicator(start, 1f, resolvedTrackColor, strokeWidthPx, LinearStrokeCap)
        }

        // Line 1
        if (firstLineHead.value - firstLineTail.value > 0) {
            drawLinearIndicator(
                firstLineHead.value,
                firstLineTail.value,
                color,
                strokeWidthPx,
                LinearStrokeCap,
            )
        }

        // Track between line 1 and line 2
        if (firstLineTail.value > gapSizeFraction) {
            val start = if (secondLineHead.value > 0) secondLineHead.value + gapSizeFraction else 0f
            val end = if (firstLineTail.value < 1f) firstLineTail.value - gapSizeFraction else 1f
            drawLinearIndicator(start, end, resolvedTrackColor, strokeWidthPx, LinearStrokeCap)
        }

        // Line 2
        if (secondLineHead.value - secondLineTail.value > 0) {
            drawLinearIndicator(
                secondLineHead.value,
                secondLineTail.value,
                color,
                strokeWidthPx,
                LinearStrokeCap,
            )
        }

        // Track after line 2
        if (secondLineTail.value > gapSizeFraction) {
            val end = if (secondLineTail.value < 1) secondLineTail.value - gapSizeFraction else 1f
            drawLinearIndicator(0f, end, resolvedTrackColor, strokeWidthPx, LinearStrokeCap)
        }
    }
}

/** Contains default values used by [LinearProgressIndicator]. */
public object LinearProgressIndicatorDefaults {
    /** Default color of the progress line. */
    public val color: Color
        @Composable get() = GlimmerTheme.colors.primary

    /** Default visible gap between the progress line and the track. */
    public val GapSize: Dp = ProgressIndicatorGapSize
}

private fun DrawScope.drawLinearIndicator(
    startFraction: Float,
    endFraction: Float,
    color: Color,
    strokeWidth: Float,
    strokeCap: StrokeCap,
) {
    val width = size.width
    val height = size.height
    // Start drawing from the vertical center of the stroke
    val yOffset = height / 2

    val isLtr = layoutDirection == LayoutDirection.Ltr
    val barStart = (if (isLtr) startFraction else 1f - endFraction) * width
    val barEnd = (if (isLtr) endFraction else 1f - startFraction) * width

    // if there isn't enough space to draw the stroke caps, fall back to StrokeCap.Butt
    if (strokeCap == StrokeCap.Butt || strokeWidth > width) {
        // Progress line
        drawLine(color, Offset(barStart, yOffset), Offset(barEnd, yOffset), strokeWidth)
    } else {
        // need to adjust barStart and barEnd for the stroke caps
        val strokeCapOffset = strokeWidth / 2
        val adjustedBarStart = barStart.fastCoerceIn(strokeCapOffset, width - strokeCapOffset)
        val adjustedBarEnd = barEnd.fastCoerceIn(strokeCapOffset, width - strokeCapOffset)

        if (abs(endFraction - startFraction) > 0) {
            // Progress line
            drawLine(
                color,
                Offset(adjustedBarStart, yOffset),
                Offset(adjustedBarEnd, yOffset),
                strokeWidth,
                strokeCap,
            )
        }
    }
}

private val LinearIndicatorHeight = ProgressIndicatorStrokeWidth
