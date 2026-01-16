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
import androidx.ink.nativeloader.UsedByNative
import kotlin.math.atan2

/**
 * Immutable parallelogram (i.e. a quadrilateral with parallel sides), defined by its [center],
 * [width], [height], [rotationDegrees], and [skew].
 */
@UsedByNative
public class ImmutableParallelogram
private constructor(
    override val center: ImmutableVec,
    @get:FloatRange(from = 0.0) override val width: Float,
    override val height: Float,
    @get:FloatRange(from = 0.0, to = 360.0, toInclusive = false)
    @AngleDegreesFloat
    override val rotationDegrees: Float,
    override val skew: Float,
) : Parallelogram() {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toImmutable(): ImmutableParallelogram = this

    override fun equals(other: Any?): Boolean =
        other === this || (other is Parallelogram && areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = hash(this)

    override fun toString(): String = "Immutable${string(this)}"

    public companion object {

        /**
         * Constructs an [ImmutableParallelogram] with a given [center], [width] and [height]. The
         * resulting [Parallelogram] has zero [rotationDegrees] and [skew]. If the [width] is less
         * than zero, the Parallelogram will be normalized.
         *
         * See the corresponding fields on [Parallelogram] for details about these parameters.
         */
        @JvmStatic
        public fun fromCenterAndDimensions(
            center: ImmutableVec,
            @FloatRange(from = 0.0) width: Float,
            height: Float,
        ): ImmutableParallelogram =
            fromCenterDimensionsAndRotationInDegrees(center, width, height, Angle.ZERO_DEGREES)

        /**
         * Constructs an [ImmutableParallelogram] with a given [center], [width], [height] and
         * [rotationDegrees]. The resulting [Parallelogram] has zero [skew]. If the [width] is less
         * than zero or if the [rotationDegrees] is not in the range [0, 360), the [Parallelogram]
         * will be normalized.
         *
         * See the corresponding fields on [Parallelogram] for details about these parameters.
         */
        @UsedByNative
        @JvmStatic
        public fun fromCenterDimensionsAndRotationInDegrees(
            center: ImmutableVec,
            @FloatRange(from = 0.0) width: Float,
            height: Float,
            @AngleDegreesFloat rotationDegrees: Float,
        ): ImmutableParallelogram =
            fromCenterDimensionsRotationInDegreesAndSkew(
                center,
                width,
                height,
                rotationDegrees,
                skew = 0f,
            )

        /**
         * Constructs an [ImmutableParallelogram] with a given [center], [width], [height],
         * [rotationDegrees] and [skew]. If the [width] is less than zero or if the
         * [rotationDegrees] is not in the range [0, 360), the [Parallelogram] will be normalized.
         *
         * See the corresponding fields on [Parallelogram] for details about these parameters.
         */
        @UsedByNative
        @JvmStatic
        public fun fromCenterDimensionsRotationInDegreesAndSkew(
            center: ImmutableVec,
            @FloatRange(from = 0.0) width: Float,
            height: Float,
            @AngleDegreesFloat rotationDegrees: Float,
            skew: Float,
        ): ImmutableParallelogram =
            normalizeAndRun(width, height, rotationDegrees) { w: Float, h: Float, r: Float ->
                ImmutableParallelogram(center, w, h, r, skew)
            }

        /**
         * Constructs an [ImmutableParallelogram] that is aligned with the [segment] and whose
         * bounds are [padding] units away from the segment and whose [skew] is zero. This makes it
         * a rectangle, that is axis-aligned only if [segment] is axis-aligned.
         */
        @JvmStatic
        public fun fromSegmentAndPadding(segment: Segment, padding: Float): ImmutableParallelogram =
            normalizeAndRun(
                width = segment.computeLength() + 2 * padding,
                height = 2 * padding,
                rotationDegrees =
                    Angle.radiansToDegrees(
                        atan2((segment.start.y - segment.end.y), (segment.start.x - segment.end.x))
                    ),
            ) { w: Float, h: Float, r: Float ->
                ImmutableParallelogram(
                    center =
                        ImmutableVec(
                            segment.end.x / 2f + segment.start.x / 2f,
                            segment.end.y / 2f + segment.start.y / 2f,
                        ),
                    width = w,
                    height = h,
                    rotationDegrees = r,
                    skew = 0f,
                )
            }
    }
}
