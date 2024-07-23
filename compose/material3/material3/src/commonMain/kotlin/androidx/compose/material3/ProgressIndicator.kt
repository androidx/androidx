/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.ProgressIndicatorDefaults.drawStopIndicator
import androidx.compose.material3.internal.IncreaseVerticalSemanticsBounds
import androidx.compose.material3.tokens.CircularProgressIndicatorTokens
import androidx.compose.material3.tokens.LinearProgressIndicatorTokens
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.material3.tokens.ProgressIndicatorTokens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Determinate Material Design linear progress indicator</a>.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Linear progress indicator
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqdiyyvh-1P-progress-indicator-configurations.png?alt=media)
 *
 * By default there is no animation between [progress] values. You can use
 * [ProgressIndicatorDefaults.ProgressAnimationSpec] as the default recommended [AnimationSpec] when
 * animating progress, such as in the following example:
 *
 * @sample androidx.compose.material3.samples.LinearProgressIndicatorSample
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param trackColor color of the track behind the indicator, visible when the progress has not
 *   reached the area of the overall indicator yet
 * @param strokeCap stroke cap to use for the ends of this progress indicator
 */
@Deprecated(
    message =
        "Use the overload that takes `gapSize` and `drawStopIndicator`, see " +
            "`LegacyLinearProgressIndicatorSample` on how to restore the previous behavior",
    replaceWith =
        ReplaceWith(
            "LinearProgressIndicator(progress, modifier, color, trackColor, strokeCap, " +
                "gapSize, drawStopIndicator)"
        ),
    level = DeprecationLevel.HIDDEN
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
) {
    LinearProgressIndicator(
        progress,
        modifier,
        color,
        trackColor,
        strokeCap,
        gapSize = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize
    )
}

/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Determinate Material Design linear progress indicator</a>.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Linear progress indicator
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqdiyyvh-1P-progress-indicator-configurations.png?alt=media)
 *
 * By default there is no animation between [progress] values. You can use
 * [ProgressIndicatorDefaults.ProgressAnimationSpec] as the default recommended [AnimationSpec] when
 * animating progress, such as in the following example:
 *
 * @sample androidx.compose.material3.samples.LinearProgressIndicatorSample
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param trackColor color of the track behind the indicator, visible when the progress has not
 *   reached the area of the overall indicator yet
 * @param strokeCap stroke cap to use for the ends of this progress indicator
 * @param gapSize size of the gap between the progress indicator and the track
 * @param drawStopIndicator lambda that will be called to draw the stop indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
    drawStopIndicator: DrawScope.() -> Unit = {
        drawStopIndicator(
            drawScope = this,
            stopSize = ProgressIndicatorDefaults.LinearTrackStopIndicatorSize,
            color = color,
            strokeCap = strokeCap
        )
    },
) {
    val coercedProgress = { progress().coerceIn(0f, 1f) }
    Canvas(
        modifier
            .then(IncreaseVerticalSemanticsBounds)
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(coercedProgress(), 0f..1f)
            }
            .size(LinearIndicatorWidth, LinearIndicatorHeight)
    ) {
        val strokeWidth = size.height
        val adjustedGapSize =
            if (strokeCap == StrokeCap.Butt || size.height > size.width) {
                gapSize
            } else {
                gapSize + strokeWidth.toDp()
            }
        val gapSizeFraction = adjustedGapSize / size.width.toDp()
        val currentCoercedProgress = coercedProgress()

        // track
        val trackStartFraction =
            currentCoercedProgress + min(currentCoercedProgress, gapSizeFraction)
        if (trackStartFraction <= 1f) {
            drawLinearIndicator(trackStartFraction, 1f, trackColor, strokeWidth, strokeCap)
        }
        // indicator
        drawLinearIndicator(0f, currentCoercedProgress, color, strokeWidth, strokeCap)
        // stop
        drawStopIndicator(this)
    }
}

