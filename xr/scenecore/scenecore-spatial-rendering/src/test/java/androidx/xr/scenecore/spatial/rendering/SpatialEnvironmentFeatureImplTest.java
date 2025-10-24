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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl.MaterialData;
import androidx.xr.scenecore.impl.impress.GltfModel;
import androidx.xr.scenecore.impl.impress.ImpressNode;
import androidx.xr.scenecore.runtime.ExrImageResource;
import androidx.xr.scenecore.runtime.GltfModelResource;
import androidx.xr.scenecore.runtime.MaterialResource;
import androidx.xr.scenecore.runtime.SpatialEnvironment.SpatialEnvironmentPreference;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.environment.EnvironmentVisibilityState;
import com.android.extensions.xr.environment.PassthroughVisibilityState;
import com.android.extensions.xr.environment.ShadowEnvironmentVisibilityState;
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.space.ShadowSpatialState;
import com.android.extensions.xr.space.SpatialState;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// Technically this doesn't need to be a Robolectric test, since it doesn't directly depend on
// any Android subsystems. However, we're currently using an Android test runner for consistency
// with other Android XR impl tests in this directory.
/** Unit tests for the AndroidXR implementation of JXRCore's SpatialEnvironment module. */
@RunWith(RobolectricTestRunner.class)
public final class SpatialEnvironmentFeatureImplTest {
    private static final int SUBSPACE_ID = 5;
    private static final int INVALID_SPLIT_ENGINE_ID = -1;
    private static final long WATER_MATERIAL_ID = 1;
    private final FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    private ActivityController<Activity> mActivityController;
    private Activity mActivity;
    private XrExtensions mXrExtensions = null;
    private Node mSubspaceNode;
    private SubspaceNode mExpectedSubspace;
    private SpatialEnvironmentFeatureImpl mEnvironment = null;
    private SplitEngineSubspaceManager mSplitEngineSubspaceManager;

