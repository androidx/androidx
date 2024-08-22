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

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo

/**
 * Immutable parallelogram (i.e. a quadrilateral with parallel sides), defined by its [center],
 * [width], [height], [rotation], and [shearFactor].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class ImmutableParallelogram
private constructor(
    override val center: ImmutableVec,
    override val width: Float,
    override val height: Float,
    @AngleRadiansFloat override val rotation: Float,
    override val shearFactor: Float,
) : Parallelogram() {

    override fun equals(other: Any?): Boolean =
        other === this || (other is Parallelogram && areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = hash(this)

    override fun toString(): String = "Immutable${string(this)}"

    public companion object {

        /**
         * Constructs an [ImmutableParallelogram] with a given [center], [width] and [height]. The
         * resulting [Parallelogram] has zero [rotation] and [shearFactor]. If the [width] is less
         * than zero, the Parallelogram will be normalized.
         */
        @JvmStatic
        public fun fromCenterAndDimensions(
            center: ImmutableVec,
            @FloatRange(from = 0.0) width: Float,
            height: Float,
        ): ImmutableParallelogram =
            normalizeAndRun(width, height, rotation = Angle.ZERO) { w: Float, h: Float, r: Float ->
                ImmutableParallelogram(center, w, h, r, shearFactor = 0f)
            }

        /**
         * Constructs an [ImmutableParallelogram] with a given [center], [width], [height] and
         * [rotation]. The resulting [Parallelogram] has zero [shearFactor]. If the [width] is less
         * than zero or if the [rotation] is not in the range [0, 2π), the [Parallelogram] will be
         * normalized.
         */
        @JvmStatic
        public fun fromCenterDimensionsAndRotation(
            center: ImmutableVec,
            @FloatRange(from = 0.0) width: Float,
            height: Float,
            @AngleRadiansFloat rotation: Float,
        ): ImmutableParallelogram =
            normalizeAndRun(width, height, rotation) { w: Float, h: Float, r: Float ->
                ImmutableParallelogram(center, w, h, r, shearFactor = 0f)
            }

        /**
         * Constructs an [ImmutableParallelogram] with a given [center], [width], [height],
         * [rotation] and [shearFactor]. If the [width] is less than zero or if the [rotation] is
         * not in the range [0, 2π), the [Parallelogram] will be normalized.
         */
        @JvmStatic
        public fun fromCenterDimensionsRotationAndShear(
            center: ImmutableVec,
            @FloatRange(from = 0.0) width: Float,
            height: Float,
            @AngleRadiansFloat rotation: Float,
            shearFactor: Float,
        ): ImmutableParallelogram =
            normalizeAndRun(width, height, rotation) { w: Float, h: Float, r: Float ->
                ImmutableParallelogram(center, w, h, r, shearFactor)
            }
    }
}
