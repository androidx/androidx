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
import androidx.xr.arcore.runtime.AnchorNotTrackingException
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import java.util.UUID

// TODO b/500091606 Remove when no longer used in G3
/**
 * Fake implementation of [Anchor][RuntimeAnchor] for testing purposes. This should not be used to
 * unit test `Anchor` APIs. Instead, use an [ArCoreTestRule]. Example:
 * ```
 * @Rule @JvmField val arCoreTestRule = ArCoreTestRule()
 *
 * @Test
 * fun update_poseMovesWithTrackable() = runTest(testDispatcher) {
 *     val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
 *     arCoreTestRule.addTrackables(testPlane)
 *     advanceUntilIdle()
 *     var trackable: Plane? = null
 *     testScope.launch(start = CoroutineStart.UNDISPATCHED) {
 *         Plane.subscribe(session).collect { trackable = it.first() }
 *     }
 *     advanceUntilIdle()
 *     val initialPose = Pose()
 *     val anchorResult = trackable.createAnchor(initialPose)
 *     val underTest = anchorResult.anchor
 *     assertThat(underTest.state.value.pose).isEqualTo(initialPose)
 * }
 * ```
 *
 * @property isTrackingAvailable a flag to represent available tracking state of the camera when
 *   creating the anchor
 * @property isAttached whether the anchor is attached to an [AnchorHolder]
 * @deprecated This will be removed in a future release. In order to test androidx.xr.arcore APIs,
 *   use an [ArCoreTestRule] in your tests.
 */
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FakeRuntimeAnchor
public constructor(override var pose: Pose, public val isTrackingAvailable: Boolean = true) :
    RuntimeAnchor {

    internal constructor(
        pose: Pose,
        anchorHolder: AnchorHolder? = null,
        isTrackingAvailable: Boolean = true,
    ) : this(pose, isTrackingAvailable) {
        this.anchorHolder = anchorHolder
    }

    init {
        if (!isTrackingAvailable) {
            throw AnchorNotTrackingException()
        }
        ++anchorsCreatedCount
        if (anchorsCreatedCount > ANCHOR_RESOURCE_LIMIT) {
            throw AnchorResourcesExhaustedException()
        }
    }

    internal var anchorHolder: AnchorHolder? = null

    override var trackingState: TrackingState = TrackingState.TRACKING

    override var persistenceState: RuntimeAnchor.PersistenceState =
        RuntimeAnchor.PersistenceState.NOT_PERSISTED

    override var uuid: UUID? = null

    public val isAttached: Boolean
        get() = anchorHolder != null

    /**
     * Generates a random UUID for the anchor and adds it to [FakePerceptionManager.anchorUuids].
     *
     * This function will only be added to the list of anchors returned by
     * [FakePerceptionManager.getPersistedAnchorUuids] if the [anchorHolder] is a
     * [FakePerceptionManager].
     */
    override fun persist() {
        uuid = UUID.randomUUID()
        persistenceState = RuntimeAnchor.PersistenceState.PERSISTED
        anchorHolder?.onAnchorPersisted(this)
    }

    override fun detach() {
        if (anchorHolder != null) {
            anchorHolder?.detachAnchor(this)
            anchorHolder = null
            --anchorsCreatedCount
        }
    }

    public companion object {
        /** Limit for the number of anchors that can be created. */
        @JvmStatic
        public val anchorResourceLimit: Int
            get() = ANCHOR_RESOURCE_LIMIT

        private const val ANCHOR_RESOURCE_LIMIT: Int = 6
        /** The current number of anchors created. */
        @JvmStatic public var anchorsCreatedCount: Int = 0
    }
}
