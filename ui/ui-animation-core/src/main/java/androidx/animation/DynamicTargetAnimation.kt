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

package androidx.animation

/**
 * Dynamic target animation allows and anticipates the animation target to change frequently. When
 * the target changes as the animation is in-flight, the animation is expected to make a continuous
 * transition to the new target.
 */
interface DynamicTargetAnimation<T> {
    /**
     * Current value of the animation.
     */
    val value: T
    /**
     * Indicates whether the animation is running.
     */
    val isRunning: Boolean
    /**
     * The target of the current animation. This target will not be the same as the value of the
     * animation, until the animation finishes un-interrupted.
     */
    val targetValue: T

    // TODO: use lambda with default values to combine the following 4 methods when b/134103877
    //  is fixed
    fun animateTo(targetValue: T)
    fun animateTo(targetValue: T, onFinished: (canceled: Boolean) -> Unit)
    fun animateTo(targetValue: T, anim: AnimationBuilder<T> = PhysicsBuilder())
    /**
     * Sets the target value, which effectively starts an animation to change the value from [value]
     * to the target value. If there is already an animation in flight, this method will interrupt
     * the ongoing animation, invoke [onFinished] that is associated with that animation, and start
     * a new animation from the current value to the new target value.
     *
     * @param targetValue The new value to animate to
     * @param anim The animation that will be used to animate from the current value to the new
     *             target value
     * @param onFinished A callback that will be invoked when the animation reaches the target or
     *                   gets canceled.
     */
    fun animateTo(
        targetValue: T,
        anim: AnimationBuilder<T> = PhysicsBuilder(),
        onFinished: (canceled: Boolean) -> Unit
    )

    /**
     * Sets the current value to the target value immediately, without any animation.
     *
     * @param targetValue The new target value to set [value] to.
     */
    fun snapTo(targetValue: T)

    /**
     * Stops any on-going animation. No op if no animation is running. Note that this method does
     * not skip the animation value to its target value. Rather the animation will be stopped in its
     * track.
     */
    fun stop()
}

/**
 * TargetAnimation class defines how to animate to a given target position.
 *
 * @param target Target position for the animation to animate to
 * @param animation The animation that will be used to animate to the target destination. This
 *                  animation defaults to a Spring Animation unless specified.
 */
data class TargetAnimation(
    val target: Float,
    val animation: AnimationBuilder<Float> = PhysicsBuilder()
)
