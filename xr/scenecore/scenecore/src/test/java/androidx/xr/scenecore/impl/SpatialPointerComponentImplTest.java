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

import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.PixelDimensions;
import androidx.xr.runtime.internal.SpatialPointerIcon;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.apibindings.FakeImpressApiImpl;
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
public final class SpatialPointerComponentImplTest {

    private static final Dimensions sVgaResolutionPx = new Dimensions(640f, 480f, 0f);
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
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
                        mXrExtensions,
                        new FakeImpressApiImpl(),
                        new EntityManager(),
                        mPerceptionLibrary,
                        mSplitEngineSubspaceManager,
                        mSplitEngineRenderer,
                        /* useSplitEngine= */ false,
                        /* unscaledGravityAlignedActivitySpace= */ false);
    }

    @After
    public void tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        mTestPlatformAdapter.dispose();
    }

    private PanelEntityImpl createTestPanelEntity() {
        Display display = mActivity.getSystemService(DisplayManager.class).getDisplays()[0];
        Context displayContext = mActivity.createDisplayContext(display);
        View view = new View(displayContext);
        view.setLayoutParams(new LayoutParams(640, 480));
        Node node = mXrExtensions.createNode();

        PanelEntityImpl panelEntity =
                new PanelEntityImpl(
                        displayContext,
                        node,
                        view,
                        mXrExtensions,
                        mEntityManager,
                        new PixelDimensions(
                                (int) sVgaResolutionPx.width, (int) sVgaResolutionPx.height),
                        "panel",
                        mMakeFakeExecutor);

        panelEntity.setParent(mTestPlatformAdapter.getActivitySpaceRootImpl());
        return panelEntity;
    }

    @Test
    public void addComponentToTwoEntity_fails() {
        PanelEntityImpl entity1 = createTestPanelEntity();
        PanelEntityImpl entity2 = createTestPanelEntity();
        SpatialPointerComponentImpl component = new SpatialPointerComponentImpl(mXrExtensions);
        assertThat(component).isNotNull();
        assertThat(entity1.addComponent(component)).isTrue();
        assertThat(entity2.addComponent(component)).isFalse();
    }

    @Test
    public void onAttach_setsSpatialPointerIconToDefault() {
        PanelEntityImpl entity = createTestPanelEntity();
        SpatialPointerComponentImpl component = new SpatialPointerComponentImpl(mXrExtensions);
        assertThat(component.onAttach(entity)).isTrue();
        assertThat(component.getSpatialPointerIcon()).isEqualTo(SpatialPointerIcon.TYPE_DEFAULT);
    }

    @Test
    public void onDetach_setsSpatialPointerIconToDefault() {
        PanelEntityImpl entity = createTestPanelEntity();
        SpatialPointerComponentImpl component = new SpatialPointerComponentImpl(mXrExtensions);
        assertThat(component.onAttach(entity)).isTrue();
        component.setSpatialPointerIcon(SpatialPointerIcon.TYPE_NONE);
        component.onDetach(entity);
        assertThat(component.getSpatialPointerIcon()).isEqualTo(SpatialPointerIcon.TYPE_DEFAULT);
    }

    @Test
    public void setSpatialPointerIcon_setsSpatialPointerIcon() {
        PanelEntityImpl entity = createTestPanelEntity();
        SpatialPointerComponentImpl component = new SpatialPointerComponentImpl(mXrExtensions);
        assertThat(component.onAttach(entity)).isTrue();
        component.setSpatialPointerIcon(SpatialPointerIcon.TYPE_NONE);
        assertThat(component.getSpatialPointerIcon()).isEqualTo(SpatialPointerIcon.TYPE_NONE);
        component.setSpatialPointerIcon(SpatialPointerIcon.TYPE_CIRCLE);
        assertThat(component.getSpatialPointerIcon()).isEqualTo(SpatialPointerIcon.TYPE_CIRCLE);
        component.setSpatialPointerIcon(SpatialPointerIcon.TYPE_DEFAULT);
        assertThat(component.getSpatialPointerIcon()).isEqualTo(SpatialPointerIcon.TYPE_DEFAULT);
    }
}
