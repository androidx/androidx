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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import android.view.Surface;

import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl.GltfNodeData;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl.StereoSurfaceEntityData;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape;
import androidx.xr.scenecore.impl.impress.ImpressApi.StereoMode;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
public final class FakeImpressApiImplTest {
    private FakeImpressApiImpl mFakeImpressApi;
    private BindingsResourceManager mResourceManager;

    @Before
    public void setUp() {
        mFakeImpressApi = new FakeImpressApiImpl();
        mResourceManager = Mockito.mock(BindingsResourceManager.class);
    }

    @Test
    public void loadImageBasedLightingAsset_returnsImageFuture() {
        ListenableFuture<ExrImage> modelFuture =
                mFakeImpressApi.loadImageBasedLightingAsset("fakeEnvironment");
        assertThat(modelFuture).isNotNull();
    }

    @Test
    public void loadImageBasedLightingAsset_withByteArrayAndKey_returnsFuture() {
        byte[] byteArray = new byte[] {};
        ListenableFuture<ExrImage> imageFuture =
                mFakeImpressApi.loadImageBasedLightingAsset(byteArray, "fakeEnvironment");
        assertThat(imageFuture).isNotNull();
    }

    @Test
    public void releaseImageBasedLightingAsset_releasesImage()
            throws ExecutionException, InterruptedException {
        ListenableFuture<ExrImage> imageFuture =
                mFakeImpressApi.loadImageBasedLightingAsset("fakeEnvironment");
        List<Long> images = mFakeImpressApi.getImageBasedLightingAssets();
        assertThat(images).isNotNull();
        assertThat(images).hasSize(1);

        Long imageToken = imageFuture.get().getNativeHandle();
        mFakeImpressApi.releaseImageBasedLightingAsset(imageToken);

        images = mFakeImpressApi.getImageBasedLightingAssets();
        assertThat(images).isEmpty();
    }

    @Test
    public void loadGltfAsset_returnsModelFuture() {
        ListenableFuture<GltfModel> modelFuture = mFakeImpressApi.loadGltfAsset("FakeAsset.glb");
        assertThat(modelFuture).isNotNull();
    }

    @Test
    public void loadGltfAsset_withByteArrayAndKey_returnsModelFuture() {
        byte[] byteArray = new byte[] {};
        ListenableFuture<GltfModel> modelFuture =
                mFakeImpressApi.loadGltfAsset(byteArray, "FakeAsset.glb");
        assertThat(modelFuture).isNotNull();
    }

    @Test
    public void getImpressNodesForToken_returnsNodes()
            throws ExecutionException, InterruptedException {
        ListenableFuture<GltfModel> modelFuture = mFakeImpressApi.loadGltfAsset("FakeAsset.glb");
        Long modelToken = modelFuture.get().getNativeHandle();
        List<Integer> nodes = mFakeImpressApi.getImpressNodesForToken(modelToken);
        assertThat(nodes).isNotNull();
    }

    @Test
    public void releaseGltfAsset_releasesModel() throws ExecutionException, InterruptedException {
        ListenableFuture<GltfModel> modelFuture = mFakeImpressApi.loadGltfAsset("FakeAsset.glb");
        Long modelToken = modelFuture.get().getNativeHandle();
        List<Integer> nodes = mFakeImpressApi.getImpressNodesForToken(modelToken);
        assertThat(nodes).isNotNull();
        mFakeImpressApi.releaseGltfAsset(modelToken);
        nodes = mFakeImpressApi.getImpressNodesForToken(modelToken);
        assertThat(nodes).isEqualTo(null);
    }

    @Test
    public void instanceGltfModel_withCollider_returnsEntityId()
            throws ExecutionException, InterruptedException {
        ListenableFuture<GltfModel> modelFuture = mFakeImpressApi.loadGltfAsset("FakeAsset.glb");
        Long modelToken = modelFuture.get().getNativeHandle();
        ImpressNode entityNode =
                mFakeImpressApi.instanceGltfModel(modelToken, /* enableCollider= */ true);
        assertThat(entityNode.getHandle()).isNotEqualTo(0);
    }

