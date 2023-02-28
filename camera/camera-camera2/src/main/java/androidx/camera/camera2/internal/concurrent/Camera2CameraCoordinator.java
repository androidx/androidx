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

package androidx.camera.camera2.internal.concurrent;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Logger;
import androidx.camera.core.concurrent.CameraCoordinator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation for {@link CameraCoordinator}.
 */
@RequiresApi(21)
public class Camera2CameraCoordinator implements CameraCoordinator {

    private static final String TAG = "Camera2CameraCoordinator";

    @NonNull private final CameraManagerCompat mCameraManager;
    @NonNull private final List<ConcurrentCameraModeListener> mConcurrentCameraModeListeners;
    @NonNull private final Map<String, String> mConcurrentCameraIdMap;
    @NonNull private List<CameraSelector> mActiveConcurrentCameraSelectors;
    @NonNull private Set<Set<String>> mConcurrentCameraIds;

    @CameraOperatingMode private int mCameraOperatingMode = CAMERA_OPERATING_MODE_UNSPECIFIED;

    public Camera2CameraCoordinator(@NonNull CameraManagerCompat cameraManager) {
        mCameraManager = cameraManager;
        mConcurrentCameraIdMap = new HashMap<>();
        mConcurrentCameraIds = new HashSet<>();
        mConcurrentCameraModeListeners = new ArrayList<>();
        mActiveConcurrentCameraSelectors = new ArrayList<>();
        retrieveConcurrentCameraIds();
    }

    @NonNull
    @Override
    public List<List<CameraSelector>> getConcurrentCameraSelectors() {
        List<List<CameraSelector>> concurrentCameraSelectorLists = new ArrayList<>();
        for (Set<String> concurrentCameraIdList: mConcurrentCameraIds) {
            List<CameraSelector> cameraSelectors = new ArrayList<>();
            for (String concurrentCameraId : concurrentCameraIdList) {
                cameraSelectors.add(createCameraSelectorById(mCameraManager, concurrentCameraId));
            }
            concurrentCameraSelectorLists.add(cameraSelectors);
        }
        return concurrentCameraSelectorLists;
    }

    @NonNull
    @Override
    public List<CameraSelector> getActiveConcurrentCameraSelectors() {
        return mActiveConcurrentCameraSelectors;
    }

    @Override
    public void setActiveConcurrentCameraSelectors(@NonNull List<CameraSelector> cameraSelectors) {
        mActiveConcurrentCameraSelectors = cameraSelectors;
    }

    @Nullable
    @Override
    public String getPairedConcurrentCameraId(@NonNull String cameraId) {
        if (mConcurrentCameraIdMap.containsKey(cameraId)) {
            return mConcurrentCameraIdMap.get(cameraId);
        }
        return null;
    }

    @CameraOperatingMode
    @Override
    public int getCameraOperatingMode() {
        return mCameraOperatingMode;
    }

    @Override
    public void setCameraOperatingMode(@CameraOperatingMode int cameraOperatingMode) {
        if (cameraOperatingMode != mCameraOperatingMode) {
            for (ConcurrentCameraModeListener listener : mConcurrentCameraModeListeners) {
                listener.notifyConcurrentCameraModeUpdated(cameraOperatingMode);
            }
        }
        // Clear the cached camera selectors if concurrent mode is off.
        if (mCameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT
                && cameraOperatingMode != CAMERA_OPERATING_MODE_CONCURRENT) {
            mActiveConcurrentCameraSelectors.clear();
        }
        mCameraOperatingMode = cameraOperatingMode;
    }

    @Override
    public void addListener(@NonNull ConcurrentCameraModeListener listener) {
        mConcurrentCameraModeListeners.add(listener);
    }

    @Override
    public void removeListener(@NonNull ConcurrentCameraModeListener listener) {
        mConcurrentCameraModeListeners.remove(listener);
    }

    private void retrieveConcurrentCameraIds() {
        try {
            mConcurrentCameraIds = mCameraManager.getConcurrentCameraIds();
        } catch (CameraAccessExceptionCompat e) {
            Logger.e(TAG, "Failed to get concurrent camera ids");
        }

        for (Set<String> concurrentCameraIdList: mConcurrentCameraIds) {
            List<String> cameraIdList = new ArrayList<>(concurrentCameraIdList);

            // TODO(b/268531569): enumerate concurrent camera ids and convert to a map for
            //  paired camera id lookup.
            if (cameraIdList.size() >= 2) {
                mConcurrentCameraIdMap.put(cameraIdList.get(0), cameraIdList.get(1));
                mConcurrentCameraIdMap.put(cameraIdList.get(1), cameraIdList.get(0));
            }
        }
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private static CameraSelector createCameraSelectorById(
            @NonNull CameraManagerCompat cameraManager,
            @NonNull String cameraId) {
        CameraSelector.Builder builder =
                new CameraSelector.Builder().addCameraFilter(cameraInfos -> {
                    for (CameraInfo cameraInfo : cameraInfos) {
                        if (cameraId.equals(Camera2CameraInfo.from(cameraInfo).getCameraId())) {
                            return Collections.singletonList(cameraInfo);
                        }
                    }

                    throw new IllegalArgumentException("No camera can be find for id: " + cameraId);
                });

        try {
            CameraCharacteristicsCompat cameraCharacteristics =
                    cameraManager.getCameraCharacteristicsCompat(cameraId);
            Integer cameraLensFacing = cameraCharacteristics.get(
                    CameraCharacteristics.LENS_FACING);
            builder.requireLensFacing(cameraLensFacing);
        } catch (CameraAccessExceptionCompat e) {
            throw new RuntimeException(e);
        }

        return builder.build();
    }
}
