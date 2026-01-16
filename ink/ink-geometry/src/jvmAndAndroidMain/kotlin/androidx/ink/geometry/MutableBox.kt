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
import kotlin.math.max
import kotlin.math.min

/**
 * A mutable axis-aligned rectangle. See [ImmutableBox] for an immutable version.
 *
 * Note that unlike [android.graphics.RectF], this does not express an opinion about axis direction
 * (e.g. the positive Y axis being "down"), because it is intended to be used with any coordinate
 * system rather than just Android screen/View space.
 */
public class MutableBox private constructor(x1: Float, y1: Float, x2: Float, y2: Float) : Box() {

    /** The lower bound in the `X` direction. */
    override var xMin: Float = min(x1, x2)
        private set

    /** The lower bound in the `Y` direction. */
    override var yMin: Float = min(y1, y2)
        private set

    /** The upper bound in the `X` direction. */
    override var xMax: Float = max(x1, x2)
        private set

    /** The upper bound in the `Y` direction. */
    override var yMax: Float = max(y1, y2)
        private set

    /**
     * Sets the lower and upper bounds in the `X` direction to new values. The minimum value becomes
     * `xMin`, and the maximum value becomes `xMax`. Returns the same instance to chain function
     * calls.
     */
    public fun setXBounds(x1: Float, x2: Float): MutableBox {
        xMin = min(x1, x2)
        xMax = max(x1, x2)
        return this
    }

    /**
     * Sets the lower and upper bounds in the `Y` direction to new values. The minimum value becomes
     * `yMin`, and the maximum value becomes `yMax`. Returns the same instance to chain function
     * calls.
     */
    public fun setYBounds(y1: Float, y2: Float): MutableBox {
        yMin = min(y1, y2)
        yMax = max(y1, y2)
        return this
    }

    /**
     * Constructs a [MutableBox] without any initial data. Fill with the appropriate setters or
     * factory functions.
     */
    public constructor() : this(0f, 0f, 0f, 0f)

    /** Constructs the smallest [MutableBox] containing the two given points. */
    public fun populateFromTwoPoints(point1: Vec, point2: Vec): MutableBox {
        setXBounds(point1.x, point2.x)
        setYBounds(point1.y, point2.y)
        return this
    }

    /**
     * Constructs a [MutableBox] with a given [center], [width], and [height]. [width] and [height]
     * must be non-negative numbers.
     */
    public fun populateFromCenterAndDimensions(
        center: Vec,
        @FloatRange(from = 0.0) width: Float,
        @FloatRange(from = 0.0) height: Float,
    ): MutableBox {
        require(width >= 0f && height >= 0f)
        setXBounds(center.x - width / 2, center.x + width / 2)
        setYBounds(center.y - height / 2, center.y + height / 2)
        return this
    }

    /** Fills this [MutableBox] with the same values contained in [input]. */
    public fun populateFrom(input: Box): MutableBox {
        xMin = input.xMin
        yMin = input.yMin
        xMax = input.xMax
        yMax = input.yMax
        return this
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toImmutable(): ImmutableBox =
        ImmutableBox(x1 = xMin, y1 = yMin, x2 = xMax, y2 = yMax)

    override fun equals(other: Any?): Boolean =
        other === this || (other is Box && Box.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = Box.hash(this)

    override fun toString(): String = "Mutable${Box.string(this)}"
}
