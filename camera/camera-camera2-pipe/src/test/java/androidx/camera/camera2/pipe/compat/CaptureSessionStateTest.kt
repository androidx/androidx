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

package androidx.camera.camera2.pipe.compat

import android.graphics.SurfaceTexture
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.CaptureSequenceProcessor
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.testing.FakeCaptureSequence
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeCaptureSessionFactory
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CaptureSessionStateTest {
    private val fakeGraphListener: GraphListener = mock()
    private val fakeSurfaceListener: CameraSurfaceManager.SurfaceListener = mock()
    private val cameraSurfaceManager =
        CameraSurfaceManager().also { it.addListener(fakeSurfaceListener) }
    private val fakeCaptureSequenceProcessor = FakeCaptureSequenceProcessor()
    private val captureSequenceProcessorFactory =
        object : Camera2CaptureSequenceProcessorFactory {
            override fun create(
                session: CameraCaptureSessionWrapper,
                surfaceMap: Map<StreamId, Surface>
            ): CaptureSequenceProcessor<Request, FakeCaptureSequence> = fakeCaptureSequenceProcessor
        }
    private val timeSource = SystemTimeSource()

    private val surface1: Surface = Surface(SurfaceTexture(1))
    private val surface2: Surface = Surface(SurfaceTexture(2))
    private val stream1: StreamId = StreamId(1)
    private val stream2: StreamId = StreamId(2)
    private val stream3Deferred: StreamId = StreamId(3)

    private val captureSessionFactory =
        FakeCaptureSessionFactory(
            requiredStreams = setOf(stream1, stream2), deferrableStreams = setOf(stream3Deferred)
        )

    private val fakeCameraDevice: CameraDeviceWrapper = mock()
    private val fakeCaptureSession: CameraCaptureSessionWrapper = mock()

    @After
    fun teardown() {
        surface1.release()
        surface2.release()
    }

    @Test
    fun disconnectBeforeCameraDoesNotAcceptCamera() = runTest {
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                this
            )
        // When disconnect is called first
        state.disconnect()

        // Setting a camera device has no effect
        state.cameraDevice = fakeCameraDevice

        // And a captureSession is never created
        advanceUntilIdle()
        verifyNoInteractions(fakeGraphListener)
    }

    @Test
    fun disconnectBeforeCameraCallsSurfaceListener() = runTest {
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                this
            )

        // When surfaces are configured
        state.configureSurfaceMap(mapOf(stream1 to surface1, stream2 to surface2))
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(surface2))

        // And a device is never set
        state.disconnect()

        // Then fakeSurfaceListener marks surfaces as inactive.
        advanceUntilIdle()
        verifyNoInteractions(fakeGraphListener)
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface2))
    }

    @Test
    fun disconnectAfterCaptureSessionDoesNotCallOnSurfaceInactive() = runTest {
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                this
            )

        // When surfaces are configured
        state.configureSurfaceMap(mapOf(stream1 to surface1, stream2 to surface2))
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(surface2))

        // And a device is set
        state.cameraDevice = fakeCameraDevice

        // Advance to make sure a capture session is created.
        advanceUntilIdle()

        // And the state is then disconnected
        state.disconnect()

        // Then fakeSurfaceListener does not mark surfaces as inactive.
        advanceUntilIdle()
        verifyNoInteractions(fakeGraphListener)
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(surface1))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(surface2))
    }

    @Test
    fun onSessionFinalizeCallsSurfaceListener() = runTest {
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                this
            )
        // When surfaces are configured
        state.configureSurfaceMap(mapOf(stream1 to surface1, stream2 to surface2))
        // And session is finalized
        state.onSessionFinalized()

        // Then fakeSurfaceListener marks surfaces as inactive.
        advanceUntilIdle()
        verifyNoInteractions(fakeGraphListener)
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface2))
    }

    @Test
    fun onConfigureFailedCallsSurfaceListener() = runTest {
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                this
            )
        // When surfaces are configured
        state.configureSurfaceMap(mapOf(stream1 to surface1, stream2 to surface2))
        // And configuration fails
        state.onConfigureFailed(fakeCaptureSession)

        // Then fakeSurfaceListener marks surfaces as inactive.
        advanceUntilIdle()
        verifyNoInteractions(fakeGraphListener)
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface2))
    }

    @Test
    fun onClosedCallsSurfaceListener() = runTest {
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                this
            )
        // When surfaces are configured
        state.configureSurfaceMap(mapOf(stream1 to surface1, stream2 to surface2))
        // And the capture session is closed
        state.onClosed(fakeCaptureSession)

        // Then fakeSurfaceListener marks surfaces as inactive.
        advanceUntilIdle()
        verifyNoInteractions(fakeGraphListener)
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface2))
    }
}
