/*
 * Copyright 2022 The Android Open Source Project
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

import static androidx.camera.core.internal.utils.SizeUtil.VGA_SIZE;
import static androidx.camera.core.internal.utils.SizeUtil.getArea;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.compat.workaround.ExcludedSupportedSizesContainer;
import androidx.camera.camera2.internal.compat.workaround.ExtraSupportedSurfaceCombinationsContainer;
import androidx.camera.camera2.internal.compat.workaround.ResolutionCorrector;
import androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.SurfaceSizeDefinition;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.internal.utils.AspectRatioUtil;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Camera device supported surface configuration combinations
 *
 * <p>{@link android.hardware.camera2.CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices. It defines what combination
 * of surface configuration type and size pairs can be supported for different hardware level camera
 * devices. This structure is used to store a list of surface combinations that are guaranteed to
 * support for this camera device.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class SupportedSurfaceCombination {
    private static final String TAG = "SupportedSurfaceCombination";
    private static final Size ZERO_SIZE = new Size(0, 0);
    private static final Size QUALITY_1080P_SIZE = new Size(1920, 1080);
    private static final Size QUALITY_480P_SIZE = new Size(720, 480);
    private static final Rational ASPECT_RATIO_4_3 = new Rational(4, 3);
    private static final Rational ASPECT_RATIO_3_4 = new Rational(3, 4);
    private static final Rational ASPECT_RATIO_16_9 = new Rational(16, 9);
    private static final Rational ASPECT_RATIO_9_16 = new Rational(9, 16);
    private final List<SurfaceCombination> mSurfaceCombinations = new ArrayList<>();
    private final Map<Integer, Size> mMaxSizeCache = new HashMap<>();
    private final String mCameraId;
    private final CamcorderProfileHelper mCamcorderProfileHelper;
    private final CameraCharacteristicsCompat mCharacteristics;
    private final ExcludedSupportedSizesContainer mExcludedSupportedSizesContainer;
    private final ExtraSupportedSurfaceCombinationsContainer
            mExtraSupportedSurfaceCombinationsContainer;
    private final int mHardwareLevel;
    private final boolean mIsSensorLandscapeResolution;
    private final Map<Integer, List<Size>> mExcludedSizeListCache = new HashMap<>();
    private boolean mIsRawSupported = false;
    private boolean mIsBurstCaptureSupported = false;
    @VisibleForTesting
    SurfaceSizeDefinition mSurfaceSizeDefinition;
    private Map<Integer, Size[]> mOutputSizesCache = new HashMap<>();
    @NonNull
    private final DisplayInfoManager mDisplayInfoManager;
    private final ResolutionCorrector mResolutionCorrector = new ResolutionCorrector();

    SupportedSurfaceCombination(@NonNull Context context, @NonNull String cameraId,
            @NonNull CameraManagerCompat cameraManagerCompat,
            @NonNull CamcorderProfileHelper camcorderProfileHelper)
            throws CameraUnavailableException {
        mCameraId = Preconditions.checkNotNull(cameraId);
        mCamcorderProfileHelper = Preconditions.checkNotNull(camcorderProfileHelper);
        mExcludedSupportedSizesContainer = new ExcludedSupportedSizesContainer(cameraId);
        mExtraSupportedSurfaceCombinationsContainer =
                new ExtraSupportedSurfaceCombinationsContainer();
        mDisplayInfoManager = DisplayInfoManager.getInstance(context);

        try {
            mCharacteristics = cameraManagerCompat.getCameraCharacteristicsCompat(mCameraId);
            Integer keyValue = mCharacteristics.get(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            mHardwareLevel = keyValue != null ? keyValue
                    : CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
            mIsSensorLandscapeResolution = isSensorLandscapeResolution();
        } catch (CameraAccessExceptionCompat e) {
            throw CameraUnavailableExceptionHelper.createFrom(e);
        }

        int[] availableCapabilities =
                mCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

        if (availableCapabilities != null) {
            for (int capability : availableCapabilities) {
                if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) {
                    mIsRawSupported = true;
                } else if (capability
                        == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE) {
                    mIsBurstCaptureSupported = true;
                }
            }
        }

        generateSupportedCombinationList();
        generateSurfaceSizeDefinition();
        checkCustomization();
    }

    String getCameraId() {
        return mCameraId;
    }

    boolean isRawSupported() {
        return mIsRawSupported;
    }

    boolean isBurstCaptureSupported() {
        return mIsBurstCaptureSupported;
    }

    /**
     * Check whether the input surface configuration list is under the capability of any combination
     * of this object.
     *
     * @param surfaceConfigList the surface configuration list to be compared
     * @return the check result that whether it could be supported
     */
    boolean checkSupported(List<SurfaceConfig> surfaceConfigList) {
        boolean isSupported = false;

        for (SurfaceCombination surfaceCombination : mSurfaceCombinations) {
            isSupported = surfaceCombination.isSupported(surfaceConfigList);

            if (isSupported) {
                break;
            }
        }

        return isSupported;
    }

    /**
     * Transform to a SurfaceConfig object with image format and size info
     *
     * @param imageFormat the image format info for the surface configuration object
     * @param size        the size info for the surface configuration object
     * @return new {@link SurfaceConfig} object
     */
    SurfaceConfig transformSurfaceConfig(int imageFormat, Size size) {
        return SurfaceConfig.transformSurfaceConfig(imageFormat, size, mSurfaceSizeDefinition);
    }

    /**
     * Finds the suggested resolutions of the newly added UseCaseConfig.
     *
     * @param existingSurfaces  the existing surfaces.
     * @param newUseCaseConfigs newly added UseCaseConfig.
     * @return the suggested resolutions, which is a mapping from UseCaseConfig to the suggested
     * resolution.
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     *                                  found. This may be due to no available output size or no
     *                                  available surface combination.
     */
    @NonNull
    Map<UseCaseConfig<?>, Size> getSuggestedResolutions(
            @NonNull List<AttachedSurfaceInfo> existingSurfaces,
            @NonNull List<UseCaseConfig<?>> newUseCaseConfigs) {
        // Refresh Preview Size based on current display configurations.
        refreshPreviewSize();
        List<SurfaceConfig> surfaceConfigs = new ArrayList<>();
        for (AttachedSurfaceInfo scc : existingSurfaces) {
            surfaceConfigs.add(scc.getSurfaceConfig());
        }

        // Use the small size (640x480) for new use cases to check whether there is any possible
        // supported combination first
        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
            surfaceConfigs.add(
                    SurfaceConfig.transformSurfaceConfig(useCaseConfig.getInputFormat(),
                            new Size(640, 480),
                            mSurfaceSizeDefinition));
        }

        if (!checkSupported(surfaceConfigs)) {
            throw new IllegalArgumentException(
                    "No supported surface combination is found for camera device - Id : "
                            + mCameraId + ".  May be attempting to bind too many use cases. "
                            + "Existing surfaces: " + existingSurfaces + " New configs: "
                            + newUseCaseConfigs);
        }

        // Get the index order list by the use case priority for finding stream configuration
        List<Integer> useCasesPriorityOrder =
                getUseCasesPriorityOrder(
                        newUseCaseConfigs);
        List<List<Size>> supportedOutputSizesList = new ArrayList<>();

        // Collect supported output sizes for all use cases
        for (Integer index : useCasesPriorityOrder) {
            List<Size> supportedOutputSizes =
                    getSupportedOutputSizes(newUseCaseConfigs.get(index));
            supportedOutputSizesList.add(supportedOutputSizes);
        }

        // Get all possible size arrangements
        List<List<Size>> allPossibleSizeArrangements =
                getAllPossibleSizeArrangements(
                        supportedOutputSizesList);

        Map<UseCaseConfig<?>, Size> suggestedResolutionsMap = null;
        // Transform use cases to SurfaceConfig list and find the first (best) workable combination
        for (List<Size> possibleSizeList : allPossibleSizeArrangements) {
            // Attach SurfaceConfig of original use cases since it will impact the new use cases
            List<SurfaceConfig> surfaceConfigList = new ArrayList<>();
            for (AttachedSurfaceInfo sc : existingSurfaces) {
                surfaceConfigList.add(sc.getSurfaceConfig());
            }

            // Attach SurfaceConfig of new use cases
            for (int i = 0; i < possibleSizeList.size(); i++) {
                Size size = possibleSizeList.get(i);
                UseCaseConfig<?> newUseCase =
                        newUseCaseConfigs.get(useCasesPriorityOrder.get(i));
                surfaceConfigList.add(
                        SurfaceConfig.transformSurfaceConfig(newUseCase.getInputFormat(), size,
                                mSurfaceSizeDefinition));
            }

            // Check whether the SurfaceConfig combination can be supported
            if (checkSupported(surfaceConfigList)) {
                suggestedResolutionsMap = new HashMap<>();
                for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
                    suggestedResolutionsMap.put(
                            useCaseConfig,
                            possibleSizeList.get(
                                    useCasesPriorityOrder.indexOf(
                                            newUseCaseConfigs.indexOf(useCaseConfig))));
                }
                break;
            }
        }
        if (suggestedResolutionsMap == null) {
            throw new IllegalArgumentException(
                    "No supported surface combination is found for camera device - Id : "
                            + mCameraId + " and Hardware level: " + mHardwareLevel
                            + ". May be the specified resolution is too large and not supported."
                            + " Existing surfaces: " + existingSurfaces
                            + " New configs: " + newUseCaseConfigs);
        }
        return suggestedResolutionsMap;
    }

    private Rational getTargetAspectRatio(@NonNull ImageOutputConfig imageOutputConfig) {
        Rational outputRatio = null;
        // Gets the corrected aspect ratio due to device constraints or null if no correction is
        // needed.
        @TargetAspectRatio.Ratio int targetAspectRatio =
                new TargetAspectRatio().get(mCameraId, mCharacteristics);
        switch (targetAspectRatio) {
            case TargetAspectRatio.RATIO_4_3:
                outputRatio = mIsSensorLandscapeResolution ? ASPECT_RATIO_4_3 : ASPECT_RATIO_3_4;
                break;
            case TargetAspectRatio.RATIO_16_9:
                outputRatio = mIsSensorLandscapeResolution ? ASPECT_RATIO_16_9 : ASPECT_RATIO_9_16;
                break;
            case TargetAspectRatio.RATIO_MAX_JPEG:
                Size maxJpegSize = fetchMaxSize(ImageFormat.JPEG);
                outputRatio = new Rational(maxJpegSize.getWidth(), maxJpegSize.getHeight());
                break;
            case TargetAspectRatio.RATIO_ORIGINAL:
                Size targetSize = getTargetSize(imageOutputConfig);
                if (imageOutputConfig.hasTargetAspectRatio()) {
                    @AspectRatio.Ratio int aspectRatio = imageOutputConfig.getTargetAspectRatio();
                    switch (aspectRatio) {
                        case AspectRatio.RATIO_4_3:
                            outputRatio = mIsSensorLandscapeResolution ? ASPECT_RATIO_4_3
                                    : ASPECT_RATIO_3_4;
                            break;
                        case AspectRatio.RATIO_16_9:
                            outputRatio = mIsSensorLandscapeResolution ? ASPECT_RATIO_16_9
                                    : ASPECT_RATIO_9_16;
                            break;
                        default:
                            Logger.e(TAG, "Undefined target aspect ratio: " + aspectRatio);
                    }
                } else if (targetSize != null) {
                    // Target size is calculated from the target resolution. If target size is not
                    // null, sizes which aspect ratio is nearest to the aspect ratio of target size
                    // will be selected in priority.
                    outputRatio = new Rational(targetSize.getWidth(), targetSize.getHeight());
                }
                break;
            default:
                // Unhandled event.
        }
        return outputRatio;
    }

    private Size fetchMaxSize(int imageFormat) {
        Size size = mMaxSizeCache.get(imageFormat);
        if (size != null) {
            return size;
        }
        Size maxSize = getMaxOutputSizeByFormat(imageFormat);
        mMaxSizeCache.put(imageFormat, maxSize);
        return maxSize;
    }

    private List<Integer> getUseCasesPriorityOrder(List<UseCaseConfig<?>> newUseCaseConfigs) {
        List<Integer> priorityOrder = new ArrayList<>();

        /*
         * Once the stream resource is occupied by one use case, it will impact the other use cases.
         * Therefore, we need to define the priority for stream resource usage. For the use cases
         * with the higher priority, we will try to find the best one for them in priority as
         * possible.
         */
        List<Integer> priorityValueList = new ArrayList<>();

        for (UseCaseConfig<?> config : newUseCaseConfigs) {
            int priority = config.getSurfaceOccupancyPriority(0);
            if (!priorityValueList.contains(priority)) {
                priorityValueList.add(priority);
            }
        }

        Collections.sort(priorityValueList);
        // Reverse the priority value list in descending order since larger value means higher
        // priority
        Collections.reverse(priorityValueList);

        for (int priorityValue : priorityValueList) {
            for (UseCaseConfig<?> config : newUseCaseConfigs) {
                if (priorityValue == config.getSurfaceOccupancyPriority(0)) {
                    priorityOrder.add(newUseCaseConfigs.indexOf(config));
                }
            }
        }

        return priorityOrder;
    }

    @NonNull
    @VisibleForTesting
    List<Size> getSupportedOutputSizes(@NonNull UseCaseConfig<?> config) {
        int imageFormat = config.getInputFormat();
        ImageOutputConfig imageOutputConfig = (ImageOutputConfig) config;
        Size[] outputSizes = getCustomizedSupportSizesFromConfig(imageFormat, imageOutputConfig);
        if (outputSizes == null) {
            outputSizes = getAllOutputSizesByFormat(imageFormat);
        }
        List<Size> outputSizeCandidates = new ArrayList<>();
        Size maxSize = imageOutputConfig.getMaxResolution(null);
        Size maxOutputSizeByFormat = getMaxOutputSizeByFormat(imageFormat);

        // Set maxSize as the max resolution setting or the max supported output size for the
        // image format, whichever is smaller.
        if (maxSize == null || getArea(maxOutputSizeByFormat) < getArea(maxSize)) {
            maxSize = maxOutputSizeByFormat;
        }

        // Sort the output sizes. The Comparator result must be reversed to have a descending order
        // result.
        Arrays.sort(outputSizes, new CompareSizesByArea(true));

        Size targetSize = getTargetSize(imageOutputConfig);
        Size minSize = VGA_SIZE;
        int defaultSizeArea = getArea(VGA_SIZE);
        int maxSizeArea = getArea(maxSize);
        // When maxSize is smaller than 640x480, set minSize as 0x0. It means the min size bound
        // will be ignored. Otherwise, set the minimal size according to min(DEFAULT_SIZE,
        // TARGET_RESOLUTION).
        if (maxSizeArea < defaultSizeArea) {
            minSize = ZERO_SIZE;
        } else if (targetSize != null && getArea(targetSize) < defaultSizeArea) {
            minSize = targetSize;
        }

        // Filter out the ones that exceed the maximum size and the minimum size. The output
        // sizes candidates list won't have duplicated items.
        for (Size outputSize : outputSizes) {
            if (getArea(outputSize) <= getArea(maxSize) && getArea(outputSize) >= getArea(minSize)
                    && !outputSizeCandidates.contains(outputSize)) {
                outputSizeCandidates.add(outputSize);
            }
        }

        if (outputSizeCandidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Can not get supported output size under supported maximum for the format: "
                            + imageFormat);
        }

        Rational aspectRatio = getTargetAspectRatio(imageOutputConfig);

        // Check the default resolution if the target resolution is not set
        targetSize = targetSize == null ? imageOutputConfig.getDefaultResolution(null) : targetSize;

        List<Size> supportedResolutions = new ArrayList<>();
        Map<Rational, List<Size>> aspectRatioSizeListMap = new HashMap<>();

        if (aspectRatio == null) {
            // If no target aspect ratio is set, all sizes can be added to the result list
            // directly. No need to sort again since the source list has been sorted previously.
            supportedResolutions.addAll(outputSizeCandidates);

            // If the target resolution is set, use it to remove unnecessary larger sizes.
            if (targetSize != null) {
                removeSupportedSizesByTargetSize(supportedResolutions, targetSize);
            }
        } else {
            // Rearrange the supported size to put the ones with the same aspect ratio in the front
            // of the list and put others in the end from large to small. Some low end devices may
            // not able to get an supported resolution that match the preferred aspect ratio.

            // Group output sizes by aspect ratio.
            aspectRatioSizeListMap = groupSizesByAspectRatio(outputSizeCandidates);

            // If the target resolution is set, use it to remove unnecessary larger sizes.
            if (targetSize != null) {
                // Remove unnecessary larger sizes from each aspect ratio size list
                for (Rational key : aspectRatioSizeListMap.keySet()) {
                    removeSupportedSizesByTargetSize(aspectRatioSizeListMap.get(key), targetSize);
                }
            }

            // Sort the aspect ratio key set by the target aspect ratio.
            List<Rational> aspectRatios = new ArrayList<>(aspectRatioSizeListMap.keySet());
            Collections.sort(aspectRatios,
                    new AspectRatioUtil.CompareAspectRatiosByDistanceToTargetRatio(aspectRatio));

            // Put available sizes into final result list by aspect ratio distance to target ratio.
            for (Rational rational : aspectRatios) {
                for (Size size : aspectRatioSizeListMap.get(rational)) {
                    // A size may exist in multiple groups in mod16 condition. Keep only one in
                    // the final list.
                    if (!supportedResolutions.contains(size)) {
                        supportedResolutions.add(size);
                    }
                }
            }
        }

        supportedResolutions = mResolutionCorrector.insertOrPrioritize(
                SurfaceConfig.getConfigType(config.getInputFormat()),
                supportedResolutions);

        return supportedResolutions;
    }

    @Nullable
    private Size getTargetSize(@NonNull ImageOutputConfig imageOutputConfig) {
        int targetRotation = imageOutputConfig.getTargetRotation(Surface.ROTATION_0);
        // Calibrate targetSize by the target rotation value.
        Size targetSize = imageOutputConfig.getTargetResolution(null);
        targetSize = flipSizeByRotation(targetSize, targetRotation);
        return targetSize;
    }

    // Use target rotation to calibrate the size.
    @Nullable
    private Size flipSizeByRotation(@Nullable Size size, int targetRotation) {
        Size outputSize = size;
        // Calibrates the size with the display and sensor rotation degrees values.
        if (size != null && isRotationNeeded(targetRotation)) {
            outputSize = new Size(/* width= */size.getHeight(), /* height= */size.getWidth());
        }
        return outputSize;
    }

    private boolean isRotationNeeded(int targetRotation) {
        Integer sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Preconditions.checkNotNull(sensorOrientation, "Camera HAL in bad state, unable to "
                + "retrieve the SENSOR_ORIENTATION");
        int relativeRotationDegrees =
                CameraOrientationUtil.surfaceRotationToDegrees(targetRotation);

        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        Integer lensFacing = mCharacteristics.get(CameraCharacteristics.LENS_FACING);
        Preconditions.checkNotNull(lensFacing, "Camera HAL in bad state, unable to retrieve the "
                + "LENS_FACING");

        boolean isOppositeFacingScreen = CameraCharacteristics.LENS_FACING_BACK == lensFacing;

        int sensorRotationDegrees = CameraOrientationUtil.getRelativeImageRotation(
                relativeRotationDegrees,
                sensorOrientation,
                isOppositeFacingScreen);
        return sensorRotationDegrees == 90 || sensorRotationDegrees == 270;
    }

    private boolean isSensorLandscapeResolution() {
        Size pixelArraySize =
                mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);

        // Make the default value is true since usually the sensor resolution is landscape.
        return pixelArraySize != null ? pixelArraySize.getWidth() >= pixelArraySize.getHeight()
                : true;
    }

    private Map<Rational, List<Size>> groupSizesByAspectRatio(List<Size> sizes) {
        Map<Rational, List<Size>> aspectRatioSizeListMap = new HashMap<>();

        // Add 4:3 and 16:9 entries first. Most devices should mainly have supported sizes of
        // these two aspect ratios. Adding them first can avoid that if the first one 4:3 or 16:9
        // size is a mod16 alignment size, the aspect ratio key may be different from the 4:3 or
        // 16:9 value.
        aspectRatioSizeListMap.put(ASPECT_RATIO_4_3, new ArrayList<>());
        aspectRatioSizeListMap.put(ASPECT_RATIO_16_9, new ArrayList<>());

        for (Size outputSize : sizes) {
            Rational matchedKey = null;

            for (Rational key : aspectRatioSizeListMap.keySet()) {
                // Put the size into all groups that is matched in mod16 condition since a size
                // may match multiple aspect ratio in mod16 algorithm.
                if (AspectRatioUtil.hasMatchingAspectRatio(outputSize, key)) {
                    matchedKey = key;

                    List<Size> sizeList = aspectRatioSizeListMap.get(matchedKey);
                    if (!sizeList.contains(outputSize)) {
                        sizeList.add(outputSize);
                    }
                }
            }

            // Create new item if no matching group is found.
            if (matchedKey == null) {
                aspectRatioSizeListMap.put(
                        new Rational(outputSize.getWidth(), outputSize.getHeight()),
                        new ArrayList<>(Collections.singleton(outputSize)));
            }
        }

        return aspectRatioSizeListMap;
    }

    /**
     * Removes unnecessary sizes by target size.
     *
     * <p>If the target resolution is set, a size that is equal to or closest to the target
     * resolution will be selected. If the list includes more than one size equal to or larger
     * than the target resolution, only one closest size needs to be kept. The other larger sizes
     * can be removed so that they won't be selected to use.
     *
     * @param supportedSizesList The list should have been sorted in descending order.
     * @param targetSize         The target size used to remove unnecessary sizes.
     */
    private void removeSupportedSizesByTargetSize(List<Size> supportedSizesList,
            Size targetSize) {
        if (supportedSizesList == null || supportedSizesList.isEmpty()) {
            return;
        }

        int indexBigEnough = -1;
        List<Size> removeSizes = new ArrayList<>();

        // Get the index of the item that is equal to or closest to the target size.
        for (int i = 0; i < supportedSizesList.size(); i++) {
            Size outputSize = supportedSizesList.get(i);
            if (outputSize.getWidth() >= targetSize.getWidth()
                    && outputSize.getHeight() >= targetSize.getHeight()) {
                // New big enough item closer to the target size is found. Adding the previous
                // one into the sizes list that will be removed.
                if (indexBigEnough >= 0) {
                    removeSizes.add(supportedSizesList.get(indexBigEnough));
                }

                indexBigEnough = i;
            } else {
                break;
            }
        }

        // Remove the unnecessary items that are larger than the item closest to the target size.
        supportedSizesList.removeAll(removeSizes);
    }

    private List<List<Size>> getAllPossibleSizeArrangements(
            List<List<Size>> supportedOutputSizesList) {
        int totalArrangementsCount = 1;

        for (List<Size> supportedOutputSizes : supportedOutputSizesList) {
            totalArrangementsCount *= supportedOutputSizes.size();
        }

        // If totalArrangementsCount is 0 means that there may some problem to get
        // supportedOutputSizes
        // for some use case
        if (totalArrangementsCount == 0) {
            throw new IllegalArgumentException("Failed to find supported resolutions.");
        }

        List<List<Size>> allPossibleSizeArrangements = new ArrayList<>();

        // Initialize allPossibleSizeArrangements for the following operations
        for (int i = 0; i < totalArrangementsCount; i++) {
            List<Size> sizeList = new ArrayList<>();
            allPossibleSizeArrangements.add(sizeList);
        }

        /*
         * Try to list out all possible arrangements by attaching all possible size of each column
         * in sequence. We have generated supportedOutputSizesList by the priority order for
         * different use cases. And the supported outputs sizes for each use case are also arranged
         * from large to small. Therefore, the earlier size arrangement in the result list will be
         * the better one to choose if finally it won't exceed the camera device's stream
         * combination capability.
         */
        int currentRunCount = totalArrangementsCount;
        int nextRunCount = currentRunCount / supportedOutputSizesList.get(0).size();

        for (int currentIndex = 0; currentIndex < supportedOutputSizesList.size(); currentIndex++) {
            List<Size> supportedOutputSizes = supportedOutputSizesList.get(currentIndex);
            for (int i = 0; i < totalArrangementsCount; i++) {
                List<Size> surfaceConfigList = allPossibleSizeArrangements.get(i);

                surfaceConfigList.add(
                        supportedOutputSizes.get((i % currentRunCount) / nextRunCount));
            }

            if (currentIndex < supportedOutputSizesList.size() - 1) {
                currentRunCount = nextRunCount;
                nextRunCount =
                        currentRunCount / supportedOutputSizesList.get(currentIndex + 1).size();
            }
        }

        return allPossibleSizeArrangements;
    }

    @NonNull
    private Size[] excludeProblematicSizes(@NonNull Size[] outputSizes, int imageFormat) {
        List<Size> excludedSizes = fetchExcludedSizes(imageFormat);
        List<Size> resultSizesList = new ArrayList<>(Arrays.asList(outputSizes));
        resultSizesList.removeAll(excludedSizes);
        return resultSizesList.toArray(new Size[0]);
    }

    @Nullable
    private Size[] getCustomizedSupportSizesFromConfig(int imageFormat,
            @NonNull ImageOutputConfig config) {
        Size[] outputSizes = null;

        // Try to retrieve customized supported resolutions from config.
        List<Pair<Integer, Size[]>> formatResolutionsPairList =
                config.getSupportedResolutions(null);

        if (formatResolutionsPairList != null) {
            for (Pair<Integer, Size[]> formatResolutionPair : formatResolutionsPairList) {
                if (formatResolutionPair.first == imageFormat) {
                    outputSizes = formatResolutionPair.second;
                    break;
                }
            }
        }

        if (outputSizes != null) {
            outputSizes = excludeProblematicSizes(outputSizes, imageFormat);

            // Sort the output sizes. The Comparator result must be reversed to have a descending
            // order result.
            Arrays.sort(outputSizes, new CompareSizesByArea(true));
        }

        return outputSizes;
    }

    @NonNull
    private Size[] getAllOutputSizesByFormat(int imageFormat) {
        Size[] outputs = mOutputSizesCache.get(imageFormat);
        if (outputs == null) {
            outputs = doGetAllOutputSizesByFormat(imageFormat);
            mOutputSizesCache.put(imageFormat, outputs);
        }

        return outputs;
    }

    @NonNull
    private Size[] doGetAllOutputSizesByFormat(int imageFormat) {
        Size[] outputSizes;

        StreamConfigurationMap map =
                mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
            throw new IllegalArgumentException("Can not retrieve SCALER_STREAM_CONFIGURATION_MAP");
        }

        if (Build.VERSION.SDK_INT < 23
                && imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
            // This is a little tricky that 0x22 that is internal defined in
            // StreamConfigurationMap.java to be equal to ImageFormat.PRIVATE that is public
            // after Android level 23 but not public in Android L. Use {@link SurfaceTexture}
            // or {@link MediaCodec} will finally mapped to 0x22 in StreamConfigurationMap to
            // retrieve the output sizes information.
            outputSizes = map.getOutputSizes(SurfaceTexture.class);
        } else {
            outputSizes = map.getOutputSizes(imageFormat);
        }

        if (outputSizes == null) {
            throw new IllegalArgumentException(
                    "Can not get supported output size for the format: " + imageFormat);
        }

        outputSizes = excludeProblematicSizes(outputSizes, imageFormat);

        // Sort the output sizes. The Comparator result must be reversed to have a descending order
        // result.
        Arrays.sort(outputSizes, new CompareSizesByArea(true));

        return outputSizes;
    }

    /**
     * Get max supported output size for specific image format
     *
     * @param imageFormat the image format info
     * @return the max supported output size for the image format
     */
    Size getMaxOutputSizeByFormat(int imageFormat) {
        Size[] outputSizes = getAllOutputSizesByFormat(imageFormat);

        return Collections.max(Arrays.asList(outputSizes), new CompareSizesByArea());
    }

    private void generateSupportedCombinationList() {
        mSurfaceCombinations.addAll(
                GuaranteedConfigurationsUtil.generateSupportedCombinationList(mHardwareLevel,
                        mIsRawSupported, mIsBurstCaptureSupported));

        mSurfaceCombinations.addAll(
                mExtraSupportedSurfaceCombinationsContainer.get(mCameraId, mHardwareLevel));
    }

    private void checkCustomization() {
        // TODO(b/119466260): Integrate found feasible stream combinations into supported list
    }

    // Utility classes and methods:
    // *********************************************************************************************

    private void generateSurfaceSizeDefinition() {
        Size analysisSize = new Size(640, 480);
        Size previewSize = mDisplayInfoManager.getPreviewSize();
        Size recordSize = getRecordSize();
        mSurfaceSizeDefinition =
                SurfaceSizeDefinition.create(analysisSize, previewSize, recordSize);
    }

    private void refreshPreviewSize() {
        mDisplayInfoManager.refresh();
        if (mSurfaceSizeDefinition == null) {
            generateSurfaceSizeDefinition();
        } else {
            Size previewSize = mDisplayInfoManager.getPreviewSize();
            mSurfaceSizeDefinition = SurfaceSizeDefinition.create(
                    mSurfaceSizeDefinition.getAnalysisSize(),
                    previewSize,
                    mSurfaceSizeDefinition.getRecordSize());
        }
    }

    /**
     * RECORD refers to the camera device's maximum supported recording resolution, as determined by
     * CamcorderProfile.
     */
    @NonNull
    private Size getRecordSize() {
        int cameraId;

        try {
            cameraId = Integer.parseInt(mCameraId);
        } catch (NumberFormatException e) {
            // The camera Id is not an integer because the camera may be a removable device. Use
            // StreamConfigurationMap to determine the RECORD size.
            return getRecordSizeFromStreamConfigurationMap();
        }

        CamcorderProfile profile = null;

        if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        }

        if (profile != null) {
            return new Size(profile.videoFrameWidth, profile.videoFrameHeight);
        }

        return getRecordSizeByHasProfile(cameraId);
    }

    /**
     * Return the maximum supported video size for cameras using data from the stream
     * configuration map.
     *
     * @return Maximum supported video size.
     */
    @NonNull
    private Size getRecordSizeFromStreamConfigurationMap() {
        StreamConfigurationMap map =
                mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
            throw new IllegalArgumentException("Can not retrieve SCALER_STREAM_CONFIGURATION_MAP");
        }

        Size[] videoSizeArr = map.getOutputSizes(MediaRecorder.class);

        if (videoSizeArr == null) {
            return QUALITY_480P_SIZE;
        }

        Arrays.sort(videoSizeArr, new CompareSizesByArea(true));

        for (Size size : videoSizeArr) {
            // Returns the largest supported size under 1080P
            if (size.getWidth() <= QUALITY_1080P_SIZE.getWidth()
                    && size.getHeight() <= QUALITY_1080P_SIZE.getHeight()) {
                return size;
            }
        }

        return QUALITY_480P_SIZE;
    }

    /**
     * Return the maximum supported video size for cameras by
     * {@link CamcorderProfile#hasProfile(int, int)}.
     *
     * @return Maximum supported video size.
     */
    @NonNull
    private Size getRecordSizeByHasProfile(int cameraId) {
        Size recordSize = QUALITY_480P_SIZE;
        CamcorderProfile profile = null;

        // Check whether 4KDCI, 2160P, 2K, 1080P, 720P, 480P (sorted by size) are supported by
        // CamcorderProfile
        if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_4KDCI)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_4KDCI);
        } else if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_2160P)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_2160P);
        } else if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_2K)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_2K);
        } else if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_1080P);
        } else if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_720P);
        } else if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_480P);
        }

        if (profile != null) {
            recordSize = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
        }

        return recordSize;
    }

    @NonNull
    private List<Size> fetchExcludedSizes(int imageFormat) {
        List<Size> excludedSizes = mExcludedSizeListCache.get(imageFormat);

        if (excludedSizes == null) {
            excludedSizes = mExcludedSupportedSizesContainer.get(imageFormat);
            mExcludedSizeListCache.put(imageFormat, excludedSizes);
        }

        return excludedSizes;
    }
}
