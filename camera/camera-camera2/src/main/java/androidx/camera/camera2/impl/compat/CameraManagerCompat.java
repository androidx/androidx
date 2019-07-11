/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.impl.compat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import java.util.concurrent.Executor;

/**
 * Helper for accessing features in {@link CameraManager} in a backwards compatible fashion.
 */
@RequiresApi(21)
public final class CameraManagerCompat {
    private final CameraManagerCompatImpl mImpl;

    private CameraManagerCompat(CameraManagerCompatImpl impl) {
        mImpl = impl;
    }


    /** Get a {@link CameraManagerCompat} instance for a provided context. */
    public static CameraManagerCompat from(Context context) {
        if (Build.VERSION.SDK_INT >= 28) {
            return new CameraManagerCompat(new CameraManagerCompatApi28Impl(context));
        }

        return new CameraManagerCompat(new CameraManagerCompatBaseImpl(context));
    }

    /**
     * Register a callback to be notified about camera device availability.
     *
     * <p>The behavior of this method matches that of {@link
     * CameraManager#registerAvailabilityCallback(CameraManager.AvailabilityCallback, Handler)},
     * except that it uses {@link Executor} as an argument instead of {@link Handler}.
     *
     * <p>When registering an availability callback with
     * {@link #registerAvailabilityCallback(Executor, CameraManager.AvailabilityCallback)}, it
     * should always be unregistered by calling
     * {@link #unregisterAvailabilityCallback(CameraManager.AvailabilityCallback)} on <b>the same
     * instance</b> of {@link CameraManagerCompat}. Unregistering through a difference instance
     * or directly through {@link CameraManager} may fail to unregister the callback with the
     * camera service.
     *
     * @param executor The executor which will be used to invoke the callback.
     * @param callback the new callback to send camera availability notices to
     * @throws IllegalArgumentException if the executor is {@code null}.
     */
    @SuppressLint("LambdaLast") // Maybe remove after https://issuetracker.google.com/135275901
    public void registerAvailabilityCallback(
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull CameraManager.AvailabilityCallback callback) {
        mImpl.registerAvailabilityCallback(executor, callback);
    }

    /**
     * Remove a previously-added callback; the callback will no longer receive connection and
     * disconnection callbacks.
     *
     * <p>All callbacks registered through an instance of {@link CameraManagerCompat} should be
     * unregistered through <b>the same instance</b>, otherwise the callback may fail to
     * unregister with the camera service.
     *
     * <p>Removing a callback that isn't registered has no effect.</p>
     *
     * @param callback The callback to remove from the notification list
     */
    public void unregisterAvailabilityCallback(
            @NonNull CameraManager.AvailabilityCallback callback) {
        mImpl.unregisterAvailabilityCallback(callback);
    }

    /**
     * Open a connection to a camera with the given ID.
     *
     * <p>The behavior of this method matches that of
     * {@link CameraManager#openCamera(String, CameraDevice.StateCallback, Handler)}, except that
     * it uses {@link Executor} as an argument instead of {@link Handler}.
     *
     * @param cameraId The unique identifier of the camera device to open
     * @param executor The executor which will be used when invoking the callback.
     * @param callback The callback which is invoked once the camera is opened
     * @throws CameraAccessException    if the camera is disabled by device policy,
     *                                  has been disconnected, or is being used by a
     *                                  higher-priority camera API client.
     * @throws IllegalArgumentException if cameraId, the callback or the executor was null,
     *                                  or the cameraId does not match any currently or
     *                                  previously available
     *                                  camera device.
     * @throws SecurityException        if the application does not have permission to
     *                                  access the camera
     * @see CameraManager#getCameraIdList
     * @see android.app.admin.DevicePolicyManager#setCameraDisabled
     */
    @RequiresPermission(android.Manifest.permission.CAMERA)
    public void openCamera(@NonNull String cameraId,
            @NonNull /*@CallbackExecutor*/ Executor executor,
            @NonNull CameraDevice.StateCallback callback)
            throws CameraAccessException {
        mImpl.openCamera(cameraId, executor, callback);
    }

    /**
     * Gets the underlying framework {@link CameraManager} object.
     *
     * <p>This method can be used gain access to {@link CameraManager} methods not exposed by
     * {@link CameraManagerCompat}.
     */
    @NonNull
    public CameraManager unwrap() {
        return mImpl.getCameraManager();
    }

    interface CameraManagerCompatImpl {

        void registerAvailabilityCallback(
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraManager.AvailabilityCallback callback);

        void unregisterAvailabilityCallback(@NonNull CameraManager.AvailabilityCallback callback);

        @RequiresPermission(android.Manifest.permission.CAMERA)
        void openCamera(@NonNull String cameraId,
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraDevice.StateCallback callback)
                throws CameraAccessException;

        @NonNull
        CameraManager getCameraManager();
    }

    static final class AvailabilityCallbackExecutorWrapper extends
            CameraManager.AvailabilityCallback {

        private final Executor mExecutor;
        final CameraManager.AvailabilityCallback mWrappedCallback;

        AvailabilityCallbackExecutorWrapper(@NonNull Executor executor,
                @NonNull CameraManager.AvailabilityCallback wrappedCallback) {
            mExecutor = executor;
            mWrappedCallback = wrappedCallback;
        }

        @RequiresApi(29)
        public void onCameraAccessPrioritiesChanged() {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mWrappedCallback.onCameraAccessPrioritiesChanged();
                }
            });
        }

        public void onCameraAvailable(@NonNull final String cameraId) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mWrappedCallback.onCameraAvailable(cameraId);
                }
            });
        }


        public void onCameraUnavailable(@NonNull final String cameraId) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mWrappedCallback.onCameraUnavailable(cameraId);
                }
            });
        }
    }
}

