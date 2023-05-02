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

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.DurationNs
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.core.Token
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal sealed class CameraState

internal object CameraStateUnopened : CameraState()

internal data class CameraStateOpen(val cameraDevice: CameraDeviceWrapper) : CameraState()

internal data class CameraStateClosing(val cameraErrorCode: CameraError? = null) : CameraState()

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

    // Record the camera ErrorCode, if the camera closed due to an error.
    val cameraErrorCode: CameraError? = null
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
 * This behavior allows a virtual camera to be attached a [CameraDevice.StateCallback] and to replay
 * the open sequence. This behavior a camera manager to run multiple open attempts and to recover
 * from various classes of errors that will be invisible to the [VirtualCamera] by allowing the
 * [VirtualCamera] to be attached to the real camera after the camera is opened successfully (Which
 * may involve multiple calls to open).
 *
 * Disconnecting the VirtualCamera will cause an artificial close events to be generated on the
 * state property, but may not cause the underlying [CameraDevice] to be closed.
 */
internal interface VirtualCamera {
    val state: Flow<CameraState>
    val value: CameraState
    fun disconnect(lastCameraError: CameraError? = null)
}

internal val virtualCameraDebugIds = atomic(0)

internal class VirtualCameraState(
    val cameraId: CameraId,
    val graphListener: GraphListener
) : VirtualCamera {
    private val debugId = virtualCameraDebugIds.incrementAndGet()
    private val lock = Any()

    @GuardedBy("lock")
    private var closed = false

    @GuardedBy("lock")
    private var currentVirtualAndroidCamera: VirtualAndroidCameraDevice? = null

    // This is intended so that it will only ever replay the most recent event to new subscribers,
    // but to never drop events for existing subscribers.
    private val _stateFlow = MutableSharedFlow<CameraState>(replay = 1, extraBufferCapacity = 3)
    private val _states = _stateFlow.distinctUntilChanged()

    @GuardedBy("lock")
    private var _lastState: CameraState = CameraStateUnopened

    override val state: Flow<CameraState>
        get() = _states

    override val value: CameraState
        get() = synchronized(lock) { _lastState }

    private var job: Job? = null
    private var wakelockToken: Token? = null

    init {
        // Emit the initial unopened state.
        check(_stateFlow.tryEmit(_lastState))
    }

    internal suspend fun connect(state: Flow<CameraState>, wakelockToken: Token?) = coroutineScope {
        synchronized(lock) {
            if (closed) {
                wakelockToken?.release()
                return@coroutineScope
            }

            // Here we generally relay what we receive from AndroidCameraState's state flow, except
            // for CameraStateOpen. When the AndroidCameraDevice is provided through
            // CameraStateOpen, we create a wrapper (VirtualAndroidCameraDevice) around it,
            // allowing the AndroidCameraDevice to be "disconnected". This prevents additional calls
            // such as createCaptureSession() from being executed on the camera device.
            //
            // Why it's needed: When 2 CameraGraphs are created and started in quick succession, say
            // we have CameraGraph-1 and CameraGraph-2, it is possible for CameraGraph-2 to create
            // its capture session _earlier_ than CameraGraph-1, as they run on separate threads.
            // Because the two createCaptureSession() calls happen out of order, the more recent
            // call wins, causing the session for CameraGraph-1 to succeed (even when it's already
            // closed) and the session for CameraGraph-2 to fail (even though it was started most
            // recently).
            //
            // Relevant bug: b/269619541
            job = launch {
                state.collect {
                    synchronized(lock) {
                        if (closed) {
                            this.cancel()
                        }
                        if (it is CameraStateOpen) {
                            val virtualAndroidCamera = VirtualAndroidCameraDevice(
                                it.cameraDevice as AndroidCameraDevice
                            )
                            // The ordering here is important. We need to set the current
                            // VirtualAndroidCameraDevice before emitting it out. Otherwise, the
                            // capture session can be started while we still don't have the current
                            // VirtualAndroidCameraDevice to disconnect when
                            // VirtualCameraState.disconnect() is called in parallel.
                            currentVirtualAndroidCamera = virtualAndroidCamera
                            emitState(CameraStateOpen(virtualAndroidCamera))
                        } else {
                            emitState(it)
                        }
                    }
                }
            }
            this@VirtualCameraState.wakelockToken = wakelockToken
        }
    }

    override fun disconnect(lastCameraError: CameraError?) {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true

            Log.info { "Disconnecting $this" }

            currentVirtualAndroidCamera?.disconnect()
            job?.cancel()
            wakelockToken?.release()

            // Emulate a CameraClosing -> CameraClosed sequence.
            if (value !is CameraStateClosed) {
                if (_lastState !is CameraStateClosing) {
                    emitState(CameraStateClosing())
                }
                emitState(
                    CameraStateClosed(
                        cameraId,
                        cameraClosedReason = ClosedReason.APP_DISCONNECTED,
                        cameraErrorCode = lastCameraError
                    )
                )
            }
        }
    }

    @GuardedBy("lock")
    private fun emitState(state: CameraState) {
        _lastState = state
        check(_stateFlow.tryEmit(state)) { "Failed to emit $state in ${this@VirtualCameraState}" }
    }

    override fun toString(): String = "VirtualCamera-$debugId"
}

