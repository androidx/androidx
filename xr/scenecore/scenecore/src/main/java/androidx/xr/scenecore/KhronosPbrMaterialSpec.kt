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

package androidx.xr.scenecore

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * KhronosPbrMaterialSpec class used to define the properties of a PBR material. The fields of this
 * class are based on the Khronos spec for PBR parameters, but may diverge over time.
 * https://www.khronos.org/gltf/pbr
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class KhronosPbrMaterialSpec
internal constructor(
    public val lightingModel: @LightingModelValues Int,
    public val blendMode: @BlendModeValues Int,
    public val doubleSidedMode: @DoubleSidedModeValues Int,
) {
    /**
     * Defines the lighting model used for the material. Although these values are based on the
     * Khronos PBR specification, they may diverge over time.
     */
    public object LightingModel {
        /** Lit material. */
        public const val LIT: Int = 0
        /** Unlit material. */
        public const val UNLIT: Int = 1
    }

    /**
     * Used to set the lighting model values of a [KhronosPbrMaterialSpec]
     *
     * This API will be re-designed once it goes through local AXR API council review (b/420551533).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [LightingModel.LIT, LightingModel.UNLIT])
    @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
    internal annotation class LightingModelValues

    /**
     * Defines the blending mode used for the material. Although these values are based on the
     * Khronos PBR specification, they may diverge over time.
     */
    public object BlendMode {
        /** Opaque blending. */
        public const val OPAQUE: Int = 0
        /** Masked blending. */
        public const val MASKED: Int = 1
        /** Transparent blending. */
        public const val TRANSPARENT: Int = 2
        /** Refractive blending. */
        public const val REFRACTIVE: Int = 3
    }

    /**
     * Used to set the blend mode values of a [KhronosPbrMaterialSpec]
     *
     * This API will be re-designed once it goes through local AXR API council review (b/420551533).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value = [BlendMode.OPAQUE, BlendMode.MASKED, BlendMode.TRANSPARENT, BlendMode.REFRACTIVE]
    )
    @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
    internal annotation class BlendModeValues

    /**
     * Defines whether the material is double sided. Although these values are based on the Khronos
     * PBR specification, they may diverge over time.
     */
    public object DoubleSidedMode {
        /** Single sided material. */
        public const val SINGLE_SIDED: Int = 0
        /** Double sided material. */
        public const val DOUBLE_SIDED: Int = 1
    }

    /**
     * Used to set the double sided mode values of a [KhronosPbrMaterialSpec]
     *
     * This API will be re-designed once it goes through local AXR API council review (b/420551533).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [DoubleSidedMode.SINGLE_SIDED, DoubleSidedMode.DOUBLE_SIDED])
    @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
    internal annotation class DoubleSidedModeValues

    public companion object {
        @JvmOverloads
        @JvmStatic
        public fun create(
            lightingModel: @LightingModelValues Int = LightingModel.LIT,
            blendMode: @BlendModeValues Int = BlendMode.OPAQUE,
            doubleSidedMode: @DoubleSidedModeValues Int = DoubleSidedMode.SINGLE_SIDED,
        ): KhronosPbrMaterialSpec {
            return KhronosPbrMaterialSpec(lightingModel, blendMode, doubleSidedMode)
        }
    }
}
