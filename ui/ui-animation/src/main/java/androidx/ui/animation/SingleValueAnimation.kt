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

package androidx.ui.animation

import androidx.animation.AnimationBuilder
import androidx.animation.AnimationEndReason
import androidx.animation.AnimationVector
import androidx.animation.PhysicsBuilder
import androidx.animation.TwoWayConverter
import androidx.compose.Composable
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.graphics.Color

// TODO: Implement animate for all built in data types - dp, px, etc
/**
 * Fire-and-forget animation [Composable] for [Float]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue], and [animatedFloat] for fling support.
 *
 * @sample androidx.ui.animation.samples.VisibilityTransition
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Float,
    animBuilder: AnimationBuilder<Float> = remember { PhysicsBuilder() },
    endListener: ((Float) -> Unit)? = null
): Float {
    val anim = remember {
        AnimatedFloatModel(target, AnimationClockAmbient.current)
    }
    // TODO: Support changing animation while keeping the same target
    onCommit(target) {
        if (endListener != null) {
            anim.animateTo(target, animBuilder) { reason, value ->
                if (reason == AnimationEndReason.TargetReached) {
                    endListener.invoke(value)
                }
            }
        } else {
            anim.animateTo(target, animBuilder)
        }
    }
    return anim.value
}

/**
 * Fire-and-forget animation [Composable] for [Color]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @sample androidx.ui.animation.samples.ColorTransition
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Color,
    animBuilder: AnimationBuilder<Color> = remember { PhysicsBuilder() },
    endListener: ((Color) -> Unit)? = null
): Color {
    return animate(
        target, ColorToVectorConverter(target.colorSpace),
        animBuilder, endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for any value. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter
 * the course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @sample androidx.ui.animation.samples.ArbitraryValueTypeTransition
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun <T, V : AnimationVector> animate(
    target: T,
    converter: TwoWayConverter<T, V>,
    animBuilder: AnimationBuilder<T> = remember { PhysicsBuilder() },
    endListener: ((T) -> Unit)? = null
): T {
    val anim = remember {
        AnimatedValueModel(target, converter, AnimationClockAmbient.current)
    }
    // TODO: Support changing animation while keeping the same target
    onCommit(target) {
        if (endListener != null) {
            anim.animateTo(target, animBuilder) { reason, value ->
                if (reason == AnimationEndReason.TargetReached) {
                    endListener.invoke(value)
                }
            }
        } else {
            anim.animateTo(target, animBuilder)
        }
    }
    return anim.value
}