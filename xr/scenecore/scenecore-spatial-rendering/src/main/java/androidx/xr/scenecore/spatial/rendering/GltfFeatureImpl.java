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

import android.util.Log;

import androidx.xr.runtime.math.BoundingBox;
import androidx.xr.runtime.math.FloatSize3d;
import androidx.xr.scenecore.impl.impress.GltfModel;
import androidx.xr.scenecore.impl.impress.ImpressApi;
import androidx.xr.scenecore.impl.impress.ImpressNode;
import androidx.xr.scenecore.impl.impress.Material;
import androidx.xr.scenecore.runtime.GltfEntity;
import androidx.xr.scenecore.runtime.GltfFeature;
import androidx.xr.scenecore.runtime.MaterialResource;

import com.android.extensions.xr.XrExtensions;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Implementation of a SceneCore GltfEntity.
 *
 * <p>This is used to create an entity that contains a glTF object.
 */
// TODO: b/375520647 - Add unit tests for this class.
class GltfFeatureImpl extends BaseRenderingFeature implements GltfFeature {
    private final ImpressNode mModelImpressNode;
    @GltfEntity.AnimationStateValue private int mAnimationState = GltfEntity.AnimationState.STOPPED;
    private final Map<String, Integer> meshOverrides = new HashMap<>();

    GltfFeatureImpl(
            GltfModel gltfModel,
            ImpressApi impressApi,
            SplitEngineSubspaceManager splitEngineSubspaceManager,
            XrExtensions extensions) {
        super(impressApi, splitEngineSubspaceManager, extensions);

        mModelImpressNode = impressApi.instanceGltfModel(((GltfModel) gltfModel).getNativeHandle());
        bindImpressNodeToSubspace("gltf_entity_subspace_", mModelImpressNode);
    }

    @Override
    @NonNull
    public FloatSize3d getSize() {
        return getGltfModelBoundingBox().getHalfExtents().times(2);
    }

    @Override
    @NonNull
    public BoundingBox getGltfModelBoundingBox() {
        return mImpressApi.getGltfModelBoundingBox(mModelImpressNode);
    }

    @Override
    public void startAnimation(
            boolean looping, @Nullable String animationName, @NonNull Executor executor) {
        // TODO: b/362826747 - Add a listener interface so that the application can be
        // notified that the animation has stopped, been cancelled (by starting another animation)
        // and / or shown an error state if something went wrong.

        ListenableFuture<Void> future =
                mImpressApi.animateGltfModel(mModelImpressNode, animationName, looping);
        mAnimationState = GltfEntity.AnimationState.PLAYING;

        // At the moment, we don't do anything interesting on failure except for logging. If we
        // didn't care about logging the failure, we could just not register a listener at all if
        // the animation is looping, since it will never terminate normally.
        future.addListener(
                () -> {
                    try {
                        future.get();
                        // The animation played to completion and has stopped
                        mAnimationState = GltfEntity.AnimationState.STOPPED;
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            // If this happened, then it's likely Impress is shutting down and we
                            // need to shut down as well.
                            Thread.currentThread().interrupt();
                        } else {
                            // Some other error happened.  Log it and stop the animation.
                            Log.e("GltfEntityImpl", "Could not start animation: " + e);
                            mAnimationState = GltfEntity.AnimationState.STOPPED;
                        }
                    }
                },
                executor);
    }

    @Override
    public void stopAnimation() {
        if (mAnimationState == GltfEntity.AnimationState.PLAYING) {
            mImpressApi.stopGltfModelAnimation(mModelImpressNode);
            mAnimationState = GltfEntity.AnimationState.STOPPED;
        }
    }

    @Override
    @GltfEntity.AnimationStateValue
    public int getAnimationState() {
        return mAnimationState;
    }

    @Override
    public void setMaterialOverride(
            @NonNull MaterialResource material, @NonNull String nodeName, int primitiveIndex) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setMaterialOverride(
                mModelImpressNode,
                ((Material) material).getNativeHandle(),
                nodeName,
                primitiveIndex);
        meshOverrides.put(nodeName, primitiveIndex);
    }

    @Override
    public void clearMaterialOverride(@NonNull String nodeName, int primitiveIndex) {
        mImpressApi.clearMaterialOverride(mModelImpressNode, nodeName, primitiveIndex);
        meshOverrides.remove(nodeName, primitiveIndex);
    }

    @Override
    public void setColliderEnabled(boolean enableCollider) {
        mImpressApi.setGltfModelColliderEnabled(mModelImpressNode, enableCollider);
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        for (Map.Entry<String, Integer> entry : new HashMap<>(meshOverrides).entrySet()) {
            mImpressApi.clearMaterialOverride(mModelImpressNode, entry.getKey(), entry.getValue());
        }
        meshOverrides.clear();
        super.dispose();
    }
}
