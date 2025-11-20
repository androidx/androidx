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

package androidx.compose.material3

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.internal.IncreaseVerticalSemanticsBounds
import androidx.compose.material3.internal.circularWavyProgressIndicator
import androidx.compose.material3.internal.linearWavyProgressIndicator
import androidx.compose.material3.tokens.CircularProgressIndicatorTokens
import androidx.compose.material3.tokens.LinearProgressIndicatorTokens
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.material3.tokens.ProgressIndicatorTokens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn

// TODO Update the docs images to point to the expressive (wavy) versions of the progress indicators
/**
 * [Material Design determinate wavy linear progress
 * indicator](https://m3.material.io/components/progress-indicators/overview)
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Linear wavy progress indicator
 * image](https://developer.android.com/images/reference/androidx/compose/material3/linear-wavy-progress-indicator.png)
 *
 * This version of a linear progress indicator accepts arguments, such as [amplitude], [wavelength],
 * and [waveSpeed] to render the progress as a waveform.
 *
 * By default there is no animation between [progress] values. You can use
 * [WavyProgressIndicatorDefaults.ProgressAnimationSpec] as the default recommended [AnimationSpec]
 * when animating progress, such as in the following example:
 *
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color the progress indicator color
 * @param trackColor the indicator's track color, visible when the progress has not reached the area
 *   of the overall indicator yet
 * @param stroke a [Stroke] that will be used to draw this indicator
 * @param trackStroke a [Stroke] that will be used to draw the indicator's track
 * @param gapSize the gap between the track and the progress parts of the indicator
 * @param stopSize the size of the stop indicator at the end of the track. Note that the stop
 *   indicator is required if the track has a contrast below 3:1 with its container or the surface
 *   behind the container.
 * @param amplitude a lambda that provides an amplitude for the wave path as a function of the
 *   indicator's progress. 0.0 represents no amplitude, and 1.0 represents an amplitude that will
 *   take the full height of the progress indicator. Values outside of this range are coerced into
 *   the range.
 * @param wavelength the length of a wave. Will be applied in case the path has an [amplitude] that
 *   is greater than zero and represents a wave.
 * @param waveSpeed the speed in which the wave will move when the [amplitude] is greater than zero.
 *   The value here represents a DP per seconds, and by default it's matched to the [wavelength] to
 *   render an animation that moves the wave by one wave length per second.
 * @sample androidx.compose.material3.samples.LinearWavyProgressIndicatorSample
 *
 * You may also follow the Material guidelines to create a thicker version of this indicator, like
 * in this example:
 *
 * @sample androidx.compose.material3.samples.LinearThickWavyProgressIndicatorSample
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LinearWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    stroke: Stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
    trackStroke: Stroke = WavyProgressIndicatorDefaults.linearTrackStroke,
    gapSize: Dp = WavyProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
    stopSize: Dp = WavyProgressIndicatorDefaults.LinearTrackStopIndicatorSize,
    amplitude: (progress: Float) -> Float = WavyProgressIndicatorDefaults.indicatorAmplitude,
    wavelength: Dp = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
    waveSpeed: Dp = wavelength, // Match to 1 wavelength per second
) {
    Spacer(
        modifier =
            modifier
                .then(IncreaseVerticalSemanticsBounds)
                .semantics(mergeDescendants = true) {
                    // Ensure progress lambda is accessed safely for semantics
                    val currentProgress = progress().takeUnless { it.isNaN() } ?: 0f
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(currentProgress.fastCoerceIn(0f, 1f), 0f..1f)
                }
                .size(
                    width = WavyProgressIndicatorDefaults.LinearContainerWidth,
                    height = WavyProgressIndicatorDefaults.LinearContainerHeight,
                )
                .clipToBounds()
                .linearWavyProgressIndicator(
                    progress = progress,
                    color = color,
                    trackColor = trackColor,
                    stroke = stroke,
                    trackStroke = trackStroke,
                    gapSize = gapSize,
                    stopSize = stopSize,
                    amplitude = amplitude,
                    wavelength = wavelength,
                    waveSpeed = waveSpeed,
                )
    )
}

// TODO Update the docs images to point to the expressive (wavy) versions of the progress indicators
/**
 * [Material Design indeterminate linear wavy progress
 * indicator](https://m3.material.io/components/progress-indicators/overview)
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Indeterminate linear wavy progress indicator
 * image](https://developer.android.com/images/reference/androidx/compose/material3/indeterminate-linear-wavy-progress-indicator.png)
 *
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color the progress indicator color
 * @param trackColor the indicator's track color, visible when the progress has not reached the area
 *   of the overall indicator yet
 * @param stroke a [Stroke] that will be used to draw this indicator
 * @param trackStroke a [Stroke] that will be used to draw the indicator's track
 * @param gapSize the gap between the track and the progress parts of the indicator
 * @param amplitude the wave's amplitude. 0.0 represents no amplitude, and 1.0 represents an
 *   amplitude that will take the full height of the progress indicator. Values outside of this
 *   range are coerced into the range.
 * @param wavelength the length of a wave
 * @param waveSpeed the speed in which the wave will move when the [amplitude] is greater than zero.
 *   The value here represents a DP per seconds, and by default it's matched to the [wavelength] to
 *   render an animation that moves the wave by one wave length per second.
 * @sample androidx.compose.material3.samples.IndeterminateLinearWavyProgressIndicatorSample
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LinearWavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    stroke: Stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
    trackStroke: Stroke = WavyProgressIndicatorDefaults.linearTrackStroke,
    gapSize: Dp = WavyProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
    @FloatRange(from = 0.0, to = 1.0) amplitude: Float = 1f,
    wavelength: Dp = WavyProgressIndicatorDefaults.LinearIndeterminateWavelength,
    waveSpeed: Dp = wavelength, // Match to 1 wavelength per second
) {
    val infiniteTransition = rememberInfiniteTransition("LinearWavyProgressIndicatorProgress")
    val firstLineHead =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateFirstLineHeadAnimationSpec,
            label = "LinearWavyProgressIndicatorFirstHead",
        )
    val firstLineTail =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateFirstLineTailAnimationSpec,
            label = "LinearWavyProgressIndicatorFirstTail",
        )
    val secondLineHead =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateSecondLineHeadAnimationSpec,
            label = "LinearWavyProgressIndicatorSecondHead",
        )
    val secondLineTail =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateSecondLineTailAnimationSpec,
            label = "LinearWavyProgressIndicatorSecondTail",
        )

    Spacer(
        modifier =
            modifier
                .then(IncreaseVerticalSemanticsBounds)
                .progressSemantics()
                .size(
                    WavyProgressIndicatorDefaults.LinearContainerWidth,
                    WavyProgressIndicatorDefaults.LinearContainerHeight,
                )
                .clipToBounds()
                .linearWavyProgressIndicator(
                    firstLineHeadProgress = { firstLineHead.value },
                    firstLineTailProgress = { firstLineTail.value },
                    secondLineHeadProgress = { secondLineHead.value },
                    secondLineTailProgress = { secondLineTail.value },
                    color = color,
                    trackColor = trackColor,
                    stroke = stroke,
                    trackStroke = trackStroke,
                    gapSize = gapSize,
                    amplitude = amplitude.fastCoerceIn(0f, 1f),
                    wavelength = wavelength,
                    waveSpeed = waveSpeed,
                )
    )
}

// TODO Update the docs images to point to the expressive (wavy) versions of the progress indicators
/**
 * [Material Design determinate circular progress
 * indicator](https://m3.material.io/components/progress-indicators/overview)
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Circular wavy progress indicator
 * image](https://developer.android.com/images/reference/androidx/compose/material3/circular-wavy-progress-indicator.png)
 *
 * By default there is no animation between [progress] values. You can use
 * [ProgressIndicatorDefaults.ProgressAnimationSpec] as the default recommended [AnimationSpec] when
 * animating progress, such as in the following example:
 *
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color the progress indicator color
 * @param trackColor the indicator's track color, visible when the progress has not reached the area
 *   of the overall indicator yet
 * @param stroke a [Stroke] that will be used to draw this indicator
 * @param trackStroke a [Stroke] that will be used to draw the indicator's track
 * @param gapSize the gap between the track and the progress parts of the indicator
 * @param amplitude a lambda that provides an amplitude for the wave path as a function of the
 *   indicator's progress. 0.0 represents no amplitude, and 1.0 represents a max amplitude. Values
 *   outside of this range are coerced into the range.
 * @param wavelength the length of a wave in this circular indicator. Note that the actual
 *   wavelength may be different to ensure a continuous wave shape.
 * @param waveSpeed the speed in which the wave will move when the [amplitude] is greater than zero.
 *   The value here represents a DP per seconds, and by default it's matched to the [wavelength] to
 *   render an animation that moves the wave by one wave length per second. Note that the actual
 *   speed may be slightly different, as the [wavelength] can be adjusted to ensure a continuous
 *   wave shape.
 * @sample androidx.compose.material3.samples.CircularWavyProgressIndicatorSample
 *
 * You may also follow the Material guidelines to create a thicker version of this indicator, like
 * in this example:
 *
 * @sample androidx.compose.material3.samples.CircularThickWavyProgressIndicatorSample
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun CircularWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    stroke: Stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
    trackStroke: Stroke = WavyProgressIndicatorDefaults.circularTrackStroke,
    gapSize: Dp = WavyProgressIndicatorDefaults.CircularIndicatorTrackGapSize,
    amplitude: (progress: Float) -> Float = WavyProgressIndicatorDefaults.indicatorAmplitude,
    wavelength: Dp = WavyProgressIndicatorDefaults.CircularWavelength,
    waveSpeed: Dp = wavelength, // Match to 1 wavelength per second
) {
    Spacer(
        modifier =
            modifier
                .size(WavyProgressIndicatorDefaults.CircularContainerSize)
                .circularWavyProgressIndicator(
                    progress = progress,
                    color = color,
                    trackColor = trackColor,
                    stroke = stroke,
                    trackStroke = trackStroke,
                    gapSize = gapSize,
                    amplitude = amplitude,
                    wavelength = wavelength,
                    waveSpeed = waveSpeed,
                )
                .semantics(mergeDescendants = true) {
                    val progressValue = progress()
                    // Check for NaN, as the ProgressBarRangeInfo will throw an exception.
                    val clampedProgress = progressValue.coerceIn(0f, 1f)
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(
                            current = if (clampedProgress.isNaN()) 0f else clampedProgress,
                            range = 0f..1f,
                        )
                }
    )
}

// TODO Update the docs images to point to the expressive (wavy) versions of the progress indicators
/**
 * [Material Design indeterminate circular progress
 * indicator](https://m3.material.io/components/progress-indicators/overview)
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * ![Indeterminate circular wavy progress indicator
 * image](https://developer.android.com/images/reference/androidx/compose/material3/indeterminate-circular-wavy-progress-indicator.png)
 *
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color the progress indicator color
 * @param trackColor the indicator's track color, visible when the progress has not reached the area
 *   of the overall indicator yet
 * @param stroke a [Stroke] that will be used to draw this indicator
 * @param trackStroke a [Stroke] that will be used to draw the indicator's track
 * @param gapSize the gap between the track and the progress parts of the indicator
 * @param amplitude the wave's amplitude. 0.0 represents no amplitude, and 1.0 represents an
 *   amplitude that will take the full height of the progress indicator. Values outside of this
 *   range are coerced into the range.
 * @param wavelength the length of a wave in this circular indicator. Note that the actual
 *   wavelength may be different to ensure a continuous wave shape.
 * @param waveSpeed the speed in which the wave will move when the [amplitude] is greater than zero.
 *   The value here represents a DP per seconds, and by default it's matched to the [wavelength] to
 *   render an animation that moves the wave by one wave length per second. Note that the actual
 *   speed may be slightly different, as the [wavelength] can be adjusted to ensure a continuous
 *   wave shape.
 * @sample androidx.compose.material3.samples.IndeterminateCircularWavyProgressIndicatorSample
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun CircularWavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    stroke: Stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
    trackStroke: Stroke = WavyProgressIndicatorDefaults.circularTrackStroke,
    gapSize: Dp = WavyProgressIndicatorDefaults.CircularIndicatorTrackGapSize,
    @FloatRange(from = 0.0, to = 1.0) amplitude: Float = 1f,
    wavelength: Dp = WavyProgressIndicatorDefaults.CircularWavelength,
    waveSpeed: Dp = wavelength, // Match to 1 wavelength per second
) {
    Box(modifier = modifier.size(WavyProgressIndicatorDefaults.CircularContainerSize)) {
        Spacer(
            Modifier.fillMaxSize()
                .circularWavyProgressIndicator(
                    color = color,
                    trackColor = trackColor,
                    stroke = stroke,
                    trackStroke = trackStroke,
                    gapSize = gapSize,
                    amplitude = amplitude,
                    wavelength = wavelength,
                    waveSpeed = waveSpeed,
                )
        )
        // To overcome b/347736702 we are separating the progressSemantics() call to an independent
        // spacer, and wrap the spacer with the indicator content and this spacer in a Box.
        Spacer(modifier = Modifier.fillMaxSize().progressSemantics())
    }
}

/** Contains the default values used for wavy progress indicators */
@ExperimentalMaterial3ExpressiveApi
object WavyProgressIndicatorDefaults {

