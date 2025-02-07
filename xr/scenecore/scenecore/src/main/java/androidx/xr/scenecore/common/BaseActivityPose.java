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

package androidx.xr.scenecore.common;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivityPose;

/**
 * Base implementation of JXRCore ActivityPose.
 *
 * <p>A ActivityPose is an object that has a pose in the world space.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public abstract class BaseActivityPose implements ActivityPose {
    @Override
    @NonNull
    public Pose getActivitySpacePose() {
        throw new UnsupportedOperationException(
                "getActivitySpacePose is not implemented for this ActivityPose.");
    }

    /** Returns the pose for this entity, relative to the activity space root. */
    @NonNull
    public Pose getPoseInActivitySpace() {
        throw new UnsupportedOperationException(
                "getPoseInActivitySpace is not implemented for this ActivityPose.");
    }

    @Override
    @NonNull
    public Vector3 getWorldSpaceScale() {
        return new Vector3(1f, 1f, 1f);
    }

    @Override
    @NonNull
    public Vector3 getActivitySpaceScale() {
        throw new UnsupportedOperationException(
                "getActivitySpaceScale is not implemented for this ActivityPose.");
    }

    @Override
    @NonNull
    public Pose transformPoseTo(@NonNull Pose pose, @NonNull ActivityPose destination) {

        // TODO: b/355680575 - Revisit if we need to account for parent rotation when calculating
        // the
        // scale. This code might produce unexpected results when non-uniform scale is involved in
        // the
        // parent-child entity hierarchy.

        // Compute the inverse scale of the destination entity in the activity space.
        BaseActivityPose baseDestination = (BaseActivityPose) destination;
        Vector3 destinationScale = baseDestination.getActivitySpaceScale();
        Vector3 inverseDestinationScale =
                new Vector3(
                        1f / destinationScale.getX(),
                        1f / destinationScale.getY(),
                        1f / destinationScale.getZ());

        // Compute the transformation to the destination entity from this local entity.
        Pose activityToLocal = this.getPoseInActivitySpace();
        Pose activityToDestination = baseDestination.getPoseInActivitySpace();
        Pose destinationToActivity =
                new Pose(
                                activityToDestination
                                        .getTranslation()
                                        .times(inverseDestinationScale),
                                activityToDestination.getRotation())
                        .getInverse();
        Pose destinationToLocal =
                destinationToActivity.compose(
                        new Pose(
                                activityToLocal.getTranslation().times(inverseDestinationScale),
                                activityToLocal.getRotation()));

        // Apply the transformation to the destination entity, from this entity, on the local pose.
        return destinationToLocal.compose(
                new Pose(
                        pose.getTranslation()
                                .times(this.getActivitySpaceScale().times(inverseDestinationScale)),
                        pose.getRotation()));
    }
}
