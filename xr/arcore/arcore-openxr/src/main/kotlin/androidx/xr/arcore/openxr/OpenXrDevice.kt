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

package androidx.xr.arcore.openxr

import androidx.xr.arcore.runtime.ArDevice
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose

/**
 * Wraps the device tracking data.
 *
 * @property devicePose the [Pose] of the device
 */
internal class OpenXrDevice() : ArDevice, Updatable {

    override var devicePose: Pose = Pose()
        private set

    override var trackingState: TrackingState = TrackingState.STOPPED
        private set

    /**
     * Updates the entity retrieving its state at [xrTime].
     *
     * @param xrTime the number of nanoseconds since the start of the OpenXR epoch
     */
    override fun update(xrTime: Long) {
        val deviceState = nativeGetDeviceState(xrTime)

        devicePose = deviceState.pose ?: devicePose
        trackingState = deviceState.trackingState
    }

    private external fun nativeGetDeviceState(monotonicTimeNs: Long): DeviceState
}
