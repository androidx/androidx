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

import androidx.ui.core.Dp
import androidx.ui.core.Px
import androidx.ui.core.PxPosition
import androidx.ui.core.lerp
import androidx.ui.lerp
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp

internal open class StateImpl<T>(val name: T) : MutableTransitionState, TransitionState {

    internal val props: MutableMap<PropKey<Any>, Any> = mutableMapOf()

    override operator fun <T> set(propKey: PropKey<T>, prop: T) {
        @Suppress("UNCHECKED_CAST")
        propKey as PropKey<Any>
        if (props[propKey] != null) {
            throw IllegalArgumentException("prop name $propKey already exists")
        }

        props[propKey] = prop as Any
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun <T> get(propKey: PropKey<T>): T {
        propKey as PropKey<Any>
        return props[propKey] as T
    }
}

/**
 * [TransitionState] holds a number of property values. The value of a property can be queried via
 * [get], providing its property key.
 */
interface TransitionState {
    operator fun <T> get(propKey: PropKey<T>): T
}

/**
 * [MutableTransitionState] is used in [TransitionDefinition] for constructing various
 * [TransitionState]s with corresponding properties and their values.
 */
interface MutableTransitionState {
    operator fun <T> set(propKey: PropKey<T>, prop: T)
}

/**
 * Property key of [T] type.
 */
interface PropKey<T> {
    fun interpolate(a: T, b: T, fraction: Float): T
}

/**
 * Built-in property key for [Color] properties.
 */
class ColorPropKey : PropKey<Color> {
    override fun interpolate(a: Color, b: Color, fraction: Float): Color {
        return lerp(a, b, fraction)
    }
}

/**
 * Built-in property key for [Float] properties.
 */
class FloatPropKey : PropKey<Float> {
    override fun interpolate(a: Float, b: Float, fraction: Float) =
        lerp(a, b, fraction)
}

// TODO: refactor out the entirely independent bit of the animation engine
/**
 * Built-in property key for [Int] properties.
 */
class IntPropKey : PropKey<Int> {
    override fun interpolate(a: Int, b: Int, fraction: Float) =
        lerp(a, b, fraction).toInt()
}

/**
 * Built-in property key for [Px] properties.
 */
class PxPropKey : PropKey<Px> {
    override fun interpolate(a: Px, b: Px, fraction: Float): Px =
        lerp(a, b, fraction)
}

/**
 * Built-in property key for [Dp] properties.
 */
class DpPropKey : PropKey<Dp> {
    override fun interpolate(a: Dp, b: Dp, fraction: Float): Dp =
        lerp(a, b, fraction)
}

/**
 * Built-in property key for [PxPosition] properties.
 */
class PxPositionPropKey : PropKey<PxPosition> {
    override fun interpolate(a: PxPosition, b: PxPosition, fraction: Float): PxPosition =
        lerp(a, b, fraction)
}