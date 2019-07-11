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

package androidx.camera.core;

import android.content.Context;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A collection of {@link BaseCamera} instances.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class CameraRepository implements UseCaseGroup.StateChangeListener {
    private static final String TAG = "CameraRepository";

    private final Object mCamerasLock = new Object();

    @GuardedBy("mCamerasLock")
    private final Map<String, BaseCamera> mCameras = new HashMap<>();

    /**
     * Initializes the repository from a {@link Context}.
     *
     * <p>All cameras queried from the {@link CameraFactory} will be added to the repository.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void init(CameraFactory cameraFactory) {
        synchronized (mCamerasLock) {
            try {
                Set<String> camerasList = cameraFactory.getAvailableCameraIds();
                for (String id : camerasList) {
                    Log.d(TAG, "Added camera: " + id);
                    mCameras.put(id, cameraFactory.getCamera(id));
                }
            } catch (Exception e) {
                throw new IllegalStateException("Unable to enumerate cameras", e);
            }
        }
    }

    /**
     * Gets a {@link BaseCamera} for the given id.
     *
     * @param cameraId id for the camera
     * @return a {@link BaseCamera} paired to this id
     * @throws IllegalArgumentException if there is no camera paired with the id
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public BaseCamera getCamera(String cameraId) {
        synchronized (mCamerasLock) {
            BaseCamera camera = mCameras.get(cameraId);

            if (camera == null) {
                throw new IllegalArgumentException("Invalid camera: " + cameraId);
            }

            return camera;
        }
    }

    /**
     * Gets the set of all camera ids.
     *
     * @return set of all camera ids
     */
    Set<String> getCameraIds() {
        synchronized (mCamerasLock) {
            return Collections.unmodifiableSet(mCameras.keySet());
        }
    }

    /**
     * Attaches all the use cases in the {@link UseCaseGroup} and opens all the associated cameras.
     *
     * <p>This will start streaming data to the uses cases which are also online.
     */
    @Override
    public void onGroupActive(UseCaseGroup useCaseGroup) {
        synchronized (mCamerasLock) {
            Map<String, Set<UseCase>> cameraIdToUseCaseMap = useCaseGroup.getCameraIdToUseCaseMap();
            for (Map.Entry<String, Set<UseCase>> cameraUseCaseEntry :
                    cameraIdToUseCaseMap.entrySet()) {
                BaseCamera camera = getCamera(cameraUseCaseEntry.getKey());
                attachUseCasesToCamera(camera, cameraUseCaseEntry.getValue());
            }
        }
    }

    /** Attaches a set of use cases to a camera. */
    @GuardedBy("mCamerasLock")
    private void attachUseCasesToCamera(BaseCamera camera, Set<UseCase> useCases) {
        camera.addOnlineUseCase(useCases);
    }

    /**
     * Detaches all the use cases in the {@link UseCaseGroup} and closes the camera with no attached
     * use cases.
     */
    @Override
    public void onGroupInactive(UseCaseGroup useCaseGroup) {
        synchronized (mCamerasLock) {
            Map<String, Set<UseCase>> cameraIdToUseCaseMap = useCaseGroup.getCameraIdToUseCaseMap();
            for (Map.Entry<String, Set<UseCase>> cameraUseCaseEntry :
                    cameraIdToUseCaseMap.entrySet()) {
                BaseCamera camera = getCamera(cameraUseCaseEntry.getKey());
                detachUseCasesFromCamera(camera, cameraUseCaseEntry.getValue());
            }
        }
    }

    /** Detaches a set of use cases from a camera. */
    @GuardedBy("mCamerasLock")
    private void detachUseCasesFromCamera(BaseCamera camera, Set<UseCase> useCases) {
        camera.removeOnlineUseCase(useCases);
    }
}
