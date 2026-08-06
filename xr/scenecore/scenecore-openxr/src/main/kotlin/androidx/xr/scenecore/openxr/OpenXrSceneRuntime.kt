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

package androidx.xr.scenecore.openxr

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
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
import androidx.xr.scenecore.runtime.SpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialEnvironment
import androidx.xr.scenecore.runtime.SpatialModeChangeListener
import androidx.xr.scenecore.runtime.SpatialPointerComponent
import androidx.xr.scenecore.runtime.SpatialVisibility
import androidx.xr.scenecore.runtime.SubspaceNodeEntity
import androidx.xr.scenecore.runtime.TrackableComponent
import java.util.concurrent.Executor
import java.util.function.Consumer

internal class OpenXrSceneRuntime
private constructor(
    private val activity: Activity,
    private val unscaledGravityAlignedActivitySpace: Boolean = true,
) : SceneRuntime {

    internal val nativeWrapper = SceneCoreOpenXrNative()

    internal var isDestroyed: Boolean = false
        private set

    override val config: Config = Config.Builder().build()

    override fun initialize() {
        check(!isDestroyed) { "Cannot initialize OpenXrSceneRuntime after it has been destroyed." }
        val nativeData = XrDevice.getCurrentDevice(activity).getNativeInstanceData(activity)
        // TODO: b/538912011 - Validate instancePointer and check return values of
        // nativeWrapper.init() and createSpatialContainer().
        if (nativeData.instancePointer != INVALID_HANDLE) {
            nativeWrapper.init(
                nativeData.instancePointer,
                INVALID_HANDLE, // TODO: b/538912011 - Provide real session handle.
                nativeData.functionTablePointer,
            )
            nativeWrapper.createSpatialContainer()
        }
    }

    override fun destroy() {
        if (isDestroyed) return
        isDestroyed = true
        nativeWrapper.destroy()
    }

    override val spatialCapabilities: SpatialCapabilities
        get() = TODO("OpenXrSceneRuntime.spatialCapabilities is not yet implemented")

    override val activitySpace: ActivitySpace
        get() = TODO("OpenXrSceneRuntime.activitySpace is not yet implemented")

    override val perceptionSpaceActivityPose: PerceptionSpaceScenePose
        get() = TODO("OpenXrSceneRuntime.perceptionSpaceActivityPose is not yet implemented")

    override val mainPanelEntity: PanelEntity
        get() = TODO("OpenXrSceneRuntime.mainPanelEntity is not yet implemented")

    override var keyEntity: Entity?
        get() = TODO("OpenXrSceneRuntime.keyEntity getter is not yet implemented")
        set(_) {
            TODO("OpenXrSceneRuntime.keyEntity setter is not yet implemented")
        }

    override val spatialEnvironment: SpatialEnvironment
        get() = TODO("OpenXrSceneRuntime.spatialEnvironment is not yet implemented")

    override var spatialModeChangeListener: SpatialModeChangeListener?
        get() = TODO("OpenXrSceneRuntime.spatialModeChangeListener getter is not yet implemented")
        set(_) {
            TODO("OpenXrSceneRuntime.spatialModeChangeListener setter is not yet implemented")
        }

    override val soundPoolExtensionsWrapper: SoundPoolExtensionsWrapper
        get() = TODO("OpenXrSceneRuntime.soundPoolExtensionsWrapper is not yet implemented")

    override val audioTrackExtensionsWrapper: AudioTrackExtensionsWrapper
        get() = TODO("OpenXrSceneRuntime.audioTrackExtensionsWrapper is not yet implemented")

    override val mediaPlayerExtensionsWrapper: MediaPlayerExtensionsWrapper
        get() = TODO("OpenXrSceneRuntime.mediaPlayerExtensionsWrapper is not yet implemented")

    override val isBoundaryConsentGranted: Boolean
        get() = TODO("OpenXrSceneRuntime.isBoundaryConsentGranted is not yet implemented")

    override fun getScenePoseFromPerceptionPose(pose: Pose): ScenePose =
        TODO("OpenXrSceneRuntime.getScenePoseFromPerceptionPose is not yet implemented")

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

    override fun createAnchorEntity(): AnchorEntity =
        TODO("OpenXrSceneRuntime.createAnchorEntity is not yet implemented")

    override fun createEntity(pose: Pose, name: String?, parent: Entity?): Entity =
        TODO("OpenXrSceneRuntime.createEntity is not yet implemented")

    override fun createSubspaceNodeEntity(
        nodeHolder: NodeHolder<*>,
        size: Dimensions,
    ): SubspaceNodeEntity =
        TODO("OpenXrSceneRuntime.createSubspaceNodeEntity is not yet implemented")

    override fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ) {
        TODO("OpenXrSceneRuntime.addSpatialCapabilitiesChangedListener is not yet implemented")
    }

    override fun removeSpatialCapabilitiesChangedListener(listener: Consumer<SpatialCapabilities>) {
        TODO("OpenXrSceneRuntime.removeSpatialCapabilitiesChangedListener is not yet implemented")
    }

    override fun setSpatialVisibilityChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialVisibility>,
    ) {
        TODO("OpenXrSceneRuntime.setSpatialVisibilityChangedListener is not yet implemented")
    }

    override fun clearSpatialVisibilityChangedListener() {
        TODO("OpenXrSceneRuntime.clearSpatialVisibilityChangedListener is not yet implemented")
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
