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
import android.os.Build
import android.os.Handler
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.core.Log
import kotlinx.atomicfu.atomic
import java.io.Closeable

/**
 * Interface shim for [CameraCaptureSession] with minor modifications.
 *
 * This interface has been modified to correct nullness, adjust exceptions, and to return or produce
 * wrapper interfaces instead of the native Camera2 types.
 */
internal interface CameraCaptureSessionWrapper : UnsafeWrapper<CameraCaptureSession>, Closeable {

    /**
     * @see [CameraCaptureSession.getDevice]
     *
     * @return The [CameraDeviceWrapper] that created this CameraCaptureSession
     */
    val device: CameraDeviceWrapper

    /**
     * @see [CameraCaptureSession.isReprocessable].
     *
     * @return True if the application can submit reprocess capture requests with this camera capture
     * session. false otherwise.
     */
    val isReprocessable: Boolean

    /**
     * @see [CameraCaptureSession.getInputSurface]
     *
     * @return The Surface where reprocessing capture requests get the input images from.
     */
    val inputSurface: Surface?

    /**
     * @see [CameraCaptureSession.abortCaptures].
     */
    @Throws(ObjectUnavailableException::class)
    fun abortCaptures()

    /**
     * @see [CameraCaptureSession.capture].
     *
     * @param request The settings for this exposure
     * @param listener The callback object to notify once this request has been processed.
     * @param handler The handler on which the listener should be invoked, or null to use the
     * current thread's looper.
     * @return An unique capture sequence id.
     */
    @Throws(ObjectUnavailableException::class)
    fun capture(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int

    /**
     * @see [CameraCaptureSession.captureBurst].
     *
     * @param requests A list of CaptureRequest(s) for this sequence of exposures
     * @param listener A callback object to notify each time one of the requests in the burst has been
     * processed.
     * @param handler The handler on which the listener should be invoked, or null to use the current
     * thread's looper.
     * @return An unique capture sequence id.
     */
    @Throws(ObjectUnavailableException::class)
    fun captureBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int

    /**
     * @see [CameraCaptureSession.setRepeatingBurst]
     *
     * @param requests A list of settings to cycle through indefinitely.
     * @param listener A callback object to notify each time one of the requests in the repeating
     * bursts has finished processing.
     * @param handler The handler on which the listener should be invoked, or null to use the current
     * thread's looper.
     * @return An unique capture sequence ID.
     */
    @Throws(ObjectUnavailableException::class)
    fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int

    /**
     * @see [CameraCaptureSession.setRepeatingRequest].
     *
     * @param request The request to repeat indefinitely.
     * @param listener The callback object to notify every time the request finishes processing.
     * @param handler The handler on which the listener should be invoked, or null to use the current
     * thread's looper.
     * @return An unique capture sequence ID.
     */
    @Throws(ObjectUnavailableException::class)
    fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int

    /**
     * @see [CameraCaptureSession.stopRepeating].
     */
    @Throws(ObjectUnavailableException::class)
    fun stopRepeating()

    /** Forwards to CameraCaptureSession#finalizeOutputConfigurations  */
    @Throws(ObjectUnavailableException::class)
    fun finalizeOutputConfigurations(outputConfigs: List<OutputConfigurationWrapper>)

    /** @see CameraCaptureSession.StateCallback */
    interface StateCallback {
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

internal class AndroidCaptureSessionStateCallback(
    private val device: CameraDeviceWrapper,
    private val stateCallback: CameraCaptureSessionWrapper.StateCallback
) : CameraCaptureSession.StateCallback() {
    private val captureSession = atomic<CameraCaptureSessionWrapper?>(null)

    override fun onConfigured(session: CameraCaptureSession) {
        stateCallback.onConfigured(getWrapped(session))
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        stateCallback.onConfigureFailed(getWrapped(session))
    }

    override fun onReady(session: CameraCaptureSession) {
        stateCallback.onReady(getWrapped(session))
    }

    override fun onActive(session: CameraCaptureSession) {
        stateCallback.onActive(getWrapped(session))
    }

    override fun onClosed(session: CameraCaptureSession) {
        stateCallback.onClosed(getWrapped(session))
    }

    override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
        stateCallback.onCaptureQueueEmpty(getWrapped(session))
    }

    private fun getWrapped(session: CameraCaptureSession): CameraCaptureSessionWrapper {
        var local = captureSession.value
        if (local != null) {
            return local
        }

        local = wrapSession(session)
        if (captureSession.compareAndSet(null, local)) {
            return local
        }
        return captureSession.value!!
    }

    private fun wrapSession(session: CameraCaptureSession): CameraCaptureSessionWrapper {
        // Starting in Android P, it's possible for the standard "createCaptureSession" method to
        // return a CameraConstrainedHighSpeedCaptureSession depending on the configuration. If
        // this happens, several methods are not allowed, the behavior is different, and interacting
        // with the session requires several behavior changes for these interactions to work well.
        return if (Build.VERSION.SDK_INT >= 23 &&
            session is CameraConstrainedHighSpeedCaptureSession
        ) {
            AndroidCameraConstrainedHighSpeedCaptureSession(device, session)
        } else {
            AndroidCameraCaptureSession(device, session)
        }
    }
}

internal open class AndroidCameraCaptureSession(
    override val device: CameraDeviceWrapper,
    private val cameraCaptureSession: CameraCaptureSession
) : CameraCaptureSessionWrapper {
    override fun abortCaptures() {
        rethrowCamera2Exceptions {
            cameraCaptureSession.abortCaptures()
        }
    }

    override fun capture(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int {
        return rethrowCamera2Exceptions {
            cameraCaptureSession.capture(request, listener, handler)
        }
    }

    override fun captureBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int {
        return rethrowCamera2Exceptions {
            cameraCaptureSession.captureBurst(requests, listener, handler)
        }
    }

    override fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int {
        return rethrowCamera2Exceptions {
            cameraCaptureSession.setRepeatingBurst(
                requests, listener,
                handler
            )
        }
    }

    override fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int {
        return rethrowCamera2Exceptions {
            cameraCaptureSession.setRepeatingRequest(request, listener, handler)
        }
    }

    override fun stopRepeating() {
        rethrowCamera2Exceptions {
            cameraCaptureSession.stopRepeating()
        }
    }

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
    override fun finalizeOutputConfigurations(outputConfigs: List<OutputConfigurationWrapper>) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            "Attempting to call finalizeOutputConfigurations before O is not supported and may " +
                "lead to to unexpected behavior if an application is expects this call to " +
                "succeed."
        }

        rethrowCamera2Exceptions {
            Api26Compat.finalizeOutputConfigurations(
                cameraCaptureSession,
                outputConfigs.map {
                    it.unwrap()
                }
            )
        }
    }

    override fun unwrap(): CameraCaptureSession? {
        return cameraCaptureSession
    }

    override fun close() {
        return cameraCaptureSession.close()
    }
}

/**
 * An implementation of [CameraConstrainedHighSpeedCaptureSessionWrapper] forwards calls to a
 * real [CameraConstrainedHighSpeedCaptureSession].
 */
@RequiresApi(23)
internal class AndroidCameraConstrainedHighSpeedCaptureSession internal constructor(
    device: CameraDeviceWrapper,
    private val session: CameraConstrainedHighSpeedCaptureSession
) : AndroidCameraCaptureSession(device, session), CameraConstrainedHighSpeedCaptureSessionWrapper {
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
}
