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
 * [AnimationVector] class that is the base class of [AnimationVector1D], [AnimationVector2D],
 * [AnimationVector3D] and [AnimationVector3D]. In order to animate any arbitrary type, it is
 * required to provide a [TwoWayConverter] that defines how to convert that arbitrary type T to an
 * [AnimationVector], and vice versa. Depending on how many dimensions this type T has, it may need
 * to be converted to any of the subclasses of [AnimationVector]. For example, a position based
 * object should be converted to [AnimationVector2D], whereas an object that describes rectangle
 * bounds should convert to [AnimationVector4D].
 */
sealed class AnimationVector {
    internal abstract fun reset()
}

/**
 * This class defines a 1D vector. It contains only one Float value that is initialized in the
 * constructor.
 *
 * @param initVal initial value to set the [value] field to.
 */
class AnimationVector1D(initVal: Float) : AnimationVector() {
    /**
     * This field holds the only Float value in this [AnimationVector1D] object.
     */
    var value: Float = initVal
        internal set

    // internal
    override fun reset() {
        value = 0f
    }

    internal object arithmetic : Arithmetic<AnimationVector1D> {
        override fun plus(value: AnimationVector1D, other: AnimationVector1D): AnimationVector1D =
            AnimationVector1D(value.value + other.value)

        override fun minus(
            value: AnimationVector1D,
            subtract: AnimationVector1D
        ): AnimationVector1D =
            AnimationVector1D(value.value - subtract.value)

        override fun times(value: AnimationVector1D, multiplier: Float): AnimationVector1D =
            AnimationVector1D(value.value * multiplier)

        override fun interpolate(
            from: AnimationVector1D,
            to: AnimationVector1D,
            fraction: Float
        ): AnimationVector1D =
            AnimationVector1D(lerp(from.value, to.value, fraction))
    }

    override fun toString(): String {
        return "AnimationVector1D: value = $value"
    }

    override fun equals(other: Any?): Boolean =
        other is AnimationVector1D && other.value == value

    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * This class defines a 2D vector that contains two Float values for the two dimensions.
 *
 * @param v1 initial value to set on the first dimension
 * @param v2 initial value to set on the second dimension
 */
class AnimationVector2D(v1: Float, v2: Float) : AnimationVector() {
    /**
     * Float value field for the first dimension of the 2D vector.
     */
    var v1: Float = v1
        internal set
    /**
     * Float value field for the second dimension of the 2D vector.
     */
    var v2: Float = v2
        internal set

    // internal
    override fun reset() {
        v1 = 0f
        v2 = 0f
    }

    internal object arithmetic : Arithmetic<AnimationVector2D> {
        override fun times(value: AnimationVector2D, multiplier: Float) =
            AnimationVector2D(
                v1 = value.v1 * multiplier,
                v2 = value.v2 * multiplier
            )

        override fun minus(value: AnimationVector2D, subtract: AnimationVector2D) =
            AnimationVector2D(
                v1 = value.v1 - subtract.v1,
                v2 = value.v2 - subtract.v2
            )

        override fun plus(value: AnimationVector2D, other: AnimationVector2D): AnimationVector2D =
            AnimationVector2D(
                v1 = value.v1 + other.v1,
                v2 = value.v2 + other.v2
            )

        override fun interpolate(
            from: AnimationVector2D,
            to: AnimationVector2D,
            fraction: Float
        ): AnimationVector2D =
            AnimationVector2D(
                lerp(from.v1, to.v1, fraction),
                lerp(from.v2, to.v2, fraction)
            )
    }

    override fun toString(): String {
        return "AnimationVector2D: v1 = $v1, v2 = $v2"
    }

    override fun equals(other: Any?): Boolean =
        other is AnimationVector2D && other.v1 == v1 && other.v2 == v2

    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * This class defines a 3D vector that contains three Float value fields for the three dimensions.
 *
 * @param v1 initial value to set on the first dimension
 * @param v2 initial value to set on the second dimension
 * @param v3 initial value to set on the third dimension
 */
class AnimationVector3D(v1: Float, v2: Float, v3: Float) : AnimationVector() {
    // Internally mutable, so we don't have to create a number of small objects per anim frame
    /**
     * Float value field for the first dimension of the 3D vector.
     */
    var v1: Float = v1
        internal set
    /**
     * Float value field for the second dimension of the 3D vector.
     */
    var v2: Float = v2
        internal set
    /**
     * Float value field for the third dimension of the 3D vector.
     */
    var v3: Float = v3
        internal set

