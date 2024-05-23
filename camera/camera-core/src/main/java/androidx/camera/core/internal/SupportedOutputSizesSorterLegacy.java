/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.internal;

import static androidx.camera.core.impl.utils.AspectRatioUtil.hasMatchingAspectRatio;
import static androidx.camera.core.internal.SupportedOutputSizesSorter.getResolutionListGroupingAspectRatioKeys;
import static androidx.camera.core.internal.SupportedOutputSizesSorter.groupSizesByAspectRatio;
import static androidx.camera.core.internal.SupportedOutputSizesSorter.sortSupportedSizesByFallbackRuleClosestHigherThenLower;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_ZERO;
import static androidx.camera.core.internal.utils.SizeUtil.getArea;

import android.hardware.camera2.CameraCharacteristics;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.AspectRatioUtil;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.camera.core.impl.utils.CompareSizesByArea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class used to sort the supported output sizes according to the legacy use case configs
 */
class SupportedOutputSizesSorterLegacy {
    private static final String TAG = "SupportedOutputSizesCollector";
    private final int mSensorOrientation;
    private final int mLensFacing;
    private final Rational mFullFovRatio;
    private final boolean mIsSensorLandscapeResolution;

    SupportedOutputSizesSorterLegacy(@NonNull CameraInfoInternal cameraInfoInternal,
            @Nullable Rational fullFovRatio) {
        mSensorOrientation = cameraInfoInternal.getSensorRotationDegrees();
        mLensFacing = cameraInfoInternal.getLensFacing();
        mFullFovRatio = fullFovRatio;
        // Determines the sensor resolution orientation info by the full FOV ratio.
        mIsSensorLandscapeResolution = mFullFovRatio != null ? mFullFovRatio.getNumerator()
                >= mFullFovRatio.getDenominator() : true;
    }

    /**
     * Sorts the resolution candidate list by the following steps:
     *
     * 1. Filters out the candidate list according to the mini and max resolution.
     * 2. Sorts the candidate list according to legacy target aspect ratio or resolution settings.
     */
    @NonNull
    List<Size> sortSupportedOutputSizes(
            @NonNull List<Size> resolutionCandidateList,
            @NonNull UseCaseConfig<?> useCaseConfig) {
        if (resolutionCandidateList.isEmpty()) {
            return resolutionCandidateList;
        }

        List<Size> descendingSizeList = new ArrayList<>(resolutionCandidateList);

        // Sort the result sizes. The Comparator result must be reversed to have a descending
        // order result.
        Collections.sort(descendingSizeList, new CompareSizesByArea(true));

        List<Size> filteredSizeList = new ArrayList<>();
        ImageOutputConfig imageOutputConfig = (ImageOutputConfig) useCaseConfig;
        Size maxSize = imageOutputConfig.getMaxResolution(null);
        Size maxSupportedOutputSize = descendingSizeList.get(0);

        // Set maxSize as the max resolution setting or the max supported output size for the
        // image format, whichever is smaller.
        if (maxSize == null || getArea(maxSupportedOutputSize) < getArea(maxSize)) {
            maxSize = maxSupportedOutputSize;
        }

        Size targetSize = getTargetSize(imageOutputConfig);
        Size minSize = RESOLUTION_VGA;
        int defaultSizeArea = getArea(RESOLUTION_VGA);
        int maxSizeArea = getArea(maxSize);
        // When maxSize is smaller than 640x480, set minSize as 0x0. It means the min size bound
        // will be ignored. Otherwise, set the minimal size according to min(DEFAULT_SIZE,
        // TARGET_RESOLUTION).
        if (maxSizeArea < defaultSizeArea) {
            minSize = RESOLUTION_ZERO;
        } else if (targetSize != null && getArea(targetSize) < defaultSizeArea) {
            minSize = targetSize;
        }

        // Filter out the ones that exceed the maximum size and the minimum size. The output
        // sizes candidates list won't have duplicated items.
        for (Size outputSize : descendingSizeList) {
            if (getArea(outputSize) <= getArea(maxSize) && getArea(outputSize) >= getArea(minSize)
                    && !filteredSizeList.contains(outputSize)) {
                filteredSizeList.add(outputSize);
            }
        }

        if (filteredSizeList.isEmpty()) {
            throw new IllegalArgumentException(
                    "All supported output sizes are filtered out according to current resolution "
                            + "selection settings. \nminSize = "
                            + minSize + "\nmaxSize = " + maxSize + "\ninitial size list: "
                            + descendingSizeList);
        }

        Rational aspectRatio = getTargetAspectRatioByLegacyApi(imageOutputConfig, filteredSizeList);

        // Check the default resolution if the target resolution is not set
        targetSize = targetSize == null ? imageOutputConfig.getDefaultResolution(null) : targetSize;

        List<Size> resultSizeList = new ArrayList<>();
        Map<Rational, List<Size>> aspectRatioSizeListMap = new HashMap<>();

        if (aspectRatio == null) {
            // If no target aspect ratio is set, all sizes can be added to the result list
            // directly. No need to sort again since the source list has been sorted previously.
            resultSizeList.addAll(filteredSizeList);

            // If the target resolution is set, use it to sort the sizes list.
            if (targetSize != null) {
                sortSupportedSizesByFallbackRuleClosestHigherThenLower(resultSizeList, targetSize,
                        true);
            }
        } else {
            // Rearrange the supported size to put the ones with the same aspect ratio in the front
            // of the list and put others in the end from large to small. Some low end devices may
            // not able to get an supported resolution that match the preferred aspect ratio.

            // Group output sizes by aspect ratio.
            aspectRatioSizeListMap = groupSizesByAspectRatio(filteredSizeList);

            // If the target resolution is set, sort the sizes against it.
            if (targetSize != null) {
                for (Rational key : aspectRatioSizeListMap.keySet()) {
                    sortSupportedSizesByFallbackRuleClosestHigherThenLower(
                            aspectRatioSizeListMap.get(key), targetSize, true);
                }
            }

            // Sort the aspect ratio key set by the target aspect ratio.
            List<Rational> aspectRatios = new ArrayList<>(aspectRatioSizeListMap.keySet());
            Collections.sort(aspectRatios,
                    new AspectRatioUtil.CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace(
                            aspectRatio, mFullFovRatio));

            // Put available sizes into final result list by aspect ratio distance to target ratio.
            for (Rational rational : aspectRatios) {
                for (Size size : aspectRatioSizeListMap.get(rational)) {
                    // A size may exist in multiple groups in mod16 condition. Keep only one in
                    // the final list.
                    if (!resultSizeList.contains(size)) {
                        resultSizeList.add(size);
                    }
                }
            }
        }

        return resultSizeList;
    }

