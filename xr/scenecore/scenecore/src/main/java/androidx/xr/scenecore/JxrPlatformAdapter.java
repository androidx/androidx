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

package androidx.xr.scenecore;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.xr.arcore.Anchor;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** Interface for SceneCore Platform operations. This is not intended to be used by Applications. */
// TODO Add API versioning
// TODO: b/322549913 - Move subclasses into separate files
public interface JxrPlatformAdapter {

    /** Returns the Environment for the Session. */
    @NonNull
    SpatialEnvironment getSpatialEnvironment();

    /** A function to create a SceneCore Entity */
    @NonNull
    LoggingEntity createLoggingEntity(@NonNull Pose pose);

    /** Returns the Activity Space entity at the root of the scene. */
    @NonNull
    ActivitySpace getActivitySpace();

    /** Returns the HeadActivityPose for the Session or null if it is not ready */
    @Nullable
    HeadActivityPose getHeadActivityPose();

    /**
     * Returns the CameraViewActivityPose for the specified camera type or null if it is not
     * ready/available.
     */
    @Nullable
    CameraViewActivityPose getCameraViewActivityPose(
            @CameraViewActivityPose.CameraType int cameraType);

    /** Returns the PerceptionSpaceActivityPose for the Session. */
    @NonNull
    PerceptionSpaceActivityPose getPerceptionSpaceActivityPose();

    /**
     * Returns the entity that represents the ActivitySpace root.
     *
     * <p>SDK's factory methods are expected to use this entity as the default parent for all
     * content entities when no parent is specified.
     */
    // TODO: b/378680989 - Remove this method.
    @NonNull
    Entity getActivitySpaceRootImpl();

    /** Loads glTF Asset for the given asset name from the assets folder. */
    // Suppressed to allow CompletableFuture.
    @SuppressWarnings({"AndroidJdkLibsChecker", "AsyncSuffixFuture"})
    @Nullable
    ListenableFuture<GltfModelResource> loadGltfByAssetName(@NonNull String assetName);

    /**
     * Loads glTF Asset for the given asset name from the assets folder using the Split Engine
     * route. The future returned by this method will fire listeners on the UI thread if
     * Runnable::run is supplied.
     */
    @SuppressWarnings("AsyncSuffixFuture")
    @Nullable
    ListenableFuture<GltfModelResource> loadGltfByAssetNameSplitEngine(@NonNull String assetName);

    /** Loads an ExrImage for the given asset name from the assets folder. */
    // Suppressed to allow CompletableFuture.
    @SuppressWarnings({"AndroidJdkLibsChecker", "AsyncSuffixFuture"})
    @Nullable
    ListenableFuture<ExrImageResource> loadExrImageByAssetName(@NonNull String assetName);

    /**
     * A factory function to create a SceneCore GltfEntity. The parent may be the activity space or
     * GltfEntity in the scene.
     */
    @NonNull
    GltfEntity createGltfEntity(
            @NonNull Pose pose,
            @NonNull GltfModelResource loadedGltf,
            @Nullable Entity parentEntity);

    /** A factory function for an Entity which displays drawable surfaces. */
    @SuppressWarnings("LambdaLast")
    @NonNull
    StereoSurfaceEntity createStereoSurfaceEntity(
            @StereoSurfaceEntity.StereoMode int stereoMode,
            @NonNull StereoSurfaceEntity.CanvasShape canvasShape,
            @NonNull Pose pose,
            @NonNull Entity parentEntity);

    /** Return the Spatial Capabilities set that are currently supported by the platform. */
    @NonNull
    SpatialCapabilities getSpatialCapabilities();

    /**
     * Adds the given {@link Consumer} as a listener to be invoked when this Session's current
     * SpatialCapabilities change. {@link Consumer#accept(SpatialCapabilities)} will be invoked on
     * the given Executor.
     */
    void addSpatialCapabilitiesChangedListener(
            @NonNull Executor callbackExecutor, @NonNull Consumer<SpatialCapabilities> listener);

    /**
     * Releases the given {@link Consumer} from receiving updates when the Session's {@link
     * SpatialCapabilities} change.
     */
    void removeSpatialCapabilitiesChangedListener(@NonNull Consumer<SpatialCapabilities> listener);

    /**
     * If the primary Activity for this Session has focus, causes it to be placed in FullSpace Mode.
     * Otherwise, this call does nothing.
     */
    void requestFullSpaceMode();

    /**
     * If the primary Activity for this Session has focus, causes it to be placed in HomeSpace Mode.
     * Otherwise, this call does nothing.
     */
    void requestHomeSpaceMode();

    /**
     * A factory function to create a platform PanelEntity. The parent can be any entity.
     *
     * @param pose Initial pose of the panel.
     * @param view View inflating this panel.
     * @param surfaceDimensionsPx Dimensions for the underlying surface for the given view.
     * @param dimensions Size of the panel in meters.
     * @param name Name of the panel.
     * @param context Application Context.
     * @param parent Parent entity.
     */
    @NonNull
    PanelEntity createPanelEntity(
            @NonNull Pose pose,
            @NonNull View view,
            @NonNull PixelDimensions surfaceDimensionsPx,
            @NonNull Dimensions dimensions,
            @NonNull String name,
            @SuppressWarnings("ContextFirst") @NonNull Context context,
            @NonNull Entity parent);

    /** Get the PanelEntity associated with the main window for the Activity. */
    @NonNull
    PanelEntity getMainPanelEntity();

    /**
     * Factory function to create ActivityPanel to launch/move activity into.
     *
     * @param pose Initial pose of the panel.
     * @param windowBoundsPx Boundary for the window
     * @param name Name of the panel.
     * @param hostActivity Activity to host the panel.
     * @param parent Parent entity.
     */
    @NonNull
    ActivityPanelEntity createActivityPanelEntity(
            @NonNull Pose pose,
            @NonNull PixelDimensions windowBoundsPx,
            @NonNull String name,
            @NonNull Activity hostActivity,
            @NonNull Entity parent);

    /**
     * A factory function to create an Anchor entity.
     *
     * @param bounds Bounds for this Anchor.
     * @param planeType Orientation of the plane to which this anchor should attach.
     * @param planeSemantic Semantic type of the plane to which this anchor should attach.
     * @param searchTimeout How long to search for an anchor. If this is Duration.ZERO, this will
     *     search for an anchor indefinitely.
     */
    @NonNull
    AnchorEntity createAnchorEntity(
            @NonNull Dimensions bounds,
            @NonNull PlaneType planeType,
            @NonNull PlaneSemantic planeSemantic,
            @NonNull Duration searchTimeout);

    /**
     * A factory function to create an Anchor entity from a {@link androidx.xr.arcore.Anchor}.
     *
     * @param anchor The {@link androidx.xr.arcore.Anchor} to create the Anchor entity from.
     */
    @NonNull
    AnchorEntity createAnchorEntity(@NonNull Anchor anchor);

    /**
     * Unpersist an AnchorEntity. It will clean up the data in the storage that is required to
     * retrieve the anchor. Returns whether the anchor was successfully unpersisted.
     *
     * @param uuid UUID of the anchor to unpersist.
     */
    boolean unpersistAnchor(@NonNull UUID uuid);

    /**
     * A factory function to create a content-less entity. This entity is used as a connection point
     * for attaching children entities and managing them (i.e. setPose()) as a group.
     *
     * @param pose Initial pose of the entity.
     * @param name Name of the entity.
     * @param parent Parent entity.
     */
    @NonNull
    Entity createEntity(@NonNull Pose pose, @NonNull String name, @NonNull Entity parent);

    /**
     * Create an Interactable component.
     *
     * @param executor Executor to use for input callbacks.
     * @param listener [JxrPlatformAdapter.InputEventListener] for this component.
     * @return InteractableComponent instance.
     */
    @SuppressLint("ExecutorRegistration")
    @NonNull
    InteractableComponent createInteractableComponent(
            @NonNull Executor executor, @NonNull InputEventListener listener);

    /**
     * Create an instance of [MovableComponent]. This component allows the user to move the entity.
     *
     * @param systemMovable A [boolean] which causes the system to automatically apply transform
     *     updates to the entity in response to user interaction.
     * @param scaleInZ A [boolean] which tells the system to update the scale of the Entity as the
     *     user moves it closer and further away. This is mostly useful for Panel auto-rescaling
     *     with Distance
     * @param anchorPlacement AnchorPlacement information for when to anchor the entity.
     * @param shouldDisposeParentAnchor A [boolean] which tells the system to dispose of the parent
     *     anchor if that entity was created by the moveable component and is moved off of it.
     * @return [MovableComponent] instance.
     */
    @NonNull
    MovableComponent createMovableComponent(
            boolean systemMovable,
            boolean scaleInZ,
            @NonNull Set<AnchorPlacement> anchorPlacement,
            boolean shouldDisposeParentAnchor);

    /**
     * Creates an instance of an AnchorPlacement object.
     *
     * <p>This can be used in movable components to specify the anchor placement for the entity.
     *
     * @param planeTypeFilter A set of plane types to filter for.
     * @param planeSemanticFilter A set of plane semantics to filter for.
     * @return [AnchorPlacement] instance.
     */
    @NonNull
    AnchorPlacement createAnchorPlacementForPlanes(
            @NonNull Set<PlaneType> planeTypeFilter,
            @NonNull Set<PlaneSemantic> planeSemanticFilter);

