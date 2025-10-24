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

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.impl.perception.ViewProjection;
import androidx.xr.scenecore.impl.perception.ViewProjections;
import androidx.xr.scenecore.runtime.CameraViewScenePose;
import androidx.xr.scenecore.runtime.HitTestResult;
import androidx.xr.scenecore.runtime.PixelDimensions;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A ScenePose representing a user's camera. This can be used to determine the location and field of
 * view of the camera.
 */
final class CameraViewScenePoseImpl extends BaseScenePose implements CameraViewScenePose {
    private final PerceptionLibrary mPerceptionLibrary;
    @CameraType private final int mCameraType;
    private final ActivitySpaceImpl mActivitySpace;
    private final OpenXrScenePoseHelper mOpenXrScenePoseHelper;
    // Default the pose to null. A null pose indicates that the camera is not ready yet.
    private Pose mLastOpenXrPose = null;

    CameraViewScenePoseImpl(
            @CameraType int cameraType,
            ActivitySpaceImpl activitySpace,
            AndroidXrEntity activitySpaceRoot,
            PerceptionLibrary perceptionLibrary) {
        mCameraType = cameraType;
        mPerceptionLibrary = perceptionLibrary;
        mActivitySpace = activitySpace;
        mOpenXrScenePoseHelper = new OpenXrScenePoseHelper(activitySpace, activitySpaceRoot);
    }

    @Override
    public @NonNull Pose getPoseInActivitySpace() {
        return mOpenXrScenePoseHelper.getPoseInActivitySpace(getPoseInOpenXrReferenceSpace());
    }

    @Override
    public @NonNull Pose getActivitySpacePose() {
        return mOpenXrScenePoseHelper.getActivitySpacePose(getPoseInOpenXrReferenceSpace());
    }

    @Override
    public @NonNull Vector3 getActivitySpaceScale() {
        // This WorldPose is assumed to always have a scale of 1.0f in the OpenXR reference space.
        return mOpenXrScenePoseHelper.getActivitySpaceScale(new Vector3(1f, 1f, 1f));
    }

    @Override
    public @NonNull ListenableFuture<HitTestResult> hitTest(
            @NonNull Vector3 origin,
            @NonNull Vector3 direction,
            @HitTestFilterValue int hitTestFilter) {
        return mActivitySpace.hitTestRelativeToActivityPose(origin, direction, hitTestFilter, this);
    }

    private @Nullable ViewProjection getViewProjection() {
        final Session session = mPerceptionLibrary.getSession();
        if (session == null) {
            // Cannot retrieve the camera pose with a null perception session.
            return null;
        }
        ViewProjections perceptionViews = session.getStereoViews();
        if (perceptionViews == null) {
            // Error retrieving the camera.
            return null;
        }
        if (mCameraType == CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE) {
            return perceptionViews.getLeftEye();
        } else if (mCameraType == CameraViewScenePose.CameraType.CAMERA_TYPE_RIGHT_EYE) {
            return perceptionViews.getRightEye();
        } else {
            // Unsupported camera type: mCameraType
            return null;
        }
    }

    /** Gets the pose in the OpenXR reference space. Can be null if it is not yet ready. */
    public @Nullable Pose getPoseInOpenXrReferenceSpace() {
        ViewProjection viewProjection = getViewProjection();
        if (viewProjection != null) {
            mLastOpenXrPose = RuntimeUtils.fromPerceptionPose(viewProjection.getPose());
        }
        return mLastOpenXrPose;
    }

    @Override
    @CameraType
    public int getCameraType() {
        return mCameraType;
    }

    @Override
    public @NonNull Fov getFov() {
        ViewProjection viewProjection = getViewProjection();
        if (viewProjection == null) {
            return new Fov(0, 0, 0, 0);
        }
        return RuntimeUtils.fovFromPerceptionFov(viewProjection.getFov());
    }

    // Suppress warnings: windowManager's getDefaultDisplay and getRealMetrics.
    @SuppressWarnings("deprecation")
    @Override
    public @NonNull PixelDimensions getDisplayResolutionInPixels() {
        Activity activity = mPerceptionLibrary.getActivity();
        WindowManager windowManager = activity.getSystemService(WindowManager.class);
        if (windowManager == null) {
            // WindowManager not available, cannot get display resolution. Returning (0, 0).
            return new PixelDimensions(0, 0); // Fallback if WindowManager is not available
        }

        Display display = windowManager.getDefaultDisplay();
        if (display == null) {
            // Default display not available, cannot get display resolution. Returning (0,0).
            return new PixelDimensions(0, 0); // Fallback if display is not available
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);

        // Divide the width by 2 because we want single eye resolution, not full display resolution
        return new PixelDimensions(displayMetrics.widthPixels / 2, displayMetrics.heightPixels);
    }
}
