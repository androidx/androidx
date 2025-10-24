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

import androidx.annotation.VisibleForTesting;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.xr.runtime.math.Matrix3;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.math.Vector4;
import androidx.xr.scenecore.impl.impress.ExrImage;
import androidx.xr.scenecore.impl.impress.GltfModel;
import androidx.xr.scenecore.impl.impress.ImpressApi;
import androidx.xr.scenecore.impl.impress.ImpressApiImpl;
import androidx.xr.scenecore.impl.impress.KhronosPbrMaterial;
import androidx.xr.scenecore.impl.impress.Material;
import androidx.xr.scenecore.impl.impress.Texture;
import androidx.xr.scenecore.impl.impress.WaterMaterial;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.ExrImageResource;
import androidx.xr.scenecore.runtime.GltfEntity;
import androidx.xr.scenecore.runtime.GltfFeature;
import androidx.xr.scenecore.runtime.GltfModelResource;
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec;
import androidx.xr.scenecore.runtime.MaterialResource;
import androidx.xr.scenecore.runtime.RenderingEntityFactory;
import androidx.xr.scenecore.runtime.RenderingRuntime;
import androidx.xr.scenecore.runtime.SceneRuntime;
import androidx.xr.scenecore.runtime.SpatialEnvironmentExt;
import androidx.xr.scenecore.runtime.SurfaceEntity;
import androidx.xr.scenecore.runtime.TextureResource;
import androidx.xr.scenecore.runtime.TextureSampler;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;

import com.android.extensions.xr.XrExtensions;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngine;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

/**
 * Implementation of [RenderingRuntime] for devices that support the [Feature.SPATIAL] system
 * feature.
 */
class SpatialRenderingRuntime implements RenderingRuntime {
    private static final String SPLIT_ENGINE_LIBRARY_NAME = "impress_api_jni";

    @SuppressWarnings("UnusedVariable")
    private final @NonNull RenderingEntityFactory mRenderingEntityFactory;

    private @Nullable Activity mActivity;
    private @Nullable SpatialEnvironmentFeatureImpl mSpatialEnvironmentFeature;

    @SuppressWarnings("UnusedVariable")
    private final XrExtensions mExtensions;

    private final ImpressApi mImpressApi;
    private SplitEngineSubspaceManager mSplitEngineSubspaceManager;
    private ImpSplitEngineRenderer mSplitEngineRenderer;
    private boolean mIsDestroyed = false;
    private boolean mFrameLoopStarted;

    private SpatialRenderingRuntime(
            @NonNull SceneRuntime sceneRuntime,
            @NonNull Activity activity,
            @NonNull XrExtensions extensions,
            @NonNull ImpressApi impressApi,
            @NonNull SplitEngineSubspaceManager subspaceManager,
            @NonNull ImpSplitEngineRenderer renderer) {
        if (!(sceneRuntime instanceof RenderingEntityFactory)) {
            throw new IllegalArgumentException(
                    "Expected sceneRuntime to be a RenderingEntityFactory");
        }
        mRenderingEntityFactory = (RenderingEntityFactory) sceneRuntime;
        mActivity = activity;
        mExtensions = extensions;
        mImpressApi = impressApi;
        mSplitEngineRenderer = renderer;
        mSplitEngineSubspaceManager = subspaceManager;
        mSpatialEnvironmentFeature =
                new SpatialEnvironmentFeatureImpl(
                        mActivity, mImpressApi, mSplitEngineSubspaceManager, mExtensions);

        ((SpatialEnvironmentExt) sceneRuntime.getSpatialEnvironment())
                .onRenderingFeatureReady(mSpatialEnvironmentFeature);
    }

