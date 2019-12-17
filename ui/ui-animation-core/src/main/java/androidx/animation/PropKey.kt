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
 * [TwoWayConverter] class contains the definition on how to convert from an arbitrary type [T]
 * to a [AnimationVector], and convert the [AnimationVector] back to the type [T]. This allows animations to run on
 * any type of objects, e.g. position, rectangle, color, etc.
 */
sealed class TwoWayConverter<T, V : AnimationVector> {
    /**
     * Defines how a type [T] should be converted to a Vector type (i.e. [AnimationVector1D], [AnimationVector2D],
     * [AnimationVector3D] or [AnimationVector4D], depends on the dimensions of type T).
     */
    abstract val convertToVector: (T) -> V
    /**
     * Defines how to convert a Vector type (i.e. [AnimationVector1D], [AnimationVector2D], [AnimationVector3D] or [AnimationVector4D],
     * depends on the dimensions of type T) back to type [T].
     */
    abstract val convertFromVector: (V) -> T

    internal abstract fun createNewVector(): V
    internal abstract val arithmetic: Arithmetic<V>
}

/**
 * Type converter to convert type [T] to and from a [AnimationVector1D].
 */
class TypeConverter1D<T>(
    override val convertToVector: (T) -> AnimationVector1D,
    override val convertFromVector: (AnimationVector1D) -> T
) : TwoWayConverter<T, AnimationVector1D>() {
    override fun createNewVector(): AnimationVector1D {
        return AnimationVector1D(0f)
    }

    override val arithmetic: Arithmetic<AnimationVector1D>
        get() = AnimationVector1D.arithmetic
}

/**
 * Type converter to convert type [T] to and from a [AnimationVector2D].
 */
class TypeConverter2D<T>(
    override val convertToVector: (T) -> AnimationVector2D,
    override val convertFromVector: (AnimationVector2D) -> T
) : TwoWayConverter<T, AnimationVector2D>() {
    override fun createNewVector(): AnimationVector2D {
        return AnimationVector2D(0f, 0f)
    }

    override val arithmetic: Arithmetic<AnimationVector2D>
        get() = AnimationVector2D.arithmetic
}

/**
 * Type converter to convert type [T] to and from a [AnimationVector3D].
 */
class TypeConverter3D<T>(
    override val convertToVector: (T) -> AnimationVector3D,
    override val convertFromVector: (AnimationVector3D) -> T
) : TwoWayConverter<T, AnimationVector3D>() {
    override fun createNewVector(): AnimationVector3D {
        return AnimationVector3D(0f, 0f, 0f)
    }

    override val arithmetic: Arithmetic<AnimationVector3D>
        get() = AnimationVector3D.arithmetic
}

/**
 * Type converter to convert type [T] to and from a [AnimationVector4D].
 */
class TypeConverter4D<T>(
    override val convertToVector: (T) -> AnimationVector4D,
    override val convertFromVector: (AnimationVector4D) -> T
) : TwoWayConverter<T, AnimationVector4D>() {
    override fun createNewVector(): AnimationVector4D {
        return AnimationVector4D(0f, 0f, 0f, 0f)
    }

    override val arithmetic: Arithmetic<AnimationVector4D>
        get() = AnimationVector4D.arithmetic
}

/**
 * Property key of [T] type.
 */
interface PropKey<T, V : AnimationVector> {
    val typeConverter: TwoWayConverter<T, V>
}

internal fun lerp(start: Float, stop: Float, fraction: Float) =
    (start * (1 - fraction) + stop * fraction)

internal fun lerp(start: Int, stop: Int, fraction: Float) =
    (start * (1 - fraction) + stop * fraction).toInt()

/**
 * Built-in property key for [Float] properties.
 */
class FloatPropKey : PropKey<Float, AnimationVector1D> {
    override val typeConverter = FloatToVectorConverter
}

/**
 * Built-in property key for [Int] properties.
 */
class IntPropKey : PropKey<Int, AnimationVector1D> {
    override val typeConverter = IntToVectorConverter
}

/**
 * A [TwoWayConverter] that converts [Float] from and to [AnimationVector1D]
 */
val FloatToVectorConverter: TwoWayConverter<Float, AnimationVector1D> =
    TypeConverter1D({ AnimationVector1D(it) }, { it.value })

/**
 * A [TwoWayConverter] that converts [Int] from and to [AnimationVector1D]
 */
val IntToVectorConverter: TwoWayConverter<Int, AnimationVector1D> =
    TypeConverter1D({ AnimationVector1D(it.toFloat()) }, { it.value.toInt() })