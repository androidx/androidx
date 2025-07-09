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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import android.app.Activity
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.xr.runtime.SessionConnector
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.internal.SpatialModeChangeListener as RtSpatialModeChangeListener
import androidx.xr.runtime.internal.SpatialVisibility as RtSpatialVisibility
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
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
@Suppress("NotCloseable")
public class Scene : SessionConnector {

    internal val entityManager = EntityManager()

    internal lateinit var platformAdapter: JxrPlatformAdapter
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
     * The ActivitySpace is a special entity that represents the space in which the application is
     * launched. It is the default parent of all entities in the scene.
     *
     * The ActivitySpace is created automatically when the Session is created.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public lateinit var activitySpace: ActivitySpace
        private set

    // TODO: 378706624 - Remove this method once we have a better way to handle the root entity.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public lateinit var activitySpaceRoot: Entity
        private set

    /**
     * The [SpatialUser] represents the user within the XR scene, providing access to tracking
     * information for the user's head and eyes.
     *
     * Use it to get the following:
     * - **Head Pose**: Access [SpatialUser.head] to get the position and orientation of the user's
     *   head in the scene.
     * - **Camera Views**: Access [SpatialUser.cameraViews] to get the pose and field of view for
     *   each of the user's camera views.
     *
     * Note: Accessing properties on [SpatialUser] requires head tracking to be enabled in the
     * session [androidx.xr.runtime.Session.config].
     *
     * @see SpatialUser
     * @see Head
     * @see CameraView
     */
    public lateinit var spatialUser: SpatialUser
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
     * Returns the current [SpatialCapabilities] of the Session. The set of capabilities can change
     * within a session. The returned object will not update if the capabilities change; this method
     * should be called again to get the latest set of capabilities.
     */
    public var spatialCapabilities: SpatialCapabilities = SpatialCapabilities(0)
        private set
        get() = platformAdapter.spatialCapabilities.toSpatialCapabilities()

    /**
     * The [Entity] that will be used by the default [SpatialModeChangeListener] to be placed at a
     * location provided by the system thinks is an optimal placement for content when the scene
     * enters FULL_SPACE_MANAGED mode or is re-centered. This ensures continuity of the user's
     * attention between sequential FULL_SPACE_MANAGED sessions.
     *
     * Unmovable entities are not allowed to be the key, for example, [AnchorEntity].
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public var keyEntity: Entity? = null
        private set

    /**
     * The [SpatialModeChangeListener] used to handle scenegraph updates when the spatial mode for
     * the scene changes.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public var spatialModeChangeListener: SpatialModeChangeListener =
        /**
         * The default [spatialModeChangeListener], which translates the key entity to the
         * recommended pose when the scene encounters spatial mode change, and applies the
         * recommended scale. This default handler can be replaced with the client's own
         * [SpatialModeChangeListener] by updating the [Scene.spatialModeChangeListener] property.
         */
        object : SpatialModeChangeListener {
            override fun onSpatialModeChanged(recommendedPose: Pose, recommendedScale: Float) {
                keyEntity?.setPose(recommendedPose, Space.ACTIVITY)
                keyEntity?.setScale(recommendedScale, Space.ACTIVITY)
            }
        }

