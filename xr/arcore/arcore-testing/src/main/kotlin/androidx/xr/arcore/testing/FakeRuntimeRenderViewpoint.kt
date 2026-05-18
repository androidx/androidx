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
import androidx.xr.arcore.runtime.RenderViewpoint
import androidx.xr.runtime.math.FieldOfView
import androidx.xr.runtime.math.Pose

// TODO b/500091606 Remove when no longer used in G3
/**
 * Fake implementation of [RenderViewpoint] for testing purposes. This should not be used to unit
 * test [RenderViewpoint] APIs. Instead, use an [ArCoreTestRule]. Example:
 * ```
 * @Rule @JvmField val arCoreTestRule = ArCoreTestRule()
 *
 * @Test
 * fun left_returnsPose() = runTest(testDispatcher) {
 *     arCoreTestRule.leftRenderViewpoint.pose = EXPECTED_POSE
 *     advanceUntilIdle()
 *     val underTest = RenderViewpoint.left(session)!!
 *     assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_POSE)
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
public class FakeRuntimeRenderViewpoint(
    override var pose: Pose = Pose(),
    override var fieldOfView: FieldOfView = FieldOfView(0f, 0f, 0f, 0f),
) : RenderViewpoint {}
