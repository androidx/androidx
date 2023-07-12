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
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.internal.Camera2CameraInfoImpl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.core.util.Preconditions;

import java.util.Map;

/**
 * An interface for retrieving Camera2-related camera information.
 */
@ExperimentalCamera2Interop
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Camera2CameraInfo {
    private static final String TAG = "Camera2CameraInfo";
    private final Camera2CameraInfoImpl mCamera2CameraInfoImpl;

    /**
     * Creates a new camera information with Camera2 implementation.
     *
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
     * @throws IllegalArgumentException if the camera info does not contain the camera2 information
     *                                  (e.g., if CameraX was not initialized with a
     *                                  {@link androidx.camera.camera2.Camera2Config}).
     */
    @NonNull
    public static Camera2CameraInfo from(@NonNull CameraInfo cameraInfo) {
        CameraInfoInternal cameraInfoImpl =
                ((CameraInfoInternal) cameraInfo).getImplementation();
        Preconditions.checkArgument(cameraInfoImpl instanceof Camera2CameraInfoImpl,
                "CameraInfo doesn't contain Camera2 implementation.");
        return ((Camera2CameraInfoImpl) cameraInfoImpl).getCamera2CameraInfo();
    }

    /**
     * Gets the string camera ID.
     *
     * <p>The camera ID is the same as the camera ID that would be obtained from
     * {@link android.hardware.camera2.CameraManager#getCameraIdList()}. The ID that is retrieved
     * is not static and can change depending on the current internal configuration of the
     * {@link androidx.camera.core.Camera} from which the CameraInfo was retrieved.
     *
     * The Camera is a logical camera which can be backed by multiple
     * {@link android.hardware.camera2.CameraDevice}. However, only one CameraDevice is active at
     * one time. When the CameraDevice changes then the camera id will change.
     *
     * @return the camera ID.
     * @throws IllegalStateException if the camera info does not contain the camera 2 camera ID
     *                               (e.g., if CameraX was not initialized with a
     *                               {@link androidx.camera.camera2.Camera2Config}).
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
     * @param <T> The type of the characteristic value.
     * @param key The {@link CameraCharacteristics.Key} of the characteristic.
     * @return the value of the characteristic.
     */
    @Nullable
    public <T> T getCameraCharacteristic(@NonNull CameraCharacteristics.Key<T> key) {
        return mCamera2CameraInfoImpl.getCameraCharacteristicsCompat().get(key);
    }

    /**
     * Returns the {@link CameraCharacteristics} for this camera.
     *
     * <p>The CameraCharacteristics will be the ones that would be obtained by
     * {@link android.hardware.camera2.CameraManager#getCameraCharacteristics(String)}. The
     * CameraCharacteristics that are retrieved are not static and can change depending on the
     * current internal configuration of the camera.
     *
     * @param cameraInfo The {@link CameraInfo} to extract the CameraCharacteristics from.
     * @throws IllegalStateException if the camera info does not contain the camera 2
     *                               characteristics(e.g., if CameraX was not initialized with a
     *                               {@link androidx.camera.camera2.Camera2Config}).
     */
    // TODO: Hidden until new extensions API released.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static CameraCharacteristics extractCameraCharacteristics(
            @NonNull CameraInfo cameraInfo) {
        CameraInfoInternal cameraInfoImpl = ((CameraInfoInternal) cameraInfo).getImplementation();
        Preconditions.checkState(cameraInfoImpl instanceof Camera2CameraInfoImpl,
                "CameraInfo does not contain any Camera2 information.");
        Camera2CameraInfoImpl impl = (Camera2CameraInfoImpl) cameraInfoImpl;
        return impl.getCameraCharacteristicsCompat().toCameraCharacteristics();
    }

    /**
     * Returns a map consisting of the camera ids and the {@link CameraCharacteristics}s.
     *
     * <p>For every camera, the map contains at least the CameraCharacteristics for the camera id.
     * If the camera is logical camera, it will also contain associated physical camera ids and
     * their CameraCharacteristics.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Map<String, CameraCharacteristics> getCameraCharacteristicsMap() {
        return mCamera2CameraInfoImpl.getCameraCharacteristicsMap();
    }
}
