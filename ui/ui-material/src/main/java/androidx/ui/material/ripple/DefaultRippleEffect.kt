/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.animation.FastOutSlowInEasing
import androidx.animation.FloatPropKey
import androidx.animation.InterruptionHandling
import androidx.animation.LinearEasing
import androidx.animation.TransitionAnimation
import androidx.animation.TweenBuilder
import androidx.animation.createAnimation
import androidx.animation.transitionDefinition
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.ui.animation.PxPositionPropKey
import androidx.ui.graphics.Color
import androidx.ui.graphics.drawscope.DrawScope
import androidx.ui.graphics.drawscope.clipRect
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.center
import androidx.ui.unit.dp
import androidx.ui.unit.getDistance
import androidx.ui.unit.inMilliseconds
import androidx.ui.unit.milliseconds
import androidx.ui.unit.toOffset
import kotlin.math.max

/**
 * Used to specify this type of [RippleEffect] for [ripple].
 */
object DefaultRippleEffectFactory : RippleEffectFactory {

    override fun create(
        size: PxSize,
        startPosition: PxPosition,
        density: Density,
        radius: Dp?,
        clipped: Boolean,
        clock: AnimationClockObservable,
        onAnimationFinished: ((RippleEffect) -> Unit)
    ): RippleEffect {
        return DefaultRippleEffect(
            size,
            startPosition,
            density,
            radius,
            clipped,
            clock,
            onAnimationFinished
        )
    }
}

/**
 * [RippleEffect]s are drawn as part of [ripple] as a visual indicator for a pressed state.
 *
 * Use [ripple] to add an animation for your component.
 *
 * This is a default implementation based on the Material Design specification.
 *
 * A circular ripple effect whose origin starts at the input touch point and
 * whose radius expands from 60% of the final value. The ripple origin
 * animates to the center of its target layout for the bounded version
 * and stays in the center for the unbounded one.
 *
 * @param size The size of the target layout.
 * @param startPosition The position the animation will start from.
 * @param density The [Density] object to convert the dimensions.
 * @param radius Effects grow up to this size.
 * @param clipped If true the effect should be clipped by the target layout bounds.
 * @param clock The animation clock observable that will drive this ripple effect
 * @param onAnimationFinished Call when the effect animation has been finished.
 */
private class DefaultRippleEffect(
    size: PxSize,
    startPosition: PxPosition,
    density: Density,
    radius: Dp? = null,
    private val clipped: Boolean,
    clock: AnimationClockObservable,
    private val onAnimationFinished: ((RippleEffect) -> Unit)
) : RippleEffect {

    private val animation: TransitionAnimation<RippleTransition.State>
    private var transitionState = RippleTransition.State.Initial
    private var finishRequested = false
    private var animationPulse by mutableStateOf(0L)

    init {
        val surfaceSize = size
        val startRadius = getRippleStartRadius(surfaceSize)
        val targetRadius = with(density) {
            radius?.toPx() ?: getRippleEndRadius(clipped, surfaceSize)
        }

        val center = size.center()
        animation = RippleTransition.definition(
            startRadius = startRadius,
            endRadius = targetRadius,
            startCenter = startPosition,
            endCenter = center
        ).createAnimation(clock)
        animation.onUpdate = {
            // TODO We shouldn't need this animationPulse hack b/152631516
            animationPulse++
        }
        animation.onStateChangeFinished = { stage ->
            transitionState = stage
            if (transitionState == RippleTransition.State.Finished) {
                onAnimationFinished(this)
            }
        }
        // currently we are in Initial state, now we start the animation:
        animation.toState(RippleTransition.State.Revealed)
    }

    override fun finish(canceled: Boolean) {
        finishRequested = true
        animation.toState(RippleTransition.State.Finished)
    }

    override fun DrawScope.draw(color: Color) {
        animationPulse // model read so we will be redrawn with the next animation values

        val alpha = if (transitionState == RippleTransition.State.Initial && finishRequested) {
            // if we still fading-in we should immediately switch to the final alpha.
            1f
        } else {
            animation[RippleTransition.Alpha]
        }

        val centerOffset = animation[RippleTransition.Center].toOffset()
        val radius = animation[RippleTransition.Radius]

        val modulatedColor = color.copy(alpha = color.alpha * alpha)
        if (clipped) {
            clipRect {
                drawCircle(modulatedColor, radius, centerOffset)
            }
        } else {
            drawCircle(modulatedColor, radius, centerOffset)
        }
    }
}

/**
 * The Ripple transition specification.
 */
private object RippleTransition {

    enum class State {
        /** The starting state.  */
        Initial,
        /** User is still touching the surface.  */
        Revealed,
        /** User stopped touching the surface.  */
        Finished
    }

    private val FadeInDuration = 75.milliseconds
    private val RadiusDuration = 225.milliseconds
    private val FadeOutDuration = 150.milliseconds

    val Alpha = FloatPropKey()
    val Radius = FloatPropKey()
    val Center = PxPositionPropKey()

    fun definition(
        startRadius: Float,
        endRadius: Float,
        startCenter: PxPosition,
        endCenter: PxPosition
    ) = transitionDefinition {
        state(State.Initial) {
            this[Alpha] = 0f
            this[Radius] = startRadius
            this[Center] = startCenter
        }
        state(State.Revealed) {
            this[Alpha] = 1f
            this[Radius] = endRadius
            this[Center] = endCenter
        }
        state(State.Finished) {
            this[Alpha] = 0f
            // the rest are the same as for Revealed
            this[Radius] = endRadius
            this[Center] = endCenter
        }
        transition(State.Initial to State.Revealed) {
            Alpha using tween {
                duration = FadeInDuration.inMilliseconds().toInt()
                easing = LinearEasing
            }
            Radius using tween {
                duration = RadiusDuration.inMilliseconds().toInt()
                easing = FastOutSlowInEasing
            }
            Center using tween {
                duration = RadiusDuration.inMilliseconds().toInt()
                easing = LinearEasing
            }
            // we need to always finish the radius animation before starting fading out
            interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
        }
        transition(State.Revealed to State.Finished) {
            fun <T> TweenBuilder<T>.toFinished() {
                duration = FadeOutDuration.inMilliseconds().toInt()
                easing = LinearEasing
            }
            Alpha using tween { toFinished() }
            Radius using tween { toFinished() }
            Center using tween { toFinished() }
        }
    }
}

/**
 * According to specs the starting radius is equal to 60% of the largest dimension of the
 * surface it belongs to.
 */
internal fun getRippleStartRadius(size: PxSize) =
    max(size.width, size.height) * 0.3f

/**
 * According to specs the ending radius
 * - expands to 10dp beyond the border of the surface it belongs to for bounded ripples
 * - fits within the border of the surface it belongs to for unbounded ripples
 */
internal fun Density.getRippleEndRadius(bounded: Boolean, size: PxSize): Float {
    val radiusCoveringBounds =
        (PxPosition(size.width, size.height).getDistance() / 2f).value
    return if (bounded) {
        radiusCoveringBounds + BoundedRippleExtraRadius.toPx()
    } else {
        radiusCoveringBounds
    }
}

private val BoundedRippleExtraRadius = 10.dp
