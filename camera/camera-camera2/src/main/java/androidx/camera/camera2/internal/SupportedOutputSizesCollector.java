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

import static androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_16_9;
import static androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_3_4;
import static androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_4_3;
import static androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_9_16;
import static androidx.camera.core.impl.utils.AspectRatioUtil.hasMatchingAspectRatio;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.StreamConfigurationMapCompat;
import androidx.camera.camera2.internal.compat.workaround.ExcludedSupportedSizesContainer;
import androidx.camera.camera2.internal.compat.workaround.ResolutionCorrector;
import androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Logger;
import androidx.camera.core.ResolutionSelector;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.SizeCoordinate;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.utils.AspectRatioUtil;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.internal.utils.SizeUtil;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The supported output sizes collector to help collect the available resolution candidate list
 * according to the use case config and the following settings in {@link ResolutionSelector}:
 *
 * 1. Preferred aspect ratio
 * 2. Preferred resolution
 * 3. Max resolution
 * 4. Is high resolution enabled
 *
 * The problematic resolutions retrieved from {@link ExcludedSupportedSizesContainer} will also
 * be execulded.
 */
@RequiresApi(21)
final class SupportedOutputSizesCollector {
    private static final String TAG = "SupportedOutputSizesCollector";
    private final String mCameraId;
    @NonNull
    private final CameraCharacteristicsCompat mCharacteristics;
    @NonNull
    private final DisplayInfoManager mDisplayInfoManager;
    private final ResolutionCorrector mResolutionCorrector = new ResolutionCorrector();
    private final Map<Integer, Size[]> mOutputSizesCache = new HashMap<>();
    private final Map<Integer, Size[]> mHighResolutionOutputSizesCache = new HashMap<>();
    private final Map<Integer, Size> mMaxSizeCache = new HashMap<>();
    private final ExcludedSupportedSizesContainer mExcludedSupportedSizesContainer;
    private final Map<Integer, List<Size>> mExcludedSizeListCache = new HashMap<>();
    private final boolean mIsSensorLandscapeResolution;
    private final boolean mIsBurstCaptureSupported;
    private final Size mActiveArraySize;
    private final int mSensorOrientation;
    private final int mLensFacing;

    SupportedOutputSizesCollector(@NonNull String cameraId,
            @NonNull CameraCharacteristicsCompat cameraCharacteristics,
            @NonNull DisplayInfoManager displayInfoManager) {
        mCameraId = cameraId;
        mCharacteristics = cameraCharacteristics;
        mDisplayInfoManager = displayInfoManager;

        mExcludedSupportedSizesContainer = new ExcludedSupportedSizesContainer(cameraId);

        mIsSensorLandscapeResolution = isSensorLandscapeResolution(mCharacteristics);
        mIsBurstCaptureSupported = isBurstCaptureSupported();

        Rect rect = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        mActiveArraySize = rect != null ? new Size(rect.width(), rect.height()) : null;

        mSensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        mLensFacing = mCharacteristics.get(CameraCharacteristics.LENS_FACING);
    }

    /**
     * Collects and sorts the resolution candidate list by the following steps:
     *
     * 1. Collects the candidate list by the high resolution enable setting.
     * 2. Filters out the candidate list according to the min size bound, max resolution or
     * excluded resolution quirk.
     * 3. Sorts the candidate list according to the rules of legacy resolution API or new
     * Resolution API.
     * 4. Forces select specific resolutions according to ResolutionCorrector workaround.
     */
    @NonNull
    List<Size> getSupportedOutputSizes(@NonNull ResolutionSelector resolutionSelector,
            int imageFormat, @Nullable Size miniBoundingSize, boolean isHighResolutionDisabled,
            @Nullable Size[] customizedSupportSizes) {
        // 1. Collects the candidate list by the high resolution enable setting.
        List<Size> resolutionCandidateList = collectResolutionCandidateList(resolutionSelector,
                imageFormat, isHighResolutionDisabled, customizedSupportSizes);

        // 2. Filters out the candidate list according to the min size bound, max resolution or
        // excluded resolution quirk.
        resolutionCandidateList = filterOutResolutionCandidateListBySettings(
                resolutionCandidateList, resolutionSelector, imageFormat);

        // 3. Sorts the candidate list according to the rules of new Resolution API.
        resolutionCandidateList = sortResolutionCandidateListByResolutionSelector(
                resolutionCandidateList, resolutionSelector,
                mDisplayInfoManager.getMaxSizeDisplay().getRotation(), miniBoundingSize);

        // 4. Forces select specific resolutions according to ResolutionCorrector workaround.
        resolutionCandidateList = mResolutionCorrector.insertOrPrioritize(
                SurfaceConfig.getConfigType(imageFormat), resolutionCandidateList);

        return resolutionCandidateList;
    }

