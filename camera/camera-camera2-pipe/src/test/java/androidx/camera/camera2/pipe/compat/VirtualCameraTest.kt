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

package androidx.camera.camera2.pipe.compat

import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import androidx.camera.camera2.pipe.core.Token
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runBlockingTest

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCoroutinesApi::class)
internal class VirtualCameraStateTest {
    private val mainLooper = shadowOf(getMainLooper())
    private val cameraId = RobolectricCameras.create()
    private val testCamera = RobolectricCameras.open(cameraId)

    @After
    fun teardown() {
        mainLooper.idle()
        RobolectricCameras.clear()
    }

    @Test
    fun virtualCameraStateCanBeDisconnected() = runTest(UnconfinedTestDispatcher()) {
        // This test asserts that the virtual camera starts in an unopened state and is changed to
        // "Closed" when disconnect is invoked on the VirtualCamera.
        val virtualCamera = VirtualCameraState(cameraId)
        assertThat(virtualCamera.state.value).isInstanceOf(CameraStateUnopened.javaClass)

        virtualCamera.disconnect()
        assertThat(virtualCamera.state.value).isInstanceOf(CameraStateClosed::class.java)

        val closedState = virtualCamera.state.value as CameraStateClosed
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.APP_DISCONNECTED)

        // Disconnecting a virtual camera does not propagate statistics.
        assertThat(closedState.cameraErrorCode).isNull()
        assertThat(closedState.cameraException).isNull()
        assertThat(closedState.cameraRetryCount).isNull()
        assertThat(closedState.cameraRetryDurationNs).isNull()
        assertThat(closedState.cameraOpenDurationNs).isNull()
        assertThat(closedState.cameraActiveDurationNs).isNull()
        assertThat(closedState.cameraClosingDurationNs).isNull()
    }

    @Test
    fun virtualCameraStateConnectsToFlow() = runTest {
        // This test asserts that when a virtual camera is connected to a flow of CameraState
        // changes that it receives those changes and can be subsequently disconnected, which stops
        // additional events from being passed to the virtual camera instance.
        val virtualCamera = VirtualCameraState(cameraId)
        val cameraState = flowOf(
            CameraStateOpen(
                AndroidCameraDevice(
                    testCamera.metadata,
                    testCamera.cameraDevice,
                    testCamera.cameraId
                )
            )
        )
        virtualCamera.connect(
            cameraState,
            object : Token {
                override fun release(): Boolean {
                    return true
                }
            }
        )

        virtualCamera.state.first { it !is CameraStateUnopened }

        assertThat(virtualCamera.state.value).isInstanceOf(CameraStateOpen::class.java)
        virtualCamera.disconnect()
        assertThat(virtualCamera.state.value).isInstanceOf(CameraStateClosed::class.java)

        val closedState = virtualCamera.state.value as CameraStateClosed
        assertThat(closedState.cameraId).isEqualTo(cameraId)
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.APP_DISCONNECTED)
    }

    @Suppress("DEPRECATION") // fails with runTest {} api - b/220870228
    @Test
    fun virtualCameraStateRespondsToClose() = runBlockingTest {
        // This tests that a listener attached to the virtualCamera.state property will receive all
        // of the events, starting from CameraStateUnopened.
        val virtualCamera = VirtualCameraState(cameraId)
        val states = listOf(
            CameraStateOpen(
                AndroidCameraDevice(
                    testCamera.metadata,
                    testCamera.cameraDevice,
                    testCamera.cameraId
                )
            ),
            CameraStateClosing,
            CameraStateClosed(
                cameraId,
                ClosedReason.CAMERA2_ERROR,
                cameraErrorCode = 5
            )
        )

        val events = mutableListOf<CameraState>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            virtualCamera.state.collect {
                events.add(it)
            }
        }

        virtualCamera.connect(
            states.asFlow(),
            object : Token {
                override fun release(): Boolean {
                    return true
                }
            }
        )

        // Suspend until the state is closed
        virtualCamera.state.first { it is CameraStateClosed }
        job.cancelAndJoin()

        val expectedStates = listOf(CameraStateUnopened).plus(states)
        assertThat(events).containsExactlyElementsIn(expectedStates)
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCoroutinesApi::class)
internal class AndroidCameraDeviceTest {
    private val mainLooper = shadowOf(getMainLooper())
    private val cameraId = RobolectricCameras.create()
    private val testCamera = RobolectricCameras.open(cameraId)
    private val now = Timestamps.now()

