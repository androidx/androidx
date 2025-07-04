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

import android.util.Log;

import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;

/**
 * A helper class for converting poses from an OpenXR pose to a pose in the activity space or world
 * space.
 */
final class OpenXrActivityPoseHelper {
    private static final String TAG = "OpenXrPoseHelper";
    private final ActivitySpaceImpl mActivitySpace;
    private final AndroidXrEntity mActivitySpaceRoot;

    OpenXrActivityPoseHelper(ActivitySpaceImpl activitySpace, AndroidXrEntity activitySpaceRoot) {
        mActivitySpace = activitySpace;
        mActivitySpaceRoot = activitySpaceRoot;
    }

    /**
     * Returns the pose relative to the activity space by transforming with the OpenXR reference
     * space. If there is an error retrieving the openXR reference space, this will return the
     * identity pose.
     */
    public Pose getPoseInActivitySpace(Pose openXrToPose) {
        if (mActivitySpace == null) {
            Log.e(TAG, "Cannot get pose in Activity Space with a null Activity Space.");
            return new Pose();
        }

        // The ActivityPose should have unit scale (1.0f, 1.0f, 1.0f) and it should have no
        // direct parent, but the activity space can have a non-unit scale.
        // However, openXrToActivitySpace does not have the scale applied to it so we need to apply
        // the scale from ActivitySpace to the OpenXR pose to properly compute values in scaled
        // space.
        final Pose openXrToActivitySpace = mActivitySpace.getPoseInOpenXrReferenceSpace();
        // TODO: b/353575470 throw an exception here instead of returning identity pose.
        if (openXrToActivitySpace == null || openXrToPose == null) {
            Log.e(
                    TAG,
                    "Cannot retrieve pose in underlying space for the ActivityPose. Returning"
                            + " identity pose.");
            return new Pose();
        }

        final Pose activitySpaceToOpenXr = openXrToActivitySpace.getInverse();
        final Pose scaledActivitySpacetoOpenXr =
                activitySpaceToOpenXr.copy(
                        activitySpaceToOpenXr
                                .getTranslation()
                                .div(mActivitySpace.getWorldSpaceScale()));
        // Apply the inverse of the ActivitySpace scale to the OpenXR pose.
        final Pose scaledOpenXrToPose =
                new Pose(
                        openXrToPose.getTranslation().div(mActivitySpace.getWorldSpaceScale()),
                        openXrToPose.getRotation());
        return scaledActivitySpacetoOpenXr.compose(scaledOpenXrToPose);
    }

    /** Returns the ActivityPose's pose in the activity space. */
    public Pose getActivitySpacePose(Pose openXrToPose) {
        if (mActivitySpaceRoot == null) {
            Log.e(TAG, "Cannot get pose in World Space Pose with a null World Space Entity.");
            return new Pose();
        }

        // ActivitySpace and the nodeless entity have unit scale and the nodeless entity has no
        // direct
        // parent so we can just compose the two poses without scaling.
        final Pose activitySpaceToPose = this.getPoseInActivitySpace(openXrToPose);
        final Pose worldSpaceToActivitySpace =
                mActivitySpaceRoot.getPoseInActivitySpace().getInverse();
        return worldSpaceToActivitySpace.compose(activitySpaceToPose);
    }

    /** Returns the scale of the WorldPose with respect to the activity space. */
    public Vector3 getActivitySpaceScale(Vector3 openXrScale) {
        if (mActivitySpace == null) {
            Log.e(TAG, "Cannot get scale in Activity Space with a null Activity Space Entity.");
            return new Vector3(1f, 1f, 1f);
        }
        return openXrScale.div(mActivitySpace.getWorldSpaceScale());
    }
}
