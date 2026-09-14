/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("BanConcurrentHashMap")
@file:SuppressLint("RestrictedApiAndroidX")

package androidx.xr.scenecore.openxr

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.Trackable
import androidx.xr.runtime.Config
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.getNativeInstanceData
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.ActivityPanelEntity
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.AnchorPlacement
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.BoundsComponent
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.InteractableComponent
import androidx.xr.scenecore.runtime.MediaPlayerExtensionsWrapper
import androidx.xr.scenecore.runtime.MovableComponent
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.PlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.PointerCaptureComponent
import androidx.xr.scenecore.runtime.PositionalAudioComponent
import androidx.xr.scenecore.runtime.ResizableComponent
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SoundEffectPool
import androidx.xr.scenecore.runtime.SoundEffectPoolComponent
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SoundFieldAudioComponent
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialEnvironment
import androidx.xr.scenecore.runtime.SpatialModeChangeListener
import androidx.xr.scenecore.runtime.SpatialPointerComponent
import androidx.xr.scenecore.runtime.SpatialVisibility
import androidx.xr.scenecore.runtime.SubspaceNodeEntity
import androidx.xr.scenecore.runtime.TrackableComponent
import androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl
import androidx.xr.scenecore.runtime.impl.PlatformReferenceScenePose
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer

/** Implementation of [SceneRuntime] for devices that support OpenXR. */
internal class OpenXrSceneRuntime
/**
 * Internal constructor visible for testing to allow injecting test doubles and dependencies.
 *
 * For standard production creation, use [OpenXrSceneRuntime.create].
 *
 * @param activity The [Activity] hosting this SceneCore session.
 * @param unscaledGravityAlignedActivitySpace Whether the activity space is unscaled and
 *   gravity-aligned.
 * @param nativeWrapper The [SceneCoreOpenXrNative] bridge for native OpenXR calls.
 * @param sceneNodeRegistry The [OpenXrSceneNodeRegistry] tracking entity node mappings.
 * @param scheduledExecutorService The [ScheduledExecutorService] for asynchronous tasks.
 */
