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

import static androidx.xr.runtime.testing.math.MathAssertions.assertPose;
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.runtime.internal.ActivityPose.HitTestFilter;
import androidx.xr.runtime.internal.CameraViewActivityPose;
import androidx.xr.runtime.internal.HitTestResult;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.perception.Fov;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.impl.perception.ViewProjection;
import androidx.xr.scenecore.impl.perception.ViewProjections;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Vec3;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.apibindings.FakeImpressApiImpl;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameter;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.List;

/** Test for common behaviour for ActivityPoses whose world position is retrieved from OpenXr. */
@RunWith(ParameterizedRobolectricTestRunner.class)
public final class OpenXrActivityPoseTest {
    private final AndroidXrEntity mActivitySpaceRoot = mock(AndroidXrEntity.class);
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
    private final FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);

    enum OpenXrActivityPoseType {
        HEAD_ACTIVITY_POSE,
        CAMERA_ACTIVITY_POSE,
    }

    @Parameter(0)
    public OpenXrActivityPoseType testActivityPoseType;

    BaseActivityPose mTestActivityPose;

    /** Creates and return list of OpenXrActivityPoseType values. */
    @Parameters
    public static List<Object> data() throws Exception {
        return Arrays.asList(
                new Object[] {
                    OpenXrActivityPoseType.HEAD_ACTIVITY_POSE,
                    OpenXrActivityPoseType.CAMERA_ACTIVITY_POSE
                });
    }

    @Before
    public void doBeforeEachTest() {
        // By default, set the activity space to the root of the underlying OpenXR reference space.
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        when(mActivitySpaceRoot.getWorldSpaceScale())
                .thenReturn(mActivitySpace.getWorldSpaceScale());
    }

    /** Creates a HeadActivityPoseImpl instance. */
    private HeadActivityPoseImpl createHeadActivityPose(
            ActivitySpaceImpl activitySpace, AndroidXrEntity activitySpaceRoot) {
        return new HeadActivityPoseImpl(activitySpace, activitySpaceRoot, mPerceptionLibrary);
    }

    /** Creates a CameraViewActivityPoseImpl instance. */
    private CameraViewActivityPoseImpl createCameraViewActivityPose(
            ActivitySpaceImpl activitySpace, AndroidXrEntity activitySpaceRoot) {
        return new CameraViewActivityPoseImpl(
                CameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE,
                activitySpace,
                activitySpaceRoot,
                mPerceptionLibrary);
    }

    private BaseActivityPose createTestActivityPose() {
        return createTestActivityPose(mActivitySpace, mActivitySpaceRoot);
    }

    private BaseActivityPose createTestActivityPose(
            ActivitySpaceImpl activitySpace, AndroidXrEntity activitySpaceRoot) {
        switch (testActivityPoseType) {
            case HEAD_ACTIVITY_POSE:
                return createHeadActivityPose(activitySpace, activitySpaceRoot);
            case CAMERA_ACTIVITY_POSE:
                return createCameraViewActivityPose(activitySpace, activitySpaceRoot);
        }
        return null;
    }

    private void setPerceptionPose(Pose pose) {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                pose == null ? null : RuntimeUtils.poseToPerceptionPose(pose);
        switch (testActivityPoseType) {
            case HEAD_ACTIVITY_POSE:
                {
                    when(mSession.getHeadPose()).thenReturn(perceptionPose);
                    break;
                }
            case CAMERA_ACTIVITY_POSE:
                {
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
    }

    /** Creates a generic glTF entity. */
    private GltfEntityImpl createGltfEntity() {
        long modelToken = -1;
        try {
            ListenableFuture<Long> modelTokenFuture =
                    mFakeImpressApi.loadGltfAsset("FakeGltfAsset.glb");
            // This resolves the transformation of the Future from a SplitEngine token to the JXR
            // GltfModelResource.  This is a hidden detail from the API surface's perspective.
            mExecutor.runAll();
            modelToken = modelTokenFuture.get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        GltfModelResourceImpl model = new GltfModelResourceImpl(modelToken);
        return new GltfEntityImpl(
                mActivity,
                model,
                mActivitySpace,
                mFakeImpressApi,
                mSplitEngineSubspaceManager,
                mXrExtensions,
                mEntityManager,
                mExecutor);
    }

    @Test
    public void
            getPoseInActivitySpace_noActivitySpaceOpenXrReferenceSpacePose_returnsIdentityPose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        setPerceptionPose(pose);
        mActivitySpace.mOpenXrReferenceSpacePose = null;

        assertPose(mTestActivityPose.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_whenAtSamePose_returnsIdentityPose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(mTestActivityPose.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);

        assertPose(mTestActivityPose.getPoseInActivitySpace(), pose);
    }

    @Test
    public void getActivitySpaceScale_returnsInverseOfActivitySpaceWorldScale() throws Exception {
        float activitySpaceScale = 5f;
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.fromScale(activitySpaceScale));
        mTestActivityPose = createTestActivityPose();
        assertVector3(
                mTestActivityPose.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));
    }

    @Test
    public void
            getPoseInActivitySpace_withScaledTranslatedActivitySpace_returnsScaledDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        Quaternion.Identity,
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        Pose expectedPose = new Pose(new Vector3(-0.5f, -1.0f, -1.5f), new Quaternion(0, 1, 0, 1));

        assertPose(mTestActivityPose.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void getPoseInActivitySpace_witRotatedPerceptionPose_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Quaternion perceptionQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(0, 0, 0), perceptionQuaternion);
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        new Vector3(0f, 0f, 0f),
                        Quaternion.Identity,
                        /* scale= */ new Vector3(1f, 1f, 1f)));

        // If the activitySpace has an identity rotation, then there shouldn't be any change
        Pose expectedPose = new Pose(new Vector3(0f, 0f, 0f), perceptionQuaternion);
        assertPose(mTestActivityPose.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void getPoseInActivitySpace_witRotatedActivitySpace_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(0, 0, 0), Quaternion.Identity);
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(
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

        assertPose(mTestActivityPose.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void getPoseInActivitySpace_withScaledAndRotatedActivitySpace_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(1, 1, 1), Quaternion.Identity);
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        activitySpaceQuaternion,
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        // A 90 degree rotation around the z axis is a clockwise rotation of the XY plane.
        Pose expectedPose =
                new Pose(
                        new Vector3(-1.0f, 0.5f, -1.5f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, -90f)));

        assertPose(mTestActivityPose.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void
            getPoseInActivitySpace_withCustomScaledAndRotatedActivitySpace_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(1, 1, 1), Quaternion.Identity);
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        activitySpaceQuaternion,
                        /* scale= */ new Vector3(1f, 2f, 3f)));
        // A 90 degree rotation around the z axis is a clockwise rotation of the XY plane.
        Pose expectedPose =
                new Pose(
                        new Vector3(-2.5f, 0f, -1f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, -90f)));

        assertPose(mTestActivityPose.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void
            getPoseInActivitySpace_withMinusScaledAndRotatedActivitySpace_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(1, 1, 1), Quaternion.Identity);
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(
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

        assertPose(mTestActivityPose.getPoseInActivitySpace(), expectedPose);
    }

    // TODO: Add tests with children of these entities

    @Test
    public void getPoseInActivitySpace_withNoActivitySpace_returnsIdentityPose() {
        mTestActivityPose = createTestActivityPose(/* activitySpace= */ null, mActivitySpaceRoot);

        assertPose(mTestActivityPose.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getActivitySpacePose_whenAtSamePose_returnsIdentityPose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        setPerceptionPose(pose);
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(pose);

        assertPose(mTestActivityPose.getActivitySpacePose(), new Pose());
    }

    @Test
    public void getActivitySpacePose_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        setPerceptionPose(pose);
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(new Pose());

        assertPose(mTestActivityPose.getActivitySpacePose(), pose);
    }

    @Test
    public void getActivitySpacePose_withScaledActivitySpace_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        Quaternion.Identity,
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(new Pose());
        Pose expectedPose = new Pose(new Vector3(-0.5f, -1.0f, -1.5f), new Quaternion(0, 1, 0, 1));

        assertPose(mTestActivityPose.getActivitySpacePose(), expectedPose);
    }

    @Test
    public void getActivitySpacePose_withScaledAndRotatedActivitySpace_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(1, 1, 1), Quaternion.Identity);
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        activitySpaceQuaternion,
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(new Pose());
        // A 90 degree rotation around the z axis is a clockwise rotation of the XY plane.
        Pose expectedPose =
                new Pose(
                        new Vector3(-1.0f, 0.5f, -1.5f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, -90f)));

        assertPose(mTestActivityPose.getActivitySpacePose(), expectedPose);
    }

    @Test
    public void getActivitySpacePoseWithError_returnsLastKnownPose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        setPerceptionPose(pose);
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(new Pose());
        assertPose(mTestActivityPose.getActivitySpacePose(), pose);

        setPerceptionPose(null);
        assertPose(mTestActivityPose.getActivitySpacePose(), pose);
    }

    @Test
    public void getActivitySpacePose_withNonAndroidXrActivitySpaceRoot_returnsIdentityPose()
            throws Exception {
        mTestActivityPose = createTestActivityPose(mActivitySpace, /* activitySpaceRoot= */ null);

        assertPose(mTestActivityPose.getActivitySpacePose(), new Pose());
    }

    @Test
    public void transformPoseTo_withActivitySpace_returnsTransformedPose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);

        Pose userHeadSpaceOffset =
                new Pose(
                        new Vector3(10f, 0f, 0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)));
        Pose transformedPose =
                mTestActivityPose.transformPoseTo(userHeadSpaceOffset, mActivitySpace);
        assertPose(
                transformedPose,
                new Pose(
                        new Vector3(11f, 2f, 3f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f))));
    }

    @Test
    public void
            transformPoseTo_withScaledActivitySpaceAndDifferentSourcePose_returnsTransformedPose() {
        mTestActivityPose = createTestActivityPose();
        Pose openXrPose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        setPerceptionPose(openXrPose);
        mActivitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        /* translation= */ new Vector3(2f, 3f, 4f),
                        /* rotation= */ Quaternion.Identity,
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        assertVector3(mTestActivityPose.getActivitySpaceScale(), new Vector3(0.5f, 0.5f, 0.5f));

        Pose userHeadSpaceOffset =
                new Pose(
                        new Vector3(10f, 5f, 4f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)));
        Pose transformedPose =
                mTestActivityPose.transformPoseTo(userHeadSpaceOffset, mActivitySpace);
        assertPose(
                transformedPose,
                new Pose(
                        new Vector3(4.5f, 2f, 1.5f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f))));
    }

    @Test
    public void
            transformPoseTo_withScaledActivitySpaceAndIdentityOffset_returnsSourcePoseScaledInActivitySpace() {
        mTestActivityPose = createTestActivityPose();
        Pose openXrPose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        setPerceptionPose(openXrPose);
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.fromScale(2f));

        Pose expectedPose = mTestActivityPose.getPoseInActivitySpace();
        Pose transformedPose = mTestActivityPose.transformPoseTo(Pose.Identity, mActivitySpace);
        assertPose(transformedPose, expectedPose);
    }

    @Test
    public void transformPoseTo_withScaledActivitySpaceAtSourcePose_returnsScaledOffset() {
        mTestActivityPose = createTestActivityPose();
        Pose openXrPose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        setPerceptionPose(openXrPose);
        mActivitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        openXrPose.getTranslation(),
                        openXrPose.getRotation(),
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        assertVector3(mTestActivityPose.getActivitySpaceScale(), new Vector3(0.5f, 0.5f, 0.5f));

        Pose userHeadSpaceOffset =
                new Pose(
                        new Vector3(10f, 0f, 0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)));
        Pose transformedPose =
                mTestActivityPose.transformPoseTo(userHeadSpaceOffset, mActivitySpace);
        assertPose(
                transformedPose,
                new Pose(
                        new Vector3(5f, 0f, 0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f))));
    }

    @Test
    public void transformPoseTo_fromActivitySpaceChild_returnsUserHeadSpacePose() {
        mTestActivityPose = createTestActivityPose();
        GltfEntityImpl childEntity1 = createGltfEntity();
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        Pose childPose = new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity);

        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        mActivitySpace.addChild(childEntity1);
        childEntity1.setPose(childPose);
        setPerceptionPose(pose);

        assertPose(
                mActivitySpace.transformPoseTo(new Pose(), mTestActivityPose),
                new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity));

        Pose transformedPose = childEntity1.transformPoseTo(new Pose(), mTestActivityPose);
        assertPose(transformedPose, new Pose(new Vector3(-2f, -4f, -6f), Quaternion.Identity));
    }

    @Test
    public void hitTest_returnsTransformedHitTest() throws Exception {
        mTestActivityPose = createTestActivityPose();
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(new Pose());
        setPerceptionPose(
                new Pose(
                        new Vector3(1f, 1, 1f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f))));
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
                mTestActivityPose.hitTest(
                        new Vector3(1f, 1f, 1f), new Vector3(1f, 1f, 1f), HitTestFilter.SELF_SCENE);
        mExecutor.runAll();
        HitTestResult hitTestResult = hitTestResultFuture.get();

        assertThat(hitTestResult.getDistance()).isEqualTo(distance);
        // Since the entity is rotated 90 degrees about the x axis, the hit position should be
        // rotated
        // 90 degrees about the x axis.
        assertVector3(hitTestResult.getHitPosition(), new Vector3(0f, 2f, -1f));
        assertVector3(hitTestResult.getSurfaceNormal(), new Vector3(0f, 0f, -1f));
        assertThat(hitTestResult.getSurfaceType())
                .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE);
    }

    @Test
    public void hitTest_withScaledActivitySpace_returnsTransformedHitTest() throws Exception {
        mTestActivityPose = createTestActivityPose();
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.fromScale(2f));
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(new Pose());
        setPerceptionPose(
                new Pose(
                        new Vector3(1f, 1f, 1f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f))));
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
                mTestActivityPose.hitTest(
                        new Vector3(1f, 1f, 1f), new Vector3(1f, 1f, 1f), HitTestFilter.SELF_SCENE);
        mExecutor.runAll();
        HitTestResult hitTestResult = hitTestResultFuture.get();

        assertThat(hitTestResult.getDistance()).isEqualTo(distance);
        // Since the entity is rotated 90 degrees about the x axis, the hit position should be
        // rotated
        // 90 degrees about the x axis.
        assertVector3(hitTestResult.getHitPosition(), new Vector3(0f, 2f, -1f));
        assertVector3(hitTestResult.getSurfaceNormal(), new Vector3(0f, 0f, -2f));
        assertThat(hitTestResult.getSurfaceType())
                .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE);
    }
}