    /**
     * Create an instance of [ResizableComponent]. This component allows the user to resize the
     * entity.
     *
     * @param minimumSize Minimum size constraint.
     * @param maximumSize Maximum size constraint.
     * @return [ResizableComponent] instance.
     */
    @NonNull
    ResizableComponent createResizableComponent(
            @NonNull Dimensions minimumSize, @NonNull Dimensions maximumSize);

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
     *     may be temporarily lost by the application for a variety of reasons and this callback
     *     will notify of when that happens.
     * @param inputListener Callback that will receive captured [InputEvent]s
     */
    @NonNull
    PointerCaptureComponent createPointerCaptureComponent(
            @NonNull Executor executor,
            @NonNull PointerCaptureComponent.StateListener stateListener,
            @NonNull InputEventListener inputListener);

    /**
     * A factory function to recreate an Anchor entity which was persisted in a previous session.
     *
     * @param uuid The UUID of the persisted anchor.
     * @param searchTimeout How long to search for an anchor. If this is Duration.ZERO, this will
     *     search for an anchor indefinitely.
     */
    @NonNull
    AnchorEntity createPersistedAnchorEntity(@NonNull UUID uuid, @NonNull Duration searchTimeout);

    /**
     * Sets the full space mode flag to the given {@link Bundle}.
     *
     * <p>The {@link Bundle} then could be used to launch an {@link Activity} with requesting to
     * enter full space mode through {@link Activity#startActivity}. If there's a bundle used for
     * customizing how the {@link Activity} should be started by {@link ActivityOptions.toBundle} or
     * {@link androidx.core.app.ActivityOptionsCompat.toBundle}, it's suggested to use the bundle to
     * call this method.
     *
     * <p>The flag will be ignored when no {@link Intent.FLAG_ACTIVITY_NEW_TASK} is set in the
     * bundle, or it is not started from a focused Activity context.
     *
     * <p>This flag is also ignored when the {@link android.window.PROPERTY_XR_ACTIVITY_START_MODE}
     * property is set to a value other than XR_ACTIVITY_START_MODE_UNDEFINED in the
     * AndroidManifest.xml file for the activity being launched.
     *
     * @param bundle the input bundle to set with the full space mode flag.
     * @return the input {@code bundle} with the full space mode flag set.
     */
    @NonNull
    Bundle setFullSpaceMode(@NonNull Bundle bundle);

    /**
     * Sets the inherit full space mode environment flag to the given {@link Bundle}.
     *
     * <p>The {@link Bundle} then could be used to launch an {@link Activity} with requesting to
     * enter full space mode while inherit the existing environment through {@link
     * Activity#startActivity}. If there's a bundle used for customizing how the {@link Activity}
     * should be started by {@link ActivityOptions.toBundle} or {@link
     * androidx.core.app.ActivityOptionsCompat.toBundle}, it's suggested to use the bundle to call
     * this method.
     *
     * <p>When launched, the activity will be in full space mode and also inherits the environment
     * from the launching activity. If the inherited environment needs to be animated, the launching
     * activity has to continue updating the environment even after the activity is put into the
     * stopped state.
     *
     * <p>The flag will be ignored when no {@link Intent.FLAG_ACTIVITY_NEW_TASK} is set in the
     * intent, or it is not started from a focused Activity context.
     *
     * <p>The flag will also be ignored when there is no environment to inherit or the activity has
     * its own environment set already.
     *
     * <p>This flag is ignored too when the {@link android.window.PROPERTY_XR_ACTIVITY_START_MODE}
     * property is set to a value other than XR_ACTIVITY_START_MODE_UNDEFINED in the
     * AndroidManifest.xml file for the activity being launched.
     *
     * <p>For security reasons, Z testing for the new activity is disabled, and the activity is
     * always drawn on top of the inherited environment. Because Z testing is disabled, the activity
     * should not spatialize itself.
     *
     * @param bundle the input bundle to set with the inherit full space mode environment flag.
     * @return the input {@code bundle} with the inherit full space mode flag set.
     */
    @NonNull
    Bundle setFullSpaceModeWithEnvironmentInherited(@NonNull Bundle bundle);

    /**
     * Sets a preferred main panel aspect ratio for home space mode.
     *
     * <p>The ratio is only applied to the activity. If the activity launches another activity in
     * the same task, the ratio is not applied to the new activity. Also, while the activity is in
     * full space mode, the preference is temporarily removed.
     *
     * <p>If the activity's current aspect ratio differs from the {@code preferredRatio}, the panel
     * is automatically resized. This resizing preserves the panel's area. To avoid runtime
     * resizing, consider specifying the desired aspect ratio in your {@code AndroidManifest.xml}.
     * This ensures your activity launches with the preferred aspect ratio from the start.
     *
     * @param activity the activity to set the preference.
     * @param preferredRatio the aspect ratio determined by taking the panel's width over its
     *     height. A value <= 0.0f means there are no preferences.
     */
    void setPreferredAspectRatio(@NonNull Activity activity, float preferredRatio);

    /** Starts the SceneCore renderer. */
    void startRenderer();

    /** Stops the SceneCore renderer. */
    void stopRenderer();

    /** Disposes of the resources used by the platform adapter. */
    void dispose();

    /** Type of plane based on orientation i.e. Horizontal or Vertical. */
    enum PlaneType {
        HORIZONTAL,
        VERTICAL,
        ANY
    }

    /** Semantic plane types. */
    enum PlaneSemantic {
        WALL,
        FLOOR,
        CEILING,
        TABLE,
        ANY
    }

    /** Base interface for all components. */
    interface Component {
        /**
         * Lifecycle event, called when component is attached to an Entity.
         *
         * @param entity Entity the component is attached to.
         * @return True if the component can attach to the given entity.
         */
        boolean onAttach(@NonNull Entity entity);

        /**
         * Lifecycle event, called when component is detached from an Entity.
         *
         * @param entity Entity the component detached from.
         */
        void onDetach(@NonNull Entity entity);
    }

    /** Component to enable input interactions. */
    interface InteractableComponent extends Component {}

    /** Component to enable a high level user movement affordance. */
    interface MovableComponent extends Component {
        /**
         * Modes for scaling the entity as the user moves it closer and further away. *
         *
         * <p>DEFAULT: The panel scales in the same way as home space mode.
         *
         * <p>DMM: The panel scales in a way that the user-perceived panel size never changes.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    ScaleWithDistanceMode.DEFAULT,
                    ScaleWithDistanceMode.DMM,
                })
        @interface ScaleWithDistanceMode {
            int DEFAULT = 3;
            int DMM = 2;
        }

        /** Returns the current scale with distance mode. */
        @ScaleWithDistanceMode
        int getScaleWithDistanceMode();

        /**
         * Sets the scale with distance mode.
         *
         * @param scaleWithDistanceMode The scale with distance mode to set
         */
        void setScaleWithDistanceMode(@ScaleWithDistanceMode int scaleWithDistanceMode);

        /** Sets the size of the interaction highlight extent. */
        void setSize(@NonNull Dimensions dimensions);

        /**
         * Adds the listener to the set of active listeners for the move events.
         *
         * <p>The listener is invoked on the provided executor. If the app intends to modify the UI
         * elements/views during the callback, the app should provide the thread executor that is
         * appropriate for the UI operations. For example, if the app is using the main thread to
         * render the UI, the app should provide the main thread (Looper.getMainLooper()) executor.
         * If the app is using a separate thread to render the UI, the app should provide the
         * executor for that thread.
         *
         * @param executor The executor to run the listener on.
         * @param moveEventListener The move event listener to set.
         */
        void addMoveEventListener(
                @NonNull Executor executor, @NonNull MoveEventListener moveEventListener);

        /**
         * Removes the listener from the set of active listeners for the move events.
         *
         * @param moveEventListener the move event listener to remove
         */
        void removeMoveEventListener(@NonNull MoveEventListener moveEventListener);
    }

    /**
     * Interface for an AnchorPlacement.
     *
     * <p>This is used to set possible conditions in which an entity with a MovableComponent can be
     * anchored. This can be set with createAnchorPlacementForPlanes.
     */
    interface AnchorPlacement {}

    /** Component to enable resize semantics. */
    interface ResizableComponent extends Component {
        /**
         * Sets the size of the entity.
         *
         * <p>The size of the entity is the size of the bounding box that contains the content of
         * the entity. The size of the content inside that bounding box is fully controlled by the
         * application.
         *
         * @param dimensions Dimensions for the Entity in meters.
         */
        void setSize(@NonNull Dimensions dimensions);

        /**
         * Sets the minimum size constraint for the entity.
         *
         * <p>The minimum size constraint is used to set constraints on how small the user can
         * resize the bounding box of the entity up to. The size of the content inside that bounding
         * box is fully controlled by the application.
         *
         * @param minSize Minimum size constraint for the Entity in meters.
         */
        void setMinimumSize(@NonNull Dimensions minSize);

