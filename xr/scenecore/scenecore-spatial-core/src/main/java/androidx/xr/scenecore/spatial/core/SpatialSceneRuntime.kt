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

@file:Suppress("BanConcurrentHashMap", "BanSynchronizedMethods")

package androidx.xr.scenecore.spatial.core

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.database.ContentObserver
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.ActivityPanelEntity
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.AnchorPlacement
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.BoundsComponent
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
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
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.PointerCaptureComponent
import androidx.xr.scenecore.runtime.PositionalAudioComponent
import androidx.xr.scenecore.runtime.RenderingEntityFactory
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
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.SurfaceFeature
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.spatial.core.RuntimeUtils.convertPerceivedResolution
import androidx.xr.scenecore.spatial.core.RuntimeUtils.convertSpatialCapabilities
import androidx.xr.scenecore.spatial.core.RuntimeUtils.convertSpatialVisibility
import androidx.xr.scenecore.spatial.core.RuntimeUtils.getMatrix
import androidx.xr.scenecore.spatial.core.RuntimeUtils.getPositionFromTransform
import androidx.xr.scenecore.spatial.core.RuntimeUtils.getRotationFromTransform
import com.android.extensions.xr.XrExtensionResult
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.space.ActivityPanelLaunchParameters
import com.android.extensions.xr.space.SpatialState
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Implementation of [SceneRuntime] for devices that support the
 * [androidx.xr.runtime.interfaces.Feature.SPATIAL] system feature.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SpatialSceneRuntime
