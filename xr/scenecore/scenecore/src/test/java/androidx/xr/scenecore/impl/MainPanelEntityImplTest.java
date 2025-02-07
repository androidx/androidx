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

import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeNode;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class MainPanelEntityImplTest {
    private final FakeXrExtensions mFakeExtensions = new FakeXrExtensions();
    private final FakeImpressApi mFakeImpressApi = new FakeImpressApi();
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mHostActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    ImpSplitEngineRenderer mSplitEngineRenderer = Mockito.mock(ImpSplitEngineRenderer.class);
    private JxrPlatformAdapterAxr mTestRuntime;
    private MainPanelEntityImpl mMainPanelEntity;

    @Before
    public void setUp() {
        when(mPerceptionLibrary.initSession(eq(mHostActivity), anyInt(), eq(mFakeExecutor)))
                .thenReturn(immediateFuture(Mockito.mock(Session.class)));

        mTestRuntime =
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

        mMainPanelEntity = (MainPanelEntityImpl) mTestRuntime.getMainPanelEntity();
    }

    @Test
    public void runtimeGetMainPanelEntity_returnsPanelEntityImpl() {
        assertThat(mMainPanelEntity).isNotNull();
    }

    @Test
    public void mainPanelEntitySetPixelDimensions_callsExtensions() {
        PixelDimensions kTestPixelDimensions = new PixelDimensions(14, 14);
        mMainPanelEntity.setPixelDimensions(kTestPixelDimensions);
        assertThat(mFakeExtensions.getMainWindowWidth()).isEqualTo(kTestPixelDimensions.width);
        assertThat(mFakeExtensions.getMainWindowHeight()).isEqualTo(kTestPixelDimensions.height);
    }

    @Test
    public void mainPanelEntitySetSize_callsExtensions() {
        // TODO(b/352630025): remove this once setSize is removed.
        // This should have the same effect as setPixelDimensions, except that it has to convert
        // from Dimensions to PixelDimensions, so it casts float to int.
        Dimensions kTestDimensions = new Dimensions(123.0f, 123.0f, 123.0f);
        mMainPanelEntity.setSize(kTestDimensions);
        assertThat(mFakeExtensions.getMainWindowWidth()).isEqualTo((int) kTestDimensions.width);
        assertThat(mFakeExtensions.getMainWindowWidth()).isEqualTo((int) kTestDimensions.height);
    }

    @Test
    public void createActivityPanelEntity_setsCornersTo32Dp() {
        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter. Validate that the
        // corner radius is set to 32dp.
        assertThat(mMainPanelEntity.getCornerRadius()).isEqualTo(32.0f);
        FakeNode fakeNode = (FakeNode) mMainPanelEntity.getNode();
        assertThat(fakeNode.getCornerRadius()).isEqualTo(32.0f);
    }
}
