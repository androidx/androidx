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

package androidx.camera.camera2.pipe.graph

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_LOCKED
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.tryAcquireToken
import androidx.camera.camera2.pipe.internal.FrameCaptureQueue
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraGraphSessionImplTest {

    private val graphState3A = GraphState3A()
    private val listener3A = Listener3A()
    private val graphProcessor =
        FakeGraphProcessor(
            graphState3A = graphState3A,
            graphListener3A = listener3A,
            defaultListeners = listOf(listener3A)
        )
    private val fakeCaptureSequenceProcessor = FakeCaptureSequenceProcessor()
    private val fakeGraphRequestProcessor = GraphRequestProcessor.from(fakeCaptureSequenceProcessor)
    private val controller3A =
        Controller3A(
            graphProcessor,
            // Make sure our characteristics shows that it supports AF trigger.
            FakeCameraMetadata(
                characteristics =
                    mapOf(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE to 1.0f)
            ),
            graphState3A,
            listener3A
        )
    private val frameCaptureQueue = FrameCaptureQueue()
    private val sessionMutex = Mutex()
    private val sessionToken = sessionMutex.tryAcquireToken()!!

    private val session =
        CameraGraphSessionImpl(sessionToken, graphProcessor, controller3A, frameCaptureQueue)

    @Test
    fun createCameraGraphSession() {
        assertThat(session).isNotNull()
    }

    @Test
    fun sessionCannotBeUsedAfterClose() {
        session.close()

        val result = assertThrows<IllegalStateException> { session.submit(Request(listOf())) }
        result.hasMessageThat().contains("submit")
    }

    @Test
    fun stopRepeatingShouldCancel3ARequests() = runTest {
        val streamId = StreamId(1)
        val surfaceTexture = SurfaceTexture(0).also { it.setDefaultBufferSize(640, 480) }
        val surface = Surface(surfaceTexture)
        graphProcessor.onGraphStarted(fakeGraphRequestProcessor)
        fakeCaptureSequenceProcessor.surfaceMap = mapOf(streamId to surface)

        session.startRepeating(Request(streams = listOf(StreamId(1))))
        graphProcessor.invalidate()

        val deferred = session.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE)

        assertThat(deferred.isCompleted).isFalse()

        // Don't return any results to simulate that the 3A conditions haven't been met, but the
        // app calls stopRepeating(). In which case, we should fail here with SUBMIT_CANCELLED.
        session.stopRepeating()
        assertThat(deferred.isCompleted).isTrue()
        val result = deferred.await()
        assertThat(result.status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
    }

    @Test
    fun initiate3ARequestsShouldThrowWhenSessionIsClosed() = runTest {
        graphProcessor.onGraphStarted(fakeGraphRequestProcessor)
        session.startRepeating(Request(streams = listOf(StreamId(1))))
        graphProcessor.invalidate()
        advanceUntilIdle()

        // Now close the session
        session.close()
        assertThrows<IllegalStateException> {
            session.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE)
        }
    }

    @Test
    fun lock3AShouldFailWhenInvokedBeforeStartRepeating() = runTest {
        graphProcessor.onGraphStarted(fakeGraphRequestProcessor)

        val afResult = session.lock3A(afLockBehavior = Lock3ABehavior.IMMEDIATE).await()
        assertThat(afResult.status).isEqualTo(Result3A.Status.SUBMIT_FAILED)

        val aeResult = session.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE).await()
        assertThat(aeResult.status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
    }

    @Test
    fun lock3AShouldSucceedWhenInvokedAfterStartRepeatingAndConverged() = runTest {
        val streamId = StreamId(1)
        val surfaceTexture = SurfaceTexture(0).also { it.setDefaultBufferSize(640, 480) }
        val surface = Surface(surfaceTexture)
        val requestMetadata = FakeRequestMetadata(requestNumber = RequestNumber(10))

        graphProcessor.onGraphStarted(fakeGraphRequestProcessor)
        fakeCaptureSequenceProcessor.surfaceMap = mapOf(streamId to surface)

        session.startRepeating(Request(streams = listOf(streamId)))
        graphProcessor.invalidate()
        advanceUntilIdle()

        val result = session.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE)
        advanceUntilIdle()

        listener3A.onTotalCaptureResult(
            requestMetadata,
            FrameNumber(10),
            FakeFrameInfo(
                metadata =
                    FakeFrameMetadata(
                        resultMetadata =
                            mapOf(CaptureResult.CONTROL_AE_STATE to CONTROL_AE_STATE_LOCKED)
                    ),
                requestMetadata = requestMetadata
            )
        )

        assertThat(result.await().status).isEqualTo(Result3A.Status.OK)
        surface.release()
        surfaceTexture.release()
    }

    @Test
    fun lock3AShouldFailWhenInvokedAfterStartAndStopRepeating() = runTest {
        val streamId = StreamId(1)
        val surfaceTexture = SurfaceTexture(0).also { it.setDefaultBufferSize(640, 480) }
        val surface = Surface(surfaceTexture)

        graphProcessor.onGraphStarted(fakeGraphRequestProcessor)
        fakeCaptureSequenceProcessor.surfaceMap = mapOf(streamId to surface)

        session.startRepeating(Request(streams = listOf(streamId)))
        graphProcessor.invalidate()
        advanceUntilIdle()

        // Stop repeating
        session.stopRepeating()

        // Now lock3A should fail immediately with SUBMIT_FAILED.
        val result = session.lock3A(afLockBehavior = Lock3ABehavior.IMMEDIATE).await()
        assertThat(result.status).isEqualTo(Result3A.Status.SUBMIT_FAILED)

        surface.release()
        surfaceTexture.release()
    }
}
