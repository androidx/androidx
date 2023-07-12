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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.os.Handler
import android.view.Surface
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.UnsafeWrapper
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
     * @param handler The handler on which the listener should be invoked, or null to use the
     *   current thread's looper.
     * @return An unique capture sequence id.
     * @see [CameraCaptureSession.capture].
     */
    fun capture(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int?

    /**
     * @param requests A list of CaptureRequest(s) for this sequence of exposures
     * @param listener A callback object to notify each time one of the requests in the burst has
     *   been processed.
     * @param handler The handler on which the listener should be invoked, or null to use the
     *   current thread's looper.
     * @return An unique capture sequence id.
     * @see [CameraCaptureSession.captureBurst].
     */
    fun captureBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int?

    /**
     * @param requests A list of settings to cycle through indefinitely.
     * @param listener A callback object to notify each time one of the requests in the repeating
     *   bursts has finished processing.
     * @param handler The handler on which the listener should be invoked, or null to use the
     *   current thread's looper.
     * @return An unique capture sequence ID.
     * @see [CameraCaptureSession.setRepeatingBurst]
     */
    fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int?

    /**
     * @param request The request to repeat indefinitely.
     * @param listener The callback object to notify every time the request finishes processing.
     * @param handler The handler on which the listener should be invoked, or null to use the
     *   current thread's looper.
     * @return An unique capture sequence ID.
     * @see [CameraCaptureSession.setRepeatingRequest].
     */
    fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int?

    /** @see [CameraCaptureSession.stopRepeating]. */
    fun stopRepeating(): Boolean

    /** Forwards to CameraCaptureSession#finalizeOutputConfigurations */
    fun finalizeOutputConfigurations(outputConfigs: List<OutputConfigurationWrapper>): Boolean

    /** @see CameraCaptureSession.StateCallback */
    interface StateCallback : OnSessionFinalized {
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
    @Throws(ObjectUnavailableException::class)
    fun createHighSpeedRequestList(request: CaptureRequest): List<CaptureRequest>
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class AndroidCaptureSessionStateCallback(
    private val device: CameraDeviceWrapper,
    private val stateCallback: CameraCaptureSessionWrapper.StateCallback,
    lastStateCallback: OnSessionFinalized?,
    private val cameraErrorListener: CameraErrorListener,
    private val interopSessionStateCallback: CameraCaptureSession.StateCallback? = null
) : CameraCaptureSession.StateCallback() {
    private val _lastStateCallback = atomic(lastStateCallback)
    private val captureSession = atomic<CameraCaptureSessionWrapper?>(null)

    override fun onConfigured(session: CameraCaptureSession) {
        stateCallback.onConfigured(getWrapped(session, cameraErrorListener))

        // b/249258992 - This is a workaround to ensure previous CameraCaptureSession.StateCallback
        //   instances receive some kind of "finalization" signal if onClosed is not fired by the
        //   framework after a subsequent session has been configured.
        finalizeLastSession()
        interopSessionStateCallback?.onConfigured(session)
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        stateCallback.onConfigureFailed(getWrapped(session, cameraErrorListener))
        finalizeSession()
        interopSessionStateCallback?.onConfigureFailed(session)
    }

    override fun onReady(session: CameraCaptureSession) {
        stateCallback.onReady(getWrapped(session, cameraErrorListener))
        interopSessionStateCallback?.onReady(session)
    }

    override fun onActive(session: CameraCaptureSession) {
        stateCallback.onActive(getWrapped(session, cameraErrorListener))
        interopSessionStateCallback?.onActive(session)
    }

    override fun onClosed(session: CameraCaptureSession) {
        stateCallback.onClosed(getWrapped(session, cameraErrorListener))
        finalizeSession()
        interopSessionStateCallback?.onClosed(session)
    }

    override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
        stateCallback.onCaptureQueueEmpty(getWrapped(session, cameraErrorListener))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Api26CompatImpl.onCaptureQueueEmpty(session, interopSessionStateCallback)
        }
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
        return if (Build.VERSION.SDK_INT >= 23 &&
            session is CameraConstrainedHighSpeedCaptureSession
        ) {
            AndroidCameraConstrainedHighSpeedCaptureSession(device, session, cameraErrorListener)
        } else {
            AndroidCameraCaptureSession(device, session, cameraErrorListener)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private object Api26CompatImpl {
        @DoNotInline
        @JvmStatic
        fun onCaptureQueueEmpty(
            session: CameraCaptureSession,
            interopSessionStateCallback: CameraCaptureSession.StateCallback?
        ) {
            interopSessionStateCallback?.onCaptureQueueEmpty(session)
        }
    }
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal open class AndroidCameraCaptureSession(
    override val device: CameraDeviceWrapper,
    private val cameraCaptureSession: CameraCaptureSession,
    private val cameraErrorListener: CameraErrorListener,
) : CameraCaptureSessionWrapper {
    override fun abortCaptures(): Boolean =
        catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
            cameraCaptureSession.abortCaptures()
        } != null

    override fun capture(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int? = catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
        cameraCaptureSession.capture(
            request,
            listener,
            handler
        )
    }

    override fun captureBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int? = catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
        cameraCaptureSession.captureBurst(requests, listener, handler)
    }

    override fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int? = catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
        cameraCaptureSession.setRepeatingBurst(requests, listener, handler)
    }

    override fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int? = catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
        cameraCaptureSession.setRepeatingRequest(request, listener, handler)
    }

    override fun stopRepeating(): Boolean =
        catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
            cameraCaptureSession.stopRepeating()
        } != null

    override val isReprocessable: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Api23Compat.isReprocessable(cameraCaptureSession)
            }
            // Reprocessing is not supported  prior to Android M
            return false
        }

    override val inputSurface: Surface?
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Api23Compat.getInputSurface(cameraCaptureSession)
            }
            // Reprocessing is not supported prior to Android M, and a CaptureSession that does not
            // support reprocessing will have a null input surface on M and beyond.
            return null
        }

    @RequiresApi(26)
    override fun finalizeOutputConfigurations(
        outputConfigs: List<OutputConfigurationWrapper>
    ): Boolean {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            "Attempting to call finalizeOutputConfigurations before O is not supported and may " +
                "lead to to unexpected behavior if an application is expects this call to " +
                "succeed."
        }

        return catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
            Api26Compat.finalizeOutputConfigurations(
                cameraCaptureSession,
                outputConfigs.map { it.unwrapAs(OutputConfiguration::class) })
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
}

