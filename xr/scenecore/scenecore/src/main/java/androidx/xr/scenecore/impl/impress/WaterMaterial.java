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

import org.jspecify.annotations.NonNull;

/** A Material which renders water effects using the built-in Impress water material. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class WaterMaterial extends Material {
    private final ImpressApi impressApi;

    private WaterMaterial(Builder builder) {
        super(builder.impressApi, builder.nativeMaterial);

        this.impressApi = builder.impressApi;
    }

    /**
     * Sets the reflection map for the water material.
     *
     * @param reflectionMap The native handle of the texture to be used as the reflection map.
     */
    public void setReflectionMap(long reflectionMap) {
        impressApi.setReflectionMapOnWaterMaterial(getNativeHandle(), reflectionMap);
    }

    /**
     * Sets the normal map for the water material.
     *
     * @param normalMap The native handle of the texture to be used as the normal map.
     */
    public void setNormalMap(long normalMap) {
        impressApi.setNormalMapOnWaterMaterial(getNativeHandle(), normalMap);
    }

    /**
     * Sets the normal tiling for the water material.
     *
     * @param normalTiling The tiling of the normal map.
     */
    public void setNormalTiling(float normalTiling) {
        impressApi.setNormalTilingOnWaterMaterial(getNativeHandle(), normalTiling);
    }

    /**
     * Sets the normal speed for the water material.
     *
     * @param normalSpeed The speed of the normal map.
     */
    public void setNormalSpeed(float normalSpeed) {
        impressApi.setNormalSpeedOnWaterMaterial(getNativeHandle(), normalSpeed);
    }

    /**
     * Sets the alpha step multiplier for the water material.
     *
     * @param alphaStepMultiplier The alpha step multiplier.
     */
    public void setAlphaStepMultiplier(float alphaStepMultiplier) {
        impressApi.setAlphaStepMultiplierOnWaterMaterial(getNativeHandle(), alphaStepMultiplier);
    }

    /**
     * Sets the alpha map for the water material.
     *
     * @param alphaMap The native handle of the texture to be used as the alpha map.
     */
    public void setAlphaMap(long alphaMap) {
        impressApi.setAlphaMapOnWaterMaterial(getNativeHandle(), alphaMap);
    }

    /**
     * Sets the normal z for the water material.
     *
     * @param normalZ The normal z.
     */
    public void setNormalZ(float normalZ) {
        impressApi.setNormalZOnWaterMaterial(getNativeHandle(), normalZ);
    }

    /**
     * Sets the normal boundary for the water material.
     *
     * @param normalBoundary The normal boundary.
     */
    public void setNormalBoundary(float normalBoundary) {
        impressApi.setNormalBoundaryOnWaterMaterial(getNativeHandle(), normalBoundary);
    }

    /** Use Builder to construct a WaterMaterial object instance. */
    public static class Builder {
        private ImpressApi impressApi;
        private long nativeMaterial = -1;

        @NonNull
        public Builder setImpressApi(@NonNull ImpressApi impressApi) {
            this.impressApi = impressApi;
            return this;
        }

        @NonNull
        public Builder setNativeMaterial(long nativeMaterial) {
            this.nativeMaterial = nativeMaterial;
            return this;
        }

        @NonNull
        public WaterMaterial build() {
            if (impressApi == null || nativeMaterial == -1) {
                throw new IllegalStateException("Water material not built properly.");
            }
            return new WaterMaterial(this);
        }
    }
}
