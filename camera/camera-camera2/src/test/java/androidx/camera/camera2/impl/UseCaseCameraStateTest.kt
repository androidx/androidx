/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.impl

import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW
import androidx.camera.camera2.adapter.CameraStateAdapter
import androidx.camera.camera2.adapter.GraphStateToCameraStateAdapter
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.adapter.asListenableFuture
import androidx.camera.camera2.compat.quirk.CaptureIntentPreviewQuirk
import androidx.camera.camera2.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.compat.workaround.TemplateParamsQuirkOverride
import androidx.camera.camera2.config.UseCaseCameraContext
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.testing.FakeCameraGraph
import androidx.camera.camera2.testing.FakeCameraGraphSession
import androidx.camera.camera2.testing.FakeCameraGraphSession.RequestStatus.ABORTED
import androidx.camera.camera2.testing.FakeCameraGraphSession.RequestStatus.FAILED
import androidx.camera.camera2.testing.FakeCameraGraphSession.RequestStatus.TOTAL_CAPTURE_DONE
import androidx.camera.camera2.testing.FakeSurface
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.Quirks
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [Config.ALL_SDKS])
class UseCaseCameraStateTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
    private val testExecutor = Executors.newFixedThreadPool(4)
    private val testIoDispatcher = testExecutor.asCoroutineDispatcher()

    @get:Rule val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val surface = FakeSurface()
    private val surfaceToStreamMap: Map<DeferrableSurface, StreamId> = mapOf(surface to StreamId(0))

    private val fakeCameraGraphSession = FakeCameraGraphSession()
    private val fakeCameraGraph = FakeCameraGraph(fakeCameraGraphSession)
    val cameraStateAdapter = CameraStateAdapter()
    val fakeUseCaseCameraContext =
        UseCaseCameraContext(
            cameraGraphProvider = { fakeCameraGraph },
            cameraStateAdapter = cameraStateAdapter,
            graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter),
            streamConfigMapProvider = { emptyMap() },
            defaultSurfaceToStreamMap = surfaceToStreamMap,
        )

    private val useCaseCameraState =
        UseCaseCameraState(
            useCaseCameraContext = fakeUseCaseCameraContext,
            templateParamsOverride = NoOpTemplateParamsOverride,
        )

    @Before
    fun setUp() {
        fakeCameraGraphSession.startRepeatingSignal = CompletableDeferred() // not complete yet
    }

    @After
    fun tearDown() {
        surface.close()
        testExecutor.shutdown()
    }

    @Test
    fun updateAsyncCompletes_whenStopRepeating(): Unit = runBlocking {
        // stopRepeating is called when there is no stream after updateAsync call
        val result = useCaseCameraState.updateAsync(streams = emptySet()).asListenableFuture()

        assertFutureCompletes(result)
    }

    @Test
    fun updateAsyncCompletes_whenStartRepeating(): Unit = runBlocking {
        // startRepeating is called when there is at least one stream after updateAsync call
        val result =
            useCaseCameraState.updateAsync(streams = setOf(StreamId(0))).asListenableFuture()

        // simulate startRepeating request being completed in camera
        fakeCameraGraphSession.startRepeatingSignal.complete(TOTAL_CAPTURE_DONE)

        assertFutureCompletes(result)
    }

    @Test
    fun updateAsyncFails_whenStartRepeatingRequestFails(): Unit = runBlocking {
        // startRepeating is called when there is at least one stream after updateAsync call
        val result =
            useCaseCameraState.updateAsync(streams = setOf(StreamId(0))).asListenableFuture()

        // simulate startRepeating request failing in camera framework level
        fakeCameraGraphSession.startRepeatingSignal.complete(FAILED)

        assertFutureFails(result)
    }

    @Test
    fun updateAsyncIncomplete_whenStartRepeatingRequestIsAborted(): Unit = runTest {
        // startRepeating is called when there is at least one stream after updateAsync call
        val result =
            useCaseCameraState.updateAsync(streams = setOf(StreamId(0))).asListenableFuture()

        // simulate startRepeating request being aborted by camera framework level
        fakeCameraGraphSession.startRepeatingSignal.complete(ABORTED)

        advanceUntilIdle()
        assertFutureStillWaiting(result)
    }

    @Test
    fun updateAsyncIncomplete_whenNewRequestSubmitted(): Unit = runTest {
        // startRepeating is called when there is at least one stream after updateAsync call
        val result =
            useCaseCameraState.updateAsync(streams = setOf(StreamId(0))).asListenableFuture()

        // simulate startRepeating request being aborted by camera framework level
        fakeCameraGraphSession.startRepeatingSignal.complete(ABORTED)
        advanceUntilIdle()

        // simulate startRepeating being called again
        fakeCameraGraphSession.startRepeatingSignal = CompletableDeferred() // reset
        useCaseCameraState.updateAsync(streams = setOf(StreamId(0)))

        advanceUntilIdle()
        assertFutureStillWaiting(result)
    }

    @Test
    fun previousUpdateAsyncCompletes_whenNewStartRepeatingRequestCompletesAfterAbort(): Unit =
        runTest {
            // startRepeating is called when there is at least one stream after updateAsync call
            val result =
                useCaseCameraState.updateAsync(streams = setOf(StreamId(0))).asListenableFuture()

            // simulate startRepeating request being aborted by camera framework level
            fakeCameraGraphSession.startRepeatingSignal.complete(ABORTED)
            advanceUntilIdle()

            // simulate startRepeating being called again
            fakeCameraGraphSession.startRepeatingSignal = CompletableDeferred() // reset
            useCaseCameraState.updateAsync(streams = setOf(StreamId(0)))
            fakeCameraGraphSession.startRepeatingSignal.complete(TOTAL_CAPTURE_DONE) // completed

            assertFutureCompletes(result)
        }

    @Test
    fun previousUpdateAsyncCompletes_whenInvokedTwice(): Unit = runBlocking {
        // startRepeating is called when there is at least one stream after updateAsync call
        val result =
            useCaseCameraState.updateAsync(streams = setOf(StreamId(0))).asListenableFuture()

        useCaseCameraState.updateAsync(streams = setOf(StreamId(1))).asListenableFuture()

        // simulate startRepeating request being completed in camera
        fakeCameraGraphSession.startRepeatingSignal.complete(TOTAL_CAPTURE_DONE)

        assertFutureCompletes(result)
    }

    @Test
    fun updateAsync_overrideTemplateParams(): Unit = runBlocking {
        val useCaseCameraState =
            UseCaseCameraState(
                useCaseCameraContext = fakeUseCaseCameraContext,
                templateParamsOverride =
                    TemplateParamsQuirkOverride(
                        Quirks(listOf(object : CaptureIntentPreviewQuirk {}))
                    ),
            )

        // startRepeating is called when there is at least one stream after updateAsync call
        val template = RequestTemplate(TEMPLATE_RECORD)
        val result =
            useCaseCameraState
                .updateAsync(streams = setOf(StreamId(0)), template = template)
                .asListenableFuture()

        // simulate startRepeating request being completed in camera
        fakeCameraGraphSession.startRepeatingSignal.complete(TOTAL_CAPTURE_DONE)

        assertFutureCompletes(result)

        assertThat(fakeCameraGraphSession.repeatingRequests.size).isEqualTo(1)
        val request = fakeCameraGraphSession.repeatingRequests[0]
        assertThat(request.template).isEqualTo(template)
        assertThat(request[CONTROL_CAPTURE_INTENT]).isEqualTo(CONTROL_CAPTURE_INTENT_PREVIEW)
    }

    @Test
    fun updateCameraState_completesAllSignals_underHighConcurrentLoad() = runBlocking {
        // --- Setup ---
        val fakeSession = ControllableFakeCameraGraphSession()
        val fakeGraph = FakeCameraGraph(fakeSession)
        val cameraStateAdapter = CameraStateAdapter()
        val useCaseCameraContext =
            UseCaseCameraContext(
                cameraGraphProvider = { fakeGraph },
                cameraStateAdapter = cameraStateAdapter,
                graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter),
                streamConfigMapProvider = { emptyMap() },
                defaultSurfaceToStreamMap = surfaceToStreamMap,
            )
        val useCaseCameraState =
            UseCaseCameraState(useCaseCameraContext, NoOpTemplateParamsOverride)

        // --- Act ---
        val jobCount = 500
        val deferredList = mutableListOf<Deferred<Unit>>()

        withContext(testIoDispatcher) {
            repeat(jobCount) {
                launch {
                    val deferred =
                        useCaseCameraState.updateAsync(
                            parameters =
                                mapOf(
                                    CaptureRequest.CONTROL_AF_MODE to
                                        CaptureRequest.CONTROL_AF_MODE_AUTO
                                )
                        )
                    synchronized(deferredList) { deferredList.add(deferred) }
                }
            }
        }

        // --- Assert ---
        deferredList.joinAll()
        val exceptions = deferredList.mapNotNull { it.getCompletionExceptionOrNull() }
        assertThat(exceptions).isEmpty()
    }

    @Test(timeout = 2000L) // Fail this test if it deadlocks
    fun onTotalCaptureResult_fastPath_returnsImmediately_whenLockIsHeld() = runBlocking {
        // --- Setup ---
        val blocker = Semaphore(0)

        val fakeSession =
            ControllableFakeCameraGraphSession().apply { startRepeatingBlocker = blocker }
        val fakeGraph = FakeCameraGraph(fakeSession)
        val cameraStateAdapter = CameraStateAdapter()
        val useCaseCameraContext =
            UseCaseCameraContext(
                cameraGraphProvider = { fakeGraph },
                cameraStateAdapter = cameraStateAdapter,
                graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter),
                streamConfigMapProvider = { emptyMap() },
                defaultSurfaceToStreamMap = surfaceToStreamMap,
            )
        val useCaseCameraState =
            UseCaseCameraState(useCaseCameraContext, NoOpTemplateParamsOverride)
        // --- Act ---
        val lockHoldingJob =
            launch(testIoDispatcher) {
                useCaseCameraState.updateAsync(
                    streams = setOf(StreamId(0)),
                    parameters =
                        mapOf(CaptureRequest.CONTROL_AF_MODE to CaptureRequest.CONTROL_AF_MODE_AUTO),
                )
            }

        delay(250) // Allow time for lockHoldingJob to acquire the UseCaseCameraState lock
        assertThat(lockHoldingJob.isActive).isTrue()

        val capturedListener = fakeSession.lastCapturedListener
        assertThat(capturedListener).isNotNull()
        val frameNumber = FrameNumber(0)
        val frameInfo = FakeFrameInfo()
        val requestMetadata = FakeRequestMetadata()

        val fastPathJob = launch {
            capturedListener!!.onTotalCaptureResult(requestMetadata, frameNumber, frameInfo)
        }

        // --- Assert ---
        fastPathJob.join()

        // --- Cleanup ---
        blocker.release()
        lockHoldingJob.cancel()
    }

    private fun <T> assertFutureCompletes(future: ListenableFuture<T>) {
        future[3, TimeUnit.SECONDS]
    }

    private fun <T> assertFutureFails(future: ListenableFuture<T>) {
        assertThrows(ExecutionException::class.java) { future[3, TimeUnit.SECONDS] }
    }

    private fun <T> assertFutureStillWaiting(future: ListenableFuture<T>) {
        assertWithMessage("Future already completed instead of waiting")
            .that(future.isDone)
            .isFalse()
    }

    /**
     * A test-specific fake session that adds asynchronous callbacks and blocking capabilities to
     * simulate race conditions and lock-holding.
     */
    internal class ControllableFakeCameraGraphSession : FakeCameraGraphSession() {
        var startRepeatingBlocker: Semaphore? = null
        var lastCapturedListener: Request.Listener? = null

        override fun startRepeating(request: Request) {
            lastCapturedListener =
                request.listeners.firstOrNull {
                    it::class.java.enclosingClass?.simpleName == "UseCaseCameraState"
                }

            startRepeatingBlocker?.acquire()
            super.startRepeating(request)
        }
    }
}
