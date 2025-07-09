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

package androidx.xr.scenecore.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.runtime.internal.CameraViewActivityPose;
import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.PerceivedResolutionResult;
import androidx.xr.runtime.internal.PixelDimensions;
import androidx.xr.runtime.internal.Space;
import androidx.xr.runtime.internal.SurfaceEntity;
import androidx.xr.runtime.internal.SurfaceEntity.CanvasShape;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.space.ShadowSpatialState;
import com.android.extensions.xr.space.SpatialState;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;
import com.google.common.truth.Truth;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.function.Supplier;

@RunWith(RobolectricTestRunner.class)
public final class SurfaceEntityImplTest {
    private static final int SUBSPACE_ID = 5;

    private SurfaceEntityImpl mSurfaceEntity;
    private EntityManager mEntityManager;
    // TODO: Update this test to handle Entity.dispose() for Exceptions when FakeImpress is
    //       updated.
    private FakeImpressApiImpl mImpressApi;

    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();

    @Before
    public void setUp() {
        mImpressApi = new FakeImpressApiImpl();
        createDefaultSurfaceEntity(new SurfaceEntity.CanvasShape.Quad(1f, 1f));
    }

    @After
    public void tearDown() {
        mEntityManager.clear();
        if (mSurfaceEntity != null) {
            mSurfaceEntity.dispose();
        }
    }

