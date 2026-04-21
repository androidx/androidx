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
import androidx.xr.arcore.runtime.Anchor as RuntimeAnchor
import androidx.xr.arcore.runtime.AugmentedObject as RuntimeObject
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose

// TODO b/500091606 Remove when no longer used in G3
/**
 * Fake implementation of [AugmentedObject][RuntimeObject] for testing purposes. This should not be
 * used to unit test `AugmentedObject` APIs. Instead, use an [ArCoreTestRule]. Example:
 * ```
 * @Rule @JvmField val arCoreTestRule = ArCoreTestRule()
 *
 * @Test
 * fun subscribe_collectReturnsObject() = runTest(testDispatcher) {
 *     val testObject = TestAugmentedObject(AugmentedObjectCategory.KEYBOARD)
 *     arCoreTestRule.addTrackables(testObject)
 *     advanceUntilIdle()
 *     var underTest = emptyList<AugmentedObject>()
 *     testScope.launch(start = CoroutineStart.UNDISPATCHED) {
 *         AugmentedObject.subscribe(session).collect { underTest = it.toList() }
 *     }
 *     advanceUntilIdle()
 *     assertThat(underTest.single().state.value.category).isEqualTo(testObject.category)
 * }
 * ```
 *
 * @property anchors a [MutableCollection] of [Anchors][RuntimeAnchor] attached to this object
 * @deprecated This will be removed in a future release. In order to test androidx.xr.arcore APIs,
 *   use an [ArCoreTestRule] in your tests.
 */
@SuppressWarnings("HiddenSuperclass")
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FakeRuntimeAugmentedObject(
    override var centerPose: Pose = Pose(),
    override var extents: FloatSize3d = FloatSize3d(),
    override var category: AugmentedObjectCategory = AugmentedObjectCategory.KEYBOARD,
    override var trackingState: TrackingState = TrackingState.TRACKING,
    public val anchors: MutableCollection<RuntimeAnchor> = mutableListOf(),
) : RuntimeObject {}