    /** Create a new @c RenderingRuntime. */
    @VisibleForTesting
    static @NonNull SpatialRenderingRuntime create(
            @NonNull SceneRuntime sceneRuntime,
            @NonNull Activity activity,
            @Nullable ImpressApi impressApi,
            @Nullable SplitEngineSubspaceManager splitEngineSubspaceManager,
            @Nullable ImpSplitEngineRenderer splitEngineRenderer) {
        XrExtensions extensions = XrExtensionsProvider.getXrExtensions();
        if (extensions == null) throw new IllegalStateException("XrExtensions is null");
        if (impressApi == null) impressApi = new ImpressApiImpl();
        if (splitEngineRenderer == null) {
            ImpSplitEngine.SplitEngineSetupParams impApiSetupParams =
                    new ImpSplitEngine.SplitEngineSetupParams();
            impApiSetupParams.jniLibraryName = SPLIT_ENGINE_LIBRARY_NAME;
            splitEngineRenderer =
                    ImpSplitEngineRenderer.create(activity, impApiSetupParams, extensions);
        }
        if (splitEngineSubspaceManager == null) {
            splitEngineSubspaceManager =
                    new SplitEngineSubspaceManager(
                            splitEngineRenderer, extensions, null, null, SPLIT_ENGINE_LIBRARY_NAME);
        }
        impressApi.setup(splitEngineRenderer.getView());
        return new SpatialRenderingRuntime(
                sceneRuntime,
                activity,
                extensions,
                impressApi,
                splitEngineSubspaceManager,
                splitEngineRenderer);
    }

    /**
     * Create a new @c SpatialRenderingRuntime.
     *
     * @param sceneRuntime The SceneRuntime provide basic function for creating entities.
     * @param activity The Activity to use.
     * @return A new SpatialRenderingRuntime.
     */
    static @NonNull SpatialRenderingRuntime create(
            @NonNull SceneRuntime sceneRuntime, @NonNull Activity activity) {
        return SpatialRenderingRuntime.create(sceneRuntime, activity, null, null, null);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private @Nullable ListenableFuture<GltfModelResource> loadGltfAsset(
            Supplier<ListenableFuture<GltfModel>> modelLoader) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ResolvableFuture<GltfModelResource> gltfModelResourceFuture = ResolvableFuture.create();

        ListenableFuture<GltfModel> gltfTokenFuture;
        try {
            gltfTokenFuture = modelLoader.get();
        } catch (RuntimeException e) {
            return null;
        }

        gltfTokenFuture.addListener(
                () -> {
                    try {
                        GltfModel gltfToken = gltfTokenFuture.get();
                        gltfModelResourceFuture.set(gltfToken);
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        if (e instanceof CancellationException) {
                            gltfModelResourceFuture.cancel(false);
                        } else {
                            gltfModelResourceFuture.setException(e);
                        }
                    }
                },
                mActivity::runOnUiThread);

        return gltfModelResourceFuture;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private @Nullable ListenableFuture<ExrImageResource> loadExrImage(
            Supplier<ListenableFuture<ExrImage>> assetLoader) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ResolvableFuture<ExrImageResource> exrImageResourceFuture = ResolvableFuture.create();

        ListenableFuture<ExrImage> exrImageTokenFuture;
        try {
            exrImageTokenFuture = assetLoader.get();
        } catch (RuntimeException e) {
            return null;
        }

        exrImageTokenFuture.addListener(
                () -> {
                    try {
                        ExrImage exrImageToken = exrImageTokenFuture.get();
                        exrImageResourceFuture.set(exrImageToken);
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        if (e instanceof CancellationException) {
                            exrImageResourceFuture.cancel(false);
                        } else {
                            exrImageResourceFuture.setException(e);
                        }
                    }
                },
                mActivity::runOnUiThread);

        return exrImageResourceFuture;
    }

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @NonNull ListenableFuture<GltfModelResource> loadGltfByAssetName(@NonNull String name) {
        return loadGltfAsset(() -> mImpressApi.loadGltfAsset(name));
    }

    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @NonNull ListenableFuture<GltfModelResource> loadGltfByByteArray(
            byte @NonNull [] assetData, @NonNull String assetKey) {
        return loadGltfAsset(() -> mImpressApi.loadGltfAsset(assetData, assetKey));
    }

