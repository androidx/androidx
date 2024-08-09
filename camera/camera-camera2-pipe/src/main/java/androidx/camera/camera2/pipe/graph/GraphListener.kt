/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.GraphState.GraphStateError

public interface GraphListener {
    /**
     * Used to indicate that the graph is starting. This is called immediately when a [CameraGraph]
     * is being started.
     */
    public fun onGraphStarting() {}

    /**
     * Used to indicate that the graph has been initialized and is ready to actively process
     * requests using the provided [GraphRequestProcessor] interface.
     */
    public fun onGraphStarted(requestProcessor: GraphRequestProcessor)

    /**
     * Used to indicate that the graph is stopping. This is called immediately when a [CameraGraph]
     * is being stopped.
     */
    public fun onGraphStopping() {}

    /**
     * Used to indicate that a previously initialized [GraphRequestProcessor] is no longer
     * available.
     */
    public fun onGraphStopped(requestProcessor: GraphRequestProcessor?)

    /**
     * Used to indicate that the internal state of the [GraphRequestProcessor] has changed. This is
     * a signal that previously queued requests may now succeed if they previously failed.
     */
    public fun onGraphModified(requestProcessor: GraphRequestProcessor)

    /** Used to indicate that the graph has encountered an error. */
    public fun onGraphError(graphStateError: GraphStateError)
}