    @Test
    public void instanceGltfModel_withoutCollider_returnsEntityId()
            throws ExecutionException, InterruptedException {
        ListenableFuture<GltfModel> modelFuture = mFakeImpressApi.loadGltfAsset("FakeAsset.glb");
        Long modelToken = modelFuture.get().getNativeHandle();
        ImpressNode entityNode =
                mFakeImpressApi.instanceGltfModel(modelToken, /* enableCollider= */ false);
        assertThat(entityNode.getHandle()).isNotEqualTo(0);
    }

    @Test
    public void createImpressNode_returnsEntityId() {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        assertThat(entityNode.getHandle()).isNotEqualTo(0);
        ImpressNode entityNode2 = mFakeImpressApi.createImpressNode();
        // The entityId is incremented by 1 whenever a new node is created.
        assertThat(entityNode2.getHandle()).isEqualTo(entityNode.getHandle() + 1);
    }

    @Test
    public void getImpressNodes_returnsNodes() {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        Map<GltfNodeData, GltfNodeData> nodes = mFakeImpressApi.getImpressNodes();
        assertThat(nodes).hasSize(1);
        GltfNodeData lastNode =
                Iterables.getLast(nodes.keySet(), Iterables.getLast(nodes.keySet(), null));
        for (GltfNodeData node : nodes.keySet()) {
            lastNode = node;
        }
        assertThat(lastNode).isNotNull();
        assertThat(lastNode.getEntityId()).isEqualTo(entityNode.getHandle());
    }

    @Test
    public void destroyImpressNode_destroysNode() {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        Map<GltfNodeData, GltfNodeData> nodes = mFakeImpressApi.getImpressNodes();
        int nodesSize = nodes.size();
        mFakeImpressApi.destroyImpressNode(entityNode);
        nodes = mFakeImpressApi.getImpressNodes();
        assertThat(nodes).hasSize(nodesSize - 1);
    }

    @Test
    public void setGltfModelColliderEnabled_enablesCollider() {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setGltfModelColliderEnabled(
                                        entityNode, /* enableCollider= */ true));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setGltfModelColliderEnabled_disablesCollider() {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setGltfModelColliderEnabled(
                                        entityNode, /* enableCollider= */ false));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void animateGltfModel_animatesModelWithoutLooping() {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        int animatingSize = mFakeImpressApi.impressNodeAnimatingSize();
        ListenableFuture<Void> future =
                mFakeImpressApi.animateGltfModel(entityNode, "animation_name", /* loop= */ false);
        assertThat(future).isNotNull();
        int animatingSize2 = mFakeImpressApi.impressNodeAnimatingSize();
        assertThat(animatingSize2).isEqualTo(animatingSize + 1);
    }

    @Test
    public void animateGltfModel_animatesModelWithLooping() {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        int animatingSize = mFakeImpressApi.impressNodeLoopAnimatingSize();
        ListenableFuture<Void> future =
                mFakeImpressApi.animateGltfModel(entityNode, "animation_name", /* loop= */ true);
        assertThat(future).isNotNull();
        int animatingSize2 = mFakeImpressApi.impressNodeLoopAnimatingSize();
        assertThat(animatingSize2).isEqualTo(animatingSize + 1);
    }

    @Test
    public void stopGltfModelAnimation_stopsModelWithoutLooping() {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        ListenableFuture<Void> future =
                mFakeImpressApi.animateGltfModel(entityNode, "animation_name", /* loop= */ false);
        assertThat(future).isNotNull();
        int animatingSize = mFakeImpressApi.impressNodeAnimatingSize();
        mFakeImpressApi.stopGltfModelAnimation(entityNode);
        int animatingSize2 = mFakeImpressApi.impressNodeAnimatingSize();
        assertThat(animatingSize2).isEqualTo(animatingSize - 1);
    }

