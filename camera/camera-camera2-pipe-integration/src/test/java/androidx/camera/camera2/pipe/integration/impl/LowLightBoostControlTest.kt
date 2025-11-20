/*
 * Copyright 2025 The Android Open Source Project
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
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
import android.hardware.camera2.CameraMetadata.CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE
import android.hardware.camera2.CaptureResult.CONTROL_LOW_LIGHT_BOOST_STATE
import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeState3AControlCreator
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.core.CameraControl
import androidx.camera.core.LowLightBoostState.ACTIVE
import androidx.camera.core.LowLightBoostState.INACTIVE
import androidx.camera.core.LowLightBoostState.OFF
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

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalCoroutinesApi::class)
class LowLightBoostControlTest {

    companion object {
        private const val DEFAULT_CAMERA_ID = "0"
        private val executor = MoreExecutors.directExecutor()
        private val fakeUseCaseThreads by lazy {
            val dispatcher = executor.asCoroutineDispatcher()
            val cameraScope = CoroutineScope(Job() + dispatcher)

            UseCaseThreads(cameraScope, executor, dispatcher)
        }
    }

    private val metadata =
        FakeCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                    intArrayOf(CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY)
            )
        )

    private val fakeCameraProperties =
        CameraPipeCameraProperties(CameraConfig(CameraId(DEFAULT_CAMERA_ID)), metadata)

    private val fakeUseCaseCameraRequestControl = FakeUseCaseCameraRequestControl()

    private val addParameterResultDeferred: CompletableDeferred<Unit> = CompletableDeferred()

    private val neverCompleteLowLightBoostRequestControl =
        FakeUseCaseCameraRequestControl().apply {
            // Set a CompletableDeferred without set it to completed.
            addParameterResult = addParameterResultDeferred
        }

    private val comboRequestListener = ComboRequestListener()

    private lateinit var lowLightBoostControl: LowLightBoostControl

    @Before
    fun setUp() {
        lowLightBoostControl =
            LowLightBoostControl(
                fakeCameraProperties.metadata,
                State3AControl(fakeCameraProperties, NoOpAutoFlashAEModeDisabler).apply {
                    requestControl = fakeUseCaseCameraRequestControl
                },
                fakeUseCaseThreads,
                comboRequestListener,
            )
    }

    @Test
    fun enableLowLightBoost_whenIsNotSupported(): Unit = runBlocking {
        assertThrows<IllegalStateException> {
            // Creates camera properties without low-light boost support
            val fakeCameraProperties = FakeCameraProperties()

            LowLightBoostControl(
                    fakeCameraProperties.metadata,
                    State3AControl(fakeCameraProperties, NoOpAutoFlashAEModeDisabler).apply {
                        requestControl = fakeUseCaseCameraRequestControl
                    },
                    fakeUseCaseThreads,
                    comboRequestListener,
                )
                .also { it.requestControl = fakeUseCaseCameraRequestControl }
                .setLowLightBoostAsync(true)
                .await()
        }
    }

    @Test
    fun getLowLightBoostState_whenIsNotSupported() {
        // Creates camera properties without low-light boost support
        val fakeCameraProperties = FakeCameraProperties()

        val lowLightBoostState =
            LowLightBoostControl(
                    fakeCameraProperties.metadata,
                    State3AControl(fakeCameraProperties, NoOpAutoFlashAEModeDisabler).apply {
                        requestControl = fakeUseCaseCameraRequestControl
                    },
                    fakeUseCaseThreads,
                    comboRequestListener,
                )
                .also { it.requestControl = fakeUseCaseCameraRequestControl }
                .lowLightBoostStateLiveData
                .value

        Truth.assertThat(lowLightBoostState).isEqualTo(OFF)
    }

    @Test
    fun enableLowLightBoost_whenInactive(): Unit = runBlocking {
        assertThrows<CameraControl.OperationCanceledException> {
            lowLightBoostControl.setLowLightBoostAsync(true).await()
        }
    }

    @Test
    fun getLowLightBoostControlState_whenInactive() {
        Truth.assertThat(lowLightBoostControl.lowLightBoostStateLiveData.value).isEqualTo(OFF)
    }

    @Test
    fun enableLowLightBoost_lowLightBoostStateInactive(): Unit = runBlocking {
        activateLowLightBoost()
        lowLightBoostControl.setLowLightBoostAsync(true)
        Truth.assertThat(lowLightBoostControl.lowLightBoostStateLiveData.value).isEqualTo(INACTIVE)
    }

    @Test
    fun disableLowLightBoost_lowLightBoostControlStateOff() {
        activateLowLightBoost()
        lowLightBoostControl.setLowLightBoostAsync(true)
        val firstLowLightBoostState =
            Objects.requireNonNull<Int>(lowLightBoostControl.lowLightBoostStateLiveData.value)
        lowLightBoostControl.setLowLightBoostAsync(false)
        val secondLowLightBoostState = lowLightBoostControl.lowLightBoostStateLiveData.value
        Truth.assertThat(firstLowLightBoostState).isEqualTo(INACTIVE)
        Truth.assertThat(secondLowLightBoostState).isEqualTo(OFF)
    }

    @Test
    fun enableDisableLowLightBoost_futureWillCompleteSuccessfully(): Unit = runBlocking {
        activateLowLightBoost()

        val deferred1 = lowLightBoostControl.setLowLightBoostAsync(true)
        // Job should be completed without exception
        deferred1.awaitWithTimeout()

        val deferred2 = lowLightBoostControl.setLowLightBoostAsync(false)
        // Job should be completed without exception
        deferred2.awaitWithTimeout()
    }

    private fun simulateLowLightBoostStateUpdate(state: Int? = null) =
        comboRequestListener.onTotalCaptureResult(
            FakeRequestMetadata(),
            FrameNumber(0L),
            FakeFrameInfo(
                FakeFrameMetadata(
                    state?.let { mapOf(CONTROL_LOW_LIGHT_BOOST_STATE to state) } ?: emptyMap()
                )
            ),
        )

    @Test
    fun enableLowLightBoostTwice_cancelPreviousFuture(): Unit = runBlocking {
        recreateNeverCompleteLowLightBoostControl()
        activateLowLightBoost(neverCompleteLowLightBoostRequestControl)

        val deferred = lowLightBoostControl.setLowLightBoostAsync(true)

        lowLightBoostControl.setLowLightBoostAsync(true)

        assertThrows<CameraControl.OperationCanceledException> { deferred.await() }
    }

    @Test
    fun invokeReset_cancelPreviousFuture(): Unit = runBlocking {
        recreateNeverCompleteLowLightBoostControl()
        activateLowLightBoost(neverCompleteLowLightBoostRequestControl)

        val deferred = lowLightBoostControl.setLowLightBoostAsync(true)

        // reset() will be called after all the UseCases are detached.
        lowLightBoostControl.reset()

        assertThrows<CameraControl.OperationCanceledException> { deferred.await() }
    }

    @Test
    fun invokeResetWhenLowLightBoostOn_changeToLowLightBoostOff() {
        activateLowLightBoost()
        lowLightBoostControl.setLowLightBoostAsync(true)
        simulateLowLightBoostStateUpdate(CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE)
        val initialLowLightBoostState = lowLightBoostControl.lowLightBoostStateLiveData.value

        // reset() will be called after all the UseCases are detached.
        lowLightBoostControl.reset()

        val lowLightBoostStateAfterReset = lowLightBoostControl.lowLightBoostStateLiveData.value
        Truth.assertThat(initialLowLightBoostState).isEqualTo(ACTIVE)
        Truth.assertThat(lowLightBoostStateAfterReset).isEqualTo(OFF)
    }

    @Test
    fun setInActiveWhenLowLightBoostOn_changeToLowLightBoostInactive() {
        activateLowLightBoost()
        lowLightBoostControl.setLowLightBoostAsync(true)
        // Makes the state become ACTIVE
        simulateLowLightBoostStateUpdate(CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE)
        val initialLowLightBoostState = lowLightBoostControl.lowLightBoostStateLiveData.value

        // requestControl will be cleared when the control becomes inactive
        lowLightBoostControl.requestControl = null

        // Verifies that the state becomes INACTIVE after making the control inactive
        val lowLightBoostStateAfterInactive = lowLightBoostControl.lowLightBoostStateLiveData.value
        Truth.assertThat(initialLowLightBoostState).isEqualTo(ACTIVE)
        Truth.assertThat(lowLightBoostStateAfterInactive).isEqualTo(INACTIVE)
    }

    @Test
    fun enableDisableLowLightBoost_observeLowLightBoostStateLiveData() {
        activateLowLightBoost()
        val receivedStates = mutableListOf<Int?>()
        // The observer should be notified of initial state
        lowLightBoostControl.lowLightBoostStateLiveData.observe(
            TestLifecycleOwner(Lifecycle.State.STARTED, UnconfinedTestDispatcher()),
            object : Observer<Int?> {
                private var mValue: Int? = null

                override fun onChanged(value: Int?) {
                    if (mValue != value) {
                        mValue = value
                        receivedStates.add(value)
                    }
                }
            },
        )
        lowLightBoostControl.setLowLightBoostAsync(true)
        lowLightBoostControl.setLowLightBoostAsync(false)
        Truth.assertThat(receivedStates[0]).isEqualTo(OFF) // initial state
        Truth.assertThat(receivedStates[1]).isEqualTo(INACTIVE) // by setLowLightBoostAsync(true)
        Truth.assertThat(receivedStates[2]).isEqualTo(OFF) // by setLowLightBoostAsync(false)
    }

    @Test
    fun useCaseCameraUpdated_setLowLightBoostResultShouldPropagate(): Unit = runBlocking {
        recreateNeverCompleteLowLightBoostControl()
        activateLowLightBoost(neverCompleteLowLightBoostRequestControl)

        val deferred = lowLightBoostControl.setLowLightBoostAsync(true)
        // Act. Simulate the UseCaseCamera is recreated.
        lowLightBoostControl.requestControl = neverCompleteLowLightBoostRequestControl
        // Completes the Deferred
        addParameterResultDeferred.complete(Unit)

        // Assert. The setLowLightBoostAsync task should be completed.
        Truth.assertThat(deferred.awaitWithTimeout()).isNotNull()
    }

    @Test
    fun useCaseCameraUpdated_onlyCompleteLatestRequest(): Unit = runBlocking {
        recreateNeverCompleteLowLightBoostControl()
        activateLowLightBoost(neverCompleteLowLightBoostRequestControl)

        val deferred = lowLightBoostControl.setLowLightBoostAsync(true)

        // Act. Simulate the UseCaseCamera is recreated.
        lowLightBoostControl.requestControl = neverCompleteLowLightBoostRequestControl
        // Act. Set LowLightBoost mode again.
        val deferred2 = lowLightBoostControl.setLowLightBoostAsync(false)
        // Completes the Deferred
        addParameterResultDeferred.complete(Unit)

        // Assert. The previous setLowLightBoostAsync task should be cancelled
        assertThrows<CameraControl.OperationCanceledException> { deferred.awaitWithTimeout() }
        // Assert. The latest setLowLightBoostAsync task should be completed.
        Truth.assertThat(deferred2.awaitWithTimeout()).isNotNull()
    }

    @Test
    fun enableLowLightBoost_whenDisabledByUseCaseSessionConfig(): Unit = runBlocking {
        activateLowLightBoost()
        val deferred =
            lowLightBoostControl
                .also { it.setLowLightBoostDisabledByUseCaseSessionConfig(true) }
                .setLowLightBoostAsync(true)

        assertThrows<IllegalStateException> { deferred.await() }
    }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ) = withTimeout(timeMillis) { await() }

    private fun activateLowLightBoost(
        requestControl: UseCaseCameraRequestControl = fakeUseCaseCameraRequestControl
    ) {
        lowLightBoostControl.requestControl = requestControl
    }

    private fun recreateNeverCompleteLowLightBoostControl() {
        lowLightBoostControl =
            LowLightBoostControl(
                fakeCameraProperties.metadata,
                FakeState3AControlCreator.createState3AControl(
                    fakeCameraProperties,
                    neverCompleteLowLightBoostRequestControl,
                ),
                fakeUseCaseThreads,
                comboRequestListener,
            )
    }
}
