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

package androidx.xr.runtime.testing

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.xr.runtime.SubspaceNodeHolder
import androidx.xr.runtime.TypeHolder
import androidx.xr.runtime.internal.ActivityPanelEntity
import androidx.xr.runtime.internal.ActivitySpace
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.AnchorPlacement
import androidx.xr.runtime.internal.AudioTrackExtensionsWrapper
import androidx.xr.runtime.internal.CameraViewActivityPose
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.Entity
import androidx.xr.runtime.internal.ExrImageResource
import androidx.xr.runtime.internal.GltfEntity
import androidx.xr.runtime.internal.GltfModelResource
import androidx.xr.runtime.internal.HeadActivityPose
import androidx.xr.runtime.internal.InputEventListener
import androidx.xr.runtime.internal.InteractableComponent
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.KhronosPbrMaterialSpec
import androidx.xr.runtime.internal.LoggingEntity
import androidx.xr.runtime.internal.MaterialResource
import androidx.xr.runtime.internal.MediaPlayerExtensionsWrapper
import androidx.xr.runtime.internal.PanelEntity
import androidx.xr.runtime.internal.PerceptionSpaceActivityPose
import androidx.xr.runtime.internal.PixelDimensions
import androidx.xr.runtime.internal.PlaneSemantic
import androidx.xr.runtime.internal.PlaneType
import androidx.xr.runtime.internal.PointerCaptureComponent
import androidx.xr.runtime.internal.SoundPoolExtensionsWrapper
import androidx.xr.runtime.internal.SpatialCapabilities
import androidx.xr.runtime.internal.SpatialEnvironment
import androidx.xr.runtime.internal.SpatialModeChangeListener
import androidx.xr.runtime.internal.SpatialPointerComponent
import androidx.xr.runtime.internal.SpatialVisibility
import androidx.xr.runtime.internal.SubspaceNodeEntity
import androidx.xr.runtime.internal.SurfaceEntity
import androidx.xr.runtime.internal.TextureResource
import androidx.xr.runtime.internal.TextureSampler
import androidx.xr.runtime.math.Matrix3
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4
import com.google.androidxr.splitengine.SubspaceNode
import com.google.common.util.concurrent.Futures.immediateFailedFuture
import com.google.common.util.concurrent.Futures.immediateFuture
import com.google.common.util.concurrent.ListenableFuture
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executor
import java.util.function.Consumer

