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

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.materialcore.screenHeightDp
import kotlin.math.PI
import kotlin.math.asin

/**
 * Indeterminate Material Design arc progress indicator.
 *
 * Indeterminate progress indicator expresses an unspecified wait time and animates indefinitely.
 * This overload provides a variation over the usual circular spinner by allowing the start and end
 * angles to be specified.
 *
 * Example of indeterminate arc progress indicator:
 *
 * @sample androidx.wear.compose.material3.samples.IndeterminateProgressArcSample
 * @param startAngle the start angle of this progress indicator arc (specified in degrees). It is
 *   recommended to use [ArcProgressIndicatorDefaults.IndeterminateStartAngle]. Measured clockwise
 *   from the three o'clock position.
 * @param endAngle the end angle of this progress indicator arc (specified in degrees). It is
 *   recommended to use [ArcProgressIndicatorDefaults.IndeterminateEndAngle]. Measured clockwise
 *   from the three o'clock position.
 * @param modifier Modifier to be applied to the ArcProgressIndicator.
 * @param angularDirection Determines whether the animation is in the clockwise or counter-clockwise
 *   direction.
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   color for this progress indicator.
 * @param strokeWidth The stroke width for the progress indicator. The recommended value is
 *   [ArcProgressIndicatorDefaults.IndeterminateStrokeWidth].
 * @param gapSize The size (in Dp) of the gap between the ends of the progress indicator and the
 *   track. The stroke end caps are not included in this distance.
 */
@Composable
fun ArcProgressIndicator(
    startAngle: Float = ArcProgressIndicatorDefaults.IndeterminateStartAngle,
    endAngle: Float = ArcProgressIndicatorDefaults.IndeterminateEndAngle,
    modifier: Modifier = Modifier,
    angularDirection: AngularDirection = AngularDirection.CounterClockwise,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.colors(),
    strokeWidth: Dp = ArcProgressIndicatorDefaults.IndeterminateStrokeWidth,
    gapSize: Dp = ArcProgressIndicatorDefaults.calculateRecommendedGapSize(strokeWidth),
) {
    val infiniteTransition = rememberInfiniteTransition()
    val head =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = arcIndeterminateHeadAnimationSpec
        )
    val tail =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = arcIndeterminateTailAnimationSpec
        )

    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    val adjustedGapSize = gapSize + strokeWidth
    val fullSweep = ((endAngle - startAngle) % 360 + 360) % 360

    Canvas(modifier) {
        val gapSizeSweep = asin(adjustedGapSize.toPx() / size.width) * 360f / PI.toFloat()

        // Track before arc
        val beforeTrackSweep =
            if (tail.value >= 0.999f) (1 - head.value) * fullSweep
            else (1 - head.value) * fullSweep - gapSizeSweep
        if (beforeTrackSweep > 0) {
            drawCircularIndicator(
                startAngle =
                    if (angularDirection == AngularDirection.CounterClockwise) startAngle
                    else endAngle,
                sweep =
                    if (angularDirection == AngularDirection.CounterClockwise) beforeTrackSweep
                    else -beforeTrackSweep,
                colors.trackBrush,
                stroke = stroke
            )
        }

        // Arc
        val arcStart =
            if (angularDirection == AngularDirection.CounterClockwise)
                endAngle - tail.value * fullSweep
            else startAngle + tail.value * fullSweep
        val arcSweep =
            if (angularDirection == AngularDirection.CounterClockwise)
                (tail.value - head.value) * fullSweep
            else (head.value - tail.value) * fullSweep
        drawCircularIndicator(
            startAngle = arcStart,
            sweep = arcSweep,
            colors.indicatorBrush,
            stroke = stroke
        )

        // Track after arc
        val afterTrackSweep =
            if (tail.value >= 0.999f) tail.value * fullSweep
            else tail.value * fullSweep - gapSizeSweep
        if (afterTrackSweep > 0) {
            drawCircularIndicator(
                startAngle =
                    if (angularDirection == AngularDirection.CounterClockwise) endAngle
                    else startAngle,
                sweep =
                    if (angularDirection == AngularDirection.CounterClockwise) -afterTrackSweep
                    else afterTrackSweep,
                colors.trackBrush,
                stroke = stroke
            )
        }
    }
}

/** Contains default values for [ArcProgressIndicator]. */
object ArcProgressIndicatorDefaults {
    /** The default start angle in degrees for an indeterminate arc progress indicator */
    const val IndeterminateStartAngle = 62f

    /** The default end angle in degrees for an indeterminate arc progress indicator */
    const val IndeterminateEndAngle = 118f

    /** Stroke width of the indeterminate arc progress indicator. */
    val IndeterminateStrokeWidth = 8.dp

    /**
     * The recommended diameter of the indeterminate arc progress indicator, which leaves room for
     * additional content such as a message above the indicator.
     */
    val recommendedIndeterminateDiameter: Dp
        @Composable
        get() {
            // Calculate the recommended diameter as 76% of screen height.
            val screenHeight = screenHeightDp()
            return 0.76.dp * screenHeight.toFloat()
        }

    /**
     * Returns recommended size of the gap based on `strokeWidth`.
     *
     * The absolute value can be customized with `gapSize` parameter on [CircularProgressIndicator].
     */
    fun calculateRecommendedGapSize(strokeWidth: Dp): Dp = strokeWidth / 3f
}

/** Class to define angular direction - Clockwise and Counter Clockwise. */
@JvmInline
value class AngularDirection internal constructor(internal val type: Int) {
    companion object {
        /** Clockwise is the standard direction for an analog clock. */
        val Clockwise = AngularDirection(0)

        /** CounterClockwise is the opposite direction to Clockwise */
        val CounterClockwise = AngularDirection(1)
    }
}

/** An animation spec for indeterminate arc progress indicator head position. */
internal val arcIndeterminateHeadAnimationSpec =
    infiniteRepeatable(
        animation =
            keyframes {
                durationMillis = TotalArcAnimationDuration
                0f at HeadDelay using ArcIndeterminateProgressEasing
                1f at HeadDuration + HeadDelay
            }
    )

/** An animation spec for indeterminate arc progress indicator tail position. */
internal val arcIndeterminateTailAnimationSpec =
    infiniteRepeatable(
        animation =
            keyframes {
                durationMillis = TotalArcAnimationDuration
                0f at TailDelay using ArcIndeterminateProgressEasing
                1f at TailDuration + TailDelay
            }
    )

// Total duration for one arc cycle
internal const val TotalArcAnimationDuration = 2000 // extra-long4 * 2

// Duration of the head and tail animations for the indeterminate arc indicator
internal const val HeadDuration = 600 // long4
internal const val TailDuration = 600 // long4

// Delay before the start of the head and tail animations for the indeterminate arc indicator
internal const val HeadDelay = 0
internal const val TailDelay = 300 // medium2

internal val ArcIndeterminateProgressEasing = CubicBezierEasing(0.3f, 0f, 0.7f, 1f)
