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
@file:Suppress("DEPRECATION")

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor as RuntimeAnchor
import androidx.xr.arcore.runtime.Plane as RuntimePlane
import androidx.xr.arcore.runtime.Plane.Label
import androidx.xr.arcore.runtime.Plane.Type
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2

// TODO b/500091606 Remove when no longer used in G3
/**
 * Fake implementation of [Plane][RuntimePlane]. This should not be used to unit test `Plane` APIs.
 * Instead, use an [ArCoreTestRule]. Example:
 * ```
 * @Rule @JvmField val arCoreTestRule = ArCoreTestRule()
 *
 * @Test
 * fun update_trackingStateMatchesTestPlaneVisibility() = runTest(testDispatcher) {
 *     val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
 *     arCoreTestRule.addTrackables(testPlane)
 *     advanceUntilIdle()
 *     var underTest = emptyList<Plane>()
 *     testScope.launch(start = CoroutineStart.UNDISPATCHED) {
 *         Plane.subscribe(session).collect { underTest = it.toList() }
 *     }
 *     advanceUntilIdle()
 *     assertThat(underTest.single().state.value.trackingState).isEqualTo(TrackingState.TRACKING)
 * }
 * ```
 *
 * @property anchors list of the [FakeRuntimeAnchors][FakeRuntimeAnchor] that are attached to the
 *   plane
 * @deprecated This will be removed in a future release. In order to test androidx.xr.arcore APIs,
 *   use an [ArCoreTestRule] in your tests.
 */
@SuppressWarnings("HiddenSuperclass")
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
@Suppress("DEPRECATION")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FakeRuntimePlane(
    override val type: Type = RuntimePlane.Type.HORIZONTAL_UPWARD_FACING,
    override val label: Label = RuntimePlane.Label.FLOOR,
    override var trackingState: TrackingState = TrackingState.TRACKING,
    override var centerPose: Pose = Pose(),
    override var extents: FloatSize2d = FloatSize2d(),
    override var vertices: List<Vector2> = emptyList(),
    override var subsumedBy: RuntimePlane? = null,
    public val anchors: MutableCollection<RuntimeAnchor> = mutableListOf(),
) : RuntimePlane, AnchorHolder {

    /** Creates a new [FakeRuntimeAnchor] and adds it to [anchors]. */
    @Suppress("DEPRECATION")
    override fun createAnchor(pose: Pose): RuntimeAnchor {
        val anchor = FakeRuntimeAnchor(centerPose.compose(pose), this)
        anchors.add(anchor)
        return anchor
    }

    /** Removes the given [anchor] from [anchors]. */
    override fun detachAnchor(anchor: RuntimeAnchor) {
        anchors.remove(anchor)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY) override fun onAnchorPersisted(anchor: RuntimeAnchor) {}
}
