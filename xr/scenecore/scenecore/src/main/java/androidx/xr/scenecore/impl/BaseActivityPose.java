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

package androidx.xr.scenecore.impl;

import androidx.xr.runtime.internal.ActivityPose;
import androidx.xr.runtime.internal.HitTestResult;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

/**
 * Base implementation of JXRCore ActivityPose.
 *
 * <p>A ActivityPose is an object that has a pose in the world space.
 */
abstract class BaseActivityPose implements ActivityPose {
    @Override
    public @NonNull Pose getActivitySpacePose() {
        throw new UnsupportedOperationException(
                "getActivitySpacePose is not implemented for this ActivityPose.");
    }

    /** Returns the pose for this entity, relative to the activity space root. */
    public @NonNull Pose getPoseInActivitySpace() {
        throw new UnsupportedOperationException(
                "getPoseInActivitySpace is not implemented for this ActivityPose.");
    }

    @Override
    public @NonNull Vector3 getWorldSpaceScale() {
        return new Vector3(1f, 1f, 1f);
    }

    @Override
    public @NonNull Vector3 getActivitySpaceScale() {
        throw new UnsupportedOperationException(
                "getActivitySpaceScale is not implemented for this ActivityPose.");
    }

    @Override
    public @NonNull ListenableFuture<HitTestResult> hitTest(
            @NonNull Vector3 origin,
            @NonNull Vector3 direction,
            @HitTestFilterValue int hitTestFilter) {
        throw new UnsupportedOperationException(
                "hitTest is not implemented for this ActivityPose.");
    }

    @Override
    public @NonNull Pose transformPoseTo(@NonNull Pose pose, @NonNull ActivityPose destination) {

        // This code might produce unexpected results when non-uniform scale
        // is involved in the parent-child entity hierarchy.

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