        /**
         * Sets the maximum size constraint for the entity.
         *
         * <p>The maximum size constraint is used to set constraints on how large the user can
         * resize the bounding box of the entity up to. The size of the content inside that bounding
         * box is fully controlled by the application.
         *
         * @param maxSize Maximum size constraint for the Entity in meters.
         */
        void setMaximumSize(@NonNull Dimensions maxSize);

        /**
         * Sets the aspect ratio of the entity during resizing.
         *
         * <p>The aspect ratio is determined by taking the panel's width over its height. A value of
         * 0.0f (or negative) means there are no preferences.
         *
         * <p>This method does not immediately resize the entity. The new aspect ratio will be
         * applied the next time the user resizes the entity through the reform UI. During this
         * resize operation, the entity's current area will be preserved.
         *
         * <p>If a different resizing behavior is desired, such as fixing the width and adjusting
         * the height, the client can manually resize the entity to the preferred dimensions before
         * calling this method. No automatic resizing will occur when using the reform UI then.
         *
         * @param fixedAspectRatio Aspect ratio during resizing.
         */
        void setFixedAspectRatio(float fixedAspectRatio);

        /**
         * Sets whether or not content (including content of all child nodes) is auto-hidden during
         * resizing. Defaults to true.
         *
         * @param autoHideContent Whether or not content is auto-hidden during resizing.
         */
        void setAutoHideContent(boolean autoHideContent);

        /**
         * Sets whether the size of the ResizableComponent is automatically updated to match during
         * an ongoing resize (to match the proposed size as resize events are received). Defaults to
         * true.
         *
         * @param autoUpdateSize Whether or not the size of the ResizableComponent is automatically
         *     updated during resizing.
         */
        void setAutoUpdateSize(boolean autoUpdateSize);

        /**
         * Sets whether to force showing the resize overlay even when this entity is not being
         * resized. Defaults to false.
         *
         * @param show Whether or not to force show the resize overlay.
         */
        void setForceShowResizeOverlay(boolean show);

        /**
         * Adds the listener to the set of listeners that are invoked through the resize operation,
         * such as start, ongoing and end.
         *
         * <p>The listener is invoked on the provided executor. If the app intends to modify the UI
         * elements/views during the callback, the app should provide the thread executor that is
         * appropriate for the UI operations. For example, if the app is using the main thread to
         * render the UI, the app should provide the main thread (Looper.getMainLooper()) executor.
         * If the app is using a separate thread to render the UI, the app should provide the
         * executor for that thread.
         *
         * @param executor The executor to use for the listener callback.
         * @param resizeEventListener The listener to be invoked when a resize event occurs.
         */
        // TODO: b/361638845 - Mirror the Kotlin API for ResizeListener.
        void addResizeEventListener(
                @NonNull Executor executor, @NonNull ResizeEventListener resizeEventListener);

        /**
         * Removes the given listener from the set of listeners for the resize events.
         *
         * @param resizeEventListener The listener to be removed.
         */
        void removeResizeEventListener(@NonNull ResizeEventListener resizeEventListener);
    }

    /** Component to enable pointer capture. */
    interface PointerCaptureComponent extends Component {
        int POINTER_CAPTURE_STATE_PAUSED = 0;
        int POINTER_CAPTURE_STATE_ACTIVE = 1;
        int POINTER_CAPTURE_STATE_STOPPED = 2;

        /** The possible states of pointer capture. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    POINTER_CAPTURE_STATE_PAUSED,
                    POINTER_CAPTURE_STATE_ACTIVE,
                    POINTER_CAPTURE_STATE_STOPPED,
                })
        @interface PointerCaptureState {}

        /** Functional interface for receiving updates about the state of pointer capture. */
        interface StateListener {
            /**
             * Called when the state of pointer capture changes.
             *
             * @param newState The new state of pointer capture.
             */
            void onStateChanged(@PointerCaptureState int newState);
        }
    }

    /** Interface for a SceneCore resource. A resource represents a loadable resource. */
    interface Resource {}

    /**
     * Interface for an EXR resource. These HDR images can be used for image based lighting and
     * skyboxes.
     */
    interface ExrImageResource extends Resource {}

    /** Interface for a glTF resource. This can be used for creating glTF entities. */
    interface GltfModelResource extends Resource {}

    /** Interface for Input listener. */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @FunctionalInterface
    interface InputEventListener {
        /**
         * Called when an input event occurs.
         *
         * @param event The input event that occurred.
         */
        void onInputEvent(@NonNull InputEvent event);
    }

    /** Interface for MoveEvent listener. */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @FunctionalInterface
    interface MoveEventListener {
        /**
         * Called when a move event occurs.
         *
         * @param event The move event that occurred.
         */
        void onMoveEvent(@NonNull MoveEvent event);
    }

    /** Interface for ResizeEvent listener. */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @FunctionalInterface
    interface ResizeEventListener {
        /**
         * Called when a resize event occurs.
         *
         * @param event The resize event that occurred.
         */
        void onResizeEvent(@NonNull ResizeEvent event);
    }

    /** Interface for a SceneCore ActivityPose */
    interface ActivityPose {
        /** Returns the pose for this entity, relative to the activity space root. */
        @NonNull
        Pose getActivitySpacePose();

        // TODO: b/364303733 - Consider deprecating this method.
        /**
         * Returns the scale of this ActivityPose. For base ActivityPoses, the scale is (1,1,1). For
         * entities this returns the accumulated scale. This value includes the parent's scale, and
         * is similar to a ActivitySpace scale.
         *
         * @return Total [Vector3] scale applied to self and children.
         */
        @NonNull
        Vector3 getWorldSpaceScale();

        /**
         * Returns the scale of this WorldPose relative to the activity space. This returns the
         * accumulated scale which includes the parent's scale, but does not include the scale of
         * the activity space itself.
         *
         * @return Total [Vector3] scale applied to self and children relative to the activity
         *     space.
         */
        @NonNull
        Vector3 getActivitySpaceScale();

        /**
         * Returns a pose relative to this entity transformed into a pose relative to the
         * destination.
         *
         * @param pose A pose in this entity's local coordinate space.
         * @param destination The entity which the returned pose will be relative to.
         * @return The pose relative to the destination entity.
         */
        @NonNull
        Pose transformPoseTo(@NonNull Pose pose, @NonNull ActivityPose destination);
    }

    /** Interface for a SceneCore head ActivityPose. This is the position of the user's head. */
    interface HeadActivityPose extends ActivityPose {}

    /**
     * Interface for a SceneCore camera view ActivityPose. This is the position of a user's camera.
     *
     * <p>The camera's field of view can be retrieved from this CameraViewActivityPose.
     */
    interface CameraViewActivityPose extends ActivityPose {
        int CAMERA_TYPE_UNKNOWN = 0;
        int CAMERA_TYPE_LEFT_EYE = 1;
        int CAMERA_TYPE_RIGHT_EYE = 2;

        /** Returns the type of camera that this space represents. */
        @CameraType
        int getCameraType();

        /**
         * The angles (in radians) representing the sides of the view frustum. These are not
         * expected to change over the lifetime of the session but in rare cases may change due to
         * updated camera settings
         */
        class Fov {

            public final float angleLeft;
            public final float angleRight;
            public final float angleUp;
            public final float angleDown;

            public Fov(float angleLeft, float angleRight, float angleUp, float angleDown) {
                this.angleLeft = angleLeft;
                this.angleRight = angleRight;
                this.angleUp = angleUp;
                this.angleDown = angleDown;
            }
        }

        /** Returns the field of view for this camera. */
        @NonNull
        Fov getFov();

        /** Describes the type of camera that this space represents. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    CAMERA_TYPE_UNKNOWN,
                    CAMERA_TYPE_LEFT_EYE,
                    CAMERA_TYPE_RIGHT_EYE,
                })
        @interface CameraType {}
    }

    /**
     * Interface for the perception space ActivityPose. This is the origin of the space used by
     * ARCore for XR.
     */
    interface PerceptionSpaceActivityPose extends ActivityPose {}

    /** Interface for a SceneCore Entity */
    interface Entity extends ActivityPose {

        /** Returns the pose for this entity, relative to its parent. */
        @NonNull
        Pose getPose();

        /** Updates the pose (position and rotation) of the Entity relative to its parent. */
        void setPose(@NonNull Pose pose);

        /**
         * Returns the scale of this entity, relative to its parent. Note that this doesn't include
         * the parent's scale.
         *
         * @return Current [Vector3] scale applied to self and children.
         */
        @NonNull
        Vector3 getScale();

        /**
         * Sets the scale of this entity relative to its parent. This value will affect the
         * rendering of this Entity's children. As the scale increases, this will stretch the
         * content of the Entity.
         *
         * @param scale The [Vector3] scale factor from the parent.
         */
        void setScale(@NonNull Vector3 scale);

        /**
         * Add given Entity as child. The child Entity's pose will be relative to the pose of its
         * parent
         *
         * @param child The child entity.
         */
        void addChild(@NonNull Entity child);

        /** Sets the provided Entities to be children of the Entity. */
        void addChildren(@NonNull List<Entity> children);

