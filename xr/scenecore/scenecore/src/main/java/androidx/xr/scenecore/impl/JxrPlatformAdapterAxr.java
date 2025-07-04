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

package androidx.xr.scenecore.impl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Pair;
import androidx.xr.runtime.SubspaceNodeHolder;
import androidx.xr.runtime.internal.ActivityPanelEntity;
import androidx.xr.runtime.internal.ActivitySpace;
import androidx.xr.runtime.internal.Anchor;
import androidx.xr.runtime.internal.AnchorEntity;
import androidx.xr.runtime.internal.AnchorPlacement;
import androidx.xr.runtime.internal.AudioTrackExtensionsWrapper;
import androidx.xr.runtime.internal.CameraViewActivityPose;
import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.ExrImageResource;
import androidx.xr.runtime.internal.GltfEntity;
import androidx.xr.runtime.internal.GltfModelResource;
import androidx.xr.runtime.internal.HeadActivityPose;
import androidx.xr.runtime.internal.InputEventListener;
import androidx.xr.runtime.internal.InteractableComponent;
import androidx.xr.runtime.internal.JxrPlatformAdapter;
import androidx.xr.runtime.internal.KhronosPbrMaterialSpec;
import androidx.xr.runtime.internal.LoggingEntity;
import androidx.xr.runtime.internal.MaterialResource;
import androidx.xr.runtime.internal.MediaPlayerExtensionsWrapper;
import androidx.xr.runtime.internal.MovableComponent;
import androidx.xr.runtime.internal.PanelEntity;
import androidx.xr.runtime.internal.PerceptionSpaceActivityPose;
import androidx.xr.runtime.internal.PixelDimensions;
import androidx.xr.runtime.internal.PlaneSemantic;
import androidx.xr.runtime.internal.PlaneType;
import androidx.xr.runtime.internal.PointerCaptureComponent;
import androidx.xr.runtime.internal.ResizableComponent;
import androidx.xr.runtime.internal.SoundPoolExtensionsWrapper;
import androidx.xr.runtime.internal.Space;
import androidx.xr.runtime.internal.SpatialCapabilities;
import androidx.xr.runtime.internal.SpatialEnvironment;
import androidx.xr.runtime.internal.SpatialModeChangeListener;
import androidx.xr.runtime.internal.SpatialPointerComponent;
import androidx.xr.runtime.internal.SpatialVisibility;
import androidx.xr.runtime.internal.SubspaceNodeEntity;
import androidx.xr.runtime.internal.SurfaceEntity;
import androidx.xr.runtime.internal.TextureResource;
import androidx.xr.runtime.internal.TextureSampler;
import androidx.xr.runtime.math.Matrix3;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.math.Vector4;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.impl.perception.ViewProjections;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransaction;
import com.android.extensions.xr.space.ActivityPanel;
import com.android.extensions.xr.space.ActivityPanelLaunchParameters;
import com.android.extensions.xr.space.SpatialState;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;
import com.google.ar.imp.apibindings.ImpressApi;
import com.google.ar.imp.apibindings.ImpressApiImpl;
import com.google.ar.imp.apibindings.KhronosPbrMaterial;
import com.google.ar.imp.apibindings.Texture;
import com.google.ar.imp.apibindings.WaterMaterial;
import com.google.ar.imp.view.splitengine.ImpSplitEngine;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Implementation of JxrPlatformAdapter for AndroidXR. */
// TODO(b/373435470): Remove "deprecation" and "UnnecessarilyFullyQualified"
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
@SuppressWarnings({"UnnecessarilyFullyQualified", "BanSynchronizedMethods", "BanConcurrentHashMap"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class JxrPlatformAdapterAxr implements JxrPlatformAdapter {
    @VisibleForTesting static final String TAG = "JxrPlatformAdapterAxr";
    private static final String SPLIT_ENGINE_LIBRARY_NAME = "impress_api_jni";

    private final ActivitySpaceImpl mActivitySpace;
    private final HeadActivityPoseImpl mHeadActivityPose;
    private final PerceptionSpaceActivityPoseImpl mPerceptionSpaceActivityPose;
    private final List<CameraViewActivityPoseImpl> mCameraActivityPoses = new ArrayList<>();
    private final ScheduledExecutorService mExecutor;
    private final XrExtensions mExtensions;

    private final SoundPoolExtensionsWrapper mSoundPoolExtensionsWrapper;
    private final AudioTrackExtensionsWrapper mAudioTrackExtensionsWrapper;
    private final MediaPlayerExtensionsWrapper mMediaPlayerExtensionsWrapper;

    private final PerceptionLibrary mPerceptionLibrary;
    private final SpatialEnvironmentImpl mEnvironment;
    private final boolean mUseSplitEngine;
    private final Node mTaskWindowLeashNode;
    private final int mOpenXrReferenceSpaceType;
    private final EntityManager mEntityManager;
    private final PanelEntity mMainPanelEntity;
    private final ImpressApi mImpressApi;
    private final Map<Consumer<SpatialCapabilities>, Executor>
            mSpatialCapabilitiesChangedListeners = new ConcurrentHashMap<>();

    private @Nullable Pair<Executor, Consumer<SpatialVisibility>> mSpatialVisibilityHandler = null;
    private final Map<Consumer<PixelDimensions>, Executor> mPerceivedResolutionChangedListeners =
            new ConcurrentHashMap<>();
    @VisibleForTesting boolean mIsExtensionVisibilityStateCallbackRegistered = false;

    // TODO b/373481538: remove lazy initialization once XR Extensions bug is fixed. This will allow
    // us to remove the lazySpatialStateProvider instance and pass the spatialState directly.
    private final AtomicReference<SpatialState> mSpatialState = new AtomicReference<>(null);

    // Returns the currently-known spatial state, or fetches it from the extensions if it has never
    // been set. The spatial state is kept updated in the SpatialStateCallback.
    private final Supplier<SpatialState> mLazySpatialStateProvider;

    private @Nullable Activity mActivity;
    private SplitEngineSubspaceManager mSplitEngineSubspaceManager;
    private ImpSplitEngineRenderer mSplitEngineRenderer;
    private boolean mFrameLoopStarted;
    private boolean mIsDisposed;
    private SpatialModeChangeListener mSpatialModeChangeListener;

    private JxrPlatformAdapterAxr(
            Activity activity,
            ScheduledExecutorService executor,
            XrExtensions extensions,
            @Nullable ImpressApi impressApi,
            EntityManager entityManager,
            PerceptionLibrary perceptionLibrary,
            @Nullable SplitEngineSubspaceManager subspaceManager,
            @Nullable ImpSplitEngineRenderer renderer,
            Node sceneRootNode,
            Node taskWindowLeashNode,
            boolean useSplitEngine,
            boolean unscaledGravityAlignedActivitySpace) {
        mActivity = activity;
        mExecutor = executor;
        mExtensions = extensions;

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
        mEntityManager = entityManager;
        mPerceptionLibrary = perceptionLibrary;
        mTaskWindowLeashNode = taskWindowLeashNode;
        mEnvironment =
                new SpatialEnvironmentImpl(
                        activity,
                        extensions,
                        sceneRootNode,
                        mLazySpatialStateProvider,
                        useSplitEngine);
        mActivitySpace =
                new ActivitySpaceImpl(
                        sceneRootNode,
                        activity,
                        extensions,
                        entityManager,
                        mLazySpatialStateProvider,
                        unscaledGravityAlignedActivitySpace,
                        executor);
        mEntityManager.addSystemSpaceActivityPose(mActivitySpace);
        mHeadActivityPose =
                new HeadActivityPoseImpl(
                        mActivitySpace,
                        (AndroidXrEntity) getActivitySpaceRootImpl(),
                        perceptionLibrary);
        mEntityManager.addSystemSpaceActivityPose(mHeadActivityPose);
        mPerceptionSpaceActivityPose =
                new PerceptionSpaceActivityPoseImpl(
                        mActivitySpace, (AndroidXrEntity) getActivitySpaceRootImpl());
        mEntityManager.addSystemSpaceActivityPose(mPerceptionSpaceActivityPose);
        mCameraActivityPoses.add(
                new CameraViewActivityPoseImpl(
                        CameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE,
                        mActivitySpace,
                        (AndroidXrEntity) getActivitySpaceRootImpl(),
                        perceptionLibrary));
        mCameraActivityPoses.add(
                new CameraViewActivityPoseImpl(
                        CameraViewActivityPose.CameraType.CAMERA_TYPE_RIGHT_EYE,
                        mActivitySpace,
                        (AndroidXrEntity) getActivitySpaceRootImpl(),
                        perceptionLibrary));
        mCameraActivityPoses.forEach(mEntityManager::addSystemSpaceActivityPose);
        mUseSplitEngine = useSplitEngine;
        mOpenXrReferenceSpaceType = extensions.getOpenXrWorldReferenceSpaceType();

        mMainPanelEntity =
                new MainPanelEntityImpl(
                        activity, taskWindowLeashNode, extensions, entityManager, executor);
        mMainPanelEntity.setParent(mActivitySpace);

        // TODO:b/377918731 - Move this logic into factories and inject SE into the constructor
        if (impressApi == null) {
            // TODO: b/370116937) - Check against useSplitEngine as well and don't load this if
            //                      SplitEngine is disabled.
            mImpressApi = new ImpressApiImpl();
        } else {
            mImpressApi = impressApi;
        }

        if (useSplitEngine && subspaceManager == null && renderer == null) {
            ImpSplitEngine.SplitEngineSetupParams impApiSetupParams =
                    new ImpSplitEngine.SplitEngineSetupParams();
            impApiSetupParams.jniLibraryName = SPLIT_ENGINE_LIBRARY_NAME;
            mSplitEngineRenderer =
                    ImpSplitEngineRenderer.create(activity, impApiSetupParams, extensions);
            startRenderer();
            mSplitEngineSubspaceManager =
                    new SplitEngineSubspaceManager(
                            mSplitEngineRenderer,
                            extensions,
                            sceneRootNode,
                            taskWindowLeashNode,
                            SPLIT_ENGINE_LIBRARY_NAME);
            mImpressApi.setup(mSplitEngineRenderer.getView());
            mEnvironment.onSplitEngineReady(mSplitEngineSubspaceManager, mImpressApi);
        }
    }

    /** Create a new @c JxrPlatformAdapterAxr. */
    public static @NonNull JxrPlatformAdapterAxr create(
            @NonNull Activity activity,
            boolean unscaledGravityAlignedActivitySpace,
            @NonNull ScheduledExecutorService executor) {
        return create(
                activity,
                executor,
                Objects.requireNonNull(XrExtensionsProvider.getXrExtensions()),
                null,
                new EntityManager(),
                new PerceptionLibrary(),
                null,
                null,
                /* useSplitEngine= */ true,
                unscaledGravityAlignedActivitySpace);
    }

    /** Create a new @c JxrPlatformAdapterAxr. */
    public static @NonNull JxrPlatformAdapterAxr create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            boolean useSplitEngine) {
        return create(
                activity,
                executor,
                Objects.requireNonNull(XrExtensionsProvider.getXrExtensions()),
                null,
                new EntityManager(),
                new PerceptionLibrary(),
                null,
                null,
                useSplitEngine,
                /* unscaledGravityAlignedActivitySpace= */ false);
    }

    /** Create a new @c JxrPlatformAdapterAxr. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static @NonNull JxrPlatformAdapterAxr create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            @NonNull Node sceneRootNode,
            @NonNull Node taskWindowLeashNode) {
        return create(
                activity,
                executor,
                Objects.requireNonNull(XrExtensionsProvider.getXrExtensions()),
                null,
                new EntityManager(),
                new PerceptionLibrary(),
                null,
                null,
                sceneRootNode,
                taskWindowLeashNode,
                /* useSplitEngine= */ false,
                /* unscaledGravityAlignedActivitySpace= */ false);
    }

    /** Create a new @c JxrPlatformAdapterAxr. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static @NonNull JxrPlatformAdapterAxr create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            @NonNull XrExtensions extensions,
            @Nullable ImpressApi impressApi,
            @NonNull PerceptionLibrary perceptionLibrary,
            @Nullable SplitEngineSubspaceManager splitEngineSubspaceManager,
            @Nullable ImpSplitEngineRenderer splitEngineRenderer) {
        return create(
                activity,
                executor,
                extensions,
                impressApi,
                new EntityManager(),
                perceptionLibrary,
                splitEngineSubspaceManager,
                splitEngineRenderer,
                /* useSplitEngine= */ false,
                /* unscaledGravityAlignedActivitySpace= */ false);
    }

    static JxrPlatformAdapterAxr create(
            Activity activity,
            ScheduledExecutorService executor,
            XrExtensions extensions,
            ImpressApi impressApi,
            EntityManager entityManager,
            PerceptionLibrary perceptionLibrary,
            SplitEngineSubspaceManager splitEngineSubspaceManager,
            ImpSplitEngineRenderer splitEngineRenderer,
            boolean useSplitEngine,
            boolean unscaledGravityAlignedActivitySpace) {
        Node sceneRootNode = extensions.createNode();
        Log.i(TAG, "Impl Node for task $activity.taskId is root scene node: " + sceneRootNode);
        Node taskWindowLeashNode = extensions.createNode();
        // TODO: b/376934871 - Check async results.
        extensions.attachSpatialScene(
                activity,
                sceneRootNode,
                taskWindowLeashNode,
                executor,
                (result) -> Log.i(TAG, "attachSpatialScene result: " + result));
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction
                    .setName(sceneRootNode, "SpatialSceneAndActivitySpaceRootNode")
                    .setParent(taskWindowLeashNode, sceneRootNode)
                    .setName(taskWindowLeashNode, "MainPanelAndTaskWindowLeashNode")
                    .apply();
        }
        return create(
                activity,
                executor,
                extensions,
                impressApi,
                entityManager,
                perceptionLibrary,
                splitEngineSubspaceManager,
                splitEngineRenderer,
                sceneRootNode,
                taskWindowLeashNode,
                useSplitEngine,
                unscaledGravityAlignedActivitySpace);
    }

    static JxrPlatformAdapterAxr create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            @NonNull XrExtensions extensions,
            @Nullable ImpressApi impressApi,
            @NonNull EntityManager entityManager,
            @NonNull PerceptionLibrary perceptionLibrary,
            @Nullable SplitEngineSubspaceManager splitEngineSubspaceManager,
            @Nullable ImpSplitEngineRenderer splitEngineRenderer,
            @NonNull Node sceneRootNode,
            @NonNull Node taskWindowLeashNode,
            boolean useSplitEngine,
            boolean unscaledGravityAlignedActivitySpace) {
        JxrPlatformAdapterAxr runtime =
                new JxrPlatformAdapterAxr(
                        activity,
                        executor,
                        extensions,
                        impressApi,
                        entityManager,
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        sceneRootNode,
                        taskWindowLeashNode,
                        useSplitEngine,
                        unscaledGravityAlignedActivitySpace);

        Log.i(TAG, "Initing perception library soon");
        runtime.initPerceptionLibrary();
        return runtime;
    }

    private static GltfModelResourceImpl getModelResourceFromToken(long token) {
        return new GltfModelResourceImpl(token);
    }

    private static ExrImageResourceImpl getExrImageResourceFromToken(long token) {
        return new ExrImageResourceImpl(token);
    }

    private static TextureResourceImpl getTextureResourceFromToken(long token) {
        return new TextureResourceImpl(token);
    }

    private static MaterialResourceImpl getMaterialResourceFromToken(long token) {
        return new MaterialResourceImpl(token);
    }

    // Note that this is called on the Activity's UI thread so we should be careful to not  block
    // it.
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

    private synchronized void initPerceptionLibrary() {
        if (mPerceptionLibrary.getSession() != null) {
            Log.w(TAG, "Cannot init perception session, already initialized.");
            return;
        }
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, mOpenXrReferenceSpaceType, mExecutor);
        Objects.requireNonNull(sessionFuture)
                .addListener(
                        () -> {
                            try {
                                sessionFuture.get();
                            } catch (Exception e) {
                                if (e instanceof InterruptedException) {
                                    Thread.currentThread().interrupt();
                                }
                                Log.e(
                                        TAG,
                                        "Failed to init perception session with error: "
                                                + e.getMessage());
                            }
                        },
                        mExecutor);
    }

    @Override
    public @NonNull SpatialCapabilities getSpatialCapabilities() {
        return RuntimeUtils.convertSpatialCapabilities(
                mLazySpatialStateProvider.get().getSpatialCapabilities());
    }

    @Override
    public void enablePanelDepthTest(boolean enabled) {
        mExtensions.enablePanelDepthTest(mActivity, enabled);
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
                        mExecutor, // Executor for the combined callback itself
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
                Log.d(TAG, "Registered combined visibility callback with XrExtensions.");
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not set combined VisibilityStateCallback: " + e.getMessage());
            }
        } else if (!shouldHaveCallback && mIsExtensionVisibilityStateCallbackRegistered) {
            // Clear the combined callback
            try {
                mExtensions.clearVisibilityStateCallback(mActivity);
                mIsExtensionVisibilityStateCallbackRegistered = false;
                Log.d(TAG, "Cleared combined visibility callback from XrExtensions.");
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not clear VisibilityStateCallback: " + e.getMessage());
            }
        }
    }

    @Override
    public @NonNull LoggingEntity createLoggingEntity(@NonNull Pose pose) {
        LoggingEntityImpl entity = new LoggingEntityImpl(mActivity);
        entity.setPose(pose, Space.PARENT);
        return entity;
    }

    @Override
    public @NonNull SpatialEnvironment getSpatialEnvironment() {
        return mEnvironment;
    }

    @Override
    public @NonNull ActivitySpace getActivitySpace() {
        return mActivitySpace;
    }

    @Override
    public @Nullable HeadActivityPose getHeadActivityPose() {
        // If it is unable to retrieve a pose the head in not yet loaded in openXR so return null.
        if (mHeadActivityPose.getPoseInOpenXrReferenceSpace() == null) {
            return null;
        }
        return mHeadActivityPose;
    }

    @Override
    public @Nullable CameraViewActivityPose getCameraViewActivityPose(
            @CameraViewActivityPose.CameraType int cameraType) {
        CameraViewActivityPoseImpl cameraViewActivityPose = null;
        if (cameraType == CameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE) {
            cameraViewActivityPose = mCameraActivityPoses.get(0);
        } else if (cameraType == CameraViewActivityPose.CameraType.CAMERA_TYPE_RIGHT_EYE) {
            cameraViewActivityPose = mCameraActivityPoses.get(1);
        }
        // If it is unable to retrieve a pose the camera in not yet loaded in openXR so return null.
        if (cameraViewActivityPose == null
                || cameraViewActivityPose.getPoseInOpenXrReferenceSpace() == null) {
            return null;
        }
        return cameraViewActivityPose;
    }

    @Override
    public @NonNull PerceptionSpaceActivityPose getPerceptionSpaceActivityPose() {
        return mPerceptionSpaceActivityPose;
    }

    /**
     * Get the user's current eye views relative to @c XR_REFERENCE_SPACE_TYPE_UNBOUNDED_ANDROID.
     */
    public @Nullable ViewProjections getStereoViewsInOpenXrUnboundedSpace() {
        Session session = mPerceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Perception session is uninitialized, returning null head pose.");
            return null;
        }
        return session.getStereoViews();
    }

    @Override
    public @NonNull Entity getActivitySpaceRootImpl() {
        // Trivially returns the activity space for now, but it could be updated to return any other
        // singleton space entity. That space entity will define the world space origin of the SDK
        // and
        // will be the default parent for new content entities.

        return mActivitySpace;
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

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @Nullable ListenableFuture<GltfModelResource> loadGltfByAssetName(@NonNull String name) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Loading glTFs is not supported without SplitEngine.");
        } else {
            return loadGltfAsset(() -> mImpressApi.loadGltfAsset(name));
        }
    }

    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @Nullable ListenableFuture<GltfModelResource> loadGltfByByteArray(
            byte @NonNull [] assetData, @NonNull String assetKey) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Loading glTFs is not supported without SplitEngine.");
        } else {
            return loadGltfAsset(() -> mImpressApi.loadGltfAsset(assetData, assetKey));
        }
    }

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @Nullable ListenableFuture<ExrImageResource> loadExrImageByAssetName(
            @NonNull String assetName) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Loading ExrImages is not supported without SplitEngine.");
        } else {
            return loadExrImage(() -> mImpressApi.loadImageBasedLightingAsset(assetName));
        }
    }

    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @Nullable ListenableFuture<ExrImageResource> loadExrImageByByteArray(
            byte @NonNull [] assetData, @NonNull String assetKey) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Loading ExrImages is not supported without SplitEngine.");
        } else {
            return loadExrImage(() -> mImpressApi.loadImageBasedLightingAsset(assetData, assetKey));
        }
    }

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @Nullable ListenableFuture<TextureResource> loadTexture(
            @NonNull String path, @NonNull TextureSampler sampler) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Loading textures is not supported without SplitEngine.");
        }
        ResolvableFuture<TextureResource> textureResourceFuture = ResolvableFuture.create();
        // TODO:b/374216912 - Consider calling setFuture() here to catch if the application calls
        // cancel() on the return value from this function, so we can propagate the cancelation
        // message
        // to the Impress API.

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ListenableFuture<Texture> textureFuture;
        try {
            textureFuture = mImpressApi.loadTexture(path, RuntimeUtils.getTextureSampler(sampler));
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to load texture with error: " + e.getMessage());
            // TODO:b/375070346 - make this method NonNull and set the textureResourceFuture to an
            // exception and return that.
            return null;
        }

        textureFuture.addListener(
                () -> {
                    try {
                        Texture texture = textureFuture.get();
                        textureResourceFuture.set(
                                getTextureResourceFromToken(texture.getNativeHandle()));
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        Log.e(TAG, "Failed to load texture with error: " + e.getMessage());
                        textureResourceFuture.setException(e);
                    }
                },
                // It's convenient for the main application for us to dispatch their listeners on
                // the main
                // thread, because they are required to call back to Impress from there, and it's
                // likely
                // that they will want to call back into the SDK to create entities from within a
                // listener.
                // We defensively post to the main thread here, but in practice this should not
                // cause a
                // thread hop because the Impress API already dispatches its callbacks to the main
                // thread.
                mActivity::runOnUiThread);
        return textureResourceFuture;
    }

    @Override
    public @Nullable TextureResource borrowReflectionTexture() {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Borrowing textures is not supported without SplitEngine.");
        }
        Texture texture = mImpressApi.borrowReflectionTexture();
        if (texture == null) {
            return null;
        }
        return getTextureResourceFromToken(texture.getNativeHandle());
    }

    @Override
    public void destroyTexture(@NonNull TextureResource texture) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Destroying textures is not supported without SplitEngine.");
        }
        TextureResourceImpl textureResource = (TextureResourceImpl) texture;
        mImpressApi.destroyNativeObject(textureResource.getTextureToken());
    }

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @Nullable ListenableFuture<MaterialResource> createWaterMaterial(
            boolean isAlphaMapVersion) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Loading water materials is not supported without SplitEngine.");
        }
        ResolvableFuture<MaterialResource> materialResourceFuture = ResolvableFuture.create();
        // TODO:b/374216912 - Consider calling setFuture() here to catch if the application calls
        // cancel() on the return value from this function, so we can propagate the cancelation
        // message
        // to the Impress API.

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ListenableFuture<WaterMaterial> materialFuture;
        try {
            materialFuture = mImpressApi.createWaterMaterial(isAlphaMapVersion);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to load water material with error: " + e.getMessage());
            // TODO:b/375070346 - make this method NonNull and set the textureResourceFuture to an
            // exception and return that.
            return null;
        }

        materialFuture.addListener(
                () -> {
                    try {
                        WaterMaterial material = materialFuture.get();
                        materialResourceFuture.set(
                                getMaterialResourceFromToken(material.getNativeHandle()));
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        Log.e(TAG, "Failed to load water material with error: " + e.getMessage());
                        materialResourceFuture.setException(e);
                    }
                },
                // It's convenient for the main application for us to dispatch their listeners on
                // the main
                // thread, because they are required to call back to Impress from there, and it's
                // likely
                // that they will want to call back into the SDK to create entities from within a
                // listener.
                // We defensively post to the main thread here, but in practice this should not
                // cause a
                // thread hop because the Impress API already dispatches its callbacks to the main
                // thread.
                mActivity::runOnUiThread);
        return materialResourceFuture;
    }

    @Override
    public void destroyWaterMaterial(@NonNull MaterialResource material) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Destroying materials is not supported without SplitEngine.");
        }
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.destroyNativeObject(((MaterialResourceImpl) material).getMaterialToken());
    }

    @Override
    public void setReflectionMapOnWaterMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource reflectionMap) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Setting material parameters is not supported without SplitEngine.");
        }
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(reflectionMap instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setReflectionMapOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) reflectionMap).getTextureToken());
    }

    @Override
    public void setNormalMapOnWaterMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource normalMap) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Setting material parameters is not supported without SplitEngine.");
        }
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(normalMap instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setNormalMapOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) normalMap).getTextureToken());
    }

    @Override
    public void setNormalTilingOnWaterMaterial(
            @NonNull MaterialResource material, float normalTiling) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Setting material parameters is not supported without SplitEngine.");
        }
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setNormalTilingOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), normalTiling);
    }

    @Override
    public void setNormalSpeedOnWaterMaterial(
            @NonNull MaterialResource material, float normalSpeed) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Setting material parameters is not supported without SplitEngine.");
        }
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setNormalSpeedOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), normalSpeed);
    }

    @Override
    public void setAlphaStepMultiplierOnWaterMaterial(
            @NonNull MaterialResource material, float alphaStepMultiplier) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Setting material parameters is not supported without SplitEngine.");
        }
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setAlphaStepMultiplierOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), alphaStepMultiplier);
    }

    @Override
    public void setAlphaMapOnWaterMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource alphaMap) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Setting material parameters is not supported without SplitEngine.");
        }
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(alphaMap instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setAlphaMapOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) alphaMap).getTextureToken());
    }

    @Override
    public void setNormalZOnWaterMaterial(@NonNull MaterialResource material, float normalZ) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Setting material parameters is not supported without SplitEngine.");
        }
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setNormalZOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), normalZ);
    }

    @Override
    public void setNormalBoundaryOnWaterMaterial(
            @NonNull MaterialResource material, float normalBoundary) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Setting material parameters is not supported without SplitEngine.");
        }
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setNormalBoundaryOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), normalBoundary);
    }

    @SuppressWarnings("AsyncSuffixFuture")
    @Override
    public @Nullable ListenableFuture<MaterialResource> createKhronosPbrMaterial(
            @NonNull KhronosPbrMaterialSpec spec) {
        ResolvableFuture<MaterialResource> materialResourceFuture = ResolvableFuture.create();
        // TODO:b/374216912 - Consider calling setFuture() here to catch if the application calls
        // cancel() on the return value from this function, so we can propagate the cancelation
        // message
        // to the Impress API.

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ListenableFuture<KhronosPbrMaterial> materialFuture;
        try {
            materialFuture =
                    mImpressApi.createKhronosPbrMaterial(
                            RuntimeUtils.getKhronosPbrMaterialSpec(spec));
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to load Khronos PBR material with error: " + e.getMessage());
            // TODO:b/375070346 - make this method NonNull and set the textureResourceFuture to an
            // exception and return that.
            return null;
        }

        materialFuture.addListener(
                () -> {
                    try {
                        KhronosPbrMaterial material = materialFuture.get();
                        materialResourceFuture.set(
                                getMaterialResourceFromToken(material.getNativeHandle()));
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        Log.e(
                                TAG,
                                "Failed to load Khronos PBR material with error: "
                                        + e.getMessage());
                        materialResourceFuture.setException(e);
                    }
                },
                // It's convenient for the main application for us to dispatch their listeners on
                // the main
                // thread, because they are required to call back to Impress from there, and it's
                // likely
                // that they will want to call back into the SDK to create entities from within a
                // listener.
                // We defensively post to the main thread here, but in practice this should not
                // cause a
                // thread hop because the Impress API already dispatches its callbacks to the main
                // thread.
                mActivity::runOnUiThread);
        return materialResourceFuture;
    }

    @Override
    public void destroyKhronosPbrMaterial(@NonNull MaterialResource material) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.destroyNativeObject(((MaterialResourceImpl) material).getMaterialToken());
    }

    @Override
    public void setBaseColorTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource baseColor) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(baseColor instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setBaseColorTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) baseColor).getTextureToken());
    }

    @Override
    public void setBaseColorUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setBaseColorUvTransformOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                data[0],
                data[1],
                data[2],
                data[3],
                data[4],
                data[5],
                data[6],
                data[7],
                data[8]);
    }

    @Override
    public void setBaseColorFactorsOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Vector4 factors) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setBaseColorFactorsOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                factors.getX(),
                factors.getY(),
                factors.getZ(),
                factors.getW());
    }

    @Override
    public void setMetallicRoughnessTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource metallicRoughness) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(metallicRoughness instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setMetallicRoughnessTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) metallicRoughness).getTextureToken());
    }

    @Override
    public void setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                data[0],
                data[1],
                data[2],
                data[3],
                data[4],
                data[5],
                data[6],
                data[7],
                data[8]);
    }

    @Override
    public void setMetallicFactorOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float factor) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setMetallicFactorOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), factor);
    }

    @Override
    public void setRoughnessFactorOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float factor) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setRoughnessFactorOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), factor);
    }

    @Override
    public void setNormalTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource normal) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(normal instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setNormalTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) normal).getTextureToken());
    }

    @Override
    public void setNormalUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setNormalUvTransformOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                data[0],
                data[1],
                data[2],
                data[3],
                data[4],
                data[5],
                data[6],
                data[7],
                data[8]);
    }

    @Override
    public void setNormalFactorOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float factor) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setNormalFactorOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), factor);
    }

    @Override
    public void setAmbientOcclusionTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource ambientOcclusion) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(ambientOcclusion instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setAmbientOcclusionTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) ambientOcclusion).getTextureToken());
    }

    @Override
    public void setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                data[0],
                data[1],
                data[2],
                data[3],
                data[4],
                data[5],
                data[6],
                data[7],
                data[8]);
    }

    @Override
    public void setAmbientOcclusionFactorOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float factor) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setAmbientOcclusionFactorOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), factor);
    }

    @Override
    public void setEmissiveTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource emissive) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(emissive instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setEmissiveTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) emissive).getTextureToken());
    }

    @Override
    public void setEmissiveUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setEmissiveUvTransformOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                data[0],
                data[1],
                data[2],
                data[3],
                data[4],
                data[5],
                data[6],
                data[7],
                data[8]);
    }

    @Override
    public void setEmissiveFactorsOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Vector3 factors) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setEmissiveFactorsOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                factors.getX(),
                factors.getY(),
                factors.getZ());
    }

    @Override
    public void setClearcoatTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource clearcoat) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(clearcoat instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setClearcoatTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) clearcoat).getTextureToken());
    }

    @Override
    public void setClearcoatNormalTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource clearcoatNormal) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(clearcoatNormal instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setClearcoatNormalTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) clearcoatNormal).getTextureToken());
    }

    @Override
    public void setClearcoatRoughnessTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource clearcoatRoughness) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(clearcoatRoughness instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setClearcoatRoughnessTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) clearcoatRoughness).getTextureToken());
    }

    @Override
    public void setClearcoatFactorsOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float intensity, float roughness, float normal) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setClearcoatFactorsOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), intensity, roughness, normal);
    }

    @Override
    public void setSheenColorTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource sheenColor) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(sheenColor instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setSheenColorTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) sheenColor).getTextureToken());
    }

    @Override
    public void setSheenColorFactorsOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Vector3 factors) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setSheenColorFactorsOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                factors.getX(),
                factors.getY(),
                factors.getZ());
    }

    @Override
    public void setSheenRoughnessTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource sheenRoughness) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(sheenRoughness instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setSheenRoughnessTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) sheenRoughness).getTextureToken());
    }

    @Override
    public void setSheenRoughnessFactorOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float factor) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setSheenRoughnessFactorOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), factor);
    }

    @Override
    public void setTransmissionTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource transmission) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(transmission instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setTransmissionTextureOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) transmission).getTextureToken());
    }

    @Override
    public void setTransmissionUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setTransmissionUvTransformOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                data[0],
                data[1],
                data[2],
                data[3],
                data[4],
                data[5],
                data[6],
                data[7],
                data[8]);
    }

    @Override
    public void setTransmissionFactorOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float factor) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setTransmissionFactorOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), factor);
    }

    @Override
    public void setIndexOfRefractionOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float indexOfRefraction) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setIndexOfRefractionOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), indexOfRefraction);
    }

    @Override
    public void setAlphaCutoffOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float alphaCutoff) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setAlphaCutoffOnKhronosPbrMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), alphaCutoff);
    }

    @Override
    public @Nullable TextureResource getReflectionTextureFromIbl(
            @NonNull ExrImageResource iblToken) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "Getting reflection texture from an IBL is not supported without SplitEngine.");
        }
        ExrImageResourceImpl exrImageResource = (ExrImageResourceImpl) iblToken;
        Texture texture =
                mImpressApi.getReflectionTextureFromIbl(exrImageResource.getExtensionImageToken());
        if (texture == null) {
            return null;
        }
        return getTextureResourceFromToken(texture.getNativeHandle());
    }

    @Override
    public @NonNull GltfEntity createGltfEntity(
            @NonNull Pose pose, @NonNull GltfModelResource model, @Nullable Entity parentEntity) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "GltfEntity is not supported without SplitEngine.");
        } else {
            return createGltfEntitySplitEngine(pose, model, parentEntity);
        }
    }

    @Override
    public @NonNull SurfaceEntity createSurfaceEntity(
            @SurfaceEntity.StereoMode int stereoMode,
            @NonNull Pose pose,
            SurfaceEntity.@NonNull CanvasShape canvasShape,
            @SurfaceEntity.ContentSecurityLevel int contentSecurityLevel,
            @SurfaceEntity.SuperSampling int superSampling,
            @NonNull Entity parentEntity) {
        if (!mUseSplitEngine) {
            throw new UnsupportedOperationException(
                    "SurfaceEntity is not supported without SplitEngine.");
        } else {
            return createSurfaceEntitySplitEngine(
                    stereoMode,
                    canvasShape,
                    contentSecurityLevel,
                    superSampling,
                    pose,
                    parentEntity);
        }
    }

    @Override
    public @NonNull PanelEntity createPanelEntity(
            @NonNull Context context,
            @NonNull Pose pose,
            @NonNull View view,
            @NonNull Dimensions dimensions,
            @NonNull String name,
            @NonNull Entity parent) {

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
            @NonNull Entity parent) {

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
    @SuppressLint("ExecutorRegistration")
    public @NonNull InteractableComponent createInteractableComponent(
            @NonNull Executor executor, @NonNull InputEventListener listener) {
        return new InteractableComponentImpl(executor, listener);
    }

    @Override
    public @NonNull MovableComponent createMovableComponent(
            boolean systemMovable,
            boolean scaleInZ,
            @NonNull Set<AnchorPlacement> anchorPlacement,
            boolean shouldDisposeParentAnchor) {
        return new MovableComponentImpl(
                systemMovable,
                scaleInZ,
                anchorPlacement,
                shouldDisposeParentAnchor,
                mPerceptionLibrary,
                mExtensions,
                mActivitySpace,
                (AndroidXrEntity) getActivitySpaceRootImpl(),
                mPerceptionSpaceActivityPose,
                mEntityManager,
                new PanelShadowRenderer(
                        mActivitySpace, mPerceptionSpaceActivityPose, mActivity, mExtensions),
                mExecutor);
    }

    @Override
    public @NonNull AnchorPlacement createAnchorPlacementForPlanes(
            @NonNull Set<PlaneType> planeTypeFilter,
            @NonNull Set<PlaneSemantic> planeSemanticFilter) {
        AnchorPlacementImpl anchorPlacement = new AnchorPlacementImpl();
        anchorPlacement.mPlaneTypeFilter.addAll(planeTypeFilter);
        anchorPlacement.mPlaneSemanticFilter.addAll(planeSemanticFilter);
        return anchorPlacement;
    }

    @Override
    public @NonNull ResizableComponent createResizableComponent(
            @NonNull Dimensions minimumSize, @NonNull Dimensions maximumSize) {
        return new ResizableComponentImpl(mExecutor, mExtensions, minimumSize, maximumSize);
    }

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

    @Override
    public @NonNull ActivityPanelEntity createActivityPanelEntity(
            @NonNull Pose pose,
            @NonNull PixelDimensions windowBoundsPx,
            @NonNull String name,
            @NonNull Activity hostActivity,
            @NonNull Entity parent) {

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
    public @NonNull AnchorEntity createAnchorEntity(
            @NonNull Dimensions bounds,
            @NonNull PlaneType planeType,
            @NonNull PlaneSemantic planeSemantic,
            @NonNull Duration searchTimeout) {
        Node node = mExtensions.createNode();
        return AnchorEntityImpl.createSemanticAnchor(
                mActivity,
                node,
                bounds,
                planeType,
                planeSemantic,
                searchTimeout,
                getActivitySpace(),
                getActivitySpaceRootImpl(),
                mExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
    }

    @Override
    public @NonNull AnchorEntity createAnchorEntity(@NonNull Anchor anchor) {
        Node node = mExtensions.createNode();
        return AnchorEntityImpl.createAnchorFromRuntimeAnchor(
                mActivity,
                node,
                anchor,
                getActivitySpace(),
                getActivitySpaceRootImpl(),
                mExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
    }

    @Override
    public @NonNull Entity createGroupEntity(
            @NonNull Pose pose, @NonNull String name, @NonNull Entity parent) {
        Node node = mExtensions.createNode();
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setName(node, name).apply();
        }

        // This entity is used to back JXR Core's GroupEntity.
        Entity entity =
                new AndroidXrEntity(mActivity, node, mExtensions, mEntityManager, mExecutor) {};
        entity.setParent(parent);
        entity.setPose(pose, Space.PARENT);
        return entity;
    }

    @Override
    public @NonNull AnchorEntity createPersistedAnchorEntity(
            @NonNull UUID uuid, @NonNull Duration searchTimeout) {
        Node node = mExtensions.createNode();
        return AnchorEntityImpl.createPersistedAnchor(
                mActivity,
                node,
                uuid,
                searchTimeout,
                getActivitySpace(),
                getActivitySpaceRootImpl(),
                mExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
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
    public void setPreferredAspectRatio(@NonNull Activity activity, float preferredRatio) {
        // TODO: b/376934871 - Check async results.
        mExtensions.setPreferredAspectRatio(
                activity, preferredRatio, Runnable::run, (result) -> {});
    }

    @Override
    public void startRenderer() {
        if (mSplitEngineRenderer == null || mFrameLoopStarted) {
            return;
        }
        mFrameLoopStarted = true;
        mSplitEngineRenderer.startFrameLoop();
    }

    @Override
    public void stopRenderer() {
        if (mSplitEngineRenderer == null || !mFrameLoopStarted) {
            return;
        }
        mFrameLoopStarted = false;
        mSplitEngineRenderer.stopFrameLoop();
    }

    @Override
    public void dispose() {
        // TODO(b/413711724): Further limit what this class does once it's disposed.
        if (mIsDisposed) {
            Log.i(TAG, "Ignoring repeated disposes");
            return;
        }

        Log.i(TAG, "Disposing resources");
        mEnvironment.dispose();
        mExtensions.clearSpatialStateCallback(mActivity);
        clearSpatialVisibilityChangedListener();
        mPerceivedResolutionChangedListeners.clear();
        // This will trigger clearing the callback from XrExtensions if it was registered
        updateExtensionsVisibilityCallback();

        // TODO: b/376934871 - Check async results.
        mExtensions.detachSpatialScene(mActivity, Runnable::run, (result) -> {});
        mActivity = null;
        mEntityManager.getAllEntities().forEach(Entity::dispose);
        mEntityManager.clear();
        if (mSplitEngineRenderer != null && mSplitEngineSubspaceManager != null) {
            mSplitEngineSubspaceManager.destroy();
            mSplitEngineRenderer.destroy();
        }
        mIsDisposed = true;
    }

    public void setSplitEngineSubspaceManager(
            @Nullable SplitEngineSubspaceManager splitEngineSubspaceManager) {
        mSplitEngineSubspaceManager = splitEngineSubspaceManager;
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

    /**
     * Get the underlying OpenXR session that backs perception.
     *
     * <p>The OpenXR session is created on JXR's primary thread so this may return {@code
     * XR_NULL_HANDLE} for a few frames at startup.
     *
     * @return the OpenXR XrSession, encoded in a jlong
     */
    public long getNativeSession() {
        Session session = mPerceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Perception session is uninitilazied, returning XR_NULL_HANDLE");
            return Session.XR_NULL_HANDLE;
        }

        long nativeSession = session.getNativeSession();
        if (nativeSession == Session.XR_NULL_HANDLE) {
            Log.w(TAG, "Perception session initialized, but native session not yet created");
            return Session.XR_NULL_HANDLE;
        }

        return nativeSession;
    }

    /**
     * Get the underlying OpenXR instance that backs perception.
     *
     * <p>The OpenXR instance is created on JXR's primary thread so this may return {@code
     * XR_NULL_HANDLE} for a few frames at startup.
     *
     * @return the OpenXR XrInstance, encoded in a jlong
     */
    public long getNativeInstance() {
        Session session = mPerceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Perception session is uninitilazied, returning XR_NULL_HANDLE");
            return Session.XR_NULL_HANDLE;
        }

        long nativeInstance = session.getNativeInstance();
        if (nativeInstance == Session.XR_NULL_HANDLE) {
            Log.w(TAG, "Perception session initialized, but native instance not yet created");
            return Session.XR_NULL_HANDLE;
        }

        return nativeInstance;
    }

    private SurfaceEntity createSurfaceEntitySplitEngine(
            @SurfaceEntity.StereoMode int stereoMode,
            SurfaceEntity.CanvasShape canvasShape,
            @SurfaceEntity.ContentSecurityLevel int contentSecurityLevel,
            @SurfaceEntity.SuperSampling int superSampling,
            Pose pose,
            @NonNull Entity parentEntity) {

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        SurfaceEntity entity =
                new SurfaceEntityImpl(
                        mActivity,
                        parentEntity,
                        mImpressApi,
                        mSplitEngineSubspaceManager,
                        mExtensions,
                        mEntityManager,
                        mExecutor,
                        stereoMode,
                        canvasShape,
                        contentSecurityLevel,
                        superSampling);
        entity.setPose(pose, Space.PARENT);
        return entity;
    }

    private GltfEntity createGltfEntitySplitEngine(
            Pose pose, GltfModelResource model, Entity parentEntity) {

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }
        if (parentEntity == null) {
            throw new IllegalArgumentException("parentEntity cannot be null");
        }
        GltfEntity entity =
                new GltfEntityImpl(
                        mActivity,
                        (GltfModelResourceImpl) model,
                        parentEntity,
                        mImpressApi,
                        mSplitEngineSubspaceManager,
                        mExtensions,
                        mEntityManager,
                        mExecutor);
        entity.setPose(pose, Space.PARENT);
        return entity;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private @Nullable ListenableFuture<GltfModelResource> loadGltfAsset(
            Supplier<ListenableFuture<Long>> modelLoader) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ResolvableFuture<GltfModelResource> gltfModelResourceFuture = ResolvableFuture.create();

        ListenableFuture<Long> gltfTokenFuture;
        try {
            gltfTokenFuture = modelLoader.get();
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to load glTF model: " + e.getMessage());
            return null;
        }

        gltfTokenFuture.addListener(
                () -> {
                    try {
                        long gltfToken = gltfTokenFuture.get();
                        gltfModelResourceFuture.set(getModelResourceFromToken(gltfToken));
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        Log.e(TAG, "Failed to load glTF model: " + e.getMessage());
                        gltfModelResourceFuture.setException(e);
                    }
                },
                mActivity::runOnUiThread);

        return gltfModelResourceFuture;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private @Nullable ListenableFuture<ExrImageResource> loadExrImage(
            Supplier<ListenableFuture<Long>> assetLoader) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ResolvableFuture<ExrImageResource> exrImageResourceFuture = ResolvableFuture.create();

        ListenableFuture<Long> exrImageTokenFuture;
        try {
            exrImageTokenFuture = assetLoader.get();
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to load EXR image: " + e.getMessage());
            return null;
        }

        exrImageTokenFuture.addListener(
                () -> {
                    try {
                        long exrImageToken = exrImageTokenFuture.get();
                        exrImageResourceFuture.set(getExrImageResourceFromToken(exrImageToken));
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        Log.e(TAG, "Failed to load EXR image: " + e.getMessage());
                        exrImageResourceFuture.setException(e);
                    }
                },
                mActivity::runOnUiThread);

        return exrImageResourceFuture;
    }

    @Override
    public @NonNull SubspaceNodeEntity createSubspaceNodeEntity(
            @NonNull SubspaceNodeHolder<?> subspaceNodeHolder, @NonNull Dimensions size) {
        SubspaceNodeEntityImpl subspaceNodeEntity =
                new SubspaceNodeEntityImpl(
                        mActivity,
                        mExtensions,
                        mEntityManager,
                        mExecutor,
                        SubspaceNodeHolder.assertGetValue(
                            subspaceNodeHolder, SubspaceNode.class).getSubspaceNode(),
                        size);
        subspaceNodeEntity.setParent(mActivitySpace);
        return subspaceNodeEntity;
    }

    @Override
    public void setSpatialModeChangeListener(
            @NonNull SpatialModeChangeListener SpatialModeChangeListener) {
        mSpatialModeChangeListener = SpatialModeChangeListener;
        mActivitySpace.setSpatialModeChangeListener(SpatialModeChangeListener);
    }

    @Override
    public @NonNull SpatialModeChangeListener getSpatialModeChangeListener() {
        return mSpatialModeChangeListener;
    }
}
