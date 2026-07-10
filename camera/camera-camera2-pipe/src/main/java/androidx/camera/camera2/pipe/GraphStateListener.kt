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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo

/** A listener for receiving state updates from a CameraGraph. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GraphStateListener {
    /**
     * Called when the [CameraGraph] is transitioning to the [GraphState.GraphStateStarting] state.
     */
    public fun onGraphStarting() {}

    /**
     * Called when the [CameraGraph] has transitioned to the [GraphState.GraphStateStarted] state.
     */
    public fun onGraphStarted() {}

    /**
     * Called when the [CameraGraph] is transitioning to the [GraphState.GraphStateStopping] state.
     */
    public fun onGraphStopping() {}

    /**
     * Called when the [CameraGraph] has transitioned to the [GraphState.GraphStateStopped] state.
     */
    public fun onGraphStopped() {}

    /**
     * Called when the [CameraGraph] has encountered an error and transitioned to the
     * [GraphState.GraphStateError] state.
     *
     * The listener can check [GraphState.GraphStateError.willAttemptRetry] on the [graphStateError]
     * parameter to see if the [CameraGraph] will automatically try to recover. If recovery is not
     * attempted, the graph will typically transition to [GraphState.GraphStateStopped].
     *
     * @param graphStateError The error state, containing the [CameraError] and retry information.
     */
    public fun onGraphError(graphStateError: GraphState.GraphStateError) {}
}
