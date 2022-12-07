/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.hardware.camera2.CameraDevice.StateCallback
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.DurationNs
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
private val cameraRetryTimeout = DurationNs(10_000_000_000) // 10 seconds

internal interface CameraOpener {
    fun openCamera(cameraId: CameraId, stateCallback: StateCallback)
}

internal interface CameraAvailabilityMonitor {
    suspend fun awaitAvailableCamera(cameraId: CameraId, timeoutMillis: Long): Boolean
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class Camera2CameraOpener @Inject constructor(
    private val cameraManager: Provider<CameraManager>,
    private val threads: Threads
) : CameraOpener {

    @SuppressLint(
        "MissingPermission", // Permissions are checked by calling methods.
    )
    override fun openCamera(cameraId: CameraId, stateCallback: StateCallback) {
        val instance = cameraManager.get()
        Debug.trace("CameraDevice-${cameraId.value}#openCamera") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Api28Compat.openCamera(
                    instance,
                    cameraId.value,
                    threads.camera2Executor,
                    stateCallback
                )
            } else {
                instance.openCamera(
                    cameraId.value,
                    stateCallback,
                    threads.camera2Handler
                )
            }
        }
    }
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class Camera2CameraAvailabilityMonitor @Inject constructor(
    private val cameraManager: Provider<CameraManager>,
    private val threads: Threads
) : CameraAvailabilityMonitor {
    override suspend fun awaitAvailableCamera(cameraId: CameraId, timeoutMillis: Long): Boolean =
        withTimeoutOrNull(timeoutMillis) {
            awaitAvailableCamera(cameraId)
        } ?: false

    private suspend fun awaitAvailableCamera(cameraId: CameraId) =
        suspendCancellableCoroutine { continuation ->
            val availabilityCallback = object : CameraManager.AvailabilityCallback() {
                override fun onCameraAvailable(cameraIdString: String) {
                    if (cameraIdString == cameraId.value) {
                        Log.debug { "$cameraId is now available." }
                        continuation.resume(true)
                    }
                }

                override fun onCameraAccessPrioritiesChanged() {
                    Log.debug { "Access priorities changed." }
                    continuation.resume(true)
                }
            }

            val manager = cameraManager.get()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Api28Compat.registerAvailabilityCallback(
                    manager,
                    threads.camera2Executor,
                    availabilityCallback
                )
            } else {
                manager.registerAvailabilityCallback(
                    availabilityCallback,
                    threads.camera2Handler
                )
            }

            continuation.invokeOnCancellation {
                manager.unregisterAvailabilityCallback(availabilityCallback)
            }
        }
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class CameraStateOpener @Inject constructor(
    private val cameraOpener: CameraOpener,
    private val cameraMetadataProvider: CameraMetadataProvider,
    private val timeSource: TimeSource
) {
    internal suspend fun tryOpenCamera(
        cameraId: CameraId,
        attempts: Int,
        requestTimestamp: TimestampNs,
    ): CameraOpenResult {
        val metadata = cameraMetadataProvider.getMetadata(cameraId)
        val cameraState = AndroidCameraState(
            cameraId,
            metadata,
            attempts,
            requestTimestamp,
            timeSource
        )

        try {
            cameraOpener.openCamera(cameraId, cameraState)

            // Suspend until we are no longer in a "starting" state.
            val result = cameraState.state.first {
                it !is CameraStateUnopened
            }
            when (result) {
                is CameraStateOpen ->
                    return CameraOpenResult(cameraState = cameraState)

                is CameraStateClosing -> {
                    cameraState.close()
                    return CameraOpenResult(errorCode = result.cameraErrorCode)
                }

                is CameraStateClosed -> {
                    cameraState.close()
                    return CameraOpenResult(errorCode = result.cameraErrorCode)
                }

                is CameraStateUnopened -> {
                    cameraState.close()
                    throw IllegalStateException("Unexpected CameraState: $result")
                }
            }
        } catch (exception: Throwable) {
            Log.warn(exception) { "Failed to open $cameraId" }
            cameraState.closeWith(exception)
            return CameraOpenResult(errorCode = CameraError.from(exception))
        }
    }

    internal data class CameraOpenResult(
        val cameraState: AndroidCameraState? = null,
        val errorCode: CameraError? = null,
    )
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class RetryingCameraStateOpener @Inject constructor(
    private val cameraStateOpener: CameraStateOpener,
    private val cameraAvailabilityMonitor: CameraAvailabilityMonitor,
    private val timeSource: TimeSource
) {
    internal suspend fun openCameraWithRetry(
        cameraId: CameraId
    ): AndroidCameraState? {
        val requestTimestamp = Timestamps.now(timeSource)
        var attempts = 0

        while (true) {
            attempts++

            val result = cameraStateOpener.tryOpenCamera(cameraId, attempts, requestTimestamp)
            with(result) {
                if (cameraState != null) {
                    return cameraState
                }

                if (errorCode == null) {
                    // Camera open failed without an error. This can only happen if the
                    // VirtualCameraState is disconnected by the app itself. As such, we should just
                    // abandon the camera open attempt.
                    Log.warn {
                        "Camera open failed without an error. " +
                            "The CameraGraph may have been stopped or closed. " +
                            "Abandoning the camera open attempt."
                    }
                    return null
                }

                if (!shouldRetry(errorCode, attempts, requestTimestamp)) {
                    Log.error {
                        "Failed to open camera $cameraId after $attempts attempts " +
                            "and ${(Timestamps.now(timeSource) - requestTimestamp).formatMs()}. " +
                            "Last error was $errorCode."
                    }
                    return null
                }
            }

            // Listen to availability - if we are notified that the cameraId is available then
            // retry immediately.
            if (!cameraAvailabilityMonitor.awaitAvailableCamera(cameraId, timeoutMillis = 500)) {
                Log.debug { "Timeout expired, retrying camera open for camera $cameraId" }
            }
        }
    }

    companion object {
        internal fun shouldRetry(
            errorCode: CameraError,
            attempts: Int,
            firstAttemptTimestampNs: TimestampNs,
            timeSource: TimeSource = SystemTimeSource()
        ): Boolean {
            val elapsed = Timestamps.now(timeSource) - firstAttemptTimestampNs
            if (elapsed > cameraRetryTimeout) {
                return false
            }
            return when (errorCode) {
                CameraError.ERROR_CAMERA_IN_USE -> attempts <= 1
                CameraError.ERROR_CAMERA_LIMIT_EXCEEDED -> true
                CameraError.ERROR_CAMERA_DISABLED -> attempts <= 1
                CameraError.ERROR_CAMERA_DEVICE -> true
                CameraError.ERROR_CAMERA_SERVICE -> true
                CameraError.ERROR_CAMERA_DISCONNECTED -> true
                CameraError.ERROR_ILLEGAL_ARGUMENT_EXCEPTION -> true
                CameraError.ERROR_SECURITY_EXCEPTION -> attempts <= 1
                else -> {
                    Log.error { "Unexpected CameraError: $this" }
                    false
                }
            }
        }
    }
}