/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe.framegraph

import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameCapture
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class PendingFrameCaptureTest {
    private val request = Request(streams = emptyList())
    private val pendingFrameCapture = PendingFrameCapture(request)

    @Test
    fun initialStatusIsPending() {
        assertThat(pendingFrameCapture.status).isEqualTo(OutputStatus.PENDING)
        assertThat(pendingFrameCapture.getFrame()).isNull()
    }

    @Test
    fun setFrameCapture_updatesStatusAndDelegatesGetFrame() {
        val mockFrame: Frame = mock()
        val mockCapture: FrameCapture = mock()
        whenever(mockCapture.status).thenReturn(OutputStatus.AVAILABLE)
        whenever(mockCapture.getFrame()).thenReturn(mockFrame)

        pendingFrameCapture.setFrameCapture(mockCapture)

        assertThat(pendingFrameCapture.status).isEqualTo(OutputStatus.AVAILABLE)
        assertThat(pendingFrameCapture.getFrame()).isEqualTo(mockFrame)
    }

    @Test
    fun addListener_beforeSetFrameCapture_isTransferred() {
        val listener: Frame.Listener = mock()
        val mockCapture: FrameCapture = mock()

        pendingFrameCapture.addListener(listener)
        pendingFrameCapture.setFrameCapture(mockCapture)

        // Verify the listener was passed to the real capture once it arrived.
        verify(mockCapture).addListener(listener)
        verify(listener, never()).onFrameComplete()
    }

    @Test
    fun addListener_afterSetFrameCapture_isDelegatedImmediately() {
        val listener: Frame.Listener = mock()
        val mockCapture: FrameCapture = mock()
        pendingFrameCapture.setFrameCapture(mockCapture)

        pendingFrameCapture.addListener(listener)

        verify(mockCapture).addListener(listener)
    }

    @Test
    fun awaitFrame_suspendsUntilSetFrameCapture() = runTest {
        val mockFrame: Frame = mock()
        val mockCapture: FrameCapture = mock()
        whenever(mockCapture.awaitFrame()).thenReturn(mockFrame)

        var resultFrame: Frame? = null
        val job = launch { resultFrame = pendingFrameCapture.awaitFrame() }

        advanceUntilIdle()
        assertThat(resultFrame).isNull()

        pendingFrameCapture.setFrameCapture(mockCapture)
        advanceUntilIdle()

        assertThat(resultFrame).isEqualTo(mockFrame)
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun close_setsStatusAndNotifiesPendingListeners() {
        val listener: Frame.Listener = mock()
        pendingFrameCapture.addListener(listener)

        pendingFrameCapture.close()

        assertThat(pendingFrameCapture.status).isEqualTo(OutputStatus.UNAVAILABLE)
        verify(listener).onFrameComplete()
    }

    @Test
    fun abort_setsStatusAndNotifiesPendingListeners() {
        val listener: Frame.Listener = mock()
        pendingFrameCapture.addListener(listener)

        pendingFrameCapture.abort()

        assertThat(pendingFrameCapture.status).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
        verify(listener).onFrameComplete()
    }

    @Test
    fun setFrameCapture_afterClose_closesTheProvidedCapture() {
        val mockCapture: FrameCapture = mock()

        pendingFrameCapture.close()
        pendingFrameCapture.setFrameCapture(mockCapture)

        // If the placeholder was already closed, any late-arriving capture should be closed
        // immediately.
        verify(mockCapture).close()
    }

    @Test
    fun addListener_afterClose_callsOnFrameCompleteImmediately() {
        val listener: Frame.Listener = mock()

        pendingFrameCapture.close()
        pendingFrameCapture.addListener(listener)

        verify(listener).onFrameComplete()
    }

    @Test
    fun awaitFrame_returnsNullWhenClosed() = runTest {
        launch { pendingFrameCapture.close() }

        val frame = pendingFrameCapture.awaitFrame()
        assertThat(frame).isNull()
    }
}
