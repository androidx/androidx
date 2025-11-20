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

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraInterop
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import kotlin.reflect.KClass
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic

/**
 * Interface shim for [CameraExtensionSession] with minor modifications.
 *
 * This interface has been modified to correct nullness, adjust exceptions, and to return or produce
 * wrapper interfaces instead of the native Camera2 types.
 */
internal interface CameraExtensionSessionWrapper :
    CameraCaptureSessionWrapper, UnsafeWrapper, AutoCloseable {

    /**
     * @return The [CameraDeviceWrapper] that created this CameraExtensionSession
     * @see [CameraExtensionSession.getDevice]
     */
    override val device: CameraDeviceWrapper

    /** @see [CameraExtensionSession.stopRepeating]. */
    override fun stopRepeating(): Boolean

    /** @see CameraExtensionSession.StateCallback */
    interface StateCallback : SessionStateCallback {
        /** @see CameraExtensionSession.StateCallback.onClosed */
        fun onClosed(session: CameraExtensionSessionWrapper)

        /** @see CameraExtensionSession.StateCallback.onConfigureFailed */
        fun onConfigureFailed(session: CameraExtensionSessionWrapper)

        /** @see CameraExtensionSession.StateCallback.onConfigured */
        fun onConfigured(session: CameraExtensionSessionWrapper)
    }

    fun getRealTimeCaptureLatency(): CameraExtensionSession.StillCaptureLatency?
}

@RequiresApi(31)
internal class AndroidExtensionSessionStateCallback(
    private val device: CameraDeviceWrapper,
    private val stateCallback: CameraExtensionSessionWrapper.StateCallback,
    lastStateCallback: SessionStateCallback?,
    private val cameraErrorListener: CameraErrorListener,
    private val interopCaptureSessionListener: CameraInterop.CaptureSessionListener? = null,
    private val callbackExecutor: Executor,
) : CameraExtensionSession.StateCallback() {
    private val _lastStateCallback = atomic(lastStateCallback)
    private val extensionSession = atomic<CameraExtensionSessionWrapper?>(null)

    override fun onConfigured(session: CameraExtensionSession) {
        val sessionWrapper = getWrapped(session, cameraErrorListener)
        stateCallback.onConfigured(sessionWrapper)

        // b/249258992 - This is a workaround to ensure previous
        // CameraExtensionSession.StateCallback instances receive some kind of "finalization"
        // signal if onClosed is not fired by the framework after a subsequent session
        // has been configured.
        finalizeLastSession()
        interopCaptureSessionListener?.onConfigured(device.cameraId, sessionWrapper.id)
    }

    override fun onConfigureFailed(session: CameraExtensionSession) {
        val sessionWrapper = getWrapped(session, cameraErrorListener)
        stateCallback.onConfigureFailed(sessionWrapper)
        finalizeSession()
        interopCaptureSessionListener?.onConfigureFailed(device.cameraId, sessionWrapper.id)
    }

    override fun onClosed(session: CameraExtensionSession) {
        val sessionWrapper = getWrapped(session, cameraErrorListener)
        stateCallback.onClosed(getWrapped(session, cameraErrorListener))
        finalizeSession()
        interopCaptureSessionListener?.onClosed(device.cameraId, sessionWrapper.id)
    }

    private fun getWrapped(
        session: CameraExtensionSession,
        cameraErrorListener: CameraErrorListener,
    ): CameraExtensionSessionWrapper {
        var local = extensionSession.value
        if (local != null) {
            return local
        }

        local = wrapSession(session, cameraErrorListener)
        if (extensionSession.compareAndSet(null, local)) {
            return local
        }
        return extensionSession.value!!
    }

    private fun wrapSession(
        session: CameraExtensionSession,
        cameraErrorListener: CameraErrorListener,
    ): CameraExtensionSessionWrapper {
        return AndroidCameraExtensionSession(device, session, cameraErrorListener, callbackExecutor)
    }

    private fun finalizeSession() {
        finalizeLastSession()
        stateCallback.onSessionFinalized()
    }

    private fun finalizeLastSession() {
        // Clear out the reference to the previous session, if one was set.
        val previousSession = _lastStateCallback.getAndSet(null)
        previousSession?.let { previousSession.onSessionFinalized() }
    }
}

