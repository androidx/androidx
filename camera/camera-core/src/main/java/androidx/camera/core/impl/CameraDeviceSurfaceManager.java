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
import android.util.Size;

import androidx.camera.core.InitializationException;
import androidx.camera.core.SessionConfig;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
         * @param context            the android context
         * @param cameraManager      the camera manager object used to query the camera information.
         * @param availableCameraIds current available camera ids.
         * @return the factory instance
         * @throws InitializationException if it fails to create the factory
         */
        @NonNull CameraDeviceSurfaceManager newInstance(@NonNull Context context,
                @Nullable Object cameraManager, @NonNull Set<String> availableCameraIds)
                throws InitializationException;
    }

    /**
     * Transform to a SurfaceConfig object with cameraId, image format and size info
     *
     * @param cameraMode    the working camera mode.
     * @param cameraId      the camera id of the camera device to transform the object
     * @param imageFormat   the image format info for the surface configuration object
     * @param size          the size info for the surface configuration object
     * @param streamUseCase the stream use case for the surface configuration object
     * @return new {@link SurfaceConfig} object
     * @throws IllegalArgumentException if the {@code cameraId} is not found in the supported
     *                                  combinations, or if there isn't a supported combination of
     *                                  surfaces available for the given parameters.
     */
    @NonNull SurfaceConfig transformSurfaceConfig(
            @CameraMode.Mode int cameraMode,
            @NonNull String cameraId,
            int imageFormat,
            @NonNull Size size,
            @NonNull StreamUseCase streamUseCase);

    /**
     * Retrieves a map of suggested stream specifications for the given list of use cases.
     *
     * @param cameraMode                         the working camera mode.
     * @param cameraId                           the camera id of the camera device used by the
     *                                           use cases
     * @param existingSurfaces                   list of surfaces already configured and used by
     *                                           the camera. The stream specifications for these
     *                                           surface can not change.
     * @param newUseCaseConfigsSupportedSizeMap  map of configurations of the use cases to the
     *                                           supported output sizes list that will be given a
     *                                           suggested stream specification
     * @param isPreviewStabilizationOn           whether the preview stabilization is enabled.
     * @param hasVideoCapture                    whether the use cases has video capture.
     * @param isFeatureComboInvocation           whether a code flow invoked through feature combo
     *                                           APIs (e.g. {@link
     *                                           SessionConfig#requiredFeatureGroup}).
     * @param findMaxSupportedFrameRate          if {@code true}, the maximum supported frame
     *                                           rate will be calculated and returned in
     *                              {@link SurfaceStreamSpecQueryResult#getMaxSupportedFrameRate()}
     *                                           and the target frame rate settings in use cases
     *                                           will be ignored while calculating the stream spec.
     *                                           If {@code false}, the value of
     *                               {@link SurfaceStreamSpecQueryResult#getMaxSupportedFrameRate()}
     *                                           is undetermined.
     * @return a {@link SurfaceStreamSpecQueryResult}.
     * @throws IllegalStateException    if not initialized
     * @throws IllegalArgumentException if {@code newUseCaseConfigs} is an empty list, if
     *                                  there isn't a supported combination of surfaces
     *                                  available, or if the {@code cameraId}
     *                                  is not a valid id.
     */
    @NonNull SurfaceStreamSpecQueryResult getSuggestedStreamSpecs(
            @CameraMode.Mode int cameraMode,
            @NonNull String cameraId,
            @NonNull List<AttachedSurfaceInfo> existingSurfaces,
            @NonNull Map<UseCaseConfig<?>, List<Size>> newUseCaseConfigsSupportedSizeMap,
            boolean isPreviewStabilizationOn,
            boolean hasVideoCapture,
            boolean isFeatureComboInvocation,
            boolean findMaxSupportedFrameRate);
}
