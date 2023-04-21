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

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.util.ArrayMap;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.impl.utils.MainThreadAsyncHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Helper for accessing features in {@link CameraManager} in a backwards compatible fashion.
 */
@RequiresApi(21)
public final class CameraManagerCompat {
    private final CameraManagerCompatImpl mImpl;

    @GuardedBy("mCameraCharacteristicsMap")
    private final Map<String, CameraCharacteristicsCompat> mCameraCharacteristicsMap =
            new ArrayMap<>(4);

    private CameraManagerCompat(CameraManagerCompatImpl impl) {
        mImpl = impl;
    }

    /** Get a {@link CameraManagerCompat} instance for a provided context. */
    @NonNull
    public static CameraManagerCompat from(@NonNull Context context) {
        return CameraManagerCompat.from(context, MainThreadAsyncHandler.getInstance());
    }

    /**
     * Get a {@link CameraManagerCompat} instance for a provided context, and using the provided
     * compat handler for scheduling to {@link Executor} APIs.
     *
     * @param context       Context used to retrieve the {@link CameraManager}.
     * @param compatHandler {@link Handler} used for all APIs taking an {@link Executor} argument
     *                      on lower API levels. If the API level does not support directly
     *                      executing on an Executor, it will first be posted to this handler and
     *                      the executor will be called from there.
     */
    @NonNull
    public static CameraManagerCompat from(@NonNull Context context,
            @NonNull Handler compatHandler) {
        return new CameraManagerCompat(CameraManagerCompatImpl.from(context, compatHandler));
    }

    /**
     * Get a {@link CameraManagerCompat} instance from a provided {@link CameraManagerCompatImpl}.
     *
     */
    @VisibleForTesting
    @NonNull
    public static CameraManagerCompat from(@NonNull final CameraManagerCompatImpl impl) {
        return new CameraManagerCompat(impl);
    }

    /**
     * Return the list of currently connected camera devices by identifier, including cameras that
     * may be in use by other camera API clients.
     *
     * <p>The behavior of this method matches that of {@link CameraManager#getCameraIdList()},
     * except that {@link CameraAccessExceptionCompat} is thrown in place of
     * {@link CameraAccessException} for convenience.
     *
     * @return The list of currently connected camera devices.
     */
    @NonNull
    public String[] getCameraIdList() throws CameraAccessExceptionCompat {
        return mImpl.getCameraIdList();
    }

