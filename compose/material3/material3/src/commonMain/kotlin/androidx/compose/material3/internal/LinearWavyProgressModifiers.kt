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
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.DecreasingAmplitudeAnimationSpec
import androidx.compose.material3.IncreasingAmplitudeAnimationSpec
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A [Modifier] that draws a determinate wavy linear progress indicator.
 *
 * @param progress a lambda that returns the progress of the indicator (e.g. 0 to 1).
 * @param amplitude a lambda that returns the amplitude of the indicator as a function of the
 *   progress. The value here represents a fraction of the height of the indicator (e.g. 0 to 1).
 * @param color the color of the indicator.
 * @param trackColor the color of the track.
 * @param stroke the stroke of the indicator.
 * @param trackStroke the stroke of the track.
 * @param gapSize the gap size between the indicator and the track.
 * @param stopSize the size of the stop indicator.
 * @param wavelength the wavelength of the indicator.
 * @param waveSpeed the speed of the wave.
 */
internal fun Modifier.linearWavyProgressIndicator(
    progress: () -> Float,
    amplitude: (progress: Float) -> Float,
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    gapSize: Dp,
    stopSize: Dp,
    wavelength: Dp,
    waveSpeed: Dp,
): Modifier =
    this.then(
        DeterminateLinearWavyProgressElement(
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
        )
    )

/**
 * A [Modifier] that draws an indeterminate wavy linear progress indicator.
 *
 * @param firstLineHeadProgress the animated head progress (0..1) of the first line.
 * @param firstLineTailProgress the animated tail progress (0..1) of the first line.
 * @param secondLineHeadProgress the animated head progress (0..1) of the second line.
 * @param secondLineTailProgress the animated tail progress (0..1) of the second line.
 * @param color the color of the indicator.
 * @param trackColor the color of the track.
 * @param stroke the stroke of the indicator.
 * @param trackStroke the stroke of the track.
 * @param gapSize the gap size between the indicator and the track.
 * @param amplitude the fixed amplitude (0..1) relative to half height.
 * @param wavelength the wavelength of the indicator.
 * @param waveSpeed the speed of the wave.
 */
internal fun Modifier.linearWavyProgressIndicator(
    firstLineHeadProgress: () -> Float,
    firstLineTailProgress: () -> Float,
    secondLineHeadProgress: () -> Float,
    secondLineTailProgress: () -> Float,
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    gapSize: Dp,
    amplitude: Float,
    wavelength: Dp,
    waveSpeed: Dp,
): Modifier =
    this.then(
        IndeterminateLinearWavyProgressElement(
            firstLineHeadProgress = firstLineHeadProgress,
            firstLineTailProgress = firstLineTailProgress,
            secondLineHeadProgress = secondLineHeadProgress,
            secondLineTailProgress = secondLineTailProgress,
            color = color,
            trackColor = trackColor,
            stroke = stroke,
            trackStroke = trackStroke,
            gapSize = gapSize,
            wavelength = wavelength,
            waveSpeed = waveSpeed,
            amplitude = amplitude,
        )
    )

/** A base class for linear wavy progress [ModifierNodeElement]s. */
@Suppress("ModifierNodeInspectableProperties")
private abstract class BaseLinearWavyProgressElement<N : BaseLinearWavyProgressNode>(
    open val color: Color,
    open val trackColor: Color,
    open val stroke: Stroke,
    open val trackStroke: Stroke,
    open val gapSize: Dp,
    open val wavelength: Dp,
    open val waveSpeed: Dp,
) : ModifierNodeElement<N>() {

    override fun update(node: N) {
        node.color = color
        node.trackColor = trackColor
        node.stroke = stroke
        node.trackStroke = trackStroke
        node.gapSize = gapSize
        node.wavelength = wavelength
        node.waveSpeed = waveSpeed
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseLinearWavyProgressElement<*>) return false

        if (color != other.color) return false
        if (trackColor != other.trackColor) return false
        if (stroke != other.stroke) return false
        if (trackStroke != other.trackStroke) return false
        if (gapSize != other.gapSize) return false
        if (wavelength != other.wavelength) return false
        if (waveSpeed != other.waveSpeed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + trackColor.hashCode()
        result = 31 * result + stroke.hashCode()
        result = 31 * result + trackStroke.hashCode()
        result = 31 * result + gapSize.hashCode()
        result = 31 * result + wavelength.hashCode()
        result = 31 * result + waveSpeed.hashCode()
        return result
    }

    /**
     * Base implementation of [InspectorInfo.inspectableProperties] that can be called from the
     * implementation of [InspectorInfo.inspectableProperties] override.
     */
    protected fun InspectorInfo.baseInspectableProperties() {
        properties["color"] = color
        properties["trackColor"] = trackColor
        properties["stroke"] = stroke
        properties["trackStroke"] = trackStroke
        properties["gapSize"] = gapSize
        properties["wavelength"] = wavelength
        properties["waveSpeed"] = waveSpeed
    }
}

