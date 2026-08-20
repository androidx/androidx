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

package androidx.compose.material3.a2ui

import android.graphics.Matrix as AndroidMatrix
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Applies the A2UI loading shimmer effect, including a staggered breathing opacity pulse, shape
 * clipping, and an expanding radial highlight sweep.
 *
 * @param isLight whether the current surface theme is light, used to select light or dark skeleton
 *   palette colors
 * @param shape [Shape] used to clip the indicator's graphics layer
 * @param index 0-based index of this indicator within its group, used to stagger the breathing
 *   opacity pulse
 * @param groupState optional shared [ShimmerIndicatorGroupState] that synchronizes the radial sweep
 *   and breathing phase across multiple indicators in a group
 */
internal fun Modifier.shimmerIndicator(
    isLight: Boolean,
    shape: Shape,
    index: Int = 0,
    groupState: ShimmerIndicatorGroupState? = null,
): Modifier =
    this.then(
        ShimmerIndicatorElement(
            isLight = isLight,
            shape = shape,
            index = index,
            groupState = groupState,
        )
    )

internal data class ShimmerIndicatorElement(
    val isLight: Boolean,
    val shape: Shape,
    val index: Int,
    val groupState: ShimmerIndicatorGroupState?,
) : ModifierNodeElement<ShimmerIndicatorNode>() {
    override fun create(): ShimmerIndicatorNode =
        ShimmerIndicatorNode(
            isLight = isLight,
            shape = shape,
            index = index,
            groupState = groupState,
        )

    override fun update(node: ShimmerIndicatorNode) {
        node.update(
            isLight = isLight,
            shape = shape,
            index = index,
            groupState = groupState,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shimmerIndicator"
        properties["isLight"] = isLight
        properties["shape"] = shape
        properties["index"] = index
        properties["groupState"] = groupState
    }
}

internal class ShimmerIndicatorNode(
    var isLight: Boolean,
    var shape: Shape,
    var index: Int,
    var groupState: ShimmerIndicatorGroupState?,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode {

    internal var state: ShimmerIndicatorGroupState? = null
        private set

    private val shaderMatrix = AndroidMatrix()
    private var cachedIsLight: Boolean? = null
    private var cachedShader: Shader? = null
    private var cachedBrush: ShaderBrush? = null

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val breathingPhase = state?.breathingPhase ?: 0f
        alpha = calculateBreathingAlpha(breathingPhase, index)
        shape = this@ShimmerIndicatorNode.shape
        clip = true
    }

    override fun onAttach() {
        val activeState = groupState ?: ShimmerIndicatorGroupState(coroutineScope)
        state = activeState
        activeState.onIndicatorAttached()
    }

    override fun onDetach() {
        state?.onIndicatorDetached()
        state = null
    }

    fun update(
        isLight: Boolean,
        shape: Shape,
        index: Int,
        groupState: ShimmerIndicatorGroupState?,
    ) {
        var layerInvalidated = false
        var drawInvalidated = false

        if (this.groupState !== groupState) {
            this.groupState = groupState
            if (isAttached) {
                state?.onIndicatorDetached()
                val activeState = groupState ?: ShimmerIndicatorGroupState(coroutineScope)
                this.state = activeState
                activeState.onIndicatorAttached()
            }
            layerInvalidated = true
            drawInvalidated = true
        }
        if (this.isLight != isLight) {
            this.isLight = isLight
            drawInvalidated = true
        }
        if (this.shape != shape) {
            this.shape = shape
            layerInvalidated = true
        }
        if (this.index != index) {
            this.index = index
            layerInvalidated = true
        }
        if (isAttached) {
            if (layerInvalidated) {
                invalidatePlacement()
            }
            if (drawInvalidated) {
                invalidateDraw()
            }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeRelativeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) {
            return
        }

        val baseColor = if (isLight) SkeletonBaseLight else SkeletonBaseDark
        drawRect(color = baseColor)

        val currentState = state
        val highlightAlpha = (currentState?.highlightAlpha ?: 0f).coerceIn(0f, 1f)
        if (highlightAlpha > 0f) {
            val progress = (currentState?.sweepProgress ?: 0f).coerceIn(0f, 1f)

            val rootCoordinates = currentState?.rootCoordinates
            val childCoordinates = requireLayoutCoordinates()
            var shapeX = 0f
            var shapeY = 0f
            var containerWidth = width
            var containerHeight = height

            if (
                rootCoordinates != null &&
                    rootCoordinates.isAttached &&
                    childCoordinates.isAttached &&
                    rootCoordinates.findRootCoordinates() === childCoordinates.findRootCoordinates()
            ) {
                val offset = rootCoordinates.localPositionOf(childCoordinates, Offset.Zero)
                shapeX = offset.x
                shapeY = offset.y
                containerWidth = rootCoordinates.size.width.toFloat().coerceAtLeast(width)
                containerHeight = rootCoordinates.size.height.toFloat().coerceAtLeast(height)
            }

            var shader = cachedShader
            var brush = cachedBrush
            if (shader == null || brush == null || cachedIsLight != isLight) {
                shader = createShimmerShader(isLight)
                brush = ShaderBrush(shader)
                cachedShader = shader
                cachedBrush = brush
                cachedIsLight = isLight
            }

            val maxDimension = max(containerWidth, containerHeight)
            val radius = (progress * maxDimension * 2f).coerceAtLeast(1f)
            shaderMatrix.setScale(radius, radius)
            shaderMatrix.postTranslate(-shapeX, -shapeY)
            shader.setLocalMatrix(shaderMatrix)

            drawRect(brush = brush, alpha = highlightAlpha)
        }

        drawContent()
    }
}

internal val LocalShimmerIndicatorGroupState =
    compositionLocalOf<ShimmerIndicatorGroupState?> { null }

@Stable
internal class ShimmerIndicatorGroupState(private val coroutineScope: CoroutineScope) {
    var rootCoordinates: LayoutCoordinates? = null
    var activeIndicatorCount: Int = 0
        private set

    var sweepProgress by mutableFloatStateOf(0f)
        private set

    var highlightAlpha by mutableFloatStateOf(0f)
        private set

    var breathingPhase by mutableFloatStateOf(0f)
        private set

    private var animationJob: Job? = null

    fun onIndicatorAttached() {
        activeIndicatorCount++
        if (activeIndicatorCount == 1 && animationJob == null) {
            animationJob = coroutineScope.launch {
                launch {
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = SweepAnimationSpec,
                    ) { value, _ ->
                        sweepProgress = value
                    }
                }
                launch {
                    // Starts and ends each cycle at 0f while peaking at 1f via keyframes in
                    // HighlightAlphaAnimationSpec.
                    animate(
                        initialValue = 0f,
                        targetValue = 0f,
                        animationSpec = HighlightAlphaAnimationSpec,
                    ) { value, _ ->
                        highlightAlpha = value
                    }
                }
                launch {
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = BreathingAnimationSpec,
                    ) { value, _ ->
                        breathingPhase = value
                    }
                }
            }
        }
    }

    fun onIndicatorDetached() {
        activeIndicatorCount = (activeIndicatorCount - 1).coerceAtLeast(0)
        if (activeIndicatorCount == 0) {
            animationJob?.cancel()
            animationJob = null
            sweepProgress = 0f
            highlightAlpha = 0f
            breathingPhase = 0f
        }
    }
}

