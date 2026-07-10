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

package androidx.camera.camera2.pipe

import android.view.Surface
import androidx.annotation.RestrictTo
import kotlinx.atomicfu.atomic

/** Public interfaces that are used to more safely interact with raw events from Camera2. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object CameraInterop {
    /**
     * Wrapper for observing events from the internal
     * [android.hardware.camera2.CameraCaptureSession.StateCallback] events.
     */
    public interface CaptureSessionListener {

        /**
         * @see [android.hardware.camera2.CameraCaptureSession.StateCallback.onConfigured].
         * @see [android.hardware.camera2.CameraExtensionSession.StateCallback.onConfigured].
         */
        public fun onConfigured(cameraId: CameraId, captureSessionId: CameraCaptureSessionId)

        /**
         * @see [android.hardware.camera2.CameraCaptureSession.StateCallback.onConfigureFailed].
         * @see [android.hardware.camera2.CameraExtensionSession.StateCallback.onConfigureFailed].
         */
        public fun onConfigureFailed(cameraId: CameraId, captureSessionId: CameraCaptureSessionId)

        /**
         * @see [android.hardware.camera2.CameraCaptureSession.StateCallback.onReady]. Not supported
         *   for [android.hardware.camera2.CameraExtensionSession].
         */
        public fun onReady(cameraId: CameraId, captureSessionId: CameraCaptureSessionId)

        /**
         * @see [android.hardware.camera2.CameraCaptureSession.StateCallback.onActive]. Not
         *   supported for [android.hardware.camera2.CameraExtensionSession].
         */
        public fun onActive(cameraId: CameraId, captureSessionId: CameraCaptureSessionId)

        /**
         * @see [android.hardware.camera2.CameraCaptureSession.StateCallback.onCaptureQueueEmpty].
         *   Not supported for [android.hardware.camera2.CameraExtensionSession].
         */
        public fun onCaptureQueueEmpty(cameraId: CameraId, captureSessionId: CameraCaptureSessionId)

        /**
         * @see [android.hardware.camera2.CameraCaptureSession.StateCallback.onClosed].
         * @see [android.hardware.camera2.CameraExtensionSession.StateCallback.onClosed].
         */
        public fun onClosed(cameraId: CameraId, captureSessionId: CameraCaptureSessionId)

        /**
         * @see [android.hardware.camera2.CameraCaptureSession.StateCallback.onSurfacePrepared]. Not
         *   supported for [android.hardware.camera2.CameraExtensionSession].
         */
        public fun onSurfacePrepared(
            cameraId: CameraId,
            captureSessionId: CameraCaptureSessionId,
            surface: Surface,
        )
    }

    @JvmInline public value class CameraCaptureSessionId(public val value: Int)

    @JvmStatic private val captureSessionIds = atomic(0)

    @JvmStatic
    internal fun nextCameraCaptureSessionId(): CameraCaptureSessionId =
        CameraCaptureSessionId(captureSessionIds.incrementAndGet())

    /**
     * Callback for observing the global state of the camera devices managed by CameraPipe.
     *
     * These callbacks are intended for logic that must run before any system-level interaction with
     * [android.hardware.camera2.CameraManager] begins, and after all such interactions have
     * completely finished and no further actions are queued.
     */
    public interface CameraSystemCallbacks {
        /**
         * Called synchronously before the first camera device is opened.
         *
         * This method provides a strong guarantee that it will complete before any interaction with
         * the system camera service occurs. If this method blocks, it will delay the opening of the
         * camera device and block all subsequent CameraPipe operations.
         */
        public fun onCameraSystemStarting()

        /**
         * Called synchronously after the last camera device has been closed.
         *
         * This is only called if there are no other pending requests to open a camera device,
         * providing a strong guarantee that all system camera resources have been released and no
         * new open requests are currently being processed.
         */
        public fun onCameraSystemStopped()
    }
}
