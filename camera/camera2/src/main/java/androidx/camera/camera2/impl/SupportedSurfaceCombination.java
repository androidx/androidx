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
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.os.Build;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

import androidx.camera.core.CameraDeviceConfig;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageFormatConstants;
import androidx.camera.core.ImageOutputConfig;
import androidx.camera.core.SurfaceCombination;
import androidx.camera.core.SurfaceConfig;
import androidx.camera.core.SurfaceConfig.ConfigSize;
import androidx.camera.core.SurfaceConfig.ConfigType;
import androidx.camera.core.SurfaceSizeDefinition;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseConfig;

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
    private static final Size MAX_PREVIEW_SIZE = new Size(1920, 1080);
    private static final Size DEFAULT_SIZE = new Size(640, 480);
    private static final Size ZERO_SIZE = new Size(0, 0);
    private static final Size QUALITY_2160P_SIZE = new Size(3840, 2160);
    private static final Size QUALITY_1080P_SIZE = new Size(1920, 1080);
    private static final Size QUALITY_720P_SIZE = new Size(1280, 720);
    private static final Size QUALITY_480P_SIZE = new Size(720, 480);
    private final List<SurfaceCombination> mSurfaceCombinations = new ArrayList<>();
    private final Map<Integer, Size> mMaxSizeCache = new HashMap<>();
    private String mCameraId;
    private CameraCharacteristics mCharacteristics;
    private int mHardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    private boolean mIsRawSupported = false;
    private boolean mIsBurstCaptureSupported = false;
    private SurfaceSizeDefinition mSurfaceSizeDefinition;
    private CamcorderProfileHelper mCamcorderProfileHelper;

    SupportedSurfaceCombination(
            Context context, String cameraId, CamcorderProfileHelper camcorderProfileHelper) {
        mCameraId = cameraId;
        mCamcorderProfileHelper = camcorderProfileHelper;
        init(context);
    }

    private SupportedSurfaceCombination() {
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

        if (getAllOutputSizesByFormat(imageFormat) == null) {
            throw new IllegalArgumentException(
                    "Can not get supported output size for the format: " + imageFormat);
        }

        /**
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

    Map<UseCase, Size> getSuggestedResolutions(
            List<UseCase> originalUseCases, List<UseCase> newUseCases) {
        Map<UseCase, Size> suggestedResolutionsMap = new HashMap<>();

        // Get the index order list by the use case priority for finding stream configuration
        List<Integer> useCasesPriorityOrder = getUseCasesPriorityOrder(newUseCases);
        List<List<Size>> supportedOutputSizesList = new ArrayList<>();

        // Collect supported output sizes for all use cases
        for (Integer index : useCasesPriorityOrder) {
            List<Size> supportedOutputSizes = getSupportedOutputSizes(newUseCases.get(index));
            supportedOutputSizesList.add(supportedOutputSizes);
        }

        // Get all possible size arrangements
        List<List<Size>> allPossibleSizeArrangements =
                getAllPossibleSizeArrangements(supportedOutputSizesList);

        // Transform use cases to SurfaceConfig list and find the first (best) workable combination
        for (List<Size> possibleSizeList : allPossibleSizeArrangements) {
            List<SurfaceConfig> surfaceConfigList = new ArrayList<>();

            // Attach SurfaceConfig of original use cases since it will impact the new use cases
            if (originalUseCases != null) {
                for (UseCase useCase : originalUseCases) {
                    CameraDeviceConfig config = (CameraDeviceConfig) useCase.getUseCaseConfig();
                    String useCaseCameraId;
                    try {
                        useCaseCameraId = CameraX.getCameraWithLensFacing(config.getLensFacing());
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                                "Unable to get camera ID for use case " + useCase.getName(), e);
                    }
                    Size resolution = useCase.getAttachedSurfaceResolution(useCaseCameraId);

                    surfaceConfigList.add(
                            transformSurfaceConfig(useCase.getImageFormat(), resolution));
                }
            }

            // Attach SurfaceConfig of new use cases
            for (int i = 0; i < possibleSizeList.size(); i++) {
                Size size = possibleSizeList.get(i);
                UseCase newUseCase =
                        newUseCases.get(useCasesPriorityOrder.get(i));
                surfaceConfigList.add(transformSurfaceConfig(newUseCase.getImageFormat(), size));
            }

            // Check whether the SurfaceConfig combination can be supported
            if (checkSupported(surfaceConfigList)) {
                for (UseCase useCase : newUseCases) {
                    suggestedResolutionsMap.put(
                            useCase,
                            possibleSizeList.get(
                                    useCasesPriorityOrder.indexOf(newUseCases.indexOf(useCase))));
                }
                break;
            }
        }

        return suggestedResolutionsMap;
    }

    // If the device is LEGACY + Android 5.0, the aspect ratio need to be corrected, because
    // there is a bug which was fixed in L MR1.
    boolean requiresCorrectedAspectRatio() {
        return mHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                && Build.VERSION.SDK_INT == 21;
    }

    // Gets the corrected aspect ratio due to device constraints or null if no correction is needed.
    Rational getCorrectedAspectRatio(UseCaseConfig<?> useCaseConfig) {
        Rational outputRatio = null;
        /**
         * If the device is LEGACY + Android 5.0, then return the same aspect ratio as maximum JPEG
         * resolution. The Camera2 LEGACY mode API always sends the HAL a configure call with the
         * same aspect ratio as the maximum JPEG resolution, and do the cropping/scaling before
         * returning the output. There is a bug because of a flipped scaling factor in the
         * intermediate texture transform matrix, and it was fixed in L MR1.
         */
        if (mHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                && Build.VERSION.SDK_INT == 21) {
            Size maxJpegSize = fetchMaxSize(ImageFormat.JPEG);
            outputRatio = new Rational(maxJpegSize.getWidth(), maxJpegSize.getHeight());
            ImageOutputConfig config = (ImageOutputConfig) useCaseConfig;
            int targetRotation = config.getTargetRotation(Surface.ROTATION_0);
            outputRatio = rotateAspectRatioByRotation(outputRatio, targetRotation);
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

    private List<Integer> getUseCasesPriorityOrder(List<UseCase> newUseCases) {
        List<Integer> priorityOrder = new ArrayList<>();

        /**
         * Once the stream resource is occupied by one use case, it will impact the other use cases.
         * Therefore, we need to define the priority for stream resource usage. For the use cases
         * with the higher priority, we will try to find the best one for them in priority as
         * possible.
         */
        List<Integer> priorityValueList = new ArrayList<>();

        for (UseCase useCase : newUseCases) {
            UseCaseConfig<?> config = useCase.getUseCaseConfig();
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
            for (UseCase useCase : newUseCases) {
                UseCaseConfig<?> config = useCase.getUseCaseConfig();
                if (priorityValue == config.getSurfaceOccupancyPriority(0)) {
                    priorityOrder.add(newUseCases.indexOf(useCase));
                }
            }
        }

        return priorityOrder;
    }

    private List<Size> getSupportedOutputSizes(UseCase useCase) {
        int imageFormat = useCase.getImageFormat();
        Size[] outputSizes = getAllOutputSizesByFormat(imageFormat);
        List<Size> outputSizeCandidates = new ArrayList<>();
        ImageOutputConfig config = (ImageOutputConfig) useCase.getUseCaseConfig();
        Size maxSize = config.getMaxResolution(getMaxOutputSizeByFormat(imageFormat));

        // Sort the output sizes. The Comparator result must be reversed to have a descending order
        // result.
        Arrays.sort(outputSizes, new CompareSizesByArea(true));

        // Filter out the ones that exceed the maximum size
        for (Size outputSize : outputSizes) {
            if (outputSize.getWidth() * outputSize.getHeight()
                    <= maxSize.getWidth() * maxSize.getHeight()) {
                outputSizeCandidates.add(outputSize);
            }
        }

        if (outputSizeCandidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Can not get supported output size under supported maximum for the format: "
                            + imageFormat);
        }

        // Check whether the desired default resolution is included in the original supported list
        boolean isDefaultResolutionSupported = outputSizeCandidates.contains(DEFAULT_SIZE);

        // If the target resolution is set, use it to find the minimum one from big enough items
        Size targetSize = config.getTargetResolution(ZERO_SIZE);

        if (!targetSize.equals(ZERO_SIZE)) {
            int indexBigEnough = 0;

            // Get the index of the item that is big enough for the view size
            for (int i = 0; i < outputSizeCandidates.size(); i++) {
                Size outputSize = outputSizeCandidates.get(i);
                if (outputSize.getWidth() * outputSize.getHeight()
                        >= targetSize.getWidth() * targetSize.getHeight()) {
                    indexBigEnough = i;
                } else {
                    break;
                }
            }

            // Remove the additional items that is larger than the big enough item
            outputSizeCandidates.subList(0, indexBigEnough).clear();
        }

        if (outputSizeCandidates.isEmpty() && !isDefaultResolutionSupported) {
            throw new IllegalArgumentException(
                    "Can not get supported output size for the desired output size quality for "
                            + "the format: "
                            + imageFormat);
        }

        // Rearrange the supported size to put the ones with the same aspect ratio in the front
        // of the list and put others in the end from large to small. Some low end devices may
        // not able to get an supported resolution that match the preferred aspect ratio.
        List<Size> sizesMatchAspectRatio = new ArrayList<>();
        List<Size> sizesNotMatchAspectRatio = new ArrayList<>();

        Rational aspectRatio = config.getTargetAspectRatio(null);
        int targetRotation = config.getTargetRotation(Surface.ROTATION_0);
        aspectRatio = rotateAspectRatioByRotation(aspectRatio, targetRotation);

        for (Size outputSize : outputSizeCandidates) {
            // If target aspect ratio is set, moves the matched results to the front of the list.
            if (aspectRatio != null && aspectRatio.equals(
                    new Rational(outputSize.getWidth(), outputSize.getHeight()))) {
                sizesMatchAspectRatio.add(outputSize);
            } else {
                sizesNotMatchAspectRatio.add(outputSize);
            }
        }

        List<Size> supportedResolutions = new ArrayList<>();
        // No need to sort again since the source list has been sorted previously
        supportedResolutions.addAll(sizesMatchAspectRatio);
        supportedResolutions.addAll(sizesNotMatchAspectRatio);

        // If there is no available size for the conditions and default resolution is in the
        // supported
        // list, return the default resolution.
        if (supportedResolutions.isEmpty() && !isDefaultResolutionSupported) {
            supportedResolutions.add(DEFAULT_SIZE);
        }

        return supportedResolutions;
    }

    // Use target rotation to calibrate the aspect ratio.
    private Rational rotateAspectRatioByRotation(Rational aspectRatio, int targetRotation) {
        Rational outputRatio = aspectRatio;
        int sensorRotationDegrees;
        try {
            sensorRotationDegrees = CameraX.getCameraInfo(mCameraId).getSensorRotationDegrees(
                    targetRotation);
        } catch (CameraInfoUnavailableException e) {
            throw new IllegalArgumentException("Unable to retrieve camera sensor orientation.", e);
        }
        // Calibrates the aspect ratio with the display and sensor rotation degrees values.
        // Otherwise, retrieves default aspect ratio for the target use case.
        if (aspectRatio != null && (sensorRotationDegrees == 90 || sensorRotationDegrees == 270)) {
            outputRatio = new Rational(aspectRatio.getDenominator(),
                    aspectRatio.getNumerator());
        }
        return outputRatio;
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

        /**
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

    private Size[] getAllOutputSizesByFormat(int imageFormat) {
        if (mCharacteristics == null) {
            throw new IllegalStateException("CameraCharacteristics is null.");
        }

        StreamConfigurationMap map =
                mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
            throw new IllegalArgumentException(
                    "Can not get supported output size for the format: " + imageFormat);
        }

        Size[] outputSizes;
        if (Build.VERSION.SDK_INT < 23
                && imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
            // This is a little tricky that 0x22 that is internal defined in
            // StreamConfigurationMap.java
            // to be equal to ImageFormat.PRIVATE that is public after Android level 23 but not
            // public in
            // Android L. Use {@link SurfaceTexture} or {@link MediaCodec} will finally mapped to
            // 0x22 in
            // StreamConfigurationMap to retrieve the output sizes information.
            outputSizes = map.getOutputSizes(SurfaceTexture.class);
        } else {
            outputSizes = map.getOutputSizes(imageFormat);
        }

        if (outputSizes == null) {
            throw new IllegalArgumentException(
                    "Can not get supported output size for the format: " + imageFormat);
        }

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

    private void init(Context context) {
        CameraManager cameraManager =
                (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        try {
            generateSupportedCombinationList(cameraManager);
            generateSurfaceSizeDefinition(windowManager);
        } catch (CameraAccessException e) {
            throw new IllegalArgumentException(
                    "Generate supported combination list and size definition fail - CameraId:"
                            + mCameraId,
                    e);
        }
        checkCustomization();
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

    private void generateSupportedCombinationList(CameraManager cameraManager)
            throws CameraAccessException {
        mCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);

        Integer keyValue = mCharacteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

        if (keyValue != null) {
            mHardwareLevel = keyValue;
        }

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
    private Size getPreviewSize(WindowManager windowManager) {
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
    private Size getRecordSize() {
        Size recordSize = QUALITY_480P_SIZE;

        // Check whether 2160P, 1080P, 720P, 480P are supported by CamcorderProfile
        if (mCamcorderProfileHelper.hasProfile(
                Integer.parseInt(mCameraId), CamcorderProfile.QUALITY_2160P)) {
            recordSize = QUALITY_2160P_SIZE;
        } else if (mCamcorderProfileHelper.hasProfile(
                Integer.parseInt(mCameraId), CamcorderProfile.QUALITY_1080P)) {
            recordSize = QUALITY_1080P_SIZE;
        } else if (mCamcorderProfileHelper.hasProfile(
                Integer.parseInt(mCameraId), CamcorderProfile.QUALITY_720P)) {
            recordSize = QUALITY_720P_SIZE;
        } else if (mCamcorderProfileHelper.hasProfile(
                Integer.parseInt(mCameraId), CamcorderProfile.QUALITY_480P)) {
            recordSize = QUALITY_480P_SIZE;
        }

        return recordSize;
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
}