    /**
     * Collects the resolution candidate list.
     *
     * 1. Customized supported resolutions list will be returned when it exists
     * 2. Otherwise, the sizes retrieved from {@link StreamConfigurationMap#getOutputSizes(int)}
     * will be the base of the resolution candidate list.
     * 3. High resolution sizes retrieved from
     * {@link StreamConfigurationMap#getHighResolutionOutputSizes(int)} will be included when
     * {@link ResolutionSelector#isHighResolutionEnabled()} returns true.
     *
     * The returned list will be sorted in descending order and duplicate items will be removed.
     */
    @NonNull
    private List<Size> collectResolutionCandidateList(
            @NonNull ResolutionSelector resolutionSelector, int imageFormat,
            boolean isHighResolutionDisabled, @Nullable Size[] customizedSupportedSizes) {
        Size[] outputSizes = customizedSupportedSizes;

        if (outputSizes == null) {
            boolean highResolutionEnabled =
                    !isHighResolutionDisabled && resolutionSelector.isHighResolutionEnabled();
            outputSizes = getAllOutputSizesByFormat(imageFormat, highResolutionEnabled);
        }

        // Sort the output sizes. The Comparator result must be reversed to have a descending order
        // result.
        Arrays.sort(outputSizes, new CompareSizesByArea(true));

        List<Size> resultList = Arrays.asList(outputSizes);

        if (resultList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Resolution candidate list is empty when collecting by the settings!");
        }

        return resultList;
    }

    /**
     * Filters out the resolution candidate list by the max resolution setting.
     *
     * The input size list should have been sorted in descending order.
     */
    private List<Size> filterOutResolutionCandidateListBySettings(
            @NonNull List<Size> resolutionCandidateList,
            @NonNull ResolutionSelector resolutionSelector, int imageFormat) {
        // Retrieves the max resolution setting. When ResolutionSelector is used, all resolution
        // selection logic should depend on ResolutionSelector's settings.
        Size maxResolution = resolutionSelector.getMaxResolution();

        // Filter out the resolution candidate list by the max resolution. Sizes that any edge
        // exceeds the max resolution will be filtered out.
        List<Size> resultList;

        if (maxResolution == null) {
            resultList = new ArrayList<>(resolutionCandidateList);
        } else {
            resultList = new ArrayList<>();
            for (Size outputSize : resolutionCandidateList) {
                if (!SizeUtil.isLongerInAnyEdge(outputSize, maxResolution)) {
                    resultList.add(outputSize);
                }
            }
        }

        resultList = excludeProblematicSizes(resultList, imageFormat);

        if (resultList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Resolution candidate list is empty after filtering out by the settings!");
        }

        return resultList;
    }

    /**
     * Sorts the resolution candidate list according to the new ResolutionSelector API logic.
     *
     * The list will be sorted by the following order:
     * 1. size of preferred resolution
     * 2. a resolution with preferred aspect ratio, is not smaller than, and is closest to the
     * preferred resolution.
     * 3. resolutions with preferred aspect ratio and is smaller than the preferred resolution
     * size in descending order of resolution area size.
     * 4. Other sizes sorted by CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace and
     * area size.
     */
    @NonNull
    private List<Size> sortResolutionCandidateListByResolutionSelector(
            @NonNull List<Size> resolutionCandidateList,
            @NonNull ResolutionSelector resolutionSelector,
            @ImageOutputConfig.RotationValue int targetRotation,
            @Nullable Size miniBoundingSize) {
        Rational aspectRatio = getTargetAspectRatioByResolutionSelector(resolutionSelector);
        Preconditions.checkNotNull(aspectRatio, "ResolutionSelector should also have aspect ratio"
                + " value.");

        Size targetSize = getTargetSizeByResolutionSelector(resolutionSelector, targetRotation,
                mSensorOrientation, mLensFacing);
        List<Size> resultList = sortResolutionCandidateListByTargetAspectRatioAndSize(
                resolutionCandidateList, aspectRatio, miniBoundingSize);

        // Moves the target size to the first position if it exists in the resolution candidate
        // list and there is no quirk that needs to select specific aspect ratio sizes in priority.
        if (resultList.contains(targetSize) && canResolutionBeMovedToHead(targetSize)) {
            resultList.remove(targetSize);
            resultList.add(0, targetSize);
        }

        return resultList;
    }

