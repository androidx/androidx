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
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.math.Pose
import com.google.ar.core.Anchor as ARCore1xAnchor
import java.util.UUID

/**
 * Wraps the native [ARCore1xAnchor] with the [Anchor] interface.
 *
 * @property arCoreAnchor The underlying [ARCore1xAnchor] instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCoreAnchor internal constructor(internal val _arCoreAnchor: ARCore1xAnchor) :
    Anchor {

    @UnsupportedArCoreCompatApi public fun arCoreAnchor(): ARCore1xAnchor = _arCoreAnchor

    /** ARCore 1.x does not support persistent anchors; this property is always [NOT_PERSISTED] */
    override val persistenceState: Anchor.PersistenceState = Anchor.PersistenceState.NOT_PERSISTED

    /**
     * The pose of the anchor.
     *
     * This property simply gets the pose from the underlying [ARCore1xAnchor] instance, and
     * converts it to a [Pose].
     *
     * @return The pose of the anchor.
     */
    override val pose: Pose
        get() = _arCoreAnchor.pose.toRuntimePose()

    /**
     * The tracking state of the anchor.
     *
     * This property simply gets the tracking state from the underlying [ARCore1xAnchor] instance,
     * and converts it to a [TrackingState].
     *
     * @return The tracking state of the anchor.
     */
    override val trackingState: TrackingState
        get() = TrackingState.fromArCoreTrackingState(_arCoreAnchor.trackingState)

    /* ARCore 1.x does not support persistent anchors; this property is always null. */
    override val uuid: UUID? = null

    /**
     * Detaches the anchor from the underlying [ARCore1xAnchor] instance.
     *
     * This method simply calls the [ARCore1xAnchor.detach] method.
     */
    override fun detach() {
        _arCoreAnchor.detach()
    }

    /* ARCore 1.x does not support persistent anchors; this method throws [NotImplementedError] when called. */
    override fun persist() {
        throw NotImplementedError("Persistent anchors are not supported.")
    }
}
