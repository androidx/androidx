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

package androidx.xr.runtime.testing

import androidx.xr.runtime.internal.SpatialModeChangeListener
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

public class FakeSpatialModeChangeListener : SpatialModeChangeListener {
    public var lastRecommendedPose: Pose? = null
    public var lastRecommendedScale: Vector3? = null
    public var updateCount: Int = 0

    override fun onSpatialModeChanged(recommendedPose: Pose, recommendedScale: Vector3) {
        this.lastRecommendedPose = recommendedPose
        this.lastRecommendedScale = recommendedScale
        this.updateCount++
    }

    public fun reset() {
        lastRecommendedPose = null
        lastRecommendedScale = null
        updateCount = 0
    }
}