    @Test
    public void stopGltfModelAnimation_stopsModelWithLooping() {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        ListenableFuture<Void> future =
                mFakeImpressApi.animateGltfModel(entityNode, "animation_name", /* loop= */ true);
        assertThat(future).isNotNull();
        int animatingSize = mFakeImpressApi.impressNodeLoopAnimatingSize();
        mFakeImpressApi.stopGltfModelAnimation(entityNode);
        int animatingSize2 = mFakeImpressApi.impressNodeLoopAnimatingSize();
        assertThat(animatingSize2).isEqualTo(animatingSize - 1);
    }

    @Test
    public void impressNodeHasParent_byDefault_returnsFalse() {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        boolean hasParent = mFakeImpressApi.impressNodeHasParent(entityNode);
        assertThat(hasParent).isFalse();
    }

    @Test
    public void impressNodeHasParent_whenParentIsSet_returnsTrue() {
        ImpressNode childEntityNode = mFakeImpressApi.createImpressNode();
        ImpressNode parentEntityNode = mFakeImpressApi.createImpressNode();
        mFakeImpressApi.setImpressNodeParent(childEntityNode, parentEntityNode);
        boolean hasParent = mFakeImpressApi.impressNodeHasParent(childEntityNode);
        assertThat(hasParent).isTrue();
    }

    @Test
    public void getImpressNodeParent_returnsParent() {
        ImpressNode childEntityNode = mFakeImpressApi.createImpressNode();
        ImpressNode parentEntityNode = mFakeImpressApi.createImpressNode();
        mFakeImpressApi.setImpressNodeParent(childEntityNode, parentEntityNode);
        int entityId = mFakeImpressApi.getImpressNodeParent(childEntityNode);
        assertThat(entityId).isEqualTo(parentEntityNode.getHandle());
    }

    @Test
    public void getImpressNodeParent_whenParentIsNotSet_returnsNegativeOne() {
        ImpressNode childEntityNode = mFakeImpressApi.createImpressNode();
        int entityId = mFakeImpressApi.getImpressNodeParent(childEntityNode);
        assertThat(entityId).isEqualTo(-1);
    }

    @Test
    public void createStereoSurface_createsStereoSurface() {
        int stereoMode = StereoMode.MONO;
        ImpressNode stereoSurfaceNode = mFakeImpressApi.createStereoSurface(stereoMode);
        Map<ImpressNode, StereoSurfaceEntityData> stereoSurface =
                mFakeImpressApi.getStereoSurfaceEntities();
        StereoSurfaceEntityData stereoSurfaceData = stereoSurface.get(stereoSurfaceNode);
        assertNotNull(stereoSurfaceData);
        int stereoMode2 = stereoSurfaceData.getStereoMode();
        assertThat(stereoMode).isEqualTo(stereoMode2);
        Surface surface = stereoSurfaceData.getSurface();
        assertThat(surface).isNotNull();
    }

    @Test
    public void setStereoSurfaceEntityCanvasShapeQuad_setsCanvasShapeQuad() {
        int stereoMode = StereoMode.MONO;
        ImpressNode stereoSurfaceNode = mFakeImpressApi.createStereoSurface(stereoMode);
        mFakeImpressApi.setStereoSurfaceEntityCanvasShapeQuad(stereoSurfaceNode, 11.0f, 11.0f);
        Map<ImpressNode, StereoSurfaceEntityData> stereoSurface =
                mFakeImpressApi.getStereoSurfaceEntities();
        StereoSurfaceEntityData stereoSurfaceData = stereoSurface.get(stereoSurfaceNode);
        assertNotNull(stereoSurfaceData);
        CanvasShape canvasShape = stereoSurfaceData.getCanvasShape();
        assertThat(canvasShape).isEqualTo(CanvasShape.QUAD);
        float width = stereoSurfaceData.getWidth();
        assertThat(width).isEqualTo(11.0f);
        float height = stereoSurfaceData.getHeight();
        assertThat(height).isEqualTo(11.0f);
    }