    /**
     * A default [AnimationSpec] that should be used when animating between progress in a
     * determinate progress indicator.
     */
    val ProgressAnimationSpec: AnimationSpec<Float> =
        tween(
            durationMillis = MotionTokens.DurationLong2.toInt(),
            easing = MotionTokens.EasingLinearCubicBezier,
        )

    /** A default active indicator [Color]. */
    val indicatorColor: Color
        @Composable get() = ProgressIndicatorTokens.ActiveIndicatorColor.value

    /** A default track [Color]. */
    val trackColor: Color
        @Composable get() = ProgressIndicatorTokens.TrackColor.value

    /** A default linear progress indicator active indicator [Stroke]. */
    val linearIndicatorStroke: Stroke
        @Composable
        get() =
            Stroke(
                width =
                    with(LocalDensity.current) {
                        LinearProgressIndicatorTokens.ActiveThickness.toPx()
                    },
                cap = StrokeCap.Round,
            )

    /** A default circular progress indicator active indicator [Stroke]. */
    val circularIndicatorStroke: Stroke
        @Composable
        get() =
            Stroke(
                width =
                    with(LocalDensity.current) {
                        CircularProgressIndicatorTokens.ActiveThickness.toPx()
                    },
                cap = StrokeCap.Round,
            )

    /** A default linear progress indicator track [Stroke]. */
    val linearTrackStroke: Stroke
        @Composable
        get() =
            Stroke(
                width =
                    with(LocalDensity.current) {
                        LinearProgressIndicatorTokens.TrackThickness.toPx()
                    },
                cap = StrokeCap.Round,
            )

