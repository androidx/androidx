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

package androidx.camera.camera2.impl;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraDeviceConfig;
import androidx.camera.core.CameraDeviceSurfaceManager;
import androidx.camera.core.CameraX;
import androidx.camera.core.SurfaceConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Camera device manager to provide the guaranteed supported stream capabilities related info for
 * all camera devices
 *
 * <p>{@link android.hardware.camera2.CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices. It defines what combination
 * of surface configuration type and size pairs can be supported for different hardware level camera
 * devices. This structure is used to store the guaranteed supported stream capabilities related
 * info.
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public final class Camera2DeviceSurfaceManager implements CameraDeviceSurfaceManager {
    private static final String TAG = "Camera2DeviceSurfaceManager";
    private static final Size MAXIMUM_PREVIEW_SIZE = new Size(1920, 1080);
    private final Map<String, SupportedSurfaceCombination> mCameraSupportedSurfaceCombinationMap =
            new HashMap<>();
    private boolean mIsInitialized = false;

    public Camera2DeviceSurfaceManager(Context context) {
        init(context, new CamcorderProfileHelper() {
            @Override
            public boolean hasProfile(int cameraId, int quality) {
                return CamcorderProfile.hasProfile(cameraId, quality);
            }
        });
    }

    @VisibleForTesting
    Camera2DeviceSurfaceManager(Context context, CamcorderProfileHelper camcorderProfileHelper) {
        init(context, camcorderProfileHelper);
    }

    /**
     * Check whether the input surface configuration list is under the capability of any combination
     * of this object.
     *
     * @param cameraId          the camera id of the camera device to be compared
     * @param surfaceConfigList the surface configuration list to be compared
     * @return the check result that whether it could be supported
     */
    @Override
    public boolean checkSupported(
            String cameraId, List<SurfaceConfig> surfaceConfigList) {
        boolean isSupported = false;

        if (!mIsInitialized) {
            throw new IllegalStateException("Camera2DeviceSurfaceManager is not initialized.");
        }

        if (surfaceConfigList == null || surfaceConfigList.isEmpty()) {
            return true;
        }

        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

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
     */
    @Override
    public SurfaceConfig transformSurfaceConfig(String cameraId, int imageFormat, Size size) {
        SurfaceConfig surfaceConfig = null;

        if (!mIsInitialized) {
            throw new IllegalStateException("Camera2DeviceSurfaceManager is not initialized.");
        }

        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

        if (supportedSurfaceCombination != null) {
            surfaceConfig =
                    supportedSurfaceCombination.transformSurfaceConfig(imageFormat, size);
        }

        return surfaceConfig;
    }

    /**
     * Retrieves a map of suggested resolutions for the given list of use cases.
     *
     * @param cameraId         the camera id of the camera device used by the use cases
     * @param originalUseCases list of use cases with existing surfaces
     * @param newUseCases      list of new use cases
     * @return map of suggested resolutions for given use cases
     */
    @Override
    public Map<UseCase, Size> getSuggestedResolutions(
            String cameraId, List<UseCase> originalUseCases, List<UseCase> newUseCases) {

        if (newUseCases == null || newUseCases.isEmpty()) {
            throw new IllegalArgumentException("No new use cases to be bound.");
        }

        UseCaseSurfaceOccupancyManager.checkUseCaseLimitNotExceeded(originalUseCases, newUseCases);

        // Use the small size (640x480) for new use cases to check whether there is any possible
        // supported combination first
        List<SurfaceConfig> surfaceConfigs = new ArrayList<>();

        if (originalUseCases != null) {
            for (UseCase useCase : originalUseCases) {
                String useCaseCameraId = getCameraIdFromConfig(useCase.getUseCaseConfig());
                Size resolution = useCase.getAttachedSurfaceResolution(useCaseCameraId);

                surfaceConfigs.add(
                        transformSurfaceConfig(cameraId, useCase.getImageFormat(), resolution));
            }
        }

        for (UseCase useCase : newUseCases) {
            surfaceConfigs.add(
                    transformSurfaceConfig(cameraId, useCase.getImageFormat(), new Size(640, 480)));
        }

        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

        if (supportedSurfaceCombination == null
                || !supportedSurfaceCombination.checkSupported(surfaceConfigs)) {
            throw new IllegalArgumentException(
                    "No supported surface combination is found for camera device - Id : "
                            + cameraId + ".  May be attempting to bind too many use cases.");
        }

        return supportedSurfaceCombination.getSuggestedResolutions(originalUseCases, newUseCases);
    }

    private void init(Context context, CamcorderProfileHelper camcorderProfileHelper) {
        if (!mIsInitialized) {
            CameraManager cameraManager =
                    (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

            try {
                for (String cameraId : cameraManager.getCameraIdList()) {
                    mCameraSupportedSurfaceCombinationMap.put(
                            cameraId,
                            new SupportedSurfaceCombination(
                                    context, cameraId, camcorderProfileHelper));
                }
            } catch (CameraAccessException e) {
                throw new IllegalArgumentException("Fail to get camera id list", e);
            }

            mIsInitialized = true;
        }
    }

    /**
     * Get max supported output size for specific camera device and image format
     *
     * @param cameraId    the camera Id
     * @param imageFormat the image format info
     * @return the max supported output size for the image format
     */
    @Override
    public Size getMaxOutputSize(String cameraId, int imageFormat) {
        if (!mIsInitialized) {
            throw new IllegalStateException("CameraDeviceSurfaceManager is not initialized.");
        }

        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

        if (supportedSurfaceCombination == null) {
            throw new IllegalArgumentException(
                    "Fail to find supported surface info - CameraId:" + cameraId);
        }

        return supportedSurfaceCombination.getMaxOutputSizeByFormat(imageFormat);
    }

    /**
     * Retrieves the preview size, choosing the smaller of the display size and 1080P.
     *
     * @return preview size from {@link androidx.camera.core.SurfaceSizeDefinition}
     */
    @Override
    public Size getPreviewSize() {
        if (!mIsInitialized) {
            throw new IllegalStateException("CameraDeviceSurfaceManager is not initialized.");
        }

        // 1920x1080 is maximum preview size
        Size previewSize = MAXIMUM_PREVIEW_SIZE;

        if (!mCameraSupportedSurfaceCombinationMap.isEmpty()) {
            // Preview size depends on the display size and 1080P. Therefore, we can get the first
            // camera
            // device's preview size to return it.
            String cameraId = (String) mCameraSupportedSurfaceCombinationMap.keySet().toArray()[0];
            previewSize =
                    mCameraSupportedSurfaceCombinationMap
                            .get(cameraId)
                            .getSurfaceSizeDefinition()
                            .getPreviewSize();
        }

        return previewSize;
    }

    /**
     * Checks whether the use case requires a corrected aspect ratio due to device constraints.
     *
     * @param useCaseConfig to check aspect ratio
     * @return the check result that whether aspect ratio need to be corrected
     */
    @Override
    public boolean requiresCorrectedAspectRatio(@NonNull UseCaseConfig<?> useCaseConfig) {
        if (!mIsInitialized) {
            throw new IllegalStateException("CameraDeviceSurfaceManager is not initialized.");
        }
        String cameraId = getCameraIdFromConfig(useCaseConfig);
        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

        if (supportedSurfaceCombination == null) {
            throw new IllegalArgumentException(
                    "Fail to find supported surface info - CameraId:" + cameraId);
        }
        return supportedSurfaceCombination.requiresCorrectedAspectRatio();
    }

    /**
     * Returns the corrected aspect ratio for the given use case configuration or {@code null} if
     * no correction is needed.
     *
     * @param useCaseConfig to check aspect ratio
     * @return the corrected aspect ratio for the use case
     */
    @Nullable
    @Override
    public Rational getCorrectedAspectRatio(@NonNull UseCaseConfig<?> useCaseConfig) {
        if (!mIsInitialized) {
            throw new IllegalStateException("CameraDeviceSurfaceManager is not initialized.");
        }
        String cameraId = getCameraIdFromConfig(useCaseConfig);
        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

        if (supportedSurfaceCombination == null) {
            throw new IllegalArgumentException(
                    "Fail to find supported surface info - CameraId:" + cameraId);
        }
        return supportedSurfaceCombination.getCorrectedAspectRatio(useCaseConfig);
    }

    private String getCameraIdFromConfig(UseCaseConfig<?> useCaseConfig) {
        CameraDeviceConfig config = (CameraDeviceConfig) useCaseConfig;
        String cameraId;
        try {
            cameraId = CameraX.getCameraWithLensFacing(config.getLensFacing());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to get camera ID for use case " + useCaseConfig.getTargetName(), e);
        }
        return cameraId;
    }

    enum Operation {
        ADD_CONFIG,
        REMOVE_CONFIG
    }
}
