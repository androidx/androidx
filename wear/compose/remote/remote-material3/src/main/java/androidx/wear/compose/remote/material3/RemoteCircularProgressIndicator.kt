/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.remote.creation.compose.state.asin
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap

/**
 * Material Design circular progress indicator.
 *
 * Animates changes to [progress] with the [RemoteMotionScheme.slowEffectsSpec] of
 * [RemoteMaterialTheme.motionScheme].
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorSample
 *
 * Example that changes [progress] on click:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorAnimatedSample
 * @param progress The progress of this progress indicator where 0.0 represents no progress and 1.0
 *   represents completion. Values outside of this range are coerced into the range 0..1. Progress
 *   changes are animated.
 * @param modifier Modifier to be applied to the [RemoteCircularProgressIndicator].
 * @param enabled controls the enabled state. When enabled is `false`, this component will appear
 *   visually disabled. Note that only solid colors are supported when [enabled] is an expression,
 *   otherwise it must be a constant.
 * @param startAngle The starting position of the progress arc, measured clockwise in degrees. For
 *   example, 0 is 3 o'clock.
 * @param endAngle The ending position of the progress arc.
 * @param colors [RemoteProgressIndicatorColors] that will be used to resolve the indicator and
 *   track color.
 * @param strokeWidth The stroke width for the progress indicator. Defaults to
 *   [RemoteProgressIndicatorDefaults.DefaultStrokeWidth].
 * @param gapSize The size (in [RemoteDp]) of the gap between the ends of the progress indicator and
 *   the track. The stroke endcaps are not included in this distance.
 */
@RemoteComposable
@Composable
public fun RemoteCircularProgressIndicator(
    progress: RemoteFloat,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    startAngle: RemoteFloat = RemoteProgressIndicatorDefaults.StartAngle,
    endAngle: RemoteFloat = startAngle,
    colors: RemoteProgressIndicatorColors = RemoteProgressIndicatorDefaults.colors(),
    strokeWidth: RemoteDp = RemoteProgressIndicatorDefaults.DefaultStrokeWidth,
    gapSize: RemoteDp = RemoteProgressIndicatorDefaults.calculateRecommendedGapSize(strokeWidth),
) {
    // Coerce before animating, as Wear Material3 does, so an out of range value animates from the
    // end of the indicator instead of pausing there.
    val animatedProgress = animateProgress(clamp(progress, 0f.rf, 1f.rf))
    RemoteCanvas(modifier = modifier.fillMaxSize()) {
        val fullSweep = 360f.rf - ((startAngle - endAngle) % 360f.rf + 360f.rf) % 360f.rf
        val sweepAngle = animatedProgress * fullSweep
        drawIndicatorArcs(startAngle, sweepAngle, fullSweep, strokeWidth, gapSize, colors, enabled)
    }
}

/**
 * Indeterminate Material Design circular progress indicator.
 *
 * Indeterminate progress indicator expresses an unspecified wait time and spins indefinitely.
 *
 * Example of indeterminate circular progress indicator:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteIndeterminateCircularProgressIndicatorSample
 * @param modifier Modifier to be applied to the [RemoteCircularProgressIndicator].
 * @param colors [RemoteProgressIndicatorColors] that will be used to resolve the indicator and
 *   track color.
 * @param strokeWidth The stroke width for the progress indicator.
 * @param gapSize The size (in [RemoteDp]) of the gap between the ends of the progress indicator and
 *   the track.
 */