    // internal
    override fun reset() {
        v1 = 0f
        v2 = 0f
        v3 = 0f
    }

    internal object arithmetic : Arithmetic<AnimationVector3D> {
        override fun times(value: AnimationVector3D, multiplier: Float) =
            AnimationVector3D(
                v1 = value.v1 * multiplier,
                v2 = value.v2 * multiplier,
                v3 = value.v3 * multiplier
            )

        override fun minus(value: AnimationVector3D, subtract: AnimationVector3D) =
            AnimationVector3D(
                v1 = value.v1 - subtract.v1,
                v2 = value.v2 - subtract.v2,
                v3 = value.v3 - subtract.v3
            )

        override fun plus(value: AnimationVector3D, other: AnimationVector3D): AnimationVector3D =
            AnimationVector3D(
                v1 = value.v1 + other.v1,
                v2 = value.v2 + other.v2,
                v3 = value.v3 + other.v3
            )

        override fun interpolate(
            from: AnimationVector3D,
            to: AnimationVector3D,
            fraction: Float
        ): AnimationVector3D =
            AnimationVector3D(
                lerp(from.v1, to.v1, fraction),
                lerp(from.v2, to.v2, fraction),
                lerp(from.v3, to.v3, fraction)
            )
    }

    override fun toString(): String {
        return "AnimationVector3D: v1 = $v1, v2 = $v2, v3 = $v3"
    }

    override fun equals(other: Any?): Boolean =
        other is AnimationVector3D && other.v1 == v1 && other.v2 == v2 && other.v3 == v3

    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * This class defines a 4D vector that contains four Float fields for its four dimensions.
 *
 * @param v1 initial value to set on the first dimension
 * @param v2 initial value to set on the second dimension
 * @param v3 initial value to set on the third dimension
 * @param v4 initial value to set on the fourth dimension
 */
class AnimationVector4D(v1: Float, v2: Float, v3: Float, v4: Float) : AnimationVector() {
    // Internally mutable, so we don't have to create a number of small objects per anim frame
    /**
     * Float value field for the first dimension of the 4D vector.
     */
    var v1: Float = v1
        internal set
    /**
     * Float value field for the second dimension of the 4D vector.
     */
    var v2: Float = v2
        internal set
    /**
     * Float value field for the third dimension of the 4D vector.
     */
    var v3: Float = v3
        internal set
    /**
     * Float value field for the fourth dimension of the 4D vector.
     */
    var v4: Float = v4
        internal set

    override fun reset() {
        v1 = 0f
        v2 = 0f
        v3 = 0f
        v4 = 0f
    }

    internal object arithmetic : Arithmetic<AnimationVector4D> {
        override fun times(value: AnimationVector4D, multiplier: Float) =
            AnimationVector4D(
                v1 = value.v1 * multiplier,
                v2 = value.v2 * multiplier,
                v3 = value.v3 * multiplier,
                v4 = value.v4 * multiplier
            )

        override fun minus(value: AnimationVector4D, subtract: AnimationVector4D) =
            AnimationVector4D(
                v1 = value.v1 - subtract.v1,
                v2 = value.v2 - subtract.v2,
                v3 = value.v3 - subtract.v3,
                v4 = value.v4 - subtract.v4
            )

        override fun plus(value: AnimationVector4D, other: AnimationVector4D): AnimationVector4D =
            AnimationVector4D(
                v1 = value.v1 + other.v1,
                v2 = value.v2 + other.v2,
                v3 = value.v3 + other.v3,
                v4 = value.v4 + other.v4
            )

        override fun interpolate(
            from: AnimationVector4D,
            to: AnimationVector4D,
            fraction: Float
        ): AnimationVector4D =
            AnimationVector4D(
                lerp(from.v1, to.v1, fraction),
                lerp(from.v2, to.v2, fraction),
                lerp(from.v3, to.v3, fraction),
                lerp(from.v4, to.v4, fraction)
            )
    }

    override fun toString(): String {
        return "AnimationVector4D: v1 = $v1, v2 = $v2, v3 = $v3, v4 = $v4"
    }

    override fun equals(other: Any?): Boolean =
        other is AnimationVector4D &&
                other.v1 == v1 &&
                other.v2 == v2 &&
                other.v3 == v3 &&
                other.v4 == v4

    override fun hashCode(): Int = System.identityHashCode(this)
}

internal interface Arithmetic<T> {
    fun plus(value: T, other: T): T
    fun minus(value: T, subtract: T): T
    fun times(value: T, multiplier: Float): T
    fun interpolate(from: T, to: T, fraction: Float): T
}