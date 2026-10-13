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
package androidx.wear.compose.remote.material3

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
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.PI

private const val SPRING_STIFFNESS = 50f
private const val SPRING_DAMPING_RATIO = 1f
private val MIN_SWEEP_ANGLE = 0.05f.rf
private val EPSILON = 0.001f.rf
private val RADIANS_TO_DEGREES = (180f / PI.toFloat()).rf

/**
 * A generalized progress arc component for display on Wear OS.
 *
 * To also animate the start and end of the progress, for example for a timer or a goal, use
 * [RemoteIntroOutroCurvedProgressIndicator].
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCurvedProgressIndicatorSample For an
 *   animated progress, see:
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCurvedProgressIndicatorAnimatedSample
 * @param progress A float value representing progress (typically 0f to 1f).
 * @param modifier Modifier to be applied to the canvas.
 * @param enabled controls the enabled state. When enabled is `false`, this component will appear
 *   visually disabled.
 * @param startAngle The starting position of the progress arc in degrees. For example, 135 is
 *   bottom left.
 * @param sweepAngle The total sweep angle of the progress arc in degrees. For example, 90 degrees.
 * @param colors [RemoteProgressIndicatorColors] that will be used to resolve the indicator and
 *   track color.
 * @param strokeWidth Width of the arc in dp.
 * @param padding Padding from the canvas/screen edge in dp.
 * @param gapAngleDegrees Size of the visual separation gap between segments in degrees. Set to 0f
 *   for continuous bars.
 * @param dotFadeOutFraction The fraction of the dot's size (from 0.0 to 1.0) below which the dot
 *   fades its opacity to transparent as it collapses/expands. Set to 0.0 to disable opacity fading
 *   and shrink purely through size.
 * @param dotCollapsible Whether the indicator's dot should collapse as it approaches the end of the
 *   track.
 * @param reverseDirection Whether to reverse the direction of the progress indicator (RTL).
 */
@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX")
public fun RemoteCurvedProgressIndicator(
    progress: RemoteFloat,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    startAngle: RemoteFloat = RemoteProgressIndicatorDefaults.CurvedIndicatorStartAngle,
    sweepAngle: RemoteFloat = RemoteProgressIndicatorDefaults.CurvedIndicatorSweepAngle,
    colors: RemoteProgressIndicatorColors = RemoteProgressIndicatorDefaults.colors(),
    strokeWidth: RemoteDp = RemoteProgressIndicatorDefaults.CurvedIndicatorStrokeWidth,
    padding: RemoteDp = RemoteProgressIndicatorDefaults.CurvedIndicatorPadding,
    gapAngleDegrees: RemoteFloat = RemoteProgressIndicatorDefaults.CurvedIndicatorGapAngleDegrees,
    dotFadeOutFraction: RemoteFloat =
        RemoteProgressIndicatorDefaults.CurvedIndicatorDotFadeOutFraction,
    dotCollapsible: RemoteBoolean = true.rb,
    reverseDirection: RemoteBoolean = false.rb,
) {
    RemoteCurvedProgressIndicatorImpl(
        progress = progress,
        animateIntroOutro = false,
        modifier = modifier,
        enabled = enabled,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        colors = colors,
        strokeWidth = strokeWidth,
        padding = padding,
        gapAngleDegrees = gapAngleDegrees,
        dotFadeOutFraction = dotFadeOutFraction,
        dotCollapsible = dotCollapsible,
        reverseDirection = reverseDirection,
        countDown = false.rb,
    )
}