    /**
     * Returns the target aspect ratio rational value according to the legacy API settings.
     */
    private Rational getTargetAspectRatioByLegacyApi(@NonNull ImageOutputConfig imageOutputConfig,
            @NonNull List<Size> resolutionCandidateList) {
        Rational outputRatio = null;

        if (imageOutputConfig.hasTargetAspectRatio()) {
            @AspectRatio.Ratio int aspectRatio = imageOutputConfig.getTargetAspectRatio();
            outputRatio = SupportedOutputSizesSorter.getTargetAspectRatioRationalValue(aspectRatio,
                    mIsSensorLandscapeResolution);
        } else {
            // The legacy resolution API will use the aspect ratio of the target size to
            // be the fallback target aspect ratio value when the use case has no target
            // aspect ratio setting.
            Size targetSize = getTargetSize(imageOutputConfig);
            if (targetSize != null) {
                outputRatio = getAspectRatioGroupKeyOfTargetSize(targetSize,
                        resolutionCandidateList);
            }
        }

        return outputRatio;
    }

    @Nullable
    private Size getTargetSize(@NonNull ImageOutputConfig imageOutputConfig) {
        int targetRotation = imageOutputConfig.getTargetRotation(Surface.ROTATION_0);
        // Calibrate targetSize by the target rotation value.
        Size targetSize = imageOutputConfig.getTargetResolution(null);
        targetSize = flipSizeByRotation(targetSize, targetRotation, mLensFacing,
                mSensorOrientation);
        return targetSize;
    }

    /**
     * Returns the aspect ratio group key of the target size when grouping the input resolution
     * candidate list.
     *
     * The resolution candidate list will be grouped with mod 16 consideration. Therefore, we
     * also need to consider the mod 16 factor to find which aspect ratio of group the target size
     * might be put in. So that sizes of the group will be selected to use in the highest priority.
     */
    @Nullable
    private static Rational getAspectRatioGroupKeyOfTargetSize(@Nullable Size targetSize,
            @NonNull List<Size> resolutionCandidateList) {
        if (targetSize == null) {
            return null;
        }

        List<Rational> aspectRatios = getResolutionListGroupingAspectRatioKeys(
                resolutionCandidateList);

        for (Rational aspectRatio : aspectRatios) {
            if (hasMatchingAspectRatio(targetSize, aspectRatio)) {
                return aspectRatio;
            }
        }

        return new Rational(targetSize.getWidth(), targetSize.getHeight());
    }

    // Use target rotation to calibrate the size.
    @Nullable
    private static Size flipSizeByRotation(@Nullable Size size, int targetRotation, int lensFacing,
            int sensorOrientation) {
        Size outputSize = size;
        // Calibrates the size with the display and sensor rotation degrees values.
        if (size != null && isRotationNeeded(targetRotation, lensFacing, sensorOrientation)) {
            outputSize = new Size(/* width= */size.getHeight(), /* height= */size.getWidth());
        }
        return outputSize;
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
}
