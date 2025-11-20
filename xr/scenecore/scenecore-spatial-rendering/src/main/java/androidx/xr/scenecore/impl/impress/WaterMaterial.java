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

/** A Material which renders water effects using the built-in Impress water material. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WaterMaterial extends Material {
    private final ImpressApi mImpressApi;

    private WaterMaterial(Builder builder) {
        super(builder.mImpressApi, builder.mNativeMaterial);

        mImpressApi = builder.mImpressApi;
    }

    /**
     * Sets the reflection map for the water material.
     *
     * @param reflectionMap The native handle of the texture to be used as the reflection map.
     * @param sampler The sampler used for the reflection map texture.
     */
    public void setReflectionMap(long reflectionMap, @NonNull TextureSampler sampler) {
        mImpressApi.setReflectionMapOnWaterMaterial(getNativeHandle(), reflectionMap, sampler);
    }

    /**
     * Sets the normal map for the water material.
     *
     * @param normalMap The native handle of the texture to be used as the normal map.
     * @param sampler The sampler used for the normal map texture.
     */
    public void setNormalMap(long normalMap, @NonNull TextureSampler sampler) {
        mImpressApi.setNormalMapOnWaterMaterial(getNativeHandle(), normalMap, sampler);
    }

    /**
     * Sets the normal tiling for the water material.
     *
     * @param normalTiling The tiling of the normal map.
     */
    public void setNormalTiling(float normalTiling) {
        mImpressApi.setNormalTilingOnWaterMaterial(getNativeHandle(), normalTiling);
    }

    /**
     * Sets the normal speed for the water material.
     *
     * @param normalSpeed The speed of the normal map.
     */
    public void setNormalSpeed(float normalSpeed) {
        mImpressApi.setNormalSpeedOnWaterMaterial(getNativeHandle(), normalSpeed);
    }

    /**
     * Sets the alpha step multiplier for the water material.
     *
     * @param alphaStepMultiplier The alpha step multiplier.
     */
    public void setAlphaStepMultiplier(float alphaStepMultiplier) {
        mImpressApi.setAlphaStepMultiplierOnWaterMaterial(getNativeHandle(), alphaStepMultiplier);
    }

    /**
     * Sets the alpha map for the water material.
     *
     * @param alphaMap The native handle of the texture to be used as the alpha map.
     * @param sampler The sampler used for the alpha map texture.
     */
    public void setAlphaMap(long alphaMap, @NonNull TextureSampler sampler) {
        mImpressApi.setAlphaMapOnWaterMaterial(getNativeHandle(), alphaMap, sampler);
    }

    /**
     * Sets the normal z for the water material.
     *
     * @param normalZ The normal z.
     */
    public void setNormalZ(float normalZ) {
        mImpressApi.setNormalZOnWaterMaterial(getNativeHandle(), normalZ);
    }

    /**
     * Sets the normal boundary for the water material.
     *
     * @param normalBoundary The normal boundary.
     */
    public void setNormalBoundary(float normalBoundary) {
        mImpressApi.setNormalBoundaryOnWaterMaterial(getNativeHandle(), normalBoundary);
    }

    /** Use Builder to construct a WaterMaterial object instance. */
    public static class Builder {
        private ImpressApi mImpressApi;
        private long mNativeMaterial = -1;

        /** Sets the Impress API. */
        @NonNull
        public Builder setImpressApi(@NonNull ImpressApi impressApi) {
            this.mImpressApi = impressApi;
            return this;
        }

        /** Sets the native material. */
        @NonNull
        public Builder setNativeMaterial(long nativeMaterial) {
            this.mNativeMaterial = nativeMaterial;
            return this;
        }

        /** Builds the WaterMaterial. */
        @NonNull
        public WaterMaterial build() {
            if (mImpressApi == null || mNativeMaterial == -1) {
                throw new IllegalStateException("Water material not built properly.");
            }
            return new WaterMaterial(this);
        }
    }
}
