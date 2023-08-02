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
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCamera
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.CameraControl
import androidx.camera.core.TorchState
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.SessionConfig
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.AfterClass
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import java.util.Objects
import java.util.concurrent.Executors

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCoroutinesApi::class)
class TorchControlTest {

    companion object {
        private val executor = Executors.newSingleThreadExecutor()
        private val fakeUseCaseThreads by lazy {
            val dispatcher = executor.asCoroutineDispatcher()
            val cameraScope = CoroutineScope(Job() + dispatcher)

            UseCaseThreads(
                cameraScope,
                executor,
                dispatcher
            )
        }

        @JvmStatic
        @AfterClass
        fun close() {
            executor.shutdown()
        }
    }

    private val metadata = FakeCameraMetadata(
        mapOf(
            CameraCharacteristics.FLASH_INFO_AVAILABLE to true,
        ),
    )

    private val neverCompleteTorchRequestControl = object : UseCaseCameraRequestControl {
        override fun addParametersAsync(
            type: UseCaseCameraRequestControl.Type,
            values: Map<CaptureRequest.Key<*>, Any>,
            optionPriority: androidx.camera.core.impl.Config.OptionPriority,
            tags: Map<String, Any>,
            streams: Set<StreamId>?,
            template: RequestTemplate?,
            listeners: Set<Request.Listener>
        ): Deferred<Unit> {
            return CompletableDeferred(Unit)
        }

        override fun setConfigAsync(
            type: UseCaseCameraRequestControl.Type,
            config: androidx.camera.core.impl.Config?,
            tags: Map<String, Any>,
            streams: Set<StreamId>?,
            template: RequestTemplate?,
            listeners: Set<Request.Listener>
        ): Deferred<Unit> {
            return CompletableDeferred(Unit)
        }

        override fun setSessionConfigAsync(sessionConfig: SessionConfig): Deferred<Unit> {
            return CompletableDeferred(Unit)
        }

        override suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A> {
            // Return a CompletableDeferred without set it to completed.
            return CompletableDeferred()
        }

        override suspend fun startFocusAndMeteringAsync(
            aeRegions: List<MeteringRectangle>,
            afRegions: List<MeteringRectangle>,
            awbRegions: List<MeteringRectangle>
        ): Deferred<Result3A> {
            return CompletableDeferred(Result3A(status = Result3A.Status.OK))
        }

        override suspend fun issueSingleCaptureAsync(
            captureSequence: List<CaptureConfig>,
            captureMode: Int,
            flashType: Int,
            flashMode: Int,
        ): List<Deferred<Void?>> {
            return listOf(CompletableDeferred(null))
        }
    }

    private lateinit var torchControl: TorchControl

    @Before
    fun setUp() {
        torchControl = TorchControl(
            FakeCameraProperties(metadata),
            fakeUseCaseThreads,
        )
        torchControl.useCaseCamera = FakeUseCaseCamera()
    }

    @Test
    fun enableTorch_whenNoFlashUnit(): Unit = runBlocking {
        assertThrows<IllegalStateException> {
            // Without a flash unit, this Job will complete immediately with a IllegalStateException
            TorchControl(
                FakeCameraProperties(),
                fakeUseCaseThreads,
            ).also {
                it.useCaseCamera = FakeUseCaseCamera()
            }.setTorchAsync(true).await()
        }
    }

    @Test
    fun getTorchState_whenNoFlashUnit() {
        val torchState = TorchControl(
            FakeCameraProperties(),
            fakeUseCaseThreads,
        ).also {
            it.useCaseCamera = FakeUseCaseCamera()
        }.torchStateLiveData.value

        Truth.assertThat(torchState).isEqualTo(TorchState.OFF)
    }

    @Test
    fun enableTorch_whenInactive(): Unit = runBlocking {
        assertThrows<CameraControl.OperationCanceledException> {
            TorchControl(
                FakeCameraProperties(metadata),
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
}