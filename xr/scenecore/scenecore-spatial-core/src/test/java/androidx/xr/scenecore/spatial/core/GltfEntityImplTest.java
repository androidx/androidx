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

package androidx.xr.scenecore.spatial.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.runtime.NodeHolder;
import androidx.xr.runtime.math.BoundingBox;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.GltfEntity;
import androidx.xr.scenecore.runtime.GltfFeature;
import androidx.xr.scenecore.runtime.MaterialResource;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeGltfFeature;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class GltfEntityImplTest {
    private static final int OPEN_XR_REFERENCE_SPACE_TYPE = 1;
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final EntityManager mEntityManager = new EntityManager();
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private ActivitySpaceImpl mActivitySpace;
    private GltfEntityImpl mGltfEntity;
    private GltfFeature mFakeGltfFeature;
    private final GltfFeature mMockGltfFeature = Mockito.mock(GltfFeature.class);

    @Before
    public void setUp() {
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

    @After
    public void tearDown() {
        if (mGltfEntity != null) mGltfEntity.dispose();
        mGltfEntity = null;
    }

    private GltfEntityImpl createGltfEntity(Activity activity) {
        NodeHolder<?> nodeHolder = new NodeHolder<>(mXrExtensions.createNode(), Node.class);
        mFakeGltfFeature =
                FakeGltfFeature.Companion.createWithMockFeature(mMockGltfFeature, nodeHolder);

        return new GltfEntityImpl(
                activity,
                mFakeGltfFeature,
                mActivitySpace,
                mXrExtensions,
                mEntityManager,
                mExecutor);
    }

    @Test
    public void getGltfModelBoundingBox_returnsBoundingBox() {
        BoundingBox expectedResult = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One);
        when(mMockGltfFeature.getGltfModelBoundingBox()).thenReturn(expectedResult);

        BoundingBox boundingBox = mGltfEntity.getGltfModelBoundingBox();

        verify(mMockGltfFeature).getGltfModelBoundingBox();
        assertThat(boundingBox).isEqualTo(expectedResult);
    }

    @Test
    public void startAnimation_startsAnimation() {
        when(mMockGltfFeature.getAnimationState()).thenReturn(GltfEntity.AnimationState.PLAYING);

        mGltfEntity.startAnimation(/* looping= */ true, "test_animation");

        verify(mMockGltfFeature).startAnimation(true, "test_animation", mExecutor);
        assertThat(mGltfEntity.getAnimationState()).isEqualTo(GltfEntity.AnimationState.PLAYING);
    }

    @Test
    public void stopAnimation_stopsAnimation() {
        mGltfEntity.startAnimation(/* looping= */ true, "test_animation");

        verify(mMockGltfFeature).startAnimation(true, "test_animation", mExecutor);

        mGltfEntity.stopAnimation();

        verify(mMockGltfFeature).stopAnimation();
    }

    @Test
    public void setMaterialOverrideGltfEntity_materialOverridesNode() throws Exception {
        MaterialResource material = Mockito.mock(MaterialResource.class);
        String nodeName = "fake_node_name";
        int primitiveIndex = 0;

        mGltfEntity.setMaterialOverride(material, nodeName, primitiveIndex);

        verify(mMockGltfFeature).setMaterialOverride(material, nodeName, primitiveIndex);
    }

    @Test
    public void clearMaterialOverrideGltfEntity_clearsMaterialOverride() throws Exception {
        String nodeName = "fake_node_name";
        int primitiveIndex = 0;

        mGltfEntity.clearMaterialOverride(nodeName, primitiveIndex);

        verify(mMockGltfFeature).clearMaterialOverride(nodeName, primitiveIndex);
    }

    @Test
    public void dispose_featureDisposed() {
        mGltfEntity.dispose();

        verify(mMockGltfFeature).dispose();

        mGltfEntity = null;
    }
}
