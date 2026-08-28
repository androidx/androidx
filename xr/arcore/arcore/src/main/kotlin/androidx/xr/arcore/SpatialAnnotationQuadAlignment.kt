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

import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi

/** Enumeration of supported 3D quad alignment modes. */
@ExperimentalSpatialAnnotationsApi
public class SpatialAnnotationQuadAlignment internal constructor(internal val value: Int) {
    public companion object {
        /** Upright HUD-style bounding box locked to world gravity, parallel to screen plane. */
        @JvmField
        public val SCREEN: SpatialAnnotationQuadAlignment = SpatialAnnotationQuadAlignment(0)

        /** Skewed 3D quad glued directly to orientation and slope of physical object. */
        @JvmField
        public val OBJECT: SpatialAnnotationQuadAlignment = SpatialAnnotationQuadAlignment(1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialAnnotationQuadAlignment) return false
        return value == other.value
    }

    override fun hashCode(): Int = value

    override fun toString(): String =
        when (value) {
            0 -> "SCREEN"
            1 -> "OBJECT"
            else -> "UNKNOWN($value)"
        }
}
