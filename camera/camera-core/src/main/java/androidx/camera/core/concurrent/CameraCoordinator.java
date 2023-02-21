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

package androidx.camera.core.concurrent;


import android.hardware.camera2.CameraManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.impl.CameraStateRegistry;

import java.util.List;

/**
 * Coordinator for concurrent camera.
 *
 * <p>It coordinates the order of camera device open and camera capture session configuration.
 * All camera devices intended to be operated concurrently, must be opened before configuring
 * sessions on any of the camera devices.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
public interface CameraCoordinator {

    /**
     * Initializes the map for concurrent camera ids and convert camera ids to camera selectors.
     */
    void init();

    /**
     * Returns concurrent camera selectors, which are converted from concurrent camera ids
     * queried from {@link CameraManager#getConcurrentCameraIds()}.
     *
     * <p>This API is exposed to external users to select one combination of supported concurrent
     * {@link CameraSelector}s to bind.
     *
     * @return List of list of {@link CameraSelector}.
     */
    @NonNull
    List<List<CameraSelector>> getConcurrentCameraSelectors();

    /**
     * Returns paired camera id in concurrent mode.
     *
     * <p>The paired camera id dictionary is constructed when {@link CameraCoordinator#init()} is
     * called. This internal API is used to look up paired camera id when coordinating device
     * open and session config in {@link CameraStateRegistry}. Currently only dual cameras will
     * be supported in concurrent mode.
     *
     * @param cameraId camera id.
     * @return The paired camera id if exists or null if paired camera not exists.
     */
    @Nullable
    String getPairedConcurrentCameraId(@NonNull String cameraId);

    /**
     * Returns concurrent camera mode.
     *
     * @return true if concurrent mode is on, otherwise returns false.
     */
    boolean isConcurrentCameraModeOn();

    /**
     * Sets concurrent camera mode.
     *
     * <p>This internal API will be called when user binds user cases to cameras, which will
     * enable or disable concurrent camera mode based on the input config.
     *
     * @param enabled true if concurrent camera mode is enabled, otherwise false.
     */
    void setConcurrentCameraMode(boolean enabled);

    /**
     * Adds listener for concurrent camera mode update.
     * @param listener
     */
    void addListener(@NonNull ConcurrentCameraModeListener listener);

    /**
     * Removes listener for concurrent camera mode update.
     * @param listener
     */
    void removeListener(@NonNull ConcurrentCameraModeListener listener);

    /**
     * Interface for concurrent camera mode update.
     *
     * <p>Everytime user sets concurrent mode, the observer will be notified and update related
     * states or parameters accordingly. E.g. in
     * {@link CameraStateRegistry}, we will update the number of max
     * allowed cameras if concurrent mode is set.
     */
    interface ConcurrentCameraModeListener {
        void notifyConcurrentCameraModeUpdated(boolean isConcurrentCameraModeOn);
    }
}
