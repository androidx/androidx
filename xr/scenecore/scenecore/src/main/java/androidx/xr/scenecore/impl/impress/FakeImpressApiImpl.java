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
import android.view.Surface;

import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.xr.runtime.internal.KhronosPbrMaterialSpec;
import androidx.xr.runtime.internal.TextureSampler;

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

        int mImpressNode;
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

        public void setMaterialOverride(@Nullable MaterialData materialOverride) {
            this.mMaterialOverride = materialOverride;
        }

        public int getEntityId() {
            return mEntityId;
        }

        @Nullable
        public MaterialData getMaterialOverride() {
            return mMaterialOverride;
        }
    }

    // Vector of image based lighting asset tokens.
    private final List<Long> mImageBasedLightingAssets = new ArrayList<>();
    // Map of model tokens to the list of impress nodes that are instances of that model.
    private final Map<Long, List<Integer>> mGltfModels = new HashMap<>();
    // Map of impress nodes to their parent impress nodes.
    private final Map<GltfNodeData, GltfNodeData> mImpressNodes = new HashMap<>();
    // Map of impress nodes and animations that are currently playing (non looping)
    final Map<Integer, AnimationInProgress> mImpressAnimatedNodes = new HashMap<>();
    // Map of impress nodes and animations that are currently playing (looping)
    final Map<Integer, AnimationInProgress> mImpressLoopAnimatedNodes = new HashMap<>();
    // Map of impress entity nodes to their associated StereoSurfaceEntityData
    final Map<Integer, StereoSurfaceEntityData> mStereoSurfaceEntities = new HashMap<>();
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
            @NonNull byte[] data, @NonNull String key) {
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
    public ListenableFuture<Long> loadGltfAsset(@NonNull byte[] data, @NonNull String key) {
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
    public int instanceGltfModel(long gltfToken) {
        return instanceGltfModel(gltfToken, true);
    }

    @Override
    public int instanceGltfModel(long gltfToken, boolean enableCollider) {
        if (!mGltfModels.containsKey(gltfToken)) {
            throw new IllegalArgumentException("Model token not found");
        }
        int entityId = mNextNodeId++;
        mGltfModels.get(gltfToken).add(entityId);
        GltfNodeData gltfNodeData = new GltfNodeData();
        gltfNodeData.setEntityId(entityId);
        mImpressNodes.put(gltfNodeData, null);
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
    public void stopGltfModelAnimation(int impressNode) {
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
    public int createImpressNode() {
        int entityId = mNextNodeId++;
        GltfNodeData gltfNodeData = new GltfNodeData();
        gltfNodeData.setEntityId(entityId);
        mImpressNodes.put(gltfNodeData, null);
        return entityId;
    }

    @Override
    public void destroyImpressNode(int impressNode) {
        GltfNodeData gltfNodeData = getGltfNodeData(impressNode);
        if (gltfNodeData == null) {
            throw new IllegalArgumentException("Impress node not found");
        }
        for (Map.Entry<Long, List<Integer>> pair : mGltfModels.entrySet()) {
            if (pair.getValue().contains(impressNode)) {
                pair.getValue().remove(Integer.valueOf(impressNode));
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
    public void setImpressNodeParent(int impressNodeChild, int impressNodeParent) {
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
    public boolean impressNodeHasParent(int impressNode) {
        GltfNodeData gltfNodeData = getGltfNodeData(impressNode);
        if (gltfNodeData == null) {
            return false;
        }
        return mImpressNodes.get(gltfNodeData) != null;
    }

    /** Returns the parent impress node for the given impress node. */
    public int getImpressNodeParent(int impressNode) {
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
    public int createStereoSurface(@StereoMode int stereoMode) {
        return createStereoSurface(
                stereoMode, ContentSecurityLevel.NONE, /* useSuperSampling= */ false);
    }

    // TODO - b/410899125: Set the content security level properly.
    @Override
    public int createStereoSurface(
            @StereoMode int stereoMode, @ContentSecurityLevel int contentSecurityLevel) {
        return createStereoSurface(stereoMode, contentSecurityLevel, /* useSuperSampling= */ false);
    }

    @Override
    public int createStereoSurface(
            @StereoMode int stereoMode,
            @ContentSecurityLevel int contentSecurityLevel,
            boolean useSuperSampling) {
        StereoSurfaceEntityData data = new StereoSurfaceEntityData();
        data.mImpressNode = createImpressNode();
        data.mSurface = new TestSurface(data.mImpressNode);
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
    public void setFeatherRadiusForStereoSurface(
            int panelImpressNode, float radiusX, float radiusY) {
        if (!mStereoSurfaceEntities.containsKey(panelImpressNode)) {
            // TODO: b/387323937 - the Native code currently CHECK fails in this case
            throw new IllegalArgumentException("Couldn't find stereo surface entity!");
        }
        mStereoSurfaceEntities.get(panelImpressNode).mFeatherRadiusX = radiusX;
        mStereoSurfaceEntities.get(panelImpressNode).mFeatherRadiusY = radiusY;
    }

    @Override
    public void setStereoModeForStereoSurface(int panelImpressNode, @StereoMode int mode) {
        if (!mStereoSurfaceEntities.containsKey(panelImpressNode)) {
            // TODO: b/387323937 - the Native code currently CHECK fails in this case
            throw new IllegalArgumentException("Couldn't find stereo surface entity!");
        }
        mStereoSurfaceEntities.get(panelImpressNode).mStereoMode = mode;
    }

    @Override
    public void setContentColorMetadataForStereoSurface(
            int stereoSurfaceNode,
            @ColorSpace int colorSpace,
            @ColorTransfer int colorTransfer,
            @ColorRange int colorRange,
            int maxLuminance) {}

    @Override
    public void resetContentColorMetadataForStereoSurface(int stereoSurfaceNode) {}

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Texture> loadTexture(
            @NonNull String path, @NonNull TextureSampler sampler) {
        long textureImageToken = mNextTextureId++;
        Texture texture =
                new Texture.Builder()
                        .setImpressApi(this)
                        .setNativeTexture(textureImageToken)
                        .setTextureSampler(sampler)
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
                .setTextureSampler(null)
                .build();
    }

    @Override
    @NonNull
    public Texture getReflectionTextureFromIbl(long iblToken) {
        long textureImageToken = mNextTextureId++;
        return new Texture.Builder()
                .setImpressApi(this)
                .setNativeTexture(textureImageToken)
                .setTextureSampler(null)
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
    public void setReflectionMapOnWaterMaterial(long nativeMaterial, long reflectionMap) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setNormalMapOnWaterMaterial(long nativeMaterial, long normalMap) {
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
    public void setAlphaMapOnWaterMaterial(long nativeWaterMaterial, long alphaMap) {
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
            long nativeKhronosPbrMaterial, long baseColorTexture) {
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
            long nativeKhronosPbrMaterial, long metallicRoughnessTexture) {
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
            long nativeKhronosPbrMaterial, long normalTexture) {
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
            long nativeKhronosPbrMaterial, long ambientOcclusionTexture) {
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
            long nativeKhronosPbrMaterial, long emissiveTexture) {
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
            long nativeKhronosPbrMaterial, long clearcoatTexture) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setClearcoatNormalTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long clearcoatNormalTexture) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setClearcoatRoughnessTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long clearcoatRoughnessTexture) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setClearcoatFactorsOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float intensity, float roughness, float normal) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setSheenColorTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long sheenColorTexture) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setSheenColorFactorsOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float x, float y, float z) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setSheenRoughnessTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long sheenRoughnessTexture) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setSheenRoughnessFactorOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float factor) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setTransmissionTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long transmissionTexture) {
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
            int impressNode, long nativeMaterial, @NonNull String meshName) {
        GltfNodeData gltfNodeData = getGltfNodeData(impressNode);
        if (gltfNodeData == null) {
            throw new IllegalArgumentException("Impress node not found");
        }
        gltfNodeData.setMaterialOverride(mMaterials.get(nativeMaterial));
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
    public void setPrimaryAlphaMaskForStereoSurface(int impressNode, long alphaMask) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setAuxiliaryAlphaMaskForStereoSurface(int impressNode, long alphaMask) {
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
    private GltfNodeData getGltfNodeData(int impressNode) {
        for (Map.Entry<GltfNodeData, GltfNodeData> pair : mImpressNodes.entrySet()) {
            if (pair.getKey().mEntityId == impressNode) {
                return pair.getKey();
            }
        }
        return null;
    }
}
