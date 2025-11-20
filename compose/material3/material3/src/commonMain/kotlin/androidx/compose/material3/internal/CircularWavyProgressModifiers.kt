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
import androidx.compose.material3.CircularAdditionalRotationDegreesTarget
import androidx.compose.material3.CircularGlobalRotationDegreesTarget
import androidx.compose.material3.CircularIndeterminateMaxProgress
import androidx.compose.material3.CircularIndeterminateMinProgress
import androidx.compose.material3.DecreasingAmplitudeAnimationSpec
import androidx.compose.material3.IncreasingAmplitudeAnimationSpec
import androidx.compose.material3.circularIndeterminateGlobalRotationAnimationSpec
import androidx.compose.material3.circularIndeterminateProgressAnimationSpec
import androidx.compose.material3.circularIndeterminateRotationAnimationSpec
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A [Modifier] that draws a determinate wavy circular progress indicator.
 *
 * Note: Apply a semantics modifiers (e.g., `Modifier.semantics { ... }` or custom
 * `Modifier.progressSemantics()`) separately.
 *
 * @param progress Lambda returning the current progress (0.0 to 1.0).
 * @param color The color of the progress indicator wave.
 * @param trackColor The color of the track background.
 * @param stroke The [Stroke] style for the progress wave.
 * @param trackStroke The [Stroke] style for the track.
 * @param gapSize The gap between the track and the progress wave.
 * @param amplitude Lambda providing the wave amplitude (0.0 to 1.0) based on progress.
 * @param wavelength The preferred wavelength of the wave.
 * @param waveSpeed The speed of the wave's motion (Dp per second).
 */
