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
import androidx.animation.ValueHolder
import androidx.compose.Model
import androidx.ui.foundation.gestures.DragValueController
import androidx.ui.foundation.gestures.Draggable
import androidx.ui.lerp

/**
 * Controller that proxy all dragging events to [AnimatedFloat] and [FlingConfig]
 *
 * It makes it possible to have animation support for [Draggable] composable
 * as well as to have fling animation after drag has ended, which is defined by [FlingConfig]
 *
 * @param animatedFloat instance of AnimatedFloat to control
 * @param flingConfig sets behavior of the fling after drag has ended.
 * Default is null, which means no fling will occur no matter the velocity
 */
class AnimatedFloatDragController(
    val animatedFloat: AnimatedFloat,
    private val flingConfig: FlingConfig? = null
) : DragValueController {

    /**
     * whether or not this controller should update value on drag / fling events
     */
    var enabled: Boolean = true

    /**
     * Construct controller that creates and owns AnimatedFloat instance
     *
     * @param initialValue initial value for AnimatedFloat to set it up
     * @param flingConfig sets behavior of the fling after drag has ended.
     * Default is null, which means no fling will occur no matter the velocity
     */
    constructor(initialValue: Float, flingConfig: FlingConfig?)
            : this(AnimatedFloat(AnimValueHolder(initialValue, ::lerp)), flingConfig)

    override val currentValue
        get() = animatedFloat.value

    override fun setBounds(min: Float, max: Float) = animatedFloat.setBounds(min, max)

    override fun onDrag(target: Float) {
        if (enabled) animatedFloat.snapTo(target)
    }

    override fun onDragEnd(velocity: Float, onValueSettled: (Float) -> Unit) {
        if (flingConfig != null && enabled) {
            val config = flingConfig.copy(
                onAnimationEnd =
                { endReason: AnimationEndReason, value: Float, finalVelocity: Float ->
                    if (endReason != AnimationEndReason.Interrupted) onValueSettled(value)
                    flingConfig.onAnimationEnd?.invoke(endReason, value, finalVelocity)
                })
            animatedFloat.fling(config, velocity)
        } else {
            onValueSettled(animatedFloat.value)
        }
    }
}

@Model
private class AnimValueHolder<T>(
    override var value: T,
    override val interpolator: (T, T, Float) -> T
) : ValueHolder<T>