/**
 * A [RemoteCurvedProgressIndicator] that plays spring animations at the start and end of the
 * progress, for example for a timer, or for a goal such as a distance, calories or steps.
 *
 * The intro animation plays as soon as [progress] leaves its start, and the outro animation plays
 * once [progress] reaches its end, so it finishes shortly after the end. In between, the arc
 * follows [progress] exactly. Pass a progress that is clamped to its range, so that reaching the
 * end gives exactly its end value.
 *
 * @param progress A float value representing progress, from 0f to 1f.
 * @param modifier Modifier to be applied to the canvas.
 * @param enabled controls the enabled state. When enabled is `false`, this component will appear
 *   visually disabled.
 * @param startAngle The starting position of the progress arc in degrees. For example, 135 is
 *   bottom left.
 * @param sweepAngle The total sweep angle of the progress arc in degrees. For example, 90 degrees.
 * @param colors [RemoteProgressIndicatorColors] that will be used to resolve the indicator and
 *   track color.
 * @param strokeWidth Width of the arc in dp.
 * @param padding Padding from the canvas/screen edge in dp.
 * @param gapAngleDegrees Size of the visual separation gap between segments in degrees. Set to 0f
 *   for continuous bars.
 * @param dotFadeOutFraction The fraction of the dot's size (from 0.0 to 1.0) below which the dot
 *   fades its opacity to transparent as it collapses/expands. Set to 0.0 to disable opacity fading
 *   and shrink purely through size.
 * @param reverseDirection Whether to reverse the direction of the progress indicator (RTL).
 * @param countDown Whether the progress goes from 1f down to 0f, for example the time or distance
 *   left. If true, the intro animation plays when progress leaves 1f, and the outro animation plays
 *   when progress reaches 0f.
 */
@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX")
public fun RemoteIntroOutroCurvedProgressIndicator(
    progress: RemoteFloat,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    startAngle: RemoteFloat = RemoteProgressIndicatorDefaults.CurvedIndicatorStartAngle,
    sweepAngle: RemoteFloat = RemoteProgressIndicatorDefaults.CurvedIndicatorSweepAngle,
    colors: RemoteProgressIndicatorColors = RemoteProgressIndicatorDefaults.colors(),
    strokeWidth: RemoteDp = RemoteProgressIndicatorDefaults.CurvedIndicatorStrokeWidth,
    padding: RemoteDp = RemoteProgressIndicatorDefaults.CurvedIndicatorPadding,
    gapAngleDegrees: RemoteFloat = RemoteProgressIndicatorDefaults.CurvedIndicatorGapAngleDegrees,
    dotFadeOutFraction: RemoteFloat =
        RemoteProgressIndicatorDefaults.CurvedIndicatorDotFadeOutFraction,
    reverseDirection: RemoteBoolean = false.rb,
    countDown: RemoteBoolean = false.rb,
) {
    RemoteCurvedProgressIndicatorImpl(
        progress = progress,
        animateIntroOutro = true,
        modifier = modifier,
        enabled = enabled,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        colors = colors,
        strokeWidth = strokeWidth,
        padding = padding,
        gapAngleDegrees = gapAngleDegrees,
        dotFadeOutFraction = dotFadeOutFraction,
        // The animations grow the dot at the start and collapse it at the end.
        dotCollapsible = true.rb,
        reverseDirection = reverseDirection,
        countDown = countDown,
    )
}

@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX")
private fun RemoteCurvedProgressIndicatorImpl(
    progress: RemoteFloat,
    animateIntroOutro: Boolean,
    modifier: RemoteModifier,
    enabled: RemoteBoolean,
    startAngle: RemoteFloat,
    sweepAngle: RemoteFloat,
    colors: RemoteProgressIndicatorColors,
    strokeWidth: RemoteDp,
    padding: RemoteDp,
    gapAngleDegrees: RemoteFloat,
    dotFadeOutFraction: RemoteFloat,
    dotCollapsible: RemoteBoolean,
    reverseDirection: RemoteBoolean,
    countDown: RemoteBoolean,
) {
    RemoteCanvas(modifier = modifier.fillMaxSize()) {
        val dotCollapseFreezeFraction = dotCollapsible.select(ifTrue = 0f.rf, ifFalse = 1f.rf)

        val strokePx = strokeWidth.toPx()
        val paddingPx = padding.toPx()
        val diameter = min(width, height)
        val outerRadius = (diameter / 2f.rf) - paddingPx
        val arcRadius = outerRadius - (strokePx / 2f.rf)
        val arcDimen = max(0f.rf, arcRadius * 2f.rf)
        val left = (width / 2f.rf) - arcRadius
        val top = (height / 2f.rf) - arcRadius

        val thicknessDegrees = (strokePx / max(EPSILON, arcRadius)) * RADIANS_TO_DEGREES
        val halfThicknessDegrees = thicknessDegrees / 2f.rf
        val totalTravel = gapAngleDegrees + thicknessDegrees
        val minProgress = max(EPSILON, totalTravel / max(EPSILON, sweepAngle))
        val maxProgress = 1.0f.rf - totalTravel / max(EPSILON, sweepAngle)
        val drawActiveStartBase = startAngle + halfThicknessDegrees

        // With the intro and outro animations, the same geometry is driven by a progress that
        // eases in and out at both ends.
        val animatedProgress =
            if (animateIntroOutro) {
                calculateAnimatedProgress(
                    progress = progress,
                    countDown = countDown,
                    endFraction = minProgress,
                )
            } else {
                progress
            }

        val arcParams =
            calculateCollapsibleArcParams(
                animatedProgress = animatedProgress,
                sweepAngle = sweepAngle,
                gapAngleDegrees = gapAngleDegrees,
                dotCollapsible = dotCollapsible,
                dotCollapseFreezeFraction = dotCollapseFreezeFraction,
                thicknessDegrees = thicknessDegrees,
                halfThicknessDegrees = halfThicknessDegrees,
                minProgress = minProgress,
                maxProgress = maxProgress,
                drawActiveStartBase = drawActiveStartBase,
                totalTravel = totalTravel,
            )

        drawCurvedProgressArcs(
            params = arcParams,
            reverseDirection = reverseDirection,
            strokePx = strokePx,
            colors = colors,
            enabled = enabled,
            left = left,
            top = top,
            arcDimen = arcDimen,
            dotFadeOutFraction = dotFadeOutFraction,
        )
    }
}

