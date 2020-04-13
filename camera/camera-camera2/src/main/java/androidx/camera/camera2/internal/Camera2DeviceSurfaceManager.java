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
import android.util.Rational;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.SurfaceSizeDefinition;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.core.util.Preconditions;

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
 */
public final class Camera2DeviceSurfaceManager implements CameraDeviceSurfaceManager {
    private static final String TAG = "Camera2DeviceSurfaceManager";
    private static final Size MAXIMUM_PREVIEW_SIZE = new Size(1920, 1080);
    private final Map<String, SupportedSurfaceCombination> mCameraSupportedSurfaceCombinationMap =
            new HashMap<>();
    private final CamcorderProfileHelper mCamcorderProfileHelper;

    /**
     * Creates a new, initialized Camera2DeviceSurfaceManager.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public Camera2DeviceSurfaceManager(@NonNull Context context) throws CameraUnavailableException {
        this(context, CamcorderProfile::hasProfile);
    }

    Camera2DeviceSurfaceManager(@NonNull Context context,
            @NonNull CamcorderProfileHelper camcorderProfileHelper)
            throws CameraUnavailableException {
        Preconditions.checkNotNull(camcorderProfileHelper);
        mCamcorderProfileHelper = camcorderProfileHelper;
        init(context);
    }

    /**
     * Prepare necessary resources for the surface manager.
     */
    private void init(@NonNull Context context) throws CameraUnavailableException {
        Preconditions.checkNotNull(context);
        CameraManagerCompat cameraManager = CameraManagerCompat.from(context);

        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                mCameraSupportedSurfaceCombinationMap.put(
                        cameraId,
                        new SupportedSurfaceCombination(
                                context, cameraId, mCamcorderProfileHelper));
            }
        } catch (CameraAccessExceptionCompat e) {
            throw CameraUnavailableExceptionHelper.createFrom(e);
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

    /**
     * Get max supported output size for specific camera device and image format
     *
     * @param cameraId    the camera Id
     * @param imageFormat the image format info
     * @return the max supported output size for the image format
     * @throws IllegalStateException if not initialized
     */
    @NonNull
    @Override
    public Size getMaxOutputSize(@NonNull String cameraId, int imageFormat) {
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
     * @return preview size from {@link SurfaceSizeDefinition}
     * @throws IllegalStateException if not initialized
     */
    @NonNull
    @Override
    public Size getPreviewSize() {
        // 1920x1080 is maximum preview size
        Size previewSize = MAXIMUM_PREVIEW_SIZE;

        if (!mCameraSupportedSurfaceCombinationMap.isEmpty()) {
            // Preview size depends on the display size and 1080P. Therefore, we can get the first
            // camera device's preview size to return it.
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
     * @param cameraId the camera Id
     * @return the check result that whether aspect ratio need to be corrected
     * @throws IllegalStateException    if not initialized
     * @throws IllegalArgumentException if supported surface information for given camera id
     *                                  can't be found.
     */
    @Override
    public boolean requiresCorrectedAspectRatio(@NonNull String cameraId) {
        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

        if (supportedSurfaceCombination == null) {
            throw new IllegalArgumentException(
                    "Fail to find supported surface info - CameraId:" + cameraId);
        }
        return supportedSurfaceCombination.requiresCorrectedAspectRatio();
    }

    /**
     * Returns the corrected aspect ratio for the camera id or {@code null} if
     * no correction is needed.
     *
     * @param cameraId the camera Id of the device that requires correction
     * @param rotation desired rotation of output aspect ratio relative to natural orientation
     * @return the corrected aspect ratio for the use case
     * @throws IllegalStateException    if not initialized
     * @throws IllegalArgumentException if supported surface information for given camera id
     *                                  can't be found.
     */
    @Nullable
    @Override
    public Rational getCorrectedAspectRatio(@NonNull String cameraId,
            @ImageOutputConfig.RotationValue int rotation) {
        SupportedSurfaceCombination supportedSurfaceCombination =
                mCameraSupportedSurfaceCombinationMap.get(cameraId);

        if (supportedSurfaceCombination == null) {
            throw new IllegalArgumentException(
                    "Fail to find supported surface info - CameraId:" + cameraId);
        }
        return supportedSurfaceCombination.getCorrectedAspectRatio(rotation);
    }
}
