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
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.material3.tokens.ProgressIndicatorTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
 * image](https://developer.android.com/images/reference/androidx/compose/material3/linear-progress-indicator.png)
 *
 * This version of a linear progress indicator accepts arguments, such as [amplitude], [wavelength],
 * and [waveSpeed] to render the progress as a waveform.
 *
 * By default there is no animation between [progress] values. You can use
 * [WavyProgressIndicatorDefaults.ProgressAnimationSpec] as the default recommended [AnimationSpec]
 * when animating progress, such as in the following example:
 *
 * @sample androidx.compose.material3.samples.LinearWavyProgressIndicatorSample
 *
 * You may also follow the Material guidelines to create a thicker version of this indicator, like
 * in this example:
 *
 * @sample androidx.compose.material3.samples.LinearThickWavyProgressIndicatorSample
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
 */
// TODO: Mark with expressive experimental annotation
@ExperimentalMaterial3Api
@Composable
fun LinearWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    stroke: Stroke = WavyProgressIndicatorDefaults.indicatorStroke,
    trackStroke: Stroke = WavyProgressIndicatorDefaults.trackStroke,
    gapSize: Dp = WavyProgressIndicatorDefaults.IndicatorTrackGapSize,
    stopSize: Dp = WavyProgressIndicatorDefaults.LinearTrackStopIndicatorSize,
    amplitude: (progress: Float) -> Float = WavyProgressIndicatorDefaults.indicatorAmplitude,
    wavelength: Dp = WavyProgressIndicatorDefaults.LinearWavelength,
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
            .then(IncreaseSemanticsBounds)
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(coercedProgress(), 0f..1f)
            }
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
 * ![Linear wavy progress indicator
 * image](https://developer.android.com/images/reference/androidx/compose/material3/linear-progress-indicator.png)
 *
 * @sample androidx.compose.material3.samples.IndeterminateLinearWavyProgressIndicatorSample
 *
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color the progress indicator color
 * @param trackColor the indicator's track color, visible when the progress has not reached the area
 *   of the overall indicator yet
 * @param stroke a [Stroke] that will be used to draw this indicator
 * @param trackStroke a [Stroke] that will be used to draw the indicator's track
 * @param gapSize the gap between the track and the progress parts of the indicator
 * @param wavelength the length of a wave
 */
// TODO: Mark with expressive experimental annotation
@ExperimentalMaterial3Api
@Composable
fun LinearWavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    stroke: Stroke = WavyProgressIndicatorDefaults.indicatorStroke,
    trackStroke: Stroke = stroke,
    gapSize: Dp = WavyProgressIndicatorDefaults.IndicatorTrackGapSize,
    wavelength: Dp = WavyProgressIndicatorDefaults.LinearWavelength
) {
    val infiniteTransition = rememberInfiniteTransition()
    val firstLineHead =
        infiniteTransition.animateFloat(
            0f,
            1f,
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = LinearAnimationDuration
                        0f at FirstLineHeadDelay using IndeterminateLinearProgressEasing
                        1f at FirstLineHeadDuration + FirstLineHeadDelay
                    }
            )
        )
    val firstLineTail =
        infiniteTransition.animateFloat(
            0f,
            1f,
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = LinearAnimationDuration
                        0f at FirstLineTailDelay using IndeterminateLinearProgressEasing
                        1f at FirstLineTailDuration + FirstLineTailDelay
                    }
            )
        )
    val secondLineHead =
        infiniteTransition.animateFloat(
            0f,
            1f,
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = LinearAnimationDuration
                        0f at SecondLineHeadDelay using IndeterminateLinearProgressEasing
                        1f at SecondLineHeadDuration + SecondLineHeadDelay
                    }
            )
        )
    val secondLineTail =
        infiniteTransition.animateFloat(
            0f,
            1f,
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = LinearAnimationDuration
                        0f at SecondLineTailDelay using IndeterminateLinearProgressEasing
                        1f at SecondLineTailDuration + SecondLineTailDelay
                    }
            )
        )

    val waveOffset =
        infiniteTransition.animateFloat(
            0f,
            1f,
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = LinearAnimationDuration
                        1f at LinearAnimationDuration using IndeterminateLinearProgressEasing
                    }
            )
        )

    // Holds the start and end progress fractions (each two consecutive numbers in the array hold
    // the start and the end fractions for a single path)
    // In this case of an indeterminate progress indicator, we have 2 progress paths that can
    // appear at the same time while the indicator animates.
    val progressFractions = floatArrayOf(0f, 0f, 0f, 0f)
    val progressDrawingCache = remember { LinearProgressDrawingCache() }
    Spacer(
        modifier
            .then(IncreaseSemanticsBounds)
            .progressSemantics()
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
                    amplitude = 1f,
                    waveOffset = (waveOffset.value * 3) % 1f,
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

/** Contains the default values used for wavy progress indicators */
// TODO: Mark with expressive experimental annotation
@ExperimentalMaterial3Api
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

    /** A default active indicator [Stroke]. */
    val indicatorStroke: Stroke
        @Composable
        get() =
            Stroke(
                width =
                    with(LocalDensity.current) {
                        4.dp.toPx() // TODO: Link to a token value for a thin PI
                    },
                cap = StrokeCap.Round
            )

    /** A default track [Stroke]. */
    val trackStroke: Stroke
        @Composable
        get() =
            Stroke(
                width =
                    with(LocalDensity.current) {
                        4.dp.toPx() // TODO: Link to a token value for a thin PI
                    },
                cap = StrokeCap.Round
            )

    /** A default wavelength of a linear progress indicator when it's in a wavy form. */
    val LinearWavelength: Dp = 40.dp // TODO: Link to a token value

    /** A default linear progress indicator container height. */
    val LinearContainerHeight: Dp = 10.dp // TODO: Link to a token value

    /** A default linear progress indicator container width. */
    val LinearContainerWidth: Dp = 240.dp // TODO: Link to a token value

    /** A default linear stop indicator size. */
    val LinearTrackStopIndicatorSize: Dp = 4.dp // TODO: Link to a token value

    /** A default gap size that appears in between the active indicator and the track. */
    val IndicatorTrackGapSize: Dp = 4.dp // TODO: Link to a token value

    /** A function that returns the indicator's amplitude for a given progress */
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
        var horizontalInsets = currentStrokeCapWidth

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
                // track
                adjustedTrackGapSize = min(barHead, currentIndicatorTrackGapSize)
                horizontalInsets = min(barHead, currentStrokeCapWidth)
            }
            // Coerce the bar's head and tail with the horizontalInsets (i.e leave room for the
            // drawing of the stroke caps). The adjustments here also ensure that the progress is
            // visible even when the progress is very small by leaving the tail's value unadjusted.
            val coerceRange = horizontalInsets..(width - currentStrokeCapWidth)
            val adjustedBarHead = barHead.coerceIn(coerceRange)
            val adjustedBarTail =
                if (barTail + horizontalInsets < adjustedBarHead) {
                    barTail.coerceIn(coerceRange)
                } else {
                    barTail
                }

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
                        // wave
                        // to the given amplitude percentage.
                        if (amplitude != 1f) {
                            scale(y = amplitude)
                        }
                    }
                )
            }

            // While we draw the progress parts from left to right, we also draw the track parts
            // from right to left and update the nextEndTrackOffset on every pass.
            val adaptiveTrackSpacing = horizontalInsets * 2 + adjustedTrackGapSize
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

// Total duration for one linear cycle
private const val LinearAnimationDuration = 1750

// Duration of the head and tail animations for both lines
private const val FirstLineHeadDuration = 1000
private const val FirstLineTailDuration = 1000
private const val SecondLineHeadDuration = 850
private const val SecondLineTailDuration = 850

// Delay before the start of the head and tail animations for both lines
private const val FirstLineHeadDelay = 0
private const val FirstLineTailDelay = 250
private const val SecondLineHeadDelay = 650
private const val SecondLineTailDelay = 900

private val IndeterminateLinearProgressEasing = MotionTokens.EasingEmphasizedAccelerateCubicBezier

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
