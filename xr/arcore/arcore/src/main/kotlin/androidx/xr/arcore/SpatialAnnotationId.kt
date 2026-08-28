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

/** A strongly-typed identifier mapped to a requested [SpatialAnnotation]. */
@ExperimentalSpatialAnnotationsApi
public class SpatialAnnotationId private constructor(private val value: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialAnnotationId) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    public companion object {
        /**
         * Creates a [SpatialAnnotationId] from a string.
         *
         * @param value the string value of the [SpatialAnnotationId]
         * @return a [SpatialAnnotationId] with the given value
         * @throws IllegalArgumentException if the value is empty
         */
        @JvmStatic
        public fun fromString(value: String): SpatialAnnotationId {
            require(value.isNotEmpty()) { "SpatialAnnotationId value must not be empty." }
            return SpatialAnnotationId(value)
        }
    }
}
