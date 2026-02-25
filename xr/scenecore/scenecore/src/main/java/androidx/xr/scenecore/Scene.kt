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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.SessionConnector
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Entity as RtEntity
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialModeChangeListener as RtSpatialModeChangeListener
import androidx.xr.scenecore.runtime.SpatialVisibility as RtSpatialVisibility
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Scene is the primary interface to SceneCore functionality for the application. Each spatialized
 * [Activity] must create and hold an instance of a Scene.
 *
 * Once created, the application can use the Scene object to create spatialized entities, such as
 * Widget panels and geometric models, set the background environment, and anchor content to the
 * real world.
 */
// TODO: b/455593773 - Restrict ctor once YTXR ports to JXR proper, and is no longer a chimeric app.
@Suppress("NotCloseable")
public class Scene @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public constructor() :
    SessionConnector {

    internal val entityManager = EntityManager()

    internal lateinit var sceneRuntime: SceneRuntime
        private set

    /**
     * The [SpatialEnvironment] for this scene.
     *
     * This object provides APIs to manage the XR background and passthrough settings. Use it to set
     * a custom skybox, define the 3D geometry of the environment, and control the opacity of the
     * camera passthrough feed.
     *
     * @see SpatialEnvironment
     */
    public lateinit var spatialEnvironment: SpatialEnvironment
        private set

    /**
     * The [PerceptionSpace] represents the origin of the space in which ARCore for Jetpack XR
     * provides tracking info. The transformations provided by the PerceptionSpace are only valid
     * for the call frame, as the transformation can be changed by the system at any time.
     */
    public lateinit var perceptionSpace: PerceptionSpace
        private set

    /**
     * The [ActivitySpace] is a special entity that represents the space in which the application is
     * launched. It is the default parent of all entities in the scene.
     *
     * The ActivitySpace is created automatically when the [androidx.xr.runtime.Session] is created.
     */
    public lateinit var activitySpace: ActivitySpace
        private set

    /**
     * A spatialized [MainPanelEntity] associated with the "main window" for the Activity. When in
     * Home Space Mode, this is the application's "main window".
     *
     * If called multiple times, this will return the same MainPanelEntity.
     */
    public lateinit var mainPanelEntity: MainPanelEntity
        private set

    /**
     * The current [Set] of [SpatialCapability] constants available in the Session. The set may
     * change within a session. The returned object will not update if the capabilities change; this
     * method should be called again to get the latest set of capabilities, or clients can subscribe
     * to changes with [addSpatialCapabilitiesChangedListener].
     */
    public lateinit var spatialCapabilities: Set<SpatialCapability>
        private set

    /**
     * The primary [Entity] that acts as a spatial reference for the scene's content.
     *
     * The default behavior on a spatial mode change uses this Entity to maintain a consistent
     * spatial context for the user. When the scene enters Full Space Mode or is re-centered, the
     * system provides a recommended pose and scale. This ensures continuity of the user's attention
     * across spatial mode changes such as during transitions into Full Space Mode.
     *
     * Unmovable Entities, such as [AnchorEntity] or [ActivitySpace], cannot be set as the
     * [Scene.keyEntity] and will throw [IllegalArgumentException] if set.
     *
     * This field can be `null` if no key entity has been set (default), or if the key entity was
     * cleared by setting this value to `null`. When `null`, the default listener takes no action
     * during spatial mode changes.
     *
     * When a new non-null [Entity] is assigned as [keyEntity], the [spatialModeChangedListener] is
     * immediately invoked with the last known recommended pose and scale values if the following
     * conditions are met:
     * 1. The previous value of [keyEntity] was `null`.
     * 2. There are cached pose and scale values, provided by the system earlier.
     */
    public var keyEntity: Entity? = null
        set(value) {
            when (value) {
                is AnchorEntity ->
                    throw IllegalArgumentException("AnchorEntity cannot be set as the keyEntity.")

                is ActivitySpace ->
                    throw IllegalArgumentException("ActivitySpace cannot be set as the keyEntity.")

                else -> {
                    // If the previous keyEntity was from null value, invoke the
                    // spatialModeChangedListener to apply cached values to new keyEntity.
                    val wasNull = field == null
                    field = value
                    sceneRuntime.keyEntity = (value as? BaseEntity<*>)?.rtEntity
                    // If we've just transitioned from a null to a non-null entity,
                    // and we have cached values, apply them to the new entity.
                    if (wasNull && value != null) {
                        lastRecommendedPose?.let { pose ->
                            lastRecommendedScale?.let { scale ->
                                val event = SpatialModeChangeEvent(pose, scale.x)
                                spatialModeChangedExecutor.execute {
                                    spatialModeChangedListener.accept(event)
                                }
                            }
                        }
                    }
                }
            }
        }

    /**
     * Checks if boundary consent has been granted, which is a key safety prerequisite before
     * showing immersive content(i.e.,content that fully or substantially obscures the passthrough
     * view).
     *
     * @return `true` if the user has granted consent. Returns `false` otherwise, in which case
     *   showing immersive content is strongly discouraged.
     *
     * **Note:** Advanced users may disable the entire boundary system in developer settings. If the
     * boundary system is disabled, this method will also return `true`, as this is treated as an
     * **implicit** form of consent. However, in this specific scenario, the system will not present
     * the boundary line to the user upon approach.
     */
    public val isBoundaryConsentGranted: Boolean
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        get() = sceneRuntime.isBoundaryConsentGranted

    private var lastRecommendedPose: Pose? = null
    private var lastRecommendedScale: Vector3? = null

    private val defaultSpatialModeChangedListener =
        Consumer<SpatialModeChangeEvent> { event ->
            keyEntity?.setPose(event.recommendedPose, Space.ACTIVITY)
            keyEntity?.setScale(event.recommendedScale, Space.ACTIVITY)
        }
    private var spatialModeChangedListener = defaultSpatialModeChangedListener
    private var spatialModeChangedExecutor: Executor = HandlerExecutor.mainThreadExecutor

    private val spatialCapabilitiesListeners:
        ConcurrentMap<Consumer<Set<SpatialCapability>>, Executor> =
        ConcurrentHashMap()

    private val rtSpatialCapabilitiesListener =
        Consumer<SpatialCapabilities> {
            val spatialCaps = it.toSpatialCapabilities()
            spatialCapabilities = spatialCaps
            spatialCapabilitiesListeners.forEach { (listener, executor) ->
                executor.execute { listener.accept(spatialCaps) }
            }
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun initialize(runtimes: List<JxrRuntime>) {
        this.sceneRuntime = runtimes.filterIsInstance<SceneRuntime>().first()
        spatialEnvironment = SpatialEnvironment(sceneRuntime, entityManager)
        perceptionSpace = PerceptionSpace.create(sceneRuntime)
        activitySpace = ActivitySpace.create(sceneRuntime, entityManager)
        val perceptionRuntime = runtimes.filterIsInstance<PerceptionRuntime>().first()
        mainPanelEntity =
            MainPanelEntity.create(
                perceptionRuntime.lifecycleManager,
                sceneRuntime,
                perceptionSpace,
                entityManager,
            )
        sceneRuntime.spatialModeChangeListener =
            RtSpatialModeChangeListener { recommendedPose, recommendedScale ->
                lastRecommendedPose = recommendedPose
                lastRecommendedScale = recommendedScale
                val event = SpatialModeChangeEvent(recommendedPose, recommendedScale.x)
                spatialModeChangedExecutor.execute { spatialModeChangedListener.accept(event) }
            }

        spatialCapabilities = sceneRuntime.spatialCapabilities.toSpatialCapabilities()
        sceneRuntime.addSpatialCapabilitiesChangedListener(
            HandlerExecutor.mainThreadExecutor,
            rtSpatialCapabilitiesListener,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun close() {
        entityManager.clear()
        sceneRuntime.removeSpatialCapabilitiesChangedListener(rtSpatialCapabilitiesListener)
        spatialCapabilitiesListeners.keys.forEach { removeSpatialCapabilitiesChangedListener(it) }
        keyEntity = null
        clearSpatialModeChangedListener()
        clearSpatialVisibilityChangedListener()
        removeSceneFromCache(this)
    }

    /**
     * The current clipping configuration of all panels in the Scene.
     *
     * Setting this property updates the clipping behavior.
     *
     * @see PanelClippingConfig
     */
    public var panelClippingConfig: PanelClippingConfig = PanelClippingConfig()
        set(value) {
            field = value
            sceneRuntime.enablePanelDepthTest(value.isDepthTestEnabled)
        }

    /**
     * Adds the given [Consumer] as a listener to be invoked when the boundary consent state
     * changes.
     *
     * The listener will be invoked asynchronously on the **main thread executor**.
     *
     * @param listener The [Consumer] to be invoked with the new boundary consent state (`true` if
     *   granted, `false` otherwise). Refer to [Scene.isBoundaryConsentGranted] for a detailed
     *   explanation of the states.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun addOnBoundaryConsentChangedListener(listener: Consumer<Boolean>) {
        addOnBoundaryConsentChangedListener(HandlerExecutor.mainThreadExecutor, listener)
    }

    /**
     * Adds the given [Consumer] as a listener to be invoked when the boundary consent state
     * changes.
     *
     * @param callbackExecutor The [Executor] on which to invoke the listener.
     * @param listener The [Consumer] to be invoked asynchronously on the given [callbackExecutor]
     *   with the new boundary consent state (`true` if granted, `false` otherwise). Refer to
     *   [Scene.isBoundaryConsentGranted] for a detailed explanation of the states.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun addOnBoundaryConsentChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<Boolean>,
    ) {
        sceneRuntime.addOnBoundaryConsentChangedListener(callbackExecutor, listener)
    }

    /**
     * Releases the given [Consumer] from receiving updates when the boundary consent state changes.
     *
     * The listeners are automatically released at the end of the Scene's lifecycle even if this
     * method is not explicitly called.
     *
     * @param listener The [Consumer] to be removed. It will no longer receive change events.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun removeOnBoundaryConsentChangedListener(listener: Consumer<Boolean>) {
        sceneRuntime.removeOnBoundaryConsentChangedListener(listener)
    }

    /**
     * Adds the given [Consumer] as a listener to be invoked when this
     * [androidx.xr.runtime.Session]'s spatial capabilities change.
     *
     * @param listener The Consumer to be invoked asynchronously, on the main thread. The set
     *   includes every currently-available [SpatialCapability].
     */
    public fun addSpatialCapabilitiesChangedListener(
        listener: Consumer<Set<SpatialCapability>>
    ): Unit = addSpatialCapabilitiesChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /**
     * Adds the given [Consumer] as a listener to be invoked when this
     * [androidx.xr.runtime.Session]'s spatial capabilities change.
     *
     * @param callbackExecutor The [Executor] to run the listener on.
     * @param listener The Consumer to be invoked asynchronously on the given callbackExecutor. The
     *   set includes every currently-available [SpatialCapability].
     */
    public fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<Set<SpatialCapability>>,
    ) {
        spatialCapabilitiesListeners[listener] = callbackExecutor
    }

    /**
     * Releases the given [Consumer] from receiving updates when the [androidx.xr.runtime.Session]'s
     * [SpatialCapability] change.
     *
     * The listeners are automatically released at the end of the Scene's lifecycle even if this
     * method is not explicitly called.
     *
     * @param listener The Consumer to be removed. It will no longer receive change events.
     */
    public fun removeSpatialCapabilitiesChangedListener(
        listener: Consumer<Set<SpatialCapability>>
    ) {
        spatialCapabilitiesListeners.remove(listener)
    }

    /**
     * Returns all entities of the given type or its subtypes.
     *
     * @param type the type of [Entity] to return.
     * @return a list of all entities of the given type.
     */
    public fun <T : Entity> getEntitiesOfType(type: Class<out T>): List<T> =
        entityManager.getEntitiesOfType(type)

    internal fun getEntityForRtEntity(entity: RtEntity): Entity? {
        return entityManager.getEntityForRtEntity(entity)
    }

    /**
     * Sets the listener to be invoked when the spatial visibility of the rendered content of the
     * entire scene (all entities, including children of [AnchorEntity]s and [ActivitySpace])
     * changes within the user's field of view. In Home Space Mode, the listener continues to
     * monitor the spatial visibility of the application's main panel.
     *
     * This API only checks if the bounding box of all rendered content (even if partially
     * transparent) is within the user's field of view. Content not rendered due to full
     * transparency (alpha=0) or being hidden is not considered. If the entities in the scene or any
     * of their ancestors are hidden using [Entity.setEnabled] (enabled=false) or if the entities
     * are turned fully transparent using [Entity.setAlpha] (alpha=0.0), then the SpatialVisibility
     * checks will return [SpatialVisibility.OUTSIDE_FIELD_OF_VIEW].
     *
     * The listener is invoked on the provided [Executor].
     *
     * There can only be one listener set at a time. If a new listener is set, the previous listener
     * will be released.
     *
     * @param callbackExecutor The [Executor] to run the listener on.
     * @param listener The [Consumer] to be invoked asynchronously on the given callbackExecutor
     *   whenever the [SpatialVisibility] of the renderable content changes.
     */
    public fun setSpatialVisibilityChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialVisibility>,
    ) {
        // Wrap client's listener in a callback that converts the sceneRuntime's
        // SpatialVisibility.
        val rtListener =
            Consumer<RtSpatialVisibility> { rtVisibility: RtSpatialVisibility ->
                listener.accept(rtVisibility.toSpatialVisibility())
            }
        sceneRuntime.setSpatialVisibilityChangedListener(callbackExecutor, rtListener)
    }

    /**
     * Sets the listener to be invoked on the main thread executor when the spatial visibility of
     * the rendered content of the entire scene (all entities, including children of [AnchorEntity]s
     * and [ActivitySpace]) changes within the user's field of view. In Home Space Mode, the
     * listener continues to monitor the spatial visibility of the application's main panel.
     *
     * This API only checks if the bounding box of all rendered content (even if partially
     * transparent) is within the user's field of view. Content not rendered due to full
     * transparency (alpha=0) or being hidden is not considered. If the entities in the scene or any
     * of their ancestors are hidden using [Entity.setEnabled] (enabled=false) or if the entities
     * are turned fully transparent using [Entity.setAlpha] (alpha=0.0), then the SpatialVisibility
     * checks will return [SpatialVisibility.OUTSIDE_FIELD_OF_VIEW].
     *
     * There can only be one listener set at a time. If a new listener is set, the previous listener
     * will be released.
     *
     * @param listener The [Consumer] to be invoked asynchronously on the main thread whenever the
     *   [SpatialVisibility] of the renderable content changes.
     */
    public fun setSpatialVisibilityChangedListener(listener: Consumer<SpatialVisibility>): Unit =
        setSpatialVisibilityChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /**
     * Releases the listener previously added by [setSpatialVisibilityChangedListener].
     *
     * The listener is automatically released at the end of the Scene's lifecycle even if this
     * method is not explicitly called.
     */
    public fun clearSpatialVisibilityChangedListener(): Unit =
        sceneRuntime.clearSpatialVisibilityChangedListener()

    /**
     * Sets the listener to be invoked when the spatial mode for the scene has changed.
     *
     * The listener is invoked on the provided [Executor].
     *
     * There can only be one listener set at a time. If a new listener is set, the previous listener
     * will be released.
     *
     * @param callbackExecutor The [Executor] to run the listener on.
     * @param listener The [Consumer] to be invoked asynchronously on the given callbackExecutor
     *   whenever the spatial mode has changed.
     */
    public fun setSpatialModeChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialModeChangeEvent>,
    ) {
        spatialModeChangedListener = listener
        spatialModeChangedExecutor = callbackExecutor
    }

    /**
     * Sets the listener to be invoked on the main thread executor when the spatial mode for the
     * scene has changed.
     *
     * There can only be one listener set at a time. If a new listener is set, the previous listener
     * will be released.
     *
     * @param listener The [Consumer] to be invoked asynchronously on the main thread whenever the
     *   spatial mode has changed.
     */
    public fun setSpatialModeChangedListener(listener: Consumer<SpatialModeChangeEvent>) {
        setSpatialModeChangedListener(HandlerExecutor.mainThreadExecutor, listener)
    }

    /**
     * Releases the listener previously set by [setSpatialModeChangedListener] and reinstates the
     * default behavior of automatically updating the [keyEntity]'s pose and scale on the main
     * thread executor.
     *
     * The listener is automatically released at the end of the Scene's lifecycle even if this
     * method is not explicitly called.
     */
    public fun clearSpatialModeChangedListener() {
        spatialModeChangedListener = defaultSpatialModeChangedListener
        spatialModeChangedExecutor = HandlerExecutor.mainThreadExecutor
    }

    /**
     * If the [Activity] has focus, causes the Activity to be placed in Full Space Mode. Otherwise,
     * this call does nothing.
     */
    public fun requestFullSpaceMode(): Unit = sceneRuntime.requestFullSpaceMode()

    /**
     * If the [Activity] has focus, causes the Activity to be placed in Home Space Mode. Otherwise,
     * this call does nothing.
     */
    public fun requestHomeSpaceMode(): Unit = sceneRuntime.requestHomeSpaceMode()
}
