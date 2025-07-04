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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import java.util.UUID

/** Describes a fixed location and orientation in the real world. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Anchor {

    /** Describes the state of persistence for an [Anchor]. */
    public class PersistenceState private constructor(private val value: Int) {
        public companion object {
            /** The anchor has not been requested to be persisted. */
            @JvmField public val NOT_PERSISTED: PersistenceState = PersistenceState(0)

            /** The anchor has been requested to be persisted but the operation is still pending. */
            @JvmField public val PENDING: PersistenceState = PersistenceState(1)

            /** The anchor has been persisted. */
            @JvmField public val PERSISTED: PersistenceState = PersistenceState(2)
        }
    }

    /** The location of the anchor in the world coordinate space. */
    public val pose: Pose

    /** The current state of the pose of this anchor. */
    public val trackingState: TrackingState

    /** The [PersistenceState] for this anchor. */
    public val persistenceState: PersistenceState

    /** The [UUID] that identifies this Anchor if it is persisted. */
    public val uuid: UUID?

    /**
     * Detaches this anchor from its [Trackable]. After detaching, the anchor will not be updated
     * anymore and cannot be reattached to another trackable.
     */
    public fun detach()

    /**
     * Sends a request to persist this anchor. The value of [persistenceState] will be updated based
     * on the progress of this operation. The value of [uuid] will be set if the operation is
     * successful.
     */
    public fun persist()
}
