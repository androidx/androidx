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

package androidx.xr.scenecore.projected

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
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
import androidx.xr.scenecore.runtime.LoggingEntity
import androidx.xr.scenecore.runtime.MediaPlayerExtensionsWrapper
import androidx.xr.scenecore.runtime.MovableComponent
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.PlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType
import androidx.xr.scenecore.runtime.PointerCaptureComponent
import androidx.xr.scenecore.runtime.ResizableComponent
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import androidx.xr.scenecore.runtime.SpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialEnvironment
import androidx.xr.scenecore.runtime.SpatialModeChangeListener
import androidx.xr.scenecore.runtime.SpatialPointerComponent
import androidx.xr.scenecore.runtime.SpatialVisibility
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer

internal class ProjectedSceneRuntime
internal constructor(
    private val activity: Activity,
    private val mServiceClient: ProjectedSceneCoreServiceClient,
    private val executor: ScheduledExecutorService,
) : SceneRuntime {

    private var mIsDestroyed = false

    override val spatialCapabilities: SpatialCapabilities
        get() = TODO("Not yet implemented")

    override val activitySpace: ActivitySpace
        get() = TODO("Not yet implemented")

    override val perceptionSpaceActivityPose: PerceptionSpaceScenePose
        get() = TODO("Not yet implemented")

    override val mainPanelEntity: PanelEntity
        get() = TODO("Not yet implemented")

    override var keyEntity: Entity? = null
        get() = TODO("Not yet implemented")

    override val spatialEnvironment: SpatialEnvironment
        get() = TODO("Not yet implemented")

    override var spatialModeChangeListener: SpatialModeChangeListener? = null
        get() = TODO("Not yet implemented")

    override val soundPoolExtensionsWrapper: SoundPoolExtensionsWrapper
        get() = TODO("Not yet implemented")

    override val audioTrackExtensionsWrapper: AudioTrackExtensionsWrapper
        get() = TODO("Not yet implemented")

    override val mediaPlayerExtensionsWrapper: MediaPlayerExtensionsWrapper
        get() = TODO("Not yet implemented")

    override val isBoundaryConsentGranted: Boolean
        get() = TODO("Not yet implemented")

    override fun getScenePoseFromPerceptionPose(pose: Pose): ScenePose {
        TODO("Not yet implemented")
    }

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        dimensions: Dimensions,
        name: String,
        parent: Entity?,
    ): PanelEntity {
        TODO("Not yet implemented")
    }

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        pixelDimensions: PixelDimensions,
        name: String,
        parent: Entity?,
    ): PanelEntity {
        TODO("Not yet implemented")
    }

    override fun createActivityPanelEntity(
        pose: Pose,
        windowBoundsPx: PixelDimensions,
        name: String,
        hostActivity: Activity,
        parent: Entity?,
    ): ActivityPanelEntity {
        TODO("Not yet implemented")
    }

    override fun createAnchorEntity(): AnchorEntity {
        TODO("Not yet implemented")
    }

    override fun createGroupEntity(pose: Pose, name: String, parent: Entity?): Entity {
        TODO("Not yet implemented")
    }

    override fun createLoggingEntity(pose: Pose): LoggingEntity {
        TODO("Not yet implemented")
    }

    override fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ) {
        TODO("Not yet implemented")
    }

    override fun removeSpatialCapabilitiesChangedListener(listener: Consumer<SpatialCapabilities>) {
        TODO("Not yet implemented")
    }

    override fun setSpatialVisibilityChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialVisibility>,
    ) {
        TODO("Not yet implemented")
    }

    override fun clearSpatialVisibilityChangedListener() {
        TODO("Not yet implemented")
    }

    override fun addPerceivedResolutionChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<PixelDimensions>,
    ) {
        TODO("Not yet implemented")
    }

    override fun removePerceivedResolutionChangedListener(listener: Consumer<PixelDimensions>) {
        TODO("Not yet implemented")
    }

    override fun requestFullSpaceMode() {
        TODO("Not yet implemented")
    }

    override fun requestHomeSpaceMode() {
        TODO("Not yet implemented")
    }

    override fun setFullSpaceMode(bundle: Bundle): Bundle {
        TODO("Not yet implemented")
    }

    override fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle {
        TODO("Not yet implemented")
    }

    override fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float) {
        TODO("Not yet implemented")
    }

    override fun enablePanelDepthTest(enabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun createInteractableComponent(
        executor: Executor,
        listener: InputEventListener,
    ): InteractableComponent {
        TODO("Not yet implemented")
    }

    override fun createAnchorPlacementForPlanes(
        planeTypeFilter: Set<@JvmSuppressWildcards PlaneType>,
        planeSemanticFilter: Set<@JvmSuppressWildcards PlaneSemantic>,
    ): AnchorPlacement {
        TODO("Not yet implemented")
    }

    override fun createMovableComponent(
        systemMovable: Boolean,
        scaleInZ: Boolean,
        userAnchorable: Boolean,
    ): MovableComponent {
        TODO("Not yet implemented")
    }

    override fun createResizableComponent(
        minimumSize: Dimensions,
        maximumSize: Dimensions,
    ): ResizableComponent {
        TODO("Not yet implemented")
    }

    override fun createPointerCaptureComponent(
        executor: Executor,
        stateListener: PointerCaptureComponent.StateListener,
        inputListener: InputEventListener,
    ): PointerCaptureComponent {
        TODO("Not yet implemented")
    }

    override fun createSpatialPointerComponent(): SpatialPointerComponent {
        TODO("Not yet implemented")
    }

    override fun addOnBoundaryConsentChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<Boolean>,
    ) {
        TODO("Not yet implemented")
    }

    override fun removeOnBoundaryConsentChangedListener(listener: Consumer<Boolean>) {
        TODO("Not yet implemented")
    }

    override fun createBoundsComponent(): BoundsComponent {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        super.destroy()
        mServiceClient.unbindService()
        mIsDestroyed = true
    }

    public companion object {
        public fun create(
            activity: Activity,
            serviceClient: ProjectedSceneCoreServiceClient,
            executor: ScheduledExecutorService,
        ): SceneRuntime {
            return ProjectedSceneRuntime(activity, serviceClient, executor)
        }
    }
}
