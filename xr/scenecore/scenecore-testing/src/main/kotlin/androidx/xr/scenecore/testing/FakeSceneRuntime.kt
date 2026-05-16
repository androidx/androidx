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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.Trackable
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.ActivityPanelEntity
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.BoundsComponent
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.LoggingEntity
import androidx.xr.scenecore.runtime.MeshEntity
import androidx.xr.scenecore.runtime.MeshFeature
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.PlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.PointerCaptureComponent
import androidx.xr.scenecore.runtime.PositionalAudioComponent
import androidx.xr.scenecore.runtime.RenderingEntityFactory
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SoundEffectPool
import androidx.xr.scenecore.runtime.SoundEffectPoolComponent
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SoundFieldAudioComponent
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import androidx.xr.scenecore.runtime.SpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialModeChangeListener
import androidx.xr.scenecore.runtime.SpatialPointerComponent
import androidx.xr.scenecore.runtime.SpatialVisibility
import androidx.xr.scenecore.runtime.SubspaceNodeEntity
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.SurfaceFeature
import androidx.xr.scenecore.runtime.TrackableComponent
import androidx.xr.scenecore.testing.internal.FakeAnchorEntity as InternalFakeAnchorEntity
import androidx.xr.scenecore.testing.internal.FakeEntity as InternalFakeEntity
import androidx.xr.scenecore.testing.internal.FakeMeshEntity as InternalFakeMeshEntity
import androidx.xr.scenecore.testing.internal.FakePerceptionSpaceScenePose as InternalFakePerceptionSpaceScenePose
import androidx.xr.scenecore.testing.internal.FakePositionalAudioComponent as InternalFakePositionalAudioComponent
import androidx.xr.scenecore.testing.internal.FakeSceneRuntime as InternalFakeSceneRuntime
import androidx.xr.scenecore.testing.internal.FakeSoundEffectPool as InternalFakeSoundEffectPool
import androidx.xr.scenecore.testing.internal.FakeSoundEffectPoolComponent as InternalFakeSoundEffectPoolComponent
import androidx.xr.scenecore.testing.internal.FakeSoundPoolExtensionsWrapper as InternalFakeSoundPoolExtensionsWrapper
import androidx.xr.scenecore.testing.internal.FakeSurfaceEntity as InternalFakeSurfaceEntity
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Test-only implementation of [androidx.xr.scenecore.runtime.SceneRuntime].
 *
 * @param executor This used to input [executor] for tests.
 */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeSceneRuntime(public val executor: Executor? = null) :
    SceneRuntime, RenderingEntityFactory {

    internal var internalRuntime: InternalFakeSceneRuntime = InternalFakeSceneRuntime(executor)

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

    override var spatialCapabilities: SpatialCapabilities
        get() = internalRuntime.spatialCapabilities
        private set(value) {
            internalRuntime.spatialCapabilities = value
        }

    override val activitySpace: FakeActivitySpace = FakeActivitySpace(internalRuntime.activitySpace)

    override val perceptionSpaceActivityPose: PerceptionSpaceScenePose =
        FakePerceptionSpaceScenePose(
            internalRuntime.perceptionSpaceActivityPose as InternalFakePerceptionSpaceScenePose
        )

    override val soundPoolExtensionsWrapper: SoundPoolExtensionsWrapper =
        FakeSoundPoolExtensionsWrapper(
            internalRuntime.soundPoolExtensionsWrapper as InternalFakeSoundPoolExtensionsWrapper
        )

    override val audioTrackExtensionsWrapper: FakeAudioTrackExtensionsWrapper =
        FakeAudioTrackExtensionsWrapper(internalRuntime.audioTrackExtensionsWrapper)

    override val mediaPlayerExtensionsWrapper: FakeMediaPlayerExtensionsWrapper =
        FakeMediaPlayerExtensionsWrapper(internalRuntime.mediaPlayerExtensionsWrapper)

    override val mainPanelEntity: PanelEntity = FakePanelEntity()

    private var _keyEntity: Entity? = null

    override var keyEntity: Entity?
        get() = _keyEntity
        set(value) {
            _keyEntity = value

            when (value) {
                null -> {
                    internalRuntime.keyEntity = null
                }
                is FakeEntity -> {
                    internalRuntime.keyEntity = value.fakeInternal as InternalFakeEntity
                }

                else -> {
                    // When integrated with Compose, the keyEntity might be a Compose-specific
                    // representation of an Entity. In such cases, this FakeSceneRuntime does
                    // not need to do anything with it, as the Compose integration handles
                    // the key entity state separately. Only FakeEntity types, which are
                    // created by this FakeSceneRuntime, need to be mirrored to the internalRuntime.
                }
            }
        }

    override val spatialEnvironment: FakeSpatialEnvironment =
        FakeSpatialEnvironment(internalRuntime.spatialEnvironment)

    override var spatialModeChangeListener: SpatialModeChangeListener?
        get() = internalRuntime.spatialModeChangeListener
        set(value) {
            internalRuntime.spatialModeChangeListener = value
        }

    override fun getScenePoseFromPerceptionPose(pose: Pose): ScenePose {
        return FakePerceptionSpaceScenePose(
            internalRuntime.getScenePoseFromPerceptionPose(pose)
                as InternalFakePerceptionSpaceScenePose
        )
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
        return FakeAnchorEntity(internalRuntime.createAnchorEntity() as InternalFakeAnchorEntity)
    }

    override fun createGltfEntity(
        feature: GltfFeature,
        pose: Pose,
        parentEntity: Entity?,
    ): FakeGltfEntity {
        val nonNullExecutor = checkNotNull(executor) { "Set executor before test" }

        require(feature is FakeGltfFeature) {
            "The feature passed to FakeSceneRuntime must be an instance of FakeGltfFeature."
        }

        val gltfEntity = FakeGltfEntity(feature, nonNullExecutor)
        gltfEntity.setPose(pose)
        gltfEntity.parent = parentEntity

        internalRuntime.createGltfEntity(feature.fakeInternal, pose, parentEntity).let {
            gltfEntity.fakeInternal = it
        }

        return gltfEntity
    }

    override fun createSurfaceEntity(
        feature: SurfaceFeature,
        pose: Pose,
        parentEntity: Entity?,
    ): SurfaceEntity {
        val fakeFeature = feature as FakeSurfaceFeature

        val internalEntity =
            internalRuntime.createSurfaceEntity(fakeFeature.fakeInternal, pose, parentEntity)
                as InternalFakeSurfaceEntity

        val surfaceEntity = FakeSurfaceEntity(fakeFeature, internalEntity)
        surfaceEntity.setPose(pose)
        surfaceEntity.parent = parentEntity

        return surfaceEntity
    }

    override fun createMeshEntity(
        feature: MeshFeature,
        pose: Pose,
        parentEntity: Entity?,
    ): MeshEntity {
        val meshEntity = FakeMeshEntity(feature as FakeMeshFeature)
        meshEntity.setPose(pose)
        meshEntity.parent = parentEntity
        meshEntity.fakeInternal =
            internalRuntime.createMeshEntity(
                feature.fakeInternal,
                pose,
                (parentEntity as? FakeEntity)?.fakeInternal as? InternalFakeEntity,
            ) as InternalFakeMeshEntity

        return meshEntity
    }

    override fun createEntity(pose: Pose, name: String?, parent: Entity?): Entity {
        val entityName = name ?: ""
        val entity =
            FakeEntity(
                entityName,
                internalRuntime.createEntity(pose, name, parent) as InternalFakeEntity,
            )
        entity.setPose(pose)
        entity.parent = parent

        return entity
    }

    @Deprecated("Use createEntity instead.")
    override fun createGroupEntity(pose: Pose, name: String, parent: Entity?): Entity {
        return createEntity(pose, name, parent)
    }

    override fun createLoggingEntity(pose: Pose): LoggingEntity =
        object : LoggingEntity, FakeEntity() {}

    override fun createSubspaceNodeEntity(
        nodeHolder: NodeHolder<*>,
        size: Dimensions,
    ): SubspaceNodeEntity = FakeSubspaceNodeEntity(size)

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
        get() = internalRuntime.spatialCapabilitiesChangedMap

    override fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ) {
        internalRuntime.addSpatialCapabilitiesChangedListener(callbackExecutor, listener)
    }

    override fun removeSpatialCapabilitiesChangedListener(listener: Consumer<SpatialCapabilities>) {
        internalRuntime.removeSpatialCapabilitiesChangedListener(listener)
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
        get() = internalRuntime.spatialVisibilityChangedMap

    override fun setSpatialVisibilityChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialVisibility>,
    ) {
        internalRuntime.setSpatialVisibilityChangedListener(callbackExecutor, listener)
    }

    override fun clearSpatialVisibilityChangedListener() {
        internalRuntime.clearSpatialVisibilityChangedListener()
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
        get() = internalRuntime.perceivedResolutionChangedMap

    override fun addPerceivedResolutionChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<PixelDimensions>,
    ) {
        internalRuntime.addPerceivedResolutionChangedListener(callbackExecutor, listener)
    }

    override fun removePerceivedResolutionChangedListener(listener: Consumer<PixelDimensions>) {
        internalRuntime.removePerceivedResolutionChangedListener(listener)
    }

    /**
     * For test purposes only.
     *
     * Stores the [Activity] that was last provided to the [setPreferredAspectRatio] method. Tests
     * can inspect this property to verify the correct activity was used.
     */
    public var lastSetPreferredAspectRatioActivity: Activity?
        get() = internalRuntime.lastSetPreferredAspectRatioActivity
        set(value) {
            internalRuntime.lastSetPreferredAspectRatioActivity = value
        }

    /**
     * For test purposes only.
     *
     * Stores the ratio that was last provided to the [setPreferredAspectRatio] method. Tests can
     * inspect this property to verify the correct ratio was set.
     */
    public var lastSetPreferredAspectRatioRatio: Float
        get() = internalRuntime.lastSetPreferredAspectRatio
        set(value) {
            internalRuntime.lastSetPreferredAspectRatio = value
        }

    override fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float) {
        internalRuntime.setPreferredAspectRatio(activity, preferredRatio)
    }

    override fun requestFullSpaceMode() {
        internalRuntime.requestFullSpaceMode()
        // FakeActivitySpace has not bypassed data to internal, use current.
        activitySpace.onBoundsChanged(
            Dimensions(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    override fun requestHomeSpaceMode() {
        internalRuntime.requestHomeSpaceMode()
        // FakeActivitySpace has not bypassed data to internal, use current.
        activitySpace.onBoundsChanged(Dimensions(1f, 1f, 1f))
    }

    override fun setFullSpaceMode(bundle: Bundle): Bundle = bundle

    override fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle = bundle

    /** This value is used to verify the result of [enablePanelDepthTest] in tests. */
    public var enabledPanelDepthTest: Boolean
        get() = internalRuntime.enabledPanelDepthTest
        internal set(value) {
            internalRuntime.enabledPanelDepthTest = value
        }

    override fun enablePanelDepthTest(enabled: Boolean) {
        internalRuntime.enablePanelDepthTest(enabled)
    }

    override fun createInteractableComponent(
        executor: Executor,
        listener: InputEventListener,
    ): FakeInteractableComponent {
        return FakeInteractableComponent(
            internalRuntime.createInteractableComponent(executor, listener)
        )
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

    override fun createTrackableComponent(
        lifecycleOwner: LifecycleOwner,
        trackable: Trackable<Trackable.State>,
        poseExtractor: ((Any?) -> Pose?),
    ): TrackableComponent {
        val mappedFlow: Flow<Pose> = trackable.state.mapNotNull { state -> poseExtractor(state) }

        return androidx.xr.scenecore.testing.internal.FakeTrackableComponent(
            coroutineScope = lifecycleOwner.lifecycleScope,
            poseFlow = mappedFlow,
        )
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
        internal const val DEFAULT_DP_PER_METER: Float = 2000f

        public const val ALL_SPATIAL_CAPABILITIES: Int =
            SpatialCapabilities.SPATIAL_CAPABILITY_UI or
                SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT or
                SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO or
                SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT or
                SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL or
                SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY
    }

    override val isBoundaryConsentGranted: Boolean
        get() = internalRuntime.isBoundaryConsentGranted

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
        get() = internalRuntime.boundaryConsentChangedMap

    override fun addOnBoundaryConsentChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<Boolean>,
    ) {
        internalRuntime.addOnBoundaryConsentChangedListener(callbackExecutor, listener)
    }

    override fun removeOnBoundaryConsentChangedListener(listener: Consumer<Boolean>) {
        internalRuntime.removeOnBoundaryConsentChangedListener(listener)
    }

    override fun createPositionalAudioComponent(
        context: Context,
        params: PointSourceParams,
    ): PositionalAudioComponent {
        return FakePositionalAudioComponent(
            context,
            params,
            internalRuntime.createPositionalAudioComponent(context, params)
                as InternalFakePositionalAudioComponent,
        )
    }

    override fun createSoundFieldAudioComponent(
        context: Context,
        rtSoundFieldAttributes: SoundFieldAttributes,
    ): SoundFieldAudioComponent {
        return FakeSoundFieldAudioComponent(context)
    }

    override fun createSoundEffectPool(maxStreams: Int): SoundEffectPool =
        FakeSoundEffectPool(
            internalRuntime.createSoundEffectPool(maxStreams) as InternalFakeSoundEffectPool
        )

    override fun createSoundEffectPoolComponent(
        soundEffectPool: SoundEffectPool
    ): SoundEffectPoolComponent {
        val fakeSoundEffectPoolComponent =
            FakeSoundEffectPoolComponent(
                internalRuntime.createSoundEffectPoolComponent(soundEffectPool)
                    as InternalFakeSoundEffectPoolComponent
            )

        return fakeSoundEffectPoolComponent
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
        internalRuntime.onBoundaryConsentChanged(boundaryConsent)
    }

    override val virtualPixelDensity: Float = 2000f

    override fun destroy() {
        internalRuntime.destroy()
    }
}
