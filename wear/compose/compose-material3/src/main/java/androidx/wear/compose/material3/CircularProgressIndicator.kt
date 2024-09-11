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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.tokens.MotionTokens
import androidx.wear.compose.materialcore.isSmallScreen
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min

/**
 * Material Design circular progress indicator.
 *
 * Example of a full screen [CircularProgressIndicator]. Note that the padding
 * [CircularProgressIndicatorDefaults.FullScreenPadding] should be applied:
 *
 * @sample androidx.wear.compose.material3.samples.FullScreenProgressIndicatorSample
 *
 * Example of progress showing overflow value (more than 1) by [CircularProgressIndicator]:
 *
 * @sample androidx.wear.compose.material3.samples.OverflowProgressIndicatorSample
 *
 * Example of progress indicator wrapping media control by [CircularProgressIndicator]:
 *
 * @sample androidx.wear.compose.material3.samples.MediaButtonProgressIndicatorSample
 *
 * Example of a [CircularProgressIndicator] with small progress values:
 *
 * @sample androidx.wear.compose.material3.samples.SmallValuesProgressIndicatorSample
 *
 * Progress indicators express the proportion of completion of an ongoing task.
 *
 * @param progress The progress of this progress indicator where 0.0 represents no progress and 1.0
 *   represents completion.
 * @param modifier Modifier to be applied to the CircularProgressIndicator.
 * @param allowProgressOverflow When progress overflow is allowed, values smaller than 0.0 will be
 *   coerced to 0, while values larger than 1.0 will be wrapped around and shown as overflow with a
 *   different track color [ProgressIndicatorColors.overflowTrackBrush]. For example values 1.2, 2.2
 *   etc will be shown as 20% progress with the overflow color. When progress overflow is not
 *   allowed, progress values will be coerced into the range 0..1.
 * @param startAngle The starting position of the progress arc, measured clockwise in degrees (0
 *   to 360) from the 3 o'clock position. For example, 0 and 360 represent 3 o'clock, 90 and 180
 *   represent 6 o'clock and 9 o'clock respectively. Default is 270 degrees
 *   [CircularProgressIndicatorDefaults.StartAngle] (top of the screen).
 * @param endAngle The ending position of the progress arc, measured clockwise in degrees (0 to 360)
 *   from the 3 o'clock position. For example, 0 and 360 represent 3 o'clock, 90 and 180 represent 6
 *   o'clock and 9 o'clock respectively. By default equal to [startAngle].
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   color for this progress indicator in different states.
 * @param strokeWidth The stroke width for the progress indicator. The recommended values are
 *   [CircularProgressIndicatorDefaults.largeStrokeWidth] and
 *   [CircularProgressIndicatorDefaults.smallStrokeWidth].
 * @param gapSize The size (in Dp) of the gap between the ends of the progress indicator and the
 *   track. The stroke endcaps are not included in this distance.
 * @param enabled controls the enabled state. Although this component is not clickable, it can be
 *   contained within a clickable component. When enabled is `false`, this component will appear
 *   visually disabled.
 */
@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    allowProgressOverflow: Boolean = false,
    startAngle: Float = CircularProgressIndicatorDefaults.StartAngle,
    endAngle: Float = startAngle,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.colors(),
    strokeWidth: Dp = CircularProgressIndicatorDefaults.largeStrokeWidth,
    gapSize: Dp = CircularProgressIndicatorDefaults.calculateRecommendedGapSize(strokeWidth),
    enabled: Boolean = true,
) {
    // Canvas internally uses Spacer.drawBehind.
    // Using Spacer.drawWithCache to optimize the stroke allocations.
    Spacer(
        modifier
            .clearAndSetSemantics {}
            .fillMaxSize()
            .focusable()
            .drawWithCache {
                val currentProgress = progress()
                val coercedProgress = coerceProgress(currentProgress, allowProgressOverflow)
                val fullSweep = 360f - ((startAngle - endAngle) % 360 + 360) % 360
                var progressSweep = fullSweep * coercedProgress
                val hasOverflow = allowProgressOverflow && currentProgress > 1.0f
                val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                val minSize = min(size.height, size.width)
                // Sweep angle between two progress indicator segments.
                val gapSweep =
                    asin((stroke.width + gapSize.toPx()) / (minSize - stroke.width)).toDegrees() *
                        2f

                if (progressSweep > 0) {
                    progressSweep = max(progressSweep, gapSweep)
                }

                onDrawWithContent {
                    // Draw an indicator.
                    drawIndicatorSegment(
                        startAngle = startAngle,
                        sweep = progressSweep,
                        gapSweep = gapSweep,
                        brush = colors.indicatorBrush(enabled),
                        stroke = stroke
                    )

                    // Draw a background.
                    drawIndicatorSegment(
                        startAngle = startAngle + progressSweep,
                        sweep = fullSweep - progressSweep,
                        gapSweep = gapSweep,
                        brush = colors.trackBrush(enabled, hasOverflow),
                        stroke = stroke
                    )
                }
            }
    )
}

