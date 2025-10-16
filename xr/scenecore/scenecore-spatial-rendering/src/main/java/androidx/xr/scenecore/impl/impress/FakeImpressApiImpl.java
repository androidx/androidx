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

package androidx.xr.scenecore.impl.impress;

import android.content.res.Resources.NotFoundException;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec;
import androidx.xr.scenecore.runtime.TextureSampler;

import com.google.ar.imp.view.View;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fake implementation of the JNI API for communicating with the Impress Split Engine instance for
 * testing purposes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeImpressApiImpl implements ImpressApi {
    static class AnimationInProgress {
        public String name;
        public ResolvableFuture<Void> fireOnDone;
    }

    /** Test bookkeeping data for a Android Surface */
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

        ImpressNode mImpressNode;
        Surface mSurface;
        boolean mUseSuperSampling;
        @StereoMode int mStereoMode;
        // This is a union of the CanvasShape parameters
        float mWidth;
        float mHeight;
        float mRadius;
        CanvasShape mCanvasShape;
        float mFeatherRadiusX;
        float mFeatherRadiusY;

        boolean mColliderEnabled;

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

        public float getFeatherRadiusX() {
            return mFeatherRadiusX;
        }

        public float getFeatherRadiusY() {
            return mFeatherRadiusY;
        }

        @Nullable
        public CanvasShape getCanvasShape() {
            return mCanvasShape;
        }

        public boolean getColliderEnabled() {
            return mColliderEnabled;
        }
    }

    /** Test bookkeeping data for a Material */
    public static class MaterialData {
        /** Enum representing the different built-in material types that can be created. */
        public enum Type {
            GENERIC,
            WATER,
            WATER_ALPHA,
            KHRONOS_PBR
        }

        @NonNull Type mType;
        long mMaterialHandle;

        public MaterialData(@NonNull Type type, long materialHandle) {
            this.mType = type;
            this.mMaterialHandle = materialHandle;
        }

        @NonNull
        public Type getType() {
            return mType;
        }

        public long getMaterialHandle() {
            return mMaterialHandle;
        }
    }

    /** Test bookkeeping data for a Gltf gltfToken */
    public static class GltfNodeData {
        int mEntityId;
        @Nullable MaterialData mMaterialOverride;

        public void setEntityId(int entityId) {
            this.mEntityId = entityId;
        }

        /** Sets the material override for a specific mesh of a node */
        public void setMaterialOverride(
                @Nullable MaterialData materialOverride,
                @NonNull String nodeName,
                int primitiveIndex) {
            this.mMaterialOverride = materialOverride;
        }

        /** Clears a material override for a specific mesh of a node. */
        public void clearMaterialOverride(@NonNull String nodeName, int primitiveIndex) {
            this.mMaterialOverride = null;
        }

        public int getEntityId() {
            return mEntityId;
        }

        @Nullable
        public MaterialData getMaterialOverride() {
            return mMaterialOverride;
        }
    }

    // Non-functional resource manager.
    private final BindingsResourceManager mResourceManager =
            new BindingsResourceManager(new Handler(Looper.getMainLooper()));
    // Vector of image based lighting asset tokens.
    private final List<Long> mImageBasedLightingAssets = new ArrayList<>();
    // Map of model tokens to the list of impress nodes that are instances of that model.
    private final Map<Long, List<Integer>> mGltfModels = new HashMap<>();
    // Map of impress nodes to their parent impress nodes.
    private final Map<GltfNodeData, GltfNodeData> mImpressNodes = new HashMap<>();
    // Map of impress nodes and animations that are currently playing (non looping)
    final Map<ImpressNode, AnimationInProgress> mImpressAnimatedNodes = new HashMap<>();
    // Map of impress nodes and animations that are currently playing (looping)
    final Map<ImpressNode, AnimationInProgress> mImpressLoopAnimatedNodes = new HashMap<>();
    // Map of impress entity nodes to their associated StereoSurfaceEntityData
    final Map<ImpressNode, StereoSurfaceEntityData> mStereoSurfaceEntities = new HashMap<>();
    // Map of texture image tokens to their associated Texture object
    final Map<Long, Texture> mTextureImages = new HashMap<>();
    // Map of material tokens to their associated MaterialData object
    final Map<Long, MaterialData> mMaterials = new HashMap<>();
    private int mNextImageBasedLightingAssetId = 1;
    private int mNextModelId = 1;
    private int mNextNodeId = 1;
    private long mNextTextureId = 1;
    private long mNextMaterialId = 1;
    private long mCurrentEnvironmentLightId = -1;

    @NonNull
    public Map<ImpressNode, StereoSurfaceEntityData> getStereoSurfaceEntities() {
        return mStereoSurfaceEntities;
    }

    @Override
    public void setup(@Nullable View view) {}

    @Override
    public void onResume() {}

    @Override
    public void onPause() {}

    @Override
    @NonNull
    public BindingsResourceManager getBindingsResourceManager() {
        if (mResourceManager == null) {
            throw new IllegalStateException("BindingsResourceManager is not initialized");
        }
        return mResourceManager;
    }

    @Override
    public void releaseImageBasedLightingAsset(long iblToken) {
        if (!mImageBasedLightingAssets.contains(iblToken)) {
            throw new NotFoundException("Image based lighting asset token not found");
        }
        mImageBasedLightingAssets.remove(iblToken);
    }

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Long> loadImageBasedLightingAsset(@NonNull String path) {
        long imageBasedLightingAssetToken = mNextImageBasedLightingAssetId++;
        mImageBasedLightingAssets.add(imageBasedLightingAssetToken);
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        ResolvableFuture<Long> ret = ResolvableFuture.create();
        ret.set(imageBasedLightingAssetToken);
        return ret;
    }

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Long> loadImageBasedLightingAsset(
            byte @NonNull [] data, @NonNull String key) {
        long imageBasedLightingAssetToken = mNextImageBasedLightingAssetId++;
        mImageBasedLightingAssets.add(imageBasedLightingAssetToken);
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        ResolvableFuture<Long> ret = ResolvableFuture.create();
        ret.set(imageBasedLightingAssetToken);
        return ret;
    }

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Long> loadGltfAsset(@NonNull String path) {
        long gltfToken = mNextModelId++;
        mGltfModels.put(gltfToken, new ArrayList<>());
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        ResolvableFuture<Long> ret = ResolvableFuture.create();
        ret.set(gltfToken);
        return ret;
    }

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Long> loadGltfAsset(byte @NonNull [] data, @NonNull String key) {
        long gltfToken = mNextModelId++;
        mGltfModels.put(gltfToken, new ArrayList<>());
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        ResolvableFuture<Long> ret = ResolvableFuture.create();
        ret.set(gltfToken);
        return ret;
    }

    @Override
    public void releaseGltfAsset(long gltfToken) {
        if (!mGltfModels.containsKey(gltfToken)) {
            throw new NotFoundException("Model token not found");
        }
        mGltfModels.remove(gltfToken);
    }

    @Override
    public @NonNull ImpressNode instanceGltfModel(long gltfToken) {
        return instanceGltfModel(gltfToken, true);
    }

    @Override
    public @NonNull ImpressNode instanceGltfModel(long gltfToken, boolean enableCollider) {
        if (!mGltfModels.containsKey(gltfToken)) {
            throw new IllegalArgumentException("Model token not found");
        }
        int entityId = mNextNodeId++;
        mGltfModels.get(gltfToken).add(entityId);
        GltfNodeData gltfNodeData = new GltfNodeData();
        gltfNodeData.setEntityId(entityId);
        mImpressNodes.put(gltfNodeData, null);
        return new ImpressNode(entityId);
    }

    @Override
    public void setGltfModelColliderEnabled(
            @NonNull ImpressNode impressNode, boolean enableCollider) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Void> animateGltfModel(
            @NonNull ImpressNode impressNode, @Nullable String animationName, boolean loop) {
        ResolvableFuture<Void> future = ResolvableFuture.create();
        if (getGltfNodeData(impressNode) == null) {
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
    public void stopGltfModelAnimation(@NonNull ImpressNode impressNode) {
        if (getGltfNodeData(impressNode) == null) {
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
    public @NonNull ImpressNode createImpressNode() {
        int entityId = mNextNodeId++;
        GltfNodeData gltfNodeData = new GltfNodeData();
        gltfNodeData.setEntityId(entityId);
        mImpressNodes.put(gltfNodeData, null);
        return new ImpressNode(entityId);
    }

    @Override
    public void destroyImpressNode(@NonNull ImpressNode impressNode) {
        GltfNodeData gltfNodeData = getGltfNodeData(impressNode);
        if (gltfNodeData == null) {
            throw new IllegalArgumentException("Impress node not found");
        }
        for (Map.Entry<Long, List<Integer>> pair : mGltfModels.entrySet()) {
            if (pair.getValue().contains(impressNode.getHandle())) {
                pair.getValue().remove(Integer.valueOf(impressNode.getHandle()));
            }
        }
        for (Map.Entry<GltfNodeData, GltfNodeData> pair : mImpressNodes.entrySet()) {
            if (pair.getValue() != null && pair.getValue().equals(gltfNodeData)) {
                pair.setValue(null);
            }
        }
        mImpressNodes.remove(gltfNodeData);
        if (mStereoSurfaceEntities.containsKey(impressNode)) {
            mStereoSurfaceEntities.remove(impressNode);
        }
    }

    @Override
    public void setImpressNodeParent(
            @NonNull ImpressNode impressNodeChild, @NonNull ImpressNode impressNodeParent) {
        GltfNodeData childGltfNodeData = getGltfNodeData(impressNodeChild);
        GltfNodeData parentGltfNodeData = getGltfNodeData(impressNodeParent);
        if (childGltfNodeData == null || parentGltfNodeData == null) {
            throw new IllegalArgumentException("Impress node(s) not found");
        }
        mImpressNodes.put(childGltfNodeData, parentGltfNodeData);
    }

    /** Gets the impress nodes for glTF models that match the given token. */
    @NonNull
    public List<Integer> getImpressNodesForToken(long gltfToken) {
        return mGltfModels.get(gltfToken);
    }

    /** Returns true if the given impress node has a parent. */
    public boolean impressNodeHasParent(@NonNull ImpressNode impressNode) {
        GltfNodeData gltfNodeData = getGltfNodeData(impressNode);
        if (gltfNodeData == null) {
            return false;
        }
        return mImpressNodes.get(gltfNodeData) != null;
    }

    /** Returns the parent impress node for the given impress node. */
    public int getImpressNodeParent(@NonNull ImpressNode impressNode) {
        GltfNodeData gltfNodeData = getGltfNodeData(impressNode);
        GltfNodeData parentGltfNodeData = mImpressNodes.get(gltfNodeData);
        if (gltfNodeData == null || parentGltfNodeData == null) {
            return -1;
        }
        return parentGltfNodeData.mEntityId;
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
    public @NonNull ImpressNode createStereoSurface(@StereoMode int stereoMode) {
        return createStereoSurface(
                stereoMode, ContentSecurityLevel.NONE, /* useSuperSampling= */ false);
    }

    // TODO - b/410899125: Set the content security level properly.
    @Override
    public @NonNull ImpressNode createStereoSurface(
            @StereoMode int stereoMode, @ContentSecurityLevel int contentSecurityLevel) {
        return createStereoSurface(stereoMode, contentSecurityLevel, /* useSuperSampling= */ false);
    }

    @Override
    public @NonNull ImpressNode createStereoSurface(
            @StereoMode int stereoMode,
            @ContentSecurityLevel int contentSecurityLevel,
            boolean useSuperSampling) {
        StereoSurfaceEntityData data = new StereoSurfaceEntityData();
        data.mImpressNode = createImpressNode();
        data.mSurface = new TestSurface(data.mImpressNode.getHandle());
        data.mUseSuperSampling = useSuperSampling;
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
    public void setStereoSurfaceEntityCanvasShapeQuad(
            @NonNull ImpressNode impressNode, float width, float height) {
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
    public void setStereoSurfaceEntityCanvasShapeSphere(
            @NonNull ImpressNode impressNode, float radius) {
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
    public void setStereoSurfaceEntityCanvasShapeHemisphere(
            @NonNull ImpressNode impressNode, float radius) {
        StereoSurfaceEntityData data = mStereoSurfaceEntities.get(impressNode);
        data.mCanvasShape = StereoSurfaceEntityData.CanvasShape.VR_180_HEMISPHERE;
        data.mRadius = radius;
    }

    @Override
    public void setStereoSurfaceEntityColliderEnabled(
            @NonNull ImpressNode impressNode, boolean enableCollider) {
        StereoSurfaceEntityData data = mStereoSurfaceEntities.get(impressNode);
        if (data != null) {
            data.mColliderEnabled = enableCollider;
        }
    }

    @Override
    @NonNull
    public Surface getSurfaceFromStereoSurface(@NonNull ImpressNode panelImpressNode) {
        if (!mStereoSurfaceEntities.containsKey(panelImpressNode)) {
            // TODO: b/387323937 - the Native code currently CHECK fails in this case
            throw new IllegalArgumentException("Couldn't find stereo surface entity!");
        }
        return mStereoSurfaceEntities.get(panelImpressNode).mSurface;
    }

    @Override
    public void setFeatherRadiusForStereoSurface(
            @NonNull ImpressNode panelImpressNode, float radiusX, float radiusY) {
        if (!mStereoSurfaceEntities.containsKey(panelImpressNode)) {
            // TODO: b/387323937 - the Native code currently CHECK fails in this case
            throw new IllegalArgumentException("Couldn't find stereo surface entity!");
        }
        mStereoSurfaceEntities.get(panelImpressNode).mFeatherRadiusX = radiusX;
        mStereoSurfaceEntities.get(panelImpressNode).mFeatherRadiusY = radiusY;
    }

    @Override
    public void setStereoModeForStereoSurface(
            @NonNull ImpressNode panelImpressNode, @StereoMode int mode) {
        if (!mStereoSurfaceEntities.containsKey(panelImpressNode)) {
            // TODO: b/387323937 - the Native code currently CHECK fails in this case
            throw new IllegalArgumentException("Couldn't find stereo surface entity!");
        }
        mStereoSurfaceEntities.get(panelImpressNode).mStereoMode = mode;
    }

    @Override
    public void setContentColorMetadataForStereoSurface(
            @NonNull ImpressNode stereoSurfaceNode,
            @ColorSpace int colorSpace,
            @ColorTransfer int colorTransfer,
            @ColorRange int colorRange,
            int maxLuminance) {}

    @Override
    public void resetContentColorMetadataForStereoSurface(@NonNull ImpressNode stereoSurfaceNode) {}

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Texture> loadTexture(@NonNull String path) {
        long textureImageToken = mNextTextureId++;
        Texture texture =
                new Texture.Builder()
                        .setImpressApi(this)
                        .setNativeTexture(textureImageToken)
                        .build();
        mTextureImages.put(textureImageToken, texture);
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        ResolvableFuture<Texture> ret = ResolvableFuture.create();
        ret.set(texture);
        return ret;
    }

    @Override
    @NonNull
    public Texture borrowReflectionTexture() {
        long textureImageToken = mNextTextureId++;
        return new Texture.Builder()
                .setImpressApi(this)
                .setNativeTexture(textureImageToken)
                .build();
    }

    @Override
    @NonNull
    public Texture getReflectionTextureFromIbl(long iblToken) {
        long textureImageToken = mNextTextureId++;
        return new Texture.Builder()
                .setImpressApi(this)
                .setNativeTexture(textureImageToken)
                .build();
    }

    @Override
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @NonNull
    public ListenableFuture<WaterMaterial> createWaterMaterial(boolean isAlphaMapVersion) {
        long materialToken = mNextMaterialId++;
        WaterMaterial material =
                new WaterMaterial.Builder()
                        .setImpressApi(this)
                        .setNativeMaterial(materialToken)
                        .build();
        mMaterials.put(materialToken, new MaterialData(MaterialData.Type.WATER, materialToken));
        ResolvableFuture<WaterMaterial> ret = ResolvableFuture.create();
        ret.set(material);
        return ret;
    }

    @Override
    public void setReflectionMapOnWaterMaterial(
            long nativeMaterial, long reflectionMap, @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setNormalMapOnWaterMaterial(
            long nativeMaterial, long normalMap, @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setNormalTilingOnWaterMaterial(long nativeMaterial, float normalTiling) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setNormalSpeedOnWaterMaterial(long nativeMaterial, float normalSpeed) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setAlphaStepMultiplierOnWaterMaterial(
            long nativeMaterial, float alphaStepMultiplier) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setAlphaMapOnWaterMaterial(
            long nativeWaterMaterial, long alphaMap, @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setNormalZOnWaterMaterial(long nativeWaterMaterial, float normalZ) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setNormalBoundaryOnWaterMaterial(long nativeWaterMaterial, float normalBoundary) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setAlphaStepUOnWaterMaterial(
            long nativeWaterMaterial, float x, float y, float z, float w) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setAlphaStepVOnWaterMaterial(
            long nativeWaterMaterial, float x, float y, float z, float w) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    @SuppressWarnings("RestrictTo")
    @NonNull
    public ListenableFuture<KhronosPbrMaterial> createKhronosPbrMaterial(
            @NonNull KhronosPbrMaterialSpec spec) {
        long materialToken = mNextMaterialId++;
        KhronosPbrMaterial material =
                new KhronosPbrMaterial.Builder()
                        .setImpressApi(this)
                        .setNativeMaterial(materialToken)
                        .build();
        mMaterials.put(
                materialToken, new MaterialData(MaterialData.Type.KHRONOS_PBR, materialToken));
        ResolvableFuture<KhronosPbrMaterial> ret = ResolvableFuture.create();
        ret.set(material);
        return ret;
    }

    @Override
    public void setBaseColorTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long baseColorTexture, @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setBaseColorUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setBaseColorFactorsOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float x, float y, float z, float w) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setMetallicRoughnessTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long metallicRoughnessTexture,
            @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setMetallicFactorOnKhronosPbrMaterial(long nativeKhronosPbrMaterial, float factor) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setRoughnessFactorOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float factor) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setNormalTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long normalTexture, @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setNormalUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setNormalFactorOnKhronosPbrMaterial(long nativeKhronosPbrMaterial, float factor) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setAmbientOcclusionTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long ambientOcclusionTexture,
            @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setAmbientOcclusionFactorOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float factor) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setEmissiveTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long emissiveTexture, @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setEmissiveUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setEmissiveFactorsOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float x, float y, float z) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setClearcoatTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long clearcoatTexture, @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setClearcoatNormalTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long clearcoatNormalTexture,
            @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setClearcoatRoughnessTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long clearcoatRoughnessTexture,
            @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setClearcoatFactorsOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float intensity, float roughness, float normal) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setSheenColorTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long sheenColorTexture,
            @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setSheenColorFactorsOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float x, float y, float z) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setSheenRoughnessTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long sheenRoughnessTexture,
            @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setSheenRoughnessFactorOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float factor) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setTransmissionTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long transmissionTexture,
            @NonNull TextureSampler sampler) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setTransmissionUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setTransmissionFactorOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float factor) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setIndexOfRefractionOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float indexOfRefraction) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setAlphaCutoffOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float alphaCutoff) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void destroyNativeObject(long nativeHandle) {
        if (mMaterials.containsKey(nativeHandle)) {
            mMaterials.remove(nativeHandle);
        }
        if (mTextureImages.containsKey(nativeHandle)) {
            mTextureImages.remove(nativeHandle);
        }
    }

    @Override
    public void setMaterialOverride(
            @NonNull ImpressNode impressNode,
            long nativeMaterial,
            @NonNull String nodeName,
            int primitiveIndex) {
        GltfNodeData gltfNodeData = getGltfNodeData(impressNode);
        if (gltfNodeData == null) {
            throw new IllegalArgumentException("Impress node not found");
        }
        gltfNodeData.setMaterialOverride(mMaterials.get(nativeMaterial), nodeName, primitiveIndex);
    }

    @Override
    public void clearMaterialOverride(
            @NonNull ImpressNode impressNode, @NonNull String nodeName, int primitiveIndex) {
        GltfNodeData gltfNodeData = getGltfNodeData(impressNode);
        if (gltfNodeData == null) {
            throw new IllegalArgumentException("Impress node not found");
        }
        gltfNodeData.clearMaterialOverride(nodeName, primitiveIndex);
    }

    @Override
    public void setPreferredEnvironmentLight(long iblToken) {
        mCurrentEnvironmentLightId = iblToken;
    }

    @Override
    public void clearPreferredEnvironmentIblAsset() {
        mCurrentEnvironmentLightId = -1;
    }

    @Override
    public void setPrimaryAlphaMaskForStereoSurface(
            @NonNull ImpressNode impressNode, long alphaMask) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setAuxiliaryAlphaMaskForStereoSurface(
            @NonNull ImpressNode impressNode, long alphaMask) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void disposeAllResources() {
        mImageBasedLightingAssets.clear();
        mImpressNodes.clear();
        mGltfModels.clear();
        mTextureImages.clear();
        mMaterials.clear();
    }

    /** Returns the map of texture image tokens to their associated Texture object. */
    @NonNull
    public Map<Long, Texture> getTextureImages() {
        return mTextureImages;
    }

    /** Returns the map of material tokens to their associated MaterialData object. */
    @NonNull
    public Map<Long, MaterialData> getMaterials() {
        return mMaterials;
    }

    /** Returns the map of impress nodes to their parent impress nodes. */
    @NonNull
    public Map<GltfNodeData, GltfNodeData> getImpressNodes() {
        return mImpressNodes;
    }

    // Returns the list of image based lighting assets that have been loaded.
    @NonNull
    public List<Long> getImageBasedLightingAssets() {
        return mImageBasedLightingAssets;
    }

    // Returns the map of glTF model tokens to their associated impress nodes.
    @NonNull
    public Map<Long, List<Integer>> getGltfModels() {
        return mGltfModels;
    }

    /** Returns the current environment light token. */
    public long getCurrentEnvironmentLight() {
        return mCurrentEnvironmentLightId;
    }

    @Nullable
    private GltfNodeData getGltfNodeData(@NonNull ImpressNode impressNode) {
        for (Map.Entry<GltfNodeData, GltfNodeData> pair : mImpressNodes.entrySet()) {
            if (pair.getKey().mEntityId == impressNode.getHandle()) {
                return pair.getKey();
            }
        }
        return null;
    }
}
