/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3.internal

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationScope
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.DecreasingAmplitudeAnimationSpec
import androidx.compose.material3.FirstLineTailDelay
import androidx.compose.material3.FirstLineTailDuration
import androidx.compose.material3.IncreasingAmplitudeAnimationSpec
import androidx.compose.material3.LinearAnimationDuration
import androidx.compose.material3.SecondLineHeadDelay
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.animateIndeterminateCycle
import androidx.compose.material3.linearIndeterminateFirstLineHeadAnimationSpec
import androidx.compose.material3.linearIndeterminateFirstLineTailAnimationSpec
import androidx.compose.material3.linearIndeterminateSecondLineHeadAnimationSpec
import androidx.compose.material3.linearIndeterminateSecondLineTailAnimationSpec
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
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
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A [Modifier] that draws a wavy linear progress indicator.
 *
 * @param progress a lambda that returns the progress of the indicator (e.g. 0 to 1), or a
 *   non-finite value for an indeterminate indicator.
 * @param color the color of the indicator.
 * @param trackColor the color of the track.
 * @param stroke the stroke of the indicator.
 * @param trackStroke the stroke of the track.
 * @param gapSize the gap size between the indicator and the track.
 * @param stopSize the size of the stop indicator.
 * @param amplitude a lambda that returns the amplitude of the indicator as a function of the
 *   progress. The value here represents a fraction of the height of the indicator (e.g. 0 to 1).
 * @param wavelength the wavelength of the determinate indicator.
 * @param waveSpeed the speed of the wave of the determinate indicator.
 * @param indeterminateWavelength the wavelength of the indeterminate indicator.
 * @param indeterminateWaveSpeed the speed of the wave of the indeterminate indicator.
 */
internal fun Modifier.linearWavyProgressIndicator(
    progress: () -> Float,
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    gapSize: Dp,
    stopSize: Dp,
    amplitude: (progress: Float) -> Float,
    wavelength: Dp,
    waveSpeed: Dp,
    indeterminateWavelength: Dp = WavyProgressIndicatorDefaults.LinearIndeterminateWavelength,
    indeterminateWaveSpeed: Dp = indeterminateWavelength,
): Modifier =
    this.then(
        LinearWavyProgressElement(
            progress = progress,
            amplitude = amplitude,
            color = color,
            trackColor = trackColor,
            stroke = stroke,
            trackStroke = trackStroke,
            gapSize = gapSize,
            stopSize = stopSize,
            wavelength = wavelength,
            waveSpeed = waveSpeed,
            indeterminateWavelength = indeterminateWavelength,
            indeterminateWaveSpeed = indeterminateWaveSpeed,
        )
    )

