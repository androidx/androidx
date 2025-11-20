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

package androidx.xr.scenecore.impl.impress;

import androidx.annotation.RestrictTo;
import androidx.xr.scenecore.runtime.TextureSampler;

import org.jspecify.annotations.NonNull;

/**
 * A Material with Physically Based Rendering parameters (https://www.khronos.org/gltf/pbr) that can
 * be used to override internal meshes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class KhronosPbrMaterial extends Material {
    private final ImpressApi mImpressApi;

    private KhronosPbrMaterial(Builder builder) {
        super(builder.mImpressApi, builder.mNativeMaterial);

        this.mImpressApi = builder.mImpressApi;
    }

    /**
     * Sets the base color texture for the Khronos PBR material. This texture defines the albedo or
     * diffuse color of the material.
     *
     * @param baseColorTexture The native handle of the base color texture
     * @param sampler The sampler used for the base color texture
     */
    public void setBaseColorTexture(long baseColorTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setBaseColorTextureOnKhronosPbrMaterial(
                getNativeHandle(), baseColorTexture, sampler);
    }

    /**
     * Sets the UV transformation matrix for the base color texture. This allows for scaling,
     * rotating, and translating the texture coordinates.
     *
     * @param ux The X component of the first row of the transformation matrix.
     * @param uy The Y component of the first row of the transformation matrix.
     * @param uz The Z component of the first row of the transformation matrix.
     * @param vx The X component of the second row of the transformation matrix.
     * @param vy The Y component of the second row of the transformation matrix.
     * @param vz The Z component of the second row of the transformation matrix.
     * @param wx The X component of the third row of the transformation matrix.
     * @param wy The Y component of the third row of the transformation matrix.
     * @param wz The Z component of the third row of the transformation matrix.
     */
    public void setBaseColorUvTransform(
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        mImpressApi.setBaseColorUvTransformOnKhronosPbrMaterial(
                getNativeHandle(), ux, uy, uz, vx, vy, vz, wx, wy, wz);
    }

    /**
     * Sets the base color factors for the Khronos PBR material. These factors multiply the base
     * color texture or defines a uniform base color.
     *
     * @param x The X component of the base color factors.
     * @param y The Y component of the base color factors.
     * @param z The Z component of the base color factors.
     * @param w The W component of the base color factors.
     */
    public void setBaseColorFactors(float x, float y, float z, float w) {
        mImpressApi.setBaseColorFactorsOnKhronosPbrMaterial(getNativeHandle(), x, y, z, w);
    }

    /**
     * Sets the metallic-roughness texture for the Khronos PBR material. This texture defines the
     * metallic and roughness properties of the material.
     *
     * @param metallicRoughnessTexture The native handle of the metallic-roughness texture
     * @param sampler The sampler used for the metallic-roughness texture
     */
    public void setMetallicRoughnessTexture(
            long metallicRoughnessTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setMetallicRoughnessTextureOnKhronosPbrMaterial(
                getNativeHandle(), metallicRoughnessTexture, sampler);
    }

    /**
     * Sets the UV transformation matrix for the metallic-roughness texture. Controls how the
     * metallic-roughness texture is mapped onto the surface.
     *
     * @param ux The X component of the first row of the transformation matrix.
     * @param uy The Y component of the first row of the transformation matrix.
     * @param uz The Z component of the first row of the transformation matrix.
     * @param vx The X component of the second row of the transformation matrix.
     * @param vy The Y component of the second row of the transformation matrix.
     * @param vz The Z component of the second row of the transformation matrix.
     * @param wx The X component of the third row of the transformation matrix.
     * @param wy The Y component of the third row of the transformation matrix.
     * @param wz The Z component of the third row of the transformation matrix.
     */
    public void setMetallicRoughnessUvTransform(
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        mImpressApi.setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
                getNativeHandle(), ux, uy, uz, vx, vy, vz, wx, wy, wz);
    }

    /**
     * Sets the metallic factor for the Khronos PBR material. Controls the metalness of the
     * material, ranging from non-metal to metal.
     *
     * @param factor The metallic factor.
     */
    public void setMetallicFactor(float factor) {
        mImpressApi.setMetallicFactorOnKhronosPbrMaterial(getNativeHandle(), factor);
    }

    /**
     * Sets the roughness factor for the Khronos PBR material. Controls the surface roughness,
     * affecting the sharpness of reflections.
     *
     * @param factor The roughness factor.
     */
    public void setRoughnessFactor(float factor) {
        mImpressApi.setRoughnessFactorOnKhronosPbrMaterial(getNativeHandle(), factor);
    }

    /**
     * Sets the normal map texture for the Khronos PBR material. This texture perturbs the surface
     * normals, creating detailed surface features.
     *
     * @param normalTexture The native handle of the normal map texture
     * @param sampler The sampler used for the normal map texture
     */
    public void setNormalTexture(long normalTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setNormalTextureOnKhronosPbrMaterial(getNativeHandle(), normalTexture, sampler);
    }

    /**
     * Sets the UV transformation matrix for the normal map texture. Adjusts the mapping of the
     * normal map texture.
     *
     * @param ux The X component of the first row of the transformation matrix.
     * @param uy The Y component of the first row of the transformation matrix.
     * @param uz The Z component of the first row of the transformation matrix.
     * @param vx The X component of the second row of the transformation matrix.
     * @param vy The Y component of the second row of the transformation matrix.
     * @param vz The Z component of the second row of the transformation matrix.
     * @param wx The X component of the third row of the transformation matrix.
     * @param wy The Y component of the third row of the transformation matrix.
     * @param wz The Z component of the third row of the transformation matrix.
     */
    public void setNormalUvTransform(
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        mImpressApi.setNormalUvTransformOnKhronosPbrMaterial(
                getNativeHandle(), ux, uy, uz, vx, vy, vz, wx, wy, wz);
    }

    /**
     * Sets the factor of the normal map effect. Controls the strength of the normal map's
     * influence.
     *
     * @param factor The factor of the normal map.
     */
    public void setNormalFactor(float factor) {
        mImpressApi.setNormalFactorOnKhronosPbrMaterial(getNativeHandle(), factor);
    }

    /**
     * Sets the ambient occlusion texture for the Khronos PBR material. Simulates the occlusion of
     * ambient light by surface details.
     *
     * @param ambientOcclusionTexture The native handle of the ambient occlusion texture
     * @param sampler The sampler used for the ambient occlusion texture
     */
    public void setAmbientOcclusionTexture(
            long ambientOcclusionTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setAmbientOcclusionTextureOnKhronosPbrMaterial(
                getNativeHandle(), ambientOcclusionTexture, sampler);
    }

    /**
     * Sets the UV transformation matrix for the ambient occlusion texture. Controls the mapping of
     * the ambient occlusion texture.
     *
     * @param ux The X component of the first row of the transformation matrix.
     * @param uy The Y component of the first row of the transformation matrix.
     * @param uz The Z component of the first row of the transformation matrix.
     * @param vx The X component of the second row of the transformation matrix.
     * @param vy The Y component of the second row of the transformation matrix.
     * @param vz The Z component of the second row of the transformation matrix.
     * @param wx The X component of the third row of the transformation matrix.
     * @param wy The Y component of the third row of the transformation matrix.
     * @param wz The Z component of the third row of the transformation matrix.
     */
    public void setAmbientOcclusionUvTransform(
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        mImpressApi.setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
                getNativeHandle(), ux, uy, uz, vx, vy, vz, wx, wy, wz);
    }

    /**
     * Sets the factor of the ambient occlusion effect.
     *
     * @param factor The factor of the ambient occlusion.
     */
    public void setAmbientOcclusionFactor(float factor) {
        mImpressApi.setAmbientOcclusionFactorOnKhronosPbrMaterial(getNativeHandle(), factor);
    }

    /**
     * Sets the emissive texture for the Khronos PBR material. Defines the light emitted by the
     * material.
     *
     * @param emissiveTexture The native handle of the emissive texture
     * @param sampler The sampler used for the emissive texture
     */
    public void setEmissiveTexture(long emissiveTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setEmissiveTextureOnKhronosPbrMaterial(
                getNativeHandle(), emissiveTexture, sampler);
    }

    /**
     * Sets the UV transformation matrix for the emissive texture.
     *
     * @param ux The X component of the first row of the transformation matrix.
     * @param uy The Y component of the first row of the transformation matrix.
     * @param uz The Z component of the first row of the transformation matrix.
     * @param vx The X component of the second row of the transformation matrix.
     * @param vy The Y component of the second row of the transformation matrix.
     * @param vz The Z component of the second row of the transformation matrix.
     * @param wx The X component of the third row of the transformation matrix.
     * @param wy The Y component of the third row of the transformation matrix.
     * @param wz The Z component of the third row of the transformation matrix.
     */
    public void setEmissiveUvTransform(
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        mImpressApi.setEmissiveUvTransformOnKhronosPbrMaterial(
                getNativeHandle(), ux, uy, uz, vx, vy, vz, wx, wy, wz);
    }

    /**
     * Sets the emissive color factors for the Khronos PBR material. Multiplies the emissive texture
     * or defines a uniform emissive color.
     *
     * @param x The X component of the emissive factors.
     * @param y The Y component of the emissive factors.
     * @param z The Z component of the emissive factors.
     */
    public void setEmissiveFactors(float x, float y, float z) {
        mImpressApi.setEmissiveFactorsOnKhronosPbrMaterial(getNativeHandle(), x, y, z);
    }

    /**
     * Sets the clearcoat texture for the Khronos PBR material. Adds a clearcoat layer to the
     * material, affecting reflections.
     *
     * @param clearcoatTexture The native handle of the clearcoat texture
     * @param sampler The sampler used for the clearcoat texture
     */
    public void setClearcoatTexture(long clearcoatTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setClearcoatTextureOnKhronosPbrMaterial(
                getNativeHandle(), clearcoatTexture, sampler);
    }

    /**
     * Sets the clearcoat normal texture for the Khronos PBR material. Perturbs the normals of the
     * clearcoat layer.
     *
     * @param clearcoatNormalTexture The native handle of the clearcoat normal texture
     * @param sampler The sampler used for the clearcoat normal texture
     */
    public void setClearcoatNormalTexture(
            long clearcoatNormalTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setClearcoatNormalTextureOnKhronosPbrMaterial(
                getNativeHandle(), clearcoatNormalTexture, sampler);
    }

    /**
     * Sets the clearcoat roughness texture for the Khronos PBR material. Controls the roughness of
     * the clearcoat layer.
     *
     * @param clearcoatRoughnessTexture The native handle of the clearcoat roughness texture
     * @param sampler The sampler used for the clearcoat rougness texture
     */
    public void setClearcoatRoughnessTexture(
            long clearcoatRoughnessTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setClearcoatRoughnessTextureOnKhronosPbrMaterial(
                getNativeHandle(), clearcoatRoughnessTexture, sampler);
    }

    /**
     * Sets the clearcoat factor for the Khronos PBR material. Multiplies the clearcoat texture or
     * defines a uniform clearcoat color.
     *
     * @param intensity The intensity of the clearcoat.
     * @param roughness The roughness of the clearcoat.
     * @param normal The normal of the clearcoat.
     */
    public void setClearcoatFactors(float intensity, float roughness, float normal) {
        mImpressApi.setClearcoatFactorsOnKhronosPbrMaterial(
                getNativeHandle(), intensity, roughness, normal);
    }

    /**
     * Sets the sheen color texture for the Khronos PBR material. Defines the color of the sheen
     * effect, visible at grazing angles.
     *
     * @param sheenColorTexture The native handle of the sheen color texture
     * @param sampler The sampler used for the sheen color texture
     */
    public void setSheenColorTexture(long sheenColorTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setSheenColorTextureOnKhronosPbrMaterial(
                getNativeHandle(), sheenColorTexture, sampler);
    }

    /**
     * Sets the sheen color factors for the Khronos PBR material. Multiplies the sheen color texture
     * or defines a uniform sheen color.
     *
     * @param x The X component of the sheen color factors.
     * @param y The Y component of the sheen color factors.
     * @param z The Z component of the sheen color factors.
     */
    public void setSheenColorFactors(float x, float y, float z) {
        mImpressApi.setSheenColorFactorsOnKhronosPbrMaterial(getNativeHandle(), x, y, z);
    }

    /**
     * Sets the sheen roughness texture for the Khronos PBR material. Controls the roughness of the
     * sheen effect.
     *
     * @param sheenRoughnessTexture The native handle of the sheen roughness texture
     * @param sampler The sampler used for the sheen roughness texture
     */
    public void setSheenRoughnessTexture(
            long sheenRoughnessTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setSheenRoughnessTextureOnKhronosPbrMaterial(
                getNativeHandle(), sheenRoughnessTexture, sampler);
    }

    /**
     * Sets the sheen roughness factor for the Khronos PBR material. Controls the roughness of the
     * sheen effect.
     *
     * @param factor The sheen roughness factor.
     */
    public void setSheenRoughnessFactor(float factor) {
        mImpressApi.setSheenRoughnessFactorOnKhronosPbrMaterial(getNativeHandle(), factor);
    }

    /**
     * Sets the transmission texture for the Khronos PBR material. Defines the transmission of light
     * through the material.
     *
     * @param transmissionTexture The native handle of the transmission texture
     * @param sampler The sampler used for the transmission texture
     */
    public void setTransmissionTexture(long transmissionTexture, @NonNull TextureSampler sampler) {
        mImpressApi.setTransmissionTextureOnKhronosPbrMaterial(
                getNativeHandle(), transmissionTexture, sampler);
    }

    /**
     * Sets the UV transformation matrix for the transmission texture.
     *
     * @param ux The X component of the first row of the transformation matrix.
     * @param uy The Y component of the first row of the transformation matrix.
     * @param uz The Z component of the first row of the transformation matrix.
     * @param vx The X component of the second row of the transformation matrix.
     * @param vy The Y component of the second row of the transformation matrix.
     * @param vz The Z component of the second row of the transformation matrix.
     * @param wx The X component of the third row of the transformation matrix.
     * @param wy The Y component of the third row of the transformation matrix.
     * @param wz The Z component of the third row of the transformation matrix.
     */
    public void setTransmissionUvTransform(
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        mImpressApi.setTransmissionUvTransformOnKhronosPbrMaterial(
                getNativeHandle(), ux, uy, uz, vx, vy, vz, wx, wy, wz);
    }

    /**
     * Sets the transmission factor for the Khronos PBR material. Controls the amount of light
     * transmitted through the material.
     *
     * @param factor The transmission factor.
     */
    public void setTransmissionFactor(float factor) {
        mImpressApi.setTransmissionFactorOnKhronosPbrMaterial(getNativeHandle(), factor);
    }

    /**
     * Sets the index of refraction for the Khronos PBR material. Defines how much light bends when
     * entering the material.
     *
     * @param indexOfRefraction The index of refraction.
     */
    public void setIndexOfRefraction(float indexOfRefraction) {
        mImpressApi.setIndexOfRefractionOnKhronosPbrMaterial(getNativeHandle(), indexOfRefraction);
    }

    /**
     * Sets the alpha cutoff for the Khronos PBR material. Defines the threshold for transparency,
     * used for cutout effects.
     *
     * @param alphaCutoff The alpha cutoff value.
     */
    public void setAlphaCutoff(float alphaCutoff) {
        mImpressApi.setAlphaCutoffOnKhronosPbrMaterial(getNativeHandle(), alphaCutoff);
    }

    /** Use Builder to construct a KhronosPbrMaterial object instance. */
    public static class Builder {
        private ImpressApi mImpressApi;
        private long mNativeMaterial = -1;

        /** Sets the Impress API. */
        @NonNull
        public Builder setImpressApi(@NonNull ImpressApi impressApi) {
            mImpressApi = impressApi;
            return this;
        }

        /** Sets the native material. */
        @NonNull
        public Builder setNativeMaterial(long nativeMaterial) {
            mNativeMaterial = nativeMaterial;
            return this;
        }

        /** Builds the Khronos PBR material. */
        @NonNull
        public KhronosPbrMaterial build() {
            if (mImpressApi == null || mNativeMaterial == -1) {
                throw new IllegalStateException("Khronos PBR material not built properly.");
            }
            return new KhronosPbrMaterial(this);
        }
    }
}
