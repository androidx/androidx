/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GlowIndication is an [Indication] that displays a diffused shadow behind the component it is
 * applied to. It takes in parameters like [Color], [Shape], blur radius, and Offset to let users
 * customise it to their brand personality.
 */
@Stable
internal class GlowIndication(
    private val color: Color,
    private val shape: Shape,
    private val glowBlurRadius: Dp,
    private val offsetX: Dp,
    private val offsetY: Dp
) : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val animatedGlowBlurRadius by animateDpAsState(targetValue = glowBlurRadius)
        return GlowIndicationInstance(
            color = color,
            shape = shape,
            glowBlurRadius = animatedGlowBlurRadius,
            offsetX = offsetX,
            offsetY = offsetY,
            density = LocalDensity.current
        )
    }
}

private class GlowIndicationInstance(
    color: Color,
    private val shape: Shape,
    private val density: Density,
    private val glowBlurRadius: Dp,
    private val offsetX: Dp,
    private val offsetY: Dp
) : IndicationInstance {
    val shadowColor = color.toArgb()
    val transparentColor = color.copy(alpha = 0f).toArgb()

    val paint = Paint()
    val frameworkPaint = paint.asFrameworkPaint()

    init {
        frameworkPaint.color = transparentColor

        with(density) {
            frameworkPaint.setShadowLayer(
                glowBlurRadius.toPx(),
                offsetX.toPx(),
                offsetY.toPx(),
                shadowColor
            )
        }
    }

    override fun ContentDrawScope.drawIndication() {
        drawIntoCanvas { canvas ->
            when (
                val shapeOutline = shape.createOutline(
                    size = size,
                    layoutDirection = layoutDirection,
                    density = this@GlowIndicationInstance.density
                )
            ) {
                is Outline.Rectangle -> canvas.drawRect(shapeOutline.rect, paint)

                is Outline.Rounded -> {
                    val shapeCornerRadiusX = shapeOutline.roundRect.topLeftCornerRadius.x
                    val shapeCornerRadiusY = shapeOutline.roundRect.topLeftCornerRadius.y

                    canvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        shapeCornerRadiusX,
                        shapeCornerRadiusY,
                        paint
                    )
                }

                is Outline.Generic -> canvas.drawPath(shapeOutline.path, paint)
            }
        }
        drawContent()
    }
}

/**
 * Creates and remembers an instance of [GlowIndication].
 * @param color describes the color of the background glow.
 * @param shape describes the shape on which the glow will be clipped.
 * @param glowBlurRadius describes how long and blurred would the glow shadow be.
 * @param offsetX describes the horizontal offset of the glow from the composable.
 * @param offsetY describes the vertical offset of the glow from the composable.
 * @return A remembered instance of [GlowIndication].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun rememberGlowIndication(
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    shape: Shape = RectangleShape,
    glowBlurRadius: Dp = 0.dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp
) = remember(color, shape, glowBlurRadius, offsetX, offsetY) {
    GlowIndication(
        color = color,
        shape = shape,
        glowBlurRadius = glowBlurRadius,
        offsetX = offsetY,
        offsetY = offsetX
    )
}

/**
 * Border indication will add a border around the composable (independent from [Modifier.border])
 * that can react to different [Indication] states like focused, pressed, etc.
 * @param brush describes the color/gradient of the border.
 * @param width describes the width/thickness of the border.
 * @param shape describes the shape of the border. It is generally kept the same as the composable
 * it's being applied to.
 * @param inset describes the offset of the border from the composable it's being applied to.
 */
@Immutable
internal class BorderIndication(
    private val brush: Brush,
    private val width: Dp,
    private val shape: Shape,
    private val inset: Dp = 0.dp
) : Indication {

    /**
     * Creates an instance of [BorderIndication] from [androidx.tv.material3.Border].
     * @param border the [androidx.tv.material3.Border] instance that is used to create and return
     * an [BorderIndication] instance
     */
    @OptIn(ExperimentalTvMaterial3Api::class)
    constructor(border: Border) :
        this(
            brush = border.border.brush,
            width = border.border.width,
            shape = border.shape,
            inset = border.inset
        )
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val density = LocalDensity.current
        val brushState = rememberUpdatedState(brush)
        val widthState = rememberUpdatedState(width)
        val shapeState = rememberUpdatedState(shape)
        val insetState = rememberUpdatedState(inset)

        return remember(interactionSource, density) {
            BorderIndicationInstance(
                brush = brushState,
                width = widthState,
                shape = shapeState,
                density = density,
                inset = insetState
            )
        }
    }
}

internal class BorderIndicationInstance(
    private val brush: State<Brush>,
    private val width: State<Dp>,
    private val shape: State<Shape>,
    private val density: Density,
    private val inset: State<Dp>
) : IndicationInstance {
    override fun ContentDrawScope.drawIndication() {
        drawContent()

        inset(inset = -inset.value.toPx()) {
            drawOutline(
                outline = shape.value.createOutline(
                    size = size,
                    layoutDirection = layoutDirection,
                    density = this@BorderIndicationInstance.density
                ),
                brush = brush.value,
                alpha = 1f,
                style = Stroke(
                    width = width.value.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

internal object ScaleIndicationTokens {
    const val focusDuration: Int = 300
    const val unFocusDuration: Int = 500
    const val pressedDuration: Int = 120
    const val releaseDuration: Int = 300
    val enterEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)
}

/**
 * ScaleIndication is an [Indication] that scales the composable by the provided factor. This
 * indication by default will create a smooth animation between the state changes.
 */
@Stable
internal class ScaleIndication(private val scale: Float) : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val interaction by interactionSource.interactions.collectAsState(
            initial = FocusInteraction.Focus()
        )

        val animationSpec = defaultScaleAnimationSpec(interaction)

        val animatedScale by animateFloatAsState(
            targetValue = scale,
            animationSpec = animationSpec
        )

        return remember(animatedScale, animationSpec) {
            ScaleIndicationInstance(scale = animatedScale)
        }
    }

    private fun defaultScaleAnimationSpec(interaction: Interaction): TweenSpec<Float> =
        tween(
            durationMillis = when (interaction) {
                is FocusInteraction.Focus -> ScaleIndicationTokens.focusDuration
                is FocusInteraction.Unfocus -> ScaleIndicationTokens.unFocusDuration
                is PressInteraction.Press -> ScaleIndicationTokens.pressedDuration
                is PressInteraction.Release -> ScaleIndicationTokens.releaseDuration
                is PressInteraction.Cancel -> ScaleIndicationTokens.releaseDuration
                else -> ScaleIndicationTokens.releaseDuration
            },
            easing = ScaleIndicationTokens.enterEasing
        )
}

internal class ScaleIndicationInstance(
    private val scale: Float
) : IndicationInstance {
    override fun ContentDrawScope.drawIndication() {
        scale(scale) {
            this@drawIndication.drawContent()
        }
    }
}
