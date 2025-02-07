/*
 * Copyright 2024 The Android Open Source Project
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
import static com.google.common.util.concurrent.Futures.immediateFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;

import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.JxrPlatformAdapter;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivityPanelEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeActivityPanel;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeNode;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class ActivityPanelEntityImplTest {
    private final FakeXrExtensions mFakeExtensions = new FakeXrExtensions();
    private final FakeImpressApi mFakeImpressApi = new FakeImpressApi();
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mHostActivity = mActivityController.create().start().get();
    private final PixelDimensions mWindowBoundsPx = new PixelDimensions(640, 480);
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private final ImpSplitEngineRenderer mSplitEngineRenderer =
            Mockito.mock(ImpSplitEngineRenderer.class);

    private ActivityPanelEntity createActivityPanelEntity() {
        return createActivityPanelEntity(mWindowBoundsPx);
    }

    private ActivityPanelEntity createActivityPanelEntity(PixelDimensions windowBoundsPx) {
        when(mPerceptionLibrary.initSession(eq(mHostActivity), anyInt(), eq(mFakeExecutor)))
                .thenReturn(immediateFuture(Mockito.mock(Session.class)));

        JxrPlatformAdapter fakeRuntime =
                JxrPlatformAdapterAxr.create(
                        mHostActivity,
                        mFakeExecutor,
                        mFakeExtensions,
                        mFakeImpressApi,
                        new EntityManager(),
                        mPerceptionLibrary,
                        mSplitEngineSubspaceManager,
                        mSplitEngineRenderer,
                        /* useSplitEngine= */ false);
        Pose mPose = new Pose();

        return fakeRuntime.createActivityPanelEntity(
                mPose,
                windowBoundsPx,
                "test",
                mHostActivity,
                fakeRuntime.getActivitySpaceRootImpl());
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
        FakeNode fakeNode = (FakeNode) ((ActivityPanelEntityImpl) activityPanelEntity).getNode();
        assertThat(fakeNode.getCornerRadius()).isEqualTo(32.0f);
    }

    @Test
    public void createPanel_smallPanelWidth_setsCornerRadiusToPanelSize() {
        ActivityPanelEntity activityPanelEntity =
                createActivityPanelEntity(new PixelDimensions(40, 1000));

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to half the width.
        assertThat(activityPanelEntity.getCornerRadius()).isEqualTo(20f);
        FakeNode fakeNode = (FakeNode) ((ActivityPanelEntityImpl) activityPanelEntity).getNode();
        assertThat(fakeNode.getCornerRadius()).isEqualTo(20f);
    }

    @Test
    public void createPanel_smallPanelHeight_setsCornerRadiusToPanelSize() {
        ActivityPanelEntity activityPanelEntity =
                createActivityPanelEntity(new PixelDimensions(1000, 40));

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to half the height.
        assertThat(activityPanelEntity.getCornerRadius()).isEqualTo(20f);
        FakeNode fakeNode = (FakeNode) ((ActivityPanelEntityImpl) activityPanelEntity).getNode();
        assertThat(fakeNode.getCornerRadius()).isEqualTo(20f);
    }

    @Test
    public void activityPanelEntityLaunchActivity_callsActivityPanel() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        Intent launchIntent = mActivityController.getIntent();
        activityPanelEntity.launchActivity(launchIntent, null);

        FakeActivityPanel fakePanel = mFakeExtensions.getActivityPanelForHost(mHostActivity);

        assertThat(fakePanel.getLaunchIntent()).isEqualTo(launchIntent);
        assertThat(fakePanel.getBundle()).isNull();
        assertThat(fakePanel.getBounds())
                .isEqualTo(new Rect(0, 0, mWindowBoundsPx.width, mWindowBoundsPx.height));
    }

    @Test
    public void activityPanelEntityMoveActivity_callActivityPanel() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        activityPanelEntity.moveActivity(mHostActivity);

        FakeActivityPanel fakePanel = mFakeExtensions.getActivityPanelForHost(mHostActivity);

        assertThat(fakePanel.getActivity()).isEqualTo(mHostActivity);

        assertThat(fakePanel.getBounds())
                .isEqualTo(new Rect(0, 0, mWindowBoundsPx.width, mWindowBoundsPx.height));
    }

    @Test
    public void activityPanelEntitySetSize_callsSetPixelDimensions() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        Dimensions dimensions = new Dimensions(400f, 300f, 0f);
        activityPanelEntity.setSize(dimensions);

        FakeActivityPanel fakePanel = mFakeExtensions.getActivityPanelForHost(mHostActivity);

        assertThat(fakePanel.getBounds())
                .isEqualTo(new Rect(0, 0, (int) dimensions.width, (int) dimensions.height));

        // SetSize redirects to setPixelDimensions, so we check the same thing here.
        PixelDimensions viewDimensions = activityPanelEntity.getPixelDimensions();
        assertThat(viewDimensions.width).isEqualTo((int) dimensions.width);
        assertThat(viewDimensions.height).isEqualTo((int) dimensions.height);
    }

    @Test
    public void activityPanelEntitySetPixelDimensions_callActivityPanel() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        PixelDimensions dimensions = new PixelDimensions(400, 300);
        activityPanelEntity.setPixelDimensions(dimensions);

        FakeActivityPanel fakePanel = mFakeExtensions.getActivityPanelForHost(mHostActivity);

        assertThat(fakePanel.getBounds())
                .isEqualTo(new Rect(0, 0, dimensions.width, dimensions.height));

        PixelDimensions viewDimensions = activityPanelEntity.getPixelDimensions();
        assertThat(viewDimensions.width).isEqualTo(dimensions.width);
        assertThat(viewDimensions.height).isEqualTo(dimensions.height);
    }

    @Test
    public void activityPanelEntityDispose_callsActivityPanelDelete() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        activityPanelEntity.dispose();

        FakeActivityPanel fakePanel = mFakeExtensions.getActivityPanelForHost(mHostActivity);

        assertThat(fakePanel.isDeleted()).isTrue();
    }
}
