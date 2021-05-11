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
import android.graphics.ImageFormat;
import android.graphics.Point;
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
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.compat.workaround.ExcludedSupportedSizesContainer;
import androidx.camera.camera2.internal.compat.workaround.ExtraSupportedSurfaceCombinationsContainer;
import androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.SurfaceConfig.ConfigSize;
import androidx.camera.core.impl.SurfaceConfig.ConfigType;
import androidx.camera.core.impl.SurfaceSizeDefinition;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
final class SupportedSurfaceCombination {
    private static final String TAG = "SupportedSurfaceCombination";
    private static final Size MAX_PREVIEW_SIZE = new Size(1920, 1080);
    private static final Size DEFAULT_SIZE = new Size(640, 480);
    private static final Size ZERO_SIZE = new Size(0, 0);
    private static final Size QUALITY_1080P_SIZE = new Size(1920, 1080);
    private static final Size QUALITY_480P_SIZE = new Size(720, 480);
    private static final int ALIGN16 = 16;
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
    private SurfaceSizeDefinition mSurfaceSizeDefinition;
    private Map<Integer, Size[]> mOutputSizesCache = new HashMap<>();

    SupportedSurfaceCombination(@NonNull Context context, @NonNull String cameraId,
            @NonNull CameraManagerCompat cameraManagerCompat,
            @NonNull CamcorderProfileHelper camcorderProfileHelper)
            throws CameraUnavailableException {
        mCameraId = Preconditions.checkNotNull(cameraId);
        mCamcorderProfileHelper = Preconditions.checkNotNull(camcorderProfileHelper);
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mExcludedSupportedSizesContainer = new ExcludedSupportedSizesContainer(cameraId);
        mExtraSupportedSurfaceCombinationsContainer =
                new ExtraSupportedSurfaceCombinationsContainer(cameraId);

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
        generateSupportedCombinationList();
        generateSurfaceSizeDefinition(windowManager);
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
        ConfigType configType;
        ConfigSize configSize = ConfigSize.NOT_SUPPORT;

        /*
         * PRIV refers to any target whose available sizes are found using
         * StreamConfigurationMap.getOutputSizes(Class) with no direct application-visible format,
         * YUV refers to a target Surface using the ImageFormat.YUV_420_888 format, JPEG refers to
         * the ImageFormat.JPEG format, and RAW refers to the ImageFormat.RAW_SENSOR format.
         */
        if (imageFormat == ImageFormat.YUV_420_888) {
            configType = ConfigType.YUV;
        } else if (imageFormat == ImageFormat.JPEG) {
            configType = ConfigType.JPEG;
        } else if (imageFormat == ImageFormat.RAW_SENSOR) {
            configType = ConfigType.RAW;
        } else {
            configType = ConfigType.PRIV;
        }

        Size maxSize = fetchMaxSize(imageFormat);

        // Compare with surface size definition to determine the surface configuration size
        if (size.getWidth() * size.getHeight()
                <= mSurfaceSizeDefinition.getAnalysisSize().getWidth()
                * mSurfaceSizeDefinition.getAnalysisSize().getHeight()) {
            configSize = ConfigSize.ANALYSIS;
        } else if (size.getWidth() * size.getHeight()
                <= mSurfaceSizeDefinition.getPreviewSize().getWidth()
                * mSurfaceSizeDefinition.getPreviewSize().getHeight()) {
            configSize = ConfigSize.PREVIEW;
        } else if (size.getWidth() * size.getHeight()
                <= mSurfaceSizeDefinition.getRecordSize().getWidth()
                * mSurfaceSizeDefinition.getRecordSize().getHeight()) {
            configSize = ConfigSize.RECORD;
        } else if (size.getWidth() * size.getHeight() <= maxSize.getWidth() * maxSize.getHeight()) {
            configSize = ConfigSize.MAXIMUM;
        }

        return SurfaceConfig.create(configType, configSize);
    }

