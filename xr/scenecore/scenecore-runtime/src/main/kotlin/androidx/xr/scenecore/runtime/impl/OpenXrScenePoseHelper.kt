/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.runtime.impl

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.ActivitySpace

// TODO: b/496641320 - Rename the class to something more generic like "ReferenceScenePoseHelper"
/**
 * A helper class for converting poses from an OpenXR pose to a pose in the activity space or world
 * space.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OpenXrScenePoseHelper(private val activitySpace: ActivitySpace) {
    /**
     * Returns the pose relative to the activity space by transforming with the OpenXR reference
     * space. If there is an error retrieving the openXR reference space, this will return the
     * identity pose.
     */
    public fun getActivitySpacePose(openXrToPose: Pose?): Pose {
        // The ScenePose should have unit scale (1.0f, 1.0f, 1.0f) and it should have no
        // direct parent, but the activity space can have a non-unit scale.
        // However, openXrToActivitySpace does not have the scale applied to it so we need to apply
        // the scale from ActivitySpace to the OpenXR pose to properly compute values in scaled
        // space.
        val openXrToActivitySpace = activitySpace.poseInOpenXrReferenceSpace
        // TODO: b/353575470 throw an exception here instead of returning identity pose.
        if (openXrToActivitySpace == null || openXrToPose == null) {
            // TODO: b/437878722 Only remove log. Should throw exception, but need update unit tests
            return Pose()
        }

        val activitySpaceToOpenXr = openXrToActivitySpace.inverse
        val scaledActivitySpaceToOpenXr =
            activitySpaceToOpenXr.copy(
                activitySpaceToOpenXr.translation.scale(activitySpace.worldSpaceScale.inverse())
            )
        // Apply the inverse of the ActivitySpace scale to the OpenXR pose.
        val scaledOpenXrToPose =
            Pose(
                openXrToPose.translation.scale(activitySpace.worldSpaceScale.inverse()),
                openXrToPose.rotation,
            )
        return scaledActivitySpaceToOpenXr.compose(scaledOpenXrToPose)
    }

    /** Returns the scale of the WorldPose with respect to the activity space. */
    public fun getActivitySpaceScale(openXrScale: Vector3): Vector3 {
        return openXrScale.scale(activitySpace.worldSpaceScale.inverse())
    }
}