    private val spatialCapabilitiesListeners:
        ConcurrentMap<Consumer<SpatialCapabilities>, Consumer<RtSpatialCapabilities>> =
        ConcurrentHashMap()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun initialize(
        lifecycleManager: LifecycleManager,
        platformAdapter: JxrPlatformAdapter,
    ): Unit {
        this.platformAdapter = platformAdapter
        spatialEnvironment = SpatialEnvironment(platformAdapter)
        perceptionSpace = PerceptionSpace.create(platformAdapter)
        activitySpace = ActivitySpace.create(platformAdapter, entityManager)
        spatialUser = SpatialUser.create(lifecycleManager, platformAdapter)
        mainPanelEntity = MainPanelEntity.create(lifecycleManager, platformAdapter, entityManager)
        activitySpaceRoot =
            entityManager.getEntityForRtEntity(platformAdapter.activitySpaceRootImpl)!!
        platformAdapter.spatialModeChangeListener =
            object : RtSpatialModeChangeListener {
                override fun onSpatialModeChanged(
                    recommendedPose: Pose,
                    recommendedScale: Vector3,
                ) {
                    spatialModeChangeListener.onSpatialModeChanged(
                        recommendedPose,
                        recommendedScale.x,
                    )
                }
            }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun close(): Unit {
        entityManager.clear()
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
            platformAdapter.enablePanelDepthTest(value.isDepthTestEnabled)
        }

    /**
     * Adds the given [Consumer] as a listener to be invoked when this [Session]'s current
     * [SpatialCapabilities] change.
     *
     * @param listener The Consumer to be invoked asynchronously on the main thread executor
     *   whenever the SpatialCapabilities changes.
     */
    public fun addSpatialCapabilitiesChangedListener(
        listener: Consumer<SpatialCapabilities>
    ): Unit = addSpatialCapabilitiesChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /**
     * Adds the given [Consumer] as a listener to be invoked when this [Session]'s current
     * [SpatialCapabilities] change.
     *
     * @param callbackExecutor The [Executor] to run the listener on.
     * @param listener The Consumer to be invoked asynchronously on the given callbackExecutor
     *   whenever the SpatialCapabilities changes.
     */
    public fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ): Unit {
        // wrap the client's listener in a callback that receives & converts the platformAdapter
        // SpatialCapabilities type.
        val rtListener: Consumer<RtSpatialCapabilities> =
            Consumer<RtSpatialCapabilities> { rtCaps: RtSpatialCapabilities ->
                listener.accept(rtCaps.toSpatialCapabilities())
            }
        spatialCapabilitiesListeners.compute(
            listener,
            { _, _ ->
                platformAdapter.addSpatialCapabilitiesChangedListener(callbackExecutor, rtListener)
                rtListener
            },
        )
    }

    /**
     * Releases the given [Consumer] from receiving updates when the [Session]'s
     * [SpatialCapabilities] change.
     *
     * @param listener The Consumer to be removed. It will no longer receive change events.
     */
    public fun removeSpatialCapabilitiesChangedListener(
        listener: Consumer<SpatialCapabilities>
    ): Unit {
        spatialCapabilitiesListeners.computeIfPresent(
            listener,
            { _, rtListener ->
                platformAdapter.removeSpatialCapabilitiesChangedListener(rtListener)
                null
            },
        )
    }

