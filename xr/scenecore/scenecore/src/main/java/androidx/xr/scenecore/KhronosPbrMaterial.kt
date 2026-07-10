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

import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4
import androidx.xr.scenecore.runtime.MaterialResource as RtMaterial
import androidx.xr.scenecore.runtime.RenderingRuntime

/**
 * Represents a lit PBR (Physically-Based Rendering) material, which defines the visual appearance
 * of a surface by simulating its interaction with light.
 *
 * This material implements the Khronos PBR (Physically-Based Rendering) metallic-roughness model.
 * It is a direct implementation of the following glTF features:
 * - [Core glTF 2.0
 *   Material](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#reference-material)
 *   (pbrMetallicRoughness, normalTexture, occlusionTexture, emissiveTexture)
 * - [KHR_materials_clearcoat
 *   extension](https://github.com/KhronosGroup/glTF/tree/main/extensions/2.0/Khronos/KHR_materials_clearcoat)
 * - [KHR_materials_sheen
 *   extension](https://github.com/KhronosGroup/glTF/tree/main/extensions/2.0/Khronos/KHR_materials_sheen)
 */
public class KhronosPbrMaterial
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val material: RtMaterial,
    internal val alphaMode: AlphaMode,
    internal val session: Session,
) : Material {

    /**
     * Closes the [KhronosPbrMaterial] and releases its underlying graphics resources.
     *
     * After being closed, the [KhronosPbrMaterial] should not be used further.
     */
    @MainThread
    override public fun close() {
        session.renderingRuntime.destroyKhronosPbrMaterial(material)
    }

    /**
     * Sets the material's base color texture (often called albedo).
     *
     * This texture defines the main color of the surface. It is typically used to apply visual
     * patterns like wood grain or fabric weaves. By default this is a white texture, where all
     * pixels are [1, 1, 1, 1]. In other words, if this is left as default, the base color will
     * always be the base color factor.
     *
     * @param texture The [Texture] to be used as the base color texture, in sRGB color space.
     * @param sampler The [TextureSampler] to be used when sampling the base color texture.
     */
    @JvmOverloads
    @MainThread
    public fun setBaseColorTexture(texture: Texture, sampler: TextureSampler = TextureSampler()) {
        session.renderingRuntime.setBaseColorTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
    }

    /**
     * Sets a linear multiplier for the base color.
     *
     * By default this is [1, 1, 1, 1].
     *
     * @param factor The [Vector4] (RGBA) factor multiplied component-wise with the base color
     *   texture.
     */
    @MainThread
    public fun setBaseColorFactor(factor: Vector4) {
        session.renderingRuntime.setBaseColorFactorsOnKhronosPbrMaterial(material, factor)
    }

    /**
     * Sets a texture defining metallic and roughness properties.
     *
     * By default this is a white texture, where all pixels are [1, 1, 1, 1]. In other words, if
     * this is left as default, the metalic and roughness values will always come from the
     * corresponding factors.
     *
     * @param texture The [Texture] to be used. The texture must use its blue channel for metallic
     *   and green channel for roughness, in linear space.
     * @param sampler The [TextureSampler] to be used when sampling the texture.
     */
    @JvmOverloads
    @MainThread
    public fun setMetallicRoughnessTexture(
        texture: Texture,
        sampler: TextureSampler = TextureSampler(),
    ) {
        session.renderingRuntime.setMetallicRoughnessTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
    }

    /**
     * Sets a scalar multiplier for the material's metallic property.
     *
     * @param factor The metallic factor. Default is 1.0. Valid values are between 0.0 and 1.0,
     *   inclusive.
     */
    @MainThread
    public fun setMetallicFactor(@FloatRange(from = 0.0, to = 1.0) factor: Float) {
        session.renderingRuntime.setMetallicFactorOnKhronosPbrMaterial(material, factor)
    }

    /**
     * Sets a scalar multiplier for the material's roughness.
     *
     * @param factor The roughness factor. Default is 1.0. Valid values are between 0.0 and 1.0,
     *   inclusive.
     */
    @MainThread
    public fun setRoughnessFactor(@FloatRange(from = 0.0, to = 1.0) factor: Float) {
        session.renderingRuntime.setRoughnessFactorOnKhronosPbrMaterial(material, factor)
    }

    /**
     * Sets the normal map texture for surface detail.
     *
     * A normal map is a texture that simulates fine surface details, such as bumps, grooves, or
     * scratches, by modifying the way light reflects off the surface. This creates the illusion of
     * depth and complexity. By default the texture is unset, in which case no normal mapping is
     * done.
     *
     * @param texture The [Texture] to be used as the normal map, in tangent space and linear color.
     * @param scale A scalar multiplier controlling the strength of the normal map. Default is 1.0.
     * @param sampler The [TextureSampler] to be used when sampling the normal texture.
     */
    @JvmOverloads
    @MainThread
    public fun setNormalTexture(
        texture: Texture,
        @FloatRange(from = 0.0) scale: Float = 1.0f,
        sampler: TextureSampler = TextureSampler(),
    ) {
        session.renderingRuntime.setNormalTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
        // TODO(b/441548345): Combine these calls at the renderer level.
        session.renderingRuntime.setNormalFactorOnKhronosPbrMaterial(material, scale)
    }

    /**
     * Sets the ambient occlusion texture.
     *
     * By default this is unset, in which case there is no ambient occlusion.
     *
     * @param texture The [Texture] to be used. It must use the red channel in linear space.
     * @param strength A scalar multiplier controlling the strength of the occlusion. Default is
     *   1.0. Valid values are between 0.0 and 1.0, inclusive.
     * @param sampler The [TextureSampler] to be used when sampling the texture.
     * @throws IllegalArgumentException if strength is outside of the range 0-1, inclusive.
     */
    @JvmOverloads
    @MainThread
    public fun setOcclusionTexture(
        texture: Texture,
        @FloatRange(from = 0.0, to = 1.0) strength: Float = 1.0f,
        sampler: TextureSampler = TextureSampler(),
    ) {
        session.renderingRuntime.setAmbientOcclusionTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
        // TODO(b/441548345): Combine these calls at the renderer level.
        session.renderingRuntime.setAmbientOcclusionFactorOnKhronosPbrMaterial(material, strength)
    }

    /**
     * Sets the emissive texture, defining light emitted by the material.
     *
     * By default this is a white texture, where all pixels are [1, 1, 1, 1]. In other words, if
     * this is left as default, the emissive values will always come from the emissive factor.
     *
     * @param texture The [Texture] to be used as the emissive texture, in sRGB color space.
     * @param sampler The [TextureSampler] to be used when sampling the emissive texture.
     */
    @JvmOverloads
    @MainThread
    public fun setEmissiveTexture(texture: Texture, sampler: TextureSampler = TextureSampler()) {
        session.renderingRuntime.setEmissiveTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
    }

    /**
     * Sets a linear multiplier for the emissive color.
     *
     * By default this is [0, 0, 0].
     *
     * @param factor The [Vector3] (in red, green, blue format) factor multiplied component-wise
     *   with the emissive texture.
     */
    @MainThread
    public fun setEmissiveFactor(factor: Vector3) {
        session.renderingRuntime.setEmissiveFactorsOnKhronosPbrMaterial(material, factor)
    }

    /**
     * Sets the clearcoat intensity texture.
     *
     * By default this is a white texture, where all pixels are [1, 1, 1, 1]. In other words, if
     * this is left as default, the clearcoat intensity values will always come from the clearcoat
     * factor.
     *
     * @param texture The [Texture] to be used. The texture defines the clearcoat layer's strength,
     *   using the red channel in linear space.
     * @param sampler The [TextureSampler] to be used when sampling the texture.
     */
    @JvmOverloads
    @MainThread
    public fun setClearcoatTexture(texture: Texture, sampler: TextureSampler = TextureSampler()) {
        session.renderingRuntime.setClearcoatTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
    }

    /**
     * Sets the normal map texture for the clearcoat layer.
     *
     * By default the texture is unset, in which case no normal mapping is done for the clearcoat,
     * even if the base color has normal mapping.
     *
     * @param texture The [Texture] to be used as the clearcoat normal texture, in tangent space and
     *   linear color.
     * @param scale A scalar multiplier controlling the strength of the clearcoat normal. Default is
     *   1.0.
     * @param sampler The [TextureSampler] to be used when sampling the texture.
     */
    @JvmOverloads
    @MainThread
    public fun setClearcoatNormalTexture(
        texture: Texture,
        @FloatRange(from = 0.0) scale: Float = 1.0f,
        sampler: TextureSampler = TextureSampler(),
    ) {
        session.renderingRuntime.setClearcoatNormalTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
        // TODO(b/441548345): Combine these calls at the renderer level.
        session.renderingRuntime.setClearcoatFactorsOnKhronosPbrMaterial(
            material,
            0.0f,
            0.0f,
            scale,
        )
    }

    /**
     * Sets the clearcoat roughness texture.
     *
     * By default this is a white texture, where all pixels are [1, 1, 1, 1]. In other words, if
     * this is left as default, the clearcoat roughness values will always come from the clearcoat
     * roughness factor.
     *
     * @param texture The [Texture] to be used. It defines the clearcoat layer's roughness, using
     *   the green channel in linear space.
     * @param sampler The [TextureSampler] to be used when sampling the texture.
     */
    @JvmOverloads
    @MainThread
    public fun setClearcoatRoughnessTexture(
        texture: Texture,
        sampler: TextureSampler = TextureSampler(),
    ) {
        session.renderingRuntime.setClearcoatRoughnessTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
    }

    /**
     * Sets a scalar multiplier for the clearcoat layer's intensity.
     *
     * Other methods for controlling the clearcoat layer include:
     * - [setClearcoatTexture]
     * - [setClearcoatNormalTexture]
     * - [setClearcoatRoughnessTexture]
     * - [setClearcoatRoughnessFactor]
     *
     * @param factor The clearcoat intensity factor. Default is 0.0. Valid values are between 0.0
     *   and 1.0, inclusive.
     */
    @MainThread
    public fun setClearcoatFactor(@FloatRange(from = 0.0, to = 1.0) factor: Float) {
        session.renderingRuntime.setClearcoatFactorsOnKhronosPbrMaterial(
            material,
            factor,
            0.0f,
            0.0f,
        )
    }

    /**
     * Sets a scalar multiplier for the clearcoat layer's roughness.
     *
     * @param factor The clearcoat roughness factor. Default is 0.0. Valid values are between 0.0
     *   and 1.0, inclusive.
     */
    @MainThread
    public fun setClearcoatRoughnessFactor(@FloatRange(from = 0.0, to = 1.0) factor: Float) {
        session.renderingRuntime.setClearcoatFactorsOnKhronosPbrMaterial(
            material,
            0.0f,
            factor,
            0.0f,
        )
    }

    /**
     * Sets the sheen color texture, for materials like fabric.
     *
     * By default this is a white texture, where all pixels are [1, 1, 1, 1]. In other words, if
     * this is left as default, the sheen color values will always come from the sheen color factor.
     *
     * @param texture The [Texture] to be used, in sRGB.
     * @param sampler The [TextureSampler] to be used when sampling the texture.
     */
    @JvmOverloads
    @MainThread
    public fun setSheenColorTexture(texture: Texture, sampler: TextureSampler = TextureSampler()) {
        session.renderingRuntime.setSheenColorTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
    }

    /**
     * Sets a linear multiplier for the sheen color.
     *
     * By default this is [0, 0, 0].
     *
     * @param factor The [Vector3] (RGB) factor multiplied component-wise with the sheen color
     *   texture.
     */
    @MainThread
    public fun setSheenColorFactor(factor: Vector3) {
        session.renderingRuntime.setSheenColorFactorsOnKhronosPbrMaterial(material, factor)
    }

    /**
     * Sets the sheen roughness texture.
     *
     * By default this is a white texture, where all pixels are [1, 1, 1, 1]. In other words, if
     * this is left as default, the sheen roughness values will always come from the sheen roughness
     * factor.
     *
     * @param texture The [Texture] to be used. It defines the sheen layer's roughness, using the
     *   alpha channel in linear space.
     * @param sampler The [TextureSampler] to be used when sampling the texture.
     */
    @JvmOverloads
    @MainThread
    public fun setSheenRoughnessTexture(
        texture: Texture,
        sampler: TextureSampler = TextureSampler(),
    ) {
        session.renderingRuntime.setSheenRoughnessTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
    }

    /**
     * Sets a scalar multiplier for the sheen layer's roughness.
     *
     * @param factor The sheen roughness factor. Default is 0.0. Valid values are between 0.0 and
     *   1.0, inclusive.
     */
    @MainThread
    public fun setSheenRoughnessFactor(@FloatRange(from = 0.0, to = 1.0) factor: Float) {
        session.renderingRuntime.setSheenRoughnessFactorOnKhronosPbrMaterial(material, factor)
    }

    /**
     * Sets the alpha cutoff threshold.
     *
     * This value is only used when the material's [alphaMode] is [AlphaMode.MASK].
     *
     * @param alphaCutoff The alpha cutoff. Fragments with alpha below this value are discarded.
     *   Default is 0.5. Valid values are between 0.0 and 1.0, inclusive.
     * @throws IllegalArgumentException if the material's alphaMode is not ALPHA_MODE_MASK.
     */
    @MainThread
    public fun setAlphaCutoff(@FloatRange(from = 0.0, to = 1.0) alphaCutoff: Float) {
        check(alphaMode == AlphaMode.MASK) {
            "Alpha cutoff can only be set when the material's alpha mode is set to ALPHA_MODE_MASK."
        }
        session.renderingRuntime.setAlphaCutoffOnKhronosPbrMaterial(material, alphaCutoff)
    }

    public companion object {
        internal suspend fun createAsync(
            renderingRuntime: RenderingRuntime,
            alphaMode: AlphaMode,
            session: Session,
        ): KhronosPbrMaterial {
            val material =
                renderingRuntime.createKhronosPbrMaterial(alphaMode.toRtKhronosPbrMaterialSpec())
            return KhronosPbrMaterial(material, alphaMode, session)
        }

        /**
         * Asynchronously creates a [KhronosPbrMaterial].
         *
         * @param session The active [Session] in which to create the material.
         * @param alphaMode The [AlphaMode] to use for the material.
         * @return The newly created [KhronosPbrMaterial].
         */
        @MainThread
        @JvmStatic
        public suspend fun create(session: Session, alphaMode: AlphaMode): KhronosPbrMaterial {
            return createAsync(session.renderingRuntime, alphaMode, session)
        }
    }
}