@Suppress("ModifierNodeInspectableProperties")
private class LinearWavyProgressElement(
    val progress: () -> Float,
    val amplitude: (progress: Float) -> Float,
    val color: Color,
    val trackColor: Color,
    val stroke: Stroke,
    val trackStroke: Stroke,
    val gapSize: Dp,
    val stopSize: Dp,
    val wavelength: Dp,
    val waveSpeed: Dp,
    val indeterminateWavelength: Dp,
    val indeterminateWaveSpeed: Dp,
) : ModifierNodeElement<LinearWavyProgressNode>() {

    override fun create(): LinearWavyProgressNode =
        LinearWavyProgressNode(
            progress = progress,
            amplitude = amplitude,
            colorParameter = color,
            trackColorParameter = trackColor,
            strokeParameter = stroke,
            trackStrokeParameter = trackStroke,
            gapSizeParameter = gapSize,
            stopSizeParameter = stopSize,
            wavelengthParameter = wavelength,
            waveSpeedParameter = waveSpeed,
            indeterminateWavelengthParameter = indeterminateWavelength,
            indeterminateWaveSpeedParameter = indeterminateWaveSpeed,
        )

    override fun update(node: LinearWavyProgressNode) {
        node.color = color
        node.trackColor = trackColor
        node.stroke = stroke
        node.trackStroke = trackStroke
        node.gapSize = gapSize
        node.stopSize = stopSize
        node.wavelength = wavelength
        node.waveSpeed = waveSpeed
        node.indeterminateWavelength = indeterminateWavelength
        node.indeterminateWaveSpeed = indeterminateWaveSpeed
        if (node.progress !== progress || node.amplitude !== amplitude) {
            node.progress = progress
            node.amplitude = amplitude
            node.invalidateDrawCache()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "linearWavyProgressIndicator"
        properties["color"] = color
        properties["trackColor"] = trackColor
        properties["stroke"] = stroke
        properties["trackStroke"] = trackStroke
        properties["gapSize"] = gapSize
        properties["stopSize"] = stopSize
        properties["wavelength"] = wavelength
        properties["waveSpeed"] = waveSpeed
        properties["indeterminateWavelength"] = indeterminateWavelength
        properties["indeterminateWaveSpeed"] = indeterminateWaveSpeed
        // Excluding the amplitude and progress lambdas
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearWavyProgressElement) return false

        if (progress !== other.progress) return false
        if (amplitude !== other.amplitude) return false
        if (color != other.color) return false
        if (trackColor != other.trackColor) return false
        if (stroke != other.stroke) return false
        if (trackStroke != other.trackStroke) return false
        if (gapSize != other.gapSize) return false
        if (stopSize != other.stopSize) return false
        if (wavelength != other.wavelength) return false
        if (waveSpeed != other.waveSpeed) return false
        if (indeterminateWavelength != other.indeterminateWavelength) return false
        if (indeterminateWaveSpeed != other.indeterminateWaveSpeed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = progress.hashCode()
        result = 31 * result + amplitude.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + trackColor.hashCode()
        result = 31 * result + stroke.hashCode()
        result = 31 * result + trackStroke.hashCode()
        result = 31 * result + gapSize.hashCode()
        result = 31 * result + stopSize.hashCode()
        result = 31 * result + wavelength.hashCode()
        result = 31 * result + waveSpeed.hashCode()
        result = 31 * result + indeterminateWavelength.hashCode()
        result = 31 * result + indeterminateWaveSpeed.hashCode()
        return result
    }
}

private class LinearWavyProgressNode(
    var progress: () -> Float,
    var amplitude: (progress: Float) -> Float,
    colorParameter: Color,
    trackColorParameter: Color,
    strokeParameter: Stroke,
    trackStrokeParameter: Stroke,
    gapSizeParameter: Dp,
    stopSizeParameter: Dp,
    wavelengthParameter: Dp,
    waveSpeedParameter: Dp,
    indeterminateWavelengthParameter: Dp,
    indeterminateWaveSpeedParameter: Dp,
) : DelegatingNode() {

    var color: Color = colorParameter
        set(value) {
            if (field != value) {
                field = value
                cacheDrawNode.invalidateDraw()
            }
        }

    var trackColor: Color = trackColorParameter
        set(value) {
            if (field != value) {
                field = value
                cacheDrawNode.invalidateDraw()
            }
        }

    var stroke: Stroke = strokeParameter
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawCache()
            }
        }

    var trackStroke: Stroke = trackStrokeParameter
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawCache()
            }
        }

    var gapSize: Dp = gapSizeParameter
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawCache()
            }
        }

    var stopSize: Dp = stopSizeParameter
        set(value) {
            if (field != value) {
                field = value
                cacheDrawNode.invalidateDraw()
            }
        }

    var wavelength: Dp = wavelengthParameter
        set(value) {
            if (field != value) {
                field = value
                if (!indeterminate) updateOffsetAnimation() // Restart animation
                invalidateDrawCache()
            }
        }

    var waveSpeed: Dp = waveSpeedParameter
        set(value) {
            if (field != value) {
                field = value
                if (!indeterminate) updateOffsetAnimation() // Restart animation
                invalidateDrawCache()
            }
        }

    var indeterminateWavelength: Dp = indeterminateWavelengthParameter
        set(value) {
            if (field != value) {
                field = value
                if (indeterminate) updateOffsetAnimation() // Restart animation
                invalidateDrawCache()
            }
        }

    var indeterminateWaveSpeed: Dp = indeterminateWaveSpeedParameter
        set(value) {
            if (field != value) {
                field = value
                if (indeterminate) updateOffsetAnimation() // Restart animation
                invalidateDrawCache()
            }
        }

    private var indeterminate = false
    private var indeterminateJob: Job? = null
    private var cyclePlayTimeMillis = 0f
    private var drawSecondLine = true
    private var handoffFraction = 1f

    private val waveOffset = mutableFloatStateOf(0f)
    private var offsetAnimationJob: Job? = null

    private var amplitudeAnimatable: Animatable<Float, AnimationVector1D>? = null
    private var amplitudeAnimationJob: Job? = null
    private var currentTargetAmplitude = -1f

    private val progressDrawingCache = LinearProgressDrawingCache()
    private val determinateFractions = FloatArray(2)
    private val indeterminateFractions = FloatArray(4)
    private val firstLineHead =
        linearIndeterminateFirstLineHeadAnimationSpec.vectorize(Float.VectorConverter)
    private val firstLineTail =
        linearIndeterminateFirstLineTailAnimationSpec.vectorize(Float.VectorConverter)
    private val secondLineHead =
        linearIndeterminateSecondLineHeadAnimationSpec.vectorize(Float.VectorConverter)
    private val secondLineTail =
        linearIndeterminateSecondLineTailAnimationSpec.vectorize(Float.VectorConverter)

    private val activeWavelength: Dp
        get() = if (indeterminate) indeterminateWavelength else wavelength

    private val activeWaveSpeed: Dp
        get() = if (indeterminate) indeterminateWaveSpeed else waveSpeed

    override fun onAttach() {
        if (!progress().isFinite()) {
            startIndeterminateAnimation()
        } else {
            // Potentially start the wave offset animation.
            updateOffsetAnimation()
        }
        // The draw cache outlives a detach, so rebuild it to restart what onDetach() reset.
        invalidateDrawCache()
    }

    override fun onDetach() {
        amplitudeAnimatable = null
        currentTargetAmplitude = -1f
        indeterminateJob = null
        indeterminate = false
        handoffFraction = 1f
    }

    fun invalidateDrawCache() {
        cacheDrawNode.invalidateDrawCache()
    }

    /** Updates the wave offset animation, potentially starting it if it is not already running. */
    private fun updateOffsetAnimation() {
        offsetAnimationJob?.cancel()
        offsetAnimationJob = null

        if (!isAttached) return

        val currentWavelength = activeWavelength
        val currentWaveSpeed = activeWaveSpeed
        if (currentWaveSpeed > 0.dp && currentWavelength > 0.dp) {
            val durationMillis =
                ((currentWavelength / currentWaveSpeed) * 1000)
                    .fastRoundToInt()
                    .coerceAtLeast(MinAnimationDuration)
            offsetAnimationJob = coroutineScope.launch {
                // Start from current offset
                val startValue = waveOffset.floatValue
                val offsetAnimatable = Animatable(startValue)
                val endValue = startValue + 1f
                offsetAnimatable.updateBounds(startValue, endValue)
                offsetAnimatable.animateTo(
                    targetValue = endValue,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                ) {
                    waveOffset.floatValue = value % 1f
                }
            }
        } else {
            // No speed or wavelength, so we ensure that the offset is snapped to 0
            waveOffset.floatValue = 0f
        }
    }

    /**
     * Updates an amplitude change animation, potentially starting it if it is not already running.
     */
    private fun updateAmplitudeAnimation(targetAmplitude: Float) {
        val currentAmplitudeAnimatable =
            amplitudeAnimatable
                ?: Animatable(initialValue = targetAmplitude).also {
                    amplitudeAnimatable = it
                    currentTargetAmplitude = targetAmplitude
                }

        if (isAttached && currentTargetAmplitude != targetAmplitude) {
            currentTargetAmplitude = targetAmplitude
            amplitudeAnimationJob?.cancel()
            amplitudeAnimationJob = coroutineScope.launch {
                currentAmplitudeAnimatable.animateTo(
                    targetValue = targetAmplitude,
                    animationSpec =
                        if (currentAmplitudeAnimatable.value < targetAmplitude) {
                            IncreasingAmplitudeAnimationSpec
                        } else {
                            DecreasingAmplitudeAnimationSpec
                        },
                )
            }
        }
    }

    private fun determinateProgress(): Float {
        val currentProgress = progress()
        return if (!currentProgress.isFinite()) 0f else currentProgress.fastCoerceIn(0f, 1f)
    }

    private fun targetAmplitude(): Float {
        val value = amplitude(if (indeterminate) Float.NaN else determinateProgress())
        return if (value.isNaN()) 1f else value.fastCoerceIn(0f, 1f)
    }

    private fun startIndeterminateAnimation() {
        indeterminate = true
        cyclePlayTimeMillis = 0f
        drawSecondLine = true
        handoffFraction = 1f
        updateOffsetAnimation()
        indeterminateJob?.cancel()
        indeterminateJob = coroutineScope.launch {
            val clock = AnimationState(0f)
            val onFrame: AnimationScope<Float, AnimationVector1D>.() -> Unit = {
                val playTimeMillis = value % LinearAnimationDuration
                if (playTimeMillis < cyclePlayTimeMillis) drawSecondLine = true
                cyclePlayTimeMillis = playTimeMillis
                cacheDrawNode.invalidateDraw()
            }
            do {
                clock.animateIndeterminateCycle(
                    cycleDurationMillis = LinearAnimationDuration,
                    isIndeterminate = { !progress().isFinite() },
                    onFrame = onFrame,
                )
                drawSecondLine = drawSecondLine && cyclePlayTimeMillis > SecondLineHeadDelay
                val exitDistanceMillis =
                    (if (drawSecondLine) LinearAnimationDuration else FirstLineExitMillis) -
                        cyclePlayTimeMillis
                if (exitDistanceMillis > 0f) {
                    clock.animateTo(
                        targetValue = clock.value + exitDistanceMillis,
                        animationSpec = handoffExitAnimationSpec(exitDistanceMillis),
                        sequentialAnimation = true,
                    ) {
                        onFrame()
                        if (!progress().isFinite()) cancelAnimation()
                    }
                }
            } while (!progress().isFinite())

            indeterminate = false
            cyclePlayTimeMillis = 0f
            handoffFraction = 0f
            val determinateAmplitude = targetAmplitude()
            currentTargetAmplitude = determinateAmplitude
            amplitudeAnimationJob?.cancel()
            amplitudeAnimatable?.snapTo(determinateAmplitude)
            updateOffsetAnimation()
            cacheDrawNode.invalidateDrawCache()
            Animatable(0f).animateTo(
                targetValue = 1f,
                animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
            ) {
                handoffFraction = value
                cacheDrawNode.invalidateDraw()
            }
        }
    }

    private fun progressFractions(): FloatArray =
        if (indeterminate) {
            indeterminateFractions[0] = firstLineTail.valueAt(cyclePlayTimeMillis)
            indeterminateFractions[1] = firstLineHead.valueAt(cyclePlayTimeMillis)
            indeterminateFractions[2] =
                if (drawSecondLine) secondLineTail.valueAt(cyclePlayTimeMillis) else 0f
            indeterminateFractions[3] =
                if (drawSecondLine) secondLineHead.valueAt(cyclePlayTimeMillis) else 0f
            indeterminateFractions
        } else {
            determinateFractions[1] = determinateProgress() * handoffFraction
            determinateFractions
        }

    private val cacheDrawNode: CacheDrawModifierNode =
        delegate(
            CacheDrawModifierNode {
                val currentProgress = progress()
                if (!currentProgress.isFinite() && !indeterminate) {
                    startIndeterminateAnimation()
                }

                updateAmplitudeAnimation(targetAmplitude())

                onDrawWithContent {
                    // Read from the states here and update the LinearProgressDrawingCache inside
                    // the onDrawWithContent to avoid voiding the cache at this
                    // CacheDrawModifierNode.
                    val currentAmplitude = amplitudeAnimatable?.value ?: 0f
                    val fractions = progressFractions()
                    progressDrawingCache.updatePaths(
                        size = size,
                        wavelength = activeWavelength.toPx(),
                        progressFractions = fractions,
                        amplitude = currentAmplitude,
                        waveOffset = if (currentAmplitude > 0f) waveOffset.floatValue else 0f,
                        gapSize = gapSize.toPx(),
                        stroke = stroke,
                        trackStroke = trackStroke,
                    )

                    // Draw
                    with(progressDrawingCache) {
                        rotate(if (layoutDirection == LayoutDirection.Ltr) 0f else 180f) {
                            // Draw track
                            drawPath(
                                path = trackPathToDraw,
                                color = trackColor,
                                style = trackStroke,
                            )

                            // Draw the progress
                            val progressPaths = progressPathsToDraw
                            if (progressPaths != null) {
                                for (i in progressPaths.indices) {
                                    drawPath(path = progressPaths[i], color = color, style = stroke)
                                }
                            }

                            if (!indeterminate) {
                                drawStopIndicator(
                                    progressEnd = fractions[1],
                                    progressIndicatorSize = size,
                                    maxStopIndicatorSize = stopSize * handoffFraction,
                                    horizontalInsets = currentStrokeCapWidth,
                                    trackStroke = trackStroke,
                                    color = color,
                                )
                            }
                        }
                    }
                }
            }
        )
}

