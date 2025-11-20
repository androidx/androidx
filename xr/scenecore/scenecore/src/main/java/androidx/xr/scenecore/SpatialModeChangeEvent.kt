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

package androidx.xr.scenecore

import androidx.xr.runtime.math.Pose

/**
 * An event that is dispatched when the spatial mode for the scene has changed.
 *
 * @property recommendedPose The recommended pose for the key entity. The pose is relative to
 *   [ActivitySpace] origin, not relative to the key entity's parent.
 * @property recommendedScale The recommended scale for the key entity. The scale value is the
 *   accumulated scale for this entity i.e. accumulated scale in [ActivitySpace], not relative to
 *   the key entity's parent.
 */
public class SpatialModeChangeEvent(
    public val recommendedPose: Pose,
    public val recommendedScale: Float,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialModeChangeEvent) return false
        if (recommendedPose != other.recommendedPose) return false
        if (recommendedScale != other.recommendedScale) return false
        return true
    }

    override fun hashCode(): Int {
        var result = recommendedPose.hashCode()
        result = 31 * result + recommendedScale.hashCode()
        return result
    }

    override fun toString(): String {
        return "SpatialModeChangeEvent(" +
            "recommendedPose=$recommendedPose, " +
            "recommendedScale=$recommendedScale" +
            ")"
    }
}
