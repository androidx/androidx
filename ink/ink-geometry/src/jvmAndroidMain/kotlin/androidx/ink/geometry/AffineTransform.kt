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
import kotlin.jvm.JvmField

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
 * [ImmutableAffineTransform] and [MutableAffineTransform] are the two concrete implementations of
 * this.
 */
public abstract class AffineTransform internal constructor() {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public abstract val m00: Float
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public abstract val m10: Float
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public abstract val m20: Float
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public abstract val m01: Float
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public abstract val m11: Float
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public abstract val m21: Float

    /**
     * Returns an immutable copy of this object. This will return itself if called on an immutable
     * instance.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract fun asImmutable(): ImmutableAffineTransform

    /**
     * Populates [outAffineTransform] with the inverse of this [AffineTransform]. The same
     * [MutableAffineTransform] instance can be used as the output to avoid additional allocations.
     * Returns [outAffineTransform].
     */
    public fun computeInverse(outAffineTransform: MutableAffineTransform): MutableAffineTransform {
        val determinant = m00 * m11 - m10 * m01
        require(determinant != 0F) {
            "The inverse of the AffineTransform cannot be found because the determinant is 0."
        }
        val newM00 = m11 / determinant
        val newM10 = -m10 / determinant
        val newM20 = (m10 * m21 - m20 * m11) / determinant
        val newM01 = -m01 / determinant
        val newM11 = m00 / determinant
        val newM21 = (m20 * m01 - m00 * m21) / determinant
        outAffineTransform.setValues(newM00, newM10, newM20, newM01, newM11, newM21)
        return outAffineTransform
    }

    private fun applyTransformX(x: Float, y: Float): Float = m00 * x + m10 * y + m20

    private fun applyTransformY(x: Float, y: Float): Float = m01 * x + m11 * y + m21

    /**
     * Apply the [AffineTransform] to the [Vec] and store the result in the [MutableVec]. The same
     * [MutableVec] can be used as both the input and output to avoid additional allocations.
     * Returns [outVec].
     */
    public fun applyTransform(vec: Vec, outVec: MutableVec): MutableVec {
        val newX = applyTransformX(vec.x, vec.y)
        outVec.y = applyTransformY(vec.x, vec.y)
        outVec.x = newX
        return outVec
    }

    /**
     * Apply the [AffineTransform] to the [Segment] and store the result in the [MutableSegment].
     * The same [MutableSegment] can be used as both the input and output to avoid additional
     * allocations. Returns [outSegment].
     */
    public fun applyTransform(segment: Segment, outSegment: MutableSegment): MutableSegment {
        applyTransform(segment.start, outSegment.start)
        applyTransform(segment.end, outSegment.end)
        return outSegment
    }

    /**
     * Apply the [AffineTransform] to the [Triangle] and store the result in the [MutableTriangle].
     * The same [MutableTriangle] can be used as both the input and output to avoid additional
     * allocations. Returns [outTriangle].
     */
    public fun applyTransform(triangle: Triangle, outTriangle: MutableTriangle): MutableTriangle {
        applyTransform(triangle.p0, outTriangle.p0)
        applyTransform(triangle.p1, outTriangle.p1)
        applyTransform(triangle.p2, outTriangle.p2)
        return outTriangle
    }

    /**
     * Apply the [AffineTransform] to the [Box] and store the result in the [MutableParallelogram].
     * This is the only Apply function where the input cannot also be the output, as applying an
     * Affine Transform to a Box makes a Parallelogram.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun applyTransform(
        box: Box,
        outParallelogram: MutableParallelogram,
    ): MutableParallelogram {
        AffineTransformHelper.nativeApplyParallelogram(
            affineTransformA = m00,
            affineTransformB = m10,
            affineTransformC = m20,
            affineTransformD = m01,
            affineTransformE = m11,
            affineTransformF = m21,
            parallelogramCenterX = (box.xMin + box.xMax) / 2,
            parallelogramCenterY = (box.yMin + box.yMax) / 2,
            parallelogramWidth = box.width,
            parallelogramHeight = box.height,
            parallelogramRotation = 0f,
            parallelogramShearFactor = 0f,
            out = outParallelogram,
        )
        return outParallelogram
    }

    /**
     * Apply the [AffineTransform] to the [Parallelogram] and store the result in the
     * [MutableParallelogram]. The same [MutableParallelogram] can be used as both the input and
     * output to avoid additional allocations.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun applyTransform(
        parallelogram: Parallelogram,
        outParallelogram: MutableParallelogram,
    ): MutableParallelogram {
        AffineTransformHelper.nativeApplyParallelogram(
            affineTransformA = m00,
            affineTransformB = m10,
            affineTransformC = m20,
            affineTransformD = m01,
            affineTransformE = m11,
            affineTransformF = m21,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramRotation = parallelogram.rotation,
            parallelogramShearFactor = parallelogram.shearFactor,
            out = outParallelogram,
        )
        return outParallelogram
    }

    /**
     * Populates the first 6 elements of [outArray] with the values of this transform, starting with
     * the top left corner of the matrix and proceeding in row-major order.
     *
     * In performance-sensitive code, prefer to pass in an array that has already been allocated and
     * is being reused, rather than relying on the default behavior of allocating a new instance for
     * each call.
     *
     * Prefer to apply this transform to an object, such as with [applyTransform], rather than
     * accessing the actual numeric values of this transform. This function is useful for when the
     * values are needed in bulk but not to apply a transform, for example for serialization.
     *
     * To set these values on a transform in the same order that they are retrieved here, use the
     * [ImmutableAffineTransform] constructor or use [MutableAffineTransform.setValues].
     */
    @JvmOverloads
    @Size(min = 6)
    @Suppress("ArrayReturn") // Returning the input value for chaining.
    public fun getValues(@Size(min = 6) outArray: FloatArray = FloatArray(6)): FloatArray {
        outArray[0] = m00
        outArray[1] = m10
        outArray[2] = m20
        outArray[3] = m01
        outArray[4] = m11
        outArray[5] = m21
        return outArray
    }

    public companion object {
        /**
         * Constant representing an identity transformation, which maps a point to itself, i.e. it
         * leaves it unchanged.
         */
        @JvmField
        public val IDENTITY: ImmutableAffineTransform =
            ImmutableAffineTransform(1f, 0f, 0f, 0f, 1f, 0f)

        /**
         * Returns true if [first] and [second] have the same values for all properties of
         * [AffineTransform].
         */
        internal fun areEquivalent(first: AffineTransform, second: AffineTransform): Boolean =
            first.m00 == second.m00 &&
                first.m10 == second.m10 &&
                first.m20 == second.m20 &&
                first.m01 == second.m01 &&
                first.m11 == second.m11 &&
                first.m21 == second.m21

        /** Returns a hash code for [affineTransform] using its [AffineTransform] properties. */
        internal fun hash(affineTransform: AffineTransform): Int =
            affineTransform.run {
                var result = m00.hashCode()
                result = 31 * result + m10.hashCode()
                result = 31 * result + m20.hashCode()
                result = 31 * result + m01.hashCode()
                result = 31 * result + m11.hashCode()
                result = 31 * result + m21.hashCode()
                return result
            }

        /**
         * Returns a string representation for [affineTransform] using its [AffineTransform]
         * properties.
         */
        internal fun string(affineTransform: AffineTransform): String =
            affineTransform.run {
                "AffineTransform(m00=$m00, m10=$m10, m20=$m20, m01=$m01, m11=$m11, m21=$m21)"
            }
    }
}
