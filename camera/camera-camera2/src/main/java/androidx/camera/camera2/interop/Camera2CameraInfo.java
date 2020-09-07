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

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.internal.Camera2CameraInfoImpl;
import androidx.camera.core.CameraInfo;
import androidx.core.util.Preconditions;

/**
 * An interface for retrieving Camera2-related camera information.
 */
@ExperimentalCamera2Interop
public final class Camera2CameraInfo {

    private final Camera2CameraInfoImpl mCamera2CameraInfoImpl;

    /**
     * Creates a new camera information with Camera2 implementation.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public Camera2CameraInfo(@NonNull Camera2CameraInfoImpl camera2CameraInfoImpl) {
        mCamera2CameraInfoImpl = camera2CameraInfoImpl;
    }

    /**
     * Gets the {@link Camera2CameraInfo} from a {@link CameraInfo}.
     *
     * @param cameraInfo The {@link CameraInfo} to get from.
     * @return The camera information with Camera2 implementation.
     * @throws IllegalStateException if the camera info does not contain the camera2 information
     *                               (e.g., if CameraX was not initialized with a
     *                               {@link androidx.camera.camera2.Camera2Config}).
     */
    @NonNull
    public static Camera2CameraInfo fromCameraInfo(@NonNull CameraInfo cameraInfo) {
        Preconditions.checkState(cameraInfo instanceof Camera2CameraInfoImpl,
                "CameraInfo doesn't contain Camera2 implementation.");
        return ((Camera2CameraInfoImpl) cameraInfo).getCamera2CameraInfo();
    }

    /**
     * Gets the string camera ID.
     *
     * <p>The camera ID is the same as the camera ID that would be obtained from
     * {@link android.hardware.camera2.CameraManager#getCameraIdList()}.
     *
     * @return the camera ID.
     */
    @NonNull
    public String getCameraId() {
        return mCamera2CameraInfoImpl.getCameraId();
    }

    /**
     * Gets a camera characteristic value.
     *
     * <p>The characteristic value is the same as the value in the {@link CameraCharacteristics}
     * that would be obtained from
     * {@link android.hardware.camera2.CameraManager#getCameraCharacteristics(String)}.
     *
     * @param <T>        The type of the characteristic value.
     * @param key        The {@link CameraCharacteristics.Key} of the characteristic.
     * @return the value of the characteristic.
     */
    @Nullable
    public <T> T getCameraCharacteristic(@NonNull CameraCharacteristics.Key<T> key) {
        return mCamera2CameraInfoImpl.getCameraCharacteristics().get(key);
    }
}
