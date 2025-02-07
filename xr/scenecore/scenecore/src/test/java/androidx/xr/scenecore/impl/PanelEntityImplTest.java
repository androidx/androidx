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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.xr.extensions.node.Node;
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
public class PanelEntityImplTest {
    private static final Dimensions kVgaResolutionPx = new Dimensions(640f, 480f, 0f);
    private static final Dimensions kHdResolutionPx = new Dimensions(1280f, 720f, 0f);
    private final FakeXrExtensions mFakeExtensions = new FakeXrExtensions();
    FakeImpressApi mFakeImpressApi = new FakeImpressApi();
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mMakeFakeExecutor =
            new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = mock(PerceptionLibrary.class);
    private final EntityManager mEntityManager = new EntityManager();
    private JxrPlatformAdapterAxr mTestPlatformAdapter;

    SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    ImpSplitEngineRenderer mSplitEngineRenderer = Mockito.mock(ImpSplitEngineRenderer.class);

    @Before
    public void setUp() {
        when(mPerceptionLibrary.initSession(eq(mActivity), anyInt(), eq(mMakeFakeExecutor)))
                .thenReturn(immediateFuture(Mockito.mock(Session.class)));

        mTestPlatformAdapter =
                JxrPlatformAdapterAxr.create(
                        mActivity,
                        mMakeFakeExecutor,
                        mFakeExtensions,
                        mFakeImpressApi,
                        new EntityManager(),
                        mPerceptionLibrary,
                        mSplitEngineSubspaceManager,
                        mSplitEngineRenderer,
                        /* useSplitEngine= */ false);
    }

    private PanelEntityImpl createPanelEntity(Dimensions surfaceDimensionsPx) {
        Display display = mActivity.getSystemService(DisplayManager.class).getDisplays()[0];
        Context displayContext = mActivity.createDisplayContext(display);
        View view = new View(displayContext);
        view.setLayoutParams(new LayoutParams(640, 480));
        Node node = mFakeExtensions.createNode();

        PanelEntityImpl panelEntity =
                new PanelEntityImpl(
                        node,
                        view,
                        mFakeExtensions,
                        mEntityManager,
                        new PixelDimensions(
                                (int) surfaceDimensionsPx.width, (int) surfaceDimensionsPx.height),
                        "panel",
                        displayContext,
                        mMakeFakeExecutor);

        // TODO(b/352829122): introduce a TestRootEntity which can serve as a parent
        panelEntity.setParent(mTestPlatformAdapter.getActivitySpaceRootImpl());
        return panelEntity;
    }

    @Test
    public void getSizeForPanelEntity_returnsSizeInMeters() {
        PanelEntityImpl panelEntity = createPanelEntity(kVgaResolutionPx);

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        assertThat(panelEntity.getSize().width).isEqualTo(640f);
        assertThat(panelEntity.getSize().height).isEqualTo(480f);
        assertThat(panelEntity.getSize().depth).isEqualTo(0f);
    }

    @Test
    public void setSizeForPanelEntity_setsSize() {
        PanelEntityImpl panelEntity = createPanelEntity(kHdResolutionPx);

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        assertThat(panelEntity.getSize().width).isEqualTo(1280f);
        assertThat(panelEntity.getSize().height).isEqualTo(720f);
        assertThat(panelEntity.getSize().depth).isEqualTo(0f);

        panelEntity.setSize(kVgaResolutionPx);

        assertThat(panelEntity.getSize().width).isEqualTo(640f);
        assertThat(panelEntity.getSize().height).isEqualTo(480f);
        assertThat(panelEntity.getSize().depth).isEqualTo(0f);
    }

    @Test
    public void setSizeForPanelEntity_updatesPixelDimensions() {
        PanelEntityImpl panelEntity = createPanelEntity(kHdResolutionPx);

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        assertThat(panelEntity.getSize().width).isEqualTo(1280f);
        assertThat(panelEntity.getSize().height).isEqualTo(720f);
        assertThat(panelEntity.getSize().depth).isEqualTo(0f);

        panelEntity.setSize(kVgaResolutionPx);

        assertThat(panelEntity.getSize().width).isEqualTo(640f);
        assertThat(panelEntity.getSize().height).isEqualTo(480f);
        assertThat(panelEntity.getSize().depth).isEqualTo(0f);

        assertThat(panelEntity.getPixelDimensions().width).isEqualTo(640);
        assertThat(panelEntity.getPixelDimensions().height).isEqualTo(480);
    }

    @Test
    public void createPanel_setsCornerRadius() {
        PanelEntityImpl panelEntity = createPanelEntity(kVgaResolutionPx);

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to 32dp.
        assertThat(panelEntity.getCornerRadius()).isEqualTo(32.0f);
        FakeNode fakeNode = (FakeNode) panelEntity.getNode();
        assertThat(fakeNode.getCornerRadius()).isEqualTo(32.0f);
    }

    @Test
    public void createPanel_smallPanelWidth_setsCornerRadiusToPanelSize() {
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(40f, 1000f, 0f));

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to 32dp.
        assertThat(panelEntity.getCornerRadius()).isEqualTo(20f);
        FakeNode fakeNode = (FakeNode) panelEntity.getNode();
        assertThat(fakeNode.getCornerRadius()).isEqualTo(20f);
    }

    @Test
    public void createPanel_smallPanelHeight_setsCornerRadiusToPanelSize() {
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(1000f, 40f, 0f));

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to 32dp.
        assertThat(panelEntity.getCornerRadius()).isEqualTo(20f);
        FakeNode fakeNode = (FakeNode) panelEntity.getNode();
        assertThat(fakeNode.getCornerRadius()).isEqualTo(20f);
    }
}