/**
 * An implementation of [CameraConstrainedHighSpeedCaptureSessionWrapper] forwards calls to a real
 * [CameraConstrainedHighSpeedCaptureSession].
 */
@RequiresApi(23)
internal class AndroidCameraConstrainedHighSpeedCaptureSession
internal constructor(
    device: CameraDeviceWrapper,
    private val session: CameraConstrainedHighSpeedCaptureSession,
    private val cameraErrorListener: CameraErrorListener,
) : AndroidCameraCaptureSession(device, session, cameraErrorListener),
    CameraConstrainedHighSpeedCaptureSessionWrapper {
    @Throws(ObjectUnavailableException::class)
    override fun createHighSpeedRequestList(request: CaptureRequest): List<CaptureRequest> {
        return try {
            // This converts a single CaptureRequest into a list of CaptureRequest(s) that must be
            // submitted together during high speed recording.
            session.createHighSpeedRequestList(request)
        } catch (e: IllegalStateException) {

            // b/111749845: If the camera device is closed before calling
            // createHighSpeedRequestList it may throw an [IllegalStateException]. Since this can
            // happen during normal operation of the camera, log and rethrow the error as a standard
            // exception that can be ignored.
            Log.warn { "Failed to createHighSpeedRequestList. $device may be closed." }
            throw ObjectUnavailableException(e)
        } catch (e: IllegalArgumentException) {

            // b/111749845: If the surface (such as the viewfinder) is destroyed before calling
            // createHighSpeedRequestList it may throw an [IllegalArgumentException]. Since this can
            // happen during normal operation of the camera, log and rethrow the error as a standard
            // exception that can be ignored.
            Log.warn {
                "Failed to createHighSpeedRequestList from $device because the output surface" +
                    " was destroyed before calling createHighSpeedRequestList."
            }
            throw ObjectUnavailableException(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraConstrainedHighSpeedCaptureSession::class -> session as T?
            else -> super.unwrapAs(type)
        }
}
