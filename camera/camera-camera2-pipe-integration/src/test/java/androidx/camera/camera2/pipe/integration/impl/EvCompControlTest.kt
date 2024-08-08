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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Range
import android.util.Rational
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.EvCompImpl
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.core.CameraControl
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class EvCompControlTest {
    private val fakeUseCaseThreads by lazy {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val cameraScope = CoroutineScope(Job() + dispatcher)

        UseCaseThreads(
            cameraScope,
            executor,
            dispatcher,
        )
    }
    private val metadata =
        FakeCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE to Range.create(-4, 4),
                CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP to Rational.parseRational("1/2"),
            ),
        )
    private val comboRequestListener = ComboRequestListener()
    private lateinit var exposureControl: EvCompControl

    @Before
    fun setUp() {
        exposureControl =
            EvCompControl(
                EvCompImpl(FakeCameraProperties(metadata), fakeUseCaseThreads, comboRequestListener)
            )
        exposureControl.requestControl = FakeUseCaseCameraRequestControl()
    }

    @Test
    fun setExposureTwice_theFirstCallShouldBeCancelled(): Unit = runBlocking {
        val deferred: Deferred<Int> = exposureControl.updateAsync(1)
        val deferred1: Deferred<Int> = exposureControl.updateAsync(2)

        // Assert. The second call should keep working.
        assertThat(deferred1.isCompleted).isFalse()
        // Assert. The first call should be cancelled with a
        // CameraControl.OperationCanceledException.
        assertThrows<CameraControl.OperationCanceledException> { deferred.awaitWithTimeout() }
    }

    @Test
    fun setExposureCancelled_theCompensationValueShouldKeepInControl() = runBlocking {
        // Arrange.
        val deferred = exposureControl.updateAsync(1)

        // Act.
        deferred.cancel()

        // Assert. The new value should be set to the exposure control even when the task fails.
        assertThat(exposureControl.exposureState.exposureCompensationIndex).isEqualTo(1)
    }

    @Test
    fun exposureControlInactive_setExposureTaskShouldCancel(): Unit = runBlocking {
        val deferred = exposureControl.updateAsync(1)

        // Act. Simulate control inactive by set useCaseCamera to null & call reset().
        exposureControl.requestControl = null
        exposureControl.reset()

        // Assert. The exposure control has been set to inactive. It should throw an exception.
        assertThrows<CameraControl.OperationCanceledException> { deferred.awaitWithTimeout() }
    }

    @Test
    fun setExposureNotInRange_shouldCompleteTheTaskWithException(): Unit = runBlocking {
        // Assert. The Exposure index 5 is not in the valid range. It should throw an exception.
        assertThrows<IllegalArgumentException> { exposureControl.updateAsync(5).awaitWithTimeout() }
    }

    @Test
    fun setExposureOnNotSupportedCamera_shouldCompleteTheTaskWithException(): Unit = runBlocking {
        // Arrange.
        val evCompCompat =
            EvCompImpl(
                // Fake CameraProperties without CONTROL_AE_COMPENSATION related properties.
                FakeCameraProperties(),
                fakeUseCaseThreads,
                comboRequestListener
            )
        exposureControl = EvCompControl(evCompCompat)
        exposureControl.requestControl = FakeUseCaseCameraRequestControl()

        // Act.
        val deferred = exposureControl.updateAsync(1)

        // Assert. This camera does not support the exposure compensation, the task should fail.
        assertThrows<IllegalArgumentException> { deferred.awaitWithTimeout() }
    }

    @Test
    fun useCaseCameraUpdated_setExposureResultShouldPropagate(): Unit = runBlocking {
        val targetEv = 1
        val deferred = exposureControl.updateAsync(targetEv)

        // Act. Simulate the UseCaseCamera is recreated.
        exposureControl.requestControl = FakeUseCaseCameraRequestControl()
        comboRequestListener.simulateAeConverge(exposureValue = targetEv)

        // Assert. The setEV task should be completed.
        assertThat(deferred.awaitWithTimeout()).isEqualTo(targetEv)
    }

    @Test
    fun useCaseCameraUpdated_onlyCompleteLatestRequest(): Unit = runBlocking {
        val targetEv = 2
        val deferred = exposureControl.updateAsync(1)

        // Act. Simulate the UseCaseCamera is recreated,
        exposureControl.requestControl = FakeUseCaseCameraRequestControl()
        // Act. Submits a new EV value.
        val deferred2 = exposureControl.updateAsync(targetEv)
        comboRequestListener.simulateAeConverge(exposureValue = targetEv)

        // Assert. The previous setEV task should be cancelled
        assertThrows<CameraControl.OperationCanceledException> { deferred.awaitWithTimeout() }
        // Assert. The latest setEV task should be completed.
        assertThat(deferred2.awaitWithTimeout()).isEqualTo(targetEv)
    }

    private suspend fun Deferred<Int>.awaitWithTimeout(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ) = withTimeout(timeMillis) { await() }

    private fun ComboRequestListener.simulateAeConverge(
        exposureValue: Int,
        frameNumber: FrameNumber = FrameNumber(101L),
    ) {
        val requestMetadata =
            FakeRequestMetadata(
                requestParameters =
                    mapOf(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION to exposureValue),
                requestNumber = RequestNumber(1)
            )
        val resultMetaData =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION to exposureValue,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_CONVERGED,
                    ),
                frameNumber = frameNumber,
            )
        fakeUseCaseThreads.sequentialExecutor.execute {
            onComplete(
                requestMetadata,
                frameNumber,
                FakeFrameInfo(
                    metadata = resultMetaData,
                    requestMetadata = requestMetadata,
                )
            )
        }
    }
}
