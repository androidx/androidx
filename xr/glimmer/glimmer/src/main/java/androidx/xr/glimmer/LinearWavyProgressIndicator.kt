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

package androidx.xr.glimmer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.xr.glimmer.internal.progressindicator.IncreaseVerticalSemanticsBounds
import androidx.xr.glimmer.internal.progressindicator.IndicatorAmplitude
import androidx.xr.glimmer.internal.progressindicator.LinearIndicatorWidth
import androidx.xr.glimmer.internal.progressindicator.LinearStrokeCap
import androidx.xr.glimmer.internal.progressindicator.MotionTokens
import androidx.xr.glimmer.internal.progressindicator.ProgressIndicatorGapSize
import androidx.xr.glimmer.internal.progressindicator.ProgressIndicatorStrokeWidth
import androidx.xr.glimmer.internal.progressindicator.linearWavyProgressIndicator

/**
 * A determinate wavy progress indicator that shows how far a process has advanced.
 *
 * The progress line follows a wave that moves by one [wavelength] per second, while the track stays
 * straight. The wave, including its stroke, fills the indicator's height and flattens when the
 * progress is close to empty or full. There is no animation between [progress] values; animate the
 * value before passing it in if needed.
 *
 * @sample androidx.xr.glimmer.samples.LinearWavyProgressIndicatorSample
 * @param progress the progress of this indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range. Return a
 *   non-finite value, such as [Float.NaN], while the progress is unknown to show an indeterminate
 *   indicator.
 * @param modifier the [Modifier] to be applied to this indicator
 * @param color the [Color] of the progress line
 * @param trackColor the [Color] of the track that the progress line hasn't reached yet. If
 *   [Color.Unspecified], uses a less prominent shade of the content color provided by the nearest
 *   [surface].
 * @param strokeWidth the thickness of the progress line and the track
 * @param gapSize the visible gap between the progress line and the track
 * @param wavelength the length of one wave
 */
@Composable
public fun LinearWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = LinearWavyProgressIndicatorDefaults.color,
    trackColor: Color = Color.Unspecified,
    strokeWidth: Dp = LinearWavyProgressIndicatorDefaults.StrokeWidth,
    gapSize: Dp = LinearWavyProgressIndicatorDefaults.GapSize,
    wavelength: Dp = LinearWavyProgressIndicatorDefaults.DeterminateWavelength,
) {
    val stroke =
        with(LocalDensity.current) { Stroke(width = strokeWidth.toPx(), cap = LinearStrokeCap) }
    Spacer(
        modifier =
            modifier
                .then(IncreaseVerticalSemanticsBounds)
                .semantics(mergeDescendants = true) {
                    val currentProgress = progress()
                    progressBarRangeInfo =
                        if (!currentProgress.isFinite()) {
                            ProgressBarRangeInfo.Indeterminate
                        } else {
                            ProgressBarRangeInfo(currentProgress.fastCoerceIn(0f, 1f), 0f..1f)
                        }
                }
                .size(LinearIndicatorWidth, LinearWavyIndicatorHeight)
                .clipToBounds()
                .linearWavyProgressIndicator(
                    progress = progress,
                    color = color,
                    trackColor = trackColor,
                    stroke = stroke,
                    trackStroke = stroke,
                    gapSize = gapSize,
                    amplitude = IndicatorAmplitude,
                    wavelength = wavelength,
                    waveSpeed = wavelength, // Match to 1 wavelength per second
                )
    )
}

/**
 * An indeterminate wavy progress indicator that shows that a process is ongoing, without indicating
 * how far it has advanced.
 *
 * The progress lines follow a wave that moves by one [wavelength] per second, while the track stays
 * straight. The wave, including its stroke, fills the indicator's height.
 *
 * @sample androidx.xr.glimmer.samples.IndeterminateLinearWavyProgressIndicatorSample
 * @param modifier the [Modifier] to be applied to this indicator
 * @param color the [Color] of the moving progress lines
 * @param trackColor the [Color] of the track behind the progress lines. If [Color.Unspecified],
 *   uses a less prominent shade of the content color provided by the nearest [surface].
 * @param strokeWidth the thickness of the progress lines and the track
 * @param gapSize the visible gap between the progress lines and the track
 * @param wavelength the length of one wave
 */
@Composable
public fun LinearWavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = LinearWavyProgressIndicatorDefaults.color,
    trackColor: Color = Color.Unspecified,
    strokeWidth: Dp = LinearWavyProgressIndicatorDefaults.StrokeWidth,
    gapSize: Dp = LinearWavyProgressIndicatorDefaults.GapSize,
    wavelength: Dp = LinearWavyProgressIndicatorDefaults.IndeterminateWavelength,
) {
    val stroke =
        with(LocalDensity.current) { Stroke(width = strokeWidth.toPx(), cap = LinearStrokeCap) }
    Spacer(
        modifier =
            modifier
                .then(IncreaseVerticalSemanticsBounds)
                .progressSemantics()
                .size(LinearIndicatorWidth, LinearWavyIndicatorHeight)
                .clipToBounds()
                .linearWavyProgressIndicator(
                    progress = { Float.NaN },
                    color = color,
                    trackColor = trackColor,
                    stroke = stroke,
                    trackStroke = stroke,
                    gapSize = gapSize,
                    amplitude = { 1f },
                    wavelength = wavelength,
                    waveSpeed = wavelength, // Match to 1 wavelength per second
                    indeterminateWavelength = wavelength,
                    indeterminateWaveSpeed = wavelength,
                )
    )
}

/** Contains default values used by [LinearWavyProgressIndicator]. */
public object LinearWavyProgressIndicatorDefaults {
    /** Default color of the progress line. */
    public val color: Color
        @Composable get() = LinearProgressIndicatorDefaults.color

    /** Default thickness of the progress line and the track. */
    public val StrokeWidth: Dp = ProgressIndicatorStrokeWidth

    /** Default visible gap between the progress line and the track. */
    public val GapSize: Dp = ProgressIndicatorGapSize

    /** Default length of one wave in a determinate [LinearWavyProgressIndicator]. */
    public val DeterminateWavelength: Dp = 40.dp

    /** Default length of one wave in an indeterminate [LinearWavyProgressIndicator]. */
    public val IndeterminateWavelength: Dp = 20.dp
}

/**
 * A default [AnimationSpec] that should be used when animating between progress in a determinate
 * wavy progress indicator.
 */
internal val WavyProgressAnimationSpec: AnimationSpec<Float> =
    tween(
        durationMillis = MotionTokens.DurationLong2.toInt(),
        easing = MotionTokens.EasingLinearCubicBezier,
    )

// The default stroke plus 3dp of wave above and below it.
private val LinearWavyIndicatorHeight = ProgressIndicatorStrokeWidth + 6.dp
