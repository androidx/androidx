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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.app.Activity;

import androidx.xr.runtime.math.FloatSize2d;
import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.impl.impress.ExrImage;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl;
import androidx.xr.scenecore.impl.impress.GltfModel;
import androidx.xr.scenecore.impl.impress.Material;
import androidx.xr.scenecore.impl.impress.Texture;
import androidx.xr.scenecore.runtime.ExrImageResource;
import androidx.xr.scenecore.runtime.GltfEntity;
import androidx.xr.scenecore.runtime.GltfFeature;
import androidx.xr.scenecore.runtime.GltfModelResource;
import androidx.xr.scenecore.runtime.MaterialResource;
import androidx.xr.scenecore.runtime.RenderingEntityFactory;
import androidx.xr.scenecore.runtime.RenderingRuntime;
import androidx.xr.scenecore.runtime.SceneRuntime;
import androidx.xr.scenecore.runtime.SurfaceEntity;
import androidx.xr.scenecore.runtime.TextureResource;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeSceneRuntime;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Objects;

/** Tests for {@link SpatialRenderingRuntime}. */
@RunWith(RobolectricTestRunner.class)
// TODO: b/441552980 - add unit tests for gltf animations
public class SpatialRenderingRuntimeTest {
    private static final int OPEN_XR_REFERENCE_SPACE_TYPE = 1;
    private SceneRuntime mSceneRuntime;
    private RenderingRuntime mRenderingRuntime;
    private SpatialRenderingRuntime mSpatialRenderingRuntime;

    /** The factory in SceneRuntime */
    private RenderingEntityFactory mRenderingEntityFactory;

    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    Activity mActivity;
    private final FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    ImpSplitEngineRenderer mSplitEngineRenderer = Mockito.mock(ImpSplitEngineRenderer.class);
    private final @NonNull XrExtensions mXrExtensions =
            Objects.requireNonNull(XrExtensionsProvider.getXrExtensions());

