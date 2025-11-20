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
 * An immutable axis-aligned rectangle. See [MutableBox] for a mutable version.
 *
 * Note that unlike [android.graphics.RectF], this does not express an opinion about axis direction
 * (e.g. the positive `Y` axis being "down"), because it is intended to be used with any coordinate
 * system rather than just Android screen / View space.
 */
public class ImmutableBox internal constructor(x1: Float, y1: Float, x2: Float, y2: Float) : Box() {

    /** The lower bound in the `X` direction. */
    override val xMin: Float = min(x1, x2)

    /** The lower bound in the `Y` direction. */
    override val yMin: Float = min(y1, y2)

    /** The upper bound in the `X` direction. */
    override val xMax: Float = max(x1, x2)

    /** The upper bound in the `Y` direction. */
    override val yMax: Float = max(y1, y2)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override fun toImmutable(): ImmutableBox = this

    override fun equals(other: Any?): Boolean =
        other === this || (other is Box && Box.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = Box.hash(this)

    override fun toString(): String = "Immutable${Box.string(this)}"

    public companion object {
        /** Constructs an [ImmutableBox] with a given [center], [width], and [height]. */
        @JvmStatic
        public fun fromCenterAndDimensions(
            center: Vec,
            @FloatRange(from = 0.0) width: Float,
            @FloatRange(from = 0.0) height: Float,
        ): ImmutableBox {
            require(width >= 0f && height >= 0f)
            return ImmutableBox(
                center.x - width / 2,
                center.y - height / 2,
                center.x + width / 2,
                center.y + height / 2,
            )
        }

        /** Constructs the smallest [ImmutableBox] containing the two given points. */
        @JvmStatic
        public fun fromTwoPoints(point1: Vec, point2: Vec): ImmutableBox =
            ImmutableBox(point1.x, point1.y, point2.x, point2.y)

        /**
         * Overload that just takes the x and y components of the vectors. This isn't part of the
         * public API, but allows internal callers to avoid unnecessary allocations.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromTwoPoints(x1: Float, y1: Float, x2: Float, y2: Float): ImmutableBox =
            ImmutableBox(x1, y1, x2, y2)
    }
}
