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

package androidx.xr.scenecore.runtime

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.math.Pose
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Defines the contract for a platform-agnostic runtime that manages the scene graph and spatial
 * logic backend.
 *
 * This interface is responsible for the logical structure of the XR experience, managing the
 * hierarchy of entities, their transformations, and their behaviors. It provides the core
 * functionalities for spatial computing, such as world tracking, plane detection, and anchoring
 * objects to the physical environment. It also handles user interaction components and spatial
 * audio.
 *
 * This API is intended for internal use only and is not a public API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SceneRuntime : JxrRuntime {
    /** Return the Spatial Capabilities set that are currently supported by the platform. */
    public val spatialCapabilities: SpatialCapabilities

    /** Returns the Activity Space entity at the root of the scene. */
    public val activitySpace: ActivitySpace

    /** Returns the PerceptionSpaceScenePose for the Session. */
    public val perceptionSpaceActivityPose: PerceptionSpaceScenePose

    /** Get the PanelEntity associated with the main window for the Runtime. */
    public val mainPanelEntity: PanelEntity

    /**
     * Key entity, specifying the entity to use for spatial continuity hint across spatial state
     * transitions.
     */
    public var keyEntity: Entity?

    /** Returns the Environment for the Session. */
    public val spatialEnvironment: SpatialEnvironment

    /**
     * Returns a [SpatialModeChangeListener] instance.
     *
     * Setting this property will update the handler that is used to process spatial mode changes.
     */
    public var spatialModeChangeListener: SpatialModeChangeListener?

    /** Returns a [SoundPoolExtensionsWrapper] instance. */
    public val soundPoolExtensionsWrapper: SoundPoolExtensionsWrapper

    /** Returns a [AudioTrackExtensionsWrapper] instance. */
    public val audioTrackExtensionsWrapper: AudioTrackExtensionsWrapper

    /** Returns a [MediaPlayerExtensionsWrapper] instance. */
    public val mediaPlayerExtensionsWrapper: MediaPlayerExtensionsWrapper

    /**
     * The current state of the boundary consent from the underlying system.
     *
     * This is `true` if consent has been explicitly granted or the boundary system has been
     * disabled by the user (implicit consent). Otherwise, this is `false`.
     */
    public val isBoundaryConsentGranted: Boolean

    /**
     * Returns an [ScenePose] based off of a position within the perception space.
     *
     * @param pose The pose with respect to the perception space [ActivitySpace].
     */
    public fun getScenePoseFromPerceptionPose(pose: Pose): ScenePose

    /**
     * A factory function to create a platform PanelEntity. The parent can be any entity.
     *
     * @param context Application Context.
     * @param pose Initial pose of the panel.
     * @param view View inflating this panel.
     * @param dimensions Size of the panel in meters.
     * @param name Name of the panel.
     * @param parent Parent entity.
     */
    public fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        dimensions: Dimensions,
        name: String,
        parent: Entity?,
    ): PanelEntity

    /**
     * A factory function to create a platform PanelEntity. The parent can be any entity.
     *
     * @param context Application Context.
     * @param pose Initial pose of the panel.
     * @param view View inflating this panel.
     * @param pixelDimensions Dimensions for the underlying surface for the given view in pixels.
     * @param name Name of the panel.
     * @param parent Parent entity.
     */
    public fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        pixelDimensions: PixelDimensions,
        name: String,
        parent: Entity?,
    ): PanelEntity

    /**
     * Factory function to create ActivityPanel to launch/move activity into.
     *
     * @param pose Initial pose of the panel.
     * @param windowBoundsPx Boundary for the window
     * @param name Name of the panel.
     * @param hostActivity Activity to host the panel.
     * @param parent Parent entity.
     */
    public fun createActivityPanelEntity(
        pose: Pose,
        windowBoundsPx: PixelDimensions,
        name: String,
        hostActivity: Activity,
        parent: Entity?,
    ): ActivityPanelEntity

    /** A factory function to create an Anchor entity. */
    public fun createAnchorEntity(): AnchorEntity

    /**
     * A factory function to create a basic entity. This entity is used as a connection point for
     * attaching children entities and managing them (i.e. setPose()) as a group.
     *
     * @param pose Initial pose of the entity.
     * @param name Name of the entity.
     * @param parent Parent entity.
     */
    public fun createEntity(pose: Pose, name: String?, parent: Entity?): Entity

    @Deprecated(message = "Use createEntity instead.")
    public fun createGroupEntity(pose: Pose, name: String, parent: Entity?): Entity

    /** A function to create a XR Runtime Entity. */
    public fun createLoggingEntity(pose: Pose): LoggingEntity

    /**
     * Adds the given {@link Consumer} as a listener to be invoked when this Session's current
     * SpatialCapabilities change. {@link Consumer#accept(SpatialCapabilities)} will be invoked on
     * the given Executor.
     *
     * @param callbackExecutor Executor on which the listener will be invoked.
     * @param listener Listener to be invoked when the Session's SpatialCapabilities change.
     */
    @Suppress("ExecutorRegistration")
    public fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    )

    /**
     * Releases the given {@link Consumer} from receiving updates when the Session's {@link
     * SpatialCapabilities} change.
     *
     * @param listener Listener to be removed from the list of listeners.
     */
    public fun removeSpatialCapabilitiesChangedListener(listener: Consumer<SpatialCapabilities>)

    /**
     * Sets the listener to be invoked when the spatial visibility of the rendered content of the
     * entire scene (all entities, including children of anchors and activitySpace) changes within
     * the user's field of view.
     *
     * This API only checks if the bounds of the renderable content are within the user's field of
     * view. It does not check if the rendered content is visible to the user. For example, if the
     * user is looking straight ahead, and there's only a single invisible child entity (alpha = 0)
     * in front of the user, this API will return SpatialVisibility.WITHIN_FOV even though the user
     * cannot see anything.
     *
     * The listener is invoked on the provided executor. If the app intends to modify the UI
     * elements/views during the callback, the app should provide the thread executor that is
     * appropriate for the UI operations. For example, if the app is using the main thread to render
     * the UI, the app should provide the main thread (Looper.getMainLooper()) executor. If the app
     * is using a separate thread to render the UI, the app should provide the executor for that
     * thread.
     *
     * There can only be one listener set at a time. If a new listener is set, the previous listener
     * will be released.
     *
     * @param callbackExecutor The executor to run the listener on.
     * @param listener The [Consumer] to be invoked asynchronously on the given callbackExecutor
     *   whenever the spatial visibility of the renderable content changes. The parameter passed to
     *   the Consumer’s accept method is the new value for [SpatialVisibility].
     */
    public fun setSpatialVisibilityChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialVisibility>,
    )

    /** Releases the listener previously added by [setSpatialVisibilityChangedListener]. */
    public fun clearSpatialVisibilityChangedListener()

    /**
     * Sets the listener to be invoked when the perceived resolution of the main window changes in
     * Home Space Mode.
     *
     * The main panel's own rotation and the display's viewing direction are disregarded; this value
     * represents the pixel dimensions of the panel on the camera view without changing its distance
     * to the display.
     *
     * The listener is invoked on the provided executor. If the app intends to modify the UI
     * elements/views during the callback, the app should provide the thread executor that is
     * appropriate for the UI operations. For example, if the app is using the main thread to render
     * the UI, the app should provide the main thread (Looper.getMainLooper()) executor. If the app
     * is using a separate thread to render the UI, the app should provide the executor for that
     * thread.
     *
     * Non-zero values are only guaranteed in Home Space Mode. In Full Space Mode, the callback will
     * always return a (0,0) size. Use the [PanelEntity.getPerceivedResolution] or
     * [SurfaceEntity.getPerceivedResolution] methods directly on the relevant entities to retrieve
     * non-zero values in Full Space Mode.
     *
     * @param callbackExecutor The executor to run the listener on.
     * @param listener The [Consumer] to be invoked asynchronously on the given callbackExecutor
     *   whenever the maximum perceived resolution of the main panel changes. The parameter passed
     *   to the Consumer’s accept method is the new value for [PixelDimensions] value for perceived
     *   resolution.
     */
    public fun addPerceivedResolutionChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<PixelDimensions>,
    )

    /**
     * Releases the listener previously added by [addPerceivedResolutionChangedListener].
     *
     * @param listener The [Consumer] to be removed. It will no longer receive change events.
     */
    public fun removePerceivedResolutionChangedListener(listener: Consumer<PixelDimensions>)

    /**
     * If the primary Activity for the Session that owns this object has focus, causes it to be
     * placed in FullSpace Mode. Otherwise, this call does nothing.
     */
    public fun requestFullSpaceMode()

    /**
     * If the primary Activity for the Session that owns this object has focus, causes it to be
     * placed in HomeSpace Mode. Otherwise, this call does nothing.
     */
    public fun requestHomeSpaceMode()

    /**
     * Sets the full space mode flag to the given {@link Bundle}.
     *
     * The {@link Bundle} then could be used to launch an {@link Activity} with requesting to enter
     * full space mode through {@link Activity#startActivity}. If there's a bundle used for
     * customizing how the {@link Activity} should be started by {@link ActivityOptions.toBundle} or
     * {@link androidx.core.app.ActivityOptionsCompat.toBundle}, it's suggested to use the bundle to
     * call this method.
     *
     * The flag will be ignored when no {@link Intent.FLAG_ACTIVITY_NEW_TASK} is set in the bundle,
     * or it is not started from a focused Activity context.
     *
     * This flag is also ignored when the {@link android.window.PROPERTY_XR_ACTIVITY_START_MODE}
     * property is set to a value other than XR_ACTIVITY_START_MODE_UNDEFINED in the
     * AndroidManifest.xml file for the activity being launched.
     *
     * @param bundle the input bundle to set with the full space mode flag.
     * @return the input {@code bundle} with the full space mode flag set.
     */
    public fun setFullSpaceMode(bundle: Bundle): Bundle

    /**
     * Sets the inherit full space mode environment flag to the given {@link Bundle}.
     *
     * The {@link Bundle} then could be used to launch an {@link Activity} with requesting to enter
     * full space mode while inherit the existing environment through {@link
     * Activity#startActivity}. If there's a bundle used for customizing how the {@link Activity}
     * should be started by {@link ActivityOptions.toBundle} or {@link
     * androidx.core.app.ActivityOptionsCompat.toBundle}, it's suggested to use the bundle to call
     * this method.
     *
     * When launched, the activity will be in full space mode and also inherits the environment from
     * the launching activity. If the inherited environment needs to be animated, the launching
     * activity has to continue updating the environment even after the activity is put into the
     * stopped state.
     *
     * The flag will be ignored when no {@link Intent.FLAG_ACTIVITY_NEW_TASK} is set in the intent,
     * or it is not started from a focused Activity context.
     *
     * The flag will also be ignored when there is no environment to inherit or the activity has its
     * own environment set already.
     *
     * This flag is ignored too when the {@link android.window.PROPERTY_XR_ACTIVITY_START_MODE}
     * property is set to a value other than XR_ACTIVITY_START_MODE_UNDEFINED in the
     * AndroidManifest.xml file for the activity being launched.
     *
     * For security reasons, Z testing for the new activity is disabled, and the activity is always
     * drawn on top of the inherited environment. Because Z testing is disabled, the activity should
     * not spatialize itself.
     *
     * @param bundle the input bundle to set with the inherit full space mode environment flag.
     * @return the input {@code bundle} with the inherit full space mode flag set.
     */
    public fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle

    /**
     * Sets a preferred main panel aspect ratio for home space mode.
     *
     * The ratio is only applied to the activity. If the activity launches another activity in the
     * same task, the ratio is not applied to the new activity. Also, while the activity is in full
     * space mode, the preference is temporarily removed.
     *
     * If the activity's current aspect ratio differs from the {@code preferredRatio}, the panel is
     * automatically resized. This resizing preserves the panel's area. To avoid runtime resizing,
     * consider specifying the desired aspect ratio in your {@code AndroidManifest.xml}. This
     * ensures your activity launches with the preferred aspect ratio from the start.
     *
     * @param activity the activity to set the preference.
     * @param preferredRatio the aspect ratio determined by taking the panel's width over its
     *   height. A value <= 0.0f means there are no preferences.
     */
    public fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float)

    /**
     * Sets whether the depth test is enabled for all panels in the Scene when the Scene is in full
     * space mode. Panels in home space mode are unaffected.
     *
     * When the depth test for panels is enabled, panels in the Scene will undergo depth testing,
     * where they can appear behind other content in the Scene. When the depth test is disabled,
     * panels in the Scene do not undergo depth tests, that will always be drawn on top of other
     * objects in the Scene that were already drawn. Panels and non-panel content (ex:
     * SurfaceEntity, GltfEntity) are always drawn after the SpatialEnvironment in back to front
     * order when such an order exists. Subsequent content will be drawn on top of panels with no
     * depth test if the subsequent content is drawn later.
     *
     * This method says "panel" because it only affects panels. Other content in the Scene is
     * unaffected by this setting.
     *
     * By default the depth test is enabled for all panels in the Scene. It can be disabled, or
     * re-enabled, by using this method.
     *
     * @param enabled True to enable the depth test for all panels in the Scene (default), false to
     *   disable the depth test for all panels in the Scene.
     */
    public fun enablePanelDepthTest(enabled: Boolean)

    /**
     * Create an Interactable component.
     *
     * @param executor Executor to use for input callbacks.
     * @param listener [InputEventListener] for this component.
     * @return InteractableComponent instance.
     */
    // @Suppress("ExecutorRegistration")
    public fun createInteractableComponent(
        executor: Executor,
        listener: InputEventListener,
    ): InteractableComponent

    /**
     * Creates an instance of an AnchorPlacement object.
     *
     * <p>This can be used in movable components to specify the anchor placement for the entity.
     *
     * @param planeTypeFilter A set of plane types to filter for.
     * @param planeSemanticFilter A set of plane semantics to filter for.
     * @return [AnchorPlacement] instance.
     */
    public fun createAnchorPlacementForPlanes(
        planeTypeFilter: Set<@JvmSuppressWildcards PlaneType>,
        planeSemanticFilter: Set<@JvmSuppressWildcards PlaneSemantic>,
    ): AnchorPlacement

    /**
     * Create an instance of [MovableComponent]. This component allows the user to move the entity.
     *
     * @param systemMovable A [Boolean] which causes the system to automatically apply transform
     *   updates to the entity in response to user interaction.
     * @param scaleInZ A [Boolean] which tells the system to update the scale of the Entity as the
     *   user moves it closer and further away. This is mostly useful for Panel auto-rescaling with
     *   Distance
     * @param userAnchorable A [Boolean] which tells the system that the entity can be anchored to
     *   planes detected by the system.
     * @return [MovableComponent] instance.
     */
    public fun createMovableComponent(
        systemMovable: Boolean,
        scaleInZ: Boolean,
        userAnchorable: Boolean,
    ): MovableComponent

    /**
     * Create an instance of [ResizableComponent]. This component allows the user to resize the
     * entity.
     *
     * @param minimumSize Minimum size constraint.
     * @param maximumSize Maximum size constraint.
     * @return [ResizableComponent] instance.
     */
    public fun createResizableComponent(
        minimumSize: Dimensions,
        maximumSize: Dimensions,
    ): ResizableComponent

    /**
     * Create an instance of {@link PointerCaptureComponent}. This component allows the user to
     * capture and redirect to itself all input that would be received by entities other than the
     * Entity it is attached to and that entity's children.
     *
     * <p>In order to enable pointer capture, an application must be in full space and the entity it
     * is attached to must be visible.
     *
     * <p>Attach this component to the entity to enable pointer capture, detach the component to
     * restore normal input flow.
     *
     * @param executor Executor used to propagate state and input events.
     * @param stateListener Callback for updates to the state of pointer capture. Pointer capture
     *   may be temporarily lost by the application for a variety of reasons and this callback will
     *   notify of when that happens.
     * @param inputListener Callback that will receive captured [InputEvent]s
     */
    public fun createPointerCaptureComponent(
        executor: Executor,
        stateListener: PointerCaptureComponent.StateListener,
        inputListener: InputEventListener,
    ): PointerCaptureComponent

    public fun createSpatialPointerComponent(): SpatialPointerComponent

    /**
     * Creates an instance of [BoundsComponent].
     *
     * This component allows an application to monitor changes to the spatial bounds of an entity.
     * Once created, the component can be attached to an entity that supports bounds tracking. After
     * attachment, listeners can be added via [BoundsComponent.addOnBoundsUpdateListener] to receive
     * notifications when the entity's bounds change due to animations or other transformations.
     *
     * @return A new [BoundsComponent] instance.
     * @see BoundsComponent
     */
    public fun createBoundsComponent(): BoundsComponent

    /**
     * Adds the given [Consumer] as a listener to be invoked when the boundary consent state
     * changes.
     *
     * @param callbackExecutor The [Executor] on which to invoke the listener.
     * @param listener The [Consumer] to be invoked asynchronously on the given [callbackExecutor]
     *   with the new boundary consent state (`true` if granted, `false` otherwise). Refer to
     *   [isBoundaryConsentGranted] for a detailed explanation of the states.
     */
    public fun addOnBoundaryConsentChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<Boolean>,
    )

    /**
     * Releases the given [Consumer] from receiving updates when the boundary consent state changes.
     *
     * @param listener The [Consumer] to be removed. It will no longer receive change events.
     */
    public fun removeOnBoundaryConsentChangedListener(listener: Consumer<Boolean>)
}
