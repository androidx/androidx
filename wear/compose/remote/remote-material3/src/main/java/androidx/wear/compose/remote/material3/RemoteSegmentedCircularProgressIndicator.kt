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
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.asin
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap

/**
 * Material Design segmented circular progress indicator.
 *
 * A segmented variant of [RemoteCircularProgressIndicator] that is divided into equally sized
 * segments. This overload of [RemoteSegmentedCircularProgressIndicator] fills the segments in order
 * based on [progress].
 *
 * Example of [RemoteSegmentedCircularProgressIndicator] with progress value:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSegmentedCircularProgressIndicatorSample
 *
 * For an animated progress, see:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSegmentedCircularProgressIndicatorAnimatedSample
 * @param segmentCount Number of equal segments that the progress indicator should be divided into.
 *   Has to be a constant [RemoteInt] equal or greater to 1.
 * @param progress The progress of this progress indicator where 0.0 represents no progress and 1.0
 *   represents completion. Values outside of this range are coerced into the range.
 * @param modifier Modifier to be applied to the progress indicator.
 * @param enabled Controls the enabled state. When enabled is `false`, this component will appear
 *   visually disabled. Note that the disabled colors are only applied when [enabled] is a constant.
 * @param startAngle The starting position of the progress arc, measured clockwise in degrees from
 *   the 3 o'clock position. Defaults to [RemoteProgressIndicatorDefaults.StartAngle] (top of the
 *   screen).
 * @param endAngle The ending position of the progress arc, measured clockwise in degrees from the 3
 *   o'clock position. By default equal to [startAngle].
 * @param colors [RemoteProgressIndicatorColors] that will be used to resolve the indicator and
 *   track color for this progress indicator.
 * @param strokeWidth The stroke width for the progress indicator. Defaults to
 *   [RemoteProgressIndicatorDefaults.DefaultStrokeWidth].
 * @param gapSize The size of the gap between segments. The stroke endcaps are not included in this
 *   distance.
 */
@RemoteComposable
@Composable
public fun RemoteSegmentedCircularProgressIndicator(
    segmentCount: RemoteInt,
    progress: RemoteFloat,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    startAngle: RemoteFloat = RemoteProgressIndicatorDefaults.StartAngle,
    endAngle: RemoteFloat = startAngle,
    colors: RemoteProgressIndicatorColors = RemoteProgressIndicatorDefaults.colors(),
    strokeWidth: RemoteDp = RemoteProgressIndicatorDefaults.DefaultStrokeWidth,
    gapSize: RemoteDp = RemoteProgressIndicatorDefaults.calculateRecommendedGapSize(strokeWidth),
) {
    val count =
        requireNotNull(segmentCount.constantValueOrNull) {
            "segmentCount must be a constant RemoteInt"
        }
    require(count >= 1) { "segmentCount must be at least 1, was $count" }
    RemoteCanvas(modifier = modifier.fillMaxSize()) {
        val progressInSegments = progress * count.toFloat().rf
        drawSegments(
            segmentCount = count,
            startAngle = startAngle,
            endAngle = endAngle,
            strokeWidth = strokeWidth,
            gapSize = gapSize,
            colors = colors,
            enabled = enabled,
        ) { segmentIndex ->
            min(1f.rf, max(0f.rf, progressInSegments - segmentIndex.toFloat().rf))
        }
    }
}

/**
 * Material Design segmented circular progress indicator.
 *
 * A segmented variant of [RemoteCircularProgressIndicator] that is divided into equally sized
 * segments. This overload of [RemoteSegmentedCircularProgressIndicator] allows for each segment to
 * be individually indicated as completed, such as for showing activity for intervals within a
 * longer period.
 *
 * Example of [RemoteSegmentedCircularProgressIndicator] where the segments are turned on/off:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSegmentedCircularProgressIndicatorBinarySample
 * @param segmentCount Number of equal segments that the progress indicator should be divided into.
 *   Has to be a constant [RemoteInt] equal or greater to 1.
 * @param segmentValue A function that for each segment index between 0 and [segmentCount] - 1
 *   returns a [RemoteBoolean] that is true if this segment should be displayed with the indicator
 *   color to show progress, and false if the segment should be displayed with the track color.
 * @param modifier Modifier to be applied to the progress indicator.
 * @param enabled Controls the enabled state. When enabled is `false`, this component will appear
 *   visually disabled. Note that the disabled colors are only applied when [enabled] is a constant.
 * @param startAngle The starting position of the progress arc, measured clockwise in degrees from
 *   the 3 o'clock position. Defaults to [RemoteProgressIndicatorDefaults.StartAngle] (top of the
 *   screen).
 * @param endAngle The ending position of the progress arc, measured clockwise in degrees from the 3
 *   o'clock position. By default equal to [startAngle].
 * @param colors [RemoteProgressIndicatorColors] that will be used to resolve the indicator and
 *   track color for this progress indicator.
 * @param strokeWidth The stroke width for the progress indicator. Defaults to
 *   [RemoteProgressIndicatorDefaults.DefaultStrokeWidth].
 * @param gapSize The size of the gap between segments. The stroke endcaps are not included in this
 *   distance.
 */
