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
import androidx.compose.animation.core.AnimationScope
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

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
 *   represents full progress. Values outside of this range are coerced into the range. Return a
 *   non-finite value (such as [Float.NaN] or [Float.POSITIVE_INFINITY]) while the progress is
 *   unknown to display an indeterminate indicator. When the progress becomes known, the
 *   indeterminate lines leave the track and the indicator then animates from no progress to the
 *   returned value.
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
 *   the range. The lambda receives [Float.NaN] while the indicator is indeterminate.
 * @param wavelength the length of a wave. Will be applied in case the path has an [amplitude] that
 *   is greater than zero and represents a wave. While the indicator is indeterminate, the
 *   [WavyProgressIndicatorDefaults.LinearIndeterminateWavelength] is used instead.
 * @param waveSpeed the speed in which the wave will move when the [amplitude] is greater than zero.
 *   The value here represents a DP per seconds, and by default it's matched to the [wavelength] to
 *   render an animation that moves the wave by one wave length per second. While the indicator is
 *   indeterminate, the wave moves by one indeterminate wave length per second.
 * @sample androidx.compose.material3.samples.LinearWavyProgressIndicatorSample
 *
 * You may also follow the Material guidelines to create a thicker version of this indicator, like
 * in this example:
 *
 * @sample androidx.compose.material3.samples.LinearThickWavyProgressIndicatorSample
 *
 * To display an indeterminate indicator until the progress is known, return [Float.NaN] from the
 * [progress] lambda until then:
 *
 * @sample androidx.compose.material3.samples.IndeterminateToDeterminateWavyProgressIndicatorSample
 *
 * @material3expressive
 */