private const val FirstLineExitMillis = FirstLineTailDelay + FirstLineTailDuration
private const val HandoffMaxPlaybackSpeed = 2.5f
private const val HandoffPlaybackRampMillis = 150f
private const val HandoffPlaybackAcceleration =
    (HandoffMaxPlaybackSpeed - 1f) / HandoffPlaybackRampMillis
private const val HandoffPlaybackRampDistanceMillis =
    (1f + HandoffMaxPlaybackSpeed) / 2f * HandoffPlaybackRampMillis

private val ZeroVector = AnimationVector1D(0f)
private val OneVector = AnimationVector1D(1f)

// Keyframes truncate the play time to whole milliseconds, so round away the float error first.
private fun VectorizedAnimationSpec<AnimationVector1D>.valueAt(playTimeMillis: Float): Float =
    getValueFromNanos(
            playTimeMillis.fastRoundToInt() * 1_000_000L,
            ZeroVector,
            OneVector,
            ZeroVector,
        )
        .value

/**
 * Plays [distanceMillis] of the indeterminate cycle at a speed that ramps up linearly from 1x to
 * [HandoffMaxPlaybackSpeed] over [HandoffPlaybackRampMillis].
 */
private fun handoffExitAnimationSpec(distanceMillis: Float): AnimationSpec<Float> {
    val durationMillis =
        if (distanceMillis <= HandoffPlaybackRampDistanceMillis) {
            (sqrt(1f + 2f * HandoffPlaybackAcceleration * distanceMillis) - 1f) /
                HandoffPlaybackAcceleration
        } else {
            HandoffPlaybackRampMillis +
                (distanceMillis - HandoffPlaybackRampDistanceMillis) / HandoffMaxPlaybackSpeed
        }
    return tween(
        durationMillis = durationMillis.fastRoundToInt().coerceAtLeast(1),
        easing = { fraction ->
            handoffExitDistanceMillis(fraction * durationMillis) / distanceMillis
        },
    )
}