/**
 * Indeterminate Material Design circular progress indicator.
 *
 * Indeterminate progress indicator expresses an unspecified wait time and spins indefinitely.
 *
 * Example of indeterminate progress indicator:
 *
 * @sample androidx.wear.compose.material3.samples.IndeterminateProgressIndicatorSample
 * @param modifier Modifier to be applied to the CircularProgressIndicator.
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   color for this progress indicator.
 * @param strokeWidth The stroke width for the progress indicator. The recommended values is
 *   [CircularProgressIndicatorDefaults.IndeterminateStrokeWidth].
 * @param gapSize The size (in Dp) of the gap between the ends of the progress indicator and the
 *   track. The stroke endcaps are not included in this distance.
 */
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.colors(),
    strokeWidth: Dp = CircularProgressIndicatorDefaults.IndeterminateStrokeWidth,
    gapSize: Dp = CircularProgressIndicatorDefaults.calculateRecommendedGapSize(strokeWidth),
) {
    val stroke =
        with(LocalDensity.current) { Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round) }

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

    Canvas(
        modifier.size(CircularProgressIndicatorDefaults.IndeterminateCircularIndicatorDiameter)
    ) {
        val sweep = progressAnimation.value * 360f
        val adjustedGapSize = gapSize + strokeWidth
        val gapSizeSweep = (adjustedGapSize.value / (PI * size.width.toDp().value).toFloat()) * 360f

        rotate(globalRotation.value + additionalRotation.value) {
            drawCircularIndicator(
                sweep + min(sweep, gapSizeSweep),
                360f - sweep - min(sweep, gapSizeSweep) * 2,
                colors.trackBrush,
                stroke
            )
            drawCircularIndicator(startAngle = 0f, sweep, colors.indicatorBrush, stroke)
        }
    }
}

/** Contains default values for [CircularProgressIndicator]. */
object CircularProgressIndicatorDefaults {
    /** Large stroke width for circular progress indicator. */
    val largeStrokeWidth: Dp
        @Composable get() = if (isSmallScreen()) 8.dp else 12.dp

    /** Small stroke width for circular progress indicator. */
    val smallStrokeWidth: Dp
        @Composable get() = if (isSmallScreen()) 5.dp else 8.dp

    /**
     * The default angle used for the start of the progress indicator arc.
     *
     * This can be customized with `startAngle` parameter on [CircularProgressIndicator].
     */
    val StartAngle = 270f

    /**
     * Returns recommended size of the gap based on `strokeWidth`.
     *
     * The absolute value can be customized with `gapSize` parameter on [CircularProgressIndicator].
     */
    fun calculateRecommendedGapSize(strokeWidth: Dp): Dp = strokeWidth / 3f

    /** Padding used for displaying [CircularProgressIndicator] full screen. */
    val FullScreenPadding = PaddingDefaults.edgePadding

    /** Diameter of the indicator circle for indeterminate progress. */
    internal val IndeterminateCircularIndicatorDiameter = 24.dp

    /** Default stroke width for indeterminate [CircularProgressIndicator]. */
    val IndeterminateStrokeWidth = 3.dp
}

private fun DrawScope.drawCircularIndicator(
    startAngle: Float,
    sweep: Float,
    brush: Brush,
    stroke: Stroke
) {
    // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
    // To do this we need to remove half the stroke width from the total diameter for both sides.
    val diameterOffset = stroke.width / 2
    val arcDimen = size.width - 2 * diameterOffset
    drawArc(
        brush = brush,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(diameterOffset, diameterOffset),
        size = Size(arcDimen, arcDimen),
        style = stroke
    )
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
                        MotionTokens
                            .EasingEmphasizedDecelerate // MotionTokens.EasingEmphasizedDecelerateCubicBezier // 300ms
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

// The indeterminate circular indicator easing constants for its motion
internal val CircularProgressEasing = MotionTokens.EasingStandard
internal const val CircularIndeterminateMinProgress = 0.1f
internal const val CircularIndeterminateMaxProgress = 0.87f

internal const val CircularAnimationProgressDuration = 6000
internal const val CircularAnimationAdditionalRotationDelay = 1500
internal const val CircularAnimationAdditionalRotationDuration = 300
internal const val CircularAdditionalRotationDegreesTarget = 360f
internal const val CircularGlobalRotationDegreesTarget = 1080f