/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Indeterminate Material Design linear progress indicator</a>.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Linear progress indicator
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqdiyyvh-1P-progress-indicator-configurations.png?alt=media)
 *
 * @sample androidx.compose.material3.samples.IndeterminateLinearProgressIndicatorSample
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param trackColor color of the track behind the indicator, visible when the progress has not
 *   reached the area of the overall indicator yet
 * @param strokeCap stroke cap to use for the ends of this progress indicator
 */
@Deprecated(
    message =
        "Use the overload that takes `gapSize`, see `" +
            "LegacyIndeterminateLinearProgressIndicatorSample` on how to restore the previous behavior",
    replaceWith =
        ReplaceWith("LinearProgressIndicator(modifier, color, trackColor, strokeCap, gapSize)"),
    level = DeprecationLevel.HIDDEN
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
) {
    LinearProgressIndicator(
        modifier,
        color,
        trackColor,
        strokeCap,
        gapSize = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
    )
}

/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Indeterminate Material Design linear progress indicator</a>.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Linear progress indicator
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqdiyyvh-1P-progress-indicator-configurations.png?alt=media)
 *
 * @sample androidx.compose.material3.samples.IndeterminateLinearProgressIndicatorSample
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param trackColor color of the track behind the indicator, visible when the progress has not
 *   reached the area of the overall indicator yet
 * @param strokeCap stroke cap to use for the ends of this progress indicator
 * @param gapSize size of the gap between the progress indicator and the track
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val firstLineHead =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateFirstLineHeadAnimationSpec
        )
    val firstLineTail =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateFirstLineTailAnimationSpec
        )
    val secondLineHead =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateSecondLineHeadAnimationSpec
        )
    val secondLineTail =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateSecondLineTailAnimationSpec
        )
    Canvas(
        modifier
            .then(IncreaseVerticalSemanticsBounds)
            .progressSemantics()
            .size(LinearIndicatorWidth, LinearIndicatorHeight)
    ) {
        val strokeWidth = size.height
        val adjustedGapSize =
            if (strokeCap == StrokeCap.Butt || size.height > size.width) {
                gapSize
            } else {
                gapSize + strokeWidth.toDp()
            }
        val gapSizeFraction = adjustedGapSize / size.width.toDp()

        // Track before line 1
        if (firstLineHead.value < 1f - gapSizeFraction) {
            val start = if (firstLineHead.value > 0) firstLineHead.value + gapSizeFraction else 0f
            drawLinearIndicator(start, 1f, trackColor, strokeWidth, strokeCap)
        }

        // Line 1
        if (firstLineHead.value - firstLineTail.value > 0) {
            drawLinearIndicator(
                firstLineHead.value,
                firstLineTail.value,
                color,
                strokeWidth,
                strokeCap,
            )
        }

        // Track between line 1 and line 2
        if (firstLineTail.value > gapSizeFraction) {
            val start = if (secondLineHead.value > 0) secondLineHead.value + gapSizeFraction else 0f
            val end = if (firstLineTail.value < 1f) firstLineTail.value - gapSizeFraction else 1f
            drawLinearIndicator(start, end, trackColor, strokeWidth, strokeCap)
        }

        // Line 2
        if (secondLineHead.value - secondLineTail.value > 0) {
            drawLinearIndicator(
                secondLineHead.value,
                secondLineTail.value,
                color,
                strokeWidth,
                strokeCap,
            )
        }

        // Track after line 2
        if (secondLineTail.value > gapSizeFraction) {
            val end = if (secondLineTail.value < 1) secondLineTail.value - gapSizeFraction else 1f
            drawLinearIndicator(0f, end, trackColor, strokeWidth, strokeCap)
        }
    }
}

@Deprecated(
    message = "Use the overload that takes `progress` as a lambda",
    replaceWith =
        ReplaceWith(
            "LinearProgressIndicator(\n" +
                "progress = { progress },\n" +
                "modifier = modifier,\n" +
                "color = color,\n" +
                "trackColor = trackColor,\n" +
                "strokeCap = strokeCap,\n" +
                ")"
        )
)
@Composable
fun LinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
) =
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = strokeCap,
    )

