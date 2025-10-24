/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime.math

/**
 * Represents an axis-aligned bounding box in 3D space, defined by its minimum and maximum corner
 * points.
 *
 * @property min A [Vector3] representing the minimum corner of the box (lowest x, y, and z values).
 * @property max A [Vector3] representing the maximum corner of the box (highest x, y, and z
 *   values).
 * @property center The center point of the box.
 * @property halfExtents The distance from the center to each face of the box along the axes. The
 *   total width, height, and depth of the box are twice the half-extent values.
 */
public class BoundingBox
private constructor(
    public val min: Vector3,
    public val max: Vector3,
    public val center: Vector3,
    public val halfExtents: FloatSize3d,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoundingBox) return false

        if (min != other.min) return false
        if (max != other.max) return false

        // center and halfExtents are derived from min/max, so checking min/max is sufficient.
        return true
    }

    override fun hashCode(): Int {
        var result = min.hashCode()
        result = 31 * result + max.hashCode()
        return result
    }

    override fun toString(): String {
        return "BoundingBox(min=$min, max=$max, center=$center, halfExtents=[" +
            "width=${halfExtents.width}, height=${halfExtents.height}, depth=${halfExtents.depth}])"
    }

    public companion object {
        /**
         * Creates a [BoundingBox] with the given minimum and maximum corner points.
         *
         * This factory method ensures that the created bounding box is valid by checking that each
         * component of the `min` point is less than or equal to the corresponding component of the
         * `max` point.
         *
         * @param min A [Vector3] representing the minimum corner of the box (lowest x, y, and z
         *   values).
         * @param max A [Vector3] representing the maximum corner of the box (highest x, y, and z
         *   values).
         * @return A new [BoundingBox] instance.
         * @throws IllegalArgumentException if any component of [max] is not greater than the
         *   corresponding component of [min].
         */
        @JvmStatic
        public fun fromMinMax(min: Vector3, max: Vector3): BoundingBox {
            require(min.x <= max.x) {
                "min.x (${min.x}) must be less than or equal to max.x (${max.x})"
            }
            require(min.y <= max.y) {
                "min.y (${min.y}) must be less than or equal to max.y (${max.y})"
            }
            require(min.z <= max.z) {
                "min.z (${min.z}) must be less than or equal to max.z (${max.z})"
            }

            val center = (min + max) * 0.5f
            val halfExtents = FloatSize3d.fromVector3((max - min) * 0.5f)
            return BoundingBox(min, max, center, halfExtents)
        }

        /**
         * Creates a [BoundingBox] from a center point and its half-extents.
         *
         * @param center The center point of the box.
         * @param halfExtents The distance from the center to each face of the box. Each component
         *   must be greater than or equal to zero.
         * @return A new [BoundingBox] instance.
         * @throws IllegalArgumentException if any component of [halfExtents] is not greater than or
         *   equal to 0.
         */
        @JvmStatic
        public fun fromCenterAndHalfExtents(
            center: Vector3,
            halfExtents: FloatSize3d,
        ): BoundingBox {
            require(halfExtents.width >= 0f) {
                "halfExtents.width (${halfExtents.width}) must be greater than or equal to 0"
            }
            require(halfExtents.height >= 0f) {
                "halfExtents.height (${halfExtents.height}) must be greater than or equal to 0"
            }
            require(halfExtents.depth >= 0f) {
                "halfExtents.depth (${halfExtents.depth}) must be greater than or equal to 0"
            }

            val halfExtentsVector =
                Vector3(halfExtents.width, halfExtents.height, halfExtents.depth)
            val min = center - halfExtentsVector
            val max = center + halfExtentsVector

            return BoundingBox(min, max, center, halfExtents)
        }
    }
}
