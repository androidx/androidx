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

import androidx.annotation.RestrictTo
import androidx.annotation.Size

/**
 * Describes the transformation between the packed integer representation of a vertex attribute and
 * its actual value. The actual value of component N is defined as `actualN = scaleN * packed +
 * offsetN`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
public class MeshAttributeUnpackingParams(
    // The val components below is a defensive copy of this parameter.
    @Size(min = 1, max = 4) components: List<ComponentUnpackingParams>
) {

    public val components: List<ComponentUnpackingParams> = components.toList()

    /** The actual value of a component is defined as `actual = scale * packed + offset`. */
    public class ComponentUnpackingParams(public val offset: Float, public val scale: Float) {
        override fun equals(other: Any?): Boolean {
            // NOMUTANTS -- Check the instance first to short circuit faster.
            if (this === other) return true
            return other is ComponentUnpackingParams &&
                offset == other.offset &&
                scale == other.scale
        }

        // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
        override fun hashCode(): Int {
            return 31 * offset.hashCode() + scale.hashCode()
        }

        override fun toString(): String {
            return "ComponentUnpackingParams(offset=$offset, scale=$scale)"
        }
    }

    public companion object {

        /** Create a [MeshAttributeUnpackingParams] for a single-component attribute. */
        @JvmStatic
        public fun create(offset: Float, scale: Float): MeshAttributeUnpackingParams {
            return MeshAttributeUnpackingParams(listOf(ComponentUnpackingParams(offset, scale)))
        }

        /**
         * Create a [MeshAttributeUnpackingParams] using the values from arrays [offsets] and
         * [scales]. Both arrays must be the same size.
         */
        @JvmStatic
        public fun create(
            @Size(min = 1, max = 4) offsets: FloatArray,
            @Size(min = 1, max = 4) scales: FloatArray,
        ): MeshAttributeUnpackingParams {
            require(offsets.size == scales.size) {
                "Given ${offsets.size} offsets and ${scales.size} scales but those should be the same size."
            }
            return MeshAttributeUnpackingParams(offsets.zip(scales, ::ComponentUnpackingParams))
        }
    }

    init {
        require(components.size in 1..4) {
            "Given ${components.size} components but there should be between 1 and 4."
        }
    }

    override fun equals(other: Any?): Boolean {
        // NOMUTANTS -- Check the instance first to short circuit faster.
        if (this === other) return true
        return other is MeshAttributeUnpackingParams && components == other.components
    }

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int {
        return components.hashCode()
    }

    override fun toString(): String {
        return "MeshAttributeUnpackingParams[${components.size}](components=$components)"
    }
}
