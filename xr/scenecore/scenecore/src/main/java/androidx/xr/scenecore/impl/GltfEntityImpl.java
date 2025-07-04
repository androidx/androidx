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

import android.content.Context;
import android.util.Log;

import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.GltfEntity;
import androidx.xr.runtime.internal.MaterialResource;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.NodeTransaction;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;
import com.google.ar.imp.apibindings.ImpressApi;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of a SceneCore GltfEntity.
 *
 * <p>This is used to create an entity that contains a glTF object.
 */
// TODO: b/375520647 - Add unit tests for this class.
class GltfEntityImpl extends AndroidXrEntity implements GltfEntity {
    private final ImpressApi mImpressApi;
    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager;
    private final SubspaceNode mSubspace;
    private final int mModelImpressNode;
    private final int mSubspaceImpressNode;
    @AnimationStateValue private int mAnimationState = AnimationState.STOPPED;

    GltfEntityImpl(
            Context context,
            GltfModelResourceImpl gltfModelResource,
            Entity parentEntity,
            ImpressApi impressApi,
            SplitEngineSubspaceManager splitEngineSubspaceManager,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(context, extensions.createNode(), extensions, entityManager, executor);
        mImpressApi = impressApi;
        mSplitEngineSubspaceManager = splitEngineSubspaceManager;
        setParent(parentEntity);

        // System will only render Impress nodes that are parented by this subspace node.
        mSubspaceImpressNode = impressApi.createImpressNode();
        String subspaceName = "gltf_entity_subspace_" + mSubspaceImpressNode;

        mSubspace = splitEngineSubspaceManager.createSubspace(subspaceName, mSubspaceImpressNode);

        if (mSubspace != null) {
            try (NodeTransaction transaction = extensions.createNodeTransaction()) {
                // Make the Entity node a parent of the subspace node.
                transaction.setParent(mSubspace.getSubspaceNode(), mNode).apply();
            }
        }
        mModelImpressNode =
                impressApi.instanceGltfModel(gltfModelResource.getExtensionModelToken());
        impressApi.setImpressNodeParent(mModelImpressNode, mSubspaceImpressNode);
        // The Impress node hierarchy is: Subspace Impress node --- parent of ---> model Impress
        // node.
        // The CPM node hierarchy is: Entity CPM node --- parent of ---> Subspace CPM node.
    }

    @Override
    public void startAnimation(boolean looping, @Nullable String animationName) {
        // TODO: b/362826747 - Add a listener interface so that the application can be
        // notified that the animation has stopped, been cancelled (by starting another animation)
        // and / or shown an error state if something went wrong.

        ListenableFuture<Void> future =
                mImpressApi.animateGltfModel(mModelImpressNode, animationName, looping);
        mAnimationState = AnimationState.PLAYING;

        // At the moment, we don't do anything interesting on failure except for logging. If we
        // didn't
        // care about logging the failure, we could just not register a listener at all if the
        // animation
        // is looping, since it will never terminate normally.
        future.addListener(
                () -> {
                    try {
                        future.get();
                        // The animation played to completion and has stopped
                        mAnimationState = AnimationState.STOPPED;
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            // If this happened, then it's likely Impress is shutting down and we
                            // need to
                            // shut down as well.
                            Thread.currentThread().interrupt();
                        } else {
                            // Some other error happened.  Log it and stop the animation.
                            Log.e("GltfEntityImpl", "Could not start animation: " + e);
                            mAnimationState = AnimationState.STOPPED;
                        }
                    }
                },
                mExecutor);
    }

    @Override
    public void stopAnimation() {
        if (mAnimationState == AnimationState.PLAYING) {
            mImpressApi.stopGltfModelAnimation(mModelImpressNode);
            mAnimationState = AnimationState.STOPPED;
        }
    }

    @Override
    @AnimationStateValue
    public int getAnimationState() {
        return mAnimationState;
    }

    @Override
    public void setMaterialOverride(@NonNull MaterialResource material, @NonNull String meshName) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setMaterialOverride(
                mModelImpressNode, ((MaterialResourceImpl) material).getMaterialToken(), meshName);
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        // Destroying the subspace will also destroy the underlying Impress nodes.
        if (mSubspace != null) {
            mSplitEngineSubspaceManager.deleteSubspace(mSubspace.subspaceId);
        }
        super.dispose();
    }

    public void setColliderEnabled(boolean enableCollider) {
        mImpressApi.setGltfModelColliderEnabled(mModelImpressNode, enableCollider);
    }
}
