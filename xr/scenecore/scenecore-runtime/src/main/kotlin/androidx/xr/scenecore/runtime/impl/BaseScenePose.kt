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

package androidx.xr.scenecore.runtime.impl

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.ScenePose

/**
 * Base implementation of SceneCore ScenePose.
 *
 * <p>A ScenePose is an object that has a pose in the world space.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public abstract class BaseScenePose : ScenePose {
    override val activitySpacePose: Pose
        get() =
            throw UnsupportedOperationException(
                "getActivitySpacePose is not implemented for this ScenePose."
            )

    /** Returns the pose for this entity, relative to the activity space root. */
    public open val poseInActivitySpace: Pose
        get() =
            throw UnsupportedOperationException(
                "getPoseInActivitySpace is not implemented for this ScenePose."
            )

    override val worldSpaceScale: Vector3
        get() = Vector3(1f, 1f, 1f)

    override val activitySpaceScale: Vector3
        get() =
            throw UnsupportedOperationException(
                "getActivitySpaceScale is not implemented for this ScenePose."
            )

    override suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @ScenePose.HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult {
        throw UnsupportedOperationException("hitTest is not implemented for this ScenePose.")
    }

    override fun transformPoseTo(pose: Pose, destination: ScenePose): Pose {
        // This code might produce unexpected results when non-uniform scale
        // is involved in the parent-child entity hierarchy.

        // Compute the inverse scale of the destination entity in the activity space.
        val baseDestination = destination as BaseScenePose
        val destinationScale = baseDestination.activitySpaceScale
        val inverseDestinationScale =
            Vector3(1f / destinationScale.x, 1f / destinationScale.y, 1f / destinationScale.z)

        // Compute the transformation to the destination entity from this local entity.
        val activityToLocal = this.poseInActivitySpace
        val activityToDestination = baseDestination.poseInActivitySpace
        val destinationToActivity =
            Pose(
                    activityToDestination.translation.scale(inverseDestinationScale),
                    activityToDestination.rotation,
                )
                .inverse

        val destinationToLocal =
            destinationToActivity.compose(
                Pose(
                    activityToLocal.translation.scale(inverseDestinationScale),
                    activityToLocal.rotation,
                )
            )

        // Apply the transformation to the destination entity, from this entity, on the local pose.
        return destinationToLocal.compose(
            Pose(
                pose.translation.scale(this.activitySpaceScale.scale(inverseDestinationScale)),
                pose.rotation,
            )
        )
    }

    override fun transformPositionTo(position: Vector3, destination: ScenePose): Vector3 {
        return this.transformPoseTo(Pose(position), destination).translation
    }

    override fun transformVectorTo(vector: Vector3, destination: ScenePose): Vector3 {
        // To isolate rotation and scale, transform two points: the origin and the vector endpoint.
        // The difference between the transformed points yields the transformed vector.

        // Transform the origin point
        val originPointInDest = this.transformPositionTo(Vector3.Zero, destination)

        // Transform the point representing the vector endpoint
        val vectorEndPointInDest = this.transformPositionTo(vector, destination)

        // The transformed vector is the difference between the transformed endpoint and origin.
        return vectorEndPointInDest - originPointInDest
    }

    override fun transformDirectionTo(direction: Vector3, destination: ScenePose): Vector3 {
        // We need to transform the direction vector into the destination space similar to
        // transformVectorTo and then we need to remove the scaling effect
        val transformedVectorInDest = transformVectorTo(direction, destination)

        // Preserve the original magnitude by normalizing and rescaling.
        val originalMagnitude = direction.length
        if (originalMagnitude == 0.0f) {
            return Vector3.Zero
        }
        return transformedVectorInDest.toNormalized() * originalMagnitude
    }
}