        /** Returns the parent entity for this Entity. */
        @Nullable
        Entity getParent();

        /**
         * Sets the parent Entity for this Entity. The child Entity's pose will be relative to the
         * pose of its parent.
         *
         * @param parent The parent entity.
         */
        void setParent(@Nullable Entity parent);

        /** Sets context-text for this entity to be consumed by Accessibility systems. */
        void setContentDescription(@NonNull String text);

        /** Returns the all child entities of this Entity. */
        @NonNull
        List<Entity> getChildren();

        /**
         * Sets the size for the given Entity.
         *
         * @param dimensions Dimensions for the Entity in meters.
         */
        void setSize(@NonNull Dimensions dimensions);

        /** Returns the set alpha transparency level for this Entity. */
        float getAlpha();

        /**
         * Sets the alpha transparency for the given Entity.
         *
         * @param alpha Alpha transparency level for the Entity.
         */
        void setAlpha(float alpha);

        /** Returns the total alpha transparency level for this Entity. */
        float getActivitySpaceAlpha();

        /**
         * Sets the local hidden state of this Entity. When true, this Entity and all descendants
         * will not be rendered in the scene. When the hidden state is false, an entity will be
         * rendered if its ancestors are not hidden.
         *
         * @param hidden The new local hidden state of this Entity.
         */
        void setHidden(boolean hidden);

        /**
         * Returns the hidden status of this Entity.
         *
         * @param includeParents Whether to include the hidden status of parents in the returned
         *     value.
         * @return If includeParents is true, the returned value will be true if this Entity or any
         *     of its ancestors is hidden. If includeParents is false, the local hidden state is
         *     returned. Regardless of the local hidden state, an entity will not be rendered if any
         *     of its ancestors are hidden.
         */
        boolean isHidden(boolean includeParents);

        /**
         * Adds the listener to the set of active input listeners, for input events targeted to this
         * entity or its child entities.
         *
         * @param executor The executor to run the listener on.
         * @param listener The input event listener to add.
         */
        void addInputEventListener(
                @NonNull Executor executor, @NonNull InputEventListener listener);

        /** Removes the given listener from the set of active input listeners. */
        void removeInputEventListener(@NonNull InputEventListener listener);

        /**
         * Dispose any system resources held by this entity, and transitively calls dispose() on all
         * the children. Once disposed, Entity shouldn't be used again.
         */
        void dispose();

        /**
         * Add these components to entity.
         *
         * @param component Component to add to the Entity.
         * @return True if the given component is added to the Entity.
         */
        boolean addComponent(@NonNull Component component);

        /**
         * Remove the given component from the entity.
         *
         * @param component Component to remove from the entity.
         */
        void removeComponent(@NonNull Component component);

        /** Remove all components from this entity. */
        void removeAllComponents();
    }

    /**
     * Interface for updating the background image/geometry and passthrough settings.
     *
     * <p>The application can set either / both a skybox and a glTF for geometry, then toggle their
     * visibility by enabling or disabling passthrough. The skybox and geometry will be remembered
     * across passthrough mode changes.
     */
    interface SpatialEnvironment {

        /** A class that represents the user's preferred spatial environment. */
        class SpatialEnvironmentPreference {
            /**
             * The preferred geometry for the environment based on a pre-loaded glTF model. If null,
             * there will be no geometry
             */
            @Nullable public final GltfModelResource geometry;

            /**
             * The preferred skybox for the environment based on a pre-loaded EXR Image. If null, it
             * will be all black.
             */
            @Nullable public final ExrImageResource skybox;

            public SpatialEnvironmentPreference(
                    @Nullable ExrImageResource skybox, @Nullable GltfModelResource geometry) {
                this.skybox = skybox;
                this.geometry = geometry;
            }

            @Override
            public boolean equals(@Nullable Object o) {
                if (o == this) {
                    return true;
                }
                if (o instanceof SpatialEnvironmentPreference) {
                    SpatialEnvironmentPreference other = (SpatialEnvironmentPreference) o;
                    return Objects.equals(other.skybox, skybox)
                            && Objects.equals(other.geometry, geometry);
                }
                return false;
            }

            @Override
            public int hashCode() {
                return Objects.hash(skybox, geometry);
            }
        }

        /**
         * Sets the preference for passthrough state by requesting a change in passthrough opacity.
         *
         * <p>Passthrough visibility cannot be set directly to on/off modes. Instead, a desired
         * passthrough opacity value between 0.0f and 1.0f can be requested which will dictate which
         * mode is used. A passthrough opacity within 0.01f of 0.0f will disable passthrough, and
         * will be returned as 0.0f by [getPassthroughOpacityPreference]. An opacity value within
         * 0.01f of 1.0f will enable full passthrough and it will be returned as 1.0f by
         * [getPassthroughOpacityPreference]. Any other value in the range will result in a
         * semi-transparent passthrough.
         *
         * <p>Requesting to set passthrough opacity to a value that is not in the range of 0.0f to
         * 1.0f will result in the value getting clamped to 0.0f or 1.0f depending on which one is
         * closer.
         *
         * <p>If the value is set to null, the opacity will be managed by the system.
         *
         * <p>Requests to change opacity are only immediately attempted to be honored if the
         * activity has the [SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL] capability.
         * When the request is honored, this returns [SetPassthroughOpacityPreferenceChangeApplied].
         * When the activity does not have the capability to control the passthrough state, this
         * returns [SetPassthroughOpacityPreferenceChangePending] to indicate that the application
         * passthrough opacity preference has been set and is pending to be automatically applied
         * when the app regains capabilities to control passthrough state.
         *
         * <p>When passthrough state changes, whether due to this request succeeding or due to any
         * other system or user initiated change, [OnPassthroughOpacityChangedListener] will be
         * notified.
         */
        @CanIgnoreReturnValue
        @NonNull
        public SetPassthroughOpacityPreferenceResult setPassthroughOpacityPreference(
                @SuppressWarnings("AutoBoxing") @Nullable Float passthroughOpacityPreference);

        /**
         * Gets the current passthrough opacity value between 0 and 1 where 0.0f means no
         * passthrough, and 1.0f means full passthrough.
         *
         * <p>This value can be overwritten by user-enabled or system-enabled passthrough and will
         * not always match the opacity value returned by [getPassthroughOpacityPreference].
         */
        float getCurrentPassthroughOpacity();

        /**
         * Gets the last passthrough opacity requested through [setPassthroughOpacityPreference].
         *
         * <p>This may be different from the actual current state returned by
         * [getCurrentPassthroughOpacity], but it should be applied as soon as the
         * [SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL] capability is gained.
         * Defaults to null, if [setPassthroughOpacityPreference] was never called.
         *
         * <p>If set to null, the passthrough opacity will default to the user preference managed
         * through the system.
         */
        @SuppressWarnings("AutoBoxing")
        @Nullable
        Float getPassthroughOpacityPreference();

        /**
         * Notifies an application when the passthrough state changes, such as when the application
         * enters or exits passthrough or when the passthrough opacity changes. This [listener] will
         * be called on the Application's UI thread.
         */
        void addOnPassthroughOpacityChangedListener(@NonNull Consumer<Float> listener);

        /** Remove a listener previously added by [addOnPassthroughOpacityChangedListener]. */
        void removeOnPassthroughOpacityChangedListener(@NonNull Consumer<Float> listener);

        /**
         * Returns true if the environment set by [setSpatialEnvironmentPreference] is active.
         *
         * <p>Spatial environment preference set through [setSpatialEnvironmentPreference] are shown
         * when this is true, but passthrough or other objects in the scene could partially or
         * totally occlude them. When this is false, the default system environment will be active
         * instead.
         */
        boolean isSpatialEnvironmentPreferenceActive();

        /**
         * Sets the preferred spatial environment for the application.
         *
         * <p>Note that this method only sets a preference and does not cause an immediate change
         * unless [isSpatialEnvironmentPreferenceActive] is already true. Once the device enters a
         * state where the XR background can be changed and the
         * [SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENTS] capability is available, the
         * preferred spatial environment for the application will be automatically displayed.
         *
         * <p>Setting the preference to null will disable the preferred spatial environment for the
         * application, meaning the default system environment will be displayed instead.
         *
         * <p>If the given [SpatialEnvironmentPreference] is not null, but all of its properties are
         * null, then the spatial environment will consist of a black skybox and no geometry
         * [isSpatialEnvironmentPreferenceActive] is true.
         *
         * <p>Changes to the Environment state will be notified via the
         * [OnSpatialEnvironmentChangedListener].
         */
        @NonNull
        @CanIgnoreReturnValue
        SetSpatialEnvironmentPreferenceResult setSpatialEnvironmentPreference(
                @Nullable SpatialEnvironmentPreference preference);

        /**
         * Gets the preferred spatial environment for the application.
         *
         * <p>The returned value is always what was most recently supplied to
         * [setSpatialEnvironmentPreference], or null if no preference has been set.
         *
         * <p>See [isSpatialEnvironmentPreferenceActive] or the
         * [OnSpatialEnvironmentChangedListener] events to know when this preference becomes active.
         */
        @Nullable
        SpatialEnvironmentPreference getSpatialEnvironmentPreference();

