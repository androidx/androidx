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

@file:Suppress("EXPERIMENTAL_API_USAGE")
@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraDevice
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.DurationNs
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.core.Token
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal sealed class CameraState
internal object CameraStateUnopened : CameraState()
internal data class CameraStateOpen(val cameraDevice: CameraDeviceWrapper) : CameraState()
internal object CameraStateClosing : CameraState()
internal data class CameraStateClosed(
    val cameraId: CameraId,

    // Record the reason that the camera was closed.
    val cameraClosedReason: ClosedReason,

    // Record the number of retry attempts, if the camera took multiple attempts to open.
    val cameraRetryCount: Int? = null,

    // Record the number of nanoseconds it took to open the camera, including retry attempts.
    val cameraRetryDurationNs: DurationNs? = null,

    // Record the exception that was thrown while trying to open the camera
    val cameraException: Throwable? = null,

    // Record the number of nanoseconds it took for the final open attempt.
    val cameraOpenDurationNs: DurationNs? = null,

    // Record the duration the camera device was active. If onOpened is never called, this value
    // will never be set.
    val cameraActiveDurationNs: DurationNs? = null,

    // Record the duration the camera device took to invoke close() on the CameraDevice object.
    val cameraClosingDurationNs: DurationNs? = null,

    // Record the Camera2 ErrorCode, if the camera closed due to an error.
    val cameraErrorCode: Int? = null
) : CameraState()

internal enum class ClosedReason {
    APP_CLOSED,
    APP_DISCONNECTED,

    CAMERA2_CLOSED,
    CAMERA2_DISCONNECTED,
    CAMERA2_ERROR,
    CAMERA2_EXCEPTION
}

/**
 * A [VirtualCamera] reflects and replays the state of a "Real" [CameraDevice.StateCallback].
 *
 * This behavior allows a virtual camera to be attached a [CameraDevice.StateCallback] and to
 * replay the open sequence. This behavior a camera manager to run multiple open attempts and to
 * recover from various classes of errors that will be invisible to the [VirtualCamera] by
 * allowing the [VirtualCamera] to be attached to the real camera after the camera is opened
 * successfully (Which may involve multiple calls to open).
 *
 * Disconnecting the VirtualCamera will cause an artificial close events to be generated on the
 * state property, but may not cause the underlying [CameraDevice] to be closed.
 */
internal interface VirtualCamera {
    val state: Flow<CameraState>
    fun disconnect()
}

internal val virtualCameraDebugIds = atomic(0)

internal class VirtualCameraState(
    val cameraId: CameraId
) : VirtualCamera {
    private val debugId = virtualCameraDebugIds.incrementAndGet()
    private val lock = Any()

    @GuardedBy("lock")
    private var closed = false

    private val _state = MutableStateFlow<CameraState>(CameraStateUnopened)
    override val state: StateFlow<CameraState>
        get() = _state

    private var job: Job? = null
    private var token: Token? = null

    internal suspend fun connect(state: Flow<CameraState>, wakelockToken: Token?) = coroutineScope {
        synchronized(lock) {
            if (closed) {
                wakelockToken?.release()
                return@coroutineScope
            }

            job = launch {
                state.collect { _state.value = it }
            }
            token = wakelockToken
        }
    }

    override fun disconnect() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true

            Log.info { "Disconnecting $this" }

            job?.cancel()
            token?.release()

            // Emulate a CameraClosing -> CameraClosed sequence.
            if (_state.value !is CameraStateClosed) {
                if (_state.value !is CameraStateClosing) {
                    _state.value = CameraStateClosing
                }
                @SuppressWarnings("SyntheticAccessor")
                _state.value = CameraStateClosed(
                    cameraId,
                    cameraClosedReason = ClosedReason.APP_DISCONNECTED
                )
            }
        }
    }

    override fun toString(): String = "VirtualCamera-$debugId"
}