@VisibleForTesting
internal constructor(
    private var activity: Activity?,
    private val unscaledGravityAlignedActivitySpace: Boolean = true,
    internal val nativeWrapper: SceneCoreOpenXrNative = SceneCoreOpenXrNative(),
    internal val sceneNodeRegistry: OpenXrSceneNodeRegistry = OpenXrSceneNodeRegistry(),
    private val scheduledExecutorService: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(),
) : SceneRuntime {

    internal var isDestroyed: Boolean = false
        private set

    override val config: Config = Config.Builder().build()

    override val activitySpace: ActivitySpace =
        OpenXrActivitySpace(
            activity,
            INVALID_HANDLE,
            nativeWrapper,
            sceneNodeRegistry,
            scheduledExecutorService,
        )

    private val perceptionSpaceScenePose: PerceptionSpaceScenePose =
        PerceptionSpaceScenePoseImpl(activitySpace)

    private val spatialCapabilitiesChangedListeners =
        ConcurrentHashMap<Consumer<SpatialCapabilities>, Executor>()
    @Volatile
    private var spatialVisibilityHandler: Pair<Executor, Consumer<SpatialVisibility>>? = null

    init {
        sceneNodeRegistry.addSystemSpaceScenePose(activitySpace)
        sceneNodeRegistry.addSystemSpaceScenePose(perceptionSpaceScenePose)
    }

    internal var isInitialized: Boolean = false
        private set

    override fun initialize() {
        check(!isDestroyed) { "Cannot initialize OpenXrSceneRuntime after it has been destroyed." }
        if (isInitialized) return
        isInitialized = true
        val nativeData = XrDevice.getCurrentDevice(activity!!).getNativeInstanceData(activity!!)
        if (nativeData.instancePointer != INVALID_HANDLE && nativeData.instancePointer != 0L) {
            if (
                !nativeWrapper.init(
                    nativeData.instancePointer,
                    nativeData.sessionPointer,
                    nativeData.functionTablePointer,
                )
            ) {
                throw IllegalStateException("SceneCoreOpenXrNative.init failed.")
            }
            if (!nativeWrapper.createSpatialContainer()) {
                throw IllegalStateException("SceneCoreOpenXrNative.createSpatialContainer failed.")
            }
            val rootHandle = nativeWrapper.getRootEntityHandle()
            if (rootHandle != INVALID_HANDLE) {
                (activitySpace as? OpenXrEntity)?.bindEntityHandle(rootHandle)
                (mainPanelEntity as? OpenXrEntity)?.bindEntityHandle(
                    nativeWrapper.createSceneEntity()
                )
                mainPanelEntity.parent = activitySpace
            }
        }
    }

    override fun destroy() {
        if (isDestroyed) return
        activity = null
        isDestroyed = true
        (activitySpace as? OpenXrEntity)?.dispose()
        (mainPanelEntity as? OpenXrEntity)?.dispose()
        sceneNodeRegistry.getAllEntities().forEach(Entity::dispose)
        sceneNodeRegistry.clear()
        spatialCapabilitiesChangedListeners.clear()
        spatialVisibilityHandler = null
        scheduledExecutorService.shutdown()
        nativeWrapper.destroy()
    }

    // TODO: b/558684002 - Implement actual OpenXR spatial capabilities query.
    override val spatialCapabilities: SpatialCapabilities = SpatialCapabilities(0)

    override val perceptionSpaceActivityPose: PerceptionSpaceScenePose
        get() = perceptionSpaceScenePose

    override val mainPanelEntity: PanelEntity =
        OpenXrMainPanelEntity(
            activity,
            INVALID_HANDLE,
            nativeWrapper,
            sceneNodeRegistry,
            scheduledExecutorService,
            parent = activitySpace,
        )

    // TODO: b/558683521 - Implement keyEntity spatial continuity hint.
    override var keyEntity: Entity? = null

    override val spatialEnvironment: SpatialEnvironment = OpenXrSpatialEnvironment(nativeWrapper)

    // TODO: b/558683128 - Implement spatial mode change callback handling.
    override var spatialModeChangeListener: SpatialModeChangeListener? = null

    override val soundPoolExtensionsWrapper: SoundPoolExtensionsWrapper
        get() = TODO("OpenXrSceneRuntime.soundPoolExtensionsWrapper is not yet implemented")

    override val audioTrackExtensionsWrapper: AudioTrackExtensionsWrapper
        get() = TODO("OpenXrSceneRuntime.audioTrackExtensionsWrapper is not yet implemented")

    override val mediaPlayerExtensionsWrapper: MediaPlayerExtensionsWrapper
        get() = TODO("OpenXrSceneRuntime.mediaPlayerExtensionsWrapper is not yet implemented")

    override val isBoundaryConsentGranted: Boolean
        get() = TODO("OpenXrSceneRuntime.isBoundaryConsentGranted is not yet implemented")

    override fun getScenePoseFromPerceptionPose(pose: Pose): ScenePose =
        PlatformReferenceScenePose(activitySpace, pose)

    // TODO: b/538961468 - Implement OpenXrPanelEntity with SurfaceControlViewHost support.
    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        dimensions: Dimensions,
        name: String,
        parent: Entity?,
    ): PanelEntity = TODO("OpenXrSceneRuntime.createPanelEntity is not yet implemented")

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        pixelDimensions: PixelDimensions,
        name: String,
        parent: Entity?,
    ): PanelEntity = TODO("OpenXrSceneRuntime.createPanelEntity is not yet implemented")

    override fun createActivityPanelEntity(
        pose: Pose,
        windowBoundsPx: PixelDimensions,
        name: String,
        hostActivity: Activity,
        parent: Entity?,
    ): ActivityPanelEntity =
        TODO("OpenXrSceneRuntime.createActivityPanelEntity is not yet implemented")

    // TODO: b/538946673 - Implement OpenXrAnchorEntity and Panel-to-Anchor pinning.
    override fun createAnchorEntity(): AnchorEntity =
        TODO("OpenXrSceneRuntime.createAnchorEntity is not yet implemented")

    override fun createEntity(pose: Pose, name: String?, parent: Entity?): Entity {
        check(!isDestroyed) { "Cannot create entity after OpenXrSceneRuntime has been destroyed." }
        val entityHandle =
            if (nativeWrapper.nativeScenecore != INVALID_HANDLE) {
                nativeWrapper.createSceneEntity()
            } else {
                INVALID_HANDLE
            }

        // TODO: b/551976417 - Pass `name` down to native OpenXR scene entity via transaction once
        // supported.
        val entity: Entity =
            object :
                OpenXrEntity(
                    activity,
                    entityHandle,
                    nativeWrapper,
                    sceneNodeRegistry,
                    scheduledExecutorService,
                ) {}
        entity.parent = parent
        entity.setPose(pose, Space.PARENT)
        return entity
    }

    override fun createSubspaceNodeEntity(
        nodeHolder: NodeHolder<*>,
        size: Dimensions,
    ): SubspaceNodeEntity =
        TODO("OpenXrSceneRuntime.createSubspaceNodeEntity is not yet implemented")

    // TODO: b/558684002 - Implement OpenXR spatial capabilities callbacks.
    override fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ) {
        spatialCapabilitiesChangedListeners[listener] = callbackExecutor
    }

    override fun removeSpatialCapabilitiesChangedListener(listener: Consumer<SpatialCapabilities>) {
        spatialCapabilitiesChangedListeners.remove(listener)
    }

    // TODO: b/558687235 - Implement OpenXR spatial visibility callbacks.
    override fun setSpatialVisibilityChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialVisibility>,
    ) {
        spatialVisibilityHandler = callbackExecutor to listener
    }

    override fun clearSpatialVisibilityChangedListener() {
        spatialVisibilityHandler = null
    }

    override fun addPerceivedResolutionChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<PixelDimensions>,
    ) {
        TODO("OpenXrSceneRuntime.addPerceivedResolutionChangedListener is not yet implemented")
    }

    override fun removePerceivedResolutionChangedListener(listener: Consumer<PixelDimensions>) {
        TODO("OpenXrSceneRuntime.removePerceivedResolutionChangedListener is not yet implemented")
    }

    override fun requestFullSpaceMode() {
        TODO("OpenXrSceneRuntime.requestFullSpaceMode is not yet implemented")
    }

    override fun requestHomeSpaceMode() {
        TODO("OpenXrSceneRuntime.requestHomeSpaceMode is not yet implemented")
    }

    override fun setFullSpaceMode(bundle: Bundle): Bundle =
        TODO("OpenXrSceneRuntime.setFullSpaceMode is not yet implemented")

    override fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle =
        TODO("OpenXrSceneRuntime.setFullSpaceModeWithEnvironmentInherited is not yet implemented")

    override fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float) {
        TODO("OpenXrSceneRuntime.setPreferredAspectRatio is not yet implemented")
    }

    override fun enablePanelDepthTest(enabled: Boolean) {
        TODO("OpenXrSceneRuntime.enablePanelDepthTest is not yet implemented")
    }

    override fun createInteractableComponent(
        executor: Executor,
        listener: InputEventListener,
    ): InteractableComponent =
        TODO("OpenXrSceneRuntime.createInteractableComponent is not yet implemented")

    override fun createAnchorPlacementForPlanes(
        planeTypeFilter: Set<PlaneType>,
        planeSemanticFilter: Set<PlaneSemantic>,
    ): AnchorPlacement =
        TODO("OpenXrSceneRuntime.createAnchorPlacementForPlanes is not yet implemented")

    override fun createMovableComponent(
        systemMovable: Boolean,
        scaleInZ: Boolean,
        userAnchorable: Boolean,
    ): MovableComponent = TODO("OpenXrSceneRuntime.createMovableComponent is not yet implemented")

    override fun createTrackableComponent(
        lifecycleOwner: LifecycleOwner,
        trackable: Trackable<Trackable.State>,
        poseExtractor: (Any?) -> Pose?,
    ): TrackableComponent =
        TODO("OpenXrSceneRuntime.createTrackableComponent is not yet implemented")

    override fun createResizableComponent(
        minimumSize: Dimensions,
        maximumSize: Dimensions,
    ): ResizableComponent =
        TODO("OpenXrSceneRuntime.createResizableComponent is not yet implemented")

    override fun createPointerCaptureComponent(
        executor: Executor,
        stateListener: PointerCaptureComponent.StateListener,
        inputListener: InputEventListener,
    ): PointerCaptureComponent =
        TODO("OpenXrSceneRuntime.createPointerCaptureComponent is not yet implemented")

    override fun createSpatialPointerComponent(): SpatialPointerComponent =
        TODO("OpenXrSceneRuntime.createSpatialPointerComponent is not yet implemented")

    override fun createBoundsComponent(): BoundsComponent =
        TODO("OpenXrSceneRuntime.createBoundsComponent is not yet implemented")

    override fun addOnBoundaryConsentChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<Boolean>,
    ) {
        TODO("OpenXrSceneRuntime.addOnBoundaryConsentChangedListener is not yet implemented")
    }

    override fun removeOnBoundaryConsentChangedListener(listener: Consumer<Boolean>) {
        TODO("OpenXrSceneRuntime.removeOnBoundaryConsentChangedListener is not yet implemented")
    }

    override fun createPositionalAudioComponent(
        context: Context,
        params: PointSourceParams,
    ): PositionalAudioComponent =
        TODO("OpenXrSceneRuntime.createPositionalAudioComponent is not yet implemented")

    override fun createSoundFieldAudioComponent(
        context: Context,
        rtSoundFieldAttributes: SoundFieldAttributes,
    ): SoundFieldAudioComponent =
        TODO("OpenXrSceneRuntime.createSoundFieldAudioComponent is not yet implemented")

    override fun createSoundEffectPool(maxStreams: Int): SoundEffectPool =
        TODO("OpenXrSceneRuntime.createSoundEffectPool is not yet implemented")

    override fun createSoundEffectPoolComponent(
        soundEffectPool: SoundEffectPool
    ): SoundEffectPoolComponent =
        TODO("OpenXrSceneRuntime.createSoundEffectPoolComponent is not yet implemented")

    override val virtualPixelDensity: Float
        get() = 1000.0f

    companion object {
        fun create(
            activity: Activity,
            unscaledGravityAlignedActivitySpace: Boolean = true,
        ): OpenXrSceneRuntime = OpenXrSceneRuntime(activity, unscaledGravityAlignedActivitySpace)
    }
}
