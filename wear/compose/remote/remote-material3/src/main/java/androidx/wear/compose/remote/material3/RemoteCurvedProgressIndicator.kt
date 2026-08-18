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

private const val INTRO_DURATION_SEC = 0.85f
private const val OUTRO_DURATION_SEC = 0.85f
private const val MILLIS_IN_SECOND = 1000f
private const val SPRING_STIFFNESS = 50f
private const val SPRING_DAMPING_RATIO = 1f
private const val MIN_OUTRO_START_FRACTION = 0.5f
private const val MAX_OUTRO_START_FRACTION = 0.999f
private const val INTRO_SWEEP_OVERLAP_MULTIPLIER = 0.5f
private val MIN_SWEEP_ANGLE = 0.05f.rf
private val EPSILON = 0.001f.rf
private val RADIANS_TO_DEGREES = (180f / Math.PI.toFloat()).rf

/**
 * A generalized progress arc component for display on Wear OS.
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
        totalTimerDurationMillis = 0L,
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
 * A specialized progress arc component for timers that includes spring physics animations at the
 * start and end of the timer duration.
 *
 * @param progress A float value representing progress (typically 0f to 1f).
 * @param totalTimerDurationMillis Total duration of the timer in milliseconds. Needed because
 *   fixed-duration intro and outro animations run for a constant amount of time regardless of timer
 *   length, requiring the component to calculate progress elapsed during these transitions.
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
 * @param countDown Whether this is a countdown timer (i.e., progress goes from top to bottom). If
 *   true, the intro animation plays when progress is near 1.0, and the outro animation plays when
 *   progress approaches 0.0. If false, the other way around will be done.
 */
@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX")
public fun RemoteCurvedProgressIndicator(
    progress: RemoteFloat,
    totalTimerDurationMillis: Long,
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
        totalTimerDurationMillis = totalTimerDurationMillis,
        modifier = modifier,
        enabled = enabled,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        colors = colors,
        strokeWidth = strokeWidth,
        padding = padding,
        gapAngleDegrees = gapAngleDegrees,
        dotFadeOutFraction = dotFadeOutFraction,
        dotCollapsible = false.rb,
        reverseDirection = reverseDirection,
        countDown = countDown,
    )
}

