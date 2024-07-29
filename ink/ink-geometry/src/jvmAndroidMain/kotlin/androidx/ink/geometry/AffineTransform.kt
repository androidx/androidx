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
import kotlin.jvm.JvmField

/**
 * An affine transformation in the plane. The transformation can be thought of as a 3x3 matrix:
 * ```
 *   ⎡a  b  c⎤
 *   ⎢d  e  f⎥
 *   ⎣0  0  1⎦
 * ```
 *
 * Applying the transformation can be thought of as a matrix multiplication, with the
 * to-be-transformed point represented as a column vector with an extra 1:
 * ```
 *   ⎡a  b  c⎤   ⎡x⎤   ⎡a*x + b*y + c⎤
 *   ⎢d  e  f⎥ * ⎢y⎥ = ⎢d*x + e*y + f⎥
 *   ⎣0  0  1⎦   ⎣1⎦   ⎣      1      ⎦
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
 * then the `rotate * translate` first translates 10 units in the positive x-direction, then rotates
 * 90° about the origin.
 *
 * This class follows AndroidX guidelines ({@link http://go/androidx-api-guidelines#kotlin-data}) to
 * avoid Kotlin data classes.
 *
 * See [MutableAffineTransform] and [ImmutableAffineTransform] for implementations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public interface AffineTransform {
    public val a: Float
    public val b: Float
    public val c: Float
    public val d: Float
    public val e: Float
    public val f: Float

    /**
     * Returns an immutable copy of this object. This will return itself if called on an immutable
     * instance.
     */
    public fun asImmutable(): ImmutableAffineTransform {
        return ImmutableAffineTransform(
            a = this.a,
            b = this.b,
            c = this.c,
            d = this.d,
            e = this.e,
            f = this.f,
        )
    }

    /**
     * Populates [output] with the inverse of the [AffineTransform]. The same MutableAffineTransform
     * can be used as the output to avoid additional allocations.
     */
    public fun populateInverse(output: MutableAffineTransform) {
        val determinant = a * e - b * d
        require(determinant != 0F) {
            "The inverse of the AffineTransform cannot be found because the determinant is 0."
        }
        val newA = e / determinant
        val newB = -b / determinant
        val newC = (b * f - c * e) / determinant
        val newD = -d / determinant
        val newE = a / determinant
        val newF = (c * d - a * f) / determinant
        output.a = newA
        output.b = newB
        output.c = newC
        output.d = newD
        output.e = newE
        output.f = newF
    }

    private fun transformX(x: Float, y: Float): Float = a * x + b * y + c

    private fun transformY(x: Float, y: Float): Float = d * x + e * y + f

    /**
     * Apply the [AffineTransform] to the [Vec] and store the result in the [MutableVec]. The same
     * [MutableVec] can be used as both the input and output to avoid additional allocations.
     */
    public fun applyTransform(vec: Vec, output: MutableVec) {
        val newX = transformX(vec.x, vec.y)
        output.y = transformY(vec.x, vec.y)
        output.x = newX
    }

    /**
     * Apply the [AffineTransform] to the [Segment] and store the result in the [MutableSegment].
     * The same [MutableSegment] can be used as both the input and output to avoid additional
     * allocations.
     */
    public fun applyTransform(segment: Segment, output: MutableSegment) {
        output.start(
            transformX(segment.start.x, segment.start.y),
            transformY(segment.start.x, segment.start.y),
        )
        output.end(
            transformX(segment.end.x, segment.end.y),
            transformY(segment.end.x, segment.end.y)
        )
    }

    /**
     * Apply the [AffineTransform] to the [Triangle] and store the result in the [MutableTriangle].
     * The same [MutableTriangle] can be used as both the input and output to avoid additional
     * allocations.
     */
    public fun applyTransform(triangle: Triangle, output: MutableTriangle) {
        output.p0(
            transformX(triangle.p0.x, triangle.p0.y),
            transformY(triangle.p0.x, triangle.p0.y)
        )
        output.p1(
            transformX(triangle.p1.x, triangle.p1.y),
            transformY(triangle.p1.x, triangle.p1.y)
        )
        output.p2(
            transformX(triangle.p2.x, triangle.p2.y),
            transformY(triangle.p2.x, triangle.p2.y)
        )
    }

    /**
     * Apply the [AffineTransform] to the [Box] and store the result in the [MutableParallelogram].
     * This is the only Apply function where the input cannot also be the output, as applying an
     * Affine Transform to a Box makes a Parallelogram.
     */
    public fun applyTransform(box: Box, outputParallelogram: MutableParallelogram) {
        AffineTransformHelper.nativeApplyParallelogram(
            affineTransformA = a,
            affineTransformB = b,
            affineTransformC = c,
            affineTransformD = d,
            affineTransformE = e,
            affineTransformF = f,
            parallelogramCenterX = (box.xMin + box.xMax) / 2,
            parallelogramCenterY = (box.yMin + box.yMax) / 2,
            parallelogramWidth = box.width,
            parallelogramHeight = box.height,
            parallelogramRotation = 0f,
            parallelogramShearFactor = 0f,
            out = outputParallelogram,
        )
    }

    /**
     * Apply the [AffineTransform] to the [Parallelogram] and store the result in the
     * [MutableParallelogram]. The same [MutableParallelogram] can be used as both the input and
     * output to avoid additional allocations.
     */
    public fun applyTransform(
        parallelogram: Parallelogram,
        outputParallelogram: MutableParallelogram,
    ) {
        AffineTransformHelper.nativeApplyParallelogram(
            affineTransformA = a,
            affineTransformB = b,
            affineTransformC = c,
            affineTransformD = d,
            affineTransformE = e,
            affineTransformF = f,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramRotation = parallelogram.rotation,
            parallelogramShearFactor = parallelogram.shearFactor,
            out = outputParallelogram,
        )
    }

    public companion object {
        /**
         * Constant representing an identity transformation, which maps a point to itself, i.e. it
         * leaves it unchanged.
         */
        @JvmField
        public val IDENTITY: ImmutableAffineTransform =
            ImmutableAffineTransform(a = 1f, b = 0f, c = 0f, d = 0f, e = 1f, f = 0f)

        /**
         * Returns true if [first] and [second] have the same values for all properties of
         * [AffineTransform].
         */
        internal fun areEquivalent(first: AffineTransform, second: AffineTransform): Boolean =
            first.a == second.a &&
                first.b == second.b &&
                first.c == second.c &&
                first.d == second.d &&
                first.e == second.e &&
                first.f == second.f

        /** Returns a hash code for [affineTransform] using its [AffineTransform] properties. */
        internal fun hash(affineTransform: AffineTransform): Int =
            affineTransform.run {
                var result = a.hashCode()
                result = 31 * result + b.hashCode()
                result = 31 * result + c.hashCode()
                result = 31 * result + d.hashCode()
                result = 31 * result + e.hashCode()
                result = 31 * result + f.hashCode()
                return result
            }

        /**
         * Returns a string representation for [affineTransform] using its [AffineTransform]
         * properties.
         */
        internal fun string(affineTransform: AffineTransform): String =
            affineTransform.run { "AffineTransform(a=$a, b=$b, c=$c, d=$d, e=$e, f=$f)" }
    }
}