    @Before
    public void setUp() {
        mActivityController = Robolectric.buildActivity(Activity.class);
        mActivity = mActivityController.create().start().get();
        // Reset our state.
        mXrExtensions = XrExtensionsProvider.getXrExtensions();
        Node sceneRootNode = mXrExtensions.createNode();
        mSubspaceNode = mXrExtensions.createNode();
        mExpectedSubspace = new SubspaceNode(SUBSPACE_ID, mSubspaceNode);

        mSplitEngineSubspaceManager = Mockito.mock(SplitEngineSubspaceManager.class);
        when(mSplitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
                .thenReturn(mExpectedSubspace);

        mEnvironment =
                new SpatialEnvironmentFeatureImpl(
                        mActivity, mFakeImpressApi, mSplitEngineSubspaceManager, mXrExtensions);
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private ExrImageResource fakeLoadEnvironment(String name) {
        try {
            return mFakeImpressApi.loadImageBasedLightingAsset(name).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private GltfModelResource fakeLoadGltfAsset(String name) {
        try {
            return mFakeImpressApi.loadGltfAsset(name).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private MaterialResource fakeLoadMaterial(boolean isAlphaMapVersion) {
        try {
            return mFakeImpressApi.createWaterMaterial(isAlphaMapVersion).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    @Test
    public void getPreferredSpatialEnvironment_returnsSetPreferredSpatialEnvironment() {
        SpatialEnvironmentPreference preference = new SpatialEnvironmentPreference(null, null);
        mEnvironment.setPreferredSpatialEnvironment(preference);

        assertThat(mEnvironment.getPreferredSpatialEnvironment()).isEqualTo(preference);
    }

    @Test
    public void setPreferredSpatialEnvironmentNull_removesEnvironment() {
        ExrImageResource exr = fakeLoadEnvironment("fakeEnvironmentAsset");
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");

        // Ensure that an environment is set.
        mEnvironment.setPreferredSpatialEnvironment(new SpatialEnvironmentPreference(exr, gltf));

        long initialSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        List<Integer> geometryNodes =
                mFakeImpressApi.getImpressNodesForToken(((GltfModel) gltf).getNativeHandle());
        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();
        int animatingNodes = mFakeImpressApi.impressNodeAnimatingSize();
        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(geometryNodes).isNotEmpty();
        assertThat(materials).isEmpty();
        assertThat(animatingNodes).isEqualTo(0);
        assertThat(loopingAnimatingNodes).isEqualTo(0);

        assertThat(mFakeImpressApi.impressNodeHasParent(new ImpressNode(geometryNodes.get(0))))
                .isTrue();

        // Ensure environment is removed
        mEnvironment.setPreferredSpatialEnvironment(null);

        long finalSkybox = mFakeImpressApi.getCurrentEnvironmentLight();

        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNull();
    }

    @Test
    public void
            setPreferredSpatialEnvironmentWithNullSkyboxAndNullGeometry_doesNotDetachEnvironment() {
        ExrImageResource exr = fakeLoadEnvironment("fakeEnvironmentAsset");
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");

        // Ensure that an environment is set.
        mEnvironment.setPreferredSpatialEnvironment(new SpatialEnvironmentPreference(exr, gltf));

        long initialSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        List<Integer> geometryNodes =
                mFakeImpressApi.getImpressNodesForToken(((GltfModel) gltf).getNativeHandle());

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(geometryNodes).isNotEmpty();

        assertThat(mFakeImpressApi.impressNodeHasParent(new ImpressNode(geometryNodes.get(0))))
                .isTrue();

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        mEnvironment.setPreferredSpatialEnvironment(new SpatialEnvironmentPreference(null, null));

        long finalSkybox = mFakeImpressApi.getCurrentEnvironmentLight();

        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void
            setPreferredSpatialEnvWithSkyboxAndGeoWithNodeAndAnimation_doesNotDetachEnvironment() {
        ExrImageResource exr = fakeLoadEnvironment("fakeEnvironment");
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");
        // Create dummy regular version of the water material.
        MaterialResource material = fakeLoadMaterial(false);
        String nodeName = "fakeNode";
        String animationName = "fakeAnimation";

        // Ensure that an environment is set.
        mEnvironment.setPreferredSpatialEnvironment(
                new SpatialEnvironmentPreference(exr, gltf, material, nodeName, animationName));

        long initialSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        List<Integer> geometryNodes =
                mFakeImpressApi.getImpressNodesForToken(((GltfModel) gltf).getNativeHandle());
        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();
        int animatingNodes = mFakeImpressApi.impressNodeAnimatingSize();
        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(geometryNodes).isNotEmpty();
        assertThat(mFakeImpressApi.impressNodeHasParent(new ImpressNode(geometryNodes.get(0))))
                .isTrue();
        assertThat(materials).isNotEmpty();
        assertThat(materials.keySet().toArray()[0]).isEqualTo(WATER_MATERIAL_ID);
        assertThat(materials.get(WATER_MATERIAL_ID).getType()).isEqualTo(MaterialData.Type.WATER);
        assertThat(animatingNodes).isEqualTo(0);
        assertThat(loopingAnimatingNodes).isEqualTo(1);

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        mEnvironment.setPreferredSpatialEnvironment(new SpatialEnvironmentPreference(null, null));

        long finalSkybox = mFakeImpressApi.getCurrentEnvironmentLight();

        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void
            setPreferredSpatialEnvFromNullPrefToNullSkyboxAndGeometry_doesNotDetachEnvironment() {
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");

        // Ensure that an environment is set.
        mEnvironment.setPreferredSpatialEnvironment(null);

        long initialSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        List<Integer> geometryNodes =
                mFakeImpressApi.getImpressNodesForToken(((GltfModel) gltf).getNativeHandle());

        assertThat(initialSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(geometryNodes).isEmpty();

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        mEnvironment.setPreferredSpatialEnvironment(new SpatialEnvironmentPreference(null, null));

        long finalSkybox = mFakeImpressApi.getCurrentEnvironmentLight();

        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void setNewSpatialEnvironmentPreference_replacesOldSpatialEnvironmentPreference() {
        ExrImageResource exr = fakeLoadEnvironment("fakeEnvironment");
        ExrImageResource newExr = fakeLoadEnvironment("newFakeEnvironment");
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");
        GltfModelResource newGltf = fakeLoadGltfAsset("newFakeGltfAsset");

        // Ensure that an environment is set a first time.
        mEnvironment.setPreferredSpatialEnvironment(new SpatialEnvironmentPreference(exr, gltf));

        long initialSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        List<Integer> geometryNodes =
                mFakeImpressApi.getImpressNodesForToken(((GltfModel) gltf).getNativeHandle());

        // Ensure that an environment is set a second time.
        mEnvironment.setPreferredSpatialEnvironment(
                new SpatialEnvironmentPreference(newExr, newGltf));

        long newSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        List<Integer> newGeometryNodes =
                mFakeImpressApi.getImpressNodesForToken(((GltfModel) newGltf).getNativeHandle());

        // None of the nodes should be null.
        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(geometryNodes).isNotEmpty();
        assertThat(newSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(newGeometryNodes).isNotEmpty();
        // Only the new nodes should have a parent.
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();
        assertThat(mFakeImpressApi.impressNodeHasParent(new ImpressNode(newGeometryNodes.get(0))))
                .isTrue();
        // The resources should be different.
        assertThat(initialSkybox).isNotEqualTo(newSkybox);
        assertThat(geometryNodes.get(0)).isNotEqualTo(newGeometryNodes.get(0));
        // The environment node should still be attached.
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void setNewSpatialEnvironmentPreference_callsOnBeforeNodeAttachedListener() {
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");
        AtomicInteger timesCalled = new AtomicInteger();

        mEnvironment.accept(node -> timesCalled.getAndIncrement());

        // Ensure that an environment is set a first time.
        mEnvironment.setPreferredSpatialEnvironment(new SpatialEnvironmentPreference(null, gltf));

        assertThat(timesCalled.get()).isEqualTo(1);
    }

    @Test
    public void
            setPreferredSpatialEnvironmentGeometryWithMaterialAndNodeName_materialIsOverriden() {
        ExrImageResource exr = fakeLoadEnvironment("fakeEnvironment");
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");
        // Create dummy regular version of the water material.
        MaterialResource material = fakeLoadMaterial(false);
        String nodeName = "fakeNode";
        String animationName = "fakeAnimation";

        // Ensure that an environment is set.
        mEnvironment.setPreferredSpatialEnvironment(
                new SpatialEnvironmentPreference(exr, gltf, material, nodeName, animationName));

        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();
        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();

        assertThat(
                        mFakeImpressApi.getImpressNodes().keySet().stream()
                                .filter(
                                        node ->
                                                node.getMaterialOverride() != null
                                                        && node.getMaterialOverride().getType()
                                                                == MaterialData.Type.WATER)
                                .toArray())
                .hasLength(1); // 1 glTF node that should be overridden with the water material.
        assertThat(materials).isNotEmpty();
        assertThat(materials.keySet().toArray()[0]).isEqualTo(WATER_MATERIAL_ID);
        assertThat(materials.get(WATER_MATERIAL_ID).getType()).isEqualTo(MaterialData.Type.WATER);
        assertThat(loopingAnimatingNodes).isEqualTo(1);
    }

    @Test
    public void setPreferredSpatialEnvGeometryWithMaterialAndNoNodeName_materialIsNotOverriden() {
        ExrImageResource exr = fakeLoadEnvironment("fakeEnvironment");
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");
        // Create dummy regular version of the water material.
        MaterialResource material = fakeLoadMaterial(false);
        String animationName = "fakeAnimation";

        // Ensure that an environment is set.
        mEnvironment.setPreferredSpatialEnvironment(
                new SpatialEnvironmentPreference(exr, gltf, material, null, animationName));

        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();

        // 2 nodes are subspace (parent) and glTF (child) used for the environment. Both have no
        // material override so we expect the length of the filter to be 2.
        assertThat(
                        mFakeImpressApi.getImpressNodes().keySet().stream()
                                .filter(node -> node.getMaterialOverride() == null)
                                .toArray())
                .hasLength(2);
        assertThat(materials).isNotEmpty();
        assertThat(materials.keySet().toArray()[0]).isEqualTo(WATER_MATERIAL_ID);
        assertThat(materials.get(WATER_MATERIAL_ID).getType()).isEqualTo(MaterialData.Type.WATER);
    }

    @Test
    public void setPreferredSpatialEnvGeometryWithNoMaterialAndNodeName_materialIsNotOverriden() {
        ExrImageResource exr = fakeLoadEnvironment("fakeEnvironment");
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");
        String nodeName = "fakeNode";
        String animationName = "fakeAnimation";

        // Ensure that an environment is set.
        mEnvironment.setPreferredSpatialEnvironment(
                new SpatialEnvironmentPreference(exr, gltf, null, nodeName, animationName));

        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();

        // 2 nodes are subspace (parent) and glTF (child) used for the environment. Both have no
        // material override so we expect the length of the filter to be 2.
        assertThat(
                        mFakeImpressApi.getImpressNodes().keySet().stream()
                                .filter(node -> node.getMaterialOverride() == null)
                                .toArray())
                .hasLength(2);
        assertThat(materials).isEmpty();
    }

    @Test
    public void setPreferredSpatialEnvironmentGeometryWithNoAnimationName_geometryIsNotAnimating() {
        ExrImageResource exr = fakeLoadEnvironment("fakeEnvironment");
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");
        String animationName = "fakeAnimation";

        // Ensure that an environment is set.
        mEnvironment.setPreferredSpatialEnvironment(
                new SpatialEnvironmentPreference(exr, gltf, null, null, animationName));

        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();
        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();

        assertThat(loopingAnimatingNodes).isEqualTo(1);
        // 2 nodes are subspace (parent) and glTF (child) used for the environment. Both have no
        // material override so we expect the length of the filter to be 2.
        assertThat(
                        mFakeImpressApi.getImpressNodes().keySet().stream()
                                .filter(node -> node.getMaterialOverride() == null)
                                .toArray())
                .hasLength(2);
        assertThat(materials).isEmpty();
    }

    @Test
    public void dispose_clearsResources() {
        ExrImageResource exr = fakeLoadEnvironment("fakeEnvironment");
        GltfModelResource gltf = fakeLoadGltfAsset("fakeGltfAsset");
        SpatialState spatialState = ShadowSpatialState.create();
        ShadowSpatialState.extract(spatialState)
                .setEnvironmentVisibilityState(
                        /* environmentVisibilityState= */ ShadowEnvironmentVisibilityState.create(
                                EnvironmentVisibilityState.APP_VISIBLE));
        ShadowSpatialState.extract(spatialState)
                .setPassthroughVisibilityState(
                        /* passthroughVisibilityState= */ ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.APP, 0.5f));

        mEnvironment.setPreferredSpatialEnvironment(new SpatialEnvironmentPreference(exr, gltf));

        long initialSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        List<Integer> geometryNodes =
                mFakeImpressApi.getImpressNodesForToken(((GltfModel) gltf).getNativeHandle());

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(geometryNodes).isNotEmpty();
        assertThat(mFakeImpressApi.impressNodeHasParent(new ImpressNode(geometryNodes.get(0))))
                .isTrue();
        assertThat(mEnvironment.getPreferredSpatialEnvironment()).isNotNull();

        mEnvironment.dispose();

        long finalSkybox = mFakeImpressApi.getCurrentEnvironmentLight();

        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID);
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNull();
        assertThat(mEnvironment.getPreferredSpatialEnvironment()).isNull();
    }
}
