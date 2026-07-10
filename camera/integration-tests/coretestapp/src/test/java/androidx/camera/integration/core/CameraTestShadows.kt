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

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.StateCallback
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowCameraDeviceImpl
import org.robolectric.shadows.ShadowCameraManager

const val TAG = "ShadowWrapper"

/** Static bridge to link the test's agent instance to the Robolectric shadows. */
object ShadowCameraBridge {
    @JvmStatic var agent: ShadowCameraAgent? = null
}

private fun getAgent(): ShadowCameraAgent =
    ShadowCameraBridge.agent
        ?: throw IllegalStateException("ShadowCameraAgent not initialized in test setup")

/**
 * This shadow uses a "Wrapper Callback" pattern. It lets the base ShadowCameraManager create the
 * device and notifies the Agent of lifecycle events.
 */
@Implements(CameraManager::class)
class TestShadowCameraManager : ShadowCameraManager() {

    /**
     * This wrapper intercepts callbacks from the base shadow, notifies our agent, and then passes
     * the call to the real CameraX callback.
     */
    private class WrapperStateCallback(
        val realCameraXCallback: CameraDevice.StateCallback,
        val realCameraXExecutor: Executor,
        val agent: ShadowCameraAgent,
    ) : CameraDevice.StateCallback() {

        override fun onOpened(realShadowDevice: CameraDevice) {
            Log.d(TAG, "WRAPPER: onOpened. Registering device with agent.")
            agent.registerOpenDevice(realShadowDevice, realCameraXCallback, realCameraXExecutor)
            agent.triggerOnCameraUnavailable(realShadowDevice.id)
            realCameraXExecutor.execute { realCameraXCallback.onOpened(realShadowDevice) }
        }

        override fun onClosed(realShadowDevice: CameraDevice) {
            Log.d(TAG, "WRAPPER: onClosed. Unregistering device.")
            agent.unregisterDevice(realShadowDevice)
            agent.triggerOnCameraAvailable(realShadowDevice.id)
            realCameraXExecutor.execute { realCameraXCallback.onClosed(realShadowDevice) }
        }

        override fun onDisconnected(realShadowDevice: CameraDevice) {
            Log.d(TAG, "WRAPPER: onDisconnected.")
            realCameraXExecutor.execute { realCameraXCallback.onDisconnected(realShadowDevice) }
        }

        override fun onError(realShadowDevice: CameraDevice, error: Int) {
            Log.d(TAG, "WRAPPER: onError.")
            realCameraXExecutor.execute { realCameraXCallback.onError(realShadowDevice, error) }
        }
    }

    /**
     * Checks if an error should be injected. If so, it creates a mock device, fires the onError
     * callback, and returns the mock.
     *
     * @return A mock [CameraDevice] if an error was injected, or `null` to proceed normally.
     */
    private fun handlePendingError(
        cameraId: String,
        realCallback: CameraDevice.StateCallback,
        realExecutor: Executor,
    ): CameraDevice? {
        val agent = getAgent()

        // Check if the test has queued up an error to be injected.
        val pendingError = agent.consumeNextOpenError()
        if (pendingError != null) {
            Log.d(TAG, "Injecting open-camera error code: ${pendingError.errorCode}")

            // As requested, create a MOCK CameraDevice instead of using internal methods.
            val mockDevice = Mockito.mock(CameraDevice::class.java)
            Mockito.`when`(mockDevice.id).thenReturn(cameraId)

            // Define the behavior for the close() method on the mock.
            // When close() is called, it should trigger the onClosed callback.
            Mockito.doAnswer { _: InvocationOnMock ->
                    Log.d(TAG, "Mock CameraDevice closed. Firing onClosed callback.")
                    realExecutor.execute { realCallback.onClosed(mockDevice) }
                    null // close() returns void.
                }
                .`when`(mockDevice)
                .close()

            // Fire the onError callback directly without opening a real device.
            realExecutor.execute { realCallback.onError(mockDevice, pendingError.errorCode) }
            return mockDevice
        }

        // No error to inject, return null to proceed.
        return null
    }

    @Implementation(minSdk = Build.VERSION_CODES.S)
    override fun openCameraDeviceUserAsync(
        cameraId: String,
        callback: CameraDevice.StateCallback,
        executor: Executor,
        uid: Int,
        oomScoreOffset: Int,
    ): CameraDevice {
        // Return mock device if an error is injected
        handlePendingError(cameraId, callback, executor)?.let {
            return it
        }

        // No error, proceed with the normal open flow.
        val wrapperCallback = WrapperStateCallback(callback, executor, getAgent())

        // Call SUPER with the correct signature for this SDK level.
        return super.openCameraDeviceUserAsync(
            cameraId,
            wrapperCallback, // Pass our wrapper
            executor, // Pass the real executor
            uid,
            oomScoreOffset,
        )
    }