/** Resolved layout and style parameters for drawing active indicator and remaining track arcs. */
private class CurvedArcParams(
    val activeScale: RemoteFloat,
    val remainingScale: RemoteFloat,
    val activeStart: RemoteFloat,
    val activeSweep: RemoteFloat,
    val remainingStart: RemoteFloat,
    val remainingSweep: RemoteFloat,
    val isActiveHidden: RemoteBoolean,
    val isRemainingHidden: RemoteBoolean,
)

/**
 * Calculates arc layout and scaling parameters for static / collapsible progress. With the intro
 * and outro animations, it's driven by the progress from [calculateAnimatedProgress].
 */
private fun calculateCollapsibleArcParams(
    animatedProgress: RemoteFloat,
    sweepAngle: RemoteFloat,
    gapAngleDegrees: RemoteFloat,
    dotCollapsible: RemoteBoolean,
    dotCollapseFreezeFraction: RemoteFloat,
    thicknessDegrees: RemoteFloat,
    halfThicknessDegrees: RemoteFloat,
    minProgress: RemoteFloat,
    maxProgress: RemoteFloat,
    drawActiveStartBase: RemoteFloat,
    totalTravel: RemoteFloat,
): CurvedArcParams {
    val isZero = animatedProgress.isLessThanOrEqualTo(0f.rf)
    val isOne = animatedProgress.isGreaterThanOrEqualTo(1f.rf)
    val underMinProgress = animatedProgress.isLessThan(minProgress)
    val overMaxProgress = animatedProgress.isGreaterThan(maxProgress)

    val slideFraction = gapAngleDegrees / max(EPSILON, totalTravel)
    val growthFraction = max(EPSILON, 1.0f.rf - slideFraction)

    // Intro calculations (2-phase: track slides back, then active dot grows)
    val introProgress = clamp(animatedProgress / minProgress, 0f.rf, 1f.rf)
    val trackSlideProgress =
        lerp(
            dotCollapseFreezeFraction,
            1f.rf,
            clamp(introProgress / max(EPSILON, slideFraction), 0f.rf, 1f.rf),
        )
    val activeScaleUnclamped = clamp((introProgress - slideFraction) / growthFraction, 0f.rf, 1f.rf)
    val activeScale = lerp(dotCollapseFreezeFraction, 1f.rf, activeScaleUnclamped)

    // Outro calculations (2-phase: remaining dot collapses, then active sweep slides forward)
    val outroProgress =
        clamp((animatedProgress - maxProgress) / max(EPSILON, 1.0f.rf - maxProgress), 0f.rf, 1f.rf)
    val collapseFraction = growthFraction
    val remainingScaleUnclamped =
        clamp(1.0f.rf - outroProgress / max(EPSILON, collapseFraction), 0f.rf, 1f.rf)
    val remainingScale = lerp(dotCollapseFreezeFraction, 1f.rf, remainingScaleUnclamped)
    val outroSlideProgress =
        dotCollapsible.select(
            ifTrue =
                clamp(
                    (outroProgress - collapseFraction) / max(EPSILON, slideFraction),
                    0f.rf,
                    1f.rf,
                ),
            ifFalse = 0f.rf,
        )

    val activeStartShift = -halfThicknessDegrees * (1f.rf - activeScale)
    val shiftedActiveStartBase = drawActiveStartBase + activeStartShift

    val maxActiveSweep = max(0f.rf, sweepAngle - gapAngleDegrees - 2f.rf * thicknessDegrees)
    val sweepRange = max(EPSILON, maxProgress - minProgress)
    val sweepProgress = clamp((animatedProgress - minProgress) / sweepRange, 0f.rf, 1f.rf)
    val activeSweepBase = sweepProgress * maxActiveSweep

    val activeSweepVal =
        underMinProgress.select(
            ifTrue = MIN_SWEEP_ANGLE,
            ifFalse =
                isOne.select(
                    ifTrue = sweepAngle - thicknessDegrees,
                    ifFalse =
                        overMaxProgress.select(
                            ifTrue =
                                activeSweepBase +
                                    thicknessDegrees * (1.0f.rf - remainingScale) +
                                    gapAngleDegrees * outroSlideProgress,
                            ifFalse = activeSweepBase,
                        ),
                ),
        )

    val activeSweep =
        isZero.select(ifTrue = 0f.rf, ifFalse = clamp(activeSweepVal, MIN_SWEEP_ANGLE, sweepAngle))

    val clampedActiveSweep = clamp(activeSweepVal, MIN_SWEEP_ANGLE, sweepAngle)
    val outroAnchorShift = halfThicknessDegrees * (1f.rf - remainingScale)

    val activeSweepOffset =
        underMinProgress.select(
            ifTrue = gapAngleDegrees * trackSlideProgress + thicknessDegrees * activeScale,
            ifFalse = clampedActiveSweep + gapAngleDegrees + thicknessDegrees,
        )

    val expectedRemainingEnd =
        drawActiveStartBase + sweepAngle - thicknessDegrees + outroAnchorShift

    val remainingStartBase =
        isZero.select(
            ifTrue = drawActiveStartBase,
            ifFalse =
                overMaxProgress.select(
                    ifTrue = expectedRemainingEnd,
                    ifFalse = drawActiveStartBase + activeSweepOffset,
                ),
        )

    val expectedRemainingSweep =
        overMaxProgress.select(
            ifTrue = MIN_SWEEP_ANGLE,
            ifFalse = max(MIN_SWEEP_ANGLE, expectedRemainingEnd - remainingStartBase),
        )

    val isJumpToEndEnabled = !dotCollapsible
    val isRemainingHidden =
        remainingScale.isLessThanOrEqualTo(0f.rf) or (isOne and isJumpToEndEnabled)
    val remainingSweep =
        isRemainingHidden.select(
            ifTrue = 0f.rf,
            ifFalse =
                isOne.select(
                    ifTrue = 0f.rf,
                    ifFalse = clamp(expectedRemainingSweep, 0f.rf, sweepAngle),
                ),
        )

    val isActiveHidden = activeScale.isLessThanOrEqualTo(0f.rf) or isZero

    return CurvedArcParams(
        activeScale = activeScale,
        remainingScale = remainingScale,
        activeStart = shiftedActiveStartBase,
        activeSweep = activeSweep,
        remainingStart = remainingStartBase,
        remainingSweep = remainingSweep,
        isActiveHidden = isActiveHidden,
        isRemainingHidden = isRemainingHidden,
    )
}

