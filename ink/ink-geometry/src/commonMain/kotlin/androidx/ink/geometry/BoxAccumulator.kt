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
import kotlin.math.max
import kotlin.math.min

/**
 * A helper class for accumulating the minimum bounding boxes of zero or more geometry objects. In
 * colloquial terms, this can be used to find the smallest Box that contains a set of objects.
 */
@UsedByNative
public class BoxAccumulator() {

    /**
     * The bounds, which are valid only if [hasBounds] is `true`. When [hasBounds] is `false`, this
     * object exists (so that allocating a new instance of an underlying [MutableBox] doesn't happen
     * at an inconvenient time), but its data is invalid and is waiting to be overwritten to
     * represent a non-empty region.
     */
    private var bounds = MutableBox()

    /** `true` if [bounds] holds valid data, and `false` otherwise. */
    private var hasBounds: Boolean = false

    /** The currently accumulated bounding box, or null if empty. */
    public val box: Box?
        get() = if (hasBounds) bounds else null

    /** Constructs a [BoxAccumulator], populating the to accumulated bounding box from [box]. */
    public constructor(box: Box) : this() {
        add(box)
    }

    /**
     * Returns true if this [BoxAccumulator] is not empty; this is equivalent to checking if the
     * [box] property is null.
     *
     * Note that a zero-area [Box] is not considered empty, as a [Box] contains its boundary; so
     * [isEmpty] will return false even if you have added only a single point to the
     * [BoxAccumulator].
     */
    public fun isEmpty(): Boolean = !hasBounds

    /**
     * Resets the [BoxAccumulator] instance to contain just [input]. If [input] is null, the
     * instance will be reset to empty.
     *
     * Returns the modified instance to allow chaining function calls.
     *
     * A [BoxAccumulator] can be efficiently set to the same values as another [BoxAccumulator] with
     * `populateFrom(other.box)`.
     *
     * @return `this`
     */
    public fun populateFrom(input: Box?): BoxAccumulator = reset().add(input)

    /**
     * Reset this object to have no bounds.
     *
     * Returns the modified instance to allow chaining function calls.
     *
     * @return `this`
     */
    @UsedByNative
    public fun reset(): BoxAccumulator {
        hasBounds = false
        bounds.setXBounds(Float.NaN, Float.NaN).setYBounds(Float.NaN, Float.NaN)
        return this
    }

    private fun addPoint(x: Float, y: Float): BoxAccumulator {
        if (hasBounds) {
            bounds
                .setXBounds(min(bounds.xMin, x), max(bounds.xMax, x))
                .setYBounds(min(bounds.yMin, y), max(bounds.yMax, y))
        } else {
            hasBounds = true
            bounds.setXBounds(x, x).setYBounds(y, y)
        }
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [other]. If
     * [other] is null, this is a no-op.
     *
     * Returns the modified instance to allow chaining function calls.
     *
     * @return `this`
     */
    public fun add(other: BoxAccumulator?): BoxAccumulator {
        other?.box?.let {
            addPoint(it.xMin, it.yMin)
            addPoint(it.xMax, it.yMax)
        }
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains `Point`.
     *
     * Returns the modified instance to allow chaining function calls.
     *
     * @return `this`
     */
    public fun add(point: Vec): BoxAccumulator = addPoint(point.x, point.y)

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [segment].
     *
     * Returns the modified instance to allow chaining function calls.
     *
     * @return `this`
     */
    public fun add(segment: Segment): BoxAccumulator =
        addPoint(segment.start.x, segment.start.y).addPoint(segment.end.x, segment.end.y)

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [triangle].
     *
     * Returns the modified instance to allow chaining function calls.
     *
     * @return `this`
     */
    public fun add(triangle: Triangle): BoxAccumulator {
        addPoint(triangle.p0.x, triangle.p0.y)
        addPoint(triangle.p1.x, triangle.p1.y)
        addPoint(triangle.p2.x, triangle.p2.y)
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [box]. If
     * [box] is null, this is a no-op.
     *
     * Returns the modified instance to allow chaining function calls.
     *
     * @return `this`
     */
    public fun add(box: Box?): BoxAccumulator {
        box?.let {
            addPoint(it.xMin, it.yMin)
            addPoint(it.xMax, it.yMax)
        }
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains
     * [parallelogram].
     *
     * Returns the modified instance to allow chaining function calls.
     *
     * @return `this`
     */
    public fun add(parallelogram: Parallelogram): BoxAccumulator {
        if (hasBounds) {
            val oldXMin = bounds.xMin
            val oldYMin = bounds.yMin
            val oldXMax = bounds.xMax
            val oldYMax = bounds.yMax
            parallelogram.computeBoundingBox(bounds)
            addPoint(oldXMin, oldYMin)
            addPoint(oldXMax, oldYMax)
        } else {
            hasBounds = true
            parallelogram.computeBoundingBox(bounds)
        }
        return this
    }

    /**
     * Expands the accumulated bounding box (if necessary) such that it also contains [mesh]. If
     * [mesh] is empty, this is a no-op.
     *
     * Returns the modified instance to allow chaining function calls.
     *
     * @return `this`
     */
    public fun add(mesh: PartitionedMesh): BoxAccumulator = add(mesh.computeBoundingBox())

    /**
     * Compares this [BoxAccumulator] with [other], and returns true if either: Both this and
     * [other] are empty, or neither this and [other] are empty and their [box]es are almost equal
     * per [Box.isAlmostEqual]
     */
    public fun isAlmostEqual(
        other: BoxAccumulator,
        @FloatRange(from = 0.0) tolerance: Float,
    ): Boolean =
        this === other ||
            if (hasBounds) {
                other.hasBounds && bounds.isAlmostEqual(other.bounds, tolerance)
            } else {
                !other.hasBounds
            }

    /**
     * Overwrite the entries of this object with new values. This is useful for recycling an
     * instance.
     *
     * Returns the modified instance to allow chaining function calls.
     *
     * @return `this`
     */
    @UsedByNative
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun populateFrom(x1: Float, y1: Float, x2: Float, y2: Float): BoxAccumulator {
        hasBounds = true
        bounds.setXBounds(x1, x2).setYBounds(y1, y2)
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
        fun areEquivalent(first: BoxAccumulator, second: BoxAccumulator): Boolean =
            first === second ||
                if (first.hasBounds) {
                    second.hasBounds && Box.areEquivalent(first.bounds, second.bounds)
                } else {
                    !second.hasBounds
                }
    }
}
