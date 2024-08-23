/*
 * Copyright 2024 The Android Open Source Project
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

import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CaptureSequence
import androidx.camera.camera2.pipe.CaptureSequenceProcessor
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequence
import androidx.camera.camera2.pipe.testing.FakeMetadata.Companion.TEST_KEY
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class GraphLoopTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
    private val shutdownScope = CoroutineScope(testDispatcher)

    private val graphState3A = GraphState3A()
    private val listener3A = Listener3A()
    private val defaultParameters = emptyMap<Any, Any?>()
    private val requiredParameters = emptyMap<Any, Any?>()
    private val mockListener: Request.Listener = mock<Request.Listener>()

    private val fakeCameraMetadata = FakeCameraMetadata()
    private val fakeCameraId = fakeCameraMetadata.camera
    private val stream1 = StreamId(1)
    private val stream2 = StreamId(2)
    private val fakeSurfaces = FakeSurfaces()
    private val surfaceMap =
        mapOf(
            stream1 to fakeSurfaces.createFakeSurface(),
            stream2 to fakeSurfaces.createFakeSurface()
        )

    private val csp1 = SimpleCSP(fakeCameraId, surfaceMap)
    private val csp2 = SimpleCSP(fakeCameraId, surfaceMap)

    private val grp1 = GraphRequestProcessor.from(csp1)
    private val grp2 = GraphRequestProcessor.from(csp2)

    private val request1 = Request(streams = listOf(stream1))
    private val request2 = Request(streams = listOf(stream2))
    private val request3 = Request(streams = listOf(stream1, stream2))
    private val cameraGraphId = CameraGraphId.nextId()

    private val graphLoop =
        GraphLoop(
            cameraGraphId = cameraGraphId,
            defaultParameters = defaultParameters,
            requiredParameters = requiredParameters,
            graphListeners = listOf(mockListener),
            graphState3A = graphState3A,
            listeners = listOf(listener3A),
            shutdownScope = shutdownScope,
            dispatcher = testDispatcher,
        )

    @After
    fun teardown() {
        fakeSurfaces.close()
        shutdownScope.cancel()
    }

    @Test
    fun graphLoopSubmitsRequests() =
        testScope.runTest {
            graphLoop.submit(listOf(request1))
            graphLoop.submit(listOf(request2))
            graphLoop.requestProcessor = grp1
            assertThat(csp1.events).isEmpty()

            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isCapture).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)
        }

    @Test
    fun abortRemovesPendingRequests() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.submit(listOf(request1))
            graphLoop.abort()
            assertThat(csp1.events).isEmpty()

            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isAbort).isTrue()
        }

    @Test
    fun abortBeforeRequestProcessorDoesNotInvokeAbortOnRequestProcessor() =
        testScope.runTest {
            graphLoop.submit(listOf(request1))
            graphLoop.abort()
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            assertThat(csp1.events).isEmpty()

            graphLoop.submit(listOf(request2))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].requests).containsExactly(request2)
        }

    @Test
    fun repeatingRequestsCanBeSkipped() =
        testScope.runTest {
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = request2
            graphLoop.requestProcessor = grp1
            assertThat(csp1.events).isEmpty()
            advanceUntilIdle()
            assertThat(csp1.events.size).isEqualTo(1)

            graphLoop.repeatingRequest = request3
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request2)

            assertThat(csp1.events[1].isRepeating).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request3)
        }

    @Test
    fun nullRequestProcessorHaltsProcessing() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            graphLoop.requestProcessor = null
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[1].isClose).isTrue()
        }

    @Test
    fun nullRepeatingRequestInvokesStopRepeating() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            // Set to null and toggle to ensure only one stopRepeating event is issued.
            graphLoop.repeatingRequest = null
            graphLoop.repeatingRequest = request2
            graphLoop.repeatingRequest = null
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[1].isStopRepeating).isTrue()
        }

    @Test
    fun repeatingAfterStopRepeatingDoesNotSkipStopRepeating() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            // Set to null and toggle to ensure only one stopRepeating event is issued.
            graphLoop.repeatingRequest = null
            graphLoop.repeatingRequest = request2
            graphLoop.repeatingRequest = null
            graphLoop.repeatingRequest = request2
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            assertThat(csp1.events[1].isStopRepeating).isTrue()

            assertThat(csp1.events[2].isRepeating).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
        }

    @Test
    fun changingRequestProcessorsReIssuesRepeatingRequest() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            assertThat(csp2.events.size).isEqualTo(1)
            assertThat(csp2.events[0].isRepeating).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
        }

    @Test
    fun changingRequestProcessorsReIssuesCaptureRequests() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            csp1.shutdown() // reject incoming requests
            graphLoop.submit(listOf(request1))
            graphLoop.submit(listOf(request2))
            advanceUntilIdle()

            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            assertThat(csp2.events.size).isEqualTo(2)
            assertThat(csp2.events[0].isCapture).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
            assertThat(csp2.events[1].isCapture).isTrue()
            assertThat(csp2.events[1].requests).containsExactly(request2)
        }

    @Test
    fun capturesThatFailCanBeRetried() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            csp1.shutdown() // reject incoming requests
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            assertThat(csp2.events.size).isEqualTo(1)
            assertThat(csp2.events[0].isRepeating).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
        }

    @Test
    fun closingGraphLoopAbortsPendingRequests() =
        testScope.runTest {
            graphLoop.submit(listOf(request1))
            graphLoop.submit(listOf(request2))
            graphLoop.close()

            // Ensure close does not synchronously cause shutdown to fire.
            verify(mockListener, never()).onAborted(request1)
            verify(mockListener, never()).onAborted(request2)

            advanceUntilIdle()

            // Ensure listeners have been invoked.
            verify(mockListener).onAborted(request1)
            verify(mockListener).onAborted(request2)
        }

    @Test
    fun mixedUpdatesPrioritizeRepeatingRequests() =
        testScope.runTest {
            graphLoop.submit(listOf(request1))
            graphLoop.repeatingRequest = request2
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request2)
            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
        }

    @Test
    fun submitParametersUsesLatestRepeatingRequest() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = request2
            graphLoop.submit(mapOf<Any, Any?>(TEST_KEY to 42))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request2)
            assertThat(csp1.events[1].isCapture).isTrue() // Capture, based on request 2, with keys
            assertThat(csp1.events[1].requests).containsExactly(request2)
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)
        }

    @Test
    fun abortCaptureIsOnlyInvokedOnActiveGraphRequestProcessor() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.submit(listOf(request1))
            graphLoop.abort()
            graphLoop.requestProcessor = grp2 // Change the graphRequestProcessor
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isClose).isTrue()
            assertThat(csp1.events[1].isAbort).isTrue() // Abort is allowed to fire after close.

            assertThat(csp2.events).isEmpty()
        }

    @Test
    fun stopCaptureIsOnlyInvokedOnActiveGraphRequestProcessor() =
        testScope.runTest {
            graphLoop.repeatingRequest = request1
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            graphLoop.repeatingRequest = request2
            graphLoop.repeatingRequest = null
            graphLoop.requestProcessor = grp2 // Change the graphRequestProcessor
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[1].isClose).isTrue()
            assertThat(csp1.events[2].isStopRepeating).isTrue() // StopRepeating is allowed to fire

            assertThat(csp2.events).isEmpty()
        }

    @Test
    fun abortAndStopDoNotPropagateToNewRequestProcessor() =
        testScope.runTest {
            graphLoop.repeatingRequest = request1
            graphLoop.submit(listOf(request2))
            graphLoop.repeatingRequest = null
            graphLoop.abort()
            graphLoop.submit(listOf(request3))
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isCapture).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request3)
        }

    @Test
    fun stopCaptureOnlyRemovesPriorStopCapturesFromSameGraphRequestProcessor() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = null // issue stopCapture #1 with grp1
            graphLoop.requestProcessor = grp2
            graphLoop.repeatingRequest = request2
            graphLoop.repeatingRequest = null // issue stopCapture #2 with grp2 (skip r1, r2)

            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isClose).isTrue()
            assertThat(csp1.events[1].isStopRepeating).isTrue()

            assertThat(csp2.events.size).isEqualTo(1)
            assertThat(csp2.events[0].isStopRepeating).isTrue()
        }

    @Test
    fun submitParametersBeforeRequestProcessorUsesLatestRepeatingRequest() =
        testScope.runTest {
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = request2
            graphLoop.submit(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.repeatingRequest = request3
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request3)
            assertThat(csp1.events[0].requiredParameters).isEmpty()
            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)
        }

    @Test
    fun abortWillSkipSubmitParameters() =
        testScope.runTest {
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = request2
            graphLoop.submit(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.repeatingRequest = request3
            graphLoop.requestProcessor = grp1
            graphLoop.abort()
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isAbort).isTrue()
            assertThat(csp1.events[1].isRepeating).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request3)
            assertThat(csp1.events[1].requiredParameters).isEmpty()
        }

    @Test
    fun requestsCanBeSubmittedWithParameters() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.submit(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.submit(listOf(request2))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].requiredParameters).isEmpty()

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)

            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
            assertThat(csp1.events[2].requiredParameters).isEmpty()
        }

    @Test
    fun defaultParametersAreAppliedToAllRequests() =
        testScope.runTest {
            val gl =
                GraphLoop(
                    cameraGraphId = cameraGraphId,
                    defaultParameters = mapOf<Any, Any?>(TEST_KEY to 10),
                    requiredParameters = requiredParameters,
                    graphListeners = listOf(mockListener),
                    graphState3A = graphState3A,
                    listeners = listOf(listener3A),
                    shutdownScope = shutdownScope,
                    dispatcher = testDispatcher,
                )

            gl.requestProcessor = grp1
            gl.repeatingRequest = request1
            gl.submit(mapOf<Any, Any?>(TEST_KEY to 42))
            gl.submit(listOf(request2))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].defaultParameters).containsEntry(TEST_KEY, 10)
            assertThat(csp1.events[0].requiredParameters).isEmpty()

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].defaultParameters).containsEntry(TEST_KEY, 10)
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)

            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
            assertThat(csp1.events[2].defaultParameters).containsEntry(TEST_KEY, 10)
            assertThat(csp1.events[2].requiredParameters).isEmpty()
        }

    @Test
    fun requiredParametersOverrideSubmittedParameters() =
        testScope.runTest {
            val gl =
                GraphLoop(
                    cameraGraphId = cameraGraphId,
                    defaultParameters = emptyMap<Any, Any?>(),
                    requiredParameters = mapOf<Any, Any?>(TEST_KEY to 10),
                    graphListeners = listOf(mockListener),
                    graphState3A = graphState3A,
                    listeners = listOf(listener3A),
                    shutdownScope = shutdownScope,
                    dispatcher = testDispatcher,
                )

            gl.requestProcessor = grp1
            gl.repeatingRequest = request1
            gl.submit(mapOf<Any, Any?>(TEST_KEY to 42))
            gl.submit(listOf(request2))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].defaultParameters).isEmpty()
            assertThat(csp1.events[0].requiredParameters).containsEntry(TEST_KEY, 10)

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].defaultParameters).isEmpty()
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 10)

            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
            assertThat(csp1.events[2].defaultParameters).isEmpty()
            assertThat(csp1.events[2].requiredParameters).containsEntry(TEST_KEY, 10)
        }

    @Test
    fun requestsSubmittedToClosedRequestProcessorAreEnqueuedToTheNextOne() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            grp1.shutdown()
            graphLoop.repeatingRequest = request1
            graphLoop.submit(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.submit(listOf(request2))
            advanceUntilIdle()

            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            assertThat(csp2.events.size).isEqualTo(3)
            assertThat(csp2.events[0].isRepeating).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
            assertThat(csp2.events[0].requiredParameters).isEmpty()

            assertThat(csp2.events[1].isCapture).isTrue()
            assertThat(csp2.events[1].requests).containsExactly(request1)
            assertThat(csp2.events[1].requiredParameters).containsEntry(TEST_KEY, 42)

            assertThat(csp2.events[2].isCapture).isTrue()
            assertThat(csp2.events[2].requests).containsExactly(request2)
            assertThat(csp2.events[2].requiredParameters).isEmpty()
        }

    @Test
    fun closingGraphLoopClosesRequestProcessor() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.close()
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()
        }

    @Test
    fun swappingRequestProcessorClosesPreviousRequestProcessor() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()

            assertThat(csp2.events).isEmpty()
        }

    @Test
    fun submitParametersUseInitialRequest() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.submit(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.repeatingRequest = request2
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request2)
            assertThat(csp1.events[0].requiredParameters).isEmpty()

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1) // uses original request
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)
        }

    @Test
    fun submitParametersWorksIfRepeatingRequestIsStopped() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.submit(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.repeatingRequest = null
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isStopRepeating).isTrue()

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1) // uses original request
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)
        }

    @Test
    fun exceptionsAreThrown() {
        assertThrows(RuntimeException::class.java) {
                testScope.runTest {
                    graphLoop.requestProcessor = grp1
                    csp1.throwOnBuild = true
                    graphLoop.repeatingRequest = request1

                    advanceUntilIdle()
                }
            }
            .hasMessageThat()
            .contains("Test Exception")
    }

    @Test
    fun stopRepeatingCancelsTriggers() =
        testScope.runTest {
            val listener = Result3AStateListenerImpl({ _ -> true }, 10, 1_000_000_000)
            listener3A.addListener(listener)
            assertThat(listener.result.isCompleted).isFalse()

            graphLoop.repeatingRequest = null

            assertThat(listener.result.isCompleted).isTrue()
            assertThat(listener.result.await().status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
        }

    @Test
    fun clearingRequestProcessorCancelsTriggers() =
        testScope.runTest {
            // Setup the graph loop so that the repeating request and trigger are enqueued before
            // the graphRequestProcessor is configured. Assert that the listener is not invoked
            // until after the requestProcessor is stopped.
            graphLoop.repeatingRequest = request1
            val listener = Result3AStateListenerImpl({ _ -> true }, 10, 1_000_000_000)
            listener3A.addListener(listener)
            graphLoop.submit(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            assertThat(listener.result.isCompleted).isFalse()

            graphLoop.requestProcessor = null
            advanceUntilIdle()

            assertThat(listener.result.isCompleted).isTrue()
            assertThat(listener.result.await().status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
        }

    @Test
    fun shutdownRequestProcessorCancelsTriggers() =
        testScope.runTest {
            // Arrange
            val listener = Result3AStateListenerImpl({ _ -> true }, 10, 1_000_000_000)
            listener3A.addListener(listener)

            // Act
            graphLoop.requestProcessor = null

            // Assert
            assertThat(listener.result.isCompleted).isTrue()
            assertThat(listener.result.await().status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
        }

    @Test
    fun swappingRequestProcessorsDoesNotCancelTriggers() {
        testScope.runTest {
            // Arrange

            // Setup the graph loop so that the repeating request and trigger are enqueued before
            // the graphRequestProcessor is configured. Assert that the listener is not invoked
            // until after the requestProcessor is stopped.
            graphLoop.requestProcessor = grp1
            val listener = Result3AStateListenerImpl({ _ -> true }, 10, 1_000_000_000)
            listener3A.addListener(listener)
            graphLoop.requestProcessor = grp2 // Does not cancel trigger
            advanceUntilIdle()
            assertThat(listener.result.isCompleted).isFalse()

            // Act
            graphLoop.requestProcessor = null // Cancel triggers

            // Assert
            assertThat(listener.result.isCompleted).isTrue()
            assertThat(listener.result.await().status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
        }
    }

    @Test
    fun pausingCaptureProcessingPreventsCaptureRequests() =
        testScope.runTest {
            // Arrange
            graphLoop.requestProcessor = grp1
            graphLoop.captureProcessingEnabled = false // Disable captureProcessing

            // Act
            graphLoop.submit(listOf(request1))
            graphLoop.submit(listOf(request2))
            advanceUntilIdle()

            // Assert: Events are not processed
            assertThat(csp1.events.size).isEqualTo(0)
        }

    @Test
    fun resumingCaptureProcessingResumesCaptureRequests() =
        testScope.runTest {
            // Arrange
            graphLoop.requestProcessor = grp1
            graphLoop.captureProcessingEnabled = false // Disable captureProcessing

            // Act
            graphLoop.submit(listOf(request1))
            graphLoop.submit(listOf(request2))
            advanceUntilIdle()
            graphLoop.captureProcessingEnabled = true // Enable processing
            advanceUntilIdle()

            // Assert: Events are not processed
            assertThat(csp1.events.size).isEqualTo(2)

            assertThat(csp1.events[0].isCapture).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)
        }

    @Test
    fun disablingCaptureProcessingAllowsRepeatingRequests() =
        testScope.runTest {
            // Arrange
            graphLoop.requestProcessor = grp1

            // Act
            graphLoop.captureProcessingEnabled = false // Disable captureProcessing
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            // Assert
            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
        }

    @Test
    fun settingNullForRequestProcessorAfterCloseDoesNotCrash() =
        testScope.runTest {
            // Arrange
            graphLoop.requestProcessor = grp1
            graphLoop.close()

            // Act
            graphLoop.requestProcessor = null
            advanceUntilIdle()

            // Assert: does not crash, and only Close is invoked.
            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()
        }

    @Test
    fun settingRequestProcessorAfterCloseCausesRequestProcessorToBeShutdown() =
        testScope.runTest {
            // Arrange
            graphLoop.close()

            // Act
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            // Assert: Does not crash, and request processor is closed.
            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()
        }

    @Test
    fun settingRequestProcessorAfterShutdownCausesRequestProcessorToBeShutdown() =
        testScope.runTest {
            // Arrange

            graphLoop.requestProcessor = grp1
            graphLoop.close()
            advanceUntilIdle() // Shutdown fully completes

            // Act
            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            // Assert: Does not crash, and request processor is closed.
            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()

            assertThat(csp2.events.size).isEqualTo(1)
            assertThat(csp2.events[0].isClose).isTrue()
        }

    private val SimpleCSP.SimpleCSPEvent.requests: List<Request>
        get() = (this as SimpleCSP.Submit).captureSequence.captureRequestList

    private val SimpleCSP.SimpleCSPEvent.requiredParameters: Map<*, Any?>
        get() = (this as SimpleCSP.Submit).captureSequence.requiredParameters

    private val SimpleCSP.SimpleCSPEvent.defaultParameters: Map<*, Any?>
        get() = (this as SimpleCSP.Submit).captureSequence.defaultParameters

    private val SimpleCSP.SimpleCSPEvent.isRepeating: Boolean
        get() = (this as? SimpleCSP.Submit)?.captureSequence?.repeating ?: false

    private val SimpleCSP.SimpleCSPEvent.isCapture: Boolean
        get() = (this as? SimpleCSP.Submit)?.captureSequence?.repeating == false

    private val SimpleCSP.SimpleCSPEvent.isAbort: Boolean
        get() = this is SimpleCSP.AbortCaptures

    private val SimpleCSP.SimpleCSPEvent.isStopRepeating: Boolean
        get() = this is SimpleCSP.StopRepeating

    private val SimpleCSP.SimpleCSPEvent.isClose: Boolean
        get() = this is SimpleCSP.Close

    internal class SimpleCSP(
        private val cameraId: CameraId,
        private val surfaceMap: Map<StreamId, Surface>
    ) : CaptureSequenceProcessor<Request, FakeCaptureSequence> {
        val events = mutableListOf<SimpleCSPEvent>()
        var throwOnBuild = false
        private var closed = false
        private val sequenceIds = atomic(0)

        override fun build(
            isRepeating: Boolean,
            requests: List<Request>,
            defaultParameters: Map<*, Any?>,
            requiredParameters: Map<*, Any?>,
            listeners: List<Request.Listener>,
            sequenceListener: CaptureSequence.CaptureSequenceListener
        ): FakeCaptureSequence? {
            if (closed) return null
            if (throwOnBuild) throw RuntimeException("Test Exception")
            return FakeCaptureSequence.create(
                cameraId = cameraId,
                repeating = isRepeating,
                requests = requests,
                surfaceMap = surfaceMap,
                defaultParameters = defaultParameters,
                requiredParameters = requiredParameters,
                listeners = listeners,
                sequenceListener = sequenceListener
            )
        }

        override fun abortCaptures() {
            events.add(AbortCaptures)
        }

        override fun stopRepeating() {
            events.add(StopRepeating)
        }

        override suspend fun shutdown() {
            closed = true
            events.add(Close)
        }

        override fun submit(captureSequence: FakeCaptureSequence): Int? {
            if (!closed) {
                events.add(Submit(captureSequence))
                return sequenceIds.incrementAndGet()
            }
            return null
        }

        sealed class SimpleCSPEvent

        object Close : SimpleCSPEvent()

        object StopRepeating : SimpleCSPEvent()

        object AbortCaptures : SimpleCSPEvent()

        data class Submit(val captureSequence: FakeCaptureSequence) : SimpleCSPEvent()
    }
}