    /**
     * Return set of set of camera ids, each set includes one combination of the camera ids that
     * could operate concurrently.
     *
     * <p>This API is added in API Level 30, any lower API version will return empty set instead.
     *
     * <p>The behavior of this method matches that of {@link CameraManager#getConcurrentCameraIds()}
     * except that {@link CameraAccessExceptionCompat} is thrown in place of
     * {@link CameraAccessException} for convenience.
     *
     * @return Set of set of camera ids.
     * @throws CameraAccessExceptionCompat
     */
    @NonNull
    public Set<Set<String>> getConcurrentCameraIds() throws CameraAccessExceptionCompat {
        return mImpl.getConcurrentCameraIds();
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
     * Returns a {@link CameraCharacteristicsCompat} associated with the given camera id.
     *
     * <p>It will return cached instance if the {@link CameraCharacteristicsCompat} has been
     * retrieved by the same camera id. If cached instance is returned, it won't throw any
     * {@link CameraAccessExceptionCompat} exception even when camera is disconnected.
     *
     * <p>The returned {@link CameraCharacteristicsCompat} will also cache the retrieved values to
     * speed up the subsequent query.
     *
     * @param cameraId The id of the camera device to query. This could be either a standalone
     *                 camera ID which can be directly opened by {@link #openCamera}, or a
     *                 physical camera ID that can only used as part of a logical multi-camera.
     * @return a {@link CameraCharacteristicsCompat} associated with the given camera id.
     * @throws IllegalArgumentException    if the cameraId does not match any known camera device.
     * @throws CameraAccessExceptionCompat if the camera device has been disconnected or the
     *                                     device is in Do Not Disturb mode with an early version
     *                                     of Android P.
     */
    @NonNull
    public CameraCharacteristicsCompat getCameraCharacteristicsCompat(@NonNull String cameraId)
            throws CameraAccessExceptionCompat {
        CameraCharacteristicsCompat characteristics;
        synchronized (mCameraCharacteristicsMap) {
            characteristics = mCameraCharacteristicsMap.get(cameraId);
            if (characteristics == null) {
                try {
                    characteristics = CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                            mImpl.getCameraCharacteristics(cameraId), cameraId);
                    mCameraCharacteristicsMap.put(cameraId, characteristics);
                } catch (AssertionError e) {
                    // Some devices may throw AssertionError when creating CameraCharacteristics
                    // and FPS ranges are null. Catch the AssertionError and throw a
                    // CameraAccessExceptionCompat to make the app be able to receive an
                    // exception to gracefully handle it.
                    throw new CameraAccessExceptionCompat(
                            CameraAccessExceptionCompat.CAMERA_CHARACTERISTICS_CREATION_ERROR,
                            e.getMessage(), e);
                }
            }
        }
        return characteristics;
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
     * @throws CameraAccessExceptionCompat if the camera is disabled by device policy, has been
     *                                     disconnected, is being used by a higher-priority
     *                                     camera API client or the device is in Do Not Disturb
     *                                     mode with an early version of Android P.
     * @throws IllegalArgumentException    if cameraId, the callback or the executor was null,
     *                                     or the cameraId does not match any currently or
     *                                     previously available camera device.
     * @throws SecurityException           if the application does not have permission to access
     *                                     the camera
     * @see CameraManager#getCameraIdList
     * @see android.app.admin.DevicePolicyManager#setCameraDisabled
     */
    @RequiresPermission(android.Manifest.permission.CAMERA)
    public void openCamera(@NonNull String cameraId,
            @NonNull /*@CallbackExecutor*/ Executor executor,
            @NonNull CameraDevice.StateCallback callback)
            throws CameraAccessExceptionCompat {
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

    /** Provides backwards compatibility to {@link CameraManager} features. */
    public interface CameraManagerCompatImpl {

        /**
         * Return the list of currently connected camera devices by identifier, including cameras
         * that may be in use by other camera API clients.
         */
        @NonNull
        String[] getCameraIdList() throws CameraAccessExceptionCompat;

        /**
         * Return the set of concurrent camera id set which could operate concurrently.
         */
        @NonNull
        Set<Set<String>> getConcurrentCameraIds() throws CameraAccessExceptionCompat;

        void registerAvailabilityCallback(
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraManager.AvailabilityCallback callback);

        void unregisterAvailabilityCallback(@NonNull CameraManager.AvailabilityCallback callback);

        @NonNull
        CameraCharacteristics getCameraCharacteristics(@NonNull String cameraId)
                throws CameraAccessExceptionCompat;

        @RequiresPermission(android.Manifest.permission.CAMERA)
        void openCamera(@NonNull String cameraId,
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraDevice.StateCallback callback)
                throws CameraAccessExceptionCompat;

        @NonNull
        CameraManager getCameraManager();

        /**
         * Returns a {@link CameraManagerCompatImpl} instance depending on the API level
         *
         * @param context       Context used to retrieve the {@link CameraManager}.
         * @param compatHandler {@link Handler} used for all APIs taking an {@link Executor}
         *                      argument on lower API levels. If the API level does not support
         *                      directly executing on an Executor, it will first be posted to
         *                      this handler and the executor will be called from there.
         */
        @NonNull
        static CameraManagerCompatImpl from(@NonNull Context context,
                @NonNull Handler compatHandler) {
            if (Build.VERSION.SDK_INT >= 30) {
                return new CameraManagerCompatApi30Impl(context);
            } else if (Build.VERSION.SDK_INT >= 29) {
                return new CameraManagerCompatApi29Impl(context);
            } else if (Build.VERSION.SDK_INT >= 28) {
                // Can use Executor directly on API 28+
                return CameraManagerCompatApi28Impl.create(context);
            }
            // Pass compat handler to implementation.
            return CameraManagerCompatBaseImpl.create(context, compatHandler);
        }
    }

    @RequiresApi(21)
    static final class AvailabilityCallbackExecutorWrapper extends
            CameraManager.AvailabilityCallback {

        private final Executor mExecutor;
        final CameraManager.AvailabilityCallback mWrappedCallback;
        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private boolean mDisabled = false;

        AvailabilityCallbackExecutorWrapper(@NonNull Executor executor,
                @NonNull CameraManager.AvailabilityCallback wrappedCallback) {
            mExecutor = executor;
            mWrappedCallback = wrappedCallback;
        }

        // Used to ensure that callbacks do not run after "unregisterAvailabilityCallback" has
        // returned. Once disabled, the wrapper can no longer be used.
        void setDisabled() {
            synchronized (mLock) {
                mDisabled = true;
            }
        }

        @RequiresApi(29)
        @Override
        public void onCameraAccessPrioritiesChanged() {
            synchronized (mLock) {
                if (!mDisabled) {
                    mExecutor.execute(() -> ApiCompat.Api29Impl.onCameraAccessPrioritiesChanged(
                            mWrappedCallback));
                }
            }
        }

        @Override
        public void onCameraAvailable(@NonNull final String cameraId) {
            synchronized (mLock) {
                if (!mDisabled) {
                    mExecutor.execute(() -> mWrappedCallback.onCameraAvailable(cameraId));
                }
            }
        }

        @Override
        public void onCameraUnavailable(@NonNull final String cameraId) {
            synchronized (mLock) {
                if (!mDisabled) {
                    mExecutor.execute(() -> mWrappedCallback.onCameraUnavailable(cameraId));
                }
            }
        }
    }
}

