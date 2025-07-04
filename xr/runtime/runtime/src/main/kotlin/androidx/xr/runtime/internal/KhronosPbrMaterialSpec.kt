/*
 * Copyright 2025 The Android Open Source Project
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
 * KhronosPbrMaterialSpec class used to define the way a Khronos PBR material is rendered. The
 * fields of this spec class are based on underlying renderer definition but may diverge over time.
 *
 * @property lightingModel an [Int] which describes the lighting model used for the material. Must
 *   be one of [LightingModel].
 * @property blendMode an [Int] which describes the blending mode used for the material. Must be one
 *   of [BlendMode].
 * @property doubleSidedMode an [Int] which describes whether the material is double sided. Must be
 *   one of [DoubleSidedMode]
 *
 * This API will be re-designed once it goes through local AXR API council review (b/420551533).
 */
// TODO(b/422251760): Use POKO pattern for internal inline value class.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class KhronosPbrMaterialSpec(
    @KhronosPbrMaterialSpec.LightingModel public val lightingModel: Int,
    @KhronosPbrMaterialSpec.BlendMode public val blendMode: Int,
    @KhronosPbrMaterialSpec.DoubleSidedMode public val doubleSidedMode: Int,
) {
    /**
     * Defines the lighting model used for the material. Although these values are based on the
     * Khronos PBR specification, they may diverge over time.
     */
    public annotation class LightingModel {
        public companion object {
            public const val LIT: Int = 0
            public const val UNLIT: Int = 1
        }
    }

    /**
     * Defines the blending mode used for the material. Although these values are based on the
     * Khronos PBR specification, they may diverge over time.
     */
    public annotation class BlendMode {
        public companion object {
            public const val OPAQUE: Int = 0
            public const val MASKED: Int = 1
            public const val TRANSPARENT: Int = 2
            public const val REFRACTIVE: Int = 3
        }
    }

    /**
     * Defines whether the material is double sided. Although these values are based on the Khronos
     * PBR specification, they may diverge over time.
     */
    public annotation class DoubleSidedMode {
        public companion object {
            public const val SINGLE_SIDED: Int = 0
            public const val DOUBLE_SIDED: Int = 1
        }
    }

    public companion object {
        /** Lit material. */
        public const val LIT: Int = 0

        /** Unlit material. */
        public const val UNLIT: Int = 1

        /** Opaque blending. */
        public const val OPAQUE: Int = 0

        /** Masked blending. */
        public const val MASKED: Int = 1

        /** Alpha blending. */
        public const val TRANSPARENT: Int = 2

        /** Refractive blending. */
        public const val REFRACTIVE: Int = 3

        /** Single sided material. */
        public const val SINGLE_SIDED: Int = 0

        /** Double sided material. */
        public const val DOUBLE_SIDED: Int = 1
    }
}
