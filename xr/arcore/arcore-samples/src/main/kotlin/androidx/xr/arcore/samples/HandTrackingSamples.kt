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

package androidx.xr.arcore.samples

import androidx.annotation.Sampled
import androidx.lifecycle.Lifecycle
import androidx.xr.arcore.Hand
import androidx.xr.arcore.HandJointType
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Sampled
fun getLeftHand(session: Session, lifecycle: Lifecycle) {
    yourCoroutineScope.launch {
        Hand.left(session)?.state?.collect { leftHandState ->
            // early out since we only care if the hand is actively tracking.
            if (leftHandState.trackingState != TrackingState.TRACKING) return@collect

            val joints = leftHandState.handJoints

            // This function could also just interpret the joint poses to try and detect if the
            // user is making a specific gesture.
            yourRenderHandModelFunction(joints, isLeftHand = true)
        }
    }
}

@Sampled
fun getRightHand(session: Session, lifecycle: Lifecycle) {
    yourCoroutineScope.launch {
        Hand.right(session)?.state?.collect { rightHandState ->
            // early out since we only care if the hand is actively tracking.
            if (rightHandState.trackingState != TrackingState.TRACKING) return@collect

            val joints = rightHandState.handJoints

            // This function could also just interpret the joint poses to try and detect if the
            // user is making a specific gesture.
            yourRenderHandModelFunction(joints, isLeftHand = false)
        }
    }
}

private fun yourRenderHandModelFunction(joints: Map<HandJointType, Pose>, isLeftHand: Boolean) {}

private val yourCoroutineScope = MainScope()