@Suppress("DEPRECATION")
@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun LinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
) =
    LinearProgressIndicator(
        progress,
        modifier,
        color,
        trackColor,
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    )

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
) =
    LinearProgressIndicator(
        modifier,
        color,
        trackColor,
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    )

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
    if (strokeCap == StrokeCap.Butt || height > width) {
        // Progress line
        drawLine(color, Offset(barStart, yOffset), Offset(barEnd, yOffset), strokeWidth)
    } else {
        // need to adjust barStart and barEnd for the stroke caps
        val strokeCapOffset = strokeWidth / 2
        val coerceRange = strokeCapOffset..(width - strokeCapOffset)
        val adjustedBarStart = barStart.coerceIn(coerceRange)
        val adjustedBarEnd = barEnd.coerceIn(coerceRange)

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

/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Determinate Material Design circular progress indicator</a>.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Circular progress indicator
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqdiyyvh-1P-progress-indicator-configurations.png?alt=media)
 *
 * By default there is no animation between [progress] values. You can use
 * [ProgressIndicatorDefaults.ProgressAnimationSpec] as the default recommended [AnimationSpec] when
 * animating progress, such as in the following example:
 *
 * @sample androidx.compose.material3.samples.CircularProgressIndicatorSample
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param strokeWidth stroke width of this progress indicator
 * @param trackColor color of the track behind the indicator, visible when the progress has not
 *   reached the area of the overall indicator yet
 * @param strokeCap stroke cap to use for the ends of this progress indicator
 */
@Deprecated(
    message =
        "Use the overload that takes `gapSize`, see " +
            "`LegacyCircularProgressIndicatorSample` on how to restore the previous behavior",
    replaceWith =
        ReplaceWith(
            "CircularProgressIndicator(progress, modifier, color, strokeWidth, trackColor, " +
                "strokeCap, gapSize)"
        ),
    level = DeprecationLevel.HIDDEN
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = ProgressIndicatorDefaults.circularDeterminateTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
) {
    CircularProgressIndicator(
        progress,
        modifier,
        color,
        strokeWidth,
        trackColor,
        strokeCap,
        gapSize = ProgressIndicatorDefaults.CircularIndicatorTrackGapSize
    )
}

/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Determinate Material Design circular progress indicator</a>.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Circular progress indicator
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqdiyyvh-1P-progress-indicator-configurations.png?alt=media)
 *
 * By default there is no animation between [progress] values. You can use
 * [ProgressIndicatorDefaults.ProgressAnimationSpec] as the default recommended [AnimationSpec] when
 * animating progress, such as in the following example:
 *
 * @sample androidx.compose.material3.samples.CircularProgressIndicatorSample
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param strokeWidth stroke width of this progress indicator
 * @param trackColor color of the track behind the indicator, visible when the progress has not
 *   reached the area of the overall indicator yet
 * @param strokeCap stroke cap to use for the ends of this progress indicator
 * @param gapSize size of the gap between the progress indicator and the track
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = ProgressIndicatorDefaults.circularDeterminateTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.CircularIndicatorTrackGapSize,
) {
    val coercedProgress = { progress().coerceIn(0f, 1f) }
    val stroke = with(LocalDensity.current) { Stroke(width = strokeWidth.toPx(), cap = strokeCap) }
    Canvas(
        modifier
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(coercedProgress(), 0f..1f)
            }
            .size(CircularIndicatorDiameter)
    ) {
        // Start at 12 o'clock
        val startAngle = 270f
        val sweep = coercedProgress() * 360f
        val adjustedGapSize =
            if (strokeCap == StrokeCap.Butt || size.height > size.width) {
                gapSize
            } else {
                gapSize + strokeWidth
            }
        val gapSizeSweep = (adjustedGapSize.value / (PI * size.width.toDp().value).toFloat()) * 360f

        drawCircularIndicator(
            startAngle + sweep + min(sweep, gapSizeSweep),
            360f - sweep - min(sweep, gapSizeSweep) * 2,
            trackColor,
            stroke
        )
        drawDeterminateCircularIndicator(startAngle, sweep, color, stroke)
    }
}

