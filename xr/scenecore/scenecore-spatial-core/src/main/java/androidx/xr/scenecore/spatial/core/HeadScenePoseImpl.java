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
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.runtime.HeadScenePose;
import androidx.xr.scenecore.runtime.HitTestResult;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An ScenePose representing the head of the user. This can be used to determine the location of the
 * user's head.
 */
class HeadScenePoseImpl extends BaseScenePose implements HeadScenePose {
    private final PerceptionLibrary mPerceptionLibrary;
    private final ActivitySpaceImpl mActivitySpace;
    private final OpenXrScenePoseHelper mOpenXrScenePoseHelper;
    // Default the pose to null. A null pose indicates that the head is not ready yet.
    private Pose mLastOpenXrPose = null;

    HeadScenePoseImpl(
            ActivitySpaceImpl activitySpace,
            AndroidXrEntity activitySpaceRoot,
            PerceptionLibrary perceptionLibrary) {
        this.mActivitySpace = activitySpace;
        mPerceptionLibrary = perceptionLibrary;
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

    /** Gets the pose in the OpenXR reference space. Can be null if it is not yet ready. */
    public @Nullable Pose getPoseInOpenXrReferenceSpace() {
        final Session session = mPerceptionLibrary.getSession();
        if (session == null) {
            return mLastOpenXrPose;
        }
        androidx.xr.scenecore.impl.perception.Pose perceptionHeadPose = session.getHeadPose();
        if (perceptionHeadPose != null) {
            mLastOpenXrPose = RuntimeUtils.fromPerceptionPose(perceptionHeadPose);
        }
        return mLastOpenXrPose;
    }
}
