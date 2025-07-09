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

package androidx.xr.scenecore.impl;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.xr.runtime.internal.GltfEntity;
import androidx.xr.runtime.internal.MaterialResource;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl;
import androidx.xr.scenecore.impl.impress.WaterMaterial;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
public class GltfEntityImplTest {
    private static final int OPEN_XR_REFERENCE_SPACE_TYPE = 1;
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final EntityManager mEntityManager = new EntityManager();
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private ActivitySpaceImpl mActivitySpace;
    private final FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private GltfEntityImpl mGltfEntity;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();

        assertThat(mXrExtensions).isNotNull();

        ShadowXrExtensions.extract(mXrExtensions)
                .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE);

        Node taskNode = mXrExtensions.createNode();
        mActivitySpace =
                new ActivitySpaceImpl(
                        taskNode,
                        activity,
                        mXrExtensions,
                        mEntityManager,
                        () -> mXrExtensions.getSpatialState(activity),
                        /* unscaledGravityAlignedActivitySpace= */ false,
                        mExecutor);

        mGltfEntity = createGltfEntity(activity);
    }

    private GltfEntityImpl createGltfEntity(Activity activity)
            throws ExecutionException, InterruptedException {
        long modelToken = -1;
        ListenableFuture<Long> modelTokenFuture =
                mFakeImpressApi.loadGltfAsset("FakeGltfAsset.glb");
        modelToken = modelTokenFuture.get();
        GltfModelResourceImpl modelResource = new GltfModelResourceImpl(modelToken);
        return new GltfEntityImpl(
                activity,
                modelResource,
                mActivitySpace,
                mFakeImpressApi,
                mSplitEngineSubspaceManager,
                mXrExtensions,
                mEntityManager,
                mExecutor);
    }

    @Nullable
    private MaterialResource createWaterMaterial(boolean isAlphaMapVersion)
            throws ExecutionException, InterruptedException {
        ResolvableFuture<MaterialResource> materialResourceFuture = ResolvableFuture.create();
        ListenableFuture<androidx.xr.scenecore.impl.impress.WaterMaterial> materialFuture =
                mFakeImpressApi.createWaterMaterial(isAlphaMapVersion);

        WaterMaterial material = materialFuture.get();
        materialResourceFuture.set(new MaterialResourceImpl(material.getNativeHandle()));

        return materialResourceFuture.get();
    }

    @Test
    public void startAnimation_startsAnimation() {
        mGltfEntity.startAnimation(/* looping= */ true, "test_animation");

        assertThat(mGltfEntity.getAnimationState()).isEqualTo(GltfEntity.AnimationState.PLAYING);
    }

    @Test
    public void stopAnimation_stopsAnimation() {
        mGltfEntity.startAnimation(/* looping= */ true, "test_animation");

        assertThat(mGltfEntity.getAnimationState()).isEqualTo(GltfEntity.AnimationState.PLAYING);

        mGltfEntity.stopAnimation();

        assertThat(mGltfEntity.getAnimationState()).isEqualTo(GltfEntity.AnimationState.STOPPED);
    }

    @Test
    public void setMaterialOverrideGltfEntity_materialOverridesMesh() throws Exception {
        MaterialResource material = createWaterMaterial(/* isAlphaMapVersion= */ false);

        assertThat(material).isNotNull();

        mGltfEntity.setMaterialOverride(material, "fake_mesh_name");

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

    // TODO: b/426594104 provide a fake SplitEngineSubspaceManager and cover the dispose() method
}