/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Indeterminate Material Design circular progress indicator</a>.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Circular progress indicator
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqdiyyvh-1P-progress-indicator-configurations.png?alt=media)
 *
 * @sample androidx.compose.material3.samples.IndeterminateCircularProgressIndicatorSample
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param strokeWidth stroke width of this progress indicator
 * @param trackColor color of the track behind the indicator, visible when the progress has not
 *   reached the area of the overall indicator yet
 * @param strokeCap stroke cap to use for the ends of this progress indicator
 */
@Deprecated(
    message = "Use the overload that takes `gapSize`",
    replaceWith =
        ReplaceWith(
            "CircularProgressIndicator(modifier, color, strokeWidth, trackColor, strokeCap, " +
                "gapSize)"
        ),
    level = DeprecationLevel.HIDDEN
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
) =
    CircularProgressIndicator(
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
        trackColor = trackColor,
        strokeCap = strokeCap,
        gapSize = ProgressIndicatorDefaults.CircularIndicatorTrackGapSize
    )

/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Indeterminate Material Design circular progress indicator</a>.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Circular progress indicator
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqdiyyvh-1P-progress-indicator-configurations.png?alt=media)
 *
 * @sample androidx.compose.material3.samples.IndeterminateCircularProgressIndicatorSample
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param strokeWidth stroke width of this progress indicator
 * @param trackColor color of the track behind the indicator, visible when the progress has not
 *   reached the area of the overall indicator yet
 * @param strokeCap stroke cap to use for the ends of this progress indicator
 * @param gapSize size of the gap between the progress indicator and the track
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.CircularIndicatorTrackGapSize
) {
    val stroke = with(LocalDensity.current) { Stroke(width = strokeWidth.toPx(), cap = strokeCap) }

    val infiniteTransition = rememberInfiniteTransition()
    // A global rotation that does a 360 degrees rotation in 6 seconds.
    val globalRotation =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = CircularGlobalRotationDegreesTarget,
            animationSpec = circularIndeterminateGlobalRotationAnimationSpec
        )

    // An additional rotation that moves by 90 degrees in 500ms and then rest for 1 second.
    val additionalRotation =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = CircularAdditionalRotationDegreesTarget,
            animationSpec = circularIndeterminateRotationAnimationSpec
        )

    // Indicator progress animation that will be changing the progress up and down as the indicator
    // rotates.
    val progressAnimation =
        infiniteTransition.animateFloat(
            initialValue = CircularIndeterminateMinProgress,
            targetValue = CircularIndeterminateMaxProgress,
            animationSpec = circularIndeterminateProgressAnimationSpec
        )

    Canvas(modifier.progressSemantics().size(CircularIndicatorDiameter)) {
        val sweep = progressAnimation.value * 360f
        val adjustedGapSize =
            if (strokeCap == StrokeCap.Butt || size.height > size.width) {
                gapSize
            } else {
                gapSize + strokeWidth
            }
        val gapSizeSweep = (adjustedGapSize.value / (PI * size.width.toDp().value).toFloat()) * 360f

        rotate(globalRotation.value + additionalRotation.value) {
            drawCircularIndicator(
                sweep + min(sweep, gapSizeSweep),
                360f - sweep - min(sweep, gapSizeSweep) * 2,
                trackColor,
                stroke
            )
            drawDeterminateCircularIndicator(startAngle = 0f, sweep, color, stroke)
        }
    }
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Use the overload that takes `progress` as a lambda",
    replaceWith =
        ReplaceWith(
            "CircularProgressIndicator(\n" +
                "progress = { progress },\n" +
                "modifier = modifier,\n" +
                "color = color,\n" +
                "strokeWidth = strokeWidth,\n" +
                "trackColor = trackColor,\n" +
                "strokeCap = strokeCap,\n" +
                ")"
        )
)
@Composable
fun CircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = ProgressIndicatorDefaults.circularTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
) =
    CircularProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
        trackColor = trackColor,
        strokeCap = strokeCap,
    )

