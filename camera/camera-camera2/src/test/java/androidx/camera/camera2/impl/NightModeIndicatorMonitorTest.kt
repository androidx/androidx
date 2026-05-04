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

import android.hardware.camera2.CaptureResult.EXTENSION_NIGHT_MODE_INDICATOR
import android.hardware.camera2.CaptureResult.EXTENSION_NIGHT_MODE_INDICATOR_ON
import android.os.Looper.getMainLooper
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.adapter.awaitUntil
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.camera2.testing.FakeUseCaseCameraRequestControl
import androidx.camera.core.NightModeIndicator
import androidx.lifecycle.Observer
import com.google.common.truth.Truth
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = 36)
@OptIn(ExperimentalCoroutinesApi::class)
class NightModeIndicatorMonitorTest {

    companion object {
        private const val DEFAULT_CAMERA_ID = "0"
        private val executor = MoreExecutors.directExecutor()
        private val fakeUseCaseThreads by lazy {
            val dispatcher = executor.asCoroutineDispatcher()
            val cameraScope = CoroutineScope(Job() + dispatcher)

            UseCaseThreads(cameraScope, executor, dispatcher)
        }
    }

    private val metadata = FakeCameraMetadata(resultKeys = setOf(EXTENSION_NIGHT_MODE_INDICATOR))

    private val fakeCameraProperties =
        CameraPipeCameraProperties(CameraConfig(CameraId(DEFAULT_CAMERA_ID)), metadata)

    private val comboRequestListener = ComboRequestListener()

    private lateinit var nightModeIndicatorMonitor: NightModeIndicatorMonitor

    @Before
    fun setUp() {
        nightModeIndicatorMonitor =
            NightModeIndicatorMonitor(fakeCameraProperties.metadata, comboRequestListener)
    }

    @Test
    fun getNightModeIndicator_whenIsNotSupported() {
        // Creates camera properties without night mode indicator support
        val fakeCameraProperties = FakeCameraProperties()

        val indicatorState =
            NightModeIndicatorMonitor(fakeCameraProperties.metadata, comboRequestListener)
                .nightModeIndicatorLiveData
                .value

        Truth.assertThat(indicatorState).isEqualTo(NightModeIndicator.UNKNOWN)
    }

    @Test
    fun getNightModeIndicator_initialState() {
        Truth.assertThat(nightModeIndicatorMonitor.nightModeIndicatorLiveData.value)
            .isEqualTo(NightModeIndicator.UNKNOWN)
    }

    @Test
    fun simulateIndicatorUpdate_receivesValue(): Unit = runBlocking {
        nightModeIndicatorMonitor.requestControl = FakeUseCaseCameraRequestControl()
        simulateAndMonitorIndicator(EXTENSION_NIGHT_MODE_INDICATOR_ON)
    }

    @Test
    fun sessionInactive_resetsValue(): Unit = runBlocking {
        nightModeIndicatorMonitor.requestControl = FakeUseCaseCameraRequestControl()
        simulateAndMonitorIndicator(EXTENSION_NIGHT_MODE_INDICATOR_ON)

        nightModeIndicatorMonitor.requestControl = null
        Truth.assertThat(nightModeIndicatorMonitor.nightModeIndicatorLiveData.value)
            .isEqualTo(NightModeIndicator.UNKNOWN)
    }

    private fun simulateAndMonitorIndicator(camera2Indicator: Int): Unit = runBlocking {
        val deferred: CompletableDeferred<Int> = CompletableDeferred()
        val observer = Observer<Int> { indicator -> deferred.complete(indicator) }
        try {
            nightModeIndicatorMonitor.nightModeIndicatorLiveData.observeForever(observer)
            simulateNightModeIndicatorUpdate(camera2Indicator)
            shadowOf(getMainLooper()).idle()
            Truth.assertThat(deferred.awaitUntil(1000)).isTrue()
            Truth.assertThat(nightModeIndicatorMonitor.nightModeIndicatorLiveData.value)
                .isEqualTo(NightModeIndicator.RECOMMENDED)
        } finally {
            nightModeIndicatorMonitor.nightModeIndicatorLiveData.removeObserver(observer)
        }
    }

    private fun simulateNightModeIndicatorUpdate(state: Int) =
        comboRequestListener.onTotalCaptureResult(
            FakeRequestMetadata(),
            FrameNumber(0L),
            FakeFrameInfo(FakeFrameMetadata(mapOf(EXTENSION_NIGHT_MODE_INDICATOR to state))),
        )
}