/** A base class for linear wavy progress [Modifier] nodes. */
private abstract class BaseLinearWavyProgressNode(
    colorParameter: Color,
    trackColorParameter: Color,
    strokeParameter: Stroke,
    trackStrokeParameter: Stroke,
    gapSizeParameter: Dp,
    wavelengthParameter: Dp,
    waveSpeedParameter: Dp,
) : DelegatingNode() {

    var color: Color = colorParameter
        set(value) {
            if (field != value) {
                field = value
                invalidateDraw()
            }
        }

    var trackColor: Color = trackColorParameter
        set(value) {
            if (field != value) {
                field = value
                invalidateDraw()
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

    var wavelength: Dp = wavelengthParameter
        set(value) {
            if (field != value) {
                field = value
                updateOffsetAnimation() // Restart animation
                invalidateDrawCache()
            }
        }

    var waveSpeed: Dp = waveSpeedParameter
        set(value) {
            if (field != value) {
                field = value
                updateOffsetAnimation() // Restart animation
                invalidateDrawCache()
            }
        }

    override fun onAttach() {
        // Potentially start the wave offset animation.
        updateOffsetAnimation()
    }

    override fun onDetach() {
        amplitudeAnimatable = null
    }

    protected val waveOffset = mutableFloatStateOf(0f)
    private var offsetAnimationJob: Job? = null

    protected var amplitudeAnimatable: Animatable<Float, AnimationVector1D>? = null
    protected var amplitudeAnimationJob: Job? = null

    protected val progressDrawingCache = LinearProgressDrawingCache()

    /** Updates the wave offset animation, potentially starting it if it is not already running. */
    protected fun updateOffsetAnimation() {
        offsetAnimationJob?.cancel()
        offsetAnimationJob = null

        if (!isAttached) return

        if (waveSpeed > 0.dp && wavelength > 0.dp) {
            val durationMillis =
                ((wavelength / waveSpeed) * 1000)
                    .fastRoundToInt()
                    .coerceAtLeast(MinAnimationDuration)
            offsetAnimationJob =
                coroutineScope.launch {
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
    protected fun updateAmplitudeAnimation(targetAmplitudePx: Float) {
        val currentAmplitudeAnimatable =
            amplitudeAnimatable
                ?: Animatable(initialValue = targetAmplitudePx).also { amplitudeAnimatable = it }

        // Check if the amplitude target has changed AND animation is not already running
        // towards it. If so, launch the amplitude animation within the node's scope
        if (
            isAttached &&
                currentAmplitudeAnimatable.targetValue != targetAmplitudePx &&
                (amplitudeAnimationJob == null || amplitudeAnimationJob?.isCompleted == true)
        ) {
            amplitudeAnimationJob =
                coroutineScope.launch {
                    currentAmplitudeAnimatable.animateTo(
                        targetValue = targetAmplitudePx,
                        animationSpec =
                            if (currentAmplitudeAnimatable.value < targetAmplitudePx) {
                                IncreasingAmplitudeAnimationSpec
                            } else {
                                DecreasingAmplitudeAnimationSpec
                            },
                    )
                }
        }
    }

    /** Provides the progress fractions (2 for determinate, 4 for indeterminate). */
    protected abstract fun getProgressFractions(): FloatArray

    /**
     * Invalidates this modifier's draw layer, ensuring that a draw pass will be run on the next
     * frame.
     */
    protected abstract fun invalidateDraw()

    /**
     * Invalidate the draw cache for changes in things like stroke width, amplitude, etc. This call
     * may also start or stop animations that update the wave drawings.
     */
    protected abstract fun invalidateDrawCache()
}

private class DeterminateLinearWavyProgressElement(
    val progress: () -> Float,
    val amplitude: (progress: Float) -> Float,
    override val color: Color,
    override val trackColor: Color,
    override val stroke: Stroke,
    override val trackStroke: Stroke,
    override val gapSize: Dp,
    val stopSize: Dp,
    override val wavelength: Dp,
    override val waveSpeed: Dp,
) :
    BaseLinearWavyProgressElement<DeterminateLinearWavyProgressNode>(
        color,
        trackColor,
        stroke,
        trackStroke,
        gapSize,
        wavelength,
        waveSpeed,
    ) {

    override fun create(): DeterminateLinearWavyProgressNode =
        DeterminateLinearWavyProgressNode(
            progress = progress,
            amplitude = amplitude,
            stopSizeParameter = stopSize,
            colorParameter = color,
            trackColorParameter = trackColor,
            strokeParameter = stroke,
            trackStrokeParameter = trackStroke,
            gapSizeParameter = gapSize,
            wavelengthParameter = wavelength,
            waveSpeedParameter = waveSpeed,
        )

    override fun update(node: DeterminateLinearWavyProgressNode) {
        super.update(node)
        node.stopSize = stopSize
        if (node.progress !== progress || node.amplitude !== amplitude) {
            node.progress = progress
            node.amplitude = amplitude
            node.cacheDrawNodeDelegate.invalidateDrawCache()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "determinateLinearWavyProgressIndicator"
        properties["stopSize"] = stopSize
        baseInspectableProperties()
        // Excluding the amplitude and progress lambdas
    }

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is DeterminateLinearWavyProgressElement) return false
        if (
            stopSize != other.stopSize ||
                progress !== other.progress ||
                amplitude !== other.amplitude
        )
            return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + stopSize.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + amplitude.hashCode()
        return result
    }
}

private class DeterminateLinearWavyProgressNode(
    var progress: () -> Float,
    var amplitude: (progress: Float) -> Float,
    stopSizeParameter: Dp,
    colorParameter: Color,
    trackColorParameter: Color,
    strokeParameter: Stroke,
    trackStrokeParameter: Stroke,
    gapSizeParameter: Dp,
    wavelengthParameter: Dp,
    waveSpeedParameter: Dp,
) :
    BaseLinearWavyProgressNode(
        colorParameter,
        trackColorParameter,
        strokeParameter,
        trackStrokeParameter,
        gapSizeParameter,
        wavelengthParameter,
        waveSpeedParameter,
    ) {

    var stopSize: Dp = stopSizeParameter
        set(value) {
            if (field != value) {
                field = value
                cacheDrawNodeDelegate.invalidateDraw()
            }
        }

    private val progressFractionsArray = floatArrayOf(0f, 0f)

    override fun getProgressFractions(): FloatArray {
        progressFractionsArray[0] = 0f // Start is always 0f for determinate
        progressFractionsArray[1] = progress().fastCoerceIn(0f, 1f)
        return progressFractionsArray
    }

    override fun invalidateDraw() {
        cacheDrawNodeDelegate.invalidateDraw()
    }

    override fun invalidateDrawCache() {
        cacheDrawNodeDelegate.invalidateDrawCache()
    }

    val cacheDrawNodeDelegate: CacheDrawModifierNode =
        delegate(
            CacheDrawModifierNode {
                val coercedProgress = progress().fastCoerceIn(0f, 1f)
                val targetAmplitudePx = amplitude(coercedProgress).fastCoerceIn(0f, 1f)

                // In case the amplitude was updated, update the animation that will transition it
                // smoothly.
                updateAmplitudeAnimation(targetAmplitudePx)

                onDrawWithContent {
                    // Read from the states here and update the LinearProgressDrawingCache inside
                    // the onDrawWithContent to avoid voiding the cache at this
                    // CacheDrawModifierNode.
                    val currentAmplitude = amplitudeAnimatable?.value ?: 0f
                    progressDrawingCache.updatePaths(
                        size = size,
                        wavelength = wavelength.toPx(),
                        progressFractions = getProgressFractions(),
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

                            // Draw a stop indicator
                            drawStopIndicator(
                                progressEnd = getProgressFractions()[1],
                                progressIndicatorSize = size,
                                maxStopIndicatorSize = stopSize,
                                horizontalInsets = currentStrokeCapWidth,
                                trackStroke = trackStroke,
                                color = color,
                            )
                        }
                    }
                }
            }
        )
}

private class IndeterminateLinearWavyProgressElement(
    val firstLineHeadProgress: () -> Float,
    val firstLineTailProgress: () -> Float,
    val secondLineHeadProgress: () -> Float,
    val secondLineTailProgress: () -> Float,
    override val color: Color,
    override val trackColor: Color,
    override val stroke: Stroke,
    override val trackStroke: Stroke,
    override val gapSize: Dp,
    override val wavelength: Dp,
    override val waveSpeed: Dp,
    val amplitude: Float,
) :
    BaseLinearWavyProgressElement<IndeterminateLinearWavyProgressNode>(
        color,
        trackColor,
        stroke,
        trackStroke,
        gapSize,
        wavelength,
        waveSpeed,
    ) {

    override fun create(): IndeterminateLinearWavyProgressNode =
        IndeterminateLinearWavyProgressNode(
            firstLineHeadProgress = firstLineHeadProgress,
            firstLineTailProgress = firstLineTailProgress,
            secondLineHeadProgress = secondLineHeadProgress,
            secondLineTailProgress = secondLineTailProgress,
            amplitudeParameter = amplitude,
            // Pass common parameters to base node constructor
            colorParameter = color,
            trackColorParameter = trackColor,
            strokeParameter = stroke,
            trackStrokeParameter = trackStroke,
            gapSizeParameter = gapSize,
            wavelengthParameter = wavelength,
            waveSpeedParameter = waveSpeed,
        )

    override fun update(node: IndeterminateLinearWavyProgressNode) {
        super.update(node)
        node.firstLineHeadProgress = firstLineHeadProgress
        node.firstLineTailProgress = firstLineTailProgress
        node.secondLineHeadProgress = secondLineHeadProgress
        node.secondLineTailProgress = secondLineTailProgress
        node.amplitude = amplitude
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "indeterminateLinearWavyProgressIndicator"
        properties["amplitude"] = amplitude
        baseInspectableProperties()
        // Exclude the lambdas for head and tail progress
    }

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is IndeterminateLinearWavyProgressElement) return false
        if (amplitude != other.amplitude) return false
        // Exclude the lambdas for head and tail progress
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + amplitude.hashCode()
        // Exclude the lambdas for head and tail progress
        return result
    }
}

private class IndeterminateLinearWavyProgressNode(
    var firstLineHeadProgress: () -> Float,
    var firstLineTailProgress: () -> Float,
    var secondLineHeadProgress: () -> Float,
    var secondLineTailProgress: () -> Float,
    amplitudeParameter: Float,
    colorParameter: Color,
    trackColorParameter: Color,
    strokeParameter: Stroke,
    trackStrokeParameter: Stroke,
    gapSizeParameter: Dp,
    wavelengthParameter: Dp,
    waveSpeedParameter: Dp,
) :
    BaseLinearWavyProgressNode(
        colorParameter,
        trackColorParameter,
        strokeParameter,
        trackStrokeParameter,
        gapSizeParameter,
        wavelengthParameter,
        waveSpeedParameter,
    ) {

    var amplitude: Float = amplitudeParameter.fastCoerceIn(0f, 1f)
        set(value) {
            val coerced = value.fastCoerceIn(0f, 1f)
            if (field != coerced) {
                field = coerced
                invalidateDrawCache()
            }
        }

    private val progressFractionsArray = floatArrayOf(0f, 0f, 0f, 0f)

    override fun getProgressFractions(): FloatArray {
        progressFractionsArray[0] = firstLineTailProgress()
        progressFractionsArray[1] = firstLineHeadProgress()
        progressFractionsArray[2] = secondLineTailProgress()
        progressFractionsArray[3] = secondLineHeadProgress()
        return progressFractionsArray
    }

    override fun invalidateDraw() {
        cacheDrawNodeDelegate.invalidateDraw()
    }

    override fun invalidateDrawCache() {
        cacheDrawNodeDelegate.invalidateDrawCache()
    }

    private val cacheDrawNodeDelegate: CacheDrawModifierNode =
        delegate(
            CacheDrawModifierNode {
                // In case the amplitude was updated, update the animation that will transition it
                // smoothly.
                updateAmplitudeAnimation(amplitude)

                onDrawWithContent {
                    // Read from the states here and update the LinearProgressDrawingCache inside
                    // the onDrawWithContent to avoid voiding the cache at this
                    // CacheDrawModifierNode.
                    val currentAmplitude = amplitudeAnimatable?.value ?: 0f
                    progressDrawingCache.updatePaths(
                        size = size,
                        wavelength = wavelength.toPx(),
                        progressFractions = getProgressFractions(),
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

                            // Draw progress
                            val progressPaths = progressPathsToDraw
                            if (progressPaths != null) {
                                for (i in progressPaths.indices) {
                                    drawPath(path = progressPaths[i], color = color, style = stroke)
                                }
                            }
                        }
                    }
                }
            }
        )
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