    /** A default circular progress indicator track [Stroke]. */
    val circularTrackStroke: Stroke
        @Composable
        get() =
            Stroke(
                width =
                    with(LocalDensity.current) {
                        CircularProgressIndicatorTokens.TrackThickness.toPx()
                    },
                cap = StrokeCap.Round,
            )

    /** A default wavelength of a determinate linear progress indicator when it's in a wavy form. */
    val LinearDeterminateWavelength: Dp = LinearProgressIndicatorTokens.ActiveWaveWavelength

    /** A default wavelength of a linear progress indicator when it's in a wavy form. */
    val LinearIndeterminateWavelength: Dp =
        LinearProgressIndicatorTokens.IndeterminateActiveWaveWavelength

    /** A default linear progress indicator container height. */
    val LinearContainerHeight: Dp = LinearProgressIndicatorTokens.WaveHeight

    /** A default linear progress indicator container width. */
    val LinearContainerWidth: Dp = 240.dp

    /** A default linear stop indicator size. */
    val LinearTrackStopIndicatorSize: Dp = LinearProgressIndicatorTokens.StopSize

    /** A default circular progress indicator container size. */
    val CircularContainerSize: Dp = CircularProgressIndicatorTokens.WaveSize

    /** A default wavelength of a circular progress indicator when it's in a wavy form. */
    val CircularWavelength: Dp = CircularProgressIndicatorTokens.ActiveWaveWavelength

