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
import java.util.Collections
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

    private val openCameras = Collections.synchronizedMap(mutableMapOf<String, OpenCameraInfo>())
    private val availabilityCallbacks =
        Collections.synchronizedMap(LinkedHashMap<CameraManager.AvailabilityCallback, Handler>())
    private val availabilityCallbackExecutors =
        Collections.synchronizedMap(LinkedHashMap<CameraManager.AvailabilityCallback, Executor>())

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
        synchronized(availabilityCallbacks) {
            availabilityCallbacks.forEach { (callback, handler) ->
                handler.post { callback.onCameraAvailable(cameraId) }
            }
        }
        synchronized(availabilityCallbackExecutors) {
            availabilityCallbackExecutors.forEach { (callback, executor) ->
                executor.execute { callback.onCameraAvailable(cameraId) }
            }
        }
    }

    /** Manually triggers the onCameraUnavailable callback for all registered listeners. */
    fun triggerOnCameraUnavailable(cameraId: String) {
        Log.d("ShadowAgent", "Triggering onCameraUnavailable for $cameraId")
        synchronized(availabilityCallbacks) {
            availabilityCallbacks.forEach { (callback, handler) ->
                handler.post { callback.onCameraUnavailable(cameraId) }
            }
        }
        synchronized(availabilityCallbackExecutors) {
            availabilityCallbackExecutors.forEach { (callback, executor) ->
                executor.execute { callback.onCameraUnavailable(cameraId) }
            }
        }
    }

    /** Fires an error or disconnect callback on a specific open camera device. */
    fun notifyOpenDeviceError(cameraId: String, scenario: DeviceErrorScenario) {
        val openInfo = openCameras.remove(cameraId) ?: return
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
        availabilityCallbacks[callback] = handler
        listenerRegisteredLatch.countDown()
    }

    fun registerAvailabilityCallback(
        executor: Executor,
        callback: CameraManager.AvailabilityCallback,
    ) {
        availabilityCallbackExecutors[callback] = executor
        listenerRegisteredLatch.countDown()
    }

    fun unregisterAvailabilityCallback(callback: CameraManager.AvailabilityCallback) {
        availabilityCallbacks.remove(callback)
        availabilityCallbackExecutors.remove(callback)
    }

    fun registerOpenDevice(
        device: CameraDevice,
        callback: CameraDevice.StateCallback,
        executor: Executor,
    ) {
        openCameras[device.id] = OpenCameraInfo(device, callback, executor)
    }

    fun unregisterDevice(device: CameraDevice) {
        openCameras.remove(device.id)
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
