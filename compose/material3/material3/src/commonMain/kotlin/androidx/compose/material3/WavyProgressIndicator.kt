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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.internal.IncreaseVerticalSemanticsBounds
import androidx.compose.material3.internal.toPath
import androidx.compose.material3.tokens.CircularProgressIndicatorTokens
import androidx.compose.material3.tokens.LinearProgressIndicatorTokens
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.material3.tokens.ProgressIndicatorTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

// TODO Update the docs images to point to the expressive (wavy) versions of the progress indicators
/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Determinate Material Design wavy linear progress indicator</a>.
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
    waveSpeed: Dp = wavelength // Match to 1 wavelength per second
) {
    val coercedProgress = { progress().coerceIn(0f, 1f) }
    val lastOffsetValue = remember { mutableFloatStateOf(0f) }
    val offsetAnimatable =
        remember(waveSpeed, wavelength) { Animatable(lastOffsetValue.floatValue) }
    LaunchedEffect(waveSpeed, wavelength) {
        if (waveSpeed > 0.dp) {
            // Compute the duration as a Dp per second.
            val durationMillis = ((wavelength / waveSpeed) * 1000).toInt()
            if (durationMillis > 0) {
                // Update the bounds to start from the current value and to end at the value plus 1.
                // This will ensure that there are no jumps in the animation in case the wave's
                // speed or length are changing.
                offsetAnimatable.updateBounds(
                    lastOffsetValue.floatValue,
                    lastOffsetValue.floatValue + 1
                )
                offsetAnimatable.animateTo(
                    lastOffsetValue.floatValue + 1,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        )
                ) {
                    lastOffsetValue.floatValue = value % 1f
                }
            }
        }
    }

    var amplitudeAnimatable by remember {
        mutableStateOf<Animatable<Float, AnimationVector1D>?>(null)
    }
    val coroutineScope = rememberCoroutineScope()
    // Holds the start and end progress fractions.
    val progressFractions = floatArrayOf(0f, 0f)
    val progressDrawingCache = remember { LinearProgressDrawingCache() }
    Spacer(
        modifier
            .then(IncreaseVerticalSemanticsBounds)
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(coercedProgress(), 0f..1f)
            }
            .requiredSizeIn(minWidth = LinearContainerMinWidth)
            .size(
                width = WavyProgressIndicatorDefaults.LinearContainerWidth,
                height = WavyProgressIndicatorDefaults.LinearContainerHeight
            )
            .clipToBounds()
            .drawWithCache {
                val progressValue = coercedProgress()
                val trackGapSize = gapSize.toPx()

                // Animate changes in the amplitude. As this requires a progress value, we do it
                // inside the Spacer to avoid redundant recompositions.
                val amplitudeForProgress = amplitude(progressValue).coerceIn(0f, 1f)
                val animatedAmplitude =
                    amplitudeAnimatable
                        ?: Animatable(amplitudeForProgress, Float.VectorConverter).also {
                            amplitudeAnimatable = it
                        }

                if (animatedAmplitude.targetValue != amplitudeForProgress) {
                    coroutineScope.launch {
                        animatedAmplitude.animateTo(
                            targetValue = amplitudeForProgress,
                            animationSpec =
                                if (animatedAmplitude.targetValue < amplitudeForProgress) {
                                    IncreasingAmplitudeAnimationSpec
                                } else {
                                    DecreasingAmplitudeAnimationSpec
                                }
                        )
                    }
                }

                // Update the end fraction only (i.e. the head of the progress)
                progressFractions[1] = progressValue
                progressDrawingCache.updatePaths(
                    size = size,
                    wavelength = wavelength.toPx(),
                    progressFractions = progressFractions,
                    amplitude = animatedAmplitude.value,
                    waveOffset =
                        if (animatedAmplitude.value > 0f) lastOffsetValue.floatValue else 0f,
                    gapSize = trackGapSize,
                    stroke = stroke,
                    trackStroke = trackStroke
                )
                onDrawWithContent {
                    with(progressDrawingCache) {
                        rotate(if (layoutDirection == LayoutDirection.Ltr) 0f else 180f) {
                            // Draw the track
                            drawPath(
                                path = trackPathToDraw,
                                color = trackColor,
                                style = trackStroke
                            )

                            // Draw the progress
                            for (i in progressPathsToDraw!!.indices) {
                                drawPath(
                                    path = progressPathsToDraw!![i],
                                    color = color,
                                    style = stroke
                                )
                            }

                            // Draw the stop indicator for a11y
                            drawStopIndicator(
                                progressEnd = progressValue,
                                progressIndicatorSize = size,
                                maxStopIndicatorSize = stopSize,
                                horizontalInsets = currentStrokeCapWidth,
                                trackStroke = trackStroke,
                                color = color
                            )
                        }
                    }
                }
            }
    )
}

// TODO Update the docs images to point to the expressive (wavy) versions of the progress indicators
/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Indeterminate Material Design linear wavy progress indicator</a>.
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
    waveSpeed: Dp = wavelength // Match to 1 wavelength per second
) {
    // Progress offset animation for the waves that is determined by the wave speed and wavelength.
    val lastOffsetValue = remember { mutableFloatStateOf(0f) }
    val offsetAnimatable =
        remember(waveSpeed, wavelength) { Animatable(lastOffsetValue.floatValue) }
    LaunchedEffect(waveSpeed, wavelength) {
        if (waveSpeed > 0.dp) {
            // Compute the duration as a Dp per second.
            val durationMillis = ((wavelength / waveSpeed) * 1000).toInt()
            if (durationMillis > 0) {
                // Update the bounds to start from the current value and to end at the value plus 1.
                // This will ensure that there are no jumps in the animation in case the wave's
                // speed or length are changing.
                offsetAnimatable.updateBounds(
                    lastOffsetValue.floatValue,
                    lastOffsetValue.floatValue + 1
                )
                offsetAnimatable.animateTo(
                    lastOffsetValue.floatValue + 1,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        )
                ) {
                    lastOffsetValue.floatValue = value % 1f
                }
            }
        }
    }

    // Head and tail animation for the two progress lines we will be displaying.
    val infiniteTransition = rememberInfiniteTransition()
    val firstLineHead =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateFirstLineHeadAnimationSpec
        )
    val firstLineTail =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateFirstLineTailAnimationSpec
        )
    val secondLineHead =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateSecondLineHeadAnimationSpec
        )
    val secondLineTail =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = linearIndeterminateSecondLineTailAnimationSpec
        )

    // Holds the start and end progress fractions (each two consecutive numbers in the array hold
    // the start and the end fractions for a single path)
    // In this case of an indeterminate progress indicator, we have 2 progress paths that can
    // appear at the same time while the indicator animates.
    val progressFractions = floatArrayOf(0f, 0f, 0f, 0f)
    val progressDrawingCache = remember { LinearProgressDrawingCache() }
    val coercedAmplitude = amplitude.coerceIn(0f, 1f)
    Spacer(
        modifier
            .then(IncreaseVerticalSemanticsBounds)
            .progressSemantics()
            .requiredSizeIn(minWidth = LinearContainerMinWidth)
            .size(
                WavyProgressIndicatorDefaults.LinearContainerWidth,
                WavyProgressIndicatorDefaults.LinearContainerHeight
            )
            .clipToBounds()
            .drawWithCache {
                progressFractions[0] = firstLineTail.value
                progressFractions[1] = firstLineHead.value
                progressFractions[2] = secondLineTail.value
                progressFractions[3] = secondLineHead.value

                // Update the paths.
                progressDrawingCache.updatePaths(
                    size = size,
                    wavelength = wavelength.toPx(),
                    progressFractions = progressFractions,
                    amplitude = coercedAmplitude,
                    waveOffset = if (coercedAmplitude > 0f) lastOffsetValue.floatValue else 0f,
                    gapSize = max(0f, gapSize.toPx()),
                    stroke = stroke,
                    trackStroke = trackStroke
                )
                onDrawWithContent {
                    with(progressDrawingCache) {
                        rotate(if (layoutDirection == LayoutDirection.Ltr) 0f else 180f) {
                            // Draw the track
                            drawPath(trackPathToDraw, color = trackColor, style = trackStroke)

                            // Draw the progress
                            for (i in progressPathsToDraw!!.indices) {
                                drawPath(progressPathsToDraw!![i], color = color, style = stroke)
                            }
                        }
                    }
                }
            }
    )
}

