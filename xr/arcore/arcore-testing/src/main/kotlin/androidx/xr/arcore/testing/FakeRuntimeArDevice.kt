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

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.ArDevice as RuntimeArDevice
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose

// TODO b/500091606 Remove when no longer used in G3
/**
 * Fake implementation of [ArDevice][RuntimeArDevice] for testing purposes. This should not be used
 * to unit test `ArDevice` APIs. Instead, use an [ArCoreTestRule]. Example:
 * ```
 * @Rule @JvmField val arCoreTestRule = ArCoreTestRule()
 *
 * @Test
 * fun pose_tracksTranslation() = runTest(testDispatcher) {
 *     val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
 *     arCoreTestRule.device.pose = expectedPose
 *     advanceUntilIdle()
 *     val underTest = ArDevice.getInstance(session)
 *     assertThat(underTest.state.value.devicePose.translation)
 *         .isEqualTo(expectedPose.translation)
 * }
 * ```
 *
 * @deprecated This will be removed in a future release. In order to test androidx.xr.arcore APIs,
 *   use an [ArCoreTestRule] in your tests.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
public class FakeRuntimeArDevice(
    override var devicePose: Pose = Pose(),
    override var trackingState: TrackingState = TrackingState.STOPPED,
) : RuntimeArDevice {

    public companion object {}
}
