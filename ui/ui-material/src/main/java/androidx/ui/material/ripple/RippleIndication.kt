/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.material.ripple

import androidx.animation.AnimationBuilder
import androidx.animation.AnimationClockObservable
import androidx.animation.LinearEasing
import androidx.animation.TweenBuilder
import androidx.compose.Composable
import androidx.compose.Stable
import androidx.compose.State
import androidx.compose.StructurallyEqual
import androidx.compose.frames.modelListOf
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.animation.AnimatedFloatModel
import androidx.ui.animation.asDisposableClock
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.ContentDrawScope
import androidx.ui.foundation.IndicationInstance
import androidx.ui.foundation.Indication
import androidx.ui.foundation.Interaction
import androidx.ui.foundation.InteractionState
import androidx.ui.graphics.Color
import androidx.ui.graphics.drawscope.DrawScope
import androidx.ui.graphics.drawscope.clipRect
import androidx.ui.graphics.useOrElse
import androidx.ui.material.MaterialTheme
import androidx.ui.unit.Dp
import androidx.ui.geometry.Offset
import androidx.ui.unit.PxSize
import androidx.ui.unit.center
import androidx.ui.util.fastForEach

/**
 * Material implementation of [IndicationInstance] that expresses indication via ripples. This
 * [IndicationInstance] will be used by default in Modifier.indication() if you have a
 * [MaterialTheme] set in your hierarchy.
 *
 * RippleIndication responds to [Interaction.Pressed] by starting a new [RippleAnimation], and
 * responds to other interactions by showing a fixed state layer.
 *
 * By default this [Indication] with default parameters will be provided by [MaterialTheme]
 * through [androidx.ui.foundation.IndicationAmbient], and hence used in interactions such as
 * [androidx.ui.foundation.clickable] out of the box. You can also manually create a
 * [RippleIndication] and provide it to [androidx.ui.foundation.indication] in order to
 * customize its appearance.
 *
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded
 * ripples always animate from the target layout center, bounded ripples animate from the touch
 * position.
 * @param radius Effects grow up to this size. If null is provided the size would be calculated
 * based on the target layout size.
 * @param color The Ripple color is usually the same color used by the text or iconography in the
 * component. If [Color.Unset] is provided the color will be calculated by
 * [RippleTheme.defaultColor]. This color will then have [RippleTheme.rippleOpacity] applied
 */
@Composable
fun RippleIndication(
    bounded: Boolean = true,
    radius: Dp? = null,
    color: Color = Color.Unset
): RippleIndication {
    val theme = RippleThemeAmbient.current
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val resolvedColor = color.useOrElse { theme.defaultColor() }
    val colorState = state(StructurallyEqual) { resolvedColor }
    colorState.value = resolvedColor
    val interactionOpacity = theme.rippleOpacity()
    return remember(bounded, radius, theme, clock) {
        RippleIndication(bounded, radius, colorState, interactionOpacity, clock)
    }
}

/**
 * Material implementation of [IndicationInstance] that expresses indication via ripples. This
 * [IndicationInstance] will be used by default in Modifier.indication() if you have a
 * [MaterialTheme] set in your hierarchy.
 *
 * RippleIndication responds to [Interaction.Pressed] by starting a new [RippleAnimation], and
 * responds to other interactions by showing a fixed state layer.
 *
 * By default this [Indication] with default parameters will be provided by [MaterialTheme]
 * through [androidx.ui.foundation.IndicationAmbient], and hence used in interactions such as
 * [androidx.ui.foundation.clickable] out of the box. You can also manually create a
 * [RippleIndication] and provide it to [androidx.ui.foundation.indication] in order to
 * customize its appearance.
 */
@Stable
class RippleIndication internal constructor(
    private val bounded: Boolean,
    private val radius: Dp? = null,
    private var color: State<Color>,
    private val rippleOpacity: RippleOpacity,
    private val clock: AnimationClockObservable
) : Indication {
    override fun createInstance(): IndicationInstance {
        return RippleIndicationInstance(bounded, radius, color, rippleOpacity, clock)
    }

    // to force stability on this indication we need equals and hashcode, there's no value in
    // making this class to be "data class"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RippleIndication

        if (bounded != other.bounded) return false
        if (radius != other.radius) return false
        if (color != other.color) return false
        if (rippleOpacity != other.rippleOpacity) return false
        if (clock != other.clock) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bounded.hashCode()
        result = 31 * result + (radius?.hashCode() ?: 0)
        result = 31 * result + color.hashCode()
        result = 31 * result + rippleOpacity.hashCode()
        result = 31 * result + clock.hashCode()
        return result
    }
}

