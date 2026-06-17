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

/**
 * A helper class for converting poses from the underlying platform reference space pose to a pose
 * in the activity space or world space.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PlatformReferenceScenePoseHelper(private val activitySpace: ActivitySpace) {
    /**
     * Returns the pose relative to the activity space by transforming with the reference space. If
     * there is an error retrieving the reference space, this will return the identity pose.
     */
    public fun getActivitySpacePose(platformReferenceToPose: Pose?): Pose {
        // The ScenePose should have unit scale (1.0f, 1.0f, 1.0f) and it should have no
        // direct parent, but the activity space can have a non-unit scale.
        // However, platformReferenceToActivitySpace does not have the scale applied to it so we
        // need to apply  the scale from ActivitySpace to the platform reference pose to properly
        // compute values in scaled  space.
        val platformReferenceToActivitySpace = activitySpace.poseInPlatformReferenceSpace
        // TODO: b/353575470 throw an exception here instead of returning identity pose.
        if (platformReferenceToActivitySpace == null || platformReferenceToPose == null) {
            // TODO: b/437878722 Only remove log. Should throw exception, but need update unit tests
            return Pose()
        }

        val activitySpaceToPlatformReference = platformReferenceToActivitySpace.inverse
        val scaledActivitySpaceToPlatformReference =
            activitySpaceToPlatformReference.copy(
                activitySpaceToPlatformReference.translation.scale(
                    activitySpace.worldSpaceScale.inverse()
                )
            )
        // Apply the inverse of the ActivitySpace scale to the platform reference pose.
        val scaledPlatformReferenceToPose =
            Pose(
                platformReferenceToPose.translation.scale(activitySpace.worldSpaceScale.inverse()),
                platformReferenceToPose.rotation,
            )
        return scaledActivitySpaceToPlatformReference.compose(scaledPlatformReferenceToPose)
    }

    /** Returns the scale of the WorldPose with respect to the activity space. */
    public fun getActivitySpaceScale(platformReferenceScale: Vector3): Vector3 {
        return platformReferenceScale.scale(activitySpace.worldSpaceScale.inverse())
    }
}