@Suppress("DEPRECATION")
@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun CircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth
) =
    CircularProgressIndicator(
        progress,
        modifier,
        color,
        strokeWidth,
        trackColor = ProgressIndicatorDefaults.circularTrackColor,
        strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
    )

@Suppress("DEPRECATION")
@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth
) =
    CircularProgressIndicator(
        modifier,
        color,
        strokeWidth,
        trackColor = ProgressIndicatorDefaults.circularTrackColor,
        strokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
    )

private fun DrawScope.drawCircularIndicator(
    startAngle: Float,
    sweep: Float,
    color: Color,
    stroke: Stroke
) {
    // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
    // To do this we need to remove half the stroke width from the total diameter for both sides.
    val diameterOffset = stroke.width / 2
    val arcDimen = size.width - 2 * diameterOffset
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(diameterOffset, diameterOffset),
        size = Size(arcDimen, arcDimen),
        style = stroke
    )
}

private fun DrawScope.drawCircularIndicatorTrack(color: Color, stroke: Stroke) =
    drawCircularIndicator(0f, 360f, color, stroke)

private fun DrawScope.drawDeterminateCircularIndicator(
    startAngle: Float,
    sweep: Float,
    color: Color,
    stroke: Stroke
) = drawCircularIndicator(startAngle, sweep, color, stroke)

private fun DrawScope.drawIndeterminateCircularIndicator(
    startAngle: Float,
    strokeWidth: Dp,
    sweep: Float,
    color: Color,
    stroke: Stroke
) {
    val strokeCapOffset =
        if (stroke.cap == StrokeCap.Butt) {
            0f
        } else {
            // Length of arc is angle * radius
            // Angle (radians) is length / radius
            // The length should be the same as the stroke width for calculating the min angle
            (180.0 / PI).toFloat() * (strokeWidth / (CircularIndicatorDiameter / 2)) / 2f
        }

    // Adding a stroke cap draws half the stroke width behind the start point, so we want to
    // move it forward by that amount so the arc visually appears in the correct place
    val adjustedStartAngle = startAngle + strokeCapOffset

    // When the start and end angles are in the same place, we still want to draw a small sweep, so
    // the stroke caps get added on both ends and we draw the correct minimum length arc
    val adjustedSweep = max(sweep, 0.1f)

    drawCircularIndicator(adjustedStartAngle, adjustedSweep, color, stroke)
}

/**
 * Contains the default values used for [LinearProgressIndicator] and [CircularProgressIndicator].
 */
object ProgressIndicatorDefaults {
    /** Default color for a linear progress indicator. */
    val linearColor: Color
        @Composable get() = ProgressIndicatorTokens.ActiveIndicatorColor.value

    /** Default color for a circular progress indicator. */
    val circularColor: Color
        @Composable get() = ProgressIndicatorTokens.ActiveIndicatorColor.value

    /** Default track color for a linear progress indicator. */
    val linearTrackColor: Color
        @Composable get() = ProgressIndicatorTokens.TrackColor.value

    /** Default track color for a circular progress indicator. */
    @Deprecated(
        "Renamed to circularDeterminateTrackColor or circularIndeterminateTrackColor",
        ReplaceWith("ProgressIndicatorDefaults.circularIndeterminateTrackColor"),
        DeprecationLevel.WARNING
    )
    val circularTrackColor: Color
        @Composable get() = Color.Transparent

    /** Default track color for a circular determinate progress indicator. */
    val circularDeterminateTrackColor: Color
        @Composable get() = ProgressIndicatorTokens.TrackColor.value

    /** Default track color for a circular indeterminate progress indicator. */
    val circularIndeterminateTrackColor: Color
        @Composable get() = Color.Transparent

    /** Default stroke width for a circular progress indicator. */
    val CircularStrokeWidth: Dp = CircularProgressIndicatorTokens.TrackThickness

    /** Default stroke cap for a linear progress indicator. */
    val LinearStrokeCap: StrokeCap = StrokeCap.Round

    /** Default stroke cap for a determinate circular progress indicator. */
    val CircularDeterminateStrokeCap: StrokeCap = StrokeCap.Round

