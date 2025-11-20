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
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.Camera2Logger
import androidx.camera.core.CameraState
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.LiveDataObservable
import androidx.core.util.Consumer
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2

@CameraScope
public class CameraStateAdapter @Inject constructor() {
    private val lock = Any()

    internal val cameraInternalState = LiveDataObservable<CameraInternal.State>()
    internal val cameraState = MutableLiveData<CameraState>()

    @GuardedBy("lock") private var currentGraph: CameraGraph? = null

    @GuardedBy("lock") private var currentCameraInternalState = CameraInternal.State.CLOSED

    @GuardedBy("lock") private var currentCameraStateError: CameraState.StateError? = null

    @GuardedBy("lock") private var isRemoved = false

    @GuardedBy("lock")
    private val cameraStateListeners = mutableMapOf<Consumer<CameraState>, Executor>()

    init {
        postCameraState(CameraInternal.State.CLOSED)
    }

    /**
     * Forces the state to CLOSED with a ERROR_CAMERA_REMOVED error.
     *
     * This is called when the camera is physically removed or becomes permanently unavailable,
     * providing an immediate state update to clients.
     */
    public fun onRemoved() {
        val error = CameraState.StateError.create(CameraState.ERROR_CAMERA_REMOVED)
        synchronized(lock) {
            if (isRemoved) return

            Camera2Logger.debug { "Camera is removed, forcing state to CLOSED." }
            isRemoved = true
            currentCameraInternalState = CameraInternal.State.CLOSED
            currentCameraStateError = error
            postCameraState(currentCameraInternalState, currentCameraStateError)

            // Clear the graph reference as it's no longer valid.
            currentGraph = null
        }
    }

    public fun onGraphUpdated(cameraGraph: CameraGraph): Unit =
        synchronized(lock) {
            Camera2Logger.debug { "Camera graph updated from $currentGraph to $cameraGraph" }
            if (currentCameraInternalState != CameraInternal.State.CLOSED) {
                postCameraState(CameraInternal.State.CLOSING)
                postCameraState(CameraInternal.State.CLOSED)
            }
            currentGraph = cameraGraph
            currentCameraInternalState = CameraInternal.State.CLOSED
        }

    public fun onGraphStateUpdated(cameraGraph: CameraGraph, graphState: GraphState): Unit =
        synchronized(lock) {
            // Ignore any events if the camera has been marked as removed.
            if (isRemoved) {
                Camera2Logger.warn { "Ignoring graph state update $graphState on removed camera." }
                return
            }

            Camera2Logger.debug { "$cameraGraph state updated to $graphState" }
            handleStateTransition(cameraGraph, graphState)
        }

    @GuardedBy("lock")
    private fun handleStateTransition(cameraGraph: CameraGraph, graphState: GraphState) {
        // If the transition came from a different camera graph, consider it stale and ignore it.
        if (cameraGraph != currentGraph) {
            Camera2Logger.debug { "Ignored stale transition $graphState for $cameraGraph" }
            return
        }

        val nextComboState = calculateNextState(currentCameraInternalState, graphState)
        if (nextComboState == null) {
            Camera2Logger.warn {
                "Impermissible state transition: " +
                    "current camera internal state: $currentCameraInternalState, " +
                    "received graph state: $graphState"
            }
            return
        }
        currentCameraInternalState = nextComboState.state
        currentCameraStateError = nextComboState.error

        // Now that the current graph state is updated, post the latest states.
        Camera2Logger.debug { "Updated current camera internal state to $nextComboState" }
        postCameraState(currentCameraInternalState, currentCameraStateError)
    }

    private fun postCameraState(
        internalState: CameraInternal.State,
        stateError: CameraState.StateError? = null,
    ) {
        cameraInternalState.postValue(internalState)

        val publicState = CameraState.create(internalState.toCameraState(), stateError)

        cameraState.setOrPostValue(publicState)

        val listeners = synchronized(lock) { cameraStateListeners.entries.toList() }
        listeners.forEach { (listener, executor) ->
            executor.execute { listener.accept(publicState) }
        }
    }