    Map<UseCaseConfig<?>, Size> getSuggestedResolutions(
            List<SurfaceConfig> existingSurfaces, List<UseCaseConfig<?>> newUseCaseConfigs) {
        Map<UseCaseConfig<?>, Size> suggestedResolutionsMap = new HashMap<>();

        // Get the index order list by the use case priority for finding stream configuration
        List<Integer> useCasesPriorityOrder = getUseCasesPriorityOrder(newUseCaseConfigs);
        List<List<Size>> supportedOutputSizesList = new ArrayList<>();

        // Collect supported output sizes for all use cases
        for (Integer index : useCasesPriorityOrder) {
            List<Size> supportedOutputSizes =
                    getSupportedOutputSizes(newUseCaseConfigs.get(index));
            supportedOutputSizesList.add(supportedOutputSizes);
        }

        // Get all possible size arrangements
        List<List<Size>> allPossibleSizeArrangements =
                getAllPossibleSizeArrangements(supportedOutputSizesList);

        // Transform use cases to SurfaceConfig list and find the first (best) workable combination
        for (List<Size> possibleSizeList : allPossibleSizeArrangements) {
            // Attach SurfaceConfig of original use cases since it will impact the new use cases
            List<SurfaceConfig> surfaceConfigList = new ArrayList<>(existingSurfaces);

            // Attach SurfaceConfig of new use cases
            for (int i = 0; i < possibleSizeList.size(); i++) {
                Size size = possibleSizeList.get(i);
                UseCaseConfig<?> newUseCase =
                        newUseCaseConfigs.get(useCasesPriorityOrder.get(i));
                surfaceConfigList.add(transformSurfaceConfig(newUseCase.getInputFormat(), size));
            }

            // Check whether the SurfaceConfig combination can be supported
            if (checkSupported(surfaceConfigList)) {
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

        return suggestedResolutionsMap;
    }

    private Rational getTargetAspectRatio(@NonNull ImageOutputConfig imageOutputConfig) {
        Rational outputRatio = null;
        // Gets the corrected aspect ratio due to device constraints or null if no correction is
        // needed.
        @TargetAspectRatio.Ratio int targetAspectRatio =
                new TargetAspectRatio().get(imageOutputConfig, mCameraId, mCharacteristics);
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

    SurfaceSizeDefinition getSurfaceSizeDefinition() {
        return mSurfaceSizeDefinition;
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
        Size minSize = DEFAULT_SIZE;
        int defaultSizeArea = getArea(DEFAULT_SIZE);
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
                    new CompareAspectRatiosByDistanceToTargetRatio(aspectRatio));

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
        Integer sensorOrientation =
                mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
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

    static boolean hasMatchingAspectRatio(Size resolution, Rational aspectRatio) {
        boolean isMatch = false;
        if (aspectRatio == null) {
            isMatch = false;
        } else if (aspectRatio.equals(
                new Rational(resolution.getWidth(), resolution.getHeight()))) {
            isMatch = true;
        } else if (getArea(resolution) >= getArea(DEFAULT_SIZE)) {
            // Only do mod 16 calculation if the size is equal to or larger than 640x480. It is
            // because the aspect ratio will be affected critically by mod 16 calculation if the
            // size is small. It may result in unexpected outcome such like 256x144 will be
            // considered as 18.5:9.
            isMatch = isPossibleMod16FromAspectRatio(resolution, aspectRatio);
        }
        return isMatch;
    }

    /**
     * For codec performance improvement, OEMs may make the supported sizes to be mod16 alignment
     * . It means that the width or height of the supported size will be multiple of 16. The
     * result number after applying mod16 alignment can be the larger or smaller number that is
     * multiple of 16 and is closest to the original number. For example, a standard 16:9
     * supported size is 1920x1080. It may become 1920x1088 on some devices because 1088 is
     * multiple of 16. This function uses the target aspect ratio to calculate the possible
     * original width or height inversely. And then, checks whether the possibly original width or
     * height is in the range that the mod16 aligned height or width can support.
     */
    private static boolean isPossibleMod16FromAspectRatio(Size resolution, Rational aspectRatio) {
        int width = resolution.getWidth();
        int height = resolution.getHeight();

        Rational invAspectRatio = new Rational(/* numerator= */aspectRatio.getDenominator(),
                /* denominator= */aspectRatio.getNumerator());
        if (width % 16 == 0 && height % 16 == 0) {
            return ratioIntersectsMod16Segment(Math.max(0, height - ALIGN16), width, aspectRatio)
                    || ratioIntersectsMod16Segment(Math.max(0, width - ALIGN16), height,
                    invAspectRatio);
        } else if (width % 16 == 0) {
            return ratioIntersectsMod16Segment(height, width, aspectRatio);
        } else if (height % 16 == 0) {
            return ratioIntersectsMod16Segment(width, height, invAspectRatio);
        }
        return false;
    }

    private static int getArea(Size size) {
        return size.getWidth() * size.getHeight();
    }

    private static boolean ratioIntersectsMod16Segment(int height, int mod16Width,
            Rational aspectRatio) {
        Preconditions.checkArgument(mod16Width % 16 == 0);
        double aspectRatioWidth =
                height * aspectRatio.getNumerator() / (double) aspectRatio.getDenominator();
        return aspectRatioWidth > Math.max(0, mod16Width - ALIGN16) && aspectRatioWidth < (
                mod16Width + ALIGN16);
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
                if (hasMatchingAspectRatio(outputSize, key)) {
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

    List<SurfaceCombination> getLegacySupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        // (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        // Below two combinations are all supported in the combination
        // (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination4);

        // (YUV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination5);

        // (PRIV, PREVIEW) + (PRIV, PREVIEW)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        combinationList.add(surfaceCombination6);

        // (PRIV, PREVIEW) + (YUV, PREVIEW)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        combinationList.add(surfaceCombination7);

        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination8);

        return combinationList;
    }

    List<SurfaceCombination> getLimitedSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, PREVIEW) + (PRIV, RECORD)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (YUV, RECORD)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination2);

        // (YUV, PREVIEW) + (YUV, RECORD)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination3);

        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD));
        combinationList.add(surfaceCombination4);

        // (PRIV, PREVIEW) + (YUV, RECORD) + (JPEG, RECORD)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD));
        combinationList.add(surfaceCombination5);

        // (YUV, PREVIEW) + (YUV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination6);

        return combinationList;
    }

    List<SurfaceCombination> getFullSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, PREVIEW) + (PRIV, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        // (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination4);

        // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.ANALYSIS));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination5);

        // (YUV, ANALYSIS) + (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.ANALYSIS));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination6);

        return combinationList;
    }

    List<SurfaceCombination> getRAWSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        // (YUV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination4);

        // (PRIV, PREVIEW) + (YUV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination5);

        // (YUV, PREVIEW) + (YUV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination6);

        // (PRIV, PREVIEW) + (JPEG, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination7);

        // (YUV, PREVIEW) + (JPEG, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination8);

        return combinationList;
    }

    List<SurfaceCombination> getBurstSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, PREVIEW) + (PRIV, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        // (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        return combinationList;
    }

    List<SurfaceCombination> getLevel3SupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (YUV, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.ANALYSIS));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (JPEG, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.ANALYSIS));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        return combinationList;
    }

    private void generateSupportedCombinationList() {
        mSurfaceCombinations.addAll(getLegacySupportedCombinationList());

        if (mHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
                || mHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                || mHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) {
            mSurfaceCombinations.addAll(getLimitedSupportedCombinationList());
        }

        if (mHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                || mHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) {
            mSurfaceCombinations.addAll(getFullSupportedCombinationList());
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

        if (mIsRawSupported) {
            mSurfaceCombinations.addAll(getRAWSupportedCombinationList());
        }

        if (mIsBurstCaptureSupported
                && mHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) {
            mSurfaceCombinations.addAll(getBurstSupportedCombinationList());
        }

        if (mHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) {
            mSurfaceCombinations.addAll(getLevel3SupportedCombinationList());
        }

        mSurfaceCombinations.addAll(mExtraSupportedSurfaceCombinationsContainer.get());
    }

    private void checkCustomization() {
        // TODO(b/119466260): Integrate found feasible stream combinations into supported list
    }

    // Utility classes and methods:
    // *********************************************************************************************

    private void generateSurfaceSizeDefinition(WindowManager windowManager) {
        Size analysisSize = new Size(640, 480);
        Size previewSize = getPreviewSize(windowManager);
        Size recordSize = getRecordSize();
        mSurfaceSizeDefinition =
                SurfaceSizeDefinition.create(analysisSize, previewSize, recordSize);
    }

    /**
     * PREVIEW refers to the best size match to the device's screen resolution, or to 1080p
     * (1920x1080), whichever is smaller.
     */
    @SuppressWarnings("deprecation") /* defaultDisplay */
    @NonNull
    public static Size getPreviewSize(@NonNull WindowManager windowManager) {
        Point displaySize = new Point();
        windowManager.getDefaultDisplay().getRealSize(displaySize);

        Size displayViewSize;
        if (displaySize.x > displaySize.y) {
            displayViewSize = new Size(displaySize.x, displaySize.y);
        } else {
            displayViewSize = new Size(displaySize.y, displaySize.x);
        }

        // Limit the max preview size to under min(display size, 1080P) by comparing the area size
        Size previewSize =
                Collections.min(
                        Arrays.asList(
                                new Size(displayViewSize.getWidth(), displayViewSize.getHeight()),
                                MAX_PREVIEW_SIZE),
                        new CompareSizesByArea());

        return previewSize;
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

    /** Comparator based on area of the given {@link Size} objects. */
    static final class CompareSizesByArea implements Comparator<Size> {
        private boolean mReverse = false;

        CompareSizesByArea() {
        }

        CompareSizesByArea(boolean reverse) {
            mReverse = reverse;
        }

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            int result =
                    Long.signum(
                            (long) lhs.getWidth() * lhs.getHeight()
                                    - (long) rhs.getWidth() * rhs.getHeight());

            if (mReverse) {
                result *= -1;
            }

            return result;
        }
    }

    /** Comparator based on how close they are to the target aspect ratio. */
    static final class CompareAspectRatiosByDistanceToTargetRatio implements Comparator<Rational> {
        private Rational mTargetRatio;

        CompareAspectRatiosByDistanceToTargetRatio(Rational targetRatio) {
            mTargetRatio = targetRatio;
        }

        @Override
        public int compare(Rational lhs, Rational rhs) {
            if (lhs.equals(rhs)) {
                return 0;
            }

            final Float lhsRatioDelta = Math.abs(lhs.floatValue() - mTargetRatio.floatValue());
            final Float rhsRatioDelta = Math.abs(rhs.floatValue() - mTargetRatio.floatValue());

            int result = (int) Math.signum(lhsRatioDelta - rhsRatioDelta);
            return result;
        }
    }
}