private class RippleIndicationInstance internal constructor(
    private val bounded: Boolean,
    private val radius: Dp? = null,
    private var color: State<Color>,
    private val rippleOpacity: RippleOpacity,
    private val clock: AnimationClockObservable
) : IndicationInstance {

    private val stateLayer = StateLayer(clock, bounded, rippleOpacity)

    private val ripples = modelListOf<RippleAnimation>()
    private var currentPressPosition: Offset? = null
    private var currentRipple: RippleAnimation? = null

    override fun ContentDrawScope.drawIndication(interactionState: InteractionState) {
        val targetRadius =
            radius?.toPx() ?: getRippleEndRadius(bounded, PxSize(size.width, size.height))
        drawContent()
        with(stateLayer) {
            drawStateLayer(interactionState, targetRadius, color.value)
        }
        val pressPosition = interactionState.interactionPositionFor(Interaction.Pressed)
        if (pressPosition != null) {
            if (currentPressPosition != pressPosition) {
                addRipple(targetRadius, pressPosition)
            }
        } else {
            // TODO: possibly handle cancelling the animation here, need to clarify spec for when
            // ripples and state layers overlap
            removeRipple()
        }
        drawRipples(color.value)
    }

    private fun ContentDrawScope.addRipple(targetRadius: Float, pressPosition: Offset) {
        currentRipple?.finish()
        val pxSize = PxSize(size.width, size.height)
        val position = if (bounded) pressPosition else pxSize.center()
        val ripple = RippleAnimation(pxSize, position, targetRadius, bounded, clock) { ripple ->
            ripples.remove(ripple)
            if (currentRipple == ripple) {
                currentRipple = null
            }
        }
        ripples.add(ripple)
        currentPressPosition = pressPosition
        currentRipple = ripple
    }

    private fun removeRipple() {
        currentRipple?.finish()
        currentRipple = null
        currentPressPosition = null
    }

    private fun DrawScope.drawRipples(color: Color) {
        ripples.fastForEach {
            with(it) {
                val alpha = rippleOpacity.opacityForInteraction(Interaction.Pressed)
                if (alpha != 0f) {
                    draw(color.copy(alpha = alpha))
                }
            }
        }
    }

    override fun onDispose() {
        ripples.clear()
        currentRipple = null
    }
}

private class StateLayer(
    clock: AnimationClockObservable,
    private val bounded: Boolean,
    private val rippleOpacity: RippleOpacity
) {
    private val animatedOpacity = AnimatedFloatModel(0f, clock)
    private var previousInteractions: Set<Interaction> = emptySet()
    private var lastDrawnInteraction: Interaction? = null

    fun ContentDrawScope.drawStateLayer(
        interactionState: InteractionState,
        targetRadius: Float,
        color: Color
    ) {
        val currentInteractions = interactionState.value
        var handled = false

        // Handle a new interaction
        for (interaction in currentInteractions) {
            // Stop looping if we have already moved to a new state
            if (handled) break

            // Move to the next interaction if this interaction is not a new interaction
            if (interaction in previousInteractions) continue

            // Pressed state is explicitly handled with a ripple animation, and not a state layer
            if (interaction is Interaction.Pressed) continue

            // Move to the next interaction if this is not an interaction we show a state layer for
            val targetOpacity = rippleOpacity.opacityForInteraction(interaction)
            if (targetOpacity == 0f) continue

            val animationBuilder = animationBuilderForInteraction(interaction)
            animatedOpacity.animateTo(
                targetOpacity,
                animationBuilder
            )

            lastDrawnInteraction = interaction
            handled = true
        }

        // Clean up any stale interactions if we have not moved to a new interaction
        if (!handled) {
            val previousInteraction = lastDrawnInteraction
            if (previousInteraction != null && previousInteraction !in currentInteractions) {
                animatedOpacity.animateTo(
                    0f,
                    animationBuilderForInteraction(previousInteraction)
                )

                lastDrawnInteraction = null
            }
        }

        previousInteractions = currentInteractions

        val modulatedColor = color.copy(alpha = animatedOpacity.value)

        if (animatedOpacity.value > 0f) {
            if (bounded) {
                clipRect {
                    drawCircle(modulatedColor, targetRadius)
                }
            } else {
                drawCircle(modulatedColor, targetRadius)
            }
        }
    }

    /**
     * TODO: handle [interaction] for hover / focus states
     */
    @Suppress("UNUSED_PARAMETER")
    private fun animationBuilderForInteraction(interaction: Interaction): AnimationBuilder<Float> {
        return TweenBuilder<Float>().apply {
            duration = 15
            easing = LinearEasing
        }
    }
}
