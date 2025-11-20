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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.runtime.NodeHolder;
import androidx.xr.runtime.math.FloatSize2d;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.CameraViewScenePose;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.PerceivedResolutionResult;
import androidx.xr.scenecore.runtime.PixelDimensions;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.SurfaceEntity;
import androidx.xr.scenecore.runtime.SurfaceEntity.Shape;
import androidx.xr.scenecore.runtime.SurfaceFeature;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeSurfaceFeature;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.space.ShadowSpatialState;
import com.android.extensions.xr.space.SpatialState;

import com.google.common.truth.Truth;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.function.Supplier;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public final class SurfaceEntityImplTest {
    private SurfaceEntityImpl mSurfaceEntity;
    private EntityManager mEntityManager;

    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private FakeSurfaceFeature mFakeSurfaceFeature;
    private final SurfaceFeature mMockSurfaceFeature = Mockito.mock(SurfaceFeature.class);

    @Before
    public void setUp() {
        createDefaultSurfaceEntity(new Shape.Quad(new FloatSize2d(1f, 1f)));
    }

    @After
    public void tearDown() {
        mEntityManager.clear();
        if (mSurfaceEntity != null) {
            mSurfaceEntity.dispose();
        }
    }

    private SurfaceEntityImpl createDefaultSurfaceEntity(Shape shape) {
        XrExtensions xrExtensions = XrExtensionsProvider.getXrExtensions();

        Assert.assertNotNull(xrExtensions);

        NodeHolder<?> nodeHolder = new NodeHolder<>(xrExtensions.createNode(), Node.class);
        mFakeSurfaceFeature =
                (FakeSurfaceFeature)
                        FakeSurfaceFeature.Companion.createWithMockFeature(
                                mMockSurfaceFeature, nodeHolder);

        mEntityManager = new EntityManager();
        FakeScheduledExecutorService executor = new FakeScheduledExecutorService();
        Supplier<SpatialState> spatialStateProvider = ShadowSpatialState::create;
        Entity parentEntity =
                new ActivitySpaceImpl(
                        xrExtensions.createNode(),
                        mActivity,
                        xrExtensions,
                        mEntityManager,
                        spatialStateProvider,
                        false,
                        executor);

        int stereoMode = SurfaceEntity.StereoMode.MONO;
        Pose pose = Pose.Identity;

        mSurfaceEntity =
                new SurfaceEntityImpl(
                        mActivity,
                        mFakeSurfaceFeature,
                        parentEntity,
                        xrExtensions,
                        mEntityManager,
                        executor);
        mSurfaceEntity.setPose(pose, Space.PARENT);

        return mSurfaceEntity;
    }

    private CameraViewScenePose setupDefaultMockCameraView() {
        CameraViewScenePose cameraView = mock(CameraViewScenePose.class);
        when(cameraView.getCameraType())
                .thenReturn(CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE);
        when(cameraView.getActivitySpacePose())
                .thenReturn(new Pose(new Vector3(0f, 0f, 0f), Quaternion.Identity));

        CameraViewScenePose.Fov fov =
                new CameraViewScenePose.Fov(
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
    public void setShape_setsShape() {
        SurfaceEntity.Shape expectedShape = new SurfaceEntity.Shape.Quad(new FloatSize2d(12f, 12f));
        mSurfaceEntity.setShape(expectedShape);

        verify(mMockSurfaceFeature).setShape(expectedShape);

        when(mMockSurfaceFeature.getShape()).thenReturn(expectedShape);
        SurfaceEntity.Shape shape = mSurfaceEntity.getShape();

        assertThat(shape.getClass()).isEqualTo(expectedShape.getClass());
        assertThat(shape.getDimensions()).isEqualTo(expectedShape.getDimensions());

        expectedShape = new SurfaceEntity.Shape.Sphere(11f);

        mSurfaceEntity.setShape(expectedShape);

        verify(mMockSurfaceFeature).setShape(expectedShape);

        when(mMockSurfaceFeature.getShape()).thenReturn(expectedShape);
        shape = mSurfaceEntity.getShape();

        assertThat(shape.getClass()).isEqualTo(expectedShape.getClass());
        assertThat(shape.getDimensions()).isEqualTo(expectedShape.getDimensions());

        expectedShape = new SurfaceEntity.Shape.Hemisphere(10f);

        mSurfaceEntity.setShape(expectedShape);

        verify(mMockSurfaceFeature).setShape(expectedShape);

        when(mMockSurfaceFeature.getShape()).thenReturn(expectedShape);
        shape = mSurfaceEntity.getShape();

        assertThat(shape.getClass()).isEqualTo(expectedShape.getClass());
        assertThat(shape.getDimensions()).isEqualTo(expectedShape.getDimensions());
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void setStereoMode_setsStereoMode() {
        int expectedStereoMode = SurfaceEntity.StereoMode.MONO;
        mSurfaceEntity.setStereoMode(expectedStereoMode);

        verify(mMockSurfaceFeature).setStereoMode(expectedStereoMode);

        when(mMockSurfaceFeature.getStereoMode()).thenReturn(expectedStereoMode);
        int stereoMode = mSurfaceEntity.getStereoMode();

        assertThat(stereoMode).isEqualTo(expectedStereoMode);

        expectedStereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM;
        mSurfaceEntity.setStereoMode(expectedStereoMode);

        verify(mMockSurfaceFeature).setStereoMode(expectedStereoMode);

        when(mMockSurfaceFeature.getStereoMode()).thenReturn(expectedStereoMode);
        stereoMode = mSurfaceEntity.getStereoMode();

        assertThat(stereoMode).isEqualTo(expectedStereoMode);
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void dispose_supports_reentry() {
        Shape.Quad quadShape = new Shape.Quad(new FloatSize2d(1.0f, 1.0f)); // 1m x 1m local
        mSurfaceEntity = createDefaultSurfaceEntity(quadShape);

        // Note that we don't test that dispose prevents manipulating other properties because that
        // is enforced at the API level, rather than the implementation level.
        mSurfaceEntity.dispose();
        mSurfaceEntity.dispose(); // shouldn't crash
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void setEdgeFeather_forwardsToFeature() {
        float kFeatherRadiusX = 0.14f;
        float kFeatherRadiusY = 0.28f;
        SurfaceEntity.EdgeFeather expectedFeather =
                new SurfaceEntity.EdgeFeather.RectangleFeather(kFeatherRadiusX, kFeatherRadiusY);
        mSurfaceEntity.setEdgeFeather(expectedFeather);

        verify(mMockSurfaceFeature).setEdgeFeather(expectedFeather);

        when(mMockSurfaceFeature.getEdgeFeather()).thenReturn(expectedFeather);

        SurfaceEntity.EdgeFeather returnedFeather = mSurfaceEntity.getEdgeFeather();

        assertThat(returnedFeather).isEqualTo(expectedFeather);
    }

    @Test
    public void setColliderEnabled_forwardsToFeature() {
        mSurfaceEntity.setColliderEnabled(true);
        verify(mMockSurfaceFeature).setColliderEnabled(true);
        assertThat(mFakeSurfaceFeature.getColliderEnabled()).isTrue();

        mSurfaceEntity.setColliderEnabled(false);
        verify(mMockSurfaceFeature).setColliderEnabled(false);
        assertThat(mFakeSurfaceFeature.getColliderEnabled()).isFalse();
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
        Shape.Quad quadShape = new Shape.Quad(new FloatSize2d(2.0f, 1.0f)); // 2m wide, 1m high
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
        Shape.Sphere sphereShape = new Shape.Sphere(1.0f); // radius 1m
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
        Shape.Quad quadShape = new Shape.Quad(new FloatSize2d(2.0f, 1.0f));
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
        Shape.Quad quadShape = new Shape.Quad(new FloatSize2d(1.0f, 1.0f)); // 1m x 1m local
        mSurfaceEntity = createDefaultSurfaceEntity(quadShape);
        setupDefaultMockCameraView();

        mSurfaceEntity.setPose(new Pose(new Vector3(0f, 0f, -2f), Quaternion.Identity)); // 2m away
        mSurfaceEntity.setScale(new Vector3(2f, 3f, 1f)); // Scaled to 2m wide, 3m high

        PerceivedResolutionResult result = mSurfaceEntity.getPerceivedResolution();
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success.class);
        PerceivedResolutionResult.Success successResult =
                (PerceivedResolutionResult.Success) result;

        // The width and height are flipped because perceivedResolution calculations will
        // always place the largest dimension as the width, and the second as height.
        Truth.assertThat(successResult.getPerceivedResolution().width).isEqualTo(750);
        Truth.assertThat(successResult.getPerceivedResolution().height).isEqualTo(500);
    }
}
