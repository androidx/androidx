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

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.MaterialResource as RtMaterial
import androidx.xr.scenecore.runtime.RenderingRuntime

/** A Material which implements a water effect. */
// TODO(b/396201066): Add unit tests for this class if we end up making it public.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WaterMaterial
internal constructor(
    override val material: RtMaterial,
    internal val isAlphaMapVersion: Boolean,
    internal val session: Session,
) : Material {

    /**
     * Closes the [WaterMaterial] and releases its underlying graphics resources.
     *
     * After being closed, the [WaterMaterial] should not be used further.
     */
    @MainThread
    override public fun close() {
        session.renderingRuntime.destroyWaterMaterial(material)
    }

    /**
     * Sets the reflection map texture for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param reflectionMap The [CubeMapTexture] to be used as the reflection cube.
     * @param sampler The [TextureSampler] to be used when sampling the reflection map texture.
     */
    @MainThread
    public fun setReflectionMap(reflectionMap: CubeMapTexture, sampler: TextureSampler) {
        session.renderingRuntime.setReflectionMapOnWaterMaterial(
            material,
            reflectionMap.texture,
            sampler.toRtTextureSampler(),
        )
    }

    /**
     * Sets the normal map texture for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param normalMap The [Texture] to be used as the normal map.
     * @param sampler The [TextureSampler] to be used when sampling the normal map texture.
     */
    @MainThread
    public fun setNormalMap(normalMap: Texture, sampler: TextureSampler) {
        session.renderingRuntime.setNormalMapOnWaterMaterial(
            material,
            normalMap.texture,
            sampler.toRtTextureSampler(),
        )
    }

    /**
     * Sets the normal tiling for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param normalTiling The tiling of the normal map.
     */
    @MainThread
    public fun setNormalTiling(normalTiling: Float) {
        session.renderingRuntime.setNormalTilingOnWaterMaterial(material, normalTiling)
    }

    /**
     * Sets the normal speed for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param normalSpeed The speed of the normal map.
     */
    @MainThread
    public fun setNormalSpeed(normalSpeed: Float) {
        session.renderingRuntime.setNormalSpeedOnWaterMaterial(material, normalSpeed)
    }

    /**
     * Sets the alpha step multiplier for the alpha map version of the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param alphaStepMultiplier The alpha step multiplier.
     * @throws IllegalStateException if the water material is not the alpha map version.
     */
    @MainThread
    public fun setAlphaStepMultiplier(alphaStepMultiplier: Float) {
        if (isAlphaMapVersion) {
            session.renderingRuntime.setAlphaStepMultiplierOnWaterMaterial(
                material,
                alphaStepMultiplier,
            )
        } else {
            throw IllegalStateException(
                "The alpha step multiplier can only be set for alpha map version of the water material."
            )
        }
    }

    /**
     * Sets the alpha map for the alpha map version of the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param alphaMap The alpha map.
     * @param sampler The [TextureSampler] to be used when sampling the alpha map texture.
     * @throws IllegalStateException if the water material is not the alpha map version.
     */
    @MainThread
    public fun setAlphaMap(alphaMap: Texture, sampler: TextureSampler) {
        if (isAlphaMapVersion) {
            session.renderingRuntime.setAlphaMapOnWaterMaterial(
                material,
                alphaMap.texture,
                sampler.toRtTextureSampler(),
            )
        } else {
            throw IllegalStateException(
                "The alpha map can only be set for alpha map version of the water material."
            )
        }
    }

    /**
     * Sets the normal z for the alpha map version of the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param normalZ The normal z.
     * @throws IllegalStateException if the water material is not the alpha map version.
     */
    @MainThread
    public fun setNormalZ(normalZ: Float) {
        if (isAlphaMapVersion) {
            session.renderingRuntime.setNormalZOnWaterMaterial(material, normalZ)
        } else {
            throw IllegalStateException(
                "The normal Z can only be set for alpha map version of the water material.."
            )
        }
    }

    /**
     * Sets the normal boundary for the alpha map version of the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param normalBoundary The normal boundary.
     * @throws IllegalStateException if the water material is not the alpha map version.
     */
    @MainThread
    public fun setNormalBoundary(normalBoundary: Float) {
        if (isAlphaMapVersion) {
            session.renderingRuntime.setNormalBoundaryOnWaterMaterial(material, normalBoundary)
        } else {
            throw IllegalStateException(
                "The normal boundary can only be set for alpha map version of the water material."
            )
        }
    }

    public companion object {
        internal suspend fun createAsync(
            renderingRuntime: RenderingRuntime,
            isAlphaMapVersion: Boolean,
            session: Session,
        ): WaterMaterial {
            val material = renderingRuntime.createWaterMaterial(isAlphaMapVersion)
            return WaterMaterial(material, isAlphaMapVersion, session)
        }

        /**
         * Public factory function for a [WaterMaterial], where the material is asynchronously
         * loaded.
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session The [Session] to use for loading the model.
         * @param isAlphaMapVersion If the water material should be the alpha map version or not.
         * @return a [WaterMaterial] upon completion.
         */
        @MainThread
        @JvmStatic
        @Suppress("AsyncSuffixFuture")
        public suspend fun create(session: Session, isAlphaMapVersion: Boolean): WaterMaterial {
            return WaterMaterial.createAsync(session.renderingRuntime, isAlphaMapVersion, session)
        }
    }
}
