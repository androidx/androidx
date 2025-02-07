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

package androidx.xr.scenecore.testing;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.ar.imp.apibindings.ImpressApi;
import com.google.ar.imp.view.View;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fake implementation of the JNI API for communicating with the Impress Split Engine instance for
 * testing purposes.
 */
public class FakeImpressApi implements ImpressApi {

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings("RestrictTo")
    static class AnimationInProgress {
        public String name;
        public ResolvableFuture<Void> fireOnDone;
    }

    /** Test bookkeeping data for a Android Surface */
    @SuppressWarnings({"ParcelCreator", "ParcelNotFinal"})
    public static class TestSurface extends Surface {
        public TestSurface(int id) {
            super(new SurfaceTexture(id));
        }
    }

    /** Test bookkeeping data for a StereoSurfaceEntity */
    public static class StereoSurfaceEntityData {
        /** Enum representing the different canvas shapes that can be created. */
        public enum CanvasShape {
            QUAD,
            VR_360_SPHERE,
            VR_180_HEMISPHERE
        }

        int mImpressNode;
        Surface mSurface;
        @StereoMode int mStereoMode;

        // This is a union of the CanvasShape parameters
        float mWidth;
        float mHeight;
        float mRadius;
        CanvasShape mCanvasShape;

        @Nullable
        public Surface getSurface() {
            return mSurface;
        }

        @StereoMode
        public int getStereoMode() {
            return mStereoMode;
        }

        public float getWidth() {
            return mWidth;
        }

        public float getHeight() {
            return mHeight;
        }

        public float getRadius() {
            return mRadius;
        }

        @Nullable
        public CanvasShape getCanvasShape() {
            return mCanvasShape;
        }
    }

    // Map of model tokens to the list of impress nodes that are instances of that model.
    private final Map<Long, List<Integer>> mGltfModels = new HashMap<>();
    // Map of impress nodes to their parent impress nodes.
    private final Map<Integer, Integer> mImpressNodes = new HashMap<>();

    // Map of impress nodes and animations that are currently playing (non looping)
    final Map<Integer, AnimationInProgress> mImpressAnimatedNodes = new HashMap<>();

    // Map of impress nodes and animations that are currently playing (looping)
    final Map<Integer, AnimationInProgress> mImpressLoopAnimatedNodes = new HashMap<>();

    // Map of impress entity nodes to their associated StereoSurfaceEntityData
    final Map<Integer, StereoSurfaceEntityData> mStereoSurfaceEntities = new HashMap<>();

    private int mNextModelId = 1;
    private int mNextNodeId = 1;

    @NonNull
    public Map<Integer, StereoSurfaceEntityData> getStereoSurfaceEntities() {
        return mStereoSurfaceEntities;
    }

    @Override
    public void setup(@NonNull View view) {}

    @Override
    public void onResume() {}

    @Override
    public void onPause() {}

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Long> loadGltfModel(@NonNull String name) {
        long modelToken = mNextModelId++;
        mGltfModels.put(modelToken, new ArrayList<>());
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        ResolvableFuture<Long> ret = ResolvableFuture.create();
        ret.set(modelToken);

        return ret;
    }

    @Override
    public void releaseGltfModel(long modelToken) {
        if (!mGltfModels.containsKey(modelToken)) {
            throw new IllegalArgumentException("Model token not found");
        }
        mGltfModels.remove(modelToken);
    }

    @Override
    public int instanceGltfModel(long modelToken) {
        return instanceGltfModel(modelToken, true);
    }

    @Override
    public int instanceGltfModel(long modelToken, boolean enableCollider) {
        if (!mGltfModels.containsKey(modelToken)) {
            throw new IllegalArgumentException("Model token not found");
        }
        int entityId = mNextNodeId++;
        mGltfModels.get(modelToken).add(entityId);
        mImpressNodes.put(entityId, null);
        return entityId;
    }

    @Override
    public void setGltfModelColliderEnabled(int impressNode, boolean enableCollider) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Void> animateGltfModel(
            int impressNode, @Nullable String animationName, boolean loop) {
        ResolvableFuture<Void> future = ResolvableFuture.create();
        if (mImpressNodes.get(impressNode) == null) {
            future.setException(new IllegalArgumentException("Impress node not found"));
            return future;
        }

        AnimationInProgress animationInProgress = new AnimationInProgress();
        animationInProgress.name = animationName;
        animationInProgress.fireOnDone = future;
        if (loop) {
            mImpressLoopAnimatedNodes.put(impressNode, animationInProgress);
        } else {
            mImpressAnimatedNodes.put(impressNode, animationInProgress);
        }
        return future;
    }

    @Override
    public void stopGltfModelAnimation(int impressNode) {
        if (mImpressNodes.get(impressNode) == null) {
            throw new IllegalArgumentException("Impress node not found");
        } else if (!mImpressAnimatedNodes.containsKey(impressNode)
                && !mImpressLoopAnimatedNodes.containsKey(impressNode)) {
            throw new IllegalArgumentException("Impress node is not animating");
        } else if (mImpressAnimatedNodes.containsKey(impressNode)) {
            mImpressAnimatedNodes.remove(impressNode);
        } else if (mImpressLoopAnimatedNodes.containsKey(impressNode)) {
            mImpressLoopAnimatedNodes.remove(impressNode);
        }
    }

    @Override
    public int createImpressNode() {
        int entityId = mNextNodeId++;
        mImpressNodes.put(entityId, null);
        return entityId;
    }

