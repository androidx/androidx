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

import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.xr.runtime.internal.CameraViewActivityPose;
import androidx.xr.runtime.internal.HitTestResult;
import androidx.xr.runtime.internal.PixelDimensions;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.impl.perception.ViewProjection;
import androidx.xr.scenecore.impl.perception.ViewProjections;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A ActivityPose representing a user's camera. This can be used to determine the location and field
 * of view of the camera.
 */
final class CameraViewActivityPoseImpl extends BaseActivityPose implements CameraViewActivityPose {
    private static final String TAG = "CameraViewActivityPose";
    private final PerceptionLibrary mPerceptionLibrary;
    @CameraType private final int mCameraType;
    private final ActivitySpaceImpl mActivitySpace;
    private final OpenXrActivityPoseHelper mOpenXrActivityPoseHelper;
    // Default the pose to null. A null pose indicates that the camera is not ready yet.
    private Pose mLastOpenXrPose = null;

    CameraViewActivityPoseImpl(
            @CameraType int cameraType,
            ActivitySpaceImpl activitySpace,
            AndroidXrEntity activitySpaceRoot,
            PerceptionLibrary perceptionLibrary) {
        mCameraType = cameraType;
        mPerceptionLibrary = perceptionLibrary;
        mActivitySpace = activitySpace;
        mOpenXrActivityPoseHelper = new OpenXrActivityPoseHelper(activitySpace, activitySpaceRoot);
    }

    @Override
    public Pose getPoseInActivitySpace() {
        return mOpenXrActivityPoseHelper.getPoseInActivitySpace(getPoseInOpenXrReferenceSpace());
    }

    @Override
    public @NonNull Pose getActivitySpacePose() {
        return mOpenXrActivityPoseHelper.getActivitySpacePose(getPoseInOpenXrReferenceSpace());
    }

    @Override
    public @NonNull Vector3 getActivitySpaceScale() {
        // This WorldPose is assumed to always have a scale of 1.0f in the OpenXR reference space.
        return mOpenXrActivityPoseHelper.getActivitySpaceScale(new Vector3(1f, 1f, 1f));
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
            Log.w(TAG, "Cannot retrieve the camera pose with a null perception session.");
            return null;
        }
        ViewProjections perceptionViews = session.getStereoViews();
        if (perceptionViews == null) {
            Log.e(TAG, "Error retrieving the camera.");
            return null;
        }
        if (mCameraType == CameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE) {
            return perceptionViews.getLeftEye();
        } else if (mCameraType == CameraViewActivityPose.CameraType.CAMERA_TYPE_RIGHT_EYE) {
            return perceptionViews.getRightEye();
        } else {
            Log.w(TAG, "Unsupported camera type: " + mCameraType);
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

    @Override
    public @NonNull PixelDimensions getDisplayResolutionInPixels() {
        Activity activity = mPerceptionLibrary.getActivity();
        WindowManager windowManager = activity.getSystemService(WindowManager.class);
        if (windowManager == null) {
            Log.w(
                    TAG,
                    "WindowManager not available, cannot get display resolution. Returning (0,"
                            + "0).");
            return new PixelDimensions(0, 0); // Fallback if WindowManager is not available
        }

        Display display = windowManager.getDefaultDisplay();
        if (display == null) {
            Log.w(
                    TAG,
                    "Default display not available, cannot get display resolution. Returning "
                            + "(0,0).");
            return new PixelDimensions(0, 0); // Fallback if display is not available
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);

        // Divide the width by 2 because we want single eye resolution, not full display resolution
        return new PixelDimensions(displayMetrics.widthPixels / 2, displayMetrics.heightPixels);
    }
}
