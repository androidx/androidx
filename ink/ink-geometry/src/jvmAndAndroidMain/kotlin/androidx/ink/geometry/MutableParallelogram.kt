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
 * Mutable parallelogram (i.e. a quadrilateral with parallel sides), defined by its [center],
 * [width], [height], [rotation], and [skew].
 */
@UsedByNative
public class MutableParallelogram
private constructor(
    override var center: MutableVec,
    @FloatRange(from = 0.0) width: Float,
    override var height: Float,
    @FloatRange(from = 0.0, to = 360.0, toInclusive = false)
    @AngleDegreesFloat
    rotationDegrees: Float,
    override var skew: Float,
) : Parallelogram() {

    init {
        check(width >= 0) { "Width must be non-negative." }
        check(rotationDegrees >= 0 && rotationDegrees < Angle.FULL_TURN_DEGREES) {
            "Rotation must be in the range [0, 360)."
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toImmutable(): ImmutableParallelogram =
        ImmutableParallelogram.fromCenterDimensionsRotationInDegreesAndSkew(
            center.toImmutable(),
            width,
            height,
            rotationDegrees,
            skew,
        )

    @get:FloatRange(from = 0.0, to = 360.0, toInclusive = false)
    @get:AngleDegreesFloat
    override var rotationDegrees: Float = rotationDegrees
        set(@FloatRange(from = 0.0, to = 360.0, toInclusive = false) @AngleDegreesFloat value) {
            field = Angle.normalizedDegrees(value)
        }

    @get:FloatRange(from = 0.0)
    override var width: Float = width
        set(@FloatRange(from = 0.0) value) {
            // A [Parallelogram] may *not* have a negative width. If an operation is performed on
            // [Parallelogram] resulting in a negative width, it will be normalized.
            if (value >= 0) {
                field = value
                return
            }
            normalizeAndRun(value, height, rotationDegrees) { w: Float, h: Float, rDegrees: Float ->
                field = w
                height = h
                rotationDegrees = rDegrees
            }
        }

    /**
     * Constructs an empty [MutableParallelogram] with a center at the origin, a zero width, a zero
     * height, zero rotation, and zero skew. This is intended for subsequent population with one of
     * the `populateFrom` methods.
     */
    public constructor() : this(MutableVec(), 0f, 0f, Angle.ZERO_DEGREES, 0f)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @UsedByNative
    public fun setCenterDimensionsRotationInDegreesAndSkew(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        @AngleDegreesFloat rotationDegrees: Float,
        skew: Float,
    ): MutableParallelogram = run {
        normalizeAndRun(width, height, rotationDegrees) { w: Float, h: Float, degrees: Float ->
            this.width = w
            this.height = h
            this.rotationDegrees = degrees
            this.skew = skew
            this.center.x = centerX
            this.center.y = centerY
            this
        }
    }

    /**
     * Fills this [MutableParallelogram] with the same values contained in [input].
     *
     * Returns the modified instance to allow chaining calls.
     *
     * @return `this`
     */
    public fun populateFrom(input: Parallelogram): MutableParallelogram {
        center.x = input.center.x
        center.y = input.center.y
        width = input.width
        height = input.height
        rotationDegrees = input.rotationDegrees
        skew = input.skew
        return this
    }

    override fun equals(other: Any?): Boolean =
        other === this || (other is Parallelogram && Parallelogram.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = hash(this)

    override fun toString(): String = "Mutable${string(this)}"

    /**
     * Populates a [MutableParallelogram] to have a given [center], [width] and [height]. The
     * resulting [Parallelogram] has zero [rotation] and [skew]. If the [width] is less than zero,
     * the [Parallelogram] will be normalized.
     *
     * See the corresponding fields on [Parallelogram] for details about these parameters.
     *
     * @return `this`
     */
    public fun populateFromCenterAndDimensions(
        center: MutableVec,
        @FloatRange(from = 0.0) width: Float,
        height: Float,
    ): MutableParallelogram =
        populateFromCenterDimensionsAndRotationInDegrees(center, width, height, Angle.ZERO_DEGREES)

    /**
     * Populates the [MutableParallelogram] to have a given [center], [width], [height] and
     * [rotationDegrees], with zero [skew]. If the [width] is less than zero or if the
     * [rotationDegrees] is not in the range [0, 360), it will be normalized.
     *
     * See the corresponding fields on [Parallelogram] for details about these parameters.
     *
     * @return `this`
     */
    public fun populateFromCenterDimensionsAndRotationInDegrees(
        center: MutableVec,
        @FloatRange(from = 0.0) width: Float,
        height: Float,
        @FloatRange(from = 0.0, to = 360.0, toInclusive = false)
        @AngleDegreesFloat
        rotationDegrees: Float,
    ): MutableParallelogram =
        populateFromCenterDimensionsRotationInDegreesAndSkew(
            center,
            width,
            height,
            rotationDegrees,
            skew = 0f,
        )

    /**
     * Populates the [MutableParallelogram] to have a given [center], [width], [height],
     * [rotationDegrees] and [skew]. If the [width] is less than zero or if [rotationDegrees] is not
     * within the range [0, 360.0), it will be normalized.
     *
     * See the corresponding fields on [Parallelogram] for details about these parameters.
     *
     * @return `this`
     */
    public fun populateFromCenterDimensionsRotationInDegreesAndSkew(
        center: MutableVec,
        @FloatRange(from = 0.0) width: Float,
        height: Float,
        @FloatRange(from = 0.0, to = 360.0, toInclusive = false)
        @AngleDegreesFloat
        rotationDegrees: Float,
        skew: Float,
    ): MutableParallelogram =
        normalizeAndRun(width, height, rotationDegrees) { w: Float, h: Float, degrees: Float ->
            setCenterDimensionsRotationInDegreesAndSkew(center.x, center.y, w, h, degrees, skew)
        }

    /**
     * Populates the [MutableParallelogram] to be aligned with the [segment] with its bounds
     * [padding] units away from the segment and [skew] of zero.
     *
     * @return `this`
     */
    public fun populateFromSegmentAndPadding(
        segment: Segment,
        padding: Float,
    ): MutableParallelogram =
        normalizeAndRun(
            width = segment.computeLength() + 2 * padding,
            height = 2 * padding,
            rotationDegrees =
                Angle.radiansToDegrees(
                    atan2((segment.start.y - segment.end.y), (segment.start.x - segment.end.x))
                ),
        ) { w: Float, h: Float, rDegrees: Float ->
            setCenterDimensionsRotationInDegreesAndSkew(
                segment.end.x / 2f + segment.start.x / 2f,
                segment.end.y / 2f + segment.start.y / 2f,
                width = w,
                height = h,
                rotationDegrees = rDegrees,
                skew = 0f,
            )
        }

    // Declared as a target for extension functions.
    public companion object
}
