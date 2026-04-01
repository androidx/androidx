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

package androidx.xr.arcore.projected

import androidx.xr.arcore.runtime.ArDevice as RuntimeArDevice
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose

/**
 * @property devicePose the [Pose] of the device
 * @property trackingState the [TrackingState] of the device
 */
internal class ProjectedArDevice : RuntimeArDevice {

    override var devicePose: Pose = Pose()
        private set

    override var trackingState: TrackingState = TrackingState.STOPPED
        private set

    internal fun update(trackingState: TrackingState, pose: Pose?) {
        this.trackingState = trackingState

        if (pose != null) {
            devicePose = pose
        }
    }
}
