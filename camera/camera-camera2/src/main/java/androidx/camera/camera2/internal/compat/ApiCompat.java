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

package androidx.camera.camera2.internal.compat;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * A helper class to address ClassVerificationFailure.
 *
 * <p>See b/188451897.
 */
public final class ApiCompat {

    private ApiCompat() {
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 5.0 (API 21).
     */
    @RequiresApi(21)
    public static class Api21Impl {

        private Api21Impl() {
        }

        /**
         * @see CameraDevice#close()
         */
        @DoNotInline
        public static void close(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
     */
    @RequiresApi(23)
    public static class Api23Impl {

        private Api23Impl() {
        }

        /**
         * @see CameraCaptureSession.StateCallback#onSurfacePrepared(CameraCaptureSession, Surface)
         */
        @DoNotInline
        public static void onSurfacePrepared(
                @NonNull CameraCaptureSession.StateCallback callback,
                @NonNull CameraCaptureSession session,
                @NonNull Surface surface) {
            callback.onSurfacePrepared(session, surface);
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 7.0 (API 24).
     */
    @RequiresApi(24)
    public static class Api24Impl {

        private Api24Impl() {
        }

        /**
         * @see CameraCaptureSession.CaptureCallback#onCaptureBufferLost(CameraCaptureSession,
         * CaptureRequest, Surface, long)
         */
        @DoNotInline
        public static void onCaptureBufferLost(
                @NonNull CameraCaptureSession.CaptureCallback callback,
                @NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                @NonNull Surface surface,
                long frame) {
            callback.onCaptureBufferLost(session, request, surface, frame);
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 8.0 (API 26).
     */
    @RequiresApi(26)
    public static class Api26Impl {

        private Api26Impl() {
        }

        /**
         * @see CameraCaptureSession.StateCallback#onCaptureQueueEmpty(CameraCaptureSession)
         */
        @DoNotInline
        public static void onCaptureQueueEmpty(
                @NonNull CameraCaptureSession.StateCallback callback,
                @NonNull CameraCaptureSession session) {
            callback.onCaptureQueueEmpty(session);
        }

        /**
         * @see OutputConfiguration
         */
        @DoNotInline
        @NonNull
        public static <T> OutputConfiguration newOutputConfiguration(@NonNull Size surfaceSize,
                @NonNull Class<T> klass) {
            return new OutputConfiguration(surfaceSize, klass);
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 10 (API 29).
     */
    @RequiresApi(29)
    public static class Api29Impl {

        private Api29Impl() {
        }

        /**
         * @see CameraManager.AvailabilityCallback#onCameraAccessPrioritiesChanged()
         */
        @DoNotInline
        public static void onCameraAccessPrioritiesChanged(
                @NonNull CameraManager.AvailabilityCallback callback) {
            callback.onCameraAccessPrioritiesChanged();
        }
    }
}
