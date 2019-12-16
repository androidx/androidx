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

package androidx.camera.camera2.internal;

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraInternal;
import androidx.camera.core.Observable;
import androidx.camera.core.impl.LiveDataObservable;
import androidx.core.util.Preconditions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A registry that tracks the state of cameras and publishes the number of cameras available to
 * open.
 */
final class CameraAvailabilityRegistry {
    private static final boolean DEBUG = false;
    private StringBuilder mDebugString = DEBUG ? new StringBuilder() : null;
    private static final String TAG = "AvailabilityRegistry";

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final int mMaxAllowedOpenedCameras;
    private final Executor mExecutor;
    private final LiveDataObservable<Integer> mAvailableCameras;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final Map<CameraInternal, CameraInternal.State> mCameraStates = new HashMap<>();


    /**
     * Creates a new registry with a limit of {@code maxAllowedOpenCameras} allowed to be opened.
     *
     * @param maxAllowedOpenedCameras The limit of number of simultaneous open cameras.
     * @param executor                An executor used for state callbacks on
     */
    CameraAvailabilityRegistry(int maxAllowedOpenedCameras, @NonNull Executor executor) {
        mMaxAllowedOpenedCameras = maxAllowedOpenedCameras;
        mExecutor = Preconditions.checkNotNull(executor);
        mAvailableCameras = new LiveDataObservable<>();
        mAvailableCameras.postValue(maxAllowedOpenedCameras);
    }

    /**
     * Registers a camera with the registry.
     *
     * <p>Once registered, the state will be tracked until the camera is released. Once released,
     * the camera will be automatically unregistered.
     *
     * @param cameraInternal The camera to register.
     */
    void registerCamera(@NonNull final CameraInternal cameraInternal) {
        synchronized (mLock) {
            if (!mCameraStates.containsKey(cameraInternal)) {
                mCameraStates.put(cameraInternal, null);

                cameraInternal.getCameraState().addObserver(mExecutor,
                        new Observable.Observer<CameraInternal.State>() {
                            @Override
                            public void onNewData(@Nullable CameraInternal.State state) {
                                if (state == CameraInternal.State.RELEASED) {
                                    unregisterCamera(cameraInternal, this);
                                } else {
                                    updateState(cameraInternal, state);
                                }
                            }

                            @Override
                            public void onError(@NonNull Throwable t) {
                                // Ignore errors on state for now. Handle these in the future if
                                // needed.
                            }
                        });
            }
        }
    }

    /**
     * Returns an observable stream of the current available camera count.
     *
     * <p>This count is a best effort count of cameras available to be opened on this device.
     * This should only be used as a hint for when cameras can be opened. Due to the asynchronous
     * nature of notifications and when the camera device is opened, users should still expect
     * that attempting to open cameras may fail and should handle errors appropriately.
     */
    Observable<Integer> getAvailableCameraCount() {
        return mAvailableCameras;
    }

    @WorkerThread
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void unregisterCamera(CameraInternal cameraInternal,
            Observable.Observer<CameraInternal.State> observer) {
        int availableCameras;
        synchronized (mLock) {
            cameraInternal.getCameraState().removeObserver(observer);
            if (mCameraStates.remove(cameraInternal) == null) {
                return;
            }

            availableCameras = recalculateAvailableCameras();
        }

        mAvailableCameras.postValue(availableCameras);
    }

    @WorkerThread
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void updateState(CameraInternal cameraInternal, CameraInternal.State state) {
        int availableCameras;
        synchronized (mLock) {
            // If mCameraStates does not contain the camera, it may have been unregistered.
            // Or, if the state has not been updated, ignore this update.
            if (!mCameraStates.containsKey(cameraInternal) || mCameraStates.put(cameraInternal,
                    state) == state) {
                return;
            }

            availableCameras = recalculateAvailableCameras();
        }

        mAvailableCameras.postValue(availableCameras);
    }

    @WorkerThread
    @GuardedBy("mLock")
    private int recalculateAvailableCameras() {
        if (DEBUG) {
            mDebugString.setLength(0);
            mDebugString.append("Recalculating open cameras:\n");
            mDebugString.append(String.format(Locale.US, "%-45s%-22s\n", "Camera", "State"));
            mDebugString.append(
                    "-------------------------------------------------------------------\n");
        }
        // Count the number of cameras that are not in a closed state state. Closed states are
        // considered to be CLOSED, PENDING_OPEN or OPENING, since we can't guarantee a camera
        // has actually be open in these states. All cameras that are in a CLOSING or RELEASING
        // state may have previously been open, so we will count them as open.
        int openCount = 0;
        for (Map.Entry<CameraInternal, CameraInternal.State> entry : mCameraStates.entrySet()) {
            if (DEBUG) {
                String stateString =
                        entry.getValue() != null ? entry.getValue().toString() : "UNKNOWN";
                mDebugString.append(String.format(Locale.US, "%-45s%-22s\n",
                        entry.getKey().toString(),
                        stateString));
            }
            if (entry.getValue() != CameraInternal.State.CLOSED
                    && entry.getValue() != CameraInternal.State.OPENING
                    && entry.getValue() != CameraInternal.State.PENDING_OPEN) {
                openCount++;
            }
        }
        if (DEBUG) {
            mDebugString.append(
                    "-------------------------------------------------------------------\n");
            mDebugString.append(String.format(Locale.US, "Open count: %d (Max allowed: %d)",
                    openCount,
                    mMaxAllowedOpenedCameras));
            Log.d(TAG, mDebugString.toString());
        }

        // Calculate available cameras value (clamped to 0 or more)
        return Math.max(mMaxAllowedOpenedCameras - openCount, 0);
    }
}
