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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.HandJointType
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import java.nio.FloatBuffer

/** Describes a hand. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Hand {

    public companion object {
        /**
         * Parses the hand joint data from the buffer.
         *
         * @param trackingState the current [TrackingState] of the hand.
         * @param handJointsBuffer the [ByteBuffer] containing the pose of each joint in the hand.
         * @return a map of [HandJointType] to [Pose] representing the current pose of each joint in
         *   the hand.
         */
        @JvmStatic
        public fun parseHandJoint(
            trackingState: TrackingState,
            handJointsBuffer: FloatBuffer,
        ): Map<HandJointType, Pose> {
            if (trackingState != TrackingState.TRACKING) {
                return emptyMap()
            }
            val buffer = handJointsBuffer.duplicate()
            val jointCount = HandJointType.values().size
            val poses = mutableListOf<Pose>()
            repeat(jointCount) {
                val qx = buffer.get()
                val qy = buffer.get()
                val qz = buffer.get()
                val qw = buffer.get()
                val px = buffer.get()
                val py = buffer.get()
                val pz = buffer.get()
                poses.add(Pose(Vector3(px, py, pz), Quaternion(qx, qy, qz, qw)))
            }
            return HandJointType.values().zip(poses).toMap()
        }
    }

    /** The current [TrackingState] of the hand's data. */
    public val trackingState: TrackingState

    /** The value describing the data of the hand, including trackingState and handJoints' poses. */
    public val handJointsBuffer: FloatBuffer

    /** The value describing the poses of the hand joints. */
    public val handJoints: Map<HandJointType, Pose>
        get() = parseHandJoint(trackingState, handJointsBuffer)
}
