/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes;

import static androidx.camera.core.CameraUnavailableException.CAMERA_ERROR;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraIdentifier;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.Logger;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Observable;
import androidx.core.util.Pair;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * A {@link CameraFactory} implementation that contains and produces fake cameras.
 *
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class FakeCameraFactory implements CameraFactory, CameraFactory.Interrogator {

    private static final String TAG = "FakeCameraFactory";

    private @Nullable Set<String> mCachedCameraIds;

    private final @Nullable CameraSelector mAvailableCamerasSelector;

    private @Nullable Object mCameraManager = null;

    private @NonNull CameraCoordinator mCameraCoordinator = new FakeCameraCoordinator();

    private @NonNull Observable<List<CameraIdentifier>> mCameraSourceObservable =
            new ControllableObservable();

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Map<String, Pair<Integer, Callable<CameraInternal>>> mCameraMap = new HashMap<>();

    private boolean mShouldThrowOnInterrogate = false;

    public FakeCameraFactory() {
        mAvailableCamerasSelector = null;
    }

    public FakeCameraFactory(@Nullable CameraSelector availableCamerasSelector) {
        mAvailableCamerasSelector = availableCamerasSelector;

        updateCameraPresence();
    }

    @Override
    public @NonNull CameraInternal getCamera(@NonNull String cameraId)
            throws CameraUnavailableException {
        Pair<Integer, Callable<CameraInternal>> cameraPair = mCameraMap.get(cameraId);
        if (cameraPair != null) {
            try {
                Callable<CameraInternal> cameraCallable = Preconditions.checkNotNull(
                        cameraPair.second);
                return cameraCallable.call();
            } catch (Throwable t) {
                throw new CameraUnavailableException(CAMERA_ERROR, t);
            }
        }
        throw new IllegalArgumentException("Unknown camera: " + cameraId);
    }

    /**
     * Inserts a {@link Callable} for creating cameras with the given camera ID.
     *
     * @param cameraId       Identifier to use for the camera.
     * @param cameraInternal Callable used to provide the Camera implementation.
     */
    public void insertCamera(@CameraSelector.LensFacing int lensFacing, @NonNull String cameraId,
            @NonNull Callable<CameraInternal> cameraInternal) {
        // Invalidate caches
        mCachedCameraIds = null;

        mCameraMap.put(cameraId, Pair.create(lensFacing, cameraInternal));

        updateCameraPresence();
    }

    /**
     * Inserts a camera and sets it as the default front camera.
     *
     * <p>This is a convenience method for calling
     * {@link #insertCamera(int, String, Callable)} with
     * {@link CameraSelector#LENS_FACING_FRONT} for all lens facing arguments.
     *
     * @param cameraId       Identifier to use for the front camera.
     * @param cameraInternal Camera implementation.
     */
    public void insertDefaultFrontCamera(@NonNull String cameraId,
            @NonNull Callable<CameraInternal> cameraInternal) {
        insertCamera(CameraSelector.LENS_FACING_FRONT, cameraId, cameraInternal);
    }

    /**
     * Inserts a camera and sets it as the default back camera.
     *
     * <p>This is a convenience method for calling
     * {@link #insertCamera(int, String, Callable)} with
     * {@link CameraSelector#LENS_FACING_BACK} for all lens facing arguments.
     *
     * @param cameraId       Identifier to use for the back camera.
     * @param cameraInternal Camera implementation.
     */
    public void insertDefaultBackCamera(@NonNull String cameraId,
            @NonNull Callable<CameraInternal> cameraInternal) {
        insertCamera(CameraSelector.LENS_FACING_BACK, cameraId, cameraInternal);
    }

    /**
     * Removes a camera with the given camera ID.
     *
     * <p>Subsequent calls to {@link #getAvailableCameraIds()} will no longer include this camera,
     * and {@link #getCamera(String)} will throw an {@link IllegalArgumentException} for it.
     *
     * @param cameraId Identifier of the camera to remove.
     * @return The {@link Callable} that was associated with the removed camera, or {@code null}
     * if the camera was not found.
     */
    public @Nullable Callable<CameraInternal> removeCamera(@NonNull String cameraId) {
        // Invalidate caches
        mCachedCameraIds = null;

        // Remove from the map and return the old value.
        Pair<Integer, Callable<CameraInternal>> removed = mCameraMap.remove(cameraId);

        updateCameraPresence();

        if (removed != null) {
            return removed.second;
        }
        return null; // Not found
    }

    @Override
    public @NonNull Set<String> getAvailableCameraIds() {
        // Lazily cache the set of all camera ids. This cache will be invalidated anytime a new
        // camera is added.
        if (mCachedCameraIds == null) {
            if (mAvailableCamerasSelector == null) {
                mCachedCameraIds = Collections.unmodifiableSet(new HashSet<>(mCameraMap.keySet()));
            } else {
                mCachedCameraIds = Collections.unmodifiableSet(
                        new HashSet<>(filterCameraIds(mCameraMap.keySet())));
            }
        }
        return mCachedCameraIds;
    }

    public void setShouldThrowOnInterrogate(boolean shouldThrow) {
        mShouldThrowOnInterrogate = shouldThrow;
    }

    @NonNull
    @Override
    public List<String> getAvailableCameraIds(@NonNull List<String> cameraIds) {
        if (mShouldThrowOnInterrogate) {
            // Reset the flag after use to avoid affecting subsequent tests.
            mShouldThrowOnInterrogate = false;
            throw new IllegalStateException("Test Exception from Interrogator");
        }

        if (mAvailableCamerasSelector == null) {
            // No selector, just return the input list but ensure cameras exist in our map.
            List<String> existingIds = new ArrayList<>();
            for (String cameraId : cameraIds) {
                if (mCameraMap.containsKey(cameraId)) {
                    existingIds.add(cameraId);
                }
            }
            return existingIds;
        }
        return filterCameraIds(cameraIds);
    }

    /**
     * A private helper to apply the CameraSelector filter to any list of camera IDs.
     * This is used by both getAvailableCameraIds() and the new Interrogator method.
     */
    private @NonNull List<String> filterCameraIds(@NonNull Iterable<String> cameraIds) {
        Preconditions.checkNotNull(mAvailableCamerasSelector);
        final List<String> filteredCameraIds = new ArrayList<>();
        for (String cameraId : cameraIds) {
            if (!mCameraMap.containsKey(cameraId)) {
                continue;
            }
            final Callable<CameraInternal> callable =
                    Objects.requireNonNull(mCameraMap.get(cameraId)).second;
            if (callable == null) {
                continue;
            }
            try {
                final CameraInternal camera = callable.call();
                LinkedHashSet<CameraInternal> filteredCameraInternals =
                        mAvailableCamerasSelector.filter(
                                new LinkedHashSet<>(Collections.singleton(camera)));
                if (!filteredCameraInternals.isEmpty()) {
                    filteredCameraIds.add(cameraId);
                }
            } catch (Exception exception) {
                Logger.e(TAG, "Failed to get access to the camera instance.", exception);
            }
        }
        return filteredCameraIds;
    }

    @Override
    public @NonNull CameraCoordinator getCameraCoordinator() {
        return mCameraCoordinator;
    }

    public void setCameraCoordinator(@NonNull CameraCoordinator cameraCoordinator) {
        mCameraCoordinator = cameraCoordinator;
    }

    public void setCameraManager(@Nullable Object cameraManager) {
        mCameraManager = cameraManager;
    }

    @Override
    public @Nullable Object getCameraManager() {
        return mCameraManager;
    }

    @Override
    public @NonNull Observable<List<CameraIdentifier>> getCameraPresenceSource() {
        return mCameraSourceObservable;
    }

    public void setCameraPresenceSource(
            @NonNull Observable<List<CameraIdentifier>> cameraSourceObservable) {
        mCameraSourceObservable = cameraSourceObservable;
    }

    @Override
    public void onCameraIdsUpdated(@NonNull List<String> cameraIds) {

    }

    /**
     * A new private helper to push updates to the camera presence observable.
     */
    private void updateCameraPresence() {
        Set<String> availableIds = getAvailableCameraIds();
        List<CameraIdentifier> identifiers = new ArrayList<>();
        for (String id : availableIds) {
            identifiers.add(CameraIdentifier.Factory.create(id));
        }

        // This check is needed because setCameraPresenceSource can overwrite our observable.
        if (mCameraSourceObservable instanceof ControllableObservable) {
            ((ControllableObservable) mCameraSourceObservable).updateData(identifiers);
        }
    }

    /**
     * A simple observable implementation that allows internal updates.
     */
    private static class ControllableObservable implements Observable<List<CameraIdentifier>> {
        private Observer<? super List<CameraIdentifier>> mObserver;
        private Executor mExecutor;
        private List<CameraIdentifier> mData = new ArrayList<>();

        @Override
        public void addObserver(@NonNull Executor executor,
                @NonNull Observer<? super List<CameraIdentifier>> observer) {
            mExecutor = executor;
            mObserver = observer;
            updateData(mData);
        }

        @Override
        public void removeObserver(@NonNull Observer<? super List<CameraIdentifier>> observer) {
            if (Objects.equals(mObserver, observer)) {
                mObserver = null;
                mExecutor = null;
            }
        }

        @Override
        public @NonNull ListenableFuture<List<CameraIdentifier>> fetchData() {
            return Futures.immediateFuture(mData);
        }

        void updateData(@NonNull List<CameraIdentifier> data) {
            mData = data;
            if (mExecutor != null && mObserver != null) {
                Observer<? super List<CameraIdentifier>> observer = mObserver;
                mExecutor.execute(() -> observer.onNewData(data));
            }
        }
    }
}
