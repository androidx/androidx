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

package androidx.xr.scenecore.spatial.rendering;

import android.app.Activity;
import android.os.Looper;

import androidx.xr.scenecore.impl.impress.ExrImage;
import androidx.xr.scenecore.impl.impress.GltfModel;
import androidx.xr.scenecore.impl.impress.ImpressApi;
import androidx.xr.scenecore.impl.impress.ImpressNode;
import androidx.xr.scenecore.impl.impress.Material;
import androidx.xr.scenecore.runtime.ExrImageResource;
import androidx.xr.scenecore.runtime.GltfModelResource;
import androidx.xr.scenecore.runtime.MaterialResource;
import androidx.xr.scenecore.runtime.SpatialEnvironment.SpatialEnvironmentPreference;
import androidx.xr.scenecore.runtime.SpatialEnvironmentFeature;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransaction;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class SpatialEnvironmentFeatureImpl extends BaseRenderingFeature
        implements SpatialEnvironmentFeature, Consumer<Consumer<Node>> {
    public static final String GEOMETRY_NODE_NAME = "EnvironmentGeometryNode";
    private final AtomicReference<SpatialEnvironmentPreference> mSpatialEnvironmentPreference =
            new AtomicReference<>(null);
    private SubspaceNode mGeometrySubspaceSplitEngine;
    private ImpressNode mGeometrySubspaceImpressNode;
    private @NonNull Node mRootEnvironmentNode;
    private ImpressNode mGeometryImpressNode;
    private Material mMaterialOverride;
    private String mOverriddenNodeName;
    private @Nullable Consumer<Node> mOnBeforeNodeAttachedListener = null;
    private final @NonNull Activity mActivity;
    private boolean mIsDisposed = false;

    SpatialEnvironmentFeatureImpl(
            @NonNull Activity activity,
            @NonNull ImpressApi impressApi,
            @NonNull SplitEngineSubspaceManager splitEngineSubspaceManager,
            @NonNull XrExtensions extensions) {
        super(impressApi, splitEngineSubspaceManager, extensions);
        mActivity = activity;
        // Use mNode from parent
        mRootEnvironmentNode = mNode;
    }

    /**
     * Updates the system's preferred IBL asset. This applies a skybox that has been generated from
     * a preprocessed EXR image through SplitEngine. If skybox is null, this method clears the
     * preferred IBL selection, resulting in the system skybox being used.
     */
    private void applySkybox(@Nullable ExrImageResource skybox) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        mImpressApi.clearPreferredEnvironmentIblAsset();
        if (skybox != null) {
            mImpressApi.setPreferredEnvironmentLight(((ExrImage) skybox).getNativeHandle());
        }
    }

    /**
     * Stages updates to the CPM graph for the Environment to reflect a new geometry preference. If
     * geometry is null, this method unsets the client geometry preference, resulting in the system
     * geometry being used.
     *
     * @throws IllegalStateException if called on a thread other than the main thread.
     */
    private void applyGeometry(
            @Nullable GltfModelResource geometry,
            @Nullable MaterialResource material,
            @Nullable String nodeName,
            @Nullable String animationName) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        mGeometrySubspaceImpressNode = mImpressApi.createImpressNode();
        String subspaceName = "geometry_subspace_" + mGeometrySubspaceImpressNode.getHandle();

        mGeometrySubspaceSplitEngine =
                mSplitEngineSubspaceManager.createSubspace(
                        subspaceName, mGeometrySubspaceImpressNode.getHandle());

        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setName(mGeometrySubspaceSplitEngine.getSubspaceNode(), GEOMETRY_NODE_NAME)
                    .setPosition(mGeometrySubspaceSplitEngine.getSubspaceNode(), 0.0f, 0.0f, 0.0f)
                    .setScale(mGeometrySubspaceSplitEngine.getSubspaceNode(), 1.0f, 1.0f, 1.0f)
                    .setOrientation(
                            mGeometrySubspaceSplitEngine.getSubspaceNode(), 0.0f, 0.0f, 0.0f, 1.0f)
                    .apply();
        }

        if (geometry != null) {
            mGeometryImpressNode =
                    mImpressApi.instanceGltfModel(
                            ((GltfModel) geometry).getNativeHandle(), /* enableCollider= */ false);
            if (material != null && nodeName != null) {
                mMaterialOverride = (Material) material;
                mOverriddenNodeName = nodeName;
                mImpressApi.setMaterialOverride(
                        mGeometryImpressNode,
                        mMaterialOverride.getNativeHandle(),
                        mOverriddenNodeName,
                        /* primitiveIndex= */ 0);
            }
            if (animationName != null) {
                ListenableFuture<Void> unused =
                        mImpressApi.animateGltfModel(mGeometryImpressNode, animationName, true);
            }
            mImpressApi.setImpressNodeParent(mGeometryImpressNode, mGeometrySubspaceImpressNode);
        }
    }

    @Override
    public void accept(Consumer<Node> nodeConsumer) {
        mOnBeforeNodeAttachedListener = nodeConsumer;
    }

    @Override
    public @Nullable SpatialEnvironmentPreference getPreferredSpatialEnvironment() {
        return mSpatialEnvironmentPreference.get();
    }

    @Override
    public void setPreferredSpatialEnvironment(
            @Nullable SpatialEnvironmentPreference newPreference) {
        // This synchronized block makes sure following members are updated atomically:
        // mSpatialEnvironmentPreference, mRootEnvironmentNode, mExtensions,
        // mGeometrySubspaceSplitEngine, mGeometrySubspaceImpressNode.
        mSpatialEnvironmentPreference.getAndUpdate(
                prevPreference -> {
                    if (Objects.equals(newPreference, prevPreference)) {
                        return prevPreference;
                    }

                    GltfModelResource newGeometry =
                            newPreference == null ? null : newPreference.getGeometry();
                    GltfModelResource prevGeometry =
                            prevPreference == null ? null : prevPreference.getGeometry();
                    ExrImageResource newSkybox =
                            newPreference == null ? null : newPreference.getSkybox();
                    ExrImageResource prevSkybox =
                            prevPreference == null ? null : prevPreference.getSkybox();
                    MaterialResource newMaterial =
                            newPreference == null ? null : newPreference.getGeometryMaterial();
                    String newNodeName =
                            newPreference == null ? null : newPreference.getGeometryNodeName();
                    String newAnimationName =
                            newPreference == null ? null : newPreference.getGeometryAnimationName();

                    if (!Objects.equals(newGeometry, prevGeometry)) {
                        applyGeometry(newGeometry, newMaterial, newNodeName, newAnimationName);
                    }

                    // TODO: b/392948759 - Fix StrictMode violations triggered whenever skybox is
                    // set.
                    if (!Objects.equals(newSkybox, prevSkybox)
                            || (prevPreference == null && newPreference != null)) {
                        if (newSkybox == null) {
                            applySkybox(null);
                        } else {
                            applySkybox(newSkybox);
                        }
                    }

                    if (newPreference == null) {
                        // Detaching the app environment to go back to the system environment.
                        mExtensions.detachSpatialEnvironment(
                                mActivity, Runnable::run, (result) -> {});
                    } else {
                        // TODO(b/408276187): Add unit test that verifies that the skybox mode is
                        // correctly set.
                        int skyboxMode = XrExtensions.ENVIRONMENT_SKYBOX_APP;
                        if (newSkybox == null) {
                            skyboxMode = XrExtensions.NO_SKYBOX;
                        }
                        // Transitioning to a new app environment.
                        Node currentRootEnvironmentNode;
                        if (!Objects.equals(newGeometry, prevGeometry)) {
                            // Environment geometry has changed, create a new environment node and
                            // attach the geometry subspace to it.
                            currentRootEnvironmentNode = mExtensions.createNode();
                            if (mGeometrySubspaceSplitEngine != null) {
                                try (NodeTransaction transaction =
                                        mExtensions.createNodeTransaction()) {
                                    NodeTransaction unused =
                                            transaction.setParent(
                                                    mGeometrySubspaceSplitEngine.getSubspaceNode(),
                                                    currentRootEnvironmentNode);
                                    transaction.apply();
                                }
                            }
                        } else {
                            // Environment geometry has not changed, use the existing environment
                            // node.
                            currentRootEnvironmentNode = mRootEnvironmentNode;
                        }
                        if (mOnBeforeNodeAttachedListener != null) {
                            mOnBeforeNodeAttachedListener.accept(currentRootEnvironmentNode);
                        }
                        mExtensions.attachSpatialEnvironment(
                                mActivity,
                                currentRootEnvironmentNode,
                                skyboxMode,
                                Runnable::run,
                                (result) -> {
                                    // Update the root environment node to the current root node.
                                    mRootEnvironmentNode = currentRootEnvironmentNode;
                                });
                    }

                    return newPreference;
                });
    }

    @Override
    public void dispose() {
        if (mIsDisposed) return;
        mIsDisposed = true;

        super.dispose();
        if (mGeometrySubspaceSplitEngine != null) {
            if (mMaterialOverride != null) {
                mImpressApi.clearMaterialOverride(
                        mGeometryImpressNode, mOverriddenNodeName, /* primitiveIndex= */ 0);
            }
            try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
                transaction.setParent(mGeometrySubspaceSplitEngine.getSubspaceNode(), null).apply();
            }
            mSplitEngineSubspaceManager.deleteSubspace(mGeometrySubspaceSplitEngine.subspaceId);
            mGeometrySubspaceSplitEngine = null;
            mImpressApi.clearPreferredEnvironmentIblAsset();
        }

        mGeometrySubspaceSplitEngine = null;
        mGeometrySubspaceImpressNode = null;
        mRootEnvironmentNode = null;
        mSpatialEnvironmentPreference.set(null);
        // TODO: b/376934871 - Check async results.
        mExtensions.detachSpatialEnvironment(mActivity, Runnable::run, (result) -> {});
    }
}
