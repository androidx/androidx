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
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose;

import org.jspecify.annotations.NonNull;

/** A ScenePose representing the origin of the OpenXR reference space. */
final class PerceptionSpaceScenePoseImpl extends BaseScenePose implements PerceptionSpaceScenePose {
    private final OpenXrScenePoseHelper mOpenXrScenePoseHelper;

    PerceptionSpaceScenePoseImpl(
            ActivitySpaceImpl activitySpace, AndroidXrEntity activitySpaceRoot) {
        mOpenXrScenePoseHelper = new OpenXrScenePoseHelper(activitySpace, activitySpaceRoot);
    }

    @Override
    public @NonNull Pose getPoseInActivitySpace() {
        return mOpenXrScenePoseHelper.getPoseInActivitySpace(new Pose());
    }

    @Override
    public @NonNull Pose getActivitySpacePose() {
        return mOpenXrScenePoseHelper.getActivitySpacePose(new Pose());
    }

    @Override
    // TODO: b/378680989 - Remove getPoseInActivitySpace from Impl.
    public @NonNull Vector3 getActivitySpaceScale() {
        // This ScenePose is assumed to always have a scale of 1.0f in the OpenXR reference
        // space.
        return mOpenXrScenePoseHelper.getActivitySpaceScale(new Vector3(1f, 1f, 1f));
    }
}
