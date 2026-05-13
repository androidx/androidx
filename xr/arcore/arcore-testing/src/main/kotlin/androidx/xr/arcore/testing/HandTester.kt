/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore.testing

import androidx.xr.arcore.HandJointType
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.internal.FakePerceptionRuntime
import androidx.xr.arcore.testing.internal.FakeRuntimeHand
import androidx.xr.arcore.testing.internal.FakeRuntimeHand.Companion.bufferSize
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.math.Pose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.iterator

/**
 * An object which allows for testing a simulation of one of the user's hands in an ARCore unit test
 * environment.
 *
 * @property isVisible whether the hand is currently in view of the runtime
 * @property handJointMap a [Map] of [HandJointType] to that joint's [Pose]
 */
public class HandTester
internal constructor(
    private val arCoreTestRule: ArCoreTestRule,
    private val fakeRuntimeHand: FakeRuntimeHand,
) {

    private val _handJointMap: MutableMap<HandJointType, Pose> =
        HandJointType.entries.associateWith { Pose() }.toMutableMap()

    public var isVisible: Boolean = false
        set(value) {
            field = value
            if (arCoreTestRule.runtime.config.handTracking != HandTrackingMode.DISABLED) {
                fakeRuntimeHand.trackingState =
                    if (value) {
                        TrackingState.TRACKING
                    } else {
                        TrackingState.PAUSED
                    }
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var handJointMap: Map<HandJointType, Pose>
        get() = _handJointMap.toMap()
        set(value) {
            for ((joint, pose) in value) {
                _handJointMap[joint] = pose
            }
            if (arCoreTestRule.runtime.config.handTracking != HandTrackingMode.DISABLED) {
                val buffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.nativeOrder())

                HandJointType.entries.forEach { handJointType ->
                    val handJointPose = handJointMap[handJointType]!!
                    buffer.putFloat(handJointPose.rotation.x)
                    buffer.putFloat(handJointPose.rotation.y)
                    buffer.putFloat(handJointPose.rotation.z)
                    buffer.putFloat(handJointPose.rotation.w)
                    buffer.putFloat(handJointPose.translation.x)
                    buffer.putFloat(handJointPose.translation.y)
                    buffer.putFloat(handJointPose.translation.z)
                }

                buffer.flip()
                fakeRuntimeHand.handJointsBuffer = buffer.asFloatBuffer()
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }
}