internal val androidCameraDebugIds = atomic(0)

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class AndroidCameraState(
    val cameraId: CameraId,
    val metadata: CameraMetadata,
    private val attemptNumber: Int,
    private val attemptTimestampNanos: TimestampNs,
    private val timeSource: TimeSource,
    private val cameraErrorListener: CameraErrorListener,
    private val camera2DeviceCloser: Camera2DeviceCloser,
    private val interopDeviceStateCallback: CameraDevice.StateCallback? = null,
    private val interopSessionStateCallback: CameraCaptureSession.StateCallback? = null
) : CameraDevice.StateCallback() {
    private val debugId = androidCameraDebugIds.incrementAndGet()
    private val lock = Any()

    @GuardedBy("lock")
    private var opening = false

    @GuardedBy("lock")
    private var pendingClose: ClosingInfo? = null

    private val cameraDeviceClosed = CountDownLatch(1)

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
                Timestamps.now(timeSource)
            }
    }

    fun close() {
        val current = _state.value
        val device =
            if (current is CameraStateOpen) {
                current.cameraDevice
            } else {
                null
            }

        closeWith(
            device?.unwrapAs(CameraDevice::class),
            @Suppress("SyntheticAccessor") ClosingInfo(ClosedReason.APP_CLOSED)
        )
    }

    suspend fun awaitClosed() {
        state.first { it is CameraStateClosed }
    }

    internal fun awaitCameraDeviceClosed(timeoutMillis: Long): Boolean =
        cameraDeviceClosed.await(timeoutMillis, TimeUnit.MILLISECONDS)

    override fun onOpened(cameraDevice: CameraDevice) {
        check(cameraDevice.id == cameraId.value)
        val openedTimestamp = Timestamps.now(timeSource)
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
        val currentCloseInfo = synchronized(lock) {
            if (pendingClose == null) {
                opening = true
            }
            pendingClose
        }
        interopDeviceStateCallback?.onOpened(cameraDevice)
        if (currentCloseInfo != null) {
            camera2DeviceCloser.closeCamera(
                cameraDevice = cameraDevice,
                closeUnderError = currentCloseInfo.errorCode != null,
                androidCameraState = this
            )
            return
        }

        // Update _state.value _without_ holding the lock. This may block the calling thread for a
        // while if it synchronously calls createCaptureSession.
        _state.value =
            CameraStateOpen(
                AndroidCameraDevice(
                    metadata,
                    cameraDevice,
                    cameraId,
                    cameraErrorListener,
                    interopSessionStateCallback
                )
            )

        // Check to see if we received close() or other events in the meantime.
        val closeInfo =
            synchronized(lock) {
                opening = false
                pendingClose
            }
        if (closeInfo != null) {
            _state.value = CameraStateClosing(closeInfo.errorCode)
            camera2DeviceCloser.closeCamera(
                cameraDevice = cameraDevice,
                closeUnderError = closeInfo.errorCode != null,
                androidCameraState = this
            )
            _state.value = computeClosedState(closeInfo)
        }
        Debug.traceStop()
    }

    override fun onDisconnected(cameraDevice: CameraDevice) {
        check(cameraDevice.id == cameraId.value)
        Debug.traceStart { "Camera-${cameraId.value}#onDisconnected" }
        Log.debug { "$cameraId: onDisconnected" }
        cameraDeviceClosed.countDown()

        closeWith(
            cameraDevice,
            @Suppress("SyntheticAccessor")
            ClosingInfo(
                ClosedReason.CAMERA2_DISCONNECTED,
                errorCode = CameraError.ERROR_CAMERA_DISCONNECTED
            )
        )
        interopDeviceStateCallback?.onDisconnected(cameraDevice)
        Debug.traceStop()
    }

    override fun onError(cameraDevice: CameraDevice, errorCode: Int) {
        check(cameraDevice.id == cameraId.value)
        Debug.traceStart { "Camera-${cameraId.value}#onError-$errorCode" }
        Log.debug { "$cameraId: onError $errorCode" }
        cameraDeviceClosed.countDown()

        closeWith(
            cameraDevice,
            @Suppress("SyntheticAccessor")
            ClosingInfo(ClosedReason.CAMERA2_ERROR, errorCode = CameraError.from(errorCode))
        )
        interopDeviceStateCallback?.onError(cameraDevice, errorCode)
        Debug.traceStop()
    }

    override fun onClosed(cameraDevice: CameraDevice) {
        check(cameraDevice.id == cameraId.value)
        Debug.traceStart { "Camera-${cameraId.value}#onClosed" }
        Log.debug { "$cameraId: onClosed" }
        cameraDeviceClosed.countDown()

        closeWith(
            cameraDevice, @Suppress("SyntheticAccessor") ClosingInfo(ClosedReason.CAMERA2_CLOSED)
        )
        interopDeviceStateCallback?.onClosed(cameraDevice)
        Debug.traceStop()
    }

    internal fun closeWith(throwable: Throwable) {
        val errorCode = CameraError.from(throwable)
        // This can happen with CAMERA_ERROR where it can be ERROR_CAMERA_DEVICE or
        // ERROR_CAMERA_SERVICE. We leave that till onError() tells us the actual error.
        if (errorCode == CameraError.ERROR_UNDETERMINED) {
            return
        }
        closeWith(throwable, errorCode)
    }

    private fun closeWith(throwable: Throwable, cameraError: CameraError) {
        closeWith(
            null,
            @Suppress("SyntheticAccessor")
            ClosingInfo(
                ClosedReason.CAMERA2_EXCEPTION, errorCode = cameraError, exception = throwable
            )
        )
    }

    private fun closeWith(cameraDevice: CameraDevice?, closeRequest: ClosingInfo) {
        val currentState = _state.value
        val cameraDeviceWrapper =
            if (currentState is CameraStateOpen) {
                currentState.cameraDevice
            } else {
                null
            }

        val closeInfo =
            synchronized(lock) {
                if (pendingClose == null) {
                    pendingClose = closeRequest
                    if (!opening) {
                        return@synchronized closeRequest
                    }
                }
                null
            }
        if (closeInfo != null) {
            // If the camera error is an Exception during open, the error should be reported by
            // RetryingCameraStateOpener.
            if (closeInfo.errorCode != null && closeInfo.reason != ClosedReason.CAMERA2_EXCEPTION) {
                cameraErrorListener.onCameraError(
                    cameraId,
                    closeInfo.errorCode,
                    willAttemptRetry = false
                )
            }
            _state.value = CameraStateClosing(closeInfo.errorCode)

            camera2DeviceCloser.closeCamera(
                cameraDeviceWrapper,
                cameraDevice,
                closeUnderError = closeInfo.errorCode != null,
                androidCameraState = this,
            )
            _state.value = computeClosedState(closeInfo)
        }
    }

    private fun computeClosedState(closingInfo: ClosingInfo): CameraStateClosed {
        val now = Timestamps.now(timeSource)
        val openedTimestamp = openTimestampNanos
        val closingTimestamp = closingInfo.closingTimestamp
        val retryDuration = openedTimestamp?.let { it - attemptTimestampNanos }
        val openDuration = openedTimestamp?.let { it - requestTimestampNanos }

        // opened -> closing (or now)
        val activeDuration =
            when {
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
        val closingTimestamp: TimestampNs = Timestamps.now(SystemTimeSource()),
        val errorCode: CameraError? = null,
        val exception: Throwable? = null
    )

    override fun toString(): String = "CameraState-$debugId"
}
