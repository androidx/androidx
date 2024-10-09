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

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.media.ImageReader
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraBackendFactory
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.internal.CameraBackendsImpl
import androidx.camera.camera2.pipe.internal.CameraGraphParametersImpl
import androidx.camera.camera2.pipe.internal.FrameCaptureQueue
import androidx.camera.camera2.pipe.internal.FrameDistributor
import androidx.camera.camera2.pipe.internal.GraphLifecycleManager
import androidx.camera.camera2.pipe.internal.ImageSourceMap
import androidx.camera.camera2.pipe.media.ImageReaderImageSources
import androidx.camera.camera2.pipe.testing.CameraControllerSimulator
import androidx.camera.camera2.pipe.testing.FakeAudioRestrictionController
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraGraphImplTest {
    private val testScope = TestScope()

    private val context = ApplicationProvider.getApplicationContext() as Context
    private val metadata =
        FakeCameraMetadata(
            mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL),
        )
    private val fakeGraphProcessor = FakeGraphProcessor()
    private val imageReader1 = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 4)
    private val imageReader2 = ImageReader.newInstance(1920, 1080, ImageFormat.YUV_420_888, 4)
    private val fakeSurfaceListener: CameraSurfaceManager.SurfaceListener = mock()
    private val cameraSurfaceManager = CameraSurfaceManager()

    private val stream1Config =
        CameraStream.Config.create(Size(1280, 720), StreamFormat.YUV_420_888)
    private val stream2Config =
        CameraStream.Config.create(Size(1920, 1080), StreamFormat.YUV_420_888)

    private val graphId = CameraGraphId.nextId()
    private val graphConfig =
        CameraGraph.Config(
            camera = metadata.camera,
            streams = listOf(stream1Config, stream2Config),
        )
    private val threads = FakeThreads.fromTestScope(testScope)
    private val backend = FakeCameraBackend(fakeCameras = mapOf(metadata.camera to metadata))
    private val backends =
        CameraBackendsImpl(
            defaultBackendId = backend.id,
            cameraBackends = mapOf(backend.id to CameraBackendFactory { backend }),
            context,
            threads
        )
    private val cameraContext = CameraBackendsImpl.CameraBackendContext(context, threads, backends)
    private val graphLifecycleManager = GraphLifecycleManager(threads)
    private val imageSources = ImageReaderImageSources(threads)
    private val frameCaptureQueue = FrameCaptureQueue()
    private val cameraController =
        CameraControllerSimulator(cameraContext, graphId, graphConfig, fakeGraphProcessor)
    private val cameraControllerProvider: () -> CameraControllerSimulator = { cameraController }
    private val streamGraph = StreamGraphImpl(metadata, graphConfig, cameraControllerProvider)
    private val imageSourceMap = ImageSourceMap(graphConfig, streamGraph, imageSources)
    private val frameDistributor =
        FrameDistributor(imageSourceMap.imageSources, frameCaptureQueue) {}
    private val cameraGraphFlags = CameraGraph.Flags()
    private val surfaceGraph =
        SurfaceGraph(
            streamGraph,
            cameraController,
            cameraSurfaceManager,
            emptyMap(),
            cameraGraphFlags,
        )
    private val audioRestriction = FakeAudioRestrictionController()
    private val cameraGraph =
        CameraGraphImpl(
            graphConfig,
            metadata,
            graphLifecycleManager,
            fakeGraphProcessor,
            fakeGraphProcessor,
            streamGraph,
            surfaceGraph,
            backend,
            cameraController,
            GraphState3A(),
            Listener3A(),
            frameDistributor,
            frameCaptureQueue,
            audioRestriction,
            graphId,
            CameraGraphParametersImpl()
        )
    private val stream1: CameraStream =
        checkNotNull(cameraGraph.streams[stream1Config]) {
            "Failed to find stream for $stream1Config!"
        }

    private val stream2 =
        checkNotNull(cameraGraph.streams[stream2Config]) {
            "Failed to find stream for $stream2Config!"
        }

    init {
        cameraSurfaceManager.addListener(fakeSurfaceListener)
    }

    @Before
    fun setUp() {
        cameraController.streamGraph = streamGraph
    }

    @Test fun createCameraGraphImpl() = testScope.runTest { assertThat(cameraGraph).isNotNull() }

    @Test
    fun testAcquireSession() =
        testScope.runTest {
            val session = cameraGraph.acquireSession()
            assertThat(session).isNotNull()
        }

    @Test
    fun testAcquireSessionOrNull() =
        testScope.runTest {
            val session = cameraGraph.acquireSessionOrNull()
            assertThat(session).isNotNull()
        }

    @Test
    fun testAcquireSessionOrNullAfterAcquireSession() =
        testScope.runTest {
            val session = cameraGraph.acquireSession()
            assertThat(session).isNotNull()

            // Since a session is already active, an attempt to acquire another session will fail.
            val session1 = cameraGraph.acquireSessionOrNull()
            assertThat(session1).isNull()

            // Closing an active session should allow a new session instance to be created.
            session.close()

            val session2 = cameraGraph.acquireSessionOrNull()
            assertThat(session2).isNotNull()
        }

    @Test
    fun sessionSubmitsRequestsToGraphProcessor() =
        testScope.runTest {
            val session = checkNotNull(cameraGraph.acquireSessionOrNull())
            val request = Request(listOf())
            session.submit(request)
            advanceUntilIdle()

            assertThat(fakeGraphProcessor.requestQueue).contains(listOf(request))
        }

    @Test
    fun sessionSetsRepeatingRequestOnGraphProcessor() =
        testScope.runTest {
            val session = checkNotNull(cameraGraph.acquireSessionOrNull())
            val request = Request(listOf())
            session.startRepeating(request)
            advanceUntilIdle()

            assertThat(fakeGraphProcessor.repeatingRequest).isSameInstanceAs(request)
        }

    @Test
    fun sessionAbortsRequestOnGraphProcessor() =
        testScope.runTest {
            val session = checkNotNull(cameraGraph.acquireSessionOrNull())
            val request = Request(listOf())
            session.submit(request)
            session.abort()
            advanceUntilIdle()

            assertThat(fakeGraphProcessor.requestQueue).isEmpty()
        }

    @Test
    fun closingSessionDoesNotCloseGraphProcessor() =
        testScope.runTest {
            val session = cameraGraph.acquireSessionOrNull()
            checkNotNull(session).close()
            advanceUntilIdle()

            assertThat(fakeGraphProcessor.closed).isFalse()
        }

    @Test
    fun closingCameraGraphClosesGraphProcessor() =
        testScope.runTest {
            cameraGraph.close()
            assertThat(fakeGraphProcessor.closed).isTrue()
        }

    @Test
    fun stoppingCameraGraphStopsGraphProcessor() =
        testScope.runTest {
            assertThat(cameraController.started).isFalse()
            assertThat(fakeGraphProcessor.closed).isFalse()
            cameraGraph.start()
            assertThat(cameraController.started).isTrue()
            cameraGraph.stop()
            assertThat(cameraController.started).isFalse()
            assertThat(fakeGraphProcessor.closed).isFalse()
            cameraGraph.start()
            assertThat(cameraController.started).isTrue()
            cameraGraph.close()
            assertThat(cameraController.started).isFalse()
            assertThat(fakeGraphProcessor.closed).isTrue()
        }

    @Test
    fun closingCameraGraphClosesAssociatedSurfaces() =
        testScope.runTest {
            cameraGraph.setSurface(stream1.id, imageReader1.surface)
            cameraGraph.setSurface(stream2.id, imageReader2.surface)
            cameraGraph.close()

            verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(imageReader1.surface))
            verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(imageReader2.surface))
            verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(imageReader1.surface))
            verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(imageReader1.surface))
        }

    @Test
    fun useSessionInOperatesInOrder() =
        testScope.runTest {
            val events = mutableListOf<Int>()
            val job1 =
                cameraGraph.useSessionIn(testScope) {
                    yield()
                    events += 2
                }
            val job2 =
                cameraGraph.useSessionIn(testScope) {
                    delay(100)
                    events += 3
                }
            val job3 =
                cameraGraph.useSessionIn(testScope) {
                    yield()
                    events += 4
                }

            events += 1
            job1.join()
            job2.join()
            job3.join()

            assertThat(events).containsExactly(1, 2, 3, 4).inOrder()
        }

    @Test
    fun useSessionWithEarlyCloseAllowsInterleavedExecution() =
        testScope.runTest {
            val events = mutableListOf<Int>()
            val job1 =
                cameraGraph.useSessionIn(testScope) { session ->
                    yield()
                    events += 2
                    session.close()
                    delay(1000)
                    events += 5
                }
            val job2 =
                cameraGraph.useSessionIn(testScope) {
                    delay(100)
                    events += 3
                }
            val job3 =
                cameraGraph.useSessionIn(testScope) {
                    yield()
                    events += 4
                }

            events += 1
            job1.join()
            job2.join()
            job3.join()

            assertThat(events).containsExactly(1, 2, 3, 4, 5).inOrder()
        }

    @Test
    fun useSessionInWithRunBlockingDoesNotStall() = runBlocking {
        val deferred = cameraGraph.useSessionIn(this) { delay(1) }
        deferred.await() // Make sure this does not block.
    }

    @Test
    fun coroutineScope_isCanceledWithException() =
        testScope.runTest {
            val scope = CoroutineScope(Job())

            val deferred = scope.async { throw RuntimeException() }
            deferred.join()

            // Ensure the deferred is completed with an exception, and that the scope is NOT active.
            assertThat(deferred.isCompleted).isTrue()
            assertThat(deferred.getCompletionExceptionOrNull())
                .isInstanceOf(RuntimeException::class.java)
            assertThrows<RuntimeException> { deferred.await() }
            assertThat(scope.isActive).isFalse()
        }

    @Test
    fun coroutineSupervisorScope_isNotCanceledWithException() =
        testScope.runTest {
            val scope = CoroutineScope(SupervisorJob())

            val deferred = scope.async { throw RuntimeException() }
            deferred.join()

            // Ensure the deferred is completed with an exception, and that the scope remains
            // active.
            assertThat(deferred.isCompleted).isTrue()
            assertThat(deferred.getCompletionExceptionOrNull())
                .isInstanceOf(RuntimeException::class.java)
            assertThrows<RuntimeException> { deferred.await() }
            assertThat(scope.isActive).isTrue()
        }

    @Test
    fun useSessionIn_scopeIsCanceledWithException() =
        testScope.runTest {
            val scope = CoroutineScope(Job())

            val deferred = cameraGraph.useSessionIn(scope) { throw RuntimeException() }
            deferred.join()

            assertThat(deferred.isCompleted).isTrue()
            assertThat(deferred.getCompletionExceptionOrNull())
                .isInstanceOf(RuntimeException::class.java)
            assertThrows<RuntimeException> { deferred.await() }
            assertThat(scope.isActive).isFalse() // Regular scopes are canceled
        }

    @Test
    fun useSessionIn_supervisorScopeIsNotCanceledWithException() =
        testScope.runTest {
            val scope = CoroutineScope(SupervisorJob())
            val deferred = cameraGraph.useSessionIn(scope) { throw RuntimeException() }
            deferred.join()

            assertThat(deferred.isCompleted).isTrue()
            assertThat(deferred.getCompletionExceptionOrNull())
                .isInstanceOf(RuntimeException::class.java)
            assertThrows<RuntimeException> { deferred.await() }
            assertThat(scope.isActive).isTrue() // Supervisor scopes are not canceled
        }

    @Test
    fun coroutineSupervisorTestScope_isNotCanceledWithException() =
        testScope.runTest {
            // This illustrates the correct way to create a scope that uses the testScope
            // dispatcher, does delay skipping, but also does not fail the test if an exception
            // occurs when doing scope.async. This is useful if, for example, in a real environment
            // scope represents a supervisor job that will not crash if a coroutine fails and if
            // some other system is handling the result of the deferred.
            val scope = CoroutineScope(testScope.coroutineContext + Job())

            val deferred =
                scope.async {
                    delay(100000) // Delay skipping
                    throw RuntimeException()
                }
            deferred.join()

            assertThat(deferred.isCompleted).isTrue()
            assertThat(deferred.getCompletionExceptionOrNull())
                .isInstanceOf(RuntimeException::class.java)
            assertThrows<RuntimeException> { deferred.await() }
            assertThat(scope.isActive).isFalse()
            assertThat(testScope.isActive).isTrue()
        }

    @Test
    fun useSessionIn_withSupervisorTestScopeDoesNotCancelTestScope() =
        testScope.runTest {
            // Create a scope that uses the testScope dispatcher and delaySkipping, but does not
            // fail
            // the test if an exception occurs in useSessionIn.
            val scope = CoroutineScope(testScope.coroutineContext + SupervisorJob())

            // If you pass in a testScope to useSessionIn, any exception will cause the test to
            // fail. If, instead, you want to test that the deferred handles the exception, you must
            // pass in an independent CoroutineScope.
            val deferred = cameraGraph.useSessionIn(scope) { throw RuntimeException() }
            deferred.join()

            assertThat(deferred.isCompleted).isTrue()
            assertThat(deferred.getCompletionExceptionOrNull())
                .isInstanceOf(RuntimeException::class.java)
            assertThat(scope.isActive).isTrue() // Supervisor scopes are not canceled
            assertThat(testScope.isActive).isTrue()
        }

    @Test
    fun useSessionIn_withCancellationDoesNotFailTest() =
        testScope.runTest {
            val deferred =
                cameraGraph.useSessionIn(testScope) {
                    throw CancellationException() // Throwing cancellation does not cause the test
                    // to fail.
                }
            deferred.join()

            assertThat(deferred.isActive).isFalse()
            assertThat(deferred.isCompleted).isTrue()
            assertThat(deferred.isCancelled).isTrue()
            assertThat(deferred.getCompletionExceptionOrNull())
                .isInstanceOf(CancellationException::class.java)
            assertThat(testScope.isActive).isTrue()
        }

    @Test
    fun useSession_throwsExceptions() =
        testScope.runTest {
            assertThrows<RuntimeException> { cameraGraph.useSession { throw RuntimeException() } }
        }

    @Test
    fun testGetOutputLatency() =
        testScope.runTest {
            assertThat(cameraController.getOutputLatency(null)).isNull()
            cameraController.simulateOutputLatency()
            assertThat(cameraController.getOutputLatency(null)?.estimatedLatencyNs)
                .isEqualTo(cameraController.outputLatencySet?.estimatedLatencyNs)
        }
}