    /**
     * Calculates the next CameraX camera internal state based on the current camera internal state
     * and the graph state received from CameraGraph. Returns null when there's no permissible state
     * transition.
     */
    internal fun calculateNextState(
        currentState: CameraInternal.State,
        graphState: GraphState,
    ): CombinedCameraState? =
        when (currentState) {
            CameraInternal.State.CLOSED ->
                when (graphState) {
                    GraphStateStarting -> CombinedCameraState(CameraInternal.State.OPENING)
                    GraphStateStarted -> CombinedCameraState(CameraInternal.State.OPEN)
                    else -> null
                }
            CameraInternal.State.OPENING ->
                when (graphState) {
                    GraphStateStarted -> CombinedCameraState(CameraInternal.State.OPEN)
                    is GraphStateError ->
                        if (graphState.willAttemptRetry) {
                            CombinedCameraState(
                                CameraInternal.State.OPENING,
                                graphState.cameraError.toCameraStateError(),
                            )
                        } else {
                            if (isRecoverableError(graphState.cameraError)) {
                                CombinedCameraState(
                                    CameraInternal.State.PENDING_OPEN,
                                    graphState.cameraError.toCameraStateError(),
                                )
                            } else {
                                CombinedCameraState(
                                    CameraInternal.State.CLOSING,
                                    graphState.cameraError.toCameraStateError(),
                                )
                            }
                        }
                    GraphStateStopping -> CombinedCameraState(CameraInternal.State.CLOSING)
                    GraphStateStopped -> CombinedCameraState(CameraInternal.State.CLOSED)
                    else -> null
                }
            CameraInternal.State.OPEN ->
                when (graphState) {
                    GraphStateStopping -> CombinedCameraState(CameraInternal.State.CLOSING)
                    GraphStateStopped -> CombinedCameraState(CameraInternal.State.CLOSED)
                    is GraphStateError ->
                        if (isRecoverableError(graphState.cameraError)) {
                            CombinedCameraState(
                                CameraInternal.State.PENDING_OPEN,
                                graphState.cameraError.toCameraStateError(),
                            )
                        } else {
                            CombinedCameraState(
                                CameraInternal.State.CLOSED,
                                graphState.cameraError.toCameraStateError(),
                            )
                        }
                    else -> null
                }
            CameraInternal.State.CLOSING ->
                when (graphState) {
                    GraphStateStopped -> CombinedCameraState(CameraInternal.State.CLOSED)
                    GraphStateStarting -> CombinedCameraState(CameraInternal.State.OPENING)
                    is GraphStateError ->
                        CombinedCameraState(
                            CameraInternal.State.CLOSING,
                            graphState.cameraError.toCameraStateError(),
                        )
                    else -> null
                }
            CameraInternal.State.PENDING_OPEN ->
                when (graphState) {
                    GraphStateStarting -> CombinedCameraState(CameraInternal.State.OPENING)
                    GraphStateStarted -> CombinedCameraState(CameraInternal.State.OPEN)
                    is GraphStateError ->
                        if (isRecoverableError(graphState.cameraError)) {
                            CombinedCameraState(
                                CameraInternal.State.PENDING_OPEN,
                                graphState.cameraError.toCameraStateError(),
                            )
                        } else {
                            CombinedCameraState(
                                CameraInternal.State.CLOSED,
                                graphState.cameraError.toCameraStateError(),
                            )
                        }
                    else -> null
                }
            else -> null
        }

    internal fun addCameraStateListener(executor: Executor, listener: Consumer<CameraState>) {
        synchronized(lock) { cameraStateListeners[listener] = executor }
    }

    internal fun removeCameraStateListener(listener: Consumer<CameraState>) {
        synchronized(lock) { cameraStateListeners.remove(listener) }
    }

    internal data class CombinedCameraState(
        val state: CameraInternal.State,
        val error: CameraState.StateError? = null,
    )

    public companion object {
        internal fun CameraError.toCameraStateError(): CameraState.StateError =
            CameraState.StateError.create(
                when (this) {
                    CameraError.ERROR_UNDETERMINED -> CameraState.ERROR_CAMERA_FATAL_ERROR
                    CameraError.ERROR_CAMERA_IN_USE -> CameraState.ERROR_CAMERA_IN_USE
                    CameraError.ERROR_CAMERA_LIMIT_EXCEEDED -> CameraState.ERROR_MAX_CAMERAS_IN_USE
                    CameraError.ERROR_CAMERA_DISABLED -> CameraState.ERROR_CAMERA_DISABLED
                    CameraError.ERROR_CAMERA_DEVICE -> CameraState.ERROR_OTHER_RECOVERABLE_ERROR
                    CameraError.ERROR_CAMERA_SERVICE -> CameraState.ERROR_CAMERA_FATAL_ERROR
                    CameraError.ERROR_CAMERA_DISCONNECTED -> CameraState.ERROR_CAMERA_IN_USE
                    CameraError.ERROR_ILLEGAL_ARGUMENT_EXCEPTION ->
                        CameraState.ERROR_CAMERA_FATAL_ERROR
                    CameraError.ERROR_SECURITY_EXCEPTION -> CameraState.ERROR_CAMERA_FATAL_ERROR
                    CameraError.ERROR_GRAPH_CONFIG -> CameraState.ERROR_STREAM_CONFIG
                    CameraError.ERROR_DO_NOT_DISTURB_ENABLED ->
                        CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED
                    CameraError.ERROR_UNKNOWN_EXCEPTION -> CameraState.ERROR_CAMERA_FATAL_ERROR
                    CameraError.ERROR_CAMERA_OPENER -> CameraState.ERROR_CAMERA_FATAL_ERROR
                    CameraError.ERROR_CAMERA_OPEN_TIMEOUT -> CameraState.ERROR_CAMERA_FATAL_ERROR
                    else -> throw IllegalArgumentException("Unexpected CameraError: $this")
                }
            )

        internal fun CameraInternal.State.toCameraState(): CameraState.Type =
            when (this) {
                CameraInternal.State.CLOSED -> CameraState.Type.CLOSED
                CameraInternal.State.OPENING -> CameraState.Type.OPENING
                CameraInternal.State.OPEN -> CameraState.Type.OPEN
                CameraInternal.State.CLOSING -> CameraState.Type.CLOSING
                CameraInternal.State.PENDING_OPEN -> CameraState.Type.PENDING_OPEN
                else -> throw IllegalArgumentException("Unexpected CameraInternal state: $this")
            }

        internal fun isRecoverableError(cameraError: CameraError) =
            cameraError == CameraError.ERROR_CAMERA_DISCONNECTED ||
                cameraError == CameraError.ERROR_CAMERA_IN_USE ||
                cameraError == CameraError.ERROR_CAMERA_LIMIT_EXCEEDED ||
                cameraError == CameraError.ERROR_CAMERA_DEVICE

        internal fun MutableLiveData<CameraState>.setOrPostValue(cameraState: CameraState) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                this.value = cameraState
            } else {
                this.postValue(cameraState)
            }
        }
    }
}
