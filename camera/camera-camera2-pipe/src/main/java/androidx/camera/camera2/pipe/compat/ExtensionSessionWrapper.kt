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

@file:RequiresApi(31) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CaptureRequest
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import java.util.concurrent.Executor
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

/**
 * Interface shim for [CameraExtensionSession] with minor modifications.
 *
 * This interface has been modified to correct nullness, adjust exceptions, and to return or produce
 * wrapper interfaces instead of the native Camera2 types.
 */
internal interface CameraExtensionSessionWrapper : UnsafeWrapper, AutoCloseable {

    /**
     * @return The [CameraDeviceWrapper] that created this CameraExtensionSession
     * @see [CameraExtensionSession.getDevice]
     */
    val device: CameraDeviceWrapper

    /**
     * @param request The settings for this exposure
     * @param listener The callback object to notify once this request has been processed.
     * @param executor The executor on which the listener should be invoked, or null to use the
     *   current thread's looper.
     * @return An unique capture sequence id.
     * @see [CameraExtensionSession.capture].
     */
    fun capture(
        request: CaptureRequest,
        executor: Executor,
        listener: CameraExtensionSession.ExtensionCaptureCallback
    ): Int?

    /**
     * @param request The request to repeat indefinitely.
     * @param executor The executor on which the listener should be invoked, or null to use the
     *   current thread's looper.
     * @param listener The callback object to notify every time the request finishes processing.
     * @return An unique capture sequence ID.
     * @see [CameraExtensionSession.setRepeatingRequest].
     */
    fun setRepeatingRequest(
        request: CaptureRequest,
        executor: Executor,
        listener: CameraExtensionSession.ExtensionCaptureCallback
    ): Int?

    /** @see [CameraExtensionSession.stopRepeating]. */
    fun stopRepeating(): Boolean

    /** @see CameraExtensionSession.StateCallback */
    interface StateCallback : OnSessionFinalized {
        /** @see CameraExtensionSession.StateCallback.onClosed */
        fun onClosed(session: CameraExtensionSessionWrapper)

        /** @see CameraExtensionSession.StateCallback.onConfigureFailed */
        fun onConfigureFailed(session: CameraExtensionSessionWrapper)

        /** @see CameraExtensionSession.StateCallback.onConfigured */
        fun onConfigured(session: CameraExtensionSessionWrapper)
    }
}

@RequiresApi(31) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class AndroidExtensionSessionStateCallback(
    private val device: CameraDeviceWrapper,
    private val stateCallback: CameraExtensionSessionWrapper.StateCallback,
    lastStateCallback: OnSessionFinalized?,
    private val cameraErrorListener: CameraErrorListener,
    private val interopSessionStateCallback: CameraExtensionSession.StateCallback? = null
) : CameraExtensionSession.StateCallback() {
    private val _lastStateCallback = atomic(lastStateCallback)
    private val extensionSession = atomic<CameraExtensionSessionWrapper?>(null)

    override fun onConfigured(session: CameraExtensionSession) {
        stateCallback.onConfigured(getWrapped(session, cameraErrorListener))

        // b/249258992 - This is a workaround to ensure previous
        // CameraExtensionSession.StateCallback instances receive some kind of "finalization"
        // signal if onClosed is not fired by the framework after a subsequent session
        // has been configured.
        finalizeLastSession()
        interopSessionStateCallback?.onConfigured(session)
    }

    override fun onConfigureFailed(session: CameraExtensionSession) {
        stateCallback.onConfigureFailed(getWrapped(session, cameraErrorListener))
        finalizeSession()
        interopSessionStateCallback?.onConfigureFailed(session)
    }

    override fun onClosed(session: CameraExtensionSession) {
        stateCallback.onClosed(getWrapped(session, cameraErrorListener))
        finalizeSession()
        interopSessionStateCallback?.onClosed(session)
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
        return AndroidCameraExtensionSession(device, session, cameraErrorListener)
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

@RequiresApi(31) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal open class AndroidCameraExtensionSession(
    override val device: CameraDeviceWrapper,
    private val cameraExtensionSession: CameraExtensionSession,
    private val cameraErrorListener: CameraErrorListener,
) : CameraExtensionSessionWrapper {
    override fun capture(
        request: CaptureRequest,
        executor: Executor,
        listener: CameraExtensionSession.ExtensionCaptureCallback
    ): Int? = catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
        cameraExtensionSession.capture(
            request,
            executor,
            listener,
        )
    }

    override fun setRepeatingRequest(
        request: CaptureRequest,
        executor: Executor,
        listener: CameraExtensionSession.ExtensionCaptureCallback,
    ): Int? = catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
        cameraExtensionSession.setRepeatingRequest(request, executor, listener)
    }

    override fun stopRepeating(): Boolean =
        catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
            cameraExtensionSession.stopRepeating()
        } != null

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraExtensionSession::class -> cameraExtensionSession as T?
            else -> null
        }

    override fun close() {
        return cameraExtensionSession.close()
    }
}