    @Override
    public void destroyGltfModel(@NonNull GltfModelResource gltfModel) {
        GltfModel gltfModelResource = (GltfModel) gltfModel;
        mImpressApi.releaseGltfAsset(gltfModelResource.getNativeHandle());
    }

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @NonNull ListenableFuture<ExrImageResource> loadExrImageByAssetName(
            @NonNull String assetName) {
        return loadExrImage(() -> mImpressApi.loadImageBasedLightingAsset(assetName));
    }

    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @NonNull ListenableFuture<ExrImageResource> loadExrImageByByteArray(
            byte @NonNull [] assetData, @NonNull String assetKey) {
        return loadExrImage(() -> mImpressApi.loadImageBasedLightingAsset(assetData, assetKey));
    }

    @Override
    public void destroyExrImage(@NonNull ExrImageResource exrImage) {
        ExrImage exrImageResource = (ExrImage) exrImage;
        mImpressApi.releaseImageBasedLightingAsset(exrImageResource.getNativeHandle());
    }

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @NonNull ListenableFuture<TextureResource> loadTexture(@NonNull String path) {
        ResolvableFuture<TextureResource> textureResourceFuture = ResolvableFuture.create();
        // TODO:b/374216912 - Consider calling setFuture() here to catch if the application calls
        // cancel() on the return value from this function, so we can propagate the cancelation
        // message to the Impress API.

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ListenableFuture<Texture> textureFuture;
        try {
            textureFuture = mImpressApi.loadTexture(path);
        } catch (RuntimeException e) {
            textureResourceFuture.setException(e);
            return textureResourceFuture;
        }

        textureFuture.addListener(
                () -> {
                    try {
                        Texture texture = textureFuture.get();
                        textureResourceFuture.set(texture);
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        if (e instanceof CancellationException) {
                            textureResourceFuture.cancel(false);
                        } else {
                            textureResourceFuture.setException(e);
                        }
                    }
                },
                // It's convenient for the main application for us to dispatch their listeners on
                // the main thread, because they are required to call back to Impress from there,
                // and it's likely that they will want to call back into the SDK to create entities
                // from within a listener. We defensively post to the main thread here, but in
                // practice this should not cause a thread hop because the Impress API already
                // dispatches its callbacks to the main thread.
                mActivity::runOnUiThread);
        return textureResourceFuture;
    }

    @Override
    public @Nullable TextureResource borrowReflectionTexture() {
        Texture texture = mImpressApi.borrowReflectionTexture();
        if (texture == null) {
            return null;
        }
        return texture;
    }

    @Override
    public void destroyTexture(@NonNull TextureResource texture) {
        Texture textureResource = (Texture) texture;
        mImpressApi.destroyNativeObject(textureResource.getNativeHandle());
    }

    @Override
    public @Nullable TextureResource getReflectionTextureFromIbl(
            @NonNull ExrImageResource iblToken) {
        Texture texture =
                mImpressApi.getReflectionTextureFromIbl(((ExrImage) iblToken).getNativeHandle());
        if (texture == null) {
            return null;
        }
        return texture;
    }

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @NonNull ListenableFuture<MaterialResource> createWaterMaterial(
            boolean isAlphaMapVersion) {
        ResolvableFuture<MaterialResource> materialResourceFuture = ResolvableFuture.create();
        // TODO:b/374216912 - Consider calling setFuture() here to catch if the application calls
        // cancel() on the return value from this function, so we can propagate the cancelation
        // message to the Impress API.

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ListenableFuture<WaterMaterial> materialFuture;
        try {
            materialFuture = mImpressApi.createWaterMaterial(isAlphaMapVersion);
        } catch (RuntimeException e) {
            materialResourceFuture.setException(e);
            return materialResourceFuture;
        }

        materialFuture.addListener(
                () -> {
                    try {
                        WaterMaterial material = materialFuture.get();
                        materialResourceFuture.set(material);
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        if (e instanceof CancellationException) {
                            materialResourceFuture.cancel(false);
                        } else {
                            materialResourceFuture.setException(e);
                        }
                    }
                },
                // It's convenient for the main application for us to dispatch their listeners on
                // the main thread, because they are required to call back to Impress from there,
                // and it's likely that they will want to call back into the SDK to create entities
                // from within a listener. We defensively post to the main thread here, but in
                // practice this should not cause a thread hop because the Impress API already
                // dispatches its callbacks to the main thread.
                mActivity::runOnUiThread);
        return materialResourceFuture;
    }

