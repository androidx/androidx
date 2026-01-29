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

import static org.junit.Assert.assertThrows;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;

import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector2;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider;
import androidx.xr.scenecore.runtime.ActivityPanelEntity;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.PixelDimensions;
import androidx.xr.scenecore.runtime.SceneRuntime;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.NodeRepository;
import com.android.extensions.xr.space.ActivityPanel;
import com.android.extensions.xr.space.ShadowActivityPanel;

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
public class ActivityPanelEntityImplTest {
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mHostActivity = mActivityController.create().start().get();
    private final PixelDimensions mWindowBoundsPx = new PixelDimensions(640, 480);
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();
    private SceneRuntime mFakeRuntime;

    @Before
    public void setUp() {
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(1);
        mFakeRuntime =
                SpatialSceneRuntime.create(
                        mHostActivity,
                        mFakeExecutor,
                        mXrExtensions,
                        new EntityManager(),
                        /* unscaledGravityAlignedActivitySpace= */ false);
    }

    @After
    public void tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        mFakeRuntime.destroy();
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(null);
    }

    private ActivityPanelEntity createActivityPanelEntity() {
        return createActivityPanelEntity(mWindowBoundsPx);
    }

    private ActivityPanelEntity createActivityPanelEntity(PixelDimensions windowBoundsPx) {
        Pose mPose = new Pose();

        return mFakeRuntime.createActivityPanelEntity(
                mPose, windowBoundsPx, "test", mHostActivity, mFakeRuntime.getActivitySpace());
    }

    @Test
    public void createActivityPanelEntity_returnsActivityPanelEntity() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        assertThat(activityPanelEntity).isNotNull();
    }

    @Test
    public void createActivityPanelEntity_setsCornersTo32dp() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter. Validate that the
        // corner radius is set to 32dp.
        assertThat(activityPanelEntity.getCornerRadius()).isEqualTo(32.0f);
        assertThat(
                        mNodeRepository.getCornerRadius(
                                ((ActivityPanelEntityImpl) activityPanelEntity).getNode()))
                .isEqualTo(32.0f);
    }

    @Test
    public void createPanel_smallPanelWidth_setsCornerRadiusToPanelSize() {
        ActivityPanelEntity activityPanelEntity =
                createActivityPanelEntity(new PixelDimensions(40, 1000));

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to half the width.
        assertThat(activityPanelEntity.getCornerRadius()).isEqualTo(20f);
        assertThat(
                        mNodeRepository.getCornerRadius(
                                ((ActivityPanelEntityImpl) activityPanelEntity).getNode()))
                .isEqualTo(20f);
    }

    @Test
    public void createPanel_smallPanelHeight_setsCornerRadiusToPanelSize() {
        ActivityPanelEntity activityPanelEntity =
                createActivityPanelEntity(new PixelDimensions(1000, 40));

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to half the height.
        assertThat(activityPanelEntity.getCornerRadius()).isEqualTo(20f);
        assertThat(
                        mNodeRepository.getCornerRadius(
                                ((ActivityPanelEntityImpl) activityPanelEntity).getNode()))
                .isEqualTo(20f);
    }

    @Test
    public void activityPanelEntityStartActivity_callsActivityPanel() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        Intent launchIntent = mActivityController.getIntent();
        activityPanelEntity.launchActivity(launchIntent, null);

        ShadowActivityPanel panel =
                ShadowActivityPanel.extract(
                        ShadowXrExtensions.extract(mXrExtensions)
                                .getActivityPanelForHost(mHostActivity));

        assertThat(panel.getLaunchIntent()).isEqualTo(launchIntent);
        assertThat(panel.getBundle()).isNull();
        assertThat(panel.getBounds())
                .isEqualTo(new Rect(0, 0, mWindowBoundsPx.width, mWindowBoundsPx.height));
    }

    @Test
    public void activityPanelEntityMoveActivity_callActivityPanel() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        activityPanelEntity.moveActivity(mHostActivity);

        ShadowActivityPanel panel =
                ShadowActivityPanel.extract(
                        ShadowXrExtensions.extract(mXrExtensions)
                                .getActivityPanelForHost(mHostActivity));

        assertThat(panel.getActivity()).isEqualTo(mHostActivity);

        assertThat(panel.getBounds())
                .isEqualTo(new Rect(0, 0, mWindowBoundsPx.width, mWindowBoundsPx.height));
    }

    @Test
    public void activityPanelEntitySetSize_callsSetSizeInPixels() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        Dimensions dimensions = new Dimensions(400f, 300f, 0f);
        activityPanelEntity.setSize(dimensions);

        ActivityPanel panel =
                ShadowXrExtensions.extract(mXrExtensions).getActivityPanelForHost(mHostActivity);

        assertThat(ShadowActivityPanel.extract(panel).getBounds())
                .isEqualTo(new Rect(0, 0, (int) dimensions.width, (int) dimensions.height));

        // SetSize redirects to setSizeInPixels, so we check the same thing here.
        PixelDimensions viewDimensions = activityPanelEntity.getSizeInPixels();

        assertThat(viewDimensions.width).isEqualTo((int) dimensions.width);
        assertThat(viewDimensions.height).isEqualTo((int) dimensions.height);
    }

    @Test
    public void activityPanelEntitysetSizeInPixels_callActivityPanel() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        PixelDimensions dimensions = new PixelDimensions(400, 300);
        activityPanelEntity.setSizeInPixels(dimensions);

        ActivityPanel panel =
                ShadowXrExtensions.extract(mXrExtensions).getActivityPanelForHost(mHostActivity);

        assertThat(ShadowActivityPanel.extract(panel).getBounds())
                .isEqualTo(new Rect(0, 0, dimensions.width, dimensions.height));

        PixelDimensions viewDimensions = activityPanelEntity.getSizeInPixels();

        assertThat(viewDimensions.width).isEqualTo(dimensions.width);
        assertThat(viewDimensions.height).isEqualTo(dimensions.height);
    }

    @Test
    public void activityPanelEntityDispose_callsActivityPanelDelete() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        activityPanelEntity.dispose();

        ActivityPanel panel =
                ShadowXrExtensions.extract(mXrExtensions).getActivityPanelForHost(mHostActivity);

        assertThat(ShadowActivityPanel.extract(panel).isDeleted()).isTrue();
    }

    @Test
    public void transformPixelCoordinatesToPose_topLeft_returnsCorrectPose() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        activityPanelEntity.setSize(new Dimensions(1f, 1f, 0f));
        Pose pose = activityPanelEntity.transformPixelCoordinatesToPose(new Vector2(0f, 0f));
        Vector3 expected = new Vector3(-0.5f, 0.5f, 0.0f);
        assertThat(pose.getTranslation()).isEqualTo(expected);
    }

    @Test
    public void transformNormalizedCoordinatesToPose_center_returnsIdentity() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        Pose pose = activityPanelEntity.transformNormalizedCoordinatesToPose(new Vector2(0f, 0f));
        assertThat(pose).isEqualTo(Pose.Identity);
    }

    @Test
    public void transformNormalizedCoordinatesToPose_topLeft_returnsCorrectPose() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        activityPanelEntity.setSize(new Dimensions(1f, 1f, 0f));
        Pose pose = activityPanelEntity.transformNormalizedCoordinatesToPose(new Vector2(-1f, 1f));
        Vector3 expected = new Vector3(-0.5f, 0.5f, 0.0f);
        assertThat(pose.getTranslation()).isEqualTo(expected);
    }

    @Test
    public void getParent_nullParent_returnsNull() {
        ActivityPanelEntity activityPanelEntity =
                mFakeRuntime.createActivityPanelEntity(
                        new Pose(), mWindowBoundsPx, "test", mHostActivity, /* parent= */ null);

        assertThat(activityPanelEntity.getParent()).isEqualTo(null);
    }

    @Test
    public void getPoseInParentSpace_nullParent_returnsIdentity() {
        ActivityPanelEntity activityPanelEntity =
                mFakeRuntime.createActivityPanelEntity(
                        new Pose(), mWindowBoundsPx, "test", mHostActivity, /* parent= */ null);

        activityPanelEntity.setPose(Pose.Identity);
        assertThat(activityPanelEntity.getPose(Space.PARENT)).isEqualTo(Pose.Identity);
    }

    @Test
    public void getPoseInActivitySpace_nullParent_throwsException() {
        ActivityPanelEntity activityPanelEntity =
                mFakeRuntime.createActivityPanelEntity(
                        new Pose(), mWindowBoundsPx, "test", mHostActivity, /* parent= */ null);

        assertThrows(
                IllegalStateException.class, () -> activityPanelEntity.getPose(Space.ACTIVITY));
    }

    @Test
    public void getPoseInRealWorldSpace_nullParent_throwsException() {
        ActivityPanelEntity activityPanelEntity =
                mFakeRuntime.createActivityPanelEntity(
                        new Pose(), mWindowBoundsPx, "test", mHostActivity, /* parent= */ null);

        assertThrows(
                IllegalStateException.class, () -> activityPanelEntity.getPose(Space.REAL_WORLD));
    }
}
