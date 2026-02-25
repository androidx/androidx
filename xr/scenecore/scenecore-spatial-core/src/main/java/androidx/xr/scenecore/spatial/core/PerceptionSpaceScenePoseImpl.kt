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
package androidx.xr.scenecore.spatial.core

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.impl.BaseScenePose

/** A ScenePose representing the origin of the OpenXR reference space. */
internal class PerceptionSpaceScenePoseImpl(activitySpace: ActivitySpaceImpl) :
    BaseScenePose(), PerceptionSpaceScenePose {
    private val openXrScenePoseHelper: OpenXrScenePoseHelper = OpenXrScenePoseHelper(activitySpace)

    // TODO: b/378680989 - Remove getPoseInActivitySpace from Impl.
    override val poseInActivitySpace: Pose
        get() = openXrScenePoseHelper.getPoseInActivitySpace(Pose())

    override val activitySpacePose: Pose
        get() = openXrScenePoseHelper.getActivitySpacePose(Pose())

    override val activitySpaceScale: Vector3
        // This ScenePose is assumed to always have a scale of 1.0f in the OpenXR reference
        // space.
        get() = openXrScenePoseHelper.getActivitySpaceScale(Vector3(1f, 1f, 1f))
}
