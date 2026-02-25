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

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Eye as Eye
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose

/**
 * Wraps OpenXR eye tracking data with the [Eye] interface.
 *
 * @property isOpen whether the eye is open
 * @property pose the [Pose] of the eye
 * @property trackingState the [TrackingState] of the eye
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrEye() : Eye {
    override var isOpen: Boolean = false
        private set

    override var pose: Pose = Pose()
        private set

    override var trackingState: TrackingState = TrackingState.PAUSED
        private set

    internal fun update(data: EyeData) {
        when (data.state) {
            EyeStatus.GAZING -> {
                isOpen = true
                trackingState = TrackingState.TRACKING
                pose = data.pose
            }
            EyeStatus.SHUT -> {
                isOpen = false
                trackingState = TrackingState.TRACKING
                pose = data.pose
            }
            EyeStatus.INVALID -> {
                isOpen = false
                trackingState = TrackingState.PAUSED
                pose = Pose()
            }
        }
        pose = data.pose
    }
}
