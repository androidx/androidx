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
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.impl.impress.ImpressApi.ColorRange
import androidx.xr.scenecore.impl.impress.ImpressApi.ColorSpace
import androidx.xr.scenecore.impl.impress.ImpressApi.ColorTransfer
import androidx.xr.scenecore.impl.impress.ImpressApi.ContentSecurityLevel
import androidx.xr.scenecore.impl.impress.ImpressApi.DrawMode
import androidx.xr.scenecore.impl.impress.ImpressApi.MediaBlendingMode
import androidx.xr.scenecore.impl.impress.ImpressApi.StereoMode
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec
import androidx.xr.scenecore.runtime.TextureSampler
import com.google.ar.imp.view.View
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlinx.coroutines.CompletableDeferred

/**
 * Fake implementation of the JNI API for communicating with the Impress Split Engine instance for
 * testing purposes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeImpressApiImpl : ImpressApi {
    public data class AnimationInProgress(
        var name: String?,
        var fireOnDone: ResolvableFuture<Void?>? = null,
        var looping: Boolean = false,
        var speed: Float = 1.0f,
        var startTime: Float = 0.0f,
        var channel: Int = 0,
        var playbackTime: Float = 0.0f,
        var paused: Boolean = false,
    )

    /** Test bookkeeping data for a Android Surface */
    public class TestSurface(id: Int) : Surface(SurfaceTexture(id))

    /** Test bookkeeping data for a StereoSurfaceEntity */
    public data class StereoSurfaceEntityData(
        public var impressNode: ImpressNode,
        public var surface: Surface? = null,
        public var useSuperSampling: Boolean = false,
        @StereoMode public var stereoMode: Int = 0,
        @MediaBlendingMode public var mediaBlendingMode: Int = 0,
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
        public var leftPositions: FloatBuffer? = null,
        public var leftTexCoords: FloatBuffer? = null,
        public var leftIndices: IntBuffer? = null,
        public var rightPositions: FloatBuffer? = null,
        public var rightTexCoords: FloatBuffer? = null,
        public var rightIndices: IntBuffer? = null,
        @DrawMode public var drawMode: Int = 0,
    ) {
        /** Enum representing the different canvas shapes that can be created. */
        public enum class CanvasShape {
            QUAD,
            VR_360_SPHERE,
            VR_180_HEMISPHERE,
            CUSTOM_MESH,
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
        public var name: String = ""
        public val children: MutableList<GltfNodeData> = ArrayList()
        public val transform: FloatArray = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f)
        public var isReskinningScheduled: Boolean = false
        public val nodeMaterialOverrides: MutableMap<Int, MaterialData> = HashMap()

        /** Sets the material override for a specific mesh of a specific node */
        public fun setGltfModelNodeMaterialOverride(
            materialData: MaterialData,
            primitiveIndex: Int,
        ) {
            nodeMaterialOverrides[primitiveIndex] = materialData
        }

        /** Clears a material override for a specific mesh of a specific node. */
        public fun clearGltfModelNodeMaterialOverride(primitiveIndex: Int) {
            nodeMaterialOverrides.remove(primitiveIndex)
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
    // Map of impress nodes to their channel-based animations
    private val channelAnimations: MutableMap<ImpressNode, MutableMap<Int, AnimationInProgress>> =
        HashMap()
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
    private val modelHierarchies = mutableMapOf<Long, List<String>>()

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
        val rootNode = ImpressNode(entityId)
        modelHierarchies[gltfToken]?.forEach { childName ->
            val childId = nextNodeId++
            val childNodeData =
                GltfNodeData().apply {
                    this.entityId = childId
                    this.name = childName
                }
            impressNodes[childNodeData] = gltfNodeData
            gltfNodeData.children.add(childNodeData)
        }
        return rootNode
    }

    override fun setGltfModelColliderEnabled(impressNode: ImpressNode, enableCollider: Boolean) {
        throw IllegalArgumentException("not implemented")
    }

    override fun setGltfReformAffordanceEnabled(
        impressNode: ImpressNode,
        enabled: Boolean,
        systemMovable: Boolean,
    ) {
        throw IllegalArgumentException("not implemented")
    }

    override suspend fun animateGltfModel(
        impressNode: ImpressNode,
        animationName: String?,
        looping: Boolean,
        speed: Float,
        startTime: Float,
        channel: Int,
    ): Void? {
        if (getGltfNodeData(impressNode) == null) {
            throw IllegalArgumentException("Impress node not found")
        }
        val animationInProgress =
            AnimationInProgress(
                name = animationName,
                fireOnDone = null,
                looping = looping,
                speed = speed,
                startTime = startTime,
                channel = channel,
                playbackTime = startTime,
                paused = false,
            )

        val nodeAnims = channelAnimations.getOrPut(impressNode) { HashMap() }
        nodeAnims[channel] = animationInProgress
        return null
    }

    override fun stopGltfModelAnimation(impressNode: ImpressNode, channel: Int) {
        val nodeAnims = channelAnimations[impressNode]
        if (nodeAnims != null) {
            nodeAnims.remove(channel)
            if (nodeAnims.isEmpty()) {
                channelAnimations.remove(impressNode)
            }
        }
    }

    override fun toggleGltfModelAnimation(
        impressNode: ImpressNode,
        playing: Boolean,
        channel: Int,
    ) {
        val nodeAnims = channelAnimations[impressNode]
        val animation = nodeAnims?.get(channel)
        if (animation != null) {
            animation.paused = !playing
        }
    }

    override fun setGltfModelAnimationPlaybackTime(
        impressNode: ImpressNode,
        playbackTime: Float,
        channel: Int,
    ) {
        val nodeAnims = channelAnimations[impressNode]
        nodeAnims?.get(channel)?.playbackTime = playbackTime
    }

    override fun setGltfModelAnimationSpeed(impressNode: ImpressNode, speed: Float, channel: Int) {
        val nodeAnims = channelAnimations[impressNode]
        nodeAnims?.get(channel)?.speed = speed
    }

    override fun getGltfModelAnimationCount(impressNode: ImpressNode): Int {
        return 0
    }

    override fun getGltfModelAnimationName(impressNode: ImpressNode, index: Int): String {
        return ""
    }

    override fun getGltfModelAnimationDurationSeconds(impressNode: ImpressNode, index: Int): Float {
        return 0f
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

    /** This method parents an Impress node to another using their respective node objects. */
    override fun setImpressNodeParent(
        impressNodeChild: ImpressNode,
        impressNodeParent: ImpressNode,
    ) {
        val childGltfNodeData = getGltfNodeData(impressNodeChild)
        val parentGltfNodeData = getGltfNodeData(impressNodeParent)
        require(childGltfNodeData != null && parentGltfNodeData != null) {
            "Impress node(s) not found"
        }

        val oldParent = impressNodes[childGltfNodeData]
        oldParent?.children?.remove(childGltfNodeData)

        impressNodes[childGltfNodeData] = parentGltfNodeData
        parentGltfNodeData.children.add(childGltfNodeData)
    }

    /** Returns the parent node of the given Impress node. */
    override fun getImpressNodeParent(impressNode: ImpressNode): ImpressNode {
        val gltfNodeData = getGltfNodeData(impressNode)
        val parentGltfNodeData = impressNodes[gltfNodeData]

        return if (gltfNodeData == null || parentGltfNodeData == null) {
            ImpressNode(-1)
        } else {
            ImpressNode(parentGltfNodeData.entityId)
        }
    }

    /** This method returns the number of child node of a given Impress node. */
    override fun getImpressNodeChildCount(impressNode: ImpressNode): Int {
        val nodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        return nodeData.children.size
    }

    /** This method returns the child node of an Impress node at a specific index. */
    override fun getImpressNodeChildAt(impressNode: ImpressNode, childIndex: Int): ImpressNode {
        val nodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        if (childIndex < 0 || childIndex >= nodeData.children.size) {
            throw IllegalArgumentException("Invalid child index")
        }
        return ImpressNode(nodeData.children[childIndex].entityId)
    }

    /**
     * This method returns the name of the Impress node. An empty string will be returned if the
     * node does not have a name.
     */
    override fun getImpressNodeName(impressNode: ImpressNode): String {
        val nodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        return nodeData.name
    }

    /** Sets the local transform (TRS) of an Impress node. */
    override fun setImpressNodeLocalTransform(impressNode: ImpressNode, transform: Matrix4) {
        val nodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        val pose = transform.pose
        val scale = transform.scale
        nodeData.transform[0] = pose.translation.x
        nodeData.transform[1] = pose.translation.y
        nodeData.transform[2] = pose.translation.z
        nodeData.transform[3] = pose.rotation.x
        nodeData.transform[4] = pose.rotation.y
        nodeData.transform[5] = pose.rotation.z
        nodeData.transform[6] = pose.rotation.w
        nodeData.transform[7] = scale.x
        nodeData.transform[8] = scale.y
        nodeData.transform[9] = scale.z
    }

    /** Retrieves the local transform (TRS) of an Impress node. */
    override fun getImpressNodeLocalTransform(impressNode: ImpressNode): Matrix4 {
        val nodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        return Matrix4.fromTrs(
            Vector3(nodeData.transform[0], nodeData.transform[1], nodeData.transform[2]),
            Quaternion(
                nodeData.transform[3],
                nodeData.transform[4],
                nodeData.transform[5],
                nodeData.transform[6],
            ),
            Vector3(nodeData.transform[7], nodeData.transform[8], nodeData.transform[9]),
        )
    }

    /** Sets the transform (TRS) of an Impress node relative to an relative node. */
    override fun setImpressNodeRelativeTransform(
        impressNode: ImpressNode,
        relativeNode: ImpressNode,
        transform: Matrix4,
    ) {
        val nodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        val pose = transform.pose
        val scale = transform.scale
        nodeData.transform[0] = pose.translation.x
        nodeData.transform[1] = pose.translation.y
        nodeData.transform[2] = pose.translation.z
        nodeData.transform[3] = pose.rotation.x
        nodeData.transform[4] = pose.rotation.y
        nodeData.transform[5] = pose.rotation.z
        nodeData.transform[6] = pose.rotation.w
        nodeData.transform[7] = scale.x
        nodeData.transform[8] = scale.y
        nodeData.transform[9] = scale.z
    }

    /** Retrieves the transform (TRS) of an Impress node relative to an relative node. */
    override fun getImpressNodeRelativeTransform(
        impressNode: ImpressNode,
        relativeNode: ImpressNode,
    ): Matrix4 {
        val nodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")

        if (impressNode == relativeNode) {
            return Matrix4.Identity
        } else {
            return Matrix4.fromTrs(
                Vector3(nodeData.transform[0], nodeData.transform[1], nodeData.transform[2]),
                Quaternion(
                    nodeData.transform[3],
                    nodeData.transform[4],
                    nodeData.transform[5],
                    nodeData.transform[6],
                ),
                Vector3(nodeData.transform[7], nodeData.transform[8], nodeData.transform[9]),
            )
        }
    }

    /**
     * Schedules reskinning of a glTF model. This should be called after modifying node transforms
     * that affect skinned meshes.
     */
    override fun scheduleGltfReskinning(impressNode: ImpressNode) {
        val nodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        nodeData.isReskinningScheduled = true
    }

    /** Gets the impress nodes for glTF models that match the given token. */
    public fun getImpressNodesForToken(gltfToken: Long): List<Int>? = gltfModels[gltfToken]

    /** Returns true if the given impress node has a parent. */
    public fun impressNodeHasParent(impressNode: ImpressNode): Boolean {
        val gltfNodeData = getGltfNodeData(impressNode) ?: return false
        return impressNodes[gltfNodeData] != null
    }

    /** Returns the number of impress nodes that are currently animating. */
    public fun impressNodeAnimatingSize(): Int = impressAnimatedNodes.size

    /** Returns the number of animating Impress nodes that are currently looping. */
    public fun impressNodeLoopAnimatingSize(): Int = impressLoopAnimatedNodes.size

    /** Returns the number of animating Impress nodes that are currently paused. */
    public fun impressNodeAnimationPausingSize(): Int = impressPausedAnimatedNode.size

    /** Returns the map of channel animations for a given node. */
    public fun getChannelAnimations(impressNode: ImpressNode): Map<Int, AnimationInProgress>? =
        channelAnimations[impressNode]

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
        return createStereoSurface(
            stereoMode,
            MediaBlendingMode.TRANSPARENT,
            contentSecurityLevel,
            useSuperSampling,
        )
    }

    override fun createStereoSurface(
        @StereoMode stereoMode: Int,
        @MediaBlendingMode mediaBlendingMode: Int,
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
                mediaBlendingMode = mediaBlendingMode,
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

    override fun setStereoSurfaceEntityCanvasShapeCustomMesh(
        impressNode: ImpressNode,
        leftPositions: FloatBuffer,
        leftTexCoords: FloatBuffer,
        leftIndices: IntBuffer?,
        rightPositions: FloatBuffer?,
        rightTexCoords: FloatBuffer?,
        rightIndices: IntBuffer?,
        @DrawMode drawMode: Int,
    ) {
        val data =
            stereoSurfaceEntities[impressNode]
                ?: throw IllegalArgumentException("Couldn't find stereo surface entity!")
        data.canvasShape = StereoSurfaceEntityData.CanvasShape.CUSTOM_MESH
        data.leftPositions = leftPositions
        data.leftTexCoords = leftTexCoords
        data.leftIndices = leftIndices
        data.rightPositions = rightPositions
        data.rightTexCoords = rightTexCoords
        data.rightIndices = rightIndices
        data.drawMode = drawMode
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

    override fun setBlendingModeForStereoSurfaceEntity(
        panelImpressNode: ImpressNode,
        @MediaBlendingMode blendingMode: Int,
    ) {
        val data =
            stereoSurfaceEntities[panelImpressNode]
                ?: throw IllegalArgumentException("Couldn't find stereo surface entity!")
        data.mediaBlendingMode = blendingMode
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

    /** Sets a material override for a specific primitive of a specific glTF model node. */
    override fun setGltfModelNodeMaterialOverride(
        impressNode: ImpressNode,
        nativeMaterial: Long,
        primitiveIndex: Int,
    ) {
        val gltfNodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        val materialData =
            materials[nativeMaterial] ?: throw IllegalArgumentException("Material not found")
        gltfNodeData.setGltfModelNodeMaterialOverride(materialData, primitiveIndex)
    }

    /** Clears a material override for a specific primitive of a specific glTF model node. */
    override fun clearGltfModelNodeMaterialOverride(impressNode: ImpressNode, primitiveIndex: Int) {
        val gltfNodeData =
            getGltfNodeData(impressNode) ?: throw IllegalArgumentException("Impress node not found")
        gltfNodeData.clearGltfModelNodeMaterialOverride(primitiveIndex)
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

    public fun registerModelHierarchy(gltfToken: Long, nodeNames: List<String>) {
        modelHierarchies[gltfToken] = nodeNames
    }

    private fun getGltfNodeData(impressNode: ImpressNode): GltfNodeData? {
        return impressNodes.keys.firstOrNull { it.entityId == impressNode.handle }
    }
}
