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

package androidx.camera.core.impl;

import android.content.Context;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.InitializationException;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A collection of {@link CameraInternal} instances.
 */
public final class CameraRepository implements UseCaseMediator.StateChangeCallback {
    private static final String TAG = "CameraRepository";

    private final Object mCamerasLock = new Object();

    @GuardedBy("mCamerasLock")
    private final Map<String, CameraInternal> mCameras = new LinkedHashMap<>();
    @GuardedBy("mCamerasLock")
    private final Set<CameraInternal> mReleasingCameras = new HashSet<>();
    @GuardedBy("mCamerasLock")
    private ListenableFuture<Void> mDeinitFuture;
    @GuardedBy("mCamerasLock")
    private CallbackToFutureAdapter.Completer<Void> mDeinitCompleter;

    /**
     * Initializes the repository from a {@link Context}.
     *
     * <p>All cameras queried from the {@link CameraFactory} will be added to the repository.
     */
    public void init(@NonNull CameraFactory cameraFactory) throws InitializationException {
        synchronized (mCamerasLock) {
            try {
                Set<String> camerasList = cameraFactory.getAvailableCameraIds();
                for (String id : camerasList) {
                    Log.d(TAG, "Added camera: " + id);
                    mCameras.put(id, cameraFactory.getCamera(id));
                }
            } catch (CameraUnavailableException e) {
                throw new InitializationException(e);
            }
        }
    }

    /**
     * Clear and release all cameras from the repository.
     */
    @NonNull
    public ListenableFuture<Void> deinit() {
        synchronized (mCamerasLock) {
            // If the camera list is empty, we can either return the current deinit future that
            // has not yet completed, or an immediate successful future if we are already
            // completely deinitialized.
            if (mCameras.isEmpty()) {
                return mDeinitFuture == null ? Futures.immediateFuture(null) : mDeinitFuture;
            }

            ListenableFuture<Void> currentFuture = mDeinitFuture;
            if (currentFuture == null) {
                // Create a single future that will be used to track closing of all cameras.
                // Once all cameras have been released, this future will complete. This future
                // will stay active until all cameras in mReleasingCameras has completed, even if
                // CameraRepository is initialized and deinitialized multiple times in quick
                // succession.
                currentFuture = CallbackToFutureAdapter.getFuture((completer) -> {
                    synchronized (mCamerasLock) {
                        mDeinitCompleter = completer;
                    }
                    return "CameraRepository-deinit";
                });
                mDeinitFuture = currentFuture;
            }

            // Ensure all of the cameras have been added here before we start releasing so that
            // if the first camera release finishes inline it won't complete the future prematurely.
            mReleasingCameras.addAll(mCameras.values());
            for (final CameraInternal cameraInternal : mCameras.values()) {
                // Release the camera and wait for it to complete. We keep track of which cameras
                // are still releasing with mReleasingCameras.
                cameraInternal.release().addListener(() -> {
                    synchronized (mCamerasLock) {
                        // When the camera has completed releasing, we can now remove it from
                        // mReleasingCameras. Any time a camera finishes releasing, we need to
                        // check if all cameras a finished so we can finish the future which is
                        // waiting for all cameras to release.
                        mReleasingCameras.remove(cameraInternal);
                        if (mReleasingCameras.isEmpty()) {
                            Preconditions.checkNotNull(mDeinitCompleter);
                            // Every camera has been released. Signal successful completion of
                            // deinit().
                            mDeinitCompleter.set(null);
                            mDeinitCompleter = null;
                            mDeinitFuture = null;
                        }
                    }
                }, CameraXExecutors.directExecutor());
            }

            // Ensure all cameras are removed from the active "mCameras" map. This map can be
            // repopulated by #init().
            mCameras.clear();

            return currentFuture;
        }
    }

    /**
     * Gets a {@link CameraInternal} for the given id.
     *
     * @param cameraId id for the camera
     * @return a {@link CameraInternal} paired to this id
     * @throws IllegalArgumentException if there is no camera paired with the id
     */
    @NonNull
    public CameraInternal getCamera(@NonNull String cameraId) {
        synchronized (mCamerasLock) {
            CameraInternal cameraInternal = mCameras.get(cameraId);

            if (cameraInternal == null) {
                throw new IllegalArgumentException("Invalid camera: " + cameraId);
            }

            return cameraInternal;
        }
    }

    /**
     * Gets the set of all cameras.
     *
     * @return set of all cameras
     */
    @NonNull
    public Set<CameraInternal> getCameras() {
        synchronized (mCamerasLock) {
            return new LinkedHashSet<>(mCameras.values());
        }
    }

    /**
     * Gets the set of all camera ids.
     *
     * @return set of all camera ids
     */
    @NonNull
    Set<String> getCameraIds() {
        synchronized (mCamerasLock) {
            return new LinkedHashSet<>(mCameras.keySet());
        }
    }

    /**
     * Attaches all the use cases in the {@link UseCaseMediator} and opens all the associated
     * cameras.
     *
     * <p>This will start streaming data to the uses cases which are also online.
     */
    @Override
    public void onActive(@NonNull UseCaseMediator useCaseMediator) {
        synchronized (mCamerasLock) {
            Map<String, Set<UseCase>> cameraIdToUseCaseMap =
                    useCaseMediator.getCameraIdToUseCaseMap();
            for (Map.Entry<String, Set<UseCase>> cameraUseCaseEntry :
                    cameraIdToUseCaseMap.entrySet()) {
                CameraInternal cameraInternal = getCamera(cameraUseCaseEntry.getKey());
                attachUseCasesToCamera(cameraInternal, cameraUseCaseEntry.getValue());
            }
        }
    }

    /** Attaches a set of use cases to a camera. */
    @GuardedBy("mCamerasLock")
    private void attachUseCasesToCamera(CameraInternal cameraInternal, Set<UseCase> useCases) {
        cameraInternal.attachUseCases(useCases);
    }

    /**
     * Detaches all the use cases in the {@link UseCaseMediator} and closes the camera with no
     * attached
     * use cases.
     */
    @Override
    public void onInactive(@NonNull UseCaseMediator useCaseMediator) {
        synchronized (mCamerasLock) {
            Map<String, Set<UseCase>> cameraIdToUseCaseMap =
                    useCaseMediator.getCameraIdToUseCaseMap();
            for (Map.Entry<String, Set<UseCase>> cameraUseCaseEntry :
                    cameraIdToUseCaseMap.entrySet()) {
                CameraInternal cameraInternal = getCamera(cameraUseCaseEntry.getKey());
                detachUseCasesFromCamera(cameraInternal, cameraUseCaseEntry.getValue());
            }
        }
    }

    /** Detaches a set of use cases from a camera. */
    @GuardedBy("mCamerasLock")
    private void detachUseCasesFromCamera(CameraInternal cameraInternal, Set<UseCase> useCases) {
        cameraInternal.detachUseCases(useCases);
    }
}
