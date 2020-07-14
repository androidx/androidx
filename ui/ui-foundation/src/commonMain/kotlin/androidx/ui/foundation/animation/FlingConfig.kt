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

package androidx.ui.foundation.animation

import androidx.animation.AnimatedFloat
import androidx.animation.AnimationEndReason
import androidx.animation.AnimationSpec
import androidx.animation.ExponentialDecay
import androidx.animation.FloatDecayAnimationSpec
import androidx.animation.OnAnimationEnd
import androidx.animation.SpringSpec
import androidx.animation.TargetAnimation
import androidx.animation.fling
import androidx.compose.Composable
import androidx.compose.Immutable
import kotlin.math.abs

/**
 * Class to specify fling behavior.
 *
 * When drag has ended, this class specifies what to do given the velocity
 * with which drag ended and AnimatedFloat instance to perform fling on and read current value.
 *
 * Config that provides natural fling with customizable behaviour
 * e.g fling friction or result target adjustment.
 *
 * If you want to only be able to drag/animate between predefined set of values,
 * consider using [AnchorsFlingConfig] function to generate such behaviour.
 *
 * @param decayAnimation the animation to control fling behaviour
 * @param onAnimationEnd callback to be invoked when fling finishes by decay
 * or being interrupted by gesture input.
 * Consider second boolean param "cancelled" to know what happened.
 * @param adjustTarget callback to be called at the start of fling
 * so the final value for fling can be adjusted
 */
@Immutable
data class FlingConfig(
    val decayAnimation: FloatDecayAnimationSpec,
    val onAnimationEnd: OnAnimationEnd? = null,
    val adjustTarget: (Float) -> TargetAnimation? = { null }
)

@Composable
internal expect fun ActualFlingConfig(
    onAnimationEnd: OnAnimationEnd?,
    adjustTarget: (Float) -> TargetAnimation?
): FlingConfig

/**
 * Specify fling behavior configured for the current composition. See [FlingConfig].
 *
 * @param onAnimationEnd callback to be invoked when fling finishes by decay
 * or being interrupted by gesture input.
 * Consider second boolean param "cancelled" to know what happened.
 * @param adjustTarget callback to be called at the start of fling
 * so the final value for fling can be adjusted
 */
@Composable
fun FlingConfig(
    onAnimationEnd: OnAnimationEnd? = null,
    adjustTarget: (Float) -> TargetAnimation? = { null }
): FlingConfig = ActualFlingConfig(onAnimationEnd, adjustTarget)

/**
 * Starts a fling animation with the specified starting velocity and fling configuration.
 *
 * @param config configuration that specifies fling behaviour
 * @param startVelocity Starting velocity of the fling animation
 */
fun AnimatedFloat.fling(config: FlingConfig, startVelocity: Float) {
    fling(
        startVelocity,
        config.decayAnimation,
        config.adjustTarget,
        config.onAnimationEnd
    )
}

/**
 * Create fling config with anchors will make sure that after drag has ended,
 * the value will be animated to one of the points from the predefined list.
 *
 * It takes velocity into account, though value will be animated to the closest
 * point in provided list considering velocity.
 *
 * @param anchors set of anchors to animate to
 * @param onAnimationEnd callback to be invoked when animation value reaches desired anchor
 * or fling being interrupted by gesture input.
 * Consult [AnimationEndReason] param to know what happened.
 * @param animationSpec animation which will be used for animations
 * @param decayAnimation decay animation to be used to calculate closest point in the anchors set
 * considering velocity.
 */
fun AnchorsFlingConfig(
    anchors: List<Float>,
    animationSpec: AnimationSpec<Float> = SpringSpec(),
    onAnimationEnd: OnAnimationEnd? = null,
    decayAnimation: FloatDecayAnimationSpec = ExponentialDecay()
): FlingConfig {
    val adjustTarget: (Float) -> TargetAnimation? = { target ->
        val point = anchors.minByOrNull { abs(it - target) }
        val adjusted = point ?: target
        TargetAnimation(adjusted, animationSpec)
    }
    return FlingConfig(decayAnimation, onAnimationEnd, adjustTarget)
}