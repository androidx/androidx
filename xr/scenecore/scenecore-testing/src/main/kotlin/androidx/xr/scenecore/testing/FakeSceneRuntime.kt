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

package androidx.xr.scenecore.testing

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.ActivityPanelEntity
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.BoundsComponent
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.InteractableComponent
import androidx.xr.scenecore.runtime.LoggingEntity
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.PlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType
import androidx.xr.scenecore.runtime.PointerCaptureComponent
import androidx.xr.scenecore.runtime.RenderingEntityFactory
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import androidx.xr.scenecore.runtime.SpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialModeChangeListener
import androidx.xr.scenecore.runtime.SpatialPointerComponent
import androidx.xr.scenecore.runtime.SpatialVisibility
import androidx.xr.scenecore.runtime.SubspaceNodeEntity
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.SurfaceFeature
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Test-only implementation of [androidx.xr.scenecore.runtime.SceneRuntime].
 *
 * @param executor This used to input [executor] for tests.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSceneRuntime(public val executor: Executor? = null) :
    SceneRuntime, RenderingEntityFactory {

    /* Tracks the current state of the adapter according to where it is in its lifecycle. */
    public enum class State {
        CREATED,
        STARTED,
        PAUSED,
        DESTROYED,
    }

    private var _state: Enum<State> = State.CREATED

    /** The last [FakeMovableComponent] created or injected via [createMovableComponent]. */
    public var lastMovableComponent: FakeMovableComponent? = null
        private set

    /**
     * The current state of the adapter will transition based on the lifecycle of the adapter. It
     * starts off as [State.CREATED] and transitions to [State.STARTED] when startRenderer is
     * called. When stopRenderer is called, it transitions to [State.PAUSED]. When dispose is
     * called, it transitions to [State.DESTROYED].
     */
    public val state: Enum<State>
        get() = _state

    override var spatialCapabilities: SpatialCapabilities =
        SpatialCapabilities(ALL_SPATIAL_CAPABILITIES)
        private set(value) {
            field = value
            spatialCapabilitiesChangedMap.forEach { (consumer, executor) ->
                executor.execute { consumer.accept(value) }
            }
        }

    override val activitySpace: FakeActivitySpace = FakeActivitySpace()

    override val perceptionSpaceActivityPose: PerceptionSpaceScenePose =
        FakePerceptionSpaceScenePose()

    override val soundPoolExtensionsWrapper: SoundPoolExtensionsWrapper =
        FakeSoundPoolExtensionsWrapper()

    override val audioTrackExtensionsWrapper: FakeAudioTrackExtensionsWrapper =
        FakeAudioTrackExtensionsWrapper()

    override val mediaPlayerExtensionsWrapper: FakeMediaPlayerExtensionsWrapper =
        FakeMediaPlayerExtensionsWrapper()

    override val mainPanelEntity: PanelEntity = FakePanelEntity()

    override var keyEntity: Entity? = null

    override val spatialEnvironment: FakeSpatialEnvironment = FakeSpatialEnvironment()

    override var spatialModeChangeListener: SpatialModeChangeListener? =
        FakeSpatialModeChangeListener()

    override fun getScenePoseFromPerceptionPose(pose: Pose): ScenePose {
        return FakePerceptionSpaceScenePose()
    }

    public var deviceDpPerMeter: Float = DEFAULT_DP_PER_METER

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        dimensions: Dimensions,
        name: String,
        parent: Entity?,
    ): PanelEntity =
        FakePanelEntity(view, name).apply {
            dpPerMeter = deviceDpPerMeter
            size = dimensions
            this.parent = parent
            setPose(pose)
        }

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        pixelDimensions: PixelDimensions,
        name: String,
        parent: Entity?,
    ): PanelEntity =
        FakePanelEntity(view, name).apply {
            dpPerMeter = deviceDpPerMeter
            sizeInPixels = pixelDimensions
            this.parent = parent
            setPose(pose)
        }

    override fun createActivityPanelEntity(
        pose: Pose,
        windowBoundsPx: PixelDimensions,
        name: String,
        hostActivity: Activity,
        parent: Entity?,
    ): ActivityPanelEntity =
        FakeActivityPanelEntity(name).apply {
            dpPerMeter = deviceDpPerMeter
            sizeInPixels = windowBoundsPx
            this.parent = parent
            setPose(pose)
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createAnchorEntity(): AnchorEntity {
        return FakeAnchorEntity()
    }

    override fun createGltfEntity(
        feature: GltfFeature,
        pose: Pose,
        parentEntity: Entity?,
    ): FakeGltfEntity {
        if (executor == null) throw NullPointerException("Set executor before test")

        val gltfEntity = FakeGltfEntity(feature, executor)
        gltfEntity.setPose(pose)
        gltfEntity.parent = parentEntity

        return gltfEntity
    }

    override fun createSurfaceEntity(
        feature: SurfaceFeature,
        pose: Pose,
        parentEntity: Entity?,
    ): SurfaceEntity {
        val surfaceEntity = FakeSurfaceEntity()
        surfaceEntity.setPose(pose)
        surfaceEntity.parent = parentEntity

        return surfaceEntity
    }

    override fun createGroupEntity(pose: Pose, name: String, parent: Entity?): Entity {
        val entity = FakeEntity(name)
        entity.setPose(pose)
        entity.parent = parent

        return entity
    }

    override fun createLoggingEntity(pose: Pose): LoggingEntity =
        object : LoggingEntity, FakeEntity() {}

    /**
     * For test purposes only.
     *
     * A map tracking the listeners registered for spatial capability changes. The key is the
     * [Executor] on which the listener should be invoked, and the value is the [Consumer] listener
     * itself.
     *
     * This map is populated by calls to [addSpatialCapabilitiesChangedListener] and modified by
     * [removeSpatialCapabilitiesChangedListener]. Tests can inspect its contents to verify that the
     * correct listeners are registered with their intended executors.
     */
    public val spatialCapabilitiesChangedMap: Map<Consumer<SpatialCapabilities>, Executor>
        get() = _spatialCapabilitiesChangedMap

    private val _spatialCapabilitiesChangedMap:
        MutableMap<Consumer<SpatialCapabilities>, Executor> =
        mutableMapOf()

    override fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ) {
        _spatialCapabilitiesChangedMap[listener] = callbackExecutor
    }

    override fun removeSpatialCapabilitiesChangedListener(listener: Consumer<SpatialCapabilities>) {
        _spatialCapabilitiesChangedMap.remove(listener)
    }

    /**
     * For test purposes only.
     *
     * A map tracking the listener registered for spatial visibility changes. The key is the
     * [Executor] on which the listener should be invoked, and the value is the [Consumer] listener
     * itself.
     *
     * This map is populated by calls to [setSpatialVisibilityChangedListener] and cleared by
     * [clearSpatialVisibilityChangedListener]. Tests can inspect its contents to verify that the
     * correct listener is registered or that it has been successfully cleared.
     */
    public val spatialVisibilityChangedMap: Map<Consumer<SpatialVisibility>, Executor>
        get() = _spatialVisibilityChangedMap

    private val _spatialVisibilityChangedMap: MutableMap<Consumer<SpatialVisibility>, Executor> =
        mutableMapOf()

    override fun setSpatialVisibilityChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialVisibility>,
    ) {
        _spatialVisibilityChangedMap[listener] = callbackExecutor
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
     * This map is populated by calls to [addPerceivedResolutionChangedListener] and modified by
     * [removePerceivedResolutionChangedListener]. Tests can inspect its contents to verify that the
     * correct listeners are registered or that they have been successfully removed.
     */
    public val perceivedResolutionChangedMap: Map<Consumer<PixelDimensions>, Executor>
        get() = _perceivedResolutionChangedMap

    private val _perceivedResolutionChangedMap: MutableMap<Consumer<PixelDimensions>, Executor> =
        mutableMapOf()

    override fun addPerceivedResolutionChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<PixelDimensions>,
    ) {
        _perceivedResolutionChangedMap[listener] = callbackExecutor
    }

    override fun removePerceivedResolutionChangedListener(listener: Consumer<PixelDimensions>) {
        _perceivedResolutionChangedMap.remove(listener)
    }

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

    override fun requestFullSpaceMode() {
        spatialCapabilities = SpatialCapabilities(ALL_SPATIAL_CAPABILITIES)
        activitySpace.onBoundsChanged(
            Dimensions(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    override fun requestHomeSpaceMode() {
        spatialCapabilities = SpatialCapabilities(0)
        activitySpace.onBoundsChanged(Dimensions(1f, 1f, 1f))
    }

    override fun setFullSpaceMode(bundle: Bundle): Bundle = bundle

    override fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle = bundle

    /** This value is used to verify the result of [enablePanelDepthTest] in tests. */
    public var enabledPanelDepthTest: Boolean = false
        internal set

    override fun enablePanelDepthTest(enabled: Boolean) {
        enabledPanelDepthTest = enabled
    }

    override fun createInteractableComponent(
        executor: Executor,
        listener: InputEventListener,
    ): InteractableComponent {
        val interactableComponent = FakeInteractableComponent()
        interactableComponent.inputEventListenersMap[listener] = executor
        return interactableComponent
    }

    override fun createAnchorPlacementForPlanes(
        planeTypeFilter: Set<@JvmSuppressWildcards PlaneType>,
        planeSemanticFilter: Set<@JvmSuppressWildcards PlaneSemantic>,
    ): FakeAnchorPlacement = FakeAnchorPlacement(planeTypeFilter, planeSemanticFilter)

    override fun createMovableComponent(
        systemMovable: Boolean,
        scaleInZ: Boolean,
        userAnchorable: Boolean,
    ): FakeMovableComponent {
        val movableComponent = FakeMovableComponent()
        movableComponent.systemMovable = systemMovable
        movableComponent.scaleInZ = scaleInZ
        movableComponent.userAnchorable = userAnchorable
        lastMovableComponent = movableComponent
        return movableComponent
    }

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
        val pointerCaptureComponent = FakePointerCaptureComponent(executor, stateListener)
        pointerCaptureComponent.inputListener = inputListener
        return pointerCaptureComponent
    }

    override fun createSpatialPointerComponent(): SpatialPointerComponent =
        FakeSpatialPointerComponent()

    override fun createBoundsComponent(): BoundsComponent = FakeBoundsComponent()

    // Assuming the subspaceNodeHolder contains a valid FakeSubspaceNode and a valid FakeNode.
    public fun createSubspaceNodeEntity(node: FakeNode, size: Dimensions): SubspaceNodeEntity =
        FakeSubspaceNodeEntity()

    public companion object {
        internal const val DEFAULT_DP_PER_METER: Float = 1151.856f

        public const val ALL_SPATIAL_CAPABILITIES: Int =
            SpatialCapabilities.SPATIAL_CAPABILITY_UI or
                SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT or
                SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO or
                SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT or
                SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL or
                SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY
    }

    private var _isBoundaryConsentGranted = false
    override val isBoundaryConsentGranted: Boolean
        get() = _isBoundaryConsentGranted

    /**
     * For test purposes only.
     *
     * A map tracking the listeners registered for boundary consent changes. The key is the
     * [Consumer] listener ,and the value is the [Executor] on which the listener should be invoked.
     *
     * This map is populated by calls to [addOnBoundaryConsentChangedListener] and modified by
     * [removeOnBoundaryConsentChangedListener]. Tests can inspect its contents to verify that the
     * correct listeners are registered with their intended executors.
     */
    public val boundaryConsentChangedMap: Map<Consumer<Boolean>, Executor>
        get() = _boundaryConsentChangedMap

    private val _boundaryConsentChangedMap: MutableMap<Consumer<Boolean>, Executor> = mutableMapOf()

    override fun addOnBoundaryConsentChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<Boolean>,
    ) {
        _boundaryConsentChangedMap[listener] = callbackExecutor
    }

    override fun removeOnBoundaryConsentChangedListener(listener: Consumer<Boolean>) {
        _boundaryConsentChangedMap.remove(listener)
    }

    /**
     * For test purposes only.
     *
     * Changes the internal state of boundary consent and triggers registered listeners if the
     * effective consent state has changed.
     *
     * @param boundaryConsent The new value for boundary consent.
     */
    public fun onBoundaryConsentChanged(boundaryConsent: Boolean) {
        val oldBoundaryConsent = _isBoundaryConsentGranted
        val newBoundaryConsent = boundaryConsent
        _isBoundaryConsentGranted = newBoundaryConsent

        if (oldBoundaryConsent != newBoundaryConsent) {
            _boundaryConsentChangedMap.forEach { (listener, executor) ->
                executor.execute { listener.accept(newBoundaryConsent) }
            }
        }
    }
}
