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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/**
 * TextureSampler class used to define the way a texture gets sampled. The fields of this sampler
 * are based on the public Filament TextureSampler class but may diverge over time.
 * https://github.com/google/filament/blob/main/android/filament-android/src/main/java/com/google/android/filament/TextureSampler.java
 *
 * @param wrapModeS wrap mode S for the texture sampler.
 * @param wrapModeT wrap mode T for the texture sampler.
 * @param wrapModeR wrap mode R for the texture sampler.
 * @param minFilter min filter for the texture sampler.
 * @param magFilter mag filter for the texture sampler.
 * @param compareMode compare mode for the texture sampler.
 * @param compareFunc compare function for the texture sampler.
 * @param anisotropyLog2 anisotropy log 2 for the texture sampler.
 */
// TODO(b/422251760): Use POKO pattern for internal inline value class.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class TextureSampler(
    @TextureSampler.WrapMode public val wrapModeS: Int,
    @TextureSampler.WrapMode public val wrapModeT: Int,
    @TextureSampler.WrapMode public val wrapModeR: Int,
    @TextureSampler.WrapMode public val minFilter: Int,
    @TextureSampler.WrapMode public val magFilter: Int,
    @TextureSampler.WrapMode public val compareMode: Int,
    @TextureSampler.WrapMode public val compareFunc: Int,
    @TextureSampler.WrapMode public val anisotropyLog2: Int,
) {
    /**
     * Defines how texture coordinates outside the range [0, 1] are handled. Although these values
     * are based on the public Filament values, they may diverge over time.
     */
    public annotation class WrapMode {
        public companion object {
            public const val CLAMP_TO_EDGE: Int = 0
            public const val REPEAT: Int = 1
            public const val MIRRORED_REPEAT: Int = 2
        }
    }

    /**
     * Specifies how the texture is sampled when it's minified (appears smaller than its original
     * size). Although these values are based on the public Filament values, they may diverge over
     * time.
     */
    public annotation class MinFilter {
        public companion object {
            public const val NEAREST: Int = 0
            public const val LINEAR: Int = 1
            public const val NEAREST_MIPMAP_NEAREST: Int = 2
            public const val LINEAR_MIPMAP_NEAREST: Int = 3
            public const val NEAREST_MIPMAP_LINEAR: Int = 4
            public const val LINEAR_MIPMAP_LINEAR: Int = 5
        }
    }

    /**
     * Specifies how the texture is sampled when it's magnified (appears larger than its original
     * size). Although these values are based on the public Filament values, they may diverge over
     * time.
     */
    public annotation class MagFilter {
        public companion object {
            public const val NEAREST: Int = 0
            public const val LINEAR: Int = 1
        }
    }

    /**
     * Used for depth texture comparisons, determining how the sampled depth value is compared to a
     * reference depth. Although these values are based on the public Filament values, they may
     * diverge over time.
     */
    public annotation class CompareMode {
        public companion object {
            public const val NONE: Int = 0
            public const val COMPARE_TO_TEXTURE: Int = 1
        }
    }

    /**
     * Comparison functions for the depth sampler. Although these values are based on the public
     * Filament values, they may diverge over time.
     */
    public annotation class CompareFunc {
        public companion object {
            public const val LE: Int = 0
            public const val GE: Int = 1
            public const val L: Int = 2
            public const val G: Int = 3
            public const val E: Int = 4
            public const val NE: Int = 5
            public const val A: Int = 6
            public const val N: Int = 7
        }
    }

    public companion object {
        /** The edge of the texture extends to infinity. */
        public const val CLAMP_TO_EDGE: Int = 0

        /** The texture infinitely repeats in the wrap direction. */
        public const val REPEAT: Int = 1

        /** The texture infinitely repeats and mirrors in the wrap direction. */
        public const val MIRRORED_REPEAT: Int = 2

        /** No filtering. Nearest neighbor is used. */
        public const val NEAREST: Int = 0

        /** Box filtering. Weighted average of 4 neighbors is used. */
        public const val LINEAR: Int = 1

        /** Mip-mapping is activated. But no filtering occurs. */
        public const val NEAREST_MIPMAP_NEAREST: Int = 2

        /** Box filtering within a mip-map level. */
        public const val LINEAR_MIPMAP_NEAREST: Int = 3

        /** Mip-map levels are interpolated, but no other filtering occurs. */
        public const val NEAREST_MIPMAP_LINEAR: Int = 4

        /** Both interpolated Mip-mapping and linear filtering are used. */
        public const val LINEAR_MIPMAP_LINEAR: Int = 5

        /** No filtering. Nearest neighbor is used. */
        public const val MAG_NEAREST: Int = 0

        /** Box filtering. Weighted average of 4 neighbors is used. */
        public const val MAG_LINEAR: Int = 1

        public const val NONE: Int = 0

        public const val COMPARE_TO_TEXTURE: Int = 1

        /** Less or equal */
        public const val LE: Int = 0

        /** Greater or equal */
        public const val GE: Int = 1

        /** Strictly less than */
        public const val L: Int = 2

        /** Strictly greater than */
        public const val G: Int = 3

        /** Equal */
        public const val E: Int = 4

        /** Not equal */
        public const val NE: Int = 5

        /** Always. Depth testing is deactivated. */
        public const val A: Int = 6

        /** Never. The depth test always fails. */
        public const val N: Int = 7
    }
}
