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
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/**
 * A helper class for accumulating the minimum bounding boxes of zero or more geometry objects. In
 * colloquial terms, this can be used to find the smallest Box that contains a set of objects.
 */
@UsedByNative
public class BoxAccumulator {
    /**
     * The bounds, which are valid only if [hasBounds] is `true`. When [hasBounds] is `false`, this
     * object exists (so that allocating a new instance of an underlying [MutableBox] doesn't happen
     * at an inconvenient time), but its data is invalid and is waiting to be overwritten to
     * represent a non-empty region.
     */
    private var _bounds: MutableBox

    /** `true` if [_bounds] holds valid data, and `false` otherwise. */
    private var hasBounds: Boolean

    /** The currently accumulated bounding box, which may be empty */
    public val box: Box?
        get() = if (hasBounds) _bounds else null

    private constructor(hasBounds: Boolean, bounds: MutableBox) {
        this.hasBounds = hasBounds
        this._bounds = bounds
    }

    /** Constructs an empty [BoxAccumulator] */
    public constructor() : this(false, MutableBox())

    /** Constructs a [BoxAccumulator], populating the to accumulated bounding box from [box]. */
    public constructor(
        box: Box
    ) : this(
        true,
        MutableBox()
            .populateFromTwoPoints(
                ImmutableVec(box.xMin, box.yMin),
                ImmutableVec(box.xMax, box.yMax)
            ),
    )

    /**
     * Returns true if this [BoxAccumulator] is not empty; this is equivalent to checking if the
     * [box] property is null.
     *
     * Note that a zero-area [Box] is not considered empty, as a [Box] contains its boundary; so
     * [isEmpty] will return false even if you have added only a single point to the
     * [BoxAccumulator].
     */
    public fun isEmpty(): Boolean = !hasBounds

    /** Populates this [BoxAccumulator] with the same values contained in [input]. */
    public fun populateFrom(input: BoxAccumulator): BoxAccumulator {
        reset().add(input)
        return this
    }

