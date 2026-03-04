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

package androidx.xr.scenecore.spatial.core;

import static androidx.xr.scenecore.spatial.core.RuntimeUtils.getPositionFromTransform;
import static androidx.xr.scenecore.spatial.core.RuntimeUtils.getRotationFromTransform;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Pair;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.runtime.ActivityPanelEntity;
import androidx.xr.scenecore.runtime.ActivitySpace;
import androidx.xr.scenecore.runtime.AnchorEntity;
import androidx.xr.scenecore.runtime.AnchorPlacement;
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper;
import androidx.xr.scenecore.runtime.BoundsComponent;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.GltfEntity;
import androidx.xr.scenecore.runtime.GltfFeature;
import androidx.xr.scenecore.runtime.InputEventListener;
import androidx.xr.scenecore.runtime.InteractableComponent;
import androidx.xr.scenecore.runtime.LoggingEntity;
import androidx.xr.scenecore.runtime.MediaPlayerExtensionsWrapper;
import androidx.xr.scenecore.runtime.MovableComponent;
import androidx.xr.scenecore.runtime.PanelEntity;
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose;
import androidx.xr.scenecore.runtime.PixelDimensions;
import androidx.xr.scenecore.runtime.PlaneSemantic;
import androidx.xr.scenecore.runtime.PlaneType;
import androidx.xr.scenecore.runtime.PointerCaptureComponent;
import androidx.xr.scenecore.runtime.RenderingEntityFactory;
import androidx.xr.scenecore.runtime.ResizableComponent;
import androidx.xr.scenecore.runtime.ScenePose;
import androidx.xr.scenecore.runtime.SceneRuntime;
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.SpatialCapabilities;
import androidx.xr.scenecore.runtime.SpatialEnvironment;
import androidx.xr.scenecore.runtime.SpatialModeChangeListener;
import androidx.xr.scenecore.runtime.SpatialPointerComponent;
import androidx.xr.scenecore.runtime.SpatialVisibility;
import androidx.xr.scenecore.runtime.SubspaceNodeEntity;
import androidx.xr.scenecore.runtime.SurfaceEntity;
import androidx.xr.scenecore.runtime.SurfaceFeature;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransaction;
import com.android.extensions.xr.space.ActivityPanel;
import com.android.extensions.xr.space.ActivityPanelLaunchParameters;
import com.android.extensions.xr.space.SpatialState;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Implementation of [SceneRuntime] for devices that support the [Feature.SPATIAL] system feature.
 */
