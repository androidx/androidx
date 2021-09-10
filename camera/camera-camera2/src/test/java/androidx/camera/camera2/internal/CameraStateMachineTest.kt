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
package androidx.camera.camera2.internal

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.camera.core.CameraState
import androidx.camera.core.CameraState.ERROR_CAMERA_IN_USE
import androidx.camera.core.CameraState.ERROR_MAX_CAMERAS_IN_USE
import androidx.camera.core.CameraState.StateError
import androidx.camera.core.CameraState.Type
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraStateRegistry
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCamera
import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraStateMachineTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /** Wrapper method that initializes the required test parameters, then runs the test's body. */
    private fun runTest(body: (CameraStateMachine, StateObserver) -> Unit) {
        val registry = CameraStateRegistry(1)
        val stateMachine = CameraStateMachine(registry)
        val stateObserver = StateObserver()
        stateMachine.stateLiveData.observeForever(stateObserver)

        // Test body
        body.invoke(stateMachine, stateObserver)

        stateMachine.stateLiveData.removeObserver(stateObserver)
    }

    @Test
    fun shouldEmitClosedStateInitially() =
        runTest { _, stateObserver ->
            stateObserver
                .assertHasState(CameraState.create(Type.CLOSED))
                .assertHasNoMoreStates()
        }

    @Test
    fun shouldNotEmitNewState_whenStateHasNotChanged() =
        runTest { stateMachine, stateObserver ->
            stateMachine.updateState(CameraInternal.State.OPENING, null)
            stateMachine.updateState(CameraInternal.State.OPENING, null)

            stateObserver
                .assertHasState(CameraState.create(Type.CLOSED))
                .assertHasState(CameraState.create(Type.OPENING))
                .assertHasNoMoreStates()
        }

    @Test
    fun shouldNotEmitNewState_whenStateAndErrorHaveNotChanged() =
        runTest { stateMachine, stateObserver ->
            stateMachine.updateState(
                CameraInternal.State.OPENING,
                StateError.create(ERROR_CAMERA_IN_USE)
            )
            stateMachine.updateState(
                CameraInternal.State.OPENING,
                StateError.create(ERROR_CAMERA_IN_USE)
            )

            stateObserver
                .assertHasState(CameraState.create(Type.CLOSED))
                .assertHasState(
                    CameraState.create(
                        Type.OPENING,
                        StateError.create(ERROR_CAMERA_IN_USE)
                    )
                )
                .assertHasNoMoreStates()
        }

    @Test
    fun shouldEmitNewState_whenStateChanges() =
        runTest { stateMachine, stateObserver ->
            stateMachine.updateState(CameraInternal.State.OPENING, null)
            stateMachine.updateState(CameraInternal.State.OPEN, null)

            stateObserver
                .assertHasState(CameraState.create(Type.CLOSED))
                .assertHasState(CameraState.create(Type.OPENING))
                .assertHasState(CameraState.create(Type.OPEN))
                .assertHasNoMoreStates()
        }

    @Test
    fun shouldEmitNewState_whenErrorChanges() =
        runTest { stateMachine, stateObserver ->
            stateMachine.updateState(
                CameraInternal.State.OPENING,
                StateError.create(ERROR_CAMERA_IN_USE)
            )
            stateMachine.updateState(
                CameraInternal.State.OPENING,
                StateError.create(ERROR_MAX_CAMERAS_IN_USE)
            )

            stateObserver
                .assertHasState(CameraState.create(Type.CLOSED))
                .assertHasState(
                    CameraState.create(
                        Type.OPENING,
                        StateError.create(ERROR_CAMERA_IN_USE)
                    )
                )
                .assertHasState(
                    CameraState.create(
                        Type.OPENING,
                        StateError.create(ERROR_MAX_CAMERAS_IN_USE)
                    )
                )
                .assertHasNoMoreStates()
        }

    @Test
    fun shouldEmitOpeningState_whenCameraIsOpening_whileAnotherIsClosing() {
        val registry = CameraStateRegistry(1)
        val stateMachine = CameraStateMachine(registry)

        // Create, open then start closing first camera
        val camera1 = FakeCamera()
        registry.registerCamera(camera1, CameraXExecutors.directExecutor(), {})
        registry.tryOpenCamera(camera1)
        registry.markCameraState(camera1, CameraInternal.State.OPEN)
        registry.markCameraState(camera1, CameraInternal.State.CLOSING)

        // Create and try to open second camera. Since the first camera is still closing, its
        // internal state will move to PENDING_OPEN
        val camera2 = FakeCamera()
        registry.registerCamera(camera2, CameraXExecutors.directExecutor(), {})
        registry.tryOpenCamera(camera2)
        registry.markCameraState(camera2, CameraInternal.State.PENDING_OPEN)

        // Get the second camera's public state
        stateMachine.updateState(CameraInternal.State.PENDING_OPEN, null)
        val newState = stateMachine.stateLiveData.value
        assertThat(newState).isEqualTo(CameraState.create(Type.OPENING))
    }

    class StateObserver : Observer<CameraState> {

        private val states = mutableListOf<CameraState>()
        private var index = 0

        override fun onChanged(state: CameraState?) {
            if (state != null) {
                states.add(state)
            }
        }

        fun assertHasState(expectedState: CameraState): StateObserver {
            val state = states[index++]
            assertThat(state).isEqualTo(expectedState)
            return this
        }

        fun assertHasNoMoreStates() {
            assertThat(index).isEqualTo(states.size)
        }
    }
}