        /**
         * Notifies an application whether or not the preferred spatial environment for the
         * application is active.
         *
         * <p>The environment will try to transition to the application environment when a non-null
         * preference is set through [setSpatialEnvironmentPreference] and the application has the
         * [SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENTS] capability. The environment
         * preferences will otherwise not be active.
         *
         * <p>The listener consumes a boolean value that is true if the environment preference is
         * active when the listener is notified.
         *
         * <p>This listener will be invoked on the Application's UI thread.
         */
        void addOnSpatialEnvironmentChangedListener(@NonNull Consumer<Boolean> listener);

        /** Remove a listener previously added by [addOnSpatialEnvironmentChangedListener]. */
        void removeOnSpatialEnvironmentChangedListener(@NonNull Consumer<Boolean> listener);

        /** Result values for calls to SpatialEnvironment.setPassthroughOpacityPreference */
        enum SetPassthroughOpacityPreferenceResult {
            /**
             * The call to [setPassthroughOpacityPreference] succeeded and should now be visible.
             */
            CHANGE_APPLIED,

            /**
             * The preference has been set, but will be applied only when the
             * [SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL] is acquired
             */
            CHANGE_PENDING,
        }

        /** Result values for calls to SpatialEnvironment.setSpatialEnvironmentPreference */
        enum SetSpatialEnvironmentPreferenceResult {
            /**
             * The call to [setSpatialEnvironmentPreference] succeeded and should now be visible.
             */
            CHANGE_APPLIED,

            /**
             * The call to [setSpatialEnvironmentPreference] successfully applied the preference,
             * but it is not immediately visible due to requesting a state change while the activity
             * does not have the [SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENTS]
             * capability to control the app environment state. The preference was still set and
             * will be applied when the capability is gained.
             */
            CHANGE_PENDING,
        }
    }

    /** Interface for a SceneCore Entity that only logs the pose. */
    interface LoggingEntity extends Entity {}

    /** Interface for a system-controlled SceneCore Entity that defines its own coordinate space. */
    interface SystemSpaceEntity extends Entity {
        /**
         * Registers a listener to be called when the underlying space has moved or changed.
         *
         * @param listener The listener to register if non-null, else stops listening if null.
         * @param executor The executor to run the listener on. Defaults to SceneCore executor if
         *     null.
         */
        void setOnSpaceUpdatedListener(
                @Nullable OnSpaceUpdatedListener listener, @Nullable Executor executor);

        /** Interface for a listener which receives changes to the underlying space. */
        @SuppressWarnings("AndroidJdkLibsChecker")
        @FunctionalInterface
        interface OnSpaceUpdatedListener {
            /** Called by the system when the underlying space has changed. */
            void onSpaceUpdated();
        }
    }

    /**
     * Interface for a SceneCore activity space. There is one activity space and it is the ancestor
     * for all elements in the scene. The activity space does not have a parent.
     */
    interface ActivitySpace extends SystemSpaceEntity {

        /** Returns the bounds of this ActivitySpace. */
        @NonNull
        Dimensions getBounds();

        /**
         * Adds a listener to be called when the bounds of the primary Activity change. If the same
         * listener is added multiple times, it will only fire each event on time.
         *
         * @param listener The listener to register.
         */
        @SuppressWarnings("ExecutorRegistration")
        void addOnBoundsChangedListener(@NonNull OnBoundsChangedListener listener);

        /**
         * Removes a listener to be called when the bounds of the primary Activity change. If the
         * given listener was not added, this call does nothing.
         *
         * @param listener The listener to unregister.
         */
        void removeOnBoundsChangedListener(@NonNull OnBoundsChangedListener listener);

        /**
         * Interface for a listener which receives changes to the bounds of the primary Activity.
         */
        interface OnBoundsChangedListener {
            // Is called by the system when the bounds of the primary Activity change
            /**
             * Called by the system when the bounds of the primary Activity change.
             *
             * @param bounds The new bounds of the primary Activity in Meters
             */
            void onBoundsChanged(@NonNull Dimensions bounds);
        }
    }

    /** Interface for a SceneCore [GltfEntity]. */
    interface GltfEntity extends Entity {
        // TODO: b/362368652 - Add an OnAnimationFinished() Listener interface
        //                     Add a getAnimationTimeRemaining() interface

        /** Specifies the current animation state of the [GltfEntity]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({AnimationState.PLAYING, AnimationState.STOPPED})
        @interface AnimationState {
            int PLAYING = 0;
            int STOPPED = 1;
        }

        /**
         * Starts the animation with the given name.
         *
         * @param animationName The name of the animation to start. If null is supplied, will play
         *     the first animation found in the glTF.
         * @param loop Whether the animation should loop.
         */
        void startAnimation(boolean loop, @Nullable String animationName);

        /** Stops the animation of the glTF entity. */
        void stopAnimation();

        /** Returns the current animation state of the glTF entity. */
        @AnimationState
        int getAnimationState();
    }

    /** Interface for a SceneCore Panel entity */
    interface PanelEntity extends Entity {
        /**
         * Returns the dimensions of the view underlying this PanelEntity.
         *
         * @return The current [PixelDimensions] of the underlying surface.
         */
        @NonNull
        PixelDimensions getPixelDimensions();

        /**
         * Sets the pixel (not Dp) dimensions of the view underlying this PanelEntity. Calling this
         * might cause the layout of the Panel contents to change. Updating this will not cause the
         * scale or pixel density to change.
         *
         * @param dimensions The [PixelDimensions] of the underlying surface to set.
         */
        void setPixelDimensions(@NonNull PixelDimensions dimensions);

        /**
         * Sets a corner radius on all four corners of this PanelEntity.
         *
         * @param value Corner radius in meters.
         * @throws IllegalArgumentException if radius is <= 0.0f.
         */
        void setCornerRadius(float value);

        /** Gets the corner radius of this PanelEntity in meters. Has a default value of 0. */
        float getCornerRadius();

        /**
         * Gets the number of pixels per meter for this panel. This value reflects changes to scale,
         * including parent scale.
         *
         * @return Vector3 scale applied to pixels within the Panel. (Z will be 0)
         */
        @NonNull
        Vector3 getPixelDensity();

        /**
         * Returns the spatial size of this Panel in meters. This includes any scaling applied to
         * this panel by itself or its parents, which might be set via changes to setScale.
         *
         * @return [Dimensions] size of this panel in meters. (Z will be 0)
         */
        @NonNull
        Dimensions getSize();
    }

    /** Interface for a SceneCore ActivityPanel entity. */
    interface ActivityPanelEntity extends PanelEntity {
        /**
         * Launches the given activity into the panel.
         *
         * @param intent Intent to launch the activity.
         * @param bundle Bundle to pass to the activity, can be null.
         */
        void launchActivity(@NonNull Intent intent, @Nullable Bundle bundle);

        /**
         * Moves the given activity into the panel.
         *
         * @param activity Activity to move into the ActivityPanel.
         */
        void moveActivity(@NonNull Activity activity);
    }

    /** Interface for a surface which images can be rendered into. */
    interface StereoSurfaceEntity extends Entity {

        /** Represents the shape of the spatial canvas which the surface is texture mapped to. */
        public static interface CanvasShape {

            @NonNull
            public abstract Dimensions getDimensions();

            /**
             * A 2D rectangle-shaped canvas. Width and height are represented in the local spatial
             * coordinate system of the entity. (0,0,0) is the center of the canvas.
             */
            public static final class Quad implements CanvasShape {
                public final float width;
                public final float height;

                public Quad(float width, float height) {
                    this.width = width;
                    this.height = height;
                }

                @Override
                @NonNull
                public Dimensions getDimensions() {
                    return new Dimensions(width, height, 0);
                }
            }

            /**
             * A sphere-shaped canvas. Radius is represented in the local spatial coordinate system
             * of the entity. (0,0,0) is the center of the sphere.
             */
            public static final class Vr360Sphere implements CanvasShape {
                public final float radius;

                public Vr360Sphere(float radius) {
                    this.radius = radius;
                }

                @Override
                @NonNull
                public Dimensions getDimensions() {
                    return new Dimensions(radius * 2, radius * 2, radius * 2);
                }
            }

            /**
             * A hemisphere-shaped canvas. Radius is represented in the local spatial coordinate
             * system of the entity. (0,0,0) is the center of the base of the hemisphere.
             */
            public static final class Vr180Hemisphere implements CanvasShape {
                public final float radius;

                public Vr180Hemisphere(float radius) {
                    this.radius = radius;
                }

                @Override
                @NonNull
                public Dimensions getDimensions() {
                    // Note that the depth dimension is only a single radius
                    return new Dimensions(radius * 2, radius * 2, radius);
                }
            }
        }