// Suppress BanSynchronizedMethods for onSpatialStateChanged().
// Suppress BanConcurrentHashMap for mSpatialCapabilitiesChangedListeners since XR minSdk is 24.
@SuppressWarnings({"BanSynchronizedMethods", "BanConcurrentHashMap"})
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SpatialSceneRuntime implements SceneRuntime, RenderingEntityFactory {
    private static final String GUARDIAN_CONSENT_GRANTED = "guardian_consent_granted";
    private static final String TOGGLE_GUARDIAN = "toggle_guardian";
    private final ScheduledExecutorService mExecutor;
    private final XrExtensions mExtensions;
    private final Node mSceneRootNode;
    private final Node mTaskWindowLeashNode;
    private final EntityManager mEntityManager;
    private final SpatialEnvironmentImpl mEnvironment;
    private final SoundPoolExtensionsWrapper mSoundPoolExtensionsWrapper;
    private final AudioTrackExtensionsWrapper mAudioTrackExtensionsWrapper;
    private final MediaPlayerExtensionsWrapper mMediaPlayerExtensionsWrapper;
    private final Map<Consumer<SpatialCapabilities>, Executor>
            mSpatialCapabilitiesChangedListeners = new ConcurrentHashMap<>();
    private final Map<Consumer<PixelDimensions>, Executor> mPerceivedResolutionChangedListeners =
            new ConcurrentHashMap<>();
    private final Map<Consumer<Boolean>, Executor> mBoundaryConsentListeners =
            new ConcurrentHashMap<>();
    // TODO b/373481538: remove lazy initialization once XR Extensions bug is fixed. This will allow
    // us to remove the lazySpatialStateProvider instance and pass the spatialState directly.
    private final AtomicReference<SpatialState> mSpatialState = new AtomicReference<>(null);
    // Returns the currently-known spatial state, or fetches it from the extensions if it has never
    // been set. The spatial state is kept updated in the SpatialStateCallback.
    private final Supplier<SpatialState> mLazySpatialStateProvider;
    private final ActivitySpaceImpl mActivitySpace;

    /** Returns the PerceptionSpaceScenePose for the Session. */
    private final PerceptionSpaceScenePoseImpl mPerceptionSpaceScenePose;

    private final PanelEntity mMainPanelEntity;
    private final AtomicBoolean mIsBoundaryConsentGrantedCache;
    private final int mSpatialApiVersion;
    @VisibleForTesting boolean mIsExtensionVisibilityStateCallbackRegistered = false;
    @VisibleForTesting Closeable mKeyEntityTransformCloseable;
    private @Nullable Activity mActivity;
    private boolean mIsDestroyed;
    private @Nullable Pair<Executor, Consumer<SpatialVisibility>> mSpatialVisibilityHandler;
    private @Nullable SpatialModeChangeListener mSpatialModeChangeListener;
    private @Nullable ContentObserver mBoundaryConsentObserver;
    private @Nullable Entity mKeyEntity = null;

    private SpatialSceneRuntime(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            @NonNull XrExtensions extensions,
            @NonNull EntityManager entityManager,
            @NonNull Node sceneRootNode,
            @NonNull Node taskWindowLeashNode) {
        mActivity = activity;
        mExecutor = executor;
        mExtensions = extensions;
        mSceneRootNode = sceneRootNode;
        mTaskWindowLeashNode = taskWindowLeashNode;
        mEntityManager = entityManager;

        mLazySpatialStateProvider =
                () ->
                        mSpatialState.updateAndGet(
                                oldSpatialState -> {
                                    if (oldSpatialState == null) {
                                        oldSpatialState = mExtensions.getSpatialState(activity);
                                    }
                                    return oldSpatialState;
                                });
        setSpatialStateCallback();

        mSoundPoolExtensionsWrapper =
                new SoundPoolExtensionsWrapperImpl(
                        extensions.getXrSpatialAudioExtensions().getSoundPoolExtensions());
        mAudioTrackExtensionsWrapper =
                new AudioTrackExtensionsWrapperImpl(
                        extensions.getXrSpatialAudioExtensions().getAudioTrackExtensions(),
                        entityManager);
        mMediaPlayerExtensionsWrapper =
                new MediaPlayerExtensionsWrapperImpl(
                        extensions.getXrSpatialAudioExtensions().getMediaPlayerExtensions());

        mEnvironment =
                new SpatialEnvironmentImpl(
                        activity, extensions, sceneRootNode, mLazySpatialStateProvider);

        mActivitySpace =
                new ActivitySpaceImpl(
                        sceneRootNode,
                        activity,
                        extensions,
                        entityManager,
                        mLazySpatialStateProvider,
                        executor);
        mEntityManager.addSystemSpaceActivityPose(mActivitySpace);
        mPerceptionSpaceScenePose = new PerceptionSpaceScenePoseImpl(mActivitySpace);
        mEntityManager.addSystemSpaceActivityPose(mPerceptionSpaceScenePose);
        mMainPanelEntity =
                new MainPanelEntityImpl(
                        activity, taskWindowLeashNode, extensions, entityManager, executor);
        mMainPanelEntity.setParent(mActivitySpace);
        // Initialize the boundary consent cache and register the listener.
        mIsBoundaryConsentGrantedCache = new AtomicBoolean(calculateBoundaryConsentState());
        registerBoundaryConsentStateListener();
        mSpatialApiVersion = new SpatialCoreApiVersionProvider().getSpatialApiVersion();
    }

    static @NonNull SpatialSceneRuntime create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            @NonNull XrExtensions extensions,
            @NonNull EntityManager entityManager) {
        return create(
                activity,
                executor,
                extensions,
                entityManager,
                /* sceneRootNode= */ extensions.createNode(),
                /* taskWindowLeashNode= */ extensions.createNode());
    }

    public static @NonNull SpatialSceneRuntime create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            @NonNull Node sceneRootNode,
            @NonNull Node taskWindowLeashNode) {
        return create(
                activity,
                executor,
                Objects.requireNonNull(XrExtensionsProvider.getXrExtensions()),
                new EntityManager(),
                /* sceneRootNode= */ sceneRootNode,
                /* taskWindowLeashNode= */ taskWindowLeashNode);
    }

    static @NonNull SpatialSceneRuntime create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            @NonNull XrExtensions extensions,
            @NonNull EntityManager entityManager,
            @NonNull Node sceneRootNode,
            @NonNull Node taskWindowLeashNode) {
        // TODO: b/376934871 - Check async results.
        extensions.attachSpatialScene(
                activity, sceneRootNode, taskWindowLeashNode, executor, (result) -> {});
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction
                    .setName(sceneRootNode, "SpatialSceneAndActivitySpaceRootNode")
                    .setParent(taskWindowLeashNode, sceneRootNode)
                    .setName(taskWindowLeashNode, "MainPanelAndTaskWindowLeashNode")
                    .apply();
        }

        return new SpatialSceneRuntime(
                activity, executor, extensions, entityManager, sceneRootNode, taskWindowLeashNode);
    }

    /** Create a new @c SpatialSceneRuntime. */
    public static @NonNull SpatialSceneRuntime create(
            @NonNull Activity activity, @NonNull ScheduledExecutorService executor) {
        return create(
                activity,
                executor,
                Objects.requireNonNull(XrExtensionsProvider.getXrExtensions()),
                new EntityManager());
    }

    @Override
    public void destroy() {
        if (mIsDestroyed) {
            return;
        }
        mEnvironment.dispose();
        clearKeyEntitySubscription(false);
        mSpatialModeChangeListener = null;
        mExtensions.clearSpatialStateCallback(mActivity);

        unregisterBoundaryConsentStateListener();
        mBoundaryConsentListeners.clear();

        clearSpatialVisibilityChangedListener();
        mPerceivedResolutionChangedListeners.clear();
        // This will trigger clearing the callback from XrExtensions if it was registered
        updateExtensionsVisibilityCallback();

        // TODO: b/376934871 - Check async results.
        mExtensions.detachSpatialScene(mActivity, Runnable::run, (result) -> {});
        mActivity = null;
        mEntityManager.getAllEntities().forEach(Entity::dispose);
        mEntityManager.clear();
        mIsDestroyed = true;
    }

    @VisibleForTesting
    @NonNull Node getSceneRootNode() {
        return mSceneRootNode;
    }

    @VisibleForTesting
    @NonNull Node getTaskWindowLeashNode() {
        return mTaskWindowLeashNode;
    }

    @Override
    public @NonNull SpatialCapabilities getSpatialCapabilities() {
        return RuntimeUtils.convertSpatialCapabilities(
                mLazySpatialStateProvider.get().getSpatialCapabilities());
    }

    @Override
    public @NonNull ActivitySpace getActivitySpace() {
        return mActivitySpace;
    }

    @Override
    public @NonNull ScenePose getScenePoseFromPerceptionPose(@NonNull Pose perceptionPose) {
        return new OpenXrScenePose((ActivitySpaceImpl) getActivitySpace(), perceptionPose);
    }

    @Override
    public @NonNull PerceptionSpaceScenePose getPerceptionSpaceActivityPose() {
        return mPerceptionSpaceScenePose;
    }

    @Override
    public @NonNull SpatialEnvironment getSpatialEnvironment() {
        return mEnvironment;
    }

    @Override
    public @Nullable SpatialModeChangeListener getSpatialModeChangeListener() {
        return mSpatialModeChangeListener;
    }

    @Override
    public void setSpatialModeChangeListener(
            @Nullable SpatialModeChangeListener spatialModeChangeListener) {
        mSpatialModeChangeListener = spatialModeChangeListener;
        mActivitySpace.setSpatialModeChangeListener(spatialModeChangeListener);
    }

    @Override
    public @NonNull SoundPoolExtensionsWrapper getSoundPoolExtensionsWrapper() {
        return mSoundPoolExtensionsWrapper;
    }

    @Override
    public @NonNull AudioTrackExtensionsWrapper getAudioTrackExtensionsWrapper() {
        return mAudioTrackExtensionsWrapper;
    }

    @Override
    public @NonNull MediaPlayerExtensionsWrapper getMediaPlayerExtensionsWrapper() {
        return mMediaPlayerExtensionsWrapper;
    }

    @Override
    public @NonNull PanelEntity createPanelEntity(
            @NonNull Context context,
            @NonNull Pose pose,
            @NonNull View view,
            @NonNull Dimensions dimensions,
            @NonNull String name,
            @Nullable Entity parent) {

        Node node = mExtensions.createNode();
        PanelEntity panelEntity =
                new PanelEntityImpl(
                        context,
                        node,
                        view,
                        mExtensions,
                        mEntityManager,
                        dimensions,
                        name,
                        mExecutor);
        panelEntity.setParent(parent);
        panelEntity.setPose(pose, Space.PARENT);
        return panelEntity;
    }

    @Override
    public @NonNull PanelEntity createPanelEntity(
            @NonNull Context context,
            @NonNull Pose pose,
            @NonNull View view,
            @NonNull PixelDimensions pixelDimensions,
            @NonNull String name,
            @Nullable Entity parent) {

        Node node = mExtensions.createNode();
        PanelEntity panelEntity =
                new PanelEntityImpl(
                        context,
                        node,
                        view,
                        mExtensions,
                        mEntityManager,
                        pixelDimensions,
                        name,
                        mExecutor);
        panelEntity.setParent(parent);
        panelEntity.setPose(pose, Space.PARENT);
        return panelEntity;
    }

    @Override
    public @NonNull PanelEntity getMainPanelEntity() {
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setVisibility(mTaskWindowLeashNode, true).apply();
        }
        return mMainPanelEntity;
    }

    @Override
    public @NonNull ActivityPanelEntity createActivityPanelEntity(
            @NonNull Pose pose,
            @NonNull PixelDimensions windowBoundsPx,
            @NonNull String name,
            @NonNull Activity hostActivity,
            @Nullable Entity parent) {

        // TODO(b/352630140): Move this into a static factory method of ActivityPanelEntityImpl.
        Rect windowBoundsRect = new Rect(0, 0, windowBoundsPx.width, windowBoundsPx.height);
        ActivityPanel activityPanel =
                mExtensions.createActivityPanel(
                        hostActivity, new ActivityPanelLaunchParameters(windowBoundsRect));

        activityPanel.setWindowBounds(windowBoundsRect);
        ActivityPanelEntityImpl activityPanelEntity =
                new ActivityPanelEntityImpl(
                        hostActivity,
                        activityPanel.getNode(),
                        name,
                        mExtensions,
                        mEntityManager,
                        activityPanel,
                        windowBoundsPx,
                        mExecutor);
        activityPanelEntity.setParent(parent);
        activityPanelEntity.setPose(pose, Space.PARENT);
        return activityPanelEntity;
    }

    @Override
    public @NonNull AnchorEntity createAnchorEntity() {
        Node node = mExtensions.createNode();
        return AnchorEntityImpl.create(
                mActivity, node, mActivitySpace, mExtensions, mEntityManager, mExecutor);
    }

    @Override
    @NonNull
    public GltfEntity createGltfEntity(
            @NonNull GltfFeature feature, @NonNull Pose pose, @Nullable Entity parentEntity) {
        GltfEntity entity =
                new GltfEntityImpl(
                        mActivity, feature, parentEntity, mExtensions, mEntityManager, mExecutor);
        entity.setPose(pose, Space.PARENT);
        return entity;
    }

    @Override
    public @NonNull SurfaceEntity createSurfaceEntity(
            @NonNull SurfaceFeature feature, @NonNull Pose pose, @Nullable Entity parentEntity) {
        SurfaceEntity entity =
                new SurfaceEntityImpl(
                        mActivity, feature, parentEntity, mExtensions, mEntityManager, mExecutor);
        entity.setPose(pose, Space.PARENT);
        return entity;
    }

    @NonNull
    public SubspaceNodeEntity createSubspaceNodeEntity(
            @NonNull Node node, @NonNull Dimensions size) {
        SubspaceNodeEntity entity =
                new SubspaceNodeEntityImpl(mActivity, mExtensions, node, mEntityManager, mExecutor);
        entity.setSize(size);
        return entity;
    }

    @Override
    public @NonNull Entity createEntity(
            @NonNull Pose pose, @Nullable String name, @Nullable Entity parent) {
        Node node = mExtensions.createNode();
        if (name != null) {
            try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
                transaction.setName(node, name).apply();
            }
        }

        // This entity is used to back SceneCore's Entity.
        Entity entity =
                new AndroidXrEntity(mActivity, node, mExtensions, mEntityManager, mExecutor) {};
        entity.setParent(parent);
        entity.setPose(pose, Space.PARENT);
        return entity;
    }

    @Override
    @Deprecated
    public @NonNull Entity createGroupEntity(
            @NonNull Pose pose, @NonNull String name, @Nullable Entity parent) {
        return createEntity(pose, name, parent);
    }

    @Override
    public @NonNull LoggingEntity createLoggingEntity(@NonNull Pose pose) {
        LoggingEntityImpl entity = new LoggingEntityImpl(mActivity);
        entity.setPose(pose, Space.PARENT);
        return entity;
    }

    // Note that this is called on the Activity's UI thread so we should be careful to not block it.
    // It is synchronized because we assume this.spatialState cannot be updated elsewhere during the
    // execution of this method.
    @VisibleForTesting
    synchronized void onSpatialStateChanged(@NonNull SpatialState newSpatialState) {
        SpatialState previousSpatialState = mSpatialState.getAndSet(newSpatialState);
        boolean spatialCapabilitiesChanged =
                previousSpatialState == null
                        || !newSpatialState
                                .getSpatialCapabilities()
                                .equals(previousSpatialState.getSpatialCapabilities());

        boolean hasBoundsChanged =
                previousSpatialState == null
                        || !newSpatialState.getBounds().equals(previousSpatialState.getBounds());

        EnumSet<SpatialEnvironmentImpl.ChangedSpatialStates> changedSpatialStates =
                mEnvironment.setSpatialState(newSpatialState);
        boolean environmentVisibilityChanged =
                changedSpatialStates.contains(
                        SpatialEnvironmentImpl.ChangedSpatialStates.ENVIRONMENT_CHANGED);
        boolean passthroughVisibilityChanged =
                changedSpatialStates.contains(
                        SpatialEnvironmentImpl.ChangedSpatialStates.PASSTHROUGH_CHANGED);

        // Fire the state change events only after all the states have been updated.
        if (environmentVisibilityChanged) {
            mEnvironment.fireOnSpatialEnvironmentChangedEvent();
        }
        if (passthroughVisibilityChanged) {
            mEnvironment.firePassthroughOpacityChangedEvent();
        }

        // Get the scene parent transform and update the activity space.
        if (newSpatialState.getSceneParentTransform() != null) {
            mActivitySpace.handleOriginUpdate(
                    RuntimeUtils.getMatrix(newSpatialState.getSceneParentTransform()));
        }

        if (spatialCapabilitiesChanged) {
            SpatialCapabilities spatialCapabilities =
                    RuntimeUtils.convertSpatialCapabilities(
                            newSpatialState.getSpatialCapabilities());

            mSpatialCapabilitiesChangedListeners.forEach(
                    (listener, executor) ->
                            executor.execute(() -> listener.accept(spatialCapabilities)));
        }

        if (hasBoundsChanged) {
            mActivitySpace.onBoundsChanged(newSpatialState.getBounds());
        }
    }

    private void setSpatialStateCallback() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mExtensions.setSpatialStateCallback(
                mActivity, mainHandler::post, this::onSpatialStateChanged);
    }

    @Override
    public void addSpatialCapabilitiesChangedListener(
            @NonNull Executor executor, @NonNull Consumer<SpatialCapabilities> listener) {
        mSpatialCapabilitiesChangedListeners.put(listener, executor);
    }

    @Override
    public void removeSpatialCapabilitiesChangedListener(
            @NonNull Consumer<SpatialCapabilities> listener) {
        mSpatialCapabilitiesChangedListeners.remove(listener);
    }

    @Override
    public void setSpatialVisibilityChangedListener(
            @NonNull Executor callbackExecutor, @NonNull Consumer<SpatialVisibility> listener) {
        mSpatialVisibilityHandler = new Pair<>(callbackExecutor, listener);
        updateExtensionsVisibilityCallback();
    }

    @Override
    public void clearSpatialVisibilityChangedListener() {
        mSpatialVisibilityHandler = null;
        updateExtensionsVisibilityCallback();
    }

    @Override
    public void addPerceivedResolutionChangedListener(
            @NonNull Executor callbackExecutor, @NonNull Consumer<PixelDimensions> listener) {
        mPerceivedResolutionChangedListeners.put(listener, callbackExecutor);
        updateExtensionsVisibilityCallback();
    }

    @Override
    public void removePerceivedResolutionChangedListener(
            @NonNull Consumer<PixelDimensions> listener) {
        mPerceivedResolutionChangedListeners.remove(listener);
        updateExtensionsVisibilityCallback();
    }

    private synchronized void updateExtensionsVisibilityCallback() {
        boolean shouldHaveCallback =
                mSpatialVisibilityHandler != null
                        || !mPerceivedResolutionChangedListeners.isEmpty();

        if (shouldHaveCallback && !mIsExtensionVisibilityStateCallbackRegistered) {
            // Register the combined callback
            try {
                mExtensions.setVisibilityStateCallback(
                        mActivity,
                        mExecutor,
                        // Executor for the combined callback itself
                        (com.android.extensions.xr.space.VisibilityState visibilityStateEvent) -> {
                            // Dispatch to SpatialVisibility listener
                            if (mSpatialVisibilityHandler != null) {
                                SpatialVisibility jxrSpatialVisibility =
                                        RuntimeUtils.convertSpatialVisibility(
                                                visibilityStateEvent.getVisibility());
                                mSpatialVisibilityHandler.first.execute(
                                        () ->
                                                mSpatialVisibilityHandler.second.accept(
                                                        jxrSpatialVisibility));
                            }

                            // Dispatch to PerceivedResolution listeners
                            if (!mPerceivedResolutionChangedListeners.isEmpty()) {
                                PixelDimensions jxrPerceivedResolution =
                                        RuntimeUtils.convertPerceivedResolution(
                                                visibilityStateEvent.getPerceivedResolution());

                                mPerceivedResolutionChangedListeners.forEach(
                                        (listener, executor) ->
                                                executor.execute(
                                                        () ->
                                                                listener.accept(
                                                                        jxrPerceivedResolution)));
                            }
                        });
                mIsExtensionVisibilityStateCallbackRegistered = true;
            } catch (RuntimeException e) {
                throw new RuntimeException(
                        "Could not set combined VisibilityStateCallback: " + e.getMessage());
            }
        } else if (!shouldHaveCallback && mIsExtensionVisibilityStateCallbackRegistered) {
            // Clear the combined callback
            try {
                mExtensions.clearVisibilityStateCallback(mActivity);
                mIsExtensionVisibilityStateCallbackRegistered = false;
            } catch (RuntimeException e) {
                throw new RuntimeException(
                        "Could not clear VisibilityStateCallback: " + e.getMessage());
            }
        }
    }

    @Override
    public void requestFullSpaceMode() {
        // TODO: b/376934871 - Check async results.
        mExtensions.requestFullSpaceMode(
                mActivity, /* requestEnter= */ true, Runnable::run, (result) -> {});
    }

    @Override
    public void requestHomeSpaceMode() {
        // TODO: b/376934871 - Check async results.
        mExtensions.requestFullSpaceMode(
                mActivity, /* requestEnter= */ false, Runnable::run, (result) -> {});
    }

    @Override
    public @NonNull Bundle setFullSpaceMode(@NonNull Bundle bundle) {
        return mExtensions.setFullSpaceStartMode(bundle);
    }

    @Override
    public @NonNull Bundle setFullSpaceModeWithEnvironmentInherited(@NonNull Bundle bundle) {
        return mExtensions.setFullSpaceStartModeWithEnvironmentInherited(bundle);
    }

    @Override
    public void enablePanelDepthTest(boolean enabled) {
        mExtensions.enablePanelDepthTest(mActivity, enabled);
    }

    @Override
    public void setPreferredAspectRatio(@NonNull Activity activity, float preferredRatio) {
        // TODO: b/376934871 - Check async results.
        mExtensions.setPreferredAspectRatio(
                activity, preferredRatio, Runnable::run, (result) -> {});
    }

    @Override
    public @NonNull InteractableComponent createInteractableComponent(
            @NonNull Executor executor, @NonNull InputEventListener listener) {
        return new InteractableComponentImpl(executor, listener);
    }

    @Override
    public @NonNull AnchorPlacement createAnchorPlacementForPlanes(
            @NonNull Set<PlaneType> planeTypeFilter,
            @NonNull Set<PlaneSemantic> planeSemanticFilter) {
        AnchorPlacementImpl anchorPlacement = new AnchorPlacementImpl();
        anchorPlacement.planeTypeFilter.addAll(planeTypeFilter);
        anchorPlacement.planeSemanticFilter.addAll(planeSemanticFilter);
        return anchorPlacement;
    }

    @Override
    public @NonNull MovableComponent createMovableComponent(
            boolean systemMovable, boolean scaleInZ, boolean userAnchorable) {
        return new MovableComponentImpl(
                systemMovable,
                scaleInZ,
                userAnchorable,
                mActivitySpace,
                new EntityShadowRendererImpl(
                        mActivitySpace, mPerceptionSpaceScenePose, mActivity, mExtensions),
                mExecutor);
    }

    @Override
    public @NonNull ResizableComponent createResizableComponent(
            @NonNull Dimensions minimumSize, @NonNull Dimensions maximumSize) {
        return new ResizableComponentImpl(mExecutor, mExtensions, minimumSize, maximumSize);
    }

    // Suppress warnings for factory function
    @Override
    @SuppressLint("ExecutorRegistration")
    @SuppressWarnings("ExecutorRegistration")
    public @NonNull PointerCaptureComponent createPointerCaptureComponent(
            @NonNull Executor executor,
            PointerCaptureComponent.@NonNull StateListener stateListener,
            @NonNull InputEventListener inputListener) {
        return new PointerCaptureComponentImpl(executor, stateListener, inputListener);
    }

    @Override
    public @NonNull SpatialPointerComponent createSpatialPointerComponent() {
        return new SpatialPointerComponentImpl(mExtensions);
    }

    /** Calculates the current boundary consent state directly from system settings. */
    private boolean calculateBoundaryConsentState() {
        if (mActivity == null) {
            throw new IllegalStateException(
                    "Cannot calculate boundary consent on a destroyed runtime.");
        }
        // TODO: b/464401298 - Implement boundary consent logic for Spatial API >= 2
        ContentResolver resolver = mActivity.getContentResolver();
        boolean isExplicitBoundaryConsentGranted =
                (Settings.Secure.getInt(resolver, GUARDIAN_CONSENT_GRANTED, 0) == 1);
        boolean isBoundaryEnabledInDeveloperOptions =
                (Settings.System.getInt(resolver, TOGGLE_GUARDIAN, 1) == 1);
        return (!isBoundaryEnabledInDeveloperOptions || isExplicitBoundaryConsentGranted);
    }

    private void registerBoundaryConsentStateListener() {
        // TODO: b/464401298 - Implement boundary consent logic for Spatial API >= 2
        if (mActivity == null) {
            throw new IllegalStateException("Cannot register listener on a destroyed runtime.");
        }
        if (mBoundaryConsentObserver != null) {
            return; // Already registered.
        }
        Uri isExplicitBoundaryConsentGrantedUri =
                Settings.Secure.getUriFor(GUARDIAN_CONSENT_GRANTED);
        Uri isBoundaryEnabledInDeveloperOptionsUri = Settings.System.getUriFor(TOGGLE_GUARDIAN);
        // Registers the ContentObserver to listen for changes in boundary settings.
        mBoundaryConsentObserver =
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        mExecutor.execute(
                                () -> {
                                    // Recalculate the current state
                                    boolean newGrantedState = calculateBoundaryConsentState();

                                    // Only update cache and notify listeners
                                    // if the state has actually changed
                                    if (mIsBoundaryConsentGrantedCache.compareAndSet(
                                            !newGrantedState, newGrantedState)) {
                                        mBoundaryConsentListeners.forEach(
                                                (consumer, anExecutor) ->
                                                        anExecutor.execute(
                                                                () ->
                                                                        consumer.accept(
                                                                                newGrantedState)));
                                    }
                                });
                    }
                };

        ContentResolver resolver = mActivity.getContentResolver();
        resolver.registerContentObserver(
                isExplicitBoundaryConsentGrantedUri,
                /* notifyForDescendants= */ false,
                mBoundaryConsentObserver);
        resolver.registerContentObserver(
                isBoundaryEnabledInDeveloperOptionsUri,
                /* notifyForDescendants= */ false,
                mBoundaryConsentObserver);
    }

    private void unregisterBoundaryConsentStateListener() {
        // TODO: b/464401298 - Implement boundary consent logic for Spatial API >= 2
        if (mBoundaryConsentObserver != null && mActivity != null) {
            mActivity.getContentResolver().unregisterContentObserver(mBoundaryConsentObserver);
            mBoundaryConsentObserver = null;
        }
    }

    @Override
    public boolean isBoundaryConsentGranted() {
        return mIsBoundaryConsentGrantedCache.get();
    }

    @Override
    public void addOnBoundaryConsentChangedListener(
            @NonNull Executor executor, @NonNull Consumer<Boolean> listener) {
        mBoundaryConsentListeners.put(listener, executor);
    }

    @Override
    public void removeOnBoundaryConsentChangedListener(@NonNull Consumer<Boolean> listener) {
        mBoundaryConsentListeners.remove(listener);
    }

    @Override
    public @NonNull BoundsComponent createBoundsComponent() {
        return new BoundsComponentImpl();
    }

    @Override
    public @Nullable Entity getKeyEntity() {
        return mKeyEntity;
    }

    @Override
    public void setKeyEntity(@Nullable Entity entity) {
        if (Objects.equals(mKeyEntity, entity)) {
            return;
        }

        // Always clean up the old entity's subscription first.
        clearKeyEntitySubscription(true);

        mKeyEntity = entity;

        // If the new entity is valid, set up a new subscription.
        if (mKeyEntity instanceof AndroidXrEntity) {
            setupKeyEntitySubscription((AndroidXrEntity) mKeyEntity);
        }
    }

    /** Clears any existing subscription for the current key entity. */
    private void clearKeyEntitySubscription(boolean throwException) {
        if (mKeyEntityTransformCloseable == null) {
            return;
        }
        if (mSpatialApiVersion >= 2) {
            try {
                mKeyEntityTransformCloseable.close();
                mExtensions.getUnderlyingObject().clearSpatialContinuityHint(mActivity);
            } catch (IOException e) {
                if (throwException) {
                    // Re-throw as an unchecked exception but include the original cause.
                    throw new RuntimeException(
                            "Could not close the key entity's transform subscription.", e);
                }
            } finally {
                // Ensure the reference is cleared even if closing fails.
                mKeyEntityTransformCloseable = null;
            }
        }
    }

    /** Creates a new subscription to the transform of the given key entity. */
    private void setupKeyEntitySubscription(@NonNull AndroidXrEntity entity) {
        if (mSpatialApiVersion >= 2) {
            mKeyEntityTransformCloseable =
                    entity.getNode()
                            .subscribeToTransform(
                                    mExecutor,
                                    nodeTransform -> {
                                        Matrix4 transform =
                                                RuntimeUtils.getMatrix(
                                                        nodeTransform.getTransform());
                                        mExtensions
                                                .getUnderlyingObject()
                                                .setSpatialContinuityHint(
                                                        mActivity,
                                                        getPositionFromTransform(transform),
                                                        getRotationFromTransform(transform));
                                    });
        }
    }
}