// TODO Update the docs images to point to the expressive (wavy) versions of the progress indicators
/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Determinate Material Design circular progress indicator</a>.
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
    waveSpeed: Dp = wavelength // Match to 1 wavelength per second
) {
    val circularShapes = remember { CircularShapes() }
    val lastOffsetValue = remember { mutableFloatStateOf(0f) }
    val offsetAnimatable =
        remember(waveSpeed, wavelength) { Animatable(lastOffsetValue.floatValue) }

    with(circularShapes) {
        // Have the LaunchedEffect execute whenever a change in the currentVertexCount state
        // happens.
        if (currentVertexCount.intValue >= MinCircularVertexCount) {
            LaunchedEffect(waveSpeed) {
                if (waveSpeed > 0.dp) {
                    // Computes the duration as a Dp per second, and take into account the vertex
                    // count. We use the currentVertexCount to compute the duration for the wave's
                    // motion to be as close as possible to the requested speed.
                    val durationMillis =
                        ((wavelength / waveSpeed) * 1000 * currentVertexCount.intValue).toInt()
                    if (durationMillis > 0) {
                        // Update the bounds to start from the current value and to end at the value
                        // plus 1. This will ensure that there are no jumps in the animation in case
                        // the wave's speed or length are changing.
                        offsetAnimatable.updateBounds(
                            lowerBound = lastOffsetValue.floatValue,
                            upperBound = lastOffsetValue.floatValue + 1
                        )
                        offsetAnimatable.animateTo(
                            lastOffsetValue.floatValue + 1,
                            animationSpec =
                                infiniteRepeatable(
                                    animation = tween(durationMillis, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart,
                                )
                        ) {
                            lastOffsetValue.floatValue = value % 1f
                        }
                    }
                }
            }
        }
    }

    PathProgressIndicator(
        progress = progress,
        // Resolves the Path from the morph by using the amplitude value as a Morph's progress.
        // Ensure that the Path is created with `repeatPath = supportMotion` to allow us to offset
        // the progress later to simulate motion, if enabled.
        progressPath = {
            progressAmplitude,
            progressWavelength,
            strokeWidth,
            size,
            supportMotion,
            path ->
            circularShapes.update(size, progressWavelength, strokeWidth)
            circularShapes.activeIndicatorMorph!!.toPath(
                progress = progressAmplitude,
                path = path,
                repeatPath = supportMotion,
                // The RoundedPolygon used in the Morph were normalized (i.e. moves to (0.5, 0.5)).
                rotationPivotX = 0.5f,
                rotationPivotY = 0.5f,
            )
        },
        // Resolves the Path from a RoundedPolygon that represents the track.
        trackPath = { _, progressWavelength, strokeWidth, size, path ->
            circularShapes.update(size, progressWavelength, strokeWidth)
            circularShapes.trackPolygon!!.toPath(path = path)
        },
        modifier = modifier.size(WavyProgressIndicatorDefaults.CircularContainerSize),
        color = color,
        trackColor = trackColor,
        stroke = stroke,
        trackStroke = trackStroke,
        gapSize = gapSize,
        amplitude = amplitude,
        waveOffset = { lastOffsetValue.floatValue },
        wavelength = wavelength,
        enableProgressMotion = true
    )
}

// TODO Update the docs images to point to the expressive (wavy) versions of the progress indicators
/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Indeterminate Material Design circular progress indicator</a>.
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
    waveSpeed: Dp = wavelength // Match to 1 wavelength per second
) {
    val circularShapes = remember { CircularShapes() }
    val lastOffsetValue = remember { mutableFloatStateOf(0f) }
    val offsetAnimatable =
        remember(waveSpeed, wavelength) { Animatable(lastOffsetValue.floatValue) }

    with(circularShapes) {
        // Have the LaunchedEffect execute whenever a change in the currentVertexCount state
        // happens.
        if (currentVertexCount.intValue >= MinCircularVertexCount) {
            LaunchedEffect(waveSpeed) {
                if (waveSpeed > 0.dp) {
                    // Computes the duration as a Dp per second, and take into account the vertex
                    // count. We use the currentVertexCount to compute the duration for the wave's
                    // motion to be as close as possible to the requested speed.
                    val durationMillis =
                        ((wavelength / waveSpeed) * 1000 * currentVertexCount.intValue).toInt()
                    if (durationMillis > 0) {
                        // Update the bounds to start from the current value and to end at the value
                        // plus 1. This will ensure that there are no jumps in the animation in case
                        // the wave's speed or length are changing.
                        offsetAnimatable.updateBounds(
                            lowerBound = lastOffsetValue.floatValue,
                            upperBound = lastOffsetValue.floatValue + 1
                        )
                        offsetAnimatable.animateTo(
                            lastOffsetValue.floatValue + 1,
                            animationSpec =
                                infiniteRepeatable(
                                    animation = tween(durationMillis, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart,
                                )
                        ) {
                            lastOffsetValue.floatValue = value % 1f
                        }
                    }
                }
            }
        }
    }
    PathProgressIndicator(
        modifier = modifier.size(WavyProgressIndicatorDefaults.CircularContainerSize),
        // Resolves the Path from a RoundedPolygon that represents the active indicator.
        progressPath = { _, progressWavelength, strokeWidth, size, supportMotion, path ->
            circularShapes.update(size, progressWavelength, strokeWidth)
            circularShapes.activeIndicatorMorph!!.toPath(
                progress = amplitude,
                path = path,
                repeatPath = supportMotion,
                rotationPivotX = 0.5f,
                rotationPivotY = 0.5f,
            )
        },
        // Resolves the Path from a RoundedPolygon that represents the track.
        trackPath = { _, progressWavelength, strokeWidth, size, path ->
            circularShapes.update(size, progressWavelength, strokeWidth)
            circularShapes.trackPolygon!!.toPath(path = path)
        },
        color = color,
        trackColor = trackColor,
        stroke = stroke,
        trackStroke = trackStroke,
        amplitude = amplitude.coerceIn(0f, 1f),
        waveOffset = { lastOffsetValue.floatValue },
        wavelength = wavelength,
        gapSize = gapSize,
        progressStart = CircularIndeterminateMinProgress,
        progressEnd = CircularIndeterminateMaxProgress,
        enableProgressMotion = true
    )
}

/**
 * Determinate path-based progress indicator.
 *
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range.
 * @param progressPath a function that returns a progress [Path] for a given amplitude and other
 *   parameters that may affect the shape. The function is provided with a [Path] object to avoid
 *   having to create a new [Path] object, and also takes an argument that indicates whether of not
 *   this Path should support motion (see [enableProgressMotion] for more info).
 * @param trackPath a function that returns an optional trackPath [Path] for a given amplitude and
 *   other parameters that may affect the shape. The function is provided with a [Path] object to
 *   avoid having to create a new [Path] object.
 * @param color the progress indicator color
 * @param trackColor the indicator's track color, visible when the progress has not reached the area
 *   of the overall indicator yet
 * @param stroke a [Stroke] that will be used to draw this indicator's progress
 * @param trackStroke a [Stroke] that will be used to draw the indicator's track
 * @param gapSize the gap between the track and the progress parts of the indicator
 * @param amplitude a lambda that provides an amplitude for the wave path as a function of the
 *   indicator's progress. 0.0 represents no amplitude, and 1.0 represents a max amplitude. Values
 *   outside of this range are coerced into the range. This amplitude it passed into the
 *   [progressPath] to generate the Path that will be drawn as progress.
 * @param waveOffset a lambda that controls the offset of the drawn wave and can be used to apply
 *   motion to the wave. The expected value is between 0.0 to 1.0. Values outside of this range are
 *   coerced into the range. An offset will only be applied when [enableProgressMotion] is true.
 * @param wavelength the length of a wave in this circular indicator. Note that the actual
 *   wavelength may be different to ensure a continuous wave shape.
 * @param enableProgressMotion indicates if a progress motion should be enabled for the provided
 *   progress. When enabled, the calls to the [progressPath] will be made with a `supportMotion =
 *   true`, and the generated [Path] will need to be repeated to allow drawing it while shifting the
 *   start and stop points, as well as rotating it, in order to simulate a progress motion.
 */
@Composable
private fun PathProgressIndicator(
    modifier: Modifier,
    progress: () -> Float,
    progressPath:
        (
            amplitude: Float,
            wavelength: Float,
            strokeWidth: Float,
            size: Size,
            supportMotion: Boolean,
            path: Path
        ) -> Path,
    trackPath:
        (amplitude: Float, wavelength: Float, strokeWidth: Float, size: Size, path: Path) -> Path,
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    gapSize: Dp,
    amplitude: (progress: Float) -> Float,
    waveOffset: () -> Float,
    wavelength: Dp,
    enableProgressMotion: Boolean
) {
    val coercedProgress = { progress().coerceIn(0f, 1f) }
    var amplitudeAnimatable by remember {
        mutableStateOf<Animatable<Float, AnimationVector1D>?>(null)
    }
    val coroutineScope = rememberCoroutineScope()
    // Holds the start and end progress fractions.
    val progressDrawingCache = remember { CircularProgressDrawingCache() }
    Spacer(
        modifier
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(coercedProgress(), 0f..1f)
            }
            .drawWithCache {
                val progressValue = coercedProgress()
                val trackGapSize = gapSize.toPx()

                // Animate changes in the amplitude. As this requires a progress value, we do it
                // inside the Spacer to avoid redundant recompositions.
                val amplitudeForProgress = amplitude(progressValue)
                val animatedAmplitude =
                    amplitudeAnimatable
                        ?: Animatable(amplitudeForProgress, Float.VectorConverter).also {
                            amplitudeAnimatable = it
                        }

                if (animatedAmplitude.targetValue != amplitudeForProgress) {
                    coroutineScope.launch {
                        if (animatedAmplitude.isRunning) {
                            // In case the amplitude animation is running, update the upperBound to
                            // match the new amplitudeForProgress. This will help when that
                            // amplitudeForProgress is changing before the previous change animation
                            // is done.
                            animatedAmplitude.updateBounds(
                                lowerBound = 0f,
                                upperBound = amplitudeForProgress
                            )
                        }
                        animatedAmplitude.animateTo(
                            targetValue = amplitudeForProgress,
                            animationSpec =
                                if (animatedAmplitude.targetValue < amplitudeForProgress) {
                                    IncreasingAmplitudeAnimationSpec
                                } else {
                                    DecreasingAmplitudeAnimationSpec
                                }
                        )
                    }
                }
                progressDrawingCache.updatePaths(
                    size = size,
                    progressPath = progressPath,
                    trackPath = trackPath,
                    enableProgressMotion = enableProgressMotion,
                    startProgress = 0f,
                    endProgress = progressValue,
                    amplitude = animatedAmplitude.value,
                    waveOffset =
                        if (animatedAmplitude.value > 0f) {
                            waveOffset().coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                    wavelength = wavelength.toPx(),
                    gapSize = trackGapSize,
                    stroke = stroke,
                    trackStroke = trackStroke
                )
                onDrawWithContent {
                    with(progressDrawingCache) {
                        if (layoutDirection == LayoutDirection.Rtl) {
                            // Scaling on the X will flip the drawing for RTL
                            scale(scaleX = -1f, scaleY = 1f) {
                                drawCircularIndicator(
                                    color = color,
                                    trackColor = trackColor,
                                    stroke = stroke,
                                    trackStroke = trackStroke,
                                    drawingCache = this@with
                                )
                            }
                        } else {
                            drawCircularIndicator(
                                color = color,
                                trackColor = trackColor,
                                stroke = stroke,
                                trackStroke = trackStroke,
                                drawingCache = this
                            )
                        }
                    }
                }
            }
    )
}

/**
 * Indeterminate path-based progress indicator.
 *
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param progressPath a function that returns a progress [Path] for a given amplitude and other
 *   parameters that may affect the shape. The function is provided with a [Path] object to avoid
 *   having to create a new [Path] object.
 * @param trackPath a function that returns an optional trackPath [Path] for a given amplitude and
 *   other parameters that may affect the shape. The function is provided with a [Path] object to
 *   avoid having to create a new [Path] object.
 * @param color the progress indicator color
 * @param trackColor the indicator's track color, visible when the progress has not reached the area
 *   of the overall indicator yet
 * @param stroke a [Stroke] that will be used to draw this indicator's progress
 * @param trackStroke a [Stroke] that will be used to draw the indicator's track
 * @param gapSize the gap between the track and the progress parts of the indicator
 * @param amplitude the wave's amplitude. 0.0 represents no amplitude, and 1.0 represents an
 *   amplitude that will take the full height of the progress indicator. Values outside of this
 *   range are coerced into the range.
 * @param waveOffset a lambda that controls the offset of the drawn wave and can be used to apply
 *   motion to the wave. The expected value is between 0.0 to 1.0. Values outside of this range are
 *   coerced into the range. An offset will only be applied when [enableProgressMotion] is true.
 * @param wavelength the length of a wave in this circular indicator. Note that the actual
 *   wavelength may be different to ensure a continuous wave shape.
 * @param progressStart the progress value that this indeterminate indicator will start animating
 *   from towards the [progressEnd] value. This value is expected to be between 0.0 and 1.0, and
 *   smaller than [progressEnd].
 * @param progressEnd the progress value that this indeterminate indicator will progress towards
 *   when animating from the [progressStart]. This value is expected to be between 0.0 and 1.0, and
 *   greater than [progressStart].
 * @param enableProgressMotion indicates if a progress motion should be enabled for the provided
 *   progress. When enabled, the calls to the [progressPath] will be made with a `supportMotion =
 *   true`, and the generated [Path] will need to be repeated to allow drawing it while shifting the
 *   start and stop points, as well as rotating it, in order to simulate a progress motion.
 */
@Composable
private fun PathProgressIndicator(
    modifier: Modifier,
    progressPath:
        (
            amplitude: Float,
            wavelength: Float,
            strokeWidth: Float,
            size: Size,
            supportMotion: Boolean,
            path: Path
        ) -> Path,
    trackPath:
        (amplitude: Float, wavelength: Float, strokeWidth: Float, size: Size, path: Path) -> Path,
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    gapSize: Dp,
    @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
    waveOffset: () -> Float,
    wavelength: Dp,
    @FloatRange(from = 0.0, to = 1.0) progressStart: Float,
    @FloatRange(from = 0.0, to = 1.0) progressEnd: Float,
    enableProgressMotion: Boolean
) {
    require(progressEnd > progressStart) {
        "Expecting a progress end that is greater than the progress start"
    }
    val infiniteTransition = rememberInfiniteTransition()
    // A global rotation that does a 1080 degrees rotation in 6 seconds.
    val globalRotation =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = CircularGlobalRotationDegreesTarget,
            animationSpec = circularIndeterminateGlobalRotationAnimationSpec
        )

    // An additional rotation that moves by 90 degrees in 500ms and then rest for 1 second.
    val additionalRotation =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = CircularAdditionalRotationDegreesTarget,
            animationSpec = circularIndeterminateRotationAnimationSpec
        )

    // Indicator progress animation that will be changing the progress up and down as the indicator
    // rotates.
    val progressAnimation =
        infiniteTransition.animateFloat(
            initialValue = CircularIndeterminateMinProgress,
            targetValue = CircularIndeterminateMaxProgress,
            animationSpec = circularIndeterminateProgressAnimationSpec
        )

    // Holds the start and end progress fractions.
    val progressDrawingCache = remember { CircularProgressDrawingCache() }
    Box(modifier = modifier) {
        Spacer(
            // Apply the rotation from the animation.
            Modifier.fillMaxSize()
                // Adding 90 degrees to align the Wavy indicator motion to the NTC indicator motion.
                .graphicsLayer { rotationZ = globalRotation.value + additionalRotation.value + 90 }
                .drawWithCache {
                    val trackGapSize = gapSize.toPx()
                    with(progressDrawingCache) {
                        // Update the paths and set a fixed start and end progress values to create
                        // a gap between the start and end when we cut pieced from the PathMeasure
                        // to draw.
                        // Note that the path we construct here only take into account the progress
                        // Stroke when being calculated. The track's Stroke is applied when drawing
                        // only.
                        updatePaths(
                            size = size,
                            progressPath = progressPath,
                            trackPath = trackPath,
                            enableProgressMotion = enableProgressMotion,
                            startProgress = 0f,
                            endProgress = progressAnimation.value,
                            amplitude = amplitude,
                            waveOffset =
                                if (amplitude > 0f) {
                                    waveOffset().coerceIn(0f, 1f)
                                } else {
                                    0f
                                },
                            wavelength = wavelength.toPx(),
                            gapSize = trackGapSize,
                            stroke = stroke,
                            trackStroke = trackStroke
                        )
                    }
                    onDrawWithContent {
                        drawCircularIndicator(
                            color = color,
                            trackColor = trackColor,
                            stroke = stroke,
                            trackStroke = trackStroke,
                            drawingCache = progressDrawingCache
                        )
                    }
                }
        )
        // To overcome b/347736702 we are separating the progressSemantics() call to an independent
        // spacer, and wrap the spacer with the indicator content and this spacer in a Box.
        Spacer(modifier = Modifier.fillMaxSize().progressSemantics())
    }
}

