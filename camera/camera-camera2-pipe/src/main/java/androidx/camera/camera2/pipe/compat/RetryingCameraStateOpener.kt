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
import android.app.admin.DevicePolicyManager
import android.hardware.camera2.CameraDevice.StateCallback
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.DurationNs
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.graph.GraphListener
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.resume
import kotlinx.atomicfu.atomic
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

internal interface DevicePolicyManagerWrapper {
    val camerasDisabled: Boolean
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
                private val awaitComplete = atomic(false)

                override fun onCameraAvailable(cameraIdString: String) {
                    if (cameraIdString == cameraId.value) {
                        Log.debug { "$cameraId is now available." }
                        if (awaitComplete.compareAndSet(expect = false, update = true)) {
                            continuation.resume(true)
                        }
                    }
                }

                override fun onCameraAccessPrioritiesChanged() {
                    Log.debug { "Access priorities changed." }
                    if (awaitComplete.compareAndSet(expect = false, update = true)) {
                        continuation.resume(true)
                    }
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
internal class AndroidDevicePolicyManagerWrapper @Inject constructor(
    private val devicePolicyManager: DevicePolicyManager
) : DevicePolicyManagerWrapper {
    override val camerasDisabled: Boolean
        get() = Debug.trace("DevicePolicyManager#getCameraDisabled") {
            devicePolicyManager.getCameraDisabled(null)
        }
}

internal data class OpenCameraResult(
    val cameraState: AndroidCameraState? = null,
    val errorCode: CameraError? = null,
)

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class CameraStateOpener @Inject constructor(
    private val cameraOpener: CameraOpener,
    private val cameraMetadataProvider: CameraMetadataProvider,
    private val timeSource: TimeSource,
    private val cameraInteropConfig: CameraPipe.CameraInteropConfig?
) {
    internal suspend fun tryOpenCamera(
        cameraId: CameraId,
        attempts: Int,
        requestTimestamp: TimestampNs,
    ): OpenCameraResult {
        val metadata = cameraMetadataProvider.getMetadata(cameraId)
        val cameraState = AndroidCameraState(
            cameraId,
            metadata,
            attempts,
            requestTimestamp,
            timeSource,
            cameraInteropConfig?.cameraDeviceStateCallback,
            cameraInteropConfig?.cameraSessionStateCallback
        )

        try {
            cameraOpener.openCamera(cameraId, cameraState)

            // Suspend until we are no longer in a "starting" state.
            val result = cameraState.state.first {
                it !is CameraStateUnopened
            }
            when (result) {
                is CameraStateOpen ->
                    return OpenCameraResult(cameraState = cameraState)

                is CameraStateClosing -> {
                    cameraState.close()
                    return OpenCameraResult(errorCode = result.cameraErrorCode)
                }

                is CameraStateClosed -> {
                    cameraState.close()
                    return OpenCameraResult(errorCode = result.cameraErrorCode)
                }

                is CameraStateUnopened -> {
                    cameraState.close()
                    throw IllegalStateException("Unexpected CameraState: $result")
                }
            }
        } catch (exception: Throwable) {
            Log.warn(exception) { "Failed to open $cameraId" }
            cameraState.closeWith(exception)
            return OpenCameraResult(errorCode = CameraError.from(exception))
        }
    }
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class RetryingCameraStateOpener @Inject constructor(
    private val cameraStateOpener: CameraStateOpener,
    private val cameraAvailabilityMonitor: CameraAvailabilityMonitor,
    private val timeSource: TimeSource,
    private val devicePolicyManager: DevicePolicyManagerWrapper
) {
    internal suspend fun openCameraWithRetry(
        cameraId: CameraId,
        graphListener: GraphListener
    ): OpenCameraResult {
        val requestTimestamp = Timestamps.now(timeSource)
        var attempts = 0

        while (true) {
            attempts++

            val result = cameraStateOpener.tryOpenCamera(cameraId, attempts, requestTimestamp)
            with(result) {
                if (cameraState != null) {
                    return result
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
                    return result
                }

                val willRetry = shouldRetry(
                    errorCode,
                    attempts,
                    requestTimestamp,
                    timeSource,
                    devicePolicyManager.camerasDisabled
                )
                // Always notify if the decision is to not retry the camera open, otherwise allow
                // 1 open call to happen silently without generating an error, and notify about each
                // error after that point.
                if (!willRetry || attempts > 1) {
                    graphListener.onGraphError(GraphStateError(errorCode, willRetry))
                }
                if (!willRetry) {
                    Log.error {
                        "Failed to open camera $cameraId after $attempts attempts " +
                            "and ${(Timestamps.now(timeSource) - requestTimestamp).formatMs()}. " +
                            "Last error was $errorCode."
                    }
                    return result
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
            timeSource: TimeSource = SystemTimeSource(),
            camerasDisabledByDevicePolicy: Boolean
        ): Boolean {
            val elapsed = Timestamps.now(timeSource) - firstAttemptTimestampNs
            if (elapsed > cameraRetryTimeout) {
                return false
            }
            return when (errorCode) {
                CameraError.ERROR_CAMERA_IN_USE ->
                    // The error indicates that camera is in use, possibly by an app with higher
                    // priority [1].
                    //
                    // Historically, we retry once to avoid polling for camera opens. Starting with
                    // the introduction of multi-resume on Android 10 (Q) however [2], it becomes
                    // easier to switch between apps, causing the issue to show up more prominently
                    // [3]. We therefore should retry continuously on Android OS versions >= Q.
                    //
                    // [1] b/38330838 - Cannot launch camera app during video call.
                    // [2] https://source.android.com/docs/core/display/multi_display/multi-resume
                    // [3] b/181777896 - Fatal error while switching between apps using camera.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        attempts <= 1
                    } else {
                        true
                    }

                CameraError.ERROR_CAMERA_LIMIT_EXCEEDED -> true
                CameraError.ERROR_CAMERA_DISABLED ->
                    // The error indicates indicates that the current camera is currently disabled,
                    // either by a device level policy [1] or because the app isn't considered
                    // foreground (which can, under rare circumstances, happen when the app is
                    // actually in the foreground due to racey foreground status propagation in the
                    // Android Framework) [2]
                    //
                    // When cameras are disabled by policy, we retry just once in case we have a
                    // transient error, and only retry repeatedly if cameras aren't disabled by
                    // policy.
                    //
                    // [1] b/77827041 - Camera has been disabled because of security policies
                    // [2] b/250234453 - Fatal error encountered while switching camera app between
                    //                   foreground and background.
                    if (camerasDisabledByDevicePolicy) {
                        attempts <= 1
                    } else {
                        true
                    }

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