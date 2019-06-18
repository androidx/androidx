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
 * A value holder contains two fields: A mutable value that is expected to change throughout an
 * animation, and an immutable value interpolator.
 */
interface ValueHolder<T> {
    /**
     * Value of the [ValueHolder]. This value will be updated by subclasses of [BaseAnimatedValue].
     */
    var value: T
    /**
     * Value interpolator that will be used to interpolate two values of type [T].
     */
    val interpolator: (start: T, end: T, fraction: Float) -> T
}

/**
 * [ValueHolderImpl] is a data class that defines two fields: value (of type [T]) and a value
 * interpolator.
 *
 * @param value This value field gets updated during animation
 * @param interpolator Value interpolator defines how two values of type [T] should be interpolated.
 */
data class ValueHolderImpl<T>(
    override var value: T,
    override val interpolator: (T, T, Float) -> T
) : ValueHolder<T>
