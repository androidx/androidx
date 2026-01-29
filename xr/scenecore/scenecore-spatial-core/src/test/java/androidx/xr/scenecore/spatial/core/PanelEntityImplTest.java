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
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.xr.runtime.FieldOfView;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector2;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.PerceivedResolutionResult;
import androidx.xr.scenecore.runtime.PixelDimensions;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeScenePose;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;

import com.google.common.truth.Truth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public class PanelEntityImplTest {
    private static final Dimensions K_VGA_RESOLUTION_PX = new Dimensions(640f, 480f, 0f);
    private static final Dimensions K_HD_RESOLUTION_PX = new Dimensions(1280f, 720f, 0f);
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mMakeFakeExecutor =
            new FakeScheduledExecutorService();
    private final EntityManager mEntityManager = new EntityManager();
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();
    private final PixelDimensions mViewPlaneResolution = new PixelDimensions(2000, 1000);
    private SpatialSceneRuntime mRuntime;
    private FakeScenePose mRenderViewScenePose;
    private FieldOfView mRenderViewFov;

    @Before
    public void setUp() {
        String widthAndHeightConfig =
                "+w" + mViewPlaneResolution.width + "dp-h" + mViewPlaneResolution.height + "dp";
        RuntimeEnvironment.setQualifiers(widthAndHeightConfig);
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(1);
        mRuntime =
                SpatialSceneRuntime.create(
                        mActivity,
                        mMakeFakeExecutor,
                        mXrExtensions,
                        mEntityManager,
                        /* unscaledGravityAlignedActivitySpace= */ false);
        mRenderViewScenePose = new FakeScenePose();
        mRenderViewScenePose.setActivitySpacePose(
                new Pose(new Vector3(0f, 0f, 0f), Quaternion.Identity));
        mRenderViewFov =
                new FieldOfView(
                        (float) Math.atan(1.0),
                        (float) Math.atan(1.0),
                        (float) Math.atan(1.0),
                        (float) Math.atan(1.0));
    }

    @After
    public void tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        mRuntime.destroy();
        mEntityManager.clear();
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(null);
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
        panelEntity.setParent(mRuntime.getActivitySpace());
        return panelEntity;
    }

    @Test
    public void getSizeForPanelEntity_returnsSizeInMeters() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX);

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        assertThat(panelEntity.getSize().width).isEqualTo(640f);
        assertThat(panelEntity.getSize().height).isEqualTo(480f);
        assertThat(panelEntity.getSize().depth).isEqualTo(0f);
    }

    @Test
    public void setSizeForPanelEntity_setsSize() {
        PanelEntityImpl panelEntity = createPanelEntity(K_HD_RESOLUTION_PX);

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        assertThat(panelEntity.getSize().width).isEqualTo(1280f);
        assertThat(panelEntity.getSize().height).isEqualTo(720f);
        assertThat(panelEntity.getSize().depth).isEqualTo(0f);

        panelEntity.setSize(K_VGA_RESOLUTION_PX);

        assertThat(panelEntity.getSize().width).isEqualTo(640f);
        assertThat(panelEntity.getSize().height).isEqualTo(480f);
        assertThat(panelEntity.getSize().depth).isEqualTo(0f);
    }

    @Test
    public void setSizeForPanelEntity_updatesPixelDimensions() {
        PanelEntityImpl panelEntity = createPanelEntity(K_HD_RESOLUTION_PX);

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        assertThat(panelEntity.getSize().width).isEqualTo(1280f);
        assertThat(panelEntity.getSize().height).isEqualTo(720f);
        assertThat(panelEntity.getSize().depth).isEqualTo(0f);

        panelEntity.setSize(K_VGA_RESOLUTION_PX);

        assertThat(panelEntity.getSize().width).isEqualTo(640f);
        assertThat(panelEntity.getSize().height).isEqualTo(480f);
        assertThat(panelEntity.getSize().depth).isEqualTo(0f);
        assertThat(panelEntity.getSizeInPixels().width).isEqualTo(640);
        assertThat(panelEntity.getSizeInPixels().height).isEqualTo(480);
    }

    @Test
    public void createPanel_setsCornerRadius() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX);

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
    public void getPerceivedResolution_validCameraAndPanelInFront_returnsSuccess() {
        // Panel created with PixelDimensions(2,1). With pixel density 1.0, size is 2m x 1m.
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(2f, 1f, 0f));

        // Place panel 2m in front of camera. Camera is at (0,0,0). Panel at (0,0,-2).
        // Panel is parented to ActivitySpaceRoot (identity pose and scale by default).
        // Panel's local pose becomes its activity space pose.
        // Panel's local scale is (1,1,1) by default from FakeXrExtensions.
        // So, panelEntity.getScale(Space.ACTIVITY) should be (1,1,1).
        panelEntity.setPose(new Pose(new Vector3(0f, 0f, -2f), Quaternion.Identity));

        PerceivedResolutionResult result =
                panelEntity.getPerceivedResolution(mRenderViewScenePose, mRenderViewFov);

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

        // Place panel very close to the camera (distance < EPSILON)
        float veryCloseDistance = PerceivedResolutionUtils.PERCEIVED_RESOLUTION_EPSILON / 2f;
        panelEntity.setPose(new Pose(new Vector3(0f, 0f, -veryCloseDistance), Quaternion.Identity));

        PerceivedResolutionResult result =
                panelEntity.getPerceivedResolution(mRenderViewScenePose, mRenderViewFov);

        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose.class);
    }

    @Test
    public void getPerceivedResolution_panelAtEpsilonDistance_returnsEntityTooClose() {
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(2f, 1f, 0f));

        // Place panel exactly at EPSILON distance
        panelEntity.setPose(
                new Pose(
                        new Vector3(0f, 0f, -PerceivedResolutionUtils.PERCEIVED_RESOLUTION_EPSILON),
                        Quaternion.Identity));

        PerceivedResolutionResult result =
                panelEntity.getPerceivedResolution(mRenderViewScenePose, mRenderViewFov);

        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose.class);
    }

    @Test
    public void getPerceivedResolution_panelWithScale_calculatesCorrectly() {
        PanelEntityImpl panelEntity = createPanelEntity(new Dimensions(1f, 1f, 0f)); // 1m x 1m
        // local size

        panelEntity.setPose(new Pose(new Vector3(0f, 0f, -2f), Quaternion.Identity));
        panelEntity.setScale(new Vector3(2f, 3f, 1f)); // Scale the panel

        PerceivedResolutionResult result =
                panelEntity.getPerceivedResolution(mRenderViewScenePose, mRenderViewFov);

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

    @Test
    public void transformPixelCoordinatesToPose_center_returnsIdentity() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX); // 640px x 480px
        Pose pose = panelEntity.transformPixelCoordinatesToPose(new Vector2(320f, 240f));
        assertThat(pose).isEqualTo(Pose.Identity);
    }

    @Test
    public void transformPixelCoordinatesToPose_topLeft_returnsCorrectPose() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX); // 640px x 480px
        Pose pose = panelEntity.transformPixelCoordinatesToPose(new Vector2(0f, 0f));
        Vector3 expected =
                new Vector3(
                        panelEntity.getSize().width * -0.5f,
                        panelEntity.getSize().height * 0.5f,
                        0.0f);
        assertThat(pose.getTranslation()).isEqualTo(expected);
    }

    @Test
    public void transformPixelCoordinatesToPose_bottomRight_returnsCorrectPose() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX); // 640px x 480px
        Pose pose = panelEntity.transformPixelCoordinatesToPose(new Vector2(640f, 480f));
        Vector3 expected =
                new Vector3(
                        panelEntity.getSize().width * 0.5f,
                        panelEntity.getSize().height * -0.5f,
                        0.0f);
        assertThat(pose.getTranslation()).isEqualTo(expected);
    }

    @Test
    public void transformNormalizedCoordinatesToPose_center_returnsIdentity() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX);
        Pose pose = panelEntity.transformNormalizedCoordinatesToPose(new Vector2(0f, 0f));
        assertThat(pose).isEqualTo(Pose.Identity);
    }

    @Test
    public void transformNormalizedCoordinatesToPose_topLeft_returnsCorrectPose() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX);
        float width = 10.0f;
        float height = 20.0f;
        panelEntity.setSize(new Dimensions(width, height, 0.0f));
        Pose pose = panelEntity.transformNormalizedCoordinatesToPose(new Vector2(-1f, 1f));
        Vector3 expected = new Vector3(-width / 2, height / 2, 0.0f);
        assertThat(pose.getTranslation()).isEqualTo(expected);
    }

    @Test
    public void transformNormalizedCoordinatesToPose_bottomRight_returnsCorrectPose() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX);
        float width = 10.0f;
        float height = 20.0f;
        panelEntity.setSize(new Dimensions(width, height, 0.0f));
        Pose pose = panelEntity.transformNormalizedCoordinatesToPose(new Vector2(1f, -1f));
        Vector3 expected = new Vector3(width / 2, -height / 2, 0.0f);
        assertThat(pose.getTranslation()).isEqualTo(expected);
    }

    @Test
    public void getParent_nullParent_returnsNull() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX);
        panelEntity.setParent(null);
        assertThat(panelEntity.getParent()).isEqualTo(null);
    }

    @Test
    public void getPoseInParentSpace_nullParent_returnsIdentity() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX);
        panelEntity.setParent(null);
        panelEntity.setPose(Pose.Identity);
        assertThat(panelEntity.getPose(Space.PARENT)).isEqualTo(Pose.Identity);
    }

    @Test
    public void getPoseInActivitySpace_nullParent_throwsException() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX);
        panelEntity.setParent(null);
        assertThrows(IllegalStateException.class, () -> panelEntity.getPose(Space.ACTIVITY));
    }

    @Test
    public void getPoseInRealWorldSpace_nullParent_throwsException() {
        PanelEntityImpl panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX);
        panelEntity.setParent(null);
        assertThrows(IllegalStateException.class, () -> panelEntity.getPose(Space.REAL_WORLD));
    }
}
