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
 * See [MutableAffineTransform] for mutable alternative to this class.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class ImmutableAffineTransform(
    override val a: Float,
    override val b: Float,
    override val c: Float,
    override val d: Float,
    override val e: Float,
    override val f: Float,
) : AffineTransform {

    override public fun asImmutable(): ImmutableAffineTransform = this

    /**
     * Component-wise equality operator for [ImmutableAffineTransform].
     *
     * Due to the propagation floating point precision errors, operations that may be equivalent
     * over the real numbers are not always equivalent for floats, and might return false for
     * [equals] in some cases.
     */
    override fun equals(other: Any?): Boolean =
        other === this || (other is AffineTransform && AffineTransform.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = AffineTransform.hash(this)

    override fun toString(): String = "Immutable${AffineTransform.hash(this)}"

    public companion object {
        /** Returns a transformation that translates by the given [offset] vector. */
        @JvmStatic
        public fun translate(offset: Vec): ImmutableAffineTransform =
            ImmutableAffineTransform(a = 1f, b = 0f, c = offset.x, d = 0f, e = 1f, f = offset.y)

        /**
         * Returns a transformation that scales in both the x- and y-direction by the given pair of
         * factors; [xScaleFactor] and [yScaleFactor] respectively, centered about the origin.
         */
        @JvmStatic
        public fun scale(xScaleFactor: Float, yScaleFactor: Float): ImmutableAffineTransform =
            ImmutableAffineTransform(
                a = xScaleFactor,
                b = 0f,
                c = 0f,
                d = 0f,
                e = yScaleFactor,
                f = 0f
            )

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
    }
}
