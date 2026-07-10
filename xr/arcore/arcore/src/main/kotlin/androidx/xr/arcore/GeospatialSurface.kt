/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore

/** The type of surface on which to create an anchor. */
public class GeospatialSurface private constructor(private val value: Int) {
    public companion object {
        /** The terrain surface. */
        @JvmField public val TERRAIN: GeospatialSurface = GeospatialSurface(0)
        /** The rooftop surface. */
        @JvmField public val ROOFTOP: GeospatialSurface = GeospatialSurface(1)
    }

    override fun hashCode(): Int {
        return 31 * value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeospatialSurface) return false
        return value == other.value
    }

    /**
     * Returns a string representation of [GeospatialSurface] for debugging.
     *
     * Note: Not intended for production use.
     */
    override fun toString(): String =
        when (value) {
            0 -> "TERRAIN"
            1 -> "ROOFTOP"
            else -> "UNKNOWN"
        }
}
