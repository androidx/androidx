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

import android.os.Build
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import androidx.camera.core.CameraState
import androidx.camera.core.impl.CameraInternal
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraStateAdapterTest {
    private val cameraStateAdapter = CameraStateAdapter()
    private val cameraGraph1 = FakeCameraGraph()
    private val cameraGraph2 = FakeCameraGraph()

    @Test
    fun testCalculateNextStateWhenClosed() {
        val nextStateWhenGraphStateStarting =
            cameraStateAdapter.calculateNextState(CameraInternal.State.CLOSED, GraphStateStarting)
        val nextStateWhenGraphStateStarted =
            cameraStateAdapter.calculateNextState(CameraInternal.State.CLOSED, GraphStateStarted)
        val nextStateWhenGraphStateStopping =
            cameraStateAdapter.calculateNextState(CameraInternal.State.CLOSED, GraphStateStopping)
        val nextStateWhenGraphStateStopped =
            cameraStateAdapter.calculateNextState(CameraInternal.State.CLOSED, GraphStateStopped)
        val nextStateWhenGraphStateError =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSED,
                GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, true)
            )

        assertThat(nextStateWhenGraphStateStarting!!.state).isEqualTo(CameraInternal.State.OPENING)
        assertThat(nextStateWhenGraphStateStarted!!.state).isEqualTo(CameraInternal.State.OPEN)
        assertThat(nextStateWhenGraphStateStopping).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStopped).isEqualTo(null)
        assertThat(nextStateWhenGraphStateError).isEqualTo(null)
    }

    @Test
    fun testCalculateNextStateWhenOpening() {
        val nextStateWhenGraphStateStarting =
            cameraStateAdapter.calculateNextState(CameraInternal.State.OPENING, GraphStateStarting)
        val nextStateWhenGraphStateStarted =
            cameraStateAdapter.calculateNextState(CameraInternal.State.OPENING, GraphStateStarted)
        val nextStateWhenGraphStateStopping =
            cameraStateAdapter.calculateNextState(CameraInternal.State.OPENING, GraphStateStopping)
        val nextStateWhenGraphStateStopped =
            cameraStateAdapter.calculateNextState(CameraInternal.State.OPENING, GraphStateStopped)
        val nextStateWhenGraphStateErrorWillRetry =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPENING,
                GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, willAttemptRetry = true)
            )
        // TODO(b/263201241): Add tests for PENDING_OPEN transitions.
        val nextStateWhenGraphStateErrorWillNotRetry =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPENING,
                GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, willAttemptRetry = false)
            )

        assertThat(nextStateWhenGraphStateStarting).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStarted!!.state).isEqualTo(CameraInternal.State.OPEN)
        assertThat(nextStateWhenGraphStateStopping!!.state).isEqualTo(CameraInternal.State.CLOSING)
        assertThat(nextStateWhenGraphStateStopped!!.state).isEqualTo(CameraInternal.State.CLOSED)
        assertThat(nextStateWhenGraphStateErrorWillRetry!!.state).isEqualTo(
            CameraInternal.State.OPENING
        )
        assertThat(nextStateWhenGraphStateErrorWillRetry.error!!.code).isEqualTo(
            CameraState.ERROR_MAX_CAMERAS_IN_USE
        )
        assertThat(nextStateWhenGraphStateErrorWillNotRetry!!.state).isEqualTo(
            CameraInternal.State.CLOSING
        )
        assertThat(nextStateWhenGraphStateErrorWillNotRetry.error!!.code).isEqualTo(
            CameraState.ERROR_MAX_CAMERAS_IN_USE
        )
    }

    @Test
    fun testCalculateNextStateWhenOpen() {
        val nextStateWhenGraphStateStarting =
            cameraStateAdapter.calculateNextState(CameraInternal.State.OPEN, GraphStateStarting)
        val nextStateWhenGraphStateStarted =
            cameraStateAdapter.calculateNextState(CameraInternal.State.OPEN, GraphStateStarted)
        val nextStateWhenGraphStateStopping =
            cameraStateAdapter.calculateNextState(CameraInternal.State.OPEN, GraphStateStopping)
        val nextStateWhenGraphStateStopped =
            cameraStateAdapter.calculateNextState(CameraInternal.State.OPEN, GraphStateStopped)
        // TODO(b/263201241): Add tests when we handle errors while camera is already opened.
        val nextStateWhenGraphStateError =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.OPEN,
                GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, true)
            )

        assertThat(nextStateWhenGraphStateStarting).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStarted).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStopping!!.state).isEqualTo(CameraInternal.State.CLOSING)
        assertThat(nextStateWhenGraphStateStopped!!.state).isEqualTo(CameraInternal.State.CLOSED)
        assertThat(nextStateWhenGraphStateError).isEqualTo(null)
    }

    @Test
    fun testCalculateNextStateWhenClosing() {
        val nextStateWhenGraphStateStarting =
            cameraStateAdapter.calculateNextState(CameraInternal.State.CLOSING, GraphStateStarting)
        val nextStateWhenGraphStateStarted =
            cameraStateAdapter.calculateNextState(CameraInternal.State.CLOSING, GraphStateStarted)
        val nextStateWhenGraphStateStopping =
            cameraStateAdapter.calculateNextState(CameraInternal.State.CLOSING, GraphStateStopping)
        val nextStateWhenGraphStateStopped =
            cameraStateAdapter.calculateNextState(CameraInternal.State.CLOSING, GraphStateStopped)
        val nextStateWhenGraphStateError =
            cameraStateAdapter.calculateNextState(
                CameraInternal.State.CLOSING,
                GraphStateError(CameraError.ERROR_CAMERA_LIMIT_EXCEEDED, true)
            )

        assertThat(nextStateWhenGraphStateStarting!!.state).isEqualTo(CameraInternal.State.OPENING)
        assertThat(nextStateWhenGraphStateStarted).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStopping).isEqualTo(null)
        assertThat(nextStateWhenGraphStateStopped!!.state).isEqualTo(CameraInternal.State.CLOSED)
        assertThat(nextStateWhenGraphStateError).isEqualTo(null)
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
}