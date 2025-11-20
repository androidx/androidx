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
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.os.Handler
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraInterop
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

/**
 * Interface shim for [CameraCaptureSession] with minor modifications.
 *
 * This interface has been modified to correct nullness, adjust exceptions, and to return or produce
 * wrapper interfaces instead of the native Camera2 types.
 */
internal interface CameraCaptureSessionWrapper : UnsafeWrapper, AutoCloseable {
    val id: CameraInterop.CameraCaptureSessionId

    /**
     * @return The [CameraDeviceWrapper] that created this CameraCaptureSession
     * @see [CameraCaptureSession.getDevice]
     */
    val device: CameraDeviceWrapper

    /**
     * @return True if the application can submit reprocess capture requests with this camera
     *   capture session. false otherwise.
     * @see [CameraCaptureSession.isReprocessable].
     */
    val isReprocessable: Boolean

    /**
     * @return The Surface where reprocessing capture requests get the input images from.
     * @see [CameraCaptureSession.getInputSurface]
     */
    val inputSurface: Surface?

    /** @see [CameraCaptureSession.abortCaptures]. */
    fun abortCaptures(): Boolean

    /**
     * @param request The settings for this exposure
     * @param listener The callback object to notify once this request has been processed.
     * @return An unique capture sequence id.
     * @see [CameraCaptureSession.capture].
     */
    fun capture(request: CaptureRequest, listener: CameraCaptureSession.CaptureCallback): Int?

    /**
     * @param requests A list of CaptureRequest(s) for this sequence of exposures
     * @param listener A callback object to notify each time one of the requests in the burst has
     *   been processed.
     * @return An unique capture sequence id.
     * @see [CameraCaptureSession.captureBurst].
     */
    fun captureBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int?

    /**
     * @param requests A list of settings to cycle through indefinitely.
     * @param listener A callback object to notify each time one of the requests in the repeating
     *   bursts has finished processing.
     * @return An unique capture sequence ID.
     * @see [CameraCaptureSession.setRepeatingBurst]
     */
    fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int?

    /**
     * @param request The request to repeat indefinitely.
     * @param listener The callback object to notify every time the request finishes processing.
     * @return An unique capture sequence ID.
     * @see [CameraCaptureSession.setRepeatingRequest].
     */
    fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int?

    /** @see [CameraCaptureSession.stopRepeating]. */
    fun stopRepeating(): Boolean

    /** Forwards to CameraCaptureSession#finalizeOutputConfigurations */
    fun finalizeOutputConfigurations(outputConfigs: List<OutputConfigurationWrapper>): Boolean

    /** @see CameraCaptureSession.StateCallback */
    interface StateCallback : SessionStateCallback {
        /** @see CameraCaptureSession.StateCallback.onActive */
        fun onActive(session: CameraCaptureSessionWrapper)

        /** @see CameraCaptureSession.StateCallback.onClosed */
        fun onClosed(session: CameraCaptureSessionWrapper)

        /** @see CameraCaptureSession.StateCallback.onConfigureFailed */
        fun onConfigureFailed(session: CameraCaptureSessionWrapper)

        /** @see CameraCaptureSession.StateCallback.onConfigured */
        fun onConfigured(session: CameraCaptureSessionWrapper)

        /** @see CameraCaptureSession.StateCallback.onReady */
        fun onReady(session: CameraCaptureSessionWrapper)

        /** @see CameraCaptureSession.StateCallback.onReady */
        fun onCaptureQueueEmpty(session: CameraCaptureSessionWrapper)
    }
}

internal interface CameraConstrainedHighSpeedCaptureSessionWrapper : CameraCaptureSessionWrapper {
    /**
     * Forwards to [CameraConstrainedHighSpeedCaptureSession.createHighSpeedRequestList]
     *
     * @param request A capture list.
     * @return A list of high speed requests.
     */
    fun createHighSpeedRequestList(request: CaptureRequest): List<CaptureRequest>?
}

