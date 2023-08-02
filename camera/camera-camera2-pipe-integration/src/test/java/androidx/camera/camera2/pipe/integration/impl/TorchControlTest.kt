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
import android.os.Build
import androidx.camera.camera2.pipe.Result3A
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
import androidx.camera.core.TorchState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import com.google.common.util.concurrent.MoreExecutors
import java.util.Objects
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
@OptIn(ExperimentalCoroutinesApi::class)
class TorchControlTest {

    companion object {
        private val executor = MoreExecutors.directExecutor()
        private val fakeUseCaseThreads by lazy {
            val dispatcher = executor.asCoroutineDispatcher()
            val cameraScope = CoroutineScope(Job() + dispatcher)

            UseCaseThreads(
                cameraScope,
                executor,
                dispatcher
            )
        }
    }

    private val metadata = FakeCameraMetadata(
        mapOf(
            CameraCharacteristics.FLASH_INFO_AVAILABLE to true,
        ),
    )

    private val neverCompleteTorchRequestControl = FakeUseCaseCameraRequestControl().apply {
        // Set a CompletableDeferred without set it to completed.
        setTorchResult = CompletableDeferred()
    }
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

    private lateinit var torchControl: TorchControl

    @Before
    fun setUp() {
        val fakeUseCaseCamera = FakeUseCaseCamera()
        val fakeCameraProperties = FakeCameraProperties(metadata)
        torchControl = TorchControl(
            fakeCameraProperties,
            State3AControl(
                fakeCameraProperties,
                NoOpAutoFlashAEModeDisabler,
                aeFpsRange
            ).apply {
                useCaseCamera = fakeUseCaseCamera
            },
            fakeUseCaseThreads,
        )
        torchControl.useCaseCamera = fakeUseCaseCamera
    }

    @Test
    fun enableTorch_whenNoFlashUnit(): Unit = runBlocking {
        assertThrows<IllegalStateException> {
            val fakeUseCaseCamera = FakeUseCaseCamera()
            val fakeCameraProperties = FakeCameraProperties()

            // Without a flash unit, this Job will complete immediately with a IllegalStateException
            TorchControl(
                fakeCameraProperties,
                State3AControl(
                    fakeCameraProperties,
                    NoOpAutoFlashAEModeDisabler,
                    aeFpsRange
                ).apply {
                    useCaseCamera = fakeUseCaseCamera
                },
                fakeUseCaseThreads,
            ).also {
                it.useCaseCamera = fakeUseCaseCamera
            }.setTorchAsync(true).await()
        }
    }

    @Test
    fun getTorchState_whenNoFlashUnit() {
        val fakeUseCaseCamera = FakeUseCaseCamera()
        val fakeCameraProperties = FakeCameraProperties()

        val torchState = TorchControl(
            fakeCameraProperties,
            State3AControl(
                fakeCameraProperties,
                NoOpAutoFlashAEModeDisabler,
                aeFpsRange
            ).apply {

                useCaseCamera = fakeUseCaseCamera
            },
            fakeUseCaseThreads,
        ).also {
            it.useCaseCamera = fakeUseCaseCamera
        }.torchStateLiveData.value

        Truth.assertThat(torchState).isEqualTo(TorchState.OFF)
    }

    @Test
    fun enableTorch_whenInactive(): Unit = runBlocking {
        assertThrows<CameraControl.OperationCanceledException> {
            val fakeUseCaseCamera = FakeUseCaseCamera()
            val fakeCameraProperties = FakeCameraProperties(metadata)

            TorchControl(
                fakeCameraProperties,
                State3AControl(
                    fakeCameraProperties,
                    NoOpAutoFlashAEModeDisabler,
                    aeFpsRange
                ).apply {
                    useCaseCamera = fakeUseCaseCamera
                },
                fakeUseCaseThreads,
            ).setTorchAsync(true).await()
        }
    }

    @Test
    fun getTorchState_whenInactive() {
        torchControl.useCaseCamera = null
        Truth.assertThat(torchControl.torchStateLiveData.value).isEqualTo(TorchState.OFF)
    }

    @Test
    fun enableTorch_torchStateOn(): Unit = runBlocking {
        torchControl.setTorchAsync(true)
        // LiveData is updated synchronously. Don't need to wait for the result of the setTorchAsync
        Truth.assertThat(torchControl.torchStateLiveData.value).isEqualTo(TorchState.ON)
    }

    @Test
    fun disableTorch_TorchStateOff() {
        torchControl.setTorchAsync(true)
        // LiveData is updated synchronously. Don't need to wait for the result of the setTorchAsync
        val firstTorchState = Objects.requireNonNull<Int>(torchControl.torchStateLiveData.value)
        torchControl.setTorchAsync(false)
        // LiveData is updated synchronously. Don't need to wait for the result of the setTorchAsync
        val secondTorchState = torchControl.torchStateLiveData.value
        Truth.assertThat(firstTorchState).isEqualTo(TorchState.ON)
        Truth.assertThat(secondTorchState).isEqualTo(TorchState.OFF)
    }

