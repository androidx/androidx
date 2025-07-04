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

import androidx.xr.runtime.internal.PerceptionSpaceActivityPose;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;

import org.jspecify.annotations.NonNull;

/** A ActivityPose representing the origin of the OpenXR reference space. */
final class PerceptionSpaceActivityPoseImpl extends BaseActivityPose
        implements PerceptionSpaceActivityPose {

    private final OpenXrActivityPoseHelper mOpenXrActivityPoseHelper;

    PerceptionSpaceActivityPoseImpl(
            ActivitySpaceImpl activitySpace, AndroidXrEntity activitySpaceRoot) {
        mOpenXrActivityPoseHelper = new OpenXrActivityPoseHelper(activitySpace, activitySpaceRoot);
    }

    @Override
    public Pose getPoseInActivitySpace() {
        return mOpenXrActivityPoseHelper.getPoseInActivitySpace(new Pose());
    }

    @Override
    public @NonNull Pose getActivitySpacePose() {
        return mOpenXrActivityPoseHelper.getActivitySpacePose(new Pose());
    }

    @Override
    public @NonNull Vector3 getActivitySpaceScale() {
        // This ActivityPose is assumed to always have a scale of 1.0f in the OpenXR reference
        // space.
        return mOpenXrActivityPoseHelper.getActivitySpaceScale(new Vector3(1f, 1f, 1f));
    }
}
