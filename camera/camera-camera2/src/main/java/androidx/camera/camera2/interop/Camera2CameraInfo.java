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

package androidx.camera.camera2.interop;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.Camera2CameraInfoImpl;
import androidx.camera.core.CameraInfo;
import androidx.core.util.Preconditions;

/**
 * Provides ability to extract Camera2-related camera information from {@link CameraInfo}.
 */
@ExperimentalCamera2Interop
public final class Camera2CameraInfo {

    /**
     * Returns the string camera ID for this camera.
     *
     * <p>The camera ID is the same as the camera ID that would be obtained from
     * {@link android.hardware.camera2.CameraManager#getCameraIdList()}.
     *
     * @param cameraInfo The {@link CameraInfo} to extract the camera ID from.
     * @return the camera ID.
     * @throws IllegalStateException if the camera info does not contain the camera 2 camera ID
     * (e.g., if CameraX was not initialized with a {@link androidx.camera.camera2.Camera2Config}).
     */
    @NonNull
    public static String extractCameraId(@NonNull CameraInfo cameraInfo) {
        Preconditions.checkState(cameraInfo instanceof Camera2CameraInfoImpl, "CameraInfo does "
                + "not contain any Camera2 information.");
        Camera2CameraInfoImpl impl = (Camera2CameraInfoImpl) cameraInfo;
        return impl.getCameraId();
    }

    // Should not be instantiated.
    private Camera2CameraInfo() {}
}
