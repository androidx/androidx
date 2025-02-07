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

import android.app.Activity;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.xr.extensions.XrExtensionResult;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.environment.EnvironmentVisibilityState;
import androidx.xr.extensions.environment.PassthroughVisibilityState;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.passthrough.PassthroughState;
import androidx.xr.extensions.space.SpatialState;
import androidx.xr.scenecore.JxrPlatformAdapter.ExrImageResource;
import androidx.xr.scenecore.JxrPlatformAdapter.GltfModelResource;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;
import com.google.ar.imp.apibindings.ImpressApi;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Concrete implementation of SpatialEnvironment / XR Wallpaper for Android XR. */
// TODO(b/373435470): Remove "deprecation"
@SuppressWarnings({"deprecation", "BanSynchronizedMethods"})
final class SpatialEnvironmentImpl implements SpatialEnvironment {

    public static final String TAG = "SpatialEnvironmentImpl";

    public static final String SKYBOX_NODE_NAME = "EnvironmentSkyboxNode";
    public static final String GEOMETRY_NODE_NAME = "EnvironmentGeometryNode";
    public static final String PASSTHROUGH_NODE_NAME = "EnvironmentPassthroughNode";
    @VisibleForTesting final Node mPassthroughNode;
    private final XrExtensions mXrExtensions;
    private final Node mRootEnvironmentNode;
    private final boolean mUseSplitEngine;
    @Nullable private Activity mActivity;
    // Used to represent the geometry
    private Node mGeometryNode;
    // the "xrExtensions.setEnvironment" call effectively makes a node into a skybox
    private Node mSkyboxNode;
    private SubspaceNode mGeometrySubspaceSplitEngine;
    private int mGeometrySubspaceImpressNode;
    private boolean mIsSpatialEnvironmentPreferenceActive = false;
    @Nullable private SpatialEnvironmentPreference mSpatialEnvironmentPreference = null;

    // The active passthrough opacity value is updated with every opacity change event. A null value
    // indicates it has not yet been initialized and the value should be read from the
    // spatialStateProvider.
    private Float mActivePassthroughOpacity = null;
    // Initialized to null to let system control opacity until preference is explicitly set.
    private Float mPassthroughOpacityPreference = null;
    private SplitEngineSubspaceManager mSplitEngineSubspaceManager;
    private ImpressApi mImpressApi;
    private final Supplier<SpatialState> mSpatialStateProvider;
    private SpatialState mPreviousSpatialState = null;

    private final Set<Consumer<Boolean>> mOnSpatialEnvironmentChangedListeners =
            Collections.synchronizedSet(new HashSet<>());

    private final Set<Consumer<Float>> mOnPassthroughOpacityChangedListeners =
            Collections.synchronizedSet(new HashSet<>());

    // Assets used to turn the skybox black when devs pass in null for the skybox.
    private ListenableFuture<ExrImageResource> mNullSkyboxResourceFuture;
    private ExrImageResource mNullSkyboxResource = null;

