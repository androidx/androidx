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
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.impl.BaseScenePose

internal class OpenXrScenePose(
    private val activitySpace: ActivitySpaceImpl,
    private val perceptionPose: Pose?,
) : BaseScenePose() {

    private val openXrScenePoseHelper: OpenXrScenePoseHelper = OpenXrScenePoseHelper(activitySpace)

    override val poseInActivitySpace: Pose
        get() = openXrScenePoseHelper.getPoseInActivitySpace(poseInOpenXrReferenceSpace)

    override val activitySpacePose: Pose
        get() = openXrScenePoseHelper.getActivitySpacePose(poseInOpenXrReferenceSpace)

    // This WorldPose is assumed to always have a scale of 1.0f in the OpenXR reference space.
    override val activitySpaceScale: Vector3
        get() = openXrScenePoseHelper.getActivitySpaceScale(Vector3(1f, 1f, 1f))

    override suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @ScenePose.HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult =
        activitySpace.hitTestRelativeToActivityPose(origin, direction, hitTestFilter, this)

    /** Returns the pose relative to the OpenXR reference space (may be null if not ready). */
    val poseInOpenXrReferenceSpace: Pose?
        get() = perceptionPose
}