    /** Default stroke cap for an indeterminate circular progress indicator. */
    val CircularIndeterminateStrokeCap: StrokeCap = StrokeCap.Round

    /** Default track stop indicator size for a linear progress indicator. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3Api
    @ExperimentalMaterial3Api
    val LinearTrackStopIndicatorSize: Dp = LinearProgressIndicatorTokens.StopSize

    /** Default indicator track gap size for a linear progress indicator. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3Api
    @ExperimentalMaterial3Api
    val LinearIndicatorTrackGapSize: Dp = LinearProgressIndicatorTokens.TrackActiveSpace

    /** Default indicator track gap size for a circular progress indicator. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3Api
    @ExperimentalMaterial3Api
    val CircularIndicatorTrackGapSize: Dp = CircularProgressIndicatorTokens.TrackActiveSpace

    /**
     * The default [AnimationSpec] that should be used when animating between progress in a
     * determinate progress indicator.
     */
    val ProgressAnimationSpec =
        SpringSpec(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow,
            // The default threshold is 0.01, or 1% of the overall progress range, which is quite
            // large and noticeable. We purposefully choose a smaller threshold.
            visibilityThreshold = 1 / 1000f
        )

    /**
     * Draws the stop indicator at the end of the track.
     *
     * @param drawScope the [DrawScope]
     * @param stopSize size of this stop indicator, it cannot be bigger than the track's height
     * @param color color of this stop indicator
     * @param strokeCap stroke cap to use for the ends of this stop indicator
     */
    fun drawStopIndicator(
        drawScope: DrawScope,
        stopSize: Dp,
        color: Color,
        strokeCap: StrokeCap,
    ) {
        with(drawScope) {
            val adjustedStopSize =
                min(stopSize.toPx(), size.height) // Stop can't be bigger than track
            val stopOffset = (size.height - adjustedStopSize) / 2 // Offset from end
            if (strokeCap == StrokeCap.Round) {
                drawCircle(
                    color = color,
                    radius = adjustedStopSize / 2f,
                    center =
                        Offset(
                            x = size.width - (adjustedStopSize / 2f) - stopOffset,
                            y = size.height / 2f
                        )
                )
            } else {
                drawRect(
                    color = color,
                    topLeft =
                        Offset(
                            x = size.width - adjustedStopSize - stopOffset,
                            y = (size.height - adjustedStopSize) / 2f
                        ),
                    size = Size(width = adjustedStopSize, height = adjustedStopSize)
                )
            }
        }
    }
}

/** A global animation spec for indeterminate circular progress indicator. */
internal val circularIndeterminateGlobalRotationAnimationSpec
    get() =
        infiniteRepeatable<Float>(
            animation = tween(CircularAnimationProgressDuration, easing = LinearEasing)
        )

/**
 * An animation spec for indeterminate circular progress indicators that infinitely rotates a 360
 * degrees.
 */
internal val circularIndeterminateRotationAnimationSpec
    get() =
        infiniteRepeatable(
            animation =
                keyframes {
                    durationMillis = CircularAnimationProgressDuration // 6000ms
                    90f at
                        CircularAnimationAdditionalRotationDuration using
                        MotionTokens.EasingEmphasizedDecelerateCubicBezier // 300ms
                    90f at CircularAnimationAdditionalRotationDelay // hold till 1500ms
                    180f at
                        CircularAnimationAdditionalRotationDuration +
                            CircularAnimationAdditionalRotationDelay // 1800ms
                    180f at CircularAnimationAdditionalRotationDelay * 2 // hold till 3000ms
                    270f at
                        CircularAnimationAdditionalRotationDuration +
                            CircularAnimationAdditionalRotationDelay * 2 // 3300ms
                    270f at CircularAnimationAdditionalRotationDelay * 3 // hold till 4500ms
                    360f at
                        CircularAnimationAdditionalRotationDuration +
                            CircularAnimationAdditionalRotationDelay * 3 // 4800ms
                    360f at CircularAnimationProgressDuration // hold till 6000ms
                }
        )

