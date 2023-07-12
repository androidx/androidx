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
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.AeFpsRange
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCamera
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class FlashControlTest {
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
    private val metadata = FakeCameraMetadata(
        mapOf(
            CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to intArrayOf(
                CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                CaptureRequest.CONTROL_AE_MODE_ON,
                CaptureRequest.CONTROL_AE_MODE_OFF,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            ),
        ),
    )
    private val fakeRequestControl = FakeUseCaseCameraRequestControl()
    private val fakeUseCaseCamera = FakeUseCaseCamera(requestControl = fakeRequestControl)
    private val aeFpsRange = AeFpsRange(
        CameraQuirks(
            FakeCameraMetadata(),
            StreamConfigurationMapCompat(
                StreamConfigurationMapBuilder.newBuilder().build(),
                OutputSizesCorrector(
                    FakeCameraMetadata(),
                    StreamConfigurationMapBuilder.newBuilder().build()
                )
            )
        )
    )
    private val state3AControl =
        State3AControl(
            FakeCameraProperties(metadata),
            NoOpAutoFlashAEModeDisabler,
            aeFpsRange
            ).apply {
            useCaseCamera = fakeUseCaseCamera
        }
    private lateinit var flashControl: FlashControl

    @Before
    fun setUp() {
        flashControl = FlashControl(
            state3AControl = state3AControl,
            threads = fakeUseCaseThreads,
        )
        flashControl.useCaseCamera = fakeUseCaseCamera
    }

    @Test
    fun setFlash_whenInactive(): Unit = runBlocking {
        val fakeUseCaseCamera = FakeUseCaseCamera()
        val fakeCameraProperties = FakeCameraProperties()

        val flashControl = FlashControl(
            State3AControl(
                fakeCameraProperties,
                NoOpAutoFlashAEModeDisabler,
                aeFpsRange
            ).apply {
                useCaseCamera = fakeUseCaseCamera
            },
            fakeUseCaseThreads,
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
        assertThat(fakeRequestControl.addParameterCalls[0]).containsAtLeastEntriesIn(
            mapOf(CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
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
        assertThat(fakeRequestControl.addParameterCalls[0]).containsAtLeastEntriesIn(
            mapOf(CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
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
        assertThat(fakeRequestControl.addParameterCalls[0]).containsAtLeastEntriesIn(
            mapOf(CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
        )
        assertThat(fakeRequestControl.addParameterCalls[1]).containsAtLeastEntriesIn(
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

        assertThrows<CameraControl.OperationCanceledException> {
            deferred.awaitWithTimeout()
        }
    }

    @Test
    fun setInActive_cancelPreviousFuture(): Unit = runBlocking {
        // Arrange. Set a never complete deferred.
        fakeRequestControl.addParameterResult = CompletableDeferred()
        val deferred = flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON)

        // Act. call reset & clear the UseCaseCamera.
        flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON)
        flashControl.reset()
        flashControl.useCaseCamera = null

        assertThrows<CameraControl.OperationCanceledException> {
            deferred.awaitWithTimeout()
        }
    }

    @Test
    fun useCaseCameraUpdated_setFlashResultShouldPropagate(): Unit = runBlocking {
        // Arrange.
        fakeRequestControl.addParameterResult = CompletableDeferred()

        val deferred = flashControl.setFlashAsync(ImageCapture.FLASH_MODE_ON)
        val fakeRequestControl = FakeUseCaseCameraRequestControl().apply {
            addParameterResult = CompletableDeferred()
        }
        val fakeUseCaseCamera = FakeUseCaseCamera(requestControl = fakeRequestControl)

        // Act. Simulate the UseCaseCamera is recreated.
        flashControl.useCaseCamera = fakeUseCaseCamera
        state3AControl.useCaseCamera = fakeUseCaseCamera

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
        val fakeRequestControl = FakeUseCaseCameraRequestControl().apply {
            addParameterResult = CompletableDeferred()
        }
        val fakeUseCaseCamera = FakeUseCaseCamera(requestControl = fakeRequestControl)

        // Act. Simulate the UseCaseCamera is recreated.
        flashControl.useCaseCamera = fakeUseCaseCamera
        state3AControl.useCaseCamera = fakeUseCaseCamera
        // Act. Submits a new Flash mode.
        val deferred2 = flashControl.setFlashAsync(ImageCapture.FLASH_MODE_AUTO)
        // Simulate setFlash is completed on the recreated UseCaseCamera
        fakeRequestControl.addParameterResult.complete(Unit)

        // Assert. The previous set Flash mode task should be cancelled
        assertThrows<CameraControl.OperationCanceledException> {
            deferred.awaitWithTimeout()
        }
        // Assert. The latest set Flash mode task should be completed.
        assertThat(deferred2.awaitWithTimeout()).isNotNull()
    }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ) = withTimeout(timeMillis) {
        await()
    }
}
