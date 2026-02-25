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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.math.Matrix3
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4

/**
 * RenderingRuntime encapsulates all the platform-specific rendering-related operations. Its
 * responsibilities include toggle the render loop, loading assets, and creating renderable scene
 * entities.
 *
 * It is designed to work in tandem with a [SceneRuntime], which manages the scene graph. An
 * instance of `RenderingRuntime` is always created for a specific [SceneRuntime], and both are
 * expected to operate within the same context and lifecycle.
 *
 * This API is not intended to be used by applications.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface RenderingRuntime : JxrRuntime {
    /**
     * Loads glTF Asset for the given asset name from the assets folder. The Coroutine returned by
     * this method will fire listeners on the UI thread if Runnable::run is supplied.
     *
     * @param assetName The name of the asset to load from the assets folder.
     * @return A glTF model. Will be null if the asset was not found.
     */
    public suspend fun loadGltfByAssetName(assetName: String): GltfModelResource

    /**
     * Loads glTF Asset from a provided byte array. The Coroutine returned by this method will fire
     * listeners on the UI thread if Runnable::run is supplied.
     *
     * @param assetData A gltfAsset in the form of a byte array.
     * @param assetKey The name of the asset to load from the cache.
     * @return A glTF model when it is loaded. Will be null if the asset was not found.
     */
    // TODO(b/397746548): Add InputStream support for loading glTFs.
    // Suppressed to allow CompletableFuture.
    public suspend fun loadGltfByByteArray(
        assetData: ByteArray,
        assetKey: String,
    ): GltfModelResource

    /**
     * Destroys the given glTF model resource.
     *
     * @param gltfModel The glTF model resource to destroy.
     */
    public fun destroyGltfModel(gltfModel: GltfModelResource)

    /**
     * Loads an ExrImage for the given asset name from the assets folder.
     *
     * @param assetName The name of the asset to load from the assets folder.
     * @return An ExrImage. Will be null if the asset was not found.
     */
    public suspend fun loadExrImageByAssetName(assetName: String): ExrImageResource

    /**
     * Loads an ExrImage from a provided byte array.
     *
     * @param assetData An ExrImage in the form of a byte array.
     * @param assetKey The name of the asset to load from the cache.
     * @return An ExrImage. Will be null if the asset was not found.
     */
    public suspend fun loadExrImageByByteArray(
        assetData: ByteArray,
        assetKey: String,
    ): ExrImageResource

    /**
     * Destroys the given EXR image resource.
     *
     * @param exrImage The EXR image resource to destroy.
     */
    public fun destroyExrImage(exrImage: ExrImageResource)

    /**
     * Loads a texture resource for the given asset name or URL. The Coroutine returned by this
     * method will fire listeners on the UI thread if Runnable::run is supplied.
     *
     * @param assetName The name of the texture file to load or the URL of the remote texture.
     * @return A texture.
     */
    public suspend fun loadTexture(assetName: String): TextureResource

    /** Borrows the reflection texture from the currently set environment IBL. */
    public fun borrowReflectionTexture(): TextureResource?

    /**
     * Destroys the given texture resource.
     *
     * @param texture The name of the texture to destroy.
     */
    public fun destroyTexture(texture: TextureResource)

    /**
     * Returns the reflection texture from the given IBL.
     *
     * @param iblToken An ExrImageResource representing a loaded Image-Based Lighting (IBL) asset.
     * @return A TextureResource representing the reflection texture, or null if the texture was not
     *   found.
     */
    public fun getReflectionTextureFromIbl(iblToken: ExrImageResource): TextureResource?

    /**
     * Creates a water material by querying it from the system's built-in materials. The Coroutine
     * returned by this method will fire listeners on the UI thread if Runnable::run is supplied.
     *
     * @param isAlphaMapVersion True if the water material should be the alpha map version.
     * @return A WaterMaterial backed by an imp::WaterMaterial. The WaterMaterial can be destroyed
     *   by passing it to destroyNativeObject.
     */
    public suspend fun createWaterMaterial(isAlphaMapVersion: Boolean): MaterialResource

    /**
     * Destroys the given water material resource.
     *
     * @param material The name of the WaterMaterial to destroy.
     */
    public fun destroyWaterMaterial(material: MaterialResource)

    /**
     * Sets the reflection map texture for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param reflectionMap The handle of the texture to be used as the reflection map.
     * @param sampler The sampler used for the reflection map texture.
     */
    public fun setReflectionMapOnWaterMaterial(
        material: MaterialResource,
        reflectionMap: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the normal map texture for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param normalMap The handle of the texture to be used as the normal map.
     * @param sampler The sampler used for the normal map texture.
     */
    public fun setNormalMapOnWaterMaterial(
        material: MaterialResource,
        normalMap: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the normal tiling for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param normalTiling The tiling to use for the normal map.
     */
    public fun setNormalTilingOnWaterMaterial(material: MaterialResource, normalTiling: Float)

    /**
     * Sets the normal speed for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param normalSpeed The speed to use for the normal map.
     */
    public fun setNormalSpeedOnWaterMaterial(material: MaterialResource, normalSpeed: Float)

    /**
     * Sets the alpha step multiplier for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param alphaStepMultiplier The alpha step multiplier to use for the water material.
     */
    public fun setAlphaStepMultiplierOnWaterMaterial(
        material: MaterialResource,
        alphaStepMultiplier: Float,
    )

    /**
     * Sets the alpha map for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param alphaMap The handle of the texture to be used as the alpha map.
     * @param sampler The sampler used for the alpha map texture.
     */
    public fun setAlphaMapOnWaterMaterial(
        material: MaterialResource,
        alphaMap: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the normal z for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param normalZ The normal z to use for the water material.
     */
    public fun setNormalZOnWaterMaterial(material: MaterialResource, normalZ: Float)

    /**
     * Sets the normal boundary for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param normalBoundary The normal boundary to use for the water material.
     */
    public fun setNormalBoundaryOnWaterMaterial(material: MaterialResource, normalBoundary: Float)

    /**
     * Creates a Khronos PBR material by querying it from the system's built-in materials. The
     * Coroutine returned by this method will fire listeners on the UI thread if Runnable::run is
     * supplied.
     */
    public suspend fun createKhronosPbrMaterial(spec: KhronosPbrMaterialSpec): MaterialResource

    /**
     * Destroys the given Khronos PBR material resource.
     *
     * @param material The KhronosPbrMaterial to destroy.
     */
    public fun destroyKhronosPbrMaterial(material: MaterialResource)

    /**
     * Sets the base color texture for the Khronos PBR material. This texture defines the albedo or
     * diffuse color of the material.
     *
     * @param material The handle of the Khronos PBR material.
     * @param baseColor The handle of the base color texture.
     * @param sampler The sampler used for the base color texture.
     */
    public fun setBaseColorTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        baseColor: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the base color texture. This allows for scaling,
     * rotating, and translating the texture coordinates.
     *
     * @param material The handle of the Khronos PBR material.
     * @param uvTransform The uv coordinates of the transform stored in a matrix.
     */
    public fun setBaseColorUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    )

    /**
     * Sets the base color factors for the Khronos PBR material. These factors multiplies the base
     * color texture or defines a uniform base color.
     *
     * @param material The handle of the Khronos PBR material.
     * @param factors The base colors on the Khronos PBR material.
     */
    public fun setBaseColorFactorsOnKhronosPbrMaterial(material: MaterialResource, factors: Vector4)

    /**
     * Sets the metallic-roughness texture for the Khronos PBR material. This texture defines the
     * metallic and roughness properties of the material.
     *
     * @param material The handle of the Khronos PBR material.
     * @param metallicRoughness The handle of the metallic-roughness texture.
     * @param sampler The sampler used for the metallic-roughness texture.
     */
    public fun setMetallicRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        metallicRoughness: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the metallic-roughness texture. Controls how the
     * metallic-roughness texture is mapped onto the surface.
     *
     * @param material The handle of the Khronos PBR material.
     * @param uvTransform The uv coordinates of the transform stored in a matrix.
     */
    public fun setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    )

    /**
     * Sets the metallic factor for the Khronos PBR material. Controls the metalness of the
     * material, ranging from non-metal to metal.
     *
     * @param material The handle of the Khronos PBR material.
     * @param factor The metallic factor.
     */
    public fun setMetallicFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float)

    /**
     * Sets the roughness factor for the Khronos PBR material. Controls the surface roughness,
     * affecting the sharpness of reflections.
     *
     * @param material The handle of the Khronos PBR material.
     * @param factor The roughness factor.
     */
    public fun setRoughnessFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float)

    /**
     * Sets the normal map texture for the Khronos PBR material. This texture perturbs the surface
     * normals, creating detailed surface features.
     *
     * @param material The handle of the Khronos PBR material.
     * @param normal The handle of the normal map texture.
     * @param sampler The sampler used for the normal texture.
     */
    public fun setNormalTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        normal: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the normal map texture. Adjusts the mapping of the
     * normal map texture.
     *
     * @param material The handle of the Khronos PBR material.
     * @param uvTransform The uv coordinates of the transform stored in a matrix.
     */
    public fun setNormalUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    )

    /**
     * Sets the factor of the normal map effect. Controls the strength of the normal map's
     * influence.
     *
     * @param material The handle of the Khronos PBR material.
     * @param factor The factor of the normal map.
     */
    public fun setNormalFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float)

    /**
     * Sets the ambient occlusion texture for the Khronos PBR material. Simulates the occlusion of
     * ambient light by surface details.
     *
     * @param material The handle of the Khronos PBR material.
     * @param ambientOcclusion The handle of the ambient occlusion texture.
     * @param sampler The sampler used for the ambient occlusion texture.
     */
    public fun setAmbientOcclusionTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        ambientOcclusion: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the ambient occlusion texture. Controls the mapping of
     * the ambient occlusion texture.
     *
     * @param material The native handle of the Khronos PBR material.
     * @param uvTransform The uv coordinates of the transform stored in a matrix.
     */
    public fun setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    )

    /**
     * Sets the factor of the ambient occlusion effect.
     *
     * @param material The handle of the Khronos PBR material.
     * @param factor The factor of the ambient occlusion.
     */
    public fun setAmbientOcclusionFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    )

    /**
     * Sets the emissive texture for the Khronos PBR material. Defines the light emitted by the
     * material.
     *
     * @param material The handle of the Khronos PBR material.
     * @param emissive The handle of the emissive texture.
     * @param sampler The sampler used for the emissive texture.
     */
    public fun setEmissiveTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        emissive: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the emissive texture.
     *
     * @param material The handle of the Khronos PBR material.
     * @param uvTransform The uv coordinates of the transform stored in a matrix.
     */
    public fun setEmissiveUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    )

    /**
     * Sets the emissive color factors for the Khronos PBR material. Multiplies the emissive texture
     * or defines a uniform emissive color.
     *
     * @param material The handle of the Khronos PBR material.
     * @param factors An RGB [androidx.xr.runtime.math.Vector3] where `(x, y, z)` maps to `(Red,
     *   Green, Blue)`.
     */
    public fun setEmissiveFactorsOnKhronosPbrMaterial(material: MaterialResource, factors: Vector3)

    /**
     * Sets the clearcoat texture for the Khronos PBR material. Adds a clearcoat layer to the
     * material, affecting reflections.
     *
     * @param material The handle of the Khronos PBR material.
     * @param clearcoat The handle of the clearcoat texture.
     * @param sampler The sampler used for the clearcoat texture.
     */
    public fun setClearcoatTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoat: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the clearcoat normal texture for the Khronos PBR material. Perturbs the normals of the
     * clearcoat layer.
     *
     * @param material The handle of the Khronos PBR material.
     * @param clearcoatNormal The handle of the clearcoat normal texture.
     * @param sampler The sampler used for the clearcoat normal texture.
     */
    public fun setClearcoatNormalTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoatNormal: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the clearcoat roughness texture for the Khronos PBR material. Controls the roughness of
     * the clearcoat layer.
     *
     * @param material The handle of the Khronos PBR material.
     * @param clearcoatRoughness The handle of the clearcoat roughness texture.
     * @param sampler The sampler used for the clearcoat roughness texture.
     */
    public fun setClearcoatRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoatRoughness: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the clearcoat factor for the Khronos PBR material. Multiplies the clearcoat texture or
     * defines a uniform clearcoat color.
     *
     * @param material The handle of the Khronos PBR material.
     * @param intensity The intensity of the clearcoat.
     * @param roughness The roughness of the clearcoat.
     * @param normal The normal of the clearcoat.
     */
    public fun setClearcoatFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        intensity: Float,
        roughness: Float,
        normal: Float,
    )

    /**
     * Sets the sheen color texture for the Khronos PBR material. Defines the color of the sheen
     * effect, visible at grazing angles.
     *
     * @param material The handle of the Khronos PBR material.
     * @param sheenColor The handle of the sheen color texture.
     * @param sampler The sampler used for the sheen color texture.
     */
    public fun setSheenColorTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        sheenColor: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the sheen color factors for the Khronos PBR material. Multiplies the sheen color texture
     * or defines a uniform sheen color.
     *
     * @param material The handle of the Khronos PBR material.
     * @param factors The sheen colors on the Khronos PBR material.
     */
    public fun setSheenColorFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        factors: Vector3,
    )

    /**
     * Sets the sheen roughness texture for the Khronos PBR material. Controls the roughness of the
     * sheen effect.
     *
     * @param material The handle of the Khronos PBR material.
     * @param sheenRoughness The handle of the sheen roughness texture.
     * @param sampler The sampler used for the sheen roughness texture.
     */
    public fun setSheenRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        sheenRoughness: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the sheen roughness factor for the Khronos PBR material. Controls the roughness of the
     * sheen effect.
     *
     * @param material The handle of the Khronos PBR material.
     * @param factor The sheen roughness factor.
     */
    public fun setSheenRoughnessFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    )

    /**
     * Sets the transmission texture for the Khronos PBR material. Defines the transmission of light
     * through the material.
     *
     * @param material The handle of the Khronos PBR material.
     * @param transmission The handle of the transmission texture.
     * @param sampler The sampler used for the transmission texture.
     */
    public fun setTransmissionTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        transmission: TextureResource,
        sampler: TextureSampler,
    )

    /**
     * Sets the UV transformation matrix for the transmission texture.
     *
     * @param material The handle of the Khronos PBR material.
     * @param uvTransform The uv coordinates of the transform stored in a matrix.
     */
    public fun setTransmissionUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    )

    /**
     * Sets the transmission factor for the Khronos PBR material. Controls the amount of light
     * transmitted through the material.
     *
     * @param material The handle of the Khronos PBR material.
     * @param factor The transmission factor.
     */
    public fun setTransmissionFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float)

    /**
     * Sets the index of refraction for the Khronos PBR material. Defines how much light bends when
     * entering the material.
     *
     * @param material The handle of the Khronos PBR material.
     * @param indexOfRefraction The index of refraction.
     */
    public fun setIndexOfRefractionOnKhronosPbrMaterial(
        material: MaterialResource,
        indexOfRefraction: Float,
    )

    /**
     * Sets the alpha cutoff for the Khronos PBR material. Defines the threshold for transparency,
     * used for cutout effects.
     *
     * @param material The handle of the Khronos PBR material.
     * @param alphaCutoff The alpha cutoff value.
     */
    public fun setAlphaCutoffOnKhronosPbrMaterial(material: MaterialResource, alphaCutoff: Float)

    /**
     * A factory function to create a SceneCore GltfEntity. The parent may be the activity space or
     * GltfEntity in the scene.
     *
     * @param pose The initial position and orientation of the new GltfEntity in the 3D scene.
     * @param loadedGltf Represents the 3D model data that will be rendered for this entity.
     * @param parentEntity The parent for the newly created GltfEntity in the scene hierarchy.
     */
    public fun createGltfEntity(
        pose: Pose,
        loadedGltf: GltfModelResource,
        parentEntity: Entity?,
    ): GltfEntity

    /**
     * Factory method for SurfaceEntity.
     *
     * @param stereoMode Stereo mode for the surface.
     * @param mediaBlendingMode The [SurfaceEntity.MediaBlendingMode] which describes the media
     *   blending mode of the surface.
     * @param pose Pose of this entity relative to its parent, default value is Identity.
     * @param shape The [SurfaceEntity.Shape] which describes the 3D geometry of the entity.
     * @param surfaceProtection The [SurfaceEntity.SurfaceProtection] which describes whether DRM is
     *   enabled.
     * @param superSampling The [SurfaceEntity.SuperSampling] which describes whether super sampling
     *   is enabled. Whether to use super sampling for the surface.
     * @param parentEntity The parent entity of this entity.
     * @return A [SurfaceEntity] which is a child of the parent entity.
     */
    public fun createSurfaceEntity(
        stereoMode: Int,
        @SurfaceEntity.MediaBlendingMode mediaBlendingMode: Int,
        pose: Pose,
        shape: SurfaceEntity.Shape,
        @SurfaceEntity.SurfaceProtection surfaceProtection: Int,
        superSampling: Int,
        parentEntity: Entity?,
    ): SurfaceEntity
}
