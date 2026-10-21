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

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.CameraInterop
import androidx.camera.common.CameraId
import androidx.camera.core.impl.SessionConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class CameraInteropStateCallbackRepositoryTest {

    private fun mockCameraDevice(id: String = "0"): CameraDevice =
        mock<CameraDevice>().also { whenever(it.id).thenReturn(id) }

    @Test
    fun clearWhenClosed_deliversOnClosedThenReleasesCallbacks() {
        val repository = CameraInteropStateCallbackRepository()
        var closedCount = 0
        var openedCount = 0
        val deviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    openedCount++
                }

                override fun onClosed(camera: CameraDevice) {
                    closedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val sessionConfig =
            SessionConfig.Builder().apply { addDeviceStateCallback(deviceCallback) }.build()
        val mockDevice = mockCameraDevice("0")

        repository.updateCallbacks(sessionConfig, CameraId("0"))
        repository.deviceStateCallback.onOpened(mockDevice)
        assertThat(openedCount).isEqualTo(1)

        // Unbind all UseCases while device is open
        repository.clearWhenClosed(CameraId("0"))

        // Terminal onClosed must still be delivered to the opened device's callback
        repository.deviceStateCallback.onClosed(mockDevice)
        assertThat(closedCount).isEqualTo(1)

        // Subsequent callbacks must not be delivered because the callback list was cleared
        repository.deviceStateCallback.onOpened(mockDevice)
        assertThat(openedCount).isEqualTo(1)
    }

    @Test
    fun clearWhenClosedBeforeOpen_immediatelyReleasesCallbacks() {
        val repository = CameraInteropStateCallbackRepository()
        var openedCount = 0
        var closedCount = 0
        val deviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    openedCount++
                }

                override fun onClosed(camera: CameraDevice) {
                    closedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val sessionConfig =
            SessionConfig.Builder().apply { addDeviceStateCallback(deviceCallback) }.build()
        val mockDevice = mockCameraDevice("0")

        repository.updateCallbacks(sessionConfig, CameraId("0"))
        // Unbind before onOpened fires (e.g., deferred graph or open cancelled/failed)
        repository.clearWhenClosed(CameraId("0"))

        repository.deviceStateCallback.onOpened(mockDevice)
        repository.deviceStateCallback.onClosed(mockDevice)
        assertThat(openedCount).isEqualTo(0)
        assertThat(closedCount).isEqualTo(0)
    }

    @Test
    fun sessionCallbacks_handlesSessionReplacementAndClearsOnClosed() {
        val repository = CameraInteropStateCallbackRepository()
        var configuredCount = 0
        var closedCount = 0
        val sessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    configuredCount++
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}

                override fun onClosed(session: CameraCaptureSession) {
                    closedCount++
                }
            }
        val sessionConfig =
            SessionConfig.Builder().apply { addSessionStateCallback(sessionCallback) }.build()
        val cameraId = CameraId("0")
        val session1 = CameraInterop.CameraCaptureSessionId(1)
        val session2 = CameraInterop.CameraCaptureSessionId(2)

        repository.updateCallbacks(sessionConfig, cameraId)
        repository.sessionStateCallback.onConfigured(cameraId, session1)
        // Session 2 replaces Session 1 on the same camera without onClosed for Session 1
        repository.sessionStateCallback.onConfigured(cameraId, session2)
        assertThat(configuredCount).isEqualTo(2)

        repository.clearWhenClosed(cameraId)
        repository.sessionStateCallback.onClosed(cameraId, session2)
        assertThat(closedCount).isEqualTo(1)

        // Released after Session 2 closes
        repository.sessionStateCallback.onConfigured(cameraId, session2)
        assertThat(configuredCount).isEqualTo(2)
    }

    @Test
    fun onDisconnectedThenOnClosed_deliversBothAndClearsSessionCallbacks() {
        val repository = CameraInteropStateCallbackRepository()
        var disconnectedCount = 0
        var deviceClosedCount = 0
        var sessionConfiguredCount = 0
        val deviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    deviceClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {
                    disconnectedCount++
                }

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val sessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    sessionConfiguredCount++
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }
        val sessionConfig =
            SessionConfig.Builder()
                .apply {
                    addDeviceStateCallback(deviceCallback)
                    addSessionStateCallback(sessionCallback)
                }
                .build()
        val cameraId = CameraId("0")
        val mockDevice = mockCameraDevice("0")

        repository.updateCallbacks(sessionConfig, cameraId)
        repository.deviceStateCallback.onOpened(mockDevice)
        repository.clearWhenClosed(cameraId)

        // onDisconnected fires before terminal onClosed
        repository.deviceStateCallback.onDisconnected(mockDevice)
        assertThat(disconnectedCount).isEqualTo(1)
        repository.deviceStateCallback.onClosed(mockDevice)
        assertThat(deviceClosedCount).isEqualTo(1)

        // Both device and session callbacks should now be cleared
        repository.deviceStateCallback.onClosed(mockDevice)
        assertThat(deviceClosedCount).isEqualTo(1)
        repository.sessionStateCallback.onConfigured(
            cameraId,
            CameraInterop.CameraCaptureSessionId(1),
        )
        assertThat(sessionConfiguredCount).isEqualTo(0)
    }

    @Test
    fun onErrorBeforeOnOpened_thenClearWhenClosed_stillDeliversOnClosedAndReleases() {
        val repository = CameraInteropStateCallbackRepository()
        var errorCount = 0
        var closedCount = 0
        var sessionConfiguredCount = 0
        val deviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    closedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {
                    errorCount++
                }
            }
        val sessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    sessionConfiguredCount++
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }
        val sessionConfig =
            SessionConfig.Builder()
                .apply {
                    addDeviceStateCallback(deviceCallback)
                    addSessionStateCallback(sessionCallback)
                }
                .build()
        val camera0 = CameraId("0")
        val device0 = mockCameraDevice("0")

        repository.updateCallbacks(sessionConfig, camera0)
        // Camera fails with onError (without onOpened), then UseCases are unbound before onClosed
        repository.deviceStateCallback.onError(
            device0,
            CameraDevice.StateCallback.ERROR_CAMERA_IN_USE,
        )
        assertThat(errorCount).isEqualTo(1)
        repository.clearWhenClosed(camera0)

        repository.deviceStateCallback.onClosed(device0)
        assertThat(closedCount).isEqualTo(1)

        // Both device and session callbacks for camera 0 should now be cleared
        repository.deviceStateCallback.onClosed(device0)
        assertThat(closedCount).isEqualTo(1)
        repository.sessionStateCallback.onConfigured(
            camera0,
            CameraInterop.CameraCaptureSessionId(1),
        )
        assertThat(sessionConfiguredCount).isEqualTo(0)
    }

    @Test
    fun cameraSwitch_deliversEachCamerasCallbacksIndependently() {
        val repository = CameraInteropStateCallbackRepository()
        var cam0DeviceClosed = 0
        var cam0SessionClosed = 0
        var cam1DeviceOpened = 0
        var cam1SessionConfigured = 0

        val cam0DeviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    cam0DeviceClosed++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val cam0SessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {}

                override fun onConfigureFailed(session: CameraCaptureSession) {}

                override fun onClosed(session: CameraCaptureSession) {
                    cam0SessionClosed++
                }
            }
        val cam1DeviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cam1DeviceOpened++
                }

                override fun onClosed(camera: CameraDevice) {}

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val cam1SessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cam1SessionConfigured++
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }

        val config0 =
            SessionConfig.Builder()
                .apply {
                    addDeviceStateCallback(cam0DeviceCallback)
                    addSessionStateCallback(cam0SessionCallback)
                }
                .build()
        val config1 =
            SessionConfig.Builder()
                .apply {
                    addDeviceStateCallback(cam1DeviceCallback)
                    addSessionStateCallback(cam1SessionCallback)
                }
                .build()

        val camera0 = CameraId("0")
        val camera1 = CameraId("1")
        val device0 = mockCameraDevice("0")
        val device1 = mockCameraDevice("1")
        val session0 = CameraInterop.CameraCaptureSessionId(10)
        val session1 = CameraInterop.CameraCaptureSessionId(20)

        // Camera 0 opens and configures session
        repository.updateCallbacks(config0, camera0)
        repository.deviceStateCallback.onOpened(device0)
        repository.sessionStateCallback.onConfigured(camera0, session0)

        // Switch from Camera 0 to Camera 1 before Camera 0 finishes closing asynchronously
        repository.clearWhenClosed(camera0)
        repository.updateCallbacks(config1, camera1)

        // Camera 0's async close callbacks must still reach Camera 0's listeners (and not Camera 1)
        repository.sessionStateCallback.onClosed(camera0, session0)
        repository.deviceStateCallback.onClosed(device0)
        assertThat(cam0SessionClosed).isEqualTo(1)
        assertThat(cam0DeviceClosed).isEqualTo(1)

        // Camera 1's open/configure callbacks reach Camera 1's listeners
        repository.deviceStateCallback.onOpened(device1)
        repository.sessionStateCallback.onConfigured(camera1, session1)
        assertThat(cam1DeviceOpened).isEqualTo(1)
        assertThat(cam1SessionConfigured).isEqualTo(1)
    }

    @Test
    fun rebindWithDistinctSessionCallbacks_deliversOldCloseToOldCallback() {
        val repository = CameraInteropStateCallbackRepository()
        var callback1Configured = 0
        var callback1Closed = 0
        var callback2Configured = 0
        var callback2Closed = 0
        val callback1 =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    callback1Configured++
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}

                override fun onClosed(session: CameraCaptureSession) {
                    callback1Closed++
                }
            }
        val callback2 =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    callback2Configured++
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}

                override fun onClosed(session: CameraCaptureSession) {
                    callback2Closed++
                }
            }
        val config1 = SessionConfig.Builder().apply { addSessionStateCallback(callback1) }.build()
        val config2 = SessionConfig.Builder().apply { addSessionStateCallback(callback2) }.build()
        val camera0 = CameraId("0")
        val session1 = CameraInterop.CameraCaptureSessionId(1)
        val session2 = CameraInterop.CameraCaptureSessionId(2)

        repository.updateCallbacks(config1, camera0)
        repository.sessionStateCallback.onConfigured(camera0, session1)
        assertThat(callback1Configured).isEqualTo(1)

        // Rebind with config2; session2.onConfigured arrives before session1.onClosed
        repository.updateCallbacks(config2, camera0)
        repository.sessionStateCallback.onConfigured(camera0, session2)
        assertThat(callback2Configured).isEqualTo(1)

        repository.sessionStateCallback.onClosed(camera0, session1)
        assertThat(callback1Closed).isEqualTo(1)
        assertThat(callback2Closed).isEqualTo(0)

        repository.clearWhenClosed(camera0)
        repository.sessionStateCallback.onClosed(camera0, session2)
        assertThat(callback2Closed).isEqualTo(1)
    }

    @Test
    fun rebindAfterClearWhenClosedWhileDeviceClosing_deliversOldCloseToOldCallback() {
        val repository = CameraInteropStateCallbackRepository()
        var gen1ClosedCount = 0
        var gen2OpenedCount = 0
        var gen2ClosedCount = 0
        val gen1Callback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    gen1ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val gen2Callback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    gen2OpenedCount++
                }

                override fun onClosed(camera: CameraDevice) {
                    gen2ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val config1 = SessionConfig.Builder().apply { addDeviceStateCallback(gen1Callback) }.build()
        val config2 = SessionConfig.Builder().apply { addDeviceStateCallback(gen2Callback) }.build()
        val camera0 = CameraId("0")
        val device1 = mockCameraDevice("0")
        val device2 = mockCameraDevice("0")

        repository.updateCallbacks(config1, camera0)
        repository.deviceStateCallback.onOpened(device1)
        repository.clearWhenClosed(camera0)

        // Rebind with reuseOpenDevices = true before device1 finishes closing
        repository.updateCallbacks(config2, camera0, reuseOpenDevices = true)
        repository.deviceStateCallback.onClosed(device1)
        assertThat(gen1ClosedCount).isEqualTo(1)
        assertThat(gen2ClosedCount).isEqualTo(0)

        repository.deviceStateCallback.onOpened(device2)
        assertThat(gen2OpenedCount).isEqualTo(1)
        repository.clearWhenClosed(camera0)
        repository.deviceStateCallback.onClosed(device2)
        assertThat(gen2ClosedCount).isEqualTo(1)
    }

    @Test
    fun rebindWithReusedCameraDevice_routesEventsToCurrentCallbacksAndDropsPrevious() {
        val repository = CameraInteropStateCallbackRepository()
        var gen1ErrorCount = 0
        var gen1DisconnectedCount = 0
        var gen1ClosedCount = 0
        var gen2ErrorCount = 0
        var gen2DisconnectedCount = 0
        var gen2ClosedCount = 0

        val gen1Callback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    gen1ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {
                    gen1DisconnectedCount++
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    gen1ErrorCount++
                }
            }
        val gen2Callback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    gen2ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {
                    gen2DisconnectedCount++
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    gen2ErrorCount++
                }
            }
        val config1 = SessionConfig.Builder().apply { addDeviceStateCallback(gen1Callback) }.build()
        val config2 = SessionConfig.Builder().apply { addDeviceStateCallback(gen2Callback) }.build()
        val camera0 = CameraId("0")
        val device0 = mockCameraDevice("0")

        // Generation 1 opens device0
        repository.updateCallbacks(config1, camera0, reuseOpenDevices = true)
        repository.deviceStateCallback.onOpened(device0)

        // Normal rebind (reuseOpenDevices = true): device0 remains open without a second onOpened
        repository.updateCallbacks(config2, camera0, reuseOpenDevices = true)

        // Events while generation 2 is bound must route to gen2Callback, not gen1Callback
        repository.deviceStateCallback.onDisconnected(device0)
        repository.deviceStateCallback.onError(
            device0,
            CameraDevice.StateCallback.ERROR_CAMERA_DEVICE,
        )
        repository.deviceStateCallback.onClosed(device0)

        assertThat(gen1DisconnectedCount).isEqualTo(0)
        assertThat(gen1ErrorCount).isEqualTo(0)
        assertThat(gen1ClosedCount).isEqualTo(0)
        assertThat(gen2DisconnectedCount).isEqualTo(1)
        assertThat(gen2ErrorCount).isEqualTo(1)
        assertThat(gen2ClosedCount).isEqualTo(1)

        // Because device0 already closed, clearWhenClosed leaves no retained state
        repository.clearWhenClosed(camera0)
        repository.deviceStateCallback.onClosed(device0)
        assertThat(gen2ClosedCount).isEqualTo(1)
    }

    @Test
    fun overlappingCameraOpenDuringQuirkRebind_routesOldDeviceCloseToOldCallback() {
        val repository = CameraInteropStateCallbackRepository()
        var gen1ClosedCount = 0
        var gen2OpenedCount = 0
        var gen2ClosedCount = 0
        val gen1Callback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    gen1ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val gen2Callback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    gen2OpenedCount++
                }

                override fun onClosed(camera: CameraDevice) {
                    gen2ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val config1 = SessionConfig.Builder().apply { addDeviceStateCallback(gen1Callback) }.build()
        val config2 = SessionConfig.Builder().apply { addDeviceStateCallback(gen2Callback) }.build()
        val camera0 = CameraId("0")
        val device1 = mockCameraDevice("0")
        val device2 = mockCameraDevice("0")

        // Generation 1 opens device1
        repository.updateCallbacks(config1, camera0, reuseOpenDevices = false)
        repository.deviceStateCallback.onOpened(device1)

        // Quirk rebind (reuseOpenDevices = false) while device1 is still closing
        repository.updateCallbacks(config2, camera0, reuseOpenDevices = false)
        // Late close of device1 routes to gen1Callback while device2 opens with gen2Callback
        repository.deviceStateCallback.onClosed(device1)
        assertThat(gen1ClosedCount).isEqualTo(1)

        repository.deviceStateCallback.onOpened(device2)
        repository.clearWhenClosed(camera0)
        repository.deviceStateCallback.onClosed(device2)
        assertThat(gen2OpenedCount).isEqualTo(1)
        assertThat(gen2ClosedCount).isEqualTo(1)
    }

    @Test
    fun deviceCloseWhenSessionCloseOmittedByHal_releasesSessionCallbacks() {
        val repository = CameraInteropStateCallbackRepository()
        var sessionConfiguredCount = 0
        var sessionActiveCount = 0
        var deviceClosedCount = 0
        val deviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    deviceClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val sessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    sessionConfiguredCount++
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}

                override fun onActive(session: CameraCaptureSession) {
                    sessionActiveCount++
                }
            }
        val sessionConfig =
            SessionConfig.Builder()
                .apply {
                    addDeviceStateCallback(deviceCallback)
                    addSessionStateCallback(sessionCallback)
                }
                .build()
        val camera0 = CameraId("0")
        val device0 = mockCameraDevice("0")
        val session0 = CameraInterop.CameraCaptureSessionId(1)

        repository.updateCallbacks(sessionConfig, camera0)
        repository.deviceStateCallback.onOpened(device0)
        repository.sessionStateCallback.onConfigured(camera0, session0)
        assertThat(sessionConfiguredCount).isEqualTo(1)

        // Unbind while device0 is closing asynchronously
        repository.clearWhenClosed(camera0)

        // Device finishes closing, and HAL omits CameraCaptureSession.onClosed (b/146773463)
        repository.deviceStateCallback.onClosed(device0)
        assertThat(deviceClosedCount).isEqualTo(1)

        // Session callback state must now be released even though session.onClosed never arrived
        repository.sessionStateCallback.onActive(camera0, session0)
        assertThat(sessionActiveCount).isEqualTo(0)
    }

    @Test
    fun unbindThenRebindReusingOpenDevice_routesEventsToNewCallbacksAndReleasesOld() {
        val repository = CameraInteropStateCallbackRepository()
        var gen1ClosedCount = 0
        var gen2ConfiguredCount = 0
        var gen2ClosedCount = 0

        val gen1DeviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    gen1ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val gen2DeviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    gen2ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val gen2SessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    gen2ConfiguredCount++
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }

        val config1 =
            SessionConfig.Builder().apply { addDeviceStateCallback(gen1DeviceCallback) }.build()
        val config2 =
            SessionConfig.Builder()
                .apply {
                    addDeviceStateCallback(gen2DeviceCallback)
                    addSessionStateCallback(gen2SessionCallback)
                }
                .build()
        val camera0 = CameraId("0")
        val device0 = mockCameraDevice("0")
        val session1 = CameraInterop.CameraCaptureSessionId(1)
        val session2 = CameraInterop.CameraCaptureSessionId(2)

        repository.updateCallbacks(config1, camera0, reuseOpenDevices = true)
        repository.deviceStateCallback.onOpened(device0)
        repository.sessionStateCallback.onConfigured(camera0, session1)

        // Detach all UseCases, then immediately re-attach within ActiveCamera's 1s WakeLock window:
        // camera-pipe reuses device0 without calling onOpened again.
        repository.clearWhenClosed(camera0)
        repository.updateCallbacks(config2, camera0, reuseOpenDevices = true)
        repository.sessionStateCallback.onConfigured(camera0, session2)
        assertThat(gen2ConfiguredCount).isEqualTo(1)

        // Subsequent close on the reused device0 (even without onDisconnected/onError) must route
        // to gen2, not gen1
        repository.deviceStateCallback.onClosed(device0)
        assertThat(gen1ClosedCount).isEqualTo(0)
        assertThat(gen2ClosedCount).isEqualTo(1)
    }

    @Test
    fun rebindWithReuseTrueWhenDeviceClosesBeforeNewDeviceOpens_routesOldCloseToOldCallback() {
        val repository = CameraInteropStateCallbackRepository()
        var gen1ClosedCount = 0
        var gen2OpenedCount = 0
        var gen2ClosedCount = 0
        val gen1Callback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onClosed(camera: CameraDevice) {
                    gen1ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val gen2Callback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    gen2OpenedCount++
                }

                override fun onClosed(camera: CameraDevice) {
                    gen2ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val config1 = SessionConfig.Builder().apply { addDeviceStateCallback(gen1Callback) }.build()
        val config2 = SessionConfig.Builder().apply { addDeviceStateCallback(gen2Callback) }.build()
        val camera0 = CameraId("0")
        val device1 = mockCameraDevice("0")
        val device2 = mockCameraDevice("0")

        repository.updateCallbacks(config1, camera0, reuseOpenDevices = true)
        repository.deviceStateCallback.onOpened(device1)

        // Rebind where reuseOpenDevices is true, but camera-pipe closes device1 before opening
        // device2 (e.g., LEGACY close-before-createCaptureSession)
        repository.updateCallbacks(config2, camera0, reuseOpenDevices = true)
        repository.deviceStateCallback.onClosed(device1)
        assertThat(gen1ClosedCount).isEqualTo(1)
        assertThat(gen2ClosedCount).isEqualTo(0)

        repository.deviceStateCallback.onOpened(device2)
        assertThat(gen2OpenedCount).isEqualTo(1)
        repository.clearWhenClosed(camera0)
        repository.deviceStateCallback.onClosed(device2)
        assertThat(gen2ClosedCount).isEqualTo(1)
    }

    @Test
    fun legacyReopenDuringClose_deliversOnClosedToOriginalDeviceAfterTempDeviceCloses() {
        val repository = CameraInteropStateCallbackRepository()
        var gen1OpenedCount = 0
        var gen1ClosedCount = 0
        val gen1Callback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    gen1OpenedCount++
                }

                override fun onClosed(camera: CameraDevice) {
                    gen1ClosedCount++
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val config1 = SessionConfig.Builder().apply { addDeviceStateCallback(gen1Callback) }.build()
        val camera0 = CameraId("0")
        val originalDevice = mockCameraDevice("0")
        val reopenedTempDevice = mockCameraDevice("0")

        repository.updateCallbacks(config1, camera0)
        repository.deviceStateCallback.onOpened(originalDevice)
        assertThat(gen1OpenedCount).isEqualTo(1)

        // Unbind and simulate LEGACY Camera2DeviceCloser reopen-on-close sequence:
        // tempDevice opens and closes before androidCameraState.onFinalized(originalDevice)
        // delivers onClosed(originalDevice).
        repository.clearWhenClosed(camera0)
        repository.deviceStateCallback.onOpened(reopenedTempDevice)
        repository.deviceStateCallback.onClosed(reopenedTempDevice)
        assertThat(gen1OpenedCount).isEqualTo(1)
        assertThat(gen1ClosedCount).isEqualTo(0)

        repository.deviceStateCallback.onClosed(originalDevice)
        assertThat(gen1ClosedCount).isEqualTo(1)
    }

    @Test
    fun rebindWithNewDevice_deliversTrailingSessionCloseAfterNewSessionConfigured() {
        val repository = CameraInteropStateCallbackRepository()
        var gen1SessionClosedCount = 0
        var gen2SessionConfiguredCount = 0
        val gen1SessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {}

                override fun onConfigureFailed(session: CameraCaptureSession) {}

                override fun onClosed(session: CameraCaptureSession) {
                    gen1SessionClosedCount++
                }
            }
        val gen2SessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    gen2SessionConfiguredCount++
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }
        val config1 =
            SessionConfig.Builder().apply { addSessionStateCallback(gen1SessionCallback) }.build()
        val config2 =
            SessionConfig.Builder().apply { addSessionStateCallback(gen2SessionCallback) }.build()
        val camera0 = CameraId("0")
        val device1 = mockCameraDevice("0")
        val device2 = mockCameraDevice("0")
        val session1 = CameraInterop.CameraCaptureSessionId(1)
        val session2 = CameraInterop.CameraCaptureSessionId(2)

        repository.updateCallbacks(config1, camera0, reuseOpenDevices = false)
        repository.deviceStateCallback.onOpened(device1)
        repository.sessionStateCallback.onConfigured(camera0, session1)

        // Rebind opens device2 and configures session2 before session1's trailing onClosed arrives
        repository.updateCallbacks(config2, camera0, reuseOpenDevices = false)
        repository.deviceStateCallback.onOpened(device2)
        repository.sessionStateCallback.onConfigured(camera0, session2)
        assertThat(gen2SessionConfiguredCount).isEqualTo(1)

        repository.sessionStateCallback.onClosed(camera0, session1)
        assertThat(gen1SessionClosedCount).isEqualTo(1)
    }
}
