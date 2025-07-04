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
 */
public class BoundingBox(public val min: Vector3, public val max: Vector3) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoundingBox) return false

        if (min != other.min) return false
        if (max != other.max) return false

        return true
    }

    override fun hashCode(): Int {
        var result = min.hashCode()
        result = 31 * result + max.hashCode()
        return result
    }

    override fun toString(): String {
        return "BoundingBox(min=$min, max=$max)"
    }
}
