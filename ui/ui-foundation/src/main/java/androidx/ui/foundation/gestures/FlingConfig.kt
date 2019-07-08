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

package androidx.ui.foundation.gestures

import androidx.animation.AnimatedFloat
import androidx.animation.AnimationBuilder
import androidx.animation.DecayAnimation
import androidx.animation.ExponentialDecay
import androidx.animation.PhysicsBuilder
import androidx.animation.TargetAnimation
import androidx.animation.fling
import kotlin.math.abs

/**
 * Interface to specify fling behavior in [AnimatedDraggable].
 *
 * When drag has ended, this class specifies what to do given the velocity
 * with which drag ended and AnimatedFloat instance to perform fling on and read current value.
 *
 * If you need natural fling support, use [DefaultFlingConfig] or
 * [DecayFlingConfig] to control how much friction is applied to the fling
 *
 * If you want to only be able to drag/animate between predefined set of values,
 * consider using [AnchorsFlingConfig].
 *
 */
interface FlingConfig {
    fun fling(value: AnimatedFloat, startVelocity: Float)
}

/**
 * Fling config with anchors will make sure that after drag has ended,
 * the value will be animated to one of the points from the predefined list.
 *
 * It takes velocity into account, though value will be animated to the closest
 * point in provided list considering velocity.
 *
 * @see ExponentialDecay to understand when to pass your own decayAnimation.
 *
 * @param animationAnchors set of anchors to animate to
 * @param onAnimationFinished callback to be invoked when animation value reaches desired anchor
 * or fling being interrupted by gesture input.
 * Consider the second boolean param "cancelled" to know what happened.
 * @param animationBuilder animation which will be used for animations
 * @param decayAnimation decay animation to be used to calculate closest point in the anchors set
 * considering velocity.
 */
data class AnchorsFlingConfig(
    val animationAnchors: List<Float>,
    val onAnimationFinished: ((finishValue: Float, cancelled: Boolean) -> Unit)? = null,
    val animationBuilder: AnimationBuilder<Float> = PhysicsBuilder(),
    val decayAnimation: DecayAnimation = ExponentialDecay()
) : FlingConfig {

    private val adjust: (Float) -> TargetAnimation? = { target ->
        val point = animationAnchors.minBy { abs(it - target) }
        val adjusted = point ?: target
        TargetAnimation(adjusted, animationBuilder)
    }

    override fun fling(
        value: AnimatedFloat,
        startVelocity: Float
    ) {
        value.fling(
            startVelocity,
            decayAnimation,
            adjust,
            onAnimationFinished?.let {
                { cancelled: Boolean -> it.invoke(value.value, cancelled) }
            }
        )
    }
}

/**
 * Config that provides natural fling with customizable decay behavior
 * e.g fling friction or velocity threshold. Natural fling config doesn't
 * specify where this fling will end.
 *
 * The most common Decay animation is [ExponentialDecay].
 *
 * @param decayAnimation the animation to control fling behaviour
 * @param onFlingFinished callback to be invoked when fling finishes by decay
 * or being interrupted by gesture input.
 * Consider second boolean param "cancelled" to know what happened.
 * @param adjustTarget callback to be called at the start of fling
 * so the final value for fling can be adjusted
 */
data class DecayFlingConfig(
    val decayAnimation: DecayAnimation,
    val onFlingFinished: ((finishValue: Float, cancelled: Boolean) -> Unit)? = null,
    val adjustTarget: (Float) -> TargetAnimation? = { null }
) : FlingConfig {
    override fun fling(value: AnimatedFloat, startVelocity: Float) {
        value.fling(
            startVelocity,
            decayAnimation,
            adjustTarget = adjustTarget,
            onFinished = onFlingFinished?.let {
                { cancelled: Boolean -> it.invoke(value.value, cancelled) }
            }
        )
    }
}

/**
 * Default fling config sets decay animation to [ExponentialDecay] to provide natural fling
 * and no calls no callback when fling finishes.
 */
object DefaultFlingConfig : FlingConfig {
    override fun fling(value: AnimatedFloat, startVelocity: Float) {
        value.fling(startVelocity, ExponentialDecay())
    }
}