@RequiresApi(31)
internal open class AndroidCameraExtensionSession(
    override val device: CameraDeviceWrapper,
    private val cameraExtensionSession: CameraExtensionSession,
    private val cameraErrorListener: CameraErrorListener,
    private val callbackExecutor: Executor,
) : CameraExtensionSessionWrapper {
    override val id: CameraInterop.CameraCaptureSessionId =
        CameraInterop.nextCameraCaptureSessionId()
    private val frameNumbers: AtomicLong = atomic(0L)
    private val extensionSessionMap: MutableMap<CameraExtensionSession, Long> = HashMap()

    override fun capture(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int? =
        catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                cameraExtensionSession.capture(
                    request,
                    callbackExecutor,
                    Camera2CaptureSessionCallbackToExtensionCaptureCallback(
                        listener as Camera2CaptureCallback
                    ),
                )
            } else {
                cameraExtensionSession.capture(
                    request,
                    callbackExecutor,
                    Camera2CaptureSessionCallbackToExtensionCaptureCallbackAndroidS(
                        listener as Camera2CaptureCallback,
                        mutableMapOf(),
                    ),
                )
            }
        }

    override fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int? =
        catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                cameraExtensionSession.setRepeatingRequest(
                    request,
                    callbackExecutor,
                    Camera2CaptureSessionCallbackToExtensionCaptureCallback(
                        listener as Camera2CaptureCallback
                    ),
                )
            } else {
                cameraExtensionSession.setRepeatingRequest(
                    request,
                    callbackExecutor,
                    Camera2CaptureSessionCallbackToExtensionCaptureCallbackAndroidS(
                        listener as Camera2CaptureCallback,
                        mutableMapOf(),
                    ),
                )
            }
        }

    override fun stopRepeating(): Boolean =
        catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
            cameraExtensionSession.stopRepeating()
        } != null

    override val isReprocessable: Boolean
        get() = false

    override val inputSurface: Surface?
        get() = null

    override fun abortCaptures(): Boolean = false

    override fun captureBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int? {
        requests.forEach { captureRequest -> capture(captureRequest, listener) }
        return null
    }

    override fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int? {
        check(requests.size == 1) {
            "CameraExtensionSession does not support setRepeatingBurst for more than one" +
                "CaptureRequest"
        }
        return setRepeatingRequest(requests.single(), listener)
    }

    override fun finalizeOutputConfigurations(
        outputConfigs: List<OutputConfigurationWrapper>
    ): Boolean {
        Log.warn { "CameraExtensionSession does not support finalizeOutputConfigurations()" }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraExtensionSession::class -> cameraExtensionSession as T?
            else -> null
        }

    override fun close() {
        return cameraExtensionSession.close()
    }

    override fun getRealTimeCaptureLatency(): CameraExtensionSession.StillCaptureLatency? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return cameraExtensionSession.realtimeStillCaptureLatency
        }
        return null
    }

    inner class Camera2CaptureSessionCallbackToExtensionCaptureCallback(
        private val captureCallback: Camera2CaptureCallback
    ) : CameraExtensionSession.ExtensionCaptureCallback() {
        private val frameQueue = ConcurrentLinkedQueue<Long>()

        override fun onCaptureStarted(
            session: CameraExtensionSession,
            request: CaptureRequest,
            timestamp: Long,
        ) {
            val frameNumber = incrementAndGetNextFrameNumber(session)
            captureCallback.onCaptureStarted(request, frameNumber, timestamp)
        }

        override fun onCaptureProcessStarted(
            session: CameraExtensionSession,
            request: CaptureRequest,
        ) {}

        override fun onCaptureProcessProgressed(
            session: CameraExtensionSession,
            request: CaptureRequest,
            progress: Int,
        ) {
            captureCallback.onCaptureProcessProgressed(request, progress)
        }

        override fun onCaptureFailed(session: CameraExtensionSession, request: CaptureRequest) {
            val frameNumber = dequeueFrameNumber(session)
            captureCallback.onCaptureFailed(request, FrameNumber(frameNumber))
        }

        override fun onCaptureSequenceCompleted(session: CameraExtensionSession, sequenceId: Int) {
            val frameNumber = extensionSessionMap[session]
            captureCallback.onCaptureSequenceCompleted(sequenceId, frameNumber!!)
        }

        override fun onCaptureSequenceAborted(session: CameraExtensionSession, sequenceId: Int) {
            captureCallback.onCaptureSequenceAborted(sequenceId)
        }

        override fun onCaptureResultAvailable(
            session: CameraExtensionSession,
            request: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            val frameNumber = dequeueFrameNumber(session)
            captureCallback.onCaptureCompleted(request, result, FrameNumber(frameNumber))
        }

        private fun incrementAndGetNextFrameNumber(session: CameraExtensionSession): Long {
            val frameNumber = frameNumbers.incrementAndGet()
            extensionSessionMap[session] = frameNumber
            frameQueue.add(frameNumber)
            return frameNumber
        }

        private fun dequeueFrameNumber(session: CameraExtensionSession): Long {
            // For some cases, onCaptureStarted might not come before the other callback is invoked.
            // It will cause NoSuchElementException. Checks whether the frameQueue is empty to add
            // an item before doing the remove operation to avoid the unexpected exception.
            // See b/433869312 for more details.
            if (frameQueue.isEmpty()) {
                incrementAndGetNextFrameNumber(session)
            }
            return frameQueue.remove()
        }
    }

    /**
     * [CameraExtensionSession.ExtensionCaptureCallback]'s onCaptureResultAvailable is gated behind
     * Android T, so any devices on Android S or older will not see onCaptureResultAvailable being
     * triggered. This implementation calls onCaptureCompleted in onCaptureStarted and does not keep
     * track of completed or failed frames for repeating requests.
     */
    inner class Camera2CaptureSessionCallbackToExtensionCaptureCallbackAndroidS(
        private val captureCallback: Camera2CaptureCallback,
        private val captureRequestMap: MutableMap<CaptureRequest, MutableList<Long>>,
    ) : CameraExtensionSession.ExtensionCaptureCallback() {

        override fun onCaptureStarted(
            session: CameraExtensionSession,
            request: CaptureRequest,
            timestamp: Long,
        ) {
            val frameNumber = frameNumbers.incrementAndGet()
            extensionSessionMap[session] = frameNumber
            captureRequestMap.getOrPut(request) { mutableListOf() }.add(frameNumber)
            captureCallback.onCaptureStarted(request, frameNumber, timestamp)
        }

        override fun onCaptureProcessStarted(
            session: CameraExtensionSession,
            request: CaptureRequest,
        ) {}

        override fun onCaptureFailed(session: CameraExtensionSession, request: CaptureRequest) {
            if (captureRequestMap[request]!!.size == 1) {
                val frameNumber = captureRequestMap[request]!![0]
                captureCallback.onCaptureFailed(request, FrameNumber(frameNumber))
            } else {
                Log.info {
                    "onCaptureFailed is not triggered for repeating requests. Request " +
                        "frame numbers: " +
                        captureRequestMap[request]!!.stream()
                }
            }
        }

        override fun onCaptureProcessProgressed(
            session: CameraExtensionSession,
            request: CaptureRequest,
            progress: Int,
        ) {
            captureCallback.onCaptureProcessProgressed(request, progress)
        }

        override fun onCaptureSequenceCompleted(session: CameraExtensionSession, sequenceId: Int) {
            val frameNumber = extensionSessionMap[session]
            captureCallback.onCaptureSequenceCompleted(sequenceId, frameNumber!!)
        }

        override fun onCaptureSequenceAborted(session: CameraExtensionSession, sequenceId: Int) {
            captureCallback.onCaptureSequenceAborted(sequenceId)
        }
    }
}