    @NonNull
    private Size[] getAllOutputSizesByFormat(int imageFormat, boolean highResolutionEnabled) {
        Size[] outputs = mOutputSizesCache.get(imageFormat);
        if (outputs == null) {
            outputs = doGetOutputSizesByFormat(imageFormat);
            mOutputSizesCache.put(imageFormat, outputs);
        }

        Size[] highResolutionOutputs = null;

        // A device that does not support the BURST_CAPTURE capability,
        // StreamConfigurationMap#getHighResolutionOutputSizes() will return null.
        if (highResolutionEnabled && mIsBurstCaptureSupported) {
            highResolutionOutputs = mHighResolutionOutputSizesCache.get(imageFormat);

            // High resolution output sizes list may be empty. If it is empty and cached in the
            // map, don't need to query it again.
            if (highResolutionOutputs == null && !mHighResolutionOutputSizesCache.containsKey(
                    imageFormat)) {
                highResolutionOutputs = doGetHighResolutionOutputSizesByFormat(imageFormat);
                mHighResolutionOutputSizesCache.put(imageFormat, highResolutionOutputs);
            }
        }

        // Combines output sizes if high resolution sizes list is not empty.
        if (highResolutionOutputs != null) {
            Size[] allOutputs = Arrays.copyOf(highResolutionOutputs,
                    highResolutionOutputs.length + outputs.length);
            System.arraycopy(outputs, 0, allOutputs, highResolutionOutputs.length, outputs.length);
            outputs = allOutputs;
        }

        return outputs;
    }

    @NonNull
    private Size[] doGetOutputSizesByFormat(int imageFormat) {
        StreamConfigurationMap map =
                mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
            throw new IllegalArgumentException("Can not retrieve SCALER_STREAM_CONFIGURATION_MAP");
        }

        StreamConfigurationMapCompat mapCompat =
                StreamConfigurationMapCompat.toStreamConfigurationMapCompat(map);
        Size[] outputSizes = mapCompat.getOutputSizes(imageFormat);
        if (outputSizes == null) {
            throw new IllegalArgumentException(
                    "Can not get supported output size for the format: " + imageFormat);
        }

