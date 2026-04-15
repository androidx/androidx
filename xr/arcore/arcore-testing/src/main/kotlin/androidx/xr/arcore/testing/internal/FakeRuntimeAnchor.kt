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

package androidx.xr.arcore.testing.internal

import androidx.xr.arcore.runtime.Anchor as RuntimeAnchor
import androidx.xr.arcore.runtime.AnchorNotTrackingException
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import java.util.UUID

internal class FakeRuntimeAnchor(
    override var pose: Pose,
    var anchorHolder: AnchorHolder? = null,
    isTrackingAvailable: Boolean = true,
) : RuntimeAnchor {
    init {
        if (!isTrackingAvailable) {
            throw AnchorNotTrackingException()
        }
        ++anchorsCreatedCount
        if (anchorsCreatedCount > anchorResourceLimit) {
            throw AnchorResourcesExhaustedException()
        }
        FakePerceptionRuntime.allowOneMoreCallToUpdate()
    }

    override var trackingState: TrackingState = TrackingState.TRACKING

    override var persistenceState: RuntimeAnchor.PersistenceState =
        RuntimeAnchor.PersistenceState.NOT_PERSISTED

    override var uuid: UUID? = null

    val isAttached: Boolean
        get() = anchorHolder != null

    override fun persist() {
        uuid = UUID.randomUUID()
        persistenceState = RuntimeAnchor.PersistenceState.PERSISTED
        anchorHolder?.onAnchorPersisted(this)
    }

    override fun detach() {
        if (anchorHolder != null) {
            anchorHolder!!.detachAnchor(this)
            anchorHolder = null
            --anchorsCreatedCount
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RuntimeAnchor) return false
        return pose == other.pose &&
            trackingState == other.trackingState &&
            persistenceState == other.persistenceState &&
            uuid == other.uuid
    }

    override fun hashCode(): Int {
        var result = pose.hashCode()
        result = 31 * result + trackingState.hashCode()
        result = 31 * result + persistenceState.hashCode()
        uuid?.let { result = 31 * result + it.hashCode() }
        return result
    }

    companion object {
        /** Limit for the number of anchors that can be created. */
        @JvmStatic var anchorResourceLimit: Int = 6

        /** The current number of anchors created. */
        @JvmStatic var anchorsCreatedCount: Int = 0
    }
}
