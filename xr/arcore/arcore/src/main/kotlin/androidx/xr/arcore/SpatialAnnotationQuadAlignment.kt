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

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.SpatialAnnotationQuadAlignment as RuntimeSpatialAnnotationQuadAlignment
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi

/**
 * Alignment of the 3D quad produced for a tracked Spatial Annotation.
 *
 * The alignment determines which plane the quad lies in:
 * - [SCREEN]: a plane facing the viewer, kept upright by gravity.
 * - [OBJECT]: the object's plane as framed in the reference image.
 * - [SURFACE]: the physical surface of a planar target.
 */
@ExperimentalSpatialAnnotationsApi
public class SpatialAnnotationQuadAlignment internal constructor(internal val value: Int) {
    public companion object {
        /**
         * Quad faces the viewer and stays upright relative to world gravity.
         *
         * The result is a regular, upright rectangle parallel to the screen, regardless of how the
         * object is oriented.
         */
        @JvmField
        public val SCREEN: SpatialAnnotationQuadAlignment = SpatialAnnotationQuadAlignment(0)

        /**
         * Quad follows the object's perspective as framed in the reference image.
         *
         * The result is a skewed quadrilateral that matches how the object appeared when the
         * reference image was captured. Unlike [SURFACE], it is not aligned to the object's
         * physical surface.
         */
        @JvmField
        public val OBJECT: SpatialAnnotationQuadAlignment = SpatialAnnotationQuadAlignment(1)

        /**
         * Quad lies flat against the physical surface of the tracked target.
         *
         * The result is coplanar with the target, as if stuck to it, regardless of the angle at
         * which the reference image was captured. Intended for planar targets such as posters,
         * screens, or labels.
         */
        @JvmField
        public val SURFACE: SpatialAnnotationQuadAlignment = SpatialAnnotationQuadAlignment(2)
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
            2 -> "SURFACE"
            else -> "UNKNOWN($value)"
        }
}

@ExperimentalSpatialAnnotationsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SpatialAnnotationQuadAlignment.toRuntimeAlignment():
    RuntimeSpatialAnnotationQuadAlignment =
    when (this) {
        SpatialAnnotationQuadAlignment.SCREEN -> RuntimeSpatialAnnotationQuadAlignment.SCREEN
        SpatialAnnotationQuadAlignment.OBJECT -> RuntimeSpatialAnnotationQuadAlignment.OBJECT
        SpatialAnnotationQuadAlignment.SURFACE -> RuntimeSpatialAnnotationQuadAlignment.SURFACE
        else -> throw IllegalArgumentException("Unknown alignment!")
    }

@ExperimentalSpatialAnnotationsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RuntimeSpatialAnnotationQuadAlignment.toSpatialAnnotationQuadAlignment():
    SpatialAnnotationQuadAlignment =
    when (this) {
        RuntimeSpatialAnnotationQuadAlignment.SCREEN -> SpatialAnnotationQuadAlignment.SCREEN
        RuntimeSpatialAnnotationQuadAlignment.OBJECT -> SpatialAnnotationQuadAlignment.OBJECT
        RuntimeSpatialAnnotationQuadAlignment.SURFACE -> SpatialAnnotationQuadAlignment.SURFACE
        else -> throw IllegalArgumentException("Unknown alignment!")
    }