internal class AndroidCaptureSessionStateCallback(
    private val device: CameraDeviceWrapper,
    private val stateCallback: CameraCaptureSessionWrapper.StateCallback,
    lastStateCallback: SessionStateCallback?,
    private val cameraErrorListener: CameraErrorListener,
    private val interopCaptureSessionListener: CameraInterop.CaptureSessionListener? = null,
    private val callbackHandler: Handler,
) : CameraCaptureSession.StateCallback() {
    private val _lastStateCallback = atomic(lastStateCallback)
    private val captureSession = atomic<CameraCaptureSessionWrapper?>(null)

    override fun onConfigured(session: CameraCaptureSession) {
        val wrappedSession = getWrapped(session, cameraErrorListener)
        stateCallback.onConfigured(wrappedSession)

        // b/249258992 - This is a workaround to ensure previous CameraCaptureSession.StateCallback
        //   instances receive some kind of "finalization" signal if onClosed is not fired by the
        //   framework after a subsequent session has been configured.
        finalizeLastSession()
        interopCaptureSessionListener?.onConfigured(device.cameraId, wrappedSession.id)
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        val wrappedSession = getWrapped(session, cameraErrorListener)
        stateCallback.onConfigureFailed(wrappedSession)
        finalizeSession()
        interopCaptureSessionListener?.onConfigureFailed(device.cameraId, wrappedSession.id)
    }

    override fun onReady(session: CameraCaptureSession) {
        val wrappedSession = getWrapped(session, cameraErrorListener)
        stateCallback.onReady(getWrapped(session, cameraErrorListener))
        interopCaptureSessionListener?.onReady(device.cameraId, wrappedSession.id)
    }

    override fun onActive(session: CameraCaptureSession) {
        val wrappedSession = getWrapped(session, cameraErrorListener)
        stateCallback.onActive(getWrapped(session, cameraErrorListener))
        interopCaptureSessionListener?.onActive(device.cameraId, wrappedSession.id)
    }

    override fun onClosed(session: CameraCaptureSession) {
        val wrappedSession = getWrapped(session, cameraErrorListener)
        stateCallback.onClosed(getWrapped(session, cameraErrorListener))
        finalizeSession()
        interopCaptureSessionListener?.onClosed(device.cameraId, wrappedSession.id)
    }

    override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
        val wrappedSession = getWrapped(session, cameraErrorListener)
        stateCallback.onCaptureQueueEmpty(getWrapped(session, cameraErrorListener))
        interopCaptureSessionListener?.onCaptureQueueEmpty(device.cameraId, wrappedSession.id)
    }

    private fun getWrapped(
        session: CameraCaptureSession,
        cameraErrorListener: CameraErrorListener,
    ): CameraCaptureSessionWrapper {
        var local = captureSession.value
        if (local != null) {
            return local
        }

        local = wrapSession(session, cameraErrorListener)
        if (captureSession.compareAndSet(null, local)) {
            return local
        }
        return captureSession.value!!
    }

    private fun wrapSession(
        session: CameraCaptureSession,
        cameraErrorListener: CameraErrorListener,
    ): CameraCaptureSessionWrapper {
        // Starting in Android P, it's possible for the standard "createCaptureSession" method to
        // return a CameraConstrainedHighSpeedCaptureSession depending on the configuration. If
        // this happens, several methods are not allowed, the behavior is different, and interacting
        // with the session requires several behavior changes for these interactions to work well.
        return if (session is CameraConstrainedHighSpeedCaptureSession) {
            AndroidCameraConstrainedHighSpeedCaptureSession(
                device,
                session,
                cameraErrorListener,
                callbackHandler,
            )
        } else {
            AndroidCameraCaptureSession(device, session, cameraErrorListener, callbackHandler)
        }
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

internal val captureSessionIds = atomic(0)

internal open class AndroidCameraCaptureSession(
    override val device: CameraDeviceWrapper,
    private val cameraCaptureSession: CameraCaptureSession,
    private val cameraErrorListener: CameraErrorListener,
    private val callbackHandler: Handler,
) : CameraCaptureSessionWrapper {

    override fun abortCaptures(): Boolean =
        instrumentAndCatch("abortCaptures") { cameraCaptureSession.abortCaptures() } != null

    override fun capture(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int? =
        instrumentAndCatch("capture") {
            cameraCaptureSession.capture(request, listener, callbackHandler)
        }

    override fun captureBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int? =
        instrumentAndCatch("captureBurst") {
            cameraCaptureSession.captureBurst(requests, listener, callbackHandler)
        }

    override fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int? =
        instrumentAndCatch("setRepeatingBurst") {
            cameraCaptureSession.setRepeatingBurst(requests, listener, callbackHandler)
        }

    override fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int? =
        instrumentAndCatch("setRepeatingRequest") {
            cameraCaptureSession.setRepeatingRequest(request, listener, callbackHandler)
        }

    override fun stopRepeating(): Boolean =
        instrumentAndCatch("stopRepeating") { cameraCaptureSession.stopRepeating() } != null

    override val id: CameraInterop.CameraCaptureSessionId =
        CameraInterop.nextCameraCaptureSessionId()

    override val isReprocessable: Boolean
        get() = cameraCaptureSession.isReprocessable

    override val inputSurface: Surface?
        get() = cameraCaptureSession.inputSurface

    @RequiresApi(26)
    override fun finalizeOutputConfigurations(
        outputConfigs: List<OutputConfigurationWrapper>
    ): Boolean {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            "Attempting to call finalizeOutputConfigurations before O is not supported and may " +
                "lead to to unexpected behavior if an application is expects this call to " +
                "succeed."
        }

        return instrumentAndCatch("finalizeOutputConfigurations") {
            Api26Compat.finalizeOutputConfigurations(
                cameraCaptureSession,
                outputConfigs.map { it.unwrapAs(OutputConfiguration::class) },
            )
        } != null
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraCaptureSession::class -> cameraCaptureSession as T?
            else -> null
        }

    override fun close() {
        return cameraCaptureSession.close()
    }

    /** Utility function to trace, measure, and suppress exceptions for expensive method calls. */
    @Throws(ObjectUnavailableException::class)
    internal inline fun <T> instrumentAndCatch(fnName: String, crossinline block: () -> T) =
        Debug.instrument("CXCP#$fnName-${device.cameraId.value}") {
            catchAndReportCameraExceptions(device.cameraId, cameraErrorListener, block)
        }
}

/**
 * An implementation of [CameraConstrainedHighSpeedCaptureSessionWrapper] forwards calls to a real
 * [CameraConstrainedHighSpeedCaptureSession].
 */
internal class AndroidCameraConstrainedHighSpeedCaptureSession
internal constructor(
    device: CameraDeviceWrapper,
    private val session: CameraConstrainedHighSpeedCaptureSession,
    cameraErrorListener: CameraErrorListener,
    callbackHandler: Handler,
) :
    AndroidCameraCaptureSession(device, session, cameraErrorListener, callbackHandler),
    CameraConstrainedHighSpeedCaptureSessionWrapper {
    override fun createHighSpeedRequestList(request: CaptureRequest): List<CaptureRequest>? =
        try {
            // This converts a single CaptureRequest into a list of CaptureRequest(s) that must be
            // submitted together during high speed recording.
            Debug.trace("CXCP#createHighSpeedRequestList") {
                session.createHighSpeedRequestList(request)
            }
        } catch (e: IllegalStateException) {

            // b/111749845: If the camera device is closed before calling
            // createHighSpeedRequestList it may throw an [IllegalStateException]. Since this can
            // happen during normal operation of the camera, log and rethrow the error as a standard
            // exception that can be ignored.
            Log.warn { "Failed to createHighSpeedRequestList. $device may be closed." }
            null
        } catch (e: IllegalArgumentException) {

            // b/111749845: If the surface (such as the viewfinder) is destroyed before calling
            // createHighSpeedRequestList it may throw an [IllegalArgumentException]. Since this can
            // happen during normal operation of the camera, log and rethrow the error as a standard
            // exception that can be ignored.
            Log.warn {
                "Failed to createHighSpeedRequestList from $device because the output surface" +
                    " was destroyed before calling createHighSpeedRequestList."
            }
            null
        } catch (e: UnsupportedOperationException) {

            // b/358592149: When a high speed session is closed, and then another high speed session
            // is opened, the resources from the previous session might not be available yet.
            // Since Camera2CaptureSequenceProcessor will try to create the session again, log
            // and rethrow the error as a standard exception that can be ignored.
            Log.warn {
                "Failed to createHighSpeedRequestList from $device because the output surface" +
                    " was not available."
            }
            null
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraConstrainedHighSpeedCaptureSession::class -> session as T?
            else -> super.unwrapAs(type)
        }
}
