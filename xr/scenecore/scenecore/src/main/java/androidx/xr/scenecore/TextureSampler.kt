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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * TextureSampler class used to define the way a texture gets sampled. The fields of this sampler
 * are based on the public Filament TextureSampler class but may diverge over time.
 * https://github.com/google/filament/blob/main/android/filament-android/src/main/java/com/google/android/filament/TextureSampler.java
 *
 * @property minFilter an [Int] which describes how neighboring texels are sampled when the rendered
 *   size is smaller than the texture. Must be one of [MinFilter].
 * @property magFilter an [Int] which describes how neighboring texels are sampled when the rendered
 *   size is larger than the texture. Must be one of [MagFilter].
 * @property wrapModeS an [Int] which describes how texture coordinates outside the [0-1] range are
 *   handled. Must be one of [WrapMode]
 * @property wrapModeT an [Int] which describes how texture coordinates outside the [0-1] range are
 *   handled. Must be one of [WrapMode]
 * @property wrapModeR an [Int] which describes how texture coordinates outside the [0-1] range are
 *   handled. Must be one of [WrapMode]
 * @property compareMode an [Int] which describes how depth texture sampling comparisons are
 *   handled. Must be one of [CompareMode].
 * @property compareFunc an [Int] which describes how depth texture sampling comparisons are
 *   evaluated. Must be one of [CompareFunc].
 * @property anisotropyLog2 an [Int] which controls the level of anisotropic filtering applied to
 *   the texture. Higher values mean more samples and better quality, at increased GPU cost.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class TextureSampler
internal constructor(
    public val minFilter: @MinFilterValues Int,
    public val magFilter: @MagFilterValues Int,
    public val wrapModeS: @WrapModeValues Int,
    public val wrapModeT: @WrapModeValues Int,
    public val wrapModeR: @WrapModeValues Int,
    public val compareMode: @CompareModeValues Int,
    public val compareFunc: @CompareFuncValues Int,
    public val anisotropyLog2: Int,
) {
    /**
     * Defines how texture coordinates outside the range [0, 1] are handled. Although these values
     * are based on the public Filament values, they may diverge over time.
     */
    public object WrapMode {
        /** The edge of the texture extends to infinity. */
        public const val CLAMP_TO_EDGE: Int = 0
        /** The texture infinitely repeats in the wrap direction. */
        public const val REPEAT: Int = 1
        /** The texture infinitely repeats and mirrors in the wrap direction. */
        public const val MIRRORED_REPEAT: Int = 2
    }

    /** Used to set the wrap mode values of a [TextureSampler] */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [WrapMode.CLAMP_TO_EDGE, WrapMode.REPEAT, WrapMode.MIRRORED_REPEAT])
    @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
    internal annotation class WrapModeValues

    /**
     * Specifies how the texture is sampled when it's minified (appears smaller than its original
     * size). Although these values are based on the public Filament values, they may diverge over
     * time.
     */
    public object MinFilter {
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
    }

    /** Used to set the min filter values of a [TextureSampler] */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                MinFilter.NEAREST,
                MinFilter.LINEAR,
                MinFilter.NEAREST_MIPMAP_NEAREST,
                MinFilter.LINEAR_MIPMAP_NEAREST,
                MinFilter.NEAREST_MIPMAP_LINEAR,
                MinFilter.LINEAR_MIPMAP_LINEAR,
            ]
    )
    @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
    internal annotation class MinFilterValues

    /**
     * Specifies how the texture is sampled when it's magnified (appears larger than its original
     * size). Although these values are based on the public Filament values, they may diverge over
     * time.
     */
    public object MagFilter {
        /** No filtering. Nearest neighbor is used. */
        public const val NEAREST: Int = 0
        /** Box filtering. Weighted average of 4 neighbors is used. */
        public const val LINEAR: Int = 1
    }

    /** Used to set the mag filter values of a [TextureSampler] */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [MagFilter.NEAREST, MagFilter.LINEAR])
    @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
    internal annotation class MagFilterValues

    /**
     * Used for depth texture comparisons, determining how the sampled depth value is compared to a
     * reference depth. Although these values are based on the public Filament values, they may
     * diverge over time.
     */
    public object CompareMode {
        public const val NONE: Int = 0
        public const val COMPARE_TO_TEXTURE: Int = 1
    }

    /** Used to set the compare mode values of a [TextureSampler] */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [CompareMode.NONE, CompareMode.COMPARE_TO_TEXTURE])
    @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
    internal annotation class CompareModeValues

    /**
     * Comparison functions for the depth sampler. Although these values are based on the public
     * Filament values, they may diverge over time.
     */
    public object CompareFunc {
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

    /** Used to set the compare mode values of a [TextureSampler] */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                CompareFunc.LE,
                CompareFunc.GE,
                CompareFunc.L,
                CompareFunc.G,
                CompareFunc.E,
                CompareFunc.NE,
                CompareFunc.A,
                CompareFunc.N,
            ]
    )
    @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
    internal annotation class CompareFuncValues

    public companion object {
        @JvmOverloads
        @JvmStatic
        public fun create(
            minFilter: @MinFilterValues Int = MinFilter.LINEAR,
            magFilter: @MagFilterValues Int = MagFilter.LINEAR,
            wrapModeS: @WrapModeValues Int = WrapMode.REPEAT,
            wrapModeT: @WrapModeValues Int = WrapMode.REPEAT,
            wrapModeR: @WrapModeValues Int = WrapMode.REPEAT,
            compareMode: @CompareModeValues Int = CompareMode.NONE,
            compareFunc: @CompareFuncValues Int = CompareFunc.LE,
            // Controls the level of anisotropic filtering applied to the texture, improving the
            // appearance of textures at steep angles. Higher values mean more samples and better
            // quality,
            // but also increased GPU load.
            anisotropyLog2: Int = 0,
        ): TextureSampler {
            return TextureSampler(
                minFilter,
                magFilter,
                wrapModeS,
                wrapModeT,
                wrapModeR,
                compareMode,
                compareFunc,
                anisotropyLog2,
            )
        }
    }
}
