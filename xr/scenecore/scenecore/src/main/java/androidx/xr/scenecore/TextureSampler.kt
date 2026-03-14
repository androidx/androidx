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

package androidx.xr.scenecore

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo

/**
 * Defines the sampling behavior for a texture.
 *
 * The fields of this sampler are based on the public
 * [Filament TextureSampler class](https://github.com/google/filament/blob/main/android/filament-android/src/main/java/com/google/android/filament/TextureSampler.java)
 * but may diverge over time.
 *
 * @property minificationFilter an [Int] which describes how neighboring texels are sampled when the
 *   rendered size is smaller than the texture.
 * @property magnificationFilter an [Int] which describes how neighboring texels are sampled when
 *   the rendered size is larger than the texture.
 * @property wrapModeHorizontal an [Int] which describes how texture coordinates outside the [0-1]
 *   range are handled along the horizontal axis.
 * @property wrapModeVertical an [Int] which describes how texture coordinates outside the [0-1]
 *   range are handled along the vertical axis.
 * @property wrapModeDepth an [Int] which describes how texture coordinates outside the [0-1] range
 *   are handled along the depth axis.
 */
public class TextureSampler
@RestrictTo(RestrictTo.Scope.LIBRARY)
constructor(
    /**
     * an [Int] which describes how neighboring texels are sampled when the rendered size is smaller
     * than the texture.
     */
    public val minificationFilter: MinificationFilter = MinificationFilter.LINEAR,
    /**
     * an [Int] which describes how neighboring texels are sampled when the rendered size is larger
     * than the texture.
     */
    public val magnificationFilter: MagnificationFilter = MagnificationFilter.LINEAR,
    /**
     * an [Int] which describes how texture coordinates outside the [0-1] range are handled along
     * the horizontal axis.
     */
    public val wrapModeHorizontal: WrapMode = WrapMode.REPEAT,
    /**
     * an [Int] which describes how texture coordinates outside the [0-1] range are handled along
     * the vertical axis.
     */
    public val wrapModeVertical: WrapMode = WrapMode.REPEAT,
    /**
     * an [Int] which describes how texture coordinates outside the [0-1] range are handled along
     * the depth axis.
     */
    public val wrapModeDepth: WrapMode = WrapMode.REPEAT,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val compareMode: CompareMode = CompareMode.NONE,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val compareFunction: CompareFunction = CompareFunction.LESSER_OR_EQUAL,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY) @IntRange(from = 0) public val anisotropyLog2: Int = 0,
) {
    /**
     * Defines the sampling behavior for a texture.
     *
     * The fields of this sampler are based on the public
     * [Filament TextureSampler class](https://github.com/google/filament/blob/main/android/filament-android/src/main/java/com/google/android/filament/TextureSampler.java)
     * but may diverge over time.
     *
     * @param minificationFilter an [Int] which describes how neighboring texels are sampled when
     *   the rendered size is smaller than the texture.
     * @param magnificationFilter an [Int] which describes how neighboring texels are sampled when
     *   the rendered size is larger than the texture.
     * @param wrapModeHorizontal an [Int] which describes how texture coordinates outside the [0-1]
     *   range are handled along the horizontal axis.
     * @param wrapModeVertical an [Int] which describes how texture coordinates outside the [0-1]
     *   range are handled along the vertical axis.
     * @param wrapModeDepth an [Int] which describes how texture coordinates outside the [0-1] range
     *   are handled along the depth axis.
     */
    @JvmOverloads
    public constructor(
        minificationFilter: MinificationFilter = MinificationFilter.LINEAR,
        magnificationFilter: MagnificationFilter = MagnificationFilter.LINEAR,
        wrapModeHorizontal: WrapMode = WrapMode.REPEAT,
        wrapModeVertical: WrapMode = WrapMode.REPEAT,
        wrapModeDepth: WrapMode = WrapMode.REPEAT,
    ) : this(
        minificationFilter,
        magnificationFilter,
        wrapModeHorizontal,
        wrapModeVertical,
        wrapModeDepth,
        CompareMode.NONE,
        CompareFunction.LESSER_OR_EQUAL,
        0,
    )

    /** Defines the constants for texture wrap modes. */
    public class WrapMode private constructor(private val name: String) {
        public companion object {
            /** The edge of the texture extends to infinity. */
            @JvmField public val CLAMP_TO_EDGE: WrapMode = WrapMode("CLAMP_TO_EDGE")

            /** The texture infinitely repeats in the wrap direction. */
            @JvmField public val REPEAT: WrapMode = WrapMode("REPEAT")

            /** The texture infinitely repeats and mirrors in the wrap direction. */
            @JvmField public val MIRRORED_REPEAT: WrapMode = WrapMode("MIRRORED_REPEAT")
        }

        override fun toString(): String = name
    }

    /** Defines the constants for texture minification filters. */
    public class MinificationFilter private constructor(private val name: String) {
        public companion object {
            /** No filtering. Nearest neighbor is used. */
            @JvmField public val NEAREST: MinificationFilter = MinificationFilter("NEAREST")

            /** Box filtering. Weighted average of 4 neighbors is used. */
            @JvmField public val LINEAR: MinificationFilter = MinificationFilter("LINEAR")

            /** Mip-mapping is activated, but no filtering occurs. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @JvmField
            public val NEAREST_MIPMAP_NEAREST: MinificationFilter =
                MinificationFilter("NEAREST_MIPMAP_NEAREST")

            /** Box filtering within a mip-map level. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @JvmField
            public val LINEAR_MIPMAP_NEAREST: MinificationFilter =
                MinificationFilter("LINEAR_MIPMAP_NEAREST")

            /** Mip-map levels are interpolated, but no other filtering occurs. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @JvmField
            public val NEAREST_MIPMAP_LINEAR: MinificationFilter =
                MinificationFilter("NEAREST_MIPMAP_LINEAR")

            /** Both interpolated Mip-mapping and linear filtering are used. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @JvmField
            public val LINEAR_MIPMAP_LINEAR: MinificationFilter =
                MinificationFilter("LINEAR_MIPMAP_LINEAR")
        }

        override fun toString(): String = name
    }

    /** Defines the constants for texture magnification filters. */
    public class MagnificationFilter private constructor(private val name: String) {
        public companion object {
            /** No filtering. Nearest neighbor is used. */
            @JvmField public val NEAREST: MagnificationFilter = MagnificationFilter("NEAREST")

            /** Box filtering. Weighted average of 4 neighbors is used. */
            @JvmField public val LINEAR: MagnificationFilter = MagnificationFilter("LINEAR")
        }

        override fun toString(): String = name
    }

    /** Defines the constants for depth texture comparison modes. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public class CompareMode private constructor(private val name: String) {
        public companion object {
            /** The comparison function is not used. */
            @JvmField public val NONE: CompareMode = CompareMode("NONE")

            /** The comparison function is used. */
            @JvmField public val COMPARE_TO_TEXTURE: CompareMode = CompareMode("COMPARE_TO_TEXTURE")
        }

        override fun toString(): String = name
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    /** Defines the constants for depth texture comparison functions. */
    public class CompareFunction private constructor(private val name: String) {
        public companion object {
            /** Passes if the incoming depth is less than or equal to the stored depth. */
            @JvmField
            public val LESSER_OR_EQUAL: CompareFunction = CompareFunction("LESSER_OR_EQUAL")

            /** Passes if the incoming depth is greater than or equal to the stored depth. */
            @JvmField
            public val GREATER_OR_EQUAL: CompareFunction = CompareFunction("GREATER_OR_EQUAL")

            /** Passes if the incoming depth is strictly less than the stored depth. */
            @JvmField public val LESSER: CompareFunction = CompareFunction("LESSER")

            /** Passes if the incoming depth is strictly greater than the stored depth. */
            @JvmField public val GREATER: CompareFunction = CompareFunction("GREATER")

            /** Passes if the incoming depth is equal to the stored depth. */
            @JvmField public val EQUAL: CompareFunction = CompareFunction("EQUAL")

            /** Passes if the incoming depth is not equal to the stored depth. */
            @JvmField public val NOT_EQUAL: CompareFunction = CompareFunction("NOT_EQUAL")

            /** Always passes. Depth testing is effectively deactivated. */
            @JvmField public val ALWAYS: CompareFunction = CompareFunction("ALWAYS")

            /** Never passes. The depth test always fails. */
            @JvmField public val NEVER: CompareFunction = CompareFunction("NEVER")
        }

        override fun toString(): String = name
    }
}