/**
 * Calculates the progress that drives [calculateCollapsibleArcParams] when the intro and outro are
 * animated.
 *
 * The collapsible geometry plays the intro while its progress goes from 0 to [endFraction], and the
 * outro while it goes from 1 - [endFraction] to 1. With the animations, each end is driven by a
 * spring instead, so that it takes the same time whatever the progress speed, while the bar follows
 * the progress from start to end:
 * - the intro spring starts as soon as the progress leaves its start;
 * - the outro spring starts once the progress reaches its end, and ends about 0.85 s later.
 *
 * Between the two springs, the indicator at X % is the mirror image of the one at (100 - X) %.
 */
@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.calculateAnimatedProgress(
    progress: RemoteFloat,
    countDown: RemoteBoolean,
    endFraction: RemoteFloat,
): RemoteFloat {
    val elapsedProgress = countDown.select(ifTrue = 1.0f.rf - progress, ifFalse = progress)

    val animateScale = { target: RemoteFloat ->
        remote.animateSpring(
            rf = target,
            stiffness = SPRING_STIFFNESS,
            dampingRatio = SPRING_DAMPING_RATIO,
        )
    }
    val introScale =
        animateScale(
            elapsedProgress.isLessThanOrEqualTo(0f.rf).select(ifTrue = 0f.rf, ifFalse = 1f.rf)
        )
    val outroScale =
        animateScale(
            elapsedProgress.isGreaterThanOrEqualTo(1f.rf).select(ifTrue = 1f.rf, ifFalse = 0f.rf)
        )

    // The bar follows the progress from start to end, and the springs play on top of it.
    val barProgress = clamp(elapsedProgress, 0f.rf, 1f.rf)

    // Guards very short arcs, where the two ends would take more than the whole arc.
    val clampedEndFraction = min(endFraction, 0.5f.rf)
    val easedProgress =
        clampedEndFraction * introScale +
            (1f.rf - 2f.rf * clampedEndFraction) * barProgress +
            clampedEndFraction * outroScale

    return countDown.select(ifTrue = 1.0f.rf - easedProgress, ifFalse = easedProgress)
}

