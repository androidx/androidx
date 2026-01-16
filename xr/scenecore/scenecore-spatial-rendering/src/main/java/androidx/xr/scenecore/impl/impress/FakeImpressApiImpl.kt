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

package androidx.xr.scenecore.impl.impress

import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.concurrent.futures.ResolvableFuture
import androidx.xr.scenecore.impl.impress.ImpressApi.ColorRange
import androidx.xr.scenecore.impl.impress.ImpressApi.ColorSpace
import androidx.xr.scenecore.impl.impress.ImpressApi.ColorTransfer
import androidx.xr.scenecore.impl.impress.ImpressApi.ContentSecurityLevel
import androidx.xr.scenecore.impl.impress.ImpressApi.StereoMode
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec
import androidx.xr.scenecore.runtime.TextureSampler
import com.google.ar.imp.view.View
import kotlinx.coroutines.CompletableDeferred

/**
 * Fake implementation of the JNI API for communicating with the Impress Split Engine instance for
 * testing purposes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeImpressApiImpl : ImpressApi {
    internal data class AnimationInProgress(
        var name: String?,
        var fireOnDone: ResolvableFuture<Void?>?,
    )

    /** Test bookkeeping data for a Android Surface */
    public class TestSurface(id: Int) : Surface(SurfaceTexture(id))

    /** Test bookkeeping data for a StereoSurfaceEntity */
    public data class StereoSurfaceEntityData(
        public var impressNode: ImpressNode,
        public var surface: Surface? = null,
        public var useSuperSampling: Boolean = false,
        @StereoMode public var stereoMode: Int = 0,
        public var width: Float = 0f,
        public var height: Float = 0f,
        public var radius: Float = 0f,
        public var canvasShape: CanvasShape? = null,
        public var featherRadiusX: Float = 0f,
        public var featherRadiusY: Float = 0f,
        public var cornerRadius: Float = 0f,
        public var surfaceWidth: Int = 1,
        public var surfaceHeight: Int = 1,
        public var colliderEnabled: Boolean = false,
    ) {
        /** Enum representing the different canvas shapes that can be created. */
        public enum class CanvasShape {
            QUAD,
            VR_360_SPHERE,
            VR_180_HEMISPHERE,
        }
    }

    /** Test bookkeeping data for a Material */
    public data class MaterialData(val type: Type, val materialHandle: Long) {
        /** Enum representing the different built-in material types that can be created. */
        public enum class Type {
            GENERIC,
            WATER,
            WATER_ALPHA,
            KHRONOS_PBR,
        }
    }

    /** Test bookkeeping data for a Gltf gltfToken */
    public class GltfNodeData {
        public var entityId: Int = 0
        public var materialOverride: MaterialData? = null

        /** Sets the material override for a specific mesh of a node */
        public fun setMaterialOverride(
            materialOverride: MaterialData?,
            nodeName: String,
            primitiveIndex: Int,
        ) {
            this.materialOverride = materialOverride
        }

        /** Clears a material override for a specific mesh of a node. */
        public fun clearMaterialOverride(nodeName: String, primitiveIndex: Int) {
            this.materialOverride = null
        }
    }

    // Non-functional resource manager.
    private val resourceManager = BindingsResourceManager(Handler(Looper.getMainLooper()))
    // Vector of image based lighting asset tokens.
    private val imageBasedLightingAssets: MutableMap<Long, ExrImage> = mutableMapOf()
    // Map of model tokens to the list of impress nodes that are instances of that model.
    private val gltfModels: MutableMap<Long, MutableList<Int>> = HashMap()
    // Map of impress nodes to their parent impress nodes.
    private val impressNodes: MutableMap<GltfNodeData, GltfNodeData?> = HashMap()
    // Map of impress nodes and animations that are currently playing (non looping)
    private val impressAnimatedNodes: MutableMap<ImpressNode, AnimationInProgress> = HashMap()
    // Map of impress nodes and animations that are currently playing (looping)
    private val impressLoopAnimatedNodes: MutableMap<ImpressNode, AnimationInProgress> = HashMap()
    // Vector of animating Impress nodes that are currently paused.
    private val impressPausedAnimatedNode: MutableList<ImpressNode> = ArrayList()
    // Map of impress entity nodes to their associated StereoSurfaceEntityData
    private val stereoSurfaceEntities: MutableMap<ImpressNode, StereoSurfaceEntityData> = HashMap()
    // Map of texture image tokens to their associated Texture object
    private val textureImages: MutableMap<Long, Texture> = HashMap()
    // Map of material tokens to their associated MaterialData object
    private val materials: MutableMap<Long, MaterialData> = HashMap()

    private var nextImageBasedLightingAssetId: Int = 1
    private var nextModelId: Int = 1
    private var nextNodeId: Int = 1
    private var nextTextureId: Long = 1
    private var nextMaterialId: Long = 1
    private var currentEnvironmentLightId: Long = -1
    private val activeAnimations = mutableMapOf<ImpressNode, CompletableDeferred<Unit>>()

    override fun setup(view: View?) {}

    @VisibleForTesting override fun setup(nativeTestViewHandle: Long) {}

    override fun onResume() {}

    override fun onPause() {}

    override fun getBindingsResourceManager(): BindingsResourceManager = resourceManager

    override fun releaseImageBasedLightingAsset(iblToken: Long) {
        imageBasedLightingAssets.remove(iblToken)
    }

    @Suppress("RestrictTo")
    override suspend fun loadImageBasedLightingAsset(path: String): ExrImage {
        val token = (nextImageBasedLightingAssetId++).toLong()
        val exrImage: ExrImage =
            ExrImage.Builder().setImpressApi(this).setNativeExrImage(token).build()
        imageBasedLightingAssets[token] = exrImage
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        return exrImage
    }

    @Suppress("RestrictTo")
    override suspend fun loadImageBasedLightingAsset(data: ByteArray, key: String): ExrImage {
        val token = (nextImageBasedLightingAssetId++).toLong()
        val exrImage: ExrImage =
            ExrImage.Builder().setImpressApi(this).setNativeExrImage(token).build()
        imageBasedLightingAssets[token] = exrImage
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        return exrImage
    }

    @Suppress("RestrictTo")
    override suspend fun loadGltfAsset(path: String): GltfModel {
        val token = (nextModelId++).toLong()
        gltfModels[token] = ArrayList()
        val gltfModel: GltfModel =
            GltfModel.Builder().setImpressApi(this).setNativeGltfModel(token).build()
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        return gltfModel
    }

    @Suppress("RestrictTo")
    override suspend fun loadGltfAsset(data: ByteArray, key: String): GltfModel {
        val token = (nextModelId++).toLong()
        gltfModels[token] = ArrayList()
        val gltfModel: GltfModel =
            GltfModel.Builder().setImpressApi(this).setNativeGltfModel(token).build()
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        return gltfModel
    }

    override fun releaseGltfAsset(gltfToken: Long) {
        gltfModels.remove(gltfToken)
    }

    override fun instanceGltfModel(gltfToken: Long): ImpressNode {
        return instanceGltfModel(gltfToken, true)
    }

    override fun instanceGltfModel(gltfToken: Long, enableCollider: Boolean): ImpressNode {
        require(gltfModels.containsKey(gltfToken)) { "Model token not found" }
        val entityId = nextNodeId++
        gltfModels[gltfToken]?.add(entityId)
        val gltfNodeData = GltfNodeData().apply { this.entityId = entityId }
        impressNodes[gltfNodeData] = null
        return ImpressNode(entityId)
    }

    override fun setGltfModelColliderEnabled(impressNode: ImpressNode, enableCollider: Boolean) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setGltfReformAffordanceEnabled(impressNode: ImpressNode, enabled: Boolean) {
        throw IllegalArgumentException("not implemented")
    }

    @Suppress("RestrictTo")
    override suspend fun animateGltfModel(
        impressNode: ImpressNode,
        animationName: String?,
        looping: Boolean,
    ): Void? {
        if (getGltfNodeData(impressNode) == null) {
            throw IllegalArgumentException("Impress node not found")
        }
        val animationInProgress = AnimationInProgress(animationName, null)
        if (looping) {
            impressLoopAnimatedNodes[impressNode] = animationInProgress
        } else {
            impressAnimatedNodes[impressNode] = animationInProgress
        }
        return null
    }

    override fun stopGltfModelAnimation(impressNode: ImpressNode) {
        activeAnimations[impressNode]?.complete(Unit)

        when {
            getGltfNodeData(impressNode) == null ->
                throw IllegalArgumentException("Impress node not found")
            !impressAnimatedNodes.containsKey(impressNode) &&
                !impressLoopAnimatedNodes.containsKey(impressNode) ->
                throw IllegalArgumentException("Impress node is not animating")
            impressAnimatedNodes.containsKey(impressNode) ->
                impressAnimatedNodes.remove(impressNode)
            impressLoopAnimatedNodes.containsKey(impressNode) ->
                impressLoopAnimatedNodes.remove(impressNode)
        }
    }

    override fun toggleGltfModelAnimation(impressNode: ImpressNode, playing: Boolean) {
        if (getGltfNodeData(impressNode) == null) {
            throw IllegalArgumentException("Impress node not found")
        }
        if (playing) {
            // playing as true = resume animation
            if (!impressPausedAnimatedNode.contains(impressNode)) {
                throw IllegalArgumentException(
                    "Animation in this Impress node is not in " + "pausing status"
                )
            }
            impressPausedAnimatedNode.remove(impressNode)
        } else {
            // toggle as false = pause animation
            if (
                !impressAnimatedNodes.containsKey(impressNode) &&
                    !impressLoopAnimatedNodes.containsKey(impressNode)
            ) {
                throw IllegalArgumentException("Impress node is not animating")
            } else if (impressAnimatedNodes.containsKey(impressNode)) {
                impressPausedAnimatedNode.add(impressNode)
            } else if (impressLoopAnimatedNodes.containsKey(impressNode)) {
                impressPausedAnimatedNode.add(impressNode)
            }
        }
    }

    override fun createImpressNode(): ImpressNode {
        val entityId = nextNodeId++
        val gltfNodeData = GltfNodeData().apply { this.entityId = entityId }
        impressNodes[gltfNodeData] = null
        return ImpressNode(entityId)
    }

    override fun destroyImpressNode(impressNode: ImpressNode) {
        val gltfNodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        for ((_, value) in gltfModels) {
            value.remove(impressNode.handle)
        }
        for ((key, value) in impressNodes) {
            if (value != null && value == gltfNodeData) {
                impressNodes[key] = null
            }
        }
        impressNodes.remove(gltfNodeData)
        stereoSurfaceEntities.remove(impressNode)
    }

    override fun setImpressNodeParent(
        impressNodeChild: ImpressNode,
        impressNodeParent: ImpressNode,
    ) {
        val childGltfNodeData = getGltfNodeData(impressNodeChild)
        val parentGltfNodeData = getGltfNodeData(impressNodeParent)
        require(childGltfNodeData != null && parentGltfNodeData != null) {
            "Impress node(s) not found"
        }
        impressNodes[childGltfNodeData] = parentGltfNodeData
    }

    /** Gets the impress nodes for glTF models that match the given token. */
    public fun getImpressNodesForToken(gltfToken: Long): List<Int>? = gltfModels[gltfToken]

    /** Returns true if the given impress node has a parent. */
    public fun impressNodeHasParent(impressNode: ImpressNode): Boolean {
        val gltfNodeData = getGltfNodeData(impressNode) ?: return false
        return impressNodes[gltfNodeData] != null
    }

    /** Returns the parent impress node for the given impress node. */
    public fun getImpressNodeParent(impressNode: ImpressNode): Int {
        val gltfNodeData = getGltfNodeData(impressNode)
        val parentGltfNodeData = impressNodes[gltfNodeData]
        return if (gltfNodeData == null || parentGltfNodeData == null) -1
        else parentGltfNodeData.entityId
    }

    /** Returns the number of impress nodes that are currently animating. */
    public fun impressNodeAnimatingSize(): Int = impressAnimatedNodes.size

    /** Returns the number of animating Impress nodes that are currently looping. */
    public fun impressNodeLoopAnimatingSize(): Int = impressLoopAnimatedNodes.size

    /** Returns the number of animating Impress nodes that are currently paused. */
    public fun impressNodeAnimationPausingSize(): Int = impressPausedAnimatedNode.size

    override fun createStereoSurface(@StereoMode stereoMode: Int): ImpressNode {
        return createStereoSurface(
            stereoMode,
            ContentSecurityLevel.NONE,
            /* useSuperSampling= */ false,
        )
    }

    // TODO - b/410899125: Set the content security level properly.
    override fun createStereoSurface(
        @StereoMode stereoMode: Int,
        @ContentSecurityLevel contentSecurityLevel: Int,
    ): ImpressNode {
        return createStereoSurface(stereoMode, contentSecurityLevel, /* useSuperSampling= */ false)
    }

    override fun createStereoSurface(
        @StereoMode stereoMode: Int,
        @ContentSecurityLevel contentSecurityLevel: Int,
        useSuperSampling: Boolean,
    ): ImpressNode {
        val impressNode: ImpressNode = createImpressNode()
        val data =
            StereoSurfaceEntityData(
                impressNode = impressNode,
                surface = TestSurface(impressNode.handle),
                useSuperSampling = useSuperSampling,
                stereoMode = stereoMode,
                canvasShape = null,
            )
        stereoSurfaceEntities[data.impressNode] = data
        return data.impressNode
    }

    override fun setStereoSurfaceEntityCanvasShapeQuad(
        impressNode: ImpressNode,
        width: Float,
        height: Float,
        cornerRadius: Float,
    ) {
        val data =
            stereoSurfaceEntities[impressNode]
                ?: throw IllegalArgumentException("Couldn't find stereo surface entity!")
        data.canvasShape = StereoSurfaceEntityData.CanvasShape.QUAD
        data.width = width
        data.height = height
        data.cornerRadius = cornerRadius
    }

    override fun setStereoSurfaceEntityCanvasShapeSphere(impressNode: ImpressNode, radius: Float) {
        val data =
            stereoSurfaceEntities[impressNode]
                ?: throw IllegalArgumentException("Couldn't find stereo surface entity!")
        data.canvasShape = StereoSurfaceEntityData.CanvasShape.VR_360_SPHERE
        data.radius = radius
    }

    override fun setStereoSurfaceEntityCanvasShapeHemisphere(
        impressNode: ImpressNode,
        radius: Float,
    ) {
        val data =
            stereoSurfaceEntities[impressNode]
                ?: throw IllegalArgumentException("Couldn't find stereo surface entity!")
        data.canvasShape = StereoSurfaceEntityData.CanvasShape.VR_180_HEMISPHERE
        data.radius = radius
    }

    override fun setStereoSurfaceEntityColliderEnabled(
        impressNode: ImpressNode,
        enableCollider: Boolean,
    ) {
        stereoSurfaceEntities[impressNode]?.colliderEnabled = enableCollider
    }

    override fun getSurfaceFromStereoSurface(panelImpressNode: ImpressNode): Surface {
        // TODO: b/387323937 - the Native code currently CHECK fails in this case
        return stereoSurfaceEntities[panelImpressNode]?.surface
            ?: throw IllegalArgumentException("Couldn't find stereo surface entity!")
    }

    override fun setStereoSurfaceEntitySurfaceSize(
        impressNode: ImpressNode,
        width: Int,
        height: Int,
    ) {
        require(!(width <= 0 || height <= 0)) { "Surface dimensions must be positive!" }
        require(stereoSurfaceEntities.containsKey(impressNode)) {
            "Couldn't find stereo surface entity!"
        }
        val data: StereoSurfaceEntityData = stereoSurfaceEntities[impressNode]!!
        data.surfaceWidth = width
        data.surfaceHeight = height
    }

    override fun setFeatherRadiusForStereoSurface(
        panelImpressNode: ImpressNode,
        radiusX: Float,
        radiusY: Float,
    ) {
        // TODO: b/387323937 - the Native code currently CHECK fails in this case
        val data =
            stereoSurfaceEntities[panelImpressNode]
                ?: throw IllegalArgumentException("Couldn't find stereo surface entity!")
        data.featherRadiusX = radiusX
        data.featherRadiusY = radiusY
    }

    override fun setStereoModeForStereoSurface(
        panelImpressNode: ImpressNode,
        @StereoMode stereoMode: Int,
    ) {
        // TODO: b/387323937 - the Native code currently CHECK fails in this case
        val data =
            stereoSurfaceEntities[panelImpressNode]
                ?: throw IllegalArgumentException("Couldn't find stereo surface entity!")
        data.stereoMode = stereoMode
    }

    override fun setContentColorMetadataForStereoSurface(
        stereoSurfaceNode: ImpressNode,
        @ColorSpace colorSpace: Int,
        @ColorTransfer colorTransfer: Int,
        @ColorRange colorRange: Int,
        maxLuminance: Int,
    ) {}

    override fun resetContentColorMetadataForStereoSurface(stereoSurfaceNode: ImpressNode) {}

    @Suppress("RestrictTo")
    override suspend fun loadTexture(path: String): Texture {
        val textureImageToken = nextTextureId++
        val texture =
            Texture.Builder().setImpressApi(this).setNativeTexture(textureImageToken).build()
        textureImages[textureImageToken] = texture
        return texture
    }

    override fun borrowReflectionTexture(): Texture {
        val textureImageToken = nextTextureId++
        return Texture.Builder().setImpressApi(this).setNativeTexture(textureImageToken).build()
    }

    override fun getReflectionTextureFromIbl(iblToken: Long): Texture {
        val textureImageToken = nextTextureId++
        return Texture.Builder().setImpressApi(this).setNativeTexture(textureImageToken).build()
    }

    @Suppress("RestrictTo")
    override suspend fun createWaterMaterial(isAlphaMapVersion: Boolean): WaterMaterial {
        val materialToken = nextMaterialId++
        val material =
            WaterMaterial.Builder().setImpressApi(this).setNativeMaterial(materialToken).build()
        materials[materialToken] = MaterialData(MaterialData.Type.WATER, materialToken)
        return material
    }

    override fun setReflectionMapOnWaterMaterial(
        nativeWaterMaterial: Long,
        reflectionMap: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setNormalMapOnWaterMaterial(
        nativeWaterMaterial: Long,
        normalMap: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setNormalTilingOnWaterMaterial(nativeWaterMaterial: Long, normalTiling: Float) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setNormalSpeedOnWaterMaterial(nativeWaterMaterial: Long, normalSpeed: Float) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setAlphaStepMultiplierOnWaterMaterial(
        nativeWaterMaterial: Long,
        alphaStepMultiplier: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setAlphaMapOnWaterMaterial(
        nativeWaterMaterial: Long,
        alphaMap: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setNormalZOnWaterMaterial(nativeWaterMaterial: Long, normalZ: Float) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setNormalBoundaryOnWaterMaterial(nativeWaterMaterial: Long, boundary: Float) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setAlphaStepUOnWaterMaterial(
        nativeWaterMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setAlphaStepVOnWaterMaterial(
        nativeWaterMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    @Suppress("RestrictTo")
    override suspend fun createKhronosPbrMaterial(
        spec: KhronosPbrMaterialSpec
    ): KhronosPbrMaterial {
        val materialToken = nextMaterialId++
        val material =
            KhronosPbrMaterial.Builder()
                .setImpressApi(this)
                .setNativeMaterial(materialToken)
                .build()
        materials[materialToken] = MaterialData(MaterialData.Type.KHRONOS_PBR, materialToken)
        return material
    }

    override fun setBaseColorTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        baseColorTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setBaseColorUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setBaseColorFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setMetallicRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        metallicRoughnessTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setMetallicFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setRoughnessFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setNormalTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        normalTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setNormalUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setNormalFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setAmbientOcclusionTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ambientOcclusionTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setAmbientOcclusionFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setEmissiveTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        emissiveTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setEmissiveUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setEmissiveFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setClearcoatTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setClearcoatNormalTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatNormalTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setClearcoatRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        clearcoatRoughnessTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setClearcoatFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        intensity: Float,
        roughness: Float,
        normal: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setSheenColorTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        sheenColorTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setSheenColorFactorsOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        x: Float,
        y: Float,
        z: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setSheenRoughnessTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        sheenRoughnessTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setSheenRoughnessFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setTransmissionTextureOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        transmissionTexture: Long,
        sampler: TextureSampler,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setTransmissionUvTransformOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        wx: Float,
        wy: Float,
        wz: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setTransmissionFactorOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        factor: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setIndexOfRefractionOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        indexOfRefraction: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setAlphaCutoffOnKhronosPbrMaterial(
        nativeKhronosPbrMaterial: Long,
        alphaCutoff: Float,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun destroyNativeObject(nativeHandle: Long) {
        materials.remove(nativeHandle)
        textureImages.remove(nativeHandle)
    }

    override fun setMaterialOverride(
        impressNode: ImpressNode,
        nativeMaterial: Long,
        nodeName: String,
        primitiveIndex: Int,
    ) {
        val gltfNodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        gltfNodeData.setMaterialOverride(materials[nativeMaterial], nodeName, primitiveIndex)
    }

    override fun clearMaterialOverride(
        impressNode: ImpressNode,
        nodeName: String,
        primitiveIndex: Int,
    ) {
        val gltfNodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        gltfNodeData.clearMaterialOverride(nodeName, primitiveIndex)
    }

    override fun setPreferredEnvironmentLight(iblToken: Long) {
        currentEnvironmentLightId = iblToken
    }

    override fun clearPreferredEnvironmentIblAsset() {
        currentEnvironmentLightId = -1
    }

    override fun setPrimaryAlphaMaskForStereoSurface(
        panelImpressNode: ImpressNode,
        alphaMask: Long,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setAuxiliaryAlphaMaskForStereoSurface(
        panelImpressNode: ImpressNode,
        alphaMask: Long,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override fun disposeAllResources() {
        imageBasedLightingAssets.clear()
        impressNodes.clear()
        gltfModels.clear()
        textureImages.clear()
        materials.clear()
    }

    /** Returns the map of texture image tokens to their associated Texture object. */
    public fun getTextureImages(): MutableMap<Long, Texture> {
        return textureImages
    }

    /** Returns the map of material tokens to their associated MaterialData object. */
    public fun getMaterials(): MutableMap<Long, MaterialData> {
        return materials
    }

    /** Returns the map of impress nodes to their parent impress nodes. */
    public fun getImpressNodes(): MutableMap<GltfNodeData, GltfNodeData?> {
        return impressNodes
    }

    // Returns the list of image based lighting assets that have been loaded.
    public fun getImageBasedLightingAssets(): List<Long> {
        return imageBasedLightingAssets.keys.toList()
    }

    // Returns the map of glTF model tokens to their associated impress nodes.
    public fun getGltfModels(): MutableMap<Long, MutableList<Int>> {
        return gltfModels
    }

    /** Returns the current environment light token. */
    public fun getCurrentEnvironmentLight(): Long {
        return currentEnvironmentLightId
    }

    public fun getStereoSurfaceEntities(): MutableMap<ImpressNode, StereoSurfaceEntityData> {
        return stereoSurfaceEntities
    }

    private fun getGltfNodeData(impressNode: ImpressNode): GltfNodeData? {
        return impressNodes.keys.firstOrNull { it.entityId == impressNode.handle }
    }
}
