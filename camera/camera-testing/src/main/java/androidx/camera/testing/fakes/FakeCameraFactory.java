/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.testing.fakes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraInternal;
import androidx.core.util.Pair;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A {@link CameraFactory} implementation that contains and produces fake cameras.
 *
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(Scope.LIBRARY_GROUP)
public final class FakeCameraFactory implements CameraFactory {

    private static final String TAG = "FakeCameraFactory";

    @Nullable
    private Set<String> mCachedCameraIds;

    @Nullable
    private final CameraSelector mAvailableCamerasSelector;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Map<String, Pair<Integer, Callable<CameraInternal>>> mCameraMap = new HashMap<>();

    public FakeCameraFactory() {
        mAvailableCamerasSelector = null;
    }

    public FakeCameraFactory(@Nullable CameraSelector availableCamerasSelector) {
        mAvailableCamerasSelector = availableCamerasSelector;
    }

    @Override
    @NonNull
    public CameraInternal getCamera(@NonNull String cameraId) {
        Pair<Integer, Callable<CameraInternal>> cameraPair = mCameraMap.get(cameraId);
        if (cameraPair != null) {
            try {
                Callable<CameraInternal> cameraCallable = Preconditions.checkNotNull(
                        cameraPair.second);
                return cameraCallable.call();
            } catch (Exception e) {
                throw new RuntimeException("Unable to create camera.", e);
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

    @Override
    @NonNull
    public Set<String> getAvailableCameraIds() {
        // Lazily cache the set of all camera ids. This cache will be invalidated anytime a new
        // camera is added.
        if (mCachedCameraIds == null) {
            if (mAvailableCamerasSelector == null) {
                mCachedCameraIds = Collections.unmodifiableSet(new HashSet<>(mCameraMap.keySet()));
            } else {
                mCachedCameraIds = Collections.unmodifiableSet(new HashSet<>(filteredCameraIds()));
            }
        }
        return mCachedCameraIds;
    }

    /** Returns a list of camera ids filtered with {@link #mAvailableCamerasSelector}. */
    @NonNull
    private List<String> filteredCameraIds() {
        Preconditions.checkNotNull(mAvailableCamerasSelector);
        final List<String> filteredCameraIds = new ArrayList<>();
        for (Map.Entry<String, Pair<Integer, Callable<CameraInternal>>> entry :
                mCameraMap.entrySet()) {
            final Callable<CameraInternal> callable = entry.getValue().second;
            if (callable == null) {
                continue;
            }
            try {
                final CameraInternal camera = callable.call();
                try {
                    LinkedHashSet<CameraInternal> filteredCameraInternals =
                            mAvailableCamerasSelector.filter(
                                    new LinkedHashSet<>(Collections.singleton(camera)));
                    if (!filteredCameraInternals.isEmpty()) {
                        filteredCameraIds.add(entry.getKey());
                    }
                } catch (IllegalArgumentException exception) {
                    // No op. The camera was not selected by the selector
                }
            } catch (Exception exception) {
                Logger.e(TAG, "Failed to get access to the camera instance.", exception);
            }
        }
        return filteredCameraIds;
    }

    @Nullable
    @Override
    public Object getCameraManager() {
        return null;
    }
}
