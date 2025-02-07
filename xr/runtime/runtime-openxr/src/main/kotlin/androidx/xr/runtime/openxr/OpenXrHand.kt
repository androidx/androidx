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

package androidx.xr.runtime.openxr

import androidx.xr.runtime.internal.Hand
import androidx.xr.runtime.internal.HandJointType
import androidx.xr.runtime.math.Pose

/** Wraps the native [XrHandJointLocationsEXT] with the [Hand] interface. */
public class OpenXrHand internal constructor(private val isLeftHand: Boolean) : Hand, Updatable {

    override var isActive: Boolean = false
        private set

    override var handJoints: Map<HandJointType, Pose> = emptyMap()
        private set

    override fun update(xrTime: Long) {
        val handState: HandState =
            nativeLocateHandJoints(isLeftHand, xrTime)
                ?: throw IllegalStateException("Could not locate hand joints for hand.")

        isActive = handState.isActive
        handJoints = HandJointType.values().zip(handState.handJoints).toMap()
    }

    private external fun nativeLocateHandJoints(isLeftHand: Boolean, timestampNs: Long): HandState?
}