@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX")
private fun RemoteCurvedProgressIndicatorImpl(
    progress: RemoteFloat,
    totalTimerDurationMillis: Long,
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
        val hasTimer = totalTimerDurationMillis > 0L
        val totalDurationSec =
            if (hasTimer) totalTimerDurationMillis.toFloat() / MILLIS_IN_SECOND else 1f
        val dotCollapseFreezeFraction = dotCollapsible.select(ifTrue = 0f.rf, ifFalse = 1f.rf)

        val animatedProgress = progress

        val strokePx = strokeWidth.toPx()
        val paddingPx = padding.toPx()
        val diameter = min(width, height)
        val outerRadius = (diameter / 2f.rf) - paddingPx
        val arcRadius = outerRadius - (strokePx / 2f.rf)
        val arcDimen = max(0f.rf, arcRadius * 2f.rf)
        val left = (width / 2f.rf) - arcRadius
        val top = (height / 2f.rf) - arcRadius

        val thicknessDegrees = (strokePx / arcRadius) * RADIANS_TO_DEGREES
        val halfThicknessDegrees = thicknessDegrees / 2f.rf
        val totalTravel = gapAngleDegrees + thicknessDegrees
        val minProgress = totalTravel / sweepAngle
        val maxProgress = 1.0f.rf - totalTravel / sweepAngle
        val drawActiveStartBase = startAngle + halfThicknessDegrees
        val totalOutroTravel = totalTravel

        val arcParams =
            if (hasTimer) {
                calculateTimerArcParams(
                    animatedProgress = animatedProgress,
                    totalDurationSec = totalDurationSec,
                    countDown = countDown,
                    dotCollapseFreezeFraction = dotCollapseFreezeFraction,
                    sweepAngle = sweepAngle,
                    gapAngleDegrees = gapAngleDegrees,
                    thicknessDegrees = thicknessDegrees,
                    halfThicknessDegrees = halfThicknessDegrees,
                    maxProgress = maxProgress,
                    drawActiveStartBase = drawActiveStartBase,
                    totalOutroTravel = totalOutroTravel,
                )
            } else {
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
            }

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
 * Calculates arc layout and scaling parameters for static / collapsible progress (when
 * totalTimerDurationMillis == 0).
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
    val trackSlideProgress = clamp(introProgress / slideFraction, 0f.rf, 1f.rf)
    val activeScaleUnclamped = clamp((introProgress - slideFraction) / growthFraction, 0f.rf, 1f.rf)
    val activeScale = lerp(dotCollapseFreezeFraction, 1f.rf, activeScaleUnclamped)

    // Outro calculations (2-phase: remaining dot collapses, then active sweep slides forward)
    val outroProgress =
        clamp((animatedProgress - maxProgress) / max(EPSILON, 1.0f.rf - maxProgress), 0f.rf, 1f.rf)
    val collapseFraction = growthFraction
    val remainingScaleUnclamped = clamp(1.0f.rf - outroProgress / collapseFraction, 0f.rf, 1f.rf)
    val remainingScale = lerp(dotCollapseFreezeFraction, 1f.rf, remainingScaleUnclamped)
    val outroSlideProgress =
        dotCollapsible.select(
            ifTrue = clamp((outroProgress - collapseFraction) / slideFraction, 0f.rf, 1f.rf),
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
 * Calculates arc layout and scaling parameters for continuous timer animations (when
 * totalTimerDurationMillis > 0).
 */
@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.calculateTimerArcParams(
    animatedProgress: RemoteFloat,
    totalDurationSec: Float,
    countDown: RemoteBoolean,
    dotCollapseFreezeFraction: RemoteFloat,
    sweepAngle: RemoteFloat,
    gapAngleDegrees: RemoteFloat,
    thicknessDegrees: RemoteFloat,
    halfThicknessDegrees: RemoteFloat,
    maxProgress: RemoteFloat,
    drawActiveStartBase: RemoteFloat,
    totalOutroTravel: RemoteFloat,
): CurvedArcParams {
    val elapsedTimerProgress =
        countDown.select(ifTrue = 1.0f.rf - animatedProgress, ifFalse = animatedProgress)

    val isZero = elapsedTimerProgress.isLessThanOrEqualTo(0f.rf)
    val isOne = elapsedTimerProgress.isGreaterThanOrEqualTo(1f.rf)

    // Evaluate fractions locally using standard Kotlin to bypass constructing redundant remote AST
    // nodes
    val fullOutroFraction = OUTRO_DURATION_SEC / totalDurationSec
    val outroCollapseStartThreshold =
        (1.0f - fullOutroFraction).coerceIn(MIN_OUTRO_START_FRACTION, MAX_OUTRO_START_FRACTION).rf

    val effectiveOutroDurationSec = OUTRO_DURATION_SEC.rf * dotCollapseFreezeFraction
    val outroFraction = effectiveOutroDurationSec / totalDurationSec.rf
    val outroTriggerThreshold =
        clamp(1.0f.rf - outroFraction, MIN_OUTRO_START_FRACTION.rf, MAX_OUTRO_START_FRACTION.rf)

    val isOverCollapseStart =
        elapsedTimerProgress.isGreaterThanOrEqualTo(outroCollapseStartThreshold)
    val isOverOutroThreshold = elapsedTimerProgress.isGreaterThan(outroTriggerThreshold)

    val remainingScaleRange = max(EPSILON, outroTriggerThreshold - outroCollapseStartThreshold)
    val remainingScaleUnclamped =
        clamp((outroTriggerThreshold - elapsedTimerProgress) / remainingScaleRange, 0f.rf, 1f.rf)

    val animateScale = { target: RemoteFloat ->
        remote.animateSpring(
            rf = target,
            stiffness = SPRING_STIFFNESS,
            dampingRatio = SPRING_DAMPING_RATIO,
        )
    }

    val activeIntroScale = animateScale(isZero.select(ifTrue = 0f.rf, ifFalse = 1f.rf))
    val remainingOutroScale =
        animateScale(isOverOutroThreshold.select(ifTrue = 0f.rf, ifFalse = 1f.rf))

    val introAnimationScale = activeIntroScale

    val trackSlideProgress = clamp(introAnimationScale / 0.5f.rf, 0f.rf, 1f.rf)
    val dotProgress = clamp((introAnimationScale - 0.5f.rf) / 0.5f.rf, 0f.rf, 1f.rf)

    val dynamicScale = dotProgress
    val outroBaseScale = isZero.select(ifTrue = 1f.rf, ifFalse = remainingOutroScale)

    val outroProgress = 1.0f.rf - outroBaseScale
    val collapseFraction = thicknessDegrees / max(EPSILON, totalOutroTravel)

    val outroSlideProgress =
        clamp(
            (outroProgress - collapseFraction) / max(1f.rf - collapseFraction, 0.001f.rf),
            0f.rf,
            1f.rf,
        )

    val collapsedScale = clamp(1.0f.rf - outroProgress / collapseFraction, 0f.rf, 1f.rf)

    val activeScale = countDown.select(ifTrue = collapsedScale, ifFalse = dynamicScale)
    val remainingScale = countDown.select(ifTrue = dynamicScale, ifFalse = collapsedScale)

    val fullIntroFraction = INTRO_DURATION_SEC / totalDurationSec
    val clampedOverlap = fullIntroFraction * INTRO_SWEEP_OVERLAP_MULTIPLIER
    val startSweepProgress = clamp(clampedOverlap.rf, 0f.rf, maxProgress)

    val maxActiveSweep = max(0f.rf, sweepAngle - gapAngleDegrees - 2f.rf * thicknessDegrees)
    val sweepRange = max(EPSILON, outroTriggerThreshold - startSweepProgress)
    val sweepProgress =
        clamp((elapsedTimerProgress - startSweepProgress) / sweepRange, 0f.rf, 1f.rf)

    val effectiveSweepProgress =
        countDown.select(ifTrue = 1.0f.rf - sweepProgress, ifFalse = sweepProgress)
    val activeSweepBase = effectiveSweepProgress * maxActiveSweep

    val countDownIntroExtra =
        isOverCollapseStart.select(
            ifTrue = 0f.rf,
            ifFalse =
                gapAngleDegrees * (1f.rf - trackSlideProgress) +
                    thicknessDegrees * (1f.rf - dotProgress),
        )

    val activeSweepVal =
        countDown.select(
            ifTrue = activeSweepBase + countDownIntroExtra,
            ifFalse = activeSweepBase + totalOutroTravel * outroProgress,
        )

    val activeSweep = clamp(activeSweepVal, MIN_SWEEP_ANGLE, sweepAngle)
    val clampedActiveSweepBase = clamp(activeSweepBase, MIN_SWEEP_ANGLE, sweepAngle)

    val outroAnchorShift = halfThicknessDegrees * (1f.rf - remainingScale)

    val slideFactor =
        countDown.select(ifTrue = 1f.rf - outroSlideProgress, ifFalse = trackSlideProgress)
    val scaleFactor =
        countDown.select(
            ifTrue = isOverOutroThreshold.select(ifTrue = activeScale, ifFalse = 1f.rf),
            ifFalse = activeScale,
        )

    val activeSweepOffset =
        clampedActiveSweepBase +
            gapAngleDegrees * slideFactor +
            thicknessDegrees * scaleFactor +
            outroAnchorShift

    val remainingStartBase = drawActiveStartBase + activeSweepOffset

    val expectedRemainingEnd =
        drawActiveStartBase + sweepAngle - thicknessDegrees + outroAnchorShift

    val expectedRemainingSweep = max(MIN_SWEEP_ANGLE, expectedRemainingEnd - remainingStartBase)
    val isRemainingHidden = remainingScale.isLessThanOrEqualTo(0f.rf)
    val remainingSweep =
        isRemainingHidden.select(
            ifTrue = 0f.rf,
            ifFalse = clamp(expectedRemainingSweep, 0f.rf, sweepAngle),
        )

    val activeStartShift = -halfThicknessDegrees * (1f.rf - activeScale)
    val shiftedActiveStartBase = drawActiveStartBase + activeStartShift
    val isActiveHidden = activeScale.isLessThanOrEqualTo(0f.rf)

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