internal fun Modifier.circularWavyProgressIndicator(
    progress: () -> Float,
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    gapSize: Dp,
    amplitude: (progress: Float) -> Float,
    wavelength: Dp,
    waveSpeed: Dp,
): Modifier =
    this.then(
        DeterminateCircularWavyProgressElement(
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
    )

/**
 * A [Modifier] that draws an indeterminate wavy circular progress indicator.
 *
 * Note: Apply a semantics modifiers (e.g., `Modifier.semantics { ... }` or custom
 * `Modifier.progressSemantics()`) separately.
 *
 * @param color The color of the progress indicator wave.
 * @param trackColor The color of the track background.
 * @param stroke The [Stroke] style for the progress wave.
 * @param trackStroke The [Stroke] style for the track.
 * @param gapSize The gap between the track and the progress wave.
 * @param amplitude The wave's amplitude (0.0 to 1.0).
 * @param wavelength The preferred wavelength of the wave.
 * @param waveSpeed The speed of the wave's motion (Dp per second).
 */
internal fun Modifier.circularWavyProgressIndicator(
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    gapSize: Dp,
    @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
    wavelength: Dp,
    waveSpeed: Dp,
): Modifier =
    this.then(
        IndeterminateCircularWavyProgressElement(
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

/** A base class for circular wavy progress [ModifierNodeElement]s. */
@Suppress("ModifierNodeInspectableProperties")
private abstract class BaseCircularWavyProgressElement<N : BaseCircularWavyProgressNode>(
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
        if (other !is BaseCircularWavyProgressElement<*>) return false

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

/** A base class for circular wavy progress [Modifier] nodes. */
private abstract class BaseCircularWavyProgressNode(
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
                // Wavelength affects the offset animation
                startOffsetAnimation()
            }
        }

    var waveSpeed: Dp = waveSpeedParameter
        set(value) {
            if (field != value) {
                field = value
                // Speed affects the offset animation
                startOffsetAnimation()
            }
        }

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

    /**
     * Returns true if a wave form is currently being drawn. This would be true if the
     * implementation's amplitude is greater than zero.
     */
    protected abstract fun isDrawingWave(): Boolean

    protected val circularShapes = CircularShapes()
    protected val progressDrawingCache = CircularProgressDrawingCache()

    // Offset animation
    protected val waveOffsetState = mutableFloatStateOf(0f)
    protected var offsetAnimatable: Animatable<Float, AnimationVector1D>? = null
    protected var offsetAnimationJob: Job? = null

    // A vertex count used to calculate the speed of the wave offset animation. The animation will
    // only run when this number is greater than zero.
    protected var vertexCountForCurrentAnimation = -1

    /** A function that returns a track [Path] for a given progress. */
    protected fun trackPathProvider(
        amplitude: Float,
        wavelength: Float,
        strokeWidth: Float,
        size: Size,
        path: Path,
    ): Path? = circularShapes.getTrackPath(path = path)

    /** A function that returns a progress [Path] for a given progress. */
    protected fun progressPathProvider(
        amplitude: Float,
        wavelength: Float,
        strokeWidth: Float,
        size: Size,
        supportMotion: Boolean,
        path: Path,
    ): Path =
        circularShapes.getProgressPath(
            amplitude = amplitude,
            path = path,
            repeatPath = supportMotion,
        )

    /**
     * Starts the wave offset animation.
     *
     * The animation will only start if the amplitude is greater than zero and the
     * [vertexCountForCurrentAnimation], the [waveSpeed], and the [wavelength] are greater than
     * zero.
     */
    protected fun startOffsetAnimation() {
        // Cancel previous job if running
        stopOffsetAnimation()

        // Don't animate if not attached or not drawing a wave (e.g. zero amplitude).
        if (!isAttached || !coroutineScope.isActive || !isDrawingWave()) return

        // Start the animation only if we have a valid vertex count and positive waveSpeed and
        // wavelength.
        if (waveSpeed > 0.dp && wavelength > 0.dp && vertexCountForCurrentAnimation > 0) {
            // Use the current vertex count from shapes if needed for duration precision.
            val durationMillis =
                ((wavelength / waveSpeed) * 1000 * vertexCountForCurrentAnimation)
                    .fastRoundToInt()
                    .coerceAtLeast(MinAnimationDuration)

            // Re-create animatable to ensure it picks up the latest offset state
            val startOffset = waveOffsetState.floatValue
            offsetAnimatable = Animatable(startOffset)
            offsetAnimationJob =
                coroutineScope.launch {
                    val anim = offsetAnimatable ?: return@launch // Safety check
                    // Update bounds relative to the current offset
                    anim.updateBounds(startOffset, startOffset + 1f)
                    anim.animateTo(
                        targetValue = startOffset + 1f,
                        animationSpec =
                            infiniteRepeatable(
                                animation = tween(durationMillis, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart,
                            ),
                    ) {
                        // Update the state on each frame
                        waveOffsetState.floatValue = value % 1f
                    }
                }
        } else {
            // No speed or wavelength, snap offset to 0
            waveOffsetState.floatValue = 0f
        }
    }

    protected fun stopOffsetAnimation() {
        offsetAnimationJob?.cancel()
        offsetAnimationJob = null
        offsetAnimatable = null
    }
}

/** A [ModifierNodeElement] for a determinate wavy circular progress indicator. */
private class DeterminateCircularWavyProgressElement(
    val progress: () -> Float,
    override val color: Color,
    override val trackColor: Color,
    override val stroke: Stroke,
    override val trackStroke: Stroke,
    override val gapSize: Dp,
    val amplitude: (progress: Float) -> Float,
    override val wavelength: Dp,
    override val waveSpeed: Dp,
) :
    BaseCircularWavyProgressElement<DeterminateCircularWavyProgressNode>(
        color = color,
        trackColor = trackColor,
        stroke = stroke,
        trackStroke = trackStroke,
        gapSize = gapSize,
        wavelength = wavelength,
        waveSpeed = waveSpeed,
    ) {

    override fun create(): DeterminateCircularWavyProgressNode =
        DeterminateCircularWavyProgressNode(
            progress = progress,
            amplitude = amplitude,
            colorParameter = color,
            trackColorParameter = trackColor,
            strokeParameter = stroke,
            trackStrokeParameter = trackStroke,
            gapSizeParameter = gapSize,
            wavelengthParameter = wavelength,
            waveSpeedParameter = waveSpeed,
        )

    override fun update(node: DeterminateCircularWavyProgressNode) {
        super.update(node)
        if (node.progress !== progress || node.amplitude !== amplitude) {
            node.progress = progress
            node.amplitude = amplitude
            node.cacheDrawNode.invalidateDrawCache()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "determinateCircularWavyProgressIndicator"
        baseInspectableProperties()
        // Lambdas are excluded
    }

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is DeterminateCircularWavyProgressElement) return false
        if (progress !== other.progress || amplitude !== other.amplitude) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + amplitude.hashCode()
        return result
    }
}

/** A [ModifierNodeElement] for an indeterminate wavy circular progress indicator. */
private class IndeterminateCircularWavyProgressElement(
    override val color: Color,
    override val trackColor: Color,
    override val stroke: Stroke,
    override val trackStroke: Stroke,
    override val gapSize: Dp,
    @FloatRange(from = 0.0, to = 1.0) val amplitude: Float,
    override val wavelength: Dp,
    override val waveSpeed: Dp,
) :
    BaseCircularWavyProgressElement<IndeterminateCircularWavyProgressNode>(
        color = color,
        trackColor = trackColor,
        stroke = stroke,
        trackStroke = trackStroke,
        gapSize = gapSize,
        wavelength = wavelength,
        waveSpeed = waveSpeed,
    ) {

    override fun create(): IndeterminateCircularWavyProgressNode =
        IndeterminateCircularWavyProgressNode(
            colorParameter = color,
            trackColorParameter = trackColor,
            strokeParameter = stroke,
            trackStrokeParameter = trackStroke,
            gapSizeParameter = gapSize,
            amplitudeParameter = amplitude,
            wavelengthParameter = wavelength,
            waveSpeedParameter = waveSpeed,
        )

    override fun update(node: IndeterminateCircularWavyProgressNode) {
        super.update(node)
        node.amplitude = amplitude
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "indeterminateCircularWavyProgressIndicator"
        baseInspectableProperties()
        properties["amplitude"] = amplitude // Add specific fixed amplitude
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndeterminateCircularWavyProgressElement) return false
        if (!super.equals(other)) return false // Check base properties
        if (amplitude != other.amplitude) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + amplitude.hashCode()
        return result
    }
}

/** A node that draws and animates the determinate circular wavy progress indicator. */
private class DeterminateCircularWavyProgressNode(
    var progress: () -> Float,
    var amplitude: (progress: Float) -> Float,
    colorParameter: Color,
    trackColorParameter: Color,
    strokeParameter: Stroke,
    trackStrokeParameter: Stroke,
    gapSizeParameter: Dp,
    wavelengthParameter: Dp,
    waveSpeedParameter: Dp,
) :
    BaseCircularWavyProgressNode(
        colorParameter = colorParameter,
        trackColorParameter = trackColorParameter,
        strokeParameter = strokeParameter,
        trackStrokeParameter = trackStrokeParameter,
        gapSizeParameter = gapSizeParameter,
        wavelengthParameter = wavelengthParameter,
        waveSpeedParameter = waveSpeedParameter,
    ) {

    // Amplitude animation
    private val amplitudeState = mutableFloatStateOf(0f)
    private var amplitudeAnimatable: Animatable<Float, AnimationVector1D>? = null
    private var amplitudeAnimationJob: Job? = null

    override fun onAttach() {
        // No op here. The first draw will resolve the circularShapes.currentVertexCount and will
        // trigger the updateOffsetAnimationState() to start the animation at the right speed.
    }

    override fun onDetach() {
        super.onDetach()
        amplitudeAnimatable = null
        vertexCountForCurrentAnimation = -1
    }

    override fun invalidateDraw() {
        cacheDrawNode.invalidateDraw()
    }

    override fun invalidateDrawCache() {
        cacheDrawNode.invalidateDrawCache()
    }

    override fun isDrawingWave() = amplitudeState.floatValue > 0f

    val cacheDrawNode =
        delegate(
            CacheDrawModifierNode {
                val currentProgress = progress().fastCoerceIn(0f, 1f)
                val currentGapPx = gapSize.toPx()
                val currentWavelengthPx = wavelength.toPx()
                val enableMotion = waveSpeed > 0.dp

                //  Amplitude animation
                val targetAmplitude = amplitude(currentProgress).fastCoerceIn(0f, 1f)

                amplitudeAnimatable
                    ?: Animatable(initialValue = targetAmplitude).also {
                        amplitudeAnimatable = it
                        amplitudeState.floatValue = targetAmplitude
                    }

                // Launch amplitude animation if target changed and node attached/active
                if (
                    isAttached &&
                        amplitudeAnimatable!!.targetValue != targetAmplitude &&
                        (amplitudeAnimationJob == null ||
                            amplitudeAnimationJob?.isCompleted == true)
                ) {
                    amplitudeAnimationJob =
                        coroutineScope.launch {
                            val anim = amplitudeAnimatable ?: return@launch // Safety check
                            anim.animateTo(
                                targetValue = targetAmplitude,
                                animationSpec =
                                    if (anim.value < targetAmplitude) {
                                        IncreasingAmplitudeAnimationSpec
                                    } else {
                                        DecreasingAmplitudeAnimationSpec
                                    },
                            ) {
                                amplitudeState.floatValue = value
                            }

                            // Check if we need to stop the animation if the target amplitude is 0
                            // at the end of the animation.
                            if (targetAmplitude == 0f) {
                                stopOffsetAnimation()
                            }
                        }
                }

                // Pass stroke width for potentially calculating polygon radius accurately
                circularShapes.update(
                    size = size,
                    wavelength = currentWavelengthPx,
                    strokeWidth = stroke.width,
                    requiresMorph = amplitudeAnimationJob != null,
                )

                // Update the vertexCountForCurrentAnimation after the circularShapes.update().
                if (vertexCountForCurrentAnimation != circularShapes.currentVertexCount.intValue) {
                    vertexCountForCurrentAnimation =
                        circularShapes.currentVertexCount.intValue.coerceAtLeast(
                            MinCircularVertexCount
                        )
                }

                // Start the offset animation when the amplitude changes to a positive number. The
                // animation will end when the amplitudeAnimatable finishes and reached
                // zero amplitude again.
                if (
                    targetAmplitude > 0 &&
                        (offsetAnimationJob == null || offsetAnimationJob?.isCompleted == true)
                ) {
                    startOffsetAnimation()
                }

                onDrawWithContent {
                    // Read from the state here and update the CircularProgressDrawingCache inside
                    // the onDrawWithContent to avoid voiding the cache at this
                    // CacheDrawModifierNode.
                    val animatedAmplitudeValue = amplitudeState.floatValue
                    // Update the drawing cache
                    progressDrawingCache.updatePaths(
                        size = size,
                        progressPathProvider = ::progressPathProvider,
                        trackPathProvider = ::trackPathProvider,
                        enableProgressMotion = enableMotion,
                        startProgress = 0f,
                        endProgress = currentProgress,
                        amplitude = animatedAmplitudeValue,
                        waveOffset =
                            if (animatedAmplitudeValue > 0f && enableMotion) {
                                waveOffsetState.floatValue
                            } else {
                                0f
                            },
                        wavelength = currentWavelengthPx,
                        gapSize = currentGapPx,
                        stroke = stroke,
                        trackStroke = trackStroke,
                    )
                    drawCircularIndicator(
                        color = color,
                        trackColor = trackColor,
                        stroke = stroke,
                        trackStroke = trackStroke,
                        drawingCache = progressDrawingCache,
                    )
                }
            }
        )
}

/** A node that draws and animates the indeterminate circular wavy progress indicator. */
private class IndeterminateCircularWavyProgressNode(
    colorParameter: Color,
    trackColorParameter: Color,
    strokeParameter: Stroke,
    trackStrokeParameter: Stroke,
    gapSizeParameter: Dp,
    amplitudeParameter: Float,
    wavelengthParameter: Dp,
    waveSpeedParameter: Dp,
) :
    BaseCircularWavyProgressNode(
        colorParameter = colorParameter,
        trackColorParameter = trackColorParameter,
        strokeParameter = strokeParameter,
        trackStrokeParameter = trackStrokeParameter,
        gapSizeParameter = gapSizeParameter,
        wavelengthParameter = wavelengthParameter,
        waveSpeedParameter = waveSpeedParameter,
    ) {

    // Specific animations for indeterminate progress
    private var globalRotationAnimatable: Animatable<Float, AnimationVector1D>? = null
    private var additionalRotationAnimatable: Animatable<Float, AnimationVector1D>? = null
    private var progressSweepAnimatable: Animatable<Float, AnimationVector1D>? = null
    private var indeterminateAnimationsJob: Job? = null

    var amplitude: Float = amplitudeParameter.coerceIn(0f, 1f)
        set(value) {
            val coerced = value.coerceIn(0f, 1f)
            val oldValue = field
            if (field != coerced) {
                field = coerced
                // If amplitude > 0 was false and is now true, start offset animation.
                // If amplitude becomes 0, stop offset animation.
                if (field > 0f && oldValue == 0f) startOffsetAnimation()
                else if (field == 0f) stopOffsetAnimation()
                cacheDrawNode.invalidateDrawCache()
            }
        }

    override fun onAttach() {
        startIndeterminateAnimations()
        // The offset animation will be triggered from cacheDrawNode or amplitude setter
    }

    override fun onDetach() {
        globalRotationAnimatable = null
        additionalRotationAnimatable = null
        progressSweepAnimatable = null
        vertexCountForCurrentAnimation = -1
    }

    override fun invalidateDraw() {
        cacheDrawNode.invalidateDraw()
    }

    override fun invalidateDrawCache() {
        cacheDrawNode.invalidateDrawCache()
    }

    override fun isDrawingWave(): Boolean = amplitude > 0f

    /**
     * Starts specific indeterminate animations, which include the global rotation, additional
     * rotation, and progress sweep animation.
     */
    private fun startIndeterminateAnimations() {
        indeterminateAnimationsJob?.cancel()

        if (!isAttached || !coroutineScope.isActive) return

        // Initialize the animatables if they are null
        globalRotationAnimatable = globalRotationAnimatable ?: Animatable(0f)
        additionalRotationAnimatable = additionalRotationAnimatable ?: Animatable(0f)
        progressSweepAnimatable =
            progressSweepAnimatable ?: Animatable(CircularIndeterminateMinProgress)

        indeterminateAnimationsJob =
            coroutineScope.launch {
                // Launch all three indeterminate animations concurrently
                launch {
                    val globalRotationAnim =
                        globalRotationAnimatable ?: return@launch // Safety check
                    globalRotationAnim.animateTo(
                        targetValue =
                            globalRotationAnim.value + CircularGlobalRotationDegreesTarget,
                        animationSpec = circularIndeterminateGlobalRotationAnimationSpec,
                    ) {
                        cacheDrawNode.invalidateDraw()
                    }
                }
                launch {
                    val additionalRotationAnim =
                        additionalRotationAnimatable ?: return@launch // Safety check
                    additionalRotationAnim.animateTo(
                        targetValue =
                            additionalRotationAnim.value + CircularAdditionalRotationDegreesTarget,
                        animationSpec = circularIndeterminateRotationAnimationSpec,
                    ) {
                        cacheDrawNode.invalidateDraw()
                    }
                }
                launch {
                    val progressSweepAnim = progressSweepAnimatable ?: return@launch // Safety check
                    progressSweepAnim.animateTo(
                        targetValue =
                            if (
                                progressSweepAnim.value <
                                    (CircularIndeterminateMinProgress +
                                        CircularIndeterminateMaxProgress) / 2
                            )
                                CircularIndeterminateMaxProgress
                            else CircularIndeterminateMinProgress,
                        animationSpec = circularIndeterminateProgressAnimationSpec,
                    ) {
                        cacheDrawNode.invalidateDraw()
                    }
                }
            }
    }

    private val cacheDrawNode =
        delegate(
            CacheDrawModifierNode {
                val currentWavelengthPx = wavelength.toPx()
                val currentGapPx = gapSize.toPx()
                val enableMotion = waveSpeed > 0.dp && amplitude > 0f

                // Update the CircularShapes with the values. It will generate the RoundedPolygons
                // required for the progress and track. Since the amplitude is not animating in the
                // indeterminate indicator, the requiresMorph is set to true only when the amplitude
                // is between 0 and 1. In those cases we will need a Morph object to generate a Path
                // that represents that amplitude.
                circularShapes.update(
                    size = size,
                    wavelength = currentWavelengthPx,
                    strokeWidth = stroke.width,
                    requiresMorph = amplitude > 0f && amplitude < 1f,
                )

                // Update the vertexCountForCurrentAnimation after the circularShapes.update().
                if (vertexCountForCurrentAnimation != circularShapes.currentVertexCount.intValue) {
                    vertexCountForCurrentAnimation =
                        circularShapes.currentVertexCount.intValue.coerceAtLeast(
                            MinCircularVertexCount
                        )
                }

                // Start the offset animation when the amplitude changes to a positive number. The
                // animation will end when the amplitudeAnimatable finishes and reached
                // zero amplitude again.
                if (
                    amplitude > 0f &&
                        (offsetAnimationJob == null || offsetAnimationJob?.isCompleted == true)
                ) {
                    startOffsetAnimation()
                }

                onDrawWithContent {
                    // Get the current animation values (or defaults if not yet running).
                    // Read from the states here and update the CircularProgressDrawingCache inside
                    // the onDrawWithContent to avoid voiding the cache at this
                    // CacheDrawModifierNode.
                    val currentGlobalRotation = globalRotationAnimatable?.value ?: 0f
                    val currentAdditionalRotation = additionalRotationAnimatable?.value ?: 0f
                    val currentProgressSweep =
                        progressSweepAnimatable?.value ?: CircularIndeterminateMinProgress

                    // Update the drawing cache
                    progressDrawingCache.updatePaths(
                        size = size,
                        progressPathProvider = ::progressPathProvider,
                        trackPathProvider = ::trackPathProvider,
                        enableProgressMotion = enableMotion,
                        startProgress = 0f,
                        endProgress = currentProgressSweep,
                        amplitude = amplitude,
                        waveOffset = if (enableMotion) waveOffsetState.floatValue else 0f,
                        wavelength = currentWavelengthPx,
                        gapSize = currentGapPx,
                        stroke = stroke,
                        trackStroke = trackStroke,
                    )

                    // Draw
                    withTransform(
                        transformBlock = {
                            // Apply rotation
                            rotate(
                                degrees = currentGlobalRotation + currentAdditionalRotation + 90f
                            )
                        }
                    ) {
                        drawCircularIndicator(
                            color = color,
                            trackColor = trackColor,
                            stroke = stroke,
                            trackStroke = trackStroke,
                            drawingCache = progressDrawingCache,
                        )
                    }
                }
            }
        )
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

    // Reusable Matrices for processing paths
    private val scaleMatrix = Matrix()
    private val transformMatrix = Matrix()

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
     * @param progressPathProvider a function that returns a progress [Path] for a given amplitude
     *   and other parameters that may affect the shape. The function is provided with a [Path]
     *   object to avoid having to create a new [Path] object.
     * @param trackPathProvider a function that returns an optional trackPath [Path] for a given
     *   amplitude and other parameters that may affect the shape. The function is provided with a
     *   [Path] object to avoid having to create a new [Path] object.
     * @param enableProgressMotion indicates if a progress motion should be enabled for the provided
     *   progress by offsetting the wave's drawing. When enabled, the calls to the
     *   [progressPathProvider] will be made with a `supportMotion = true`, and the generated [Path]
     *   will be repeated to allow drawing it while shifting the start and stop points and rotating
     *   it to simulate a motion.
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
        progressPathProvider:
            (
                amplitude: Float,
                wavelength: Float,
                strokeWidth: Float,
                size: Size,
                supportsMotion: Boolean,
                path: Path,
            ) -> Path,
        trackPathProvider:
            (
                amplitude: Float, wavelength: Float, strokeWidth: Float, size: Size, path: Path,
            ) -> Path?,
        enableProgressMotion: Boolean,
        @FloatRange(from = 0.0, to = 1.0) startProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) endProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
        @FloatRange(from = 0.0, to = 1.0) waveOffset: Float,
        @FloatRange(from = 0.0, fromInclusive = false) wavelength: Float,
        @FloatRange(from = 0.0) gapSize: Float,
        stroke: Stroke,
        trackStroke: Stroke,
    ) {
        val pathsUpdates =
            updateFullPaths(
                size = size,
                progressPathProvider = progressPathProvider,
                trackPathProvider = trackPathProvider,
                enableProgressMotion = enableProgressMotion,
                amplitude = amplitude,
                wavelength = wavelength,
                gapSize = gapSize,
                stroke = stroke,
                trackStroke = trackStroke,
            )
        updateDrawPaths(
            forceUpdate = pathsUpdates,
            startProgress = startProgress,
            endProgress = endProgress,
            waveOffset = waveOffset,
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
     * @param progressPathProvider a function that returns a progress [Path] for a given amplitude
     *   and other parameters that may affect the shape. The function is provided with a [Path]
     *   object to avoid having to create a new [Path] object.
     * @param trackPathProvider a function that returns an optional trackPath [Path] for a given
     *   amplitude and other parameters that may affect the shape. The function is provided with a
     *   [Path] object to avoid having to create a new [Path] object.
     * @param enableProgressMotion indicates if a progress motion should be enabled for the provided
     *   progress. When enabled, the calls to the [progressPathProvider] will be made with a
     *   `supportMotion = true`, and the generated [Path] will be repeated to allow drawing it while
     *   shifting the start and stop points and rotating it to simulate a motion.
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
        progressPathProvider:
            (
                amplitude: Float,
                wavelength: Float,
                strokeWidth: Float,
                size: Size,
                supportsMotion: Boolean,
                path: Path,
            ) -> Path,
        trackPathProvider:
            (
                amplitude: Float, wavelength: Float, strokeWidth: Float, size: Size, path: Path,
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
        progressPathProvider(
            amplitude,
            wavelength,
            stroke.width,
            size,
            enableProgressMotion,
            fullProgressPath,
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
            trackPathProvider(amplitude, wavelength, stroke.width, size, fullTrackPath)
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
        @FloatRange(from = 0.0, to = 1.0) waveOffset: Float,
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
            val coercedWaveOffset = waveOffset.fastCoerceIn(0f, 1f)
            val startStopShift = coercedWaveOffset * progressPathLength

            progressPathMeasure.getSegment(
                startDistance = pStart + startStopShift,
                stopDistance = pStop + startStopShift,
                destination = progressPathToDraw,
            )

            val offsetAngle = coercedWaveOffset * 360 % 360
            if (offsetAngle != 0f) {
                val fullProgressBounds = fullProgressPath.getBounds()
                // Rotate the progress path to adjust for the shift.
                progressPathToDraw.translate(
                    Offset(-fullProgressBounds.center.x, -fullProgressBounds.center.y)
                )
                transformMatrix.reset()
                progressPathToDraw.transform(
                    transformMatrix.apply { rotateZ(degrees = -offsetAngle) }
                )
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
                destination = progressPathToDraw,
            )
        }
        if (trackPathLength > 0) {
            val tStart = endProgress * trackPathLength + trackSpacing
            val tStop = trackPathLength - trackSpacing
            trackPathMeasure.getSegment(
                startDistance = tStart,
                stopDistance = tStop,
                destination = trackPathToDraw,
            )
        }

        // Cache
        currentStartProgress = startProgress
        currentEndProgress = endProgress
        currentWaveOffset = waveOffset
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
    private var trackPolygon: RoundedPolygon? = null

    /**
     * A normalized [RoundedPolygon] for the circular indicator progress.
     *
     * This property is guaranteed not to be null after a successful call to [update].
     *
     * @see RoundedPolygon.normalized
     * @see update
     */
    private var activeIndicatorPolygon: RoundedPolygon? = null

    /**
     * A [Morph] for the circular indicator progress track. This morph will transition from a circle
     * to a wavy star shape.
     *
     * This property is guaranteed not to be null after a successful call to [update] with a
     * `requiresMorph = true`.
     *
     * Note that the [RoundedPolygon]s that this [Morph] is constructed with are normalized, which
     * allows us to process the generated paths more efficiently by just scaling them to the right
     * size.
     */
    private var activeIndicatorMorph: Morph? = null

    /** Holds the current vertex count as a state. */
    val currentVertexCount = mutableIntStateOf(-1)

    /**
     * Updates the shapes according to the size of the circular loader and its wave's wavelength and
     * strokeWidth.
     *
     * In case the given parameters are different from the current ones, the shapes will be updated.
     *
     * @param size the dimensions of the current drawing environment that the shape are updated for
     * @param wavelength the length of a wave in the rendered circular shape in pixels. This actual
     *   length may end up differently to ensure a continuous wave shape.
     * @param strokeWidth the stroke's width in pixels
     * @param requiresMorph indicate that a [Morph] should be created for the active indicator if it
     *   was not already. The flag is useful to effectively only ask for the creation of the [Morph]
     *   when needed, for example, when an amplitude is set for the first time to a value that is
     *   between 0 and 1.
     */
    fun update(
        size: Size,
        @FloatRange(from = 0.0, fromInclusive = false) wavelength: Float,
        @FloatRange(from = 0.0, fromInclusive = false) strokeWidth: Float,
        requiresMorph: Boolean,
    ) {
        require(wavelength > 0f) { "Wavelength should be greater than zero" }
        if (size == currentSize && wavelength == currentWavelength) {
            if (requiresMorph && activeIndicatorMorph == null) {
                // This is the first time a requiredMorph is set, so we need to create a new Morph.
                // At this point, the trackPolygon and activeIndicatorPolygon are already set.
                activeIndicatorMorph = Morph(start = trackPolygon!!, end = activeIndicatorPolygon!!)
            }
            return
        }
        // Compute the number of edges as a factor of the circle size that the morph will be
        // rendered in and its proposed wavelength (2πr / wavelength), where the radius takes into
        // account the stroke's width.
        val r = size.minDimension / 2 - strokeWidth / 2
        val numVertices = max(MinCircularVertexCount, (2 * PI * r / wavelength).fastRoundToInt())

        if (numVertices != currentVertexCount.intValue) {
            // Note that we match the vertices number at the track's polygon. This will result in a
            // smoother morphing between the active indicator and the track.
            trackPolygon = RoundedPolygon.circle(numVertices = numVertices).normalized()
            activeIndicatorPolygon =
                RoundedPolygon.star(
                        numVerticesPerRadius = numVertices,
                        innerRadius = 0.75f,
                        rounding = CornerRounding(radius = 0.35f, smoothing = 0.4f),
                        innerRounding = CornerRounding(radius = 0.5f),
                    )
                    .normalized()
            if (requiresMorph) {
                activeIndicatorMorph = Morph(start = trackPolygon!!, end = activeIndicatorPolygon!!)
            }
        }

        currentSize = size
        currentWavelength = wavelength
        currentVertexCount.intValue = numVertices
    }

    /** Returns the path for the track polygon. */
    fun getTrackPath(path: Path): Path {
        trackPolygon?.toPath(path = path)
        return path
    }

    /**
     * Returns the path for the active indicator polygon (i.e. the progress indication path). In
     * case a [Morph] was previously created at the [update], the [Morph] will be used to generate
     * the [Path]. Otherwise, the active indicator polygon will be returned when the amplitude is 1,
     * and the track polygon otherwise.
     */
    fun getProgressPath(
        @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
        path: Path,
        repeatPath: Boolean,
        rotationPivotX: Float = 0.5f,
        rotationPivotY: Float = 0.5f,
    ): Path {
        if (activeIndicatorMorph != null) {
            activeIndicatorMorph!!.toPath(
                progress = amplitude,
                path = path,
                repeatPath = repeatPath,
                // The RoundedPolygon used in the Morph were normalized (i.e. moves to
                // (0.5, 0.5)).
                rotationPivotX = rotationPivotX,
                rotationPivotY = rotationPivotY,
            )
        } else {
            // In case there is no Morph (i.e. it was never created at the update call because a
            // requiresMorph was never true), we return the active indicator polygon in case the
            // amplitude is 1. Otherwise, we return the track polygon.
            if (amplitude == 1f && activeIndicatorPolygon != null) {
                activeIndicatorPolygon!!.toPath(path = path, repeatPath = repeatPath)
            } else {
                trackPolygon?.toPath(path = path, repeatPath = repeatPath)
            }
        }
        return path
    }
}

private const val MinCircularVertexCount = 5
// Minimum animation duration to ensure we don't overwhelm the CPU.
private const val MinAnimationDuration = 50
