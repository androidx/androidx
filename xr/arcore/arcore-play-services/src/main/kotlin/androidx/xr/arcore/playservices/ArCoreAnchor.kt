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

package androidx.xr.arcore.playservices

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import com.google.ar.core.Anchor as ARCore1xAnchor
import java.util.UUID

/**
 * Wraps a [com.google.ar.core.Anchor] with the [Anchor] interface.
 *
 * @property arCoreAnchor the underlying [ARCore1xAnchor] instance
 * @property persistenceState the [Anchor.PersistenceState] of the anchor
 * @property pose the [Pose] of the anchor
 * @property trackingState the [TrackingState] of the anchor
 * @property uuid the [UUID] of the anchor
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCoreAnchor internal constructor(internal val _arCoreAnchor: ARCore1xAnchor) :
    Anchor {

    @UnsupportedArCoreCompatApi public fun arCoreAnchor(): ARCore1xAnchor = _arCoreAnchor

    override val persistenceState: Anchor.PersistenceState = Anchor.PersistenceState.NOT_PERSISTED

    override val pose: Pose
        get() = _arCoreAnchor.pose.toRuntimePose()

    override val trackingState: TrackingState
        get() = TrackingState.fromArCoreTrackingState(_arCoreAnchor.trackingState)

    override val uuid: UUID? = null

    /**
     * Detaches the anchor from the underlying [ARCore1xAnchor] instance.
     *
     * This method simply calls the [ARCore1xAnchor.detach] method.
     */
    override fun detach() {
        _arCoreAnchor.detach()
    }

    /**
     * ARCore 1.x does not support persistent anchors; this method throws [NotImplementedError] when
     * called.
     */
    override fun persist() {
        throw NotImplementedError("Persistent anchors are not supported.")
    }
}