    @Test
    public void setStereoSurfaceEntityCanvasShapeSphere_setsCanvasShapeSphere() {
        int stereoMode = StereoMode.MONO;
        ImpressNode stereoSurfaceNode = mFakeImpressApi.createStereoSurface(stereoMode);
        mFakeImpressApi.setStereoSurfaceEntityCanvasShapeSphere(stereoSurfaceNode, 11.0f);
        Map<ImpressNode, StereoSurfaceEntityData> stereoSurface =
                mFakeImpressApi.getStereoSurfaceEntities();
        StereoSurfaceEntityData stereoSurfaceData = stereoSurface.get(stereoSurfaceNode);
        assertNotNull(stereoSurfaceData);
        CanvasShape canvasShape = stereoSurfaceData.getCanvasShape();
        assertThat(canvasShape).isEqualTo(CanvasShape.VR_360_SPHERE);
        float radius = stereoSurfaceData.getRadius();
        assertThat(radius).isEqualTo(11.0f);
    }

    @Test
    public void setStereoSurfaceEntityCanvasShapeHemisphere_setsCanvasShapeHemisphere() {
        int stereoMode = StereoMode.MONO;
        ImpressNode stereoSurfaceNode = mFakeImpressApi.createStereoSurface(stereoMode);
        mFakeImpressApi.setStereoSurfaceEntityCanvasShapeHemisphere(stereoSurfaceNode, 11.0f);
        Map<ImpressNode, StereoSurfaceEntityData> stereoSurface =
                mFakeImpressApi.getStereoSurfaceEntities();
        StereoSurfaceEntityData stereoSurfaceData = stereoSurface.get(stereoSurfaceNode);
        assertNotNull(stereoSurfaceData);
        CanvasShape canvasShape = stereoSurfaceData.getCanvasShape();
        assertThat(canvasShape).isEqualTo(CanvasShape.VR_180_HEMISPHERE);
        float radius = stereoSurfaceData.getRadius();
        assertThat(radius).isEqualTo(11.0f);
    }

    @Test
    public void getSurfaceFromStereoSurface_returnsSurface() {
        int stereoMode = StereoMode.MONO;
        ImpressNode stereoSurfaceNode = mFakeImpressApi.createStereoSurface(stereoMode);
        Surface surface = mFakeImpressApi.getSurfaceFromStereoSurface(stereoSurfaceNode);
        assertThat(surface).isNotNull();
    }

