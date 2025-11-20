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

package androidx.xr.arcore.openxr

import androidx.annotation.RestrictTo
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose

/**
 * Represents the current state of a [Hand] instance.
 *
 * According to https://registry.khronos.org/OpenXR/specs/1.0/man/html/XrHandJointEXT.html, the hand
 * joints should match the order defined in the same page as the enum values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class HandState(
    internal val trackingState: TrackingState = TrackingState.PAUSED,
    internal val handJoints: List<Pose> = listOf(),
) {
    init {
        require(trackingState != TrackingState.TRACKING || handJoints.isNotEmpty()) {
            "Hand joints cannot be empty if the hand is being tracked."
        }
    }
}
