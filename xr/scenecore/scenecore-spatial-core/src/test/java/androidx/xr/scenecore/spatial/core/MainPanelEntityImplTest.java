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

import android.app.Activity;

import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector2;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.PixelDimensions;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.NodeRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public class MainPanelEntityImplTest {
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mHostActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private SpatialSceneRuntime mTestRuntime;
    private MainPanelEntityImpl mMainPanelEntity;

    @Before
    public void setUp() {
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(1);
        mTestRuntime =
                SpatialSceneRuntime.create(
                        mHostActivity,
                        mFakeExecutor,
                        mXrExtensions,
                        new EntityManager(),
                        /* unscaledGravityAlignedActivitySpace= */ false);

        mMainPanelEntity = (MainPanelEntityImpl) mTestRuntime.getMainPanelEntity();
    }

    @After
    public void tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        mTestRuntime.destroy();
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(null);
    }

    @Test
    public void runtimeGetMainPanelEntity_returnsPanelEntityImpl() {
        assertThat(mMainPanelEntity).isNotNull();
    }

    @Test
    public void mainPanelEntitysetSizeInPixels_callsExtensions() {
        PixelDimensions kTestPixelDimensions = new PixelDimensions(14, 14);
        mMainPanelEntity.setSizeInPixels(kTestPixelDimensions);

        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);
        assertThat(shadowXrExtensions.getMainWindowWidth(mHostActivity))
                .isEqualTo(kTestPixelDimensions.width);
        assertThat(shadowXrExtensions.getMainWindowHeight(mHostActivity))
                .isEqualTo(kTestPixelDimensions.height);
    }

    @Test
    public void mainPanelEntitySetSize_callsExtensions() {
        Dimensions kTestDimensions = new Dimensions(123.0f, 123.0f, 123.0f);
        mMainPanelEntity.setSize(kTestDimensions);

        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);
        assertThat(shadowXrExtensions.getMainWindowWidth(mHostActivity))
                .isEqualTo((int) kTestDimensions.width);
        assertThat(shadowXrExtensions.getMainWindowHeight(mHostActivity))
                .isEqualTo((int) kTestDimensions.height);
    }

    @Test
    public void createActivityPanelEntity_setsCornersTo32Dp() {
        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter. Validate that the
        // corner radius is set to 32dp.
        assertThat(mMainPanelEntity.getCornerRadius()).isEqualTo(32.0f);
        assertThat(NodeRepository.getInstance().getCornerRadius(mMainPanelEntity.getNode()))
                .isEqualTo(32.0f);
    }

    @Test
    public void transformPixelCoordinatesToPose_topLeft_returnsCorrectPose() {
        Pose pose = mMainPanelEntity.transformPixelCoordinatesToPose(new Vector2(0f, 0f));
        Vector3 expected =
                new Vector3(
                        mMainPanelEntity.getSize().width * -0.5f,
                        mMainPanelEntity.getSize().height * 0.5f,
                        0.0f);
        assertThat(pose.getTranslation()).isEqualTo(expected);
    }

    @Test
    public void transformNormalizedCoordinatesToPose_center_returnsIdentity() {
        Pose pose = mMainPanelEntity.transformNormalizedCoordinatesToPose(new Vector2(0f, 0f));
        assertThat(pose).isEqualTo(Pose.Identity);
    }

    @Test
    public void transformNormalizedCoordinatesToPose_topLeft_returnsCorrectPose() {
        Pose pose = mMainPanelEntity.transformNormalizedCoordinatesToPose(new Vector2(-1f, 1f));
        Vector3 expected =
                new Vector3(
                        mMainPanelEntity.getSize().width * -0.5f,
                        mMainPanelEntity.getSize().height * 0.5f,
                        0.0f);
        assertThat(pose.getTranslation()).isEqualTo(expected);
    }
}
