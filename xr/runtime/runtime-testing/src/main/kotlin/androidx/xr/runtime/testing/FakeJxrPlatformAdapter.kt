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
import android.os.Bundle
import android.view.View
import androidx.annotation.RestrictTo
import androidx.xr.runtime.SubspaceNodeHolder
import androidx.xr.runtime.TypeHolder
import androidx.xr.runtime.internal.ActivityPanelEntity
import androidx.xr.runtime.internal.ActivitySpace
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.AnchorEntity
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
import androidx.xr.runtime.internal.MovableComponent
import androidx.xr.runtime.internal.PanelEntity
import androidx.xr.runtime.internal.PerceptionSpaceActivityPose
import androidx.xr.runtime.internal.PixelDimensions
import androidx.xr.runtime.internal.PlaneSemantic
import androidx.xr.runtime.internal.PlaneType
import androidx.xr.runtime.internal.PointerCaptureComponent
import androidx.xr.runtime.internal.ResizableComponent
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
    ): ListenableFuture<ExrImageResource> =
        immediateFailedFuture<ExrImageResource>(NotImplementedError())

    @Suppress("AsyncSuffixFuture")
    override fun loadTexture(
        assetName: String,
        sampler: TextureSampler,
    ): ListenableFuture<TextureResource>? =
        immediateFailedFuture<TextureResource>(NotImplementedError())

    override fun borrowReflectionTexture(): TextureResource? = null

    override fun destroyTexture(texture: TextureResource) {}

    override fun getReflectionTextureFromIbl(iblToken: ExrImageResource): TextureResource? = null

    @Suppress("AsyncSuffixFuture")
    override fun createWaterMaterial(
        isAlphaMapVersion: Boolean
    ): ListenableFuture<MaterialResource>? =
        immediateFailedFuture<MaterialResource>(NotImplementedError())

    override fun destroyWaterMaterial(material: MaterialResource) {}

    override fun setReflectionMapOnWaterMaterial(
        material: MaterialResource,
        reflectionMap: TextureResource,
    ) {}

    override fun setNormalMapOnWaterMaterial(
        material: MaterialResource,
        normalMap: TextureResource,
    ) {}

    override fun setNormalTilingOnWaterMaterial(material: MaterialResource, normalTiling: Float) {}

    override fun setNormalSpeedOnWaterMaterial(material: MaterialResource, normalSpeed: Float) {}

    override fun setAlphaStepMultiplierOnWaterMaterial(
        material: MaterialResource,
        alphaStepMultiplier: Float,
    ) {}

    override fun setAlphaMapOnWaterMaterial(
        material: MaterialResource,
        alphaMap: TextureResource,
    ) {}

    override fun setNormalZOnWaterMaterial(material: MaterialResource, normalZ: Float) {}

    override fun setNormalBoundaryOnWaterMaterial(
        material: MaterialResource,
        normalBoundary: Float,
    ) {}

    @Suppress("AsyncSuffixFuture")
    override fun createKhronosPbrMaterial(
        spec: KhronosPbrMaterialSpec
    ): ListenableFuture<MaterialResource>? =
        immediateFailedFuture<MaterialResource>(NotImplementedError())

    override fun destroyKhronosPbrMaterial(material: MaterialResource) {}

    override fun setBaseColorTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        baseColor: TextureResource,
    ) {}

    override fun setBaseColorUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {}

    override fun setBaseColorFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        factors: Vector4,
    ) {}

    override fun setMetallicRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        metallicRoughness: TextureResource,
    ) {}

    override fun setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {}

    override fun setMetallicFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float) {}

    override fun setRoughnessFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    ) {}

    override fun setNormalTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        normal: TextureResource,
    ) {}

    override fun setNormalUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {}

    override fun setNormalFactorOnKhronosPbrMaterial(material: MaterialResource, factor: Float) {}

    override fun setAmbientOcclusionTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        ambientOcclusion: TextureResource,
    ) {}

    override fun setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {}

    override fun setAmbientOcclusionFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    ) {}

    override fun setEmissiveTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        emissive: TextureResource,
    ) {}

    override fun setEmissiveUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {}

    override fun setEmissiveFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        factors: Vector3,
    ) {}

    override fun setClearcoatTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoat: TextureResource,
    ) {}

    override fun setClearcoatNormalTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoatNormal: TextureResource,
    ) {}

    override fun setClearcoatRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        clearcoatRoughness: TextureResource,
    ) {}

    override fun setClearcoatFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        intensity: Float,
        roughness: Float,
        normal: Float,
    ) {}

    override fun setSheenColorTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        sheenColor: TextureResource,
    ) {}

    override fun setSheenColorFactorsOnKhronosPbrMaterial(
        material: MaterialResource,
        factors: Vector3,
    ) {}

    override fun setSheenRoughnessTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        sheenRoughness: TextureResource,
    ) {}

    override fun setSheenRoughnessFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    ) {}

    override fun setTransmissionTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        transmission: TextureResource,
    ) {}

    override fun setTransmissionUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    ) {}

    override fun setTransmissionFactorOnKhronosPbrMaterial(
        material: MaterialResource,
        factor: Float,
    ) {}

    override fun setIndexOfRefractionOnKhronosPbrMaterial(
        material: MaterialResource,
        indexOfRefraction: Float,
    ) {}

    override fun setAlphaCutoffOnKhronosPbrMaterial(
        material: MaterialResource,
        alphaCutoff: Float,
    ) {}

    override fun createGltfEntity(
        pose: Pose,
        loadedGltf: GltfModelResource,
        parentEntity: Entity,
    ): GltfEntity = FakeGltfEntity()

    override fun createSurfaceEntity(
        stereoMode: Int,
        pose: Pose,
        canvasShape: SurfaceEntity.CanvasShape,
        contentSecurityLevel: Int,
        superSampling: Int,
        parentEntity: Entity,
    ): SurfaceEntity = FakeSurfaceEntity()

    override fun enablePanelDepthTest(enabled: Boolean) {}

    override fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ) {}

    override fun removeSpatialCapabilitiesChangedListener(
        listener: Consumer<SpatialCapabilities>
    ) {}

    override fun setSpatialVisibilityChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialVisibility>,
    ) {}

    override fun clearSpatialVisibilityChangedListener() {}

    override fun addPerceivedResolutionChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<PixelDimensions>,
    ) {}

    override fun removePerceivedResolutionChangedListener(listener: Consumer<PixelDimensions>) {}

    override fun createLoggingEntity(pose: Pose): LoggingEntity =
        object : LoggingEntity, FakeEntity() {}

    override fun requestFullSpaceMode() {}

    override fun requestHomeSpaceMode() {}

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        dimensions: Dimensions,
        name: String,
        parent: Entity,
    ): PanelEntity = FakePanelEntity()

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        pixelDimensions: PixelDimensions,
        name: String,
        parent: Entity,
    ): PanelEntity = FakePanelEntity()

    override fun createActivityPanelEntity(
        pose: Pose,
        windowBoundsPx: PixelDimensions,
        name: String,
        hostActivity: Activity,
        parent: Entity,
    ): ActivityPanelEntity = FakeActivityPanelEntity()

    override fun createAnchorEntity(
        bounds: Dimensions,
        planeType: PlaneType,
        planeSemantic: PlaneSemantic,
        searchTimeout: Duration,
    ): AnchorEntity = FakeAnchorEntity()

    override fun createAnchorEntity(anchor: Anchor): AnchorEntity = FakeAnchorEntity()

    override fun createGroupEntity(pose: Pose, name: String, parent: Entity): Entity = FakeEntity()

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
    ): InteractableComponent = object : InteractableComponent, FakeComponent() {}

    override fun createMovableComponent(
        systemMovable: Boolean,
        scaleInZ: Boolean,
        anchorPlacement: Set<@JvmSuppressWildcards AnchorPlacement>,
        shouldDisposeParentAnchor: Boolean,
    ): MovableComponent = FakeMovableComponent()

    override fun createAnchorPlacementForPlanes(
        planeTypeFilter: Set<@JvmSuppressWildcards PlaneType>,
        planeSemanticFilter: Set<@JvmSuppressWildcards PlaneSemantic>,
    ): AnchorPlacement = object : AnchorPlacement {}

    override fun createResizableComponent(
        minimumSize: Dimensions,
        maximumSize: Dimensions,
    ): ResizableComponent = FakeResizableComponent()

    @Suppress("ExecutorRegistration")
    override fun createPointerCaptureComponent(
        executor: Executor,
        stateListener: PointerCaptureComponent.StateListener,
        inputListener: InputEventListener,
    ): PointerCaptureComponent = object : PointerCaptureComponent, FakeComponent() {}

    override fun createSpatialPointerComponent(): SpatialPointerComponent =
        FakeSpatialPointerComponent()

    override fun createPersistedAnchorEntity(uuid: UUID, searchTimeout: Duration): AnchorEntity =
        FakeAnchorEntity()

    override fun setFullSpaceMode(bundle: Bundle): Bundle = bundle

    override fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle = bundle

    override fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float) {}

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
