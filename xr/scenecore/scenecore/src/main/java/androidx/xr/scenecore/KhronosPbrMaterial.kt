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
import androidx.concurrent.futures.ResolvableFuture
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.MaterialResource as RtMaterial
import androidx.xr.runtime.math.Matrix3
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4
import com.google.common.util.concurrent.ListenableFuture

/**
 * A Material which implements the Khronos Physically Based Rendering (PBR) spec. The Khronos spec
 * for PBR parameters can be found at https://www.khronos.org/gltf/pbr.
 *
 * This API will be re-designed once it goes through local AXR API council review (b/420551533).
 */
// TODO(b/396201066): Add unit tests for this class if we end up making it public.
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class KhronosPbrMaterial
internal constructor(
    internal val materialResource: RtMaterial,
    internal val spec: KhronosPbrMaterialSpec,
    internal val session: Session,
) : Material(materialResource) {

    /**
     * Disposes the given Khronos PBR material resource.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * Destroys the native (and Java object) corresponding to that Khronos PBR material.
     */
    // TODO(b/376277201): Provide Session.GltfModel.dispose().
    @MainThread
    public fun dispose() {
        session.platformAdapter.destroyKhronosPbrMaterial(materialResource)
    }

    /**
     * Sets the material's base color using a texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the base color texture. The texture parameter
     *   defines the diffuse albedo for non-metallic surfaces and the specular color for metallic
     *   surfaces, typically in sRGB color space.
     */
    @MainThread
    public fun setBaseColorTexture(texture: Texture) {
        session.platformAdapter.setBaseColorTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Applies a 2D transformation to the UV coordinates of the base color texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param transform The [Matrix3] to be used for the base color texture UV transformation. The
     *   transform parameter is a 3x3 matrix for scaling, rotation, or translation.
     */
    @MainThread
    public fun setBaseColorUvTransform(transform: Matrix3) {
        session.platformAdapter.setBaseColorUvTransformOnKhronosPbrMaterial(
            materialResource,
            transform,
        )
    }

    /**
     * Sets a linear multiplier for the base color.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param factors The [Vector4] (RGBA) to be used as the base color factors. These factors are
     *   applied to the base color texture or define a solid color if no texture is used.
     */
    @MainThread
    public fun setBaseColorFactors(factors: Vector4) {
        session.platformAdapter.setBaseColorFactorsOnKhronosPbrMaterial(materialResource, factors)
    }

    /**
     * Sets a texture defining metallic and roughness properties.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the metallic-roughness texture. The texture
     *   typically uses its blue channel for metallic and green channel for roughness, in linear
     *   space.
     */
    @MainThread
    public fun setMetallicRoughnessTexture(texture: Texture) {
        session.platformAdapter.setMetallicRoughnessTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Applies a 2D transformation to the UV coordinates of the metallic-roughness texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param transform The [Matrix3] to be used for the metallic-roughness texture UV
     *   transformation. The transform parameter is a 3x3 matrix.
     */
    @MainThread
    public fun setMetallicRoughnessUvTransform(transform: Matrix3) {
        session.platformAdapter.setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
            materialResource,
            transform,
        )
    }

    /**
     * Sets a scalar multiplier for the material's metallic property.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param factor The metallic factor. If no metallic-roughness texture is used, this factor
     *   directly defines metallicity.
     */
    @MainThread
    public fun setMetallicFactor(factor: Float) {
        session.platformAdapter.setMetallicFactorOnKhronosPbrMaterial(materialResource, factor)
    }

    /**
     * Sets a scalar multiplier for the material's roughness.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param factor The roughness factor. If no metallic-roughness texture is used, this factor
     *   directly defines roughness.
     */
    @MainThread
    public fun setRoughnessFactor(factor: Float) {
        session.platformAdapter.setRoughnessFactorOnKhronosPbrMaterial(materialResource, factor)
    }

    /**
     * Sets the normal map texture for surface detail.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the normal map. The texture provides normal
     *   information, typically in tangent space and linear color.
     */
    @MainThread
    public fun setNormalTexture(texture: Texture) {
        session.platformAdapter.setNormalTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Applies a 2D transformation to the UV coordinates of the normal texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param transform The [Matrix3] to be used for the normal map texture UV transformation. The
     *   transform parameter is a 3x3 matrix.
     */
    @MainThread
    public fun setNormalUvTransform(transform: Matrix3) {
        session.platformAdapter.setNormalUvTransformOnKhronosPbrMaterial(
            materialResource,
            transform,
        )
    }

    /**
     * Controls the intensity of the normal map effect.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param factor The normal map factor. The factor scales the perceived depth of the normal
     *   details.
     */
    @MainThread
    public fun setNormalFactor(factor: Float) {
        session.platformAdapter.setNormalFactorOnKhronosPbrMaterial(materialResource, factor)
    }

    /**
     * Sets the ambient occlusion texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the ambient occlusion texture. The texture defines
     *   how much ambient light reaches surface points, typically using the red channel in linear
     *   space.
     */
    @MainThread
    public fun setAmbientOcclusionTexture(texture: Texture) {
        session.platformAdapter.setAmbientOcclusionTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Applies a 2D transformation to the UV coordinates of the ambient occlusion texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param transform The [Matrix3] to be used for the ambient occlusion texture UV
     *   transformation. The transform parameter is a 3x3 matrix.
     */
    @MainThread
    public fun setAmbientOcclusionUvTransform(transform: Matrix3) {
        session.platformAdapter.setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
            materialResource,
            transform,
        )
    }

    /**
     * Controls the strength of the ambient occlusion effect.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param factor The ambient occlusion factor. The factor scales the intensity of the ambient
     *   occlusion.
     */
    @MainThread
    public fun setAmbientOcclusionFactor(factor: Float) {
        session.platformAdapter.setAmbientOcclusionFactorOnKhronosPbrMaterial(
            materialResource,
            factor,
        )
    }

    /**
     * Sets the emissive texture, defining light emitted by the material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the emissive texture. The texture is typically in
     *   sRGB color space.
     */
    @MainThread
    public fun setEmissiveTexture(texture: Texture) {
        session.platformAdapter.setEmissiveTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Applies a 2D transformation to the UV coordinates of the emissive texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param transform The [Matrix3] to be used for the emissive texture UV transformation. The
     *   transform parameter is a 3x3 matrix.
     */
    @MainThread
    public fun setEmissiveUvTransform(transform: Matrix3) {
        session.platformAdapter.setEmissiveUvTransformOnKhronosPbrMaterial(
            materialResource,
            transform,
        )
    }

    /**
     * Sets a linear multiplier for the emissive color.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param factors The [Vector3] (RGB) to be used as the emissive factors. These factors are
     *   applied to the emissive texture or define a solid emissive color if no texture is used.
     */
    @MainThread
    public fun setEmissiveFactors(factors: Vector3) {
        session.platformAdapter.setEmissiveFactorsOnKhronosPbrMaterial(materialResource, factors)
    }

    /**
     * Sets the clearcoat intensity texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the clearcoat texture. The texture defines the
     *   clearcoat layer's strength, typically using the red channel in linear space.
     */
    @MainThread
    public fun setClearcoatTexture(texture: Texture) {
        session.platformAdapter.setClearcoatTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Sets the normal map texture for the clearcoat layer.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the clearcoat normal texture. The texture provides
     *   surface normal details for the clearcoat, in tangent space and linear color.
     */
    @MainThread
    public fun setClearcoatNormalTexture(texture: Texture) {
        session.platformAdapter.setClearcoatNormalTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Sets the clearcoat roughness texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the clearcoat roughness texture. The texture
     *   defines the clearcoat layer's roughness, typically using the green channel in linear space.
     */
    @MainThread
    public fun setClearcoatRoughnessTexture(texture: Texture) {
        session.platformAdapter.setClearcoatRoughnessTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Sets factors for the clearcoat layer, that is, the intensity, roughness, and normal scale.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param intensity The intensity factor for the clearcoat layer.
     * @param roughness The roughness factor for the clearcoat layer.
     * @param normal The normal scale factor for the clearcoat layer's normal map.
     */
    @MainThread
    public fun setClearcoatFactors(intensity: Float, roughness: Float, normal: Float) {
        session.platformAdapter.setClearcoatFactorsOnKhronosPbrMaterial(
            materialResource,
            intensity,
            roughness,
            normal,
        )
    }

    /**
     * Sets the sheen color texture, for materials like fabric.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the sheen color texture. The texture defines the
     *   sheen layer's color, typically in sRGB.
     */
    @MainThread
    public fun setSheenColorTexture(texture: Texture) {
        session.platformAdapter.setSheenColorTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Sets a linear multiplier for the sheen color.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param factors The [Vector3] (RGB) to be used as the sheen color factors. These factors are
     *   applied to the sheen color texture or define a solid sheen color.
     */
    @MainThread
    public fun setSheenColorFactors(factors: Vector3) {
        session.platformAdapter.setSheenColorFactorsOnKhronosPbrMaterial(materialResource, factors)
    }

    /**
     * Sets the sheen roughness texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the sheen roughness texture. The texture defines
     *   the sheen layer's roughness, typically using the alpha channel in linear space.
     */
    @MainThread
    public fun setSheenRoughnessTexture(texture: Texture) {
        session.platformAdapter.setSheenRoughnessTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Sets a scalar multiplier for the sheen layer's roughness.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param factor The sheen roughness factor. If no sheen roughness texture is used, this factor
     *   directly defines sheen roughness.
     */
    @MainThread
    public fun setSheenRoughnessFactor(factor: Float) {
        session.platformAdapter.setSheenRoughnessFactorOnKhronosPbrMaterial(
            materialResource,
            factor,
        )
    }

    /**
     * Sets the transmission texture, defining how much light passes through the material.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param texture The [Texture] to be used as the transmission texture. The texture typically
     *   uses the red channel for the transmission factor, in linear space.
     */
    @MainThread
    public fun setTransmissionTexture(texture: Texture) {
        session.platformAdapter.setTransmissionTextureOnKhronosPbrMaterial(
            materialResource,
            texture.texture,
        )
    }

    /**
     * Applies a 2D transformation to the UV coordinates of the transmission texture.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param transform The [Matrix3] to be used for the transmission texture UV transformation. The
     *   transform parameter is a 3x3 matrix.
     */
    @MainThread
    public fun setTransmissionUvTransform(transform: Matrix3) {
        session.platformAdapter.setTransmissionUvTransformOnKhronosPbrMaterial(
            materialResource,
            transform,
        )
    }

    /**
     * Sets a scalar multiplier for the material's transmission.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param factor The transmission factor. If no transmission texture is used, this factor
     *   directly defines the transmission fraction.
     */
    @MainThread
    public fun setTransmissionFactor(factor: Float) {
        session.platformAdapter.setTransmissionFactorOnKhronosPbrMaterial(materialResource, factor)
    }

    /**
     * Sets the material's index of refraction (IOR).
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param indexOfRefraction The index of refraction. The indexOfRefraction value affects how
     *   light bends when passing through or reflecting off the material.
     */
    @MainThread
    public fun setIndexOfRefraction(indexOfRefraction: Float) {
        session.platformAdapter.setIndexOfRefractionOnKhronosPbrMaterial(
            materialResource,
            indexOfRefraction,
        )
    }

    /**
     * Sets the alpha cutoff threshold for materials in "MASK" alpha mode.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param alphaCutoff The alpha cutoff. Fragments with alpha below alphaCutoff are discarded.
     */
    @MainThread
    public fun setAlphaCutoff(alphaCutoff: Float) {
        session.platformAdapter.setAlphaCutoffOnKhronosPbrMaterial(materialResource, alphaCutoff)
    }

    public companion object {
        // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for
        // classes
        // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
        // warning, however, we get a build error - go/bugpattern/RestrictTo.
        @SuppressWarnings("RestrictTo")
        internal fun createAsync(
            platformAdapter: JxrPlatformAdapter,
            spec: KhronosPbrMaterialSpec,
            session: Session,
        ): ListenableFuture<KhronosPbrMaterial> {
            val materialResourceFuture =
                platformAdapter.createKhronosPbrMaterial(spec.toRtKhronosPbrMaterialSpec())
            val materialFuture = ResolvableFuture.create<KhronosPbrMaterial>()

            // TODO: b/375070346 - remove this `!!` when we're sure the future is non-null.
            materialResourceFuture!!.addListener(
                {
                    try {
                        val material = materialResourceFuture.get()
                        materialFuture.set(KhronosPbrMaterial(material, spec, session))
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
         * Asynchronously creates a Khronos PBR material based on the provided
         * KhronosPbrMaterialSpec. This specification allows for initial setup of material
         * parameters.
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session The [Session] to use for loading the model.
         * @param spec The [KhronosPbrMaterialSpec] to use for the material.
         * @return a ListenableFuture<KhronosPbrMaterial>. Listeners will be called on the main
         *   thread if Runnable::run is supplied.
         */
        @MainThread
        @JvmStatic
        public fun create(
            session: Session,
            spec: KhronosPbrMaterialSpec,
        ): ListenableFuture<KhronosPbrMaterial> {
            return KhronosPbrMaterial.createAsync(session.platformAdapter, spec, session)
        }
    }
}
