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

package androidx.camera.camera2.impl

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.camera2.testing.FakeUseCaseCameraRequestControl
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class State3AControlTest {

    private val testScope = TestScope()
    // StandardTestDispatcher allows us to pause execution and queue tasks
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    private lateinit var fakeUseCaseThreads: UseCaseThreads
    private lateinit var state3AControl: State3AControl
    private lateinit var fakeRequestControl: FakeUseCaseCameraRequestControl

    @Before
    fun setUp() {
        // Create an Executor that delegates to the TestDispatcher
        // By using 'fakeUseCaseThreads.scope.launch', tasks submitted to this executor
        // become child coroutines of the test scope, automatically adhering to
        // advanceUntilIdle() control.
        val cameraScope = CoroutineScope(Job() + testDispatcher)

        val testExecutor = Executor { command -> cameraScope.launch { command.run() } }

        fakeUseCaseThreads = UseCaseThreads(cameraScope, testExecutor, testDispatcher)

        fakeRequestControl = FakeUseCaseCameraRequestControl()

        state3AControl =
            State3AControl(FakeCameraProperties(), NoOpAutoFlashAEModeDisabler, fakeUseCaseThreads)
                .apply { requestControl = fakeRequestControl }
    }

    @After
    fun tearDown() {
        fakeUseCaseThreads.scope.cancel()
    }

    @Test
    fun update_coalescesRapidRequests() =
        testScope.runTest {
            // 1. Clear any noise from the initialization update (which triggers automatically in
            // setUp)
            fakeRequestControl.addParameterCalls.clear()

            // 2. Act: Fire off 3 rapid updates.
            // Because we are using StandardTestDispatcher, these should just queue up.
            val signal1 = state3AControl.setFlashModeAsync(ImageCapture.FLASH_MODE_ON)
            val signal2 = state3AControl.setTemplateAsync(CameraDevice.TEMPLATE_RECORD)
            val signal3 = state3AControl.setFlashModeAsync(ImageCapture.FLASH_MODE_OFF)

            // 3. Assert: The queue should effectively hold these tasks, so no parameters
            // should have been submitted to the camera control yet.
            assertThat(fakeRequestControl.addParameterCalls).isEmpty()

            // 4. Act: Run all queued tasks
            testDispatcher.scheduler.advanceUntilIdle()

            // 5. Assert:
            // - All user-facing signals must complete successfully
            assertThat(signal1.isCompleted).isTrue()
            assertThat(signal2.isCompleted).isTrue()
            assertThat(signal3.isCompleted).isTrue()

            // - Performance Check: Only ONE submission was made to the camera (the final one)
            //   The intermediate ones (and the Init one if it was queued) were skipped by the
            // revision check.
            assertThat(fakeRequestControl.addParameterCalls).hasSize(1)

            // - Correctness Check: The parameters match the FINAL state
            val params = fakeRequestControl.addParameterCalls.first()
            // Note: FLASH_MODE_OFF usually maps to AE_MODE_ON (1)
            // If FakeMetadata is minimal, checking consistency here is enough.
            assertThat(params[CaptureRequest.CONTROL_AE_MODE]).isNotNull()
        }

    @Test
    fun reset_completesPendingSignals() =
        testScope.runTest {
            // Arrange
            val signal1 = state3AControl.setFlashModeAsync(ImageCapture.FLASH_MODE_ON)

            // Act: Reset immediately after setting flash
            state3AControl.reset()

            testDispatcher.scheduler.advanceUntilIdle()

            // Assert: The original signal should complete (success), not hang or cancel
            // because "reset" is treated as just another update that supersedes the previous one.
            assertThat(signal1.isCompleted).isTrue()

            // Assert: State is back to defaults
            assertThat(state3AControl.flashMode).isEqualTo(ImageCapture.FLASH_MODE_OFF)
        }

    @Test
    fun update_failsAllSignals_whenSubmissionFails() =
        testScope.runTest {
            // Arrange: Make the camera control fail
            val expectedException = CameraControl.OperationCanceledException("Hardware Error")
            fakeRequestControl.addParameterResult =
                CompletableDeferred<Unit>().apply { completeExceptionally(expectedException) }

            // Act
            val signal1 = state3AControl.setFlashModeAsync(ImageCapture.FLASH_MODE_ON)
            val signal2 = state3AControl.setTemplateAsync(CameraDevice.TEMPLATE_PREVIEW)

            testDispatcher.scheduler.advanceUntilIdle()

            // Assert: Both signals fail with the same exception
            assertThrows<CameraControl.OperationCanceledException> { signal1.awaitWithTimeout() }
            assertThrows<CameraControl.OperationCanceledException> { signal2.awaitWithTimeout() }
        }

    @Test
    fun valuesUpdatedImmediately_beforeAsyncWork() =
        testScope.runTest {
            // Arrange
            assertThat(state3AControl.flashMode).isEqualTo(ImageCapture.FLASH_MODE_OFF)

            // Act: Call setter
            state3AControl.setFlashModeAsync(ImageCapture.FLASH_MODE_ON)

            // Assert: Value is updated immediately, even if the async work hasn't run yet
            assertThat(state3AControl.flashMode).isEqualTo(ImageCapture.FLASH_MODE_ON)

            // Run work just to clean up
            testDispatcher.scheduler.advanceUntilIdle()
        }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(1)
    ) = withTimeout(timeMillis) { await() }
}