    /** Reset this object to have no bounds. Returns the same instance to chain function calls. */
    @UsedByNative
    public fun reset(): BoxAccumulator {
        hasBounds = false
        _bounds.setXBounds(Float.NaN, Float.NaN).setYBounds(Float.NaN, Float.NaN)
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [other]. If
     * [other] is null, this is a no-op.
     *
     * @return `this`
     */
    public fun add(other: BoxAccumulator?): BoxAccumulator {
        BoxAccumulatorNative.nativeAddOptionalBox(
            envelopeHasBounds = hasBounds,
            envelopeBoundsXMin = _bounds.xMin,
            envelopeBoundsYMin = _bounds.yMin,
            envelopeBoundsXMax = _bounds.xMax,
            envelopeBoundsYMax = _bounds.yMax,
            boxHasBounds = other?.box != null,
            boxXMin = other?.box?.xMin ?: Float.NaN,
            boxYMin = other?.box?.yMin ?: Float.NaN,
            boxXMax = other?.box?.xMax ?: Float.NaN,
            boxYMax = other?.box?.yMax ?: Float.NaN,
            output = this,
        )
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [point].
     *
     * @return `this`
     */
    public fun add(point: Vec): BoxAccumulator {
        BoxAccumulatorNative.nativeAddPoint(
            envelopeHasBounds = hasBounds,
            envelopeBoundsXMin = _bounds.xMin,
            envelopeBoundsYMin = _bounds.yMin,
            envelopeBoundsXMax = _bounds.xMax,
            envelopeBoundsYMax = _bounds.yMax,
            pointX = point.x,
            pointY = point.y,
            output = this,
        )
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [segment].
     *
     * @return `this`
     */
    public fun add(segment: Segment): BoxAccumulator {
        BoxAccumulatorNative.nativeAddSegment(
            envelopeHasBounds = hasBounds,
            envelopeBoundsXMin = _bounds.xMin,
            envelopeBoundsYMin = _bounds.yMin,
            envelopeBoundsXMax = _bounds.xMax,
            envelopeBoundsYMax = _bounds.yMax,
            segmentStartX = segment.start.x,
            segmentStartY = segment.start.y,
            segmentEndX = segment.end.x,
            segmentEndY = segment.end.y,
            output = this,
        )
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [triangle].
     *
     * @return `this`
     */
    public fun add(triangle: Triangle): BoxAccumulator {
        BoxAccumulatorNative.nativeAddTriangle(
            envelopeHasBounds = hasBounds,
            envelopeBoundsXMin = _bounds.xMin,
            envelopeBoundsYMin = _bounds.yMin,
            envelopeBoundsXMax = _bounds.xMax,
            envelopeBoundsYMax = _bounds.yMax,
            triangleP0X = triangle.p0.x,
            triangleP0Y = triangle.p0.y,
            triangleP1X = triangle.p1.x,
            triangleP1Y = triangle.p1.y,
            triangleP2X = triangle.p2.x,
            triangleP2Y = triangle.p2.y,
            output = this,
        )
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [box]. If
     * [box] is null, this is a no-op.
     *
     * @return `this`
     */
    public fun add(box: Box?): BoxAccumulator {
        BoxAccumulatorNative.nativeAddOptionalBox(
            envelopeHasBounds = hasBounds,
            envelopeBoundsXMin = _bounds.xMin,
            envelopeBoundsYMin = _bounds.yMin,
            envelopeBoundsXMax = _bounds.xMax,
            envelopeBoundsYMax = _bounds.yMax,
            boxHasBounds = box != null,
            boxXMin = box?.xMin ?: Float.NaN,
            boxYMin = box?.yMin ?: Float.NaN,
            boxXMax = box?.xMax ?: Float.NaN,
            boxYMax = box?.yMax ?: Float.NaN,
            output = this,
        )
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains
     * [parallelogram].
     *
     * @return `this`
     */
    public fun add(parallelogram: Parallelogram): BoxAccumulator {
        BoxAccumulatorNative.nativeAddParallelogram(
            envelopeHasBounds = hasBounds,
            envelopeBoundsXMin = _bounds.xMin,
            envelopeBoundsYMin = _bounds.yMin,
            envelopeBoundsXMax = _bounds.xMax,
            envelopeBoundsYMax = _bounds.yMax,
            parallelogramCenterX = parallelogram.center.x,
            parallelogramCenterY = parallelogram.center.y,
            parallelogramWidth = parallelogram.width,
            parallelogramHeight = parallelogram.height,
            parallelogramAngleInRadian = parallelogram.rotation,
            parallelogramShearFactor = parallelogram.shearFactor,
            output = this,
        )
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [mesh]. If
     * [mesh] is empty, this is a no-op.
     *
     * @return `this`
     */
    public fun add(mesh: PartitionedMesh): BoxAccumulator = this.add(mesh.computeBoundingBox())

    /**
     * Compares this [BoxAccumulator] with [other], and returns true if either: Both this and
     * [other] are empty Neither this and [other] are empty, and their [box]es are almost equal per
     * [Box.isAlmostEqual]
     */
    public fun isAlmostEqual(
        other: BoxAccumulator,
        @FloatRange(from = 0.0) tolerance: Float,
    ): Boolean =
        (isEmpty() && other.isEmpty()) ||
            (!isEmpty() && !other.isEmpty() && box!!.isAlmostEqual(other.box!!, tolerance))

    /**
     * Overwrite the entries of this object with new values. This is useful for recycling an
     * instance.
     *
     * @return `this`
     */
    @UsedByNative
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun overwriteFrom(x1: Float, y1: Float, x2: Float, y2: Float): BoxAccumulator {
        hasBounds = true
        _bounds.setXBounds(x1, x2).setYBounds(y1, y2)
        return this
    }

    override fun equals(other: Any?): Boolean =
        other === this || (other is BoxAccumulator && areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = box?.hashCode() ?: 0

    override fun toString(): String = "BoxAccumulator(box=$box)"

    private companion object {
        /**
         * Returns true if [first] and [second] have the same values for all properties of
         * [BoxAccumulator].
         */
        internal fun areEquivalent(first: BoxAccumulator, second: BoxAccumulator): Boolean {
            if (first.isEmpty() && second.isEmpty()) return true // both empty
            return first.box != null &&
                second.box != null &&
                Box.areEquivalent(first.box!!, second.box!!)
        }
    }
}

/** Helper object to contain native JNI calls */
private object BoxAccumulatorNative {

    init {
        NativeLoader.load()
    }

    /**
     * Helper method to construct a native C++ [Envelope] and [Segment], add the native [Segment] to
     * the native [Envelope], and update [output] using the result.
     */
    @UsedByNative
    external fun nativeAddSegment(
        envelopeHasBounds: Boolean,
        envelopeBoundsXMin: Float,
        envelopeBoundsYMin: Float,
        envelopeBoundsXMax: Float,
        envelopeBoundsYMax: Float,
        segmentStartX: Float,
        segmentStartY: Float,
        segmentEndX: Float,
        segmentEndY: Float,
        output: BoxAccumulator,
    )

    /**
     * Helper method to construct a native C++ [Envelope] and [Triangle], add the native [Triangle]
     * to the native [Envelope], and update [output] using the result.
     */
    @UsedByNative
    external fun nativeAddTriangle(
        envelopeHasBounds: Boolean,
        envelopeBoundsXMin: Float,
        envelopeBoundsYMin: Float,
        envelopeBoundsXMax: Float,
        envelopeBoundsYMax: Float,
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        output: BoxAccumulator,
    )

    /**
     * Helper method to construct a native C++ [Envelope] and [Parallelogram], add the native
     * [Parallelogram] to the native [Envelope], and update [output] using the result.
     */
    @UsedByNative
    external fun nativeAddParallelogram(
        envelopeHasBounds: Boolean,
        envelopeBoundsXMin: Float,
        envelopeBoundsYMin: Float,
        envelopeBoundsXMax: Float,
        envelopeBoundsYMax: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramAngleInRadian: Float,
        parallelogramShearFactor: Float,
        output: BoxAccumulator,
    )

    /**
     * Helper method to construct a native C++ [Envelope] and [Point], add the native [Point] to the
     * native [Envelope], and update [output] using the result.
     */
    @UsedByNative
    external fun nativeAddPoint(
        envelopeHasBounds: Boolean,
        envelopeBoundsXMin: Float,
        envelopeBoundsYMin: Float,
        envelopeBoundsXMax: Float,
        envelopeBoundsYMax: Float,
        pointX: Float,
        pointY: Float,
        output: BoxAccumulator,
    )

    /**
     * Helper method to construct a native C++ [Envelope] using [this], add the optional box to the
     * native [Envelope], and update [output] using the result.
     */
    @UsedByNative
    external fun nativeAddOptionalBox(
        envelopeHasBounds: Boolean,
        envelopeBoundsXMin: Float,
        envelopeBoundsYMin: Float,
        envelopeBoundsXMax: Float,
        envelopeBoundsYMax: Float,
        boxHasBounds: Boolean,
        boxXMin: Float,
        boxYMin: Float,
        boxXMax: Float,
        boxYMax: Float,
        output: BoxAccumulator,
    )
}
