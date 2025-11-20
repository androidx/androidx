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

package androidx.xr.arcore.testing

import androidx.xr.arcore.runtime.Hand as RuntimeHand
import androidx.xr.runtime.TrackingState
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/** Test-only implementation of [androidx.xr.arcore.runtime.Hand]. */
public class FakeRuntimeHand(
    override var trackingState: TrackingState = TrackingState.PAUSED,
    override var handJointsBuffer: FloatBuffer = ByteBuffer.allocate(0).asFloatBuffer(),
) : RuntimeHand {
    public companion object {}
}
