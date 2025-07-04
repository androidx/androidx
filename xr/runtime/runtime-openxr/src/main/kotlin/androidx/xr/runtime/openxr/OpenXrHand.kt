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

import androidx.annotation.RestrictTo
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Hand
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** Wraps the native [XrHandJointLocationsEXT] with the [Hand] interface. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrHand internal constructor(private val isLeftHand: Boolean) : Hand, Updatable {

    override var trackingState: TrackingState = TrackingState.PAUSED
        private set

    override var handJointsBuffer: FloatBuffer = ByteBuffer.allocate(0).asFloatBuffer()
        private set

    override fun update(xrTime: Long) {
        val handDataBuffer = nativeGetHandDataBuffer(isLeftHand, xrTime)
        if (handDataBuffer == null) {
            trackingState = TrackingState.PAUSED
            return
        }

        trackingState =
            if (handDataBuffer.int != 0) TrackingState.TRACKING else TrackingState.PAUSED
        handJointsBuffer = handDataBuffer.slice().order(ByteOrder.nativeOrder()).asFloatBuffer()
    }

    private external fun nativeGetHandDataBuffer(
        isLeftHand: Boolean,
        timestampNs: Long,
    ): ByteBuffer?
}