@RemoteComposable
@Composable
public fun RemoteSegmentedCircularProgressIndicator(
    segmentCount: RemoteInt,
    segmentValue: (segmentIndex: Int) -> RemoteBoolean,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    startAngle: RemoteFloat = RemoteProgressIndicatorDefaults.StartAngle,
    endAngle: RemoteFloat = startAngle,
    colors: RemoteProgressIndicatorColors = RemoteProgressIndicatorDefaults.colors(),
    strokeWidth: RemoteDp = RemoteProgressIndicatorDefaults.DefaultStrokeWidth,
    gapSize: RemoteDp = RemoteProgressIndicatorDefaults.calculateRecommendedGapSize(strokeWidth),
) {
    val count =
        requireNotNull(segmentCount.constantValueOrNull) {
            "segmentCount must be a constant RemoteInt"
        }
    require(count >= 1) { "segmentCount must be at least 1, was $count" }
    RemoteCanvas(modifier = modifier.fillMaxSize()) {
        drawSegments(
            segmentCount = count,
            startAngle = startAngle,
            endAngle = endAngle,
            strokeWidth = strokeWidth,
            gapSize = gapSize,
            colors = colors,
            enabled = enabled,
        ) { segmentIndex ->
            segmentValue(segmentIndex).select(1f.rf, 0f.rf)
        }
    }
}

/**
 * Draws [segmentCount] track segments, and an indicator on top of each segment filled according to
 * [segmentFraction], a value between 0 (empty) and 1 (full).
 */
@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.drawSegments(
    segmentCount: Int,
    startAngle: RemoteFloat,
    endAngle: RemoteFloat,
    strokeWidth: RemoteDp,
    gapSize: RemoteDp,
    colors: RemoteProgressIndicatorColors,
    enabled: RemoteBoolean,
    segmentFraction: (segmentIndex: Int) -> RemoteFloat,
) {
    val fullSweep = 360f.rf - ((startAngle - endAngle) % 360f.rf + 360f.rf) % 360f.rf
    val strokePx = strokeWidth.toPx()
    val gapSizePx = gapSize.toPx()
    val diameter = min(width, height)
    val diameterOffset = strokePx / 2f.rf
    val arcDimen = diameter - (diameterOffset * 2f.rf)
    val topLeft =
        RemoteOffset(
            diameterOffset + (width - diameter) / 2f.rf,
            diameterOffset + (height - diameter) / 2f.rf,
        )
    val arcSize = RemoteSize(arcDimen, arcDimen)

    // Sweep angle of the gap between two segments.
    val chordRatio =
        min(1f.rf, max(0f.rf, (strokePx + gapSizePx) / max(0.001f.rf, diameter - strokePx)))
    val gapSweep = asin(chordRatio) * (360f / Math.PI.toFloat()).rf
    val segmentSweep = fullSweep / segmentCount.toFloat().rf

    val trackPaint = RemotePaint {
        style = PaintingStyle.Stroke
        this.strokeWidth = strokePx
        strokeCap = StrokeCap.Round
        with(colors.trackBrush(enabled)) { applyTo(this@RemotePaint, size) }
    }
    val indicatorPaint = RemotePaint {
        style = PaintingStyle.Stroke
        strokeCap = StrokeCap.Round
        with(colors.indicatorBrush(enabled)) { applyTo(this@RemotePaint, size) }
    }
    val baseIndicatorColor = indicatorPaint.color
    val baseIndicatorStrokePx = strokePx + AntiAliasingStrokePadding.rf

    for (segment in 0 until segmentCount) {
        val segmentStartAngle =
            startAngle + fullSweep * segment.toFloat().rf / segmentCount.toFloat().rf

        // Track
        drawArc(
            paint = trackPaint,
            startAngle = segmentStartAngle + gapSweep / 2f.rf,
            sweepAngle = max(0f.rf, segmentSweep - gapSweep),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
        )

        // Indicator. When a segment becomes active (progressSweep > 0), its dot scales in from the
        // leading edge using a spring intro animation (~0.85s) while its arc sweep advances in
        // parallel, and opacity fades in from 0% at 0% width to 100% at 50% width, matching the
        // Wear Material3 motion spec.
        val progressSweep = segmentSweep * segmentFraction(segment)
        val hasProgress = progressSweep.isGreaterThan(0f.rf)
        val dotScale =
            remote.animateSpring(
                rf = hasProgress.select(1f.rf, 0f.rf),
                stiffness = SpringStiffness,
                dampingRatio = SpringDampingRatio,
            )
        val isVisible = hasProgress and dotScale.isGreaterThan(0f.rf)
        val dotAlpha = min(1f.rf, dotScale * 2f.rf)
        indicatorPaint.strokeWidth = baseIndicatorStrokePx * dotScale
        indicatorPaint.color =
            isVisible.select(
                baseIndicatorColor.copy(alpha = baseIndicatorColor.alpha * dotAlpha),
                Color.Transparent.rc,
            )
        drawArc(
            paint = indicatorPaint,
            startAngle = segmentStartAngle + (gapSweep / 2f.rf) * dotScale,
            // A zero sweep draws nothing, while a tiny sweep with a round cap draws a dot.
            sweepAngle = isVisible.select(max(MinDotSweep, progressSweep - gapSweep), 0f.rf),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
        )
    }
}

/** Extra stroke width (in px) for the indicator to cover the anti-aliased edges of the track. */
private const val AntiAliasingStrokePadding = 1f

/** Sweep angle used to draw a dot with a round stroke cap. */
private val MinDotSweep: RemoteFloat = 0.05f.rf

/** Spring stiffness for the ~0.85s segment dot intro animation. */
private const val SpringStiffness = 50f

/** Critically damped spring ratio for the segment dot intro animation. */
private const val SpringDampingRatio = 1f
