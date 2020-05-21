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

import androidx.animation.AnimationClockObservable
import androidx.compose.Composable
import androidx.compose.State
import androidx.compose.StructurallyEqual
import androidx.compose.frames.modelListOf
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.animation.asDisposableClock
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.ContentDrawScope
import androidx.ui.foundation.IndicationInstance
import androidx.ui.foundation.Indication
import androidx.ui.foundation.Interaction
import androidx.ui.foundation.InteractionState
import androidx.ui.graphics.Color
import androidx.ui.graphics.useOrElse
import androidx.ui.material.MaterialTheme
import androidx.ui.unit.Dp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.center
import androidx.ui.unit.px
import androidx.ui.util.fastForEach

/**
 * Material implementation of [IndicationInstance] that expresses indication via ripples. This
 * [IndicationInstance] will be used by default in Modifier.indication() if you have a [MaterialTheme]
 * set in your hierarchy.
 *
 * Ripple responds to a tap by starting a new [RippleEffect] animation.
 * For creating an effect it uses the [RippleTheme.factory].
 *
 * By default this [Indication] with default parameters will be provided by
 * [MaterialTheme]. You can also manually create a [RippleIndication] and provide it to [Modifier
 * .indication] in order to customize its appearance.
 *
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded
 * ripples always animate from the target layout center, bounded ripples animate from the touch
 * position.
 * @param radius Effects grow up to this size. If null is provided the size would be calculated
 * based on the target layout size.
 * @param color The Ripple color is usually the same color used by the text or iconography in the
 * component. If [Color.Unset] is provided the color will be calculated by
 * [RippleTheme.defaultColor].
 */
@Composable
fun RippleIndication(
    bounded: Boolean = true,
    radius: Dp? = null,
    color: Color = Color.Unset
): RippleIndication {
    val rippleTheme = RippleThemeAmbient.current
    val resolvedColor =
        (color.useOrElse { rippleTheme.defaultColor() }).copy(alpha = rippleTheme.opacity())
    val colorState = state(StructurallyEqual) { resolvedColor }
    val clocks = AnimationClockAmbient.current.asDisposableClock()
    val indication = remember(bounded, radius, clocks, rippleTheme) {
        RippleIndication(bounded, radius, colorState, rippleTheme.factory, clocks)
    }
    colorState.value = resolvedColor
    return indication
}

/**
 * Material implementation of [IndicationInstance] that expresses indication via ripples. This
 * [IndicationInstance] will be used by default in Modifier.indication() if you have a [MaterialTheme]
 * set in your hierarchy.
 *
 * Ripple responds to a tap by starting a new [RippleEffect] animation.
 * For creating an effect it uses the [RippleTheme.factory].
 *
 * By default an [Indication] that creates instances of this class will be provided by
 * [MaterialTheme]. You can also manually create a [RippleIndicationInstance] and provide it to [Modifier
 * .indication] in order to customize its appearance.
 */
class RippleIndication internal constructor(
    private val bounded: Boolean,
    private val radius: Dp? = null,
    private var color: State<Color>,
    private val factory: RippleEffectFactory,
    private val clock: AnimationClockObservable
) : Indication {
    override fun createInstance(): IndicationInstance {
        return RippleIndicationInstance(bounded, radius, color, factory, clock)
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
        if (factory != other.factory) return false
        if (clock != other.clock) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bounded.hashCode()
        result = 31 * result + (radius?.hashCode() ?: 0)
        result = 31 * result + color.hashCode()
        result = 31 * result + factory.hashCode()
        result = 31 * result + clock.hashCode()
        return result
    }
}

private class RippleIndicationInstance internal constructor(
    private val bounded: Boolean,
    private val radius: Dp? = null,
    private var color: State<Color>,
    private val factory: RippleEffectFactory,
    private val clock: AnimationClockObservable
) : IndicationInstance {

    private val effects = modelListOf<RippleEffect>()
    private var currentPressPosition: PxPosition? = null
    private var currentEffect: RippleEffect? = null

    override fun ContentDrawScope.drawIndication(interactionState: InteractionState) {
        refreshEffectsState(interactionState)
        drawContent()
        effects.fastForEach {
            with(it) {
                draw(color.value)
            }
        }
    }

    private fun ContentDrawScope.refreshEffectsState(state: InteractionState) {
        val pressPosition = state.interactionPositionFor(Interaction.Pressed)
        if (pressPosition == null) {
            cleanPressState()
        } else if (currentPressPosition != pressPosition) {
            startRippleEffect(pressPosition)
        }
    }

    private fun ContentDrawScope.startRippleEffect(pressPosition: PxPosition) {
        currentEffect?.finish(false)
        val pxSize = PxSize(size.width.px, size.height.px)
        val position = if (bounded) pressPosition else pxSize.center()
        val effect =
            factory.create(pxSize, position, this, radius, bounded, clock) { effect ->
                effects.remove(effect)
                if (currentEffect == effect) {
                    currentEffect = null
                }
            }
        effects.add(effect)
        currentPressPosition = pressPosition
        currentEffect = effect
    }

    private fun cleanPressState() {
        currentEffect?.finish(false)
        currentEffect = null
        currentPressPosition = null
    }

    override fun onDispose() {
        effects.fastForEach { it.dispose() }
        effects.clear()
        currentEffect = null
    }
}
