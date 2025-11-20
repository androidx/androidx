/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.annotation.RestrictTo
import androidx.annotation.Size
import kotlin.math.cos
import kotlin.math.sin

/**
 * An immutable affine transformation in the plane. Individual operations can be constructed with
 * factory methods like [translate] and [rotateDegrees].
 *
 * See [AffineTransform] for more general documentation about how these transforms are represented.
 * See [MutableAffineTransform] for a mutable alternative to this class.
 *
 * @constructor Constructs this transform with 6 float values, starting with the top left corner of
 *   the matrix and proceeding in row-major order. Prefer to create this object with functions that
 *   apply specific transform operations, such as [scale] or [translate], rather than directly
 *   passing in the actual numeric values of this transform. This constructor is useful for when the
 *   values are needed to be provided all at once, for example for serialization. To access these
 *   values in the same order as they are passed in here, use [AffineTransform.getValues]. To
 *   construct this object using an array as input, there is another public constructor for that.
 */
public class ImmutableAffineTransform
public constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override val m00: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override val m10: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override val m20: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override val m01: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override val m11: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override val m21: Float,
) : AffineTransform() {

    /**
     * Like the primary constructor, but accepts a [FloatArray] instead of individual [Float]
     * values.
     */
    public constructor(
        @Size(min = 6) values: FloatArray
    ) : this(values[0], values[1], values[2], values[3], values[4], values[5])

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun toImmutable(): ImmutableAffineTransform = this

    /**
     * Component-wise equality operator for [ImmutableAffineTransform].
     *
     * Due to the propagation floating point precision errors, operations that may be equivalent
     * over the real numbers are not always equivalent for floats, and might return false for
     * [equals] in some cases.
     */
    override fun equals(other: Any?): Boolean =
        other === this || (other is AffineTransform && areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = hash(this)

    override fun toString(): String = "Immutable${string(this)}"

    public companion object {
        /** Returns a transformation that translates by the given [offset] vector. */
        @JvmStatic
        public fun translate(offset: Vec): ImmutableAffineTransform =
            ImmutableAffineTransform(1f, 0f, offset.x, 0f, 1f, offset.y)

        /**
         * Returns a transformation that scales in both the x- and y-direction by the given pair of
         * factors; [xScaleFactor] and [yScaleFactor] respectively, centered about the origin.
         */
        @JvmStatic
        public fun scale(xScaleFactor: Float, yScaleFactor: Float): ImmutableAffineTransform =
            ImmutableAffineTransform(xScaleFactor, 0f, 0f, 0f, yScaleFactor, 0f)

        /**
         * Returns a transformation that scales in both the x- and y-direction by the given
         * [scaleFactor], centered about the origin.
         */
        @JvmStatic
        public fun scale(scaleFactor: Float): ImmutableAffineTransform =
            scale(scaleFactor, scaleFactor)

        /**
         * Returns a transformation that scales in the x-direction by the given factor, centered
         * about the origin.
         */
        @JvmStatic
        public fun scaleX(scaleFactor: Float): ImmutableAffineTransform = scale(scaleFactor, 1f)

        /**
         * Returns a transformation that scales in the y-direction by the given factor, centered
         * about the origin.
         */
        @JvmStatic
        public fun scaleY(scaleFactor: Float): ImmutableAffineTransform = scale(1f, scaleFactor)

        /** Returns a transformation that skews in the x-direction by the given factor. */
        @JvmStatic
        public fun skewX(sx: Float): ImmutableAffineTransform =
            ImmutableAffineTransform(1f, sx, 0f, 0f, 1f, 0f)

        /** Returns a transformation that skews in the y-direction by the given factor. */
        @JvmStatic
        public fun skewY(sy: Float): ImmutableAffineTransform =
            ImmutableAffineTransform(1f, 0f, 0f, sy, 1f, 0f)

        /**
         * Returns a transformation that rotates [degrees] in the direction from the positive x-axis
         * towards the positive y-axis.
         */
        @JvmStatic
        public fun rotateDegrees(@AngleDegreesFloat degrees: Float): ImmutableAffineTransform {
            val radians = Angle.degreesToRadians(degrees)
            val y = sin(radians)
            val x = cos(radians)
            return ImmutableAffineTransform(x, -y, 0f, y, x, 0f)
        }
    }
}
