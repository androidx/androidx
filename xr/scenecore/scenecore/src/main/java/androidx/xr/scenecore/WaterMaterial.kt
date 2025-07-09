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

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.ResolvableFuture
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.MaterialResource as RtMaterial
import com.google.common.util.concurrent.ListenableFuture

/** Represents a Material in SceneCore. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class Material(internal val material: RtMaterial?)

/** A Material which implements a water effect. */
// TODO(b/396201066): Add unit tests for this class if we end up making it public.
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class WaterMaterial
internal constructor(
    internal val materialResource: RtMaterial,
    internal val isAlphaMapVersion: Boolean,
    internal val session: Session,
) : Material(materialResource) {

    /**
     * Disposes the given water material resource.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * Currently, a glTF model (which this material will be used with) can't be disposed. This means
     * that calling dispose on the material will lead to a crash if the call is made out of order,
     * that is, if the material is disposed before the glTF model that uses it.
     */
    // TODO(b/376277201): Provide Session.GltfModel.dispose().
    @MainThread
    public fun dispose() {
        session.platformAdapter.destroyWaterMaterial(materialResource)
    }

    /**
     * Sets the reflection map texture for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param reflectionMap The [CubeMapTexture] to be used as the reflection cube.
     */
    @MainThread
    public fun setReflectionMap(reflectionMap: CubeMapTexture) {
        session.platformAdapter.setReflectionMapOnWaterMaterial(
            materialResource,
            reflectionMap.texture,
        )
    }

    /**
     * Sets the normal map texture for the water material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param normalMap The [Texture] to be used as the normal map.
     */
    @MainThread
    public fun setNormalMap(normalMap: Texture) {
        session.platformAdapter.setNormalMapOnWaterMaterial(materialResource, normalMap.texture)
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
        session.platformAdapter.setNormalTilingOnWaterMaterial(materialResource, normalTiling)
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
        session.platformAdapter.setNormalSpeedOnWaterMaterial(materialResource, normalSpeed)
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
            session.platformAdapter.setAlphaStepMultiplierOnWaterMaterial(
                materialResource,
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
     * @throws IllegalStateException if the water material is not the alpha map version.
     */
    @MainThread
    public fun setAlphaMap(alphaMap: Texture) {
        if (isAlphaMapVersion) {
            session.platformAdapter.setAlphaMapOnWaterMaterial(materialResource, alphaMap.texture)
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
            session.platformAdapter.setNormalZOnWaterMaterial(materialResource, normalZ)
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
            session.platformAdapter.setNormalBoundaryOnWaterMaterial(
                materialResource,
                normalBoundary,
            )
        } else {
            throw IllegalStateException(
                "The normal boundary can only be set for alpha map version of the water material."
            )
        }
    }

    public companion object {
        // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for
        // classes
        // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
        // warning, however, we get a build error - go/bugpattern/RestrictTo.
        @SuppressWarnings("RestrictTo")
        internal fun createAsync(
            platformAdapter: JxrPlatformAdapter,
            isAlphaMapVersion: Boolean,
            session: Session,
        ): ListenableFuture<WaterMaterial> {
            val materialResourceFuture = platformAdapter.createWaterMaterial(isAlphaMapVersion)
            val materialFuture = ResolvableFuture.create<WaterMaterial>()

            // TODO: b/375070346 - remove this `!!` when we're sure the future is non-null.
            materialResourceFuture!!.addListener(
                {
                    try {
                        val material = materialResourceFuture.get()
                        materialFuture.set(WaterMaterial(material, isAlphaMapVersion, session))
                    } catch (e: Exception) {
                        if (e is InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                        materialFuture.setException(e)
                    }
                },
                Runnable::run,
            )
            return materialFuture
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
            return WaterMaterial.createAsync(session.platformAdapter, isAlphaMapVersion, session)
                .awaitSuspending()
        }
    }
}
