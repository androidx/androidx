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
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.remember
import androidx.ui.foundation.ValueHolder
import androidx.ui.foundation.gestures.Draggable

/**
 * Value holder that allows to animate value that it holds.
 *
 * The main purpose of this class is to represent state of [Draggable] and provide both
 * drag value retrieval and animation/fling capabilities by implementing
 * [ValueHolder] and [DynamicTargetAnimation] interfaces respectively.
 */
@Model
class AnimatedValueHolder(initial: Float) : ValueHolder<Float> {

    @Suppress("DEPRECATION")
    val animatedFloat = object : AnimatedFloat() {
        val holder = ListeneableValueHolder(initial, { this@AnimatedValueHolder.value = it })
        override var value: Float
            set(value) = holder.onValueChanged(value)
            get() = holder.current
    }

    /**
     * Sets up the bounds that this value should be constrained to.
     *
     * @param min Lower bound of the value. Defaults to [Float.NEGATIVE_INFINITY]
     * @param max Upper bound of the value. Defaults to [Float.POSITIVE_INFINITY]
     */
    fun setBounds(min: Float = Float.NEGATIVE_INFINITY, max: Float = Float.POSITIVE_INFINITY) {
        animatedFloat.setBounds(min, max)
    }

    /**
     * Starts a fling animation with the specified starting velocity and fling configuration.
     *
     * @param config configuration that specifies fling behaviour
     * @param startVelocity Starting velocity of the fling animation
     */
    fun fling(config: FlingConfig, startVelocity: Float) {
        animatedFloat.fling(config, startVelocity)
    }

    /**
     * current value of this holder
     */
    override var value: Float = initial
        private set
}

/**
 * Effect to construct and memorize value holder
 *
 * This effects sets initial value inside this holder to [initial], and can
 * rewrite min and max bound without reconstructing the whole holder if they change.
 *
 * @param initial initial value to put inside the value holder
 * @param minBound minimal bound for the value inside the holder
 * @param maxBound maximal bound for the value inside the holder
 */
@Composable
fun animatedDragValue(initial: Float, minBound: Float, maxBound: Float): AnimatedValueHolder {
    val vh = remember { AnimatedValueHolder(initial) }
    vh.setBounds(minBound, maxBound)
    return vh
}

private class ListeneableValueHolder(
    var current: Float,
    var onValueChanged: (Float) -> Unit
) : androidx.animation.ValueHolder<Float> {
    override var value: Float
        get() = current
        set(value) {
            current = value
            onValueChanged(value)
        }
}