    @Test
    public void getSurfaceFromStereoSurface_whenSurfaceDoesNotExist_throwsException() {
        ImpressNode stereoSurfaceNode = new ImpressNode(12345);
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.getSurfaceFromStereoSurface(stereoSurfaceNode));
        assertThat(thrown).hasMessageThat().contains("Couldn't find stereo surface entity!");
    }

    @Test
    public void setStereoSurfaceEntitySurfaceSize_whenSizeIsNegative_throwsException() {
        int stereoMode = StereoMode.MONO;
        ImpressNode stereoSurfaceNode = mFakeImpressApi.createStereoSurface(stereoMode);
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setStereoSurfaceEntitySurfaceSize(
                                        stereoSurfaceNode, -1, -1));
        assertThat(thrown).hasMessageThat().contains("Surface dimensions must be positive!");
    }

    @Test
    public void setStereoSurfaceEntitySurfaceSize_whenNodeDoesNotExist_throwsException() {
        ImpressNode stereoSurfaceNode = new ImpressNode(12345);
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setStereoSurfaceEntitySurfaceSize(
                                        stereoSurfaceNode, 640, 480));
        assertThat(thrown).hasMessageThat().contains("Couldn't find stereo surface entity!");
    }

    @Test
    public void setStereoSurfaceEntitySurfaceSize_updatesDimensions() {
        int stereoMode = StereoMode.MONO;
        int kWidth = 640;
        int kHeight = 480;
        ImpressNode stereoSurfaceNode = mFakeImpressApi.createStereoSurface(stereoMode);
        mFakeImpressApi.setStereoSurfaceEntitySurfaceSize(stereoSurfaceNode, kWidth, kHeight);
        Map<ImpressNode, StereoSurfaceEntityData> stereoSurface =
                mFakeImpressApi.getStereoSurfaceEntities();
        StereoSurfaceEntityData stereoSurfaceData = stereoSurface.get(stereoSurfaceNode);
        assertNotNull(stereoSurfaceData);
        int width = stereoSurfaceData.getSurfaceWidth();
        int height = stereoSurfaceData.getSurfaceHeight();
        assertThat(width).isEqualTo(kWidth);
        assertThat(height).isEqualTo(kHeight);
    }

    @Test
    public void setFeatherRadiusForStereoSurface_setsFeatherRadius() {
        int stereoMode = StereoMode.MONO;
        ImpressNode stereoSurfaceNode = mFakeImpressApi.createStereoSurface(stereoMode);
        float radiusX = 11.0f;
        float radiusY = 12.0f;
        mFakeImpressApi.setFeatherRadiusForStereoSurface(stereoSurfaceNode, radiusX, radiusY);
        Map<ImpressNode, StereoSurfaceEntityData> stereoSurface =
                mFakeImpressApi.getStereoSurfaceEntities();
        StereoSurfaceEntityData stereoSurfaceData = stereoSurface.get(stereoSurfaceNode);
        assertNotNull(stereoSurfaceData);
        float featherRadiusX = stereoSurfaceData.getFeatherRadiusX();
        float featherRadiusY = stereoSurfaceData.getFeatherRadiusY();
        assertThat(featherRadiusX).isEqualTo(radiusX);
        assertThat(featherRadiusY).isEqualTo(radiusY);
    }

    @Test
    public void setStereoModeForStereoSurface_setsStereoMode() {
        int stereoMode = StereoMode.MONO;
        ImpressNode stereoSurfaceNode = mFakeImpressApi.createStereoSurface(stereoMode);
        stereoMode = StereoMode.SIDE_BY_SIDE;
        mFakeImpressApi.setStereoModeForStereoSurface(stereoSurfaceNode, stereoMode);
        Map<ImpressNode, StereoSurfaceEntityData> stereoSurface =
                mFakeImpressApi.getStereoSurfaceEntities();
        StereoSurfaceEntityData stereoSurfaceData = stereoSurface.get(stereoSurfaceNode);
        assertNotNull(stereoSurfaceData);
        int stereoMode2 = stereoSurfaceData.getStereoMode();
        assertThat(stereoMode).isEqualTo(stereoMode2);
        stereoMode = StereoMode.TOP_BOTTOM;
        mFakeImpressApi.setStereoModeForStereoSurface(stereoSurfaceNode, stereoMode);
        stereoSurface = mFakeImpressApi.getStereoSurfaceEntities();
        stereoSurfaceData = stereoSurface.get(stereoSurfaceNode);
        assertNotNull(stereoSurfaceData);
        stereoMode2 = stereoSurfaceData.getStereoMode();
        assertThat(stereoMode).isEqualTo(stereoMode2);
    }

    @Test
    public void setStereoSurfaceEntityColliderEnabled_setsColliderEnabled() {
        int stereoMode = StereoMode.MONO;
        ImpressNode stereoSurfaceNode = mFakeImpressApi.createStereoSurface(stereoMode);

        // Enable collider
        mFakeImpressApi.setStereoSurfaceEntityColliderEnabled(stereoSurfaceNode, true);

        Map<ImpressNode, StereoSurfaceEntityData> stereoSurfaces =
                mFakeImpressApi.getStereoSurfaceEntities();
        StereoSurfaceEntityData stereoSurfaceData = stereoSurfaces.get(stereoSurfaceNode);
        assertNotNull(stereoSurfaceData);
        assertThat(stereoSurfaceData.getColliderEnabled()).isTrue();

        // Disable collider
        mFakeImpressApi.setStereoSurfaceEntityColliderEnabled(stereoSurfaceNode, false);
        assertThat(stereoSurfaceData.getColliderEnabled()).isFalse();
    }

    @Test
    public void loadTexture_loadsTexture() throws ExecutionException, InterruptedException {
        ListenableFuture<Texture> textureFuture = mFakeImpressApi.loadTexture("FakeAsset.exr");
        assertThat(textureFuture).isNotNull();
        Texture texture = textureFuture.get();
        assertThat(texture).isNotNull();
    }

    @Test
    public void borrowReflectionTexture_returnsTexture() {
        Texture texture = mFakeImpressApi.borrowReflectionTexture();
        assertThat(texture).isNotNull();
        Texture texture2 =
                new Texture.Builder()
                        .setImpressApi(mFakeImpressApi)
                        .setNativeTexture(texture.getNativeHandle())
                        .build();
        assertThat(texture2).isNotNull();
    }

    @Test
    public void getReflectionTextureFromIbl_returnsTexture() {
        Texture texture = mFakeImpressApi.getReflectionTextureFromIbl(0);
        assertThat(texture).isNotNull();
        Texture texture2 =
                new Texture.Builder()
                        .setImpressApi(mFakeImpressApi)
                        .setNativeTexture(texture.getNativeHandle())
                        .build();
        assertThat(texture2).isNotNull();
    }

    @Test
    public void createWaterMaterial_returnsWaterMaterialFuture()
            throws ExecutionException, InterruptedException {
        ListenableFuture<WaterMaterial> waterMaterialFuture =
                mFakeImpressApi.createWaterMaterial(true);
        assertThat(waterMaterialFuture).isNotNull();
        WaterMaterial waterMaterial = waterMaterialFuture.get();
        assertThat(waterMaterial).isNotNull();
    }

    @Test
    public void setReflectionMapOnWaterMaterial_setsReflectionMapOnWaterMaterial() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setReflectionMapOnWaterMaterial(0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setNormalMapOnWaterMaterial_setsNormalMapOnWaterMaterial() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setNormalMapOnWaterMaterial(0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setNormalTilingOnWaterMaterial_setsNormalTilingOnWaterMaterial() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setNormalTilingOnWaterMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setNormalSpeedOnWaterMaterial_setsNormalSpeedOnWaterMaterial() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setNormalTilingOnWaterMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setAlphaStepMultiplierOnWaterMaterial_setsAlphaStepMultiplierOnWaterMaterial() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setAlphaStepMultiplierOnWaterMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setAlphaMapOnWaterMaterial_setsAlphaMapOnWaterMaterial() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setAlphaMapOnWaterMaterial(0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setNormalZOnWaterMaterial_setsNormalZOnWaterMaterial() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setNormalZOnWaterMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setNormalBoundaryOnWaterMaterial_setsNormalBoundaryOnWaterMaterial() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setNormalBoundaryOnWaterMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setAlphaStepUOnWaterMaterial_setsAlphaStepUOnWaterMaterial() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setAlphaStepUOnWaterMaterial(0, 0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setAlphaStepVOnWaterMaterial_setsAlphaStepVOnWaterMaterial() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setAlphaStepVOnWaterMaterial(0, 0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void createKhronosPbrMaterial_createsKhronosPbrMaterial()
            throws ExecutionException, InterruptedException {
        ListenableFuture<KhronosPbrMaterial> materialFuture =
                mFakeImpressApi.createKhronosPbrMaterial(null);
        assertThat(materialFuture).isNotNull();
        KhronosPbrMaterial material = materialFuture.get();
        assertThat(material).isNotNull();
        Map<Long, FakeImpressApiImpl.MaterialData> materials = mFakeImpressApi.getMaterials();
        assertThat(materials).containsKey(material.getNativeHandle());
    }

    @Test
    public void setBaseColorTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setBaseColorTextureOnKhronosPbrMaterial(0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setBaseColorUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setBaseColorUvTransformOnKhronosPbrMaterial(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setBaseColorFactorsOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setBaseColorFactorsOnKhronosPbrMaterial(
                                        0, 0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setMetallicRoughnessTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setMetallicRoughnessTextureOnKhronosPbrMaterial(
                                        0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setMetallicRoughnessUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setMetallicFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setMetallicFactorOnKhronosPbrMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setRoughnessFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setRoughnessFactorOnKhronosPbrMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setNormalTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setNormalTextureOnKhronosPbrMaterial(0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setNormalUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setNormalUvTransformOnKhronosPbrMaterial(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setNormalFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setNormalFactorOnKhronosPbrMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setAmbientOcclusionTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setAmbientOcclusionTextureOnKhronosPbrMaterial(
                                        0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setAmbientOcclusionUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setAmbientOcclusionFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setAmbientOcclusionFactorOnKhronosPbrMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setEmissiveTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setEmissiveTextureOnKhronosPbrMaterial(0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setEmissiveUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setEmissiveUvTransformOnKhronosPbrMaterial(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setEmissiveFactorsOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setEmissiveFactorsOnKhronosPbrMaterial(0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setClearcoatTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setClearcoatTextureOnKhronosPbrMaterial(0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setClearcoatNormalTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setClearcoatNormalTextureOnKhronosPbrMaterial(
                                        0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setClearcoatRoughnessTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setClearcoatRoughnessTextureOnKhronosPbrMaterial(
                                        0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setClearcoatFactorsOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setClearcoatFactorsOnKhronosPbrMaterial(0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setSheenColorTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setSheenColorTextureOnKhronosPbrMaterial(0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setSheenColorFactorsOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setSheenColorFactorsOnKhronosPbrMaterial(0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setSheenRoughnessTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setSheenRoughnessTextureOnKhronosPbrMaterial(
                                        0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setSheenRoughnessFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setSheenRoughnessFactorOnKhronosPbrMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setTransmissionTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setTransmissionTextureOnKhronosPbrMaterial(
                                        0, 0, null));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setTransmissionUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setTransmissionUvTransformOnKhronosPbrMaterial(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setTransmissionFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setTransmissionFactorOnKhronosPbrMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setIndexOfRefractionOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setIndexOfRefractionOnKhronosPbrMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setAlphaCutoffOnKhronosPbrMaterial_throwsUnimplementedError() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mFakeImpressApi.setAlphaCutoffOnKhronosPbrMaterial(0, 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void destroyNativeObject_destroysNativeWaterMaterialObject()
            throws ExecutionException, InterruptedException {
        WaterMaterial waterMaterial = mFakeImpressApi.createWaterMaterial(true).get();
        long nativeHandle = waterMaterial.getNativeHandle();
        int initialMaterialCount = mFakeImpressApi.getMaterials().size();
        mFakeImpressApi.destroyNativeObject(nativeHandle);
        int finalMaterialCount = mFakeImpressApi.getMaterials().size();
        assertThat(finalMaterialCount).isEqualTo(initialMaterialCount - 1);
    }

    @Test
    public void destroyNativeObject_destroysNativeTextureObject()
            throws ExecutionException, InterruptedException {
        Texture texture = mFakeImpressApi.loadTexture("FakeAsset.exr").get();
        long nativeHandle = texture.getNativeHandle();
        int initialTextureCount = mFakeImpressApi.getTextureImages().size();
        mFakeImpressApi.destroyNativeObject(nativeHandle);
        int finalTextureCount = mFakeImpressApi.getTextureImages().size();
        assertThat(finalTextureCount).isEqualTo(initialTextureCount - 1);
    }

    @Test
    public void setMaterialOverride_setsMaterialOverride()
            throws ExecutionException, InterruptedException {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        WaterMaterial material = mFakeImpressApi.createWaterMaterial(true).get();
        String nodeName = "fake_node_name";
        int primitiveIndex = 0;

        mFakeImpressApi.setMaterialOverride(
                entityNode, material.getNativeHandle(), nodeName, primitiveIndex);
        Map<GltfNodeData, GltfNodeData> nodes = mFakeImpressApi.getImpressNodes();
        boolean foundMaterial = false;
        for (Map.Entry<GltfNodeData, GltfNodeData> node : nodes.entrySet()) {
            if (node.getKey().getEntityId() == entityNode.getHandle()
                    && Objects.requireNonNull(node.getKey().getMaterialOverride())
                                    .getMaterialHandle()
                            == material.getNativeHandle()) {
                foundMaterial = true;
            }
        }
        assertThat(foundMaterial).isTrue();
    }

    @Test
    public void clearMaterialOverride_clearsMaterialOverride()
            throws ExecutionException, InterruptedException {
        ImpressNode entityNode = mFakeImpressApi.createImpressNode();
        WaterMaterial material = mFakeImpressApi.createWaterMaterial(true).get();
        String nodeName = "fake_node_name";
        int primitiveIndex = 0;

        mFakeImpressApi.setMaterialOverride(
                entityNode, material.getNativeHandle(), nodeName, primitiveIndex);
        mFakeImpressApi.clearMaterialOverride(entityNode, nodeName, primitiveIndex);

        Map<GltfNodeData, GltfNodeData> nodes = mFakeImpressApi.getImpressNodes();
        boolean overrideWasCleared = false;
        for (Map.Entry<GltfNodeData, GltfNodeData> node : nodes.entrySet()) {
            if (node.getKey().getEntityId() == entityNode.getHandle()
                    && node.getKey().getMaterialOverride() == null) {
                overrideWasCleared = true;
            }
        }
        assertThat(overrideWasCleared).isTrue();
    }

    @Test
    public void setPreferredEnvironmentLight_setsPreferredEnvironmentLight() {
        long iblToken = 11;
        mFakeImpressApi.setPreferredEnvironmentLight(iblToken);
        long currentEnvironmentLightId = mFakeImpressApi.getCurrentEnvironmentLight();
        assertThat(currentEnvironmentLightId).isEqualTo(iblToken);
    }

    @Test
    public void clearPreferredEnvironmentIblAsset_clearsPreferredEnvironmentIblAsset() {
        long iblToken = 11;
        mFakeImpressApi.setPreferredEnvironmentLight(iblToken);
        mFakeImpressApi.clearPreferredEnvironmentIblAsset();
        long currentEnvironmentLightId = mFakeImpressApi.getCurrentEnvironmentLight();
        // Returns -1 as an id when there's no current environment light id.
        assertThat(currentEnvironmentLightId).isEqualTo(-1);
    }

    @Test
    public void setPrimaryAlphaMaskForStereoSurface_setsPrimaryAlphaMaskForStereoSurface() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setPrimaryAlphaMaskForStereoSurface(
                                        new ImpressNode(0), 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void setAuxiliaryAlphaMaskForStereoSurface_setsAuxiliaryAlphaMaskForStereoSurface() {
        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFakeImpressApi.setAuxiliaryAlphaMaskForStereoSurface(
                                        new ImpressNode(0), 0));
        assertThat(thrown).hasMessageThat().contains("not implemented");
    }

    @Test
    public void disposeAllResources_disposesAllResources() {
        ListenableFuture<ExrImage> unused =
                mFakeImpressApi.loadImageBasedLightingAsset("fakeEnvironment");
        ImpressNode unused2 = mFakeImpressApi.createImpressNode();
        ListenableFuture<GltfModel> unused3 = mFakeImpressApi.loadGltfAsset("fakeAsset");
        ListenableFuture<Texture> unused4 = mFakeImpressApi.loadTexture("FakeAsset.exr");
        ListenableFuture<WaterMaterial> unused5 = mFakeImpressApi.createWaterMaterial(false);
        mFakeImpressApi.disposeAllResources();
        assertThat(mFakeImpressApi.getImageBasedLightingAssets()).isEmpty();
        assertThat(mFakeImpressApi.getImpressNodes()).isEmpty();
        assertThat(mFakeImpressApi.getGltfModels()).isEmpty();
        assertThat(mFakeImpressApi.getTextureImages()).isEmpty();
        assertThat(mFakeImpressApi.getMaterials()).isEmpty();
    }
}
