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

import androidx.xr.runtime.FieldOfView;
import androidx.xr.runtime.NodeHolder;
import androidx.xr.runtime.math.FloatSize2d;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.PerceivedResolutionResult;
import androidx.xr.scenecore.runtime.PixelDimensions;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.SurfaceEntity;
import androidx.xr.scenecore.runtime.SurfaceEntity.Shape;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeScenePose;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeSurfaceFeature;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.space.ShadowSpatialState;
import com.android.extensions.xr.space.SpatialState;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.function.Supplier;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public final class SurfaceEntityImplTest {
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private final Supplier<SpatialState> mSpatialStateProvider = ShadowSpatialState::create;
    private final PixelDimensions mViewPlaneResolution = new PixelDimensions(2000, 1000);
    private SurfaceEntityImpl mSurfaceEntity;
    private EntityManager mEntityManager;
    private ActivitySpaceImpl mActivitySpace;
    private FakeSurfaceFeature mFakeSurfaceFeature;
    private FakeScenePose mRenderViewScenePose;
    private FieldOfView mRenderViewFov;

    @Before
    public void setUp() {
        String widthAndHeightConfig =
                "+w" + mViewPlaneResolution.width + "dp-h" + mViewPlaneResolution.height + "dp";
        RuntimeEnvironment.setQualifiers(widthAndHeightConfig);

        Assert.assertNotNull(mXrExtensions);

        NodeHolder<?> nodeHolder = new NodeHolder<>(mXrExtensions.createNode(), Node.class);
        mFakeSurfaceFeature = new FakeSurfaceFeature(nodeHolder);
        mEntityManager = new EntityManager();

        mActivitySpace =
                new ActivitySpaceImpl(
                        mXrExtensions.createNode(),
                        mActivity,
                        mXrExtensions,
                        mEntityManager,
                        mSpatialStateProvider,
                        mExecutor);
        mEntityManager.addSystemSpaceActivityPose(new PerceptionSpaceScenePoseImpl(mActivitySpace));

        mSurfaceEntity =
                new SurfaceEntityImpl(
                        mActivity,
                        mFakeSurfaceFeature,
                        mActivitySpace,
                        mXrExtensions,
                        mEntityManager,
                        mExecutor);
        mSurfaceEntity.setPose(Pose.Identity, Space.PARENT);

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
        mEntityManager.clear();
        if (mSurfaceEntity != null) {
            mSurfaceEntity.dispose();
        }
        if (mActivitySpace != null) {
            mActivitySpace.dispose();
        }
    }

    private void assertShapeIsSetCorrectly(SurfaceEntity.Shape expectedShape) {
        mSurfaceEntity.setShape(expectedShape);
        SurfaceEntity.Shape actualShape = mSurfaceEntity.getShape();

        assertThat(actualShape).isInstanceOf(expectedShape.getClass());
        assertThat(actualShape.getDimensions()).isEqualTo(expectedShape.getDimensions());
    }

    @Test
    public void setShape_setsShape() {
        assertShapeIsSetCorrectly(new SurfaceEntity.Shape.Quad(new FloatSize2d(12f, 12f), 1.5f));
        assertThat(((SurfaceEntity.Shape.Quad) mSurfaceEntity.getShape()).getCornerRadius())
                .isEqualTo(1.5f);
        assertShapeIsSetCorrectly(new SurfaceEntity.Shape.Sphere(11f));
        assertShapeIsSetCorrectly(new SurfaceEntity.Shape.Hemisphere(10f));
    }

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

    @Test
    public void dispose_supports_reentry() {
        // Note that we don't test that dispose prevents manipulating other properties because that
        // is enforced at the API level, rather than the implementation level.
        mSurfaceEntity.dispose();
        mSurfaceEntity.dispose(); // shouldn't crash
    }

    @Test
    public void setEdgeFeather_forwardsToFeature() {
        float kFeatherRadiusX = 0.14f;
        float kFeatherRadiusY = 0.28f;
        SurfaceEntity.EdgeFeather expectedFeather =
                new SurfaceEntity.EdgeFeather.RectangleFeather(kFeatherRadiusX, kFeatherRadiusY);
        mSurfaceEntity.setEdgeFeather(expectedFeather);
        SurfaceEntity.EdgeFeather returnedFeather = mSurfaceEntity.getEdgeFeather();

        assertThat(returnedFeather).isEqualTo(expectedFeather);
    }

    @Test
    public void setColliderEnabled_forwardsToFeature() {
        mSurfaceEntity.setColliderEnabled(true);

        assertThat(mFakeSurfaceFeature.getColliderEnabled()).isTrue();

        mSurfaceEntity.setColliderEnabled(false);

        assertThat(mFakeSurfaceFeature.getColliderEnabled()).isFalse();
    }

    @Test
    public void getPerceivedResolution_quadInFront_returnsSuccess() {
        Shape.Quad quadShape = new Shape.Quad(new FloatSize2d(2.0f, 1.0f)); // 2m wide, 1m high
        mFakeSurfaceFeature.setShape(quadShape);

        mSurfaceEntity.setPose(new Pose(new Vector3(0f, 0f, -2f), Quaternion.Identity)); // 2m away
        mSurfaceEntity.setScale(new Vector3(1f, 1f, 1f));

        PerceivedResolutionResult result =
                mSurfaceEntity.getPerceivedResolution(mRenderViewScenePose, mRenderViewFov);
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success.class);
        PerceivedResolutionResult.Success successResult =
                (PerceivedResolutionResult.Success) result;

        assertThat(successResult.getPerceivedResolution().width).isEqualTo(500);
        assertThat(successResult.getPerceivedResolution().height).isEqualTo(250);
    }

    @Test
    public void getPerceivedResolution_sphereInFront_returnsSuccess() {
        Shape.Sphere sphereShape = new Shape.Sphere(1.0f); // radius 1m
        mFakeSurfaceFeature.setShape(sphereShape);

        mSurfaceEntity.setPose(new Pose(new Vector3(0f, 0f, -3f), Quaternion.Identity)); //
        // Center 3m away
        mSurfaceEntity.setScale(new Vector3(1f, 1f, 1f));

        PerceivedResolutionResult result =
                mSurfaceEntity.getPerceivedResolution(mRenderViewScenePose, mRenderViewFov);
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success.class);
        PerceivedResolutionResult.Success successResult =
                (PerceivedResolutionResult.Success) result;

        assertThat(successResult.getPerceivedResolution().width).isEqualTo(500);
        assertThat(successResult.getPerceivedResolution().height).isEqualTo(500);
    }

    @Test
    public void getPerceivedResolution_quadTooClose_returnsEntityTooClose() {
        Shape.Quad quadShape = new Shape.Quad(new FloatSize2d(2.0f, 1.0f));
        mFakeSurfaceFeature.setShape(quadShape);

        float veryCloseDistance = PerceivedResolutionUtils.PERCEIVED_RESOLUTION_EPSILON / 2f;
        mSurfaceEntity.setPose(
                new Pose(new Vector3(0f, 0f, -veryCloseDistance), Quaternion.Identity));
        mSurfaceEntity.setScale(new Vector3(1f, 1f, 1f));

        PerceivedResolutionResult result =
                mSurfaceEntity.getPerceivedResolution(mRenderViewScenePose, mRenderViewFov);
        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose.class);
    }

    @Test
    public void getPerceivedResolution_quadWithScale_calculatesCorrectly() {
        Shape.Quad quadShape = new Shape.Quad(new FloatSize2d(1.0f, 1.0f)); // 1m x 1m local
        mFakeSurfaceFeature.setShape(quadShape);

        mSurfaceEntity.setPose(new Pose(new Vector3(0f, 0f, -2f), Quaternion.Identity)); // 2m away
        mSurfaceEntity.setScale(new Vector3(2f, 3f, 1f)); // Scaled to 2m wide, 3m high

        PerceivedResolutionResult result =
                mSurfaceEntity.getPerceivedResolution(mRenderViewScenePose, mRenderViewFov);
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success.class);
        PerceivedResolutionResult.Success successResult =
                (PerceivedResolutionResult.Success) result;

        // The width and height are flipped because perceivedResolution calculations will
        // always place the largest dimension as the width, and the second as height.
        assertThat(successResult.getPerceivedResolution().width).isEqualTo(750);
        assertThat(successResult.getPerceivedResolution().height).isEqualTo(500);
    }

    @Test
    public void getParent_nullParent_returnsNull() {
        mSurfaceEntity.setParent(null);
        assertThat(mSurfaceEntity.getParent()).isEqualTo(null);
    }

    @Test
    public void getPoseInParentSpace_nullParent_returnsIdentity() {
        mSurfaceEntity.setParent(null);
        mSurfaceEntity.setPose(Pose.Identity);
        assertThat(mSurfaceEntity.getPose(Space.PARENT)).isEqualTo(Pose.Identity);
    }

    @Test
    public void getPoseInActivitySpace_nullParent_throwsException() {
        mSurfaceEntity.setParent(null);
        assertThrows(IllegalStateException.class, () -> mSurfaceEntity.getPose(Space.ACTIVITY));
    }

    @Test
    public void getPoseInRealWorldSpace_nullParent_throwsException() {
        mSurfaceEntity.setParent(null);
        assertThrows(IllegalStateException.class, () -> mSurfaceEntity.getPose(Space.REAL_WORLD));
    }
}
