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

package androidx.camera.core.impl;

import android.content.Context;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.InitializationException;

import java.util.List;
import java.util.Map;

/**
 * Camera device manager to provide the guaranteed supported stream capabilities related info for
 * all camera devices
 */
public interface CameraDeviceSurfaceManager {

    /**
     * Interface for deferring creation of a CameraDeviceSurfaceManager.
     */
    interface Provider {
        /**
         * Creates a new, initialized instance of a CameraDeviceSurfaceManager.
         *
         * @param context the android context
         * @return the factory instance
         * @throws InitializationException if it fails to create the factory
         */
        @NonNull
        CameraDeviceSurfaceManager newInstance(@NonNull Context context)
                throws InitializationException;
    }

    /**
     * Check whether the input surface configuration list is under the capability of any combination
     * of this object.
     *
     * @param cameraId          the camera id of the camera device to be compared
     * @param surfaceConfigList the surface configuration list to be compared
     * @return the check result that whether it could be supported
     */
    boolean checkSupported(String cameraId, List<SurfaceConfig> surfaceConfigList);

    /**
     * Transform to a SurfaceConfig object with cameraId, image format and size info
     *
     * @param cameraId    the camera id of the camera device to transform the object
     * @param imageFormat the image format info for the surface configuration object
     * @param size        the size info for the surface configuration object
     * @return new {@link SurfaceConfig} object
     */
    SurfaceConfig transformSurfaceConfig(String cameraId, int imageFormat, Size size);

    /**
     * Get max supported output size for specific camera device and image format
     *
     * @param cameraId    the camera Id
     * @param imageFormat the image format info
     * @return the max supported output size for the image format
     */
    @Nullable
    Size getMaxOutputSize(String cameraId, int imageFormat);

    /**
     * Retrieves a map of suggested resolutions for the given list of use cases.
     *
     * @param cameraId          the camera id of the camera device used by the use cases
     * @param existingSurfaces  list of surfaces already configured and used by the camera. The
     *                          resolutions for these surface can not change.
     * @param newUseCaseConfigs list of configurations of the use cases that will be given a
     *                          suggested resolution
     * @return map of suggested resolutions for given use cases
     *
     * @throws IllegalStateException    if not initialized
     * @throws IllegalArgumentException if {@code newUseCaseConfigs} is an empty list, if
     *      there isn't a supported combination of surfaces available, or if the {@code cameraId}
     *      is not a valid id.
     */
    @NonNull
    Map<UseCaseConfig<?>, Size> getSuggestedResolutions(
            @NonNull String cameraId,
            @NonNull List<SurfaceConfig> existingSurfaces,
            @NonNull List<UseCaseConfig<?>> newUseCaseConfigs);

    /**
     * Retrieves the preview size, choosing the smaller of the display size and 1080P.
     *
     * @return the size used for the on screen preview
     */
    Size getPreviewSize();

    /**
     * Checks whether a corrected aspect ratio is required due to device constraints.
     *
     * @param cameraId the camera id of the camera device used by the use cases
     * @return the check result that whether aspect ratio need to be corrected
     */
    boolean requiresCorrectedAspectRatio(@NonNull String cameraId);


    /**
     * Returns the corrected aspect ratio for the given use case configuration or {@code null} if
     * no correction is needed.
     *
     * @param cameraId the camera id of the camera device used by the use cases
     * @param rotation desired rotation of output aspect ratio relative to natural orientation
     * @return the corrected aspect ratio for the use case
     */
    @Nullable
    Rational getCorrectedAspectRatio(@NonNull String cameraId,
            @ImageOutputConfig.RotationValue int rotation);
}