@Composable
public fun LinearWavyProgressIndicator(
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
                    val currentProgress = progress()
                    progressBarRangeInfo =
                        if (!currentProgress.isFinite()) {
                            ProgressBarRangeInfo.Indeterminate
                        } else {
                            ProgressBarRangeInfo(currentProgress.fastCoerceIn(0f, 1f), 0f..1f)
                        }
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
 *
 * @material3expressive
 */
@Composable
public fun LinearWavyProgressIndicator(
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
                    progress = { Float.NaN },
                    color = color,
                    trackColor = trackColor,
                    stroke = stroke,
                    trackStroke = trackStroke,
                    gapSize = gapSize,
                    stopSize = 0.dp,
                    amplitude = { amplitude },
                    wavelength = wavelength,
                    waveSpeed = waveSpeed,
                    indeterminateWavelength = wavelength,
                    indeterminateWaveSpeed = waveSpeed,
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
 *   represents full progress. Values outside of this range are coerced into the range. Return a
 *   non-finite value (such as [Float.NaN] or [Float.POSITIVE_INFINITY]) while the progress is
 *   unknown to display an indeterminate indicator. When the progress becomes known, the indicator
 *   rotates to the start of the track while animating to the returned value.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color the progress indicator color
 * @param trackColor the indicator's track color, visible when the progress has not reached the area
 *   of the overall indicator yet
 * @param stroke a [Stroke] that will be used to draw this indicator
 * @param trackStroke a [Stroke] that will be used to draw the indicator's track
 * @param gapSize the gap between the track and the progress parts of the indicator
 * @param amplitude a lambda that provides an amplitude for the wave path as a function of the
 *   indicator's progress. 0.0 represents no amplitude, and 1.0 represents a max amplitude. Values
 *   outside of this range are coerced into the range. The lambda receives [Float.NaN] while the
 *   indicator is indeterminate.
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
 *
 * To display an indeterminate indicator until the progress is known, return [Float.NaN] from the
 * [progress] lambda until then:
 *
 * @sample androidx.compose.material3.samples.IndeterminateToDeterminateWavyProgressIndicatorSample
 *
 * @material3expressive
 */
@Composable
public fun CircularWavyProgressIndicator(
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
                    val currentProgress = progress()
                    progressBarRangeInfo =
                        if (!currentProgress.isFinite()) {
                            ProgressBarRangeInfo.Indeterminate
                        } else {
                            ProgressBarRangeInfo(currentProgress.fastCoerceIn(0f, 1f), 0f..1f)
                        }
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
 *
 * @material3expressive
 */
@Composable
public fun CircularWavyProgressIndicator(
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
    Box(
        // Due to issue where Talkback produces jarring noises on focus (b/347736702) we keep the
        // progressSemantics modifier in a separate wrapping box from the Spacer.
        modifier =
            modifier.size(WavyProgressIndicatorDefaults.CircularContainerSize).progressSemantics()
    ) {
        Spacer(
            Modifier.fillMaxSize()
                .circularWavyProgressIndicator(
                    progress = { Float.NaN },
                    color = color,
                    trackColor = trackColor,
                    stroke = stroke,
                    trackStroke = trackStroke,
                    gapSize = gapSize,
                    amplitude = { amplitude },
                    wavelength = wavelength,
                    waveSpeed = waveSpeed,
                )
        )
    }
}

/** Contains the default values used for wavy progress indicators */
public object WavyProgressIndicatorDefaults {

    /**
     * A default [AnimationSpec] that should be used when animating between progress in a
     * determinate progress indicator.
     */
    public val ProgressAnimationSpec: AnimationSpec<Float> =
        tween(
            durationMillis = MotionTokens.DurationLong2.toInt(),
            easing = MotionTokens.EasingLinearCubicBezier,
        )

    /** A default active indicator [Color]. */
    public val indicatorColor: Color
        @Composable get() = ProgressIndicatorTokens.ActiveIndicatorColor.value

    /** A default track [Color]. */
    public val trackColor: Color
        @Composable get() = ProgressIndicatorTokens.TrackColor.value

    /** A default linear progress indicator active indicator [Stroke]. */
    public val linearIndicatorStroke: Stroke
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
    public val circularIndicatorStroke: Stroke
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
    public val linearTrackStroke: Stroke
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
    public val circularTrackStroke: Stroke
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
    public val LinearDeterminateWavelength: Dp = LinearProgressIndicatorTokens.ActiveWaveWavelength

    /** A default wavelength of a linear progress indicator when it's in a wavy form. */
    public val LinearIndeterminateWavelength: Dp =
        LinearProgressIndicatorTokens.IndeterminateActiveWaveWavelength

    /** A default linear progress indicator container height. */
    public val LinearContainerHeight: Dp = LinearProgressIndicatorTokens.WaveHeight

    /** A default linear progress indicator container width. */
    public val LinearContainerWidth: Dp = 240.dp

    /** A default linear stop indicator size. */
    public val LinearTrackStopIndicatorSize: Dp = LinearProgressIndicatorTokens.StopSize

    /** A default circular progress indicator container size. */
    public val CircularContainerSize: Dp = CircularProgressIndicatorTokens.WaveSize

    /** A default wavelength of a circular progress indicator when it's in a wavy form. */
    public val CircularWavelength: Dp = CircularProgressIndicatorTokens.ActiveWaveWavelength

    /**
     * A default gap size that appears in between the active indicator and the track at the linear
     * progress indicator.
     */
    public val LinearIndicatorTrackGapSize: Dp = LinearProgressIndicatorTokens.TrackActiveSpace

    /**
     * A default gap size that appears in between the active indicator and the track at the circular
     * progress indicator.
     */
    public val CircularIndicatorTrackGapSize: Dp = CircularProgressIndicatorTokens.TrackActiveSpace

    /** A function that returns a determinate indicator's amplitude for a given progress. */
    public val indicatorAmplitude: (progress: Float) -> Float = { progress ->
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

/**
 * Advances this clock by one unit per millisecond, calling [onFrame] on every frame, until
 * [isIndeterminate] returns false. The clock follows [MotionDurationScale]. Its value modulo
 * [cycleDurationMillis] is the play time in the indeterminate cycle.
 */
internal suspend fun AnimationState<Float, AnimationVector1D>.animateIndeterminateCycle(
    cycleDurationMillis: Int,
    isIndeterminate: () -> Boolean,
    onFrame: AnimationScope<Float, AnimationVector1D>.() -> Unit,
) {
    var sequentialAnimation = true
    while (isIndeterminate()) {
        var stoppedByPolicy = false
        try {
            animateTo(
                targetValue = value + cycleDurationMillis,
                animationSpec =
                    infiniteRepeatable(tween(cycleDurationMillis, easing = LinearEasing)),
                sequentialAnimation = sequentialAnimation,
            ) {
                onFrame()
                if (!isIndeterminate()) cancelAnimation()
            }
        } catch (e: CancellationException) {
            // An InfiniteAnimationPolicy, such as the one in tests, stops infinite animations
            // without cancelling the coroutine.
            currentCoroutineContext().ensureActive()
            stoppedByPolicy = true
        }
        // An infinite animation also ends when animations are disabled.
        val motionDurationScale = currentCoroutineContext()[MotionDurationScale]
        snapshotFlow {
            !isIndeterminate() ||
                (!stoppedByPolicy && (motionDurationScale?.scaleFactor ?: 1f) > 0f)
        }
            .first { it }
        sequentialAnimation = false
    }
}
