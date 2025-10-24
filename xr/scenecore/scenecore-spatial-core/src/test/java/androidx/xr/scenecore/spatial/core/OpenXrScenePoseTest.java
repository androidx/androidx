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

import static androidx.xr.runtime.testing.math.MathAssertions.assertPose;
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.runtime.NodeHolder;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.perception.Fov;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.impl.perception.ViewProjection;
import androidx.xr.scenecore.impl.perception.ViewProjections;
import androidx.xr.scenecore.runtime.CameraViewScenePose;
import androidx.xr.scenecore.runtime.GltfFeature;
import androidx.xr.scenecore.runtime.HitTestResult;
import androidx.xr.scenecore.runtime.ScenePose.HitTestFilter;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeGltfFeature;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.Vec3;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameter;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.List;

/** Test for common behaviour for ScenePoses whose world position is retrieved from OpenXr. */
@RunWith(ParameterizedRobolectricTestRunner.class)
public final class OpenXrScenePoseTest {
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final PerceptionLibrary mPerceptionLibrary = mock(PerceptionLibrary.class);
    private final Session mSession = mock(Session.class);
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private final EntityManager mEntityManager = new EntityManager();
    private final Activity mActivity =
            Robolectric.buildActivity(Activity.class).create().start().get();
    private final ActivitySpaceImpl mActivitySpace =
            new ActivitySpaceImpl(
                    mXrExtensions.createNode(),
                    mActivity,
                    mXrExtensions,
                    mEntityManager,
                    () -> mXrExtensions.getSpatialState(mActivity),
                    /* unscaledGravityAlignedActivitySpace= */ false,
                    mExecutor);
    private final GltfFeature mMockGltfFeature = Mockito.mock(GltfFeature.class);

    enum OpenXrScenePoseType {
        HEAD_ACTIVITY_POSE,
        CAMERA_ACTIVITY_POSE,
        PERCEPTION_POSE_ACTIVITY_POSE,
    }

    @Parameter() public OpenXrScenePoseType testScenePoseType;

    BaseScenePose mTestScenePose;

    /** Creates and return list of OpenXrScenePoseType values. */
    @Parameters
    public static List<Object> data() {
        return Arrays.asList(
                new Object[] {
                    OpenXrScenePoseType.HEAD_ACTIVITY_POSE,
                    OpenXrScenePoseType.CAMERA_ACTIVITY_POSE,
                    OpenXrScenePoseType.PERCEPTION_POSE_ACTIVITY_POSE
                });
    }

    /** Creates a HeadScenePoseImpl instance. */
    private HeadScenePoseImpl createHeadScenePose(ActivitySpaceImpl activitySpace) {
        return new HeadScenePoseImpl(activitySpace, activitySpace, mPerceptionLibrary);
    }

    /** Creates a CameraViewScenePoseImpl instance. */
    private CameraViewScenePoseImpl createCameraViewScenePose(ActivitySpaceImpl activitySpace) {
        return new CameraViewScenePoseImpl(
                CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE,
                activitySpace,
                activitySpace,
                mPerceptionLibrary);
    }

    /** Creates an OpenXrActivityPose instance. */
    private OpenXrScenePose createOpenXrScenePose(ActivitySpaceImpl activitySpace, Pose pose) {
        return new OpenXrScenePose(activitySpace, activitySpace, pose);
    }

    private BaseScenePose createTestScenePose(Pose pose) {
        switch (testScenePoseType) {
            case HEAD_ACTIVITY_POSE:
                setPerceptionPose(pose);
                return createHeadScenePose(mActivitySpace);
            case CAMERA_ACTIVITY_POSE:
                setPerceptionPose(pose);
                return createCameraViewScenePose(mActivitySpace);
            case PERCEPTION_POSE_ACTIVITY_POSE:
                return createOpenXrScenePose(mActivitySpace, pose);
        }
        return null;
    }

    private void setPerceptionPose(Pose pose) {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                pose == null ? null : RuntimeUtils.poseToPerceptionPose(pose);
        switch (testScenePoseType) {
            case HEAD_ACTIVITY_POSE:
                when(mSession.getHeadPose()).thenReturn(perceptionPose);
                break;

            case CAMERA_ACTIVITY_POSE:
                if (perceptionPose == null) {
                    when(mSession.getStereoViews()).thenReturn(null);
                    break;
                }
                ViewProjection viewProjection =
                        new ViewProjection(perceptionPose, new Fov(0, 0, 0, 0));
                when(mSession.getStereoViews())
                        .thenReturn(new ViewProjections(viewProjection, viewProjection));
                break;
        }
    }