    SpatialEnvironmentImpl(
            @NonNull Activity activity,
            @NonNull XrExtensions xrExtensions,
            @NonNull Node rootSceneNode,
            @NonNull Supplier<SpatialState> spatialStateProvider,
            @NonNull ListenableFuture<ExrImageResource> nullSkyboxResourceFuture,
            boolean useSplitEngine) {
        mActivity = activity;
        mXrExtensions = xrExtensions;
        mPassthroughNode = xrExtensions.createNode();
        mRootEnvironmentNode = xrExtensions.createNode();
        mGeometryNode = xrExtensions.createNode();
        mSkyboxNode = xrExtensions.createNode();
        mUseSplitEngine = useSplitEngine;
        mSpatialStateProvider = spatialStateProvider;
        mNullSkyboxResourceFuture = nullSkyboxResourceFuture;

        try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
            transaction
                    .setName(mGeometryNode, GEOMETRY_NODE_NAME)
                    .setName(mSkyboxNode, SKYBOX_NODE_NAME)
                    .setName(mPassthroughNode, PASSTHROUGH_NODE_NAME)
                    .setParent(mGeometryNode, mRootEnvironmentNode)
                    .setParent(mSkyboxNode, mRootEnvironmentNode)
                    .setParent(mPassthroughNode, rootSceneNode)
                    .apply();
        }
    }

    // TODO: Remove these once we know the Equals() and Hashcode() methods are correct.
    boolean hasEnvironmentVisibilityChanged(@NonNull SpatialState spatialState) {
        if (mPreviousSpatialState == null) {
            return true;
        }

        final EnvironmentVisibilityState previousEnvironmentVisibility =
                mPreviousSpatialState.getEnvironmentVisibility();
        final EnvironmentVisibilityState currentEnvironmentVisibility =
                spatialState.getEnvironmentVisibility();

        if (previousEnvironmentVisibility.getCurrentState()
                != currentEnvironmentVisibility.getCurrentState()) {
            return true;
        }

        return false;
    }

    // TODO: Remove these once we know the Equals() and Hashcode() methods are correct.
    boolean hasPassthroughVisibilityChanged(@NonNull SpatialState spatialState) {
        if (mPreviousSpatialState == null) {
            return true;
        }

        final PassthroughVisibilityState previousPassthroughVisibility =
                mPreviousSpatialState.getPassthroughVisibility();
        final PassthroughVisibilityState currentPassthroughVisibility =
                spatialState.getPassthroughVisibility();

        if (previousPassthroughVisibility.getCurrentState()
                != currentPassthroughVisibility.getCurrentState()) {
            return true;
        }

        if (previousPassthroughVisibility.getOpacity()
                != currentPassthroughVisibility.getOpacity()) {
            return true;
        }

        return false;
    }

    // Package Private enum to return which spatial states have changed.
    enum ChangedSpatialStates {
        ENVIRONMENT_CHANGED,
        PASSTHROUGH_CHANGED
    }

    // Package Private method to set the current passthrough opacity and
    // isSpatialEnvironmentPreferenceActive from JxrPlatformAdapterAxr.
    // This method is synchronized because it sets several internal state variables at once, which
    // should be treated as an atomic set. We could consider replacing with AtomicReferences.
    @CanIgnoreReturnValue
    synchronized EnumSet<ChangedSpatialStates> setSpatialState(@NonNull SpatialState spatialState) {
        EnumSet<ChangedSpatialStates> changedSpatialStates =
                EnumSet.noneOf(ChangedSpatialStates.class);
        boolean passthroughVisibilityChanged = hasPassthroughVisibilityChanged(spatialState);
        if (passthroughVisibilityChanged) {
            changedSpatialStates.add(ChangedSpatialStates.PASSTHROUGH_CHANGED);
            mActivePassthroughOpacity =
                    RuntimeUtils.getPassthroughOpacity(spatialState.getPassthroughVisibility());
        }

        // TODO: b/371082454 - Check if the app is in FSM to ensure APP_VISIBLE refers to the
        // current
        // app and not another app that is visible.
        boolean environmentVisibilityChanged = hasEnvironmentVisibilityChanged(spatialState);
        if (environmentVisibilityChanged) {
            changedSpatialStates.add(ChangedSpatialStates.ENVIRONMENT_CHANGED);
            mIsSpatialEnvironmentPreferenceActive =
                    RuntimeUtils.getIsSpatialEnvironmentPreferenceActive(
                            spatialState.getEnvironmentVisibility().getCurrentState());
        }

        mPreviousSpatialState = spatialState;
        return changedSpatialStates;
    }

    /** Flushes passthrough Node state to XrExtensions. */
    private void applyPassthroughChange(float opacityVal) {
        if (opacityVal > 0.0f) {
            try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
                transaction
                        .setPassthroughState(
                                mPassthroughNode, opacityVal, PassthroughState.PASSTHROUGH_MODE_MAX)
                        .apply();
            }
        } else {
            try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
                transaction
                        .setPassthroughState(
                                mPassthroughNode,
                                /* passthroughOpacity= */ 0.0f,
                                PassthroughState.PASSTHROUGH_MODE_OFF)
                        .apply();
            }
        }
    }

    @Override
    @NonNull
    @CanIgnoreReturnValue
    public SetPassthroughOpacityPreferenceResult setPassthroughOpacityPreference(
            @Nullable Float opacity) {
        // To work around floating-point precision issues, the opacity preference is documented to
        // clamp
        // to 0.0f if it is set below 1% opacity and it clamps to 1.0f if it is set above 99%
        // opacity.

        @Nullable
        Float newPassthroughOpacityPreference =
                opacity == null
                        ? null
                        : (opacity < 0.01f ? 0.0f : (opacity > 0.99f ? 1.0f : opacity));

        if (Objects.equals(newPassthroughOpacityPreference, mPassthroughOpacityPreference)) {
            return SetPassthroughOpacityPreferenceResult.CHANGE_APPLIED;
        }

        mPassthroughOpacityPreference = newPassthroughOpacityPreference;

        // to this method when they are removed

        // Passthrough should be enabled only if the user has explicitly set the
        // PassthroughOpacityPreference to a non-null and non-zero value, otherwise disabled.
        if (mPassthroughOpacityPreference != null && mPassthroughOpacityPreference != 0.0f) {
            applyPassthroughChange(mPassthroughOpacityPreference.floatValue());
        } else {
            applyPassthroughChange(0.0f);
        }

        if (RuntimeUtils.convertSpatialCapabilities(
                        mSpatialStateProvider.get().getSpatialCapabilities())
                .hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL)) {
            return SetPassthroughOpacityPreferenceResult.CHANGE_APPLIED;
        } else {
            return SetPassthroughOpacityPreferenceResult.CHANGE_PENDING;
        }
    }

    // Synchronized because we may need to update the entire Spatial State if the opacity has not
    // been
    // initialized previously.
    @Override
    public synchronized float getCurrentPassthroughOpacity() {
        if (mActivePassthroughOpacity == null) {
            setSpatialState(mSpatialStateProvider.get());
        }
        return mActivePassthroughOpacity.floatValue();
    }

    @Override
    @Nullable
    public Float getPassthroughOpacityPreference() {
        return mPassthroughOpacityPreference;
    }

    // This is called on the Activity's UI thread - so we should be careful to not block it.
    synchronized void firePassthroughOpacityChangedEvent(float opacity) {
        for (Consumer<Float> listener : mOnPassthroughOpacityChangedListeners) {
            listener.accept(opacity);
        }
    }

    @Override
    public void addOnPassthroughOpacityChangedListener(Consumer<Float> listener) {
        mOnPassthroughOpacityChangedListeners.add(listener);
    }

    @Override
    public void removeOnPassthroughOpacityChangedListener(Consumer<Float> listener) {
        mOnPassthroughOpacityChangedListeners.remove(listener);
    }

    /**
     * Stages updates to the CPM graph for the Environment to reflect a new skybox preference. If
     * skybox is null, this method unsets the client skybox preference, resulting in the system
     * skybox being used.
     */
    private void applySkybox(@Nullable ExrImageResourceImpl skybox) {
        // We need to create a new node here because we can't re-use the old CPM node when changing
        // geometry and skybox.
        try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
            transaction.setParent(mSkyboxNode, null).apply();
        }

        mSkyboxNode = mXrExtensions.createNode();
        try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
            transaction
                    .setName(mSkyboxNode, SKYBOX_NODE_NAME)
                    .setParent(mSkyboxNode, mRootEnvironmentNode);
            if (skybox != null) {
                transaction.setEnvironment(mSkyboxNode, skybox.getToken());
            }
            transaction.apply();
        }
    }

    /**
     * Stages updates to the CPM graph for the Environment to reflect a new geometry preference. If
     * geometry is null, this method unsets the client geometry preference, resulting in the system
     * geometry being used.
     */
    private void applyGeometryLegacy(@Nullable GltfModelResourceImpl geometry) {
        // We need to create a new node here because we can't re-use the old CPM node when changing
        // geometry and skybox.
        try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
            transaction.setParent(mGeometryNode, null).apply();
        }
        mGeometryNode = mXrExtensions.createNode();
        try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
            transaction
                    .setName(mGeometryNode, GEOMETRY_NODE_NAME)
                    .setParent(mGeometryNode, mRootEnvironmentNode);
            if (geometry != null) {
                transaction.setGltfModel(mGeometryNode, geometry.getExtensionModelToken());
            }
            transaction.apply();
        }
    }

    /**
     * Stages updates to the CPM graph for the Environment to reflect a new geometry preference. If
     * geometry is null, this method unsets the client geometry preference, resulting in the system
     * geometry being used.
     *
     * @throws IllegalStateException if called on a thread other than the main thread.
     */
    private void applyGeometrySplitEngine(@Nullable GltfModelResourceImplSplitEngine geometry) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        int prevGeometrySubspaceImpressNode = -1;
        SubspaceNode prevGeometrySubspaceSplitEngine = null;
        if (mGeometrySubspaceSplitEngine != null) {
            prevGeometrySubspaceSplitEngine = mGeometrySubspaceSplitEngine;
            mGeometrySubspaceSplitEngine = null;
            prevGeometrySubspaceImpressNode = mGeometrySubspaceImpressNode;
            mGeometrySubspaceImpressNode = -1;
        }

        mGeometrySubspaceImpressNode = mImpressApi.createImpressNode();
        String subspaceName = "geometry_subspace_" + mGeometrySubspaceImpressNode;

        mGeometrySubspaceSplitEngine =
                mSplitEngineSubspaceManager.createSubspace(
                        subspaceName, mGeometrySubspaceImpressNode);

        try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
            transaction
                    .setName(mGeometrySubspaceSplitEngine.subspaceNode, GEOMETRY_NODE_NAME)
                    .setParent(mGeometrySubspaceSplitEngine.subspaceNode, mRootEnvironmentNode)
                    .setPosition(mGeometrySubspaceSplitEngine.subspaceNode, 0.0f, 0.0f, 0.0f)
                    .setScale(mGeometrySubspaceSplitEngine.subspaceNode, 1.0f, 1.0f, 1.0f)
                    .setOrientation(
                            mGeometrySubspaceSplitEngine.subspaceNode, 0.0f, 0.0f, 0.0f, 1.0f)
                    .apply();
        }

        if (geometry != null) {
            int modelImpressNode =
                    mImpressApi.instanceGltfModel(
                            geometry.getExtensionModelToken(), /* enableCollider= */ false);
            mImpressApi.setImpressNodeParent(modelImpressNode, mGeometrySubspaceImpressNode);
        }

        if (prevGeometrySubspaceSplitEngine != null && prevGeometrySubspaceImpressNode != -1) {
            // Detach the previous geometry subspace from the root environment node.
            try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
                transaction.setParent(prevGeometrySubspaceSplitEngine.subspaceNode, null).apply();
            }
            // Destroying the subspace will also destroy the underlying Impress node for the
            // Environment
            // geometry.
            mSplitEngineSubspaceManager.deleteSubspace(prevGeometrySubspaceSplitEngine.subspaceId);

            prevGeometrySubspaceSplitEngine = null;
            prevGeometrySubspaceImpressNode = -1;
        }
    }

    void onSplitEngineReady(SplitEngineSubspaceManager subspaceManager, ImpressApi api) {
        mSplitEngineSubspaceManager = subspaceManager;
        mImpressApi = api;
    }

    @Override
    @NonNull
    @CanIgnoreReturnValue
    public SetSpatialEnvironmentPreferenceResult setSpatialEnvironmentPreference(
            @Nullable SpatialEnvironmentPreference newPreference) {
        // TODO: b/378914007 This method is not safe for reentrant calls.

        if (Objects.equals(newPreference, mSpatialEnvironmentPreference)) {
            return SetSpatialEnvironmentPreferenceResult.CHANGE_APPLIED;
        }

        GltfModelResource newGeometry = newPreference == null ? null : newPreference.geometry;
        GltfModelResource prevGeometry =
                mSpatialEnvironmentPreference == null
                        ? null
                        : mSpatialEnvironmentPreference.geometry;
        ExrImageResource newSkybox = newPreference == null ? null : newPreference.skybox;
        ExrImageResource prevSkybox =
                mSpatialEnvironmentPreference == null ? null : mSpatialEnvironmentPreference.skybox;

        // TODO(b/329907079): Map GltfModelResourceImplSplitEngine to GltfModelResource in Impl
        // Layer
        if (newGeometry != null) {
            if (mUseSplitEngine && !(newGeometry instanceof GltfModelResourceImplSplitEngine)) {
                throw new IllegalArgumentException(
                        "SplitEngine is enabled but the prefererred geometry is not of type"
                                + " GltfModelResourceImplSplitEngine.");
            } else if (!mUseSplitEngine && !(newGeometry instanceof GltfModelResourceImpl)) {
                throw new IllegalArgumentException(
                        "SplitEngine is disabled but the prefererred geometry is not of type"
                                + " GltfModelResourceImpl.");
            }
        }

        // TODO b/329907079: Map ExrImageResourceImpl to ExrImageResource in Impl Layer
        if (newSkybox != null && !(newSkybox instanceof ExrImageResourceImpl)) {
            throw new IllegalArgumentException(
                    "The prefererred skybox is not of type ExrImageResourceImpl.");
        }

        if (!Objects.equals(newGeometry, prevGeometry)) {
            // TODO: b/354711945 - Remove this check once we migrate completely to SplitEngine
            if (mUseSplitEngine) {
                applyGeometrySplitEngine((GltfModelResourceImplSplitEngine) newGeometry);
            } else {
                applyGeometryLegacy((GltfModelResourceImpl) newGeometry);
            }
        }

        // TODO: b/392948759 - Fix StrictMode violations triggered whenever skybox is set.
        if (!Objects.equals(newSkybox, prevSkybox)
                || (mSpatialEnvironmentPreference == null && newPreference != null)) {
            // If the preference object is non-null but contains a null skybox, we set a black
            // skybox.
            if (mNullSkyboxResourceFuture == null) {
                Log.e(TAG, "Failed to get null skybox resource.");
            } else if (newSkybox == null && newPreference != null) {
                // Lazy initialization of the null skybox resource.
                if (mNullSkyboxResource == null) {
                    try {
                        mNullSkyboxResource = mNullSkyboxResourceFuture.get();
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e(TAG, "Failed to get null skybox resource.");
                    }
                }
                // Set the skybox to a black texture
                if (mNullSkyboxResource != null) {
                    newSkybox = mNullSkyboxResource;
                }
            }
            applySkybox((ExrImageResourceImpl) newSkybox);
        }

        try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
            if (newSkybox == null && newGeometry == null) {
                mXrExtensions.detachSpatialEnvironment(
                        mActivity,
                        (result) -> logXrExtensionResult("detachSpatialEnvironment", result),
                        Runnable::run);
            } else {
                mXrExtensions.attachSpatialEnvironment(
                        mActivity,
                        mRootEnvironmentNode,
                        (result) -> logXrExtensionResult("attachSpatialEnvironment", result),
                        Runnable::run);
            }
        }

        mSpatialEnvironmentPreference = newPreference;

        if (RuntimeUtils.convertSpatialCapabilities(
                        mSpatialStateProvider.get().getSpatialCapabilities())
                .hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)) {
            return SetSpatialEnvironmentPreferenceResult.CHANGE_APPLIED;
        } else {
            return SetSpatialEnvironmentPreferenceResult.CHANGE_PENDING;
        }
    }

    private void logXrExtensionResult(String prefix, XrExtensionResult result) {
        // TODO: b/376934871 - Better error handling?
        switch (result.getResult()) {
            case XrExtensionResult.XR_RESULT_SUCCESS:
            case XrExtensionResult.XR_RESULT_SUCCESS_NOT_VISIBLE:
                Log.d(TAG, prefix + ": success (" + result.getResult() + ")");
                break;
            case XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED:
                Log.d(TAG, prefix + ": ignored, already applied (" + result.getResult() + ")");
                break;
            case XrExtensionResult.XR_RESULT_ERROR_NOT_ALLOWED:
            case XrExtensionResult.XR_RESULT_ERROR_SYSTEM:
                Log.e(TAG, prefix + ": error (" + result.getResult() + ")");
                break;
            default:
                Log.e(TAG, prefix + ": Unexpected return value (" + result.getResult() + ")");
                break;
        }
    }

    @Override
    @Nullable
    public SpatialEnvironmentPreference getSpatialEnvironmentPreference() {
        return mSpatialEnvironmentPreference;
    }

    @Override
    public boolean isSpatialEnvironmentPreferenceActive() {
        return mIsSpatialEnvironmentPreferenceActive;
    }

    // This is called on the Activity's UI thread - so we should be careful to not block it.
    synchronized void fireOnSpatialEnvironmentChangedEvent(
            boolean isSpatialEnvironmentPreferenceActive) {
        for (Consumer<Boolean> listener : mOnSpatialEnvironmentChangedListeners) {
            listener.accept(isSpatialEnvironmentPreferenceActive);
        }
    }

    @Override
    public void addOnSpatialEnvironmentChangedListener(Consumer<Boolean> listener) {
        mOnSpatialEnvironmentChangedListeners.add(listener);
    }

    @Override
    public void removeOnSpatialEnvironmentChangedListener(Consumer<Boolean> listener) {
        mOnSpatialEnvironmentChangedListeners.remove(listener);
    }

    /**
     * Disposes of the environment and all of its resources.
     *
     * <p>This should be called when the environment is no longer needed.
     */
    public void dispose() {
        if (mUseSplitEngine) {
            if (mGeometrySubspaceSplitEngine != null) {
                try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
                    transaction.setParent(mGeometrySubspaceSplitEngine.subspaceNode, null).apply();
                }
                mSplitEngineSubspaceManager.deleteSubspace(mGeometrySubspaceSplitEngine.subspaceId);
                mGeometrySubspaceSplitEngine = null;
                mImpressApi.destroyImpressNode(mGeometrySubspaceImpressNode);
            }
        }
        mActivePassthroughOpacity = null;
        mPassthroughOpacityPreference = null;
        try (NodeTransaction transaction = mXrExtensions.createNodeTransaction()) {
            transaction
                    .setParent(mSkyboxNode, null)
                    .setParent(mGeometryNode, null)
                    .setParent(mPassthroughNode, null)
                    .apply();
        }
        mGeometrySubspaceSplitEngine = null;
        mGeometrySubspaceImpressNode = 0;
        mSplitEngineSubspaceManager = null;
        mImpressApi = null;
        mSpatialEnvironmentPreference = null;
        mIsSpatialEnvironmentPreferenceActive = false;
        mOnPassthroughOpacityChangedListeners.clear();
        mOnSpatialEnvironmentChangedListeners.clear();
        mNullSkyboxResourceFuture = null;
        mNullSkyboxResource = null;
        // TODO: b/376934871 - Check async results.
        mXrExtensions.detachSpatialEnvironment(mActivity, (result) -> {}, Runnable::run);
        mActivity = null;
    }
}
