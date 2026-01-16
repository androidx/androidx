/*
 * Copyright 2024 The Android Open Source Project
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
 * Represents a ray in 3D space. A ray is defined by an origin point and a direction vector.
 *
 * @property origin the origin of the ray
 * @property direction the direction of the ray
 */
public class Ray(
    public val origin: Vector3 = Vector3(),
    public val direction: Vector3 = Vector3(),
) {
    /** Creates a new Ray with the same values as the [other] ray. */
    public constructor(other: Ray) : this(other.origin, other.direction)

    /** Returns true if this ray is equal to the [other] ray. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ray) return false

        return this.origin == other.origin && this.direction == other.direction
    }

    override fun hashCode(): Int = 31 * origin.hashCode() + direction.hashCode()

    override fun toString(): String = "[origin=$origin, direction=$direction]"
}