@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX") // RemoteTimeVariables, rotate
public fun RemoteCircularProgressIndicator(
    modifier: RemoteModifier = RemoteModifier,
    colors: RemoteProgressIndicatorColors = RemoteProgressIndicatorDefaults.colors(),
    strokeWidth: RemoteDp = RemoteProgressIndicatorDefaults.IndeterminateStrokeWidth,
    gapSize: RemoteDp = RemoteProgressIndicatorDefaults.calculateRecommendedGapSize(strokeWidth),
) {
    RemoteCanvas(
        modifier =
            modifier.size(RemoteProgressIndicatorDefaults.IndeterminateCircularIndicatorDiameter)
    ) {
        val time = RemoteTimeVariables.continuousSeconds
        val durationSeconds = 5f.rf
        val timeProgress = (time % durationSeconds) / durationSeconds

        // Global rotation: 1440 degrees in 5s
        val globalRotation = timeProgress * 1440f.rf

        val segmentProgress = (timeProgress % 0.25f.rf) * 4f.rf

        val easedProgress = RemoteMotionTokens.easeStandard(segmentProgress)

        val sweepConditions =
            listOf(
                0.25f.rf to lerp(0f.rf, 0.8f.rf, easedProgress),
                0.50f.rf to lerp(0.8f.rf, 0.2f.rf, easedProgress),
                0.75f.rf to lerp(0.2f.rf, 0.8f.rf, easedProgress),
            )
        val sweepAnimation =
            sweepConditions.foldRight(lerp(0.8f.rf, 0f.rf, easedProgress)) { pair, acc ->
                timeProgress.isLessThan(pair.first).select(pair.second, acc)
            }

        val rotationConditions =
            listOf(
                0.25f.rf to 0f.rf,
                0.50f.rf to lerp(0f.rf, 360f.rf, easedProgress),
                0.75f.rf to 360f.rf,
            )
        val additionalRotation =
            rotationConditions.foldRight(lerp(360f.rf, 720f.rf, easedProgress)) { pair, acc ->
                timeProgress.isLessThan(pair.first).select(pair.second, acc)
            }

        val totalRotation = 270f.rf + globalRotation + additionalRotation

        val sweepAngle = sweepAnimation * 360f.rf

        rotate(degrees = totalRotation, pivot = center) {
            drawIndicatorArcs(0f.rf, sweepAngle, 360f.rf, strokeWidth, gapSize, colors, true.rb)
        }
    }
}

private fun RemoteDrawScope.drawIndicatorArcs(
    startAngle: RemoteFloat,
    sweepAngle: RemoteFloat,
    fullSweep: RemoteFloat,
    strokeWidth: RemoteDp,
    gapSize: RemoteDp,
    colors: RemoteProgressIndicatorColors,
    enabled: RemoteBoolean,
) {
    val strokePx = strokeWidth.toPx()
    val gapSizePx = gapSize.toPx()
    val diameter = min(width, height)
    val diameterOffset = strokePx / 2f.rf
    val arcDimen = diameter - (diameterOffset * 2f.rf)

    val left = diameterOffset + (width - diameter) / 2f.rf
    val top = diameterOffset + (height - diameter) / 2f.rf
    val right = left + arcDimen
    val bottom = top + arcDimen

    val chordRatio =
        clamp((strokePx + gapSizePx) / max(0.001f.rf, diameter - strokePx), 0f.rf, 1f.rf)
    val gapSweep = asin(chordRatio) * (360f / Math.PI.toFloat()).rf

    val trackGapSweep = min(sweepAngle, gapSweep)

    // Track
    val trackPaint = RemotePaint {
        style = PaintingStyle.Stroke
        this.strokeWidth = strokePx
        strokeCap = StrokeCap.Round
        with(colors.trackBrush(enabled)) { applyTo(this@RemotePaint, size) }
    }
    drawArc(
        paint = trackPaint,
        startAngle = startAngle + sweepAngle + trackGapSweep / 2f.rf,
        sweepAngle = max(0f.rf, fullSweep - sweepAngle - trackGapSweep),
        useCenter = false,
        topLeft = RemoteOffset(left, top),
        size = RemoteSize(right - left, bottom - top),
    )

    // Indicator
    val indicatorPaint = RemotePaint {
        style = PaintingStyle.Stroke
        this.strokeWidth = strokePx
        strokeCap = StrokeCap.Round
        with(colors.indicatorBrush(enabled)) { applyTo(this@RemotePaint, size) }
    }
    drawArc(
        paint = indicatorPaint,
        startAngle = startAngle + gapSweep / 2f.rf,
        sweepAngle = max(0f.rf, sweepAngle - gapSweep),
        useCenter = false,
        topLeft = RemoteOffset(left, top),
        size = RemoteSize(right - left, bottom - top),
    )
}