    @Implementation(minSdk = Build.VERSION_CODES.P, maxSdk = Build.VERSION_CODES.R)
    override fun openCameraDeviceUserAsync(
        cameraId: String,
        callback: CameraDevice.StateCallback,
        executor: Executor,
        uid: Int,
    ): CameraDevice {
        // Return mock device if an error is injected
        handlePendingError(cameraId, callback, executor)?.let {
            return it
        }

        // No error, proceed with the normal open flow.
        val wrapperCallback = WrapperStateCallback(callback, executor, getAgent())

        // Call SUPER with the correct signature for this SDK level.
        return super.openCameraDeviceUserAsync(
            cameraId,
            wrapperCallback, // Pass our wrapper
            executor, // Pass the real executor
            uid,
        )
    }

    @Implementation(minSdk = Build.VERSION_CODES.N_MR1, maxSdk = Build.VERSION_CODES.O_MR1)
    override fun openCameraDeviceUserAsync(
        cameraId: String,
        callback: CameraDevice.StateCallback,
        handler: Handler,
        uid: Int,
    ): CameraDevice {
        val executor = Executor { handler.post(it) }

        // Return mock device if an error is injected
        handlePendingError(cameraId, callback, executor)?.let {
            return it
        }

        // No error, proceed with the normal open flow.
        val wrapperCallback = WrapperStateCallback(callback, executor, getAgent())

        // Call SUPER with the correct signature for this SDK level.
        return super.openCameraDeviceUserAsync(
            cameraId,
            wrapperCallback, // Pass our wrapper
            Handler(Looper.getMainLooper()), // Pass the safe main looper handler
            uid,
        )
    }

    @Implementation(maxSdk = Build.VERSION_CODES.N)
    override fun openCameraDeviceUserAsync(
        cameraId: String,
        callback: CameraDevice.StateCallback,
        handler: Handler,
    ): CameraDevice {
        val executor = Executor { handler.post(it) }

        // Return mock device if an error is injected
        handlePendingError(cameraId, callback, executor)?.let {
            return it
        }

        // No error, proceed with the normal open flow.
        val wrapperCallback = WrapperStateCallback(callback, executor, getAgent())

        // Call SUPER with the correct signature for this SDK level.
        return super.openCameraDeviceUserAsync(
            cameraId,
            wrapperCallback, // Pass our wrapper
            Handler(Looper.getMainLooper()), // Pass the safe main looper handler
        )
    }

    @Implementation
    override fun registerAvailabilityCallback(
        callback: CameraManager.AvailabilityCallback,
        handler: Handler?,
    ) {
        // DO NOT call super.registerAvailabilityCallback.
        // This prevents the base shadow from automatically firing callbacks.
        getAgent().registerAvailabilityCallback(callback, handler ?: getAgent().testHandler)
    }

    @Implementation
    fun registerAvailabilityCallback(
        executor: Executor,
        callback: CameraManager.AvailabilityCallback,
    ) {
        getAgent().registerAvailabilityCallback(executor, callback)
    }

    @Implementation
    override fun unregisterAvailabilityCallback(callback: CameraManager.AvailabilityCallback) {
        getAgent().unregisterAvailabilityCallback(callback)
    }
}

/** This shadow forwards all session creation to the agent. */
@Implements(className = "android.hardware.camera2.impl.CameraDeviceImpl", isInAndroidSdk = false)
class TestShadowCameraDeviceImpl : ShadowCameraDeviceImpl() {

    private fun createSessionWithAgent(
        outputs: List<Surface>,
        callback: StateCallback,
        executor: Executor,
        handler: Handler,
    ) {
        checkIfCameraClosedOrInError()
        super.createCaptureSession(
            outputs,
            object : StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    getAgent()
                        .hijackSessionAndFireCallback(
                            session,
                            callback,
                            executor,
                            wasSuccessful = true,
                        )
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    getAgent()
                        .hijackSessionAndFireCallback(
                            session,
                            callback,
                            executor,
                            wasSuccessful = false,
                        )
                }
            },
            handler,
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Implementation(minSdk = Build.VERSION_CODES.P)
    @Throws(CameraAccessException::class)
    override fun createCaptureSession(config: SessionConfiguration) {
        createSessionWithAgent(
            outputs = config.outputConfigurations.flatMap { it.surfaces }.toList(),
            callback = config.stateCallback,
            executor = config.executor,
            handler = getAgent().testHandler,
        )
    }

    @Implementation(maxSdk = Build.VERSION_CODES.O_MR1)
    @Throws(CameraAccessException::class)
    override fun createCaptureSession(
        outputs: List<Surface>,
        callback: StateCallback,
        handler: Handler?,
    ) {
        val executor = Executor { r ->
            if (handler != null) {
                handler.post(r)
            } else {
                r.run()
            }
        }
        createSessionWithAgent(
            outputs = outputs,
            callback = callback,
            executor = executor,
            handler = getAgent().testHandler,
        )
    }
}
