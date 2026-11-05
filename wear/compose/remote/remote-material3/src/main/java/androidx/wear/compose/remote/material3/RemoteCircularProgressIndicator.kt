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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
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
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.remote.creation.compose.state.asin
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import kotlin.math.PI

/**
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorSample
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorAnimatedSample
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
 * @sample androidx.wear.compose.remote.material3.samples.RemoteIndeterminateCircularProgressIndicatorSample
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

        drawIndicatorArcs(totalRotation, sweepAngle, 360f.rf, strokeWidth, gapSize, colors, true.rb)
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
    val coercedStrokeWidth = max(0.rdp, strokeWidth)
    val isZeroStroke = coercedStrokeWidth.isLessThanOrEqualTo(0.rdp)
    val strokePx = coercedStrokeWidth.toPx()
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
    val gapSweep = asin(chordRatio) * (360f / PI.toFloat()).rf

    val trackGapSweep = min(sweepAngle, gapSweep)

    // Track
    val trackPaint = RemotePaint {
        style = RemotePaintingStyle.Stroke
        this.strokeWidth = strokePx
        strokeCap = RemoteStrokeCap.Round
        with(colors.trackBrush(enabled)) { applyTo(this@RemotePaint, size) }
    }
    drawArc(
        paint = trackPaint,
        startAngle = startAngle + sweepAngle + trackGapSweep / 2f.rf,
        sweepAngle = isZeroStroke.select(0f.rf, max(0f.rf, fullSweep - sweepAngle - trackGapSweep)),
        useCenter = false,
        topLeft = RemoteOffset(left, top),
        size = RemoteSize(right - left, bottom - top),
    )

    // Indicator
    val indicatorPaint = RemotePaint {
        style = RemotePaintingStyle.Stroke
        this.strokeWidth = strokePx
        strokeCap = RemoteStrokeCap.Round
        with(colors.indicatorBrush(enabled)) { applyTo(this@RemotePaint, size) }
    }
    drawArc(
        paint = indicatorPaint,
        startAngle = startAngle + gapSweep / 2f.rf,
        sweepAngle = isZeroStroke.select(0f.rf, max(0f.rf, sweepAngle - gapSweep)),
        useCenter = false,
        topLeft = RemoteOffset(left, top),
        size = RemoteSize(right - left, bottom - top),
    )
}