    @Test
    fun enableDisableTorch_futureWillCompleteSuccessfully(): Unit = runBlocking {
        // Job should be completed without exception
        torchControl.setTorchAsync(true).await()

        // Job should be completed without exception
        torchControl.setTorchAsync(false).await()
    }

    @Test
    fun enableTorchTwice_cancelPreviousFuture(): Unit = runBlocking {
        val deferred = torchControl.also {
            it.useCaseCamera = FakeUseCaseCamera(requestControl = neverCompleteTorchRequestControl)
        }.setTorchAsync(true)

        torchControl.setTorchAsync(true)

        assertThrows<CameraControl.OperationCanceledException> {
            deferred.await()
        }
    }

    @Test
    fun setInActive_cancelPreviousFuture(): Unit = runBlocking {
        val deferred = torchControl.also {
            it.useCaseCamera = FakeUseCaseCamera(requestControl = neverCompleteTorchRequestControl)
        }.setTorchAsync(true)

        // reset() will be called after all the UseCases are detached.
        torchControl.reset()

        assertThrows<CameraControl.OperationCanceledException> {
            deferred.await()
        }
    }

    @Test
    fun setInActiveWhenTorchOn_changeToTorchOff() {
        torchControl.setTorchAsync(true)
        val initialTorchState = torchControl.torchStateLiveData.value

        // reset() will be called after all the UseCases are detached.
        torchControl.reset()

        val torchStateAfterInactive = torchControl.torchStateLiveData.value
        Truth.assertThat(initialTorchState).isEqualTo(TorchState.ON)
        Truth.assertThat(torchStateAfterInactive).isEqualTo(TorchState.OFF)
    }

    @Test
    fun enableDisableTorch_observeTorchStateLiveData() {
        val receivedTorchState = mutableListOf<Int?>()
        // The observer should be notified of initial state
        torchControl.torchStateLiveData.observe(
            TestLifecycleOwner(
                Lifecycle.State.STARTED,
                UnconfinedTestDispatcher()
            ), object : Observer<Int?> {
                private var mValue: Int? = null
                override fun onChanged(value: Int?) {
                    if (mValue != value) {
                        mValue = value
                        receivedTorchState.add(value)
                    }
                }
            })
        torchControl.setTorchAsync(true)
        torchControl.setTorchAsync(false)
        Truth.assertThat(receivedTorchState[0]).isEqualTo(TorchState.OFF) // initial state
        Truth.assertThat(receivedTorchState[1]).isEqualTo(TorchState.ON) // by setTorchAsync(true)
        Truth.assertThat(receivedTorchState[2]).isEqualTo(TorchState.OFF) // by setTorchAsync(false)
    }

    @Test
    fun useCaseCameraUpdated_setTorchResultShouldPropagate(): Unit = runBlocking {
        // Arrange.
        torchControl.useCaseCamera =
            FakeUseCaseCamera(requestControl = neverCompleteTorchRequestControl)

        val deferred = torchControl.setTorchAsync(true)
        val fakeRequestControl = FakeUseCaseCameraRequestControl().apply {
            setTorchResult = CompletableDeferred<Result3A>()
        }
        val fakeUseCaseCamera = FakeUseCaseCamera(requestControl = fakeRequestControl)

        // Act. Simulate the UseCaseCamera is recreated.
        torchControl.useCaseCamera = fakeUseCaseCamera

        // Simulate setTorch is completed in the recreated UseCaseCamera
        fakeRequestControl.setTorchResult.complete(Result3A(status = Result3A.Status.OK))

        // Assert. The setTorch task should be completed.
        Truth.assertThat(deferred.awaitWithTimeout()).isNotNull()
    }

    @Test
    fun useCaseCameraUpdated_onlyCompleteLatestRequest(): Unit = runBlocking {
        // Arrange.
        torchControl.useCaseCamera =
            FakeUseCaseCamera(requestControl = neverCompleteTorchRequestControl)

        val deferred = torchControl.setTorchAsync(true)
        val fakeRequestControl = FakeUseCaseCameraRequestControl().apply {
            setTorchResult = CompletableDeferred()
        }
        val fakeUseCaseCamera = FakeUseCaseCamera(requestControl = fakeRequestControl)

        // Act. Simulate the UseCaseCamera is recreated.
        torchControl.useCaseCamera = fakeUseCaseCamera
        // Act. Set Torch mode again.
        val deferred2 = torchControl.setTorchAsync(false)
        // Simulate setTorch is completed in the recreated UseCaseCamera
        fakeRequestControl.setTorchResult.complete(Result3A(status = Result3A.Status.OK))

        // Assert. The previous setTorch task should be cancelled
        assertThrows<CameraControl.OperationCanceledException> {
            deferred.awaitWithTimeout()
        }
        // Assert. The latest setTorch task should be completed.
        Truth.assertThat(deferred2.awaitWithTimeout()).isNotNull()
    }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ) = withTimeout(timeMillis) {
        await()
    }
}
