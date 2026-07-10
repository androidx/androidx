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

package androidx.camera.integration.core

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * A test agent to control camera behavior in Robolectric tests.
 *
 * This agent manages availability callbacks and tracks open camera devices, allowing tests to
 * simulate specific event sequences.
 */
class ShadowCameraAgent(val testHandler: Handler) {

    private var listenerRegisteredLatch = CountDownLatch(1)
    private var nextOpenError: DeviceOpenError? = null

    private data class OpenCameraInfo(
        val device: CameraDevice,
        val callback: CameraDevice.StateCallback,
        val executor: Executor,
    )

    // Using IdentityHashMap to ensure we track specific CameraDevice instances by reference.
    // Regular HashMaps might fail if the framework stub's equals/hashCode is not tied to identity.
    private val openCameras = java.util.IdentityHashMap<CameraDevice, OpenCameraInfo>()

    // Dual-index for O(1) lookup by cameraId.
    private val cameraIdToDevices = mutableMapOf<String, MutableSet<CameraDevice>>()

    private val availabilityCallbacks = LinkedHashMap<CameraManager.AvailabilityCallback, Handler>()
    private val availabilityCallbackExecutors =
        LinkedHashMap<CameraManager.AvailabilityCallback, Executor>()

    private val lock = Any()

    enum class DeviceErrorScenario {
        ON_ERROR,
        ON_DISCONNECTED,
    }

    enum class DeviceOpenError(val errorCode: Int) {
        CAMERA_IN_USE(CameraDevice.StateCallback.ERROR_CAMERA_IN_USE),
        MAX_CAMERAS_IN_USE(CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE),
        CAMERA_DISABLED(CameraDevice.StateCallback.ERROR_CAMERA_DISABLED),
        CAMERA_DEVICE_ERROR(CameraDevice.StateCallback.ERROR_CAMERA_DEVICE),
        CAMERA_SERVICE_ERROR(CameraDevice.StateCallback.ERROR_CAMERA_SERVICE),
    }

    /** Manually triggers the onCameraAvailable callback for all registered listeners. */
    fun triggerOnCameraAvailable(cameraId: String) {
        Log.d("ShadowAgent", "Triggering onCameraAvailable for $cameraId")
        synchronized(lock) {
            availabilityCallbacks.forEach { (callback, handler) ->
                handler.post { callback.onCameraAvailable(cameraId) }
            }
            availabilityCallbackExecutors.forEach { (callback, executor) ->
                executor.execute { callback.onCameraAvailable(cameraId) }
            }
        }
    }

    /** Manually triggers the onCameraUnavailable callback for all registered listeners. */
    fun triggerOnCameraUnavailable(cameraId: String) {
        Log.d("ShadowAgent", "Triggering onCameraUnavailable for $cameraId")
        synchronized(lock) {
            availabilityCallbacks.forEach { (callback, handler) ->
                handler.post { callback.onCameraUnavailable(cameraId) }
            }
            availabilityCallbackExecutors.forEach { (callback, executor) ->
                executor.execute { callback.onCameraUnavailable(cameraId) }
            }
        }
    }

    /** Fires an error or disconnect callback on a specific open camera device. */
    fun notifyOpenDeviceError(cameraId: String, scenario: DeviceErrorScenario) {
        val openInfo =
            synchronized(lock) {
                val devices = cameraIdToDevices[cameraId]
                val device = devices?.firstOrNull() ?: return@synchronized null

                // Remove from primary map and cleanup index
                val info = openCameras.remove(device)
                devices.remove(device)
                if (devices.isEmpty()) {
                    cameraIdToDevices.remove(cameraId)
                }
                info
            } ?: return

        when (scenario) {
            DeviceErrorScenario.ON_ERROR -> {
                Log.d("ShadowAgent", "Simulating onError for $cameraId.")
                openInfo.executor.execute {
                    openInfo.callback.onError(
                        openInfo.device,
                        CameraDevice.StateCallback.ERROR_CAMERA_DEVICE,
                    )
                }
            }
            DeviceErrorScenario.ON_DISCONNECTED -> {
                Log.d("ShadowAgent", "Simulating onDisconnected for $cameraId.")
                openInfo.executor.execute { openInfo.callback.onDisconnected(openInfo.device) }
            }
        }
    }

    /** Closes all open devices and clears the internal tracking state. */
    fun closeAllOpenDevices() {
        val devicesToClose =
            synchronized(lock) {
                val snapshot = openCameras.keys.toList()
                openCameras.clear()
                cameraIdToDevices.clear()
                snapshot
            }
        devicesToClose.forEach {
            try {
                it.close()
            } catch (e: Exception) {
                Log.e("ShadowAgent", "Failed to close device ${it.id}", e)
            }
        }
    }

    /** Configures the agent to fail the next attempt to open a camera. */
    fun failNextOpenCameraWith(error: DeviceOpenError?) {
        nextOpenError = error
    }

    /**
     * Checks if an open-camera error has been queued. If so, it consumes the error and returns it.
     * This is called by the shadow to inject the failure.
     */
    fun consumeNextOpenError(): DeviceOpenError? {
        val error = nextOpenError
        nextOpenError = null
        return error
    }

    /** Allows a test to wait until a CameraManager.AvailabilityCallback has been registered. */
    fun awaitListenerRegistration(timeout: Long, unit: TimeUnit): Boolean {
        return listenerRegisteredLatch.await(timeout, unit)
    }

    fun registerAvailabilityCallback(
        callback: CameraManager.AvailabilityCallback,
        handler: Handler,
    ) {
        synchronized(lock) {
            availabilityCallbacks[callback] = handler
            listenerRegisteredLatch.countDown()
        }
    }

    fun registerAvailabilityCallback(
        executor: Executor,
        callback: CameraManager.AvailabilityCallback,
    ) {
        synchronized(lock) {
            availabilityCallbackExecutors[callback] = executor
            listenerRegisteredLatch.countDown()
        }
    }

    fun unregisterAvailabilityCallback(callback: CameraManager.AvailabilityCallback) {
        synchronized(lock) {
            availabilityCallbacks.remove(callback)
            availabilityCallbackExecutors.remove(callback)
        }
    }

    fun registerOpenDevice(
        device: CameraDevice,
        callback: CameraDevice.StateCallback,
        executor: Executor,
    ) {
        synchronized(lock) {
            openCameras[device] = OpenCameraInfo(device, callback, executor)
            cameraIdToDevices.getOrPut(device.id) { mutableSetOf() }.add(device)
        }
    }

    fun unregisterDevice(device: CameraDevice) {
        synchronized(lock) {
            openCameras.remove(device)
            val devices = cameraIdToDevices[device.id]
            devices?.remove(device)
            if (devices.isNullOrEmpty()) {
                cameraIdToDevices.remove(device.id)
            }
        }
    }

    fun hijackSessionAndFireCallback(
        session: CameraCaptureSession,
        callback: CameraCaptureSession.StateCallback,
        executor: Executor,
        wasSuccessful: Boolean,
    ) {
        Log.d("ShadowAgent", "AGENT: Hijacked session. Firing success: $wasSuccessful.")
        if (wasSuccessful) {
            executor.execute { callback.onConfigured(session) }
        } else {
            executor.execute { callback.onConfigureFailed(session) }
        }
    }
}
