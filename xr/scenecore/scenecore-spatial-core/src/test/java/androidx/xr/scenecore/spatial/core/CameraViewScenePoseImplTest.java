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

import static org.mockito.Mockito.when;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.impl.perception.ViewProjection;
import androidx.xr.scenecore.impl.perception.ViewProjections;
import androidx.xr.scenecore.runtime.CameraViewScenePose;
import androidx.xr.scenecore.runtime.CameraViewScenePose.CameraType;
import androidx.xr.scenecore.runtime.CameraViewScenePose.Fov;
import androidx.xr.scenecore.runtime.PixelDimensions;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

// Suppress warnings: windowManager's getDefaultDisplay and getRealMetrics.
@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
public final class CameraViewScenePoseImplTest {

    private final AndroidXrEntity mActivitySpaceRoot = Mockito.mock(AndroidXrEntity.class);
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final PerceptionLibrary mPerceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    private final Session mSession = Mockito.mock(Session.class);
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private final Activity mActivity =
            Robolectric.buildActivity(Activity.class).create().start().get();
    private final ActivitySpaceImpl mActivitySpace =
            new ActivitySpaceImpl(
                    mXrExtensions.createNode(),
                    mActivity,
                    mXrExtensions,
                    new EntityManager(),
                    () -> mXrExtensions.getSpatialState(mActivity),
                    /* unscaledGravityAlignedActivitySpace= */ false,
                    mExecutor);

    /** Creates a CameraViewScenePoseImpl. */
    private CameraViewScenePoseImpl createCameraViewScenePose(@CameraType int cameraType) {
        return new CameraViewScenePoseImpl(
                cameraType, mActivitySpace, mActivitySpaceRoot, mPerceptionLibrary);
    }

    @Test
    public void getCameraType_returnsCameraType() {
        CameraViewScenePoseImpl cameraScenePoseLeft =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE);

        assertThat(cameraScenePoseLeft.getCameraType())
                .isEqualTo(CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE);