    /**
     * A default gap size that appears in between the active indicator and the track at the linear
     * progress indicator.
     */
    val LinearIndicatorTrackGapSize: Dp = LinearProgressIndicatorTokens.TrackActiveSpace

    /**
     * A default gap size that appears in between the active indicator and the track at the circular
     * progress indicator.
     */
    val CircularIndicatorTrackGapSize: Dp = CircularProgressIndicatorTokens.TrackActiveSpace

    /** A function that returns a determinate indicator's amplitude for a given progress. */
    val indicatorAmplitude: (progress: Float) -> Float = { progress ->
        // Sets the amplitude to the max on 10%, and back to zero on 95% of the progress.
        if (progress <= 0.1f || progress >= 0.95f) {
            0f
        } else {
            1f
        }
    }
}

// Animation spec for increasing the amplitude drawing when its changing.
internal val IncreasingAmplitudeAnimationSpec: AnimationSpec<Float> =
    tween(
        durationMillis = MotionTokens.DurationLong2.toInt(),
        easing = MotionTokens.EasingStandardCubicBezier,
    )

// Animation spec for decreasing the amplitude drawing when its changing.
internal val DecreasingAmplitudeAnimationSpec: AnimationSpec<Float> =
    tween(
        durationMillis = MotionTokens.DurationLong2.toInt(),
        easing = MotionTokens.EasingEmphasizedAccelerateCubicBezier,
    )