    @Override
    public void destroyImpressNode(int impressNode) {
        if (!mImpressNodes.containsKey(impressNode)) {
            throw new IllegalArgumentException("Impress node not found");
        }
        for (Map.Entry<Long, List<Integer>> pair : mGltfModels.entrySet()) {
            if (pair.getValue().contains(impressNode)) {
                pair.getValue().remove(Integer.valueOf(impressNode));
            }
        }
        for (Map.Entry<Integer, Integer> pair : mImpressNodes.entrySet()) {
            if (pair.getValue() != null && pair.getValue().equals((Integer) impressNode)) {
                pair.setValue(null);
            }
        }
        mImpressNodes.remove(impressNode);

        if (mStereoSurfaceEntities.containsKey(impressNode)) {
            mStereoSurfaceEntities.remove(impressNode);
        }
    }

    @Override
    public void setImpressNodeParent(int impressNodeChild, int impressNodeParent) {
        if (!mImpressNodes.containsKey(impressNodeChild)
                || !mImpressNodes.containsKey(impressNodeParent)) {
            throw new IllegalArgumentException("Impress node(s) not found");
        }
        mImpressNodes.put(impressNodeChild, impressNodeParent);
    }

    /** Gets the impress nodes for glTF models that match the given token. */
    @NonNull
    public List<Integer> getImpressNodesForToken(long modelToken) {
        return mGltfModels.get(modelToken);
    }

    /** Returns true if the given impress node has a parent. */
    public boolean impressNodeHasParent(int impressNode) {
        return mImpressNodes.containsKey(impressNode) && mImpressNodes.get(impressNode) != null;
    }

    /** Returns the parent impress node for the given impress node. */
    public int getImpressNodeParent(int impressNode) {
        return mImpressNodes.get(impressNode);
    }

    /** Returns the number of impress nodes that are currently animating. */
    public int impressNodeAnimatingSize() {
        return mImpressAnimatedNodes.size();
    }

    /** Returns the number of impress nodes that looping animations. */
    public int impressNodeLoopAnimatingSize() {
        return mImpressLoopAnimatedNodes.size();
    }

    @Override
    public int createStereoSurface(@StereoMode int stereoMode) {
        StereoSurfaceEntityData data = new StereoSurfaceEntityData();
        data.mImpressNode = createImpressNode();
        data.mSurface = new TestSurface(data.mImpressNode);
        data.mStereoMode = stereoMode;
        data.mCanvasShape = null;
        mStereoSurfaceEntities.put(data.mImpressNode, data);
        return data.mImpressNode;
    }

    /**
     * This method sets the canvas shape of a StereoSurfaceEntity using its Impress ID.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param width The width in local spatial units to set the quad to.
     * @param height The height in local spatial units to set the quad to.
     */
    @Override
    public void setStereoSurfaceEntityCanvasShapeQuad(int impressNode, float width, float height) {
        if (!mStereoSurfaceEntities.containsKey(impressNode)) {
            throw new IllegalArgumentException("Couldn't find stereo surface entity!");
        }
        StereoSurfaceEntityData data = mStereoSurfaceEntities.get(impressNode);
        data.mCanvasShape = StereoSurfaceEntityData.CanvasShape.QUAD;
        data.mWidth = width;
        data.mHeight = height;
    }

    /**
     * This method sets the canvas shape of a StereoSurfaceEntity using its Impress ID.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param radius The radius in local spatial units to set the sphere to.
     */
    @Override
    public void setStereoSurfaceEntityCanvasShapeSphere(int impressNode, float radius) {
        if (!mStereoSurfaceEntities.containsKey(impressNode)) {
            throw new IllegalArgumentException("Couldn't find stereo surface entity!");
        }
        StereoSurfaceEntityData data = mStereoSurfaceEntities.get(impressNode);
        data.mCanvasShape = StereoSurfaceEntityData.CanvasShape.VR_360_SPHERE;
        data.mRadius = radius;
    }

    /**
     * This method sets the canvas shape of a StereoSurfaceEntity using its Impress ID.
     *
     * @param impressNode The Impress node which hosts the StereoSurfaceEntity to be updated.
     * @param radius The radius in local spatial units of the hemisphere.
     */
    @Override
    public void setStereoSurfaceEntityCanvasShapeHemisphere(int impressNode, float radius) {
        StereoSurfaceEntityData data = mStereoSurfaceEntities.get(impressNode);
        data.mCanvasShape = StereoSurfaceEntityData.CanvasShape.VR_180_HEMISPHERE;
        data.mRadius = radius;
    }

    @Override
    @NonNull
    public Surface getSurfaceFromStereoSurface(int panelImpressNode) {
        if (!mStereoSurfaceEntities.containsKey(panelImpressNode)) {
            // TODO: b/387323937 - the Native code currently CHECK fails in this case
            throw new IllegalArgumentException("Couldn't find stereo surface entity!");
        }
        return mStereoSurfaceEntities.get(panelImpressNode).mSurface;
    }

    @Override
    public void setStereoModeForStereoSurface(int panelImpressNode, @StereoMode int mode) {
        if (!mStereoSurfaceEntities.containsKey(panelImpressNode)) {
            // TODO: b/387323937 - the Native code currently CHECK fails in this case
            throw new IllegalArgumentException("Couldn't find stereo surface entity!");
        }
        mStereoSurfaceEntities.get(panelImpressNode).mStereoMode = mode;
    }
}