private constructor(
    activity: Activity,
    private val scheduledExecutorService: ScheduledExecutorService,
    private val xrExtensions: XrExtensions,
    private val entityManager: EntityManager,
    @get:VisibleForTesting internal val sceneRootNode: Node,
    @get:VisibleForTesting internal val taskWindowLeashNode: Node,
) : SceneRuntime, RenderingEntityFactory {
    private val spatialEnvironmentImpl: SpatialEnvironmentImpl
    private val spatialCapabilitiesChangedListeners =
        ConcurrentHashMap<Consumer<SpatialCapabilities>, Executor>()
    private val perceivedResolutionChangedListeners =
        ConcurrentHashMap<Consumer<PixelDimensions>, Executor>()
    private val boundaryConsentListeners = ConcurrentHashMap<Consumer<Boolean>, Executor>()
    // TODO b/373481538: remove lazy initialization once XR Extensions bug is fixed. This will allow
    // us to remove the lazySpatialStateProvider instance and pass the spatialState directly.
    private val spatialState = AtomicReference<SpatialState?>(null)
    // Returns the currently-known spatial state, or fetches it from the extensions if it has never
    // been set. The spatial state is kept updated in the SpatialStateCallback.
    private val lazySpatialStateProvider: Supplier<SpatialState>
    /** Returns the PerceptionSpaceScenePose for the Session. */
    private val perceptionSpaceScenePose: PerceptionSpaceScenePoseImpl
    private val isBoundaryConsentGrantedCache: AtomicBoolean
    private val spatialApiVersion: Int
    private var activity: Activity?
    private var isDestroyed = false
    private var spatialVisibilityHandler: Pair<Executor, Consumer<SpatialVisibility>>? = null
    private var boundaryConsentObserver: ContentObserver? = null
    @VisibleForTesting internal var isExtensionVisibilityStateCallbackRegistered: Boolean = false
    @VisibleForTesting internal var keyEntityTransformCloseable: Closeable? = null
    override val activitySpace: ActivitySpaceImpl
    override val mainPanelEntity: PanelEntity
        get() {
            xrExtensions.createNodeTransaction().use { transaction ->
                transaction.setVisibility(taskWindowLeashNode, true).apply()
            }
            return field
        }

    @VisibleForTesting
    override val soundPoolExtensionsWrapper: SoundPoolExtensionsWrapper =
        SoundPoolExtensionsWrapperImpl(xrExtensions.xrSpatialAudioExtensions.soundPoolExtensions)
    @VisibleForTesting
    override val audioTrackExtensionsWrapper: AudioTrackExtensionsWrapper =
        AudioTrackExtensionsWrapperImpl(xrExtensions.xrSpatialAudioExtensions.audioTrackExtensions)
    @VisibleForTesting
    override val mediaPlayerExtensionsWrapper: MediaPlayerExtensionsWrapper =
        MediaPlayerExtensionsWrapperImpl(
            xrExtensions.xrSpatialAudioExtensions.mediaPlayerExtensions
        )
    override val spatialCapabilities: SpatialCapabilities
        get() = convertSpatialCapabilities(lazySpatialStateProvider.get().spatialCapabilities)

    override val isBoundaryConsentGranted: Boolean
        get() = isBoundaryConsentGrantedCache.get()

    override val perceptionSpaceActivityPose: PerceptionSpaceScenePose
        get() = perceptionSpaceScenePose

    override val spatialEnvironment: SpatialEnvironment
        get() = spatialEnvironmentImpl

    override var spatialModeChangeListener: SpatialModeChangeListener? = null
        set(value) {
            field = value
            activitySpace.setSpatialModeChangeListener(value)
        }

    override var keyEntity: Entity? = null
        set(value) {
            if (field == value) {
                return
            }
            clearKeyEntitySubscription(true)
            field = value

            // If the new entity is valid, set up a new subscription.
            if (value is AndroidXrEntity) {
                setupKeyEntitySubscription(value)
            }
        }

    init {
        this.activity = activity

        lazySpatialStateProvider =
            Supplier<SpatialState> {
                spatialState.updateAndGet { oldState ->
                    oldState ?: xrExtensions.getSpatialState(activity)
                }!!
            }
        setSpatialStateCallback()

        spatialEnvironmentImpl =
            SpatialEnvironmentImpl(activity, xrExtensions, sceneRootNode, lazySpatialStateProvider)

        activitySpace =
            ActivitySpaceImpl(
                sceneRootNode,
                activity,
                xrExtensions,
                entityManager,
                lazySpatialStateProvider,
                scheduledExecutorService,
            )
        entityManager.addSystemSpaceActivityPose(activitySpace)
        perceptionSpaceScenePose = PerceptionSpaceScenePoseImpl(activitySpace)
        entityManager.addSystemSpaceActivityPose(perceptionSpaceScenePose)
        mainPanelEntity =
            MainPanelEntityImpl(
                activity,
                taskWindowLeashNode,
                xrExtensions,
                entityManager,
                scheduledExecutorService,
            )
        mainPanelEntity.parent = activitySpace
        // Initialize the boundary consent cache and register the listener.
        isBoundaryConsentGrantedCache = AtomicBoolean(calculateBoundaryConsentState())
        registerBoundaryConsentStateListener()
        spatialApiVersion = SpatialCoreApiVersionProvider().spatialApiVersion
    }

    override fun destroy() {
        if (isDestroyed) {
            return
        }
        spatialEnvironmentImpl.dispose()
        clearKeyEntitySubscription(false)
        spatialModeChangeListener = null
        xrExtensions.clearSpatialStateCallback(activity)

        unregisterBoundaryConsentStateListener()
        boundaryConsentListeners.clear()

        clearSpatialVisibilityChangedListener()
        perceivedResolutionChangedListeners.clear()
        // This will trigger clearing the callback from XrExtensions if it was registered
        updateExtensionsVisibilityCallback()

        // TODO: b/376934871 - Check async results.
        xrExtensions.detachSpatialScene(activity, { it.run() }) { _: XrExtensionResult -> }
        activity = null
        entityManager.getAllEntities().forEach(Entity::dispose)
        entityManager.clear()
        isDestroyed = true
    }

    override fun getScenePoseFromPerceptionPose(pose: Pose): ScenePose {
        return OpenXrScenePose(activitySpace, pose)
    }

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        dimensions: Dimensions,
        name: String,
        parent: Entity?,
    ): PanelEntity {
        val node = xrExtensions.createNode()
        val panelEntity: PanelEntity =
            PanelEntityImpl(
                context,
                node,
                view,
                xrExtensions,
                entityManager,
                dimensions,
                name,
                scheduledExecutorService,
            )
        panelEntity.parent = parent
        panelEntity.setPose(pose, Space.PARENT)
        return panelEntity
    }

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        pixelDimensions: PixelDimensions,
        name: String,
        parent: Entity?,
    ): PanelEntity {
        val node = xrExtensions.createNode()
        val panelEntity: PanelEntity =
            PanelEntityImpl(
                context,
                node,
                view,
                xrExtensions,
                entityManager,
                pixelDimensions,
                name,
                scheduledExecutorService,
            )
        panelEntity.parent = parent
        panelEntity.setPose(pose, Space.PARENT)
        return panelEntity
    }

    override fun createActivityPanelEntity(
        pose: Pose,
        windowBoundsPx: PixelDimensions,
        name: String,
        hostActivity: Activity,
        parent: Entity?,
    ): ActivityPanelEntity {
        // TODO(b/352630140): Move this into a static factory method of ActivityPanelEntityImpl.

        val windowBoundsRect = Rect(0, 0, windowBoundsPx.width, windowBoundsPx.height)
        val activityPanel =
            xrExtensions.createActivityPanel(
                hostActivity,
                ActivityPanelLaunchParameters(windowBoundsRect),
            )

        activityPanel.setWindowBounds(windowBoundsRect)
        val activityPanelEntity =
            ActivityPanelEntityImpl(
                hostActivity,
                activityPanel.node,
                name,
                xrExtensions,
                entityManager,
                activityPanel,
                windowBoundsPx,
                scheduledExecutorService,
            )
        activityPanelEntity.parent = parent
        activityPanelEntity.setPose(pose, Space.PARENT)
        return activityPanelEntity
    }

    override fun createAnchorEntity(): AnchorEntity {
        val node = xrExtensions.createNode()
        return AnchorEntityImpl.create(
            checkNotNull(activity),
            node,
            activitySpace,
            xrExtensions,
            entityManager,
            scheduledExecutorService,
        )
    }

    override fun createGltfEntity(
        feature: GltfFeature,
        pose: Pose,
        parentEntity: Entity?,
    ): GltfEntity {
        val entity: GltfEntity =
            GltfEntityImpl(
                checkNotNull(activity),
                feature,
                parentEntity,
                xrExtensions,
                entityManager,
                scheduledExecutorService,
            )
        entity.setPose(pose, Space.PARENT)
        return entity
    }

    override fun createSurfaceEntity(
        feature: SurfaceFeature,
        pose: Pose,
        parentEntity: Entity?,
    ): SurfaceEntity {
        val entity: SurfaceEntity =
            SurfaceEntityImpl(
                checkNotNull(activity),
                feature,
                parentEntity,
                xrExtensions,
                entityManager,
                scheduledExecutorService,
            )
        entity.setPose(pose, Space.PARENT)
        return entity
    }

    public fun createSubspaceNodeEntity(node: Node, size: Dimensions): SubspaceNodeEntity {
        val entity: SubspaceNodeEntity =
            SubspaceNodeEntityImpl(
                checkNotNull(activity),
                xrExtensions,
                node,
                entityManager,
                scheduledExecutorService,
            )
        entity.size = size
        return entity
    }

    override fun createEntity(pose: Pose, name: String?, parent: Entity?): Entity {
        val node = xrExtensions.createNode()
        name?.let {
            xrExtensions.createNodeTransaction().use { transaction ->
                transaction.setName(node, name).apply()
            }
        }
        // This entity is used to back SceneCore's Entity.
        val entity: Entity =
            object :
                AndroidXrEntity(
                    activity,
                    node,
                    xrExtensions,
                    entityManager,
                    scheduledExecutorService,
                ) {}
        entity.parent = parent
        entity.setPose(pose, Space.PARENT)
        return entity
    }

    @Deprecated("Use createEntity instead.")
    override fun createGroupEntity(pose: Pose, name: String, parent: Entity?): Entity {
        return createEntity(pose, name, parent)
    }

    override fun createLoggingEntity(pose: Pose): LoggingEntity {
        val entity = LoggingEntityImpl(checkNotNull(activity))
        entity.setPose(pose, Space.PARENT)
        return entity
    }

    // Note that this is called on the Activity's UI thread so we should be careful to not block it.
    // It is synchronized because we assume this.spatialState cannot be updated elsewhere during the
    // execution of this method.
    @VisibleForTesting
    @Synchronized
    public fun onSpatialStateChanged(newSpatialState: SpatialState) {
        val previousSpatialState = spatialState.getAndSet(newSpatialState)
        val spatialCapabilitiesChanged =
            previousSpatialState == null ||
                (newSpatialState.spatialCapabilities != previousSpatialState.spatialCapabilities)

        val hasBoundsChanged =
            previousSpatialState == null || newSpatialState.bounds != previousSpatialState.bounds

        val changedSpatialStates = spatialEnvironmentImpl.setSpatialState(newSpatialState)
        val environmentVisibilityChanged =
            changedSpatialStates.contains(
                SpatialEnvironmentImpl.ChangedSpatialStates.ENVIRONMENT_CHANGED
            )
        val passthroughVisibilityChanged =
            changedSpatialStates.contains(
                SpatialEnvironmentImpl.ChangedSpatialStates.PASSTHROUGH_CHANGED
            )

        // Fire the state change events only after all the states have been updated.
        if (environmentVisibilityChanged) {
            spatialEnvironmentImpl.fireOnSpatialEnvironmentChangedEvent()
        }
        if (passthroughVisibilityChanged) {
            spatialEnvironmentImpl.firePassthroughOpacityChangedEvent()
        }

        // Get the scene parent transform and update the activity space.
        if (newSpatialState.sceneParentTransform != null) {
            activitySpace.handleOriginUpdate(getMatrix(newSpatialState.sceneParentTransform))
        }

        if (spatialCapabilitiesChanged) {
            val spatialCapabilities =
                convertSpatialCapabilities(newSpatialState.spatialCapabilities)

            spatialCapabilitiesChangedListeners.forEach { (listener, executor) ->
                executor.execute { listener.accept(spatialCapabilities) }
            }
        }

        if (hasBoundsChanged) {
            activitySpace.onBoundsChanged(newSpatialState.bounds)
        }
    }

    private fun setSpatialStateCallback() {
        val mainHandler = Handler(Looper.getMainLooper())
        xrExtensions.setSpatialStateCallback(activity, mainHandler::post) { newSpatialState ->
            this.onSpatialStateChanged(newSpatialState)
        }
    }

    override fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ) {
        spatialCapabilitiesChangedListeners[listener] = callbackExecutor
    }

    override fun removeSpatialCapabilitiesChangedListener(listener: Consumer<SpatialCapabilities>) {
        spatialCapabilitiesChangedListeners.remove(listener)
    }

    override fun setSpatialVisibilityChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialVisibility>,
    ) {
        spatialVisibilityHandler = callbackExecutor to listener
        updateExtensionsVisibilityCallback()
    }

    override fun clearSpatialVisibilityChangedListener() {
        spatialVisibilityHandler = null
        updateExtensionsVisibilityCallback()
    }

    override fun addPerceivedResolutionChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<PixelDimensions>,
    ) {
        perceivedResolutionChangedListeners[listener] = callbackExecutor
        updateExtensionsVisibilityCallback()
    }

    override fun removePerceivedResolutionChangedListener(listener: Consumer<PixelDimensions>) {
        perceivedResolutionChangedListeners.remove(listener)
        updateExtensionsVisibilityCallback()
    }

    @Synchronized
    private fun updateExtensionsVisibilityCallback() {
        val shouldHaveCallback =
            spatialVisibilityHandler != null || perceivedResolutionChangedListeners.isNotEmpty()

        if (shouldHaveCallback && !isExtensionVisibilityStateCallbackRegistered) {
            // Register the combined callback
            try {
                xrExtensions.setVisibilityStateCallback(activity, scheduledExecutorService) {
                    visibilityStateEvent ->
                    // Dispatch to SpatialVisibility listener
                    spatialVisibilityHandler?.let { (executor, listener) ->
                        visibilityStateEvent?.let { event ->
                            val jxrSpatialVisibility = convertSpatialVisibility(event.visibility)
                            executor.execute { listener.accept(jxrSpatialVisibility) }
                        }
                    }

                    // Dispatch to PerceivedResolution listeners
                    if (perceivedResolutionChangedListeners.isNotEmpty()) {
                        visibilityStateEvent?.let { event ->
                            val jxrPerceivedResolution =
                                convertPerceivedResolution(event.perceivedResolution)

                            perceivedResolutionChangedListeners.forEach { (listener, executor) ->
                                executor.execute { listener.accept(jxrPerceivedResolution) }
                            }
                        }
                    }
                }
                isExtensionVisibilityStateCallbackRegistered = true
            } catch (e: RuntimeException) {
                throw RuntimeException(
                    "Could not set combined VisibilityStateCallback: " + e.message
                )
            }
        } else if (!shouldHaveCallback && isExtensionVisibilityStateCallbackRegistered) {
            // Clear the combined callback
            try {
                xrExtensions.clearVisibilityStateCallback(activity)
                isExtensionVisibilityStateCallbackRegistered = false
            } catch (e: RuntimeException) {
                throw RuntimeException("Could not clear VisibilityStateCallback: " + e.message)
            }
        }
    }

    override fun requestFullSpaceMode() {
        // TODO: b/376934871 - Check async results.
        xrExtensions.requestFullSpaceMode(
            activity,
            /* requestEnter= */ true,
            { it.run() },
            { _: XrExtensionResult -> },
        )
    }

    override fun requestHomeSpaceMode() {
        // TODO: b/376934871 - Check async results.
        xrExtensions.requestFullSpaceMode(
            activity,
            /* requestEnter= */ false,
            { it.run() },
            { _: XrExtensionResult -> },
        )
    }

    override fun setFullSpaceMode(bundle: Bundle): Bundle {
        return xrExtensions.setFullSpaceStartMode(bundle)
    }

    override fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle {
        return xrExtensions.setFullSpaceStartModeWithEnvironmentInherited(bundle)
    }

    override fun enablePanelDepthTest(enabled: Boolean) {
        xrExtensions.enablePanelDepthTest(activity, enabled)
    }

    override fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float) {
        // TODO: b/376934871 - Check async results.
        xrExtensions.setPreferredAspectRatio(
            activity,
            preferredRatio,
            { it.run() },
            { _: XrExtensionResult -> },
        )
    }

    override fun createInteractableComponent(
        executor: Executor,
        listener: InputEventListener,
    ): InteractableComponent {
        return InteractableComponentImpl(executor, listener)
    }

    override fun createAnchorPlacementForPlanes(
        planeTypeFilter: Set<@JvmSuppressWildcards PlaneType>,
        planeSemanticFilter: Set<@JvmSuppressWildcards PlaneSemantic>,
    ): AnchorPlacement {
        val anchorPlacement = AnchorPlacementImpl()
        anchorPlacement.planeTypeFilter.addAll(planeTypeFilter)
        anchorPlacement.planeSemanticFilter.addAll(planeSemanticFilter)
        return anchorPlacement
    }

    override fun createMovableComponent(
        systemMovable: Boolean,
        scaleInZ: Boolean,
        userAnchorable: Boolean,
    ): MovableComponent {
        return MovableComponentImpl(
            systemMovable,
            scaleInZ,
            userAnchorable,
            activitySpace,
            EntityShadowRendererImpl(
                activitySpace,
                perceptionSpaceScenePose,
                checkNotNull(activity),
                xrExtensions,
            ),
            scheduledExecutorService,
        )
    }

    override fun createResizableComponent(
        minimumSize: Dimensions,
        maximumSize: Dimensions,
    ): ResizableComponent {
        return ResizableComponentImpl(
            scheduledExecutorService,
            xrExtensions,
            minimumSize,
            maximumSize,
        )
    }

    // Suppress warnings for factory function
    @SuppressLint("ExecutorRegistration")
    override fun createPointerCaptureComponent(
        executor: Executor,
        stateListener: PointerCaptureComponent.StateListener,
        inputListener: InputEventListener,
    ): PointerCaptureComponent {
        return PointerCaptureComponentImpl(executor, stateListener, inputListener)
    }

    override fun createSpatialPointerComponent(): SpatialPointerComponent {
        return SpatialPointerComponentImpl(xrExtensions)
    }

    /** Calculates the current boundary consent state directly from system settings. */
    private fun calculateBoundaryConsentState(): Boolean {
        checkNotNull(activity) { "Cannot calculate boundary consent on a destroyed runtime." }
        // TODO: b/464401298 - Implement boundary consent logic for Spatial API >= 2
        val resolver = checkNotNull(activity).contentResolver
        val isExplicitBoundaryConsentGranted =
            (Settings.Secure.getInt(resolver, GUARDIAN_CONSENT_GRANTED, 0) == 1)
        val isBoundaryEnabledInDeveloperOptions =
            (Settings.System.getInt(resolver, TOGGLE_GUARDIAN, 1) == 1)
        return (!isBoundaryEnabledInDeveloperOptions || isExplicitBoundaryConsentGranted)
    }

    private fun registerBoundaryConsentStateListener() {
        // TODO: b/464401298 - Implement boundary consent logic for Spatial API >= 2
        checkNotNull(activity) { "Cannot register listener on a destroyed runtime." }
        if (boundaryConsentObserver != null) {
            return // Already registered.
        }
        val isExplicitBoundaryConsentGrantedUri =
            Settings.Secure.getUriFor(GUARDIAN_CONSENT_GRANTED)
        val isBoundaryEnabledInDeveloperOptionsUri = Settings.System.getUriFor(TOGGLE_GUARDIAN)
        // Registers the ContentObserver to listen for changes in boundary settings.
        boundaryConsentObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    scheduledExecutorService.execute {
                        // Recalculate the current state
                        val newGrantedState = calculateBoundaryConsentState()

                        // Only update cache and notify listeners
                        // if the state has actually changed
                        if (
                            isBoundaryConsentGrantedCache.compareAndSet(
                                !newGrantedState,
                                newGrantedState,
                            )
                        ) {
                            boundaryConsentListeners.forEach { (consumer, anExecutor) ->
                                anExecutor.execute { consumer.accept(newGrantedState) }
                            }
                        }
                    }
                }
            }

        val resolver = checkNotNull(activity).contentResolver
        resolver.registerContentObserver(
            isExplicitBoundaryConsentGrantedUri,
            /* notifyForDescendants= */ false,
            boundaryConsentObserver!!,
        )
        resolver.registerContentObserver(
            isBoundaryEnabledInDeveloperOptionsUri,
            /* notifyForDescendants= */ false,
            boundaryConsentObserver!!,
        )
    }

    private fun unregisterBoundaryConsentStateListener() {
        // TODO: b/464401298 - Implement boundary consent logic for Spatial API >= 2
        if (boundaryConsentObserver != null && activity != null) {
            checkNotNull(activity)
                .contentResolver
                .unregisterContentObserver(boundaryConsentObserver!!)
            boundaryConsentObserver = null
        }
    }

    override fun addOnBoundaryConsentChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<Boolean>,
    ) {
        boundaryConsentListeners[listener] = callbackExecutor
    }

    override fun removeOnBoundaryConsentChangedListener(listener: Consumer<Boolean>) {
        boundaryConsentListeners.remove(listener)
    }

    override fun createBoundsComponent(): BoundsComponent {
        return BoundsComponentImpl()
    }

    /** Clears any existing subscription for the current key entity. */
    private fun clearKeyEntitySubscription(throwException: Boolean) {
        if (keyEntityTransformCloseable == null) {
            return
        }
        if (spatialApiVersion >= 2) {
            try {
                keyEntityTransformCloseable!!.close()
                xrExtensions.underlyingObject.clearSpatialContinuityHint(activity)
            } catch (e: IOException) {
                if (throwException) {
                    // Re-throw as an unchecked exception but include the original cause.
                    throw RuntimeException(
                        "Could not close the key entity's transform subscription.",
                        e,
                    )
                }
            } finally {
                // Ensure the reference is cleared even if closing fails.
                keyEntityTransformCloseable = null
            }
        }
    }

    /** Creates a new subscription to the transform of the given key entity. */
    private fun setupKeyEntitySubscription(entity: AndroidXrEntity) {
        if (spatialApiVersion >= 2) {
            keyEntityTransformCloseable =
                entity.getNode().subscribeToTransform(scheduledExecutorService) { nodeTransform ->
                    val transform = getMatrix(nodeTransform!!.transform)
                    xrExtensions.underlyingObject.setSpatialContinuityHint(
                        activity,
                        getPositionFromTransform(transform),
                        getRotationFromTransform(transform),
                    )
                }
        }
    }

    override fun createPositionalAudioComponent(
        context: Context,
        params: PointSourceParams,
    ): PositionalAudioComponent =
        PositionalAudioComponentImpl(context, audioTrackExtensionsWrapper, params)

    override fun createSoundFieldAudioComponent(
        context: Context,
        rtSoundFieldAttributes: SoundFieldAttributes,
    ): SoundFieldAudioComponent =
        SoundFieldAudioComponentImpl(context, audioTrackExtensionsWrapper, rtSoundFieldAttributes)

    override fun createSoundEffectPool(maxStreams: Int): SoundEffectPool =
        SoundEffectPoolImpl(maxStreams, soundPoolExtensionsWrapper, soundEffectPlayer = null)

    override fun createSoundEffectPoolComponent(
        soundEffectPool: SoundEffectPool
    ): SoundEffectPoolComponent {
        check(soundEffectPool is SoundEffectPoolImpl) {
            "SoundEffectPool must be an instance of SoundEffectPoolImpl created from the same runtime."
        }
        return SoundEffectPoolComponentImpl(soundEffectPool)
    }

    public companion object {
        private const val GUARDIAN_CONSENT_GRANTED = "guardian_consent_granted"
        private const val TOGGLE_GUARDIAN = "toggle_guardian"

        @JvmStatic
        public fun create(
            activity: Activity,
            executor: ScheduledExecutorService,
            sceneRootNode: Node,
            taskWindowLeashNode: Node,
        ): SpatialSceneRuntime {
            return create(
                activity,
                executor,
                extensions = requireNotNull(getXrExtensions()),
                EntityManager(),
                sceneRootNode,
                taskWindowLeashNode,
            )
        }

        @JvmStatic
        @JvmOverloads
        public fun create(
            activity: Activity,
            executor: ScheduledExecutorService,
            extensions: XrExtensions,
            entityManager: EntityManager,
            sceneRootNode: Node = extensions.createNode(),
            taskWindowLeashNode: Node = extensions.createNode(),
        ): SpatialSceneRuntime {
            // TODO: b/376934871 - Check async results.
            extensions.attachSpatialScene(activity, sceneRootNode, taskWindowLeashNode, executor) {
                _: XrExtensionResult ->
            }
            extensions.createNodeTransaction().use { transaction ->
                transaction
                    .setName(sceneRootNode, "SpatialSceneAndActivitySpaceRootNode")
                    .setParent(taskWindowLeashNode, sceneRootNode)
                    .setName(taskWindowLeashNode, "MainPanelAndTaskWindowLeashNode")
                    .apply()
            }
            return SpatialSceneRuntime(
                activity,
                executor,
                extensions,
                entityManager,
                sceneRootNode,
                taskWindowLeashNode,
            )
        }

        /** Create a new @c SpatialSceneRuntime. */
        @JvmStatic
        public fun create(
            activity: Activity,
            executor: ScheduledExecutorService,
        ): SpatialSceneRuntime {
            return create(activity, executor, requireNotNull(getXrExtensions()), EntityManager())
        }
    }
}
