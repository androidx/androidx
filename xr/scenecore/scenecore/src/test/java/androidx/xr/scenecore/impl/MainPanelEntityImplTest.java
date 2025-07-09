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

import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.PixelDimensions;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.NodeRepository;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class MainPanelEntityImplTest {
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
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
                        mXrExtensions,
                        mFakeImpressApi,
                        new EntityManager(),
                        mPerceptionLibrary,
                        mSplitEngineSubspaceManager,
                        mSplitEngineRenderer,
                        /* useSplitEngine= */ false,
                        /* unscaledGravityAlignedActivitySpace= */ false);

        mMainPanelEntity = (MainPanelEntityImpl) mTestRuntime.getMainPanelEntity();
    }

    @After
    public void tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        mTestRuntime.dispose();
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
}
