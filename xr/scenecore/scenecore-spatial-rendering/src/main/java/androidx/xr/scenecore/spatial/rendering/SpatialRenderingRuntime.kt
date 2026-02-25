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

package androidx.xr.scenecore.spatial.rendering

import android.app.Activity
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.math.Matrix3
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4
import androidx.xr.scenecore.impl.impress.ExrImage
import androidx.xr.scenecore.impl.impress.GltfModel
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressApiImpl
import androidx.xr.scenecore.impl.impress.KhronosPbrMaterial
import androidx.xr.scenecore.impl.impress.Material
import androidx.xr.scenecore.impl.impress.Texture
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.ExrImageResource
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfModelResource
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.RenderingEntityFactory
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialEnvironmentExt
import androidx.xr.scenecore.runtime.SpatialEnvironmentFeature
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.TextureResource
import androidx.xr.scenecore.runtime.TextureSampler
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import com.android.extensions.xr.XrExtensions
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.ar.imp.view.splitengine.ImpSplitEngine
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer

/**
 * Implementation of [RenderingRuntime] for devices that support the
 * [androidx.xr.runtime.interfaces.Feature.SPATIAL] system feature.
 */
internal class SpatialRenderingRuntime
private constructor(
    sceneRuntime: SceneRuntime,
    private var activity: Activity?,
    private val extensions: XrExtensions,
    private val impressApi: ImpressApi,
    private val subspaceManager: SplitEngineSubspaceManager,
    private val renderer: ImpSplitEngineRenderer,
) : RenderingRuntime {

    private lateinit var renderingEntityFactory: RenderingEntityFactory
    private var spatialEnvironmentFeature: SpatialEnvironmentFeatureImpl?
    private var isDestroyed = false
    private var frameLoopStarted = false

    init {
        require(sceneRuntime is RenderingEntityFactory) {
            "Expected sceneRuntime to be a RenderingEntityFactory"
        }
        this.renderingEntityFactory = sceneRuntime
        // TODO(b/458776699): Handle activity nullability.
        // TODO(b/458779328): Fix incorrect use of !! on a nullable return value.
        this.spatialEnvironmentFeature =
            SpatialEnvironmentFeatureImpl(activity!!, impressApi, subspaceManager, extensions)

        (sceneRuntime.spatialEnvironment as SpatialEnvironmentExt).onRenderingFeatureReady(
            spatialEnvironmentFeature as SpatialEnvironmentFeature
        )
    }

    @SuppressWarnings("RestrictTo")
    override suspend fun loadGltfByAssetName(assetName: String): GltfModelResource {
        return impressApi.loadGltfAsset(assetName)
    }

    @SuppressWarnings("RestrictTo")
    override suspend fun loadGltfByByteArray(
        assetData: ByteArray,
        assetKey: String,
    ): GltfModelResource {
        // TODO(b/458779328): Fix incorrect use of !! on a nullable return value.
        return impressApi.loadGltfAsset(assetData, assetKey)
    }

    @Override
    override fun destroyGltfModel(gltfModel: GltfModelResource) {
        (gltfModel as GltfModel).destroy()
    }

    @SuppressWarnings("RestrictTo")
    override suspend fun loadExrImageByAssetName(assetName: String): ExrImageResource {
        return impressApi.loadImageBasedLightingAsset(assetName)
    }

    @SuppressWarnings("RestrictTo")
    override suspend fun loadExrImageByByteArray(
        assetData: ByteArray,
        assetKey: String,
    ): ExrImageResource {
        return impressApi.loadImageBasedLightingAsset(assetData, assetKey)
    }

    @Override
    override fun destroyExrImage(exrImage: ExrImageResource) {
        (exrImage as ExrImage).destroy()
    }

    @SuppressWarnings("RestrictTo")
    override suspend fun loadTexture(assetName: String): TextureResource {
        // TODO:b/374216912 - Consider calling setFuture() here to catch if the application calls
        // cancel() on the return value from this function, so we can propagate the cancelation
        // message to the Impress API.
        check(Looper.getMainLooper().isCurrentThread) {
            "This method must be called on the main thread."
        }
        // It's convenient for the main application for us to dispatch their listeners on
        // the main thread, because they are required to call back to Impress from there,
        // and it's likely that they will want to call back into the SDK to create entities
        // from within a listener. We defensively post to the main thread here, but in
        // practice this should not cause a thread hop because the Impress API already
        // dispatches its callbacks to the main thread.
        val texture: Texture = impressApi.loadTexture(assetName)
        return texture
    }

    override fun borrowReflectionTexture(): TextureResource? {
        return impressApi.borrowReflectionTexture()
    }

    override fun destroyTexture(texture: TextureResource) {
        (texture as Texture).destroy()
    }

    override fun getReflectionTextureFromIbl(iblToken: ExrImageResource): TextureResource? {
        val texture: Texture? =
            impressApi.getReflectionTextureFromIbl((iblToken as ExrImage).nativeHandle)
        return texture
    }

    @SuppressWarnings("RestrictTo")
    override suspend fun createWaterMaterial(isAlphaMapVersion: Boolean): MaterialResource {
        // TODO:b/374216912 - Consider calling setFuture() here to catch if the application calls
        // cancel() on the return value from this function, so we can propagate the cancelation
        // message to the Impress API.
        check(Looper.getMainLooper().isCurrentThread) {
            "This method must be called on the main thread."
        }
        // It's convenient for the main application for us to dispatch their listeners on
        // the main thread, because they are required to call back to Impress from there,
        // and it's likely that they will want to call back into the SDK to create entities
        // from within a listener. We defensively post to the main thread here, but in
        // practice this should not cause a thread hop because the Impress API already
        // dispatches its callbacks to the main thread.
        val materialResource = impressApi.createWaterMaterial(isAlphaMapVersion)
        return materialResource
    }

    override fun destroyWaterMaterial(material: MaterialResource) {
        require(material is Material) { "MaterialResource is not a Material" }
        material.destroy()
    }

    override fun setReflectionMapOnWaterMaterial(
        material: MaterialResource,
        reflectionMap: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(reflectionMap is Texture) { "TextureResource is not a Texture" }
        impressApi.setReflectionMapOnWaterMaterial(
            material.nativeHandle,
            reflectionMap.nativeHandle,
            sampler,
        )
    }

    override fun setNormalMapOnWaterMaterial(
        material: MaterialResource,
        normalMap: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(normalMap is Texture) { "TextureResource is not a Texture" }
        impressApi.setNormalMapOnWaterMaterial(
            material.nativeHandle,
            normalMap.nativeHandle,
            sampler,
        )
    }

    override fun setNormalTilingOnWaterMaterial(material: MaterialResource, normalTiling: Float) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setNormalTilingOnWaterMaterial(material.nativeHandle, normalTiling)
    }

    override fun setNormalSpeedOnWaterMaterial(material: MaterialResource, normalSpeed: Float) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setNormalSpeedOnWaterMaterial(material.nativeHandle, normalSpeed)
    }

    override fun setAlphaStepMultiplierOnWaterMaterial(
        material: MaterialResource,
        alphaStepMultiplier: Float,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setAlphaStepMultiplierOnWaterMaterial(material.nativeHandle, alphaStepMultiplier)
    }

    override fun setAlphaMapOnWaterMaterial(
        material: MaterialResource,
        alphaMap: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(alphaMap is Texture) { "TextureResource is not a Texture" }
        impressApi.setAlphaMapOnWaterMaterial(material.nativeHandle, alphaMap.nativeHandle, sampler)
    }

    override fun setNormalZOnWaterMaterial(material: MaterialResource, normalZ: Float) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setNormalZOnWaterMaterial(material.nativeHandle, normalZ)
    }

    override fun setNormalBoundaryOnWaterMaterial(
        material: MaterialResource,
        normalBoundary: Float,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setNormalBoundaryOnWaterMaterial(material.nativeHandle, normalBoundary)
    }

    override suspend fun createKhronosPbrMaterial(spec: KhronosPbrMaterialSpec): MaterialResource {
        check(Looper.getMainLooper().isCurrentThread) {
            "This method must be called on the main thread."
        }
        val material: KhronosPbrMaterial = impressApi.createKhronosPbrMaterial(spec)
        return material
    }

    override fun destroyKhronosPbrMaterial(material: MaterialResource) {
        require(material is Material) { "MaterialResource is not a Material" }
        material.destroy()
    }

    override fun setBaseColorTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        baseColor: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(baseColor is Texture) { "TextureResource is not a Texture" }
        impressApi.setBaseColorTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            baseColor.nativeHandle,
            sampler,
        )
    }

    override fun setBaseColorUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        val data = uvTransform.data
        impressApi.setBaseColorUvTransformOnKhronosPbrMaterial(
            material.nativeHandle,
            data[0],
            data[1],
            data[2],
            data[3],
            data[4],
            data[5],
            data[6],
            data[7],
            data[8],
        )
    }

    override fun setBaseColorFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        factors: Vector4,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setBaseColorFactorsOnKhronosPbrMaterial(
            material.nativeHandle,
            factors.x,
            factors.y,
            factors.z,
            factors.w,
        )
    }

    override fun setMetallicRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        metallicRoughness: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(metallicRoughness is Texture) { "TextureResource is not a Texture" }
        impressApi.setMetallicRoughnessTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            metallicRoughness.nativeHandle,
            sampler,
        )
    }

    override fun setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        val data = uvTransform.data
        impressApi.setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
            material.nativeHandle,
            data[0],
            data[1],
            data[2],
            data[3],
            data[4],
            data[5],
            data[6],
            data[7],
            data[8],
        )
    }

    override fun setMetallicFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setMetallicFactorOnKhronosPbrMaterial(material.nativeHandle, factor)
    }

    override fun setRoughnessFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setRoughnessFactorOnKhronosPbrMaterial(material.nativeHandle, factor)
    }

    override fun setNormalTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        normal: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(normal is Texture) { "TextureResource is not a Texture" }
        impressApi.setNormalTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            normal.nativeHandle,
            sampler,
        )
    }

    override fun setNormalUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        val data = uvTransform.data
        impressApi.setNormalUvTransformOnKhronosPbrMaterial(
            material.nativeHandle,
            data[0],
            data[1],
            data[2],
            data[3],
            data[4],
            data[5],
            data[6],
            data[7],
            data[8],
        )
    }

    override fun setNormalFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setNormalFactorOnKhronosPbrMaterial(material.nativeHandle, factor)
    }

    override fun setAmbientOcclusionTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        ambientOcclusion: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(ambientOcclusion is Texture) { "TextureResource is not a Texture" }
        impressApi.setAmbientOcclusionTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            ambientOcclusion.nativeHandle,
            sampler,
        )
    }

    override fun setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        val data = uvTransform.data
        impressApi.setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
            material.nativeHandle,
            data[0],
            data[1],
            data[2],
            data[3],
            data[4],
            data[5],
            data[6],
            data[7],
            data[8],
        )
    }

    override fun setAmbientOcclusionFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setAmbientOcclusionFactorOnKhronosPbrMaterial(material.nativeHandle, factor)
    }

    override fun setEmissiveTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        emissive: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(emissive is Texture) { "TextureResource is not a Texture" }
        impressApi.setEmissiveTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            emissive.nativeHandle,
            sampler,
        )
    }

    override fun setEmissiveUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        val data = uvTransform.data
        impressApi.setEmissiveUvTransformOnKhronosPbrMaterial(
            material.nativeHandle,
            data[0],
            data[1],
            data[2],
            data[3],
            data[4],
            data[5],
            data[6],
            data[7],
            data[8],
        )
    }

    override fun setEmissiveFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        factors: Vector3,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setEmissiveFactorsOnKhronosPbrMaterial(
            material.nativeHandle,
            factors.x,
            factors.y,
            factors.z,
        )
    }

    override fun setClearcoatTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoat: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(clearcoat is Texture) { "TextureResource is not a Texture" }
        impressApi.setClearcoatTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            clearcoat.nativeHandle,
            sampler,
        )
    }

    override fun setClearcoatNormalTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoatNormal: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(clearcoatNormal is Texture) { "TextureResource is not a Texture" }
        impressApi.setClearcoatNormalTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            clearcoatNormal.nativeHandle,
            sampler,
        )
    }

    override fun setClearcoatRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoatRoughness: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(clearcoatRoughness is Texture) { "TextureResource is not a Texture" }
        impressApi.setClearcoatRoughnessTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            clearcoatRoughness.nativeHandle,
            sampler,
        )
    }

    override fun setClearcoatFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        intensity: Float,
        roughness: Float,
        normal: Float,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setClearcoatFactorsOnKhronosPbrMaterial(
            material.nativeHandle,
            intensity,
            roughness,
            normal,
        )
    }

    override fun setSheenColorTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        sheenColor: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(sheenColor is Texture) { "TextureResource is not a Texture" }
        impressApi.setSheenColorTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            sheenColor.nativeHandle,
            sampler,
        )
    }

    override fun setSheenColorFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        factors: Vector3,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setSheenColorFactorsOnKhronosPbrMaterial(
            material.nativeHandle,
            factors.x,
            factors.y,
            factors.z,
        )
    }

    override fun setSheenRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        sheenRoughness: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(sheenRoughness is Texture) { "TextureResource is not a Texture" }
        impressApi.setSheenRoughnessTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            sheenRoughness.nativeHandle,
            sampler,
        )
    }

    override fun setSheenRoughnessFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setSheenRoughnessFactorOnKhronosPbrMaterial(material.nativeHandle, factor)
    }

    override fun setTransmissionTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        transmission: TextureResource,
        sampler: TextureSampler,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        require(transmission is Texture) { "TextureResource is not a Texture" }
        impressApi.setTransmissionTextureOnKhronosPbrMaterial(
            material.nativeHandle,
            transmission.nativeHandle,
            sampler,
        )
    }

    override fun setTransmissionUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        val data = uvTransform.data
        impressApi.setTransmissionUvTransformOnKhronosPbrMaterial(
            material.nativeHandle,
            data[0],
            data[1],
            data[2],
            data[3],
            data[4],
            data[5],
            data[6],
            data[7],
            data[8],
        )
    }

    override fun setTransmissionFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setTransmissionFactorOnKhronosPbrMaterial(material.nativeHandle, factor)
    }

    override fun setIndexOfRefractionOnKhronosPbrMaterial(
        material: MaterialResource,
        indexOfRefraction: Float,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setIndexOfRefractionOnKhronosPbrMaterial(
            material.nativeHandle,
            indexOfRefraction,
        )
    }

    override fun setAlphaCutoffOnKhronosPbrMaterial(
        material: MaterialResource,
        alphaCutoff: Float,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setAlphaCutoffOnKhronosPbrMaterial(material.nativeHandle, alphaCutoff)
    }

    override fun createGltfEntity(
        pose: Pose,
        loadedGltf: GltfModelResource,
        parentEntity: Entity?,
    ): GltfEntity {
        // TODO(b/458779328): Fix incorrect use of !! on a nullable return value.
        val feature =
            GltfFeatureImpl(
                loadedGltf as GltfModel,
                impressApi,
                subspaceManager,
                extensions,
                renderer,
            )
        return renderingEntityFactory.createGltfEntity(feature, pose, parentEntity)
    }

    override fun createSurfaceEntity(
        @SurfaceEntity.StereoMode stereoMode: Int,
        @SurfaceEntity.MediaBlendingMode mediaBlendingMode: Int,
        pose: Pose,
        shape: SurfaceEntity.Shape,
        @SurfaceEntity.SurfaceProtection surfaceProtection: Int,
        @SurfaceEntity.SuperSampling superSampling: Int,
        parentEntity: Entity?,
    ): SurfaceEntity {
        check(Looper.getMainLooper().isCurrentThread) {
            "This method must be called on the main thread."
        }

        // TODO(b/458779328): Fix incorrect use of !! on a nullable return value.
        val feature =
            SurfaceFeatureImpl(
                impressApi,
                subspaceManager,
                extensions,
                stereoMode,
                mediaBlendingMode,
                shape,
                surfaceProtection,
                superSampling,
            )
        return renderingEntityFactory.createSurfaceEntity(feature, pose, parentEntity)
    }

    // JxrRuntime lifecycle
    override fun resume() {
        // Start renderer
        if (frameLoopStarted) {
            return
        }
        frameLoopStarted = true
        renderer.startFrameLoop()
    }

    override fun pause() {
        // Stop renderer
        if (!frameLoopStarted) {
            return
        }
        frameLoopStarted = false
        renderer.stopFrameLoop()
    }

    override fun destroy() {
        if (isDestroyed) {
            return
        }
        activity = null

        if (frameLoopStarted) {
            frameLoopStarted = false
            renderer.stopFrameLoop()
        }

        // mSpatialEnvironmentFeature.dispose() will be invoked once in SceneRuntime.dispose()
        // to make the XrExtensions operations happen before the SceneRuntime detaching the
        // scene. Do the destroy here again to clean our own resource formally.
        spatialEnvironmentFeature?.dispose()
        spatialEnvironmentFeature = null
        impressApi.disposeAllResources()
        subspaceManager.destroy()
        renderer.destroy()
        isDestroyed = true
    }

    @VisibleForTesting public fun isFrameLoopStarted(): Boolean = frameLoopStarted

    public companion object {
        private const val SPLIT_ENGINE_LIBRARY_NAME = "impress_api_jni"

        @VisibleForTesting
        @JvmStatic
        internal fun create(
            sceneRuntime: SceneRuntime,
            activity: Activity,
            impressApi: ImpressApi?,
            splitEngineSubspaceManager: SplitEngineSubspaceManager?,
            splitEngineRenderer: ImpSplitEngineRenderer?,
        ): SpatialRenderingRuntime {
            val extensions =
                getXrExtensions() ?: throw IllegalStateException("XrExtensions is null")

            val finalImpressApi = impressApi ?: ImpressApiImpl()

            val finalSplitEngineRenderer =
                splitEngineRenderer
                    ?: run {
                        val impApiSetupParams =
                            ImpSplitEngine.SplitEngineSetupParams().apply {
                                jniLibraryName = SPLIT_ENGINE_LIBRARY_NAME
                            }
                        ImpSplitEngineRenderer.create(activity, impApiSetupParams, extensions)
                    }

            val finalSubspaceManager =
                splitEngineSubspaceManager
                    ?: SplitEngineSubspaceManager(
                        finalSplitEngineRenderer,
                        extensions,
                        null,
                        null,
                        SPLIT_ENGINE_LIBRARY_NAME,
                    )

            if (finalSplitEngineRenderer != null) {
                finalImpressApi.setup(finalSplitEngineRenderer.view)
            }

            return SpatialRenderingRuntime(
                sceneRuntime,
                activity,
                extensions,
                finalImpressApi,
                finalSubspaceManager,
                finalSplitEngineRenderer,
            )
        }

        /**
         * Create a new @c SpatialRenderingRuntime.
         *
         * @param sceneRuntime The SceneRuntime provide basic function for creating entities.
         * @param activity The Activity to use.
         * @return A new SpatialRenderingRuntime.
         */
        @JvmStatic
        public fun create(sceneRuntime: SceneRuntime, activity: Activity): SpatialRenderingRuntime {
            return create(sceneRuntime, activity, null, null, null)
        }
    }
}
