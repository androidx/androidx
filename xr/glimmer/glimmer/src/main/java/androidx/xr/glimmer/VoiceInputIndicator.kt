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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A voice input indicator that displays sound activity to the user.
 *
 * This component can be used when accepting voice input to show that the app is listening to the
 * user's input. The indicator responds to the [level] provided, showing a visual representation of
 * the audio intensity.
 *
 * See [ContainedVoiceInputIndicator] for the version of this component with a background container
 * where the indicator bars are transparent.
 *
 * @sample androidx.xr.glimmer.samples.VoiceInputIndicatorSampleUsage
 * @param level The level of the voice input, ranging from 0.0 (silence) to 1.0 (loudest).
 * @param modifier The modifier to be applied to the indicator.
 * @param indicatorColor The color of the indicator bars.
 */
@Composable
public fun VoiceInputIndicator(
    level: () -> Float,
    modifier: Modifier = Modifier,
    indicatorColor: Color = GlimmerTheme.colors.primary,
) {
    Spacer(
        modifier =
            modifier
                .size(ContainerSize)
                .then(
                    VoiceInputIndicatorElement(
                        level = level,
                        backgroundColor = Color.Transparent,
                        indicatorColor = indicatorColor,
                        isIndicatorCutOut = false,
                    )
                )
    )
}

/**
 * A voice input indicator with a background container that displays sound activity to the user. In
 * this version of the component, the indicator bars are transparent and cut through its background
 * container.
 *
 * This component can be used when accepting voice input to show that the app is listening to the
 * user's input. The indicator responds to the [level] provided, showing a visual representation of
 * the audio intensity.
 *
 * See [VoiceInputIndicator] for the version of this component with solid indicator bars and no
 * background container.
 *
 * @sample androidx.xr.glimmer.samples.VoiceInputIndicatorSampleUsage
 * @param level The level of the voice input, ranging from 0.0 (silence) to 1.0 (loudest).
 * @param modifier The modifier to be applied to the indicator.
 * @param backgroundColor The color of the background container.
 */
@Composable
public fun ContainedVoiceInputIndicator(
    level: () -> Float,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlimmerTheme.colors.primary,
) {
    Spacer(
        modifier =
            modifier
                .size(ContainerSize)
                .then(
                    VoiceInputIndicatorElement(
                        level = level,
                        backgroundColor = backgroundColor,
                        indicatorColor = Color.Transparent,
                        isIndicatorCutOut = true,
                    )
                )
    )
}

@Suppress("ModifierNodeInspectableProperties")
private class VoiceInputIndicatorElement(
    val level: () -> Float,
    val backgroundColor: Color,
    val indicatorColor: Color,
    val isIndicatorCutOut: Boolean,
) : ModifierNodeElement<VoiceInputIndicatorNode>() {
    override fun create(): VoiceInputIndicatorNode =
        VoiceInputIndicatorNode(level, backgroundColor, indicatorColor, isIndicatorCutOut)

    override fun update(node: VoiceInputIndicatorNode) {
        node.level = level
        node.backgroundColor = backgroundColor
        node.indicatorColor = indicatorColor
        node.isIndicatorCutOut = isIndicatorCutOut
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VoiceInputIndicatorElement) return false

        if (level !== other.level) return false
        if (backgroundColor != other.backgroundColor) return false
        if (indicatorColor != other.indicatorColor) return false
        if (isIndicatorCutOut != other.isIndicatorCutOut) return false

        return true
    }

    override fun hashCode(): Int {
        var result = level.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + indicatorColor.hashCode()
        result = 31 * result + isIndicatorCutOut.hashCode()
        return result
    }
}

private class VoiceInputIndicatorNode(
    var level: () -> Float,
    var backgroundColor: Color,
    var indicatorColor: Color,
    var isIndicatorCutOut: Boolean,
) : Modifier.Node(), DrawModifierNode, LayoutModifierNode {
    private val leftBar = Animatable(0f)
    private val centerBar = Animatable(0f)
    private val rightBar = Animatable(0f)
    private var lastLevel = 0f

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                compositingStrategy =
                    if (isIndicatorCutOut) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        if (backgroundColor != Color.Transparent) {
            drawCircle(color = backgroundColor)
        }

        val level = level().fastCoerceIn(0f, 1f)

        if (lastLevel != level) {
            lastLevel = level
            coroutineScope.launch {
                launch {
                    delay(LeftBarDelay)
                    animateBar(leftBar, level)
                }
                launch { animateBar(centerBar, level) }
                launch {
                    delay(RightBarDelay)
                    animateBar(rightBar, level)
                }
            }
        }

        val dotSizePx = DotSize.toPx()
        val middleBarMaxHeightPx = MiddleBarMaxHeight.toPx()
        val sideBarHeightOffsetPx = SideBarHeightOffset.toPx()

        val centerBarHeight = dotSizePx + (middleBarMaxHeightPx - dotSizePx) * centerBar.value
        val leftBarHeight =
            (dotSizePx + (middleBarMaxHeightPx - dotSizePx) * leftBar.value) * 0.3f +
                sideBarHeightOffsetPx
        val rightBarHeight =
            (dotSizePx + (middleBarMaxHeightPx - dotSizePx) * rightBar.value) * 0.3f +
                sideBarHeightOffsetPx

        val centerX = (size.width - dotSizePx) / 2
        val spacing = BarSpacing.toPx()
        val cornerRadius = CornerRadius(dotSizePx / 2)

        val blendMode = if (isIndicatorCutOut) BlendMode.Clear else BlendMode.SrcOver

        // Left bar
        drawBar(
            x = centerX - spacing - dotSizePx,
            height = leftBarHeight,
            color = indicatorColor,
            barWidth = dotSizePx,
            cornerRadius = cornerRadius,
            blendMode = blendMode,
        )
        // Center bar
        drawBar(
            x = centerX,
            height = centerBarHeight,
            color = indicatorColor,
            barWidth = dotSizePx,
            cornerRadius = cornerRadius,
            blendMode = blendMode,
        )
        // Right bar
        drawBar(
            x = centerX + spacing + dotSizePx,
            height = rightBarHeight,
            color = indicatorColor,
            barWidth = dotSizePx,
            cornerRadius = cornerRadius,
            blendMode = blendMode,
        )
    }

    private suspend fun animateBar(
        animatable: Animatable<Float, AnimationVector1D>,
        target: Float,
    ) {
        // Only animate upwards, otherwise continue existing deflate animation
        if (target > animatable.value) {
            animatable.animateTo(targetValue = target, animationSpec = InflateAnimationSpec)
            animatable.animateTo(targetValue = 0f, animationSpec = DeflateAnimationSpec)
        }
    }

    private fun DrawScope.drawBar(
        x: Float,
        height: Float,
        color: Color,
        barWidth: Float,
        cornerRadius: CornerRadius,
        blendMode: BlendMode,
    ) {
        val top = (size.height - height) / 2
        drawRoundRect(
            color = color,
            topLeft = Offset(x, top),
            size = Size(barWidth, height),
            cornerRadius = cornerRadius,
            blendMode = blendMode,
        )
    }
}

private val ContainerSize = 32.dp
private val DotSize = 6.dp
private val MiddleBarMaxHeight = DotSize * 5
private val SideBarHeightOffset = 4.2.dp
private val LeftBarDelay = 33.milliseconds
private val RightBarDelay = 66.milliseconds
private val BarSpacing = 3.dp

private val InflateAnimationSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
private val DeflateAnimationSpec =
    tween<Float>(durationMillis = 500, easing = LinearOutSlowInEasing)