    @After
    fun teardown() {
        RobolectricCameras.clear()
    }

    @Test
    fun cameraOpensAndGeneratesStats() {
        mainLooper.idleFor(200, TimeUnit.MILLISECONDS)
        val listener = AndroidCameraState(
            testCamera.cameraId,
            testCamera.metadata,
            attemptNumber = 1,
            attemptTimestampNanos = now
        )

        assertThat(listener.state.value).isInstanceOf(CameraStateUnopened.javaClass)

        // Advance the system clocks.
        mainLooper.idleFor(200, TimeUnit.MILLISECONDS)
        listener.onOpened(testCamera.cameraDevice)

        assertThat(listener.state.value).isInstanceOf(CameraStateOpen::class.java)
        assertThat((listener.state.value as CameraStateOpen).cameraDevice.unwrap())
            .isSameInstanceAs(testCamera.cameraDevice)

        mainLooper.idleFor(1000, TimeUnit.MILLISECONDS)
        listener.onClosed(testCamera.cameraDevice)
        mainLooper.idle()

        assertThat(listener.state.value).isInstanceOf(CameraStateClosed::class.java)
        val closedState = listener.state.value as CameraStateClosed

        assertThat(closedState.cameraId).isEqualTo(cameraId)
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.CAMERA2_CLOSED)
        assertThat(closedState.cameraRetryCount).isEqualTo(0)
        assertThat(closedState.cameraException).isNull()
        assertThat(closedState.cameraRetryDurationNs?.value).isAtLeast(1)
        assertThat(closedState.cameraOpenDurationNs?.value).isAtLeast(1)
        assertThat(closedState.cameraActiveDurationNs?.value).isAtLeast(1)

        // Closing duration measures how long "close()" takes to invoke on the camera device.
        // However, shimming the clocks is difficult.
        assertThat(closedState.cameraClosingDurationNs).isNotNull()
    }

    @Test
    fun multipleCloseEventsReportFirstEvent() {
        val listener = AndroidCameraState(
            testCamera.cameraId,
            testCamera.metadata,
            attemptNumber = 1,
            attemptTimestampNanos = now
        )

        listener.onDisconnected(testCamera.cameraDevice)
        listener.onError(testCamera.cameraDevice, 42)
        listener.onClosed(testCamera.cameraDevice)

        mainLooper.idle()

        val closedState = listener.state.value as CameraStateClosed
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.CAMERA2_DISCONNECTED)
    }

    @Test
    fun closingStateReportsAppClose() {
        val listener = AndroidCameraState(
            testCamera.cameraId,
            testCamera.metadata,
            attemptNumber = 1,
            attemptTimestampNanos = now
        )

        listener.close()
        mainLooper.idle()

        val closedState = listener.state.value as CameraStateClosed
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.APP_CLOSED)
    }

    @Test
    fun closingWithExceptionIsReported() {
        val listener = AndroidCameraState(
            testCamera.cameraId,
            testCamera.metadata,
            attemptNumber = 1,
            attemptTimestampNanos = now
        )

        listener.closeWith(IllegalStateException("Test Exception"))
        mainLooper.idle()

        val closedState = listener.state.value as CameraStateClosed
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.CAMERA2_EXCEPTION)
    }

    @Test
    fun errorCodesAreReported() {
        val listener = AndroidCameraState(
            testCamera.cameraId,
            testCamera.metadata,
            attemptNumber = 1,
            attemptTimestampNanos = now
        )

        listener.onError(testCamera.cameraDevice, 24)
        mainLooper.idle()

        val closedState = listener.state.value as CameraStateClosed
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.CAMERA2_ERROR)
        assertThat(closedState.cameraErrorCode).isEqualTo(24)
        assertThat(closedState.cameraException).isNull()
    }
}