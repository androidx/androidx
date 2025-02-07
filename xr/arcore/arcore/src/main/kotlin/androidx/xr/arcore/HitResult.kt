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

package androidx.xr.arcore

import androidx.xr.runtime.math.Pose

/**
 * Defines an intersection between a ray and estimated real-world geometry.
 *
 * Can be obtained from [Interaction.hitTest].
 *
 * @property distance the distance from the camera to the hit location, in meters.
 * @property hitPose the [Pose] of the intersection between a ray and the [Trackable].
 * @property trackable the [Trackable] that was hit.
 */
public class HitResult
internal constructor(
    public val distance: Float,
    public val hitPose: Pose,
    public val trackable: Trackable<Trackable.State>,
) {
    public fun createAnchor(): AnchorCreateResult {
        return trackable.createAnchor(hitPose)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HitResult) return false

        if (distance != other.distance) return false
        if (hitPose != other.hitPose) return false
        if (trackable != other.trackable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = distance.hashCode()
        result = 31 * result + hitPose.hashCode()
        result = 31 * result + trackable.hashCode()
        return result
    }
}