// TODO: b/405218432 - Implement this correctly instead of stubbing it out.
/** Test-only implementation of [JxrPlatformAdapter] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeJxrPlatformAdapter : JxrPlatformAdapter {
    /* Tracks the current state of the adapter according to where it is in its lifecycle. */
    public enum class State {
        CREATED,
        STARTED,
        PAUSED,
        DESTROYED,
    }

    private var _state: Enum<State> = State.CREATED

    /**
     * The current state of the adapter will transition based on the lifecycle of the adapter. It
     * starts off as [State.CREATED] and transitions to [State.STARTED] when startRenderer is
     * called. When stopRenderer is called, it transitions to [State.PAUSED]. When dispose is
     * called, it transitions to [State.DESTROYED].
     */
    public val state: Enum<State>
        get() = _state

    override val spatialEnvironment: SpatialEnvironment = FakeSpatialEnvironment()

    override val mainPanelEntity: PanelEntity = FakePanelEntity()

    override val activitySpace: ActivitySpace = FakeActivitySpace()

    override val headActivityPose: HeadActivityPose? =
        object : HeadActivityPose, FakeActivityPose() {}

    override val activitySpaceRootImpl: Entity = activitySpace

    override val spatialCapabilities: SpatialCapabilities = SpatialCapabilities(0)

    override val perceptionSpaceActivityPose: PerceptionSpaceActivityPose =
        object : PerceptionSpaceActivityPose, FakeActivityPose() {}

    override val soundPoolExtensionsWrapper: SoundPoolExtensionsWrapper =
        FakeSoundPoolExtensionsWrapper()

    override val audioTrackExtensionsWrapper: AudioTrackExtensionsWrapper =
        FakeAudioTrackExtensionsWrapper()

    override val mediaPlayerExtensionsWrapper: MediaPlayerExtensionsWrapper =
        FakeMediaPlayerExtensionsWrapper()

    override var spatialModeChangeListener: SpatialModeChangeListener =
        FakeSpatialModeChangeListener()

    override fun getCameraViewActivityPose(
        @CameraViewActivityPose.CameraType cameraType: Int
    ): CameraViewActivityPose? = FakeCameraViewActivityPose()

    @Suppress("AsyncSuffixFuture")
    override fun loadGltfByAssetName(assetName: String): ListenableFuture<GltfModelResource> =
        immediateFuture(FakeGltfModelResource(0))

    @Suppress("AsyncSuffixFuture")
    override fun loadGltfByByteArray(
        assetData: ByteArray,
        assetKey: String,
    ): ListenableFuture<GltfModelResource> = immediateFuture(FakeGltfModelResource(0))

    @Suppress("AsyncSuffixFuture")
    override fun loadExrImageByAssetName(assetName: String): ListenableFuture<ExrImageResource> =
        immediateFuture(FakeExrImageResource(0))

    @Suppress("AsyncSuffixFuture")
    override fun loadExrImageByByteArray(
        assetData: ByteArray,
        assetKey: String,
    ): ListenableFuture<ExrImageResource> = immediateFailedFuture(NotImplementedError())

    @Suppress("AsyncSuffixFuture")
    override fun loadTexture(
        assetName: String,
        sampler: TextureSampler,
    ): ListenableFuture<TextureResource>? = immediateFailedFuture(NotImplementedError())

    /**
     * For test purposes only.
     *
     * Controls the `TextureResource` instance returned by [borrowReflectionTexture] and
     * [getReflectionTextureFromIbl].
     *
     * <p>Tests can set this property to a [FakeResource] instance to simulate the availability of a
     * reflection texture. This allows verification that the code under test correctly handles the
     * borrowed or retrieved texture. Calling [destroyTexture] will reset this property to `null`,
     * enabling tests to also verify resource cleanup behavior.
     */
    internal var reflectionTexture: FakeResource? = null

    override fun borrowReflectionTexture(): TextureResource? {
        return reflectionTexture
    }

    override fun destroyTexture(texture: TextureResource) {
        reflectionTexture = null
    }

    override fun getReflectionTextureFromIbl(iblToken: ExrImageResource): TextureResource? {
        return reflectionTexture
    }

    /**
     * For test purposes only.
     *
     * A fake implementation of [MaterialResource] used to simulate a water material within the test
     * environment.
     *
     * <p>Instances of this class are created by [createWaterMaterial] and can be accessed for
     * verification via the [createdWaterMaterials] list. Tests can inspect the public properties of
     * this class (e.g., [reflectionMap], [normalTiling]) to confirm that the code under test
     * correctly configures the material's attributes.
     *
     * @param isAlphaMapVersion The value provided during creation, indicating which version of the
     *   water material was requested.
     */
    public class FakeWaterMaterial(public val isAlphaMapVersion: Boolean) : MaterialResource {
        public var reflectionMap: TextureResource? = null
        public var normalMap: TextureResource? = null
        public var normalTiling: Float = 0.0f
        public var normalSpeed: Float = 0.0f
        public var alphaStepMultiplier: Float = 0.0f
        public var alphaMap: TextureResource? = null
        public var normalZ: Float = 0.0f
        public var normalBoundary: Float = 0.0f
    }

    /**
     * For test purposes only.
     *
     * A list of all [FakeWaterMaterial] instances created via [createWaterMaterial]. Tests can
     * inspect this list to verify the number of materials created and to access their properties
     * for further assertions.
     */
    public val createdWaterMaterials: MutableList<FakeWaterMaterial> =
        mutableListOf<FakeWaterMaterial>()

    @Suppress("AsyncSuffixFuture")
    override fun createWaterMaterial(
        isAlphaMapVersion: Boolean
    ): ListenableFuture<MaterialResource>? {
        val newMaterial = FakeWaterMaterial(isAlphaMapVersion)
        createdWaterMaterials.add(newMaterial)
        return immediateFuture(newMaterial)
    }

    override fun destroyWaterMaterial(material: MaterialResource) {
        createdWaterMaterials.remove(material)
    }

    override fun setReflectionMapOnWaterMaterial(
        material: MaterialResource,
        reflectionMap: TextureResource,
    ) {
        (material as? FakeWaterMaterial)?.reflectionMap = reflectionMap
    }

    override fun setNormalMapOnWaterMaterial(
        material: MaterialResource,
        normalMap: TextureResource,
    ) {
        (material as? FakeWaterMaterial)?.normalMap = normalMap
    }

    override fun setNormalTilingOnWaterMaterial(material: MaterialResource, normalTiling: Float) {
        (material as? FakeWaterMaterial)?.normalTiling = normalTiling
    }

    override fun setNormalSpeedOnWaterMaterial(material: MaterialResource, normalSpeed: Float) {
        (material as? FakeWaterMaterial)?.normalSpeed = normalSpeed
    }

    override fun setAlphaStepMultiplierOnWaterMaterial(
        material: MaterialResource,
        alphaStepMultiplier: Float,
    ) {
        (material as? FakeWaterMaterial)?.alphaStepMultiplier = alphaStepMultiplier
    }

    override fun setAlphaMapOnWaterMaterial(material: MaterialResource, alphaMap: TextureResource) {
        (material as? FakeWaterMaterial)?.alphaMap = alphaMap
    }

    override fun setNormalZOnWaterMaterial(material: MaterialResource, normalZ: Float) {
        (material as? FakeWaterMaterial)?.normalZ = normalZ
    }

    override fun setNormalBoundaryOnWaterMaterial(
        material: MaterialResource,
        normalBoundary: Float,
    ) {
        (material as? FakeWaterMaterial)?.normalBoundary = normalBoundary
    }

    /**
     * For test purposes only.
     *
     * A fake implementation of [MaterialResource] used to simulate a Khronos PBR material within
     * the test environment.
     *
     * <p>Instances of this class are created by [createKhronosPbrMaterial]. Tests can inspect the
     * public properties of this class (e.g., [baseColorTexture], [metallicFactor]) to confirm that
     * the code under test correctly configures the material's attributes according to the provided
     * specification.
     *
     * @param spec The [KhronosPbrMaterialSpec] provided during creation, which defines the initial
     *   configuration of the material.
     */
    public class FakeKhronosPbrMaterial(public val spec: KhronosPbrMaterialSpec) :
        MaterialResource {
        public var baseColorTexture: TextureResource? = null
        public var baseColorUvTransform: Matrix3? = null
        public var baseColorFactors: Vector4? = null
        public var metallicRoughnessTexture: TextureResource? = null
        public var metallicRoughnessUvTransform: Matrix3? = null
        public var metallicFactor: Float? = null
        public var roughnessFactor: Float? = null
        public var normalTexture: TextureResource? = null
        public var normalUvTransform: Matrix3? = null
        public var normalFactor: Float? = null
        public var ambientOcclusionTexture: TextureResource? = null
        public var ambientOcclusionUvTransform: Matrix3? = null
        public var ambientOcclusionFactor: Float? = null
        public var emissiveTexture: TextureResource? = null
        public var emissiveUvTransform: Matrix3? = null
        public var emissiveFactors: Vector3? = null
        public var clearcoatTexture: TextureResource? = null
        public var clearcoatNormalTexture: TextureResource? = null
        public var clearcoatRoughnessTexture: TextureResource? = null
        public var clearcoatIntensity: Float? = null
        public var clearcoatRoughness: Float? = null
        public var clearcoatNormalFactor: Float? = null
        public var sheenColorTexture: TextureResource? = null
        public var sheenColorFactors: Vector3? = null
        public var sheenRoughnessTexture: TextureResource? = null
        public var sheenRoughnessFactor: Float? = null
        public var transmissionTexture: TextureResource? = null
        public var transmissionUvTransform: Matrix3? = null
        public var transmissionFactor: Float? = null
        public var indexOfRefraction: Float? = null
        public var alphaCutoff: Float? = null
    }

    public val createdKhronosPbrMaterials: MutableList<FakeKhronosPbrMaterial> =
        mutableListOf<FakeKhronosPbrMaterial>()

    @Suppress("AsyncSuffixFuture")
    override fun createKhronosPbrMaterial(
        spec: KhronosPbrMaterialSpec
    ): ListenableFuture<MaterialResource>? {
        val newMaterial = FakeKhronosPbrMaterial(spec)
        createdKhronosPbrMaterials.add(newMaterial)
        return immediateFuture(newMaterial)
    }

    override fun destroyKhronosPbrMaterial(material: MaterialResource) {
        createdKhronosPbrMaterials.remove(material)
    }

    override fun setBaseColorTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        baseColor: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.baseColorTexture = baseColor
    }

    override fun setBaseColorUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        (material as? FakeKhronosPbrMaterial)?.baseColorUvTransform = uvTransform
    }

    override fun setBaseColorFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        factors: Vector4,
    ) {
        (material as? FakeKhronosPbrMaterial)?.baseColorFactors = factors
    }

    override fun setMetallicRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        metallicRoughness: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.metallicRoughnessTexture = metallicRoughness
    }

    override fun setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        (material as? FakeKhronosPbrMaterial)?.metallicRoughnessUvTransform = uvTransform
    }

    override fun setMetallicFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float) {
        (material as? FakeKhronosPbrMaterial)?.metallicFactor = factor
    }

    override fun setRoughnessFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float) {
        (material as? FakeKhronosPbrMaterial)?.roughnessFactor = factor
    }

    override fun setNormalTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        normal: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.normalTexture = normal
    }

    override fun setNormalUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        (material as? FakeKhronosPbrMaterial)?.normalUvTransform = uvTransform
    }

    override fun setNormalFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float) {
        (material as? FakeKhronosPbrMaterial)?.normalFactor = factor
    }

    override fun setAmbientOcclusionTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        ambientOcclusion: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.ambientOcclusionTexture = ambientOcclusion
    }

    override fun setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        (material as? FakeKhronosPbrMaterial)?.ambientOcclusionUvTransform = uvTransform
    }

    override fun setAmbientOcclusionFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    ) {
        (material as? FakeKhronosPbrMaterial)?.ambientOcclusionFactor = factor
    }

    override fun setEmissiveTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        emissive: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.emissiveTexture = emissive
    }

    override fun setEmissiveUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        (material as? FakeKhronosPbrMaterial)?.emissiveUvTransform = uvTransform
    }

    override fun setEmissiveFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        factors: Vector3,
    ) {
        (material as? FakeKhronosPbrMaterial)?.emissiveFactors = factors
    }

    override fun setClearcoatTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoat: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.clearcoatTexture = clearcoat
    }

    override fun setClearcoatNormalTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoatNormal: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.clearcoatNormalTexture = clearcoatNormal
    }

    override fun setClearcoatRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoatRoughness: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.clearcoatRoughnessTexture = clearcoatRoughness
    }

    override fun setClearcoatFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        intensity: Float,
        roughness: Float,
        normal: Float,
    ) {
        (material as? FakeKhronosPbrMaterial)?.clearcoatIntensity = intensity
        (material as? FakeKhronosPbrMaterial)?.clearcoatRoughness = roughness
        (material as? FakeKhronosPbrMaterial)?.clearcoatNormalFactor = normal
    }

    override fun setSheenColorTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        sheenColor: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.sheenColorTexture = sheenColor
    }

    override fun setSheenColorFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        factors: Vector3,
    ) {
        (material as? FakeKhronosPbrMaterial)?.sheenColorFactors = factors
    }

    override fun setSheenRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        sheenRoughness: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.sheenRoughnessTexture = sheenRoughness
    }

    override fun setSheenRoughnessFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    ) {
        (material as? FakeKhronosPbrMaterial)?.sheenRoughnessFactor = factor
    }

    override fun setTransmissionTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        transmission: TextureResource,
    ) {
        (material as? FakeKhronosPbrMaterial)?.transmissionTexture = transmission
    }

    override fun setTransmissionUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {
        (material as? FakeKhronosPbrMaterial)?.transmissionUvTransform = uvTransform
    }

    override fun setTransmissionFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    ) {
        (material as? FakeKhronosPbrMaterial)?.transmissionFactor = factor
    }

    override fun setIndexOfRefractionOnKhronosPbrMaterial(
        material: MaterialResource,
        indexOfRefraction: Float,
    ) {
        (material as? FakeKhronosPbrMaterial)?.indexOfRefraction = indexOfRefraction
    }

    override fun setAlphaCutoffOnKhronosPbrMaterial(
        material: MaterialResource,
        alphaCutoff: Float,
    ) {
        (material as? FakeKhronosPbrMaterial)?.alphaCutoff = alphaCutoff
    }

    override fun createGltfEntity(
        pose: Pose,
        loadedGltf: GltfModelResource,
        parentEntity: Entity,
    ): GltfEntity {
        val gltfEntity = FakeGltfEntity()
        gltfEntity.setPose(pose)
        gltfEntity.parent = parentEntity

        return gltfEntity
    }

    override fun createSurfaceEntity(
        stereoMode: Int,
        pose: Pose,
        canvasShape: SurfaceEntity.CanvasShape,
        contentSecurityLevel: Int,
        superSampling: Int,
        parentEntity: Entity,
    ): SurfaceEntity {
        val surfaceEntity = FakeSurfaceEntity()
        surfaceEntity.stereoMode = stereoMode
        surfaceEntity.setPose(pose)
        surfaceEntity.canvasShape = canvasShape
        surfaceEntity.parent = parentEntity

        return surfaceEntity
    }

    /** This value is used to verify the result of [enablePanelDepthTest] in tests. */
    internal var enabledPanelDepthTest: Boolean = false

    override fun enablePanelDepthTest(enabled: Boolean) {
        enabledPanelDepthTest = enabled
    }

    /**
     * For test purposes only.
     *
     * A map tracking the listeners registered for spatial capability changes. The key is the
     * [Executor] on which the listener should be invoked, and the value is the [Consumer] listener
     * itself.
     *
     * <p>This map is populated by calls to [addSpatialCapabilitiesChangedListener] and modified by
     * [removeSpatialCapabilitiesChangedListener]. Tests can inspect its contents to verify that the
     * correct listeners are registered with their intended executors.
     */
    public val spatialCapabilitiesChangedMap: Map<Executor, Consumer<SpatialCapabilities>>
        get() = _spatialCapabilitiesChangedMap

    private val _spatialCapabilitiesChangedMap:
        MutableMap<Executor, Consumer<SpatialCapabilities>> =
        mutableMapOf()

    override fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ) {
        _spatialCapabilitiesChangedMap[callbackExecutor] = listener
    }

    override fun removeSpatialCapabilitiesChangedListener(listener: Consumer<SpatialCapabilities>) {
        _spatialCapabilitiesChangedMap.values.remove(listener)
    }

    /**
     * For test purposes only.
     *
     * A map tracking the listener registered for spatial visibility changes. The key is the
     * [Executor] on which the listener should be invoked, and the value is the [Consumer] listener
     * itself.
     *
     * <p>This map is populated by calls to [setSpatialVisibilityChangedListener] and cleared by
     * [clearSpatialVisibilityChangedListener]. Tests can inspect its contents to verify that the
     * correct listener is registered or that it has been successfully cleared.
     */
    public val spatialVisibilityChangedMap: Map<Executor, Consumer<SpatialVisibility>>
        get() = _spatialVisibilityChangedMap

    private val _spatialVisibilityChangedMap: MutableMap<Executor, Consumer<SpatialVisibility>> =
        mutableMapOf()

    override fun setSpatialVisibilityChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialVisibility>,
    ) {
        _spatialVisibilityChangedMap[callbackExecutor] = listener
    }

    override fun clearSpatialVisibilityChangedListener() {
        _spatialVisibilityChangedMap.clear()
    }

    /**
     * For test purposes only.
     *
     * A map tracking the listeners registered for perceived resolution changes. The key is the
     * [Executor] on which the listener should be invoked, and the value is the [Consumer] listener
     * itself.
     *
     * <p>This map is populated by calls to [addPerceivedResolutionChangedListener] and modified by
     * [removePerceivedResolutionChangedListener]. Tests can inspect its contents to verify that the
     * correct listeners are registered or that they have been successfully removed.
     */
    public val perceivedResolutionChangedMap: Map<Executor, Consumer<PixelDimensions>>
        get() = _perceivedResolutionChangedMap

    private val _perceivedResolutionChangedMap: MutableMap<Executor, Consumer<PixelDimensions>> =
        mutableMapOf()

    override fun addPerceivedResolutionChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<PixelDimensions>,
    ) {
        _perceivedResolutionChangedMap[callbackExecutor] = listener
    }

    override fun removePerceivedResolutionChangedListener(listener: Consumer<PixelDimensions>) {
        _perceivedResolutionChangedMap.values.remove(listener)
    }

    override fun createLoggingEntity(pose: Pose): LoggingEntity =
        object : LoggingEntity, FakeEntity() {}

    /** This value is used to verify [requestedFullSpaceMode] is invoked. */
    public var requestedFullSpaceMode: Boolean = false

    override fun requestFullSpaceMode() {
        requestedFullSpaceMode = true
    }

    /** This value is used to verify [requestHomeSpaceMode] is invoked. */
    public var requestedHomeSpaceMode: Boolean = false

    override fun requestHomeSpaceMode() {
        requestedHomeSpaceMode = true
    }

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        dimensions: Dimensions,
        name: String,
        parent: Entity,
    ): PanelEntity {
        val panelEntity = FakePanelEntity()
        panelEntity.setPose(pose)
        panelEntity.size = dimensions
        panelEntity.parent = parent

        return panelEntity
    }

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        pixelDimensions: PixelDimensions,
        name: String,
        parent: Entity,
    ): PanelEntity {
        val panelEntity = FakePanelEntity()
        panelEntity.setPose(pose)
        panelEntity.sizeInPixels = pixelDimensions
        panelEntity.parent = parent

        return panelEntity
    }

    override fun createActivityPanelEntity(
        pose: Pose,
        windowBoundsPx: PixelDimensions,
        name: String,
        hostActivity: Activity,
        parent: Entity,
    ): ActivityPanelEntity {
        val activityPanelEntity = FakeActivityPanelEntity()
        activityPanelEntity.setPose(pose)
        activityPanelEntity.sizeInPixels = windowBoundsPx
        activityPanelEntity.parent = parent

        return activityPanelEntity
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createAnchorEntity(
        bounds: Dimensions,
        planeType: PlaneType,
        planeSemantic: PlaneSemantic,
        searchTimeout: Duration,
    ): FakeAnchorEntity {
        val anchorCreationData =
            FakeAnchorEntity.AnchorCreationData(
                bounds = bounds,
                planeType = planeType,
                planeSemantic = planeSemantic,
                searchTimeout = searchTimeout,
            )
        return FakeAnchorEntity(anchorCreationData)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createAnchorEntity(anchor: Anchor): FakeAnchorEntity {
        return FakeAnchorEntity(anchor = anchor)
    }

    override fun createGroupEntity(pose: Pose, name: String, parent: Entity): Entity {
        val entity = FakeEntity()
        entity.setPose(pose)
        entity.parent = parent

        return entity
    }

    override fun createSubspaceNodeEntity(
        subspaceNodeHolder: SubspaceNodeHolder<*>,
        size: Dimensions,
    ): SubspaceNodeEntity =
        FakeSubspaceNodeEntity(
            TypeHolder.assertGetValue(subspaceNodeHolder, SubspaceNode::class.java),
            size,
        )

    @Suppress("ExecutorRegistration")
    override fun createInteractableComponent(
        executor: Executor,
        listener: InputEventListener,
    ): InteractableComponent = FakeInteractableComponent()

    override fun createMovableComponent(
        systemMovable: Boolean,
        scaleInZ: Boolean,
        anchorPlacement: Set<@JvmSuppressWildcards AnchorPlacement>,
        shouldDisposeParentAnchor: Boolean,
    ): FakeMovableComponent = FakeMovableComponent()

    override fun createAnchorPlacementForPlanes(
        planeTypeFilter: Set<@JvmSuppressWildcards PlaneType>,
        planeSemanticFilter: Set<@JvmSuppressWildcards PlaneSemantic>,
    ): AnchorPlacement = object : AnchorPlacement {}

    override fun createResizableComponent(
        minimumSize: Dimensions,
        maximumSize: Dimensions,
    ): FakeResizableComponent {
        val resizableComponent =
            FakeResizableComponent(minimumSize = minimumSize, maximumSize = maximumSize)

        return resizableComponent
    }

    @Suppress("ExecutorRegistration")
    override fun createPointerCaptureComponent(
        executor: Executor,
        stateListener: PointerCaptureComponent.StateListener,
        inputListener: InputEventListener,
    ): FakePointerCaptureComponent {
        return FakePointerCaptureComponent(executor, stateListener)
    }

    override fun createSpatialPointerComponent(): SpatialPointerComponent =
        FakeSpatialPointerComponent()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createPersistedAnchorEntity(
        uuid: UUID,
        searchTimeout: Duration,
    ): FakeAnchorEntity {
        val anchorCreationData =
            FakeAnchorEntity.AnchorCreationData(searchTimeout = searchTimeout, uuid = uuid)
        return FakeAnchorEntity(anchorCreationData)
    }

    override fun setFullSpaceMode(bundle: Bundle): Bundle = bundle

    override fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle = bundle

    /**
     * For test purposes only.
     *
     * Stores the [Activity] that was last provided to the [setPreferredAspectRatio] method. Tests
     * can inspect this property to verify the correct activity was used.
     */
    public var lastSetPreferredAspectRatioActivity: Activity? = null

    /**
     * For test purposes only.
     *
     * Stores the ratio that was last provided to the [setPreferredAspectRatio] method. Tests can
     * inspect this property to verify the correct ratio was set.
     */
    public var lastSetPreferredAspectRatioRatio: Float = -1f

    override fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float) {
        lastSetPreferredAspectRatioActivity = activity
        lastSetPreferredAspectRatioRatio = preferredRatio
    }

    override fun startRenderer() {
        _state = State.STARTED
    }

    override fun stopRenderer() {
        _state = State.PAUSED
    }

    override fun dispose() {
        _state = State.DESTROYED
    }
}
