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
import androidx.xr.arcore.runtime.SpatialAnnotationImageFormat as RuntimeSpatialAnnotationImageFormat
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi

/**
 * Enumeration of supported input camera image formats. Defines the native memory layout of the
 * ByteBuffer.
 */
@ExperimentalSpatialAnnotationsApi
public class SpatialAnnotationImageFormat internal constructor(internal val value: Int) {
    public companion object {
        /** 32-bit RGBA image format. */
        @JvmField public val RGBA: SpatialAnnotationImageFormat = SpatialAnnotationImageFormat(0)

        /** 8-bit grayscale image format. */
        @JvmField
        public val GRAYSCALE: SpatialAnnotationImageFormat = SpatialAnnotationImageFormat(1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialAnnotationImageFormat) return false
        return value == other.value
    }

    override fun hashCode(): Int = value

    override fun toString(): String =
        when (value) {
            0 -> "RGBA"
            1 -> "GRAYSCALE"
            else -> "UNKNOWN($value)"
        }
}

@ExperimentalSpatialAnnotationsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SpatialAnnotationImageFormat.toRuntimeFormat(): RuntimeSpatialAnnotationImageFormat =
    when (this) {
        SpatialAnnotationImageFormat.RGBA -> RuntimeSpatialAnnotationImageFormat.RGBA
        SpatialAnnotationImageFormat.GRAYSCALE -> RuntimeSpatialAnnotationImageFormat.GRAYSCALE
        else -> throw IllegalArgumentException("Unknown format!")
    }

@ExperimentalSpatialAnnotationsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RuntimeSpatialAnnotationImageFormat.toSpatialAnnotationImageFormat():
    SpatialAnnotationImageFormat =
    when (this) {
        RuntimeSpatialAnnotationImageFormat.RGBA -> SpatialAnnotationImageFormat.RGBA
        RuntimeSpatialAnnotationImageFormat.GRAYSCALE -> SpatialAnnotationImageFormat.GRAYSCALE
        else -> throw IllegalArgumentException("Unknown SpatialAnnotationImageFormat!")
    }
