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

package androidx.camera.camera2.adapter

import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.GraphStateListener

/**
 * A [androidx.camera.camera2.pipe.GraphStateListener] that forwards state changes to the
 * [CameraStateAdapter].
 */
public class GraphStateToCameraStateAdapter(private val cameraStateAdapter: CameraStateAdapter) :
    GraphStateListener {
    public lateinit var cameraGraph: CameraGraph

    override fun onGraphStarting() {
        cameraStateAdapter.onGraphStateUpdated(cameraGraph, GraphState.GraphStateStarting)
    }

    override fun onGraphStarted() {
        cameraStateAdapter.onGraphStateUpdated(cameraGraph, GraphState.GraphStateStarted)
    }

    override fun onGraphStopping() {
        cameraStateAdapter.onGraphStateUpdated(cameraGraph, GraphState.GraphStateStopping)
    }

    override fun onGraphStopped() {
        cameraStateAdapter.onGraphStateUpdated(cameraGraph, GraphState.GraphStateStopped)
    }

    override fun onGraphError(graphStateError: GraphState.GraphStateError) {
        cameraStateAdapter.onGraphStateUpdated(cameraGraph, graphStateError)
    }
}