        /**
         * Selects the view configuration for the surface. MONO creates a surface contains a single
         * view. SIDE_BY_SIDE means the surface is split in half with two views. The first half of
         * the surface maps to the left eye and the second half mapping to the right eye.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({StereoMode.MONO, StereoMode.TOP_BOTTOM, StereoMode.SIDE_BY_SIDE})
        @interface StereoMode {
            // Each eye will see the entire surface (no separation)
            int MONO = 0;
            // The [top, bottom] halves of the surface will map to [left, right] eyes
            int TOP_BOTTOM = 1;
            // The [left, right] halves of the surface will map to [left, right] eyes
            int SIDE_BY_SIDE = 2;
        }

        /**
         * Specifies how the surface content will be routed for stereo viewing. Applications must
         * render into the surface in accordance with what is specified here in order for the
         * compositor to correctly produce a stereoscopic view to the user.
         *
         * @param mode An int StereoMode
         */
        void setStereoMode(@StereoMode int mode);

        /**
         * Specifies the shape of the spatial canvas which the surface is texture mapped to.
         *
         * @param canvasShape A concrete instance of [CanvasShape].
         */
        void setCanvasShape(@NonNull CanvasShape canvasShape);

        /**
         * Retrieves the StereoMode for this Entity.
         *
         * @return An int StereoMode
         */
        @StereoMode
        int getStereoMode();

        /**
         * Retrieves the dimensions of the "spatial canvas" which the surface is mapped to. These
         * values are not impacted by scale.
         *
         * @return The canvas [Dimensions].
         */
        @NonNull
        Dimensions getDimensions();

        /**
         * Retrieves the surface that the Entity will display. The app can write into this surface
         * however it wants, i.e. MediaPlayer, ExoPlayer, or custom rendering.
         *
         * @return an Android [Surface]
         */
        @NonNull
        Surface getSurface();
    }

    /** Interface for Anchor entity. */
    interface AnchorEntity extends SystemSpaceEntity {
        /** Returns the current state of the anchor synchronously. */
        @NonNull
        State getState();

        /** Registers a listener to be called when the state of the anchor changes. */
        @SuppressWarnings("ExecutorRegistration")
        void setOnStateChangedListener(@Nullable OnStateChangedListener onStateChangedListener);

        /**
         * Persists the anchor. If the query is sent to perception service successful returns an
         * UUID, which could be used retrieve the anchor. Otherwise, return null.
         */
        @Nullable
        UUID persist();

        /** Returns the current persist state of the anchor synchronously. */
        @NonNull
        PersistState getPersistState();

        /** Returns the native pointer of the anchor. */
        // TODO(b/373711152) : Remove this method once the Jetpack XR Runtime API migration is done.
        long nativePointer();

        /** Registers a listener to be called when the persist state of the anchor changes. */
        @SuppressWarnings({"ExecutorRegistration", "PairedRegistration"})
        void registerPersistStateChangeListener(
                @NonNull PersistStateChangeListener persistStateChangeListener);

        /** Specifies the current tracking state of the Anchor. */
        enum State {
            /**
             * An UNANCHORED state could mean that the perception stack hasn't found an anchor for
             * this Space, that it has lost tracking.
             */
            UNANCHORED,
            /**
             * The ANCHORED state means that this Anchor is being actively tracked and updated by
             * the perception stack. The application should expect children to maintain their
             * relative positioning to the system's best understanding of a pose in the real world.
             */
            ANCHORED,
            /**
             * The AnchorEntity timed out while searching for an underlying anchor. This it is not
             * possible to recover the AnchorEntity.
             */
            TIMED_OUT,
            /**
             * The ERROR state means that something has gone wrong and this AnchorSpace is invalid
             * without the possibility of recovery.
             */
            ERROR,
            /**
             * The PERMISSIONS_NOT_GRANTED state means that the permissions required to use the
             * anchor i.e. SCENE_UNDERSTANDING have not been granted by the user.
             */
            PERMISSIONS_NOT_GRANTED,
        }

        /** Specifies the current persistence state of the Anchor. */
        enum PersistState {
            /** The anchor hasn't been requested to persist. */
            PERSIST_NOT_REQUESTED,
            /** The anchor is requested to persist but hasn't been persisted yet. */
            PERSIST_PENDING,
            /** The anchor is persisted successfully. */
            PERSISTED,
        }

        /** Interface for listening to Anchor state changes. */
        interface OnStateChangedListener {
            /**
             * Called when the state of the anchor changes.
             *
             * @param newState The new state of the anchor.
             */
            void onStateChanged(@NonNull State newState);
        }

        /** Interface for listening to Anchor persist state changes. */
        interface PersistStateChangeListener {
            /**
             * Called when the persist state of the anchor changes.
             *
             * @param newPersistState The new persist state of the anchor.
             */
            void onPersistStateChanged(@NonNull PersistState newPersistState);
        }
    }

    /** The dimensions of a UI element in pixels. These are always two dimensional. */
    class PixelDimensions {
        public final int width;
        public final int height;

        public PixelDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return super.toString() + ": w " + width + " x h " + height;
        }
    }

    /** The dimensions of a UI element in meters. */
    class Dimensions {
        // TODO: b/332588978 - Add a TypeAlias for Meters here.
        @SuppressWarnings("MutableBareField")
        public float width;

        @SuppressWarnings("MutableBareField")
        public float height;

        @SuppressWarnings("MutableBareField")
        public float depth;

        public Dimensions(float width, float height, float depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        @Override
        public String toString() {
            return super.toString() + ": w " + width + " x h " + height + " x d " + depth;
        }
    }

    /** Ray in 3D Cartesian space. */
    class Ray {
        @NonNull public final Vector3 origin;
        @NonNull public final Vector3 direction;

        public Ray(@NonNull Vector3 origin, @NonNull Vector3 direction) {
            this.origin = origin;
            this.direction = direction;
        }
    }

    /** MoveEvent for SceneCore Platform. */
    class MoveEvent {
        // TODO: b/350370142 - Use public getter/setter interfaces instead of public fields.
        public static final int MOVE_STATE_START = 1;
        public static final int MOVE_STATE_ONGOING = 2;
        public static final int MOVE_STATE_END = 3;

        /** State of the move action. */
        @MoveState public final int moveState;

        /** Initial ray origin and direction in activity space. */
        @NonNull public final Ray initialInputRay;

        /** Current ray origin and direction in activity space. */
        @NonNull public final Ray currentInputRay;

        /** Previous pose of the entity, relative to its parent. */
        @NonNull public final Pose previousPose;

        /** Current pose of the entity, relative to its parent. */
        @NonNull public final Pose currentPose;

        /** Previous scale of the entity. */
        @NonNull public final Vector3 previousScale;

        /** Current scale of the entity. */
        @NonNull public final Vector3 currentScale;

        /** Initial Parent of the entity at the start of the move. */
        @NonNull public final Entity initialParent;

        /** Updates parent of the entity at the end of the move or null if not updated. */
        @Nullable public final Entity updatedParent;

        /**
         * Reports an entity that was disposed and needs to be removed from the sdk EntityManager.
         */
        @Nullable public final Entity disposedEntity;

        public MoveEvent(
                int moveState,
                @NonNull Ray initialInputRay,
                @NonNull Ray currentInputRay,
                @NonNull Pose previousPose,
                @NonNull Pose currentPose,
                @NonNull Vector3 previousScale,
                @NonNull Vector3 currentScale,
                @NonNull Entity initialParent,
                @Nullable Entity updatedParent,
                @Nullable Entity disposedEntity) {
            this.moveState = moveState;
            this.initialInputRay = initialInputRay;
            this.currentInputRay = currentInputRay;
            this.previousPose = previousPose;
            this.currentPose = currentPose;
            this.previousScale = previousScale;
            this.currentScale = currentScale;
            this.initialParent = initialParent;
            this.updatedParent = updatedParent;
            this.disposedEntity = disposedEntity;
        }

        /** States of the Move action. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    MOVE_STATE_START,
                    MOVE_STATE_ONGOING,
                    MOVE_STATE_END,
                })
        public @interface MoveState {}
    }

    /** ResizeEvent for SceneCore Platform. */
    class ResizeEvent {
        public static final int RESIZE_STATE_UNKNOWN = 0;
        public static final int RESIZE_STATE_START = 1;
        public static final int RESIZE_STATE_ONGOING = 2;
        public static final int RESIZE_STATE_END = 3;

        /**
         * Proposed (width, height, depth) size in meters. The resize event listener must use this
         * proposed size to resize the content.
         */
        @NonNull public final Dimensions newSize;

        /** Current state of the Resize action. */
        @ResizeState public final int resizeState;

        public ResizeEvent(@ResizeState int resizeState, @NonNull Dimensions newSize) {
            this.resizeState = resizeState;
            this.newSize = newSize;
        }

        /** States of the Resize action. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    RESIZE_STATE_UNKNOWN,
                    RESIZE_STATE_START,
                    RESIZE_STATE_ONGOING,
                    RESIZE_STATE_END,
                })
        public @interface ResizeState {}
    }

    /** InputEvent for SceneCore Platform. */
    class InputEvent {
        /**
         * There's a possibility of ABI mismatch here when the concrete platformAdapter starts
         * receiving input events with an updated field, such as if a newer source or pointer type
         * has been added to the underlying platform OS. We need to perform a version check when the
         * platformAdapter is constructed to ensure that the application doesn't receive anything it
         * wasn't compiled against.
         */
        // TODO: b/343468347 - Implement a version check for xr extensions when creating the
        // concrete
        // platform adapter.