    /** Creates a generic glTF entity. */
    private GltfEntityImpl createGltfEntity() {
        NodeHolder<?> nodeHolder = new NodeHolder<>(mXrExtensions.createNode(), Node.class);
        GltfFeature fakeGltfFeature =
                FakeGltfFeature.Companion.createWithMockFeature(mMockGltfFeature, nodeHolder);

        return new GltfEntityImpl(
                mActivity,
                fakeGltfFeature,
                mActivitySpace,
                mXrExtensions,
                mEntityManager,
                mExecutor);
    }

    @Test
    public void
            getPoseInActivitySpace_noActivitySpaceOpenXrReferenceSpacePose_returnsIdentityPose() {
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mTestScenePose = createTestScenePose(pose);

        assertNotNull(mTestScenePose);
        assertPose(mTestScenePose.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_whenAtSamePose_returnsIdentityPose() {
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));

        assertPose(mTestScenePose.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_returnsDifferencePose() {
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);

        assertPose(mTestScenePose.getPoseInActivitySpace(), pose);
    }

    @Test
    public void getActivitySpaceScale_returnsInverseOfActivitySpaceWorldScale() {
        float activitySpaceScale = 5f;
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromScale(activitySpaceScale));
        mTestScenePose = createTestScenePose(Pose.Identity);

        assertNotNull(mTestScenePose);
        assertVector3(
                mTestScenePose.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));
    }

    @Test
    public void
            getPoseInActivitySpace_withScaledTranslatedActivitySpace_returnsScaledDifferencePose() {
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        Quaternion.Identity,
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        Pose expectedPose = new Pose(new Vector3(-0.5f, -1.0f, -1.5f), new Quaternion(0, 1, 0, 1));

        assertPose(mTestScenePose.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void getPoseInActivitySpace_witRotatedPerceptionPose_returnsDifferencePose() {
        Quaternion perceptionQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(0, 0, 0), perceptionQuaternion);
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        new Vector3(0f, 0f, 0f),
                        Quaternion.Identity,
                        /* scale= */ new Vector3(1f, 1f, 1f)));

        // If the activitySpace has an identity rotation, then there shouldn't be any change
        Pose expectedPose = new Pose(new Vector3(0f, 0f, 0f), perceptionQuaternion);

        assertPose(mTestScenePose.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void getPoseInActivitySpace_witRotatedActivitySpace_returnsDifferencePose() {
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(0, 0, 0), Quaternion.Identity);
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        new Vector3(0f, 0f, 0f),
                        activitySpaceQuaternion,
                        /* scale= */ new Vector3(1f, 1f, 1f)));
        // If perception pose is identity, then rotation should be the inverse of the activity
        // space.
        Pose expectedPose =
                new Pose(
                        new Vector3(0f, 0f, 0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, -90f)));

        assertPose(mTestScenePose.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void getPoseInActivitySpace_withScaledAndRotatedActivitySpace_returnsDifferencePose() {
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(1, 1, 1), Quaternion.Identity);
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        activitySpaceQuaternion,
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        // A 90 degree rotation around the z axis is a clockwise rotation of the XY plane.
        Pose expectedPose =
                new Pose(
                        new Vector3(-1.0f, 0.5f, -1.5f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, -90f)));

        assertPose(mTestScenePose.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void
            getPoseInActivitySpace_withCustomScaledAndRotatedActivitySpace_returnsDifferencePose() {
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(1, 1, 1), Quaternion.Identity);
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        activitySpaceQuaternion,
                        /* scale= */ new Vector3(1f, 2f, 3f)));
        // A 90 degree rotation around the z axis is a clockwise rotation of the XY plane.
        Pose expectedPose =
                new Pose(
                        new Vector3(-2.5f, 0f, -1f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, -90f)));

        assertPose(mTestScenePose.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void
            getPoseInActivitySpace_withMinusScaledAndRotatedActivitySpace_returnsDifferencePose() {
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(1, 1, 1), Quaternion.Identity);
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        activitySpaceQuaternion,
                        /* scale= */ new Vector3(-1f, -2f, -3f)));
        // A 90 degree rotation around the z axis is a clockwise rotation of the XY plane.
        Pose expectedPose =
                new Pose(
                        // We keep the scale positive for now, hence the negative translation.
                        new Vector3(-2.5f, 0f, -1f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, -90f)));

        assertPose(mTestScenePose.getPoseInActivitySpace(), expectedPose);
    }

    // TODO: Add tests with children of these entities

    @Test
    public void getActivitySpacePose_returnsDifferencePose() {
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mTestScenePose = createTestScenePose(pose);

        assertNotNull(mTestScenePose);
        assertPose(mTestScenePose.getActivitySpacePose(), pose);
    }

    @Test
    public void getActivitySpacePose_withScaledActivitySpace_returnsDifferencePose() {
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        Quaternion.Identity,
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        Pose expectedPose = new Pose(new Vector3(-0.5f, -1.0f, -1.5f), new Quaternion(0, 1, 0, 1));

        assertPose(mTestScenePose.getActivitySpacePose(), expectedPose);
    }

    @Test
    public void getActivitySpacePose_withScaledAndRotatedActivitySpace_returnsDifferencePose() {
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(1, 1, 1), Quaternion.Identity);
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        activitySpaceQuaternion,
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        // A 90 degree rotation around the z axis is a clockwise rotation of the XY plane.
        Pose expectedPose =
                new Pose(
                        new Vector3(-1.0f, 0.5f, -1.5f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, -90f)));

        assertPose(mTestScenePose.getActivitySpacePose(), expectedPose);
    }

    @Test
    public void getActivitySpacePoseWithError_returnsLastKnownPose() {
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        // Skip for OpenXrScenePose
        if (testScenePoseType == OpenXrScenePoseType.PERCEPTION_POSE_ACTIVITY_POSE) {
            return;
        }

        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mTestScenePose = createTestScenePose(pose);

        assertNotNull(mTestScenePose);
        assertPose(mTestScenePose.getActivitySpacePose(), pose);

        setPerceptionPose(null);

        assertPose(mTestScenePose.getActivitySpacePose(), pose);
    }

    @Test
    public void transformPoseTo_withActivitySpace_returnsTransformedPose() {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);

        Pose userHeadSpaceOffset =
                new Pose(
                        new Vector3(10f, 0f, 0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)));
        Pose transformedPose = mTestScenePose.transformPoseTo(userHeadSpaceOffset, mActivitySpace);

        assertPose(
                transformedPose,
                new Pose(
                        new Vector3(11f, 2f, 3f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f))));
    }

    @Test
    public void
            transformPoseTo_withScaledActivitySpaceAndDifferentSourcePose_returnsTransformedPose() {
        Pose openXrPose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        mTestScenePose = createTestScenePose(openXrPose);
        setPerceptionPose(openXrPose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        /* translation= */ new Vector3(2f, 3f, 4f),
                        /* rotation= */ Quaternion.Identity,
                        /* scale= */ new Vector3(2f, 2f, 2f)));

        assertVector3(mTestScenePose.getActivitySpaceScale(), new Vector3(0.5f, 0.5f, 0.5f));

        Pose userHeadSpaceOffset =
                new Pose(
                        new Vector3(10f, 5f, 4f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)));
        Pose transformedPose = mTestScenePose.transformPoseTo(userHeadSpaceOffset, mActivitySpace);

        assertPose(
                transformedPose,
                new Pose(
                        new Vector3(4.5f, 2f, 1.5f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f))));
    }

    @Test
    public void transformPoseTo_withScaledActivitySpace_returnsSourcePoseScaledInActivitySpace() {
        // With scaled activity space and identity offset
        Pose openXrPose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        mTestScenePose = createTestScenePose(openXrPose);
        setPerceptionPose(openXrPose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromScale(2f));

        Pose expectedPose = mTestScenePose.getPoseInActivitySpace();
        Pose transformedPose = mTestScenePose.transformPoseTo(Pose.Identity, mActivitySpace);

        assertPose(transformedPose, expectedPose);
    }

    @Test
    public void transformPoseTo_withScaledActivitySpaceAtSourcePose_returnsScaledOffset() {
        Pose openXrPose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        mTestScenePose = createTestScenePose(openXrPose);
        setPerceptionPose(openXrPose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        openXrPose.getTranslation(),
                        openXrPose.getRotation(),
                        /* scale= */ new Vector3(2f, 2f, 2f)));

        assertVector3(mTestScenePose.getActivitySpaceScale(), new Vector3(0.5f, 0.5f, 0.5f));

        Pose userHeadSpaceOffset =
                new Pose(
                        new Vector3(10f, 0f, 0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)));
        Pose transformedPose = mTestScenePose.transformPoseTo(userHeadSpaceOffset, mActivitySpace);

        assertPose(
                transformedPose,
                new Pose(
                        new Vector3(5f, 0f, 0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f))));
    }

    @Test
    public void transformPoseTo_fromActivitySpaceChild_returnsUserHeadSpacePose() {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        mTestScenePose = createTestScenePose(pose);
        GltfEntityImpl childEntity1 = createGltfEntity();
        Pose childPose = new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity);

        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        mActivitySpace.addChild(childEntity1);
        childEntity1.setPose(childPose);

        assertPose(
                mActivitySpace.transformPoseTo(new Pose(), mTestScenePose),
                new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity));

        Pose transformedPose = childEntity1.transformPoseTo(new Pose(), mTestScenePose);

        assertPose(transformedPose, new Pose(new Vector3(-2f, -4f, -6f), Quaternion.Identity));
    }

    @Test
    public void hitTest_returnsTransformedHitTest() throws Exception {
        Pose pose =
                new Pose(
                        new Vector3(1f, 1, 1f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f)));
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        float distance = 2.0f;
        Vec3 hitPosition = new Vec3(1.0f, 2.0f, 3.0f);
        Vec3 surfaceNormal = new Vec3(0.0f, 1.0f, 0.0f);
        int surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL;
        com.android.extensions.xr.space.HitTestResult extensionsHitTestResult =
                new com.android.extensions.xr.space.HitTestResult.Builder(
                                distance, hitPosition, true, surfaceType)
                        .setSurfaceNormal(surfaceNormal)
                        .build();
        ShadowXrExtensions.extract(mXrExtensions)
                .setHitTestResult(mActivity, extensionsHitTestResult);

        ListenableFuture<HitTestResult> hitTestResultFuture =
                mTestScenePose.hitTest(
                        new Vector3(1f, 1f, 1f), new Vector3(1f, 1f, 1f), HitTestFilter.SELF_SCENE);
        mExecutor.runAll();
        HitTestResult hitTestResult = hitTestResultFuture.get();

        assertThat(hitTestResult).isNotNull();
        assertThat(hitTestResult.getDistance()).isEqualTo(distance);
        // Since the entity is rotated 90 degrees about the x axis, the hit position should be
        // rotated 90 degrees about the x axis.
        assertVector3(hitTestResult.getHitPosition(), new Vector3(0f, 2f, -1f));
        assertVector3(hitTestResult.getSurfaceNormal(), new Vector3(0f, 0f, -1f));
        assertThat(hitTestResult.getSurfaceType())
                .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE);
    }

    @Test
    public void hitTest_withScaledActivitySpace_returnsTransformedHitTest() throws Exception {
        Pose pose =
                new Pose(
                        new Vector3(1f, 1f, 1f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f)));
        mTestScenePose = createTestScenePose(pose);
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromScale(2f));
        float distance = 2.0f;
        Vec3 hitPosition = new Vec3(0.5f, 1.0f, 1.5f);
        Vec3 surfaceNormal = new Vec3(0.0f, 1.0f, 0.0f);
        int surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL;
        com.android.extensions.xr.space.HitTestResult extensionsHitTestResult =
                new com.android.extensions.xr.space.HitTestResult.Builder(
                                distance, hitPosition, true, surfaceType)
                        .setSurfaceNormal(surfaceNormal)
                        .build();
        ShadowXrExtensions.extract(mXrExtensions)
                .setHitTestResult(mActivity, extensionsHitTestResult);

        ListenableFuture<HitTestResult> hitTestResultFuture =
                mTestScenePose.hitTest(
                        new Vector3(1f, 1f, 1f), new Vector3(1f, 1f, 1f), HitTestFilter.SELF_SCENE);
        mExecutor.runAll();
        HitTestResult hitTestResult = hitTestResultFuture.get();

        assertThat(hitTestResult).isNotNull();
        assertThat(hitTestResult.getDistance()).isEqualTo(distance);
        // Since the entity is rotated 90 degrees about the x axis, the hit position should be
        // rotated 90 degrees about the x axis.
        assertVector3(hitTestResult.getHitPosition(), new Vector3(0f, 2f, -1f));
        assertVector3(hitTestResult.getSurfaceNormal(), new Vector3(0f, 0f, -2f));
        assertThat(hitTestResult.getSurfaceType())
                .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE);
    }
}