/** An animation spec for indeterminate circular progress indicators progress motion. */
internal val circularIndeterminateProgressAnimationSpec
    get() =
        infiniteRepeatable(
            animation =
                keyframes {
                    durationMillis = CircularAnimationProgressDuration // 6000ms
                    CircularIndeterminateMaxProgress at
                        CircularAnimationProgressDuration / 2 using
                        CircularProgressEasing // 3000ms
                    CircularIndeterminateMinProgress at CircularAnimationProgressDuration
                }
        )

/** An animation spec for indeterminate linear progress indicator first line head position. */
internal val linearIndeterminateFirstLineHeadAnimationSpec
    get() =
        infiniteRepeatable(
            animation =
                keyframes {
                    durationMillis = LinearAnimationDuration
                    0f at FirstLineHeadDelay using LinearIndeterminateProgressEasing
                    1f at FirstLineHeadDuration + FirstLineHeadDelay
                }
        )

/** An animation spec for indeterminate linear progress indicator first line tail position. */
internal val linearIndeterminateFirstLineTailAnimationSpec
    get() =
        infiniteRepeatable(
            animation =
                keyframes {
                    durationMillis = LinearAnimationDuration
                    0f at FirstLineTailDelay using LinearIndeterminateProgressEasing
                    1f at FirstLineTailDuration + FirstLineTailDelay
                }
        )

/** An animation spec for indeterminate linear progress indicator second line head position. */
internal val linearIndeterminateSecondLineHeadAnimationSpec
    get() =
        infiniteRepeatable(
            animation =
                keyframes {
                    durationMillis = LinearAnimationDuration
                    0f at SecondLineHeadDelay using LinearIndeterminateProgressEasing
                    1f at SecondLineHeadDuration + SecondLineHeadDelay
                }
        )

/** An animation spec for indeterminate linear progress indicator second line tail position. */
internal val linearIndeterminateSecondLineTailAnimationSpec
    get() =
        infiniteRepeatable(
            animation =
                keyframes {
                    durationMillis = LinearAnimationDuration
                    0f at SecondLineTailDelay using LinearIndeterminateProgressEasing
                    1f at SecondLineTailDuration + SecondLineTailDelay
                }
        )
// LinearProgressIndicator Material specs

// Width is given in the spec but not defined as a token.
/*@VisibleForTesting*/
internal val LinearIndicatorWidth = 240.dp

/*@VisibleForTesting*/
internal val LinearIndicatorHeight = LinearProgressIndicatorTokens.Height

// CircularProgressIndicator Material specs
// Diameter of the indicator circle
/*@VisibleForTesting*/
internal val CircularIndicatorDiameter = CircularProgressIndicatorTokens.Size

// Indeterminate linear indicator transition specs

// Total duration for one linear cycle
internal const val LinearAnimationDuration = 1750

// Duration of the head and tail animations for both lines
internal const val FirstLineHeadDuration = 1000
internal const val FirstLineTailDuration = 1000
internal const val SecondLineHeadDuration = 850
internal const val SecondLineTailDuration = 850

// Delay before the start of the head and tail animations for both lines
internal const val FirstLineHeadDelay = 0
internal const val FirstLineTailDelay = 250
internal const val SecondLineHeadDelay = 650
internal const val SecondLineTailDelay = 900

internal val LinearIndeterminateProgressEasing = MotionTokens.EasingEmphasizedAccelerateCubicBezier

// Indeterminate circular indicator transition specs

// The indeterminate circular indicator easing constants for its motion
internal val CircularProgressEasing = MotionTokens.EasingStandardCubicBezier
internal const val CircularIndeterminateMinProgress = 0.1f
internal const val CircularIndeterminateMaxProgress = 0.87f

internal const val CircularAnimationProgressDuration = 6000
internal const val CircularAnimationAdditionalRotationDelay = 1500
internal const val CircularAnimationAdditionalRotationDuration = 300
internal const val CircularAdditionalRotationDegreesTarget = 360f
internal const val CircularGlobalRotationDegreesTarget = 1080f