private fun createShimmerShader(isLight: Boolean): Shader {
    val highlight = if (isLight) SkeletonHighlightLight else SkeletonHighlightDark
    val transparent =
        if (isLight) SkeletonHighlightLightTransparent else SkeletonHighlightDarkTransparent
    return RadialGradientShader(
        center = Offset.Zero,
        radius = 1f,
        colors = listOf(transparent, highlight, transparent),
    )
}

internal fun calculateBreathingAlpha(breathingPhase: Float, index: Int): Float {
    val staggerSeconds = index * ShimmerChildStaggerStepSeconds
    val staggerPhaseOffset = (staggerSeconds * 2000f) / ShimmerBreathingDurationMillis.toFloat()
    val phase = ((breathingPhase + staggerPhaseOffset) % 1f + 1f) % 1f
    val midAlpha = (ShimmerBreathingMaxAlpha + ShimmerBreathingMinAlpha) / 2f
    val amplitude = (ShimmerBreathingMaxAlpha - ShimmerBreathingMinAlpha) / 2f

    return (midAlpha + amplitude * cos(phase * 2f * PI.toFloat())).coerceIn(
        ShimmerBreathingMinAlpha,
        ShimmerBreathingMaxAlpha,
    )
}

private val SweepAnimationSpec =
    infiniteRepeatable<Float>(
        animation =
            tween(
                durationMillis = ShimmerSweepDurationMillis,
                delayMillis = ShimmerDelayMillis,
                easing = FastOutSlowInEasing,
            ),
        repeatMode = RepeatMode.Restart,
    )

private val HighlightAlphaAnimationSpec =
    infiniteRepeatable(
        animation =
            keyframes {
                durationMillis = ShimmerSweepDurationMillis
                delayMillis = ShimmerDelayMillis
                0f at 0 using LinearEasing
                1f at ShimmerHighlightPeakMillis using LinearEasing
                0f at ShimmerSweepDurationMillis using LinearEasing
            },
        repeatMode = RepeatMode.Restart,
    )

private val BreathingAnimationSpec =
    infiniteRepeatable<Float>(
        animation =
            tween(
                durationMillis = ShimmerBreathingDurationMillis,
                easing = LinearEasing,
            ),
        repeatMode = RepeatMode.Restart,
    )

internal val SkeletonBaseLight = Color(0xFFE1E3E1)
internal val SkeletonHighlightLight = Color(0xFFF8F9F8)
private val SkeletonHighlightLightTransparent = Color(0x00F8F9F8)
internal val SkeletonBaseDark = Color(0xFF2A2D2A)
internal val SkeletonHighlightDark = Color(0xFF3E443F)
private val SkeletonHighlightDarkTransparent = Color(0x003E443F)

internal const val ShimmerDelayMillis = 100
internal const val ShimmerSweepDurationMillis = 1200
internal const val ShimmerHighlightPeakMillis = 720
internal const val ShimmerBreathingDurationMillis = 2400
internal const val ShimmerBreathingMinAlpha = 0.86f
internal const val ShimmerBreathingMaxAlpha = 1.0f
internal const val ShimmerChildStaggerStepSeconds = 0.15f
