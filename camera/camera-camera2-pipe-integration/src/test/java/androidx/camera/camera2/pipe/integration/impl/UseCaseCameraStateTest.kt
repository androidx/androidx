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

package androidx.camera.camera2.pipe.integration.impl

import android.os.Build
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.adapter.asListenableFuture
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession.RequestStatus.ABORTED
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession.RequestStatus.FAILED
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession.RequestStatus.TOTAL_CAPTURE_DONE
import androidx.camera.camera2.pipe.integration.testing.FakeSurface
import androidx.camera.core.impl.DeferrableSurface
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
@OptIn(ExperimentalCoroutinesApi::class)
class UseCaseCameraStateTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val surface = FakeSurface()
    private val surfaceToStreamMap: Map<DeferrableSurface, StreamId> = mapOf(surface to StreamId(0))
    private val useCaseThreads by lazy {
        UseCaseThreads(
            testScope,
            testDispatcher.asExecutor(),
            testDispatcher
        )
    }

    private val fakeCameraGraphSession = FakeCameraGraphSession()
    private val fakeCameraGraph = FakeCameraGraph(fakeCameraGraphSession)
    private val fakeUseCaseGraphConfig = UseCaseGraphConfig(
        graph = fakeCameraGraph,
        surfaceToStreamMap = surfaceToStreamMap,
        cameraStateAdapter = CameraStateAdapter(),
    )

    private val useCaseCameraState = UseCaseCameraState(
        useCaseGraphConfig = fakeUseCaseGraphConfig,
        threads = useCaseThreads,
    )

    @Before
    fun setUp() {
        fakeCameraGraphSession.startRepeatingSignal = CompletableDeferred() // not complete yet
    }

    @After
    fun tearDown() {
        surface.close()
    }

    @Test
    fun updateAsyncCompletes_whenStopRepeating(): Unit = runBlocking {
        // stopRepeating is called when there is no stream after updateAsync call
        val result = useCaseCameraState.updateAsync(
            streams = emptySet()
        ).asListenableFuture()

        assertFutureCompletes(result)
    }

    @Test
    fun updateAsyncCompletes_whenStartRepeating(): Unit = runBlocking {
        // startRepeating is called when there is at least one stream after updateAsync call
        val result = useCaseCameraState.updateAsync(
            streams = setOf(StreamId(0))
        ).asListenableFuture()

        // simulate startRepeating request being completed in camera
        fakeCameraGraphSession.startRepeatingSignal.complete(TOTAL_CAPTURE_DONE)

        assertFutureCompletes(result)
    }

    @Test
    fun updateAsyncFails_whenStartRepeatingRequestFails(): Unit = runBlocking {
        // startRepeating is called when there is at least one stream after updateAsync call
        val result = useCaseCameraState.updateAsync(
            streams = setOf(StreamId(0))
        ).asListenableFuture()

        // simulate startRepeating request failing in camera framework level
        fakeCameraGraphSession.startRepeatingSignal.complete(FAILED)

        assertFutureFails(result)
    }

    @Test
    fun updateAsyncIncomplete_whenStartRepeatingRequestIsAborted(): Unit = runTest {
        // startRepeating is called when there is at least one stream after updateAsync call
        val result = useCaseCameraState.updateAsync(
            streams = setOf(StreamId(0))
        ).asListenableFuture()

        // simulate startRepeating request being aborted by camera framework level
        fakeCameraGraphSession.startRepeatingSignal.complete(ABORTED)

        advanceUntilIdle()
        assertFutureStillWaiting(result)
    }

    @Test
    fun updateAsyncIncomplete_whenNewRequestSubmitted(): Unit = runTest {
        // startRepeating is called when there is at least one stream after updateAsync call
        val result = useCaseCameraState.updateAsync(
            streams = setOf(StreamId(0))
        ).asListenableFuture()

        // simulate startRepeating request being aborted by camera framework level
        fakeCameraGraphSession.startRepeatingSignal.complete(ABORTED)
        advanceUntilIdle()

        // simulate startRepeating being called again
        fakeCameraGraphSession.startRepeatingSignal = CompletableDeferred() // reset
        useCaseCameraState.updateAsync(
            streams = setOf(StreamId(0))
        )

        advanceUntilIdle()
        assertFutureStillWaiting(result)
    }

    @Test
    fun previousUpdateAsyncCompletes_whenNewStartRepeatingRequestCompletesAfterAbort(): Unit =
        runTest {
            // startRepeating is called when there is at least one stream after updateAsync call
            val result = useCaseCameraState.updateAsync(
                streams = setOf(StreamId(0))
            ).asListenableFuture()

            // simulate startRepeating request being aborted by camera framework level
            fakeCameraGraphSession.startRepeatingSignal.complete(ABORTED)
            advanceUntilIdle()

            // simulate startRepeating being called again
            fakeCameraGraphSession.startRepeatingSignal = CompletableDeferred() // reset
            useCaseCameraState.updateAsync(
                streams = setOf(StreamId(0))
            )
            fakeCameraGraphSession.startRepeatingSignal.complete(TOTAL_CAPTURE_DONE) // completed

            assertFutureCompletes(result)
        }

    @Test
    fun previousUpdateAsyncCompletes_whenInvokedTwice(): Unit = runBlocking {
        // startRepeating is called when there is at least one stream after updateAsync call
        val result = useCaseCameraState.updateAsync(
            streams = setOf(StreamId(0))
        ).asListenableFuture()

        useCaseCameraState.updateAsync(
            streams = setOf(StreamId(1))
        ).asListenableFuture()

        // simulate startRepeating request being completed in camera
        fakeCameraGraphSession.startRepeatingSignal.complete(TOTAL_CAPTURE_DONE)

        assertFutureCompletes(result)
    }

    private fun <T> assertFutureCompletes(future: ListenableFuture<T>) {
        future[3, TimeUnit.SECONDS]
    }

    private fun <T> assertFutureFails(future: ListenableFuture<T>) {
        assertThrows(ExecutionException::class.java) {
            future[3, TimeUnit.SECONDS]
        }
    }

    private fun <T> assertFutureStillWaiting(future: ListenableFuture<T>) {
        assertWithMessage("Future already completed instead of waiting")
            .that(future.isDone).isFalse()
    }
}
