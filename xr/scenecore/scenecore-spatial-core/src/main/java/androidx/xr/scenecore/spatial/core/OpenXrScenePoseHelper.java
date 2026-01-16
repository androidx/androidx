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

import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;

import org.jspecify.annotations.NonNull;

/**
 * A helper class for converting poses from an OpenXR pose to a pose in the activity space or world
 * space.
 */
final class OpenXrScenePoseHelper {
    private final @NonNull ActivitySpaceImpl mActivitySpace;

    OpenXrScenePoseHelper(@NonNull ActivitySpaceImpl activitySpace) {
        mActivitySpace = activitySpace;
    }

    /**
     * Returns the pose relative to the activity space by transforming with the OpenXR reference
     * space. If there is an error retrieving the openXR reference space, this will return the
     * identity pose.
     */
    public Pose getPoseInActivitySpace(Pose openXrToPose) {
        // The ScenePose should have unit scale (1.0f, 1.0f, 1.0f) and it should have no
        // direct parent, but the activity space can have a non-unit scale.
        // However, openXrToActivitySpace does not have the scale applied to it so we need to apply
        // the scale from ActivitySpace to the OpenXR pose to properly compute values in scaled
        // space.
        final Pose openXrToActivitySpace = mActivitySpace.getPoseInOpenXrReferenceSpace();
        // TODO: b/353575470 throw an exception here instead of returning identity pose.
        if (openXrToActivitySpace == null || openXrToPose == null) {
            // TODO: b/437878722 Only remove log. Should throw exception, but need update unit tests
            return new Pose();
        }

        final Pose activitySpaceToOpenXr = openXrToActivitySpace.getInverse();
        final Pose scaledActivitySpaceToOpenXr =
                activitySpaceToOpenXr.copy(
                        activitySpaceToOpenXr
                                .getTranslation()
                                .scale(mActivitySpace.getWorldSpaceScale().inverse()));
        // Apply the inverse of the ActivitySpace scale to the OpenXR pose.
        final Pose scaledOpenXrToPose =
                new Pose(
                        openXrToPose
                                .getTranslation()
                                .scale(mActivitySpace.getWorldSpaceScale().inverse()),
                        openXrToPose.getRotation());
        return scaledActivitySpaceToOpenXr.compose(scaledOpenXrToPose);
    }

    /** Returns the ScenePose's pose in the activity space. */
    public Pose getActivitySpacePose(Pose openXrToPose) {
        // ActivitySpace and the nodeless entity have unit scale and the nodeless entity has no
        // direct parent so we can just compose the two poses without scaling.
        final Pose activitySpaceToPose = this.getPoseInActivitySpace(openXrToPose);
        final Pose worldSpaceToActivitySpace = mActivitySpace.getPoseInActivitySpace().getInverse();
        return worldSpaceToActivitySpace.compose(activitySpaceToPose);
    }

    /** Returns the scale of the WorldPose with respect to the activity space. */
    public Vector3 getActivitySpaceScale(Vector3 openXrScale) {
        return openXrScale.scale(mActivitySpace.getWorldSpaceScale().inverse());
    }
}
