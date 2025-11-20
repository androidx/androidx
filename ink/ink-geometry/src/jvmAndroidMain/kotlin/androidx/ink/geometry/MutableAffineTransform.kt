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
 * A mutable affine transformation in the plane. Individual operations can be populated with methods
 * like [populateFromTranslation] and [populateFromRotationDegrees].
 *
 * See [AffineTransform] for more general documentation about how these transforms are represented.
 * See [ImmutableAffineTransform] for an immutable alternative to this class.
 *
 * @constructor Constructs this transform with 6 float values, starting with the top left corner of
 *   the matrix and proceeding in row-major order. Prefer to create this object with functions that
 *   apply specific transform operations, such as [populateFromScale] or
 *   [populateFromRotationDegrees], rather than directly passing in the actual numeric values of
 *   this transform. This constructor is useful for when the values are needed to be provided all at
 *   once, for example for serialization. To access these values in the same order as they are
 *   passed in here, use [AffineTransform.getValues]. To construct this object using an array as
 *   input, there is another public constructor for that.
 */
public class MutableAffineTransform
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override var m00: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override var m10: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override var m20: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override var m01: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override var m11: Float,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override var m21: Float,
) : AffineTransform() {

    /**
     * Constructs an identity [MutableAffineTransform]:
     * ```
     *   ⎡1  0  0⎤
     *   ⎢0  1  0⎥
     *   ⎣0  0  1⎦
     * ```
     *
     * This is useful when pre-allocating a scratch instance to be filled later.
     */
    public constructor() : this(1f, 0f, 0f, 0f, 1f, 0f)

    /**
     * Populates this transform with the given values, starting with the top left corner of the
     * matrix and proceeding in row-major order.
     *
     * Prefer to modify this object with functions that apply specific transform operations, such as
     * [populateFromScale] or [populateFromRotationDegrees], rather than directly setting the actual
     * numeric values of this transform. This function is useful for when the values are needed to
     * be provided in bulk, for example for serialization.
     *
     * To access these values in the same order as they are set here, use
     * [AffineTransform.getValues].
     */
    public fun setValues(m00: Float, m10: Float, m20: Float, m01: Float, m11: Float, m21: Float) {
        this.m00 = m00
        this.m10 = m10
        this.m20 = m20
        this.m01 = m01
        this.m11 = m11
        this.m21 = m21
    }

    /** Like [setValues], but accepts a [FloatArray] instead of individual float values. */
    public fun setValues(@Size(min = 6) values: FloatArray) {
        m00 = values[0]
        m10 = values[1]
        m20 = values[2]
        m01 = values[3]
        m11 = values[4]
        m21 = values[5]
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toImmutable(): ImmutableAffineTransform =
        if (this == AffineTransform.IDENTITY) {
            AffineTransform.IDENTITY
        } else {
            ImmutableAffineTransform(m00, m10, m20, m01, m11, m21)
        }

    /**
     * Fills this [MutableAffineTransform] with the same values contained in [input].
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFrom(input: AffineTransform): MutableAffineTransform {
        m00 = input.m00
        m10 = input.m10
        m20 = input.m20
        m01 = input.m01
        m11 = input.m11
        m21 = input.m21
        return this
    }

    /**
     * Fills this [MutableAffineTransform] with an identity transformation, which maps a point to
     * itself, i.e. it leaves it unchanged.
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFromIdentity(): MutableAffineTransform {
        m00 = 1f
        m10 = 0f
        m20 = 0f
        m01 = 0f
        m11 = 1f
        m21 = 0f
        return this
    }

    /**
     * Fills this [MutableAffineTransform] with a transformation that translates by the given
     * [offset] vector.
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFromTranslation(offset: Vec): MutableAffineTransform {
        m00 = 1f
        m10 = 0f
        m20 = offset.x
        m01 = 0f
        m11 = 1f
        m21 = offset.y
        return this
    }

    /**
     * Fills this [MutableAffineTransform] with a transformation that scales in both the x and y
     * direction by the given [scaleFactor], centered about the origin.
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFromScale(scaleFactor: Float): MutableAffineTransform {
        m00 = scaleFactor
        m10 = 0f
        m20 = 0f
        m01 = 0f
        m11 = scaleFactor
        m21 = 0f
        return this
    }

    /**
     * Fills this [MutableAffineTransform] with a transformation that scales in both the x- and
     * y-direction by the given pair of factors; [xScaleFactor] and [yScaleFactor] respectively,
     * centered about the origin.
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFromScale(xScaleFactor: Float, yScaleFactor: Float): MutableAffineTransform {
        m00 = xScaleFactor
        m10 = 0f
        m20 = 0f
        m01 = 0f
        m11 = yScaleFactor
        m21 = 0f
        return this
    }

    /**
     * Fills this [MutableAffineTransform] with a transformation that scales in the x-direction by
     * the given factor, centered about the origin.
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFromScaleX(scaleFactor: Float): MutableAffineTransform {
        m00 = scaleFactor
        m10 = 0f
        m20 = 0f
        m01 = 0f
        m11 = 1f
        m21 = 0f
        return this
    }

    /**
     * Fills this [MutableAffineTransform] with a transformation that scales in the y-direction by
     * the given factor, centered about the origin.
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFromScaleY(scaleFactor: Float): MutableAffineTransform {
        m00 = 1f
        m10 = 0f
        m20 = 0f
        m01 = 0f
        m11 = scaleFactor
        m21 = 0f
        return this
    }

    /**
     * Fills this [MutableAffineTransform] with a transformation that skews in the x-direction by
     * the given factor.
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFromSkewX(sx: Float): MutableAffineTransform {
        m00 = 1f
        m10 = sx
        m20 = 0f
        m01 = 0f
        m11 = 1f
        m21 = 0f
        return this
    }

    /**
     * Fills this [MutableAffineTransform] with a transformation that skews in the y-direction by
     * the given factor.
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFromSkewY(sy: Float): MutableAffineTransform {
        m00 = 1f
        m10 = 0f
        m20 = 0f
        m01 = sy
        m11 = 1f
        m21 = 0f
        return this
    }

    /**
     * Fills this [MutableAffineTransform] with a transformation that rotates [degrees] in the
     * direction from the positive x-axis towards the positive y-axis.
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFromRotationDegrees(
        @AngleDegreesFloat degrees: Float
    ): MutableAffineTransform {
        val radians = Angle.degreesToRadians(degrees)
        val y = sin(radians)
        val x = cos(radians)
        m00 = x
        m10 = -y
        m20 = 0f
        m01 = y
        m11 = x
        m21 = 0f
        return this
    }

    /**
     * Component-wise equality operator for [MutableAffineTransform].
     *
     * Due to the propagation floating point precision errors, operations that may be equivalent
     * over the real numbers are not always equivalent for floats, and might return false for
     * [equals] in some cases.
     */
    override fun equals(other: Any?): Boolean =
        other === this || (other is AffineTransform && areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = hash(this)

    override fun toString(): String = "Mutable${string(this)}"
}
