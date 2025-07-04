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

package androidx.xr.runtime.testing

import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Anchor as RuntimeAnchor
import androidx.xr.runtime.internal.AnchorNotTrackingException
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.math.Pose
import java.util.UUID

/** Test-only implementation of [RuntimeAnchor] */
public class FakeRuntimeAnchor
internal constructor(
    override var pose: Pose,
    internal val anchorHolder: AnchorHolder? = null,
    /** Flag to represent available tracking state of the camera when creating the anchor. */
    public val isTrackingAvailable: Boolean = true,
) : RuntimeAnchor {
    init {
        if (!isTrackingAvailable) {
            throw AnchorNotTrackingException()
        }
        ++anchorsCreatedCount
        if (anchorsCreatedCount > ANCHOR_RESOURCE_LIMIT) {
            throw AnchorResourcesExhaustedException()
        }
    }

    override var trackingState: TrackingState = TrackingState.TRACKING

    override var persistenceState: RuntimeAnchor.PersistenceState =
        RuntimeAnchor.PersistenceState.NOT_PERSISTED

    override var uuid: UUID? = null

    /** Whether the anchor is attached to an [AnchorHolder] */
    public var isAttached: Boolean = anchorHolder != null
        private set

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
            anchorHolder.detachAnchor(this)
            isAttached = false
        }
    }

    public companion object {
        /** Limit for the number of anchors that can be created. */
        public const val ANCHOR_RESOURCE_LIMIT: Int = 5
        /** The current number of anchors created. */
        @JvmStatic public var anchorsCreatedCount: Int = 0
    }
}