        /** Unknown source. */
        public static final int SOURCE_UNKNOWN = 0;

        /**
         * Event is based on the user's head. Ray origin is at average between eyes, pushed out to
         * the near clipping plane for both eyes and points in direction head is facing. Action
         * state is based on volume up button being depressed.
         *
         * <p>Events from this source are considered sensitive and hover events are never sent.
         */
        public static final int SOURCE_HEAD = 1;

        /**
         * Event is based on (one of) the user's controller(s). Ray origin and direction are for a
         * controller aim pose as defined by OpenXR. (<a
         * href="https://registry.khronos.org/OpenXR/specs/1.1/html/xrspec.html#semantic-paths-standard-pose-identifiers">...</a>)
         * Action state is based on the primary button on the controller, usually the bottom-most
         * face button.
         */
        public static final int SOURCE_CONTROLLER = 2;

        /**
         * Event is based on one of the user's hands. Ray is a hand aim pose, with origin between
         * thumb and forefinger and points in direction based on hand orientation. Action state is
         * based on a pinch gesture.
         */
        public static final int SOURCE_HANDS = 3;

        /**
         * Event is based on a 2D mouse pointing device. Ray origin behaves the same as for
         * DEVICE_TYPE_HEAD and points in direction based on mouse movement. During a drag, the ray
         * origin moves approximating hand motion. The scrollwheel moves the ray away from / towards
         * the user. Action state is based on the primary mouse button.
         */
        public static final int SOURCE_MOUSE = 4;

        /**
         * Event is based on a mix of the head, eyes, and hands. Ray origin is at average between
         * eyes and points in direction based on a mix of eye gaze direction and hand motion. During
         * a two-handed zoom/rotate gesture, left/right pointer events will be issued; otherwise,
         * default events are issued based on the gaze ray. Action state is based on if the user has
         * done a pinch gesture or not.
         *
         * <p>Events from this source are considered sensitive and hover events are never sent.
         */
        public static final int SOURCE_GAZE_AND_GESTURE = 5;

        /**
         * Default pointer type for the source (no handedness). Occurs for SOURCE_UNKNOWN,
         * SOURCE_HEAD, SOURCE_MOUSE, and SOURCE_GAZE_AND_GESTURE.
         */
        public static final int POINTER_TYPE_DEFAULT = 0;

        /**
         * Left hand / controller pointer. Occurs for SOURCE_CONTROLLER, SOURCE_HANDS, and
         * SOURCE_GAZE_AND_GESTURE.
         */
        public static final int POINTER_TYPE_LEFT = 1;

        /**
         * Right hand / controller pointer. Occurs for SOURCE_CONTROLLER, SOURCE_HANDS, and
         * SOURCE_GAZE_AND_GESTURE.
         */
        public static final int POINTER_TYPE_RIGHT = 2;

        /** The primary action button or gesture was just pressed / started. */
        public static final int ACTION_DOWN = 0;

        /**
         * The primary action button or gesture was just released / stopped. The hit info represents
         * the node that was originally hit (ie, as provided in the ACTION_DOWN event).
         */
        public static final int ACTION_UP = 1;

        /**
         * The primary action button or gesture was pressed/active in the previous event, and is
         * still pressed/active. The hit info represents the node that was originally hit (ie, as
         * provided in the ACTION_DOWN event). The hit position may be null if the pointer is no
         * longer hitting that node.
         */
        public static final int ACTION_MOVE = 2;

        /**
         * While the primary action button or gesture was held, the pointer was disabled. This
         * happens if you are using controllers and the battery runs out, or if you are using a
         * source that transitions to a new pointer type, eg SOURCE_GAZE_AND_GESTURE.
         */
        public static final int ACTION_CANCEL = 3;

        /**
         * The primary action button or gesture is not pressed, and the pointer ray continued to hit
         * the same node. The hit info represents the node that was hit (may be null if pointer
         * capture is enabled).
         *
         * <p>Hover input events are never provided for sensitive source types.
         */
        public static final int ACTION_HOVER_MOVE = 4;

        /**
         * The primary action button or gesture is not pressed, and the pointer ray started to hit a
         * new node. The hit info represents the node that is being hit (may be null if pointer
         * capture is enabled).
         *
         * <p>Hover input events are never provided for sensitive source types.
         */
        public static final int ACTION_HOVER_ENTER = 5;

        /**
         * The primary action button or gesture is not pressed, and the pointer ray stopped hitting
         * the node that it was previously hitting. The hit info represents the node that was being
         * hit (may be null if pointer capture is enabled).
         *
         * <p>Hover input events are never provided for sensitive source types.
         */
        public static final int ACTION_HOVER_EXIT = 6;

        @SuppressWarnings("MutableBareField")
        @Source
        public int source;

        @SuppressWarnings("MutableBareField")
        @PointerType
        public int pointerType;

        /** The time this event occurred, in the android.os.SystemClock#uptimeMillis time base. */
        @SuppressWarnings({
            "GoodTime",
            "MutableBareField"
        }) // This field mirrors the XR Extensions InputEvent.
        public long timestamp;

        /**
         * The origin of the ray, in the receiver's activity space. Will be zero if the source is
         * not ray-based (eg, direct touch).
         */
        @SuppressWarnings("MutableBareField")
        @NonNull
        public Vector3 origin;

        /**
         * A point indicating the direction the ray is pointing in, in the receiver's activity
         * space. The ray is a vector starting at the origin point and passing through the direction
         * point.
         */
        @SuppressWarnings("MutableBareField")
        @NonNull
        public Vector3 direction;

        /** Info about the hit result of the ray. */
        public static class HitInfo {
            /**
             * The entity that was hit by the input ray.
             *
             * <p>ACTION_MOVE, ACTION_UP, and ACTION_CANCEL events will report the same node as was
             * hit during the initial ACTION_DOWN.
             */
            @Nullable public final Entity inputEntity;

            /**
             * The position of the hit in the receiver's activity space.
             *
             * <p>All events may report the current ray's hit position. This can be null if there no
             * longer is a collision between the ray and the input node (eg, during a drag event).
             */
            @Nullable public final Vector3 hitPosition;

            /**
             * The matrix transforming activity space coordinates into the hit entity's local
             * coordinate space.
             */
            @NonNull public final Matrix4 transform;

            /**
             * @param inputEntity the entity that was hit by the input ray.
             * @param hitPosition the position of the hit in the receiver's activity space.
             * @param transform the matrix transforming activity space coordinates into the hit
             *     entity's local coordinate space.
             */
            public HitInfo(
                    @Nullable Entity inputEntity,
                    @Nullable Vector3 hitPosition,
                    @NonNull Matrix4 transform) {
                this.inputEntity = inputEntity;
                this.hitPosition = hitPosition;
                this.transform = transform;
            }
        }

        /** Returns the current action associated with this input event. */
        @SuppressWarnings("MutableBareField")
        public int action;

        /**
         * Info on the first entity (closest to the ray origin) that was hit by the input ray, if
         * any. This info will be null if no Entity was hit.
         */
        @SuppressWarnings("MutableBareField")
        @Nullable
        public HitInfo hitInfo;

        /** Info on the second entity for the same task that was hit by the input ray, if any. */
        @SuppressWarnings("MutableBareField")
        @Nullable
        public HitInfo secondaryHitInfo;

        @SuppressWarnings("GoodTime")
        public InputEvent(
                @Source int source,
                @PointerType int pointerType,
                long timestamp,
                @NonNull Vector3 origin,
                @NonNull Vector3 direction,
                @Action int action,
                @Nullable HitInfo hitInfo,
                @Nullable HitInfo secondaryHitInfo) {
            this.source = source;
            this.pointerType = pointerType;
            this.timestamp = timestamp;
            this.origin = origin;
            this.direction = direction;
            this.action = action;
            this.hitInfo = hitInfo;
            this.secondaryHitInfo = secondaryHitInfo;
        }

        /** Describes the hardware source of the event. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    SOURCE_UNKNOWN,
                    SOURCE_HEAD,
                    SOURCE_CONTROLLER,
                    SOURCE_HANDS,
                    SOURCE_MOUSE,
                    SOURCE_GAZE_AND_GESTURE,
                })
        public @interface Source {}

        /** The type of the individual pointer. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    POINTER_TYPE_DEFAULT, // Default for the source.
                    POINTER_TYPE_LEFT, // Left hand/controller.
                    POINTER_TYPE_RIGHT, // Right hand/controller.
                })
        public @interface PointerType {}

        /**
         * Actions similar to Android's MotionEvent actions: <a
         * href="https://developer.android.com/reference/android/view/MotionEvent"></a> for keeping
         * track of a sequence of events on the same target, e.g., * HOVER_ENTER -> HOVER_MOVE ->
         * HOVER_EXIT * DOWN -> MOVE -> UP
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    ACTION_DOWN,
                    ACTION_UP,
                    ACTION_MOVE,
                    ACTION_CANCEL,
                    ACTION_HOVER_MOVE,
                    ACTION_HOVER_ENTER,
                    ACTION_HOVER_EXIT,
                })
        public @interface Action {}
    }

    /** Spatial Capabilities for SceneCore Platform. */
    class SpatialCapabilities {

