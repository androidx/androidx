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

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Hand as RuntimeHand
import androidx.xr.arcore.runtime.TrackingState
import java.nio.ByteBuffer
import java.nio.FloatBuffer

// TODO b/500091606 Remove when no longer used in G3
/**
 * Fake implementation of [Hand][RuntimeHand] for testing purposes. This should not be used to unit
 * test `Hand` APIs. Instead, use an [ArCoreTestRule]. Example:
 * ```
 * @Rule @JvmField val arCoreTestRule = ArCoreTestRule()
 *
 * @Test
 * fun left_returnsLeftHand() = runTest(testDispatcher) {
 *     arCoreTestRule.leftHand.isVisible = true
 *     advanceUntilIdle()
 *     val leftHand = Hand.left(session)
 *     assertThat(leftHand.state.value.trackingState.toRuntimeTrackingState())
 *         .isEqualTo(TrackingState.TRACKING)
 *     arCoreTestRule.leftHand.isVisible = false
 *     advanceUntilIdle()
 *     assertThat(leftHand.state.value.trackingState.toRuntimeTrackingState())
 *         .isEqualTo(TrackingState.PAUSED)
 * }
 * ```
 *
 * @deprecated This will be removed in a future release. In order to test androidx.xr.arcore APIs,
 *   use an [ArCoreTestRule] in your tests.
 */
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FakeRuntimeHand(
    override var trackingState: TrackingState = TrackingState.PAUSED,
    override var handJointsBuffer: FloatBuffer = ByteBuffer.allocate(0).asFloatBuffer(),
) : RuntimeHand {
    public companion object {}
}
