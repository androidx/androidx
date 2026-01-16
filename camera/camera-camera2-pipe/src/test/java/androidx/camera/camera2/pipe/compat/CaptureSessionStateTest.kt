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
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.Flags.FinalizeSessionOnCloseBehavior
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.CaptureSequenceProcessor
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.StrictMode
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequence
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeCaptureSessionFactory
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
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
                streamToSurfaceMap: Map<StreamId, Surface>,
                outputToSurfaceMap: Map<OutputId, Surface>,
            ): CaptureSequenceProcessor<Request, FakeCaptureSequence> = fakeCaptureSequenceProcessor
        }
    private val timeSource = SystemTimeSource()
    private val cameraGraphFlags =
        CameraGraph.Flags(
            finalizeSessionOnCloseBehavior = FinalizeSessionOnCloseBehavior.OFF,
            closeCaptureSessionOnDisconnect = false,
        )

    private val surface1: Surface = Surface(SurfaceTexture(1))
    private val surface2: Surface = Surface(SurfaceTexture(2))

    private val cameraId = CameraId("1")
    private val streamConfig1 =
        CameraStream.Config.create(Size(1280, 720), StreamFormat.YUV_420_888, cameraId)
    private val streamConfig2 =
        CameraStream.Config.create(Size(1280, 720), StreamFormat.JPEG, cameraId)
    private val streamConfig3 =
        CameraStream.Config.create(
            Size(1280, 720),
            StreamFormat.UNKNOWN,
            cameraId,
            OutputStream.OutputType.SURFACE_VIEW,
        )
    private val graphConfig =
        CameraGraph.Config(cameraId, listOf(streamConfig1, streamConfig2, streamConfig3))

    private val fakeCameraMetadata = FakeCameraMetadata(cameraId = cameraId)
    private val streamGraph: StreamGraph =
        StreamGraphImpl(fakeCameraMetadata, graphConfig, mock(), mock())

    private val stream1: StreamId = streamGraph[streamConfig1]!!.id
    private val stream2: StreamId = streamGraph[streamConfig2]!!.id
    private val stream3Deferred: StreamId = streamGraph[streamConfig3]!!.id

    private val captureSessionFactory =
        FakeCaptureSessionFactory(
            requiredStreams = setOf(stream1, stream2),
            deferrableStreams = setOf(stream3Deferred),
        )

    private val fakeCameraDevice: CameraDeviceWrapper = mock()
    private val fakeCaptureSession: CameraCaptureSessionWrapper = mock()

    @After
    fun teardown() {
        surface1.release()
        surface2.release()
    }

    @Test
    fun shutdownBeforeCameraDoesNotAcceptCamera() = runTest {
        val fakeThreads = FakeThreads.fromTestScope(this)
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                cameraGraphFlags,
                concurrentSessionSequencer = null,
                streamGraph,
                StrictMode(true),
                fakeThreads,
                this,
            )
        // When disconnect is called first
        state.shutdown()

        // Setting a camera device has no effect
        state.cameraDevice = fakeCameraDevice

        // And a captureSession is never created
        advanceUntilIdle()
        verify(fakeGraphListener, times(1)).onGraphStopped(isNull())
    }

    @Test
    fun shutdownBeforeCameraCallsSurfaceListener() = runTest {
        val fakeThreads = FakeThreads.fromTestScope(this)
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                cameraGraphFlags,
                concurrentSessionSequencer = null,
                streamGraph,
                StrictMode(true),
                fakeThreads,
                this,
            )

        // When surfaces are configured
        state.configureSurfaceMap(mapOf(stream1 to surface1, stream2 to surface2))
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(surface2))

        // And a device is never set
        state.shutdown()

        // Then fakeSurfaceListener marks surfaces as inactive.
        advanceUntilIdle()
        verify(fakeGraphListener, times(1)).onGraphStopped(isNull())
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface2))
    }

    @Test
    fun shutdownAfterCaptureSessionDoesNotCallOnSurfaceInactive() = runTest {
        val fakeThreads = FakeThreads.fromTestScope(this)
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                cameraGraphFlags,
                concurrentSessionSequencer = null,
                streamGraph,
                StrictMode(true),
                fakeThreads,
                this,
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
        state.shutdown()

        // Then fakeSurfaceListener does not mark surfaces as inactive.
        advanceUntilIdle()
        verify(fakeGraphListener, times(1)).onGraphStopped(isNull())
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(surface1))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(surface2))
    }

    @Test
    fun onSessionFinalizeCallsSurfaceListener() = runTest {
        val fakeThreads = FakeThreads.fromTestScope(this)
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                cameraGraphFlags,
                concurrentSessionSequencer = null,
                streamGraph,
                StrictMode(true),
                fakeThreads,
                this,
            )
        // When surfaces are configured
        state.configureSurfaceMap(mapOf(stream1 to surface1, stream2 to surface2))
        // And session is finalized
        state.onSessionFinalized()

        // Then fakeSurfaceListener marks surfaces as inactive.
        advanceUntilIdle()
        verify(fakeGraphListener, times(1)).onGraphStopped(isNull())
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface2))
    }

    @Test
    fun onConfigureFailedCallsSurfaceListener() = runTest {
        val fakeThreads = FakeThreads.fromTestScope(this)
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                cameraGraphFlags,
                concurrentSessionSequencer = null,
                streamGraph,
                StrictMode(true),
                fakeThreads,
                this,
            )
        // When surfaces are configured
        state.configureSurfaceMap(mapOf(stream1 to surface1, stream2 to surface2))
        // And configuration fails
        state.onConfigureFailed(fakeCaptureSession)

        // Then fakeSurfaceListener marks surfaces as inactive.
        advanceUntilIdle()
        verify(fakeGraphListener, times(1)).onGraphError(any())
        verify(fakeGraphListener, times(1)).onGraphStopped(isNull())
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface2))
    }

    @Test
    fun onClosedCallsSurfaceListener() = runTest {
        val fakeThreads = FakeThreads.fromTestScope(this)
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                cameraGraphFlags,
                concurrentSessionSequencer = null,
                streamGraph,
                StrictMode(true),
                fakeThreads,
                this,
            )
        // When surfaces are configured
        state.configureSurfaceMap(mapOf(stream1 to surface1, stream2 to surface2))
        // And the capture session is closed
        state.onClosed(fakeCaptureSession)

        // Then fakeSurfaceListener marks surfaces as inactive.
        advanceUntilIdle()
        verify(fakeGraphListener, times(1)).onGraphStopped(isNull())
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(surface2))
    }

    @Test
    fun captureSessionStateClosesCaptureSessionWhenQuirkIsEnabled() = runTest {
        val fakeThreads = FakeThreads.fromTestScope(this, Dispatchers.IO)
        val state =
            CaptureSessionState(
                fakeGraphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                CameraGraph.Flags(closeCaptureSessionOnDisconnect = true),
                concurrentSessionSequencer = null,
                streamGraph,
                StrictMode(false),
                fakeThreads,
                this,
            )

        // When surfaces are configured
        state.configureSurfaceMap(mapOf(stream1 to surface1, stream2 to surface2))
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(surface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(surface2))

        // And a device is set
        state.cameraDevice = fakeCameraDevice

        // Advance to make sure a capture session is created.
        advanceUntilIdle()

        // Feed a fake capture session
        state.onConfigured(fakeCaptureSession)

        // And the state is then disconnected
        state.shutdown()

        // Then make sure we do close the capture session.
        advanceUntilIdle()
        verify(fakeCaptureSession, times(1)).close()
    }
}
