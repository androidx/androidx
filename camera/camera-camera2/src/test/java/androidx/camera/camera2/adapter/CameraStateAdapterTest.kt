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

import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.testing.FakeCameraGraph
import androidx.camera.core.CameraState
import androidx.camera.core.CameraState.ERROR_CAMERA_DISABLED
import androidx.camera.core.CameraState.ERROR_CAMERA_REMOVED
import androidx.camera.core.CameraState.ERROR_MAX_CAMERAS_IN_USE
import androidx.camera.core.CameraState.ERROR_OTHER_RECOVERABLE_ERROR
import androidx.camera.core.impl.CameraInternal
import androidx.core.util.Consumer
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
internal class CameraStateAdapterTest {
    private val cameraStateAdapter = CameraStateAdapter()
    private val cameraGraph1 = FakeCameraGraph()
    private val cameraGraph2 = FakeCameraGraph()

    @Test
    fun testCalculateNextStateWhenClosed() {
        // Assume graph is active (just attached)
        val isGraphActive = true

        val nextStateWhenGraphStateStarting =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSED,
                GraphStateStarting,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateStarted =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSED,
                GraphStateStarted,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateStopping =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSED,
                GraphStateStopping,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateStopped =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSED,
                GraphStateStopped,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateError =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSED,
                GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, true),
                null,
                isGraphActive,
            )

        assertThat(nextStateWhenGraphStateStarting!!.state).isEqualTo(CameraInternal.State.OPENING)
        assertThat(nextStateWhenGraphStateStarted!!.state).isEqualTo(CameraInternal.State.OPEN)
        assertThat(nextStateWhenGraphStateStopping).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStopped).isEqualTo(null)
        assertThat(nextStateWhenGraphStateError).isEqualTo(null)
    }

    @Test
    fun testCalculateNextStateWhenOpening() {
        val isGraphActive = true

        val nextStateWhenGraphStateStarting =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPENING,
                GraphStateStarting,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateStarted =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPENING,
                GraphStateStarted,
                null,
                isGraphActive,
            )
        // If stopping/stopped while active -> PENDING_OPEN
        val nextStateWhenGraphStateStopping =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPENING,
                GraphStateStopping,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateStopped =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPENING,
                GraphStateStopped,
                null,
                isGraphActive,
            )
        // Retry = True -> OPENING
        val nextStateWhenGraphStateErrorWillRetry =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPENING,
                GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, willAttemptRetry = true),
                null,
                isGraphActive,
            )
        // Retry = False -> PENDING_OPEN (Waiting for supervisor restart)
        val nextStateWhenGraphStateErrorRecoverableWillNotRetry =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPENING,
                GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, willAttemptRetry = false),
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateErrorUnrecoverableWillNotRetry =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPENING,
                GraphStateError(CameraError.ERROR_CAMERA_DISABLED, willAttemptRetry = false),
                null,
                isGraphActive,
            )

        assertThat(nextStateWhenGraphStateStarting).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStarted!!.state).isEqualTo(CameraInternal.State.OPEN)
        assertThat(nextStateWhenGraphStateStopping!!.state).isEqualTo(CameraInternal.State.CLOSING)
        assertThat(nextStateWhenGraphStateStopped!!.state).isEqualTo(CameraInternal.State.CLOSED)
        assertThat(nextStateWhenGraphStateErrorWillRetry!!.state)
            .isEqualTo(CameraInternal.State.OPENING)
        assertThat(nextStateWhenGraphStateErrorRecoverableWillNotRetry!!.state)
            .isEqualTo(CameraInternal.State.PENDING_OPEN)
        assertThat(nextStateWhenGraphStateErrorUnrecoverableWillNotRetry!!.state)
            .isEqualTo(CameraInternal.State.CLOSING)
    }

    @Test
    fun testCalculateNextStateWhenOpen() {
        val isGraphActive = true

        val nextStateWhenGraphStateStarting =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPEN,
                GraphStateStarting,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateStarted =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPEN,
                GraphStateStarted,
                null,
                isGraphActive,
            )
        // Stopping -> PENDING_OPEN
        val nextStateWhenGraphStateStopping =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPEN,
                GraphStateStopping,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateStopped =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPEN,
                GraphStateStopped,
                null,
                isGraphActive,
            )
        // Retry -> OPENING
        val nextStateWhenGraphStateErrorRecoverable =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPEN,
                GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, true),
                null,
                isGraphActive,
            )
        // No Retry -> PENDING_OPEN
        val nextStateWhenGraphStateErrorUnrecoverable =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPEN,
                GraphStateError(CameraError.ERROR_CAMERA_DISABLED, false),
                null,
                isGraphActive,
            )

        assertThat(nextStateWhenGraphStateStarting).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStarted).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStopping!!.state).isEqualTo(CameraInternal.State.CLOSING)
        assertThat(nextStateWhenGraphStateStopped!!.state).isEqualTo(CameraInternal.State.CLOSED)
        assertThat(nextStateWhenGraphStateErrorRecoverable!!.state)
            .isEqualTo(CameraInternal.State.OPENING)
        assertThat(nextStateWhenGraphStateErrorUnrecoverable!!.state)
            .isEqualTo(CameraInternal.State.CLOSING)
    }

    @Test
    fun testCalculateNextStateWhenClosing() {
        // When closing, isGraphActive is FALSE because onGraphClosed() was called.
        val isGraphActive = false

        val nextStateWhenGraphStateStarting =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSING,
                GraphStateStarting,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateStarted =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSING,
                GraphStateStarted,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateStopping =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSING,
                GraphStateStopping,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateStopped =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSING,
                GraphStateStopped,
                null,
                isGraphActive,
            )
        val nextStateWhenGraphStateError =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSING,
                GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, true),
                null,
                isGraphActive,
            )

        assertThat(nextStateWhenGraphStateStarting!!.state).isEqualTo(CameraInternal.State.OPENING)
        assertThat(nextStateWhenGraphStateStarted).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStopping).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStopped!!.state).isEqualTo(CameraInternal.State.CLOSED)
        assertThat(nextStateWhenGraphStateError!!.state).isEqualTo(CameraInternal.State.CLOSING)
        assertThat(nextStateWhenGraphStateError.error?.code).isEqualTo(ERROR_MAX_CAMERAS_IN_USE)
    }

    @Test
    fun testCalculateNextStateWhenPendingOpen() {
        val isGraphActive = true

        val nextStateWhenGraphStateStarting =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.PENDING_OPEN,
                GraphStateStarting,
                null,
                isGraphActive,
            )
        assertThat(nextStateWhenGraphStateStarting!!.state).isEqualTo(CameraInternal.State.OPENING)

        // Stop with error expect it is retrying -> Stay PENDING
        val nextStateWhenGraphStateStopped =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.PENDING_OPEN,
                GraphStateStopped,
                CameraState.StateError.create(ERROR_OTHER_RECOVERABLE_ERROR),
                isGraphActive,
            )
        assertThat(nextStateWhenGraphStateStopped!!.state)
            .isEqualTo(CameraInternal.State.PENDING_OPEN)

        // Error (No Retry) -> Stay PENDING
        val nextStateWhenGraphStateError =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.PENDING_OPEN,
                GraphStateError(CameraError.ERROR_CAMERA_DISABLED, false),
                null,
                isGraphActive,
            )
        assertThat(nextStateWhenGraphStateError!!.state)
            .isEqualTo(CameraInternal.State.PENDING_OPEN)
    }

    @Test
    fun testPendingOpenStabilization() {
        // This tests that we ignore "willRetry=true" noise if we are already in PENDING_OPEN
        val isGraphActive = true

        // Arrange: Adapter is in PENDING_OPEN
        // Act: Receive a Recoverable error with Retry (which usually goes to OPENING)
        val nextState =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.PENDING_OPEN,
                GraphStateError(CameraError.ERROR_UNDETERMINED, true),
                null,
                isGraphActive,
            )

        // Assert: We stay PENDING_OPEN to avoid flickering
        assertThat(nextState!!.state).isEqualTo(CameraInternal.State.PENDING_OPEN)
    }

    @Test
    fun testCriticalErrorDowngradedToRecoverableWhenRetrying() {
        // Arrange: Error is Undetermined (Critical) but Retry is True
        val error = GraphStateError(CameraError.ERROR_UNDETERMINED, true)

        val nextState =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPENING,
                error,
                null,
                isGraphActive = true,
            )

        // Assert: Maps to OPENING
        assertThat(nextState!!.state).isEqualTo(CameraInternal.State.OPENING)
        // Assert: Error is downgraded to OTHER_RECOVERABLE
        assertThat(nextState.error!!.code).isEqualTo(ERROR_OTHER_RECOVERABLE_ERROR)
    }

    @Test
    fun testOnGraphClosed_whileInPendingOpen_transitionsToClosed() {
        // 1. Arrange: Setup an active graph and move the state to PENDING_OPEN
        // This happens when a recoverable error occurs and willAttemptRetry is false.
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        cameraStateAdapter.onGraphStateUpdated(
            cameraGraph1,
            GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, willAttemptRetry = false),
        )

        val intermediateState = cameraStateAdapter.cameraState.value!!
        assertThat(intermediateState.type).isEqualTo(CameraState.Type.PENDING_OPEN)

        // 2. Act: User explicitly closes the graph
        cameraStateAdapter.onGraphClosed(cameraGraph1)

        // 3. Assert: State should transition to CLOSED immediately
        val finalState = cameraStateAdapter.cameraState.value!!
        assertThat(finalState.type).isEqualTo(CameraState.Type.CLOSED)
    }

    @Test
    fun testNormalStateTransitions() {
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPENING)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPEN)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStopped)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)
    }

    @Test
    fun testStaleStateTransitions() {
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPEN)

        // Simulate that a new camera graph is created.
        cameraStateAdapter.onGraphUpdated(cameraGraph2)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph2, GraphStateStarting)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPENING)

        // This came from cameraGraph1 and thereby making the transition stale.
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStopped)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPENING)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph2, GraphStateStarted)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPEN)
    }

    @Test
    fun testImpermissibleStateTransitions() {
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)

        // Impermissible state transition from GraphStateStopped to GraphStateStopping
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStopping)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPEN)

        // Impermissible state transition from GraphStateStarted to GraphStateStarting
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPEN)
    }

    @Test
    fun testStateTransitionsOnRecoverableErrorsWhenOpening() {
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPENING)

        // We should transition to OPENING with an error code if we encounter errors during opening.
        cameraStateAdapter.onGraphStateUpdated(
            cameraGraph1,
            GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true),
        )
        val cameraStateWillRetry = cameraStateAdapter.cameraState.value!!
        assertThat(cameraStateWillRetry.type).isEqualTo(CameraState.Type.OPENING)
        assertThat(cameraStateWillRetry.error?.code).isEqualTo(ERROR_OTHER_RECOVERABLE_ERROR)

        // Now assume we've exceeded retries and will no longer retry.
        cameraStateAdapter.onGraphStateUpdated(
            cameraGraph1,
            GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = false),
        )
        val cameraStateNotRetry = cameraStateAdapter.cameraState.value!!
        assertThat(cameraStateNotRetry.type).isEqualTo(CameraState.Type.PENDING_OPEN)
        assertThat(cameraStateNotRetry.error?.code).isEqualTo(ERROR_OTHER_RECOVERABLE_ERROR)

        // Now we make sure we transition to OPENING and OPEN when the camera does eventually open
        // when it becomes available.
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPENING)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPEN)
    }

    @Test
    fun testStateTransitionsOnUnrecoverableErrorsWhenOpening() {
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPENING)

        cameraStateAdapter.onGraphStateUpdated(
            cameraGraph1,
            GraphStateError(CameraError.ERROR_CAMERA_DISABLED, willAttemptRetry = false),
        )
        val cameraState = cameraStateAdapter.cameraState.value!!
        assertThat(cameraState.type).isEqualTo(CameraState.Type.CLOSING)
        assertThat(cameraState.error?.code).isEqualTo(ERROR_CAMERA_DISABLED)
    }

    @Test
    fun testStateTransitionsOnRecoverableErrorsWhenOpen() {
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPEN)

        // We should transition to OPENING with an error code if we encounter errors during opening.
        cameraStateAdapter.onGraphStateUpdated(
            cameraGraph1,
            GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = false),
        )
        val cameraState = cameraStateAdapter.cameraState.value!!
        assertThat(cameraState.type).isEqualTo(CameraState.Type.PENDING_OPEN)
        assertThat(cameraState.error?.code).isEqualTo(ERROR_OTHER_RECOVERABLE_ERROR)
    }

    @Test
    fun testStateTransitionsOnUnrecoverableErrorsWhenOpen() {
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPEN)

        cameraStateAdapter.onGraphStateUpdated(
            cameraGraph1,
            GraphStateError(CameraError.ERROR_CAMERA_DISABLED, willAttemptRetry = false),
        )
        val cameraState = cameraStateAdapter.cameraState.value!!
        assertThat(cameraState.type).isEqualTo(CameraState.Type.CLOSING)
        assertThat(cameraState.error?.code).isEqualTo(ERROR_CAMERA_DISABLED)
    }

    @Test
    fun testStateTransitionsOnErrorsWhenClosing() {
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSED)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.OPEN)

        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStopping)
        assertThat(cameraStateAdapter.cameraState.value?.type).isEqualTo(CameraState.Type.CLOSING)

        // We should update the CLOSING state to include an error code.
        cameraStateAdapter.onGraphStateUpdated(
            cameraGraph1,
            GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = false),
        )
        val cameraState = cameraStateAdapter.cameraState.value!!
        assertThat(cameraState.type).isEqualTo(CameraState.Type.CLOSING)
        assertThat(cameraState.error?.code).isEqualTo(ERROR_OTHER_RECOVERABLE_ERROR)
    }

    @Test
    fun onRemoved_setsStateToClosedWithError() {
        // Arrange: Camera is in its initial CLOSED state.

        // Act: Signal a removal.
        cameraStateAdapter.onRemoved()
        ShadowLooper.idleMainLooper()

        val finalState = cameraStateAdapter.cameraState.value!!

        // Assert: The public state is CLOSED with the correct error.
        assertThat(finalState.type).isEqualTo(CameraState.Type.CLOSED)
        assertThat(finalState.error).isNotNull()
        assertThat(finalState.error!!.code).isEqualTo(ERROR_CAMERA_REMOVED)
        assertThat(cameraStateAdapter.cameraInternalState.liveData.value?.value)
            .isEqualTo(CameraInternal.State.CLOSED)
    }

    @Test
    fun onRemoved_fromOpenState_transitionsToClosedWithError() {
        // Arrange: Open the camera first.
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        assertThat(cameraStateAdapter.cameraState.value!!.type).isEqualTo(CameraState.Type.OPEN)

        // Act: Signal a removal.
        cameraStateAdapter.onRemoved()
        val finalState = cameraStateAdapter.cameraState.value!!

        // Assert: The public state immediately transitions from OPEN to CLOSED with the error.
        assertThat(finalState.type).isEqualTo(CameraState.Type.CLOSED)
        assertThat(finalState.error).isNotNull()
        assertThat(finalState.error!!.code).isEqualTo(ERROR_CAMERA_REMOVED)
    }

    @Test
    fun onGraphStateUpdated_isIgnoredAfterRemove() {
        // Arrange: Open the camera and then remove it.
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        cameraStateAdapter.onRemoved()
        val removedState = cameraStateAdapter.cameraState.value!!
        assertThat(removedState.type).isEqualTo(CameraState.Type.CLOSED)
        assertThat(removedState.error?.code).isEqualTo(ERROR_CAMERA_REMOVED)

        // Act: Try to send a stale graph event from the old graph.
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStopping)

        // Assert: The state remains unchanged, proving the event was ignored.
        val finalState = cameraStateAdapter.cameraState.value!!
        assertThat(finalState.type).isEqualTo(CameraState.Type.CLOSED)
        assertThat(finalState.error?.code).isEqualTo(ERROR_CAMERA_REMOVED)
    }

    @Test
    fun listenerIsCalled_whenStateChanges() {
        // Arrange
        val listener = TestStateListener()
        cameraStateAdapter.addCameraStateListener(MoreExecutors.directExecutor(), listener)
        ShadowLooper.idleMainLooper()

        // Act
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        ShadowLooper.idleMainLooper()

        // Assert
        assertThat(listener.states.last().type).isEqualTo(CameraState.Type.OPENING)

        // Act
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarted)
        ShadowLooper.idleMainLooper()

        // Assert
        assertThat(listener.states.last().type).isEqualTo(CameraState.Type.OPEN)
    }

    @Test
    fun listenerIsNotCalled_afterRemoval() {
        // Arrange
        val listener = TestStateListener()
        cameraStateAdapter.addCameraStateListener(MoreExecutors.directExecutor(), listener)
        cameraStateAdapter.removeCameraStateListener(listener)
        ShadowLooper.idleMainLooper()

        // Act
        cameraStateAdapter.onGraphUpdated(cameraGraph1)
        cameraStateAdapter.onGraphStateUpdated(cameraGraph1, GraphStateStarting)
        ShadowLooper.idleMainLooper()

        // Assert
        assertThat(listener.states).isEmpty()
    }

    @Test
    fun onRemoved_notifiesListener() {
        // Arrange
        val listener = TestStateListener()
        cameraStateAdapter.addCameraStateListener(MoreExecutors.directExecutor(), listener)
        ShadowLooper.idleMainLooper()

        // Act
        cameraStateAdapter.onRemoved()
        ShadowLooper.idleMainLooper()

        // Assert
        val finalState = listener.states.last()
        assertThat(finalState.type).isEqualTo(CameraState.Type.CLOSED)
        assertThat(finalState.error).isNotNull()
        assertThat(finalState.error!!.code).isEqualTo(ERROR_CAMERA_REMOVED)
    }

    private class TestStateListener : Consumer<CameraState> {
        val states = mutableListOf<CameraState>()

        override fun accept(value: CameraState) {
            states.add(value)
        }
    }
}
