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

package androidx.camera.camera2.adapter

import android.os.Looper
import androidx.annotation.GuardedBy
import androidx.camera.camera2.config.CameraScope
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.core.CameraState
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.LiveDataObservable
import androidx.core.util.Consumer
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executor
import javax.inject.Inject

@CameraScope
public class CameraStateAdapter @Inject constructor() {
    private val lock = Any()

    internal val cameraInternalState = LiveDataObservable<CameraInternal.State>()
    internal val cameraState = MutableLiveData<CameraState>()

    @GuardedBy("lock") private var currentGraph: CameraGraph? = null

    @GuardedBy("lock") private var closedGraph: CameraGraph? = null

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
            closedGraph = null
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
            closedGraph = null
            currentCameraInternalState = CameraInternal.State.CLOSED
            currentCameraStateError = null
        }

    /**
     * Signals that a specific CameraGraph has been explicitly closed by the application layer. This
     * acts as the definitive "User requested close" signal.
     */
    public fun onGraphClosed(cameraGraph: CameraGraph): Unit =
        synchronized(lock) {
            if (currentGraph == cameraGraph) {
                // If we are still in a non-closed state, force a transition to CLOSED.
                if (currentCameraInternalState != CameraInternal.State.CLOSED) {
                    // Transition to closing state and we will wait for CameraGraph to stop
                    // and then transition to CLOSED state.
                    postCameraState(CameraInternal.State.CLOSING)
                    postCameraState(CameraInternal.State.CLOSED)
                }
                closedGraph = cameraGraph
                currentCameraInternalState = CameraInternal.State.CLOSED
            }
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

        val isGraphActive = currentGraph != null && currentGraph != closedGraph
        val nextComboState =
            calculateNextState(
                currentCameraInternalState,
                graphState,
                currentCameraStateError,
                isGraphActive,
            )
        if (nextComboState == null) {
            Camera2Logger.warn {
                "Impermissible state transition: " +
                    "current camera internal state: $currentCameraInternalState, " +
                    "received graph state: $graphState"
            }
            return
        }
        // Fill Missing Transition: PENDING/CLOSED -> OPENING -> OPEN
        if (nextComboState.state == CameraInternal.State.OPEN) {
            if (
                currentCameraInternalState == CameraInternal.State.CLOSED ||
                    currentCameraInternalState == CameraInternal.State.PENDING_OPEN
            ) {
                postCameraState(CameraInternal.State.OPENING)
            }
        }
        currentCameraInternalState = nextComboState.state
        currentCameraStateError = nextComboState.error

        // Now that the current graph state is updated, post the latest states.
        Camera2Logger.debug {
            "Updated current camera internal state: $currentCameraInternalState to $nextComboState"
        }
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
        currentError: CameraState.StateError?,
        isGraphActive: Boolean,
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
                        resolveErrorEvent(graphState, CameraInternal.State.OPENING)
                    GraphStateStopping,
                    GraphStateStopped -> graphState.handleStateStop(isGraphActive, currentError)
                    else -> null
                }
            CameraInternal.State.OPEN ->
                when (graphState) {
                    GraphStateStopping,
                    GraphStateStopped -> graphState.handleStateStop(isGraphActive, currentError)
                    is GraphStateError ->
                        resolveErrorEvent(graphState, CameraInternal.State.OPENING)
                    else -> null
                }
            CameraInternal.State.CLOSING ->
                when (graphState) {
                    GraphStateStopped -> graphState.handleStateStop(isGraphActive, currentError)
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
                    is GraphStateError -> {
                        // Calculate the error code (downgrading critical to recoverable if needed)
                        val (_, newError) =
                            resolveErrorEvent(graphState, CameraInternal.State.OPENING)
                        // STAY in PENDING_OPEN. Do not go back to OPENING.
                        CombinedCameraState(CameraInternal.State.PENDING_OPEN, newError)
                    }
                    GraphStateStopping,
                    GraphStateStopped -> graphState.handleStateStop(isGraphActive, currentError)
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
        private fun GraphState.handleStateStop(
            isGraphActive: Boolean,
            currentError: CameraState.StateError?,
        ): CombinedCameraState? {
            if (isGraphActive && currentError != null) {
                return CombinedCameraState(CameraInternal.State.PENDING_OPEN, currentError)
            }

            // If the graph is not active (User closed it), strictly close.
            return when (this) {
                GraphStateStopping ->
                    CombinedCameraState(CameraInternal.State.CLOSING, currentError)
                GraphStateStopped -> CombinedCameraState(CameraInternal.State.CLOSED, currentError)
                else -> null
            }
        }

        /** Resolves a GraphStateError into a CombinedCameraState. */
        private fun resolveErrorEvent(
            graphStateError: GraphStateError,
            retryState: CameraInternal.State,
        ): CombinedCameraState {
            val cameraError = graphStateError.cameraError

            // 1. Recoverable via Retry or Native Recoverability
            if (isRecoverableError(cameraError) || graphStateError.willAttemptRetry) {
                // If retrying, force "Recoverable" type to align with CameraX contract
                val stateError =
                    if (graphStateError.willAttemptRetry && !isRecoverableError(cameraError)) {
                        CameraState.StateError.create(CameraState.ERROR_OTHER_RECOVERABLE_ERROR)
                    } else {
                        cameraError.toCameraStateError()
                    }
                val nextState =
                    if (!graphStateError.willAttemptRetry) {
                        CameraInternal.State.PENDING_OPEN
                    } else {
                        retryState
                    }
                return CombinedCameraState(nextState, stateError)
            }

            return CombinedCameraState(
                CameraInternal.State.CLOSING,
                cameraError.toCameraStateError(),
            )
        }

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
