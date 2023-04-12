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

import android.hardware.camera2.CameraDevice
import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Token
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.testing.FakeCamera2DeviceCloser
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCoroutinesApi::class)
internal class VirtualCameraStateTest {
    private val mainLooper = shadowOf(getMainLooper())
    private val cameraId = RobolectricCameras.create()
    private val testCamera = RobolectricCameras.open(cameraId)
    private val graphListener: GraphListener = mock()
    private val cameraErrorListener: CameraErrorListener = mock()

    @After
    fun teardown() {
        mainLooper.idle()
        RobolectricCameras.clear()
    }

    @Test
    fun virtualCameraStateCanBeDisconnected() = runTest {
        // This test asserts that the virtual camera starts in an unopened state and is changed to
        // "Closed" when disconnect is invoked on the VirtualCamera.
        val virtualCamera = VirtualCameraState(cameraId, graphListener)
        assertThat(virtualCamera.value).isInstanceOf(CameraStateUnopened::class.java)

        virtualCamera.disconnect()
        assertThat(virtualCamera.value).isInstanceOf(CameraStateClosed::class.java)

        val closedState = virtualCamera.value as CameraStateClosed
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
        val virtualCamera = VirtualCameraState(cameraId, graphListener)
        val cameraState =
            flowOf(
                CameraStateOpen(
                    AndroidCameraDevice(
                        testCamera.metadata,
                        testCamera.cameraDevice,
                        testCamera.cameraId,
                        cameraErrorListener,
                    )
                )
            )
        virtualCamera.connect(
            cameraState,
            object : Token {
                override fun release(): Boolean {
                    return true
                }
            })

        virtualCamera.state.first { it !is CameraStateUnopened }

        assertThat(virtualCamera.value).isInstanceOf(CameraStateOpen::class.java)
        virtualCamera.disconnect()
        assertThat(virtualCamera.value).isInstanceOf(CameraStateClosed::class.java)

        val closedState = virtualCamera.value as CameraStateClosed
        assertThat(closedState.cameraId).isEqualTo(cameraId)
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.APP_DISCONNECTED)
    }

    @Test
    fun virtualCameraStateRespondsToClose() = runTest {
        // This tests that a listener attached to the virtualCamera.state property will receive all
        // of the events, starting from CameraStateUnopened.
        val virtualCamera = VirtualCameraState(cameraId, graphListener)
        val androidCameraDevice = AndroidCameraDevice(
            testCamera.metadata,
            testCamera.cameraDevice,
            testCamera.cameraId,
            cameraErrorListener,
        )
        val cameraStateClosing = CameraStateClosing()
        val cameraStateClosed =
            CameraStateClosed(
                cameraId,
                ClosedReason.CAMERA2_ERROR,
                cameraErrorCode = CameraError.ERROR_CAMERA_SERVICE
            )
        val states =
            listOf(
                CameraStateOpen(androidCameraDevice),
                cameraStateClosing,
                cameraStateClosed
            )

        val events = mutableListOf<CameraState>()
        val job = launch { virtualCamera.state.collect { events.add(it) } }

        virtualCamera.connect(
            states.asFlow(),
            object : Token {
                override fun release(): Boolean {
                    return true
                }
            })

        advanceUntilIdle()
        job.cancelAndJoin()

        assertThat(events[0]).isSameInstanceAs(CameraStateUnopened)

        assertThat(events[1]).isInstanceOf(CameraStateOpen::class.java)
        val deviceWrapper = (events[1] as CameraStateOpen).cameraDevice
        assertThat(deviceWrapper).isInstanceOf(VirtualAndroidCameraDevice::class.java)
        val androidCameraStateInside =
            (deviceWrapper as VirtualAndroidCameraDevice).androidCameraDevice

        assertThat(androidCameraStateInside).isSameInstanceAs(androidCameraDevice)
        assertThat(events[2]).isSameInstanceAs(cameraStateClosing)
        assertThat(events[3]).isSameInstanceAs(cameraStateClosed)
    }

    @Test
    fun virtualAndroidCameraDeviceRejectsCallsWhenVirtualCameraStateIsDisconnected() = runTest {
        val virtualCamera = VirtualCameraState(cameraId, graphListener)
        val cameraState =
            flowOf(
                CameraStateOpen(
                    AndroidCameraDevice(
                        testCamera.metadata,
                        testCamera.cameraDevice,
                        testCamera.cameraId,
                        cameraErrorListener,
                    )
                )
            )
        virtualCamera.connect(
            cameraState,
            object : Token {
                override fun release(): Boolean {
                    return true
                }
            })

        virtualCamera.state.first { it !is CameraStateUnopened }

        val virtualCameraState = virtualCamera.value
        assertThat(virtualCameraState).isInstanceOf(CameraStateOpen::class.java)
        val deviceWrapper = (virtualCameraState as CameraStateOpen).cameraDevice
        assertThat(deviceWrapper).isInstanceOf(VirtualAndroidCameraDevice::class.java)

        val virtualAndroidCameraState = deviceWrapper as VirtualAndroidCameraDevice
        val result1 = virtualAndroidCameraState.createCaptureRequest(RequestTemplate(2))
        virtualCamera.disconnect()
        val result2 = virtualAndroidCameraState.createCaptureRequest(RequestTemplate(2))
        assertThat(result1).isNotNull()
        assertThat(result2).isNull()
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class AndroidCameraDeviceTest {
    private val mainLooper = shadowOf(getMainLooper())
    private val cameraId = RobolectricCameras.create()
    private val testCamera = RobolectricCameras.open(cameraId)
    private val timeSource: TimeSource = SystemTimeSource()
    private val cameraDeviceCloser = FakeCamera2DeviceCloser()
    private val now = Timestamps.now(timeSource)
    private val cameraErrorListener = object : CameraErrorListener {
        var lastCameraId: CameraId? = null
        var lastCameraError: CameraError? = null

        override fun onCameraError(
            cameraId: CameraId,
            cameraError: CameraError,
            willAttemptRetry: Boolean
        ) {
            lastCameraId = cameraId
            lastCameraError = cameraError
        }
    }

    @After
    fun teardown() {
        RobolectricCameras.clear()
    }

    @Test
    fun cameraOpensAndGeneratesStats() {
        mainLooper.idleFor(200, TimeUnit.MILLISECONDS)
        val listener =
            AndroidCameraState(
                testCamera.cameraId,
                testCamera.metadata,
                attemptNumber = 1,
                attemptTimestampNanos = now,
                timeSource,
                cameraErrorListener,
                cameraDeviceCloser,
            )

        assertThat(listener.state.value).isInstanceOf(CameraStateUnopened.javaClass)

        // Advance the system clocks.
        mainLooper.idleFor(200, TimeUnit.MILLISECONDS)
        listener.onOpened(testCamera.cameraDevice)

        assertThat(listener.state.value).isInstanceOf(CameraStateOpen::class.java)
        assertThat(
            (listener.state.value as CameraStateOpen)
                .cameraDevice
                .unwrapAs(CameraDevice::class)
        )
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
        val listener =
            AndroidCameraState(
                testCamera.cameraId,
                testCamera.metadata,
                attemptNumber = 1,
                attemptTimestampNanos = now,
                timeSource,
                cameraErrorListener,
                cameraDeviceCloser,
            )

        listener.onDisconnected(testCamera.cameraDevice)
        listener.onError(testCamera.cameraDevice, CameraDevice.StateCallback.ERROR_CAMERA_SERVICE)
        listener.onClosed(testCamera.cameraDevice)

        mainLooper.idle()

        val closedState = listener.state.value as CameraStateClosed
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.CAMERA2_DISCONNECTED)
    }

    @Test
    fun closingStateReportsAppClose() {
        val listener =
            AndroidCameraState(
                testCamera.cameraId,
                testCamera.metadata,
                attemptNumber = 1,
                attemptTimestampNanos = now,
                timeSource,
                cameraErrorListener,
                cameraDeviceCloser,
            )

        listener.close()
        mainLooper.idle()

        val closedState = listener.state.value as CameraStateClosed
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.APP_CLOSED)
    }

    @Test
    fun closingWithExceptionIsReported() {
        val listener =
            AndroidCameraState(
                testCamera.cameraId,
                testCamera.metadata,
                attemptNumber = 1,
                attemptTimestampNanos = now,
                timeSource,
                cameraErrorListener,
                cameraDeviceCloser,
            )

        listener.closeWith(IllegalArgumentException("Test Exception"))
        mainLooper.idle()

        val closedState = listener.state.value as CameraStateClosed
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.CAMERA2_EXCEPTION)
    }

    @Test
    fun errorCodesAreReported() {
        val listener =
            AndroidCameraState(
                testCamera.cameraId,
                testCamera.metadata,
                attemptNumber = 1,
                attemptTimestampNanos = now,
                timeSource,
                cameraErrorListener,
                cameraDeviceCloser,
            )

        listener.onError(testCamera.cameraDevice, CameraDevice.StateCallback.ERROR_CAMERA_SERVICE)
        mainLooper.idle()

        val closedState = listener.state.value as CameraStateClosed
        assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.CAMERA2_ERROR)
        assertThat(closedState.cameraErrorCode).isEqualTo(CameraError.ERROR_CAMERA_SERVICE)
        assertThat(closedState.cameraException).isNull()
    }

    @Test
    fun errorCodesAreReportedToGraphListener() {
        val listener =
            AndroidCameraState(
                testCamera.cameraId,
                testCamera.metadata,
                attemptNumber = 1,
                attemptTimestampNanos = now,
                timeSource,
                cameraErrorListener,
                cameraDeviceCloser,
            )

        listener.onOpened(testCamera.cameraDevice)
        listener.onError(testCamera.cameraDevice, CameraDevice.StateCallback.ERROR_CAMERA_SERVICE)
        mainLooper.idle()
        assertThat(cameraErrorListener.lastCameraId).isEqualTo(testCamera.cameraId)
        assertThat(cameraErrorListener.lastCameraError).isEqualTo(CameraError.ERROR_CAMERA_SERVICE)
    }

    @Test
    fun errorCodesAreReportedToGraphListenerWhenCameraIsNotOpened() {
        // Unless this is a camera open exception, all errors should be reported even if camera is
        // not opened. The main reason is CameraAccessException.CAMERA_ERROR, where under which, we
        // only know the nature of the error true onError(), and we should and would report that.
        val listener =
            AndroidCameraState(
                testCamera.cameraId,
                testCamera.metadata,
                attemptNumber = 1,
                attemptTimestampNanos = now,
                timeSource,
                cameraErrorListener,
                cameraDeviceCloser
            )

        listener.onError(testCamera.cameraDevice, CameraDevice.StateCallback.ERROR_CAMERA_SERVICE)
        mainLooper.idle()
        assertThat(cameraErrorListener.lastCameraId).isEqualTo(testCamera.cameraId)
        assertThat(cameraErrorListener.lastCameraError).isEqualTo(CameraError.ERROR_CAMERA_SERVICE)
    }
}