internal val androidCameraDebugIds = atomic(0)

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class AndroidCameraState(
    val cameraId: CameraId,
    val metadata: CameraMetadata,
    private val attemptNumber: Int,
    private val attemptTimestampNanos: TimestampNs
) : CameraDevice.StateCallback() {
    private val debugId = androidCameraDebugIds.incrementAndGet()
    private val lock = Any()

    @GuardedBy("lock")
    private var opening = false

    @GuardedBy("lock")
    private var pendingClose: ClosingInfo? = null

    private val requestTimestampNanos: TimestampNs
    private var openTimestampNanos: TimestampNs? = null

    private val _state = MutableStateFlow<CameraState>(CameraStateUnopened)
    val state: StateFlow<CameraState>
        get() = _state

    init {
        Log.info { "Opening $cameraId" }
        requestTimestampNanos =
            if (attemptNumber == 1) {
                attemptTimestampNanos
            } else {
                Timestamps.now()
            }
    }

    fun close() {
        val current = _state.value
        val device = if (current is CameraStateOpen) {
            current.cameraDevice
        } else {
            null
        }

        closeWith(
            device?.unwrap(),
            @Suppress("SyntheticAccessor")
            ClosingInfo(
                ClosedReason.APP_CLOSED
            )
        )
    }

    suspend fun awaitClosed() {
        state.first { it is CameraStateClosed }
    }

    override fun onOpened(cameraDevice: CameraDevice) {
        check(cameraDevice.id == cameraId.value)
        val openedTimestamp = Timestamps.now()
        openTimestampNanos = openedTimestamp

        Debug.traceStart { "Camera-${cameraId.value}#onOpened" }
        Log.info {
            val attemptDuration = openedTimestamp - requestTimestampNanos
            val totalDuration = openedTimestamp - attemptTimestampNanos
            if (attemptNumber == 1) {
                "Opened $cameraId in ${attemptDuration.formatMs()}"
            } else {
                "Opened $cameraId in ${attemptDuration.formatMs()} " +
                    "(${totalDuration.formatMs()} total) after $attemptNumber attempts."
            }
        }

        // This checks to see if close() has been invoked, or one of the close methods have been
        // invoked. If so, call close() on the cameraDevice outside of the synchronized block.
        var closeCamera = false
        synchronized(lock) {
            if (pendingClose != null) {
                closeCamera = true
            } else {
                opening = true
            }
        }
        if (closeCamera) {
            cameraDevice.close()
            return
        }

        // Update _state.value _without_ holding the lock. This may block the calling thread for a
        // while if it synchronously calls createCaptureSession.
        _state.value = CameraStateOpen(
            AndroidCameraDevice(
                metadata,
                cameraDevice,
                cameraId
            )
        )

        // Check to see if we received close() or other events in the meantime.
        val closeInfo = synchronized(lock) {
            opening = false
            pendingClose
        }
        if (closeInfo != null) {
            _state.value = CameraStateClosing
            cameraDevice.closeWithTrace()
            _state.value = computeClosedState(closeInfo)
        }
        Debug.traceStop()
    }

    override fun onDisconnected(cameraDevice: CameraDevice) {
        check(cameraDevice.id == cameraId.value)
        Debug.traceStart { "Camera-${cameraId.value}#onDisconnected" }
        Log.debug { "$cameraId: onDisconnected" }

        closeWith(
            cameraDevice,
            @Suppress("SyntheticAccessor")
            ClosingInfo(
                ClosedReason.CAMERA2_DISCONNECTED
            )
        )
        Debug.traceStop()
    }

    override fun onError(cameraDevice: CameraDevice, errorCode: Int) {
        check(cameraDevice.id == cameraId.value)
        Debug.traceStart { "Camera-${cameraId.value}#onError-$errorCode" }
        Log.debug { "$cameraId: onError $errorCode" }

        closeWith(
            cameraDevice,
            @Suppress("SyntheticAccessor")
            ClosingInfo(
                ClosedReason.CAMERA2_ERROR,
                errorCode = errorCode
            )
        )
        Debug.traceStop()
    }

    override fun onClosed(cameraDevice: CameraDevice) {
        check(cameraDevice.id == cameraId.value)
        Debug.traceStart { "Camera-${cameraId.value}#onClosed" }
        Log.debug { "$cameraId: onClosed" }

        closeWith(
            cameraDevice,
            @Suppress("SyntheticAccessor")
            ClosingInfo(
                ClosedReason.CAMERA2_CLOSED
            )
        )
        Debug.traceStop()
    }

    internal fun closeWith(throwable: Throwable) {

        closeWith(
            null,
            @Suppress("SyntheticAccessor")
            ClosingInfo(
                ClosedReason.CAMERA2_EXCEPTION,
                exception = throwable
            )
        )
    }

    private fun closeWith(cameraDevice: CameraDevice?, closeRequest: ClosingInfo) {
        val closeInfo = synchronized(lock) {
            if (pendingClose == null) {
                pendingClose = closeRequest
                if (!opening) {
                    return@synchronized closeRequest
                }
            }
            null
        }
        if (closeInfo != null) {
            _state.value = CameraStateClosing
            cameraDevice.closeWithTrace()
            _state.value = computeClosedState(closeInfo)
        }
    }

    private fun computeClosedState(
        closingInfo: ClosingInfo
    ): CameraStateClosed {
        val now = Timestamps.now()
        val openedTimestamp = openTimestampNanos
        val closingTimestamp = closingInfo.closingTimestamp
        val retryDuration = openedTimestamp?.let { it - attemptTimestampNanos }
        val openDuration = openedTimestamp?.let { it - requestTimestampNanos }

        // opened -> closing (or now)
        val activeDuration = when {
            openedTimestamp == null -> null
            else -> closingTimestamp - openedTimestamp
        }

        val closeDuration = closingTimestamp.let { now - it }

        @Suppress("SyntheticAccessor")
        return CameraStateClosed(
            cameraId,
            cameraClosedReason = closingInfo.reason,
            cameraRetryCount = attemptNumber - 1,
            cameraRetryDurationNs = retryDuration,
            cameraOpenDurationNs = openDuration,
            cameraActiveDurationNs = activeDuration,
            cameraClosingDurationNs = closeDuration,
            cameraErrorCode = closingInfo.errorCode,
            cameraException = closingInfo.exception
        )
    }

    private data class ClosingInfo(
        val reason: ClosedReason,
        val closingTimestamp: TimestampNs = Timestamps.now(),
        val errorCode: Int? = null,
        val exception: Throwable? = null
    )

    override fun toString(): String = "CameraState-$debugId"
}
