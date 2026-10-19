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
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.remote.creation.compose.state.toRad
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
 * Animates changes to [progress] with the [RemoteMotionScheme.slowEffectsSpec] of
 * [RemoteMaterialTheme.motionScheme], as a single animation across the segments. Only the length of
 * the filled segments animates, not their stroke width.
 *
 * Example of [RemoteSegmentedCircularProgressIndicator] with progress value:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSegmentedCircularProgressIndicatorSample
 *
 * Example that changes [progress] on click:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSegmentedCircularProgressIndicatorAnimatedSample
 * @param segmentCount Number of equal segments that the progress indicator should be divided into.
 *   Has to be a constant [RemoteInt] greater than or equal to 1.
 * @param progress The progress of this progress indicator where 0.0 represents no progress and 1.0
 *   represents completion. Values outside of this range are coerced into the range. Progress
 *   changes are animated.
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
    // Coerce before animating, as Wear Material3 does, so an out of range value animates from the
    // end of the indicator instead of pausing there.
    val targetProgress = clamp(progress, 0f.rf, 1f.rf)
    val animatedProgress = animateProgress(targetProgress)
    val targetFractions = List(count) { segmentFraction(targetProgress, count, it) }
    // A constant progress isn't animated, so the segments are at their targets already.
    val segmentFractions =
        if (targetProgress.hasConstantValue) {
            targetFractions
        } else {
            List(count) { segmentFraction(animatedProgress, count, it) }
        }
    RemoteCanvas(modifier = modifier.fillMaxSize()) {
        drawSegments(
            segmentCount = count,
            startAngle = startAngle,
            endAngle = endAngle,
            strokeWidth = strokeWidth,
            gapSize = gapSize,
            colors = colors,
            enabled = enabled,
            segmentFractions = segmentFractions,
            targetSegmentFractions = targetFractions,
        )
    }
}

/** Returns how much of the segment at [segmentIndex] is filled at [progress], from 0 to 1. */
private fun segmentFraction(progress: RemoteFloat, segmentCount: Int, segmentIndex: Int) =
    clamp(progress * segmentCount.toFloat().rf - segmentIndex.toFloat().rf, 0f.rf, 1f.rf)

/**
 * Material Design segmented circular progress indicator.
 *
 * A segmented variant of [RemoteCircularProgressIndicator] that is divided into equally sized
 * segments. This overload of [RemoteSegmentedCircularProgressIndicator] allows for each segment to
 * be individually indicated as completed, such as for showing activity for intervals within a
 * longer period.
 *
 * When a segment turns on or off, its length grows from its start or shrinks back to it, with the
 * [RemoteMotionScheme.fastEffectsSpec] of [RemoteMaterialTheme.motionScheme]. The stroke width
 * doesn't animate.
 *
 * Example of [RemoteSegmentedCircularProgressIndicator] where the segments are turned on/off:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSegmentedCircularProgressIndicatorBinarySample
 * @param segmentCount Number of equal segments that the progress indicator should be divided into.
 *   Has to be a constant [RemoteInt] greater than or equal to 1.
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
    val animationSpec = RemoteMaterialTheme.motionScheme.fastEffectsSpec()
    val targetFractions =
        List(count) { segmentIndex -> segmentValue(segmentIndex).select(1f.rf, 0f.rf) }
    val segmentFractions = List(count) { animateProgress(targetFractions[it], animationSpec) }
    RemoteCanvas(modifier = modifier.fillMaxSize()) {
        drawSegments(
            segmentCount = count,
            startAngle = startAngle,
            endAngle = endAngle,
            strokeWidth = strokeWidth,
            gapSize = gapSize,
            colors = colors,
            enabled = enabled,
            segmentFractions = segmentFractions,
            targetSegmentFractions = targetFractions,
        )
    }
}

/**
 * Draws [segmentCount] track segments, and an indicator on top of each segment filled according to
 * [segmentFractions], values between 0 (empty) and 1 (full) that may be animating towards
 * [targetSegmentFractions]. A segment whose target fraction is constant doesn't animate.
 */