        CameraViewScenePoseImpl cameraScenePoseRight =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_RIGHT_EYE);

        assertThat(cameraScenePoseRight.getCameraType())
                .isEqualTo(CameraViewScenePose.CameraType.CAMERA_TYPE_RIGHT_EYE);
    }

    @Test
    public void getFov_returnsCorrectFov() {
        Fov fovLeft = new Fov(1, 2, 3, 4);
        Fov fovRight = new Fov(5, 6, 7, 8);

        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        ViewProjection viewProjectionLeft =
                new ViewProjection(
                        RuntimeUtils.poseToPerceptionPose(new Pose()),
                        RuntimeUtils.perceptionFovFromFov(fovLeft));
        ViewProjection viewProjectionRight =
                new ViewProjection(
                        RuntimeUtils.poseToPerceptionPose(new Pose()),
                        RuntimeUtils.perceptionFovFromFov(fovRight));
        when(mSession.getStereoViews())
                .thenReturn(new ViewProjections(viewProjectionLeft, viewProjectionRight));

        CameraViewScenePoseImpl cameraViewScenePose =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE);

        assertThat(cameraViewScenePose.getFov().getAngleLeft()).isEqualTo(fovLeft.getAngleLeft());
        assertThat(cameraViewScenePose.getFov().getAngleRight()).isEqualTo(fovLeft.getAngleRight());
        assertThat(cameraViewScenePose.getFov().getAngleUp()).isEqualTo(fovLeft.getAngleUp());
        assertThat(cameraViewScenePose.getFov().getAngleDown()).isEqualTo(fovLeft.getAngleDown());

        CameraViewScenePoseImpl cameraScenePoseRight =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_RIGHT_EYE);

        assertThat(cameraScenePoseRight.getFov().getAngleLeft()).isEqualTo(fovRight.getAngleLeft());
        assertThat(cameraScenePoseRight.getFov().getAngleRight())
                .isEqualTo(fovRight.getAngleRight());
        assertThat(cameraScenePoseRight.getFov().getAngleUp()).isEqualTo(fovRight.getAngleUp());
        assertThat(cameraScenePoseRight.getFov().getAngleDown()).isEqualTo(fovRight.getAngleDown());
    }

    @Test
    public void transformPoseTo_returnsCorrectPose() {
        // Set the activity space to the root of the underlying OpenXR reference space.
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        Pose poseLeft = new Pose(new Vector3(1, 2, 3), new Quaternion(1, 0, 0, 0));
        Pose poseRight = new Pose(new Vector3(4, 5, 6), new Quaternion(0, 1, 0, 0));
        Fov fov = new Fov(0, 0, 0, 0);

        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        ViewProjection viewProjectionLeft =
                new ViewProjection(
                        RuntimeUtils.poseToPerceptionPose(poseLeft),
                        RuntimeUtils.perceptionFovFromFov(fov));
        ViewProjection viewProjectionRight =
                new ViewProjection(
                        RuntimeUtils.poseToPerceptionPose(poseRight),
                        RuntimeUtils.perceptionFovFromFov(fov));
        when(mSession.getStereoViews())
                .thenReturn(new ViewProjections(viewProjectionLeft, viewProjectionRight));

        CameraViewScenePoseImpl cameraScenePoseLeft =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE);

        assertPose(cameraScenePoseLeft.transformPoseTo(new Pose(), mActivitySpace), poseLeft);

        CameraViewScenePoseImpl cameraScenePoseRight =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_RIGHT_EYE);

        assertPose(cameraScenePoseRight.transformPoseTo(new Pose(), mActivitySpace), poseRight);
    }

    @Test
    public void getActivitySpaceScale_returnsInverseOfActivitySpaceWorldScale() throws Exception {
        float activitySpaceScale = 5f;
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromScale(activitySpaceScale));
        CameraViewScenePoseImpl cameraScenePoseLeft =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE);

        assertVector3(
                cameraScenePoseLeft.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));

        CameraViewScenePoseImpl cameraScenePoseRight =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_RIGHT_EYE);

        assertVector3(
                cameraScenePoseRight.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));
    }

    @Test
    public void getDisplayResolutionInPixels_returnsCorrectResolution() {
        CameraViewScenePoseImpl cameraScenePose =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE);

        Activity mockActivity = Mockito.mock(Activity.class);
        when(mPerceptionLibrary.getActivity()).thenReturn(mockActivity);
        WindowManager mockWindowManager = Mockito.mock(WindowManager.class);
        Display mockDisplay = Mockito.mock(Display.class);
        when(mockActivity.getSystemService(WindowManager.class)).thenReturn(mockWindowManager);
        when(mockWindowManager.getDefaultDisplay()).thenReturn(mockDisplay);

        final int expectedDisplayWidth = 2560;
        final int expectedDisplayHeight = 1440;

        Mockito.doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    DisplayMetrics metrics = invocation.getArgument(0);
                                    metrics.widthPixels = expectedDisplayWidth;
                                    metrics.heightPixels = expectedDisplayHeight;
                                    return null;
                                })
                .when(mockDisplay)
                .getRealMetrics(Mockito.any(DisplayMetrics.class));

        PixelDimensions resolution = cameraScenePose.getDisplayResolutionInPixels();

        // The implementation divides width by 2 for single eye resolution
        assertThat(resolution.width).isEqualTo(expectedDisplayWidth / 2);
        assertThat(resolution.height).isEqualTo(expectedDisplayHeight);
    }

    @Test
    public void getDisplayResolutionInPixels_nullWindowManager_returnsZeroDimensions() {
        CameraViewScenePoseImpl cameraScenePose =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE);

        Activity mockActivity = Mockito.mock(Activity.class);
        when(mPerceptionLibrary.getActivity()).thenReturn(mockActivity);
        when(mockActivity.getSystemService(WindowManager.class)).thenReturn(null);

        PixelDimensions resolution = cameraScenePose.getDisplayResolutionInPixels();

        assertThat(resolution.width).isEqualTo(0);
        assertThat(resolution.height).isEqualTo(0);
    }

    @Test
    public void getDisplayResolutionInPixels_nullDisplay_returnsZeroDimensions() {
        CameraViewScenePoseImpl cameraScenePose =
                createCameraViewScenePose(CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE);

        Activity mockActivity = Mockito.mock(Activity.class);
        when(mPerceptionLibrary.getActivity()).thenReturn(mockActivity);
        WindowManager mockWindowManager = Mockito.mock(WindowManager.class);
        when(mockActivity.getSystemService(WindowManager.class)).thenReturn(mockWindowManager);
        when(mockWindowManager.getDefaultDisplay()).thenReturn(null);

        PixelDimensions resolution = cameraScenePose.getDisplayResolutionInPixels();

        assertThat(resolution.width).isEqualTo(0);
        assertThat(resolution.height).isEqualTo(0);
    }
}