    private static final int SUBSPACE_ID = 5;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(Activity.class).create().start().get();
        ShadowXrExtensions.extract(mXrExtensions)
                .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE);
        FakeSceneRuntime fakeSceneRuntime = new FakeSceneRuntime(mFakeExecutor);
        mSceneRuntime = fakeSceneRuntime;

        assertThat(fakeSceneRuntime).isNotNull();

        mSpatialRenderingRuntime =
                SpatialRenderingRuntime.create(
                        mSceneRuntime,
                        mActivity,
                        mFakeImpressApi,
                        mSplitEngineSubspaceManager,
                        mSplitEngineRenderer);
        mRenderingRuntime = mSpatialRenderingRuntime;
        mRenderingEntityFactory = (RenderingEntityFactory) mSceneRuntime;
    }

    @After
    public void tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        try {
            mRenderingRuntime.destroy();
            mSceneRuntime.destroy();
        } catch (NullPointerException e) {
            // Tests which already call dispose will cause a NPE here due to Activity being null
            // when detaching from the scene.
        }
        mRenderingRuntime = null;
        mSceneRuntime = null;
    }

    private GltfEntity createGltfEntity() throws Exception {
        return createGltfEntity(new Pose());
    }

    private GltfEntity createGltfEntity(Pose pose) throws Exception {
        ListenableFuture<GltfModelResource> modelFuture =
                mRenderingRuntime.loadGltfByAssetName("FakeAsset.glb");
        assertThat(modelFuture).isNotNull();
        // This resolves the transformation of the Future from a SplitEngine token to the JXR
        // GltfModelResource.  This is a hidden detail from the API surface's perspective.
        mFakeExecutor.runAll();
        GltfModelResource model = modelFuture.get();

        GltfFeature feature =
                new GltfFeatureImpl(
                        (GltfModel) model,
                        mFakeImpressApi,
                        mSplitEngineSubspaceManager,
                        mXrExtensions);
        return mRenderingEntityFactory.createGltfEntity(
                feature, pose, mSceneRuntime.getActivitySpace());
    }

    TextureResource loadTexture() throws Exception {
        ListenableFuture<TextureResource> textureFuture =
                mRenderingRuntime.loadTexture("FakeTexture.png");
        assertThat(textureFuture).isNotNull();
        // This resolves the transformation of the Future from a SplitEngine token to the JXR
        // Texture.  This is a hidden detail from the API surface's perspective.
        mFakeExecutor.runAll();
        return textureFuture.get();
    }

    MaterialResource createWaterMaterial() throws Exception {
        ListenableFuture<MaterialResource> materialFuture =
                mRenderingRuntime.createWaterMaterial(/* isAlphaMapVersion= */ false);
        assertThat(materialFuture).isNotNull();
        // This resolves the transformation of the Future from a SplitEngine token to the JXR
        // Texture.  This is a hidden detail from the API surface's perspective.
        mFakeExecutor.runAll();
        return materialFuture.get();
    }

    @Test
    public void loadExrImageByAssetName_returnsModel() throws Exception {
        ListenableFuture<ExrImageResource> imageFuture =
                mRenderingRuntime.loadExrImageByAssetName("FakeAsset.zip");

        assertThat(imageFuture).isNotNull();

        ExrImageResource image = imageFuture.get();
        assertThat(image).isNotNull();
        ExrImage exrImage = (ExrImage) image;
        assertThat(exrImage).isNotNull();
        long token = exrImage.getNativeHandle();
        assertThat(token).isEqualTo(1);
    }

    @Test
    public void loadExrImageByByteArray_returnsModel() throws Exception {
        ListenableFuture<ExrImageResource> imageFuture =
                mRenderingRuntime.loadExrImageByByteArray(new byte[] {1, 2, 3}, "FakeAsset.zip");

        assertThat(imageFuture).isNotNull();

        ExrImageResource image = imageFuture.get();
        assertThat(image).isNotNull();
        ExrImage exrImage = (ExrImage) image;
        assertThat(exrImage).isNotNull();
        long token = exrImage.getNativeHandle();
        assertThat(token).isEqualTo(1);
    }

    @Test
    public void loadGltfByAssetName_returnsModel() throws Exception {
        ListenableFuture<GltfModelResource> modelFuture =
                mRenderingRuntime.loadGltfByAssetName("FakeAsset.glb");

        assertThat(modelFuture).isNotNull();

        GltfModelResource modelRes = modelFuture.get();
        assertThat(modelRes).isNotNull();
        GltfModel model = (GltfModel) modelRes;
        assertThat(model).isNotNull();
        long token = model.getNativeHandle();
        assertThat(token).isEqualTo(1);
    }

    @Test
    public void loadGltfByByteArray_returnsModel() throws Exception {
        ListenableFuture<GltfModelResource> modelFuture =
                mRenderingRuntime.loadGltfByByteArray(new byte[] {1, 2, 3}, "FakeAsset.glb");

        assertThat(modelFuture).isNotNull();

        GltfModelResource modelRes = modelFuture.get();
        assertThat(modelRes).isNotNull();
        GltfModel model = (GltfModel) modelRes;
        assertThat(model).isNotNull();
        long token = model.getNativeHandle();
        assertThat(token).isEqualTo(1);
    }

    @Test
    public void createGltfEntity_returnsEntity() throws Exception {
        assertThat(createGltfEntity()).isNotNull();
    }

    @Test
    public void animateGltfEntity_gltfEntityIsAnimating() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        gltfEntity.startAnimation(false, "animation_name");
        int animatingNodes = mFakeImpressApi.impressNodeAnimatingSize();
        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();

        // The fakeJniApi returns a future which immediately fires, which makes it seem like the
        // animation is done immediately. This makes it look like the animation stopped right away.
        assertThat(gltfEntity.getAnimationState()).isEqualTo(GltfEntity.AnimationState.PLAYING);
        assertThat(animatingNodes).isEqualTo(1);
        assertThat(loopingAnimatingNodes).isEqualTo(0);
    }

    @Test
    public void animateLoopGltfEntity_gltfEntityIsAnimatingInLoop() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        gltfEntity.startAnimation(true, "animation_name");
        int animatingNodes = mFakeImpressApi.impressNodeAnimatingSize();
        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();

        assertThat(gltfEntity.getAnimationState()).isEqualTo(GltfEntity.AnimationState.PLAYING);
        assertThat(animatingNodes).isEqualTo(0);
        assertThat(loopingAnimatingNodes).isEqualTo(1);
    }

    @Test
    public void stopAnimateGltfEntity_gltfEntityStopsAnimating() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        gltfEntity.startAnimation(true, "animation_name");
        gltfEntity.stopAnimation();
        int animatingNodes = mFakeImpressApi.impressNodeAnimatingSize();
        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();

        assertThat(gltfEntity.getAnimationState()).isEqualTo(GltfEntity.AnimationState.STOPPED);
        assertThat(animatingNodes).isEqualTo(0);
        assertThat(loopingAnimatingNodes).isEqualTo(0);
    }

    @Test
    public void createSurfaceEntity_returnsStereoSurface() {
        final float kTestWidth = 14.0f;
        final float kTestHeight = 28.0f;
        final float kTestSphereRadius = 7.0f;
        final float kTestHemisphereRadius = 11.0f;

        SurfaceEntity surfaceEntityQuad =
                mRenderingRuntime.createSurfaceEntity(
                        SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                        new Pose(),
                        new SurfaceEntity.Shape.Quad(new FloatSize2d(kTestWidth, kTestHeight)),
                        SurfaceEntity.SurfaceProtection.NONE,
                        SurfaceEntity.SuperSampling.DEFAULT,
                        mSceneRuntime.getActivitySpace());

        assertThat(surfaceEntityQuad).isNotNull();

        SurfaceEntity surfaceEntitySphere =
                mRenderingRuntime.createSurfaceEntity(
                        SurfaceEntity.StereoMode.TOP_BOTTOM,
                        new Pose(),
                        new SurfaceEntity.Shape.Sphere(kTestSphereRadius),
                        SurfaceEntity.SurfaceProtection.NONE,
                        SurfaceEntity.SuperSampling.DEFAULT,
                        mSceneRuntime.getActivitySpace());

        assertThat(surfaceEntitySphere).isNotNull();

        SurfaceEntity surfaceEntityHemisphere =
                mRenderingRuntime.createSurfaceEntity(
                        SurfaceEntity.StereoMode.MONO,
                        new Pose(),
                        new SurfaceEntity.Shape.Hemisphere(kTestHemisphereRadius),
                        SurfaceEntity.SurfaceProtection.NONE,
                        SurfaceEntity.SuperSampling.DEFAULT,
                        mSceneRuntime.getActivitySpace());

        assertThat(surfaceEntityHemisphere).isNotNull();
        assertThat(mFakeImpressApi.getStereoSurfaceEntities()).hasSize(3);
    }

    @Test
    public void loadTexture_returnsTexture() throws Exception {
        assertThat(loadTexture()).isNotNull();
    }

    @Test
    public void destroyTexture_removesTexture() throws Exception {
        Texture texture = (Texture) loadTexture();
        int initialTextureCount = mFakeImpressApi.getTextureImages().size();

        mFakeImpressApi.destroyNativeObject(texture.getNativeHandle());

        int finalTextureCount = mFakeImpressApi.getTextureImages().size();
        assertThat(finalTextureCount).isEqualTo(initialTextureCount - 1);
    }

    @Test
    public void createWaterMaterial_returnsWaterMaterial() throws Exception {
        assertThat(createWaterMaterial()).isNotNull();
    }

    @Test
    public void destroyWaterMaterial_removesWaterMaterial() throws Exception {
        Material material = (Material) createWaterMaterial();
        int initialMaterialCount = mFakeImpressApi.getMaterials().size();

        mFakeImpressApi.destroyNativeObject(material.getNativeHandle());

        int finalMaterialCount = mFakeImpressApi.getMaterials().size();
        assertThat(finalMaterialCount).isEqualTo(initialMaterialCount - 1);
    }

    @Test
    public void setMaterialOverrideGltfEntity_materialOverridesNode() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        MaterialResource material = createWaterMaterial();
        String nodeName = "fake_node_name";
        int primitiveIndex = 0;

        gltfEntity.setMaterialOverride(material, nodeName, primitiveIndex);

        assertThat(
                        mFakeImpressApi.getImpressNodes().keySet().stream()
                                .filter(
                                        node ->
                                                node.getMaterialOverride() != null
                                                        && node.getMaterialOverride().getType()
                                                                == FakeImpressApiImpl.MaterialData
                                                                        .Type.WATER)
                                .toArray())
                .hasLength(1);
    }

    @Test
    public void clearMaterialOverrideGltfEntity_clearsMaterialOverride() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        MaterialResource material = createWaterMaterial();
        String nodeName = "fake_node_name";
        int primitiveIndex = 0;

        gltfEntity.setMaterialOverride(material, nodeName, primitiveIndex);
        gltfEntity.clearMaterialOverride(nodeName, primitiveIndex);

        assertThat(
                        mFakeImpressApi.getImpressNodes().keySet().stream()
                                .filter(node -> node.getMaterialOverride() != null)
                                .toArray())
                .isEmpty();
    }

    @Test
    public void dispose_clearsAllApiResources() throws Exception {
        mRenderingRuntime.loadExrImageByAssetName("FakeAsset.zip");
        mRenderingRuntime.loadGltfByAssetName("FakeAsset.glb");
        createWaterMaterial();
        createGltfEntity();

        mFakeExecutor.runAll();

        assertThat(mFakeImpressApi.getImageBasedLightingAssets()).isNotEmpty();
        assertThat(mFakeImpressApi.getGltfModels()).isNotEmpty();
        assertThat(mFakeImpressApi.getMaterials()).isNotEmpty();
        assertThat(mFakeImpressApi.getImpressNodes()).isNotEmpty();

        mRenderingRuntime.destroy();

        assertThat(mFakeImpressApi.getImageBasedLightingAssets()).isEmpty();
        assertThat(mFakeImpressApi.getGltfModels()).isEmpty();
        assertThat(mFakeImpressApi.getMaterials()).isEmpty();
        assertThat(mFakeImpressApi.getImpressNodes()).isEmpty();
    }

    @Test
    public void resumeAndPauseRuntime_renderingStatusUpdated() {
        assertThat(mSpatialRenderingRuntime.isFrameLoopStarted()).isFalse();

        mRenderingRuntime.resume();

        assertThat(mSpatialRenderingRuntime.isFrameLoopStarted()).isTrue();

        mRenderingRuntime.pause();

        assertThat(mSpatialRenderingRuntime.isFrameLoopStarted()).isFalse();

        mRenderingRuntime.resume();

        assertThat(mSpatialRenderingRuntime.isFrameLoopStarted()).isTrue();
    }

    @Test
    public void destroy_disposeInvoked() {
        mRenderingRuntime.destroy();

        assertThat(mSpatialRenderingRuntime.isFrameLoopStarted()).isFalse();
        verify(mSplitEngineSubspaceManager).destroy();
    }
}
