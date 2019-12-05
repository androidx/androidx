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
 * animation, and an immutable value converter
 */
interface ValueHolder<T, V : AnimationVector> {
    /**
     * Value of the [ValueHolder]. This value will be updated by subclasses of [BaseAnimatedValue].
     */
    var value: T

    /**
     * A two way type converter that converts from value to [AnimationVector1D],
     * [AnimationVector2D], [AnimationVector3D], or [AnimationVector4D], and vice versa.
     */
    val typeConverter: TwoWayConverter<T, V>
}

/**
 * FloatValueHolder defines a value holder that holds a Float value.
 */
interface FloatValueHolder : ValueHolder<Float, AnimationVector1D> {
    override val typeConverter: TwoWayConverter<Float, AnimationVector1D>
        get() = FloatToVectorConverter
}

/**
 * [ValueHolderImpl] is a data class that defines two fields: value (of type [T]) and a type
 * converter.
 *
 * @param value This value field gets updated during animation
 * @param typeConverter A two way type converter that converts from value to [AnimationVector1D],
 *                     [AnimationVector2D], [Vector3D], or [Vector4D], and vice versa.
 */
data class ValueHolderImpl<T, V : AnimationVector>(
    override var value: T,
    override val typeConverter: TwoWayConverter<T, V>
) : ValueHolder<T, V>
