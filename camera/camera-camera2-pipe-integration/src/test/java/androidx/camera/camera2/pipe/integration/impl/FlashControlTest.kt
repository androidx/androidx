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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Looper
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.pipe.integration.compat.workaround.NotUseFlashModeTorchFor3aUpdate
import androidx.camera.camera2.pipe.integration.compat.workaround.UseFlashModeTorchFor3aUpdateImpl
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.testing.impl.mocks.MockScreenFlash
import androidx.testutils.MainDispatcherRule
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class FlashControlTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fakeUseCaseThreads by lazy {
        val executor = MoreExecutors.directExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val cameraScope = CoroutineScope(Job() + dispatcher)

        UseCaseThreads(
            cameraScope,
            executor,
            dispatcher,
        )
    }
    private val fakeRequestControl = FakeUseCaseCameraRequestControl()
    private lateinit var state3AControl: State3AControl
    private lateinit var torchControl: TorchControl
    private lateinit var flashControl: FlashControl

    private val screenFlash = MockScreenFlash()

    @Before
    fun setUp() {
        createFlashControl()
    }

    private fun createFlashControl(
        addExternalFlashAeMode: Boolean = false,
        useFlashModeTorch: Boolean = false,
    ) {
        val aeAvailableModes =
            mutableListOf(
                    CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                    CaptureRequest.CONTROL_AE_MODE_ON,
                    CaptureRequest.CONTROL_AE_MODE_OFF,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH,
                )
                .apply {
                    if (addExternalFlashAeMode) {
                        add(CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH)
                    }
                }

        val metadata =
            FakeCameraMetadata(
                mapOf(
                    CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                        aeAvailableModes.toIntArray()
                )
            )
        val cameraProperties = FakeCameraProperties(metadata)

        state3AControl =
            State3AControl(
                    cameraProperties,
                    NoOpAutoFlashAEModeDisabler,
                )
                .apply { requestControl = fakeRequestControl }

        torchControl =
            TorchControl(cameraProperties, state3AControl, fakeUseCaseThreads).apply {
                requestControl = fakeRequestControl
            }

        flashControl =
            FlashControl(
                cameraProperties = cameraProperties,
                state3AControl = state3AControl,
                threads = fakeUseCaseThreads,
                torchControl = torchControl,
                useFlashModeTorchFor3aUpdate =
                    if (useFlashModeTorch) {
                        UseFlashModeTorchFor3aUpdateImpl
                    } else {
                        NotUseFlashModeTorchFor3aUpdate
                    },
            )
        flashControl.requestControl = fakeRequestControl
        flashControl.setScreenFlash(screenFlash)
    }

    @Test
    fun setFlash_whenInactive(): Unit = runBlocking {
        val fakeCameraProperties = FakeCameraProperties()

        val flashControl =
            FlashControl(
                fakeCameraProperties,
                State3AControl(
                        fakeCameraProperties,
                        NoOpAutoFlashAEModeDisabler,
                    )
                    .apply { requestControl = fakeRequestControl },
                fakeUseCaseThreads,
                TorchControl(fakeCameraProperties, state3AControl, fakeUseCaseThreads),
                NotUseFlashModeTorchFor3aUpdate
            )

        assertThrows<CameraControl.OperationCanceledException> {
            flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON).awaitWithTimeout()
        }
    }

    @Test
    fun setFlash_flashModeOn(): Unit = runBlocking {
        // Arrange, clear data of the initial invocations.
        fakeRequestControl.addParameterCalls.clear()

        // Act.
        flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON).awaitWithTimeout()

        // Assert. AE mode should change accordingly.
        assertThat(fakeRequestControl.addParameterCalls).hasSize(1)
        assertThat(fakeRequestControl.addParameterCalls[0])
            .containsAtLeastEntriesIn(
                mapOf(
                    CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                )
            )
    }

    @Test
    fun setFlash_flashModeAuto(): Unit = runBlocking {
        // Arrange, clear data of the initial invocations.
        fakeRequestControl.addParameterCalls.clear()

        // Act.
        flashControl.setFlashAsync(ImageCapture.FLASH_MODE_AUTO).awaitWithTimeout()

        // Assert. AE mode should change accordingly.
        assertThat(fakeRequestControl.addParameterCalls).hasSize(1)
        assertThat(fakeRequestControl.addParameterCalls[0])
            .containsAtLeastEntriesIn(
                mapOf(
                    CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                )
            )
    }

    @Test
    fun setFlash_flashModeOnThenOff(): Unit = runBlocking {
        // Arrange, clear data of the initial invocations.
        fakeRequestControl.addParameterCalls.clear()

        // Act.
        flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON).awaitWithTimeout()
        flashControl.setFlashAsync(ImageCapture.FLASH_MODE_OFF).awaitWithTimeout()

        // Assert. AE mode should change accordingly.
        assertThat(fakeRequestControl.addParameterCalls).hasSize(2)
        assertThat(fakeRequestControl.addParameterCalls[0])
            .containsAtLeastEntriesIn(
                mapOf(
                    CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                )
            )
        assertThat(fakeRequestControl.addParameterCalls[1])
            .containsAtLeastEntriesIn(
                mapOf(CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON)
            )
    }

    @Test
    fun setFlashTwice_cancelPreviousFuture(): Unit = runBlocking {
        // Arrange. Set a never complete deferred.
        fakeRequestControl.addParameterResult = CompletableDeferred()

        // Act. call setFlashAsync twice.
        val deferred = flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON)
        flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON)

        assertThrows<CameraControl.OperationCanceledException> { deferred.awaitWithTimeout() }
    }

    @Test
    fun setInActive_cancelPreviousFuture(): Unit = runBlocking {
        // Arrange. Set a never complete deferred.
        fakeRequestControl.addParameterResult = CompletableDeferred()
        val deferred = flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON)

        // Act. call reset & clear the UseCaseCamera.
        flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON)
        flashControl.reset()
        flashControl.requestControl = null

        assertThrows<CameraControl.OperationCanceledException> { deferred.awaitWithTimeout() }
    }

    @Test
    fun useCaseCameraUpdated_setFlashResultShouldPropagate(): Unit = runBlocking {
        // Arrange.
        fakeRequestControl.addParameterResult = CompletableDeferred()

        val deferred = flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON)
        val fakeRequestControl =
            FakeUseCaseCameraRequestControl().apply { addParameterResult = CompletableDeferred() }

        // Act. Simulate the UseCaseCamera is recreated.
        flashControl.requestControl = fakeRequestControl
        state3AControl.requestControl = fakeRequestControl

        // Simulate setFlash is completed on the recreated UseCaseCamera
        fakeRequestControl.addParameterResult.complete(Unit)

        // Assert. The setFlash task should be completed.
        assertThat(deferred.awaitWithTimeout()).isNotNull()
    }

    @Test
    fun useCaseCameraUpdated_onlyCompleteLatestRequest(): Unit = runBlocking {
        // Arrange.
        fakeRequestControl.addParameterResult = CompletableDeferred()

        val deferred = flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON)
        val fakeRequestControl =
            FakeUseCaseCameraRequestControl().apply { addParameterResult = CompletableDeferred() }

        // Act. Simulate the UseCaseCamera is recreated.
        flashControl.requestControl = fakeRequestControl
        state3AControl.requestControl = fakeRequestControl
        // Act. Submits a new Flash mode.
        val deferred2 = flashControl.setFlashAsync(ImageCapture.FLASH_MODE_AUTO)
        // Simulate setFlash is completed on the recreated UseCaseCamera
        fakeRequestControl.addParameterResult.complete(Unit)

        // Assert. The previous set Flash mode task should be cancelled
        assertThrows<CameraControl.OperationCanceledException> { deferred.awaitWithTimeout() }
        // Assert. The latest set Flash mode task should be completed.
        assertThat(deferred2.awaitWithTimeout()).isNotNull()
    }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ) = withTimeout(timeMillis) { await() }

    @Test
    fun canSetScreenFlash() {
        val newScreenFlash = MockScreenFlash()
        flashControl.setScreenFlash(newScreenFlash)
        assertThat(flashControl.screenFlash).isEqualTo(newScreenFlash)
    }

    // 3s timeout is hardcoded in ImageCapture.ScreenFlash.apply documentation
    @Test
    fun screenFlashApplyInvokedWithAtLeast3sTimeout_whenStarted() = runTest {
        val initialTime = System.currentTimeMillis()

        flashControl.startScreenFlashCaptureTasks()

        assertThat(screenFlash.lastApplyExpirationTimeMillis)
            .isAtLeast(initialTime + TimeUnit.SECONDS.toMillis(3))
    }

    @Test
    fun screenFlashApplyInvokedWithLessThan4sTimeout_whenStarted() = runTest {
        val initialTime = System.currentTimeMillis()

        flashControl.startScreenFlashCaptureTasks()

        assertThat(screenFlash.lastApplyExpirationTimeMillis)
            .isLessThan(initialTime + TimeUnit.SECONDS.toMillis(4))
    }

    @Test
    fun screenFlashApplyInvokedInMainThread_whenStarted() = runTest {
        withContext(Dispatchers.IO) { // ensures initial call is not from main thread
            flashControl.startScreenFlashCaptureTasks()
        }

        assertThat(screenFlash.lastApplyThreadLooper).isEqualTo(Looper.getMainLooper())
    }

    @Test
    fun externalFlashAeModeNotAttemptedAtScreenFlashCapture_whenNotSupported() = runTest {
        createFlashControl(addExternalFlashAeMode = false)
        flashControl.startScreenFlashCaptureTasks()

        assertThat(state3AControl.tryExternalFlashAeMode).isFalse()
    }

    @Config(minSdk = 28)
    @Test
    fun externalFlashAeModeAttemptedAtScreenFlashCapture_whenSupported() = runTest {
        createFlashControl(addExternalFlashAeMode = true)
        flashControl.startScreenFlashCaptureTasks()

        assertThat(state3AControl.tryExternalFlashAeMode).isTrue()
    }

    @Config(minSdk = 28)
    @Test
    fun externalFlashAeModeAttempted_whenScreenFlashCaptureApplyNotCompleted() = runTest {
        createFlashControl(addExternalFlashAeMode = true)
        screenFlash.setApplyCompletedInstantly(false)

        flashControl.startScreenFlashCaptureTasks()

        assertThat(state3AControl.tryExternalFlashAeMode).isTrue()
    }

    @Test
    fun externalFlashAeModeDisabled_whenScreenFlashCaptureStopped() = runTest {
        createFlashControl(addExternalFlashAeMode = true)
        flashControl.startScreenFlashCaptureTasks()

        flashControl.stopScreenFlashCaptureTasks()

        assertThat(state3AControl.tryExternalFlashAeMode).isFalse()
    }

    @Test
    fun torchNotEnabledAtScreenFlashCapture_whenNotRequired() = runTest {
        createFlashControl(addExternalFlashAeMode = false, useFlashModeTorch = false)

        flashControl.startScreenFlashCaptureTasks()

        assertThat(torchControl.torchStateLiveData.value).isEqualTo(TorchState.OFF)
    }

    @Test
    fun torchEnabledAtScreenFlashCapture_whenRequired() = runTest {
        createFlashControl(addExternalFlashAeMode = false, useFlashModeTorch = true)

        flashControl.startScreenFlashCaptureTasks()

        assertThat(torchControl.torchStateLiveData.value).isEqualTo(TorchState.ON)
    }

    @Test
    fun torchEnabled_whenScreenFlashCaptureApplyNotCompleted() = runTest {
        createFlashControl(addExternalFlashAeMode = false, useFlashModeTorch = true)
        screenFlash.setApplyCompletedInstantly(false)

        flashControl.startScreenFlashCaptureTasks()

        assertThat(torchControl.torchStateLiveData.value).isEqualTo(TorchState.ON)
    }

    @Test
    fun torchDisabledAtScreenFlashCaptureStop_whenRequired() = runTest {
        createFlashControl(addExternalFlashAeMode = false, useFlashModeTorch = true)
        flashControl.startScreenFlashCaptureTasks()

        flashControl.stopScreenFlashCaptureTasks()

        assertThat(torchControl.torchStateLiveData.value).isEqualTo(TorchState.OFF)
    }

    @Test
    fun screenFlashClearInvokedInMainThread_whenStopped() = runTest {
        withContext(Dispatchers.IO) { // ensures initial call is not from main thread
            flashControl.stopScreenFlashCaptureTasks()
        }

        assertThat(screenFlash.lastClearThreadLooper).isEqualTo(Looper.getMainLooper())
    }
}
