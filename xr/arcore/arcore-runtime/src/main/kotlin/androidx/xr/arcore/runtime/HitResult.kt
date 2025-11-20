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

package androidx.xr.arcore.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose

/**
 * Defines an intersection between a ray and estimated real-world geometry.
 *
 * @property distance from the camera to the hit location, in meters.
 * @property hitPose of the intersection between a ray and detected real-world geometry.
 * @property trackable that was hit.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class HitResult(
    public val distance: Float,
    public val hitPose: Pose,
    public val trackable: Trackable,
) {
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