        /** The activity can spatialize itself by e.g. adding a spatial panel. */
        public static final int SPATIAL_CAPABILITY_UI = 1 << 0;

        /** The activity can create 3D contents. */
        public static final int SPATIAL_CAPABILITY_3D_CONTENT = 1 << 1;

        /** The activity can enable or disable passthrough. */
        public static final int SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL = 1 << 2;

        /** The activity can set its own environment. */
        public static final int SPATIAL_CAPABILITY_APP_ENVIRONMENT = 1 << 3;

        /** The activity can use spatial audio. */
        public static final int SPATIAL_CAPABILITY_SPATIAL_AUDIO = 1 << 4;

        /** The activity can spatially embed another activity. */
        public static final int SPATIAL_CAPABILITY_EMBED_ACTIVITY = 1 << 5;

        /** Spatial Capabilities for SceneCore Platform. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                flag = true,
                value = {
                    SPATIAL_CAPABILITY_UI,
                    SPATIAL_CAPABILITY_3D_CONTENT,
                    SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL,
                    SPATIAL_CAPABILITY_APP_ENVIRONMENT,
                    SPATIAL_CAPABILITY_SPATIAL_AUDIO,
                    SPATIAL_CAPABILITY_EMBED_ACTIVITY,
                })
        public @interface SpatialCapability {}

        /** The set of capabilities enabled for the platform. */
        @SuppressWarnings("MutableBareField")
        @SpatialCapability
        public int capabilities;

        public SpatialCapabilities(@SpatialCapability int capabilities) {
            this.capabilities = capabilities;
        }

        /**
         * Returns true if the given capability is enabled.
         *
         * @param capability The capability to check.
         * @return True if the capability is enabled, false otherwise.
         */
        public boolean hasCapability(@SpatialCapability int capability) {
            return (capabilities & capability) != 0;
        }
    }

    /** Interface for a SceneCore SoundPoolExtensionsWrapper. */
    interface SoundPoolExtensionsWrapper {

        /**
         * Plays a sound as a point source.
         *
         * @param soundPool The SoundPool to use.
         * @param soundId The ID of the sound to play.
         * @param attributes The PointSourceAttributes to use.
         * @param volume The volume of the sound.
         * @param priority The priority of the sound.
         * @param loop Whether to loop the sound.
         * @param rate The playback rate of the sound.
         * @return The result of the play operation.
         */
        int play(
                @NonNull SoundPool soundPool,
                int soundId,
                @NonNull PointSourceAttributes attributes,
                float volume,
                int priority,
                int loop,
                float rate);

        /**
         * Plays a sound as a sound field.
         *
         * @param soundPool The SoundPool to use.
         * @param soundId The ID of the sound to play.
         * @param attributes The SoundFieldAttributes to use.
         * @param volume The volume of the sound.
         * @param priority The priority of the sound.
         * @param loop Whether to loop the sound.
         * @param rate The playback rate of the sound.
         * @return The result of the play operation.
         */
        int play(
                @NonNull SoundPool soundPool,
                int soundId,
                @NonNull SoundFieldAttributes attributes,
                float volume,
                int priority,
                int loop,
                float rate);

        /**
         * Returns the spatial source type of the sound.
         *
         * @param soundPool The SoundPool to use.
         * @param streamId The stream ID of the sound.
         * @return The spatial source type of the sound.
         */
        @SpatializerConstants.SourceType
        int getSpatialSourceType(@NonNull SoundPool soundPool, int streamId);
    }

    /** Interface for a SceneCore AudioTrackExtensionsWrapper */
    interface AudioTrackExtensionsWrapper {

        /**
         * Returns the PointSourceAttributes of the AudioTrack.
         *
         * @param track The AudioTrack to get the PointSourceAttributes from.
         * @return The PointSourceAttributes of the AudioTrack.
         */
        @Nullable
        PointSourceAttributes getPointSourceAttributes(@NonNull AudioTrack track);

        /**
         * Returns the SoundFieldAttributes of the AudioTrack.
         *
         * @param track The AudioTrack to get the SoundFieldAttributes from.
         * @return The SoundFieldAttributes of the AudioTrack.
         */
        @Nullable
        SoundFieldAttributes getSoundFieldAttributes(@NonNull AudioTrack track);

        /**
         * Returns the spatial source type of the AudioTrack.
         *
         * @param track The AudioTrack to get the spatial source type from.
         * @return The spatial source type of the AudioTrack.
         */
        @SpatializerConstants.SourceType
        int getSpatialSourceType(@NonNull AudioTrack track);

        /**
         * Sets the PointSourceAttributes of the AudioTrack.
         *
         * @param builder The AudioTrack.Builder to set the PointSourceAttributes on.
         * @param attributes The PointSourceAttributes to set.
         * @return The AudioTrack.Builder with the PointSourceAttributes set.
         */
        @NonNull
        AudioTrack.Builder setPointSourceAttributes(
                @NonNull AudioTrack.Builder builder, @NonNull PointSourceAttributes attributes);

        /**
         * Sets the SoundFieldAttributes of the AudioTrack.
         *
         * @param builder The AudioTrack.Builder to set the SoundFieldAttributes on.
         * @param attributes The SoundFieldAttributes to set.
         * @return The AudioTrack.Builder with the SoundFieldAttributes set.
         */
        @NonNull
        AudioTrack.Builder setSoundFieldAttributes(
                @NonNull AudioTrack.Builder builder, @NonNull SoundFieldAttributes attributes);
    }

    /** Interface for a SceneCore MediaPlayerExtensionsWrapper */
    interface MediaPlayerExtensionsWrapper {

        /**
         * Sets the PointSourceAttributes of the MediaPlayer.
         *
         * @param mediaPlayer The MediaPlayer to set the PointSourceAttributes on.
         * @param attributes The PointSourceAttributes to set.
         */
        void setPointSourceAttributes(
                @NonNull MediaPlayer mediaPlayer, @NonNull PointSourceAttributes attributes);

        /**
         * Sets the SoundFieldAttributes of the MediaPlayer.
         *
         * @param mediaPlayer The MediaPlayer to set the SoundFieldAttributes on.
         * @param attributes The SoundFieldAttributes to set.
         */
        void setSoundFieldAttributes(
                @NonNull MediaPlayer mediaPlayer, @NonNull SoundFieldAttributes attributes);
    }

    /** Represents a SceneCore PointSourceAttributes */
    class PointSourceAttributes {
        private final Entity mEntity;

        public PointSourceAttributes(@NonNull Entity entity) {
            this.mEntity = entity;
        }

        /** Gets the SceneCore {@link Entity} for this instance. */
        @NonNull
        public Entity getEntity() {
            return this.mEntity;
        }
    }

    /** Represents a SceneCore SoundFieldAttributes */
    class SoundFieldAttributes {

        @SpatializerConstants.AmbisonicsOrder private final int mAmbisonicsOrder;

        public SoundFieldAttributes(int ambisonicsOrder) {
            this.mAmbisonicsOrder = ambisonicsOrder;
        }

        public int getAmbisonicsOrder() {
            return mAmbisonicsOrder;
        }
    }

    /** Contains the constants used to spatialize audio in SceneCore. */
    final class SpatializerConstants {

        private SpatializerConstants() {}

        /** Used to set the Ambisonics order of a [SoundFieldAttributes]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    AMBISONICS_ORDER_FIRST_ORDER,
                    AMBISONICS_ORDER_SECOND_ORDER,
                    AMBISONICS_ORDER_THIRD_ORDER,
                })
        public @interface AmbisonicsOrder {}

        /** Specifies spatial rendering using First Order Ambisonics */
        public static final int AMBISONICS_ORDER_FIRST_ORDER = 0;

        /** Specifies spatial rendering using Second Order Ambisonics */
        public static final int AMBISONICS_ORDER_SECOND_ORDER = 1;

        /** Specifies spatial rendering using Third Order Ambisonics */
        public static final int AMBISONICS_ORDER_THIRD_ORDER = 2;

        /** Represents the type of spatialization for an audio source. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    SOURCE_TYPE_BYPASS,
                    SOURCE_TYPE_POINT_SOURCE,
                    SOURCE_TYPE_SOUND_FIELD,
                })
        public @interface SourceType {}

        /** The sound source has not been spatialized with the Spatial Audio SDK. */
        public static final int SOURCE_TYPE_BYPASS = 0;

        /** The sound source has been spatialized as a 3D point source. */
        public static final int SOURCE_TYPE_POINT_SOURCE = 1;

        /** The sound source is an ambisonics sound field. */
        public static final int SOURCE_TYPE_SOUND_FIELD = 2;
    }

    /** Returns a [SoundPoolExtensionsWrapper] instance. */
    @NonNull
    SoundPoolExtensionsWrapper getSoundPoolExtensionsWrapper();

    /** Returns an [AudioTrackExtensionssWrapper] instance. */
    @NonNull
    AudioTrackExtensionsWrapper getAudioTrackExtensionsWrapper();

    /** Returns a [MediaPlayerExtensionsWrapper] instance. */
    @NonNull
    MediaPlayerExtensionsWrapper getMediaPlayerExtensionsWrapper();
}