        return outputSizes;
    }

    @Nullable
    private Size[] doGetHighResolutionOutputSizesByFormat(int imageFormat) {
        if (Build.VERSION.SDK_INT < 23) {
            return null;
        }

        Size[] outputSizes;

        StreamConfigurationMap map =
                mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
            throw new IllegalArgumentException("Can not retrieve SCALER_STREAM_CONFIGURATION_MAP");
        }

        outputSizes = Api23Impl.getHighResolutionOutputSizes(map, imageFormat);

        return outputSizes;
    }

    /**
     * Returns the target aspect ratio value corrected by quirks.
     *
     * The final aspect ratio is determined by the following order:
     * 1. The aspect ratio returned by {@link TargetAspectRatio} if it is
     * {@link TargetAspectRatio#RATIO_4_3}, {@link TargetAspectRatio#RATIO_16_9} or
     * {@link TargetAspectRatio#RATIO_MAX_JPEG}.
     * 2. The use case's original aspect ratio if {@link TargetAspectRatio} returns
     * {@link TargetAspectRatio#RATIO_ORIGINAL} and the use case has target aspect ratio setting.
     *
     * @param resolutionSelector the resolution selector of the use case.
     */
    @Nullable
    private Rational getTargetAspectRatioByResolutionSelector(
            @NonNull ResolutionSelector resolutionSelector) {
        Rational outputRatio = getTargetAspectRatioFromQuirk();

        if (outputRatio == null) {
            @AspectRatio.Ratio int aspectRatio = resolutionSelector.getPreferredAspectRatio();
            switch (aspectRatio) {
                case AspectRatio.RATIO_4_3:
                    outputRatio = mIsSensorLandscapeResolution ? ASPECT_RATIO_4_3
                            : ASPECT_RATIO_3_4;
                    break;
                case AspectRatio.RATIO_16_9:
                    outputRatio = mIsSensorLandscapeResolution ? ASPECT_RATIO_16_9
                            : ASPECT_RATIO_9_16;
                    break;
                case AspectRatio.RATIO_DEFAULT:
                    break;
                default:
                    Logger.e(TAG, "Undefined target aspect ratio: " + aspectRatio);
            }
        }
        return outputRatio;
    }

    /**
     * Returns the restricted target aspect ratio value from quirk. The returned value can be
     * null which means that no quirk to restrict the use case to use a specific target aspect
     * ratio value.
     */
    @Nullable
    private Rational getTargetAspectRatioFromQuirk() {
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
                Size maxJpegSize = fetchMaxNormalOutputSize(ImageFormat.JPEG);
                outputRatio = new Rational(maxJpegSize.getWidth(), maxJpegSize.getHeight());
                break;
            case TargetAspectRatio.RATIO_ORIGINAL:
                break;
        }

        return outputRatio;
    }

    @Nullable
    static Size getTargetSizeByResolutionSelector(@NonNull ResolutionSelector resolutionSelector,
            int targetRotation, int sensorOrientation, int lensFacing) {
        Size targetSize = resolutionSelector.getPreferredResolution();

        // Calibrate targetSize by the target rotation value if it is set by the Android View
        // coordinate orientation.
        if (resolutionSelector.getSizeCoordinate() == SizeCoordinate.ANDROID_VIEW) {
            targetSize = flipSizeByRotation(targetSize, targetRotation, lensFacing,
                    sensorOrientation);
        }
        return targetSize;
    }

    private static boolean isRotationNeeded(int targetRotation, int lensFacing,
            int sensorOrientation) {
        int relativeRotationDegrees =
                CameraOrientationUtil.surfaceRotationToDegrees(targetRotation);

        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        boolean isOppositeFacingScreen = CameraCharacteristics.LENS_FACING_BACK == lensFacing;

        int sensorRotationDegrees = CameraOrientationUtil.getRelativeImageRotation(
                relativeRotationDegrees,
                sensorOrientation,
                isOppositeFacingScreen);
        return sensorRotationDegrees == 90 || sensorRotationDegrees == 270;
    }

    @NonNull
    private List<Size> excludeProblematicSizes(@NonNull List<Size> resolutionCandidateList,
            int imageFormat) {
        List<Size> excludedSizes = fetchExcludedSizes(imageFormat);
        resolutionCandidateList.removeAll(excludedSizes);
        return resolutionCandidateList;
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

    /**
     * Sorts the resolution candidate list according to the target aspect ratio and size settings.
     *
     * 1. The resolution candidate list will be grouped by aspect ratio.
     * 2. Each group only keeps one size which is not smaller than the target size.
     * 3. The aspect ratios of groups will be sorted against to the target aspect ratio setting by
     * CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace.
     * 4. Concatenate all sizes as the result list
     */
    @NonNull
    private List<Size> sortResolutionCandidateListByTargetAspectRatioAndSize(
            @NonNull List<Size> resolutionCandidateList, @NonNull Rational aspectRatio,
            @Nullable Size miniBoundingSize) {
        // Rearrange the supported size to put the ones with the same aspect ratio in the front
        // of the list and put others in the end from large to small. Some low end devices may
        // not able to get an supported resolution that match the preferred aspect ratio.

        // Group output sizes by aspect ratio.
        Map<Rational, List<Size>> aspectRatioSizeListMap =
                groupSizesByAspectRatio(resolutionCandidateList);

        // If the target resolution is set, use it to remove unnecessary larger sizes.
        if (miniBoundingSize != null) {
            // Remove unnecessary larger sizes from each aspect ratio size list
            for (Rational key : aspectRatioSizeListMap.keySet()) {
                removeSupportedSizesByMiniBoundingSize(aspectRatioSizeListMap.get(key),
                        miniBoundingSize);
            }
        }

        // Sort the aspect ratio key set by the target aspect ratio.
        List<Rational> aspectRatios = new ArrayList<>(aspectRatioSizeListMap.keySet());
        Rational fullFovRatio = mActiveArraySize != null ? new Rational(
                mActiveArraySize.getWidth(), mActiveArraySize.getHeight()) : null;
        Collections.sort(aspectRatios,
                new AspectRatioUtil.CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace(
                        aspectRatio, fullFovRatio));

        List<Size> resultList = new ArrayList<>();

        // Put available sizes into final result list by aspect ratio distance to target ratio.
        for (Rational rational : aspectRatios) {
            for (Size size : aspectRatioSizeListMap.get(rational)) {
                // A size may exist in multiple groups in mod16 condition. Keep only one in
                // the final list.
                if (!resultList.contains(size)) {
                    resultList.add(size);
                }
            }
        }

        return resultList;
    }

    /**
     * Returns {@code true} if the input resolution can be moved to the head of resolution
     * candidate list.
     *
     * The resolution possibly can't be moved to head due to some quirks that sizes of
     * specific aspect ratio must be used to avoid problems.
     */
    private boolean canResolutionBeMovedToHead(@NonNull Size resolution) {
        @TargetAspectRatio.Ratio int targetAspectRatio =
                new TargetAspectRatio().get(mCameraId, mCharacteristics);

        switch (targetAspectRatio) {
            case TargetAspectRatio.RATIO_4_3:
                return hasMatchingAspectRatio(resolution, ASPECT_RATIO_4_3);
            case TargetAspectRatio.RATIO_16_9:
                return hasMatchingAspectRatio(resolution, ASPECT_RATIO_16_9);
            case TargetAspectRatio.RATIO_MAX_JPEG:
                Size maxJpegSize = fetchMaxNormalOutputSize(ImageFormat.JPEG);
                Rational maxJpegRatio = new Rational(maxJpegSize.getWidth(),
                        maxJpegSize.getHeight());
                return hasMatchingAspectRatio(resolution, maxJpegRatio);
        }

        return true;
    }

    private Size fetchMaxNormalOutputSize(int imageFormat) {
        Size size = mMaxSizeCache.get(imageFormat);
        if (size != null) {
            return size;
        }
        Size maxSize = getMaxNormalOutputSizeByFormat(imageFormat);
        mMaxSizeCache.put(imageFormat, maxSize);
        return maxSize;
    }

    /**
     * Gets max normal supported output size for specific image format.
     *
     * <p>Normal supported output sizes mean the sizes retrieved by the
     * {@link StreamConfigurationMap#getOutputSizes(int)}. The high resolution sizes retrieved by
     * the {@link StreamConfigurationMap#getHighResolutionOutputSizes(int)} are not included.
     *
     * @param imageFormat the image format info
     * @return the max normal supported output size for the image format
     */
    private Size getMaxNormalOutputSizeByFormat(int imageFormat) {
        Size[] outputSizes = getAllOutputSizesByFormat(imageFormat, false);

        return SizeUtil.getMaxSize(Arrays.asList(outputSizes));
    }

    private boolean isBurstCaptureSupported() {
        int[] availableCapabilities =
                mCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

        if (availableCapabilities != null) {
            for (int capability : availableCapabilities) {
                if (capability
                        == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE) {
                    return true;
                }
            }
        }

        return false;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // The following functions can be reused by the legacy resolution selection logic. The can be
    // changed as private function after the legacy resolution API is completely removed.
    //////////////////////////////////////////////////////////////////////////////////////////

    // Use target rotation to calibrate the size.
    @Nullable
    static Size flipSizeByRotation(@Nullable Size size, int targetRotation, int lensFacing,
            int sensorOrientation) {
        Size outputSize = size;
        // Calibrates the size with the display and sensor rotation degrees values.
        if (size != null && isRotationNeeded(targetRotation, lensFacing, sensorOrientation)) {
            outputSize = new Size(/* width= */size.getHeight(), /* height= */size.getWidth());
        }
        return outputSize;
    }

    static Map<Rational, List<Size>> groupSizesByAspectRatio(List<Size> sizes) {
        Map<Rational, List<Size>> aspectRatioSizeListMap = new HashMap<>();

        List<Rational> aspectRatioKeys = getResolutionListGroupingAspectRatioKeys(sizes);

        for (Rational aspectRatio: aspectRatioKeys) {
            aspectRatioSizeListMap.put(aspectRatio, new ArrayList<>());
        }

        for (Size outputSize : sizes) {
            for (Rational key : aspectRatioSizeListMap.keySet()) {
                // Put the size into all groups that is matched in mod16 condition since a size
                // may match multiple aspect ratio in mod16 algorithm.
                if (hasMatchingAspectRatio(outputSize, key)) {
                    aspectRatioSizeListMap.get(key).add(outputSize);
                }
            }
        }

        return aspectRatioSizeListMap;
    }

    /**
     * Returns the grouping aspect ratio keys of the input resolution list.
     *
     * <p>Some sizes might be mod16 case. When grouping, those sizes will be grouped into an
     * existing aspect ratio group if the aspect ratio can match by the mod16 rule.
     */
    @NonNull
    static List<Rational> getResolutionListGroupingAspectRatioKeys(
            @NonNull List<Size> resolutionCandidateList) {
        List<Rational> aspectRatios = new ArrayList<>();

        // Adds the default 4:3 and 16:9 items first to avoid their mod16 sizes to create
        // additional items.
        aspectRatios.add(ASPECT_RATIO_4_3);
        aspectRatios.add(ASPECT_RATIO_16_9);

        // Tries to find the aspect ratio which the target size belongs to.
        for (Size size : resolutionCandidateList) {
            Rational newRatio = new Rational(size.getWidth(), size.getHeight());
            boolean aspectRatioFound = aspectRatios.contains(newRatio);

            // The checking size might be a mod16 size which can be mapped to an existing aspect
            // ratio group.
            if (!aspectRatioFound) {
                boolean hasMatchingAspectRatio = false;
                for (Rational aspectRatio : aspectRatios) {
                    if (hasMatchingAspectRatio(size, aspectRatio)) {
                        hasMatchingAspectRatio = true;
                        break;
                    }
                }
                if (!hasMatchingAspectRatio) {
                    aspectRatios.add(newRatio);
                }
            }
        }

        return aspectRatios;
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
     * @param miniBoundingSize The target size used to remove unnecessary sizes.
     */
    static void removeSupportedSizesByMiniBoundingSize(List<Size> supportedSizesList,
            Size miniBoundingSize) {
        if (supportedSizesList == null || supportedSizesList.isEmpty()) {
            return;
        }

        int indexBigEnough = -1;
        List<Size> removeSizes = new ArrayList<>();

        // Get the index of the item that is equal to or closest to the target size.
        for (int i = 0; i < supportedSizesList.size(); i++) {
            Size outputSize = supportedSizesList.get(i);
            if (outputSize.getWidth() >= miniBoundingSize.getWidth()
                    && outputSize.getHeight() >= miniBoundingSize.getHeight()) {
                // New big enough item closer to the target size is found. Adding the previous
                // one into the sizes list that will be removed.
                if (indexBigEnough >= 0) {
                    removeSizes.add(supportedSizesList.get(indexBigEnough));
                }

                indexBigEnough = i;
            } else {
                // If duplicated miniBoundingSize items exist in the list, the size will be added
                // into the removeSizes list. Removes it from the removeSizes list to keep the
                // miniBoundingSize items in the final result list.
                if (indexBigEnough >= 0) {
                    removeSizes.remove(supportedSizesList.get(indexBigEnough));
                }
                break;
            }
        }

        // Remove the unnecessary items that are larger than the item closest to the target size.
        supportedSizesList.removeAll(removeSizes);
    }

    static boolean isSensorLandscapeResolution(
            @NonNull CameraCharacteristicsCompat characteristicsCompat) {
        Size pixelArraySize =
                characteristicsCompat.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);

        // Make the default value is true since usually the sensor resolution is landscape.
        return pixelArraySize == null || pixelArraySize.getWidth() >= pixelArraySize.getHeight();
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // The above functions can be reused by the legacy resolution selection logic. The can be
    // changed as private function after the legacy resolution API is completely removed.
    //////////////////////////////////////////////////////////////////////////////////////////

    @RequiresApi(23)
    private static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Size[] getHighResolutionOutputSizes(StreamConfigurationMap streamConfigurationMap,
                int format) {
            return streamConfigurationMap.getHighResolutionOutputSizes(format);
        }
    }
}
