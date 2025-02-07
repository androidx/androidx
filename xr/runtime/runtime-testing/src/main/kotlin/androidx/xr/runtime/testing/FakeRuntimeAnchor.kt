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

import androidx.xr.runtime.internal.Anchor as RuntimeAnchor
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.internal.TrackingState
import androidx.xr.runtime.math.Pose
import java.util.UUID

/** Test-only implementation of [RuntimeAnchor] */
public class FakeRuntimeAnchor(
    override var pose: Pose,
    public val anchorHolder: AnchorHolder? = null,
) : RuntimeAnchor {
    init {
        ++anchorsCreated
        if (anchorsCreated > ANCHOR_RESOURCE_LIMIT) {
            throw AnchorResourcesExhaustedException()
        }
    }

    override var trackingState: TrackingState = TrackingState.Tracking

    override var persistenceState: RuntimeAnchor.PersistenceState =
        RuntimeAnchor.PersistenceState.NotPersisted

    override var uuid: UUID? = null

    /** Whether the anchor is attached to an [AnchorHolder] */
    public var isAttached: Boolean = anchorHolder != null
        private set

    override fun persist() {
        uuid = UUID.randomUUID()
        persistenceState = RuntimeAnchor.PersistenceState.Persisted
        anchorHolder?.persistAnchor(this)
    }

    override fun detach() {
        if (anchorHolder != null) {
            anchorHolder.detachAnchor(this)
            isAttached = false
        }
    }

    public companion object {
        public const val ANCHOR_RESOURCE_LIMIT: Int = 5
        public var anchorsCreated: Int = 0
    }
}
