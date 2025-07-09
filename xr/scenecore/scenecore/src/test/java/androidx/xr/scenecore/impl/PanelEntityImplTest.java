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

import androidx.xr.runtime.internal.CameraViewActivityPose;
import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.PerceivedResolutionResult;
import androidx.xr.runtime.internal.PixelDimensions;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.truth.Truth;

import org.junit.After;
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
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mMakeFakeExecutor =
            new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = mock(PerceptionLibrary.class);
    private final EntityManager mEntityManager = new EntityManager();
    private JxrPlatformAdapterAxr mTestPlatformAdapter;
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();

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
                        mFakeImpressApi,
                        mEntityManager,
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
        mEntityManager.clear();
    }

    private PanelEntityImpl createPanelEntity(Dimensions surfaceDimensionsPx) {
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
                                (int) surfaceDimensionsPx.width, (int) surfaceDimensionsPx.height),
                        "panel",
                        mMakeFakeExecutor);

        // TODO(b/352829122): introduce a TestRootEntity which can serve as a parent
        panelEntity.setParent(mTestPlatformAdapter.getActivitySpaceRootImpl());
        return panelEntity;
    }

    private CameraViewActivityPose setupDefaultMockCameraView() {
        CameraViewActivityPose cameraView = mock(CameraViewActivityPose.class);
        when(cameraView.getCameraType())
                .thenReturn(CameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE);
        // Camera at origin, looking along -Z
        when(cameraView.getActivitySpacePose())
                .thenReturn(new Pose(new Vector3(0f, 0f, 0f), Quaternion.Identity));

        // 90 deg HFOV, 90 deg VFOV (tan(half-angle) = 1)
        CameraViewActivityPose.Fov fov =
                new CameraViewActivityPose.Fov(
                        (float) Math.atan(1.0),
                        (float) Math.atan(1.0),
                        (float) Math.atan(1.0),
                        (float) Math.atan(1.0));
        when(cameraView.getFov()).thenReturn(fov);
        // Standard display resolution for calculations
        when(cameraView.getDisplayResolutionInPixels()).thenReturn(new PixelDimensions(1000, 1000));
        // Clear the EntityManager to ensure LeftEye is the only entity inside it
        mEntityManager.clear();
        mEntityManager.addSystemSpaceActivityPose(cameraView);
        return cameraView;
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

        assertThat(panelEntity.getSizeInPixels().width).isEqualTo(640);
        assertThat(panelEntity.getSizeInPixels().height).isEqualTo(480);
    }

    @Test
    public void createPanel_setsCornerRadius() {
        PanelEntityImpl panelEntity = createPanelEntity(kVgaResolutionPx);

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to 32dp.
        assertThat(panelEntity.getCornerRadius()).isEqualTo(32.0f);
        assertThat(mNodeRepository.getCornerRadius(panelEntity.getNode())).isEqualTo(32.0f);
    }

    @Test
    public void createPanel_smallPanelWidth_setsCornerRadiusToPanelSize() {
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(40f, 1000f, 0f));

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to 32dp.
        assertThat(panelEntity.getCornerRadius()).isEqualTo(20f);
        assertThat(mNodeRepository.getCornerRadius(panelEntity.getNode())).isEqualTo(20f);
    }

    @Test
    public void createPanel_smallPanelHeight_setsCornerRadiusToPanelSize() {
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(1000f, 40f, 0f));

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to 32dp.
        assertThat(panelEntity.getCornerRadius()).isEqualTo(20f);
        assertThat(mNodeRepository.getCornerRadius(panelEntity.getNode())).isEqualTo(20f);
    }

    @Test
    public void getPerceivedResolution_noCameraView_returnsInvalidCameraView() {
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(2f, 1f, 0f));
        // Ensure mEntityManager is empty or has no left-eye camera
        mEntityManager.clear(); // Make sure no camera views are present

        PerceivedResolutionResult result = panelEntity.getPerceivedResolution();

        assertThat(result).isInstanceOf(PerceivedResolutionResult.InvalidCameraView.class);
    }

    @Test
    public void getPerceivedResolution_validCameraAndPanelInFront_returnsSuccess() {
        // Panel created with PixelDimensions(2,1). With pixel density 1.0, size is 2m x 1m.
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(2f, 1f, 0f));
        setupDefaultMockCameraView();

        // Place panel 2m in front of camera. Camera is at (0,0,0). Panel at (0,0,-2).
        // Panel is parented to ActivitySpaceRoot (identity pose and scale by default).
        // Panel's local pose becomes its activity space pose.
        // Panel's local scale is (1,1,1) by default from FakeXrExtensions.
        // So, panelEntity.getScale(Space.ACTIVITY) should be (1,1,1).
        panelEntity.setPose(new Pose(new Vector3(0f, 0f, -2f), Quaternion.Identity));

        PerceivedResolutionResult result = panelEntity.getPerceivedResolution();

        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success.class);
        PerceivedResolutionResult.Success successResult =
                (PerceivedResolutionResult.Success) result;

        // Expected calculation:
        // Panel size: 2m width, 1m height (since pixel density is 1.0)
        // Panel scale in activity space: (1,1,1)
        // Effective panel size in activity space: 2m x 1m
        // Panel distance: 2m
        // Camera FOV: 90deg H & V. Display: 1000x1000px.
        // View plane at 2m distance: width = 2 * (tan(45) + tan(45)) = 2 * (1+1) = 4m
        //                             height = 2 * (tan(45) + tan(45)) = 2 * (1+1) = 4m
        // Panel width ratio in view plane = 2m / 4m = 0.5
        // Panel height ratio in view plane = 1m / 4m = 0.25
        // Perceived pixel width = 0.5 * 1000px = 500
        // Perceived pixel height = 0.25 * 1000px = 250
        Truth.assertThat(successResult.getPerceivedResolution().width).isEqualTo(500);
        Truth.assertThat(successResult.getPerceivedResolution().height).isEqualTo(250);
    }

    @Test
    public void getPerceivedResolution_panelTooClose_returnsEntityTooClose() {
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(2f, 1f, 0f));
        setupDefaultMockCameraView();

        // Place panel very close to the camera (distance < EPSILON)
        float veryCloseDistance = PerceivedResolutionUtils.PERCEIVED_RESOLUTION_EPSILON / 2f;
        panelEntity.setPose(new Pose(new Vector3(0f, 0f, -veryCloseDistance), Quaternion.Identity));

        PerceivedResolutionResult result = panelEntity.getPerceivedResolution();

        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose.class);
    }

    @Test
    public void getPerceivedResolution_panelAtEpsilonDistance_returnsEntityTooClose() {
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(2f, 1f, 0f));
        setupDefaultMockCameraView();

        // Place panel exactly at EPSILON distance
        panelEntity.setPose(
                new Pose(
                        new Vector3(0f, 0f, -PerceivedResolutionUtils.PERCEIVED_RESOLUTION_EPSILON),
                        Quaternion.Identity));

        PerceivedResolutionResult result = panelEntity.getPerceivedResolution();

        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose.class);
    }

    @Test
    public void getPerceivedResolution_panelWithScale_calculatesCorrectly() {
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(1f, 1f, 0f)); // 1m x 1m
        // local size
        setupDefaultMockCameraView();

        panelEntity.setPose(new Pose(new Vector3(0f, 0f, -2f), Quaternion.Identity));
        panelEntity.setScale(new Vector3(2f, 3f, 1f)); // Scale the panel

        PerceivedResolutionResult result = panelEntity.getPerceivedResolution();

        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success.class);
        PerceivedResolutionResult.Success successResult =
                (PerceivedResolutionResult.Success) result;

        // Expected calculation:
        // Local panel size: 1m width, 1m height
        // Panel scale in activity space: (2,3,1)
        // Effective panel size in activity space: 1m*2f = 2m width, 1m*3f = 3m height
        // Panel distance: 2m
        // View plane at 2m distance: 4m x 4m
        // Panel width ratio in view plane = 2m / 4m = 0.5
        // Panel height ratio in view plane = 3m / 4m = 0.75
        // Perceived pixel width = 0.5 * 1000px = 500
        // Perceived pixel height = 0.75 * 1000px = 750
        Truth.assertThat(successResult.getPerceivedResolution().width).isEqualTo(500);
        Truth.assertThat(successResult.getPerceivedResolution().height).isEqualTo(750);
    }
}
