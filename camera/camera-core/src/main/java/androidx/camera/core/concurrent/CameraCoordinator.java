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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.impl.CameraStateRegistry;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Coordinator for concurrent camera.
 *
 * <p>It coordinates the order of camera device open and camera capture session configuration.
 * All camera devices intended to be operated concurrently, must be opened before configuring
 * sessions on any of the camera devices.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
public interface CameraCoordinator {

    int CAMERA_OPERATING_MODE_UNSPECIFIED = 0;

    int CAMERA_OPERATING_MODE_SINGLE = 1;

    int CAMERA_OPERATING_MODE_CONCURRENT = 2;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({CAMERA_OPERATING_MODE_UNSPECIFIED,
            CAMERA_OPERATING_MODE_SINGLE,
            CAMERA_OPERATING_MODE_CONCURRENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CameraOperatingMode {
    }

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
     * Gets active concurrent camera infos.
     *
     * @return list of active concurrent camera infos.
     */
    @NonNull
    List<CameraInfo> getActiveConcurrentCameraInfos();

    /**
     * Sets active concurrent camera infos.
     *
     * @param cameraInfos list of active concurrent camera infos.
     */
    void setActiveConcurrentCameraInfos(@NonNull List<CameraInfo> cameraInfos);

    /**
     * Returns paired camera id in concurrent mode.
     *
     * <p>The paired camera id dictionary is constructed when constructor is called. This
     * internal API is used to look up paired camera id when coordinating device open and session
     * config in {@link CameraStateRegistry}. Currently only dual cameras will be supported in
     * concurrent mode.
     *
     * @param cameraId camera id.
     * @return The paired camera id if exists or null if paired camera not exists.
     */
    @Nullable
    String getPairedConcurrentCameraId(@NonNull String cameraId);

    /**
     * Returns camera operating mode.
     *
     * @return camera operating mode including unspecific, single or concurrent.
     */
    @CameraOperatingMode
    int getCameraOperatingMode();

    /**
     * Sets concurrent camera mode.
     *
     * <p>This internal API will be called when user binds user cases to cameras, which will
     * enable or disable concurrent camera mode based on the input config.
     *
     * @param cameraOperatingMode camera operating mode including unspecific, single or concurrent.
     */
    void setCameraOperatingMode(@CameraOperatingMode int cameraOperatingMode);

    /**
     * Adds listener for concurrent camera mode update.
     * @param listener {@link ConcurrentCameraModeListener}.
     */
    void addListener(@NonNull ConcurrentCameraModeListener listener);

    /**
     * Removes listener for concurrent camera mode update.
     * @param listener {@link ConcurrentCameraModeListener}.
     */
    void removeListener(@NonNull ConcurrentCameraModeListener listener);

    /**
     * Clean up all the resources when CameraX shutdown.
     */
    void shutdown();

    /**
     * Interface for concurrent camera mode update.
     *
     * <p>Everytime user changes {@link CameraOperatingMode}, the observer will be notified and
     * update related states or parameters accordingly. E.g. in {@link CameraStateRegistry}, we
     * will update the number of max allowed cameras.
     */
    interface ConcurrentCameraModeListener {
        void onCameraOperatingModeUpdated(
                @CameraOperatingMode int prevMode,
                @CameraOperatingMode int currMode);
    }
}