/** Renders the track and active indicator arcs onto the [RemoteCanvas]. */
private fun RemoteDrawScope.drawCurvedProgressArcs(
    params: CurvedArcParams,
    reverseDirection: RemoteBoolean,
    strokePx: RemoteFloat,
    colors: RemoteProgressIndicatorColors,
    enabled: RemoteBoolean,
    left: RemoteFloat,
    top: RemoteFloat,
    arcDimen: RemoteFloat,
    dotFadeOutFraction: RemoteFloat,
) {
    val isFadeDisabled = dotFadeOutFraction.isLessThanOrEqualTo(0f.rf)
    val safeFadeDivisor = max(EPSILON, dotFadeOutFraction)
    val activeAlpha =
        isFadeDisabled.select(
            ifTrue = 1f.rf,
            ifFalse = clamp(params.activeScale / safeFadeDivisor, 0f.rf, 1f.rf),
        )
    val remainingAlpha =
        isFadeDisabled.select(
            ifTrue = 1f.rf,
            ifFalse = clamp(params.remainingScale / safeFadeDivisor, 0f.rf, 1f.rf),
        )
    val activeStrokePx = strokePx * params.activeScale
    val remainingStrokePx = strokePx * params.remainingScale

    val adjustStart = { start: RemoteFloat ->
        reverseDirection.select(ifTrue = 180f.rf - start, ifFalse = start)
    }
    val adjustSweep = { sweep: RemoteFloat ->
        reverseDirection.select(ifTrue = -sweep, ifFalse = sweep)
    }

    val finalActiveStart = adjustStart(params.activeStart)
    val finalActiveSweep = adjustSweep(params.activeSweep)
    val finalRemainingStart = adjustStart(params.remainingStart)
    val finalRemainingSweep = adjustSweep(params.remainingSweep)

    val trackPaint = RemotePaint {
        style = PaintingStyle.Stroke
        this.strokeWidth = remainingStrokePx
        strokeCap = StrokeCap.Round
        with(colors.trackBrush(enabled)) { applyTo(this@RemotePaint, size) }
        this.color = this.color.copy(alpha = this.color.alpha * remainingAlpha)
        this.color = params.isRemainingHidden.select(Color.Transparent.rc, this.color)
    }

    drawArc(
        paint = trackPaint,
        startAngle = finalRemainingStart,
        sweepAngle = finalRemainingSweep,
        useCenter = false,
        topLeft = RemoteOffset(left, top),
        size = RemoteSize(arcDimen, arcDimen),
    )

    val indicatorPaint = RemotePaint {
        style = PaintingStyle.Stroke
        this.strokeWidth = activeStrokePx
        strokeCap = StrokeCap.Round
        with(colors.indicatorBrush(enabled)) { applyTo(this@RemotePaint, size) }
        this.color = this.color.copy(alpha = this.color.alpha * activeAlpha)
        this.color = params.isActiveHidden.select(Color.Transparent.rc, this.color)
    }

    drawArc(
        paint = indicatorPaint,
        startAngle = finalActiveStart,
        sweepAngle = finalActiveSweep,
        useCenter = false,
        topLeft = RemoteOffset(left, top),
        size = RemoteSize(arcDimen, arcDimen),
    )
}