/** Draws the track and the progress of a circular progress indicator. */
private fun DrawScope.drawCircularIndicator(
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    drawingCache: CircularProgressDrawingCache,
) {
    // Draw the track
    if (trackColor != Color.Transparent && trackColor != Color.Unspecified) {
        drawPath(path = drawingCache.trackPathToDraw, color = trackColor, style = trackStroke)
    }

    // Draw the progress
    if (color != Color.Transparent && color != Color.Unspecified) {
        drawPath(path = drawingCache.progressPathToDraw, color = color, style = stroke)
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
            easing = MotionTokens.EasingLinearCubicBezier
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
                cap = StrokeCap.Round
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
                cap = StrokeCap.Round
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
                cap = StrokeCap.Round
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
                cap = StrokeCap.Round
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

/**
 * A class that returns and caches the [RoundedPolygon]s and the [Morph] that are displayed by
 * circular progress indicators.
 */
private class CircularShapes {
    private var currentSize: Size? = null
    private var currentWavelength: Float = -1f

    /**
     * A normalized [RoundedPolygon] for the circular indicator track.
     *
     * This property is guaranteed not to be null after a successful call to [update].
     *
     * @see RoundedPolygon.normalized
     */
    var trackPolygon: RoundedPolygon? = null

    /**
     * A normalized [RoundedPolygon] for the circular indicator progress.
     *
     * This property is guaranteed not to be null after a successful call to [update].
     *
     * @see RoundedPolygon.normalized
     * @see update
     */
    var activeIndicatorPolygon: RoundedPolygon? = null

    /**
     * A [Morph] for the circular indicator progress track. This morph will transition from a circle
     * to a wavy star shape.
     *
     * This property is guaranteed not to be null after a successful call to [update].
     *
     * Note that the [RoundedPolygon]s that this [Morph] is constructed with are normalized, which
     * allows us to process the generated paths more efficiently by just scaling them to the right
     * size.
     */
    var activeIndicatorMorph: Morph? = null

    /** Holds the current vertex count as a state. */
    val currentVertexCount = mutableIntStateOf(-1)

    /**
     * Updates the shapes according to the size of the circular loader and its wave's wavelength and
     * strokeWidth.
     *
     * @param size the dimensions of the current drawing environment that the shape are updated for
     * @param wavelength the length of a wave in the rendered circular shape in pixels. This actual
     *   length may end up differently to ensure a continuous wave shape.
     * @param strokeWidth the stroke's width in pixels
     */
    fun update(
        size: Size,
        @FloatRange(from = 0.0, fromInclusive = false) wavelength: Float,
        @FloatRange(from = 0.0, fromInclusive = false) strokeWidth: Float
    ) {
        require(wavelength > 0f) { "Wavelength should be greater than zero" }
        require(size.minDimension > 0f) { "Size min dimension should be greater than zero" }
        if (size == currentSize && wavelength == currentWavelength) return

        // Compute the number of edges as a factor of the circle size that the morph will be
        // rendered in and its proposed wavelength (2πr / wavelength), where the radius takes into
        // account the stroke's width.
        val r = size.minDimension / 2 - strokeWidth / 2
        val numVertices = max(MinCircularVertexCount, (2 * PI * r / wavelength).toInt())

        // Note that we match the vertices number at the track's polygon. This will result in a
        // smoother morphing between the active indicator and the track.
        trackPolygon = RoundedPolygon.circle(numVertices = numVertices).normalized()
        activeIndicatorPolygon =
            RoundedPolygon.star(
                    numVerticesPerRadius = numVertices,
                    innerRadius = 0.75f,
                    rounding = CornerRounding(radius = 0.35f, smoothing = 0.4f),
                    innerRounding = CornerRounding(radius = 0.5f)
                )
                .normalized()
        activeIndicatorMorph = Morph(start = trackPolygon!!, end = activeIndicatorPolygon!!)

        currentSize = size
        currentWavelength = wavelength
        currentVertexCount.intValue = numVertices
    }
}

/**
 * Draws an indicator at the end of the track. The size of the dot will remain constant until the
 * progress bar nears the end of the track and then it shrinks until its gone.
 */
private fun DrawScope.drawStopIndicator(
    progressEnd: Float,
    progressIndicatorSize: Size,
    maxStopIndicatorSize: Dp,
    horizontalInsets: Float,
    trackStroke: Stroke,
    color: Color
) {
    var stopIndicatorSize = min(trackStroke.width, maxStopIndicatorSize.toPx())
    // This will add an additional offset to the indicator's position in case the height of the
    // progress bar is greater than the height of the indicator.
    val indicatorXOffset =
        if (stopIndicatorSize == trackStroke.width) {
            0f
        } else {
            trackStroke.width / 4f
        }

    var indicatorX = progressIndicatorSize.width - stopIndicatorSize - indicatorXOffset
    val progressX = progressIndicatorSize.width * progressEnd + horizontalInsets
    if (indicatorX <= progressX) {
        stopIndicatorSize = max(0f, stopIndicatorSize - (progressX - indicatorX))
        indicatorX = progressX
    }

    if (stopIndicatorSize > 0) {
        if (trackStroke.cap == StrokeCap.Round) {
            drawCircle(
                color = color,
                radius = stopIndicatorSize / 2f,
                center =
                    Offset(
                        x = indicatorX + stopIndicatorSize / 2f,
                        y = progressIndicatorSize.height / 2f
                    )
            )
        } else {
            drawRect(
                color = color,
                topLeft =
                    Offset(
                        x = indicatorX,
                        y = (progressIndicatorSize.height - stopIndicatorSize) / 2f
                    ),
                size = Size(width = stopIndicatorSize, height = stopIndicatorSize)
            )
        }
    }
}

/**
 * A drawing cache of [Path]s and [PathMeasure] to be used when drawing linear progress indicators.
 */
private class LinearProgressDrawingCache {
    private var currentWavelength = -1f
    private var currentAmplitude = -1f
    private var currentSize: Size = Size.Unspecified
    private var currentProgressFractions: FloatArray? = null
    private var currentIndicatorTrackGapSize = 0f
    private var currentWaveOffset = -1f
    private var currentStroke = Stroke()
    private var currentTrackStroke = currentStroke

    // This scale value is used to grab segments from the pathMeasure in the correct length.
    // It holds a value that is the result of dividing the PathMeasure length by the actual Path
    // width (in pixels) that it's holding. When the amplitude is zero and the line is flat, the
    // scale would be 1.0. However, when the amplitude is greater than zero, the path is wavy and
    // its measured length in the PathMeasure would be longer than its measured width on screen, so
    // the scale would be greater than 1.0.
    private var progressPathScale = 1f

    /**
     * A [Path] that represents the progress indicator when it's in a complete state. A drawing path
     * can be computed from it and cached in this class with the use of [pathMeasure].
     */
    val fullProgressPath: Path = Path()

    /** A [PathMeasure] that will be used when computing a segment of a progress to be drawn */
    val pathMeasure: PathMeasure = PathMeasure()

    /** A [Path] that represents the track and will be used to draw it */
    val trackPathToDraw: Path = Path()

    /**
     * a [Path] that represents the current progress and will be used to draw it. This path is
     * derived from the [fullProgressPath] and should be computed and cached here using the
     * [pathMeasure].
     */
    var progressPathsToDraw: Array<Path>? = null

    /** The current stroke Cap width. */
    var currentStrokeCapWidth = 0f

    /**
     * Creates or updates the progress path, and caches it to avoid redundant updates before
     * updating the draw paths according to the progress.
     *
     * @param size the dimensions of the current drawing environment that this path is updated for
     * @param wavelength the length of a wave in pixels. This wavelength will only be applied when
     *   the [amplitude] is greater than zero in order to draw a wavy progress indicator.
     * @param progressFractions an array that holds the progress information for one or more
     *   progress segments that should be rendered on the indicator. Each value is the array
     *   represents a fractional progress location between 0.0 to 1.0, and a pair of values
     *   represent the start and end of a progress segment.
     * @param amplitude the amplitude of a wave in pixels. 0.0 represents no amplitude, and 1.0
     *   represents an amplitude that will take the full height of the progress indicator
     * @param waveOffset the offset that the progress bar will be created at
     * @param gapSize the gap size in pixels between the progress indicator and the track. Note that
     *   the gap will be taken out of the track path and not the progress path.
     * @param stroke the [Stroke] that will be used to plot progress's path
     * @param trackStroke the [Stroke] that will be used to plot the track's path
     */
    fun updatePaths(
        size: Size,
        @FloatRange(from = 0.0) wavelength: Float = 0f,
        progressFractions: FloatArray,
        @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
        @FloatRange(from = 0.0, to = 1.0) waveOffset: Float,
        @FloatRange(from = 0.0) gapSize: Float,
        stroke: Stroke,
        trackStroke: Stroke
    ) {
        if (currentProgressFractions == null) {
            // Just create FloatArray to match the size of the given progressFractions array.
            // We will later call copyInto to copy the incoming values into this cache.
            currentProgressFractions = FloatArray(progressFractions.size)
            progressPathsToDraw = Array(progressFractions.size / 2) { Path() }
        }
        val pathsUpdates =
            updateFullPaths(size, wavelength, amplitude, gapSize, stroke, trackStroke)
        updateDrawPaths(
            forceUpdate = pathsUpdates,
            progressFractions = progressFractions,
            amplitude = amplitude,
            waveOffset = waveOffset
        )
    }

    /**
     * Creates or updates the progress path, and caches it to avoid redundant updates. The created
     * path represents the progress indicator when it's in a complete state.
     *
     * Call this function before calling [updateDrawPaths], which will cut segments of the full path
     * for drawing using the internal [pathMeasure] that this function updates.
     *
     * @param size the dimensions of the current drawing environment that this path is updated for
     * @param wavelength the length of a wave in pixels. This wavelength will only be applied when
     *   the [amplitude] is greater than zero in order to draw a wavy progress indicator.
     * @param amplitude the amplitude of a wave in pixels. 0.0 represents no amplitude, and 1.0
     *   represents an amplitude that will take the full height of the progress indicator
     * @param gapSize the gap size in pixels between the progress indicator and the track. Note that
     *   the gap will be taken out of the track path and not the progress path.
     * @param stroke the [Stroke] that will be used to plot progress's path
     * @param trackStroke the [Stroke] that will be used to plot the track's path
     * @return true if the full paths were updated, or false otherwise
     * @see updateDrawPaths
     * @see updatePaths
     */
    private fun updateFullPaths(
        size: Size,
        @FloatRange(from = 0.0) wavelength: Float,
        @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
        @FloatRange(from = 0.0) gapSize: Float,
        stroke: Stroke,
        trackStroke: Stroke
    ): Boolean {
        if (
            currentSize == size &&
                currentWavelength == wavelength &&
                currentStroke == stroke &&
                currentTrackStroke == trackStroke &&
                currentIndicatorTrackGapSize == gapSize &&
                // Check if an amplitude change should trigger a full path update. If the amplitude
                // is turning to or from zero, this will trigger an update.
                ((currentAmplitude != 0f && amplitude != 0f) ||
                    currentAmplitude == 0f && amplitude == 0f)
        ) {
            // No update required
            return false
        }

        val height = size.height
        val width = size.width

        // Update the Stroke cap width to take into consideration when drawing the Path.
        currentStrokeCapWidth =
            if (
                (stroke.cap == StrokeCap.Butt && trackStroke.cap == StrokeCap.Butt) ||
                    height > width
            ) {
                0f
            } else {
                max(stroke.width / 2, trackStroke.width / 2)
            }

        // There are changes that should update the full path.
        fullProgressPath.rewind()
        fullProgressPath.moveTo(0f, 0f)

        if (amplitude == 0f) {
            // Just a line in this case, so we can optimize with a simple lineTo call.
            fullProgressPath.lineTo(width, 0f)
        } else {
            val halfWavelengthPx = wavelength / 2f
            var anchorX = halfWavelengthPx
            val anchorY = 0f
            var controlX = halfWavelengthPx / 2f

            // We set the amplitude to the max available height to create a sine-like path that will
            // later be Y-scaled on draw.
            // Note that with quadratic plotting, the height of the control point, when
            // perpendicular to the center point between the anchors, will plot a wave that peaks at
            // half the height.
            // We offset this height with the progress stroke's width to avoid cropping the drawing
            // later.
            var controlY = height - stroke.width

            // Plot a path that holds a couple of extra waves. This can later be used to create a
            // progressPathToDraw with a wave offset value to simulate a wave movement.
            // Note that we add more than one wave-length to support cases where the wavelength is
            // relatively large and may end up in cases where a single extra wavelength is not
            // sufficient for the wave's motion drawing.
            val widthWithExtraPhase = width + wavelength * 2
            var wavesCount = 0
            while (anchorX <= widthWithExtraPhase) {
                fullProgressPath.quadraticTo(controlX, controlY, anchorX, anchorY)
                anchorX += halfWavelengthPx
                controlX += halfWavelengthPx
                controlY *= -1f
                wavesCount++
            }
        }

        fullProgressPath.translate(Offset(x = 0f, y = height / 2f))

        // Update the PathMeasure with the full path
        pathMeasure.setPath(path = fullProgressPath, forceClosed = false)

        // Calculate the progressPathScale by dividing the length of the path that the PathMeasure
        // holds by its actual width in pixels. We will use this scale value later when grabbing
        // segments from the pathMeasure.
        val fullPathLength = pathMeasure.length
        progressPathScale = fullPathLength / (fullProgressPath.getBounds().width + 0.00000001f)

        // Cache the full path attributes (note that the amplitude is intentionally not cached here,
        // and will be cached on the updateDrawPaths call)
        currentSize = size
        currentWavelength = wavelength
        currentStroke = stroke
        currentTrackStroke = trackStroke
        currentIndicatorTrackGapSize = gapSize
        return true
    }

    /**
     * Updates and caches the draw paths by to the progress, amplitude, and wave offset.
     *
     * It's important to call this function only _after_ a call for [updateFullPaths] was made.
     *
     * @param forceUpdate for an update to the drawing paths. This flag will be set to true when the
     *   [updateFullPaths] returns true to indicate that the base paths were updated.
     * @param progressFractions an array that holds the progress information for one or more
     *   progress segments that should be rendered on the indicator. Each value is the array
     *   represents a fractional progress location between 0.0 to 1.0, and a pair of values
     *   represent the start and end of a progress segment.
     * @param amplitude the amplitude of a wave in pixels. 0.0 represents no amplitude, and 1.0
     *   represents an amplitude that will take the full height of the progress indicator.
     * @param waveOffset the offset that the progress bar will be created at
     * @see updateFullPaths
     * @see updatePaths
     */
    private fun updateDrawPaths(
        forceUpdate: Boolean,
        progressFractions: FloatArray,
        @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
        @FloatRange(from = 0.0, to = 1.0) waveOffset: Float
    ) {
        require(currentSize != Size.Unspecified) {
            "updateDrawPaths was called before updateFullPaths"
        }
        require(progressPathsToDraw!!.size == progressFractions.size / 2) {
            "the given progress fraction pairs do not match the expected number of progress paths" +
                " to draw. updateDrawPaths called with ${progressFractions.size / 2} pairs, while" +
                " there are ${progressPathsToDraw!!.size} expected progress paths."
        }
        if (
            !forceUpdate &&
                currentProgressFractions.contentEquals(progressFractions) &&
                currentAmplitude == amplitude &&
                currentWaveOffset == waveOffset
        ) {
            // No update required
            return
        }
        val width = currentSize.width
        val halfHeight = currentSize.height / 2f

        var adjustedTrackGapSize = currentIndicatorTrackGapSize

        // The path will only be visible if the Cap can be drawn, so this flag will indicate when
        // that happens to help us adjust the gap between the active indicator and the track.
        var activeIndicatorVisible = false

        // For each of the progress paths, apply a segment from the PathMeasure that was previously
        // created for the entire width of the progress bar. Also, draw the track parts in the gaps
        // between the progress parts.
        var nextEndTrackOffset = width - currentStrokeCapWidth
        trackPathToDraw.rewind()
        trackPathToDraw.moveTo(x = nextEndTrackOffset, y = halfHeight)
        for (i in progressPathsToDraw!!.indices) {
            progressPathsToDraw!![i].rewind()

            val startProgressFraction = progressFractions[i * 2]
            val endProgressFraction = progressFractions[i * 2 + 1]

            val barTail = startProgressFraction * width
            val barHead = endProgressFraction * width

            if (i == 0) {
                // Potentially shorten the gap and insets when the progress bar just enters the
                // track.
                // When rounded caps are applied, we need enough space to draw the initial path
                // (i.e. circle), so by only adjusting the gap size when the
                // barHead >= currentStrokeCapWidth we ensure that the track is not being shortened
                // in this initial progress phase.
                adjustedTrackGapSize =
                    if (barHead < currentStrokeCapWidth) {
                        0f // barHead
                    } else {
                        min(
                            barHead - currentStrokeCapWidth,
                            currentIndicatorTrackGapSize /*+ currentStrokeCapWidth * 2*/
                        )
                    }
                activeIndicatorVisible = barHead >= currentStrokeCapWidth
            }
            // Coerce the bar's head and tail to ensure we leave room for the drawing of the
            // stroke's caps.
            val coerceRange = currentStrokeCapWidth..(width - currentStrokeCapWidth)
            val adjustedBarHead = barHead.coerceIn(coerceRange)
            val adjustedBarTail = barTail.coerceIn(coerceRange)

            // Update the progressPathToDraw
            if (abs(endProgressFraction - startProgressFraction) > 0) {
                // For flat lines (i.e. amplitude == 0), there is no need to offset the wave.
                val waveShift =
                    if (amplitude != 0f) {
                        waveOffset * currentWavelength
                    } else {
                        0f
                    }
                pathMeasure.getSegment(
                    startDistance = (adjustedBarTail + waveShift) * progressPathScale,
                    stopDistance = (adjustedBarHead + waveShift) * progressPathScale,
                    destination = progressPathsToDraw!![i]
                )

                // Translate and scale the draw path by the wave shift and the amplitude.
                progressPathsToDraw!![i].transform(
                    Matrix().apply {
                        translate(
                            x = if (waveShift > 0f) -waveShift else 0f,
                            y = (1f - amplitude) * halfHeight
                        )
                        // The progressPathToDraw is a segment of the full progress path, which is
                        // always in the maximum possible amplitude. This scaling will flatten the
                        // wave to the given amplitude percentage.
                        if (amplitude != 1f) {
                            scale(y = amplitude)
                        }
                    }
                )
            }

            // While we draw the progress parts from left to right, we also draw the track parts
            // from right to left and update the nextEndTrackOffset on every pass.
            // Before that, we calculate the spacing between the active indicator and the track to
            // adjust it if needed when the progress is small.
            val adaptiveTrackSpacing =
                if (activeIndicatorVisible) {
                    adjustedTrackGapSize + currentStrokeCapWidth * 2
                } else {
                    adjustedTrackGapSize
                }
            if (nextEndTrackOffset > adjustedBarHead + adaptiveTrackSpacing) {
                trackPathToDraw.lineTo(
                    x = max(currentStrokeCapWidth, adjustedBarHead + adaptiveTrackSpacing),
                    y = halfHeight
                )
            }

            if (barHead > barTail) {
                // Update the nextEndTrackOffset and move the path to prepare for the next draw.
                nextEndTrackOffset =
                    max(currentStrokeCapWidth, adjustedBarTail - adaptiveTrackSpacing)
                trackPathToDraw.moveTo(x = nextEndTrackOffset, y = halfHeight)
            }
        }

        // Final track drawing, if needed. This will fill any track gaps to the left of the
        // progress.
        if (nextEndTrackOffset > currentStrokeCapWidth) {
            trackPathToDraw.lineTo(x = currentStrokeCapWidth, y = halfHeight)
        }

        // Cache
        progressFractions.copyInto(currentProgressFractions!!)
        currentAmplitude = amplitude
        currentWaveOffset = waveOffset
    }
}

/**
 * A drawing cache of [Path]s and [PathMeasure] to be used when drawing circular progress
 * indicators.
 */
private class CircularProgressDrawingCache {
    private var currentAmplitude = -1f
    private var currentWavelength = -1f
    private var currentSize: Size = Size.Unspecified

    /** Zero to one value that represents the progress start position. */
    private var currentStartProgress = 0f

    /** Zero to one value that represents the progress end position. */
    private var currentEndProgress = 0f
    private var currentIndicatorTrackGapSize = 0f
    private var currentWaveOffset = -1f
    private var currentStroke = Stroke()
    private var currentTrackStroke = currentStroke

    private var progressPathLength = 0f
    private var trackPathLength = 0f
    private var currentProgressMotionEnabled = false

    // Reusable Matrix for processing paths
    private val scaleMatrix = Matrix()

    /**
     * A [Path] that represents the progress indicator when it's in a complete state. A drawing path
     * can be computed from it and cached in this class with the use of [progressPathMeasure].
     */
    val fullProgressPath: Path = Path()

    /**
     * A [Path] that represents the progress indicator's track when it's in a complete state. A
     * drawing path can be computed from it and cached in this class with the use of
     * [trackPathMeasure].
     */
    val fullTrackPath: Path = Path()

    /** a [Path] that represents the current progress and will be used to draw it. */
    val progressPathToDraw: Path = Path()

    /** A [Path] that represents the track and will be used to draw it */
    val trackPathToDraw: Path = Path()

    /** A [PathMeasure] that will be used when computing a segment of a progress to be drawn */
    val progressPathMeasure: PathMeasure = PathMeasure()

    /** A [PathMeasure] that will be used when computing a segment of a track to be drawn */
    val trackPathMeasure: PathMeasure = PathMeasure()

    /** The current stroke Cap width. */
    var currentStrokeCapWidth = 0f

    /**
     * Creates or updates the progress and track paths, and caches them to avoid redundant updates
     * before updating the draw paths according to the progress.
     *
     * @param size the dimensions of the current drawing environment that this path is updated for
     * @param progressPath a function that returns a progress [Path] for a given amplitude and other
     *   parameters that may affect the shape. The function is provided with a [Path] object to
     *   avoid having to create a new [Path] object.
     * @param trackPath a function that returns an optional trackPath [Path] for a given amplitude
     *   and other parameters that may affect the shape. The function is provided with a [Path]
     *   object to avoid having to create a new [Path] object.
     * @param enableProgressMotion indicates if a progress motion should be enabled for the provided
     *   progress by offsetting the wave's drawing. When enabled, the calls to the [progressPath]
     *   will be made with a `supportMotion = true`, and the generated [Path] will be repeated to
     *   allow drawing it while shifting the start and stop points and rotating it to simulate a
     *   motion.
     * @param startProgress a fractional start progress value between 0.0 to 1.0
     * @param endProgress a fractional start progress value between 0.0 to 1.0
     * @param amplitude the amplitude of a wave in pixels. 0.0 represents no amplitude, and 1.0
     *   represents an amplitude that will take the full height of the progress indicator.
     * @param waveOffset the offset that the progress indicator will be created at. This value will
     *   only bee applied to the progress path in case [enableProgressMotion] is true.
     * @param wavelength the length of a wave in the rendered circular shape. This actual length may
     *   end up differently to ensure a continuous wave shape.
     * @param gapSize the gap size in pixels between the progress indicator and the track. Note that
     *   the gap will be taken out of the track path and not the progress path.
     * @param stroke the [Stroke] that will be used to plot the paths
     * @param trackStroke a [Stroke] that will be used to draw the indicator's track. By default,
     *   the track's stroke matches the provided [stroke], so use this parameter to specify a
     *   different stroke if required.
     */
    fun updatePaths(
        size: Size,
        progressPath:
            (
                amplitude: Float,
                wavelength: Float,
                strokeWidth: Float,
                size: Size,
                supportsMotion: Boolean,
                path: Path
            ) -> Path,
        trackPath:
            (
                amplitude: Float, wavelength: Float, strokeWidth: Float, size: Size, path: Path
            ) -> Path?,
        enableProgressMotion: Boolean,
        @FloatRange(from = 0.0, to = 1.0) startProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) endProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
        @FloatRange(from = 0.0, to = 1.0) waveOffset: Float,
        @FloatRange(from = 0.0, fromInclusive = false) wavelength: Float,
        @FloatRange(from = 0.0) gapSize: Float,
        stroke: Stroke,
        trackStroke: Stroke
    ) {
        val pathsUpdates =
            updateFullPaths(
                size = size,
                progressPath = progressPath,
                trackPath = trackPath,
                enableProgressMotion = enableProgressMotion,
                amplitude = amplitude,
                wavelength = wavelength,
                gapSize = gapSize,
                stroke = stroke,
                trackStroke = trackStroke
            )
        updateDrawPaths(
            forceUpdate = pathsUpdates,
            startProgress = startProgress,
            endProgress = endProgress,
            waveOffset = waveOffset
        )
    }

    /**
     * Updates the progress and track paths, and caches them to avoid redundant updates.
     *
     * Call this function before calling [updateDrawPaths], which will cut segments of the full
     * paths for drawing using the internal [progressPathMeasure] and [trackPathMeasure] that this
     * function updates.
     *
     * @param size the dimensions of the current drawing environment that this path is updated for
     * @param progressPath a function that returns a progress [Path] for a given amplitude and other
     *   parameters that may affect the shape. The function is provided with a [Path] object to
     *   avoid having to create a new [Path] object.
     * @param trackPath a function that returns an optional trackPath [Path] for a given amplitude
     *   and other parameters that may affect the shape. The function is provided with a [Path]
     *   object to avoid having to create a new [Path] object.
     * @param enableProgressMotion indicates if a progress motion should be enabled for the provided
     *   progress. When enabled, the calls to the [progressPath] will be made with a `supportMotion
     *   = true`, and the generated [Path] will be repeated to allow drawing it while shifting the
     *   start and stop points and rotating it to simulate a motion.
     * @param amplitude the amplitude of a wave in pixels. 0.0 represents no amplitude, and 1.0
     *   represents an amplitude that will take the full height of the progress indicator.
     * @param wavelength the length of a wave in the rendered circular shape. This actual length may
     *   end up differently to ensure a continuous wave shape.
     * @param gapSize the gap size in pixels between the progress indicator and the track. Note that
     *   the gap will be taken out of the track path and not the progress path.
     * @param stroke the [Stroke] that will be used to plot the paths
     * @param trackStroke a [Stroke] that will be used to draw the indicator's track. By default,
     *   the track's stroke matches the provided [stroke], so use this parameter to specify a
     *   different stroke if required.
     * @return true if the full paths were updated, or false otherwise
     * @see updateDrawPaths
     * @see updatePaths
     */
    private fun updateFullPaths(
        size: Size,
        progressPath:
            (
                amplitude: Float,
                wavelength: Float,
                strokeWidth: Float,
                size: Size,
                supportsMotion: Boolean,
                path: Path
            ) -> Path,
        trackPath:
            (
                amplitude: Float, wavelength: Float, strokeWidth: Float, size: Size, path: Path
            ) -> Path?,
        enableProgressMotion: Boolean,
        @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
        @FloatRange(from = 0.0, fromInclusive = false) wavelength: Float,
        @FloatRange(from = 0.0) gapSize: Float,
        stroke: Stroke,
        trackStroke: Stroke,
    ): Boolean {
        if (
            currentSize == size &&
                currentAmplitude == amplitude &&
                currentWavelength == wavelength &&
                currentStroke == stroke &&
                currentTrackStroke == trackStroke &&
                currentIndicatorTrackGapSize == gapSize &&
                currentProgressMotionEnabled == enableProgressMotion
        ) {
            // No update required
            return false
        }

        val height = size.height
        val width = size.width

        // Update the Stroke cap width to take into consideration when drawing the Path.
        currentStrokeCapWidth =
            if (
                (stroke.cap == StrokeCap.Butt && trackStroke.cap == StrokeCap.Butt) ||
                    height > width
            ) {
                0f
            } else {
                max(stroke.width / 2, trackStroke.width / 2)
            }

        scaleMatrix.reset()
        scaleMatrix.apply { scale(x = width - stroke.width, y = height - stroke.width) }

        fullProgressPath.rewind()

        // Note that we pass in the enableProgressMotion when generating the Path. This may
        // generate a path that is double in length to support offsetting the drawing, so we make
        // sure to adjust for it when storing the progressPathLength.
        progressPath(
            amplitude,
            wavelength,
            stroke.width,
            size,
            enableProgressMotion,
            fullProgressPath
        )
        processPath(fullProgressPath, size, scaleMatrix)
        progressPathMeasure.setPath(path = fullProgressPath, forceClosed = true)
        progressPathLength =
            if (enableProgressMotion) {
                progressPathMeasure.length / 2
            } else {
                progressPathMeasure.length
            }

        fullTrackPath.rewind()
        val trackPathForAmplitude =
            trackPath(amplitude, wavelength, stroke.width, size, fullTrackPath)
        if (trackPathForAmplitude != null) {
            processPath(fullTrackPath, size, scaleMatrix)
            trackPathMeasure.setPath(path = fullTrackPath, forceClosed = true)
            trackPathLength = trackPathMeasure.length
        } else {
            trackPathLength = 0f
        }

        // Cache the full path attributes
        currentSize = size
        currentAmplitude = amplitude
        currentWavelength = wavelength
        currentStroke = stroke
        currentTrackStroke = trackStroke
        currentIndicatorTrackGapSize = gapSize
        currentProgressMotionEnabled = enableProgressMotion

        return true
    }

    /** Process a given path by scaling it and then centering it inside a given size. */
    private fun processPath(path: Path, size: Size, scaleMatrix: Matrix) {
        path.transform(scaleMatrix)
        val progressPathBounds = path.getBounds()
        // Translate the path to align its center with the available size center.
        path.translate(size.center - progressPathBounds.center)
    }

    /**
     * Updates and caches the draw paths by to the progress and wave offset.
     *
     * It's important to call this function only _after_ a call for [updateFullPaths] was made.
     *
     * @param forceUpdate for an update to the drawing paths. This flag will be set to true when the
     *   [updateFullPaths] returns true to indicate that the base paths were updated.
     * @param startProgress a fractional start progress value between 0.0 to 1.0
     * @param endProgress a fractional start progress value between 0.0 to 1.0
     * @param waveOffset the offset that the progress indicator will be rendered at
     * @see updateFullPaths
     * @see updatePaths
     */
    private fun updateDrawPaths(
        forceUpdate: Boolean,
        @FloatRange(from = 0.0, to = 1.0) startProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) endProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) waveOffset: Float
    ) {
        require(currentSize != Size.Unspecified) {
            "updateDrawPaths was called before updateFullPaths"
        }
        if (
            !forceUpdate &&
                currentStartProgress == startProgress &&
                currentEndProgress == endProgress &&
                currentWaveOffset == waveOffset
        ) {
            // No update required
            return
        }

        trackPathToDraw.rewind()
        progressPathToDraw.rewind()

        val pStart = startProgress * progressPathLength
        val pStop = endProgress * progressPathLength

        val trackGapSize = min(pStop, currentIndicatorTrackGapSize)
        val horizontalInsets = min(pStop, currentStrokeCapWidth)
        val trackSpacing = horizontalInsets * 2 + trackGapSize

        // Handle offsetting the path when motion is enabled. We assume that the provided path when
        // motion is enabled was repeated to allow us to call getSegment with offsets and then
        // rotate the progressPathToDraw in order to create a shifted path as the progress moves.
        if (currentProgressMotionEnabled) {
            val coercedWaveOffset = waveOffset.coerceIn(0f, 1f)
            val startStopShift = coercedWaveOffset * progressPathLength

            progressPathMeasure.getSegment(
                startDistance = pStart + startStopShift,
                stopDistance = pStop + startStopShift,
                destination = progressPathToDraw
            )

            val offsetAngle = coercedWaveOffset * 360 % 360
            if (offsetAngle != 0f) {
                val fullProgressBounds = fullProgressPath.getBounds()
                // Rotate the progress path to adjust for the shift.
                progressPathToDraw.translate(
                    Offset(-fullProgressBounds.center.x, -fullProgressBounds.center.y)
                )
                progressPathToDraw.transform(Matrix().apply { rotateZ(degrees = -offsetAngle) })
                // Translate the path to align its center with the available size center.
                progressPathToDraw.translate(
                    Offset(fullProgressBounds.center.x, fullProgressBounds.center.y)
                )
            }
        } else {
            // No motion, so just grab the segment for the start and stop.
            progressPathMeasure.getSegment(
                startDistance = pStart,
                stopDistance = pStop,
                destination = progressPathToDraw
            )
        }
        if (trackPathLength > 0) {
            val tStart = endProgress * trackPathLength + trackSpacing
            val tStop = trackPathLength - trackSpacing
            trackPathMeasure.getSegment(
                startDistance = tStart,
                stopDistance = tStop,
                destination = trackPathToDraw
            )
        }

        // Cache
        currentStartProgress = startProgress
        currentEndProgress = endProgress
        currentWaveOffset = waveOffset
    }
}

// Set the linear indicator min width to the smallest circular indicator value at the tokens. Small
// linear indicators should be substituted with circular ones.
private val LinearContainerMinWidth = CircularProgressIndicatorTokens.Size

// Animation spec for increasing the amplitude drawing when its changing.
private val IncreasingAmplitudeAnimationSpec: AnimationSpec<Float> =
    tween(
        durationMillis = MotionTokens.DurationLong2.toInt(),
        easing = MotionTokens.EasingStandardCubicBezier
    )

// Animation spec for decreasing the amplitude drawing when its changing.
private val DecreasingAmplitudeAnimationSpec: AnimationSpec<Float> =
    tween(
        durationMillis = MotionTokens.DurationLong2.toInt(),
        easing = MotionTokens.EasingEmphasizedAccelerateCubicBezier
    )

private const val MinCircularVertexCount = 5
