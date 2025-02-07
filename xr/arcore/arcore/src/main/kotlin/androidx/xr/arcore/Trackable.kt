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

package androidx.xr.arcore

import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.StateFlow

/** An object that ARCore for Jetpack XR can track and that [Anchors] can be attached to. */
public interface Trackable<out State> {

    /** The subset of data that is common to the state of all [Trackable] instances. */
    public interface State {
        /** Whether this trackable is being tracked or not. */
        public val trackingState: TrackingState
    }

    /** Emits the current state of this trackable. */
    public val state: StateFlow<Trackable.State>

    /**
     * Creates an [Anchor] that is attached to this trackable, using the given initial [pose] in the
     * world coordinate space.
     */
    public fun createAnchor(pose: Pose): AnchorCreateResult
}
