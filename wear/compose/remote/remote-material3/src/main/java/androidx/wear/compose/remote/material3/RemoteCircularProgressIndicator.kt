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
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Material Design circular progress indicator.
 *
 * The progress arc is always drawn clockwise from [startAngle] to [endAngle]. When [endAngle]
 * equals [startAngle] (the default), a full 360-degree circle is drawn; when [endAngle] is less
 * than [startAngle], the arc wraps clockwise past 0 degrees (3 o'clock).
 *
 * Animates changes to [progress] with the [RemoteMotionScheme.slowEffectsSpec] of
 * [RemoteMaterialTheme.motionScheme].
 *
 * Example of a [RemoteCircularProgressIndicator] showing static progress:
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
 * @param startAngle The starting position of the progress arc, measured clockwise in degrees from
 *   the 3 o'clock position. Defaults to [RemoteProgressIndicatorDefaults.StartAngle] (12 o'clock).
 * @param endAngle The ending position of the progress arc, measured clockwise in degrees from the 3
 *   o'clock position. Defaults to [startAngle] (a full 360-degree circle). The arc is always drawn
 *   clockwise from [startAngle] to [endAngle], wrapping past 0 degrees if [endAngle] is less than
 *   [startAngle].
 * @param colors [RemoteProgressIndicatorColors] that will be used to resolve the indicator and
 *   track color.
 * @param strokeWidth The stroke width for the progress indicator. Defaults to
 *   [RemoteProgressIndicatorDefaults.DefaultStrokeWidth]. Negative values are coerced to 0; nothing
 *   is drawn when [strokeWidth] is 0 or negative.
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
        val fullSweep = circularFullSweep(startAngle, endAngle)
        val sweepAngle = animatedProgress * fullSweep
        drawIndicatorArcs(
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            fullSweep = fullSweep,
            isFullCircle = fullSweep.isGreaterThanOrEqualTo(360f.rf),
            strokeWidth = strokeWidth,
            gapSize = gapSize,
            colors = colors,
            enabled = enabled,
        )
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
 * @param strokeWidth The stroke width for the progress indicator. Defaults to
 *   [RemoteProgressIndicatorDefaults.IndeterminateStrokeWidth]. Negative values are coerced to 0;
 *   nothing is drawn when [strokeWidth] is 0 or negative.
 * @param gapSize The size (in [RemoteDp]) of the gap between the ends of the progress indicator and
 *   the track.
 */
@RemoteComposable
@Composable
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

        drawIndicatorArcs(
            startAngle = totalRotation,
            sweepAngle = sweepAngle,
            fullSweep = 360f.rf,
            isFullCircle = false.rb,
            strokeWidth = strokeWidth,
            gapSize = gapSize,
            colors = colors,
            enabled = true.rb,
        )
    }
}

/**
 * Draws the track and the indicator of a circular progress indicator, with the indicator sweeping
 * [sweepAngle] degrees clockwise from [startAngle], and the track the rest of [fullSweep].
 */
private fun RemoteDrawScope.drawIndicatorArcs(
    startAngle: RemoteFloat,
    sweepAngle: RemoteFloat,
    fullSweep: RemoteFloat,
    isFullCircle: RemoteBoolean,
    strokeWidth: RemoteDp,
    gapSize: RemoteDp,
    colors: RemoteProgressIndicatorColors,
    enabled: RemoteBoolean,
) {
    val geometry = circularArcGeometry(strokeWidth, gapSize)
    val gapSweep = geometry.gapSweep
    val clampedSweep = clamp(sweepAngle, 0f.rf, fullSweep)

    // Track, from the end of the indicator to the end of the full sweep. Its gap grows with the
    // indicator, so that at progress 0 the track is drawn as a whole, without a gap.
    drawIndicatorSegment(
        startAngle = startAngle + clampedSweep,
        sweepAngle = fullSweep - clampedSweep,
        gapSweep = min(clampedSweep, gapSweep),
        brush = colors.trackBrush(enabled),
        geometry = geometry,
    )

    // Indicator. On a full circle, the gap shrinks with the rest of the track, so that at
    // progress 1 the indicator closes into a full ring, without a notch at its start.
    val closingGapSweep = min(gapSweep, fullSweep - clampedSweep)
    drawIndicatorSegment(
        startAngle = startAngle,
        sweepAngle = clampedSweep,
        gapSweep = isFullCircle.select(closingGapSweep, gapSweep),
        brush = colors.indicatorBrush(enabled),
        geometry = geometry,
    )
}

/**
 * Draws a segment of the indicator or the track, sweeping [sweepAngle] degrees clockwise from
 * [startAngle], with half of [gapSweep] left out at each end to make room for the round caps and
 * the gap between segments.
 *
 * A segment that doesn't sweep more than [gapSweep] is drawn as a dot at its center instead, with a
 * stroke width that scales with how much of the gap it sweeps, so that it shrinks smoothly to
 * nothing rather than leaving a full size round cap. The draw operations are the same in every
 * case, only the values change.
 */
private fun RemoteDrawScope.drawIndicatorSegment(
    startAngle: RemoteFloat,
    sweepAngle: RemoteFloat,
    gapSweep: RemoteFloat,
    brush: RemoteBrush,
    geometry: CircularArcGeometry,
) {
    // From 0 to 1 while the segment is within the gap, then 1 once it is an arc.
    val fill = min(1f.rf, sweepAngle / max(0.001f.rf, gapSweep))
    val isVisible = geometry.isZeroStroke.select(false.rb, sweepAngle.isGreaterThan(0f.rf))
    val paint = RemotePaint {
        style = RemotePaintingStyle.Stroke
        strokeCap = RemoteStrokeCap.Round
        strokeWidth = geometry.strokePx * fill
        with(brush) { applyTo(this@RemotePaint, size) }
        // A stroke width of 0 still draws a hairline, and an arc a dot, so hide them instead.
        color = isVisible.select(color, Color.Transparent.rc)
    }
    drawArc(
        paint = paint,
        // Within the gap, this is the center of the segment, where its dot is drawn.
        startAngle = startAngle + min(sweepAngle, gapSweep) / 2f.rf,
        // A tiny sweep with a round cap draws a dot.
        sweepAngle = max(MinDotSweep, sweepAngle - gapSweep),
        useCenter = false,
        topLeft = geometry.topLeft,
        size = geometry.arcSize,
    )
}