private fun RemoteDrawScope.drawSegments(
    segmentCount: Int,
    startAngle: RemoteFloat,
    endAngle: RemoteFloat,
    strokeWidth: RemoteDp,
    gapSize: RemoteDp,
    colors: RemoteProgressIndicatorColors,
    enabled: RemoteBoolean,
    segmentFractions: List<RemoteFloat>,
    targetSegmentFractions: List<RemoteFloat>,
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
    val arcRadius = arcDimen / 2f.rf
    val arcCenter = RemoteOffset(topLeft.x + arcRadius, topLeft.y + arcRadius)

    // Sweep angle of the gap between two segments.
    val chordRatio =
        clamp((strokePx + gapSizePx) / max(0.001f.rf, diameter - strokePx), 0f.rf, 1f.rf)
    val gapSweep = asin(chordRatio) * (360f / Math.PI.toFloat()).rf
    val segmentSweep = fullSweep / segmentCount.toFloat().rf

    val trackPaint = RemotePaint {
        style = PaintingStyle.Stroke
        this.strokeWidth = strokePx
        strokeCap = StrokeCap.Round
        with(colors.trackBrush(enabled)) { applyTo(this@RemotePaint, size) }
    }
    val indicatorStrokePx = strokePx + AntiAliasingStrokePadding.rf
    val indicatorPaint = RemotePaint {
        style = PaintingStyle.Stroke
        this.strokeWidth = indicatorStrokePx
        strokeCap = StrokeCap.Round
        with(colors.indicatorBrush(enabled)) { applyTo(this@RemotePaint, size) }
    }
    val indicatorColor = indicatorPaint.color
    val dotPaint = RemotePaint {
        style = PaintingStyle.Fill
        with(colors.indicatorBrush(enabled)) { applyTo(this@RemotePaint, size) }
    }

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

        // Indicator. Like Wear Material3, only its length animates, at the full stroke width.
        val fraction = segmentFractions[segment]
        val targetFraction = targetSegmentFractions[segment]
        val animatedSweep = segmentSweep * fraction
        val targetSweep = segmentSweep * targetFraction
        // A progress too small to fill the gap at the start of the segment is shown as a full dot,
        // when it is the target rather than a step of the animation.
        val holdsDot =
            targetSweep.isGreaterThan(0f.rf) and
                targetSweep.isLessThanOrEqualTo(gapSweep) and
                animatedSweep.isGreaterThan(0f.rf)
        val progressSweep = holdsDot.select(max(animatedSweep, gapSweep), animatedSweep)

        // An arc, which is a full dot at the start of the segment when it fills just the gap.
        val isArc = progressSweep.isGreaterThanOrEqualTo(gapSweep)
        indicatorPaint.color = isArc.select(indicatorColor, Color.Transparent.rc)
        drawArc(
            paint = indicatorPaint,
            startAngle = segmentStartAngle + gapSweep / 2f.rf,
            // A zero sweep draws nothing, while a tiny sweep with a round cap draws a dot.
            sweepAngle = isArc.select(max(MinDotSweep, progressSweep - gapSweep), 0f.rf),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
        )

        // While the animation grows a segment from nothing or shrinks it to nothing, the end of
        // the indicator is a dot, scaled and faded by how much of the gap the progress fills, so
        // that it doesn't pop in or linger at full size. A segment whose target fraction is
        // constant doesn't animate, and never needs that dot, since holdsDot shows any sweep up to
        // the gap as a full dot, so its dot is left out of the document. This is decided from the
        // target fraction, as the sweeps depend on the canvas size and are never constant.
        if (targetFraction.hasConstantValue) continue
        val isPartialDot = progressSweep.isGreaterThan(0f.rf) and !isArc
        val dotFraction = progressSweep / max(0.001f.rf, gapSweep)
        val dotRadius = indicatorStrokePx / 2f.rf * dotFraction
        val dotAngle = toRad(segmentStartAngle + progressSweep / 2f.rf)
        dotPaint.color =
            isPartialDot.select(
                indicatorColor.copy(
                    alpha = indicatorColor.alpha * min(1f.rf, dotRadius * 2f.rf / strokePx)
                ),
                Color.Transparent.rc,
            )
        drawCircle(
            paint = dotPaint,
            radius = isPartialDot.select(dotRadius, 0f.rf),
            center =
                RemoteOffset(
                    arcCenter.x + arcRadius * cos(dotAngle),
                    arcCenter.y + arcRadius * sin(dotAngle),
                ),
        )
    }
}

/** Extra stroke width (in px) for the indicator to cover the anti-aliased edges of the track. */
private const val AntiAliasingStrokePadding = 1f

/** Sweep angle used to draw a dot with a round stroke cap. */
private val MinDotSweep: RemoteFloat = 0.05f.rf
