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

package androidx.camera.camera2.internal;

import android.content.Context;
import android.media.CamcorderProfile;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Camera device manager to provide the guaranteed supported stream capabilities related info for
 * all camera devices
 *
 * <p>{@link android.hardware.camera2.CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices. It defines what combination
 * of surface configuration type and size pairs can be supported for different hardware level camera
 * devices. This structure is used to store the guaranteed supported stream capabilities related
 * info.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Camera2DeviceSurfaceManager implements CameraDeviceSurfaceManager {
    private static final String TAG = "Camera2DeviceSurfaceManager";
    private final Map<String, SupportedSurfaceCombination> mCameraSupportedSurfaceCombinationMap =
            new HashMap<>();
    private final CamcorderProfileHelper mCamcorderProfileHelper;

    /**
     * Creates a new, initialized Camera2DeviceSurfaceManager.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public Camera2DeviceSurfaceManager(@NonNull Context context,
            @Nullable Object cameraManager, @NonNull Set<String> availableCameraIds)
            throws CameraUnavailableException {
        this(context, new CamcorderProfileHelper() {
            @Override
            public boolean hasProfile(int cameraId, int quality) {
                return CamcorderProfile.hasProfile(cameraId, quality);
            }

            @Override
            @SuppressWarnings("deprecation")
            public CamcorderProfile get(int cameraId, int quality) {
                return CamcorderProfile.get(cameraId, quality);
            }
        }, cameraManager, availableCameraIds);
    }

    Camera2DeviceSurfaceManager(@NonNull Context context,
            @NonNull CamcorderProfileHelper camcorderProfileHelper,
            @Nullable Object cameraManager,
            @NonNull Set<String> availableCameraIds)
            throws CameraUnavailableException {
        Preconditions.checkNotNull(camcorderProfileHelper);
        mCamcorderProfileHelper = camcorderProfileHelper;

        CameraManagerCompat cameraManagerCompat;
        if (cameraManager instanceof CameraManagerCompat) {
            cameraManagerCompat = (CameraManagerCompat) cameraManager;
        } else {
            cameraManagerCompat = CameraManagerCompat.from(context);
        }
        init(context, cameraManagerCompat, availableCameraIds);
    }

    /**
     * Prepare necessary resources for the surface manager.
     */
    private void init(@NonNull Context context, @NonNull CameraManagerCompat cameraManager,
            @NonNull Set<String> availableCameraIds)
            throws CameraUnavailableException {
        Preconditions.checkNotNull(context);

        for (String cameraId : availableCameraIds) {
            mCameraSupportedSurfaceCombinationMap.put(
                    cameraId,
                    new SupportedSurfaceCombination(
                            context, cameraId, cameraManager, mCamcorderProfileHelper));
        }
    }

    /**
     * Check whether the input surface configuration list is under the capability of any combination
     * of this object.
     *
     * @param cameraId          the camera id of the camera device to be compared
     * @param surfaceConfigList the surface configuration list to be compared
     * @return the check result that whether it could be supported
     * @throws IllegalStateException if not initialized
     */
    @Override
    public boolean checkSupported(
            @NonNull String cameraId, @Nullable List<SurfaceConfig> surfaceConfigList) {
        if (surfaceConfigList == null || surfaceConfigList.isEmpty()) {
            return true;
        }

        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

        boolean isSupported = false;
        if (supportedSurfaceCombination != null) {
            isSupported = supportedSurfaceCombination.checkSupported(surfaceConfigList);
        }

        return isSupported;
    }

    /**
     * Transform to a SurfaceConfig object with cameraId, image format and size info
     *
     * @param cameraId    the camera id of the camera device to transform the object
     * @param imageFormat the image format info for the surface configuration object
     * @param size        the size info for the surface configuration object
     * @return new {@link SurfaceConfig} object
     * @throws IllegalStateException if not initialized
     */
    @Nullable
    @Override
    public SurfaceConfig transformSurfaceConfig(@NonNull String cameraId, int imageFormat,
            @NonNull Size size) {
        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

        SurfaceConfig surfaceConfig = null;
        if (supportedSurfaceCombination != null) {
            surfaceConfig =
                    supportedSurfaceCombination.transformSurfaceConfig(imageFormat, size);
        }

        return surfaceConfig;
    }

    /**
     * Retrieves a map of suggested resolutions for the given list of use cases.
     *
     * @param cameraId          the camera id of the camera device used by the use cases
     * @param existingSurfaces  list of surfaces already configured and used by the camera. The
     *                          resolutions for these surface can not change.
     * @param newUseCaseConfigs list of configurations of the use cases that will be given a
     *                          suggested resolution
     * @return map of suggested resolutions for given use cases
     * @throws IllegalStateException    if not initialized
     * @throws IllegalArgumentException if {@code newUseCaseConfigs} is an empty list, if
     *                                  there isn't a supported combination of surfaces
     *                                  available, or if the {@code cameraId}
     *                                  is not a valid id.
     */
    @NonNull
    @Override
    public Map<UseCaseConfig<?>, Size> getSuggestedResolutions(
            @NonNull String cameraId,
            @NonNull List<SurfaceConfig> existingSurfaces,
            @NonNull List<UseCaseConfig<?>> newUseCaseConfigs) {
        Preconditions.checkArgument(!newUseCaseConfigs.isEmpty(), "No new use cases to be bound.");

        // Use the small size (640x480) for new use cases to check whether there is any possible
        // supported combination first
        List<SurfaceConfig> surfaceConfigs = new ArrayList<>(existingSurfaces);

        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
            surfaceConfigs.add(
                    transformSurfaceConfig(cameraId,
                            useCaseConfig.getInputFormat(),
                            new Size(640, 480)));
        }

        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

        if (supportedSurfaceCombination == null) {
            throw new IllegalArgumentException("No such camera id in supported combination list: "
                    + cameraId);
        }

        if (!supportedSurfaceCombination.checkSupported(surfaceConfigs)) {
            throw new IllegalArgumentException(
                    "No supported surface combination is found for camera device - Id : "
                            + cameraId + ".  May be attempting to bind too many use cases. "
                            + "Existing surfaces: " + existingSurfaces + " New configs: "
                            + newUseCaseConfigs);
        }

        return supportedSurfaceCombination.getSuggestedResolutions(existingSurfaces,
                newUseCaseConfigs);
    }
}