    private SurfaceEntityImpl createDefaultSurfaceEntity(CanvasShape canvasShape) {
        XrExtensions xrExtensions = XrExtensionsProvider.getXrExtensions();

        SplitEngineSubspaceManager splitEngineSubspaceManager =
                mock(SplitEngineSubspaceManager.class);

        Assert.assertNotNull(xrExtensions);
        Node node = xrExtensions.createNode();
        SubspaceNode expectedSubspaceNode = new SubspaceNode(SUBSPACE_ID, node);

        when(splitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
                .thenReturn(expectedSubspaceNode);

        int subspaceImpressNode = mImpressApi.createImpressNode();
        String subspaceName = "stereo_surface_panel_entity_subspace_" + subspaceImpressNode;
        SubspaceNode subspaceNode =
                splitEngineSubspaceManager.createSubspace(subspaceName, subspaceImpressNode);

        mEntityManager = new EntityManager();
        FakeScheduledExecutorService executor = new FakeScheduledExecutorService();
        Supplier<SpatialState> spatialStateProvider = ShadowSpatialState::create;
        Entity parentEntity =
                new ActivitySpaceImpl(
                        subspaceNode.getSubspaceNode(),
                        mActivity,
                        xrExtensions,
                        mEntityManager,
                        spatialStateProvider,
                        false,
                        executor);

        int stereoMode = SurfaceEntity.StereoMode.MONO;
        Pose pose = Pose.Identity;
        int contentSecurityLevel = 0;
        int useSuperSampling = 0;

        mSurfaceEntity =
                new SurfaceEntityImpl(
                        mActivity,
                        parentEntity,
                        mImpressApi,
                        splitEngineSubspaceManager,
                        xrExtensions,
                        mEntityManager,
                        executor,
                        stereoMode,
                        canvasShape,
                        contentSecurityLevel,
                        useSuperSampling);
        mSurfaceEntity.setPose(pose, Space.PARENT);

        return mSurfaceEntity;
    }

    private CameraViewActivityPose setupDefaultMockCameraView() {
        CameraViewActivityPose cameraView = mock(CameraViewActivityPose.class);
        when(cameraView.getCameraType())
                .thenReturn(CameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE);
        when(cameraView.getActivitySpacePose())
                .thenReturn(new Pose(new Vector3(0f, 0f, 0f), Quaternion.Identity));

        CameraViewActivityPose.Fov fov =
                new CameraViewActivityPose.Fov(
                        (float) Math.atan(1.0), (float) Math.atan(1.0),
                        (float) Math.atan(1.0), (float) Math.atan(1.0));
        when(cameraView.getFov()).thenReturn(fov);
        when(cameraView.getDisplayResolutionInPixels()).thenReturn(new PixelDimensions(1000, 1000));
        mEntityManager.clear();
        mEntityManager.addSystemSpaceActivityPose(cameraView);
        return cameraView;
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void setCanvasShape_setsCanvasShape() {
        SurfaceEntity.CanvasShape expectedCanvasShape =
                new SurfaceEntity.CanvasShape.Quad(12f, 12f);
        mSurfaceEntity.setCanvasShape(expectedCanvasShape);
        SurfaceEntity.CanvasShape canvasShape = mSurfaceEntity.getCanvasShape();

        assertThat(canvasShape.getClass()).isEqualTo(expectedCanvasShape.getClass());
        assertThat(canvasShape.getDimensions()).isEqualTo(expectedCanvasShape.getDimensions());

        expectedCanvasShape = new SurfaceEntity.CanvasShape.Vr360Sphere(11f);
        mSurfaceEntity.setCanvasShape(expectedCanvasShape);
        canvasShape = mSurfaceEntity.getCanvasShape();

        assertThat(canvasShape.getClass()).isEqualTo(expectedCanvasShape.getClass());
        assertThat(canvasShape.getDimensions()).isEqualTo(expectedCanvasShape.getDimensions());

        expectedCanvasShape = new SurfaceEntity.CanvasShape.Vr180Hemisphere(10f);
        mSurfaceEntity.setCanvasShape(expectedCanvasShape);
        canvasShape = mSurfaceEntity.getCanvasShape();

        assertThat(canvasShape.getClass()).isEqualTo(expectedCanvasShape.getClass());
        assertThat(canvasShape.getDimensions()).isEqualTo(expectedCanvasShape.getDimensions());
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void setStereoMode_setsStereoMode() {
        int expectedStereoMode = SurfaceEntity.StereoMode.MONO;
        mSurfaceEntity.setStereoMode(expectedStereoMode);
        int stereoMode = mSurfaceEntity.getStereoMode();

        assertThat(stereoMode).isEqualTo(expectedStereoMode);

        expectedStereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM;
        mSurfaceEntity.setStereoMode(expectedStereoMode);
        stereoMode = mSurfaceEntity.getStereoMode();

        assertThat(stereoMode).isEqualTo(expectedStereoMode);
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void setEdgeFeather_forwardsToImpress() {
        float kFeatherRadiusX = 0.14f;
        float kFeatherRadiusY = 0.28f;
        SurfaceEntity.EdgeFeather expectedFeather =
            new SurfaceEntity.EdgeFeather.SmoothFeather(kFeatherRadiusX, kFeatherRadiusY);
        mSurfaceEntity.setEdgeFeather(expectedFeather);
        SurfaceEntity.EdgeFeather returnedFeather = mSurfaceEntity.getEdgeFeather();
        assertThat(returnedFeather).isEqualTo(expectedFeather);

        // Apply Fake Impress checks
        FakeImpressApiImpl.StereoSurfaceEntityData surfaceEntityData =
            mImpressApi
                .getStereoSurfaceEntities().get(mSurfaceEntity.getEntityImpressNode());
        assertThat(surfaceEntityData.getFeatherRadiusX()).isEqualTo(kFeatherRadiusX);
        assertThat(surfaceEntityData.getFeatherRadiusY()).isEqualTo(kFeatherRadiusY);

        // Set back to SolidEdge to simulate turning feathering off
        expectedFeather = new SurfaceEntity.EdgeFeather.SolidEdge();
        mSurfaceEntity.setEdgeFeather(expectedFeather);
        returnedFeather = mSurfaceEntity.getEdgeFeather();
        assertThat(surfaceEntityData.getFeatherRadiusX()).isEqualTo(0.0f);
        assertThat(surfaceEntityData.getFeatherRadiusY()).isEqualTo(0.0f);
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void getPerceivedResolution_noCameraView_returnsInvalidCameraView() {
        mEntityManager.clear(); // Ensure no camera views
        PerceivedResolutionResult result = mSurfaceEntity.getPerceivedResolution();
        assertThat(result).isInstanceOf(PerceivedResolutionResult.InvalidCameraView.class);
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void getPerceivedResolution_quadInFront_returnsSuccess() {
        CanvasShape.Quad quadShape = new CanvasShape.Quad(2.0f, 1.0f); // 2m wide, 1m high
        // Recreate mSurfaceEntity with the specific shape for this test
        mSurfaceEntity = createDefaultSurfaceEntity(quadShape);
        setupDefaultMockCameraView();

        mSurfaceEntity.setPose(new Pose(new Vector3(0f, 0f, -2f), Quaternion.Identity)); // 2m away
        mSurfaceEntity.setScale(new Vector3(1f, 1f, 1f));

        PerceivedResolutionResult result = mSurfaceEntity.getPerceivedResolution();
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success.class);
        PerceivedResolutionResult.Success successResult =
                (PerceivedResolutionResult.Success) result;

        Truth.assertThat(successResult.getPerceivedResolution().width).isEqualTo(500);
        Truth.assertThat(successResult.getPerceivedResolution().height).isEqualTo(250);
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void getPerceivedResolution_sphereInFront_returnsSuccess() {
        CanvasShape.Vr360Sphere sphereShape = new CanvasShape.Vr360Sphere(1.0f); // radius 1m
        mSurfaceEntity = createDefaultSurfaceEntity(sphereShape);
        setupDefaultMockCameraView();

        mSurfaceEntity.setPose(new Pose(new Vector3(0f, 0f, -3f), Quaternion.Identity)); //
        // Center 3m away
        mSurfaceEntity.setScale(new Vector3(1f, 1f, 1f));

        PerceivedResolutionResult result = mSurfaceEntity.getPerceivedResolution();
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success.class);
        PerceivedResolutionResult.Success successResult =
                (PerceivedResolutionResult.Success) result;

        Truth.assertThat(successResult.getPerceivedResolution().width).isEqualTo(500);
        Truth.assertThat(successResult.getPerceivedResolution().height).isEqualTo(500);
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void getPerceivedResolution_quadTooClose_returnsEntityTooClose() {
        CanvasShape.Quad quadShape = new CanvasShape.Quad(2.0f, 1.0f);
        mSurfaceEntity = createDefaultSurfaceEntity(quadShape);
        setupDefaultMockCameraView();

        float veryCloseDistance = PerceivedResolutionUtils.PERCEIVED_RESOLUTION_EPSILON / 2f;
        mSurfaceEntity.setPose(
                new Pose(new Vector3(0f, 0f, -veryCloseDistance), Quaternion.Identity));
        mSurfaceEntity.setScale(new Vector3(1f, 1f, 1f));

        PerceivedResolutionResult result = mSurfaceEntity.getPerceivedResolution();
        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose.class);
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void getPerceivedResolution_quadWithScale_calculatesCorrectly() {
        CanvasShape.Quad quadShape = new CanvasShape.Quad(1.0f, 1.0f); // 1m x 1m local
        mSurfaceEntity = createDefaultSurfaceEntity(quadShape);
        setupDefaultMockCameraView();

        mSurfaceEntity.setPose(new Pose(new Vector3(0f, 0f, -2f), Quaternion.Identity)); // 2m away
        mSurfaceEntity.setScale(new Vector3(2f, 3f, 1f)); // Scaled to 2m wide, 3m high

        PerceivedResolutionResult result = mSurfaceEntity.getPerceivedResolution();
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success.class);
        PerceivedResolutionResult.Success successResult =
                (PerceivedResolutionResult.Success) result;

        // The width and height are flipped because perceievedResolution calculations will
        // always place the largest dimension as the width, and the second as height.
        Truth.assertThat(successResult.getPerceivedResolution().width).isEqualTo(750);
        Truth.assertThat(successResult.getPerceivedResolution().height).isEqualTo(500);
    }
}