    /**
     * Configures a [Bundle] to request that a new [Activity] be launched directly into Full Space
     * Mode.
     *
     * The configured bundle can be used to launch an Activity directly into Full Space Mode through
     * [Activity.startActivity]. If there's a bundle used for customizing how the Activity should be
     * started by [android.app.ActivityOptions.toBundle] or
     * [androidx.core.app.ActivityOptionsCompat.toBundle], it's suggested to use the bundle to call
     * this method.
     *
     * The flag will be ignored when no [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] is set in
     * the bundle, or it is not started from a focused Activity context.
     *
     * This flag is also ignored when the
     * [androidx.xr.runtime.manifest.PROPERTY_XR_ACTIVITY_START_MODE] property is set to a value
     * other than [androidx.xr.runtime.manifest.XR_ACTIVITY_START_MODE_UNDEFINED] in the
     * AndroidManifest.xml file for the activity being launched.
     *
     * @param bundle the input bundle to set with the Full Space Mode flag.
     * @return the input bundle with the Full Space Mode flag set.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun configureBundleForFullSpaceModeLaunch(bundle: Bundle): Bundle =
        platformAdapter.setFullSpaceMode(bundle)

    /**
     * Configures a [Bundle] to request that a new [Activity] be launched directly into Full Space
     * Mode while inheriting the environment from the launching activity.
     *
     * The configured bundle can be used to launch an Activity directly into Full Space Mode while
     * inherit the existing environment through [Activity.startActivity]. If there's a bundle used
     * for customizing how the Activity should be started by [android.app.ActivityOptions.toBundle]
     * or [androidx.core.app.ActivityOptionsCompat.toBundle], it's suggested to use the bundle to
     * call this method.
     *
     * When launched, the Activity will be in Full Space Mode and will inherit the environment from
     * the launching activity. If the inherited environment needs to be animated, the launching
     * activity has to continue updating the environment even after the activity is put into the
     * stopped state.
     *
     * The flag will be ignored when no [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] is set in
     * the intent, or it is not started from a focused Activity context.
     *
     * The flag will also be ignored when there is no environment to inherit or when the activity
     * has its own environment set already.
     *
     * This flag is also ignored when the
     * [androidx.xr.runtime.manifest.PROPERTY_XR_ACTIVITY_START_MODE] property is set to a value
     * other than [androidx.xr.runtime.manifest.XR_ACTIVITY_START_MODE_UNDEFINED] in the
     * AndroidManifest.xml file for the activity being launched.
     *
     * For security reasons, Z-testing for the new activity is disabled, and the activity is always
     * drawn on top of the inherited environment. Because Z-testing is disabled, the activity should
     * not spatialize itself.
     *
     * @param bundle the input bundle to configure with the inherit Full Space Mode environment
     *   flag.
     * @return the input bundle with the inherit Full Space Mode flag set.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun configureBundleForFullSpaceModeLaunchWithEnvironmentInherited(
        bundle: Bundle
    ): Bundle = platformAdapter.setFullSpaceModeWithEnvironmentInherited(bundle)

    /**
     * Sets a preferred main panel aspect ratio for Home Space Mode.
     *
     * The ratio is only applied to the [Activity]. If the activity launches another activity in the
     * same task, the ratio is not applied to the new activity. Also, while the activity is in Full
     * Space Mode, the preference is temporarily ignored.
     *
     * If the activity's current aspect ratio differs from the `preferredRatio`, the panel is
     * automatically resized. This resizing preserves the panel's area. To avoid runtime resizing,
     * consider specifying the desired aspect ratio in your AndroidManifest.xml. This ensures your
     * activity launches with the preferred aspect ratio from the start.
     *
     * @param activity the activity for which to set the preference.
     * @param preferredRatio the aspect ratio determined by taking the panel's width over its
     *   height. A value <= 0.0f means there are no preferences.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float): Unit =
        platformAdapter.setPreferredAspectRatio(activity, preferredRatio)

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
     * of their ancestors are hidden using [Entity.setHidden] (hidden=true) or if the entities are
     * turned fully transparent using [Entity.setAlpha] (alpha=0.0), then the SpatialVisibility
     * checks will return [SpatialVisibility.SPATIAL_VISIBILITY_OUTSIDE_FIELD_OF_VIEW].
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
        listener: Consumer<@SpatialVisibilityValue Int>,
    ): Unit {
        // Wrap client's listener in a callback that converts the platformAdapter's
        // SpatialVisibility.
        val rtListener =
            Consumer<RtSpatialVisibility> { rtVisibility: RtSpatialVisibility ->
                listener.accept(rtVisibility.toSpatialVisibility())
            }
        platformAdapter.setSpatialVisibilityChangedListener(callbackExecutor, rtListener)
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
     * of their ancestors are hidden using [Entity.setHidden] (hidden=true) or if the entities are
     * turned fully transparent using [Entity.setAlpha] (alpha=0.0), then the SpatialVisibility
     * checks will return [SpatialVisibility.SPATIAL_VISIBILITY_OUTSIDE_FIELD_OF_VIEW].
     *
     * There can only be one listener set at a time. If a new listener is set, the previous listener
     * will be released.
     *
     * @param listener The [Consumer] to be invoked asynchronously on the main thread whenever the
     *   [SpatialVisibility] of the renderable content changes.
     */
    public fun setSpatialVisibilityChangedListener(
        listener: Consumer<@SpatialVisibilityValue Int>
    ): Unit = setSpatialVisibilityChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /** Releases the listener previously added by [setSpatialVisibilityChangedListener]. */
    public fun clearSpatialVisibilityChangedListener(): Unit =
        platformAdapter.clearSpatialVisibilityChangedListener()

    /**
     * The [Entity] that will be used by the default [SpatialModeChangeListener] to be placed at a
     * location provided by the system thinks is an optimal placement for content when the scene
     * enters FULL_SPACE_MANAGED mode or is re-centered. This ensures continuity of the user's
     * attention between sequential FULL_SPACE_MANAGED sessions.
     *
     * Unmovable entities are not allowed to be the key, for example, [AnchorEntity].
     *
     * Setting null as key is allowed - in which case the default spatial mode change handler will
     * be no-op.
     *
     * @param entity the entity to set as the key.
     * @return true if the entity was successfully set as the key, false otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun setKeyEntity(entity: Entity?): Boolean {
        when (entity) {
            is AnchorEntity -> return false
            else -> {
                keyEntity = entity
                return true
            }
        }
    }

    /**
     * If the [Activity] has focus, causes the Activity to be placed in Full Space Mode. Otherwise,
     * this call does nothing.
     */
    public fun requestFullSpaceMode(): Unit = platformAdapter.requestFullSpaceMode()

    /**
     * If the [Activity] has focus, causes the Activity to be placed in Home Space Mode. Otherwise,
     * this call does nothing.
     */
    public fun requestHomeSpaceMode(): Unit = platformAdapter.requestHomeSpaceMode()
}
