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

private val MIN_SWEEP_ANGLE: RemoteFloat = 0.05f.rf

/**
 * A generalized progress arc component for display on Wear OS.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCurvedProgressIndicatorSample
 *
 * For an animated progress, see:
 *
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
 * @param dotCollapseFreezeFraction The progress fraction (0f to 1f) at which the collapsing dot
 *   freezes its minimum size. `0f` means continuous collapse down to 0; `1f` means freezing at full
 *   dot size without shrinking; for example, `0.5f` means that the dot won't collapse past half of
 *   its original size.
 * @param animationDurationMillis Duration of the player-side dynamic progress transition in
 *   milliseconds.
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
    dotCollapseFreezeFraction: RemoteFloat =
        RemoteProgressIndicatorDefaults.CurvedIndicatorDotCollapseFreezeFraction,
    animationDurationMillis: Int = RemoteProgressIndicatorDefaults.AnimationDurationMillis,
    reverseDirection: RemoteBoolean = false.rb,
) {
    RemoteCanvas(modifier = modifier.fillMaxSize()) {
        val animatedProgress: RemoteFloat =
            if (animationDurationMillis > 0) {
                remote.animateFloat(duration = animationDurationMillis / 1000f, rf = progress)
            } else {
                progress
            }

        val strokePx: RemoteFloat = strokeWidth.toPx()
        val paddingPx: RemoteFloat = padding.toPx()
        val diameter: RemoteFloat = min(width, height)
        val outerRadius: RemoteFloat = (diameter / 2f.rf) - paddingPx
        val arcRadius: RemoteFloat = outerRadius - (strokePx / 2f.rf)
        val arcDimen: RemoteFloat = max(0f.rf, arcRadius * 2f.rf)

        val left: RemoteFloat = (width / 2f.rf) - arcRadius
        val top: RemoteFloat = (height / 2f.rf) - arcRadius

        val radiansToDegrees: RemoteFloat = (180f / Math.PI.toFloat()).rf
        val thicknessDegrees: RemoteFloat = (strokePx / arcRadius) * radiansToDegrees

        val minProgress: RemoteFloat = thicknessDegrees / sweepAngle
        val maxProgress: RemoteFloat = 1.0f.rf - (gapAngleDegrees + thicknessDegrees) / sweepAngle

        val isZero: RemoteBoolean =
            animatedProgress.isLessThan(0f.rf) or animatedProgress.isEqualTo(0f.rf)
        val isOne: RemoteBoolean =
            animatedProgress.isGreaterThan(1f.rf) or animatedProgress.isEqualTo(1f.rf)
        val isEmptyOrFull: RemoteBoolean = isZero or isOne

        val clampedProgress: RemoteFloat =
            isEmptyOrFull.select(
                ifTrue = animatedProgress,
                ifFalse = clamp(animatedProgress, minProgress, maxProgress),
            )

        val underMinProgress: RemoteBoolean = animatedProgress.isLessThan(minProgress)
        val overMaxProgress: RemoteBoolean = animatedProgress.isGreaterThan(maxProgress)

        val activeScaleUnclamped: RemoteFloat = clamp(animatedProgress / minProgress, 0f.rf, 1f.rf)
        val remainingScaleUnclamped: RemoteFloat =
            clamp((1.0f.rf - animatedProgress) / (1.0f.rf - maxProgress), 0f.rf, 1f.rf)

        val activeScale: RemoteFloat = lerp(dotCollapseFreezeFraction, 1f.rf, activeScaleUnclamped)
        val remainingScale: RemoteFloat =
            lerp(dotCollapseFreezeFraction, 1f.rf, remainingScaleUnclamped)

        val activeStrokePx: RemoteFloat = underMinProgress.select(strokePx * activeScale, strokePx)
        val remainingStrokePx: RemoteFloat =
            overMaxProgress.select(strokePx * remainingScale, strokePx)

        val activeSweepBase: RemoteFloat = clampedProgress * sweepAngle - thicknessDegrees
        val activeSweepVal: RemoteFloat =
            underMinProgress.select(
                ifTrue = clamp(activeSweepBase * activeScale, MIN_SWEEP_ANGLE, sweepAngle),
                ifFalse =
                    overMaxProgress.select(
                        ifTrue =
                            activeSweepBase +
                                (sweepAngle - thicknessDegrees - activeSweepBase) *
                                    (1.0f.rf - remainingScale),
                        ifFalse = activeSweepBase,
                    ),
            )
        val activeSweep: RemoteFloat =
            isZero.select(
                ifTrue = 0f.rf,
                ifFalse = clamp(activeSweepVal, MIN_SWEEP_ANGLE, sweepAngle),
            )

        val activeSweepOffset: RemoteFloat =
            underMinProgress.select(
                ifTrue =
                    activeSweepBase * activeScale +
                        gapAngleDegrees +
                        (thicknessDegrees / 2f.rf) * (1.0f.rf + activeScale),
                ifFalse =
                    overMaxProgress.select(
                        ifTrue =
                            activeSweep +
                                gapAngleDegrees +
                                (thicknessDegrees / 2f.rf) * (1.0f.rf + remainingScale),
                        ifFalse = activeSweep + gapAngleDegrees + thicknessDegrees,
                    ),
            )

        val drawActiveStartBase: RemoteFloat = startAngle + (thicknessDegrees / 2f.rf)
        val remainingStartBase: RemoteFloat =
            isEmptyOrFull.select(
                ifTrue = drawActiveStartBase,
                ifFalse = drawActiveStartBase + activeSweepOffset,
            )

        val remainingSweepBase: RemoteFloat =
            isOne.select(
                ifTrue = 0f.rf,
                ifFalse =
                    isZero.select(
                        ifTrue = sweepAngle - thicknessDegrees,
                        ifFalse =
                            (1.0f.rf - clampedProgress) * sweepAngle -
                                gapAngleDegrees -
                                thicknessDegrees,
                    ),
            )

        val remainingSweepVal: RemoteFloat =
            overMaxProgress.select(
                ifTrue = clamp(remainingSweepBase * remainingScale, MIN_SWEEP_ANGLE, sweepAngle),
                ifFalse = remainingSweepBase,
            )

        val remainingSweep: RemoteFloat =
            isOne.select(
                ifTrue = 0f.rf,
                ifFalse = clamp(remainingSweepVal, MIN_SWEEP_ANGLE, sweepAngle),
            )

        val finalActiveStart: RemoteFloat =
            reverseDirection.select(
                ifTrue = 180f.rf - drawActiveStartBase,
                ifFalse = drawActiveStartBase,
            )
        val finalActiveSweep: RemoteFloat =
            reverseDirection.select(ifTrue = -activeSweep, ifFalse = activeSweep)

        val finalRemainingStart: RemoteFloat =
            reverseDirection.select(
                ifTrue = 180f.rf - remainingStartBase,
                ifFalse = remainingStartBase,
            )
        val finalRemainingSweep: RemoteFloat =
            reverseDirection.select(ifTrue = -remainingSweep, ifFalse = remainingSweep)

        // Remaining / Track Background
        val trackPaint = RemotePaint {
            style = PaintingStyle.Stroke
            this.strokeWidth = remainingStrokePx
            strokeCap = StrokeCap.Round
            with(colors.trackBrush(enabled)) { applyTo(this@RemotePaint, size) }
            this.color = isOne.select(Color.Transparent.rc, this.color)
        }
        drawArc(
            paint = trackPaint,
            startAngle = finalRemainingStart,
            sweepAngle = finalRemainingSweep,
            useCenter = false,
            topLeft = RemoteOffset(left, top),
            size = RemoteSize(arcDimen, arcDimen),
        )

        // Active / Indicator Foreground
        val indicatorPaint = RemotePaint {
            style = PaintingStyle.Stroke
            this.strokeWidth = activeStrokePx
            strokeCap = StrokeCap.Round
            with(colors.indicatorBrush(enabled)) { applyTo(this@RemotePaint, size) }
            this.color = isZero.select(Color.Transparent.rc, this.color)
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
}
