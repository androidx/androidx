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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.concurrent.CameraCoordinator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link CameraCoordinator} implementation that contains concurrent camera mode and camera id
 * information.
 *
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeCameraCoordinator implements CameraCoordinator {

    @NonNull private Map<String, String> mConcurrentCameraIdMap;
    @NonNull private List<List<String>> mConcurrentCameraIds;
    @NonNull private List<List<CameraSelector>> mConcurrentCameraSelectors;
    @NonNull private List<CameraInfo> mActiveConcurrentCameraInfos;
    @NonNull private final List<ConcurrentCameraModeListener> mConcurrentCameraModeListeners;

    @CameraOperatingMode private int mCameraOperatingMode;

    public FakeCameraCoordinator() {
        mConcurrentCameraIdMap = new HashMap<>();
        mConcurrentCameraIds = new ArrayList<>();
        mConcurrentCameraSelectors = new ArrayList<>();
        mActiveConcurrentCameraInfos = new ArrayList<>();
        mConcurrentCameraModeListeners = new ArrayList<>();
    }

    /**
     * Adds concurrent camera id and camera selectors.
     *
     * @param cameraIdAndSelectors combinations of camera id and selector.
     */
    public void addConcurrentCameraIdsAndCameraSelectors(
            @NonNull Map<String, CameraSelector> cameraIdAndSelectors) {
        mConcurrentCameraIds.add(new ArrayList<>(cameraIdAndSelectors.keySet()));
        mConcurrentCameraSelectors.add(new ArrayList<>(cameraIdAndSelectors.values()));

        for (List<String> concurrentCameraIdList: mConcurrentCameraIds) {
            List<String> cameraIdList = new ArrayList<>(concurrentCameraIdList);
            mConcurrentCameraIdMap.put(cameraIdList.get(0), cameraIdList.get(1));
            mConcurrentCameraIdMap.put(cameraIdList.get(1), cameraIdList.get(0));
        }
    }

    @NonNull
    @Override
    public List<List<CameraSelector>> getConcurrentCameraSelectors() {
        return mConcurrentCameraSelectors;
    }

    @NonNull
    @Override
    public List<CameraInfo> getActiveConcurrentCameraInfos() {
        return mActiveConcurrentCameraInfos;
    }

    @Override
    public void setActiveConcurrentCameraInfos(@NonNull List<CameraInfo> cameraInfos) {
        mActiveConcurrentCameraInfos = cameraInfos;
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
                listener.onCameraOperatingModeUpdated(
                        mCameraOperatingMode,
                        cameraOperatingMode);
            }
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

    @Override
    public void shutdown() {
        mConcurrentCameraIdMap.clear();
        mConcurrentCameraIds.clear();
        mConcurrentCameraSelectors.clear();
        mActiveConcurrentCameraInfos.clear();
        mConcurrentCameraModeListeners.clear();
    }
}