    @Override
    public void destroyWaterMaterial(@NonNull MaterialResource material) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        ((Material) material).destroy();
    }

    @Override
    public void setReflectionMapOnWaterMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource reflectionMap,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(reflectionMap instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setReflectionMapOnWaterMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) reflectionMap).getNativeHandle(),
                sampler);
    }

    @Override
    public void setNormalMapOnWaterMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource normalMap,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(normalMap instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setNormalMapOnWaterMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) normalMap).getNativeHandle(),
                sampler);
    }

    @Override
    public void setNormalTilingOnWaterMaterial(
            @NonNull MaterialResource material, float normalTiling) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setNormalTilingOnWaterMaterial(
                ((Material) material).getNativeHandle(), normalTiling);
    }

    @Override
    public void setNormalSpeedOnWaterMaterial(
            @NonNull MaterialResource material, float normalSpeed) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setNormalSpeedOnWaterMaterial(
                ((Material) material).getNativeHandle(), normalSpeed);
    }

    @Override
    public void setAlphaStepMultiplierOnWaterMaterial(
            @NonNull MaterialResource material, float alphaStepMultiplier) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setAlphaStepMultiplierOnWaterMaterial(
                ((Material) material).getNativeHandle(), alphaStepMultiplier);
    }

    @Override
    public void setAlphaMapOnWaterMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource alphaMap,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(alphaMap instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setAlphaMapOnWaterMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) alphaMap).getNativeHandle(),
                sampler);
    }

    @Override
    public void setNormalZOnWaterMaterial(@NonNull MaterialResource material, float normalZ) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setNormalZOnWaterMaterial(((Material) material).getNativeHandle(), normalZ);
    }

    @Override
    public void setNormalBoundaryOnWaterMaterial(
            @NonNull MaterialResource material, float normalBoundary) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setNormalBoundaryOnWaterMaterial(
                ((Material) material).getNativeHandle(), normalBoundary);
    }

    @SuppressWarnings("AsyncSuffixFuture")
    @Override
    public @NonNull ListenableFuture<MaterialResource> createKhronosPbrMaterial(
            @NonNull KhronosPbrMaterialSpec spec) {
        ResolvableFuture<MaterialResource> materialResourceFuture = ResolvableFuture.create();
        // TODO:b/374216912 - Consider calling setFuture() here to catch if the application calls
        // cancel() on the return value from this function, so we can propagate the cancelation
        // message to the Impress API.

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ListenableFuture<KhronosPbrMaterial> materialFuture;
        try {
            materialFuture = mImpressApi.createKhronosPbrMaterial(spec);
        } catch (RuntimeException e) {
            materialResourceFuture.setException(e);
            return materialResourceFuture;
        }

        materialFuture.addListener(
                () -> {
                    try {
                        KhronosPbrMaterial material = materialFuture.get();
                        materialResourceFuture.set(material);
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        if (e instanceof CancellationException) {
                            materialResourceFuture.cancel(false);
                        } else {
                            materialResourceFuture.setException(e);
                        }
                    }
                },
                // It's convenient for the main application for us to dispatch their listeners on
                // the main thread, because they are required to call back to Impress from there,
                // and it's likely that they will want to call back into the SDK to create entities
                // from within a listener. We defensively post to the main thread here, but in
                // practice this should not cause a thread hop because the Impress API already
                // dispatches its callbacks to the main thread.
                mActivity::runOnUiThread);
        return materialResourceFuture;
    }

    @Override
    public void destroyKhronosPbrMaterial(@NonNull MaterialResource material) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        ((Material) material).destroy();
    }

    @Override
    public void setBaseColorTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource baseColor,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(baseColor instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setBaseColorTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) baseColor).getNativeHandle(),
                sampler);
    }

    @Override
    public void setBaseColorUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setBaseColorUvTransformOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
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
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setBaseColorFactorsOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                factors.getX(),
                factors.getY(),
                factors.getZ(),
                factors.getW());
    }

    @Override
    public void setMetallicRoughnessTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource metallicRoughness,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(metallicRoughness instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setMetallicRoughnessTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) metallicRoughness).getNativeHandle(),
                sampler);
    }

    @Override
    public void setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
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
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setMetallicFactorOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(), factor);
    }

    @Override
    public void setRoughnessFactorOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float factor) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setRoughnessFactorOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(), factor);
    }

    @Override
    public void setNormalTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource normal,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(normal instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setNormalTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) normal).getNativeHandle(),
                sampler);
    }

    @Override
    public void setNormalUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setNormalUvTransformOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
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
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setNormalFactorOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(), factor);
    }

    @Override
    public void setAmbientOcclusionTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource ambientOcclusion,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(ambientOcclusion instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setAmbientOcclusionTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) ambientOcclusion).getNativeHandle(),
                sampler);
    }

    @Override
    public void setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
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
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setAmbientOcclusionFactorOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(), factor);
    }

    @Override
    public void setEmissiveTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource emissive,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(emissive instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setEmissiveTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) emissive).getNativeHandle(),
                sampler);
    }

    @Override
    public void setEmissiveUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setEmissiveUvTransformOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
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
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setEmissiveFactorsOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                factors.getX(),
                factors.getY(),
                factors.getZ());
    }

    @Override
    public void setClearcoatTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource clearcoat,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(clearcoat instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setClearcoatTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) clearcoat).getNativeHandle(),
                sampler);
    }

    @Override
    public void setClearcoatNormalTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource clearcoatNormal,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(clearcoatNormal instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setClearcoatNormalTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) clearcoatNormal).getNativeHandle(),
                sampler);
    }

    @Override
    public void setClearcoatRoughnessTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource clearcoatRoughness,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(clearcoatRoughness instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setClearcoatRoughnessTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) clearcoatRoughness).getNativeHandle(),
                sampler);
    }

    @Override
    public void setClearcoatFactorsOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float intensity, float roughness, float normal) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setClearcoatFactorsOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(), intensity, roughness, normal);
    }

    @Override
    public void setSheenColorTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource sheenColor,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(sheenColor instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setSheenColorTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) sheenColor).getNativeHandle(),
                sampler);
    }

    @Override
    public void setSheenColorFactorsOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Vector3 factors) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setSheenColorFactorsOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                factors.getX(),
                factors.getY(),
                factors.getZ());
    }

    @Override
    public void setSheenRoughnessTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource sheenRoughness,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(sheenRoughness instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setSheenRoughnessTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) sheenRoughness).getNativeHandle(),
                sampler);
    }

    @Override
    public void setSheenRoughnessFactorOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float factor) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setSheenRoughnessFactorOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(), factor);
    }

    @Override
    public void setTransmissionTextureOnKhronosPbrMaterial(
            @NonNull MaterialResource material,
            @NonNull TextureResource transmission,
            @NonNull TextureSampler sampler) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        if (!(transmission instanceof Texture)) {
            throw new IllegalArgumentException("TextureResource is not a Texture");
        }
        mImpressApi.setTransmissionTextureOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
                ((Texture) transmission).getNativeHandle(),
                sampler);
    }

    @Override
    public void setTransmissionUvTransformOnKhronosPbrMaterial(
            @NonNull MaterialResource material, @NonNull Matrix3 uvTransform) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        float[] data = uvTransform.getData();
        mImpressApi.setTransmissionUvTransformOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(),
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
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setTransmissionFactorOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(), factor);
    }

    @Override
    public void setIndexOfRefractionOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float indexOfRefraction) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setIndexOfRefractionOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(), indexOfRefraction);
    }

    @Override
    public void setAlphaCutoffOnKhronosPbrMaterial(
            @NonNull MaterialResource material, float alphaCutoff) {
        if (!(material instanceof Material)) {
            throw new IllegalArgumentException("MaterialResource is not a Material");
        }
        mImpressApi.setAlphaCutoffOnKhronosPbrMaterial(
                ((Material) material).getNativeHandle(), alphaCutoff);
    }

    @Override
    @NonNull
    public GltfEntity createGltfEntity(
            @NonNull Pose pose,
            @NonNull GltfModelResource loadedGltf,
            @NonNull Entity parentEntity) {
        GltfFeature feature =
                new GltfFeatureImpl(
                        (GltfModel) loadedGltf,
                        mImpressApi,
                        mSplitEngineSubspaceManager,
                        mExtensions);
        return mRenderingEntityFactory.createGltfEntity(feature, pose, parentEntity);
    }

    @Override
    @NonNull
    public SurfaceEntity createSurfaceEntity(
            @SurfaceEntity.StereoMode int stereoMode,
            @NonNull Pose pose,
            SurfaceEntity.@NonNull Shape canvasShape,
            @SurfaceEntity.SurfaceProtection int contentSecurityLevel,
            @SurfaceEntity.SuperSampling int superSampling,
            @NonNull Entity parentEntity) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        SurfaceFeatureImpl feature =
                new SurfaceFeatureImpl(
                        mImpressApi,
                        mSplitEngineSubspaceManager,
                        mExtensions,
                        stereoMode,
                        canvasShape,
                        contentSecurityLevel,
                        superSampling);
        return mRenderingEntityFactory.createSurfaceEntity(feature, pose, parentEntity);
    }

    // JxrRuntime lifecycle
    @Override
    public void resume() {
        // Start renderer
        if (mSplitEngineRenderer == null || mFrameLoopStarted) {
            return;
        }
        mFrameLoopStarted = true;
        mSplitEngineRenderer.startFrameLoop();
    }

    @Override
    public void pause() {
        // Stop renderer
        if (mSplitEngineRenderer == null || !mFrameLoopStarted) {
            return;
        }
        mFrameLoopStarted = false;
        mSplitEngineRenderer.stopFrameLoop();
    }

    @Override
    public void destroy() {
        if (mIsDestroyed) {
            return;
        }

        mActivity = null;
        if (mSplitEngineRenderer != null && mSplitEngineSubspaceManager != null) {
            if (mFrameLoopStarted) {
                mFrameLoopStarted = false;
                mSplitEngineRenderer.stopFrameLoop();
            }

            // mSpatialEnvironmentFeature.dispose() will be invoked once in SceneRuntime.dispose()
            // to make the XrExtensions operations happen before the SceneRuntime detaching the
            // scene. Do the destroy here again to clean our own resource formally.
            if (mSpatialEnvironmentFeature != null) {
                mSpatialEnvironmentFeature.dispose();
                mSpatialEnvironmentFeature = null;
            }
            mImpressApi.disposeAllResources();
            mSplitEngineSubspaceManager.destroy();
            mSplitEngineRenderer.destroy();
            mSplitEngineSubspaceManager = null;
            mSplitEngineRenderer = null;
        }
        mIsDestroyed = true;
    }

    @VisibleForTesting
    boolean isFrameLoopStarted() {
        return mFrameLoopStarted;
    }
}
