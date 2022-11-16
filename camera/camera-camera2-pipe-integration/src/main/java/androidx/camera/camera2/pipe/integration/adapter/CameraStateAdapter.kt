/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.os.Looper
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraState
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.LiveDataObservable
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject

@CameraScope
@RequiresApi(21)
class CameraStateAdapter @Inject constructor() {
    private val lock = Any()

    internal val cameraInternalState = LiveDataObservable<CameraInternal.State>()
    internal val cameraState = MutableLiveData<CameraState>()

    @GuardedBy("lock")
    private var currentGraph: CameraGraph? = null

    @GuardedBy("lock")
    private var currentGraphState: GraphState = GraphStateStopped

    init {
        postCameraState(CameraInternal.State.CLOSED)
    }

    public fun onGraphUpdated(cameraGraph: CameraGraph) = synchronized(lock) {
        Log.debug { "Camera graph updated from $currentGraph to $cameraGraph" }
        if (currentGraphState != GraphStateStopped) {
            postCameraState(CameraInternal.State.CLOSING)
            postCameraState(CameraInternal.State.CLOSED)
        }
        currentGraph = cameraGraph
        currentGraphState = GraphStateStopped
    }

    public fun onGraphStateUpdated(cameraGraph: CameraGraph, graphState: GraphState) =
        synchronized(lock) {
            Log.debug { "$cameraGraph state updated to $graphState" }
            handleStateTransition(cameraGraph, graphState)
        }

    @GuardedBy("lock")
    private fun handleStateTransition(cameraGraph: CameraGraph, graphState: GraphState) {
        // If the transition came from a different camera graph, consider it stale and ignore it.
        if (cameraGraph != currentGraph) {
            Log.debug { "Ignored stale transition $graphState for $cameraGraph" }
            return
        }

        if (!isTransitionPermissible(currentGraphState, graphState)) {
            Log.warn { "Impermissible state transition from $currentGraphState to $graphState" }
            return
        }
        currentGraphState = graphState

        // Now that the current graph state is updated, post the latest states.
        Log.debug { "Updated current graph state to $currentGraphState" }
        postCameraState(currentGraphState.toCameraInternalState())
    }

    private fun postCameraState(internalState: CameraInternal.State) {
        cameraInternalState.postValue(internalState)
        cameraState.setOrPostValue(CameraState.create(internalState.toCameraState()))
    }

    private fun isTransitionPermissible(oldState: GraphState, newState: GraphState): Boolean {
        return when (oldState) {
            GraphStateStarting ->
                newState == GraphStateStarted ||
                    newState == GraphStateStopping ||
                    newState == GraphStateStopped

            GraphStateStarted ->
                newState == GraphStateStopping ||
                    newState == GraphStateStopped

            GraphStateStopping ->
                newState == GraphStateStopped ||
                    newState == GraphStateStarting

            GraphStateStopped ->
                newState == GraphStateStarting ||
                    newState == GraphStateStarted

            else -> false
        }
    }
}

@RequiresApi(21)
internal fun GraphState.toCameraInternalState(): CameraInternal.State = when (this) {
    GraphStateStarting -> CameraInternal.State.OPENING
    GraphStateStarted -> CameraInternal.State.OPEN
    GraphStateStopping -> CameraInternal.State.CLOSING
    GraphStateStopped -> CameraInternal.State.CLOSED
    else -> throw IllegalArgumentException("Unexpected graph state: $this")
}

@RequiresApi(21)
internal fun CameraInternal.State.toCameraState(): CameraState.Type = when (this) {
    CameraInternal.State.CLOSED -> CameraState.Type.CLOSED
    CameraInternal.State.OPENING -> CameraState.Type.OPENING
    CameraInternal.State.OPEN -> CameraState.Type.OPEN
    CameraInternal.State.CLOSING -> CameraState.Type.CLOSING
    CameraInternal.State.PENDING_OPEN -> CameraState.Type.PENDING_OPEN
    else -> throw IllegalArgumentException("Unexpected CameraInternal state: $this")
}

internal fun MutableLiveData<CameraState>.setOrPostValue(cameraState: CameraState) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        this.value = cameraState
    } else {
        this.postValue(cameraState)
    }
}