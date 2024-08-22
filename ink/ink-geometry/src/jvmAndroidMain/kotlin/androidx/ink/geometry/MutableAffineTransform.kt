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

/**
 * An affine transformation in the plane. The transformation can be thought of as a 3x3 matrix:
 * ```
 *   ⎡m00  m10  m20⎤
 *   ⎢m01  m11  m21⎥
 *   ⎣ 0    0    1 ⎦
 * ```
 *
 * Applying the transformation can be thought of as a matrix multiplication, with the
 * to-be-transformed point represented as a column vector with an extra 1:
 * ```
 *   ⎡m00  m10  m20⎤   ⎡x⎤   ⎡m00*x + m10*y + m20⎤
 *   ⎢m01  m11  m21⎥ * ⎢y⎥ = ⎢m01*x + m11*y + m21⎥
 *   ⎣ 0    0    1 ⎦   ⎣1⎦   ⎣         1         ⎦
 * ```
 *
 * Transformations are composed via multiplication. Multiplication is not commutative (i.e. A*B !=
 * B*A), and the left-hand transformation is composed "after" the right hand transformation. E.g.,
 * if you have:
 * ```
 * val rotate = ImmutableAffineTransform.rotate(Angle.degreesToRadians(45))
 * val translate = ImmutableAffineTransform.translate(Vec(10, 0))
 * ```
 *
 * then `rotate * translate` first translates 10 units in the positive x-direction, then rotates 45°
 * about the origin.
 *
 * See [ImmutableAffineTransform] for an immutable alternative to this class.
 *
 * @constructor Constructs this transform with 6 float values, starting with the top left corner of
 *   the matrix and proceeding in row-major order. Prefer to create this object with functions that
 *   apply specific transform operations, such as [populateFromScale] or [populateFromRotate],
 *   rather than directly passing in the actual numeric values of this transform. This constructor
 *   is useful for when the values are needed to be provided all at once, for example for
 *   serialization. To access these values in the same order as they are passed in here, use
 *   [AffineTransform.getValues]. To construct this object using an array as input, there is another
 *   public constructor for that.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
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
     * [populateFromScale] or [populateFromRotate], rather than directly setting the actual numeric
     * values of this transform. This function is useful for when the values are needed to be
     * provided in bulk, for example for serialization.
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
    override fun asImmutable(): ImmutableAffineTransform =
        ImmutableAffineTransform(m00, m10, m20, m01, m11, m21)

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
