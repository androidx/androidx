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

package androidx.camera.camera2.internal.compat;

import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.core.impl.utils.MainThreadAsyncHandler;

import java.util.concurrent.Executor;

/**
 * Helper for accessing features in {@link CameraDevice} in a backwards compatible fashion.
 */
@RequiresApi(21)
public final class CameraDeviceCompat {

    /**
     * Standard camera operation mode.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static final int SESSION_OPERATION_MODE_NORMAL =
            0; // ICameraDeviceUser.NORMAL_MODE;
    /**
     * Constrained high-speed operation mode.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static final int SESSION_OPERATION_MODE_CONSTRAINED_HIGH_SPEED =
            1; // ICameraDeviceUser.CONSTRAINED_HIGH_SPEED_MODE;

    private final CameraDeviceCompatImpl mImpl;

    // Class is not a wrapper. Should not be instantiated.
    private CameraDeviceCompat(@NonNull CameraDevice cameraDevice, @NonNull Handler compatHandler) {
        if (Build.VERSION.SDK_INT >= 28) {
            mImpl = new CameraDeviceCompatApi28Impl(cameraDevice);
        } else if (Build.VERSION.SDK_INT >= 24) {
            mImpl = CameraDeviceCompatApi24Impl.create(cameraDevice, compatHandler);
        } else if (Build.VERSION.SDK_INT >= 23) {
            mImpl = CameraDeviceCompatApi23Impl.create(cameraDevice, compatHandler);
        } else {
            mImpl = CameraDeviceCompatBaseImpl.create(cameraDevice, compatHandler);
        }
    }

    /**
     * Provides a backward-compatible wrapper for {@link CameraDevice}.
     *
     * <p>All APIs making use of an  {@link Executor} will use the main thread to
     * dispatch to that executor. Callers wanting to avoid using the main thread for dispatching
     * should use {@link #toCameraDeviceCompat(CameraDevice, Handler)}.
     *
     * @param captureSession {@link CameraDevice} class to wrap
     * @return wrapped class
     * @see #toCameraDeviceCompat(CameraDevice, Handler)
     */
    @NonNull
    public static CameraDeviceCompat toCameraDeviceCompat(
            @NonNull CameraDevice captureSession) {
        return CameraDeviceCompat.toCameraDeviceCompat(captureSession,
                MainThreadAsyncHandler.getInstance());
    }

    /**
     * Provides a backward-compatible wrapper for {@link CameraDevice}.
     *
     * <p>All APIs making use of an {@link Executor} as an argument will use the provided
     * {@link Handler} to dispatch callbacks on the executor.
     *
     * @param cameraDevice {@link CameraDevice} class to wrap
     * @param compatHandler {@link Handler} used for dispatching callbacks to executor APIs.
     * @return wrapped class
     */
    @NonNull
    public static CameraDeviceCompat toCameraDeviceCompat(
            @NonNull CameraDevice cameraDevice, @NonNull Handler compatHandler) {
        return new CameraDeviceCompat(cameraDevice, compatHandler);
    }

    /**
     * Provides the platform class object represented by this object.
     *
     * @return platform class object
     * @see #toCameraDeviceCompat(CameraDevice)
     * @see #toCameraDeviceCompat(CameraDevice, Handler)
     */
    @NonNull
    public CameraDevice toCameraDevice() {
        return mImpl.unwrap();
    }

    /**
     * Create a new {@link CameraDevice} using a {@link SessionConfigurationCompat}
     * helper object that aggregates all supported parameters.
     *
     * @param config A session configuration (see {@link SessionConfigurationCompat}).
     * @throws IllegalArgumentException    In case the session configuration
     *                                     is invalid; or the output configurations are empty; or
     *                                     the session configuration executor is invalid.
     * @throws CameraAccessExceptionCompat In case the camera device is no longer connected or
     *                                     has encountered a fatal error.
     */
    public void createCaptureSession(@NonNull SessionConfigurationCompat config)
            throws CameraAccessExceptionCompat {
        mImpl.createCaptureSession(config);
    }

    interface CameraDeviceCompatImpl {
        void createCaptureSession(@NonNull SessionConfigurationCompat config)
                throws CameraAccessExceptionCompat;

        @NonNull
        CameraDevice unwrap();
    }

    @RequiresApi(21)
    static final class StateCallbackExecutorWrapper extends CameraDevice.StateCallback {

        final CameraDevice.StateCallback mWrappedCallback;
        private final Executor mExecutor;

        StateCallbackExecutorWrapper(@NonNull Executor executor,
                @NonNull CameraDevice.StateCallback wrappedCallback) {
            mExecutor = executor;
            mWrappedCallback = wrappedCallback;
        }

        @Override
        public void onOpened(@NonNull final CameraDevice camera) {
            mExecutor.execute(() -> mWrappedCallback.onOpened(camera));
        }

        @Override
        public void onDisconnected(@NonNull final CameraDevice camera) {
            mExecutor.execute(() -> mWrappedCallback.onDisconnected(camera));
        }

        @Override
        public void onError(@NonNull final CameraDevice camera, final int error) {
            mExecutor.execute(() -> mWrappedCallback.onError(camera, error));
        }

        @Override
        public void onClosed(@NonNull final CameraDevice camera) {
            mExecutor.execute(() -> mWrappedCallback.onClosed(camera));
        }
    }

}