/**
 * The distance in the indeterminate cycle that [handoffExitAnimationSpec] covers in [timeMillis].
 */
private fun handoffExitDistanceMillis(timeMillis: Float): Float =
    if (timeMillis <= HandoffPlaybackRampMillis) {
        timeMillis + HandoffPlaybackAcceleration / 2f * timeMillis * timeMillis
    } else {
        HandoffPlaybackRampDistanceMillis +
            HandoffMaxPlaybackSpeed * (timeMillis - HandoffPlaybackRampMillis)
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
    color: Color,
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
                        y = progressIndicatorSize.height / 2f,
                    ),
            )
        } else {
            drawRect(
                color = color,
                topLeft =
                    Offset(
                        x = indicatorX,
                        y = (progressIndicatorSize.height - stopIndicatorSize) / 2f,
                    ),
                size = Size(width = stopIndicatorSize, height = stopIndicatorSize),
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
        trackStroke: Stroke,
    ) {
        val sizeChanged = currentProgressFractions?.size != progressFractions.size
        if (sizeChanged) {
            // Create a FloatArray to match the size of the given progressFractions array.
            // We will later call copyInto to copy the incoming values into this cache.
            currentProgressFractions = FloatArray(progressFractions.size)
            progressPathsToDraw = Array(progressFractions.size / 2) { Path() }
        }
        val pathsUpdates =
            updateFullPaths(size, wavelength, amplitude, gapSize, stroke, trackStroke)
        updateDrawPaths(
            forceUpdate = pathsUpdates || sizeChanged,
            progressFractions = progressFractions,
            amplitude = amplitude,
            waveOffset = waveOffset,
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
        trackStroke: Stroke,
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
        @FloatRange(from = 0.0, to = 1.0) waveOffset: Float,
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
                            currentIndicatorTrackGapSize, /*+ currentStrokeCapWidth * 2*/
                        )
                    }
                activeIndicatorVisible = barHead >= currentStrokeCapWidth
            }
            // Coerce the bar's head and tail to ensure we leave room for the drawing of the
            // stroke's caps.
            val adjustedBarHead =
                barHead.fastCoerceIn(currentStrokeCapWidth, width - currentStrokeCapWidth)
            val adjustedBarTail =
                barTail.fastCoerceIn(currentStrokeCapWidth, width - currentStrokeCapWidth)

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
                    destination = progressPathsToDraw!![i],
                )

                // Translate and scale the draw path by the wave shift and the amplitude.
                progressPathsToDraw!![i].transform(
                    Matrix().apply {
                        translate(
                            x = if (waveShift > 0f) -waveShift else 0f,
                            y = (1f - amplitude) * halfHeight,
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
                    y = halfHeight,
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

// Minimum animation duration to ensure we don't overwhelm the CPU.
private const val MinAnimationDuration = 50
